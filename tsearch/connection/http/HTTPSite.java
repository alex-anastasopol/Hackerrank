package ro.cst.tsearch.connection.http;

import java.io.IOException;
import java.net.UnknownHostException;
import java.util.Iterator;
import java.util.Vector;

import org.apache.commons.httpclient.DefaultHttpMethodRetryHandler;
import org.apache.commons.httpclient.Header;
import org.apache.commons.httpclient.HostConfiguration;
import org.apache.commons.httpclient.HttpMethodBase;
import org.apache.commons.httpclient.NameValuePair;
import org.apache.commons.httpclient.NoHttpResponseException;
import org.apache.commons.httpclient.URI;
import org.apache.commons.httpclient.cookie.CookiePolicy;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.commons.httpclient.methods.PostMethod;
import org.apache.commons.httpclient.methods.StringRequestEntity;
import org.apache.commons.httpclient.params.HttpMethodParams;
import org.apache.commons.httpclient.protocol.Protocol;
import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Category;

import ro.cst.tsearch.ServerConfig;
import ro.cst.tsearch.community.CommunityAttributes;
import ro.cst.tsearch.connection.LoginResponse;
import ro.cst.tsearch.connection.LoginResponse.LoginStatus;
import ro.cst.tsearch.connection.http.auth.ssl.EasySSLProtocolSocketFactory;
import ro.cst.tsearch.utils.InstanceManager;



public  class HTTPSite extends HTTPLock implements HTTPSiteInterface
{   
    protected static final Category logger= Category.getInstance(HTTPSite.class.getName());
    
    private HTTPSession session = null;
    protected HTTPSiteManager siteManager;
    private Object userData = null;
    
    protected long searchId=-1;
    protected long miServerId=-1;//TSServer
    
    protected boolean onLogOut = false;
    
    protected boolean isStopedByTimeOut = false;
    
    
    public void  setSearchId(long searchId){
    	this.searchId = searchId;
    }
    
    //set this true if you want the site manager call on logout function to be activated
    protected boolean callOnLogout = false;
    
    private long timeAmprent = Long.MAX_VALUE;
    
    //  always call setSearchId() when you use Httpsite
    protected HTTPSite() {
    };
    
    private void updateTimeAmprent(){
    	timeAmprent = System.currentTimeMillis();
    }
    
    /* (non-Javadoc)
	 * @see ro.cst.tsearch.connection.http.HTTPSiteInterface#process(ro.cst.tsearch.connection.http.HTTPRequest)
	 */
    public HTTPResponse process(HTTPRequest request)
    {
    	//if is not the logout request and callOnLogout is enabled
    	 if(siteManager!=null  && !onLogOut && callOnLogout){
         	siteManager.addHttpSite(this);
         	updateTimeAmprent();
         }
    	
        request.setHttpSite( this );
        HTTPResponse response = null;
        
        if (request == null) {
            logger.debug("HTTPSite.process() - processing error: request == null ");
            Thread.dumpStack();
        }
        
        boolean justAcquired = false;
        if (session == null)            
            justAcquired = true;
       
        boolean canContinue = false;
        if(!onLogOut){
	        siteManager.enqueue( request );
	        
	        if (session.getStatus() == HTTPSession.STATUS_NOT_KNOWN)
	        {
	            logger.debug("###Logging in " + session);
	            session.setStatus(HTTPSession.STATUS_LOGGING_IN);
	            boolean loginOK = onLogin().getStatus() == LoginStatus.STATUS_SUCCESS;
	            if (loginOK)
	                session.setStatus(HTTPSession.STATUS_LOGGED_IN);
	            else
	                session.setStatus(HTTPSession.STATUS_NOT_KNOWN);
	        }
	        
	        siteManager.enqueue( request ); 
	        canContinue  = true;
        }
        else{
        	if(siteManager.canContinue(this)){
        		canContinue  = true;
        	}
        }
        
        if (session.getStatus() != HTTPSession.STATUS_NOT_KNOWN && canContinue)
        {           
            logger.debug( 
                    //" --- EXEC (tthread: " + Thread.currentThread().toString() +
                    //" \n\tsite: " + toString() +
                    //" \n\tsession: " + session.toString() + ") "
                    " executing " + request.getURL() );
            
            response = exec(request);
        }
        
        if ( justAcquired && !isLocked() && session != null && session.getStatus() != HTTPSession.STATUS_LOGGING_IN)
        {
            siteManager.sessionUnlock(this);            
        }
        
        request.setInQueue( false );
        
       
        
        return response;            
    }
    
    private HTTPResponse exec(HTTPRequest request)
    {
        HTTPResponse response = null;
        
        try
        {
            response = new HTTPResponse();          
        
            onBeforeRequest(request);
            
            HttpMethodBase method = null;

            String url = request.getURL();
            url = url.replaceAll("\\|", "%7c");
            
            if ( request.getMethod() == HTTPRequest.GET )
            {
                method = new GetMethod( url );
            }
            else if (request.getMethod() == HTTPRequest.POST)
            {
                method = new PostMethod( request.getURL() );
                
                //set post params
                StringBuffer sb = new StringBuffer();
                
                if( request.getXmlPostData() == null ){
                
	                Vector<String> postParameterOrder = request.getPostParameterOrder();
	                
	                if( postParameterOrder == null ){
		                Iterator postParameters = request.postParameters.keySet().iterator();
		                while (postParameters.hasNext())
		                {
		                    String name = (String) postParameters.next();
		                    HTTPRequest.ParametersVector paramValues = (HTTPRequest.ParametersVector) request.postParameters.get(name);
		                    
		                    if (name != null && paramValues != null) {
		                        for( int i = 0 ; i < paramValues.size() ; i ++ )
		                        {
		                            NameValuePair postParam = new NameValuePair( name, ((String)paramValues.elementAt( i )==null)?"":(String) paramValues.elementAt( i ) );
		                            //((PostMethod) method).setParameter(name, paramValues.elementAt( i ));
		                            ((PostMethod) method).addParameter( postParam );
		                            sb.append(name + "=[" + paramValues.elementAt( i ) + "], ");
		                        }
		                    }
		                }
	                }
	                else{
	                	for( int postParamPos = 0 ; postParamPos < postParameterOrder.size() ; postParamPos ++ ){
	                		String name = postParameterOrder.elementAt( postParamPos );
	                		
		                    HTTPRequest.ParametersVector paramValues = (HTTPRequest.ParametersVector) request.postParameters.get(name);
		                    
		                    if (name != null && paramValues != null) {
		                        for( int i = 0 ; i < paramValues.size() ; i ++ )
		                        {
		                            NameValuePair postParam = new NameValuePair( name, (String) paramValues.elementAt( i ) );
		                            //((PostMethod) method).setParameter(name, paramValues.elementAt( i ));
		                            ((PostMethod) method).addParameter( postParam );
		                            sb.append(name + "=[" + paramValues.elementAt( i ) + "], ");
		                        }
		                    }                		
	                	}
	                }
                }
                else{
                	((PostMethod) method).setRequestEntity( new StringRequestEntity( request.getXmlPostData()) );
                	sb.append( request.getXmlPostData() );
                }
                
                HTTPManager.log("   POST PARAMS: " + ro.cst.tsearch.utils.StringUtils.getMaxCharacters(sb.toString(), HTTPSiteInterface.MAX_CHARACTERS_TO_LOG_ON_INFO));
                if (!"".equals(request.getEntity())) {                	
                	HTTPManager.log("   POST DATA: " + request.getEntity());                	
                }
            }
            
            method.setFollowRedirects(false);
            
            if ( getSession().getLastRequest() != null )
            {
                if ( request.getHeader("Referer")==null)
                {
                    request.setHeader("Referer", getSession().getLastRequest());
                }
            }
            
            getSession().setLastRequest(request.getURL());

            
            //set headers
            Iterator headers = request.headers.keySet().iterator();
            while (headers.hasNext())
            {
                String name = (String) headers.next();
                String value = (String) request.headers.get(name);
                if (name != null && value != null)
                    method.setRequestHeader(name, value);
            }
            
            if(siteManager.requestTimeout  == 0){ //we don't want infinite timeout
            	siteManager.requestTimeout  = -1;
            }
            
            if (siteManager.requestTimeout !=-1 ) {
                
                getSession().httpClient.getParams().setSoTimeout(siteManager.requestTimeout);
                getSession().httpClient.getParams().setConnectionManagerTimeout(siteManager.requestTimeout);
            }

            HttpMethodParams methodParams = new HttpMethodParams() ;
            
            methodParams.setSoTimeout(siteManager.requestTimeout);
            
            methodParams.setBooleanParameter( HttpMethodParams.SINGLE_COOKIE_HEADER, true );
            methodParams.setCookiePolicy( CookiePolicy.BROWSER_COMPATIBILITY );
            methodParams.setParameter(HttpMethodParams.RETRY_HANDLER, new DefaultHttpMethodRetryHandler(0, true));
            
            method.setParams( methodParams );

            String entity = request.getEntity();
            if (method instanceof PostMethod && !"".equals(entity)) {
            	entity = entity.replaceAll("@@[^@]*@@", "");
            	((PostMethod)method).setRequestEntity(new StringRequestEntity(entity));
            }
            
          //please do not delete this
            if(ServerConfig.isBurbProxyEnabled()) {
            	HostConfiguration config = getSession().httpClient.getHostConfiguration();
            	
            	if(StringUtils.isEmpty(config.getProxyHost())) {
	    	        config.setProxy("127.0.0.1", ServerConfig.getBurbProxyPort(8081));
	    	        /* Trust unsigned ssl certificates when using proxy */
	    	        Protocol.registerProtocol("https", new Protocol("https", new EasySSLProtocolSocketFactory(), 443));
            	}
            }
           
	            for ( int i = 0; i < HTTPManager.MAX_REDIRECTS; i++ )
	            {
	                int ret = 0;
	                for (int retryCountIndex = 0; retryCountIndex < 5; retryCountIndex++) {
	                	try {
		    				ret = getSession().httpClient.executeMethod(method);
		    				break;
	                	} catch (NoHttpResponseException httpResponseException) {
	                		httpResponseException.printStackTrace();
	                		try {
								Thread.sleep(1000);
							} catch (InterruptedException e) {
								e.printStackTrace();
							}
		    			} 	
					}
	                
	                
	                response.returnCode = ret;
	                
	                if ( ret == 302 && request.noRedirects )
	                    break;
	                
	                if ( ret == 302 )
	                {                   
	                    String location = method.getResponseHeader("Location").getValue();
	                    
	                    request.setRedirectLocation(location);
	        			onRedirect(request);
	        			String newLocation = request.getRedirectLocation();
	        			if (!location.equals(newLocation)) {
	        				location = newLocation;
	        			}
	                    
	                    logger.debug( "Redirecting to:" + location);
	                    if(request.isReplaceSpaceOnredirect()){
	                    	location = location.replaceAll(" ", "%20");
	                    }
	                    if (location.startsWith("/"))
	                    {
	                        logger.debug("redirect /");
	                        URI u = method.getURI();
	                        String host = u.getScheme() + "://" + u.getAuthority();
	                        
	                        location = host + location;
	                        
	                        logger.debug( "---->" + location);
	                    }
	                    else
	                    if (!location.startsWith("http"))
	                    {
	                        logger.debug("redirect ./");
	                        
	                        URI u = method.getURI(); 
	                        String host =  u.getScheme() + "://" + u.getAuthority() + u.getCurrentHierPath();
	                    
	                        logger.debug("host: " + host);
	                        logger.debug("location: " + location);
	                        
	                        while (location.indexOf("../") >= 0) {
	                            location = location.replaceFirst("../", "");
	                            host = host.substring(0, host.lastIndexOf("/"));
	                        }
	                        
	                        location = host + "/" + location;
	                        
	                        logger.debug( "---->" + location);
	                    }
	
	                    location = location.replaceAll("\\|", "%7c"); 
	                    
	                    method = new GetMethod( location );
	                    method.setFollowRedirects(false);
	                    
	                    if ( getSession() != null && getSession().getLastRequest() != null )
	                        method.setRequestHeader("Referer", location );  
	                    
	                    methodParams = new HttpMethodParams() ;
	                    
	                    methodParams.setBooleanParameter( HttpMethodParams.SINGLE_COOKIE_HEADER, true );
	                    methodParams.setCookiePolicy( CookiePolicy.BROWSER_COMPATIBILITY );
	                    method.setParams( methodParams );
	                    
	                    if ( getSession() != null )
	                        getSession().setLastRequest(location);
	                    
	                } else 
	                    break;
	            }    
	            
        
                            
            response.is = method.getResponseBodyAsStream();
            
            //get the response headers
            try{
            	
            	Header[] allHeaders = method.getResponseHeaders();
            	for( int headerIdx = 0 ; headerIdx < allHeaders.length ; headerIdx ++ ){
            		response.headers.put( allHeaders[headerIdx].getName() , allHeaders[headerIdx].getValue());
            	}
            }
            catch( Exception e ){
            	logger.info( "Error Retrieving response headers!" );
            }
            
            try
            {
                response.contentType = method.getResponseHeader("Content-Type").getValue();
            }
            catch (Exception nevermind) {};
            try
            {
                response.contentLenght = Long.parseLong( method.getResponseHeader("Content-Length").getValue());
            }
            catch (Exception nevermind) {};         
            
        }
        catch (UnknownHostException e)
        {
            HTTPManager.log(e.getMessage());
            logger.error("UnknownHostException ", e);
        }
        catch (IOException e)
        {
            HTTPManager.log(e.getMessage());
            logger.error("IOException ", e);
        }       
        
        onAfterRequest(request, response);
        
        /*try {
            logger.debug( 
                    " FINISH --------------- \n\tthread: " + Thread.currentThread().toString() +
                    " \n\tsite: " + toString() +
                    " \n\tsession: " + session.toString() +
                    " \n\t==> executed " + request.getURL() );
        } catch (Exception e) {}*/
        
        return response;
    }
    
    /* (non-Javadoc)
	 * @see ro.cst.tsearch.connection.http.HTTPSiteInterface#destroySession()
	 */
    public void destroySession()
    {
//        setInQueue(false);
        
        siteManager.sessionDestroy(this);
    }
    
    /* (non-Javadoc)
	 * @see ro.cst.tsearch.connection.http.HTTPSiteInterface#destroyAllSessions()
	 */
    public void destroyAllSessions()
    {
        //for the sites that allow only one possible session
//        setInQueue( false );
        
        siteManager.destroyAllSessions( this );
    }
    
    public void unlock()
    {
        siteManager.sessionUnlock(this);
        super.unlock();
    }
            
       public LoginResponse onLogin(){
    	   return new LoginResponse(LoginStatus.STATUS_SUCCESS, "Logged in");
    }
    
	public boolean onLogout() {
		// TODO Auto-generated method stub
		return true;
	}
    
    public void onHTTPAuth(HTTPSession session) {};
    public void onBeforeRequest(HTTPRequest req) {};
    /* (non-Javadoc)
	 * @see ro.cst.tsearch.connection.http.HTTPSiteInterface#onRedirect(ro.cst.tsearch.connection.http.HTTPRequest)
	 */
    public void onRedirect(HTTPRequest req) {};
    /* (non-Javadoc)
	 * @see ro.cst.tsearch.connection.http.HTTPSiteInterface#onAfterRequest(ro.cst.tsearch.connection.http.HTTPRequest, ro.cst.tsearch.connection.http.HTTPResponse)
	 */
    public void onAfterRequest(HTTPRequest req, HTTPResponse res) {};
    
    /* (non-Javadoc)
	 * @see ro.cst.tsearch.connection.http.HTTPSiteInterface#setSession(ro.cst.tsearch.connection.http.HTTPSession)
	 */
    public void setSession(HTTPSession session) 
    {
        this.session = session;
        
        if (session != null)
        {
            if (siteManager.getProxyIP()!=null)
            {
                session.httpClient.getHostConfiguration().setProxy(
                    siteManager.getProxyIP(), siteManager.getProxyPort());
            }       
            onHTTPAuth(session);
        }
    }

    /* (non-Javadoc)
	 * @see ro.cst.tsearch.connection.http.HTTPSiteInterface#hasSession()
	 */
    public boolean hasSession()
    {
        return session != null;
    }

    /* (non-Javadoc)
	 * @see ro.cst.tsearch.connection.http.HTTPSiteInterface#getSession()
	 */
    public HTTPSession getSession()
    {
        return session;
    }
    
    public HTTPUser getUser(String sid)
    {
        return null;
    }
    
    /* (non-Javadoc)
	 * @see ro.cst.tsearch.connection.http.HTTPSiteInterface#getUserData()
	 */
    public Object getUserData()
    {
        return userData;
    }

    /* (non-Javadoc)
	 * @see ro.cst.tsearch.connection.http.HTTPSiteInterface#setUserData(java.lang.Object)
	 */
    public void setUserData(Object userData)
    {
        this.userData = userData;
    }

	public long getMiServerId() {
		return miServerId;
	}

	public void setMiServerId(long miServerId) {
		this.miServerId = miServerId;
	}

	public long getTimeAmprent() {
		return timeAmprent;
	}

	public void setTimeAmprent(long timeAmprent) {
		this.timeAmprent = timeAmprent;
	}

	public boolean isCallOnLogoutActivated() {
		return callOnLogout;
	}

	public void setCallOnLogout(boolean callOnLogout) {
		this.callOnLogout = callOnLogout;
	}

	
    
    /*public synchronized boolean isInQueue()
    {
        if (currentRequest != null)
            return currentRequest.inQueue;
        else 
        {
            logger.debug("HTTPSite.isInQueue called but currentSession is null, return false.");
            
            return false;
        }
    }*/

    /*public synchronized void setInQueue(boolean inQueue)
    {
        if (currentRequest != null)
            currentRequest.inQueue = inQueue;
        else
            logger.debug("HTTPSite.setInQueue() called but currentRequest is null.");
    }*/
    
    /*public synchronized void setIsWaiting( boolean value )
    {
        if (currentRequest != null)
            currentRequest.isWaiting = value;
        else
            logger.debug("HTTPSite.setIsWaiting() called but currentRequest is null.");
    }*/
    
    /*public synchronized boolean isWaiting()
    {
        if (currentRequest != null)
            return currentRequest.isWaiting ;
        else
        {
            logger.debug("HTTPSite.isWaiting called but currentSession is null, return false.");
                
            return false;
        }
    }
    */
	public String getCurrentCommunityId(){
		CommunityAttributes communityAttributes = InstanceManager.getManager().getCurrentInstance(searchId).getCurrentCommunity();
		String string = communityAttributes.getID().toString();
   		return  string;
	}
	public int getCurrentCommunityIdAsInt(){
   		return  InstanceManager.getManager().getCurrentInstance(searchId).getCurrentCommunity().getID().intValue();
	}

	@Override
	public void performSpecificActionsAfterCreation() {
		// TODO Auto-generated method stub
		
	}
}
