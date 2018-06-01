package ro.cst.tsearch.connection.http2;

import org.apache.commons.io.IOUtils;

import ro.cst.tsearch.connection.LoginResponse;
import ro.cst.tsearch.connection.LoginResponse.LoginStatus;
import ro.cst.tsearch.connection.http.HTTPRequest;
import ro.cst.tsearch.connection.http.HTTPResponse;

public class OHFranklinAO extends HttpSite{
	@Override
	public LoginResponse onLogin() {
		
		String link = getCrtServerLink() + ".com/altIndex.jsp";
		HTTPRequest req = new HTTPRequest(link);
		
		HTTPResponse res = process(req);
		res.getResponseAsString();
		
		req.setHeader("Host", "franklincountyoh.metacama.com");
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
		
			// fix the url
			String url = req.getURL();
			url = url.replaceAll("\\s", "+");
			
			if (url.contains("map_select_subdivisions") || url.contains("map_select_condominiums")){
				url = url.replaceAll("(?is)franklincountyoh", "fcgis1");
			} else if (url.contains("/rpt/subd") || url.contains("/rpt/condo")){
				url = url.replaceAll("franklincountyoh.metacama.com", "209.51.193.89");
			}
			req.setURL(url);
			
			// get the link
			HTTPResponse httpResponse = process(req);				
			String htmlResponse = httpResponse.getResponseAsString();
			
			// bypass response
			httpResponse.is = IOUtils.toInputStream(htmlResponse);
			req.setBypassResponse(httpResponse);

	}
}
