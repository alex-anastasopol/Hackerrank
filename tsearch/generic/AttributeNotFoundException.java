/*
 * Text here
 */

package ro.cst.tsearch.generic;


/** 
 * Thrown to indicate a parameter does not exist.
 *
 * @see SessionParser
 *
 */
public class AttributeNotFoundException extends Exception {

    /**
     * Constructs a new AttributeNotFoundException with no detail message.
     */
    public AttributeNotFoundException() {
        super();
    }

    /**
     * Constructs a new AttributeNotFoundException with the specified
     * detail message.
     *
     * @param s the detail message
     */
    public AttributeNotFoundException(String s) {
        super(s);
    }
}
