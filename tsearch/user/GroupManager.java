package ro.cst.tsearch.user;

import java.sql.PreparedStatement;

import ro.cst.tsearch.database.ConnectionPool;
import ro.cst.tsearch.database.DBConnection;
import ro.cst.tsearch.database.DBManager;
import ro.cst.tsearch.database.DatabaseData;
import ro.cst.tsearch.exceptions.BaseException;
import ro.cst.tsearch.utils.DBConstants;

public class GroupManager{
        
    /**  
     * Bind a name to a group and store the information on LDAP server. 
     * The dn must de created as follow:
     * ga.getDN() + name
     * The name is the name of the context where the object will be bound.
     * Parameters:
     *  ga - the group to bind; possibly null
     *  dn - the name to bind; may not be empty
     */
    public static GroupAttributes createNewGroup(GroupAttributes ga)
        throws BaseException {
        
        String stm = "INSERT INTO " + DBConstants.TABLE_GROUP + " VALUES(?,?,null)";

		try {
			DBManager.getSimpleTemplate().update(stm,ga.getAttribute(GroupAttributes.ID),ga.getAttribute(GroupAttributes.NAME));
		} catch (Exception e) {
			throw new BaseException("SQLException:" + e.getMessage());
		}

        return ga;
    }
        
    /**
     *
     */
    public static void delete(String name)
        throws BaseException {
                     
        String stm = "DELETE FROM " + DBConstants.TABLE_GROUP 
            + " WHERE "
            + GroupAttributes.GROUP_NAME + "= ?";
        
		try {
			DBManager.getSimpleTemplate().update(stm,name);
		} catch (Exception e) {
			throw new BaseException("SQLException:" + e.getMessage());
		}
        
    }

    /**
     *
     */
    public static void delete(long id)
        throws BaseException {
                     
        String stm = "DELETE FROM " + DBConstants.TABLE_GROUP 
            + " WHERE "
            + GroupAttributes.GROUP_ID + "=" + id;

        DBConnection conn = null;        
        try {
            
            conn = ConnectionPool.getInstance().requestConnection();
            DatabaseData result = conn.executeSQL(stm);
            
        } catch(Exception e){
            throw new BaseException("SQLException:" + e.getMessage());
        } finally {
			try{
			    ConnectionPool.getInstance().releaseConnection(conn);
			}catch(BaseException e){
			    throw new BaseException("SQLException:" + e.getMessage());
			}			
		}
    }

    /**
     * 
     */
    public static GroupAttributes getGroup(long id)
        throws BaseException {
        
        String stm = "SELECT * FROM " + DBConstants.TABLE_GROUP 
            + " WHERE "
            + GroupAttributes.GROUP_ID + "=" + id;
        
        GroupAttributes ga;
        DBConnection conn = null;        
        try {
            
            conn = ConnectionPool.getInstance().requestConnection();
            DatabaseData result = conn.executeSQL(stm);
            ga = new GroupAttributes(result, 0);
            
        } catch(Exception e){
            throw new BaseException("SQLException:" + e.getMessage());
        } finally {
			try{
			    ConnectionPool.getInstance().releaseConnection(conn);
			}catch(BaseException e){
			    throw new BaseException("SQLException:" + e.getMessage());
			}			
		}
                
        return ga;
    }

    /**
     * 
     */
    public static GroupAttributes getGroup(String name)
        throws BaseException {
       
        String stm = "SELECT * FROM " + DBConstants.TABLE_GROUP 
            + " WHERE "
            + GroupAttributes.GROUP_NAME + "=?";
        
        GroupAttributes ga;
        DBConnection conn = null;        
        try {
            
            conn = ConnectionPool.getInstance().requestConnection();
    		PreparedStatement pstmt = conn.prepareStatement( stm );
    		pstmt.setString( 1, name);
    		DatabaseData result = conn.executePrepQuery(pstmt);	
    		pstmt.close();	
    		
            ga = new GroupAttributes(result, 0);
            
        } catch(Exception e){
            throw new BaseException("SQLException:" + e.getMessage());
        } finally {
			try{
			    ConnectionPool.getInstance().releaseConnection(conn);
			}catch(BaseException e){
			    throw new BaseException("SQLException:" + e.getMessage());
			}			
		}
                
        return ga;
    }


}                                                                                                     




