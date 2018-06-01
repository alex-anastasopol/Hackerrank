/*
 * @(#)CommunityUtils.java 1.30 2000/08/15
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
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;

import javax.servlet.http.HttpSession;

import org.apache.log4j.Logger;
import org.springframework.jdbc.core.simple.SimpleJdbcTemplate;

import ro.cst.tsearch.MailConfig;
import ro.cst.tsearch.data.DataException;
import ro.cst.tsearch.database.ConnectionPool;
import ro.cst.tsearch.database.DBConnection;
import ro.cst.tsearch.database.DBManager;
import ro.cst.tsearch.database.DatabaseData;
import ro.cst.tsearch.database.rowmapper.CommunityUserTemplatesMapper;
import ro.cst.tsearch.exceptions.BaseException;
import ro.cst.tsearch.servlet.BaseServlet;
import ro.cst.tsearch.servlet.community.UploadPolicyDoc;
import ro.cst.tsearch.templates.TemplateBuilder;
import ro.cst.tsearch.templates.TemplatesException;
import ro.cst.tsearch.user.UserAttributes;
import ro.cst.tsearch.user.UserManager;
import ro.cst.tsearch.user.UserUtils;
import ro.cst.tsearch.utils.DBConstants;
import ro.cst.tsearch.utils.FileUtils;
import ro.cst.tsearch.utils.RequestParams;
import ro.cst.tsearch.utils.StringUtils;

/**
 * CommunityUtils class provides utils functions related to communities.
 */
public class CommunityUtils {


	public static final String TEMPLATE_ID = "templateId";
	public static final String TEMPLATE_SHORT = "templateShortName";
	public static final String TEMPLATE_NAME = "templateName";
	
	//DB Fields
	public static final String DB_TEMPLATE_ID = "TEMPLATE_ID";
	public static final String DB_TEMPLATE_SHORT = "SHORT_NAME";
	public static final String DB_TEMPLATE_NAME = "NAME";


	public static final String TEMPL_PATH_OUT = "templates";
	public static final String TSD_TEMPL_PATH = "TSDTemplates";
	
	private static final Logger logger = Logger.getLogger(UserUtils.class);
	
    /**
     * 
     */
    public static CommunityAttributes getCommunityFromName(String name)
        throws BaseException {
        CommunityAttributes ua = CommunityManager.getCommunity(name);
        return ua;
    }

    /**
     * 
     */
    public static CommunityAttributes getCommunity(String name)
        throws BaseException {
        CommunityAttributes ua = CommunityManager.getCommunity(name);
        return ua;
    }

    /**
     * 
     */
    public static CommunityAttributes getCommunityFromId(long id)
        throws BaseException {
        CommunityAttributes ua = CommunityManager.getCommunity(id);
        return ua;
    }


    /**
     * @return the communityAdminID
     */
    public static BigDecimal getCommunityAdministrator(CommunityAttributes community)
        throws BaseException {
        return new BigDecimal(community.getAttribute(CommunityAttributes.COMM_ADMIN).toString());
    }

    /** @return the number users belonging to a community */
    public static int getNrUsers(String commName)
        throws BaseException{

        CommunityAttributes ca = CommunityUtils.getCommunityFromName(commName);
        return getNrUsers(ca);
    }


    /** @return the number users belonging to a community */
    public static int getNrUsers(CommunityAttributes ca)
        throws BaseException{        
        
        DBConnection conn = null;
        try{
            
            conn = ConnectionPool.getInstance().requestConnection();
            String stm = "SELECT DISTINCT " + UserAttributes.USER_ID + " FROM " + DBConstants.TABLE_USER
                + " WHERE " + CommunityAttributes.COMMUNITY_ID + "= ? ";
            
            PreparedStatement pstmt = conn.prepareStatement( stm );
            pstmt.setObject( 1,  ca.getAttribute(CommunityAttributes.ID) , java.sql.Types.BIGINT );
            
			DatabaseData result = conn.executePrepQuery(pstmt);	
			pstmt.close();
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

    /**
     * @return an array of all communities objects, grouped in the specified category.
     */
    public static CommunityAttributes[] getCommunitiesInCategory(BigDecimal categId, CommunityFilter cf)
        throws BaseException{
        
        DBConnection conn = null;
        try{
            
            conn = ConnectionPool.getInstance().requestConnection();

            String stm = "SELECT * "  
            			+ " FROM " + DBConstants.TABLE_COMMUNITY
                		+ " WHERE " + CommunityAttributes.COMMUNITY_CATEGORY
                 		+ "=" + categId
                		+ " ORDER BY " + DBManager.sqlColumnName(cf.getSortCriteria());
			logger.debug(stm);
			PreparedStatement pstmt = conn.prepareStatement( stm );
            
			DatabaseData result = conn.executePrepQuery(pstmt);	
			pstmt.close();
			
            int resultRows = result.getRowNumber();

            CommunityAttributes[] cns = new CommunityAttributes[resultRows];
            for (int i=0; i<resultRows; i++) {
                cns[i] = new CommunityAttributes(result,i);
            }
            return cns;
            
        } catch(Exception e) {
            throw new BaseException("SQLException:" + e.getMessage());
        } finally {
			try{
			    ConnectionPool.getInstance().releaseConnection(conn);
			}catch(BaseException e){
			    throw new BaseException("SQLException:" + e.getMessage());
			}			
		}
    }
    
    /*public static boolean isCommunityVisible(String user_id, String comm_id)
    throws BaseException{
            DBConnection connection = InstanceManager.getCurrentInstance().getDBConnection();
            String stm = "SELECT * from " + DataConstants.TABLE_USER_COMMUNITY
                         + " WHERE "  + DataConstants.USER_ID + "=" + user_id
                         + " AND "    + DataConstants.COMM_ID + "=" + comm_id 
                         + " AND "    + DataConstants.COMM_VISIBLE + "=1";
            Log.debug (stm);
            DatabaseData result = connection.executeSQL(stm);
            int resultRows = result.getRowNumber();
            if(resultRows >0)
                return true;
            else return false;
    

    }*/

    /**
    *Get all communities visible in a group (ex category) for an user
    */
    /*public static String[] getCommunitiesInCategoryVisibleFromUser(String category, String user_id)
    throws BaseException{
        try{
            DBConnection connection = InstanceManager.getCurrentInstance().getDBConnection();
            CategoryAttributes ca = CategoryManager.getCategory(category);
            String stm = "SELECT UNIQUE "
                +  "b."+ DataConstants.COMM_NAME  
                + " FROM " + DataConstants.TABLE_CATEGORY +" a , "
                + DataConstants.TABLE_COMMUNITY  + " b ,"           
                + DataConstants.TABLE_USER_COMMUNITY      +" c  "           
                + " WHERE a."  + DataConstants.CATEG_ID + "="
                + " b."        + DataConstants.CATEG_ID 
                + " AND b."    + DataConstants.COMM_ID + "="
                +      " c."   + DataConstants.COMM_ID 
                + " AND c."    + DataConstants.USER_ID + "="
                + user_id
                + " AND  c."     + DataConstants.COMM_VISIBLE + "=1"
                + " AND a."      + DataConstants.CATEG_NAME   + "="
                + "'" + category + "'"
                ;
            Log.debug (stm);
            DatabaseData result = connection.executeSQL(stm);
            int resultRows = result.getRowNumber();
            String[] cns = new String[resultRows];
            for (int i=0; i<resultRows; i++) {
                cns[i] = (String)result.getValue(DataConstants.COMM_NAME, i);
            }
            return cns;
        }catch(Exception e){
            throw new BaseException("SQLException:" + e.getMessage());
        }

    }*/

    /**
    *Get all communities visible for an user
    */
    /*public static String[] getCommunitiesVisibleFromUser(String user_id)
    throws BaseException{
        try{
            DBConnection connection = InstanceManager.getCurrentInstance().getDBConnection();
            String stm = "SELECT UNIQUE " + "b."+ DataConstants.COMM_ID  
			+ " FROM " + DataConstants.TABLE_COMMUNITY  + " b ," + DataConstants.TABLE_USER_COMMUNITY + " c "           
			+ " WHERE b."  + DataConstants.COMM_ID + "=c." + DataConstants.COMM_ID 
			+ " AND c."    + DataConstants.USER_ID + "=" + user_id
			+ " AND c."     + DataConstants.COMM_VISIBLE + "=1"
			;
            //Log.debug (stm);
            DatabaseData result = connection.executeSQL(stm);
            int resultRows = result.getRowNumber();
            String[] cns = new String[resultRows];
            for (int i=0; i<resultRows; i++) {
                cns[i] = result.getValue(1, i).toString();
            }
            return cns;
        }catch(Exception e){
	    e.printStackTrace();
            throw new BaseException("SQLException:" + e.getMessage());
        }

    }*/
    
    public static String[] getCommunitiesInCategoryFromUser(String category, String user_id)
    throws BaseException{
        
        DBConnection conn = null;
        try{
            
            conn = ConnectionPool.getInstance().requestConnection();
            
            String stm = "SELECT DISTINCT "
                +  "b."+ CommunityAttributes.COMMUNITY_NAME   
                + " FROM " + DBConstants.TABLE_CATEGORY +" a , "
                + DBConstants.TABLE_COMMUNITY  + " b ,"           
                + DBConstants.TABLE_USER_COMMUNITY      +" c  "           
                + " WHERE a."  + CategoryAttributes.CATEGORY_ID + "="
                + " b."        + CommunityAttributes.COMMUNITY_CATEGORY 
                + " AND b."    + CommunityAttributes.COMMUNITY_ID + "="
                +      " c."   + CommunityAttributes.COMMUNITY_ID 
                + " AND c."    + UserAttributes.USER_ID + "="
                + " ? "
                + " AND a."      + CategoryAttributes.CATEGORY_NAME   + "="
                + " ? "
                ;
            logger.debug (stm);
            
            PreparedStatement pstmt = conn.prepareStatement( stm );
            pstmt.setObject( 1, user_id , java.sql.Types.BIGINT);
			pstmt.setString( 2, category );
            
			DatabaseData result = conn.executePrepQuery(pstmt);	
			pstmt.close();
			
            int resultRows = result.getRowNumber();
            String[] cns = new String[resultRows];
            for (int i=0; i<resultRows; i++) {
                cns[i] = (String)result.getValue(CommunityAttributes.COMMUNITY_NAME, i);
            }
            return cns;
        }catch(Exception e){
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
     * @return an array of
     * {@link ro.cst.vems.data.community.CommunityAttributes CommunityAttributes}
     * with all communities in the sistem.
     */
    public static CommunityAttributes[] getAllCommunities(HttpSession session)
        throws BaseException, IOException {

        CommunityFilter cf = new CommunityFilter();
        return CommunityManager.getCommunities(cf, true,session);
    }

    public static CommunityAttributes[] getAllCommunities(CommunityFilter cf,HttpSession session)
        throws BaseException, IOException {       
        
	return CommunityManager.getCommunities(cf, true,session);
    }




    /**
     * receives as parameter a community attribute;
     * @return a hashtable containing basic information related to a community
     * such as: {@link ro.cst.vems.data.CommunityAttributes.NAME community name}
     * , {@link ro.cst.vems.data.CommunityAttributes.CATEGORY community category}
     * , {@link ro.cst.vems.data.CommunityAttributes.DESCRIPTION community description}
     * and so on;
     * @exception BaseException if the community having the
     * requested name was not found;
     */
    public static Hashtable getCommunityInfo(CommunityAttributes ca)
        throws BaseException{

        Hashtable hash = new Hashtable();
		hash.put(CommunityAttributes.COMMUNITY_ID,ca.getAttribute(CommunityAttributes.ID).toString());
        hash.put(CommunityAttributes.COMMUNITY_NAME,(String)ca.getAttribute(CommunityAttributes.NAME));
        hash.put(CommunityAttributes.COMMUNITY_CATEGORY, new Long(ca.getAttribute(CommunityAttributes.CATEGORY).toString()));
        hash.put(CommunityAttributes.COMMUNITY_DESCRIPTION, (ca.getAttribute(CommunityAttributes.DESCRIPTION)==null)?"N/A":(String)ca.getAttribute(CommunityAttributes.DESCRIPTION));
        hash.put(CommunityAttributes.COMMUNITY_CTIME, (ca.getAttribute(CommunityAttributes.CTIME)==null)?new BigInteger("0"):(BigInteger)ca.getAttribute(CommunityAttributes.CTIME));
        hash.put(CommunityAttributes.COMMUNITY_LAST_ACCESS, (ca.getAttribute(CommunityAttributes.LAST_ACCESS)==null)?new BigDecimal(0):(BigDecimal)ca.getAttribute(CommunityAttributes.LAST_ACCESS));
        hash.put(CommunityAttributes.COMMUNITY_SEE_ALSO, (ca.getAttribute(CommunityAttributes.SEE_ALSO)==null)?"N/A":(String)ca.getAttribute(CommunityAttributes.SEE_ALSO));
        hash.put(CommunityAttributes.COMMUNITY_ADMIN, new Long(ca.getAttribute(CommunityAttributes.COMM_ADMIN).toString()));
        hash.put(CommunityAttributes.COMMUNITY_ADDRESS, (ca.getAttribute(CommunityAttributes.ADDRESS)==null)?"N/A":(String)ca.getAttribute(CommunityAttributes.ADDRESS));
        hash.put(CommunityAttributes.COMMUNITY_EMAIL, (ca.getAttribute(CommunityAttributes.EMAIL)==null)?"N/A":(String)ca.getAttribute(CommunityAttributes.EMAIL));
        hash.put(CommunityAttributes.COMMUNITY_CONTACT, (ca.getAttribute(CommunityAttributes.CONTACT)==null)?"N/A":(String)ca.getAttribute(CommunityAttributes.CONTACT));
        hash.put(CommunityAttributes.COMMUNITY_PHONE, (ca.getAttribute(CommunityAttributes.PHONE)==null)?"N/A":(String)ca.getAttribute(CommunityAttributes.PHONE));
        hash.put(CommunityAttributes.COMMUNITY_TEMPLATE, new BigDecimal(0));
        hash.put(CommunityAttributes.COMMUNITY_CODE, (ca.getAttribute(CommunityAttributes.CODE)==null)?"N/A":(String)ca.getAttribute(CommunityAttributes.CODE));
        hash.put(CommunityAttributes.COMMUNITY_OFFSET, (ca.getAttribute(CommunityAttributes.OFFSET)==null?new Integer(0):new Integer(ca.getAttribute(CommunityAttributes.OFFSET).toString())));
        hash.put(CommunityAttributes.COMMUNITY_AUTOFILEID, (ca.getAttribute(CommunityAttributes.AUTOFILEID)==null?new Integer(0): new Integer(ca.getAttribute(CommunityAttributes.AUTOFILEID).toString())));
        hash.put(CommunityAttributes.COMMUNITY_DEFAULT_SLA, (ca.getAttribute(CommunityAttributes.DEFAULT_SLA)==null?new BigInteger("0"):new BigInteger(ca.getAttribute(CommunityAttributes.DEFAULT_SLA).toString())) );
        hash.put(CommunityAttributes.COMMUNITY_SUPPORT_EMAIL, (ca.getAttribute(CommunityAttributes.SUPPORT_EMAIL)==null?MailConfig.getTicketSupportEmailAddress():ca.getAttribute(CommunityAttributes.SUPPORT_EMAIL).toString()) );
        hash.put(CommunityAttributes.COMMUNITY_IGNORE_PRIVACY_STATEMENT, (ca.getAttribute(CommunityAttributes.IGNORE_PRIVACY_STATEMENT)==null?Boolean.FALSE:new Boolean(ca.getAttribute(CommunityAttributes.IGNORE_PRIVACY_STATEMENT).toString())) );
        hash.put(CommunityAttributes.COMMUNITY_PLACEHOLDER, (ca.getAttribute(CommunityAttributes.PLACEHOLDER)==null?"N/A":ca.getAttribute(CommunityAttributes.PLACEHOLDER).toString()));
        hash.put(CommunityAttributes.COMMUNITY_NUMBER_OF_UPDATES, (ca.getAttribute(CommunityAttributes.NUMBER_OF_UPDATES) == null ? new Integer(0) : new Integer(ca.getAttribute(CommunityAttributes.NUMBER_OF_UPDATES).toString())));
        hash.put(CommunityAttributes.COMMUNITY_NUMBER_OF_DAYS, (ca.getAttribute(CommunityAttributes.NUMBER_OF_DAYS) == null ? new Integer(0) : new Integer(ca.getAttribute(CommunityAttributes.NUMBER_OF_DAYS).toString())));
        return hash;
    }

    /**
     * receives as parameter a community name;
     * @return a hashtable containing basic information related to a community
     * such as: {@link ro.cst.vems.data.CommunityAttributes.NAME community name}
     * , {@link ro.cst.vems.data.CommunityAttributes.CATEGORY community category}
     * , {@link ro.cst.vems.data.CommunityAttributes.DESCRIPTION community description}
     * and so on;
     * @exception BaseException if the community having the
     * requested name was not found;
     */
    public static Hashtable getCommunityInfo(String commId)
        throws BaseException{
        CommunityAttributes ca = CommunityUtils.getCommunityFromId(Long.parseLong(commId));
        return CommunityUtils.getCommunityInfo(ca);
    }
        
	/**
	 * Create a new 
	 * {@link ro.cst.vems.data.CommunityAttributes CommunityAttributes} object.
	 * Parameters:
	 *  @param hash A hashtable object with all necesary informations about the new community object.
	 */
	public static CommunityAttributes createCommunity(Hashtable hash)
		throws BaseException, DataException {

		long commAdmin = ((Long)hash.get(CommunityAttributes.COMMUNITY_ADMIN)).longValue();
		if(commAdmin<=0) {
			commAdmin = UserManager.getTSAdminUser().getID().longValue();
		}
			
		CommunityAttributes ca = new CommunityAttributes((String)hash.get(CommunityAttributes.COMMUNITY_NAME),
														 (String)hash.get(CommunityAttributes.COMMUNITY_DESCRIPTION),
														 commAdmin ,
														 ((Long)hash.get(CommunityAttributes.COMMUNITY_CATEGORY)).longValue(),
														 (String)hash.get(CommunityAttributes.COMMUNITY_SEE_ALSO),
														 (String)hash.get(CommunityAttributes.COMMUNITY_ADDRESS),
														 (String)hash.get(CommunityAttributes.COMMUNITY_EMAIL),
														 (String)hash.get(CommunityAttributes.COMMUNITY_PHONE),
														 (String)hash.get(CommunityAttributes.COMMUNITY_CONTACT),
														 (BigInteger)hash.get(CommunityAttributes.COMMUNITY_CTIME),
														 (String)hash.get(CommunityAttributes.COMMUNITY_CODE),
														 (Integer)hash.get(CommunityAttributes.COMMUNITY_OFFSET),
														 (Integer)hash.get(CommunityAttributes.COMMUNITY_AUTOFILEID),
                                                         (BigInteger)hash.get(CommunityAttributes.COMMUNITY_DEFAULT_SLA),
                                                         (String)hash.get(CommunityAttributes.COMMUNITY_SUPPORT_EMAIL),
                                                         (Boolean)hash.get(CommunityAttributes.COMMUNITY_IGNORE_PRIVACY_STATEMENT),
                                                         (String)hash.get(CommunityAttributes.COMMUNITY_PLACEHOLDER),
                                                         (Integer)hash.get(CommunityAttributes.COMMUNITY_NUMBER_OF_UPDATES),
                                                         (Integer)hash.get(CommunityAttributes.COMMUNITY_NUMBER_OF_DAYS)
														 );
														 

					
		ca = CommunityManager.createNewCommunity(ca, hash, hash.get(UploadPolicyDoc.SESSION_ID).toString());
				    	
		return ca;
	}

	/**
	 * Modify all the informations related to a
	 * {@link ro.cst.tsearch.CommunityAttributes CommunityAttributes} object.
	 * Parameters:
	 *  @param hash A hashtable object with all necesary informations about the new community attributes.
	 */
	public static void modifyCommunity(Hashtable hash)
		throws BaseException{

		CommunityAttributes ca = new CommunityAttributes( (String)hash.get(CommunityAttributes.COMMUNITY_NAME),
															    (String)hash.get(CommunityAttributes.COMMUNITY_DESCRIPTION),
																((Long)hash.get(CommunityAttributes.COMMUNITY_ADMIN)).longValue(),
																((Long)hash.get(CommunityAttributes.COMMUNITY_CATEGORY)).longValue(),
																(String)hash.get(CommunityAttributes.COMMUNITY_SEE_ALSO),
																(String)hash.get(CommunityAttributes.COMMUNITY_ADDRESS),
																(String)hash.get(CommunityAttributes.COMMUNITY_EMAIL),
																(String)hash.get(CommunityAttributes.COMMUNITY_PHONE),
																(String)hash.get(CommunityAttributes.COMMUNITY_CONTACT),
																(BigInteger)hash.get(CommunityAttributes.COMMUNITY_CTIME),
																(String)hash.get(CommunityAttributes.COMMUNITY_CODE),
																(Integer)hash.get(CommunityAttributes.COMMUNITY_OFFSET),
																(Integer)hash.get(CommunityAttributes.COMMUNITY_AUTOFILEID),
                                                                (BigInteger)hash.get(CommunityAttributes.COMMUNITY_DEFAULT_SLA),
                                                                (String)hash.get(CommunityAttributes.COMMUNITY_SUPPORT_EMAIL),
                                                                (Boolean)hash.get(CommunityAttributes.COMMUNITY_IGNORE_PRIVACY_STATEMENT),
                                                                (String)hash.get(CommunityAttributes.COMMUNITY_PLACEHOLDER),
                                                                (Integer)hash.get(CommunityAttributes.COMMUNITY_NUMBER_OF_UPDATES),
                                                                (Integer)hash.get(CommunityAttributes.COMMUNITY_NUMBER_OF_DAYS)
																);

		//CommunityAttributes oca =  getCommunityFromName((String)hash.get(CommunityAttributes.COMMUNITY_NAME));
		save(ca, (String)hash.get(CommunityAttributes.COMMUNITY_ID), hash);
	}
	
	
	/** Update the attributes of a community into database.
	 * @param ca The new attreibutes.
	 * @param oca The attributes which have to be replaced.
	 */
	public static void save(CommunityAttributes ca, String commId, Hashtable hashWithFiles)
		throws BaseException {
        ca.setID(new BigDecimal(commId));
        Boolean ignorePrivacyStm = (Boolean) ca.getAttribute(CommunityAttributes.IGNORE_PRIVACY_STATEMENT);
        
		String stm = "UPDATE "
			+ DBConstants.TABLE_COMMUNITY
			+ " SET "
			+ CommunityAttributes.COMMUNITY_CATEGORY + "=" + ca.getAttribute(CommunityAttributes.CATEGORY) + ", "
			+ CommunityAttributes.COMMUNITY_NAME + "= ?, "
			+ CommunityAttributes.COMMUNITY_DESCRIPTION + "=?,"
			+ CommunityAttributes.COMMUNITY_ADMIN + "=" + ca.getAttribute(CommunityAttributes.COMM_ADMIN) + ","
			+ CommunityAttributes.COMMUNITY_ADDRESS + "='" + ca.getAttribute(CommunityAttributes.ADDRESS) + "', "
			+ CommunityAttributes.COMMUNITY_PHONE + "='" + ca.getAttribute(CommunityAttributes.PHONE) + "', "
			+ CommunityAttributes.COMMUNITY_EMAIL + "='" + ca.getAttribute(CommunityAttributes.EMAIL) + "', "
			+ CommunityAttributes.COMMUNITY_CONTACT + "='" + ca.getAttribute(CommunityAttributes.CONTACT) + "', "
			+ CommunityAttributes.COMMUNITY_SEE_ALSO + "=?, "
			+ CommunityAttributes.COMMUNITY_CODE + "='" + ca.getAttribute(CommunityAttributes.CODE) + "', "
			+ CommunityAttributes.COMMUNITY_OFFSET + "=" + ca.getAttribute(CommunityAttributes.OFFSET) + ","
			+ CommunityAttributes.COMMUNITY_AUTOFILEID + "=" + ca.getAttribute(CommunityAttributes.AUTOFILEID) + ","
            + CommunityAttributes.COMMUNITY_DEFAULT_SLA + "=" + ca.getAttribute(CommunityAttributes.DEFAULT_SLA) + ","
            + CommunityAttributes.COMMUNITY_SUPPORT_EMAIL + "='" + ca.getAttribute(CommunityAttributes.SUPPORT_EMAIL) + "', "
            + CommunityAttributes.COMMUNITY_IGNORE_PRIVACY_STATEMENT + "=" + (ignorePrivacyStm.equals(Boolean.TRUE)?"1":"0") + " , "
            + CommunityAttributes.COMMUNITY_PLACEHOLDER + "='" + ca.getAttribute(CommunityAttributes.PLACEHOLDER) + "' " + " , "
            + CommunityAttributes.COMMUNITY_NUMBER_OF_UPDATES + "=" + ca.getAttribute(CommunityAttributes.NUMBER_OF_UPDATES) + ", "
            + CommunityAttributes.COMMUNITY_NUMBER_OF_DAYS + "=" + ca.getAttribute(CommunityAttributes.NUMBER_OF_DAYS)
			+ " WHERE "
			+ CommunityAttributes.COMMUNITY_ID
			+ "= ? ";
		
		DBConnection conn = null;
		try{
		    
		    conn = ConnectionPool.getInstance().requestConnection();
		    
	        PreparedStatement pstmt = conn.prepareStatement( stm );
	        
	        pstmt.setObject( 1, ca.getAttribute(CommunityAttributes.NAME) , java.sql.Types.VARCHAR);
	        pstmt.setObject( 2, ca.getAttribute(CommunityAttributes.DESCRIPTION) , java.sql.Types.VARCHAR);
	        pstmt.setObject( 3, ca.getAttribute(CommunityAttributes.SEE_ALSO) , java.sql.Types.VARCHAR);
	        pstmt.setObject( 4, commId , java.sql.Types.BIGINT);
	        
			pstmt.executeUpdate();	
			pstmt.close();

		//	UploadPolicyDoc.updateDB(null, UploadPolicyDoc.COMMUNITY_EXISTS, ca);
			conn.commit();
			conn.setAutoCommit(false);
			File photo  = (File)hashWithFiles.get(CommunityAttributes.COMMUNITY_LOGO);
			File termsOfUse = (File)hashWithFiles.get(CommunityAttributes.COMMUNITY_TERMS_OF_USE);
			File privacyState = (File)hashWithFiles.get(CommunityAttributes.COMMUNITY_PRIVACY_STATEMENT);
			editFileEntryInDB(commId, 
					photo, 
					null, 
					DBConstants.TABLE_COMMUNITY_LOGO, 
					CommunityAttributes.COMMUNITY_LOGO, 
					null, true);
			editFileEntryInDB(commId, 
					termsOfUse, 
					privacyState, 
					DBConstants.TABLE_COMMUNITY_TERMS_OF_USE, 
					CommunityAttributes.COMMUNITY_TERMS_OF_USE, 
					CommunityAttributes.COMMUNITY_PRIVACY_STATEMENT, false);
			
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

	/**Deletes this entry from database.
	 * @param ca the community object which have to be deleted.
	 */
	
	public static void delete(String commId)
		throws BaseException {
        
		try {
			SimpleJdbcTemplate sjt = DBManager.getSimpleTemplate();
			
			sjt.update("DELETE FROM " + DBConstants.TABLE_COMMUNITY_SEQ + " WHERE comm_id = ?", commId);
            sjt.update("DELETE FROM " + DBConstants.TABLE_COMMUNITY + " WHERE " + 
            		CommunityAttributes.COMMUNITY_ID + "= ?", commId);
            sjt.update("DELETE FROM " + DBConstants.TABLE_COMMUNITY_SITES + " WHERE " +
            		DBConstants.FIELD_COMMUNITY_SITES_COMMUNITY_ID + "= ?", commId);
            sjt.update("DELETE FROM " + DBConstants.TABLE_COUNTY_COMMUNITY + " WHERE " +
            		DBConstants.FIELD_COUNTY_COMMUNITY_COMMUNITY_ID + "= ?", commId);
			
		} catch (Exception e) {
			logger.error("Error while deleting a community " + commId, e);
			throw new BaseException("SQLException:" + e.getMessage());
		}
		
	}

	public static boolean hasPhoto(CommunityAttributes ca){
	 byte[] photo = ca.getLOGO();
		 if(photo != null && photo.length !=0)
		 	return true;
		 return false; 
	}
	
	public static boolean hasEntry(BigDecimal commId, String tableName) throws BaseException {
	    
	    DBConnection conn = null;
		try {
		    
		    conn = ConnectionPool.getInstance().requestConnection();
			String stm =
				"SELECT "
					+ CommunityAttributes.COMMUNITY_ID
					+ " FROM "
					+ tableName /*DBConstants.TABLE_COMMUNITY_LOGO*/
					+ " WHERE "
					+ CommunityAttributes.COMMUNITY_ID
					+ "="
					+ commId;
			
			DatabaseData result = conn.executeSQL(stm);
			int resultRows = result.getRowNumber();

			if (resultRows != 0)
				return true;

			return false;

		} catch (Exception e) {
			throw new BaseException("SQLException" + e.getMessage());
		} finally {
			try{
			    ConnectionPool.getInstance().releaseConnection(conn);
			}catch(BaseException e){
			    throw new BaseException("SQLException:" + e.getMessage());
			}			
		}
	}

	public static byte[] getCommLogo(CommunityAttributes ca){
		return ca.getLOGO();
	}
	
	private static Map<BigDecimal, byte[]> cachedLogos = new ConcurrentHashMap<BigDecimal, byte[]>();
	
	public static void invalidateLogo(BigDecimal commId){
		if(cachedLogos.containsKey(commId)){
			try{
				cachedLogos.remove(commId);
			}catch(RuntimeException e){}
		}
	}
	
	public static byte[] getCommLogo(BigDecimal commId) throws BaseException {
		byte[] logo = cachedLogos.get(commId);
		if(logo == null){
			logo = loadCommLogo(commId);
			cachedLogos.put(commId, logo);
		}
		return logo;
	}
	
	public static byte[] loadCommLogo(BigDecimal commId)
		throws BaseException {

		DBConnection conn = null;			
		// beacause we have html file generated by pdf conversion ...outerside of Instance Manager context
		
		try {
		    
		    conn = ConnectionPool.getInstance().requestConnection();
			String sql = "SELECT "
				+ CommunityAttributes.COMMUNITY_LOGO
				+ " FROM "
				+ DBConstants.TABLE_COMMUNITY_LOGO
				+ " WHERE "
				+ CommunityAttributes.COMMUNITY_ID
				+ "= ?";
				//+ commId; 
			PreparedStatement pstmt = conn.prepareStatement(sql);
			//ResultSet resultBlob = stmt.executeQuery(sql);

			//if (resultBlob.next()) {
			//	Blob blobPhoto = resultBlob.getBlob(1);					
			//	return blobPhoto.getBytes(1, (int) blobPhoto.length());					
			//} else
			//	return null;
			pstmt.setLong(1, commId.longValue());
			ResultSet rs = pstmt.executeQuery();
			byte[] blob=null;
			if(rs.next()){
				blob = rs.getBytes(1);
			}
			return blob;
			
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
	/**
	 * 
	 * @param commId
	 * @param fileToStore1
	 * @param fileToStore2
	 * @param tableName
	 * @param fieldName1
	 * @param fieldName2
	 * @return
	 * 
	 * only fieldName2 and fileToStore2 could be null!!!!
	 */
	public static boolean editFileEntryInDB(String commId, 
									File fileToStore1,
									File fileToStore2,
									String tableName, 
									String fieldName1,
									String fieldName2,
									boolean isLogo) {
		DBConnection conn = null;
		//Statement stmt = null;
		//ResultSet resultBlob = null;
		String stm = "";
		try {
			conn = ConnectionPool.getInstance().requestConnection();
			conn.setAutoCommit(false);
			if (fileToStore1 != null || fileToStore2 != null) {
				if (hasEntry(new BigDecimal(commId), tableName/*DBConstants.TABLE_COMMUNITY_LOGO*/)) {
					String qry =
						" DELETE from "
							+ tableName /*DBConstants.TABLE_COMMUNITY_LOGO*/
							+ " WHERE "
							+ CommunityAttributes.COMMUNITY_ID
							+ "= ? ";
		        	PreparedStatement pstmt = conn.prepareStatement( qry );
		        	pstmt.setObject( 1, commId , java.sql.Types.BIGINT);
		        	pstmt.executeUpdate();			
					
				}
				
				if (fileToStore1 != null || fileToStore2 != null) {
					stm = "INSERT INTO " + tableName /*DBConstants.TABLE_COMMUNITY_LOGO*/
							+ " ( " + CommunityAttributes.COMMUNITY_ID 
							+ (fileToStore1 != null ? ", " + fieldName1:"")/*CommunityAttributes.COMMUNITY_LOGO*/ 
							+ (fileToStore2 != null ? ", " + fieldName2:"") + " ) " 
							+ " VALUES ( ? " 
							+ (fileToStore1 != null ? ", ?":"") 
							+ (fileToStore2 != null ? ", ?":"") + " ) ";
					
					//conn.executeSQL(stm);
					PreparedStatement pstmt = conn.prepareStatement(stm);
					pstmt.setLong(1, Long.parseLong(commId));
					FileInputStream fis1 = null;
					FileInputStream fis2 = null;
					if(fileToStore1!=null){
						fis1 = new FileInputStream(fileToStore1);
						pstmt.setBinaryStream(2, fis1, (int)fileToStore1.length());
						if(fileToStore2!=null) {
							fis2 = new FileInputStream(fileToStore2);
							pstmt.setBinaryStream(3, new FileInputStream(fileToStore2), (int)fileToStore2.length());
						}
						
					} else {
						if(fileToStore2!=null){
							fis2 = new FileInputStream(fileToStore2);
							pstmt.setBinaryStream(2, new FileInputStream(fileToStore2), (int)fileToStore2.length());
						}
					}
					
					pstmt.executeUpdate();
					pstmt.close();
					if(fis1!=null)	fis1.close();
					if(fis2!=null)	fis2.close();
					conn.commit();
									
					
					if(fileToStore1 != null && fileToStore1.delete()==true){
						logger.info("File "+ fileToStore1.getName() + " succesufully deleted from WEB SERVER!");
					}else if(fileToStore1 != null){
						logger.info("Cannot delete File "+ fileToStore1.getName() + " from WEB SERVER!");				
					}	
					if(fileToStore2 != null && fileToStore2.delete() == true){
						logger.info("File "+ fileToStore2.getName() + " succesufully deleted from WEB SERVER!");
					}else if (fileToStore2 != null && fileToStore2.delete() == false){
						logger.info("Cannot delete File "+ fileToStore2.getName() + " from WEB SERVER!");				
					}
				}
			} else if (fileToStore1 == null && isLogo) {
				if (!hasEntry(new BigDecimal(commId), tableName/*DBConstants.TABLE_COMMUNITY_LOGO*/)) {
					if(!tableName.equalsIgnoreCase(DBConstants.TABLE_COMMUNITY_LOGO)) {
						stm = "INSERT INTO " + tableName /*DBConstants.TABLE_COMMUNITY_LOGO*/
							+ " ( " + CommunityAttributes.COMMUNITY_ID 
							+ ", " + fieldName1 + ") " 
							+ " VALUES ( " + Long.parseLong(commId) + ", "
							+ "(SELECT LOGO FROM TS_COMMUNITY_LOGO WHERE COMM_ID=(SELECT COMM_ID FROM TS_COMMUNITY WHERE COMM_NAME LIKE '%Default%')))";
						conn.executeSQL(stm);
					} else {	
						//this is because Mysql does not allow to update a table that is in the FROM clause
						stm = "INSERT INTO " + DBConstants.TABLE_EXTRA_TABLE + " (FIELD_BLOB) " +
						" VALUES (SELECT LOGO FROM TS_COMMUNITY_LOGO WHERE COMM_ID=(SELECT COMM_ID FROM TS_COMMUNITY WHERE COMM_NAME LIKE '%Default%'))";
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
			    ConnectionPool.getInstance().releaseConnection(conn);
			}catch(BaseException e){
			}			
		}
		return true;
	}
	
	
	
	public static String getHTMLStylePolicy(long commId, long userId) {

		List<CommunityUserTemplatesMapper> templates = DBManager .getSimpleTemplate().query( 
						UserUtils.SQL_TEMPLATES_LIST_FOR_USER_PROFILE,
						new CommunityUserTemplatesMapper(), userId, commId);

		String retHTML = "";

		retHTML += "<table>";
				
		List<CommunityUserTemplatesMapper> justBPTemplates = new ArrayList<CommunityUserTemplatesMapper>();
		
		for (CommunityUserTemplatesMapper cutm : templates) {
			if(cutm.isCodeBookLibrary()) {
				justBPTemplates.add(cutm);
			}
		}
		
		int boilerPlatesTemplates = justBPTemplates.size();
		
		templates.removeAll(justBPTemplates);
		templates.addAll(0, justBPTemplates);
		
//		Collections.sort(templates, new Comparator<CommunityUserTemplatesMapper>() {
//
//			@Override
//			public int compare(CommunityUserTemplatesMapper o1,
//					CommunityUserTemplatesMapper o2) {
//				if(o1.isCodeBookLibrary()) {
//					return -1;
//				}
//				return 1;
//			}
//		});
		
		for (CommunityUserTemplatesMapper cutm : templates) {
		
			long templateId = cutm.getTemplateId();
			String tn = cutm.getName();
			String tsn = cutm.getShortName();
			
			if(!StringUtils.areAllEmpty(tn,tsn)) { 
				boolean isBoiler = (boilerPlatesTemplates>=1 && cutm.isCodeBookLibrary() );
				boolean userHasTemplate = (cutm.getUserId() == userId);
				
				retHTML += "<tr>";
				retHTML += "<td>";
				retHTML += "<input  type='"+ (isBoiler?"radio":"checkbox")+"' name='"+UserAttributes.USER_TEMPLATES+"' value='"+ templateId +"' " + (userHasTemplate?" checked ":"")+ " />";
				retHTML += "</td>";	
				retHTML += /*"<td>" + tn+ "</td>" +*/ "<td>"+ "<span title='" + tsn+ "'>"+ tn + "</span>"+"</td>";
				retHTML += "<td>";
				
				//retHTML += "<div id='span_user_templates_product_"+templateId+"' style='"+(userHasTemplate?"display:inline;":"display:none;")+"'>";
				LinkedHashMap<Integer,HashMap> products = CommunityProducts.getProductsMap(commId);
				for (Entry<Integer,HashMap> e : products.entrySet()) {
					String name = (String) e.getValue().get(Products.FIELD_NAME);
					retHTML += "<input title='"+name+"' name='user_templates_product_"+templateId+"' type='checkbox' "
							+ (UserUtils.isTemplateEnabledOnProduct(cutm.getEnableProduct(), e.getKey()) ? " checked " : "")
							+ " value='"+e.getKey()+"'/>";
				}
				retHTML += "&nbsp;&nbsp;&nbsp;<input title='Internal Only' name='user_templates_product_" + templateId + "' type='checkbox' "
						+ (UserUtils.isTemplateEnabledOnProduct(cutm.getEnableProduct(), Products.INTERNAL_ONLY) ? " checked " : "")
						+ " value='" + Products.INTERNAL_ONLY + "'/>";
				retHTML += "<input title='RPC Associate Only' name='user_templates_product_" + templateId + "' type='checkbox' "
						+ (UserUtils.isTemplateEnabledOnProduct(cutm.getEnableProduct(), Products.RPC_ASSOCIATE_ONLY) ? " checked " : "") 
						+ " value='" + Products.RPC_ASSOCIATE_ONLY + "'/>";
				retHTML += "</div>";
				retHTML += "</td>";
				retHTML += "<td>";
				String templateName = cutm.getPath();
				if (UserUtils.templateIsDoc(templateName) || UserUtils.templateIsHtml(templateName)) {
					int exportFormat = cutm.getExportFormat();
					String selectedTiff = (TemplateBuilder.TIFF_EXPORT_FORMAT==exportFormat)?" selected":"";
					String selectedPdf = (TemplateBuilder.PDF_EXPORT_FORMAT==exportFormat)?" selected":"";
					String selectedNoChange = (TemplateBuilder.NO_CHANGE_EXPORT_FORMAT==exportFormat)?" selected":"";
					retHTML += "<SELECT size=1 title=\"Export Format\" name =\"" 
							+ RequestParams.USER_TEMPLATES_EXPORT_FORMAT + "_" + templateId + "\" id=\"" 
							+ RequestParams.USER_TEMPLATES_EXPORT_FORMAT + "_" + templateId + "\">" +
							"<option value=\"" + TemplateBuilder.TIFF_EXPORT_FORMAT + "\"" + selectedTiff + ">TIFF</option>" +
							"<option value=\"" + TemplateBuilder.PDF_EXPORT_FORMAT + "\"" + selectedPdf + ">PDF</option>" +
							"<option value=\"" + TemplateBuilder.NO_CHANGE_EXPORT_FORMAT + "\"" + selectedNoChange + ">No change</option>" +
							"</SELECT>";
				}
				retHTML += "</td>";
				retHTML += "</tr>";
			}
		}
		retHTML += "</table>";
		return retHTML.equals("<table></table>") ? "<i>No additional documents</i>" : retHTML;
	}
	

	public static String getTSDTemplatePath(long commId) throws TemplatesException{
		String fSep = File.separator;
		String templatesPath = BaseServlet.FILES_PATH + fSep + TEMPL_PATH_OUT;
		String TSDTemplPath = templatesPath + fSep + TSD_TEMPL_PATH;
		String commTSDTeplatePath = TSDTemplPath + fSep + commId;
		
		commTSDTeplatePath=commTSDTeplatePath.replaceAll("[/][/]","/");
		
		if (!FileUtils.checkDirectory(templatesPath))
			throw new TemplatesException("The templates folder >>> " + templatesPath + " <<< couldn't be created");
		if (!FileUtils.checkDirectory(TSDTemplPath))
			throw new TemplatesException("The templates folder >>> " + TSDTemplPath + " <<< couldn't be created");
		if (!FileUtils.checkDirectory(commTSDTeplatePath))
			throw new TemplatesException("The templates folder >>> " + commTSDTeplatePath + " <<< couldn't be created");
		return commTSDTeplatePath + fSep;
	}
	
	
}








