package ro.cst.tsearch.connection.http2;

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import ro.cst.tsearch.connection.LoginResponse;
import ro.cst.tsearch.connection.LoginResponse.LoginStatus;
import ro.cst.tsearch.connection.http.HTTPRequest;
import ro.cst.tsearch.parser.SimpleHtmlParser;
import ro.cst.tsearch.utils.StringUtils;

public class TNShelbyEP extends HttpSite {
	
	public static final String[] ADD_PARAM_NAMES = { "__LASTFOCUS", "__VIEWSTATE", "__EVENTTARGET", "__EVENTARGUMENT", "__EVENTVALIDATION" };
	public static final String[] REQ_PARAM_NAMES = { "__VIEWSTATE", "__EVENTVALIDATION" };
	
	public static final String FORM_NAME = "aspnetForm";
	public static final String PARAMETERS_NAVIGATION_LINK = "params.link";
	
	@Override
	public LoginResponse onLogin() {
		String loginLink = getLoginLink();
		
		HTTPRequest req = new HTTPRequest( loginLink );
        req.setMethod( HTTPRequest.GET );
        String response = process( req ).getResponseAsString();
        //isolate parameters
		Map<String, String> params = new SimpleHtmlParser(response).getForm(FORM_NAME).getParams();
		Map<String,String> addParams = new HashMap<String,String>();
		for (String key : ADD_PARAM_NAMES) {
			String value = "";
			if (params.containsKey(key)) {
				value = params.get(key);
			}
			addParams.put(key, value);
		}
        
		// check parameters
		for(String key: REQ_PARAM_NAMES){
			if(StringUtils.isEmpty(addParams.get(key))){
				return new LoginResponse(LoginStatus.STATUS_INVALID_PAGE, "Invalid parameters");
			}
		}
		
		// store parameters
		setAttribute("tax.bill.number.params", addParams);
		
		//------------------------------------------------------
		req = new HTTPRequest( loginLink );
        req.setMethod( HTTPRequest.POST );
        for (Entry<String, String> entry : addParams.entrySet()) {
			req.setPostParameter(entry.getKey(), entry.getValue());
		}
        req.setPostParameter("__EVENTTARGET", "ctl00$MainBodyPlaceHolder$radParcelNo");
        req.setPostParameter("ctl00$ScriptManager1", "ctl00$MainBodyPlaceHolder$UpdatePanel1|ctl00$MainBodyPlaceHolder$radParcelNo");
        req.setPostParameter("ctl00$MainBodyPlaceHolder$SearchFields", "radParcelNo");
        
        response = process( req ).getResponseAsString();
        //isolate parameters
		params = new SimpleHtmlParser(response).getForm(FORM_NAME).getParams();
		addParams = new HashMap<String,String>();
		for (String key : ADD_PARAM_NAMES) {
			String value = "";
			if (params.containsKey(key)) {
				value = params.get(key);
			}
			addParams.put(key, value);
		}
        
		// check parameters
		for(String key: REQ_PARAM_NAMES){
			if(StringUtils.isEmpty(addParams.get(key))){
				return new LoginResponse(LoginStatus.STATUS_INVALID_PAGE, "Invalid parameters");
			}
		}
		
		// store parameters
		setAttribute("parcel.number.params", addParams);
		//----------------------------------------------------
		req = new HTTPRequest( loginLink );
        req.setMethod( HTTPRequest.POST );
        for (Entry<String, String> entry : addParams.entrySet()) {
			req.setPostParameter(entry.getKey(), entry.getValue());
		}
        req.setPostParameter("__EVENTTARGET", "ctl00$MainBodyPlaceHolder$radOwnerNameAddress");
        req.setPostParameter("ctl00$ScriptManager1", "ctl00$MainBodyPlaceHolder$UpdatePanel1|ctl00$MainBodyPlaceHolder$radOwnerNameAddress");
        req.setPostParameter("ctl00$MainBodyPlaceHolder$SearchFields", "radOwnerNameAddress");
        
        response = process( req ).getResponseAsString();
        //isolate parameters
		params = new SimpleHtmlParser(response).getForm(FORM_NAME).getParams();
		addParams = new HashMap<String,String>();
		for (String key : ADD_PARAM_NAMES) {
			String value = "";
			if (params.containsKey(key)) {
				value = params.get(key);
			}
			addParams.put(key, value);
		}
        
		// check parameters
		for(String key: REQ_PARAM_NAMES){
			if(StringUtils.isEmpty(addParams.get(key))){
				return new LoginResponse(LoginStatus.STATUS_INVALID_PAGE, "Invalid parameters");
			}
		}
		
		// store parameters
		setAttribute("property.params", addParams);
		
		return new LoginResponse(LoginStatus.STATUS_SUCCESS, "Logged in");
		
		
	}
	
	@SuppressWarnings("unchecked")
	@Override
	public void onBeforeRequestExcl(HTTPRequest req) {
		req.setHeader("Host", "epayments.memphistn.gov");
		Map<String,String> addParams = null;
		if (!StringUtils.isEmpty((String)req.getPostFirstParameter("ctl00$MainBodyPlaceHolder$txtPropOwnerLastName")) ||
				!StringUtils.isEmpty((String)req.getPostFirstParameter("ctl00$MainBodyPlaceHolder$txtPropAddress")))
		{
			addParams = (HashMap<String,String>)getAttribute("property.params");
			
		} 
		else if (!StringUtils.isEmpty((String)req.getPostFirstParameter("ctl00$MainBodyPlaceHolder$txtTaxBillNo"))) 
		{
			addParams = (HashMap<String,String>)getAttribute("tax.bill.number.params");
			req.setHeader("Referer", getRefererLink());
		} 
		
		
		else if (!StringUtils.isEmpty((String)req.getPostFirstParameter("ctl00$MainBodyPlaceHolder$txtParcelNo"))) 
		{
			addParams = (HashMap<String,String>)getAttribute("parcel.number.params");
			req.setHeader("Referer", getRefererLink());
		} 
		else if (!StringUtils.isEmpty((String)req.getPostFirstParameter("__EVENTTARGET"))) 
		{
			addParams = (Map<String, String>) getSearch().getAdditionalInfo(
					ro.cst.tsearch.connection.http2.TNShelbyEP.PARAMETERS_NAVIGATION_LINK);
			req.setHeader("Referer", getRefererLink());
			addParams.remove("__EVENTTARGET");
		}
		else if(req.getMethod() == HTTPRequest.POST){
			// remove the seq parameter
			String seq = req.getPostFirstParameter("seq");
			req.removePostParameters("seq");
			
			// additional parameters are stored inside the search since they are too long
			addParams = (Map<String, String>)getTransientSearchAttribute("params:" + seq);			
			String url = req.getURL();
			url = StringUtils.urlDecode(req.getURL());
			url = url.replaceAll("&amp;", "&");
			req.modifyURL(url);
			for(Map.Entry<String, String> entry: addParams.entrySet()){
				req.setPostParameter(entry.getKey(), entry.getValue());
			}
			return;
			
		}
		
		if(addParams != null) {			
			for (String key : ADD_PARAM_NAMES) {
				String value = "";
				if (addParams.containsKey(key)) {
					value = addParams.get(key);
				}
				req.setPostParameter(key, value);
			}			
		}
	}
	
	private String getLoginLink(){
		return getSiteLink();
	}
	
	private String getRefererLink(){
		return getLoginLink() + "Default.aspx";
	}

}
