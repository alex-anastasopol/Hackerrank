package ro.cst.tsearch.connection.http2;

import org.apache.commons.httpclient.HttpStatus;
import org.apache.commons.httpclient.protocol.Protocol;

import ro.cst.tsearch.connection.LoginResponse;
import ro.cst.tsearch.connection.LoginResponse.LoginStatus;
import ro.cst.tsearch.connection.http.HTTPRequest;
import ro.cst.tsearch.connection.http.HTTPResponse;
import ro.cst.tsearch.connection.http.auth.ssl.EasySSLProtocolSocketFactory;

public class TXGenericInsuranceDepartment extends HttpSite {
	@Override
	public LoginResponse onLogin() {
		String siteLink = getSiteLink();
		HTTPRequest request = new HTTPRequest(siteLink);
		request.setMethod(HTTPRequest.GET);
		
		Protocol easyhttps = new Protocol("https", new EasySSLProtocolSocketFactory(), 443);
        Protocol.registerProtocol("https", easyhttps);
		
		HTTPResponse process = process(request);
		LoginResponse response = new LoginResponse(LoginStatus.STATUS_UNKNOWN, "The first page to be accessed is unreachable");
		
		if (process.getReturnCode() == HttpStatus.SC_OK){
			response = new LoginResponse(LoginStatus.STATUS_SUCCESS, "Logged in");
		}
		return response;
	}
	
}
