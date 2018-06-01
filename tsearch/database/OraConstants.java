/*
 * @(#)DataConstants.java 1.30 2000/08/17
 * Copyright (c) 1998-2000 CornerSoft Technologies, SRL
 * Bucharest, Romania
 * All Rights Reserved.
 */
package ro.cst.tsearch.database;
import java.util.ResourceBundle;

import ro.cst.tsearch.utils.URLMaping;

public class OraConstants {
	
	private static ResourceBundle rbc = ResourceBundle.getBundle(URLMaping.ORA_CONFIG);
	/* start FIELDS DECLARATION*/	
	public static final int CONN_INACTIVITY_TIMEOUT;
	public static final int CONN_TTL_TIMEOUT;
	
	public static final int CONN_MINNUMBER;
	public static final int CONN_MAXNUMBER;
	public static final String DATABASE_USER;
	public static final String DATABASE_PASSWD;
	public static final String DATABASE_MACHINE;
	public static final String DATABASE_SID;
	public static final int DATABASE_PORT;
	public static final int DATABASE_POOL_MONITOR_STATISTICS_INTERVAL;
	public static final int DATABASE_POOL_HELPER_THREADS;
	public static final String REPLICATION_USER;
	public static final String REPLICATION_PASSWORD;
	//static initialize begin
	static {
		//open LocalStrings.properties file
		CONN_INACTIVITY_TIMEOUT = getInt(rbc, "conn.inactivityTimeOut");
		CONN_TTL_TIMEOUT = getInt(rbc, "conn.TTLTimeOut");
		CONN_MAXNUMBER = getInt(rbc, "conn.maxNumber");
		CONN_MINNUMBER = getInt(rbc, "conn.minNumber");
		
		DATABASE_USER = getString(rbc, "database.user");
		DATABASE_PASSWD = getString(rbc, "database.passwd");
		DATABASE_MACHINE = getString(rbc, "database.machine");
		DATABASE_SID = getString(rbc, "database.sid");
		DATABASE_PORT = getInt(rbc, "database.port");
		DATABASE_POOL_MONITOR_STATISTICS_INTERVAL =
			getInt(rbc, "pool.monitorStatisticsInterval");
		DATABASE_POOL_HELPER_THREADS = getInt(rbc, "pool.numHelperThreads");
		
		REPLICATION_USER = getString(rbc, "database.user.replication");
		REPLICATION_PASSWORD = getString(rbc, "database.pass.replication");
	}
	/**
	 * Gets a String object from ResourceBundle for coresponding key
	 */
	private static String getString(ResourceBundle rb, String key) {
		return rb.getString(key).trim();
	}
	/**
	 * Gets a byte from ResourceBundle for coresponding key
	 */
	private static byte getByte(ResourceBundle rb, String key) {
		return Byte.parseByte(rb.getString(key).trim().substring(2), 16);
	}
	/**
	 * Gets an int from ResoutceBundle for coresponding key
	 */
	private static int getInt(ResourceBundle rb, String key) {
		return Integer.parseInt(rb.getString(key).trim());
	}
	private static long getLong(ResourceBundle rb, String key) {
		return Long.parseLong(rb.getString(key).trim());
	}
	/**
	 * Builds a timeout for used connections blocked in application
	 */
	private static long getTimeOut(ResourceBundle rb, String key) {
		return Integer.parseInt(rb.getString(key).trim()) * 60 * 1000;
	}
	/**
	 * Gets a double value from ResourceBundle for coresponding key
	 */
	private static double getDouble(ResourceBundle rb, String key) {
		return Double.parseDouble(rb.getString(key).trim());
	}
	/**
	 * @return
	 */
}
