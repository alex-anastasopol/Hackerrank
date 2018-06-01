package ro.cst.tsearch.data;

import java.sql.ResultSet;
import java.sql.SQLException;

import org.springframework.jdbc.core.simple.ParameterizedRowMapper;

import ro.cst.tsearch.utils.DBConstants;

public class CountyStateUser implements ParameterizedRowMapper<CountyStateUser> {

	private int countyId;
	private String countyName;
	private String stateAbrv;
	private long userId;
	private String userAlias;
	private String restrictionAllowedAbstractors;
	private String restrictionAllowedAgents;
	
	private String ordersAgents;
	private String ordersProducts;
	private String ordersSubcategories;
	private String ordersWarnings;
	
	public int getCountyId() {
		return countyId;
	}
	public void setCountyId(int countyId) {
		this.countyId = countyId;
	}
	public String getCountyName() {
		return countyName;
	}
	public void setCountyName(String countyName) {
		this.countyName = countyName;
	}
	public String getStateAbrv() {
		return stateAbrv;
	}
	public void setStateAbrv(String stateAbrv) {
		this.stateAbrv = stateAbrv;
	}
	public long getUserId() {
		return userId;
	}
	public void setUserId(long userId) {
		this.userId = userId;
	}
	public String getUserAlias() {
		return userAlias;
	}
	public void setUserAlias(String userAlias) {
		this.userAlias = userAlias;
	}
	@Override
	public CountyStateUser mapRow(ResultSet resultSet, int rowNum) throws SQLException {
		CountyStateUser countyStateUser = new CountyStateUser();
		countyStateUser.setCountyId(resultSet.getInt("countyId"));
		countyStateUser.setCountyName(resultSet.getString("countyName"));
		countyStateUser.setStateAbrv(resultSet.getString("stateAbrv"));
		countyStateUser.setUserId(resultSet.getLong(DBConstants.FIELD_USER_ID));
		try {
			countyStateUser.setUserAlias(resultSet.getString("userName"));
		} catch (Exception e) {}
		return countyStateUser;
	}
	
	public String getCountyUserKey(){
		return countyId + "_" + userId;
	}
	public String getRestrictionAllowedAbstractors() {
		return restrictionAllowedAbstractors;
	}
	public void setRestrictionAllowedAbstractors(
			String restrictionAllowedAbstractors) {
		this.restrictionAllowedAbstractors = restrictionAllowedAbstractors;
	}
	public String getRestrictionAllowedAgents() {
		return restrictionAllowedAgents;
	}
	public void setRestrictionAllowedAgents(String restrictionAllowedAgents) {
		this.restrictionAllowedAgents = restrictionAllowedAgents;
	}
	public String getOrdersAgents() {
		return ordersAgents;
	}
	public void setOrdersAgents(String ordersAgents) {
		this.ordersAgents = ordersAgents;
	}
	public String getOrdersProducts() {
		return ordersProducts;
	}
	public void setOrdersProducts(String ordersProducts) {
		this.ordersProducts = ordersProducts;
	}
	public String getOrdersSubcategories() {
		return ordersSubcategories;
	}
	public void setOrdersSubcategories(String ordersSubcategories) {
		this.ordersSubcategories = ordersSubcategories;
	}
	public String getOrdersWarnings() {
		return ordersWarnings;
	}
	public void setOrdersWarnings(String ordersWarnings) {
		this.ordersWarnings = ordersWarnings;
	}
}
