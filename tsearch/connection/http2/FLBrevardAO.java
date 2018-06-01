package ro.cst.tsearch.connection.http2;

import ro.cst.tsearch.connection.LoginResponse;
import ro.cst.tsearch.connection.http.HTTPRequest;
import ro.cst.tsearch.servers.bean.DataSite;

/**
 * Sep 25, 2012
 * @author Oprina George
 *   
 * May 1, 2013 - site changed
 * @author Olivia V
 * 
 */

public class FLBrevardAO extends HttpSite {

	public static String SERVER_LINK = "https://www.bcpao.us";

	public LoginResponse onLogin() {
		DataSite dataSite = getDataSite();
		
		try {
			String url = dataSite.getLink();
			
			HTTPRequest req = new HTTPRequest(url, HTTPRequest.GET);	
			String resp = execute(req);

			if (resp.contains("Search Real Estate Records by"))
				return LoginResponse.getDefaultSuccessResponse();

		} catch (Exception e) {
			e.printStackTrace();
		}

		return LoginResponse.getDefaultFailureResponse();
	}

	
	public void onBeforeRequestExcl(HTTPRequest req) {
        String url = req.getURL();  
        
        if (url.contains("Show_parcel")) { 	//details page requests
        		req.setMethod(HTTPRequest.GET);
        		
        } else { //req for searches & links: next, prev,
    		req.setMethod(HTTPRequest.POST);
    	} 
	}  

}
