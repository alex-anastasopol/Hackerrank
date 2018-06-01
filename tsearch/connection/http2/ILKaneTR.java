/**
 * 
 */
package ro.cst.tsearch.connection.http2;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import ro.cst.tsearch.connection.LoginResponse;
import ro.cst.tsearch.connection.LoginResponse.LoginStatus;
import ro.cst.tsearch.connection.http.HTTPRequest;
import ro.cst.tsearch.connection.http.HTTPResponse;
import ro.cst.tsearch.parser.SimpleHtmlParser;
import ro.cst.tsearch.utils.StringUtils;

/**
 * @author mihaib
  */
public class ILKaneTR extends HttpSite {
	
	private static final String[] ADD_PARAM_NAMES = { "__VIEWSTATE", "__EVENTVALIDATION",};
	private static final String[] REQ_PARAM_NAMES = { "__VIEWSTATE", "__EVENTVALIDATION" };
	public String cookie="";
		
	@Override
	public LoginResponse onLogin() {

		String link = getCrtServerLink() + ".us/TaxAssessment/";
		String resp = "";
		HTTPRequest req = new HTTPRequest(link);
		HTTPResponse res = process(req);
		this.cookie = res.getHeader("Set-Cookie");
		//resp = execute(req);
		resp = res.getResponseAsString();
		
		//isolate parameters
		Map<String,String> addParams = isolateParams(resp, "Access");
		
		// check parameters
		if (!checkParams(addParams)){
			return new LoginResponse(LoginStatus.STATUS_INVALID_PAGE, "Invalid parameters");
		}
		setAttribute("params", addParams);
		
		return new LoginResponse(LoginStatus.STATUS_SUCCESS, "Logged in");
	}
	
	/**
	 * Get the current server link
	 * @return
	 */
	private String getCrtServerLink(){
		String link = getSiteLink();
		int idx = link.indexOf(".us");
		if(idx == -1){
			throw new RuntimeException("County " + getDataSite().getName() + " not supported by this class!");
		}
		return link.substring(0, idx);
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
	public void onBeforeRequestExcl(HTTPRequest req) {
		@SuppressWarnings("unused")
		String resp = "";
		req.setHeader("Host", "www.co.kane.il.us");
		Map<String,String> addParams = null;
		if (req.getURL().indexOf("Treasurer.aspx") == -1){				
			req.setMethod(HTTPRequest.POST);
			addParams = (HashMap<String,String>)getAttribute("params");
			
			//isolate parameters 
			req = addParams(addParams, req);
		} else {
			
			req.setHeader("Referer", "http://www.co.kane.il.us/TaxAssessment/Access.aspx");
		}
		req.setHeader("Cookie", this.cookie);
		resp = execute(req);
		
		return;		
		
	}
}