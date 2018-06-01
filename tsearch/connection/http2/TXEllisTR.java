package ro.cst.tsearch.connection.http2;

import org.apache.commons.httpclient.protocol.Protocol;

import ro.cst.tsearch.connection.LoginResponse;
import ro.cst.tsearch.connection.LoginResponse.LoginStatus;
import ro.cst.tsearch.connection.http.HTTPRequest;
import ro.cst.tsearch.connection.http.auth.ssl.EasySSLProtocolSocketFactory;
import ro.cst.tsearch.servers.bean.DataSite;

public class TXEllisTR extends HttpSite {
	
	public LoginResponse onLogin() {
		
		Protocol easyhttps = new Protocol("https", new EasySSLProtocolSocketFactory(), 443);
        Protocol.registerProtocol("https", easyhttps);
        
		DataSite dataSite = getDataSite();
        try{	        
	        HTTPRequest req = new HTTPRequest(dataSite.getLink(), HTTPRequest.GET);	
	        String response = execute(req);
//	        if (response.contains("Please re-enter the numbers and letters below to continue")) {
//	        	
//	        }
	        if(!response.contains("Find Your Property Tax Balance")){
	        	return new LoginResponse(LoginStatus.STATUS_INVALID_PAGE, "Official site not functional");
	        }
	        
	        return new LoginResponse(LoginStatus.STATUS_SUCCESS, "Logged in");
	        
        } catch(Exception e){
        	e.printStackTrace();
        	logger.error("Problem connection on TX Ellis TR; Exception is: ", e);
        }
        
		return new LoginResponse(LoginStatus.STATUS_UNKNOWN, "Error found");    
	}
	
}
