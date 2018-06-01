package ro.cst.tsearch.connection.http2;

import static ro.cst.tsearch.connection.http.HTTPRequest.GET;
import static ro.cst.tsearch.connection.http.HTTPRequest.POST;

import java.util.HashMap;
import java.util.Map;

import org.apache.commons.httpclient.Cookie;
import org.apache.commons.httpclient.HttpState;
import org.apache.commons.lang.StringUtils;

import ro.cst.tsearch.connection.LoginResponse;
import ro.cst.tsearch.connection.http.HTTPRequest;
import ro.cst.tsearch.connection.http.HTTPResponse;
import ro.cst.tsearch.parser.SimpleHtmlParser;
import ro.cst.tsearch.parser.SimpleHtmlParser.Form;

/**
 * 
 * @author Oprina George
 * 
 *         Oct 25, 2011
 */

public class FLGenericCC extends HttpSite {
	public static String			ORIGINAL_LINK	= "http://www.floridaucc.com/UCCWEB/";

	private static final String[]	PARAM_NAMES		= { "__EVENTTARGET", "__EVENTARGUMENT", "__LASTFOCUS", "__VIEWSTATE", "__PREVIOUSPAGE",
													"__EVENTVALIDATION" };

	private Map<String, String>		req_params		= new HashMap<String, String>();

	public LoginResponse onLogin() {
		HttpState httpState = getHttpClient().getState();

		try {
			HTTPRequest req1 = new HTTPRequest(ORIGINAL_LINK + "search.aspx", GET);

			HTTPResponse resp1 = process(req1);

			Form form1 = new SimpleHtmlParser(resp1.getResponseAsString()).getForm("form1");

			if (form1 == null)
				return LoginResponse.getDefaultFailureResponse();

			HashMap<String, String> params = (HashMap<String, String>) form1.getParams();

			HTTPRequest req2 = new HTTPRequest(ORIGINAL_LINK + "SearchDisclaimer.aspx", POST);

			req2.setPostParameter("__EVENTTARGET", "AcceptCheckbox");
			req2.setPostParameter("__EVENTARGUMENT", "");
			req2.setPostParameter("__LASTFOCUS", "");
			req2.setPostParameter("__VIEWSTATE", StringUtils.defaultString(params.get("__VIEWSTATE")));
			req2.setPostParameter("__EVENTVALIDATION", StringUtils.defaultString(params.get("__EVENTVALIDATION")));
			req2.setPostParameter("AcceptCheckbox", "on");

			HTTPResponse resp2 = process(req2);

			form1 = new SimpleHtmlParser(resp2.getResponseAsString()).getForm("form1");

			if (form1 == null)
				return LoginResponse.getDefaultFailureResponse();

			params = (HashMap<String, String>) form1.getParams();

			HTTPRequest req3 = new HTTPRequest(ORIGINAL_LINK + "search.aspx", POST);

			req3.setPostParameter("__EVENTTARGET", "");
			req3.setPostParameter("__EVENTARGUMENT", "");
			req3.setPostParameter("__LASTFOCUS", "");
			req3.setPostParameter("__VIEWSTATE", StringUtils.defaultString(params.get("__VIEWSTATE")));
			req3.setPostParameter("__PREVIOUSPAGE", StringUtils.defaultString(params.get("__PREVIOUSPAGE")));
			req3.setPostParameter("__EVENTVALIDATION", StringUtils.defaultString(params.get("__EVENTVALIDATION")));
			req3.setPostParameter("AcceptCheckbox", "on");
			req3.setPostParameter("nextButton.x", "33");
			req3.setPostParameter("nextButton.y", "12");

			Cookie c1 = httpState.getCookies()[0];
			Cookie c2 = new Cookie(c1.getDomain(), "ucc_search_disclaimer", "YES");
			c2.setPath("/");
			c2.setPathAttributeSpecified(true);
			httpState.clearCookies();
			httpState.addCookies(new Cookie[] { c1, c2 });

			HTTPResponse resp3 = process(req3);

			if (!resp3.getResponseAsString().contains("Select Search Type:"))
				return LoginResponse.getDefaultFailureResponse();

			Form search = new SimpleHtmlParser(resp3.getResponseAsString()).getForm("search");
			if (search != null)
				req_params = search.getParams();
			form1 = new SimpleHtmlParser(resp3.getResponseAsString()).getForm("form1");

			if (form1 != null)
				req_params = form1.getParams();

		} catch (Exception e) {
			e.printStackTrace();
			return LoginResponse.getDefaultFailureResponse();
		}

		return LoginResponse.getDefaultSuccessResponse();
	}

	public void onBeforeRequestExcl(HTTPRequest req) {
	}

	public void onBeforeRequest(HTTPRequest req) {
		if (req.getMethod() == POST) {
			String url = req.getURL();

			if (url.equals(ORIGINAL_LINK + "search.aspx")) {
				String typeOfSearch = req.getPostFirstParameter("SearchOptionDropDownList");
				setTransientSearchAttribute("typeOfSearch", typeOfSearch);

				if (!req_params.isEmpty())
					for (String key : new String[] { "__EVENTTARGET", "__EVENTARGUMENT", "__VIEWSTATE", "__EVENTVALIDATION" })
						req.setPostParameter(key, StringUtils.defaultString(req_params.get(key)));
			} else if (url.contains("SearchResults.aspx") && (req.hasPostParameter("Next") || req.hasPostParameter("Prev"))) {
				String seq = req.getPostFirstParameter("seq");
				req.removePostParameters("seq");

				@SuppressWarnings("unchecked")
				Map<String, String> params = (Map<String, String>) getTransientSearchAttribute("params:" + seq);

				String[] url_param_names = new String[] { "sst", "sov", "sot", "st", "fn", "rn", "ii", "ft", "epn" };

				StringBuffer new_url_buf = new StringBuffer(url + "?");

				boolean next = req.hasPostParameter("Next");
				boolean prev = req.hasPostParameter("Prev");

				req.removePostParameters("Next");
				req.removePostParameters("Prev");

				for (String key : url_param_names) {
					if (key.equals("sot"))
						new_url_buf.append("&" + key + "=" + StringUtils.defaultString(req.getPostFirstParameter(key)).replace(" ", "%20"));
					else if (key.equals("st"))
						new_url_buf.append("&" + key + "="
								+ StringUtils.defaultString(req.getPostFirstParameter(key)).replace(" ", "+").replace(",", ""));
					else
						new_url_buf.append("&" + key + "=" + StringUtils.defaultString(req.getPostFirstParameter(key)));
					req.removePostParameters(key);
				}

				req.setURL(new_url_buf.toString().replace("?&", "?"));

				if (params != null) {
					for (String key : PARAM_NAMES)
						if (!key.equals("__LASTFOCUS"))
							req.setPostParameter(key, StringUtils.defaultString(params.get(key)));
				}

				if (next) {
					req.setPostParameter("ButtonNext.x", "22");
					req.setPostParameter("ButtonNext.y", "13");
				} else if (prev) {
					req.setPostParameter("ButtonPrevious.x", "79");
					req.setPostParameter("ButtonPrevious.y", "11");
				}
			}
		}
	}

	public void onAfterRequest(HTTPRequest req, HTTPResponse res) {
	}

	public byte[] getImage(String lnk) {
		try {
			if (lnk.split("\\?").length == 2) {
				HTTPRequest req = new HTTPRequest(lnk, GET);
				HTTPResponse res = process(req);
				return res.getResponseAsByte();
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		return null;
	}
}
