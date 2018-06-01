package ro.cst.tsearch.connection.http;

import ro.cst.tsearch.connection.LoginResponse;
import ro.cst.tsearch.connection.LoginResponse.LoginStatus;
import ro.cst.tsearch.utils.InstanceManager;

public class KYJeffersonTR extends HTTPSite{

	@Override
	public LoginResponse onLogin() {

        setUserData( InstanceManager.getManager().getCurrentInstance(searchId).getCrtSearchContext().getSearchID() + "KYJeffersonTR" );
        
		boolean testLog =false;
		HTTPRequest req = new HTTPRequest( "http://www.jcsoky.org/ptax_search.htm" );
        req.setMethod( HTTPRequest.GET );
        req.noRedirects = true;        
        HTTPResponse res = process( req );
        
        if (res.getReturnCode() == 200 ){
        	testLog = true;
        }
       
        req = new HTTPRequest( "http://www.jcsoky.org/ptax_search_name.asp" );
        req.setMethod( HTTPRequest.GET );
        req.noRedirects = true;        
        res = process( req );

        if (res.getReturnCode() == 200 && testLog ){
        	return new LoginResponse(LoginStatus.STATUS_SUCCESS, "Logged in");
        }
        
        return new LoginResponse(LoginStatus.STATUS_INVALID_PAGE, "Login failed");
	}

	
	 public void onBeforeRequest(HTTPRequest req)
	 {
         setUserData( InstanceManager.getManager().getCurrentInstance(searchId).getCrtSearchContext().getSearchID() + "KYJeffersonTR" );
	 }
	
}
