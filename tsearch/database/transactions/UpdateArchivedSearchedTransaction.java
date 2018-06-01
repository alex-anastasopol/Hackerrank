package ro.cst.tsearch.database.transactions;

import org.springframework.jdbc.core.simple.SimpleJdbcTemplate;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionCallback;

import ro.cst.tsearch.data.SearchToArchive;
import ro.cst.tsearch.database.ConnectionPool;
import ro.cst.tsearch.utils.DBConstants;

public class UpdateArchivedSearchedTransaction implements TransactionCallback {

	private SearchToArchive archive = null;
	
	
	public UpdateArchivedSearchedTransaction(SearchToArchive archive) {
		super();
		this.archive = archive;
	}


	public Object doInTransaction(TransactionStatus s) {
		try {
			String stm = " update "+ DBConstants.TABLE_SEARCH +" set TSR_FILE_LINK = ? " + 
				 " where " +DBConstants.FIELD_SEARCH_ID  +" = ? ";
			
			String stmFlags = "update " + DBConstants.TABLE_SEARCH_FLAGS + " SET " + 
				DBConstants.FIELD_SEARCH_FLAGS_ARCHIVED + " = 1 WHERE " + 
				DBConstants.FIELD_SEARCH_FLAGS_ID + " = ?" ;
			
			SimpleJdbcTemplate sjt = ConnectionPool.getInstance().getSimpleTemplate();
			
			sjt.update(stm,archive.getTSRlink(),archive.getId());
			sjt.update(stmFlags,archive.getId());
			
		} catch (Exception e) {
			e.printStackTrace();
			s.setRollbackOnly();
			return false;
		}
		return true;
	}

}
