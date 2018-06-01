package ro.cst.tsearch.database.rowmapper;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.util.List;
import java.util.Vector;

import org.springframework.jdbc.core.PreparedStatementCreatorFactory;
import org.springframework.jdbc.core.SqlParameter;
import org.springframework.jdbc.core.simple.ParameterizedRowMapper;
import org.springframework.jdbc.core.simple.SimpleJdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;

import ro.cst.tsearch.database.ConnectionPool;
import ro.cst.tsearch.database.DBManager;
import ro.cst.tsearch.user.UserManager;

public class UserFilterMapper implements ParameterizedRowMapper<UserFilterMapper> {

	public static final String TABLE_USER_FILTERS					=	"ts_user_filters";
	
	public static final String FIELD_USER_FILTERS_ID 		= "user_filters_id";
	public static final String FIELD_USER_ID 				= "user_id";
	public static final String FIELD_FILTER_VALUE_LONG		= "filterValue";
	public static final String FIELD_FILTER_VALUE_STRING	= "companyName";
	public static final String FIELD_TYPE 					= "type";
	
	private Long id				= null;
	private Long userId			= null;
	private Long valueLong		= null;
	private String valueString  = null;
	private int type			= 0;
	
	/**
	 * @return the id
	 */
	public Long getId() {
		return id;
	}

	/**
	 * @param id the id to set
	 */
	public void setId(Long id) {
		this.id = id;
	}

	/**
	 * @return the userId
	 */
	public Long getUserId() {
		return userId;
	}

	/**
	 * @param userId the userId to set
	 */
	public void setUserId(Long userId) {
		this.userId = userId;
	}

	/**
	 * @return the valueLong
	 */
	public Long getValueLong() {
		return valueLong;
	}

	/**
	 * @param valueLong the valueLong to set
	 */
	public void setValueLong(Long valueLong) {
		this.valueLong = valueLong;
	}

	/**
	 * @return the valueString
	 */
	public String getValueString() {
		return valueString;
	}

	/**
	 * @param valueString the valueString to set
	 */
	public void setValueString(String valueString) {
		this.valueString = valueString;
	}

	/**
	 * @return the type
	 */
	public int getType() {
		return type;
	}

	/**
	 * @param type the type to set
	 */
	public void setType(int type) {
		this.type = type;
	}

	@Override
	public UserFilterMapper mapRow(ResultSet resultSet, int rowNum) throws SQLException {
		UserFilterMapper ufm = new UserFilterMapper();
		ufm.setId(resultSet.getLong(FIELD_USER_FILTERS_ID));
		try {
			ufm.setUserId(resultSet.getLong(FIELD_USER_ID));
		} catch (Exception e) {
			ufm.setUserId(null);
		}
		ufm.setType(resultSet.getInt(FIELD_TYPE));
		try {
			ufm.setValueLong(resultSet.getLong(FIELD_FILTER_VALUE_LONG));
		} catch (Exception e) {
			ufm.setValueLong(null);
		}
		try {
			ufm.setValueString(resultSet.getString(FIELD_FILTER_VALUE_STRING));
		} catch (Exception e) {
			ufm.setValueString(null);
		}
		return ufm;
	}
	
	
	private static final String SQL_INSERT_NEW_FILTER = "INSERT INTO " + 
		TABLE_USER_FILTERS + " ( " +
		FIELD_USER_ID + ", " + 
		FIELD_FILTER_VALUE_LONG + ", " +
		FIELD_FILTER_VALUE_STRING + ", " + 
		FIELD_TYPE + ") VALUES (?,?,?,?)";
	private static final String SQL_INSERT_NEW_FILTER_LONG = "INSERT INTO " + 
		TABLE_USER_FILTERS + " ( " +
		FIELD_USER_ID + ", " + 
		FIELD_FILTER_VALUE_LONG + ", " +
		FIELD_TYPE + ") VALUES (?,?,?)";
	private static final String SQL_INSERT_NEW_FILTER_STRING = "INSERT INTO " + 
		TABLE_USER_FILTERS + " ( " +
		FIELD_USER_ID + ", " + 
		FIELD_FILTER_VALUE_STRING + ", " + 
		FIELD_TYPE + ") VALUES (?,?,?)";
	
	/**
	 * Stores this object in the database.
	 * It must have a valid userId, type at at least one value (long or string)
	 * If everything goes well the id is set to the one generated
	 * @return true if the save was done
	 */
	public boolean save() {
		try {
			
			
			
			if(type == 0 || getUserId() == null) {
				return false;
			}
			SimpleJdbcTemplate sjt = ConnectionPool.getInstance().getSimpleTemplate();
			
			if(getId() == null) {
				PreparedStatementCreatorFactory pstat = null;
				Object[] params = null;
				if(getValueLong() == null) {
					if(getValueString() == null) {
						return false;
					} else {
						pstat = new PreparedStatementCreatorFactory(SQL_INSERT_NEW_FILTER_STRING);
						pstat.addParameter(new SqlParameter(FIELD_USER_ID,Types.BIGINT));
						pstat.addParameter(new SqlParameter(FIELD_FILTER_VALUE_STRING,Types.VARCHAR));
						params = new Object[]{ getUserId(), getValueString(), getType() };
					}
				} else {
					if(getValueString() == null) {
						pstat = new PreparedStatementCreatorFactory(SQL_INSERT_NEW_FILTER_LONG);
						pstat.addParameter(new SqlParameter(FIELD_USER_ID,Types.BIGINT));
						pstat.addParameter(new SqlParameter(FIELD_FILTER_VALUE_LONG,Types.BIGINT));
						params = new Object[]{ getUserId(), getValueLong(), getType() };
					} else {
						pstat = new PreparedStatementCreatorFactory(SQL_INSERT_NEW_FILTER);
						pstat.addParameter(new SqlParameter(FIELD_USER_ID,Types.BIGINT));
						pstat.addParameter(new SqlParameter(FIELD_FILTER_VALUE_LONG,Types.BIGINT));
						pstat.addParameter(new SqlParameter(FIELD_FILTER_VALUE_STRING,Types.VARCHAR));
						params = new Object[]{ getUserId(), getValueLong(), getValueString(), getType() };
					}
				}
				pstat.setReturnGeneratedKeys(true);
				pstat.addParameter(new SqlParameter(FIELD_TYPE,Types.INTEGER));
				
				KeyHolder generatedKeyHolder = new GeneratedKeyHolder();
				
				if(sjt.getJdbcOperations().update(pstat.newPreparedStatementCreator(params),generatedKeyHolder)!=1){
					//if the insert did not affect just one row we have an error
					return false;
				}
				setId(generatedKeyHolder.getKey().longValue());
			
			} else {
				//TODO: must implement the update
				return false;
			}
			
		} catch (Exception e) {
			return false;
		}
		return true;
	}

	/**
	 * 
	 * @return the string representation
	 *  
	 */
	public String toString() {
		StringBuffer buffer = new StringBuffer();
		buffer.append("UserFilterMapper[");
		buffer.append("id = ").append(id);
		buffer.append(" type = ").append(type);
		buffer.append(" userId = ").append(userId);
		buffer.append(" valueLong = ").append(valueLong);
		buffer.append(" valueString = ").append(valueString);
		buffer.append("]");
		return buffer.toString();
	}

	private static final String SQL_GET_ATTRIBUTES_LONG = " SELECT GROUP_CONCAT( CONVERT(" + 
		FIELD_FILTER_VALUE_LONG + ",CHAR) SEPARATOR ',') FROM " + 
		TABLE_USER_FILTERS + " WHERE " + 
		FIELD_USER_ID + " = ? AND " + 
		FIELD_TYPE + " = ? GROUP BY " + 
		FIELD_USER_ID + ", " + FIELD_TYPE;
	private static final String SQL_GET_ATTRIBUTES_STRING = " SELECT GROUP_CONCAT( CONVERT(" + 
		FIELD_FILTER_VALUE_STRING + ",CHAR) SEPARATOR ',') FROM " + 
		TABLE_USER_FILTERS + " WHERE " + 
		FIELD_USER_ID + " = ? AND " + 
		FIELD_TYPE + " = ? GROUP BY " + 
		FIELD_USER_ID + ", " + FIELD_TYPE;
	public static String getAttributesAsString(long userId, int type) {
		try {
			if (type != UserManager.dashboardFilterTypes.get("CompanyAgent") && type != UserManager.dashboardFilterTypes.get("CategAndSubcateg")) {
				return DBManager.getSimpleTemplate().queryForObject(SQL_GET_ATTRIBUTES_LONG, String.class, userId, type);
			} else {
				return DBManager.getSimpleTemplate().queryForObject(SQL_GET_ATTRIBUTES_STRING, String.class, userId, type);
			}
		} catch (Exception e) {
			return "-1";
		}
		
	}
	
	private static final String SQL_DELETE_ATTRIBUTES = " DELETE FROM " + 
		TABLE_USER_FILTERS + " WHERE " + 
		FIELD_USER_ID + " = ? AND " + FIELD_TYPE + " = ?";
	public static int clearAttributes(long userId, int type) {
		try {
			return DBManager.getSimpleTemplate().update(SQL_DELETE_ATTRIBUTES, userId, type);
		} catch (Exception e) {
			return -1;
		}
		
	}
	
	public static void clearAttributes(List<Long> userIds, int type) {
		if(userIds != null) {
			for (Long userId : userIds) {
				clearAttributes(userId, type);
			}
		}
	}
	
	private static final String SQL_COPY_FILTER_BY_TYPE = "INSERT INTO " + TABLE_USER_FILTERS + " ( " + 
		FIELD_FILTER_VALUE_LONG + ", " + FIELD_FILTER_VALUE_STRING + ", " + FIELD_TYPE + ", " + FIELD_USER_ID + " ) SELECT " + 
		FIELD_FILTER_VALUE_LONG + ", " + FIELD_FILTER_VALUE_STRING + ", " + FIELD_TYPE + ", @@type@@ " +
				"FROM " + TABLE_USER_FILTERS + " WHERE " + FIELD_USER_ID + " = ? and " + FIELD_TYPE + " = ?";
	public static void copyAllowedFilter(long srcUserId, long destUserId, int filterType) {
		try {
			DBManager.getSimpleTemplate().update(SQL_COPY_FILTER_BY_TYPE.replace("@@type@@", destUserId + ""), srcUserId, filterType);
		} catch (Exception e) {
			e.printStackTrace();
		}
		
	}

}
