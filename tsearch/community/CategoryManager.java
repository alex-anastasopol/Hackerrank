/**
 * @(#) CommunityAdmin.java 1.3 12/14/2000
 *
 * Copyright 1999-2000 CornerSoft Technologies SRL.All rights reserved.
 *
 * This software is proprietary information of CornerSoft Technologies.
 * Use is subject to license terms.
 */
package ro.cst.tsearch.community;

import java.math.BigDecimal;
import java.sql.PreparedStatement;
import java.sql.SQLException;

import ro.cst.tsearch.data.DataException;
import ro.cst.tsearch.database.ConnectionPool;
import ro.cst.tsearch.database.DBConnection;
import ro.cst.tsearch.database.DBManager;
import ro.cst.tsearch.database.DatabaseData;
import ro.cst.tsearch.exceptions.BaseException;
import ro.cst.tsearch.utils.DBConstants;

/**
 * CategoryManager holds a collection of utilities to manage all 
 * datas related to categories.
 */
public class CategoryManager {
    

    /**
     * Create a new category, along with associated attributes. 
     * @param ca The category object which have to be added.
     */
    public static CategoryAttributes createNewCategory(CategoryAttributes ca/*, String limits*/)
        throws BaseException,DataException {
        
        

        String stm = "INSERT INTO " + DBConstants.TABLE_CATEGORY 
        										  + " (CATEG_NAME, TIMESTAMP) "
        										  + " VALUES( ?, null )";
      
        DBConnection conn = null;
        try{   
            conn = ConnectionPool.getInstance().requestConnection();
            PreparedStatement pstmt =conn.prepareStatement( stm );
            
            pstmt.setString( 1 ,  ca.getAttribute(CategoryAttributes.NAME).toString() );
                        
			pstmt.executeUpdate();			
			conn.commit();
			pstmt.close();
			
            long id = DBManager.getLastId(conn);
            ca.setAttribute(CategoryAttributes.ID, new Long(id));
            
        }catch(Exception e){
            throw new BaseException("SQLException:" + e.getMessage());
        }finally{
			try{
			    ConnectionPool.getInstance().releaseConnection(conn);
			}catch(BaseException e){
			    throw new BaseException("SQLException:" + e.getMessage());
			}			
		}
        
        return ca;
    }
        
    /**
     * Delete a category from the DataBase. 
     * @param username The name of the category which have to be deleted.
     */
    public static void delete(String categId)
        throws BaseException {

        String stm = "DELETE FROM " + DBConstants.TABLE_CATEGORY + " WHERE "
            + CategoryAttributes.CATEGORY_ID + " = ? ";
        
		try {
			DBManager.getSimpleTemplate().update(stm,categId);
		} catch (Exception e) {
			throw new BaseException("SQLException:" + e.getMessage());
		}

    }

    /**
     * Delete a category from the DataBase. 
     * @param id The identifier of the category which have to be deleted.
     */
    public static void delete(long id)
        throws BaseException {

        String stm = "DELETE FROM " + DBConstants.TABLE_CATEGORY + " WHERE "
            		   + CategoryAttributes.CATEGORY_ID + "=" + id;
        
        DBConnection conn = null;
        try{
            
            conn = ConnectionPool.getInstance().requestConnection();            
            conn.executeSQL(stm);
            
        }catch(Exception e){
            throw new BaseException("SQLException:" + e.getMessage());
        }finally{
			try{
			    ConnectionPool.getInstance().releaseConnection(conn);
			}catch(BaseException e){
			    throw new BaseException("SQLException:" + e.getMessage());
			}			
		}

    }
	/** Retrieves the number of communities object based on it's identifier.
	 * @param id The identifier of the category.
	 */
	public static int getNrComm(long id)
		throws BaseException {

		String stm = "SELECT "+ CommunityAttributes.COMMUNITY_ID 		
						+ " FROM " 
						+ DBConstants.TABLE_COMMUNITY 
						+ " WHERE "
						+ CommunityAttributes.COMMUNITY_CATEGORY + "=" + id;
        
		DBConnection conn = null;
        try{
            
            conn = ConnectionPool.getInstance().requestConnection();
			DatabaseData result = conn.executeSQL(stm);
			return result.getRowNumber();
			
		}catch(Exception e){			
			throw new BaseException("SQLException:" + e.getMessage());
		}finally{
			try{
			    ConnectionPool.getInstance().releaseConnection(conn);
			}catch(BaseException e){
			    throw new BaseException("SQLException:" + e.getMessage());
			}			
		}
		
		
	}

    /** Retrieves a category object based on it's identifier.
     * @param id The identifier of the category.
     */
    public static CategoryAttributes getCategory(long id)
        throws BaseException {

        String stm = "SELECT "+ CategoryAttributes.CATEGORY_NAME + ","+ CategoryAttributes.CATEGORY_ID
            + " FROM " + DBConstants.TABLE_CATEGORY + " WHERE "
            + CategoryAttributes.CATEGORY_ID + "=" + id;
        
        DBConnection conn = null;
        try{
            conn = ConnectionPool.getInstance().requestConnection();
            DatabaseData result = conn.executeSQL(stm);
            CategoryAttributes ca = new CategoryAttributes(result, 0);
            return ca;
        }catch(Exception e){
            throw new BaseException("SQLException:" + e.getMessage());
        }finally{
			try{
			    ConnectionPool.getInstance().releaseConnection(conn);
			}catch(BaseException e){
			    throw new BaseException("SQLException:" + e.getMessage());
			}			
		}

    }

    /** Retrieves a category object based on it's name.
     * @param name The name of the category.
     */
    public static CategoryAttributes getCategory(String name)
        throws BaseException {

        String stm = "SELECT "
            + CategoryAttributes.CATEGORY_NAME + ","
            + CategoryAttributes.CATEGORY_ID
            + " FROM " + DBConstants.TABLE_CATEGORY 
            + " WHERE "
            + CategoryAttributes.CATEGORY_NAME + "= ? ";

        DBConnection conn = null;
		
        try{
            conn = ConnectionPool.getInstance().requestConnection();
            PreparedStatement pstmt =conn.prepareStatement( stm );
            
            pstmt.setString( 1 ,  name );
			DatabaseData result = conn.executePrepQuery(pstmt);	
			CategoryAttributes ca = new CategoryAttributes(result, 0);
    		pstmt.close();
            return ca;
            
        }catch(Exception e){
            throw new BaseException("SQLException:" + e.getMessage());
        }finally{
			try{
			    ConnectionPool.getInstance().releaseConnection(conn);
			}catch(BaseException e){
			    throw new BaseException("SQLException:" + e.getMessage());
			}			
		}
    }
	public static CategoryAttributes getCategory(BigDecimal id)
		throws BaseException {

		String stm = "SELECT "
			+ CategoryAttributes.CATEGORY_NAME + ","
			+ CategoryAttributes.CATEGORY_ID
			+ " FROM " + DBConstants.TABLE_CATEGORY 
			+ " WHERE "
			+ CategoryAttributes.CATEGORY_ID + "=" + id ;
		
		DBConnection conn = null;
		try{
		    conn = ConnectionPool.getInstance().requestConnection();
			DatabaseData result = conn.executeSQL(stm);
			CategoryAttributes ca = new CategoryAttributes(result, 0);
			return ca;
		}catch(Exception e){
			throw new BaseException("SQLException:" + e.getMessage());
		}finally{
			try{
			    ConnectionPool.getInstance().releaseConnection(conn);
			}catch(BaseException e){
			    throw new BaseException("SQLException:" + e.getMessage());
			}			
		}
	}

    /** Retrieves an array of category objects.
     * @param sorted The sort criteria.
     */
    public static CategoryAttributes[] getCategories(String criteria)
        throws BaseException {

        String stm = "SELECT "
            + CategoryAttributes.CATEGORY_NAME + ","
            + CategoryAttributes.CATEGORY_ID
            + " FROM " + DBConstants.TABLE_CATEGORY + " ORDER BY ? ";
        
        DBConnection conn = null;
		try{
		    conn = ConnectionPool.getInstance().requestConnection();
	        PreparedStatement pstmt =conn.prepareStatement( stm );
	            
	        pstmt.setString( 1 ,  criteria );
			DatabaseData result = conn.executePrepQuery(pstmt);	
            int resultRows = result.getRowNumber();
            
            CategoryAttributes[] cas = new CategoryAttributes[resultRows];
            for (int i=0; i<resultRows; i++) {
                cas[i] = new CategoryAttributes(result, i);
            }
            
            pstmt.close();
            return cas;
        }catch(Exception e){
            throw new BaseException("SQLException:" + e.getMessage());
        }finally{
			try{
			    ConnectionPool.getInstance().releaseConnection(conn);
			}catch(BaseException e){
			    throw new BaseException("SQLException:" + e.getMessage());
			}			
		}
    }
}










