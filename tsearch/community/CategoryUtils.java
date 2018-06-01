/**
 * @(#) DocumentUtils.java 1.3 12/4/2000
 *
 * Copyright 1999-2000 CornerSoft Technologies SRL.All rights reserved.
 *
 * This software is proprietary information of CornerSoft Technologies.
 * Use is subject to license terms. 
 */
package ro.cst.tsearch.community;
import java.sql.PreparedStatement;

import org.apache.log4j.Logger;

import ro.cst.tsearch.data.DataException;
import ro.cst.tsearch.database.ConnectionPool;
import ro.cst.tsearch.database.DBConnection;
import ro.cst.tsearch.database.DatabaseData;
import ro.cst.tsearch.exceptions.BaseException;
import ro.cst.tsearch.user.UserAttributes;
import ro.cst.tsearch.utils.DBConstants;

/**
 * CategoryUtils class provides utils functions related to category.
 */
public class CategoryUtils {
	
	private static final Logger logger = Logger.getLogger(CategoryUtils.class);
	
	/** Creates a new category, along with associated attributes.
	 * @param ca The category object.
	 * @returns the new CategoryAttributes created
	 */
	public static CategoryAttributes createCategory(
		CategoryAttributes ca
		/*,String limits*/)
		throws BaseException, DataException {
		CategoryAttributes caa = CategoryManager.createNewCategory(ca/*, limits*/);
		return caa;
	}
	/**
	 * Delete a category object based on it's name.
	 * Parameters:
	 *  @param username The category name.
	 */
	public static void deleteCategory(String categId) throws BaseException {
				
		CategoryManager.delete(categId);
	}
	public static CategoryAttributes getCategory(String name)
		throws BaseException {
		return CategoryManager.getCategory(name);
	}
	/**Return a 
	 * {@link ro.cst.vems.data.community.CategoryAttributes CategoryAttributes} object.
	 * Parameters:
	 *  @param id The category identifier.
	 */
	public static CategoryAttributes getCategory(long id)
		throws BaseException {
		return CategoryManager.getCategory(id);
	}
	/** 
	 * Return an array of 
	 * {@link ro.cst.vems.data.category.CategoryAttributes CategoryAttributes}objects.
	 * Parameters:
	 *  @param special The sort criteria.
	 */
	public static CategoryAttributes[] getCategories(String sorted)
		throws BaseException {
		CategoryAttributes[] cas = CategoryManager.getCategories(sorted);
		return cas;
	}
	/** 
	 * Return an array of category names.
	 * Parameters:
	 *  @param special The sort criteria.
	 */
	public static String[] getCategoriesAsString(String sorted)
		throws BaseException {
		CategoryAttributes[] caA = CategoryUtils.getCategories(sorted);
		String[] cNameA = new String[caA.length];
		for (int i = 0; i < caA.length; i++) {
			cNameA[i] = (String) caA[i].getAttribute(CategoryAttributes.NAME);
		}
		return cNameA;
	}
	public static String[] getAllCategoryForUser(String user_id)
		throws BaseException {
		String stm =
			"SELECT DISTINCT "
				+ "a."
				+ CategoryAttributes.CATEGORY_NAME
				+ " FROM "
				+ DBConstants.TABLE_CATEGORY
				+ " a , "
				+ DBConstants.TABLE_COMMUNITY
				+ " b ,"
				+ DBConstants.TABLE_USER_COMMUNITY
				+ " c  "
				+ " WHERE a."
				+ CategoryAttributes.CATEGORY_ID
				+ "="
				+ " b."
				+ CommunityAttributes.COMMUNITY_CATEGORY
				+ " AND b."
				+ CommunityAttributes.COMMUNITY_ID
				+ "="
				+ " c."
				+ CommunityAttributes.COMMUNITY_ID
				+ " AND c."
				+ UserAttributes.USER_ID
				+ "="
				+ " ? ";
		logger.debug("Statement..." + stm);
		
		 DBConnection conn = null;
	     try {   
	        
	        conn = ConnectionPool.getInstance().requestConnection();
	        PreparedStatement pstmt = conn.prepareStatement( stm );
	        
	        pstmt.setString( 1 ,  user_id );
			DatabaseData result = conn.executePrepQuery(pstmt);	
			
			int resultRows = result.getRowNumber();
			String[] categName = new String[resultRows];
			for (int i = 0; i < resultRows; i++) {
				categName[i] = (String) result.getValue(1, i);
			}
			
			pstmt.close();
			return categName;
			
		} catch (Exception e) {
			throw new BaseException("SQLException:" + e.getMessage());
		} finally {
			try{
			    ConnectionPool.getInstance().releaseConnection(conn);
			}catch(BaseException e){
			    throw new BaseException("SQLException:" + e.getMessage());
			}			
		}
	}
}
