
package crwlrprsr;

import java.io.BufferedInputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URL;
import java.util.ArrayList;

import java.util.Random;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import org.htmlparser.filters.NodeClassFilter.*;
import org.htmlparser.filters.NodeClassFilter;
import org.htmlparser.tags.*;
import org.htmlparser.util.*;


/**
 * This code contains the main logic of the crawler. The crawler is crawler uses ExecutorService
 * to provide multithreading. Generally 6 crawlers are the maximum. This has been tested via
 * extremely un-scientific means. When Moore's law catches up, the number can be increased.
 *
 * Delay and sleep intervals have been implemented to minimise the load on the server (victim?).
 *
 * Extracting of the links is implemented using the open sourced <b>HTMLParser</b> out of sheer
 * laziness.
 * @author Benjamin
 */
public class Crwlr {

    static int SLEEP_INTERVAL = 1000;
    static int DELAY_INTERVAL = 1000;
    static int NUM_THREADS = 6;
    static UrlQ urlq = new UrlQ();

    static String FIRST_URL;
    static ExecutorService executor;

    volatile static boolean stopFlag = false;

    /**
     * Main method used for testing without any other classes. This allows the crawler to be
     * run independently of other classes.
     * @param args
     */

    public static void main(String[] args){

        /*************************************************************/
        /* This demonstrates the main flow of setting up the crawler */
        /*************************************************************/

        // 1. Set up the number of threads, sleep and delay intervals
        setNumThreads(getNumThreads());
        setSleepInterval(getSleepInterval());
        setDelayInterval(getDelayInterval());

        // 2. Set up the ExecutorService (for mulithreading)
        executor = Executors.newFixedThreadPool(6);

        // 3. Set up the (victim) URL
        setFirstURL("http://www.browntool.com/");

        // 
        setUrlQ(urlq);


        // 4. Add the URL to ... wait for it ... the UrlQ!
        urlq.addFirstURL(getFirstURL());

        // 5. Run the threads
        while( NUM_THREADS -- > 0 ) {
            executor.execute(new CrwlrThread(urlq));
        }

        // 6. Cleanly shutdown the executor when done.
        executor.shutdown();
    }


    /**
     * Stops the crawling. WARNING: Don't expect this to be immediate, since some of the
     * threads might be in the midst of processing. Note that NullPointerExceptions might 
     * pop up also, although generally I have tried to reduce them.
     */
    public synchronized static void stop(){
        stopFlag = true;

    }

    /**
     * Sleep intervals is the time in between when the crawler is wating for a url
     * from the UrlQ if its either not present or all the urls have already been crawled
     * previously.
     * @param interval Sets the sleep interval in milliseconds (ms)
     */
    public static void setSleepInterval(int interval){
        SLEEP_INTERVAL = interval;
    }

    /**
     * Delay intervals is the time in between retries when downloading webpages
     * @param interval Sets the delay interval in milliseconds (ms)
     */
    public static void setDelayInterval(int interval){
        DELAY_INTERVAL = interval;
    }

    /**
     *
     * @param num Sets the number of threads. The GUI enforces that the maximum number is
     * set to 6.
     */
    public static void setNumThreads(int num){
        NUM_THREADS = num;
    }

    /**
     *
     * @return The number of threads currently set.
     */
    public static int getNumThreads(){
        return NUM_THREADS;
    }

    /**
     *
     * @return The sleep interval in milliseconds (ms)
     */
    public static int getSleepInterval(){
        return SLEEP_INTERVAL;
    }
    
    /**
     *
     * @return The delay interval in milliseconds (ms)
     */
    public static int getDelayInterval(){
        return DELAY_INTERVAL;
    }

    /**
     *
     * @param url The first URL for the crawler to begin crawling
     */
    public static void setFirstURL(String url){
        FIRST_URL = url;
    }

    /**
     *
     * @return The first URL to crawl
     */
    public static String getFirstURL(){
        return FIRST_URL;
    }

    /**
     *
     * @param u The instance of UrlQ for the crawler to use
     */
    public static void setUrlQ(UrlQ u){
        urlq = u;
    }

    /**
     *
     * @return The UrlQ which the crawler is currently using
     */
    public static UrlQ getUrlQ(){
        return urlq;
    }


    /**
     *
     * @return A percentage to mark the progress of crawling
     */
    public static int percentDone(){
        return (int)((double)urlq.getNumCrawled() * 100 / (double) urlq.getMaxURLs());
    }

    /**
     * The logic for a single crawler thread
     */
    public static class CrwlrThread implements Runnable {

        private UrlQ q;
        private Random generator = new Random();

        // You've been warned.
        public CrwlrThread() {
            System.out.println("Don't use this constructor");
        }

        public CrwlrThread(UrlQ q) {
            this.q = q;
        }

        /**
         * Downloads the webpage. It doesn't except any paraments since the URL is gotten
         * from the UrlQ.
         * @return An array! First element contains the page URL. Second element contains
         * the page itself.
         */
        public synchronized String[] downloadPage() {

            // Returns if stop flag has been set.
            if(Crwlr.stopFlag) return null;

            URL url = null;
            String page = "";

            // Alright, I hardcorded this. And I'm too lazy to do otherwise. :X
            int MAX_TRIES = 100;
            int tries = 0;

            // Randomly generate  from 1000 to 10000 ms
            DELAY_INTERVAL = (int) generator.nextDouble() * 1000 + DELAY_INTERVAL;

            System.out.println("DELAY INTERVAL :" + DELAY_INTERVAL );

            try{

                // Sleep first! Otherwise you might get blocked! (Like I did from GSMArena
                // after crawling 3000 x 6 == 180 000 links). lol.
                Thread.sleep(DELAY_INTERVAL);

                while (!stopFlag){
                
                    url = q.getURL();
                 
                    if (url == null){
                        
                        // If UrlQ doesn't have a URL for us, we wait patiently ...
                        System.out.println("Thread sleeping ...");
                        Thread.sleep(SLEEP_INTERVAL);
                        
                        tries++;

                        // ... till we finally lose our patience.
                        if(tries > MAX_TRIES)
                            stop();

                    }else{
                        tries = 0; //reset tries
                        break;
                    }
                }//~for loop

                
            }catch(InterruptedException ex){ ex.getMessage(); }//ignore since no interrupts
            
            if(Crwlr.stopFlag) return null;

            System.out.println("Downloading: " + url.toExternalForm());


            /***********************************************/
            /* Logic to set up the webpage for downloading */
            /***********************************************/

            InputStream is = null;
            DataInputStream dis = null;

            try {

                is = url.openStream();
                dis = new DataInputStream( new BufferedInputStream(is) );

                String line = null;

                // Builds the HTML page
                while(( line = dis.readLine() )!=null) {
                    page += line + "\n";
                }

                // Release resources.
                is.close();
                dis.close();
            }
            catch ( IOException e ) {
                e.printStackTrace();
            }

            // arr[0]: Url of the webpage
            // arr[1]: The actual contents of the webpage
            String[] arr = new String[2];
            arr[0] = url.toExternalForm();
            arr[1] = page;

            return arr;
        }


        /**
         * Extract links from the webpage.
         * @param arr This value is returned from downloadPage()
         * @return An arraylist of hyperlinks
         */
        public synchronized ArrayList<String> extractLinks(String[] arr) {

            // Returns if stop flag has been set
            if(Crwlr.stopFlag) return null;

            ArrayList<String> links = new ArrayList<String>();

            try {

                // Remember: arr[0] is the URL ...
                URI urllink = new URI(arr[0]);

                // This parser is the HTMLParser. Not the one I wrote.
                org.htmlparser.Parser parser = new org.htmlparser.Parser();

                // ... arr[1] is the page itself
                parser.setInputHTML(arr[1]);

                // Extract all notes which are hyperlinks
                NodeList list = parser.extractAllNodesThatMatch(new NodeClassFilter (LinkTag.class));

                for (int i = 0; i < list.size (); i++){

                    LinkTag extracted = (LinkTag)list.elementAt(i);

                    String extractedLink = extracted.getLink();
                    
                    // (i.e. : http://www.pdadb.net)
                    if ( extractedLink.startsWith("http://") ){
                        //System.out.println("0. Adding Link:" + extractedLink);
                        links.add(removeUglyChar(extractedLink));
                        //System.out.println(extractedLink);

                    }
                    // (i.e. : www.pdadb.net/)
                    else if ( extractedLink.startsWith("www.") ){

                        //System.out.println("1. Adding Link:" + extractedLink);
                        extractedLink = "http://" + extractedLink;
                        //System.out.println(extractedLink);
                    }
                    // (i.e. : /index.php)
                    else if ( extractedLink.startsWith("/") ){

                        //System.out.println("2. Adding Link:" + extractedLink);
                        extractedLink = urllink.getHost() + extractedLink;
                        //System.out.println(extractedLink);
                        
                    }
                    // (i.e. : index.php)
                    else {
                        
                        //System.out.println("3. Adding Link:" + extractedLink);
                        extractedLink = urllink.getHost() + "/" + extractedLink;
                        //System.out.println(extractedLink);
                    }

                    // Otherwise we have MalformedUrl Errors
                    if ( !extractedLink.startsWith("http://") ){

                        //System.out.println("4. Adding Link:" + extractedLink);
                        extractedLink = "http://" + extractedLink;
                        
                    }

                    // Finally, we can add the link ...
                    System.out.println(removeUglyChar(extractedLink));
                    links.add(removeUglyChar(extractedLink));
                }
            } catch (Exception e) {e.getMessage();}

            // .. return our value and exit from the method
            return links;
        }

        private String removeUglyChar(String s){

            s = s.replaceAll(";", "");
            s = s.replaceAll("amp", "");
            String t = "window.location.href='/";

            if(s.contains(t)){
                s = s.replace(t, "");
                s = s.substring(0, s.length()-1);
            }

            return s;
        }



        /**
         * The method is the one where the multi-threading takes place.
         *
         * Note: Adding a URL to the queue is as simple as writing
         * q.addURL(extractLinks(downloadPage())). This of course assumes that there
         * is some URL in the queue which is NOT crawled.
         */
        public void run() {

            // downloadPage will keep looking for new entries in the database
            while (!Crwlr.stopFlag && !q.limitReached()){

                System.out.println("< Thread ID: " + Thread.currentThread().getId() + " >");
                
                // Adds the links to the UrlQ.
                q.addURL(extractLinks(downloadPage()));
            }

            System.out.print("Limit Reached or Stop Flag");
        }
    }
} //~ Crwlr