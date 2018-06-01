package ro.cst.tsearch.connection.http;


import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Set;
import java.util.TreeSet;
import java.util.Vector;

import org.apache.log4j.Category;
import ro.cst.tsearch.utils.InstanceManager;


public class HTTPSiteManager extends Thread
{
	protected static final Category logger = Category.getInstance(HTTPSiteManager.class.getName());
	
	String siteName, wrapper;
	public String getSiteName() { return siteName; };	
	
	private Set<HTTPSite> myHttpSitesList = new HashSet<HTTPSite>();
	
	public synchronized void addHttpSite(HTTPSite s) {
		myHttpSitesList.add(s);
	}
	
	public synchronized boolean removeHttpSite(HTTPSite s) {
		return myHttpSitesList.remove(s);
	}
	
	private long timeToCallOnLogout = 1000 * 60 * 5;
	
	
	
	private  synchronized  void callOnLogOut() {
		
		long curentTime = System.currentTimeMillis();
		Vector<HTTPSite> vec = new Vector<HTTPSite>();
		
		for( HTTPSite site : myHttpSitesList ){
			if(site.isCallOnLogoutActivated()){
				if( curentTime  - site.getTimeAmprent()  >  timeToCallOnLogout){
					//System.err.println(curentTime  - site.getTimeAmprent() +"   " + timeToCallOnLogout +"  expresion " +(curentTime  - site.getTimeAmprent()  >  timeToCallOnLogout));
					try{
						if(!site.isStopedByTimeOut){
							System.err.println("&&&&&&&&&&&&& &&&& ----  APEL ON LOGOUT");
							site .onLogout();
						}
					}
					catch(Exception e){
						e.printStackTrace();
					}
					vec.add(site);
				}
			}
			else{
				vec.add(site);
			}
		}
		
		for(int i=0;i<vec.size();i++){
			myHttpSitesList.remove(vec.get(i));
		}
		
	}
	
	// locks	
	Object stats = new Object();
	Object todo = new Object();
	Object queue = new Object();
	
	// statistics
	int statMaxAtOnce = 0;
	int statTotalServed = 0;
	int statActive = 0;
	
	// requests thread queue
	LinkedList requestQueue = new LinkedList();
	
	long lastRequestTime = 0;
		
	//int maxRequestsAtOnce = 0; 		/* max ongoing requests on the site at a time */	
	int maxRequestsPerTimeUnit = 0; /* max requests per second */
	int timeUnit = 1000;	
	int timeBetweenRequests = 0;
	int maxSessions;
		
	///////////
	LinkedList requestTimes = new LinkedList();	
			
	//proxy
	String proxyIP = null;
	int proxyPort = 3128;
	
	//req timeout
	int requestTimeout = -1;
	
	
	public boolean canContinue(HTTPSiteInterface s, HTTPSession session)
	{
		ensureStarted();
		long ctime = System.currentTimeMillis();
		
		////logger.debug( "deci <" + (ctime - session.lastRequestTime) + ">");
		
		if ( timeBetweenRequests > 0 ) 			
			if ( ctime - session.lastRequestTime < timeBetweenRequests )
			{
				//logger.debug("nu se poate");			
				return false;					
			}
		
		if ( maxRequestsPerTimeUnit > 0) // if requestsPerSecond is activated
		{						
			if ( session.requestTimes.size() >= maxRequestsPerTimeUnit )
			{
				//clean up old requests
				try
				{
					while ( true )
					{
						Long first = (Long) session.requestTimes.getFirst();
						if ( ctime - first.longValue() > timeUnit )
						{
							//logger.debug("-" + (ctime - first.longValue()) + " " + timeUnit );
							session.requestTimes.removeFirst();
							
						}
						else
							break;
					}
				} catch (Exception nevermind) {};
			}
				
			//if count of all requests that passed through in the last second is max then fail
			if ( session.requestTimes.size() >= maxRequestsPerTimeUnit  )
			{
				//logger.debug("session " + session + " cannot continue because size = " + session.requestTimes.size());
				return false;
			}
			// else am mutat mai jos requestTimes.addLast(new Long(ctime));
							
		}
		
		session.requestTimes.addLast(new Long(ctime));

		return true;
	}
	
	public void setSession( HTTPSession session ){
		//adds a session to the sessions list
		if( !sessions.contains( session ) ){
			sessions.add( session );
		}
	}
	
	public HTTPSession getFirstSession(){
		//get the first session in the list
		
		HTTPSession session = null;
		
		Iterator it = sessions.iterator();
		while (it.hasNext())
		{					
			session = (HTTPSession) it.next();
			if ( session.isLocked() )
			{
				session = null;
				continue;
			}
			
			break;					
		}
		
		return session;
	}
	
	public boolean canContinue(HTTPSiteInterface s)
	{
		
		HTTPSession session = null;		
		if ( s.getSession() != null )
		{
			session = s.getSession();
			if ( !canContinue(s, session) )
				return false;
		}
		else
		{
			if (maxSessions == 0)
			{
				session = new HTTPSession(sessions.size());				
			}
			else
			if (maxSessions > sessions.size()) //pot sa mai fac o sesiune?
			{
				session = new HTTPSession(sessions.size());						
				sessions.add(session);
				if (!canContinue(s, session))
					session = null;
			}
			else
			{				
				Iterator it = sessions.iterator();
				while (it.hasNext())
				{					
					session = (HTTPSession) it.next();
					if ( session.isLocked() )
					{
						session = null;
						continue;
					}
					
					if ( !canContinue(s, session) )
					{
						session = null;
						continue;
					}
					
					break;					
				}
			}			
		}	
		
		if (session == null)
			return false;		
		
		lastRequestTime = System.currentTimeMillis();
		session.lastRequestTime = lastRequestTime;
		
		session.lock();
		s.setSession(session);
						
		return true;
	}
	
	public void run()
	{
		boolean more = true;
		while (true)
		{		
			try
			{	
					callOnLogOut() ;
				
				if (!more)
					Thread.sleep(500);
				else
					Thread.sleep(50);
				
				//logger.debug(" VERIFY --------------- \n\t size: " + requestQueue.size());
				
				synchronized (queue)
				{	
					try
					{						
						more = false;
						Iterator requests = requestQueue.iterator();
						while (requests.hasNext())
						{
							HTTPRequest req = (HTTPRequest) requests.next();
							
							//logger.debug(" VERIFY --------------- \n\t size: " +
							//		"\n\tsite: " + s.toString());
							
							if (canContinue(req.getHttpSite()))
							{
								synchronized (req)
								{
									//logger.debug(Thread.currentThread().toString() + " can continue." );
									if ( req.getIsWaiting() ) {
                                        req.notify();
										requests.remove();
									}
								}
							}
							else
							{
								//logger.debug(Thread.currentThread().toString() + " cannot continue. " + requestQueue.size() + " in queue.");
								more = true;
							}
						}
					}
					catch (Exception e)
					{	
						e.printStackTrace();
						break; //start again
					}
				}
				
			}
			catch (Exception e)
			{
				e.printStackTrace();
			}		
		}		
	}
	
	public void leave()
	{				
		synchronized (stats)
		{
			statTotalServed ++;
			statActive --;
		}		
	}
	
	private static String wrapperprefix = "ro.cst.tsearch.connection.http.";
	
	public  HTTPSiteManager(String siteName,String wrapper,int timeBetweenRequests,int maxSessions,
			int requestTimeout ,int maxRequestsPerTimeUnit,int timeUnit,String proxyIP,int proxyPort )
	{
		this.maxSessions = maxSessions;
		this.siteName = siteName;
		this.wrapper = wrapperprefix + wrapper;
		this.timeBetweenRequests = timeBetweenRequests;
		this.requestTimeout = requestTimeout;
		this.maxRequestsPerTimeUnit = maxRequestsPerTimeUnit;
		this.timeUnit = timeUnit;
		this.proxyIP = proxyIP;
		this.proxyPort = proxyPort;
		this.setName("HTTPSiteManager - " + siteName);
	}
	
	public String toString()
	{
		return "SiteManager " + getSiteName() 
				+ " reqPerSecond:" + maxRequestsPerTimeUnit
				+ " timeout:" + requestTimeout
				+ "]";
	}	
	
	public HTTPSite getSite(long searchId,long miServerId)
	{
		try
		{
			
			HTTPSite s = (HTTPSite) Class.forName(wrapper).newInstance();
			//always call setSearchId() after you instanciate a Httpsite
			s.setSearchId(searchId);
			//			always call setMiServerId() after you instanciate a Httpsite
			s.setMiServerId(miServerId);
			
			//addHttpSite(s);
			
			s.siteManager = this;
			return s;
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}
		return null;
	}
	
	public boolean enqueue(HTTPRequest request)
	{
		ensureStarted();
		
		if ( request.getInQueue() )
			return true;
		// trecere la nivel cu calea ferata industriala
		try
		{
			// add the request to the queue
			synchronized (queue)
			{
				requestQueue.add(request);	
			}
			
			//	wait for the site manager to wake us up
			synchronized (request)
			{   /* we don't want losing notify() semnals */
                request.setIsWaiting( true );
				//logger.debug(Thread.currentThread().toString() + " waiting in queue");
                request.wait();
				//logger.debug(Thread.currentThread().toString() + " escaped from queue");
                request.setIsWaiting( false );
			}
			
		}
		catch (Exception e)
		{
			e.printStackTrace();
			return false;
		}
				
		request.setInQueue( true );
        
		return true;		
	}

	public String getProxyIP() {
		return proxyIP;
	}

	public int getProxyPort() {
		return proxyPort;
	}

	TreeSet sessions = new TreeSet();
	
	public void sessionUnlock(HTTPSiteInterface site)
	{
		//synchronized (site) {
			
			if (site.getSession() != null )
			{			
				//logger.debug("Session unlocked: " + site.getSession().toString());
				HTTPSession session = site.getSession();
				session.unlock();
				site.setSession(null);
				
			} 
			else 
			{
				//logger.debug("Session to unlock already null.");
			}
		//}
	}
	
    public void destroyAllSessions(HTTPSiteInterface site)
    {
        site.setSession( null );
        
        Iterator it = sessions.iterator();
        HTTPSession session;
        
        while (it.hasNext())
        {                   
            session = (HTTPSession) it.next();
            
            session.unlock();
            it.remove();
        }
    }
    
	public void sessionDestroy(HTTPSiteInterface site)
	{
		//synchronized (site) {
			
			if (site.getSession() != null )
			{			
				//logger.debug("Session released: " + site.getSession().toString());				
				HTTPSession session = site.getSession();
				session.unlock();
				site.setSession(null);
			
				sessions.remove(session);
			}
			else
			{
				//logger.debug("Session to release already null.");
			}
		//}
	}
	
	public static HTTPSiteInterface pairHTTPSiteForTSServer(String ssiteID,long searchId,int  miServerId){
		
		//String siteKey = searchId + ssiteID;
		String siteKey = InstanceManager.getManager().getCurrentInstance(searchId).getCrtSearchContext().getSearchID() + ssiteID;
	   
		HTTPSiteInterface site = InstanceManager.getManager().getCurrentInstance(searchId).getSite(siteKey);
	
		try {
			
			if (site == null) {
				site = HTTPManager.getSite( ssiteID , siteKey ,searchId,miServerId);
				InstanceManager.getManager().getCurrentInstance(searchId).setSite(siteKey, site);
			}
			if (site == null) {
				site = HTTPManager.getSite( ssiteID,searchId,miServerId );
				InstanceManager.getManager().getCurrentInstance(searchId).setSite(siteKey, site);
			}
		}
		catch(Exception e){
			e.printStackTrace(System.err);
		}
		
		return site;
	}

	public long getTimeToCallOnLogout() {
		return timeToCallOnLogout;
	}

	public void setTimeToCallOnLogout(long timeToCallOnLogout) {
		this.timeToCallOnLogout = timeToCallOnLogout;
	}
	
	private boolean started = false;
	
	private synchronized void ensureStarted(){
		if(started){
			return;
		}
		started = true;
		start();
	}
	
}
