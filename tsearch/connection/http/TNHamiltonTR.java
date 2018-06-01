package ro.cst.tsearch.connection.http;

import ro.cst.tsearch.connection.LoginResponse;
import ro.cst.tsearch.connection.LoginResponse.LoginStatus;
import ro.cst.tsearch.utils.StringUtils;


public class TNHamiltonTR extends HTTPSite {

	public static String nameSearchViewState = "";
	public static String nameSearchEventValid = "";
	
	public static String addresSearchViewState = "";
	public static String addressSearchEventValid = "";
	
	public static String pidSearchViewState = "";
	public static String pidSearchEventValid = "";
	
	private boolean loggingIn = false;
	
	public LoginResponse onLogin() 
	{

		loggingIn = true;
		
		String tmpViewState = "";
		String tmpEventValid = "";
		
		HTTPRequest req = new HTTPRequest( "http://www2.hamiltontn.gov/TrusteeInquiry/AppFolder/PropertySearch.aspx" );
		HTTPResponse res = process(req);
		String siteResponse = res.getResponseAsString();

		//goto name search page
		tmpViewState = StringUtils.getTextBetweenDelimiters("\"__VIEWSTATE\" value=\"", "\"", siteResponse);
		tmpEventValid = StringUtils.getTextBetweenDelimiters("\"__EVENTVALIDATION\" value=\"", "\"", siteResponse);
		
		req = new HTTPRequest("http://www2.hamiltontn.gov/TrusteeInquiry/AppFolder/PropertySearch.aspx");
		req.setMethod( HTTPRequest.POST );
		
		req.setPostParameter( "__EVENTTARGET" , "ctl00$MainContent$tabLastName");
		req.setPostParameter( "__EVENTARGUMENT" , "");
		req.setPostParameter( "__VIEWSTATE" , tmpViewState);
		req.setPostParameter( "__VIEWSTATEENCRYPTED" , "");
		req.setPostParameter( "__EVENTVALIDATION" , tmpEventValid);
		res = process(req);
		siteResponse = res.getResponseAsString();
		nameSearchViewState = StringUtils.getTextBetweenDelimiters("\"__VIEWSTATE\" value=\"", "\"", siteResponse);
		nameSearchEventValid = StringUtils.getTextBetweenDelimiters("\"__EVENTVALIDATION\" value=\"", "\"", siteResponse);
		
		
		//goto addres search
		req = new HTTPRequest("http://www2.hamiltontn.gov/TrusteeInquiry/AppFolder/PropertySearch.aspx");
		req.setMethod( HTTPRequest.POST );
		
		req.setPostParameter( "__EVENTTARGET" , "ctl00$MainContent$tabAddress");
		req.setPostParameter( "__EVENTARGUMENT" , "");
		req.setPostParameter( "__VIEWSTATE" , tmpViewState);
		req.setPostParameter( "__VIEWSTATEENCRYPTED" , "");
		req.setPostParameter( "__EVENTVALIDATION" , tmpEventValid);
		res = process(req);
		siteResponse = res.getResponseAsString();
		addresSearchViewState = StringUtils.getTextBetweenDelimiters("\"__VIEWSTATE\" value=\"", "\"", siteResponse);
		addressSearchEventValid = StringUtils.getTextBetweenDelimiters("\"__EVENTVALIDATION\" value=\"", "\"", siteResponse);

		
		//goto pid search
		req = new HTTPRequest("http://www2.hamiltontn.gov/TrusteeInquiry/AppFolder/PropertySearch.aspx");
		req.setMethod( HTTPRequest.POST );

		req.setPostParameter( "__EVENTTARGET" , "ctl00$MainContent$tabMGP");
		req.setPostParameter( "__EVENTARGUMENT" , "");
		req.setPostParameter( "__VIEWSTATE" , tmpViewState);
		req.setPostParameter( "__VIEWSTATEENCRYPTED" , "");
		req.setPostParameter( "__EVENTVALIDATION" , tmpEventValid);
		res = process(req);
		siteResponse = res.getResponseAsString();
		pidSearchViewState = StringUtils.getTextBetweenDelimiters("\"__VIEWSTATE\" value=\"", "\"", siteResponse);
		pidSearchEventValid = StringUtils.getTextBetweenDelimiters("\"__EVENTVALIDATION\" value=\"", "\"", siteResponse);
		
		loggingIn = false;
		
		return new LoginResponse(LoginStatus.STATUS_SUCCESS, "Logged in");
	}
	
	public void onBeforeRequest(HTTPRequest req) {
		if( loggingIn ) {
			return;
		}
		
		if( req.getPostFirstParameter( "ctl00$MainContent$cmdLName_Search" ) != null ) {
			req.removePostParameters( "__VIEWSTATE" );
			req.removePostParameters( "__EVENTVALIDATION" );
			
			req.setPostParameter( "__VIEWSTATE" , nameSearchViewState);
			req.setPostParameter( "__EVENTVALIDATION" , nameSearchEventValid);
		} else if (req.getPostFirstParameter( "ctl00$MainContent$cmdPropAddress_Search" ) != null) {
			req.removePostParameters( "__VIEWSTATE" );
			req.removePostParameters( "__EVENTVALIDATION" );
			
			req.setPostParameter( "__VIEWSTATE" , addresSearchViewState);
			req.setPostParameter( "__EVENTVALIDATION" , addressSearchEventValid);			
		} else if (req.getPostFirstParameter( "ctl00$MainContent$cmdMGP_Search" ) != null) {
			req.removePostParameters( "__VIEWSTATE" );
			req.removePostParameters( "__EVENTVALIDATION" );
			
			req.setPostParameter( "__VIEWSTATE" , pidSearchViewState);
			req.setPostParameter( "__EVENTVALIDATION" , pidSearchEventValid);			
		}
	}
}
