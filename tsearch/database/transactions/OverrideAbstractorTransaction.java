package ro.cst.tsearch.database.transactions;

import org.springframework.jdbc.core.simple.SimpleJdbcTemplate;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionCallback;

import ro.cst.tsearch.database.ConnectionPool;
import ro.cst.tsearch.utils.DBConstants;
import ro.cst.tsearch.utils.StringUtils;

public class OverrideAbstractorTransaction implements TransactionCallback {

	private String abstractorId = null;
	private String searchList = null;
	private boolean overrideAgent = false;
	
	public OverrideAbstractorTransaction(String abstractorId, String searchList, boolean overrideAgent) {
		this.abstractorId = abstractorId;
		this.searchList = StringUtils.makeValidNumberList(searchList);
		this.overrideAgent = overrideAgent;
	}

	/**
	 *  sql = " UPDATE " + TABLE_SEARCH + " SET "
	            	+ ABSTRACTOR_ID + " = " + abstractorIdArray[0]
	            	+ ", checked_by = 1 WHERE id in ( " + newList + " )";
	 */
	public Object doInTransaction(TransactionStatus status) {
		
		try{
			SimpleJdbcTemplate sjt = ConnectionPool.getInstance().getSimpleTemplate();
			
		    String sql = " UPDATE " + DBConstants.TABLE_SEARCH + " SET " +
        		DBConstants.FIELD_SEARCH_ABSTRACT_ID + " = ? ";
        	
        	//if we have the overrideAgent flag set, we update both the agent id and the abstractor id
        	if(overrideAgent) sql += "," +  DBConstants.FIELD_SEARCH_AGENT_ID + " = ? ";
        	
        	sql+= " WHERE id in ( ";
        	
        	String[] listElements = searchList.split(",");
        	
        	for(int i=1;i<=listElements.length;i++) {
        		if(i!=listElements.length) sql += " ?, ";
        		else sql += " ? ";
        	}
        	
        	sql +=" )";
        	
        	if(!overrideAgent) {
        		sjt.update(sql, (Object[]) (abstractorId + "," + searchList).split(","));
        	}
        	
        	if(overrideAgent)
        		sjt.update(sql,(Object[]) (abstractorId + "," + abstractorId + "," + searchList).split(","));
        	
			
			sql = " UPDATE " + DBConstants.TABLE_SEARCH_FLAGS + " SET " +
    			DBConstants.FIELD_SEARCH_FLAGS_CHECKED_BY + " = 1 WHERE " +
    			DBConstants.FIELD_SEARCH_FLAGS_ID + " in ( ";
			
			String[] listElements1 = searchList.split(",");
        	
        	for(int i=1;i<=listElements1.length;i++) {
        		if(i!=listElements1.length) sql += " ?, ";
        		else sql += " ? ";
        	}
        	
        	sql +=" )";
        	
			sjt.update(sql,(Object[]) searchList.split(","));
			
        	return Boolean.TRUE;
        	
	    }catch(RuntimeException e){ 
	    	// failure - rollback
	    	e.printStackTrace();
	    	status.setRollbackOnly();    		    	
	    	return Boolean.FALSE;
	    }
	}

}
