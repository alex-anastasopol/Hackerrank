package ro.cst.tsearch.connection.http2;

import ro.cst.tsearch.connection.http.HTTPRequest;

public class COPitkinTR extends COGenericTylerTechTR {

	public COPitkinTR() {
		singleLineCookies = false;
	} 
	
	@Override
	public void onBeforeRequestExcl(HTTPRequest req) {
		req.setHeader("User-Agent", getUserAgentValue());
		super.onBeforeRequestExcl(req);
	}
	
	@Override
	public void onBeforeRequest(HTTPRequest req) {
		req.setHeader("User-Agent", getUserAgentValue());
		super.onBeforeRequestExcl(req);
	}

	
	public String getUserAgentValue() {
		return "Mozilla/5.0 (Windows NT 5.1; rv:2.0) Gecko/20100101 Firefox/4.0";
	}

}
