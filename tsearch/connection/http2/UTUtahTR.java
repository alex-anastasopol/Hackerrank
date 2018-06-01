package ro.cst.tsearch.connection.http2;

import ro.cst.tsearch.connection.http.HTTPRequest;

public class UTUtahTR extends HttpSite {

	@Override
	public void onBeforeRequest(HTTPRequest req) {
		String url = req.getURL();
		//redirect to another host
		if (!url.contains("TaxPayoff")) url = url.replace("www.co.utah.ut.us", "www.utahcountyonline.org");
		url = url + "&";
		//replace % with %25
		url = url.replaceAll("%&", "%25&");
		url = url.substring(0, url.length()-1);
		req.setURL(url);
	}
	
	public String getPage(String link) {
		HTTPRequest req = new HTTPRequest(link);
		return execute(req);
	}
	
}
