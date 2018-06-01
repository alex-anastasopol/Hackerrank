package ro.cst.tsearch.connection.http2;

import ro.cst.tsearch.connection.LoginResponse;
import ro.cst.tsearch.connection.LoginResponse.LoginStatus;
import ro.cst.tsearch.connection.http.HTTPRequest;
import ro.cst.tsearch.servers.bean.DataSite;

public class MDCharlesTR extends HttpSite {

	@Override
	public String getUserAgentValue() {
		return "Mozilla/5.0 (Windows NT 5.1; rv:18.0) Gecko/20100101 Firefox/18.0";
	}
	
	public String getAcceptLang() {
		return "en-US,en;q=0.5"; 
	}
	public String getContentType () {
		return "application/x-www-form-urlencoded"; 
	}

	
	@Override
	public void onBeforeRequestExcl(HTTPRequest req) {
        String currentRequest = req.getURL();  
		
        req.setHeader("User-Agent", getUserAgentValue());
        req.setHeader("Accept-Language", getAcceptLang());
        req.setHeader("Content-Type", getContentType());

        if (currentRequest.contains("addressList.jsp")) {
        	if (!currentRequest.contains("?")) {
        		//searching requests
        		req.setMethod(HTTPRequest.POST);
        	} else {
        		//req for links: next, prev, first, last
        		req.setMethod(HTTPRequest.GET);
        	}
        } else if (currentRequest.contains("selection.jsp") || currentRequest.contains("display.jsp") || currentRequest.contains("legal.jsp")
        		|| currentRequest.contains("taxPeriod.jsp") || currentRequest.contains("taxType.jsp")) {
        	//req for: details page, legal desc, legend of tax codes
        	req.setMethod(HTTPRequest.GET);
        }
	}

	@Override
	public LoginResponse onLogin() {
		DataSite dataSite = getDataSite();
				
        try{	        
	        HTTPRequest req = new HTTPRequest(dataSite.getLink(), HTTPRequest.GET);	
	        String response = execute(req);
	        
	        if(!response.contains("Property Tax Account Inquiry")){
	        	return new LoginResponse(LoginStatus.STATUS_INVALID_PAGE, "Official site not functional");
	        }
        }
        
        catch(Exception e){
        	e.printStackTrace();
        	logger.error("Problem parsing form on MDCharlesTR:", e);
        }       
        return new LoginResponse(LoginStatus.STATUS_SUCCESS, "Logged in");
	}
	
}
