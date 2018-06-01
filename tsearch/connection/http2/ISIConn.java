package ro.cst.tsearch.connection.http2;

import java.util.HashMap;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.httpclient.protocol.Protocol;

import ro.cst.tsearch.connection.LoginResponse;
import ro.cst.tsearch.connection.LoginResponse.LoginStatus;
import ro.cst.tsearch.connection.http.HTTPRequest;
import ro.cst.tsearch.connection.http.HTTPResponse;
import ro.cst.tsearch.connection.http.auth.ssl.EasySSLProtocolSocketFactory;
import ro.cst.tsearch.data.SitesPasswords;
import ro.cst.tsearch.parser.SimpleHtmlParser;
import ro.cst.tsearch.servers.HashCountyToIndex;
import ro.cst.tsearch.servers.types.TSServersFactory;
import ro.cst.tsearch.utils.InstanceManager;
import ro.cst.tsearch.utils.StringUtils;

/**
 * @author mihaib
 */

public class ISIConn extends HttpSite{
	
	private String loginViewState  = "";
	private static final String[] ADD_PARAM_NAMES = {"__VIEWSTATE"};
	private static final String[] REQ_PARAM_NAMES = { "__VIEWSTATE"};
	
	public static Hashtable<String,String> DDL_COUNTY;
	
	static {
		DDL_COUNTY = new Hashtable<String,String>();
		DDL_COUNTY.put("Cook", "31");
		DDL_COUNTY.put("DeKalb", "37");
		DDL_COUNTY.put("DuPage", "43");
		DDL_COUNTY.put("Kane", "89");
		DDL_COUNTY.put("Kendall", "93");
		DDL_COUNTY.put("Lake", "97");
		DDL_COUNTY.put("McHenry", "111");
		DDL_COUNTY.put("Will", "197");
		DDL_COUNTY.put("Boone", "7");
		DDL_COUNTY.put("Grundy", "63");
		DDL_COUNTY.put("LaSalle", "99");
		DDL_COUNTY.put("Winnebago", "201");
	}
	
	private String getLoginLink(){
		return getSiteLink();
	}
	
	/**
	 * Get the current server link
	 * @return
	 */
	public String getCrtServerLink(){
		String link = getSiteLink();
		int idx = link.indexOf(".com");
		if(idx == -1){
			throw new RuntimeException("County " + getDataSite().getName() + " not supported by this class!");
		}
		return link.substring(0, idx);
	}
	
	@Override
	public LoginResponse onLogin() {
		
		HTTPRequest req;
		HTTPResponse res;
		String pageResponse;
		
		String loginLink = getLoginLink();
		String link = getCrtServerLink();
		
		Protocol.registerProtocol("https", new Protocol("https", new EasySSLProtocolSocketFactory(), 443));
		
		//go to login page                 
		req = new HTTPRequest(loginLink);
		res = process(req);
		pageResponse = res.getResponseAsString();

		//isolate parameters
		Map<String,String> addParams = isolateParams(pageResponse, "frmMain");
		
		// check parameters
		if (!checkParams(addParams)){
			return new LoginResponse(LoginStatus.STATUS_INVALID_PAGE, "Invalid parameters");
		}
		setAttribute("params", addParams);
		
		// post login data
		req = new HTTPRequest(loginLink );
		req.setMethod(HTTPRequest.POST);
		req.setHeader("Referer", loginLink);
		req.setPostParameter("txtUserName" , SitesPasswords.getInstance().getPasswordValue(getCurrentCommunityId(), getClass().getSimpleName(), "user"));
		req.setPostParameter("txtPasswd" , SitesPasswords.getInstance().getPasswordValue(getCurrentCommunityId(), getClass().getSimpleName(), "password"));
		req.setPostParameter("btnLogin" , "Login" );
		req = addParams(addParams, req);

		res = process(req);
		pageResponse = res.getResponseAsString();
		
		// check for success
		if(pageResponse.contains("Login failed.")){
			return new LoginResponse(LoginStatus.STATUS_INVALID_CREDENTIALS, "Invalid credentials");
		}	
		if (!pageResponse.matches("(?is).*Your\\s*user\\s*account\\s*has\\s*been\\s*deactivated.*")) {
			
			// retrieve the search.aspx	page in order to get the __VIEWSTATE
			req = new HTTPRequest(link + ".com/ISI/search/search.aspx" );
			req.setMethod(HTTPRequest.GET);
			res = process(req);
			pageResponse = res.getResponseAsString();
			
			//isolate parameters
			addParams = isolateParams(pageResponse, "Form1");
			
			// check parameters
			if (!checkParams(addParams)){
				return new LoginResponse(LoginStatus.STATUS_INVALID_PAGE, "Invalid parameters");
			}
			setAttribute("params", addParams);
			
			//change the county on search page
			String county = InstanceManager.getManager().getCurrentInstance(searchId)
    			.getCrtSearchContext().getSa().getCountyName().replaceAll("[\\s\\p{Punct}]+", "");
			req = new HTTPRequest(link + ".com/ISI/search/search.aspx" );
			req.setMethod(HTTPRequest.POST);
			req.setPostParameter("__EVENTTARGET", "ddlCounty");
			req.setPostParameter("ddlCounty", DDL_COUNTY.get(county));
			req.setPostParameter("isMapVisible", "");
			req.setPostParameter("txtStreetNumberFrom", "");
			req.setPostParameter("txtStreetNumberTo", "");
			req.setPostParameter("txtStreetDirPre", "");
			req.setPostParameter("txtStreetName", "");
			req.setPostParameter("txtStreetDirPost", "");
			req.setPostParameter("txtStreetDesc", "");
			req.setPostParameter("txtAddressUnit", "");
			req.setPostParameter("txtPIN", "");
			req.setPostParameter("txtOwner", "");
			req.setPostParameter("txtSavedSearch", "");
			req = addParams(addParams, req);
			
			res = process(req);
			pageResponse = res.getResponseAsString();
			
			// isolate viewState
			loginViewState = StringUtils.getTextBetweenDelimiters("\"__VIEWSTATE\" value=\"" , "\"", pageResponse);
		}
		
		// indicate success
		return new LoginResponse(LoginStatus.STATUS_SUCCESS, "Logged in");
	}
	
	@SuppressWarnings("unchecked")
	@Override
	public void onBeforeRequestExcl(HTTPRequest req) {
		
		Pattern p = Pattern.compile("(?:interm|next):dgResults:_ctl(\\d+):_ctl(\\d+)");
		Matcher m = p.matcher(req.getURL());
		if(m.find()){
			if (req.getPostParameter("lnk") != null) {
				String lnk = req.getPostFirstParameter("lnk");
				req.removePostParameters("lnk");
			
				Map<String,String> addParams = (Map<String, String>)getTransientSearchAttribute("params:" + lnk);
				req.modifyURL(addParams.get("nextLink"));
				req.setPostParameter("__VIEWSTATE", addParams.get("__VIEWSTATE"));
				req.setPostParameter("__EVENTTARGET" , "dgResults:_ctl" + m.group(1) + ":_ctl" + m.group(2));
				req.setPostParameter("__EVENTARGUMENT" , "");
			return;
			}
		}
		
		// check if a request from search page
		if("Search".equals(req.getPostFirstParameter("btnSearch"))){
    		req.setPostParameter("__VIEWSTATE" , loginViewState);
    		req.setPostParameter("__EVENTTARGET" , "");
    		req.setPostParameter("__EVENTARGUMENT" , "");
    	
    		return;
		} 

	}	
	
	public static Map<String, String> isolateParams(String page, String form){
		Map<String, String> params = new SimpleHtmlParser(page).getForm(form).getParams();
		Map<String,String> addParams = new HashMap<String,String>();
		for (String key : ADD_PARAM_NAMES) {
			String value = "";
			if (params.containsKey(key)) {
				value = params.get(key);
			}
			addParams.put(key, value);
		}
		
		return addParams;
	}
	
	public static HTTPRequest addParams(Map<String, String> addParams, HTTPRequest request){
		Iterator<String> i = addParams.keySet().iterator();
		while(i.hasNext()){
			String k = i.next();
			request.setPostParameter(k, addParams.get(k));
		}		
		return request;
	}
	
	public static boolean checkParams(Map<String, String> addParams){
		for(String key: REQ_PARAM_NAMES){
			if(StringUtils.isEmpty(addParams.get(key))){
				return false;
			}
		}
		return true;
	}

}
