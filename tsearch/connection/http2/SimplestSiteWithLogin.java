package ro.cst.tsearch.connection.http2;

import org.apache.commons.httpclient.protocol.Protocol;

import ro.cst.tsearch.connection.LoginResponse;
import ro.cst.tsearch.connection.http.HTTPRequest;
import ro.cst.tsearch.connection.http.auth.ssl.EasySSLProtocolSocketFactory;

public class SimplestSiteWithLogin extends SimplestSite {
	@Override
	public LoginResponse onLogin() {
		
		Protocol easyhttps = new Protocol("https", new EasySSLProtocolSocketFactory(), 443);
        Protocol.registerProtocol("https", easyhttps);
        
		HTTPRequest req = new HTTPRequest(getSiteLink());
		
		try {
			execute(req);
		} catch (Exception e) {
			logger.error("Error loggin in for site " + getDataSite().getName() + " and link " + getSiteLink(), e);
			return LoginResponse.getDefaultFailureResponse();
		}
		// indicate success
		return LoginResponse.getDefaultSuccessResponse();
		
		
	}
}
