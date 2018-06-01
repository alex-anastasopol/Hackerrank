package ro.cst.tsearch.connection.http3;

import ro.cst.tsearch.connection.http.HTTPRequest;

public class CAMontereyTR extends CAMegabyteCommonSiteTR {
	
	@Override
	public void onBeforeRequestExcl(HTTPRequest req) {
		
		countyName = "monterey";
		super.onBeforeRequestExcl(req);
	}
}
