package crwlrprsr;

import java.io.BufferedInputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import org.htmlparser.*;
import org.htmlparser.filters.AndFilter;
import org.htmlparser.filters.OrFilter;
import org.htmlparser.filters.HasAttributeFilter;
import org.htmlparser.filters.NotFilter;
import org.htmlparser.filters.TagNameFilter;
import org.htmlparser.util.*;


public class BTParserST {

    static boolean DEBUG = true;

    String url = null;
    String page = null;
    String parsedPage = null;

    UrlQ uQ;
    ToolsDB db;
    Parser parser;
    ArrayList itemList = new ArrayList<Item>();

    public BTParserST(){

        db = new ToolsDB();
        uQ = new UrlQ();
    }

    
    protected String downloadPage(String u) {

        InputStream is = null;
        DataInputStream dis = null;

        try {

            URL url = new URL(u);

            is = url.openStream();
            dis = new DataInputStream( new BufferedInputStream(is) );

            String line = null;

            page = "";
            // Builds the HTML page
            while(( line = dis.readLine() )!=null) {
                page += line + "\n";
            }

            is.close();
            dis.close();

        }
        catch ( IOException e ) {
            return "";
        }

        return page;
    } //~ downloadPage()

   

    // Extract information from a single (Brown Tools) page
    public void processSinglePage() {
        
        String title = "1";
        String itemCode = "2";
        String skuEan = "3";
        String price = "4";
        String imgUrl;
        String categories[];
        String description = "";
        
        parser = new Parser();

        try {

            parser.setInputHTML(page);
            // This filter retrives the description of the page
            NodeFilter filter_p = new TagNameFilter("p");

            NodeList list = new NodeList();

            for (NodeIterator e = parser.elements (); e.hasMoreNodes ();){

                e.nextNode().collectInto(list, filter_p);
            }

            description = list.toHtml();
            //System.out.println(list.toHtml());

            // This set of filters gets the menu bar, product title, Item code, SKU/EAN and Price
            TagNameFilter filter_title = new TagNameFilter("title");
            HasAttributeFilter filter_catsh = new HasAttributeFilter("class", "CATSubHead");
            HasAttributeFilter filter_catpn = new HasAttributeFilter("class", "CATProductNumber");
            HasAttributeFilter filter_catuc = new HasAttributeFilter("class", "CATProductListItem");
            OrFilter filter_sh_pn = new OrFilter(filter_catsh, filter_catpn);
            OrFilter filter_sh_pn_uc = new OrFilter(filter_catuc, filter_sh_pn);
            // Super duper uber filter ... Ya ZAAAAA! 
            OrFilter filter_sh_pn_uc_title = new OrFilter(filter_sh_pn_uc, filter_title);

            NodeList list2 = new NodeList();
            parser = new Parser();
            parser.setInputHTML(page);

            for (NodeIterator e = parser.elements (); e.hasMoreNodes ();){
                e.nextNode().collectInto(list2, filter_sh_pn_uc_title);
            }

            //System.out.println(list2.asString().trim());
            
            // Perform some post processing to retreive necessary information
            String s = list2.asString().trim();
            s = s.replace("Product ListItem code", "|Item code|");
            s = s.replace("StoreItem code", "|Item code|");
            s = s.replace("SKU/EAN", "|SKU/EAN|");
            s = s.replace("Price", "|Price|");
            s = s.substring(0, s.indexOf("Quantity"));

            String[] arr = s.split("\\|");



            title = arr[0].trim();

            for(int i = 1; i < arr.length; i++) {

                if(arr[i].equals("Item code"))
                    itemCode = arr[i+1].trim();
                else if(arr[i].equals("SKU/EAN"))
                    skuEan = arr[i+1].trim();
                else if(arr[i].equals("Price"))
                    price = arr[i+1].trim();
            }

            //System.out.println(title + " " + itemCode + " " + skuEan + " " + price);


            // This set of filters is to determine the type of tool based
            // on the websites navigation bar. Eg: Brown Tools > Cutting Tools > ...)
            NodeList list3 = new NodeList();
            parser = new Parser();
            parser.setInputHTML(page);

            HasAttributeFilter filter_catcb = new HasAttributeFilter("class", "CATCommandButton");


            for (NodeIterator e = parser.elements (); e.hasMoreNodes ();){
                e.nextNode().collectInto(list3, filter_catcb);
            }



            Node[] noA = list3.toNodeArray();
            
            // Assuming no more than 4 categories
            categories = new String[4];

            System.out.println(noA.length);
            // Skip ahead, the first 3 are useless.
            for(int i = 3; i < noA.length; i++) {
                categories[i-3] = noA[i].toPlainTextString();
                //System.out.println(noA[i].toPlainTextString());
            }

            // This set of filters is to retreive the picture link from the page.
            NodeList list4 = new NodeList();
            parser = new Parser();
            parser.setInputHTML(page);

            //TagNameFilter filter_td = new TagNameFilter("TD");
            TagNameFilter filter_img = new TagNameFilter("img");
            HasAttributeFilter filter_id = new HasAttributeFilter("id");
            NotFilter filter_nti = new NotFilter(new HasAttributeFilter("title", "Minimize"));
            HasAttributeFilter filter_sr = new HasAttributeFilter("src");
            HasAttributeFilter filter_al = new HasAttributeFilter("alt");
            HasAttributeFilter filter_st = new HasAttributeFilter("style");
            AndFilter filter1 = new AndFilter(filter_id, filter_nti);
            AndFilter filter2 = new AndFilter(filter_sr, filter_al);
            AndFilter filter3 = new AndFilter(filter_st, filter_img);
            AndFilter filter1_2 = new AndFilter(filter1, filter2);
            AndFilter filter1_2_3 = new AndFilter(filter1_2, filter3);

            for (NodeIterator e = parser.elements (); e.hasMoreNodes ();){
                e.nextNode().collectInto(list4, filter1_2_3);
            }

            Node[] noAr = list4.toNodeArray();

            // Only this one contains the url link
            imgUrl = noAr[1].toHtml();

            int srcIdx = imgUrl.indexOf("src=") + 5;
            int altIdx = imgUrl.indexOf("alt=") - 2;

            imgUrl = "http://www.browntool.com" + imgUrl.substring(srcIdx, altIdx);

            //System.out.println(imgUrl);


            // Now add all the gathered information into itemList
            Item i = new Item(title, itemCode, skuEan, price, imgUrl, description, categories);
            itemList.add(i);
            db.insertDB(i.getItemCode(), i.getName(), i.getDesc(), i.getImgUrl(),
                    i.getCategories(), i.getSkuEan(), i.getPrice());

            System.out.println("Item Added: " + i.toString());


        } catch (Exception e){
            e.getMessage();
        }
    } //~ processSinglePage

    void printItemList(){
        for(Object it: itemList){
            System.out.println(((Item)it).toString());
        }
    }


    public void runParser() {

        String tempURL;
        String sql = "SELECT * FROM urlqueue u WHERE crawled != '2' && url LIKE '%CategoryID%' && url LIKE '%ProductID%'";

        ResultSet rs;


        try {

            rs = uQ.queryDB(sql);

            while(rs.next()) {
                tempURL = rs.getString(1);
                System.out.println("Parsing: " + tempURL);
                page = downloadPage(tempURL);

                if(page.length() > 0) {
                    processSinglePage();
                    // Set the page to crawled = 2
                    String sql2 = "UPDATE urlqueue SET crawled = '2' where url = '" + tempURL + "'";
                    System.out.println("Updating: " + sql);
                    uQ.updateDB(sql2);
                }
            }
        } catch (SQLException e) {
            e.getMessage();
        }
    }





    public static void main(String[] args) {

        BTParserST btp = new BTParserST();
        btp.runParser();

        //btp.page = btp.downloadPage("http://www.browntool.com/Default.aspx?tabid=255&CategoryID=195&List=1&Level=a&ProductID=1239");
        //btp.processSinglePage();
        //btp.printItemList();
    }


} //~ End PDAdbST


class Item {

    String name, itemCode, skuEan, price, imgUrl, desc;
    String[] categories;

    public Item(String name, String itemCode, String skuEan, String price,
            String imgUrl, String desc, String[] categories) {
        this.name = name;
        this.itemCode = itemCode;
        this.skuEan = skuEan;
        this.price = price;
        this.imgUrl = imgUrl;
        this.desc = desc;
        this.categories = categories;
    }

    public String[] getCategories() {
        return categories;
    }

    public String getDesc() {
        return desc;
    }

    public String getImgUrl() {
        return imgUrl;
    }

    public String getItemCode() {
        return itemCode;
    }

    public String getName() {
        return name;
    }

    public String getPrice() {
        return price;
    }

    public String getSkuEan() {
        return skuEan;
    }

    public void setCatagories(String[] catagories) {
        this.categories = catagories;
    }

    public void setDesc(String desc) {
        this.desc = desc;
    }

    public void setImgUrl(String imgUrl) {
        this.imgUrl = imgUrl;
    }

    public void setItemCode(String itemCode) {
        this.itemCode = itemCode;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setPrice(String price) {
        this.price = price;
    }

    public void setSkuEan(String skuEan) {
        this.skuEan = skuEan;
    }

    @Override
    public String toString(){

        String cat = "";

        for(int i=0; i < categories.length; i++){
            if(categories[i]!=null){
                    cat += categories[i] + " ";
            }
        }

        return "Categories  : " + cat + "\n" +
               "Product Name: " + this.name + "\n" +
               "Item Code   : " + this.itemCode + "\n" +
               "SKU/EAN     : " + this.skuEan + "\n"+
               "Price       : " + this.price + "\n" +
               "Image URL   : " + this.imgUrl + "\n" +
               "HTML Desc   : " + this.desc;
    }

    public void addItem(ArrayList al, Item i){
        al.add(i);

    }


}
