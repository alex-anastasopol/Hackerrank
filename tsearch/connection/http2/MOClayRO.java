package ro.cst.tsearch.connection.http2;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.httpclient.HostConfiguration;
import org.apache.commons.httpclient.protocol.Protocol;

import ro.cst.tsearch.connection.LoginResponse;
import ro.cst.tsearch.connection.LoginResponse.LoginStatus;
import ro.cst.tsearch.connection.http.HTTPRequest;
import ro.cst.tsearch.connection.http.HTTPResponse;
import ro.cst.tsearch.connection.http.auth.ssl.EasySSLProtocolSocketFactory;
import ro.cst.tsearch.datatrace.Utils;
import ro.cst.tsearch.parser.SimpleHtmlParser;
import ro.cst.tsearch.utils.StringUtils;

public class MOClayRO extends HttpSite {
	
	private static final String[] ADD_PARAM_NAMES = { "__LASTFOCUS", "__VIEWSTATE", "__EVENTTARGET", "__EVENTARGUMENT", "__EVENTVALIDATION" };
	private static final String[] REQ_PARAM_NAMES = { "__VIEWSTATE", "__EVENTVALIDATION" };
	
	private static final String FORM_NAME = "ctl00";
	
	public LoginResponse onLogin( )	{
		
		setAttribute(SINGLE_LINE_COOKIES_IN_REDIRECT,true);
		
		String user = getPassword("user");
		if(StringUtils.isEmpty(user)) {
			return LoginResponse.getDefaultInvalidCredentialsResponse();
		}
		
		//String logInToUrl = "http://recorder.claycogov.com/iRecordClient/Login.aspx";
            
        HTTPRequest req = new HTTPRequest(getSiteLink());    
        req.setMethod( HTTPRequest.GET );
        String response = process( req ).getResponseAsString();
        //isolate parameters
		Map<String, String> params = new SimpleHtmlParser(response).getForm("LOGIN").getParams();
		Map<String,String> addParams = new HashMap<String,String>();
		for (String key : ADD_PARAM_NAMES) {
			String value = "";
			if (params.containsKey(key)) {
				value = params.get(key);
			}
			addParams.put(key, value);
		}
        
		req.setMethod(HTTPRequest.POST);
		req.clearPostParameters();
		req.setPostParameter( "btnLogin", "Login" );
		req.setPostParameter("USERID", getPassword("user"));
		req.setPostParameter("PASSWORD", getPassword("password"));
		
		// check parameters
		for(String key: REQ_PARAM_NAMES){
			if(StringUtils.isEmpty(addParams.get(key))){
				return new LoginResponse(LoginStatus.STATUS_INVALID_PAGE, "Invalid parameters");
			} else {
				req.setPostParameter(key, addParams.get(key));
			}
		}
		
		response = process( req ).getResponseAsString();
		params = new SimpleHtmlParser(response).getForm("FORM_CRITERIA").getParams();
		addParams = new HashMap<String,String>();
		for (String key : ADD_PARAM_NAMES) {
			String value = "";
			if (params.containsKey(key)) {
				value = params.get(key);
			}
			addParams.put(key, value);
		}
		// check parameters
		for(String key: REQ_PARAM_NAMES){
			if(StringUtils.isEmpty(addParams.get(key))){
				return new LoginResponse(LoginStatus.STATUS_INVALID_PAGE, "Invalid parameters");
			}
		}
		
		// store parameters
		setAttribute("name.simple.params", addParams);

		// ----- start advanced name search ----------
		req.clearPostParameters();
		for (String key : REQ_PARAM_NAMES) {
			req.setPostParameter(key, addParams.get(key));
		}
		req.setPostParameter("__EVENTTARGET", "lbSearchType");
		req.setPostParameter("NameType", "RBTN_ALL");
		req.modifyURL("http://recorder.claycogov.com/iRecordClient/REALSearchByName.aspx");
		response = process( req ).getResponseAsString();
		params = new SimpleHtmlParser(response).getForm("FORM_CRITERIA").getParams();
		addParams = new HashMap<String,String>();
		for (String key : ADD_PARAM_NAMES) {
			String value = "";
			if (params.containsKey(key)) {
				value = params.get(key);
			}
			addParams.put(key, value);
		}
		// check parameters
		for(String key: REQ_PARAM_NAMES){
			if(StringUtils.isEmpty(addParams.get(key))){
				return new LoginResponse(LoginStatus.STATUS_INVALID_PAGE, "Invalid parameters");
			}
		}
		
		// store parameters
		setAttribute("name.advanced.params", addParams);
		// ----- end advanced name search ----------
		
		// ----- start simple book-page search ----------
		req.clearPostParameters();
		req.modifyURL("http://recorder.claycogov.com/iRecordClient/REALSearchByBookPage.aspx");
		req.setMethod(HTTPRequest.GET);
				
		response = process( req ).getResponseAsString();
		params = new SimpleHtmlParser(response).getForm("FORM_CRITERIA").getParams();
		addParams = new HashMap<String,String>();
		for (String key : ADD_PARAM_NAMES) {
			String value = "";
			if (params.containsKey(key)) {
				value = params.get(key);
			}
			addParams.put(key, value);
		}
		// check parameters
		for(String key: REQ_PARAM_NAMES){
			if(StringUtils.isEmpty(addParams.get(key))){
				return new LoginResponse(LoginStatus.STATUS_INVALID_PAGE, "Invalid parameters");
			}
		}
		
		// store parameters
		setAttribute("bp.simple.params", addParams);
		// ----- end simple book-page search ----------
		
		// ----- start advanced book-page search ----------
		req.clearPostParameters();
		req.setMethod(HTTPRequest.POST);
		for (String key : REQ_PARAM_NAMES) {
			req.setPostParameter(key, addParams.get(key));
		}
		req.setPostParameter("__EVENTTARGET", "lbSearchType");
		req.setPostParameter("NameType", "RBTN_ALL");
		
		response = process( req ).getResponseAsString();
		params = new SimpleHtmlParser(response).getForm("FORM_CRITERIA").getParams();
		addParams = new HashMap<String,String>();
		for (String key : ADD_PARAM_NAMES) {
			String value = "";
			if (params.containsKey(key)) {
				value = params.get(key);
			}
			addParams.put(key, value);
		}
		// check parameters
		for(String key: REQ_PARAM_NAMES){
			if(StringUtils.isEmpty(addParams.get(key))){
				return new LoginResponse(LoginStatus.STATUS_INVALID_PAGE, "Invalid parameters");
			}
		}
		
		// store parameters
		setAttribute("bp.advanced.params", addParams);
		// ----- end advanced book-page search ----------
		
		
		return new LoginResponse(LoginStatus.STATUS_SUCCESS, "Logged in");
	}
	
	@SuppressWarnings("unchecked")
	@Override
	public void onBeforeRequestExcl(HTTPRequest req) {
		req.setHeader("Host", "recorder.claycogov.com");
		req.removePostParameters("SearchInstructions");
		Map<String,String> addParams = null;
		if (StringUtils.isNotEmpty((String)req.getPostFirstParameter("LNAME")) && req.getPostFirstParameter("TYPE") == null){
			addParams = (HashMap<String,String>)getAttribute("name.simple.params");
			req.setHeader("Referer", "http://recorder.claycogov.com/iRecordClient/REALSearchByName.aspx");
			
			if(addParams != null) {			
				for (String key : ADD_PARAM_NAMES) {
					String value = "";
					if (addParams.containsKey(key)) {
						value = addParams.get(key);
					}
					req.setPostParameter(key, value);
				}			
			}
		} else if (req.getURL().contains("DisplayDocument.aspx")){
			String resp = execute(req);
			if (resp.contains("Click here to View the Document")){
				String link = req.getURL();
				String imageLink = link.substring(0, link.indexOf(".mo.us")) + ".mo.us";
				Pattern pat = Pattern.compile("(?is)href\\s*=\\s*[\\\"|']([^\\\"]+)\\\"[^>]*>Click here to View the Document");
				Matcher mat = pat.matcher(resp);
				if (mat.find()){
					HTTPRequest req1 = new HTTPRequest(imageLink + "/" + mat.group(1), HTTPRequest.GET);
					HTTPResponse res = process(req1);
					
					res.contentType = "application/pdf";
					// bypass response
					res.is = res.getResponseAsStream();
					req.setBypassResponse(res);
				}
			}
		} else if (!req.getURL().contains("REALSummary.aspx")){
			if (req.getPostFirstParameter("pageParam") != null){
				String pageParam = req.getPostFirstParameter("pageParam");
				req.removePostParameters("pageParam");
				
				req.setPostParameter("__EVENTTARGET", pageParam);
				String seq = req.getPostFirstParameter("seq");
				req.removePostParameters("seq");
				
				// additional parameters are stored inside the search since they are too long
				addParams = (Map<String, String>)getTransientSearchAttribute("params:" + seq);			
				for(Map.Entry<String, String> entry: addParams.entrySet()){
					req.setPostParameter(entry.getKey(), entry.getValue());
				}
			} else{
				if (req.getURL().contains("REALSearchByBookPage.aspx") && req.getPostFirstParameter("TYPE") == null){// go to book page module
					HTTPRequest req1 = new HTTPRequest(req.getURL(), HTTPRequest.GET);
					String resp = execute(req1);
					addParams = isolateParams(resp, "FORM_CRITERIA");
					if (checkParams(addParams)){
						setAttribute("params", addParams);
					}
				}
				
				if (req.getPostFirstParameter("TYPE") != null && req.getPostFirstParameter("ACTION") == null){// go to advanced search
					HTTPRequest req1 = new HTTPRequest(req.getURL(), HTTPRequest.GET);
					String resp = execute(req1);
					addParams = isolateParams(resp, "FORM_CRITERIA");
					if (checkParams(addParams)){
						setAttribute("params", addParams);
					}
					
					req1 = new HTTPRequest(req.getURL(), HTTPRequest.POST);
					addParams = (HashMap<String,String>)getAttribute("params");
					req1.addPostParameters(addParams);
					req1.removePostParameters("__EVENTTARGET");
					req1.setPostParameter("__EVENTTARGET", "lbSearchType");
					
					if (req.getURL().contains("REALSearchByName.aspx")){
						req1.setPostParameter("LNAME", "");
						req1.setPostParameter("FNAME", "");
						req1.setPostParameter("NameType", "RBTN_ALL");
					} else if (req.getURL().contains("REALSearchByBookPage.aspx")){
						req1.setPostParameter("SEARCH_INSTRUMENT_NUMBER", "");
						req1.setPostParameter("SEARCH_BOOK", "");
						req1.setPostParameter("SEARCH_PAGE", "");
					}
					resp = execute(req1);
					addParams = isolateParams(resp, "FORM_CRITERIA");
					if (checkParams(addParams)){
						setAttribute("params", addParams);
					}
					
					req.removePostParameters("TYPE");
				}
				
				addParams = (HashMap<String,String>)getAttribute("params");
				req.addPostParameters(addParams);
			}
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
	
	@Override
	protected void addSpecificSiteProxy() {
		if (Utils.isJvmArgumentTrue("debugForATSProgrammer")) {
			HostConfiguration config = getHttpClient().getHostConfiguration();
	        config.setProxy("192.168.92.55", 8080);
	        
	        /* Trust unsigned ssl certificates when using proxy */
        	Protocol.registerProtocol("https", new Protocol("https", new EasySSLProtocolSocketFactory(), 443));
        }
	}

}
