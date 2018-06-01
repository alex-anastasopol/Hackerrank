package ro.cst.tsearch.connection.http2;

import java.io.ByteArrayInputStream;

import ro.cst.tsearch.connection.LoginResponse;
import ro.cst.tsearch.connection.LoginResponse.LoginStatus;
import ro.cst.tsearch.connection.http.HTTPRequest;
import ro.cst.tsearch.connection.http.HTTPResponse;

public class FLOrangeTR extends HttpSite {
	
	public LoginResponse onLogin( ) {
		HTTPRequest req = new HTTPRequest(dataSite.getLink());
        req.setMethod( HTTPRequest.GET );
		String response = execute(req);
		if (response.contains("Property Tax Bill Search and/or Payment") || response.contains("Property Tax Search")) {
			return new LoginResponse(LoginStatus.STATUS_SUCCESS, "Logged in");
		}
		return new LoginResponse(LoginStatus.STATUS_INVALID_PAGE, "Invalid response");
	}

	public void onBeforeRequestExcl(HTTPRequest req) {
		String url = req.getURL();
		String[] params = url.replaceFirst(".*[?](.*)", "$1").split("&");
		
		if(params.length == 2) {
			if(params[0].startsWith("SearchType") && params[1].startsWith("Criteria")) {
				// change parameter order
				req.setURL(url.replaceFirst("[?].*", "?" + params[1] + "&" + params[0]));
			}
		}
	}
	
	@Override
	public void onAfterRequest(HTTPRequest req, HTTPResponse res) {
		if (res.getReturnCode() == 404 && res.getResponseAsString().contains("We're sorry but we could not find the page you are looking for")) {
			res.returnCode = 200;
			String content = res.getResponseAsString();
			res.is = new ByteArrayInputStream(content.getBytes());
		}
	}
}

