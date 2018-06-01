package ro.cst.tsearch.connection.http2;

import ro.cst.tsearch.connection.LoginResponse;
import ro.cst.tsearch.connection.LoginResponse.LoginStatus;
import ro.cst.tsearch.connection.http.HTTPRequest;
import ro.cst.tsearch.data.SitesPasswords;
import ro.cst.tsearch.utils.StringUtils;

public class KSJohnsonRO extends HttpSite {

	private String login() {

		String passKey = getDataSite().getName();

		// Get username and password from DB.
		String user = SitesPasswords.getInstance().getPasswordValue(
				getCurrentCommunityId(), passKey, "user");
		String pass = SitesPasswords.getInstance().getPasswordValue(
				getCurrentCommunityId(), passKey, "password");

		if (StringUtils.isEmpty(user) && StringUtils.isEmpty(pass)) {
			user = SitesPasswords.getInstance()
					.getPasswordValue(getCurrentCommunityId(),
							getClass().getSimpleName(), "user");
			pass = SitesPasswords.getInstance().getPasswordValue(
					getCurrentCommunityId(), getClass().getSimpleName(),
					"password");
		}

		if (StringUtils.isEmpty(user) || StringUtils.isEmpty(pass)) {
			return null;
		}
		
		setAttribute(SINGLE_LINE_COOKIES_IN_REDIRECT,true);
		// Try to login.
		HTTPRequest req = new HTTPRequest(getSiteLink(), HTTPRequest.POST);
		req.setPostParameter("DTSUser", getPassword("user"));
		req.setPostParameter("DTSPassword", getPassword("password"));
		req.setPostParameter("Accepted",
				"I understand and accept the above statement");
		String res = execute(req);

		return res;
	}

	@Override
	public LoginResponse onLogin() {
		// Attempt login.
		String page = login();

		if (page == null) {
			return LoginResponse.getDefaultInvalidCredentialsResponse();
		}

		// Check login response.
		if (!page.contains("Search for public index information by name and date")
				&& !page.contains("Generate Reports")) {
			if (page.contains("The page cannot be found"))
				return new LoginResponse(LoginStatus.STATUS_INVALID_PAGE,
						"Invalid response. Please try again during the normal operating hours.");
			else
				return new LoginResponse(LoginStatus.STATUS_INVALID_PAGE,
						"Invalid response");
		}

		return new LoginResponse(LoginStatus.STATUS_SUCCESS, "Logged in");
	}

	@Override
	public void onBeforeRequestExcl(HTTPRequest req) {
		if(req.getURL().contains("LoadImage.asp")) {
			req.setHeader("Referer", ro.cst.tsearch.servers.types.KSJohnsonRO.LOAD_IMAGE_REFERER);
		}
		super.onBeforeRequestExcl(req);
	}

}
