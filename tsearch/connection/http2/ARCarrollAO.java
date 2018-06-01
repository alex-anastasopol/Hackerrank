package ro.cst.tsearch.connection.http2;

import static ro.cst.tsearch.connection.http.HTTPRequest.*;

import java.util.Map;

import ro.cst.tsearch.connection.LoginResponse;
import ro.cst.tsearch.connection.LoginResponse.LoginStatus;
import ro.cst.tsearch.connection.http.HTTPRequest;
import ro.cst.tsearch.connection.http.HTTPResponse;
import ro.cst.tsearch.parser.SimpleHtmlParser;
import ro.cst.tsearch.parser.SimpleHtmlParser.Form;

/**
 * 
 * @author Oprina George
 * 
 *         Mar 15, 2011
 */

public class ARCarrollAO extends HttpSite {

	private static final String[] ADD_PARAM_NAMES = { "__VIEWSTATE",
			"__EVENTVALIDATION" };
	private static final String[] REQ_PARAM_NAMES = { "__VIEWSTATE",
			"__EVENTVALIDATION" };
	private static final String FORM_NAME = "aspnetForm";

	private Map<String, String> params = null;
	private Map<String, String> first_params = null;

	@Override
	public LoginResponse onLogin() {
		try {
			HTTPRequest req1 = new HTTPRequest(
					"http://actdatascout.com/default.aspx?ci=2", GET);
			HTTPResponse res = process(req1);
			String response = res.getResponseAsString();
			if (response
					.contains("actDataScout, your source for Arkansas Assessor's Office real property records")) {
				Form form = new SimpleHtmlParser(response).getForm(FORM_NAME);
				params = form.getParams();
				first_params = params;
				if (!params.containsKey(REQ_PARAM_NAMES[0]))
					return new LoginResponse(LoginStatus.STATUS_INVALID_PAGE, "Did not find required parameter");
				// return true;
			} else
				return new LoginResponse(LoginStatus.STATUS_INVALID_PAGE, "Invalid page");
		} catch (Exception e) {
			e.printStackTrace();
		}
		return new LoginResponse(LoginStatus.STATUS_SUCCESS, "Logged in");
	}

	@Override
	public void onBeforeRequest(HTTPRequest req) {
		// get params for address & subdiv
		if (req.hasPostParameter("__EVENTTARGET")
				&& req.getPostFirstParameter("__EVENTTARGET").equals("")) {
			if (req.hasPostParameter("ctl00$MainContent$ctl00$SearchRadioButtonList"))
				if (req.getPostFirstParameter(
						"ctl00$MainContent$ctl00$SearchRadioButtonList")
						.equals("3")
						|| req.getPostFirstParameter(
								"ctl00$MainContent$ctl00$SearchRadioButtonList")
								.equals("4")) {
					HTTPRequest req2 = new HTTPRequest(
							"http://actdatascout.com/default.aspx?ci=2", POST);

					if (params != null) {
						for (String s : ADD_PARAM_NAMES)
							req2.setPostParameter(s, params.get(s));
						req2.setPostParameter("__LASTFOCUS", "");
						req2.setPostParameter("__EVENTARGUMENT", "");
						req2.setPostParameter(
								"ctl00$MainContent$ctl00$searchTextBox", "");
						if (req.getPostFirstParameter(
								"ctl00$MainContent$ctl00$SearchRadioButtonList")
								.equals("3")) {
							req2.setPostParameter("__EVENTTARGET",
									"ctl00$MainContent$ctl00$SearchRadioButtonList$3");
							req2.setPostParameter(
									"ctl00$MainContent$ctl00$SearchRadioButtonList",
									"3");
						} else {
							req2.setPostParameter("__EVENTTARGET",
									"ctl00$MainContent$ctl00$SearchRadioButtonList$4");
							req2.setPostParameter(
									"ctl00$MainContent$ctl00$SearchRadioButtonList",
									"4");
						}
					}
					HTTPResponse res2 = process(req2);
					String response2 = res2.getResponseAsString();
					Form form2 = new SimpleHtmlParser(response2)
							.getForm(FORM_NAME);
					params = form2.getParams();
				}
		}
		super.onBeforeRequest(req);
	}

	@SuppressWarnings("unchecked")
	@Override
	public void onBeforeRequestExcl(HTTPRequest req) {

		if (req.getMethod() == 1) {
			if (req.hasPostParameter("ci")) {
				req.removePostParameters("ci");
				String ci = "?ci=2";
				String url = req.getURL();
				url += ci;
				req.modifyURL(url);
			}

			if (req.hasPostParameter("seq")) {
				String seq = req.getPostFirstParameter("seq");
				req.removePostParameters("seq");

				Map<String, String> param = null;

				if (seq != null) {
					param = (Map<String, String>) getTransientSearchAttribute("params:"
							+ seq);
				}
				if (param != null) {
					for (String s : ADD_PARAM_NAMES) {
						if (param.containsKey(s))
							req.setPostParameter(s, param.get(s));
					}
				}
			} else if (req
					.hasPostParameter("ctl00$MainContent$ctl00$Button1.x")
					&& req.hasPostParameter("ctl00$MainContent$ctl00$Button1.y")
					&& req.hasPostParameter("ctl00$MainContent$ctl00$SearchRadioButtonList")) {
				if (params != null
						&& ((String) req
								.getPostFirstParameter(ADD_PARAM_NAMES[0]))
								.equals("")) {
					for (String s : ADD_PARAM_NAMES) {
						if (params.containsKey(s)) {
							req.removePostParameters(s);
							req.setPostParameter(s, params.get(s));
						}
					}
				}
			}
		}
	}

	@Override
	public void onAfterRequest(HTTPRequest req, HTTPResponse res) {
		// if request for 3/4 then go back
		if (req.hasPostParameter("ctl00$MainContent$ctl00$SearchRadioButtonList"))
			if (req.getPostFirstParameter(
					"ctl00$MainContent$ctl00$SearchRadioButtonList")
					.equals("3")
					|| req.getPostFirstParameter(
							"ctl00$MainContent$ctl00$SearchRadioButtonList")
							.equals("4")) {
				params = first_params;
			}
	}

}
