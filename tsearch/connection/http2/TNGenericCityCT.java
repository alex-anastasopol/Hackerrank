package ro.cst.tsearch.connection.http2;

import ro.cst.tsearch.connection.LoginResponse;
import ro.cst.tsearch.connection.LoginResponse.LoginStatus;
import ro.cst.tsearch.connection.http.HTTPRequest;
import ro.cst.tsearch.connection.http.HTTPResponse;

public class TNGenericCityCT extends HttpSite {

	protected String cityName = "";

	protected void setcntyName(String s) {
		this.cityName = s;
	}

	public LoginResponse onLogin() {

		String link = getCrtServerLink();
		HTTPRequest req = new HTTPRequest(link);
		req.setMethod(HTTPRequest.GET);
		req.setHeader("Host", getCrtServerLink().toLowerCase().replaceAll("https*://",""));

		HTTPResponse res = process(req);
		String response = res.getResponseAsString();

		if (response
				.contains("You may begin by choosing a search method below.")) {
			return new LoginResponse(LoginStatus.STATUS_SUCCESS, "Logged in!");
		} else
			return new LoginResponse(LoginStatus.STATUS_INVALID_PAGE,
					"Wrong response from server!");
	}

	private String getCrtServerLink() {
		String link = getSiteLink();
		int idx = link.indexOf(".org");
		if (idx == -1) {
			throw new RuntimeException("County " + getDataSite().getName()
					+ " not supported by this class!");
		}
		return link.substring(0, idx) + ".org";
	}

	@Override
	public void onBeforeRequestExcl(HTTPRequest req) {

		String url = req.getURL();

		req.removePostParameters("search[86:&gt;:0]");
		req.setPostParameter("search[86:>:0]", "use");
		url = url.replaceFirst("(?is)[/][/]search.php", "/search.php");
		url = url.replaceFirst("(?is)[/][/]taxcard.php", "/taxcard.php");
		// url = url.replace("rutherford", cntyName);
		req.modifyURL(url);

	}

	public String getPage(String link) {
		HTTPRequest req = new HTTPRequest(link);
		req.setMethod(HTTPRequest.GET);

		return execute(req);
	}
}
