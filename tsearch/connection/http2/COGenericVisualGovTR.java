
package ro.cst.tsearch.connection.http2;

import static ro.cst.tsearch.connection.http.HTTPRequest.GET;
import static ro.cst.tsearch.connection.http.HTTPRequest.POST;
import static ro.cst.tsearch.utils.StringUtils.isEmpty;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import ro.cst.tsearch.connection.LoginResponse;
import ro.cst.tsearch.connection.LoginResponse.LoginStatus;
import ro.cst.tsearch.connection.http.HTTPRequest;
import ro.cst.tsearch.parser.SimpleHtmlParser;
import ro.cst.tsearch.parser.SimpleHtmlParser.Form;
import ro.cst.tsearch.utils.StringUtils;

/**
 * @author vladb
 *
 */
public class COGenericVisualGovTR extends HttpSite {

	private static final String[] ADD_PARAM_NAMES = { "__LASTFOCUS", "__VIEWSTATE", "__EVENTTARGET", "__EVENTARGUMENT", "__EVENTVALIDATION" };
	private static final String[] REQ_PARAM_NAMES = { "__VIEWSTATE", "__EVENTVALIDATION" };
	private static final String FORM_NAME = "aspnetForm";
	
	private HTTPRequest createHTTPRequest(String html, Form form, String action) {
		HTTPRequest req = new HTTPRequest(getSiteLink(), POST);
		req.addPostParameters(form.getParams());
		
		if (!isEmpty(action)) 
		{
			Pattern pattern = Pattern.compile("href=\"javascript:__doPostBack\\('([^']+)','([^']*)'\\)\">" + action);
			Matcher matcher = pattern.matcher(html);

			if (matcher.find())
			{
				req.removePostParameters("__EVENTTARGET");
				req.removePostParameters("__EVENTARGUMENT");
				req.setPostParameter("__EVENTTARGET", matcher.group(1));
				req.setPostParameter("__EVENTARGUMENT", matcher.group(2));
			}
			else
			{
				throw new RuntimeException("Could not find action: " + action);
			}
		}
		return req;

	}
	
	@Override
	public LoginResponse onLogin() {
		
		try {
			// go to taxes search 
			HTTPRequest req2 = new HTTPRequest(getSiteLink(), GET);
			String p2 = exec(req2).getResponseAsString();
			
			Form form2 = new SimpleHtmlParser(p2).getForm(FORM_NAME);
			for (String search : form2.getValues("ctl00$ContentPlaceHolder1$Search")) {
				
				// get the search page
				HTTPRequest req3 = createHTTPRequest(p2, form2, null);
				req3.removePostParameters("ctl00$ContentPlaceHolder1$Search");
				req3.setPostParameter("ctl00$ContentPlaceHolder1$Search", search);
				String p3 = exec(req3).getResponseAsString();
				
				// isolate parameters
				Map<String, String> params = new SimpleHtmlParser(p3).getForm(FORM_NAME).getParams();
				Map<String, String> addParams = new HashMap<String,String>();
				for (String key : ADD_PARAM_NAMES) {
					String value = "";
					if (params.containsKey(key)) {
						value = params.get(key);
					}
					addParams.put(key, value);
				}
				
				// check parameters
				for(String key: REQ_PARAM_NAMES){
					if(!addParams.containsKey(key)){
						String errorMsg = "Did not find required parameter " + key + " for " + search + " search!";
						logger.error(errorMsg);
						return new LoginResponse(LoginStatus.STATUS_INVALID_PAGE, errorMsg);
					}
				}
				
				// store parameters
				setAttribute(search + ".params", addParams);
			}
		} catch (IOException e) {
			e.printStackTrace();
			return new LoginResponse(LoginStatus.STATUS_UNKNOWN, "Exception occured: " + e.toString());
		}
		
		return new LoginResponse(LoginStatus.STATUS_SUCCESS, "Logged in");
	}
	
	@SuppressWarnings("unchecked")
	@Override
	public void onBeforeRequestExcl(HTTPRequest req) {
		
		// add the additional search parameters, obtained during login
		if(req.hasPostParameter("ctl00$ContentPlaceHolder1$btnSearch")){
			String search = "";
			if(req.getURL().contains("NameSearch.aspx")){
				search = "name";
			} else if(req.getURL().contains("AddressSearch.aspx")){
				search = "address";
			} else if(req.getURL().contains("ScheduleSearch.aspx")){
				search = "schedulenumber";
			} else {
				throw new RuntimeException("This URL is not a valid search URL:" + req.getURL());
			}
			Map<String,String> addParams = (Map<String,String>)getAttribute(search + ".params");
			if (addParams != null){
				for(String key:ADD_PARAM_NAMES){ 
					req.setPostParameter(key, addParams.get(key));
				}
			} else {
				throw new RuntimeException("Search parameters are null. The site may be down." + req.getURL());
			}
		} else {
			// remove the seq parameter
			String seq = req.getPostFirstParameter("seq");
			req.removePostParameters("seq");

			// additional parameters are stored inside the search since they are too long
			Map<String, String> params = (Map<String, String>)getTransientSearchAttribute("params:" + seq);			
			if(params != null){
				String url = req.getURL();
				url = StringUtils.urlDecode(req.getURL());
				url = url.replaceAll("&amp;", "&");
				req.modifyURL(url);
				for(Map.Entry<String, String> entry: params.entrySet()){
					req.setPostParameter(entry.getKey(), entry.getValue());
				}
			}
		}
	}

}
