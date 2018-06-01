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
public class TNGenericEgovTR extends HttpSite {
	
	@Override
	public LoginResponse onLogin() {

		// go to taxes search 
		HTTPRequest req = new HTTPRequest(getSiteLink(), GET);
		execute(req);
		
		return new LoginResponse(LoginStatus.STATUS_SUCCESS, "Logged in");
	}
	
	@Override
	public void onBeforeRequestExcl(HTTPRequest req) {
		
		String oldUrl = req.getURL();
		if(oldUrl.trim().matches(".*/mod.php")) {
			String newUrl = oldUrl + "?mod=propertytax&mode=public_lookup&action=";
			req.setURL(newUrl);
		}
	}

}
