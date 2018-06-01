package ro.cst.tsearch.database.rowmapper;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

import org.apache.log4j.Logger;
import org.springframework.dao.CannotAcquireLockException;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.simple.ParameterizedRowMapper;
import org.springframework.jdbc.core.simple.SimpleJdbcTemplate;

import ro.cst.tsearch.database.DBManager;

public class ImageCount implements ParameterizedRowMapper<ImageCount> {
	
	private static final Logger logger = Logger.getLogger(ImageCount.class);
	
	public static final String TABLE_IMAGE_COUNT = "ts_image_count";
	public static final String FIELD_SEARCH_ID = "searchId";
	public static final String FIELD_DATA_SOURCE = "datasource";
	public static final String FIELD_COUNT = "count";
	public static final String FIELD_DESCRIPTION = "description";
	
	
	public static int DATASOURCE_TOTAL = -2;
	private int INVALID_VALUE = -1;
	
	/**
	 * Maps the column {@link ImageCount}.FIELD_SEARCH_ID
	 */
	private long searchId = INVALID_VALUE;
	/**
	 * Maps the column {@link ImageCount}.FIELD_DATA_SOURCE
	 */
	private int siteType = INVALID_VALUE;
	/**
	 * Maps the column {@link ImageCount}.FIELD_COUNT
	 */
	private int count = INVALID_VALUE;
	/**
	 * Maps the column {@link ImageCount}.FIELD_DESCRIPTION
	 */
	private String description = null;
	
	public static final String INSERT_IMAGE_COUNT = "INSERT INTO " + 
			TABLE_IMAGE_COUNT + "(" + 
			FIELD_SEARCH_ID + " , " + 
			FIELD_DATA_SOURCE + "," + 
			FIELD_COUNT + ") VALUES (?,?,?)";
	
	public static final String INSERT_UPDATE_IMAGE_COUNT = "INSERT INTO " + 
			TABLE_IMAGE_COUNT + "(" + 
			FIELD_SEARCH_ID + " , " + 
			FIELD_DATA_SOURCE + "," +
			FIELD_COUNT + "," + 
			FIELD_DESCRIPTION + ") VALUES (?,?,?,?) on duplicate key update " +
			FIELD_COUNT + " = ?, " + 
			FIELD_DESCRIPTION + " = ? ";
	
	
	private static final String SQL_SELECT_BY_SEARCHID = 
			"select * from " + 
			TABLE_IMAGE_COUNT + " where " + 
			FIELD_SEARCH_ID + " = ? ";
	
	
	public long getSearchId() {
		return searchId;
	}
	public void setSearchId(long searchId) {
		this.searchId = searchId;
	}
	public int getSiteType() {
		return siteType;
	}
	public int getDataSource() {
		return getSiteType();
	}
	public void setSiteType(int siteType) {
		this.siteType = siteType;
	}
	public void setDataSource(int dataSource) {
		setSiteType(dataSource);
	}
	public int getCount() {
		return count;
	}
	public void setCount(int count) {
		this.count = count;
	}
	public String getDescription() {
		return description;
	}
	public void setDescription(String description) {
		this.description = description;
	}
	@Override
	public ImageCount mapRow(ResultSet rs, int rowNum) throws SQLException {
		ImageCount imageCount = new ImageCount();
		try {
			imageCount.setSearchId(rs.getLong(FIELD_SEARCH_ID));
		} catch (Exception e) {
			e.printStackTrace();
		}
		imageCount.setCount(rs.getInt(FIELD_COUNT));
		imageCount.setSiteType(rs.getInt(FIELD_DATA_SOURCE));
		imageCount.setDescription(rs.getString(FIELD_DESCRIPTION));
		return imageCount;
	}
	
	public boolean saveToDatabase() {
		return saveToDatabase(5);
	}
	
	public boolean saveToDatabase(int retry) {
		if(getSearchId() == INVALID_VALUE) {
			throw new NullPointerException("SearchId is invalid. Please update to a valid value and redo transaction");
		}
		if(getSiteType() == INVALID_VALUE) {
			throw new NullPointerException("SiteType/DataSource is invalid. Please update to a valid value and redo transaction");
		}
		if(getCount() == INVALID_VALUE) {
			throw new NullPointerException("Count is invalid. Please update to a valid value and redo transaction");
		}
		if(retry <= 0) {
			retry = 1;
		}
		SimpleJdbcTemplate sjt = DBManager.getSimpleTemplate();
		for (int i = 0; i < retry; i++) {
			try {
				sjt.update(INSERT_UPDATE_IMAGE_COUNT, getSearchId(), getDataSource(), getCount(), getDescription(), getCount(), getDescription());
				return true;
			} catch (CannotAcquireLockException cale) {
				logger.error("Could not Acquire lock, number of retry " + i, cale);
			} catch (DataAccessException e) {
				logger.error("Error while saving entry in database", e);
			}
		}
		return false;
	}
	
	public static List<ImageCount> getImageCountForSearch(long searchId) {
		SimpleJdbcTemplate sjt = DBManager.getSimpleTemplate();
		try {
			return sjt.query(SQL_SELECT_BY_SEARCHID, new ImageCount(), searchId);
		} catch (DataAccessException e) {
			logger.error("Error while getting imageCount for searchid " + searchId, e); 
		}
		return null;
		
	}

}
