/*
 * Created on May 27, 2003
 *
 * To change the template for this generated file go to
 * Window>Preferences>Java>Code Generation>Code and Comments
 */
package ro.cst.tsearch.exceptions;

/**
 * @author elmarie
 *
 * To change the template for this generated type comment go to
 * Window>Preferences>Java>Code Generation>Code and Comments
 */
public class BaseException extends Exception {
	/*
	 * Text here
	 */
	public BaseException() {
		super();
	}

	/**
	 * Constructs a new BaseException with the specified
	 * detail message.
	 *
	 * @param s the detail message
	 */
	public BaseException(String s) {
		super(s);
	}

	public BaseException(Throwable ex) {
		super(ex);
	}
}
