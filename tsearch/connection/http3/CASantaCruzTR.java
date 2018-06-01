package ro.cst.tsearch.connection.http3;

import ro.cst.tsearch.connection.LoginResponse;
import ro.cst.tsearch.connection.LoginResponse.LoginStatus;
import ro.cst.tsearch.connection.http.HTTPRequest;
import ro.cst.tsearch.utils.StringUtils;

public class CASantaCruzTR extends HttpSite3 {

	@Override
	public LoginResponse onLogin() {
		try {
			HTTPRequest req = new HTTPRequest(getSiteLink(), HTTPRequest.GET);
			String responseAsString = execute(req);
			if (!StringUtils.extractParameter(responseAsString,
					"(To\\s+view\\s+and\\s*/\\s*or\\s+pay\\s+your\\s+Secured/Unsecured\\s+Property\\s+Tax\\s+Bills)").isEmpty()) {
				return new LoginResponse(LoginStatus.STATUS_SUCCESS, "Logged in");
			}
			return LoginResponse.getDefaultFailureResponse();
		} catch (Exception e) {
			e.printStackTrace();
			return LoginResponse.getDefaultFailureResponse();
		}
	}
}
