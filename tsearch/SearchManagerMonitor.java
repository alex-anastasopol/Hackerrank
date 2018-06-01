package ro.cst.tsearch;

import java.util.Collection;
import java.util.Iterator;
import java.util.Vector;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;

import org.apache.log4j.Category;
import org.apache.log4j.Logger;

public class SearchManagerMonitor extends Thread {
    
	protected static final Category logger = Logger.getLogger(SearchManagerMonitor.class);
	
	private long logInterval 		= 5 * 60 * 1000; // 5 min
	private long disposeInterval 	= 10 * 60 * 1000; // 10 min
	
	private static SearchManagerMonitor monitor = null;
	
	public static void init() {
	    
	    if (monitor == null) {
	        
	        System.err.println("***********************************************\n");
		    System.err.println("********** START SEARCH MANAGER MONITOR *******\n");
		    
		    logger.info("********** START SEARCH MANAGER MONITOR *******");
		    
		    monitor = new SearchManagerMonitor();
			monitor.setDaemon(true);
			monitor.setName("SearchManager Monitor");
			monitor.start();    
	    }
	}
	
	public static SearchManagerMonitor getInstance(){
		return monitor;
	}
	
	public long getDisposeInterval(){
		return disposeInterval;
	}
	
	@SuppressWarnings("rawtypes")
	public void run() {

		while (true) {

			try {

			    System.err.println("********** Begin - Clean search data **********\n");
			    
			    logger.debug("********** Begin - Clean search data **********");
			    
			    long currentTime = System.currentTimeMillis();
			    
			    Lock lockAllSearches = SearchManager.getLockAllSearches();
			    //lockAllSearches.lock();
			    
			    if(lockAllSearches.tryLock() || lockAllSearches.tryLock(60, TimeUnit.SECONDS)) {
			    
			    	try {
	    			    Collection c = SearchManager.getSearches().values();
	    		        for (Iterator iterator = c.iterator(); iterator.hasNext();) {
	    		            Vector searches = (Vector) iterator.next();
	    		            for (int i = searches.size() - 1; i >= 0; i--) {
	    		                Search search = (Search) searches.get(i);
	    		                if (search.disposeTime > 0 &&
	    		                        currentTime - search.disposeTime > disposeInterval) {
	    		                	System.err.println("REMOVE SEARCH - SearchID=[" + search.getSearchID() + "], disposeTime=[" + search.disposeTime + "]");
	    		                	logger.info("REMOVE SEARCH - SearchID=[" + search.getSearchID() + "], disposeTime=[" + search.disposeTime + "]");
	    		                    searches.remove(i);
	    		                }
	    		            }
	    		            
	    		            if (searches.size() == 0) {
	    		            	iterator.remove();
	    		            }
	    		        }
	                } finally {
	                	lockAllSearches.unlock();
	                }
			    	System.err.println("********** End - Clean search data ************");
			    	logger.error("********** End - Clean search data ************");
			    } else {
			    	System.err.println("********** ERROR - Clean search data cannot be done because lock cannot be acquired ************");
			    	logger.error("********** ERROR - Clean search data cannot be done because lock cannot be acquired ************");
			    }

				Thread.sleep(logInterval);
				
			} catch (Exception ignored) {}
		}

	}
	
	 
}