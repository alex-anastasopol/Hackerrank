package ro.cst.tsearch.connection.http3;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import ro.cst.tsearch.connection.LoginResponse;
import ro.cst.tsearch.connection.LoginResponse.LoginStatus;
import ro.cst.tsearch.connection.http.HTTPRequest;
import ro.cst.tsearch.connection.http.HTTPResponse;
import ro.cst.tsearch.servers.bean.DataSite;

public class ILGenericCyberDriveCC extends HttpSite3 {
	@Override
	public LoginResponse onLogin() {
		DataSite dataSite = getDataSite();
		HTTPRequest request = new HTTPRequest(dataSite.getLink());
		HTTPResponse res = process(request);
		String responseString = res.getResponseAsString();

		if (res.getReturnCode() == 200) {
			Pattern pat = Pattern.compile("UCC\\s+Search");
			Matcher mat = pat.matcher(responseString);
			if (mat.find()) {
				return new LoginResponse(LoginStatus.STATUS_SUCCESS, "Logged in");
			}
		}
		return new LoginResponse(LoginStatus.STATUS_UNKNOWN, "Login failed");
	}
}
