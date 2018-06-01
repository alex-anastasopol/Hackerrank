package ro.cst.tsearch.connection.http3;

import java.io.IOException;
import java.util.Iterator;
import java.util.Map;

import org.apache.commons.lang.StringUtils;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;

import ro.cst.tsearch.connection.LoginResponse;
import ro.cst.tsearch.connection.LoginResponse.LoginStatus;
import ro.cst.tsearch.connection.http.HTTPRequest;
import ro.cst.tsearch.connection.http.HTTPResponse;

public class KSJohnsonAOM extends HttpSite3 {

	/**
	 * @author MihaiB
	 * 
	 */
	
	@Override
	public LoginResponse onLogin() {

		HTTPResponse response;
		HTTPRequest request = new HTTPRequest(getSiteLink(), HTTPRequest.GET);
		try {
			response = exec(request);
		} catch (IOException e) {
			return new LoginResponse(LoginStatus.STATUS_INVALID_PAGE, "Login Page not found");
		}
		if (response.getResponseAsString().contains("Search for")){
			
			return new LoginResponse(LoginStatus.STATUS_SUCCESS, "Logged in");
		}
		
		
		return new LoginResponse(LoginStatus.STATUS_INVALID_PAGE, "Login Page not found");
	}
	
	@Override
	public void onBeforeRequestExcl(HTTPRequest req) {
		
		if (StringUtils.isNotEmpty(req.getPostFirstParameter("prefixText"))){
			JSONObject jsonReq = new JSONObject();
			String searchValue = req.getPostFirstParameter("prefixText");
			try {
				jsonReq.accumulate("prefixText", searchValue);
				req.setEntity(jsonReq.toString());
				//req.setEntity("{ 'prefixText':" + " '" + searchValue + "' }");
				req.setHeader("Content-Type", "application/json; charset=utf-8");
				req.removePostParameters("prefixText");
			} catch (JSONException e) {
				e.printStackTrace();
			}
		}
		super.onBeforeRequestExcl(req);
	}
	
	public String getPage(String link, Map<String, String> params) {
		HTTPRequest req = null;
		if (params == null){
			req = new HTTPRequest(link, HTTPRequest.GET);
		} else {
			req = new HTTPRequest(link, HTTPRequest.POST);
			Iterator<String> i = params.keySet().iterator();
			while(i.hasNext()){
				String k = i.next();
				req.setPostParameter(k, params.get(k));
			}
		}
		
		return execute(req);
	}
}
