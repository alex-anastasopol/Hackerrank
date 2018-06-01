package ro.cst.tsearch.threads;

import java.io.File;
import java.io.IOException;
import java.util.Calendar;
import java.util.Iterator;
import java.util.ResourceBundle;
import java.util.TimeZone;
import java.util.Timer;
import java.util.TimerTask;
import java.util.Vector;
import java.util.concurrent.TimeUnit;

import org.apache.log4j.Category;
import org.springframework.jdbc.core.simple.SimpleJdbcTemplate;

import ro.cst.tsearch.SearchManager;
import ro.cst.tsearch.ServerConfig;
import ro.cst.tsearch.bean.dsma.DSMAConstants;
import ro.cst.tsearch.database.DBLoadInfo;
import ro.cst.tsearch.database.DBManager;
import ro.cst.tsearch.servlet.BaseServlet;
import ro.cst.tsearch.titledocument.TSDManager;
import ro.cst.tsearch.utils.DBConstants;
import ro.cst.tsearch.utils.FileUtils;
import ro.cst.tsearch.utils.URLMaping;

public class DeleteLocalContext extends TimerTask {

	private static final Category logger = Category.getInstance(DeleteLocalContext.class.getName());
	private static ResourceBundle rbc = ResourceBundle.getBundle(URLMaping.SERVER_CONFIG);
	
	private int period;
	
	public DeleteLocalContext(int period) {
		this.period = period;
	}
	
	public DeleteLocalContext(){
		
	}

	public static void init() {
        
        // if the delete.context.start is defined and has "false" propery, then do not run the delete context task
        boolean start = true;
        try{	
        	start = Boolean.parseBoolean( rbc.getString( "delete.local.context.start" ).trim() );
        }catch(Exception e){        	
        }
        if(!start){
        	System.err.println("delete.local.context.start=false");
        	return;
        }
        
        
        int period = 2; // in days, how many previous days should be scanned for undeleted search context
        long interval = 24;	///12 hours
        int startHour = 3;	//3 AM
        
        
        try {
        	period = Integer.parseInt(rbc.getString("delete.local.context.period"));
        } catch (Exception e){
        	System.err.println("ERROR: Using default values");
        	e.printStackTrace();
        }
        
        try {
        	interval = Long.parseLong(rbc.getString("delete.local.context.interval"));
        } catch (Exception e){
        	System.err.println("ERROR: Using default values");
        	e.printStackTrace();
        }
        interval *= 3600000;	//3600*1000 : this is hours in miliseconds
        try {
        	startHour = Integer.parseInt(rbc.getString("delete.local.context.startHour"));
        } catch (Exception e){
        	System.err.println("ERROR: Using default values");
        	e.printStackTrace();
        }
        DeleteLocalContext deleteContext = new DeleteLocalContext(period);
        DeleteLocalContext deleteContextNow = new DeleteLocalContext(period);
        Calendar cal = Calendar.getInstance(TimeZone.getDefault());
        //cal.add(Calendar.DAY_OF_MONTH, -1);
        cal.set(Calendar.HOUR_OF_DAY, startHour);
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.SECOND, 0);
        
        
        Timer timer = new Timer("DeleteLocalContext", true);
   	 	timer.schedule( (TimerTask) deleteContext, cal.getTime(), interval );	//run task frequently
   	 	
   	 	
   	 	Timer timerNow = new Timer("DeleteLocalContextNow", true);
   	 	timerNow.schedule((TimerTask) deleteContextNow, 60*1000);	//run task in 1 minute

   	 	deleteContext.deleteTemporaryFolder();
   	 	
	}
	
	
	@Override
	public void run() {
		// TODO Auto-generated method stub
		logger.info("Starting DELETE_LOCAL_CONTEXT...");
		deleteOldSearches(BaseServlet.FILES_PATH);
		deleteOldLoadInformation();
		deleteOldDiskInformation();
		deleteOldSearchStatistics();
	}

	private void deleteOldLoadInformation() {
		
		SimpleJdbcTemplate sjt = DBManager.getSimpleTemplate();
		
		try {
			int nr = sjt.update("DELETE FROM " + DBConstants.TABLE_USAGE_INFO + " WHERE " + 
				DBConstants.FIELD_USAGE_INFO_TIMESTAMP + " < now() - INTERVAL ? Hour", DSMAConstants.INTERVAL_HOUR);
			logger.debug("deleteOldLoadInformation: Deleted " + nr + " entries from " + DBConstants.TABLE_USAGE_INFO);
		} catch (Exception e) {
	        DBLoadInfo.getLogger().error("deleteOldLoadInformation: Error cleaning table " + DBConstants.TABLE_USAGE_INFO, e);
	    }
		
		String sqlDeleteLoadInfo = "DELETE FROM " + 
				DBConstants.TABLE_LOAD_INFO + " WHERE " + 
				DBConstants.FIELD_LOAD_INFO_TYPE + " = ? and " + 
				DBConstants.FIELD_LOAD_INFO_TIMESTAMP + " < now() - INTERVAL ? DAY";
		Object[][] toDelete = new Object[][]
				{
				new Object[]{DBConstants.LOAD_USAGE_DAY, DSMAConstants.INTERVAL_DAY / 24},
				new Object[]{DBConstants.LOAD_USAGE_WEEK, DSMAConstants.INTERVAL_WEEK / 24},
				new Object[]{DBConstants.LOAD_USAGE_MONTH, DSMAConstants.INTERVAL_MONTH / 24},
				new Object[]{DBConstants.LOAD_USAGE_YEAR, DSMAConstants.INTERVAL_YEAR / 24}};
		for (Object[] toDeleteEntry : toDelete) {
			try {
				int nr = sjt.update(sqlDeleteLoadInfo, toDeleteEntry[0], toDeleteEntry[1]);
				logger.debug("deleteOldLoadInformation: Deleted " + nr + " entries from " + DBConstants.TABLE_LOAD_INFO 
						+ " for type " + toDeleteEntry[0] + " older than " + toDeleteEntry[1] + " days");
				TimeUnit.MINUTES.sleep(2);
			} catch (Exception e) {
		        DBLoadInfo.getLogger().error("deleteOldLoadInformation: Error cleaning table " + DBConstants.TABLE_LOAD_INFO 
		        		+ " for type " + toDeleteEntry[0] + " older than " + toDeleteEntry[1] + " days", e);
		    }
		}
	}
	
	private void deleteTemporaryFolder() {
		File folder = new File(ServerConfig.getTsrCreationTempFolder());
		if(folder.isDirectory()) {
			try {
				org.apache.commons.io.FileUtils.cleanDirectory(folder);
			} catch (IOException e) {
				logger.error("Error while cleaning folder " + folder.getName(), e);
			}
		}
	}
	
	private void deleteOldDiskInformation() {
		String sql = "DELETE FROM " + 
			DBConstants.TABLE_USAGE_DISK + " WHERE " + 
			DBConstants.FIELD_USAGE_DISK_TYPE + " = ? and " + 
			DBConstants.FIELD_USAGE_DISK_TIMESTAMP + " < now() - INTERVAL ? DAY";
		
		Object[][] toDelete = new Object[][]
				{
				new Object[]{1, 2},
				new Object[]{2, 31},
				new Object[]{3, 365}};
		
		SimpleJdbcTemplate sjt = DBManager.getSimpleTemplate();
		
		for (Object[] toDeleteEntry : toDelete) {
			try {
				int nr = sjt.update(sql, toDeleteEntry[0], toDeleteEntry[1]);
				logger.debug("deleteOldDiskInformation: Deleted " + nr + " entries from " + DBConstants.TABLE_USAGE_DISK 
						+ " for type " + toDeleteEntry[0] + " older than " + toDeleteEntry[1] + " days");
				TimeUnit.MINUTES.sleep(2);
			} catch (Exception e) {
		        DBLoadInfo.getLogger().error("deleteOldDiskInformation: Error cleaning table " + DBConstants.TABLE_USAGE_DISK 
		        		+ " for type " + toDeleteEntry[0] + " older than " + toDeleteEntry[1] + " days", e);
		    }
		}
		
	}
	
	private void deleteOldSearchStatistics() {
		String sql = "DELETE FROM " + 
			DBConstants.TABLE_SEARCH_STATISTICS + " WHERE " + DBConstants.FIELD_SEARCH_STATISTICS_TYPE +  " = ? AND " + 
			DBConstants.FIELD_SEARCH_STATISTICS_TIMESTAMP +  " < now() - INTERVAL ? DAY";
		
		Object[][] toDelete = new Object[][]
				{
				new Object[]{DBConstants.LOAD_USAGE_WEEK, DSMAConstants.INTERVAL_WEEK / 24},
				new Object[]{DBConstants.LOAD_USAGE_MONTH, DSMAConstants.INTERVAL_MONTH / 24},
				new Object[]{DBConstants.LOAD_USAGE_YEAR, DSMAConstants.INTERVAL_YEAR / 24}};
		
		SimpleJdbcTemplate sjt = DBManager.getSimpleTemplate();
		
		for (Object[] toDeleteEntry : toDelete) {
			try {
				int nr = sjt.update(sql, toDeleteEntry[0], toDeleteEntry[1]);
				logger.debug("deleteOldSearchStatistics: Deleted " + nr + " entries from " + DBConstants.TABLE_SEARCH_STATISTICS 
						+ " for type " + toDeleteEntry[0] + " older than " + toDeleteEntry[1] + " days");
				TimeUnit.MINUTES.sleep(2);
			} catch (Exception e) {
		        DBLoadInfo.getLogger().error("deleteOldSearchStatistics: Error cleaning table " + DBConstants.TABLE_SEARCH_STATISTICS 
		        		+ " for type " + toDeleteEntry[0] + " older than " + toDeleteEntry[1] + " days", e);
		    }
		}
		
	}

	public void deleteOldSearches(String sSitePath) {
	    
	    Calendar calendar = Calendar.getInstance(TimeZone.getTimeZone("GMT"));
	    
	    for (int d = 0; d < period; d++) {
			calendar.add(Calendar.DATE, -1);
			
			String sYesterday = String.valueOf(calendar.get(Calendar.YEAR));
			sYesterday  += "_" + String.valueOf(calendar.get(Calendar.MONTH) + 1);
			sYesterday  += "_" + String.valueOf(calendar.get(Calendar.DAY_OF_MONTH));
					
			String filesFolderPath = sSitePath + TSDManager.TSDDir + File.separator + sYesterday + File.separator;
			File filesFolder = new File(filesFolderPath);
			logger.debug("Deleting from dir: " + filesFolderPath);
			String[] fileList = filesFolder.list();
			Vector<Long> searches = new Vector<Long>();
			if (fileList != null && fileList.length > 1) { 
			    
				for (int i = 0; i < fileList.length; i++) {
					try {
						if( !fileList[i].endsWith(".zip") ){
						    long searchId = Long.parseLong(fileList[i]);
						    searches.add(searchId);
						}
					    
					} catch (Exception e) {
						e.printStackTrace();
					    FileUtils.deleteDir(new File(filesFolderPath + fileList[i]));
					    logger.debug("Folder [" + filesFolderPath + fileList[i] + "] was deleted.");
					}
				}
				
				Vector<Long> exists = DBManager.getSearchesWithContext(searches);
				for (Iterator<Long> iter = searches.iterator(); iter.hasNext();) {
					Long srch = (Long) iter.next();
					if(!exists.contains(srch)){
						//it is not in the database
						//must check if it is in memory
						try {
							if(SearchManager.getSearch(srch, false) == null){
								//it is not in memory
								FileUtils.deleteDir(new File(filesFolderPath + srch));
						        File zipFile = new File( filesFolderPath + srch + ".zip" );
						        if( zipFile.exists() ){
						        	zipFile.delete();
						        }
						        logger.debug("Folder [" + filesFolderPath + srch + "] was deleted.");
							} else {
								logger.debug("The context [" + filesFolderPath + srch + "] will not be deleted .");
							}
						} catch (Exception e) {
							e.printStackTrace();
						    FileUtils.deleteDir(new File(filesFolderPath + srch));
						    logger.debug("Folder [" + filesFolderPath + srch + "] was deleted.");
						} 
					} else {
				        logger.debug("The context [" + filesFolderPath + srch + "] will not be deleted .");    
				    }
				}
				
			}
		}
	}
}
