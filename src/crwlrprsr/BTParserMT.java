package crwlrprsr;

// That's alot of imports.
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * This code contains the main logic of the parser. One caveat is that this parser is written 
 * specifically to parse webpages found in www.pdadb.net, primarily because it has a huge database
 * of phones.
 * 
 * The parser ensures that all URLs stored in UrlQ corresponds to a unique model of phone/pda. 
 * 
 */

public class BTParserMT {

    static int NUM_THREADS = 10;
    static int DELAY_INTERVAL;
    
    static ExecutorService executor;
    static UrlQ urlq;

    volatile static boolean stopFlag = false;


    public static void main(String[] args) throws Exception{

        if(NUM_THREADS < 0){
            setNumThreads(10);
        }

        executor = Executors.newFixedThreadPool(getNumThreads());

        while( NUM_THREADS -- > 0 ) {
            executor.execute(new BTParserST());
            Thread.sleep(10 * 1000);
        }
        executor.shutdown();
    }
    
    /**
     * 
     * @param num Sets the number of threads for the parser.
     */
    public static void setNumThreads(int num){
        NUM_THREADS = num;
    }
    
    /**
     * 
     * @return The number of threads currently set for the parser.
     */
    public static int getNumThreads(){
        return NUM_THREADS;
    }
    
    /**
     * 
     * @param interval Sets the delay interval (for downloading the webpage) in milliseconds (ms)
     */
    public static void setDelayInterval(int interval){
        DELAY_INTERVAL = interval;
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
     * @param u The UrlQ to be used by the parser.
     */
    public static void setUrlQ(UrlQ u){
        urlq = u;
    }
    
    /**
     * 
     * @return The UrlQ used by the parser.
     */
    public static UrlQ getUrlQ(){
        return urlq;
    }

    /**
     * Stops the parsing. WARNING: Do not expect this to be immediate. And don't be alarmed if 
     * the compiler spits out alot of NullPointerExceptions. 
     */
    public synchronized static void stop(){
        stopFlag = true;

    }

   
} //~ GSMArenaParser