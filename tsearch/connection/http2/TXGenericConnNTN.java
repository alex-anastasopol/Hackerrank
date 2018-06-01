package ro.cst.tsearch.connection.http2;


import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.httpclient.HostConfiguration;
import org.apache.commons.io.IOUtils;

import ro.cst.tsearch.connection.LoginResponse;
import ro.cst.tsearch.connection.LoginResponse.LoginStatus;
import ro.cst.tsearch.connection.http.HTTPRequest;
import ro.cst.tsearch.connection.http.HTTPResponse;
import ro.cst.tsearch.data.SitesPasswords;
import ro.cst.tsearch.datatrace.Utils;
import ro.cst.tsearch.parser.SimpleHtmlParser;
import ro.cst.tsearch.parser.SimpleHtmlParser.Form;
import ro.cst.tsearch.utils.Tidy;



/**
 * @author mihaib
*/

public class TXGenericConnNTN extends HttpSite{
	
	private static String FORM_NAME = "WebLogin";
	
	private static final Pattern LOCATION_PAT = Pattern.compile("(?is)window.location.replace\\(\\s*['|\"]([^'|^\"]*)");
	
	@Override
	public LoginResponse onLogin() {
		
		setAttribute(SINGLE_LINE_COOKIES_IN_REDIRECT, true);
		
		HTTPRequest req = new HTTPRequest(getSiteLink(), HTTPRequest.GET);
		HTTPResponse res = process(req);
		String response = res.getResponseAsString();
		
		response = Tidy.tidyParse(response, null);
		
		Form form = new SimpleHtmlParser(response).getForm(FORM_NAME);
		if (form == null){
			return new LoginResponse(LoginStatus.STATUS_INVALID_PAGE, "Login form not found");
		} else {
			
			String action = form.action;

			//List<Input> inputs = form.inputs;
			HTTPRequest reqLogin = new HTTPRequest(action, HTTPRequest.POST);
			String username = SitesPasswords.getInstance().getPasswordValue(getCurrentCommunityId(searchId), "TXGenericServerNTN", "user");
			String password = SitesPasswords.getInstance().getPasswordValue(getCurrentCommunityId(searchId), "TXGenericServerNTN", "password");
			
			reqLogin.setPostParameter("BrowserStatus", "BAD");
			reqLogin.setPostParameter("USERNAME", username);
			reqLogin.setPostParameter("PASSWORD", password);
			
			res = process(reqLogin);
			response = res.getResponseAsString();
			
			String newUrl = "";
			
			Matcher mat  = LOCATION_PAT.matcher(response);
			if (mat.find()){
				newUrl = mat.group(1);
			} else {
				return new LoginResponse(LoginStatus.STATUS_INVALID_PAGE, "Redirect location link not found");
			}
			
			if (newUrl.indexOf("relogin") > 0){
				return new LoginResponse(LoginStatus.STATUS_INVALID_CREDENTIALS, "Invalid credentials");
			} else if (newUrl.indexOf("launch-point") > 0){
				req = new HTTPRequest(getHost(action) + newUrl, HTTPRequest.GET);
				
				res = process(req);
				response = res.getResponseAsString();
				
			}
			// indicate success
			return new LoginResponse(LoginStatus.STATUS_SUCCESS, "Logged in");
		}
		
	}
	
	private String getHost(String url){
		
		int index = url.indexOf("https://");
		if(index >= 0) {
			url = url.substring(index  + "https://".length());
		}
		
		index = url.indexOf("/");
		if(index >= 0) {
			url = url.substring(0, index);
		}
		
		if (!url.contains("https://")){
			return "https://" + url;
		}
		
		return url;
	}
	
	@Override
	public void onBeforeRequestExcl(HTTPRequest req) {
    	
		HTTPResponse httpResp = null;
		String response = "";
		
		httpResp = process(req);
		response = httpResp.getResponseAsString();
		
		Matcher mat = LOCATION_PAT.matcher(response);
		if (mat.find()){
			HTTPRequest newRequest = new HTTPRequest(getHost(req.getURL()) + mat.group(1), HTTPRequest.GET);
			httpResp = process(newRequest);
			response = httpResp.getResponseAsString();
			
			Pattern pat = Pattern.compile("(?is)window.open\\(\\s*['|\\\"]([^'|^\\\"]*)['|\\\"]\\s*,\\s*['|\\\"]PDFCertificates");
			mat.reset();
			mat = pat.matcher(response);
			if (mat.find()){
				req.setURL(mat.group(1));
			} else {
				httpResp.contentType = "text/html; charset=utf-8";
				// bypass response
				httpResp.is = IOUtils.toInputStream(response);
				req.setBypassResponse(httpResp);
			}
		}
	}
	
//	public HTTPResponse getPage(String src) {
//		
//		String link = "https://www.lpstax.com" + src.substring(src.indexOf("=/") + 1, src.length());
//		HTTPRequest request = new HTTPRequest(link);
//		HTTPResponse response = process(request);
//		
//		return response;
//	}	
	
	@Override
	public void onAfterRequest(HTTPRequest req, HTTPResponse res) {
		if(res != null) {
			if(res.getReturnCode() == 502 && res.getLastURI().toString().endsWith("tc-property-browse.w")) {
				destroySession();
			}
		}
	}
	
	@Override
	protected void addSpecificSiteProxy() {
		if (Utils.isJvmArgumentTrue("debugForATSProgrammer")) {
    		HostConfiguration config = getHttpClient().getHostConfiguration();
	        config.setProxy("192.168.92.55", 8080);
        }
	}
}