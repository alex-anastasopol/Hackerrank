package ro.cst.tsearch.connection.http2;

import java.util.Map;

import ro.cst.tsearch.connection.LoginResponse;
import ro.cst.tsearch.connection.LoginResponse.LoginStatus;
import ro.cst.tsearch.connection.http.HTTPRequest;
import ro.cst.tsearch.connection.http.HTTPResponse;

public class GenericDTSConnRO extends HttpSite {
	
	public LoginResponse onLogin() {
		
		// get the search page
		HTTPRequest req = new HTTPRequest(getSiteLink());
		
		HTTPResponse res = process(req);
		String response = res.getResponseAsString();

		if(response.indexOf("DATA ONLY") < 0) {
			return new LoginResponse(LoginStatus.STATUS_INVALID_PAGE, "Login failed");
		}
		return new LoginResponse(LoginStatus.STATUS_SUCCESS, "Logged in");

	}

	@SuppressWarnings("unchecked")
	@Override
	public void onBeforeRequestExcl(HTTPRequest req) {
		
		String url = req.getURL();	
		url = url.replaceAll("&amp;(amp;)*","&");
		if (req.getMethod() == HTTPRequest.POST){
			if (req.getPostFirstParameter("fromDate") != null){
				String fromDate = req.getPostFirstParameter("fromDate");
				if (!"".equals(fromDate)){
					String[] dateParts = fromDate.split("\\s+");
					if (dateParts.length == 3){
						req.setPostParameter("StartMonth", dateParts[0]);
						req.setPostParameter("StartDay", dateParts[1].replace(",", ""));
						req.setPostParameter("StartYear", dateParts[2]);
						req.removePostParameters("fromDate");
					}
				}
			}
			if (req.getPostFirstParameter("toDate") != null){
				String toDate = req.getPostFirstParameter("toDate");
				if (!"".equals(toDate)){
					String[] dateParts = toDate.split("\\s+");
					if (dateParts.length == 3){
						req.setPostParameter("EndMonth", dateParts[0]);
						req.setPostParameter("EndDay", dateParts[1].replace(",", ""));
						req.setPostParameter("EndYear", dateParts[2]);
						req.removePostParameters("toDate");
					}
				}
			}
			if (req.getPostFirstParameter("navig") != null){
				Map<String,String> navParams = (Map<String, String>)getTransientSearchAttribute("paramsNav:");
				req.removePostParameters("navig");
				if (navParams != null){
					req.addPostParameters(navParams);
				}
			} else { 
				if (req.getPostFirstParameter("DocTypeCats") != null){
					if ("".equals(req.getPostFirstParameter("DocTypeCats"))){
						req.removePostParameters("DocTypeCats");
					}
				}
				if (req.getPostFirstParameter("DocTypes") != null){
					if ("".equals(req.getPostFirstParameter("DocTypes"))){
						req.removePostParameters("DocTypes");
					}
				}
			}
			if (req.getPostFirstParameter("DocTypes") != null){
				if (!"".equals(req.getPostFirstParameter("DocTypes")) && req.getPostFirstParameter("DocTypes").contains(",")){
					String[] doctypes = req.getPostFirstParameter("DocTypes").split("\\s*,\\s*");
					req.removePostParameters("DocTypes");
					for (String doctype : doctypes) {
						req.setPostParameter("DocTypes", doctype);
					}
				}
			}
		}
		
	}
}
