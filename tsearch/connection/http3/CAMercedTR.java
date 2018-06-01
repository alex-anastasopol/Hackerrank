package ro.cst.tsearch.connection.http3;

import ro.cst.tsearch.connection.http.HTTPRequest;

public class CAMercedTR extends CAMegabyteCommonSiteTR {
	
	@Override
	public void onBeforeRequestExcl(HTTPRequest req) {
		countyName = "merced";
		super.onBeforeRequestExcl(req);
	}
}
