package ro.cst.tsearch.connection.http2;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.io.IOUtils;

import ro.cst.tsearch.connection.LoginResponse;
import ro.cst.tsearch.connection.LoginResponse.LoginStatus;
import ro.cst.tsearch.connection.http.HTTPRequest;
import ro.cst.tsearch.connection.http.HTTPResponse;
import ro.cst.tsearch.data.SitesPasswords;
import ro.cst.tsearch.utils.RegExUtils;
import ro.cst.tsearch.utils.StringUtils;
import ro.cst.tsearch.utils.TiffConcatenator;

public class ARWashingtonRO extends HttpSite {
	
	private String viewstate = "";
	
	private static final String URL_SRCH_QUICK_NAME = "http://www.co.washington.ar.us/eSearch/LandRecords/protected/SrchQuickName.aspx";
	private static final String URL_SRCH_INST_NUMBER = "http://www.co.washington.ar.us/eSearch/LandRecords/protected/SrchInstNumber.aspx";
	private static final String URL_SRCH_QUICK_PROPERTY = "http://www.co.washington.ar.us/eSearch/LandRecords/protected/SrchQuickProperty.aspx";
	private static final String URL_MESSAGE_CENTER = "http://www.co.washington.ar.us/eSearch/User/protected/MessageCenter.aspx";
	
	private static final String[] ALWAYS_PRESENT_PARAMETERS_NAMES = {"__LASTFOCUS", "__EVENTARGUMENT", "__SCROLLPOSITIONX", "__SCROLLPOSITIONY", 
								                        			 "__VIEWSTATEENCRYPTED", "ctl00$txtJobReference"};
	private static final String[] ALWAYS_PRESENT_PARAMETERS_VALUES = {"", "", "0", "0", "", ""};
	
	private static final String EMPTY_FILE_NUMBER_1 = "    -        ";
	private static final String EMPTY_FILE_NUMBER_2 = "  -    -      ";
	
	private static final String DATE_FROM_PARAMETER = "ctl00$cphMain$SrchDates1$txtFiledFrom";
	private static final String DATE_THRU_PARAMETER = "ctl00$cphMain$SrchDates1$txtFiledThru";
	private static final String CHECKBOX_NAME_PARAMETER = "ctl00$cphMain$SrchNames1$cbSameInstrument";
	
	private static final String INDEX_TYPES_PARAMETER = "ctl00$cphMain$SrchIndexInformation1$lbIndexTypes";
	private static final String KIND_GROUPS_PARAMETER = "ctl00$cphMain$SrchIndexInformation1$lbKindGroups";
	private static final String KINDS_PARAMETER = "ctl00$cphMain$SrchIndexInformation1$lbKinds";
	
	private static final String[] EMPTY_PARAMS_NAME_SEARCH = {"__VIEWSTATE",
			 								                  "ctl00$cphMain$SrchDates1$weFiledFrom_ClientState", 
						 								      "ctl00$cphMain$SrchDates1$weFiledThru_ClientState",
						 								      "ctl00$cphMain$SrchDates1$meeFiledFrom_ClientState", 
						 								      "ctl00$cphMain$SrchDates1$meeFiledThru_ClientState",
						 								      "ctl00$cphMain$SrchIndexInformation1$meeAmountMin_ClientState", 
						 								      "ctl00$cphMain$SrchIndexInformation1$meeAmountMax_ClientState",
						 								      "ctl00$cphMain$SrchIndexInformation1$weAmountsMin_ClientState", 
						 								      "ctl00$cphMain$SrchIndexInformation1$weAmountsMax_ClientState",
						 								      "ctl00$cphMain$SrchIndexInformation1$lseIndexTypes_LB_ClientState",
						 								      "ctl00$cphMain$SrchIndexInformation1$lseKinds_ClientState",
			            									  "ctl00$cphMain$SrchIndexInformation1$lseKindGroup_ClientState"};
	
	private static final String[] EMPTY_PARAMS_FILE_NUMBER_SEARCH = {"__VIEWSTATE",
			 														 "__EVENTTARGET",
														             "ctl00$cphMain$SrchNames1$txtFirmSurName",
														             "ctl00$cphMain$SrchNames1$txtMiddleName",
														             "ctl00$cphMain$SrchNames1$txtGivenName1",
														             "ctl00$cphMain$SrchNames1$txtNameTitle",
														             "ctl00$cphMain$SrchNames1$txtGivenName2",
														             "ctl00$cphMain$SrchDates1$weFiledFrom_ClientState",
														             "ctl00$cphMain$SrchDates1$weFiledThru_ClientState",
														             "ctl00$cphMain$SrchDates1$meeFiledFrom_ClientState",
														             "ctl00$cphMain$SrchDates1$meeFiledThru_ClientState",
														             "ctl00$cphMain$SrchDates1$txtFiledFrom",
														             "ctl00$cphMain$SrchDates1$txtFiledThru",
														             "ctl00$cphMain$SrchIndexInformation1$lseIndexTypes_LB_ClientState",
														             "ctl00$cphMain$SrchIndexInformation1$lseKinds_ClientState",
														             "ctl00$cphMain$SrchIndexInformation1$lseKindGroup_ClientState",
														             "ctl00$cphMain$SrchIndexInformation1$meeAmountMin_ClientState",
														             "ctl00$cphMain$SrchIndexInformation1$meeAmountMax_ClientState",
														             "ctl00$cphMain$SrchIndexInformation1$weAmountsMin_ClientState",
														             "ctl00$cphMain$SrchIndexInformation1$weAmountsMax_ClientState"};
	
	private static final String[] EMPTY_PARAMS_PROPERTY_SEARCH_PROPERTY_TYPE = {"__VIEWSTATE",
			                                                                   "ctl00$cphMain$SrchProperty1$lseSubdivisions_ClientState",
			                                                                   "ctl00$cphMain$SrchProperty1$lseSection_ClientState",
			                                                                   "ctl00$cphMain$SrchProperty1$lseTownship_ClientState",
			                                                                   "ctl00$cphMain$SrchProperty1$lseQtrSection_ClientState", 
			                                                                   "ctl00$cphMain$SrchProperty1$lseQtrQtrSection_ClientState",
			                                                                   "ctl00$cphMain$SrchProperty1$lseUnplattedLot_ClientState",
			                                                                   "ctl00$cphMain$SrchProperty1$lseRange_ClientState",
			                                                                   "ctl00$cphMain$SrchProperty1$lseUnit_ClientState",
			                                                                   "ctl00$cphMain$SrchProperty1$lseBlock_ClientState",
			                                                                   "ctl00$cphMain$SrchProperty1$lseLot_ClientState",	
			                                                                   "ctl00$cphMain$SrchProperty1$meeZip_ClientState",
			                                                                   "ctl00$cphMain$SrchProperty1$meeZipExt_ClientState",
			                                                                   "ctl00$cphMain$SrchProperty1$txtComments",
			                                                                   "ctl00$cphMain$SrchDates1$weFiledFrom_ClientState",
			                                                                   "ctl00$cphMain$SrchDates1$weFiledThru_ClientState",
			                                                                   "ctl00$cphMain$SrchDates1$meeFiledFrom_ClientState",
			                                                                   "ctl00$cphMain$SrchDates1$meeFiledThru_ClientState",
			                                                                   "ctl00$cphMain$SrchDates1$txtFiledFrom",
			                                                                   "ctl00$cphMain$SrchDates1$txtFiledThru",
			                                                                   "ctl00$cphMain$SrchIndexInformation1$lseIndexTypes_LB_ClientState",
			                                                                   "ctl00$cphMain$SrchIndexInformation1$lseKinds_ClientState",
			                                                                   "ctl00$cphMain$SrchIndexInformation1$lseKindGroup_ClientState",
			                                                                   "ctl00$cphMain$SrchIndexInformation1$meeAmountMin_ClientState",
			                                                                   "ctl00$cphMain$SrchIndexInformation1$meeAmountMax_ClientState",
			                                                                   "ctl00$cphMain$SrchIndexInformation1$weAmountsMin_ClientState",
			                                                                   "ctl00$cphMain$SrchIndexInformation1$weAmountsMax_ClientState",
			                                                                   "ctl00$cphMain$SrchIndexInformation1$txtAmountMin",
			                                                                   "ctl00$cphMain$SrchIndexInformation1$txtAmountMax",
			   															       "ctl00$cphMain$SrchIndexInformation1$txtDescription"};
	
	private static final String[] EMPTY_PARAMS_PROPERTY_SEARCH_MAIN_REQUEST = {"__VIEWSTATE",
			   																   "__EVENTTARGET",
			   																   "ctl00$cphMain$SrchProperty1$lseSubdivisions_ClientState",
			   																   "ctl00$cphMain$SrchProperty1$lseSection_ClientState",
			   																   "ctl00$cphMain$SrchProperty1$lseTownship_ClientState",
			   																   "ctl00$cphMain$SrchProperty1$lseQtrSection_ClientState",
			   																   "ctl00$cphMain$SrchProperty1$lseQtrQtrSection_ClientState",
			   																   "ctl00$cphMain$SrchProperty1$lseUnplattedLot_ClientState",
			   																   "ctl00$cphMain$SrchProperty1$lseRange_ClientState",
			   																   "ctl00$cphMain$SrchProperty1$lseUnit_ClientState",
			   																   "ctl00$cphMain$SrchProperty1$lseBlock_ClientState",
			   																   "ctl00$cphMain$SrchProperty1$lseLot_ClientState",
			   																   "ctl00$cphMain$SrchProperty1$meeZip_ClientState",
			   																   "ctl00$cphMain$SrchProperty1$meeZipExt_ClientState",
			   																   "ctl00$cphMain$SrchDates1$weFiledFrom_ClientState",
			   																   "ctl00$cphMain$SrchDates1$weFiledThru_ClientState",
			   																   "ctl00$cphMain$SrchDates1$meeFiledFrom_ClientState",
			   																   "ctl00$cphMain$SrchDates1$meeFiledThru_ClientState",
			   																   "ctl00$cphMain$SrchIndexInformation1$lseIndexTypes_LB_ClientState",
			   																   "ctl00$cphMain$SrchIndexInformation1$lseKinds_ClientState",
			   																   "ctl00$cphMain$SrchIndexInformation1$lseKindGroup_ClientState",
			   																   "ctl00$cphMain$SrchIndexInformation1$meeAmountMin_ClientState",
			   																   "ctl00$cphMain$SrchIndexInformation1$meeAmountMax_ClientState",
			   																   "ctl00$cphMain$SrchIndexInformation1$weAmountsMin_ClientState",
          																	   "ctl00$cphMain$SrchIndexInformation1$weAmountsMax_ClientState"};
	
	private static final String[] EMPTY_PARAMS_CERTIFICATION_DATE = {"__VIEWSTATE",
            														 "__EVENTTARGET",
            														 "ctl00$cphMain$SrchNames1$txtFirmSurName",
            														 "ctl00$cphMain$SrchNames1$txtMiddleName",
            														 "ctl00$cphMain$SrchNames1$txtGivenName1",
            														 "ctl00$cphMain$SrchNames1$txtNameTitle",
            														 "ctl00$cphMain$SrchNames1$txtGivenName2",
            														 "ctl00$cphMain$SrchDates1$weFiledFrom_ClientState",
            														 "ctl00$cphMain$SrchDates1$weFiledThru_ClientState",
            														 "ctl00$cphMain$SrchDates1$meeFiledFrom_ClientState",
            														 "ctl00$cphMain$SrchDates1$meeFiledThru_ClientState",
            														 "ctl00$cphMain$SrchDates1$txtFiledFrom",
            														 "ctl00$cphMain$SrchDates1$txtFiledThru",
            														 "ctl00$cphMain$SrchIndexInformation1$lseIndexTypes_LB_ClientState",
            														 "ctl00$cphMain$SrchIndexInformation1$lseKinds_ClientState",
            														 "ctl00$cphMain$SrchIndexInformation1$lseKindGroup_ClientState",
            														 "ctl00$cphMain$SrchIndexInformation1$meeAmountMin_ClientState",
            														 "ctl00$cphMain$SrchIndexInformation1$meeAmountMax_ClientState",
            														 "ctl00$cphMain$SrchIndexInformation1$weAmountsMin_ClientState",
            														 "ctl00$cphMain$SrchIndexInformation1$weAmountsMax_ClientState"};
	
	@Override
	public String getUserAgentValue() {
		return "Mozilla/5.0 (Windows NT 5.1; rv:2.0) Gecko/20100101 Firefox/4.0";
	}

	@Override
	public LoginResponse onLogin() {
		
		//get user name and password from database
		String user = SitesPasswords.getInstance().getPasswordValue(getCurrentCommunityId(), "ARWashingtonRO", "user");
		String password = SitesPasswords.getInstance().getPasswordValue(getCurrentCommunityId(), "ARWashingtonRO", "password");
		
		if(StringUtils.isEmpty(user) || StringUtils.isEmpty(password)) {
			return new LoginResponse(LoginStatus.STATUS_INVALID_CREDENTIALS, "Couldn't find user / password in database");
		}
		
		setAttribute(HttpSite.SINGLE_LINE_COOKIES_IN_REDIRECT, Boolean.TRUE);
		
		String serverAddress = getSiteLink();
				
		String resp1 = "";
		HTTPRequest req1 = new HTTPRequest(serverAddress, HTTPRequest.GET);		//go to server address
		req1.setHeader("User-Agent", getUserAgentValue());
		try {
			resp1 = exec(req1).getResponseAsString();
		} catch(IOException e){
			logger.error(e);
			throw new RuntimeException(e);
		}
		String viewstate_resp1 = getParameterValue("__VIEWSTATE", resp1);
		String scriptHiddenField = RegExUtils.getFirstMatch("_TSM_CombinedScripts_=([^\"]+)\"", resp1, 1);
						
		HTTPRequest req2 = new HTTPRequest(serverAddress, HTTPRequest.POST);	//send user and password
		for (int i=0;i<ALWAYS_PRESENT_PARAMETERS_NAMES.length;i++) {
			req2.setPostParameter(ALWAYS_PRESENT_PARAMETERS_NAMES[i], ALWAYS_PRESENT_PARAMETERS_VALUES[i]);
		}
		req2.setPostParameter("__VIEWSTATE", viewstate_resp1);
		req2.setPostParameter("__EVENTTARGET", "");
		req2.setPostParameter("ctl00$cphMain$blkLogin$txtUsername", user);
		req2.setPostParameter("ctl00$cphMain$blkLogin$txtPassword", password);
		req2.setPostParameter("ctl00$cphMain$blkLogin$btnLogin", "Sign In");
		req2.setPostParameter("ctl00_smScriptMan_HiddenField", scriptHiddenField);
		String resp2 = "";
		try {
			resp2 = exec(req2).getResponseAsString();
		} catch(IOException e){
			logger.error(e);
			throw new RuntimeException(e);
		}
		
		if(resp2.contains("The username and password combination supplied is invalid")) {
			logger.error("Invalid username/password");
			return LoginResponse.getDefaultInvalidCredentialsResponse();
		}
		
		viewstate = getParameterValue("__VIEWSTATE", resp2);
		goToSpecificPage();
		
		return LoginResponse.getDefaultSuccessResponse();
	}
	
	protected void goToSpecificPage() {
		HTTPRequest req3 = new HTTPRequest(getSiteLink(), HTTPRequest.POST);	//go to Circuit Clerk Records and Index
		for (int i=0;i<ALWAYS_PRESENT_PARAMETERS_NAMES.length;i++) {
			req3.setPostParameter(ALWAYS_PRESENT_PARAMETERS_NAMES[i], ALWAYS_PRESENT_PARAMETERS_VALUES[i]);
		}
		req3.setPostParameter("__VIEWSTATE", viewstate);
		req3.setPostParameter("__EVENTTARGET", "ctl00$cphMain$repCounties$ctl00$lbCounty");
		req3.setURL(URL_MESSAGE_CENTER);
		try {
			exec(req3);
		} catch(IOException e){
			logger.error(e);
			throw new RuntimeException(e);
		}
	}

	@Override
	public void onBeforeRequest(HTTPRequest req) {
		
		req.setHeader("User-Agent", getUserAgentValue());
		
		if (req.getMethod()==HTTPRequest.POST) {
			
			String url = req.getURL();
			
			String param = req.getPostFirstParameter("param");
			if ("prevnext".equals(param)) {						//navigation page
				String eventTarget = req.getPostFirstParameter("__EVENTTARGET");
				req.removePostParameters("param");
				req.setPostParameter("ctl00$smScriptMan", "ctl00$cphMain$lrrgResults$upMainResults|" + eventTarget);
				for (int i=0;i<ALWAYS_PRESENT_PARAMETERS_NAMES.length;i++) {
					if (!"__EVENTARGUMENT".equals(ALWAYS_PRESENT_PARAMETERS_NAMES[i])) {
						req.setPostParameter(ALWAYS_PRESENT_PARAMETERS_NAMES[i], ALWAYS_PRESENT_PARAMETERS_VALUES[i]);
					}
				}
				req.setPostParameter("__VIEWSTATE", "");
				req.setPostParameter("ctl00$ddlCountySelect", "-1");
			} else if ("image".equals(param)) {
				req.removePostParameters("param");
			}else if (url.contains("SrchQuickName.aspx")) {	//Name Search
				
				//click on Name
				HTTPRequest req1 = new HTTPRequest(URL_SRCH_QUICK_NAME, HTTPRequest.POST);
				for (int i=0;i<ALWAYS_PRESENT_PARAMETERS_NAMES.length;i++) {
					req1.setPostParameter(ALWAYS_PRESENT_PARAMETERS_NAMES[i], ALWAYS_PRESENT_PARAMETERS_VALUES[i]);
				}
				
				req1.setPostParameter("__VIEWSTATE", "");
				req1.setPostParameter("ctl00$NavMenuIdxRec$btnNav_IdxRec_Name", "Name");
				req1.setPostParameter("ctl00$ddlCountySelect", "-1");
				
				try {
					exec(req1);
				} catch(IOException e){
					logger.error(e);
					throw new RuntimeException(e);
				}
				
				for (int i=0;i<ALWAYS_PRESENT_PARAMETERS_NAMES.length;i++) {
					req.setPostParameter(ALWAYS_PRESENT_PARAMETERS_NAMES[i], ALWAYS_PRESENT_PARAMETERS_VALUES[i]);
				}
				
				for (int i=0;i<EMPTY_PARAMS_NAME_SEARCH.length;i++) {
					req.setPostParameter(EMPTY_PARAMS_NAME_SEARCH[i], "");
				}
				
				String empty_params_to_remove[] = {INDEX_TYPES_PARAMETER,
		                                           KIND_GROUPS_PARAMETER, 
						                           KINDS_PARAMETER};
				for (int i=0;i<empty_params_to_remove.length;i++) {
					String value = req.getPostFirstParameter(empty_params_to_remove[i]);
					if (StringUtils.isEmpty(value)) {
						req.removePostParameters(empty_params_to_remove[i]);
					}
				}
				
				String checkBoxValue = req.getPostFirstParameter(CHECKBOX_NAME_PARAMETER);
				if (!"on".equals(checkBoxValue)) {
					req.removePostParameters(CHECKBOX_NAME_PARAMETER);
				}
				
				String dateFrom = req.getPostFirstParameter(DATE_FROM_PARAMETER);
				req.removePostParameters(DATE_FROM_PARAMETER);
				dateFrom = verifyAndCorrectDate(dateFrom);
				req.setPostParameter(DATE_FROM_PARAMETER, dateFrom);
				String dateThru = req.getPostFirstParameter(DATE_THRU_PARAMETER);
				req.removePostParameters(DATE_THRU_PARAMETER);
				dateThru = verifyAndCorrectDate(dateThru);
				req.setPostParameter(DATE_THRU_PARAMETER, dateThru);
				
				//if Index Type is selected
				String indexType = req.getPostFirstParameter(INDEX_TYPES_PARAMETER);
				if (!StringUtils.isEmpty(indexType)) {
					req.removePostParameters("__EVENTTARGET");
					req.setPostParameter("__EVENTTARGET", INDEX_TYPES_PARAMETER);
					req.removePostParameters("ctl00$cphMain$btnSearchAll");
					req.setPostParameter("hiddenInputToUpdateATBuffer_CommonToolkitScripts", "0");
					try {
						exec(req);
					} catch(IOException e){
						logger.error(e);
						throw new RuntimeException(e);
					}
				}
				
				//if Groups is selected
				String groups = req.getPostFirstParameter(KIND_GROUPS_PARAMETER);
				if (!StringUtils.isEmpty(groups)) {
					req.removePostParameters("__EVENTTARGET");
					req.setPostParameter("__EVENTTARGET", KIND_GROUPS_PARAMETER);
					req.removePostParameters("ctl00$cphMain$btnSearchAll");
					req.setPostParameter("hiddenInputToUpdateATBuffer_CommonToolkitScripts", "0");
					try {
						exec(req);
					} catch(IOException e){
						logger.error(e);
						throw new RuntimeException(e);
					}
				}
				
				//if Kind is selected
				String kinds = req.getPostFirstParameter(KINDS_PARAMETER);
				if (!StringUtils.isEmpty(kinds)) {
					req.removePostParameters("__EVENTTARGET");
					req.setPostParameter("__EVENTTARGET", KINDS_PARAMETER);
					req.removePostParameters("ctl00$cphMain$btnSearchAll");
					req.setPostParameter("hiddenInputToUpdateATBuffer_CommonToolkitScripts", "0");
					try {
						exec(req);
					} catch(IOException e){
						logger.error(e);
						throw new RuntimeException(e);
					}
				}
				
				req.setPostParameter("__EVENTTARGET", "");
				req.setPostParameter("ctl00$cphMain$btnSearchAll", "SEARCH - Show Final Results");
				req.removePostParameters("hiddenInputToUpdateATBuffer_CommonToolkitScripts");
				req.setPostParameter("hiddenInputToUpdateATBuffer_CommonToolkitScripts", "1");
				
			} else if (url.contains("SrchInstNumber.aspx")) {	//File Number Search
				
				//click on File Number
				HTTPRequest req1 = new HTTPRequest(URL_SRCH_QUICK_NAME, HTTPRequest.POST);
				for (int i=0;i<ALWAYS_PRESENT_PARAMETERS_NAMES.length;i++) {
					req1.setPostParameter(ALWAYS_PRESENT_PARAMETERS_NAMES[i], ALWAYS_PRESENT_PARAMETERS_VALUES[i]);
				}
				
				for (int i=0;i<EMPTY_PARAMS_FILE_NUMBER_SEARCH.length;i++) {
					req1.setPostParameter(EMPTY_PARAMS_FILE_NUMBER_SEARCH[i], "");
				}
				
				req1.setPostParameter("ctl00$NavMenuIdxRec$btnNav_IdxRec_File", "File Number");
				req1.setPostParameter("ctl00$ddlCountySelect", "-1");
				req1.setPostParameter("ctl00$cphMain$SrchNames1$ddlWildcardLast", "0");
				req1.setPostParameter("ctl00$cphMain$SrchNames1$ddlWildcardFirst1", "0");
				req1.setPostParameter("ctl00$cphMain$SrchNames1$ddlWildcardFirst2", "0");
				req1.setPostParameter("ctl00$cphMain$SrchNames1$ddlParty", "-1");
				req1.setPostParameter("hiddenInputToUpdateATBuffer_CommonToolkitScripts", "1");
				
				try {
					exec(req1);
				} catch(IOException e){
					logger.error(e);
					throw new RuntimeException(e);
				}
				
				String indexType = req.getPostFirstParameter("ctl00$cphMain$ddlIndexType");
				if (!indexType.equals("ALL")) {
					
					//select Index Type
					HTTPRequest req2 = new HTTPRequest(URL_SRCH_INST_NUMBER, HTTPRequest.POST);
					for (int i=0;i<ALWAYS_PRESENT_PARAMETERS_NAMES.length;i++) {
						req2.setPostParameter(ALWAYS_PRESENT_PARAMETERS_NAMES[i], ALWAYS_PRESENT_PARAMETERS_VALUES[i]);
					}
					
					String empty_params_2[] = {"__VIEWSTATE",
											   "ctl00$cphMain$meeFileNumber_ClientState",
											   "ctl00$cphMain$txtFileNumberSuffix"};
					for (int i=0;i<empty_params_2.length;i++) {
						req2.setPostParameter(empty_params_2[i], "");
					}
					
					req2.setPostParameter("ctl00$smScriptMan", "ctl00$cphMain$ctl00|ctl00$cphMain$ddlIndexType");
					req2.setPostParameter("__EVENTTARGET", "ctl00$cphMain$ddlIndexType");
					req2.setPostParameter("ctl00$ddlCountySelect", "-1");
					req2.setPostParameter("ctl00$cphMain$ddlIndexType", indexType);
					req2.setPostParameter("ctl00$cphMain$txtFileNumber", "    -        ");
					
					try {
						exec(req2);
					} catch(IOException e){
						logger.error(e);
						throw new RuntimeException(e);
					}
					
				}
				
				for (int i=0;i<ALWAYS_PRESENT_PARAMETERS_NAMES.length;i++) {
					req.setPostParameter(ALWAYS_PRESENT_PARAMETERS_NAMES[i], ALWAYS_PRESENT_PARAMETERS_VALUES[i]);
				}
				
				String empty_params_2[] = {"__VIEWSTATE",
										   "__EVENTTARGET",
						                   "ctl00$cphMain$meeFileNumber_ClientState"};
				
				for (int i=0;i<empty_params_2.length;i++) {
					req.setPostParameter(empty_params_2[i], "");
				}
				
				String fileNumberParameter = "ctl00$cphMain$txtFileNumber";
				String fileNumber = req.getPostFirstParameter(fileNumberParameter);
				req.removePostParameters(fileNumberParameter);
				fileNumber = verifyAndCorrectFileNumber(fileNumber, indexType);
				req.setPostParameter(fileNumberParameter, fileNumber);
				if (!indexType.equals("ALL") && !indexType.equals("REL")) {
					req.removePostParameters("ctl00$cphMain$txtFileNumberSuffix");
				}
				
			} else if (url.contains("SrchQuickProperty.aspx")) {	//Property Search
				
				//click on Property
				HTTPRequest req1 = new HTTPRequest(URL_SRCH_QUICK_PROPERTY, HTTPRequest.POST);
				for (int i=0;i<ALWAYS_PRESENT_PARAMETERS_NAMES.length;i++) {
					req1.setPostParameter(ALWAYS_PRESENT_PARAMETERS_NAMES[i], ALWAYS_PRESENT_PARAMETERS_VALUES[i]);
				}
				
				String empty_params[] = {"__VIEWSTATE",
										 "__EVENTTARGET"};
				for (int i=0;i<empty_params.length;i++) {
					req1.setPostParameter(empty_params[i], "");
				}
				
				req1.setPostParameter("ctl00$NavMenuIdxRec$btnNav_IdxRec_Property", "Property");
				req1.setPostParameter("ctl00$ddlCountySelect", "-1");
				
				try {
					exec(req1);
				} catch(IOException e){
					logger.error(e);
					throw new RuntimeException(e);
				}
				
				//select Property Type
				HTTPRequest req2 = new HTTPRequest(URL_SRCH_QUICK_PROPERTY, HTTPRequest.POST);
				for (int i=0;i<ALWAYS_PRESENT_PARAMETERS_NAMES.length;i++) {
					req2.setPostParameter(ALWAYS_PRESENT_PARAMETERS_NAMES[i], ALWAYS_PRESENT_PARAMETERS_VALUES[i]);
				}
				
				for (int i=0;i<EMPTY_PARAMS_PROPERTY_SEARCH_PROPERTY_TYPE.length;i++) {
					req2.setPostParameter(EMPTY_PARAMS_PROPERTY_SEARCH_PROPERTY_TYPE[i], "");
				}
				
				req2.setPostParameter("__EVENTTARGET", "ctl00$cphMain$SrchProperty1$ddlPropertyType");
				req2.setPostParameter("ctl00$smScriptMan", "ctl00$cphMain$SrchProperty1$upProperty|ctl00$cphMain$SrchProperty1$ddlPropertyType");
				req2.setPostParameter("ctl00$ddlCountySelect", "-1");
				req2.setPostParameter("ctl00$cphMain$SrchProperty1$grpPropSearchType", "rbPlatted");
				String sourceModule = req.getPostFirstParameter("sourceModule");
				if (sourceModule==null) {
					sourceModule = "";
				}
				req2.setPostParameter("ctl00$cphMain$SrchProperty1$ddlPropertyType", sourceModule);
				req.removePostParameters("sourceModule");
				
				try {
					exec(req2);
				} catch(IOException e){
					logger.error(e);
					throw new RuntimeException(e);
				}
				
				String suffixes[] = {"_2", "_3", "_4"};
				for (int i=0;i<suffixes.length;i++) {
					req.removePostParameters(INDEX_TYPES_PARAMETER+suffixes[i]);
					req.removePostParameters(KIND_GROUPS_PARAMETER+suffixes[i]);
					req.removePostParameters(KINDS_PARAMETER+suffixes[i]);
				}
				
				for (int i=0;i<ALWAYS_PRESENT_PARAMETERS_NAMES.length;i++) {
					req.setPostParameter(ALWAYS_PRESENT_PARAMETERS_NAMES[i], ALWAYS_PRESENT_PARAMETERS_VALUES[i]);
				}
				
				for (int i=0;i<EMPTY_PARAMS_PROPERTY_SEARCH_MAIN_REQUEST.length;i++) {
					req.setPostParameter(EMPTY_PARAMS_PROPERTY_SEARCH_MAIN_REQUEST[i], "");
				}
				
				req.setPostParameter("ctl00$ddlCountySelect", "-1");
				req.setPostParameter("ctl00$cphMain$SrchProperty1$ddlPropertyType", sourceModule);
				
				String empty_params_to_remove[] = {INDEX_TYPES_PARAMETER,
							                       KIND_GROUPS_PARAMETER, 
							                       KINDS_PARAMETER,
							                       "ctl00_cphMain_SrchProperty1_lbSubdivisions"};
				for (int i=0;i<empty_params_to_remove.length;i++) {
					String value = req.getPostFirstParameter(empty_params_to_remove[i]);
					if (StringUtils.isEmpty(value)) {
						req.removePostParameters(empty_params_to_remove[i]);
					}
				}
				
				String dateFrom = req.getPostFirstParameter(DATE_FROM_PARAMETER);
				req.removePostParameters(DATE_FROM_PARAMETER);
				dateFrom = verifyAndCorrectDate(dateFrom);
				req.setPostParameter(DATE_FROM_PARAMETER, dateFrom);
				String dateThru = req.getPostFirstParameter(DATE_THRU_PARAMETER);
				req.removePostParameters(DATE_THRU_PARAMETER);
				dateThru = verifyAndCorrectDate(dateThru);
				req.setPostParameter(DATE_THRU_PARAMETER, dateThru);
				
				//if Index Type is selected
				String indexType = req.getPostFirstParameter(INDEX_TYPES_PARAMETER);
				if (!StringUtils.isEmpty(indexType)) {
					req.removePostParameters("__EVENTTARGET");
					req.setPostParameter("__EVENTTARGET", INDEX_TYPES_PARAMETER);
					req.removePostParameters("ctl00$cphMain$btnSearchAll");
					req.setPostParameter("hiddenInputToUpdateATBuffer_CommonToolkitScripts", "0");
					try {
						exec(req);
					} catch(IOException e){
						logger.error(e);
						throw new RuntimeException(e);
					}
				}
				
				//if Groups is selected
				String groups = req.getPostFirstParameter(KIND_GROUPS_PARAMETER);
				if (!StringUtils.isEmpty(groups)) {
					req.removePostParameters("__EVENTTARGET");
					req.setPostParameter("__EVENTTARGET", KIND_GROUPS_PARAMETER);
					req.removePostParameters("ctl00$cphMain$btnSearchAll");
					req.setPostParameter("hiddenInputToUpdateATBuffer_CommonToolkitScripts", "0");
					try {
						exec(req);
					} catch(IOException e){
						logger.error(e);
						throw new RuntimeException(e);
					}
				}
				
				//if Kind is selected
				String kinds = req.getPostFirstParameter(KINDS_PARAMETER);
				if (!StringUtils.isEmpty(kinds)) {
					req.removePostParameters("__EVENTTARGET");
					req.setPostParameter("__EVENTTARGET", KINDS_PARAMETER);
					req.removePostParameters("ctl00$cphMain$btnSearchAll");
					req.setPostParameter("hiddenInputToUpdateATBuffer_CommonToolkitScripts", "0");
					try {
						exec(req);
					} catch(IOException e){
						logger.error(e);
						throw new RuntimeException(e);
					}
				}
				
				req.setPostParameter("__EVENTTARGET", "");
				req.setPostParameter("ctl00$cphMain$btnSearchAll", "SEARCH - Show Final Results");
				req.removePostParameters("hiddenInputToUpdateATBuffer_CommonToolkitScripts");
				req.setPostParameter("hiddenInputToUpdateATBuffer_CommonToolkitScripts", "1");
				
			}
		}
	}
	
	@Override
	public void onAfterRequest(HTTPRequest req, HTTPResponse res) {
		
		if (req.getMethod()==HTTPRequest.POST) {
			
			if (req.getRedirectLocation().contains("V1Viewer.aspx")) {				//request for the image
				byte[] completeImage = getImageFromResponse(res.getResponseAsString());
				if (completeImage!=null && completeImage.length!=0) {
					res.is = new ByteArrayInputStream(completeImage);
					res.contentLenght = completeImage.length;
					res.contentType = "image/tiff";
					res.returnCode = 200;
					res.body = null;
				} else {
					res.is = IOUtils.toInputStream("<html>Error while downloading the image</html>");
					res.body = "<html>Error while downloading the image</html>";
					res.contentLenght = res.body.length();
					res.returnCode = 200;
					return;
				}
			} else {
				
				String stringResponse = res.getResponseAsString();
				if (stringResponse.matches("(?is).*?<a[^>]*>\\s*\\[\\+\\]\\s*</a>.*")) {	//some rows must be expanded
					String eventTarget = "";
					Matcher matcher = Pattern.compile("(?is)<a href=\"javascript:__doPostBack\\('([^']+)',''\\)\">Expand All Rows</a>")
						.matcher(res.getResponseAsString());
					if (matcher.find()) {
						eventTarget = matcher.group(1);
					}
						
					HTTPRequest req2 = new HTTPRequest(req.getURL(), HTTPRequest.POST);		//Expand All Rows
					req2.setURL(getLastRequest());
					for (int i=0;i<ALWAYS_PRESENT_PARAMETERS_NAMES.length;i++) {
						req2.setPostParameter(ALWAYS_PRESENT_PARAMETERS_NAMES[i], ALWAYS_PRESENT_PARAMETERS_VALUES[i]);
					}
					req2.setPostParameter("__VIEWSTATE", "");
					req2.setPostParameter("__EVENTTARGET", eventTarget);
					req2.setPostParameter("ctl00$smScriptMan", "ctl00$cphMain$lrrgResults$upMainResults|" + eventTarget);
					try {
						HTTPResponse resp = exec(req2);
						res.is = new ByteArrayInputStream(resp.getResponseAsByte());
						res.contentLenght= resp.contentLenght;
						res.contentType = resp.contentType;
						res.headers = resp.headers;
						res.returnCode = resp.returnCode;
						res.body = resp.body;
						res.setLastURI(resp.getLastURI());
					} catch(IOException e){
						logger.error(e);
						throw new RuntimeException(e);
					}
				} else {
					res.is = new ByteArrayInputStream(stringResponse.getBytes());
				}
			}
		}	
	}

	private String verifyAndCorrectFileNumber(String fileNumber, String indexType) {
		
		if (indexType.equals("ALL") || indexType.equals("LIE") || indexType.equals("MIS") 	//xxxx-dddddddd
				|| indexType.equals("REL") || indexType.equals("FIN")) {					//x=any character, d=digit
			if (fileNumber.length()>13) {
				return EMPTY_FILE_NUMBER_1;
			}
			String split[] = fileNumber.split("-", -1);
			if (split.length>2) {
				return EMPTY_FILE_NUMBER_1;
			} else if (split.length==2) {
				String p1 = split[0];
				String p2 = split[1];
				if (p1.length()>4) {
					return EMPTY_FILE_NUMBER_1;
				}
				if (p2.length()>8) {
					return EMPTY_FILE_NUMBER_1;
				}
				if (!p2.matches("\\d+")) {
					return EMPTY_FILE_NUMBER_1;
				}
				p1 = org.apache.commons.lang.StringUtils.rightPad(p1, 4, " ");
				p2 = org.apache.commons.lang.StringUtils.rightPad(p2, 8, " ");
				return p1 + "-" + p2; 
			} else {		
				if (fileNumber.length()>12) {
					return EMPTY_FILE_NUMBER_1;
				}
				if (fileNumber.trim().length()==0) {
					return EMPTY_FILE_NUMBER_1;
				}
				fileNumber = org.apache.commons.lang.StringUtils.rightPad(fileNumber, 4, " ");
				if (fileNumber.length()==4) {
					return fileNumber + "-        ";
				} else {
					String p1 = fileNumber.substring(0, 4);
					String p2 = fileNumber.substring(4);
					if (!p2.matches("\\d+")) {
						return EMPTY_FILE_NUMBER_1;
					}
					p2 = org.apache.commons.lang.StringUtils.rightPad(p2, 8, " ");
					return p1 + "-" + p2;
				}
			}
		} else if (indexType.equals("CHA") || indexType.equals("CIV") || indexType.equals("CRI")) {	//dd-xxxx-dddddd
			if (fileNumber.length()>14) {
				return EMPTY_FILE_NUMBER_2;
			}
			String split[] = fileNumber.split("-", -1);
			if (split.length>3) {
				return EMPTY_FILE_NUMBER_2;
			} else if (split.length==3) {
				String p1 = split[0];
				String p2 = split[1];
				String p3 = split[2];
				if (p1.length()>2) {
					return EMPTY_FILE_NUMBER_2;
				}
				if (p2.length()>4) {
					return EMPTY_FILE_NUMBER_2;
				}
				if (p3.length()>6) {
					return EMPTY_FILE_NUMBER_2;
				}
				if (!p1.matches("\\d+")) {
					return EMPTY_FILE_NUMBER_2;
				}
				if (!p3.matches("\\d+")) {
					return EMPTY_FILE_NUMBER_2;
				}
				p1 = org.apache.commons.lang.StringUtils.rightPad(p1, 2, " ");
				p2 = org.apache.commons.lang.StringUtils.rightPad(p2, 4, " ");
				p3 = org.apache.commons.lang.StringUtils.rightPad(p3, 6, " ");
				return p1 + "-" + p2 + "-" + p3; 
			} else if (split.length==2) {
				if (fileNumber.length()>13) {
					return EMPTY_FILE_NUMBER_2;
				}
				String p1 = split[0];
				String p2 = split[1];
				if (p1.length()>2) {
					return EMPTY_FILE_NUMBER_2;
				}
				if (p2.length()>10) {
					return EMPTY_FILE_NUMBER_2;
				}
				if (!p1.matches("\\d+")) {
					return EMPTY_FILE_NUMBER_2;
				}
				p1 = org.apache.commons.lang.StringUtils.rightPad(p1, 2, " ");
				p2 = org.apache.commons.lang.StringUtils.rightPad(p2, 4, " ");
				if (p2.length()==4) {
					return p1 + "-" + p2 + "-      ";
				} else {
					String p21 = p2.substring(0, 4);
					String p22 = p2.substring(4);
					if (!p22.matches("\\d+")) {
						return EMPTY_FILE_NUMBER_2;
					}
					p22 = org.apache.commons.lang.StringUtils.rightPad(p22, 6, " ");
					return p1 + "-" + p21 + "-" + p22;
				}
			} else {						
				if (fileNumber.length()>12) {
					return EMPTY_FILE_NUMBER_2;
				}
				if (fileNumber.trim().length()==0) {
					return EMPTY_FILE_NUMBER_2;
				}
				fileNumber = org.apache.commons.lang.StringUtils.rightPad(fileNumber, 2, " ");
				if (!fileNumber.matches("\\d+")) {
					return EMPTY_FILE_NUMBER_2;
				}
				if (fileNumber.length()==2) {
					return fileNumber + "-    -      ";
				} else {
					String p1 = fileNumber.substring(0, 2);
					String p2 = fileNumber.substring(2);
					if (!p1.matches("\\d+")) {
						return EMPTY_FILE_NUMBER_2;
					}
					p2 = org.apache.commons.lang.StringUtils.rightPad(p2, 4, " ");
					if (p2.length()==4) {
						return p1 + "-" + p2 + "-      ";
					} else {
						String p21 = p2.substring(0, 4);
						String p22 = p2.substring(4);
						if (!p22.matches("\\d+")) {
							return EMPTY_FILE_NUMBER_2;
						}
						p22 = org.apache.commons.lang.StringUtils.rightPad(p22, 6, " ");
						return p1 + "-" + p21 + "-" + p22;
					}
				}
			}
		} 
		
		return fileNumber;
	}
	
	private String verifyAndCorrectDate(String date) { 
		
		if (StringUtils.isEmpty(date)) {
			return "";
		}
		
		SimpleDateFormat df = new SimpleDateFormat("MM/dd/yyyy");
		try	{
			df.parse(date);
		}
		catch (ParseException e)
		{
			return "";
		}
		
		return date;
	}
	
	public String getCertificationDate() {
		if (this.status != STATUS_LOGGED_IN) {
			onLogin();
		}
		
		String resp = "";
		
		//go to Verified Dates
		HTTPRequest req = new HTTPRequest(URL_SRCH_QUICK_NAME, HTTPRequest.POST);
		for (int i=0;i<ALWAYS_PRESENT_PARAMETERS_NAMES.length;i++) {
			req.setPostParameter(ALWAYS_PRESENT_PARAMETERS_NAMES[i], ALWAYS_PRESENT_PARAMETERS_VALUES[i]);
		}
		
		for (int i=0;i<EMPTY_PARAMS_CERTIFICATION_DATE.length;i++) {
			req.setPostParameter(EMPTY_PARAMS_CERTIFICATION_DATE[i], "");
		}
		
		req.setPostParameter("ctl00$NavMenuIdxRec$btnNav_IdxRec_CertifiedDates", "Verified Dates");
		req.setPostParameter("ctl00$ddlCountySelect", "-1");
		req.setPostParameter("ctl00$cphMain$SrchNames1$ddlWildcardLast", "0");
		req.setPostParameter("ctl00$cphMain$SrchNames1$ddlWildcardFirst1", "0");
		req.setPostParameter("ctl00$cphMain$SrchNames1$ddlWildcardFirst2", "0");
		req.setPostParameter("ctl00$cphMain$SrchNames1$ddlParty", "-1");
		req.setPostParameter("hiddenInputToUpdateATBuffer_CommonToolkitScripts", "1");
		
		try {
			resp = exec(req).getResponseAsString();
		} catch(IOException e){
			logger.error(e);
			throw new RuntimeException(e);
		}
		
		return resp;
		
	}
	
	private String getParameterValue(String parameter, String text) {
		String result = "";
		
		Matcher matcher = Pattern.compile("<input.+?id=\"" + parameter + "\".+value=\"(.+?)\"").matcher(text);
		if (matcher.find()) {
			result = matcher.group(1);
		}
		
		return result;
	}
	
	public byte[] getImage(String link) {
		String response = getImageResponse(link);
		return getImageFromResponse(response);
	}
	
	private String getImageResponse(String link) {
		
		String response = "";
		
		link = link.replaceAll(".*?Link=", "");
		String[] split1 = link.split("\\?");
		if (split1.length==2) {
			link = split1[0];
			HTTPRequest req = new HTTPRequest(link, HTTPRequest.POST);
			String split2[] = split1[1].split("&");
			for (int i=0;i<split2.length;i++) {
				String par = split2[i];
				par = par.replace("\\$", "$");
				int index = par.indexOf("=");
				if (index>-1) {
					String name = par.substring(0, index);
					String value = par.substring(index+1);
					if (!name.equals("param")) {
						req.setPostParameter(name, value);
					}
				}
			}
			try {
				response = exec(req).getResponseAsString();
			} catch(IOException e){
				logger.error(e);
				throw new RuntimeException(e);
			}
		}
		
		return response;
	}
	
	private byte[] getImageFromResponse(String response) {
		
		try {
			
			String newURL = "";
			Matcher matcher = Pattern.compile("(?is)<param\\s+name=\"Page\\d+\"\\s+value=\"([^\"]+)\"")
				.matcher(response);
			List<byte[]> pages = new ArrayList<byte[]>();
			while (matcher.find()) {
				newURL = matcher.group(1);
				HTTPRequest req2 = new HTTPRequest(newURL, HTTPRequest.GET);		//get image
				try {
					HTTPResponse resp = exec(req2);
					pages.add(resp.getResponseAsByte());
				} catch(IOException e){
					logger.error(e);
					throw new RuntimeException(e);
				}
			}
			byte[] completeImage = TiffConcatenator.concateTiff(pages);
			
			return completeImage;
			
		} catch (Exception e) {
			e.printStackTrace();
		}
		
		return null;
	}
}
