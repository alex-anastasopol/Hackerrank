package ro.cst.tsearch.connection.http3;

import java.io.IOException;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.http.cookie.Cookie;

import ro.cst.tsearch.connection.LoginResponse;
import ro.cst.tsearch.connection.LoginResponse.LoginStatus;
import ro.cst.tsearch.connection.http.HTTPRequest;
import ro.cst.tsearch.connection.http.HTTPResponse;

public class OHFranklinTR2 extends HttpSite3 {

	/**
	 * @author MihaiB
	 * 
	 */
	private String JSESSIONID = "";
	
	@Override
	public LoginResponse onLogin() {

		HTTPResponse response;
		HTTPRequest request = new HTTPRequest(getSiteLink(), HTTPRequest.GET);
		try {
			response = exec(request);
		} catch (IOException e) {
			return new LoginResponse(LoginStatus.STATUS_INVALID_PAGE, "Login Page not found");
		}
		if (response.getResponseAsString().contains("Property Search")){
			
			List<Cookie> cookies = getCookies();
			if (cookies != null && cookies.size() > 0){
				for (Cookie cookie : cookies) {
					if ("JSESSIONID".equals(cookie.getName())){
						JSESSIONID = cookie.getValue();
						break;
					}
				}
			}
			return new LoginResponse(LoginStatus.STATUS_SUCCESS, "Logged in");
		}
		
		
		return new LoginResponse(LoginStatus.STATUS_INVALID_PAGE, "Login Page not found");
	}
	
	@Override
	public void onBeforeRequestExcl(HTTPRequest req) {
		String url = req.getURL();
		url = url.replaceFirst("(?is)&searchType=name", "");
		url = url.replaceFirst("(?is)&searchType=address", "");
		req.setURL(url);
		
		super.onBeforeRequestExcl(req);
	}
	
	public String getPage(String link, Map<String, String> params) {
		HTTPRequest req = null;
		if (params == null){
			req = new HTTPRequest(link, HTTPRequest.GET);
		} else {
			req = new HTTPRequest(link, HTTPRequest.POST);
			Iterator<String> i = params.keySet().iterator();
			while(i.hasNext()){
				String k = i.next();
				req.setPostParameter(k, params.get(k));
			}
		}
		
		return execute(req);
	}
}
