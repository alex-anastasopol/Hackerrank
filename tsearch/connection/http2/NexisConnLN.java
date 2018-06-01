package ro.cst.tsearch.connection.http2;


import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


import ro.cst.tsearch.connection.LoginResponse;
import ro.cst.tsearch.connection.LoginResponse.LoginStatus;
import ro.cst.tsearch.connection.http.HTTPRequest;
import ro.cst.tsearch.connection.http.HTTPResponse;
import ro.cst.tsearch.data.SitesPasswords;
import ro.cst.tsearch.parser.SimpleHtmlParser;
import ro.cst.tsearch.parser.SimpleHtmlParser.Form;
import ro.cst.tsearch.utils.StringUtils;



/**
 * @author mihaib
*/

public class NexisConnLN extends HttpSite{


	private static final String[] ADD_PARAM_NAMES = { "__VIEWSTATE", "__EVENTTARGET", "__EVENTARGUMENT", "__EVENTVALIDATION" };
	private static final String[] REQ_PARAM_NAMES = { "__VIEWSTATE", "__EVENTVALIDATION" };
	
	private static String linkForConfirm = "";
	
	private static final Pattern CHECK_BROWSER_PAT = Pattern.compile("(?is)href\\s*=\\s*[\\\"]([^\\\"]+)\\\">Please click here to continue");
	private static final Pattern ONLOAD_FRAME_PAT = Pattern.compile("(?is)location.href\\s*=\\s*[\\\"]([^\\\"]+)\\\"");
	
	private static final Pattern GOTO_RESULTS_PAT = Pattern.compile("(?is)fireFeatureEvent\\(\\\"([^\\\"]+)\\\"\\s*,\\s*\\\"([^\\\"]+)");
	
	private static final String FORM1 = "form1";
	private static final String ROSETTAFORM = "RosettaForm";
	
	@Override
	public LoginResponse onLogin() {
		
		String userId = SitesPasswords.getInstance().getPasswordValue(getCurrentCommunityId(searchId), "NexisServerLN", "user");
		String password = SitesPasswords.getInstance().getPasswordValue(getCurrentCommunityId(searchId), "NexisServerLN", "password");
		
		if(org.apache.commons.lang.StringUtils.isEmpty(userId) || org.apache.commons.lang.StringUtils.isEmpty(password)) {
			return LoginResponse.getDefaultInvalidCredentialsResponse();
		}
		
		
		setAttribute(SINGLE_LINE_COOKIES_IN_REDIRECT, true);
		
		HTTPRequest req = new HTTPRequest(getSiteLink(), HTTPRequest.GET);
		HTTPResponse res = process(req);
		String response = res.getResponseAsString();
		
		Form form = new SimpleHtmlParser(response).getForm("auth_SignonForm");
		if (form == null){
			return new LoginResponse(LoginStatus.STATUS_INVALID_PAGE, "Login form not found");
		} else {
			
			String action = form.action;
			//List<Input> inputs = form.inputs;
			HTTPRequest reqLogin = new HTTPRequest(action, HTTPRequest.POST);
			
			
			reqLogin.setPostParameter("webId", userId);
			reqLogin.setPostParameter("password", password);
			reqLogin.setPostParameter("dispatch", "signon");
			reqLogin.setPostParameter("originSignonForm", "signonform");
			reqLogin.setPostParameter("login_protocol", "http");
			reqLogin.setPostParameter("screenHt", "");
			reqLogin.setPostParameter("screenWid", "");
			reqLogin.setPostParameter("x", "28");
			reqLogin.setPostParameter("y", "5");		
			
			res = process(reqLogin);
			response = res.getResponseAsString();
			
			Matcher mat = CHECK_BROWSER_PAT.matcher(response);
			if (mat.find()){
				reqLogin = new HTTPRequest(mat.group(1).replaceAll("&bhjs=\\-?\\d+", "&bhcp=1&bhjs=1&bhqs=1"), HTTPRequest.GET);
				res = process(reqLogin);
				response = res.getResponseAsString();
				
				mat.reset();
				mat = ONLOAD_FRAME_PAT.matcher(response);
				if (mat.find()){
					reqLogin = new HTTPRequest(mat.group(1).replaceAll("(?is)&amp;", "&"), HTTPRequest.GET);
					response = execute(reqLogin);
					
					if (response.indexOf("System Error") != -1){
						return new LoginResponse(LoginStatus.STATUS_INVALID_PAGE, "Invalid parameters");
					}
					
					form = new SimpleHtmlParser(response).getForm(FORM1);
					if (form != null){
						linkForConfirm = "https://r3.nexis.com/nexisprma/US/" + form.action;
					}
					
					//isolate parameters
					Map<String,String> addParams = isolateParams(response, FORM1);
					
					// check parameters
					if (!checkParams(addParams)){
						return new LoginResponse(LoginStatus.STATUS_INVALID_PAGE, "Invalid parameters");
					}
					
					setAttribute("params", addParams);
					
				} else {
					return new LoginResponse(LoginStatus.STATUS_INVALID_PAGE, "Redirect link not found");
				}
			} else {
				return new LoginResponse(LoginStatus.STATUS_INVALID_PAGE, "Redirect link not found");
			}
			
			// indicate success
			return new LoginResponse(LoginStatus.STATUS_SUCCESS, "Logged in");
		}
		
	}
	
	@Override
	public void onBeforeRequestExcl(HTTPRequest req) {
    	
		HTTPResponse httpResp = null;
		String response = "";
			
		@SuppressWarnings("unchecked")
		Map<String,String> addParams = (Map<String, String>) getAttribute("params");
			
		//first must pass the confirm page choosing DPPA and GLBA
		if (req.getPostFirstParameter("ctl00$MainContent$dppaList") != null && req.getPostFirstParameter("ctl00$MainContent$glbaList") != null){
				
			HTTPRequest reqConfirm = new HTTPRequest(linkForConfirm.replaceAll("(?is)&amp;", "&"), HTTPRequest.POST);
			if (addParams != null){
				reqConfirm.addPostParameters(addParams);
			}
			reqConfirm.setPostParameter("ctl00$MainContent$dppaList", req.getPostFirstParameter("ctl00$MainContent$dppaList"));
			reqConfirm.setPostParameter("ctl00$MainContent$glbaList", req.getPostFirstParameter("ctl00$MainContent$glbaList"));
			reqConfirm.setPostParameter("ctl00$MainContent$btnConfirm1", "Confirm");
				
			httpResp = process(reqConfirm);
			response = httpResp.getResponseAsString();
							
			addParams = isolateParams(response, ROSETTAFORM);
				
			req.removePostParameters("ctl00$MainContent$dppaList");
			req.removePostParameters("ctl00$MainContent$glbaList");
		} 
		if (req.getURL().contains("FindABusiness.aspx")){
			HTTPRequest reqChangeToBusiness = new HTTPRequest("https://r3.nexis.com/nexisprma/US/FindABusiness.aspx", HTTPRequest.GET);
			httpResp = process(reqChangeToBusiness);
			response = httpResp.getResponseAsString();
			
			addParams = isolateParams(response, ROSETTAFORM);
		}
		if (req.getURL().contains("Results.aspx")){
			@SuppressWarnings("unchecked")
			Map<String,String> navParams = (Map<String, String>)getTransientSearchAttribute("paramsNav:");
			if (navParams != null && req.getPostFirstParameter("nav") != null){
				String setId = req.getPostFirstParameter("setId");
				String url = req.getURL();
				if (StringUtils.isNotEmpty(setId)){
					req.setURL(url + "?setId=" + setId);
				}
				String navParam = req.getPostFirstParameter("nav");
				req.removePostParameters("nav");
				req.setPostParameter(navParam + ".x", "13");
				req.setPostParameter(navParam + ".y", "5");
				req.setPostParameter("ctl00$MainContent$toolbar$filterTerms", "");
				
				req.addPostParameters(navParams);
			}
			
		} else {
			if (req.getMethod() == HTTPRequest.POST){
				req.addPostParameters(addParams);
			}
			
			String url = req.getURL();
			String docNumber = StringUtils.extractParameterFromUrl(url, "docNumber");
			url = url.replaceFirst("(?is)&docNumber=" + docNumber, "");
			req.setURL(url);
			httpResp = process(req);
			response = httpResp.getResponseAsString();
					
			Matcher mat = GOTO_RESULTS_PAT.matcher(response);
					
			if (mat.find()){
				req.setURL(mat.group(2));
				req.setMethod(HTTPRequest.GET);
			}
		}
		
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
	
	@Override
	public void onAfterRequest(HTTPRequest req, HTTPResponse res) {

		if(status != STATUS_LOGGING_IN && (res == null || res.returnCode == 0)){
		
		}	
	}
	
}