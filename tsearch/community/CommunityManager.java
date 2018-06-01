/*
 * @(#)CommunityManager.java 1.30 2000/08/15
 * Copyright (c) 1998-2000 CornerSoft Technologies, SRL
 * Bucharest, Romania
 * All Rights Reserved.
 *
 * This software is he confidential and proprietary information of
 * CornerSoft Technologies, SRL. ("Confidential Information"). You shall not
 * disclose such Confidential Information and shall use it only in accordance
 * with the terms of the license agreement you entered into with CST.
 */
 
package ro.cst.tsearch.community;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Hashtable;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpSession;

import org.apache.log4j.Logger;
import org.springframework.jdbc.core.simple.ParameterizedRowMapper;
import org.springframework.jdbc.core.simple.SimpleJdbcTemplate;

import ro.cst.tsearch.ServerConfig;
import ro.cst.tsearch.data.DataException;
import ro.cst.tsearch.data.User;
import ro.cst.tsearch.database.ConnectionPool;
import ro.cst.tsearch.database.DBConnection;
import ro.cst.tsearch.database.DBManager;
import ro.cst.tsearch.database.DatabaseData;
import ro.cst.tsearch.exceptions.BaseException;
import ro.cst.tsearch.generic.Util;
import ro.cst.tsearch.user.UserAttributes;
import ro.cst.tsearch.utils.DBConstants;
import ro.cst.tsearch.utils.SessionParams;

/**
 * CommuntiyManager holds a collection of utilities to manage all 
 * datas related to communities.
 */
public class CommunityManager {

	private static final Logger logger = Logger.getLogger(CommunityManager.class);
    /**
     * Create and return a new communtiy object, along with associated attributes.
     * @param ca The community object which have to be created.
     */
    public static CommunityAttributes createNewCommunity(CommunityAttributes ca, Hashtable hashWithFiles, String sessionId)
        throws BaseException, DataException {        

        long id = 0;
		

        String stm = "INSERT INTO " + DBConstants.TABLE_COMMUNITY + "(" 
            + CommunityAttributes.COMMUNITY_CATEGORY + ","
            + CommunityAttributes.COMMUNITY_NAME + ","
            + CommunityAttributes.COMMUNITY_DESCRIPTION + ","
            + CommunityAttributes.COMMUNITY_ADMIN + ","
            + CommunityAttributes.COMMUNITY_CTIME + ","
            + CommunityAttributes.COMMUNITY_LAST_ACCESS + ","
            + CommunityAttributes.COMMUNITY_ADDRESS + ","
            + CommunityAttributes.COMMUNITY_PHONE + ","
            + CommunityAttributes.COMMUNITY_EMAIL + ","
            + CommunityAttributes.COMMUNITY_CONTACT + ","
            + CommunityAttributes.COMMUNITY_CODE + ","
            + CommunityAttributes.COMMUNITY_SEE_ALSO + ","
            + CommunityAttributes.COMMUNITY_CRNCY_ID + ","
            + CommunityAttributes.COMMUNITY_INV_DUE_OFFSET + ","
            + CommunityAttributes.COMMUNITY_TEMPLATE + ","
            + CommunityAttributes.COMMUNITY_OFFSET + ","
            + CommunityAttributes.COMMUNITY_AUTOFILEID+ ","
            + CommunityAttributes.COMMUNITY_IGNORE_PRIVACY_STATEMENT + ","
            + CommunityAttributes.COMMUNITY_PLACEHOLDER
            + ") VALUES("
            + " ?, "
            + " ?, "
            + " ?, "
            + " ?, "
            + " ?, "
            + " ?, "
            + " ?, "
            + " ?, "
            + " ?, "
            + " ?, "
            + " ?, "
            + " ?, "
            + " 0, "
            + " ?, "
            + " 0, "
            + " ?, "
            + " ?, "
            + " ?, "
            + " ? "
            + ")";
        
        DBConnection conn = null;
        
        try {
        	conn = ConnectionPool.getInstance().requestConnection();
        	PreparedStatement pstmt = conn.prepareStatement( stm );
			int k=1;
			
			pstmt.setObject( k++, ca.getAttribute(CommunityAttributes.CATEGORY),java.sql.Types.INTEGER);
			pstmt.setString( k++, ca.getAttribute(CommunityAttributes.NAME).toString() );
			pstmt.setObject( k++, ca.getAttribute(CommunityAttributes.DESCRIPTION) );
			pstmt.setObject( k++, ca.getAttribute(CommunityAttributes.COMM_ADMIN),java.sql.Types.BIGINT);
			pstmt.setObject( k++, ca.getAttribute(CommunityAttributes.CTIME),java.sql.Types.BIGINT);
			pstmt.setObject( k++, ca.getAttribute(CommunityAttributes.LAST_ACCESS),java.sql.Types.BIGINT);
			pstmt.setString( k++, ca.getAttribute(CommunityAttributes.ADDRESS).toString());
			pstmt.setString( k++, ca.getAttribute(CommunityAttributes.PHONE).toString());
			pstmt.setString( k++, ca.getAttribute(CommunityAttributes.EMAIL).toString());
			pstmt.setString( k++, ca.getAttribute(CommunityAttributes.CONTACT).toString());
			pstmt.setString( k++, ca.getAttribute(CommunityAttributes.CODE).toString());
			pstmt.setObject( k++, ca.getAttribute(CommunityAttributes.SEE_ALSO) );
			pstmt.setObject( k++, ca.getAttribute(CommunityAttributes.INV_DUE_OFFSET),java.sql.Types.BIGINT);
			pstmt.setObject( k++, ca.getAttribute(CommunityAttributes.OFFSET),java.sql.Types.INTEGER);
			pstmt.setObject( k++, ca.getAttribute(CommunityAttributes.AUTOFILEID),java.sql.Types.INTEGER);
			pstmt.setObject( k++, ca.getAttribute(CommunityAttributes.IGNORE_PRIVACY_STATEMENT),java.sql.Types.BOOLEAN);
			
			pstmt.setString( k++, ca.getAttribute(CommunityAttributes.PLACEHOLDER).toString() );
			
            conn.setAutoCommit(false);
			pstmt.executeUpdate();			
            
            id = DBManager.getLastId(conn);
            ca.setAttribute(CommunityAttributes.ID, new BigInteger( (new Long(id)).toString() ));
            
            setCommunityOffset(ca);
   
           // UploadPolicyDoc.updateDB(sessionId, UploadPolicyDoc.COMMUNITY_NEW, ca);
            
			File photo  = (File)hashWithFiles.get(CommunityAttributes.COMMUNITY_LOGO);
			File termsOfUse = (File)hashWithFiles.get(CommunityAttributes.COMMUNITY_TERMS_OF_USE);
			File privacyState = (File)hashWithFiles.get(CommunityAttributes.COMMUNITY_PRIVACY_STATEMENT);
			
			/*Statement stmt = null;
			ResultSet resultBlob = null;*/
			conn.commit();
			if (insertFileInDB(new Long(id).toString(),photo, null,DBConstants.TABLE_COMMUNITY_LOGO,CommunityAttributes.COMMUNITY_LOGO, null, true)) {	
			}
			if (insertFileInDB(new Long(id).toString(),termsOfUse, privacyState,DBConstants.TABLE_COMMUNITY_TERMS_OF_USE,CommunityAttributes.COMMUNITY_TERMS_OF_USE, CommunityAttributes.COMMUNITY_PRIVACY_STATEMENT, false)) {
				
			}
            
        }catch (Exception e){
            e.printStackTrace();
            throw new BaseException("SQLException:" + e.getMessage());
        } finally {
			try{
				if (conn != null) {
					try {
						conn.commit();
					} catch (SQLException e1) {
						e1.printStackTrace();
					}
				}
			    ConnectionPool.getInstance().releaseConnection(conn);
			}catch(BaseException e){
			    throw new BaseException("SQLException:" + e.getMessage());
			}			
		}
        
        if(id>0) {
        	try {
        		int sitesCount = DBManager.getSimpleTemplate().update("insert into " + 
        				DBConstants.TABLE_COMMUNITY_SITES + " select " + id + ", id_county, site_type, p2, 0 FROM ts_sites");
        		System.out.println("Inserted " + sitesCount + " sites into database");
        	}catch(Exception e) {
        		e.printStackTrace();
        	}
        	try {
        		int sitesCount = DBManager.getSimpleTemplate().update("insert into " + 
        				DBConstants.TABLE_COUNTY_COMMUNITY + " ( " +
        				DBConstants.FIELD_COUNTY_COMMUNITY_COUNTY_ID + ", " + 
        				DBConstants.FIELD_COUNTY_COMMUNITY_COMMUNITY_ID + ") select id, " + id + 
        				" FROM " + DBConstants.TABLE_COUNTY);
        		System.out.println("Inserted " + sitesCount + " rows into database");
        		DBManager.getSimpleTemplate().update(
        				"update county_community, ts_county set county_community.default_start_date_offset = 30 " +
        				"where county_community.county_id = ts_county.id and ts_county.state_id in (5,18,43) and county_community.community_id = ?", id);
        		DBManager.getSimpleTemplate().update(
        				"update county_community, ts_county set county_community.default_start_date_offset = 40 " +
        				"where county_community.county_id = ts_county.id and ts_county.state_id in (23, 36) and county_community.community_id = ?", id);
        		DBManager.getSimpleTemplate().update(
        				"update county_community, ts_county set county_community.default_start_date_offset = 55 " +
        				"where county_community.county_id = ts_county.id and ts_county.state_id = 10 and county_community.community_id = ?", id);
        		DBManager.getSimpleTemplate().update(
        				"update county_community, ts_county set county_community.default_start_date_offset = 110 " +
        				"where county_community.county_id = ts_county.id and ts_county.state_id = 6 and county_community.community_id = ?", id);
        	}catch(Exception e) {
        		e.printStackTrace();
        	}
        }
        
        return ca;
    }

    /**Deletes this entry from database.
     * @param ca the community object which have to be deleted.
     */
    public static void delete(CommunityAttributes ca)
        throws BaseException {
        
        try{
            SimpleJdbcTemplate sjt = DBManager.getSimpleTemplate();
            long commId = ((BigDecimal)ca.getAttribute(CommunityAttributes.ID)).longValue();
            sjt.update("DELETE FROM " + DBConstants.TABLE_COMMUNITY_SEQ + " WHERE comm_id = ?", commId);
            
            sjt.update("DELETE FROM " + DBConstants.TABLE_COMMUNITY + " WHERE " + 
            		CommunityAttributes.COMMUNITY_ID + "= ?", commId);
            sjt.update("DELETE FROM " + DBConstants.TABLE_COMMUNITY_SITES + " WHERE " +
            		DBConstants.FIELD_COMMUNITY_SITES_COMMUNITY_ID + "= ?", commId);
            sjt.update("DELETE FROM " + DBConstants.TABLE_COUNTY_COMMUNITY + " WHERE " +
            		DBConstants.FIELD_COUNTY_COMMUNITY_COMMUNITY_ID + "= ?", commId);
            
        }catch (Exception e){
        	logger.error("Error while deleting a community " + ca, e);
            throw new BaseException("SQLException:" + e.getMessage());
        } 
    }


    /**Deletes this entry from database.
     * @param name the name of the community which have to be deleted.
     */
    public static void delete(String name)
        throws BaseException {
        
        CommunityAttributes ca = CommunityUtils.getCommunityFromName(name);
        CommunityManager.delete(ca);
    }
    /** Retrieves a community object based on it's identifier.
     * @param id The identifier of the community.
     */
    public static CommunityAttributes getCommunity(long id)
        throws BaseException {

        String stm = "";

        stm = "SELECT "
            + CommunityAttributes.COMMUNITY_NAME        + ","
            + CommunityAttributes.COMMUNITY_CATEGORY    + ","
            + CommunityAttributes.COMMUNITY_DESCRIPTION + ","
            + CommunityAttributes.COMMUNITY_CTIME       + ","
            + CommunityAttributes.COMMUNITY_SEE_ALSO    + ","
            + CommunityAttributes.COMMUNITY_ADMIN       + ","
            + CommunityAttributes.COMMUNITY_ADDRESS     + ","
            + CommunityAttributes.COMMUNITY_EMAIL       + ","
            + CommunityAttributes.COMMUNITY_CONTACT     + ","
            + CommunityAttributes.COMMUNITY_PHONE       + ","
            + CommunityAttributes.COMMUNITY_TEMPLATE    + ","
            + CommunityAttributes.COMMUNITY_CODE        + ","
            + CommunityAttributes.COMMUNITY_CRNCY_ID   + ","
            + CommunityAttributes.COMMUNITY_INV_DUE_OFFSET + ","
            + CommunityAttributes.COMMUNITY_LAST_ACCESS    + ","
            + CommunityAttributes.COMMUNITY_ID             + ","
            + CommunityAttributes.COMMUNITY_TSD_INDEX + ","
            + CommunityAttributes.COMMUNITY_COMMITMENT + ","
			+ CommunityAttributes.COMMUNITY_TEMPLATES_PATH + ","
			+ CommunityAttributes.COMMUNITY_OFFSET + ","
			+ CommunityAttributes.COMMUNITY_AUTOFILEID + ","
            + CommunityAttributes.COMMUNITY_DEFAULT_SLA + ","
            + CommunityAttributes.COMMUNITY_SUPPORT_EMAIL + ","
            + CommunityAttributes.COMMUNITY_IGNORE_PRIVACY_STATEMENT + ","
            + CommunityAttributes.COMMUNITY_PLACEHOLDER + ","
            + CommunityAttributes.COMMUNITY_NUMBER_OF_UPDATES + ","
            + CommunityAttributes.COMMUNITY_NUMBER_OF_DAYS
            + " FROM " + DBConstants.TABLE_COMMUNITY
            + " WHERE "
            + CommunityAttributes.COMMUNITY_ID + "=" + id;
        
        DBConnection conn = null;
        try{
            
            conn = ConnectionPool.getInstance().requestConnection();
            DatabaseData result = conn.executeSQL(stm);
            
            if (result.getRowNumber() > 0){
            	CommunityAttributes ca = new CommunityAttributes(result, 0);            	
            	byte[] logo = CommunityUtils.getCommLogo(ca.getID());
            	if(logo!=null)
            		ca.setLOGO(logo);				
				return ca;
            } else {				
				throw new BaseException("no_community_id:"+id+ stm);            	
            }
            
        } catch (Exception e){
            e.printStackTrace();
            throw new BaseException("SQLException:" + e.getMessage());
            
        } finally {
            try{
			    ConnectionPool.getInstance().releaseConnection(conn);
			}catch(BaseException e){
			    throw new BaseException("SQLException:" + e.getMessage());
			}	
        }
    }
    
    public static List<Integer> getAllCommunityIds(boolean includeClosedGroup) {
    	String sql = "SELECT " + DBConstants.FIELD_COMMUNITY_COMM_ID + " from " + DBConstants.TABLE_COMMUNITY;
    	if(!includeClosedGroup) {
    		sql += " com join " + DBConstants.TABLE_CATEGORY + " cat ON com.categ_id = cat.categ_id where cat.categ_name != '" + ServerConfig.getClosedCommunityName() + "'";
    	}
    	SimpleJdbcTemplate sjt = DBManager.getSimpleTemplate();
    	List<Integer> communityIds = sjt.query(sql, new ParameterizedRowMapper<Integer>() {

			@Override
			public Integer mapRow(ResultSet resultSet, int rowNum) throws SQLException {
				return resultSet.getInt(DBConstants.FIELD_COMMUNITY_COMM_ID);
			}
    		
		});
    	return communityIds;
    }
    
    public static Map<String,String> getAllCommunityBasicData(boolean excludeHiddenCommunities) {
    	SimpleJdbcTemplate sjt = DBManager.getSimpleTemplate();
    	String sql = "SELECT com." + DBConstants.FIELD_COMMUNITY_COMM_ID + ", " +
    			     "com." + DBConstants.FIELD_COMMUNITY_COMM_NAME + " " +  
    			     "FROM " + DBConstants.TABLE_COMMUNITY + " com ";
    	if (excludeHiddenCommunities) {
    		sql+= "JOIN " + DBConstants.TABLE_CATEGORY + " cat " +
    			  "on com." + DBConstants.FIELD_COMMUNITY_CATEG_ID + "=cat." + CategoryAttributes.CATEGORY_ID + " " +
    			  "where cat." + CategoryAttributes.CATEGORY_NAME + "!=? ";
    	}		
    	sql += "ORDER BY com." + DBConstants.FIELD_COMMUNITY_COMM_NAME;
    	List<Map<String,Object>> communityValueLabel = null;
		if (excludeHiddenCommunities) {
			communityValueLabel = sjt.queryForList(sql, ServerConfig.getClosedCommunityName());
		} else {
			communityValueLabel = sjt.queryForList(sql);
		}
    	
		Map<String, String> valueLabelMap = new LinkedHashMap<String, String>();
		for (Map<String, Object> map : communityValueLabel) {
			String value = map.get(DBConstants.FIELD_COMMUNITY_COMM_ID).toString();
			String label = (String) map.get(DBConstants.FIELD_COMMUNITY_COMM_NAME);
			valueLabelMap.put(value, label);
		}
    	return valueLabelMap;
    }

    /** Retrieves a community object based on it's name.
     * @param name The identifier of the community.
     */
    public static CommunityAttributes getCommunity(String name)
        throws BaseException {


			String stm = "SELECT "
				 + CommunityAttributes.COMMUNITY_NAME        + ","
				 + CommunityAttributes.COMMUNITY_CATEGORY    + ","
				 + CommunityAttributes.COMMUNITY_DESCRIPTION + ","
				 + CommunityAttributes.COMMUNITY_CTIME       + ","
				 + CommunityAttributes.COMMUNITY_SEE_ALSO    + ","
				 + CommunityAttributes.COMMUNITY_ADMIN       + ","
				 + CommunityAttributes.COMMUNITY_ADDRESS     + ","
				 + CommunityAttributes.COMMUNITY_EMAIL       + ","
				 + CommunityAttributes.COMMUNITY_CONTACT     + ","
				 + CommunityAttributes.COMMUNITY_PHONE       + ","
				 + CommunityAttributes.COMMUNITY_TEMPLATE    + ","
				 + CommunityAttributes.COMMUNITY_CODE        + ","
				 + CommunityAttributes.COMMUNITY_CRNCY_ID   + ","
				 + CommunityAttributes.COMMUNITY_INV_DUE_OFFSET + ","
				 + CommunityAttributes.COMMUNITY_LAST_ACCESS    + ","
                 + CommunityAttributes.COMMUNITY_ID             + ","
                 + CommunityAttributes.COMMUNITY_TSD_INDEX		+ ","
				 + CommunityAttributes.COMMUNITY_COMMITMENT 	+ ","
				 + CommunityAttributes.COMMUNITY_TEMPLATES_PATH + ","
				 + CommunityAttributes.COMMUNITY_OFFSET + ","
				 + CommunityAttributes.COMMUNITY_AUTOFILEID + ","
				 + CommunityAttributes.COMMUNITY_DEFAULT_SLA + ","
				 + CommunityAttributes.COMMUNITY_SUPPORT_EMAIL+ ","
		         + CommunityAttributes.COMMUNITY_IGNORE_PRIVACY_STATEMENT + ","
				 + CommunityAttributes.COMMUNITY_PLACEHOLDER + ","
				 + CommunityAttributes.COMMUNITY_NUMBER_OF_UPDATES + ","
				 + CommunityAttributes.COMMUNITY_NUMBER_OF_DAYS
				 + " FROM " + DBConstants.TABLE_COMMUNITY
            + " WHERE "
            + CommunityAttributes.COMMUNITY_NAME + "= ? ";

		DBConnection conn = null;
		
        try{
            
            conn = ConnectionPool.getInstance().requestConnection();
            PreparedStatement pstmt = conn.prepareStatement( stm );

			pstmt.setString( 1, name);
			DatabaseData result = conn.executePrepQuery(pstmt);	
			
            if (result.getRowNumber()>0)
                return new CommunityAttributes(result, 0);
            else
                throw new BaseException("no_community_name:"+name);
            
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

    /** Retrieves an array of community objects.
     * @param cf The search filter.
     * @param special Specify if the array is sorted or not.
     */    
    public static CommunityAttributes[] getCommunities(CommunityFilter cf, boolean special,HttpSession session)
        throws BaseException {
         
			String stm = "SELECT "
				 + CommunityAttributes.COMMUNITY_NAME        + ","
				 + CommunityAttributes.COMMUNITY_CATEGORY    + ","
				 + CommunityAttributes.COMMUNITY_DESCRIPTION + ","
				 + CommunityAttributes.COMMUNITY_CTIME       + ","
				 + CommunityAttributes.COMMUNITY_SEE_ALSO    + ","
				 + CommunityAttributes.COMMUNITY_ADMIN       + ","
				 + CommunityAttributes.COMMUNITY_ADDRESS     + ","
				 + CommunityAttributes.COMMUNITY_EMAIL       + ","
				 + CommunityAttributes.COMMUNITY_CONTACT     + ","
				 + CommunityAttributes.COMMUNITY_PHONE       + ","
				 + CommunityAttributes.COMMUNITY_TEMPLATE    + ","
				 + CommunityAttributes.COMMUNITY_CODE        + ","
				 + CommunityAttributes.COMMUNITY_CRNCY_ID   + ","
				 + CommunityAttributes.COMMUNITY_INV_DUE_OFFSET + ","
				 + CommunityAttributes.COMMUNITY_LAST_ACCESS    + ","
                 + CommunityAttributes.COMMUNITY_ID             + ","
                 + CommunityAttributes.COMMUNITY_TSD_INDEX + ","
                 + CommunityAttributes.COMMUNITY_COMMITMENT + ","
				 + CommunityAttributes.COMMUNITY_TEMPLATES_PATH + ","
				 + CommunityAttributes.COMMUNITY_OFFSET + ","
				 + CommunityAttributes.COMMUNITY_AUTOFILEID + ","
				 + CommunityAttributes.COMMUNITY_DEFAULT_SLA + ","
				 + CommunityAttributes.COMMUNITY_SUPPORT_EMAIL+ ","
		         + CommunityAttributes.COMMUNITY_IGNORE_PRIVACY_STATEMENT + ","
		         + CommunityAttributes.COMMUNITY_PLACEHOLDER + ","
		         + CommunityAttributes.COMMUNITY_NUMBER_OF_UPDATES + ","
		         + CommunityAttributes.COMMUNITY_NUMBER_OF_DAYS
				 + " FROM " + DBConstants.TABLE_COMMUNITY + " a ";
			//User currentUser = (User) session.getAttribute(SessionParams.CURRENT_USER);
        if(cf.getAdminCommuntiesFlag ()){
	    	//UserAttributes ua = InstanceManager.getCurrentInstance ().getCurrentUser ();
		 stm = stm + " WHERE "  + "a." +  CommunityAttributes.COMMUNITY_ADMIN + " = ? " ;
		}
        if(special)
            stm = stm + " ORDER BY ? ";
    	
        logger.info("Get Communities..." + stm);
        
        DBConnection conn = null;
        try {
            
            conn = ConnectionPool.getInstance().requestConnection();
            PreparedStatement pstmt = conn.prepareStatement( stm );

            int k = 1;
            if(cf.getAdminCommuntiesFlag ()){
  	    	  User currentUser = (User) session.getAttribute(SessionParams.CURRENT_USER);
	    	  UserAttributes ua =currentUser.getUserAttributes();
              pstmt.setObject( k++, ua.getAttribute (UserAttributes.ID) , java.sql.Types.BIGINT);
            }
            
            if(special) 
              pstmt.setString( k++, cf.getSortCriteria());
            
			DatabaseData result = conn.executePrepQuery(pstmt);	

            
            int resultRows = result.getRowNumber();
            
            CommunityAttributes[] cas = new CommunityAttributes[resultRows];
            for (int i=0; i<resultRows; i++) {
                cas[i] = new CommunityAttributes(result, i);
            }
            return cas;
            
        }catch (Exception e){
            e.printStackTrace();
            throw new BaseException("SQLException:" + e.getMessage());
        } finally {
            try{
			    ConnectionPool.getInstance().releaseConnection(conn);
			}catch(BaseException e){
			    throw new BaseException("SQLException:" + e.getMessage());
			}	
        }
   }
    
    public static void setCommunityOffset(CommunityAttributes ca) throws BaseException {
        
        DBConnection conn = null;
        long id = ((BigInteger)ca.getAttribute(CommunityAttributes.ID)).longValue();
        String sql;
        try {
            
            conn = ConnectionPool.getInstance().requestConnection();
            sql = "SELECT comm_offset FROM " + DBConstants.TABLE_COMMUNITY_SEQ + " WHERE comm_id = " + id;
            
            DatabaseData data = conn.executeSQL(sql);
            
            //if we found something in the table we must update the value, else we do a clean insert
            if(data.getRowNumber()>0){
            	sql = "UPDATE " + DBConstants.TABLE_COMMUNITY_SEQ + " SET comm_offset=" + 
            			 "? " +
            			 "WHERE comm_id=" + id;
            	
        		try {
        			DBManager.getSimpleTemplate().update(sql, 	(ca.getAttribute(CommunityAttributes.OFFSET) == null || 
                																		((Integer)ca.getAttribute(CommunityAttributes.OFFSET)).longValue() < 1 ? 
                																		"1" : ca.getAttribute(CommunityAttributes.OFFSET))
                        			);
        		} catch (Exception e) {
        			e.printStackTrace();
        		}
        		
                                
            }
            else {
            	sql = "INSERT INTO " + DBConstants.TABLE_COMMUNITY_SEQ + " (comm_id,comm_offset)" + 
            			 "VALUES( ?, ? )";
            	
            	
            	try {        			
        			DBManager.getSimpleTemplate().update(sql,id, 	(ca.getAttribute(CommunityAttributes.OFFSET) == null || 
                 																			((Integer)ca.getAttribute(CommunityAttributes.OFFSET)).longValue() < 1 ? 
                 																			"1" : ca.getAttribute(CommunityAttributes.OFFSET))
                        			);
        		} catch (Exception e) {
        			e.printStackTrace();
        		}
            }
      
            
        } catch (Exception e) {
            e.printStackTrace();
            throw new BaseException("SQLException:" + e.getMessage());
        } finally {
            try {
			    ConnectionPool.getInstance().releaseConnection(conn);
			} catch(BaseException e){
			    throw new BaseException("SQLException:" + e.getMessage());
			}	
        }
    }
    
    public static long getCommunityOffset(CommunityAttributes ca) throws BaseException {

        long nextOffset = 0;
        long id = ((BigInteger)ca.getAttribute(CommunityAttributes.ID)).longValue();
        String sql; 
        
        DBConnection conn = null;
        try {
            
            conn = ConnectionPool.getInstance().requestConnection();
            sql = "SELECT comm_offset FROM " + DBConstants.TABLE_COMMUNITY_SEQ + " WHERE comm_id = " + id;
            DatabaseData dbData = conn.executeSQL(sql);
            
            if (dbData.getRowNumber() == 0) {
            	setCommunityOffset(ca);
            	DBConnection conn2 = null;
            	try{
            		conn2 = ConnectionPool.getInstance().requestConnection();
            		dbData = conn2.executeSQL(sql);
                } catch (Exception e) {
                    e.printStackTrace();
                    throw new BaseException("SQLException:" + e.getMessage());
                } finally {
                    try {
        			    ConnectionPool.getInstance().releaseConnection(conn2);
        			} catch(BaseException e){
        			    throw new BaseException("SQLException:" + e.getMessage());
        			}	
                }
            }
            nextOffset = ((Long)dbData.getValue(1, 0)).longValue();
            
            //must increment this value and write it back to the database
            
            sql = "UPDATE " + DBConstants.TABLE_COMMUNITY_SEQ + " SET comm_offset=" + (nextOffset+1) + " WHERE comm_id = " + id;
            conn.executeUpdate(sql);
            
        } catch (Exception e) {
            e.printStackTrace();
            throw new BaseException("SQLException:" + e.getMessage());
        } finally {
            try {
			    ConnectionPool.getInstance().releaseConnection(conn);
			} catch(BaseException e){
			    throw new BaseException("SQLException:" + e.getMessage());
			}	
        }
        
        return nextOffset;
    }
    
    public static boolean insertFileInDB (String commId, 
    				File fileToStore1, 
					File fileToStore2,
					String tableName, 
					String fieldName1,
					String fieldName2,
					boolean isLogo) throws
						BaseException{
    	String stm = "";
		DBConnection conn = null;
		PreparedStatement pstmt = null;
		//ResultSet resultBlob = null;
		conn = ConnectionPool.getInstance().requestConnection();
        try {
        	conn.setAutoCommit(false);
			if (fileToStore1 != null || fileToStore2 != null) {
				stm = "INSERT INTO " + tableName 
						+ " ( " + CommunityAttributes.COMMUNITY_ID
						+ (isLogo == false ?", " + CommunityAttributes.COMMUNITY_TERMS_ID : "")
						+ (fieldName1 != null ? "," + fieldName1 : "") /*CommunityAttributes.COMMUNITY_LOGO*/ + " ) "
						+ (fieldName2 != null ? "," + fieldName2 : "")
				+" VALUES ( ?" + (fieldName1 != null ? ", ?" : "") 
				+ (fieldName2 != null ? ", ?" : "") + " ) ";
				logger.debug("insertFileInDB: " + stm);
				//conn.executeSQL(stm);
				pstmt = conn.prepareStatement(stm);
				
				pstmt.setLong(1, Long.parseLong(commId));
				FileInputStream fis1 = null;
				FileInputStream fis2 = null;
				
				if(fieldName1!=null){
					fis1 = new FileInputStream(fileToStore1);
					pstmt.setBinaryStream(2, fis1, (int)fileToStore1.length());
					if(fieldName2!=null) {
						fis2 = new FileInputStream(fileToStore2);
						pstmt.setBinaryStream(3, new FileInputStream(fileToStore2), (int)fileToStore2.length());
					}
					
				} else {
					if(fieldName2!=null){
						fis2 = new FileInputStream(fileToStore2);
						pstmt.setBinaryStream(2, new FileInputStream(fileToStore2), (int)fileToStore2.length());
					}
				}
				
				pstmt.executeUpdate();
				pstmt.close();
				if(fis1!=null)	fis1.close();
				if(fis2!=null)	fis2.close();
				conn.commit();
				
				if(fileToStore1.delete()==true){
					logger.info("File "+ fileToStore1.getName() + " succesufully deleted from WEB SERVER!");
				}else{
					logger.info("Can't delete File "+ fileToStore1.getName() + " from WEB SERVER!");				
				}
				if(fileToStore2 != null && fileToStore2.delete() == true){
					logger.info("File "+ fileToStore2.getName() + " succesufully deleted from WEB SERVER!");
				}else if (fileToStore2 != null && fileToStore2.delete() == false){
					logger.info("Cannot delete File "+ fileToStore2.getName() + " from WEB SERVER!");				
				}
			}else if (fileToStore1 == null && isLogo){
				if(!tableName.equalsIgnoreCase(DBConstants.TABLE_COMMUNITY_LOGO)) {
					stm = "INSERT INTO " + tableName /*DBConstants.TABLE_COMMUNITY_LOGO*/
						+ " ( " + CommunityAttributes.COMMUNITY_ID 
						+ ", " + fieldName1 + ") " 
						+ " VALUES ( " +  Long.parseLong(commId) + ", "
						+ "(SELECT LOGO FROM " + DBConstants.TABLE_COMMUNITY_LOGO + " WHERE COMM_ID=(SELECT COMM_ID FROM " + DBConstants.TABLE_COMMUNITY + " WHERE COMM_NAME LIKE '%Default%')))";
					conn.executeSQL(stm);
				} else {	
					//this is because Mysql does not allow to update a table that is in the FROM clause
					stm = "INSERT INTO " + DBConstants.TABLE_EXTRA_TABLE + " (FIELD_BLOB) " +
					" SELECT LOGO FROM TS_COMMUNITY_LOGO WHERE COMM_ID=(SELECT COMM_ID FROM TS_COMMUNITY WHERE COMM_NAME LIKE '%Default%')";
					conn.executeSQL(stm);
					long last_id = DBManager.getLastId(conn);
					stm = "INSERT INTO " + DBConstants.TABLE_COMMUNITY_LOGO
						+ " ( " + CommunityAttributes.COMMUNITY_ID + ", " + fieldName1 + ") " 
						+ " VALUES ( " + Long.parseLong(commId) + ", (SELECT FIELD_BLOB FROM " + DBConstants.TABLE_EXTRA_TABLE + " WHERE ID = " + last_id + " ))";
					conn.executeSQL(stm);
					stm = "DELETE FROM " + DBConstants.TABLE_EXTRA_TABLE + " WHERE ID=" + last_id;
					conn.executeSQL(stm);
				}
			}
		} catch (FileNotFoundException e) {
			e.printStackTrace();
			return false;
		} catch (BaseException e) {
			e.printStackTrace();
			return false;
		} catch (SQLException e) {
			e.printStackTrace();
			return false;
		} catch (IOException e) {
			e.printStackTrace();
			return false;
		} finally {
			try{
				if (conn != null) {
					try {
						conn.commit();
						//conn.setAutoCommit(true);
					} catch (SQLException e1) {
						e1.printStackTrace();
					}
				}
			    ConnectionPool.getInstance().releaseConnection(conn);
			}catch(BaseException e){
			    throw new BaseException("SQLException:" + e.getMessage());
			}			
		}
    	return true;
    }
    
    //move communities to closed group
    public static void hideCommunities(String commIds) {
    	if (Util.isParameterValid(commIds)) {
    		try {
        		String closedName = ro.cst.tsearch.ServerConfig.getClosedCommunityName();
        		CategoryAttributes closedGroup = CategoryManager.getCategory(closedName);
        		BigDecimal bi = null;
        		try {
        			bi = closedGroup.getID();
        		} catch (NullPointerException npe) { //the group does not exist
        			try {
        				CategoryAttributes newCA = new CategoryAttributes();
            			newCA.setNAME(closedName);
            			CategoryUtils.createCategory(newCA);
            			closedGroup = CategoryManager.getCategory(closedName);
            			bi = closedGroup.getID();
        			} catch (DataException de) {
        				logger.error("Error while creating a new community", de);
        			}
        		}
        		int closedId = bi.intValue();
        		String sqlSaveGroupInBackup = "UPDATE " + DBConstants.TABLE_COMMUNITY + " SET " +
        			DBConstants.FIELD_COMMUNITY_CATEG_ID_BACKUP + " = " + DBConstants.FIELD_COMMUNITY_CATEG_ID +
        			" WHERE " + DBConstants.FIELD_COMMUNITY_CATEG_ID + " != " + Integer.toString(closedId) +
        			" AND " + DBConstants.FIELD_COMMUNITY_COMM_ID + " IN ($COMMIDS$)" ;
        		String sqlChangeGroup = "UPDATE " + DBConstants.TABLE_COMMUNITY + " SET " +
    				DBConstants.FIELD_COMMUNITY_CATEG_ID + " = " + Integer.toString(closedId) +
    				" WHERE " + DBConstants.FIELD_COMMUNITY_CATEG_ID + " != " + Integer.toString(closedId) +
        			" AND " + DBConstants.FIELD_COMMUNITY_COMM_ID + " IN ($COMMIDS$)" ;
        		
        		DBManager.getSimpleTemplate().update(sqlSaveGroupInBackup.replace("$COMMIDS$", commIds));
        		DBManager.getSimpleTemplate().update(sqlChangeGroup.replace("$COMMIDS$", commIds));
        	}
        	catch (BaseException be) {
        		logger.error("Error while hiding communities", be);
        	}
    	}
    }
    
    //restore communities from closed group
    public static void unhideCommunities(String commIds) {
    	if (Util.isParameterValid(commIds)) {
    		try {
        		String closedName = ro.cst.tsearch.ServerConfig.getClosedCommunityName();
        		CategoryAttributes closedGroup = CategoryManager.getCategory(closedName);
        		BigDecimal bi = null;
        		try {
        			bi = closedGroup.getID();
        		} catch (NullPointerException npe) { //the group does not exist
        			logger.error("There are not closed communities", npe);
        			return;
        		}
        		int closedId = bi.intValue();
        		String sqlRestoreGroup = "UPDATE " + DBConstants.TABLE_COMMUNITY + " SET " +
        			DBConstants.FIELD_COMMUNITY_CATEG_ID + " = " + DBConstants.FIELD_COMMUNITY_CATEG_ID_BACKUP +
        			" WHERE " + DBConstants.FIELD_COMMUNITY_CATEG_ID + " = " + Integer.toString(closedId) +
        			" AND " + DBConstants.FIELD_COMMUNITY_COMM_ID + " IN ($COMMIDS$)" ;
        		
        		DBManager.getSimpleTemplate().update(sqlRestoreGroup.replace("$COMMIDS$", commIds));
        	}
        	catch (BaseException be) {
        		logger.error("Error while unhiding communities", be);
        	}
    	}
    } 

}
