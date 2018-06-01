package ro.cst.tsearch.connection.http2;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;

import ro.cst.tsearch.connection.LoginResponse;
import ro.cst.tsearch.connection.LoginResponse.LoginStatus;
import ro.cst.tsearch.connection.http.HTTPRequest;
import ro.cst.tsearch.connection.http.HTTPResponse;
import ro.cst.tsearch.utils.StringUtils;

public class FLHillsboroughRO extends HttpSite {

	
	public LoginResponse onLogin() {
		
		// get the search page
		HTTPRequest req = new HTTPRequest("http://pubrec3.hillsclerk.com/oncore/search.aspx");
		HTTPResponse res = process(req);
		String response = res.getResponseAsString();

		// obtain the viewstate
		String viewState = StringUtils.extractParameter(response, "<input type=\"hidden\" name=\"__VIEWSTATE\" value=\"([^\"]+)\"");
		setAttribute("viewState", viewState);
		if(StringUtils.isEmpty(viewState)) {
			return new LoginResponse(LoginStatus.STATUS_INVALID_PAGE, "Invalid parameters");
		}
		return new LoginResponse(LoginStatus.STATUS_SUCCESS, "Logged in");

	}

	@Override
	public void onBeforeRequestExcl(HTTPRequest req) {
		
		// fix the url : the engine sometimes replaces more & with ?
		// leave first ? alone, then replace the others with &
		String url = req.getURL();	
		url = url.replaceAll("&amp;(amp;)*","&");
		int idx = url.indexOf("?");
		if(idx != -1 && url.length() > 2){
			url = url.substring(0, idx) + "?" + (url.substring(idx+1).replace('?','&'));			
		}
		url = url.replace("&parentSite=true", "");
		req.modifyURL(url);
		
		// treat initial search queries: create corresponding GET link
		String searchType = req.getPostFirstParameter("SearchType");		
		if(searchType != null){
			
			String bd  = StringUtils.urlEncode(req.getPostFirstParameter("txtBeginDate"));
			String ed  = StringUtils.urlEncode(req.getPostFirstParameter("txtEndDate"));
			String n   = StringUtils.urlEncode(req.getPostFirstParameter("txtName"));
			String bt  = StringUtils.urlEncode(req.getPostFirstParameter("ddBookType"));
			String d   = StringUtils.urlEncode(req.getPostFirstParameter("txtRecordDate"));
			String pt  = StringUtils.urlEncode(req.getPostFirstParameter("ddPartyType"));
			String st  = StringUtils.urlEncode(req.getPostFirstParameter("SearchType"));		
			String b   = StringUtils.urlEncode(req.getPostFirstParameter("txtBook"));
			String p   = StringUtils.urlEncode(req.getPostFirstParameter("txtPage"));
			String cfn = StringUtils.urlEncode(req.getPostFirstParameter("txtInstrumentNumber"));
			String cc  = StringUtils.urlEncode(req.getPostFirstParameter("ddCommentsChoice"));
			String cmt = StringUtils.urlEncode(req.getPostFirstParameter("txtComments"));
			String dt  = StringUtils.urlEncode(req.getPostFirstParameter("txtDocTypes"));
			
			String query = "";			
			if ("fullname".equals(searchType)) {	
				query = "bd=" + bd + "&ed=" + ed + "&n=" + n + "&bt=" + bt + "&d=" + d + "&dt=" + dt + "&pt=" + pt + "&st=" + st;
			}else if("bookpage".equals(searchType)) {	
				query = "bd=" + bd + "&ed=" + ed + "&bt=" + bt + "&b=" + b + "&p=" + p + "&d=" + d + "&pt=" + pt + "&st=" + st;
			}else if("instrument".equals(searchType)){
				query = "bd=" + bd + "&ed=" + ed + "&bt=" + bt + "&cfn=" + cfn + "&d=" + d + "&pt=" + pt + "&st=" + st;
			}else if("img".equals(searchType)){
				if(StringUtils.isEmpty(b)||StringUtils.isEmpty(p)){//we will make an instrument search
					query = "bd=" + bd + "&ed=" + ed + "&bt=" + bt + "&cfn=" + cfn + "&d=" + d + "&pt=" + pt + "&st=" + "instrument";
				}
				else{//we will make a book page search
					query = "bd=" + bd + "&ed=" + ed + "&bt=" + bt + "&b=" + b + "&p=" + p + "&d=" + d + "&pt=" + pt + "&st=" + "bookpage";
				}
			} else if("comments".equals(searchType)){
				query = "bd=" + bd + "&ed=" + ed + "&bt=" + bt + "&d=" + d + "&cc=" + cc + "&pt=" + pt + "&cmt=" + cmt + "&dt=" + dt + "&st=" + st;
			}
			
			req.modifyURL("http://pubrec3.hillsclerk.com/oncore/search.aspx?" + query);
			req.removePostParameters("__VIEWSTATE");
			req.setPostParameter("__VIEWSTATE", (String)getAttribute("viewState"));
		}
		
		// treat POST + GET links
		idx = url.indexOf("&postParams=true");
		if(idx!= -1){
			
			req.modifyURL(url.substring(0, idx));
			req.setMethod(HTTPRequest.POST);
						
			// extract params
			String params [] = url.substring(idx + "&postParams=true&".length()).split("&");
			for(String param: params){
				int idx2 = param.indexOf("=");
				try{
					String name = URLDecoder.decode(param.substring(0, idx2), "UTF-8");
					String value = URLDecoder.decode(param.substring(idx2+1), "UTF-8");
					req.setPostParameter(name, value);
				}catch(UnsupportedEncodingException e){
					throw new RuntimeException(e);
				}			
			}
			
			// use either the viewstate for next results either the one for crossreferences			
			String pageType = req.getPostFirstParameter("pageType");
			if(pageType != null){
				req.removePostParameters("pageType");
				if("next".equals(pageType)){
					//String viewState2 = (String) search.getAdditionalInfo("FLHillsboroughRO-next-viewstate");
					String viewState2 = (String)getTransientSearchAttribute("next-viewstate");
					req.setPostParameter("__VIEWSTATE", viewState2);
				} else if("cref".equals(pageType)){
					//String viewState2 = (String) search.getAdditionalInfo("FLHillsboroughRO-cref-viewstate");
					String viewState2 = (String)getTransientSearchAttribute("cref-viewstate");
					req.setPostParameter("__VIEWSTATE", viewState2);
					req.removePostParameters("bp");
				}
			}
		}
		
		req.removePostParameters("crossRefSource");
		req.removePostParameters("isSubResult");
	}
}
