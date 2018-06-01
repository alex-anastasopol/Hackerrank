
package ro.cst.tsearch.exceptions;

/**
 * 
 * @author mihaib
 *
 */
public class LoginException extends RuntimeException{
	
	public static final long serialVersionUID = 10000;
	

	public LoginException(String message){
		super(message);
	}
	
	public LoginException(Throwable exception) {
		super(exception);
	}
	
}