/**
 * 
 */
package ro.cst.tsearch.connection.http2;

import static ro.cst.tsearch.utils.StringUtils.extractParameter;
import static ro.cst.tsearch.utils.StringUtils.isEmpty;

import java.util.concurrent.TimeUnit;

import org.apache.commons.io.IOUtils;

import ro.cst.tsearch.connection.LoginResponse;
import ro.cst.tsearch.connection.LoginResponse.LoginStatus;
import ro.cst.tsearch.connection.http.HTTPRequest;
import ro.cst.tsearch.connection.http.HTTPResponse;
import ro.cst.tsearch.utils.StringUtils;

/**
 * @author radu bacrau
 * 
 * @category is used by FLBrevard, FLDuval, FLEscambia, 
							FLNassau, FLPolk, FLSantaRosa, FLStJohns, FLSumter, FLWalton, TNDavidsonTR,
							FLFlaglerTR, FLColumbiaTR, FLGilchristTR, FLHendryTR, FLSarasota
	if you add a new county, PLEASE add it here in this list
	if you modify this generic class PLEASE check all the counties from this list.
			Thank you.
  */

public class FLGenericGovernmaxTR extends HttpSite {
	
	@Override
	public LoginResponse onLogin() {
		
		String crtServerLink = getCrtServerLink();
		String link =  crtServerLink + "/collectmax/collect30.asp";
		String resp = "";
	
		resp = process(new HTTPRequest(link)).getResponseAsString();
		String src = StringUtils.extractParameter(resp, "(?is)src\\s*=\\s*\\\"([^\\\"]+)");
		HTTPRequest req = new HTTPRequest(crtServerLink + "/collectmax/" + src, HTTPRequest.GET);
		resp = process(req).getResponseAsString();

		String sid = extractParameter(resp, "[&\\?]sid=([0-9A-Z]+)");
		if(isEmpty(sid)){
			logger.error("Could not login!");
			return new LoginResponse(LoginStatus.STATUS_UNKNOWN, "Login failed");
		}
		setAttribute("sid", sid);
		return new LoginResponse(LoginStatus.STATUS_SUCCESS, "Logged in");
	}
	
	/**
	 * Get the current server link
	 * @return
	 */
	private String getCrtServerLink(){
		String link = getSiteLink();
		int idx = link.indexOf("/collectmax/collect30.asp");
		if(idx == -1){
			throw new RuntimeException("County " + getDataSite().getName() + " not supported by this class!");
		}
		return link.substring(0, idx);
	}

	@Override
	public void onBeforeRequest(HTTPRequest req) {
		
		// don't do anything during logging in
		if(status != STATUS_LOGGED_IN){
			return;
		}

		// don't do anything while we're already inside a onBeforeRequest call
		if(getAttribute("onBeforeRequest") == Boolean.TRUE){
			return;
		}
		
		// mark that we're treating onBeforeRequest
		setAttribute("onBeforeRequest", Boolean.TRUE);

		try{

			// remove additional unneeded param
			req.removePostParameters("go.x");
			
			// set the proper sid
			boolean hasSid = "x".equals(req.getPostFirstParameter("sid"));
			if(hasSid){
				req.removePostParameters("sid");
				req.setPostParameter("sid", (String)getAttribute("sid"));
			}
			
			
			if (req.getPostParameter("owner") != null && req.getPostParameter("owner2") != null) 
	        {           
	        	String text = req.getPostParameter("owner") + " " + req.getPostParameter("owner2");
	        	
	        	req.removePostParameters("owner");

	            req.setPostParameter("owner", text);
	            
	            req.postParameters.remove("owner2");
	        }
	        else if (req.getPostParameter("streetname") != null && req.getPostParameter("streetname1") != null) 
	        {           
	        	String streetName = req.getPostParameter("streetname") + " " + req.getPostParameter("streetname1");
	            
	        	req.removePostParameters("streetname");
	        	req.setPostParameter("streetname", streetName.trim());
	        	
	            req.postParameters.remove("streetname1");
	        }
			
			
			// fix the url
			String url = req.getURL();
			if(!isEmpty(req.getPostFirstParameter("l_nm")) && url.endsWith("/collectmax/search_collect.asp")){
				url += "?go.x=1";
			}
			url = url.replace("|", "%7c");
			url = url.replaceFirst("&dummy=[^&]*", "");
			
			req.modifyURL(url);
			
			// get the link
			HTTPResponse httpResponse = process(req);				
			String htmlResponse = httpResponse.getResponseAsString();
			
			// check if it redirects somewhere
			String newLink = extractParameter(htmlResponse, "location\\.replace\\('([^']+)'");
			
			
			while(!isEmpty(newLink)){
				
				newLink = newLink.replace("|", "%7C");
				
				if(url.contains("tn-davidson-taxcollector") && !newLink.contains("agency/tn-davidson-taxcollector")) {
					newLink = "agency/tn-davidson-taxcollector/" + newLink;
				}
				
				// wait
				try{
					TimeUnit.MILLISECONDS.sleep(150);
				}catch(InterruptedException e){}
				
				// get the redirected page
				newLink = getCrtServerLink() + "/collectmax/" + newLink;
				
				HTTPRequest newReq = new HTTPRequest(newLink);
				httpResponse = process(newReq);				
				htmlResponse = httpResponse.getResponseAsString();
				
				newLink = extractParameter(htmlResponse, "location\\.replace\\('([^']+)'");
			}
			
			// bypass response
			httpResponse.is = IOUtils.toInputStream(htmlResponse);
			req.setBypassResponse(httpResponse);

		} finally {
			
			// mark that we are out of treating onBeforeRequest 
			setAttribute("onBeforeRequest", Boolean.FALSE);
			
		}
	}
	
}
