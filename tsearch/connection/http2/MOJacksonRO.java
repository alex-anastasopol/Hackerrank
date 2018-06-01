package ro.cst.tsearch.connection.http2;

import ro.cst.tsearch.connection.LoginResponse;
import ro.cst.tsearch.connection.LoginResponse.LoginStatus;
import ro.cst.tsearch.connection.http.HTTPRequest;
import ro.cst.tsearch.connection.http.HTTPResponse;
import ro.cst.tsearch.utils.StringUtils;

public class MOJacksonRO extends HttpSite {

	@Override
	public LoginResponse onLogin() {
		HTTPRequest req = new HTTPRequest(getLoginLink());
		process(req).getResponseAsString();
		
		return new LoginResponse(LoginStatus.STATUS_SUCCESS, "Logged in");
	}


	@Override
	public void onBeforeRequestExcl(HTTPRequest req) {
	
		String url = req.getURL();
		
		req.setHeader("Host", getHeader());
		if (req.getHeader("Referer") == null)
			req.setHeader("Referer", getLoginLink());

		int idx = url.indexOf("?");
		if(idx == -1){
			return;
		}

		String newUrl = url.substring(0, idx) + "?";
		boolean first = true;
		for(String pair: url.substring(idx + 1).split("&")){
			idx = pair.indexOf("=");
			if(idx == -1){
				continue;
			}
			String name = pair.substring(0, idx);
			String value = "";
			if(idx != pair.length() - 1){
				value = pair.substring(idx + 1);
			}
			if(!first){
				newUrl += "&";
			}
			first = false;
			newUrl += name + "=" + StringUtils.urlEncode(StringUtils.urlDecode(value));
		}
		
		req.modifyURL(newUrl);
	}
	
	@Override
	public void onAfterRequest(HTTPRequest req, HTTPResponse res) {
		if(status != STATUS_LOGGING_IN && (res == null || res.returnCode == 0)){
			destroySession();    		
		}
	}
	
	private String getLoginLink(){
		return getSiteLink();
	}
	
	private String getHeader(){
		String loginLink = getSiteLink();
		
		int index = loginLink.indexOf("http://");
		if(index >= 0) {
			loginLink = loginLink.substring(index  + "http://".length());
		}
		index = loginLink.indexOf("/");
		if(index >= 0) {
			loginLink = loginLink.substring(0,index);
		}
		
		return loginLink;
	}

}
