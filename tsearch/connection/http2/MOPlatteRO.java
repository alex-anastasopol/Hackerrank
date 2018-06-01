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
import ro.cst.tsearch.parser.SimpleHtmlParser;
import ro.cst.tsearch.utils.StringUtils;



/**
 * @author mihaib
*/

public class MOPlatteRO extends HttpSite{

	private static final String[] ADD_PARAM_NAMES = { "__VIEWSTATE", "__EVENTVALIDATION",};
	private static final String[] REQ_PARAM_NAMES = { "__VIEWSTATE", "__EVENTVALIDATION" };

	@Override
	public LoginResponse onLogin() {
		
		setAttribute(SINGLE_LINE_COOKIES_IN_REDIRECT, true);
		
		String link = getSiteLink();

		HTTPRequest req = new HTTPRequest(link, HTTPRequest.GET);
		String resp = execute(req);
		
		//isolate parameters
		Map<String,String> addParams = isolateParams(resp, "LOGIN");
		
		// check parameters
		if (!checkParams(addParams)){
			return new LoginResponse(LoginStatus.STATUS_INVALID_PAGE, "Invalid parameters");
		}
		setAttribute("params", addParams);
		req = new HTTPRequest(link, HTTPRequest.POST);
		req.setPostParameter("USERID", getPassword("user"));
		req.setPostParameter("PASSWORD", getPassword("password"));
		req.setPostParameter("btnLogin", "Login");
		req.addPostParameters(addParams);
		
		resp = execute(req);
		
		if(resp.contains("Error:  Login Failed")){
			logger.error("Error:  Login Failed!");
			return new LoginResponse(LoginStatus.STATUS_INVALID_CREDENTIALS, "Invalid credentials");
		} else if(resp.contains("btnLogin")){
			logger.error("Login undone!");
			return new LoginResponse(LoginStatus.STATUS_UNKNOWN, "Login failed");
		}
		
		addParams = isolateParams(resp, "FORM_CRITERIA");
		
		// check parameters
		if (!checkParams(addParams)){
			return new LoginResponse(LoginStatus.STATUS_INVALID_PAGE, "Invalid parameters");
		}
		setAttribute("params", addParams);
				
		// indicate success
		return new LoginResponse(LoginStatus.STATUS_SUCCESS, "Logged in");
	}
	

	@SuppressWarnings("unchecked")
	@Override
	public void onBeforeRequestExcl(HTTPRequest req) {

		String link = getSiteLink();
		
		Map<String,String> addParams = null;
		
		if (req.getURL().contains("DisplayDocument.aspx")){
			String resp = execute(req);
			if (resp.contains("Click here to View the Document")){
				String imageLink = link.substring(0, link.indexOf(".mo.us")) + ".mo.us";
				Pattern pat = Pattern.compile("(?is)href\\s*=\\s*[\\\"|']([^\\\"]+)\\\"[^>]*>Click here to View the Document");
				Matcher mat = pat.matcher(resp);
				if (mat.find()){
					HTTPRequest req1 = new HTTPRequest(imageLink + "/" + mat.group(1), HTTPRequest.GET);
					HTTPResponse res = process(req1);
					
					res.contentType = "application/pdf";
					// bypass response
					res.is = res.getResponseAsStream();
					req.setBypassResponse(res);
				}
			}
		} else if (!req.getURL().contains("Offset") && !req.getURL().contains("REALSummary.aspx")){
			if (req.getPostFirstParameter("navigation") != null && req.getPostFirstParameter("seq") != null){
				String navParam = req.getPostFirstParameter("navigation");
				req.removePostParameters("navigation");
				
				req.setPostParameter("__EVENTTARGET", navParam);
				String seq = req.getPostFirstParameter("seq");
				req.removePostParameters("seq");
				
				// additional parameters are stored inside the search since they are too long
				addParams = (Map<String, String>)getTransientSearchAttribute("params:" + seq);
				
				for(Map.Entry<String, String> entry: addParams.entrySet()){
					req.setPostParameter(entry.getKey(), entry.getValue());
				}
			} else{
				String url = req.getURL();
				if (addParams == null){// go to module
					HTTPRequest req1 = new HTTPRequest(url, HTTPRequest.GET);
					String resp = execute(req1);
					addParams = isolateParams(resp, "FORM_CRITERIA");
					if (checkParams(addParams)){
						setAttribute("params", addParams);
					}
				}
					
				if (req.getPostFirstParameter("TYPE") != null && req.getPostFirstParameter("ACTION") == null){// go to advanced search
					HTTPRequest req1 = new HTTPRequest(url, HTTPRequest.POST);
					if (url.contains("REALSearchByBookPage.aspx")){
						req1.setPostParameter("SEARCH_INSTRUMENT_NUMBER", "");
						req1.setPostParameter("SEARCH_BOOK", "");
						req1.setPostParameter("SEARCH_PAGE", "");
						req1.addPostParameters(addParams);
						req1.setPostParameter("__EVENTTARGET", "lbSearchType");
					} else if (url.contains("REALSearchByName.aspx")){
						req1.setPostParameter("LNAME", "");
						req1.setPostParameter("FNAME", "");
						req1.setPostParameter("NameType", "RBTN_ALL");
						req1.addPostParameters(addParams);
						req1.setPostParameter("__EVENTTARGET", "lbSearchType");
					}
					String resp = execute(req1);
					addParams = isolateParams(resp, "FORM_CRITERIA");
					if (checkParams(addParams)){
						setAttribute("params", addParams);
					}
				}
				req.removePostParameters("TYPE");
				//req.setURL(req.getURL() + "?TYPE=ADVANCED");
				addParams = (HashMap<String,String>)getAttribute("params");
				req.addPostParameters(addParams);
			}
		}
    	
    	return;
	}	
	
	public String getPage(String src) {
		
		String link = getSiteLink() + src;
		
		HTTPRequest req = new HTTPRequest(link);
		
		return execute(req);
	}

	public byte[] getImage(String lnk){
		String link = getSiteLink() + lnk;
		
		String imageLink = link.substring(0, link.indexOf("/iRecord")) + lnk.substring(lnk.indexOf("/iRecord"), lnk.length());
		HTTPResponse res = null;
		
		try {
			HTTPRequest req = new HTTPRequest(imageLink, HTTPRequest.GET);
			res = process(req);
			
		} catch (Exception e) {
			e.printStackTrace();
			return null;
		}
		return res.getResponseAsByte();
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
			destroySession();    		
		}	
	}
	
}