package ro.cst.tsearch.search.util;

import java.util.TimerTask;

import org.apache.log4j.Category;
import org.apache.log4j.Logger;

import ro.cst.tsearch.Search;
import ro.cst.tsearch.database.DBManager;
import ro.cst.tsearch.database.transactions.InsertSearchFilesTransaction;
import ro.cst.tsearch.threads.ASMaster;
import ro.cst.tsearch.threads.ASThread;
import ro.cst.tsearch.threads.GPMaster;
import ro.cst.tsearch.threads.GPThread;
import ro.cst.tsearch.utils.FileLogger;
import ro.cst.tsearch.utils.InstanceManager;
import ro.cst.tsearch.utils.ReflectionUtils;
import ro.cst.tsearch.utils.SearchLogger;

public class SearchRemoverTask extends TimerTask {
	
	protected static final Category logger= Logger.getLogger(SearchRemoverTask.class);
	
	private Search search = null;
	
	public SearchRemoverTask(Search search){
		this.search = search;
	}
	

	@Override
	public void run() {
		if( search == null || !search.isRequestClean())
			this.cancel();
		ASThread thread = ASMaster.getSearch(search.getID());
		GPThread gpt = GPMaster.getThread(search.getID());
		if(search.isRequestClean() && !search.isOcrInProgess() &&
				!(thread != null && thread.isAlive()) && gpt == null ) {
		
			if(!search.isFakeSearch() && 
				(DBManager.getTSRGenerationStatus(search.getID())!= Search.SEARCH_TSR_CREATED) ) {//if we don't check for tsrcreated, will override the indexLog in DB; B4392
				try {
					
					String userLogin = search.getSa().getAbstractorObject().getLOGIN();
					
					String info = "\n<BR><B>Search was auto-saved (removed from memory) </B> on: " 
    						+ SearchLogger.getTimeStampFormat1(userLogin, "&#44; ") 
    						+ " </BR>\n";
					
					SearchLogger.info(info, search.getID());
					
					Search.saveSearch(search);
					DBManager.zipAndSaveSearchToDB(search);
			        InsertSearchFilesTransaction insertSearchFilesTransaction = 
			        	new InsertSearchFilesTransaction(search);
			        DBManager.getTransactionTemplate().execute(insertSearchFilesTransaction);
			        
				} catch (Exception e) {
					e.printStackTrace();
				}
			} 
			InstanceManager.getManager().removeCurrentInstance(search.getID());
            ReflectionUtils.nullifyReferenceFields(search);
            System.err.println();
            logger.debug("*****************  Really  removeSearch from currentInstance " + search.getSearchID());
            FileLogger.info( "*****************  Really  removeSearch from currentInstance " + search.getSearchID(), FileLogger.SEARCH_OWNER_LOG );  
            this.cancel();
		}

	}

}
