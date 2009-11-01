/**
 * This class handles the UrlQ, for the crawler and parser. A note on convetion here.
 * http://www.someurl.com "0"  => URL has not been crawled, just inserted.
 * http://www.someurl.com "1"  => URL has been crawled, but not parsed.
 * http://www.someurl.com "2"  => URL has been parsed. No further processing needed.
 */
package crwlrprsr;

import java.net.MalformedURLException;
import java.net.URL;
import java.sql.*;
import java.util.ArrayList;

public class UrlQ extends AbstractDatabase{

    private int maxURL = 6000;

    // Hardcoded!
    String server = "localhost";
    String port = "3306";
    String user = "ben";
    String password = "ben";
    String schema = "btools";

    public UrlQ(){
       openDB();
    }




    /**
     * Shuts down the database.
     */
    public void closeDB() {
        try {
            con.close();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    /***********/
    /* SETTERS */
    /***********/
    public void setServer(String s) {
        server = s;
    }

    public void setUser(String u) {
        user = u;
    }

    public void setPass(String p) {
        password = p;
    }

    public void setPort(String p) {
        port = p;
    }

    public void setSchema(String s){
        schema = s;
    }

    
    /***********/
    /* GETTERS */
    /***********/


    public String getUser() {
        return user;
    }

    public String getPass() {
        return password;
    }
    public String getPort() {
        return port;
    }

    public String getSchema(){
        return schema;
    }

    public String getServer() {
        return server;
    }


    public synchronized boolean limitReached() {

        if(this.getNumCrawled() >= 6000) {
           
            System.out.println("Limit Reached for Crawler");
            Crwlr.stop();
            return true;
        }
        return false;
    }


  


    /**
     *
     * @return Number of URLs stored in UrlQ.
     */
    public synchronized int getNumCrawled() {
        
        ResultSet result = null; 

        int res = 0;
        result = queryDB("SELECT COUNT(*) FROM urlqueue;");

        try {
            result.next();
            res = result.getInt(1);
        } catch (Exception e) {
            e.getMessage();
        }
        

        System.out.println("getNumCrawled: " + res);
        return res;
    }

    /**
     * 
     * @return The number of parsed Urls.
     */
    public synchronized int getNumParsed() {

        ResultSet result = null;
        int res = 0;

        try {

            Statement stmt = con.createStatement();

            try {

                result = stmt.executeQuery("SELECT COUNT(*) FROM urlqueue WHERE crawled = 2;");
                result.next();
                res = result.getInt(1);

            } catch (Exception e) {
                e.printStackTrace();
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }


        System.out.println("getNumParsed: " + res);
        return res;
    }


    public int getMaxURLs() {
        return maxURL;
    }
    
    public synchronized URL getURL() {
        try{
            // Changed here
            ResultSet rs = queryDB("SELECT * FROM urlqueue WHERE crawled = 0 LIMIT 1;");
            if (rs.next()){

                String u = rs.getString("url");
               
                //updateDB("UPDATE urlqueue SET crawled ='1' WHERE url = '" + u + "';");

                // Inefficient!
                updateDB("DELETE FROM urlqueue WHERE url = '" + u + "';");
                updateDB("INSERT INTO urlqueue VALUES ('" + u + "', 1);");

                return new URL(u);
            }
        }catch(Exception e){
                System.out.println("-----Error getting URL-----");
                System.out.println(e.getMessage());
                System.out.println("---------------------------");
        }

    return null; //empty condition

    }

    public void setMaxURL(int m){
        maxURL = m;
    }
    
   
    // Deletes contents of database
    public String truncate1() {

        String result = "";
        try {
            con = DriverManager.getConnection("jdbc:mysql://localhost:3306/smicrwlr", "ben", "ben");
            Statement stmt = con.createStatement();

            try {
                String sql = "TRUNCATE urlqueue;";
                stmt.executeUpdate(sql);


            } catch ( Exception e ) {
                ; // Ignore Exceptions caused by duplicates
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return result;
    }
    
    // Overloaded Version of addURL
    public synchronized void addURL(ArrayList<String> arr) {

        if(arr == null) return;

        for(String a:arr){
            addURL(a);
        }
    }
    
    public void addFirstURL(String s) {
        
        updateDB("INSERT INTO urlqueue VALUES ('" + s + "', 0);");
    }

    /**
     * Pre-conditions : ExtractURL would have properly formatted the URLs
     * Post-conditions: URLS have been added if it conformes to inDomain
     * @param s The URL to be added
     * @return TRUE if URL has been successfully entered into the database. False otherwise.
     */
    public synchronized boolean addURL(String s) {

        if(limitReached()) return false;

        URL tmpURL;

        System.out.println("Before inDomain, s:" + s);
        if(inDomain(s)) {
         
            try{
                System.out.println("URL? :" + s);
                tmpURL = new URL(s);
                updateDB("INSERT INTO urlqueue VALUES ('" + tmpURL.toExternalForm() + "', 0);");
                return true;
            } catch (MalformedURLException e){
                System.out.println("-----URLQ Insert Error-----");
                System.out.println(e.getMessage());
                System.out.println("---------------------------");
            } catch (Exception e) {
                System.out.println("Something else went wrong in url queue");
                e.getMessage();
            }
        }
        return false;
    }
    



    /**
     * This method is actually a filter. This means that if the website code changes,
     * this needs to be changed too.
     * @param url
     * @return TRUE if its a valid page containing phone data.
     */
    public synchronized boolean inDomain(String url){

        // This is the specific string in the url which contains all the data
        // on the tools.
        if(url.contains("browntool.com") && !url.contains("__doPostBack")){
            System.out.println(url + " is in domain");
            
            return true;
        }
        return false;
    }

    /**
     * 
     * @param s The URL.
     * @return TRUE if URL has been crawled.
     */
    public synchronized boolean isProcessed(String s) {
        try{
            ResultSet rs = queryDB("SELECT crawled FROM urlqueue WHERE url='" + s + "';");
            if (rs.next()){
                int r = rs.getInt("crawled");    
                
                if(r == 1) return true;
            }
        }catch(Exception e){
                System.out.println("-----Error in isProcessed -----");
                System.out.println(e.getMessage());
                System.out.println("---------------------------");
        }       
        return false;
    }    
} //~UrlQ
