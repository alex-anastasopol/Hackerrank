
package ro.cst.tsearch.user;

import java.util.ArrayList;
import java.util.Arrays;

import org.apache.log4j.Logger;
import org.springframework.jdbc.core.simple.SimpleJdbcTemplate;

import ro.cst.tsearch.database.ConnectionPool;
import ro.cst.tsearch.database.DBConnection;
import ro.cst.tsearch.database.DBManager;
import ro.cst.tsearch.database.DatabaseData;
import ro.cst.tsearch.exceptions.BaseException;
import ro.cst.tsearch.utils.DBConstants;

/**
 *  
 */
public class GroupUtils {

	private static final Logger logger = Logger.getLogger(GroupUtils.class);
	
    public static GroupAttributes createGroup(GroupAttributes ga) throws BaseException {

        return GroupManager.createNewGroup(ga);
    }

    public static GroupAttributes getGroup(String name) throws BaseException {

        return GroupManager.getGroup(name);
    }

    public static GroupAttributes getGroup(long id) throws BaseException {

        return GroupManager.getGroup(id);
    }

    public static GroupAttributes[] getGroupsWithoutTSAdmin() throws BaseException {
        
        String stm = "SELECT * FROM " + DBConstants.TABLE_GROUP + " WHERE " + GroupAttributes.GROUP_ID + "!="
                + GroupAttributes.TA_ID;

        DBConnection conn = null;
        GroupAttributes[] gas;
        try {
            
            conn = ConnectionPool.getInstance().requestConnection();
	        DatabaseData result = conn.executeSQL(stm);
	        
	        int resultRows = result.getRowNumber();
	        gas = new GroupAttributes[resultRows];
	
	        for (int i = 0; i < resultRows; i++)
	            gas[i] = new GroupAttributes(result, i);
	        
        }catch(Exception e){
            throw new BaseException("SQLException:" + e.getMessage());
        }finally{
			try{
			    ConnectionPool.getInstance().releaseConnection(conn);
			}catch(BaseException e){
			    throw new BaseException("SQLException:" + e.getMessage());
			}			
		}
        Arrays.sort(gas);
        return gas;
    }
    
    /**
     * get groups with lesser or equal importance than the user given as parameter 
     */
    public static GroupAttributes[] getGroups(UserAttributes ua) throws BaseException {
        
        String stm = "SELECT * FROM " + DBConstants.TABLE_GROUP;

        DBConnection conn = null;
        GroupAttributes[] gas;
        try {
            
            conn = ConnectionPool.getInstance().requestConnection();
	        DatabaseData result = conn.executeSQL(stm);
	        
	        ArrayList<GroupAttributes> list = new ArrayList<GroupAttributes>();
	        for (int i = 0; i < result.getRowNumber(); i++) {
	        	GroupAttributes ga = new GroupAttributes(result, i);
	        	if (ga.compareTo(ua)<=0) {
	        		list.add(ga);
	        	}
	        }
	        
	        gas = list.toArray(new GroupAttributes[list.size()]);
	        
	        
        }catch(Exception e){
            throw new BaseException("SQLException:" + e.getMessage());
        }finally{
			try{
			    ConnectionPool.getInstance().releaseConnection(conn);
			}catch(BaseException e){
			    throw new BaseException("SQLException:" + e.getMessage());
			}			
		}
        Arrays.sort(gas);
        return gas;
    }
    
    public static final String SQL_GET_GROUPS_FOR_COMMADMIN = "SELECT * FROM " + DBConstants.TABLE_GROUP + " WHERE " + 
    						GroupAttributes.GROUP_ID + " = ? and " + GroupAttributes.GROUP_ID + " = ? and " + GroupAttributes.GROUP_ID + " = ?";
    
    public static GroupAttributes[] getGroupsForCommAdmin(boolean isOnlyCommAdmin, boolean isTSAdmin) throws BaseException {
    	
    	DBConnection conn = null;
        GroupAttributes[] gas;
        String stm = "SELECT * FROM " + DBConstants.TABLE_GROUP + " WHERE " + GroupAttributes.GROUP_ID + " != " + GroupAttributes.AM_ID; 
		
        if (isOnlyCommAdmin){
			stm += " and " + GroupAttributes.GROUP_ID + " != " + GroupAttributes.CCA_ID;
        }
		if (!isTSAdmin){
			stm += " and " + GroupAttributes.GROUP_ID + " != " + GroupAttributes.TA_ID;
		}
		
        try {
            
            conn = ConnectionPool.getInstance().requestConnection();
	        DatabaseData result = conn.executeSQL(stm);
	        
	        int resultRows = result.getRowNumber();
	        gas = new GroupAttributes[resultRows];
	
	        for (int i = 0; i < resultRows; i++)
	            gas[i] = new GroupAttributes(result, i);
	        
        }catch(Exception e){
            throw new BaseException("SQLException:" + e.getMessage());
        }finally{
			try{
			    ConnectionPool.getInstance().releaseConnection(conn);
			}catch(BaseException e){
			    throw new BaseException("SQLException:" + e.getMessage());
			}			
		}
        Arrays.sort(gas);
        return gas;
		
    }

    /**
     * return the name for the all groups
     * 
     * @param an
     *            array with all group objects
     */
    public static String[] getGroupsNameWithoutTSAdmin() throws BaseException {
        
        String stm = "SELECT " + GroupAttributes.GROUP_NAME + " FROM " + DBConstants.TABLE_GROUP + " WHERE "
                + GroupAttributes.GROUP_ID + " != " + GroupAttributes.TA_ID;
        
        DBConnection conn = null;
        String[] nameGroup;
        try {
	    
            conn = ConnectionPool.getInstance().requestConnection();
            DatabaseData result = conn.executeSQL(stm);
            
	        int resultRows = result.getRowNumber();
	        nameGroup = new String[resultRows];
	        
	        for (int i = 0; i < resultRows; i++)
	            nameGroup[i] = (String) result.getValue(GroupAttributes.GROUP_NAME, i);
	        
        }catch(Exception e){
            throw new BaseException("SQLException:" + e.getMessage());
        }finally{
			try{
			    ConnectionPool.getInstance().releaseConnection(conn);
			}catch(BaseException e){
			    throw new BaseException("SQLException:" + e.getMessage());
			}			
		}
        
        return nameGroup;

    }

    /**
     * get the Groups ID without the gid of VPOAdmin
     * 
     *  
     */

    /*
     * public static int [] getGroupIDsWithoutVPOAdmin() throws BaseException{ DBConnection connection =
     * InstanceManager.getCurrentInstance().getDBConnection(); String stm = "SELECT "+ DataConstants.GROUP_ID + "
     * FROM " + DataConstants.TABLE_GROUP + " WHERE " + DataConstants.GROUP_ID + " != " +
     * DataConstants.USER_VPOADMIN_ID; DatabaseData result = connection.executeSQL(stm); int resultRows =
     * result.getRowNumber(); int [] groupID = new int [resultRows]; for(int i=0; i <resultRows; i++)
     * groupID[i] = ((BigDecimal)result.getValue(DataConstants.GROUP_ID, i)).intValue (); return groupID;
     *  }
     */

    public static void deleteGroup(GroupAttributes aba) throws BaseException {

        GroupManager.delete((String) aba.getAttribute(GroupAttributes.NAME));
    }

    public static boolean groupEquals(GroupAttributes ga1, GroupAttributes ga2) {
        if (ga1 == null) {
            if (ga2 == null) {
                return true;
            } else {
                return false;
            }
        } else {
            return ga1.equals(ga2);
        }
    }

    public static String getGroupShortTitle(UserAttributes ua) {
        String ret = GroupAttributes.ABS_NICK;
        switch (ua.getGROUP().intValue()) {
        case GroupAttributes.CA_ID:
            ret = GroupAttributes.CA_NICK;
            break;
        case GroupAttributes.AM_ID:
            ret = GroupAttributes.AM_NICK;
            break;
        case GroupAttributes.ABS_ID:
            ret = GroupAttributes.ABS_NICK;
            break;
        case GroupAttributes.AG_ID:
            ret = GroupAttributes.AG_NICK;
            break;
        case GroupAttributes.CCA_ID:
            ret = GroupAttributes.CCA_NICK;
            break;    
        case GroupAttributes.TA_ID:
            ret = GroupAttributes.TA_NICK;
            break;
        }
        return ret;
    }
    
    public static String getGroupTitle(UserAttributes ua) {
        String ret = GroupAttributes.ABS_NAME;
        switch (ua.getGROUP().intValue()) {
        case GroupAttributes.CA_ID:
            ret = GroupAttributes.CA_NAME;
            break;
        case GroupAttributes.AM_ID:
            ret = GroupAttributes.AM_NAME;
            break;
        case GroupAttributes.ABS_ID:
            ret = GroupAttributes.ABS_NAME;
            break;
        case GroupAttributes.AG_ID:
            ret = GroupAttributes.AG_NAME;
            break;
        case GroupAttributes.CCA_ID:
            ret = GroupAttributes.CCA_NAME;
            break;    
        case GroupAttributes.TA_ID:
            ret = GroupAttributes.TA_NAME;
            break;
        }
        return ret;
    }
    
    public static int compare(UserAttributes ua1, UserAttributes ua2) {
		return GroupAttributes.SORT_ORDER.get(ua1.getGROUP().intValue()).compareTo(GroupAttributes.SORT_ORDER.get(ua2.getGROUP().intValue()));
	}
   
}