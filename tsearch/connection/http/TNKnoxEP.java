package ro.cst.tsearch.connection.http;

import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpMethod;
import org.apache.commons.httpclient.protocol.Protocol;

import ro.cst.tsearch.connection.LoginResponse;
import ro.cst.tsearch.connection.LoginResponse.LoginStatus;
import ro.cst.tsearch.utils.InstanceManager;

public class TNKnoxEP extends HTTPSite {
	private Protocol myhttps = new Protocol("https", new ro.cst.tsearch.connection.http.auth.ssl.EasySSLProtocolSocketFactory(), 443);
	
	private String pageName = "";
	private boolean loggingIn = false;
	
	public void onHTTPAuth(HTTPSession session) {	
		HttpClient httpClient = session.httpClient;
		httpClient.getHostConfiguration().setHost("secure.cityofknoxville.org", 443, myhttps);
	}
	
	public LoginResponse onLogin() {
		
		setUserData( InstanceManager.getManager().getCurrentInstance(searchId).getCrtSearchContext().getSearchID() + "TNKnoxEP" );

		loggingIn = true;
		
		HTTPRequest req = new HTTPRequest("https://secure.cityofknoxville.org/directprop");
		req.noRedirects = true;
		HTTPResponse res = process( req );
		String siteResponse = res.getResponseAsString();
		
		String gotoLocation = res.getHeader( "Location" );
		req = new HTTPRequest(gotoLocation);
		req.noRedirects = true;
		res = process( req );
		siteResponse = res.getResponseAsString();
		
		gotoLocation = res.getHeader( "location" );
		req = new HTTPRequest(gotoLocation);
		req.noRedirects = true;
		res = process( req );
		siteResponse = res.getResponseAsString();

		gotoLocation = res.getHeader( "location" );
		req = new HTTPRequest(gotoLocation);
		req.noRedirects = true;
		res = process( req );
		siteResponse = res.getResponseAsString();

		gotoLocation = res.getHeader( "location" );
		req = new HTTPRequest(gotoLocation);
		req.noRedirects = true;
		res = process( req );
		siteResponse = res.getResponseAsString();
		
		gotoLocation = res.getHeader( "location" );
		req = new HTTPRequest(gotoLocation);
		req.noRedirects = true;
		res = process( req );
		siteResponse = res.getResponseAsString();
		
		try {
			pageName = gotoLocation.substring( gotoLocation.lastIndexOf("/") + 1 );
		} catch(Exception e) {
			e.printStackTrace();
		}
		
		loggingIn = false;
		
		return new LoginResponse(LoginStatus.STATUS_SUCCESS, "Logged in");
	}
	
    public void onBeforeRequest(HTTPRequest req) {
    	
    	if( loggingIn ) {
    		return;
    	}
    	
    	if( req.URL.contains( "/searchByNameModule" ) || req.URL.contains("/searchByPidModule") ) {
    		req.URL = "https://secure.cityofknoxville.org/" + pageName + "GenericWait.asp";
    		
    		HTTPResponse res = process( req );
    		String siteResponse = res.getResponseAsString();
    		
    		HTTPRequest req2 = new HTTPRequest("https://secure.cityofknoxville.org/Tax_Search/" + pageName + "Execute.asp");
    		req2.noRedirects = true;
    		req2.setMethod( HTTPRequest.GET );
    		res = process(req2);
    		
    		siteResponse = res.getResponseAsString();
    		
    		String gotoLocation = res.getHeader( "Location" );
    		req.modifyURL("https://secure.cityofknoxville.org/Tax_Search/" + gotoLocation);
    		req.setMethod( HTTPRequest.GET );
    		req.clearPostParameters();
    		 
    	} else if( req.URL.contains( "secure." ) ) {
    		req.URL = req.URL.replace( "http://" , "https://");
    	}
    }
}