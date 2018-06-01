package ro.cst.tsearch.connection.http;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import ro.cst.tsearch.connection.ATSConn;
import ro.cst.tsearch.connection.ATSConnConstants;
import ro.cst.tsearch.connection.BridgeConn;
import ro.cst.tsearch.connection.LoginResponse;
import ro.cst.tsearch.connection.LoginResponse.LoginStatus;
import ro.cst.tsearch.data.SitesPasswords;
import ro.cst.tsearch.utils.InstanceManager;

public class TNShelbyDN extends HTTPSite {
	
	private static String firstPage = "http://www.memphisdailynews.com/";
	private static String logInUrl = "http://www.memphisdailynews.com/Default.aspx";
	private static String searchPage = "http://www.memphisdailynews.com/NASearch.aspx";
	private String viewState = "";
	private String eventValidation = "";
	
	private boolean loggingIn = false;

	public LoginResponse onLogin() {
		//setUserData(InstanceManager.getManager().getCurrentInstance(searchId).getCrtSearchContext().getSearchID() + "TNShelbyDN");
		loggingIn = true;
		
		HTTPRequest req = new HTTPRequest( firstPage );
		
		HTTPResponse res = process( req );
		
		String pageData = res.getResponseAsString();
		
		
        viewState = getParam("__VIEWSTATE", pageData, 20);
        eventValidation = getParam("__EVENTVALIDATION", pageData, 26);


        req = new HTTPRequest(logInUrl);
        
		//req.noRedirects = true;
        req.setMethod( HTTPRequest.POST );
        
        req.setPostParameter("ctl00$ContentPane$tbUsername", SitesPasswords.getInstance().getPasswordValue(getCurrentCommunityId(), "TNShelbyDN", "user"));
        req.setPostParameter("ctl00$ContentPane$tbPassword", SitesPasswords.getInstance().getPasswordValue(getCurrentCommunityId(), "TNShelbyDN", "password"));
        req.setPostParameter("__EVENTTARGET","");
        req.setPostParameter("__EVENTARGUMENT","");
        req.setPostParameter("__VIEWSTATE", viewState);
        req.setPostParameter("__EVENTVALIDATION", eventValidation);
        req.setPostParameter("ctl00$ContentPane$btnLogin","Login");
        req.setPostParameter("ctl00$ContentPane$tbFName", "");
        req.setPostParameter("ctl00$ContentPane$tbLname","");
        req.setPostParameter("ctl00$ContentPane$tbCompany","");
        req.setPostParameter("ctl00$ContentPane$cbRemember","");
        req.setPostParameter("ctl00$ContentPane$tbEmail","");
        req.setPostParameter("__SCROLLPOSITIONX","0");
        req.setPostParameter("__SCROLLPOSITIONY","0");
        
        
        req.setHeader("Accept","text/xml,application/xml,application/xhtml+xml,text/html;q=0.9,text/plain;q=0.8,image/png,*/*;q=0.5");
        req.setHeader("Accept-Encoding", "gzip, deflate");
        req.setHeader("Accept-Language","en-us,en");
        req.setHeader("Connection","Keep-Alive");
        req.setHeader("User-Agent", "Mozilla/5.0 (X11; U; SunOS i86pc; en-US; rv:1.8.1.1) Gecko/20061228 Firefox/2.0.0.1");
        req.setHeader("Content-Type", "application/x-www-form-urlencoded");
        req.setHeader("Host", "www.memphisdailynews.com");
        req.setHeader("Referer", firstPage);
        req.noRedirects = true;
        res = process( req );
        
        pageData = res.getResponseAsString();
        viewState = getParam("__VIEWSTATE", pageData, 20);
        eventValidation = getParam("__EVENTVALIDATION", pageData, 26);

        //go to first page
        req = new HTTPRequest( firstPage );
        req.noRedirects = true;
		res = process( req );

		pageData = res.getResponseAsString();
        
        req = new HTTPRequest(searchPage);
        req.setMethod(HTTPRequest.GET);
        
        
        /*req.setPostParameter( "__EVENTTARGET", "ctl00$ContentPane$lbPropertySearch" );
        req.setPostParameter( "__EVENTARGUMENT", "" );
        req.setPostParameter( "__VIEWSTATE", viewState );
        req.setPostParameter( "ctl00$ContentPane$tbFirstName", "" );
        req.setPostParameter( "ctl00$ContentPane$tbLastName", "" );
        req.setPostParameter( "ctl00$ContentPane$tbCompany", "" );
        req.setPostParameter( "ctl00$ContentPane$cbPrimary", "on" );
        req.setPostParameter( "ctl00$ContentPane$tbFrom", "" );
        req.setPostParameter( "ctl00$ContentPane$tbTo", "" );
        req.setPostParameter( "ctl00$ContentPane$hfState", "" );
        req.setPostParameter( "ctl00$RightPane$tbCrimeAddr", "" );
        req.setPostParameter( "ctl00$RightPane$tbCrimeCity", "" );
        req.setPostParameter( "ctl00$RightPane$tbCrimeZip", "" );
        req.setPostParameter( "ctl00$RightPane$Report", "rbRep4" );
        req.setPostParameter( "ctl00$RightPane$tbPromo", "" );
        req.setPostParameter( "__EVENTVALIDATION", eventValidation );
        req.setPostParameter( "__LASTFOCUS", "");
        req.setPostParameter( "__SCROLLPOSITIONX", "0");
        req.setPostParameter( "__SCROLLPOSITIONY", "68");
        req.setPostParameter( "ctl00$ContentPane$ctl01", "on");
        */
        req.setHeader("Referer", searchPage);
        
        process(req);
        
        pageData = res.getResponseAsString();
        
        loggingIn = false;
        
        return new LoginResponse(LoginStatus.STATUS_SUCCESS, "Logged in");
	}

	public void onBeforeRequest(HTTPRequest req) {
		
		if( loggingIn ){
			return;
		}

		if( !"".equals( viewState ) ){
			req.removePostParameters( "__VIEWSTATE" );
			
			req.setPostParameter( "__VIEWSTATE" , viewState);
		}
		
		if( !"".equals( eventValidation ) ){
			req.removePostParameters( "__EVENTVALIDATION" );
			
			req.setPostParameter( "__EVENTVALIDATION" , eventValidation);
		}

		System.currentTimeMillis();
	}

    private String getParam( String paramName, String pageData, int length)
    {
        int istart = pageData.indexOf(paramName + "\" value=\"");
		int iend = pageData.indexOf("\"", istart + length);
		String s = pageData.substring(istart + length, iend);
        
        return s;
    }

}
