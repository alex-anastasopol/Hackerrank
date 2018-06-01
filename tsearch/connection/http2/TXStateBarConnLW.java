package ro.cst.tsearch.connection.http2;


import java.util.Map;

import ro.cst.tsearch.connection.LoginResponse;
import ro.cst.tsearch.connection.LoginResponse.LoginStatus;
import ro.cst.tsearch.connection.http.HTTPRequest;



/**
 * @author mihaib
*/

public class TXStateBarConnLW extends HttpSite{


	@Override
	public LoginResponse onLogin() {
		
		setAttribute(SINGLE_LINE_COOKIES_IN_REDIRECT, true);
		
		HTTPRequest req = new HTTPRequest(getSiteLink());
		String response = execute(req);
		
		if (response.indexOf("Find lawyers") == -1)
			return new LoginResponse(LoginStatus.STATUS_UNKNOWN, "Login failed");
		
		
		// indicate success
		return new LoginResponse(LoginStatus.STATUS_SUCCESS, "Logged in");
		
	}
	
	@SuppressWarnings("unchecked")
	@Override
	public void onBeforeRequestExcl(HTTPRequest req) {
    	
		if (req.getMethod() == HTTPRequest.POST){
			if (req.getPostFirstParameter("TBLSCertified") != null){
				if ("".equals(req.getPostFirstParameter("TBLSCertified"))){
					req.removePostParameters("TBLSCertified");
				}
			}
			if (req.getPostFirstParameter("PracticeArea") != null){
				if ("".equals(req.getPostFirstParameter("PracticeArea"))){
					req.removePostParameters("PracticeArea");
				}
			}
			if (req.getPostFirstParameter("ServicesProvided") != null){
				if ("".equals(req.getPostFirstParameter("ServicesProvided"))){
					req.removePostParameters("ServicesProvided");
				}
			}
			if (req.getPostFirstParameter("LanguagesSpoken") != null){
				if ("".equals(req.getPostFirstParameter("LanguagesSpoken"))){
					req.removePostParameters("LanguagesSpoken");
				}
			}
			if (req.getPostFirstParameter("LawSchool") != null){
				if ("".equals(req.getPostFirstParameter("LawSchool"))){
					req.removePostParameters("LawSchool");
				}
			}
			
			String url = req.getURL();
			if (req.getPostFirstParameter("Section") != null){
				url += "?Section=" + req.getPostFirstParameter("Section");
				req.removePostParameters("Section");
				
				if (req.getPostFirstParameter("Template") != null){
					url += "&Template=" + req.getPostFirstParameter("Template");
					req.removePostParameters("Template");
				} else if (req.getPostFirstParameter("template") != null){
					url += "&template=" + req.getPostFirstParameter("template");
					req.removePostParameters("template");
				}
				req.setURL(url);
			}
			if (req.getPostFirstParameter("ButtonName") != null){
				Map<String,String> navParams = (Map<String, String>)getTransientSearchAttribute("paramsNav:");
				if (navParams != null){
					req.addPostParameters(navParams);
				}
			}
		}
			
	}
	
	
}