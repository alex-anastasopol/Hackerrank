package ro.cst.tsearch.connection.http2;

import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;

import org.apache.commons.httpclient.protocol.Protocol;
import org.htmlparser.Parser;
import org.htmlparser.filters.HasAttributeFilter;
import org.htmlparser.filters.NotFilter;
import org.htmlparser.filters.TagNameFilter;
import org.htmlparser.tags.LinkTag;
import org.htmlparser.tags.TableColumn;
import org.htmlparser.tags.TableRow;
import org.htmlparser.tags.TableTag;
import org.htmlparser.util.NodeList;
import org.htmlparser.util.ParserException;

import ro.cst.tsearch.connection.LoginResponse;
import ro.cst.tsearch.connection.LoginResponse.LoginStatus;
import ro.cst.tsearch.connection.http.HTTPRequest;
import ro.cst.tsearch.connection.http.HTTPResponse;
import ro.cst.tsearch.connection.http.HTTPRequest.ParametersVector;
import ro.cst.tsearch.connection.http.auth.ssl.EasySSLProtocolSocketFactory;
import ro.cst.tsearch.data.SitesPasswords;
import ro.cst.tsearch.utils.StringUtils;

public class XXStewartPriorPF extends HttpSite {

	private static final String[] ADD_PARAM_NAMES = { "__LASTFOCUS", "__VIEWSTATE", "__EVENTTARGET", "__EVENTARGUMENT", "__EVENTVALIDATION" };
	private static final String[] REQ_PARAM_NAMES_LOGIN = { "__VIEWSTATE", "__EVENTVALIDATION" };
	private static final String[] REQ_PARAM_NAMES_STEWART_SEARCH_FILES_LINK = { "__VIEWSTATE", "__EVENTVALIDATION", "__PREVIOUSPAGE" };
	public static final String[] REQ_PARAM_NAMES_SEARCH = { "__VIEWSTATE", "__EVENTVALIDATION", /*"__PREVIOUSPAGE"*/ };
	
	public static final String FORM_NAME = "aspnetForm";
	private static final String PARAM_LOGIN_PREFIX = "ctl00$loginView$SumsLoginControl$";
	private static final String STEWART_PRIOR_FILES_LINK_PREFIX = "ctl00_loginView_portalMenu_";
	
	private static final String PARAMETER_PREFIX_1 = "ats-prefix1-";
	private static final String PARAMETER_PREFIX_2 = "ats-prefix2-";
	
	private static final String PARAMETERS_GENERAL = "params";
	public static final String PARAMETERS_NAVIGATION_LINK = "params.link";
	
	@Override
	public LoginResponse onLogin() {
		
		String user = SitesPasswords.getInstance().getPasswordValue(getCurrentCommunityId(), "XXStewartPriorPF", "user");
		String pass = SitesPasswords.getInstance().getPasswordValue(getCurrentCommunityId(), "XXStewartPriorPF", "password");
		if(StringUtils.isEmpty(user) || StringUtils.isEmpty(pass)) {
			return new LoginResponse(LoginStatus.STATUS_INVALID_CREDENTIALS, "Couldn't get credentials from database");
		}
		
		Protocol easyhttps = new Protocol("https", new EasySSLProtocolSocketFactory(), 443);
        Protocol.registerProtocol("https", easyhttps);
		
		HTTPRequest request = new HTTPRequest("https://sums.stewart.com/Login.aspx");
		HTTPResponse response = process( request );
		String responseAsString = response.getResponseAsString();		
		Map<String,String> addParams = fillAndValidateConnectionParams(
				responseAsString, ADD_PARAM_NAMES, REQ_PARAM_NAMES_LOGIN, FORM_NAME);
		if(addParams == null) {
			return new LoginResponse(LoginStatus.STATUS_INVALID_PAGE, "Invalid parameters");
		}
		
		request.setMethod(HTTPRequest.POST);
		request.setHeader("Referer", "https://sums.stewart.com/Login.aspx");
		//request.setHeader("Cookie", response.getHeader("Set-Cookie")+" "+response.getHeader("set-cookie"));
		for (int i = 0; i < ADD_PARAM_NAMES.length; i++) {
			if(!addParams.containsKey(ADD_PARAM_NAMES[i])) {
				request.setPostParameter(ADD_PARAM_NAMES[i], "");
			} else {
				request.setPostParameter(ADD_PARAM_NAMES[i], addParams.get(ADD_PARAM_NAMES[i]));
			}
		}
		request.setPostParameter(PARAM_LOGIN_PREFIX + "UserName", user);
		request.setPostParameter(PARAM_LOGIN_PREFIX + "Password", pass);
		request.setPostParameter(PARAM_LOGIN_PREFIX + "LoginButton", "Login");
		setAttribute(SINGLE_LINE_COOKIES_IN_REDIRECT, true);
		response = process( request );
		responseAsString = response.getResponseAsString();
		addParams = fillAndValidateConnectionParams(
				responseAsString, REQ_PARAM_NAMES_LOGIN, REQ_PARAM_NAMES_LOGIN, FORM_NAME);
		if(addParams == null) {
			return new LoginResponse(LoginStatus.STATUS_INVALID_PAGE, "Invalid parameters");
		}
		
		request = new HTTPRequest("https://sums.stewart.com/Default.aspx", HTTPRequest.POST);
		request.setHeader("Referer", "https://sums.stewart.com/Default.aspx");
		//request.setHeader("Cookie", response.getHeader("Cookie"));
		for (int i = 0; i < REQ_PARAM_NAMES_LOGIN.length; i++) {
			request.setPostParameter(REQ_PARAM_NAMES_LOGIN[i], 
					addParams.get(REQ_PARAM_NAMES_LOGIN[i]));

		}
		request.setPostParameter(STEWART_PRIOR_FILES_LINK_PREFIX + "ExpandState", "n");
		request.setPostParameter(STEWART_PRIOR_FILES_LINK_PREFIX + "SelectedNode", "ctl00_loginView_portalMenun0");
		request.setPostParameter(STEWART_PRIOR_FILES_LINK_PREFIX + "PopulateLog", "");
		request.setPostParameter("__EVENTTARGET", "ctl00$loginView$portalMenu");
		request.setPostParameter("__EVENTARGUMENT", "sSPF");
		
		responseAsString = process( request ).getResponseAsString();
		try {
			addParams = fillAndValidateConnectionParams(
				responseAsString, REQ_PARAM_NAMES_LOGIN, FORM_NAME);
		} catch (Exception e) {
			destroySession();
			e.printStackTrace();
			return new LoginResponse(LoginStatus.STATUS_INVALID_PAGE, "Invalid parameters");
		}
		if(addParams == null) {
			return new LoginResponse(LoginStatus.STATUS_INVALID_PAGE, "Invalid parameters");
		}
		
		request = new HTTPRequest("http://priorfiles.stewart.com/Default.aspx", HTTPRequest.POST);
		request.setHeader("Referer", "http://priorfiles.stewart.com/Default.aspx");
		
		for (int i = 0; i < REQ_PARAM_NAMES_LOGIN.length; i++) {
			request.setPostParameter(REQ_PARAM_NAMES_LOGIN[i], 
					addParams.get(REQ_PARAM_NAMES_LOGIN[i]));
		}
		request.setPostParameter("__EVENTTARGET", "ctl00$loginView$portalMenu");
		request.setPostParameter("__EVENTARGUMENT", "sSPF\\NavURL:Search/SearchPriorFiles.aspx");
		
		request.setPostParameter(STEWART_PRIOR_FILES_LINK_PREFIX + "ExpandState", "en");
		request.setPostParameter(STEWART_PRIOR_FILES_LINK_PREFIX + "SelectedNode", "ctl00_loginView_portalMenun1");
		request.setPostParameter(STEWART_PRIOR_FILES_LINK_PREFIX + "PopulateLog", "");
		
		responseAsString = process( request ).getResponseAsString();
		addParams = fillAndValidateConnectionParams(
				responseAsString, REQ_PARAM_NAMES_LOGIN, FORM_NAME);
		if(addParams == null) {
			return new LoginResponse(LoginStatus.STATUS_INVALID_PAGE, "Invalid parameters");
		}
		
		//click on General Search
		request = new HTTPRequest("http://priorfiles.stewart.com/Search/SearchPriorFiles.aspx", HTTPRequest.POST);
		request.setHeader("Referer", "http://priorfiles.stewart.com/Search/SearchPriorFiles.aspx");
		for (int i = 0; i < REQ_PARAM_NAMES_LOGIN.length; i++) {
			request.setPostParameter(REQ_PARAM_NAMES_LOGIN[i], 
					addParams.get(REQ_PARAM_NAMES_LOGIN[i]));

		}
		request.setPostParameter("__EVENTTARGET", "ctl00$ContentPlaceHolder1$SearchCategory");
		request.setPostParameter("__EVENTARGUMENT", "activeTabChanged:1");
		
		request.setPostParameter(STEWART_PRIOR_FILES_LINK_PREFIX + "ExpandState", "en");
		request.setPostParameter(STEWART_PRIOR_FILES_LINK_PREFIX + "SelectedNode", "ctl00_loginView_portalMenun1");
		request.setPostParameter(STEWART_PRIOR_FILES_LINK_PREFIX + "PopulateLog", "");
		
		responseAsString = process( request ).getResponseAsString();
		addParams = fillAndValidateConnectionParams(
				responseAsString, REQ_PARAM_NAMES_LOGIN, FORM_NAME);
		if(addParams == null) {
			return new LoginResponse(LoginStatus.STATUS_INVALID_PAGE, "Invalid parameters");
		}
		
		//choose State
		request = new HTTPRequest("http://priorfiles.stewart.com/Search/SearchPriorFiles.aspx", HTTPRequest.POST);
		request.setHeader("Referer", "http://priorfiles.stewart.com/Search/SearchPriorFiles.aspx");
		for (int i = 0; i < REQ_PARAM_NAMES_LOGIN.length; i++) {
			request.setPostParameter(REQ_PARAM_NAMES_LOGIN[i], 
					addParams.get(REQ_PARAM_NAMES_LOGIN[i]));

		}
		request.setPostParameter("__EVENTTARGET", "ctl00$ContentPlaceHolder1$cboState");
		request.setPostParameter("__EVENTARGUMENT", "");
		request.setPostParameter("ctl00$ContentPlaceHolder1$ScriptManager1", "ctl00$ContentPlaceHolder1$DisplaySection_Address|ctl00$ContentPlaceHolder1$cboState");
		request.setPostParameter("ctl00_ContentPlaceHolder1_SearchCategory_ClientState", "{\"ActiveTabIndex\":1,\"TabState\":[true,true,true]}");
		request.setPostParameter("ctl00$ContentPlaceHolder1$cboPolicyType", "All Policy Types");
		request.setPostParameter("ctl00$ContentPlaceHolder1$cboState", getState());
		request.setPostParameter("__ASYNCPOST", "true");
		
		request.setPostParameter(STEWART_PRIOR_FILES_LINK_PREFIX + "ExpandState", "en");
		request.setPostParameter(STEWART_PRIOR_FILES_LINK_PREFIX + "SelectedNode", "ctl00_loginView_portalMenun1");
		request.setPostParameter(STEWART_PRIOR_FILES_LINK_PREFIX + "PopulateLog", "");
		
		responseAsString = process( request ).getResponseAsString();
		addParams = fillAndValidateConnectionParamsLastRow(
				responseAsString, REQ_PARAM_NAMES_LOGIN);
		if(addParams == null) {
			return new LoginResponse(LoginStatus.STATUS_INVALID_PAGE, "Invalid parameters");
		}
		
		setAttribute(PARAMETERS_GENERAL, addParams);
		
		return new LoginResponse(LoginStatus.STATUS_SUCCESS, "Logged in");
	}
	
	private Map<String, String> fillAndValidateConnectionParamsLastRow(
			String responseAsString, String[] reqParamNamesSearch) {
		Map<String,String> addParams = new HashMap<String, String>();
		int index = responseAsString.indexOf("__VIEWSTATE|");
		if(index >= 0) {
			responseAsString = responseAsString.substring(index);
			String[] parameters = responseAsString.split("\\|");
			for (int i = 0; i < parameters.length; i++) {
				for (int j = 0; j < reqParamNamesSearch.length; j++) {
					if(parameters[i].equals(reqParamNamesSearch[j])) {
						i++;
						addParams.put(reqParamNamesSearch[j], parameters[i]);
					}	
				}
				
			}
		}
		return addParams;
	}

	@Override
	public void onBeforeRequestExcl(HTTPRequest req) {
		setPrefixParametersForRequest(req);
		setStateParametersForRequest(req);
		String tmpPolicyNumber = req.getPostFirstParameter("tmpPolicyNumber");
		if(StringUtils.isNotEmpty(tmpPolicyNumber)) {
			//this is a image search, so we must do all steps to find the link
			req.removePostParameters("tmpPolicyNumber");
			String page = process(req).getResponseAsString();
			Parser parser = Parser.createParser(page, null);
			
			try {
				NodeList nodeList = parser.parse(null);
				NodeList smallInfoTables = nodeList.extractAllNodesThatMatch(new TagNameFilter("table"),true)
					.extractAllNodesThatMatch(new HasAttributeFilter("id","ctl00_ContentPlaceHolder1_grdSearchResults"))
					.extractAllNodesThatMatch(new TagNameFilter("table"),true)
					.extractAllNodesThatMatch(new HasAttributeFilter("style","font-family: Verdana; font-size: x-small"))
					.extractAllNodesThatMatch(new NotFilter(new HasAttributeFilter("cellspacing")));
				for (int i = 0; i < smallInfoTables.size(); i++) {
					TableTag smallInfoTable = (TableTag)smallInfoTables.elementAt(i);
					TableRow[] smallInfoRows = smallInfoTable.getRows();
					for (int j = 1; j < smallInfoRows.length; j++) {
						TableColumn[] smallInfoColumns = smallInfoRows[j].getColumns();
						if(smallInfoColumns.length == 5) {
							NodeList linkToImageList = smallInfoColumns[3].getChildren().extractAllNodesThatMatch(new TagNameFilter("a"));
							if(linkToImageList.size() > 0) {
								LinkTag imageLinkTag = (LinkTag)linkToImageList.elementAt(0);
								String tmpPolicyNumberNew = smallInfoColumns[0].toPlainTextString().trim();
								if(tmpPolicyNumber.equals(tmpPolicyNumberNew)){
									req.setMethod(HTTPRequest.GET);
									req.modifyURL("http://priorfiles.stewart.com/Search/" + imageLinkTag.getLink());
									req.setHeader("Referer", "http://priorfiles.stewart.com/Search/SearchPriorFiles.aspx");
									return;
								}
								
								
							}
							
						}
					}	
				}
				
				
				
				
			} catch (ParserException e) {
				e.printStackTrace();
			}
		}
		
	}
	
	@SuppressWarnings("unchecked")
	private void setStateParametersForRequest(HTTPRequest req) {
		if(req.getMethod() == HTTPRequest.POST) {
			Map<String,String> addParams = null;
			if(StringUtils.isNotEmpty(req.getPostFirstParameter("__EVENTTARGET"))) {
				addParams = (Map<String, String>)getSearch().getAdditionalInfo(PARAMETERS_NAVIGATION_LINK);
			} else {
				addParams = (HashMap<String,String>)getAttribute(PARAMETERS_GENERAL);
			}
			if(addParams != null) {			
				for (String key : REQ_PARAM_NAMES_SEARCH) {
					String value = "";
					if (addParams.containsKey(key)) {
						value = addParams.get(key);
					}
					req.setPostParameter(key, value);
				}			
			}
			req.setPostParameter("ctl00_ContentPlaceHolder1_SearchCategory_ClientState","{\"ActiveTabIndex\":2,\"TabState\":[true,true,true]}");
		} else {
			String url = req.getURL();
			url = url.replaceFirst("&dummy=[^&]*", "");
			url = url.replaceFirst("&tmpShortLegal=[^&]*", "");
			url = url.replaceFirst("&tmpFullLegal=[^&]*", "");
			url = url.replaceFirst("&tmpParsedAddress=[^&]*", "");
			req.modifyURL(url);
		}
		
	}

	private void setPrefixParametersForRequest(HTTPRequest req) {
		String prefix1 = req.getPostFirstParameter(PARAMETER_PREFIX_1);
		String prefix2 = req.getPostFirstParameter(PARAMETER_PREFIX_2);
		req.removePostParameters(PARAMETER_PREFIX_1);
		req.removePostParameters(PARAMETER_PREFIX_2);
		
		Object keys[]=(Object[])req.getPostParameters().keySet().toArray();
		for (Object keyObject : keys) {
			String key = (String)keyObject;
			if(!StringUtils.isEmpty(prefix1)) {
				if(key.startsWith(PARAMETER_PREFIX_1)) {
					ParametersVector value = req.getPostParameter(key);
					req.removePostParameters(key);
					req.setPostParametersVector(
							key.replaceFirst(PARAMETER_PREFIX_1, Matcher.quoteReplacement(prefix1)), value);
				}
				
			} 
			if(!StringUtils.isEmpty(prefix2)) {
				if(key.startsWith(PARAMETER_PREFIX_2)) {
					ParametersVector value = req.getPostParameter(key);
					req.removePostParameters(key);
					req.setPostParametersVector(
							key.replaceFirst(PARAMETER_PREFIX_2, Matcher.quoteReplacement(prefix2)), value);
				}
			}
			
		}
	}
}
