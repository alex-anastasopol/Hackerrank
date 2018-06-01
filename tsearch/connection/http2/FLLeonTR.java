package ro.cst.tsearch.connection.http2;

import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang.StringUtils;

import ro.cst.tsearch.connection.LoginResponse;
import ro.cst.tsearch.connection.LoginResponse.LoginStatus;
import ro.cst.tsearch.connection.http.HTTPRequest;
import ro.cst.tsearch.parser.SimpleHtmlParser;
import ro.cst.tsearch.parser.SimpleHtmlParser.Form;
import ro.cst.tsearch.parser.SimpleHtmlParser.Input;

public class FLLeonTR extends HttpSite {

	private static final String[]		ADD_PARAM_NAMES	= { "__LASTFOCUS", "__VIEWSTATE", "__EVENTTARGET", "__EVENTARGUMENT", "__EVENTVALIDATION" };
	private static final String[]		REQ_PARAM_NAMES	= { "__VIEWSTATE", "__EVENTVALIDATION" };

	public static String FORM_NAME = "frmDisclaimer";
	
	@Override
	public LoginResponse onLogin() {
		String link = getCrtServerLink();
		
		String response = execute(new HTTPRequest(link));
		
		if (response.toLowerCase().contains("disclaimer page")){
			SimpleHtmlParser html = new SimpleHtmlParser(response);
			Form form = html.getForm(FORM_NAME);
			
			if (form != null){
				String action = form.action;
				
				HTTPRequest req = new HTTPRequest(link.replaceFirst("\\w+\\.aspx$", action), HTTPRequest.POST);
				
				List<Input> inputs = form.inputs;
				if (inputs != null){
					for (Input input : inputs) {
						if (!input.name.toLowerCase().contains("cancel")){
							req.setPostParameter(input.name, input.value);
						}
					}
					response = execute(req);
					
					//isolate parameters
					Map<String,String> addParams = isolateParams(response, "aspnetForm");
					
					// check parameters
					if (!checkParams(addParams)){
						return new LoginResponse(LoginStatus.STATUS_INVALID_PAGE, "Invalid parameters");
					}
					setAttribute("params", addParams);
					
					return new LoginResponse(LoginStatus.STATUS_SUCCESS, "Logged in");
					
				} else{
					return new LoginResponse(LoginStatus.STATUS_INVALID_PAGE, "Disclaimer page form parameters not found");
				}
			} else{
				return new LoginResponse(LoginStatus.STATUS_INVALID_PAGE, "Disclaimer page form not found");
			}
		} else {
			return new LoginResponse(LoginStatus.STATUS_INVALID_PAGE, "Disclaimer page not found");
		}
	}
	
	@Override
	public void onBeforeRequestExcl(HTTPRequest req) {
		if (req.getPostParameter("ats_link_attributes_key") != null) {
			makeRequestWithPost(req);

		} else {
			String searchType = req.getPostFirstParameter("SearchType");
			if (searchType != null) {
				if (ro.cst.tsearch.utils.StringUtils.isNotEmpty(searchType)){
					req.removePostParameters("SearchType");
					
					Map<String,String> addParams = null;
					
					String response = execute(new HTTPRequest(req.getURL(), HTTPRequest.GET));
					addParams = isolateParams(response, "aspnetForm");
					
					if (addParams != null){
						if (checkParams(addParams)){
							for(Map.Entry<String, String> entry: addParams.entrySet()){
								req.setPostParameter(entry.getKey(), entry.getValue());
							}
						}
					}
				}
			} 
		}
	}

	@SuppressWarnings("unchecked")
	private void makeRequestWithPost(HTTPRequest req) {
		String lnk = req.getPostFirstParameter("ats_link_attributes_key");
		req.removePostParameters("ats_link_attributes_key");
		Map<String, String> addParams = (Map<String, String>) getTransientSearchAttribute("params:" + lnk);
		String qryString = ((String) getTransientSearchAttribute("request_querry:" + lnk)).replaceAll(" ", "+");
		if (qryString.endsWith("Exact=")) {
			qryString = qryString.replace("Exact=", "");
		} else {
			qryString = qryString.replace("Exact=&", "");
		}
		
		String newLink = req.getURL() + "?" + qryString;
		req.modifyURL(newLink);

		Object[] keys = req.getPostParameters().keySet().toArray();
		String targetKey =  "__EVENTTARGET";
		addParams.put(targetKey, req.getPostParameter(targetKey).getFirstParameter());
		for (Object string : keys) {
			req.getPostParameters().remove(string);
		}
		req.addPostParameters(addParams);


	}

	// https://www.leontaxcollector.net/itm/PropertyDetails.aspx?Acctno=+++++++++++++++243025++D0040&Acctyear=2003&Acctbtyear=&Owner=SMITH+MARY&Page=7
	public String getCurrentAccountDetails(String yearLink) {
		String link = getCrtServerLink().substring(0, getCrtServerLink().indexOf("PropertySearch"));
		link = link + yearLink.replaceAll("&amp;", "&");
		HTTPRequest req = new HTTPRequest(link);
		return execute(req);
	}

	private String getCrtServerLink() {
		return getSiteLink();
	}

	public String getAppraiserPage(String link) {
		HTTPRequest req = new HTTPRequest(link);
		return execute(req);
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

}
