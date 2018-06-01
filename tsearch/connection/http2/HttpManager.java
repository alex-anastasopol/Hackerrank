package ro.cst.tsearch.connection.http2;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import ro.cst.tsearch.Search;
import ro.cst.tsearch.connection.http.HTTPSiteManagerInterface;
import ro.cst.tsearch.servers.HashCountyToIndex;
import ro.cst.tsearch.servers.bean.DataSite;

/**
 * This class offers methods for getting a site and releasing a site
 * @author dumitru bacrau
 */
public class HttpManager {
	
	
	/**
	 * Prevent instantiation
	 */
	private HttpManager() {}
	
	/**
	 * Map with the site manager for each site
	 */
	private static final Map<String,HttpSiteManager> managers = new HashMap<String,HttpSiteManager>();
	
	/**
	 * Set of all supported sites. We could have used managers.keySet(), but this solution is potentially faster
	 */
	private static final Set<String> supportedSites = new HashSet<String>();
	
	/**
	 * Load map with all site managers
	 */
	
//	static {
//		loadAllSites();
//	}


	public static void reloadAllSites() {
		managers.clear();
		supportedSites.clear();
		loadAllSites();
	}
	

	public static void loadAllSites() {
		// iterate through all sites
		Map<String, DataSite> allSites = HashCountyToIndex.getAllSitesExternal();
		
		for (String siteId : allSites.keySet()) {
			load(Long.parseLong(siteId));
		}
	}
	
	/**
	 * Check whether a certain site is supported 
	 * @param name
	 * @return
	 */
	public static boolean isSiteSupported(String name){
		return supportedSites.contains(name);
	}
	
	/**
	 * Obtain site for given site name and searchId
	 * @param siteName
	 * @param searchId
	 * @return
	 */
	public static HttpSite getSite(String siteName, long searchId){
		return managers.get(siteName).getSite(searchId);
	}
	
	/**
	 * Release given site
	 * @param site
	 */
	public static void releaseSite(HttpSite site){
		site.getSiteManager().releaseSite(site);
	}

	/**
	 * Destroy all sites related to the given search
	 * @param searchId
	 */
	public static void destroySearch(Search search){
		for(HTTPSiteManagerInterface manager: search.getHttpSiteManagers()){
			manager.destroySearch(search.getID());
		}
	}
	
	/**
	 * Get the names of all supported sites
	 * @return
	 */
	public static Collection<String> getSupportedSiteNames(){
		return Collections.unmodifiableCollection(supportedSites);
	}
	
	/**
	 * Get the report for a certain site manager
	 * @param siteName
	 * @return
	 */
	public static String getReport(String siteName){
		
		if("none".equals(siteName)){
			return "";
		}
		
		if("all".equals(siteName)){
			StringBuilder result = new StringBuilder();
			for(String name: getSupportedSiteNames()){
				result.append(managers.get(name).getReport());
			}
			return result.toString();
		}
		
		if(!isSiteSupported(siteName)){
			return "Site: " + siteName + " NOT supported!";
		}		
		
		return managers.get(siteName).getReport();
	}
	
	
	public static void reload(long serverId) {
		load(serverId, true);
	}

	public static void load(long serverId) {
		load(serverId, false);
	}
	
	/**
	 * Load (or reload) informations for the search site given by id
	 * @param commId the community to load
	 * @param id
	 * @param reload
	 */
	public synchronized static void load(long id,boolean reload) {
		// get site parameters
		DataSite dat = HashCountyToIndex.getDateSiteForMIServerID( String.valueOf(id) );
		String connClassFileName = dat.getClassConnFilename();
		
		// skip sites that have not specified a connection class
		if(connClassFileName == null || "".equals(connClassFileName) || "null".equals(connClassFileName)){
			return;
		}
		
		// skip sites that do not use third connection 
		if(dat.getConnType() != DataSite.HTTP_CONNECTION_2){
			if(reload) {
				managers.remove(dat.getName());
				supportedSites.remove(dat.getName());
			}
			return;
		}

		HttpSiteManager siteManagerInstance = new HttpSiteManager(dat);
		
		// add it to map with all managers
		managers.put(dat.getName(), siteManagerInstance );	
		
		// add to the set of supported sites
		if(!supportedSites.contains(dat.getName())) {  
			supportedSites.add(dat.getName());
		}
	}
}
