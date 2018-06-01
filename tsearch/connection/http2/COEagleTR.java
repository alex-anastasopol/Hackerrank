package ro.cst.tsearch.connection.http2;

import java.io.ByteArrayInputStream;
import java.util.Map;

import ro.cst.tsearch.connection.LoginResponse;
import ro.cst.tsearch.connection.LoginResponse.LoginStatus;
import ro.cst.tsearch.connection.http.HTTPRequest;
import ro.cst.tsearch.connection.http.HTTPResponse;
import ro.cst.tsearch.parser.SimpleHtmlParser;
import ro.cst.tsearch.servers.bean.DataSite;

public class COEagleTR extends HttpSite {
	
	public static final String FORM_NAME = "docSearch";
	
	@Override
	public LoginResponse onLogin() {
		
		DataSite dataSite = getDataSite(); 
		
		HTTPRequest request = new HTTPRequest(dataSite.getLink());
		
		String responseAsString = execute(request);
		
		if(responseAsString != null) {
			try {
				Map<String,String> addParams = new SimpleHtmlParser(responseAsString).getForm(FORM_NAME).getParams();
				if(addParams.containsKey("AccountNumID")) {
					return new LoginResponse(LoginStatus.STATUS_SUCCESS, "Logged in");
				}
			} catch (Exception e) {
				logger.error("Problem Parsing Form on CODenverAO", e);
			}
		}
		
		return new LoginResponse(LoginStatus.STATUS_INVALID_PAGE, "Invalid response");
	}
	
	@Override
	public void onAfterRequest(HTTPRequest req, HTTPResponse res) {
		String content = "";
		if (res!= null && res.getContentType().contains("text/html")){
			content = res.getResponseAsString();
			res.is = new ByteArrayInputStream(content.getBytes());
			if(content.contains("Your session is no longer active, please login to access this page")) {
				destroySession();
			}
		}
		
	}

}
