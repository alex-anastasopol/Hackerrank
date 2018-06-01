package ro.cst.tsearch.utils.helpers;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang.StringUtils;

import ro.cst.tsearch.utils.DBConstants;

public class ModuleDocumentCleaner {
	public static void main(String[] args) throws ClassNotFoundException, IllegalAccessException, InstantiationException, SQLException {
		
		int limit = 5;
		
		Connection connection = SyncTool.getConnectionServer("ats04db");
		PreparedStatement preparedStatementList = null;
		List<Long> moduleIdsForSearch = new ArrayList<>();
		
		preparedStatementList = connection.prepareStatement(
				"SELECT * FROM modules_to_delete limit " + limit
				);
		
		int ciclyIndex = 0;
		while (true) {
			
			System.out.println("Starting cicle " + ciclyIndex);
			
			ResultSet resultSet = preparedStatementList.executeQuery();
			moduleIdsForSearch.clear();
			while(resultSet.next()) {
				moduleIdsForSearch.add(resultSet.getLong(1));
			}
			if(moduleIdsForSearch.isEmpty()) {
				break;
			}
			
			PreparedStatement preparedStatement = connection.prepareStatement(
					"SELECT " + DBConstants.FIELD_MODULE_TO_DOCUMENT_DOCUMENT_ID + " FROM " + 
							DBConstants.TABLE_MODULE_TO_DOCUMENT + " WHERE " + 
							DBConstants.FIELD_MODULE_ID + " in (" + 
							StringUtils.join(moduleIdsForSearch, ",") + ")"
					);
			List<Long> documentsIdsForSearch = new ArrayList<Long>();
			
			resultSet = preparedStatement.executeQuery();
			while(resultSet.next()) {
				documentsIdsForSearch.add(resultSet.getLong(1));
			}
			
			String SQL_DELETE_MODULE_TO_DOCUMENT = 
					"DELETE FROM " + DBConstants.TABLE_MODULE_TO_DOCUMENT + " WHERE " + 
						DBConstants.FIELD_MODULE_TO_DOCUMENT_MODULE_ID + " in (" + 
							StringUtils.join(moduleIdsForSearch, ",") + ") AND " + 
						DBConstants.FIELD_MODULE_TO_DOCUMENT_DOCUMENT_ID + " = ";
			
			String SQL_CHECK_MODULES_FOR_DOCUMENT =
					"SELECT count(*) FROM " + DBConstants.TABLE_MODULE_TO_DOCUMENT + " WHERE " + 
							DBConstants.FIELD_MODULE_TO_DOCUMENT_DOCUMENT_ID + " = ";
			int deleteModuleToDoc = 0;
			int deleteDoc = 0;
			int indexDoc = 0;
			System.out.println("To run " + documentsIdsForSearch.size() + " times... ");
			for (Long documentId : documentsIdsForSearch) {
				
				Statement stm = connection.createStatement();
				//delete modules from modules table for the given search
				int noOrRowsAffected = stm.executeUpdate(SQL_DELETE_MODULE_TO_DOCUMENT + documentId);
				deleteModuleToDoc += noOrRowsAffected;
				if (noOrRowsAffected > 0) {
					ResultSet executeQuery = stm.executeQuery(SQL_CHECK_MODULES_FOR_DOCUMENT + documentId);
					if(executeQuery.next()) {
						if (executeQuery.getInt(1) == 0) {
							//if the modules was deleted, then delete all the documents corresponding to those modules
							String SQL_DELETE_DOCUMENT_BY_ID =
									"DELETE FROM " + 
											DBConstants.TABLE_RECOVER_DOCUMENTS + " WHERE " + 
											DBConstants.FIELD_RECOVER_DOCUMENTS_ID + " =  " + documentId;
							deleteDoc += stm.executeUpdate(SQL_DELETE_DOCUMENT_BY_ID);
						} else {
							//keep the document as cache for other searches
						}
					}
					
				}
				indexDoc++;
				if(indexDoc%10 == 0) {
					System.out.println("Done " + indexDoc + " runs...");
				}
				
			}
			System.out.println("Done " + indexDoc + " runs...");
			
			String SQL_DELETE_MODULES = 
					"DELETE FROM " + DBConstants.TABLE_MODULES + " WHERE " + 
							DBConstants.FIELD_MODULE_ID + " in (" + 
									StringUtils.join(moduleIdsForSearch, ",") + ")";
			Statement stm = connection.createStatement();
			int executeUpdate = stm.executeUpdate(SQL_DELETE_MODULES);
			System.out.println("Deleted Module_to_documents " + deleteModuleToDoc);
			System.out.println("Deleted Documents " + deleteDoc);
			System.out.println("Deleted Modules " + executeUpdate);
			stm.executeUpdate("DELETE FROM modules_to_delete WHERE " + 
							DBConstants.FIELD_MODULE_ID + " in (" + 
									StringUtils.join(moduleIdsForSearch, ",") + ")");
			System.out.println();
			ciclyIndex++;
		}
		
		
	}
}
