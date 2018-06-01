/*
 * Text here
 */

package ro.cst.tsearch.utils;

import javax.servlet.ServletException;

/** 
 * Thrown to indicate a parameter does not exist.
 *
 * @see ro.cst.vems.servlet.ParameterParser
 *
 * Base code was taked from com.oreilly.servlet package developed by 
 * Jason Hunter <jhunter@acm.org>
 */
public class ParameterNotFoundException extends ServletException {

    /**
     * Constructs a new ParameterNotFoundException with no detail message.
     */
    public ParameterNotFoundException() {
	super();
    }

    /**
     * Constructs a new ParameterNotFoundException with the specified
     * detail message.
     *
     * @param s the detail message
     */
    public ParameterNotFoundException(String s) {
	super(s);
    }
}
