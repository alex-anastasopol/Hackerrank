/*
 * Created on Dec 2, 2003
 *
 * To change the template for this generated file go to
 * Window>Preferences>Java>Code Generation>Code and Comments
 */
package ro.cst.tsearch.settings;

import java.io.IOException;
import java.math.BigDecimal;

import org.apache.log4j.Category;

import ro.cst.tsearch.database.ConnectionPool;
import ro.cst.tsearch.database.DBConnection;
import ro.cst.tsearch.database.DatabaseData;
import ro.cst.tsearch.exceptions.BaseException;
import ro.cst.tsearch.user.UserAttributes;
import ro.cst.tsearch.utils.DBConstants;
import ro.cst.tsearch.utils.Log;

/**
 * @author nae
 *
 * To change the template for this generated type comment go to
 * Window>Preferences>Java>Code Generation>Code and Comments
 */
public class Settings {
	/**
	 * 
	 */
	protected static final Category logger= Category.getInstance(Settings.class.getName());
	
	public static final String ATTRIBUTE ="ATTRIBUTE"; 
	public static final String VALUE ="VALUE";
	public static final String MODULE = "MODULE";
	
	/*module name*/
	public static final String SETTINGS_MODULE = "SETTINGS";
	public static final String SEARCH_MODULE = "SEARCH";
	
	/*module attribute*/
	public static final String INVOICE_DETAILS = "INVOICE_DETAILS";
	public static final String LAST_COUNTY="LAST_COUNTY";
	public static final String LAST_STATE="LAST_STATE";
	public static final String DISTRIBUTION_TYPE="DISTRIBUTION_TYPE";
	public static final String DISTRIBUTION_MODE="DISTRIBUTION_MODE"; 
	
	
	 
	/* invoice details value */
	
	public static final int NO_VALUE = 0;
	public static final int NO_INVOICE_DETAILS = 0;	
	public static final int WITH_INVOICE_DETAILS=1;
	
	public static final int DISTRIBUTION_TIFF= 0;
	public static final int DISTRIBUTION_PDF = 1;
		 	
	public static void updateAttributes(String module, String attribute,int value, BigDecimal userId) 
		throws BaseException {
	    		
		String stm = "UPDATE " + DBConstants.TABLE_SETTINGS + " SET "
					  + VALUE + " = " + value
					  + " WHERE "
					  + ATTRIBUTE + "='" + attribute
					  + "' AND " 
					  + MODULE	+ "='" + module
					  + "' AND "
					  + UserAttributes.USER_ID + "=" + userId;

		DBConnection conn = null;
		try{
		    
		    conn = ConnectionPool.getInstance().requestConnection();
		    conn.executeSQL(stm);
		    conn.commit();
		    
		}catch(Exception e){
			logger.error("SQL: Error to execute update " + e.getMessage() );
		} finally{
			try{
			    ConnectionPool.getInstance().releaseConnection(conn);
			}catch(BaseException e){
			    throw new BaseException("SQLException:" + e.getMessage());
			}			
		}
	}

	public static void insertAttributes(String module, String attribute,int value, BigDecimal userId) throws
	BaseException{
		
		String stm = "INSERT INTO " + DBConstants.TABLE_SETTINGS + " ( "
					  + VALUE + ", " + ATTRIBUTE + ", " + MODULE  + ", " + UserAttributes.USER_ID   
					  + " ) VALUES ( "
					  + value + ", '" + attribute + "', '" +  module + "'," + userId + ")";
		
	    DBConnection conn = null;
		try{
		    
		    conn = ConnectionPool.getInstance().requestConnection();
		    conn.executeSQL(stm);
			
		}catch(Exception e){
			logger.error("SQL: Error to execute insert " + e.getMessage() );
		} finally{
			try{
			    ConnectionPool.getInstance().releaseConnection(conn);
			}catch(BaseException e){
			    throw new BaseException("SQLException:" + e.getMessage());
			}			
		}
	}

	public static int selectAttributes(String module, String attribute, BigDecimal userId) throws
	BaseException, IOException {
		
		String stm = "SELECT " + VALUE + " FROM " + DBConstants.TABLE_SETTINGS 					  
					  + " WHERE "
					  + ATTRIBUTE + "='" + attribute
					  + "' AND " 
					  + MODULE	+ "='" + module
					  + "' AND "
					  + UserAttributes.USER_ID + "=" + userId;		
		
		DBConnection conn = null;
		try{
		    
		    conn = ConnectionPool.getInstance().requestConnection();
			DatabaseData data = conn.executeSQL(stm);
			
			if(data.getRowNumber() >0) {
				Object o = data.getValue(1,0);
				
				if (o != null)
					return Integer.parseInt(o.toString());
				else
					return NO_VALUE;
			}
			else 
				return NO_VALUE; 
			
		}catch(Exception e){
			logger.error("SQL: Error to execute select " + e.getMessage() );
		} finally{
			try{
			    ConnectionPool.getInstance().releaseConnection(conn);
			}catch(BaseException e){
			    throw new BaseException("SQLException:" + e.getMessage());
			}			
		}
		
		return 0;
	}
	
	public static boolean hasAttributes(String module, String attribute,int value, BigDecimal userId) throws
	BaseException {
	    		
		String stm = "SELECT " + VALUE + " FROM " + DBConstants.TABLE_SETTINGS 					  
					  + " WHERE "
					  + ATTRIBUTE + "='" + attribute
					  + "' AND " 
					  + MODULE	+ "='" + module
					  + "' AND "
					  + UserAttributes.USER_ID + "=" + userId;
		
		DBConnection conn = null;
		try {
		    
		    conn = ConnectionPool.getInstance().requestConnection();
			DatabaseData data = conn.executeSQL(stm);			
			return (data.getRowNumber()>0);
			
		} catch(Exception e){
			logger.error("SQL: Error to execute select " + e.getMessage() );
		} finally {
			try{
			    ConnectionPool.getInstance().releaseConnection(conn);
			}catch(BaseException e){
			    throw new BaseException("SQLException:" + e.getMessage());
			}			
		}
		
		return false;
	}
	
	public static void manipulateAttribute(String module, String attribute,int value, BigDecimal userId) throws
	BaseException{
		if(hasAttributes(module,attribute, value, userId)){
			updateAttributes(module,attribute,value,userId);
		}else{
			insertAttributes(module,attribute,value, userId);
		}
	}	
	
	
}
