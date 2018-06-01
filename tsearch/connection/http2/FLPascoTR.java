package ro.cst.tsearch.connection.http2;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.lang.StringUtils;
import org.htmlparser.Node;

import ro.cst.tsearch.connection.LoginResponse;
import ro.cst.tsearch.connection.LoginResponse.LoginStatus;
import ro.cst.tsearch.connection.http.HTTPRequest;
import ro.cst.tsearch.connection.http.HTTPResponse;
import ro.cst.tsearch.parser.HtmlParser3;
import ro.cst.tsearch.parser.SimpleHtmlParser;


public class FLPascoTR extends HttpSite {

	private static final String[]	ADD_PARAM_NAMES	= { "__VIEWSTATE", "__EVENTVALIDATION", };
	private static final String[]	REQ_PARAM_NAMES	= { "__VIEWSTATE", "__EVENTVALIDATION" };
	private static final String		FORM_NAME		= "aspnetForm";

	public LoginResponse onLogin() {
		HTTPRequest req = new HTTPRequest(getSiteLink());
		HTTPResponse res = process(req);
		String response = res.getResponseAsString();
		HtmlParser3 htmlParser3 = new HtmlParser3(response);

		// isolate parameters
		Map<String, String> addParams = isolateParams(response, FORM_NAME);

		// check parameters
		if (!checkParams(addParams)) {
			return new LoginResponse(LoginStatus.STATUS_INVALID_PAGE, "Invalid parameters");
		}

		// disclaimer
		Node disclaimerAcceptNode = htmlParser3.getNodeById("ctl00_ContentBody_aupNormal");
		if (disclaimerAcceptNode != null) {
			HTTPRequest disclaimerRequest = new HTTPRequest(getLastRequest());
			disclaimerRequest.setMethod(HTTPRequest.POST);
			disclaimerRequest.setPostParameter("ctl00$ContentBody$aupNormal", "I accept");
			disclaimerRequest.addPostParameters(addParams);

			res = process(disclaimerRequest);
		}

		if (res.getResponseAsString().contains("REAL ESTATE PROPERTY SEARCH")) {
			return new LoginResponse(LoginStatus.STATUS_SUCCESS, "Logged in");
		}

		return new LoginResponse(LoginStatus.STATUS_INVALID_PAGE, "Invalid response");
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

	public void onBeforeRequestExcl(HTTPRequest req) {
		if (status == STATUS_LOGGED_IN && req.getMethod() == HTTPRequest.POST) {
			// isolate parameters
			HTTPRequest req1 = new HTTPRequest(req.getURL());
			HTTPResponse resp1 = process(req1);
			try {
				HashMap<String, HTTPRequest.ParametersVector> postParameters = req.getPostParameters();
				HashMap<String, String> postParamsToUnescape = new HashMap<String, String>();
				for (Map.Entry<String, HTTPRequest.ParametersVector> entry : postParameters.entrySet()) {
					if (!entry.getKey().equals(URLDecoder.decode(entry.getKey(), "UTF-8"))) {
						postParamsToUnescape.put(entry.getKey(), entry.getValue().toString());
					}
				}

				if (!postParamsToUnescape.isEmpty()) {
					for (Map.Entry<String, String> entry : postParamsToUnescape.entrySet()) {
						req.setPostParameter(URLDecoder.decode(entry.getKey(), "UTF-8"), entry.getValue().toString());
						req.removePostParameters(entry.getKey());
					}
				}
			}

			catch (UnsupportedEncodingException e) {
				e.printStackTrace();
			}
			Map<String, String> addParams = isolateParams(resp1.getResponseAsString(), FORM_NAME);

			// check parameters
			if (checkParams(addParams)) {
				req.addPostParameters(addParams);
			}
		}
	}

}