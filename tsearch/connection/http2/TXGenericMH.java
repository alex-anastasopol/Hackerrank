package ro.cst.tsearch.connection.http2;

import ro.cst.tsearch.connection.http.HTTPRequest;

public class TXGenericMH extends HttpSite {

	public String getPage(String link) {
		link = link.replaceAll("&amp;", "&");
		HTTPRequest req = new HTTPRequest(link);
		return execute(req);
	}
	
}