package ro.cst.tsearch.connection.http2;

import static ro.cst.tsearch.connection.http.HTTPRequest.GET;

import ro.cst.tsearch.connection.LoginResponse;
import ro.cst.tsearch.connection.LoginResponse.LoginStatus;
import ro.cst.tsearch.connection.http.HTTPRequest;

/**
 * 
 * this connection class it is used by COPitkinAO and CORouttTR. modify something here, test both sites.
 *
 */
public class COPitkinAO extends HttpSite {
	
	@Override
	public LoginResponse onLogin() {
		
		// get the search page
		HTTPRequest req1 = new HTTPRequest(getSiteLink(), GET);
		execute(req1);
		
		return new LoginResponse(LoginStatus.STATUS_SUCCESS, "Logged in");
	}
}
