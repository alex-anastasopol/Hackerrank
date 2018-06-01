package ro.cst.tsearch.connection.http3;

import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import org.apache.http.client.HttpClient;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.impl.conn.PoolingClientConnectionManager;
import org.apache.log4j.Logger;

import ro.cst.tsearch.connection.http.HTTPCleanerConections;
import ro.cst.tsearch.servers.bean.DataSite;

/**
 * 
 * @author Oprina George
 *
 * Feb 27, 2013
 */
public class HttpSiteManager3 {
	
	/**
	 * HttpClient connection manager
	 */
	private static  PoolingClientConnectionManager connectionManager = null;	
	
	protected static final Logger logger = Logger.getLogger(HttpSiteManager3.class);
	
	/**
	 * Create connection manager
	 */	
	static {
		connectionManager = new PoolingClientConnectionManager ();
		HTTPCleanerConections.addManager(connectionManager);
		connectionManager.setDefaultMaxPerRoute(250);
		connectionManager.setMaxTotal(10000);
	}		

	private static final String WRAPPER_PREFIX = "ro.cst.tsearch.connection.http3.";
	
	private final int maxRequestsPerTimeUnit;
	private final int timeUnit;	
	private final int timeBetweenRequests;
	private final int absTimeBetweenRequests;
	private final int maxSessions;
	private final String wrapper;
	//private final String siteName;
	private final DataSite dataSite;
	private final int requestTimeout; 	
	@SuppressWarnings("unused")
	private final String proxyIP;
	@SuppressWarnings("unused")
	private final int proxyPort;	
		
	private Map<Long,HttpSite3> activeSites = new LinkedHashMap<Long,HttpSite3>();
	private Set<HttpSite3> inactiveSites = new HashSet<HttpSite3>();
	private Set<Long> inUseSites = new HashSet<Long>();
	
	private int crtSessions = 0;
	
	public HttpSiteManager3(DataSite dat) {
		this.dataSite = dat;
		this.maxSessions = dat.getMaxSessions();
		//this.siteName = dat.getName();
		this.wrapper = WRAPPER_PREFIX + dat.getClassConnFilename();
		this.timeBetweenRequests = dat.getTimeBetweenRequests();
		
		// we don't want infinite timeout
		if(dat.getConnectionTimeout() == 0) {
			this.requestTimeout = -1;
		} else {
			this.requestTimeout = dat.getConnectionTimeout();
		}
		this.maxRequestsPerTimeUnit = dat.getMaxRequestsPerSecond();
		this.timeUnit = dat.getUnits();
		this.proxyIP = null;
		this.proxyPort = 3128;		
		this.absTimeBetweenRequests = dat.getAbsTimeBetweenRequests();
		
		
	}


	/**
	 * @return the requestTimeout
	 */
	public int getRequestTimeout() {
		return requestTimeout;
	}

	
	/**
	 * Get a HttpSite
	 * @param searchId
	 * @return
	 */
	public synchronized HttpSite3 getSite(long searchId){
		
		// wait for the other module(s) of the search
		// to release the site, in order to allow, for example,
		// automatic in parallel with parent site or image view
		while(inUseSites.contains(searchId)){
			try{
				wait();
			}catch(InterruptedException e){
				Thread.currentThread().interrupt();
			}
		}
		
		// mark the search id as in use
		// we do it here, before any of the getSite methods
		// exits the synchonized region by doing a wait()
		inUseSites.add(searchId);
		
		// obtain the HttpSite
		HttpSite3 site = null;
		switch(maxSessions){
			case -1: site = getSiteNoSes(searchId); break;
			case  0: site = getSiteInfSes(searchId); break;
			default: site = getSiteFixSes(searchId); break;
		}
		
		// set the HttpSite search id
		site.setSearchId(searchId);
		
		// mark session for destruction if it was not used lately
		if(site.getLastQueryTime() != 0 && System.currentTimeMillis() - site.getLastQueryTime() > HttpSite3.SESSION_TIMEOUT){
			site.onLogout();
			site.setDestroy(true);
		}
		site.performSpecificActionsAfterCreation();
		return site;
	}
	
	/**
	 * Release a HttpSite
	 * @param site
	 */
	public synchronized void releaseSite(HttpSite3 site){
		switch(maxSessions){
			case -1: releaseSiteNoSes(site); break;
			case 0:  releaseSiteInfSes(site); break;
			default: releaseSiteFixSes(site); break;
		}
		inUseSites.remove(site.getSearchId());
		notifyAll();
	}
	
	/**
	 * Destroy all sites related to the  given search id
	 * @param searchId
	 */
	public synchronized void destroySearch(long searchId){
		switch(maxSessions){
			case -1: destroySerchNoSes(searchId); break;
			case 0:  destroySearchInfSes(searchId); break;
			default: destroySearchFixSes(searchId); break;
		}
	}

	/**
	 * Create a site when there's no session
	 * @param searchId
	 * @return
	 */
	private HttpSite3 getSiteNoSes(long searchId){
		HttpSite3 site = null;
		if(inactiveSites.size() > 0){
			site = extractFirst(inactiveSites);
		} else {
			site = createHttpSite();
		}
		return site;
	}
	
	/**
	 * Create site when there's one session per search and sessions are unlimited
	 * @param searchId
	 * @return
	 */
	private HttpSite3 getSiteInfSes(long searchId){
		HttpSite3 site = null;
		if(activeSites.containsKey(searchId)){
			site = activeSites.remove(searchId);
		} else if (inactiveSites.size() > 0){
			site = extractFirst(inactiveSites);
		} else {
			site = createHttpSite();
		}
		return site;
	}

	/**
	 * Create site when there's only a fixed number of possible sessions
	 * @param searchId
	 * @return
	 */
	private HttpSite3 getSiteFixSes(long searchId){
		HttpSite3 site = null;
		while(site == null){
			if(activeSites.containsKey(searchId)){
				site = activeSites.remove(searchId);				
			} else if (inactiveSites.size() > 0){
				site = extractFirst(inactiveSites);
			} else if(crtSessions < maxSessions){				
				site = createHttpSite();
				site.setSid(crtSessions);
				crtSessions += 1;
			} else if(activeSites.size() >0){
				long removeSearchId = -1;				
				for(Map.Entry<Long, HttpSite3> entry: activeSites.entrySet()){
					removeSearchId = entry.getKey();
					site = entry.getValue();
					break;
				}
				activeSites.remove(removeSearchId);				
			} else {
				try{
					wait();
				}catch(InterruptedException e){
					Thread.currentThread().interrupt();
				}
			}
		}
		return site;
	}

	/**
	 * Release site when there's no session
	 * @param site
	 */
	private void releaseSiteNoSes(HttpSite3 site){
		inactiveSites.add(site);
	}
	
	/**
	 * Release site when there's one session per search and sessions are unlimited
	 * @param site
	 */
	private void releaseSiteInfSes(HttpSite3 site){
		activeSites.put(site.getSearchId(), site);
	}
	
	/**
	 * Release site when there's only a fixed number of possible sessions
	 * @param site
	 */
	private void releaseSiteFixSes(HttpSite3 site){
		activeSites.put(site.getSearchId(), site);
	}
	
	/**
	 * 
	 * @param searchId
	 */
	private void destroySerchNoSes(long searchId){
		
	}
	
	/**
	 * 
	 * @param searchId
	 */
	private void destroySearchInfSes(long searchId){
		if(activeSites.containsKey(searchId)){
			HttpSite3 site = activeSites.remove(searchId);
			inactiveSites.add(site);
		}
	}
	
	/**
	 * 
	 * @param searchId
	 */
	private void destroySearchFixSes(long searchId){
		if(activeSites.containsKey(searchId)){
			HttpSite3 site = activeSites.remove(searchId);
			inactiveSites.add(site);
		}		
	}

	/**
	 * Create a new HttpSite instance
	 * @return
	 */
	private HttpSite3 createHttpSite(){	
		try{	
			HttpSite3 site = (HttpSite3)Class.forName(wrapper).newInstance();
			site.setDataSite(dataSite);
			site.setHttpClient(createHttpClient());
			site.setSiteManager(this);
			return site;		
		} catch(Exception e){
			throw new RuntimeException(e);
		}
	}
	
	/**
	 * Create a Http Client
	 * @return
	 */
	public static HttpClient createHttpClient(){
		DefaultHttpClient httpclient = new DefaultHttpClient(connectionManager);
//		httpclient.setKeepAliveStrategy(new ConnectionKeepAliveStrategy() {
//
//		    public long getKeepAliveDuration(HttpResponse response, HttpContext context) {
//		        // Honor 'keep-alive' header
//		        HeaderElementIterator it = new BasicHeaderElementIterator(
//		                response.headerIterator(HTTP.CONN_KEEP_ALIVE));
//		        while (it.hasNext()) {
//		            HeaderElement he = it.nextElement();
//		            String param = he.getName(); 
//		            String value = he.getValue();
//		            if (value != null && param.equalsIgnoreCase("timeout")) {
//		                try {
//		                    return Long.parseLong(value) * 1000;
//		                } catch(NumberFormatException ignore) {
//		                }
//		            }
//		        }
//		        HttpHost target = (HttpHost) context.getAttribute(
//		                ExecutionContext.HTTP_TARGET_HOST);
////		        if ("www.naughty-server.com".equalsIgnoreCase(target.getHostName())) {
////		            // Keep alive for 5 seconds only
////		            return 5 * 1000;
////		        } else {
//		            // otherwise keep alive for 30 seconds
//		            return 30 * 1000;
////		        }
//		    }
//		    
//		});
		
		return httpclient;
	}
	
	/**
	 * Extract first element from non-empty collection
	 * @param collection
	 * @return
	 */
	private HttpSite3 extractFirst(Collection<HttpSite3> collection){
		for(HttpSite3 site: collection){
			collection.remove(site);
			return site;
		}
		throw new RuntimeException("Empty collection!");
	}
	
	private volatile long lastRequestTime = 0;
	
	
	/**
	 * Enforce the timing policy 
	 */
	public synchronized void requestPermit(HttpSite3 site){
		
		// enforce absolute time between requests
		if(absTimeBetweenRequests > 0){					
			long sleep = absTimeBetweenRequests - (System.currentTimeMillis() - lastRequestTime);
			boolean first = true;
			while(sleep > 0){
				sleep += 10;
				if(logger.isInfoEnabled()){
					if(first){
						logger.info(getDataSite().getName() + ": enforce ATR=" + absTimeBetweenRequests);
						first = false;
					}
					logger.info(getDataSite().getName() + ": sleep " + sleep);
				}					
				try{				
					TimeUnit.MILLISECONDS.sleep(sleep);
				}catch(InterruptedException e){
					Thread.currentThread().interrupt();
					return;
				}
				sleep = absTimeBetweenRequests - (System.currentTimeMillis() - lastRequestTime);
			}
			if(!first && logger.isInfoEnabled()){ 
				logger.info(getDataSite().getName() + ": enforce ATR Done.");
			}
		}
		
		// enforce the time between requests
		if (timeBetweenRequests > 0){ 	
			long sleep = timeBetweenRequests - (System.currentTimeMillis() - site.getLastQueryTime());
			boolean first = true;
			while(sleep > 0){
				sleep += 10;
				if(logger.isInfoEnabled()){
					if(first){
						logger.info(getDataSite().getName() + ": enforce TBR=" + timeBetweenRequests);
						first = false;
					}
					logger.info(getDataSite().getName() + ": sleep " + sleep);
				}				
				try{
					TimeUnit.MILLISECONDS.sleep(sleep);
				}catch(InterruptedException e){
					Thread.currentThread().interrupt();
					return;
				}
				sleep = timeBetweenRequests - (System.currentTimeMillis() - site.getLastQueryTime());
			}
			if(!first && logger.isInfoEnabled()){ 
				logger.info(getDataSite().getName() + ": enforce TBR Done.");
			}			
		}
		
		// enforce the max number of queries in a certain amount of time
		if(maxRequestsPerTimeUnit > 0 && timeUnit > 0){			
			long sleep = site.computeSleepTime(timeUnit, maxRequestsPerTimeUnit);
			boolean first = true;
			while(sleep > 0){
				sleep += 10;
				if(logger.isInfoEnabled()){
					if(first){
						logger.info(getDataSite().getName() + ": enforce MTBR=" + maxRequestsPerTimeUnit + " TU=" + timeUnit);
						first = false;
					}	
					logger.info(getDataSite().getName() + ": sleep " + sleep);
				}
				try{
					TimeUnit.MILLISECONDS.sleep(sleep);
				}catch(InterruptedException e){
					Thread.currentThread().interrupt();
					return;
				}
				sleep = site.computeSleepTime(timeUnit, maxRequestsPerTimeUnit);
			}			
			if(!first && logger.isInfoEnabled()){ 
				logger.info(getDataSite().getName() + ": enforce MTBR Done.");
			}			
		}
		
		// update lastQueryTime for this HttpSiteManager
		lastRequestTime = System.currentTimeMillis();
		
		// update session/HttpSite lastQueryTime
		site.setLastQueryTime(lastRequestTime);
		
		// add to list of queries
		if(maxRequestsPerTimeUnit > 0 && timeUnit > 0){
			site.addLastQueryTime(lastRequestTime);
		}
		if(logger.isInfoEnabled()){ 
			logger.info(getDataSite().getName() + ": ---------------------");
		}
	}
	
	/**
	 * 
	 * @return
	 */
	public synchronized String getReport(){
		StringBuilder report = new StringBuilder();
		report.append("<br/><hr/>\n<b>" + getDataSite().getName() + "</b>: ");
		switch(maxSessions){
			case -1: report.append("NO SESSIONS<br/>\n"); break;
			case 0:  report.append("INFINITE SESSIONS<br/>\n");  break;
			default: report.append("N="+maxSessions + "SESSIONS<br/>\n"); break;
		}
		report.append("In use search ids count= " + inUseSites.size() + ":" + inUseSites + "<br/>\n");
		report.append("Inactive sites count=" + inactiveSites.size() + " size=" + "?" + " bytes: <br/>\n");
		for(HttpSite3 site: inactiveSites){
			report.append("<li>" + site + "</li>");
		}
		report.append("Active sites count=" + activeSites.size() + " size=" + "?" + " bytes : <br/>\n");
		for(Map.Entry<Long, HttpSite3> entry: activeSites.entrySet()){
			report.append("<li>" + entry.getKey() + ":" + entry.getValue() + "</li>\n");
		}
		report.append("<hr/>\n");
		return report.toString();
	}


	public DataSite getDataSite() {
		return dataSite;
	}
}
