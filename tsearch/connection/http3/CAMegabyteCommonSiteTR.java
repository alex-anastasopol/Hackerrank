package ro.cst.tsearch.connection.http3;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;

import ro.cst.tsearch.connection.LoginResponse;
import ro.cst.tsearch.connection.LoginResponse.LoginStatus;
import ro.cst.tsearch.connection.http.HTTPRequest;
import ro.cst.tsearch.connection.http.HTTPResponse;
import ro.cst.tsearch.utils.StringUtils;

public class CAMegabyteCommonSiteTR extends HttpSite3 {
	protected String	sid;
	protected String	sidDelimiterLeft	= "megabytecommonsite/";
	protected String	sidDelimiterRight	= "/PublicInquiry";
	protected String	countyName			= "";

	@Override
	public LoginResponse onLogin() {
		try {
			String link = getSiteLink();
			String serverHomeLink = getDataSite().getServerHomeLink();
			String responseAsString = "";
			HTTPRequest req = new HTTPRequest(link, HTTPRequest.GET);
			req.noRedirects = true;

			HTTPResponse res = process(req);
			responseAsString = res.getResponseAsString();

			if (res.getReturnCode() / 100 == 3) {

				sid = StringUtils.extractParameter(responseAsString, "(?is)" + sidDelimiterLeft + "(.*?)" + sidDelimiterRight);
				String redirectPage = StringUtils.extractParameter(responseAsString, "(?is)<a\\b[^>]*href=(?:'|\")([^>]*)(?:'|\")[^>]*>");
				req = new HTTPRequest(serverHomeLink + redirectPage.substring(1));

				req.noRedirects = false;
				res = process(req);
				responseAsString = res.getResponseAsString();
			}

			if (!StringUtils.extractParameter(responseAsString, "(?is)(Tax\\s+Inquiry\\s*:?\\s*Please\\s+enter\\s+search\\s+criteria)").isEmpty()) {
				return new LoginResponse(LoginStatus.STATUS_SUCCESS, "Logged in");
			}

			return LoginResponse.getDefaultFailureResponse();
		} catch (Exception e) {
			e.printStackTrace();
			sid = null;
			return LoginResponse.getDefaultFailureResponse();
		}
	}

	@Override
	public void onBeforeRequestExcl(HTTPRequest req) {

		String oldURL = req.getURL();
		String newUrl = oldURL.replaceFirst("(?i)(" + sidDelimiterLeft + ")(" + sidDelimiterRight.substring(1) + ")", "$1" + sid + "/$2");

		if (req.getMethod() == HTTPRequest.POST) {// when doing a search
			// add sid to url
			String edtArgs = "?CN=" + countyName + "&DEPT=tax&SITE=public";

			newUrl += edtArgs + "&PG=taxresults";

			try {
				edtArgs = URLEncoder.encode(edtArgs, "UTF-8");
			} catch (UnsupportedEncodingException e) {
				e.printStackTrace();
			}
			req.setPostParameter("edtargs", edtArgs);

			// remove unneeded POST parameters added from PS module destinationPage field
			if (req.hasPostParameter("CN")) {
				req.removePostParameters("CN");
			}
			if (req.hasPostParameter("DEPT")) {
				req.removePostParameters("DEPT");
			}
			if (req.hasPostParameter("SITE")) {
				req.removePostParameters("SITE");
			}
			if (req.hasPostParameter("PG")) {
				req.removePostParameters("PG");
			}
		}

		if (req.getMethod() == HTTPRequest.GET) {
			if (newUrl.contains("ownerNameIntermediary")) {// remove the owner name param on the link
				newUrl = newUrl.replaceFirst("(?:\\?|&)ownerNameIntermediary=.*?(&|$)", "$1");
			}
			if (newUrl.contains("taxRateArea")) {// remove the tax rate area param on the link
				newUrl = newUrl.replaceFirst("(?:\\?|&)taxRateArea=.*?(&|$)", "$1");
			}
		}

		req.setURL(newUrl);
	}
}
