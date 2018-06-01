package ro.cst.tsearch.database;

import java.util.List;

import ro.cst.tsearch.ServerConfig;
import ro.cst.tsearch.utils.DBConstants;

import com.stewart.ats.archive.ArchiveEntry;
import com.stewart.ats.archive.DocIndexArchiveEntry;
import com.stewart.ats.filereplication.FileStatus;

public class DBArchive {
	
	private static final String SQL_GET_SEARCHES_TO_BE_ARCHIVED_LIST =
		"SELECT s." + DBConstants.FIELD_SEARCH_ID + 
			" , s." + DBConstants.FIELD_SEARCH_LAST_SAVE_DATE + 
			" , d." + DBConstants.FIELD_SEARCH_DATA_VERSION +
			" , s." + DBConstants.FIELD_SEARCH_SDATE + 
			" FROM " + DBConstants.TABLE_SEARCH + " s " + 
			" JOIN " + DBConstants.TABLE_SEARCH_FLAGS + " f " + 
				" ON s." + DBConstants.FIELD_SEARCH_ID + " = f." + DBConstants.FIELD_SEARCH_FLAGS_ID + 
			" JOIN " + DBConstants.TABLE_SEARCH_DATA + " d " + 
				" ON s." + DBConstants.FIELD_SEARCH_ID + " = d." + DBConstants.FIELD_SEARCH_DATA_SEARCHID + 
			" WHERE s." + DBConstants.FIELD_SEARCH_LAST_SAVE_DATE + " < DATE_SUB(NOW(), INTERVAL ? DAY) " + 
			" AND f." + DBConstants.FIELD_SEARCH_FLAGS_ARCHIVED + " = 0 " +
			" AND s." + DBConstants.FIELD_SEARCH_ID + " not in ( SELECT fs." + FileStatus.FIELD_FILE_ID +
				" FROM " + FileStatus.TABLE_FILE_STATUS + " fs " + 
				" WHERE fs." + FileStatus.FIELD_ENTRY_TYPE + " = " + FileStatus.VALUE_ENTRY_TYPE_SEARCH_ARCHIVE +
				" AND fs." + FileStatus.FIELD_SERVER_ID + " = ? ) " +
			" AND (-1 = ? OR s." + DBConstants.FIELD_SEARCH_COMM_ID + " = ?) " +
			" AND d." + DBConstants.FIELD_SEARCH_DATA_CONTEXT + " is not null " +
			" LIMIT ? "
		;
	
	public static List<ArchiveEntry> getSearchesToBeArchived(int limit, int olderThanDays, int commId) {
		try {
			return DBManager.getSimpleTemplate().query(
					SQL_GET_SEARCHES_TO_BE_ARCHIVED_LIST, 
					new ArchiveEntry(), 
					olderThanDays,
					ServerConfig.getServerId(),
					commId, commId,
					limit);
		} catch (Exception e) {
			return null;
		}
		
	}
	
	
	//only for testing purpose
	private static final String SQL_GET_SEARCHES_BY_SEARCH_ID_TO_BE_ARCHIVED_LIST =
			"SELECT s." + DBConstants.FIELD_SEARCH_ID + 
				" , s." + DBConstants.FIELD_SEARCH_LAST_SAVE_DATE + 
				" , d." + DBConstants.FIELD_SEARCH_DATA_VERSION +
				" , s." + DBConstants.FIELD_SEARCH_SDATE + 
				" FROM " + DBConstants.TABLE_SEARCH + " s " + 
				" JOIN " + DBConstants.TABLE_SEARCH_FLAGS + " f " + 
					" ON s." + DBConstants.FIELD_SEARCH_ID + " = f." + DBConstants.FIELD_SEARCH_FLAGS_ID + 
				" JOIN " + DBConstants.TABLE_SEARCH_DATA + " d " + 
					" ON s." + DBConstants.FIELD_SEARCH_ID + " = d." + DBConstants.FIELD_SEARCH_DATA_SEARCHID + 
				" WHERE s." + DBConstants.FIELD_SEARCH_ID + " = ? " + 
				" AND f." + DBConstants.FIELD_SEARCH_FLAGS_ARCHIVED + " = 0 " +
				" AND s." + DBConstants.FIELD_SEARCH_ID + " not in ( SELECT fs." + FileStatus.FIELD_FILE_ID +
					" FROM " + FileStatus.TABLE_FILE_STATUS + " fs " + 
					" WHERE fs." + FileStatus.FIELD_ENTRY_TYPE + " = " + FileStatus.VALUE_ENTRY_TYPE_SEARCH_ARCHIVE +
					" AND fs." + FileStatus.FIELD_SERVER_ID + " = ? ) " +
				" AND (-1 = ? OR s." + DBConstants.FIELD_SEARCH_COMM_ID + " = ?) " +
				" AND d." + DBConstants.FIELD_SEARCH_DATA_CONTEXT + " is not null " +
				" LIMIT ? "
			;
	
	
	//only for testing purpose
	public static List<ArchiveEntry> getSearchesToBeArchivedBySearchId(int limit, long searchId, int commId) {
		try {
			return DBManager.getSimpleTemplate().query(
					SQL_GET_SEARCHES_BY_SEARCH_ID_TO_BE_ARCHIVED_LIST, 
					new ArchiveEntry(), 
					searchId,
					ServerConfig.getServerId(),
					commId, commId,
					limit);
		} catch (Exception e) {
			return null;
		}
		
	}
	
	
	private static final String SQL_GET_ARCHIVED_SEARCHES_LIST =
			"SELECT DISTINCT s." + DBConstants.FIELD_SEARCH_ID + 
				" , s." + DBConstants.FIELD_SEARCH_LAST_SAVE_DATE + 
				" , d." + DBConstants.FIELD_SEARCH_DATA_VERSION +
				" , s." + DBConstants.FIELD_SEARCH_SDATE +
				" FROM " + DBConstants.TABLE_SEARCH + " s " + 
				" JOIN " + DBConstants.TABLE_SEARCH_FLAGS + " f " + 
					" ON s." + DBConstants.FIELD_SEARCH_ID + " = f." + DBConstants.FIELD_SEARCH_FLAGS_ID + 
				" JOIN " + DBConstants.TABLE_SEARCH_DATA + " d " + 
					" ON s." + DBConstants.FIELD_SEARCH_ID + " = d." + DBConstants.FIELD_SEARCH_DATA_SEARCHID + 
				" JOIN " + DBConstants.TABLE_DOCUMENT_INDEX + " i " + 
					" ON s." + DBConstants.FIELD_SEARCH_ID + " = i." + DBConstants.FIELD_DOCUMENT_INDEX_SEARCHID + 	
				" WHERE s." + DBConstants.FIELD_SEARCH_LAST_SAVE_DATE + " < DATE_SUB(NOW(), INTERVAL ? DAY) " + 
				" AND f." + DBConstants.FIELD_SEARCH_FLAGS_ARCHIVED + " = 0 " +
				" AND f." + DBConstants.FIELD_SEARCH_FLAGS_TO_DISK + " = 1 " +
				" AND (-1 = ? OR s." + DBConstants.FIELD_SEARCH_COMM_ID + " = ?) " +
				" AND d." + DBConstants.FIELD_SEARCH_DATA_CONTEXT + " is null " +
				" AND (i." + DBConstants.FIELD_DOCUMENT_INDEX_BLOB + " is not null " +
						" OR i." + DBConstants.FIELD_DOCUMENT_INDEX_CONTENT + " is not null) " +
				//" GROUP BY s." + DBConstants.FIELD_SEARCH_ID + 
				//		" , s." + DBConstants.FIELD_SEARCH_LAST_SAVE_DATE + 
				//		" , d." + DBConstants.FIELD_SEARCH_DATA_VERSION +
				//		" , s." + DBConstants.FIELD_SEARCH_SDATE + 
				" LIMIT ? "
			;
		
		public static List<DocIndexArchiveEntry> getArchivedSearches(int limit, int olderThanDays, int commId) {
			try {
				return DBManager.getSimpleTemplate().query(
						SQL_GET_ARCHIVED_SEARCHES_LIST, 
						new DocIndexArchiveEntry(), 
						olderThanDays,
						commId, commId,
						limit);
			} catch (Exception e) {
				return null;
			}
			
		}
		
		
		//only for testing purpose
		private static final String SQL_GET_ARCHIVED_SEARCHES_BY_SEARCHID_LIST =
				"SELECT DISTINCT s." + DBConstants.FIELD_SEARCH_ID + 
					" , s." + DBConstants.FIELD_SEARCH_LAST_SAVE_DATE + 
					" , d." + DBConstants.FIELD_SEARCH_DATA_VERSION +
					" , s." + DBConstants.FIELD_SEARCH_SDATE + 
					" FROM " + DBConstants.TABLE_SEARCH + " s " + 
					" JOIN " + DBConstants.TABLE_SEARCH_FLAGS + " f " + 
						" ON s." + DBConstants.FIELD_SEARCH_ID + " = f." + DBConstants.FIELD_SEARCH_FLAGS_ID + 
					" JOIN " + DBConstants.TABLE_SEARCH_DATA + " d " + 
						" ON s." + DBConstants.FIELD_SEARCH_ID + " = d." + DBConstants.FIELD_SEARCH_DATA_SEARCHID + 
					" JOIN " + DBConstants.TABLE_DOCUMENT_INDEX + " i " + 
						" ON s." + DBConstants.FIELD_SEARCH_ID + " = i." + DBConstants.FIELD_DOCUMENT_INDEX_SEARCHID + 	
					" WHERE s." + DBConstants.FIELD_SEARCH_ID + " = ? " + 
					" AND f." + DBConstants.FIELD_SEARCH_FLAGS_ARCHIVED + " = 0 " +
					" AND f." + DBConstants.FIELD_SEARCH_FLAGS_TO_DISK + " = 1 " +
					" AND (-1 = ? OR s." + DBConstants.FIELD_SEARCH_COMM_ID + " = ?) " +
					" AND d." + DBConstants.FIELD_SEARCH_DATA_CONTEXT + " is null " + 
					" AND (i." + DBConstants.FIELD_DOCUMENT_INDEX_BLOB + " is not null " +
							" OR i." + DBConstants.FIELD_DOCUMENT_INDEX_CONTENT + " is not null) " +
					" LIMIT ? "
				;
		
		
		//only for testing purpose
		public static List<DocIndexArchiveEntry> getArchivedSearchesBySearchId(int limit, long searchId, int commId) {
			try {
				return DBManager.getSimpleTemplate().query(
						SQL_GET_ARCHIVED_SEARCHES_BY_SEARCHID_LIST, 
						new DocIndexArchiveEntry(), 
						searchId,
						commId, commId,
						limit);
			} catch (Exception e) {
				return null;
			}
			
		}

}
