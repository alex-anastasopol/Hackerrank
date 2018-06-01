package ro.cst.tsearch.connection.http3;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.Map;

import ro.cst.tsearch.connection.LoginResponse;
import ro.cst.tsearch.connection.LoginResponse.LoginStatus;
import ro.cst.tsearch.connection.http.HTTPRequest;
import ro.cst.tsearch.connection.http.HTTPResponse;

public class OHFranklinRO extends HttpSite3 {
	
	public LoginResponse onLogin() {
		
		// get the search page
		String link = getSiteLink();
		HTTPRequest req = new HTTPRequest(link);
		
		HTTPResponse res = process(req);
		String response = res.getResponseAsString();

		if(response.indexOf("Search for public") > 0) {
			return new LoginResponse(LoginStatus.STATUS_SUCCESS, "Logged in");
			
		}
		return new LoginResponse(LoginStatus.STATUS_INVALID_PAGE, "Login failed");

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
			} else if (req.getPostFirstParameter("UserQuery") != null){
				String value = req.getPostFirstParameter("UserQuery");
				Map<String,String> userQueryParams = (Map<String, String>)getTransientSearchAttribute("UserQuery:" + value);
				req.removePostParameters("UserQuery");
				req.setPostParameter("UserQuery", userQueryParams.get("UserQuery"));
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
		} else if (req.getMethod() == HTTPRequest.GET){
			if (url.indexOf("Summary+Data") > -1 || url.indexOf("Summary Data") > -1){
				try {
					url = URLDecoder.decode(url, "UTF-8");
				} catch (UnsupportedEncodingException e) {
					e.printStackTrace();
				}
				url = url.replaceAll("(?is)&amp;", "&");
				String[] urlParts = url.split("\\?");
				if (urlParts.length == 2){
					req.setURL(urlParts[0]);
					String[] urlParams = urlParts[1].split("&");
					for (String urlParam : urlParams) {
						String[] nameValue = urlParam.split("=");
						if (nameValue.length == 1){
							req.setPostParameter(nameValue[0], "");
						} else if (nameValue.length == 2){
							req.setPostParameter(nameValue[0], nameValue[1]);
						}
					}
				}
	        	req.setMethod(HTTPRequest.POST);
	        	req.setHeader("Referer", "http://recorderweb.co.franklin.oh.us/Rec/SimpleQuery.asp");
	        } else if (url.indexOf("LoadImage") >= 0){
	            //image download link
	            url = url.replaceAll("=", "");
	            req.setURL(url);
	            
	            //set the referer
	            req.setHeader("Referer", "http://recorderweb.co.franklin.oh.us/Rec/SimpleQuery.asp");
	        }
		}
	}
	
	@Override
	public void onAfterRequest(HTTPRequest req, HTTPResponse res) {
		if (res == null || res.returnCode == 0){
			destroySession();
		}
	}
}
