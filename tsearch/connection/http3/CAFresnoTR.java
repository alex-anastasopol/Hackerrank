package ro.cst.tsearch.connection.http3;

import ro.cst.tsearch.connection.LoginResponse;
import ro.cst.tsearch.connection.LoginResponse.LoginStatus;
import ro.cst.tsearch.connection.http.HTTPRequest;
import ro.cst.tsearch.connection.http.HTTPRequest.ParametersVector;
import ro.cst.tsearch.connection.http.HTTPResponse;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.apache.commons.lang.StringUtils;

import ro.cst.tsearch.parser.SimpleHtmlParser;

public class CAFresnoTR extends HttpSite3 {

	private static final String[]	ADD_PARAM_NAMES	= { "__VIEWSTATE", "__EVENTVALIDATION", };
	private static final String[]	REQ_PARAM_NAMES	= { "__VIEWSTATE", "__EVENTVALIDATION" };
	private static final String		FORM_NAME		= "Form1";

	@Override
	public LoginResponse onLogin() {

		// access site and get parameters
		String link = getSiteLink();
		String responseAsString = "";
		HTTPRequest req = new HTTPRequest(link, HTTPRequest.GET);
		HTTPResponse res = process(req);
		responseAsString = res.getResponseAsString();
		Map<String, String> addParams = isolateParams(responseAsString, FORM_NAME);
		addParams(addParams, req);

		// get search page and get parameters again
		req.setMethod(HTTPRequest.POST);
		req.setPostParameter("StartSearch", "Start Search");
		res = process(req);
		responseAsString = res.getResponseAsString();
		addParams = isolateParams(responseAsString, FORM_NAME);

		// check parameters
		if (!checkParams(addParams)) {
			return new LoginResponse(LoginStatus.STATUS_INVALID_PAGE, "Invalid parameters");
		}

		for (int i = 0; i < addParams.size(); i++) {
			setAttribute(REQ_PARAM_NAMES[i], addParams.get(REQ_PARAM_NAMES[i]));
		}

		return new LoginResponse(LoginStatus.STATUS_SUCCESS, "Logged in");
	}

	public static Map<String, String> isolateParams(String response, String form) {
		Map<String, String> params = new SimpleHtmlParser(response).getForm(form).getParams();
		Map<String, String> addParams = new HashMap<String, String>();
		for (String key : ADD_PARAM_NAMES) {
			String value = "";
			if (params.containsKey(key)) {
				value = params.get(key);
			}
			addParams.put(key, value);
		}
		return addParams;
	}

	public static boolean checkParams(Map<String, String> addParams) {
		for (String key : REQ_PARAM_NAMES) {
			if (StringUtils.isEmpty(addParams.get(key))) {
				return false;
			}
		}
		return true;
	}

	public static HTTPRequest addParams(Map<String, String> addParams, HTTPRequest request) {
		Iterator<String> i = addParams.keySet().iterator();
		while (i.hasNext()) {
			String k = i.next();
			request.setPostParameter(k, addParams.get(k));
		}
		return request;
	}

	@Override
	public void onBeforeRequestExcl(HTTPRequest req) {
		if (req.getMethod() == HTTPRequest.POST) {

			Map<String, String> addParams = new HashMap<String, String>();
			for (int i = 0; i < REQ_PARAM_NAMES.length; i++) {
				addParams.put(REQ_PARAM_NAMES[i], getAttribute(REQ_PARAM_NAMES[i]).toString());
			}

			if (checkParams(addParams)) {
				addParams(addParams, req);
			}
			String pin1Name = "parcelnumber1";
			ParametersVector parcelNumber1V = req.getPostParameter(pin1Name);

			if (parcelNumber1V != null) {
				String parcelNumber1 = parcelNumber1V.toString();
				parcelNumber1 = ro.cst.tsearch.utils.StringUtils.extractParameter(parcelNumber1, "^\\s*(\\d{3})-?\\d{3}-?\\d{2}");
				if (!parcelNumber1.isEmpty()) {
					req.removePostParameters(pin1Name);
					req.setPostParameter(pin1Name, parcelNumber1);
				}
			}
		}

	}

}
