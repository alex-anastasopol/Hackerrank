package ro.cst.tsearch.connection.http2;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.apache.commons.lang.StringUtils;

import ro.cst.tsearch.connection.LoginResponse;
import ro.cst.tsearch.connection.LoginResponse.LoginStatus;
import ro.cst.tsearch.connection.http.HTTPRequest;

public class ILWillAO extends SimplestSite {
	public static final String ATS_LINK_ATTRIBUTES_KEY = "ats_link_attributes_key";
	
	private static final String ON_LOGIN_TRUE = "?onLogin=true";

	private static final String[] REQ_PARAM_NAMES = { "__VIEWSTATE", "__EVENTVALIDATION", "__PREVIOUSPAGE", "__LASTFOCUS", "__EVENTTARGET",
			"__EVENTARGUMENT" };

	private static final String[] MODULES_PATHS = { "search_pin.aspx", "search_address.aspx", "search_sale.aspx", "search_nhood.aspx" };
	private static final String FORM_NAME = "aspnetForm";

	@Override
	public LoginResponse onLogin() {
		String link = getSiteLink();
		for (String path : MODULES_PATHS) {
			if (link.contains("/disclaimer.aspx")) {
				link = link.replaceAll("/disclaimer.aspx", "");
			}
			HTTPRequest request = new HTTPRequest(link + "/" + path+ON_LOGIN_TRUE, HTTPRequest.GET);
			String response = process(request).getResponseAsString();
			Map<String, String> addParams = fillConnectionParams(response, REQ_PARAM_NAMES, FORM_NAME);
			if (addParams == null) {
				return new LoginResponse(LoginStatus.STATUS_INVALID_PAGE, "Invalid parameters");
			}
			setAttribute("search.params." + path, addParams);
		}

		return new LoginResponse(LoginStatus.STATUS_SUCCESS, "Logged in");
	}

	@Override
	public void onBeforeRequest(HTTPRequest req) {
		String lnk = req.getPostFirstParameter(ATS_LINK_ATTRIBUTES_KEY);
		req.removePostParameters(ATS_LINK_ATTRIBUTES_KEY);
		Map<String, String> postParams = (Map<String, String>) getTransientSearchAttribute("post_params:" + lnk);
		if (postParams!=null){
			Set<String> keySet = postParams.keySet();
			for (String key : keySet) {
				if(StringUtils.isNotEmpty(postParams.get(key))){
					req.removePostParameters(key);
					Map<String,String> params = new HashMap<String,String>();
					params.put(key, postParams.get(key));
					req.addPostParameters(params);
				}
			}
//			req.addPostParameters(postParams);
		}
		String url = req.getURL();
		String[] path = null;
		if (!url.contains(ON_LOGIN_TRUE)){
			path = url.split("/");
			if (!url.contains("search_pin.aspx")) {
				url = url.replaceAll(path[path.length - 1], "results.aspx");
				req.setURL(url);
			}
		}else{
			url = url.replace(ON_LOGIN_TRUE, "");
			path = url.split("/");
			req.setURL(url);
		}
		
		Map<String, String> addParams = (Map<String, String>) getAttribute("search.params." + path[path.length - 1]);
		if (addParams != null) {
			// put the request parameters in the form
			for (int i = 0; i < REQ_PARAM_NAMES.length; i++) {
				req.removePostParameters(REQ_PARAM_NAMES[i]);
				req.setPostParameter(REQ_PARAM_NAMES[i], addParams.get(REQ_PARAM_NAMES[i]));
			}
			if (url.contains("search_pin.aspx")) {
				req.removePostParameters("__PREVIOUSPAGE");
			}
		}
		super.onBeforeRequest(req);
	}
	
	@Override
	public void onBeforeRequestExcl(HTTPRequest req) {
		//String lnk = req.getPostFirstParameter(ATS_LINK_ATTRIBUTES_KEY);
		req.removePostParameters(ATS_LINK_ATTRIBUTES_KEY);
		//Map<String, String> addParams = (Map<String, String>) getTransientSearchAttribute("params:" + lnk);
	}
}
