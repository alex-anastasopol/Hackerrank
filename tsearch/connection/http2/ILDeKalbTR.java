package ro.cst.tsearch.connection.http2;

import ro.cst.tsearch.connection.http.HTTPRequest;

public class ILDeKalbTR extends HttpSite {

	@Override
	public void onBeforeRequest(HTTPRequest req) {
		
		String url = req.getURL();
		if (url.contains("QTASResults.asp")) {		//details page
			req.setURL("http://gisweb.co.de-kalb.il.us/qtas/QTASResults.asp");
			req.setPostParameter("btnSubmit", "CLICK HERE TO VIEW PARCEL INFORMATION");
		}
	}
	
	public String getPage(String link) {
		HTTPRequest req = new HTTPRequest(link);
		return execute(req);
	}
}
