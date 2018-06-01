package ro.cst.tsearch.connection.http2;

import static ro.cst.tsearch.connection.http.HTTPRequest.GET;
import static ro.cst.tsearch.connection.http.HTTPRequest.POST;

import java.util.Map;

import ro.cst.tsearch.connection.LoginResponse;
import ro.cst.tsearch.connection.LoginResponse.LoginStatus;
import ro.cst.tsearch.connection.http.HTTPRequest;
import ro.cst.tsearch.connection.http.HTTPResponse;
import ro.cst.tsearch.parser.SimpleHtmlParser;

/**
 * 
 * @author Oprina George
 * 
 *         May 16, 2011
 */

public class NVClarkAO extends HttpSite {

	private static final String[] PARAM_NAMES = { "__EVENTTARGET",
			"__EVENTARGUMENT", "__VIEWSTATE", "__EVENTVALIDATION" };

	private static final String FORM_NAME = "AssrInput";

	@Override
	public LoginResponse onLogin() {
		try {
			HTTPRequest req1 = new HTTPRequest("http://www.clarkcountynv.gov/depts/assessor/Pages/RecordSearch.aspx", GET);
			HTTPResponse res1 = process(req1);
			String response1 = res1.getResponseAsString();
			if (!response1.contains("Real Property Records"))
				return new LoginResponse(LoginStatus.STATUS_INVALID_PAGE, "Invalid page");
		} catch (Exception e) {
			e.printStackTrace();
		}
		return new LoginResponse(LoginStatus.STATUS_SUCCESS, "Logged in");
	}

	@SuppressWarnings("unchecked")
	@Override
	public void onBeforeRequest(HTTPRequest req) {
		String url = req.getURL();
		if (req.getMethod() == POST
				&& (url.equals("http://redrock.clarkcountynv.gov/AssrRealProp/pcl.aspx")
						|| url.equals("http://redrock.clarkcountynv.gov/AssrRealProp/ownr.aspx") 
						|| url.equals("http://redrock.clarkcountynv.gov/AssrRealProp/site.aspx"))) {
			HTTPRequest req1 = new HTTPRequest(url, GET);
			HTTPResponse res1 = process(req1);
			String response1 = res1.getResponseAsString();

			Map<String, String> params = new SimpleHtmlParser(response1).getForm(FORM_NAME).getParams();

			if (params != null) {
				for (String s : PARAM_NAMES) {
					if (params.containsKey(s))
						req.setPostParameter(s, params.get(s));
				}
			}
		} else if (req.getMethod() == POST
				&& (url.equals("http://redrock.clarkcountynv.gov/AssrRealProp/ownerList.aspx") 
						|| url.equals("http://redrock.clarkcountynv.gov/AssrRealProp/siteList.aspx"))) {

			String seq = req.getPostFirstParameter("seq");
			String page = req.getPostFirstParameter("page");
			String inst = req.getPostFirstParameter("inst");

			url += "?inst=" + inst;

			req.setURL(url);

			req.removePostParameters("seq");
			req.removePostParameters("page");
			req.removePostParameters("inst");

			req.setPostParameter(PARAM_NAMES[0], page);

			Map<String, String> params = (Map<String, String>) getTransientSearchAttribute("params:" + seq);

			if (params != null) {
				for (String s : PARAM_NAMES) {
					if (params.containsKey(s)
							&& !s.equals(PARAM_NAMES[0]))
						req.setPostParameter(s, params.get(s));
				}
			}
		}
	}

	@Override
	public void onAfterRequest(HTTPRequest req, HTTPResponse res) {
	}

}
