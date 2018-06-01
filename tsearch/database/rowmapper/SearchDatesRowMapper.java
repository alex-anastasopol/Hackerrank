package ro.cst.tsearch.database.rowmapper;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Date;

import org.springframework.jdbc.core.simple.ParameterizedRowMapper;

import ro.cst.tsearch.utils.DBConstants;

public class SearchDatesRowMapper implements ParameterizedRowMapper<SearchDatesRowMapper> {
	
	private long searchId;
	private Date sdate;
	private Date sdateBackup;
	
	@Override
	public SearchDatesRowMapper mapRow(ResultSet resultSet, int rowNum) 
	throws SQLException {
		SearchDatesRowMapper srm = new SearchDatesRowMapper();
		srm.setSearchId(resultSet.getLong(DBConstants.FIELD_SEARCH_ID));
		srm.setSdate(resultSet.getTimestamp(DBConstants.FIELD_SEARCH_SDATE));
		srm.setSdateBackup(resultSet.getTimestamp("sdate_backup"));
		return srm;
	}

	public long getSearchId() {
		return searchId;
	}

	public void setSearchId(long searchId) {
		this.searchId = searchId;
	}

	public Date getSdate() {
		return sdate;
	}

	public void setSdate(Date sdate) {
		this.sdate = sdate;
	}

	public Date getSdateBackup() {
		return sdateBackup;
	}

	public void setSdateBackup(Date sdateBackup) {
		this.sdateBackup = sdateBackup;
	}

}
