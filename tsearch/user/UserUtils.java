/*
 * Text here
 */

package ro.cst.tsearch.user;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.sql.Blob;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Vector;

import org.apache.log4j.Logger;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.simple.ParameterizedRowMapper;

import com.stewart.ats.tsrindex.client.template.TemplatesInitUtils;

import ro.cst.tsearch.community.CommunityAttributes;
import ro.cst.tsearch.community.CommunityProducts;
import ro.cst.tsearch.community.CommunityUtils;
import ro.cst.tsearch.community.Products;
import ro.cst.tsearch.database.ConnectionPool;
import ro.cst.tsearch.database.DBConnection;
import ro.cst.tsearch.database.DBManager;
import ro.cst.tsearch.database.DatabaseData;
import ro.cst.tsearch.database.rowmapper.CommunityTemplatesMapper;
import ro.cst.tsearch.database.rowmapper.CommunityUserTemplatesMapper;
import ro.cst.tsearch.exceptions.BaseException;
import ro.cst.tsearch.servers.parentsite.State;
import ro.cst.tsearch.servlet.TemplatesServlet;
import ro.cst.tsearch.templates.TemplateBuilder;
import ro.cst.tsearch.templates.TemplatesException;
import ro.cst.tsearch.utils.DBConstants;
import ro.cst.tsearch.utils.FormatNumber;
import ro.cst.tsearch.utils.RequestParams;
import ro.cst.tsearch.utils.StrUtil;
import ro.cst.tsearch.utils.StringUtils;
import ro.cst.tsearch.utils.URLMaping;

/**
 *
 */
public class UserUtils { 

	private static final String RESOURCES_PATH = URLMaping.IMAGES_DIR + "/user/";
	//se tin imaginile specifice tipului de fisier. 
	private static HashMap hashCorespImg;

	private static String IMG_FILE_UNKNOWN;
	
	public static final String TEMPLATE_ID = "templateId";
	public static final String TEMPLATE_SHORT = "templateShortName";
	public static final String TEMPLATE_NAME = "templateName";
	public static final String TEMPLATE_PATH = "templatePath";
	public static final String TEMPLATE_LAST_UPDATE = "templateLastUpdate";
	public static final String TEMPLATE_STYLE_BOLD = "templateStyleBold";	
	public static final String TEMPLATE_CONTENT	 = "templateContent";
	public static final String TEMPLATE_COMM			 = "templateComm";
	
	
	
	//DB Fields
	public static final String DB_TEMPLATE_ID = "TEMPLATE_ID";
	public static final String DB_TEMPLATE_SHORT = "SHORT_NAME";
	public static final String DB_TEMPLATE_NAME = "NAME";
	public static final String DB_TEMPLATE_PATH = "PATH";
	public static final String DB_TEMPLATE_COMM_ID = "COMM_ID";
	public static final String DB_TEMPLATE_LAST_UPDATE = "LAST_UPDATE";
	
	public static final String DB_USER_TEMPLATES = "DELIV_TEMPLATES";
	public static final String DB_USER_TEMPLATES_DELIMITER = ":";
	public static final String DB_USER_COMMA_DELIMITER = ",";
	
	
	public static final int FILTER_BOILER_PLATES_EXCLUDE = 1;
	public static final int FILTER_IGNORE_USER = 2;
	public static final int FILTER_BOILER_PLATES_ONLY = 3;
	public static final int FILTER_TSD_ONLY = 4;
	public static final int FILTER_TSD_AND_BP_ONLY = 5;
	
	private static final Logger logger = Logger.getLogger(UserUtils.class);
	

	static {

		IMG_FILE_UNKNOWN =
			new String(RESOURCES_PATH +  "SmPage0.gif");
		hashCorespImg = new HashMap();
		hashCorespImg.put(
			".pdf",
			new String(
				RESOURCES_PATH + "pdficonsmall.gif"));
		hashCorespImg.put(
			".doc",
			new String(
				RESOURCES_PATH + "SmFile_doc.gif"));
		hashCorespImg.put(
			".jpg",
			new String(
				RESOURCES_PATH + "SmFile_jpg.gif"));
		hashCorespImg.put(
			".ppt",
			new String(
				RESOURCES_PATH + "SmFile_ppt.gif"));
		hashCorespImg.put(
			".txt",
			new String(
				RESOURCES_PATH + "SmFile_txt.gif"));
		hashCorespImg.put(
			".xls",
			new String(
				RESOURCES_PATH + "SmFile_xls.gif"));
		hashCorespImg.put(
			".zip",
			new String(
				RESOURCES_PATH + "SmFile_zip.gif"));
		hashCorespImg.put(
			".rtf",
			new String(
				RESOURCES_PATH + "SmFile_doc.gif"));
	}
	/**
	 *
	 */	
	public static UserAttributes getUserFromId(long id)
		throws BaseException {

		UserAttributes ua = UserManager.getUser(new BigDecimal(id));
		return ua;
	}

	/**
	 *
	 */
	public static UserAttributes getUserFromId(BigDecimal id)
		throws BaseException {

		UserAttributes ua = UserManager.getUser(id);
		return ua;
	}

	/**
	 * Get the user email
	 */
	public static String getUserEmail(String login) throws BaseException {		

		String stm =
			"SELECT "
				+ UserAttributes.USER_EMAIL
				+ " FROM "
				+ DBConstants.TABLE_USER
				+ " WHERE "
				+ UserAttributes.USER_LOGIN
				+ "=? LIMIT 1";
		
		try {
			return DBManager.getSimpleTemplate().queryForObject(stm, String.class, login);
		} catch (Exception e) {
			e.printStackTrace();
			throw new BaseException("SQLException:" + e.getMessage());
		}
	}

	/**
	 * Get the user email
	 */
	public static String getUserEmail(long uid) throws BaseException {		

		String stm =
			"SELECT "
				+ UserAttributes.USER_EMAIL
				+ " FROM "
				+ DBConstants.TABLE_USER
				+ " WHERE "
				+ UserAttributes.USER_ID
				+ "=? LIMIT 1";

		try {
			return DBManager.getSimpleTemplate().queryForObject(stm, String.class, uid);
		} catch (Exception e) {
			e.printStackTrace();
			throw new BaseException("SQLException:" + e.getMessage());
		}
		
	}

	/**
	 * Get the user email
	 */
	public static String getUserEmail(UserAttributes ua)
		throws BaseException {
		return UserUtils.getUserEmail(
			(String) ua.getAttribute(UserAttributes.LOGIN));
	}
	/**
	 * Get user pages number set 
	 */
	public static int getUserPages(UserAttributes ua) throws BaseException {
		
		String stm =
			" select "
				+ DBConstants.USER_PAGES
				+ " from "
				+ DBConstants.TABLE_USER_SETTINGS
				+ " where "
				+ UserAttributes.USER_ID
				+ "=?";
		
		try {
			int result = DBManager.getSimpleTemplate().queryForInt(stm, ua.getAttribute(UserAttributes.ID)); 
			return result==0?DBConstants.USERS_ROWNUM:result;
		} 
		catch(EmptyResultDataAccessException e) {
			return DBConstants.USERS_ROWNUM;
		}
		catch (DataAccessException e) {
			e.printStackTrace();
			throw new BaseException("SQLException:" + e.getMessage());
		}

	}

	public static boolean isUserPages(UserAttributes ua)
		throws BaseException {		

		String stm =
			"select count(*) from "
				+ DBConstants.TABLE_USER_SETTINGS
				+ " where "
				+ UserAttributes.USER_ID
				+ " = ?";
		
		try {
			return ((DBManager.getSimpleTemplate().queryForInt(stm,ua.getAttribute(UserAttributes.ID))) > 0);
		} 
		catch (Exception e) {
			e.printStackTrace();
			throw new BaseException("SQLException:" + e.getMessage());
		}		
	}

	public static void setUserPages(String userpages, UserAttributes ua)
		throws BaseException {		
		
		if (userpages.equals(""))
			userpages = new Integer(DBConstants.USERS_ROWNUM).toString();
		
		String stm =
			" insert into "
				+ DBConstants.TABLE_USER_SETTINGS + " (user_id, pages, timestamp) "
				+ " values ("
				//+ DBConstants.TABLE_USER_SETTINGS
				//+ "_seq.nextval , "
				+ ua.getAttribute(UserAttributes.ID)
				+ " , "
				+ userpages
				+ ", null ) ";
		
		String qry =
			" update "
				+ DBConstants.TABLE_USER_SETTINGS
				+ " set "
				+ DBConstants.USER_PAGES
				+ " = "
				+ userpages
				+ " where "
				+ UserAttributes.USER_ID
				+ " = "
				+ ua.getAttribute(UserAttributes.ID);
	
		try {
		if (isUserPages(ua)) 
				DBManager.getSimpleTemplate().update(qry);
		else
				DBManager.getSimpleTemplate().update(stm);
		} catch (Exception e) {
			e.printStackTrace();
			throw new BaseException("Error to update user pages!");
		}
	}

	/**
	 * Get the user full name
	 */
	public static String getUserFullName(String login)
		throws BaseException {

		UserAttributes ua = UserManager.getUser(login, false);
		return UserUtils.getUserFullName(ua);
	}

	/**
	 * Get the user full name
	 */
	public static String getUserFullName(long uid) throws BaseException {

		UserAttributes ua = UserUtils.getUserFromId(uid);
		return UserUtils.getUserFullName(ua);
	}

	/**
	 * Get the user full name
	 */
	public static String getUserFullName(UserAttributes ua) {
		Object ofName = ua.getAttribute(UserAttributes.FIRSTNAME);
		String fName="";
		if (ofName!=null) fName = (String)ofName;
		Object omName= ua.getAttribute(UserAttributes.MIDDLENAME);
		String mName = "";
		if(omName!=null){		
			mName = (String)omName;
			if (!(mName).equals("N/A"))
				mName = (String) ua.getAttribute(UserAttributes.MIDDLENAME);
		}
		Object olName = ua.getAttribute(UserAttributes.LASTNAME);
		String lName = "";
		if(olName!=null)
			lName=(String)olName;
		if (mName.equals(""))
			return fName + " " + lName;
		else
			return fName + " " + mName + " " + lName;
	}

	public static String getUserFullNameForList(UserAttributes ua)
		throws BaseException {
		String fName = (String) ua.getAttribute(UserAttributes.FIRSTNAME);
		String mName = "";
		if (!((String) ua.getAttribute(UserAttributes.MIDDLENAME))
			.equals("N/A"))
			mName = (String) ua.getAttribute(UserAttributes.MIDDLENAME);
		String lName = (String) ua.getAttribute(UserAttributes.LASTNAME);
		if (mName.equals(""))
			return lName.toUpperCase() + ", " + fName;
		else
			return lName.toUpperCase() + ", " + fName + " " + mName;
	}

	public static String getUserFullNameSpecial(UserAttributes ua)
		throws BaseException {

		String fName = (String) ua.getAttribute(UserAttributes.FIRSTNAME);
		String lName = (String) ua.getAttribute(UserAttributes.LASTNAME);
		return fName.charAt(0) + ". " + lName;
	}

	public static boolean userEquals(UserAttributes ua1, UserAttributes ua2) {
		if (ua1 == null) {
			if (ua2 == null) {
				return true;
			} else {
				return false;
			}
		} else {
			return ua1.equals(ua2);
		}
	}

	/**
	 * Get all the users which belongs to a community sorted by
	 * a criteria specified in UserFilter. The default sort criteria is from name
	 */

	/*public static UserAttributes[] getUsersInCommunity(
		UserFilter uf,
		boolean special,
		Object community)
		throws BaseException {
	    
		String stm;
		BigDecimal comm_id;

		//gets the comm_id from received community
		if (community instanceof String) {
			comm_id =
				(BigDecimal) (CommunityUtils
					.getCommunityFromName((String) community)
					.getAttribute(CommunityAttributes.ID));
		} else {
			comm_id =
				(BigDecimal) (InstanceManager
					.getCurrentInstance()
					.getCurrentCommunity()
					.getAttribute(CommunityAttributes.ID));
		}
		
		stm =
			"SELECT DISTINCT "
				+ DBConstants.TABLE_USER
				+ "."
				+ UserAttributes.USER_ID
				+ ", "
				+ UserAttributes.USER_LASTNAME
				+ ", "
				+ UserAttributes.USER_FIRSTNAME
				+ ", "
				+ UserAttributes.USER_MIDDLENAME
				+ ", "
				+ UserAttributes.USER_LOGIN
				+ ", "
				+ UserAttributes.USER_PASSWD
				+ ", "
				+ UserAttributes.USER_COMPANY
				+ ", "
				+ UserAttributes.USER_EMAIL
				+ ", "
				+ UserAttributes.USER_ALTEMAIL
				+ ", "
				+ UserAttributes.USER_PHONE
				+ ", "
				+ UserAttributes.USER_ALTPHONE
				+ ", "
				+ UserAttributes.USER_ICQ
				+ ", "
				+ UserAttributes.USER_AOL
				+ ", "
				+ UserAttributes.USER_YAHOO
				+ ", "
				+ UserAttributes.USER_WADDRESS
				+ ", "
				+ UserAttributes.USER_WCITY
				+ ", "
				+ UserAttributes.USER_WSTATE
				+ ", "
				+ UserAttributes.USER_WZCODE
				+ ", "
				+ UserAttributes.USER_WCOUNTRY
				+ ", "
				+ UserAttributes.USER_WCOMPANY
				+ ", "
				+ UserAttributes.USER_EDITEDBY
				+ ", "
				+ DBConstants.TABLE_USER
				+ "."
				+ UserAttributes.USER_GROUP
				+ ", "
				+ UserAttributes.USER_LASTLOGIN
				+ ", "
				+ UserAttributes.USER_DELETED
				+ ", "
				+ UserAttributes.USER_LASTCOMM
				+ ", "
				+ UserAttributes.USER_PCARD_ID
				+ ", "
				+ UserAttributes.USER_WCARD_ID
				+ ", "
				+ UserAttributes.USER_DATEOFBIRTH
				+ ", "
				+ UserAttributes.USER_PLACE
				+ ", "
				+ UserAttributes.USER_PADDRESS
				+ ", "
				+ UserAttributes.USER_PLOCATION
				+ ", "
				+ UserAttributes.USER_HPHONE
				+ ", "
				+ UserAttributes.USER_MPHONE
				+ ", "
				+ UserAttributes.USER_PAGER
				+ ", "
				+ UserAttributes.USER_INSTANT_MESSENGER
				+ ", "
				+ UserAttributes.USER_MESSENGER_NUMBER
				+ ", "
				+ UserAttributes.USER_HCITY
				+ ", "
				+ UserAttributes.USER_HSTATE
				+ ", "
				+ UserAttributes.USER_HZIPCODE
				+ ", "
				+ UserAttributes.USER_HCOUNTRY
				+ ", "
				+ UserAttributes.USER_DISTRIBUTION_TYPE
				+ ", "
				+ UserAttributes.USER_DISTRIBUTION_MODE
				+ ", "
				+ UserAttributes.USER_DELIV_TEMPLATES
				+ ", "
				+ UserAttributes.USER_ADDRESS
				//+ ", "
				//+ UserAttributes.USER_C2ARATEINDEX
				//+ ", "
				//+ UserAttributes.USER_ATS2CRATEINDEX				
				+ " "
				+ " FROM "
				+ DBConstants.TABLE_USER
				/*+ ","
				+ DBConstants.TABLE_USER_COMMUNITY/
				+ " WHERE "
				+ CommunityAttributes.COMMUNITY_ID
				+ "="
				+ comm_id.longValue()
				/*+ " AND "
				+ DBConstants.TABLE_USER
				+ "."
				+ UserAttributes.USER_ID
				+ "="
				+ DBConstants.TABLE_USER_COMMUNITY
				+ "."
				+ UserAttributes.USER_ID/
				+ " AND "
				+ UserAttributes.USER_DELETED
				+ "=0";

		//use the sort criteria from received UserFilter
		if (special)
			stm = stm + " ORDER BY " + uf.getSortCriteria();

		DBConnection conn = null;
		try {
		    
		    conn = ConnectionPool.getInstance().requestConnection();
			DatabaseData result = conn.executeSQL(stm);
			
			int resultRows = result.getRowNumber();
			UserAttributes[] uas = new UserAttributes[resultRows];
			for (int i = 0; i < resultRows; i++) {
				uas[i] = new UserAttributes(result, i);
			}
			
			return uas;
			
		} catch (Exception e) {
			throw new BaseException("SQLException:" + e.getMessage());
		} finally {
			try{
			    ConnectionPool.getInstance().releaseConnection(conn);
			}catch(BaseException e){
			    e.printStackTrace();
			}			
		}
	}*/


	public static Vector getUsersInCommunity(
		UserFilter uf,
		boolean special,
		String community,
		int pageNumber,
		int userpages,
		String showHidden)
		throws BaseException {
	    
		String stm = new String("");
		
		stm =
			"SELECT DISTINCT "
				+ DBConstants.TABLE_USER
				+ "."
				+ UserAttributes.USER_ID
				+ ", "
				+ DBConstants.TABLE_USER
				+ "."
				+ UserAttributes.USER_LASTNAME
				+ ", "
				+ DBConstants.TABLE_USER
				+ "."
				+ UserAttributes.USER_FIRSTNAME
				+ ", "
				+ DBConstants.TABLE_USER
				+ "."
				+ UserAttributes.USER_MIDDLENAME
				+ ", "
				+ DBConstants.TABLE_USER
				+ "."
				+ UserAttributes.USER_LOGIN
				+ ", "
				+ DBConstants.TABLE_USER
				+ "."
				+ UserAttributes.USER_PASSWD
				+ ", "
				+ DBConstants.TABLE_USER
				+ "."
				+ UserAttributes.USER_COMPANY
				+ ", "
				+ DBConstants.TABLE_USER
				+ "."
				+ UserAttributes.USER_EMAIL
				+ ", "
				+ DBConstants.TABLE_USER
				+ "."
				+ UserAttributes.USER_ALTEMAIL
				+ ", "
				+ DBConstants.TABLE_USER
				+ "."
				+ UserAttributes.USER_PHONE
				+ ", "
				+ DBConstants.TABLE_USER
				+ "."
				+ UserAttributes.USER_ALTPHONE
				+ ", "
				+ DBConstants.TABLE_USER
				+ "."
				+ UserAttributes.USER_ICQ
				+ ", "
				+ DBConstants.TABLE_USER
				+ "."
				+ UserAttributes.USER_AOL
				+ ", "
				+ DBConstants.TABLE_USER
				+ "."
				+ UserAttributes.USER_YAHOO
				+ ", "
				+ DBConstants.TABLE_USER
				+ "."
				+ UserAttributes.USER_WADDRESS
				+ ", "
				+ DBConstants.TABLE_USER
				+ "."
				+ UserAttributes.USER_WCITY
				+ ", "
				+ DBConstants.TABLE_USER
				+ "."
				+ UserAttributes.USER_WSTATE
				+ ", "
				+ DBConstants.TABLE_USER
				+ "."
				+ UserAttributes.USER_WZCODE
				+ ", "
				+ DBConstants.TABLE_USER
				+ "."
				+ UserAttributes.USER_WCOUNTRY
				+ ", "
				+ DBConstants.TABLE_USER
				+ "."
				+ UserAttributes.USER_WCOMPANY
				+ ", "
				+ DBConstants.TABLE_USER
				+ "."
				+ UserAttributes.USER_EDITEDBY
				+ ", "
				+ DBConstants.TABLE_USER
				+ "."
				+ UserAttributes.USER_GROUP
				+ ", "
				+ DBConstants.TABLE_USER
				+ "."
				+ UserAttributes.USER_LASTLOGIN
				+ ", "
				+ DBConstants.TABLE_USER
				+ "."
				+ UserAttributes.USER_DELETED
				+ ", "
				+ DBConstants.TABLE_USER
				+ "."
				+ UserAttributes.USER_LASTCOMM
				+ ", "
				+ DBConstants.TABLE_USER
				+ "."
				+ UserAttributes.USER_PCARD_ID
				+ ", "
				+ DBConstants.TABLE_USER
				+ "."
				+ UserAttributes.USER_WCARD_ID
				+ ", "
				+ DBConstants.TABLE_USER
				+ "."
				+ UserAttributes.USER_DATEOFBIRTH
				+ ", "
				+ DBConstants.TABLE_USER
				+ "."
				+ UserAttributes.USER_PLACE
				+ ", "
				+ DBConstants.TABLE_USER
				+ "."
				+ UserAttributes.USER_PADDRESS
				+ ", "
				+ DBConstants.TABLE_USER
				+ "."
				+ UserAttributes.USER_PLOCATION
				+ ", "
				+ DBConstants.TABLE_USER
				+ "."
				+ UserAttributes.USER_HPHONE
				+ ", "
				+ DBConstants.TABLE_USER
				+ "."
				+ UserAttributes.USER_MPHONE
				+ ", "
				+ DBConstants.TABLE_USER
				+ "."
				+ UserAttributes.USER_PAGER
				+ ", "
				+ DBConstants.TABLE_USER
				+ "."
				+ UserAttributes.USER_INSTANT_MESSENGER
				+ ", "
				+ DBConstants.TABLE_USER
				+ "."
				+ UserAttributes.USER_MESSENGER_NUMBER
				+ ", "
				+ DBConstants.TABLE_USER
				+ "."
				+ UserAttributes.USER_HCITY
				+ ", "
				+ DBConstants.TABLE_USER
				+ "."
				+ UserAttributes.USER_HSTATE
				+ ", "
				+ DBConstants.TABLE_USER
				+ "."
				+ UserAttributes.USER_HZIPCODE
				+ ", "
				+ DBConstants.TABLE_USER
				+ "."
				+ UserAttributes.USER_HCOUNTRY
				+ ", "
				+ DBConstants.TABLE_USER
				+ "."
				+ UserAttributes.USER_DISTRIBUTION_TYPE
				+ ", "
				+ DBConstants.TABLE_USER
				+ "."
				+ UserAttributes.USER_DISTRIBUTION_MODE
				+ ", "
				+ DBConstants.TABLE_USER
				+ "."
				+ UserAttributes.USER_ADDRESS
				+ ", "
				+ DBConstants.TABLE_USER
				+ "."
				+ UserAttributes.USER_ASSIGN_MODE
				+ ", "
				+ DBConstants.TABLE_USER
				+ "."
				+ UserAttributes.USER_SINGLE_SEAT
				+ ", "
				+ DBConstants.TABLE_USER
				+ "."
				+ UserAttributes.USER_DISTRIB_ATS	
				+ ", "
				+ DBConstants.TABLE_USER
				+ "."
				+ UserAttributes.USER_DISTRIB_LINK
				+ ", " + DBConstants.TABLE_USER	+ "." + UserAttributes.USER_INTERACTIVE
				+ ", " + DBConstants.TABLE_USER	+ "." + UserAttributes.USER_OUTSOURCE
				+ ", " + DBConstants.TABLE_USER	+ "." + UserAttributes.USER_AUTO_ASSIGN_SEARCH_LOCKED
				+ ", " + DBConstants.TABLE_USER	+ "." + UserAttributes.USER_AUTO_UPDATE
				+ ", " + DBConstants.TABLE_USER	+ "." + UserAttributes.USER_OTHER_FILES_ON_SSF
				+ ", " + DBConstants.TABLE_USER	+ "." + UserAttributes.SEND_IMAGES_SURECLOSE
				+ ", " + DBConstants.TABLE_USER	+ "." + UserAttributes.SEND_REPORT_SURECLOSE
				+ ", " + DBConstants.TABLE_USER	+ "." + UserAttributes.USER_MODIFIED_BY
				+ ", " + DBConstants.TABLE_USER	+ "." + UserAttributes.USER_DATE_MODIFIED
				//+ ", "
				//+ DBConstants.TABLE_USER
				//+ "."
				//+ UserAttributes.USER_C2ARATEINDEX
				//+ ", "
				//+ DBConstants.TABLE_USER
				//+ "."
				//+ UserAttributes.USER_ATS2CRATEINDEX				
				+ " "
				+ " FROM "
				+ DBConstants.TABLE_USER;
				/*+ ","
				+ DBConstants.TABLE_USER_COMMUNITY;*/
		if (uf.getFindFlag())
			stm = stm + uf.getJoinTables();

		stm =
			stm
				+ " WHERE "
				+ DBConstants.TABLE_USER
				/*+ DBConstants.TABLE_USER_COMMUNITY*/
				+ "."
				+ CommunityAttributes.COMMUNITY_ID
				+ "="
				+ Long.parseLong(community);

		/*if (!uf.getFindFlag() || uf.getJoinCondition().equals("")){
		
			/*stm =
				stm
					+ " AND "
					+ DBConstants.TABLE_USER
					+ "."
					+ UserAttributes.USER_ID
					+ "="
					+ DBConstants.TABLE_USER_COMMUNITY
					+ "."
					+ UserAttributes.USER_ID;*/
		//}else
			//stm = stm + uf.getJoinCondition();

		//use the sort criteria from received UserFilter
		if (uf.getFindFlag())
			stm = stm + uf.getUserFind();
		if (uf.getLikeFlag())
			stm = stm + " AND " + uf.getSortLike();
		
		if("no".equals(showHidden)){
			stm += " AND " + DBConstants.TABLE_USER + "." + UserAttributes.USER_HIDDEN + "=0 ";
		} else if("hidden".equals(showHidden)){
			stm += " AND " + DBConstants.TABLE_USER + "." + UserAttributes.USER_HIDDEN + "=1 ";
		} else if("all".equals(showHidden)){ }
		
		if (special)
			stm =
				stm
					+ " ORDER BY "
					+ uf.getSortCriteria()
					+ " "
					+ uf.getSortOrder();
		
		DBConnection conn = null;
		try {
		    
		    conn = ConnectionPool.getInstance().requestConnection();
			
		    PreparedStatement pstmt = conn.prepareStatement( stm );	
			int k=1;
		    
			if (uf.getFindFlag() && !uf.getFindUserValues().isEmpty() ) {
				for (Iterator iter = uf.getFindUserValues().iterator(); iter.hasNext();) {
					String s = (String ) iter.next();
					pstmt.setString( k++, s);	
				}
			}

			if (uf.getLikeFlag()) {
				pstmt.setString( k++, uf.getSortLikeValue());
			}
				
			DatabaseData result = conn.executePrepQuery(pstmt);	
			pstmt.close();	
			
			int resultRows = result.getRowNumber();
			Vector vector = new Vector();
			int fromPage =
				pageNumber * userpages /*DataConstants.USERS_ROWNUM*/;
			int toPage;
			if ((toPage = fromPage + userpages /*DataConstants.USERS_ROWNUM*/
				) - resultRows >= 0)
				toPage = resultRows;
			vector.add(new Integer(pageNumber));
			for (int i = fromPage; i < toPage; i++) {
				vector.add(new UserAttributes(result, i));
			}

			if (resultRows % userpages == 0)
				vector.add(new Integer((resultRows / userpages)
				/*DataConstants.USERS_ROWNUM*/
				-pageNumber - 1));
			else
				vector.add(new Integer((resultRows / userpages)
				/*DataConstants.USERS_ROWNUM*/
				-pageNumber));

			return vector;

		} catch (Exception e) {
			throw new BaseException("SQLException:" + e.getMessage());
		} finally {
			try{
			    ConnectionPool.getInstance().releaseConnection(conn);
			}catch(BaseException e){
			    e.printStackTrace();
			}			
		}
		
	}

	public static Vector getAllUsersForCommunities(
		UserFilter uf,
		boolean special,
		int pageNumber,
		CommunityAttributes cas[],
		int userpages)
		throws BaseException {
	    
		String stm = new String("");
		String commidStr = new String();
		
		for (int i = cas.length - 1; i >= 0; i--) {

			commidStr = commidStr + cas[i].getAttribute(CommunityAttributes.ID);
			if (i > 0)
				commidStr = commidStr + ",";

		}
		
		stm =
			"SELECT DISTINCT "
				+ DBConstants.TABLE_USER
				+ "."
				+ UserAttributes.USER_ID
				+ ", "
				+ DBConstants.TABLE_USER
				+ "."
				+ UserAttributes.USER_LASTNAME
				+ ", "
				+ DBConstants.TABLE_USER
				+ "."
				+ UserAttributes.USER_FIRSTNAME
				+ ", "
				+ DBConstants.TABLE_USER
				+ "."
				+ UserAttributes.USER_MIDDLENAME
				+ ", "
				+ DBConstants.TABLE_USER
				+ "."
				+ UserAttributes.USER_LOGIN
				+ ", "
				+ DBConstants.TABLE_USER
				+ "."
				+ UserAttributes.USER_PASSWD
				+ ", "
				+ DBConstants.TABLE_USER
				+ "."
				+ UserAttributes.USER_COMPANY
				+ ", "
				+ DBConstants.TABLE_USER
				+ "."
				+ UserAttributes.USER_EMAIL
				+ ", "
				+ DBConstants.TABLE_USER
				+ "."
				+ UserAttributes.USER_ALTEMAIL
				+ ", "
				+ DBConstants.TABLE_USER
				+ "."
				+ UserAttributes.USER_PHONE
				+ ", "
				+ DBConstants.TABLE_USER
				+ "."
				+ UserAttributes.USER_ALTPHONE
				+ ", "
				+ DBConstants.TABLE_USER
				+ "."
				+ UserAttributes.USER_ICQ
				+ ", "
				+ DBConstants.TABLE_USER
				+ "."
				+ UserAttributes.USER_AOL
				+ ", "
				+ DBConstants.TABLE_USER
				+ "."
				+ UserAttributes.USER_YAHOO
				+ ", "
				+ DBConstants.TABLE_USER
				+ "."
				+ UserAttributes.USER_WADDRESS
				+ ", "
				+ DBConstants.TABLE_USER
				+ "."
				+ UserAttributes.USER_WCITY
				+ ", "
				+ DBConstants.TABLE_USER
				+ "."
				+ UserAttributes.USER_WSTATE
				+ ", "
				+ DBConstants.TABLE_USER
				+ "."
				+ UserAttributes.USER_WZCODE
				+ ", "
				+ DBConstants.TABLE_USER
				+ "."
				+ UserAttributes.USER_WCOUNTRY
				+ ", "
				+ DBConstants.TABLE_USER
				+ "."
				+ UserAttributes.USER_WCOMPANY
				+ ", "
				+ DBConstants.TABLE_USER
				+ "."
				+ UserAttributes.USER_EDITEDBY
				+ ", "
				+ DBConstants.TABLE_USER
				+ "."
				+ UserAttributes.USER_GROUP
				+ ", "
				+ DBConstants.TABLE_USER
				+ "."
				+ UserAttributes.USER_LASTLOGIN
				+ ", "
				+ DBConstants.TABLE_USER
				+ "."
				+ UserAttributes.USER_DELETED
				+ ", "
				+ DBConstants.TABLE_USER
				+ "."
				+ UserAttributes.USER_LASTCOMM
				+ ", "
				+ DBConstants.TABLE_USER
				+ "."
				+ UserAttributes.USER_PCARD_ID
				+ ", "
				+ DBConstants.TABLE_USER
				+ "."
				+ UserAttributes.USER_WCARD_ID
				+ ", "
				+ DBConstants.TABLE_USER
				+ "."
				+ UserAttributes.USER_DATEOFBIRTH
				+ ", "
				+ DBConstants.TABLE_USER
				+ "."
				+ UserAttributes.USER_PLACE
				+ ", "
				+ DBConstants.TABLE_USER
				+ "."
				+ UserAttributes.USER_PADDRESS
				+ ", "
				+ DBConstants.TABLE_USER
				+ "."
				+ UserAttributes.USER_PLOCATION
				+ ", "
				+ DBConstants.TABLE_USER
				+ "."
				+ UserAttributes.USER_HPHONE
				+ ", "
				+ DBConstants.TABLE_USER
				+ "."
				+ UserAttributes.USER_MPHONE
				+ ", "
				+ DBConstants.TABLE_USER
				+ "."
				+ UserAttributes.USER_PAGER
				+ ", "
				+ DBConstants.TABLE_USER
				+ "."
				+ UserAttributes.USER_INSTANT_MESSENGER
				+ ", "
				+ DBConstants.TABLE_USER
				+ "."
				+ UserAttributes.USER_MESSENGER_NUMBER
				+ ", "
				+ DBConstants.TABLE_USER
				+ "."
				+ UserAttributes.USER_HCITY
				+ ", "
				+ DBConstants.TABLE_USER
				+ "."
				+ UserAttributes.USER_HSTATE
				+ ", "
				+ DBConstants.TABLE_USER
				+ "."
				+ UserAttributes.USER_HZIPCODE
				+ ", "
				+ DBConstants.TABLE_USER
				+ "."
				+ UserAttributes.USER_HCOUNTRY
				+ ", "
				+ DBConstants.TABLE_USER
				+ "."
				+ UserAttributes.USER_DISTRIBUTION_TYPE
				+ ", "
				+ DBConstants.TABLE_USER
				+ "."
				+ UserAttributes.USER_DISTRIBUTION_MODE
				+ ", "
				+ DBConstants.TABLE_USER
				+ "."
				+ UserAttributes.USER_TEMPLATES
				+ ", "
				+ DBConstants.TABLE_USER
				+ "."
				+ UserAttributes.USER_ADDRESS
				+ ", "
				+ DBConstants.TABLE_USER
				+ "."
				+ UserAttributes.USER_ASSIGN_MODE
				+ ", "
				+ DBConstants.TABLE_USER
				+ "."
				+ UserAttributes.USER_SINGLE_SEAT	
				+ ", "
				+ DBConstants.TABLE_USER
				+ "."
				+ UserAttributes.USER_DISTRIB_ATS
				+ ", "
				+ DBConstants.TABLE_USER
				+ "."
				+ UserAttributes.USER_DISTRIB_LINK	
				+ ", "
				+ DBConstants.TABLE_USER
				+ "."
				+ UserAttributes.USER_INTERACTIVE
				+ ", " + DBConstants.TABLE_USER	+ "." + UserAttributes.USER_OUTSOURCE
				+ ", " + DBConstants.TABLE_USER	+ "." + UserAttributes.USER_AUTO_ASSIGN_SEARCH_LOCKED
				+ ", " + DBConstants.TABLE_USER	+ "." + UserAttributes.USER_AUTO_UPDATE
				+ ", " + DBConstants.TABLE_USER	+ "." + UserAttributes.USER_OTHER_FILES_ON_SSF
				+ ", " + DBConstants.TABLE_USER	+ "." + UserAttributes.SEND_IMAGES_SURECLOSE
				+ ", " + DBConstants.TABLE_USER	+ "." + UserAttributes.SEND_REPORT_SURECLOSE
				+ ", " + DBConstants.TABLE_USER	+ "." + UserAttributes.USER_MODIFIED_BY
				+ ", " + DBConstants.TABLE_USER	+ "." + UserAttributes.USER_DATE_MODIFIED
				//+ ", "
				//+ DBConstants.TABLE_USER
				//+ "."
				//+ UserAttributes.USER_C2ARATEINDEX
				//+ ", "
				//+ DBConstants.TABLE_USER
				//+ "."
				//+ UserAttributes.USER_ATS2CRATEINDEX							
				+ " "
				+ " FROM "
				+ DBConstants.TABLE_USER
				/*+ ","
				+ DBConstants.TABLE_USER_COMMUNITY*/;
		if (uf.getFindFlag())
			stm = stm + uf.getJoinTables();
		stm =
			stm
				+ " WHERE "
				/*+ DBConstants.TABLE_USER_COMMUNITY*/
				+ DBConstants.TABLE_USER
				+ "."
				+ CommunityAttributes.COMMUNITY_ID
				+ " in ("
				+ StringUtils.makeValidNumberList(commidStr)
				+ ")";
		//comm_id.longValue()		 

		if (!uf.getFindFlag() || uf.getJoinCondition().equals("")) {
			/*stm =
				stm
					+ " AND "
					+ DBConstants.TABLE_USER
					+ "."
					+ UserAttributes.USER_ID
					+ "="
					+ DBConstants.TABLE_USER_COMMUNITY
					+ "."
					+ UserAttributes.USER_ID;*/
		} else {
			//stm = stm + uf.getJoinCondition();
		}

		/*stm = stm + " AND " + DataConstants.USER_DELETED + "=0";*/

		//use the sort criteria from received UserFilter
		if (uf.getFindFlag())
			stm = stm + uf.getUserFind();
		if (uf.getLikeFlag())
			stm = stm + " AND " + uf.getSortLike();
		if (special)
			stm =
				stm
					+ " ORDER BY "
					+ uf.getSortCriteria()
					+ " "
					+ uf.getSortOrder();

		DBConnection conn = null;
		try {
		    
		    conn = ConnectionPool.getInstance().requestConnection();
			
		    PreparedStatement pstmt = conn.prepareStatement( stm );	
			int k=1;
		    
			if (uf.getFindFlag() && !uf.getFindUserValues().isEmpty() ) {
				for (Iterator iter = uf.getFindUserValues().iterator(); iter.hasNext();) {
					String s = (String ) iter.next();
					pstmt.setString( k++, s);	
				}
			}

			if (uf.getLikeFlag()) {
				pstmt.setString( k++, uf.getSortLikeValue());
			}
				
			DatabaseData result = conn.executePrepQuery(pstmt);	
			pstmt.close();	
			
			int resultRows = result.getRowNumber();
			Vector vector = new Vector();
			int fromPage =
				pageNumber * userpages /*DataConstants.USERS_ROWNUM*/;
			int toPage;
			if ((toPage = fromPage + userpages /*DataConstants.USERS_ROWNUM*/
				) - resultRows > 0)
				toPage = resultRows;
			vector.add(new Integer(pageNumber));
			for (int i = fromPage; i < toPage; i++) {
				vector.add(new UserAttributes(result, i));
			}

			if (resultRows % userpages == 0)
				vector.add(new Integer((resultRows / userpages)
				/*DataConstants.USERS_ROWNUM*/
				-pageNumber - 1));
			else
				vector.add(new Integer((resultRows / userpages)
				/*DataConstants.USERS_ROWNUM*/
				-pageNumber));
			return vector;

		} catch (Exception e) {
			throw new BaseException("SQLException:" + e.getMessage());
		} finally {
			try{
			    ConnectionPool.getInstance().releaseConnection(conn);
			}catch(BaseException e){
			    e.printStackTrace();
			}			
		}
	}
	
/*	public static Vector getAllEXCommAdmin(UserAttributes[] uas)
	throws BaseException{
		Vector v = new Vector();
		
		for(int i=0;i<uas.length;i++){
			int gid = ((BigDecimal)uas[i].getAttribute(UserAttributes.GROUP)).intValue();
			if(gid == DataConstants.USER_ADMIN_ID || gid == DataConstants.USER_EX_ID)
				v.addElement(uas[i]);
		}
	  	return v;	
	}*/

	/**
	 * special = some more specific filter,
	 * this particular case means alphabetically ordered
	 */
	public static UserAttributes[] getAllTSUsers(
		boolean special,
		UserFilter uf)
		throws BaseException, IOException {

		//UserFilter uf = new UserFilter();
		UserAttributes[] uas = UserManager.getUsers(uf, special);
		return uas;
	}

	public static UserAttributes[] getAllVPOUsers(boolean special)
		throws BaseException, IOException {

		UserFilter uf = new UserFilter();
		UserAttributes[] uas = UserManager.getUsers(uf, special);
		return uas;
	}

	/**
	* special = some more specific filter,
	* this particular case means alphabetically ordered
	*/

	public static UserAttributes[] getUsers(UserFilter uf, boolean special)
		throws BaseException, IOException {

		return UserManager.getUsers(uf, special);
	}

	public static UserAttributes[] getUsersType(
		CommunityAttributes ca,
		String userType)
		throws BaseException {

		String[] loginA = UserUtils.getUsersTypeAsLogin(ca, userType);
		UserAttributes[] uaA = new UserAttributes[loginA.length];
		for (int i = 0; i < loginA.length; i++) {
			uaA[i] = UserManager.getUser(loginA[i], false);
		}
		return uaA;
	}

	/**
	 *
	 */
	public static String[] getUsersTypeAsLogin(
		String community,
		String userType)
		throws BaseException {

		CommunityAttributes ca = CommunityUtils.getCommunityFromName(community);
		return getUsersTypeAsLogin(ca, userType);
	}

	/**
	 * Get all users whith a specific role into a community
	 */
	public static String[] getUsersTypeAsLogin(
		CommunityAttributes ca,
		String userType)
		throws BaseException {

		GroupAttributes ga = GroupUtils.getGroup(userType);
		int typeId =
			((BigInteger) ga.getAttribute(GroupAttributes.ID)).intValue();
		
		String stm =
			"SELECT DISTINCT "
				+ UserAttributes.USER_LOGIN
				+ " FROM "
				+ DBConstants.TABLE_USER_COMMUNITY
				+ ","
				+ DBConstants.TABLE_USER
				+ " WHERE "
				+ DBConstants.TABLE_USER
				+ "."
				+ CommunityAttributes.COMMUNITY_ID
				+ "= ?"
				+ " AND "
				+ DBConstants.TABLE_USER_COMMUNITY
				+ "."
				+ GroupAttributes.GROUP_ID
				+ "="
				+ typeId
				+ " AND "
				+ DBConstants.TABLE_USER
				+ "."
				+ UserAttributes.USER_ID
				+ "="
				+ DBConstants.TABLE_USER_COMMUNITY
				+ "."
				+ UserAttributes.USER_ID
				+ " AND "
				+ DBConstants.TABLE_USER
				+ "."
				+ UserAttributes.USER_DELETED
				+ "=0"
				+ " AND " + DBConstants.TABLE_USER + "." + UserAttributes.USER_HIDDEN + "=0 "
				+ " ORDER BY "
				+ UserAttributes.USER_LOGIN;

        ParameterizedRowMapper<String> mapper = new ParameterizedRowMapper<String>() {
            public String mapRow(ResultSet rs, int rowNum) throws SQLException {
                return rs.getString(UserAttributes.USER_LOGIN) ;
            }
        };
        
        try {
        	
        List<String> result = DBManager.getSimpleTemplate().query(stm,mapper,ca.getAttribute(CommunityAttributes.ID));
        String[] us = new String[result.size()];
        us = result.toArray(us);
        return us;
        
		} catch (Exception e) {
			throw new BaseException("SQLException:" + e.getMessage());
		}
	}

	/**
	 * Get all users with a specific role from all the sistem
	 * sorted by login name.
	 */
	public static UserAttributes[] getUsersType(int userType)
	throws BaseException {
		String[] loginA = UserUtils.getUsersTypeAsLogin(userType);
		UserAttributes[] uaA = new UserAttributes[loginA.length];
		for (int i = 0; i < loginA.length; i++) {
			uaA[i] = UserManager.getUser(loginA[i], false);
		}
		return uaA;		
	}
	
	public static UserAttributes[] getUsersType(String userType)
		throws BaseException {

		String[] loginA = UserUtils.getUsersTypeAsLogin(userType);
		UserAttributes[] uaA = new UserAttributes[loginA.length];
		for (int i = 0; i < loginA.length; i++) {
			uaA[i] = UserManager.getUser(loginA[i], false);
		}
		return uaA;
	}

	/**
	 */
	public static String[] getUsersTypeAsLogin(int typeId)
	throws BaseException {
		
		String stm =
			"SELECT DISTINCT "
				+ UserAttributes.USER_LOGIN
				+ " FROM "
				+ DBConstants.TABLE_USER
				/*+ ","
				+ DBConstants.TABLE_USER_GROUP*/
				+ " WHERE "
				/*+ DBConstants.TABLE_USER_GROUP
				+ "."
				+ GroupAttributes.GROUP_ID*/
				+ DBConstants.TABLE_USER
				+ "."
				+ UserAttributes.USER_GROUP
				+ "="
				+ typeId
				/*+ " AND "
				+ DBConstants.TABLE_USER
				+ "."
				+ UserAttributes.USER_ID
				+ "="
				+ DBConstants.TABLE_USER_GROUP
				+ "."
				+ UserAttributes.USER_ID*/
				+ " AND "
				+ DBConstants.TABLE_USER
				+ "."
				+ UserAttributes.USER_DELETED
				+ "=0"
				+ " AND " + DBConstants.TABLE_USER + "." + UserAttributes.USER_HIDDEN + "=0"
				+ " ORDER BY "
				+ UserAttributes.USER_LOGIN;

		DBConnection conn = null;
		try {
		    
		    conn = ConnectionPool.getInstance().requestConnection();
			DatabaseData result = conn.executeSQL(stm);
			
			int resultRows = result.getRowNumber();
			String[] us = new String[resultRows];
			for (int i = 0; i < resultRows; i++)
				us[i] = (String) result.getValue(UserAttributes.USER_LOGIN, i);
			
			return us;
			
		} catch (Exception e) {
			throw new BaseException("SQLException:" + e.getMessage());
		} finally {
			try{
			    ConnectionPool.getInstance().releaseConnection(conn);
			}catch(BaseException e){
			    e.printStackTrace();
			}			
		}
		
	}
	
	public static String[] getUsersTypeAsLogin(String userType)
		throws BaseException {

		GroupAttributes ga = GroupUtils.getGroup(userType);
		int typeId =
			((BigDecimal) ga.getAttribute(GroupAttributes.ID)).intValue();
		return getUsersTypeAsLogin(typeId);
	}

	/**
	 * Verify if a user belongs to a community
	 */
	public static boolean existInCommunity(long uid, long cid)
		throws BaseException {
		
		String stm =
			" SELECT 1 FROM "
				+ DBConstants.TABLE_USER_COMMUNITY
				+ " WHERE "
				+ CommunityAttributes.COMMUNITY_ID
				+ "="
				+ cid
				+ " AND "
				+ UserAttributes.USER_ID
				+ "="
				+ uid;

		DBConnection conn = null;
		try {
		    
		    conn = ConnectionPool.getInstance().requestConnection();
			DatabaseData result = conn.executeSQL(stm);
			
			return result.getRowNumber() != 0;

		} catch (Exception e) {
			throw new BaseException("SQLException:" + e.getMessage());
		} finally {
			try{
			    ConnectionPool.getInstance().releaseConnection(conn);
			}catch(BaseException e){
			    e.printStackTrace();
			}			
		}
	}

	/**
	 * Verify if a user belongs to a community
	 */
	public static boolean existInCommunity(String login, String commName)
		throws BaseException {

		UserAttributes ua = UserManager.getUser(login, false);
		long uid =
			((BigDecimal) ua.getAttribute(UserAttributes.ID)).longValue();

		CommunityAttributes ca = CommunityUtils.getCommunityFromName(commName);
		long cid =
			((BigDecimal) ca.getAttribute(CommunityAttributes.ID)).longValue();

		return UserUtils.existInCommunity(uid, cid);
	}

	/**
	 * Verify if a user belongs to a community
	 */
	public static boolean existInCommunity(long uid, String commName)
		throws BaseException {

		CommunityAttributes ca = CommunityUtils.getCommunityFromName(commName);
		long cid =
			((BigDecimal) ca.getAttribute(CommunityAttributes.ID)).longValue();

		return UserUtils.existInCommunity(uid, cid);
	}

	/**
	 *
	 */
	public static boolean hasRole(long uId, long typeId)
		throws BaseException {
		
		String stm =
			"SELECT 1 FROM "
				+ DBConstants.TABLE_USER_GROUP
				+ " WHERE "
				+ UserAttributes.USER_ID
				+ "="
				+ uId
				+ " AND "
				+ GroupAttributes.GROUP_ID
				+ "="
				+ typeId;

		DBConnection conn = null;
		try {
		    
		    conn = ConnectionPool.getInstance().requestConnection();
			DatabaseData result = conn.executeSQL(stm);

			return result.getRowNumber() != 0;

		} catch (Exception e) {
			throw new BaseException("SQLException:" + e.getMessage());
		} finally {
			try{
			    ConnectionPool.getInstance().releaseConnection(conn);
			}catch(BaseException e){
			    e.printStackTrace();
			}			
		}
	}

	/**
	 * Verify if a user has a specific role into a community
	 */
	public static boolean hasRoleInCommunity(long uId, int typeId, long commId)
		throws BaseException {

		String stm =
			"SELECT 1 FROM "
				+ DBConstants.TABLE_USER_COMMUNITY
				+ " WHERE "
				+ UserAttributes.USER_ID
				+ "="
				+ uId
				+ " AND "
				+ GroupAttributes.GROUP_ID
				+ "="
				+ typeId
				+ " AND "
				+ CommunityAttributes.COMMUNITY_ID
				+ "="
				+ commId;

		DBConnection conn = null;
		try {
		    
		    conn = ConnectionPool.getInstance().requestConnection();
			DatabaseData result = conn.executeSQL(stm);

			return result.getRowNumber() != 0;

		} catch (Exception e) {
			throw new BaseException("SQLException:" + e.getMessage());
		} finally {
			try{
			    ConnectionPool.getInstance().releaseConnection(conn);
			}catch(BaseException e){
			    e.printStackTrace();
			}			
		}
	}
	/**
	 *
	 */
	public static boolean hasCommunity(String login, String commName)
		throws BaseException {

		UserAttributes ua = UserManager.getUser(login, false);
		long uid =
			((BigDecimal) ua.getAttribute(UserAttributes.ID)).longValue();

		CommunityAttributes ca = CommunityUtils.getCommunityFromName(commName);
		long cid =
			((BigDecimal) ca.getAttribute(CommunityAttributes.ID)).longValue();

		return UserUtils.existInCommunity(uid, cid);
	}

	/**
	 *
	 */
	public static void updateLastLogin(String login, long loginTime) throws BaseException{

		UserAttributes ua = UserManager.getUser(login, false);
		UserUtils.updateLastLogin(ua, loginTime);
	}

	/**
	 *
	 */
	public static void updateLastLogin(UserAttributes ua, long loginTime) throws BaseException {

		String stm =
			"UPDATE "
				+ DBConstants.TABLE_USER
				+ " SET "
				+ UserAttributes.USER_LASTLOGIN
				+ "="
				+ loginTime
				+ " WHERE "
				+ UserAttributes.USER_ID
				+ "=?";
		
		try {
			DBManager.getSimpleTemplate().update(stm,ua.getAttribute(UserAttributes.ID));
		} catch (Exception e) {
			e.printStackTrace();
			throw new BaseException("SQLException:" + e.getMessage());
		}
	}

	/**
	 * receive a username and
	 * @return a hashtable containg user infos
	 */
	public static Hashtable getUserInfo(String login) throws BaseException {
		UserAttributes ua = UserManager.getUser(login, false);
		return UserUtils.getUserInfo(ua);
	}

	/**
	 * @param {@link ro.cst.vems.data.user.UserAttributes UserAttributes}
	 * instance for which we get infos
	 * @return a hashtable containg user infos
	 * @exception {@link ro.cst.vems.data.BaseException BaseException}
	 */
	public static Hashtable getUserInfo(UserAttributes ua)
		throws BaseException {

		Hashtable hash = new Hashtable();
		hash.put(UserAttributes.USER_ID, ua.getAttribute(UserAttributes.ID));
		hash.put(
			UserAttributes.USER_LOGIN,
			ua.getAttribute(UserAttributes.LOGIN));
		Object objLastName= ua.getAttribute(UserAttributes.LASTNAME);	
		hash.put(
			UserAttributes.USER_LASTNAME,
			(objLastName!=null)?objLastName:"N/A");
		Object firstName= ua.getAttribute(UserAttributes.FIRSTNAME);
		hash.put(
			UserAttributes.USER_FIRSTNAME,
			(firstName!=null)?firstName:"N/A");
		Object middleName = ua.getAttribute(UserAttributes.MIDDLENAME); 
		hash.put(
			UserAttributes.USER_MIDDLENAME,
			(middleName!=null)?middleName:"");
		Object userCompany =ua.getAttribute(UserAttributes.COMPANY); 		
		hash.put(
			UserAttributes.USER_COMPANY,
			(userCompany!=null)?userCompany:"N/A");				
		hash.put(
			UserAttributes.USER_GROUP,
			ua.getAttribute(UserAttributes.GROUP));
		Object email = ua.getAttribute(UserAttributes.EMAIL); 
		hash.put(
			UserAttributes.USER_EMAIL,
			(email!=null)?email:"N/A");
		Object altEmail = ua.getAttribute(UserAttributes.ALTEMAIL);
		hash.put(
			UserAttributes.USER_ALTEMAIL,
			(altEmail!=null)?altEmail:"N/A");
		Object phone = ua.getAttribute(UserAttributes.PHONE);
		hash.put(
			UserAttributes.USER_PHONE,
			(phone!=null)?phone:"N/A");
		Object altPhone = ua.getAttribute(UserAttributes.ALTPHONE);
		hash.put(
			UserAttributes.USER_ALTPHONE,
			(altPhone!=null)?altPhone:"N/A");
		//hash.put(UserAttributes.USER_ICQ, ua.getAttribute(UserAttributes.ICQ));
		//hash.put(UserAttributes.USER_AOL, ua.getAttribute(UserAttributes.AOL));
		/*hash.put(
			UserAttributes.USER_YAHOO,
			ua.getAttribute(UserAttributes.YAHOO));*/
		Object waddress = ua.getAttribute(UserAttributes.WADDRESS);	
		hash.put(
			UserAttributes.USER_WADDRESS,(waddress!=null)?waddress:"N/A"
			);
		Object wcity = ua.getAttribute(UserAttributes.WCITY);
		hash.put(
			UserAttributes.USER_WCITY,
			(wcity!=null)?wcity:"N/A");
		Object wstate = ua.getAttribute(UserAttributes.WSTATE);
		hash.put(
			UserAttributes.USER_WSTATE,
			(wstate!=null)?wstate:"N/A");
		Object wzcode = ua.getAttribute(UserAttributes.WZCODE); 
		hash.put(
			UserAttributes.USER_WZCODE,
			(wzcode!=null)?wzcode:"N/A");
		Object wzcountry =ua.getAttribute(UserAttributes.WCONTRY); 
		hash.put(
			UserAttributes.USER_WCOUNTRY,
			(wzcountry!=null)?wzcountry:"N/A");
		Object wcompany = ua.getAttribute(UserAttributes.WCOMPANY);
		hash.put(
			UserAttributes.USER_WCOMPANY,
			(wcompany!=null)?wcompany:"N/A");
		Object lastLogin = ua.getAttribute(UserAttributes.LASTLOGIN);
		hash.put(
			UserAttributes.USER_LASTLOGIN,
			(lastLogin!=null)?lastLogin:"N/A");
		
		Object  pccardid = ua.getAttribute(UserAttributes.PCARD_ID);
		hash.put(
			UserAttributes.USER_PCARD_ID,
			(pccardid!=null)?pccardid:"N/A");
		Object wcardid = ua.getAttribute(UserAttributes.WCARD_ID);
		hash.put(
			UserAttributes.USER_WCARD_ID,
			(wcardid!=null)?wcardid:"N/A");
		BigDecimal dateofbirth = ua.getDATEOFBIRTH();
		hash.put(
			UserAttributes.USER_DATEOFBIRTH,
			(dateofbirth!=null)?dateofbirth:new BigDecimal(0));			
		Object place = ua.getAttribute(UserAttributes.PLACE);
		hash.put(
			UserAttributes.USER_PLACE,
			(place!=null)?place:"N/A");
		/*Object paddress = ua.getAttribute(UserAttributes.PADDRESS); 	
		hash.put(
			UserAttributes.USER_PADDRESS,
			(paddress!=null)?paddress:"N/A");*/
	
		Object state = ua.getAttribute(UserAttributes.STATE_ID); 	
		hash.put(
			UserAttributes.USER_STATE_ID,
			(state!=null)?(String)(State.getState( new BigDecimal(state.toString())).getName().toString()):"N/A");
			
		Object streetno = ua.getAttribute(UserAttributes.STREETNO); 	
		hash.put(
			UserAttributes.USER_STREETNO,
			(streetno!=null)?streetno:"N/A");
			
		Object streetdirection = ua.getAttribute(UserAttributes.STREETDIRECTION); 	
		hash.put(
			UserAttributes.USER_STREETDIRECTION,
			(streetdirection!=null)?streetdirection:"N/A");
			
		Object streetname = ua.getAttribute(UserAttributes.STREETNAME); 	
		hash.put(
			UserAttributes.USER_STREETNAME,
			(streetname!=null)?streetname:"N/A");
			
		Object streetsuffix = ua.getAttribute(UserAttributes.STREETSUFFIX); 	
		hash.put(
			UserAttributes.USER_STREETSUFFIX,
			(streetsuffix!=null)?streetsuffix:"N/A");

		Object streetunit = ua.getAttribute(UserAttributes.STREETUNIT); 	
		hash.put(
			UserAttributes.USER_STREETUNIT,
			(streetunit!=null)?streetunit:"N/A");

			
		Object plocation = ua.getAttribute(UserAttributes.PLOCATION); 
		hash.put(
			UserAttributes.USER_PLOCATION,
			(plocation!=null)?plocation:"N/A");
		Object hphone = ua.getAttribute(UserAttributes.HPHONE);		 	
		hash.put(
			UserAttributes.USER_HPHONE,
			(hphone!=null)?hphone:"N/A");
		
		Object mphone = ua.getAttribute(UserAttributes.MPHONE);		
		hash.put(
			UserAttributes.USER_MPHONE,
			(mphone!=null)?mphone:"");
			
		Object pager = ua.getAttribute(UserAttributes.PAGER);
		hash.put(
			UserAttributes.USER_PAGER,
			(pager!=null)?pager:"N/A");
		Object instantMsg = ua.getAttribute(UserAttributes.INSTANT_MESSENGER);	
		hash.put(
			UserAttributes.USER_INSTANT_MESSENGER,(instantMsg!=null)?instantMsg:"N/A");
		
		Object msgNumber = ua.getAttribute(UserAttributes.MESSENGER_NUMBER);
		hash.put(
			UserAttributes.USER_MESSENGER_NUMBER,
			(msgNumber!=null)?msgNumber:"N/A");
		
		Object hcity = ua.getAttribute(UserAttributes.HCITY); 
		hash.put(
			UserAttributes.USER_HCITY,
			(hcity!=null)?hcity:"N/A");	
		Object hstate = ua.getAttribute(UserAttributes.HSTATE);
		hash.put(
			UserAttributes.USER_HSTATE,
			(hstate!=null)?hstate:"N/A");
		Object hzipcode = ua.getAttribute(UserAttributes.HZIPCODE);	
		hash.put(
			UserAttributes.USER_HZIPCODE,
			(hzipcode!=null)?hzipcode:"N/A");
		Object hcountry = ua.getAttribute(UserAttributes.HCOUNTRY);	
		hash.put(
			UserAttributes.USER_HCOUNTRY,
			(hcountry!=null)?hcountry:"N/A");
		Object distribution_type = ua.getAttribute(UserAttributes.DISTRIBUTION_TYPE);	
		hash.put(
			UserAttributes.USER_DISTRIBUTION_TYPE,
			(distribution_type!=null)?distribution_type:"N/A");
		Object distribution_mode = ua.getAttribute(UserAttributes.DISTRIBUTION_MODE);	
		hash.put(
			UserAttributes.USER_DISTRIBUTION_MODE,
			(distribution_mode!=null)?distribution_mode:"N/A");
		Object deliv_templates = ua.getAttribute(UserAttributes.TEMPLATES);	
		hash.put(
			UserAttributes.USER_TEMPLATES,
			(deliv_templates!=null)?deliv_templates:"N/A");
		Object address = ua.getAttribute(UserAttributes.ADDRESS);	
		hash.put(
			UserAttributes.USER_ADDRESS,
			(address!=null)?address:"N/A");
		Object c2aRateIndex = ua.getAttribute(UserAttributes.C2ARATEINDEX);	
		hash.put(
			UserAttributes.USER_C2ARATEINDEX,
			(c2aRateIndex!=null)?c2aRateIndex:new BigDecimal(1));
		Object ats2cRateIndex = ua.getAttribute(UserAttributes.ATS2CRATEINDEX);	
		hash.put(
			UserAttributes.USER_ATS2CRATEINDEX,
			(ats2cRateIndex!=null)?ats2cRateIndex:new BigDecimal(1));
		Object ratingFromDate = ua.getAttribute(UserAttributes.RATINGFROMDATE);	
		hash.put(
			UserAttributes.USER_RATINGFROMDATE,
			(ratingFromDate!=null)?ratingFromDate:new Date(System.currentTimeMillis()));
			
		hash.put(UserAttributes.USER_SINGLE_SEAT, ua.isSINGLE_SEAT() ? "1": "0");		
		hash.put(UserAttributes.USER_DISTRIB_ATS, new Integer(ua.getDISTRIB_ATS()));
		hash.put(UserAttributes.USER_DISTRIB_LINK, new Integer(ua.getDISTRIB_LINK()));
		hash.put(UserAttributes.USER_INTERACTIVE, new Boolean(ua.isINTERACTIVE()));
		hash.put(UserAttributes.USER_OUTSOURCE, ua.getOUTSOURCE());
		hash.put(UserAttributes.USER_AUTO_ASSIGN_SEARCH_LOCKED, new Boolean(ua.isAUTO_ASSIGN_SEARCH_LOCKED()));
		hash.put(UserAttributes.USER_AUTO_UPDATE, ua.isAUTO_UPDATE() ? "1": "0");
		hash.put(UserAttributes.USER_OTHER_FILES_ON_SSF, ua.isOTHER_FILES_ON_SSF() ? "1": "0");
		hash.put(UserAttributes.SEND_IMAGES_SURECLOSE, new Integer(ua.getSEND_IMAGES_FORCLOSURE()));
		hash.put(UserAttributes.SEND_REPORT_SURECLOSE, new Integer(ua.getSEND_REPORT_FORCLOSURE()));
		hash.put(UserAttributes.MODIFIED_BY, ua.getMODIFIED_BY() == null ? -1 : new Integer(ua.getMODIFIED_BY()));
		hash.put(UserAttributes.DATE_MODIFIED, new Date(ua.getDATE_MODIFIED().getTime()));
		return hash;
	}

	/**Copy an user from the main database into a community database
	 * Parameters:
	 *  user_id - the user loginname
	 *  comm - the community name
	 */
	/*public static void copyUserInCommunity(String user_id, String comm)
		throws BaseException {

		UserAttributes ua = UserManager.getUser(user_id);
		CommunityAttributes ca = CommunityUtils.getCommunityFromName(comm);
		Integer[] roles = UserUtils.getUserRoleAsInteger(ua);

		String stm = "";
		DBConnection connection =
			InstanceManager.getCurrentInstance().getDBConnection();

		for (int i = 0; i < roles.length; i++) {
			stm =
				"INSERT INTO "
					+ DBConstants.TABLE_USER_COMMUNITY
					+ " VALUES("
					+ ca.getAttribute(CommunityAttributes.ID)
					+ ", "
					+ ua.getAttribute(UserAttributes.ID)
					+ ", "
					+ roles[i]
					+ ",null,1)";

			try {
				connection.executeSQL(stm);
			} catch (Exception e) {
				throw new BaseException("SQLException:" + e.getMessage());
			}
		}
	}*/


	/*public static void deleteUser(Long id) throws BaseException {

		UserAttributes ua = getUserFromId(id.longValue());
		UserUtils.deleteUser(ua);
	}*/

	/*public static void deleteUser(String login) throws BaseException {

		UserAttributes ua = UserManager.getUser(login);
		UserUtils.deleteUser(ua);

	}*/
	/**
	 *
	 */
	/*public static void deleteUser(UserAttributes ua) throws BaseException {
		DBConnection connection =
			InstanceManager.getCurrentInstance().getDBConnection();
		String stm;

		Long[] userComm = UserUtils.getUserMembershipAsLong(ua);
		//String fullName = ua.getUserFullName();

		try {
			if (!isUserVPOAdmin(ua)) {
				for (int i = 0; i < userComm.length; i++) {
					UserUtils.deleteUser(ua, userComm[i].longValue());
				}
				UserManager.deleteUser(ua);
			} else
				throw new BaseException("User is VEMS ADMINISTRATOR !");
		} catch (Exception e) {
			throw new BaseException("SQLException:" + e.getMessage());
		}
	}*/

	/*public static void deleteUser(UserAttributes ua, long commId)
		throws BaseException {

		try {
			String fullName = ua.getUserFullName();

			if (isACommAdmin(ua, commId)) {
				throw new BaseException(
					"The User "
						+ fullName
						+ " can not be deleted! User has Community Administrator!");
			}
			DBConnection connection =
				InstanceManager.getCurrentInstance().getDBConnection();
			String stm;

			if (((BigDecimal) ua.getAttribute(UserAttributes.LASTCOMM))
				.longValue()
				== commId) {
				CommunityAttributes[] cas = UserUtils.getUserMembership(ua);
				if (cas.length == 1)
					cas[0] =
						CommunityUtils.getCommunityFromName(
							UserAttributes.COMM_DEFAULT_NAME);
				stm =
					"UPDATE "
						+ DBConstants.TABLE_USER
						+ " SET "
						+ UserAttributes.USER_LASTCOMM
						+ "="
						+ cas[0].getAttribute(CommunityAttributes.ID)
						+ " WHERE "
						+ UserAttributes.USER_ID
						+ "="
						+ ua.getAttribute(UserAttributes.ID);
				connection.executeSQL(stm);
			}

			stm =
				"DELETE FROM "
					+ DBConstants.TABLE_USER_COMMUNITY
					+ " WHERE "
					+ UserAttributes.USER_ID
					+ "="
					+ ua.getAttribute(UserAttributes.ID)
					+ " AND "
					+ CommunityAttributes.COMM_ID
					+ "="
					+ commId;
			connection.executeSQL(stm);

			UserRights.resetCommunityRights(
				((BigDecimal) ua.getAttribute(UserAttributes.ID)).longValue(),
				commId);
		} catch (Exception e) {
			throw new BaseException("SQLException:" + e.getMessage());
		}
	}*/

	public static boolean isTSAdmin(UserAttributes ua)
	throws BaseException {
		return (ua.getGROUP().intValue() == GroupAttributes.TA_ID);
	}

	public static boolean isCommAdmin(UserAttributes ua)
		throws BaseException {			
			return (ua.getGROUP().intValue() == GroupAttributes.CA_ID);
		
		}
	public static boolean isTSCAdmin(UserAttributes ua)throws BaseException {
		return (ua.getGROUP().intValue() == GroupAttributes.CCA_ID);
	}

	public static boolean isAgent(UserAttributes ua)
		throws BaseException {			
			return (ua.getGROUP().intValue() == GroupAttributes.AG_ID);
	
		}
	
	public static boolean isAbstractor(UserAttributes ua)
	throws BaseException {			
		return (ua.getGROUP().intValue() == GroupAttributes.ABS_ID);

	}
		
	/*public static boolean isACommAdmin(UserAttributes ua)
		throws BaseException {
		CommunityAttributes ca =
			InstanceManager.getCurrentInstance().getCurrentCommunity();
		return isACommAdmin(ua, ca);
	}*/

	/*public static boolean isACommAdmin(
		UserAttributes ua,
		CommunityAttributes ca)
		throws BaseException {

		return isACommAdmin(
			ua,
			((BigDecimal) ca.getAttribute(CommunityAttributes.ID)).longValue());
	}*/

	/*public static boolean isACommAdmin(UserAttributes ua, long commId)
		throws BaseException {

		long userId =
			((BigDecimal) ua.getAttribute(UserAttributes.ID)).longValue();
		return (UserRights.getCommunityRights(userId, commId) & 2) != 0;
	}*/



	public static boolean isUserCommAdmin(
		UserAttributes ua,
		CommunityAttributes ca)
		throws BaseException {

		long uid =
			((BigDecimal) ua.getAttribute(UserAttributes.ID)).longValue();
		long adminId =
			((BigDecimal) ca.getAttribute(CommunityAttributes.COMM_ADMIN))
				.longValue();

		return uid == adminId;
	}


	/**
	 *
	 */
	public static String getUserRoleList(UserAttributes ua)
		throws BaseException {

		String[] roles = UserUtils.getUserRole(ua);
		String list = (roles.length > 0) ? "" : " - ";

		for (int i = 0; i < roles.length; i++) {
			list = list + roles[i];
			if (i < roles.length - 1)
				list = list + ", ";
		}
		return list;
	}

	/**
	 *just for group for tsearch aplication
	 */
	public static String[] getUserRole(UserAttributes ua)
		throws BaseException {

	    DBConnection conn = null;
		try {
		    
			long uid =
				((BigInteger) ua.getAttribute(UserAttributes.ID)).longValue();

			String stm =
				"SELECT DISTINCT "
					+ GroupAttributes.GROUP_NAME
					+ " FROM "
					+ DBConstants.TABLE_USER
					+ ","
					+ DBConstants.TABLE_GROUP
					+ " WHERE "
					+ UserAttributes.USER_ID
					+ "="
					+ uid
					+ " AND "
					+ DBConstants.TABLE_USER
					+ "."
					+ GroupAttributes.GROUP_ID
					+ "="
					+ DBConstants.TABLE_GROUP
					+ "."
					+ GroupAttributes.GROUP_ID; 
			if (logger.isDebugEnabled())
				logger.debug(stm);
			
			conn = ConnectionPool.getInstance().requestConnection();
			DatabaseData result = conn.executeSQL(stm);
			
			int rows = result.getRowNumber();
			String[] roles = new String[rows];
			for (int i = 0; i < roles.length; i++) {
				roles[i] = (String) result.getValue(1, i);
			}
			
			return roles;
			
		} catch (Exception e) {
			throw new BaseException("SQLException:" + e.getMessage());
		} finally {
			try{
			    ConnectionPool.getInstance().releaseConnection(conn);
			}catch(BaseException e){
			    e.printStackTrace();
			}			
		}

	}

	public static String getUserRoleList(String login)
		throws BaseException {
		UserAttributes ua = UserManager.getUser(login, false);
		return UserUtils.getUserRoleList(ua);
	}

	public static byte[] getUserPicture(String login) throws BaseException {

		DBConnection conn = null;		

		try {
		    
		    String stm = "SELECT vup."
				+ UserAttributes.USER_PHOTO
				+ " FROM "
				+ DBConstants.TABLE_USER_PHOTO
				+ " vup "
				+ " , "
				+ DBConstants.TABLE_USER
				+ " vu "
				+ " WHERE "
				+ " vu."
				+ UserAttributes.USER_ID
				+ " = "
				+ " vup."
				+ UserAttributes.USER_ID
				+ " AND "
				+ " vu."
				+ UserAttributes.USER_LOGIN
				+ "=?";
			
		    conn = ConnectionPool.getInstance().requestConnection();
		    
			PreparedStatement pstmt = conn.prepareStatement( stm );
			pstmt.setString( 1, login);
			DatabaseData result = conn.executePrepQuery(pstmt);	
			pstmt.close();	
			
			if (result.getRowNumber() > 0) {
				return (byte[]) result.getValue(1, 0); 
			} else {
				try {
					FileInputStream fis =
						new FileInputStream(
							new File("jsp/images/0photo.jpg"));
					StrUtil strUtil  = new StrUtil(fis);
					return (byte[]) strUtil.getStreamBytes();
				} catch (IOException e) {
					throw new BaseException(
						"UserUtils.getPicture(login).IOException"
							+ e.getMessage());
				}
			}
			
		} catch (Exception e) {
			throw new BaseException("SQLException:" + e.getMessage());
		} finally {
			try{
			    ConnectionPool.getInstance().releaseConnection(conn);
			}catch(BaseException e){
			    e.printStackTrace();
			}			
		}
	}

	public static byte[] getUserResume(BigDecimal uid)
		throws BaseException {

		DBConnection conn = null;
		try {
		    
		    conn = ConnectionPool.getInstance().requestConnection();
			Statement stmt = conn.createStatement();
			
			ResultSet resultBlob =
				stmt.executeQuery(
					"SELECT "
						+ UserAttributes.USER_RESUME
						+ " FROM "
						+ DBConstants.TABLE_USER_RESUME
						+ " WHERE "
						+ UserAttributes.USER_ID
						+ "="
						+ uid);

			if (resultBlob.next()) {
				Blob blobPhoto = resultBlob.getBlob(1);
				return blobPhoto.getBytes(1, (int) blobPhoto.length());
			} else
				throw new BaseException("SQLException: Unable to get the User's Resume!");
			
		} catch (Exception e) {
			throw new BaseException("SQLException:" + e.getMessage());
		} finally {
			try{
			    ConnectionPool.getInstance().releaseConnection(conn);
			}catch(BaseException e){
			    e.printStackTrace();
			}			
		}
	}

	public static String getUserResumeName(BigDecimal uid)
		throws BaseException {

		DBConnection conn = null;
		try {

			String stm =
				"SELECT "
					+ DBConstants.USER_RESUME_TITLE
					+ " FROM "
					+ DBConstants.TABLE_USER_RESUME
					+ " WHERE "
					+ UserAttributes.USER_ID
					+ "="
					+ uid;

			conn = ConnectionPool.getInstance().requestConnection();
			DatabaseData result = conn.executeSQL(stm);
			
			return (String) result.getValue(DBConstants.USER_RESUME_TITLE, 0);

		} catch (Exception e) {
			throw new BaseException("SQLException:" + e.getMessage());
		} finally {
			try{
			    ConnectionPool.getInstance().releaseConnection(conn);
			}catch(BaseException e){
			    e.printStackTrace();
			}			
		}
	}

	public static String getSize(long size) throws BaseException {
		double bytes = new Double(size).doubleValue();
		if (bytes < 1024)
			return new FormatNumber(FormatNumber.INTEGER).getNumber(bytes)
				+ "B";
		else if ((bytes / 1024) < 1024)
			return new FormatNumber(FormatNumber.ONE_DECIMAL).getNumber(
				bytes / 1024)
				+ "KB";
		else
			return new FormatNumber(FormatNumber.ONE_DECIMAL).getNumber(
				bytes / (1024 * 1024))
				+ "M";
	}
	
	public static String getUserResumeSize(BigDecimal uid)
		throws BaseException {

		DBConnection conn = null;
		try {

			String stm =
				"SELECT "
					+ DBConstants.USER_RESUME_SIZE
					+ " FROM "
					+ DBConstants.TABLE_USER_RESUME
					+ " WHERE "
					+ UserAttributes.USER_ID
					+ "="
					+ uid;

			conn = ConnectionPool.getInstance().requestConnection();
			DatabaseData result = conn.executeSQL(stm);
			
			return getSize(Long.parseLong(result.getValue(DBConstants.USER_RESUME_SIZE, 0).toString()));

		} catch (Exception e) {
			throw new BaseException("SQLException:" + e.getMessage());
		} finally {
			try{
			    ConnectionPool.getInstance().releaseConnection(conn);
			}catch(BaseException e){
			    e.printStackTrace();
			}			
		}
	}
	

		
	/*public static CommunityAttributes getUserLastCommAccessed(UserAttributes ua)
		throws BaseException {

		CommunityAttributes ca;
		try {
			ca =
				CommunityUtils.getCommunityFromId(
					((BigDecimal) ua.getAttribute(UserAttributes.LASTCOMM))
						.longValue());
		} catch (Exception e) {
			CommunityAttributes[] cas = UserUtils.getUserMembership(ua);
			if (cas.length > 0) {
				ca = cas[0];
			} else
				throw new BaseException(
					"No active Communities for User " + ua.getUserFullName());
		}

		return ca;
	}

	public static void setUserLastCommAccessed(
		UserAttributes ua,
		String commName)
		throws BaseException {

		try {
			CommunityAttributes ca =
				CommunityUtils.getCommunityFromName(commName);
			DBConnection connection =
				InstanceManager.getCurrentInstance().getDBConnection();
			String stm =
				"UPDATE "
					+ UserAttributes.TABLE_USER
					+ " SET "
					+ UserAttributes.USER_LASTCOMM
					+ "="
					+ ca.getAttribute(CommunityAttributes.ID)
					+ " WHERE "
					+ UserAttributes.USER_ID
					+ "="
					+ ua.getAttribute(UserAttributes.ID);
			connection.executeSQL(stm);
		} catch (Exception e) {
			throw new BaseException("SQLException" + e.getMessage());
		}
	}
	
	public static void setUserLastCommAccessed(UserAttributes ua, long commId)
		throws BaseException {

		try {
			DBConnection connection =
				InstanceManager.getCurrentInstance().getDBConnection();
			String stm =
				"UPDATE "
					+ UserAttributes.TABLE_USER
					+ " SET "
					+ UserAttributes.USER_LASTCOMM
					+ "="
					+ commId
					+ " WHERE "
					+ UserAttributes.USER_ID
					+ "="
					+ ua.getAttribute(UserAttributes.ID);
			connection.executeSQL(stm);
		} catch (Exception e) {
			throw new BaseException("SQLException" + e.getMessage());
		}
	}
	*/
	
	public static boolean hasResume(BigDecimal uid) throws BaseException {
		
	    DBConnection conn = null;
	    try {
			
			String stm =
				"SELECT "
					+ UserAttributes.USER_ID
					+ " FROM "
					+ DBConstants.TABLE_USER_RESUME
					+ " WHERE "
					+ UserAttributes.USER_ID
					+ "="
					+ uid;
			
			conn = ConnectionPool.getInstance().requestConnection();
			DatabaseData result = conn.executeSQL(stm);
			
			int resultRows = result.getRowNumber();
			return (resultRows != 0);				

		} catch (Exception e) {
			throw new BaseException("SQLException" + e.getMessage());
		} finally {
			try{
			    ConnectionPool.getInstance().releaseConnection(conn);
			}catch(BaseException e){
			    e.printStackTrace();
			}			
		}
	}

	public static boolean hasResume(UserAttributes ua)
		throws BaseException {
		return hasResume((BigDecimal) ua.getAttribute(UserAttributes.ID));
	}

	public static boolean hasPhoto(BigDecimal uid) throws BaseException {
		
	    DBConnection conn = null;
	    try {
			
			String stm =
				"SELECT "
					+ UserAttributes.USER_ID
					+ " FROM "
					+ DBConstants.TABLE_USER_PHOTO
					+ " WHERE "
					+ UserAttributes.USER_ID
					+ "="
					+ uid;
			
			conn = ConnectionPool.getInstance().requestConnection();
			DatabaseData result = conn.executeSQL(stm);
			
			int resultRows = result.getRowNumber();

			return (resultRows != 0);				

		} catch (Exception e) {
			throw new BaseException("SQLException" + e.getMessage());
		} finally {
			try{
			    ConnectionPool.getInstance().releaseConnection(conn);
			}catch(BaseException e){
			    e.printStackTrace();
			}			
		}
	}
	
	public static boolean hasPhoto(UserAttributes ua) throws BaseException {
		return hasPhoto((BigDecimal) ua.getAttribute(UserAttributes.ID));
	}

	/*
	 * just for TSAdministrator
	 */
	public static String[] getUserMembershipAsString(UserAttributes ua)
		throws BaseException {
	    
		
		long uid =
			((BigInteger) ua.getAttribute(UserAttributes.ID)).longValue();
		
		String stm =
			"SELECT DISTINCT "
				+ CommunityAttributes.COMMUNITY_NAME
				+ " FROM "
				+ DBConstants.TABLE_USER
				+ ", "
				+ DBConstants.TABLE_COMMUNITY
				+ " WHERE "
				+ UserAttributes.USER_ID
				+ "="
				+ uid
				+ " AND "
				+ DBConstants.TABLE_COMMUNITY
				+ "."
				+ CommunityAttributes.COMMUNITY_ID
				+ "="
				+ DBConstants.TABLE_USER
				+ "."
				+ UserAttributes.USER_COMMID;
		
		String[] comm = new String[0];
		
		DBConnection conn = null;		
		try {
		    
		    conn = ConnectionPool.getInstance().requestConnection();
		    DatabaseData result = conn.executeSQL(stm);
		
			int rows = result.getRowNumber();
			comm = new String[rows];
			for (int i = 0; i < rows; i++) {
				comm[i] = (String) result.getValue(1, i);
			}
			
		} catch (Exception e) {
		    e.printStackTrace();
		} finally {
			try{
			    ConnectionPool.getInstance().releaseConnection(conn);
			}catch(BaseException e){
			    e.printStackTrace();
			}			
		}

		return comm;
	}	
	/**
	 *
	 */
	public static String getUserMembershipAsStringList(String login)
		throws BaseException {

		UserAttributes ua = UserManager.getUser(login, false);
		String[] communities = getUserMembershipAsString(ua);

		String list = (communities.length > 0) ? "" : " - ";

		for (int i = 0; i < communities.length; i++) {
			list = list + communities[i];
			if (i < communities.length - 1)
				list = list + ", ";
		}
		return list;
	}
	
	public static String getSpecificImg(String fileName) {
		try {
			String ext = fileName.substring(fileName.length() - 4);
			if (hashCorespImg.containsKey(ext))
				return (String) hashCorespImg.get(ext);
			else
				return IMG_FILE_UNKNOWN;
		} catch (Exception e) {
			//in cazul in care fisierul nu are extensie.
			return IMG_FILE_UNKNOWN;
		}
	}
	
	
	public static String SQL_TEMPLATES_LIST_FOR_USER_PROFILE = 
			"SELECT " + 
				DBConstants.TABLE_COMMUNITY_TEMPLATES + "." + DBConstants.FIELD_COMMUNITY_TEMPLATES_ID + "," +
				DBConstants.FIELD_COMMUNITY_TEMPLATES_COMM_ID + "," +
				DBConstants.FIELD_COMMUNITY_TEMPLATES_NAME + "," + 
				DBConstants.FIELD_COMMUNITY_TEMPLATES_SHORT_NAME  + "," +
				DBConstants.FIELD_COMMUNITY_TEMPLATES_FILENAME  + "," +
				DBConstants.FIELD_USER_TEMPLATES_USER_ID + "," +
				DBConstants.FIELD_USER_TEMPLATES_ENABLE_PRODUCT + "," +
				DBConstants.FIELD_USER_TEMPLATES_EXPORT_FORMAT +
				" FROM " +
					DBConstants.TABLE_COMMUNITY_TEMPLATES + 
					" LEFT JOIN " + DBConstants.TABLE_USER_TEMPLATES + 
					" ON " + 
						DBConstants.TABLE_COMMUNITY_TEMPLATES + "." + DBConstants.FIELD_COMMUNITY_TEMPLATES_ID + "=" + 
						DBConstants.TABLE_USER_TEMPLATES + "." + DBConstants.FIELD_USER_TEMPLATES_TEMPLATE_ID + 
						" AND " +
							DBConstants.TABLE_USER_TEMPLATES + "." + DBConstants.FIELD_USER_TEMPLATES_USER_ID + " = ? " +
				" WHERE " +
				DBConstants.TABLE_COMMUNITY_TEMPLATES + "." + DBConstants.FIELD_COMMUNITY_COMM_ID + "= ?" + 
				" order by " + DBConstants.FIELD_COMMUNITY_TEMPLATES_NAME;

	
	public static String SQL_TEMPLATE_FOR_USER_PROFILE = SQL_TEMPLATES_LIST_FOR_USER_PROFILE + " AND " +
			DBConstants.TABLE_USER_TEMPLATES + "." + DBConstants.FIELD_USER_TEMPLATES_TEMPLATE_ID + "= ?";
	
	public static boolean templateIsDoc(String templateName) {
		String extension = templateName.substring(templateName.lastIndexOf(".") + 1);
		return "doc".equalsIgnoreCase(extension);
	}
	
	public static boolean templateIsHtml(String templateName) {
		String extension = templateName.substring(templateName.lastIndexOf(".") + 1);
		return "htm".equalsIgnoreCase(extension) || "html".equalsIgnoreCase(extension);
	}
	
	/**
	 * Retrieves user templates as HTML String
	 * @param userId
	 * @return String retHTML
	 */
	public static String getHTMLViewPolicy(long userId) {

		long commId = -1;

		try {
			UserAttributes ua = getUserFromId(userId);
			commId = ua.getCOMMID().longValue();
		} catch (BaseException e1) {
			e1.printStackTrace();
		}

		String retHTML = "";

		List<CommunityUserTemplatesMapper> templates = DBManager
				.getSimpleTemplate().query(SQL_TEMPLATES_LIST_FOR_USER_PROFILE,
						new CommunityUserTemplatesMapper(), userId, commId);
		
		retHTML = "<table>";
		LinkedHashMap<Integer,HashMap> products = CommunityProducts.getProductsMap(commId);
		
		for (CommunityUserTemplatesMapper cutm : templates) {

			if (!StringUtils.areAllEmpty(cutm.getName(), cutm.getShortName())) {
				boolean userHasTemplate = (cutm.getUserId() == userId);

				retHTML += "<tr>";				
				retHTML += "<td>"+(userHasTemplate ? "<strong>" : "")+jsEscapedStr(cutm.getName()) + (userHasTemplate ? "</strong>" : "") +"</td>"+
				"<td>"+(userHasTemplate ? "<strong>" : "")+jsEscapedStr(cutm.getShortName()) + (userHasTemplate ? "</strong>" : "") +"</td>";
				retHTML += "<td>";
				if(userHasTemplate) {
					for (Entry<Integer,HashMap> e : products.entrySet()) {
						retHTML += "<a class='tooltip' href='#'><input type='checkbox' disabled "+(isTemplateEnabledOnProduct(cutm.getEnableProduct(),e.getKey())?" checked ":"") + "/><span>"+e.getValue().get(Products.FIELD_NAME)+"</span></a>";
					}
					retHTML += "&nbsp;&nbsp;&nbsp;<a class='tooltip' href='#'><input type='checkbox' disabled "+(isTemplateEnabledOnProduct(cutm.getEnableProduct(), Products.INTERNAL_ONLY) ? " checked " : "") + "/><span>Internal Only</span></a>";
					retHTML += "<a class='tooltip' href='#'><input type='checkbox' disabled "+(isTemplateEnabledOnProduct(cutm.getEnableProduct(), Products.RPC_ASSOCIATE_ONLY) ? " checked " : "") + "/><span>RPC Associate Only</span></a>";
				}
				retHTML += "</td>";
				retHTML += "<td>";
				if(userHasTemplate) {
					String templateName = cutm.getPath();
					if (templateIsDoc(templateName) || templateIsHtml(templateName)) {
						long templateId = cutm.getTemplateId();
						int exportFormat = cutm.getExportFormat();
						String selectedTiff = (TemplateBuilder.TIFF_EXPORT_FORMAT==exportFormat)?" selected":"";
						String selectedPdf = (TemplateBuilder.PDF_EXPORT_FORMAT==exportFormat)?" selected":"";
						String selectedNoChange = (TemplateBuilder.NO_CHANGE_EXPORT_FORMAT==exportFormat)?" selected":"";
						retHTML += "<SELECT disabled=\"disabled\" size=1 title=\"Export Format\" name =\"" 
								+ RequestParams.USER_TEMPLATES_EXPORT_FORMAT + "_" + templateId + "\" id=\"" 
								+ RequestParams.USER_TEMPLATES_EXPORT_FORMAT + "_" + templateId + "\">" +
								"<option value=\"" + TemplateBuilder.TIFF_EXPORT_FORMAT + "\"" + selectedTiff + ">TIFF</option>" +
								"<option value=\"" + TemplateBuilder.PDF_EXPORT_FORMAT + "\"" + selectedPdf + ">PDF</option>" +
								"<option value=\"" + TemplateBuilder.NO_CHANGE_EXPORT_FORMAT + "\"" + selectedNoChange + ">No change</option>" +
								"</SELECT>";
					}
				}
				retHTML += "</td>";
				retHTML += "</tr>";	
			}
		}
		retHTML += "</table>";
		return retHTML;
	}
	
	public static boolean isTemplateEnabledOnProduct(long enabledProduct, int productId) {
		return ((1<<productId) & enabledProduct) != 0; 	
	}
	
	public static long calculateTemplateEnabledOnProduct(String[] productIds) {
		if(productIds==null) {
			return 0;
		}
		long enabledProduct = 0;
		for(String productIdString : productIds) {
			int productId = Integer.parseInt(productIdString);
			enabledProduct += (1<<productId);
		}
		return enabledProduct;
	}
	
	public static boolean containsId(String idsString, String idMatch) {
		idsString = DB_USER_TEMPLATES_DELIMITER + idsString + DB_USER_TEMPLATES_DELIMITER;
		if (idsString.indexOf(DB_USER_TEMPLATES_DELIMITER + idMatch + DB_USER_TEMPLATES_DELIMITER) >= 0)
			return true;
		return false;
	}
	
	public static String jsEscapedStr(String strJSIn) {
		if(StringUtils.isEmpty(strJSIn)) return "";
		return strJSIn.replaceAll("'","\\\\'");
	}
	
	
	/**
	 * 
	 * @param userId
	 * @return HashMap with user templates
	 * @throws TemplatesException
	 */
	public static List<CommunityTemplatesMapper> getUserTemplates(long userId, int productId) throws TemplatesException {

		int filterType = -1;
//		long startTime = System.currentTimeMillis();
		List<CommunityTemplatesMapper> userTemplates = getUserTemplates(userId, -1, filterType, productId);
//		long endTime = System.currentTimeMillis();
//		System.err.println("getUserTemplates time " + ((endTime - startTime)) + " miliseconds ");
		
		return userTemplates;
	}

	
	private static final String SQL_GET_TEMPLATES_INGORE_USER_NAME = " FROM " + 
		DBConstants.TABLE_COMMUNITY_TEMPLATES + " WHERE " + 
		DBConstants.FIELD_COMMUNITY_TEMPLATES_COMM_ID + "=?";
	private static final String SQL_GET_ALL_TEMPLATES_FOR_USER = " FROM " +
		DBConstants.TABLE_COMMUNITY_TEMPLATES + " WHERE " + 
		DBConstants.FIELD_COMMUNITY_TEMPLATES_ID + " IN " + 
				" (SELECT " + DBConstants.FIELD_USER_TEMPLATES_TEMPLATE_ID + 
				" FROM " + DBConstants.TABLE_USER_TEMPLATES + 
				" WHERE " + DBConstants.FIELD_USER_TEMPLATES_USER_ID + "=? " +
				" AND " + 
					"( " + DBConstants.FIELD_USER_TEMPLATES_ENABLE_PRODUCT + " & ? " + 
					" OR " + DBConstants.FIELD_USER_TEMPLATES_ENABLE_PRODUCT + " is null " + ")" +
				" ) ";
	private static final String SQL_GET_ALL_TEMPLATES_FOR_USER_FILENAME = "SELECT * FROM " +
			DBConstants.TABLE_COMMUNITY_TEMPLATES + " WHERE " + 
			DBConstants.FIELD_COMMUNITY_TEMPLATES_ID + " IN " + 
					" (SELECT " + DBConstants.FIELD_USER_TEMPLATES_TEMPLATE_ID + 
					" FROM " + DBConstants.TABLE_USER_TEMPLATES + 
					" WHERE " + DBConstants.FIELD_USER_TEMPLATES_USER_ID + "=? " +
					" AND " + 
						"( " + DBConstants.FIELD_USER_TEMPLATES_ENABLE_PRODUCT + " & ? " + 
						" OR " + DBConstants.FIELD_USER_TEMPLATES_ENABLE_PRODUCT + " is null " + ")" +
					" ) AND " + 
					DBConstants.FIELD_COMMUNITY_TEMPLATES_FILENAME + " = ?";
	private static final String SQL_GET_TEMPLATES_FOR_USER_FILTER_BOILER_EXCLUDE = 
		" FROM " +
		DBConstants.TABLE_COMMUNITY_TEMPLATES + " WHERE " + 
		DBConstants.FIELD_COMMUNITY_TEMPLATES_ID + " IN " + 
			" (SELECT " + DBConstants.FIELD_USER_TEMPLATES_TEMPLATE_ID + 
			" FROM " + DBConstants.TABLE_USER_TEMPLATES + 
			" WHERE " + DBConstants.FIELD_USER_TEMPLATES_USER_ID + "=? " +
			" AND " + 
			"( " + DBConstants.FIELD_USER_TEMPLATES_ENABLE_PRODUCT + " & ? " + 
			" OR " + DBConstants.FIELD_USER_TEMPLATES_ENABLE_PRODUCT + " is null " + ")" +
			" ) " +
			" AND INSTR(" + DBConstants.FIELD_COMMUNITY_TEMPLATES_FILENAME + "," + "'" + TemplatesInitUtils.TEMPLATE_CB_CONTAINS+"')=0 " +
			" AND INSTR(" + DBConstants.FIELD_COMMUNITY_TEMPLATES_FILENAME + "," + "'" + TemplatesInitUtils.TEMPLATE_BP_CONTAINS+"')=0 ";;
	
	private static final String SQL_GET_TEMPLATES_FOR_USER_FILTER_ONLY_START = 
			" FROM " +
			DBConstants.TABLE_COMMUNITY_TEMPLATES + " WHERE " + 
			DBConstants.FIELD_COMMUNITY_TEMPLATES_ID + " IN " + 
				" (SELECT " + DBConstants.FIELD_USER_TEMPLATES_TEMPLATE_ID + 
				" FROM " + DBConstants.TABLE_USER_TEMPLATES + 
				" WHERE " + DBConstants.FIELD_USER_TEMPLATES_USER_ID + "=? " +
				" AND " + 
				"( " + DBConstants.FIELD_USER_TEMPLATES_ENABLE_PRODUCT + " & ? " + 
				" OR " + DBConstants.FIELD_USER_TEMPLATES_ENABLE_PRODUCT + " is null " + ")" +
				" ) " +
				" AND ( ";
	private static final String SQL_GET_TEMPLATES_FOR_USER_FILTER_BP_ONLY_MIDDLE = 
			" ( INSTR(" + DBConstants.FIELD_COMMUNITY_TEMPLATES_FILENAME + ", ?)!=0 OR INSTR(" + DBConstants.FIELD_COMMUNITY_TEMPLATES_FILENAME + ", ?)!=0 )";
	private static final String SQL_GET_TEMPLATES_FOR_USER_FILTER_ONLY_MIDDLE = 
			" ( INSTR(" + DBConstants.FIELD_COMMUNITY_TEMPLATES_FILENAME + ", ?)!=0 )";
	private static final String SQL_GET_TEMPLATES_FOR_USER_FILTER_ONLY_END = ")"; 
	
	/**
	 * Loads the templates including the content for the given parameters
	 * @param userId
	 * @param commId
	 * @param filterType
	 * @param productId
	 * @return
	 */
	public static List<CommunityTemplatesMapper> getUserTemplates(
			long userId, long commId, int filterType, int productId) {
		return getUserTemplates(userId, commId, filterType, productId, true);
	}
	
	/**
	 * 
	 * @param agentId the agent that has the templates
	 * @param commId only used if filterType is {@link UserUtils.FILTER_IGNORE_USER}
	 * @param filterType
	 * @param productId
	 * @param fillContent if false the file content will not be loaded
	 * @return
	 */
	public static List<CommunityTemplatesMapper> getUserTemplates(
			long agentId, long commId, int filterType, int productId, boolean fillContent) {

		int enableProduct = (1 << 11) - 1;
		if (productId != -1) {
			enableProduct = 1 << productId;
		}
		
		String sqlStart = null;
		if(fillContent) {
			sqlStart =  "SELECT * ";
		} else {
			sqlStart =  "SELECT " + 
					DBConstants.FIELD_COMMUNITY_TEMPLATES_ID + ", " +
					DBConstants.FIELD_COMMUNITY_TEMPLATES_COMM_ID + ", " +
					DBConstants.FIELD_COMMUNITY_TEMPLATES_NAME + ", " +
					DBConstants.FIELD_COMMUNITY_TEMPLATES_SHORT_NAME + ", " +
					DBConstants.FIELD_COMMUNITY_TEMPLATES_LAST_UPDATE + ", " +
					DBConstants.FIELD_COMMUNITY_TEMPLATES_FILENAME
					;
		}
		
		// if any filter was defined apply it when retrieving templates
		List<CommunityTemplatesMapper> templates = new ArrayList<CommunityTemplatesMapper>();
		try {

			switch (filterType) {
			case FILTER_BOILER_PLATES_EXCLUDE:
				templates = DBManager.getSimpleTemplate().query(
						sqlStart + SQL_GET_TEMPLATES_FOR_USER_FILTER_BOILER_EXCLUDE,
						new CommunityTemplatesMapper(),
						agentId,
						enableProduct);
				break;
			case FILTER_BOILER_PLATES_ONLY:
				templates = DBManager.getSimpleTemplate().query(
						sqlStart 
							+ SQL_GET_TEMPLATES_FOR_USER_FILTER_ONLY_START
							+ SQL_GET_TEMPLATES_FOR_USER_FILTER_BP_ONLY_MIDDLE
							+ SQL_GET_TEMPLATES_FOR_USER_FILTER_ONLY_END,
						new CommunityTemplatesMapper(),
						agentId,
						enableProduct,
						TemplatesInitUtils.TEMPLATE_BP_CONTAINS, TemplatesInitUtils.TEMPLATE_CB_CONTAINS);
				break;
			case FILTER_TSD_ONLY:
				templates = DBManager.getSimpleTemplate().query(
						sqlStart 
							+ SQL_GET_TEMPLATES_FOR_USER_FILTER_ONLY_START
							+ SQL_GET_TEMPLATES_FOR_USER_FILTER_ONLY_MIDDLE
							+ SQL_GET_TEMPLATES_FOR_USER_FILTER_ONLY_END,
						new CommunityTemplatesMapper(),
						agentId,
						enableProduct,
						TemplatesInitUtils.TEMPLATE_TSD_START);
				break;
			case FILTER_TSD_AND_BP_ONLY:
				templates = DBManager.getSimpleTemplate().query(
						sqlStart 
							+ SQL_GET_TEMPLATES_FOR_USER_FILTER_ONLY_START
							+ SQL_GET_TEMPLATES_FOR_USER_FILTER_ONLY_MIDDLE
							+ " OR " 
							+ SQL_GET_TEMPLATES_FOR_USER_FILTER_BP_ONLY_MIDDLE
							+ SQL_GET_TEMPLATES_FOR_USER_FILTER_ONLY_END,
						new CommunityTemplatesMapper(),
						agentId,
						enableProduct,
						TemplatesInitUtils.TEMPLATE_TSD_START, 
						TemplatesInitUtils.TEMPLATE_CB_CONTAINS, TemplatesInitUtils.TEMPLATE_BP_CONTAINS);
				break;
			case FILTER_IGNORE_USER:
				templates = DBManager.getSimpleTemplate().query(
						sqlStart + SQL_GET_TEMPLATES_INGORE_USER_NAME,
						new CommunityTemplatesMapper(),
						commId);
				break;
			default:
				templates = DBManager.getSimpleTemplate().query(
						sqlStart + SQL_GET_ALL_TEMPLATES_FOR_USER,
						new CommunityTemplatesMapper(),
						agentId,
						enableProduct);
				break;
			}

		} catch (Exception e) {
			e.printStackTrace();
		}
		return templates;
	}
	
	public static CommunityTemplatesMapper getTemplateByFileName(long agentId, int productId, String templateName) {
		int enableProduct = (1 << 11) - 1;
		if (productId != -1) {
			enableProduct = 1 << productId;
		}
		List<CommunityTemplatesMapper> templates = DBManager.getSimpleTemplate().query(
				SQL_GET_ALL_TEMPLATES_FOR_USER_FILENAME,
				new CommunityTemplatesMapper(),
				agentId,
				enableProduct,
				templateName);
		if(!templates.isEmpty()) {
			//should be just one file but just in case...
			return templates.get(0);
		}
		return null;
	}
	
	public static CommunityTemplatesMapper getTemplateById(long templateId) {
		CommunityTemplatesMapper template = null;
		try {
			template = DBManager.getSimpleTemplate().queryForObject(
					"SELECT * FROM " + DBConstants.TABLE_COMMUNITY_TEMPLATES + " WHERE " + DBConstants.FIELD_COMMUNITY_TEMPLATES_ID + " = ?",
					new CommunityTemplatesMapper(),
					templateId);
		} catch (Exception e) {
			logger.error("Something happpened while loading template id " + templateId, e);
		}
		return template;
	}
	
	
	public static int getUserTemplatesCount(long userId, long productId) {
		try {
		return DBManager.getSimpleTemplate().queryForInt(
				"SELECT COUNT(*) " + SQL_GET_ALL_TEMPLATES_FOR_USER,
				new CommunityTemplatesMapper(), 
				userId);
		}catch (Exception e) {
			e.printStackTrace();
		}
		return 0;
	}
	
	public static HashMap getTemplate(int templateId) {
		String sqlTemplas = "SELECT * FROM "
			+ "TS_COMMUNITY_TEMPLATES WHERE TEMPLATE_ID="
			+ templateId;
		DBConnection conn = null;
		HashMap templateHash = new HashMap();
		try {
		    
		    conn = ConnectionPool.getInstance().requestConnection();
			DatabaseData data = conn.executeSQL(sqlTemplas);
			
			for (int i = 0; i<data.getRowNumber(); i++){
				templateHash.put(TEMPLATE_ID, data.getValue(DB_TEMPLATE_ID,i).toString());
				templateHash.put(TEMPLATE_NAME, data.getValue(DB_TEMPLATE_NAME,i).toString());
				templateHash.put(TEMPLATE_SHORT, data.getValue(DB_TEMPLATE_SHORT,i).toString());
				templateHash.put(TEMPLATE_LAST_UPDATE, data.getValue(DB_TEMPLATE_LAST_UPDATE,i).toString());
				templateHash.put(TEMPLATE_PATH, data.getValue(DB_TEMPLATE_PATH,i).toString());
			}
			
			if (templateId == -1) {
				templateHash.put(TEMPLATE_ID, "-1");
				templateHash.put(TEMPLATE_NAME, "XMLedATSFields");
				templateHash.put(TEMPLATE_SHORT, "xATSf");
				//templateHash.put(TEMPLATE_LAST_UPDATE, data.getValue(DB_TEMPLATE_LAST_UPDATE,i).toString());
				//templateHash.put(TEMPLATE_PATH, data.getValue(DB_TEMPLATE_PATH,i).toString());
			}
			
			if (templateId == -2) {
				templateHash.put(TEMPLATE_ID, "-2");
				templateHash.put(TEMPLATE_NAME, "HTMLedATSFields");
				templateHash.put(TEMPLATE_SHORT, "hATSf");
				//templateHash.put(TEMPLATE_LAST_UPDATE, data.getValue(DB_TEMPLATE_LAST_UPDATE,i).toString());
				//templateHash.put(TEMPLATE_PATH, data.getValue(DB_TEMPLATE_PATH,i).toString());
			}
			
		} catch (BaseException e) {
			logger.error(e);
		} finally{
			try{
			    ConnectionPool.getInstance().releaseConnection(conn);
			}catch(BaseException e){
				logger.error(e);
			}			
		}
		return templateHash;
	}
	
	public static String getTemplatePolicyName(int commId, String templatePath) {
		String sqlTemplas = "SELECT name FROM "
			+ "TS_COMMUNITY_TEMPLATES WHERE COMM_ID=?"
			+ " AND PATH=?";
		String policyName = "";
		try {
			List<Map<String,Object>> data = DBManager.getSimpleTemplate().queryForList(sqlTemplas, commId, templatePath); 
			if (data.size()==1) {
				policyName = (String)data.get(0).get("name");
			}
		} 
		catch (DataAccessException e) {
			e.printStackTrace();
		}
		return policyName;
	}
	
	public static boolean isTemplatePolicyNameAlreadySaved(int commId, String templatePolicyName) {
		boolean result = false;
		
		String sqlTemplas = "SELECT " + DBConstants.FIELD_COMMUNITY_TEMPLATES_ID + " FROM "
			+ "TS_COMMUNITY_TEMPLATES WHERE COMM_ID=?"
			+ " AND NAME=?";
		
		try {
			List<Map<String,Object>> data = DBManager.getSimpleTemplate().queryForList(sqlTemplas, commId, templatePolicyName); 
			if (data.size()>0) {
				result = true;
			}
		} 
		catch (DataAccessException e) {
			e.printStackTrace();
		}
		
		return result;
	}
	
	public static boolean isTemplatePolicyFileNameAlreadySaved(int commId, String templatePolicyFileName) {
		boolean result = false;
		
		String sqlTemplas = "SELECT " + DBConstants.FIELD_COMMUNITY_TEMPLATES_ID + " FROM "
			+ "TS_COMMUNITY_TEMPLATES WHERE COMM_ID=?"
			+ " AND PATH=?";
		
		try {
			List<Map<String,Object>> data = DBManager.getSimpleTemplate().queryForList(sqlTemplas, commId, templatePolicyFileName); 
			if (data.size()>0) {
				result = true;
			}
		} 
		catch (DataAccessException e) {
			e.printStackTrace();
		}
		
		return result;
	}
	
	public static boolean hasPolicy(UserAttributes ua,int productId) {
		long userId = ua.getID().longValue();
		List<CommunityTemplatesMapper> templatesUser = null;
		try {
			templatesUser = getUserTemplates(userId,productId);
		} catch (TemplatesException e) {
			e.printStackTrace();
			return false;
		}
		if (templatesUser.size() > 0)
			return true;
		return false;
		
	}
	
	// parameters: String List of Id's separated by :
	// returns: String array with every id
	public String[] explodeToList(String idContent)
	{
		String   sepWord = DB_USER_TEMPLATES_DELIMITER;
		String[] idList; 
		
			if (idContent.indexOf(sepWord)==-1)
			{
			    idList = new String[1];
				idList[0] = idContent;
			}
			else
			{
			    idList = idContent.split(sepWord);
			}
		
		
	 return idList;
	}
	
	////parameters: String array of id, String separator word
	/// returns: String List of id's
	
	
	public String implodeToString (String[] idList, String sepWord)
	{		
		String toString = "";
		for (int i=0;i<idList.length-1;i++)
			toString = idList[i]+sepWord;
		    toString = idList[idList.length-1];
			
     return toString;					
	}
	
	public static boolean isPasswordValid(String password) {
		int enforcedRules[] = new int[] { 0, 0, 0, 0 };
				
	     for ( int i = 0; i < password.length(); ++i ) {
             char ch = password.charAt( i );
             if( ch>='a' && ch<='z' ) enforcedRules[0] = 1;
             if( ch>='A' && ch<='Z') enforcedRules[1] = 1;
             if( ch>='0' && ch<='9')  enforcedRules[2] = 1;
             if(ch=='~' || ch=='!' || ch=='@' || ch=='#' || ch=='$' || ch=='%' ||
            		 ch=='^' || ch=='&' || ch=='*' || ch=='(' || ch==')' ||
            		 ch=='-' || ch=='+' || ch=='=' || ch=='_' || ch=='.'  ) 
            	 enforcedRules[3] = 1;
            }
             
		return ((enforcedRules[0] + enforcedRules[1] + enforcedRules[2] + enforcedRules[3])>=3)
			&& (password.length() >=8)
			;
	}
	
	
	public static Boolean canEditIgnorePrivacyStatement(UserAttributes ua){
		boolean isEditable = 
			(ua.getGROUP().intValue()==GroupAttributes.CA_ID)||
			(ua.getGROUP().intValue()==GroupAttributes.CCA_ID)||
			(ua.getGROUP().intValue()==GroupAttributes.TA_ID)||
			(ua.getGROUP().intValue()==GroupAttributes.AM_ID);
		return isEditable;
	}

	

}
