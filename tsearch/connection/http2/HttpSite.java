/**
 * 
 */
package ro.cst.tsearch.connection.http2;

import java.io.IOException;
import java.net.ConnectException;
import java.net.InetAddress;
import java.net.URLEncoder;
import java.net.UnknownHostException;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.Set;
import java.util.Vector;

import org.apache.commons.httpclient.DefaultHttpMethodRetryHandler;
import org.apache.commons.httpclient.Header;
import org.apache.commons.httpclient.HeaderElement;
import org.apache.commons.httpclient.HostConfiguration;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpMethodBase;
import org.apache.commons.httpclient.NameValuePair;
import org.apache.commons.httpclient.NoHttpResponseException;
import org.apache.commons.httpclient.URI;
import org.apache.commons.httpclient.cookie.CookiePolicy;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.commons.httpclient.methods.PostMethod;
import org.apache.commons.httpclient.methods.StringRequestEntity;
import org.apache.commons.httpclient.methods.multipart.MultipartRequestEntity;
import org.apache.commons.httpclient.params.HttpMethodParams;
import org.apache.commons.httpclient.protocol.Protocol;
import org.apache.log4j.Logger;

import ro.cst.tsearch.Search;
import ro.cst.tsearch.ServerConfig;
import ro.cst.tsearch.community.CommunityAttributes;
import ro.cst.tsearch.connection.LoginResponse;
import ro.cst.tsearch.connection.LoginResponse.LoginStatus;
import ro.cst.tsearch.connection.http.HTTPManager;
import ro.cst.tsearch.connection.http.HTTPRequest;
import ro.cst.tsearch.connection.http.HTTPResponse;
import ro.cst.tsearch.connection.http.HTTPSession;
import ro.cst.tsearch.connection.http.HTTPSiteInterface;
import ro.cst.tsearch.connection.http.auth.ssl.EasySSLProtocolSocketFactory;
import ro.cst.tsearch.data.SitesPasswords;
import ro.cst.tsearch.exceptions.LoginException;
import ro.cst.tsearch.parser.SimpleHtmlParser;
import ro.cst.tsearch.searchsites.client.GWTDataSite;
import ro.cst.tsearch.servers.bean.DataSite;
import ro.cst.tsearch.utils.InstanceManager;
import ro.cst.tsearch.utils.Log;
import ro.cst.tsearch.utils.StringUtils;

/**
 * @author dumitru bacrau
 */
public class HttpSite implements HTTPSiteInterface {

	protected boolean singleLineCookies = true;
	
	protected static final Logger logger = Logger.getLogger(HttpSite.class);
	
	protected static final int STATUS_NOT_KNOWN   = 0;
	protected static final int STATUS_LOGGING_IN  = 1;
	protected static final int STATUS_LOGGED_IN   = 2;
	protected static final int SESSION_TIMEOUT    = 15 * 60 * 1000; // 30 minutes in ms
    protected static final String SINGLE_LINE_COOKIES_IN_REDIRECT = "SINGLE_LINE_COOKIES_IN_REDIRECT";
	
    protected int status = STATUS_NOT_KNOWN;
            	
	protected long searchId = 0;
	private HttpSiteManager siteManager = null;

	protected DataSite dataSite = null;
	private boolean destroySession = false;
	private String lastRequest = null;
	private HttpClient httpClient = null;
	
	private long lastRequestTime = 0;
	private LinkedList<Long> lastRequestTimes = new LinkedList<Long>();
	private Map<String,Object> attributes = new HashMap<String,Object>();
		
	protected int sid = 0;
	public void setSid(int sid){ 
		this.sid = sid; 
	}
	/**
	 * @param searchId the searchId to set
	 */
	public void setSearchId(long searchId) {
		this.searchId = searchId;
	}

	/**
	 * @return the searchId
	 */
	public long getSearchId() {
		return searchId;
	}
	
	public Search getSearch(){
		return InstanceManager.getManager().getCurrentInstance(getSearchId()).getCrtSearchContext();
	}

	/**
	 * @param siteManager the siteManager to set
	 */
	public void setSiteManager(HttpSiteManager siteManager) {
		this.siteManager = siteManager;
	}

	/**
	 * @return the siteManager
	 */
	public HttpSiteManager getSiteManager() {
		return siteManager;
	}

	
	/**
	 * 
	 * @param destroySession
	 */
	public void setDestroy(boolean destroySession) {
		this.destroySession = destroySession;
	}

	/**
	 * 
	 * @return
	 */
	public boolean isDestroy() {
		return destroySession;
	}
	
	/**
	 * 
	 * @param httpClient
	 */
	public void setHttpClient(HttpClient httpClient){
		this.httpClient = httpClient;
	}
	
	public HttpClient getHttpClient() {
		return httpClient;
	}
	/**
	 * Process a request
	 */
	public HTTPResponse process(HTTPRequest request) {		
		
		HTTPResponse response = null;		
		
		try{			
			
			
			// destroy session
			if(destroySession){
				httpClient = HttpSiteManager.createHttpClient(); 
				attributes.clear();
				status = STATUS_NOT_KNOWN;
				lastRequest = null;
				destroySession = false;
			}
			
			// login if necessary
			if(status == STATUS_NOT_KNOWN){
				siteManager.requestPermit(this);
				status = STATUS_LOGGING_IN;
				try{
					LoginResponse loginResponse = onLogin();
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
				siteManager.requestPermit(this);
				
				// execute request
				response = exec(request);				
			}
				
			// call after request
			onAfterRequest(request, response);	
			
		}catch(IOException e){
			
			// call after request
			onAfterRequest(request, response);
			
			logger.error(e);
			throw new RuntimeException(e);
			
		}
		
		return response;
	}
	
	@SuppressWarnings("unchecked")
	protected HTTPResponse exec(HTTPRequest request) throws IOException {

        HTTPResponse response = new HTTPResponse();
        HttpMethodBase method = null;
        
        if (request.getMethod() == HTTPRequest.GET) {
			
        	method = new GetMethod(request.getURL());
        	
		} else if (request.getMethod() == HTTPRequest.POST) {
			
			method = new PostMethod(request.getURL());
			StringBuffer sb = new StringBuffer();
			
			// set post params
			if( request.getXmlPostData() == null ){
				if (request.getXmlPostData() == null) {
					if (request.getPartPostData() == null) {
						// non XML and non multipart/form-data
						Vector<String> postParameterOrder = request.getPostParameterOrder();					
						if (postParameterOrder == null) {
							// no particular order
							for(String name: request.postParameters.keySet()){
								if(name == null){ continue;}
								HTTPRequest.ParametersVector paramValues = (HTTPRequest.ParametersVector) request.postParameters.get(name);
								if(paramValues == null){ continue; }
								for(String value: (Vector<String>)paramValues){
									if(value == null){ value = ""; }
									NameValuePair postParam = new NameValuePair(name, value);
									((PostMethod) method).addParameter(postParam);
									String toLogValue = null;
									if(value.length() > 20) {
										toLogValue = value.substring(0, 20) + "...";
									} else {
										toLogValue = value.substring(0, value.length());
									}
									sb.append(name + "=["+ toLogValue + "], ");
								}
							}
						}else{
							// enforce parameter order
							for(String name: postParameterOrder){
								if(name == null){ continue; }
								HTTPRequest.ParametersVector paramValues = (HTTPRequest.ParametersVector) request.postParameters.get(name);
								for(String value: (Vector<String>)paramValues){						
									NameValuePair postParam = new NameValuePair(name, value);
									((PostMethod) method).addParameter(postParam);
									String toLogValue = null;
									if(value.length() > 20) {
										toLogValue = value.substring(0, 20) + "...";
									} else {
										toLogValue = value.substring(0, value.length());
									}
									sb.append(name + "=["+ toLogValue + "], ");
								}
							}
						}
					} else {
						setMultipartRequestEntity(request, method);
					}
				}
			} else {
				// XML post data
				((PostMethod) method).setRequestEntity(new StringRequestEntity(request.getXmlPostData()));
				sb.append(request.getXmlPostData());
			}

			// log the post params
			logger.info("   POST PARAMS: " + StringUtils.getMaxCharacters(sb.toString(), HTTPSiteInterface.MAX_CHARACTERS_TO_LOG_ON_INFO));
			if (!"".equals(request.getEntity())) {
				logger.info("   POST DATA: " + request.getEntity());
			}
		}
            
        method.setFollowRedirects(false);
            
        // set referrer
        if (lastRequest != null ){
            if (request.getHeader("Referer")==null){
                request.setHeader("Referer", lastRequest);
            }
        }
        
        // update last request
        lastRequest = request.getURL();
        
        // set headers
        for(String name: (Set<String>)request.headers.keySet()){
        	if(name == null){ continue; }
        	String value = (String) request.headers.get(name);
        	if(value == null){ continue; }
        	
        	
        	if(name.equals("User-Agent")) {
        		if(value.equals(HTTPRequest.USER_AGENT_DEFAULT) && !HTTPRequest.USER_AGENT_DEFAULT.equals(getUserAgentValue())) {
        			//need to override the default value set in HTTPRequest constructor
        			value = getUserAgentValue();
        		}
        	}
        	
            method.setRequestHeader(name, value);
        }

        if(siteManager.getRequestTimeout() != -1){
            httpClient.getParams().setSoTimeout(siteManager.getRequestTimeout());
            httpClient.getParams().setConnectionManagerTimeout(siteManager.getRequestTimeout());
        }

		// set method params
		HttpMethodParams methodParams = new HttpMethodParams() ;
		methodParams.setSoTimeout(siteManager.getRequestTimeout());           		
		methodParams.setBooleanParameter(HttpMethodParams.SINGLE_COOKIE_HEADER, singleLineCookies);
		methodParams.setCookiePolicy(CookiePolicy.BROWSER_COMPATIBILITY );
		methodParams.setParameter(HttpMethodParams.RETRY_HANDLER, new DefaultHttpMethodRetryHandler(0, true));
		method.setParams( methodParams );

		// set entity if the case
        String entity = request.getEntity();
        if (method instanceof PostMethod && !"".equals(entity)) {
        	entity = entity.replaceAll("@@[^@]*@@", "");
           	((PostMethod)method).setRequestEntity(new StringRequestEntity(entity));
        }
        
        //please do not delete this
        if(ServerConfig.isBurbProxyEnabled()) {
        	HostConfiguration config = httpClient.getHostConfiguration();
	        config.setProxy("127.0.0.1", ServerConfig.getBurbProxyPort(8081));
        }
        
        addSpecificSiteProxy();
        
        /* Trust unsigned ssl certificates when using proxy */
        Protocol.registerProtocol("https", new Protocol("https", new EasySSLProtocolSocketFactory(), 443));
        
        // try MAX_REDIRECTS times
        String ditamaiCookie = "";
//        modifyClientBeforeExecute(httpClient,method);
        for (int i = 0; i < HTTPManager.MAX_REDIRECTS; i++) {
			int ret = 0;
			try {
				ret = httpClient.executeMethod(method);
			} catch (ConnectException e) {
				if ("Connection timed out: connect".equals(e.getMessage())){
					throw new LoginException(e.getMessage());
				} else {
					logger.error(e);
				}
			} catch (NoHttpResponseException e) {
				// retry execute method;  for now, only for ILWillTR
				ret = httpClient.executeMethod(method);				
			} catch (UnknownHostException e) {
				
				String message = "SearchID:\n " + searchId + "\n has URI = " + method.getURI().toString() + "\n\n and caused UnknownHostException. Retrying";
				
				try{
					InetAddress[] inetAddresses = InetAddress.getAllByName(method.getURI().getHost());
					if(inetAddresses == null || inetAddresses.length == 0) {
						message += "\n\nNo IP found for this host";
					} else {
						for (InetAddress inetAddress : inetAddresses) {
							message += "\n\nFound IP = " + inetAddress.getHostAddress();
						}
					}
				} catch (Exception innerException) {
					logger.error(innerException);
				}
				
				Log.sendExceptionViaEmail(e,message);
				
				
				ret = httpClient.executeMethod(method);	
			}

			response.returnCode = ret;

			if((ret != 302 && ret != 301 && ret != 303) || request.noRedirects){
				break;
			}

			String location = method.getResponseHeader("Location").getValue();
			
			request.setRedirectLocation(location);
			onRedirect(request);
			String newLocation = request.getRedirectLocation();
			if (!location.equals(newLocation)) {
				location = newLocation;
			}
			
			logger.debug("Redirecting to:" + location);
			if (request.isReplaceSpaceOnredirect()) {
				location = location.replaceAll(" ", "%20");
			}
			if (location.startsWith("/")) {
				logger.debug("redirect /");
				URI u = method.getURI();
				String host = u.getScheme() + "://" + u.getAuthority();
				location = host + location;
				logger.debug("---->" + location);
			} else if (!location.startsWith("http")) {
				logger.debug("redirect ./");
				URI u = method.getURI();
				String host = u.getScheme() + "://" + u.getAuthority() + u.getCurrentHierPath();
				logger.debug("host: " + host);
				logger.debug("location: " + location);
				while (location.indexOf("../") >= 0) {
					location = location.replaceFirst("../", "");
					host = host.substring(0, host.lastIndexOf("/"));
				}
				location = host + "/" + location;
				logger.debug("---->" + location);
			}

			location = location.replaceAll("\\|", "%7c");
			location = location.replaceAll(" ","%20");
			location = location.replaceAll("\\\\","%5C");
			
			setAttribute("redirect-cookie", method.getRequestHeader("Cookie"));
			
			Header respHeader = method.getResponseHeader("Set-Cookie");
//			Header[] cookieHeaders = method.getResponseHeaders("Set-Cookie");
			Header reqHeader = method.getRequestHeader("Cookie");
			method = new GetMethod(location);
//			processCookieHeaders(cookieHeaders, method);
			method.setFollowRedirects(false);
						
			if (getDataSite().getSiteTypeInt() == GWTDataSite.IS_TYPE){//this need for ILxxxIS to keep the Cookies
				if (i == 0){
					HeaderElement[] hElem = respHeader.getElements();
					if (hElem.length < 4) {
						ditamaiCookie = reqHeader.getValue();
					} else {
						for (int h = 0; h < hElem.length; h++){
							if (hElem[h].getValue() != null){
								ditamaiCookie += hElem[h].getName() + "=" + hElem[h].getValue() + ";";
							}	
						}
					}
				}
				method.setRequestHeader("Cookie", ditamaiCookie);
			}
				
			// update referer and last request
			if (lastRequest != null){
				method.setRequestHeader("Referer", lastRequest);
			}
			method.setRequestHeader("User-Agent", getUserAgentValue());
			method.setRequestHeader("Accept", getAccept());
			method.setRequestHeader("Accept-Language",getAcceptLanguage());
			method.setRequestHeader("Connection","Keep-Alive");
			lastRequest = location;
			
			// set method params
			methodParams = new HttpMethodParams();
			methodParams.setBooleanParameter(HttpMethodParams.SINGLE_COOKIE_HEADER, (Boolean)getAttribute(SINGLE_LINE_COOKIES_IN_REDIRECT,false));
			
			methodParams.setCookiePolicy(CookiePolicy.BROWSER_COMPATIBILITY);
			method.setParams(methodParams);
			
			siteManager.requestPermit(this);
		}    
	                   
        // set input stream
        response.is = method.getResponseBodyAsStream();
        response.setLastURI(method.getURI());
        // get the response headers
        try{
        	boolean isSingleCookie=false;
	        for(Header header: method.getResponseHeaders()){
	        	method.getParams().getBooleanParameter(HttpMethodParams.SINGLE_COOKIE_HEADER,isSingleCookie);
	        	if(response.headers.get(header.getName())==null){
	        		response.headers.put(header.getName(), header.getValue());
	        	}
	        	else{
	        		if(header.getName().equalsIgnoreCase("Set-Cookie")){
	        			/*if(this.siteName.equals("FLHernandoTR")||
	        					this.siteName.equals("FLMiamiDadeTR")||
	        					this.siteName.equals("FLMiami-DadeTR")||
	        					this.siteName.equals("FLPalm BeachTR")){ 
	        				response.headers.put(header.getName(), response.headers.get(header.getName())+" "+header.getValue());
	        			}*/
	        			response.headers.put(header.getName(), response.headers.get(header.getName())+" "+header.getValue());
	        		}
	        	}
	        }
	    }catch(Exception e){
        	logger.info("Error Retrieving response headers!" );
        }
           
	    if (method.getResponseHeader("Content-Type") == null){ // ILWillTR has no Content-Type
	    	response.contentType = "text/html";
	    }
	    
		// set content type
		try{
			response.contentType = method.getResponseHeader("Content-Type").getValue();
		}catch (RuntimeException ignored) {};
		
		// set content length
		try{
			response.contentLenght = Long.parseLong(method.getResponseHeader("Content-Length").getValue());
		}catch (RuntimeException ignored) {};                
	       
        return response;
       
	}


	protected void setMultipartRequestEntity(HTTPRequest request, HttpMethodBase method) {
		// multipart/form-data
		MultipartRequestEntity mre = new MultipartRequestEntity(request.getPartPostData(), method.getParams());
		request.setHeader("Content-Type", mre.getContentType());
		((PostMethod) method).setRequestEntity(mre);
	}
	
	//	protected void modifyClientBeforeExecute(HttpClient httpClient2, HttpMethodBase method) {
//		// TODO Auto-generated method stub
//		
//	}
//	protected void processCookieHeaders(Header[] cookieHeaders, HttpMethodBase method) {
//		
//	}
	/**
	 * Empty implementation
	 * @param req
	 */	
	public void onBeforeRequest(HTTPRequest req) {
		// don't do anything during logging in
		if (status != STATUS_LOGGED_IN) {
			return;
		}

		// don't do anything while we're already inside a onBeforeRequest call
		if (getAttribute("onBeforeRequest") == Boolean.TRUE) {
			return;
		}

		// mark that we're treating onBeforeRequest
		setAttribute("onBeforeRequest", Boolean.TRUE);
		try {

			onBeforeRequestExcl(req);

		} finally {
			// we're out of onBeforeRequest
			setAttribute("onBeforeRequest", Boolean.FALSE);
		}
	}
	
	/**
	 * Empty implementation
	 * @param req
	 */
	public void onBeforeRequestExcl(HTTPRequest req){}
	
	/**
	 * Empty implementation
	 */
	public void onAfterRequest(HTTPRequest req, HTTPResponse res) {}

	/**
	 * Empty implementation
	 */
	public LoginResponse onLogin() { return new LoginResponse(LoginStatus.STATUS_SUCCESS, "Logged in"); }
	
	/**
	 * Empty implementation
	 */
	public boolean onLogout() { return false; }

	/**
	 * Empty implementation
	 */
	public void onRedirect(HTTPRequest req) {}
			
	/** 
	 * Destroy current site
	 * The exception will go up the stack and the 
	 * HttpManager will destroy the session
	 */
	public void destroySession() {
		destroySession = true;
		//throw new RuntimeException("DestroySession");
	}

	/**
	 *  not used in new connection
	 *  @throws RuntimeException
	 */	
	@Deprecated
	public void destroyAllSessions() {
		//throw new RuntimeException("Unsupported method: destroyAllSessions");
	}

	/**
	 *  not used in new connection
	 *  @throws RuntimeException
	 *  
	 */
	@Deprecated
	public void setSession(HTTPSession session) {
		throw new RuntimeException("Unsupported method: setSession");
	}
	
	/**
	 *  not used in new connection
	 *  @throws RuntimeException
	 */
	@Deprecated
	public boolean hasSession() {
		throw new RuntimeException("Unsupported method: hasSession");
	}

	/**
	 *  not used in new connection
	 *  @throws RuntimeException
	 */
	@Deprecated
	public HTTPSession getSession() {
		throw new RuntimeException("Unsupported method: getSession");
	}

	/**
	 *  not used in new connection
	 *  @throws RuntimeException
	 */
	@Deprecated
	public void setUserData(Object userData) {
		throw new RuntimeException("Unsupported method: setUserData");
	}
	
	/**
	 *  not used in new connection
	 *  @throws RuntimeException
	 */
	@Deprecated
	public Object getUserData() {
		throw new RuntimeException("Unsupported method: getUserData");
	}

	/**
	 *  not used in new connection
	 *  @throws RuntimeException
	 */
	@Deprecated
	public void lock(){
		throw new RuntimeException("Unsupported method: lock");
	}
	
	/**
	 *  not used in new connection
	 *  @throws RuntimeException
	 */
	@Deprecated
	public void unlock(){
		throw new RuntimeException("Unsupported method: unlock");
	}

	/**
	 * 
	 * @param lastQueryTime
	 */
	public void setLastQueryTime(long lastQueryTime) {
		this.lastRequestTime = lastQueryTime;
	}
	
	/**
	 * 
	 * @param queryTime
	 */
	public void addLastQueryTime(long queryTime){
		lastRequestTimes.add(queryTime);
	}

	/**
	 * 
	 * @return
	 */
	public long getLastQueryTime() {
		return lastRequestTime;
	}
	
	/**
	 * Compute how much time we need to sleep
	 * @param timeUnit
	 * @param maxRequestsPerTimeUnit
	 * @return time to sleep
	 */
	public long computeSleepTime(int timeUnit, int maxRequestsPerTimeUnit){
	
		// verify that we have valid parameters
		if(timeUnit <= 0 || maxRequestsPerTimeUnit <= 0){
			throw new RuntimeException("Invalid parameters!");
		}
			
		// remove expired time stamps
		long crtTime = System.currentTimeMillis();
		while(lastRequestTimes.size() > 0){
			if(crtTime - lastRequestTimes.getFirst() > timeUnit){
				lastRequestTimes.removeFirst();
			} else {
				break;
			}
		}

		// if there are less than max requests in last (part of) window, then we let go
		if(lastRequestTimes.size() < maxRequestsPerTimeUnit){
			return 0;
		}
		
		// we have to wait TU * N / MAX - W
		// so that (in equal spacing case) we achieve the maximum density
		// this a heuristic, in some cases we might end up waiting more or less than needed
		// depending on how the queries are distributed in the current window, but we can live with that
		return (timeUnit * lastRequestTimes.size()) / maxRequestsPerTimeUnit 
		       - (crtTime - lastRequestTimes.getLast());
				
	}
	
	@Override
	public String toString(){
		return "HttpSite(searchId=" + searchId + ",lastRequestTime=" + lastRequestTime + ")";
	}
	
	protected Object getTransientSearchAttribute(String name){
		Search search = InstanceManager.getManager().getCurrentInstance(searchId).getCrtSearchContext();
		return search.getAdditionalInfo(getDataSite().getName() + ":" + name);
	}
	
	protected void setTransientSearchAttribute(String name, Object value){
		Search search = InstanceManager.getManager().getCurrentInstance(searchId).getCrtSearchContext();
		search.setAdditionalInfo(getDataSite().getName() + ":" + name, value);	
	}
	
	protected void removeTransientSearchAttribute(String name){
		Search search = InstanceManager.getManager().getCurrentInstance(searchId).getCrtSearchContext();
		search.removeAdditionalInfo(getDataSite().getName() + ":" + name);		
	}	

	protected Object getAttribute(String name, Object defaultValue){
		if(attributes.get(name)==null)
			return defaultValue;
		return attributes.get(name);
	}
	
	protected Object getAttribute(String name){
		return attributes.get(name);
	}
	
	protected void setAttribute(String name, Object value){
		attributes.put(name, value);
	}
	
	protected void removeAttribute(String name){
		attributes.remove(name);
	}
	
	protected void clearAttributes(){
		attributes.clear();
	}
	
	/**
	 * Get county
	 * @return
	 */
	protected String getCounty(){
		return getDataSite().getCountyName();
	}
	
	/**
	 * Get state
	 * @return
	 */
	protected String getState(){
		return getDataSite().getStateAbrev();
	}
	
	/**
	 * Execute a method and obtain the response
	 * @param request
	 * @return response
	 */
	protected String execute(HTTPRequest request){
		return process(request).getResponseAsString();
	}

	/**
	 * Get password parameter
	 * @param name
	 * @return
	 */
	protected String getPassword(String name){
		return SitesPasswords.getInstance().getPasswordValue(getCurrentCommunityId(), getDataSite().getName(), name);
	}
	
	public String getCurrentCommunityId(){
		CommunityAttributes communityAttributes = InstanceManager.getManager().getCurrentInstance(searchId).getCurrentCommunity();
		String string = communityAttributes.getID().toString();
   		return  string;
	}
	
	public String getCurrentCommunityId(long searchId){
		CommunityAttributes communityAttributes = InstanceManager.getManager().getCurrentInstance(searchId).getCurrentCommunity();
		String string = communityAttributes.getID().toString();
   		return  string;
	}
	
	public int getCurrentCommunityIdAsInt(){
   		return  InstanceManager.getManager().getCurrentInstance(searchId).
   			getCurrentCommunity().getID().intValue();
	}
	
	public static HTTPResponse executeSimplePostRequest(HTTPRequest request) {
		try {
			return executeSimplePostRequest(request,10000,true);
		} catch (IOException e) {
			e.printStackTrace();
		}
		return new HTTPResponse();
	}
	
	public static HTTPResponse executeSimplePostRequest(HTTPRequest request,int timeout, boolean singleLineCookies) throws IOException {
		
		HTTPResponse response = new HTTPResponse();
        HttpClient httpClient = new HttpClient();
        HttpMethodBase method = null;;
        
        if (request.getMethod() == HTTPRequest.GET) {
			
        	method = new GetMethod(request.getURL());
        	
		} else if (request.getMethod() == HTTPRequest.POST) {
			
			method = new PostMethod(request.getURL());
			StringBuffer sb = new StringBuffer();
			
			// set post params
			if( request.getXmlPostData() == null ){
				if (request.getXmlPostData() == null) {
					// non XML
					Vector<String> postParameterOrder = request.getPostParameterOrder();					
					if (postParameterOrder == null) {
						// no particular order
						for(String name: request.postParameters.keySet()){
							if(name == null){ continue;}
							HTTPRequest.ParametersVector paramValues = (HTTPRequest.ParametersVector) request.postParameters.get(name);
							if(paramValues == null){ continue; }
							for(String value: (Vector<String>)paramValues){
								if(value == null){ value = ""; }
								NameValuePair postParam = new NameValuePair(name, value);
								((PostMethod) method).addParameter(postParam);
								sb.append(name + "=["+ value + "], ");
							}
						}
					}else{
						// enforce parameter order
						for(String name: postParameterOrder){
							if(name == null){ continue; }
							HTTPRequest.ParametersVector paramValues = (HTTPRequest.ParametersVector) request.postParameters.get(name);
							for(String value: (Vector<String>)paramValues){						
								NameValuePair postParam = new NameValuePair(name, value);
								((PostMethod) method).addParameter(postParam);
								sb.append(name + "=["+ value + "], ");
							}
						}
					}
				}
			} else {
				// XML post data
				((PostMethod) method).setRequestEntity(new StringRequestEntity(request.getXmlPostData()));
				sb.append(request.getXmlPostData());
			}

			// log the post params
			logger.info("   POST PARAMS: " + StringUtils.getMaxCharacters(sb.toString(), HTTPSiteInterface.MAX_CHARACTERS_TO_LOG_ON_INFO));
			if (!"".equals(request.getEntity())) {
				logger.info("   POST DATA: " + request.getEntity());
			}
		}
            
        method.setFollowRedirects(false);
            
        // set headers
        for(String name: (Set<String>)request.headers.keySet()){
        	if(name == null){ continue; }
        	String value = (String) request.headers.get(name);
        	if(value == null){ continue; }
            method.setRequestHeader(name, value);
        }

        httpClient.getParams().setSoTimeout(timeout);
        httpClient.getParams().setConnectionManagerTimeout(timeout);

		// set method params
		HttpMethodParams methodParams = new HttpMethodParams() ;
		methodParams.setSoTimeout(timeout);           		
		methodParams.setBooleanParameter(HttpMethodParams.SINGLE_COOKIE_HEADER, singleLineCookies);
		methodParams.setCookiePolicy(CookiePolicy.BROWSER_COMPATIBILITY );
		methodParams.setParameter(HttpMethodParams.RETRY_HANDLER, new DefaultHttpMethodRetryHandler(0, true));
		method.setParams( methodParams );

		// set entity if the case
        String entity = request.getEntity();
        if (method instanceof PostMethod && !"".equals(entity)) {
        	entity = entity.replaceAll("@@[^@]*@@", "");
           	((PostMethod)method).setRequestEntity(new StringRequestEntity(entity));
        }
        
        //please do not delete this
        if(ServerConfig.isBurbProxyEnabled()) {
        	HostConfiguration config = httpClient.getHostConfiguration();
	        config.setProxy("127.0.0.1", ServerConfig.getBurbProxyPort(8081));
	        /* Trust unsigned ssl certificates when using proxy */
	        Protocol.registerProtocol("https", new Protocol("https", new EasySSLProtocolSocketFactory(), 443));
        }
        
        // try MAX_REDIRECTS times
        for (int i = 0; i < HTTPManager.MAX_REDIRECTS; i++) {
			int ret = httpClient.executeMethod(method);
			response.returnCode = ret;

			if(ret != 302 || request.noRedirects){
				break;
			}

			String location = method.getResponseHeader("Location").getValue();
			logger.debug("Redirecting to:" + location);
			if (request.isReplaceSpaceOnredirect()) {
				location = location.replaceAll(" ", "%20");
			}
			if (location.startsWith("/")) {
				logger.debug("redirect /");
				URI u = method.getURI();
				String host = u.getScheme() + "://" + u.getAuthority();
				location = host + location;
				logger.debug("---->" + location);
			} else if (!location.startsWith("http")) {
				logger.debug("redirect ./");
				URI u = method.getURI();
				String host = u.getScheme() + "://" + u.getAuthority() + u.getCurrentHierPath();
				logger.debug("host: " + host);
				logger.debug("location: " + location);
				while (location.indexOf("../") >= 0) {
					location = location.replaceFirst("../", "");
					host = host.substring(0, host.lastIndexOf("/"));
				}
				location = host + "/" + location;
				logger.debug("---->" + location);
			}

			location = location.replaceAll("\\|", "%7c");
			location = location.replaceAll(" ","%20");
						
			method = new GetMethod(location);
			method.setFollowRedirects(false);
				
		// set method params
			methodParams = new HttpMethodParams();
			methodParams.setBooleanParameter(HttpMethodParams.SINGLE_COOKIE_HEADER, singleLineCookies);
			
			methodParams.setCookiePolicy(CookiePolicy.BROWSER_COMPATIBILITY);
			method.setParams(methodParams);

			request.setRedirectLocation(location);

		}    
	                   
        // set input stream
        response.is = method.getResponseBodyAsStream();
           
        // get the response headers
        try{
        	boolean isSingleCookie=false;
	        for(Header header: method.getResponseHeaders()){
	        	method.getParams().getBooleanParameter(HttpMethodParams.SINGLE_COOKIE_HEADER,isSingleCookie);
	        	if(response.headers.get(header.getName())==null){
	        		response.headers.put(header.getName(), header.getValue());
	        	}
	        }
	    }catch(Exception e){
        	logger.info("Error Retrieving response headers!" );
        }
           
		// set content type
		try{
			response.contentType = method.getResponseHeader("Content-Type").getValue();
		}catch (RuntimeException ignored) {};
		
		// set content length
		try{
			response.contentLenght = Long.parseLong(method.getResponseHeader("Content-Length").getValue());
		}catch (RuntimeException ignored) {};                
	       
        return response;
	}
	
	/**
	 * Parses the htmlResponse, finds the form with <em>formName</em> and tries to extract all the parameters in the given array.<br>
	 * In at least one parameter for <em>toFill</em> is empty the method returns null
	 * @param htmlResponse the response to process
	 * @param toFill the keys to load
	 * @param formName the form to parse
	 * @return a hash which has all the elements in <em>toFill</em> as keys and as values their corresponding data in the form or empty string
	 */
	public static Map<String, String> fillAndValidateConnectionParams(String htmlResponse, String[] toFill, String formName) {
		Map<String, String> params = new SimpleHtmlParser(htmlResponse).getForm(formName).getParams();
		Map<String,String> addParams = new HashMap<String,String>();
		for (String key : toFill) {
			String value = "";
			if (params.containsKey(key)) {
				value = params.get(key);
				if(StringUtils.isEmpty(value)) {
					return null;
				}
			} else {
				return null;
			}
			addParams.put(key, value);
		}
		return addParams;
	}
	
	/**
	 * Parses the htmlResponse, finds the form with <em>formName</em> and tries to extract all the parameters in the given array.<br>
	 * In at least one parameter for <em>toValidate</em> is empty the method returns null
	 * @param htmlResponse the response to process
	 * @param toFill the keys to load
	 * @param toValidate the keys that must not be empty in the form
	 * @param formName the form to parse
	 * @return a hash which has all the elements in <em>toFill</em> as keys and as values their corresponding data in the form or empty string
	 */
	public static Map<String, String> fillAndValidateConnectionParams(String htmlResponse, String[] toFill, String[] toValidate, String formName) {
		Map<String, String> params = new SimpleHtmlParser(htmlResponse).getForm(formName).getParams();
		Map<String,String> addParams = new HashMap<String,String>();
		for (String key : toFill) {
			addParams.put(key, params.get(key)); 
			
		}
		for (int i = 0; i < toValidate.length; i++) {
			if(StringUtils.isEmpty(addParams.get(toValidate[i]))) {
				return null;
			}
		}
		return addParams;
	}
	
	/**
	 * Parses the htmlResponse, finds the form with <em>formName</em> and tries to extract all the parameters in the given array.<br>
	 * <b>It does not matter if they are found or not</b>
	 * @param htmlResponse the response to process
	 * @param toFill the keys to load is available
	 * @param formName the form to parse
	 * @return a hash which has all the elements in <em>toFill</em> as keys and as values their corresponding data in the form
	 */
	public static Map<String, String> fillConnectionParams(String htmlResponse, String[] toFill, String formName) {
		Map<String, String> params = new SimpleHtmlParser(htmlResponse).getForm(formName).getParams();
		Map<String,String> addParams = new HashMap<String,String>();
		for (String key : toFill) {
			String value = "";
			if (params.containsKey(key)) {
				value = params.get(key);
			} 
			addParams.put(key, value);
		}
		return addParams;
	}
	@Override
	public void performSpecificActionsAfterCreation() {
		// TODO Auto-generated method stub
		
	}
	public boolean isDestroySession() {
		return destroySession;
	}
	public void setDestroySession(boolean destroySession) {
		this.destroySession = destroySession;
	}
	public Map<String, Object> getAttributes() {
		return attributes;
	}
	public void setAttributes(Map<String, Object> attributes) {
		this.attributes = attributes;
	}
	/**
	 * @return the link set in SearchSites for this site
	 */
	public String getSiteLink () {
		return getDataSite().getLink();
	}
	/**
	 * Returns the full DataSite object set in SearchSites. <br>
	 * @return the DataSite for the current site
	 */
	public DataSite getDataSite () {
		return dataSite;
	}
	public void setDataSite(DataSite dataSite) {
		this.dataSite = dataSite;
	}
	
	public String getUserAgentValue() {
		return HTTPRequest.USER_AGENT_DEFAULT;
	}
	
	public String getAccept() {
		return "image/gif, image/x-xbitmap, image/jpeg, image/pjpeg, application/vnd.ms-excel, application/vnd.ms-powerpoint, application/msword, application/x-shockwave-flash, */*";
	}
	
	public String getAcceptLanguage() {
		return "en-us";
	}
	public String getLastRequest() {
		return lastRequest;
	}
	public void setLastRequest(String lastRequest) {
		this.lastRequest = lastRequest;
	}	
	
	protected void addSpecificSiteProxy() {
		
	}
}
