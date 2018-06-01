package ro.cst.tsearch.connection.http2;

import java.io.ByteArrayInputStream;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.apache.commons.httpclient.Cookie;
import org.apache.commons.httpclient.HttpState;
import org.apache.http.HttpStatus;

import ro.cst.tsearch.connection.LoginResponse;
import ro.cst.tsearch.connection.LoginResponse.LoginStatus;
import ro.cst.tsearch.connection.http.HTTPRequest;
import ro.cst.tsearch.connection.http.HTTPResponse;
import ro.cst.tsearch.data.SitesPasswords;

public class ARGenericCountyDataAOTR extends HttpSite {

	private static final String sitePasswordKey = "ARGenericCountyDataAOTR";
	private static final String LOGIN_PAGE = "/login.asp";
	private static final String LOGIN_ACTION = "/login.asp?from=";

	private boolean isPrivateLogin = true;

	public LoginResponse onLogin() {
		setAttribute(SINGLE_LINE_COOKIES_IN_REDIRECT, false);
		// make login
		if (isPrivateLogin()) {
			String userId = SitesPasswords.getInstance().getPasswordValue(getCurrentCommunityId(searchId), sitePasswordKey, "user");
			String password = SitesPasswords.getInstance().getPasswordValue(getCurrentCommunityId(searchId), sitePasswordKey, "password");
			String crtDomain = getCrtDomain();
			HTTPRequest loginPageReq = new HTTPRequest(crtDomain + LOGIN_PAGE);
			HTTPResponse loginPageResp = process(loginPageReq);

			HTTPRequest loginActionReq = new HTTPRequest(crtDomain + LOGIN_ACTION);
			loginActionReq.setMethod(HTTPRequest.POST);
			loginActionReq.setPostParameter("username", userId);
			loginActionReq.setPostParameter("password", password);
			loginActionReq.setPostParameter("loginbtn.x", "37");
			loginActionReq.setPostParameter("loginbtn.y", "10");

			HTTPResponse loginActionResp = process(loginActionReq);
			
			String link = getSiteLink();
			HTTPRequest req = new HTTPRequest(link);
			process(req);

		} else {
			String link = getSiteLink();
			HTTPRequest req = new HTTPRequest(link);
			process(req);
		}
		return new LoginResponse(LoginStatus.STATUS_SUCCESS, "Logged in");
	}

	public String cookie = "";

	private String getCrtDomain() {
		String link = getSiteLink();
		if (link.contains("?")) {
			int indexOf = link.lastIndexOf("/");
			link = link.substring(0, indexOf);
		}
		// link = StringUtils.replaceLast(link, "/", "");
		return link;
	}

	@Override
	public void onBeforeRequest(HTTPRequest req) {
		super.onBeforeRequest(req);
	}

	@Override
	public void onRedirect(HTTPRequest req) {
		HttpState httpState = getHttpClient().getState();
		Cookie[] cookies = httpState.getCookies();
		List<Cookie> cookieList = Arrays.asList(cookies);
		Cookie countyCookie = null;
		for (Cookie cookie : cookies) {
			if (cookie.getName().equals("CountyCookie")){
				countyCookie = cookie;
			}
		}
		if (countyCookie != null){
			int countyCookieIndex = cookieList.indexOf(countyCookie);
			
			Collections.swap(cookieList, 0, countyCookieIndex);
			httpState.clearCookies();
			httpState.addCookies(cookieList.toArray(new Cookie[]{}));
			
		}
	}
	
	@Override
	public void onAfterRequest(HTTPRequest req, HTTPResponse res) {
		String content = "";
		if (res != null) {
			if (res.getContentType().contains("text/html")) {
				content = res.getResponseAsString();
				res.is = new ByteArrayInputStream(content.getBytes());
				if (content.contains("Your Session Has Expired")|| res.getReturnCode() == HttpStatus.SC_MOVED_TEMPORARILY) {
					destroySession();
				}
			}
		}
	}

	public void setPrivateLogin(boolean isPrivateLogin) {
		this.isPrivateLogin = isPrivateLogin;
	}

	public boolean isPrivateLogin() {
		return isPrivateLogin;
	}

}
