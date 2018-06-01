package ro.cst.tsearch.servers.parentsite;

import java.sql.ResultSet;
import java.sql.SQLException;

import org.springframework.jdbc.core.simple.ParameterizedRowMapper;

/**
 * immutable class holding county information 
 */
public class CountyWithState implements Comparable<CountyWithState>, ParameterizedRowMapper<CountyWithState> {

	private int countyId;
	private String countyName;
	private String stateAbrv;
	private int stateId;
	
	public CountyWithState(int id, String name, String state, int stateId) {
		countyId = id;
		countyName = name;
		stateAbrv = state;			
		this.stateId = stateId;
	}
	
	public CountyWithState() {
	}
	
	public String toString(){
		return "CountyWithState(id=" + countyId +", countyName=" + countyName + ", stateAbrv=" + stateAbrv + ")";		
	}

	public int compareTo(CountyWithState ct) {		
		return countyName.compareTo(ct.countyName);
	}

	@Override
	public CountyWithState mapRow(ResultSet resultSet, int rowNum) throws SQLException {
		CountyWithState cws = new CountyWithState(
				resultSet.getInt("countyId"),
				resultSet.getString("countyName"), 
				resultSet.getString("stateAbrv"),
				resultSet.getInt("stateId"));
		return cws;
	}

	/**
	 * @return the countyId
	 */
	public int getCountyId() {
		return countyId;
	}

	/**
	 * @param countyId the countyId to set
	 */
	public void setCountyId(int countyId) {
		this.countyId = countyId;
	}

	/**
	 * @return the countyName
	 */
	public String getCountyName() {
		return countyName;
	}

	/**
	 * @param countyName the countyName to set
	 */
	public void setCountyName(String countyName) {
		this.countyName = countyName;
	}

	/**
	 * @return the stateAbrv
	 */
	public String getStateAbrv() {
		return stateAbrv;
	}

	/**
	 * @param stateAbrv the stateAbrv to set
	 */
	public void setStateAbrv(String stateAbrv) {
		this.stateAbrv = stateAbrv;
	}

	public int getStateId() {
		return stateId;
	}

	public void setStateId(int stateId) {
		this.stateId = stateId;
	}

}
