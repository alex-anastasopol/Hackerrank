package ro.cst.tsearch.connection.http;

import ro.cst.tsearch.connection.LoginResponse;
import ro.cst.tsearch.connection.LoginResponse.LoginStatus;
import ro.cst.tsearch.utils.InstanceManager;
import ro.cst.tsearch.utils.StringUtils;

public class KYJeffersonMS extends HTTPSite {

	
	public LoginResponse onLogin() {
		
		setUserData( InstanceManager.getManager().getCurrentInstance(searchId).getCrtSearchContext().getSearchID() + "KYJeffersonMS" );
		
		HTTPRequest req = new HTTPRequest( "http://msdlouky.org/msdwarrants" );
        req.setMethod( HTTPRequest.GET );
        req.noRedirects = false;        
        HTTPResponse res = process( req );
        String strdata = res.getResponseAsString();  
		
        req = new HTTPRequest( "http://msdlouky.org/msdwarrants/" );
        req.setMethod( HTTPRequest.GET );
        req.noRedirects = false;        
         res = process( req );
       strdata = res.getResponseAsString();  
		
        if(res.getReturnCode() == 301){
        	
        }
       
        return new LoginResponse(LoginStatus.STATUS_SUCCESS, "Logged in");
	}
	
	@Override
	public void onBeforeRequest(HTTPRequest req) {
		if(req.hasPostParameter("flg")) {
			String url = req.getURL();
			url += "?flg=" + req.getPostFirstParameter("flg");
			req.modifyURL(url);
			req.removePostParameters("flg");
		}
	}

}
