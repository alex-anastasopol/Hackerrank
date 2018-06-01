package ro.cst.tsearch.database.transactions;

import static ro.cst.tsearch.utils.DBConstants.TABLE_SEARCH;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;
import org.springframework.jdbc.core.simple.SimpleJdbcTemplate;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionCallback;

import ro.cst.tsearch.Search;
import ro.cst.tsearch.database.ConnectionPool;
import ro.cst.tsearch.user.UserAttributes;
import ro.cst.tsearch.user.UserManager;
import ro.cst.tsearch.utils.DBConstants;
import ro.cst.tsearch.utils.FileLogger;
import ro.cst.tsearch.utils.InstanceManager;

public class SetSearchOwnerTransaction implements TransactionCallback {

	private static final Logger logger = Logger.getLogger(SetSearchOwnerTransaction.class);
	
	private long searchId;
	private long userId;
	private long abstractorId;
	private Long secondaryAbstractor;
	private boolean locked;
	
	public SetSearchOwnerTransaction(long searchId, long userId, long abstractorId, boolean locked) {
		super();
		this.searchId = searchId;
		this.userId = userId;
		this.abstractorId = abstractorId;
		this.locked = locked;
	}
	
	public SetSearchOwnerTransaction(Search search, long userId, long abstractorId, boolean locked) {
		super();
		this.searchId = search.getID();
		this.userId = userId;
		this.abstractorId = abstractorId;
		this.locked = locked;
		this.secondaryAbstractor = search.getSecondaryAbstractorId();
	}

	public long getAbstractorId() {
		return abstractorId;
	}

	public void setAbstractorId(long abstractorId) {
		this.abstractorId = abstractorId;
	}

	public boolean isLocked() {
		return locked;
	}

	public void setLocked(boolean locked) {
		this.locked = locked;
	}

	public long getSearchId() {
		return searchId;
	}

	public void setSearchId(long searchId) {
		this.searchId = searchId;
	}

	public long getUserId() {
		return userId;
	}

	public void setUserId(long userId) {
		this.userId = userId;
	}

	public Object doInTransaction(TransactionStatus status) {
        try {
        	SimpleJdbcTemplate sjt = ConnectionPool.getInstance().getSimpleTemplate();
        	
            /*
             * Ca abstractor se seteaza cel care a checkat searchul
             * 
             * daca userId <= 0, nu se face ssetarea asta, deoarece idurile coerspund unor stari ale searchului
             * 
             * */
            
            String sql = "SELECT CHECKED_BY, ABSTRACT_ID FROM "+ TABLE_SEARCH + " a JOIN " + 
            	DBConstants.TABLE_SEARCH_FLAGS + " b ON a." +
            	DBConstants.FIELD_SEARCH_ID + " = b." + 
            	DBConstants.FIELD_SEARCH_FLAGS_ID + " WHERE a.ID = " + searchId;

            FileLogger.info( " Get status and search owner for search ID = " + searchId, FileLogger.SEARCH_OWNER_LOG );
            
            List<Map<String, Object>> allData = sjt.queryForList(sql);
            Map<String, Object> data = new HashMap<String, Object>();
            if(allData.size()>=1)
            	data = allData.get(0);
            
            long checkedById = -1;
            long ownerId = -1;

            if (data.get("CHECKED_BY") != null) {
                checkedById = (Integer)data.get("CHECKED_BY");
                if(checkedById>0)
                	checkedById = 1;
            }
            
            if (data.get("ABSTRACT_ID") != null) {
                ownerId = ((BigInteger)data.get("ABSTRACT_ID")).longValue();
            }
            
            FileLogger.info( " CHECKED_BY = " + checkedById + "\n ABSTRACT_ID = " + ownerId + "\n\n", FileLogger.SEARCH_OWNER_LOG );
           
            String sqlFlags = null;
            sql = null;
            
            boolean validQuery = false;
            
            if (abstractorId > 0)
            {
                FileLogger.info( " abstractorId > 0", FileLogger.SEARCH_OWNER_LOG );
                sqlFlags = "UPDATE "+ DBConstants.TABLE_SEARCH_FLAGS +" SET CHECKED_BY = ";
                if (locked)
                	sqlFlags += (userId > 0 ? 1 : userId );
                else
                	sqlFlags += (userId > 0 ? 0 : userId );
                sqlFlags += " WHERE " + DBConstants.FIELD_SEARCH_FLAGS_ID + " = " + searchId;
                
                sql = "UPDATE " + DBConstants.TABLE_SEARCH + " SET ABSTRACT_ID=" + abstractorId;
                if(secondaryAbstractor != null) {
                	sql += ", " + DBConstants.FIELD_SEARCH_SEC_ABSTRACT_ID + " = " + secondaryAbstractor ;
                }
                sql += " WHERE ID=" + searchId;
                
                if( checkedById == 0 || abstractorId == ownerId )
                {
                    FileLogger.info( "checkedById == 0 || abstractorId == ownerId --> query( " + sql + "/" + sqlFlags + " ) valid", 
                    		FileLogger.SEARCH_OWNER_LOG );
                    validQuery = true;
                }
                else
                {
                    FileLogger.info( " checkedById != 0 && abstractorId != ownerId ", FileLogger.SEARCH_OWNER_LOG );
                    UserAttributes user = UserManager.getUser( new BigDecimal(ownerId) );
                    
                    FileLogger.info( " Get User with id = " + ownerId, FileLogger.SEARCH_OWNER_LOG );
                    
                    InstanceManager.getManager().getCurrentInstance(searchId).setCurrentUser(user);
                    
                    FileLogger.info( " setCurrentUser() to user id = " + ownerId, FileLogger.SEARCH_OWNER_LOG );
                    
                    sql = "UPDATE " + DBConstants.TABLE_SEARCH + " SET ABSTRACT_ID = " + ownerId;
                    if(secondaryAbstractor != null) {
                    	sql += ", " + DBConstants.FIELD_SEARCH_SEC_ABSTRACT_ID + " = " + secondaryAbstractor ;
                    }
                    sql += " WHERE ID=" + searchId;
                    
                    
                    FileLogger.info( "query( " + sql + "/" + sqlFlags + " ) --> valid", FileLogger.SEARCH_OWNER_LOG  );
                    validQuery = true;
                }
                
            }
            else
            {
                if( userId > 0 )
                {
                	sqlFlags = "UPDATE "+ DBConstants.TABLE_SEARCH_FLAGS +" SET CHECKED_BY = ";
                    if (locked)
                    	sqlFlags += " 1 ";
                    else
                    	sqlFlags += " 0 ";
                    sqlFlags += " WHERE " + DBConstants.FIELD_SEARCH_FLAGS_ID + " = " + searchId;
                    
                    FileLogger.info( " userId > 0", FileLogger.SEARCH_OWNER_LOG );

                    sql = "UPDATE " + DBConstants.TABLE_SEARCH + " SET ABSTRACT_ID = " + userId;
                    if(secondaryAbstractor != null) {
                    	sql += ", " + DBConstants.FIELD_SEARCH_SEC_ABSTRACT_ID + " = " + secondaryAbstractor ;
                    }
                    sql += " WHERE ID=" + searchId;

                    if( checkedById == 0 || userId == ownerId )
                    {
                        FileLogger.info( " checkedById == 0 || userId == ownerId --> query( " + sql + "/" + sqlFlags + " ) valid", FileLogger.SEARCH_OWNER_LOG );
                        validQuery = true;
                    } 
                    else
                    {
                        FileLogger.info( " checkedById != 0 && userId != ownerId", FileLogger.SEARCH_OWNER_LOG );
                        FileLogger.info( " Get User with id = " + ownerId, FileLogger.SEARCH_OWNER_LOG );
                        UserAttributes user = UserManager.getUser( new BigDecimal(ownerId) );
                        
                        FileLogger.info( " setCurrentUser() to user id = " + ownerId, FileLogger.SEARCH_OWNER_LOG );
                        
                        InstanceManager.getManager().getCurrentInstance(searchId).setCurrentUser(user);
                        
                        sql = "UPDATE " + DBConstants.TABLE_SEARCH + " SET ABSTRACT_ID=" + ownerId;
                        if(secondaryAbstractor != null) {
                        	sql += ", " + DBConstants.FIELD_SEARCH_SEC_ABSTRACT_ID + " = " + secondaryAbstractor ;
                        }
                        sql += " WHERE ID=" + searchId;
                        
                        FileLogger.info( "query( " + sql + "/" + sqlFlags + " ) --> valid", FileLogger.SEARCH_OWNER_LOG );
                        
                        validQuery = true;
                    }
                }
                else
                {
                    FileLogger.info( " userId <= 0 && abstractorId <= 0", FileLogger.SEARCH_OWNER_LOG );
                    
                    sqlFlags = "UPDATE "+ DBConstants.TABLE_SEARCH_FLAGS +" SET CHECKED_BY = ";
                    if (locked)
                    	sqlFlags += userId;
                    else
                    	sqlFlags += " 0 ";
                    sqlFlags += " WHERE " + DBConstants.FIELD_SEARCH_FLAGS_ID + " = " + searchId;
                    
                    FileLogger.info( "query( " + sqlFlags + " ) --> valid", FileLogger.SEARCH_OWNER_LOG );
                    
                    validQuery = true;
                }
            }

            if( validQuery )
            {
                logger.debug("setSearchOwner / VALID :  " + sql + "/" + sqlFlags);
                if(sql!=null)
                	sjt.update(sql);
                if(sqlFlags!=null)
                	sjt.update(sqlFlags);
                
            }
            else
            {
                logger.debug("setSearchOwner / INVALID :  " + sql + "/" + sqlFlags);
                logger.debug("currentUser : " + 
                    InstanceManager.getManager().getCurrentInstance(searchId).getCurrentUser().getID().longValue());
            }

        } catch (Exception e) {
            e.printStackTrace();
            status.setRollbackOnly();
            return false;
        } 
		return true;
	}

}
