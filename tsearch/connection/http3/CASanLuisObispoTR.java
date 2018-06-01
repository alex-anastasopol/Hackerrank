package ro.cst.tsearch.connection.http3;

import java.util.Map;

import org.apache.commons.httpclient.protocol.Protocol;
import org.apache.commons.lang.StringEscapeUtils;
import org.htmlparser.Node;
import org.htmlparser.tags.LinkTag;

import ro.cst.tsearch.connection.LoginResponse;
import ro.cst.tsearch.connection.LoginResponse.LoginStatus;
import ro.cst.tsearch.connection.http.HTTPRequest;
import ro.cst.tsearch.connection.http.HTTPRequest.ParametersVector;
import ro.cst.tsearch.connection.http.auth.ssl.EasySSLProtocolSocketFactory;
import ro.cst.tsearch.parser.HtmlParser3;
import ro.cst.tsearch.utils.RegExUtils;
import ro.cst.tsearch.utils.StringUtils;

public class CASanLuisObispoTR extends AdvancedTemplateSite {
	private static String	urlPOSTParamName	= "csus";
	private static String	txtAPNParamName		= "txtAPN";

	public CASanLuisObispoTR() {
		setFormName("Form1");

		MAIN_PARAMETERS = new String[2];
		MAIN_PARAMETERS[0] = "__VIEWSTATE";
		MAIN_PARAMETERS[1] = "__EVENTVALIDATION";
		MAIN_PARAMETERS_KEY = "search.params";

		TARGET_ARGUMENT_MIDDLE_KEY = ":params:";
		TARGET_ARGUMENT_PARAMETERS = new String[4];
		TARGET_ARGUMENT_PARAMETERS[0] = "__VIEWSTATE";
		TARGET_ARGUMENT_PARAMETERS[1] = "__EVENTTARGET";
		TARGET_ARGUMENT_PARAMETERS[2] = "__EVENTARGUMENT";
		TARGET_ARGUMENT_PARAMETERS[3] = "__EVENTVALIDATION";
	}

	@Override
	public LoginResponse onLogin() {
		try {
			Protocol easyhttps = new Protocol("https", new EasySSLProtocolSocketFactory(), 443);
			Protocol.registerProtocol("https", easyhttps);

			String siteLink = getSiteLink();

			// do first request and get and fill main params
			HTTPRequest request = new HTTPRequest(siteLink);
			String responseAsString = process(request).getResponseAsString();
			Map<String, String> addParams = fillAndValidateConnectionParams(responseAsString, MAIN_PARAMETERS, FORM_NAME);
			if (addParams == null) {
				return new LoginResponse(LoginStatus.STATUS_INVALID_PAGE, "Invalid parameters");
			}
			setAttribute(MAIN_PARAMETERS_KEY, addParams);

			if (!StringUtils.extractParameter(responseAsString,
					"(?is)(>\\s*Property\\s+Tax\\s+Information\\s+Home\\s*<)").isEmpty()) {

				// do second request to get to the search page
				HtmlParser3 htmlParser3 = new HtmlParser3(responseAsString);
				Node securedTaxesLinkNode = htmlParser3.getNodeById("btnSecuredSearch");
				if (securedTaxesLinkNode != null && securedTaxesLinkNode instanceof LinkTag) {
					String securedTaxesParam = RegExUtils.getFirstMatch("doPostBack\\('([^']*)'",
							StringEscapeUtils.unescapeHtml(((LinkTag) securedTaxesLinkNode).getLink()), 1);
					request = new HTTPRequest(siteLink, HTTPRequest.POST);
					request.setPostParameter(TARGET_ARGUMENT_PARAMETERS[1], securedTaxesParam);
					onBeforeRequestExcl(request);
					responseAsString = execute(request);

					setFormName("theForm");
					addParams = fillAndValidateConnectionParams(responseAsString, MAIN_PARAMETERS, FORM_NAME);
					setAttribute(MAIN_PARAMETERS_KEY, addParams);

					// set form name back to the initial one, so that when login is done again the form name is correct
					setFormName("Form1");

					if (addParams == null) {
						return new LoginResponse(LoginStatus.STATUS_INVALID_PAGE, "Invalid parameters");
					}
				}
				return new LoginResponse(LoginStatus.STATUS_SUCCESS, "Logged in");
			}

			return LoginResponse.getDefaultFailureResponse();
		} catch (Exception e) {
			e.printStackTrace();
			return LoginResponse.getDefaultFailureResponse();
		}
	}

	@Override
	public void onBeforeRequestExcl(HTTPRequest req) {
		try {
			String url = req.getURL();
			if (req.getMethod() == HTTPRequest.POST) {

				super.onBeforeRequestExcl(req);

				ParametersVector urlPOSTParamPV = req.getPostParameter(urlPOSTParamName);
				if (urlPOSTParamPV != null) {// set a POST param as a param on the url and remove it as a POST param
					req.setURL(url + "?" + urlPOSTParamName + "=" + urlPOSTParamPV.toString());
					req.removePostParameters(urlPOSTParamName);
				}

				ParametersVector txtAPNParamPV = req.getPostParameter(txtAPNParamName);
				if (txtAPNParamPV != null) {// split Assessment no param in parent site to three params and remove it
					String txtAPNParam = txtAPNParamPV.toString().replaceAll("[^\\d]+", "");
					if (txtAPNParam.length() > 9) {
						txtAPNParam = txtAPNParam.substring(0, 9);
					}

					java.util.List<String> txtAPNParts = RegExUtils.getMatches("(\\d{3})(\\d{3})(\\d{3})", txtAPNParam);
					if (txtAPNParts.size() == 3) {
						req.setPostParameter("txtAPN1", txtAPNParts.get(0));
						req.setPostParameter("txtAPN2", txtAPNParts.get(1));
						req.setPostParameter("txtAPN3", txtAPNParts.get(2));
					}

					req.removePostParameters(txtAPNParamName);
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}
