/*
 *Text here
 */

package ro.cst.tsearch.generic;

import javax.servlet.http.*;


/** 
 * A class to simplify attribute handling.  It can return parameters of
 * any primitive type (no casting or parsing required), can throw an 
 * exception when a parameter is not found (simplifying error handling),
 * and can accept default values (eliminating error handling).
 * <p>
 * It is used like this:
 * <blockquote><pre>
 * SessionParser parser = new SessionParser(ses);
 * &nbsp;
 * int ratio = parser.getIntAttribute("ratio", 1);
 * &nbsp;
 * int count = 0;
 * try {
 *   count = parser.getIntAttribute("count");
 * }
 * catch (NumberFormatException e) {
 *   handleMalformedCount();
 * }
 * catch (ClassCaseException e) {
 *   handleMalformedCount();
 * }
 * catch (AttributeNotFoundException e) {
 *   handleNoCount();
 * }
 * </pre></blockquote>
 *
 * @see AttributeNotFoundException
 */
public class AttributeParser {

    private HttpSession ses;

    /**
     * Constructs a new AttributeParser to handle the parameters of the
     * given request.
     *
     * @param ses the session
     */
    public AttributeParser(HttpSession ses) {
        this.ses = ses;
    }

    /**
     * Return the session object
     */
    public HttpSession getSession() {
        return ses;
    }

    /**
     * Gets the named attribute value as a String
     *
     * @param name the attribute name
     * @return the attribute value as an Object
     * @exception AttributeNotFoundException if the attribute was not found
     * @exception ClassCastException if the attribute was found but the cast to String can't be done
     */
    public Object getObjectAttribute(String name)
        throws AttributeNotFoundException, ClassCastException {

        Object value = ses.getAttribute(name);        
        if (value == null)
            throw new AttributeNotFoundException(name + " not found");
        else
            return value;
    }

    /**
     * Gets the named attribute value as an Object, with a default.
     * Returns the default value if the attribute is not found or 
     * the cast to string throw an error
     * 
     * @param name the attribute name
     * @param def the default attribute value
     * @return the attribute value as a String, or the default
     */
    public Object getObjectAttribute(String name, Object def) {
        try { 
            Object value = getObjectAttribute(name);
            if (value == null) {
                return def;
            }
            return value;
        } catch (Exception e) { 
            return def; 
        }
    }

    /**
     * Gets the named attribute value as a String
     *
     * @param name the attribute name
     * @return the attribute value as a String
     * @exception AttributeNotFoundException if the attribute was not found
     * @exception ClassCastException if the attribute was found but the cast to String can't be done
     */
    public String getStringAttribute(String name)
        throws AttributeNotFoundException, ClassCastException {

        String value = (String)ses.getAttribute(name);        
        if (value == null)
            throw new AttributeNotFoundException(name + " not found");
        else
            return value;
    }


    /**
     * Gets the named attribute value as a String, with a default.
     * Returns the default value if the attribute is not found or 
     * the cast to string throw an error
     * 
     * @param name the attribute name
     * @param def the default attribute value
     * @return the attribute value as a String, or the default
     */
    public String getStringAttribute(String name, String def) {
        try { 
            return getStringAttribute(name);
        } catch (Exception e) { 
            return def; 
        }
    }


    /**
     * Gets the named attribute value as a boolean
     *
     * @param name the attribute name
     * @return the attribute value as a boolean
     * @exception AttributeNotFoundException if the attribute was not found
     * @exception ClassCastException if the attribute was found but the cast throws an exception
     */
    public boolean getBooleanAttribute(String name)
        throws AttributeNotFoundException, ClassCastException {

        return Boolean.valueOf(getStringAttribute(name)).booleanValue();
    }


    /**
     * Gets the named attribute value as a boolean, with a default.
     * Returns the default value if the attribute is not found.
     * 
     * @param name the attribute name
     * @param def the default attribute value
     * @return the attribute value as a boolean, or the default
     */
    public boolean getBooleanAttribute(String name, boolean def) {
        try { 
            return getBooleanAttribute(name); 
        }catch (Exception e) { 
            return def; 
        }
    }




    /**
     * Gets the named attribute value as a int
     *
     * @param name the attribute name
     * @return the attribute value as a int
     * @exception AttributeNotFoundException if the parameter was not found
     * @exception NumberFormatException if the parameter could not be converted
     * to a int
     * @exception ClassCastException  if the parameter could not be converted to String    
     */
    public int getIntAttribute(String name)
        throws AttributeNotFoundException, ClassCastException, NumberFormatException {
        return Integer.parseInt(getStringAttribute(name));
    }


    /**
     * Gets the named attribute value as a int, with a default.
     * Returns the default value if the attribute is not found.
     * 
     * @param name the attribute name
     * @param def the default attribute value
     * @return the attribute value as a int, or the default
     */
    public int getIntAttribute(String name, int def) {
        try { 
            return getIntAttribute(name); 
        }catch (Exception e) { 
            return def; 
        }
    }


}
