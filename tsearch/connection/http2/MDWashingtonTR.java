package ro.cst.tsearch.connection.http2;

import ro.cst.tsearch.connection.LoginResponse;
import ro.cst.tsearch.connection.LoginResponse.LoginStatus;
import ro.cst.tsearch.connection.http.HTTPRequest;
import ro.cst.tsearch.servers.bean.DataSite;


public class MDWashingtonTR extends HttpSite {
	
	@Override
	public void onBeforeRequestExcl(HTTPRequest req) {
        String url = req.getURL();
        
        if (url.contains("result_parcel_annual.aspx")) {
        	url += "&DistHyphen=-";
        }
        
       	req.setMethod(HTTPRequest.GET);                     
        req.setURL(url);
	}

	
	@Override
	public LoginResponse onLogin() {
		DataSite dataSite = getDataSite();
				
        try{	        
//			@SuppressWarnings("deprecation")
//			Protocol easyhttps = new Protocol("https", new EasySSLProtocolSocketFactory(), 443);
//	        Protocol.registerProtocol("https", easyhttps);	        
	        
	        HTTPRequest req = new HTTPRequest(dataSite.getLink(), HTTPRequest.GET);	   
	        String response = execute(req);
	        
        	if (response.contains("403 - Forbidden: Access is denied.")) {
        		return new LoginResponse(LoginStatus.STATUS_INVALID_PAGE, "Official site not functional");
        	}
        }
        catch(Exception e){
        	e.printStackTrace();
        	logger.error("Problem parsing form on MDWashingtonTR:", e);
        }       
        
        return new LoginResponse(LoginStatus.STATUS_SUCCESS, "Logged in");
	}

}
