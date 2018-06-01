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
public class COGunnisonAO extends HttpSite {

	@Override
	public LoginResponse onLogin() {

		// go to taxes search 
		HTTPRequest req = new HTTPRequest(getSiteLink(), GET);
		execute(req);
		
		return new LoginResponse(LoginStatus.STATUS_SUCCESS, "Logged in");
	}
	
	@Override
	public void onBeforeRequest(HTTPRequest req) {
		
		if (req.getMethod()==HTTPRequest.GET) {
			String url = req.getURL();
			if (url.contains("/co_gunnison_subdivision.php")) {
				url += "&";
				url = url.replaceAll("sub_code%5B%5D=&", "");
				url = url.replaceAll("condo_code%5B%5D=&", "");
				url = url.replaceFirst("&$", "");
				req.modifyURL(url);
			}
		}
	}	
	
}
