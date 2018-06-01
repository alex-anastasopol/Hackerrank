package ro.cst.tsearch.connection.http2;

import ro.cst.tsearch.connection.LoginResponse;
import ro.cst.tsearch.connection.LoginResponse.LoginStatus;
import ro.cst.tsearch.connection.http.HTTPRequest;
import ro.cst.tsearch.connection.http.HTTPResponse;


/**
 * The connection file that will be set in SearchSites
 * All servers will contain one single connection file that will keep track of everything
 * @author AndreiA
 *
 */
public class GenericSKLD extends HttpSite {
	private static GenericSKLDSite internalSite;
	
	@Override
	public void performSpecificActionsAfterCreation() {
		if(internalSite == null) {
			internalSite = new GenericSKLDSite();
			internalSite.setSiteManager(getSiteManager());
			internalSite.setHttpClient(getHttpClient());
		}
	}
	
	@Override
	public LoginResponse onLogin() {
		internalSite.setSearchId(getSearchId());
		return internalSite.onLogin(getSearchId(), getDataSite());
	}
	@Override
	public void onBeforeRequest(HTTPRequest req) {
		internalSite.setSearchId(getSearchId());
		internalSite.onBeforeRequest(req);
		
	}
		
	@Override
	public HTTPResponse process(HTTPRequest request) {
		request.setSearchId(getSearchId());
		//request.setSiteName(getSiteName());
		request.setDataSite(getDataSite());
		return internalSite.addAndExecuteRequest(request);
	}

}
