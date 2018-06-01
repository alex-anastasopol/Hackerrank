package ro.cst.tsearch.connection.http2;

import java.util.HashMap;
import java.util.Map;

import org.apache.commons.httpclient.Header;

import ro.cst.tsearch.connection.LoginResponse;
import ro.cst.tsearch.connection.LoginResponse.LoginStatus;
import ro.cst.tsearch.connection.http.HTTPRequest;
import ro.cst.tsearch.parser.SimpleHtmlParser;
import ro.cst.tsearch.utils.StringUtils;

public class KSJohnsonAO extends HttpSite {

	private static final String[] ADD_PARAM_NAMES = { "__LASTFOCUS", "__VIEWSTATE", "__EVENTTARGET", "__EVENTARGUMENT", "__EVENTVALIDATION" };
	private static final String[] REQ_PARAM_NAMES = { "__VIEWSTATE", "__EVENTVALIDATION" };
	
	private static final String FORM_NAME = "Form1";
	
	@Override
	public LoginResponse onLogin() {
		
		HTTPRequest req;
		
		req = new HTTPRequest("http://land.jocogov.org/default.aspx", HTTPRequest.GET);
		String response = execute(req);
		//isolate parameters
		Map<String, String> params = new SimpleHtmlParser(response).getForm(FORM_NAME).getParams();
		Map<String,String> addParams = new HashMap<String,String>();
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
		
		req = new HTTPRequest("http://land.jocogov.org/default.aspx", HTTPRequest.POST);
		for(String key: REQ_PARAM_NAMES){
			req.setPostParameter(key, addParams.get(key));	
		}
		
		req.setPostParameter("btnYes", "I Agree");
		
		response = execute(req);
		params = new SimpleHtmlParser(response).getForm(FORM_NAME).getParams();
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
		
		if(response.contains("btnYes")) {
		
			req = new HTTPRequest("http://land.jocogov.org/landsearch.aspx", HTTPRequest.GET);
			for(String key: REQ_PARAM_NAMES){
				req.setPostParameter(key, addParams.get(key));	
			}
			response = execute(req);
			
			params = new SimpleHtmlParser(response).getForm(FORM_NAME).getParams();
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
		
		}
		
		setAttribute("params", addParams);
		
		return new LoginResponse(LoginStatus.STATUS_SUCCESS, "Logged in");		
	}

	@SuppressWarnings("unchecked")
	@Override
	public void onBeforeRequestExcl(HTTPRequest req) {
		if (req.getMethod() == HTTPRequest.GET) {
			String url = req.getURL();
			if (org.apache.commons.lang.StringUtils.containsIgnoreCase(url, "summary.aspx")) {
				req.setURL(url.replaceAll("\\+", "%20"));
			}
		}
		
		Map<String,String> addParams = null;
		if(req.hasPostParameter("__VIEWSTATE")){
			addParams = (HashMap<String,String>)getAttribute("params");
			for(String key: REQ_PARAM_NAMES){
				req.removePostParameters(key);
				req.setPostParameter(key, addParams.get(key));	
			}
		}
		if(req.hasPostParameter("ddlDir")) {
			String ddlDir = req.getPostFirstParameter("ddlDir");
			if(ddlDir.trim().isEmpty()) {
				req.removePostParameters("ddlDir");
				req.setPostParameter("ddlDir", "All");
			}
		}
		if(req.hasPostParameter("ddlcity")) {
			String ddlCity = req.getPostFirstParameter("ddlcity");
			if(ddlCity.trim().isEmpty()) {
				req.removePostParameters("ddlcity");
				req.setPostParameter("ddlcity", "All");
			}
		}
		req.removePostParameters("__EVENTTARGET");
		req.setPostParameter("__EVENTTARGET", "");
		req.removePostParameters("__EVENTARGUMENT");
		req.setPostParameter("__EVENTARGUMENT", "");
	}
	
	@Override
	public void onRedirect(HTTPRequest req) {
		if (status != STATUS_LOGGED_IN) {
			return;
		}
		if (getAttribute("redirect-cookie") instanceof Header) {
			Header cookie = (Header) getAttribute("redirect-cookie");
			req.setHeader(cookie.getName(), cookie.getValue());
		}
	}


}
