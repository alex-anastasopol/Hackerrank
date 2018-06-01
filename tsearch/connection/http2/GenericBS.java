package ro.cst.tsearch.connection.http2;

import static ro.cst.tsearch.connection.http.HTTPRequest.GET;

import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import ro.cst.tsearch.connection.LoginResponse;
import ro.cst.tsearch.connection.LoginResponse.LoginStatus;
import ro.cst.tsearch.connection.http.HTTPRequest;

public class GenericBS extends HttpSite {

	public String viewState = "";
	String eventValidation = "";
	
	@Override
	public LoginResponse onLogin() {

		String link = getSiteLink();
		
		HTTPRequest req = new HTTPRequest(link, GET);
		String responseAsString  = execute(req);
		
		if(responseAsString != null)
		{
			Matcher matcher1 = Pattern.compile("(?is)id=\"__VIEWSTATE\" value=\"(.*?)\"").matcher(responseAsString);
			if (matcher1.find()) {
				viewState = matcher1.group(1);
			}
			
			Matcher matcher2 = Pattern.compile("(?is)id=\"__EVENTVALIDATION\" value=\"(.*?)\"").matcher(responseAsString);
			if (matcher2.find()) {
				eventValidation = matcher2.group(1);
			}
		}	
			
		return new LoginResponse(LoginStatus.STATUS_SUCCESS, "Logged in");
	}
	
	@SuppressWarnings("unchecked")
	@Override
	public void onBeforeRequest(HTTPRequest req) {

		if (req.getMethod() == HTTPRequest.POST)							//Name Search, ID Search
		{
			req.setPostParameter("__VIEWSTATE", viewState);
			req.setPostParameter("__EVENTVALIDATION", eventValidation);
			String url = req.getURL();
			if (url.indexOf("SearchResultForm.aspx")!=-1) { 				//previous/next page
				url = url.replaceAll("SearchForm.aspx/", "");
				req.setURL(url);
				Map<String, String>  addParams = new HashMap<String, String>();
				String keyParameter = req.getPostFirstParameter("seq");
				if (keyParameter!=null)
				{
					addParams = (Map<String, String>) getTransientSearchAttribute("params:" + keyParameter);
					req.removePostParameters("seq");
				}
				String newViewState = addParams.get("__VIEWSTATE");
				String newEventValidation = addParams.get("__EVENTVALIDATION");
				req.removePostParameters("__VIEWSTATE");
				req.removePostParameters("__EVENTVALIDATION");
				req.setPostParameter("__VIEWSTATE", newViewState);
				req.setPostParameter("__EVENTVALIDATION", newEventValidation);
			}	
		} else if (req.getMethod() == HTTPRequest.GET) {					
			String url = req.getURL();
			
			if (url.toLowerCase().contains("institutionhistory.aspx")) {		//institution history
				url = getSiteLink().replaceFirst("(?is)SearchForm.aspx", "") + url;
				url = url.replaceFirst("(?is)&amp;", "&");
			} else {															//details page
				url = url.replaceFirst("(?is)SearchForm.aspx/", "");
			}
			
			req.setURL(url);
		}		
	}

}
