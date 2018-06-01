package ro.cst.tsearch.connection.http2;

import ro.cst.tsearch.connection.http.HTTPRequest;

public class COMesaTR extends COGenericTylerTechTR {

	@Override
	public String getLoginLink() {
		return "/Treasurer/web/loginPOST.jsp?submit=Login&guest=true";
	}

	public String getAssessorPage(String accountId) {
		String link = "http://www.imap.mesacounty.us/assessor_lookup/Assessor_Parcel_Report.aspx?Account=" + accountId;
		HTTPRequest req = new HTTPRequest(link);
		return execute(req);
	}

}
