package ro.cst.tsearch.connection.http3;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Vector;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.io.IOUtils;
import org.apache.http.cookie.Cookie;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.impl.cookie.BasicClientCookie;
import org.htmlparser.filters.HasAttributeFilter;
import org.htmlparser.filters.TagNameFilter;
import org.htmlparser.tags.InputTag;
import org.htmlparser.tags.LinkTag;
import org.htmlparser.tags.ScriptTag;
import org.htmlparser.util.NodeList;

import ro.cst.tsearch.connection.ATSConnConstants;
import ro.cst.tsearch.connection.LoginResponse;
import ro.cst.tsearch.connection.LoginResponse.LoginStatus;
import ro.cst.tsearch.connection.http.HTTPRequest;
import ro.cst.tsearch.connection.http.HTTPResponse;
import ro.cst.tsearch.data.SitesPasswords;
import ro.cst.tsearch.parser.HtmlParser3;
import ro.cst.tsearch.parser.SimpleHtmlParser;
import ro.cst.tsearch.servers.bean.DataSite;
import ro.cst.tsearch.utils.StringUtils;


public class OHCuyahogaPR extends HttpSite3 {

	//"__LASTFOCUS", 
	private static final String[] ADD_PARAM_NAMES = { "__VIEWSTATE", "__EVENTTARGET", "__EVENTARGUMENT", "__EVENTVALIDATION"};
	private static final String[] REQ_PARAM_NAMES = { "__VIEWSTATE", "__EVENTVALIDATION" };
	
	public static final Pattern FORM_PATTERN = Pattern.compile("<form(.*?)name=\"formQuery\"\\s+action=\"([^\"]+)\"\\s*>");
	private static final Pattern VIEWSTATE_PAT = Pattern.compile("(?is)__VIEWSTATE\\s*\\|([^\\|]+)");
	private static final Pattern EVENTVALIDATION_PAT = Pattern.compile("(?is)__EVENTVALIDATION\\s*\\|([^\\|]+)");
	
	private String btnAccept 		= "ctl00$mpContentPH$btnYes";
	private String searchByCase		= "ctl00$mpContentPH$btnSearchByCase";
	private String searchByParty	= "ctl00$mpContentPH$btnSearchByPerson";
	private String ajax_HiddenField	= "";
	
	private static Map<String,String> parameters;
	static {
		parameters = new HashMap<String,String>();
		
	}
	
	public String[] getTargetArgumentParameters() {
		return REQ_PARAM_NAMES;
	}
	
	public String getPage(String link, Map<String, String> params) {
		HTTPRequest req = null;
		if (params == null){
			req = new HTTPRequest(link, HTTPRequest.GET);
		} else {
			req = new HTTPRequest(link, HTTPRequest.POST);
			Iterator<String> i = params.keySet().iterator();
			while(i.hasNext()){
				String k = i.next();
				req.setPostParameter(k, params.get(k));
			}
		}
		
		return super.execute(req);
	}
	
	public String getNextDetailsPage(String link, Map<String, String> params) {
		HTTPRequest req = null;
		req = new HTTPRequest(link, HTTPRequest.POST);
		Iterator<String> i = params.keySet().iterator();
		while(i.hasNext()){
			String k = i.next();
			req.setPostParameter(k, params.get(k));
		}
		
		return super.execute(req);
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
	
//	public static Map<String, String> getReqParams(String page, String form) {
//		Map<String, String> params = new SimpleHtmlParser(page).getForm(form).getParams();
//		Map<String,String> addParams = new HashMap<String,String>();
//		
//		for (String key : REQ_PARAM_NAMES) {
//			String value = "";
//			if (params.containsKey(key)) {
//				value = params.get(key);
//			}
//			addParams.put(key, value);
//		}
//		
//		return addParams;
//	}
	
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
	
	
	public String getHiddenParam(NodeList nodeList)  {
		String hiddenVal = "";
		String hiddenKey = "";
		
		if (nodeList!= null && nodeList.size() > 5) {
			ScriptTag script = (ScriptTag) nodeList.elementAt(4);			
			
			if (script != null) {
				Pattern p = Pattern.compile("(?is)[^?]+\\?_TSM_HiddenField_=([^&]+)[^_]+_TSM_CombinedScripts_=([^\"]+)");
				Matcher m = p.matcher(script.getAttribute("src"));
				
				if (m.find()) {
					hiddenKey = m.group(1);
					if ("ajax_HiddenField".equals(hiddenKey)) {
						hiddenVal = m.group(2);
						try {
							hiddenVal = URLDecoder.decode(hiddenVal, "UTF-8");
						} catch (UnsupportedEncodingException e) {							
							e.printStackTrace();
							logger.trace(e);
						}
					}
				}
			}
		}
		
		return hiddenVal;
	}

	
	
	@Override
	public LoginResponse onLogin() {
//		Protocol easyhttps = new Protocol("https", new EasySSLProtocolSocketFactory(), 443);
//      Protocol.registerProtocol("https", easyhttps);
		
		/**
		 * Get first page and check if we are on login page
		 * 01. ---------------------------------------------------------------------------------------------------------
		 */
		HTTPRequest request = new HTTPRequest(getSiteLink());
		request.setHeader("User-Agent", getUserAgentValue());
		request.setHeader("Accept", getAccept());
		request.setHeader("Accept-Language", getAcceptLanguage());
		request.setHeader("Accept-Encoding", "gzip, deflate");
		request.setHeader("Accept-Charset", "ISO-8859-1,utf-8;q=0.7,*;q=0.7");
		
		String responseAsString = execute(request);
		String redirectURL = request.getRedirectLocation();
		Pattern titlePattern = Pattern.compile("(?is)<title>(.*?)</title>");
		
		if(responseAsString == null) {
			return new LoginResponse(LoginStatus.STATUS_INVALID_PAGE, "No respose for " + getSiteLink());
		}
		
		Matcher matcher = titlePattern.matcher(responseAsString);
		Map<String,String> addParams = null;
		Map<String,String> cookie = new HashMap<String, String>();
		
		if(matcher.find()) {
			String title = matcher.group(1);
			if(title.contains("Web Docket")) {
				//isolate parameters
//				addParams = isolateParams(responseAsString, "form1");
				addParams = isolateParams(responseAsString, "form1", ADD_PARAM_NAMES);
			} else {
				return new LoginResponse(LoginStatus.STATUS_INVALID_PAGE, "Unexpected page received with title " + title);
			}
			
		} else {
			return new LoginResponse(LoginStatus.STATUS_INVALID_PAGE, "Unexpected page received with no title ");
		}
		
		/**
		 * GET /pa/TOS.aspx
		 * 02. ---------------------------------------------------------------------------------------------------------
		 */
		if (!redirectURL.contains("www")) {
			redirectURL = getSiteLink().substring(0, getSiteLink().lastIndexOf("/")) + redirectURL.substring(redirectURL.lastIndexOf("/"));
		}
		request.modifyURL(redirectURL);
		request.setMethod(HTTPRequest.POST);
		request.addPostParameters(addParams);
		request.setPostParameter(btnAccept, "Yes");
		request.setPostParameter("ajax_HiddenField", ajax_HiddenField);
		
		responseAsString = execute(request);
		if(responseAsString == null) {
			return new LoginResponse(LoginStatus.STATUS_INVALID_PAGE, "No respose for TOS.aspx");
		}
		
		if(responseAsString.contains("Search by Case")) {
			NodeList scripts = new HtmlParser3(responseAsString).getNodeList().extractAllNodesThatMatch(new TagNameFilter("form"), true)
					.extractAllNodesThatMatch(new HasAttributeFilter("id", "form1"))
					.extractAllNodesThatMatch(new TagNameFilter("script"), true);
			
			if (scripts != null) {
				ajax_HiddenField = getHiddenParam(scripts);
			}
			
//			addParams = isolateParams(responseAsString, "form1");
			addParams = isolateParams(responseAsString, "form1", ADD_PARAM_NAMES);
			
			// check parameters
			if (!checkParams(addParams)){
				return new LoginResponse(LoginStatus.STATUS_INVALID_PAGE, "Invalid parameters");
			}
			
			addParams.put("ajax_HiddenField", ajax_HiddenField);
			cookie.put(getCookies().get(0).getName(), getCookies().get(0).getValue());
			cookie.put(getCookies().get(1).getName(), getCookies().get(1).getValue());
			setAttribute("params", addParams);
			setAttribute("cookie", cookie);
			
			return LoginResponse.getDefaultSuccessResponse();
			
		} else {
			return new LoginResponse(LoginStatus.STATUS_INVALID_PAGE, "Could not get login page!");
		}
	}
	
	@Override
	public void onBeforeRequestExcl(HTTPRequest req) {
		Map<String,String> addParams = (HashMap<String,String>)getAttribute("params");
		Map<String,String> cookies = (HashMap<String,String>)getAttribute("cookie");
		String resp = "";
		
		if (req.getPostFirstParameter(searchByCase) != null) {
			HTTPRequest req1 = new HTTPRequest(req.getURL(), HTTPRequest.POST);
			req1.addPostParameters(addParams);
					
			for(Map.Entry<String, String> entry: cookies.entrySet()) {
				req.setHeader(entry.getKey(), entry.getValue());
				req1.setHeader(entry.getKey(), entry.getValue());
			}
			for(Map.Entry<String, HTTPRequest.ParametersVector> entry: req.getPostParameters().entrySet()) {
				if (!searchByCase.equals(entry.getKey())) 
					req1.setPostParameter(entry.getKey(), entry.getValue().toString());
			}
			req1.removePostParameters("__EVENTTARGET");
			req1.setPostParameter("__EVENTTARGET", "ctl00$mpContentPH$txtCaseNum");
			req1.setPostParameter("__LASTFOCUS", "");
			req1.setPostParameter("__ASYNCPOST", "true");
					
			resp = execute(req1);
			Matcher ev = EVENTVALIDATION_PAT.matcher(resp);
			if (ev.find()){
				addParams.remove("__EVENTVALIDATION");
				addParams.put("__EVENTVALIDATION", ev.group(1));
			}
			Matcher vs = VIEWSTATE_PAT.matcher(resp);
				if (vs.find()){
				addParams.remove("__VIEWSTATE");
				addParams.put("__VIEWSTATE", vs.group(1));
			}
				
			req.addPostParameters(addParams);
		
		} else if (req.getPostFirstParameter(searchByParty) != null) {
			HTTPRequest req1 = null;
			
			if ("C".equals(req.getPostFirstParameter("ctl00$mpContentPH$rblPersonCo"))) {
				req1 = new HTTPRequest(req.getURL(), HTTPRequest.POST);
				req1.addPostParameters(addParams);
						
				for(Map.Entry<String, String> entry: cookies.entrySet()) {
					req.setHeader(entry.getKey(), entry.getValue());
					req1.setHeader(entry.getKey(), entry.getValue());
				}
				for(Map.Entry<String, HTTPRequest.ParametersVector> entry: req.getPostParameters().entrySet()) {
					if (!searchByParty.equals(entry.getKey())) 
						req1.setPostParameter(entry.getKey(), entry.getValue().toString());
				}
				
				req1.setPostParameter("__LASTFOCUS", "");
				req1.setPostParameter("__ASYNCPOST", "true");
				req1.removePostParameters("__EVENTTARGET");
				req1.setPostParameter("__EVENTTARGET", "ctl00$mpContentPH$rblPersonCo$1");
				req1.setPostParameter("ctl00$ajax", "ctl00$mpContentPH$upPPLName|ctl00$mpContentPH$rblPersonCo$1");
				req1.removePostParameters("ctl00$mpContentPH$txtLName");
				req1.setPostParameter("ctl00$mpContentPH$txtLName", "");
				req1.removePostParameters("ctl00$mpContentPH$txtFName");
				req1.setPostParameter("ctl00$mpContentPH$txtFName", "");
				req1.removePostParameters("ctl00$mpContentPH$txtMName");
				req1.setPostParameter("ctl00$mpContentPH$txtMName", "");
				
				req.removePostParameters("ctl00$mpContentPH$txtFName");
				req.removePostParameters("ctl00$mpContentPH$txtMName");
			
			} else if ("P".equals(req.getPostFirstParameter("ctl00$mpContentPH$rblPersonCo"))) {
				req1 = new HTTPRequest(req.getURL(), HTTPRequest.GET);
				
				req.removePostParameters("__EVENTTARGET");
				req.setPostParameter("__EVENTTARGET", "");
			}
			
			resp = execute(req1);
			
			if ("C".equals(req.getPostFirstParameter("ctl00$mpContentPH$rblPersonCo"))) {
				Matcher ev = EVENTVALIDATION_PAT.matcher(resp);
				if (ev.find()){
					addParams.remove("__EVENTVALIDATION");
					addParams.put("__EVENTVALIDATION", ev.group(1));
				}
				Matcher vs = VIEWSTATE_PAT.matcher(resp);
				if (vs.find()){
					addParams.remove("__VIEWSTATE");
					addParams.put("__VIEWSTATE", vs.group(1));
				}
			
			} else if ("P".equals(req.getPostFirstParameter("ctl00$mpContentPH$rblPersonCo"))) {
//				Map<String,String> reqParams = getReqParams(resp, "form1");
				Map<String,String> reqParams = isolateParams(resp, "form1", REQ_PARAM_NAMES);
				addParams.remove("__EVENTVALIDATION");
				addParams.remove("__VIEWSTATE");
				
				for (String key : REQ_PARAM_NAMES) {
					String value = "";
					if (reqParams.containsKey(key)) {
						value = reqParams.get(key);
					}
					addParams.put(key, value);
				}
			}
			
			addParams.put("__LASTFOCUS", "");
			req.addPostParameters(addParams);
		
		} else if (req.getURL().contains("SearchResults.aspx")) {
			String seq = req.getPostFirstParameter("seq");;
			addParams = (HashMap<String,String>)getTransientSearchAttribute("params:" + seq);
			
			if (addParams != null){
				req.removePostParameters("seq");
				//req.removePostParameters("__EVENTTARGET");
				for(Map.Entry<String, String> entry: addParams.entrySet()) {
					req.setPostParameter(entry.getKey(), entry.getValue());
				}
				for(Map.Entry<String, String> entry: cookies.entrySet()) {
					req.setHeader(entry.getKey(), entry.getValue());
				}
				
			}
		
		} else if (req.getURL().contains("CaseCosts.aspx") || req.getURL().contains("CaseDocket.aspx") 
				|| req.getURL().contains("CaseEvents.aspx") || req.getURL().contains("CaseParties.aspx")
				|| req.getURL().contains("CaseRequirements.aspx") || req.getURL().contains("CaseServices.aspx")) {
			// gather the rest of details info
			for(Map.Entry<String, String> entry: cookies.entrySet()) {
				req.setHeader(entry.getKey(), entry.getValue());
			}
		}

	}
	

	public String getFormLink(HtmlParser3 parser) {
		NodeList allLinks = parser.getNodeList().extractAllNodesThatMatch(new TagNameFilter("a"), true)
				.extractAllNodesThatMatch(new HasAttributeFilter("href", "#"))
				.extractAllNodesThatMatch(new HasAttributeFilter("class", "LibTabLink"));
		
		LinkTag formSearchLink = new LinkTag();
		
		for (int i = 0; i < allLinks.size(); i++) {
			LinkTag tempLink = (LinkTag) allLinks.elementAt(i);
			if("Search".equals(tempLink.getLinkText())) {
				formSearchLink = tempLink;
				break;
			}
		}
		
		String onClick = formSearchLink.getAttribute("onClick");
		
		Pattern pattern = Pattern.compile("TabButtonHandler\\((\\d+),'http://10.10.50.10/LibertyIMS::([^']+)'\\)");
		Matcher matcher = pattern.matcher(onClick);
		
		String newLink = null;
		
		if(matcher.find()) {
			newLink = "https://vpn3030.insnoc.com/http/0/10.10.50.10/LibertyIMS::" + matcher.group(2) + "cmd=XMLExecTBButton;ID=" + matcher.group(1);
		}
		return newLink;
	}
	

	@Override
	public String getUserAgentValue() {
		return "Mozilla/5.0 (Windows NT 5.1; rv:22.0) Gecko/20100101 Firefox/22.0";
	}
	
	@Override
	public String getAcceptLanguage() {
		return "en-US,en;q=0.5";
	}
	
}

