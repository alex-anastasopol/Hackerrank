package ro.cst.tsearch.connection.http2;

import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import org.apache.commons.io.IOUtils;
import org.apache.log4j.Logger;

import ro.cst.tsearch.connection.LoginResponse;
import ro.cst.tsearch.connection.LoginResponse.LoginStatus;
import ro.cst.tsearch.connection.http.HTTPRequest;
import ro.cst.tsearch.connection.http.HTTPResponse;
import ro.cst.tsearch.data.SitesPasswords;
import ro.cst.tsearch.utils.StringUtils;

public class MIWayneRO extends HttpSite {

	protected static final Logger logger = Logger.getLogger(MIWayneRO.class);
	
	private volatile long		lastAccessTimeMillis	= System.currentTimeMillis();
	private volatile boolean	alive 					= true;
	private volatile boolean	onLogout 				= false;
	
	{
		ScheduledThreadPoolExecutor threadpool = new ScheduledThreadPoolExecutor(1);
		threadpool.scheduleAtFixedRate( 
				new Runnable(){
					public void run() {
						try{
							if( ( ( System.currentTimeMillis() - lastAccessTimeMillis) >= 180000 )  && alive ){
								onLogout();
								destroySession();
								alive = false;
							}
						}
						catch(Exception e){
							e.printStackTrace();
							destroySession();
							alive = false;
						}
					}
				}
				,0,2, TimeUnit.MINUTES
		);
	}
	
	
	/**
	 * login
	 */
	@Override
	public LoginResponse onLogin() {

		// get username and password from database
		String userName = SitesPasswords.getInstance().getPasswordValue(getCurrentCommunityId(), "MIWayneRO", "user_" + sid);
		String password = SitesPasswords.getInstance().getPasswordValue(getCurrentCommunityId(), "MIWayneRO", "password_" + sid);
		
		//go to login page
		HTTPRequest req = new HTTPRequest("http://www.waynecountylandrecords.com/Login.aspx");
		HTTPResponse res = process(req);
		String pageResponse = res.getResponseAsString();
		String viewState = StringUtils.getTextBetweenDelimiters("\"__VIEWSTATE\" value=\"", "\"", pageResponse);
		
		if("".equals(viewState)){
			logger.error("view state not found in the login page");
			return new LoginResponse(LoginStatus.STATUS_INVALID_PAGE, "VIEWSTATE not found in the login page");
		}
		
		//post login data
		req = new HTTPRequest("http://www.waynecountylandrecords.com/Login.aspx");
		req.setMethod(HTTPRequest.POST);
		req.setPostParameter("__EVENTTARGET", "btnLogon");
		req.setPostParameter("__EVENTARGUMENT", "0");
		req.setPostParameter("__VIEWSTATE", viewState);
		req.setPostParameter("MainMenu1:MT", "");
		req.setPostParameter("MainMenu1:timerData", "-1");
		req.setPostParameter("MainMenu1:timerUrl", "0");
		req.setPostParameter("UserName", userName);
		req.setPostParameter("Password", password);
		req.setPostParameter("btnLogon__10", ":0");
		res = process(req);
		pageResponse = res.getResponseAsString();

		boolean mustPurge = 
			pageResponse.contains("The account identifed by the provided Logon Name has encountered it's log on limit") ||
			pageResponse.contains("The account identified by the provided Logon Name is already logged-on");
		
		if (mustPurge) {
			
			// user already logged in, purge session
			String message = StringUtils.extractParameter(pageResponse, "<label for=\"lstPrevSessions_\\d\">([^<]+)</label>");
			info("onLogin :already logged in:" + message);
			
			viewState = StringUtils.getTextBetweenDelimiters("\"__VIEWSTATE\" value=\"", "\"", pageResponse);
			if("".equals(viewState)){
				logger.error("view state not found in the login+purge page");
				return new LoginResponse(LoginStatus.STATUS_INVALID_PAGE, "VIEWSTATE not found in the login page");
			}			

			req = new HTTPRequest("http://www.waynecountylandrecords.com/Login.aspx");
			req.setMethod(HTTPRequest.POST);
			req.setPostParameter("__EVENTTARGET", "btnLogon");
			req.setPostParameter("__EVENTARGUMENT", "0");
			req.setPostParameter("__VIEWSTATE", viewState);
			req.setPostParameter("MainMenu1:MT", "");
			req.setPostParameter("MainMenu1:timerData", "-1");
			req.setPostParameter("MainMenu1:timerUrl", "0");
			req.setPostParameter("UserName", userName);
			req.setPostParameter("Password", password);
			req.setPostParameter("btnLogon__10", ":0");
			req.setPostParameter("lstPrevSessions:0", "on");
			res = process(req);
			pageResponse = res.getResponseAsString();
		}

		// extract session id, userId and log them
		String sessionId = null, userId = null;
		try{
			sessionId = StringUtils.extractParameter(pageResponse, "Session ID: (\\d+)");
			userId = StringUtils.extractParameter(pageResponse, "User ID: '([^']+)'");
			info("onLogin loggedIn: sessionId=" + sessionId + " userId=" + userId);
		}catch(RuntimeException ignored){}
		
		// check that everything went OK
		if(StringUtils.isEmpty(userId) || StringUtils.isEmpty(sessionId)){
			return new LoginResponse(LoginStatus.STATUS_UNKNOWN, "Login failed");
		}

		// get the search page, for viewState
		HTTPRequest loginReq = new HTTPRequest("http://www.waynecountylandrecords.com/RealEstate/SearchEntry.aspx");
		res = process(loginReq);
		pageResponse = res.getResponseAsString();		
		viewState = StringUtils.getTextBetweenDelimiters( "\"__VIEWSTATE\" value=\"" , "\"", pageResponse);
		if("".equals(viewState)){
			logger.error("view state not found in the search page loaded after successfull login");
			return new LoginResponse(LoginStatus.STATUS_INVALID_PAGE, "VIEWSTATE not found in the search page loaded after successfull login");
		}
		
		setAttribute("viewState", viewState);		
		return new LoginResponse(LoginStatus.STATUS_SUCCESS, "Logged in");
	}
	
	/**
	 * logout
	 */
	@Override
	public boolean onLogout() {
		onLogout = true;
		try{
			if(status != STATUS_LOGGED_IN){
				info("onLogout: not logged in!");
				return false;
			}
			String pageResponse = "";
	
			boolean success = false;
			String sessionId="";
			for (int i = 0; i < 3; i++) {
				try {
					HTTPRequest req = new HTTPRequest("http://www.waynecountylandrecords.com/logout.aspx");
					HTTPResponse res = process(req);
					pageResponse = res.getResponseAsString();
					success = true;
					StringUtils.extractParameter(pageResponse, "Summary for session #(\\d+):");
					String minutesConnected = StringUtils.extractParameter(pageResponse, "Minutes Connected</td><td[^>]*>(\\d+)</td>");
					info("onLogout: sessionId=" + sessionId + " minutes Connected=" + minutesConnected);
					break;
				} catch (Exception e) {
					info("onLogout: exception " + (i + 1) + "/3");
				}
			}
	
			if (!success) {
				return false;
			}
			
	
			if (pageResponse.indexOf("Invalid") > 0) {
				info("onLogout: Invalid sessionId="+sessionId);
				return false;
			}
			
			if (pageResponse.indexOf("Log Off Confirmation") >= 0) {
				info("onLogout: Log Off Confirmation sessionId="+sessionId);
				return true;
			}
			
			info("onLogout: Success");
			return false;
		}
		finally{
			onLogout = false;
		}
	}
	
	/**
	 * before request
	 */
	@Override
	public void onBeforeRequest(HTTPRequest req) {
		
		if( !onLogout ){
			lastAccessTimeMillis = System.currentTimeMillis();
			alive 				 = true;
		}
		
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
			
			// add view state parameter if necessary, for search
			if("Search".equals(req.getPostFirstParameter("__EVENTTARGET"))){
				req.setPostParameter("__VIEWSTATE", (String)getAttribute("viewState"));
				return;
			} 
			
			// treat intermediate click case
			String origUrl = req.getURL();
			String url = origUrl;
			url = url.replaceFirst("&inst=[^&]*","");
			url = url.replaceFirst("&doct=[^&]*","");
			url = url.replaceFirst("&dummy=[^&]*", "");
			url = url.replaceFirst("&isSubResult=true", "");
			url = url.replaceFirst("&crossRefSource=[^&]+", "");
			url = url.replaceFirst("&parentSite=true", "");
			req.modifyURL(url);			

			if(url.contains("type=dtl")){
				
				// try to get the details
				HTTPRequest httpRequest = new HTTPRequest(url);
				HTTPResponse httpResponse = process(httpRequest);				
				String htmlResponse = httpResponse.getResponseAsString();
				
				if(htmlResponse.contains("Security check failed: access denied") ||
				   htmlResponse.contains("No results available") ||
				   htmlResponse.contains("No item available for the requested action") ||
				   htmlResponse.contains("SearchResults.aspx?AccessDetail=false")){
					// search by instrument and doctype
					if(!instrSearch(origUrl, true)){
						info("Image could not be retrieved!");
					}					
				} else {
					httpResponse.is = IOUtils.toInputStream(htmlResponse);
					req.setBypassResponse(httpResponse);
				}
				
				return;
			}
			
			// treat get image click case
			if(url.contains("type=img")){
								
				// get image intermediate page
				url = url.replaceFirst("&inst=[^&]*&doct=[^&]*", "");
				info("get image interm page: " + url);
				HTTPRequest req2 = new HTTPRequest(url);
				req2.setHeader("Referer", "http://www.waynecountylandrecords.com/RealEstate/SearchDetail.aspx");
				String page = process(req2).getResponseAsString();
				
				// check to see if we were given access 
				String imageLink = StringUtils.extractParameter(page, "var _v1LoadFile = ['\"]([^'\"]+)['\"]");
				if(page.contains("Security check failed: access denied") ||
				   page.contains("No item available for the requested action") ||
				   "".equals(imageLink)){
					
					info("get image interm page returned error. force search by instr no + doctype");
					
					// perform the instrument search
					if(!instrSearch(origUrl, true)){
						info("Image could not be retrieved!");
					}
					
					// get the intermediate image page again
					HTTPRequest req3 = new HTTPRequest(url);
					req3.setHeader("Referer", "http://www.waynecountylandrecords.com/RealEstate/SearchDetail.aspx");
					page = process(req3).getResponseAsString();										
				}
				
				// get the image itself
				imageLink = StringUtils.extractParameter(page, "var _v1LoadFile = ['\"]([^'\"]+)['\"]");
				if("".equals(imageLink)){
					info("Image could not be retrieved!");
					throw new RuntimeException("Image could not be retrieved!");
				}
				
				imageLink = "http://www.waynecountylandrecords.com/" + imageLink;			
				req.modifyURL(imageLink);
				
				return;
			}
			
		}finally{
			// we're out of onBeforeRequest
			setAttribute("onBeforeRequest", Boolean.FALSE);
		}
	}
	
	/**
	 * Empty parameters to be sent when creating an instr + doctype search
	 */
	private static final String [] EMPTY_INSTR_SEARCH_PARAMS = {
		"Mainmenu1:MT",            
		"f:d1_hidden",             
		"fxd1_input",              
		"f_d1_DrpPnl_Calendar1",   
		"f:d2_hidden",             
		"fxd2_input",              
		"f_d2_DrpPnl_Calendar1",   
		"f:t4",                    
		"f:t5",                    
		"f:t1",                    
		"f:t2",                    
		"f:r1",                    
		"f:t21",                   
		"f:t22",                   
		"f:t23",                   
		"f:t31",                   
		"f:d31",                   
		"f:t32",                   
		"f:t33",                   
		"f:txtSubdivision",        
		"f:txtSDBook",             
		"f:txtSDPage",             
		"f:t41",                   
		"f:t42",                   
		"f:t44",                   
		"f:t43",                   
		"f:txtCondo",              
		"f:txtCPlanNo",            
		"f:t52",                   
		"f:t53",                   
		"f:t56",                   
		"f:t57",                   
		"f:d61",                   
		"f:d62",                   
		"f:d63",                   
		"f:d64",                   
		"f:d7",                    
		"f:t71"		
	};

	
	/**
	 * Search for a certain instrument on the document server
	 * @param url
	 * @param force
	 * @return
	 */
	private boolean instrSearch(String url, boolean force){
		
		String inst = StringUtils.extractParameter(url, "inst=([A-Z0-9]+)");		
		String doct = StringUtils.extractParameter(url, "doct=([A-Z0-9]+)");
		
		// check if we have both inst and doct
		if("".equals(inst) || "".equals(doct)){
			logger.error("Could not find doct and inst in: " + url);
			return false;
		}
		
		info("perfom search: inst=" + inst + " doct=" + doct);
		
		HTTPRequest req = new HTTPRequest("http://www.waynecountylandrecords.com/RealEstate/SearchEntry.aspx");
		req.setMethod(HTTPRequest.POST);

		// set relevant params
		req.setPostParameter("f:t3", inst);
		req.setPostParameter("f:d3", doct);

		// set viewstate
		req.setPostParameter("__VIEWSTATE", (String)getAttribute("viewState"));     

		// set fixed params
		req.setPostParameter("__EVENTTARGET","Search");           
		req.setPostParameter("__EVENTARGUMENT","0");         
		req.setPostParameter("Mainmenu1:timerData","-1");     
		req.setPostParameter("Mainmenu1:timerUrl","0");  
		req.setPostParameter("Search__10",":0");

		// set empty params
		for(String param: EMPTY_INSTR_SEARCH_PARAMS){
			req.setPostParameter(param, "");
		}
		
		// issue the request
		process(req).getResponseAsString();
		
		return true;		
	}

	/**
	 * log a message, together with instance id and session id
	 * @param message
	 */
	private void info(String message) {
		logger.info("search=" + searchId + " sid=" + sid + " :" + message);
	}

	@Override
	public void onRedirect(HTTPRequest req){
		String location = req.getRedirectLocation();
		if(location.contains("?e=newSession") || location.contains("?e=sessionTerminated")){
			setDestroy(true);
			throw new RuntimeException("Redirected to " + location + ". Session needs to be destroyed");
		}
	}
	
}
