package ro.cst.tsearch.user;

/**
 * Maps some user fields to an object
 * @author andrei
 *
 */
public class UserSimpleMapper {
	private long userID;
	private String login;
	private String firstName;
	private String lastName;
	private int userType;
	public UserSimpleMapper(long userID, String login, String firstName, String lastName, int userType) {
		super();
		this.userID = userID;
		this.login = login;
		this.firstName = firstName;
		this.lastName = lastName;
		this.userType = userType;
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
	public String getLogin() {
		return login;
	}
	public void setLogin(String login) {
		this.login = login;
	}
	public long getUserID() {
		return userID;
	}
	public void setUserID(long userID) {
		this.userID = userID;
	}
	
	public int getUserType() {
		return userType;
	}
	public void setUserType(int userType) {
		this.userType = userType;
	}
	public String printOption(){
		return "<option value=\"" + userID + "\">" + login + "</option>";
	}

}
