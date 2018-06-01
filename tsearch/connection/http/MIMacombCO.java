package ro.cst.tsearch.connection.http;

import ro.cst.tsearch.connection.LoginResponse;
import ro.cst.tsearch.connection.LoginResponse.LoginStatus;
import ro.cst.tsearch.utils.InstanceManager;


public class MIMacombCO extends HTTPSite {
	
	boolean onLogin=false;
	
	public LoginResponse onLogin() {
		
        setUserData( InstanceManager.getManager().getCurrentInstance(searchId).getCrtSearchContext().getSearchID() + "MIMacombCO" );
		onLogin = true;

		HTTPRequest req = new HTTPRequest( "http://maxweb01.macombcountymi.gov/pa/CRTVNameSearch.html" );
		HTTPResponse res = process( req );
		String siteResponse = res.getResponseAsString();
		onLogin = false;
		return new LoginResponse(LoginStatus.STATUS_SUCCESS, "Logged in");
	}
	
	public void onBeforeRequest(HTTPRequest req) {

        setUserData( InstanceManager.getManager().getCurrentInstance(searchId).getCrtSearchContext().getSearchID() + "MIMacombCO" );
		
	}
}
