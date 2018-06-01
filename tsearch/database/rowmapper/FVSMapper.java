package ro.cst.tsearch.database.rowmapper;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Date;

import org.springframework.jdbc.core.simple.ParameterizedRowMapper;

import ro.cst.tsearch.utils.DBConstants;

public class FVSMapper implements ParameterizedRowMapper<FVSMapper> {

	/**
	 * Mapper for FVS
	 */
	
	public static final String TABLE_FVS_DATA = "fvs_data";	
	
	public static final String FIELD_SEARCH_ID = "search_id";
	public static final String FIELD_FLAG = "flag";
	public static final String FIELD_ABSTR_FILE_NO = "abstr_fileno";
	public static final String FIELD_RUN_TIME = "run_time";
	public static final String FIELD_FLAG_DATE = "flag_date";
	public static final String FIELD_COMM_ID = "comm_id";
	public static final String FIELD_AGENT_ID = "agent_id";
	public static final String FIELD_UPDATES_RUNNED = "updates_runned";
	public static final String FIELD_COUNTY_ID = "county_id";

	
	
	private long search_id;
	private String flag;
	private String abstr_fileno;
	private Date run_time;
	private Date flag_date;
	private long comm_id;
	private long agent_id;
	private int updates_runned;
	private long county_id;
	
	public static final String SQL_SELECT_FVS_DATA = "SELECT * FROM " + TABLE_FVS_DATA + " WHERE ";
	
	public static final String SQL_SELECT_FVS_FLAGGED_SEARCHES = "SELECT * FROM " + DBConstants.TABLE_FVS_DATA + 
																			" WHERE " + DBConstants.FIELD_FVS_FLAG + " = ? " + 
																			" ORDER BY " + DBConstants.FIELD_FVS_COMM_ID;
	
	public static final String SQL_SELECT_FVS_UPDATES_RUNNED = "SELECT " + DBConstants.FIELD_FVS_UPDATES_RUNNED + 
																" FROM " + DBConstants.TABLE_FVS_DATA + 
																" WHERE " + DBConstants.FIELD_FVS_SEARCH_ID + " = ? ";
	
	@Override
	public FVSMapper mapRow(ResultSet resultSet, int rowNum) throws SQLException {
		FVSMapper fvsDataMapper = new FVSMapper();
		fvsDataMapper.setSearch_id(resultSet.getLong(FIELD_SEARCH_ID));
		fvsDataMapper.setFlag(resultSet.getString(FIELD_FLAG));
		fvsDataMapper.setAbstr_fileno(resultSet.getString(FIELD_ABSTR_FILE_NO));
		fvsDataMapper.setRun_time(resultSet.getTime(FIELD_RUN_TIME));
		fvsDataMapper.setFlag_date(resultSet.getDate(FIELD_FLAG_DATE));
		fvsDataMapper.setComm_id(resultSet.getLong(FIELD_COMM_ID));
		fvsDataMapper.setAgent_id(resultSet.getLong(FIELD_AGENT_ID));
		fvsDataMapper.setUpdates_runned(resultSet.getInt(FIELD_UPDATES_RUNNED));
		fvsDataMapper.setCounty_id(resultSet.getLong(FIELD_COUNTY_ID));

		return fvsDataMapper;
	}

	public long getSearch_id() {
		return search_id;
	}

	public void setSearch_id(long search_id) {
		this.search_id = search_id;
	}

	public String getFlag() {
		return flag;
	}

	public void setFlag(String flag) {
		this.flag = flag;
	}

	public String getAbstr_fileno() {
		return abstr_fileno;
	}

	public void setAbstr_fileno(String abstr_fileno) {
		this.abstr_fileno = abstr_fileno;
	}

	public Date getRun_time() {
		return run_time;
	}

	public void setRun_time(Date run_time) {
		this.run_time = run_time;
	}

	public Date getFlag_date() {
		return flag_date;
	}

	public void setFlag_date(Date flag_date) {
		this.flag_date = flag_date;
	}

	public long getComm_id() {
		return comm_id;
	}

	public void setComm_id(long comm_id) {
		this.comm_id = comm_id;
	}

	public long getAgent_id() {
		return agent_id;
	}

	public void setAgent_id(long agent_id) {
		this.agent_id = agent_id;
	}

	public int getUpdates_runned() {
		return updates_runned;
	}

	public void setUpdates_runned(int updates_runned) {
		this.updates_runned = updates_runned;
	}

	public long getCounty_id() {
		return county_id;
	}

	public void setCounty_id(long county_id) {
		this.county_id = county_id;
	}
	
	
}
