package ro.cst.tsearch.database.transactions;

import org.springframework.jdbc.core.simple.SimpleJdbcTemplate;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionCallback;

import ro.cst.tsearch.Search;
import ro.cst.tsearch.bean.SearchAttributes;
import ro.cst.tsearch.database.ConnectionPool;
import ro.cst.tsearch.user.UserInfo;

/**
 * Transaction that saves the state of some attributes (state, county, start date) to be used with the next new search
 *
 */
public class UserInfoTransaction implements TransactionCallback {

	private Search search = null;
	
	public UserInfoTransaction(Search search){
		this.search = search;
	}
	
	@Override
	public Object doInTransaction(TransactionStatus status) {
		
		SimpleJdbcTemplate sjt = ConnectionPool.getInstance().getSimpleTemplate();
		
		try {
			
			int userId = -1;
			if (search.getAgent() == null
					|| search.getAgent().getID() == null)
			{
				if (search.getSa().getAbstractorObject() == null)
				{
					return true;
				}
				else
				{
					try 
					{
						userId = search.getSa().getAbstractorObject().getID().intValue();
					} 
					catch (Exception e)
					{
						return true;
					}
				}
			} 
			else
			{
				try
				{
					userId = search.getAgent().getID().intValue();
				}
				catch (Exception e) 
				{
					return true;
				}
			}
			
			SearchAttributes sa = search.getSa();
			String county = sa.getAtribute(SearchAttributes.P_COUNTY);
			String state = sa.getAtribute(SearchAttributes.P_STATE);
			String startDate = sa.getAtribute(SearchAttributes.FROMDATE);
			
			String stmDelete = " delete from " + 
				UserInfo.DB_SETTINGS_TABLE + " where " +
				UserInfo.DB_USER_ID_FIELD + " = " + userId + " AND " + 
				UserInfo.DB_MODULE_FIELD + "<>'" + UserInfo.DB_MODULE_SETTINGS + "'";
			
			String insertCounty = "insert into " +
				UserInfo.DB_SETTINGS_TABLE + " (" + 
				UserInfo.DB_USER_ID_FIELD + ", " + UserInfo.DB_ATTRIBUTE_FIELD + ", " + 
				UserInfo.DB_MODULE_FIELD + ", " + UserInfo.DB_VALUE_FIELD + ") " +
				" VALUES ( " + userId + ", '" + 
				UserInfo.DB_LAST_COUNTY + "', '" + 
				UserInfo.DB_MODULE_SEARCH + "', ?)";
			
			String insertState = "insert into " +
				UserInfo.DB_SETTINGS_TABLE + " (" + 
				UserInfo.DB_USER_ID_FIELD + ", " + UserInfo.DB_ATTRIBUTE_FIELD + ", " + 
				UserInfo.DB_MODULE_FIELD + ", " + UserInfo.DB_VALUE_FIELD + ") " +
				" VALUES ( " + userId + ", '" + 
				UserInfo.DB_LAST_STATE + "', '" + 
				UserInfo.DB_MODULE_SEARCH + "', ?)";
			
			String insertDate = "insert into " +
				UserInfo.DB_SETTINGS_TABLE + " (" + 
				UserInfo.DB_USER_ID_FIELD + ", " + UserInfo.DB_ATTRIBUTE_FIELD + ", " + 
				UserInfo.DB_MODULE_FIELD + ", " + UserInfo.DB_VALUE_FIELD + ") " +
				" VALUES ( " + userId + ", '" + 
				UserInfo.DB_LAST_START_DATE + "', '" + 
				UserInfo.DB_MODULE_SEARCH + "', ?)";
							
							
			
			sjt.update(stmDelete);
			sjt.update(insertCounty,county);
			sjt.update(insertState,state);
			
			if ( !sa.getAtribute(SearchAttributes.SEARCHUPDATE).trim().toLowerCase().equals("true") ) 
				// on Update we do not save the date
				sjt.update(insertDate,startDate);
				
			
		} catch (Exception e) {
			e.printStackTrace();
			status.setRollbackOnly();
			return false;
		}
		
		return true;
	}

}
