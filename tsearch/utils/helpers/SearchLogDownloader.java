package ro.cst.tsearch.utils.helpers;

import java.io.File;
import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Date;
import java.util.zip.ZipException;

import org.apache.commons.io.FileUtils;

import ro.cst.tsearch.threads.AsynchSearchLogSaverThread;
import ro.cst.tsearch.utils.ZipUtils;

import com.stewart.ats.archive.ArchiveEntry;

public class SearchLogDownloader {

	public static void main(String[] args) throws ClassNotFoundException, IllegalAccessException, InstantiationException, SQLException, IOException {
		int limit = 20;
//		String pathPrefix = "\\\\atsserver\\samba\\search_logs";
		
//		downloadOrderLog(limit);
		
//		downloadSearchLog(limit);

		downloadTsrILog(limit);
	}
	
	protected static void downloadOrderLog(int limit) throws SQLException, ClassNotFoundException, IllegalAccessException, InstantiationException, IOException {
		Connection connection = SyncTool.getConnectionServer("local");
		PreparedStatement preparedStatementList = null;
		
		preparedStatementList = connection.prepareStatement("select search_id, searchOrder from ts_search_data_blob where searchOrder is not null limit " + limit);
		int ciclyIndex = 0;
		int deletedDocuments = 0;
		while (true) {
			
			System.out.println("Starting cicle " + ciclyIndex);
			
			ResultSet resultSet = preparedStatementList.executeQuery();
			
			String deleteSQL = "update ts_search_data_blob set searchOrder = null where search_id in (";
			int initialSize = deleteSQL.length();
			int donePerCycle = 0;
			while(resultSet.next()) {
				long searchId = resultSet.getLong(1);
				deleteSQL += searchId + ", ";
				byte[] document = resultSet.getBytes(2);
				if(document != null && document.length <= 3) {
					document = null;
				}
				
				if(document != null) {
					String sambaFolderForSearch = AsynchSearchLogSaverThread.getSambaFolderForSearch(searchId);
					String fullPath = sambaFolderForSearch + AsynchSearchLogSaverThread.FILE_NAME_ORDER;
					
					byte[] documentToSave = document;
					try {
						documentToSave = ZipUtils.unzipBytes(document);
						if(documentToSave == null) {
							documentToSave = document;
						}
					} catch (Exception e){
						System.out.println("ZipUtils");
					}
					FileUtils.writeByteArrayToFile( new File(fullPath),  documentToSave);
					donePerCycle ++;
				}
			}
			if(initialSize == deleteSQL.length()) {
				break;
			}
			deleteSQL = deleteSQL.substring(0, deleteSQL.length() - 2) + ")";
			
			Statement stm = connection.createStatement();
			//delete modules from modules table for the given search
			int noOrRowsAffected = stm.executeUpdate(deleteSQL);
			deletedDocuments += noOrRowsAffected;
			System.out.println("Updated " + noOrRowsAffected + ", saved " + donePerCycle + " more documents with a total of updated " + deletedDocuments + " with sql " + deleteSQL);
			ciclyIndex++;
		}
	}
	
	protected static void downloadTsrILog(int limit) throws SQLException, ClassNotFoundException, IllegalAccessException, InstantiationException, IOException {
		Connection connection = SyncTool.getConnectionServer("local");
		PreparedStatement preparedStatementList = null;
		
		preparedStatementList = connection.prepareStatement("select search_id, searchIndex from ts_search_data_blob where searchIndex is not null limit " + limit);
		int ciclyIndex = 0;
		int deletedDocuments = 0;
		while (true) {
			
			System.out.println("Starting cicle " + ciclyIndex);
			
			ResultSet resultSet = preparedStatementList.executeQuery();
			
			String deleteSQL = "update ts_search_data_blob set searchIndex = null where search_id in (";
			int initialSize = deleteSQL.length();
			int donePerCycle = 0;
			while(resultSet.next()) {
				long searchId = resultSet.getLong(1);
				deleteSQL += searchId + ", ";
				byte[] document = resultSet.getBytes(2);
				if(document != null && document.length <= 3) {
					document = null;
				}
				
				if(document != null) {
					String sambaFolderForSearch = AsynchSearchLogSaverThread.getSambaFolderForSearch(searchId);
					String fullPath = sambaFolderForSearch + AsynchSearchLogSaverThread.FILE_NAME_TSRI_LOG;
					
					byte[] documentToSave = document;
					try {
						documentToSave = ZipUtils.unzipBytes(document);
						if(documentToSave == null) {
							documentToSave = document;
						}
					} catch (Exception e){
						System.out.println("ZipUtils");
					}
					FileUtils.writeByteArrayToFile( new File(fullPath),  documentToSave);
					donePerCycle ++;
				}
			}
			if(initialSize == deleteSQL.length()) {
				break;
			}
			deleteSQL = deleteSQL.substring(0, deleteSQL.length() - 2) + ")";
			
			Statement stm = connection.createStatement();
			//delete modules from modules table for the given search
			int noOrRowsAffected = stm.executeUpdate(deleteSQL);
			deletedDocuments += noOrRowsAffected;
			System.out.println("Updated " + noOrRowsAffected + ", saved " + donePerCycle + " more documents with a total of updated " + deletedDocuments + " with sql " + deleteSQL);
			ciclyIndex++;
		}
	}

	protected static void downloadSearchLog(int limit) throws SQLException, ClassNotFoundException, IllegalAccessException, InstantiationException, IOException {
		Connection connection = SyncTool.getConnectionServer("local");
		PreparedStatement preparedStatementList = null;
		
		preparedStatementList = connection.prepareStatement("select s.id, f.toDisk, b.searchLog, s.comm_id, s.sdate "
				+ "from ts_search s "
				+ "join ts_search_flags f on s.id = f.search_id "
				+ "left join ts_search_data_blob b on s.id = b.search_id "
//				+ "where s.id = 4136535");
				+ "where log_original_location != 4 limit " + limit);
		
//				"select CONVERT_TZ(s.sdate, '+00:00', @@session.time_zone) sdate, di.* "
//				+ "from ts_documents_index di join "
//				+ "ts_search s on di.searchid = s.id where (document is not null or document is not null) "
//				+ "limit " + limit
//				);
		int ciclyIndex = 0;
		int deletedDocuments = 0;
		while (true) {
			
			System.out.println("Starting cicle " + ciclyIndex);
			
			ResultSet resultSet = preparedStatementList.executeQuery();
			
			String deleteSQL = "update ts_search_flags set log_original_location = 4 where search_id in (";
			int initialSize = deleteSQL.length();
			int donePerCycle = 0;
			while(resultSet.next()) {
				Date date = resultSet.getTimestamp(5);
				long searchId = resultSet.getLong(1);
				
				
				deleteSQL += searchId + ", ";
				
				byte[] document = resultSet.getBytes(3);
				if(document != null && document.length <= 3) {
					document = null;
				}
				if(document == null) {
					File archivedFile = new File(ArchiveEntry.getSearchLogCompletePath(searchId, date));
					if(archivedFile.exists()) {
						document = FileUtils.readFileToByteArray(archivedFile);
					}
				}
				if(document != null) {
					String sambaFolderForSearch = AsynchSearchLogSaverThread.getSambaFolderForSearch(searchId);
					String fullPath = sambaFolderForSearch + "logFile_mixed.html";
					
					File sambaFolderForSearchFile = new File(sambaFolderForSearch);
					if(sambaFolderForSearchFile.isDirectory()) {
						System.out.println("Cleaning already saved data on disk " + sambaFolderForSearch);
						FileUtils.deleteDirectory(sambaFolderForSearchFile);
					}
					byte[] documentToSave = document;
					try {
						documentToSave = ZipUtils.unzipBytes(document);
						if(documentToSave == null) {
							documentToSave = document;
						}
					} catch (Exception e){
						System.out.println("ZipUtils");
					}
					FileUtils.writeByteArrayToFile( new File(fullPath),  documentToSave);
					donePerCycle ++;
				}
			}
			if(initialSize == deleteSQL.length()) {
				break;
			}
			deleteSQL = deleteSQL.substring(0, deleteSQL.length() - 2) + ")";
			
			Statement stm = connection.createStatement();
			//delete modules from modules table for the given search
			int noOrRowsAffected = stm.executeUpdate(deleteSQL);
			deletedDocuments += noOrRowsAffected;
			System.out.println("Updated " + noOrRowsAffected + ", saved " + donePerCycle + " more documents with a total of updated " + deletedDocuments + " with sql " + deleteSQL);
			ciclyIndex++;
		}
	}

}
