package ro.cst.tsearch.connection.http3;

import ro.cst.tsearch.connection.http.HTTPRequest;

public class CAStanislausTR extends CAMegabyteCommonSiteTR {
	
	@Override
	public void onBeforeRequestExcl(HTTPRequest req) {
		
		countyName = "stanislaus";
		super.onBeforeRequestExcl(req);
	}
}
