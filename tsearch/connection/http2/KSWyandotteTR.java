package ro.cst.tsearch.connection.http2;

import static ro.cst.tsearch.connection.http.HTTPRequest.GET;
import static ro.cst.tsearch.connection.http.HTTPRequest.POST;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import ro.cst.tsearch.connection.LoginResponse;
import ro.cst.tsearch.connection.LoginResponse.LoginStatus;
import ro.cst.tsearch.connection.http.HTTPRequest;
import ro.cst.tsearch.data.SitesPasswords;
import ro.cst.tsearch.parser.SimpleHtmlParser;
import ro.cst.tsearch.utils.StringUtils;

public class KSWyandotteTR extends HttpSite {
	
	private static final String[] ADD_PARAM_NAMES = { "__VIEWSTATE", "__PREVIOUSPAGE", "__EVENTVALIDATION", 
		                                              "__EVENTTARGET", "__EVENTARGUMENT" };
	private static final String[] REQ_PARAM_NAMES = { "__VIEWSTATE", "__PREVIOUSPAGE", "__EVENTVALIDATION" };
	
	private Map<String,String> addParams = new HashMap<String,String>();
	
	@Override
	public LoginResponse onLogin() {
		
		setAttribute(HttpSite.SINGLE_LINE_COOKIES_IN_REDIRECT, Boolean.TRUE);
		
		//get user name and password from database
		String user = SitesPasswords.getInstance().getPasswordValue(getCurrentCommunityId(), "KSWyandotteTR", "user");
		String password = SitesPasswords.getInstance().getPasswordValue(getCurrentCommunityId(), "KSWyandotteTR", "password");
		
		if(StringUtils.isEmpty(user) || StringUtils.isEmpty(password)) {
			return new LoginResponse(LoginStatus.STATUS_INVALID_CREDENTIALS, "Couldn't find user / password in database");
		}
		
		//go to first page
		String link1 = getSiteLink();
		HTTPRequest req1 = new HTTPRequest(link1, GET);
		String responseAsString  = execute(req1);
		
		addParams = isolateParams(responseAsString, "aspnetForm");
		if (!checkParams(addParams)){
			return new LoginResponse(LoginStatus.STATUS_INVALID_PAGE, "Invalid parameters");
		}
					
		//go to 'Log In'
		String link2 = link1 + "Default.aspx";
		HTTPRequest req2 = new HTTPRequest(link2, POST);
		for (String key: REQ_PARAM_NAMES) {
			req2.setPostParameter(key, addParams.get(key));
		}
		req2.setPostParameter("__EVENTTARGET", "ctl00$LeftMenuContentPlaceHolder$LeftMenu$LoginStatus1$ctl02");
		responseAsString = execute(req2);
		
		addParams = isolateParams(responseAsString, "aspnetForm");
		if (!checkParams(addParams)){
			return new LoginResponse(LoginStatus.STATUS_INVALID_PAGE, "Invalid parameters");
		}
		
		//send user name and password
		String link3 = getLastRequest();
		HTTPRequest req3 = new HTTPRequest(link3, POST);
		for (String key: REQ_PARAM_NAMES) {
			req3.setPostParameter(key, addParams.get(key));
		}
		req3.setPostParameter("ctl00$MainAreaContentPlaceHolder$Login1$UserName", user);
		req3.setPostParameter("ctl00$MainAreaContentPlaceHolder$Login1$Password", password);
		req3.setPostParameter("ctl00$MainAreaContentPlaceHolder$Login1$LoginButton", "Log In");
		responseAsString = execute(req3);
		
		addParams = isolateParams(responseAsString, "aspnetForm");
		if (!checkParams(addParams)){
			return new LoginResponse(LoginStatus.STATUS_INVALID_PAGE, "Invalid parameters");
		}
		
		//go to 'Real Estate Search'
		String link4 = link1 + "Search/Default.aspx";
		HTTPRequest req4 = new HTTPRequest(link4, POST);
		for (String key: REQ_PARAM_NAMES) {
			req4.setPostParameter(key, addParams.get(key));
		}
		req4.setPostParameter("__EVENTTARGET", "ctl00$LeftMenuContentPlaceHolder$LeftMenu$LandsWebLinkButton");
		responseAsString = execute(req4);
		
		addParams = isolateParams(responseAsString, "aspnetForm");
		if (!checkParams(addParams)){
			return new LoginResponse(LoginStatus.STATUS_INVALID_PAGE, "Invalid parameters");
		}
		
		//click on 'Accept'
		String link5 = getLastRequest();
		HTTPRequest req5 = new HTTPRequest(link5, POST);
		for (String key: REQ_PARAM_NAMES) {
			req5.setPostParameter(key, addParams.get(key));
		}
		req5.setPostParameter("ctl00$MainAreaContentPlaceHolder$btnAccept", "Accept");
		responseAsString = execute(req5);
		
		addParams = isolateParams(responseAsString, "aspnetForm");
		if (!checkParams(addParams)){
			return new LoginResponse(LoginStatus.STATUS_INVALID_PAGE, "Invalid parameters");
		}
		
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
	public void onBeforeRequest(HTTPRequest req) {
		if (this.status == STATUS_LOGGED_IN) {
			if (req.getURL().contains("/landsweb/Search/Default.aspx")) {	//first intermediary page
				for (String key: REQ_PARAM_NAMES) {
					req.setPostParameter(key, addParams.get(key));
				}
				boolean isNameSearch = StringUtils.isNotEmpty(
						req.getPostFirstParameter("ctl00$MainAreaContentPlaceHolder$AllUserView$NameTextBox"));
				if (isNameSearch) {
					req.setPostParameter("__EVENTTARGET", "ctl00$MainAreaContentPlaceHolder$AllUserView$NameButton");
				} else {
					boolean isAddressSearch = StringUtils.isNotEmpty(
							req.getPostFirstParameter("ctl00$MainAreaContentPlaceHolder$StreetNameTextBox"));
					if (isAddressSearch) {
						req.setPostParameter("__EVENTTARGET", "ctl00$MainAreaContentPlaceHolder$AddressButton");
					} else {
						boolean isParcelSearch = StringUtils.isNotEmpty(
								req.getPostFirstParameter("ctl00$MainAreaContentPlaceHolder$ParcelTextBox"));
						if (isParcelSearch) {
							req.setPostParameter("__EVENTTARGET", "ctl00$MainAreaContentPlaceHolder$ParcelButton");
						} else {
							boolean isStateParcelSearch = StringUtils.isNotEmpty(
									req.getPostFirstParameter("ctl00$MainAreaContentPlaceHolder$KupnTextBox"));
							if (isStateParcelSearch) {
								req.setPostParameter("__EVENTTARGET", "ctl00$MainAreaContentPlaceHolder$KupnButton");
							} else {
								boolean isSubdivisionSearch = StringUtils.isNotEmpty(
										req.getPostFirstParameter("ctl00$MainAreaContentPlaceHolder$SubdivisionDropDownList"));
								if (isSubdivisionSearch) {
									req.setPostParameter("__EVENTTARGET", "ctl00$MainAreaContentPlaceHolder$SubdivisionButton");
								}
							}
						}
					}
				}
			} else if (req.getURL().contains("/landsweb/Search/SearchResults.aspx")) {	//next intermediary pages (2 3 etc.)
				Map<String, String>  currentAddParams = new HashMap<String, String>();	//and details page
				StringBuilder newUrl = new StringBuilder(req.getURL());
				newUrl.append("?");
				ArrayList<String> params = new ArrayList<String>();
				params.add("Type");
				for (int i=0;i<3;i++)
					for (int j=0;j<ro.cst.tsearch.servers.types.KSWyandotteTR.PARAMETER_NAMES[i].length;j++) {
						params.add(ro.cst.tsearch.servers.types.KSWyandotteTR.PARAMETER_NAMES[i][j]);
					}
				for (String param: params) {
					String value = req.getPostFirstParameter(param);
						if (value!=null) {
							try{
								value = URLEncoder.encode(value, "UTF-8");
							}catch(UnsupportedEncodingException e){
								logger.error(e);
							}
							newUrl.append(param).append("=").append(value).append("&");
							req.removePostParameters(param);
					}
				}
				newUrl.deleteCharAt(newUrl.length()-1);
				req.modifyURL(newUrl.toString());
				String keyParameter = req.getPostFirstParameter("seq");
				if (keyParameter!=null)
				{
					currentAddParams = (Map<String, String>) getTransientSearchAttribute("params:" + keyParameter);
					req.removePostParameters("seq");
				}	
				if (currentAddParams!=null)
				{
					for (String k: currentAddParams.keySet())
						req.setPostParameter(k, currentAddParams.get(k));
				}
			}
		}
	}
	
}
