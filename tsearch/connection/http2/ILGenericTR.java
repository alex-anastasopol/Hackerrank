/**	
 * 
 */
package ro.cst.tsearch.connection.http2;

import java.io.ByteArrayInputStream;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import ro.cst.tsearch.connection.LoginResponse;
import ro.cst.tsearch.connection.LoginResponse.LoginStatus;
import ro.cst.tsearch.connection.http.HTTPRequest;
import ro.cst.tsearch.connection.http.HTTPRequest.ParametersVector;
import ro.cst.tsearch.connection.http.HTTPResponse;
import ro.cst.tsearch.parser.SimpleHtmlParser;
import ro.cst.tsearch.utils.StringUtils;

/**
 *  ADD here the new county implemented with this Generic
 * 
 * Generic class ILMcHenryTR, ILKendallTR.
 * 
 * @author mihaib
  */
public class ILGenericTR extends HttpSite {
	
	private static final String[] ADD_PARAM_NAMES = { "__LASTFOCUS", "__VIEWSTATE", "__EVENTTARGET", "__EVENTARGUMENT", "__EVENTVALIDATION",};
	private static final String[] REQ_PARAM_NAMES = { "__VIEWSTATE", "__EVENTVALIDATION" };
	public String cookie="";
	
	@Override
	public LoginResponse onLogin() {

		String link = getSiteLink();
		String resp = "";
		HTTPRequest req = new HTTPRequest(link, HTTPRequest.GET);
		HTTPResponse res = process(req);
		this.cookie=res.getHeader("Set-Cookie");
		//resp = execute(req);
		resp = res.getResponseAsString();
		
		//isolate parameters
		Map<String,String> addParams = isolateParams(resp, "aspnetForm");
		
		// check parameters
		if (!checkParams(addParams)){
			return new LoginResponse(LoginStatus.STATUS_INVALID_PAGE, "Invalid parameters");
		}
		setAttribute("params", addParams);
		
		return new LoginResponse(LoginStatus.STATUS_SUCCESS, "Logged in");
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
		
		Map<String,String> addParams = null;
		String link = getSiteLink();
		String resp = "";
		
		if (req.getPostFirstParameter("ctl00$ContentPlaceHolder1$hidMenuItem") != null) {
			// Performed a search. Retain search params.
			removeAttribute("searchParams");
			setAttribute("searchParams", req.getPostParameters());
		}
		if (req.getPostFirstParameter("link") != null) {
			// Navigation required. Perform initial search again. (Bug 8145)
			HTTPRequest req1 = new HTTPRequest(link);
			req1.setMethod(HTTPRequest.POST);
			req1.setPostParameters((HashMap<String, ParametersVector>) getAttribute("searchParams"));
			req1.setHeader("Cookie", this.cookie);
			resp = execute(req1);
		}
		if ("2".equals(req.getPostFirstParameter("ctl00$ContentPlaceHolder1$hidMenuItem"))) { // cautare dupa adresa
			HTTPRequest req1 = new HTTPRequest(link);
			req1.setMethod(HTTPRequest.POST);
			addParams = (HashMap<String,String>)getAttribute("params");
			
			//isolate parameters 
			req1 = addParams(addParams, req1);
			req1.setHeader("Cookie", this.cookie);
			req1.setPostParameter("ctl00$ContentPlaceHolder1$hidMenuItem", "2");
			resp = execute(req1);
			//isolate parameters
			addParams = isolateParams(resp, "aspnetForm");
			
			setAttribute("params", addParams);
		}
		
		addParams = (HashMap<String,String>)getAttribute("params");
		if(req.getPostParameter("ctl00$ContentPlaceHolder1$txtParcelNoFrom") != null){
			if(!"".equals(req.getPostParameter("ctl00$ContentPlaceHolder1$txtParcelNoFrom"))){
				for(Map.Entry<String, String> entry: addParams.entrySet()){
					req.setPostParameter(entry.getKey(), entry.getValue());
				}
			}
		}
		
		if(req.getPostParameter("Parcel") != null){ //details link
			// remove the seq parameter
			String select = req.getPostFirstParameter("select");
			req.removePostParameters("select");
			req.removePostParameters("Parcel");
			
			// additional parameters are stored inside the search since they are too long
			addParams = (Map<String, String>)getTransientSearchAttribute("paramsDetails:");			
			String url = req.getURL();
			url = StringUtils.urlDecode(req.getURL());
			url = url.replaceAll("&amp;", "&");
			req.modifyURL(url);
			req.setPostParameter("__EVENTARGUMENT", select);
			for(Map.Entry<String, String> entry: addParams.entrySet()){
				req.setPostParameter(entry.getKey(), entry.getValue());
			}
			return;
		} else if (req.getPostParameter("link") != null){
			
			// additional parameters are stored inside the search since they are too long
			addParams = (Map<String, String>)getTransientSearchAttribute("paramsForNavigation:");			
			String url = req.getURL();
			url = StringUtils.urlDecode(req.getURL());
			url = url.replaceAll("&amp;", "&");
			req.modifyURL(url);
			String ctlFirst = null, ctlPrev = null, ctlNext = null, ctlLast = null;
		    
			for(Map.Entry<String, String> entry: addParams.entrySet()){
		      if (entry.getKey().contains("btnFirst")) {
		    	  ctlFirst = entry.getKey();
		      }  else if (entry.getKey().contains("btnPrev")) {
		    	  ctlPrev = entry.getKey();
		      } else if (entry.getKey().contains("btnNext")) {
		    	  ctlNext = entry.getKey();
		      } else if (entry.getKey().contains("btnLast")) {
		    	  ctlLast = entry.getKey();
		      }
		    }
			for(Map.Entry<String, String> entry: addParams.entrySet()){
				req.setPostParameter(entry.getKey(), entry.getValue());
			}
			if ("Previous".equals(req.getPostFirstParameter("link"))) {
				req.removePostParameters(ctlNext);
				req.removePostParameters(ctlFirst);
				req.removePostParameters(ctlLast);
			} else if ("Next".equals(req.getPostFirstParameter("link"))){
				req.removePostParameters(ctlPrev);
				req.removePostParameters(ctlFirst);
				req.removePostParameters(ctlLast);
			} else if ("First".equals(req.getPostFirstParameter("link"))){
				req.removePostParameters(ctlPrev);
				req.removePostParameters(ctlNext);
				req.removePostParameters(ctlLast);
			} else if ("Last".equals(req.getPostFirstParameter("link"))){
				req.removePostParameters(ctlPrev);
				req.removePostParameters(ctlFirst);
				req.removePostParameters(ctlNext);
			}
			req.removePostParameters("link");
			return;
		}
		
		if (req.getPostParameter("ctl00$ddYears") == null && req.getPostParameter("ctl00$ContentPlaceHolder1$SearchResultsSum1$gvSearchResults$ctl18$ddlPages") == null){	
			if(addParams != null) {			
				for (String key : ADD_PARAM_NAMES) {
					String value = "";
					if (addParams.containsKey(key)) {
						value = addParams.get(key);
					}
					req.setPostParameter(key, value);
				}			
			}
		} 
	}
	
	@Override	
	public void onAfterRequest(HTTPRequest req, HTTPResponse res) {
		String content = "";
		if (res != null) {
			if (res.getContentType().contains("text/html")){
				content = res.getResponseAsString();
				res.is = new ByteArrayInputStream(content.getBytes());
				if (content.indexOf("Invalid postback or callback argument") != -1){
					destroySession();
				}
			}
		}
	}
}