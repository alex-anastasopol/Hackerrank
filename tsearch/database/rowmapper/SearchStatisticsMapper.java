package ro.cst.tsearch.database.rowmapper;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Date;

import org.springframework.jdbc.core.simple.ParameterizedRowMapper;

import ro.cst.tsearch.utils.DBConstants;

public class SearchStatisticsMapper implements
		ParameterizedRowMapper<SearchStatisticsMapper> {

	private Integer type;
	private String serverName;
	private Date timestamp;
	private Float searchOrderCount;
	public Integer getType() {
		return type;
	}
	public void setType(Integer type) {
		this.type = type;
	}
	public String getServerName() {
		return serverName;
	}
	public void setServerName(String serverName) {
		this.serverName = serverName;
	}
	public Date getTimestamp() {
		return timestamp;
	}
	public void setTimestamp(Date timestamp) {
		this.timestamp = timestamp;
	}
	public Float getSearchOrderCount() {
		return searchOrderCount;
	}
	public void setSearchOrderCount(Float searchOrderCount) {
		this.searchOrderCount = searchOrderCount;
	}
	@Override
	public SearchStatisticsMapper mapRow(ResultSet rs, int rowNum)
			throws SQLException {
		SearchStatisticsMapper mapper = new SearchStatisticsMapper();
		try {
			mapper.setTimestamp((Date)rs.getObject(DBConstants.FIELD_SEARCH_STATISTICS_TIMESTAMP));
		} catch (Exception e) {}
		try {
			mapper.setType((Integer)rs.getObject(DBConstants.FIELD_SEARCH_STATISTICS_TYPE));
		} catch (Exception e) {}
		try {
			mapper.setServerName((String)rs.getObject(DBConstants.FIELD_SEARCH_STATISTICS_SERVER_NAME));
		} catch (Exception e) {}
		try {
			Object obj = rs.getObject(DBConstants.FIELD_SEARCH_STATISTICS_ORDER_COUNT);
			if(obj instanceof Long) {
				mapper.setSearchOrderCount(((Long)obj).floatValue());
			} else {
				mapper.setSearchOrderCount(((Double)obj).floatValue());
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		
		return mapper;
	}

}
