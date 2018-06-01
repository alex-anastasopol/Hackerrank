/**
 * Expense tracking utility
 */
package ro.cst.tsearch.utils;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Vector;

import org.apache.log4j.Logger;
import org.springframework.jdbc.core.simple.ParameterizedRowMapper;

import ro.cst.tsearch.database.DBManager;

import static ro.cst.tsearch.database.DBManager.*;
import static ro.cst.tsearch.utils.DBConstants.*;

/**
 * @author radu bacrau
 */
public class ExpenseTracking {
	
	private static final Logger logger = Logger.getLogger(ExpenseTracking.class);
	 
	/**
	 * prevent instantiation
	 */
	private ExpenseTracking(){}
	
    /**
     * Immutable class holding an expense statistics
     * @author radu bacrau
     */    
    public static class ExpenseStatistics{
    	
    	public final String atsInstance;
    	public final int expenseCount;
    	public final float expenseTotal;
    	
    	public ExpenseStatistics(String atsInstance, int expenseCount, float expenseTotal){
    		this.atsInstance = atsInstance;
    		this.expenseCount = expenseCount;
    		this.expenseTotal = expenseTotal;    		
    	}
    	
    	public String toString(){
    		return "ExpenseStatistics(atsInstance=" + atsInstance + ", expenseCount=" + expenseCount + ", expenseTotal=" + expenseTotal + ")";
    	}
    }
    
    /**
     * Compute expense statistics for each ats instance 
     * @return
     */
    public static List<ExpenseStatistics> getStatistics(){
    	
    	ParameterizedRowMapper<ExpenseStatistics> mapper = new ParameterizedRowMapper<ExpenseStatistics>() {		    
	        public ExpenseStatistics mapRow(ResultSet rs, int rowNum) throws SQLException {
	        	String atsInstance = rs.getString("ats_instance");	        	
	        	int count = rs.getInt("cnt");
	        	float totalPrice = rs.getFloat("prc");	        	
	        	return new ExpenseStatistics(atsInstance, count, totalPrice);
	        }
	    };	
	    
	    String sql =  "select ats_instance, count(*) as cnt, sum(price) as prc FROM " + TABLE_EXPENSES + " GROUP by ats_instance ORDER BY ats_instance";
	    
	    return getSimpleTemplate().query(sql, mapper);
	    
    }
    
    /**
     * Immutable class holding an expense info
     * @author radu bacrau
     */
    public static class Expense{
    	
    	public final String atsInstance;
    	public final String targetServer;
    	public final String action;
    	public final String timeStamp;
    	public final long searchId;
    	public final float price;

    	public Expense(String atsInstance, String targetServer, String action, String timeStamp, long searchId, float price){
    		this.atsInstance = atsInstance;
    		this.targetServer = targetServer;
    		this.action = action;
    		this.timeStamp = timeStamp;
    		this.searchId = searchId;
    		this.price = price;
    	}
    	
    	public String toString(){
    		return "Expense(atsInstance=" + atsInstance +", targetServer=" + targetServer + ", action=" + action + ", timeStamp=" + timeStamp + ", price=" + price + ")";
    	}
    }
	
    /**
     * Record an expense into the DB.
     * @param targetServer
     * @param action
     * @param searchId
     * @param price
     * @return true if the recording was successful
     */
    public static boolean recordExpense(String targetServer, String action, long searchId, float price){
		String atsInstance = System.getProperty("tsearch_inst", "local");
		String timeStamp = getMysqlTimeStamp();
		return recordExpense(new Expense(atsInstance, targetServer, action, timeStamp, searchId, price));	
    }
    
    /**
     * Record an expense into the DB. 
     * @param expense
     * @return true if the recording was successful
     */
    private static boolean recordExpense(Expense expense){
    	try{
    		// trim the action to the maximum supported by DB
    		String action = expense.action;
        	if(action.length() > 200){
        		action = action.substring(0, 195) + "...";
        	}
        	// construct SQL query
    		String sql = "INSERT INTO " + TABLE_EXPENSES + " (ats_instance,target_server,action,price,search_id,timestamp) VALUES (" +
    					"?, " +
    					"?, " +
    					"?, " + 
    					"?, " +
    					"?, " +
    				    expense.timeStamp + ")";

    		boolean retVal = (1 == getSimpleTemplate().update(sql,expense.atsInstance,expense.targetServer,action,expense.price,expense.searchId));
    		logger.info("Logged expense: " + expense + " Succeeded = " + retVal);
    		return retVal;
    	}catch(RuntimeException e){
    		logger.error("Cannot record expense: " + expense, e);
    		return false;
    	}
    }
   
    /**
     * Return the number of different values for a field
     * @param field
     * @return
     */
    public static List<String> getExpenseFieldValues(final String field){
    	ParameterizedRowMapper<String> mapper = new ParameterizedRowMapper<String>() {		    
	        public String mapRow(ResultSet rs, int rowNum) throws SQLException {
	        	return rs.getString(field);
	        }
	    };		
    	String sql = "SELECT DISTINCT " + DBManager.sqlColumnName(field) + " FROM " + TABLE_EXPENSES;
	    return getSimpleTemplate().query(sql, mapper);    	
    }
    
    /**
     * Compute number of expenses satisfying the criteria
     * @param atsInstance
     * @param targetServer
     * @param start
     * @param length
     * @return
     */
    public static int getExpensesCount(String atsInstance, String targetServer){
    	
    	Vector<String> queryParams = new Vector<String>();
    	
    	String sql = "SELECT count(*) from " + TABLE_EXPENSES;
    	String condition = "";
    	if(atsInstance != null && !"".equals(atsInstance)){
    		condition += " WHERE ats_instance=? ";
    		queryParams.add(atsInstance);
    	} 
    	if(targetServer != null && !"".equals(targetServer)){
    		if("".equals(condition)){
    			condition += " WHERE target_server=? ";
    			queryParams.add(targetServer);
    		} else {
    			condition += " AND target_server=? ";
    			queryParams.add(targetServer);
    		}
    	}   
    	
    	sql+=condition;
    		
    	return getSimpleTemplate().queryForInt(sql,queryParams.toArray());
    }
    
    /**
     * Return number of expenses satisfying the criteria
     * @param atsInstance
     * @param targetServer
     * @param start
     * @param length
     * @return
     */
    
    public static List<Expense> getExpenses(String atsInstance, String targetServer, String orderBy, String orderType, int start, int length){
    	
    	Vector<String> queryParams = new Vector<String>();
    	
    	String sql = "SELECT * from " + TABLE_EXPENSES;
    	String condition = "";
    	if(atsInstance != null && !"".equals(atsInstance)){
    		condition += " WHERE ats_instance=? ";
    		queryParams.add(atsInstance);
    	} 
    	if(targetServer != null && !"".equals(targetServer)){
    		if("".equals(condition)){
    			condition += " WHERE target_server=? ";
    			queryParams.add(targetServer);
    		} else {
    			condition += " AND target_server=? ";
    			queryParams.add(targetServer);
    		}
    	}
    	sql += condition;
    	if(orderBy != null && !"".equals(orderBy)){
    		if(orderType == null || "".equals(orderType)){
    			orderType = "ASC";    			
    		}
    		sql += " ORDER BY " + DBManager.sqlColumnName(orderBy) + " " + DBManager.sqlOrderType(orderType) + " ";
    	}
    	sql +=  " LIMIT " + (start-1) + "," + length;
    	
    	ParameterizedRowMapper<Expense> mapper = new ParameterizedRowMapper<Expense>() {		    
	        public Expense mapRow(ResultSet rs, int rowNum) throws SQLException {
	        	String atsInstance = rs.getString("ats_instance");
	        	String targetServer = rs.getString("target_server");
	        	String action = rs.getString("action");
	        	String timeStamp = rs.getString("timestamp");
	        	long searchId = rs.getInt("search_id");
	        	float price = rs.getFloat("price");	        	
	        	return new Expense(atsInstance, targetServer, action, timeStamp, searchId, price);
	        }
	    };		
	    
	    return getSimpleTemplate().query(sql, mapper,queryParams.toArray());
    }
    
    

}
