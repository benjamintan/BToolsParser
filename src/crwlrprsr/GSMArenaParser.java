package crwlrprsr;

// So many imports ...
import java.io.BufferedInputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import org.htmlparser.*;
import org.htmlparser.filters.OrFilter;
import org.htmlparser.filters.TagNameFilter;
import org.htmlparser.filters.HasAttributeFilter;
import org.htmlparser.util.*;

public class GSMArenaParser {

    static int numThreads = 6;


    public static void main(String[] args){
        
        ExecutorService executor = Executors.newFixedThreadPool(numThreads);

        while(numThreads-- > 0)
            executor.execute(new GSMArenaParserThread());

        executor.shutdown();
    }

    public static class GSMArenaParserThread implements Runnable{

        static boolean DEBUG = true;

        String url = null;
        String page = null;
        String parsedPage = null;

        //ParseQ parseQ;
        UrlQ uQ;
        ToolsDB pdb;

        String PhoneName, Network2G, Network3G, Announced, Status, Dimensions;
        String Weight, DType, DSize, InternalMem, CardSlot, GPRS, HSCSD, EDGE;
        String ThreeG, WLAN, Bluetooth, Infrared, USB, PrimaryCam, VideoCam;
        String SecondaryCam, OS, CPU, Messaging, Browser, Radio, Colors,GPS;

        public GSMArenaParserThread(){
            uQ = new UrlQ();
            uQ.openDB();
            pdb = new ToolsDB();
        }

        public void run() {
        
            String sql = "SELECT url FROM urlqueue;";

            try {
                ResultSet rs = uQ.queryDB(sql);
                String tempURL;

                while(rs.next()) {
                    tempURL = rs.getString(1);
                    System.out.println("Parsing: " + tempURL);
                    page = downloadPage(tempURL);

                    if(page.length() > 0) {
                        buildPage(processPage(extractSpec()));
                    }
                }
            } catch (SQLException e) {
                e.getMessage();
            }
        }

        private synchronized String downloadPage(String u) {

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


        // Extracts phone specs from GSMArena Spec page in a nice user-friendly format
        public synchronized String extractSpec() {

            Parser parser = new Parser();

            try {

                parser.setInputHTML(page);

                NodeList list = new NodeList ();
                NodeFilter filter_h1 = new TagNameFilter ("h1");
                NodeFilter filter_id = new HasAttributeFilter ("id", "specs-list");
                // Combine filters
                NodeFilter filter_both = new OrFilter(filter_h1, filter_id);

                for (NodeIterator e = parser.elements (); e.hasMoreNodes ();) {
                    e.nextNode().collectInto(list, filter_both);
                }

                parsedPage = list.asString();

            } catch (org.htmlparser.util.ParserException e) {
                e.printStackTrace();
            }

            System.out.println("Done Extracting Spec");

            return parsedPage;
        }

        // Returns an array or split terms.
        public synchronized String[] processPage(String page) {

            System.out.println("Processing Page");

            page = page.replaceFirst("General", "");
            page = page.replaceFirst("Size", "");
            page = page.replaceFirst("Display", "");
            page = page.replaceFirst("Sound", "");
            page = page.replaceFirst("Memory", "");
            page = page.replaceFirst("Data", "");
            page = page.replaceFirst("Camera", "");
            page = page.replaceFirst("Features", "");
            page = page.replaceFirst("Misc", "");
            page = page.replaceFirst("2G Network", "+2G Network+");
            page = page.replaceFirst("3G Network", "+3G Network+");
            page = page.replaceFirst("Announced", "+Announced+");
            page = page.replaceFirst("Status", "+Status+");
            page = page.replaceFirst("Released", "+Released+");
            page = page.replaceFirst("OS", "+OS+");
            page = page.replaceFirst("CPU", "+CPU+");
            page = page.replaceFirst("Messaging", "+Messaging+");
            page = page.replaceFirst("Browser", "+Browser+");
            page = page.replaceFirst("Radio", "+Radio+");
            page = page.replaceFirst("Games", "+Games+");
            page = page.replaceFirst("Colors", "+Colors+");
            page = page.replaceFirst("GPS", "+GPS+");
            page = page.replaceFirst("Stand-by", "+Stand-by+");
            page = page.replaceFirst("Talk time", "+Talk time+");
            page = page.replaceFirst("Price group", "+Price group+");
            page = page.replaceFirst("Size", "+Size+");
            page = page.replaceFirst("Phonebook", "+Phonebook+");
            page = page.replaceFirst("Card slot", "+Card slot+");
            // Clear out all those pesky spaces ...
            page = page.replaceAll("\\n\\n", "");
            page = page.replaceAll("\\n\\r", "");
            page = page.replaceAll("\\t", "");
            page = page.replaceAll("&nbsp;", "");
            page = page.replaceAll("\\n", "+");
            page = page.replaceAll("\\+\\+", "+");

            String[] parseArr = null;
            parseArr = page.split("\\+");

            if(DEBUG) {
                System.out.println("Printing Parse Array");
                for(String a:parseArr){
                    System.out.println(a);
                }
            }
            return parseArr;
        }

        // Builds up a page which stores
        public synchronized boolean buildPage(String[] a) {

            if(a == null)
                return false;

            PhoneName = a[0];
            System.out.println("PhoneName : " + PhoneName);

            for(int i=1;i< a.length;i++) {
                if(a[i].length()<=1){
                    ; // Do nothing if empty
                }

                if(a[i].matches("Dimensions")) {
                    Dimensions = a[i+1];
                }
                else if(a[i].matches("Type")) {
                    DType = a[i+1];
                }
                else if(a[i].matches("Size")) {
                    DSize = a[i+1];
                }
                else if(a[i].matches("Internal")) {
                    InternalMem = a[i+1];
                }

                else if(a[i].matches("WLAN")) {
                    WLAN = a[i+1];
                }
                else if(a[i].matches("OS")) {
                    OS = a[i+1];
                }
                else if(a[i].matches("CPU")) {
                    CPU = a[i+1];
                }
                else if(a[i].matches("Card slot")) {
                    CardSlot = a[i+1];
                }
                else if(a[i].matches("GPS")) {
                    GPS = a[i+1];
                }
            } //for

            if(DEBUG) {
                System.out.println("Phone: " + PhoneName);
                System.out.println("Display Size: " + DSize);
                System.out.println("Display Type: " + DType);
                System.out.println("WLAN: " + WLAN);
                System.out.println("GPS: " + GPS);
                System.out.println("Card Slot: " + CardSlot);
                System.out.println("OS: " + OS);
                System.out.println("CPU: " + CPU);
            }

            if(PhoneName == null || DSize == null || DType == null){
                System.out.println("No info found on page");
                return false;
            }


            // Add information to database
            String sql = "INSERT INTO phone_test " +
                    "VALUES ('" + PhoneName + "', '" + DSize + "', ' " + DType
                    + "', '" + WLAN + "', '" + GPS + "', '" + CardSlot
                    + "', '" + OS + "', '" + CPU + "');";

            System.out.println("Build Page: " + sql);

            // Populate the phone database: Final step
            //pdb.insertDB(PhoneName, DSize, DType, WLAN, GPS, CardSlot, OS, CPU);



            return true;
        }

        public static void main(String[] args) {

            GSMArenaParserThread gap = new GSMArenaParserThread();
            gap.page = gap.downloadPage("http://www.gsmarena.com/alcatel_elle_no_3-1890.php");
            String p = gap.extractSpec();
            String[] a = gap.processPage(p);
            gap.buildPage(a);
        }

    } //~ End GSMArenaParserThread

} //~ GSMArenaParser