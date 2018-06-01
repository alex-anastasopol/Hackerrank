package ro.cst.tsearch.connection.http;


import ro.cst.tsearch.connection.LoginResponse;
import ro.cst.tsearch.connection.LoginResponse.LoginStatus;
import ro.cst.tsearch.utils.InstanceManager;


public class MIWayneTR extends HTTPSite {

	public LoginResponse onLogin() {
		
		setUserData( InstanceManager.getManager().getCurrentInstance(searchId).getCrtSearchContext().getSearchID() + "MIWayneTR" );
		
		HTTPRequest req = new HTTPRequest( "http://www.waynecounty.com/pta/Disclaimer.asp" );
		HTTPResponse res = process( req );
		
		String response = res.getResponseAsString();
		
		//accept disclaimer
		req = new HTTPRequest( "http://www.waynecounty.com/pta/Disclaimer.asp" );
		req.setMethod( HTTPRequest.POST );
		
		req.setPostParameter( "accept" , "accept" );
		res = process( req );
		
		response = res.getResponseAsString();
		
		return new LoginResponse(LoginStatus.STATUS_SUCCESS, "Logged in");
	}
	
    public void onBeforeRequest(HTTPRequest req)
    {
        setUserData( InstanceManager.getManager().getCurrentInstance(searchId).getCrtSearchContext().getSearchID() + "MIWayneTR" );
    }
}
