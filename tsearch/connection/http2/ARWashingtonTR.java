package ro.cst.tsearch.connection.http2;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import ro.cst.tsearch.connection.LoginResponse;
import ro.cst.tsearch.connection.LoginResponse.LoginStatus;
import ro.cst.tsearch.connection.http.HTTPRequest;
import ro.cst.tsearch.servers.bean.DataSite;

public class ARWashingtonTR extends HttpSite {
	
	private String address = ""; 
	
	@Override
	public LoginResponse onLogin() {
		
		DataSite dataSite = getDataSite(); 
		HTTPRequest request = new HTTPRequest(dataSite.getLink());
		String responseAsString = execute(request);
		
		if(responseAsString != null) {
			
			Matcher ma = Pattern.compile("(?is)action = \\(\"SearchResults.asp([^\\\"]+)\\\"\\)").matcher(responseAsString);
			if (ma.find()) {
				address = ma.group(1);
				return new LoginResponse(LoginStatus.STATUS_SUCCESS, "Logged in");
			}
		}
		
		return new LoginResponse(LoginStatus.STATUS_INVALID_PAGE, "Invalid response");
	}
	
	@Override
	public void onBeforeRequest(HTTPRequest req) {

		String url = req.getURL();
		if (req.getMethod()==HTTPRequest.POST && url.toLowerCase().contains("searchresults.asp") && address.length()!=0) {		//intermediary page
			url += address;
			req.setURL(url);
		}
	}
}
