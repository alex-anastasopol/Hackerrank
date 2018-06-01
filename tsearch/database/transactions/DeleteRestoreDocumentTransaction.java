package ro.cst.tsearch.database.transactions;

import java.math.BigInteger;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang.StringUtils;
import org.springframework.jdbc.core.simple.ParameterizedRowMapper;
import org.springframework.jdbc.core.simple.SimpleJdbcTemplate;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionCallback;

import ro.cst.tsearch.database.DBManager;
import ro.cst.tsearch.utils.DBConstants;

import com.stewart.ats.base.document.RestoreDocumentDataI;

public class DeleteRestoreDocumentTransaction implements TransactionCallback {

	private RestoreDocumentDataI restoreDocumentDataI = null;
	
	
	public DeleteRestoreDocumentTransaction(RestoreDocumentDataI rsRestoreDocumentDataI) {
		this.restoreDocumentDataI = rsRestoreDocumentDataI;
		
		if(restoreDocumentDataI == null) {
			throw new NullPointerException("Field restoreDocumentDataI cannot be null");
		}
	}
	
	
	private static final String SQL_DELETE_DOCUMENT_BY_ID = 
		"DELETE FROM " + 
			DBConstants.TABLE_RECOVER_DOCUMENTS + " WHERE " + 
			DBConstants.FIELD_RECOVER_DOCUMENTS_ID + " = ? ";
	private static final String SQL_CHECK_MODULES_FOR_DOCUMENT =
		"SELECT count(*) FROM " + DBConstants.TABLE_MODULE_TO_DOCUMENT + " WHERE " + 
			DBConstants.FIELD_MODULE_TO_DOCUMENT_DOCUMENT_ID + " = ?";
	private static final String SQL_GET_ALL_MODULE_IDS_FOR_SEARCH = 
		"SELECT " + DBConstants.FIELD_MODULE_ID + " FROM " + 
			DBConstants.TABLE_MODULES + " WHERE " + 
			DBConstants.FIELD_MODULE_SEARCH_ID + " = ?";
	@Override
	public Object doInTransaction(TransactionStatus transactionStatus) {
		try {
			SimpleJdbcTemplate sjt = DBManager.getSimpleTemplate();
			List<Long> moduleIdsForSearch = sjt.query(
					SQL_GET_ALL_MODULE_IDS_FOR_SEARCH, new ParameterizedRowMapper<Long>() {
						@Override
						public Long mapRow(ResultSet resultSet, int arg1)
								throws SQLException {
							return resultSet.getLong(DBConstants.FIELD_MODULE_ID);
						}
					}, 
					restoreDocumentDataI.getSearchId());
			
			if(moduleIdsForSearch.isEmpty()) {
				return true;
			}
			
			String SQL_DELETE_MODULE_TO_DOCUMENT = 
				"DELETE FROM " + DBConstants.TABLE_MODULE_TO_DOCUMENT + " WHERE " + 
					DBConstants.FIELD_MODULE_TO_DOCUMENT_MODULE_ID + " in (" + 
						StringUtils.join(moduleIdsForSearch, ",") + ") AND " + 
					DBConstants.FIELD_MODULE_TO_DOCUMENT_DOCUMENT_ID + " = ?";
			//delete relations from module to documents for the given search
			int noOrRowsAffected = sjt.update(
					SQL_DELETE_MODULE_TO_DOCUMENT, 
					restoreDocumentDataI.getId());
			if(noOrRowsAffected > 0) {
				if(sjt.queryForInt(SQL_CHECK_MODULES_FOR_DOCUMENT, restoreDocumentDataI.getId()) == 0) {
					//if no another modules has this document attached to it i can delete the document
					sjt.update(SQL_DELETE_DOCUMENT_BY_ID, restoreDocumentDataI.getId());
				} else {
					//keep the document as cache for other searches
				}
			}
			//we must delete empty modules for this search
			
			String SQL_CHECK_MODULES_CNT = 
				"select m." + DBConstants.FIELD_MODULE_ID + ", count(*) cnt FROM " + 
				DBConstants.TABLE_MODULES + " m join " + 
				DBConstants.TABLE_MODULE_TO_DOCUMENT + " m2d on m2d." +
				DBConstants.FIELD_MODULE_TO_DOCUMENT_MODULE_ID + " = m." + 
				DBConstants.FIELD_MODULE_ID + " where m." +
				DBConstants.FIELD_MODULE_ID + " in (" + 
				StringUtils.join(moduleIdsForSearch, ",") + ") group by m." + 
				DBConstants.FIELD_MODULE_ID;
			
			List<Map<String, Object>> checkAndDelete = sjt.queryForList(SQL_CHECK_MODULES_CNT);
			List<Long> toDeleteModules = new ArrayList<Long>();
			HashSet<Long> foundModules = new HashSet<Long>();
			for (Map<String, Object> map : checkAndDelete) {
				Object fieldModuleId = map.get(DBConstants.FIELD_MODULE_ID);
				long moduleId = -1;
				if(fieldModuleId instanceof BigInteger) {
					moduleId = ((BigInteger)map.get(DBConstants.FIELD_MODULE_ID)).longValue();
				} else if (fieldModuleId instanceof Long) {
					moduleId = (Long)map.get(DBConstants.FIELD_MODULE_ID);
				}
				if((Long)map.get("cnt") == 0) {
					toDeleteModules.add(moduleId);
				}
				foundModules.add(moduleId);
			}
			for (Long idToCheck : moduleIdsForSearch) {
				if(!foundModules.contains(idToCheck)) {
					toDeleteModules.add(idToCheck);
				}
			}
			if(toDeleteModules.isEmpty()) {
				return true;
			}
			
			sjt.update("DELETE FROM " + 
					DBConstants.TABLE_MODULES + " WHERE " + 
					DBConstants.FIELD_MODULE_ID + " in (" +  
					StringUtils.join(toDeleteModules, ",") + ")");
			
		
		} catch (Exception e) {
			e.printStackTrace();
			transactionStatus.setRollbackOnly(); 
			return false;
		}
		
		return true;
	}

}
