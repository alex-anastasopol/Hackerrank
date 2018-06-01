
package ro.cst.tsearch.connection.http2;

import static ro.cst.tsearch.connection.http.HTTPRequest.GET;

import java.util.HashMap;
import java.util.Map;

import ro.cst.tsearch.connection.LoginResponse;
import ro.cst.tsearch.connection.LoginResponse.LoginStatus;
import ro.cst.tsearch.connection.http.HTTPRequest;
import ro.cst.tsearch.parser.SimpleHtmlParser;

/**
 * @author vladb
 *
 */
public class MDPrinceGeorgesTR extends HttpSite {
	private static final String FORM_NAME = "Form1";
	private static final String[] REQ_PARAM_NAMES = { "__VIEWSTATE" };
	
	@Override
	public LoginResponse onLogin() {
		
		try {
			// go to taxes search 
			HTTPRequest req = new HTTPRequest(getSiteLink(), GET);
			String p = execute(req);
			
			Map<String, String> params = new SimpleHtmlParser(p).getForm(FORM_NAME).getParams();
			Map<String, String> reqParams = new HashMap<String, String>();
			
			// check parameters
			for(String key : REQ_PARAM_NAMES){
				if(!params.containsKey(key)){
					String errorMsg = "Did not find required parameter " + key;
					logger.error(errorMsg);
					return new LoginResponse(LoginStatus.STATUS_INVALID_PAGE, errorMsg);
				} else {
					reqParams.put(key, params.get(key));
				}
			}
				
			// store parameters
			setAttribute("params", reqParams);
		
			return new LoginResponse(LoginStatus.STATUS_SUCCESS, "Logged in");
		} catch (Exception e) {
			e.printStackTrace();
		}
		
		return new LoginResponse(LoginStatus.STATUS_UNKNOWN, "Login failed");
	}
	
	@SuppressWarnings("unchecked")
	@Override
	public void onBeforeRequestExcl(HTTPRequest req) { 
		 
		Map<String, String> params = null;
		
		if(req.hasPostParameter("seq")) { // details link
			// remove the seq parameter
			String seq = req.getPostFirstParameter("seq");
			req.removePostParameters("seq");
	
			params = (Map<String, String>) getTransientSearchAttribute("params:" + seq);
			
		} else { // search from Parent Site
			params = (Map<String,String>)getAttribute("params");
		}
		
		// check parameters
		if(params != null) {
			for(String key : REQ_PARAM_NAMES){
				if(!params.containsKey(key)){
					logger.error("Did not find required parameter " + key);
				}
			}
			for(Map.Entry<String, String> entry : params.entrySet()){
				if(!req.hasPostParameter(entry.getKey())) {
					req.setPostParameter(entry.getKey(), entry.getValue());
				}
			}
		}
		
	}
}
