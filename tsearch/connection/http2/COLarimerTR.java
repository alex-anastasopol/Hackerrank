/**
 * 
 */
package ro.cst.tsearch.connection.http2;

import static ro.cst.tsearch.connection.http.HTTPRequest.GET;
import ro.cst.tsearch.connection.LoginResponse;
import ro.cst.tsearch.connection.LoginResponse.LoginStatus;
import ro.cst.tsearch.connection.http.HTTPRequest;

/**
 * @author vladb
 *
 */
public class COLarimerTR extends HttpSite {
	
	public COLarimerTR() {
		super();
		setAttribute(SINGLE_LINE_COOKIES_IN_REDIRECT, true);
	}
	
	@Override
	public LoginResponse onLogin() {
		
		// go to taxes search 
		HTTPRequest req = new HTTPRequest(getSiteLink() + "assessor/query/search.cfm", GET);
		execute(req);
		
		return new LoginResponse(LoginStatus.STATUS_SUCCESS, "Logged in");
	}
}
