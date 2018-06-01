package ro.cst.tsearch.connection.http2;

import static ro.cst.tsearch.connection.http.HTTPRequest.GET;
import static ro.cst.tsearch.connection.http.HTTPRequest.POST;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Vector;

import org.apache.commons.httpclient.Cookie;
import org.apache.commons.httpclient.HttpState;
import org.apache.commons.lang.StringUtils;

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
 *         Feb 15, 2011
 */

public class FLMarionTR extends HttpSite {
	private static final String[] ADD_PARAM_NAMES = { "__VIEWSTATE", "__EVENTVALIDATION" };
	private static final String[] REQ_PARAM_NAMES = { "__VIEWSTATE", "__EVENTVALIDATION" };
	private static final String FORM_NAME = "aspnetForm";

	private Map<String, String> params = null;

	
	@Override
	public String getUserAgentValue() {
		return "Mozilla/5.0 (Windows NT 6.1; WOW64; rv:26.0) Gecko/20100101 Firefox/26.0";
	}
	
	@Override
	public String getAcceptLanguage() {
		return "en-US,en;q=0.5";
	}
	
	@Override
	public String getAccept() {
		return "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8";
	}
	
	@Override
	public LoginResponse onLogin() {
		try {
			HTTPRequest req = new HTTPRequest(getSiteLink(), GET);
			HTTPResponse res = process(req);
			
			req.setHeader("User-Agent", getUserAgentValue());
			req.setHeader("Accept", getAccept());
			req.setHeader("Accept-Language", getAcceptLanguage());
			req.setHeader("Accept-Encoding", "gzip, deflate");
			req.setHeader("Connection", "keep-alive");
			String response = res.getResponseAsString();
			if (response.contains("Search Disclaimer")) {
//				Form form = new SimpleHtmlParser(response).getForm("frmDisclaimer");
//				params = form.getParams();
				params = isolateParams(response, "frmDisclaimer", REQ_PARAM_NAMES);
				
				req.setURL("http://www.mariontax.com" + req.getRedirectLocation());
				req.setMethod(POST);
				req.addPostParameters(params);
				req.setPostParameter("btnAgree", "I Agree");
				
				response = execute(req);
				
				params = isolateParams(response, FORM_NAME, REQ_PARAM_NAMES);

				return new LoginResponse(LoginStatus.STATUS_SUCCESS, "Logged in");
			} else
				return new LoginResponse(LoginStatus.STATUS_INVALID_PAGE, "Invalid response");

		} catch (Exception e) {
			e.printStackTrace();
		}
		return new LoginResponse(LoginStatus.STATUS_SUCCESS, "Logged in");
	}

	public static Map<String, String> isolateParams(String page, String form, String[] paramsSet){
		Map<String, String> params = new SimpleHtmlParser(page).getForm(form).getParams();
		Map<String,String> addParams = new HashMap<String,String>();
		for (String key : paramsSet) {
			String value = "";
			if (params.containsKey(key)) {
				value = params.get(key);
			}
			addParams.put(key, value);
		}
		
		return addParams;
	}
	
	@Override
	public void onBeforeRequest(HTTPRequest req) {
		super.onBeforeRequest(req);
	}

	@SuppressWarnings("unchecked")
	@Override
	public void onBeforeRequestExcl(HTTPRequest req) {
		req.setHeader("User-Agent", getUserAgentValue());
		req.setHeader("Accept", getAccept());
		req.setHeader("Accept-Language", getAcceptLanguage());
		req.setHeader("Accept-Encoding", "gzip, deflate");
		if (req.getMethod() == 1) {
			if (req.hasPostParameter("seq")) {
				String seq = req.getPostFirstParameter("seq");
				req.removePostParameters("seq");

				String year = req.getPostFirstParameter("Year");
				String search = req.getPostFirstParameter("Search");
				String owner = req.getPostFirstParameter("Owner").replace(" ","+");
				String exact = req.getPostFirstParameter("Exact");
				String account = req.getPostFirstParameter("Account");
				String url = "";

				if (year != null && search != null && owner != null) {
					if (exact == null)
						url = req.getURL() + "?Search=" + search + "&Owner="
								+ owner + "&Year=" + year;
					else
						url = req.getURL() + "?Search=" + search + "&Owner="
								+ owner + "&Year=" + year + "&Exact=" + exact;

					req.removePostParameters("Year");
					req.removePostParameters("Search");
					req.removePostParameters("Owner");
					if (exact != null)
						req.removePostParameters("Exact");

					req.modifyURL(url);
				} else if (search != null && account != null) {
					url = req.getURL() + "?Search=" + search + "&Account="
							+ account;
					req.removePostParameters("Search");
					req.removePostParameters("Account");

					req.modifyURL(url);
				}
				Map<String, String> param = null;

				if (seq != null) {
					param = (Map<String, String>) getTransientSearchAttribute("params:" + seq);
				}
				if (param != null) {
					for (String s : ADD_PARAM_NAMES) {
						if (param.containsKey(s))
							req.setPostParameter(s, param.get(s));
					}
				}
			} else {
				HTTPRequest req1 = new HTTPRequest(req.getURL(), GET);
				req1.setHeader("User-Agent", getUserAgentValue());
				req1.setHeader("Accept", getAccept());
				req1.setHeader("Accept-Language", getAcceptLanguage());
				req1.setHeader("Accept-Encoding", "gzip, deflate");
				req1.setHeader("Connection", "keep-alive");
				String response = "";
				
				if (req.hasPostParameter("_ctl0:ContentPlaceHolder1:txtAccount")  ||   //ParcelID search
				   req.hasPostParameter("_ctl0:ContentPlaceHolder1:txtSitusName") ||   //PropertyAddress seaerch)
				   req.hasPostParameter("_ctl0:ContentPlaceHolder1:txtOwner")) {       //OwnerName search) 
						req1.setURL(req.getURL());
						response = execute(req1);
						if (response.contains("For additional search options, hover the cursor over ")) {
							params = isolateParams(response, FORM_NAME, REQ_PARAM_NAMES);
						}
						if (params != null) {
							for (String s : ADD_PARAM_NAMES) {
								if (params.containsKey(s))
									req.setPostParameter(s, params.get(s));
							}
						}
						
						if (req.hasPostParameter("_ctl0:ContentPlaceHolder1:txtOwner")) { //OwnerSearch
							if (StringUtils.isNotEmpty(req.getPostFirstParameter("_ctl0:ContentPlaceHolder1:txtOwner"))) { //Full Owner name search
								req.removePostParameters("_ctl0:ContentPlaceHolder1:btnSearch2");
							} else { //Partial Owner name search
								req.removePostParameters("_ctl0:ContentPlaceHolder1:btnSearch1");
							}
						}
						
				} 
			}
		}
	}

	@Override
	public void onAfterRequest(HTTPRequest req, HTTPResponse res) {
		String url = req.getURL();
		if (url.contains("?Acctno=+++++++++++++++")) {
			// destroySession();
		}
	}
}
