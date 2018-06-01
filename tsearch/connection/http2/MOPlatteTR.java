package ro.cst.tsearch.connection.http2;

import java.util.Map;

import ro.cst.tsearch.connection.LoginResponse;
import ro.cst.tsearch.connection.LoginResponse.LoginStatus;
import ro.cst.tsearch.connection.http.HTTPRequest;
import ro.cst.tsearch.connection.http.HTTPResponse;
import ro.cst.tsearch.parser.SimpleHtmlParser;
import ro.cst.tsearch.parser.SimpleHtmlParser.Form;

public class MOPlatteTR extends HttpSite {

	private String PHPSESSID;
	public static String PDF_URL = "http://plattecountycollector.com/onlinerec/platter_rec_state.php?fullparcel=$$accountId$$&year=$$year$$";

	@Override
	public LoginResponse onLogin() {
		
		// get the search page
		String siteLink = getSiteLink();
		
		HTTPRequest req = new HTTPRequest(siteLink);
		String response = execute(req);
		
		if (!response.contains("Parcel Number Search")){
			return new LoginResponse(LoginStatus.STATUS_INVALID_PAGE, "Page not found");
		} else{
			Form form = new SimpleHtmlParser(response).getForm("realform");
			if (form != null){
				Map<String, String> params = form.getParams();
				if (params != null && params.get("PHPSESSID") != null){
					PHPSESSID = params.get("PHPSESSID");
				} else{
					return new LoginResponse(LoginStatus.STATUS_INVALID_PAGE, "Page not found");
				}
			} else{
				return new LoginResponse(LoginStatus.STATUS_INVALID_PAGE, "Page not found");
			}
		}
		
		// indicate success
		return new LoginResponse(LoginStatus.STATUS_SUCCESS, "Logged in");
	}
		
	@Override
	public void onBeforeRequestExcl(HTTPRequest req){
		
		String url = req.getURL();	
		url = url.replaceAll("&amp;", "&");
			
		if (url.contains("realresults.php")){
			String link = getSiteLink() + "?PHPSESSID=" + PHPSESSID + "&PHPSESSID=" + PHPSESSID;
			HTTPRequest newReq = new HTTPRequest(link, HTTPRequest.GET);
			String response = execute(newReq);
			Form form = new SimpleHtmlParser(response).getForm("realform");
			if (form != null){
				Map<String, String> params = form.getParams();
				if (params != null && params.get("PHPSESSID") != null){
					PHPSESSID = params.get("PHPSESSID");
				}
			}
		}
		
		if (req.getPostFirstParameter("seq") != null){
			@SuppressWarnings("unchecked")
			Map<String, String> addParams = (Map<String, String>) getTransientSearchAttribute("params:" + req.getPostFirstParameter("seq"));

			req.removePostParameters("seq");
			if (addParams != null){
				for(Map.Entry<String, String> entry: addParams.entrySet()){
					req.setPostParameter(entry.getKey(), entry.getValue());
				}
			}
		}
			
		if (req.getPostFirstParameter("PHPSESSID") == null){
			req.setPostParameter("PHPSESSID", PHPSESSID);
		}
		
	}
	
	public byte[] getPDFDocument(String accountId, String year) {
		String link = PDF_URL.replace("$$accountId$$", accountId).replace("$$year$$", year);
		HTTPRequest req = new HTTPRequest(link);
		HTTPResponse response = process(req);
		return response.getResponseAsByte();
	}
}
