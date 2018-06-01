package ro.cst.tsearch.connection.http;

import java.util.HashMap;

import org.apache.commons.httpclient.Credentials;
import org.apache.commons.httpclient.HttpMethod;
import org.apache.commons.httpclient.UsernamePasswordCredentials;
import org.apache.commons.httpclient.auth.AuthScope;

import ro.cst.tsearch.connection.LoginResponse;
import ro.cst.tsearch.connection.LoginResponse.LoginStatus;
import ro.cst.tsearch.data.SitesPasswords;
import ro.cst.tsearch.database.DBManager;
import ro.cst.tsearch.utils.HttpUtils;
import ro.cst.tsearch.utils.InstanceManager;
import ro.cst.tsearch.utils.StringUtils;
import ro.cst.tsearch.utils.URLMaping;


public class TNHamiltonRO extends HTTPSite {

	public String viewStatePartySearch = "";
	public String eventValidationPartySearch = "";

	public String viewStateBookPage = "";
	public String eventValidationBookPage = "";
	
	public String viewStateProperty = "";
	public String eventValidationProperty = "";
	
	public String viewStateImgLookup = "";
	public String eventValidationImgLookup = "";
	
	private boolean loggingIn = false;
	
	public LoginResponse onLogin()
	{
		String user = "user_0";
		String password = "password_0";
		// it seams that this is the only one that works
		if (URLMaping.INSTANCE_DIR.equals("ats01")){
			user = "user_8";
			password = "password_8";
		} else if (URLMaping.INSTANCE_DIR.equals("ats02")){
			user = "user_8";
			password = "password_8";
		} else if (URLMaping.INSTANCE_DIR.equals("ats03")){
			user = "user_8";
			password = "password_8";
		}
		
		setUserData( InstanceManager.getManager().getCurrentInstance(searchId).getCrtSearchContext().getSearchID() + "TNHamiltonRO" );

		loggingIn = true;
		
		//go to login page
		HTTPRequest req = new HTTPRequest("http://register.hamiltontn.gov/OnlineRecordSearch/Login.aspx?ReturnUrl=%2fonlinerecordsearch%2fDefault.aspx");
		HTTPResponse res = process( req );
		String siteResponse = res.getResponseAsString();

		HashMap<String, String> params = HttpUtils.getFormParams( siteResponse, false );
		req = new HTTPRequest( "http://register.hamiltontn.gov/OnlineRecordSearch/Login.aspx?ReturnUrl=%2fonlinerecordsearch%2fDefault.aspx" );
		req.setMethod( HTTPRequest.POST );
		req.setPostParameter( "__VIEWSTATE" , params.get( "__VIEWSTATE" ) );
		req.setPostParameter( "txtUsername" , SitesPasswords.getInstance().getPasswordValue(getCurrentCommunityId(), "TNHamiltonRO", user ) );
		req.setPostParameter( "txtPassword" , SitesPasswords.getInstance().getPasswordValue(getCurrentCommunityId(), "TNHamiltonRO", password ) );
		req.setPostParameter( "btnLogin" , "Login" );
		req.setPostParameter( "__EVENTVALIDATION" , params.get( "__EVENTVALIDATION" ) );
		res = process( req );
		siteResponse = res.getResponseAsString();
		
		//go to party search page to get viewstate and eventvalidation
		req = new HTTPRequest( "http://register.hamiltontn.gov/OnlineRecordSearch/RecordSearch/PartySearch.aspx" );
		res = process( req );
		siteResponse = res.getResponseAsString();
		
		params = HttpUtils.getFormParams( siteResponse , false);
		viewStatePartySearch = params.get( "__VIEWSTATE" );
		eventValidationPartySearch = params.get( "__EVENTVALIDATION" );
		
		//go to book page search page to get viewstate and eventvalidation
		req = new HTTPRequest( "http://register.hamiltontn.gov/OnlineRecordSearch/RecordSearch/BookPageSearch.aspx" );
		res = process( req );
		siteResponse = res.getResponseAsString();
		
		params = HttpUtils.getFormParams( siteResponse , false);
		viewStateBookPage = params.get( "__VIEWSTATE" );
		eventValidationBookPage = params.get( "__EVENTVALIDATION" );
		
		//go to property search page to get the viewstate and eventvalitation
		req = new HTTPRequest( "http://register.hamiltontn.gov/OnlineRecordSearch/RecordSearch/PropertySearch.aspx" );
		res = process( req );
		siteResponse = res.getResponseAsString();
		
		params = HttpUtils.getFormParams( siteResponse , false);
		viewStateProperty = params.get( "__VIEWSTATE" );
		eventValidationProperty = params.get( "__EVENTVALIDATION" );
		
		//go to image lookup page and get viewstate and eventvalidation
		req = new HTTPRequest( "http://register.hamiltontn.gov/OnlineRecordSearch/RecordSearch/ImageSearch.aspx" );
		res = process( req );
		siteResponse = res.getResponseAsString();
		
		params = HttpUtils.getFormParams( siteResponse , false); 
		viewStateImgLookup = params.get( "__VIEWSTATE" );
		eventValidationImgLookup = params.get( "__EVENTVALIDATION" );
		
		loggingIn = false;
		
		return new LoginResponse(LoginStatus.STATUS_SUCCESS, "Logged in");
	}
		
	public void onBeforeRequest(HTTPRequest req)
	{
		setUserData( InstanceManager.getManager().getCurrentInstance(searchId).getCrtSearchContext().getSearchID() + "TNHamiltonRO" );
		
		if( loggingIn ) {
			return;
		}
		
		if( req.getPostFirstParameter( "__EVENTTARGET" ) == null ) {
			req.setPostParameter( "__EVENTTARGET" , "");
		}
		if (req.getPostFirstParameter( "__EVENTARGUMENT" ) == null ) {
			req.setPostParameter( "__EVENTARGUMENT" , "");			
		}
		
		req.setPostParameter( "__VIEWSTATEENCRYPTED" , "");
		
		if( req.getPostFirstParameter( "__VIEWSTATE" ) == null && req.getPostFirstParameter( "__EVENTVALIDATION" ) == null ) {
			if( req.URL.contains( "PartySearch" ) ) {
				req.setPostParameter( "__VIEWSTATE" , viewStatePartySearch);
				req.setPostParameter( "__EVENTVALIDATION" , eventValidationPartySearch);
			} else if ( req.URL.contains( "BookPageSearch" ) ) {
				req.setPostParameter( "__VIEWSTATE" , viewStateBookPage);
				req.setPostParameter( "__EVENTVALIDATION" , eventValidationBookPage);			
			} else if ( req.URL.contains( "PropertySearch" ) ) {
				req.setPostParameter( "__VIEWSTATE" , viewStateProperty);
				req.setPostParameter( "__EVENTVALIDATION" , eventValidationProperty);			
			} else if ( req.URL.contains( "ImageSearch" ) ) {
				req.setPostParameter( "__VIEWSTATE" , viewStateImgLookup);
				req.setPostParameter( "__EVENTVALIDATION" , eventValidationImgLookup);			
			}
		}
		
		if ( req.URL.contains( "BookPageSearch" ) ) {
			String instrument = req.getPostFirstParameter("ctl00$MainContent$txtInstrumentNo");
			String book = req.getPostFirstParameter("ctl00$MainContent$txtBookNo");
			String docType = req.getPostFirstParameter( "ctl00$MainContent$ddlDocumentTypes" );
			String bookType = req.getPostFirstParameter( "ctl00$MainContent$ddlBookTypes");
			String page = req.getPostFirstParameter("ctl00$MainContent$txtPageNo");
			if( !StringUtils.isEmpty(instrument)) {
				req.removePostParameters( "ctl00$MainContent$btnSearch" );
				req.setPostParameter("ctl00$MainContent$btnSearchInst", "Search");				
			}else if( docType != null && !"0".equals( docType ) ) {
				req.removePostParameters( "ctl00$MainContent$btnSearch" );
				req.setPostParameter("ctl00$MainContent$btnSearchDocType", "Search");
			} 
		}
		
		if( req.getPostFirstParameter( "ctl00$MainContent$ddlSubDivisions" ) != null ) {
			//search by subdiv
			String subdivCode = req.getPostFirstParameter( "ctl00$MainContent$ddlSubDivisions" );
			String subdivName = DBManager.getHamiltonSubdivByCode( subdivCode );
			
			if( !"".equals( subdivName ) ) {
				req.setPostParameter( "ctl00$MainContent$txtSubdivision" , subdivName);
				req.setPostParameter( "ctl00$MainContent$txtSubdivisionID" , subdivCode);
				
				req.removePostParameters( "ctl00$MainContent$ddlSubDivisions" );
			}
			
		}
		
	}
	
	
	public HTTPUser getUser(String sid)
	{
		String userName = SitesPasswords.getInstance().getPasswordValue(getCurrentCommunityId(), "TNHamiltonRO", "user_" + sid );
		String password = SitesPasswords.getInstance().getPasswordValue(getCurrentCommunityId(), "TNHamiltonRO", "password_" + sid );
		
		if( !"".equals( userName ) ){
			return new HTTPUser( userName, password );
		}
		
		return null;
	}

}
