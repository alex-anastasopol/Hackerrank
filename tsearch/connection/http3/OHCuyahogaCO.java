package ro.cst.tsearch.connection.http3;

import java.io.ByteArrayInputStream;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.httpclient.URI;
import org.apache.commons.httpclient.URIException;
import org.apache.commons.io.IOUtils;

import ro.cst.tsearch.connection.LoginResponse;
import ro.cst.tsearch.connection.http.HTTPRequest;
import ro.cst.tsearch.connection.http.HTTPResponse;

public class OHCuyahogaCO extends HttpSite3 {
	
	public static final String SEQ_PARAM = "seq";
	
	public static final String EVENT_TARGET_PARAM = "__EVENTTARGET";
	public static final String EVENT_ARGUMENT_PARAM = "__EVENTARGUMENT";
	public static final String VIEW_STATE_PARAM = "__VIEWSTATE";
	public static final String EVENT_VALIDATION_PARAM = "__EVENTVALIDATION";
	
	private static final String LAST_FOCUS_PARAM = "__LASTFOCUS";
	private static final String ASYNC_POST_PARAM = "__ASYNCPOST";
	
	private static final String YES_BUTTON_PARAM = "ctl00$SheetContentPlaceHolder$btnYes";
	private static final String SCRIPT_MANAGER_PARAM = "ctl00$ScriptManager1";
	private static final String SEARCH_TYPE_PARAM = "ctl00$SheetContentPlaceHolder$rbSearches";
	
	private String viewState = "";
	private String eventValidation = "";
	
	@Override
	public LoginResponse onLogin() {
		
		//get first page
		HTTPRequest req1 = new HTTPRequest(getSiteLink(), HTTPRequest.GET);
		String resp1 = execute(req1);
		String viewState = getParameterValueFromHtml(VIEW_STATE_PARAM, resp1);
		String eventValidation = getParameterValueFromHtml(EVENT_VALIDATION_PARAM, resp1);
		
		//click on "Yes" button from "Conditions of Use and Privacy Policy "
		HTTPRequest req2 = new HTTPRequest(getSiteLink(), HTTPRequest.POST);
		req2.setPostParameter(EVENT_TARGET_PARAM, "");
		req2.setPostParameter(EVENT_ARGUMENT_PARAM, "");
		req2.setPostParameter(VIEW_STATE_PARAM, viewState);
		req2.setPostParameter(EVENT_VALIDATION_PARAM, eventValidation);
		req2.setPostParameter(YES_BUTTON_PARAM, "Yes");
		String resp2 = execute(req2);
		this.viewState = getParameterValueFromHtml(VIEW_STATE_PARAM, resp2);
		this.eventValidation = getParameterValueFromHtml(EVENT_VALIDATION_PARAM, resp2);
		
		if (resp2.contains("Please Select a Search Option")) {
			return LoginResponse.getDefaultSuccessResponse();
		} else {
			return LoginResponse.getDefaultFailureResponse();
		}
		
	}
	
	@SuppressWarnings("unchecked")
	@Override
	public void onBeforeRequestExcl(HTTPRequest req) {

		String url = req.getURL();
		
		if (req.getMethod()==HTTPRequest.POST) {
			if (url.contains("/Search.aspx")) {		//intermediary result
				String searchType = req.getPostFirstParameter(SEARCH_TYPE_PARAM);
				if (searchType!=null) {
					HTTPRequest req1 = new HTTPRequest(url, HTTPRequest.POST);
					req1.setPostParameter(SCRIPT_MANAGER_PARAM, "ctl00$SheetContentPlaceHolder$UpdatePanel1|" + SEARCH_TYPE_PARAM + "$0");
					req1.setPostParameter(EVENT_TARGET_PARAM, SEARCH_TYPE_PARAM + "$0");
					req1.setPostParameter(EVENT_ARGUMENT_PARAM, "");
					req1.setPostParameter(LAST_FOCUS_PARAM, "");
					req1.setPostParameter(VIEW_STATE_PARAM, viewState);
					req1.setPostParameter(EVENT_VALIDATION_PARAM, eventValidation);
					req1.setPostParameter(SEARCH_TYPE_PARAM, searchType);
					req1.setPostParameter(ASYNC_POST_PARAM, "true");
					String resp1 = execute(req1);
					String viewState = getParameterValueFromText(VIEW_STATE_PARAM, resp1);
					String eventValidation = getParameterValueFromText(EVENT_VALIDATION_PARAM, resp1);
					
					req.setPostParameter(EVENT_TARGET_PARAM, "");
					req.setPostParameter(EVENT_ARGUMENT_PARAM, "");
					req.setPostParameter(VIEW_STATE_PARAM, viewState);
					req.setPostParameter(EVENT_VALIDATION_PARAM, eventValidation);
					req.setPostParameter(LAST_FOCUS_PARAM, "");
					req.setPostParameter(ASYNC_POST_PARAM, "true");
				}
			} else {							//details result
				String seqParameter = req.getPostFirstParameter(SEQ_PARAM);
				if (seqParameter!=null) {
					Map<String, String> params = (Map<String, String>)getTransientSearchAttribute("params:" + seqParameter);
					req.removePostParameters(SEQ_PARAM);
					String viewState = params.get(VIEW_STATE_PARAM);
					String eventValidation = params.get(EVENT_VALIDATION_PARAM);
					String eventTarget = params.get(EVENT_TARGET_PARAM);
					String eventArgument = params.get(EVENT_ARGUMENT_PARAM);
					req.setPostParameter(VIEW_STATE_PARAM, viewState);
					req.setPostParameter(EVENT_VALIDATION_PARAM, eventValidation);
					req.setPostParameter(EVENT_TARGET_PARAM, eventTarget);
					req.setPostParameter(EVENT_ARGUMENT_PARAM, eventArgument);
				}
			}
			 
		} 
		
	}
	
	public static String getParameterValueFromHtml(String parameter, String value) {
		String result = "";
		
		Matcher matcher = Pattern.compile("<input.+?id=\"" + parameter + "\".+value=\"(.+?)\"").matcher(value);
		if (matcher.find()) {
			result = matcher.group(1);
		}
		
		return result;
	}
	
	public static String getParameterValueFromText(String parameter, String value) {
		String result = "";
		
		Matcher matcher = Pattern.compile("(?is)\\|" + parameter + "\\|([^|]*)\\|").matcher(value);
		if (matcher.find()) {
			result = matcher.group(1);
		}
		
		return result;
	}
	
	@Override
	public void onAfterRequest(HTTPRequest req, HTTPResponse res) {
		
		String url = req.getURL();
		if (url.contains("/Search.aspx") && req.getMethod()==HTTPRequest.POST) {
			String content = res.getResponseAsString();
			Matcher ma = Pattern.compile("(?is)\\bpageRedirect\\|\\|([^|]+)\\|").matcher(content);
			if (ma.find()) {												//needs redirect
				req.setMethod(HTTPRequest.GET);
				req.clearPostParameters();
				int index = url.lastIndexOf("/");
				if (index>-1) {
					try {
						url = url.substring(0, index) + URLDecoder.decode(ma.group(1), "UTF8");
					} catch (UnsupportedEncodingException e) {
						e.printStackTrace();
					}
				}
				if (url.contains("/Error.aspx?error=")) {
					res.is = IOUtils.toInputStream("<html>/Error.aspx</html>");
					res.body = "<html>/Error.aspx</html>";
					res.contentType = "text/html";
					res.contentLenght = res.body.length();
					res.returnCode = 200;
					try {
						res.setLastURI(new URI(url, false));
					} catch (URIException e) {
						e.printStackTrace();
					} catch (NullPointerException e) {
						e.printStackTrace();
					}
					return;
				} else {
					req.modifyURL(url);
					HTTPResponse resp = process(req);
					res.is = new ByteArrayInputStream(resp.getResponseAsByte());
					res.contentLenght= resp.contentLenght;
					res.contentType = resp.contentType;
					res.headers = resp.headers;
					res.returnCode = resp.returnCode;
					res.body = resp.body;
					res.setLastURI(resp.getLastURI());
				}
			} else {
				res.is = new ByteArrayInputStream(content.getBytes());
			}
		} else {
			String link = getStringURI(res.getLastURI());
			String content = res.getResponseAsString();
			if (link.contains("/CV_CaseInformation_Summary.aspx")||link.contains("/COA_CaseInformation_Summary.aspx")) {		//redirect Civil Case details page from Summary to All
				String viewState = getParameterValueFromHtml(VIEW_STATE_PARAM, content);
				String eventValidation = getParameterValueFromHtml(EVENT_VALIDATION_PARAM, content);
				req.removePostParameters(EVENT_TARGET_PARAM);
				req.removePostParameters(VIEW_STATE_PARAM);
				req.removePostParameters(EVENT_VALIDATION_PARAM);
				req.setPostParameter(EVENT_TARGET_PARAM, "ctl00$SheetContentPlaceHolder$caseHeader$lbAll");
				req.setPostParameter(VIEW_STATE_PARAM, viewState);
				req.setPostParameter(EVENT_VALIDATION_PARAM, eventValidation);
				req.modifyURL(link);
				req.setMethod(HTTPRequest.POST);
				HTTPResponse resp = process(req);
				res.is = new ByteArrayInputStream(resp.getResponseAsByte());
				res.contentLenght= resp.contentLenght;
				res.contentType = resp.contentType;
				res.headers = resp.headers;
				res.returnCode = resp.returnCode;
				res.body = resp.body;
				res.setLastURI(resp.getLastURI());
			} else if (content.contains("Server Error in '/' Application.")) {
				res.is = IOUtils.toInputStream("<html>Server Error in '/' Application.</html>");
				res.body = "<html>Server Error in '/' Application.</html>";
				res.contentLenght = res.body.length();
				res.returnCode = 200;
				return;
			} else {
				res.is = new ByteArrayInputStream(content.getBytes());
			}
		}	
		
	}
	
	public static String getStringURI(URI uri) {
		String stringURI = "";
		if (uri!=null) {
			try {
				stringURI = uri.getURI();
				if (!stringURI.startsWith("http:")) {
					stringURI = "http:" + stringURI;
				}
			} catch (URIException e) {
				e.printStackTrace();
			}
		}
		return stringURI;
	}
	
}
