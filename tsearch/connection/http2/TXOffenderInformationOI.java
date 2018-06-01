package ro.cst.tsearch.connection.http2;

import ro.cst.tsearch.connection.LoginResponse;
import ro.cst.tsearch.connection.http.HTTPRequest;
import ro.cst.tsearch.connection.http.HTTPResponse;

/**
 * 
 * @author Oprina George
 * 
 *         Apr 28, 2011
 */
public class TXOffenderInformationOI extends HttpSite {

	@Override
	public LoginResponse onLogin() {

		HTTPRequest request = new HTTPRequest(getSiteLink(), HTTPRequest.GET);
		String resp = execute(request).toLowerCase();
		if (resp.contains("texas department of criminal justice")
				&& resp.contains("offender information search"))
			return LoginResponse.getDefaultSuccessResponse();
		else
			return LoginResponse.getDefaultFailureResponse();
	}

	@Override
	public void onBeforeRequest(HTTPRequest req) {
		if (req.getMethod() == HTTPRequest.GET) {
			// clean link
			String url = req.getURL().replace("searchHints=Search+Hints", "")
					.replace("&&", "&").replace("?&", "?");
			req.setURL(url);
		}
	}

	@Override
	public void onAfterRequest(HTTPRequest req, HTTPResponse resp) {
	}
}
