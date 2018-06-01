package ro.cst.tsearch.utils;

import java.util.Hashtable;
import java.util.Iterator;
import java.util.ResourceBundle;

public class SearchReserved{
	
	private static ResourceBundle rbc = ResourceBundle.getBundle(URLMaping.SERVER_CONFIG);
	
	private Hashtable<Long, Long> reservedSearchesList = null;
	
	private long TMAX_REPLICATION = 10000; // 10 second default
	
	private SearchReserved(){
		reservedSearchesList = new Hashtable<Long, Long>();
		
		//get servers from config file
		try{
			TMAX_REPLICATION = Long.parseLong( rbc.getString( "distributedmutex.tmax.replication" ) );
		}
		catch( Exception e ){
			e.printStackTrace();
		}
		
	}
	
	private static class ReservedSearchesHolder {
		private static SearchReserved instance = new SearchReserved();
	} 
		 
	public static SearchReserved getInstance() {
		return ReservedSearchesHolder.instance;
	}
	
	public boolean isReserved( long searchId ){
		//cleanup invalid requests
		cleanTimedOutReservedRequests();
		
		
		return reservedSearchesList.get( new Long( searchId ) ) != null;
	}
	
	public void reserveSearch( long searchId ){
		reservedSearchesList.put( new Long( searchId ) , new Long( System.currentTimeMillis() ));
	}
	
	public void clearReserved( long searchId ){
		reservedSearchesList.remove( new Long( searchId ) );
	}
	
	public void cleanTimedOutReservedRequests(){
		
		Iterator<Long> searchIdsIterator = reservedSearchesList.keySet().iterator();
		
		long now = System.currentTimeMillis() ;
		
		while( searchIdsIterator.hasNext() ){
			Long searchId = searchIdsIterator.next();
			long timeStamp = reservedSearchesList.get( searchId ).longValue();
			
			//if timed out remove from list
			if( now >= timeStamp + TMAX_REPLICATION + TMAX_REPLICATION/2 ){
				searchIdsIterator.remove();
			}
			
		}
			
	}
}