
package ro.cst.tsearch.connection.http2;

import java.io.ByteArrayInputStream;

import ro.cst.tsearch.connection.LoginResponse;
import ro.cst.tsearch.connection.LoginResponse.LoginStatus;
import ro.cst.tsearch.connection.http.HTTPRequest;
import ro.cst.tsearch.connection.http.HTTPResponse;
import ro.cst.tsearch.servers.HashCountyToIndex;
import ro.cst.tsearch.servers.types.TSServersFactory;

/**
 * @author mihaib
  */
public class COJeffersonTR extends HttpSite {
	

	public String cookie="";
		@Override
	public LoginResponse onLogin() {

		String link = getCrtServerLink() + ".us/ttpsintWeb/Controller.jpf";
		//String resp = "";
		HTTPRequest req = new HTTPRequest(link);
		HTTPResponse res = process(req);
		this.cookie=res.getHeader("Set-Cookie");
		//resp = execute(req);
		//resp = res.getResponseAsString();
		
		return new LoginResponse(LoginStatus.STATUS_SUCCESS, "Logged in");
	}
	
	/**
	 * Get the current server link
	 * @return
	 */
	private String getCrtServerLink(){
		String link = getSiteLink();
		int idx = link.indexOf(".us");
		if(idx == -1){
			throw new RuntimeException("County " + getDataSite().getName() + " not supported by this class!");
		}
		return link.substring(0, idx);
	}

	@Override
	public void onBeforeRequestExcl(HTTPRequest req) {
	}
	
	@Override
	public void onAfterRequest(HTTPRequest req, HTTPResponse res) {
		String content = "";
		if (res != null){
			if (res.getContentType().contains("text/html")){
				content = res.getResponseAsString();
				res.is = new ByteArrayInputStream(content.getBytes());
				if(content.contains("Your data has timed-out")) {
					destroySession();
				}
			}
		}
		
	}
}