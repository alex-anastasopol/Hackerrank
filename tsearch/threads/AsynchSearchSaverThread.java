package ro.cst.tsearch.threads;

import java.text.NumberFormat;
import java.util.Hashtable;
import java.util.ResourceBundle;
import java.util.Vector;

import org.apache.log4j.Logger;

import ro.cst.tsearch.Search;
import ro.cst.tsearch.database.DBManager;
import ro.cst.tsearch.utils.SearchLogger;
import ro.cst.tsearch.utils.URLMaping;
import ro.cst.tsearch.utils.ZipUtils;

//thread saves search contexts
public class AsynchSearchSaverThread extends Thread{

	public static final int MAX_TIME_IN_QUEUE = 0;
	public static final int MAX_TIME_IN_ZIP = 1;
	public static final int MAX_TIME_IN_DB = 2;
	
	private static ResourceBundle rbc = ResourceBundle.getBundle(URLMaping.SERVER_CONFIG);
	public static final Logger logger = Logger.getLogger(AsynchSearchSaverThread.class);
	
	private Vector<Search> savingQueue = null;
	private Object jobNotifier = new Object();
	
	//statistics
	private Hashtable<Search, Long> timeAdded = null;
	
	//maximum time spent by a search in saving queue(sec)
	private double maxTimeInQueue = 0.0;
	
	//maximum time spent by thread zipping a context(sec)
	private double maxTimeZipping = 0.0;
	
	//maximum time spent by thread insering search to database(sec)
	private double maxTimeInsertDB = 0.0;
	
	//flag that enables / disables search context saving to database
	private boolean searchSavingToDatabaseEnabled = true;
	
	private static class AsynchSearchSaverThreadHolder {
		private static AsynchSearchSaverThread searchSaverThreadInstance = new AsynchSearchSaverThread();
	}
   
	public static AsynchSearchSaverThread getInstance() {
		return AsynchSearchSaverThreadHolder.searchSaverThreadInstance;
	}   
	
	private AsynchSearchSaverThread(){
		savingQueue = new Vector<Search>();
		
		timeAdded = new Hashtable<Search, Long>();
		
		try{
			searchSavingToDatabaseEnabled = Boolean.parseBoolean( rbc.getString( "database.search.saving" ).trim() );
		}
		catch( Exception e ){
			e.printStackTrace();
		}
		
		start();
	}
	
	public void run(){
		
		//loop
		while( true ){
			try{
				//wait for a job in queue, or 500 ms
				synchronized( jobNotifier ){
					try {
						jobNotifier.wait( 500 );
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
				}
				
				Search contextToSave = null;
				
				do{
					//pop context from queue and save them
					contextToSave = popContextFromQueue();
					
					if( contextToSave != null ){
						updateSecondsSpent( contextToSave );
						
						zipAndSaveSearchToDB( contextToSave );
					}
				}
				while( contextToSave != null );
			}
			catch( Throwable e ){
				e.printStackTrace();
			}
		}
		
	}
	
	/**
	 * Adds a search in the thread queue to be saved to database
	 * 
	 * @param context search context to be zipped and added to database
	 * if search already in queue will not be added again 
	 */
	public void saveSearchContext( Search context ){
		
		//if saving disabled, return without adding the search to the queue
		if( !searchSavingToDatabaseEnabled ){
			return;
		}
		
		synchronized( savingQueue ){
			//if not already in queue
			if( !savingQueue.contains( context ) ){
				
				//add context to queue
				savingQueue.add( context );
				
				//put time of adding to queue
				timeAdded.put( context , new Long( System.currentTimeMillis() ));
				
				synchronized (jobNotifier) {
					//notify the running thread
					jobNotifier.notifyAll();	
				}
			}
		}
	}
	
	/**
	 * 
	 * @return the first search in queue (if any)
	 * null otherwise
	 */
	private Search popContextFromQueue(){
		
		Search firstSearchInQueue = null;
		
		synchronized( savingQueue ){
			try{
				
				//try to get the first search
				firstSearchInQueue = savingQueue.firstElement();
				
				//remove search from queue
				if( firstSearchInQueue != null ){
					savingQueue.remove( firstSearchInQueue );
				}
			}
			catch( Exception e ){
				//no searches in queue, return null
			}
		}
		
		return firstSearchInQueue;
	}
	
	/**
	 * Function performs the zipping and saving to database for this search
	 * 
	 * @param context context will be zipped and added to database
	 */
	private void zipAndSaveSearchToDB( Search context ){
		
		//synch the operation on context
		synchronized( context ){
			
			long zipProcessStart = System.currentTimeMillis();
		    
			//zip search context and add it to database
			byte[] searchContext = ZipUtils.zipContext( context.getSearchDir() , context, true );
		    
		    long zipProcessEnd = System.currentTimeMillis();
		    
		    //test if max file size is exceeded
		    
		    if( searchContext.length > Search.MAX_CONTEXT_FILE_SIZE ){
		    	//set flag to display warning message
		    	context.setMaxContextEvent( true );
		    	
		    	//send email notification
		    	Search.sendWarningMailMessage( context, " - Search exceeded maximum size ordered by agent " );
		    	
		    	SearchLogger.info("Saving search failed - Search exceeded maximum size", context.getID());
		    	
		    }
		    else{
		    	try{
			    	//save search
			    	DBManager.saveCurrentSearchData( context, searchContext );
		    	}
		    	catch(Exception e){
		    		e.printStackTrace();
		    	}
		    }
			
			long dbAddProcessEnd = System.currentTimeMillis();
			
			updateZipAndDbTimeSpent(zipProcessStart, zipProcessEnd, dbAddProcessEnd);
		}
		
	}
	
	/**
	 * Function computes the maximum time spent in queue by a search
	 * 
	 * @param context search context for which we are computing time spent in queue
	 */
	private void updateSecondsSpent( Search context ){
		Long addedMillis = timeAdded.get( context );
		double secondsSpent = (System.currentTimeMillis() - addedMillis.longValue()) / 1000.0;
		
		if( secondsSpent > maxTimeInQueue ){
			maxTimeInQueue = secondsSpent;
		}
		
		//remove search from hash
		timeAdded.remove( context );
	}
	
	/**
	 * 
	 * @param zipStart timestamp for the start zipping process
	 * @param zipEnd timestamp for the end of the zipping process and the start of the database insert process
	 * @param dbEnd timestamp for the end of the database insert process
	 */
	private void updateZipAndDbTimeSpent( long zipStart, long zipEnd, long dbEnd ){
		
		double zipTime = ( zipEnd - zipStart ) / 1000.0;
		if( maxTimeZipping < zipTime ){
			maxTimeZipping = zipTime;
		}
		
		double dbTime = ( dbEnd - zipEnd ) / 1000.0;
		if( maxTimeInsertDB < dbTime ){
			maxTimeInsertDB = dbTime;
		}
		
	}
	
	/**
	 * 
	 * @param requestedStatistic the statistic that is requested
	 * @return double value representing time in seconds
	 */
	public String getStatistic( int requestedStatistic ){
		
		double returnValue = 0.0;
		
		switch( requestedStatistic ){
			case MAX_TIME_IN_QUEUE:
				returnValue = maxTimeInQueue;
				break;
			case MAX_TIME_IN_ZIP:
				returnValue = maxTimeZipping;
				break;
			case MAX_TIME_IN_DB:
				returnValue = maxTimeInsertDB;
				break;
			default:
				returnValue = 0.0;
		}
		
		NumberFormat numberFormat = NumberFormat.getInstance();
		numberFormat.setMaximumFractionDigits( 2 );
		
		return numberFormat.format( returnValue );
		
	}
}