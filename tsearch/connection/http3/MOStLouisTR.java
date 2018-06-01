package ro.cst.tsearch.connection.http3;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.GZIPInputStream;

import org.apache.commons.io.IOUtils;
import org.htmlparser.filters.HasAttributeFilter;
import org.htmlparser.filters.TagNameFilter;
import org.htmlparser.util.NodeList;

import ro.cst.tsearch.connection.LoginResponse;
import ro.cst.tsearch.connection.LoginResponse.LoginStatus;
import ro.cst.tsearch.connection.http.HTTPRequest;
import ro.cst.tsearch.connection.http.HTTPResponse;
import ro.cst.tsearch.parser.HtmlParser3;
import ro.cst.tsearch.parser.SimpleHtmlParser;

public class MOStLouisTR extends HttpSite3 {
	
	public static final String[] ADD_PARAM_NAMES = { "__VIEWSTATE", "__EVENTTARGET", "__EVENTARGUMENT", "__PREVIOUSPAGE", "__EVENTVALIDATION"};
	public static final String[] REQ_PARAM_NAMES = { "__VIEWSTATE", "__PREVIOUSPAGE", "__EVENTVALIDATION" };
	
	public static final Pattern FORM_PATTERN = Pattern.compile("(?is)<form name=\\\"SearchResults\\\"\\s+.*?action=\\\"([^\\\"]+)\\\"\\s*.*>");
	
	private String formID1 = "formRev";
	private String formID2 = "SearchResults";
	
	private static String searchByLocatorNo		= "rbutLocatorNum";
	private static String searchByName 			= "rbutName";
	private static String searchByAddress	 	= "rbutAddress";
	private static String searchBySubdiv		= "rbutSubdivision";
	
	
	public static String[] getTargetArgumentParameters() {
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
	
	public static Map<String, String> getHiddenParams(String page, String form) {
		Map<String,String> hiddenParams = new HashMap<String,String>();
		NodeList hiddenInputs = new HtmlParser3(page).getNodeList();
		
		hiddenInputs = hiddenInputs.extractAllNodesThatMatch(new TagNameFilter("form"), true)
				.extractAllNodesThatMatch(new HasAttributeFilter("id", form));
		if (hiddenInputs != null) {
			hiddenInputs = hiddenInputs.elementAt(0).getChildren().extractAllNodesThatMatch(new TagNameFilter("input"));
			if (hiddenInputs.size() > 0) {
				for (int i=0; i< hiddenInputs.size(); i++) {
					String inp = hiddenInputs.elementAt(i).getText();
					Matcher matcher = Pattern.compile("(?is)input.*?name=\\\"([^\\\"]+).*?(?:value=\\\"([^\\\"]*)\\\")?\\s*/").matcher(inp);
					if (matcher.find()) {
						String key = matcher.group(1);
						String val = "";
						if (inp.contains("value")) {
							val = matcher.group(2);
						}
						hiddenParams.put(key, val);
					}
				}
			}
		}
		
		return hiddenParams;
	}
	
	@Override
	public LoginResponse onLogin() {
		/**
		 * 01. GET page with default search module:/IAS/SearchInput.aspx
		 * ---------------------------------------------------------------------------------------------------------
		 **/
		HTTPRequest request = new HTTPRequest(getSiteLink() + "SearchInput.aspx");
		request.setHeader("User-Agent", getUserAgentValue());	
		request.setHeader("Accept", getAccept());
		request.setHeader("Accept-Language", getAcceptLanguage());
		request.setHeader("Accept-Encoding", "gzip, deflate");
		
//		String responseAsString = execute(request);
		HTTPResponse resp = process(request);
		String responseAsString = "";
		try {
			responseAsString = resp.getGZipResponseAsString();
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		Pattern titlePattern = Pattern.compile("(?is)<title>(.*)</title>");
		
		if(responseAsString == null) {
			return new LoginResponse(LoginStatus.STATUS_INVALID_PAGE, "No respose for " + getSiteLink());
		}
		
		Matcher matcher = titlePattern.matcher(responseAsString);
		Map<String,String> addParams = null;
		Map<String,String> cookie = new HashMap<String, String>();
		
		if(matcher.find()) {
			String title = matcher.group(1);
			if (title.contains("Search Input")) {
				//isolate parameters
				addParams = isolateParams(responseAsString, formID1, ADD_PARAM_NAMES);
			} else {
				return new LoginResponse(LoginStatus.STATUS_INVALID_PAGE, "Unexpected page received with title " + title);
			}
			
		} else {
			return new LoginResponse(LoginStatus.STATUS_INVALID_PAGE, "Unexpected page received with no title ");
		}
		
		/**
		 * 02. GET page with default search module: /IAS/SearchResults.aspx
		 * ---------------------------------------------------------------------------------------------------------
		 **/
		request.modifyURL(getSiteLink() + "SearchResults.aspx");
		request.setMethod(HTTPRequest.GET);
		
		//responseAsString = execute(request);
		resp = process(request);
		try {
			responseAsString = resp.getGZipResponseAsString();
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		if(responseAsString == null) {
			return new LoginResponse(LoginStatus.STATUS_INVALID_PAGE, "No respose for SearchResults.aspx");
		}
		
		if(responseAsString.contains("The search results list will be shown here when the") && responseAsString.contains("Find")  && responseAsString.contains("button is clicked.")) {
			//addParams = isolateParams(responseAsString, formID1, ADD_PARAM_NAMES);
			cookie.put(getCookies().get(0).getName(), getCookies().get(0).getValue());
			cookie.put(getCookies().get(1).getName(), getCookies().get(1).getValue());
			setAttribute("params", addParams);
			setAttribute("cookie", cookie);
			
			return LoginResponse.getDefaultSuccessResponse();
			
		} else {
			return new LoginResponse(LoginStatus.STATUS_INVALID_PAGE, "Could not get login page!");
		}
	}
	 
	
	public String getSearchCriteriaPage(HTTPRequest req, Map<String,String> cookies, String searchType) {
		String resp = "";
		HTTPRequest req1 = new HTTPRequest(getSiteLink() + "SearchInput.aspx", HTTPRequest.POST);
		
		for(Map.Entry<String, String> entry: cookies.entrySet()) {
			req.setHeader(entry.getKey(), entry.getValue());
			req1.setHeader(entry.getKey(), entry.getValue());
		}
		req1.setPostParameter("__VIEWSTATE", req.getPostFirstParameter("__VIEWSTATE"));
		req1.setPostParameter("__EVENTVALIDATION", req.getPostFirstParameter("__EVENTVALIDATION"));
		req1.setPostParameter("__PREVIOUSPAGE", req.getPostFirstParameter("__PREVIOUSPAGE"));
		req1.setPostParameter("__EVENTARGUMENT","");
		req1.setPostParameter("__EVENTTARGET", searchType);
		req1.setPostParameter("SearchType", searchType);
		req1.setPostParameter("TextCase", "ProperCase");
		
		resp = execute(req1);
		
		return resp;
	}
	
	
	public String executePreliminaryReq(HTTPRequest req, Map<String,String> cookies, Map<String,String> newParams,  String searchType) {
		String resp = "";
		HTTPRequest req1 = new HTTPRequest(req.getURL(), HTTPRequest.POST);
		
		for(Map.Entry<String, String> entry: cookies.entrySet()) {
			req1.setHeader(entry.getKey(), entry.getValue());
		}
		
		req1.setPostParameters(req.getPostParameters());
		req1.removePostParameters("__PREVIOUSPAGE");
		req1.setPostParameter("__PREVIOUSPAGE", newParams.get("__PREVIOUSPAGE"));
		req1.removePostParameters("__VIEWSTATE");
		req1.setPostParameter("__VIEWSTATE", newParams.get("__VIEWSTATE"));
		req1.removePostParameters("__EVENTVALIDATION");
		req1.setPostParameter("__EVENTVALIDATION", newParams.get("__EVENTVALIDATION"));
		
		resp = execute(req1);
		
		return resp;
	}
	
	public void prepareReqForSearch(HTTPRequest req, String prevResponse, String searchType) {
		Map<String,String> paramsToAdd = new HashMap<String, String>();
		Map<String,String> hiddenParams = new HashMap<String, String>();
		
		req.removePostParameters("__VIEWSTATE");
		req.removePostParameters("__PREVIOUSPAGE");
		req.removePostParameters("__VIEWSTATE");
		req.removePostParameters("__EVENTVALIDATION");
		req.removePostParameters("butFind");
		req.removePostParameters("SearchType");
		
		if (searchByLocatorNo.equals(searchType)) {
			req.removePostParameters("tboxLocatorNum");
		} else if (searchByName.equals(searchType)) {
			req.removePostParameters("tboxLastName");
			req.removePostParameters("tboxFirstName");
		} else if (searchByAddress.equals(searchType)) {
			req.removePostParameters("tboxAddrNum");
			req.removePostParameters("ddboxDir");
			req.removePostParameters("tboxStreet");
			req.removePostParameters("ddboxSuffix");
		} else if (searchBySubdiv.equals(searchType)) {
			req.removePostParameters("tboxSubdivision");
		} 
		
		paramsToAdd = isolateParams(prevResponse, formID2, REQ_PARAM_NAMES);
		
		hiddenParams = getHiddenParams(prevResponse, formID2);
		if (hiddenParams != null) {
			hiddenParams.remove("hidRefreshDate");
			hiddenParams.remove("hidShowDataCase");
			hiddenParams.put("hidShowDataCase","ProperCase");
			paramsToAdd.putAll(hiddenParams);
			paramsToAdd.remove("__PREVIOUSPAGE");
			paramsToAdd.put("__LASTFOCUS", "");
		}
		
		req.addPostParameters(paramsToAdd);
	}
	
	
	public void performSearch(HTTPRequest req, Map<String,String> cookies, String searchType) {
		String resp = "";
		Map<String,String> paramsToAdd = new HashMap<String, String>();
		
		resp = getSearchCriteriaPage(req, cookies, searchType);
		paramsToAdd = isolateParams(resp, formID1, REQ_PARAM_NAMES);
		
		resp = executePreliminaryReq(req, cookies, paramsToAdd, formID2);
		prepareReqForSearch(req, resp, searchType);
		
		return;
	}
	
	@Override
	public void onBeforeRequestExcl(HTTPRequest req) {
		Map<String,String> addParams 	= (HashMap<String,String>)getAttribute("params");
		Map<String,String> cookies 		= (HashMap<String,String>)getAttribute("cookie");
		
		if (req.getPostFirstParameter("SearchType") != null) {
			req.addPostParameters(addParams);
			if (searchByLocatorNo.equals(req.getPostFirstParameter("SearchType"))) {
				performSearch(req, cookies, searchByLocatorNo);
			} else if (searchByName.equals(req.getPostFirstParameter("SearchType"))) {
				performSearch(req, cookies, searchByName);
			} else if (searchByAddress.equals(req.getPostFirstParameter("SearchType"))) {
				performSearch(req, cookies, searchByAddress);
			} else if (searchBySubdiv.equals(req.getPostFirstParameter("SearchType"))) {
				performSearch(req, cookies, searchBySubdiv);
			}
		
		} else if (req.getPostFirstParameter("seq") != null) {
			String seq = req.getPostFirstParameter("seq");
			
			if (req.getPostFirstParameter("Locator") != null) { //details page
				addParams = (HashMap<String,String>)getTransientSearchAttribute("params:" + seq);
				if (addParams != null) {
					req.removePostParameters("seq");
					for(Map.Entry<String, String> entry: addParams.entrySet()) {
						if (!"__PREVIOUSPAGE".equals(entry.getKey())) {
							req.setPostParameter(entry.getKey(), entry.getValue());
						}
					}
				}
			} else if (req.getPostFirstParameter("Page") != null) {
				addParams = (HashMap<String,String>)getTransientSearchAttribute("params:" + seq);
				if (addParams != null) {
					req.removePostParameters("seq");
					for(Map.Entry<String, String> entry: addParams.entrySet()) {
						req.setPostParameter(entry.getKey(), entry.getValue());
					}
				}
			}
		
		} else if (req.getURL().contains("Collection") || req.getURL().contains("RecorderOfDeeds")) {
			req.setHeader("User-Agent", getUserAgentValue());	
			req.setHeader("Accept", getAccept());
			req.setHeader("Accept-Language", getAcceptLanguage());
			req.setHeader("Accept-Encoding", "gzip, deflate");
		}
	}
	

	@Override
	public void onAfterRequest(HTTPRequest req, HTTPResponse res) {
		if (req != null && res != null) {
			if (req.getURL().contains("/RecorderOfDeeds/") || req.getURL().contains("/Collection/")) {
				if ("gzip".equals(res.getHeader("Content-Encoding"))) {
					try {
						String responseAsString = "";
						try {
							responseAsString = res.getGZipResponseAsString();
						} catch (IOException e) {
							e.printStackTrace();
						}
						res.is = new ByteArrayInputStream(responseAsString.getBytes());
						req.setBypassResponse(res);
					} catch (Exception e) {
						System.err.println(e);
					}
				}
			}
		}
	}
	
	
	@Override
	public String getUserAgentValue() {
		return "Mozilla/5.0 (Windows NT 5.1; rv:26.0) Gecko/20100101 Firefox/26.0";
	}
	
	@Override
	public String getAcceptLanguage() {
		return "en-US,en;q=0.5";
	}
	
		
}
