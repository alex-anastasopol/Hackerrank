package ro.cst.tsearch.threads;

import ro.cst.tsearch.Search;
import ro.cst.tsearch.SearchManager;

/**
 * This class helps stop a thread.
 * It's used to continue with the application flow right away (and not wait for the thread to stop) 
 * @author andrei
 *
 */
public class ASStopperThread extends Thread {
	private Search search = null;
	
	public ASStopperThread(Search search) {
		this.setName("ASStopperThread_" + search.getID());
		this.search = search;
	}
	@Override
	public void run() {
        ASMaster.stopSearch(search);
        ASMaster.removeSearch(search);
        
        //removed synchronization since it's done in the function 
		SearchManager.removeSearch(search.getID(), true);
		
	}
}
