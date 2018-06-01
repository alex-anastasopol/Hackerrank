package ro.cst.tsearch.database;

import java.math.BigInteger;
import java.util.Date;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Vector;

import org.apache.log4j.Logger;

import ro.cst.tsearch.bean.InvoiceSolomonBean;
import ro.cst.tsearch.bean.InvoiceSolomonBeanEntry;
import ro.cst.tsearch.data.User;
import ro.cst.tsearch.database.rowmapper.NameMapper;
import ro.cst.tsearch.exceptions.BaseException;
import ro.cst.tsearch.generic.Util;
import ro.cst.tsearch.reports.data.DayReportLineData;
import ro.cst.tsearch.reports.data.InvoiceData;
import ro.cst.tsearch.reports.data.InvoiceXmlData;
import ro.cst.tsearch.reports.invoice.InvoicedSearch;
import ro.cst.tsearch.user.UserAttributes;
import ro.cst.tsearch.user.UserUtils;
import ro.cst.tsearch.utils.DBConstants;
import ro.cst.tsearch.utils.StringUtils;

import com.mchange.v2.c3p0.impl.NewProxyCallableStatement;

public class DBInvoice {
	
	private static final Logger logger = Logger.getLogger(DBInvoice.class);
	
    
    
    /**
     * @param countyId
     * @param abstractorId
     * @param agentId
     * @param stateId
     * @param fromDay
     * @param fromMonth
     * @param fromYear
     * @param toDay
     * @param toMonth
     * @param toYear
     * @param orderBy
     * @param orderType
     * @param commId
     * @return
     */
    public static DayReportLineData[] getIntervalInvoiceData(int[] countyId, int[] abstractorId, int[] agentId, int[] stateId, String[] compName, int fromDay, int fromMonth, int fromYear, int toDay, int toMonth, int toYear, String orderBy, String orderType, int commId, int isAdmin,UserAttributes ua){
        
        String sql = "call getInvoicePdfDataAllInOne(?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
        
        DBConnection conn = null;
        
        try {            
            conn = ConnectionPool.getInstance().requestConnection();
            NewProxyCallableStatement cs = (NewProxyCallableStatement)conn.prepareCall(sql);
            cs.setString(1, "," + Util.getStringFromArray(countyId) + ",");
            cs.setString(2, "," + Util.getStringFromArray(abstractorId) + ",");
            cs.setString(3, "," + Util.getStringFromArray(agentId) + ",");
            cs.setString(4, "," + Util.getStringFromArray(stateId) + ",");
            cs.setString(5, StringUtils.convertStringToHexString("," + Util.getStringFromStringArray(compName) + ","));
            cs.setInt(6, fromDay);
            cs.setInt(7, fromMonth);
            cs.setInt(8, fromYear);
            cs.setInt(9, toDay);
            cs.setInt(10, toMonth);
            cs.setInt(11, toYear);
            cs.setString(12, orderBy);
            cs.setString(13, orderType);
            cs.setInt(14, commId);
            cs.setInt(15, isAdmin);
            long startTime = System.currentTimeMillis();
            DatabaseData data = conn.executeCallableStatementWithResult(cs);
            logger.debug("getIntervalInvoiceData sql took about " + (System.currentTimeMillis() - startTime) + " millis");
            return DBReports.setDetailedReportData(data, true,ua, false, commId);
        } catch (Exception e) {
            logger.error(e.getMessage());
            e.printStackTrace(System.err);
            return new DayReportLineData[0];
        }
        finally{
            try{
                ConnectionPool.getInstance().releaseConnection(conn);
            }catch(BaseException e){
                logger.error(e);
            }           
        }
        
    }

    public static InvoiceXmlData[] getIntervalInvoiceXmlData(
    		int[] countyId, int[] abstractorId, int[] agentId, int[] stateId, String[] compName, 
    		int fromDay, int fromMonth, int fromYear, 
    		int toDay, int toMonth, int toYear, 
    		String orderBy, String orderType, int commId, int isTSAdmin){
        
        String sql = "call getIntervalInvoiceXmlData(?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";       
        DBConnection conn = null;
        
        try {
            
            conn = ConnectionPool.getInstance().requestConnection();
            
            NewProxyCallableStatement cs1 = (NewProxyCallableStatement)conn.prepareCall(sql);
            cs1.setString(1, "," + Util.getStringFromArray(countyId) + ",");
            cs1.setString(2, "," + Util.getStringFromArray(abstractorId) + ",");
            cs1.setString(3, "," + Util.getStringFromArray(agentId) + ",");
            cs1.setString(4, "," + Util.getStringFromArray(stateId) + ",");
            cs1.setString(5, StringUtils.convertStringToHexString("," + Util.getStringFromStringArray(compName) + ","));
            cs1.setInt(6, fromDay);
            cs1.setInt(7, fromMonth);
            cs1.setInt(8, fromYear);
            cs1.setInt(9, toDay);
            cs1.setInt(10, toMonth);
            cs1.setInt(11, toYear);
            cs1.setString(12, orderBy);
            cs1.setString(13, orderType);
            cs1.setInt(14, commId);
            cs1.setInt(15, isTSAdmin);
            DatabaseData data = conn.executeCallableStatementWithResult(cs1);

            return setDetailedReportXmlData(data, isTSAdmin==1?true:false);
        } catch (Exception e) {
            logger.error(e);
            e.printStackTrace();
            return new InvoiceXmlData[0];
        }
        finally{
            try{
                ConnectionPool.getInstance().releaseConnection(conn);
            }catch(BaseException e){
                logger.error(e);
            }           
        }
        
    }
        
    private static InvoiceXmlData[] setDetailedReportXmlData(DatabaseData data, boolean isTSAdmin) {
    	LinkedHashMap<Long, InvoiceXmlData> result = new LinkedHashMap<Long, InvoiceXmlData>();
    	StringBuilder searchIds = new StringBuilder();
        logger.debug("I took from database " + data.getRowNumber() + " entries");
        for (int i = 0; i < data.getRowNumber(); i++) {
        	InvoiceXmlData invoiceXmlData = new InvoiceXmlData();
            invoiceXmlData.setSearchId(Long.parseLong(data.getValue("search_id", i).toString()));
            invoiceXmlData.setId(Long.parseLong(data.getValue("ID", i).toString()));
            invoiceXmlData.setAbstrFileNo((String)data.getValue("file_no", i));
            invoiceXmlData.setAgentCompany((String)data.getValue("ag_comp", i));
            invoiceXmlData.setAgentCity((String)data.getValue("ag_city", i));
            invoiceXmlData.setAgentWorkAddress((String)data.getValue("ag_work_addr", i));
            invoiceXmlData.setAgentZip((String)data.getValue("ag_zip", i));
            invoiceXmlData.setAgentStateAbv((String)data.getValue("ag_state", i));
            
            invoiceXmlData.setPropertyNo((String)data.getValue("prop_adr_no", i));
            invoiceXmlData.setPropertyDirection((String)data.getValue("prop_adr_dir", i));
            invoiceXmlData.setPropertyName((String)data.getValue("prop_adr_name", i));
            invoiceXmlData.setPropertySuffix((String)data.getValue("prop_adr_suf", i));
            invoiceXmlData.setPropertyUnit((String)data.getValue("prop_adr_unit", i));
            invoiceXmlData.setPropertyCity((String)data.getValue("prop_adr_city", i));
            invoiceXmlData.setPropertyZip((String)data.getValue("prop_zip", i));
            invoiceXmlData.setPropertyStateAbv((String)data.getValue("prop_state", i));
            invoiceXmlData.setPropertyCounty((String)data.getValue("prop_county", i));
            
            invoiceXmlData.setProductType(((Long)data.getValue("prodType", i)).intValue());
            invoiceXmlData.setSearchFee(DBManager.getSearchFee(data,i, isTSAdmin));
            invoiceXmlData.setDoneTime(((Date)data.getValue("tsr_date", i)));

            invoiceXmlData.setAgentLastName((String)data.getValue("AGENTLAST", i));
            invoiceXmlData.setAgentFirstName((String)data.getValue("AGENTFIRST", i));
            invoiceXmlData.setAgentLogin((String)data.getValue("agentLogin", i));
            try {
            	invoiceXmlData.setAgentId((Long)data.getValue("agentId", i));
            }catch(Exception e) {
            	try {
            		invoiceXmlData.setAgentId(((BigInteger)data.getValue("agentId", i)).longValue());
            	}catch(Exception ex) {
            		invoiceXmlData.setAgentId(-1L);
            		logger.info("Invoice: Could not get agent for search "+invoiceXmlData.getId()+" ");
            	}
            }
            String tmpOp = (String)data.getValue("opAccID", i);
            if (tmpOp == null) 
            	tmpOp = ""; 
            invoiceXmlData.setPlantInvoice(tmpOp);
            invoiceXmlData.setOperatingAccountingID((String)data.getValue("personalID", i));	//no plant just yet
            try {
            	if(data.getValue("image_count", i) != null) {
            		invoiceXmlData.setImageCount(String.valueOf(data.getValue("image_count", i)));
                }else {
                	invoiceXmlData.setImageCount("N/A");
                }
            	if(data.getValue("image_count_description", i) != null) {
            		invoiceXmlData.setDataSource((String)data.getValue("image_count_description", i));
                }else {
                	invoiceXmlData.setDataSource("N/A");
                }
            } catch (Exception e){
            	logger.error("Error while setting image count");
            	e.printStackTrace();
            }
            
            try {
            	if(data.getValue("request_count_description", i) != null) {
            		invoiceXmlData.setRequestCountDescription((String) data.getValue("request_count_description", i));
                } else{
                	invoiceXmlData.setRequestCountDescription("N/A");
                }
            } catch (Exception e){
            	logger.error("Error while setting request count");
            	e.printStackTrace();
            }
            
            
            int tsrCreated = Integer.parseInt(data.getValue("tsr_created",i).toString());
            if(tsrCreated == 1) {
            	invoiceXmlData.getSearchFlags().setTsrCreated(true);
            }
            boolean isClosedSearch = (Boolean)data.getValue(DBConstants.FIELD_SEARCH_FLAGS_IS_CLOSED, i);
            invoiceXmlData.getSearchFlags().setClosed(isClosedSearch);
            invoiceXmlData.setInvoiced(((Long)data.getValue("invoicedField",i)).intValue());
            
            searchIds.append(invoiceXmlData.getId() + ", ");
            result.put(invoiceXmlData.getId(), invoiceXmlData);
        }
        
        if(result.size() > 0 ) {
        	searchIds.delete(searchIds.length()-2, searchIds.length());
        	List<NameMapper> names = DBReports.getNamesForSearchIdsFromTable(searchIds.toString(), DBConstants.TABLE_PROPERTY_OWNER);
        	for (NameMapper name : names) {
				InvoiceXmlData temp = result.get(name.getSearchId());
				if(temp != null){
					temp.addOwnerName(name.getName());
				}
			}
        	names = DBReports.getNamesForSearchIdsFromTable(searchIds.toString(), DBConstants.TABLE_PROPERTY_BUYER);
        	for (NameMapper name : names) {
				InvoiceXmlData temp = result.get(name.getSearchId());
				if(temp != null){
					temp.addBuyerName(name.getName());
				}
			}
        	
        }
        
        InvoiceXmlData resultAsArray[] = new InvoiceXmlData[0];
        
        resultAsArray = result.values().toArray( resultAsArray );
        
        return resultAsArray;
    }

	public static InvoiceData getInvoiceRecipientData(int commId, long userId){       
	    
	    DBConnection conn = null;
	    DatabaseData data;
	    InvoiceData invoiceTotal = new InvoiceData();
	
	    //completing the invoice fields for the client community/user
	    if (userId == -1){
	
	        //comm
	        String stm = "SELECT a.COMM_NAME, a.ADDRESS, a.PHONE, a.EMAIL " +
	                     "FROM "+ DBConstants.TABLE_COMMUNITY +" a " + 
	                     "WHERE a.COMM_ID = " + commId;
	
	        try {
	            
	            conn = ConnectionPool.getInstance().requestConnection();
	            
	            data = conn.executeSQL(stm);
	            invoiceTotal.setCommName((String)data.getValue(1,0));
	            invoiceTotal.setCommAddress((String)data.getValue(2,0));
	            invoiceTotal.setCommPhone((String)data.getValue(3,0));
	            invoiceTotal.setCommEmail((String)data.getValue(4,0));
	
	        } catch (BaseException e) {
	            logger.error(e);
	        } finally{
	            try{
	                ConnectionPool.getInstance().releaseConnection(conn);
	            }catch(BaseException e){
	                logger.error(e);
	            }           
	        }
	        if (logger.isDebugEnabled())
	            logger.debug("results: " + invoiceTotal.getCommName() + " /" + invoiceTotal.getCommAddress() + " /" + invoiceTotal.getCommPhone() + " /" + invoiceTotal.getCommEmail());
	        
	    } else {
	        
	        //user
	        try {
	            UserAttributes ua = UserUtils.getUserFromId(userId);
	            invoiceTotal.setCommName(ua.getFIRSTNAME() + " " + ua.getMIDDLENAME() + " " + ua.getLASTNAME());
	            invoiceTotal.setCommAddress(ua.getWADDRESS());
	            invoiceTotal.setCommPhone(ua.getPHONE());
	            invoiceTotal.setCommEmail(ua.getEMAIL());
	        } catch (BaseException e2) {
	            logger.error(e2);
	            invoiceTotal.setCommName("");
	            invoiceTotal.setCommAddress("");
	            invoiceTotal.setCommPhone("");
	            invoiceTotal.setCommEmail("");
	        }
	    }
	    
	    return invoiceTotal;
	}
	
	/**
	 * Creates an InvoiceData object initialized with comm* and agent* params
	 * @param commId
	 * @param abstractorId
	 * @return
	 */
	public static InvoiceData getInvoiceRecipientDataAsCommAdmin(int commId, long userId){       
	    
	    DBConnection conn = null;
	    DatabaseData data;
	    InvoiceData invoiceTotal = new InvoiceData();
	          
	
	    //comm
	    String stm = "SELECT a.COMM_NAME, a.ADDRESS, a.PHONE, a.EMAIL " +
	                 "FROM "+ DBConstants.TABLE_COMMUNITY +" a " + 
	                 "WHERE a.COMM_ID = " + commId;
	
	    try {
	        
	        conn = ConnectionPool.getInstance().requestConnection();
	        
	        data = conn.executeSQL(stm);
	        invoiceTotal.setCommName((String)data.getValue(1,0));
	        invoiceTotal.setCommAddress((String)data.getValue(2,0));
	        invoiceTotal.setCommPhone((String)data.getValue(3,0));
	        invoiceTotal.setCommEmail((String)data.getValue(4,0));
	
	    } catch (BaseException e) {
	        logger.error(e);
	    } finally{
	        try{
	            ConnectionPool.getInstance().releaseConnection(conn);
	        }catch(BaseException e){
	            logger.error(e);
	        }           
	    }
	    if (logger.isDebugEnabled())
	        logger.debug("results: " + invoiceTotal.getCommName() + " /" + invoiceTotal.getCommAddress() + " /" + invoiceTotal.getCommPhone() + " /" + invoiceTotal.getCommEmail());
	    
	    invoiceTotal.setAgentName("");
	    invoiceTotal.setAgentAddress("");
	    invoiceTotal.setAgentPhone("");
	    invoiceTotal.setAgentEmail("");
	    
	    if (userId != -1){	//if a user(agent) is selected
	    	try {
	            UserAttributes ua = UserUtils.getUserFromId(userId);
	            invoiceTotal.setAgentName(ua.getFIRSTNAME() + " " + ua.getMIDDLENAME() + " " + ua.getLASTNAME());
	            invoiceTotal.setAgentAddress(ua.getWADDRESS());
	            invoiceTotal.setAgentPhone(ua.getPHONE());
	            invoiceTotal.setAgentEmail(ua.getEMAIL());
	        } catch (BaseException e2) {
	            logger.error(e2);
	        }
	    }
	    
	    return invoiceTotal;
	}

	public static Vector<InvoiceSolomonBean> getIntervalInvoiceSolomonData(
			int[] countyId, 
			int[] abstractorId, int[] agentId, int[] stateId, String[] compName, 
			int fromDay, int fromMonth, int fromYear, int toDay, int toMonth, int toYear, 
			String orderBy, String orderType, int commId, int isTSAdmin, String invoiceNumber, User currentUser) {
		
		String sql = "call getInvoiceSolomonData(?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";       
        DBConnection conn = null;
        
        try {
            
            conn = ConnectionPool.getInstance().requestConnection();
            
            NewProxyCallableStatement cs1 = (NewProxyCallableStatement)conn.prepareCall(sql);
            cs1.setString(1, "," + Util.getStringFromArray(countyId) + ",");
            cs1.setString(2, "," + Util.getStringFromArray(abstractorId) + ",");
            cs1.setString(3, "," + Util.getStringFromArray(agentId) + ",");
            cs1.setString(4, "," + Util.getStringFromArray(stateId) + ",");
            cs1.setString(5, StringUtils.convertStringToHexString("," + Util.getStringFromStringArray(compName) + ","));
            cs1.setInt(6, fromDay);
            cs1.setInt(7, fromMonth);
            cs1.setInt(8, fromYear);
            cs1.setInt(9, toDay);
            cs1.setInt(10, toMonth);
            cs1.setInt(11, toYear);
            cs1.setString(12, orderBy);
            cs1.setString(13, orderType);
            cs1.setInt(14, commId);
            cs1.setInt(15, isTSAdmin);
            DatabaseData data = conn.executeCallableStatementWithResult(cs1);

            return setDetailedReportSolomonData(data, invoiceNumber, commId, currentUser);
        } catch (Exception e) {
            logger.error(e);;
            e.printStackTrace();
            return null;
        }
        finally{
            try{
                ConnectionPool.getInstance().releaseConnection(conn);
            }catch(BaseException e){
                logger.error(e);
            }           
        }
		
	}

	private static Vector<InvoiceSolomonBean> setDetailedReportSolomonData(DatabaseData data, String invoiceNumber, int commId, User user ) {
		Vector<InvoiceSolomonBean> result = new Vector<InvoiceSolomonBean>();
		
		Long currentUserId = -2L;
		InvoiceSolomonBean currentSolomon = null;
		UserAttributes ua = user.getUserAttributes();
		
		String tempId = null;
		
		for (int i = 0; i < data.getRowNumber(); i++) {
            tempId = (String)data.getValue(InvoiceSolomonBean.OPERATING_ID, i);
            Long agentId = (Long)data.getValue(InvoiceSolomonBean.AGENT_ID, i);

            if(tempId == null)
            	tempId = "N/A";
            if(agentId == null)
            	agentId = -1L;
            if(!agentId.equals(currentUserId)) {
            	currentUserId = agentId;
            	currentSolomon = new InvoiceSolomonBean();
            	currentSolomon.setOperatingId(tempId);
            	currentSolomon.setInvoiceNumber(invoiceNumber);
            	currentSolomon.setAgentId(agentId);
            	result.add(currentSolomon);
            }
            InvoiceSolomonBeanEntry solomonEntry = new InvoiceSolomonBeanEntry();
            String fileId = data.getValue("FILEID", i).toString();
            if(fileId==null)
            	fileId = "N/A";

			int istart = fileId.indexOf("-");
			int iend = fileId.indexOf("_");
			if (istart > -1 && iend > -1)
				fileId = fileId.substring(istart + 1, iend);
			
			String agentFileId = data.getValue("AGENTFILEID", i).toString();
			agentFileId = agentFileId.replaceAll("^[']+([^']+)[']+$", "$1");
			
            solomonEntry.setTransaction(fileId);
            solomonEntry.setTransactionNoAgent(agentFileId);
            
            int tsrCreated = Integer.parseInt(data.getValue("tsr_created",i).toString());
            if(tsrCreated == 1) {
            	solomonEntry.setTsrCreated(true);
            }
            
            solomonEntry.setTransactionDescription(( solomonEntry.isTsrCreated()?
            		DBManager.getProductNameFromCommunity(commId, Integer.parseInt(data.getValue("prodId", i).toString())):
            		"Index"));
            solomonEntry.setTransactionAmt( String.valueOf(DBManager.getSearchFee(data, i, ua.isTSAdmin())));
            solomonEntry.setSearchId(data.getValue("ID", i).toString());
            solomonEntry.setAgentId(agentId);
            
            solomonEntry.setInvoiced(((Long)data.getValue("invoicedField",i)).intValue());
            
            currentSolomon.addEntry(solomonEntry);

		}
		
		return result;
	}

	/**
	 * Tries to undo the invoice flag. This means that if a search was invoiced while being index and 
	 * then invoiced again after finish this operation will revert the state of the search to the previous invoice state which is index.
	 * This is done in order to avoid fee modification which takes into account if the search was first invoiced as index. 
	 * @param listChk the comma separated list that will be cleared
	 * @param columnName the database column name - different column if TSAdmin or just commAdmin
	 */
	public static void clearInvoiceFor(String listChk, String columnName) {
		String validList = StringUtils.makeValidNumberList(listChk);
		if(validList.isEmpty()) {
			return;
		}
		String sql = "SELECT s.id searchId, f." + columnName + " invoicedFlag from " + 
			DBConstants.TABLE_SEARCH + " s JOIN " + 
			DBConstants.TABLE_SEARCH_FLAGS + " f on s.id = f." + 
			DBConstants.FIELD_SEARCH_FLAGS_ID + " where s.id in (" + validList + ")";
		
		List<InvoicedSearch> invoiced = DBManager.getSimpleTemplate().query(sql, new InvoicedSearch());
		for (InvoicedSearch invoicedSearch : invoiced) {
			if(invoicedSearch.getInvoiced() == InvoicedSearch.SEARCH_INVOICED_INDEX_AND_FINISHED) {
				invoicedSearch.setInvoiced(InvoicedSearch.SEARCH_INVOICED_INDEX);
			} else {
				invoicedSearch.setInvoiced(InvoicedSearch.SEARCH_NOT_INVOICED);
			}
		}
		HashSet<InvoicedSearch> input = new HashSet<InvoicedSearch>(invoiced);
		DBManager.updateSearchesInvoiceStatus(input, columnName, false);
		
	}

}
