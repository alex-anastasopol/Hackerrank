package ro.cst.tsearch.connection.http2;

import ro.cst.tsearch.connection.http.HTTPRequest;

public class NVWashoeAO extends HttpSite {

	@Override
	public void onBeforeRequest(HTTPRequest req) {
		if (req.getURL().contains("/mysearch.php?")) {
			req.setURL(req.getURL().replace("/mysearch.php?", "/search.php~"));
		}
		super.onBeforeRequest(req);
	}
}
