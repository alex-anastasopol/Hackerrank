package ro.cst.tsearch.search.name;

import java.util.HashMap;
import java.util.Iterator;
import java.util.regex.Pattern;

import org.apache.log4j.Category;

/**
 * NameNormalizer
 *
 * Name normalization class.
 *
 * @author catalinc
 */
public class NameNormalizer {	
	
	protected static final Category logger= Category.getInstance(NameNormalizer.class.getName());

	/**
	 * Name prefixes table.
	 */
	public static HashMap namePrefixes;
	/**
	 * Name sufixes table.
	 */
	public static  HashMap nameSuffixes;
	/**
	 * Name degrees table.
	 */
	public static HashMap nameDegrees;
	/**
	 * Numbers map.
	 */
	public static HashMap numbers;
	/**
	 * Company tokens table. 
	 */	
	public static HashMap companyTokens;
	/**
	 * Spouse delimiter.
	 */
	public static String SPOUSE_DELIM	= "&";	 
	
	static {		
		//fill name prefixes expanded table		
		namePrefixes = new HashMap();
		
		namePrefixes.put("MR","MR");
		namePrefixes.put("MISTER","MR");
		namePrefixes.put("MRS","MRS");
		namePrefixes.put("MISS","MRS");
		namePrefixes.put("MS","MS");
		namePrefixes.put("MISSES","MS");
		namePrefixes.put("HON","HON");
		namePrefixes.put("THE HONORABLE","HON");
		namePrefixes.put("HONORABLE","HON");
		namePrefixes.put("REV","REV");
		namePrefixes.put("THE REVEREND","REV");
		namePrefixes.put("REVEREND","REV");
		namePrefixes.put("DR","DOCTOR");
		namePrefixes.put("DOCTOR","DR");
		namePrefixes.put("GEN","GEN");
		namePrefixes.put("GENERAL","GEN");
		namePrefixes.put("GOV","GOV");
		namePrefixes.put("GOVERNOR","GOV");
		namePrefixes.put("SEN","SEN");
		namePrefixes.put("SENATOR","SEN");		
		namePrefixes.put("REP","REP");
		namePrefixes.put("REPRESENTATIVE","REP");		
		
		//fill name suffixes table
		nameSuffixes = new HashMap();
		
		nameSuffixes.put("JR","JR");
		nameSuffixes.put("JUNIOR","JR");
		nameSuffixes.put("SR","SR");
		nameSuffixes.put("SENIOR","SR");
				
		//fill name degree table
		nameDegrees = new HashMap();
		
		nameDegrees.put("PHD","PHD");
		nameDegrees.put("PHILOSOPHIAE DOCTOR","PHD");
		nameDegrees.put("DOCTOR OF PHILOSOPHY","PHD");		
		nameDegrees.put("BS","BS");
		nameDegrees.put("BACHELOR OF SCIENCE","BS");
		nameDegrees.put("BA","BA");
		nameDegrees.put("BACHELOR OF ARTS","BA");
		nameDegrees.put("MA","MA");
		nameDegrees.put("MASTER OF ARTS","MA");
		nameDegrees.put("MS","MS");								
		nameDegrees.put("MASTER OF SCIENCE","MS");
		nameDegrees.put("MBA","MBA");								
		nameDegrees.put("MASTER OF BUSINESS ADMINISTRATION","MBA");
		nameDegrees.put("MD","MD");
		nameDegrees.put("DOCTOR OF MEDICINE","MD");
		nameDegrees.put("MED","MED");
		nameDegrees.put("MASTER OF EDUCATION","MED");
		nameDegrees.put("MPA","MPA");
		nameDegrees.put("MASTER OF PUBLIC ADMINISTRATION","MPA");
		nameDegrees.put("MTPW","MTPW");
		nameDegrees.put("MASTER OF TECHNICAL AND PROFESSIONAL WRITING","MTPW");
		nameDegrees.put("PHARMD","PHARMD");
		nameDegrees.put("DOCTOR OF PHARMACY","PHARMD");			

		// fill numbers table
		numbers = new HashMap();
		
		numbers.put("FIRST", "1");
		numbers.put("ONE", "1");
		numbers.put("TEN", "10");
		numbers.put("TENTH", "10");
		numbers.put("ELEVEN", "11");
		numbers.put("ELEVENTH", "11");
		numbers.put("TWELFTH", "12");
		numbers.put("TWELVE", "12");
		numbers.put("THIRTEEN", "13");
		numbers.put("THIRTEENTH", "13");
		numbers.put("FOURTEEN", "14");
		numbers.put("FOURTEENTH", "14");
		numbers.put("FIFTEEN", "15");
		numbers.put("FIFTEENTH", "15");
		numbers.put("SIXTEEN", "16");
		numbers.put("SIXTEENTH", "16");
		numbers.put("SEVENTEEN", "17");
		numbers.put("SEVENTEENTH", "17");
		numbers.put("EIGHTEEN", "18");
		numbers.put("EIGHTEENTH", "18");
		numbers.put("NINETEEN", "19");
		numbers.put("NINETEENTH", "19");
		numbers.put("SECOND", "2");
		numbers.put("TWO", "2");
		numbers.put("TWENTIETH", "20");
		numbers.put("TWENTY", "20");
		numbers.put("THIRD", "3");
		numbers.put("THREE", "3");
		numbers.put("FOUR", "4");
		numbers.put("FOURTH", "4");
		numbers.put("FIFTH", "5");
		numbers.put("FIVE", "5");
		numbers.put("SIX", "6");
		numbers.put("SIXTH", "6");
		numbers.put("SEVEN", "7");
		numbers.put("SEVENTH", "7");
		numbers.put("EIGHT", "8");
		numbers.put("EIGHTH", "8");
		numbers.put("NINE", "9");
		numbers.put("NINTH", "9");
		
		// fill company tokens table
		companyTokens = new HashMap();

		companyTokens.put("ACADEMY","ACADEMY");
		companyTokens.put("ACCOUNTANTS","ACCOUNTANTS");
		companyTokens.put("ADV","ADV");
		companyTokens.put("ADVANCE PLATING","ADV");
		companyTokens.put("AFFAIRS","AFFAIRS"); 		
		companyTokens.put("AFFILIATES","AFFILIATES");
		companyTokens.put("AGCY","AGCY");
		companyTokens.put("AGENCY","AGCY");
		companyTokens.put("ALTERATION","ALTERATION");
		companyTokens.put("APARTMENTS","APARTMENTS");
		companyTokens.put("APPLIANCES","APPLIANCES");
		companyTokens.put("APPLICATION","APPLICATION");
		companyTokens.put("APPRAIS","APPRAIS");
		companyTokens.put("APPRAISAL","APPRAISAL");
		companyTokens.put("APPRAISALS","APPRAISAL");
		companyTokens.put("APPRAISELS","APPRAISAL");
		companyTokens.put("APPT","APPT");
		companyTokens.put("APTS","APTS");
		companyTokens.put("ARCHITECTS","ARCHITECTS");
		companyTokens.put("ASSC","ASSC");
		companyTokens.put("ASSN","ASSN");
		companyTokens.put("ASSOC","ASSOC");
		companyTokens.put("ASSOC","ASSOC");
		companyTokens.put("ASSOCIATES","ASSOC");
		companyTokens.put("ASSOCIATION","ASSOC");
		companyTokens.put("ATM","ATM");
		companyTokens.put("ATTORNEY","ATTORNEY");
		companyTokens.put("ATTY","ATTY");
		companyTokens.put("BANK","BANK");
		companyTokens.put("BONDING","BONDING");
		companyTokens.put("BROKERAGE","BROKERAGE");
		companyTokens.put("BUREAU","BUREAU");
		companyTokens.put("BUSINESS","BUSINESS");
		companyTokens.put("C P A","C P A");
		companyTokens.put("CAPITAL","CAPITAL");
		companyTokens.put("CARE","CARE");
		companyTokens.put("CEN","CEN");
		companyTokens.put("CENTER","CENTER");
		companyTokens.put("CH","CH");
		companyTokens.put("CHANGE","CHANGE");
		companyTokens.put("CHECK CASHING","CHECK CASHING");
		companyTokens.put("CHFC","CHFC");
		companyTokens.put("CHURCH","CHURCH");
		companyTokens.put("CLEANERS","CLEANERS");
		companyTokens.put("CLEANING","CLEANING");
		companyTokens.put("CLINIC","CLINIC");
		companyTokens.put("CLU","CLU");
		companyTokens.put("CLUB","CLUB");
		companyTokens.put("CNTR","CNTR");
		companyTokens.put("CO OFF","CO OFF");
		companyTokens.put("CO","CO");
		companyTokens.put("COLLEGE-STATE","COLLEGE-STATE");
		companyTokens.put("COMMERCIAL OPERATIONS","COMMERCIAL OPERATIONS");
		companyTokens.put("COMMITTEE","COMMITTEE");
		companyTokens.put("COMMUNICATIONS","COMMUNICATIONS");
		companyTokens.put("COMMUNITY","COMMUNITY");
		companyTokens.put("COMP","COMP");
		companyTokens.put("COMPANIES","COMP");
		companyTokens.put("COMPANY","COMP");
		companyTokens.put("CONCEPTS","CONCEPTS");
		companyTokens.put("CONDOMINIUM","CONDOMINIUM");
		companyTokens.put("CONDOMINIUMS","CONDOMINIUM");
        companyTokens.put( "CONSTR", "CONSTR" );
		companyTokens.put("CONSULT","CONSULT");
		companyTokens.put("CONSULTANT","CONSULT");
		companyTokens.put("CONSULTANTS","CONSULT");
		companyTokens.put("CONSULTING","CONSULT");
		companyTokens.put("CONTR","CONTR");
		companyTokens.put("CONTRACT","CONTRACT");
		companyTokens.put("CONTRACTORS","CONTRACTORS");
		companyTokens.put("CONTROL","CONTROL");
		companyTokens.put("CORP","CORP");
		companyTokens.put("CORPORATION","CORP");
		companyTokens.put("CO-TRS","CO-TRS");
		companyTokens.put("CPA","CPA");
		companyTokens.put("CRC","CRC");
		companyTokens.put("CTR","CTR");
		companyTokens.put("D D S","DDS");
		companyTokens.put("D F","DF");
		companyTokens.put("DBA","DBA");
		companyTokens.put("DDS","DDS");
		companyTokens.put("DEALER","DEALER");
		companyTokens.put("DEPOT","DEPOT");
		companyTokens.put("DETECTIVE","DETECTIVE");
		companyTokens.put("DEV","DEV");
		companyTokens.put("DEVELOPMENT","DEV");
		companyTokens.put("DIRECTORS","DIRECTORS");
		companyTokens.put("DISPENSARY","DISPENSARY");
		companyTokens.put("DIST","DIST");
		companyTokens.put("DISTRIBUTORS","DIST");
		companyTokens.put("DIV","DIV");
		companyTokens.put("DIVISION","DIV");
		companyTokens.put("DOKTORS","DOKTORS");
		companyTokens.put("ENTERPRISE","ENTERPRISE");
		companyTokens.put("ENTERPRISES","ENTERPRISE");
		companyTokens.put("EQUIPMENT","EQUIPMENT");
		companyTokens.put("ESTATE","EST");
		companyTokens.put("ETC","ETC");
		companyTokens.put("EXECUTOR","EXECUTOR");
		companyTokens.put("FAM LTD PTNERSP","FLP");
		companyTokens.put("FAMILY LMTD PARTNRSHP","FLP");
		companyTokens.put("FAMILY TRUST","FAMILY TRUST");
		companyTokens.put("FEDERAL","FEDERAL");
		companyTokens.put("FINANCE","FINANCE");
		companyTokens.put("FINANCIAL","FINANCIAL");
		companyTokens.put("FOOD","FOOD");
		companyTokens.put("FOODS","FOODS");
		companyTokens.put("FOUNDATION","FOUNDATION");
		companyTokens.put("GP","GP");
		companyTokens.put("GALLERIES","GALLERIES");
		companyTokens.put("GALLERY","GALLERY");
		companyTokens.put("GARAGE","GARAGE");
		companyTokens.put("GARDENS","GARDENS");
		companyTokens.put("GRAPHICS","GRAPHICS");
		companyTokens.put("GROUP","GROUP");
		companyTokens.put("HOME","HOME");
		companyTokens.put("HOMES","HOMES");
		companyTokens.put("HOUSING","HOUSING");
		companyTokens.put("IMPORTS","IMPORTS");
		companyTokens.put("IMPROVEMENTS","IMPROVEMENTS");
		companyTokens.put("INC","INC");
		companyTokens.put("INDUST","INDUST");
		companyTokens.put("INDUSTRIAL","INDUST");
		companyTokens.put("INDUSTRIES","INDUST");
		companyTokens.put("INN","INN");
		companyTokens.put("INS","INS");
		companyTokens.put("INST","INS");
		companyTokens.put("INSTALLATIONS","INS");
		companyTokens.put("INSURANCE","INSURANCE");
		companyTokens.put("INTERIORS","INTERIORS");
		companyTokens.put("INTERNATIONAL","INTERNATIONAL");
		companyTokens.put("INVESTIGATIONS","INVESTIGATIONS");
		companyTokens.put("INVESTMENT","INVESTMENT");
		companyTokens.put("JOINT VENTURE","JOINT VENTURE");
		companyTokens.put("L P","LP");
		companyTokens.put("LP","LP");
		companyTokens.put("LAB","LAB");
		companyTokens.put("LABORATORY","LAB");
		companyTokens.put("LEASING","LEASING");
		companyTokens.put("LIFE ESTATE","LIFE EST");
		companyTokens.put("LIMITED","LIMITED");
		companyTokens.put("LLC","LLC");
		companyTokens.put("LLCAND","LLCAND");
		companyTokens.put("LTD","LTD");
		companyTokens.put("LTM","LTM");
		companyTokens.put("MAINT","MAINT");
		companyTokens.put("MAINTENANCE","MAINT");
		companyTokens.put("MALL","MALL");
		companyTokens.put("MANAGEMENT","MANAGEMENT");
		companyTokens.put("MANUFACTURING","MANUFACTURING");
		companyTokens.put("MARKET","MKT");
		companyTokens.put("MERCHANDISE","MERCHANDISE");
		companyTokens.put("MIP","MIP");
		companyTokens.put("MKT","MKT");
		companyTokens.put("MUSEUM","MUSEUM");
		companyTokens.put("MUTUAL","MUTUAL");
		companyTokens.put("NA","NA");
		companyTokens.put("NATIONAL","NATIONAL");
		companyTokens.put("OFFICE","OFFICE");
		companyTokens.put("OUTLET","OUTLET");
		companyTokens.put("P C","P C");
		companyTokens.put("PA","PA");
		companyTokens.put("PART","PART");
		companyTokens.put("PARTNERS I","PART");
		companyTokens.put("PARTNERSHIP","PART");
		companyTokens.put("PARTNRSHP","PART");
		companyTokens.put("PC","PC");
		companyTokens.put("PHOTO","PHOTO");
		companyTokens.put("PI","PI");
		companyTokens.put("PLC","PLC");
		companyTokens.put("PLLC","PLLC");
		companyTokens.put("PPT","PPT");
		companyTokens.put("PRODUCTS","PRODUCTS");
		companyTokens.put("PROFESSIONAL","PROFESSIONAL");
		companyTokens.put("PROFIT","PROFIT");
		companyTokens.put("PROPERTIES","PROPERTIES");
		companyTokens.put("PRTNSHP","PART");
		companyTokens.put("PTNRS","PART");
		companyTokens.put("PTNRSHP","PART");
		companyTokens.put("PTNSHP","PART");
		companyTokens.put("PURCHASE","PURCHASE");
		companyTokens.put("RC","RC");
		companyTokens.put("REAL ESTATE","REAL ESTATE");
		companyTokens.put("REALTOR","REALTOR");
		companyTokens.put("REALTORS","REALTORS");
		companyTokens.put("REALTY","REALTY");
		companyTokens.put("REGENTS UNIV","REGENTS UNIV");
		companyTokens.put("RENT-A-CAR","RENTALS");
		companyTokens.put("RENTALS","RENTALS");
		companyTokens.put("REPAIR","REPAIR");
		companyTokens.put("REPAIRS","REPAIRS");
		companyTokens.put("RES","RES");
		companyTokens.put("RESEARCH","RES");
		companyTokens.put("RESOURCES","RESOURCES");
		companyTokens.put("RETAIL","RETAIL");
		companyTokens.put("REV LIV TR","REV LIV TR");
		companyTokens.put("SALE","SALE");
		companyTokens.put("SALES","SALES");
		companyTokens.put("SALO","SALON");
		companyTokens.put("SALON","SALON");
		companyTokens.put("SELF","SELF");
		companyTokens.put("SELLERS","SELLERS");
		companyTokens.put("SERV","SERV");
		companyTokens.put("SERVICE","SERV");
		companyTokens.put("SERVICES","SERV");
		companyTokens.put("SHOP","SHOP");
		companyTokens.put("SHOPPE","SHOPPE");
		companyTokens.put("SHOWCASE","SHOWCASE");
		companyTokens.put("SHOWROOM","SHOWROOM");
		companyTokens.put("SN","SN");
		companyTokens.put("SOLUTIONS","SOLUTIONS");
		companyTokens.put("SPA","SPA");
		companyTokens.put("SPACE","SPACE");
		companyTokens.put("SPEC","SPEC");
		companyTokens.put("SPECIALISTS","SPEC");
		companyTokens.put("SPECIALITY","SPEC");
		companyTokens.put("SPECIALTIES","SPEC");
		companyTokens.put("SRVPCORP","SRVPCORP");
		companyTokens.put("STATION","STATION");
		companyTokens.put("STORAGE","STORAGE");	
		companyTokens.put("STORE","STORE");
		companyTokens.put("STUDIO","STUDIO");
		companyTokens.put("STUDIOS","STUDIO");
		companyTokens.put("SUITES","SUITES");
		companyTokens.put("SUPPLIER","SUPPLIER");
		companyTokens.put("SUPPLIERS","SUPPLIERS");
		companyTokens.put("SUPPLIES","SUPPLIES");
		companyTokens.put("SUPPLY","SUPPLY");
		companyTokens.put("SVC","SVC");
		companyTokens.put("SVCS","SVCS");
		companyTokens.put("SYS","SYS");
		companyTokens.put("TECHNOLOGIES","TECHNOLOGIES");
		companyTokens.put("TRADE","TRADE");
		companyTokens.put("TRADES","TRADES");
		companyTokens.put("TRADING","TRADING");
		companyTokens.put("TRANSPORTATION","TRANSPORTATION");
		companyTokens.put("TRAVEL","TRAVEL");
		companyTokens.put("TRUST","TRUST");
		companyTokens.put("TRI-STATE","TRI-STATE");
		companyTokens.put("TRUSTEE","TRUSTEE");
		companyTokens.put("UNION","UNION");
		companyTokens.put("UNIVERSITY","UNIVERSITY");
		companyTokens.put("UP","UP");
		companyTokens.put("VENTURE","VENTURE");
		companyTokens.put("WAREHOUSE","WAREHOUSE");
		companyTokens.put("WHOLESALE","WHOLESALE");
		companyTokens.put("WHOLESALERS","WHOLESALERS");
		companyTokens.put("WIGS","WIGS");	
	}

	/**
	 * Normalize input string according to translation table.
	 */
	public static String normalizeString(String input, HashMap normTable) {
		for (Iterator iter = normTable.keySet().iterator(); iter.hasNext();) {
			String token = (String)iter.next();
			input = input.replaceAll("\\b" + token + "\\b",(String)normTable.get(token));			
		}
		return input;
	}

	/**
	 * Normalize Roman numbers.
	 */
	public static String normalizeRomanNumbers(String s) {
		String[] t = s.split("\\s+");
		for (int i = 0; i < t.length; i++) {
			if(RomanNumber.isATSRomanNumber(t[i])) {
				if((i < t.length - 1 && t[i+1].equals(SPOUSE_DELIM)) 
					|| (i == t.length - 1)) {
						t[i] = "" + RomanNumber.parse(t[i]);
				}
			}
		}
		return implodeString(t," ");
	}

	/**
	 * @return true if s represents a name prefix.
	 */
	public static boolean isNamePrefix(String s) {
		return namePrefixes.containsKey(s);		
	}
	
	/**
	 * @return true if s represent a name sufix.
	 */
	public static boolean isNameSuffix(String s) {
		return nameSuffixes.containsKey(s) || s.matches("^\\d+$");
	}

	/**
	 * @return true if s represents a name degree.s
	 */
	public static boolean isNameDegree(String s) {
		return nameDegrees.containsKey(s);
	}
	
	/**
	 * @return true if s represens a company suffix token. 
	 */
	public static boolean isCompanySuffix(String s) {
		return companyTokens.containsKey(s);
	}
	
	/**
	 * @return true if s represents a company name.
	 */
	public static boolean isCompanyName(String s) {
		for (Iterator iter = companyTokens.keySet().iterator(); iter.hasNext();) {
			String token = (String) iter.next();
			if(Pattern.compile("(?i)\\b" + token + "\\b").matcher(s).find())
				return true;
		}
		return false;			
	}
	
	/**
	 * Test for last name as follow, if name ends with "," then true. 
	 */
	public static boolean isLastName(String s) {
		return s.endsWith(",");
	}
		
	/**
	 * @return String from arr tokens separed by specified delimiter.
	 */
	public static String implodeString(String[] arr, String delim) {
		if(arr == null || delim == null) 
			throw new IllegalArgumentException("input string array or specified delimiter cannot be null");

		String s = "";			
		for(int i = 0; i < arr.length; i++) {
			s += (i == arr.length - 1 ? arr[i] : arr[i] + delim);
		}
		 
		return s;
	}
	
	/**
	 * Normalize input string s.
	 *
	 * @param s String to be prepared for parsing.
	 * @return
	 */
	public static String normalize(String s) {
		
		s=s.toUpperCase();							
		s=s.replaceAll("\\."," ");					// replace . by spaces
		s=s.replaceAll(" ,"," ");					// remove ' ,'  
		s=s.replaceAll("\\s+", " ");				// compact spaces
		s=s.replaceAll("\\(.*\\)","");				// remove (anything)
		s=s.replaceAll("\\(", "&");					// replace '(' with '&' (spouse delimiter)
		s=s.replaceAll("/","&");					// replace / with & 
		s=s.replaceAll("\\)", "");					// remove all ')'
		s=s.replaceAll(" ET ?UXX?\\b", " &");		// replace spouse delimiter tokens (ETUX , ETUXX) with &
		s=s.replaceAll(" ET ?AL?\\b", " &");		// replace spouse delimiter tokens (ETAL , ETA) with & 
		s=s.replaceAll(" ET ?VIR\\b", " &");		// replace spouse delimiter tokens (ETVIR) with & 
		s=s.replaceAll(" AND\\b", " &");			// replace spouse delimiter tokens (AND) with &	
		s=s.replaceAll("&( &)+", "&");				// delete multiple &
		s=s.replaceAll("& *$", "");					// trim &
		s=s.replaceAll("%", "&");					// replace % with &
		s=s.replaceAll("\\b(\\d+)(ST|RD|TH)\\b","$1");	// remove ST,RD and TH number suffixes	

		s = normalizeString(s,namePrefixes);		// normalize prefixes
		s = normalizeString(s,nameSuffixes);		// normalize suffixes
		s = normalizeString(s,nameDegrees);			// normalize degrees
		s = normalizeString(s,numbers);				// normalize numbers
		s = normalizeRomanNumbers(s);				// normalize roman numbers
		
		if(isCompanyName(s)) {						// normalize company tokens
			s = normalizeString(s,companyTokens); 
		}

		return s;
	}

	public static void testCase(String[] names) {
		for (int i = 0; i < names.length; i++) {
			logger.info(names[i]+" => "+ normalize(names[i]));
		}
	}

	public static void main(String[] args) {
		testCase(new String[] {
			"MISTER JOHN Smith III ETUX JOHN LIDIA 2ST",
			"JOHN SMITH JUNIOR",
			"THE HONORABLE JOHN SMITH J.R.",
			"REVEREND JOHN Smith"}
		);		
	}
	
}
