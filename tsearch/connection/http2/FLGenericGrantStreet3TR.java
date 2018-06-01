/**
 * 
 */
package ro.cst.tsearch.connection.http2;

import static ro.cst.tsearch.connection.http.HTTPRequest.GET;

import org.mortbay.http.HttpResponse;

import ro.cst.tsearch.connection.LoginResponse;
import ro.cst.tsearch.connection.LoginResponse.LoginStatus;
import ro.cst.tsearch.connection.http.HTTPRequest;
import ro.cst.tsearch.connection.http.HTTPResponse;

/**
 * @author vladb
 *
 * @new modifications by Olivia
 *
 * ADD here the new county implemented with this Generic
 *
 * used for  Alachua, Broward, Charlotte, Citrus, Indian River, Lake, Miami-Dade, Monroe, 
 * Okaloosa, Osceola, Pinellas, St Lucie, Sumter, Volusia -like sites        
 */

public class FLGenericGrantStreet3TR extends HttpSite {

	@Override
	public LoginResponse onLogin() {

		// go to taxes search 
		HTTPRequest req = new HTTPRequest(getSiteLink(), GET);
		
		execute(req);
		
		return new LoginResponse(LoginStatus.STATUS_SUCCESS, "Logged in");
	}
	
	public String getPage(String link) {
		HTTPRequest req = new HTTPRequest(link);
		
		return execute(req);
	}
	
	@Override
	public void onAfterRequest(HTTPRequest req, HTTPResponse res) {
		if (res != null && res.getReturnCode() == HttpResponse.__403_Forbidden){
			res.returnCode = 200;
		}
		super.onAfterRequest(req, res);
	}	
}
