package ro.cst.tsearch.connection.http3;

import java.io.IOException;
import java.util.List;

import org.apache.http.cookie.Cookie;

import ro.cst.tsearch.connection.LoginResponse;
import ro.cst.tsearch.connection.LoginResponse.LoginStatus;
import ro.cst.tsearch.connection.http.HTTPRequest;
import ro.cst.tsearch.connection.http.HTTPResponse;
import ro.cst.tsearch.utils.RegExUtils;

public class OHLickingTR extends HttpSite3 {

	private String				JSESSIONID			= "";
	public static final String	VIEWSTATE			= "__VIEWSTATE";
	public static final String	VIEWSTATEENCRYPTED	= "__VIEWSTATEENCRYPTED";
	public static final String	EVENTTARGET			= "__EVENTTARGET";
	public static final String	EVENTARGUMENT		= "__EVENTARGUMENT";

	@Override
	public LoginResponse onLogin() {
		HTTPResponse response;
		HTTPRequest request = new HTTPRequest(getSiteLink(), HTTPRequest.GET);
		try {
			response = exec(request);
		} catch (IOException e) {
			return new LoginResponse(LoginStatus.STATUS_INVALID_PAGE, "Login Page not found");
		}

		if (response.getResponseAsString().contains("OnTrac - Search")) {
			List<Cookie> cookies = getCookies();
			if (cookies != null && cookies.size() > 0) {
				for (Cookie cookie : cookies) {
					if ("JSESSIONID".equals(cookie.getName())) {
						JSESSIONID = cookie.getValue();
						break;
					}
				}
			}
			return new LoginResponse(LoginStatus.STATUS_SUCCESS, "Logged in");
		}
		return new LoginResponse(LoginStatus.STATUS_INVALID_PAGE, "Login Page not found");
	}

	public static String getParamValue(String id, String text) {
		return RegExUtils.getFirstMatch("(?is)<input[^>]+id=\"" + id + "\"[^>]+=\"([^\"]*)\"[^>]/>", text, 1);
	}

	@SuppressWarnings("unchecked")
	@Override
	public void onBeforeRequestExcl(HTTPRequest req) {
		
		if (req.getMethod() == HTTPRequest.POST) {
			HTTPRequest reqBefore = new HTTPRequest(req.getURL(), HTTPRequest.GET);
			HTTPResponse respBefore = process(reqBefore);
			String respString = respBefore.getResponseAsString();

			String viewState = getParamValue(VIEWSTATE, respString);
			String viewStateEncrypted = getParamValue(VIEWSTATEENCRYPTED, respString);
			String eventTarget = getParamValue(EVENTTARGET, respString);
			String eventArgument = getParamValue(EVENTARGUMENT, respString);

			req.setPostParameter(VIEWSTATE, viewState);
			req.setPostParameter(VIEWSTATEENCRYPTED, viewStateEncrypted);
			req.setPostParameter(EVENTTARGET, eventTarget);
			req.setPostParameter(EVENTARGUMENT, eventArgument);
		}
	}
}