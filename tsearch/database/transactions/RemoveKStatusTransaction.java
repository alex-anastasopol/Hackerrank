package ro.cst.tsearch.database.transactions;

import java.util.Vector;

import org.apache.commons.lang.StringUtils;
import org.springframework.jdbc.core.simple.SimpleJdbcTemplate;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionCallback;

import ro.cst.tsearch.Search;
import ro.cst.tsearch.database.ConnectionPool;
import ro.cst.tsearch.database.DBManager;
import ro.cst.tsearch.utils.DBConstants;

public class RemoveKStatusTransaction implements TransactionCallback {

	private Vector<Long> toRemoveKStatus = null;
	
	public RemoveKStatusTransaction(Vector<Long> toRemoveKStatus) {
		this.toRemoveKStatus = toRemoveKStatus;
	}
	
	@Override
	public Object doInTransaction(TransactionStatus status) {
		
		if(toRemoveKStatus == null || toRemoveKStatus.size() == 0) {
			return true;
		}
		
		Vector<Long> tsrCreatedS= new Vector<Long>();
		Vector<Long> nonTsrCreatedS = new Vector<Long>();
		
		for(Long l:toRemoveKStatus){
			if (DBManager.getTSRGenerationStatus(l) == Search.SEARCH_TSR_CREATED){
				tsrCreatedS.add(l);
			} else {
				nonTsrCreatedS.add(l);
			}
		}
		
		try {
			if(!nonTsrCreatedS.isEmpty()){
				String toUncloseString = StringUtils.join(nonTsrCreatedS.iterator(),",");
				SimpleJdbcTemplate sjt = ConnectionPool.getInstance().getSimpleTemplate();
				String sql = "UPDATE " + DBConstants.TABLE_SEARCH + ", " + DBConstants.TABLE_SEARCH_FLAGS + " set " + 
					DBConstants.TABLE_SEARCH + "." + DBConstants.FIELD_SEARCH_TSR_DATE + " = null ," +
					DBConstants.TABLE_SEARCH + "." + DBConstants.FIELD_SEARCH_REPORTS_DATE + " = " + DBConstants.TABLE_SEARCH + "." + DBConstants.FIELD_SEARCH_SDATE + " ," +
					DBConstants.TABLE_SEARCH + "." + DBConstants.FIELD_SEARCH_TIME_ELAPSED + " = 0  where " + 
					DBConstants.TABLE_SEARCH + "." + DBConstants.FIELD_SEARCH_ID + " in (" + toUncloseString + ") and " + 
					DBConstants.TABLE_SEARCH_FLAGS + "." + DBConstants.FIELD_SEARCH_FLAGS_IS_CLOSED + " = 1  AND " + 
					DBConstants.TABLE_SEARCH_FLAGS + "." + DBConstants.FIELD_SEARCH_FLAGS_TSR_CREATED + " = 0";
				
				sjt.update(sql);
				
				sql = "update " + DBConstants.TABLE_SEARCH_FLAGS + " set " + 
					DBConstants.FIELD_SEARCH_FLAGS_IS_CLOSED + " = 0 where " + 
					DBConstants.FIELD_SEARCH_FLAGS_ID + " in ( " + toUncloseString + " ) AND " + 
					DBConstants.FIELD_SEARCH_FLAGS_IS_CLOSED + " = 1 ";
				
				sjt.update(sql);
			}
			if(!tsrCreatedS.isEmpty()){
				String toUncloseString = StringUtils.join(tsrCreatedS.iterator(),",");
				SimpleJdbcTemplate sjt = ConnectionPool.getInstance().getSimpleTemplate();
				String sql = "UPDATE " + DBConstants.TABLE_SEARCH + ", " + DBConstants.TABLE_SEARCH_FLAGS + " set " + 
					DBConstants.TABLE_SEARCH + "." + DBConstants.FIELD_SEARCH_TIME_ELAPSED + " = 0  where " + 
					DBConstants.TABLE_SEARCH + "." + DBConstants.FIELD_SEARCH_ID + " in (" + toUncloseString + ") and " + 
					DBConstants.TABLE_SEARCH_FLAGS + "." + DBConstants.FIELD_SEARCH_FLAGS_IS_CLOSED + " = 1  "; 
				
				sjt.update(sql);
				
				sql = "update " + DBConstants.TABLE_SEARCH_FLAGS + " set " + 
					DBConstants.FIELD_SEARCH_FLAGS_IS_CLOSED + " = 0 where " + 
					DBConstants.FIELD_SEARCH_FLAGS_ID + " in ( " + toUncloseString + " ) AND " + 
					DBConstants.FIELD_SEARCH_FLAGS_IS_CLOSED + " = 1 ";
				
				sjt.update(sql);
			}
		} catch (Exception e) {
			status.setRollbackOnly();
			e.printStackTrace();
			return false;
		}
			
		return true;
	}

}
