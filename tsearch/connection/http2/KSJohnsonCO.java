package ro.cst.tsearch.connection.http2;

import org.apache.log4j.Logger;

import ro.cst.tsearch.connection.LoginResponse;
import ro.cst.tsearch.connection.LoginResponse.LoginStatus;
import ro.cst.tsearch.connection.http.HTTPRequest;
import ro.cst.tsearch.connection.http.HTTPResponse;
import ro.cst.tsearch.utils.StringUtils;

public class KSJohnsonCO extends HttpSite {

	protected static final Logger logger = Logger.getLogger(KSJohnsonCO.class);
	
	/**
	* login
	*/
	@Override
	public LoginResponse onLogin() {
	
		HTTPRequest req = new HTTPRequest(getSiteLink());
		HTTPResponse res = process(req);
		
		String pageResponse = res.getResponseAsString();
		String viewState = StringUtils.getTextBetweenDelimiters("\"__VIEWSTATE\" value=\"", "\"", pageResponse);
		
		if ("".equals(viewState)) {
			logger.error("view state not found in the search page");
			return new LoginResponse(LoginStatus.STATUS_INVALID_PAGE, "VIEWSTATE not found in the search page");
		}
		
		String eventValidation = StringUtils.getTextBetweenDelimiters("\"__EVENTVALIDATION\" value=\"", "\"", pageResponse);
		
		if ("".equals(eventValidation)) {
			logger.error("event validation not found in the search page");
			return new LoginResponse(LoginStatus.STATUS_INVALID_PAGE, "EVENTVALIDATION not found in the search page");
		}
		
		setAttribute("viewState", viewState);
		setAttribute("eventValidation", eventValidation);
		
		if (res.getReturnCode() == 200) {
			return new LoginResponse(LoginStatus.STATUS_SUCCESS, "Logged in");
		}
		
		return new LoginResponse(LoginStatus.STATUS_UNKNOWN, "Login failed");
	}
	
	@Override
	public void onBeforeRequestExcl(HTTPRequest req) {
		// add view state parameter if necessary, for search
		if (!req.hasPostParameter("__VIEWSTATE")) {
			req.setPostParameter("__VIEWSTATE",(String) getAttribute("viewState"));
		}
		
		// add event validation parameter if necessary, for search
		if (!req.hasPostParameter("__EVENTVALIDATION")) {
			req.setPostParameter("__EVENTVALIDATION",(String) getAttribute("eventValidation"));
		}
	}
	
	/**
	 * log a message, together with instance id and session id
	 * 
	 * @param message
	 */
	private void info(String message) {
		logger.info("search=" + searchId + " sid=" + sid + " :" + message);
	}

	@Override
	public void onRedirect(HTTPRequest req) {
		String location = req.getRedirectLocation();
		if (location.contains("?e=newSession") || location.contains("?e=sessionTerminated")) {
			setDestroy(true);
			throw new RuntimeException("Redirected to " + location+ ". Session needs to be destroyed");
		}
	}
}