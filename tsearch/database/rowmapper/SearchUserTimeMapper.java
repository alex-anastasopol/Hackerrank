package ro.cst.tsearch.database.rowmapper;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import org.springframework.jdbc.core.namedparam.BeanPropertySqlParameterSource;
import org.springframework.jdbc.core.namedparam.SqlParameterSource;
import org.springframework.jdbc.core.simple.ParameterizedRowMapper;
import org.springframework.jdbc.core.simple.SimpleJdbcTemplate;

import ro.cst.tsearch.Search;
import ro.cst.tsearch.database.DBManager;

public class SearchUserTimeMapper implements ParameterizedRowMapper<SearchUserTimeMapper> {

	public static final String	TABLE_SEARCH_USER_TIME	= "search_user_time";
	public static final String	FIELD_SEARCH_ID			= "sut_search_id";
	public static final String	FIELD_USER_ID			= "sut_user_id";
	public static final String	FIELD_WORKED_TIME		= "sut_worked_time";
	public static final String	INSERT_NAMED_PARAMS		= "INSERT INTO " + TABLE_SEARCH_USER_TIME +
																" (" + SearchUserTimeMapper.FIELD_SEARCH_ID + "," +
																SearchUserTimeMapper.FIELD_USER_ID + "," +
																SearchUserTimeMapper.FIELD_WORKED_TIME + ") values " +
																" ( :searchId , :userId , :workedTime) ";

	private long				searchId;
	private long				userId;
	private long				workedTime;

	public SearchUserTimeMapper() {
		super();
	}

	public SearchUserTimeMapper(long searchId, long userId, long workedTime) {
		super();
		this.searchId = searchId;
		this.userId = userId;
		this.workedTime = workedTime;
	}

	public long getSearchId() {
		return searchId;
	}

	public void setSearchId(long searchId) {
		this.searchId = searchId;
	}

	public long getUserId() {
		return userId;
	}

	public void setUserId(long userId) {
		this.userId = userId;
	}

	public long getWorkedTime() {
		return workedTime;
	}

	public void setWorkedTime(long workedTime) {
		this.workedTime = workedTime;
	}

	@Override
	public SearchUserTimeMapper mapRow(ResultSet rs, int rowNum) throws SQLException {
		SearchUserTimeMapper searchUserTimeMapper = new SearchUserTimeMapper();
		searchUserTimeMapper.setSearchId(rs.getLong(FIELD_SEARCH_ID));
		searchUserTimeMapper.setUserId(rs.getLong(FIELD_USER_ID));
		searchUserTimeMapper.setWorkedTime(rs.getLong(FIELD_WORKED_TIME));
		return searchUserTimeMapper;
	}

	public static void copyUserTime(Search sourceSearch, Search destSearch) {
		destSearch.setStartIntervalWorkDate(sourceSearch.getStartIntervalWorkDate());

		SimpleJdbcTemplate jdbcTemplate = DBManager.getSimpleTemplate();
		List<SearchUserTimeMapper> sourceList = jdbcTemplate.query(
				"SELECT * FROM " + TABLE_SEARCH_USER_TIME + " where " + FIELD_SEARCH_ID + " = ? and " + FIELD_USER_ID + " = ?",
				new SearchUserTimeMapper(),
				sourceSearch.getID(),
				sourceSearch.getSa().getAbstractorObject().getID().longValue());

		if (!sourceList.isEmpty()) {
			List<SqlParameterSource> parameters = new ArrayList<SqlParameterSource>();

			for (SearchUserTimeMapper searchUserTimeMapper : sourceList) {
				searchUserTimeMapper.setSearchId(destSearch.getID());
				parameters.add(new BeanPropertySqlParameterSource(searchUserTimeMapper));
			}

			jdbcTemplate.batchUpdate(INSERT_NAMED_PARAMS,
					parameters.toArray(new SqlParameterSource[parameters.size()]));

		}

	}
	
	/**
	 * @return "1D23:58:59"
	 */
	public String getWorkedTimeFormatted() {
		StringBuilder result = new StringBuilder();
		
		long seconds = workedTime % 60;
		long minutes = workedTime / 60;
		long hours = minutes / 60;
		minutes = minutes % 60;
		long days = hours / 24;
		hours = hours % 24;
		
		if(days > 0) {
			result.append(days).append("D");
		}
		if(hours > 0) {
			if(hours < 10) {
				result.append("0");
			} 
			result.append(hours).append(":");
		} else {
			result.append("00:");
		}
		
		if(minutes > 0) {
			if(minutes < 10) {
				result.append("0");
			} 
			result.append(minutes).append(":");
		} else {
			result.append("00:");
		}
		
		if(seconds > 0) {
			if(seconds < 10) {
				result.append("0");
			} 
			result.append(seconds);
		} else {
			result.append("00");
		}
		
		return result.toString();
	}

}
