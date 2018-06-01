package ro.cst.tsearch.connection.http3;

import java.util.HashMap;
import java.util.Map;

//import org.apache.http.HttpHost;
//import org.apache.http.conn.params.ConnRoutePNames;
//import org.apache.http.conn.scheme.PlainSocketFactory;
//import org.apache.http.conn.scheme.Scheme;
//import org.apache.http.conn.scheme.SchemeRegistry;
//
//import ro.cst.tsearch.ServerConfig;
import ro.cst.tsearch.connection.LoginResponse;
import ro.cst.tsearch.connection.LoginResponse.LoginStatus;
import ro.cst.tsearch.connection.http.HTTPRequest;
import ro.cst.tsearch.connection.http.HTTPResponse;
import ro.cst.tsearch.data.CountyConstants;
import ro.cst.tsearch.data.SitesPasswords;
//import ro.cst.tsearch.datatrace.Utils;
import ro.cst.tsearch.utils.StringUtils;

public class GenericOrbit extends HttpSite3 {

	public static final int DOCUMENT_NUMBER_SEARCH_CODE = 0;
	public static final int NAME_SEARCH_CODE = 1;
	public static final int LEGAL_SEARCH_CODE = 2;
	public static final int COURT_SEARCH_CODE = 3;
	
	public static final String SEARCH_TEXT = "Search";
	public static final String DOCUMENT_NUMBER_SEARCH_TEXT = "/DocumentNumberSearch/" + SEARCH_TEXT;
	public static final String NAME_SEARCH_TEXT = "/GrantorGranteeSearch/" + SEARCH_TEXT;
	public static final String LEGAL_SEARCH_TEXT = "/SubdivisionSearch/" + SEARCH_TEXT;
	public static final String COURT_SEARCH_TEXT = "/CourtNameSearch/" + SEARCH_TEXT;
	
	public static final String DOCUMENT_NUMBER_REGION_CODE_TEXT = "RegionID_DocNumber=";
	public static final String NAME_REGION_CODE_TEXT = "RegionID_Grant=";
	public static final String LEGAL_REGION_CODE_TEXT = "RegionID_Subdivision=";
	public static final String COURT_REGION_CODE_TEXT = "RegionID_CourName=";
	
	public static final String DOCUMENT_NUMBER_COUNTY_CODE_TEXT = "CountyID_DocNumber=";
	public static final String NAME_COUNTY_CODE_TEXT = "CountyID_Grant=";
	public static final String LEGAL_COUNTY_CODE_TEXT = "CountyID_Subdivision=";
	public static final String COURT_COUNTY_CODE_TEXT = "CountyID_CourtName=";
	
	boolean isCountySelected;
	
	private static Map<Integer, String> REGION_CODES = new HashMap<Integer, String>();
	static {
		REGION_CODES.put(CountyConstants.MO_Clay, "KS");
		REGION_CODES.put(CountyConstants.MO_Jackson, "KS");
		REGION_CODES.put(CountyConstants.MO_Platte, "KS");
		REGION_CODES.put(CountyConstants.KS_Johnson, "KS");
		REGION_CODES.put(CountyConstants.KS_Wyandotte, "KS");
	}
	
	private static Map<Integer, String> COUNTY_CODES = new HashMap<Integer, String>();
	static {
		COUNTY_CODES.put(CountyConstants.MO_Clay, "CL");
		COUNTY_CODES.put(CountyConstants.MO_Jackson, "JA");
		COUNTY_CODES.put(CountyConstants.MO_Platte, "PL");
		COUNTY_CODES.put(CountyConstants.KS_Johnson, "JO");
		COUNTY_CODES.put(CountyConstants.KS_Wyandotte, "WY");
	}
	
	public static String getRegionCode(int countyId) {
		String res = REGION_CODES.get(countyId);
		if (res==null) {
			res = "";
		}
		return res;
	}
	
	public static String getCountyCode(int countyId) {
		String res = COUNTY_CODES.get(countyId);
		if (res==null) {
			res = "";
		}
		return res;
	}
	
	@Override
	public LoginResponse onLogin() {
		
		setAttribute(SINGLE_LINE_COOKIES_IN_REDIRECT, true);
		
		// home page
		HTTPRequest request = new HTTPRequest(getSiteLink(), HTTPRequest.GET);
		execute(request);				
		
		String user = getPassword("user");
		String password = getPassword("password");
		
		if(StringUtils.isEmpty(user) || StringUtils.isEmpty(password)) {
			user = SitesPasswords.getInstance().getPasswordValue(getCurrentCommunityId(), "GenericOrbit", "user");
			password = SitesPasswords.getInstance().getPasswordValue(getCurrentCommunityId(), "GenericOrbit", "password");
		}
		
		if(StringUtils.isEmpty(user) || StringUtils.isEmpty(password)) {
			return new LoginResponse(LoginStatus.STATUS_INVALID_CREDENTIALS, "Couldn't find credentials for login");
		}

		// login
		request = new HTTPRequest(getSiteLink() + "/User/Login", HTTPRequest.POST);
		request.setPostParameter("Login", "Login");
		request.setPostParameter("UserName", user);
		request.setPostParameter("Password", password);
		String page = execute(request);		
		if(!page.contains("Welcome")){
			logger.error("Could not find text 'Welcome'!");
			return new LoginResponse(LoginStatus.STATUS_INVALID_PAGE, "Could not find text 'Welcome'!");
		}

		isCountySelected = false;
		
		return new LoginResponse(LoginStatus.STATUS_SUCCESS, "Logged in");
	}

	@Override
	public void onBeforeRequestExcl(HTTPRequest req) {
		
		String regionCode = getRegionCode(getDataSite().getCountyId());
		String countyCode = getCountyCode(getDataSite().getCountyId());
		
		String url = req.getURL();
		url = url.replace("|", "%7c").replaceAll("\\\\","%5c");
		url = url.replaceAll("(?i)%257c", "%7c");
		url = url.replaceAll("(=)\\{[^=]+=([^}]+)}", "$1$2");
		int searchCode = -1;
		if (url.contains(DOCUMENT_NUMBER_SEARCH_TEXT)) {
			searchCode = DOCUMENT_NUMBER_SEARCH_CODE;
			url = url + "&" + DOCUMENT_NUMBER_REGION_CODE_TEXT + regionCode + "&" + DOCUMENT_NUMBER_COUNTY_CODE_TEXT + countyCode;
		} else if (url.contains(NAME_SEARCH_TEXT)) {
			searchCode = NAME_SEARCH_CODE;
			url = url + "&" + NAME_REGION_CODE_TEXT + regionCode + "&" + NAME_COUNTY_CODE_TEXT + countyCode;
		} else if (url.contains(LEGAL_SEARCH_TEXT)) {
			searchCode = LEGAL_SEARCH_CODE;
			url = url + "&" + LEGAL_REGION_CODE_TEXT + regionCode + "&" + LEGAL_COUNTY_CODE_TEXT + countyCode;
		} else if (url.contains(COURT_SEARCH_TEXT)) {
			searchCode = COURT_SEARCH_CODE;
			url = url + "&" + COURT_REGION_CODE_TEXT + regionCode + "&" + COURT_COUNTY_CODE_TEXT + countyCode;
		}
		if (!url.startsWith("http")) {
			url = getSiteLink() + url;
		}
		if (searchCode==DOCUMENT_NUMBER_SEARCH_CODE) {
			url = url.replaceAll("(?is)\\b(" + DOCUMENT_NUMBER_REGION_CODE_TEXT + regionCode + ")(?:(,|%2c)" + regionCode + ")+", "$1");
			url = url.replaceAll("(?is)\\b(" + DOCUMENT_NUMBER_COUNTY_CODE_TEXT + countyCode + ")(?:(,|%2c)" + countyCode + ")+", "$1");
		} else if (searchCode==NAME_SEARCH_CODE) {
			url = url.replaceAll("(?is)\\b(" + NAME_REGION_CODE_TEXT + regionCode + ")(?:(,|%2c)" + regionCode + ")+", "$1");
			url = url.replaceAll("(?is)\\b(" + NAME_COUNTY_CODE_TEXT + countyCode + ")(?:(,|%2c)" + countyCode + ")+", "$1");
		} else if (searchCode==LEGAL_SEARCH_CODE) {
			url = url.replaceAll("(?is)\\b(" + LEGAL_REGION_CODE_TEXT + regionCode + ")(?:(,|%2c)" + regionCode + ")+", "$1");
			url = url.replaceAll("(?is)\\b(" + LEGAL_COUNTY_CODE_TEXT + countyCode + ")(?:(,|%2c)" + countyCode + ")+", "$1");
		} else if (searchCode==COURT_SEARCH_CODE) {
			url = url.replaceAll("(?is)\\b(" + COURT_REGION_CODE_TEXT + regionCode + ")(?:(,|%2c)" + regionCode + ")+", "$1");
			url = url.replaceAll("(?is)\\b(" + COURT_COUNTY_CODE_TEXT + countyCode + ")(?:(,|%2c)" + countyCode + ")+", "$1");
		}
		req.setURL(url);
		
		if (!isCountySelected) {
			if (searchCode!=-1) {
				HTTPRequest req0 = new HTTPRequest(getSiteLink() + "/" + SEARCH_TEXT, HTTPRequest.GET);
				execute(req0);
				HTTPRequest req1 = new HTTPRequest(getSiteLink() + "/" + SEARCH_TEXT + "/SetSearchType?searchType=1", HTTPRequest.POST);
				execute(req1);
				HTTPRequest req2 = new HTTPRequest(getSiteLink() + "/" + SEARCH_TEXT + "/GetArangeDate?CountyID=" + countyCode + 
						"&RegionID=" + regionCode, HTTPRequest.GET);
				execute(req2);
				HTTPRequest req3 = new HTTPRequest(getSiteLink() + "/" + SEARCH_TEXT + "/ClearSessionSearchModel?searchType=All", HTTPRequest.GET);
				execute(req3);
				isCountySelected = true;
			}
		}
		
	}

	@Override
	public void onAfterRequest(HTTPRequest req, HTTPResponse res) {
		if (res == null) {
			return;
		}
	}
	
	@Override
	protected void addSpecificSiteProxy() {
//		if (Utils.isJvmArgumentTrue("debugForATSProgrammer")) {
//        	HttpHost proxy = new HttpHost(ServerConfig.getDevUsProxyAddress(), ServerConfig.getDevUsProxyPort());
//        	getHttpClient().getParams().setParameter(ConnRoutePNames.DEFAULT_PROXY,proxy);
//        	
//	        /* Trust unsigned ssl certificates when using proxy */
//        	Scheme http = new Scheme("http", 80, PlainSocketFactory.getSocketFactory());
//    		SchemeRegistry sr = getHttpClient().getConnectionManager().getSchemeRegistry();
//    		sr.register(http);
//        }
	}
	
}
