package ro.cst.tsearch.connection.http2;

import ro.cst.tsearch.connection.LoginResponse;
import ro.cst.tsearch.connection.LoginResponse.LoginStatus;
import ro.cst.tsearch.connection.http.HTTPRequest;
import ro.cst.tsearch.data.SitesPasswords;
import ro.cst.tsearch.exceptions.LoginException;
import ro.cst.tsearch.utils.StringUtils;

public class GenericDR extends HttpSite {
	
	@Override
	public LoginResponse onLogin() throws LoginException {
		
		String serverAddress = getSiteLink();
		
		HTTPRequest req1 = new HTTPRequest(serverAddress, HTTPRequest.GET);
		String response = execute(req1);
		if (StringUtils.isEmpty(response)){
			return new LoginResponse(LoginStatus.STATUS_INVALID_PAGE, "Cannot reach the login page. It is possible that the site is down. Check Document server!");
		}
		
		//get user name and password from database
		String username = SitesPasswords.getInstance().getPasswordValue(getCurrentCommunityId(), "GenericNR", "username");
		String password = SitesPasswords.getInstance().getPasswordValue(getCurrentCommunityId(), "GenericNR", "password");
		
		if(StringUtils.isEmpty(username) || StringUtils.isEmpty(password)) {
			return new LoginResponse(LoginStatus.STATUS_INVALID_CREDENTIALS, "Couldn't find user / password in database");
		}
		
		HTTPRequest req2 = new HTTPRequest(serverAddress, HTTPRequest.POST);
		req2.setPostParameter("login_username", username);
		req2.setPostParameter("login_password", password);
		String resp = execute(req2);
		
		if(resp.contains("Account Login")) {
			logger.error("Invalid username/password");
			return LoginResponse.getDefaultInvalidCredentialsResponse();
		}
		
		return LoginResponse.getDefaultSuccessResponse();
	}
	
}
