package ro.cst.tsearch.connection.http2;

import static ro.cst.tsearch.connection.http.HTTPRequest.GET;
import static ro.cst.tsearch.connection.http.HTTPRequest.POST;

import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import ro.cst.tsearch.connection.LoginResponse;
import ro.cst.tsearch.connection.LoginResponse.LoginStatus;
import ro.cst.tsearch.connection.http.HTTPRequest;

public class OHFairfieldTR extends HttpSite {
	
	public static String[] PARAMETERS_ADDRESS = {"SearchType", "Criteria1", "Criteria2", "Criteria3", "Criteria4"};
	public static String[] PARAMETERS_GET = {"__EVENTTARGET", "__EVENTARGUMENT", "__LASTFOCUS", "__VIEWSTATE", 
		                                     "ctl00$ContentPlaceHolder1$smData", "ctl00$tbSearchBox", 
		                                     "ctl00$tbSavePropertyAs", "ctl00$ContentPlaceHolder1$Tax$ddlTaxYear", 
		                                     "ctl00$ContentPlaceHolder1$Tax$fvDataSpecials$ddlDataSpecials", 
		                                     "ctl00$ContentPlaceHolder1$smData"};
	public static String[] PARAMETERS_POST = {"__EVENTTARGET", "__EVENTARGUMENT", "__VIEWSTATE"};
	boolean done = false;
		
	@Override
	public LoginResponse onLogin() {
		
		String eventTarget = "";
		String eventArgument = "";
		String viewState = "";

		setAttribute(HttpSite.SINGLE_LINE_COOKIES_IN_REDIRECT, Boolean.TRUE);
		
		String link1 = getSiteLink();
		
		HTTPRequest req1 = new HTTPRequest(link1, GET);
		String responseAsString  = execute(req1);
		
		if(responseAsString != null)
		{
			Matcher matcher1 = Pattern.compile("(?i)<input.*?id=\"__EVENTTARGET\".*?value=\"(.*?)\".*").matcher(responseAsString);
			if (matcher1.find()) {
				eventTarget = matcher1.group(1);
			}
			
			Matcher matcher2 = Pattern.compile("(?i)<input.*?id=\"__EVENTARGUMENT\".*?value=\"(.*?)\".*").matcher(responseAsString);
			if (matcher2.find()) {
				eventArgument = matcher2.group(1);
			}
			Matcher matcher3 = Pattern.compile("(?i)<input.*?id=\"__VIEWSTATE\".*?value=\"(.*?)\".*").matcher(responseAsString);
			if (matcher3.find()) {
				viewState = matcher3.group(1);
			}
			
			String link2 = "http://realestate.co.fairfield.oh.us/Disclaimer.aspx?Redirect=%2fSearch.aspx&CheckForCookies=Yes";
			HTTPRequest req2 = new HTTPRequest(link2, POST);
			req2.setPostParameter("__EVENTTARGET", eventTarget);
			req2.setPostParameter("__EVENTARGUMENT", eventArgument);
			req2.setPostParameter("__VIEWSTATE", viewState);
			req2.setPostParameter("ctl00$tbSearchBox", "Enter Parcel, Owner, or Address");
			req2.setPostParameter("ctl00$ContentPlaceHolder1$btnDisclaimerAccept", "I Accept");
			req2.setReferer("http://realestate.co.fairfield.oh.us/Disclaimer.aspx?Redirect=%2fSearch.aspx&CheckForCookies=Yes");
			execute(req2);
			
			return new LoginResponse(LoginStatus.STATUS_SUCCESS, "Logged in");
		}	
		
		return new LoginResponse(LoginStatus.STATUS_UNKNOWN, "Error logging in");
		
	}
		
	@SuppressWarnings("unchecked")
	@Override
	public void onBeforeRequest(HTTPRequest req) {

	String url;
	String viewState = "";
	String resultsPerPage = "";
	String responseAsString = "";
		
	if (req.getMethod() == HTTPRequest.GET) {
		url = req.getURL();
		if (url.toLowerCase().contains("/results.aspx") && !done ) {								//intermediary page (the first time)
			done = true;
			responseAsString = execute(req);
			done = false;
			Matcher matcher3 = Pattern.compile("(?i)<input.*?id=\"__VIEWSTATE\".*?value=\"(.*?)\".*").matcher(responseAsString);
			if (matcher3.find()) {
				viewState = matcher3.group(1);
			}
			Matcher matcher4 = Pattern.compile("(?i)ctl00\\$ContentPlaceHolder1\\$ddlResultsPerPage=(\\d+)").matcher(url);
			if (matcher4.find()) {
				resultsPerPage = matcher4.group(1);
				url = url.replaceAll("(?i)ctl00\\$ContentPlaceHolder1\\$ddlResultsPerPage=(\\d+)&?", "");
				url = url.replaceAll("&\\z", "");
			}
			req.setMethod(HTTPRequest.POST);
			req.setURL(url);
			req.setReferer(url);
			req.setPostParameter("__EVENTTARGET", "ctl00$ContentPlaceHolder1$ddlResultsPerPage");
			req.setPostParameter("__EVENTARGUMENT", "");
			req.setPostParameter("__VIEWSTATE", viewState);
			req.setPostParameter("__LASTFOCUS", "");
			req.setPostParameter("ctl00$tbSearchBox", "Enter Parcel, Owner, or Address");
			req.setPostParameter("ctl00$tbSaveSearchAs", "");
			req.setPostParameter("ctl00$ContentPlaceHolder1$ddlResultsPerPage", resultsPerPage);
		} else if(url.toLowerCase().contains("/data.aspx") && url.toLowerCase().contains("seq=")) {	//details Page (Land, Valuation, Sales, ...)
			Matcher matcher = Pattern.compile("(?is).*?\\?seq=(\\d+)").matcher(url);
			if (matcher.find()) {
				req.setMethod(HTTPRequest.POST);
				req.setURL(url.replaceAll("(?is)\\?seq=\\d+", ""));
				String keyParameter = matcher.group(1);
				if (keyParameter!=null)
				{
					Map<String, String>  addParams = new HashMap<String, String>();
					addParams = (Map<String, String>) getTransientSearchAttribute("params:" + keyParameter);
					req.removePostParameters("seq");
					for (String par: PARAMETERS_GET) {
						String value = addParams.get(par);
						req.removePostParameters(par);
						if (value!=null)
							req.setPostParameter(par, value);
					}
				}
			}
		}
	}
	else if (req.getMethod() == HTTPRequest.POST)							
		{
			url = req.getURL();
			if (url.toLowerCase().contains("/results.aspx") ) { 									//intermediary page (next times)
								
				String keyParameter = req.getPostFirstParameter("seq");
				if (keyParameter!=null)
				{
					Map<String, String>  addParams = new HashMap<String, String>();
					addParams = (Map<String, String>) getTransientSearchAttribute("params:" + keyParameter);
					req.removePostParameters("seq");
					for (String par: PARAMETERS_POST) {
						String value = addParams.get(par);
						req.removePostParameters(par);
						if (value!=null)
							req.setPostParameter(par, value);
					}
					req.setPostParameter("__LASTFOCUS", "");
					req.setPostParameter("ctl00$tbSaveSearchAs", "");
					req.setPostParameter("ctl00$tbSearchBox", "Enter Parcel, Owner, or Address");
										
					StringBuilder sb = new StringBuilder();
					sb.append("?");
					for (String par:PARAMETERS_ADDRESS) {
						String value = req.getPostFirstParameter(par);
						if (value!=null) {
							req.removePostParameters(par);
							sb.append(par).append("=").append(value).append("&");
						}
					}
					String newParam = sb.toString();
					if (newParam.length()>1) {
						if (newParam.endsWith("&"))
							newParam = newParam.substring(0,newParam.length()-1);
						url = url + newParam;
						req.setURL(url);
					}
					
				}
			}	
		} 	
	}
}
