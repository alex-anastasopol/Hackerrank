package ro.cst.tsearch.connection.http;

import ro.cst.tsearch.connection.LoginResponse;
import ro.cst.tsearch.connection.LoginResponse.LoginStatus;
import ro.cst.tsearch.utils.InstanceManager;

public class MIOaklandCO extends HTTPSite {

	boolean onLogin = false;

	public LoginResponse onLogin() {

		setUserData(InstanceManager.getManager().getCurrentInstance(searchId).getCrtSearchContext().getSearchID() + "MIOaklandCO");

		//go to main page
		HTTPRequest req = new HTTPRequest("http://www.oakgov.com/clerkrod/courtexplorer/");
		HTTPResponse res = process(req);
		@SuppressWarnings("unused")
		String siteResponse = res.getResponseAsString();

		//go to search page
		req = new HTTPRequest("http://www.oakgov.com/crts0004/main");
		res = process(req);
		siteResponse = res.getResponseAsString();

		return new LoginResponse(LoginStatus.STATUS_SUCCESS, "Logged in");
	}

	public void onBeforeRequest(HTTPRequest req) {
		setUserData(InstanceManager.getManager().getCurrentInstance(searchId).getCrtSearchContext().getSearchID() + "MIOaklandCO");

		String partyType = req.getPostFirstParameter("pType");
		if (partyType != null) {
			req.removePostParameters("ptyType");
			req.setPostParameter("ptyType", partyType);
		}

		String caseY = req.getPostFirstParameter("caseY");
		String caseN = req.getPostFirstParameter("caseN");
		String caseT = req.getPostFirstParameter("caseT");

		if (caseY != null && caseN != null && caseT != null) {
			req.removePostParameters("caseNumber");
			req.setPostParameter("caseNumber", caseY + caseN + caseT);
		}
	}
}
