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
public class ILWillTR extends HttpSite {

//	public String cookie="";
		
	@Override
	public LoginResponse onLogin() {

		String link = getCrtServerLink() + ".com";
		String resp = "";
		HTTPRequest req = new HTTPRequest(link);
		HTTPResponse res = process(req);
//		this.cookie = res.getHeader("Set-Cookie");
		//resp = execute(req);
		resp = res.getResponseAsString();
		
		/*Pattern frameLink = Pattern.compile("(?i)<frame.*?src\\s*=\\s*\\\"(menu[^\\\"]+)\\\"");
		Matcher frameMatcher = frameLink.matcher(resp);
		if (frameMatcher.find()){
			String url = getCrtServerLink() + ".com/" + frameMatcher.group(1);
			req = new HTTPRequest(url);
	    	res = process(req);
	    	resp = res.getResponseAsString();
	    	
	    	Pattern linkPat = Pattern.compile("(?i)<a.*?href\\s*=\\s*\\\"(view\\d+taxes[^\\\"]+)\\\"");
	    	Matcher linkMatcher = linkPat.matcher(resp);
	    	if (linkMatcher.find()){
	    		url2008 = getCrtServerLink() + ".com/" + linkMatcher.group(1);
	    		req = new HTTPRequest(url2008);
	    		res = process(req);
	    		resp = res.getResponseAsString();
	    		
	    		/*linkMatcher.reset();
	    		linkPat = Pattern.compile("(?i)<a.*?href\\s*=\\s*\\\"(http://tax\\.willcountydata.com[^\\\"]+)\\\"");
	    		linkMatcher = linkPat.matcher(resp);
	    		if (linkMatcher.find()) {
	    			url = linkMatcher.group(1);
	    			req = new HTTPRequest(url);
	    			res = process(req);
	    			resp = res.getResponseAsString();
	    		}*/
	    	//}
		//}
		
		
		if(resp == null) {
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
		//String resp = "";
	
//		String url = req.getURL();
//		if (url.indexOf("/ccwtr52") != -1){
//			req.setHeader("Referer", "http://willtax.willcountydata.com/ccwtr51.asp");
//		} else if (url.indexOf("/ccalm08") != -1){
//			req.setHeader("Referer", "http://willtax.willcountydata.com/maintax/ccalm07");
//		}
//		HTTPResponse res = process(req);
//		res.setHeader("Content-Type", "text/html");
		//resp = res.getResponseAsString();
		
		return;		
		
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