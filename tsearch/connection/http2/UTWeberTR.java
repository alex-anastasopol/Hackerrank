package ro.cst.tsearch.connection.http2;

import ro.cst.tsearch.connection.LoginResponse;
import ro.cst.tsearch.connection.LoginResponse.LoginStatus;
import ro.cst.tsearch.connection.http.HTTPRequest;
import ro.cst.tsearch.servers.bean.DataSite;

/**
 * 
 * @author george oprina
 * 
 */

public class UTWeberTR extends HttpSite {
	public static final String FORM_NAME = "form4";

	@Override
	public LoginResponse onLogin() {

		DataSite dataSite = getDataSite();

		HTTPRequest request = new HTTPRequest(dataSite.getLink());

		String responseAsString = execute(request);

		if (responseAsString != null) {
			try {
				return new LoginResponse(LoginStatus.STATUS_SUCCESS, "Logged in");
			} catch (Exception e) {
				logger.error("Problem Parsing Form on UTWeberTR", e);
			}
		}
		return new LoginResponse(LoginStatus.STATUS_UNKNOWN, "Login failed");
	}
}
