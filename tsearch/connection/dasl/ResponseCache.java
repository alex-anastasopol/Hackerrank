package ro.cst.tsearch.connection.dasl;

import java.io.File;
import java.io.FilenameFilter;
import java.util.HashMap;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.log4j.Logger;

import ro.cst.tsearch.ServerConfig;
import ro.cst.tsearch.utils.FileUtils;
import ro.cst.tsearch.utils.StringUtils;
import ro.cst.tsearch.utils.URLMaping;

/**
 * cache class to be used during testing
 * @author radu bacrau
 */
public class ResponseCache {
	
	protected static Logger logger = Logger.getLogger(ResponseCache.class);
	
	private Map<String,String> queryMap = new ConcurrentHashMap<String,String>();
	private String folderName; 
		
	/**
	 * Return the response corresponding to a certain query
	 * @param query
	 * @return
	 */
	public String getResponse(String query){
		String fileName = queryMap.get(query);
		if(fileName == null){
			return null;
		}
		if(!(new File(fileName).exists())){
			return null;
		}
		return FileUtils.readXMLFile(fileName);
	}


	/**
	 * Put a query-response-id touple in the cache
	 * @param query
	 * @param response
	 * @param id
	 */
	public void putResponse(String query, String response, long id){
		if(id == -1 || id == 0){
			id = System.currentTimeMillis();
		}
		String inputFileName = folderName + File.separator + id + ".query.xml";
		String outputFileName = folderName + File.separator + id + ".response.xml";
		FileUtils.writeTextFile(inputFileName, query);
		FileUtils.writeTextFile(outputFileName, response);
		queryMap.put(query, outputFileName);
	}
	
	/**
	 * Constructor: load the queries-responses saved to hard drive
	 * @param folderName
	 */
	private ResponseCache(String folderName) {
		
		this.folderName = folderName;
		
		// check the cache folder exists
		File folder = new File(folderName);
		folder.mkdirs();
		if(!folder.exists()){
			throw new RuntimeException("Folder: " + folderName + " does NOT exist!");
		}
		
		// enumerate all saved queries
		String [] reqFileNames = folder.list(new FilenameFilter(){
			public boolean accept (File f, String fileName){
				return fileName.endsWith(".query.xml");
			}
		});
		
		// load all saved queries to cache
		for(String reqFileName : reqFileNames){			
			
			// find id of the oder
			String idString = reqFileName.substring(0, reqFileName.indexOf(".query.xml"));
			if(!idString.matches("\\d+")){
				continue;
			}			
			long id = Long.valueOf(idString);
			
			reqFileName = folderName + File.separator + reqFileName;
			
			// check that response exists
			String resFileName = folderName + File.separator + id + ".response.xml";
			if(!(new File(resFileName).exists())){
				continue;
			}
			
			// load request, response into cache
			String req = FileUtils.readXMLFile(reqFileName);
			queryMap.put(req, resFileName);

		}
		
	}
	
	private static Map<String,ResponseCache> caches = new HashMap<String,ResponseCache>();
	private static Map<String,String> disabledCaches = new ConcurrentHashMap<String,String>();
	
	/**
	 * Set-up a cache using the serverName.cache.enabled key in configuration file
	 * The cache foloder is files.path/cache/serverName
	 * @param serverName
	 * @return
	 */
	public static ResponseCache setupResponseCache(String serverName){ 
		
		if(disabledCaches.containsKey(serverName)){
			return null;
		}
		
		try{
			
			ResourceBundle rb = ResourceBundle.getBundle(URLMaping.SERVER_CONFIG);			
			boolean cacheEnabled = Boolean.valueOf(rb.getString(serverName + ".cache.enabled"));
			if(!cacheEnabled){
				disabledCaches.put(serverName, "");
				return null;
			}
			
			String	cachePath = ServerConfig.getFilePath() + File.separator + "cache" + File.separator + serverName;				
			if(StringUtils.isEmpty(cachePath)){
				disabledCaches.put(serverName, "");
				return null;
			}
			
			synchronized(caches){
				ResponseCache cache = caches.get(serverName);
				if(cache == null){
					cache = new ResponseCache(cachePath);
					caches.put(serverName, cache);
				}
				return cache;
			}
				
		}catch(RuntimeException e){
			disabledCaches.put(serverName, "");
			return null;
		}
	}
}
