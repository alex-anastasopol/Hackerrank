package ro.cst.tsearch.database.rowmapper;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Date;

import org.springframework.jdbc.core.simple.ParameterizedRowMapper;

import ro.cst.tsearch.utils.DBConstants;

public class TimeGraphicMapper implements ParameterizedRowMapper<TimeGraphicMapper>{
	
	
	private Float value = Float.NaN;
	private Date time = new Date();
	private String server = "";
	private String extraField = "";
	/**
	 * @return the value
	 */
	public Float getValue() {
		return value;
	}
	/**
	 * @param value the value to set
	 */
	public void setValue(Float value) {
		this.value = value;
	}
	/**
	 * @return the time
	 */
	public Date getTime() {
		return time;
	}
	/**
	 * @param time the time to set
	 */
	public void setTime(Date time) {
		this.time = time;
	}
	/**
	 * @return the server
	 */
	public String getServer() {
		return server;
	}
	/**
	 * @param server the server to set
	 */
	public void setServer(String server) {
		this.server = server;
	}
	public String getExtraField() {
		return extraField;
	}
	public void setExtraField(String extraField) {
		this.extraField = extraField;
	}
	@Override
	public TimeGraphicMapper mapRow(ResultSet rs, int rownum)
			throws SQLException {
		TimeGraphicMapper mapper = new TimeGraphicMapper();
		mapper.setValue(rs.getFloat(DBConstants.TIME_GRAPHIC_VALUE));
		mapper.setTime(rs.getTimestamp(DBConstants.TIME_GRAPHIC_TIME));
		mapper.setServer(rs.getString(DBConstants.TIME_GRAPHIC_SERVER));
		try {
			mapper.setExtraField(rs.getString(DBConstants.TIME_GRAPHIC_EXTRA_FIELD));
		} catch (Exception e) {}
		return mapper;
	}
	/**
		 * 
		 * @return 
		 * @author 
		 */
		public String toString() {
			StringBuilder buffer = new StringBuilder();
			buffer.append("TimeGraphicMapper[");
			buffer.append("server = ").append(server);
			buffer.append(" time = ").append(time);
			buffer.append(" value = ").append(value);
			buffer.append("]");
			return buffer.toString();
		}
	
}
