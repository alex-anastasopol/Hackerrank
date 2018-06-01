/**
 * 
 */
package ro.cst.tsearch.connection.http2;


import static ro.cst.tsearch.connection.http.HTTPRequest.GET;

import java.util.HashMap;
import java.util.Map;

import ro.cst.tsearch.connection.LoginResponse;
import ro.cst.tsearch.connection.LoginResponse.LoginStatus;
import ro.cst.tsearch.connection.http.HTTPRequest;
import ro.cst.tsearch.parser.SimpleHtmlParser;
import ro.cst.tsearch.utils.StringUtils;

/**
 * @author vladb
  */
public class MOCassTR extends HttpSite {
	
	private static final String[] REQ_PARAM_NAMES = { "__VIEWSTATE", "__EVENTVALIDATION" };
	private static final String[] ADD_PARAM_NAMES = { "ctl00$mainContent$cboYears" };
	private static final String FORM_NAME = "aspnetForm";
		
	@Override
	public LoginResponse onLogin() {

		HTTPRequest req = new HTTPRequest(getSiteLink(), GET);
		String p = execute(req);
		
		Map<String, String> params = new SimpleHtmlParser(p).getForm(FORM_NAME).getParams();
		Map<String, String> addParams = new HashMap<String, String>();
		
		// check parameters
		for(String key : REQ_PARAM_NAMES){
			if(!params.containsKey(key)){
				logger.error("Did not find required parameter " + key);
				return new LoginResponse(LoginStatus.STATUS_INVALID_PAGE, "Did not find required parameter " + key);
			}
			addParams.put(key, params.get(key));
		}
			
		// store parameters
		setAttribute("params", addParams);
		
		return new LoginResponse(LoginStatus.STATUS_SUCCESS, "Logged in");
	}
	
	
	@SuppressWarnings("unchecked")
	@Override
	public void onBeforeRequestExcl(HTTPRequest req) {
		
		if(req.hasPostParameter("seq")) { 
			String seq = req.getPostFirstParameter("seq");
			req.removePostParameters("seq");
	
			Map<String, String> params = (Map<String, String>) getTransientSearchAttribute("params:" + seq);			
			if(params != null){
				String url = req.getURL();
				url = StringUtils.urlDecode(req.getURL());
				url = url.replaceAll("&amp;", "&");
				req.modifyURL(url);
				for(String key : REQ_PARAM_NAMES){
					if(params.containsKey(key)) {
						req.setPostParameter(key, params.get(key));
					} else {
						logger.error("Did not find required parameter " + key);
					}
				}
				for(String key : ADD_PARAM_NAMES){
					if(params.containsKey(key)) {
						req.setPostParameter(key, params.get(key));
					}
				}
			}
		} else {
			Map<String,String> params = (Map<String,String>)getAttribute("params");
			if(params != null) {
				for(Map.Entry<String, String> entry : params.entrySet()){
					req.setPostParameter(entry.getKey(), entry.getValue());
				}
			}
		}
		
		return;		
		
	}
}