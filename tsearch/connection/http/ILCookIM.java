package ro.cst.tsearch.connection.http;

import ro.cst.tsearch.data.SitesPasswords;
import ro.cst.tsearch.utils.InstanceManager;
import ro.cst.tsearch.utils.StringUtils;

public class ILCookIM extends HTTPSite {
	
	private boolean loggingIn = false;
	private boolean loggedIn  = false;
	
	
	/**
     * login
     */
	public boolean login(String email) {
		
		loggingIn = true;
		
		HTTPRequest req = new HTTPRequest("http://illinois.reidata.com/nn-new-ValidForm.asp");
		req.setMethod(HTTPRequest.POST);		
		req.setPostParameter("R1" , "V1");
		req.setPostParameter("Account" ,SitesPasswords.getInstance().getPasswordValue(getCurrentCommunityId(), getClass().getSimpleName(), "user"));
		req.setPostParameter("Password" , SitesPasswords.getInstance().getPasswordValue(getCurrentCommunityId(), getClass().getSimpleName(), "password"));
		req.setPostParameter("EMail", email);
		HTTPResponse res = process(req);
		String response = res.getResponseAsString();
			
		loggedIn  = true;
		loggingIn = false;

		// Sorry, Invalid Account Or Password Entered
		// Sorry, Invalid E-Mail Address Entered 
		return !response.contains("Sorry, Invalid");

	}

	
	/**
	 * before each request
	 */
	public void onBeforeRequest(HTTPRequest req) {
		
		// check if we are already logging in
		if(loggingIn){ return; }
		
		// check the request has Email field
		if(
				 req.getPostFirstParameter("B1") != null ||           // place order
				(req.getPostFirstParameter("b1") != null /*&&!loggedIn*/) // track order && !logged in
		){
			String email = req.getPostParameter("Email").toString();
			login(email);
			req.removePostParameters("Email");
		}
	}
	
}
