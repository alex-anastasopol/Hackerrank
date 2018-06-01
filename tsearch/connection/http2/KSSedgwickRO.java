package ro.cst.tsearch.connection.http2;

import static ro.cst.tsearch.connection.http.HTTPRequest.GET;
import static ro.cst.tsearch.connection.http.HTTPRequest.POST;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;

import org.apache.commons.httpclient.Cookie;
import org.apache.commons.httpclient.HttpState;
import org.apache.commons.httpclient.protocol.Protocol;
import org.apache.commons.io.IOUtils;
import org.htmlparser.Node;
import org.htmlparser.filters.HasAttributeFilter;
import org.htmlparser.filters.TagNameFilter;
import org.htmlparser.tags.InputTag;
import org.htmlparser.util.NodeList;

import ro.cst.tsearch.connection.LoginResponse;
import ro.cst.tsearch.connection.LoginResponse.LoginStatus;
import ro.cst.tsearch.connection.http.HTTPRequest;
import ro.cst.tsearch.connection.http.HTTPResponse;
import ro.cst.tsearch.connection.http.auth.ssl.EasySSLProtocolSocketFactory;
import ro.cst.tsearch.data.SitesPasswords;
import ro.cst.tsearch.parser.HtmlParser3;
import ro.cst.tsearch.utils.StringUtils;

/**
 * 
 * @author Oprina George
 * 
 *         Jan 13, 2012
 */

public class KSSedgwickRO extends HttpSite {

	public static String	SERVER_LINK		= "https://rod.sedgwickcounty.org/";

	String					first			= "";
	String					last			= "";
	String					address			= "";
	String					city			= "";
	String					postal_code		= "";
	String					driver_license	= "";
	String					state			= "";

	@Override
	public LoginResponse onLogin() {
		setAttribute(SINGLE_LINE_COOKIES_IN_REDIRECT, true);
		@SuppressWarnings("deprecation")
		Protocol easyhttps = new Protocol("https", new EasySSLProtocolSocketFactory(), 443);
		Protocol.registerProtocol("https", easyhttps);

		first = SitesPasswords.getInstance().getPasswordValue(getCurrentCommunityId(), "KSSedgwickRO", "first");
		last = SitesPasswords.getInstance().getPasswordValue(getCurrentCommunityId(), "KSSedgwickRO", "last");
		address = org.apache.commons.lang.StringUtils.defaultString(
				SitesPasswords.getInstance().getPasswordValue(getCurrentCommunityId(), "KSSedgwickRO", "address")).replace("_", " ");
		city = SitesPasswords.getInstance().getPasswordValue(getCurrentCommunityId(), "KSSedgwickRO", "city");
		postal_code = SitesPasswords.getInstance().getPasswordValue(getCurrentCommunityId(), "KSSedgwickRO", "postal_code");
		driver_license = SitesPasswords.getInstance().getPasswordValue(getCurrentCommunityId(), "KSSedgwickRO", "driver_license");
		state = SitesPasswords.getInstance().getPasswordValue(getCurrentCommunityId(), "KSSedgwickRO", "state");

		if (StringUtils.isEmpty(first) ||
				StringUtils.isEmpty(last) ||
				StringUtils.isEmpty(address) ||
				StringUtils.isEmpty(city) ||
				StringUtils.isEmpty(postal_code) ||
				StringUtils.isEmpty(driver_license) ||
				StringUtils.isEmpty(state)) {
			return new LoginResponse(LoginStatus.STATUS_INVALID_CREDENTIALS, "Couldn't get credentials from database");
		}

		return LoginResponse.getDefaultSuccessResponse();
	}

	private HTTPResponse getErrorResponse(String error) {
		HTTPResponse resp = new HTTPResponse();

		resp.body = "<html><head></head><body><div>" + error + "<div></body></html>";
		resp.contentLenght = resp.body.length();
		resp.contentType = "text/html;";
		resp.is = IOUtils.toInputStream(resp.body);
		resp.returnCode = 200;

		return resp;
	}

	@Override
	public void onBeforeRequest(HTTPRequest req) {
		String url = req.getURL();

		if (url.contains("FAKE")) {
			req.setURL(url.replace("FAKE", ""));
			return;
		}

		if (req.getMethod() == GET) {

			if (url.contains("viewimage.aspx")) {
				HTTPResponse resp = getImage(url);

				if (resp != null) {
					resp.contentType = "image/tiff";
					req.setBypassResponse(resp);
				} else {
					req.setBypassResponse(getErrorResponse("Error geting image!"));
				}
			}

		} else if (req.getMethod() == POST) {
			// login
			if (url.contains("Login")) {

				HTTPRequest reqLogin = new HTTPRequest(SERVER_LINK + "Default.aspx", POST);

				// Credentials cr = (Credentials) getTransientSearchAttribute("credentials");

				String captcha = req.getPostFirstParameter("txtCaptcha");

				req.removePostParameters("image");
				req.removePostParameters("txtCaptcha");

				if (StringUtils.isEmpty(captcha)) {
					req.setBypassResponse(getErrorResponse("No Credentials Error!"));
					return;
				}

				String view;// = getParam(respLoginGet.getResponseAsString(), "__VIEWSTATE");

				view = (String) getTransientSearchAttribute("viewState");

				reqLogin.setPostParameter("__VIEWSTATE", view);
				reqLogin.setPostParameter("txtFirstName", first);
				reqLogin.setPostParameter("txtLastName", last);
				reqLogin.setPostParameter("txtAddress", address);
				reqLogin.setPostParameter("txtCity", city);
				reqLogin.setPostParameter("drpState", state);
				reqLogin.setPostParameter("txtPostalCode", postal_code);
				reqLogin.setPostParameter("txtTelephone", "");
				reqLogin.setPostParameter("txtEmail", "");
				reqLogin.setPostParameter("txtIDNumber", driver_license);
				reqLogin.setPostParameter("drpStateID", state);
				reqLogin.setPostParameter("txtCaptcha", captcha);
				reqLogin.setPostParameter("btnAgree", "I Agree");

				// set parameters

				HTTPResponse respLogin = process(reqLogin);
				String respLoginString = respLogin.getResponseAsString();

				if (!respLoginString.contains("RMIS Instrument Search Criteria")) {
					req.setBypassResponse(getErrorResponse("Login Error!"));
					return;
				}

				// we are in, let's search

				String viewState = getParam(respLoginString, "__VIEWSTATE");

				HTTPRequest reqSearch = new HTTPRequest(url.replace("Login", ""), POST);

				reqSearch.setPostParameters(new HashMap<String, HTTPRequest.ParametersVector>(req.getPostParameters()));
				reqSearch.setPostParameter("__EVENTTARGET", "");
				reqSearch.setPostParameter("__VIEWSTATE", viewState);
				reqSearch.setPostParameter("__EVENTARGUMENT", "");
				reqSearch.setPostParameter("imgFindNow.x", "41");
				reqSearch.setPostParameter("imgFindNow.y", "9");

				HTTPResponse respSearch = process(reqSearch);
				respSearch.is = IOUtils.toInputStream(respSearch.getResponseAsString());

				req.setBypassResponse(respSearch);

			}
		}
	}

	@Override
	public void onRedirect(HTTPRequest req) {
		HttpState httpState = getHttpClient().getState();
		Cookie[] cookies = httpState.getCookies();
		List<Cookie> cookieList = Arrays.asList(cookies);
		Cookie countyCookie = null;
		for (Cookie cookie : cookies) {
			if (cookie.getName().equals("RMISSearch")) {
				countyCookie = cookie;
			}
		}
		if (countyCookie != null) {
			int countyCookieIndex = cookieList.indexOf(countyCookie);

			Collections.swap(cookieList, 0, countyCookieIndex);
			httpState.clearCookies();
			httpState.addCookies(cookieList.toArray(new Cookie[] {}));
		}
	}

	@Override
	public void onAfterRequest(HTTPRequest req, HTTPResponse res) {
	}

	public String getViewState() {
		return getParam(null, "__VIEWSTATE");
	}

	public String getParam(String resp, String param) {
		if (StringUtils.isEmpty(resp)) {
			HTTPRequest req = new HTTPRequest(SERVER_LINK + "Default.aspx", GET);
			resp = execute(req);
		}

		try {
			NodeList nodes = new HtmlParser3(resp).getNodeList();

			nodes = nodes.extractAllNodesThatMatch(new TagNameFilter("input"), true).extractAllNodesThatMatch(new HasAttributeFilter("name", param),
					true);

			if (nodes.size() > 0) {
				InputTag in = (InputTag) nodes.elementAt(0);
				return in.getAttribute("value");
			}

		} catch (Exception e) {
			e.printStackTrace();
		}

		return null;
	}

	public HTTPResponse getImage(String link) {
		try {
			if (link.contains("id")) {
				if (!link.contains(SERVER_LINK)) {
					link = SERVER_LINK + link;
				}

				if (!link.contains("?")) {
					link = link.replaceFirst("\\&", "?");
				}

				HTTPRequest reqGetImage = new HTTPRequest(link + "FAKE");

				HTTPResponse resGetImage = process(reqGetImage);

				// get Image Link

				NodeList nodes = new HtmlParser3(resGetImage.getResponseAsString()).getNodeList();

				NodeList mainTableList = nodes.extractAllNodesThatMatch(new TagNameFilter("script"), true);

				String imageLink = "";

				// find image link
				Node n = null;

				for (int i = 0; i < mainTableList.size(); i++) {
					if (mainTableList.elementAt(i).toHtml().contains("//Download the image file")) {
						n = mainTableList.elementAt(i);
						break;
					}
				}

				if (n == null) {
					return null;
				}

				String text = n.toPlainTextString().replaceAll("(?ism).*//Download the image file([^}]*)}.*", "$1").trim();

				if (text.contains("window.navigate")) {
					imageLink = text.replaceAll("(?ism)[^\\(]*\\(([^\\)]*)\\).*", "$1").trim().replace("\"", "");
				}

				if (StringUtils.isEmpty(imageLink) || !imageLink.matches("(?ism)linkfile\\.aspx\\?id=\\d+\\&.*\\.tif")) {
					return null;
				}

				String[] parts = imageLink.split("&link=");

				HTTPRequest reqImg = new HTTPRequest(SERVER_LINK + parts[0] + "&link="
						+ parts[1].replace("\\\\", "\\").replace("\\", "%5C").replace(":", "%3A"));

				HTTPResponse resImg = process(reqImg);

				if (resImg.returnCode != 200)
					return null;
				return resImg;
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		return null;
	}
}
