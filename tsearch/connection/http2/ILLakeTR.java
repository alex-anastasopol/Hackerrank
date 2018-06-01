/**
 * 
 */
package ro.cst.tsearch.connection.http2;

import java.io.ByteArrayInputStream;

import org.apache.commons.io.IOUtils;

import ro.cst.tsearch.connection.LoginResponse;
import ro.cst.tsearch.connection.LoginResponse.LoginStatus;
import ro.cst.tsearch.connection.http.HTTPRequest;
import ro.cst.tsearch.connection.http.HTTPResponse;
import ro.cst.tsearch.utils.RegExUtils;
import ro.cst.tsearch.utils.StringUtils;

/**
 * @author mihaib
  */
public class ILLakeTR extends HttpSite {
	private String correctLink = null;
		@Override
	public LoginResponse onLogin() {

		String link = dataSite.getLink();

		HTTPRequest req = new HTTPRequest(link);
		HTTPResponse res = process(req);
		String resp = res.getResponseAsString();
		link = RegExUtils.getFirstMatch("(?is)<iframe .* src=.*?\\\"(.*)\\\"\\s*ddf_src", resp, 1);
		// |http://apps01.lakecountyil.gov| /sptreasurer/collbook/collbook2.asp
		correctLink = link.substring(0, link.indexOf("/", link.indexOf("//") + 2));
		req = new HTTPRequest(link);
		res = process(req);
		resp = res.getResponseAsString();
		return new LoginResponse(LoginStatus.STATUS_SUCCESS, "Logged in");
	}
	
	@Override
	public void onBeforeRequestExcl(HTTPRequest req) {
		
		if (correctLink != null) {

			String initialURL = req.getURL();
			String initialURLBase = initialURL.substring(0, initialURL.indexOf("/", initialURL.indexOf("//") + 2));
			String correctURL = initialURL.replace(initialURLBase, correctLink);
			req.modifyURL(correctURL);

			// clear spaces from search fields
			if (req.getMethod() == HTTPRequest.POST)
			{
				if (req.getPostFirstParameter("Pin") != null)
				{
					String pin = req.getPostFirstParameter("Pin").toString().trim();
					req.setPostParameter("Pin", pin, true);
				}

				if (req.getPostFirstParameter("StreetName") != null)
				{
					String streetName = req.getPostFirstParameter("StreetName").toString().trim();
					req.setPostParameter("StreetName", streetName, true);
				}
				if (req.getPostFirstParameter("StreetNumber") != null)
				{
					String streetNumber = req.getPostFirstParameter("StreetNumber").toString().trim();
					req.setPostParameter("StreetNumber", streetNumber, true);

				}
				if (req.getPostFirstParameter("StreetNumber") != null)
				{
					String zip = req.getPostFirstParameter("Zip").toString().trim();
					req.setPostParameter("Zip", zip, true);
				}
			}
		}
			
		try{
			
			// fix the url
			String url = req.getURL();
			url = url.replaceAll("\\s", "+");
			
			// get the link
			HTTPResponse httpResponse = process(req);				
			String htmlResponse = httpResponse.getResponseAsString();
			
			// bypass response
			httpResponse.is = IOUtils.toInputStream(htmlResponse);
			req.setBypassResponse(httpResponse);

		} finally {
			
			// mark that we are out of treating onBeforeRequest 
			setAttribute("onBeforeRequest", Boolean.FALSE);
			
		}
	}
	
	@Override
	public void onAfterRequest(HTTPRequest req, HTTPResponse res) {
		String content = "";
		
	
		if (res.getContentType().contains("text/html")){
			content = res.getResponseAsString();
		}
		//ex ca 03043000430000 crapa siteul oficial
		if (content.matches("(?is).*<!--Problem-->\\s*The\\s*page\\s*cannot\\s*be\\s*displayed.*")) {
		//if (res.getReturnCode() == 500){
			content = "<font color=\"red\">There was a problem on the official site.</font>";
		}
		res.is = new ByteArrayInputStream(content.getBytes());
	}
}
