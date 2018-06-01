/**
 * 
 */
package ro.cst.tsearch.connection.http2;

import static ro.cst.tsearch.utils.StringUtils.extractParameter;
import static ro.cst.tsearch.utils.StringUtils.isEmpty;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;

import ro.cst.tsearch.connection.LoginResponse;
import ro.cst.tsearch.connection.LoginResponse.LoginStatus;
import ro.cst.tsearch.connection.http.HTTPRequest;
import ro.cst.tsearch.connection.http.HTTPResponse;
import ro.cst.tsearch.data.SitesPasswords;

/**
 * @author radu bacrau
 *
 */
public class TNGenericUsTitleSearchRO extends HttpSite {

	protected String server = "";  // www1/www2/www3/www4 ..
	
	@Override
    public LoginResponse onLogin() {
		
		// determine the current active server
    	HTTPRequest req = new HTTPRequest("http://www.ustitlesearch.net/");
    	req.noRedirects = true;
        HTTPResponse res = process(req);
        String page = res.getResponseAsString();        
        server = extractParameter(page,  "http://(www\\d+)\\.ustitlesearch.net");
        if(isEmpty(server)){
        	logger.error("Load balancing redirection expected but not found!");
        	return new LoginResponse(LoginStatus.STATUS_INVALID_PAGE, "Load balancing redirection expected but not found!");
        }
        String link = extractParameter(page, "This object may be found <a HREF=\"(http://www\\d+\\.ustitlesearch\\.net/cookiecheck\\.asp\\?serverid=www\\d+\\.ustitlesearch\\.net)\">here</a>");
        if(isEmpty(link)){
        	logger.error("Load balancing redirection expected but not found!");
        	return new LoginResponse(LoginStatus.STATUS_INVALID_PAGE, "Load balancing redirection expected but not found!");
        }
        page = process(new HTTPRequest(link)).getResponseAsString();        
        if(!page.contains("<p>This page uses frames, but your browser doesn't support them.</p>")){
        	logger.error("Expected the frames page, but did not receive it!");
        	return new LoginResponse(LoginStatus.STATUS_INVALID_PAGE, "Expected the frames page, but did not receive it!");
        }

        // go to login page
        link = "http://" + server + ".ustitlesearch.net/page.asp?page=logon.asp";
        page = process(new HTTPRequest(link)).getResponseAsString();        
        link = "http://" + server +".ustitlesearch.net/logon.asp?page=logon.asp";
        page = process(new HTTPRequest(link)).getResponseAsString();
        
        // try to login
        page = login();
        
        if(page == null) {
        	return LoginResponse.getDefaultInvalidCredentialsResponse();
        }
        
        // check if we have to kick somebody out
        link = extractParameter(page, "<a href=\"(abandon\\.asp\\?sessionid=\\d+)\" target=\"page\">");
        if(!isEmpty(link)){
        	link = "http://" + server + ".ustitlesearch.net/" + link;
        	req = new HTTPRequest(link);        	
        	page = process(req).getResponseAsString();        	
        	// try to login
        	page = login();
        }
        
        // check that login worked
        if(!page.contains("page.asp?page=whatsnew.asp")){
        	if(!page.contains("page.asp?page=message4.asp")){
        		return new LoginResponse(LoginStatus.STATUS_INVALID_PAGE, "Invalid response");
        	}
        }
        
        // change to be on the desired county
        link = "http://" + server + ".ustitlesearch.net/page.asp?page=subscription.asp";
        page = process(new HTTPRequest(link)).getResponseAsString();        
        link = "http://" + server + ".ustitlesearch.net/subscription.asp?page=subscription.asp";
        page = process(new HTTPRequest(link)).getResponseAsString(); 
        link = extractParameter(page, "<a href=\"(changesubscription[^\"]*)\" target=\"page\">" + getState() + ", " + getCounty() + "</a>");
        if(isEmpty(link)){
        	String errorMsg = "Could not change county to: " + getState() + ", " + getCounty();
        	logger.error(errorMsg);
        	return new LoginResponse(LoginStatus.STATUS_INVALID_PAGE, errorMsg);
        }        
        link = "http://" + server + ".ustitlesearch.net/" + link;
        page = process(new HTTPRequest(link)).getResponseAsString();
        link = extractParameter(page, "parent\\.location\\.replace\\(\"([^\"]+)\"\\);"); // page.asp?page=tn/montgomery.asp
        if(isEmpty(link)){
        	logger.error("Could not find the county link 1!");
        	return new LoginResponse(LoginStatus.STATUS_INVALID_PAGE, "Could not find the county link 1!");
        }
        link = "http://" + server + ".ustitlesearch.net/" + link;
        page = process(new HTTPRequest(link)).getResponseAsString();
        link = extractParameter(page, "<frame name=\"page\" src=\"([^\"]+)\""); // tn/montgomery.asp?page=tn/montgomery.asp
        if(isEmpty(link)){
        	logger.error("Could not find the county link 2!");
        	return new LoginResponse(LoginStatus.STATUS_INVALID_PAGE, "Could not find the county link 2!");       	
        }
        link = "http://" + server + ".ustitlesearch.net/" + link;
        page = process(new HTTPRequest(link)).getResponseAsString();
        // !!! This contains the certification date !!!
        if(!page.contains("MESSAGES &amp; NOTIFICATIONS")){
        	logger.error("Page does not contain \"MESSAGES &amp; NOTIFICATIONS\"!");
        	return new LoginResponse(LoginStatus.STATUS_INVALID_PAGE, "Page does not contain \"MESSAGES &amp; NOTIFICATIONS\"!");     
        }
        
        // go to search page
        link = "http://" + server + ".ustitlesearch.net/page.asp?page=searchbypartyname.asp";
        page = process(new HTTPRequest(link)).getResponseAsString();
        link = "http://" + server + ".ustitlesearch.net/searchbypartyname.asp?page=searchbypartyname.asp";
        page = process(new HTTPRequest(link)).getResponseAsString();
        
        // perform a dummy search        
        req = new HTTPRequest( "http://" + server + ".ustitlesearch.net/PartyNameSearchResults.asp" );
        req.setMethod( HTTPRequest.POST );        
        req.setPostParameter("Action", "SEARCH");
        req.setPostParameter("Page", "1");
        req.setPostParameter("PageBase", "1");
        req.setPostParameter("PartyName", "lowrance larry");
        req.setPostParameter("PartyType", "Direct");
        req.setPostParameter("BeginningDate", "10/10/2001");
        req.setPostParameter("EndingDate", "10/10/2002");
        req.setPostParameter("InstrumentType", "0");
        req.setPostParameter("PageSize", "10");
        req.setPostParameter("IncludeSubdivisions", "ON");
        req.setPostParameter("I1.x", "47");
        req.setPostParameter("I1.y", "6");
        page = process(req).getResponseAsString(); 
        
        return new LoginResponse(LoginStatus.STATUS_SUCCESS, "Logged in");
    }
    
	@Override
    public void onBeforeRequest(HTTPRequest req) {		
		
		// don't do anything during logging in
		if(status != STATUS_LOGGED_IN){
			return;
		}
		
		// don't do anything while we're already inside a onBeforeRequest call
		if(getAttribute("onBeforeRequest") == Boolean.TRUE){
			return;
		}
		
		// mark that we're treating onBeforeRequest
		setAttribute("onBeforeRequest", Boolean.TRUE);
		
		try{			
			
			String url = req.getURL();
			// clean url
			url = url.replaceFirst("&isSubResult=true", "");
			url = url.replaceFirst("&isParentSite=true", "");
			url = url.replaceFirst("&crossRefSource=[^&]+", "");
			url = url.replaceFirst("&dummy=[^&]*", "");
			// go to correct lb server
			url = url.replace("wwwX", server);
			req.modifyURL(url);
			
			// check if it is a navigation page
			if(url.contains("PartyNameSearchResults.asp") || url.contains("SubdivisionSearchResults.asp")){
				treatNavigationLink(req);
			}
			
		} finally {
			// we're finished treating onBeforeRequest
			setAttribute("onBeforeRequest", Boolean.FALSE);
		}
    }
	
	@Override
	public void onAfterRequest(HTTPRequest req, HTTPResponse res) {
		
	}
	
	@Override
	public void onRedirect(HTTPRequest req){
	
		if(req.getRedirectLocation().contains("/page.asp")){
			destroySession();
			throw new RuntimeException("Redirected to error page: session expired!");
		}
		
	}
    
	private String login(){
		
		String passKey = getDataSite().getName();
		
		// get username and password from database
		String user = SitesPasswords.getInstance().getPasswordValue(getCurrentCommunityId(), passKey, "user");
		String pass = SitesPasswords.getInstance().getPasswordValue(getCurrentCommunityId(), passKey, "password");
		
		if(StringUtils.isEmpty(user) && StringUtils.isEmpty(pass)) {
			user = SitesPasswords.getInstance().getPasswordValue(getCurrentCommunityId(), getClass().getSimpleName(), "user");
			pass = SitesPasswords.getInstance().getPasswordValue(getCurrentCommunityId(), getClass().getSimpleName(), "password");
		}
		
		if(StringUtils.isEmpty(user) || StringUtils.isEmpty(pass)) {
			return null;
		}
		// try to login
        HTTPRequest req = new HTTPRequest( "http://" + server +".ustitlesearch.net/logon.asp?AAABBBCCC=123&action=logon&username=" + user + "&password="+ pass +"&savepassword=false" );
        HTTPResponse res = process(req);
        String page = res.getResponseAsString();
        
        return page;
	}
	
	private Map<String,String> getParams(HTTPRequest req){
		Map<String,String> params = new HashMap<String,String>();
		for(String name: (Set<String>)req.getPostParameters().keySet()){
			if(!name.endsWith(".x") &&
			   !name.endsWith(".y") &&
			   !name.equals("Action") &&
               !name.equals("Page") &&
			   !name.equals("PageBase"))
			{
				params.put(name, req.getPostFirstParameter(name));
			}
		}
		return params;		
	}
	
	/**
	 * In case we have a navigation link, try to issue the request and see what happens
	 * If there's no error, then use the response as is, by bypassing
	 * If we have an error, re-issue the original search, then set the crt page to 1, then re-issue the 
	 * navigation query
	 * @param req
	 */
	@SuppressWarnings("unchecked")
	private void treatNavigationLink(HTTPRequest req){
		
		// if it is a search starter, log its data
		if("SEARCH".equals(req.getPostFirstParameter("Action")) ){
			setAttribute("crtSearch", getParams(req));
			return;
		}
		
		// if current search is not ours
		if(!getParams(req).equals(getAttribute("crtSearch"))){
			redoSearch(req);
		}

		// construct a new request
		HTTPRequest request = new HTTPRequest(req.getURL());
		request.setMethod(HTTPRequest.POST);
		request.setPostParameters((HashMap)req.getPostParameters().clone());				
		HTTPResponse httpResponse = process(request);
		String htmlResponse = httpResponse.getResponseAsString();				
		
		// check if everything went fine
		if(httpResponse.getReturnCode() == 200 && 
		   !htmlResponse.contains("Item cannot be found in the collection corresponding to the requested name or ordinal") &&
		   !htmlResponse.contains("ADODB.Recordset") &&
		   !htmlResponse.contains("error '"))
		{					
			
			// since everything went fine, go ahead and use the result
			httpResponse.is = IOUtils.toInputStream(htmlResponse);
			req.setBypassResponse(httpResponse);		
			
		} else {
			
			// redo the search
			redoSearch(req);
		}		
	}

	/**
	 * Re-do original search in order for the navigation link to work
	 * @param req
	 */
	private void redoSearch(HTTPRequest req){
		
		if(req.getURL().contains("PartyNameSearchResults.asp")){						
			
			// redo the search					
			HTTPRequest request = new HTTPRequest(req.getURL());
			request.setMethod(HTTPRequest.POST);			
			
			request.setPostParameter("Action", "SEARCH");
			request.setPostParameter("Page", "1");
			request.setPostParameter("PageBase", "1");
			request.setPostParameter("I2.x", "47");
			request.setPostParameter("I2.y", "6");						
			request.setPostParameter("PartyName", req.getPostFirstParameter("PartyName"));
			request.setPostParameter("PartyType",  req.getPostFirstParameter("PartyType"));
			request.setPostParameter("BeginningDate", req.getPostFirstParameter("BeginningDate"));
			request.setPostParameter("EndingDate", req.getPostFirstParameter("EndingDate"));
			request.setPostParameter("InstrumentType", req.getPostFirstParameter("InstrumentType"));
			request.setPostParameter("PageSize", req.getPostFirstParameter("PageSize"));
			request.setPostParameter("IncludeSubdivisions", req.getPostFirstParameter("IncludeSubdivisions"));		
			
			// send the search request
			process(request).getResponseAsString();
			
			// set crt search params			
			setAttribute("crtSearch", getParams(req));
			
			// we currently are on the first page
			req.removePostParameters("Page");
			req.setPostParameter("Page", "1");
			
		} else if (req.getURL().contains("SubdivisionSearchResults.asp")){
			
			// create POST request
			HTTPRequest request = new HTTPRequest(req.getURL());
			request.setMethod(HTTPRequest.POST);				
			
			// set fixed parameters
			request.setPostParameter("Action","SEARCH");
			request.setPostParameter("BeginningDate", "");
			request.setPostParameter("I3.x", "74");
			request.setPostParameter("I3.y", "5");
			request.setPostParameter("Page","1");
			request.setPostParameter("PageBase", "1");
			
			// copy variable parameters from original request
			request.setPostParameter("Building", req.getPostFirstParameter("Building"));
			request.setPostParameter("District", req.getPostFirstParameter("District"));
			request.setPostParameter("EndingDate", req.getPostFirstParameter("EndingDate"));
			request.setPostParameter("InstrumentType", req.getPostFirstParameter("InstrumentType"));
			request.setPostParameter("Lot", req.getPostFirstParameter("Lot"));
			request.setPostParameter("PageSize", req.getPostFirstParameter("PageSize"));
			request.setPostParameter("Phase", req.getPostFirstParameter("Phase"));
			request.setPostParameter("Section", req.getPostFirstParameter("Section"));
			request.setPostParameter("Subdivision", req.getPostFirstParameter("Subdivision"));
			request.setPostParameter("Unit", req.getPostFirstParameter("Unit"));

			// send the search request
			process(request).getResponseAsString();	
			
			// set crt search params			
			setAttribute("crtSearch", getParams(req));
			
			// modify navigation request: we currently are on the first page
			req.removePostParameters("Page");
			req.setPostParameter("Page", "1");
			
		}		
	}
	
	public String getServer() {
		return server;
	}

}
