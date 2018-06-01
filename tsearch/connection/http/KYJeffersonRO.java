package ro.cst.tsearch.connection.http;

import ro.cst.tsearch.connection.LoginResponse;
import ro.cst.tsearch.connection.LoginResponse.LoginStatus;
import ro.cst.tsearch.utils.InstanceManager;

public class KYJeffersonRO extends HTTPSite
{
    public LoginResponse onLogin()
    {
        setUserData( InstanceManager.getManager().getCurrentInstance(searchId).getCrtSearchContext().getSearchID() + "KYJeffersonRO" );
        
        //go to main page
        HTTPRequest req = new HTTPRequest( "http://www.landrecords.jcc.ky.gov/" );
        HTTPResponse res = process( req );
        
        //go to search index
        req = new HTTPRequest( "http://www.landrecords.jcc.ky.gov/records/S0Search.html" );
        res = process( req );
        
        //go to search by party name link
        req = new HTTPRequest( "http://www.landrecords.jcc.ky.gov/records/S2Search.jsp" );
        res = process( req );
        
        return new LoginResponse(LoginStatus.STATUS_SUCCESS, "Logged in");
    }
    
    
    public void onBeforeRequest(HTTPRequest req)
    {
        setUserData( InstanceManager.getManager().getCurrentInstance(searchId).getCrtSearchContext().getSearchID() + "KYJeffersonRO" );
        
        ///cgi-bin/webview/
        
        String url = req.getURL();
        if( url.indexOf( "cgi-bin/webview/" ) >= 0 )
        {
            //get the image
            url = url.replace( "http://www.landrecords.jcc.ky.gov/", "" );
            req.URL = url;
        }
    }
}