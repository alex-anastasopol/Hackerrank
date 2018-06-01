/*
 * Created on Jun 17, 2003
 *
 */
package ro.cst.tsearch.utils;

import java.io.PrintWriter;
import java.io.RandomAccessFile;
import java.io.StringWriter;
import java.io.UnsupportedEncodingException;
import java.math.BigDecimal;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.text.DateFormat;
import java.text.MessageFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Random;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.Vector;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang.StringEscapeUtils;
import org.apache.commons.lang.WordUtils;
import org.apache.log4j.Category;
import org.apache.log4j.Logger;

import ro.cst.tsearch.bean.SearchAttributes;
import ro.cst.tsearch.data.StateCountyManager;
import ro.cst.tsearch.search.filter.matchers.algorithm.MatchAlgorithm;
import ro.cst.tsearch.servers.parentsite.Company;

import com.stewart.ats.base.name.NameFormaterI;
import com.stewart.ats.base.name.NameFormaterI.TitleType;
import com.stewart.ats.base.name.NameI;
import com.stewart.ats.tsrindex.client.ocrelements.VestingInfoI;

public class StringUtils {
	
	
	public static final String newLine = "    <table bgcolor=\"green\">\n      <tr>\n        <td>\n          DO NOT DELETE THIS LINE\n        </td>\n      </tr>\n    </table>";
	public static final String regex = "<table[^>]*bgcolor[^>]*green[^>]*>\\s*<tr>\\s*<td>\\s*DO\\s*NOT\\s*DELETE\\s*THIS\\s*\\s*LINE\\s*</td>\\s*</tr>\\s*</table>";
	public static final String numberLettersRegex = "(\\d+)(\\w+)";
	public static final String etalDerivationsRegex = "(?is)\\bET\\s?(AL|UX|UXOR|VIR)\\b";
	
	public static final Pattern NUMBERS_LETTERS_PATTERN = Pattern.compile(numberLettersRegex);
	public static final Pattern ETAL_DERIVATIONS_PATTERN = Pattern.compile(etalDerivationsRegex);
	
	public static final String EMPTY_STRING ="";
	
	public static final Map<String, Pattern> userAgentVersion = new LinkedHashMap<String, Pattern>();
	static {
		userAgentVersion.put("Firefox", Pattern.compile("\\bFirefox/([0-9\\.]+)\\b"));
		userAgentVersion.put("Opera", Pattern.compile("\\bOpera/([0-9\\.]+)\\b"));
		userAgentVersion.put("Internet Explorer", Pattern.compile("\\bMSIE (\\d+\\.\\d+)\\b"));
		//Mozilla/5.0 (Windows NT 6.1; WOW64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/33.0.1750.5 Safari/537.36
		userAgentVersion.put("Chrome", Pattern.compile("Chrome/([0-9\\.]+)"));
		//Mozilla/5.0 (Windows NT 6.1; WOW64) AppleWebKit/534.57.2 (KHTML, like Gecko) Version/5.1.7 Safari/534.57.2
		userAgentVersion.put("Safari", Pattern.compile("([0-9\\.]+) Safari"));
	}
	
	
	protected static final Category logger = Logger.getLogger(StringUtils.class);
	
	// Task 7691 - the words below should appear in lowercase
	private static String[] lowercaseWords = new String[] {
		"a", "an", "the", "at", "but", "by", "down", "for", "from", "in", "into", "like", "near", "of", 
		"off", "on", "onto", "out", "over", "past", "till", "to", "up", "upon", "with", "about", 
		"above", "across", "after", "against", "along", "among", "around", "before", 
		"behind", "below", "beneath", "beside", "between", "beyond", "despite", "down", "during", 
		"except", "inside", "outside", "over", "past", "since", "through", "throughout", "toward", 
		"under", "underneath", "until", "within", "without", "and", "but", "or", "nor", "for", "so", "yet", 
		"husband", "wife"
	};

	private static String[] stateAbbrev = new String[] {
		"AL", "AK", "AZ", "AR", "CA", "CO", "CT", "DE", "DC", "FL", "GA", "HI", "ID", "IL", "IN", 
		"IA", "KS", "KY", "LA", "ME", "MD", "MA", "MI", "MN", "MS", "MO", "MT", "NE", "NV", "NH", 
		"NJ", "NM", "NY", "NC", "ND", "OH", "OK", "OR", "PA", "RI", "SC", "SD", "TN", "TX", "UT", 
		"VT", "VA", "WA", "WV", "WI", "WY"
	};	
	
	public static Vector<String> cloneVector(Vector<String> toClone) {
		if (toClone != null) {
			int size = toClone.size();
			Vector<String> ret = new Vector<String>(size);
			for (int i = 0; i < size; i++) {
				ret.add(toClone.get(i));
			}
			return ret;
		} else {
			return new Vector<String>();
		}
	}
	
	 public static String getStringIdentifiedByRegEx(String regEx, String input){
		 Pattern pattern = Pattern.compile(regEx);
		 Matcher matcher = pattern.matcher(input);
		 String returnValue = "";
		 
		 if(matcher.find()){
			 returnValue = matcher.group();
		 }
		 
		return returnValue;
	 }
	
	 public static  boolean toFile(String name,String data){
		 RandomAccessFile rand=null;
		 boolean success = true;
		 try{
	        rand=new RandomAccessFile (name,"rw");
	        rand.setLength(0);rand.seek(0);
	        rand.write(data .getBytes());
	        rand.close();
		 }catch(Exception v){
	       v.printStackTrace();
	       success = false;
	     }
         finally{
        	try{rand.close();}
        	catch(Exception e){}
         }
	    return success;
	 }
	 
	 public static String toUpperAndTrim(String str){
		 	if (str == null){
		 		return "";
		 	}
			return str.toUpperCase().trim();
	 }
	 
	 /**
	  * Access a file and returns content
	  * @param name
	  * @param accessMode
	  * @return String
	  */
	public static String fileToString(String name, String accessMode){
		
		 byte []b = null;
		 RandomAccessFile rand = null;
		 try{
		        rand=new RandomAccessFile (name,accessMode.toLowerCase());
		        b = new byte[(int)rand.length()];
		        rand.readFully(b);
		        rand.close();
	        }
	        catch(Exception v){
	        	v.printStackTrace();
	        }
	        finally{
	        	try{
	        		rand.close();
	        	}
	        	catch(Exception e){
	        		
	        	}
	        }
	     return new String(b);
		
	}
	
	/**
	 * Access a file and returns content.Also write file on disk if not exists
	 * @param name
	 * @return String content
	 */
    public static  String  fileToString(String name){
    	
     return fileToString(name,"rw");	
	 }
	
    
    public static String fileReadToString(String name){
     return fileToString(name,"r");
    }
    
	public static Object oraEscape(Object inStr) {
		String retStr;
		try {
			retStr = inStr.
					toString().
					replaceAll("'", "''");
		} catch (Exception e) {
			return inStr;
		}
		return retStr;
	}

	public static String selectiveHTMLEntityEncode(String html, char[] replacements){
		for(int i=0; i<replacements.length; i++){
			char c = replacements[i];
			html = html.replaceAll(Character.toString(c), "&#" + (int)c + ";");
		}
		return html;
	}
	
	public static String HTMLEntityEncode( Object inStr ) {
		if (inStr==null) return "";
		String input = inStr.toString();
        StringBuffer sb = new StringBuffer();
        for ( int i = 0; i < input.length(); ++i ) {
                char ch = input.charAt( i );
                if ( ch>='a' && ch<='z' || ch>='A' && ch<='Z' || ch>='0' && ch<='9' || ch==' ' ) {
                        sb.append( ch );
                } else {
                        sb.append( "&#" + (int)ch + ";" );
                }
        }
        return sb.toString();
	}
	
	//compare two time-strings or date-strings in the same format 
	public static boolean isTimeAfter(String str1, String str2){
    	
    	if((str1.contains("AM")||str1.contains("am"))&&(str2.contains("PM")||str2.contains("pm"))){
    		return false;
    	}
    	if((str2.contains("AM")||str2.contains("am"))&&(str1.contains("PM")||str1.contains("pm"))){
    		return true;
    	}
    	
    	return str1.compareTo(str2)>0;
	
    }
	
	public static boolean isStringBlank(Object str) {
		String currentStr  = "";
		try {
			currentStr = (String) str;
			return isStringBlank(currentStr);
		} catch (Exception e) {};
		return true;
	}
	
	public static boolean isStringBlank(String str) {
		if (str == null){
			return true;
		}
		int len = str.length();
		for (int i = 0; i < len; i++) {
			if (!Character.isWhitespace(str.charAt(i)))
				return false;
		}
		return true;
	}
	public static boolean isStringInvalid(String str) 
	{
		if (str == null)
		{
				return true;
		}
			int len = str.length();
			for (int i = 0; i < len; i++) 
			{
				if (!Character.isDigit(str.charAt(i)))
					return true;
			}
			
			return false;
		}
	
	public static List<String> splitString (String str){
		if ((str != null) && (!str.equals(""))){
			str = str.trim();
			String[] tokens = str.split("\\s+");
			//logger.debug( " str= [" + str + "] tokens = " + Arrays.asList(tokens));
			return Arrays.asList(tokens);
		}else{
			return Arrays.asList(new String[]{""});
		}
	}

	public static String join(List l, String delimiter){
		if (l == null) {
			return "";
		}
		StringBuffer s = new StringBuffer();
		for (Iterator iter = l.iterator(); iter.hasNext();) {
			String element = (String) iter.next();
			s.append(element);
			if ((!element.equals("")) && iter.hasNext()){  
				s.append(delimiter);	
			}
		}		
		return s.toString();
	}
	
	public static String join(String [] l, String delimiter){
		if (l == null) {
			return "";
		}
		return join(Arrays.asList(l), delimiter);
	}

	/**
	 * prepare a string (usually from a form input) in order to be
	 * printed in HTML format; is replacing quotes with escape sequences
	 * and removes white spaces from both ends
	 */
	public static String prepareStringFromHTML(String s) {
		
        if (s == null || s.equals(""))
			return "";

		s = s.trim();
		s = StringUtils.replaceString(s, "&", "&amp;");
		s = StringUtils.replaceString(s, "<", "&lt;");
		s = StringUtils.replaceString(s, ">", "&gt;");
		s = StringUtils.replaceString(s, "'", "&acute;");
		s = StringUtils.replaceString(s, "\"", "&quot;");
		//Log.debug(StringUtils.class, " inainte de prelucrare" + s);
		s = StringUtils.replaceString(s, "\r\n", "<br>");
		s = StringUtils.replaceString(s, "\n\r", "<br>");
		s = StringUtils.replaceString(s, "\r", "<br>");
		s = StringUtils.replaceString(s, "\n", "<br>");
		//logger.debug(" inainte de prelucrare" + s);
		s = StringUtils.replaceString(s, " ", "&nbsp;");
		//logger.debug( " dupa de prelucrare" + s);
		return s;
	}

	/**
	 * process the given string in order to replace all the escape sequences inserted with html 
	 * printing compatibilities and removes white spaces from both ends
	 */
	public static String prepareStringForHTML(String s) {
		if(s==null)
			return "";
		if (s.equals(""))
			return s;
		s = s.trim();
		s = StringUtils.replaceString(s, "&acute;", "'");
		s = StringUtils.replaceString(s, "&apos;", "'");
		s = StringUtils.replaceString(s, "&#39;","'");
		s = StringUtils.replaceString(s, "&quot;", "\"");
		s = StringUtils.replaceString(s, "<br>", "\n");
		s = StringUtils.replaceString(s, "<BR>", "\n");
		s = StringUtils.replaceString(s, "<P>", "");
		s = StringUtils.replaceString(s, "</P>", "");
		
		s = StringUtils.replaceString(s, "<div>", "\n");
		s = StringUtils.replaceString(s, "</div>", "");
		s = StringUtils.replaceString(s, "&lt;", "<");
		s = StringUtils.replaceString(s, "&gt;", ">");
		s = StringUtils.replaceString(s, "&nbsp;", " ");
		s = StringUtils.replaceString(s, "&amp;", "&");		
		return s;
	}

	
	public static String prepareStringForTextboxFromHtml(String s) {
		if (s.equals(""))
			return s;

		String tmp = prepareStringFromHTML(s);
		tmp = StringUtils.replaceString(tmp, "\"", "&quot;");
		return tmp;
	}

	public static String prepareStringForTextboxNotFromHtml(String s) {
		if (s.equals(""))
			return s;

		String tmp = StringUtils.replaceString(s, "\"", "&quot;");
		return tmp;
	}
    /**
	 * Gets a string like that : "	<option value=" ">Please select</option>
	 * <option value="ACRS">ACRES</option> <option value="ALY">ALLEY</option>"
	 * and returns this:  
	 * @param htmlOptionList
	 * @return
	 */
	public static String buildParentSiteComboBoxValuesFromHtml(String htmlOptionList) {
		//clean from commas 
		List<String> matches = RegExUtils.getMatches("<option value=\"(.*?)\">(.*?)</option>", htmlOptionList, 2);
		for (String value : matches) {
			if (value.contains(",")){
				String newValue = value.replaceAll(","," ");
				htmlOptionList = htmlOptionList.replaceAll(value, newValue);
			}
		}
		
		
		htmlOptionList = htmlOptionList.replaceAll("(?is)<option>(.*?)</option>", "name=$1_value=$1,");
		htmlOptionList = htmlOptionList.replaceAll("(?is)<option value=\"", "name=");
		htmlOptionList = htmlOptionList.replaceAll("(?is)\">", "_value=");
		htmlOptionList = htmlOptionList.replaceAll("(?is)</option>", ",");
		return htmlOptionList;
	}
	
    public static String prepareStringForTextbox(String s) {
        if (s.equals(""))
            return s;

        String tmp;
        tmp = HTMLEntityEncode(s).toString();
        
        tmp = s.replaceAll("\"","\\\\\"");
        tmp = tmp.replaceAll( "\\s", " " );

        
        return tmp;
    }

	public static String prepareStringForAlert(String s) {
		if (s.equals(""))
			return s;

		s = StringUtils.replaceString(s, "\"", "\\\"");
		s = StringUtils.replaceString(s, "\'", "\\'");
		s = StringUtils.replaceString(s, "\n", "\\n");
		return s;
	}

	/**
	 * Replaces, starting from 0 index, in the <pre>scope</pre> string
	 * all the <pre>lookFor</pre> sequences with the <pre>substitute</pre> string
	 * @returns the <pre>scope</pre> string modified with <pre>substitute</pre> string
	 * @param - <pre>scope</pre> the string to be modified by replacing
	 * @param - <pre>lookFor</pre> the string that must be replaced
	 * @param - <pre>substitute</pre> the string that has to placed in stead if <pre>lookFor</pre>
	 */
	public static String replaceString(String scope, String lookFor, String substitute) {

		
		return replaceString(scope, lookFor, substitute, 0);
	}

	/**
	 * Replaces, starting from <pre>startWith</pre> index, in the <pre>scope</pre> string
	 * all the <pre>lookFor</pre> sequences with the <pre>substitute</pre> string
	 * @returns the <pre>scope</pre> string modified with <pre>substitute</pre> string
	 * @param - <pre>scope</pre> the string to be modified by replacing
	 * @param - <pre>lookFor</pre> the string that must be replaced
	 * @param - <pre>substitute</pre> the string that has to placed in stead if <pre>lookFor</pre>
	 */
	public static String replaceString(String scope, String lookFor, String substitute, int startWith) {
		
		//logger.debug("replace [" + lookFor + "] with [" + substitute + "] in [" + scope);

		String ret = "";
		String right = null;
		try {
			int index = scope.indexOf(lookFor, startWith);
			//logger.debug( " index " + index);
			if (index >= 0) {
				ret = scope.substring(0, index) + substitute;
				right = scope.substring(index + lookFor.length());
				//logger.debug( " ret" + ret);
				//logger.debug( " right" + right);
			} else {
				ret = scope;
			}
			if (right != null && !right.equals(""))
				ret += replaceString(right, lookFor, substitute);
		} catch (NullPointerException eNP) { 
			logger.error("Error", eNP);
		}
		return ret;
	}

	/*
	public static String substituteSubstrings(String string, String[] substrings, String replacement) {
		logger.debug("string before substitution" + string);
		for (int i = 0; i < substrings.length; i++) {
			String substring = substrings[i];
			string = getPatternForSeparateToken(substring).matcher(string).replaceAll(replacement);
			logger.debug("string after " + substring + " substitution" + string);
		}
		logger.debug("string after substitution" + string);
		return string;
	}*/

	public static Pattern getPatternForSeparateToken(String str){
		return Pattern.compile("(\\A|.*\\W+)\\Q" + str +	"\\E(\\W+.*|\\z)", Pattern.CASE_INSENSITIVE);
	}

	public static List getAllWords(String string) {
		return getAllPatterns(patternForWord, string);
	}

	public static List getAllPatterns(Pattern pattern, String string) {
		List l = new ArrayList();
		Matcher m =  pattern.matcher(string);
		while (m.find()){
			l.add(m.group());
		}
		//logger.debug("words in [" + string + "] are " + l);
		return l;
	}

	public static List getAllPatterns(String pattern, String string) {
		List l = new ArrayList();
		Matcher m =  Pattern.compile(pattern).matcher(string);
		while (m.find()){
			l.add(m.group());
		}
		//logger.debug("words in [" + string + "] are " + l);
		return l;
	}



	/*		boolean rez = true;
			if ((!StringUtils.isStringBlank(spouseFName))){
				if ((StringUtils.containsIgnoreCase(name, spouseFName))){
					return true;
				}else{
					rez = false;
				}
			}
			if ((!StringUtils.isStringBlank(spouseMName))){
				if ((StringUtils.containsIgnoreCase(name, spouseMName))){
					return true;
				}else{
					rez = false;
				}
			}
			if ((!StringUtils.isStringBlank(spouseLName))&&(!ownerLName.equalsIgnoreCase(spouseLName))){
				if ((StringUtils.containsIgnoreCase(name, spouseLName))){
					return true;
				}else{
					rez = false;
				}
			}
			logger.debug ("matchesSpouse =  " + rez);
			return rez;
	
	
		private static String getName(String row){
			return getTextBetweenTags(2, "<CENTER>", "</CENTER>" , row);
		}
	*/
	
		public static List filterIgnoreCaseStringList(List l, String[] criterias) {
			List rez = new ArrayList(l);
	
			for (int i = 0; i < criterias.length; i++) {
				rez = StringUtils.filterIgnoreCaseStringList(rez, criterias[i]); 
			}		
	
			return rez;	
		}

	public static List filterIgnoreCaseStringList(List l, String criteria) {
		List rez = new ArrayList(l);
	
		for (Iterator iter = rez.iterator(); iter.hasNext();) {
			if (criteria.equalsIgnoreCase((String) iter.next())){
				iter.remove();
			}
		}
	
		return rez;	
	}

	public static boolean hasDigits( String string) {
		Matcher m =  Pattern.compile("\\d+").matcher(string);
		return m.find();
	}

	public static boolean hasAlpha( String string) {
			//Matcher m =  Pattern.compile("\\[a-zA-Z]+[0-9]+").matcher(string);
			//return m.find();
			for (int i = 0 ; i < string.length(); i++)
				if (Character.isLetter(string.charAt(i)) || string.length() > 5) // si daca depaseste lungimea book/page
				return true;
			return false;
		}
	
	static Pattern patternForWord = Pattern.compile("\\w+", Pattern.CASE_INSENSITIVE);

	public static List splitAfterDelimitersList(String input, String[] delimits) {
		List l = new ArrayList();
		l.add(input);
		
		for (int i = 0; i < delimits.length; i++) {
			l = splitAfterDelimiter(l,delimits[i]);
		}

		return l;
	}

	public static List splitAfterDelimiter(List l, String delimiter) {
		List rez = new ArrayList();
		
		for (Iterator iter = l.iterator(); iter.hasNext();) {
			String s = (String) iter.next();
			if (s != null)
			    rez.addAll(Arrays.asList(s.split(delimiter)));
		}
		
		return rez;
	}
	
	/**
	 * Split words from target longer then maxLength, inserting a space after the characters from delimiters
	 */
	public static String insertSpaceToSplit(String target, String delimiters, int maxLength){
		String [] words = target.split(" ");
		String newtarget = "";
		for (int j = 0; j< words.length; j++ ){
			if (words[j].length() > maxLength){
				String uniqueDelimiters = "";
				for (int i = 0; i<delimiters.length(); i++){
					char delimiter = delimiters.charAt(i);
					if (uniqueDelimiters.indexOf(delimiter) == -1){
						words[j] = words[j].replaceAll("" + delimiter, delimiter + " ");
						uniqueDelimiters += delimiters.charAt(i);
					}
				}
			}
			newtarget += words[j] + (j == words.length -1?"":" ");
		}
		return newtarget;
	}
	
	public static String removeTrailingSubstring(String string, String substring) {
		if (string.endsWith(substring)){
			string = string.substring(0, string.length()-substring.length());
		}
		return string;
	}

	public static List removeTrailingSubstring(List l, String substring) {
		List rez = new ArrayList();
		
		for (Iterator iter = l.iterator(); iter.hasNext();) {
			String string = (String) iter.next();
			rez.add(removeTrailingSubstring(string, substring));
		}
		
		return rez;
	}
	
	public static String takeOffBlankSpaces (String s){
		return takeOffPattern ("\\s", s);
	}

	public static String takeOffPattern (String pattern, String s){
		if (s==null) return s;
		else return s.replaceAll(pattern,"");
	}


	public static boolean flexibleEqualsIgnoreCaseAndBlank(String s1, String s2,long searchId){
		//logger.info("comparing " +	s1 + " with " +	s2 + " = ");
		
		String s1Orig = new String(s1);
		String s2Orig = new String(s2);
		
		if (isStringBlank(s1)){
			return true;
		}
		s1 = s1.replaceAll("\\s", "");
		
		
		if (isStringBlank(s2)){
			return true;
		}
		s2 = s2.replaceAll("\\s", "");

		s1 = s1.toUpperCase();
        s1 = MatchEquivalents.getInstance(searchId).getEquivalent( s1 );
        
		s2 = s2.toUpperCase();
        s2 = MatchEquivalents.getInstance(searchId).getEquivalent( s2 );
        
		boolean rez = s1.equals(s2) 
			|| (s1.indexOf(s2)>-1) 
			|| (s2.indexOf(s1)>-1);
		
		if(!rez) {
			BigDecimal score = MatchAlgorithm
								.getInstance(MatchAlgorithm.TYPE_IGNORE_DIFF_NO_TOKENS,s1Orig,s2Orig,searchId)
								.getScore(); 
			rez = (score.compareTo(new BigDecimal("0.70"))> 0 ? true : false);
		}
		 
		//logger.info(rez);
		return rez;
	}
	
	public static String truncString(String s, int maxNoChars){
		if ((s!=null)&&(s.length()> maxNoChars)){
			s = s.substring(0, maxNoChars-3) + "...";
		}
		return s;
	}

	public static boolean compareMultipleInts(String s1, String s2)
		{
			if (!hasDigits(s1) || !hasDigits(s2)){
				return true;
			}

			List l1 = StringUtils.getAllPatterns("\\d+", s1);
			s1 = StringUtils.join(l1,"");
			l1.add(s1);
						
			List l2 = StringUtils.getAllPatterns("\\d+", s2);
			s2 = StringUtils.join(l2,"");
			l2.add(s2);
			
			for (Iterator iter1 = l1.iterator(); iter1.hasNext();) {
				String e1 = (String) iter1.next();
				for (Iterator iter2 = l2.iterator(); iter2.hasNext();) {
					String e2 = (String) iter2.next();
					if (compareInts(e1, e2)){
						return true; 
					}
				}
			}

			return false;
		}

		public static boolean compareInts(String s1, String s2) {
			int i1;
			int i2;
			try
			{
				i1= Integer.parseInt(s1);
				i2=Integer.parseInt(s2);
			}
			catch (NumberFormatException e)
			{	
				return false;
			}
			return (i1==i2);
		}

		public static List getAllIntegers(String s){
			List rez = new ArrayList();
			
			List tokens = new ArrayList(StringUtils.getAllPatterns("\\d+", s));
			if (tokens.size()>1){
				String all = StringUtils.join(tokens,"");
				tokens.add(all);
			}
			
			for (Iterator iter = tokens.iterator(); iter.hasNext();) {
				String token = (String) iter.next();
				int i;
				try	{
					i= Integer.parseInt(token);
				}catch (NumberFormatException e){
					continue;	
				}
				rez.add(""+i);
			}
			return rez;
		}

		public static String capitalize(String s) {
			if ((s == null) || (s.length() == 0)){
				return s;
			}
			String rez = s.substring(0, 1).toUpperCase();
			rez += s.substring(1, s.length()).toLowerCase();
			return rez;
		}

		public static String getTextBetweenDelimiters(String beginDelim, String endDelim , String s){
			return getTextBetweenDelimiters(0, beginDelim, endDelim , s);
		}
		
		public static String getTextBetweenDelimiters(int beginDelimSkipNo, String beginDelim, String endDelim , String s){
			String rez = "";
		
			int idx1 = -1;
			for (int i=0 ; i<beginDelimSkipNo+1; i++){ //skipping + find the next beginDelim
				idx1= s.indexOf(beginDelim, idx1 + 1);
			}
			
			if (idx1>-1) {
				idx1 = idx1+beginDelim.length();
				int idx2 = s.indexOf(endDelim, idx1);
				if (idx2==-1){
					idx2 = s.length();
				}
				rez = s.substring(idx1, idx2);
			}		
			
			return rez;
		}

		public static String replaceFirstSubstring(String source, String replacement, String replacer){
			String rez = source;
			if (isStringBlank(replacement) || isStringBlank(source)){
				return rez;
			}

			int idx= source.indexOf(replacement);
			if (idx>-1) { 
				rez = source.substring(0, idx);
				rez += replacer;
				rez += source.substring(idx + replacement.length());
			}
			
			return rez;
		}
		
		public static String replaceFirstBetweenTags(String source, String tag1,String tag2, String replacer ){
			String rez = source;

			int idx= source.indexOf(tag1) ;
			if (idx>-1) { 
				idx += tag1.length();
				rez = source.substring(0, idx);
				rez += replacer;
				int idx2 = source.indexOf(tag2, idx);
				if (idx2>-1){
					rez += source.substring(idx2);
				}
			}
			
			return rez;
		}

		public static String transformNull(String value){
			return (value!=null?value:"");
		}
		
		public static String transformNullForHTML(String value){
			return value == null || "".equals(value.trim()) ? "&nbsp;" : value;
		}
		
		
		public static  List parseIntegers(String s, String[] delimits){
			List l = StringUtils.splitAfterDelimitersList(s, delimits);
			List rez = new ArrayList();
			for (Iterator iter = l.iterator(); iter.hasNext();) {
				String token = (String) iter.next();
				try	{
					int i= Integer.parseInt(token);
					token = ""+i;
				}catch (NumberFormatException e){}
				if(!StringUtils.isStringBlank(token)){
					rez.add(token);
				}
			}
			return rez;
		}

		public static List<String> parseLotInterval(String s, String[] delimits) {
			List l = StringUtils.splitAfterDelimitersList(s, delimits);
			
			List<String> rez = new ArrayList<String>();
			
			if( l.size() == 1 ) {
				rez.add( (String)l.get(0) );
			} else {
				String tokenStart = (String) l.get(0);
				String tokenEnd = (String) l.get(1);
				
				int tokenStartInt = -1;
				int startInt = -1;
				String startLetter = null;
				Matcher matcher = null;
				try {
					
					tokenStartInt = Integer.parseInt( tokenStart );
				} catch( Exception e ) {
					//e.printStackTrace();
					try {
						matcher = NUMBERS_LETTERS_PATTERN.matcher(tokenStart);
						if(matcher.find()){
							if(matcher.group(2).length()==1){	//just one letter
								startInt = Integer.parseInt(matcher.group(1));
								startLetter = matcher.group(2).toUpperCase();
							}
						}
					} catch (Exception ex) {
					}
				}
				
				int tokenEndInt = -1;
				int endInt = -1;
				String endLetter = null;
				try {
					tokenEndInt = Integer.parseInt( tokenEnd );
				} catch (Exception e) {
					//e.printStackTrace();
					try {
						matcher = NUMBERS_LETTERS_PATTERN.matcher(tokenEnd);
						if(matcher.find()){
							if(matcher.group(2).length()==1){	//just one letter
								endInt = Integer.parseInt(matcher.group(1));
								endLetter = matcher.group(2).toUpperCase();
							}
						}
					} catch (Exception ex) {
					}
				}
				
				if( tokenStartInt != -1 && tokenEndInt != -1 ) {
					//both are numbers
					for(int i = tokenStartInt ; i <= tokenEndInt ; i ++) {
						rez.add( String.valueOf( i ) );	
					}
				} else {
					if ( tokenStartInt == -1 && tokenEndInt == -1 ) {
						//both contains more than numbers
						if( startLetter!=null && endLetter!=null && (startInt == endInt) ){
							char startChar = startLetter.charAt(0);
							char endChar = endLetter.charAt(0);
							if(startChar > endChar){
								char temp = startChar;
								startChar = endChar;
								endChar = temp;
							}
							for (char i = startChar; i <= endChar; i++){
								rez.add( startInt + String.valueOf(i));
							}
						} 
					}
				}
				if(rez.size()==0){
					//add both whatever the consequences
					rez.add( tokenStart );
					rez.add( tokenEnd );
				}
			}

			return rez;			
		}
		/**
		 * Gets the index of every match encountered in the searchString.
		 * E.g. For stringToLookFor "abc" to look for encounters in "a abc abcdar in abc" will return [2,6,16]  
		 * @param sourceString
		 * @param stringToLookFor
		 * @return
		 */
		public static int[] getMatches(String sourceString, String stringToLookFor){
			int countMatches = org.apache.commons.lang.StringUtils.countMatches(sourceString, stringToLookFor);
			int [] matches = new int[countMatches];
			String tempString=sourceString;
			int i=0;
			int currentPos=0;//the position in original context
			while (countMatches>0){
				//find first encounter
				int encounterIndex = tempString.indexOf(stringToLookFor);
				currentPos = sourceString.length() - tempString.length() + encounterIndex;
				matches[i]=currentPos;
				//move to next encounter
				int offset = tempString.indexOf(stringToLookFor)+stringToLookFor.length();
				tempString = tempString.substring(offset);
//				currentPos = tempString.indexOf(searchString) + currentPos+(str.length()-tempString.length());
//				matches[i]=currentPos;
				i++;countMatches--;
			}
			return matches;
		}
		
		public static boolean matchPatterns(List patterns, String cand) {
			   for (int i = 0; i < patterns.size(); i++) {
				   if (matchesValue((Pattern) patterns.get(i), cand)){
					   return true;
					   //logger.debug("Invalid pattern name" );
				   }
			   }
			   return false;
		   }

	   public static boolean matchesValue(Pattern p, String cand) {
			 return (p.matcher(cand).matches());
		 }

	   
	   public static String doubleQuotes(String s) {
	       if (s == null) return s;
	       
	    	int newIndex=0;
	        while ((newIndex=s.indexOf ('\'', newIndex))>=0) {
	           s=s.substring(0, newIndex)+"''"+s.substring(newIndex+1);
	           newIndex+=2;
	        }	        
	        return s;
	    }
       
       
	/**
	 * Takes <b>valid(checked in search page)</b> owner names and creates a string using the given format
	 * @param sa the SearchAttributes containing owner names
	 * @param format the format used (combination of &lt;P>&lt;L>&lt;M>&lt;F>&lt;S>&lt;Li>&lt;Mi>&lt;Fi>)
	 * @param nameIndex the index of the name to be return or -1 for all names
	 * @return a representation of checked names or empty if none available
	 */
	public static String getOwnersAsString(SearchAttributes sa, String format, int nameIndex) {
		String compose = "";
		String name = "";
		int currentNameIndex = 0;
		for (NameI element : sa.getOwners().getNames()) {
			if (!element.isValidated()) {
				continue;
			}
			name = getNameAsString(element, format);
			if(currentNameIndex == nameIndex) {
				return name.trim();
			}
			currentNameIndex++;
			if(compose.length() > 0) {
				compose += " , ";
			} 
			compose += name;
		}
		//if I had a specified index and could not find it, return empty string
		if(nameIndex > -1) {
			return "";
		}
		return compose.trim();
	}
	
	/**
	 * Takes <b>valid(checked in search page)</b> buyer names and creates a string using the given format
	 * @param sa the SearchAttributes containing buyer names
	 * @param format the format used (combination of &lt;P>&lt;L>&lt;M>&lt;F>&lt;S>&lt;Li>&lt;Mi>&lt;Fi>)
	 * @param nameIndex the index of the name to be return or -1 for all names
	 * @return a representation of checked names or empty if none available
	 */
	public static String getBuyersAsString(SearchAttributes sa, String format, int nameIndex) {
		String compose = "";
		String name = "";
		int currentNameIndex = 0;
		for (NameI element : sa.getBuyers().getNames()) {
			if (!element.isValidated()) {
				continue;
			}
			name = getNameAsString(element, format);
			if(currentNameIndex == nameIndex) {
				return name.trim();
			}
			currentNameIndex ++;
			if(compose.length() > 0) {
				compose += " , ";
			} 
			compose += name;
		}
		//if I had a specified index and could not find it, return empty string
		if(nameIndex > -1) {
			return "";
		}
		return compose.trim();
	}	
	
	/**
	 * Returns a string representation of the given name using the given format
	 * @param element the name to be converted
	 * @param format the format used (combination of &lt;P>&lt;L>&lt;M>&lt;F>&lt;S>&lt;Li>&lt;Mi>&lt;Fi>)
	 * @return a string representation of the given name using the given format or empty if none
	 */
	private static String getNameAsString(NameI element, String format) {
		if(element == null || format == null){
			return "";
		}
		String name = format;
		name = name.replaceAll("<P>", ""); // nu avem suport pentru prefix
		name = name.replaceAll("<L>", element.getLastName());
		name = name.replaceAll("<M>", element.getMiddleName());
		name = name.replaceAll("<F>", element.getFirstName());
		name = name.replaceAll("<S>", ""); // nu exista suport pentru sufix
		String li = element.getLastName();
		li = li.length() >= 1 ? li.substring(0, 1) : "";
		
		name = name.replaceAll("<Li>", li);
		name = name.replaceAll("<Mi>", element.getMiddleInitial());
		name = name.replaceAll("<Fi>", element.getFirstInitial());
		return name;
	}
	   
	   /**
		 * intoarce indexul tagului "<i>inchidere tabela</i>" 
		 * pentru o tabela care contine la randul ei alte tabele
		 * 
		 * @param result
		 * @param istart
		 * @return
		 */
		public static int HTMLGetTableCloseTagIndex(String result, int istart) {
		    
		    String text = result.toLowerCase();
            int returnValue = istart;
		    /*
            while( text.indexOf("</table", returnValue) >  text.indexOf("<table", returnValue) )
            {
                returnValue = text.indexOf("</table", returnValue) + 8;
            }
            
            return returnValue;
            */
            
            int count = 1;
            int startTable;
            int endTable = -1;
            
            while( count > 0 && returnValue < text.length() )
            {
                startTable = text.indexOf( "<table", istart );
                endTable = text.indexOf( "</table>", istart );
                
                if( startTable != -1 && endTable != -1 )
                {
                    istart = startTable > endTable ? ( endTable + 8 ) : ( startTable + 6 );
                }
                else if( startTable == -1 && endTable != -1 )
                {
                    istart = endTable + 8;
                }
                else if( startTable != -1 && endTable == -1 )
                {
                    istart = -1;
                }
                
                if (endTable != -1)
                {
                    count = count + (startTable > endTable ? 1 : -1);
                }
                else
                    return -1;
            }
            
            return endTable + 8;
            
            /*
			if ( text.indexOf("</table", istart) >  text.indexOf("<table", istart)) {
				return HTMLGetTableCloseTagIndex(result, text.indexOf("</table", istart) + 8);
	        } else {
	        	return text.indexOf("</table", istart) + 8;
	        }*/
		}
        
        /**
         * Function alters originalString by using delimiter to delimit all occurences of stringToDelimit that
         * are not sepparated on the left and on the right using delimiter
         * 
         * @param originalString - the string to be altered
         * @param stringToDelimit - the substring in originalString that will be delimited using delimiter
         * @param delimiter - the delimiter used to delimit stringToDelimit in originalString
         * @return the altered string
         */
        public static String addDelimiterToStr( String originalString, String stringToDelimit, String delimiter )
        {
            int previous = 0;
            int current = originalString.indexOf( stringToDelimit );
            
            //for all occurences of stringToDelimit
            while( current >= 0 )
            {
                String before = originalString.substring( previous, current );
                String after = originalString.substring( current + stringToDelimit.length() );
                
                if( before.lastIndexOf( delimiter ) != ( before.length() - delimiter.length() ) )
                {
                    //delimiter not found at the end of before
                    //add delimiter to before...
                    before += delimiter;
                }
                
                if( after.indexOf( delimiter ) != 0 )
                {
                    //delimiter not found at the beginning of after
                    //add delimiter to after
                    
                    after = delimiter + after;
                }
                
                //concatenate the strings to rebuild the original string
                originalString = before + stringToDelimit + after;
                
                //advance
                previous = before.length() + stringToDelimit.length();
                current = originalString.indexOf( stringToDelimit, previous );
            }
            
            return originalString;
        }
        
        public static String replaceWithIfEmpty(String str,char c){
    		String ret=str;
    		if(ret!=null){
    			if(StringUtils.isStringBlank(str)){
    				ret=""+c;
    			}
    		}
    		else {
    			ret=""+c;;
    		}
    		return ret;
    	}
       
        /**
         * This function performs a java.lang.String.replaceAll(String, String) - like operation,
         * only that it first replaces a word with a character, so that a construction like .*?KUK
         * can be written as [^@]* with "KUK" being replaced with "@" first, then back
         * A more complex example:
         *     initial:   rsResponse = rsResponse.replaceAll("(?s)<\\/td><td>(.*?)<\\/td><tr\\s{0,1}>", "$1<td>SN</td></tr><tr>");         	
	     *     optimized: rsResponse = StringUtils.replaceAllSpecial(rsResponse, "(?s)<\\td><td>([^<\\td>]*)<\\td><tr\\s?>", "<\\td><td>$1<\\td><td>SN</td></tr><tr>", "<\\td>", "@");
	     * The initial version took 500,000,000 usec, the modified one 70 usec.
         * @param string the initial string
         * @param regex the regex
         * @param replacement the replacement string
         * @param word the word to be "escaped"
         * @param chr the charcter used for escaping - can be anything, but something less ususal, like @ would do fine
         * @return the modified string
         */
        public static String replaceAllSpecial(String string, String regex, String replacement, String word, String chr){
        	String unprobableString = "__KIKI_RIKI_BUBA__";
        	string = string.replace(chr , unprobableString);    
        	string = string.replace(word, chr);                 
        	regex = regex.replace(chr, unprobableString);
        	regex = regex.replace(word, chr);                   
        	replacement = replacement.replace(chr, unprobableString);
        	replacement = replacement.replace(word, chr);        
        	string = string.replaceAll(regex, replacement);       
        	string = string.replace(chr, word);                   
        	string = string.replace(unprobableString, chr);      
        	return string;
        }
        
        public static String deleteEmptyStringAfter(String str,char c){
    		int poz = str.lastIndexOf(c);
    		String after="";
    		try{
    			after=str.substring(poz +1);
    		}
    		catch(Exception e){
    		}
    		if(StringUtils.isStringBlank(after)){
    			str = str.substring(0,poz);
    		}		
    		return str;
    	}
        
        /**
         * 
         * @param e
         * @return
         */
        public static String exception2String(Exception e){	        
	        StringWriter sw = new StringWriter();
	        PrintWriter pw = new PrintWriter(sw);
		    try{
		    	e.printStackTrace(pw);
		    	return sw.toString();
		    }finally{
		    	pw.close();
		    }
		 }
        
        /**
         * Eliminates part from a string which is delimited by begin and end strings
         * @param str
         * @param begin
         * @param end
         * @return
         */
        public static String eliminatePart(String str, String begin, String end){
        	int startIdx = str.indexOf(begin);
        	if(startIdx != -1){
        		int endIdx = str.indexOf(end, startIdx);
        		if(endIdx != -1){
        			endIdx += end.length();
        			str = str.substring(0, startIdx) + str.substring(endIdx);
        		}
        	}
        	return str;
        }
        
        /**
         * Searches for an occurence of insideStr, then removes the part starting with begin
         * containing insideStr and ending with end. Useful for example for removing a row from
         * a table when the row contains a certain keyword
         * @param str
         * @param insideStr
         * @param begin
         * @param end
         * @return
         */
        public static String eliminatePart(String str, String insideStr, String begin, String end){
        	int insideIdx = str.indexOf(insideStr);
        	if(insideIdx != -1){
	        	int startIdx = str.lastIndexOf(begin, insideIdx);
	        	if(startIdx != -1){
	        		int endIdx = str.indexOf(end, startIdx);
	        		if(endIdx != -1){
	        			endIdx += end.length();
	        			str = str.substring(0, startIdx) + str.substring(endIdx);
	        		}
	        	}
        	}
        	return str;        	
        }
        public static int getCurentTime(int n){
        	if(n<0) n=-n;
        	Calendar cal=Calendar.getInstance();
        	
        	if(n>0){
        		return (int)cal.getTimeInMillis()%n;
        	}
        	return (int)cal.getTimeInMillis();
        }
        
        public static int indexOf(String[] s1, String s2){
        	if (s1 == null || s2 == null){
        		return 0;
        	}
        	for (int j =0; j< s1.length; j++){
        		if (s1[j].equals(s2)){
        			return j;
        		}
        	}
        	return -1;
        	
        }
        
       public static boolean equals(String[] s1, String[] s2){
    	   if(s1==null){
    		   if(s2!=null) return false;
    		   else return true;
    	   }
    	   if(s2 == null) return false;
    	   if(s1.length != s2.length)
    		   return false;
    	   for (int i = 0; i < s1.length; i++) {
    		   if(!s1[i].equals(s2[i]))
    			   return false;
    	   }
    	   return true;
       }
       
       /**
        * remove leading zeroes if they exist
        * leave null as null
        * @param str input string
        * @return string with leading zeroes removed
        */
       public static String removeLeadingZeroes(String str){
    	   if(str != null){    		   
    		   str = str.replaceFirst("^0+", "");
    	   }
    	   return str;
       }
       
   	/**
   	 * extract $1 parameter using a regex
   	 * @param data
   	 * @param regex
   	 * @return
   	 */
   	public static String extractParameter(String data, String regex){
		Pattern p = Pattern.compile(regex);
		Matcher m = p.matcher(data);
		if(m.find()){
			return m.group(1);
		} else {
			return "";
		}
   	}
   	
   	/**
   	 * Removes first row from a table
   	 * @param table
   	 * @return
   	 */
	public static String removeFirstRow(String table){
		
		int istart, iend, tmp;
		
		// determine start of row
		istart = table.indexOf("<tr");
		tmp = table.indexOf("<TR");		
		if(istart == -1 || istart > tmp && tmp != -1){ 
			istart = tmp; 
		} 
		if(istart == -1){
			return table;
		}
		
		// determine end of row
		iend = table.indexOf("</tr>", istart);
		tmp = table.indexOf("</TR>", istart);
		if(iend == -1 || iend > tmp && tmp != -1){
			iend = tmp;
		}
		if(iend == -1){
			return table;
		}
		iend += "</tr>".length();
		
		return table.substring(0,istart) + table.substring(iend);		
		
	}
	
	/**
	 * Extract whole contents of an html tag
	 * @param offset
	 * @param html
	 * @param tag
	 * @return
	 */
	public static String extractTagContents(int offset, String html, String tag){
	
		// prepare some data
		String orig = html;
		html = html.toLowerCase();
		tag = tag.toLowerCase();
		String startTag = "<" + tag;
		String endTag = "</" + tag + ">";
		
		// determine start of tag content
		int istart = html.indexOf(startTag, offset);
		if(istart == -1){
			return null;
		}
		
		int crtPos = istart + 1;
		int level = 1;
		while(level != 0){
			int epos = html.indexOf(endTag, crtPos);
			int spos = html.indexOf(startTag, crtPos);
			// if we do not find it, then exit the loop and mark error
			if(epos == -1 && spos == -1){
				crtPos = -1;				
			}
			// if both are != -1
			else if(epos != -1 && spos != -1){
				if(epos < spos){
					crtPos = epos + 1;
					level--;
				} else {
					crtPos = spos + 1;
					level++;
				}				
			}
			else if(epos != -1){
				crtPos = epos + 1;
				level--;
			} else {
				crtPos = spos + 1;
				level++;				
			}
			if(crtPos == -1 || level <0 ){
				break;
			}
		}
		if(level == 0 && crtPos != -1){
			return orig.substring(istart, crtPos + endTag.length()-1);
		} else {
			return null;
		}
		
	}

	public static int findFirstIndex(String string, int from, String ...keys){
		int min = -1;
		for(String key: keys){
			int pos = string.indexOf(key, from);
			if(pos != -1 && ((pos < min && min != -1) || min == -1)){
				min = pos;
			}
		}
		return min;
	}
	
	public static int findLastIndex(String string, int from, String ...keys){
		int max = -1;
		for(String key: keys){
			int pos = string.lastIndexOf(key, from);
			if(pos != -1 && ((pos > max && max != -1) || max == -1)){
				max = pos;
			}
		}
		return max;
	}	
	
	public static String removeSimpleTag(String table, int insidePos, String tag){
		String tag1 = tag.toLowerCase();
		String tag2 = tag.toUpperCase();
		if(insidePos == -1){
			return table;
		}
		int istart, iend;
		istart = findLastIndex(table, insidePos, "<" + tag1, "<" + tag2);
		if(istart == -1){
			return table;
		}
		iend = findFirstIndex(table, insidePos, "</"+tag1+">", "</" + tag2 + ">");
		if(iend == -1){
			return table;
		}
		iend += ("</" + tag + ">").length();
		return table.substring(0,istart) + table.substring(iend);
	}
	
	public static String removeSimpleFirstColumn(String table){		
		
		int istart = findFirstIndex(table, 0, "<tr", "<TR"); 
		int iend = findFirstIndex(table, istart, "</tr>", "</TR>"); 
		if(istart == -1 || iend == -1){ 
			return table; 
		}		
		String prefix = table.substring(0, istart);
		int lastIend = findLastIndex(table, table.length()-1, "</tr>", "</TR>"); 
		if(lastIend == -1){ 
			return table; 
		}		
		iend += "</tr>".length();
		String suffix = table.substring(lastIend);
		
		String newTable = "";
		while(istart != -1 && iend != -1){
			
			String row = table.substring(istart, iend);
			int start = findFirstIndex(row, 0, "<td", "<TD"); 
			int end = findFirstIndex(row, 0, "</td>", "</TD>");
			if(start == -1 || end == -1){
				return table;
			}			

			newTable += row.substring(0, start) + row.substring(end + "</td>".length());
			
			istart = findFirstIndex(table, istart+1, "<tr", "<TR"); 
			iend = findFirstIndex(table, istart, "</tr>", "</TR>"); 
			iend += "</tr>".length();
		}
		if(istart == -1){
			return prefix + newTable + suffix;
		}else{
			return table;
		}
	}
	
	public static String [] splitCommaList(String input){
		if(input == null){ return new String[0]; }
		// get rid of spaces
		input = input.replaceAll("\\s+","");
		// compact multiple commas
		input = input.replaceAll("\\,{2,}",",");
		// remove first comma
		if(input.startsWith(",")){ input = input.substring(1); }
		// remove last comma
		if(input.endsWith(",")){ input = input.substring(0, input.length()-1);}
		// perform split
		return input.split(",");
	}
	
	/**
	 * Checks if a string is null or it contains only spaces
	 * @param s
	 * @return
	 */
	public static boolean isEmpty(String s){
		return s == null || "".equals(s.trim());
	}
	
	public static boolean isNotEmpty(String s){
		return !isEmpty(s);
	}
	
	public static boolean areAllEmpty(String... strings) {
		for(String s : strings) {
			if(isNotEmpty(s)) {
				return false;
			}
		}
		return true;
	}
	
	public static boolean specialStringCompare(String s1,String s2){
		 if (isEmpty(s1) && isEmpty(s2)) return true;
		 else {
			 return s1.equals(s2);
		 }
	}
	/**
	 * Checks if a string[] is null or it contains only spaces
	 * @param s
	 * @return
	 */
	public static boolean isEmpty(String []s){
	
		if(s==null){
			return false;
		}
		
		for(int i=0;i<s.length;i++){
			if(!StringUtils.isEmpty(s[i])){
				return false;
			}
		}
		
		return true;
	
	}

	/**
	 * Url encode a string
	 * @param str
	 * @return encoded str or empty string if input is null
	 * @throws RuntimeException if UTF-8 encoding is not supported
	 */
	public static String urlEncode(String str){
		if(str == null){ return ""; }
		try{
			return URLEncoder.encode(str, "UTF-8");
		}catch(UnsupportedEncodingException e){
			throw new RuntimeException(e);
		}
	}
	
	/**
	 * Url decode a string
	 * @param str
	 * @return encoded str or empty string if input is null
	 * @throws RuntimeException if UTF-8 encoding is not supported
	 */
	public static String urlDecode(String str){
		if(str == null){ return ""; }
		try{
			return URLDecoder.decode(str, "UTF-8");
		}catch(UnsupportedEncodingException e){
			throw new RuntimeException(e);
		}
	}
	
	/**
	 * Format a list using a separator between two items
	 * @param items
	 * @param separator
	 * @return
	 */
    public static String formatList(List<String> items, String separator){
    	
    	String retVal = "";
		boolean first = true;
		for(String item: items){
			if(!first){
				retVal += separator;
			}
			first = false;
			retVal += item;
		}
		return retVal;
		
    }
    
    /**
	 * Format a set using a separator between two items
	 * @param items
	 * @param separator
	 * @return
	 */
    public static String formatList(Set<String> items, String separator){
    	
    	String retVal = "";
		boolean first = true;
		for(String item: items){
			if(!first){
				retVal += separator;
			}
			first = false;
			retVal += item;
		}
		return retVal;
		
    }
    /** used for random functions inside this class*/
    private static final Random random = new Random();
    
    /**
     * Generate a string with random digits in it
     * @param length
     * @return
     */
    public static synchronized String generateRandomNumber(int length){    	
    	char [] value = new char[length];
    	for(int i=0; i<length; i++){
    		value[i] = (char)('0' + random.nextInt(10));
    	}
    	return new String(value);
    }


    /**
     * Returns a new array similar to the one received but with the element at the given position replaced with the new element received
     * @param array
     * @param elem
     * @param position
     * @return
     */
	public static String[] replaceElementInArrayAtPosition(String[] array, String elem, int position) {
		if(array==null)
			return null;
		
		String[] result = new String[array.length];
		for (int i = 0; i < array.length; i++) {
			if(i!=position)
				result[i] = array[i];
			else
				result[i] = elem;
		}
		return result;
	}

	/**
	 * Format a list of items to be displayed in HTML
	 * @param title
	 * @param items
	 * @return
	 */
	public static String printHtmlList(String title, List<String> items){
		StringBuilder sb = new StringBuilder();
		sb.append(title);
		sb.append(":<br/>");
		for(Object item: items){
			sb.append("<li>");
			sb.append(StringEscapeUtils.escapeHtml(item.toString()));
			sb.append("</li>");
		}
		return sb.toString();
	}
	
	public static String createCollapsibleHeader(){
        String id = String.valueOf(System.nanoTime());  
        return 
	         "<div id=\"" + id + "_header\" class=\"submitLinkBlue\" onClick=\"JavaScript:toggle('" + id + "');\">Show Details</div>" +
	         "<div id=\"" + id + "_contents\" style=\"display:none\">";
	}
	
	/**
	 * Eliminates every character except '0-9' and ',' in the string
	 * @param list
	 * @return
	 */
	public static String makeValidNumberList(String list) {
		list = list.replaceAll("[^0-9,\\.]", "");
		return list;
	}
	
//	 table to convert a nibble to a hex char.
	static final char[] hexChar = {
	   '0' , '1' , '2' , '3' ,
	   '4' , '5' , '6' , '7' ,
	   '8' , '9' , 'a' , 'b' ,
	   'c' , 'd' , 'e' , 'f'};
	
	/** convert a String to a hex representation of the String,
	  * with 4 hex chars per char of the original String.
	  * e.g. "1abc \uabcd" gives "3161626320_abcd"
	  * @param s String to convert to hex equivalent
	  */
	public static String convertStringToHexString ( String s )
	   {
	   StringBuilder sb = new StringBuilder( s.length() * 5 - 1 );
	   for ( int i=0; i<s.length(); i++ )
	      {
	      char c = s.charAt(i);
	      if ( i != 0 )
	         {
	         //sb.append( '' );
	         }
	      // encode 16 bits as four nibbles

	      //sb.append( hexChar [ c >>> 12 & 0xf ] );
	      //sb.append( hexChar [ c >>> 8 & 0xf ] );
	      sb.append( hexChar [ c >>> 4 & 0xf ] );
	      sb.append( hexChar [ c & 0xf ] );
	      }
	   return sb.toString();
	   }
	
	   public static String HTMLEntityDecode( String input ) {
		   	input = HTMLDecodeChar(input, '<');
		   	input = HTMLDecodeChar(input, '>');
		   	input = HTMLDecodeChar(input, '&');
		   	input = HTMLDecodeChar(input, '"');
		   	input = HTMLDecodeChar(input, '\'');
		   	input = HTMLDecodeChar(input, '(');
		   	input = HTMLDecodeChar(input, ')');
		   	input = HTMLDecodeChar(input, '#');
		   	input = HTMLDecodeChar(input, '%');
		   	input = HTMLDecodeChar(input, ';');
		   	return input;
		   }
		   
	   public static String HTMLDecodeChar(String input, char ch) {
		   	return input.replaceAll("&#"+(int)ch+";", ch+"");
		  }
	   

	   public static String upperCaseFirst(String str) {
		   if(str.length()==0) return str;
		   if(str.length()==1) return str.toUpperCase();
		   return str.substring(0,1).toUpperCase() + str.substring(1, str.length()).toLowerCase() ;
	   }
	   
	   /**
	    * Convert string to lowercase, only with the first letter uppercase
	    * @param str
	    * @return
	    */
	   public static String capitalizeFirstLettersOnly(String str) {
		   Matcher m = Pattern.compile("(\\w+)").matcher(str);
		   	   
		   StringBuffer ret = new StringBuffer();

		   while(m.find()) {
			   m.appendReplacement(ret,  Matcher.quoteReplacement(upperCaseFirst(m.group())));
		   }
		   
		   m.appendTail(ret);
		   
		   return ret.toString();
	   }
	   
		/**
		 * converts HTML entities in a string to lowercase (otherwise they aren't interpreted by browsers)
		 */
		public static String convertHTMLEntitiesToLowercase(String s) {

			if(s.isEmpty()) return s;
			String[] entities = { "&amp;","&lt;","&gt;","&acute;","&quot;","&nbsp;" };
			
			for(String e : entities) {
				 Pattern p = Pattern.compile(e,Pattern.CASE_INSENSITIVE );
				 Matcher m = p.matcher(s);
				 s = m.replaceAll(e.toLowerCase());
				 m.reset();
			}
			
			return s;
		}
		
		public static String notNull(String s){
			return s != null ? s : "";
		}
		
		public static Vector<Integer> indexesOf(String s, char c){
			return indexesOf(s, "" +c);	
		}
		
		public static Vector<Integer> indexesOf(String s, String c){
			Vector<Integer> indexes = new Vector<Integer>();
			Matcher m = Pattern.compile(c).matcher(s);
			while (m.find()){
				indexes.add(m.start());
			}
			return indexes;
		}		
		
		private static final Pattern MC_PATTERN = Pattern.compile("([\\p{Punct}\\s]+Mc)(\\p{Lower})");
		private static final Pattern O_APOS_PATTERN = Pattern.compile("([\\p{Punct}\\s]+O')(\\p{Lower})");
		private static final Pattern ROMAN_PATTERN = Pattern.compile("(?is)([\\p{Punct}\\s]+)([MDCLXVI]+)(?=[\\p{Punct}\\s]+)");
		private static final Pattern STATE_PATTERN =  Pattern.compile("(?is)([\\p{Punct}\\s]+)(\\p{Upper}\\p{Lower})(?=[\\p{Punct}\\s]+)");
		
		public static String toTitleCase(String str) {
			if( str==null ){
				return null;
			}
			str = str.replaceAll("\r\n", "^");//B 6888
			String s = WordUtils.capitalizeFully(str, new char[]{' ', '(', '>',',','^'});
			
			//B 6888
			str = str.replaceAll("\\^", "\r\n");
			s = s.replaceAll("\\^", "\r\n");
			
			
			Matcher m = MC_PATTERN.matcher(s);
			{
				StringBuffer sb = new StringBuffer();
				while (m.find()) {
					m.appendReplacement(sb, Matcher.quoteReplacement(m.group(1) + m.group(2).toUpperCase()));
					//s = s.replaceFirst(m.group(), m.group(1) + m.group(2).toUpperCase());
				}
				m.appendTail(sb);
				s = sb.toString();
			}
			
			{
				m = O_APOS_PATTERN.matcher(s);
				StringBuffer sb = new StringBuffer();
				while (m.find()) {
					m.appendReplacement(sb, Matcher.quoteReplacement(m.group(1) + m.group(2).toUpperCase()));
					//s = s.replaceFirst(m.group(), m.group(1) + m.group(2).toUpperCase());
				}
				m.appendTail(sb);
				s = sb.toString();
			}
			
			{
				m = ROMAN_PATTERN.matcher(s);
				StringBuffer sb = new StringBuffer();
				while (m.find()) {
					String possibleRoman = m.group(2).toUpperCase();
					try {
						Roman.parseRoman(possibleRoman);
						m.appendReplacement(sb, Matcher.quoteReplacement(m.group().toUpperCase()));
					} catch (Exception e) {
					}
				}
				m.appendTail(sb);
				s = sb.toString();
			}
			
			{
				m = STATE_PATTERN.matcher(s);
				StringBuffer sb = new StringBuffer();
				while (m.find()) {
					String possibleState = m.group(2).toUpperCase();
					try {
						if(!"IN".equals(possibleState)) {	//Indiana is special
							if(StateCountyManager.getInstance().isStateAbrevValid(possibleState)) {
								m.appendReplacement(sb, Matcher.quoteReplacement(m.group(1) + possibleState));
								//s = s.replaceFirst(m.group(), m.group(1) + possibleState);
							}
						}
					} catch (Exception e) {
					}
				}
				m.appendTail(sb);
				s = sb.toString();
			}
			
			str = s.trim();
			int poz = 0;
			while( (poz=str.indexOf('-',poz))>0 && poz<str.length()-2 ){
				str = str.substring(0,poz+1)+str.substring(poz+1, poz+2).toUpperCase()+str.substring(poz+2);
				poz+=1;
			}
			return str;
		}
		
		/**
		 * Split a string using regex as delimiter.<br> 
		 * It will also include what is matched by regex in result
		 * @param source - string to be split
		 * @param regex - delimiter
		 * @param includeDelimiterLeft - if true, include the delimiter string in the left substring<br>
		 * 						if false, include the delimiter string in the right substring
		 * @return
		 */
		public static ArrayList<String> splitIncludeDelimiter(String source, String regex, boolean includeDelimiterLeft){
			ArrayList<String> split = new ArrayList<String>();
			Pattern c = Pattern.compile(regex);
			Matcher ma = c.matcher(source);
			int lastCharIncluded = 0;
			int curCharIncluded = 0;
			while(ma.find()){
				if (includeDelimiterLeft){
					curCharIncluded = ma.end(); 
				} else {
					curCharIncluded = ma.start();
				}
				split.add(source.substring(lastCharIncluded, curCharIncluded));
				lastCharIncluded = curCharIncluded;
			}
			if (lastCharIncluded < source.length()){
				split.add(source.substring(lastCharIncluded));
			}
			if (split.size() > 0){
				if (isEmpty(split.get(0))){
					split.remove(0);
				}
				if (isEmpty(split.get(split.size()-1))){
					split.remove(split.size()-1);
				}
			}
			
			return split;
		}
		
		public static void main(String[] a){
			//ArrayList<String> a1 = splitIncludeDelimiter("-b-c-", "-", false);
			//a = a;
			
			List<String> input = new ArrayList<String>();
			input.add("1");
			input.add("2");
			input.add("1");
			input.add("D");
			input.add("5");
			input.add("D");
			input.add("3");
			input.add("6");
			input.add("8");
			List<Interval> compactEnumerationToInterval = compactEnumerationToInterval(input);
			for (Interval interval : compactEnumerationToInterval) {
				System.out.println(interval);
			}
			
			System.out.println("----------");
			
			System.out.println(formatStringExceptLinks("asdasd <a href=\"asdsada\"> adsada <a href=\"sadas\"> adsadx",TitleType.TITLE_CASE));
			System.out.println(formatStringExceptLinks("xxx aa adsadx",TitleType.TITLE_CASE));
			
			String s = "Property (including any improvements): Lot 7, Block C, QUAIL CREEK PHASE II, " +
					"an Addition to the O'Donnell City Mc2inney of VVVVVVVVVV Mckinney, Collin Mckinney TX AK IN GG  County, Texas, " +
					"according to the map or plat thereof recorded VII  McBinney in <link0>Volume " +
					"G, Page 591</a>, Map Records, Collin O'Neill County, Texas.";
			
			System.out.println(s);
			
			s = WordUtils.capitalizeFully(s, new char[]{' ', '(', '>',','});
			System.out.println(s);
			
			System.out.println(toTitleCase(s));
			/*
			Pattern scotish1 = Pattern.compile("([\\p{Punct}\\s]+Mc)(\\p{Lower})");
			Matcher m = scotish1.matcher(s);
			while(m.find()) {
				s = s.replaceFirst(m.group(), m.group(1) + m.group(2).toUpperCase());
			}
			
			scotish1 = Pattern.compile("([\\p{Punct}\\s]+O')(\\p{Lower})");
			m = scotish1.matcher(s);
			while(m.find()) {
				s = s.replaceFirst(m.group(), m.group(1) + m.group(2).toUpperCase());
			}
			
			scotish1 = Pattern.compile("(?is)([\\p{Punct}\\s]+)([MDCLXVI]+)(?=[\\p{Punct}\\s]+)");
			m = scotish1.matcher(s);
			while(m.find()) {
				String possibleRoman = m.group(2).toUpperCase();
				try {
					Roman.parseRoman(possibleRoman);
					s = s.replaceFirst(m.group(), m.group(1) + possibleRoman);
				} catch (Exception e) {
				}
			}
			
			scotish1 = Pattern.compile("(?is)([\\p{Punct}\\s]+)(\\p{Upper}\\p{Lower})(?=[\\p{Punct}\\s]+)");
			m = scotish1.matcher(s);
			while(m.find()) {
				String possibleState = m.group(2).toUpperCase();
				try {
					if(!"IN".equals(possibleState)) {
						if(DBManager.getStateForAbvStrict(possibleState)!=null) {
							s = s.replaceFirst(m.group(), m.group(1) + possibleState);
						}
					}
					
				} catch (Exception e) {
				}
			}
			
			
			
			
			
			System.out.println(s);
			*/
		}
		
		public static String formatString(String value, NameFormaterI.TitleType type) {
			if(type == NameFormaterI.TitleType.UPPER_CASE) return value.toUpperCase();
			if(type == NameFormaterI.TitleType.LOWER_CASE) return value.toLowerCase();
			if(type == NameFormaterI.TitleType.TITLE_CASE) return StringUtils.toTitleCase(value);
			return value;
		}
		
		public static String formatStringExceptLinks(String value, NameFormaterI.TitleType type) {
			
			Pattern hrefPattern = Pattern.compile("(?i)<a href=[^>]*?>");
			Matcher hrefMatcher = hrefPattern .matcher(value);   
			Map<Integer,String> links = new HashMap<Integer,String>();
			
			StringBuffer formattedValue = new StringBuffer();
			int i = 0;
			
			while(hrefMatcher.find()) {
				try {
					String link = hrefMatcher.group();
					links.put(i, link);
					hrefMatcher.appendReplacement(formattedValue, "<link"+i+">");
					i++;
				}catch(Exception e) {
					e.printStackTrace(); 
				}
			}
			hrefMatcher.appendTail(formattedValue);
			
			String formattedValueStr = formattedValue.toString();
			formattedValueStr = formatString(formattedValueStr,type);
						
			Pattern linkPattern = Pattern.compile("(?i)<link([0-9]+)>");
			Matcher linkMatcher = linkPattern.matcher(formattedValueStr);   
					
			formattedValue = new StringBuffer();
			i = 0;
			
			while(linkMatcher.find()) {
				try {
					String link = links.get(Integer.parseInt(linkMatcher.group(1)));
					linkMatcher.appendReplacement(formattedValue, link);
				}catch(Exception e) {
					e.printStackTrace(); 
				}
			}
			linkMatcher.appendTail(formattedValue);
			
			formattedValueStr = formattedValue.toString();
			formattedValueStr = formattedValueStr.replaceAll("(?i)</a>", "</a>");
			return formattedValueStr;
		}
		
		public static String getPrettyBrowserString(String userAgent){
			if (StringUtils.isEmpty(userAgent)){
				return "";
			}
			Iterator<String> i = userAgentVersion.keySet().iterator();
			String version = null;
			String browser = "";
			while (i.hasNext() && version == null){
				browser = i.next();
				Matcher m = userAgentVersion.get(browser).matcher(userAgent);
				if (m.find()){
					version = m.group(1);
				}
			}
		// detect correct ie version if it's in compatibility view
		if (browser.equals("Internet Explorer"))
			if (userAgent.contains("Trident/6.0") && !version.equals("10.0"))
			{
				version = "10.0";
			}
			else if (userAgent.contains("Trident/5.0") && !version.equals("9.0"))
			{
				version = "9.0";
			}
			else if (userAgent.contains("Trident/4.0") && !version.equals("8.0"))
			{
				version = "8.0";
			}
		if (version != null) {
			return StringUtils.HTMLEntityEncode(" " + browser + " version " + version);
		} else {
			return StringUtils.HTMLEntityEncode(userAgent);
		}
		}
		
		/**
		 * clean the names from initial order
		 * @param s - the name
		 * @return
		 */
		public static String cleanInitialOrderName(String s){
			if (!isEmpty(s)){
				//s = s.replace(".", "");		//B6140
				//s = s.replaceAll("(?i)\\bTBD\\b", "");
				//s = s.replaceAll("(?i)\\bTO BE DETERMINED\\b", "");
				s = s.replaceAll("\\s{2,}", " ");
			}
			return s;
		}
		
		/**
		 * Clean a name if the request is from submit order.
		 * @param s - string to be cleaned
		 * @param o - request.getParameter("orderTS")
		 * @return
		 */
		public static String cleanTSOrder(String s, String o){
			if (o != null){
				return StringUtils.cleanInitialOrderName(s);
			} else {
				return s;
			}
		}
		/**
		 * Takes an array of strings and create a new string my first taking the firstLetter from each element and adding it to the result.
		 * It than goes to the second letter and so one.
		 * If an element is shorted the method will continue intercalating the rest of the elements
		 * @param strings
		 * @return
		 */
		public static String intercalateCharacters(String ...strings ){
			StringBuilder sb = new StringBuilder();
			int j = 0;
			boolean added = true;
			while (added) {
				added = false;
				for (int i = 0; i < strings.length; i++) {
					if(j < strings[i].length()) {
						added = true;
						sb.append(strings[i].charAt(j));
					}
				}
				j++;
			}
			return sb.toString();
		}
				
		/**
		 * Extracts a list of table rows from a htmlTable
		 * @param htmlTable
		 * @return a list of <tr>blah blah </tr>
		 */
		public static List<String> getTableRows(String htmlTable){
			Pattern patternTR = Pattern.compile("(?si)\\<tr>.*?</tr>");
			final Matcher matcherTR = patternTR.matcher(htmlTable);
			List<String> rows = new ArrayList<String>();
			while (matcherTR.find()) {
				final int gc = matcherTR.groupCount();
				// group 0 is the whole pattern matched,
				// loops runs from from 0 to gc, not 0 to gc-1 as is traditional.
				for (int i = 0; i <= gc; i++) {
					rows.add(matcherTR.group(i));
				}
			}
			return rows;
		}
		
		
		public static List<String> getColumnValuesFromHTMLTableForAGivenHeader(
				String htmlTable, String header) {
			List<String> tableRows = StringUtils.getTableRows(htmlTable);
			List<String> rightColumns = new ArrayList<String>();
			boolean ignoreFirstRow=true;
			int columnHeaderIndex = getColumnHeaderIndex(htmlTable, header);
			for (String rows : tableRows) {
				List<String> listFromTR = getTDListFromTR(rows);
				if (ignoreFirstRow){
					ignoreFirstRow = false;
					continue;
				}
				String string = listFromTR.get(columnHeaderIndex);
				rightColumns.add(string);
			}
			return rightColumns;
		}

		public static int getColumnHeaderIndex(String htmlTable, String header){
			List<String> tableRows = StringUtils.getTableRows(htmlTable);
			int rightColumn=-1;
			Iterator<String> it = tableRows.iterator();
			if (it.hasNext()) {
				String firstRow = it.next();
				List<String> listFromTR = getTDListFromTR(firstRow);
				String column = null;
				Iterator<String> iterator = listFromTR.iterator();
				
				do {
					column = iterator.next();
					int indexOf = column.indexOf(header);
					if (indexOf != -1) {
						rightColumn = listFromTR.indexOf(column);
					}
				} while ((iterator.hasNext())&&(rightColumn==-1));
			}
			return rightColumn;
		}
		
		public static List<String> getTDListFromTR(String trString) {
			Pattern patternTD = Pattern.compile("\\<td[^>]*>.*?</td>");

			final Matcher matcherTD = patternTD.matcher(trString);
			List<String> tdList = new ArrayList<String>();
			while (matcherTD.find()) {
				final int gc = matcherTD.groupCount();
				// group 0 is the whole pattern matched,
				// loops runs from from 0 to gc, not 0 to gc-1 as is traditional.
				for (int i = 0; i <= gc; i++) {
					tdList.add(matcherTD.group(i));
				}
			}
			return tdList;
		}

		public static String extractParameterFromUrl(String url, String parameter) {
			String value = "";
			
			Pattern urlParamPattern = Pattern.compile("(?i)(^|\\?|&)"+parameter+"=(.*?)(&|$)"); 
			Matcher m = urlParamPattern.matcher(url);
			
			if(m.find()) {
				value = m.group(2);
			}
			
			return value;
			
		}
				
		/**
		 * Extracts the GET params from an URL
		 * 
		 * @param url
		 * @return
		 */
		public static Map<String, String> extractParametersFromUrl(String url) {

			if (url.contains("?") == false)
				// No parameters.
				return new HashMap<String, String>();
			return extractParametersFromQuery(url.split("\\?")[1]);
		}

		/**
		 * Adds a GET parameter to the provided URL.
		 * @param url
		 * @param name
		 * @param value
		 * @return
		 */
		public static String addParameterToUrl(String url, String name, String value) {
			if (url == null)
				return null;
			char lastChar = url.charAt(url.length() - 1);
			if (lastChar != '&' && lastChar != '?')
				url += url.contains("?") ? "&" : "?";
			url += name + "=" + value;
			return url;
		}
		
		/**
		 * extracts the parameters and values from a url query into a Map.
		 * @param query
		 * @return
		 */
		public static Map<String, String> getQueryMap(String query)  
		{
			//remove the link related stuff
			
			if (query.contains("?")){
				String[] link= query.split("\\?");
				if(link.length==2){
					query = link[1];
				}
			}
			
			String[] params = query.split("&");
		    Map<String, String> map = new HashMap<String, String>();  
		    for (String param : params)  
		    {  
		        String[] split = param.split("=");
				String name = split.length>=1? split[0]:"";  
		        String value = split.length==2? split[1]:"";  
			    map.put(name, value);
		    }  
		    return map;  
		}  
		
		/**
		 * @param container
		 * @param cointainee
		 * @return if the size of the return list is gt 0 then you will find in the list the counter index of the word found in the cointainer.  
		 */
		public static List<Integer> stringContainsComponentsFromOtherString(String container, String cointainee){

			List<String> cointainerWords = splitString(container);
			List<String> cointaineeWords = splitString(cointainee);
			List<Integer> results = new ArrayList<Integer>();
			
			for (String s : cointaineeWords) {
				for (String cointainerW : cointainerWords) {
					if (s.equals(cointainerW)){
						results.add( cointainerWords.indexOf(s));
					}
				}
			}
			return results;
		}
		
		public static Map<String,String> extractParametersFromQuery(String urlQuery) {

			Map<String,String> parameters = new HashMap<String,String>();

			for(String str : urlQuery.split("&")) {
				String[] x = str.split("=");
				if(x.length==1) {
					parameters.put(x[0],"");
				}else {
					parameters.put(x[0],urlDecode(x[1]));
				}
			}
			
			return parameters;
		}
		
		public static String addParametersToUrl(String url, Map<String,String> parameters) {
			return addParametersToUrl(url, parameters, false);
		}
		
		public static String addParametersToUrl(String url, Map<String,String> parameters, boolean defaultValue) {
			String finalUrl = url;
			if(!url.contains("?")) {
				finalUrl += "?"; 
			}
			for(Entry<String, String> ent : parameters.entrySet()) {
				String value2 = ent.getValue();
				value2 = defaultValue?org.apache.commons.lang.StringUtils.defaultIfEmpty(value2, ""):value2;
				finalUrl +=  ent.getKey() + "=" + value2 + "&";
			}
			return finalUrl;
		}
		
		/**
		 * 
		 * @param datePattern the pattern that the @param date is expected to have e.g. "MM/DD/YYYY"
		 * @param date the actual date e.g. "12/13/1999"
		 * @return calendar filled with the date from @param date
		 */
		public static Calendar getDateFromFormat(String datePattern,String date){
			DateFormat format = new SimpleDateFormat(datePattern);
			Calendar calendar = Calendar.getInstance();
			try {
				calendar.setTime(format.parse(date));
			}catch (ParseException e) {
				e.printStackTrace();
			}
			return calendar;
		}	
			
	public static String cleanHtml(String htmlString) {
		if (StringUtils.isNotEmpty(htmlString)) {

			htmlString = htmlString.replaceAll("(?i)&nbsp;", " ");
			htmlString = htmlString.replaceAll("&amp;", "&");
			htmlString = htmlString.replaceAll("(?i)<b>", " ");
			htmlString = htmlString.replaceAll("(?i)</b>", " ");
			htmlString = htmlString.replaceAll("<i>", " ");
			htmlString = htmlString.replaceAll("</i>", " ");
			htmlString = htmlString.replaceAll("(?is)<TD>", "");
			htmlString = htmlString.replaceAll("(?is)</TD>", "");
			htmlString = htmlString.replaceAll("(?is)</?font[^>]*>", "");
			htmlString = htmlString.replaceAll("(?is)(?is)</?br[^>]*>", "");
			htmlString = org.apache.commons.lang.StringUtils.strip(htmlString);
		}
		return htmlString;
	}
		/**
	 * parses by the given regex and returns if ti finds the indicated group
	 * 
	 * @param inputString
	 * @param regex
	 * @param group
	 * @return
	 */
	public static String parseByRegEx(String inputString, String regex, int group) {
		String string = "";
		if(StringUtils.isNotEmpty(inputString)){
			Pattern compile = Pattern.compile(regex);
			Matcher matcher = compile.matcher(inputString);
			string = "";
			if (matcher.find()) {
				string = matcher.group(group);
			}
		}
		return string;
	}

	/**
	 * Parses a inputString by the provided regex. Retrieves an array with the
	 * length equal with groups. In groups are kept the group indexes that are
	 * to be kept after the parse in the resulting array
	 * 
	 * @param inputString
	 * @param regex
	 * @param groups
	 * @return
	 * @deprecated Use {@link RegExUtils#parseByRegEx(String,String,int[])} instead
	 */
	public static String[] parseByRegEx(String inputString, String regex, int[] groups) {
		return RegExUtils.parseByRegEx(inputString, regex, groups);
	}
	
	public static String getSuccessionOfValuesByRegEX(String string, String regExmarker, String regEx){
		Pattern compile = Pattern.compile(regExmarker);
		Matcher matcher = compile.matcher(string);
		int marker = 0;
		int[] matches = ro.cst.tsearch.utils.StringUtils.getMatches(string, regExmarker);
		StringBuilder result = new StringBuilder("");
		int i = 0;
		
		while (matcher.find()){
			if (matches.length==0){
				marker = string.indexOf(matcher.group());
			}else{
				marker = matches[i];
			}
			
			result.append(" " + StringUtils.getSuccessionOFValues(string, marker, regEx));
			
			if (i < matches.length){
				i++;
			}
		}
		
		return result.toString().replaceAll("\\s+"," ");
	}
	
	/**
	 * Finds values like "SECTS 2 4  6" or "LOT 32 4". Any additional characters between target values should be removed.  
	 * @param string
	 * @param marker is the value that indicates a succesion of values like: LTS, SECTS, LOTS
	 * @param regEx
	 * @return
	 */
	public static String getSuccessionOFValues(String string, String marker, String regEx) {
		int lotIndex = string.indexOf(marker);
		String lots = "";
		if (lotIndex >= 0) {
			String lotToParse = string.substring(lotIndex);
			lotToParse = lotToParse.replace("&", " ");
			String regex = regEx;
			Pattern compile = Pattern.compile(regex);
			Matcher matcher = compile.matcher(lotToParse);
			while (matcher.find()) {
				lots += matcher.group(1).trim() + " ";
			}
		}
		return lots.trim();
	}
	
	public static String getSuccessionOFValues(String tmpLegalDescription, int lotIndex, String regExLots) {
		String lotToParse = tmpLegalDescription.substring(lotIndex);
		lotToParse = lotToParse.replace("&", " ");
		String regex = regExLots;
		Pattern compile = Pattern.compile(regex);
		Matcher matcher = compile.matcher(lotToParse);
		String lots = "";
		if (matcher.find()) {
			lots = matcher.group(1).trim();
		}
		return lots;
	}
	/**
	 * Searches in stringToSearch starting with the index of the marker by the provided rexgexToParseBy. The last element is the group that 
	 * should be kept, if there are any in the given regex.
	 *  E.g. "LOT 2 dgdasgad LOT 3", is found by calling (stringtoSearch, " LOT", "(?is)\\bLOT (\\d*)", 1 )    
	 * @param stringToSearch
	 * @param marker
	 * @param regexToParseBy
	 * @param groupToKeep
	 * @return
	 */
	public static String[] findRepeatedOcurrence(String stringToSearch, String marker, String regexToParseBy, int groupToKeep) {
		int[] matches = ro.cst.tsearch.utils.StringUtils.getMatches(stringToSearch, marker);
		String[] result = new String[matches.length];
		for (int i = 0; i < matches.length; i++) {
			if (matches[i] >= 0) {
				String lotToParse = stringToSearch.substring(matches[i]);
				Pattern compile = Pattern.compile(regexToParseBy);
				Matcher matcher = compile.matcher(lotToParse);
				String lot = "";
				if (matcher.find()) {
					lot = matcher.group(groupToKeep);
				}
				result[i] = lot;
			}
		}
		return result;
	}

	public static String[] findRepeatedOcurrenceByRegExMarker(String stringToSearch, String markerRegEx, String regexToParseBy, int groupToKeep) {
		String marker = StringUtils.parseByRegEx(stringToSearch, markerRegEx, 0);
		return findRepeatedOcurrence(stringToSearch, marker, regexToParseBy, groupToKeep);
	}
	
	public static String concatenate(String[] values, String delimitator) {
		StringBuilder returnValue = new StringBuilder();
		if(values!=null){
			for (String string : values) {
				returnValue.append(string + "" + delimitator);
			}
		}
		return returnValue.toString();
	}

	public static String addCollectionValuesToString(String result, String[] array) {
		if (array != null) {
			for (String string : array) {
				if (!RegExUtils.matches("\\b"+ string +"\\b", result)) {
					string = org.apache.commons.lang.StringUtils.strip(string);
					if (isNotEmpty(string)) {
						result += " " + string;
					}
				}
			}
		}
		return result;
	}

	public static String[] splitAndKeepSplitters(String stringToSplit,
			String[] splitters, boolean removeEmptyWhiteSpaces) {
		StringBuilder buildRegExForSplit = new StringBuilder("");
		buildRegExForSplit.append("(");
		for (String splitter : splitters) {
			buildRegExForSplit.append(MessageFormat.format(
					"((?<={0})|(?={0}))|", splitter));
		}
		buildRegExForSplit.deleteCharAt(buildRegExForSplit.lastIndexOf("|") );
		buildRegExForSplit.append(")");

		String[] split = stringToSplit.split(buildRegExForSplit.toString());

		List<String> noWhiteSpaceList = new LinkedList<String>();
		if (removeEmptyWhiteSpaces) {
			for (String string : split) {
				boolean notEmpty = StringUtils.isNotEmpty(string.trim());
				if (notEmpty) {
					noWhiteSpaceList.add(string.trim());
				}
			}
			split = noWhiteSpaceList.toArray(new String[] {} );
		}
		return split;
	}
	
	public static boolean isStringInStringArray(String[] stringArray, String string){
		boolean returnValue = false;
		int i=0;
		if (StringUtils.isEmpty(string) || stringArray.length ==0){
			return false;
		}
		
		while (!returnValue && i < stringArray.length){
			if (stringArray[i++].equals(string)){
				returnValue = true;
			}
		}
		return returnValue;
	}
	
	public static String replaceLast(String string, String toReplace, String replacement) {
	    int pos = string.lastIndexOf(toReplace);
	    if (pos > -1) {
	        return string.substring(0, pos)
	             + replacement
	             + string.substring(pos + toReplace.length(), string.length());
	    } else {
	        return string;
	    }
	}

	public static String cleanAmount(String string) {
		string = string.replaceAll(",|\\$", "");
		return string;
	}
	

	public static List<HashMap<String,String>> cleanAmount(List<HashMap<String, String>> strings) {
		for (Map<String, String> map : strings) {
			Set<Entry<String,String>> entrySet = map.entrySet();
			for (Entry<String, String> entry : entrySet) {
				entry.setValue( StringUtils.cleanAmount(entry.getValue()));
			}
		}
		return strings;
	}
	
	public static String [] stripStart(String[] str, String stripChars){
		for (int i=0; i< str.length; i++) {
			String string = str[i];
			if (StringUtils.isNotEmpty(string)){
				String stripStart = org.apache.commons.lang.StringUtils.stripStart(string, stripChars);
				str[i] = stripStart; 
			}
		}
		return str;
	}
	
	// Bug 6928 - (Ticket#1413985) MyATS Title Case setting for vesting does not work correctly
	// see also Task 7691
	public static void correctVestingTitleCaseExceptions(VestingInfoI vesting, NameFormaterI.TitleType format){
		if (!format.equals(NameFormaterI.TitleType.TITLE_CASE))
			return;

		String vesting_string = vesting.getVesting();

		if (StringUtils.isEmpty(vesting_string))
			return;

		String[] exceptions = { "And", "Wife", "Husband" };

		for (int i = 0; i < exceptions.length; i++) {
			if (vesting_string.contains(exceptions[i]))
				vesting_string = vesting_string.replaceAll("(^|\\W)"
						+ exceptions[i] + "(\\W|$)",
						"$1" + exceptions[i].toLowerCase() + "$2");
		}
		
		// Task 7691
		vesting_string = correctTitleCase(vesting_string, format);
		
		vesting.setVesting(vesting_string);
	}
	
	/**
	 * Task 7691
	 * @param text
	 * @return
	 */
	public static String correctTitleCase(String text, NameFormaterI.TitleType format) {
		if (!format.equals(NameFormaterI.TitleType.TITLE_CASE)) {
			return text;
		}
		
		Matcher m = Pattern.compile("(\\w+)").matcher(text);
		StringBuffer sb = new StringBuffer();
		boolean first = true;
		String word = "";

		while(m.find()) {
			word = m.group();
			
			
			if(Arrays.asList(lowercaseWords).contains(word.toLowerCase())) {
				if(!first) {
					int end = m.end();		
					if("a".equals(word.toLowerCase()) && text.length() > end) {
						char charAt = text.charAt(end);
						if(charAt == '.') {
							//this should be initial
							m.appendReplacement(sb, Matcher.quoteReplacement(m.group().toUpperCase()));			
						} else {
							m.appendReplacement(sb, Matcher.quoteReplacement(m.group().toLowerCase()));
						}
						
					} else {
						m.appendReplacement(sb, Matcher.quoteReplacement(m.group().toLowerCase()));
					}
				}
			} else if(Arrays.asList(stateAbbrev).contains(word.toUpperCase())) {
				m.appendReplacement(sb, Matcher.quoteReplacement(m.group().toUpperCase()));
			} else {
				m.appendReplacement(sb, Matcher.quoteReplacement(m.group()));
			}
			
			first = false;
		}

		m.appendTail(sb);
		String ret = sb.toString();
		
		// capitalize the last word
		if(Arrays.asList(lowercaseWords).contains(word.toLowerCase())) {
			ret = ret.replaceFirst(word.toLowerCase() + "(\\W*)\\z", toTitleCase(word) + "$1");
		}
		
		List<String> companySuffixes = new ArrayList<String>(Company.companySuffixes);
		companySuffixes.remove("ASSC");
		companySuffixes.remove("ASSN");
		companySuffixes.remove("ASSOC");
		companySuffixes.remove("INC");
		companySuffixes.remove("UP");
		
		String companySuffixesRegex = "(?i)\\b(";
		for(String suffix : companySuffixes) {
			suffix = suffix.replaceAll("\\.", "\\\\.");
			suffix = suffix.replaceAll(" ", "\\\\s");
			companySuffixesRegex += "|"+suffix;
		}
		companySuffixesRegex = companySuffixesRegex.replaceFirst("\\|", "");
		companySuffixesRegex += ")\\b";
		
		Pattern companySuffixesPattern = Pattern.compile(companySuffixesRegex,Pattern.CASE_INSENSITIVE);
		
		m = companySuffixesPattern.matcher(ret);
		sb.setLength(0);
		while (m.find()) {
		    m.appendReplacement(sb, m.group(1).toUpperCase());
		}
		m.appendTail(sb);
		ret = sb.toString();
		
		m = ETAL_DERIVATIONS_PATTERN.matcher(ret);
		sb.setLength(0);
		while (m.find()) {
		    m.appendReplacement(sb, m.group().toLowerCase());
		}
		m.appendTail(sb);
		
		ret = sb.toString();
		ret = ret.replaceAll("\\bCO\\.", "Co.");

		return ret;
	}
	
	/**
	 * Used to compact a list of possible token into Interval like structures<br>
	 * The list must contain single tokens. A token which is a negative Integer will be considered positive as a parsing error<br>
	 * Intervals are compacted only as Integer intervals and Strings are left as found<br>
	 * Example: given an input of [1,D,2,D,5,3] it should return 3 Interval objects [1-3],[5] and [D]
	 * @param enumeration the list with the tokens to compact
	 * @return a list o Interval objects
	 */
	public static List<Interval> compactEnumerationToInterval(List<String> enumeration) {
		List<Interval> result = new ArrayList<Interval>();
		
		Set<Integer> enumerationOfInt = new TreeSet<Integer>();
		Set<String> enumerationOfString = new TreeSet<String>();
		
		for (String value : enumeration) {
			if(value != null) {
				try {
					int parseInt = Integer.parseInt(value);
					if(parseInt < 0) {
						parseInt *= -1;
					}
					enumerationOfInt.add(parseInt);
				} catch (NumberFormatException e) {
					enumerationOfString.add(value);
				}
			}
		}
		
		Interval interval = null;
		for (Integer integer : enumerationOfInt) {
			if(interval == null) {
				interval = new Interval();
				interval.setLow(integer.toString());
				result.add(interval);
			} else {
				if(interval.justNextOfHigh(integer)) {
					interval.setHigh(integer.toString());
				} else {
					interval = new Interval();
					interval.setLow(integer.toString());
					result.add(interval);
				}
			}
		}
		
		for (String string : enumerationOfString) {
			interval = new Interval();
			interval.setLow(string);
			result.add(interval);
		}
		
		return result;
	}
	
	public static String getMaxCharacters(String input, int max) {
		if(input == null) {
			return null;
		}
		if(input.length() > max) {
			return input.substring(0, max);
		} else {
			return input;
		}
	}
	
	public static String sortSelectListByText(String list, boolean isCompanyWithAgents, boolean onlyOptions, boolean doNotSortFirstEntry, boolean trimText, boolean ignoreCase) {
		if (!StringUtils.isEmpty(list)) {
			String firstPart = "";
			String middlePart = "";
			String lastPart = "";
			if (onlyOptions) {
				middlePart = list;
			} else {
				Matcher ma1 = Pattern.compile("(?is)(<select[^>]*>)(.*?)(</select>)").matcher(list.trim());
				if (ma1.matches()) {
					firstPart = ma1.group(1);
					middlePart = ma1.group(2);
					lastPart = ma1.group(3);
				}	
			}
			if (!"".equals(middlePart)) {
				boolean foundFirst = false;
				String firstOption = "";
				SortedMap<String, String> options = new TreeMap<String, String>();
				String patt = "";
				if (isCompanyWithAgents) {
					patt = "(?is)new\\s+companyWithAgents\\s*\\(\\s*'[^']*'\\s*,\\s*\\\"\\s*<option[^>]*>(.*?)</option>\\s*\\\"\\s*,[^)]+\\)\\s*,";
				} else {
					patt = "(?is)<option[^>]*>(.*?)</option>";
				}
				Matcher ma2 = Pattern.compile(patt).matcher(middlePart);
				while (ma2.find()) {
					if (doNotSortFirstEntry && !foundFirst) {
						firstOption = ma2.group(0);
						foundFirst = true;
					} else {
						String s = ma2.group(1);
						if (trimText) {
							s = s.trim();
						}		
						if (ignoreCase) {
							s = s.toLowerCase();
						}
						s = HTMLEntityDecode(s);
						options.put(s, ma2.group(0));
					}
				}
				StringBuilder sb = new StringBuilder();
				sb.append(firstPart);
				sb.append(firstOption);
				Iterator<Entry<String, String>> it = options.entrySet().iterator();
				while (it.hasNext()) {
					sb.append(it.next().getValue());
				}
				sb.append(lastPart);
				list = sb.toString();
			}
		}
		return list;
	}
	
}