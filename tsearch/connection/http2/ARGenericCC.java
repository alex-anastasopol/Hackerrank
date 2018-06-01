/**
 * 
 */
package ro.cst.tsearch.connection.http2;

import ro.cst.tsearch.connection.LoginResponse;
import ro.cst.tsearch.connection.LoginResponse.LoginStatus;
import ro.cst.tsearch.connection.http.HTTPRequest;
import ro.cst.tsearch.data.SitesPasswords;
import ro.cst.tsearch.utils.StringUtils;

/**
 * @author vladb
 *
 */
public class ARGenericCC extends HttpSite {
	@Override
	public LoginResponse onLogin() {
		// get username and password from database
		String userName = SitesPasswords.getInstance().getPasswordValue(getCurrentCommunityId(), "ARGenericCC", "user");
		String password = SitesPasswords.getInstance().getPasswordValue(getCurrentCommunityId(), "ARGenericCC", "password");
		
		if(StringUtils.isEmpty(userName) || StringUtils.isEmpty(password)) {
			return new LoginResponse(LoginStatus.STATUS_INVALID_CREDENTIALS, "Couldn't find user / password in database");
		}
		
		HTTPRequest request = new HTTPRequest(getSiteLink() + "/index.php", HTTPRequest.POST);
		request.setPostParameter("username", userName);
		request.setPostParameter("password", password);
		request.setPostParameter("ac:loginUser:main_menu", "Log+In");
		request.setPostParameter("nextaction", "loginUser");
		request.setPostParameter("nextvalue", "main_menu");
		
		String page = execute(request);		
		if(page.contains("The username and password combination you entered is not correct")){
			logger.error("Invalid username/password");
			return LoginResponse.getDefaultInvalidCredentialsResponse();
		}
		
		return LoginResponse.getDefaultSuccessResponse();
	}
}
