package ro.cst.tsearch.connection.http3;

import ro.cst.tsearch.connection.http.HTTPRequest;

public class CASanBenitoTR extends CAMegabyteCommonSiteTR {
	
	@Override
	public void onBeforeRequestExcl(HTTPRequest req) {
		countyName = "sanbenito";
		super.onBeforeRequestExcl(req);
	}
}
