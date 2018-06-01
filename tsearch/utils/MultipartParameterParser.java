/*
 *Text here
 */

package ro.cst.tsearch.utils;

import java.io.*;
import java.math.BigDecimal;

import javax.servlet.*;

import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Vector;

import com.oreilly.servlet.*;


/** 
 * A class to simplify parameter handling.  It can return parameters of
 * any primitive type (no casting or parsing required), can throw an 
 * exception when a parameter is not found (simplifying error handling),
 * and can accept default values (eliminating error handling).
 * <p>
 * It is used like this:
 * <blockquote><pre>
 * ParameterParser parser = new ParameterParser(req);
 * &nbsp;
 * float ratio = parser.getFloatParameter("ratio", 1.0);
 * &nbsp;
 * int count = 0;
 * try {
 *   count = parser.getIntParameter("count");
 * }
 * catch (NumberFormatException e) {
 *   handleMalformedCount();
 * }
 * catch (ParameterNotFoundException e) {
 *   handleNoCount();
 * }
 * </pre></blockquote>
 *
 * @see ro.cst.vems.servlet.ParameterNotFoundException
 *
 * Base code was taked from com.oreilly.servlet package developed by 
 * Jason Hunter <jhunter@acm.org>
 */
public class MultipartParameterParser {

    private static final int MAX_UPLOAD_FILE_SIZE = new Integer(1000 * 1024 * 1024).intValue();
    private MultipartRequest mreq;
    /**
     * Constructs a new ParameterParser to handle the parameters of the
     * given request.
     *
     * @param req the servlet request
     */
    public MultipartParameterParser(ServletRequest req) 
	throws IOException{

	this.mreq = new MultipartRequest(req, ".", MAX_UPLOAD_FILE_SIZE);
    }

    public MultipartParameterParser(ServletRequest req, String path)
	throws IOException{

	this.mreq = new MultipartRequest(req, path, MAX_UPLOAD_FILE_SIZE);
    }
	
    /**
     * Return the request object
     */
    public MultipartRequest getRequest() {
	return mreq;
    }

    /**
     * Gets the named parameter value as an File, with defaults
     * Returns the default value if the parameter is not found.
     *
     * @param name the parameter name
     * @param def the default parameter value
     * @return the parameter value as a file
     */
    public File getFileParameter(String name, File def){
	try{
	    return getFileParameter(name);
	} catch (Exception e){
	    return def;
	}
    }

    /**
     * Gets the named parameter value as a Files, without defaults
     *
     * @param name the parameter name
     * @return the parameter value as a array of Files
     * @exception ParameterNotFoundException if the parameter was not found
     * or was the empty file
     */
    public File getFileParameter(String name)
	throws ParameterNotFoundException {

	Enumeration files = mreq.getFileNames();
	try{
      	    while(files.hasMoreElements()){
		String paramName = (String)files.nextElement();
		if(paramName.equals(name) && (File)mreq.getFile(paramName)!= null){
		    return (File)mreq.getFile(paramName);
		}
	    }
	    throw new ParameterNotFoundException(name + "not found");
	}catch(Exception e){
	    throw new ParameterNotFoundException(name + "not found");
	}
    }
    
    public Vector getFileParameters(String name)
	throws ParameterNotFoundException {
		Enumeration files = mreq.getFileNames();
		Vector returnedFiles = new Vector(); 		
		try{
			while(files.hasMoreElements()){
				String paramName = (String)files.nextElement();
				if(paramName.startsWith(name) && (File)mreq.getFile(paramName)!= null){
					returnedFiles.add((File)mreq.getFile(paramName));
				}
			}
			return returnedFiles;			
			//throw new ParameterNotFoundException(name + "not found");
		}catch(Exception e){
			throw new ParameterNotFoundException(name + "not found");
		}
	}
	
    /**
     * Gets the named parameter value as a String
     *
     * @param name the parameter name
     * @return the parameter value as a String
     * @exception ParameterNotFoundException if the parameter was not found
     */
    public String getMultipartStringParameter(String name)
	throws ParameterNotFoundException {

	Enumeration params = mreq.getParameterNames();
	try{
	    while(params.hasMoreElements()){
		String paramName = (String)params.nextElement();
		if(paramName.equals(name) && mreq.getParameter(paramName)!=null)
		    return mreq.getParameter(paramName);
	    }
	    throw new ParameterNotFoundException(name + "not found");
	} catch (Exception e){
	    throw new ParameterNotFoundException(name + "not found");
	}
    }

    /**
     * Gets the named parameter value as a String, with a default.
     * Returns the default value if the parameter is not found or 
     * is the empty string.
     * 
     * @param name the parameter name
     * @param def the default parameter value
     * @return the parameter value as a String, or the default
     */
    public String getMultipartStringParameter(String name, String def) {
	try { 
	    return getMultipartStringParameter(name);
	} catch (Exception e) { 
	    return def; 
	}
    }

    /**
     * Gets the named parameter value as a int
     *
     * @param name the parameter name
     * @return the parameter value as a int
     * @exception ParameterNotFoundException if the parameter was not found
     * @exception NumberFormatException if the parameter could not be converted
     * to a int
     */
    public int getMultipartIntParameter(String name)
	throws ParameterNotFoundException, NumberFormatException {
	return Integer.parseInt(getMultipartStringParameter(name));
    }


    /**
     * Gets the named parameter value as a int, with a default.
     * Returns the default value if the parameter is not found.
     * 
     * @param name the parameter name
     * @param def the default parameter value
     * @return the parameter value as a int, or the default
     */
    public int getMultipartIntParameter(String name, int def) {
	try { 
	    return getMultipartIntParameter(name); 
	}catch (Exception e) { 
	    return def; 
	}
    }

    /**
     * Gets the named parameter value as a boolean
     *
     * @param name the parameter name
     * @return the parameter value as a boolean
     * @exception ParameterNotFoundException if the parameter was not found
     */
    public boolean getMultipartBooleanParameter(String name)
	throws ParameterNotFoundException {

	return Boolean.valueOf(getMultipartStringParameter(name)).booleanValue();
    }


    /**
     * Gets the named parameter value as a boolean, with a default.
     * Returns the default value if the parameter is not found.
     * 
     * @param name the parameter name
     * @param def the default parameter value
     * @return the parameter value as a boolean, or the default
     */
    public boolean getMultipartBooleanParameter(String name, boolean def) {
	try { 
	    return getMultipartBooleanParameter(name); 
	}catch (Exception e) { 
	    return def; 
	}
    }


    /**
     * Gets the named parameter value as a byte
     *
     * @param name the parameter name
     * @return the parameter value as a byte
     * @exception ParameterNotFoundException if the parameter was not found
     * @exception NumberFormatException if the parameter value could not
     * be converted to a byte
     */
    public byte getMultipartByteParameter(String name)
	throws ParameterNotFoundException, NumberFormatException {
	return Byte.parseByte(getMultipartStringParameter(name));
    }


    /**
     * Gets the named parameter value as a byte, with a default.
     * Returns the default value if the parameter is not found or cannot
     * be converted to a byte.
     * 
     * @param name the parameter name
     * @param def the default parameter value
     * @return the parameter value as a byte, or the default
     */
    public byte getMultipartByteParameter(String name, byte def) {
	try { 
	    return getMultipartByteParameter(name); 
	}catch (Exception e) { 
	    return def; 
	}
    }


    /**
     * Gets the named parameter value as a char
     *
     * @param name the parameter name
     * @return the parameter value as a char
     * @exception ParameterNotFoundException if the parameter was not found
     * or was the empty string
     */
    public char getMultipartCharParameter(String name)
	throws ParameterNotFoundException {
	String param = getMultipartStringParameter(name);
	if (param.length() == 0)
	    throw new ParameterNotFoundException(name + " is empty string");
	else
	    return (param.charAt(0));
    }

    /**
     * Gets the named parameter value as a char, with a default.
     * Returns the default value if the parameter is not found.
     * 
     * @param name the parameter name
     * @param def the default parameter value
     * @return the parameter value as a char, or the default
     */
    public char getMultipartCharParameter(String name, char def) {
	try { 
	    return getMultipartCharParameter(name); 
	}catch (Exception e) { 
	    return def; 
	}
    }



    /**
     * Gets the named parameter value as a double
     *
     * @param name the parameter name
     * @return the parameter value as a double
     * @exception ParameterNotFoundException if the parameter was not found
     * @exception NumberFormatException if the parameter could not be converted
     * to a double
     */
    public double getMultipartDoubleParameter(String name)
	throws ParameterNotFoundException, NumberFormatException {
	return Double.parseDouble(getMultipartStringParameter(name));
    }



    /**
     * Gets the named parameter value as a double, with a default.
     * Returns the default value if the parameter is not found.
     * 
     * @param name the parameter name
     * @param def the default parameter value
     * @return the parameter value as a double, or the default
     */
    public double getMultipartDoubleParameter(String name, double def) {
	try {
	    return getMultipartDoubleParameter(name); 
	}catch (Exception e) { 
	    return def; 
	}
    }


    /**
     * Gets the named parameter value as a float
     *
     * @param name the parameter name
     * @return the parameter value as a float
     * @exception ParameterNotFoundException if the parameter was not found
     * @exception NumberFormatException if the parameter could not be converted
     * to a float
     */
    public float getMultipartFloatParameter(String name)
	throws ParameterNotFoundException, NumberFormatException {
	return Float.parseFloat(getMultipartStringParameter(name));
    }


    /**
     * Gets the named parameter value as a float, with a default.
     * Returns the default value if the parameter is not found.
     * 
     * @param name the parameter name
     * @param def the default parameter value
     * @return the parameter value as a float, or the default
     */
    public float getMultipartFloatParameter(String name, float def) {
	try { 
	    return getMultipartFloatParameter(name); 
	}catch (Exception e) { 
	    return def; 
	}
    }



    /**
     * Gets the named parameter value as a long.
     *
     * @param name the parameter name
     * @return the parameter value as a long
     * @exception ParameterNotFoundException if the parameter was not found
     * @exception NumberFormatException if the parameter could not be converted
     * to a long
     */
    public long getMultipartLongParameter(String name)
	throws ParameterNotFoundException, NumberFormatException {
	return Long.parseLong(getMultipartStringParameter(name));
    }


    /**
     * Gets the named parameter value as a long, with a default.
     * Returns the default value if the parameter is not found.
     * 
     * @param name the parameter name
     * @param def the default parameter value
     * @return the parameter value as a long, or the default
     */
    public long getMultipartLongParameter(String name, long def) {
	try { 
	    return getMultipartLongParameter(name); 
	}catch (Exception e) { 
	    return def; 
	}
    }


    /**
     * Gets the named parameter value as a short
     *
     * @param name the parameter name
     * @return the parameter value as a short
     * @exception ParameterNotFoundException if the parameter was not found
     * @exception NumberFormatException if the parameter could not be converted
     * to a short
     */
    public short getMultipartShortParameter(String name)
	throws ParameterNotFoundException, NumberFormatException {
	return Short.parseShort(getMultipartStringParameter(name));
    }


    /**
     * Gets the named parameter value as a short, with a default.
     * Returns the default value if the parameter is not found.
     * 
     * @param name the parameter name
     * @param def the default parameter value
     * @return the parameter value as a short, or the default
     */
    public short getMultipartShortParameter(String name, short def) {
	try { 
	    return getMultipartShortParameter(name); 
	}catch (Exception e) { 
	    return def; 
	}
    }

	public BigDecimal getMultipartBigDecimalParameter(String name, BigDecimal def){
		try{
			return new BigDecimal(getMultipartStringParameter(name));
		} catch(Exception e){
			return def;
		}
	}

    /** Get a hashtable with all paramenter begining with 
     * a prefix. It can be used for example with a list of 
     * checkboxes having the name prefixated with a constant
     * an ended with a real name for the object (ex. an Id).
     * @param prefix is the prefix for paramenters names
     * @return a hash table having as keys the parameters names
     * after removing the prefix and as values the coresponding
     * values, as a string.
     * @see #getSetParameters(String) getSetParameters
     */
    public Hashtable getSetParameter(String prefix) {
	Hashtable ht = new Hashtable();

	Enumeration e = mreq.getParameterNames();

	while(e.hasMoreElements()) {
	    String param = (String) e.nextElement();
	    if(!param.startsWith(prefix))
		continue;

	    String[] values = new String [2];
	    if(mreq.getParameter(param)!=null)
		values[0] = mreq.getParameter(param);
	    else
		values[0] = " ";
	    
	    ht.put(param.substring(prefix.length()), values);
	}

	return ht;
    }
    public Hashtable getSetParameters(String prefix) {
	Hashtable ht = new Hashtable();

	Enumeration e = mreq.getParameterNames();

	while(e.hasMoreElements()) {
	    String param = (String) e.nextElement();
	    if(!param.startsWith(prefix))
		continue;

	    String values = " ";
	    if(mreq.getParameter(param)!=null)
		values = mreq.getParameter(param);
	    
	    ht.put(param.substring(prefix.length()), values);
	}

	return ht;
    }
    
}





