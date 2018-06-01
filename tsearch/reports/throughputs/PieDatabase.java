package ro.cst.tsearch.reports.throughputs;

import java.math.BigDecimal;
import java.util.HashMap;

import org.apache.log4j.Logger;

import com.mchange.v2.c3p0.impl.NewProxyCallableStatement;

import ro.cst.tsearch.database.ConnectionPool;
import ro.cst.tsearch.database.DBConnection;
import ro.cst.tsearch.database.DatabaseData;
import ro.cst.tsearch.exceptions.BaseException;
import ro.cst.tsearch.generic.Util;
import ro.cst.tsearch.utils.StringUtils;

public class PieDatabase {
	
	private static final Logger logger = Logger.getLogger(PieDatabase.class);
	
	public static HashMap<Long, Long> getGroupDataGeneral(String type, 
			int[] countyId, int[] abstractorId, int[] agentId, 
            int[] stateId, String[] compName, int productId){
		
		String sql = "call getGroupDataGeneral(?, ?, ?, ?, ?, ?, ?)";
		
		DBConnection conn = null;
		
		try {
			conn = ConnectionPool.getInstance().requestConnection();
			
			NewProxyCallableStatement call = (NewProxyCallableStatement)conn.prepareCall(sql);
			
			call.setInt(1, (type.equalsIgnoreCase("incomeBean")?1:0));
			call.setString(2, "," + Util.getStringFromArray(countyId) + ",");
			call.setString(3, "," + Util.getStringFromArray(abstractorId) + ",");
			call.setString(4, "," + Util.getStringFromArray(agentId) + ",");
			call.setString(5, "," + Util.getStringFromArray(stateId) + ",");
			call.setString(6, StringUtils.convertStringToHexString("," + Util.getStringFromStringArray(compName) + ","));
			call.setInt(7, productId);
			
			long time  = System.currentTimeMillis();
			DatabaseData data = conn.executeCallableStatementWithResult(call);
			logger.debug("PieDatabase: getGroupDataGeneral " + (System.currentTimeMillis()-time) + " millis");
			
			//logger.debug("nr de randuri " + data.getRowNumber());
			return setGroupReportData(data); 
			
		} catch (Exception e){
			e.printStackTrace();
		}	finally{
            try{
                ConnectionPool.getInstance().releaseConnection(conn);
            }catch(BaseException e){
                e.printStackTrace();
            }           
        }
		
		return new HashMap<Long, Long>();
	}
	
	public static HashMap<Long, Long> getGroupDataAnnual(String type, int[] countyId, 
			int[] abstractorId, int[] agentId, 
            int[] stateId, String[] compName, int productId, String year){
		
		String sql = "call getGroupDataAnnual(?, ?, ?, ?, ?, ?, ?, ?)";
		
		DBConnection conn = null;
		
		try {
			conn = ConnectionPool.getInstance().requestConnection();
			
			NewProxyCallableStatement call = (NewProxyCallableStatement)conn.prepareCall(sql);
			
			call.setInt(1, (type.equalsIgnoreCase("incomeBean")?1:0));
			call.setString(2, "," + Util.getStringFromArray(countyId) + ",");
			call.setString(3, "," + Util.getStringFromArray(abstractorId) + ",");
			call.setString(4, "," + Util.getStringFromArray(agentId) + ",");
			call.setString(5, "," + Util.getStringFromArray(stateId) + ",");
			call.setString(6, StringUtils.convertStringToHexString("," + Util.getStringFromStringArray(compName) + ","));
			call.setInt(7, productId);
			call.setInt(8, Integer.parseInt(year));
			
			long time  = System.currentTimeMillis();
			DatabaseData data = conn.executeCallableStatementWithResult(call);
			logger.debug("PieDatabase: getGroupDataAnnual " + (System.currentTimeMillis()-time) + " millis");
			//logger.debug("nr de randuri " + data.getRowNumber());
			return setGroupReportData(data); 
			
		} catch (Exception e){
			e.printStackTrace();
		}	finally{
            try{
                ConnectionPool.getInstance().releaseConnection(conn);
            }catch(BaseException e){
                e.printStackTrace();
            }           
        }
		
		return new HashMap<Long, Long>();
	}
	
	public static HashMap<Long, Long> getGroupDataMonthly(String type, int[] countyId, 
			int[] abstractorId, int[] agentId, 
            int[] stateId, String[] compName, int productId, String year, String month){
		
		String sql = "call getGroupDataMonthly(?, ?, ?, ?, ?, ?, ?, ?, ?)";
		
		DBConnection conn = null;
		
		try {
			conn = ConnectionPool.getInstance().requestConnection();
			
			NewProxyCallableStatement call = (NewProxyCallableStatement)conn.prepareCall(sql);
			
			call.setInt(1, (type.equalsIgnoreCase("incomeBean")?1:0));
			call.setString(2, "," + Util.getStringFromArray(countyId) + ",");
			call.setString(3, "," + Util.getStringFromArray(abstractorId) + ",");
			call.setString(4, "," + Util.getStringFromArray(agentId) + ",");
			call.setString(5, "," + Util.getStringFromArray(stateId) + ",");
			call.setString(6, StringUtils.convertStringToHexString("," + Util.getStringFromStringArray(compName) + ","));
			call.setInt(7, productId);
			call.setInt(8, Integer.parseInt(year));
			call.setInt(9, Integer.parseInt(month));
			long time  = System.currentTimeMillis();
			DatabaseData data = conn.executeCallableStatementWithResult(call);
			logger.debug("PieDatabase: getGroupDataMonthly " + (System.currentTimeMillis()-time) + " millis");
			//logger.debug("nr de randuri " + data.getRowNumber());
			return setGroupReportData(data); 
			
		} catch (Exception e){
			e.printStackTrace();
		}	finally{
            try{
                ConnectionPool.getInstance().releaseConnection(conn);
            }catch(BaseException e){
                e.printStackTrace();
            }           
        }
		
		return new HashMap<Long, Long>();
	}
	
	public static HashMap<Long, Long> getCommunitiesDataGeneral(String type, int[] countyId, int[] abstractorId, int[] agentId, 
            int[] stateId, String[] compName, int productId, int groupId) {
		
		String sql = "call getCommunitiesDataGeneral(?, ?, ?, ?, ?, ?, ?, ?)";
		
		DBConnection conn = null;
		
		try {
			conn = ConnectionPool.getInstance().requestConnection();
			
			NewProxyCallableStatement call = (NewProxyCallableStatement)conn.prepareCall(sql);
			
			call.setInt(1, (type.equalsIgnoreCase("incomeBean")?1:0));
			call.setString(2, "," + Util.getStringFromArray(countyId) + ",");
			call.setString(3, "," + Util.getStringFromArray(abstractorId) + ",");
			call.setString(4, "," + Util.getStringFromArray(agentId) + ",");
			call.setString(5, "," + Util.getStringFromArray(stateId) + ",");
			call.setString(6, StringUtils.convertStringToHexString("," + Util.getStringFromStringArray(compName) + ","));
			call.setInt(7, productId);
			call.setInt(8, groupId);
			
			long time  = System.currentTimeMillis();
			DatabaseData data = conn.executeCallableStatementWithResult(call);

			logger.debug("PieDatabase: getCommunitiesDataGeneral " + (System.currentTimeMillis()-time) + " millis");
			//logger.debug("nr de randuri in getCommunitiesDataGeneral:" + data.getRowNumber());
			return setGroupReportData(data); 
			
		} catch (Exception e){
			e.printStackTrace();
		}	finally{
            try{
                ConnectionPool.getInstance().releaseConnection(conn);
            }catch(BaseException e){
                e.printStackTrace();
            }           
        }
		
		return new HashMap<Long, Long>();
	}
	
	public static HashMap<Long, Long> getCommunitiesDataAnnual(String type, int[] countyId, int[] abstractorId, int[] agentId, 
            int[] stateId, String[] compName, int productId, int groupId, String year) {
		
		String sql = "call getCommunitiesDataAnnual(?, ?, ?, ?, ?, ?, ?, ?, ?)";
		
		DBConnection conn = null;
		
		try {
			conn = ConnectionPool.getInstance().requestConnection();
			
			NewProxyCallableStatement call = (NewProxyCallableStatement)conn.prepareCall(sql);
			
			call.setInt(1, (type.equalsIgnoreCase("incomeBean")?1:0));
			call.setString(2, "," + Util.getStringFromArray(countyId) + ",");
			call.setString(3, "," + Util.getStringFromArray(abstractorId) + ",");
			call.setString(4, "," + Util.getStringFromArray(agentId) + ",");
			call.setString(5, "," + Util.getStringFromArray(stateId) + ",");
			call.setString(6, StringUtils.convertStringToHexString("," + Util.getStringFromStringArray(compName) + ","));
			call.setInt(7, productId);
			call.setInt(8, groupId);
			call.setInt(9, Integer.parseInt(year));
			
			long time  = System.currentTimeMillis();
			DatabaseData data = conn.executeCallableStatementWithResult(call);

			logger.debug("PieDatabase: getCommunitiesDataAnnual " + (System.currentTimeMillis()-time) + " millis");
			//logger.debug("nr de randuri in getCommunitiesDataAnnual:" + data.getRowNumber());
			return setGroupReportData(data); 
			
		} catch (Exception e){
			e.printStackTrace();
		}	finally{
            try{
                ConnectionPool.getInstance().releaseConnection(conn);
            }catch(BaseException e){
                e.printStackTrace();
            }           
        }
		
		return new HashMap<Long, Long>();
	}
	
	public static HashMap<Long, Long> getCommunitiesDataMonthly(String type, int[] countyId, int[] abstractorId, int[] agentId, 
            int[] stateId, String[] compName, int productId, int groupId, String year, String month) {
		
		String sql = "call getCommunitiesDataMonthly(?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
		
		DBConnection conn = null;
		
		try {
			conn = ConnectionPool.getInstance().requestConnection();
			
			NewProxyCallableStatement call = (NewProxyCallableStatement)conn.prepareCall(sql);
			
			call.setInt(1, (type.equalsIgnoreCase("incomeBean")?1:0));
			call.setString(2, "," + Util.getStringFromArray(countyId) + ",");
			call.setString(3, "," + Util.getStringFromArray(abstractorId) + ",");
			call.setString(4, "," + Util.getStringFromArray(agentId) + ",");
			call.setString(5, "," + Util.getStringFromArray(stateId) + ",");
			call.setString(6, StringUtils.convertStringToHexString("," + Util.getStringFromStringArray(compName) + ","));
			call.setInt(7, productId);
			call.setInt(8, groupId);
			call.setInt(9, Integer.parseInt(year));
			call.setInt(10, Integer.parseInt(month));
			
			long time  = System.currentTimeMillis();
			DatabaseData data = conn.executeCallableStatementWithResult(call);

			logger.debug("PieDatabase: getCommunitiesDataMonthly " + (System.currentTimeMillis()-time) + " millis");
			//logger.debug("nr de randuri in getCommunitiesDataMonthly:" + data.getRowNumber());
			return setGroupReportData(data); 
			
		} catch (Exception e){
			e.printStackTrace();
		}	finally{
            try{
                ConnectionPool.getInstance().releaseConnection(conn);
            }catch(BaseException e){
                e.printStackTrace();
            }           
        }
		
		return new HashMap<Long, Long>();
	}
	
	public static HashMap<Long, Long> getStatesDataGeneral(String type, int[] stateId, int[] countyId, int[] abstractorId, int[] agentId, 
			String[] compName, int productId, int groupId, int commId, int tsAdmin) {
		
		String sql = "call getStatesDataGeneral(?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
		
		DBConnection conn = null;
		
		try {
			conn = ConnectionPool.getInstance().requestConnection();
			
			NewProxyCallableStatement call = (NewProxyCallableStatement)conn.prepareCall(sql);
			
			call.setInt(1, (type.equalsIgnoreCase("incomeBean")?1:0));
			call.setString(2, "," + Util.getStringFromArray(stateId) + ",");
			call.setString(3, "," + Util.getStringFromArray(countyId) + ",");
			call.setString(4, "," + Util.getStringFromArray(abstractorId) + ",");
			call.setString(5, "," + Util.getStringFromArray(agentId) + ",");
			call.setString(6, StringUtils.convertStringToHexString("," + Util.getStringFromStringArray(compName) + ","));
			call.setInt(7, productId);
			call.setInt(8, groupId);
			call.setInt(9, commId);
			call.setInt(10, tsAdmin);
			
			
			long time  = System.currentTimeMillis();
			DatabaseData data = conn.executeCallableStatementWithResult(call);
			logger.debug("PieDatabase: getStatesDataGeneral " + (System.currentTimeMillis()-time) + " millis");
			//logger.debug("nr de randuri in getStatesDataTSAdmin:" + data.getRowNumber());
			return setGroupReportData(data); 
			
		} catch (Exception e){
			e.printStackTrace();
		}	finally{
            try{
                ConnectionPool.getInstance().releaseConnection(conn);
            }catch(BaseException e){
                e.printStackTrace();
            }           
        }
		
		return new HashMap<Long, Long>();
	}
	
	public static HashMap<Long, Long> getStatesDataAnnual(String type, int[] stateId, int[] countyId, int[] abstractorId, int[] agentId, 
			String[] compName, int productId, int groupId, int commId, int tsAdmin, String year) {
		
		String sql = "call getStatesDataAnnual(?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
		
		DBConnection conn = null;
		
		try {
			conn = ConnectionPool.getInstance().requestConnection();
			
			NewProxyCallableStatement call = (NewProxyCallableStatement)conn.prepareCall(sql);
			
			call.setInt(1, (type.equalsIgnoreCase("incomeBean")?1:0));
			call.setString(2, "," + Util.getStringFromArray(stateId) + ",");
			call.setString(3, "," + Util.getStringFromArray(countyId) + ",");
			call.setString(4, "," + Util.getStringFromArray(abstractorId) + ",");
			call.setString(5, "," + Util.getStringFromArray(agentId) + ",");
			call.setString(6, StringUtils.convertStringToHexString("," + Util.getStringFromStringArray(compName) + ","));
			call.setInt(7, productId);
			call.setInt(8, groupId);
			call.setInt(9, commId);
			call.setInt(10, tsAdmin);
			call.setInt(11, Integer.parseInt(year));
			
			
			long time  = System.currentTimeMillis();
			DatabaseData data = conn.executeCallableStatementWithResult(call);
			logger.debug("PieDatabase: getStatesDataAnnual " + (System.currentTimeMillis()-time) + " millis");
			//logger.debug("nr de randuri in getStatesDataAnnual:" + data.getRowNumber());
			return setGroupReportData(data); 
			
		} catch (Exception e){
			e.printStackTrace();
		}	finally{
            try{
                ConnectionPool.getInstance().releaseConnection(conn);
            }catch(BaseException e){
                e.printStackTrace();
            }           
        }
		
		return new HashMap<Long, Long>();
	}
	
	public static HashMap<Long, Long> getStatesDataMonthly(String type, int[] stateId, int[] countyId, int[] abstractorId, int[] agentId, 
			String[] compName, int productId, int groupId, int commId, int tsAdmin, String year, String month) {
		
		String sql = "call getStatesDataMonthly(?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
		
		DBConnection conn = null;
		
		try {
			conn = ConnectionPool.getInstance().requestConnection();
			
			NewProxyCallableStatement call = (NewProxyCallableStatement)conn.prepareCall(sql);
			
			call.setInt(1, (type.equalsIgnoreCase("incomeBean")?1:0));
			call.setString(2, "," + Util.getStringFromArray(stateId) + ",");
			call.setString(3, "," + Util.getStringFromArray(countyId) + ",");
			call.setString(4, "," + Util.getStringFromArray(abstractorId) + ",");
			call.setString(5, "," + Util.getStringFromArray(agentId) + ",");
			call.setString(6, StringUtils.convertStringToHexString("," + Util.getStringFromStringArray(compName) + ","));
			call.setInt(7, productId);
			call.setInt(8, groupId);
			call.setInt(9, commId);
			call.setInt(10, tsAdmin);
			call.setInt(11, Integer.parseInt(year));
			call.setInt(12, Integer.parseInt(month));
			
			long time  = System.currentTimeMillis();
			DatabaseData data = conn.executeCallableStatementWithResult(call);
			logger.debug("PieDatabase: getStatesDataMonthly " + (System.currentTimeMillis()-time) + " millis");
			//logger.debug("nr de randuri in getStatesDataMonthly:" + data.getRowNumber());
			return setGroupReportData(data); 
			
		} catch (Exception e){
			e.printStackTrace();
		}	finally{
            try{
                ConnectionPool.getInstance().releaseConnection(conn);
            }catch(BaseException e){
                e.printStackTrace();
            }           
        }
		
		return new HashMap<Long, Long>();
	}		//end getStatesDataMonthly
	
	public static HashMap<Long, Long> getCountiesDataGeneral(String type, int[] stateId, int[] countyId, int[] abstractorId, int[] agentId, 
			String[] compName, int productId, int groupId, int commId, int isTSAdmin) {
		
		String sql = "call getCountiesDataGeneral(?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
		
		DBConnection conn = null;
		
		try {
			conn = ConnectionPool.getInstance().requestConnection();
			
			NewProxyCallableStatement call = (NewProxyCallableStatement)conn.prepareCall(sql);
			
			call.setInt(1, (type.equalsIgnoreCase("incomeBean")?1:0));
			call.setString(2, "," + Util.getStringFromArray(stateId) + ",");
			call.setString(3, "," + Util.getStringFromArray(countyId) + ",");
			call.setString(4, "," + Util.getStringFromArray(abstractorId) + ",");
			call.setString(5, "," + Util.getStringFromArray(agentId) + ",");
			call.setString(6, StringUtils.convertStringToHexString("," + Util.getStringFromStringArray(compName) + ","));
			call.setInt(7, productId);
			call.setInt(8, groupId);
			call.setInt(9, commId);
			call.setInt(10, isTSAdmin);
			
			long time  = System.currentTimeMillis();
			DatabaseData data = conn.executeCallableStatementWithResult(call);
			logger.debug("PieDatabase: getCountiesDataGeneral " + (System.currentTimeMillis()-time) + " millis");
			//logger.debug("nr de randuri in getCountiesDataGeneral:" + data.getRowNumber());
			return setGroupReportData(data); 
			
		}	catch (Exception e){
			e.printStackTrace();
		}	finally{
            try{
                ConnectionPool.getInstance().releaseConnection(conn);
            }	catch(BaseException e){
                e.printStackTrace();
            }           
        }
		
		return new HashMap<Long, Long>();
	}
	
	public static HashMap<Long, Long> getCountiesDataAnnual(String type, int[] stateId, int[] countyId, int[] abstractorId, int[] agentId, 
			String[] compName, int productId, int groupId, int commId, int isTSAdmin, String year) {
		
		String sql = "call getCountiesDataAnnual(?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
		
		DBConnection conn = null;
		
		try {
			conn = ConnectionPool.getInstance().requestConnection();
			
			NewProxyCallableStatement call = (NewProxyCallableStatement)conn.prepareCall(sql);
			
			call.setInt(1, (type.equalsIgnoreCase("incomeBean")?1:0));
			call.setString(2, "," + Util.getStringFromArray(stateId) + ",");
			call.setString(3, "," + Util.getStringFromArray(countyId) + ",");
			call.setString(4, "," + Util.getStringFromArray(abstractorId) + ",");
			call.setString(5, "," + Util.getStringFromArray(agentId) + ",");
			call.setString(6, StringUtils.convertStringToHexString("," + Util.getStringFromStringArray(compName) + ","));
			call.setInt(7, productId);
			call.setInt(8, groupId);
			call.setInt(9, commId);
			call.setInt(10, isTSAdmin);
			call.setInt(11, Integer.parseInt(year));
			
			long time  = System.currentTimeMillis();
			DatabaseData data = conn.executeCallableStatementWithResult(call);
			logger.debug("PieDatabase: getCountiesDataAnnual " + (System.currentTimeMillis()-time) + " millis");
			//logger.debug("nr de randuri in getCountiesDataAnnual:" + data.getRowNumber());
			return setGroupReportData(data); 
			
		}	catch (Exception e){
			e.printStackTrace();
		}	finally{
            try{
                ConnectionPool.getInstance().releaseConnection(conn);
            }	catch(BaseException e){
                e.printStackTrace();
            }           
        }
		
		return new HashMap<Long, Long>();
	}		//getCountiesDataAnnual
	
	
	public static HashMap<Long, Long> getCountiesDataMonthly(String type, int[] stateId, int[] countyId, int[] abstractorId, int[] agentId, 
			String[] compName, int productId, int groupId, int commId, int isTSAdmin, String year, String month) {
		
		String sql = "call getCountiesDataMonthly(?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
		
		DBConnection conn = null;
		
		try {
			conn = ConnectionPool.getInstance().requestConnection();
			
			NewProxyCallableStatement call = (NewProxyCallableStatement)conn.prepareCall(sql);
			
			call.setInt(1, (type.equalsIgnoreCase("incomeBean")?1:0));
			call.setString(2, "," + Util.getStringFromArray(stateId) + ",");
			call.setString(3, "," + Util.getStringFromArray(countyId) + ",");
			call.setString(4, "," + Util.getStringFromArray(abstractorId) + ",");
			call.setString(5, "," + Util.getStringFromArray(agentId) + ",");
			call.setString(6, StringUtils.convertStringToHexString("," + Util.getStringFromStringArray(compName) + ","));
			call.setInt(7, productId);
			call.setInt(8, groupId);
			call.setInt(9, commId);
			call.setInt(10, isTSAdmin);
			call.setInt(11, Integer.parseInt(year));
			call.setInt(12, Integer.parseInt(month));
			
			long time  = System.currentTimeMillis();
			DatabaseData data = conn.executeCallableStatementWithResult(call);
			logger.debug("PieDatabase: getCountiesDataMonthly " + (System.currentTimeMillis()-time) + " millis");
			//logger.debug("nr de randuri in getCountiesDataMonthly:" + data.getRowNumber());
			return setGroupReportData(data); 
			
		}	catch (Exception e){
			e.printStackTrace();
		}	finally{
            try{
                ConnectionPool.getInstance().releaseConnection(conn);
            }	catch(BaseException e){
                e.printStackTrace();
            }           
        }
		
		return new HashMap<Long, Long>();
	}		//getCountiesDataMonthly
	
	public static HashMap<Long, Long> getAbstractorsDataGeneral(String type, int[] stateId, 
			int[] countyId, int[] abstractorId, int[] agentId, String[] compName,  
            int payrateType, int productId, int groupId, int commId) {
		
		String sql = "call getAbstractorsDataGeneral(?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
		
		DBConnection conn = null;
		
		try {
			conn = ConnectionPool.getInstance().requestConnection();
			
			NewProxyCallableStatement call = (NewProxyCallableStatement)conn.prepareCall(sql);
			
			call.setInt(1, (type.equalsIgnoreCase("incomeBean")?1:0));
			call.setString(2, "," + Util.getStringFromArray(stateId) + ",");
			call.setString(3, "," + Util.getStringFromArray(countyId) + ",");
			call.setString(4, "," + Util.getStringFromArray(abstractorId) + ",");
			call.setString(5, "," + Util.getStringFromArray(agentId) + ",");
			call.setString(6, StringUtils.convertStringToHexString("," + Util.getStringFromStringArray(compName) + ","));
			call.setInt(7, payrateType);
			call.setInt(8, productId);
			call.setInt(9, groupId);
			call.setInt(10, commId);
			
			long time  = System.currentTimeMillis();
			DatabaseData data = conn.executeCallableStatementWithResult(call);
			logger.debug("PieDatabase: getAbstractorsDataGeneral " + (System.currentTimeMillis()-time) + " millis");
			//logger.debug("nr de randuri in getAbstractorDataGeneral:" + data.getRowNumber());
			return setGroupReportData(data); 
			
		}	catch (Exception e){
			e.printStackTrace();
		}	finally{
            try{
                ConnectionPool.getInstance().releaseConnection(conn);
            }	catch(BaseException e){
                e.printStackTrace();
            }           
        }
		
		return new HashMap<Long, Long>();
	}		//getAbstractorDataGeneral
		
	public static HashMap<Long, Long> getAbstractorsDataAnnual(String type, int[] stateId, 
			int[] countyId, int[] abstractorId, int[] agentId, String[] compName,  
            int payrateType, int productId, int groupId, int commId, String year) {
		
		String sql = "call getAbstractorsDataAnnual(?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
		
		DBConnection conn = null;
		
		try {
			conn = ConnectionPool.getInstance().requestConnection();
			
			NewProxyCallableStatement call = (NewProxyCallableStatement)conn.prepareCall(sql);
			
			call.setInt(1, (type.equalsIgnoreCase("incomeBean")?1:0));
			call.setString(2, "," + Util.getStringFromArray(stateId) + ",");
			call.setString(3, "," + Util.getStringFromArray(countyId) + ",");
			call.setString(4, "," + Util.getStringFromArray(abstractorId) + ",");
			call.setString(5, "," + Util.getStringFromArray(agentId) + ",");
			call.setString(6, StringUtils.convertStringToHexString("," + Util.getStringFromStringArray(compName) + ","));
			call.setInt(7, payrateType);
			call.setInt(8, productId);
			call.setInt(9, groupId);
			call.setInt(10, commId);
			call.setInt(11, Integer.parseInt(year));
			
			long time  = System.currentTimeMillis();
			DatabaseData data = conn.executeCallableStatementWithResult(call);
			logger.debug("PieDatabase: getAbstractorsDataAnnual " + (System.currentTimeMillis()-time) + " millis");
			//logger.debug("nr de randuri in getAbstractorDataAnnual:" + data.getRowNumber());
			return setGroupReportData(data); 
			
		}	catch (Exception e){
			e.printStackTrace();
		}	finally{
            try{
                ConnectionPool.getInstance().releaseConnection(conn);
            }	catch(BaseException e){
                e.printStackTrace();
            }           
        }
		
		return new HashMap<Long, Long>();
	}		//getAbstractorDataAnnual
	
	
	public static HashMap<Long, Long> getAbstractorsDataMonthly(String type, int[] stateId, 
			int[] countyId, int[] abstractorId, int[] agentId, String[] compName,  
            int payrateType, int productId, int groupId, int commId, String year, String month) {
		
		String sql = "call getAbstractorsDataMonthly(?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
		
		DBConnection conn = null;
		
		try {
			conn = ConnectionPool.getInstance().requestConnection();
			
			NewProxyCallableStatement call = (NewProxyCallableStatement)conn.prepareCall(sql);
			
			call.setInt(1, (type.equalsIgnoreCase("incomeBean")?1:0));
			call.setString(2, "," + Util.getStringFromArray(stateId) + ",");
			call.setString(3, "," + Util.getStringFromArray(countyId) + ",");
			call.setString(4, "," + Util.getStringFromArray(abstractorId) + ",");
			call.setString(5, "," + Util.getStringFromArray(agentId) + ",");
			call.setString(6, StringUtils.convertStringToHexString("," + Util.getStringFromStringArray(compName) + ","));
			call.setInt(7, payrateType);
			call.setInt(8, productId);
			call.setInt(9, groupId);
			call.setInt(10, commId);
			call.setInt(11, Integer.parseInt(year));
			call.setInt(12, Integer.parseInt(month));
			
			long time  = System.currentTimeMillis();
			DatabaseData data = conn.executeCallableStatementWithResult(call);
			logger.debug("PieDatabase: getAbstractorsDataMonthly " + (System.currentTimeMillis()-time) + " millis");
			//logger.debug("nr de randuri in getAbstractorDataMonthly:" + data.getRowNumber());
			return setGroupReportData(data); 
			
		}	catch (Exception e){
			e.printStackTrace();
		}	finally{
            try{
                ConnectionPool.getInstance().releaseConnection(conn);
            }	catch(BaseException e){
                e.printStackTrace();
            }           
        }
		
		return new HashMap<Long, Long>();
	}		//getAbstractorDataMonthly
	
	public static HashMap<Long, Long> getAgentsDataGeneral(String type, int[] stateId, int[] countyId, int[] abstractorId, int[] agentId,  
			String[] compName, int payrateType, int productId, int groupId, int commId) {
		
		String sql = "call getAgentsDataGeneral(?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
		
		DBConnection conn = null;
		
		try {
			conn = ConnectionPool.getInstance().requestConnection();
			
			NewProxyCallableStatement call = (NewProxyCallableStatement)conn.prepareCall(sql);
			
			call.setInt(1, (type.equalsIgnoreCase("incomeBean")?1:0));
			call.setString(2, "," + Util.getStringFromArray(stateId) + ",");
			call.setString(3, "," + Util.getStringFromArray(countyId) + ",");
			call.setString(4, "," + Util.getStringFromArray(abstractorId) + ",");
			call.setString(5, "," + Util.getStringFromArray(agentId) + ",");
			call.setString(6, StringUtils.convertStringToHexString("," + Util.getStringFromStringArray(compName) + ","));
			call.setInt(7, payrateType);
			call.setInt(8, productId);
			call.setInt(9, groupId);
			call.setInt(10, commId);
			
			long time  = System.currentTimeMillis();
			DatabaseData data = conn.executeCallableStatementWithResult(call);
			logger.debug("PieDatabase: getAgentsDataGeneral " + (System.currentTimeMillis()-time) + " millis");
			//logger.debug("nr de randuri in getAbstractorDataTSAdmin:" + data.getRowNumber());
			return setGroupReportData(data); 
			
		}	catch (Exception e){
			e.printStackTrace();
		}	finally{
            try{
                ConnectionPool.getInstance().releaseConnection(conn);
            }	catch(BaseException e){
                e.printStackTrace();
            }           
        }
		
		return new HashMap<Long, Long>();
	}
	
	public static HashMap<Long, Long> getAgentsDataAnnual(String type, int[] stateId, int[] countyId, int[] abstractorId, int[] agentId, 
			String[] compName, int payrateType, int productId, int groupId, int commId, String year) {
		
		String sql = "call getAgentsDataAnnual(?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
		
		DBConnection conn = null;
		
		try {
			conn = ConnectionPool.getInstance().requestConnection();
			
			NewProxyCallableStatement call = (NewProxyCallableStatement)conn.prepareCall(sql);
			
			call.setInt(1, (type.equalsIgnoreCase("incomeBean")?1:0));
			call.setString(2, "," + Util.getStringFromArray(stateId) + ",");
			call.setString(3, "," + Util.getStringFromArray(countyId) + ",");
			call.setString(4, "," + Util.getStringFromArray(abstractorId) + ",");
			call.setString(5, "," + Util.getStringFromArray(agentId) + ",");
			call.setString(6, StringUtils.convertStringToHexString("," + Util.getStringFromStringArray(compName) + ","));
			call.setInt(7, payrateType);
			call.setInt(8, productId);
			call.setInt(9, groupId);
			call.setInt(10, commId);
			call.setInt(11, Integer.parseInt(year));
			
			long time  = System.currentTimeMillis();
			DatabaseData data = conn.executeCallableStatementWithResult(call);
			logger.debug("PieDatabase: getAgentsDataAnnual " + (System.currentTimeMillis()-time) + " millis");
			//logger.debug("nr de randuri in getAbstractorDataAnnual:" + data.getRowNumber());
			return setGroupReportData(data); 
			
		}	catch (Exception e){
			e.printStackTrace();
		}	finally{
            try{
                ConnectionPool.getInstance().releaseConnection(conn);
            }	catch(BaseException e){
                e.printStackTrace();
            }           
        }
		
		return new HashMap<Long, Long>();
	}		//getAbstractorDataAnnual
	
	
	
	public static HashMap<Long, Long> getAgentsDataMonthly(String type, int[] stateId, int[] countyId, int[] abstractorId, int[] agentId, 
			String[] compName, int payrateType, int productId, int groupId, int commId, String year, String month) {
		
		String sql = "call getAgentsDataMonthly(?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
		
		DBConnection conn = null;
		
		try {
			conn = ConnectionPool.getInstance().requestConnection();
			
			NewProxyCallableStatement call = (NewProxyCallableStatement)conn.prepareCall(sql);
			
			call.setInt(1, (type.equalsIgnoreCase("incomeBean")?1:0));
			call.setString(2, "," + Util.getStringFromArray(stateId) + ",");
			call.setString(3, "," + Util.getStringFromArray(countyId) + ",");
			call.setString(4, "," + Util.getStringFromArray(abstractorId) + ",");
			call.setString(5, "," + Util.getStringFromArray(agentId) + ",");
			call.setString(6, StringUtils.convertStringToHexString("," + Util.getStringFromStringArray(compName) + ","));
			call.setInt(7, payrateType);
			call.setInt(8, productId);
			call.setInt(9, groupId);
			call.setInt(10, commId);
			call.setInt(11, Integer.parseInt(year));
			call.setInt(12, Integer.parseInt(month));
			
			long time  = System.currentTimeMillis();
			DatabaseData data = conn.executeCallableStatementWithResult(call);
			logger.debug("PieDatabase: getAgentsDataMonthly " + (System.currentTimeMillis()-time) + " millis");
			//logger.debug("nr de randuri in getAbstractorDataMonthly:" + data.getRowNumber());
			return setGroupReportData(data); 
			
		}	catch (Exception e){
			e.printStackTrace();
		}	finally{
            try{
                ConnectionPool.getInstance().releaseConnection(conn);
            }	catch(BaseException e){
                e.printStackTrace();
            }           
        }
		
		return new HashMap<Long, Long>();
	}		//getAbstractorDataMonthly
	
	
	
	public static HashMap<Long, Long> getProductsDataGeneral(String type, int[] stateId, int[] countyId, int[] abstractorId, int[] agentId, 
			String[] compName, int payrateType, int groupId, int commId, int productId) {
		
		String sql = "call getProductsDataGeneral(?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
		
		DBConnection conn = null;
		
		try {
			conn = ConnectionPool.getInstance().requestConnection();
			
			NewProxyCallableStatement call = (NewProxyCallableStatement)conn.prepareCall(sql);
			
			call.setInt(1, (type.equalsIgnoreCase("incomeBean")?1:0));
			call.setString(2, "," + Util.getStringFromArray(stateId) + ",");
			call.setString(3, "," + Util.getStringFromArray(countyId) + ",");
			call.setString(4, "," + Util.getStringFromArray(abstractorId) + ",");
			call.setString(5, "," + Util.getStringFromArray(agentId) + ",");
			call.setString(6, StringUtils.convertStringToHexString("," + Util.getStringFromStringArray(compName) + ","));
			call.setInt(7, payrateType);
			call.setInt(8, groupId);
			call.setInt(9, commId);
			call.setInt(10, productId);
			
			long time  = System.currentTimeMillis();
			DatabaseData data = conn.executeCallableStatementWithResult(call);
			logger.debug("PieDatabase: getProductsDataGeneral " + (System.currentTimeMillis()-time) + " millis");
			//logger.debug("nr de randuri in getProductsDataTSAdmin:" + data.getRowNumber());
			return setGroupReportData(data); 
			
		} catch (Exception e){
			e.printStackTrace();
		}	finally{
            try{
                ConnectionPool.getInstance().releaseConnection(conn);
            }catch(BaseException e){
                e.printStackTrace();
            }           
        }
		
		return new HashMap<Long, Long>();
	}
	
	public static HashMap<Long, Long> getProductsDataAnnual(String type, int[] stateId, int[] countyId, int[] abstractorId, int[] agentId, 
			String[] compName, int payrateType, int groupId, int commId, String year, int productType) {
		
		String sql = "call getProductsDataAnnual(?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
		
		DBConnection conn = null;
		
		try {
			conn = ConnectionPool.getInstance().requestConnection();
			
			NewProxyCallableStatement call = (NewProxyCallableStatement)conn.prepareCall(sql);
			
			call.setInt(1, (type.equalsIgnoreCase("incomeBean")?1:0));
			call.setString(2, "," + Util.getStringFromArray(stateId) + ",");
			call.setString(3, "," + Util.getStringFromArray(countyId) + ",");
			call.setString(4, "," + Util.getStringFromArray(abstractorId) + ",");
			call.setString(5, "," + Util.getStringFromArray(agentId) + ",");
			call.setString(6, StringUtils.convertStringToHexString("," + Util.getStringFromStringArray(compName) + ","));
			call.setInt(7, payrateType);
			call.setInt(8, groupId);
			call.setInt(9, commId);
			call.setInt(10, Integer.parseInt(year));
			call.setInt(11, productType);
			
			long time  = System.currentTimeMillis();
			DatabaseData data = conn.executeCallableStatementWithResult(call);
			logger.debug("PieDatabase: getProductsDataAnnual " + (System.currentTimeMillis()-time) + " millis");
			//logger.debug("nr de randuri in getProductsDataTSAdmin:" + data.getRowNumber());
			return setGroupReportData(data); 
			
		} catch (Exception e){
			e.printStackTrace();
		}	finally{
            try{
                ConnectionPool.getInstance().releaseConnection(conn);
            }catch(BaseException e){
                e.printStackTrace();
            }           
        }
		
		return new HashMap<Long, Long>();
	}
	
	public static HashMap<Long, Long> getProductsDataMonthly(String type, int[] stateId, int[] countyId, int[] abstractorId, int[] agentId, 
			String[] compName, int payrateType, int groupId, int commId, String year, String month, int productType) {
		
		String sql = "call getProductsDataMonthly(?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
		
		DBConnection conn = null;
		
		try {
			conn = ConnectionPool.getInstance().requestConnection();
			
			NewProxyCallableStatement call = (NewProxyCallableStatement)conn.prepareCall(sql);
			
			call.setInt(1, (type.equalsIgnoreCase("incomeBean")?1:0));
			call.setString(2, "," + Util.getStringFromArray(stateId) + ",");
			call.setString(3, "," + Util.getStringFromArray(countyId) + ",");
			call.setString(4, "," + Util.getStringFromArray(abstractorId) + ",");
			call.setString(5, "," + Util.getStringFromArray(agentId) + ",");
			call.setString(6, StringUtils.convertStringToHexString("," + Util.getStringFromStringArray(compName) + ","));
			call.setInt(7, payrateType);
			call.setInt(8, groupId);
			call.setInt(9, commId);
			call.setInt(10, Integer.parseInt(year));
			call.setInt(11, Integer.parseInt(month));
			call.setInt(12, productType);
			
			long time  = System.currentTimeMillis();
			DatabaseData data = conn.executeCallableStatementWithResult(call);
			logger.debug("PieDatabase: getProductsDataMonthly " + (System.currentTimeMillis()-time) + " millis");
			//logger.debug("nr de randuri in getProductsDataTSAdmin:" + data.getRowNumber());
			return setGroupReportData(data); 
			
		} catch (Exception e){
			e.printStackTrace();
		}	finally{
            try{
                ConnectionPool.getInstance().releaseConnection(conn);
            }catch(BaseException e){
                e.printStackTrace();
            }           
        }
		
		return new HashMap<Long, Long>();
	}
	
	private static HashMap<Long, Long> setGroupReportData(DatabaseData data) {
		HashMap<Long, Long> result = new HashMap<Long, Long>();
		try {
			int resultLenth = data.getRowNumber();
			if(resultLenth > 0){
				for (int i = 0; i < resultLenth; i++) {
					//logger.debug(data.getValue(2, i));
					BigDecimal noDone = null;
					if(data.getValue(2,i)!=null)
						noDone = new BigDecimal(data.getValue(2,i).toString());
					else 
						noDone = new BigDecimal(0);
					
					Long grId = Long.parseLong(data.getValue(3,i).toString());

					//logger.debug("noDone " + noDone);
					//logger.debug("grId " + grId);
					if(result.get(grId)==null){
						result.put(grId, new Long(0));
					}
					//add the new value to the value corresponding to groupId
					result.put(grId,result.get(grId)+noDone.longValue());
				}
			}
		}
		catch(Exception e){
			e.printStackTrace();
		}
		return result;
	}
}


