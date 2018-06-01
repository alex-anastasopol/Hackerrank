package ro.cst.tsearch.database.rowmapper;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.List;
import java.util.Map.Entry;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.springframework.dao.CannotAcquireLockException;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.simple.ParameterizedRowMapper;
import org.springframework.jdbc.core.simple.SimpleJdbcTemplate;

import ro.cst.tsearch.database.DBManager;
import ro.cst.tsearch.servers.HashCountyToIndex;

/**
 * 
 * @author Oprina George
 * 
 */

public class RequestCount implements ParameterizedRowMapper<RequestCount> {
	/**
	 * Count requests Task 7846
	 * 
	 */

	private static final Logger	logger								= Logger.getLogger(OrderCount.class);

	private static int			INVALID_VALUE						= -1;

	private long				searchId							= INVALID_VALUE;
	private int					commId								= INVALID_VALUE;
	private int					countyId							= INVALID_VALUE;
	private int					dataSource							= INVALID_VALUE;
	private int					total								= 0;
	private int					nonInstrumentTotal					= 0;
	private int					instrumentCount						= 0;
	private int					nameCount							= 0;
	private int					addrCount							= 0;
	private int					legalCount							= 0;
	private int					pinCount							= 0;
	private int					miscCount							= 0;
	private int					imageCount							= 0;
	private String				description							= "";

	public static final int		TYPE_INSTRUMENT_COUNT				= 0;
	public static final int		TYPE_NAME_COUNT						= 1;
	public static final int		TYPE_ADDR_COUNT						= 2;
	public static final int		TYPE_LEGAL_COUNT					= 3;
	public static final int		TYPE_PIN_COUNT						= 4;
	public static final int		TYPE_MISC_COUNT						= 5;
	public static final int		TYPE_IMAGE_COUNT					= 6;

	public static final String	REQUEST_COUNT_TYPE					= "requestCountType";

	public static final String	TABLE_REQUEST_COUNT					= "ts_request_count";

	public static final String	FIELD_SEARCH_ID						= "searchId";
	public static final String	FIELD_DATA_SOURCE					= "dataSource";
	public static final String	FIELD_TOTAL							= "total";
	public static final String	FIELD_NON_INSTRUMENT_TOTAL			= "nonInstrumentTotal";
	public static final String	FIELD_INSTRUMENT_COUNT				= "instrumentCount";
	public static final String	FIELD_NAME_COUNT					= "nameCount";
	public static final String	FIELD_ADDR_COUNT					= "addrCount";
	public static final String	FIELD_LEGAL_COUNT					= "legalCount";
	public static final String	FIELD_PIN_COUNT						= "pinCount";
	public static final String	FIELD_MISC_COUNT					= "miscCount";
	public static final String	FIELD_IMAGE_COUNT					= "imageCount";
	public static final String	FIELD_COUNTY_ID						= "countyId";
	public static final String	FIELD_COMMUNITY_ID					= "commId";
	public static final String	FIELD_DESCRIPTION					= "description";
	
	public static int			DATASOURCE_TOTAL					= -2;

	public static final String	INSERT_REQUEST_COUNT				= "INSERT INTO " +
																			TABLE_REQUEST_COUNT + "(" +
																			FIELD_SEARCH_ID + "," +
																			FIELD_COUNTY_ID + "," +
																			FIELD_COMMUNITY_ID + "," +
																			FIELD_DATA_SOURCE + "," +
																			FIELD_TOTAL + "," +
																			FIELD_NON_INSTRUMENT_TOTAL + "," +
																			FIELD_INSTRUMENT_COUNT + "," +
																			FIELD_NAME_COUNT + "," +
																			FIELD_ADDR_COUNT + "," +
																			FIELD_LEGAL_COUNT + "," +
																			FIELD_PIN_COUNT + "," +
																			FIELD_MISC_COUNT + "," +
																			FIELD_IMAGE_COUNT +
																			FIELD_DESCRIPTION +
																			") VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?)";

	public static final String	INSERT_UPDATE_REQUEST_COUNT			= "INSERT INTO " +
																			TABLE_REQUEST_COUNT + " (" +
																			FIELD_SEARCH_ID + "," +
																			FIELD_COUNTY_ID + "," +
																			FIELD_COMMUNITY_ID + "," +
																			FIELD_DATA_SOURCE + "," +
																			FIELD_TOTAL + "," +
																			FIELD_NON_INSTRUMENT_TOTAL + "," +
																			FIELD_INSTRUMENT_COUNT + "," +
																			FIELD_NAME_COUNT + "," +
																			FIELD_ADDR_COUNT + "," +
																			FIELD_LEGAL_COUNT + "," +
																			FIELD_PIN_COUNT + "," +
																			FIELD_MISC_COUNT + "," +
																			FIELD_IMAGE_COUNT + "," +
																			FIELD_DESCRIPTION +
																			") VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?) on duplicate key update " +
																			FIELD_TOTAL + " =  ? , " +
																			FIELD_NON_INSTRUMENT_TOTAL + " =  ? , " +
																			FIELD_INSTRUMENT_COUNT + " = ? , " +
																			FIELD_NAME_COUNT + " = ? , " +
																			FIELD_ADDR_COUNT + " = ? , " +
																			FIELD_LEGAL_COUNT + " = ? , " +
																			FIELD_PIN_COUNT + " = ? , " +
																			FIELD_MISC_COUNT + " = ? , " +
																			FIELD_IMAGE_COUNT + " = ? , " +
																			FIELD_DESCRIPTION + " = ? ";

	public static final String	INSERT_ADD_REQUEST_COUNT			= "INSERT INTO " +
																			TABLE_REQUEST_COUNT + "(" +
																			FIELD_SEARCH_ID + "," +
																			FIELD_COUNTY_ID + "," +
																			FIELD_COMMUNITY_ID + "," +
																			FIELD_DATA_SOURCE + "," +
																			FIELD_TOTAL + "," +
																			FIELD_NON_INSTRUMENT_TOTAL + "," +
																			FIELD_INSTRUMENT_COUNT + "," +
																			FIELD_NAME_COUNT + "," +
																			FIELD_ADDR_COUNT + "," +
																			FIELD_LEGAL_COUNT + "," +
																			FIELD_PIN_COUNT + "," +
																			FIELD_MISC_COUNT + "," +
																			FIELD_IMAGE_COUNT +
																			FIELD_DESCRIPTION +
																			") VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?) on duplicate key update " +
																			FIELD_TOTAL + " = " + FIELD_TOTAL + " + ? , " +
																			FIELD_NON_INSTRUMENT_TOTAL + " = " + FIELD_NON_INSTRUMENT_TOTAL + " + ? , " +
																			FIELD_INSTRUMENT_COUNT + " = " + FIELD_INSTRUMENT_COUNT + " + ? , " +
																			FIELD_NAME_COUNT + " = " + FIELD_NAME_COUNT + " + ? , " +
																			FIELD_ADDR_COUNT + " = " + FIELD_ADDR_COUNT + " + ? , " +
																			FIELD_LEGAL_COUNT + " = " + FIELD_LEGAL_COUNT + " + ? , " +
																			FIELD_PIN_COUNT + " = " + FIELD_PIN_COUNT + " + ? , " +
																			FIELD_MISC_COUNT + " = " + FIELD_MISC_COUNT + " + ? , " +
																			FIELD_IMAGE_COUNT + " = " + FIELD_IMAGE_COUNT + " + ?";

	public static final String	UPDATE_SEARCH_ID					= "update " + TABLE_REQUEST_COUNT +
																			" SET " + FIELD_SEARCH_ID + " = " + " ? " +
																			"WHERE " + FIELD_SEARCH_ID + " = ? ";

	public static final String	SQL_SELECT_BY_SEARCHID				= "select * from " +
																			TABLE_REQUEST_COUNT + " where " +
																			FIELD_SEARCH_ID + " = ? ";

	public static final String	SQL_SELECT_BY_SEARCHID_DATASOURCE	= "select * from " +
																			TABLE_REQUEST_COUNT + " where " +
																			FIELD_SEARCH_ID + " = ? AND " +
																			FIELD_DATA_SOURCE + " = ?";

	@Override
	public RequestCount mapRow(ResultSet rs, int rowNum) throws SQLException {
		RequestCount reqCount = new RequestCount();
		try {
			reqCount.setSearchId(rs.getLong(FIELD_SEARCH_ID));
			reqCount.setDataSource(rs.getInt(FIELD_DATA_SOURCE));
			reqCount.setTotal(rs.getInt(FIELD_TOTAL));
			reqCount.setNonInstrumentTotal(rs.getInt(FIELD_NON_INSTRUMENT_TOTAL));
			reqCount.setInstrumentCount(rs.getInt(FIELD_INSTRUMENT_COUNT));
			reqCount.setNameCount(rs.getInt(FIELD_NAME_COUNT));
			reqCount.setAddrCount(rs.getInt(FIELD_ADDR_COUNT));
			reqCount.setLegalCount(rs.getInt(FIELD_LEGAL_COUNT));
			reqCount.setPinCount(rs.getInt(FIELD_PIN_COUNT));
			reqCount.setMiscCount(rs.getInt(FIELD_MISC_COUNT));
			reqCount.setImageCount(rs.getInt(FIELD_IMAGE_COUNT));
			reqCount.setCountyId(rs.getInt(FIELD_COUNTY_ID));
			reqCount.setCommunityId(rs.getInt(FIELD_COMMUNITY_ID));
			reqCount.setDescription(rs.getString(FIELD_DESCRIPTION));
		} catch (Exception e) {
			e.printStackTrace();
		}
		return reqCount;
	}
	
	public boolean saveToDatabase() {
		return saveToDatabase(5, true);
	}

	public boolean saveToDatabase(boolean updateDescription) {
		return saveToDatabase(5, updateDescription);
	}

	public boolean saveToDatabase(int retry, boolean updateDescription) {
		if (getSearchId() == INVALID_VALUE ||
				getCountyId() == INVALID_VALUE ||
				getCommunityId() == INVALID_VALUE ||
				getDataSource() == INVALID_VALUE
				) {
			throw new NullPointerException("Invalid parameter. Please update to a valid value and redo transaction.");
		}

		this.updateTotal();
		this.updateNonInstrumentTotal();
		if (updateDescription)
			this.setDescription(HashCountyToIndex.getServerAbbreviationByType(getDataSource()) +
					"(" + getInstrumentCount() + "/" + getNonInstrumentTotal() + ")");

		if (getTotal() == INVALID_VALUE || getNonInstrumentTotal() == INVALID_VALUE) {
			throw new NullPointerException("Invalid total count. Please update to a valid value and redo transaction.");
		}

		if (retry <= 0) {
			retry = 1;
		}
		
		if(getDataSource()!= DATASOURCE_TOTAL && StringUtils.isEmpty(HashCountyToIndex.getServerAbbreviationByType(getDataSource()))){
			return false;
		}
		
		if(getDataSource()!= DATASOURCE_TOTAL && getTotal() == 0){
			logger.error("Request count 0 for " + searchId);
			return false;
		}

		SimpleJdbcTemplate sjt = DBManager.getSimpleTemplate();
		for (int i = 0; i < retry; i++) {
			try {
				sjt.update(INSERT_UPDATE_REQUEST_COUNT,
						getSearchId(), getCountyId(), getCommunityId(), getDataSource(),
						getTotal(), getNonInstrumentTotal(), getInstrumentCount(), getNameCount(), getAddrCount(), // values if new
						getLegalCount(), getPinCount(), getMiscCount(), getImageCount(), getDescription(),
						getTotal(), getNonInstrumentTotal(), getInstrumentCount(), getNameCount(), getAddrCount(), // to update
						getLegalCount(), getPinCount(), getMiscCount(), getImageCount(), getDescription());
				return true;
			} catch (CannotAcquireLockException cale) {
				logger.error("Could not Acquire lock, number of retry " + i, cale);
			} catch (DataAccessException e) {
				logger.error("Error while saving entry in database", e);
			}
		}
		return false;
	}

	public static List<RequestCount> getRequestCountForSearch(long searchId) {
		SimpleJdbcTemplate sjt = DBManager.getSimpleTemplate();
		try {
			return sjt.query(SQL_SELECT_BY_SEARCHID, new RequestCount(), searchId);
		} catch (DataAccessException e) {
			logger.error("Error while getting imageCount for searchid " + searchId, e);
		}
		return null;

	}

	public static List<RequestCount> getRequestCountForSearchByDataSource(long searchId, int datasource) {
		SimpleJdbcTemplate sjt = DBManager.getSimpleTemplate();
		try {
			return sjt.query(SQL_SELECT_BY_SEARCHID_DATASOURCE, new RequestCount(), searchId, datasource);
		} catch (DataAccessException e) {
			logger.error("Error while getting imageCount for searchid " + searchId, e);
		}
		return null;

	}

	public static void updateRequestCountSearchID(long oldSearchId, long newSearchId) {
		SimpleJdbcTemplate sjt = DBManager.getSimpleTemplate();
		try {
			sjt.update(UPDATE_SEARCH_ID, newSearchId, oldSearchId);
		} catch (Exception e) {
			logger.error("Error while updating request count for searchid " + newSearchId, e);
		}
	}

	public static HashMap<Integer, RequestCount> getRequestCountForSearchID(long searchID) {
		try {
			List<RequestCount> rcl = getRequestCountForSearch(searchID);

			HashMap<Integer, RequestCount> rcm = new HashMap<Integer, RequestCount>();

			if (rcl == null)
				return rcm;

			if (rcl != null && rcl.size() > 0)
				for (RequestCount rc : rcl) {
					rcm.put(rc.getDataSource(), rc);
				}

			return rcm;
		} catch (Exception e) {
			e.printStackTrace();
		}
		return new HashMap<Integer, RequestCount>();
	}

	public static void saveRequestCountMapToDB(HashMap<Integer, RequestCount> reqCountMap) {
		try {
			if (reqCountMap != null && !reqCountMap.isEmpty()) {
				for (Entry<Integer, RequestCount> e : reqCountMap.entrySet())
					e.getValue().saveToDatabase();
			}
		} catch (Exception e) {
			e.printStackTrace();
		}

	}

	private void updateNonInstrumentTotal() {
		this.nonInstrumentTotal = this.nameCount + this.addrCount + this.legalCount + this.pinCount + this.miscCount + this.imageCount;
	}

	private void updateTotal() {
		this.total = this.instrumentCount + this.nameCount + this.addrCount + this.legalCount + this.pinCount + this.miscCount + this.imageCount;
	}

	public int getCommunityId() {
		return commId;
	}

	public void setCommunityId(int commId) {
		this.commId = commId;
	}

	public int getCountyId() {
		return countyId;
	}

	public void setCountyId(int countyId) {
		this.countyId = countyId;
	}

	public long getSearchId() {
		return searchId;
	}

	public void setSearchId(long searchId) {
		this.searchId = searchId;
	}

	public int getDataSource() {
		return dataSource;
	}

	public void setDataSource(int dataSource) {
		this.dataSource = dataSource;
	}

	public int getTotal() {
		return total;
	}

	public void setTotal(int total) {
		this.total = total;
	}

	public int getNonInstrumentTotal() {
		return nonInstrumentTotal;
	}

	public void setNonInstrumentTotal(int nonInstrumentTotal) {
		this.nonInstrumentTotal = nonInstrumentTotal;
	}

	public int getInstrumentCount() {
		return instrumentCount;
	}

	public void setInstrumentCount(int instrumentCount) {
		this.instrumentCount = instrumentCount;
	}

	public int getNameCount() {
		return nameCount;
	}

	public void setNameCount(int nameCount) {
		this.nameCount = nameCount;
	}

	public int getAddrCount() {
		return addrCount;
	}

	public void setAddrCount(int addrCount) {
		this.addrCount = addrCount;
	}

	public int getLegalCount() {
		return legalCount;
	}

	public void setLegalCount(int legalCount) {
		this.legalCount = legalCount;
	}

	public int getPinCount() {
		return pinCount;
	}

	public void setPinCount(int pinCount) {
		this.pinCount = pinCount;
	}

	public int getMiscCount() {
		return miscCount;
	}

	public void setMiscCount(int miscCount) {
		this.miscCount = miscCount;
	}

	public int getImageCount() {
		return imageCount;
	}

	public void setImageCount(int imageCount) {
		this.imageCount = imageCount;
	}

	public void incField(int type) {
		switch (type) {
		case TYPE_INSTRUMENT_COUNT:
			this.incInstrumentCount();
			break;
		case TYPE_NAME_COUNT:
			this.incNameCount();
			break;
		case TYPE_ADDR_COUNT:
			this.incAddrCount();
			break;
		case TYPE_LEGAL_COUNT:
			this.incLegalCount();
			break;
		case TYPE_PIN_COUNT:
			this.incPinCount();
			break;
		default:
		case TYPE_MISC_COUNT:
			this.incMiscCount();
			break;
		case TYPE_IMAGE_COUNT:
			this.incImageCount();
			break;
		}
	}

	private void incInstrumentCount() {
		if (this.instrumentCount == INVALID_VALUE)
			this.instrumentCount = 1;
		else
			this.instrumentCount++;
	}

	private void incNameCount() {
		if (this.nameCount == INVALID_VALUE)
			this.nameCount = 1;
		else
			this.nameCount++;
	}

	private void incAddrCount() {
		if (this.addrCount == INVALID_VALUE)
			this.addrCount = 1;
		else
			this.addrCount++;
	}

	private void incLegalCount() {
		if (this.legalCount == INVALID_VALUE)
			this.legalCount = 1;
		else
			this.legalCount++;
	}

	private void incPinCount() {
		if (this.pinCount == INVALID_VALUE)
			this.pinCount = 1;
		else
			this.pinCount++;
	}

	private void incMiscCount() {
		if (this.miscCount == INVALID_VALUE)
			this.miscCount = 1;
		else
			this.miscCount++;
	}

	private void incImageCount() {
		if (this.imageCount == INVALID_VALUE)
			this.imageCount = 1;
		else
			this.imageCount++;
	}

	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
	}

	public static void updateRequestCount(long searchId) {
		
	}
	
	/**
	 * 
	 * @param searchId
	 * @return HTML representation for request count report
	 */
	public static String getReportForTSRI(long searchId) {
		try{
			List<RequestCount> rcl = getRequestCountForSearchByDataSource(searchId, DATASOURCE_TOTAL);
			
			if (rcl == null)
				return "";
			
			if(rcl.size() == 0)
				return "";
			
			StringBuffer buf = new StringBuffer();
			buf.append("<div id=requestCount>");
			buf.append("Request Count (Instrument/Others):<br/>");
			
			
			for(RequestCount rc : rcl){
				buf.append(rc.getDescription() +  ", ");
			}
			buf.append("</div>");
			
			return buf.toString().replace(", <", "<");
		} catch (Exception e) {
			e.printStackTrace();
		}
		return "";
	}
}
