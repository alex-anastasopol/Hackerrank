package ro.cst.tsearch.connection.http2;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map.Entry;

import ro.cst.tsearch.connection.LoginResponse;
import ro.cst.tsearch.connection.LoginResponse.LoginStatus;
import ro.cst.tsearch.connection.http.HTTPRequest;
import ro.cst.tsearch.connection.http.HTTPRequest.ParametersVector;
import ro.cst.tsearch.connection.http.HTTPResponse;
import ro.cst.tsearch.parser.HtmlParser3;

public class OHFranklinCOM extends HttpSite {

	public LoginResponse onLogin() {
		String baseUrl = getSiteLink().replaceFirst("(?is)(http://[^/]+).*", "$1");
		HTTPRequest req = new HTTPRequest(baseUrl + "/case/", HTTPRequest.GET);
		String res = execute(req);
		if (res.toString().contains("I HAVE READ AND UNDERSTAND ALL TERMS STATED ABOVE")) {
			HtmlParser3 parser = new HtmlParser3(res.toString());
			HashMap<String,String> paramsOfReq = parser.getListOfPostParams("PA_form");
			if (paramsOfReq != null) {
				req = new HTTPRequest(baseUrl + "/case/index.php", HTTPRequest.POST);
				req.setPostParameter("validate", paramsOfReq.get("validate"));
				req.setPostParameter("agree", "on");
			    req.setMethod( HTTPRequest.POST );
			    //req.noRedirects = false;        
			    res = execute(req);
			}
		
		} else {
			return new LoginResponse(LoginStatus.STATUS_UNKNOWN, "Login failed");
		}
		
		return new LoginResponse(LoginStatus.STATUS_SUCCESS, "Logged in");
		
//	    if(res.getReturnCode() == 200) {
//	    	return new LoginResponse(LoginStatus.STATUS_SUCCESS, "Logged in");
//	    }
//	
//	    return new LoginResponse(LoginStatus.STATUS_UNKNOWN, "Login failed");
	}
	
	
	public void onBeforeRequestExcl(HTTPRequest req) {
        if (req.getURL().contains("results.php")) {
        	String key = "search_fie1d_case_status";
        	for (int i=0; i < 3; i++) {
        		if (i==1) 
        			key = "search_fie1d_party_code";
        		else if (i==2)
        			key = "search_fie1d_case_type";
        		String val = req.getPostFirstParameter(key);
        		if (val != null) {
        			if ("ALL".equals(val)) {
    					req.removePostParameters(key);
    					req.setPostParameter(key, "");
    				}
        		}
        	}
        } 
	}
	
}
