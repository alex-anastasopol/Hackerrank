package ro.cst.tsearch.connection.http2;

import org.apache.commons.httpclient.protocol.Protocol;

import ro.cst.tsearch.connection.LoginResponse;
import ro.cst.tsearch.connection.LoginResponse.LoginStatus;
import ro.cst.tsearch.connection.http.HTTPRequest;
import ro.cst.tsearch.connection.http.auth.ssl.EasySSLProtocolSocketFactory;
import ro.cst.tsearch.servers.bean.DataSite;


public class MDCarrollTR extends HttpSite {

//	private String sid;

	public String getUserAgentValue() {
		return "Mozilla/5.0 (Windows NT 6.1; WOW64; rv:18.0) Gecko/20100101 Firefox/18.0";
	}
	
	@Override
	public void onBeforeRequestExcl(HTTPRequest req) {
		req.setHeader("User-Agent", getUserAgentValue());
		
        String currentRequest = req.getURL();  
        
        // search page: http://ccgovernment.carr.org/ccg/collect/bill-inq.asp
        String url = "";
        if (currentRequest.contains("sdatcert3.resiusa.org")) {  //req on AO site for Task 8690
        	url = currentRequest;
        	req.setMethod(HTTPRequest.GET);
        
        } else {
        	if (!currentRequest.contains("bill-inq.asp")) {
           	 	if (currentRequest.contains("bill-list.asp")) { //interm results
           	 		url = currentRequest.replaceFirst("(?is)(.*)/bill-list.asp", "$1" + "/bill-det.asp");
           	 		req.setMethod(HTTPRequest.POST);
           	 		
           	 	} else if (currentRequest.contains("bill-det.asp")) { //details page
           	 		url = currentRequest;
           	 		req.setMethod(HTTPRequest.GET);
           	 	}
           	 	
           	} else {
         		url = currentRequest.replaceFirst("(?is)(.*)/bill-inq.asp", "$1" + "/bill-list.asp");
           	}
        }
        
        req.setURL(url);
            
        
	}

	
	@Override
	public LoginResponse onLogin() {
		DataSite dataSite = getDataSite();
				
        try{	        
			@SuppressWarnings("deprecation")
			Protocol easyhttps = new Protocol("https", new EasySSLProtocolSocketFactory(), 443);
	        Protocol.registerProtocol("https", easyhttps);	        
	        
	        HTTPRequest req = new HTTPRequest(dataSite.getLink(), HTTPRequest.GET);	   
	        String response = execute(req);
	        
        	if (response.contains("An unexpected error has occurred:")) {
        		return new LoginResponse(LoginStatus.STATUS_INVALID_PAGE, "Official site not functional");
        	}        	 
        }
        catch(Exception e){
        	e.printStackTrace();
        	logger.error("Problem parsing form on MDBaltimoreTR:", e);
        }       
        
        return new LoginResponse(LoginStatus.STATUS_SUCCESS, "Logged in");
	}

}
