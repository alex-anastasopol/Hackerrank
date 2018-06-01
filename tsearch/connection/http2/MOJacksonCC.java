package ro.cst.tsearch.connection.http2;

import ro.cst.tsearch.connection.LoginResponse;
import ro.cst.tsearch.connection.LoginResponse.LoginStatus;
import ro.cst.tsearch.connection.http.HTTPRequest;
import ro.cst.tsearch.utils.StringUtils;

public class MOJacksonCC extends HttpSite {
	
	@Override
	public LoginResponse onLogin() {		
		// String home = 
		execute (new HTTPRequest(dataSite.getServerHomeLink()));
		String page = execute (new HTTPRequest(dataSite.getLink()));
		
		if(page.contains("action=results.asp")) {
			return new LoginResponse(LoginStatus.STATUS_SUCCESS, "Logged in");
		}
		
		return new LoginResponse(LoginStatus.STATUS_INVALID_PAGE, "Invalid response");
	}

	@Override
	public void onBeforeRequestExcl(HTTPRequest req) {		
		String url = req.getURL();
		int idx = url.indexOf("?");		
		if(idx == -1){
			return;
		}
		String link = url.substring(0, idx);
		String params = url.substring(idx + 1);		
		String newUrl = link + "?";
		boolean first = true;
		for(String param: params.split("&")){			
			int idxe = param.indexOf("=");
			if(idxe == -1){
				continue;
			}
			String name = param.substring(0, idxe);
			String value = "";
			if(!param.endsWith("=")){
				value = param.substring(idxe + 1);
			}
			if(!first){
				newUrl += "&";				
			}
			first = false;
			if(!value.contains("%")){
				value = StringUtils.urlEncode(value);
			}
			newUrl += name + "=" + value;
		}
		req.modifyURL(newUrl);
	}
}
