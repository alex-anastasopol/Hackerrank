package ro.cst.tsearch.data;

import java.sql.ResultSet;
import java.sql.SQLException;

import org.springframework.jdbc.core.simple.ParameterizedRowMapper;

public class UserCountyRate implements ParameterizedRowMapper<UserCountyRate>{	
	private UserCounty userCounty;
	private double a2cRate;
	private double c2aRate;
	public UserCounty getUserCounty() {
		return userCounty;
	}
	public void setUserCounty(UserCounty userCounty) {
		this.userCounty = userCounty;
	}
	public double getA2cRate() {
		return a2cRate;
	}
	public void setA2cRate(double rate) {
		a2cRate = rate;
	}
	public double getC2aRate() {
		return c2aRate;
	}
	public void setC2aRate(double rate) {
		c2aRate = rate;
	}
	@Override
	public UserCountyRate mapRow(ResultSet resultSet, int arg1) throws SQLException {
		UserCountyRate userCountyRate = new UserCountyRate();
		UserCounty uc = new UserCounty();
		uc.setCountyId(resultSet.getInt("county_id"));
		uc.setUserId(resultSet.getLong("user_id"));
		userCountyRate.setUserCounty(uc);
		userCountyRate.setA2cRate(resultSet.getDouble("a2cRate"));
		userCountyRate.setC2aRate(resultSet.getDouble("c2aRate"));
		return userCountyRate;
	}		
}
