package ro.cst.tsearch.connection.http2;

import ro.cst.tsearch.connection.LoginResponse;
import ro.cst.tsearch.connection.LoginResponse.LoginStatus;
import ro.cst.tsearch.connection.http.HTTPRequest;
import ro.cst.tsearch.connection.http.HTTPResponse;
import ro.cst.tsearch.data.SitesPasswords;
/**
 * @author Mihai Dediu
  */

public class AKGenericMotznikRO extends HttpSite {

	public AKGenericMotznikRO() {
		super();
	}

	@Override
	public LoginResponse onLogin() {
		
		String link = getSiteLink() + "login";		
		
		try {
			HTTPRequest loginRequest = new HTTPRequest(link,HTTPRequest.POST);		
			
			loginRequest.setHeader("User-Agent", "Mozilla/4.0 (compatible; MSIE 7.0; Windows NT 5.1; .NET CLR 2.0.50727; .NET CLR 3.0.04506.648; .NET CLR 3.5.21022)");
			
			String user = SitesPasswords.getInstance().getPasswordValue(getCurrentCommunityId(), getClass().getSimpleName(), "user");
			String pass = SitesPasswords.getInstance().getPasswordValue(getCurrentCommunityId(), getClass().getSimpleName(), "password");
			
			loginRequest.setPostParameter("UserName",user);
			loginRequest.setPostParameter("UserPassword",pass);
			loginRequest.setPostParameter("ButtonLogIn.x","29");
			loginRequest.setPostParameter("ButtonLogIn.y","13");
			
			setAttribute(SINGLE_LINE_COOKIES_IN_REDIRECT,true);
			String page = execute(loginRequest);
			if(!page.contains("Logged in as")) {
				return new LoginResponse(LoginStatus.STATUS_INVALID_CREDENTIALS, "Invalid username or password");
			}
			return new LoginResponse(LoginStatus.STATUS_SUCCESS, "Logged in");
		}catch(Exception e) {
			e.printStackTrace();
		}
		
		return new LoginResponse(LoginStatus.STATUS_UNKNOWN, "Login failed");
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

}