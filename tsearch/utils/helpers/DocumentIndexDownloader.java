package ro.cst.tsearch.utils.helpers;

import java.io.File;
import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.text.SimpleDateFormat;
import java.util.Collection;
import java.util.Date;
import java.util.regex.Matcher;

import org.apache.commons.io.FileUtils;

import ro.cst.tsearch.utils.ZipUtils;

public class DocumentIndexDownloader {
	public static void main(String[] args) throws ClassNotFoundException, IllegalAccessException, InstantiationException, SQLException, IOException {
		int limit = 20;
		String pathPrefix = "\\\\192.168.219.15\\PIC_ATS_Dev\\document_index";
		
		move(pathPrefix);
//		reinsert(pathPrefix);
		
//		Connection connection = SyncTool.getConnectionServer("ats01db");
//		PreparedStatement preparedStatementList = null;
//		
//		preparedStatementList = connection.prepareStatement(
//				"select CONVERT_TZ(s.sdate, '+00:00', @@session.time_zone) sdate, di.* from ts_documents_index di join ts_search s on di.searchid = s.id where (document is not null or document is not null) limit " + limit
//				);
//		int ciclyIndex = 0;
//		int deletedDocuments = 0;
//		while (true) {
//			
//			System.out.println("Starting cicle " + ciclyIndex);
//			
//			ResultSet resultSet = preparedStatementList.executeQuery();
//			
//			String deleteSQL = "update ts_documents_index set document = null, content = null where id in (";
//			int initialSize = deleteSQL.length();
//			while(resultSet.next()) {
//				Date date = resultSet.getTimestamp(1);
//				long searchId = resultSet.getLong(4);
//				deleteSQL += resultSet.getLong(2) + ", ";
//				SimpleDateFormat sdf = new SimpleDateFormat("/yyyy/MM/dd/");
//				sdf.setTimeZone(java.util.TimeZone.getTimeZone("GMT"));
//				String folderPath =  pathPrefix + sdf.format(date) + searchId + "/";
//				byte[] document = resultSet.getBytes(5);
//				if(document == null) {
//					document = ZipUtils.zipString(resultSet.getString(3));
//				}
//				if(document != null) {
//					FileUtils.writeByteArrayToFile(new File(folderPath + resultSet.getLong(2) + ".zip"), document);
//				}
//			}
//			if(initialSize == deleteSQL.length()) {
//				break;
//			}
//			deleteSQL = deleteSQL.substring(0, deleteSQL.length() - 2) + ")";
//			
//			Statement stm = connection.createStatement();
//			//delete modules from modules table for the given search
//			int noOrRowsAffected = stm.executeUpdate(deleteSQL);
//			deletedDocuments += noOrRowsAffected;
//			System.out.println("Delete " + noOrRowsAffected + " more documents with a total of " + deletedDocuments);
//			
//		}
	}
	
	public static void reinsert(String pathPrefix) throws ClassNotFoundException, IllegalAccessException, InstantiationException, SQLException {
		Connection connection = SyncTool.getConnectionServer("local");
		
		for (int i = 2008; i < 2014; i++) {
			Collection<File> listFiles = FileUtils.listFiles(new File(pathPrefix + "/" + i + "/"), new String[]{"zip"}, true);
			for (File file : listFiles) {
				String[] tokens = file.getPath().split(Matcher.quoteReplacement(File.separator));
				Statement stm = connection.createStatement();
				int noOrRowsAffected = 0;
				try {
					noOrRowsAffected = stm.executeUpdate("insert into ts_documents_index (id, searchId) values (" + tokens[tokens.length - 1].replace(".zip", "") + "," + tokens[tokens.length - 2] + " )");
				} catch (SQLException e) {
				}
				System.out.println(" Done " + noOrRowsAffected + " rows for " + file.getPath());
			}
		}
		
		
	}
	
	public static void move(String pathPrefix) throws ClassNotFoundException, IllegalAccessException, InstantiationException, SQLException, IOException {
		Connection connection = SyncTool.getConnectionServer("ats01db");
		for (int i = 2011; i < 2014; i++) {
			
			for (int j = 1; j <= 12 ; j++) {
				File folder = new File(pathPrefix + "/" + i + "/" + (j < 10?"0" + j + "/": j + "/"));
				System.out.println("Listing " + folder.getAbsolutePath());
				Collection<File> listFiles = FileUtils.listFiles(folder, new String[]{"zip"}, true);
				System.out.println("Found " + listFiles.size() + " files");
				for (File file : listFiles) {
					String[] tokens = file.getPath().split(Matcher.quoteReplacement(File.separator));
					int noOrRowsAffected = 0;
					try {
//						noOrRowsAffected = stm.executeUpdate();
						PreparedStatement ptsm = connection.prepareStatement("update ts_documents_index set document = ? where id = " + tokens[tokens.length - 1].replace(".zip", ""));
						ptsm.setBytes(1, FileUtils.readFileToByteArray(file));
						noOrRowsAffected = ptsm.executeUpdate();
						
						if(noOrRowsAffected > 0) {
							FileUtils.deleteQuietly(file);
						}
					} catch (SQLException e) {
					}
					System.out.println(" Done " + noOrRowsAffected + " rows for " + file.getPath());
				}
			}
			
			
		}
	}
	
}
