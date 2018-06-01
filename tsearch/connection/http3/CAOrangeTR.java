package ro.cst.tsearch.connection.http3;

import ro.cst.tsearch.connection.LoginResponse;
import ro.cst.tsearch.connection.LoginResponse.LoginStatus;
import ro.cst.tsearch.connection.http.HTTPRequest;
import ro.cst.tsearch.connection.http.HTTPResponse;
import ro.cst.tsearch.servers.bean.DataSite;
import java.util.regex.Pattern;
import java.util.regex.Matcher;

public class CAOrangeTR extends HttpSite3 {
	@Override
	public LoginResponse onLogin() {
		DataSite dataSite = getDataSite();
		HTTPRequest request = new HTTPRequest(dataSite.getLink());
		HTTPResponse res = process(request);
		if (res.getReturnCode() == 200)
		{
			Pattern p = Pattern
					.compile("(?is)Property\\s+Tax\\s+Bill\\s+key\\s+in\\s+ANY\\s+of\\s+the\\s+following,\\s+then\\s+click\\s+on\\s+the\\s+corresponding\\s+Find\\s+button\\s+below:");
			Matcher m = p.matcher(res.getResponseAsString());
			if (m.find())
				return new LoginResponse(LoginStatus.STATUS_SUCCESS, "Logged in");
		}
		return new LoginResponse(LoginStatus.STATUS_UNKNOWN, "Login failed");
	}

	@Override
	public void onBeforeRequestExcl(HTTPRequest req) {
		Pattern p1 = Pattern.compile("(?is)(.*?)&intermediaryAddress=");
		Matcher m1 = p1.matcher(req.getURL());
		if (m1.find())
		{
			req.setURL(m1.group(1));
		}
	}
}