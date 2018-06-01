/*
 * Created on Apr 23, 2004
 *
 * To change the template for this generated file go to
 * Window>Preferences>Java>Code Generation>Code and Comments
 */
package ro.cst.tsearch.connection;

/**
 * @author 
 *
 * To change the template for this generated type comment go to
 * Window>Preferences>Java>Code Generation>Code and Comments
 */
public class ConnectionException extends Exception {
	private int httpcode;
	private String log;
	/**
	 * @return
	 */
	public int getHttpcode() {
		return httpcode;
	}

	/**
	 * @param i
	 */
	public void setHttpcode(int i) {
		httpcode = i;
	}

	/**
	 * @return
	 */
	public String getLog() {
		return log;
	}

	/**
	 * @param string
	 */
	public void setLog(String string) {
		log = string;
	}

}
