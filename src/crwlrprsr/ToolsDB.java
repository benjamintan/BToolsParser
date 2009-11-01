/**
 * This class provides methods to connect and insert values from the phone database.
 * @author Benjamin 
 */

package crwlrprsr;


public class ToolsDB extends AbstractDatabase{
    
    public ToolsDB() {
        openDB(); // Connect to the database
    }
    
    public synchronized void insertDB(String itemCode, String name, String desc,
            String imgUrl, String[] cat, String skuEan, String price){
        
        String sql = "INSERT INTO btools VALUES ('"
               
                + itemCode + "', '"
                + name + "', '"
                + desc + "', '"
                + imgUrl + "', '"
                + cat[0] + "', '"
                + cat[1] + "', '"
                + cat[2] + "', '"
                + cat[3] + "', '"
                + itemCode + "', '"
                + skuEan + "', '"
                + price + "');";
        System.out.println("Print: " + sql);


        // Insert the sql
        updateDB(sql);
    }
} //~ PhoneDB