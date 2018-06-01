package ro.cst.tsearch.utils;

import java.io.BufferedReader;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Vector;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.lang.reflect.Method;
import org.apache.log4j.Category;

import ro.cst.tsearch.database.ConnectionPool;
import ro.cst.tsearch.database.DBConnection;
import ro.cst.tsearch.database.DBManager;
import ro.cst.tsearch.database.SingletonOaklandSubdivision;
import ro.cst.tsearch.exceptions.BaseException;
import ro.cst.tsearch.servers.types.MIMacombRO;
import ro.cst.tsearch.servlet.InitContext;
import ro.cst.tsearch.data.ILKaneSubdivisions;
import ro.cst.tsearch.data.OaklandSubdivisions;
import ro.cst.tsearch.data.Subdivisions;
public class SubdivisionMatcher {
	
	protected static final Category logger = Category.getInstance(SubdivisionMatcher.class.getName());

	public static final int  MO_JACKSON           	= 0;
	public static final int  KS_JOHNSON           	= 1;
	public static final int  TN_HAMILTON          	= 2;
	public static final int  MI_OAKLAND           	= 3;
	public static final int  MI_OAKLAND_SUBDIV	  	= 4;
	public static final int  MI_OAKLAND_CONDO     	= 5;
	public static final int  ATS_FILE_UPDATE      	= 6;
	public static final int  MI_MACOMB            	= 7;
	public static final int  MI_MACOMB_SUBDIV     	= 8;
	public static final int  MI_MACOMB_CONDO		= 9;
	public static final int  MI_WAYNE_SUBDIV		= 10;
	public static final int  MI_WAYNE_CONDO			= 11;	
	public static final int  IL_KANE	           	= 12;
	
	private static final int MATCHERS_NO			= 13; // !!!! increment this value every time a new matcher is added
	
	
	public static final Pattern oaklandLineDataPattern = Pattern.compile( "(?i)(\\d+)\\s+(\\d+)\\s+(.*)" );
	public static final Pattern macombLineDataPattern = Pattern.compile( "(?i)(\\d+)\\s+(\\d+)\\s+(.*)" );
	
	private static SubdivisionMatcher[] subdivisionMatchers = new SubdivisionMatcher[MATCHERS_NO];
	private long searchId=-1;
	private SubdivisionMatcher(long searchId) {
		this.searchId = searchId;
	}

	public static SubdivisionMatcher getInstance(int county,long searchId) {
		
		SubdivisionMatcher subdivisionMatcher = subdivisionMatchers[county];
			
		if (subdivisionMatcher == null) {
			subdivisionMatcher = new SubdivisionMatcher(searchId);
			subdivisionMatcher.initialize(county);
			subdivisionMatchers[county] = subdivisionMatcher;
		}
		
		return subdivisionMatcher;
	}
	
	private Hashtable subdivisions = new Hashtable();
	
	public String[] match(String candidat) {
	
		long time = System.currentTimeMillis();
		
		if (candidat == null)
			return new String[0];
		
		String key;
		
		if (candidat.length() >= 3) {
			key = candidat.substring(0, 3);
		} else 
			return new String[0];
		
		Vector closestMatches = (Vector) subdivisions.get(key);
		
		if (closestMatches == null)
			return new String[0];
		
		double score = 1;
		Vector bestMatch = new Vector();
		
		Enumeration subdivisions = closestMatches.elements();
		while (subdivisions.hasMoreElements()) {
			
			String subdivision = (String) subdivisions.nextElement();	
			double s = Math.abs(1 - BasicFilter.score(MatchEquivalents.getInstance(searchId).getEquivalent( candidat, false ), MatchEquivalents.getInstance(searchId).getEquivalent( subdivision, false ))); 
			
			if (s < 0.3) {
			
				if (s < score) {
				
					score = s;
					bestMatch = new Vector();
					
					bestMatch.add(subdivision);
				
				} else if (s == score) {
					
					bestMatch.add(subdivision);
				}
			}
			
//			System.err.println("Trying - [" + subdivision + "], score = " + (1 - s));
		}
		
		System.err.println("Match done, [" + candidat + "] -> " + bestMatch + " in " 
				+ (System.currentTimeMillis() - time) + " ms with score=" + (1 - score));
		
		return ((String[]) bestMatch.toArray(new String[bestMatch.size()]));
	}
	
	private void initialize(int county) {
		
		System.err.println("Start initialize....");
		
		String[] allSubdivisions = null;
		
		if( county != MI_OAKLAND && county != MI_OAKLAND_CONDO && county != MI_OAKLAND_SUBDIV && county != MI_MACOMB_CONDO && county != MI_MACOMB_SUBDIV && county != IL_KANE ){
			allSubdivisions = DBManager.getSubdivisions(county);
		}
		else if( county == MI_OAKLAND_CONDO || county == MI_OAKLAND_SUBDIV || county == MI_MACOMB_CONDO || county == MI_MACOMB_SUBDIV || county == IL_KANE ){
			//different case for oakland
			
			//get alll Oakland subdiv
			OaklandSubdivisions[] allOaklandSubdiv = null;
			Subdivisions[] allMacombSubdiv = null;
			ILKaneSubdivisions[] allILKaneSubdivisions = null;
			
			if( county == MI_OAKLAND_SUBDIV ){
				//allOaklandSubdiv = DBManager.getOaklandSubdivisions("", OaklandSubdivisions.DB_OAKLAND_SUBDIVISION );
				allOaklandSubdiv = SingletonOaklandSubdivision.getInstance().getSubdivision("",OaklandSubdivisions.DB_OAKLAND_SUBDIVISION);
			}
			else if ( county == MI_OAKLAND_CONDO ){
				//allOaklandSubdiv = DBManager.getOaklandSubdivisions("", OaklandSubdivisions.DB_OAKLAND_CONDOMINIUM );
				allOaklandSubdiv = SingletonOaklandSubdivision.getInstance().getSubdivision("",OaklandSubdivisions.DB_OAKLAND_CONDOMINIUM);
			}
			else if( county == MI_MACOMB_CONDO ){
				//allOaklandSubdiv = DBManager.getOaklandSubdivisions("", OaklandSubdivisions.DB_OAKLAND_CONDOMINIUM );
				allMacombSubdiv = SingletonOaklandSubdivision.getInstance().getMacombSubdivision("",Subdivisions.DB_MACOMB_CONDOMINIUM);
			}
			else if( county == MI_MACOMB_SUBDIV ){
				//allOaklandSubdiv = DBManager.getOaklandSubdivisions("", OaklandSubdivisions.DB_OAKLAND_CONDOMINIUM );
				allMacombSubdiv = SingletonOaklandSubdivision.getInstance().getMacombSubdivision("",Subdivisions.DB_MACOMB_SUBDIVISION );				
			}
			else if (county == IL_KANE){
				allILKaneSubdivisions = SingletonOaklandSubdivision.getInstance().getKaneSubdivision("", ILKaneSubdivisions.DB_IL_KANE_SUBDIVISION);
			}
		
			
			
			int length = 0;
			
			//build subdivision name array
			if( allOaklandSubdiv != null ){
				length = allOaklandSubdiv.length;
			}
			else if( allMacombSubdiv != null ){
				length = allMacombSubdiv.length;
			} else {
				length = allILKaneSubdivisions.length;
			}

			allSubdivisions = new String[ length ];
			
			//populate the array
			for( int j = 0 ; j < length ; j ++ ){
				if( allOaklandSubdiv != null ){
					allSubdivisions[j] = allOaklandSubdiv[j].getName();
				}
				else if (allMacombSubdiv != null){
					allSubdivisions[j] = allMacombSubdiv[j].getName();
				} 
				else if (allILKaneSubdivisions != null){
					allSubdivisions[j] = allILKaneSubdivisions[j].getCode() + "###" + allILKaneSubdivisions[j].getName();
				}
			}
		} 
		
		String key;
		if (subdivisions != null) {
			
			for (int i = 0; i < allSubdivisions.length; i++) {
				
				if (allSubdivisions[i] == null || allSubdivisions[i].length() < 3)
					continue;
				
				if (county == IL_KANE) {
					String[] subs = allSubdivisions[i].split("###");
					key = subs[0];
					
					Vector group = (Vector) subdivisions.get(key);
					
					if (group == null) {
						group = new Vector();
						subdivisions.put(key, group);
					}
					if (subs.length == 1){
						group.add(key);//when we have only the code, without the name
					} else{
						group.add(subs[1]);
					}
					
				} else {
					key = allSubdivisions[i].substring(0, 3);
				
					Vector group = (Vector) subdivisions.get(key);
					
					if (group == null) {
						group = new Vector();
						subdivisions.put(key, group);
					}
					group.add(allSubdivisions[i]);
				}
			}
		}
		
		System.err.println("Done!");
	}
	
	public String getSubdivisionName(String subdivCode) {
		
		long time = System.currentTimeMillis();
		
		if (subdivCode == null)
			return "";
		
		String key;
		
		if (subdivCode.length() >= 3) {
			key = subdivCode;
		} else 
			return "";
		
		System.err.println("Trying to get subdivision name for the subdivision code - [" + subdivCode + "]");
		if (subdivisions.get(key) == null)
			return "";
		
		String subdivName = (String) ((Vector) subdivisions.get(key)).get(0);

		if (subdivName == null)
			return "";
		
		if (subdivName.equals(subdivCode)){
			System.err.println("There is no subdivision name for this code!");
		}
		
		
		System.err.println("Match done, [" + subdivCode + "] -> " + subdivName + " in " 
				+ (System.currentTimeMillis() - time) + " ms");
		
		return subdivName;
	}
	
	public static void importJackson( String inputFilePath ) {
		BufferedReader br = null;
		try {
			
            DBManager.deleteSubdivisions( MO_JACKSON );
            
			//BufferedReader br = new BufferedReader(new FileReader("C:\\jackson_subdiv.txt"));
			 br = new BufferedReader(new FileReader( inputFilePath ));
			
			String subdivision, origSubdivision;
			
			StringBuffer sb = new StringBuffer();
			sb.append("<html><head>" +
						"<style>" +
					"td" +
					"{" +
					"	font-family: Verdana;" +
					"	font-size: 13px;" +
					"	border-bottom: 1px dotted;" +
					"	color: black;" +
					"	padding-left:10px;" +
					"}" +
					"</style></head><body>" +
					"<table width='100%' cellpadding='0' cellspacing='0' border='0'>");
			
//			String numbers = "\\b(\\d+\\s*ST|\\bIST\\b|\\d+ND|\\d+RD|\\d+TH|\\d+|FIRST|SECOND|THIRD|FOURTH|FIFTH|SIXTH|SEVENTH|\\WEIGHTH?|NINTH|TENTH|TWELFTH|TWENTIETH|EIGHTY-FIFTH|FIFTY-SEVENTH|FIFTY-EIGHTH|FIFTY\\s*-\\s*NINTH)";			
			while ( (subdivision = br.readLine()) != null) {
				
				origSubdivision = subdivision;
				
                String[] subdivisions = origSubdivision.split( " AKA " );
                
                for( int i = 0 ; i < subdivisions.length ; i ++ )
                {
                    subdivision = subdivisions[i];
                    
                    subdivision = cleanJackson( subdivision );
                    
//    				subdivision = subdivision.replaceAll("PLATLOTS", "PLAT");
//    				subdivision = subdivision.replaceAll("PLATLOT", "PLAT");
//    				subdivision = subdivision.replaceAll("(.*?\\bPLA?T?)(,|\\d+.*)", "$1");
//    				subdivision = subdivision.replaceAll("ACRESLOTS", "ACRES");
//    				subdivision = subdivision.replaceAll("ACRESLOT", "ACRES");
//    				subdivision = subdivision.replaceAll("OUTLOTS", "LOTS");
//    				subdivision = subdivision.replaceAll("OUTLOT", "LOTS");
//    				subdivision = subdivision.replaceAll("\\&40", "& 40");
//    				subdivision = subdivision.replaceAll("@", "AT");
//    				subdivision = subdivision.replaceAll("[\\[\\]]", "");
//    				subdivision = subdivision.replaceAll("\\bI\\s+(\\d+/\\d+)", "I$1"); //COLBORN & CYCLONE ROAD ESTATES 2ND PLAT I 33/4 UN
//    								
//    				subdivision = subdivision.replaceAll("BRIDLESPUR 2ND PLAT 65-109 205", "BRIDLESPUR 2ND PLAT");
//    				subdivision = subdivision.replaceAll("COUNTRY MEADOWS NORTH P.U.D. 25,26&29", "COUNTRY MEADOWS NORTH P.U.D.");
//    				subdivision = subdivision.replaceAll("MEI TRACK I&II RP HIDDEN CREEK OFFICE PK 4TH PK", "MEI");
//    				subdivision = subdivision.replaceAll("WESTMINSTER SUB OF NO 1/2 09-1238", "WESTMINSTER");
//    				subdivision = subdivision.replaceAll("UNION HILL EIGHT PLAT RS1-7 TRA TRB OF 2P PT3P TRJ OF 6P", "UNION HILL EIGHT PLAT");
//    				subdivision = subdivision.replaceAll("-RESURVEY", " RESURVEY");
//    						
//    				subdivision = subdivision.replaceAll("(.*?)\\WOF\\sLT(($)|(\\s.*))", "$1");
//    				subdivision = subdivision.replaceAll("(.*?)\\WOF\\sLTS(($)|(\\s.*))", "$1");
//    				subdivision = subdivision.replaceAll("(.*?)\\WOF\\sLOT(($)|(\\s.*))", "$1");
//    				subdivision = subdivision.replaceAll("(.*?)\\WOF\\sLOTS(($)|(\\s.*))", "$1");
//    				subdivision = subdivision.replaceAll("(.*?)\\WOF\\sBLK(($)|(\\s.*))", "$1");
//    				subdivision = subdivision.replaceAll("(.*?)\\WOF\\sBLOCK(($)|(\\s.*))", "$1");
//    				subdivision = subdivision.replaceAll("(.*?)\\WOF\\sPT(($)|(\\s.*))", "$1");
//    				subdivision = subdivision.replaceAll("(.*?)\\WOF\\sPTS(($)|(\\s.*))", "$1");
//    				subdivision = subdivision.replaceAll("(.*?)\\WOF\\sPRT(($)|(\\s.*))", "$1");
//    				subdivision = subdivision.replaceAll("(.*?)\\WOF\\sSOUTHERN PT(($)|(\\s.*))", "$1");
//    				subdivision = subdivision.replaceAll("(.*?)\\WOF\\sSOTHERN PT(($)|(\\s.*))", "$1");
//    				subdivision = subdivision.replaceAll("(.*?)\\WOF\\sN(($)|(\\s.*))", "$1");
//    				subdivision = subdivision.replaceAll("(.*?)\\WOF\\sS(($)|(\\s.*))", "$1");
//    				subdivision = subdivision.replaceAll("(.*?)\\WOF\\sE(($)|(\\s.*))", "$1");
//    				subdivision = subdivision.replaceAll("(.*?)\\WOF\\sV(($)|(\\s.*))", "$1");
//    				subdivision = subdivision.replaceAll("(.*?)\\WOF\\sTRACT(($)|(\\s.*))", "$1");
//    				subdivision = subdivision.replaceAll("(.*?)\\WOF\\sTR(($)|(\\s.*))", "$1");
//    				subdivision = subdivision.replaceAll("(.*?)\\WOF\\sTRACTS(($)|(\\s.*))", "$1");
//    				subdivision = subdivision.replaceAll("(.*?)\\WOF\\sOUTLOT(($)|(\\s.*))", "$1");
//    				subdivision = subdivision.replaceAll("(.*?)\\WOF\\s\\(UNITS(($)|(\\s.*))", "$1");
//    				subdivision = subdivision.replaceAll("(.*?)\\WOF\\s\\(LOTS(($)|(\\s.*))", "$1");
//    				subdivision = subdivision.replaceAll("(.*?)\\WOF\\s\\(LTS(($)|(\\s.*))", "$1");
//    				subdivision = subdivision.replaceAll("(.*?)\\WOF\\sREPLAT(($)|(\\s.*))", "$1");
//    				subdivision = subdivision.replaceAll("(.*?)\\WOF\\sSO(($)|(\\s.*))", "$1");
//    				subdivision = subdivision.replaceAll("(.*?)\\WOF\\sRS(($)|(\\s.*))", "$1");
//    				
//    				subdivision = subdivision.replaceAll(
//    						"(.*?)"+numbers+"((\\s*(\\bAND\\b|\\bTO\\b|-|,|&)\\s*)"+numbers+")*\\s+P(AR)?T\\b", "$1");				
//    				subdivision = subdivision.replaceAll("(.*?)\\WPART OF(($)|(\\s.*))", "$1");
//    				subdivision = subdivision.replaceAll("(.*?)\\WPT(($)|(\\s.*))", "$1");
//    				
//    				subdivision = subdivision.replaceAll("(.*?)\\WLT(($)|(\\s.*))", "$1");
//    				subdivision = subdivision.replaceAll("(.*?)\\WLOT(($)|(\\s.*))", "$1");
//    				subdivision = subdivision.replaceAll("(.*?)\\WLOTS(($)|(\\s.*))", "$1");
//    				subdivision = subdivision.replaceAll("(.*?)\\WL0TS(($)|(\\s.*))", "$1");
//    				subdivision = subdivision.replaceAll("(.*?)\\WLTS(($)|(\\s.*))", "$1");
//    				subdivision = subdivision.replaceAll("(.*?)\\WLT\\d+.*", "$1");
//    				subdivision = subdivision.replaceAll("(.*?)\\WLOT\\d+.*", "$1");
//    				subdivision = subdivision.replaceAll("(.*?)\\WLOTS\\d+.*", "$1");
//    				subdivision = subdivision.replaceAll("(.*?)\\WLOS\\s*\\d+.*", "$1");
//    				subdivision = subdivision.replaceAll("(.*?)\\WLTS\\d+.*", "$1");
//    				subdivision = subdivision.replaceAll("(.*?)\\W[L|B|I|RS]\\d+.*", "$1");
//    				
//    				subdivision = subdivision.replaceAll("(.*?)\\WADD\\(.*", "$1");
//    				
//    				subdivision = subdivision.replaceAll("(.*?)\\WBL(($)|(\\s.*))", "$1");
//    				subdivision = subdivision.replaceAll("(.*?)\\WBK(($)|(\\s.*))", "$1");
//    				subdivision = subdivision.replaceAll("(.*?)\\WBLK(($)|(\\s.*))", "$1");
//    				subdivision = subdivision.replaceAll("(.*?)\\WBLKS(($)|(\\s.*))", "$1");
//    				subdivision = subdivision.replaceAll("(.*?)\\WBLOCK(($)|(\\s.*))", "$1");
//    				subdivision = subdivision.replaceAll("(.*?)\\WBLOCKS(($)|(\\s.*))", "$1");
//    				subdivision = subdivision.replaceAll("(.*?)\\WBL\\d+.*", "$1");
//    				subdivision = subdivision.replaceAll("(.*?)(\\W|\\s\\d+|\\s\\d+\\-\\d+)BLK\\d+.*", "$1");
//    				subdivision = subdivision.replaceAll("(.*?)\\WBLKS\\d+.*", "$1");
//    				subdivision = subdivision.replaceAll("(.*?)\\WBLOCK\\d+.*", "$1");
//    				
//    				subdivision = subdivision.replaceAll("(.*?)\\WTR(($)|(\\s.*))", "$1");
//    				subdivision = subdivision.replaceAll("(.*?)\\WTRS(($)|(\\s.*)|(\\d+.*))", "$1");
//    				subdivision = subdivision.replaceAll("(.*?)\\WTRACT(($)|(\\s.*))", "$1");
//    				subdivision = subdivision.replaceAll("(.*?)\\WTRACTS(($)|(\\s.*))", "$1");
//    				
//    				subdivision = subdivision.replaceAll("(.*?)\\WSEC(($)|(\\b\\s*.*))", "$1");
//    				subdivision = subdivision.replaceAll("(.*?)\\s[A-Za-z]\\d+.*", "$1");
//    				
//    				subdivision = subdivision.replaceAll("(.*?)\\sUN(($)|(\\s.*))", "$1");
//    				if (subdivision.toUpperCase().indexOf("UNIT DEVELOPMENT") == -1)
//    					subdivision = subdivision.replaceAll("(.*?)\\sUNIT(($)|(\\s.*))", "$1");
//    				subdivision = subdivision.replaceAll("(.*?)\\WUNIT[\\s\\d+].*", "$1");
//    				
//    				subdivision = subdivision.replaceAll("(.*?)\\WUNITS(($)|(\\s.*))", "$1");
//    				subdivision = subdivision.replaceAll("(.*?)\\WIUNIT(($)|(\\s.*))", "$1");
//    				subdivision = subdivision.replaceAll("(.*?)\\WIUNITS(($)|(\\s.*))", "$1");
//    				
//    				subdivision = subdivision.replaceAll("(.*?)\\WAR\\s.*", "$1");
//    				
//    				subdivision = subdivision.replaceAll("(.*?)\\sNW\\d+(($)|(?!(RD|ST|TH|ND)).*$)", "$1");
//    				subdivision = subdivision.replaceAll("(.*?)\\sNE\\d+(($)|(?!(RD|ST|TH|ND)).*$)", "$1");
//    				subdivision = subdivision.replaceAll("(.*?)\\sSW\\d+(($)|(?!(RD|ST|TH|ND)).*$)", "$1");
//    				subdivision = subdivision.replaceAll("(.*?)\\sSE\\d+(($)|(?!(RD|ST|TH|ND)).*$)", "$1");
//    				
//    				subdivision = subdivision.replaceAll("(.*?)\\sN\\s\\d+(($)|(?!(RD|ST|TH|ND)).*$)", "$1");
//    				subdivision = subdivision.replaceAll("(.*?)\\sE\\s\\d+(($)|(?!(RD|ST|TH|ND)).*$)", "$1");
//    				subdivision = subdivision.replaceAll("(.*?)\\sS\\s\\d+(($)|(?!(RD|ST|TH|ND)).*$)", "$1");
//    				subdivision = subdivision.replaceAll("(.*?)\\sW\\s\\d+(($)|(?!(RD|ST|TH|ND)).*$)", "$1");
//    				
//    				subdivision = subdivision.replaceAll("(.*?)\\sN\\s\\d/.*", "$1");
//    				subdivision = subdivision.replaceAll("(.*?)\\sE\\s\\d/.*", "$1");
//    				subdivision = subdivision.replaceAll("(.*?)\\sS\\s\\d/.*", "$1");
//    				subdivision = subdivision.replaceAll("(.*?)\\sW\\s\\d/.*", "$1");
//    				
//    				subdivision = subdivision.replaceAll("(.*?)\\s[NSWEI]$", "$1");
//    				
//    				subdivision = subdivision.replaceAll("(.*?)\\sOR\\s.*", "$1");
//    
//    				subdivision = subdivision.replaceAll("(.*)( \\d+[- ]\\d+)(($)|(?!(RD|ST|TH|ND)).*$)", "$1");
//    				
//    				subdivision = subdivision.replaceAll("(.*) (.*/\\d+).*", "$1");
//    				subdivision = subdivision.replaceAll("(.*?)((\\\\\\&)|(/)|(\\\\))(.*)", "$1");
//    				
//    				subdivision = subdivision.replaceAll("(.*)[\\&,-]\\s*$", "$1");
//    				subdivision = subdivision.replaceAll("\"", "");
//    				
//    				subdivision = subdivision.replaceAll("(.*)( \\().*", "$1");
//    				subdivision = subdivision.replaceAll("(.*)(\\().*", "$1");
//    				subdivision = subdivision.replaceAll("(.*?)\\sOF(($)|(\\s+)$)", "$1");
//    				
//    				subdivision = subdivision.replaceAll(
//    						"(.*?)\\s(NO\\s\\d+\\-\\d+|\\&|\\s*\\d+[\\&\\.]\\d+(?!\\s+MILLION).*|\\w\\d+\\-\\d+)$", "$1"); //NORTH 2.5 MILLION GALLON TANK LT 1
//    				
//    				subdivision = subdivision.replaceAll("(.*?)\\s(MO)$", "$1");
//    				
//    				subdivision = subdivision.replaceAll(
//    						"(.*?)\\s(K C|KC|LS|LE|BS|BL|SU|RA|GV|IN|UN|GR|BU|VI|B|MO|VAC|NW|THE)$", "$1");
//    				
//    				subdivision = subdivision.replaceAll("(.*?)\\s*$", "$1");
//    				
//    				subdivision = subdivision.replaceAll("(\\b(RE)?PL(AT|T)?S?) P\\.?U\\.?D\\.?", "$1"); 
//    				
//    				subdivision = subdivision.replaceAll("(.*?)(-?\\s*REPLAT.*?$)", "$1");
//    					
//    				subdivision = subdivision.replaceAll(
//    						"(.*?)(?!FIRST|SECOND)&?\\s*"+numbers+"((\\s*(\\bAND\\b|\\bTO\\b|-|,|&)\\s*)"+numbers+")*(.*?REPLATS| REPLAT| REPL| REP|.*?PLATS|.*?PLAT|.*?PLT|.*?PL|.*P| ADDITION| ADD| PHASE| PH| RESURVEY| RES| AM)\\b", "$1"); 
//    				//FIRST BIBLE BAPTIST 1ST PLAT LOT1 I69/87 BS; GREGORY ESTATES & 1ST-5TH PLAT (LTS 1-11) LE; HEARNE'S 2ND AND 3RD ADDITION LOTS 16A & 16B I71/39 LE; HARTMAN HERITAGE CENTER 1,2,4 & 5TH PL (LTS 1-4A & 5-6) IN; HOCKER HIGHLAND 1ST AM LOT 1-4 I28/79 IN
//    								
//    				subdivision = subdivision.replaceAll("(?i)(.*?)(supplement)?(\\s*?plat\\s*?\\d+)$", "$1");
//    				
//    				subdivision = subdivision.replaceAll("(.*?)\\s(TO|\\d+|ADD(ITION)? (#|NO\\.?\\s)\\d*|\\d+\\s*PLA?T?|\\d+P|PHASE.*?|NO\\s\\d+|NO|CONTINUATION)$", "$1");
//    				subdivision = subdivision.replaceAll("(.*?)\\s(ADDITION)\\s*(CONTINUATION|NO\\d*)$", "$1");
//    				
//    				subdivision = subdivision.replaceAll("(.*?)\\s\\&?\\s*(A\\s?)(REPLATS?|REPL|REP|RP|RESURVEYS?|RESURBEYS?|RES-REV|RES|RS)\\s*(OF)?\\s*(PART)?$", "$1"); //EXECUTIVE PARK 25TH PLAT, A REP LOT A 03-0536 KC 
//    				
//    				subdivision = subdivision.replaceAll("(.*?)\\s(CORRECTED|CORREC|CORR|COR|FINAL|AMENDED|AMEND|AMEN|REVISED|SUPPLEMENT)\\s*(PLATS?|PT|PL)?$", "$1");
//    				subdivision = subdivision.replaceAll("(.*?)\\s(CORRECTED|CORREC|CORR|COR|FINAL|AMENDED|AMEND|AMEN|REVISED|SUPPLEMENT)\\b\\s*(PLAT?|PT|PL)?.*?$", "$1");
//    				
//    				subdivision = subdivision.replaceAll("(.*?)\\&?\\s(-?RESURVEYS?|RE-SURVEY|RES|RS|REPLATS?|REPT|REP|RP)\\b\\s*\\&?.*?$", "$1");
//    				subdivision = subdivision.replaceAll ("(.*?)\\s(PLA?T?)\\s\\d*\\w?,?$", "$1");  /*LAKEWOOD BUS CTR I-470 PLAT A,*/
//    				
//    				subdivision = subdivision.replaceAll("(.*?),?\\s(RP|REP|THE RES)$", "$1");
//    				subdivision = subdivision.replaceAll("(.*?)-\\s*(REP)$", "$1");
//    				subdivision = subdivision.replaceAll("(.*?)-?\\s*(\\&|\\d+\\s*ST|\\d+ND|\\d+RD|\\d+TH|ADDITION|ADD|THE|TO|\\d+P|SURV)$", "$1"); 
//    				
//    				subdivision = subdivision.replaceAll("\\d+[A-Z](-\\d+[A-Z])?\\s*$", ""); //EAGLE CREEK TOWNHOMES 2ND PL 40A-40D TR E1 E2 LT E I93/10 LE  
//    				
//    				subdivision = subdivision.replaceAll("\\bAREA ([IVXC]+|\\d+)\\s*$", ""); //EASTLAND CENTER 2ND PLAT AREA IV (LTS 4-9 &TR A) I68/67 IN
//    				
//    				subdivision = subdivision.replaceAll("\\bBS\\s*$", ""); //COMPLETE AUTO REPAIR SERVICES CENTER, A COMMERCIAL PUD BS
//    				
//    				subdivision = subdivision.replaceAll("\\s+-[A-Z]\\s*$", ""); //HANCOCK PLACE -A- 04-2703 KC  
//    				
//    				subdivision = subdivision.replaceAll("[\\&,-]\\s*$", ""); //HARDESTY HOME, THE RES OF LOT 7 04-2833 KC  
//    
//    				subdivision = subdivision.replaceAll("\\bNO\\.?\\s*$", ""); //HOLT NO. 2 04-5276 LE		
//    				
//    				subdivision = subdivision.replaceAll("\\bTO KANSAS CITY MISSOURI\\b", ""); //GATES THIRD ADDITION TO KANSAS CITY MISSOURI LTS 1-38 K14/60				
//    				subdivision = subdivision.replaceAll("\\bCITY OF .+", ""); //HOWARD'S W B 3RD ADD CITY OF LEES SUMMIT 04-5633 LE
//    								
//    				subdivision = subdivision.replaceAll(numbers+"\\s+PART\\s*$", ""); //JOHNSONS 1ST ADD 2ND PART LOTS 13, 32 & 33 IN
//    				subdivision = subdivision.replaceAll("\\bPL(A?T)?S?\\s*$", ""); //LEMONE SMITH BUSN & RAIL CENTER PL7 LOTS 9A & 9G I65/29 LS  
//    				
//    				subdivision = subdivision.replaceAll("\\bKC\\s*$", ""); //MEADOWVIEW [KC] (LTS 1-15) B24/67 KC
//    				
//    				subdivision = subdivision.replaceAll("\\bMINOR SUB\\s*$", ""); //RIVER OAKS 4TH PLAT MINOR SUB PT LOT 19-22 BLK 15 K40/11 GR 
//    				
//    				subdivision = subdivision.replaceAll("\\bINCL\\s*$", ""); //VILLAS AT MEADOWS AT SUMMIT RIDGE 6TH PL INCL LT 16-17  
//    				
//    				subdivision = subdivision.replaceAll("\\bOF PORTION\\b", ""); //VILLAS AT SUMMIT RIDGE 2ND REPL OF PORTION TRACT D & BLK 1
//    				
//    				subdivision = subdivision.replaceFirst("(?i)\\b(P\\.U\\.D\\.|PRO)\\s*$", ""); //Summerfield East Pro bug #576
//                    subdivision = subdivision.replaceFirst("(?i)\\b(PUD|PR\\-O|PRO\\-O)\\s*$", ""); //Summerfield East Pro bug #576 - 2
//    				
//    				subdivision = subdivision.trim();
    										
    				sb.append("<tr>");
    				sb.append("<td width='50%'>" + origSubdivision + "</td>"); 
    				sb.append("<td width='50%'>" + subdivision + "</td>");
    				sb.append("</tr>");	
    
    				if (subdivision.length() > 0 && DBManager.addSubdivision(MO_JACKSON, subdivision.trim()) == 0) {
    					logger.debug(subdivision);
    				} else {
    					logger.debug("					" + subdivision);
    				}
                }
			}
			
			sb.append("</table></body></html>");
			
            /*
			FileOutputStream fos = new FileOutputStream("C:\\result.html");
			fos.write(sb.toString().getBytes());
			fos.flush();
			fos.close();
			*/
			if(br!=null){
				br.close();
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		finally{
			if(br!=null){
				try{
					br.close();
				}
				catch(IOException ex){
					//nothing to do
				}
			}
		}
	}
	
	public static void importOakland( String inputFilePath ) {
		BufferedReader br=null;
		try {
            
			//deletes existing subdivisions
            DBManager.deleteOaklandSubdivisions();
			
			br = new BufferedReader(new FileReader( inputFilePath ));
						
			StringBuffer sb = new StringBuffer();
			sb.append("<html><head>" +
						"<style>" +
					"td" +
					"{" +
					"	font-family: Verdana;" +
					"	font-size: 13px;" +
					"	border-bottom: 1px dotted;" +
					"	color: black;" +
					"	padding-left:10px;" +
					"}" +
					"</style></head><body>" +
					"<table width='100%' cellpadding='0' cellspacing='0' border='0'>");		
			
			//this shoudl be change to csv export separator
			String splitChar     = ";";
			
			String lineData;
			String[] oaklandData; 
			String[] oaklandNames;
			OaklandSubdivisions oak = new OaklandSubdivisions() ;			
			Method[] methodLists    = oak.getClass().getDeclaredMethods();
			
			//take the column names
			oaklandNames = br.readLine().split(splitChar);
			
			while ( (lineData = br.readLine()) != null ) {
				oaklandData  =  lineData.split(splitChar);
				for (int i=0;i<oaklandData.length;i++)
				{
					for (int j=0;j<methodLists.length;j++)
						if (methodLists[j].getName().compareToIgnoreCase("SET"+oaklandNames[i].trim())==0)
						{							
							  if (methodLists[j].getParameterTypes()[0] == short.class)
								  methodLists[j].invoke(oak, Short.parseShort(oaklandData[i].trim()));
							  else if (methodLists[j].getParameterTypes()[0] == long.class)
								  methodLists[j].invoke(oak, Long.parseLong(oaklandData[i].trim()));
							  else if (methodLists[j].getParameterTypes()[0] == String.class)
								  methodLists[j].invoke(oak, oaklandData[i].trim());
						}
														  
				}
				//add current info into database
				DBManager.addOaklandSubdivision(oak);
				
			}
			
			if(br!=null){
				br.close();
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		finally{
			if(br!=null){
				try{
					br.close();
				}
				catch(IOException ex){
					//nothing to do
				}
			}
		}
		
	}
	
/*				case SubdivisionMatcher.MI_MACOMB_CONDO:
					 SubdivisionMatcher.importMacomb(uploadedFileAbsolutPath, SubdivisionMatcher.MI_MACOMB_CONDO);
                break;
				case SubdivisionMatcher.MI_MACOMB_SUBDIV:
					 SubdivisionMatcher.importMacomb(uploadedFileAbsolutPath, SubdivisionMatcher.MI_MACOMB_SUBDIV);
               break;
 * */	
	public static void importMacomb( String inputFilePath, int type) {
		BufferedReader br=null;
		try {
            
			//deletes existing subdivisions
            DBManager.deleteMacombSubdivisions();
			
			br = new BufferedReader(new FileReader( inputFilePath ));
						
			StringBuffer sb = new StringBuffer();
			sb.append("<html><head>" +
						"<style>" +
					"td" +
					"{" +
					"	font-family: Verdana;" +
					"	font-size: 13px;" +
					"	border-bottom: 1px dotted;" +
					"	color: black;" +
					"	padding-left:10px;" +
					"}" +
					"</style></head><body>" +
					"<table width='100%' cellpadding='0' cellspacing='0' border='0'>");		
			
			
			String lineData;

			while ( (lineData = br.readLine()) != null ) {
				String[] macombData  =  lineData.split("\t");
				for (int i=0;i<macombData.length;i++)
				{
					Subdivisions macombSubdiv = new Subdivisions();
					
					if( macombData.length < 4 ){
						continue;
					}
					
					String code = macombData[0];
					String phase = macombData[1];
					String area = macombData[2];
					String name = macombData[3];

					macombSubdiv.setArea( area );
					macombSubdiv.setPhase( phase );
					macombSubdiv.setName( name );
					macombSubdiv.setCode( code );
					macombSubdiv.setTypeId( type );

					//add current info into database
					DBManager.addMacombSubdivision( macombSubdiv );
					
				}

				
			}
			
			if(br!=null){
				br.close();
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		finally{
			if(br!=null){
				try{
					br.close();
				}
				catch(IOException ex){
					//nothing to do
				}
			}
		}
	}
	
	public static void importKane( String inputFilePath) {
		BufferedReader br = null;
		DBConnection conn = null;
		//deletes existing subdivisions
        //DBManager.deleteKaneSubdivisions();
		
		try {
            
			conn = ConnectionPool.getInstance().requestConnection();
			String line;
			
			br = new BufferedReader(new FileReader( inputFilePath ));
			while ( (line = br.readLine()) != null ) {
				String[] items = line.split("#####");
				if (items.length == 1){
					DBManager.getSimpleTemplate().update( "insert into " + DBConstants.TABLE_SUBDIVISIONS_IL_KANE + "(`code`) values ( ?)",items[0] );
				}else if (items.length == 2){
					DBManager.getSimpleTemplate().update( "insert into " + DBConstants.TABLE_SUBDIVISIONS_IL_KANE + "(`code`, `name`) values (?, ?)", items[0], items[1] );
				} else if (items.length == 3){
					DBManager.getSimpleTemplate().update( "insert into " + DBConstants.TABLE_SUBDIVISIONS_IL_KANE + "(`code`, `name`, `plat_doc` ) values ( ?, ?, ? )", items[0], items[1], items[2] );
				}
			}
			
			
		} catch (Exception e) {
			e.printStackTrace();
		} finally{
			try{
				if(br!=null){
					try{
					br.close();
					}
					catch(IOException ex){
						//nothing to do 
					}
				}
				if (conn != null){
					ConnectionPool.getInstance().releaseConnection(conn);
				}
			}catch(BaseException e){}
		}
	}
	
	public static void importJohnson( String inputFilePath ) {
		BufferedReader br=null;
		try {
            
            DBManager.deleteSubdivisions( KS_JOHNSON );
			
			//BufferedReader br = new BufferedReader(new FileReader("C:\\Documents and Settings\\Catalin\\Desktop\\subdiv_johnson\\subdivsJohnson.txt"));
			br = new BufferedReader(new FileReader( inputFilePath ));
			
			//BufferedReader br = new BufferedReader(new FileReader("C:\\johnson_subdiv.txt"));
			
			StringBuffer sb = new StringBuffer();
			sb.append("<html><head>" +
						"<style>" +
					"td" +
					"{" +
					"	font-family: Verdana;" +
					"	font-size: 13px;" +
					"	border-bottom: 1px dotted;" +
					"	color: black;" +
					"	padding-left:10px;" +
					"}" +
					"</style></head><body>" +
					"<table width='100%' cellpadding='0' cellspacing='0' border='0'>");		
			
			String subdivision, origSubdivision;
			while ( (subdivision = br.readLine()) != null ) {
				
				subdivision = subdivision.trim();
				origSubdivision = subdivision;
				
				subdivision = subdivision.replaceAll("FIRST AND 2ND PLAT", "1ST AND 2ND PLAT");
				subdivision = subdivision.replaceAll("(.*?)\\WLOT.*", "$1");
				subdivision = subdivision.replaceAll("(.*?)\\WLOTS.*", "$1");				
				subdivision = subdivision.replaceAll("(.*?)\\WLT.*", "$1");
				subdivision = subdivision.replaceAll("(.*?)\\WLTS.*", "$1");
				//Johnson
				subdivision = subdivision.replaceAll("(.*?)\\WL\\d*[ /-].*", "$1");
				subdivision = subdivision.replaceAll("(.*?)\\WAR(($)|( .*))", "$1");
				subdivision = subdivision.replaceAll("(.*?)\\WTR(($)|( .*))", "$1");
				subdivision = subdivision.replaceAll("(.*?)\\WPH(($)|( .*))", "$1");
				subdivision = subdivision.replaceAll("(.*?)\\WPT(($)|( .*))", "$1");				
				subdivision = subdivision.replaceAll("(.*?)\\WBLD(($)|( .*))", "$1");
				
				subdivision = subdivision.replaceAll("(.*?)\\WA\\d*(($)|( .*))", "$1");
				subdivision = subdivision.replaceAll("(.*?)\\WB\\d*(($)|( .*))", "$1");
				subdivision = subdivision.replaceAll("(.*?)\\WB\\d*(($)|( .*))", "$1");
				subdivision = subdivision.replaceAll("(.*?)\\WP\\d*(($)|( .*))", "$1");
				subdivision = subdivision.replaceAll("(.*?)\\WB\\d+L\\d+(($)|( .*))", "$1");
				//subdivision = subdivision.replaceAll("(.*?)\\WPAR.*", "$1"); ///PARK????
				
				subdivision = subdivision.replaceAll("(.*?)\\WBK.*", "$1");
				subdivision = subdivision.replaceAll("(.*?)\\WBLK.*", "$1");
				subdivision = subdivision.replaceAll("(.*?)\\WBLKS.*", "$1");
				subdivision = subdivision.replaceAll("(.*?)\\WBLOCK.*", "$1");
				subdivision = subdivision.replaceAll("(.*?)\\WBLOCKS.*", "$1");
				//Johnson
				subdivision = subdivision.replaceAll("(.*?)\\WBL\\d* .*", "$1");
				
				subdivision = subdivision.replaceAll("(.*?)\\WTRACT(($)|( .*))", "$1");
				subdivision = subdivision.replaceAll("(.*?)\\WPHASE(($)|( .*))", "$1");			
				subdivision = subdivision.replaceAll("(.*?)\\WSEC(($)|( .*))", "$1");
				subdivision = subdivision.replaceAll("(.*?)\\WSUB(($)|( .*))", "$1");
				subdivision = subdivision.replaceAll("(.*?)\\WUNIT(($)|( .*))", "$1");
				subdivision = subdivision.replaceAll("(.*?)\\WUNITS(($)|( .*))", "$1");
				
				subdivision = subdivision.replaceAll("(.*?)\\WOUTLOT.*", "$1");
				subdivision = subdivision.replaceAll("(.*?)\\WOUTLOTS.*", "$1");
				
				subdivision = subdivision.replaceAll(
						"(.*?)?(\\&|THE|,|1ST)( RP| REP| REPLAT| REPLATS)( \\d+| PART| NO.? \\d+|$)", "$1");
				
				subdivision = subdivision.replaceAll(
						"(.*?)(\\&|THE|,|\\d+ST|\\d+ND|\\d+\\s*RD|\\d+TH)\\s(REPLAT|REPL|REP|PLAT|PLT|PL|ADDITION|ADDN?|BG|B\\d+\\&?\\d*)(\\s+|$)", "$1");
				
				subdivision = subdivision.replaceAll("(.*?)(\\w+(?<!(CI|PROPER))TY)(-?\\w+(?<!(NOR|SOU|SMI))TH)?\\s(REPLAT|PLAT|PLT|ADDITION|ADDN?)$", "$1");
				subdivision = subdivision.replaceAll(
						"(.*?)\\s*((\\s\\w+(?<!CI|FACILI|PROPER)TY)\\s*-\\s*)?(FIRST|SECOND|THIRD|(\\w+(?<!SMI|NOR|SOU)TH))\\s(REPLAT|PLAT|PLT|ADDITION|ADDN?)\\s*(CORRECTED|REPLAT)?$", "$1");
				
				subdivision = subdivision.replaceAll(
						"(.*?)\\s(FIRST|SECOND|THIRD|(\\w+(?<!(NOR|SOU|SMI))TH))\\s?(AND|\\s*\\&\\s*|-)?\\s?(FIRST|SECOND|THIRD|(\\w+(?<!(NOR|SOU|SMI))TH)|(\\w+(?<!(CI|FACILI|PROPER))TY))\\s?(PLAT|PLT|ADDITION|ADD)?$", "$1");
				subdivision = subdivision.replaceAll(
						"(.*?)\\s(((\\w+(?<!(CI|FACILI|PROPER))TY))|(\\w+(?<!(NOR|SOU|SMI))TH))(.*?)(PLAT|PLT|ADDITION|ADD)$", "$1");
				
				subdivision = subdivision.replaceAll(
						"(.*?)\\s(\\d+ST|\\d+ND|\\d+RD|\\d+TH|\\d+TY)\\s?(AND|\\s*\\&\\s*)?\\s?(\\d+ST|\\d+ND|\\d+RD|\\d+TH|\\d+TY)\\s?(PLAT|PLT|ADDITION|ADD)?$", "$1");
				
				
				subdivision = subdivision.replaceAll(
						"(.*?)\\s(2ND PLATA REPLAT|PLAT AMENDED|AMENDED PLAT|3RD PLAT PART|PAR \\w+|1ST B\\d,|RESURVEY AMENDED|ANNEX.*)$","$1");
				subdivision = subdivision.replaceAll(
						"(.*?)(-?\\sREPLAT|RE-PLAT|\\sCORRECTED PLAT|PLAT CORRECTED|PLAT\\s*\\d*|\\s*-?\\s*AMENDED|RESURVEY|\\s*ADDI?TION|ADDN?)\\s*(PART)?$", "$1");
				
				subdivision = subdivision.replaceAll(
						"(.*?)(\\d+ST|\\d+ND|\\d+RD|\\d+TH)\\&\\s*(\\d+ST|\\d+ND|\\d+RD|\\d+TH)$", "$1");
				//subdivision = subdivision.replaceAll("(.*?)\\s(\\d+ST|\\d+ND|\\d+RD|\\d+TH|\\d+)$", "$1");
				
				subdivision = subdivision.replaceAll("(.*?)\\s(REPLAT|PLAT|ADDITION|RESURVEY)?\\s*(\\w+TY|(\\w+([^NORTH|SOUTH|SMITH])TH)|III|II|I|IV|VI?I?I?|I?XV?|XI?I?I?|XIV|NO.?\\s*\\d+|NUMBER\\s\\d+|1OTH)$", "$1");
				
				subdivision = subdivision.replaceAll("(.*?)\\s(REPLAT #\\d+|NO|NO TWO|# \\d+|REPLAT PORTION|RESURVEY AMENDED PLAT|REPLAT-CORRECTED|REPLAT OPEN SPACE|CORRECTION|,KS|II?\\s*|L33PT26|A5TR F|POR\\d+|ADDITION|FOURTH|THIRD)$", "$1");
				
				subdivision = subdivision.replaceAll("(.*?)(\\&?\\sRESURVEYS?|\\s-$|PART?\\s*\\d*-?\\d*)$", "$1");
				subdivision = subdivision.replaceAll("(\\w+)\\s\\d+(ST|ND|RD|TH|\\&\\d)?$", "$1");
				
				subdivision = subdivision.replaceAll("(.*)( \\&|,|\\s*-|\\s\\&\\s*|\\sAND\\s*)$", "$1");
				subdivision = subdivision.replaceAll("(\\w+)\\s\\d+(ST|ND|RD|TH|\\&\\d)?$", "$1");
				
				subdivision = subdivision.replaceAll("(.*)( \\().*", "$1");
				subdivision = subdivision.replaceAll("(.*)(\\().*", "$1");
				
				subdivision = subdivision.replaceAll("(.*)( \\d+[- ]\\d+)(($)|(.*))", "$1");
				subdivision = subdivision.replaceAll("(.*) (.*/.*).*", "$1");
				subdivision = subdivision.replaceAll("\"", "");
				
				//subdivision = subdivision.replaceAll("(.*?)\\WOF", "$1");
				
				sb.append("<tr>");
				sb.append("<td width='50%'>" + origSubdivision + "</td>"); 
				sb.append("<td width='50%'>" + subdivision + "</td>");
				sb.append("</tr>");	
				
				//System.out.println(subdivision);
				
				if (subdivision.length() > 0 && DBManager.addSubdivision(KS_JOHNSON, subdivision.trim()) == 0) {
					System.out.println(subdivision);
				} else { 
					System.out.println("					" + subdivision);
				}
			}
			
			sb.append("</table></body></html>");
			/*
			FileOutputStream fos = new FileOutputStream("C:\\result.html");
			fos.write(sb.toString().getBytes());
			fos.flush();
			fos.close();
			*/
			if(br!=null){
				br.close();
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		finally{
			if(br!=null){
				try{
					br.close();
				}
				catch(IOException ex){
					//nothing to do
				}
			}
		}
		
	}
	
	public static void importWayne( String inputFilePath, int type ){
		BufferedReader br=null;
        DBManager.deleteSubdivisions( type );
        
		DBConnection conn = null;
		
		try {
            
			conn = ConnectionPool.getInstance().requestConnection();
			String line;
			
			br = new BufferedReader(new FileReader( inputFilePath ));
			while ( (line = br.readLine()) != null ) {
				line = line.replace("'", "''");
				conn.executeSQL( "insert into " + DBConstants.TABLE_SUBDIVISIONS + "( `name`, `county` ) values ( '" + line + "', '" + type + "' )" );
			}
			
			
		} catch (Exception e) {
			e.printStackTrace();
		} finally{
			try{
				if(br!=null){
					try{
					br.close();
					}
					catch(IOException ex){
						//nothing to do 
					}
				}
				if (conn != null){
					ConnectionPool.getInstance().releaseConnection(conn);
				}
			}catch(BaseException e){}
		}
	}
	
	public static void importHamilton( String inputFilePath ) {
		BufferedReader br=null;
        DBManager.deleteSubdivisions( TN_HAMILTON );
        
		DBConnection conn = null;
		
		try {
            
			conn = ConnectionPool.getInstance().requestConnection();
			String line;
			
			br = new BufferedReader(new FileReader( inputFilePath ));
			while ( (line = br.readLine()) != null ) {
				if (line.length()>3)
				{
					conn.executeSQL(line.substring(0, line.length() - 1));
				}
			}
			
			
		} catch (Exception e) {
			e.printStackTrace();
		} finally{
			try{
				if(br!=null){
					try{
					br.close();
					}
					catch(IOException ex){
						//nothing to do 
					}
				}
				if (conn != null){
					ConnectionPool.getInstance().releaseConnection(conn);
				}
			}catch(BaseException e){}
		}
	}
	
	public static String cleanJackson(String subdivision)
    {
        String numbers = "\\b(\\d+\\s*ST|\\bIST\\b|\\d+ND|\\d+RD|\\d+TH|\\d+|FIRST|SECOND|THIRD|FOURTH|FIFTH|SIXTH|SEVENTH|\\WEIGHTH?|NINTH|TENTH|TWELFTH|TWENTIETH|EIGHTY-FIFTH|FIFTY-SEVENTH|FIFTY-EIGHTH|FIFTY\\s*-\\s*NINTH)";
        
        subdivision = subdivision.replaceAll("PLATLOTS", "PLAT");
        subdivision = subdivision.replaceAll("PLATLOT", "PLAT");
        subdivision = subdivision.replaceAll("(.*?\\bPLA?T?)(,|\\d+.*)", "$1");
        subdivision = subdivision.replaceAll("ACRESLOTS", "ACRES");
        subdivision = subdivision.replaceAll("ACRESLOT", "ACRES");
        subdivision = subdivision.replaceAll("OUTLOTS", "LOTS");
        subdivision = subdivision.replaceAll("OUTLOT", "LOTS");
        subdivision = subdivision.replaceAll("\\&40", "& 40");
        subdivision = subdivision.replaceAll("@", "AT");
        subdivision = subdivision.replaceAll("[\\[\\]]", "");
        subdivision = subdivision.replaceAll("\\bI\\s+(\\d+/\\d+)", "I$1"); //COLBORN & CYCLONE ROAD ESTATES 2ND PLAT I 33/4 UN
                        
        subdivision = subdivision.replaceAll("BRIDLESPUR 2ND PLAT 65-109 205", "BRIDLESPUR 2ND PLAT");
        subdivision = subdivision.replaceAll("COUNTRY MEADOWS NORTH P.U.D. 25,26&29", "COUNTRY MEADOWS NORTH P.U.D.");
        subdivision = subdivision.replaceAll("MEI TRACK I&II RP HIDDEN CREEK OFFICE PK 4TH PK", "MEI");
        subdivision = subdivision.replaceAll("WESTMINSTER SUB OF NO 1/2 09-1238", "WESTMINSTER");
        subdivision = subdivision.replaceAll("UNION HILL EIGHT PLAT RS1-7 TRA TRB OF 2P PT3P TRJ OF 6P", "UNION HILL EIGHT PLAT");
        subdivision = subdivision.replaceAll("-RESURVEY", " RESURVEY");
                
        subdivision = subdivision.replaceAll("(.*?)\\WOF\\sLT(($)|(\\s.*))", "$1");
        subdivision = subdivision.replaceAll("(.*?)\\WOF\\sLTS(($)|(\\s.*))", "$1");
        subdivision = subdivision.replaceAll("(.*?)\\WOF\\sLOT(($)|(\\s.*))", "$1");
        subdivision = subdivision.replaceAll("(.*?)\\WOF\\sLOTS(($)|(\\s.*))", "$1");
        subdivision = subdivision.replaceAll("(.*?)\\WOF\\sBLK(($)|(\\s.*))", "$1");
        subdivision = subdivision.replaceAll("(.*?)\\WOF\\sBLOCK(($)|(\\s.*))", "$1");
        subdivision = subdivision.replaceAll("(.*?)\\WOF\\sPT(($)|(\\s.*))", "$1");
        subdivision = subdivision.replaceAll("(.*?)\\WOF\\sPTS(($)|(\\s.*))", "$1");
        subdivision = subdivision.replaceAll("(.*?)\\WOF\\sPRT(($)|(\\s.*))", "$1");
        subdivision = subdivision.replaceAll("(.*?)\\WOF\\sSOUTHERN PT(($)|(\\s.*))", "$1");
        subdivision = subdivision.replaceAll("(.*?)\\WOF\\sSOTHERN PT(($)|(\\s.*))", "$1");
        subdivision = subdivision.replaceAll("(.*?)\\WOF\\sN(($)|(\\s.*))", "$1");
        subdivision = subdivision.replaceAll("(.*?)\\WOF\\sS(($)|(\\s.*))", "$1");
        subdivision = subdivision.replaceAll("(.*?)\\WOF\\sE(($)|(\\s.*))", "$1");
        subdivision = subdivision.replaceAll("(.*?)\\WOF\\sV(($)|(\\s.*))", "$1");
        subdivision = subdivision.replaceAll("(.*?)\\WOF\\sTRACT(($)|(\\s.*))", "$1");
        subdivision = subdivision.replaceAll("(.*?)\\WOF\\sTR(($)|(\\s.*))", "$1");
        subdivision = subdivision.replaceAll("(.*?)\\WOF\\sTRACTS(($)|(\\s.*))", "$1");
        subdivision = subdivision.replaceAll("(.*?)\\WOF\\sOUTLOT(($)|(\\s.*))", "$1");
        subdivision = subdivision.replaceAll("(.*?)\\WOF\\s\\(UNITS(($)|(\\s.*))", "$1");
        subdivision = subdivision.replaceAll("(.*?)\\WOF\\s\\(LOTS(($)|(\\s.*))", "$1");
        subdivision = subdivision.replaceAll("(.*?)\\WOF\\s\\(LTS(($)|(\\s.*))", "$1");
        subdivision = subdivision.replaceAll("(.*?)\\WOF\\sREPLAT(($)|(\\s.*))", "$1");
        subdivision = subdivision.replaceAll("(.*?)\\WOF\\sSO(($)|(\\s.*))", "$1");
        subdivision = subdivision.replaceAll("(.*?)\\WOF\\sRS(($)|(\\s.*))", "$1");
        
        subdivision = subdivision.replaceAll(
                "(.*?)"+numbers+"((\\s*(\\bAND\\b|\\bTO\\b|-|,|&)\\s*)"+numbers+")*\\s+P(AR)?T\\b", "$1");              
        subdivision = subdivision.replaceAll("(.*?)\\WPART OF(($)|(\\s.*))", "$1");
        subdivision = subdivision.replaceAll("(.*?)\\WPT(($)|(\\s.*))", "$1");
        
        subdivision = subdivision.replaceAll("(.*?)\\WLT(($)|(\\s.*))", "$1");
        subdivision = subdivision.replaceAll("(.*?)\\WLOT(($)|(\\s.*))", "$1");
        subdivision = subdivision.replaceAll("(.*?)\\WLOTS(($)|(\\s.*))", "$1");
        subdivision = subdivision.replaceAll("(.*?)\\WL0TS(($)|(\\s.*))", "$1");
        subdivision = subdivision.replaceAll("(.*?)\\WLTS(($)|(\\s.*))", "$1");
        subdivision = subdivision.replaceAll("(.*?)\\WLT\\d+.*", "$1");
        subdivision = subdivision.replaceAll("(.*?)\\WLOT\\d+.*", "$1");
        subdivision = subdivision.replaceAll("(.*?)\\WLOTS\\d+.*", "$1");
        subdivision = subdivision.replaceAll("(.*?)\\WLOS\\s*\\d+.*", "$1");
        subdivision = subdivision.replaceAll("(.*?)\\WLTS\\d+.*", "$1");
        subdivision = subdivision.replaceAll("(.*?)\\W[L|B|I|RS]\\d+.*", "$1");
        
        subdivision = subdivision.replaceAll("(.*?)\\WADD\\(.*", "$1");
        
        subdivision = subdivision.replaceAll("(.*?)\\WBL(($)|(\\s.*))", "$1");
        subdivision = subdivision.replaceAll("(.*?)\\WBK(($)|(\\s.*))", "$1");
        subdivision = subdivision.replaceAll("(.*?)\\WBLK(($)|(\\s.*))", "$1");
        subdivision = subdivision.replaceAll("(.*?)\\WBLKS(($)|(\\s.*))", "$1");
        subdivision = subdivision.replaceAll("(.*?)\\WBLOCK(($)|(\\s.*))", "$1");
        subdivision = subdivision.replaceAll("(.*?)\\WBLOCKS(($)|(\\s.*))", "$1");
        subdivision = subdivision.replaceAll("(.*?)\\WBL\\d+.*", "$1");
        subdivision = subdivision.replaceAll("(.*?)(\\W|\\s\\d+|\\s\\d+\\-\\d+)BLK\\d+.*", "$1");
        subdivision = subdivision.replaceAll("(.*?)\\WBLKS\\d+.*", "$1");
        subdivision = subdivision.replaceAll("(.*?)\\WBLOCK\\d+.*", "$1");
        
        subdivision = subdivision.replaceAll("(.*?)\\WTR(($)|(\\s.*))", "$1");
        subdivision = subdivision.replaceAll("(.*?)\\WTRS(($)|(\\s.*)|(\\d+.*))", "$1");
        subdivision = subdivision.replaceAll("(.*?)\\WTRACT(($)|(\\s.*))", "$1");
        subdivision = subdivision.replaceAll("(.*?)\\WTRACTS(($)|(\\s.*))", "$1");
        
        subdivision = subdivision.replaceAll("(.*?)\\WSEC(($)|(\\b\\s*.*))", "$1");
        subdivision = subdivision.replaceAll("(.*?)\\s[A-Za-z]\\d+.*", "$1");
        
        subdivision = subdivision.replaceAll("(.*?)\\sUN(($)|(\\s.*))", "$1");
        if (subdivision.toUpperCase().indexOf("UNIT DEVELOPMENT") == -1)
            subdivision = subdivision.replaceAll("(.*?)\\sUNIT(($)|(\\s.*))", "$1");
        subdivision = subdivision.replaceAll("(.*?)\\WUNIT[\\s\\d+].*", "$1");
        
        subdivision = subdivision.replaceAll("(.*?)\\WUNITS(($)|(\\s.*))", "$1");
        subdivision = subdivision.replaceAll("(.*?)\\WIUNIT(($)|(\\s.*))", "$1");
        subdivision = subdivision.replaceAll("(.*?)\\WIUNITS(($)|(\\s.*))", "$1");
        
        subdivision = subdivision.replaceAll("(.*?)\\WAR\\s.*", "$1");
        
        subdivision = subdivision.replaceAll("(.*?)\\sNW\\d+(($)|(?!(RD|ST|TH|ND)).*$)", "$1");
        subdivision = subdivision.replaceAll("(.*?)\\sNE\\d+(($)|(?!(RD|ST|TH|ND)).*$)", "$1");
        subdivision = subdivision.replaceAll("(.*?)\\sSW\\d+(($)|(?!(RD|ST|TH|ND)).*$)", "$1");
        subdivision = subdivision.replaceAll("(.*?)\\sSE\\d+(($)|(?!(RD|ST|TH|ND)).*$)", "$1");
        
        subdivision = subdivision.replaceAll("(.*?)\\sN\\s\\d+(($)|(?!(RD|ST|TH|ND)).*$)", "$1");
        subdivision = subdivision.replaceAll("(.*?)\\sE\\s\\d+(($)|(?!(RD|ST|TH|ND)).*$)", "$1");
        subdivision = subdivision.replaceAll("(.*?)\\sS\\s\\d+(($)|(?!(RD|ST|TH|ND)).*$)", "$1");
        subdivision = subdivision.replaceAll("(.*?)\\sW\\s\\d+(($)|(?!(RD|ST|TH|ND)).*$)", "$1");
        
        subdivision = subdivision.replaceAll("(.*?)\\sN\\s\\d/.*", "$1");
        subdivision = subdivision.replaceAll("(.*?)\\sE\\s\\d/.*", "$1");
        subdivision = subdivision.replaceAll("(.*?)\\sS\\s\\d/.*", "$1");
        subdivision = subdivision.replaceAll("(.*?)\\sW\\s\\d/.*", "$1");
        
        subdivision = subdivision.replaceAll("(.*?)\\s[NSWEI]$", "$1");
        
        subdivision = subdivision.replaceAll("(.*?)\\sOR\\s.*", "$1");

        subdivision = subdivision.replaceAll("(.*)( \\d+[- ]\\d+)(($)|(?!(RD|ST|TH|ND)).*$)", "$1");
        
        subdivision = subdivision.replaceAll("(.*) (.*/\\d+).*", "$1");
        subdivision = subdivision.replaceAll("(.*?)((\\\\\\&)|(/)|(\\\\))(.*)", "$1");
        
        subdivision = subdivision.replaceAll("(.*)[\\&,-]\\s*$", "$1");
        subdivision = subdivision.replaceAll("\"", "");
        
        subdivision = subdivision.replaceAll("(.*)( \\().*", "$1");
        subdivision = subdivision.replaceAll("(.*)(\\().*", "$1");
        subdivision = subdivision.replaceAll("(.*?)\\sOF(($)|(\\s+)$)", "$1");
        
        subdivision = subdivision.replaceAll(
                "(.*?)\\s(NO\\s\\d+\\-\\d+|\\&|\\s*\\d+[\\&\\.]\\d+(?!\\s+MILLION).*|\\w\\d+\\-\\d+)$", "$1"); //NORTH 2.5 MILLION GALLON TANK LT 1
        
        subdivision = subdivision.replaceAll("(.*?)\\s(MO)$", "$1");
        
        subdivision = subdivision.replaceAll(
                "(.*?)\\s(K C|KC|LS|LE|BS|BL|SU|RA|GV|IN|UN|GR|BU|VI|B|MO|VAC|NW|THE)$", "$1");
        
        subdivision = subdivision.replaceAll("(.*?)\\s*$", "$1");
        
        subdivision = subdivision.replaceAll("(\\b(RE)?PL(AT|T)?S?) P\\.?U\\.?D\\.?", "$1"); 
        
        subdivision = subdivision.replaceAll("(.*?)(-?\\s*REPLAT.*?$)", "$1");
            
        subdivision = subdivision.replaceAll(
                "(.*?)(?!FIRST|SECOND)&?\\s*"+numbers+"((\\s*(\\bAND\\b|\\bTO\\b|-|,|&)\\s*)"+numbers+")*(.*?REPLATS| REPLAT| REPL| REP|.*?PLATS|.*?PLAT|.*?PLT|.*?PL|.*P| ADDITION| ADD| PHASE| PH| RESURVEY| RES| AM)\\b", "$1"); 
        //FIRST BIBLE BAPTIST 1ST PLAT LOT1 I69/87 BS; GREGORY ESTATES & 1ST-5TH PLAT (LTS 1-11) LE; HEARNE'S 2ND AND 3RD ADDITION LOTS 16A & 16B I71/39 LE; HARTMAN HERITAGE CENTER 1,2,4 & 5TH PL (LTS 1-4A & 5-6) IN; HOCKER HIGHLAND 1ST AM LOT 1-4 I28/79 IN
                        
        subdivision = subdivision.replaceAll("(?i)(.*?)(supplement)?(\\s*?plat\\s*?\\d+)$", "$1");
        
        subdivision = subdivision.replaceAll("(.*?)\\s(TO|\\d+|ADD(ITION)? (#|NO\\.?\\s)\\d*|\\d+\\s*PLA?T?|\\d+P|PHASE.*?|NO\\s\\d+|NO|CONTINUATION)$", "$1");
        subdivision = subdivision.replaceAll("(.*?)\\s(ADDITION)\\s*(CONTINUATION|NO\\d*)$", "$1");
        
        subdivision = subdivision.replaceAll("(.*?)\\s\\&?\\s*(A\\s?)(REPLATS?|REPL|REP|RP|RESURVEYS?|RESURBEYS?|RES-REV|RES|RS)\\s*(OF)?\\s*(PART)?$", "$1"); //EXECUTIVE PARK 25TH PLAT, A REP LOT A 03-0536 KC 
        
        subdivision = subdivision.replaceAll("(.*?)\\s(CORRECTED|CORREC|CORR|COR|FINAL|AMENDED|AMEND|AMEN|REVISED|SUPPLEMENT)\\s*(PLATS?|PT|PL)?$", "$1");
        subdivision = subdivision.replaceAll("(.*?)\\s(CORRECTED|CORREC|CORR|COR|FINAL|AMENDED|AMEND|AMEN|REVISED|SUPPLEMENT)\\b\\s*(PLAT?|PT|PL)?.*?$", "$1");
        
        subdivision = subdivision.replaceAll("(.*?)\\&?\\s(-?RESURVEYS?|RE-SURVEY|RES|RS|REPLATS?|REPT|REP|RP)\\b\\s*\\&?.*?$", "$1");
        subdivision = subdivision.replaceAll ("(.*?)\\s(PLA?T?)\\s\\d*\\w?,?$", "$1");  /*LAKEWOOD BUS CTR I-470 PLAT A,*/
        
        subdivision = subdivision.replaceAll("(.*?),?\\s(RP|REP|THE RES)$", "$1");
        subdivision = subdivision.replaceAll("(.*?)-\\s*(REP)$", "$1");
        
        if (subdivision.toUpperCase().indexOf("ROSEWOOD HILLS") == -1) //B 2876
        	subdivision = subdivision.replaceAll("(.*?)-?\\s*(\\&|\\d+\\s*ST|\\d+ND|\\d+RD|\\d+TH|ADDITION|ADD|THE|TO|\\d+P|SURV)$", "$1"); 
        
        subdivision = subdivision.replaceAll("\\d+[A-Z](-\\d+[A-Z])?\\s*$", ""); //EAGLE CREEK TOWNHOMES 2ND PL 40A-40D TR E1 E2 LT E I93/10 LE  
        
        subdivision = subdivision.replaceAll("\\bAREA ([IVXC]+|\\d+)\\s*$", ""); //EASTLAND CENTER 2ND PLAT AREA IV (LTS 4-9 &TR A) I68/67 IN
        
        subdivision = subdivision.replaceAll("\\bBS\\s*$", ""); //COMPLETE AUTO REPAIR SERVICES CENTER, A COMMERCIAL PUD BS
        
        subdivision = subdivision.replaceAll("\\s+-[A-Z]\\s*$", ""); //HANCOCK PLACE -A- 04-2703 KC  
        
        subdivision = subdivision.replaceAll("[\\&,-]\\s*$", ""); //HARDESTY HOME, THE RES OF LOT 7 04-2833 KC  

        subdivision = subdivision.replaceAll("\\bNO\\.?\\s*$", ""); //HOLT NO. 2 04-5276 LE     
        
        subdivision = subdivision.replaceAll("\\bTO KANSAS CITY MISSOURI\\b", ""); //GATES THIRD ADDITION TO KANSAS CITY MISSOURI LTS 1-38 K14/60               
        subdivision = subdivision.replaceAll("\\bC(I?T)?Y OF .+", ""); //HOWARD'S W B 3RD ADD CITY OF LEES SUMMIT 04-5633 LE
                        
        subdivision = subdivision.replaceAll(numbers+"\\s+PART\\s*$", ""); //JOHNSONS 1ST ADD 2ND PART LOTS 13, 32 & 33 IN
        subdivision = subdivision.replaceAll("\\bPL(A?T)?S?\\s*$", ""); //LEMONE SMITH BUSN & RAIL CENTER PL7 LOTS 9A & 9G I65/29 LS  
        
        subdivision = subdivision.replaceAll("\\bKC\\s*$", ""); //MEADOWVIEW [KC] (LTS 1-15) B24/67 KC
        
        subdivision = subdivision.replaceAll("\\bMINOR SUB\\s*$", ""); //RIVER OAKS 4TH PLAT MINOR SUB PT LOT 19-22 BLK 15 K40/11 GR 
        
        subdivision = subdivision.replaceAll("\\bINCL\\s*$", ""); //VILLAS AT MEADOWS AT SUMMIT RIDGE 6TH PL INCL LT 16-17  
        
        subdivision = subdivision.replaceAll("\\bOF PORTION\\b", ""); //VILLAS AT SUMMIT RIDGE 2ND REPL OF PORTION TRACT D & BLK 1
        
        subdivision = subdivision.replaceFirst("(?i)\\b(P\\.U\\.D\\.|PRO)\\s*$", ""); //Summerfield East Pro bug #576
        subdivision = subdivision.replaceFirst("(?i)\\b(PUD|PR\\-O|PRO\\-O)\\s*$", ""); //Summerfield East Pro bug #576 - 2
        
        subdivision = subdivision.replaceFirst("\\bADD(ITION)? TO .+", ""); //fix for bug 1602 -TR subdiv is cleaned of ADD (see bug 594), so RO subdiv needs to be cleaned too 
        
        subdivision = subdivision.trim();   
        
        return subdivision;
    }
    
	public static void main(String[] args) {
		
		//importKane("D:\\subdivILKaneRO.txt");
		//importJackson("");
		//importJohnson();
		//importHamilton();
		
		//System.err.println(SubdivisionMatcher.getInstance(MO_JACKSON).match("MEADOW"));
	}
}