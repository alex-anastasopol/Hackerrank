package ro.cst.tsearch.connection.http2;

import java.io.ByteArrayInputStream;

import org.htmlparser.Node;
import org.htmlparser.filters.HasAttributeFilter;
import org.htmlparser.filters.TagNameFilter;
import org.htmlparser.util.NodeList;

import ro.cst.tsearch.connection.LoginResponse;
import ro.cst.tsearch.connection.LoginResponse.LoginStatus;
import ro.cst.tsearch.connection.http.HTTPRequest;
import ro.cst.tsearch.connection.http.HTTPResponse;
import ro.cst.tsearch.data.SitesPasswords;
import ro.cst.tsearch.parser.HtmlParser3;
import ro.cst.tsearch.utils.StringUtils;

public class KYJeffersonAO extends HttpSite{

	
	public LoginResponse onLogin() {
		
		HTTPRequest req = new HTTPRequest( "http://jeffersonpva.ky.gov/login/" );
        req.setMethod( HTTPRequest.GET );
        //req.noRedirects = true;
        
        HTTPResponse res = process( req );
                
        req = new HTTPRequest( "http://jeffersonpva.ky.gov/login/" );
        req.setMethod( HTTPRequest.POST );
        req.noRedirects = false;
        
        req.setHeader("Referer","http://jeffersonpva.ky.gov/login/");
        req.setPostParameter( "submit_login_form","Log In");
        
        String user = SitesPasswords.getInstance().getPasswordValue(getCurrentCommunityId(), "KYJeffersonAO", "user");
        String pass = SitesPasswords.getInstance().getPasswordValue(getCurrentCommunityId(), "KYJeffersonAO", "password");
        if(StringUtils.isNotEmpty(user) && StringUtils.isNotEmpty(pass)) {
        	req.setPostParameter( "vsm_username" , user);
        	req.setPostParameter( "vsm_password", pass);
        } else {
        	return new LoginResponse(LoginStatus.STATUS_INVALID_CREDENTIALS, "Username or password is missing");
        }
        
        
        res = process( req );
        String r = res.getResponseAsString();
        if(r.contains("This account has reached it's session limit")) {
        	return new LoginResponse(LoginStatus.STATUS_INVALID_PAGE, "This account has reached it's session limit");
        }
        if(res .getReturnCode() == 200 
        		
        		&& !r.matches("(?s).*<span class='error'>Your login credentials are not valid.  Please double-check your username and password.</span>.*")){
        	return new LoginResponse(LoginStatus.STATUS_SUCCESS, "Logged in");
        }
		
        return new LoginResponse(LoginStatus.STATUS_UNKNOWN, "Login failed");
	}
	
	 public void onBeforeRequest(HTTPRequest req)
	 {
		 
	 }

	 @Override
	public void onAfterRequest(HTTPRequest req, HTTPResponse res) {
		if(res.getReturnCode() == 200 && res.getContentType().contains("text/html")) {
			String responseAsString = res.getResponseAsString();
			try {
				HtmlParser3 parser = new HtmlParser3(responseAsString);
				Node node = parser.getNodeById("container");
				NodeList scriptNodeList = node.getChildren().extractAllNodesThatMatch(new TagNameFilter("script"), true)
					.extractAllNodesThatMatch(new HasAttributeFilter("type", "text/javascript"), true);
				if(scriptNodeList.size() == 1) {
					String newUrl = scriptNodeList.elementAt(0).toPlainTextString();
					newUrl = newUrl.replaceAll("window.location.href=\"([^\"]+)\";", "$1");
					if(newUrl.contains("property-details")) {
						String host = req.getURL().replaceFirst("http://", "");
						newUrl = "http://" + host.substring(0, host.indexOf("/")) + newUrl + "/";
						HTTPRequest request = new HTTPRequest(newUrl);
						HTTPResponse response = process(request);
						if (response != null && response.getReturnCode() == 200){
							res.is = response.is;
							res.returnCode = response.returnCode;
							res.body = response.body;
							res.contentLenght = response.contentLenght;
							res.contentType = response.contentType;
							res.headers = response.headers;
						} else {
							res.is = new ByteArrayInputStream(responseAsString.getBytes());
						}
					} else {
						res.is = new ByteArrayInputStream(responseAsString.getBytes());
					}
					
				} else {
					res.is = new ByteArrayInputStream(responseAsString.getBytes());
				}
			} catch (Exception e) {
				logger.error("Error while analysing onAfterRequest", e);
				res.is = new ByteArrayInputStream(responseAsString.getBytes());
			}
			
		}
		
	}
	
	 
}
