/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package crwlrprsr;

import java.sql.*;
import java.sql.DriverManager;
import java.sql.SQLException;


public abstract class AbstractDatabase {

    Connection con;

    public AbstractDatabase() {
        // Connect to the database
        System.out.println("Successfully Open Database: " + openDB());
    }

    /**
     * Connects to the database.
     * @return TRUE if connected to database successfully.
     */
    public boolean openDB() {
        try {

            con = DriverManager.getConnection("jdbc:mysql://localhost:3306/btools","root","root");

            return true;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    public void closeDB() {
        try {
            con.close();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public synchronized void updateDB(String sql) {

        try {

            Statement stmt = con.createStatement();

            try {
                System.out.println("Update DB: " + sql);
                stmt.executeUpdate(sql);
                
            } catch (com.mysql.jdbc.exceptions.jdbc4.MySQLIntegrityConstraintViolationException e) {

            } catch (Exception e) {
                // Time to revise your SQL ... tsk tsk.
                System.out.println("------- Error on updateDB -------");
                System.out.println(e.getMessage());
                e.printStackTrace();
                System.out.println("---------------------------------");
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }

    }


    public synchronized ResultSet queryDB(String sql) {

        ResultSet result = null;

        try {

            Statement stmt = con.createStatement();

            try {
                System.out.println("queryDB: " + sql);
                result = stmt.executeQuery(sql);

            } catch (Exception e) {
                System.out.println("-------- Error on queryDB -------");
                System.out.println(e.getMessage());
                System.out.println("---------------------------------");
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return result;
    }

}
