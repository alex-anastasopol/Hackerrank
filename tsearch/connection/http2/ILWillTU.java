/**
 * 
 */
package ro.cst.tsearch.connection.http2;

import java.io.ByteArrayInputStream;

import ro.cst.tsearch.connection.LoginResponse;
import ro.cst.tsearch.connection.LoginResponse.LoginStatus;
import ro.cst.tsearch.connection.http.HTTPRequest;
import ro.cst.tsearch.connection.http.HTTPResponse;

/**
 * @author mihaib
  */
public class ILWillTU extends HttpSite {

	public String cookie="";
		
	@Override
	public LoginResponse onLogin() {

		String link = getCrtServerLink() + ".com/ccwtx20.asp";
		String resp = "";
		HTTPRequest req = new HTTPRequest(link);
		HTTPResponse res = process(req);
		this.cookie = res.getHeader("Set-Cookie");
		resp = execute(req);		
		
		if(resp == null || !resp.contains("click the Submit button")) {
			return new LoginResponse(LoginStatus.STATUS_UNKNOWN, "Login failed");
		}
		
		return new LoginResponse(LoginStatus.STATUS_SUCCESS, "Logged in");
	}
	
	/**
	 * Get the current server link
	 * @return
	 */
	private String getCrtServerLink(){
		String link = getSiteLink();
		int idx = link.indexOf(".com");
		if(idx == -1){
			throw new RuntimeException("County " + getDataSite().getName() + " not supported by this class!");
		}
		return link.substring(0, idx);
	}

	

	@Override
	public void onBeforeRequestExcl(HTTPRequest req) {	
		return;		
		
	}
	
	public String getPage(String url) {
		HTTPRequest req = new HTTPRequest(url, HTTPRequest.GET);
		
		return execute(req);
	}
	@Override	
	public void onAfterRequest(HTTPRequest req, HTTPResponse res) {
		String content = "";
		if (res != null) {
			if (!res.getContentType().contains("text/html")){
				res.setHeader("Content-Type", "text/html");
				content = res.getResponseAsString();
				res.is = new ByteArrayInputStream(content.getBytes());
			}
		}
	}
	
}