
package ro.cst.tsearch.connection.http2;

import static ro.cst.tsearch.connection.http.HTTPRequest.GET;

import java.util.HashMap;
import java.util.Map;
import ro.cst.tsearch.connection.LoginResponse;
import ro.cst.tsearch.connection.LoginResponse.LoginStatus;
import ro.cst.tsearch.connection.http.HTTPRequest;
import ro.cst.tsearch.parser.SimpleHtmlParser;

/**
 * @author vladb
 *
 */
public class NVWashoeTR extends HttpSite {
	
	private static final String FORM_NAME = "Form";

	@Override
	public LoginResponse onLogin() {

		HTTPRequest req = new HTTPRequest(getSiteLink(), GET);
		String page = execute(req);
		
		Map<String, String> params = new SimpleHtmlParser(page).getForm(FORM_NAME).getParams();
		Map<String, String> _params = new HashMap<String, String>();
		
		for(Map.Entry<String, String> entry : params.entrySet()) {
			if(entry.getKey().startsWith("__")) {
				_params.put(entry.getKey(), entry.getValue());
			}
		}
		
		setAttribute("params", _params);
		
		return new LoginResponse(LoginStatus.STATUS_SUCCESS, "Logged in");
	}
	
	@SuppressWarnings("unchecked")
	@Override
	public void onBeforeRequestExcl(HTTPRequest req) {
		
		Map<String, String> params = (Map<String, String>) getAttribute("params");
		
		for(Map.Entry<String, String> entry : params.entrySet()) {
			req.setPostParameter(entry.getKey(), entry.getValue());
		}
	}
}
