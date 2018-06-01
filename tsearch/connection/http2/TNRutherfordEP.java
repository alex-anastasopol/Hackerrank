package ro.cst.tsearch.connection.http2;

import java.util.HashMap;
import java.util.Map;

import ro.cst.tsearch.connection.LoginResponse;
import ro.cst.tsearch.connection.LoginResponse.LoginStatus;
import ro.cst.tsearch.connection.http.HTTPRequest;
import ro.cst.tsearch.connection.http.HTTPResponse;
import ro.cst.tsearch.parser.SimpleHtmlParser;
import ro.cst.tsearch.utils.StringUtils;

/**
 * @author mihai blendea
  */
public class TNRutherfordEP extends HttpSite {
	
	private static final String[] ADD_PARAM_NAMES = { "__VIEWSTATE", "__EVENTTARGET", "__EVENTARGUMENT" };
	private static final String[] REQ_PARAM_NAMES = { "__VIEWSTATE" };
	
	@Override
	public LoginResponse onLogin() {
		
		String link = getCrtServerLink() + ".gov/search.aspx";
		String cookietmp="";
		
		HTTPRequest req = new HTTPRequest(link);
		
		HTTPResponse res = process(req);
		String resp = res.getResponseAsString();
		
		cookietmp = res.getHeader("Set-Cookie");
		req.setHeader("Host", "www.murfreesborotn.gov");
		req.setHeader("Referer", "http://www.murfreesborotn.gov/search.aspx");
		req.setHeader("Cookie",cookietmp);
				
		//isolate parameters
		Map<String,String> addParams = isolateParams(resp, "aspnetForm");
		
		// check parameters
		if (!checkParams(addParams)){
			return new LoginResponse(LoginStatus.STATUS_INVALID_PAGE, "Invalid parameters");
		}
		setAttribute("params", addParams);
		
		setAttribute(HttpSite.SINGLE_LINE_COOKIES_IN_REDIRECT, Boolean.TRUE);
		
		return new LoginResponse(LoginStatus.STATUS_SUCCESS, "Logged in");
	}
	
	/**
	 * Get the current server link
	 * @return
	 */
	private String getCrtServerLink(){
		String link = getSiteLink();
		int idx = link.indexOf(".gov");
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
	
	public static boolean checkParams(Map<String, String> addParams){
		for(String key: REQ_PARAM_NAMES){
			if(StringUtils.isEmpty(addParams.get(key))){
				return false;
			}
		}
		return true;
	}

	@SuppressWarnings("unchecked")
	@Override
	public void onBeforeRequest(HTTPRequest req) {
						
		Map<String, String>  addParams = new HashMap<String, String>();
		String keyParameter = req.getPostFirstParameter("seq");
		if (keyParameter!=null)
		{
			addParams = (Map<String, String>) getTransientSearchAttribute("params:" + keyParameter);
			req.removePostParameters("seq");
		}	
		if (addParams!=null)
		{
			for (String k: addParams.keySet())
				req.setPostParameter(k, addParams.get(k));
		}
	}
}
