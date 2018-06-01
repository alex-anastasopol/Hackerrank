package ro.cst.tsearch.search.name;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.Vector;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import ro.cst.tsearch.extractor.xml.GenericFunctions;
import ro.cst.tsearch.utils.StringUtils;
/**
 * 
 * @author FlorinC
 * 
 * prepare names for parsing
 *
 */
public class NameCleaner {
	private final static Vector<String> INVALID_PATTERNS = new Vector<String>();
	
	static {
		INVALID_PATTERNS.add(".*#[0-9]{2,}\\-[0-9]{2,}\\-[0-9]*.*");
		INVALID_PATTERNS.add("[0-9]+\\s+(N|S|W|E|NW|SW|SE|NE)\\s+.*");
		INVALID_PATTERNS.add(".*AIRPORT\\s*PULLING.*");
		INVALID_PATTERNS.add("UNIT\\s*[A-Z]*-*[0-9]+");
		INVALID_PATTERNS.add("TAX REPORTING #\\d+");
		INVALID_PATTERNS.add(".*\\s+GUARDIAN$");
		INVALID_PATTERNS.add(".*DEPT(\\.)?\\s*[0-9].*");
		INVALID_PATTERNS.add("DEPARTMENT OF CHEMISTRY");
		INVALID_PATTERNS.add(".*LOT\\s\\d+.*");
		INVALID_PATTERNS.add("WINDMILL VILLAGE");
		INVALID_PATTERNS.add(".*VILLAS");
		INVALID_PATTERNS.add(".*SEA ISLES");
		INVALID_PATTERNS.add(".*LAKES EDGE");
		INVALID_PATTERNS.add(".*(FOREST|TRAVEL) PARK");
		INVALID_PATTERNS.add("PARQUES DE.*");
		INVALID_PATTERNS.add(".*ON RIBBL.*");
		INVALID_PATTERNS.add("(HENLEY-ON|URB).*");
		INVALID_PATTERNS.add("LOVELAND COURTYARDS");
		INVALID_PATTERNS.add("%HOUSEHOLDER");
		INVALID_PATTERNS.add(".*(OAK FORREST|FERNDOWN|BARN SOUTHWATER)");
		INVALID_PATTERNS.add(".*BLVD$");
		//INVALID_PATTERNS.add("\\d+.*(DR|AVE)");
		INVALID_PATTERNS.add("\\d+.*\\s(RD|LN|ST|DR|AVE|BV|BLVD N|(UNIT( \\d+?)?)){1}$");
		INVALID_PATTERNS.add("%.*GENERAL MANAGER");
		INVALID_PATTERNS.add("%.*ASSET MG");
		INVALID_PATTERNS.add(".*VISTA DEL LARGO.*");
		INVALID_PATTERNS.add(".*HALL COTTAGES.*");
		INVALID_PATTERNS.add("VASTERNE HILL.*");
		INVALID_PATTERNS.add("(JAMAICA WAY|COLLINGHAM|WRIGHTINGTON BAR|TAYRANGA|BROADHURST LANE|CHRISTMAS COTTAGE BROOK ST)");
		INVALID_PATTERNS.add("^(\\w+\\s)?(DTD|UTD|TAD) \\d+(-|/)\\d+(-|/)\\d+$");
		
		//INVALID_PATTERNS.add(".*\\d+(\\s)?(ST).*");
		
	}
	
	private final static Vector<String> TRUSTEE = new Vector<String>();
	
	static {
		//TRUSTEE.add("TRUSTEE(E|S)");
		//TRUSTEE.add("\\sTRUSTEE");
		TRUSTEE.add("\\s(AS)?\\s*TRUSTEE\\s*(OF)\\s*");
		TRUSTEE.add("(CO-)?TRUSTEE(S)?");
		TRUSTEE.add(",?\\sTRUSTE(E|S)?\\b");
		//TRUSTEE.add("\\sTR(ST(EE(S)?)?|S)?\\b");
		TRUSTEE.add("\\sCO-TRUST(EES)?\\s*");
		//TRUSTEE.add("\\s+TRU\\b");  // TRU = TRUST
		TRUSTEE.add("\\sPR$");
		TRUSTEE.add("\\sPOA$");
		
	}
	
	public final static Vector<String> LIFEESTATE = new Vector<String>();
	
	static {
		//LIFEESTATE.add("\\bTRST\\b");
		//LIFEESTATE.add("\\bFAM\\b");
		//LIFEESTATE.add("\\bFAMILY LIV TRUST\\b");
		//LIFEESTATE.add("\\bFAM LIV\\b");
		//LIFEESTATE.add("\\bREV LIV TRUST\\b");
		//LIFEESTATE.add("\\bLIFE E[A-Z]*\\s*( OF)?");
		//LIFEESTATE.add("LIFE EST");
		//LIFEESTATE.add("LIFE ES");
		//LIFEESTATE.add("LIFE ESTAT");
		//LIFEESTATE.add("LIFE ESTATE");
		//LIFEESTATE.add("\\sESTATE\\s*");
		//LIFEESTATE.add("\\sESTATE\\s*(OF)?");
		//LIFEESTATE.add("\\bREV( LIV)?\\b");
		//LIFEESTATE.add("\\sLIVING TRUST\\s*");
		LIFEESTATE.add("\\bTL(/)?E\\b");
		//LIFEESTATE.add("\\bLE( &)?$");
		//LIFEESTATE.add("(,\\s)?\\bEST(\\.|\\b)+");
	}
	
	private final static Vector<String> HW = new Vector<String>();
	
	static {// be careful when you use this cleaning method and parseNameWilliamson from StringFormats. It depends on ETUX and ETVIR.
		HW.add("\\sH/W\\s*");
		HW.add("\\sW/H\\s*");
		HW.add("\\sH/$");
		HW.add("\\sET\\s*UX\\b");
		HW.add("(\\s|,)ET(\\s)?AL(S)?\\s*");
		HW.add("\\sET A(L)?\\s*");
		HW.add("\\sETA\\s*");
	}
	
	private final static Vector<String> HW_NEW = new Vector<String>();
	
	static {
		HW.add("\\sH/W\\s*");
		HW.add("\\sW/H\\s*");
		HW.add("\\sH/$");
	}
	
	private final static Vector<String> MISC = new Vector<String>();
	
	static {
		MISC.add("\\bPER REP\\b");
		MISC.add("\\bDR$");
		MISC.add("^DR\\s");
		MISC.add("\\bMD\\sPA$");
		MISC.add("\\bMD$");
		MISC.add(",\\sESQUIRE\\s*");
		MISC.add("\\sESQUIRE\\s*");
		MISC.add("\\sESQ\\s*");
		MISC.add("\\sPRES\\.\\b");
		MISC.add("\\s\\(PRES\\)\\s*");
		MISC.add(", PRES\\.\\s*");
		//MISC.add("\\(\\w+\\)"); //it's out because there might be a maiden name in here
		MISC.add("\\bMRS\\b");
		MISC.add("^AGENT FOR\\s+");
		MISC.add(",$");
		MISC.add("-$");
		MISC.add(" 111$");
		MISC.add(" OR$");
		MISC.add("\\sLUE/ROS");
		MISC.add("(\\s|/)?TIC$");
		MISC.add("(JOINT)?\\s+(TENANTS)?(JT)?(/)?RO(S)?$");
		MISC.add(",?\\s*\\bJT(\\s+TEN)?\\b$");
		MISC.add("\\s+B/E$");
		MISC.add("\\s+EM1");
		MISC.add("\\s+LI$");
		MISC.add("F/K/A\\s");
		MISC.add("BY ENT/ROS$");
		MISC.add("\\s*S PROP OW$");
		MISC.add("1/2 INT?");
		MISC.add("\\(?1/3 INT?\\)?");
		MISC.add("\\(?JT 2/9\\)?");
		//MISC.add("\\sREV\\s*");
		
	}
	
	private final static Vector<String> GLOBAL = new Vector<String>();
	
	static {
		GLOBAL.add("\\bU/?T/?A\\b");
		GLOBAL.add("\\bDA?TE?D\\s+[\\-/0-9]*\\b");
		GLOBAL.add("\\bPROPERTY TAX DEPT\\b");
		GLOBAL.add("\\bPLAINTIFF\\b");
		GLOBAL.add("\\bDEFENDENTS\\b");
		GLOBAL.add("\\bCLAIMANT\\b");
		GLOBAL.add("\\bOWNER\\b");
		//MISC.add("\\sREV\\s*");
		
	}
	public static String cleanFreeformName(String s) {
		s = s.toUpperCase();
		s = s.trim();
		s = removeExpressions(s, GLOBAL);
		return s;
	}
	/**
	 * removes from string s the regexp stored in vector e
	 * @param s - string to be cleaned
	 * @param e - expression to remove
	 * @return
	 */
	public static String removeExpressions(String s, Vector<String> e){
		Iterator<String> i  = e.iterator();
		while (i.hasNext()){
			s = s.replaceAll(i.next(), " ");
		}
 		return s;
	}
	
	/**
	 * If (word) is last name, remove (), else remove (word)
	 * @param s
	 * @return
	 */
	 
	 
	public static String paranthesisFix(String s){
		Pattern pa = Pattern.compile("\\(\\w+\\)");
		Matcher ma = pa.matcher(s);
		while (ma.find()){
			String name = ma.group().replace("(", "").replace(")", "");
			if (NameFactory.getInstance().isLast(name)){
				s = s.replace("(" + name + ")", name);
			} else {
				s= s.replace("(" + name + ")", "");
			}
		}
		return s;
	}
	
	/**
	 * removed the general expressions from string s. This method delete TRUSTEE words. It's better to use cleanNameNew(String s)
	 * @param s - string to be cleaned
	 * @return
	 * @deprecated
	 */
	public static String cleanName(String s){
		s = s.toUpperCase();
		s = s.trim();
		s = removeExpressions(s, TRUSTEE);
		s = removeExpressions(s, HW);
		s = removeExpressions(s, MISC);
		//s = removeExpressions(s, LIFEESTATE);
		return s;
	}
	
	/**
	 * correct misspelled tokens
	 * @param s - string to be corrected
	 * @return
	 */
	public static String correctName(String s){
		s = s.replaceAll("(?is)\\b(ET)\\s+(UX|AL|VIR)\\b", "$1$2");
		s = s.replaceAll("(?is)\\b(LIVING)\\s+(T)\\b", "$1 TRUST");

		return s.trim();
	}
	
	/**
	 * removed the general expressions from string s; used for new parsing of TRUSTEE, ETAL, ETUX, ETVIR
	 * @param s - string to be cleaned
	 * @return
	 */
	public static String cleanNameNew(String s){
		s = s.toUpperCase();
		s = s.trim();
		s = s.replaceAll("(?is)\\b(ET)\\s+(AL|UX|VIR)\\b", "$1$2");
		s = s.replaceAll("(?is)\\b[\\d/]{2,}", "");
		s = removeExpressions(s, HW_NEW);
		s = removeExpressions(s, MISC);
		return s;
	}
	
	public static String cleanName(String s, boolean ignoreIfCompany){
		if(ignoreIfCompany && NameUtils.isCompany(s)) {
			return s;
		} else {
			return cleanName(s);
		}
	}
	
	/**
	 * cleans s of the generic expressions plus the ones in extra
	 * @param s
	 * @param extra
	 * @return
	 * @deprecated
	 */
	public static String cleanName(String s, Vector<String> extra){
		return cleanName(s, extra, false);
	}
	public static String cleanName(String s, Vector<String> extra, boolean newParsing){
		if (newParsing){
			s = cleanNameNew(s);
		} else {
			s = cleanName(s);
		}
		s = removeExpressions(s, extra);
		return s;
	}
	
	/**
	 * replace second , with , in string with 3 commas
	 * MACALUSO,FRANK,MACALUSO,M
	 * @return
	 */
	public static String multipleCommas(String s){
		String[] t = s.split(",");
		if (t.length == 4){
			if (!s.contains("&")){
				s = t[0] + ", " + t[1] + " & " + t[2] + ", " + t[3];
			}
		}
		return s;
	}
	
	/**
	 * Cleans and apply fixes for parsing
	 * @param s
	 * @param extra
	 * @return
	 * @deprecated
	 */
	public static String cleanNameAndFix(String s, Vector<String> extra){
		return cleanNameAndFix(s, extra, false);
	}
	
	
	/**
	 * if you use this function, you MUST use @see ro.cst.tsearch.search.name.NameCleaner.removeUnderscore(String[])
	 * @param s
	 * @param extra
	 * @param newParsing
	 * @return
	 */
	public static String cleanNameAndFix(String s, Vector<String> extra, boolean newParsing){
		s = cleanName(s, extra, newParsing);
		s = s.replaceFirst("(\\s)+([A-Z]{1})\\s?-\\s?([A-Z]+|&)", "$1$2 $3");
		s = s.replaceAll("=", " ");
		s = fixScotishLikeNames(s);
		s = multipleCommas(s);
		s = composedNames(s);
		return s.trim();
	}

	/**
	 * Cleans and apply fixes for parsing
	 * @param s
	 * @param extra - extra patterns to be removed
	 * @return
	 * @deprecated
	 */
	public static String cleanNameAndFixNoScotish(String s, Vector<String> extra){
		return cleanNameAndFixNoScotish(s, extra, false);
	}
		
	public static String cleanNameAndFixNoScotish(String s, Vector<String> extra, boolean newParsing){
		s = cleanName(s, extra, newParsing);
		s = s.replaceFirst("(\\s)+([A-Z]{1})\\s?-\\s?([A-Z]+|&)", "$1$2 $3");
		s = s.replaceAll("=", " ");	
		s = multipleCommas(s);
		s = composedNames(s);
		return s.trim();
	}	
	
	/**
	 * if use this function, you MUST use @see ro.cst.tsearch.search.name.NameCleaner.removeUnderscore(String[])
	 * 
	 * @param s
	 * @return
	 */
	public static String composedNames(String s){
		s = s.replaceAll("\\b(JO) (ANN(A)?)\\b", "$1_!_!$2");
		s = s.replaceAll("\\bMARY JO\\b", "MARY_!_!JO");
		return s;
	}
	
	/**
	 * add a space after comma in names like:
	 * 
	 * @param s
	 * @return
	 */
	public static String lfmwfwmComma(String s){
		s = s.replaceFirst("(\\w),(\\w)", "$1, $2");
		return s;
	}
	/**
	 * replace underscores with blank from tokenized names 
	 * (we replace " " with _ for composed last names)
	 * @param names
	 * @return
	 */
	public static String[] removeUnderscore(String[] names){
		for (int i=0; i<names.length; i++){
			names[i] = removeUnderscore(names[i]);
		}
		return names;
	}
	
	public static String removeUnderscore(String s){
		s = s.replaceAll("_!_!", " "); //for composed names
		s = s.replaceAll("____", "-");
		s = s.replaceAll("__", "'");
		s = s.replaceFirst("_", " ");
		return s;
	}
	/**
	 * swap middle name with first name if needed
	 * @param names
	 * @return
	 */
	public static String[] middleNameFix(String[] names){
		String[] firstMiddle;
		for (int i = 0; i<names.length; i+=3){
			firstMiddle = middleNameFix(names[i], names[i+1]);
			names[i] = firstMiddle[0];
			names[i+1] = firstMiddle[1];
		}
		return names;
	}
	
	
	/**
	 * (if first is an initial and empty) and middle has only one word,
	 *  will swap first with that word from middle    
	 *  
	 */
	public static String[] middleNameFix(String first, String middle){
		Matcher m;
		if (first.length() <= 1){
				m = Pattern.compile(".*?(\\w{3,}).*?").matcher(middle);
				if (m.find() && !m.find()){
					//there is only one group of more then 2 letters in middle
					m.reset();
					m.find();
					String s = m.group(1);
					if (!GenericFunctions.nameSuffix3.matcher(s).matches()){
						middle = middle.replaceAll(s, "");
						middle = first + " " + middle;
						first = s;
					}
				}
		}
		return new String[]{first.trim().replaceAll("\\s+", " "),
				middle.trim().replaceAll("\\s+", " ")};
	}
	
	/**
	 * A D Lindsay fix
	 * if last name and another one are initials then 
	 * make the only name not initial last name
	 * @param names
	 * @return
	 */
	public static String[] lastNameFix(String[] names){
		//A D Lindsay fix
		String tmp;
		for (int i = 0; i<names.length; i+=3){
			if (names[i+2].length() == 1 &&
					(names[i].length() > 1 || names[i+1].length() > 1)){
				int swapI = (names[i].length() > 1) ? i : i+1;
				tmp = names[swapI];
				names[swapI] = names[i+2];
				names[i+2] = tmp;
			}
		}
		return names;
	}
	
	/**
	 * swap first and last if they don't fit into the lists of last and first names
	 * @param names
	 * @return
	 */
	public static String[] lastNameSwap(String[] names){
		String tmp;
		for (int i = 0; i<names.length; i+=3){
			if (NameFactory.getInstance().isLast(names[i]) 
				&& NameFactory.getInstance().isFirstMiddle(names[i+2]) 
				&& 	(!NameFactory.getInstance().isLast(names[i+2])
				 || ! NameFactory.getInstance().isFirstMiddle(names[i]))){
					tmp = names[i];
					names[i] = names[i+2];
					names[i+2] = tmp;
			}
			
		}
		return names;
	}
	
	/**
	 * swap first and last if they don't fit into the lists of last and first names
	 * @param names
	 * @return
	 */
	public static String[] lastNameSwap2(String[] names){
		String tmp;
		for (int i = 0; i<names.length; i+=3){
			if ((NameFactory.getInstance().isLastOnly(names[i]) 
				&& NameFactory.getInstance().isFirstMiddle(names[i+2]))
				|| (NameFactory.getInstance().isLast(names[i])
					&& NameFactory.getInstance().isFirstMiddleOnly(names[i+2]))){
					tmp = names[i];
					names[i] = names[i+2];
					names[i+2] = tmp;
			}
			
		}
		return names;
	}
	
	/**
	 *KARTARIK, MARK H & MARY JO N - in composedNames I replaced MARY JO with MARY_JO
	 * @param names
	 * @return
	 */
	public static String[] composedNamesFix(String[] names){
		for(int i=0; i<names.length; i+=3){
			if (names[i].matches(".*[A-Z]_!_![A-Z].*")){
				String[] s = names[i].split("_!_!");
				names[i] = s[0];
				for(int ii = 1; ii<s.length; ii++){
					names[i+1] = s[ii] + " " + names[i+1];
				}
				names[i+1]  = names[i+1].trim();
			}
		}
		return names;
	}
	
	
	public static String[] middleNameSwap(String[] names){
		for(int i=0; i<names.length; i+=3){
			if(NameFactory.getInstance().isLastOnly(names[i])
				&& NameFactory.getInstance().isFirstMiddleOnly(names[i+1])){
				String tmp = names[i];
				names[i] = names[i+1];
				names[i+1] = tmp;
			}
		}
		return names;
	}
	/**
	 * after parsing touches
	 * @param names
	 * @return
	 */
	public static String[] tokenNameAdjustment(String[] names){
		//names = removeUnderscore(names);
		names = composedNamesFix(names);
		names = lastNameFix(names);
		names = middleNameFix(names);
		return names;
	}
	
	/**
	 * Split names line JESS E SMITH & BETSY ARNOLD in two lines
	 * @param lines - strings of names
	 */
	public static String[] splitName(String line){
		//JESS E SMITH & BETSY ARNOLD
		//WRIGHT, JAMES & SMITH, NELSON
		String[] tmps = {line};
		Pattern pa = Pattern.compile("\\w+\\s+\\w+\\s+\\w+\\s+&\\s+\\w+(\\s+\\w+)*\\s+\\w+");
		Pattern pa2 = Pattern.compile("\\w+,\\s+\\w+\\s+&\\s+\\w+,\\s+\\w+");
		if (pa.matcher(line).matches() || pa2.matcher(line).matches()){
			tmps = line.split("&");
		}
		return tmps;
		
	}
	
	/**
	 * Split names line JESS E SMITH & BETSY ARNOLD in two lines
	 * @param lines - strings of names
	 */
	public static String[] splitName(String[] lines, Vector<String> excludeCompanyWords){
		
		Vector<String> tmpLines = new Vector<String>();
		String[] tmpL;
		for (String s: lines){
			if (!NameUtils.isCompany(s, excludeCompanyWords, true)){
				String[] tmps = splitName(s);
				for( String s1:tmps){
					tmpLines.add(s1);
				}
			} else {
				tmpLines.add(s);
			}
		}
		tmpL = new String[tmpLines.size()];
		for(int i = 0; i< tmpLines.size(); i++){
			tmpL[i] = tmpLines.get(i);
		}
		return tmpL;
	}
	
	/**
	 * Split names line JESS E SMITH & BETSY ARNOLD in two lines<br>
	 * done with arrayLists
	 * @param lines - strings of names
	 */
	public static ArrayList<String> splitName(ArrayList<String> lines, Vector<String> excludeCompanyWords){
		ArrayList<String> tmpLines = new ArrayList<String>();
		for (String s: lines){
			if (!NameUtils.isCompany(s, excludeCompanyWords, true)){
				String[] tmps = splitName(s);
				for( String s1:tmps){
					tmpLines.add(s1);
				}
			} else {
				tmpLines.add(s);
			}
		}
		return tmpLines;
	}	
	
	/**
	 * replace " "  from composed last names with _, ' with __
	 * 
	 * if you use this function, you MUST use @see ro.cst.tsearch.search.name.NameCleaner.removeUnderscore(String[])
	 * 
	 * @param s
	 * @return
	 */
	public final static String fixScotishLikeNames(String s){
		Matcher ma;
		//\\s before
		ma = Pattern.compile(".*[\\s%]+(LA|LE|MAC|SAN|VAN|VON|ST|MC|DE(?: LA)?|DI|EL|DEL) (\\w{3,}).*").matcher(s);
		if (ma.matches()){
			String second = ma.group(2);
			if (!NameFactory.isSuffix(second)){
				s = s.replaceAll("[\\s%](LA|LE|DE|MAC|SAN|VAN|VON|ST|MC|DE(?: LA)?|DI|EL|DEL) (\\w{3,})", " " + ma.group(1) + "_" + second);
			}
		}
		//beginning of the string
		ma = Pattern.compile("^(LA|LE|DE|MAC|O|VAN|SAN|VON|ST|MC|DE(?: LA)?|DI|EL|DEL) (\\w{3,}).*").matcher(s);
		if (ma.matches()){
			String second = ma.group(2);
			if (!NameFactory.isSuffix(second)){
				s = s.replaceFirst("(LA|LE|DE|MAC|O|VAN|SAN|VON|ST|MC|DE(?: LA)?|DI|EL|DEL) (\\w{3,})", ma.group(1) + "_" + second);
			}
		}
		s = s.replaceAll("'", "__");
		s = s.replaceAll("-", "____");
		
		return s;
	}
	
	/**
	 * FFML mane fix
	 * If names have 2 last names owner names try and merge them
	 */
	//SMITH ALLEN PAUL L pe FLDuval (firstname is smith allen)
	public static String ffmlFix(String s){
		Matcher ma = GenericFunctions.LLFM.matcher(s);
		if (ma.matches()){
			String f1 = ma.group(1);
			String f2 = ma.group(2);
			String f3 = ma.group(3);
			if (NameFactory.getInstance().isLast(f1) && NameFactory.getInstance().isLast(f2)
					&& !NameFactory.getInstance().isSameSex(f2, f3)){
				s = ma.replaceFirst("$1_$2 $3 $4"); 
			}
		}
		return s;
	}
	
	/**
	 * test a string against a predefined vector of patterns.
	 * @param s
	 * @return
	 */
	public final static boolean isValidName(String s){
		for (String s1 : INVALID_PATTERNS){
			if (s.matches(s1)){
				return false;
			}
		}
		return true;
	}
	
	/**
	 * test a string against a predefined vector of patterns.
	 * @param s - name to be tested
	 * @param invalidPatterns - extra invalid patterns
	 * @return
	 */
	public final static boolean isValidName(String s, Vector<String> invalidPatterns){
		for(int i = 0; i<invalidPatterns.size(); i++){
			if (s.matches(invalidPatterns.get(i))){
				return false;
			}
		}
		return isValidName(s);
	}
	
	public static String[] mergeNames(String[] a, String separator){
		if (a.length==0){
			return a;
		}
		String[] tempStr = new String[a.length];
		int j = 0;
		int i;
		tempStr[0] = a[0];

		for (i = 1; i < tempStr.length; i++) {
				boolean merge = false;
				if (i>=1){
					//if(a[i - 1].matches("\\w+,\\s+\\w+\\s+\\w+\\s+&")){ //SMITH, WESLEY LOUIS & on the first row
						if (a[i].matches("(\\w+)\\s(\\w){1}") || a[i].matches("^\\w+$") || a[i].matches("(\\w){1}\\s(\\w)+")){ //SHANNON L on the second row or a single word
							merge = true;
						} else if (a[i].matches("(\\w+)\\s(\\w)+")){ //ELIZABETH ANN on the second row
							String[] n = a[i].split("\\s");
							if (NameFactory.getInstance().isFemale(n[0]) &&NameFactory.getInstance().isFemale(n[1])
								|| NameFactory.getInstance().isMale(n[0]) && NameFactory.getInstance().isMale(n[1])){
								//ELIZABETH ANN
								//CATHERINE CRAIG
								merge = true;
							}
						}
					//}
					
				}
				if (merge) {
					tempStr[i - j - 1] += separator + a[i];
					j++;
				} else {
					tempStr[i - j] = a[i];
				}

		}
		//return an array without null components
		String[] tempStr2;
		if (j > 0){
			tempStr2 = new String[tempStr.length - j];
			for (i=0; i<tempStr2.length; i++){
				tempStr2[i] = tempStr[i];
			}
			return tempStr2;
		}
		return tempStr;
	}
	
	public static String[] cleanNameAndFix(String[] a, Vector<String> extra){
		String[] tmpStr = new String[a.length];
		for(int i = 0; i<a.length; i++){
			tmpStr[i] = cleanNameAndFix(a[i], extra);
		}
		return tmpStr;
	}
	
	public static String processMiddleName(String midName) {
    	if(midName.matches("[a-zA-Z]")) {
    		return midName + ".";
    	}
    	return midName;
    }
    
    public static String processNameSuffix(String suffix) {
    	if(StringUtils.isNotEmpty(suffix) && !suffix.matches("[IVX]+") && !suffix.endsWith(".")) {
    		return suffix + ".";
    	}
    	return suffix;
    }
	
	public final  static void main(String[] arg){
		System.out.println(fixScotishLikeNames("LAWRENCE, RUSSELL K H/W"));
		System.out.println(fixScotishLikeNames("C/O PETER BLAETZ, ESQUIRE"));
		System.out.println(fixScotishLikeNames("O sullivan mama & o'sullivan tata"));
		System.out.println(fixScotishLikeNames("MAC DANIELS, PHYLLIS H/W LIFE"));
		System.out.println(fixScotishLikeNames(",WILMEN MAC CUMBER,WILMEN & MAC CUMBER,CHARLOTTE"));

	}
	
}
