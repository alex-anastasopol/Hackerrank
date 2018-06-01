package ro.cst.tsearch.connection.http3;

import static ro.cst.tsearch.connection.http.HTTPRequest.GET;

import ro.cst.tsearch.connection.LoginResponse;
import ro.cst.tsearch.connection.LoginResponse.LoginStatus;
import ro.cst.tsearch.connection.http.HTTPRequest;

/**
 * @author Vladimir
 *
 */
public class VUWriter extends HttpSite3 {
	
	@Override
	public LoginResponse onLogin() {
		HTTPRequest req = new HTTPRequest(getSiteLink(), GET);
		execute(req);
		return new LoginResponse(LoginStatus.STATUS_SUCCESS, "Logged in");
	}
	
	@Override
	public void onBeforeRequestExcl(HTTPRequest req) {
		if (req.getMethod()==HTTPRequest.GET) {
			String url = req.getURL();
			if (url.contains("searchInput=")) {
				url += "&state=" + getState();
				req.modifyURL(url);
			}
		}
	}
	
}
