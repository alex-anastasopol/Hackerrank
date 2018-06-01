package ro.cst.tsearch.connection.http2;

import org.apache.commons.lang.StringUtils;

import ro.cst.tsearch.connection.LoginResponse;
import ro.cst.tsearch.connection.LoginResponse.LoginStatus;
import ro.cst.tsearch.connection.http.HTTPRequest;
import ro.cst.tsearch.connection.http.HTTPResponse;
import ro.cst.tsearch.utils.RegExUtils;


public class COFremontAO extends COGenericTylerTechTR {
	
	@Override
	public LoginResponse onLogin() {
		String link = getCrtServerLink();
		
		HTTPRequest loginRequest = new HTTPRequest(link, HTTPRequest.GET);
		loginRequest.setHeader("User-Agent", getUserAgentValue());
		String page = execute(loginRequest);
	
		if (RegExUtils.matches("Account Search", page)) {
			return new LoginResponse(LoginStatus.STATUS_SUCCESS, "Logged in");
		}
	
		return new LoginResponse(LoginStatus.STATUS_INVALID_PAGE, "Invalid response");
	}
	
	@Override
	public void onBeforeRequest(HTTPRequest req){
		// check if it's actually a post 
		if (req.getURL().contains("results.jsp")) {
			if (StringUtils.isEmpty(req.getPostFirstParameter("__search_select"))) {
				req.removePostParameters("__search_select");
			}
		}
	}
	
	
	@Override
	public void onRedirect(HTTPRequest req) {
		if(req.getRedirectLocation().contains("/web/../")) {
			String redirectLink = req.getRedirectLocation();
			redirectLink = redirectLink.replaceFirst("web/\\.\\./", "");
			req.setRedirectLocation(redirectLink);
			if (req.headers.get("Referer") != null) {
				if ( ((String)req.headers.get("Referer")).contains("web/../") ) {
					req.setReferer(((String)req.headers.get("Referer")).replaceFirst("web/\\.\\./", ""));
				}
			}
			
			req.getBypassResponse();
		}
	}
	
	@Override
	public void onAfterRequest(HTTPRequest req, HTTPResponse res) {
		if (res == null) {
			return;
		}		
		if(status != STATUS_LOGGING_IN && (res == null || res.returnCode == 0)){
				destroySession();    		
		}
		if (req.headers.get("Referer") != null) {
			if ( ((String)req.headers.get("Referer")).contains("web/../") ) {
				req.setReferer(((String)req.headers.get("Referer")).replaceFirst("web/\\.\\./", ""));
			}
		}
	}
	
}
