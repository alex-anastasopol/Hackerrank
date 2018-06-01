package ro.cst.tsearch.connection.http2;

import ro.cst.tsearch.connection.http.HTTPRequest;

/**
*
* implements the following counties from NV: Clark (including City of North Las Vegas, City of Las Vegas and City of Henderson), 
* Douglas and Washoe (including City of Reno)
*
*/

public class NVGenericCornerAM extends HttpSite {

	public String getPage(String link) {
		HTTPRequest req = new HTTPRequest(link);
		return execute(req);
	}
	
}
