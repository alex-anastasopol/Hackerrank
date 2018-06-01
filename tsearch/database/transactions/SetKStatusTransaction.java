package ro.cst.tsearch.database.transactions;

import java.util.List;
import java.util.Map;
import java.util.Vector;

import org.springframework.jdbc.core.simple.SimpleJdbcTemplate;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionCallback;

import ro.cst.tsearch.Search;
import ro.cst.tsearch.database.ConnectionPool;
import ro.cst.tsearch.database.DBManager;
import ro.cst.tsearch.utils.DBConstants;

public class SetKStatusTransaction implements TransactionCallback {
	
	private long abstractorId;
	private Vector<Long> idsList;
	private String closeDate;
	private long payrateId;
	
	public SetKStatusTransaction(long abstractorId, Vector<Long> idsList, String closeDate, long payrateId) {
		this.abstractorId = abstractorId;
		this.idsList = idsList;
		this.closeDate = closeDate;
		this.payrateId = payrateId;
	}

	public Object doInTransaction(TransactionStatus status) {
		
		try {
			
			SimpleJdbcTemplate sjt = ConnectionPool.getInstance().getSimpleTemplate();
			String sql = null;
			
			
			Vector<Long> tsrCreatedS= new Vector<Long>();
			Vector<Long> nonTsrCreatedS = new Vector<Long>();
			
			for(Long l:idsList){
				if (DBManager.getTSRGenerationStatus(l) == Search.SEARCH_TSR_CREATED){
					tsrCreatedS.add(l);
				} else {
					nonTsrCreatedS.add(l);
				}
			}
			
			if(!tsrCreatedS.isEmpty()){
				String tsrCreatedSAsString = org.apache.commons.lang.StringUtils.join(tsrCreatedS.iterator(), ",");
				if(abstractorId>=0) {
				
					sql = "SELECT a." + DBConstants.FIELD_SEARCH_ID + " FROM " + 
						DBConstants.TABLE_SEARCH + " a JOIN " + 
						DBConstants.TABLE_SEARCH_FLAGS + " af ON a." + 
						DBConstants.FIELD_SEARCH_ID + " = af." + 
						DBConstants.FIELD_SEARCH_FLAGS_ID +	" where a. " + 
						DBConstants.FIELD_SEARCH_ID + " in (" + tsrCreatedSAsString + ") AND " + 
						DBConstants.FIELD_SEARCH_STATUS + " in (0,1)  AND ( " + 
						DBConstants.FIELD_SEARCH_FLAGS_CHECKED_BY + " = 0 OR (SELECT GID FROM ts_user WHERE USER_ID = " + 
						abstractorId + ") <= 32 OR (abstract_id = agent_id AND abstract_id = " + abstractorId + "))";
					
					List<Map<String, Object>> toUpdate = sjt.queryForList(sql);
					StringBuilder toUpdateIds = new StringBuilder();
					for (Map<String, Object> map : toUpdate) {
						toUpdateIds.append(map.get(DBConstants.FIELD_SEARCH_ID) + ",");
					}
					if(toUpdateIds.length()>0)	//remove the last ","
						toUpdateIds.deleteCharAt(toUpdateIds.length()-1);
					
					if(toUpdateIds.length()==0)	//nothing to update
						return true;
					
					sql = "update " + 
						DBConstants.TABLE_SEARCH + " set " + 
						"ABSTRACT_ID = " + abstractorId + ", " +
						"USER_RATING_ID = " + payrateId + ", " + 
						DBConstants.FIELD_SEARCH_TIME_ELAPSED + " = getBussinesElapsedTime(unix_timestamp(" +DBConstants.FIELD_SEARCH_SDATE + "), " +
						"unix_timestamp(" +	closeDate + "), 1) " +
						" where ID in (" + toUpdateIds.toString() + " )";
					sjt.update(sql);
					
					sql = "update " + DBConstants.TABLE_SEARCH_FLAGS + " set " + 
						DBConstants.FIELD_SEARCH_FLAGS_CHECKED_BY + " = 0, " + 
						DBConstants.FIELD_SEARCH_FLAGS_IS_CLOSED + " = 1 where " + 
						DBConstants.FIELD_SEARCH_FLAGS_ID + " in ( " + toUpdateIds.toString() + " )";
					sjt.update(sql);
					
				} else {
					
					sql = "update ts_search_flags set checked_by = 0 WHERE search_id in (" + 
						"SELECT a.ID from TS_SEARCH a where a.id in (" + tsrCreatedSAsString + ") and a.STATUS in (0,1))";
					sjt.update(sql);
				
					sql = "update " + DBConstants.TABLE_SEARCH + " set " +
						DBConstants.FIELD_SEARCH_TIME_ELAPSED + " = getBussinesElapsedTime(unix_timestamp(" +
						DBConstants.FIELD_SEARCH_SDATE + "), unix_timestamp(" +
						closeDate + "), 1) " +
						" where ID in (" + tsrCreatedSAsString + ") AND STATUS  in (0,1)";
					sjt.update(sql);
					
					sql = "update " + DBConstants.TABLE_SEARCH_FLAGS + " set " + 
					DBConstants.FIELD_SEARCH_FLAGS_IS_CLOSED + " = 1 where " + 
					DBConstants.FIELD_SEARCH_FLAGS_ID + " in ( " + tsrCreatedSAsString + " )";
					
					sjt.update(sql);
					
				}
			}
			
			if(!nonTsrCreatedS.isEmpty()){
				String nonTsrCreatedSAsString = org.apache.commons.lang.StringUtils.join(nonTsrCreatedS.iterator(), ",");
				if(abstractorId>=0) {
				
					sql = "SELECT a." + DBConstants.FIELD_SEARCH_ID + " FROM " + 
						DBConstants.TABLE_SEARCH + " a JOIN " + 
						DBConstants.TABLE_SEARCH_FLAGS + " af ON a." + 
						DBConstants.FIELD_SEARCH_ID + " = af." + 
						DBConstants.FIELD_SEARCH_FLAGS_ID +	" where a. " + 
						DBConstants.FIELD_SEARCH_ID + " in (" + nonTsrCreatedSAsString + ") AND " + 
						DBConstants.FIELD_SEARCH_STATUS + " in (0,1)  AND ( " + 
						DBConstants.FIELD_SEARCH_FLAGS_CHECKED_BY + " = 0 OR (SELECT GID FROM ts_user WHERE USER_ID = " + 
						abstractorId + ") <= 32 OR (abstract_id = agent_id AND abstract_id = " + abstractorId + "))";
					
					List<Map<String, Object>> toUpdate = sjt.queryForList(sql);
					StringBuilder toUpdateIds = new StringBuilder();
					for (Map<String, Object> map : toUpdate) {
						toUpdateIds.append(map.get(DBConstants.FIELD_SEARCH_ID) + ",");
					}
					if(toUpdateIds.length()>0)	//remove the last ","
						toUpdateIds.deleteCharAt(toUpdateIds.length()-1);
					
					if(toUpdateIds.length()==0)	//nothing to update
						return true;
					
					sql = "update " + DBConstants.TABLE_SEARCH + 
							" set " + DBConstants.FIELD_SEARCH_TSR_DATE + " = " + closeDate +
							", " + DBConstants.FIELD_SEARCH_REPORTS_DATE + " = " + closeDate +
						", ABSTRACT_ID = " + abstractorId + ", USER_RATING_ID = " + payrateId + ", "
						+ DBConstants.FIELD_SEARCH_TIME_ELAPSED + " = getBussinesElapsedTime(unix_timestamp(" +
						DBConstants.FIELD_SEARCH_SDATE + "), unix_timestamp(" +
						closeDate + "), 1) " +
						" where ID in (" + toUpdateIds.toString() + " )";
					sjt.update(sql);
					
					sql = "update " + DBConstants.TABLE_SEARCH_FLAGS + " set " + 
						DBConstants.FIELD_SEARCH_FLAGS_CHECKED_BY + " = 0, " + 
						DBConstants.FIELD_SEARCH_FLAGS_IS_CLOSED + " = 1 where " + 
						DBConstants.FIELD_SEARCH_FLAGS_ID + " in ( " + toUpdateIds.toString() + " )";
					sjt.update(sql);
					
				} else {
					
					sql = "update ts_search_flags set checked_by = 0 WHERE search_id in (" + 
						"SELECT a.ID from TS_SEARCH a where a.id in (" + nonTsrCreatedSAsString + ") and a.STATUS in (0,1))";
					sjt.update(sql);
				
					sql = "update " + DBConstants.TABLE_SEARCH + 
							" set " + DBConstants.FIELD_SEARCH_TSR_DATE + " = " + closeDate +
							", " + DBConstants.FIELD_SEARCH_REPORTS_DATE + " = " + closeDate +
							", " + DBConstants.FIELD_SEARCH_TIME_ELAPSED + " = getBussinesElapsedTime(unix_timestamp(" +
						DBConstants.FIELD_SEARCH_SDATE + "), unix_timestamp(" +
						closeDate + "), 1) " +
						" where ID in (" + nonTsrCreatedSAsString + ") AND STATUS  in (0,1)";
					sjt.update(sql);
					
					sql = "update " + DBConstants.TABLE_SEARCH_FLAGS + " set " + 
					DBConstants.FIELD_SEARCH_FLAGS_IS_CLOSED + " = 1 where " + 
					DBConstants.FIELD_SEARCH_FLAGS_ID + " in ( " + nonTsrCreatedSAsString + " )";
					
					sjt.update(sql);
					
				}
			}
			
			
		} catch (Exception e) {
			status.setRollbackOnly();
			e.printStackTrace();
			return false;
		}
		
        return true;
	}

}
