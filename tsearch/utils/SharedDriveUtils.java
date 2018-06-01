package ro.cst.tsearch.utils;

import java.text.SimpleDateFormat;
import java.util.Date;

import org.apache.commons.lang.StringUtils;

import ro.cst.tsearch.Search;
import ro.cst.tsearch.ServerConfig;

public class SharedDriveUtils {

	/**
	 * 
	 * @param search 
	 * @return base_samba_path/2012/01/31/search_id/
	 */
	public static String getOcrFileArchiveFolder(Search search) {
		return SharedDriveUtils.getOcrFileArchiveFolder(search.getID(), search.getTSROrderDate());
	}

	/**
	 * 
	 * @param searchId
	 * @param startDate date what will be formated as yyyy/MM/dd
	 * @return base_samba_path/yyyy/MM/dd/search_id/
	 */
	public static String getOcrFileArchiveFolder(long searchId, Date startDate) {
		return getFullPathFolder(ServerConfig.getOcrFilesInSambaPath(), searchId, startDate); 
	}
	
	private static String getFullPathFolder(String prefixPath, long searchId, Date startDate) {
		if(startDate == null) {
			return null;
		}
		
		SimpleDateFormat sdf = new SimpleDateFormat("/yyyy/MM/dd/");
		sdf.setTimeZone(java.util.TimeZone.getTimeZone("GMT"));
		
		return prefixPath 
				+ sdf.format(startDate) 
				+ searchId 
				+ "/";
	}
	
	/**
	 * Search is used to get start date that will be formatted as yyyy/MM/dd and also for the seach_id<br>
	 * If base_path does not exists or is empty, <code>null</code> will be returned
	 * @param search 
	 * @return base_path/yyyy/MM/dd/search_id/ or null if shared drive is not enabled
	 */
	public static String getDocumentIndexFolder(Search search) {
		String documentIndexInSharedDrivePath = ServerConfig.getDocumentIndexInSharedDrivePath();
		if(StringUtils.isBlank(documentIndexInSharedDrivePath) || search == null) {
			return null;
		}
		return getDocumentIndexFolder(
				search.getID(), 
				search.getTSROrderDate());

	}
	
	/**
	 * Start date will be formatted as yyyy/MM/dd<br>
	 * If base_path does not exists or is empty, <code>null</code> will be returned
	 * @param searchId
	 * @param startDate
	 * @return base_path/yyyy/MM/dd/search_id/ or null if shared drive is not enabled
	 */
	public static String getDocumentIndexFolder(long searchId, Date startDate) {
		String documentIndexInSharedDrivePath = ServerConfig.getDocumentIndexInSharedDrivePath();
		if(StringUtils.isBlank(documentIndexInSharedDrivePath)) {
			return null;
		}
		return getFullPathFolder(
				documentIndexInSharedDrivePath, 
				searchId, 
				startDate);

	}
	
	public static String getDocumentIndexFile(long searchId, Date startDate, int documentIndex) {
		String documentIndexInSharedDrivePath = ServerConfig.getDocumentIndexInSharedDrivePath();
		if(StringUtils.isBlank(documentIndexInSharedDrivePath)) {
			return null;
		}
		String fullPathFolder = getFullPathFolder(
				documentIndexInSharedDrivePath, 
				searchId, 
				startDate);
		if(fullPathFolder == null) {
			return null;
		}
		return fullPathFolder + documentIndex + ".zip";

	}
	
	
	/**
	 * Search is used to get start date that will be formatted as yyyy/MM/dd and also for the seach_id<br>
	 * If base_path does not exists or is empty, <code>null</code> will be returned
	 * @param search 
	 * @return base_path/yyyy/MM/dd/search_id/ or null if backup path is not enabled
	 */
	public static String getDocumentIndexBackupLocalFolder(Search search) {
		String documentIndexInSharedDrivePath = ServerConfig.getDocumentIndexBackupLocalPath();
		if(StringUtils.isBlank(documentIndexInSharedDrivePath) || search == null) {
			return null;
		}
		return getDocumentIndexBackupLocalFolder(
				search.getID(), 
				search.getTSROrderDate());

	}
	
	/**
	 * Start date will be formatted as yyyy/MM/dd<br>
	 * If base_path does not exists or is empty, <code>null</code> will be returned
	 * @param searchId
	 * @param startDate
	 * @return base_path/yyyy/MM/dd/search_id/ or null if backup path is not enabled
	 */
	public static String getDocumentIndexBackupLocalFolder(long searchId, Date startDate) {
		String documentIndexInSharedDrivePath = ServerConfig.getDocumentIndexBackupLocalPath();
		if(StringUtils.isBlank(documentIndexInSharedDrivePath)) {
			return null;
		}
		return getFullPathFolder(
				documentIndexInSharedDrivePath, 
				searchId, 
				startDate);

	}
	
	public static String getThreadLogFolder(Date logDate, boolean backup) {
		String threadLogsPath = null;
		if(!backup) {
			threadLogsPath = ServerConfig.getThreadLogsInSharedDrivePath();
		} else {
			threadLogsPath = ServerConfig.getThreadLogsBackupLocalPath();
		}
		
		if(StringUtils.isBlank(threadLogsPath) || logDate == null) {
			return null;
		}
		return threadLogsPath + new SimpleDateFormat("/yyyy/MM/dd/").format(logDate); 
	}
	
	public static String getErrorLogFolder(Date logDate, boolean backup) {
		String threadLogsPath = null;
		if(!backup) {
			threadLogsPath = ServerConfig.getErrorLogsInSharedDrivePath();
		} else {
			threadLogsPath = ServerConfig.getErrorLogsBackupLocalPath();
		}
		
		if(StringUtils.isBlank(threadLogsPath) || logDate == null) {
			return null;
		}
		return threadLogsPath + new SimpleDateFormat("/yyyy/MM/dd/").format(logDate); 
	}
	
	
	/**
	 * Search is used to get start date that will be formatted as yyyy/MM/dd and also for the seach_id<br>
	 * If base_path does not exists or is empty, <code>null</code> will be returned
	 * @param search 
	 * @return base_path/yyyy/MM/dd/search_id/ or null if shared drive is not enabled
	 */
	public static String getSsfDocumentIndexFolder(Search search) {
		String documentIndexInSharedDrivePath = ServerConfig.getSsfDocumentIndexInSharedDrivePath();
		if(StringUtils.isBlank(documentIndexInSharedDrivePath) || search == null) {
			return null;
		}
		return getSsfDocumentIndexFolder(
				search.getID(), 
				search.getTSROrderDate());

	}
	
	/**
	 * Start date will be formatted as yyyy/MM/dd<br>
	 * If base_path does not exists or is empty, <code>null</code> will be returned
	 * @param searchId
	 * @param startDate
	 * @return base_path/yyyy/MM/dd/search_id/ or null if shared drive is not enabled
	 */
	public static String getSsfDocumentIndexFolder(long searchId, Date startDate) {
		String documentIndexInSharedDrivePath = ServerConfig.getSsfDocumentIndexInSharedDrivePath();
		if(StringUtils.isBlank(documentIndexInSharedDrivePath)) {
			return null;
		}
		return getFullPathFolder(
				documentIndexInSharedDrivePath, 
				searchId, 
				startDate);

	}
	
	/**
	 * Search is used to get start date that will be formatted as yyyy/MM/dd and also for the seach_id<br>
	 * If base_path does not exists or is empty, <code>null</code> will be returned
	 * @param search 
	 * @return base_path/yyyy/MM/dd/search_id/ or null if backup path is not enabled
	 */
	public static String getSsfDocumentIndexBackupLocalFolder(Search search) {
		String documentIndexInSharedDrivePath = ServerConfig.getSsfDocumentIndexBackupLocalPath();
		if(StringUtils.isBlank(documentIndexInSharedDrivePath) || search == null) {
			return null;
		}
		return getSsfDocumentIndexBackupLocalFolder(
				search.getID(), 
				search.getTSROrderDate());

	}
	
	/**
	 * Start date will be formatted as yyyy/MM/dd<br>
	 * If base_path does not exists or is empty, <code>null</code> will be returned
	 * @param searchId
	 * @param startDate
	 * @return base_path/yyyy/MM/dd/search_id/ or null if backup path is not enabled
	 */
	public static String getSsfDocumentIndexBackupLocalFolder(long searchId, Date startDate) {
		String documentIndexInSharedDrivePath = ServerConfig.getSsfDocumentIndexBackupLocalPath();
		if(StringUtils.isBlank(documentIndexInSharedDrivePath)) {
			return null;
		}
		return getFullPathFolder(
				documentIndexInSharedDrivePath, 
				searchId, 
				startDate);

	}
	
	public static String getSsfDocumentIndexFile(long searchId, Date startDate, long documentIndex) {
		String documentIndexInSharedDrivePath = ServerConfig.getSsfDocumentIndexInSharedDrivePath();
		if(StringUtils.isBlank(documentIndexInSharedDrivePath)) {
			return null;
		}
		String fullPathFolder = getFullPathFolder(
				documentIndexInSharedDrivePath, 
				searchId, 
				startDate);
		if(fullPathFolder == null) {
			return null;
		}
		return fullPathFolder + documentIndex + ".zip";

	}
	
	public static String getSharedLogFolderForSearch(long searchId) {
		String searchIdString = Long.toString(searchId);
		String fullPath = ServerConfig.getLogInSambaPath() + "/";
		for (int i = 0; i < searchIdString.length(); i++) {
			fullPath += searchIdString.charAt(i) + "/";
		}
		return fullPath + searchIdString + "/";
	}
	
	/**
	 * Search is used to get start date that will be formatted as yyyy/MM/dd and also for the seach_id<br>
	 * If base_path does not exists or is empty, <code>null</code> will be returned
	 * @param search 
	 * @return base_path/yyyy/MM/dd/search_id/ or null if shared drive is not enabled
	 */
	public static String getSearchContextFolder(Search search) {
		String basePath = ServerConfig.getSearchContextInSharedDrivePath();
		if(StringUtils.isBlank(basePath) || search == null) {
			return null;
		}
		return getFullPathFolder(
				basePath,
				search.getID(), 
				search.getTSROrderDate());

	}
	
	/**
	 * Search is used to get start date that will be formatted as yyyy/MM/dd and also for the seach_id<br>
	 * If base_path does not exists or is empty, <code>null</code> will be returned
	 * @param searchId
	 * @param startDate
	 * @return base_path/yyyy/MM/dd/search_id/ or null if shared drive is not enabled
	 */
	public static String getSearchContextFolder(long searchId, Date startDate) {
		String basePath = ServerConfig.getSearchContextInSharedDrivePath();
		if(StringUtils.isBlank(basePath) || startDate == null) {
			return null;
		}
		return getFullPathFolder(
				basePath,
				searchId, 
				startDate);

	}
	
	/**
	 * Search is used to get start date that will be formatted as yyyy/MM/dd and also for the seach_id<br>
	 * If base_path does not exists or is empty, <code>null</code> will be returned
	 * @param search 
	 * @return base_path/yyyy/MM/dd/search_id/ or null if backup is not enabled
	 */
	public static String getSearchContextBackupLocalFolder(Search search) {
		String basePath = ServerConfig.getSearchContextBackupLocalPath();
		if(StringUtils.isBlank(basePath) || search == null) {
			return null;
		}
		return getFullPathFolder(
				basePath,
				search.getID(), 
				search.getTSROrderDate());

	}

}
