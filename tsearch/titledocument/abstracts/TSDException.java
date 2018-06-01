/*
 * Created on Sep 6, 2004
 *
 * To change the template for this generated file go to
 * Window&gt;Preferences&gt;Java&gt;Code Generation&gt;Code and Comments
 */
package ro.cst.tsearch.titledocument.abstracts;

/**
 * @author george
 *
 * To change the template for this generated type comment go to
 * Window&gt;Preferences&gt;Java&gt;Code Generation&gt;Code and Comments
 */

public class TSDException extends Exception{
	public TSDException() {}
	public TSDException(Object classObj, String msg) {
		super("Error occurs in " + classObj.getClass().getName() 
				+ "\nError body: " + msg);
	}
}

