
package ro.cst.tsearch.connection.http2;

import ro.cst.tsearch.connection.LoginResponse;
import ro.cst.tsearch.connection.LoginResponse.LoginStatus;
import ro.cst.tsearch.connection.http.HTTPRequest;
import ro.cst.tsearch.connection.http.HTTPResponse;
import ro.cst.tsearch.utils.RegExUtils;


/**
 * @author olivia
**/
public class COAdamsTR extends COGenericTylerTechTR {
	@Override
	public LoginResponse onLogin() {
		String link = getCrtServerLink() + getLoginLink();
		
		HTTPRequest loginRequest = new HTTPRequest(link, HTTPRequest.GET);
		loginRequest.setHeader("User-Agent", getUserAgentValue());
		String page = execute(loginRequest);
	
		if (RegExUtils.matches("Search for an Account", page)) {
			return new LoginResponse(LoginStatus.STATUS_SUCCESS, "Logged in");
		}
	
		return new LoginResponse(LoginStatus.STATUS_INVALID_PAGE, "Invalid response");
	}
	
	
	@Override
	public void onRedirect(HTTPRequest req) {
		if(req.getRedirectLocation().contains("/web/../") || req.getRedirectLocation().contains("/treasurerweb/../")) {
			String redirectLink = req.getRedirectLocation();
			redirectLink = redirectLink.replaceFirst("(treasurer)?web/\\.\\./", "");
			req.setRedirectLocation(redirectLink);
			if (req.headers.get("Referer") != null) {
				String ref = (String)req.headers.get("Referer");
				if (ref.contains("web/../") || ref.contains("treasurerweb/../")) {
					req.setReferer(ref.replaceFirst("(/treasurer)?web/\\.\\./", ""));
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
			String ref = (String)req.headers.get("Referer");
			if (ref.contains("web/../")) {
				req.setReferer(ref.replaceFirst("web/\\.\\./", ""));
			}
		}
	}
	
}