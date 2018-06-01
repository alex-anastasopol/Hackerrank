package ro.cst.tsearch.database.transactions;

import java.sql.Types;

import org.springframework.jdbc.core.PreparedStatementCreatorFactory;
import org.springframework.jdbc.core.SqlParameter;
import org.springframework.jdbc.core.simple.SimpleJdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionCallback;

import ro.cst.tsearch.ServerConfig;
import ro.cst.tsearch.database.ConnectionPool;
import ro.cst.tsearch.utils.DBConstants;
import ro.cst.tsearch.utils.URLMaping;

public class NextIdTransaction implements TransactionCallback {

	public Object doInTransaction(TransactionStatus status) {
		
		try{
			SimpleJdbcTemplate sjt = ConnectionPool.getInstance().getSimpleTemplate();
			
		    String sql = "INSERT INTO " + DBConstants.TABLE_SEARCH + 
		    		" ( " + DBConstants.FIELD_SEARCH_SDATE + ", " +
		    				DBConstants.FIELD_SEARCH_REPORTS_DATE + ", " +
		    				DBConstants.FIELD_SEARCH_STATUS + ", " +
		    				"AUX_SERVER_NAME) VALUES " +
		    		" ( CONVERT_TZ(now(), @@session.time_zone, '+00:00'), " +
		    			"CONVERT_TZ(now(), @@session.time_zone, '+00:00'), " +
			    		"?, " +
			    		"'" + URLMaping.INSTANCE_DIR + "')";
		    
		    Object[] params = new Object[]{ DBConstants.SEARCH_NOT_SAVED };
		    
		    PreparedStatementCreatorFactory pstat = new PreparedStatementCreatorFactory(sql);
		    pstat.setReturnGeneratedKeys(true);
		    pstat.addParameter(new SqlParameter(DBConstants.FIELD_SEARCH_STATUS,Types.INTEGER));
		    
		    KeyHolder generatedKeyHolder = new GeneratedKeyHolder();
		    
			if(sjt.getJdbcOperations().update(pstat.newPreparedStatementCreator(params),generatedKeyHolder)!=1){
				//if the insert did not affect just one row we have an error
				throw new RuntimeException();
			}
			
			sql = "INSERT INTO " + 
				DBConstants.TABLE_SEARCH_FLAGS + " ( " + 
				DBConstants.FIELD_SEARCH_FLAGS_ID + ", " +
				DBConstants.FIELD_SEARCH_FLAGS_LOG_ORIGINAL_LOCATION + ", " + 
				DBConstants.FIELD_SEARCH_FLAGS_CHECKED_BY + " ) VALUES ( " + 
				generatedKeyHolder.getKey().longValue() + ", " + ServerConfig.getLogInTableVersion() + ", 0) ";
			
			if(sjt.update(sql)!=1) {
				throw new RuntimeException();
			}
			
			sql = "INSERT INTO " + 
				DBConstants.TABLE_SEARCH_DATA_BLOB + " ( " + 
				DBConstants.FIELD_SEARCH_DATA_BLOB_ID + " ) VALUES ( " + 
				generatedKeyHolder.getKey().longValue() + ") ";
			
			if(sjt.update(sql)!=1) {
				throw new RuntimeException();
			}
			
        	return generatedKeyHolder.getKey().longValue();
        	
	    }catch(RuntimeException e){ 
	    	// failure - rollback
	    	e.printStackTrace();
	    	status.setRollbackOnly();    		    	
	    	return 0;
	    }
	}

}
