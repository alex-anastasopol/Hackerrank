package ro.cst.tsearch.threads;

import ro.cst.tsearch.Search;
import ro.cst.tsearch.data.User;
import ro.cst.tsearch.database.DBManager;
import ro.cst.tsearch.database.transactions.SaveSearchTransaction;
import ro.cst.tsearch.exceptions.SaveSearchException;
import ro.cst.tsearch.user.UserAttributes;
import ro.cst.tsearch.utils.Log;
import ro.cst.tsearch.utils.SearchLogger;

public class SaveTsrRetryer implements Runnable{

	private int saveStatus = SaveSearchTransaction.STATUS_FAIL_OTHER_EXCEPTION;
	private int sleepInterval = 120;
	private int maxInterval = 1800;
	
	private User user;
	private Search search;
	private String notificationFile;
	private String contextPath;
	private String notificationSendTo;
	
	public SaveTsrRetryer(User user, Search search,
			String notificationFile, String contextPath, String notificationSendTo) {
		if(search == null) {
			throw new NullPointerException("search field cannot be null");
		}
		this.user = user;
		this.search = search;
		this.notificationFile = notificationFile;
		this.contextPath = contextPath;
		this.notificationSendTo = notificationSendTo;
		
	}

	@Override
	public void run() {
		int retries = 0;
		long startTime = System.currentTimeMillis();
		while(System.currentTimeMillis() - startTime < maxInterval * 1000) {
			try {
				saveStatus = DBManager.saveCurrentSearch(user, search, Search.SEARCH_TSR_CREATED, 
						notificationFile, contextPath, notificationSendTo);
			} catch (SaveSearchException e) {
				e.printStackTrace();
				GPThread.logger.error("Error while saving search " + search.getID(), e);
			}
			retries ++;
			if(saveStatus == SaveSearchTransaction.STATUS_SUCCES) {
				break;
			} else {
				GPThread.logger.info("Error while saving search " + search.getID() + ", status " + saveStatus + ", retries " + retries);
			}
			try {
				Thread.sleep(sleepInterval * 1000);
			} catch (InterruptedException e) {
				e.printStackTrace();
				break;
			}
		}
		if(saveStatus != SaveSearchTransaction.STATUS_SUCCES){
			Log.sendEmail2("SaveTsrRetryer Failed", 
	    			"Search was not correctly autosaved after TSR creation " +
	    			SearchLogger.getTimeStampFormat1(user.getUserAttributes().getNiceName(), "&#44; ") +
	    			"<br>SearchId: " + search.getID() + ", ca_id: " + search.getCommId() + ", retries: " + retries);
		}
	}

	public int getSaveStatus() {
		return saveStatus;
	}

	public void setSleepInterval(int seconds) {
		this.sleepInterval = seconds;
	}

	public void setMaxInterval(int seconds) {
		this.maxInterval = seconds;
	}

}
