/*
 *Text here
 */

package ro.cst.tsearch.utils;

import java.io.*;
import javax.servlet.*;

import ro.cst.tsearch.generic.Util;

import java.util.Enumeration;
import java.util.Vector;
import java.util.Hashtable;
import java.math.BigDecimal;

import com.oreilly.servlet.*;
import org.apache.log4j.Category;

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
public class ParameterParser {

	protected static final Category logger= Category.getInstance(ParameterParser.class.getName());
	private ServletRequest req;

	/**
	 * Constructs a new ParameterParser to handle the parameters of the
	 * given request.
	 *
	 * @param req the servlet request
	 */
	public ParameterParser(ServletRequest req) {
		this.req = req;
	}

	/**
	 * Return the request object
	 */
	public ServletRequest getRequest() {
		return req;
	}

	/**
	 * Gets the named parameter value as a String
	 *
	 * @param name the parameter name
	 * @return the parameter value as a String
	 * @exception ParameterNotFoundException if the parameter was not found
	 */
	public String getStringParameter(String name) throws ParameterNotFoundException {

		String[] values = req.getParameterValues(name);
		if (values == null)
			throw new ParameterNotFoundException(name + " not found");
		else
			return values[0];
	}

	/**
	 * Gets the named parameter value as a String
	 *
	 * @param name the parameter name
	 * @return the parameter value as a String
	 * @exception ParameterNotFoundException if the parameter was not found
	 */
	public String getMultipleStringParameter(String name) throws ParameterNotFoundException {

		String[] values = req.getParameterValues(name);
		if (values == null)
			throw new ParameterNotFoundException(name + " not found");
		else
			return Util.getStringsList(values, ",");
	}
	
	/**
	 * Gets the named parameter value as a String
	 *
	 * @param name the parameter name
	 * @return the parameter value as a String or the default
	 * @exception ParameterNotFoundException if the parameter was not found
	 */
	public String getMultipleStringParameter(String name, String def) throws ParameterNotFoundException {

		String[] values = req.getParameterValues(name);
		if (values == null)
			return def;
		else
			return Util.getStringsList(values, ",");
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
	public String getStringParameter(String name, String def) {
		try {
			return getStringParameter(name);
		} catch (Exception e) {
			return def;
		}
	}

	/**
	     * Gets the named parameter value as a String
	     *
	     * @param name the parameter name
	     * @return the parameter value as a String
	     * @exception ParameterNotFoundException if the parameter was not found
	     */
	public String getVoidStringParameter(String name) throws ParameterNotFoundException {

		String[] values = req.getParameterValues(name);
		if (values == null || values[0].equals("")) {
			throw new ParameterNotFoundException(name + " not found");
		} else
			return values[0];
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
	public String getVoidStringParameter(String name, String def) {
		try {
			return getVoidStringParameter(name);
		} catch (Exception e) {
			return def;
		}
	}

	/**
	 * Gets the named parameter value as an array of Strings
	 *
	 * @param name the parameter name
	 * @return the parameter value as a array of Strings
	 * @exception ParameterNotFoundException if the parameter was not found
	 * or was the empty string
	 */
	public String[] getStringParameters(String name) throws ParameterNotFoundException {

		String[] values = req.getParameterValues(name);
		if (values == null)
			throw new ParameterNotFoundException(name + " not found");
		else
			return values;
	}

	/**
	 * Gets the named parameter value as an array of Strings, with defaults
	 * Returns the default value if the parameter is not found.
	 *
	 * @param name the parameter name
	 * @param def the default parameter value
	 * @return the parameter value as a array of Strings
	 */
	public String[] getStringParameters(String name, String[] def) {
		try {
			return getStringParameters(name);
		} catch (Exception e) {
			return def;
		}
	}

	public Vector getStringParametersVector(String name,int startContor){
		
		Vector v =new Vector();
		
		String curentValue= "start";
		
		while(true){
			curentValue=this.getStringParameter(name+"_"+startContor,"-1");
			
			if(curentValue!=null&& (!"-1".equals(curentValue)) && (!"".equals(curentValue))){
				v.add(curentValue);
			}
			else {
				break;
			}
			startContor++;
		}
		
		return v;
	}
	
	/*  public File[] getFileParameters(String name)
	throws ParameterNotFoundException {
	
	Vector v = new Vector();
	
	MultipartRequest multi = new MultipartRequest(req, ".");
	Enumeration files = multi.getFileNames();
	while (files.hasMoreElements()) {
	    String name = (String)files.nextElement();
	    v.addElement(multi.getFile(name));
	}
	
	return (File[]) v.toArray(new File[v.size()]);
	}*/

	/**
	 * Gets the named parameter value as a BigDecimal
	 *
	 * @param name the parameter name
	 * @return the parameter value as a boolean
	 * @exception ParameterNotFoundException if the parameter was not found
	 */
	public BigDecimal getBigDecimalParameter(String name) throws ParameterNotFoundException {

		return new BigDecimal(getStringParameter(name));
	}

	/**
	 * Gets the named parameter value as a BigDecimal, with a default.
	 * Returns the default value if the parameter is not found.
	 * 
	 * @param name the parameter name
	 * @param def the default parameter value
	 * @return the parameter value as a BigDecimal, or the default
	 */
	public BigDecimal getBigDecimalParameter(String name, long def) {
		try {
			return getBigDecimalParameter(name);
		} catch (Exception e) {
			return BigDecimal.valueOf(def);
		}
	}

	public BigDecimal getBigDecimalParameter(String name, BigDecimal defaultValue) {
		try {
			return getBigDecimalParameter(name);
		} catch (Exception e) {
			return defaultValue;
		}
	}

	/**
	 * Gets the named parameter value as a array of BigDecimal values
	 *
	 * @param name the parameter name
	 * @return the parameter value as an array of BigDecimal values
	 * @exception ParameterNotFoundException if the parameter was not found
	 */
	public BigDecimal[] getBigDecimalParameters(String name) throws ParameterNotFoundException {

		String[] values = getStringParameters(name);
		BigDecimal[] ret = new BigDecimal[values.length];

		for (int i = 0; i < values.length; i++) {
			ret[i] = new BigDecimal(values[i]);
		}

		return ret;
	}

	/**
	 * Gets the named parameter value as a boolean
	 *
	 * @param name the parameter name
	 * @return the parameter value as a boolean
	 * @exception ParameterNotFoundException if the parameter was not found
	 */
	public boolean getBooleanParameter(String name) throws ParameterNotFoundException {

		return Boolean.valueOf(getStringParameter(name)).booleanValue();
	}

	/**
	 * Gets the named parameter value as a boolean, with a default.
	 * Returns the default value if the parameter is not found.
	 * 
	 * @param name the parameter name
	 * @param def the default parameter value
	 * @return the parameter value as a boolean, or the default
	 */
	public boolean getBooleanParameter(String name, boolean def) {
		try {
			return getBooleanParameter(name);
		} catch (Exception e) {
			return def;
		}
	}

	/**
	 * Gets the named parameter value as a array of boolean values
	 *
	 * @param name the parameter name
	 * @return the parameter value as an array of boolean values
	 * @exception ParameterNotFoundException if the parameter was not found
	 */
	public boolean[] getBooleanParameters(String name) throws ParameterNotFoundException {

		String[] values = getStringParameters(name);
		boolean[] ret = new boolean[values.length];

		for (int i = 0; i < values.length; i++) {
			ret[i] = Boolean.getBoolean(values[i]);
		}

		return ret;
	}

	/**
	 * Gets the named parameter value as a array of boolean values, with defaults
	 * Returns the default array if the parameter is not found.
	 *
	 * @param name the parameter name
	 * @param def the default array
	 * @return the parameter value as an array of boolean values
	 */
	public boolean[] getBooleanParameters(String name, boolean def[]) {

		try {
			return getBooleanParameters(name);
		} catch (Exception e) {
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
	public byte getByteParameter(String name) throws ParameterNotFoundException, NumberFormatException {
		return Byte.parseByte(getStringParameter(name));
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
	public byte getByteParameter(String name, byte def) {
		try {
			return getByteParameter(name);
		} catch (Exception e) {
			return def;
		}
	}

	/**
	 * Gets the named parameter value as an array of bytes.
	 *
	 * @param name the parameter name
	 * @return the parameter value as an array of bytes
	 * @exception ParameterNotFoundException if the parameter was not found
	 * @exception NumberFormatException if a the parameter value could not
	 * be converted to a byte
	 */
	public byte[] getByteParameters(String name) throws ParameterNotFoundException, NumberFormatException {

		String[] values = getStringParameters(name);
		byte[] ret = new byte[values.length];

		for (int i = 0; i < values.length; i++) {
			ret[i] = Byte.parseByte(values[i]);
		}

		return ret;
	}

	/**
	 * Gets the named parameter value as an array of bytes, with a default
	 * Returns the default array if the parameter is not found or at least
	 * value cannot converted to a byte.
	 *
	 * @param name the parameter name
	 * @param def the default array
	 * @return the parameter value as an array of bytes
	 */
	public byte[] getByteParameters(String name, byte[] def) {

		try {
			return getByteParameters(name);
		} catch (Exception e) {
			return def;
		}
	}

	public File[] getFileParameters(String name) throws ParameterNotFoundException, IOException {

		MultipartRequest multi = new MultipartRequest(req, ".");
		Vector v = new Vector();

		Enumeration files = multi.getFileNames();
		if (files == null)
			throw new ParameterNotFoundException(name + "not found");
		while (files.hasMoreElements()) {
			File f = multi.getFile((String) files.nextElement());
			v.addElement((File) f);
		}
		return (File[]) v.toArray(new File[v.size()]);

	}

	public File getFileParameter(String name) throws ParameterNotFoundException, IOException {

		MultipartRequest multi = new MultipartRequest(req, ".");

		Enumeration files = multi.getFileNames();
		if (files == null)
			throw new ParameterNotFoundException(name + "not found");
		File file = multi.getFile((String) files.nextElement());
		logger.info("nume fisier->>>>" + file.getName());
		logger.info("absolute path fisier->>>>" + file.getAbsolutePath());
		logger.info("parent fisier->>>>" + file.getParent());
		logger.info("path fisier->>>>" + file.getPath());
		return file;
	}

	/**
	 * Gets the named parameter value as a char
	 *
	 * @param name the parameter name
	 * @return the parameter value as a char
	 * @exception ParameterNotFoundException if the parameter was not found
	 * or was the empty string
	 */
	public char getCharParameter(String name) throws ParameterNotFoundException {
		String param = getStringParameter(name);
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
	public char getCharParameter(String name, char def) {
		try {
			return getCharParameter(name);
		} catch (Exception e) {
			return def;
		}
	}

	/**
	 * Gets the named parameter value as an array of chars
	 *
	 * @param name the parameter name
	 * @return the parameter value as an array of chars
	 * @exception ParameterNotFoundException if the parameter was not found
	 * or at least one value was the empty string
	 */
	public char[] getCharParameters(String name) throws ParameterNotFoundException {

		String[] values = getStringParameters(name);
		char[] ret = new char[values.length];

		for (int i = 0; i < values.length; i++) {
			if (values[i].length() == 0) {
				throw new ParameterNotFoundException(name + " has an empty string" + "at position" + i);
			} else {
				ret[i] = values[i].charAt(0);
			}
		}

		return ret;
	}

	/**
	 * Gets the named parameter value as an array of chars, with defaults
	 * Returns the default array if the parameter is not found.
	 *
	 * @param name the parameter name
	 * @param def the default array
	 * @return the parameter value as an array of chars
	 */
	public char[] getCharParameters(String name, char[] def) {

		try {
			return getCharParameters(name);
		} catch (Exception e) {
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
	public double getDoubleParameter(String name) throws ParameterNotFoundException, NumberFormatException {
		return Double.parseDouble(getStringParameter(name));
	}

	/**
	 * Gets the named parameter value as a double, with a default.
	 * Returns the default value if the parameter is not found.
	 * 
	 * @param name the parameter name
	 * @param def the default parameter value
	 * @return the parameter value as a double, or the default
	 */
	public double getDoubleParameter(String name, double def) {
		try {
			return getDoubleParameter(name);
		} catch (Exception e) {
			return def;
		}
	}	

	/**
	 * Gets the named parameter value as an array of double values
	 *
	 * @param name the parameter name
	 * @return the parameter value as an array of double values
	 * @exception ParameterNotFoundException if the parameter was not found
	 * @exception NumberFormatException if at least one value could not be 
	 * converted to a double
	 */
	public double[] getDoubleParameters(String name)
		throws ParameterNotFoundException, NumberFormatException {

		String[] values = getStringParameters(name);
		double[] ret = new double[values.length];

		for (int i = 0; i < values.length; i++) {
			ret[i] = Double.parseDouble(values[i]);
		}

		return ret;
	}

	/**
	 * Gets the named parameter value as an array of double values, with a default.
	 * Returns the default value if the parameter is not found.
	 *
	 * @param name the parameter name
	 * @param def the default array
	 * @return the parameter value as an array of double values
	 */
	public double[] getDoubleParameters(String name, double[] def) {

		try {
			return getDoubleParameters(name);
		} catch (Exception e) {
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
	public float getFloatParameter(String name) throws ParameterNotFoundException, NumberFormatException {
		return Float.parseFloat(getStringParameter(name));
	}

	/**
	 * Gets the named parameter value as a float, with a default.
	 * Returns the default value if the parameter is not found.
	 * 
	 * @param name the parameter name
	 * @param def the default parameter value
	 * @return the parameter value as a float, or the default
	 */
	public float getFloatParameter(String name, float def) {
		try {
			return getFloatParameter(name);
		} catch (Exception e) {
			return def;
		}
	}

	/**
	 * Gets the named parameter value as an array of float values
	 *
	 * @param name the parameter name
	 * @return the parameter value as an array of float values
	 * @exception ParameterNotFoundException if the parameter was not found
	 * @exception NumberFormatException if at least one value  could not be 
	 * converted to a float
	 */
	public float[] getFloatParameters(String name) throws ParameterNotFoundException, NumberFormatException {

		String[] values = getStringParameters(name);
		float[] ret = new float[values.length];

		for (int i = 0; i < values.length; i++) {
			ret[i] = Float.parseFloat(values[i]);
		}

		return ret;
	}

	/**
	 * Gets the named parameter value as an array of float values, with defaults
	 * Returns the default array if the parameter is not found.
	 *
	 * @param name the parameter name
	 * @param def the default array
	 * @return the parameter value as an array of float values
	 */
	public float[] getFloatParameters(String name, float[] def) {

		try {
			return getFloatParameters(name);
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
	public int getIntParameter(String name) throws ParameterNotFoundException, NumberFormatException {
		return Integer.parseInt(getStringParameter(name));
	}

	/**
	 * Gets the named parameter value as a int, with a default.
	 * Returns the default value if the parameter is not found.
	 * 
	 * @param name the parameter name
	 * @param def the default parameter value
	 * @return the parameter value as a int, or the default
	 */
	public int getIntParameter(String name, int def) {
		try {
			return getIntParameter(name);
		} catch (Exception e) {
			return def;
		}
	}

	/**
	 * Gets the named parameter value as an array of integers
	 *
	 * @param name the parameter name
	 * @return the parameter value as an array of integers
	 * @exception ParameterNotFoundException if the parameter was not found
	 * @exception NumberFormatException if at laeast one value parameter 
	 * could not be converted to a int
	 */
	public int[] getIntParameters(String name) throws ParameterNotFoundException, NumberFormatException {

		String[] values = getStringParameters(name);
		int[] ret = new int[values.length];

		for (int i = 0; i < values.length; i++) {
			ret[i] = Integer.parseInt(values[i]);
		}

		return ret;
	}

	/**
	 * Gets the named parameter value as an array of integers, with a default.
	 * Returns the default array if the parameter is not found.
	 *
	 * @param name the parameter name
	 * @param def the default array
	 * @return the parameter value as an array of integers
	 */
	public int[] getIntParameters(String name, int[] def) {
		try {
			return getIntParameters(name);
		} catch (Exception e) {
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
	public long getLongParameter(String name) throws ParameterNotFoundException, NumberFormatException {
		return Long.parseLong(getStringParameter(name));
	}

	/**
	 * Gets the named parameter value as a long, with a default.
	 * Returns the default value if the parameter is not found.
	 * 
	 * @param name the parameter name
	 * @param def the default parameter value
	 * @return the parameter value as a long, or the default
	 */
	public long getLongParameter(String name, long def) {
		try {
			return getLongParameter(name);
		} catch (Exception e) {
			return def;
		}
	}

	/**
	 * Gets the named parameter value as an array of long values
	 *
	 * @param name the parameter name
	 * @return the parameter value as an array of long values
	 * @exception ParameterNotFoundException if the parameter was not found
	 * @exception NumberFormatException if at laeast one value parameter 
	 * could not be converted to a long
	 */
	public long[] getLongParameters(String name) throws ParameterNotFoundException, NumberFormatException {

		String[] values = getStringParameters(name);
		long[] ret = new long[values.length];

		for (int i = 0; i < values.length; i++) {
			ret[i] = Long.parseLong(values[i]);
		}

		return ret;
	}

	/**
	 * Gets the named parameter value as an array of long values, with a default.
	 * Returns the default value if the parameter is not found.
	 *
	 * @param name the parameter name
	 * @param def the default array
	 * @return the parameter value as an array of long values
	 */
	public long[] getLongParameters(String name, long[] def) {

		try {
			return getLongParameters(name);
		} catch (Exception e) {
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
	public short getShortParameter(String name) throws ParameterNotFoundException, NumberFormatException {
		return Short.parseShort(getStringParameter(name));
	}

	/**
	 * Gets the named parameter value as a short, with a default.
	 * Returns the default value if the parameter is not found.
	 * 
	 * @param name the parameter name
	 * @param def the default parameter value
	 * @return the parameter value as a short, or the default
	 */
	public short getShortParameter(String name, short def) {
		try {
			return getShortParameter(name);
		} catch (Exception e) {
			return def;
		}
	}

	/**
	 * Gets the named parameter value as an array of short values
	 *
	 * @param name the parameter name
	 * @return the parameter value as an array of short values
	 * @exception ParameterNotFoundException if the parameter was not found
	 * @exception NumberFormatException if at least one value parameter 
	 * could not be converted to a short
	 */
	public short[] getShortParameters(String name) throws ParameterNotFoundException, NumberFormatException {

		String[] values = getStringParameters(name);
		short[] ret = new short[values.length];

		for (int i = 0; i < values.length; i++) {
			ret[i] = Short.parseShort(values[i]);
		}

		return ret;
	}

	/**
	 * Gets the named parameter value as an array of short values, with a default.
	 * Returns the default array if the parameter is not found.
	 *
	 * @param name the parameter name
	 * @param def the default array
	 * @return the parameter value as an array of short values
	 */
	public short[] getShortParameters(String name, short[] def) {

		try {
			return getShortParameters(name);
		} catch (Exception e) {
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
	public Hashtable<String, String> getSetParameter(String prefix) {
		Hashtable<String, String> ht = new Hashtable<String, String>();

		Enumeration e = req.getParameterNames();
		while (e.hasMoreElements()) {
			String param = (String) e.nextElement();

			if (!param.startsWith(prefix))
				continue;

			String[] values = req.getParameterValues(param);
			ht.put(param.substring(prefix.length()), values[0]);
		}

		return ht;
	}

	/** Same as {@link #getSetParameter(String) getSetParameter}
	 * but return values as an array of strings.
	 * For an example of using it see 
	 * {@link #exampleForSetParameter(ParameterParser,String) exampleForSetParameter}
	 * @param prefix is the prefix for paramenters names
	 * @return a hash table having as keys the parameters names
	 * after removing the prefix and as values the coresponding
	 * values, as an array of strings.
	 * @see #getSetParameter(String) getSetParameter
	 */
	public Hashtable getSetParameters(String prefix) {
		Hashtable ht = new Hashtable();

		Enumeration e = req.getParameterNames();
		while (e.hasMoreElements()) {
			String param = (String) e.nextElement();

			if (!param.startsWith(prefix))
				continue;

			String[] values = req.getParameterValues(param);
			ht.put(param.substring(prefix.length()), values);
		}

		return ht;
	}

	/** Example how to use
	 * {@link #getSetParameter(String) getSetParameter}
	 * function */
	public static String exampleForSetParameter(ParameterParser pp) {

		Hashtable ht = pp.getSetParameter("p_");
		Enumeration params = ht.keys();
		String ret = "\nExample for ParameterParser.getSetParameter with prefix=p_:";

		ret += buildFormForExample("p_");

		ret += "\n<pre>Result:\n";
		while (params.hasMoreElements()) {
			String name = (String) params.nextElement();
			String value = (String) ht.get(name);
			ret += "\n" + name + "=" + value + "\n";
		}
		return ret + "\n</pre>\n";
	}

	/**     */
	private static String buildFormForExample(String prefix) {
		String ret = "<FORM><TABLE border=0>";
		for (int i = 1; i < 10; i++) {
			ret += "<TR><TD><INPUT type='checkbox' name='"
				+ prefix
				+ "id"
				+ i
				+ "'></TD><TD>"
				+ "Parameter id"
				+ i
				+ "</TD><TD>"
				+ "checkbox name="
				+ prefix
				+ "id"
				+ i
				+ "</TD></TR>";
		}
		ret += "<TR><TD colspan='3'>" + "<INPUT type='submit' name='s' value='Submit'></TD></TR>";

		return ret + "</TABLE></FORM>";
	}

	/**
	 * @see java.lang.Object#toString()
	 */
	public String toString() {
		Enumeration paramNames = req.getParameterNames();
		if (!paramNames.hasMoreElements())
			return "NO PARAMETERS!!";

		String retValue = "";
		while (paramNames.hasMoreElements()) {
			String element = (String) paramNames.nextElement();
			String[] elemenValues = req.getParameterValues(element);

			retValue += element + " = ";
			for (int i = 0; i < elemenValues.length; i++) {
				retValue += elemenValues[i] + ((i < elemenValues.length - 1) ? ", " : "");
			}
			retValue += "\n";
		}

		return retValue;
	}

}