package ro.cst.tsearch.connection.http2;

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import ro.cst.tsearch.connection.LoginResponse;
import ro.cst.tsearch.connection.http.HTTPRequest;
import ro.cst.tsearch.connection.http.HTTPResponse;
import ro.cst.tsearch.parser.SimpleHtmlParser;
import ro.cst.tsearch.parser.SimpleHtmlParser.Form;

/**
 * 
 * @author Oprina George
 * 
 *         May 21, 2012
 */

public class COElPasoTR extends HttpSite {

	private static final String[]		ADD_PARAM_NAMES	= { "__VIEWSTATE", "__EVENTTARGET", "__EVENTARGUMENT", "__LASTFOCUS", "__EVENTVALIDATION" };

	private static Map<String, String>	parameters		= new HashMap<String, String>();

	public static String				SERVER_LINK		= "http://epmt.trs.elpasoco.com/epui/";

	@Override
	public LoginResponse onLogin() {

		try {
			HTTPRequest reqGET = new HTTPRequest(SERVER_LINK + "Search.aspx");

			String respGET = execute(reqGET);

			if (respGET.contains("Real Estate Property Tax Search") && respGET.contains("Search by Location and/or Name:")
					&& respGET.contains("Search by Schedule Number:")) {
				parameters = isolateParams(respGET, "form1");
				return LoginResponse.getDefaultSuccessResponse();
			}

		} catch (Exception e) {
			e.printStackTrace();
		}

		return LoginResponse.getDefaultFailureResponse();
	}

	@Override
	public void onBeforeRequestExcl(HTTPRequest req) {
		super.onBeforeRequestExcl(req);
	}

	@SuppressWarnings("unchecked")
	@Override
	public void onBeforeRequest(HTTPRequest req) {
		try {
			if (req.getMethod() == HTTPRequest.POST) {
				Map<String, String> params = new HashMap<String, String>();

				if (req.hasPostParameter("seq")) {
					params = (Map<String, String>) getTransientSearchAttribute("params:" + req.getPostFirstParameter("seq"));
				} else {
					params = parameters;
				}
				req.removePostParameters("seq");
				addParams(params, req, ADD_PARAM_NAMES);
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		super.onBeforeRequest(req);
	}

	@Override
	public void onAfterRequest(HTTPRequest req, HTTPResponse res) {
		super.onAfterRequest(req, res);
	}

	public static Map<String, String> isolateParams(String page, String formName) {
		try {
			//page = page.replaceFirst("(<form[^>]*)id=([^>]*>)", "$1name=$2");

			Form f = new SimpleHtmlParser(page).getForm(formName);

			if (f != null) {
				Map<String, String> params = f.getParams();
				return params;
			}
		} catch (Exception e) {
			e.printStackTrace();
		}

		return new HashMap<String, String>();
	}

	public static HTTPRequest addParams(Map<String, String> addParams, HTTPRequest request, String[] paramNames) {
		if (addParams == null)
			return request;

		if (paramNames == null) {
			for (Entry<String, String> e : addParams.entrySet()) {
				request.setPostParameter(e.getKey(), e.getValue());
			}
		} else {
			for (String s : paramNames) {
				if (addParams.containsKey(s))
					request.setPostParameter(s, addParams.get(s));
			}
		}
		return request;
	}

}
