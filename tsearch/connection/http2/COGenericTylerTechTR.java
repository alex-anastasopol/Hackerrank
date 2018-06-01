package ro.cst.tsearch.connection.http2;

import java.util.Map;
import java.util.Map.Entry;

import ro.cst.tsearch.connection.LoginResponse;
import ro.cst.tsearch.connection.LoginResponse.LoginStatus;
import ro.cst.tsearch.connection.http.HTTPRequest;
import ro.cst.tsearch.connection.http.HTTPResponse;
import ro.cst.tsearch.utils.RegExUtils;

/**
 * 
 * 
 * used by COGarfieldTR, COMesaTR, COPitkinTR, CORouttTR, COSanMiguelTR, COWeldTR
 *
 */

public class COGenericTylerTechTR extends SimplestSite {

	public COGenericTylerTechTR() {
		super();
	}

	protected String getCrtServerLink() {
		return getSiteLink();
	}

	public String getLoginLink() {
		return "/treasurer/web/loginPOST.jsp?submit=Login&guest=true";
	}

	@Override
	public LoginResponse onLogin() {
	
		String link = getCrtServerLink() + getLoginLink();
	
		HTTPRequest loginRequest = new HTTPRequest(link, HTTPRequest.GET);
		loginRequest.setHeader("User-Agent", getUserAgentValue());
		String page = execute(loginRequest);
		//List<String> matches = RegExUtils.getMatches("Search for an Account", page, 0);
	
		if (RegExUtils.matches("Search for an Account", page)) {
			return new LoginResponse(LoginStatus.STATUS_SUCCESS, "Logged in");
		}
	
		return new LoginResponse(LoginStatus.STATUS_INVALID_PAGE, "Invalid response");
	}

	@Override
	public void onRedirect(HTTPRequest req) {
		if(req.getRedirectLocation().contains("login.jsp")) {
			destroySession();
			throw new RuntimeException("Session needs to be destroyed");
		}
	}

	public String getPage(String page) {
		String link = getCrtServerLink()+page;
		HTTPRequest req = new HTTPRequest(link);
		return execute(req);
	}

	public String getPostPage(String page, Map<String,String> params) {
		String link = getCrtServerLink().replace("Default.aspx?mode=new", page);
		HTTPRequest req = new HTTPRequest(link,HTTPRequest.POST);
		for (Entry<String,String> param : params.entrySet()) {
			req.removePostParameters(param.getKey());
			req.setPostParameter(param.getKey(),param.getValue());
		}
		return execute(req);
	}

	@Override
	public void onAfterRequest(HTTPRequest req, HTTPResponse res) {
		if (res == null) {
			return;
		}		
		if(status != STATUS_LOGGING_IN && (res == null || res.returnCode == 0)){
				destroySession();    		
		}
	}
	
	public String getAssessorPage(String accountId) {
		
		return "";
	}

}