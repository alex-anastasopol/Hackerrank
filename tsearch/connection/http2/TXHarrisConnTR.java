package ro.cst.tsearch.connection.http2;

import org.apache.commons.lang.StringUtils;

import ro.cst.tsearch.connection.LoginResponse;
import ro.cst.tsearch.connection.LoginResponse.LoginStatus;
import ro.cst.tsearch.connection.http.HTTPRequest;
import ro.cst.tsearch.connection.http.HTTPResponse;

/**
 * @author mihaib
 */

public class TXHarrisConnTR extends HttpSite {

	public LoginResponse onLogin() {

		HTTPRequest request = new HTTPRequest(getSiteLink());
		String response = process(request).getResponseAsString();

		if (StringUtils.containsIgnoreCase(response, "Tax Statement Search and Payments")) {
			return new LoginResponse(LoginStatus.STATUS_SUCCESS, "Logged in");
		}

		return new LoginResponse(LoginStatus.STATUS_UNKNOWN, "");
	}

	@SuppressWarnings("unchecked")
	@Override
	public void onBeforeRequestExcl(HTTPRequest req) {
		String correctLink = req.getURL();
		if (correctLink.indexOf("SearchClass=del") > 0) {
			correctLink = correctLink.replaceFirst("CurrentResults", "DelResults");
		}
		req.setURL(correctLink);
	}
}