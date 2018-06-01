package ro.cst.tsearch.reports.data;

import java.sql.ResultSet;
import java.sql.SQLException;

import org.apache.commons.lang.StringUtils;
import org.springframework.jdbc.core.simple.ParameterizedRowMapper;

import ro.cst.tsearch.database.rowmapper.SearchUserTimeMapper;
import ro.cst.tsearch.user.UserAttributes;
import ro.cst.tsearch.utils.DBConstants;

public class AbstractorWorkedTime implements ParameterizedRowMapper<AbstractorWorkedTime> {
	private SearchUserTimeMapper userTime;
	private String firstName;
	private String lastName;
	private String loginName;
	public AbstractorWorkedTime() {
		userTime = new SearchUserTimeMapper();
	}
	public AbstractorWorkedTime(UserAttributes userAttributes) {
		this();
		setFirstName(userAttributes.getFIRSTNAME());
		setLastName(userAttributes.getLASTNAME());
		setLoginName(userAttributes.getLOGIN());
		setUserId(userAttributes.getID().longValue());
	}
	public long getUserId() {
		return userTime.getUserId();
	}
	public void setUserId(long userId) {
		this.userTime.setUserId(userId);
	}
	public String getFirstName() {
		return firstName;
	}
	public void setFirstName(String firstName) {
		this.firstName = firstName;
	}
	public String getLastName() {
		return lastName;
	}
	public void setLastName(String lastName) {
		this.lastName = lastName;
	}
	public String getLoginName() {
		return loginName;
	}
	public void setLoginName(String loginName) {
		this.loginName = loginName;
	}
	public long getWorkedTime() {
		return userTime.getWorkedTime();
	}
	public void setWorkedTime(long workedTime) {
		this.userTime.setWorkedTime(workedTime);
	}
	public long getSearchId() {
		return userTime.getSearchId();
	}
	public void setSearchId(long searchId) {
		this.userTime.setSearchId(searchId);
	}
	public void setUserTime(SearchUserTimeMapper userTime) {
		this.userTime = userTime;
	}
	public SearchUserTimeMapper getUserTime() {
		return userTime;
	}
	@Override
	public AbstractorWorkedTime mapRow(ResultSet rs, int rowNum) throws SQLException {
		AbstractorWorkedTime abstractorWorkedTime = new AbstractorWorkedTime();
		abstractorWorkedTime.setFirstName(rs.getString(DBConstants.FIELD_USER_FIRST_NAME));
		abstractorWorkedTime.setLastName(rs.getString(DBConstants.FIELD_USER_LAST_NAME));
		try {
			abstractorWorkedTime.setLoginName(rs.getString(DBConstants.FIELD_USER_LOGIN));
		} catch (Exception ignored) {
		}
		abstractorWorkedTime.setUserTime(new SearchUserTimeMapper().mapRow(rs, rowNum));
		return abstractorWorkedTime;
	}
	public String getFormatted() {
		String result = getFirstName() + " " + getLastName();
		if(getWorkedTime() > 0) {
			result += " [" + getUserTime().getWorkedTimeFormatted() + "]";
		}
		return StringUtils.normalizeSpace(result);
	}
	
	
}
