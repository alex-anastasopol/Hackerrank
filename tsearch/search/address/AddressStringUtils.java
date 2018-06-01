package ro.cst.tsearch.search.address;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Vector;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.log4j.Category;
/**
 * Tools for handling address string.
 */
public class AddressStringUtils
{
	protected static final Category logger= Category.getInstance(AddressStringUtils.class.getName());
	
	/**
	 * Split given string into tokens.
	 * Used token delimiters: space and ""
	 * 
	 * @param s String to be splitted.
	 */
	public static String[] splitString(String s)
	{
		 Pattern p=Pattern.compile("^([^\"]*)\"([^\"]*)\"");
		 Matcher m=p.matcher(s);
		 List l=new ArrayList();
		 while (m.find()) {
			 l.addAll(splitBlank(m.group(1)));
			 String s2=m.group(2).trim();
			 if (s2.length()>0)
				 l.add(s2);

			 s=s.substring(m.end());
			 m=p.matcher(s);
		 }
		 l.addAll(splitBlank(s));
		 String[] r=(String[])l.toArray(new String[0]);
		 return r;
	}

	/**
	 * Split string into tokens.
	 * Token delimiters: space.
	 * 
	 * @param s String to be splitted.
	 */
	public static List splitBlank(String s) {
		List l=new ArrayList();
		l.addAll(Arrays.asList(s.split(" +")));
		Iterator it=l.iterator();
		while (it.hasNext()) {
			String s1=(String)it.next();
			if (s1.trim().length()==0) {
				it.remove();
			}
		}
		return l;
	}
	
	/**
	 * Just prints to stdout string array.
	 * @param arr
	 */
	public static void showStringArray(String[] arr)
	{
		for(int i = 0; i < arr.length; i++)
			logger.info(i+" =[" + arr[i] + "]");
	}
	
	/**
	 * Converts given vector into string array.
	 * @param v
	 * @return String array.
	 */	
	public static String[] vector2StringArray(Vector v)
	{
		String [] s = new String[v.size()];
		for(int i = 0; i < v.size(); i++)
			s[i] = (String)v.elementAt(i);
			
		return s;
	}

	/**
	 * Returns "" escaped substrings as string array.
	 * 
	 * @param s Input string.
	 */	
	public static String[] getEscapedStrings(String s) {
		Pattern p=Pattern.compile("\"([^\"]*)\"");
		Matcher m=p.matcher(s);
		List l=new ArrayList();
		while (m.find()) {
			l.add(m.group(1));
		}
		String[] r=(String[])l.toArray(new String[0]);
		return r;		
	}

	/**
	 * Escape input string with "".
	 * 
	 * @param s String to be 'escaped'
	 * 
	 * @param escapedStrings Substrings array.
	 */
	public static String escapeString(String s,String[] escapedStrings) {
			String es = new String(s);
			
			for(int i=0; i < escapedStrings.length; i++) {
				es = es.replaceAll("(" + escapedStrings[i] + ")", "\"$1\"");
			}
			
			return es;
	}

	/**
	 * Test for address emptyness.
	 * Address empty => all 7 tokens are "";
	 * 
	 * param ai
	 * @return
	 */
	public static boolean isEmptyAddress(AddressInterface ai) {
		for(int i = 0; i < 7; i++) {
			String elem = ai.getAddressElement(i); 
			if(elem != null && !elem.equals("")) {
				return false;
			}
		}
		return true;
	}	
}
