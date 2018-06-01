package ro.cst.tsearch.connection.http2;

import ro.cst.tsearch.connection.http.HTTPRequest;

public class CORouttTR extends COGenericTylerTechTR {

	@Override
	public String getLoginLink() {
		return "/loginPOST.jsp?submit=Login&guest=true";
	}
	
	@Override
	public String getPage(String page){
		String link = getCrtServerLink().replaceFirst("(?is)/treasurer/web/", "") + page;
		HTTPRequest req = new HTTPRequest(link);
		return execute(req);
	}
	
	@Override
	public String getAssessorPage(String accountId) {
		String link = "http://agner.co.routt.co.us/assessor/taxweb/account.jsp?doc=&accountNum=" + accountId;
		HTTPRequest req = new HTTPRequest(link);
		return execute(req);
	}

}
