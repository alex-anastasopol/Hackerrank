
package ro.cst.tsearch.connection.http2;

import static ro.cst.tsearch.connection.http.HTTPRequest.GET;
import ro.cst.tsearch.connection.LoginResponse;
import ro.cst.tsearch.connection.LoginResponse.LoginStatus;
import ro.cst.tsearch.connection.http.HTTPRequest;

/**
 * @author vladb
 *
 */
public class NVGenericCountyTR extends HttpSite {

	@Override
	public LoginResponse onLogin() {
		// go to taxes search 
		HTTPRequest req = new HTTPRequest(getSiteLink(), GET);
		execute(req);
		return new LoginResponse(LoginStatus.STATUS_SUCCESS, "Logged in");
	}
	
	@Override
	public void onBeforeRequestExcl(HTTPRequest req) {
		
		if(req.hasPostParameter("srchparc")) { // remove dashes from parcel number
			String cleanValue = req.getPostFirstParameter("srchparc").replaceAll("-", "");
			req.removePostParameters("srchparc");
			req.setPostParameter("srchparc", cleanValue);
		}
	}
}
