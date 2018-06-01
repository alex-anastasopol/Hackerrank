package ro.cst.tsearch.connection.http2;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import ro.cst.tsearch.connection.LoginResponse;
import ro.cst.tsearch.connection.LoginResponse.LoginStatus;
import ro.cst.tsearch.connection.http.HTTPRequest;
import ro.cst.tsearch.utils.StringUtils;

public class FLGenericGovernmaxAO extends HttpSite {
	private String sessionId;

	public static String getSessionId(String response) {
		Matcher m = Pattern.compile("sid=([a-zA-Z0-9]+)").matcher(response);
		if (m.find()) {
			return m.group(1);
		}
		return "";
	}

	@Override
	public LoginResponse onLogin() {

		// Generate and retrieve a session ID.
		HTTPRequest request = new HTTPRequest(getSiteLink()
				+ "/agency/fl-martin-appraiser2/flmartin_home.asp");

		String response = execute(request);
		if (response != null)
			sessionId = getSessionId(response);

		if (StringUtils.isEmpty(sessionId))
			return new LoginResponse(LoginStatus.STATUS_INVALID_PAGE,
					"Invalid response");
		return new LoginResponse(LoginStatus.STATUS_SUCCESS, "Logged in");
	}

	@Override
	public void onBeforeRequest(HTTPRequest req) {

		if (req.getMethod()==HTTPRequest.POST) {
			String url = req.getURL() + "?go.x=1";
			req.modifyURL(url);
			req.setPostParameter("go", "  Go  ");
			req.setPostParameter("site", "home");
			req.setPostParameter("sid", sessionId);
			super.onBeforeRequest(req);
		} else {
			String url = req.getURL();
			req.modifyURL(url.replace("%2D", "-").replace("%", "%25").replace("|", "%7C").replace(" ", "+"));
		}
	}
}
