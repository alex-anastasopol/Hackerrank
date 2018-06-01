package ro.cst.tsearch.connection.http3;

import java.util.HashMap;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.Semaphore;

import org.apache.commons.codec.binary.Base64;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.http.HttpStatus;
import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONException;

import ro.cst.tsearch.connection.ATSConnConstants;
import ro.cst.tsearch.connection.LoginResponse;
import ro.cst.tsearch.connection.LoginResponse.LoginStatus;
import ro.cst.tsearch.connection.http.HTTPRequest;
import ro.cst.tsearch.connection.http.HTTPResponse;
import ro.cst.tsearch.data.SitesPasswords;
import ro.cst.tsearch.exceptions.LoginException;
import ro.cst.tsearch.servers.bean.DataSite;

public class GenericSiteADI extends HttpSite3 implements Runnable {

	private static final int					MAX_TIMEOUT_INTERRUPT_REQUEST			= 210000;
	private static final int					MAX_PARENT_SITE_CONSECUTIVE_REQUESTS	= 5;

	private LinkedBlockingQueue<HTTPRequest>	requestParentSiteQueue;
	private LinkedBlockingQueue<HTTPRequest>	requestAutomaticQueue;
	private Semaphore							requestSemaphore;
	private int									parentSiteExecutionCounter;
	private Thread								myThread;
	
	private String								encodedCredentials						= "";

	
	
	public GenericSiteADI() {
		requestParentSiteQueue = new LinkedBlockingQueue<HTTPRequest>();
		requestAutomaticQueue = new LinkedBlockingQueue<HTTPRequest>();
		requestSemaphore = new Semaphore(0);
		parentSiteExecutionCounter = 0;
		
		myThread = new Thread(this);
		myThread.start();
		
	}
	
	public LoginResponse onLogin(long searchId, DataSite dataSite) {

		HTTPRequest reqLogin = new HTTPRequest(dataSite.getLink(), HTTPRequest.GET);
		
		String key = dataSite.getName();
		
		String userId = SitesPasswords.getInstance().getPasswordValue(getCurrentCommunityId(searchId), key, "user");
		String password = SitesPasswords.getInstance().getPasswordValue(getCurrentCommunityId(searchId), key, "password");

		if (StringUtils.isEmpty(userId) || StringUtils.isEmpty(password)) {
			key = "TXGenericADI";
			userId = SitesPasswords.getInstance().getPasswordValue(getCurrentCommunityId(searchId), key, "user");
			password = SitesPasswords.getInstance().getPasswordValue(getCurrentCommunityId(searchId), key, "password");
			
			if (StringUtils.isEmpty(userId) || StringUtils.isEmpty(password)) {
				return LoginResponse.getDefaultInvalidCredentialsResponse();
			}
		}

		try {
			encodedCredentials = new String(Base64.encodeBase64((userId + ":" + password).getBytes()));
		} catch (Exception e) {
			logger.error("Credentials encoding failed!");
		}

		if (StringUtils.isEmpty(encodedCredentials)) {
			return new LoginResponse(LoginStatus.STATUS_INVALID_CREDENTIALS, "Invalid credentials");
		}
		reqLogin.setHeader("Authorization", "Basic " + encodedCredentials);

		HTTPResponse res = process(reqLogin);
		int returnCode = res.getReturnCode();
		if(returnCode == HttpStatus.SC_UNAUTHORIZED) {
			return LoginResponse.getDefaultInvalidCredentialsResponse();
		} else {
			String response = res.getResponseAsString();
			if (res.getContentType().contains("json")) {
				try {
					JSONArray jsonArray = new JSONArray(response);
					if (jsonArray.length() > 0) {
						// indicate success
						return new LoginResponse(LoginStatus.STATUS_SUCCESS, "Logged in");
					} else {
						return new LoginResponse(LoginStatus.STATUS_INVALID_PAGE, "Counties page not returned");
					}
				} catch (JSONException e) {
					logger.error("JSONException on parsing counties page: \n" + e);
					return new LoginResponse(LoginStatus.STATUS_INVALID_PAGE, "Counties page not returned");
				}
			} else if(response.contains("The Title Plant System is down for maintenance")) {
				return new LoginResponse(LoginStatus.STATUS_OUT_OF_BUSINESS_HOURS, "The Title Plant System is down for maintenance");
			} else {
				return new LoginResponse(LoginStatus.STATUS_INVALID_PAGE, "Unexpected login page returned");
			}
		}

	}

	@Override
	public void onBeforeRequestExcl(HTTPRequest req) {

		String url = req.getURL();
		if (url.contains("&isSubResult=true")) {
			url = url.replaceFirst("(?is)&isSubResult=true", "");
		}
		if (url.endsWith("&instrumentFake=")) {
			url = url.replaceFirst("(?is)&instrumentFake=.*", "");
		}
		
		url = url.replaceAll("(?is)(&|\\?)from_md=(?:$|&)", "$1");
		url = url.replaceAll("(?is)(&|\\?)from_year=(?:$|&)", "$1");
		url = url.replaceAll("(?is)(&|\\?)to_md=(?:$|&)", "$1");
		url = url.replaceAll("(?is)(&|\\?)to_year=(?:$|&)", "$1");
		url = url.replaceAll("(?is)(&|\\?)sec_lo=(?:$|&)", "$1");
		url = url.replaceAll("(?is)(&|\\?)sec_hi=(?:$|&)", "$1");
		
		req.setURL(url);
		
		req.setHeader("Authorization", "Basic " + encodedCredentials);

		super.onBeforeRequestExcl(req);
	}
	
	@Override
	public void onAfterRequest(HTTPRequest req, HTTPResponse res) {
		if (res != null && (res.returnCode == HttpStatus.SC_INTERNAL_SERVER_ERROR 
				|| res.returnCode == HttpStatus.SC_GATEWAY_TIMEOUT)) {
			res.returnCode = HttpStatus.SC_OK;
		}
	}
	
	public HTTPResponse addAndExecuteRequest(HTTPRequest request) {
		try {
			synchronized (request) {
				if("1".equals(request.getPostFirstParameter("isParentSite"))){
					requestParentSiteQueue.add(request);
				} else {
					requestAutomaticQueue.add(request);
				}
				requestSemaphore.release();
				request.wait();
			}
			
		} catch (InterruptedException e) {
			logger.error(searchId + ": Interrupt in addAndExecuteRequest", e);
		}
		return request.getBypassResponse();
	}
	
	@Override
	public void run() {
		
		
		Timer timerNow = null;
		SiteTimer timer = null;
		
		while(true) {
			HTTPRequest request = null;
			try {
				requestSemaphore.acquire();
				
				if(parentSiteExecutionCounter < MAX_PARENT_SITE_CONSECUTIVE_REQUESTS) {
					request = requestParentSiteQueue.poll();
					if(request != null) {
						parentSiteExecutionCounter++;
					} else {
						request = requestAutomaticQueue.poll();
						parentSiteExecutionCounter = 0;
					}
				} else {
					request = requestAutomaticQueue.poll();
					if(request == null) {
						request = requestParentSiteQueue.poll();
					} else {
						parentSiteExecutionCounter = 0;
					}
				}
				
				timerNow = new Timer("GenericSiteADITimer");
				timer = new SiteTimer(this);
				timerNow.schedule((TimerTask) timer, MAX_TIMEOUT_INTERRUPT_REQUEST, MAX_TIMEOUT_INTERRUPT_REQUEST);
				
				HTTPResponse response = process(request);
				if(response == null ) {
					logger.error(request.getSearchId() + " GenericSiteADI: Error while processing request - response is null - retry");
					response = process(request);
				} else {
					
					if(!"application/pdf".equals(response.getContentType())) {
						String responseAsString = response.getResponseAsString();
						if(responseAsString.contains("Site error") || responseAsString.contains("No End Of Search Received")) {
							logger.error(request.getSearchId() + " GenericSiteADI: Error while processing request - response is Site error - retry");
							response = process(request);
						} else {
							response.is = IOUtils.toInputStream(responseAsString);
						}
					}
				}
				request.setBypassResponse(response);
				
			} catch (Exception e) {
				logger.error(searchId + ": Error while running thread", e);
				HTTPResponse response = new HTTPResponse();
				response.is = IOUtils.toInputStream("Exception received: " + e.getMessage());
				response.returnCode = ATSConnConstants.HTTP_OK;
				response.contentType = "text/html";
				response.headers = new HashMap<String, String>();
				if(request != null) {
					request.setBypassResponse(response);
				}
				
			} finally {
				if(request != null) {
					synchronized (request) {
						request.notify();	
					}
				}
				timerNow.cancel();
				timer.cancel();
			}
		}
	}
	
	/**
	 * Process a request
	 */
	public HTTPResponse process(HTTPRequest request) {		
		
		HTTPResponse response = null;		
		
		try{
			
			setSearchId(request.getSearchId());
			
			// destroy session
			if(isDestroySession()){
				setHttpClient(HttpSiteManager3.createHttpClient());
				getAttributes().clear();
				setLastRequest(null);
				status = STATUS_NOT_KNOWN;
				setDestroySession(false);
			}
			
			// login if necessary
			if(status == STATUS_NOT_KNOWN){
				getSiteManager().requestPermit(this);
				status = STATUS_LOGGING_IN;
				try{
					LoginResponse loginResponse = onLogin(request.getSearchId(), request.getDataSite());
					if(loginResponse.getStatus() == LoginStatus.STATUS_SUCCESS){
						status = STATUS_LOGGED_IN;
					} else {
						status = STATUS_NOT_KNOWN;
						throw new LoginException("Login failed with message <" + loginResponse.getMessage() + ">!");
					}
				}catch(RuntimeException e){
					status = STATUS_NOT_KNOWN;
					throw e;
				}
			}
			
			// call before request
			onBeforeRequest(request);
			
			if(request.getBypassResponse() != null){
				
				// use the bypass response set by onBeforeRequest
				response = request.getBypassResponse();
				
			} else {

				// enforce timing constraints
				getSiteManager().requestPermit(this);
				
				// execute request
				response = exec(request);				
			}
				
			// call after request
			onAfterRequest(request, response);	
			
		}catch(Exception e){
			
			// call after request
			onAfterRequest(request, response);
			
			logger.error(e);
			throw new RuntimeException(e);
			
		}
		
		return response;
	}
	
	private class SiteTimer extends TimerTask {
		
		GenericSiteADI managedSite;

		public SiteTimer(GenericSiteADI managedSite) {
			this.managedSite = managedSite;
			
		}
		
		@Override
		public void run() {
			boolean debug = true;
			if(debug) {
				managedSite.myThread.interrupt();
			} 
		}
	}
}