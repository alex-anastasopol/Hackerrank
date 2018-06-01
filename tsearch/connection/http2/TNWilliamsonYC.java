package ro.cst.tsearch.connection.http2;


import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import ro.cst.tsearch.connection.LoginResponse;
import ro.cst.tsearch.connection.LoginResponse.LoginStatus;
import ro.cst.tsearch.connection.http.HTTPRequest;
import ro.cst.tsearch.parser.SimpleHtmlParser;
import ro.cst.tsearch.utils.StringUtils;

public class TNWilliamsonYC extends HttpSite {
	
	//private static final String[] ADD_PARAM_NAMES = { "__PREVIOUSPAGE", "__VIEWSTATE", "__EVENTTARGET", "__EVENTARGUMENT", "__LASTFOCUS", "__VIEWSTATEENCRYPTED", "__EVENTVALIDATION"};
	private static final String[] ADD_PARAM_NAMES = { "__VIEWSTATE", "__EVENTTARGET", "__EVENTARGUMENT", "__LASTFOCUS", "__EVENTVALIDATION"};
	//private static final String[] REQ_PARAM_NAMES = { "__VIEWSTATE", "__EVENTVALIDATION"};
	private static final String[] REQ_PARAM_NAMES = { "__VIEWSTATE" };
	
	private static final Pattern VIEWSTATE_PAT = Pattern.compile("(?is)__VIEWSTATE\\s*\\|([^\\|]+)");
	private static final Pattern EVENTVALIDATION_PAT = Pattern.compile("(?is)__EVENTVALIDATION\\s*\\|([^\\|]+)");
		
	private static Map<String,String> parameters;
	static {
		parameters = new HashMap<String,String>();
		
	}
	private String btnAccept 	= "ctl00$cphMainContent$SkeletonCtrl_10$btnAccept";	
	private String btnSearch 	= "ctl00$cphMainContent$SkeletonCtrl_10$btnSearch";
	
	private String searchParam 	= "ctl00$cphMainContent$SkeletonCtrl_10$drpSearchParam";
	private String taxYear 		= "ctl00$cphMainContent$SkeletonCtrl_10$drpTaxYear";
	private String payStatus	= "ctl00$cphMainContent$SkeletonCtrl_10$drpStatus";
		
	private String county = "";
	
	@Override
	public LoginResponse onLogin() {
		county = getDataSite().getCountyName();
		String resp = "";
		HTTPRequest req = new HTTPRequest(getSiteLink(), HTTPRequest.GET);
//		HTTPResponse res = process(req);
		resp = execute(req);
//		resp = res.getResponseAsString();
		
		//isolate parameters
		Map<String,String> addParams = isolateParams(resp, "aspnetForm", county);
		
		req = new HTTPRequest(getSiteLink() + "/default.aspx", HTTPRequest.POST);
		req.addPostParameters(addParams);

//		req.addPostParameters(parameters);
		
		req.setPostParameter(btnAccept, "Yes, I accept");
		resp = execute(req);
		
		addParams = isolateParams(resp, "aspnetForm",county);
		
		// check parameters
		if (!checkParams(addParams)){
			return new LoginResponse(LoginStatus.STATUS_INVALID_PAGE, "Invalid parameters");
		}
		setAttribute("params", addParams);
				
		return new LoginResponse(LoginStatus.STATUS_SUCCESS, "Logged in");
	}

	@SuppressWarnings("unchecked")
	@Override
	public void onBeforeRequestExcl(HTTPRequest req) {
		String url = req.getURL();
		Map<String,String> addParams = (HashMap<String,String>)getAttribute("params");
			
		if (addParams != null){
			if (addParams.containsKey("__PREVIOUSPAGE")){
				addParams.remove("__PREVIOUSPAGE");
			}
		}
		
		if (req.getPostFirstParameter(searchParam) != null){
			if (!(req.getPostFirstParameter(searchParam)).contains("Owner Name") || 
			  !(req.getPostFirstParameter(searchParam)).contains("Receipt") ||
			  !(req.getPostFirstParameter(searchParam)).contains("Property ID") ||
			  !(req.getPostFirstParameter(searchParam)).contains("Address")){
					HTTPRequest req1 = new HTTPRequest(req.getURL(), HTTPRequest.POST);
					req1.setPostParameter(taxYear, req.getPostFirstParameter(taxYear));
					req1.setPostParameter(payStatus, req.getPostFirstParameter(payStatus));				
					req1.setPostParameter(searchParam, req.getPostFirstParameter(searchParam));
					req1.setPostParameter("ctl00$cphMainContent$SkeletonCtrl_10$txtSearchParam", "");
					req1.setPostParameter("ctl00$cphMainContent$SkeletonCtrl_10$gvRecords$ctl03$btnSelectRecord", "View");
				
					req1.setPostParameter("ctl00$ScriptManager1", "ctl00$cphMainContent$SkeletonCtrl_10$UpdatePanel2|ctl00$cphMainContent$SkeletonCtrl_10$tbcTaxes");
					req1.setPostParameter("__ASYNCPOST", "true");
				
					req1.addPostParameters(addParams);
					req1.addPostParameters(parameters);
				
					req1.removePostParameters("__EVENTTARGET");
					req1.setPostParameter("__EVENTTARGET", searchParam);

					String res = execute(req1);
				
					Matcher ev = EVENTVALIDATION_PAT.matcher(res);
					if (ev.find()){
						addParams.remove("__EVENTVALIDATION");
						addParams.put("__EVENTVALIDATION", ev.group(1));
					}
					Matcher vs = VIEWSTATE_PAT.matcher(res);
					if (vs.find()){
						addParams.remove("__VIEWSTATE");
						addParams.put("__VIEWSTATE", vs.group(1));
					}
			}
		}
		
		if (req.getPostFirstParameter("details") != null || req.hasPostParameter("ctl00$ScriptManager1")){
			req.removePostParameters("details");
			addParams = (HashMap<String,String>)getTransientSearchAttribute("paramsDetails:");
			if (addParams != null){
				if (addParams.containsKey("__PREVIOUSPAGE")){
					addParams.remove("__PREVIOUSPAGE");
				}
				if (addParams.containsKey(btnSearch)){
					addParams.remove(btnSearch);
				}
			}
		}
		
		if(req.hasPostParameter("ctl00$ScriptManager1")){
			addParams = (HashMap<String,String>)getTransientSearchAttribute("paramsReceiptDetails:");
		}
		
		if (addParams != null && !req.hasPostParameter("ctl00$ScriptManager1")){
			req.addPostParameters(addParams);
//			req.addPostParameters(parameters);
		} else if(req.hasPostParameter("ctl00$ScriptManager1")) {
			for(String s: ADD_PARAM_NAMES){
				req.setPostParameter(s, addParams.get(s));
			}
			req.removePostParameters("__PREVIOUSPAGE");
			req.removePostParameters("__LASTFOCUS");
			req.removePostParameters("__EVENTTARGET");
			req.removePostParameters("__EVENTARGUMENT");
			req.setPostParameter("__EVENTARGUMENT","activeTabChanged:1");
			req.setPostParameter("__EVENTTARGET","ctl00$cphMainContent$SkeletonCtrl_10$tbcTaxes");
			req.setPostParameter("__ASYNCPOST","true");
		}
	}
	
	public static Map<String, String> isolateParams(String page, String form, String county ){
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
	
}
