package ro.cst.tsearch.connection.http3;

import java.util.Calendar;
import java.util.HashMap;
import java.util.TimeZone;

import org.apache.commons.io.IOUtils;

import ro.cst.tsearch.connection.ATSConnConstants;
import ro.cst.tsearch.connection.LoginResponse;
import ro.cst.tsearch.connection.LoginResponse.LoginStatus;
import ro.cst.tsearch.connection.http.HTTPRequest;
import ro.cst.tsearch.connection.http.HTTPResponse;

public class GenericConnADI extends HttpSite3 {
	
	private static GenericSiteADI internalSite;
	
	@Override
	public void performSpecificActionsAfterCreation() {
		if(internalSite == null) {
			internalSite = new GenericSiteADI();
			internalSite.setSiteManager(getSiteManager());
			internalSite.setHttpClient(getHttpClient());
		}
	}
	
	@Override
	public LoginResponse onLogin() {
		
		Calendar now = Calendar.getInstance(TimeZone.getTimeZone("CST"));
		
		Calendar low = Calendar.getInstance(TimeZone.getTimeZone("CST"));
		low.set(Calendar.HOUR_OF_DAY, 4);
		low.set(Calendar.MINUTE, 0);
		low.set(Calendar.SECOND, 0);
		Calendar high = Calendar.getInstance(TimeZone.getTimeZone("CST"));
		high.set(Calendar.HOUR_OF_DAY, 23);
		high.set(Calendar.MINUTE, 59);
		high.set(Calendar.SECOND, 59);
		
		if(now.before(low) || now.after(high)) {
			return new LoginResponse(LoginStatus.STATUS_OUT_OF_BUSINESS_HOURS, 
					"Plant is offline between between 12:00AM – 4:00AM (CST)");
		}
		
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
		
		Calendar now = Calendar.getInstance(TimeZone.getTimeZone("CST"));
		
		Calendar low = Calendar.getInstance(TimeZone.getTimeZone("CST"));
		low.set(Calendar.HOUR_OF_DAY, 4);
		low.set(Calendar.MINUTE, 0);
		low.set(Calendar.SECOND, 0);
		Calendar high = Calendar.getInstance(TimeZone.getTimeZone("CST"));
		high.set(Calendar.HOUR_OF_DAY, 23);
		high.set(Calendar.MINUTE, 59);
		high.set(Calendar.SECOND, 59); 
		
		if(now.before(low) || now.after(high)) {
			
			HTTPResponse response = new HTTPResponse();
			response.is = IOUtils.toInputStream("The Title Plant System is offline between between 12:00AM – 4:00AM (CST)");
			response.returnCode = ATSConnConstants.HTTP_OK;
			response.contentType = "text/html";
			response.headers = new HashMap<String, String>();
			if(request != null) {
				request.setBypassResponse(response);
			}
			return response;
			
		} else {
			request.setSearchId(getSearchId());
			//request.setSiteName(getSiteName());
			request.setDataSite(getDataSite());
			return internalSite.addAndExecuteRequest(request);
		}
	}
}
