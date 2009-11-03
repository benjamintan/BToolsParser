/**
 * PicParser is a customised crawler/parser which simply matches picture links on the
 * www.PDAdb.net website and adds it to the Phone Database. It would be too much of a hassle
 * to include the parsing code in PDAdbParser.
 * @author Benjamin
 */

package crwlrprsr;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.sql.ResultSet;
import javax.imageio.ImageIO;



public class BTPicDL {

    private String picURL;
    private ToolsDB tdb;

    public BTPicDL(){
       tdb = new ToolsDB();
    }




    public void runPicDownloader(){


        ResultSet rs = tdb.queryDB("SELECT DISTINCT(imgUrl) FROM  btools WHERE imgUrl LIKE 'http%'");
        String aStr;

        try {

            while (rs.next()){

                aStr = rs.getString(1);

                // Save the file to local!
                BufferedImage img = null;
                    System.out.println("Saving image ...:" + aStr);

             
                                     
                    //System.out.println("Saving image ...:" + aStr);
                    // Saves the image
                    aStr = aStr.replace("%20", " ");

                    img = ImageIO.read(new URL(aStr));

                    // Process URL: Only want the filename
                    int slash = aStr.lastIndexOf("/");
                    String newStr = aStr.substring(slash+1, aStr.length());
                    // Update the database with nicer looking filenames
                    String sql = "UPDATE btools SET imgUrl = '" + newStr + "' WHERE imgUrl = '"+ aStr + "'";
                    tdb.updateDB(sql);

                    save(img, newStr);
                    

             
            }

        }
        catch(Exception e){
              System.out.println(e.getMessage());
        }

    }

    public void save(BufferedImage toCopy, String filename) {

        String suffix = filename.substring(filename.lastIndexOf('.') + 1);
        suffix = suffix.toLowerCase();

        System.out.print(" *** Img Filename *** : " + filename);
        System.out.println("." + suffix);

        if (suffix.equals("jpg")) {

            try {
                System.out.println(" *** Img Filename *** : " + filename);
                // Create a new image, and save it to filename.

                ImageIO.write(toCopy, suffix, new File(filename));
            }
            catch (IOException e) {
                e.printStackTrace();
            }
        }
        else {
            System.out.println("Error: filename must end in .jpg");
        }
    }


    public static void main(String[] args){

        BTPicDL p = new BTPicDL();

        p.runPicDownloader();

    }
}
