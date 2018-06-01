/*
 *
 */

package ro.cst.tsearch.database;

import ro.cst.tsearch.database.OraConstants;

/**
 * Keep information about connections
 */
public class ConnectionManager {

    /*initialized driver properties*/
    public static String driver   = "com.mysql.jdbc.Driver";
    public static String user     = OraConstants.DATABASE_USER;
    public static String password = OraConstants.DATABASE_PASSWD;
    public static String host     = OraConstants.DATABASE_MACHINE;
    public static String sid      = OraConstants.DATABASE_SID;
    public static int port        = OraConstants.DATABASE_PORT;
    public static String url      = "jdbc:mysql://"	+ host + ":" + port + "/" + sid;



}
