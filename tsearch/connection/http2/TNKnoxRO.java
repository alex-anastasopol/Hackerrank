package ro.cst.tsearch.connection.http2;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import ro.cst.tsearch.connection.LoginResponse;
import ro.cst.tsearch.connection.LoginResponse.LoginStatus;
import ro.cst.tsearch.connection.http.HTTPRequest;
import ro.cst.tsearch.connection.http.HTTPResponse;
import ro.cst.tsearch.connection.http.HTTPRequest.ParametersVector;
import ro.cst.tsearch.data.SitesPasswords;
import ro.cst.tsearch.utils.StringUtils;

public class TNKnoxRO extends HttpSite {
	
	@Override
	public LoginResponse onLogin() {
		
		HTTPRequest req;
		HTTPResponse res;
		String pageResponse;
		
		
		String passKey = getDataSite().getName();

		// Get username and password from DB.
		String user = SitesPasswords.getInstance().getPasswordValue(
				getCurrentCommunityId(), passKey, "user");
		String pass = SitesPasswords.getInstance().getPasswordValue(
				getCurrentCommunityId(), passKey, "password");

		if (StringUtils.isEmpty(user) && StringUtils.isEmpty(pass)) {
			user = SitesPasswords.getInstance()
					.getPasswordValue(getCurrentCommunityId(),
							getClass().getSimpleName(), "user");
			pass = SitesPasswords.getInstance().getPasswordValue(
					getCurrentCommunityId(), getClass().getSimpleName(),
					"password");
		}

		if (StringUtils.isEmpty(user) || StringUtils.isEmpty(pass)) {
			return LoginResponse.getDefaultInvalidCredentialsResponse();
		}
		
		String loginLink = getSiteLink();
		
		//go to login page                 
		req = new HTTPRequest(loginLink);
		res = process(req);
		pageResponse = res.getResponseAsString();

		// post login data
		req = new HTTPRequest(loginLink );
		req.setMethod(HTTPRequest.POST);
		req.setHeader("Referer", loginLink);
		req.setPostParameter("DTSUser" , user);
		req.setPostParameter("DTSPassword" , pass);
		req.setPostParameter("DTSNewPassword1", "");
		req.setPostParameter("DTSNewPassword2", "");
		req.setPostParameter("Accepted", "I+Agree+to+the+Above+Conditions");
		
		res = process(req);
		pageResponse = res.getResponseAsString();
		
		if(pageResponse.contains("Password expires in")) {
			return new LoginResponse(LoginStatus.STATUS_SUCCESS, "Logged in");
		}
		return new LoginResponse(LoginStatus.STATUS_UNKNOWN, "Login failed");
		
	}
	
	@Override
    @SuppressWarnings("unchecked")
	public void onBeforeRequestExcl(HTTPRequest req) {
		if(req.getURL().contains("LoadImage.asp")) {
			req.setHeader("Referer", ro.cst.tsearch.servers.types.TNKnoxRO.FILE_REFERER);
		}
		
		// Trim params.
		HashMap<String, ParametersVector> params = req.getPostParameters();
		req.setPostParameters(new HashMap<String, HTTPRequest.ParametersVector>());
		Iterator<?> it = params.entrySet().iterator();
	    while (it.hasNext()) {
			Map.Entry<String, ParametersVector> entry = (Map.Entry<String, ParametersVector>)it.next();
	        String name = entry.getKey();
	        String value = entry.getValue().getFirstParameter();
	        if (value == null)
	        	continue;
	        req.setPostParameter(name, value.trim());
	    }
		
		super.onBeforeRequestExcl(req);
	}
	
}
