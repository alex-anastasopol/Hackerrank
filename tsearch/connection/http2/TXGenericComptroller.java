package ro.cst.tsearch.connection.http2;

import ro.cst.tsearch.connection.http.HTTPRequest;
import ro.cst.tsearch.connection.http.HTTPResponse;
import ro.cst.tsearch.utils.RegExUtils;

public class TXGenericComptroller extends HttpSite {

	public static final String THIS_IS_ACCOUNT_STATUS_PAGE = "THIS_IS_ACCOUNT_STATUS_PAGE";
	public static final String INPUT_TYPE_HIDDEN_NAME_THIS_IS_ACCOUNT_STATUS_PAGE_HTML = "<input type=\"hidden\" name=\""
			+ THIS_IS_ACCOUNT_STATUS_PAGE + "\" />";

	public String getAccountStatusDeclaration(String url) {
		url = RegExUtils.getFirstMatch("(?is)Link=(.*)", url, 1);
		url = RegExUtils.getFirstMatch("(https://[^/]+).*", getSiteLink(), 1) + url;

		HTTPRequest httpRequest = new HTTPRequest(url);
		HTTPResponse process = process(httpRequest);
		String responseAsString = process.getResponseAsString();
		return responseAsString;
	}

	@Override
	public void onAfterRequest(HTTPRequest req, HTTPResponse res) {
		super.onAfterRequest(req, res);
	}

}
