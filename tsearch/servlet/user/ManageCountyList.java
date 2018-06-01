package ro.cst.tsearch.servlet.user;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.math.BigDecimal;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Vector;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import ro.cst.tsearch.data.CountyStateUser;
import ro.cst.tsearch.data.RateCSVLine;
import ro.cst.tsearch.data.User;
import ro.cst.tsearch.database.DBUser;
import ro.cst.tsearch.database.rowmapper.UserFilterMapper;
import ro.cst.tsearch.generic.Util;
import ro.cst.tsearch.servers.parentsite.County;
import ro.cst.tsearch.servlet.BaseServlet;
import ro.cst.tsearch.user.GroupAttributes;
import ro.cst.tsearch.user.UserAttributes;
import ro.cst.tsearch.user.UserManager;
import ro.cst.tsearch.user.UserUtils;
import ro.cst.tsearch.utils.FormatDate;
import ro.cst.tsearch.utils.InstanceManager;
import ro.cst.tsearch.utils.ParameterParser;
import ro.cst.tsearch.utils.RequestParams;
import ro.cst.tsearch.utils.SessionParams;
import ro.cst.tsearch.utils.StringUtils;
import ro.cst.tsearch.utils.URLMaping;

import com.Ostermiller.util.CSVPrinter;
import com.stewart.ats.user.UserFilters;
import com.stewart.ats.user.UserManagerI;

/**
 * This Class should map all actions currently implemented by User Assign Rules Module
 * 
 * @author AndreiA
 *
 */
public class ManageCountyList extends BaseServlet {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	public static final String SET_ALLOWED_COUNTIES				= "0";
	public static final String SET_PAYRATE_A2C					= "1";	
	public static final String RESET_ALLOW_ALL_COUNTIES			= "2";
	public static final String EXPORT_ALLOWED_COUNTIES			= "3";
	public static final String EXPORT_PAYRATES					= "4";
	public static final String SET_PAYRATE_C2A					= "5";
	public static final String SET_PRODUCTS						= "6";
	public static final String SET_CATEGORIES_AND_SUBCATEGORIES	= "7";
	public static final String SET_ALL_FILTERS					= "8";
	public static final String RESET_WARNINGS						= "9";
	public static final String RESET_PRODUCTS						= "10";
	public static final String RESET_CATEGORIES_AND_SUBCATEGORIES	= "11";
	public static final String RESET_ALL_FILTERS					= "12";
	public static final String SET_WARNINGS						= "13";
	public static final String EXPORT_ALLOWED_PRODUCTS			= "14";
	public static final String EXPORT_ALLOWED_WARNINGS			= "15";
	public static final String EXPORT_ALLOWED_CATEG_AND_SUBCATEG	= "16";
	public static final String EXPORT_ALL						= "17";
	
	public static final String RESET_ALL_RESTRICTIONS			= "18";
	public static final String RESET_ALL_RATES					= "19";
	public static final String SET_ABSTRACTORS					= "20";
	public static final String RESET_ABSTRACTORS				= "21";
	public static final String SET_AGENTS						= "22";
	public static final String RESET_AGENTS						= "23";
	
	public static final String SET_ORDERS_COUNTIES				= "24";
	public static final String SET_ORDERS_AGENTS				= "25";
	public static final String RESET_ORDERS_COUNTIES			= "26";
	public static final String RESET_ORDERS_AGENTS				= "27";
	
	public static final String EXPORT_ALL_RESTRICTIONS			= "28";
	public static final String EXPORT_ALL_RATES_A2C				= "29";
	public static final String EXPORT_ALL_ASSIGNS				= "30";
	public static final String EXPORT_ALL_RATES_C2A				= "31";
	public static final String EXPORT_CSV_RATES_A2C				= "32";
	public static final String EXPORT_CSV_RATES_C2A				= "33";
	
	public static final String TITLE_MANAGE_COUNTY_LIST			= "Orders and Rates Assign Rules";
	
	
	
	public void doRequest(HttpServletRequest request, HttpServletResponse response) 
		throws IOException, ServletException {
		
		ParameterParser parameterParser = new ParameterParser(request);
		String searchId = request.getParameter(RequestParams.SEARCH_ID);
		String opCodeString = parameterParser.getStringParameter("operation");
		String destination = request.getParameter("destination");
		if(StringUtils.isEmpty(destination)) {
			destination = URLMaping.USER_MANAGE_COUNTY;
		}
		
		if(StringUtils.isEmpty(opCodeString)){
		
			if(destination.contains("?")) {
				destination += "&searchId=" + searchId;
			} else {
				destination += "?searchId=" + searchId;
			}
			forward(request, response, destination );
			return;
		}
		
		
		String[] countyIdsList = null;	//request.getParameterValues( "countyIdList" );
		
		int[] userSelectIntArray = parameterParser.getIntParameters("reportUser");
		int[] countySelectIntArray = parameterParser.getIntParameters("reportCounty");
		List<Long> userSelectVector = new Vector<Long>();
		int commId = -1;
		try {
			commId = InstanceManager.getManager().
				getCurrentInstance(Long.parseLong(searchId)).getCurrentCommunity().getID().intValue();

		} catch(Exception e){
			e.printStackTrace();
		}
		
		UserManagerI userManagerI = com.stewart.ats.user.UserManager.getInstance();
		
		try {
			userManagerI.getAccess();
			if(parameterParser.getIntParameter("pageType") == 1) {
				userSelectVector = userManagerI.validateUsersForAssignModule(userSelectIntArray, commId,
						GroupAttributes.ABS_ID,
						GroupAttributes.CA_ID,
						GroupAttributes.CCA_ID,
						GroupAttributes.TA_ID );
			} else {
				userSelectVector = userManagerI.validateUsersForAssignModule(userSelectIntArray, commId,
						GroupAttributes.ABS_ID,
						GroupAttributes.CA_ID,
						GroupAttributes.CCA_ID,
						GroupAttributes.TA_ID,
						GroupAttributes.AG_ID);
			}
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			userManagerI.releaseAccess();
		}
		
		
		String userId = request.getParameter(UserAttributes.USER_ID);
		long userIdLong = 0;
		
		User currentUser = (User) request.getSession().getAttribute(SessionParams.CURRENT_USER);
		UserAttributes crtUser = currentUser.getUserAttributes();
		UserAttributes configuredUser = null;

		Vector<Long> toRefresh = new Vector<Long>();
		toRefresh.addAll(userSelectVector);
		
		
		//------------------------------------------------------
		String operationStr = request.getParameter( "operation" );
		if( operationStr == null ){
			operationStr = "0";
		}
		
		String allowedFilter = request.getParameter( "allowedFilter" );
		if( allowedFilter == null){
			allowedFilter = "-1";
		}
		
		String ajaxCall = request.getParameter("ajaxCall");
		if( ajaxCall == null ){
			ajaxCall = "";
		}
		
		String[] stateIds = request.getParameterValues( "reportState" );
		if( stateIds == null ){
			stateIds = new String[1];
			stateIds[0] = "-2";
		}
		
		int[] stateIdsInt = new int[stateIds.length];
		for( int i = 0 ; i < stateIds.length ; i ++ ) {
			stateIdsInt[i] = -1;
			try
			{
				stateIdsInt[i] = Integer.parseInt( stateIds[i] );
			}catch( NumberFormatException nfe )	{}
		}
		
		
		
		
		String[] userIds = request.getParameterValues( "reportUser" );
		if( userIds == null ){
			userIds = new String[1];
			userIds[0] = "-2";
		}
		
		int[] userIdsInt = new int[userIds.length];
		
		for(int i = 0 ; i < userIds.length ; i ++ )
		{
			userIdsInt[i] = -2;
			try
			{
				userIdsInt[i] = Integer.parseInt( userIds[i] );
			}catch( NumberFormatException nfe )	{}
		}
		
		
		String[] c2aValuesSelected = parameterParser.getStringParameters("c2arates", new String[]{"-1"});
		String[] a2cValuesSelected = parameterParser.getStringParameters("a2crates", new String[]{"-1"});
		
		String[] countiesFilterIds = parameterParser.getStringParameters("reportCounty", new String[]{"-1"});
		
		if( countiesFilterIds == null ){
			countiesFilterIds = new String[1];
			countiesFilterIds[0] = "-2";
		}
		
		int[] countiesFilterIdsInt = new int[countiesFilterIds.length];
		
		for(int i = 0 ; i < countiesFilterIds.length ; i ++ )
		{
			countiesFilterIdsInt[i] = -2;
			try
			{
				countiesFilterIdsInt[i] = Integer.parseInt( countiesFilterIds[i] );
			}catch( NumberFormatException nfe )	{}
		}
		
		List<CountyStateUser> allCounties = DBUser.getAllCountiesForStateRatesFilter(stateIdsInt, countiesFilterIdsInt, 
				c2aValuesSelected, a2cValuesSelected, userIdsInt, commId, "countyName", "", 0);
		
		countyIdsList = new String[allCounties.size()];
		for (int i = 0; i < allCounties.size(); i++) {
			countyIdsList[i] = allCounties.get(i).getCountyUserKey();
		}
		//------------------------------------------------------
		
		
		
		
		
		

		if(opCodeString==null){
//----start restriction page			
		} else if(opCodeString.equals(SET_ALLOWED_COUNTIES) ){
			
			for (Long item : userSelectVector) {
				DBUser.resetAllowedCountiesForUser(item.toString());
				configuredUser = UserManager.getUser(new BigDecimal( item ) );
				configuredUser.loadAllowedCounties();	
			}
			
			if( countyIdsList != null ) {
				
				HashMap<Integer, Vector<String>> deletedCountiesIds = new HashMap<Integer, Vector<String>>();
				for( int j = 0 ; j < countyIdsList.length ; j ++ )
				{
					String[] parts = countyIdsList[j].split("_");
					int tmpCountyId = -1;
					int tmpUserId = -1;
					try
					{
						tmpCountyId = Integer.parseInt( parts[0] );
						tmpUserId = Integer.parseInt( parts[1] );
						configuredUser = UserUtils.getUserFromId( new BigDecimal( tmpUserId ) );
						
						
						
					}catch( Exception e ) {
						continue;
					}
					/*
					boolean checked = request.getParameter( "countyIdCheck_" + countyIdsList[j] ) != null;
					if(!checked){
						Vector<String> countiesForThisUser = deletedCountiesIds.get(tmpUserId);
						if(countiesForThisUser == null) {
							countiesForThisUser = new Vector<String>();
							deletedCountiesIds.put(tmpUserId, countiesForThisUser);
						}
						countiesForThisUser.add( parts[0] );
						continue;
					}
					*/
					//if( checked ) {
						DBUser.addAllowedCounty( configuredUser.getID(), new BigDecimal( tmpCountyId ) );
						try {
							configuredUser.getAllowedCountyList().add( County.getCounty( tmpCountyId ) );
						} catch (Exception e) {
							e.printStackTrace();
						}
					//}
				}
				
				
				for (Integer configuredUserIds : deletedCountiesIds.keySet()) {
					Vector<String> deletedCounties = deletedCountiesIds.get(configuredUserIds);
					configuredUser = UserManager.getUser(new BigDecimal( configuredUserIds ) );
					if(deletedCounties != null && deletedCounties.size() > 0) {
						if (deletedCounties.indexOf(configuredUser.getMyAtsAttributes().getReportCounty()) > -1){
							configuredUser.getMyAtsAttributes().setReportState("-1");
							configuredUser.getMyAtsAttributes().setReportState("-1");
							try {
								UserManager.updateUserDefaultFilterCounties(configuredUser, Long.parseLong(searchId));
							} catch (Exception e) {
								e.printStackTrace();
							}
						}
						//delete stored counties
						if (deletedCounties.indexOf(configuredUser.getMyAtsAttributes().getSEARCH_PAGE_COUNTY().toString()) > -1){
							configuredUser.getMyAtsAttributes().setSEARCH_PAGE_COUNTY(new BigDecimal(-1));
							configuredUser.getMyAtsAttributes().setSEARCH_PAGE_STATE(new BigDecimal(-1));
							try {
								UserManager.updateUserDefaultSearchPageCounties(configuredUser, Long.parseLong(searchId));
							} catch (Exception e) {
								e.printStackTrace();
							}
						}
						DBUser.deleteAllowedCountyListForUser( configuredUserIds.toString(), deletedCounties );
					}
					configuredUser.loadAllowedCounties();
				}
			}
			destination = URLMaping.USER_RESTRICTIONS_JSP;
		} else if(opCodeString.equals(RESET_ALLOW_ALL_COUNTIES) ){
			for (Long item : userSelectVector) {
				DBUser.resetAllowedCountiesForUser(item.toString());
				configuredUser = UserManager.getUser(new BigDecimal( item ) );
				configuredUser.loadAllowedCounties();	
			}
			destination = URLMaping.USER_RESTRICTIONS_JSP;
		} else if(opCodeString.equals(RESET_ALL_RESTRICTIONS) ){
			for (Long item : userSelectVector) {
				DBUser.resetAllowedCountiesForUser(item.toString());
				configuredUser = UserManager.getUser(new BigDecimal( item ) );
				configuredUser.loadAllowedCounties();	
			}
			
			destination = URLMaping.USER_RESTRICTIONS_JSP;
			UserFilterMapper.clearAttributes(userSelectVector, UserFilters.TYPE_RESTRICTION_ABSTRACTOR);
			UserFilterMapper.clearAttributes(userSelectVector, UserFilters.TYPE_RESTRICTION_AGENT);
		} else if(opCodeString.equals(RESET_ABSTRACTORS) ){
			UserFilterMapper.clearAttributes(userSelectVector, UserFilters.TYPE_RESTRICTION_ABSTRACTOR);
			destination = URLMaping.USER_RESTRICTIONS_JSP;
		} else if(opCodeString.equals(RESET_AGENTS) ){
			UserFilterMapper.clearAttributes(userSelectVector, UserFilters.TYPE_RESTRICTION_AGENT);
			destination = URLMaping.USER_RESTRICTIONS_JSP;
		} else if(opCodeString.equals(EXPORT_ALLOWED_COUNTIES) ){
    		String selectedUserID = request.getParameter(RequestParams.USER_COUNTY_EXPORT_USER_TO);
    		UserAttributes selectedUser = null;
    		if(selectedUserID!=null && !selectedUserID.equals("-1")) {
				try {
					selectedUser = UserUtils.getUserFromId( new BigDecimal( selectedUserID ) );
					DBUser.copyAllowedCounties(userId,selectedUserID);
					selectedUser.loadAllowedCounties();		
				} catch (Exception e) {
					e.printStackTrace();
				}
    		}
    		destination = URLMaping.USER_RESTRICTIONS_JSP;
    		
    		
		} else if(opCodeString.equals(EXPORT_ALL_RESTRICTIONS) ){
    		String[] selectedUserID = request.getParameterValues(RequestParams.USER_COUNTY_EXPORT_USER_TO);
    		userId = request.getParameter(RequestParams.USER_COUNTY_EXPORT_USER_FROM);
    		userIdLong = Long.parseLong(userId);
    		UserAttributes selectedUser = null;
    		if(selectedUserID != null) {
    			for (int i = 0; i < selectedUserID.length; i++) {
					if(selectedUserID[i] != null) {
						try {
							long selectedUserIdLong = Long.parseLong(selectedUserID[i]);
							if(selectedUserIdLong != -1) {
								selectedUser = UserUtils.getUserFromId( new BigDecimal( selectedUserIdLong ) );
								DBUser.copyAllowedCounties(userId,selectedUserID[i]);
								selectedUser.loadAllowedCounties();
								
								UserFilterMapper.clearAttributes(selectedUserIdLong, UserFilters.TYPE_RESTRICTION_AGENT);
					    		UserFilterMapper.clearAttributes(selectedUserIdLong, UserFilters.TYPE_RESTRICTION_ABSTRACTOR);
					    		UserFilterMapper.copyAllowedFilter(userIdLong,selectedUserIdLong, UserFilters.TYPE_RESTRICTION_AGENT);		
					    		UserFilterMapper.copyAllowedFilter(userIdLong,selectedUserIdLong, UserFilters.TYPE_RESTRICTION_ABSTRACTOR);	
					    		toRefresh.add(selectedUserIdLong);
							}
						} catch (Exception e) {
							e.printStackTrace();
						}
					}
				}
	    		
    		}
    		
    		
    		destination = URLMaping.USER_RESTRICTIONS_JSP;
//----- end restriction page	
    		
    		
    		
//----- start rates page			
		} else if(opCodeString.equals(SET_PAYRATE_A2C) ){
		
			if( countyIdsList != null )
			{
				String a2cRate = request.getParameter( "a2crate");

				for( int j = 0 ; j < countyIdsList.length ; j ++ )
				{
					String[] parts = countyIdsList[j].split("_");
					int tmpCountyId = -1;
					int tmpUserId = -1;
					try	{
						tmpCountyId = Integer.parseInt( parts[0] );
						tmpUserId = Integer.parseInt( parts[1] );
						configuredUser = UserUtils.getUserFromId( new BigDecimal( tmpUserId ) );
					} catch( Exception e ) {
						continue;
					}
					//boolean checked = request.getParameter( "countyIdCheck_" + countyIdsList[j] ) != null;										
					//if( checked )
					//{
						DBUser.setATS2CommunityRating(crtUser, configuredUser, a2cRate, tmpCountyId);
					//}	
				}
				
			}
			destination = URLMaping.USER_RATES_JSP;
		} else if(opCodeString.equals(SET_PAYRATE_C2A) ){
			
			if( countyIdsList != null )
			{
				String rate = request.getParameter( "c2arate");

				for( int j = 0 ; j < countyIdsList.length ; j ++ )
				{
					String[] parts = countyIdsList[j].split("_");
					int tmpCountyId = -1;
					int tmpUserId = -1;
					try	{
						tmpCountyId = Integer.parseInt( parts[0] );
						tmpUserId = Integer.parseInt( parts[1] );
						configuredUser = UserUtils.getUserFromId( new BigDecimal( tmpUserId ) );
					} catch( Exception e ) { continue;}
					//boolean checked = request.getParameter( "countyIdCheck_" + countyIdsList[j] ) != null;										
					//if( checked )
					//{
						DBUser.setCommunity2AgentRating(crtUser, configuredUser, rate, tmpCountyId);
					//}	
				}
				
			}
			destination = URLMaping.USER_RATES_JSP;

    	} else if(opCodeString.equals(EXPORT_ALL_RATES_A2C)) {
    		String[] selectedUserID = request.getParameterValues(RequestParams.USER_COUNTY_EXPORT_USER_TO);
    		userId = request.getParameter(RequestParams.USER_COUNTY_EXPORT_USER_FROM);
    		userIdLong = Long.parseLong(userId);
    		if(selectedUserID != null) {
    			UserManagerI userManager = com.stewart.ats.user.UserManager.getInstance();
    			try {
    				userManager.getAccess();
    				for (int i = 0; i < selectedUserID.length; i++) {
    					if(selectedUserID[i] != null) {
    						try {
    							long selectedUserIdLong = Long.parseLong(selectedUserID[i]);
    							if(selectedUserIdLong != -1) {
    								userManager.exportRates(userIdLong, selectedUserIdLong, true);
    							}
    						} catch (Exception e) {
    							e.printStackTrace();
    						}
    					}
    				}
    			} catch (Exception e) {
					logger.error("Error while exporting rates from user " + userIdLong, e);
				} finally {
					if(userManager != null) {
						userManager.releaseAccess();
					}
				}
    			
    		}
    	} else if(opCodeString.equals(EXPORT_ALL_RATES_C2A)) {
    		String[] selectedUserID = request.getParameterValues(RequestParams.USER_COUNTY_EXPORT_USER_TO);
    		userId = request.getParameter(RequestParams.USER_COUNTY_EXPORT_USER_FROM);
    		if(selectedUserID != null && StringUtils.isNotEmpty(userId)) {
    			userIdLong = Long.parseLong(userId);
    			UserManagerI userManager = com.stewart.ats.user.UserManager.getInstance();
    			try {
    				userManager.getAccess();
    				for (int i = 0; i < selectedUserID.length; i++) {
    					if(selectedUserID[i] != null) {
    						try {
    							long selectedUserIdLong = Long.parseLong(selectedUserID[i]);
    							if(selectedUserIdLong != -1) {
    								userManager.exportRates(userIdLong, selectedUserIdLong, false);
    							}
    						} catch (Exception e) {
    							e.printStackTrace();
    						}
    					}
    				}
    			} catch (Exception e) {
					logger.error("Error while exporting rates from user " + userIdLong, e);
				} finally {
					if(userManager != null) {
						userManager.releaseAccess();
					}
				}
    			
    		}
    	}  else if(opCodeString.equals(EXPORT_CSV_RATES_C2A)) {
    		
    		List<RateCSVLine> rawLines = RateCSVLine.loadInfoFor(
    				stateIdsInt, 
    				commId, 
    				false, countySelectIntArray, userSelectIntArray);
			
			
			String expFileName = "RATES_C2A_" + new SimpleDateFormat(FormatDate.PATTERN_MMddyyyy_HHmmss).format(new Date());
			String fileFormat = "csv";
			String rootPath = BaseServlet.FILES_PATH + File.separator + "tempZipFolder" + File.separator;
			
			FileWriter pw = null;
			try {
				pw = new FileWriter(rootPath + expFileName + "." + fileFormat);
				CSVPrinter printer = new CSVPrinter(pw);
				printer.println(RateCSVLine.getHeader(false, commId));
				for (RateCSVLine rateCSVLine : rawLines) {
					printer.println(rateCSVLine.getLineForFile());
				}
			} catch (Exception e) {
				e.printStackTrace();
			} finally {
	            try {
	                pw.close();
	            } catch (Exception e){}
	        }
		
		
			destination = URLMaping.DownloadFileAs + "?" + "pdfFile=" + File.separator + "tempZipFolder" + File.separator + expFileName + "." + fileFormat;
    	}  else if(opCodeString.equals(EXPORT_CSV_RATES_A2C)) {
    		
    		
    		List<RateCSVLine> rawLines = RateCSVLine.loadInfoFor(
    				stateIdsInt, 
    				commId,
    				true, countySelectIntArray, userSelectIntArray);
			
			
    		String expFileName = "RATES_A2C_" + new SimpleDateFormat(FormatDate.PATTERN_MMddyyyy_HHmmss).format(new Date());
			String fileFormat = "csv";
			String rootPath = BaseServlet.FILES_PATH + File.separator + "tempZipFolder" + File.separator;

			
			FileWriter pw = null;
			try {
				pw = new FileWriter(rootPath + expFileName + "." + fileFormat);
				CSVPrinter printer = new CSVPrinter(pw);
				printer.println(RateCSVLine.getHeader(true, commId));
				for (RateCSVLine rateCSVLine : rawLines) {
					printer.println(rateCSVLine.getLineForFile());
				}
			} catch (Exception e) {
				e.printStackTrace();
			} finally {
	            try {
	                pw.close();
	            } catch (Exception e){}
	        }
		
		
			destination = URLMaping.DownloadFileAs + "?" + "pdfFile=" + File.separator + "tempZipFolder" + File.separator + expFileName + "." + fileFormat;
    	}
		//---- end rate page
		
		//---- start orders assign page
		
		else if(opCodeString.equals(SET_PRODUCTS) ){
    		int[] products = parameterParser.getIntParameters(RequestParams.SEARCH_PRODUCT_TYPE, new int[]{-1});
    		UserFilterMapper.clearAttributes(userSelectVector, UserFilters.TYPE_ASSIGN_ORDERS_PRODUCT);
    		if(!Util.isValueInArray(-1, products)) {
	    		UserFilterMapper ufm = new UserFilterMapper();
	    		
	    		ufm.setType(UserFilters.TYPE_ASSIGN_ORDERS_PRODUCT);
	    		for (Long userSelect : userSelectVector) {
	    			ufm.setUserId(userSelect);
	    			for (int i = 0; i < products.length; i++) {
		    			ufm.setId(null);	//clear id so a new object will be inserted and not just updated
						ufm.setValueLong((long)products[i]);
						if(!ufm.save()) {
							logger.error("Could not save object: " + ufm);
						}
					}
				}
	    		
    		}
    		destination = URLMaping.USER_ASSIGN_JSP;
    	} else if(opCodeString.equals(SET_ABSTRACTORS) ){
    		int[] idsToSet = parameterParser.getIntParameters(RequestParams.REPORTS_ABSTRACTOR, new int[]{-1});
    		UserFilterMapper.clearAttributes(userSelectVector, UserFilters.TYPE_RESTRICTION_ABSTRACTOR);
    		for (Long userSelectId : userSelectVector) {
	    		if(!Util.isValueInArray(-1, idsToSet)) {
		    		UserFilterMapper ufm = new UserFilterMapper();
		    		ufm.setUserId(userSelectId);
		    		ufm.setType(UserFilters.TYPE_RESTRICTION_ABSTRACTOR);
		    		for (int i = 0; i < idsToSet.length; i++) {
		    			ufm.setId(null);	//clear id so a new object will be inserted and not just updated
						ufm.setValueLong((long)idsToSet[i]);
						if(!ufm.save()) {
							logger.error("Could not save object: " + ufm);
						}
					}
	    		}
    		}
    		destination = URLMaping.USER_RESTRICTIONS_JSP;
    	} else if(opCodeString.equals(SET_AGENTS) ){
    		int[] idsToSet = parameterParser.getIntParameters(RequestParams.REPORTS_AGENT, new int[]{-1});
    		UserFilterMapper.clearAttributes(userSelectVector, UserFilters.TYPE_RESTRICTION_AGENT);
    		for (Long userSelectId : userSelectVector) {
    			if(!Util.isValueInArray(-1, idsToSet)) {
    	    		UserFilterMapper ufm = new UserFilterMapper();
    	    		ufm.setUserId(userSelectId);
    	    		ufm.setType(UserFilters.TYPE_RESTRICTION_AGENT);
    	    		for (int i = 0; i < idsToSet.length; i++) {
    	    			ufm.setId(null);	//clear id so a new object will be inserted and not just updated
    					ufm.setValueLong((long)idsToSet[i]);
    					if(!ufm.save()) {
    						logger.error("Could not save object: " + ufm);
    					}
    				}
        		}
			}
    		
    		destination = URLMaping.USER_RESTRICTIONS_JSP;
    	} else if(opCodeString.equals(SET_WARNINGS) ){
    		int[] warnings = parameterParser.getIntParameters(RequestParams.WARNING_SELECT, new int[]{-1});
    		UserFilterMapper.clearAttributes(userSelectVector, UserFilters.TYPE_ASSIGN_ORDERS_WARNING);
    		if(!Util.isValueInArray(-1, warnings)) {
	    		UserFilterMapper ufm = new UserFilterMapper();
	    		ufm.setType(UserFilters.TYPE_ASSIGN_ORDERS_WARNING);
	    		for (Long userSelect : userSelectVector) {
	    			ufm.setUserId(userSelect);
		    		for (int i = 0; i < warnings.length; i++) {
		    			ufm.setId(null);	//clear id so a new object will be inserted and not just updated
						ufm.setValueLong((long)warnings[i]);
						if(!ufm.save()) {
							logger.error("Could not save object: " + ufm);
						}
					}	
				}
	    		
    		}
    		destination = URLMaping.USER_ASSIGN_JSP;
    	} else if(opCodeString.equals(SET_CATEGORIES_AND_SUBCATEGORIES) ){
    		String[] warnings = parameterParser.getStringParameters(RequestParams.SUBCATEGORY_SELECT, new String[]{"-1"});
    		UserFilterMapper.clearAttributes(userSelectVector, UserFilters.TYPE_ASSIGN_ORDERS_CATEGANDSUBCATEG);
    		if(!Util.isValueInArray("-1", warnings)) {
	    		UserFilterMapper ufm = new UserFilterMapper();
	    		ufm.setType(UserFilters.TYPE_ASSIGN_ORDERS_CATEGANDSUBCATEG);
	    		for (Long userSelect : userSelectVector) {
	    			ufm.setUserId(userSelect);
		    		
		    		for (int i = 0; i < warnings.length; i++) {
		    			ufm.setId(null);	//clear id so a new object will be inserted and not just updated
						ufm.setValueString(warnings[i]);
						if(!ufm.save()) {
							logger.error("Could not save object: " + ufm);
						}
					}	
				}
	    		
    		}
    		destination = URLMaping.USER_ASSIGN_JSP;
    	} else if(opCodeString.equals(RESET_PRODUCTS) ){
    		UserFilterMapper.clearAttributes(userSelectVector, UserFilters.TYPE_ASSIGN_ORDERS_PRODUCT);
    		destination = URLMaping.USER_ASSIGN_JSP;
    	} else if(opCodeString.equals(RESET_WARNINGS) ){
    		UserFilterMapper.clearAttributes(userSelectVector, UserFilters.TYPE_ASSIGN_ORDERS_WARNING);
    		destination = URLMaping.USER_ASSIGN_JSP;
    	} else if(opCodeString.equals(RESET_CATEGORIES_AND_SUBCATEGORIES) ){
    		UserFilterMapper.clearAttributes(userSelectVector, UserFilters.TYPE_ASSIGN_ORDERS_CATEGANDSUBCATEG);
    		destination = URLMaping.USER_ASSIGN_JSP;
    	} else if(opCodeString.equals(EXPORT_ALL_ASSIGNS) ){
    		String[] selectedUserID = request.getParameterValues(RequestParams.USER_COUNTY_EXPORT_USER_TO);
    		userId = request.getParameter(RequestParams.USER_COUNTY_EXPORT_USER_FROM);
    		userIdLong = Long.parseLong(userId);
    		if(selectedUserID != null) {
    			for (int i = 0; i < selectedUserID.length; i++) {
					if(selectedUserID[i] != null) {
						try {
							long selectedUserIdLong = Long.parseLong(selectedUserID[i]);
							if(selectedUserIdLong != -1) {
								
								UserFilterMapper.clearAttributes(selectedUserIdLong, UserFilters.TYPE_ASSIGN_ORDERS_AGENT);
								UserFilterMapper.copyAllowedFilter(userIdLong,selectedUserIdLong, UserFilters.TYPE_ASSIGN_ORDERS_AGENT);
								
								UserFilterMapper.clearAttributes(selectedUserIdLong, UserFilters.TYPE_ASSIGN_ORDERS_COUNTY);
								UserFilterMapper.copyAllowedFilter(userIdLong,selectedUserIdLong, UserFilters.TYPE_ASSIGN_ORDERS_COUNTY);
								
					    		UserFilterMapper.clearAttributes(selectedUserIdLong, UserFilters.TYPE_ASSIGN_ORDERS_PRODUCT);
					    		UserFilterMapper.copyAllowedFilter(userIdLong,selectedUserIdLong, UserFilters.TYPE_ASSIGN_ORDERS_PRODUCT);
					    		
					    		UserFilterMapper.clearAttributes(selectedUserIdLong, UserFilters.TYPE_ASSIGN_ORDERS_WARNING);
					    		UserFilterMapper.copyAllowedFilter(userIdLong,selectedUserIdLong, UserFilters.TYPE_ASSIGN_ORDERS_WARNING);
					    		
					    		UserFilterMapper.clearAttributes(selectedUserIdLong, UserFilters.TYPE_ASSIGN_ORDERS_CATEGANDSUBCATEG);
					    		UserFilterMapper.copyAllowedFilter(userIdLong,selectedUserIdLong, UserFilters.TYPE_ASSIGN_ORDERS_CATEGANDSUBCATEG);		
					    		
					    		toRefresh.add(selectedUserIdLong);
							}
						} catch (Exception e) {
							e.printStackTrace();
						}
					}
				}
	    		
    		}
    	} else if(opCodeString.equals(SET_ORDERS_AGENTS) ){
    		int[] idsToSet = parameterParser.getIntParameters(RequestParams.REPORTS_AGENT, new int[]{-1});
    		UserFilterMapper.clearAttributes(userSelectVector, UserFilters.TYPE_ASSIGN_ORDERS_AGENT);
    		if(!Util.isValueInArray(-1, idsToSet)) {
	    		UserFilterMapper ufm = new UserFilterMapper();
	    		ufm.setType(UserFilters.TYPE_ASSIGN_ORDERS_AGENT);
	    		
	    		for (Long userSelect : userSelectVector) {
	    			ufm.setUserId(userSelect);
		    		
		    		for (int i = 0; i < idsToSet.length; i++) {
		    			ufm.setId(null);	//clear id so a new object will be inserted and not just updated
						ufm.setValueLong((long)idsToSet[i]);
						if(!ufm.save()) {
							logger.error("Could not save object: " + ufm);
						}
					}	
				}
	    		
    		}
    		destination = URLMaping.USER_ASSIGN_JSP;
    	} else if(opCodeString.equals(RESET_ORDERS_AGENTS) ){
    		UserFilterMapper.clearAttributes(userSelectVector, UserFilters.TYPE_ASSIGN_ORDERS_AGENT);
    		destination = URLMaping.USER_ASSIGN_JSP;
    	} else if(opCodeString.equals(SET_ORDERS_COUNTIES) ){
    		
    		if( countyIdsList != null ) {
				
    			UserFilterMapper.clearAttributes(userSelectVector, UserFilters.TYPE_ASSIGN_ORDERS_COUNTY);
    			
				for( int j = 0 ; j < countyIdsList.length ; j ++ )
				{
					long tmpCountyId = -1;
					long tmpUserId = -1;
					try
					{
						String[] parts = countyIdsList[j].split("_");
						tmpCountyId = Long.parseLong( parts[0] );
						tmpUserId = Long.parseLong( parts[1] );
						
						UserFilterMapper ufm = new UserFilterMapper();
			    		ufm.setUserId(tmpUserId);
			    		ufm.setType(UserFilters.TYPE_ASSIGN_ORDERS_COUNTY);
			    		
						//boolean checked = request.getParameter( "countyIdCheck_" + countyIdsList[j] ) != null;
						//if( checked ) {
							ufm.setId(null);	//clear id so a new object will be inserted and not just updated
							ufm.setValueLong(tmpCountyId);
							if(!ufm.save()) {
								logger.error("Could not save object: " + ufm);
							}
						//}
					}catch( Exception e ) {
						continue;
					}
				}
			}
    		destination = URLMaping.USER_ASSIGN_JSP;
    		
    	} else if(opCodeString.equals(RESET_ORDERS_COUNTIES) ){
    		UserFilterMapper.clearAttributes(userIdLong, UserFilters.TYPE_ASSIGN_ORDERS_COUNTY);
    		destination = URLMaping.USER_ASSIGN_JSP;	
    		
//--- end orders assign page    		
    	} else if(opCodeString.equals(RESET_ALL_FILTERS) ){
    		UserFilterMapper.clearAttributes(userIdLong, UserManager.dashboardFilterTypes.get("ProductType"));
    		UserFilterMapper.clearAttributes(userIdLong, UserManager.dashboardFilterTypes.get("Warning"));
    		UserFilterMapper.clearAttributes(userIdLong, UserManager.dashboardFilterTypes.get("CategAndSubcateg"));
    		DBUser.resetAllowedCountiesForUser(userId);
			configuredUser.loadAllowedCounties();
			destination = URLMaping.USER_ASSIGN_JSP;
    	} else if(opCodeString.equals(EXPORT_ALLOWED_PRODUCTS) ){
    		String selectedUserID = request.getParameter(RequestParams.USER_COUNTY_EXPORT_USER_TO);
    		userId = request.getParameter(RequestParams.USER_COUNTY_EXPORT_USER_FROM);
    		userIdLong = Long.parseLong(userId);
    		if(selectedUserID!=null && !selectedUserID.equals("-1")) {
				try {
					UserFilterMapper.copyAllowedFilter(userIdLong,Long.parseLong(selectedUserID), UserManager.dashboardFilterTypes.get("ProductType"));		
				} catch (Exception e) {
					e.printStackTrace();
				}
    		}
    		destination = URLMaping.USER_ASSIGN_JSP;
    	} else if(opCodeString.equals(EXPORT_ALLOWED_WARNINGS) ){
    		String selectedUserID = request.getParameter(RequestParams.USER_COUNTY_EXPORT_USER_TO);
    		userId = request.getParameter(RequestParams.USER_COUNTY_EXPORT_USER_FROM);
    		userIdLong = Long.parseLong(userId);
    		if(selectedUserID!=null && !selectedUserID.equals("-1")) {
				try {
					UserFilterMapper.copyAllowedFilter(userIdLong,Long.parseLong(selectedUserID), UserManager.dashboardFilterTypes.get("Warning"));		
				} catch (Exception e) {
					e.printStackTrace();
				}
    		}
    		destination = URLMaping.USER_ASSIGN_JSP;
    	} else if(opCodeString.equals(EXPORT_ALLOWED_CATEG_AND_SUBCATEG) ){
    		String selectedUserID = request.getParameter(RequestParams.USER_COUNTY_EXPORT_USER_TO);
    		userId = request.getParameter(RequestParams.USER_COUNTY_EXPORT_USER_FROM);
    		userIdLong = Long.parseLong(userId);
    		if(selectedUserID!=null && !selectedUserID.equals("-1")) {
				try {
					UserFilterMapper.copyAllowedFilter(userIdLong,Long.parseLong(selectedUserID), UserManager.dashboardFilterTypes.get("CategAndSubcateg"));		
				} catch (Exception e) {
					e.printStackTrace();
				}
    		}
    		destination = URLMaping.USER_ASSIGN_JSP;
    	} else if(opCodeString.equals(EXPORT_ALL) ){
    		String selectedUserID = request.getParameter(RequestParams.USER_COUNTY_EXPORT_USER_TO);
    		userId = request.getParameter(RequestParams.USER_COUNTY_EXPORT_USER_FROM);
    		userIdLong = Long.parseLong(userId);
    		UserAttributes selectedUser = null;
    		if(selectedUserID!=null && !selectedUserID.equals("-1")) {
				try {
					selectedUser = UserUtils.getUserFromId( new BigDecimal( selectedUserID ) );
					DBUser.copyAllowedCounties(userId,selectedUserID);
					selectedUser.loadAllowedCounties();
					UserFilterMapper.clearAttributes(Long.parseLong(selectedUserID), UserManager.dashboardFilterTypes.get("ProductType"));
		    		UserFilterMapper.clearAttributes(Long.parseLong(selectedUserID), UserManager.dashboardFilterTypes.get("Warning"));
		    		UserFilterMapper.clearAttributes(Long.parseLong(selectedUserID), UserManager.dashboardFilterTypes.get("CategAndSubcateg"));
					UserFilterMapper.copyAllowedFilter(userIdLong,Long.parseLong(selectedUserID), UserManager.dashboardFilterTypes.get("ProductType"));
					UserFilterMapper.copyAllowedFilter(userIdLong,Long.parseLong(selectedUserID), UserManager.dashboardFilterTypes.get("Warning"));
					UserFilterMapper.copyAllowedFilter(userIdLong,Long.parseLong(selectedUserID), UserManager.dashboardFilterTypes.get("CategAndSubcateg"));
				} catch (Exception e) {
					e.printStackTrace();
				}
    		}
    	} 
		
		if(StringUtils.isNotEmpty(opCodeString)) {
			UserManagerI userManager = com.stewart.ats.user.UserManager.getInstance();
			try {
				userManager.getAccess();
				for (Long userToRefresh : toRefresh) {
					userManager.refreshInfo(userToRefresh);	
				}
				
			} catch (Throwable t) {
				logger.error("Error updating UserManagerI with operation " + opCodeString + " for userId " + userId);
			} finally {
				userManager.releaseAccess();
			} 
		}
		if(destination.contains("?")) {
			destination += "&searchId=" + searchId;
		} else {
			destination += "?searchId=" + searchId;
		}
		forward(request, response, destination );
	}

}
