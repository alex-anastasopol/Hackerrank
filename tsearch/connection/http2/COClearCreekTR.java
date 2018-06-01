package ro.cst.tsearch.connection.http2;

import ro.cst.tsearch.connection.http.HTTPRequest;


public class COClearCreekTR extends COGenericTylerTechTR {
	
	@Override
	public void onBeforeRequest(HTTPRequest req){
		String url = req.getURL();		

		if (url.indexOf("https") > -1) {
			return;
		} else {
			url = url.replaceFirst("(?is)http", "https");
			// modify link
			req.modifyURL(url);
		}
	}
	
	@Override
	public void onRedirect(HTTPRequest req) {
		String redirectUrl = req.getRedirectLocation();
		if(redirectUrl.contains("treasurerweb/../") /* || redirectUrl.contains("treasurer/web/../") */) {
			redirectUrl = redirectUrl.replaceFirst("(?is)treasurer/?web/../", "");
			req.setRedirectLocation(redirectUrl);
		} 
	}
}
