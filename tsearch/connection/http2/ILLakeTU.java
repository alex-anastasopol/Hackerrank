package ro.cst.tsearch.connection.http2;

import java.util.HashMap;
import java.util.Map;

import ro.cst.tsearch.connection.LoginResponse;
import ro.cst.tsearch.connection.LoginResponse.LoginStatus;
import ro.cst.tsearch.connection.http.HTTPRequest;
import ro.cst.tsearch.connection.http.HTTPResponse;
import ro.cst.tsearch.parser.SimpleHtmlParser;
import ro.cst.tsearch.utils.StringUtils;

public class ILLakeTU extends HttpSite {
	
	private static final String ADDRESS = "http://apps01.lakecountyil.gov/spcountyclerk/tax/EstimateOfRedemption/PinEntry.aspx";
	
	private static final String[] REQ_PARAM_NAMES = { "__VIEWSTATE", "__EVENTVALIDATION" };
	
	private Map<String,String> addParams = new HashMap<String,String>();
	
	@Override
	public LoginResponse onLogin() {
		
		HTTPRequest req = new HTTPRequest(ADDRESS);
		
		HTTPResponse resp = process(req);
		String responseAsString = resp.getResponseAsString();
		
		addParams = isolateParams(responseAsString, "form1");
		if (!checkParams(addParams)){
			return new LoginResponse(LoginStatus.STATUS_INVALID_PAGE, "Invalid parameters");
		}
		
		return new LoginResponse(LoginStatus.STATUS_SUCCESS, "Logged in");
	}
	
	public static Map<String, String> isolateParams(String page, String form){
		Map<String, String> params = new SimpleHtmlParser(page).getForm(form).getParams();
		Map<String,String> addParams = new HashMap<String,String>();
		for (String key : REQ_PARAM_NAMES) {
			String value = "";
			if (params.containsKey(key)) {
				value = params.get(key);
			}
			addParams.put(key, value);
		}
		
		return addParams;
	}
	
	public static boolean checkParams(Map<String, String> addParams){
		for(String key: REQ_PARAM_NAMES){
			if(StringUtils.isEmpty(addParams.get(key))){
				return false;
			}
		}
		return true;
	}

	@Override
	public void onBeforeRequest(HTTPRequest req) {
		
		String pin = req.getPostFirstParameter("txtPin");
		if (!StringUtils.isEmpty(pin)) {
			for (String key: REQ_PARAM_NAMES) {
				req.setPostParameter(key, addParams.get(key));
			}
		}
		
	}
	
}
