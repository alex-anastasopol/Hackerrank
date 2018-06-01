/** 
 * provides additional logging for each search 
 */
package ro.cst.tsearch.utils;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.sql.Types;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TimeZone;

import org.apache.log4j.Logger;
import org.apache.taglibs.standard.lang.jpath.encoding.HtmlEncoder;
import org.springframework.jdbc.core.PreparedStatementCreatorFactory;
import org.springframework.jdbc.core.SqlParameter;
import org.springframework.jdbc.core.simple.SimpleJdbcTemplate;

import ro.cst.tsearch.Search;
import ro.cst.tsearch.SearchManager;
import ro.cst.tsearch.ServerConfig;
import ro.cst.tsearch.bean.SearchAttributes;
import ro.cst.tsearch.bean.SearchLogEntry;
import ro.cst.tsearch.database.ConnectionPool;
import ro.cst.tsearch.database.DBManager;
import ro.cst.tsearch.exceptions.BaseException;
import ro.cst.tsearch.generic.IOUtil;
import ro.cst.tsearch.servers.HashCountyToIndex;
import ro.cst.tsearch.servers.bean.DataSite;
import ro.cst.tsearch.servlet.BaseServlet;
import ro.cst.tsearch.servlet.FileServlet;
import ro.cst.tsearch.threads.AsynchSearchLogSaverThread;


/**
 * @author radu bacrau
 *
 */
public class SearchLogger {	
	private static final Logger logger = Logger.getLogger(SearchLogger.class);
	
	private static Map<Long,PrintWriter> openedLogHandlers = new HashMap<Long,PrintWriter>();
    private static Object synch = openedLogHandlers;
 
    //server dependent logs
	private static Map<Long,PrintWriter> openedLogHandlersLocal = new HashMap<Long,PrintWriter>();
    private static Object synchLocal = openedLogHandlersLocal;
    
    private static Map<Long, Map<Long, Integer>> searchIDPairs = new LinkedHashMap<Long, Map<Long, Integer>>();
    
    private static String errorColors[] = {"black", "red"};
    public static int ERROR_MESSAGE = 1;
    public static int NORMAL_MESSAGE = 0;
    
    /**
     * Name of the file containing the search log header
     */
    public static final String HEADER_FILE_NAME  = 
    	BaseServlet.REAL_PATH + File.separator + 
    	"WEB-INF" + File.separator + 
    	"classes" + File.separator + 
    	"resource" + File.separator + 
    	"utils" + File.separator + 
    	"search_log_header.html";

    public static Logger getLogger() {
		return logger;
	}

	/**
     * logs a message in the "logFile" file located in the search folder;
     * the updated file is zipped and inserted in the database in order to 
     * have the displayed log updated instantly, when following the 'S' link from Dashboard 
     * 
     * @param info
     * @param searchId
     */
    public static void infoUpdateToDB(String info, long searchId) {
    	
    	try {
    		Search search = InstanceManager.getManager().getCurrentInstance(searchId).getCrtSearchContext();
    		if (search == null || search.getSearchDir().startsWith(String.valueOf(searchId)))
    		{
    			search = SearchManager.getSearchFromDisk(searchId);
    			if (search == null)
    				return;
    		}
    		try {
				if (searchId != search.getSearchID()) {
					long searchIdFromSearch = search.getSearchID();

					mailIssues(searchId, searchIdFromSearch, info, "infoUpdateToDB");
				}
			} catch (Exception e) {
				e.printStackTrace();
			}
			infoServerDependent(info, search);
			
			if(search.getSearchFlags().isWriteFirstLogLine()) {
    			String firstLogLine = getFirstLogLine(search);
    			logInTable(firstLogLine, searchId);
    			search.getSearchFlags().setWriteFirstLogLine(false);
    		}
    		logInTable(info, searchId);
			
			
    		if(ServerConfig.isEnableLogOldField()) {
    		
	    		PrintWriter pw = null;
	    		String fileName = null;
	    		
	    		// obtain print writer and update time stamp inside synchronized block
	    		boolean openedNow = false;
	    		boolean fileInitNow = false;
	    		synchronized(synch){
	    			pw = openedLogHandlers.get(searchId);
	    			if(pw == null){
	    				openedNow = true;
	    				fileName = search.getSearchDir() + File.separator + "logFile.html";
						if(!new File(fileName).exists()){
							try {
								byte[] logContentFromDatabase = DBManager.getSearchOrderLogs(search.getID(), FileServlet.VIEW_LOG_OLD_STYLE, true);
								if(logContentFromDatabase!=null) {
									FileUtils.writeByteArrayToFile(logContentFromDatabase,fileName);
								}else {
									FileUtils.copy(HEADER_FILE_NAME, fileName);
									fileInitNow = true;
								}
							}catch(Exception e) {
								e.printStackTrace();
								FileUtils.copy(HEADER_FILE_NAME, fileName);
								fileInitNow = true;
							}
						}    				
	    				pw = new PrintWriter(new FileOutputStream(fileName, true), true);
	    			}
	    			else
	    			{
	    				fileName = search.getSearchDir() + File.separator + "logFile.html"; 
	    			}
	    		}
	    		if (fileInitNow){
	    			String firstLogLine = getFirstLogLine(search);
	//    			logInTable(firstLogLine, searchId);
					pw.println(firstLogLine);
	    		}
	    		
	    		// actual printing performed outside synchronized block
	    		pw.println(info);
	    		pw.flush();
	    		
	    		// close the file if it was opened now
	    		if(openedNow){
	    			pw.close();
	    		}
	    		
	    		String zipToUploadFName = search.getSearchDir() /*+ File.separator*/ + searchId + "temp_unzip_file.zip";
	    		boolean zipDone = ZipUtils.zipFile(fileName, zipToUploadFName);
	    		
	    		if (zipDone)
	    		{ 
	    			SimpleJdbcTemplate conn = null;
	    			java.io.FileInputStream zipFStream = new java.io.FileInputStream( zipToUploadFName );
	            	ByteArrayOutputStream logFileBaos = new ByteArrayOutputStream();
	    			
	    			try 
	    			{
	                	IOUtil.copy( zipFStream, logFileBaos);
	    			} catch (IOException e)
	    			{
	    				throw new BaseException(e.getMessage());
	    			}
	    			
		    		String sql = "UPDATE " + DBConstants.TABLE_SEARCH_DATA_BLOB + 
					    		 " SET `searchLog` = ?" +
								 " WHERE " + DBConstants.FIELD_SEARCH_DATA_BLOB_ID + " = " + searchId;
	
	    			PreparedStatementCreatorFactory pstat = new PreparedStatementCreatorFactory(sql);
		    		pstat.addParameter(new SqlParameter(DBConstants.FIELD_SEARCH_DATA_BLOB_LOG, Types.BLOB));
	
		    		Object[] params = new Object[]{ logFileBaos.toByteArray() };
		    		try 
		    		{
		    			conn = ConnectionPool.getInstance().getSimpleTemplate();
		    			conn.getJdbcOperations().update(pstat.newPreparedStatementCreator(params));
		    		} catch (Exception e) 
		    		{
		    			e.printStackTrace();
		    			throw new BaseException("Error updating the Log File into database!");
		    		}
		    		
		    		File zipToUploadFile = new File (zipToUploadFName);
		    		zipToUploadFile.delete();//FileUtils.deleteFile(zipToUploadFName);
	
	    		}
    		
    		}
    		
    	} catch(Exception e) {
    		e.printStackTrace();
    		logger.error("infoUpdateToDB searchID: " + searchId + " with message [" + info + "]", e);
    	}
    }

    /**
     * Method used to log the information in a separate table
     * @param info
     * @param searchId
     */
	private static void logInTable(String info, long searchId) {
		if(ServerConfig.isEnableLogInSamba() && searchId > 0) {
			SearchLogEntry searchLog = new SearchLogEntry(searchId, info);
			AsynchSearchLogSaverThread.getInstance().saveLog(searchLog);
		}
	}
    
    public static void moveLogToDatabase(long searchId) {
    	if(ServerConfig.isEnableLogOldField()) {
    	try {
	    	Search search = InstanceManager.getManager().getCurrentInstance(searchId).getCrtSearchContext();
	    	String fileName = search.getSearchDir() + File.separator + "logFile.html";
	    	File logFile = new File(fileName);
	    	if(logFile.exists()) {
		    	String zipToUploadFName = search.getSearchDir() /*+ File.separator*/ + searchId + "temp_unzip_file.zip";
				boolean zipDone = ZipUtils.zipFile(fileName, zipToUploadFName);
				
				if (zipDone)
				{ 
					SimpleJdbcTemplate conn = null;
					java.io.FileInputStream zipFStream = new java.io.FileInputStream( zipToUploadFName );
		        	ByteArrayOutputStream logFileBaos = new ByteArrayOutputStream();
					
					try 
					{
		            	IOUtil.copy( zipFStream, logFileBaos);
					} catch (IOException e)
					{
						throw new BaseException(e.getMessage());
					}
					
		    		String sql = "UPDATE " + DBConstants.TABLE_SEARCH_DATA_BLOB + 
					    		 " SET `searchLog` = ?" +
								 " WHERE " + DBConstants.FIELD_SEARCH_DATA_BLOB_ID + " = " + searchId;
		
					PreparedStatementCreatorFactory pstat = new PreparedStatementCreatorFactory(sql);
		    		pstat.addParameter(new SqlParameter(DBConstants.FIELD_SEARCH_DATA_BLOB_LOG, Types.BLOB));
		
		    		Object[] params = new Object[]{ logFileBaos.toByteArray() };
		    		try 
		    		{
		    			conn = ConnectionPool.getInstance().getSimpleTemplate();
		    			conn.getJdbcOperations().update(pstat.newPreparedStatementCreator(params));
		    		} catch (Exception e) 
		    		{
		    			e.printStackTrace();
		    			throw new BaseException("Error updating the Log File into database!");
		    		}
		    		
		    		File zipToUploadFile = new File (zipToUploadFName);
		    		zipToUploadFile.delete();//FileUtils.deleteFile(zipToUploadFName);
		    		finish(search.getID());
		    		logFile.delete();
				}
	    	}
    	}catch(Exception e) {
    		e.printStackTrace();
    	}
    	}
    }

    
    /**
     * logs a message in "logFile" file located in the search folder
     * if the file is opened, then the current printWriter is used, 
     * else a new one is opened then closed
     * @param info
     * @param searchId
     */
    public static void info(String info,long searchId) {
    	try {
    		
    		Search search = InstanceManager.getManager().getCurrentInstance(searchId).getCrtSearchContext();
    		if(search == null ) {
    			SearchLogger.infoUpdateToDB(info, searchId);
    		} else {
    			try {
    				try {
						if (searchId != search.getSearchID()) {
							long searchIdFromSearch = search.getSearchID();
							mailIssues(searchId, searchIdFromSearch, info, "info");
							
							//log info to my destination
				    		logInTable(info, searchId);
							
							return;	//return if you want to write in another log
						}
					} catch (Exception e) {
						e.printStackTrace();
					}
					infoServerDependent(info, search);
    			} catch (Exception e) {
					e.printStackTrace();
				}
    			
    			if(search.getSearchFlags().isWriteFirstLogLine()) {
	    			String firstLogLine = getFirstLogLine(search);
	    			logInTable(firstLogLine, searchId);
	    			search.getSearchFlags().setWriteFirstLogLine(false);
	    		}
	    		logInTable(info, searchId);
    			
	    		
	    		if(ServerConfig.isEnableLogOldField()) {
	    			PrintWriter pw = null;
		    		// obtain print writer and update time stamp inside synchronized block
		    		boolean openedNow = false;
		    		boolean fileInitNow = false;
		    		synchronized(synch){
		    			pw = openedLogHandlers.get(searchId);
		    			String fileName = search.getSearchDir() + File.separator + "logFile.html";
		    			if(pw == null){
		    				openedNow = true;	    				
							if(!new File(fileName).exists()){
								try {
									byte[] logContentFromDatabase = DBManager.getSearchOrderLogs(search.getID(), FileServlet.VIEW_LOG_OLD_STYLE, true);
									if(logContentFromDatabase!=null) {
										FileUtils.writeByteArrayToFile(logContentFromDatabase,fileName);
									}else {
										FileUtils.copy(HEADER_FILE_NAME, fileName);
										fileInitNow = true;
										
										if (info != null && info.toLowerCase().contains("opened")){
											Log.sendEmail2("Search was opened and log was not found on database", "searchId: " + searchId + " "
													+ " search.getSearchID(): " + search.getSearchID()
													+ " on SearchLogger.info and message is: " + info);
										}
										
										logger.error("infoSimple searchID: " + searchId + " (search.getID()=" + search.getID() + ") with message [" + info + "]");
										
									}
								}catch(Exception e) {
									e.printStackTrace();
									logger.error("infoSimple openException searchID: " + searchId + " with message [" + info + "]", e);
									FileUtils.copy(HEADER_FILE_NAME, fileName);
									fileInitNow = true;
								}
							}    				
		    				pw = new PrintWriter(new FileOutputStream(fileName, true), true);
		    			}
		    		}	    		
		    		if (fileInitNow){
		    			String firstLogLine = getFirstLogLine(search);
						pw.println(firstLogLine);
		    		}
		    		
		    		// actual printing performed outside synchronized block
		    		pw.println(info);
		    		
		    		// close the file if it was opened now
		    		if(openedNow){
		    			pw.close();
		    		}
		    		
	    		}
	    		
    		}   		
    	} catch(Exception e) {
	    	e.printStackTrace();
	    	logger.error("infoSimple mainException searchID: " + searchId + " with message [" + info + "]", e);
	        	
    	}
    }

    /**
     * Logs information into server dependent logs.
     * These logs will log only the actions executed on the server holding the log file.
     * @param msg
     * @param search
     */
    public static void infoServerDependent(String msg, Search search){
    	String serverDependentLogDir = ServerConfig.getFilePathServerLogs();
    	
    	if (!StringUtils.isEmpty(serverDependentLogDir)){
    		PrintWriter pw = null;
    		boolean openedNow = false;
    		boolean fileInitNow = false;
    		synchronized(synchLocal){
    			pw = openedLogHandlersLocal.get(search.getSearchID());
    			
    			String instanceDir = URLMaping.INSTANCE_DIR;
    			File logFolder = new File(serverDependentLogDir + File.separator + search.getRelativePath());
    			String fileName = logFolder.getAbsolutePath()+ File.separator 
						+ "logFile_" + instanceDir + ".html";
    			
    			if(pw == null){
    				openedNow = true;
    				
    				if (!logFolder.exists()){
    					logFolder.mkdirs();
    				}
    				
    				if (instanceDir.equalsIgnoreCase("ats01")){
    					instanceDir = "beta";
    				}
    				
					if(!new File(fileName).exists()){
						FileUtils.copy(HEADER_FILE_NAME, fileName);
						fileInitNow = true;
					}    
					
    				try {
						pw = new PrintWriter(new FileOutputStream(fileName, true), true);
					} catch (FileNotFoundException e) {
						e.printStackTrace();
						logger.error("infoServerDependent searchID: " + search.getSearchID() + " with message [" + msg + "]", e);
					}
    			}
    		}
    		if (fileInitNow){
    			pw.println(getFirstLogLine(search));
    		}
    		// actual printing performed outside synchronized block
    		pw.println(msg);
    		
    		// close the file if it was opened now
    		if(openedNow){
    			pw.close();
    		}
    	} else {
    		logger.error("Server dependent log folder (\"files.path.serverlogs\") was not configured");
    	}
    }
    /**
     * 
     * @param searchId
     * @param searchIdFromSearch
     * @param info
     * @param whereFunction
     */
    public static void mailIssues(long searchId, long searchIdFromSearch, String info, String whereFunction){
    	
    	if (searchIDPairs.containsKey(searchId) 
				&& searchIDPairs.get(searchId).containsKey(searchIdFromSearch)){
			
    		int counter = searchIDPairs.get(searchId).get(searchIdFromSearch);
			counter++;
			
			Map<Long, Integer> numberOfMessagesIssued = searchIDPairs.get(searchId);
			numberOfMessagesIssued.put(searchIdFromSearch, counter);
			
			searchIDPairs.put(searchId, numberOfMessagesIssued);
			
			if (counter % 50 == 0){
				Log.sendEmail2("SearchIDs are not equals", "searchId: " + searchId + " "
						+ " it is not equal with search.getSearchID(): " + searchIdFromSearch
						+ " on SearchLogger." + whereFunction + " and issued " + counter + " messages. This is not good!");
			}
		} else {
			Log.sendEmail2("SearchIDs are not equals", "searchId: " + searchId + " "
						+ " it is not equal with search.getSearchID(): " + searchIdFromSearch
						+ " on SearchLogger." + whereFunction + "  when want to log this message: "+ info);
		    
			Map<Long, Integer> numberOfMessagesIssued = new LinkedHashMap<Long, Integer>();
			numberOfMessagesIssued.put(searchIdFromSearch, 1);
			
			searchIDPairs.put(searchId, numberOfMessagesIssued);
		}
    }
    /**
     * Logs a list of name-value pairs, together with a prefix and a suffix
     * @param prefix
     * @param logValues
     * @param suffix
     */
    public static void logValues(String prefix, List<String[]> logValues, String suffix,long searchId){
    	StringBuilder sb = new StringBuilder();
    	// append prefix
    	if(prefix != null) { sb.append(prefix); }
    	// print name-value pairs
    	for(String [] pair : logValues){
     		if(pair.length == 2 && !"".equals(pair[1])){
    			sb.append(pair[0] + " = " + pair[1] + " ");
    		}
    	}
    	// append suffix
    	if(suffix != null) { sb.append(suffix); }
    	// log message
    	info(sb.toString(),searchId);
    }
    
    /**
     * Closes the printWriter for the search denoted by searchID
     * @param searchID
     */
    public static void finish(long searchID){
    	synchronized(synch){    		
    		// check that we have an associated printwriter
    		if(openedLogHandlers.containsKey(searchID)){    			
    			// remove printwriter and close it
    			openedLogHandlers.remove(searchID).close();
    		}
    	}
    }
    
    /**
     * Opens the printWriter for the search denoted by searchId
     * @param searchId
     */
    public static void init(long searchId){
    	boolean fileInitNow = false;
    	synchronized(synch){
    		try{
	    		PrintWriter pw = openedLogHandlers.get(searchId);
	    		Search search = InstanceManager.getManager().getCurrentInstance(searchId).getCrtSearchContext();
				String fileName = search.getSearchDir() + File.separator + "logFile.html";
				if(pw == null){
					if(!new File(fileName).exists()){
						try {
							byte[] logContentFromDatabase = DBManager.getSearchOrderLogs(search.getID(), FileServlet.VIEW_LOG_OLD_STYLE, true);
							if(logContentFromDatabase!=null) {
								FileUtils.writeByteArrayToFile(logContentFromDatabase,fileName);
							}else {
								FileUtils.copy(HEADER_FILE_NAME, fileName);
								fileInitNow = true;
							}
						}catch(Exception e) {
							e.printStackTrace();
							FileUtils.copy(HEADER_FILE_NAME, fileName);
							fileInitNow = true;
							logger.error("init searchID: " + searchId + " [search.getSearchID()=" + search.getSearchID() + "]", e);
						}
					}
					pw = new PrintWriter(new FileOutputStream(fileName, true), true);
					openedLogHandlers.put(searchId, pw);
					if (fileInitNow){
						info(getFirstLogLine(search), searchId);
					}					
				}
				
    		}catch(FileNotFoundException e){
    			e.printStackTrace();
    			logger.error("init searchID: " + searchId, e);
    		}
		}
    }
    
	//for loggging dates. Most users are in CDT timezone, so we use this timezone as default.
    public static String getCurDateTimeCST(){
		FormatDate ASDate = new FormatDate(FormatDate.DISC_FORMAT_2);
		return ASDate.getDate(FormatDate.currentTimeMillis(), TimeZone.getDefault());
    }
    
    public static String getTimeStamp(Long searchId){
    	String user=InstanceManager.getManager().getCurrentInstance(searchId).getCurrentUser().getAttribute(1).toString();
    	return "<span class=\"timestamp\">on " + getCurDateTimeCST()
    			+ ", on server " + URLMaping.INSTANCE_DIR
    			+", By User:"+user + "</span>";
    }
    
    public static String getTimeStamp(String user){
    	return "<span class=\"timestamp\">on " + getCurDateTimeCST()
    			+ ", on server " + URLMaping.INSTANCE_DIR
    			+", By User:"+user + "</span>";
    }
    
    /**
     * @param searchId
     * @return "&lt;span class=\"timestamp\"&gt;on MMM, dd yyyy HH:mm:ss z , on server atsX&lt;/span&gt;"
     */
    public static String getTimeStampAndLocation(Long searchId){
    	return "<span class=\"timestamp\">on " + getCurDateTimeCST()
    			+ ", on server " + URLMaping.INSTANCE_DIR
    			+ "</span>";
    }

    /**
     * Format for the date: MM/DD/YYYY "separator" HH:MM:SS
     * @param userLogin
     * @param separator the String between year and hour
     * @return a span containing the html formated
     */
    public static String getTimeStampFormat1(String userLogin, String separator){
    	GregorianCalendar cal = (GregorianCalendar) Calendar.getInstance();
    	String dateTimeStrFormat1 = (cal.get( GregorianCalendar.MONTH ) + 1) + "/" +
		cal.get( GregorianCalendar.DAY_OF_MONTH ) + "/" + cal.get( GregorianCalendar.YEAR ) + separator +
		cal.get( GregorianCalendar.HOUR_OF_DAY ) + ":" + cal.get( GregorianCalendar.MINUTE ) + ":" + cal.get( GregorianCalendar.SECOND );
    	
    	return "<span class=\"timestamp\">" + dateTimeStrFormat1
    			+ ", on server " + URLMaping.INSTANCE_DIR
    			+ ", by " + userLogin + "</span>";
    }
    
    public static String getFirstLogLine(Search search){
    	String result = "<br/><b>Abstractor File no: " + search.getAbstractorFileNo() + " SearchId: " + search.getID();
    	String so_order_id = search.getSa().getAtribute(SearchAttributes.STEWARTORDERS_ORDER_ID);
    	if (!StringUtils.isEmpty(so_order_id)) {
    		result += " Source ID: " + so_order_id;
    	}
    	result += "</b>";
		return result;
    }
    
    /**
     * log that it didn't search with CCN<br>
     * The message is displayed once per automatic
     * @param name - company name
     * @param searchId
     */
	public static void logWillNotSearch(String name,long searchId) {
		DataSite server = null;
		String serverType = "";
		String serverName = "";

		try{
			server = HashCountyToIndex.getCrtServer(searchId, false);
			if (server != null){
				serverName = server.getName();
				serverType = server.getSiteTypeAbrev();
				
			} else {
				Search global = SearchManager.getSearch(searchId);
				if (global != null){
					serverType = global.getCrtServerType(false);
					serverName = global.getCrtServerName(false);
				}
			}
		} catch (BaseException e){
			e.printStackTrace();
		}	
		Search global = InstanceManager.getManager().getCurrentInstance( searchId ).getCrtSearchContext();
		if (!serverType.equals("")){
			global.getSa().setSiteNameSearchSkipped(serverType);
		}
		String warn1 = serverName + " - " + "Will not search with "+ name.toUpperCase() +" because it is a Common Company Name";
		String warn2 = "</div><span class=\"serverName\">" + serverName + "</span> - " + "<font color=\"red\">Will not search with <B>"+ name.toUpperCase() +"</b> because it is a Common Company Name.</font><BR>";
		if (!global.warningDisplayed(warn1)){
     		IndividualLogger.info( warn1 ,searchId);
     		global.addCompanyNameWarning(warn1);
		}
		if (!global.warningDisplayed(warn2)){
	        SearchLogger.info(/*StringUtils.createCollapsibleHeader() +*/ warn2, searchId);
	        global.addCompanyNameWarning(warn2);
		}
	}
	
	/**
	 * log in the format<br>
	 * FLCharlotteTR - message<br>
	 * The message is displayed once per automatic
	 * @param message
	 * @param searchId
	 * @param messageType - 0 normal (black color)
	 * 						1 error (red color)
	 */
	public static void logWithServerName(String message,long searchId, int messageType, DataSite server) {
		String serverName = "";

		if (server == null) {
			serverName = "Unknown Server";
		} else {
			serverName = server.getName();
		}
		Search global = InstanceManager.getManager().getCurrentInstance( searchId ).getCrtSearchContext();
		String warn1 = serverName + " - " + message;
		String warn2 = "</div><span class=\"serverName\">" + serverName + "</span> - " + "<font color=\"" + errorColors[messageType] + "\">" + HtmlEncoder.encode(message) + "</font><BR>";
		if (!global.warningDisplayed(warn1)){
     		IndividualLogger.info( warn1 ,searchId);
     		global.addCompanyNameWarning(warn1);
		}
		if (!global.warningDisplayed(warn2)){
	        SearchLogger.info(warn2, searchId);
	        global.addCompanyNameWarning(warn2);
		}
	}
	
	public static boolean copyLogFromOldSearchToNewSearch(long oldSearchId, String oldSearchDir, Search newSearch) throws FileNotFoundException, IOException{
		
		String fileName = oldSearchDir + File.separator + "logFile.html";
		boolean isCopied = false;
		File file = new File(fileName);
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        boolean logExists = file.exists();
        
        if (logExists) {
            IOUtil.copy(new FileInputStream(file), baos);
        } else { // get log from DB
        	try {
        		byte[] logContentFromDatabase = DBManager.getSearchOrderLogs(oldSearchId, FileServlet.VIEW_LOG_OLD_STYLE, true);
        		if(logContentFromDatabase!=null) {
        			baos.write(logContentFromDatabase);
        		}
        	} catch (Exception e) {
        		e.printStackTrace();
        		logger.error("copyLogFromOldSearchToNewSearch oldSearchId=" + oldSearchId + ", oldSearchDir=[" + oldSearchDir + "], newSearch.getSearchDir() = [" + newSearch.getSearchDir() + "]", e);
        	}
        }
        
        if(baos.size() > 0) {
	        FileOutputStream newLogFile = new FileOutputStream(newSearch.getSearchDir() + "logFile.html");
	        newLogFile.write(baos.toByteArray());
	        newLogFile.flush(); newLogFile.close();
//	        oldSearch.mustSaveSearchHTML = false;
	        isCopied = true;
        }

        return isCopied;
	}
	
	/**
	 * Modifies the Search Log header from file search_log_header.html
	 *  
	 * @param search
	 * @param headerSourceFile
	 * @param detinationFile
	 * 
	 */
	public static String updateLogHeader(long searchId, String log) {
		String searchLog = log;
		
		// for bug 7043
		if (searchLog.indexOf("@@ORIGINAL_ORDER_LINK@@") != -1)
			searchLog = searchLog.replace("@@ORIGINAL_ORDER_LINK@@", makeOriginalOrderLink(searchId));
		else {
			if (searchLog.indexOf("<!--ORIGINAL_ORDER_SPAN-->") != -1) {
				String oldLinkPattern = "original=true&view=1&viewOrder=1&userId=[^&]*&viewDescrSearchId=[^&]*&showFileId=true";
				searchLog = searchLog
						.replaceAll(
								"(?ism)" + oldLinkPattern,
								makeOriginalOrderLink(searchId)
										.replaceAll(
												".*(original=true&view=1&viewOrder=1&userId=[^&]*&viewDescrSearchId=[^&]*&showFileId=true).*",
												"$1"));
			} else {
				// old search log with no link
				searchLog = searchLog
						.replaceAll(
								"(?ism)(>Show Extra Info</span>)",
								"$1&nbsp;&nbsp;&nbsp;\n<!--ORIGINAL_ORDER_SPAN-->\n<span id=\"showOriginalOrder\" class=\"submitLinkBlue\" onClick=\""
										+ makeOriginalOrderLink(searchId)
										+ "\">Original Order</span>");
			}
		}
		if (searchLog.indexOf(">Show Extra Info</span>&nbsp;&nbsp;&nbsp;") == -1) {
			searchLog = searchLog.replaceAll(">Show Extra Info</span>",
					">Show Extra Info</span>&nbsp;&nbsp;&nbsp;");
		}
		
//		FileUtils.writeByteArrayToFile(searchLog.getBytes(), log);
		
		return searchLog;
	}
	
	private static String makeOriginalOrderLink(long searchId) {
		String link = "";

		link = "javascript:orderWindow=window.open('/title-search/jsp/TSDIndexPage/viewDescription.jsp?"
				+ "original=true"
				+ "&view=1&viewOrder=1&userId="
				+ "&viewDescrSearchId="
				+ searchId
				+ "&showFileId=true','orderWindow','toolbar=yes,location=yes,"
				+ "directories=yes,status=yes,menubar=yes,scrollbars=yes,resizable=yes,"
				+ "width=1024,height=768,left=0,top=0,screenX=0,screenY=0'); orderWindow.focus();";
		
		return link;
	}
}
