package ro.cst.tsearch.connection.http;

import java.util.Arrays;
import java.util.Vector;

import org.mortbay.http.HttpResponse;

import ro.cst.tsearch.MailConfig;
import ro.cst.tsearch.Search;
import ro.cst.tsearch.bean.SearchAttributes;
import ro.cst.tsearch.connection.LoginResponse;
import ro.cst.tsearch.connection.LoginResponse.LoginStatus;
import ro.cst.tsearch.data.SitesPasswords;
import ro.cst.tsearch.utils.InstanceManager;

public class ILCookTU extends HTTPSite {

	public String getUserAgentValue() {
		return "Mozilla/5.0 (Windows NT 6.1; WOW64; rv:18.0) Gecko/20100101 Firefox/18.0";
	}
	
	@Override
	public LoginResponse onLogin() {

		try{
			setUserData(InstanceManager.getManager().getCurrentInstance(searchId).getCrtSearchContext().getSearchID() + getClass().getSimpleName());
		}catch(Exception e){}
		
		// get user, password
		String user =  SitesPasswords.getInstance().getPasswordValue(getCurrentCommunityId(), "ILCookTU", "user");
		String password =  SitesPasswords.getInstance().getPasswordValue(getCurrentCommunityId(), "ILCookTU", "password");

		// get email address
		String email = "";
		try{
			Search search = InstanceManager.getManager().getCurrentInstance(searchId).getCrtSearchContext();
			// get abstractor first email address
			email = search.getSa().getAtribute(SearchAttributes.ABSTRACTOR_EMAIL);
			email = email.replaceAll("\\s","");
			email = email.replaceAll(",{2,}",",");
			email = email.replaceAll("^,","");
			if(email.contains(",")){ 
				email = email.substring(0, email.indexOf(",")); 
			}			
		}catch(RuntimeException e){
			email = MailConfig.getMailLoggerStatusAddress();
		}

		// get homepage
		HTTPRequest request = new HTTPRequest("http://www.taxesunlimitedonline.com/");
		String page = process(request).getResponseAsString();
		
		
		// login
		request = new HTTPRequest("http://www.taxesunlimitedonline.com/login.asp");
		request.setMethod(HTTPRequest.POST);
		request.setPostParameter("clientid", user);
		request.setPostParameter("passwd", password);
		request.setPostParameter("email", email);
		request.setPostParameter("valid", "true");
		request.setPostParameter("submit", "Submit");
		page = process(request).getResponseAsString();
		
		if(page.contains("Login Successful, please choose a service")) {
			return new LoginResponse(LoginStatus.STATUS_SUCCESS, "Logged in");
		}
		
		return new LoginResponse(LoginStatus.STATUS_UNKNOWN, "Login failed");
	}
	
	
	@Override
	public void onBeforeRequest(HTTPRequest req) {
		
		try{
			setUserData(InstanceManager.getManager().getCurrentInstance(searchId).getCrtSearchContext().getSearchID() + getClass().getSimpleName());
		}catch(Exception e){}
		
		String url = req.getURL();
		url = url.replaceFirst("&dummy=[^&]+", "");
		req.modifyURL(url);
		
		req.setHeader("User-Agent", getUserAgentValue());
		
		if(url.contains("result.asp")){
			String [] paramPos = new String[] {
			    "ordno",
			    "tx1",
			    "tx2",
			    "tx3",
			    "tx4",
			    "tx5",
			    "tx6",
			    "tx7",
			    "tx8",
			    "tx9",
			    "tx10",
			    "NumOfPins",
			    "Submit"			    
			};
			
			req.setPostParameterOrder(new Vector<String>(Arrays.asList(paramPos)));
		} 
	}
	
	
	public void onAfterRequest(HTTPRequest req, HTTPResponse res) {
		if (res.getReturnCode() == HttpResponse.__500_Internal_Server_Error){
			res.returnCode = 200;
		}
		super.onAfterRequest(req, res);
	}

}