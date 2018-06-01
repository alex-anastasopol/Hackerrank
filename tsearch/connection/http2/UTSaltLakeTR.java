package ro.cst.tsearch.connection.http2;

import static ro.cst.tsearch.connection.http.HTTPRequest.GET;
import ro.cst.tsearch.connection.LoginResponse;
import ro.cst.tsearch.connection.LoginResponse.LoginStatus;
import ro.cst.tsearch.connection.http.HTTPRequest;

public class UTSaltLakeTR extends HttpSite {

	private static final String[] PARCEL_PARAMS = { "parcelNumber1", "parcelNumber2", "parcelNumber3", "parcelNumber4",
			"parcelNumber5", "psNumber" };
	private static final String NAME_SEARCH_PAGE= "delqTax/cfml/delinqall.cfm";
	
	@Override
	public LoginResponse onLogin() {

		// go to name search page
		String link = getSiteLink();
		link = link + NAME_SEARCH_PAGE;
		
		HTTPRequest req1 = new HTTPRequest(link, GET);
		execute(req1);
			
		return new LoginResponse(LoginStatus.STATUS_SUCCESS, "Logged in");
	}
	
	@Override
	public void onBeforeRequest(HTTPRequest req) {

		String parcelNumber = req.getPostFirstParameter("ParcelNumber");
		int parcelNumberLenght = 0;
						
		if (parcelNumber!=null){
			parcelNumber = parcelNumber.trim();
			parcelNumber = parcelNumber.replaceAll("-","");		//remove dashes
			parcelNumberLenght = parcelNumber.length();
			if (parcelNumberLenght < 17)						//try to correct if there are not enough digits
				{
					String missingZeroes = "";
					for (int i = 0; i < 17 - parcelNumberLenght; i++) missingZeroes = missingZeroes + "0";
					parcelNumber = parcelNumber + missingZeroes;
				}
			String[] parcel_params = new String[6];
			parcel_params[0] = parcelNumber.substring(0, 2);
			parcel_params[1] = parcelNumber.substring(2, 4);
			parcel_params[2] = parcelNumber.substring(4, 7);
			parcel_params[3] = parcelNumber.substring(7, 10);
			parcel_params[4] = parcelNumber.substring(10, 14);
			parcel_params[5] = parcelNumber.substring(14);
			req.removePostParameters("ParcelNumber");
			for (int i = 0; i < 6; i++)
				req.setPostParameter(PARCEL_PARAMS[i], parcel_params[i]);
		}
	}
		
}
