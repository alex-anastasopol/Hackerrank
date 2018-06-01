/**
 * 
 */
package ro.cst.tsearch.connection.http2;

import static ro.cst.tsearch.connection.http.HTTPRequest.GET;

import java.util.Map;
import java.util.Vector;

import ro.cst.tsearch.connection.LoginResponse;
import ro.cst.tsearch.connection.LoginResponse.LoginStatus;
import ro.cst.tsearch.connection.http.HTTPRequest;

/**
 * @author vladb
 *
 */
public class FLWashingtonTR extends HttpSite {

	@Override
	public LoginResponse onLogin() {

		// go to taxes search 
		HTTPRequest req = new HTTPRequest(getSiteLink(), GET);
		execute(req);
		
		return new LoginResponse(LoginStatus.STATUS_SUCCESS, "Logged in");
	}
	
	@SuppressWarnings("unchecked")
	@Override
	public void onBeforeRequestExcl(HTTPRequest req) {
			
		Vector<String> order = new Vector<String>();
		
		if(req.hasPostParameter("BEGIN")) {
			order.add("BEGIN");
			order.add("TYPE");
			order.add("INPUT");
		} else {
			
			Map<String, String> params = (Map<String, String>)getTransientSearchAttribute("params:");			
			if(params != null){
				for(Map.Entry<String, String> entry: params.entrySet()){
					req.setPostParameter(entry.getKey(), entry.getValue());
				}
			}
			
			order.add("Begin");
			order.add("Type");
			order.add("Input");
		}
			
		if(req.getPostFirstParameter("Owner Search") != null) {
			order.add("Owner Search");
		} else if(req.getPostFirstParameter("Parcel Search") != null) {
			order.add("Parcel Search");
		} else if(req.getPostFirstParameter("Address Search") != null) {
			order.add("Address Search");
		}
		req.setPostParameterOrder(order);
		
		return;
	}
}
