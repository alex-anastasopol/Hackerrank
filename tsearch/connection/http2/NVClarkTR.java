package ro.cst.tsearch.connection.http2;

import ro.cst.tsearch.connection.LoginResponse;
import ro.cst.tsearch.connection.LoginResponse.LoginStatus;
import ro.cst.tsearch.connection.http.HTTPRequest;
import ro.cst.tsearch.servers.bean.DataSite;

/**
 * 
 * @author Oprina George
 * 
 *         Feb 25, 2011
 */

public class NVClarkTR extends HttpSite {
	public LoginResponse onLogin() {
		DataSite dataSite = getDataSite();

		HTTPRequest request = new HTTPRequest(dataSite.getLink());

		String responseAsString = execute(request);

		if (responseAsString != null)
			return new LoginResponse(LoginStatus.STATUS_SUCCESS, "Logged in");
		else
			return new LoginResponse(LoginStatus.STATUS_UNKNOWN, "Login failed");
	}

	@Override
	public void onBeforeRequest(HTTPRequest req) {
		if (req.hasPostParameter("City")) {
			String city = req.getPostFirstParameter("City");
			if (city.contains("VEGAS")) {
				city = city.replace("LASVEGAS", "LAS VEGAS");
				req.removePostParameters("City");
				req.setPostParameter("City", city);
			}
		}
		if (req.hasPostParameter("Last_Name"))
			if (!req.getPostFirstParameter("Last_Name").equals("")) {
				req.removePostParameters("Taxpayer");
				req.setPostParameter("Taxpayer", "Person");
			} else if (!req.getPostFirstParameter("Organization").equals("")) {
				req.removePostParameters("Taxpayer");
				req.setPostParameter("Taxpayer", "Org");
			}
	}
}
