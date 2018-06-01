package ro.cst.tsearch.connection.http2;

import static ro.cst.tsearch.connection.http.HTTPRequest.GET;
import static ro.cst.tsearch.connection.http.HTTPRequest.POST;
import ro.cst.tsearch.connection.LoginResponse;
import ro.cst.tsearch.connection.LoginResponse.LoginStatus;
import ro.cst.tsearch.connection.http.HTTPRequest;
import ro.cst.tsearch.connection.http.HTTPResponse;

/**
 * 
 * @author Oprina George
 * 
 *         Mar 30, 2011
 */

public class TNRutherfordYC extends HttpSite {

	@Override
	public LoginResponse onLogin() {
		try {
			HTTPRequest req0 = new HTTPRequest(
					"http://www.lavergnetn.gov/records/protax/index.shtml", GET);
			HTTPResponse res0 = process(req0);
			String response0 = res0.getResponseAsString();

			if (!response0.contains("Property Tax Records")) {
				return new LoginResponse(LoginStatus.STATUS_INVALID_PAGE,
						"Wrong response received for request on parrent site!");
			}

		} catch (Exception e) {
			e.printStackTrace();
		}
		return new LoginResponse(LoginStatus.STATUS_SUCCESS, "Success!");
	}

	@Override
	public void onBeforeRequestExcl(HTTPRequest req) {
	}

	@Override
	public void onBeforeRequest(HTTPRequest req) {
		if (req.getURL().contains(
				"http://www.lavergnetn.gov/records/protax/protaxname1.shtml")
				|| req.getURL()
						.contains(
								"http://www.lavergnetn.gov/records/protax/protaxadd1.shtml")) {
			if (req.getURL().contains("&") && req.getURL().contains("=")) {
				req.setMethod(POST);
				String EName = req.getURL().split("&")[1];
				req.setPostParameter(
						"EName",
						EName.substring(EName.indexOf("=") + 1).replace("%20",
								" "));
				req.setPostParameter("SUBMIT", "SUBMIT");
				req.setURL(req.getURL().substring(0, req.getURL().indexOf("&")));
			}
		}
		if(req.getURL().contains("protaxresults.shtml") && req.getMethod()==POST){
			req.setPostParameter("SUBMIT", "SUBMIT");
		}
	}
}
