package ro.cst.tsearch.connection.http2;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import ro.cst.tsearch.connection.LoginResponse;
import ro.cst.tsearch.connection.http.HTTPRequest;
import ro.cst.tsearch.connection.http.HTTPResponse;
import ro.cst.tsearch.parser.SimpleHtmlParser;
import ro.cst.tsearch.parser.SimpleHtmlParser.Form;
import ro.cst.tsearch.utils.StringUtils;

public class TNRutherfordTR extends HttpSite {
	private static final String[]		ADD_PARAM_NAMES	= { "__PREVIOUSPAGE", "__VIEWSTATE", "__EVENTTARGET", "__EVENTARGUMENT", "__ASYNCPOST",
														"__VIEWSTATEENCRYPTED", "__EVENTVALIDATION" };
	private static final String[]		REQ_PARAM_NAMES	= { "__VIEWSTATE", "__EVENTVALIDATION" };

	private static Map<String, String>	parameters		= new HashMap<String, String>();
	
	private static final String FORM_NAME = "aspnetForm";

	@Override
	public LoginResponse onLogin() {
		try {
			HTTPRequest reqGETSite = new HTTPRequest(getSiteLink());

			String respGETSite = execute(reqGETSite);

			if (respGETSite.contains("Yes, I accept")) {
				parameters = isolateParams(respGETSite, FORM_NAME);
			} else {
				return LoginResponse.getDefaultFailureResponse();
			}

			// press yes,i accept button
			HTTPRequest reqPOSTyes = new HTTPRequest("http://payments.rctrustee.org/taxes/default.aspx", HTTPRequest.POST);

			addParams(parameters, reqPOSTyes, ADD_PARAM_NAMES);
//			reqPOSTyes.setPostParameter("ctl00$ScriptManager1", "ctl00$MainContent$SkeletonCtrl_8$upTab|ctl00$MainContent$SkeletonCtrl_8$btnAccept");
			reqPOSTyes.removePostParameters("__EVENTTARGET");
			reqPOSTyes.setPostParameter("__EVENTTARGET", "ctl00$MainContent$SkeletonCtrl_8$btnAccept");

			String respPOSTyes = execute(reqPOSTyes);

			if (respPOSTyes.contains("Pay Status") && respPOSTyes.contains("Search Tips")) {
				parameters = isolateParams(respPOSTyes, FORM_NAME);
				return LoginResponse.getDefaultSuccessResponse();
			}

		} catch (Exception e) {
			e.printStackTrace();
		}

		return LoginResponse.getDefaultFailureResponse();
	}

	@SuppressWarnings("unchecked")
	@Override
	public void onBeforeRequestExcl(HTTPRequest req) {
		if (req.getMethod() == HTTPRequest.POST) {
			if (req.hasPostParameter("ctl00$MainContent$SkeletonCtrl_8$btnSearch")) {
				Map<String, String> params;
				params = parameters;
				
				if (req.getPostFirstParameter("ctl00$MainContent$SkeletonCtrl_8$drpSearchParam") != null
						&& req.getPostFirstParameter("ctl00$MainContent$SkeletonCtrl_8$drpSearchParam").contains("Property")){
					HTTPRequest req1 = new HTTPRequest(req.getURL(), HTTPRequest.POST);
					addParams(params, req1, ADD_PARAM_NAMES);
					req1.setPostParameter("ctl00$ScriptManager1", "ctl00$MainContent$SkeletonCtrl_8$upTab|ctl00$MainContent$SkeletonCtrl_8$drpSearchParam");
					req1.setPostParameter("ctl00$MainContent$SkeletonCtrl_8$drpTaxYear", req.getPostFirstParameter("ctl00$MainContent$SkeletonCtrl_8$drpTaxYear"));
					req1.setPostParameter("ctl00$MainContent$SkeletonCtrl_8$drpStatus", "Both");
					req1.setPostParameter("ctl00$MainContent$SkeletonCtrl_8$drpSearchParam", "Property ID");
					req1.setPostParameter("ctl00$MainContent$SkeletonCtrl_8$txtSearchParam", "");
					req1.removePostParameters("__EVENTTARGET");
					req1.setPostParameter("__EVENTTARGET", "ctl00$MainContent$SkeletonCtrl_8$drpSearchParam");
					String resp = execute(req1);
					if (resp.contains("MainContent_SkeletonCtrl_8_panelSearchByPropertyID")){
						params = isolateParams(resp, FORM_NAME);
					}
				}

				addParams(params, req, ADD_PARAM_NAMES);
				req.removePostParameters("__EVENTTARGET");
				req.setPostParameter("__EVENTTARGET", "ctl00$MainContent$SkeletonCtrl_8$btnSearch");
				req.setPostParameter("__LASTFOCUS", "");
			} else if (req.postParameters.toString().contains("btnSelectRecord")) {
				Map<String, String> params;
				if (req.hasPostParameter("seq")) {
					params = (Map<String, String>) getTransientSearchAttribute("params:" + req.getPostFirstParameter("seq"));
					

					if (params != null) {
						//parameters = params;
						
						addParams(params, req, ADD_PARAM_NAMES);
						req.setPostParameter("__LASTFOCUS", "");
						addParams(params, req, new String[] { "ctl00$MainContent$SkeletonCtrl_8$drpTaxYear", "ctl00$MainContent$SkeletonCtrl_8$drpStatus",
								"ctl00$MainContent$SkeletonCtrl_8$drpSearchParam", "ctl00$MainContent$SkeletonCtrl_8$txtSearchParam" });

					}
				}
			}

			req.removePostParameters("seq");
		}
	}

	@Override
	public void onAfterRequest(HTTPRequest req, HTTPResponse res) {
		if (res.getContentType().contains("plain")) {
			res.contentType = "text/html; charset=utf-8";
		}

		super.onAfterRequest(req, res);
	}

	public static HTTPRequest addParams(Map<String, String> addParams, HTTPRequest request, String[] paramNames) {
		if (addParams == null)
			return request;

		if (paramNames == null) {
			Iterator<String> i = addParams.keySet().iterator();
			while (i.hasNext()) {
				String k = i.next();
				if (addParams.containsKey(k))
					request.setPostParameter(k, addParams.get(k));
			}
		} else {
			for (String s : paramNames) {
				if (addParams.containsKey(s))
					request.setPostParameter(s, addParams.get(s));
			}
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

	public static Map<String, String> isolateParams(String page, String form) {
		try {
			page = page.replaceFirst("(<form[^>]*)id=([^>]*>)", "$1name=$2");

			Form f = new SimpleHtmlParser(page).getForm(form);

			if (f != null) {
				Map<String, String> params = f.getParams();
				return params;
			}
		} catch (Exception e) {
			e.printStackTrace();
		}

		return new HashMap<String, String>();
	}
}
