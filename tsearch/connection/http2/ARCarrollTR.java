package ro.cst.tsearch.connection.http2;

import static ro.cst.tsearch.connection.http.HTTPRequest.*;
import ro.cst.tsearch.connection.LoginResponse;
import ro.cst.tsearch.connection.LoginResponse.LoginStatus;
import ro.cst.tsearch.connection.http.HTTPRequest;
import ro.cst.tsearch.connection.http.HTTPResponse;

/**
 * 
 * @author Oprina George
 * 
 *         Mar 15, 2011
 */

public class ARCarrollTR extends HttpSite {

	@Override
	public LoginResponse onLogin() {
		try {
			HTTPRequest req0 = new HTTPRequest(
					"http://www.arcountydata.com/county.asp?county=Carroll", GET);
			HTTPResponse res0= process(req0);
			String response0 = res0.getResponseAsString();
			
			if (!response0.contains("Carroll County, AR")) {
				return new LoginResponse(LoginStatus.STATUS_INVALID_PAGE,"Wrong response received for request on parrent site!");
			}
			
			HTTPRequest req1 = new HTTPRequest(
					"http://www.arcountydata.com/propsearch.asp", GET);
			HTTPResponse res1 = process(req1);
			String response1 = res1.getResponseAsString();
			if (response1.contains("Tax Collector Record Search")) {
				return new LoginResponse(LoginStatus.STATUS_SUCCESS,"Success!");
			} else
				return new LoginResponse(LoginStatus.STATUS_INVALID_PAGE,"Wrong response received for request in parrent site!");
		} catch (Exception e) {
			e.printStackTrace();
		}
		return new LoginResponse(LoginStatus.STATUS_SUCCESS,"Success!");
	}

	@Override
	public void onBeforeRequest(HTTPRequest req) {
		if (req.hasPostParameter("taxname") && req.getMethod() == POST
				&& !req.getURL().contains("Page")) {
			req.setPostParameter("size", "100");
			String url = req.getURL()+"?Page=1";		
			req.modifyURL(url);
		}
		super.onBeforeRequest(req);
	}	
	
}
