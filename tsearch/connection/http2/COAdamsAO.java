
package ro.cst.tsearch.connection.http2;

import java.io.ByteArrayInputStream;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import ro.cst.tsearch.connection.LoginResponse;
import ro.cst.tsearch.connection.LoginResponse.LoginStatus;
import ro.cst.tsearch.connection.http.HTTPRequest;
import ro.cst.tsearch.connection.http.HTTPResponse;
import ro.cst.tsearch.parser.SimpleHtmlParser;
import ro.cst.tsearch.utils.StringUtils;

/**
 * @author mihaib
  */
public class COAdamsAO extends HttpSite {
	
	private static final String[] ADD_PARAM_NAMES = { "__LASTFOCUS", "__VIEWSTATE", "__EVENTARGUMENT", "__EVENTVALIDATION","__PREVIOUSPAGE"};
	private static final String[] REQ_PARAM_NAMES = { "__VIEWSTATE", "__EVENTVALIDATION", "__PREVIOUSPAGE" };
	public String cookie="";
	
	@Override
	public LoginResponse onLogin() {

		String link = getCrtServerLink() + ".us/quicksearch/";
		String resp = "";
		HTTPRequest req = new HTTPRequest(link);
		HTTPResponse res = process(req);
		this.cookie=res.getHeader("Set-Cookie");
		//resp = execute(req);
		resp = res.getResponseAsString();
		if (resp.indexOf("Redirecting to the") != -1){
			link = StringUtils.extractParameter(resp, "(?is)url\\s*=\\s*\\\"?([^\\\"]+)");
			req = new HTTPRequest(link);
			res = process(req);

			resp = res.getResponseAsString();
		}
		//isolate parameters
		Map<String,String> addParams = isolateParams(resp, "aspnetForm");
		
		// check parameters
		if (!checkParams(addParams)){
			return new LoginResponse(LoginStatus.STATUS_INVALID_PAGE, "Invalid parameters");
		}
		setAttribute("params", addParams);
		
		return new LoginResponse(LoginStatus.STATUS_SUCCESS, "Logged in");
	}
	
	/**
	 * Get the current server link
	 * @return
	 */
	private String getCrtServerLink(){
		String link = getSiteLink();
		int idx = link.indexOf(".us");
		if(idx == -1){
			throw new RuntimeException("County " + getDataSite().getName() + " not supported by this class!");
		}
		return link.substring(0, idx);
	}

	public static Map<String, String> isolateParams(String page, String form){
		Map<String, String> params = new SimpleHtmlParser(page).getForm(form).getParams();
		Map<String,String> addParams = new HashMap<String,String>();
		for (String key : ADD_PARAM_NAMES) {
			String value = "";
			if (params.containsKey(key)) {
				value = params.get(key);
			}
			addParams.put(key, value);
		}
		
		return addParams;
	}
	
	public static HTTPRequest addParams(Map<String, String> addParams, HTTPRequest request){
		Iterator<String> i = addParams.keySet().iterator();
		while(i.hasNext()){
			String k = i.next();
			request.setPostParameter(k, addParams.get(k));
		}		
		return request;
	}
	
	public static boolean checkParams(Map<String, String> addParams){
		for(String key: REQ_PARAM_NAMES){
			if(StringUtils.isEmpty(addParams.get(key))){
				return false;
			}
		}
		return true;
	}
	
	@SuppressWarnings("unchecked")
	@Override
	public void onBeforeRequestExcl(HTTPRequest req) {
		Map<String, String> addParams = (Map<String, String>) getAttribute("params");
		if (req.getURL().contains("AdvancedSearch")) {
			req.setPostParameter("__EVENTTARGET", "ctl00$ContentPlaceHolder$SearchSubmitLink");

			String link = getCrtServerLink() + ".us/quicksearch/AdvancedSearch.aspx";
			String resp = "";
			HTTPRequest req1 = new HTTPRequest(link, HTTPRequest.GET);
			HTTPResponse res = process(req1);
			resp = res.getResponseAsString();
			// isolate parameters
			addParams = isolateParams(resp, "aspnetForm");

			// check parameters
			if (checkParams(addParams)) {
				setAttribute("AdvancedSearchParams", addParams);
			}
		}
		if (addParams != null && req.getMethod() == HTTPRequest.POST) {
			for (Map.Entry<String, String> entry : addParams.entrySet()) {
				req.setPostParameter(entry.getKey(), entry.getValue());
			}
		}

	}
	
	@Override
	public void onAfterRequest(HTTPRequest req, HTTPResponse res) {
		String content = "";
		if (res.getContentType().contains("text/html")){
			content = res.getResponseAsString();
			res.is = new ByteArrayInputStream(content.getBytes());
			if(content.contains("Registered Log In")) {
				destroySession();
			} else {
				if (req.getMethod()==HTTPRequest.GET) {
					String url = req.getURL();
					if (url.contains("/quicksearch/doreport.aspx")) {
						Matcher ma = Pattern.compile("(?is)<body>\\s*<noscript>.*?</noscript>\\s*</body>").matcher(content);
						if (ma.find()) {		//empty response
							req.setMethod(HTTPRequest.POST);
							HTTPResponse resp = process(req);
							res.is = new ByteArrayInputStream(resp.getResponseAsByte());
							res.contentLenght= resp.contentLenght;
							res.contentType = resp.contentType;
							res.headers = resp.headers;
							res.returnCode = resp.returnCode;
							res.body = resp.body;
							res.setLastURI(resp.getLastURI());
						}
					}	
				} else {
					if (content.contains("Invalid postback or callback argument") && req.getURL().contains("/AdvancedSearch.aspx")) {
						res.returnCode = 200;
					}
				}
			}
		}
	}
}