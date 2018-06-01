package ro.cst.tsearch.user;

import java.math.BigDecimal;
import java.util.Hashtable;

import ro.cst.tsearch.database.ConnectionPool;
import ro.cst.tsearch.database.DBConnection;
import ro.cst.tsearch.database.DatabaseData;
import ro.cst.tsearch.exceptions.BaseException;


public class UserRights {

	private static Hashtable communityRights = new Hashtable();
	private static Hashtable projectRights = new Hashtable();
	private static Hashtable systemRights = new Hashtable();

	public static long getCommunityRights(long userId, long commId) {
		
	    String key = "" + userId + "__" + commId;

		if (!communityRights.containsKey(key)) {
			
			String qry =
				"SELECT gid FROM vpo_user_communnity WHERE user_id = "
					+ userId
					+ " AND comm_id = "
					+ commId;
			
			DBConnection conn = null;
			try {
			    
			    conn = ConnectionPool.getInstance().requestConnection();
				DatabaseData result = conn.executeSQL(qry);
				
				long gid = 0;
				for (int i = 0; i < result.getRowNumber(); i++)
					gid |= ((BigDecimal) result.getValue("GID", i)).longValue();
				
				communityRights.put(key, new Long(gid));
				
			} catch (Exception ex) {
				ex.printStackTrace();
			} finally{
				try{
				    ConnectionPool.getInstance().releaseConnection(conn);
				}catch(BaseException e){
				    e.printStackTrace();
				}			
			}
		}

		return ((Long) communityRights.get(key)).longValue();
	}

	public static void resetCommunityRights(long userId, long commId) {
		String key = "" + userId + "__" + commId;
		//Log.debug("(CR) - reset key : " + key);
		communityRights.remove(key);
	}

	public static long getSystemRights(long userId) {
		String key = "" + userId;

		if (!systemRights.containsKey(key)) {			
			
			String qry =
				"SELECT gid FROM vpo_user_group WHERE user_id = " + userId;

			DBConnection conn = null;
			try {
			    
			    conn = ConnectionPool.getInstance().requestConnection();
				DatabaseData result = conn.executeSQL(qry);
				
				long gid = 0;
				for (int i = 0; i < result.getRowNumber(); i++)
					gid |= ((BigDecimal) result.getValue("GID", i)).longValue();
				systemRights.put(key, new Long(gid));
				
			} catch (Exception ex) {
				ex.printStackTrace();
			} finally{
				try{
				    ConnectionPool.getInstance().releaseConnection(conn);
				}catch(BaseException e){
				    e.printStackTrace();
				}			
			}
		}

		return ((Long) systemRights.get(key)).longValue();
	}

	public static void resetSystemRights(long userId) {
		String key = "" + userId;
		//Log.debug("(SR) - reset key : " + key);
		systemRights.remove(key);
	}
}
