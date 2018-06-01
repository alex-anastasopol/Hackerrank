/**
 * 
 */
package ro.cst.tsearch.connection.http2;

import static ro.cst.tsearch.utils.StringUtils.isEmpty;

import java.util.Map;
import java.util.concurrent.TimeUnit;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;

import ro.cst.tsearch.connection.LoginResponse;
import ro.cst.tsearch.connection.LoginResponse.LoginStatus;
import ro.cst.tsearch.connection.http.HTTPRequest;
import ro.cst.tsearch.connection.http.HTTPResponse;

/**
 * @author radu bacrau
  */
public class FLCollierTR extends HttpSite {
	
	@Override
	public LoginResponse onLogin() {
		String link = getCrtServerLink() + ".com/search/index_search.php";
		String resp = process(new HTTPRequest(link)).getResponseAsString();
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

	public void onRedirect(HTTPRequest req) {
		String redirectUrl = req.getRedirectLocation();
		if (redirectUrl.contains("view.php")) {
			if (!redirectUrl.contains(req.getHeader("Host")) && redirectUrl.startsWith("view")) {
				redirectUrl = req.getURL().substring(0, req.getURL().lastIndexOf("/")+1) + redirectUrl;
			}
			req.setHeader("Referer", redirectUrl);
			req.setMethod(HTTPRequest.GET);
	    	req.modifyURL(redirectUrl);
			process(req);
		}        
	}

	@Override
	public void onBeforeRequestExcl(HTTPRequest req) {
		if (!req.getURL().startsWith("http")) {
			req.modifyURL(getCrtServerLink() + ".com/search/" + req.getURL());
		}
		req.setHeader("Host", getCrtServerLink() + ".com");

		// fix the url
		String url = req.getURL();
		url = url.replaceFirst("&dummy=[^&]*", "");
		req.modifyURL(url);
			
		if (req.getMethod() == HTTPRequest.GET) {
			Map<String,String> params = ro.cst.tsearch.utils.StringUtils.extractParametersFromUrl(req.getURL());
			if (url.contains("repeat.php") && ((url.contains("type=address")) || url.contains("type=name"))) {
				if (params.size() > 0) {
					String strName = "";
					String ownerName = "";
					String taxYear = "";
					if ("address".equals(params.get("type")) || "name".equals(params.get("type"))) {
						strName = params.get("item");
						ownerName = params.get("item");
						taxYear = params.get("year");
						
						if ((StringUtils.isNotBlank(strName) || StringUtils.isNotBlank(ownerName)) && StringUtils.isNotBlank(taxYear)) {
							HTTPRequest newReq = req;
							newReq.setMethod(HTTPRequest.POST);
							if ("address".equals(params.get("type"))) {
								newReq.setPostParameter("ADDRESS_1", strName);
							} else if ("name".equals(params.get("type"))) {
								newReq.setPostParameter("NAME",ownerName);
							}
							
							newReq.setPostParameter("tax_year", taxYear);
							url = url.replaceFirst("(?is)(https?://[^/]+).*", "$1") + "/search/search.php";
								
							newReq.modifyURL(url);
							HTTPResponse httpResponse = process(newReq);				
							String htmlResponse = httpResponse.getResponseAsString();
								
							// bypass response
							httpResponse.is = IOUtils.toInputStream(htmlResponse);
							req.setBypassResponse(httpResponse);
						}
					}
				} 
			}
		}
			
		// get the link
		HTTPResponse httpResponse = process(req);				
		String htmlResponse = httpResponse.getResponseAsString();
			
		// check if it redirects somewhere
		String newLink = req.getRedirectLocation();//extractParameter(htmlResponse, "location\\.replace\\('([^']+)'");
		if(!isEmpty(newLink) && !req.getURL().contains(newLink)) {
			try {
				TimeUnit.MILLISECONDS.sleep(150);
			} catch(InterruptedException e){}
				
			// get the redirected page
			if (!newLink.contains(getCrtServerLink())) {
				if (!newLink.startsWith("/")) {
					newLink = "/" + newLink;
				}
				newLink = getCrtServerLink() + ".com/search/index_search.php" + newLink;
			}
			
			HTTPRequest newReq = new HTTPRequest(newLink);
			httpResponse = process(newReq);				
			htmlResponse = httpResponse.getResponseAsString();
		}
			
		// bypass response
		httpResponse.is = IOUtils.toInputStream(htmlResponse);
		req.setBypassResponse(httpResponse);
	}
	
}
