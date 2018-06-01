package ro.cst.tsearch.connection.http3;

import static ro.cst.tsearch.connection.http.HTTPRequest.GET;

import java.util.Map;

import ro.cst.tsearch.connection.LoginResponse;
import ro.cst.tsearch.connection.LoginResponse.LoginStatus;
import ro.cst.tsearch.connection.http.HTTPRequest;
import ro.cst.tsearch.connection.http.HTTPRequest.ParametersVector;
import ro.cst.tsearch.parser.SimpleHtmlParser;

public class TNSumnerGallatinYA extends HttpSite3 {

	private static final String		FORM_NAME		= "aspnetForm";
	private static final String[]	REQ_PARAM_NAMES	= { "__VIEWSTATE", "__EVENTVALIDATION" };

	@Override
	public LoginResponse onLogin() {
		try {
			// go to taxes search
			HTTPRequest req = new HTTPRequest(getSiteLink(), GET);
			String p = execute(req);

			Map<String, String> params = new SimpleHtmlParser(p).getForm(FORM_NAME).getParams();

			// check parameters
			for (String key : REQ_PARAM_NAMES) {
				if (!params.containsKey(key)) {
					String errorMsg = "Did not find required parameter " + key;
					logger.error(errorMsg);
					return new LoginResponse(LoginStatus.STATUS_INVALID_PAGE, errorMsg);
				}
			}

			// store parameters
			setAttribute("params", params);

			return new LoginResponse(LoginStatus.STATUS_SUCCESS, "Logged in");
		} catch (Exception e) {
			e.printStackTrace();
		}

		return new LoginResponse(LoginStatus.STATUS_UNKNOWN, "Login failed");
	}

	@SuppressWarnings("unchecked")
	@Override
	public void onBeforeRequestExcl(HTTPRequest req) {
		int method = req.getMethod();
		String initialURL = req.getURL();
		String correctURL = "";
		if (method == HTTPRequest.POST) {
			String urlPOSTParamName = "mode";
			ParametersVector urlPOSTParamPV = req.getPostParameter(urlPOSTParamName);
			if (urlPOSTParamPV != null) {// change a POST param into a get param (put it on the URL)
				correctURL = initialURL + "?" + urlPOSTParamName + "=" + urlPOSTParamPV.toString();
				req.setURL(correctURL);
				req.removePostParameters(urlPOSTParamName);
			}
		}

		Map<String, String> params = null;

		if (req.hasPostParameter("seq")) { // if it's a details link from intermediaries
			if (correctURL.contains("ParcelBrowse.aspx")) {
				correctURL = correctURL.replaceFirst("/Default.aspx\\??(?:mode=new)?(/ParcelBrowse.aspx)", "$1");
				req.setURL(correctURL);
				req.setMethod(HTTPRequest.POST);
			}

			// remove the seq parameter
			String seq = req.getPostFirstParameter("seq");
			req.removePostParameters("seq");

			params = (Map<String, String>) getTransientSearchAttribute("params:" + seq);

		} else { // search from Parent Site
			params = (Map<String, String>) getAttribute("params");
		}

		// check parameters
		if (params != null) {
			// remove ->ctl00$ctl00$PrimaryPlaceHolder$ContentPlaceHolderMain$Control$FormLayoutItem7$ctl01$resetButton=Reset
			// and ctl00$ctl00$PrimaryPlaceHolder$ContentPlaceHolderMain$Control$RememberSearchCriteriaFormLayoutItem$ctl01$RememberCriteriaCheckBox=

			for (String key : REQ_PARAM_NAMES) {
				if (!params.containsKey(key)) {
					logger.error("Did not find required parameter " + key);
				}
			}
			for (Map.Entry<String, String> entry : params.entrySet()) {
				if (!req.hasPostParameter(entry.getKey())) {
					String key = entry.getKey();
					if (!(key.endsWith("resetButton") || key.endsWith("RememberCriteriaCheckBox"))) {
						req.setPostParameter(entry.getKey(), entry.getValue());
					}
				}
			}
		}
	}
}
