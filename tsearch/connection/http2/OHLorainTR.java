
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
public class OHLorainTR extends HttpSite {

	@Override
	public LoginResponse onLogin() {
		// get username and password from database
		String userName = SitesPasswords.getInstance().getPasswordValue(getCurrentCommunityId(), getDataSite().getName(), "user");
		String password = SitesPasswords.getInstance().getPasswordValue(getCurrentCommunityId(), getDataSite().getName(), "password");
		
		if(StringUtils.isEmpty(userName) || StringUtils.isEmpty(password)) {
			return new LoginResponse(LoginStatus.STATUS_INVALID_CREDENTIALS, "Couldn't find user / password in database");
		}
		
		HTTPRequest request = new HTTPRequest(getSiteLink().replace("register", "cgi/real-estate-access.cgi"), HTTPRequest.POST);
		request.setPostParameter("username", userName);
		request.setPostParameter("password", password);
		request.setPostParameter("s", "");
		request.setPostParameter("login", "login");
		
		String page = execute(request);		
		if(page.contains("The username and password combination entered do not match any in the database")) {
			logger.error("Invalid username/password");
			return LoginResponse.getDefaultInvalidCredentialsResponse();
		}
		
		return LoginResponse.getDefaultSuccessResponse();
	}
}
