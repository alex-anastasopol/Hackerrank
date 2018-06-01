package ro.cst.tsearch.database.rowmapper;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;
import org.springframework.dao.CannotAcquireLockException;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.simple.ParameterizedRowMapper;
import org.springframework.jdbc.core.simple.SimpleJdbcTemplate;

import ro.cst.tsearch.database.DBManager;
import ro.cst.tsearch.servers.HashCountyToIndex;
import ro.cst.tsearch.utils.StringUtils;

/**
 * 
 * @author Oprina George
 * 
 *         Apr 13, 2012
 */

public class OrderCount implements ParameterizedRowMapper<OrderCount> {
	/**
	 * Count orders by data source Task 7825
	 * 
	 */

	private static final Logger	logger								= Logger.getLogger(OrderCount.class);

	public static int			DATASOURCE_TOTAL					= -2;
	private static int			INVALID_VALUE						= -1;

	private long				id;
	private int					communityId							= INVALID_VALUE;
	private String				fileId								= "";
	private int					dataSource							= INVALID_VALUE;
	private Date				dateCounted							= null;
	private int					countyId							= INVALID_VALUE;
	private int					productId							= INVALID_VALUE;
	private long				searchId							= INVALID_VALUE;

	public static final String	TABLE_ORDER_COUNT					= "ts_order_count";
	public static final String	FIELD_ORDER_ID						= "id";
	public static final String	FIELD_ORDER_COMMUNITY_ID			= "community_id";
	public static final String	FIELD_ORDER_FILE_ID					= "file_id";
	public static final String	FIELD_ORDER_DATA_SOURCE				= "datasource";
	public static final String	FIELD_ORDER_DATE					= "date_counted";
	public static final String	FIELD_ORDER_COUNTY_ID				= "county_id";
	public static final String	FIELD_ORDER_PRODUCT_ID				= "product_id";
	public static final String	FIELD_ORDER_INITIAL_SEARCH_ID		= "initial_search_id";

	public static final String	INSERT_ORDER_COUNT					= "INSERT INTO " +
																			TABLE_ORDER_COUNT + "(" +
																			FIELD_ORDER_COMMUNITY_ID + " , " +
																			FIELD_ORDER_FILE_ID + "," +
																			FIELD_ORDER_DATA_SOURCE + "," +
																			FIELD_ORDER_DATE + "," +
																			FIELD_ORDER_COUNTY_ID + "," +
																			FIELD_ORDER_PRODUCT_ID + "," +
																			FIELD_ORDER_INITIAL_SEARCH_ID +
																			") VALUES (?,?,?,?,?,?,?)";

	public static final String	SQL_SELECT_COUNT_BY_ORDER_FILE_ID	= "select count(*) from " +
																			TABLE_ORDER_COUNT + " where " +
																			FIELD_ORDER_COMMUNITY_ID + "= ? AND " +
																			FIELD_ORDER_FILE_ID + " = ? AND " +
																			FIELD_ORDER_DATA_SOURCE + " = ?";

	public static final String	SQL_GET_ORDER_COUNT_BETWEEN_DATES	= "select " +
																			FIELD_ORDER_COMMUNITY_ID + ", " +
																			"sum(if(" + FIELD_ORDER_PRODUCT_ID + "!=10,1,0)) countNonupdates, " +
																			"sum(if(" + FIELD_ORDER_PRODUCT_ID + "=10,1,0)) countUpdates, " +
																			FIELD_ORDER_DATA_SOURCE +
																			" from " + TABLE_ORDER_COUNT +
																			" where " + FIELD_ORDER_DATE +
																			" between ? and ? group by " + FIELD_ORDER_COMMUNITY_ID + "," +
																			FIELD_ORDER_DATA_SOURCE;

	public long getId() {
		return id;
	}

	public void setId(long id) {
		this.id = id;
	}

	public int getCommunityId() {
		return communityId;
	}

	public void setCommunityId(int communityId) {
		this.communityId = communityId;
	}

	public String getFileId() {
		return fileId;
	}

	public void setFileId(String fileId) {
		this.fileId = fileId;
	}

	public Date getDateCounted() {
		return dateCounted;
	}

	public void setDateCounted(Date dateCounted) {
		this.dateCounted = dateCounted;
	}

	public int getCountyId() {
		return countyId;
	}

	public void setCountyId(int countyId) {
		this.countyId = countyId;
	}

	public int getDataSource() {
		return dataSource;
	}

	public void setDataSource(int dataSource) {
		this.dataSource = dataSource;
	}

	public int getProductId() {
		return productId;
	}

	public void setProductId(int productId) {
		this.productId = productId;
	}

	public long getSearchId() {
		return searchId;
	}

	public void setSearchId(long searchId) {
		this.searchId = searchId;
	}

	public boolean saveToDatabase() {
		return saveToDatabase(5);
	}

	public boolean saveToDatabase(int retry) {
		if (StringUtils.isEmpty(getFileId())) {
			throw new NullPointerException("File is invalid. Please update to a valid value and redo transaction");
		}
		if (getCommunityId() == INVALID_VALUE) {
			throw new NullPointerException("Community ID is invalid. Please update to a valid value and redo transaction");
		}
		if (getDataSource() == INVALID_VALUE) {
			throw new NullPointerException("DataSource is invalid. Please update to a valid value and redo transaction");
		}

		if (getCountyId() == INVALID_VALUE) {
			throw new NullPointerException("County ID is invalid. Please update to a valid value and redo transaction");
		}

		if (getDateCounted() == null) {
			throw new NullPointerException("Date Counted is invalid. Please update to a valid value and redo transaction");
		}

		if (retry <= 0) {
			retry = 1;
		}
		SimpleJdbcTemplate sjt = DBManager.getSimpleTemplate();
		for (int i = 0; i < retry; i++) {
			try {
				sjt.update(INSERT_ORDER_COUNT,
						getCommunityId(),
						getFileId(),
						getDataSource(),
						getDateCounted(),
						getCountyId(),
						getProductId(),
						getSearchId());
				return true;
			} catch (CannotAcquireLockException cale) {
				logger.error("Could not Acquire lock, number of retry " + i, cale);
			} catch (DataAccessException e) {
				logger.error("Error while saving entry in database", e);
			}
		}
		return false;
	}

	public static boolean isOrderAlreadyCounted(int commId, String fileID, int datasource) {
		if (StringUtils.isEmpty(fileID))
			return false;

		SimpleJdbcTemplate sjt = DBManager.getSimpleTemplate();
		try {
			int count = sjt.queryForInt(SQL_SELECT_COUNT_BY_ORDER_FILE_ID, commId, fileID, datasource);
			if (count > 0) {
				return true;
			}
		} catch (DataAccessException e) {
			logger.error("Error while getting orderCount for searchid ", e);
		}
		return false;
	}

	static class OrderCountReportRow implements ParameterizedRowMapper<OrderCountReportRow> {
		private int	communityId		= INVALID_VALUE;
		private int	countNonupdates	= INVALID_VALUE;
		private int	countUpdates	= INVALID_VALUE;
		private int	dataSource		= INVALID_VALUE;

		@Override
		public OrderCountReportRow mapRow(ResultSet row, int rowNum) throws SQLException {
			try {
				OrderCountReportRow ocrr = new OrderCountReportRow();
				ocrr.setCommunityId(row.getInt(FIELD_ORDER_COMMUNITY_ID));
				ocrr.setCountNonupdates(row.getInt("countNonupdates"));
				ocrr.setCountUpdates(row.getInt("countUpdates"));
				ocrr.setDataSource(row.getInt(FIELD_ORDER_DATA_SOURCE));
				return ocrr;
			} catch (Exception e) {
				e.printStackTrace();
			}
			return null;
		}

		public int getCommunityId() {
			return communityId;
		}

		public void setCommunityId(int communityId) {
			this.communityId = communityId;
		}

		public int getCountNonupdates() {
			return countNonupdates;
		}

		public void setCountNonupdates(int countNonupdates) {
			this.countNonupdates = countNonupdates;
		}
		
		public int getCountUpdates() {
			return countUpdates;
		}

		public void setCountUpdates(int countUpdates) {
			this.countUpdates = countUpdates;
		}

		public int getDataSource() {
			return dataSource;
		}

		public void setDataSource(int dataSource) {
			this.dataSource = dataSource;
		}
	}

	public static Map<Integer, Map<String, List<Integer>>> getOrderCountBetweenDates(Date startDate, Date endDate) {

		//  comm ID,       DS ,<nonupdates, updates, all>
		Map<Integer, Map<String, List<Integer>>> result = new HashMap<Integer, Map<String, List<Integer>>>();

		if (startDate == null || endDate == null)
			return null;

		try {
			// SQL between dates works this way if we have the same start and end date
			Calendar startC = Calendar.getInstance();
			startC.setTime(startDate);
			startC.set(Calendar.HOUR_OF_DAY, 0);
			startC.set(Calendar.MINUTE, 0);
			startC.set(Calendar.SECOND, 0);

			Calendar endC = Calendar.getInstance();
			endC.setTime(endDate);
			endC.set(Calendar.HOUR_OF_DAY, 23);
			endC.set(Calendar.MINUTE, 59);
			endC.set(Calendar.SECOND, 59);

			SimpleJdbcTemplate sjt = DBManager.getSimpleTemplate();
			List<OrderCountReportRow> ocrr = sjt.query(SQL_GET_ORDER_COUNT_BETWEEN_DATES, new OrderCountReportRow(), startC.getTime(), endC.getTime());

			if (ocrr != null && ocrr.size() > 0) {
				for (OrderCountReportRow ocr : ocrr) {
					String datasourceString = HashCountyToIndex.getServerAbbreviationByType(ocr.getDataSource());
					Integer countNonupdates = ocr.getCountNonupdates();
					Integer countUpdates = ocr.getCountUpdates();

					Map<String, List<Integer>> comm = result.get(ocr.getCommunityId());

					if (StringUtils.isNotEmpty(datasourceString)) {
						if (comm == null) {
							Map<String, List<Integer>> dsCount = new HashMap<String, List<Integer>>();
							List<Integer> list = new ArrayList<Integer>();
							list.add(countNonupdates);
							list.add(countUpdates);
							list.add(countNonupdates+countUpdates);
							dsCount.put(datasourceString, list);
							result.put(ocr.getCommunityId(), dsCount);
						} else {
							List<Integer> list = new ArrayList<Integer>();
							list.add(countNonupdates);
							list.add(countUpdates);
							list.add(countNonupdates+countUpdates);
							comm.put(datasourceString, list);
						}
					}
				}

				return result;
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		return null;
	}

	public OrderCount mapRow(ResultSet rs, int rowNum) throws SQLException {
		OrderCount ordercoCount = new OrderCount();
		try {
			ordercoCount.setId(rs.getLong(FIELD_ORDER_ID));
			ordercoCount.setCommunityId(rs.getInt(FIELD_ORDER_COMMUNITY_ID));
			ordercoCount.setFileId(rs.getString(FIELD_ORDER_FILE_ID));
			ordercoCount.setDataSource(rs.getInt(FIELD_ORDER_DATA_SOURCE));
			ordercoCount.setDateCounted(rs.getDate(FIELD_ORDER_DATE));
			ordercoCount.setCountyId(rs.getInt(FIELD_ORDER_COUNTY_ID));

		} catch (Exception e) {
			e.printStackTrace();
		}
		return ordercoCount;
	}
}
