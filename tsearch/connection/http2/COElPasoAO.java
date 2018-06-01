
package ro.cst.tsearch.connection.http2;

import java.io.ByteArrayInputStream;
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
public class COElPasoAO extends HttpSite {
	
	private static final String[] ADD_PARAM_NAMES = { "__LASTFOCUS", "__VIEWSTATE", "__EVENTARGUMENT", "__EVENTVALIDATION",};
	private static final String[] REQ_PARAM_NAMES = { "__VIEWSTATE", "__EVENTVALIDATION" };
	public String cookie="";
		
	@Override
	public LoginResponse onLogin() {

		String link = getCrtServerLink() + ".com/";
		String resp = "";
		HTTPRequest req = new HTTPRequest(link);
		HTTPResponse res = process(req);
		this.cookie=res.getHeader("Set-Cookie");
		resp = res.getResponseAsString();
		
		//isolate parameters
		Map<String,String> addParams = isolateParams(resp, "aspnetForm");
		
		// check parameters
		if (!checkParams(addParams)){
			return new LoginResponse(LoginStatus.STATUS_INVALID_PAGE, "Invalid parameters");
		}
		setAttribute("params", addParams);
		
		return new LoginResponse(LoginStatus.STATUS_SUCCESS, "Logged in");
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
		
	/**
	 * Get the current server link
	 * @return
	 */
	private String getCrtServerLink(){
		String link = getSiteLink();
		int idx = link.indexOf(".com");
		if(idx == -1){
			throw new RuntimeException("County " + getDataSite().getName() + " not supported by this class!");
		}
		return link.substring(0, idx);
	}

	@SuppressWarnings("unchecked")
	@Override
	public void onBeforeRequestExcl(HTTPRequest req) {
		Map<String,String> addParams = (Map<String, String>) getAttribute("params");
		if (addParams != null){
			for(Map.Entry<String, String> entry: addParams.entrySet()){
				req.setPostParameter(entry.getKey(), entry.getValue());
			}
		}
	}
	
	@Override
	public void onAfterRequest(HTTPRequest req, HTTPResponse res) {
		String content = "";
		if (res != null){
			if (res.getContentType().contains("text/html")){
				content = res.getResponseAsString();
				res.is = new ByteArrayInputStream(content.getBytes());
				if(content.contains("Your data has timed-out")) {
					destroySession();
				}
			}
		}
		
	}
}