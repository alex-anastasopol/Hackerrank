package ro.cst.tsearch.database.rowmapper;

import java.sql.ResultSet;
import java.sql.SQLException;

import org.springframework.jdbc.core.simple.ParameterizedRowMapper;

import ro.cst.tsearch.utils.DBConstants;

public class SearchIdRowMapper implements ParameterizedRowMapper<SearchIdRowMapper> {
	
	private long searchId;

	@Override
	public SearchIdRowMapper mapRow(ResultSet resultSet, int rowNum) 
	throws SQLException {
		SearchIdRowMapper srm = new SearchIdRowMapper();
		srm.setSearchId(resultSet.getLong(DBConstants.FIELD_SEARCH_ID));
		return srm;
	}

	public long getSearchId() {
		return searchId;
	}

	public void setSearchId(long searchId) {
		this.searchId = searchId;
	}

}
