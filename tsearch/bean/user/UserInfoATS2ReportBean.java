package ro.cst.tsearch.bean.user;

import java.sql.ResultSet;
import java.sql.SQLException;

import org.springframework.jdbc.core.simple.ParameterizedRowMapper;

import ro.cst.tsearch.user.GroupAttributes;
import ro.cst.tsearch.user.UserAttributes;
import ro.cst.tsearch.utils.FormatDate;

public class UserInfoATS2ReportBean implements ParameterizedRowMapper<UserInfoATS2ReportBean>{
	private String userLastName;
	private String userFirstName;
	private String userLoginName;
	private String userRole;
	private String userCompanyName;
	private String userLastLogin;
	private String userEmailAddress;
	private boolean userHidden;
	
	public UserInfoATS2ReportBean() { 
		
	}
	
	public UserInfoATS2ReportBean(String userLastName, 
			String userFirstName, 
			String userLoginName, 
			String userRole, 
			String userCompanyName, 
			String userLastLogin, 
			String userEmailAddress,
			boolean userHidden) {
		
		this.userLastName = userLastName;
		this.userFirstName = userFirstName;
		this.userLoginName = userLoginName;
		this.userRole = userRole;
		this.userCompanyName = userCompanyName;
		this.userLastLogin = userLastLogin;
		this.userEmailAddress = userEmailAddress;
		this.userHidden = userHidden;
	}

	public String getUserLastName(){
		return this.userLastName;
	}
	
	public void setUserLastName(String lastName) {
		this.userLastName = lastName;
	}
	
	public String getUserFirstName() {
		return this.userFirstName;
	}

	public void setUserFirstName(String firstName) {
		this.userFirstName = firstName;
	}

	public String getUserLoginName() {
		return this.userLoginName;
	}

	public void setUserLoginName(String loginName) {
		this.userLoginName = loginName;
	}

	public String getUserRole() {
		return this.userRole;
	}
	
	public void setUserRole(String role) {
		this.userRole = role;
	}

	public String getUserCompanyName() {
		return this.userCompanyName;
	}

	public void setUserCompanyName(String companyName) {
		this.userCompanyName = companyName;
	}

	public String getUserLastLogin() {
		return this.userLastLogin;
	}

	public void setUserLastLogin(String lastLogin) {
		this.userLastLogin = lastLogin;
	}

	public String getUserEmailAddress() {
		return this.userEmailAddress;
	}

	public void setUserEmailAddress(String emailAdr) {
		this.userEmailAddress = emailAdr;
	}

	public boolean isUserHidden() {
		return userHidden;
	}

	public void setUserHidden(boolean userHidden) {
		this.userHidden = userHidden;
	}

	@Override
	public UserInfoATS2ReportBean mapRow(ResultSet rs, int rowNum) throws SQLException {
		UserInfoATS2ReportBean bean = new UserInfoATS2ReportBean();
		
		bean.setUserCompanyName(rs.getString(UserAttributes.USER_COMPANY));
		bean.setUserEmailAddress(rs.getString(UserAttributes.USER_EMAIL));
		bean.setUserFirstName(rs.getString(UserAttributes.USER_FIRSTNAME));
		bean.setUserHidden(rs.getInt(UserAttributes.USER_HIDDEN) > 0);
		
		try {
			bean.setUserLastLogin(new FormatDate(FormatDate.LASTLOGIN_FORMAT).getLocalDate(rs.getLong(UserAttributes.USER_LASTLOGIN)));
		} catch (Exception e) {
			bean.setUserLastLogin("N/A");
		}
		bean.setUserLastName(rs.getString(UserAttributes.USER_LASTNAME));
		bean.setUserLoginName(rs.getString(UserAttributes.USER_LOGIN));
		bean.setUserRole(rs.getString(GroupAttributes.GROUP_NAME));
		
		
		return bean;
	}
	
}
