package ro.cst.tsearch.connection.http2;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.htmlparser.Node;

import ro.cst.tsearch.connection.LoginResponse;
import ro.cst.tsearch.connection.LoginResponse.LoginStatus;
import ro.cst.tsearch.connection.http.HTTPRequest;
import ro.cst.tsearch.connection.http.HTTPRequest.ParametersVector;
import ro.cst.tsearch.connection.http.HTTPResponse;
import ro.cst.tsearch.parser.HtmlParser3;
import ro.cst.tsearch.parser.SimpleHtmlParser;
import ro.cst.tsearch.utils.StringUtils;

public class ILKaneRO extends HttpSite {

	private static final String[]	ADD_PARAM_NAMES	= { "__VIEWSTATE", "__EVENTVALIDATION", };
	private static final String[]	REQ_PARAM_NAMES	= { "__VIEWSTATE", "__EVENTVALIDATION" };

	@Override
	public LoginResponse onLogin() {

		String link = dataSite.getLink();
		HTTPRequest req = new HTTPRequest(link);
		String response = execute(req);

		// indicate success
		if (response.indexOf("Land Records Search") != -1) {
			return new LoginResponse(LoginStatus.STATUS_SUCCESS, "Logged in");
		}

		return new LoginResponse(LoginStatus.STATUS_INVALID_PAGE, "Invalid response");
	}

	public static Map<String, String> isolateParams(String page, String form) {
		Map<String, String> params = new SimpleHtmlParser(page).getForm(form).getParams();
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

	public static HTTPRequest addParams(Map<String, String> addParams, HTTPRequest request) {
		Iterator<String> i = addParams.keySet().iterator();
		while (i.hasNext()) {
			String k = i.next();
			request.setPostParameter(k, addParams.get(k));
		}
		return request;
	}

	public static boolean checkParams(Map<String, String> addParams) {
		for (String key : REQ_PARAM_NAMES) {
			if (StringUtils.isEmpty(addParams.get(key))) {
				return false;
			}
		}
		return true;
	}

	@Override
	public void onBeforeRequestExcl(HTTPRequest req) {
		String url = req.getURL();

		if (req.getMethod() == HTTPRequest.POST) {

			if (url.endsWith("/Search/UCCSearch")) {// UCC Search
				ParametersVector searchTypeParam = req.getPostParameter("SearchType");
				ParametersVector variableParam = req.getPostParameter("var");
				String searchType = new String();
				String variable = new String();
				if (searchTypeParam != null && variableParam != null) {
					searchType = searchTypeParam.toString();
					variable = variableParam.toString().trim();
					if (searchType.equals("OriginalDoc") || searchType.equals("SpecificDoc")) {
						searchType += "Number";
					}
					req.removePostParameters(searchType);
					req.setPostParameter(searchType, variable);
					req.removePostParameters("var");
					execute(req);

					req.setMethod(HTTPRequest.GET);
					req.setURL(dataSite.getServerHomeLink() + "Search/UCCAjaxResults");
					req.clearPostParameters();
					req.setReferer(StringUtils.transformNull(this.getLastRequest()));

				}
			} else {
				ParametersVector criteriaParam = req.getPostParameter("criteria");
				String criteria = new String();
				if (criteriaParam != null) {
					criteria = criteriaParam.toString();
				}

				String formLink = url;
				if (!criteria.isEmpty()) {
					formLink += "?criteria=" + criteria;
				}

				req.removePostParameters("criteria");
				HTTPRequest formReq = new HTTPRequest(formLink);
				formReq.setMethod(HTTPRequest.GET);
				execute(formReq);
				req.setURL(formLink);
				execute(req);

				req.setMethod(HTTPRequest.GET);
				req.setURL(dataSite.getServerHomeLink() + "Search/InstrumentResults");
				req.clearPostParameters();
				req.setReferer(StringUtils.transformNull(this.getLastRequest()));
			}
		}
	}

	public byte[] getImage(String link) {
		byte[] imageBytes = null;

		try {
			int index = link.indexOf("Link=");
			String url = link;
			if (index != -1) {
				url = link.substring(link.indexOf("Link=") + 5);
			}
			HTTPRequest req = new HTTPRequest(url, HTTPRequest.GET);
			HTTPResponse response = process(req);

			if (response != null) {
				String resp = response.getResponseAsString();
				HtmlParser3 htmlParser3 = new HtmlParser3(resp);
				Node imageLinkNode = htmlParser3.getNodeById("imagePopup");
				if (imageLinkNode != null) {
					url = imageLinkNode.toHtml().replaceFirst("(?is).*?\\bsrc=\"([^\"]*)\".*", "$1");
					req.setURL(url);
					response = process(req);
					if (response != null) {
						resp = response.getResponseAsString();
						htmlParser3 = new HtmlParser3(resp);
						imageLinkNode = htmlParser3.getNodeById("MainContent_WebThumbnailViewer1_uri");
						if (imageLinkNode != null) {
							url = imageLinkNode.toHtml().replaceFirst("(?is).*?\\bvalue=\"([^\"]*)\".*", "$1");
							req.setReferer(StringUtils.transformNull(this.getLastRequest()));
							req.setURL(dataSite.getLink() + url.substring(1));

							imageBytes = process(req).getResponseAsByte();
						}
					}

				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		return imageBytes;
	}
}
