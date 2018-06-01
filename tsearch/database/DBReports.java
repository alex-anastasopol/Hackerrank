package ro.cst.tsearch.database;

import java.io.File;
import java.math.BigInteger;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Vector;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.log4j.Logger;
import org.springframework.dao.EmptyResultDataAccessException;

import ro.cst.tsearch.SearchFlags;
import ro.cst.tsearch.ServerConfig;
import ro.cst.tsearch.SearchFlags.CREATION_SOURCE_TYPES;
import ro.cst.tsearch.community.CommunityAttributes;
import ro.cst.tsearch.community.CommunityManager;
import ro.cst.tsearch.database.procedures.ProcedureManager;
import ro.cst.tsearch.database.procedures.TableReportProcedure;
import ro.cst.tsearch.database.procedures.TableReportProcedure.INTERVAL_TYPES;
import ro.cst.tsearch.database.rowmapper.NameMapper;
import ro.cst.tsearch.database.rowmapper.SearchUserTimeMapper;
import ro.cst.tsearch.exceptions.BaseException;
import ro.cst.tsearch.generic.Util;
import ro.cst.tsearch.reports.data.AbstractorWorkedTime;
import ro.cst.tsearch.reports.data.DayReportLineData;
import ro.cst.tsearch.reports.data.ReportLineData;
import ro.cst.tsearch.reports.invoice.InvoiceDuplicate;
import ro.cst.tsearch.servers.parentsite.County;
import ro.cst.tsearch.servlet.BaseServlet;
import ro.cst.tsearch.servlet.FileServlet;
import ro.cst.tsearch.threads.CommAdminNotifier;
import ro.cst.tsearch.user.UserAttributes;
import ro.cst.tsearch.utils.CurrentInstance;
import ro.cst.tsearch.utils.DBConstants;
import ro.cst.tsearch.utils.FileUtils;
import ro.cst.tsearch.utils.FormatDate;
import ro.cst.tsearch.utils.InstanceManager;
import ro.cst.tsearch.utils.RequestParams;
import ro.cst.tsearch.utils.StringUtils;
import ro.cst.tsearch.utils.TSOpCode;
import ro.cst.tsearch.utils.URLMaping;

import com.mchange.v2.c3p0.impl.NewProxyCallableStatement;
import com.stewart.ats.user.UserManager;
import com.stewart.ats.user.UserManagerI;
import com.stewart.ats.user.UserRestrictionsI;

public class DBReports {
	
	private static final Logger logger = Logger.getLogger(DBReports.class);
	public static final Pattern portPattern = Pattern.compile( "(?is).*(:\\d+)" );
	
	public static DayReportLineData[] getIntervalReportData(int[] countyId, int[] abstractorId, int[] agentId, 
            int[] stateId, String[] compName, int fromDay, int fromMonth, int fromYear, 
            int toDay, int toMonth, int toYear, String orderBy, 
            String orderType, int commId, int[] status, int invoice,int offset, int rowsPerPage, UserAttributes ua, int dateType){
        
        String sql = "call getDetailedReportAllInOne(?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
        DBConnection conn = null;
        
        int isTSAdmin = 0;
        
        try
        {
            if(ua.isTSAdmin())
            {
                isTSAdmin = 1;
            }
        }catch( Exception e ){}
        
        String abstractorIdString = Util.getStringFromArray(abstractorId);
        String agentIdString = Util.getStringFromArray(agentId);
        String countyIdString = Util.getStringFromArray(countyId);
        
        UserManagerI userManager = UserManager.getInstance();
        try {
			userManager.getAccess();
			UserRestrictionsI userRestrictions  = userManager.getUser(ua.getID().longValue()).getRestriction();
			
			if(userRestrictions.hasAbstractorAssigned()) {
				abstractorIdString = "";
				for (int i = 0; i < abstractorId.length; i++) {
					if(userRestrictions.isAbstractorAssigned(abstractorId[i])) {
						abstractorIdString += abstractorId[i] + ",";
					}
				}
				if(abstractorIdString.length() > 0) {
					abstractorIdString = abstractorIdString.substring(0, abstractorIdString.length() - 1);
				}
				
				if(abstractorIdString.length() == 0) {
					//auto select assigned abstractors
					abstractorIdString = Util.getStringFromArray(userRestrictions.getAllowedAbstractors());
				}
			}
			
			if(userRestrictions.hasAgentAllowed()) {
				agentIdString = "";
				for (int i = 0; i < agentId.length; i++) {
					if(userRestrictions.isAgentAllowed(agentId[i])) {
						agentIdString += agentId[i] + ",";
					}
				}
				if(agentIdString.length() > 0) {
					agentIdString = agentIdString.substring(0, agentIdString.length() - 1);
				}
				
				if(agentIdString.length() == 0) {
					//auto select assigned abstractors
					agentIdString = Util.getStringFromArray(userRestrictions.getAllowedAgentsAsArray());
				}
			}
			
		} catch (Throwable t) {
			logger.error("Error while trying to enforce restriction rules for reports for user " + ua.getID(), t);
		} finally {
			userManager.releaseAccess();
		}
		
		if(ua.isTSAdmin() || ua.isCommAdmin()) {
			//we do not care
		} else {
			Vector<County> allAllowedCounties = ua.getAllowedCountyList();
			if(allAllowedCounties.size() > 0) {	//we have something set here
				HashSet<Integer> allAllowedCountyIds = new HashSet<Integer>();
				int[] allAllowedCountiesInt = new int[allAllowedCounties.size()];
				int i = 0;
				for (County county : allAllowedCounties) {
					allAllowedCountyIds.add(county.getCountyId().intValue());
					allAllowedCountiesInt[i++] = county.getCountyId().intValue();
				}
				countyIdString = "";
				for (i = 0; i < countyId.length; i++) {
					if(allAllowedCountyIds.contains(countyId[i])) {
						countyIdString += countyId[i] + ",";
					}
				}
				if(countyIdString.length() > 0) {
					countyIdString = countyIdString.substring(0, countyIdString.length() - 1);
				}
				
				if(countyIdString.length() == 0) {
					//auto select assigned abstractors
					countyIdString = Util.getStringFromArray(allAllowedCountiesInt);
				}
			}
		}
        
        try {
            
            conn = ConnectionPool.getInstance().requestConnection();
            
            NewProxyCallableStatement cs = (NewProxyCallableStatement)conn.prepareCall(sql);
            cs.setString(1, "," + countyIdString + ",");
            cs.setString(2, "," + abstractorIdString + ",");
            cs.setString(3, "," + agentIdString + ",");
            cs.setString(4, "," + Util.getStringFromArray(stateId) + ",");
            cs.setString(5,StringUtils.convertStringToHexString("," +  Util.getStringFromStringArray(compName) + ","));
            cs.setInt(6, fromDay);
            cs.setInt(7, fromMonth);
            cs.setInt(8, fromYear);
            cs.setInt(9, toDay);
            cs.setInt(10, toMonth);
            cs.setInt(11, toYear);
            cs.setString(12, orderBy);
            cs.setString(13, orderType);
            cs.setInt(14, commId);
            cs.setString(15, "," + Util.getStringFromArray(status) + ",");
            cs.setInt(16, invoice);
            cs.setInt(17, isTSAdmin);
            cs.setInt(18, offset);
            cs.setInt(19, rowsPerPage);
            cs.setInt(20, 0);
            cs.setInt(21, ua.getID().intValue());
            cs.setInt(22, dateType);
            long startTime = System.currentTimeMillis();
            DatabaseData data = conn.executeCallableStatementWithResult(cs);
            if(logger.isTraceEnabled()) {
	            logger.trace("getDetailedReportAllInOne(" 
	            		+ "\"," + countyIdString + ",\"" + ", " 
	            		+ "\"," + abstractorIdString + ",\"" + ", " 
	            		+ "\"," + agentIdString + ",\"" + ", " 
	            		+ "\"," + Util.getStringFromArray(stateId) + ",\"" + ", " 
	            		+ "\"" + StringUtils.convertStringToHexString("," +  Util.getStringFromStringArray(compName) + ",") + "\"" + ", " 
	            		+ fromDay + ", " 
	            		+ fromMonth+ ", " 
	            		+ fromYear+ ", "
	            		+ toDay + ", " 
	            		+ toMonth+ ", " 
	            		+ toYear+ ", " 
	            		+ orderBy + ", " 
	            		+ orderType + ", " 
	            		+ commId + ", " 
	            		+ "\"," + Util.getStringFromArray(status) + ",\"" + ", "  
	            		+ invoice + ", " 
	            		+ isTSAdmin+ ", " 
	            		+ offset + ", " 
	            		+ rowsPerPage + ", " 
	            		+ 0 + ", " 
	            		+ ua.getID().intValue() + ", " 
	            		+ dateType + ")");
            }
            logger.debug("getIntervalReportData sql took about " + (System.currentTimeMillis() - startTime) + " millis");
            boolean fromInvoice = (invoice == 1) ? true : false;
            
            return setDetailedReportData(data, fromInvoice,ua, false, commId);
        } catch (Exception e) {
            logger.error("getIntervalReportData: ", e);
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
	
	
	
	/**
     * Extract searches using the specified information in the find input
     * @param countyId
     * @param abstractorId
     * @param agentId
     * @param stateId
     * @param compName
     * @param orderBy
     * @param orderType
     * @param commId
     * @param status
     * @param invoice
     * @param searchTerm
     * @param fromDay
     * @param fromMonth
     * @param fromYear
     * @param toDay
     * @param toMonth
     * @param toYear
     * @param searchField
     * @param ua
     * @param payrateType
     * @param extraSearchTerm1
     * @param extraSearchTerm2
     * @param extraSearchTerm3
     * @param extraSearchTerm4
     * @return
     */
    public static DayReportLineData[] getSearchReportAllInOne(int[] countyId, int[] abstractorId, 
    		int[] agentId, int[] stateId, String[] compName, String orderBy,   
            String orderType, int commId, int[] status, int invoice, String searchTerm,
            int fromDay, int fromMonth, int fromYear, int toDay, int toMonth, int toYear, String searchField,UserAttributes ua,
            int payrateType, String extraSearchTerm1, String extraSearchTerm2, String extraSearchTerm3, String extraSearchTerm4, 
            boolean treatStarterDifferently, int dateType){

        String sql = "call getSearchReportAllInOne(?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
        
        String abstractorIdString = Util.getStringFromArray(abstractorId);
        String agentIdString = Util.getStringFromArray(agentId);
        String countyIdString = Util.getStringFromArray(countyId);
        
        UserManagerI userManager = UserManager.getInstance();
        try {
			userManager.getAccess();
			UserRestrictionsI userRestrictions  = userManager.getUser(ua.getID().longValue()).getRestriction();
			
			if(userRestrictions.hasAbstractorAssigned()) {
				abstractorIdString = "";
				for (int i = 0; i < abstractorId.length; i++) {
					if(userRestrictions.isAbstractorAssigned(abstractorId[i])) {
						abstractorIdString += abstractorId[i] + ",";
					}
				}
				if(abstractorIdString.length() > 0) {
					abstractorIdString = abstractorIdString.substring(0, abstractorIdString.length() - 1);
				}
				
				if(abstractorIdString.length() == 0) {
					//auto select assigned abstractors
					abstractorIdString = Util.getStringFromArray(userRestrictions.getAllowedAbstractors());
				}
			}
			
			if(userRestrictions.hasAgentAllowed()) {
				agentIdString = "";
				for (int i = 0; i < agentId.length; i++) {
					if(userRestrictions.isAgentAllowed(agentId[i])) {
						agentIdString += agentId[i] + ",";
					}
				}
				if(agentIdString.length() > 0) {
					agentIdString = agentIdString.substring(0, agentIdString.length() - 1);
				}
				
				if(agentIdString.length() == 0) {
					//auto select assigned abstractors
					agentIdString = Util.getStringFromArray(userRestrictions.getAllowedAgentsAsArray());
				}
			}
			
		} catch (Throwable t) {
			logger.error("Error while trying to enforce restriction rules for reports for user " + ua.getID(), t);
		} finally {
			userManager.releaseAccess();
		}
		if(ua.isTSAdmin() || ua.isCommAdmin()) {
			//we do not care
		} else {
			Vector<County> allAllowedCounties = ua.getAllowedCountyList();
			if(allAllowedCounties.size() > 0) {	//we have something set here
				HashSet<Integer> allAllowedCountyIds = new HashSet<Integer>();
				int[] allAllowedCountiesInt = new int[allAllowedCounties.size()];
				int i = 0;
				for (County county : allAllowedCounties) {
					allAllowedCountyIds.add(county.getCountyId().intValue());
					allAllowedCountiesInt[i++] = county.getCountyId().intValue();
				}
				countyIdString = "";
				for (i = 0; i < countyId.length; i++) {
					if(allAllowedCountyIds.contains(countyId[i])) {
						countyIdString += countyId[i] + ",";
					}
				}
				if(countyIdString.length() > 0) {
					countyIdString = countyIdString.substring(0, countyIdString.length() - 1);
				}
				
				if(countyIdString.length() == 0) {
					//auto select assigned abstractors
					countyIdString = Util.getStringFromArray(allAllowedCountiesInt);
				}
			}
		}
		
		
        
        DBConnection conn = null;
        try {

            conn = ConnectionPool.getInstance().requestConnection();
            
            NewProxyCallableStatement cs = (NewProxyCallableStatement)conn.prepareCall(sql);
            cs.setString(1, "," + countyIdString + ",");
            cs.setString(2, "," + abstractorIdString + ",");
            cs.setString(3, "," + agentIdString + ",");
            cs.setString(4, "," + Util.getStringFromArray(stateId) + ",");
            cs.setString(5, StringUtils.convertStringToHexString("," + Util.getStringFromStringArray(compName) + ","));
            cs.setString(6, orderBy);
            cs.setString(7, orderType);
            cs.setInt(8, commId);
            cs.setString(9, "," + Util.getStringFromArray(status) + ",");
            cs.setInt(10, invoice);
            cs.setString(11, StringUtils.HTMLEntityDecode(searchTerm));          
            cs.setInt(12, fromDay);
            cs.setInt(13, fromMonth);
            cs.setInt(14, fromYear);
            cs.setInt(15, toDay);
            cs.setInt(16, toMonth);
            cs.setInt(17, toYear);         
            cs.setString(18, searchField);
            cs.setInt(19, payrateType);
            cs.setString(20, extraSearchTerm1);
            cs.setString(21, extraSearchTerm2);
            cs.setString(22, extraSearchTerm3);
            cs.setString(23, extraSearchTerm4);
            cs.setInt(24, 0);
            cs.setInt(25, dateType);
            DatabaseData data = conn.executeCallableStatementWithResult(cs);
            boolean fromInvoice = (invoice == 1) ? true : false;
            return setDetailedReportData(data, fromInvoice,ua, treatStarterDifferently, commId);
        } catch (Exception e) {
            logger.error(e);
            e.printStackTrace();
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
	
	
	public static DayReportLineData[] setDetailedReportData(DatabaseData data, 
			boolean isInvoice, UserAttributes ua, boolean treatStarterDifferently, int commId){
        UserAttributes currentUser = ua; 
        
        int userId = currentUser.getID().intValue();
        CommunityAttributes communityAttributes = null;   
        
        StringBuilder searchIds = new StringBuilder();
        try {
			communityAttributes = CommunityManager.getCommunity(commId);
		} catch (BaseException shouldNotHappen) {shouldNotHappen.printStackTrace();}
		int defaultHoursBack = communityAttributes.getDEFAULTSLA().intValue();
		
        HashMap<InvoiceDuplicate, Long> tsrIds = new HashMap<InvoiceDuplicate, Long> ();
        //DayReportLineData detailedReportData[] = null;
        
        String appUrl = ServerConfig.getAppUrl();
        String appPort = "";
        
        try{
        	Matcher portMatcher = portPattern.matcher( appUrl );
        	if( portMatcher.find() ){
        		appPort = portMatcher.group( 1 );
        	}
        }
        catch( Exception e ){
        	logger.info( "No port found!" );
        }
        
        LinkedHashMap<Long, DayReportLineData> report = new LinkedHashMap<Long, DayReportLineData>();
        long starttime = System.currentTimeMillis();
        long realStartTime = starttime;
        
        try { 
            logger.debug("DBManager setDetailedReportData# results number: " + data.getRowNumber());
            
            for (int i = 0; i<data.getRowNumber(); i++){
            	long endtime = System.currentTimeMillis();
            	starttime = endtime;
            	if( !currentUser.isAllowedCounty( (BigInteger)data.getValue("COUNTYID", i) ) )
                {
            		continue;
                }
            	long searchId = ((BigInteger)data.getValue("ID",i)).longValue();
            	searchIds.append(searchId + ", ");

        	    DayReportLineData detailedReportData = report.get( searchId );
        		detailedReportData = new DayReportLineData();
        		report.put(searchId, detailedReportData);
        		detailedReportData.setUa(ua);
            	detailedReportData.setId(searchId);
            	detailedReportData.setSearchId(searchId);
            	
            	detailedReportData.getSearchFlags().setCreationSourceType(
                		DBSearch.getCreationSourceTypeFromDatabaseStatus((Integer) data.getValue("sourceCreationType", i)));
            	
            	AbstractorWorkedTime abstractorWorkedTime = new AbstractorWorkedTime();
            	abstractorWorkedTime.setFirstName((String)data.getValue("abstractor_fname",i));
            	abstractorWorkedTime.setLastName((String)data.getValue("abstractor_lname",i));
            	if((Long)data.getValue("abstractor_wt",i) != null) {
            		abstractorWorkedTime.setWorkedTime((Long)data.getValue("abstractor_wt",i));
            	}
            	abstractorWorkedTime.setSearchId(searchId);
            	abstractorWorkedTime.setUserId(((BigInteger)data.getValue("abstractor_id",i)).longValue());
            	
            	AbstractorWorkedTime secondaryAbstractorWorkedTime = null;
            	
            	try {
            		
            		if(data.getValue("sec_abstractor_id",i) != null) {
						secondaryAbstractorWorkedTime = new AbstractorWorkedTime();
						secondaryAbstractorWorkedTime.setFirstName((String)data.getValue("sec_abstractor_fname",i));
						secondaryAbstractorWorkedTime.setLastName((String)data.getValue("sec_abstractor_lname",i));
						if((Long)data.getValue("sec_abstractor_wt",i) != null) {
							secondaryAbstractorWorkedTime.setWorkedTime((Long)data.getValue("sec_abstractor_wt",i));
						}
						secondaryAbstractorWorkedTime.setSearchId(searchId);
						secondaryAbstractorWorkedTime.setUserId(((BigInteger)data.getValue("sec_abstractor_id",i)).longValue());
            		}
				} catch (Exception e1) {
					logger.error("Cannot parse secondaryAbstractorWorkedTime information", e1);
					secondaryAbstractorWorkedTime = null;
				}
            	
            	
            	if(CREATION_SOURCE_TYPES.REOPENED.equals(detailedReportData.getSearchFlags().getCreationSourceType()) && secondaryAbstractorWorkedTime != null) {
            		detailedReportData.setAbstractorWorkedTime(secondaryAbstractorWorkedTime);
	            	detailedReportData.addOtherAbstractorWorkedTime(abstractorWorkedTime);
            	} else {
	            	detailedReportData.setAbstractorWorkedTime(abstractorWorkedTime);
	            	detailedReportData.addOtherAbstractorWorkedTime(secondaryAbstractorWorkedTime);
            	}
                
                
                detailedReportData.setAgentFirstName((String)data.getValue("agent_fname",i));
                detailedReportData.setAgentLastName((String)data.getValue("agent_lname",i));
                detailedReportData.setPropertyCounty((String)data.getValue("county",i));
                detailedReportData.setPropertyStreet((String)data.getValue("address_name",i));
                detailedReportData.setPropertyNo((String)data.getValue("address_no",i));
                detailedReportData.setPropertySuffix((String)data.getValue("address_suffix",i));
                detailedReportData.setSearchTimeStamp(FormatDate.getDateFromFormatedString((String)data.getValue("tsr_timestamp",i), FormatDate.TIMESTAMP));
                detailedReportData.setPropertyState((String)data.getValue("STATEABV",i));
                detailedReportData.setInvoice(((Long)data.getValue("invoiceField",i)).intValue());
                detailedReportData.setPaid(((Long)data.getValue("paidField",i)).intValue());
                
                detailedReportData.setInvoiced(((Long)data.getValue("invoicedField",i)).intValue());
                detailedReportData.setConfirmed(((Long)data.getValue("confirmedField",i)).intValue());
                detailedReportData.setArchived(((Long)data.getValue("archivedField",i)).intValue());
                detailedReportData.setTSRFolder((String)data.getValue("TSR_FOLDER",i));
                
                detailedReportData.setLogOriginalLocation((Integer)data.getValue(DBConstants.FIELD_SEARCH_FLAGS_LOG_ORIGINAL_LOCATION, i));
                
                String fileId = (String)data.getValue("file_no",i);
                fileId = org.apache.commons.lang.StringUtils.reverse(
        				org.apache.commons.lang.StringUtils.reverse(fileId).replaceAll("\\d{6}_\\d{8}_", ""));
                detailedReportData.setFileId(fileId);

                if(data.getValue("TSR_INITIAL_DATE",i)!=null)
                	detailedReportData.setTsrInitialDate(FormatDate.getDateFromFormatedString((String)data.getValue("TSR_INITIAL_DATE",i), FormatDate.TIMESTAMP));
                
                int tsrCreated = Integer.parseInt(data.getValue("TSR_CREATED",i).toString());
                if(tsrCreated == 1) {
                	detailedReportData.getSearchFlags().setTsrCreated(true);
                }
                
                Boolean starterField = (Boolean)data.getValue(DBConstants.FIELD_SEARCH_FLAGS_STARTER, i); 
                if(starterField != null) {
                	detailedReportData.getSearchFlags().setBase( starterField );
                } else {
                	detailedReportData.getSearchFlags().setBase( false );
                }
                boolean isClosedSearch = (Boolean)data.getValue(DBConstants.FIELD_SEARCH_FLAGS_IS_CLOSED, i);
                detailedReportData.getSearchFlags().setClosed(isClosedSearch);
                boolean isForReviewSearch = (Boolean)data.getValue(DBConstants.FIELD_SEARCH_FLAGS_FOR_REVIEW, i);
                detailedReportData.getSearchFlags().setForReview(isForReviewSearch);
                boolean isForFVS = (Boolean)data.getValue(DBConstants.FIELD_SEARCH_FLAGS_FOR_FVS, i);
                detailedReportData.getSearchFlags().setForFVS(isForFVS);
                
                try {
					detailedReportData.setAgentId((Long) data.getValue("agentId", i));
				} catch (Exception e) {
					try {
						detailedReportData.setAgentId(((BigInteger) data.getValue("agentId", i)).longValue());
					} catch (Exception ex) {
						detailedReportData.setAgentId(-1L);
					}
				}
				

				detailedReportData.setAgentTsrNameFormat( data.getValue("agent_tsr_name_format", i)==null?DBConstants.NAMES_FORMAT_FML:(Integer)data.getValue("agent_tsr_name_format", i));
				detailedReportData.setAgentTsrUpperLower( data.getValue("agent_tsr_upper_lower", i)==null?DBConstants.NAMES_TITLECASE:(Integer)data.getValue("agent_tsr_upper_lower", i));
				
				try {
					detailedReportData.setCountyId(((BigInteger)data.getValue("countyId", i)).intValue());
				} catch (Exception e) {
					logger.error("Error while reading field: [countyId]", e);
				}
				
				int addressBootstrappedCode = 0;
				try {
					addressBootstrappedCode = (Integer) data.getValue("isBootstrapped", i);
					detailedReportData.setAddressBootstrapped( (addressBootstrappedCode > 0)?true:false);
					detailedReportData.setAddressBootstrappedCode(addressBootstrappedCode);
				}catch(Exception e) {
					detailedReportData.setAddressBootstrappedCode(0);
					detailedReportData.setAddressBootstrapped(false);
				}
                
                // phone numbers
                detailedReportData.setWPhone((String)data.getValue("workphone", i));
                detailedReportData.setHPhone((String)data.getValue("homephone", i));
                detailedReportData.setMPhone((String)data.getValue("mobilephone", i));
                detailedReportData.setSearchDueDate((Timestamp)data.getValue("DUE_DATE", i));
                     
                
                	//------upgrade to get the product type if possible
                if(data.getValue("prodId", i)!=null){
                	detailedReportData.setProductType(Integer.parseInt(data.getValue("prodId", i).toString()));
                	if(tsrCreated == 1) {
	                	detailedReportData.setProductName(DBManager.getProductNameFromCommunity(commId, Integer.parseInt(data.getValue("prodId", i).toString())));
                	} else {
                		detailedReportData.setProductName("Index");
                	}
                	if(isInvoice) {
                		
                	}
                } else {
                	detailedReportData.setProductType(0);                	
                }
                //------end upgrade
                //------upgrade to get the was_status field now so if we need it, we won't have to do a new query
                Integer wasOpened = (Integer)data.getValue("WAS_OPENED", i);
                if(wasOpened!=null){
                	detailedReportData.setWasOpened((wasOpened.intValue()==1)?true:false);
                } else {
                	detailedReportData.setWasOpened(false);
                }
                //------end upgrade
                if( isInvoice )
                {
                    if( data.getValue(29,i) == null )
                    {
                        detailedReportData.setSearchTimeStamp(FormatDate.getDateFromFormatedString((String)data.getValue(11,i), FormatDate.TIMESTAMP));
                    }
                    else
                    {
                        detailedReportData.setSearchTimeStamp(FormatDate.getDateFromFormatedString((String)data.getValue(29,i), FormatDate.TIMESTAMP));
                    }
                    //------upgrade: setting the fee if possible (avoiding more sql queries)
                    detailedReportData.setFee(DBManager.getSearchFee(data,i,currentUser.isTSAdmin()));
                    
                    if(data.getValue("discountRatio", i) != null) {
                    	detailedReportData.setDiscountRatio((Float)data.getValue("discountRatio", i));
                    }
                    //------end upgrade     
                    
                    try {
    	                String justFileId = fileId.substring(fileId.indexOf('-') + 1);
    	                
    	                if (org.apache.commons.lang.StringUtils.countMatches(fileId, "_") == 2){
    	                	justFileId = fileId.substring(
        	                		fileId.indexOf('-') + 1,
        	                		fileId.lastIndexOf('_', fileId.lastIndexOf('_') - 2));
    	                }
    	                InvoiceDuplicate invoiceDuplicate = new InvoiceDuplicate();
    	                invoiceDuplicate.setFileId(justFileId);
    	                invoiceDuplicate.setAgentId(detailedReportData.getAgentId());
    	                invoiceDuplicate.setCountyId(detailedReportData.getCountyId());
    	                invoiceDuplicate.setProductType(detailedReportData.getProductType());
    	                invoiceDuplicate.setStreetNo(detailedReportData.getPropertyNo());
    	                invoiceDuplicate.setStreetName(detailedReportData.getPropertyStreet());
    	                if(tsrIds.get(invoiceDuplicate)==null) {
    	                	tsrIds.put(invoiceDuplicate, detailedReportData.getId());
    	                }
    	                else {
    	                	//tsrIds.put(justFileId, tsrIds.get(justFileId)+1);
    	                	detailedReportData.setDuplicate(1);
    	                	report.get(tsrIds.get(invoiceDuplicate)).setDuplicate(1);
    	                }
                    } catch (Exception e){
                    	logger.error("Error while checking duplicates", e);
                    }
                    
                    try {
                    	
                    	if(data.getValue("image_count", i) != null) {
                        	detailedReportData.setImageCount((Integer)data.getValue("image_count", i));
                        }
                    	
                    	if(data.getValue("image_count_description", i) != null) {
                        	detailedReportData.setDataSource((String)data.getValue("image_count_description", i));
                        }else {
                        	detailedReportData.setDataSource("N/A");
                        }
                    } catch (Exception e){
                    	logger.error("Error while setting image count");
                    	e.printStackTrace();
                    }
                    
                    try {
                    	if(data.getValue("request_count_description", i) != null) {
                    		detailedReportData.setRequestCountDescription((String) data.getValue("request_count_description", i));
                        } else {
                        	detailedReportData.setRequestCountDescription("N/A");
                        }
                    } catch (Exception e){
                    	logger.error("Error while setting request count");
                    	e.printStackTrace();
                    }
                    
                } else {
                	Integer colorCode = (Integer)data.getValue("color_flag", i);
                	if(colorCode != null) {
                		detailedReportData.setColorCodeStatus(colorCode.intValue());
                	}
                }
                
                
                
                String tsrSentTo = (String)data.getValue("tsr_sent_to",i);
                
                
                if( tsrSentTo == null || tsrSentTo.equals("N/A"))
                {
                    tsrSentTo = (String)data.getValue("AGENTEMAIL", i);
                }
                
                detailedReportData.setSendTo( tsrSentTo );
                detailedReportData.setCallFromInvoice( isInvoice );
                                
//              the TSR link is set with the real link, only if the TSR was created
                // <a href="#" onClick="<tsearch:LoopItemTag field = 'fileLink'/>"><tsearch:LoopItemTag field = 'fileId'/></a>
                
                String ansambluLink = "";
                
                int checkedBy = 0;
                int dbOwnerId = -1;
                try {
                	Object cB = data.getValue("CHECKED_BY",i);
                	if (cB != null){
                		checkedBy = ((Integer)cB).intValue();
                	}
                    
                    if( checkedBy > 0 )
                    {
                        checkedBy = 1;
                    }
                    
                } catch (Exception e) {
                	e.printStackTrace();
                }

                try
                {
                    dbOwnerId = ((BigInteger)data.getValue("ABSTRACT_ID",i)).intValue();
                } catch (Exception e) {
                	e.printStackTrace();
                }
                
                String statusTSR = "";
                String ansambCell = "";
                
                String tsrNewLink = (String)data.getValue("tsr_link",i);
                String createdTSRLink1 = "";
                int indexSSF = tsrNewLink.indexOf("&SSFLINK=");
                
                if(indexSSF<=0){
                	 String tsrFilePathDB = tsrNewLink.replaceAll( "http://ats0[0-9]" , "ats");
                     
                     int idxATS = tsrFilePathDB.indexOf( "ats01" );
                     if( idxATS < 0 ){
                     	idxATS = tsrFilePathDB.indexOf( "ats02" );
                     }
                     
                     if( idxATS == 0 ){
                     	tsrFilePathDB = "ats" + tsrFilePathDB.substring( idxATS + 5 );
                     }
                     
                     tsrFilePathDB = tsrFilePathDB.replaceAll( "http://[^/]*/" , "");
                     
                     createdTSRLink1 = appUrl + "/" + tsrFilePathDB; 
                     
                     String tsrFilename = tsrFilePathDB.substring(tsrFilePathDB.indexOf("f=") + 2);
                     File tsrFile = new File(BaseServlet.FILES_PATH + tsrFilename); 
                     // try to find the tsr locally
                     if(tsrFile.exists()){
                     	createdTSRLink1 = "/title-search/fs?f=" + tsrFilename;
                     } else {
                     	String server = DBManager.findTsrServer(tsrFilename);
                     	if(server != null){
                     		createdTSRLink1 = server + appPort + "/title-search/fs?f=" + tsrFilename;
                     	}
                     }
                     
                     createdTSRLink1 = escapeAmp(createdTSRLink1 , ".");
                     if(createdTSRLink1.toLowerCase().endsWith(".pdf")||createdTSRLink1.toLowerCase().endsWith(".tiff")||createdTSRLink1.toLowerCase().endsWith(".pdf")){
                     	createdTSRLink1 = createdTSRLink1+"&searchId="+searchId+"&viewTSR=true";
                     }
                }else{
                	createdTSRLink1 = tsrNewLink.substring(indexSSF+9);
                }
                
                int offset = 85;
                if ( ( checkedBy == 1 && dbOwnerId == userId ) || checkedBy == 0) {
                    
                	String extraScript = "";
                	if(tsrCreated == 1) {
                		extraScript = "<script>" +
                			"window.mm_menu_" + searchId + " = " +
                					"new Menu2(" +
                					"\"root\"," +
                					offset +  ",20," +
                					"menuFontFaceReportsMenu, menuFontSizeReportsMenu," +
                					"\"#000000\",\"#000000\",\"#ffffff\",\"#E0EDFE\",\"left\"," +
                					"\"middle\",2,0,1000,-5,7,true,true,true,0,true,true,\"normal\");" + 
                			"mm_menu_" + searchId + ".addMenuItem(" +
                					"\"View\"," +
                					"\"openCreatedTSR('" + createdTSRLink1  + "')\");" +
                			"mm_menu_" + searchId + ".addMenuItem(" +
                					"\"Reopen\"," +
                					"\"submitPage('" + URLMaping.path + URLMaping.REUSE_SEARCH + "?" + 
                					TSOpCode.OPCODE + "=" + TSOpCode.REUSE_SEARCH_CODE + "&" + 
                					RequestParams.SEARCH_ID + "=" + searchId + "'," + 
                					searchId + ",'true')\");" +
                			"mm_menu_" + searchId + ".addMenuItem(" +
                					"\"Clone\"," +
                					"\"submitPage('" + URLMaping.path + URLMaping.REUSE_SEARCH + "?" + 
                					TSOpCode.OPCODE + "=" + TSOpCode.CLONE_SEARCH_CODE + "&" + 
                					RequestParams.SEARCH_ID + "=" + searchId + "'," + 
                					searchId + ",'false')\");" +
                            "mm_menu_" + searchId + ".addMenuItem(" +
                					"\"Copy\"," +
                					"\"copySearch(" + searchId +  ")\");" +
                			"mm_menu_" + searchId + ".addMenuItem(" +
                					"\"DateDown\"," +
                					"\"submitPage('" + URLMaping.path + URLMaping.REUSE_SEARCH + "?" + 
                					TSOpCode.OPCODE + "=" + TSOpCode.REUSE_SEARCH_CODE + "&" +
                					"isDateDown=true&"+
                					RequestParams.SEARCH_ID + "=" + searchId + "'," + 
                					searchId + ",'true')\");" +
                			"mm_menu_" + searchId + ".addMenuItem(" +
                					"\"FVS Update\"," +
                					"\"submitPage('" + URLMaping.path + URLMaping.REUSE_SEARCH + "?" + 
                					TSOpCode.OPCODE + "=" + TSOpCode.FVS_UPDATE + "&" + 
                					"isFVSUpdate=true&" +
                					RequestParams.SEARCH_ID + "=" + searchId + "'," +
                					searchId + ",'false')\");" +
                			"menusForStarters[" + i + "] = mm_menu_" + searchId + "; " +
                			"</script>" ;                		
                	} else  if(isClosedSearch) {
                		
                		extraScript = "<script>" +
                			"window.mm_menu_" + searchId + " = " +
                					"new Menu2(" +
                					"\"root\"," + offset + ",20," +
                					"menuFontFaceReportsMenu, menuFontSizeReportsMenu," +
                					"\"#000000\",\"#000000\",\"#ffffff\",\"#E0EDFE\",\"left\"," +
                					"\"middle\",2,0,1000,-5,7,true,true,true,0,true,true,\"normal\");" + 
                			"mm_menu_" + searchId + ".addMenuItem(" +
                					"\"Reopen\"," +
                					"\"submitPage('" + URLMaping.path + URLMaping.REUSE_SEARCH + "?" + 
                					TSOpCode.OPCODE + "=" + TSOpCode.REUSE_SEARCH_CODE + "&" + 
                					RequestParams.SEARCH_ID + "=" + searchId + "'," + 
                					searchId + ",'true')\");" +
                			"mm_menu_" + searchId + ".addMenuItem(" +
                					"\"Clone\"," +
                					"\"submitPage('" + URLMaping.path + URLMaping.REUSE_SEARCH + "?" + 
                					TSOpCode.OPCODE + "=" + TSOpCode.CLONE_SEARCH_CODE + "&" + 
                					RequestParams.SEARCH_ID + "=" + searchId + "'," + 
                					searchId + ",'false')\");" +
                            "mm_menu_" + searchId + ".addMenuItem(" +
                					"\"Copy\"," +
                					"\"copySearch(" + searchId +  ")\");" +
                        	"mm_menu_" + searchId + ".addMenuItem(" +
                					"\"DateDown\"," +
                					"\"submitPage('" + URLMaping.path + URLMaping.REUSE_SEARCH + "?" + 
                					TSOpCode.OPCODE + "=" + TSOpCode.REUSE_SEARCH_CODE + "&" +
                					"isDateDown=true&"+
                					RequestParams.SEARCH_ID + "=" + searchId + "'," + 
                					searchId + ",'true')\");"+
                			"menusForStarters[" + i + "] = mm_menu_" + searchId + "; " +
                			"</script>" ;                		
                	} else {
                		extraScript = "<script>" +
                			"window.mm_menu_" + searchId + " = " +
                					"new Menu2(" +
                					"\"root\"," +
                					offset + ",20," +
                					"menuFontFaceReportsMenu, menuFontSizeReportsMenu," +
                					"\"#000000\",\"#000000\",\"#ffffff\",\"#E0EDFE\",\"left\"," +
                					"\"middle\",2,0,1000,-5,7,true,true,true,0,true,true,\"normal\");" + 
                			"mm_menu_" + searchId + ".addMenuItem(" +
                					"\"Open\",";
                		
                		if(detailedReportData.getSearchFlags().isBase()) {	
                			extraScript += "\" var ctrl=document.getElementById('" + i + "'); if(confirm('This is base file, modify it?')) { verifyTSR(" + 
                					searchId + "," + 
                					searchId + ",ctrl); }\" );";
                		} else {
                			extraScript += "\" var ctrl=document.getElementById('" + i + "');  verifyTSR(" + 
		        					searchId + "," + 
		        					searchId + ",ctrl)\"); ";
                		}
                		extraScript += 		
                			"mm_menu_" + searchId + ".addMenuItem(" +
                					"\"Clone\"," +
                					"\"submitPage('" + URLMaping.path + URLMaping.REUSE_SEARCH + "?" + 
                					TSOpCode.OPCODE + "=" + TSOpCode.CLONE_SEARCH_CODE + "&" + 
                					RequestParams.SEARCH_ID + "=" + searchId + "'," + 
                					searchId + ",'false')\");" +
                            "mm_menu_" + searchId + ".addMenuItem(" +
                					"\"Copy\"," +
                					"\"copySearch(" + searchId +  ")\");" +
                        	"mm_menu_" + searchId + ".addMenuItem(" +
                					"\"DateDown\"," +
                					"\" var ctrl=document.getElementById('" + i + "');  verifyTSRImpl(" + 
		        					searchId + "," + 
		        					searchId + ",ctrl,true)\"); "+
                			"menusForStarters[" + i + "] = mm_menu_" + searchId + "; " +
                			"</script>" ;
                		
                	}
                	String extraLinkParams = 
            			/*"onmouseover= \"" +
            				"MM_showMenu(window.mm_menu_" + searchId + "," +
            					//"getMouseX(event),getMouseY(event)," +
            					"getAbsLeft('" + i + "'),getAbsTop('" + i + "')," + 
            					"null,null);" +
            				"clickat=1;" +
            				"MM_showMenu(window.mm_menu_" + searchId + "," +
            					//"getMouseX(event),getMouseY(event)," +
            					"getAbsLeft('" + i + "'),getAbsTop('" + i + "')," + 
            					"null,'upbut" + searchId + "');" +
            				"htm();\" " + 
            				*/
                		"onmouseover= \"startShowTimeout(" + searchId + "," + i + ", 500," + -2 + ")\" " +
            			"onfocus=\"blur()\" " + 
            			"onmouseout=\"htm();" +
            			"stopShowTimeout(); MM_startTimeout()\"";
                	String clas=(checkedBy == 1&&tsrCreated == 0)? "class='submitLinkRed' ":"class='submitLinkBlue'";
                	ansambluLink = extraScript + 
                        "<span id='" + i + "' " +
                    			clas +
                    			extraLinkParams + " >" +
                    			StringUtils.HTMLEntityEncode(fileId) + "</span>";
                    
                    ansambCell = ansambluLink;
                } else {
                    
                    if (checkedBy == -1) { 
                        statusTSR = "Searching";
                    } else if (checkedBy == -2) {
                        statusTSR = "TSR in progress";
                    } else {
                        //errorMessage = "Search already opened by " + ua1.getUserFullName();
                        //errorMessage = "Search already opened by " + data.getValue("ABSTRACTOR_FNAME", i) + " " + data.getValue("ABSTRACTOR_LNAME", i);
                    }
                    
                	String extraScript = "";
                	
                    if(tsrCreated == 1) {

                		extraScript = "<script>" +
                			"window.mm_menu_" + detailedReportData.getId() + " = " +
                					"new Menu2(" +
                					"\"root\"," +
                					offset + ",20," +
                					"menuFontFaceReportsMenu, menuFontSizeReportsMenu," +
                					"\"#000000\",\"#000000\",\"#ffffff\",\"#E0EDFE\",\"left\"," +
                					"\"middle\",2,0,1000,-5,7,true,true,true,0,true,true,\"normal\");" + 
                			"mm_menu_" + detailedReportData.getId() + ".addMenuItem(" +
                					"\"View\"," +
                					"\"openCreatedTSR('" + createdTSRLink1  + "')\");" +
                			"mm_menu_" + detailedReportData.getId() + ".addMenuItem(" +
                					"\"Reopen\"," +
                					"\"submitPage('" + URLMaping.path + URLMaping.REUSE_SEARCH + "?" + 
                					TSOpCode.OPCODE + "=" + TSOpCode.REUSE_SEARCH_CODE + "&" + 
                					RequestParams.SEARCH_ID + "=" + detailedReportData.getId() + "'," + 
                					detailedReportData.getId() + ",'true')\");" +
                			"mm_menu_" + detailedReportData.getId() + ".addMenuItem(" +
                					"\"Clone\"," +
                					"\"submitPage('" + URLMaping.path + URLMaping.REUSE_SEARCH + "?" + 
                					TSOpCode.OPCODE + "=" + TSOpCode.CLONE_SEARCH_CODE + "&" + 
                					RequestParams.SEARCH_ID + "=" + detailedReportData.getId() + "'," + 
                					detailedReportData.getId() + ",'false')\");" +
                            "mm_menu_" + detailedReportData.getId() + ".addMenuItem(" +
                					"\"Copy\"," +
                					"\"copySearch(" + detailedReportData.getId() +  ")\");" +
                			"mm_menu_" + searchId + ".addMenuItem(" +
                					"\"DateDown\"," +
                					"\"submitPage('" + URLMaping.path + URLMaping.REUSE_SEARCH + "?" + 
                					TSOpCode.OPCODE + "=" + TSOpCode.REUSE_SEARCH_CODE + "&" + 
                					RequestParams.SEARCH_ID + "=" + searchId + "'," +
                					"isDateDown=true&"+
                					searchId + ",'true')\");"+
                			"mm_menu_" + searchId + ".addMenuItem(" +
                					"\"FVS Update\"," +
                					"\"submitPage('" + URLMaping.path + URLMaping.REUSE_SEARCH + "?" + 
                					TSOpCode.OPCODE + "=" + TSOpCode.REUSE_SEARCH_CODE + "&" + 
                					RequestParams.SEARCH_ID + "=" + searchId + "'," +
                					"isFVSUpdate=true&" +
                					searchId + ",'false')\");" +
                			"menusForStarters[" + i + "] = mm_menu_" + detailedReportData.getId() + "; " +
                			"</script>" ;
                    }
                    else if(isClosedSearch){
                    	extraScript = "<script>" +
            			"window.mm_menu_" + searchId + " = " +
            					"new Menu2(" +
            					"\"root\"," + offset + ",20," +
            					"menuFontFaceReportsMenu, menuFontSizeReportsMenu," +
            					"\"#000000\",\"#000000\",\"#ffffff\",\"#E0EDFE\",\"left\"," +
            					"\"middle\",2,0,1000,-5,7,true,true,true,0,true,true,\"normal\");" + 
            			"mm_menu_" + searchId + ".addMenuItem(" +
            					"\"Reopen\"," +
            					"\"submitPage('" + URLMaping.path + URLMaping.REUSE_SEARCH + "?" + 
            					TSOpCode.OPCODE + "=" + TSOpCode.REUSE_SEARCH_CODE + "&" + 
            					RequestParams.SEARCH_ID + "=" + searchId + "'," + 
            					searchId + ",'true')\");" +
            			"mm_menu_" + searchId + ".addMenuItem(" +
            					"\"Clone\"," +
            					"\"submitPage('" + URLMaping.path + URLMaping.REUSE_SEARCH + "?" + 
            					TSOpCode.OPCODE + "=" + TSOpCode.CLONE_SEARCH_CODE + "&" + 
            					RequestParams.SEARCH_ID + "=" + searchId + "'," + 
            					searchId + ",'false')\");" +
                        "mm_menu_" + searchId + ".addMenuItem(" +
            					"\"Copy\"," +
            					"\"copySearch(" + searchId +  ")\");" +
                    	"mm_menu_" + searchId + ".addMenuItem(" +
            					"\"DateDown\"," +
            					"\"submitPage('" + URLMaping.path + URLMaping.REUSE_SEARCH + "?" + 
            					TSOpCode.OPCODE + "=" + TSOpCode.REUSE_SEARCH_CODE + "&" +
            					"isDateDown=true&"+
            					RequestParams.SEARCH_ID + "=" + searchId + "'," + 
            					searchId + ",'true')\");"+
            			"menusForStarters[" + i + "] = mm_menu_" + searchId + "; " +
            			"</script>" ;                	
                		
                    } else {
                		
                		extraScript = "<script>" +
                			"window.mm_menu_" + detailedReportData.getId() + " = " +
                					"new Menu2(" +
                					"\"root\"," +
                					offset + ",20," +
                					"menuFontFaceReportsMenu, menuFontSizeReportsMenu," +
                					"\"#000000\",\"#000000\",\"#ffffff\",\"#E0EDFE\",\"left\"," +
                					"\"middle\",2,0,1000,-5,7,true,true,true,0,true,true,\"normal\");" + 
                			"mm_menu_" + detailedReportData.getId() + ".addMenuItem(" +
                					"\"Open\"," ;
                		
                		if(detailedReportData.getSearchFlags().isBase()) {	
                			extraScript += "\" var ctrl=document.getElementById('" + i + "'); if(confirm('This is base file, modify it?')) { verifyTSR(" + 
		                			detailedReportData.getId() + "," + 
		                			detailedReportData.getId() + ",ctrl); } \" );";
                		} else {
                			extraScript += "\" var ctrl=document.getElementById('" + i + "');  verifyTSR(" + 
	                				detailedReportData.getId() + "," + 
	                				detailedReportData.getId() + ",ctrl)\"); ";
                		}
                		extraScript += 
                			"mm_menu_" + searchId + ".addMenuItem(" +
                					"\"Clone\"," +
                					"\"submitPage('" + URLMaping.path + URLMaping.REUSE_SEARCH + "?" + 
                					TSOpCode.OPCODE + "=" + TSOpCode.CLONE_SEARCH_CODE + "&" + 
                					RequestParams.SEARCH_ID + "=" + searchId + "'," + 
                					searchId + ",'false')\");" +
                            "mm_menu_" + detailedReportData.getId() + ".addMenuItem(" +
                					"\"Copy\"," +
                					"\"copySearch(" + detailedReportData.getId() +  ")\");" +
                			"mm_menu_" + searchId + ".addMenuItem(" +
                					"\"DateDown\"," +
                					"\" var ctrl=document.getElementById('" + i + "');  verifyTSRImpl(" + 
		        					searchId + "," + 
		        					searchId + ",ctrl,true)\"); "+
                			"menusForStarters[" + i + "] = mm_menu_" + detailedReportData.getId() + "; " +
                			"</script>" ;
                    }
                    String extraLinkParams = 
                    	"onmouseover= \"startShowTimeout(" + searchId + "," + i + ", 500, "+ -2 + ")\" " +
            			"onfocus=\"blur()\" " + 
            			"onmouseout=\"htm();" +
            			"stopShowTimeout(); MM_startTimeout()\"";
                    String css = (tsrCreated == 0) ? "submitLinkRed" : "submitLinkBlue";                    
                    ansambluLink = extraScript + "<span id='" + i + "' class='" + css + "' " + extraLinkParams + ">" + 
                    StringUtils.HTMLEntityEncode(fileId) + "</span>";
                    
                    if(!statusTSR.equals(""))
                        ansambCell = statusTSR + "<br>" + ansambluLink;
                    else
                        ansambCell = ansambluLink;
                    
                } 
                detailedReportData.setFileLink(ansambCell);
                
                
                setLogsLink(detailedReportData, data, i, currentUser, defaultHoursBack);
                
                Date TSRTimeStamp = null;
                try {
                	if(data.getValue("tsr_date", i)!=null) {
                		if(tsrCreated == 1 || isClosedSearch) {
                			TSRTimeStamp = FormatDate.getDateFromFormatedString((String)data.getValue("tsr_date", i), FormatDate.TIMESTAMP);
                		}
                	}
                } catch (Exception e) {
                	e.printStackTrace();
                }
                detailedReportData.setTsrTimeStamp(TSRTimeStamp);
                                
                String note = "";
                if( data.getValue("NOTE_CLOB",i) != null )
                {
                    note = (String) data.getValue("NOTE_CLOB",i);
                }
                if ("null".equals(note) || note == null) 
                	note = "";
                detailedReportData.setNote(note);
                
                if( data.getValue("NOTE_STATUS",i) != null )
                {
                    detailedReportData.setNoteStatus(((Integer)data.getValue("NOTE_STATUS",i)).intValue());
                }
                else
                {
                    detailedReportData.setNoteStatus(0);
                }
            }    
            logger.debug("Set Detailed Data took " + (System.currentTimeMillis()-realStartTime) + " millis");
        } catch (Exception e) {
            logger.error(e);
            e.printStackTrace();
            return new DayReportLineData [0];
        }
        if (logger.isDebugEnabled())   
        	logger.debug("DBManager setDetailedReportData# exiting...");
        
 
		if (report.size() > 0) {
			searchIds.delete(searchIds.length() - 2, searchIds.length());
			List<NameMapper> names = getNamesForSearchIdsFromTable(
					searchIds.toString(), DBConstants.TABLE_PROPERTY_OWNER);
			for (NameMapper name : names) {
				DayReportLineData dayLine = report.get(name.getSearchId());
				if (dayLine != null) {
					dayLine.addOwner(name.getName());
					dayLine.getNameColors()
							.put(name.getName(), name.getColor());
				}
			}
			
			List<AbstractorWorkedTime> abstractors = DBManager.getSimpleTemplate().query("SELECT sut.*, u." + DBConstants.FIELD_USER_FIRST_NAME + ", u." + DBConstants.FIELD_USER_LAST_NAME + 
					" FROM " + SearchUserTimeMapper.TABLE_SEARCH_USER_TIME + 
					" sut join " + DBConstants.TABLE_USER + " u on sut." + SearchUserTimeMapper.FIELD_USER_ID + " = u." + DBConstants.FIELD_USER_ID + 
					" WHERE sut." + SearchUserTimeMapper.FIELD_SEARCH_ID + " in (" + searchIds.toString() + ")", new AbstractorWorkedTime());
			for (AbstractorWorkedTime abstractorWorkedTime : abstractors) {
				DayReportLineData dayLine = report.get(abstractorWorkedTime.getSearchId());
				if(dayLine != null) {
					if(dayLine.getAbstractorWorkedTime().getUserId() != abstractorWorkedTime.getUserId() 
							&& !dayLine.getOtherAbstractorWorkedTime().containsKey(abstractorWorkedTime.getUserId())){
						dayLine.addOtherAbstractorWorkedTime(abstractorWorkedTime);
					}
				}
			}
			
		}

		DayReportLineData result[] = new DayReportLineData[0];
		result = report.values().toArray(result);

		return result;
    }
	
	private static void setLogsLink(DayReportLineData detailedReportData,
			DatabaseData data, int dbDataIndex, UserAttributes currentUser, int defaultHoursBack) {
		String orderLink = "", logLink = "", tsrIndexLink = "";
		String orderLinkTsrCreated = "", logLinkTsrCreated = "", tsrIndexLinkTsrCreated = "";
        Integer orderStatus = (Integer) data.getValue("searchOrderStatus",dbDataIndex);
        Integer logStatus = (Integer) data.getValue("searchLogStatus",dbDataIndex);
        Integer indexStatus = (Integer) data.getValue("searchIndexStatus",dbDataIndex);
        int idxATS = 0;
        long searchId = detailedReportData.getId();
            
        orderLink = (String)data.getValue("TSR_FOLDER",dbDataIndex);
        orderLink = orderLink.replaceAll( "ats0[0-9]" , "ats");
        
        idxATS = orderLink.indexOf( "ats01" );
        if( idxATS < 0 ){
        	idxATS = orderLink.indexOf( "ats02" );
        }
        
        if( idxATS == 0 ){
        	orderLink = "ats" + orderLink.substring( idxATS + 5 );
        }
        
        if( !orderLink.replace("\\\\", "\\").contains( BaseServlet.FILES_PATH ) ){
        	orderLink = BaseServlet.FILES_PATH + orderLink;
        }
        
        logLink = orderLink;
        tsrIndexLink = orderLink;
        
        File orderFile = new File( orderLink + "orderFile.html" );
        File logFile = new File( logLink + "logFile.html");
        File tsrIndexFile = new File ( tsrIndexLink + "tsrIndexFile.html");
        File orderFileTsrCreated = null;
        File logFileTsrCreated = null; 
        File tsrIndexFileTsrCreated = null;
        
        if(detailedReportData.getSearchFlags().isTsrCreated()) {
        	String dbFileLink = (String)data.getValue("tsr_link",dbDataIndex);
        	dbFileLink = dbFileLink.replaceAll("http://ats0[0-9]" , "ats");
        	
        	int indexSSF = dbFileLink.indexOf("&SSFLINK=");
        	
        	if(indexSSF >0 ){
        		dbFileLink = dbFileLink.substring(0,indexSSF);
        	}
        	
            idxATS = dbFileLink.indexOf( "ats01" );
            if( idxATS < 0 ){
            	idxATS = dbFileLink.indexOf( "ats02" );
            }
            
            if( idxATS == 0 ){
            	dbFileLink = "ats" + dbFileLink.substring( idxATS + 5 );
            }
        	
        	dbFileLink = dbFileLink.replaceAll("http://[^/]*/" , "");         
        	try {
        		dbFileLink = FileUtils.removeFileExtention(dbFileLink);
        	} catch (Exception e) {
        		System.err.println("Exception caught. Trying to ignore the lack of extension.");
				e.printStackTrace();
			}
        	
        	logLinkTsrCreated = dbFileLink + ".log.html";
            tsrIndexLinkTsrCreated = dbFileLink + ".tsr.html";
            orderLinkTsrCreated = dbFileLink + ".html";
                                
            int pos = orderLinkTsrCreated.indexOf("f=") + 2;
            
            if(pos >= 2) {
            	orderLinkTsrCreated = orderLinkTsrCreated.substring(pos);
            	logLinkTsrCreated = logLinkTsrCreated.substring(pos);
            	tsrIndexLinkTsrCreated = tsrIndexLinkTsrCreated.substring(pos);
                orderFileTsrCreated    = new File(BaseServlet.FILES_PATH + orderLinkTsrCreated);
                logFileTsrCreated      = new File(BaseServlet.FILES_PATH + logLinkTsrCreated);
                tsrIndexFileTsrCreated = new File(BaseServlet.FILES_PATH + tsrIndexLinkTsrCreated);
            }
        	
        }
        
        
        
	        
        if( orderStatus != null && orderStatus.intValue() == 1 ){
    		detailedReportData.setOrderFileLink("/title-search/jsp/TSDIndexPage/viewDescription.jsp?view=" +
    				FileServlet.VIEW_ORDER + "&viewOrder=1&userId=" + 
    				currentUser.getID().toString()+"&viewDescrSearchId="+
    				searchId + "&" + RequestParams.SHOW_FILE_ID + "=true");
    	} else if( orderFile.exists() ) {
            int idx = orderLink.indexOf( BaseServlet.FILES_PATH );
            if( idx >= 0 ) 
            {
                orderLink = orderLink.substring( idx + BaseServlet.FILES_PATH.length() );
                orderLink = orderLink.replaceAll("\\\\", "/");
            }
            orderLink = URLMaping.path + "/jsp/TSDIndexPage/viewDescription.jsp?f=" + orderLink + "orderFile.html"
    			+ "&viewOrder=1&userId=" + currentUser.getID().toString()+"&viewDescrSearchId="+searchId + 
    			"&" + RequestParams.SHOW_FILE_ID + "=true";
            detailedReportData.setOrderFileLink(orderLink);
        } else if(orderFileTsrCreated != null && orderFileTsrCreated.exists()) {
        		detailedReportData.setOrderFileLink("/title-search/jsp/TSDIndexPage/viewDescription.jsp?f=" + 
        				orderLinkTsrCreated + "&viewOrder=1&userId=" + 
        				currentUser.getID().toString()+"&viewDescrSearchId="+
        				searchId + "&" + RequestParams.SHOW_FILE_ID + "=true");
    	} else {
    		detailedReportData.setOrderFileLink( "" );
    	}
        if(detailedReportData.isLogInTable() && ServerConfig.isEnableLogInSamba()) {
        	detailedReportData.setLogFileLink("/title-search/jsp/TSDIndexPage/viewDescription.jsp?view=" +FileServlet.VIEW_LOG + "&viewOrder=1&userId=" + currentUser.getID().toString()+"&viewDescrSearchId="+searchId + 
            		"&" + RequestParams.SHOW_FILE_ID + "=true");
        } else {
	        if(detailedReportData.getSearchFlags().isTsrCreated()) {
	        	if( logStatus != null && logStatus.intValue() == 1 ){
	        		detailedReportData.setLogFileLink("/title-search/jsp/TSDIndexPage/viewDescription.jsp?view=" +FileServlet.VIEW_LOG + "&viewOrder=1&userId=" + currentUser.getID().toString()+"&viewDescrSearchId="+searchId + 
	                		"&" + RequestParams.SHOW_FILE_ID + "=true");
	        	} else if(logFile.exists()) {
	                int idx = logLink.indexOf( BaseServlet.FILES_PATH );
	                if( idx >= 0 ) {
	                    logLink = logLink.substring( idx + BaseServlet.FILES_PATH.length()).replaceAll("\\\\", "/");
	                }
	            	detailedReportData.setLogFileLink(URLMaping.path + "/jsp/TSDIndexPage/viewDescription.jsp?view=" +FileServlet.VIEW_LOG + "&f=" + logLink + "logFile.html"
	            			+ "&viewOrder=1&userId=" + currentUser.getID().toString()+"&viewDescrSearchId="+searchId + 
	            			"&" + RequestParams.SHOW_FILE_ID + "=true");
	            } else if(logFileTsrCreated != null && logFileTsrCreated.exists()) {
	        		detailedReportData.setLogFileLink(URLMaping.path + "/jsp/TSDIndexPage/viewDescription.jsp?view=" +FileServlet.VIEW_LOG + "&f=" + 
	        				logLinkTsrCreated + "&viewDescrSearchId=" + 
	        				searchId + "&" + RequestParams.SHOW_FILE_ID + "=true");
	        	} else{
	        		detailedReportData.setLogFileLink( "" );
	        	}
	        	
	        } else {
	            if(logFile.exists())
	            {
	                int idx = logLink.indexOf( BaseServlet.FILES_PATH );
	                if( idx >= 0 ) {
	                    logLink = logLink.substring( idx + BaseServlet.FILES_PATH.length()).replaceAll("\\\\", "/");
	                }
	            	detailedReportData.setLogFileLink(URLMaping.path + "/jsp/TSDIndexPage/viewDescription.jsp?view=" +FileServlet.VIEW_LOG + "&f=" + logLink + "logFile.html"
	            			+ "&viewOrder=1&userId=" + currentUser.getID().toString()+"&viewDescrSearchId="+searchId + 
	            			"&" + RequestParams.SHOW_FILE_ID + "=true");
	            } else if( logStatus != null && logStatus.intValue() == 1 ){
	        		detailedReportData.setLogFileLink("/title-search/jsp/TSDIndexPage/viewDescription.jsp?view=" +FileServlet.VIEW_LOG + "&viewOrder=1&userId=" + currentUser.getID().toString()+"&viewDescrSearchId="+searchId + 
	                		"&" + RequestParams.SHOW_FILE_ID + "=true");
	            } else{
	            	detailedReportData.setLogFileLink( "" );
	            
	            }
	        }
	        
        }
        
        if( indexStatus != null && indexStatus.intValue() == 1 ){
        	detailedReportData.setTsrIndexFileLink("/title-search/jsp/TSDIndexPage/viewDescription.jsp?view=" +FileServlet.VIEW_INDEX + "&viewOrder=1&userId=" + currentUser.getID().toString()+"&viewDescrSearchId="+searchId + "&" + RequestParams.SHOW_FILE_ID + "=true");
        } else if(tsrIndexFile.exists()){
            int idx = tsrIndexLink.indexOf( BaseServlet.FILES_PATH );
            if( idx >= 0 )
            {
                tsrIndexLink = tsrIndexLink.substring( idx + BaseServlet.FILES_PATH.length() );
                tsrIndexLink = tsrIndexLink.replaceAll("\\\\", "/");
            }
            tsrIndexLink = URLMaping.path + "/jsp/TSDIndexPage/viewDescription.jsp?f=" + tsrIndexLink + "tsrIndexFile.html"
    		+ "&viewOrder=1&userId=" + currentUser.getID().toString()+"&viewDescrSearchId="+searchId  +  "&" + RequestParams.SHOW_FILE_ID + "=true";
            detailedReportData.setTsrIndexFileLink(tsrIndexLink);
        } else if(tsrIndexFileTsrCreated != null && tsrIndexFileTsrCreated.exists()) {
        		detailedReportData.setTsrIndexFileLink("/title-search/jsp/TSDIndexPage/viewDescription.jsp?f=" + 
        				tsrIndexLinkTsrCreated + "&viewDescrSearchId=" + 
        				searchId + "&" + RequestParams.SHOW_FILE_ID + "=true");
    	} else {
    		// check for it into tsr_files
        	String server = DBManager.findTsrServer(tsrIndexLink);
        	if(server != null){
        		detailedReportData.setTsrIndexFileLink("/title-search/jsp/TSDIndexPage/viewDescription.jsp?f=" + 
        				tsrIndexLinkTsrCreated + "&viewOrder=1&userId=" + 
        				currentUser.getID().toString()+"&viewDescrSearchId="+
        				searchId+"&server="+server + "&" + RequestParams.SHOW_FILE_ID + "=true");
        	} else {                    		                    		
        		// check for it into tsr_files
        		detailedReportData.setTsrIndexFileLink( "" );
        	}
    	}

        if( !detailedReportData.getSearchFlags().isClosed() 
        		&& !detailedReportData.getSearchFlags().isTsrCreated()
        		&& CommAdminNotifier.isOldSearch( (String)data.getValue("tsr_timestamp", dbDataIndex),  defaultHoursBack) )
        {
            detailedReportData.getSearchFlags().setOld(true);
        }
        
        detailedReportData.getSearchFlags().setStatus((Integer)data.getValue(DBConstants.FIELD_SEARCH_STATUS, dbDataIndex));
		
	}



	private static  String escapeAmp(String what, String item){
    	int firstPoz = what.indexOf("&");
    	int lastPoz = what.indexOf(item);
    	if( firstPoz <lastPoz && firstPoz>0 && lastPoz>0 ){
    		what = what.replaceFirst("[&]", "%26");
    	}
    	return what;
    }
	
	public static int getIntervalReportDataCount(int[] countyId, int[] abstractorId, int[] agentId, 
            int[] stateId, String[] compName, int fromDay, int fromMonth, int fromYear, 
            int toDay, int toMonth, int toYear, String orderBy, 
            String orderType, int commId, int[] status, int invoice,int offset, int rowsPerPage, UserAttributes ua, int dateType){
        
        String sql = "call getDetailedReportAllInOne(?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
        DBConnection conn = null;
        
        int isTSAdmin = 0;
        
        try
        {
            if(ua.isTSAdmin())
            {
                isTSAdmin = 1;
            }
        }catch( Exception e ){}
        
        String abstractorIdString = Util.getStringFromArray(abstractorId);
        String agentIdString = Util.getStringFromArray(agentId);
        String countyIdString = Util.getStringFromArray(countyId);
        
        UserManagerI userManager = UserManager.getInstance();
        try {
			userManager.getAccess();
			UserRestrictionsI userRestrictions  = userManager.getUser(ua.getID().longValue()).getRestriction();
			
			if(userRestrictions.hasAbstractorAssigned()) {
				abstractorIdString = "";
				for (int i = 0; i < abstractorId.length; i++) {
					if(userRestrictions.isAbstractorAssigned(abstractorId[i])) {
						abstractorIdString += abstractorId[i] + ",";
					}
				}
				if(abstractorIdString.length() > 0) {
					abstractorIdString = abstractorIdString.substring(0, abstractorIdString.length() - 1);
				}
				
				if(abstractorIdString.length() == 0) {
					//auto select assigned abstractors
					abstractorIdString = Util.getStringFromArray(userRestrictions.getAllowedAbstractors());
				}
			}
			
			if(userRestrictions.hasAgentAllowed()) {
				agentIdString = "";
				for (int i = 0; i < agentId.length; i++) {
					if(userRestrictions.isAgentAllowed(agentId[i])) {
						agentIdString += agentId[i] + ",";
					}
				}
				if(agentIdString.length() > 0) {
					agentIdString = agentIdString.substring(0, agentIdString.length() - 1);
				}
				
				if(agentIdString.length() == 0) {
					//auto select assigned abstractors
					agentIdString = Util.getStringFromArray(userRestrictions.getAllowedAgentsAsArray());
				}
			}
			
		} catch (Throwable t) {
			logger.error("Error while trying to enforce restriction rules for reports for user " + ua.getID(), t);
		} finally {
			userManager.releaseAccess();
		}
		
		if(ua.isTSAdmin() || ua.isCommAdmin()) {
			//we do not care
		} else {
			Vector<County> allAllowedCounties = ua.getAllowedCountyList();
			if(allAllowedCounties.size() > 0) {	//we have something set here
				HashSet<Integer> allAllowedCountyIds = new HashSet<Integer>();
				int[] allAllowedCountiesInt = new int[allAllowedCounties.size()];
				int i = 0;
				for (County county : allAllowedCounties) {
					allAllowedCountyIds.add(county.getCountyId().intValue());
					allAllowedCountiesInt[i++] = county.getCountyId().intValue();
				}
				countyIdString = "";
				for (i = 0; i < countyId.length; i++) {
					if(allAllowedCountyIds.contains(countyId[i])) {
						countyIdString += countyId[i] + ",";
					}
				}
				if(countyIdString.length() > 0) {
					countyIdString = countyIdString.substring(0, countyIdString.length() - 1);
				}
				
				if(countyIdString.length() == 0) {
					//auto select assigned abstractors
					countyIdString = Util.getStringFromArray(allAllowedCountiesInt);
				}
			}
		}
        
        
        try {
            
            conn = ConnectionPool.getInstance().requestConnection();
            
            NewProxyCallableStatement cs = (NewProxyCallableStatement)conn.prepareCall(sql);
            cs.setString(1, "," + countyIdString + ",");
            cs.setString(2, "," + abstractorIdString + ",");
            cs.setString(3, "," + agentIdString + ",");
            cs.setString(4, "," + Util.getStringFromArray(stateId) + ",");
            cs.setString(5,StringUtils.convertStringToHexString("," +  Util.getStringFromStringArray(compName) + ","));
            cs.setInt(6, fromDay);
            cs.setInt(7, fromMonth);
            cs.setInt(8, fromYear);
            cs.setInt(9, toDay);
            cs.setInt(10, toMonth);
            cs.setInt(11, toYear);
            cs.setString(12, orderBy);
            cs.setString(13, orderType);
            cs.setInt(14, commId);
            cs.setString(15, "," + Util.getStringFromArray(status) + ",");
            cs.setInt(16, invoice);
            cs.setInt(17, isTSAdmin);
            cs.setInt(18, offset);
            cs.setInt(19, rowsPerPage);
            cs.setInt(20, 1);
            cs.setInt(21, ua.getID().intValue());
            cs.setInt(22, dateType);
            long startTime = System.currentTimeMillis();
            DatabaseData data = conn.executeCallableStatementWithResult(cs);
            logger.debug("getIntervalReportDataCount sql took about " + (System.currentTimeMillis()-startTime) + " millis");
            // ICM8 - 3.5.61
            return ((Long)data.getValue(1,0)).intValue();
        } catch (Exception e) {
            logger.error(e);
            return -1;
        }
        finally{
            try{
                ConnectionPool.getInstance().releaseConnection(conn);
            }catch(BaseException e){
                logger.error(e);
            }           
        }
        
    }
	
	public static ReportLineData[] getTableReportData(int[] countyId, int[] abstractorId, int[] agentId, 
            int[] stateId, String[] compName,  
            Calendar fromCalendar, Calendar toCalendar,
            int commId, UserAttributes ua, INTERVAL_TYPES intervalType, Integer currentShortReportInterval) {
		TableReportProcedure trp = (TableReportProcedure)ProcedureManager.getInstance().getProcedure(TableReportProcedure.SP_NAME);
		String abstractorIdString = Util.getStringFromArray(abstractorId);
        String agentIdString = Util.getStringFromArray(agentId);
        String countyIdString = Util.getStringFromArray(countyId);
        
        UserManagerI userManager = UserManager.getInstance();
        try {
			userManager.getAccess();
			UserRestrictionsI userRestrictions  = userManager.getUser(ua.getID().longValue()).getRestriction();
			abstractorIdString = userRestrictions.getAbstractorListForSql(abstractorId);
			agentIdString = userRestrictions.getAgentListForSql(agentId);
			
		} catch (Throwable t) {
			logger.error("Error while trying to enforce restriction rules for reports for user " + ua.getID(), t);
		} finally {
			userManager.releaseAccess();
		}
		
		if(ua.isTSAdmin() || ua.isCommAdmin()) {
			//we do not care
		} else {
			Vector<County> allAllowedCounties = ua.getAllowedCountyList();
			if(allAllowedCounties.size() > 0) {	//we have something set here
				HashSet<Integer> allAllowedCountyIds = new HashSet<Integer>();
				int[] allAllowedCountiesInt = new int[allAllowedCounties.size()];
				int i = 0;
				for (County county : allAllowedCounties) {
					allAllowedCountyIds.add(county.getCountyId().intValue());
					allAllowedCountiesInt[i++] = county.getCountyId().intValue();
				}
				countyIdString = "";
				for (i = 0; i < countyId.length; i++) {
					if(allAllowedCountyIds.contains(countyId[i])) {
						countyIdString += countyId[i] + ",";
					}
				}
				if(countyIdString.length() > 0) {
					countyIdString = countyIdString.substring(0, countyIdString.length() - 1);
				}
				
				if(countyIdString.length() == 0) {
					//auto select assigned abstractors
					countyIdString = Util.getStringFromArray(allAllowedCountiesInt);
				}
			}
		}
		
		
		return trp.execute("," + countyIdString + ",", 
				"," + abstractorIdString + ",", 
				"," + agentIdString + ",", 
				"," + Util.getStringFromArray(stateId) + ",", 
				StringUtils.convertStringToHexString("," +  Util.getStringFromStringArray(compName) + ","), 
				fromCalendar, toCalendar, commId, ua.isTSAdmin(), intervalType, currentShortReportInterval);
	}
	
	private static final String SQL_REPORT_STATUS = 
		"SELECT af." + DBConstants.FIELD_SEARCH_FLAGS_ARCHIVED +
		", " + DBConstants.FIELD_SEARCH_FLAGS_CONFIRMED + 
		", " + DBConstants.FIELD_SEARCH_FLAGS_WAS_OPENED + 
		", " + DBConstants.FIELD_SEARCH_FLAGS_PAID + 
		", " + DBConstants.FIELD_SEARCH_TSR_DATE +  
		", DATE_FORMAT(a." + DBConstants.FIELD_SEARCH_SDATE + ", '%e-%c-%Y %H:%i:%S') " + DBConstants.FIELD_SEARCH_SDATE +
		", " + DBConstants.FIELD_SEARCH_STATUS + 
		", " + DBConstants.FIELD_SEARCH_FLAGS_INVOICED + 
		", STATUS_SHORT_NAME" +
		", af." + DBConstants.FIELD_SEARCH_FLAGS_IS_CLOSED + 
		" FROM " + DBConstants.TABLE_SEARCH + 
		" a JOIN " + DBConstants.TABLE_SEARCH_FLAGS +
		" af ON a." + DBConstants.FIELD_SEARCH_ID + 
		" = af." + DBConstants.FIELD_SEARCH_FLAGS_ID + 
		" JOIN ts_search_status ss ON ss.STATUS_ID = a." + DBConstants.FIELD_SEARCH_STATUS +
		" WHERE a." + DBConstants.FIELD_SEARCH_ID + " = ?";
	
	private static final String SQL_REPORT_STATUS_CADM = 
		"SELECT af." + DBConstants.FIELD_SEARCH_FLAGS_ARCHIVED +
		", " + DBConstants.FIELD_SEARCH_FLAGS_CONFIRMED + 
		", " + DBConstants.FIELD_SEARCH_FLAGS_WAS_OPENED + 
		", " + DBConstants.FIELD_SEARCH_FLAGS_PAID_CADM + " " + DBConstants.FIELD_SEARCH_FLAGS_PAID + 
		", " + DBConstants.FIELD_SEARCH_TSR_DATE +  
		", DATE_FORMAT(a." + DBConstants.FIELD_SEARCH_SDATE + ", '%e-%c-%Y %H:%i:%S') " + DBConstants.FIELD_SEARCH_SDATE +
		", " + DBConstants.FIELD_SEARCH_STATUS + 
		", " + DBConstants.FIELD_SEARCH_FLAGS_INVOICED_CADM + " " + DBConstants.FIELD_SEARCH_FLAGS_INVOICED + 
		", STATUS_SHORT_NAME" +
		", af." + DBConstants.FIELD_SEARCH_FLAGS_IS_CLOSED + 
		" FROM " + DBConstants.TABLE_SEARCH + 
		" a JOIN " + DBConstants.TABLE_SEARCH_FLAGS +
		" af ON a." + DBConstants.FIELD_SEARCH_ID + 
		" = af." + DBConstants.FIELD_SEARCH_FLAGS_ID + 
		" JOIN ts_search_status ss ON ss.STATUS_ID = a." + DBConstants.FIELD_SEARCH_STATUS +
		" WHERE a." + DBConstants.FIELD_SEARCH_ID + " = ?";
		
	public static String getReportStatus(long searchId, long crtContextSearchId){
		if(searchId == -2)
			return "";
		try {
			CurrentInstance currentInstance = InstanceManager.getManager().getCurrentInstance(crtContextSearchId);
			
			CommunityAttributes communityAttributes = currentInstance.getCurrentCommunity();;
			int defaultHoursBack = communityAttributes.getDEFAULTSLA().intValue();
			boolean isTSAdmin = currentInstance.getCurrentUser().isTSAdmin();
			
			Map<String, Object> results = DBManager.getSimpleTemplate().queryForMap(
					isTSAdmin?SQL_REPORT_STATUS:SQL_REPORT_STATUS_CADM, searchId);
			DayReportLineData day = new DayReportLineData();
			
			if(results.get(DBConstants.FIELD_SEARCH_FLAGS_CONFIRMED) != null)
				day.setConfirmed(((Long)results.get(DBConstants.FIELD_SEARCH_FLAGS_CONFIRMED)).intValue());
			if(results.get(DBConstants.FIELD_SEARCH_FLAGS_ARCHIVED) != null)
				day.setArchived(((Long)results.get(DBConstants.FIELD_SEARCH_FLAGS_ARCHIVED)).intValue());
			if(results.get(DBConstants.FIELD_SEARCH_FLAGS_WAS_OPENED) !=null )
				day.setWasOpened((Integer)results.get(DBConstants.FIELD_SEARCH_FLAGS_WAS_OPENED)==1?true:false);
			if(results.get(DBConstants.FIELD_SEARCH_FLAGS_PAID) != null)
				day.setPaid(((Long)results.get(DBConstants.FIELD_SEARCH_FLAGS_PAID)).intValue());
			if(results.get(DBConstants.FIELD_SEARCH_TSR_DATE) != null)
				day.setTsrTimeStamp((Date)results.get(DBConstants.FIELD_SEARCH_TSR_DATE));
			if(results.get(DBConstants.FIELD_SEARCH_FLAGS_INVOICED) != null)
				day.setInvoiced(((Long)results.get(DBConstants.FIELD_SEARCH_FLAGS_INVOICED)).intValue());
			
			if(results.get(DBConstants.FIELD_SEARCH_STATUS) != null) {
				if ((Integer)results.get(DBConstants.FIELD_SEARCH_STATUS) != DBConstants.SEARCH_NOT_SAVED) {
					
					SearchFlags sf = day.getSearchFlags();
					
					sf.setStatus((Integer)results.get(DBConstants.FIELD_SEARCH_STATUS));
					sf.setClosed((Boolean)results.get(DBConstants.FIELD_SEARCH_FLAGS_IS_CLOSED));
					if( CommAdminNotifier.isOldSearch( 
							(String)results.get(DBConstants.FIELD_SEARCH_SDATE), defaultHoursBack) 
							&& !sf.isClosed())
						sf.setOld(true);
				} else {
					day.getSearchFlags().setStatus((Integer)results.get(DBConstants.FIELD_SEARCH_STATUS));
				}
			}
					
			return day.getStatus();
		} catch (EmptyResultDataAccessException e) {

		} catch (Exception e) {
			e.printStackTrace();
		}
		
		return "";
	}
	
	private static final String SQL_NAMES_FOR_SEARCH_IDS = 
    	"SELECT * FROM @@table_name@@ WHERE searchId in (?)";
    public static List<NameMapper> getNamesForSearchIdsFromTable(String searchIdList, String tableName){
    	List<NameMapper> names = new ArrayList<NameMapper>();
    	try {
			names = DBManager.getSimpleTemplate().query(
					SQL_NAMES_FOR_SEARCH_IDS.replace("@@table_name@@", tableName).replace("?", searchIdList), 
					new NameMapper());
		} catch (Exception e) {
			e.printStackTrace();
		}
		return names;
    }

	public static boolean hasOrderInDatabase(long searchId) {
		try {
			return DBManager.getSimpleTemplate().queryForInt(
					"select count(*) from "
							+ DBConstants.TABLE_SEARCH_FLAGS + " where "
							+ DBConstants.FIELD_SEARCH_FLAGS_ID + " = ? and "
							+ DBConstants.FIELD_SEARCH_FLAGS_ORDER_STATUS + " = ? ", searchId, 1)
			== 1;
		} catch (Exception e) {
			logger.error("Cannot get hasOrderInDatabase for searchId " + searchId, e);
			return false;
		}
	}

}
