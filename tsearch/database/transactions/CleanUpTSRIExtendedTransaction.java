package ro.cst.tsearch.database.transactions;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

import org.apache.commons.lang.StringUtils;
import org.springframework.jdbc.core.simple.ParameterizedRowMapper;
import org.springframework.jdbc.core.simple.SimpleJdbcTemplate;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionCallback;

import ro.cst.tsearch.database.DBManager;
import ro.cst.tsearch.utils.DBConstants;

public class CleanUpTSRIExtendedTransaction implements TransactionCallback {

	private long searchId;
	
	
	public CleanUpTSRIExtendedTransaction(long searchId) {
		this.searchId = searchId;
		
	}
		
	private static final String SQL_GET_ALL_MODULE_IDS_FOR_SEARCH = 
		"SELECT " + DBConstants.FIELD_MODULE_ID + " FROM " + 
			DBConstants.TABLE_MODULES + " WHERE " + 
			DBConstants.FIELD_MODULE_SEARCH_ID + " = ?";

	@Override
	public Object doInTransaction(TransactionStatus transactionStatus) {
		try {
			SimpleJdbcTemplate sjt = DBManager.getSimpleTemplate();
			//get all modules for a searchId
			List<Long> moduleIdsForSearch = sjt.query(
					SQL_GET_ALL_MODULE_IDS_FOR_SEARCH, new ParameterizedRowMapper<Long>() {
						@Override
						public Long mapRow(ResultSet resultSet, int arg1)
								throws SQLException {
							return resultSet.getLong(DBConstants.FIELD_MODULE_ID);
						}
					}, searchId);
			
			if(moduleIdsForSearch.isEmpty()) {
				return true;
			}
			//get all documentIds for all modules found upstairs
			String SQL_GET_ALL_DOCUMENTS_IDS_FOR_MODULES = 
					"SELECT " + DBConstants.FIELD_MODULE_TO_DOCUMENT_DOCUMENT_ID + " FROM " + 
						DBConstants.TABLE_MODULE_TO_DOCUMENT + " WHERE " + 
						DBConstants.FIELD_MODULE_ID + " in (" + 
						StringUtils.join(moduleIdsForSearch, ",") + ")";
			
			List<Long> documentsIdsForSearch = sjt.query(
					SQL_GET_ALL_DOCUMENTS_IDS_FOR_MODULES, new ParameterizedRowMapper<Long>() {
						@Override
						public Long mapRow(ResultSet resultSet, int arg1)
								throws SQLException {
							return resultSet.getLong(DBConstants.FIELD_MODULE_TO_DOCUMENT_DOCUMENT_ID);
						}
					});
			
			
			String SQL_DELETE_MODULES = 
				"DELETE FROM " + DBConstants.TABLE_MODULES + " WHERE " + 
						DBConstants.FIELD_MODULE_SEARCH_ID + " = ?";
			
			//delete modules from modules table for the given search
			int noOrRowsAffected = sjt.update(SQL_DELETE_MODULES, searchId);
			
			if(noOrRowsAffected > 0 && documentsIdsForSearch.size() > 0) {
				 
				String SQL_CHECK_MODULES_FOR_DOCUMENT =
							"SELECT count(*) FROM " + DBConstants.TABLE_MODULES + " WHERE " + 
									DBConstants.FIELD_MODULE_SEARCH_ID + " = ?";
				 
				if(sjt.queryForInt(SQL_CHECK_MODULES_FOR_DOCUMENT, searchId) == 0) {
					//if the modules was deleted, then delete all the documents corresponding to those modules
					String SQL_DELETE_DOCUMENT_BY_ID =
							"DELETE FROM " + DBConstants.TABLE_RECOVER_DOCUMENTS + " WHERE " + 
									DBConstants.FIELD_RECOVER_DOCUMENTS_ID + " in (" + 
									StringUtils.join(documentsIdsForSearch, ",") + ")";
					sjt.update(SQL_DELETE_DOCUMENT_BY_ID);
				}
			}

		} catch (Exception e) {
			e.printStackTrace();
			transactionStatus.setRollbackOnly(); 
			return false;
		}
		
		return true;
	}

}
