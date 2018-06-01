package ro.cst.tsearch.threads;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Vector;
import java.util.concurrent.LinkedBlockingQueue;

import org.apache.commons.io.FileUtils;
import org.apache.log4j.Logger;

import ro.cst.tsearch.MailConfig;
import ro.cst.tsearch.Search;
import ro.cst.tsearch.ServerConfig;
import ro.cst.tsearch.bean.SearchLogEntry;
import ro.cst.tsearch.database.DBManager;
import ro.cst.tsearch.utils.DBConstants;
import ro.cst.tsearch.utils.Log;
import ro.cst.tsearch.utils.SearchLogger;
import ro.cst.tsearch.utils.SharedDriveUtils;
import ro.cst.tsearch.utils.URLMaping;

import com.stewart.ats.archive.SearchLogArchiveStatement;

public class AsynchSearchLogSaverThread extends Thread {

	public static final String					FILE_NAME_ORDER			= "orderFile.html";
	public static final String					FILE_NAME_TSRI_LOG		= "tsrIndexFile.html";
	public static final String					FILE_PREFIX_SEARCH_LOG	= "logFile_";
	
	private static final String	DIV_CLOSE	= "</div>";

	private static final String	DIV_TIMESTAMP_ATS	= "<div style=\"display:none\" class=\"timestamp_ats\">";

	private static final Logger				logger		= Logger.getLogger(AsynchSearchLogSaverThread.class);

	private LinkedBlockingQueue<SearchLogEntry>	savingQueue	= null;

	private static class AsynchSearchLogSaverThreadHolder {
		private static AsynchSearchLogSaverThread	searchLogSaverThreadInstance	= new AsynchSearchLogSaverThread();
	}

	public static AsynchSearchLogSaverThread getInstance() {
		return AsynchSearchLogSaverThreadHolder.searchLogSaverThreadInstance;
	}

	private AsynchSearchLogSaverThread() {
		savingQueue = new LinkedBlockingQueue<SearchLogEntry>();
		start();
	}

	public void run() {
		// loop
		while (true) {
			try {
				Vector<SearchLogEntry> searchLogs = new Vector<SearchLogEntry>();

				if (savingQueue.isEmpty()) {
					searchLogs.add(savingQueue.take());
					// Log.sendEmail2("Logging to database failed", "SearchId: " + searchLog.getSearchId() + " failed to log message:\n" +
					// searchLog.getSearchId());
				} else {
					savingQueue.drainTo(searchLogs, ServerConfig.getLogInTableMaxLogElements());
				}

				actualSaveToSamba(searchLogs);

			} catch (Throwable e) {
				e.printStackTrace();
			}
		}
	}

	private void actualSaveToSamba(Vector<SearchLogEntry> searchLogs) {
		
		if(ServerConfig.isEnableLogInSamba()) {
		
			Map<Long, List<SearchLogEntry>> map = new LinkedHashMap<Long, List<SearchLogEntry>>();
			
			for (SearchLogEntry searchLog : searchLogs) {
				long searchId = searchLog.getSearchId();
				List<SearchLogEntry> currentList = map.get(searchId);
				if(currentList == null) {
					currentList = new ArrayList<SearchLogEntry>();
					map.put(searchId, currentList);
				}
				currentList.add(searchLog);
			}
			
			for (List<SearchLogEntry> searchLogsBySearchId : map.values()) {
				writeToSamba(searchLogsBySearchId);
			}
		
		}
	}

	private void writeToSamba(List<SearchLogEntry> searchLogsBySearchId) {
		StringBuilder sb = new StringBuilder();
		long searchId = 0;
		for (SearchLogEntry searchLog : searchLogsBySearchId) {
			sb.append(DIV_TIMESTAMP_ATS).append(searchLog.getLoggedAt().getTime()).append(DIV_CLOSE);
			sb.append(searchLog.getText());
			sb.append("\n");
			searchId = searchLog.getSearchId();
		}
		if(searchId > 0) {
			String fullPath = getSambaFolderForSearch(searchId);
			fullPath += FILE_PREFIX_SEARCH_LOG + URLMaping.INSTANCE_DIR + ".html";
			try {
				FileUtils.write(new File(fullPath), sb.toString(), true);
				
			} catch (Exception e) {
				logger.error("Write to samba failed for path " + fullPath, e);
				Log.sendExceptionViaEmail(
						MailConfig.getMailLoggerToEmailAddress(), 
						"Logging to Samba failed", 
						e, 
						"SearchId used: " + searchId + ", path used: " + fullPath);
			}
			
		}
	}
	
	public static byte[] readOrderLogFromSamba(long searchId) {
		String fullPath = getSambaFolderForSearch(searchId);
		try {
			byte[] content = null;
			File toRead = new File(fullPath + FILE_NAME_ORDER);
			if(toRead.exists()) {
				content = FileUtils.readFileToByteArray(toRead);
			}
			return content;
		} catch (Exception e) {
			logger.error("Cannot readOrderLogFromSamba from searchid " + searchId, e);
		}
		
		return null;
	}
	
	public static byte[] readTsriLogFromSamba(long searchId) {
		String fullPath = getSambaFolderForSearch(searchId);
		try {
			byte[] content = null;
			File toRead = new File(fullPath + FILE_NAME_TSRI_LOG);
			if(toRead.exists()) {
				content = FileUtils.readFileToByteArray(toRead);
			}
			return content;
		} catch (Exception e) {
			logger.error("Cannot readOrderLogFromSamba from searchid " + searchId, e);
		}
		
		return null;
	}
	
	public static byte[] readSearchLogFromSamba(long searchId) {
		
		String fullPath = getSambaFolderForSearch(searchId);
		try {
			String[] list = null;
			File folder = new File(fullPath);
			if(folder.isDirectory()) {
				list = folder.list();
			}
			if(list == null || list.length == 0) {
				return null;
			}
			
			//best case, single log
//			if(list.length == 1) {
//				
//				StringBuilder sb = new StringBuilder();
//				if(!list[0].equals("logFile_mixed.html")) {
//					sb.append(org.apache.commons.io.FileUtils.readFileToString(new File(SearchLogger.HEADER_FILE_NAME)));
//				}
//				if(SmbClient.isEnabled()) {
//					sb.append(new String(SmbClient.getInstance().readFileToByteArray(fullPath + list[0])));
//				} else {
//					sb.append(FileUtils.readFileToString(new File(fullPath + list[0])));
//				}
//				return sb.toString().getBytes();
//			} 
			
			//multiple files, I need to sort them
			
			List<SearchLogArchiveStatement> statements = new ArrayList<SearchLogArchiveStatement>();
			String mixedFile = null;
			for (String filePath : list) {
				if(!filePath.startsWith(FILE_PREFIX_SEARCH_LOG)) {
					continue;
				}
				if(filePath.equals("logFile_mixed.html")) {
					mixedFile = filePath;
					continue;
				}
				
				String allData = null;
				allData = FileUtils.readFileToString(new File(fullPath + filePath));
				String[] split = allData.split(DIV_TIMESTAMP_ATS);
				for (String fullStatement : split) {
					try {
						int indexOf = fullStatement.indexOf(DIV_CLOSE);
						if (indexOf > 0) {
							String timestamp = fullStatement.substring(0, indexOf);
							SearchLogArchiveStatement archiveStatement = new SearchLogArchiveStatement();
							archiveStatement.setTimestamp(Long.parseLong(timestamp));
							archiveStatement.setContent(fullStatement.substring(indexOf + DIV_CLOSE.length()));
							statements.add(archiveStatement);
						}
					} catch (Exception e) {
						logger.error("Cannot prepare statement for searchId " + searchId, e);
						Log.sendExceptionViaEmail(
								MailConfig.getMailLoggerToEmailAddress(), 
								"Reading log from Samba failed", 
								e, 
								"SearchId used: " + searchId + ", content read: " + fullStatement);
					}
					
				}
			}
			
			Collections.sort(statements);
			
			StringBuilder sb = new StringBuilder();
			if(mixedFile != null) {
				String allData = null;
				allData = FileUtils.readFileToString(new File(fullPath + mixedFile));
				sb.append(allData);
			} else {
				sb.append(org.apache.commons.io.FileUtils.readFileToString(new File(SearchLogger.HEADER_FILE_NAME)));
			}
			for (SearchLogArchiveStatement searchLogArchiveStatement : statements) {
				sb.append(searchLogArchiveStatement.getContent());
			}
			if(sb.length() == 0) {
				return null;
			}
			return sb.toString().getBytes();
			
		} catch (IOException e) {
			logger.error("Cannot read Samba data from searchid " + searchId, e);
		}
		
		return null;
	}

	public static String getSambaFolderForSearch(long searchId) {
		return SharedDriveUtils.getSharedLogFolderForSearch(searchId);
	}



	public boolean saveLog(SearchLogEntry searchLog) {
		return savingQueue.add(searchLog);
	}

	/**
	 * Takes the orderContent String and saves it to a file in the shared disk in the log folder for the given search<br>
	 * It creates a file with the name "orderFile.html"
	 * @param search
	 * @param orderContent content of the order file
	 */
	public static void writeOrderFile(Search search, String orderContent) {
		String fullPath = SharedDriveUtils.getSharedLogFolderForSearch(search.getID());
		fullPath += FILE_NAME_ORDER;
		try {
			FileUtils.write(new File(fullPath), orderContent, false);
		} catch (Exception e) {
			System.err.println("Write to samba failed for path " + fullPath);
			e.printStackTrace();
			logger.error("Write to samba failed for path " + fullPath, e);
			Log.sendExceptionViaEmail(
					MailConfig.getMailLoggerToEmailAddress(), 
					"Order File on Samba failed", 
					e, 
					"SearchId used: " + search.getID() + ", path used: " + fullPath);
		}
	}
	
	/**
	 * Takes the content String and saves it to a file in the shared disk in the log folder for the given search<br>
	 * It creates a file with the name "tsrIndexFile.html"
	 * @param search
	 * @param content content of the TSRIndex log
	 */
	public static void writeTsrILogFile(Search search, String content) {
		String fullPath = SharedDriveUtils.getSharedLogFolderForSearch(search.getID());
		fullPath += FILE_NAME_TSRI_LOG;
		try {
			FileUtils.write(new File(fullPath), content, false);
		} catch (Exception e) {
			System.err.println("Write to samba failed for path " + fullPath);
			e.printStackTrace();
			logger.error("Write to samba failed for path " + fullPath, e);
			Log.sendExceptionViaEmail(
					MailConfig.getMailLoggerToEmailAddress(), 
					"TSRI Log File on Samba failed", 
					e, 
					"SearchId used: " + search.getID() + ", path used: " + fullPath);
		}
	}

	
	public static void copyLogInSamba(long searchIdSource, long searchIdDestination) {
		String fullPathSource = getSambaFolderForSearch(searchIdSource);
		String fullPathDestination = getSambaFolderForSearch(searchIdDestination);
		try {
			File folder = new File(fullPathSource);
			File[] list = null;
			if(folder.isDirectory()) {
				list = folder.listFiles();
			}
			if(list == null || list.length == 0) {
				return ;
			}
			for (File smbFile : list) {
				if(smbFile.getName().startsWith(FILE_PREFIX_SEARCH_LOG)) {
					try {
						FileUtils.copyFile(smbFile, new File(fullPathDestination + smbFile.getName()));
					} catch (Exception e) {
						logger.error("Cannot copyLogInSamba disabled from searchid " + searchIdSource + " to " + searchIdDestination + " file " + smbFile.getPath());
						Log.sendExceptionViaEmail(
								MailConfig.getMailLoggerToEmailAddress(), 
								"Copy in samba disabled failed", 
								e, 
								"Cannot copyLogInSamba disabled from searchid " + searchIdSource + " to " + searchIdDestination + 
									" from file " + smbFile.getPath() + " to " + fullPathDestination + smbFile.getName());
					}
				}
			
			}
		} catch (Exception e) {
			logger.error("Cannot copyLogInSamba from searchid " + searchIdSource + " to " + searchIdDestination);
		}
		
	}
	
	public static Logger getLogger() {
		return logger;
	}
	
	public static boolean isLogOnSamba(long searchId) {
		int dbValueForLogOriginalLocation = 0;
		try {
			dbValueForLogOriginalLocation = DBManager.getSimpleTemplate().queryForInt(
					"SELECT " + DBConstants.FIELD_SEARCH_FLAGS_LOG_ORIGINAL_LOCATION
					+ " FROM " + DBConstants.TABLE_SEARCH_FLAGS
					+ " WHERE " + DBConstants.FIELD_SEARCH_FLAGS_ID + " = ?", searchId);
		} catch (Exception e) {
			e.printStackTrace();
			String noLogError = "Cannot find value for " + DBConstants.FIELD_SEARCH_FLAGS_LOG_ORIGINAL_LOCATION +
					" in table " + DBConstants.TABLE_SEARCH_FLAGS + " for searchId " + searchId;
			logger.error(noLogError, e);
			Log.sendExceptionViaEmail(MailConfig.getMailLoggerToEmailAddress(), 
					"Getting " + DBConstants.FIELD_SEARCH_FLAGS_LOG_ORIGINAL_LOCATION + " from database failed", e, noLogError);
		}
		return dbValueForLogOriginalLocation == ServerConfig.getLogInTableVersion();
	}
}
