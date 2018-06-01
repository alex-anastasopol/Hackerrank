package ro.cst.tsearch.database;

import static ro.cst.tsearch.utils.DBConstants.TABLE_SEARCH;

import java.util.Map;

import org.apache.log4j.Logger;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.simple.SimpleJdbcTemplate;

import ro.cst.tsearch.SearchFlags.CREATION_SOURCE_TYPES;
import ro.cst.tsearch.utils.DBConstants;

public class DBSearch {
	
	private static final Logger logger = Logger.getLogger(DBSearch.class);
	
	private static final String SQL_GET_CREATION_SOURCE_TYPE = "select " 
		+ DBConstants.FIELD_SEARCH_FLAGS_SOURCE_CREATION_TYPE + " from "
		+ DBConstants.TABLE_SEARCH_FLAGS + " where " 
		+ DBConstants.FIELD_SEARCH_FLAGS_ID + " = ?";
	public static CREATION_SOURCE_TYPES getCreationSourceType(long searchId) {
		try {
			return getCreationSourceTypeFromDatabaseStatus(DBManager.getSimpleTemplate().queryForInt(SQL_GET_CREATION_SOURCE_TYPE, searchId));
		} catch (Exception e) {
			logger.error("Error while checking creationSourceType for searchId: " + searchId, e);
		}
		return null;
	}
	
	public static CREATION_SOURCE_TYPES getCreationSourceTypeFromDatabaseStatus(int databaseStatus) {
		if(databaseStatus == 1) {
			return CREATION_SOURCE_TYPES.REOPENED;
		} else if(databaseStatus == 2) {
			return CREATION_SOURCE_TYPES.CLONED;
		} else {
			return CREATION_SOURCE_TYPES.NORMAL;
		}
		
	}
	
	/**
	 * Delete searches with the given <b>status</b> that has the <b>tsrCreated</b> flag and that are older that <b>days</b>
	 * @param status
	 * @param tsrCreated
	 * @param days
	 */
	public static void deleteSearchByStatus(long status, int tsrCreated, int days) {
        SimpleJdbcTemplate sjt = DBManager.getSimpleTemplate();
        String stm = null;
        try {
            stm = "delete from "+ TABLE_SEARCH +" where " + 
            	DBConstants.FIELD_SEARCH_STATUS + " = ? and " + 
            	DBConstants.FIELD_SEARCH_ID + " in " + 
            	"(select " + DBConstants.FIELD_SEARCH_FLAGS_ID + " from " + 
            	DBConstants.TABLE_SEARCH_FLAGS + " where " +  
            	DBConstants.FIELD_SEARCH_FLAGS_TSR_CREATED + " = ? and date_add(sdate, interval ? day) < (now()))";
            int deletedSearches = sjt.update(stm, status, tsrCreated, days);
            logger.debug("Deleted " + deletedSearches + " searches");
        } catch (Exception e) {
            logger.error("DBManager deleteSearch# SQL: " + stm, e);
        }
    }
	
	private static final String SQL_GET_TSRI_LINK = "select " 
		+ DBConstants.FIELD_SEARCH_FLAGS_TSR_CREATED + ", " 
		+ DBConstants.FIELD_SEARCH_TSRI_LINK + " from "
		+ DBConstants.TABLE_SEARCH + " s join "
		+ DBConstants.TABLE_SEARCH_FLAGS + " sf on s."  
		+ DBConstants.FIELD_SEARCH_ID + " = sf."
		+ DBConstants.FIELD_SEARCH_FLAGS_ID + " where s." 
		+ DBConstants.FIELD_SEARCH_ID + " = ?";
	public static Map<String, Object> getTsriLinkInfoFromDB(long searchId) {
		try {
			return DBManager.getSimpleTemplate().queryForMap(SQL_GET_TSRI_LINK, searchId);
		} catch (Exception e) {
			logger.error("Error while checking getTsriLinkInfoFromDB for searchId: " + searchId, e);
		}
		return null;
	}
	
	public static String getColorCodeFromDatabaseStatus(int databaseStatus) {
		if(databaseStatus == 1) {
			return "#ffffcc";
		}
		return "";
	}

	private static final String SQL_GET_ABSTR_ID_AND_FILENO = "SELECT " + 
			DBConstants.FIELD_SEARCH_ABSTRACT_ID + ", " +
    		DBConstants.FIELD_SEARCH_ABSTRACT_FILENO + " FROM " + 
    		DBConstants.TABLE_SEARCH + " WHERE " + 
    		DBConstants.FIELD_SEARCH_ID + " = ?";
	public static Map<String, Object> getSearchAbstractorIdAndFileId(long searchId) {
		try {
			return DBManager.getSimpleTemplate().queryForMap(SQL_GET_ABSTR_ID_AND_FILENO, searchId);
		} catch (DataAccessException e) {
			logger.error("Error while running sql " + SQL_GET_ABSTR_ID_AND_FILENO + " with param " + searchId, e);
		}
		return null;
	}

}
