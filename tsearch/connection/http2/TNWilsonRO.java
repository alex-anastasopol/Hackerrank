package ro.cst.tsearch.connection.http2;


import ro.cst.tsearch.connection.LoginResponse;
import ro.cst.tsearch.connection.LoginResponse.LoginStatus;
import ro.cst.tsearch.connection.http.HTTPRequest;
import ro.cst.tsearch.connection.http.HTTPResponse;
import ro.cst.tsearch.data.SitesPasswords;
import ro.cst.tsearch.utils.StringUtils;

public class TNWilsonRO extends HttpSite {
	
	public LoginResponse onLogin() {
		
		//go to main page
		HTTPRequest req = new HTTPRequest("http://www.wilsondeeds.com/");
        HTTPResponse res = process(req);
        String siteStringResponse = res.getResponseAsString();
        
        if (!siteStringResponse.toLowerCase().contains("password")){
        	return new LoginResponse(LoginStatus.STATUS_INVALID_PAGE, "Log-in page not found");
        }
        //log in
        //POSTDATA=username=cstwilson5&password=CSTW%21LSON&login=1
        req = new HTTPRequest("https://secure.wilsondeeds.com/");
		req.setMethod( HTTPRequest.POST );
		req.setPostParameter("username" , SitesPasswords.getInstance().getPasswordValue(getCurrentCommunityId(), getClass().getSimpleName(), "user"));
		req.setPostParameter("password" , SitesPasswords.getInstance().getPasswordValue(getCurrentCommunityId(), getClass().getSimpleName(), "password"));
		req.setPostParameter("login" , "1");
        res = process(req);
        siteStringResponse = res.getResponseAsString();
        
		//get location redirect
        String newLocation = StringUtils.getTextBetweenDelimiters("window.location='", "'", siteStringResponse);
        
        if (org.apache.commons.lang.StringUtils.isEmpty(newLocation)){
        	return new LoginResponse(LoginStatus.STATUS_INVALID_CREDENTIALS, "Log-in failed");
        }
        
        req = new HTTPRequest(newLocation);
        res = process(req);
        siteStringResponse = res.getResponseAsString();
        
        return new LoginResponse(LoginStatus.STATUS_SUCCESS, "Logged in");
	}
	
	public void onBeforeRequestExcl(HTTPRequest req) {
		req.setHeader("Pragma","no-cache");
	    req.setHeader("Cache-Control","no-cache");
	}
	
}
