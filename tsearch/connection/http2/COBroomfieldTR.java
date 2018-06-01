/**
 * 
 */
package ro.cst.tsearch.connection.http2;

import static ro.cst.tsearch.connection.http.HTTPRequest.GET;

import org.apache.commons.httpclient.protocol.Protocol;

import ro.cst.tsearch.connection.LoginResponse;
import ro.cst.tsearch.connection.LoginResponse.LoginStatus;
import ro.cst.tsearch.connection.http.HTTPRequest;
import ro.cst.tsearch.connection.http.auth.ssl.EasySSLProtocolSocketFactory;


public class COBroomfieldTR extends HttpSite {

	@SuppressWarnings("deprecation")
	@Override
	public LoginResponse onLogin() {

		Protocol easyhttps = new Protocol("https", new EasySSLProtocolSocketFactory(), 443);
		Protocol.registerProtocol("https", easyhttps);
		
		// go to taxes search 
		HTTPRequest req = new HTTPRequest(getSiteLink(), GET);
		
		@SuppressWarnings("unused")
		String p = execute(req);
		
		return new LoginResponse(LoginStatus.STATUS_SUCCESS, "Logged in");
	}
	
	public String getPage(String link) {
		HTTPRequest req = new HTTPRequest(link);
		
		return execute(req);
	}
	
}
