/**
 * Connection class for VisualGov sites
 */
package ro.cst.tsearch.connection.http2;

import static ro.cst.tsearch.connection.http.HTTPRequest.GET;

import ro.cst.tsearch.connection.LoginResponse;
import ro.cst.tsearch.connection.LoginResponse.LoginStatus;
import ro.cst.tsearch.connection.http.HTTPRequest;

/**
 * for  Bay, DeSoto, Gulf, Hardee, Jackson, Okeechobee, Wakulla, Calhoun, Hardee - like sites   
 * ADD here the new county implemented with this Generic
 */
public class FLGenericVisualGovTR extends HttpSite {

	@Override
	public LoginResponse onLogin() {
		// go to taxes search 
		HTTPRequest req = new HTTPRequest(getSiteLink() + "/Property/SearchSelect?ClearData=True&Accept=true", GET);
		String resp = execute(req);
		if (resp.contains("Search Selection")) {
			return new LoginResponse(LoginStatus.STATUS_SUCCESS, "Logged in");
		}
		return new LoginResponse(LoginStatus.STATUS_INVALID_PAGE, "Invalid response");
	}
	
	@Override
	public void onBeforeRequestExcl(HTTPRequest req) {
		super.onBeforeRequest(req);
		String address = getSiteLink().replace("http://", "");
		req.modifyURL(req.getURL().replaceAll("generic.visualgov.address", address));
	}
}
