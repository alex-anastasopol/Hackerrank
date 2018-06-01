package ro.cst.tsearch.connection.http2;

import java.util.HashMap;
import java.util.Map;

import ro.cst.tsearch.connection.LoginResponse;
import ro.cst.tsearch.connection.LoginResponse.LoginStatus;
import ro.cst.tsearch.connection.http.HTTPRequest;
import ro.cst.tsearch.parser.SimpleHtmlParser;
import ro.cst.tsearch.utils.StringUtils;

public class TNShelbyTR extends HttpSite {
	
	private static final String[] ADD_PARAM_NAMES = { "__LASTFOCUS", "__VIEWSTATE", "__EVENTTARGET", "__EVENTARGUMENT", "__EVENTVALIDATION" };
	private static final String[] REQ_PARAM_NAMES = { "__VIEWSTATE", "__EVENTVALIDATION" };
	
	private static final String FORM_NAME = "ctl00";
	
	public LoginResponse onLogin( ) 
	{
		//http://www.shelbycountytrustee.com/TaxQry/TaxQry.aspx?Flag=0
		
		HTTPRequest req = new HTTPRequest( "https://sct.shelbycountytrustee.com/TaxQry/OwnerNameSearch.aspx?flag=0" );
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
		setAttribute("name.params", addParams);
		
		req = new HTTPRequest( "https://sct.shelbycountytrustee.com/TaxQry/PropLocSearch.aspx?flag=0" );
        req.setMethod( HTTPRequest.GET );
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
		setAttribute("address.params", addParams);
		
		req = new HTTPRequest( "https://sct.shelbycountytrustee.com/TaxQry/ParcelIDSearch.aspx?flag=0" );
        req.setMethod( HTTPRequest.GET );
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
		setAttribute("pid.params", addParams);
		
		return new LoginResponse(LoginStatus.STATUS_SUCCESS, "Logged in");
	}
	
	@SuppressWarnings("unchecked")
	@Override
	public void onBeforeRequestExcl(HTTPRequest req) {
		
		
		
		req.setHeader("Host", "sct.shelbycountytrustee.com");
		Map<String,String> addParams = null;
		if (!StringUtils.isEmpty((String)req.getPostFirstParameter("LastName")) ||
				!StringUtils.isEmpty((String)req.getPostFirstParameter("BusinessName")))
		{
			addParams = (HashMap<String,String>)getAttribute("name.params");
			req.setHeader("Referer", "https://sct.shelbycountytrustee.com/TaxQry/TaxQry.aspx?Flag=0");
		} 
		else if (!StringUtils.isEmpty((String)req.getPostFirstParameter("TxtStreetName"))) 
		{
			addParams = (HashMap<String,String>)getAttribute("address.params");
	        req.setHeader("Referer", "https://sct.shelbycountytrustee.com/TaxQry/PropLocSearch.aspx?flag=0");
		} 
		else if (!StringUtils.isEmpty((String)req.getPostFirstParameter("Parcel"))) 
		{
			addParams = (HashMap<String,String>)getAttribute("pid.params");
	        req.setHeader("Referer", "https://sct.shelbycountytrustee.com/TaxQry/ParcelIDSearch.aspx?flag=0");
	        req.removePostParameters("ion");
			req.removePostParameters("x");
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
		
		if (addParams != null) {
			for (String key : ADD_PARAM_NAMES) {
				String value = "";
				if (addParams.containsKey(key)) {
					value = addParams.get(key);
				}
				req.setPostParameter(key, value);
			}
		}
		
		// trim search fields
		if (req.getMethod() == HTTPRequest.POST)
		{// search by name
			if (req.getPostFirstParameter("FirstName") != null)
				if (req.getPostFirstParameter("FirstName").isEmpty() == false)
				{
					String FirstName = req.getPostFirstParameter("FirstName").toString().trim();
					req.setPostParameter("FirstName", FirstName, true);
				}
			if (req.getPostFirstParameter("LastName") != null)
				if (req.getPostFirstParameter("LastName").isEmpty() == false)
				{
					String LastName = req.getPostFirstParameter("LastName").toString().trim();
					req.setPostParameter("LastName", LastName, true);
				}
			// search by business name
			if (req.getPostFirstParameter("BusinessName") != null)
				if (req.getPostFirstParameter("BusinessName").isEmpty() == false)
				{
					String BusinessName = req.getPostFirstParameter("BusinessName").toString().trim();
					BusinessName = BusinessName.replaceAll("\\s+", " ");
					req.setPostParameter("BusinessName", BusinessName, true);
				}
			// search by property location
			if (req.getPostFirstParameter("TxtStreetNumber") != null)
				if (req.getPostFirstParameter("TxtStreetNumber").isEmpty() == false)
				{
					String TxtStreetNumber = req.getPostFirstParameter("TxtStreetNumber").toString().trim();
					req.setPostParameter("TxtStreetNumber", TxtStreetNumber, true);
				}
			if (req.getPostFirstParameter("TxtStreetName") != null)
				if (req.getPostFirstParameter("TxtStreetName").isEmpty() == false)
				{
					String TxtStreetName = req.getPostFirstParameter("TxtStreetName").toString().trim();
					req.setPostParameter("TxtStreetName", TxtStreetName, true);
				}
			// search by parcel ID
			if (req.getPostFirstParameter("Ward") != null)
				if (req.getPostFirstParameter("Ward").isEmpty() == false)
				{
					String Ward = req.getPostFirstParameter("Ward").toString().trim();
					req.setPostParameter("Ward", Ward, true);
				}

			if (req.getPostFirstParameter("Block") != null)
				if (req.getPostFirstParameter("Block").isEmpty() == false)
				{
					String Block = req.getPostFirstParameter("Block").toString().trim();
					req.setPostParameter("Block", Block, true);
				}
			if (req.getPostFirstParameter("SubNumber") != null)
				if (req.getPostFirstParameter("SubNumber").isEmpty() == false)
				{
					String SubNumber = req.getPostFirstParameter("SubNumber").toString().trim();
					req.setPostParameter("SubNumber", SubNumber, true);
				}
			if (req.getPostFirstParameter("Parcel") != null)
				if (req.getPostFirstParameter("Parcel").isEmpty() == false)
				{
					String Parcel = req.getPostFirstParameter("Parcel").toString().trim();
					req.setPostParameter("Parcel", Parcel, true);
				}
			if (req.getPostFirstParameter("Tag") != null)
				if (req.getPostFirstParameter("Tag").isEmpty() == false)
				{
					String Tag = req.getPostFirstParameter("Tag").toString().trim();
					req.setPostParameter("Tag", Tag, true);
				}
		}
	}

}
