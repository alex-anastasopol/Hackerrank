/*
 * Created on May 6, 2004
 *
 * To change the template for this generated file go to
 * Window>Preferences>Java>Code Generation>Code and Comments
 */
package ro.cst.tsearch.utils;
import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Category;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import ro.cst.tsearch.Search;
import ro.cst.tsearch.bean.SearchAttributes;
import ro.cst.tsearch.extractor.xml.XMLUtils;
import ro.cst.tsearch.servlet.BaseServlet;
/**
 * @author cozmin
 *
 * To change the template for this generated type comment go to
 * Window>Preferences>Java>Code Generation>Code and Comments
 */
public class Configurator {
	
	protected static final Category logger= Category.getInstance(Configurator.class.getName());
	public  static final String PARAM_TESTCASE_NAME="testcasename";
	public  static final String PARAM_TESTCASE_FILE="testcasefile";
	//public static final String properties_path="conf.tasettings";
	public static final String properties_path=BaseServlet.REAL_PATH+"WEB-INF"+File.separator+"classes"+File.separator+"conf"+File.separator+"tasettings.properties";
	public static final String http_properties_path=BaseServlet.REAL_PATH+"WEB-INF"+File.separator+"classes"+File.separator+"conf"+File.separator+"httpsettings.properties";
	private static boolean tarunning=false;
	
	/**
	    ta.hours=7 12 19
		ta.presence_test_interval=10
		ta.timeout_constant=2	 
	 */
	public static Map desc=new HashMap();
	static{
		desc.put("ta.hours","Hours (separated by blanks) when TestCases are run.");
		desc.put("ta.presence_test_interval","Minutes between 2 consecutive presence tests for all Document Servers.");
		desc.put("ta.timeout_constant","Constant multiplier for calculating timeout limit (Tout_limit=ta.timeout_constant x TestCase.time).");		
	}
	
		                  
	
	public static void main(String[] args) {
	}	
	public static Set getTestHours()
	{
		 HashSet s=new HashSet();
		 RealResourceBundle rb=RealResourceBundle.getBundle(getProperties_path());		 
		 String[] h=StringUtils.split(rb.getKey("ta.hours")," ");
		 for(int i=0;i<h.length;i++)
		    s.add(h[i].trim());		 
		 return s;
	}
	public static int getTimeout()
	{
		return Integer.parseInt(RealResourceBundle.getBundle(getProperties_path()).getKey("ta.timeout_constant"));
	}
	public static int getPresenceTestInterval()
	{
		return Integer.parseInt(RealResourceBundle.getBundle(getProperties_path()).getKey("ta.presence_test_interval"));
	}
	/**
	 * @return
	 */
	public static String getProperties_path() {
		return properties_path;
	}

	/**
	 * @return
	 */
	public static boolean isTarunning() {
		return tarunning;
	}

	/**
	 * @param b
	 */
	public static void setTarunning(boolean b) {
		tarunning = b;
	}

	/**
	 * @return
	 */
	public static String getHttp_properties_path() {
		return http_properties_path;
	}

}
