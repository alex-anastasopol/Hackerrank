package ro.cst.tsearch.connection.http3;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import ro.cst.tsearch.connection.LoginResponse;
import ro.cst.tsearch.connection.LoginResponse.LoginStatus;
import ro.cst.tsearch.connection.http.HTTPRequest;
import ro.cst.tsearch.connection.http.HTTPResponse;
import ro.cst.tsearch.parser.SimpleHtmlParser;
import ro.cst.tsearch.utils.InstanceManager;
import ro.cst.tsearch.utils.StringUtils;


public class ILMcHenryAO extends HttpSite3 {
	public static final String[] ADD_PARAM_NAMES = { "__VIEWSTATE", "__EVENTTARGET", "__EVENTARGUMENT", "__EVENTVALIDATION" };
	public static final String[] REQ_PARAM_NAMES = { "__VIEWSTATE", "__EVENTVALIDATION" };

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
	

	@Override
	public String getUserAgentValue() {
		return "Mozilla/5.0 (Windows NT 6.1; WOW64; rv:30.0) Gecko/20100101 Firefox/30.0";
	}
	
	@Override
	public String getAcceptLanguage() {
		return "en-US,en;q=0.5";
	}
	
	public String getAcceptEncoding() {
		return "gzip, deflate";
	}
	
	@Override
	public LoginResponse onLogin() {
		/**
		 * 01. Get first page and check if we are on login page
		 * ---------------------------------------------------------------------------------------------------------
		 **/
		HTTPRequest request = new HTTPRequest(getSiteLink());
		request.setHeader("User-Agent", getUserAgentValue());
		request.setHeader("Accept", getAccept());
		request.setHeader("Accept-Language", getAcceptLanguage());
		request.setHeader("Accept-Encoding", getAcceptEncoding());
		
		//String responseAsString = execute(request);
		HTTPResponse resp = process(request);
		String responseAsString = "";
		try {
			responseAsString = resp.getGZipResponseAsString();
		} catch (IOException e) {
			e.printStackTrace();
		}

		
		if(responseAsString == null) {
			return new LoginResponse(LoginStatus.STATUS_INVALID_PAGE, "No respose for " + getSiteLink());
		}
		
		Map<String,String> addParams = null;
		
		if (responseAsString.contains("Property Search") || responseAsString.contains("PROPERTY SEARCH")) {
			if (responseAsString.contains("clicking the I Agree button below")) {
				//isolate parameters
				addParams = isolateParams(responseAsString, "form1", ADD_PARAM_NAMES);
				setAttribute("params", addParams);
			} else {
				return new LoginResponse(LoginStatus.STATUS_INVALID_PAGE, "Unexpected page received when trying to log in");
			}
			
		} else {
			return new LoginResponse(LoginStatus.STATUS_INVALID_PAGE, "Unexpected page received with no title ");
		}
		
		/**
		 * 02. GET page with default search module: http://www.mchenryassessor.com/default.aspx
		 * ---------------------------------------------------------------------------------------------------------
		 **/
		request.setMethod(HTTPRequest.POST);
		request.addPostParameters(addParams);
		request.setPostParameter("btnAccept", "I Agree");
		
//		responseAsString = execute(request);
		resp = process(request);
		try {
			responseAsString = resp.getGZipResponseAsString();
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		if(responseAsString == null) {
			return new LoginResponse(LoginStatus.STATUS_INVALID_PAGE, "No respose for SrchName.aspx");
		}
		
		if(responseAsString.contains("Online Assessment Information") && responseAsString.contains("Search by Address")) {
			addParams = isolateParams(responseAsString, "form1", ADD_PARAM_NAMES);
			setAttribute("params", addParams);
			return LoginResponse.getDefaultSuccessResponse();
		} else {
			return new LoginResponse(LoginStatus.STATUS_INVALID_PAGE, "Could not get login page!");
		}
	}
	
	@Override
	public void onBeforeRequestExcl(HTTPRequest req) {
		Map<String,String> addParams = null;
		String seq = req.getPostFirstParameter("seq");
		
		if (req.getMethod() == HTTPRequest.POST) {
				//search request
				if(StringUtils.isNotEmpty(seq)) {
					req.removePostParameters("seq");
					addParams = (Map<String,String>)InstanceManager.getManager().getCurrentInstance(getSearchId()).getCrtSearchContext()
						.getAdditionalInfo(getDataSite().getName() + ":params:" + seq);
				} else {
					addParams = (HashMap<String,String>)getAttribute("params");
				}
//			}
									
			if (addParams != null) {
				for(Map.Entry<String, String> entry: addParams.entrySet()) {
					req.setPostParameter(entry.getKey(), entry.getValue());
				}
			}
		}
	}
	
	@Override
	public void onAfterRequest(HTTPRequest req, HTTPResponse res) {
		Map<String,String> params = ro.cst.tsearch.utils.StringUtils.extractParametersFromUrl(req.getURL());
		
		if (params.size() > 0) {
			if (params.containsKey("pin")) {
				destroySession();
			}
		}
		
		if (req.getPostParameters().containsKey("txtSearchPin1")) {
			if ("gzip".equals(res.getHeader("Content-Encoding"))) {
				try {
					String respAsString = "";
					try {
						respAsString = res.getGZipResponseAsString();
					} catch (IOException e) {
						e.printStackTrace();
					}
					res.is = new ByteArrayInputStream(respAsString.getBytes());
					req.setBypassResponse(res);
				} catch (Exception e) {
					System.err.println(e);
				}
			}
		}
	}
}