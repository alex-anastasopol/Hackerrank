package ro.cst.tsearch.connection.http3;

import ro.cst.tsearch.connection.LoginResponse;
import ro.cst.tsearch.connection.LoginResponse.LoginStatus;
import ro.cst.tsearch.connection.http.HTTPRequest;
import ro.cst.tsearch.connection.http.HTTPResponse;

public class CAMarinTR extends HttpSite3 {
	@Override
	public LoginResponse onLogin() {

		// access site and get parameters
		String link = getSiteLink();
		String responseAsString = "";
		HTTPRequest req = new HTTPRequest(link, HTTPRequest.GET);
		HTTPResponse res = process(req);
		responseAsString = res.getResponseAsString();

		if (responseAsString.contains("County Of Marin - Property Tax Bill")) {
			return new LoginResponse(LoginStatus.STATUS_SUCCESS, "Logged in");
		}
		return new LoginResponse(LoginStatus.STATUS_INVALID_PAGE, "Invalid parameters");
	}
}
