package ro.cst.tsearch.connection.http2;


import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.apache.commons.httpclient.Cookie;
import org.apache.commons.httpclient.HttpState;
import org.apache.commons.io.IOUtils;

import ro.cst.tsearch.connection.LoginResponse;
import ro.cst.tsearch.connection.LoginResponse.LoginStatus;
import ro.cst.tsearch.connection.http.HTTPRequest;
import ro.cst.tsearch.connection.http.HTTPResponse;
import ro.cst.tsearch.parser.SimpleHtmlParser;
import ro.cst.tsearch.parser.SimpleHtmlParser.Form;
import ro.cst.tsearch.utils.StringUtils;



/**
 * @author mihaib
*/

public class NVSoSConnCC extends HttpSite{


	private static final String[] ADD_PARAM_NAMES = { "__VIEWSTATE", "__EVENTTARGET", "__EVENTARGUMENT", "__EVENTVALIDATION" };
	private static final String[] REQ_PARAM_NAMES = { "__VIEWSTATE", "__EVENTVALIDATION" };
	
	@Override
	public LoginResponse onLogin() {
		
		setAttribute(SINGLE_LINE_COOKIES_IN_REDIRECT, true);
		
		HTTPRequest req = new HTTPRequest(getSiteLink(), HTTPRequest.GET);
	
		HTTPResponse res = process(req);
		String response = res.getResponseAsString();
		
		String maintenanceMessage = StringUtils.extractParameter(response,
				"(?is)This\\s+business\\s+search\\s+is\\s+offline\\s+for\\s+routine\\s+maintenance\\s*.");
		if (!maintenanceMessage.isEmpty()) {
			return new LoginResponse(LoginStatus.STATUS_INVALID_PAGE, maintenanceMessage);
		}
		
		// isolate parameters
		Map<String, String> addParams = isolateParams(response, "aspnetForm");

		// check parameters
		if (!checkParams(addParams)) {
			return new LoginResponse(LoginStatus.STATUS_INVALID_PAGE, "Invalid parameters");
		}

		setAttribute("params", addParams);

		// indicate success
		return new LoginResponse(LoginStatus.STATUS_SUCCESS, "Logged in");
		
	}
	
	@SuppressWarnings("unchecked")
	@Override
	public void onBeforeRequestExcl(HTTPRequest req) {
    	
		if (req.getMethod() == HTTPRequest.POST){
			if (req.getPostFirstParameter("pageParam") != null){
				Map<String,String> navParams = (Map<String, String>)getTransientSearchAttribute("paramsNav:");
				
				if (navParams != null){
					req.addPostParameters(navParams);
				}
				
				req.removePostParameters("__EVENTTARGET");
				req.setPostParameter("__EVENTTARGET", req.getPostFirstParameter("pageParam"));
				req.removePostParameters("pageParam");
				
			} else {
				HTTPRequest newReq = new HTTPRequest(req.getURL(), HTTPRequest.GET);
				String response = execute(newReq);
				Map<String,String> addParams = isolateParams(response, "aspnetForm");

				if (addParams != null){
					addParams(addParams, req);
				}
			}
		} else if (req.getURL().contains("CorpDetails.aspx")){
			String url = req.getURL();
			if (url.contains("%") && !url.contains("%25")){
				url = url.replaceAll("%", "%25");
				req.setURL(url);
			}
			
			boolean includeInactive = false;
			HttpState httpState = getHttpClient().getState();
			Cookie[] cookies = httpState.getCookies();
			for (Cookie cookie : cookies) {
				if (cookie.getName().equals("NVSOSIncludeInActive")){
					if (cookie.getValue().equalsIgnoreCase("True")){
						includeInactive = true;
					}
				}
			}
			
			if (!includeInactive){//do it in 2 steps only if the cookie has value true
				//request for details
				String response = execute(req);
				
				//another request for the same details with Include Inactive Officers checkbox checked
				if (response.contains("ctl00$MainContent$ctl00$chkIncludeInActive")){
					Map<String,String> addParams = isolateParams(response, "aspnetForm");
					HTTPRequest newReq = new HTTPRequest(req.getURL(), HTTPRequest.POST);
					if (addParams != null){
						addParams(addParams, newReq);
						newReq.setPostParameter("__EVENTTARGET", "ctl00$MainContent$ctl00$chkIncludeInActive");
						newReq.setPostParameter("ctl00$MainContent$ctl00$chkIncludeInActive", "on");
						newReq.setPostParameter("Header1:SearchBox1:request", "Search...");
						
						HTTPResponse httpResp = process(newReq);
						
						response = httpResp.getResponseAsString();
					
						// bypass response
						httpResp.is = IOUtils.toInputStream(response);
						req.setBypassResponse(httpResp);
						
						addParams = isolateParams(response, "aspnetForm");
						if (checkParams(addParams)){
							setAttribute("params", addParams);
						}
							
					}
				}
			}
			
		}
			
	}
	
	public String getPage(String link) {
		HTTPRequest req = new HTTPRequest(getDataSite().getServerHomeLink() + link, HTTPRequest.GET);
		
		String response = execute(req);
		Map<String,String> addParams = isolateParams(response, "aspnetForm");
		if (checkParams(addParams)){
			//setAttribute("params", addParams);
		}
		
		return response;
	}
	
	public static Map<String, String> isolateParams(String page, String formString) {
		Map<String, String> addParams = new HashMap<String, String>();
		Form form = new SimpleHtmlParser(page).getForm(formString);
		if (form != null) {
			Map<String, String> params = form.getParams();
			for (String key : ADD_PARAM_NAMES) {
				String value = "";
				if (params.containsKey(key)) {
					value = params.get(key);
				}
				addParams.put(key, value);
			}
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
	public void onAfterRequest(HTTPRequest req, HTTPResponse res) {

		if(req.getURL().contains("corpActions.aspx") || (res == null || res.returnCode == 0)){
			//destroySession();
		}	
	}
	
}