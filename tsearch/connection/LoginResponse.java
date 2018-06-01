package ro.cst.tsearch.connection;

/**
 * This class is used to get a detailed status from onLogin() method
 * @author vladb
 *
 */
public class LoginResponse {
	
	public enum LoginStatus { 
		STATUS_SUCCESS,
		STATUS_INVALID_PAGE,			// this should be used when the response from server isn't valid or doesn't contain the required parameters
		STATUS_REQ_TIMEOUT, 
		STATUS_UNKNOWN,					// this should be used when login fails and other statuses doesn't match
		STATUS_INVALID_CREDENTIALS,		// this should be used when Username / Password aren't valid
		STATUS_OUT_OF_BUSINESS_HOURS
	}
	
	private LoginStatus status;
	private String message;
	
	public LoginResponse() {}
	
	public LoginResponse(LoginStatus status, String message) {
		this.status = status;
		this.message = message;
	}
	
	@Override
	public String toString() {
		return "status: " + status + "\nmessage: " + message;
	}
	
	public static LoginResponse getDefaultSuccessResponse() {
		return new LoginResponse(LoginStatus.STATUS_SUCCESS, "Logged in");
	}
	
	public static LoginResponse getDefaultFailureResponse() {
		return new LoginResponse(LoginStatus.STATUS_UNKNOWN, "Login failed");
	}
	
	public static LoginResponse getDefaultInvalidCredentialsResponse() {
		return new LoginResponse(LoginStatus.STATUS_INVALID_CREDENTIALS, "Invalid credentials");
	}
	
	public LoginStatus getStatus() {
		return status;
	}
	
	public String getMessage() {
		return message;
	}
	
	public String appendMessage(String message) {
		return this.message += message; 
	}
}
