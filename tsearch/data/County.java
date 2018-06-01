package ro.cst.tsearch.data;

import java.sql.ResultSet;
import java.sql.SQLException;

import org.springframework.jdbc.core.simple.ParameterizedRowMapper;

import ro.cst.tsearch.bean.SearchAttributes;
import ro.cst.tsearch.utils.DBConstants;

public class County implements ParameterizedRowMapper<County>{
	private long id = 0;
	private String name;
	private long stateId = 0;
	private String idFips;
	private String stateAbv;
	
	@Override
	public County mapRow(ResultSet resultSet, int rowNum) throws SQLException {
		County county = new County();
		county.setId(resultSet.getLong(DBConstants.FIELD_COUNTY_ID));
		county.setStateId(resultSet.getLong(DBConstants.FIELD_COUNTY_STATE_ID));
		county.setName(resultSet.getString(DBConstants.FIELD_COUNTY_NAME));
		county.setIdFips(resultSet.getString(DBConstants.FIELD_COUNTY_FIPS_ID));
		try {
			county.setStateAbv(resultSet.getString(DBConstants.FIELD_STATE_ABV));
		} catch (Exception e) {
		}
		return county;
	}
	/**
	 * @return the id
	 */
	public long getId() {
		return id;
	}
	/**
	 * @param id the id to set
	 */
	public void setId(long id) {
		this.id = id;
	}
	/**
	 * @return the name
	 */
	public String getName() {
		return name;
	}
	/**
	 * @param name the name to set
	 */
	public void setName(String name) {
		this.name = name;
	}
	/**
	 * @return the stateId
	 */
	public long getStateId() {
		return stateId;
	}
	/**
	 * @param stateId the stateId to set
	 */
	public void setStateId(long stateId) {
		this.stateId = stateId;
	}
	/**
	 * @return the idFips
	 */
	public String getIdFips() {
		return idFips;
	}
	/**
	 * @param idFips the idFips to set
	 */
	public void setIdFips(String idFips) {
		this.idFips = idFips;
	}
	public String getStateAbv() {
		return stateAbv;
	}
	public void setStateAbv(String stateAbv) {
		this.stateAbv = stateAbv;
	}
}
