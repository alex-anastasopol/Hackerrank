package ro.cst.tsearch.connection.http2;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import ro.cst.tsearch.connection.LoginResponse;
import ro.cst.tsearch.connection.LoginResponse.LoginStatus;
import ro.cst.tsearch.connection.http.HTTPRequest;
import ro.cst.tsearch.data.SitesPasswords;
import ro.cst.tsearch.utils.StringUtils;

public class KSGenericCO extends HttpSite {
	
	private String baseUrl = "";
	private String execution = "";
		
	private static final String TOKEN_PARAM = "org.codehaus.groovy.grails.SYNCHRONIZER_TOKEN";
	private static final String URI_PARAM = "org.codehaus.groovy.grails.SYNCHRONIZER_URI";
	
	private static final String EXECUTION_PARAM = "execution";
	private static final String COUNTY_PARAM = "county";
	private static final String CIVIL_COURT_PARAM = "civilCourt";
	private static final String CRIMINAL_COURT_PARAM = "criminalCourt";
	private static final String JUVENILE_COURT_PARAM = "juvenileCourt";
	private static final String CLIENT_PARAM = "client";
	private static final String LOGIN_PARAM = "login";
	
	private static Map<String, String> COUNTY_CODES = new HashMap<String, String>();
	static {
		COUNTY_CODES.put("ALLEN", "AL");
		COUNTY_CODES.put("ANDERSON", "AN");
		COUNTY_CODES.put("ATCHISON", "AT");
		COUNTY_CODES.put("BARBER", "BA");
		COUNTY_CODES.put("BARTON", "BT");
		COUNTY_CODES.put("BOURBON", "BB");
		COUNTY_CODES.put("BROWN", "BR");
		COUNTY_CODES.put("BUTLER", "BU");
		COUNTY_CODES.put("CHASE", "CS");
		COUNTY_CODES.put("CHAUTAUQUA", "CQ");
		COUNTY_CODES.put("CHEROKEE", "CK");
		COUNTY_CODES.put("CHEYENNE", "CN");
		COUNTY_CODES.put("CLARK", "CA");
		COUNTY_CODES.put("CLAY", "CY");
		COUNTY_CODES.put("CLOUD", "CD");
		COUNTY_CODES.put("COFFEY", "CF");
		COUNTY_CODES.put("COMANCHE", "CM");
		COUNTY_CODES.put("COWLEY", "CL");
		COUNTY_CODES.put("CRAWFORD", "CR");
		COUNTY_CODES.put("DECATUR", "DC");
		COUNTY_CODES.put("DICKINSON", "DK");
		COUNTY_CODES.put("DONIPHAN", "DP");
		COUNTY_CODES.put("DOUGLAS", "DG");
		COUNTY_CODES.put("EDWARDS", "ED");
		COUNTY_CODES.put("ELK", "EK");
		COUNTY_CODES.put("ELLIS", "EL");
		COUNTY_CODES.put("ELLSWORTH", "EW");
		COUNTY_CODES.put("FINNEY", "FI");
		COUNTY_CODES.put("FORD", "FO");
		COUNTY_CODES.put("FRANKLIN", "FR");
		COUNTY_CODES.put("GEARY", "GE");
		COUNTY_CODES.put("GOVE", "GO");
		COUNTY_CODES.put("GRAHAM", "GH");
		COUNTY_CODES.put("GRANT", "GT");
		COUNTY_CODES.put("GRAY", "GY");
		COUNTY_CODES.put("GREELEY", "GL");
		COUNTY_CODES.put("GREENWOOD", "GW");
		COUNTY_CODES.put("HAMILTON", "HM");
		COUNTY_CODES.put("HARPER", "HP");
		COUNTY_CODES.put("HARVEY", "HV");
		COUNTY_CODES.put("HASKELL", "HS");
		COUNTY_CODES.put("HODGEMAN", "HG");
		COUNTY_CODES.put("JACKSON", "JA");
		COUNTY_CODES.put("JEFFERSON", "JF");
		COUNTY_CODES.put("JEWELL", "JW");
		COUNTY_CODES.put("KEARNY", "KE");
		COUNTY_CODES.put("KINGMAN", "KM");
		COUNTY_CODES.put("KIOWA", "KW");
		COUNTY_CODES.put("LABETTE", "LB");
		COUNTY_CODES.put("LANE", "LE");
		COUNTY_CODES.put("LEAVENWORTH", "LV");
		COUNTY_CODES.put("LINCOLN", "LC");
		COUNTY_CODES.put("LINN", "LN");
		COUNTY_CODES.put("LOGAN", "LG");
		COUNTY_CODES.put("LYON", "LY");
		COUNTY_CODES.put("MARION", "MN");
		COUNTY_CODES.put("MARSHALL", "MS");
		COUNTY_CODES.put("MCPHERSON", "MP");
		COUNTY_CODES.put("MEADE", "ME");
		COUNTY_CODES.put("MIAMI", "MI");
		COUNTY_CODES.put("MITCHELL", "MC");
		COUNTY_CODES.put("MONTGOMERY", "MG");
		COUNTY_CODES.put("MORRIS", "MR");
		COUNTY_CODES.put("MORTON", "MT");
		COUNTY_CODES.put("NEMAHA", "NM");
		COUNTY_CODES.put("NEOSHO", "NO");
		COUNTY_CODES.put("NESS", "NS");
		COUNTY_CODES.put("NORTON", "NT");
		COUNTY_CODES.put("OSAGE", "OS");
		COUNTY_CODES.put("OSBORNE", "OB");
		COUNTY_CODES.put("OTTAWA", "OT");
		COUNTY_CODES.put("PAWNEE", "PN");
		COUNTY_CODES.put("PHILLIPS", "PL");
		COUNTY_CODES.put("POTTAWATOMIE", "PT");
		COUNTY_CODES.put("PRATT", "PR");
		COUNTY_CODES.put("RAWLINS", "RA");
		COUNTY_CODES.put("RENO", "RN");
		COUNTY_CODES.put("REPUBLIC", "RP");
		COUNTY_CODES.put("RICE", "RC");
		COUNTY_CODES.put("RILEY", "RL");
		COUNTY_CODES.put("ROOKS", "RO");
		COUNTY_CODES.put("RUSH", "RH");
		COUNTY_CODES.put("RUSSELL", "RS");
		COUNTY_CODES.put("SALINE", "SA");
		COUNTY_CODES.put("SCOTT", "SC");
		COUNTY_CODES.put("SEDGWICK", "SG");
		COUNTY_CODES.put("SEWARD", "SW");
		COUNTY_CODES.put("SHAWNEE", "SN");
		COUNTY_CODES.put("SHERIDAN", "SD");
		COUNTY_CODES.put("SHERMAN", "SH");
		COUNTY_CODES.put("SMITH", "SM");
		COUNTY_CODES.put("STAFFORD", "SF");
		COUNTY_CODES.put("STANTON", "ST");
		COUNTY_CODES.put("STEVENS", "SV");
		COUNTY_CODES.put("SUMNER", "SU");
		COUNTY_CODES.put("THOMAS", "TH");
		COUNTY_CODES.put("TREGO", "TR");
		COUNTY_CODES.put("WABAUNSEE", "WB");
		COUNTY_CODES.put("WALLACE", "WA");
		COUNTY_CODES.put("WASHINGTON", "WS");
		COUNTY_CODES.put("WICHITA", "WH");
		COUNTY_CODES.put("WILSON", "WL");
		COUNTY_CODES.put("WOODSON", "WO");
		COUNTY_CODES.put("WYANDOTTE", "WY");
	}
	
	private static String getCountyAbbreviation(String countyName) {
		String result = COUNTY_CODES.get(countyName);
		if (result==null) {
			result = countyName;
		}
		return result;
	}
	
	@Override
	public LoginResponse onLogin() {
		
		//get user name and password from database
		String user = SitesPasswords.getInstance().getPasswordValue(getCurrentCommunityId(), "KSGenericCO", "user");
		String password = SitesPasswords.getInstance().getPasswordValue(getCurrentCommunityId(), "KSGenericCO", "password");
				
		if(StringUtils.isEmpty(user) || StringUtils.isEmpty(password)) {
			return new LoginResponse(LoginStatus.STATUS_INVALID_CREDENTIALS, "Couldn't find user / password in database");
		}
		
		setAttribute(HttpSite.SINGLE_LINE_COOKIES_IN_REDIRECT, Boolean.TRUE);
		
		//first page
		String resp1 = "";
		HTTPRequest req1 = new HTTPRequest(getSiteLink(), HTTPRequest.GET); 
		try {
			resp1 = exec(req1).getResponseAsString();
		} catch(IOException e){
			logger.error(e);
			throw new RuntimeException(e);
		}
		String token = getParameterValueFromHtml(TOKEN_PARAM, resp1);
		String uri = getParameterValueFromHtml(URI_PARAM, resp1);
		
		//login page
		HTTPRequest req2 = new HTTPRequest(getSiteLink() + "login/index", HTTPRequest.POST);
		req2.setPostParameter(TOKEN_PARAM, token);
		req2.setPostParameter(URI_PARAM, uri);
		req2.setPostParameter(LOGIN_PARAM, "Subscriber Login");
		try {
			exec(req2);
		} catch(IOException e){
			logger.error(e);
			throw new RuntimeException(e);
		}
		
		//user and password page
		String resp3 = "";
		HTTPRequest req3 = new HTTPRequest(getLastRequest().replaceAll("(?is)/subscriber$", "/login"), HTTPRequest.POST);
		req3.setPostParameter("username", user);
		req3.setPostParameter("password", password);
		req3.setPostParameter(LOGIN_PARAM, "Login");
		try {
			resp3 = exec(req3).getResponseAsString();
		} catch(IOException e){
			logger.error(e);
			throw new RuntimeException(e);
		}
		
		baseUrl = getLastRequest().replaceFirst("\\?.*", "");
		execution = extractExecutionValue(resp3);
		
		if (resp3.contains("Search by Court Type")) {
			return LoginResponse.getDefaultSuccessResponse();
		} else {
			return LoginResponse.getDefaultFailureResponse();
		}
		
	}
	
	@Override
	public void onBeforeRequest(HTTPRequest req){

		String county = getDataSite().getCountyName().toUpperCase();
		county = getCountyAbbreviation(county);
		String url = req.getURL();
		
		req.removePostParameters("help");
		
		if (req.getMethod()==HTTPRequest.POST) {
			
			String resp0 = "";
			HTTPRequest req0 = new HTTPRequest(getLastRequest().replaceFirst("(?is)(/countyCourts/search).*", "$1"), HTTPRequest.GET); 
			try {
				resp0 = exec(req0).getResponseAsString();
			} catch(IOException e){
				logger.error(e);
				throw new RuntimeException(e);
			}
			execution = extractExecutionValue(resp0);
			
			String caseNumber = req.getPostFirstParameter("caseNumber");
			String ssn = req.getPostFirstParameter("ssn");
			String lastName = req.getPostFirstParameter("lastName");
			String companyName = req.getPostFirstParameter("companyName");
			String searchDate = req.getPostFirstParameter("searchDate");
			
			String pageUrl = baseUrl;
			boolean isCaseSearch = false;
			boolean isSSNorNameorCompanyorDateSearch = false;
			
			if (caseNumber!=null) {
				isCaseSearch = true;
				pageUrl += "?execution=" + execution + "&_eventId=searchCase";
			} else if (ssn!=null || lastName!=null || companyName!=null || searchDate!=null) {
				isSSNorNameorCompanyorDateSearch = true;
				pageUrl += "?execution=" + execution + "&_eventId=searchCourt";
			}
			
			String resp1 = "";
			HTTPRequest req1 = new HTTPRequest(pageUrl, HTTPRequest.GET); 
			try {
				resp1 = exec(req1).getResponseAsString();
			} catch(IOException e){
				logger.error(e);
				throw new RuntimeException(e);
			}
			execution = extractExecutionValue(resp1);
			url += "?execution=" + execution;
			
			if (isCaseSearch) {				//Search by Case Number
				
				req.setPostParameter(EXECUTION_PARAM, execution);
				req.setPostParameter(COUNTY_PARAM, county);
				
			} else if (isSSNorNameorCompanyorDateSearch) {		//Search by SSN, Name, Company Name or Date
				
				String resp2 = "";
				HTTPRequest req2 = new HTTPRequest(url, HTTPRequest.POST);	//select County, Court Type and Client 
				req2.setPostParameter(EXECUTION_PARAM, execution);
				req2.setPostParameter(COUNTY_PARAM, county);
				req2.setPostParameter("_civilCourt", "");
				req2.setPostParameter("_criminalCourt", "");
				req2.setPostParameter("_juvenileCourt", "");
				req2.setPostParameter("_eventId_courtSearch", "Search Courts");
				
				String civilCourt = req.getPostFirstParameter(CIVIL_COURT_PARAM);
				req.removePostParameters(CIVIL_COURT_PARAM);
				if ("on".equalsIgnoreCase(civilCourt)) {
					req2.setPostParameter(CIVIL_COURT_PARAM, civilCourt);
				}
				String crmiminalCourt = req.getPostFirstParameter(CRIMINAL_COURT_PARAM);
				req.removePostParameters(CRIMINAL_COURT_PARAM);
				if ("on".equalsIgnoreCase(crmiminalCourt)) {
					req2.setPostParameter(CRIMINAL_COURT_PARAM, crmiminalCourt);
				}
				String juvenileCourt = req.getPostFirstParameter(JUVENILE_COURT_PARAM);
				req.removePostParameters(JUVENILE_COURT_PARAM);
				if ("on".equalsIgnoreCase(juvenileCourt)) {
					req2.setPostParameter(JUVENILE_COURT_PARAM, juvenileCourt);
				}
				String client = req.getPostFirstParameter(CLIENT_PARAM);
				req.removePostParameters(CLIENT_PARAM);
				if (client!=null) {
					req2.setPostParameter(CLIENT_PARAM, client);
				}
				
				try {
					resp2 = exec(req2).getResponseAsString();
				} catch(IOException e){
					logger.error(e);
					throw new RuntimeException(e);
				}
				
				execution = extractExecutionValue(resp2);
				
				url = url.replaceAll("\\?.*", "");
				if (ssn!=null) {						//Search by SSN
					
					String resp3 = "";
					HTTPRequest req3 = new HTTPRequest(url + "?execution=" + execution + "&_eventId=searchSSN", HTTPRequest.GET);
					try {
						resp3 = exec(req3).getResponseAsString();
					} catch(IOException e){
						logger.error(e);
						throw new RuntimeException(e);
					}
					execution = extractExecutionValue(resp3);
					
				} else if (companyName!=null) {			//Search by Company Name
					
					String resp4 = "";
					HTTPRequest req4 = new HTTPRequest(url + "?execution=" + execution + "&_eventId=searchCompany", HTTPRequest.GET);
					try {
						resp4 = exec(req4).getResponseAsString();
					} catch(IOException e){
						logger.error(e);
						throw new RuntimeException(e);
					}
					execution = extractExecutionValue(resp4);
					
				} else if (searchDate!=null) {				//Search by Date
				
					String resp5 = "";
					HTTPRequest req5 = new HTTPRequest(url + "?execution=" + execution + "&_eventId=searchDate", HTTPRequest.GET);
					try {
						resp5 = exec(req5).getResponseAsString();
					} catch(IOException e){
						logger.error(e);
						throw new RuntimeException(e);
					}
					execution = extractExecutionValue(resp5);
				}
				
				String reqUrl = req.getURL() + "?execution=" + execution;
				req.modifyURL(reqUrl);
				req.setPostParameter(EXECUTION_PARAM, execution);
				
			} 
		} 
		
	}
	
	private String getParameterValueFromHtml(String parameter, String html) {
		String result = "";
		
		parameter = parameter.replace(".", "\\.");
		
		Matcher matcher = Pattern.compile("(?is)<input[^>]+value=\"([^\"]+)\"[^>]+id=\"" + parameter + "\"").matcher(html);
		if (matcher.find()) {
			result = matcher.group(1);
		}
		
		return result;
	}
	
	private String extractExecutionValue(String response) {
		String result = "";
		
		Matcher ma = Pattern.compile("(?is)\\bexecution=([^\"]+)\"").matcher(response);
		if (ma.find()) {
			result = ma.group(1);
		}
		
		return result;
	}
	
}
