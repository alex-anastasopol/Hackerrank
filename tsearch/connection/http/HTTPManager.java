package ro.cst.tsearch.connection.http;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.MultiThreadedHttpConnectionManager;
import org.apache.commons.httpclient.params.HttpConnectionManagerParams;
import org.apache.log4j.Category;
import org.apache.log4j.Logger;

import ro.cst.tsearch.servers.HashCountyToIndex;
import ro.cst.tsearch.servers.bean.DataSite;
import ro.cst.tsearch.utils.StringUtils;


public class HTTPManager 
{
	protected static final Category logger = Logger.getLogger(HTTPSite.class);
	
	public static final int MAX_REDIRECTS = 7;
	
	public static HTTPManager inst = new HTTPManager();
	
	public static synchronized  void log(Object msg)
	{		
		logger.debug(msg);
	}
	
	public HashMap<String, HTTPSiteManager> siteManagers = 
		new HashMap<String,HTTPSiteManager>();
	
	private void init()
	{
		
		System.setProperty("apache.commons.httpclient.cookiespec", "COMPATIBILITY");
		
		System.setProperty( "javax.xml.parsers.DocumentBuilderFactory", 
    							"org.apache.xerces.jaxp.DocumentBuilderFactoryImpl");
	
		loadAllSites();
	}

	public void reloadAllSites() {
		siteManagers.clear();
		loadAllSites();
	}
	
	public void loadAllSites() {
		Map<String, DataSite> allSites = HashCountyToIndex.getAllSitesExternal();
		
		for (String siteId : allSites.keySet()) {
			DataSite dat = allSites.get(siteId);
			String connClassFileName = dat.getClassConnFilename();
			
			if(StringUtils.isEmpty(connClassFileName) || "null".equals(connClassFileName)){
				continue;
			}
			if(dat.getConnType() >= DataSite.HTTP_CONNECTION_2){
				continue;
			}
			if(dat.getClassConnFilename().contains("DASLConnSite")){
				continue;
			}
			if(dat.getName().endsWith("AK")){
				continue;
			}
			
			String name = dat.getName();
			HTTPSiteManager siteManagerInstance;
			/*if(name.endsWith("PA")){				
				siteManagerInstance = communitySiteManager.get("XXStewartPA");
				if(siteManagerInstance == null){
					siteManagerInstance = new HTTPSiteManager("XXStewartPA", connClassFileName, dat.getTimeBetweenRequests(),
					dat.getMaxSessions(),dat.getConnectionTimeout(),dat.getMaxRequestsPerSecond(),dat.getUnits(),null,3128);
					log( siteManagerInstance );
					communitySiteManager.put("XXStewartPA", siteManagerInstance );
					siteManagerInstance.setDaemon(true);				
				}
				communitySiteManager.put(name, siteManagerInstance );				
			} else {*/
				siteManagerInstance = new HTTPSiteManager(name,connClassFileName,dat.getTimeBetweenRequests(),
						dat.getMaxSessions(),dat.getConnectionTimeout(),dat.getMaxRequestsPerSecond(),dat.getUnits(),null,3128);
				//log( siteManagerInstance ); 
				siteManagers.put(name, siteManagerInstance );
				siteManagerInstance.setDaemon(true);		
			//}	
		}
	}

	public HTTPManager()
	{		
		init();
		
	}
	
	protected HashMap<Object, HTTPSite> lockedSites = new HashMap<Object, HTTPSite>();
	
	protected synchronized void _releaseSite(Object userData)
	{
        Iterator<Object> allUserData = lockedSites.keySet().iterator();
        
        while( allUserData.hasNext() )
        {
            Object userDataKey = allUserData.next();
            
            if( userDataKey.toString().startsWith( userData.toString() ) )
            {
                HTTPSite site = (HTTPSite) lockedSites.get(userDataKey);
                
                if ( site != null ){
                    site.unlock();
                    // remove it from memory too, so that it can be garbage-collected
                    allUserData.remove();
                }
            }
        }
        
	}
	
	protected synchronized HTTPSite _getSite(String name, Object userData,long searchId,int miServerId) throws HTTPManagerException
	{
		HTTPSiteManager sm = getHTTPSiteManager(name);
						
		if ( sm == null )
			throw new HTTPManagerException("Site named <" + name + "> not found.");
		
		HTTPSite s = null;
		
		if (userData != null) {
			s = (HTTPSite) lockedSites.get(userData);
		}
		
		if (s == null) 
			s = sm.getSite(searchId,miServerId);
		
		if ( userData != null )
		{
			s.setUserData(userData);
			s.lock();
			lockedSites.put(userData, s);
		}
		
		return s;
	}
	
	public static HTTPSite getSite(String name,long searchId,int miServerId) throws HTTPManagerException
	{
		return inst._getSite(name, null,searchId,miServerId);
	}	
	
	public static HTTPSite getSite(String name, Object userData,long searchId,int miServerId) throws HTTPManagerException
	{
		return inst._getSite(name, userData,searchId,miServerId);
		/*Site s = inst._getSite(name);
		s.setUserData(userData);
		s.lock();
		return s;*/
	}	
	
	public static void releaseSite(Object userData)
	{
		inst._releaseSite(userData);
	}
	
	public static HttpClient createHttpClient()
	{
		
		MultiThreadedHttpConnectionManager  cm = new MultiThreadedHttpConnectionManager ();
		
		HTTPCleanerConections.addManager(cm);
		
		HttpConnectionManagerParams params = new HttpConnectionManagerParams();
		params.setDefaultMaxConnectionsPerHost(100);
		params.setMaxTotalConnections(100);
		
		cm.setParams(params);
		
		return new HttpClient(cm);
	}
	
	public HTTPSiteManager getHTTPSiteManager(String siteName) {
		return siteManagers.get(siteName);
	}
	
}




