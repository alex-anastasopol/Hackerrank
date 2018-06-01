package ro.cst.tsearch.connection.http2;

import java.util.HashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import ro.cst.tsearch.connection.LoginResponse;
import ro.cst.tsearch.connection.LoginResponse.LoginStatus;
import ro.cst.tsearch.connection.http.HTTPRequest;
import ro.cst.tsearch.connection.http.HTTPRequest.ParametersVector;
import ro.cst.tsearch.connection.http.HTTPResponse;

public class OHDelawareTR extends HttpSite {

	public LoginResponse onLogin() {
		try {
			HTTPRequest req = new HTTPRequest(dataSite.getLink());
			HTTPResponse resp = process(req);
			if (resp.getReturnCode() == 200)
			{
				Pattern p = Pattern
						.compile("(?is)welcome\\s+to\\s+the\\s+delaware\\s+county\\s+auditor");
				Matcher m = p.matcher(resp.getResponseAsString());
				if (m.find())
					return new LoginResponse(LoginStatus.STATUS_SUCCESS, "Logged in");
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		return new LoginResponse(LoginStatus.STATUS_UNKNOWN, "Login failed");
	}
	
	public void onBeforeRequestExcl(HTTPRequest req) {
		String url = req.getURL();
		if (req.getMethod() == HTTPRequest.GET && url.contains("ownerNameIntermediary")) {
			req.setURL(url.replaceFirst("(.*?)&ownerNameIntermediary.*", "$1"));
		}
		if (req.getMethod() == HTTPRequest.POST) {
			HashMap<String, ParametersVector> postParams = req.getPostParameters();
			if (req.hasPostParameter("owner")) {
				Object owner = postParams.get("owner");
				req.removePostParameters("owner");
				req.setPostParameter("owner", owner.toString().trim());
			}
			else {
				if (req.hasPostParameter("Street")) {
					Object streetName = postParams.get("Street");
					req.removePostParameters("Street");
					req.setPostParameter("Street", streetName.toString().trim());
				}
				if (req.hasPostParameter("HouseNoLow")) {
					Object streetNo = postParams.get("HouseNoLow");
					req.removePostParameters("HouseNoLow");
					req.setPostParameter("HouseNoLow", streetNo.toString().trim());
				}
			}
		}
	}
}
