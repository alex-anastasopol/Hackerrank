package ro.cst.tsearch.connection.http3;

import ro.cst.tsearch.connection.LoginResponse;
import ro.cst.tsearch.connection.http.HTTPRequest;
import ro.cst.tsearch.connection.http.HTTPResponse;
import ro.cst.tsearch.utils.StringUtils;

/**
 * There is a single connection for all servers 
 */
public class TXGenericTaxNetAO extends TXGenericTaxNetAOSite {
	
	private static TXGenericTaxNetAOSite internalSite;
	private static String staticCookies;
	
	@Override
	public void performSpecificActionsAfterCreation() {
		if(internalSite == null) {
			internalSite = new TXGenericTaxNetAOSite();
			internalSite.setSiteManager(getSiteManager());
			internalSite.setHttpClient(getHttpClient());
		}
	}
	
	@Override
	public LoginResponse onLogin() {
		internalSite.setSearchId(getSearchId());
		return internalSite.onLogin(getDataSite());
	}
	
	@Override
	public void onBeforeRequestExcl(HTTPRequest req) {
		internalSite.setSearchId(getSearchId());
		boolean needsCookie = false;
		if (req.getURL().contains("/EntityItem.php")) {
			String value = req.getPostFirstParameter("needsCookie");
			if ("true".equals(value)) {
				needsCookie = true;
				req.removePostParameters("needsCookie");
			}
		} else {
			needsCookie = true;
		}
		if (needsCookie) {
			if (!StringUtils.isEmpty(staticCookies)) {
				String presentCookies = req.getHeader("Cookie");
				if (StringUtils.isEmpty(presentCookies)) {
					req.setHeader("Cookie", staticCookies);
				}
			}
		}
		internalSite.onBeforeRequestExcl(req, getDataSite());
		
	}
	
	@Override
	public void onAfterRequest(HTTPRequest req, HTTPResponse res) {
		internalSite.onAfterRequest(req, res);
		if (staticCookies==null) {
			staticCookies = getCookieString();
		}
	}
	
}
