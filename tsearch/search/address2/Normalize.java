package ro.cst.tsearch.search.address2;
//package newest; //debug

import java.util.*;
import java.util.regex.Pattern;
import java.util.regex.Matcher;

import org.apache.commons.lang.StringUtils;

//optimize the treatment of the atoms
//make sure that the rule of looking at the suffix (and if present go to the extended form as opposed to the abreviated one)
//is ok

//see why "Country road north east" gives "country road t"
//"100 main avenue drive" is treated incorrectly, not as "100 main avenue dr"

//decide if we change all main numbers from forms such as 11th main street to 11 main street

public class Normalize {
	//debug
	//public static boolean disableUpdates = false;
	//public static boolean disableUpdates = true;
	
	public static final int ABREV_NONE = 0;
	public static final int ABREV_STATE = 1;
	public static final int ABREV_SUFFIX = 2;
	public static final int ABREV_IDENT = 3;
	public static final int ABREV_DIR = 4;
	
	/**
	 * Other common types - not actually returned by this class.
	 */
	public static final int ABREV_NUMBER = 5;  // composite number type
	public static final int ABREV_RANGE = 6;   // range type, as defined in Ranges.java
	public static final int ABREV_LETTERS = 7; // letter range type, as defined in Ranges.java
	public static final int ABREV_LIST = 8;     // list type, as defined in Ranges.java
	
	public static final String COMPOSITE_NUMBER = "([0-9]+)([a-zA-Z]?)";
	
	private static final HashMap<String,String> states;
	private static final HashMap<String,String> suffixes;
	private static final HashMap<String,String> specialSuffixes;
	private static final HashMap<String,String> istateSuffixes;
	private static final HashMap<String,String> identifiers;
	private static final HashMap<String,String> directions;
	private static final HashMap<String,Integer> directionTypes;	
	private static final HashMap<String,String> numbers;
	private static final HashSet<String> saints;

	
	/**
	 * Prevent initialization
	 */
	private Normalize() {}
		
	// static block for the variables
	static {
		directions = new HashMap<String,String>();
		directions.put("E", "E");
		directions.put("EAST", "E");
		directions.put("E-R", "EAST");
		directions.put("N", "N");
		directions.put("NO", "N");
		directions.put("NORTH", "N");
		directions.put("N-R", "NORTH");
		directions.put("NE", "NE");
		directions.put("NORTHEAST", "NE");
		directions.put("NE-R", "NORTHEAST");
		directions.put("NORTHWEST", "NW");
		directions.put("NW-R", "NORTHWEST");
		directions.put("NW", "NW");
		directions.put("S", "S");
		directions.put("SO", "S");
		directions.put("SOUTH", "S");
		directions.put("S-R", "SOUTH");
		directions.put("SE", "SE");
		directions.put("SOUTHEAST", "SE");
		directions.put("SE-R", "SOUTHEAST");
		directions.put("SOUTHWEST", "SW");
		directions.put("SW-R", "SOUTHWEST");
		directions.put("SW", "SW");
		directions.put("W", "W");
		directions.put("WEST", "W");
		directions.put("W-R", "WEST");
		
		directionTypes = new HashMap<String,Integer>();
		// N,S
		directionTypes.put("N",1);
		directionTypes.put("NO",1);
		directionTypes.put("NORTH",1);
		directionTypes.put("S",1);
		directionTypes.put("SO",1);
		directionTypes.put("SOUTH",1);
		// E,W
		directionTypes.put("E",2);
		directionTypes.put("EAST",2);
		directionTypes.put("W",2);
		directionTypes.put("WEST",2);
		// NE,NW,SE,SW
		directionTypes.put("NE",3);
		directionTypes.put("NORTHEAST",3);
		directionTypes.put("NW",3);
		directionTypes.put("NORTHWEST",3);		
		directionTypes.put("SE",3);
		directionTypes.put("SOUTHEAST",3);
		directionTypes.put("SW",3);
		directionTypes.put("SOUTHWEST",3);
		
		numbers = new HashMap<String,String>();
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
		
		states = new HashMap<String,String>();
		states.put("ARMED FORCES AMERICA", "AA");
		states.put("ARMED FORCES EUROPE", "AE");
		states.put("ALASKA", "AK");
		states.put("ALABAMA", "AL");
		states.put("ARMED FORCES PACIFIC", "AP");
		states.put("ARKANSAS", "AR");
		states.put("ARIZONA", "AZ");
		states.put("CALIFORNIA", "CA");
		states.put("COLORADO", "CO");
		states.put("CONNECTICUT", "CT");
		states.put("DISTRICT OF COLUMBIA", "DC");
		states.put("DELAWARE", "DE");
		states.put("FLORIDA", "FL");
		states.put("GEORGIA", "GA");
		states.put("HAWAII", "HI");
		states.put("IOWA", "IA");
		states.put("IDAHO", "ID");
		states.put("ILLINOIS", "IL");
		states.put("INDIANA", "IN");
		states.put("KANSAS", "KS");
		states.put("KENTUCKY", "KY");
		states.put("LOUISIANA", "LA");
		states.put("MASSACHUSETTS", "MA");
		states.put("MARYLAND", "MD");
		states.put("MAINE", "ME");
		states.put("MICHIGAN", "MI");
		states.put("MINNESOTA", "MN");
		states.put("MISSOURI", "MO");
		states.put("MISSISSIPPI", "MS");
		states.put("MONTANA", "MT");
		states.put("NORTH CAROLINA", "NC");
		states.put("NORTH DAKOTA", "ND");
		states.put("NEBRASKA", "NE");
		states.put("NEW HAMPSHIRE", "NH");
		states.put("NEW JERSEY", "NJ");
		states.put("NEW MEXICO", "NM");
		states.put("NEVADA", "NV");
		states.put("NEW YORK", "NY");
		states.put("OHIO", "OH");
		states.put("OKLAHOMA", "OK");
		states.put("OREGON", "OR");
		states.put("PENNSYLVANIA", "PA");
		states.put("RHODE ISLAND", "RI");
		states.put("SOUTH CAROLINA", "SC");
		states.put("SOUTH DAKOTA", "SD");
		states.put("TENNESSEE", "TN");
		states.put("TEXAS", "TX");
		states.put("UTAH", "UT");
		states.put("VIRGINIA", "VA");
		states.put("VERMONT", "VT");
		states.put("WASHINGTON", "WA");
		states.put("WISCONSIN", "WI");
		states.put("WEST VIRGINIA", "WV");
		states.put("WYOMING", "WY");
		
		identifiers = new HashMap<String,String>();
		identifiers.put("APARTMENT", "APT");
		identifiers.put("APT-R", "APARTMENT");
		identifiers.put("APT", "APT");
		identifiers.put("BLDG", "BLDG");
		identifiers.put("BUILDING", "BLDG");
		identifiers.put("BLDG-R", "BUILDING");
		identifiers.put("BOX", "BOX");
		identifiers.put("BOX-R", "BOX");
		identifiers.put("BASEMENT", "BSMT");
		identifiers.put("BSMT-R", "BASEMENT");
		identifiers.put("BSMT", "BSMT");
		identifiers.put("DEPARTMENT", "DEPT");
		identifiers.put("DEPT-R", "DEPARTMENT");
		identifiers.put("DEPT", "DEPT");
		identifiers.put("FL", "FL");
		identifiers.put("FLOOR", "FL");
		identifiers.put("FL-R", "FLOOR");
		identifiers.put("FRNT", "FRNT");
		identifiers.put("FRONT", "FRNT");
		identifiers.put("FRNT-R", "FRONT");
		identifiers.put("HANGER", "HNGR");
		identifiers.put("HNGR-R", "HANGER");
		identifiers.put("HNGR", "HNGR");
		identifiers.put("KEY", "KEY");
		identifiers.put("KEY-R", "KEY");
		identifiers.put("LBBY", "LBBY");
		identifiers.put("LOBBY", "LBBY");
		identifiers.put("LBBY-R", "LOBBY");
		identifiers.put("LOT", "LOT");
		identifiers.put("LOT-R", "LOT");
		identifiers.put("LOWER", "LOWR");
		identifiers.put("LOWR-R", "LOWER");
		identifiers.put("LOWR", "LOWR");
		identifiers.put("OFC", "OFC");
		identifiers.put("OFFICE", "OFC");
		identifiers.put("OFC-R", "OFFICE");
		identifiers.put("PENTHOUSE", "PH");
		identifiers.put("PH-R", "PENTHOUSE");
		identifiers.put("PH", "PH");
		identifiers.put("PIER", "PIER");
		identifiers.put("PIER-R", "PIER");
		identifiers.put("PMB", "PMB");
		identifiers.put("PMB-R", "PMB");
		identifiers.put("REAR", "REAR");
		identifiers.put("REAR-R", "REAR");
		identifiers.put("RM", "RM");
		identifiers.put("ROOM", "RM");
		identifiers.put("RM-R", "ROOM");
		identifiers.put("SIDE", "SIDE");
		identifiers.put("SIDE-R", "SIDE");
		identifiers.put("SLIP", "SLIP");
		identifiers.put("SLIP-R", "SLIP");
		identifiers.put("SPACE", "SPC");
		identifiers.put("SPC-R", "SPACE");
		identifiers.put("SPC", "SPC");
		identifiers.put("STE", "STE");
		identifiers.put("SUITE", "STE");
		identifiers.put("STE-R", "SUITE");
		identifiers.put("STOP", "STOP");
		identifiers.put("STOP-R", "STOP");
		identifiers.put("TRAILER", "TRLR");
		identifiers.put("TRLR-R", "TRAILER");
		identifiers.put("TRLR", "TRLR");
		identifiers.put("UNIT", "UNIT");
		identifiers.put("UNIT-R", "UNIT");
		identifiers.put("UPPER", "UPPR");
		identifiers.put("UPPR-R", "UPPER");
		identifiers.put("UPPR", "UPPR");
		identifiers.put("UPR", "UPPR");
		
		suffixes = new HashMap<String,String>();
		suffixes.put("ALLEE", "ALY");
		suffixes.put("ALLEY", "ALY");
		suffixes.put("ALY-R", "ALLEY");
		suffixes.put("ALLY", "ALY");
		suffixes.put("ALY", "ALY");
		suffixes.put("AL", "ALY");
		suffixes.put("ANEX", "ANX");
		suffixes.put("ANNEX", "ANX");
		suffixes.put("ANX-R", "ANNEX");
		suffixes.put("ANNX", "ANX");
		suffixes.put("ANX", "ANX");
		suffixes.put("ARC", "ARC");
		suffixes.put("ARCADE", "ARC");
		suffixes.put("ARC-R", "ARCADE");
		suffixes.put("AV", "AVE");
		suffixes.put("AVE", "AVE");
		suffixes.put("AVEN", "AVE");
		suffixes.put("AVENU", "AVE");
		suffixes.put("AVENUE", "AVE");
		suffixes.put("AVE-R", "AVENUE");
		suffixes.put("AVN", "AVE");
		suffixes.put("AVNUE", "AVE");
		suffixes.put("BCH", "BCH");
		suffixes.put("BEACH", "BCH");
		suffixes.put("BCH-R", "BEACH");
		suffixes.put("BG", "BG");
		suffixes.put("BURG", "BG");
		suffixes.put("BG-R", "BURG");
		suffixes.put("BGS", "BGS");
		suffixes.put("BURGS", "BGS");
		suffixes.put("BGS-R", "BURGS");
		suffixes.put("BLF", "BLF");
		suffixes.put("BLUF", "BLF");
		suffixes.put("BLUFF", "BLF");
		suffixes.put("BLF-R", "BLUFF");
		suffixes.put("BLFS", "BLFS");
		suffixes.put("BLUFFS", "BLFS");
		suffixes.put("BLFS-R", "BLUFFS");
		suffixes.put("BLVD", "BLVD");
		suffixes.put("BLVRD", "BLVD");
		suffixes.put("BOUL", "BLVD");
		suffixes.put("BOULEVARD", "BLVD");
		suffixes.put("BLVD-R", "BOULEVARD");
		suffixes.put("BOULOVARD", "BLVD");
		suffixes.put("BOULV", "BLVD");
		suffixes.put("BOULVRD", "BLVD");
		suffixes.put("BULAVARD", "BLVD");
		suffixes.put("BULEVARD", "BLVD");
		suffixes.put("BULLEVARD", "BLVD");
		suffixes.put("BULOVARD", "BLVD");
		suffixes.put("BULVD", "BLVD");
		suffixes.put("BD", "BLVD");
		suffixes.put("BL", "BLVD");
		suffixes.put("BV", "BLVD");
		suffixes.put("BEND", "BND");
		suffixes.put("BND-R", "BEND");
		suffixes.put("BND", "BND");
		suffixes.put("BR", "BR");
		suffixes.put("BRANCH", "BR");
		suffixes.put("BR-R", "BRANCH");
		suffixes.put("BRNCH", "BR");
		suffixes.put("BRDGE", "BRG");
		suffixes.put("BRG", "BRG");
		suffixes.put("BRGE", "BRG");
		suffixes.put("BRIDGE", "BRG");
		suffixes.put("BRG-R", "BRIDGE");
		suffixes.put("BRK", "BRK");
		suffixes.put("BROOK", "BRK");
		suffixes.put("BRK-R", "BROOK");
		suffixes.put("BRKS", "BRKS");
		suffixes.put("BROOKS", "BRKS");
		suffixes.put("BRKS-R", "BROOKS");
		suffixes.put("BOT", "BTM");
		suffixes.put("BOTTM", "BTM");
		suffixes.put("BOTTOM", "BTM");
		suffixes.put("BTM-R", "BOTTOM");
		suffixes.put("BTM", "BTM");
		suffixes.put("BYP", "BYP");
		suffixes.put("BYPA", "BYP");
		suffixes.put("BYPAS", "BYP");
		suffixes.put("BYPASS", "BYP");
		suffixes.put("BYP-R", "BYPASS");
		suffixes.put("BYPS", "BYP");
		suffixes.put("BAYOO", "BYU");
		suffixes.put("BAYOU", "BYU");
		suffixes.put("BYU-R", "BAYOU");
		suffixes.put("BYO", "BYU");
		suffixes.put("BYOU", "BYU");
		suffixes.put("BYU", "BYU");
		suffixes.put("CI", "CIR");
		suffixes.put("CIR", "CIR");
		suffixes.put("CIRC", "CIR");
		suffixes.put("CIRCEL", "CIR");
		suffixes.put("CIRCL", "CIR");
		suffixes.put("CIRCLE", "CIR");
		suffixes.put("CIR-R", "CIRCLE");
		suffixes.put("CRCL", "CIR");
		suffixes.put("CRCLE", "CIR");
		suffixes.put("CIRCELS", "CIRS");
		suffixes.put("CIRCLES", "CIRS");
		suffixes.put("CIRS-R", "CIRCLES");
		suffixes.put("CIRCLS", "CIRS");
		suffixes.put("CIRCS", "CIRS");
		suffixes.put("CIRS", "CIRS");
		suffixes.put("CRCLES", "CIRS");
		suffixes.put("CRCLS", "CIRS");
		suffixes.put("CLB", "CLB");
		suffixes.put("CLUB", "CLB");
		suffixes.put("CLB-R", "CLUB");
		suffixes.put("CLF", "CLF");
		suffixes.put("CLIF", "CLF");
		suffixes.put("CLIFF", "CLF");
		suffixes.put("CLF-R", "CLIFF");
		suffixes.put("CLFS", "CLFS");
		suffixes.put("CLIFFS", "CLFS");
		suffixes.put("CLFS-R", "CLIFFS");
		suffixes.put("CLIFS", "CLFS");
		suffixes.put("CMN", "CMN");
		suffixes.put("COMMON", "CMN");
		suffixes.put("CMN-R", "COMMON");
		suffixes.put("COMN", "CMN");
		suffixes.put("COR", "COR");
		suffixes.put("CORN", "COR");
		suffixes.put("CORNER", "COR");
		suffixes.put("COR-R", "CORNER");
		suffixes.put("CRNR", "COR");
		suffixes.put("CORNERS", "CORS");
		suffixes.put("CORS-R", "CORNERS");
		suffixes.put("CORNRS", "CORS");
		suffixes.put("CORS", "CORS");
		suffixes.put("CRNRS", "CORS");
		suffixes.put("CAMP", "CP");
		suffixes.put("CP-R", "CAMP");
		suffixes.put("CMP", "CP");
		suffixes.put("CP", "CP");
		suffixes.put("CAPE", "CPE");
		suffixes.put("CPE-R", "CAPE");
		suffixes.put("CPE", "CPE");
		suffixes.put("CRECENT", "CRES");
		suffixes.put("CRES", "CRES");
		suffixes.put("CRESCENT", "CRES");
		suffixes.put("CRES-R", "CRESCENT");
		suffixes.put("CRESENT", "CRES");
		suffixes.put("CRSCNT", "CRES");
		suffixes.put("CRSENT", "CRES");
		suffixes.put("CRSNT", "CRES");
		suffixes.put("CK", "CRK");
		suffixes.put("CR", "CRK");
		suffixes.put("CREEK", "CRK");
		suffixes.put("CRK-R", "CREEK");
		suffixes.put("CREK", "CRK");
		suffixes.put("CRK", "CRK");
		suffixes.put("COARSE", "CRSE");
		suffixes.put("COURSE", "CRSE");
		suffixes.put("CRSE-R", "COURSE");
		suffixes.put("CRSE", "CRSE");
		suffixes.put("CREST", "CRST");
		suffixes.put("CRST-R", "CREST");
		suffixes.put("CRST", "CRST");
		suffixes.put("CAUSEWAY", "CSWY");
		suffixes.put("CSWY-R", "CAUSEWAY");
		suffixes.put("CAUSEWY", "CSWY");
		suffixes.put("CAUSWAY", "CSWY");
		suffixes.put("CAUSWY", "CSWY");
		suffixes.put("CSWY", "CSWY");
		suffixes.put("CORT", "CT");
		suffixes.put("COURT", "CT");
		suffixes.put("CT-R", "COURT");
		suffixes.put("CRT", "CT");
		suffixes.put("CT", "CT");
		suffixes.put("CEN", "CTR");
		suffixes.put("CENT", "CTR");
		suffixes.put("CENTER", "CTR");
		suffixes.put("CTR-R", "CENTER");
		suffixes.put("CENTR", "CTR");
		suffixes.put("CENTRE", "CTR");
		suffixes.put("CNTER", "CTR");
		suffixes.put("CNTR", "CTR");
		suffixes.put("CTR", "CTR");
		suffixes.put("CENS", "CTRS");
		suffixes.put("CENTERS", "CTRS");
		suffixes.put("CTRS-R", "CENTERS");
		suffixes.put("CENTRES", "CTRS");
		suffixes.put("CENTRS", "CTRS");
		suffixes.put("CENTS", "CTRS");
		suffixes.put("CNTERS", "CTRS");
		suffixes.put("CNTRS", "CTRS");
		suffixes.put("CTRS", "CTRS");
		suffixes.put("COURTS", "CTS");
		suffixes.put("CTS-R", "COURTS");
		suffixes.put("CTS", "CTS");
		suffixes.put("CRV", "CURV");
		suffixes.put("CURV", "CURV");
		suffixes.put("CURVE", "CURV");
		suffixes.put("CURV-R", "CURVE");
		suffixes.put("COV", "CV");
		suffixes.put("COVE", "CV");
		suffixes.put("CV-R", "COVE");
		suffixes.put("CV", "CV");
		suffixes.put("COVES", "CVS");
		suffixes.put("CVS-R", "COVES");
		suffixes.put("COVS", "CVS");
		suffixes.put("CVS", "CVS");
		suffixes.put("CAN", "CYN");
		suffixes.put("CANYN", "CYN");
		suffixes.put("CANYON", "CYN");
		suffixes.put("CYN-R", "CANYON");
		suffixes.put("CNYN", "CYN");
		suffixes.put("CYN", "CYN");
		suffixes.put("DAL", "DL");
		suffixes.put("DALE", "DL");
		suffixes.put("DL-R", "DALE");
		suffixes.put("DL", "DL");
		suffixes.put("DAM", "DM");
		suffixes.put("DM-R", "DAM");
		suffixes.put("DM", "DM");
		suffixes.put("DR", "DR");
		suffixes.put("DRIV", "DR");
		suffixes.put("DRIVE", "DR");
		suffixes.put("DRVIE", "DR");
		suffixes.put("DR-R", "DRIVE");
		suffixes.put("DRV", "DR");
		suffixes.put("DRIVES", "DRS");
		suffixes.put("DRS-R", "DRIVES");
		suffixes.put("DRIVS", "DRS");
		suffixes.put("DRS", "DRS");
		suffixes.put("DRVS", "DRS");
		suffixes.put("DIV", "DV");
		suffixes.put("DIVD", "DV");
		suffixes.put("DIVID", "DV");
		suffixes.put("DIVIDE", "DV");
		suffixes.put("DV-R", "DIVIDE");
		suffixes.put("DV", "DV");
		suffixes.put("DVD", "DV");
		suffixes.put("EST", "EST");
		suffixes.put("ESTA", "EST");
		suffixes.put("ESTATE", "EST");
		suffixes.put("EST-R", "ESTATE");
		suffixes.put("ESTAS", "ESTS");
		suffixes.put("ESTATES", "ESTS");
		suffixes.put("ESTS-R", "ESTATES");
		suffixes.put("ESTS", "ESTS");
		suffixes.put("EXP", "EXPY");
		suffixes.put("EXPR", "EXPY");
		suffixes.put("EXPRESS", "EXPY");
		suffixes.put("EXPRESSWAY", "EXPY");
		suffixes.put("EXPY-R", "EXPRESSWAY");
		suffixes.put("EXPRESWAY", "EXPY");
		suffixes.put("EXPRSWY", "EXPY");
		suffixes.put("EXPRWY", "EXPY");
		suffixes.put("EXPW", "EXPY");
		suffixes.put("EXPWY", "EXPY");
		suffixes.put("EXPY", "EXPY");
		suffixes.put("EXWAY", "EXPY");
		suffixes.put("EXWY", "EXPY");
		suffixes.put("EXT", "EXT");
		suffixes.put("EXTEN", "EXT");
		suffixes.put("EXTENSION", "EXT");
		suffixes.put("EXT-R", "EXTENSION");
		suffixes.put("EXTENSN", "EXT");
		suffixes.put("EXTN", "EXT");
		suffixes.put("EXTNSN", "EXT");
		suffixes.put("EXTENS", "EXTS");
		suffixes.put("EXTENSIONS", "EXTS");
		suffixes.put("EXTS-R", "EXTENSIONS");
		suffixes.put("EXTENSNS", "EXTS");
		suffixes.put("EXTNS", "EXTS");
		suffixes.put("EXTNSNS", "EXTS");
		suffixes.put("EXTS", "EXTS");
		suffixes.put("FAL", "FALL");
		suffixes.put("FALL", "FALL");
		suffixes.put("FALL-R", "FALL");
		suffixes.put("FIELD", "FLD");
		suffixes.put("FLD-R", "FIELD");
		suffixes.put("FLD", "FLD");
		suffixes.put("FIELDS", "FLDS");
		suffixes.put("FLDS-R", "FIELDS");
		suffixes.put("FLDS", "FLDS");
		suffixes.put("FALLS", "FLS");
		suffixes.put("FLS-R", "FALLS");
		suffixes.put("FALS", "FLS");
		suffixes.put("FLS", "FLS");
		suffixes.put("FLAT", "FLT");
		suffixes.put("FLT-R", "FLAT");
		suffixes.put("FLT", "FLT");
		suffixes.put("FLATS", "FLTS");
		suffixes.put("FLTS-R", "FLATS");
		suffixes.put("FLTS", "FLTS");
		suffixes.put("FORD", "FRD");
		suffixes.put("FRD-R", "FORD");
		suffixes.put("FRD", "FRD");
		suffixes.put("FORDS", "FRDS");
		suffixes.put("FRDS-R", "FORDS");
		suffixes.put("FRDS", "FRDS");
		suffixes.put("FORG", "FRG");
		suffixes.put("FORGE", "FRG");
		suffixes.put("FRG-R", "FORGE");
		suffixes.put("FRG", "FRG");
		suffixes.put("FORGES", "FRGS");
		suffixes.put("FRGS-R", "FORGES");
		suffixes.put("FRGS", "FRGS");
		suffixes.put("FORK", "FRK");
		suffixes.put("FRK-R", "FORK");
		suffixes.put("FRK", "FRK");
		suffixes.put("FORKS", "FRKS");
		suffixes.put("FRKS-R", "FORKS");
		suffixes.put("FRKS", "FRKS");
		suffixes.put("FOREST", "FRST");
		suffixes.put("FRST-R", "FOREST");
		suffixes.put("FORESTS", "FRST");
		suffixes.put("FORREST", "FRST");
		suffixes.put("FORRESTS", "FRST");
		suffixes.put("FORRST", "FRST");
		suffixes.put("FORRSTS", "FRST");
		suffixes.put("FORST", "FRST");
		suffixes.put("FORSTS", "FRST");
		suffixes.put("FRRESTS", "FRST");
		suffixes.put("FRRST", "FRST");
		suffixes.put("FRRSTS", "FRST");
		suffixes.put("FRST", "FRST");
		suffixes.put("FERRY", "FRY");
		suffixes.put("FRY-R", "FERRY");
		suffixes.put("FERY", "FRY");
		suffixes.put("FRRY", "FRY");
		suffixes.put("FRY", "FRY");
		suffixes.put("FORT", "FT");
		suffixes.put("FT-R", "FORT");
		suffixes.put("FRT", "FT");
		suffixes.put("FT", "FT");
		suffixes.put("FREEWAY", "FWY");
		suffixes.put("FWY-R", "FREEWAY");
		suffixes.put("FREEWY", "FWY");
		suffixes.put("FREWAY", "FWY");
		suffixes.put("FREWY", "FWY");
		suffixes.put("FRWAY", "FWY");
		suffixes.put("FRWY", "FWY");
		suffixes.put("FWY", "FWY");
		suffixes.put("GARDEN", "GDN");
		suffixes.put("GDN-R", "GARDEN");
		suffixes.put("GARDN", "GDN");
		suffixes.put("GDN", "GDN");
		suffixes.put("GRDEN", "GDN");
		suffixes.put("GRDN", "GDN");
		suffixes.put("GARDENS", "GDNS");
		suffixes.put("GDNS-R", "GARDENS");
		suffixes.put("GARDNS", "GDNS");
		suffixes.put("GDNS", "GDNS");
		suffixes.put("GRDENS", "GDNS");
		suffixes.put("GRDNS", "GDNS");
		suffixes.put("GLEN", "GLN");
		suffixes.put("GLN-R", "GLEN");
		suffixes.put("GLENN", "GLN");
		suffixes.put("GLN", "GLN");
		suffixes.put("GLENNS", "GLNS");
		suffixes.put("GLENS", "GLNS");
		suffixes.put("GLNS-R", "GLENS");
		suffixes.put("GLNS", "GLNS");
		suffixes.put("GREEN", "GRN");
		suffixes.put("GRN-R", "GREEN");
		suffixes.put("GREN", "GRN");
		suffixes.put("GRN", "GRN");
		suffixes.put("GREENS", "GRNS");
		suffixes.put("GRNS-R", "GREENS");
		suffixes.put("GRENS", "GRNS");
		suffixes.put("GRNS", "GRNS");
		suffixes.put("GROV", "GRV");
		suffixes.put("GROVE", "GRV");
		suffixes.put("GRV-R", "GROVE");
		suffixes.put("GRV", "GRV");
		suffixes.put("GROVES", "GRVS");
		suffixes.put("GRVS-R", "GROVES");
		suffixes.put("GROVS", "GRVS");
		suffixes.put("GRVS", "GRVS");
		suffixes.put("GATEWAY", "GTWY");
		suffixes.put("GTWY-R", "GATEWAY");
		suffixes.put("GATEWY", "GTWY");
		suffixes.put("GATWAY", "GTWY");
		suffixes.put("GTWAY", "GTWY");
		suffixes.put("GTWY", "GTWY");
		suffixes.put("HARB", "HBR");
		suffixes.put("HARBOR", "HBR");
		suffixes.put("HBR-R", "HARBOR");
		suffixes.put("HARBR", "HBR");
		suffixes.put("HBR", "HBR");
		suffixes.put("HRBOR", "HBR");
		suffixes.put("HARBORS", "HBRS");
		suffixes.put("HBRS-R", "HARBORS");
		suffixes.put("HBRS", "HBRS");
		suffixes.put("HILL", "HL");
		suffixes.put("HL-R", "HILL");
		suffixes.put("HL", "HL");
		suffixes.put("HILLS", "HLS");
		suffixes.put("HLS-R", "HILLS");
		suffixes.put("HLS", "HLS");
		suffixes.put("HLLW", "HOLW");
		suffixes.put("HLLWS", "HOLW");
		suffixes.put("HOLLOW", "HOLW");
		suffixes.put("HOLW-R", "HOLLOW");
		suffixes.put("HOLLOWS", "HOLW");
		suffixes.put("HOLOW", "HOLW");
		suffixes.put("HOLOWS", "HOLW");
		suffixes.put("HOLW", "HOLW");
		suffixes.put("HOLWS", "HOLW");
		suffixes.put("HEIGHT", "HTS");
		suffixes.put("HEIGHTS", "HTS");
		suffixes.put("HTS-R", "HEIGHTS");
		suffixes.put("HGTS", "HTS");
		suffixes.put("HT", "HTS");
		suffixes.put("HTS", "HTS");
		suffixes.put("HAVEN", "HVN");
		suffixes.put("HVN-R", "HAVEN");
		suffixes.put("HAVN", "HVN");
		suffixes.put("HVN", "HVN");
		suffixes.put("HIGHWAY", "HWY");
		suffixes.put("HWY-R", "HIGHWAY");
		suffixes.put("HIGHWY", "HWY");
		suffixes.put("HIWAY", "HWY");
		suffixes.put("HIWY", "HWY");
		suffixes.put("HWAY", "HWY");
		suffixes.put("HWY", "HWY");
		suffixes.put("HY", "HWY");
		suffixes.put("HYGHWAY", "HWY");
		suffixes.put("HYWAY", "HWY");
		suffixes.put("HYWY", "HWY");
		suffixes.put("INLET", "INLT");
		suffixes.put("INLT-R", "INLET");
		suffixes.put("INLT", "INLT");
		suffixes.put("ILAND", "IS");
		suffixes.put("ILND", "IS");
		suffixes.put("IS", "IS");
		suffixes.put("ISLAND", "IS");
		suffixes.put("IS-R", "ISLAND");
		suffixes.put("ISLND", "IS");
		suffixes.put("ILE", "ISLE");
		suffixes.put("ISLE", "ISLE");
		suffixes.put("ISLE-R", "ISLE");
		suffixes.put("ISLES", "ISLE");
		suffixes.put("ILANDS", "ISS");
		suffixes.put("ILNDS", "ISS");
		suffixes.put("ISLANDS", "ISS");
		suffixes.put("ISS-R", "ISLANDS");
		suffixes.put("ISLDS", "ISS");
		suffixes.put("ISLNDS", "ISS");
		suffixes.put("ISS", "ISS");
		suffixes.put("JCT", "JCT");
		suffixes.put("JCTION", "JCT");
		suffixes.put("JCTN", "JCT");
		suffixes.put("JUNCTION", "JCT");
		suffixes.put("JCT-R", "JUNCTION");
		suffixes.put("JUNCTN", "JCT");
		suffixes.put("JUNCTON", "JCT");
		suffixes.put("JCTIONS", "JCTS");
		suffixes.put("JCTNS", "JCTS");
		suffixes.put("JCTS", "JCTS");
		suffixes.put("JUNCTIONS", "JCTS");
		suffixes.put("JCTS-R", "JUNCTIONS");
		suffixes.put("JUNCTONS", "JCTS");
		suffixes.put("JUNGTNS", "JCTS");
		suffixes.put("KNL", "KNL");
		suffixes.put("KNOL", "KNL");
		suffixes.put("KNOLL", "KNL");
		suffixes.put("KNL-R", "KNOLL");
		suffixes.put("KNLS", "KNLS");
		suffixes.put("KNOLLS", "KNLS");
		suffixes.put("KNLS-R", "KNOLLS");
		suffixes.put("KNOLS", "KNLS");
		suffixes.put("KEY", "KY");
		suffixes.put("KY-R", "KEY");
		suffixes.put("KY", "KY");
		suffixes.put("KEYS", "KYS");
		suffixes.put("KYS-R", "KEYS");
		suffixes.put("KYS", "KYS");
		suffixes.put("LAND", "LAND");
		suffixes.put("LAND-R", "LAND");
		suffixes.put("LCK", "LCK");
		suffixes.put("LOCK", "LCK");
		suffixes.put("LCK-R", "LOCK");
		suffixes.put("LCKS", "LCKS");
		suffixes.put("LOCKS", "LCKS");
		suffixes.put("LCKS-R", "LOCKS");
		suffixes.put("LDG", "LDG");
		suffixes.put("LDGE", "LDG");
		suffixes.put("LODG", "LDG");
		suffixes.put("LODGE", "LDG");
		suffixes.put("LDG-R", "LODGE");
		suffixes.put("LF", "LF");
		suffixes.put("LOAF", "LF");
		suffixes.put("LF-R", "LOAF");
		suffixes.put("LGT", "LGT");
		suffixes.put("LIGHT", "LGT");
		suffixes.put("LGT-R", "LIGHT");
		suffixes.put("LT", "LGT");
		suffixes.put("LGTS", "LGTS");
		suffixes.put("LIGHTS", "LGTS");
		suffixes.put("LGTS-R", "LIGHTS");
		suffixes.put("LTS", "LGTS");
		suffixes.put("LAKE", "LK");
		suffixes.put("LK-R", "LAKE");
		suffixes.put("LK", "LK");
		suffixes.put("LAKES", "LKS");
		suffixes.put("LKS-R", "LAKES");
		suffixes.put("LKS", "LKS");
		suffixes.put("LA", "LN");
		suffixes.put("LANE", "LN");
		suffixes.put("LN-R", "LANE");
		suffixes.put("LANES", "LN");
		suffixes.put("LN", "LN");
		suffixes.put("LNS", "LN");
		suffixes.put("LANDG", "LNDG");
		suffixes.put("LANDING", "LNDG");
		suffixes.put("LNDG-R", "LANDING");
		suffixes.put("LANDNG", "LNDG");
		suffixes.put("LNDG", "LNDG");
		suffixes.put("LNDNG", "LNDG");
		suffixes.put("LOOP", "LOOP");
		suffixes.put("LOOP-R", "LOOP");
		suffixes.put("LOOPS", "LOOP");
		suffixes.put("MALL", "MALL");
		suffixes.put("MALL-R", "MALL");
		suffixes.put("MDW", "MDW");
		suffixes.put("MEADOW", "MDW");
		suffixes.put("MDW-R", "MEADOW");
		suffixes.put("MDWS", "MDWS");
		suffixes.put("MEADOWS", "MDWS");
		suffixes.put("MDWS-R", "MEADOWS");
		suffixes.put("MEDOWS", "MDWS");
		suffixes.put("MEDWS", "MDWS");
		suffixes.put("MEWS", "MEWS");
		suffixes.put("MEWS-R", "MEWS");
		suffixes.put("MIL", "ML");
		suffixes.put("MILL", "ML");
		suffixes.put("ML-R", "MILL");
		suffixes.put("ML", "ML");
		suffixes.put("MILLS", "MLS");
		suffixes.put("MLS-R", "MILLS");
		suffixes.put("MILS", "MLS");
		suffixes.put("MLS", "MLS");
		suffixes.put("MANOR", "MNR");
		suffixes.put("MNR-R", "MANOR");
		suffixes.put("MANR", "MNR");
		suffixes.put("MNR", "MNR");
		suffixes.put("MANORS", "MNRS");
		suffixes.put("MNRS-R", "MANORS");
		suffixes.put("MANRS", "MNRS");
		suffixes.put("MNRS", "MNRS");
		suffixes.put("MISN", "MSN");
		suffixes.put("MISSION", "MSN");
		suffixes.put("MSN-R", "MISSION");
		suffixes.put("MISSN", "MSN");
		suffixes.put("MSN", "MSN");
		suffixes.put("MSSN", "MSN");
		suffixes.put("MNT", "MT");
		suffixes.put("MOUNT", "MT");
		suffixes.put("MT-R", "MOUNT");
		suffixes.put("MT", "MT");
		suffixes.put("MNTAIN", "MTN");
		suffixes.put("MNTN", "MTN");
		suffixes.put("MOUNTAIN", "MTN");
		suffixes.put("MTN-R", "MOUNTAIN");
		suffixes.put("MOUNTIN", "MTN");
		suffixes.put("MTIN", "MTN");
		suffixes.put("MTN", "MTN");
		suffixes.put("MNTNS", "MTNS");
		suffixes.put("MOUNTAINS", "MTNS");
		suffixes.put("MTNS-R", "MOUNTAINS");
		suffixes.put("MTNS", "MTNS");
		suffixes.put("MOTORWAY", "MTWY");
		suffixes.put("MTWY-R", "MOTORWAY");
		suffixes.put("MOTORWY", "MTWY");
		suffixes.put("MOTRWY", "MTWY");
		suffixes.put("MOTWY", "MTWY");
		suffixes.put("MTRWY", "MTWY");
		suffixes.put("MTWY", "MTWY");
		suffixes.put("NCK", "NCK");
		suffixes.put("NECK", "NCK");
		suffixes.put("NCK-R", "NECK");
		suffixes.put("NEK", "NCK");
		suffixes.put("OPAS", "OPAS");
		suffixes.put("OVERPAS", "OPAS");
		suffixes.put("OVERPASS", "OPAS");
		suffixes.put("OPAS-R", "OVERPASS");
		suffixes.put("OVERPS", "OPAS");
		suffixes.put("OVRPS", "OPAS");
		suffixes.put("ORCH", "ORCH");
		suffixes.put("ORCHARD", "ORCH");
		suffixes.put("ORCH-R", "ORCHARD");
		suffixes.put("ORCHRD", "ORCH");
		suffixes.put("OVAL", "OVAL");
		suffixes.put("OVAL-R", "OVAL");
		suffixes.put("OVL", "OVAL");
		suffixes.put("PARK", "PARK");
		suffixes.put("PARK-R", "PARK");
		suffixes.put("PARKS", "PARK");
		suffixes.put("PK", "PARK");
		suffixes.put("PRK", "PARK");
		suffixes.put("PAS", "PASS");
		suffixes.put("PASS", "PASS");
		suffixes.put("PASS-R", "PASS");
		suffixes.put("PATH", "PATH");
		suffixes.put("PATH-R", "PATH");
		suffixes.put("PATHS", "PATH");
		suffixes.put("PIKE", "PIKE");
		suffixes.put("PIKE-R", "PIKE");
		suffixes.put("PIKES", "PIKE");
		suffixes.put("PKE", "PIKE");
		suffixes.put("PARKWAY", "PKWY");
		suffixes.put("PKWY-R", "PARKWAY");
		suffixes.put("PARKWAYS", "PKWY");
		suffixes.put("PARKWY", "PKWY");
		suffixes.put("PKWAY", "PKWY");
		suffixes.put("PKWY", "PKWY");
		suffixes.put("PKWYS", "PKWY");
		suffixes.put("PKY", "PKWY");
		suffixes.put("PW", "PKWY");
		suffixes.put("PL", "PL");
		suffixes.put("PLAC", "PL");
		suffixes.put("PLACE", "PL");
		suffixes.put("PL-R", "PLACE");
		suffixes.put("PLASE", "PL");
		suffixes.put("PLAIN", "PLN");
		suffixes.put("PLN-R", "PLAIN");
		suffixes.put("PLN", "PLN");
		suffixes.put("PLAINES", "PLNS");
		suffixes.put("PLAINS", "PLNS");
		suffixes.put("PLNS-R", "PLAINS");
		suffixes.put("PLNS", "PLNS");
		suffixes.put("PLAZ", "PLZ");
		suffixes.put("PLAZA", "PLZ");
		suffixes.put("PLZ-R", "PLAZA");
		suffixes.put("PLZ", "PLZ");
		suffixes.put("PLZA", "PLZ");
		suffixes.put("PZ", "PLZ");
		suffixes.put("PINE", "PNE");
		suffixes.put("PNE-R", "PINE");
		suffixes.put("PNE", "PNE");
		suffixes.put("PINES", "PNES");
		suffixes.put("PNES-R", "PINES");
		suffixes.put("PNES", "PNES");
		suffixes.put("PR", "PR");
		suffixes.put("PRAIR", "PR");
		suffixes.put("PRAIRIE", "PR");
		suffixes.put("PR-R", "PRAIRIE");
		suffixes.put("PRARE", "PR");
		suffixes.put("PRARIE", "PR");
		suffixes.put("PRR", "PR");
		suffixes.put("PRRE", "PR");
		suffixes.put("PORT", "PRT");
		suffixes.put("PRT-R", "PORT");
		suffixes.put("PRT", "PRT");
		suffixes.put("PORTS", "PRTS");
		suffixes.put("PRTS-R", "PORTS");
		suffixes.put("PRTS", "PRTS");
		suffixes.put("PASG", "PSGE");
		suffixes.put("PASSAGE", "PSGE");
		suffixes.put("PSGE-R", "PASSAGE");
		suffixes.put("PASSG", "PSGE");
		suffixes.put("PSGE", "PSGE");
		suffixes.put("PNT", "PT");
		suffixes.put("POINT", "PT");
		suffixes.put("PT-R", "POINT");
		suffixes.put("PT", "PT");
		suffixes.put("PNTS", "PTS");
		suffixes.put("POINTS", "PTS");
		suffixes.put("PTS-R", "POINTS");
		suffixes.put("PTS", "PTS");
		suffixes.put("RAD", "RADL");
		suffixes.put("RADIAL", "RADL");
		suffixes.put("RADL-R", "RADIAL");
		suffixes.put("RADIEL", "RADL");
		suffixes.put("RADL", "RADL");
		suffixes.put("RAMP", "RAMP");
		suffixes.put("RAMP-R", "RAMP");
		suffixes.put("RD", "RD");
		suffixes.put("ROAD", "RD");
		suffixes.put("RD-R", "ROAD");
		suffixes.put("RDG", "RDG");
		suffixes.put("RDGE", "RDG");
		suffixes.put("RIDGE", "RDG");
		suffixes.put("RDG-R", "RIDGE");
		suffixes.put("RDGS", "RDGS");
		suffixes.put("RIDGES", "RDGS");
		suffixes.put("RDGS-R", "RIDGES");
		suffixes.put("RDS", "RDS");
		suffixes.put("ROADS", "RDS");
		suffixes.put("RDS-R", "ROADS");
		suffixes.put("RIV", "RIV");
		suffixes.put("RIVER", "RIV");
		suffixes.put("RIV-R", "RIVER");
		suffixes.put("RIVR", "RIV");
		suffixes.put("RVR", "RIV");
		suffixes.put("RANCH", "RNCH");
		suffixes.put("RNCH-R", "RANCH");
		suffixes.put("RANCHES", "RNCH");
		suffixes.put("RNCH", "RNCH");
		suffixes.put("RNCHS", "RNCH");
		suffixes.put("RAOD", "ROAD");
		suffixes.put("ROW", "ROW");
		suffixes.put("ROW-R", "ROW");
		suffixes.put("RAPID", "RPD");
		suffixes.put("RPD-R", "RAPID");
		suffixes.put("RPD", "RPD");
		suffixes.put("RAPIDS", "RPDS");
		suffixes.put("RPDS-R", "RAPIDS");
		suffixes.put("RPDS", "RPDS");
		suffixes.put("REST", "RST");
		suffixes.put("RST-R", "REST");
		suffixes.put("RST", "RST");
		suffixes.put("ROUTE", "RTE");
		suffixes.put("RTE-R", "ROUTE");
		suffixes.put("RT", "RTE");
		suffixes.put("RTE", "RTE");
		suffixes.put("RUE", "RUE");
		suffixes.put("RUE-R", "RUE");
		suffixes.put("RUN", "RUN");
		suffixes.put("RUN-R", "RUN");
		suffixes.put("SHL", "SHL");
		suffixes.put("SHOAL", "SHL");
		suffixes.put("SHL-R", "SHOAL");
		suffixes.put("SHOL", "SHL");
		suffixes.put("SHLS", "SHLS");
		suffixes.put("SHOALS", "SHLS");
		suffixes.put("SHLS-R", "SHOALS");
		suffixes.put("SHOLS", "SHLS");
		suffixes.put("SHOAR", "SHR");
		suffixes.put("SHORE", "SHR");
		suffixes.put("SHR-R", "SHORE");
		suffixes.put("SHR", "SHR");
		suffixes.put("SHOARS", "SHRS");
		suffixes.put("SHORES", "SHRS");
		suffixes.put("SHRS-R", "SHORES");
		suffixes.put("SHRS", "SHRS");
		suffixes.put("SKWY", "SKWY");
		suffixes.put("SKYWAY", "SKWY");
		suffixes.put("SKWY-R", "SKYWAY");
		suffixes.put("SKYWY", "SKWY");
		suffixes.put("SMT", "SMT");
		suffixes.put("SUMIT", "SMT");
		suffixes.put("SUMITT", "SMT");
		suffixes.put("SUMMIT", "SMT");
		suffixes.put("SMT-R", "SUMMIT");
		suffixes.put("SUMT", "SMT");
		suffixes.put("SPG", "SPG");
		suffixes.put("SPNG", "SPG");
		suffixes.put("SPRING", "SPG");
		suffixes.put("SPG-R", "SPRING");
		suffixes.put("SPRNG", "SPG");
		suffixes.put("SPGS", "SPGS");
		suffixes.put("SPNGS", "SPGS");
		suffixes.put("SPRINGS", "SPGS");
		suffixes.put("SPGS-R", "SPRINGS");
		suffixes.put("SPRNGS", "SPGS");
		suffixes.put("SPR", "SPUR");
		suffixes.put("SPRS", "SPUR");
		suffixes.put("SPUR", "SPUR");
		suffixes.put("SPUR-R", "SPUR");
		suffixes.put("SPURS", "SPUR");
		suffixes.put("SQ", "SQ");
		suffixes.put("SQAR", "SQ");
		suffixes.put("SQR", "SQ");
		suffixes.put("SQRE", "SQ");
		suffixes.put("SQU", "SQ");
		suffixes.put("SQUARE", "SQ");
		suffixes.put("SQ-R", "SQUARE");
		suffixes.put("SQARS", "SQS");
		suffixes.put("SQRS", "SQS");
		suffixes.put("SQS", "SQS");
		suffixes.put("SQUARES", "SQS");
		suffixes.put("SQS-R", "SQUARES");
		suffixes.put("ST", "ST");
		suffixes.put("STR", "ST");
		suffixes.put("STREET", "ST");
		suffixes.put("ST-R", "STREET");
		suffixes.put("STRT", "ST");
		suffixes.put("STA", "STA");
		suffixes.put("STATION", "STA");
		suffixes.put("STA-R", "STATION");
		suffixes.put("STATN", "STA");
		suffixes.put("STN", "STA");
		suffixes.put("STRA", "STRA");
		suffixes.put("STRAV", "STRA");
		suffixes.put("STRAVE", "STRA");
		suffixes.put("STRAVEN", "STRA");
		suffixes.put("STRAVENUE", "STRA");
		suffixes.put("STRA-R", "STRAVENUE");
		suffixes.put("STRAVN", "STRA");
		suffixes.put("STRVN", "STRA");
		suffixes.put("STRVNUE", "STRA");
		suffixes.put("STREAM", "STRM");
		suffixes.put("STRM-R", "STREAM");
		suffixes.put("STREME", "STRM");
		suffixes.put("STRM", "STRM");
		suffixes.put("STREETS", "STS");
		suffixes.put("STS-R", "STREETS");
		suffixes.put("STS", "STS");
		suffixes.put("TER", "TER");
		suffixes.put("TERACE", "TER");
		suffixes.put("TERASE", "TER");
		suffixes.put("TERR", "TER");
		suffixes.put("TERRACE", "TER");
		suffixes.put("TER-R", "TERRACE");
		suffixes.put("TERRASE", "TER");
		suffixes.put("TERRC", "TER");
		suffixes.put("TERRICE", "TER");
		suffixes.put("TPK", "TPKE");
		suffixes.put("TPKE", "TPKE");
		suffixes.put("TRNPK", "TPKE");
		suffixes.put("TRPK", "TPKE");
		suffixes.put("TURNPIKE", "TPKE");
		suffixes.put("TPKE-R", "TURNPIKE");
		suffixes.put("TURNPK", "TPKE");
		suffixes.put("TRACK", "TRAK");
		suffixes.put("TRAK-R", "TRACK");
		suffixes.put("TRACKS", "TRAK");
		suffixes.put("TRAK", "TRAK");
		suffixes.put("TRK", "TRAK");
		suffixes.put("TRKS", "TRAK");
		suffixes.put("TRACE", "TRCE");
		suffixes.put("TRC", "TRCE");
		suffixes.put("TRCE-R", "TRACE");
		suffixes.put("TRACES", "TRCE");
		suffixes.put("TRCE", "TRCE");
		suffixes.put("TC", "TRCE");
		suffixes.put("TRAFFICWAY", "TRFY");
		suffixes.put("TRFY-R", "TRAFFICWAY");
		suffixes.put("TRAFFICWY", "TRFY");
		suffixes.put("TRAFWAY", "TRFY");
		suffixes.put("TRFCWY", "TRFY");
		suffixes.put("TRFFCWY", "TRFY");
		suffixes.put("TRFFWY", "TRFY");
		suffixes.put("TRFWY", "TRFY");
		suffixes.put("TRFY", "TRFY");
		suffixes.put("TR", "TRL");
		suffixes.put("TRAIL", "TRL");
		suffixes.put("TRL-R", "TRAIL");
		suffixes.put("TRAILS", "TRL");
		suffixes.put("TRL", "TRL");
		suffixes.put("TRLS", "TRL");
		suffixes.put("TL", "TRL");
		suffixes.put("THROUGHWAY", "TRWY");
		suffixes.put("TRWY-R", "THROUGHWAY");
		suffixes.put("THROUGHWY", "TRWY");
		suffixes.put("THRUWAY", "TRWY");
		suffixes.put("THRUWY", "TRWY");
		suffixes.put("THRWAY", "TRWY");
		suffixes.put("THRWY", "TRWY");
		suffixes.put("THWY", "TRWY");
		suffixes.put("TRWY", "TRWY");
		suffixes.put("TUNEL", "TUNL");
		suffixes.put("TUNL", "TUNL");
		suffixes.put("TUNLS", "TUNL");
		suffixes.put("TUNNEL", "TUNL");
		suffixes.put("TUNL-R", "TUNNEL");
		suffixes.put("TUNNELS", "TUNL");
		suffixes.put("TUNNL", "TUNL");
		suffixes.put("UN", "UN");
		suffixes.put("UNION", "UN");
		suffixes.put("UN-R", "UNION");
		suffixes.put("UNIONS", "UNS");
		suffixes.put("UNS-R", "UNIONS");
		suffixes.put("UNS", "UNS");
		suffixes.put("UDRPS", "UPAS");
		suffixes.put("UNDERPAS", "UPAS");
		suffixes.put("UNDERPASS", "UPAS");
		suffixes.put("UPAS-R", "UNDERPASS");
		suffixes.put("UNDERPS", "UPAS");
		suffixes.put("UNDRPAS", "UPAS");
		suffixes.put("UNDRPS", "UPAS");
		suffixes.put("UPAS", "UPAS");
		suffixes.put("VDCT", "VIA");
		suffixes.put("VIA", "VIA");
		suffixes.put("VIADCT", "VIA");
		suffixes.put("VIADUCT", "VIA");
		suffixes.put("VIA-R", "VIADUCT");
		suffixes.put("VIS", "VIS");
		suffixes.put("VIST", "VIS");
		suffixes.put("VISTA", "VIS");
		suffixes.put("VIS-R", "VISTA");
		suffixes.put("VST", "VIS");
		suffixes.put("VSTA", "VIS");
		suffixes.put("VILLE", "VL");
		suffixes.put("VL-R", "VILLE");
		suffixes.put("VL", "VL");
		suffixes.put("VILG", "VLG");
		suffixes.put("VILL", "VLG");
		suffixes.put("VILLAG", "VLG");
		suffixes.put("VILLAGE", "VLG");
		suffixes.put("VLG-R", "VILLAGE");
		suffixes.put("VILLG", "VLG");
		suffixes.put("VILLIAGE", "VLG");
		suffixes.put("VLG", "VLG");
		suffixes.put("VILGS", "VLGS");
		suffixes.put("VILLAGES", "VLGS");
		suffixes.put("VLGS-R", "VILLAGES");
		suffixes.put("VLGS", "VLGS");
		suffixes.put("VALLEY", "VLY");
		suffixes.put("VLY-R", "VALLEY");
		suffixes.put("VALLY", "VLY");
		suffixes.put("VALY", "VLY");
		suffixes.put("VLLY", "VLY");
		suffixes.put("VLY", "VLY");
		suffixes.put("VALLEYS", "VLYS");
		suffixes.put("VLYS-R", "VALLEYS");
		suffixes.put("VLYS", "VLYS");
		suffixes.put("VIEW", "VW");
		suffixes.put("VW-R", "VIEW");
		suffixes.put("VW", "VW");
		suffixes.put("VIEWS", "VWS");
		suffixes.put("VWS-R", "VIEWS");
		suffixes.put("VWS", "VWS");
		suffixes.put("WALK", "WALK");
		suffixes.put("WALK-R", "WALK");
		suffixes.put("WALKS", "WALK");
		suffixes.put("WLK", "WALK");
		suffixes.put("WALL", "WALL");
		suffixes.put("WALL-R", "WALL");
		suffixes.put("WAY", "WAY");
		suffixes.put("WAY-R", "WAY");
		suffixes.put("WY", "WAY");
		suffixes.put("WA", "WAY");
		suffixes.put("WAYS", "WAYS");
		suffixes.put("WAYS-R", "WAYS");
		suffixes.put("WEL", "WL");
		suffixes.put("WELL", "WL");
		suffixes.put("WL-R", "WELL");
		suffixes.put("WL", "WL");
		suffixes.put("WELLS", "WLS");
		suffixes.put("WLS-R", "WELLS");
		suffixes.put("WELS", "WLS");
		suffixes.put("WLS", "WLS");
		suffixes.put("CROSING", "XING");
		suffixes.put("CROSNG", "XING");
		suffixes.put("CROSSING", "XING");
		suffixes.put("XING-R", "CROSSING");
		suffixes.put("CRSING", "XING");
		suffixes.put("CRSNG", "XING");
		suffixes.put("CRSSING", "XING");
		suffixes.put("CRSSNG", "XING");
		suffixes.put("XING", "XING");
		suffixes.put("CROSRD", "XRD");
		suffixes.put("CROSSRD", "XRD");
		suffixes.put("CROSSROAD", "XRD");
		suffixes.put("XRD-R", "CROSSROAD");
		suffixes.put("CRSRD", "XRD");
		suffixes.put("XRD", "XRD");
		suffixes.put("XROAD", "XRD");
		
		saints = new HashSet<String>();
		saints.add("ABBESS");
		saints.add("ABBOT");
		saints.add("ABBOTT");
		saints.add("ABDON");
		saints.add("ABRAHAM");
		saints.add("ADRIAN");
		saints.add("AELRED");
		saints.add("AGAPETUS");
		saints.add("AGATHA");
		saints.add("AGNES");
		saints.add("ALBERT");
		saints.add("ALBINUS");
		saints.add("ALCANTARA");
		saints.add("ALEXANDRIA");
		saints.add("ALEXIUS");
		saints.add("ALFRED");
		saints.add("ALMSGIVER");
		saints.add("ALOYSIUS");
		saints.add("ALPHONSUS");
		saints.add("AMBROSE");
		saints.add("AMERICAN");
		saints.add("ANACLETUS");
		saints.add("ANDERLECHT");
		saints.add("ANDREW");
		saints.add("ANGELA");
		saints.add("ANGELS");
		saints.add("ANIAN");
		saints.add("ANICETUS");
		saints.add("ANNA");
		saints.add("ANNE");
		saints.add("ANSELM");
		saints.add("ANTHONY");
		saints.add("ANTIDE");
		saints.add("ANTIOCH");
		saints.add("ANTONINUS");
		saints.add("APOLLINAIRE");
		saints.add("APOLLINARIS");
		saints.add("APOLLONIA");
		saints.add("APOLLONIUS");
		saints.add("APOSTLE");
		saints.add("APPARITION");
		saints.add("AQUINAS");
		saints.add("AQUITAINE");
		saints.add("ARBRISSEL");
		saints.add("ARC");
		saints.add("ARCHANGEL");
		saints.add("ARMAGH");
		saints.add("ASSISI");
		saints.add("ASSUNTA");
		saints.add("ATHANASIUS");
		saints.add("ATTALUS");
		saints.add("AUGUSTINE");
		saints.add("AUXERRE");
		saints.add("AVELLINO");
		saints.add("AVILA");
		saints.add("AVITUS");
		saints.add("BADEMUS");
		saints.add("BAPTIST");
		saints.add("BARACHISIUS");
		saints.add("BARBARA");
		saints.add("BARBATUS");
		saints.add("BARNABAS");
		saints.add("BARTHOLOMEW");
		saints.add("BASIL");
		saints.add("BASILICAS");
		saints.add("BASILISSA");
		saints.add("BAYLON");
		saints.add("BECKET");
		saints.add("BEDE");
		saints.add("BEFORE");
		saints.add("BEHEADING");
		saints.add("BELLARMINE");
		saints.add("BENEDICT");
		saints.add("BENEZET");
		saints.add("BENIZI");
		saints.add("BENJAMIN");
		char I = 206;
		saints.add("BENO" + I + "T");
		saints.add("BERCHMANS");
		saints.add("BERNADETTE");
		saints.add("BERNARD");
		saints.add("BERNARDINE");
		saints.add("BERRUYER");
		saints.add("BERTHA");
		saints.add("BERTILLA");
		saints.add("BERTRAND");
		char C = 199;
		saints.add("BESAN" + C + "ON");
		saints.add("BIBIANA");
		saints.add("BISHOP");
		saints.add("BLAISE");
		saints.add("BLANDINA");
		saints.add("BLESSED");
		saints.add("BOBOLA");
		saints.add("BONAVENTURE");
		saints.add("BONIFACE");
		saints.add("BORGIA");
		saints.add("BORROMEO");
		saints.add("BOSCO");
		saints.add("BOURGEOIS");
		saints.add("BRENDAN");
		saints.add("BRIDGET");
		saints.add("BRIDGID");
		saints.add("BRITTO");
		saints.add("BRUNO");
		saints.add("CABRINI");
		saints.add("CAJETAN");
		saints.add("CALASANCTIUS");
		saints.add("CALLISTUS");
		saints.add("CALVAT");
		saints.add("CAMERINO");
		saints.add("CAMILLUS");
		saints.add("CANISIUS");
		saints.add("CANTALICE");
		saints.add("CANTERBURY");
		saints.add("CANTIUS");
		saints.add("CANUTUS");
		saints.add("CAPISTRAN");
		saints.add("CARACCIOLO");
		saints.add("CARMELITE");
		saints.add("CARTHAGE");
		saints.add("CASCIA");
		saints.add("CASIMIR");
		saints.add("CATHERINE");
		saints.add("CECILIA");
		saints.add("CELESTINE");
		saints.add("CELSUS");
		saints.add("CENTURION");
		saints.add("CHAINS");
		saints.add("CHAIR");
		saints.add("CHANEL");
		saints.add("CHANTAL");
		saints.add("CHARBEL");
		saints.add("CHARLES");
		saints.add("CHICHESTER");
		saints.add("CHILDREN");
		saints.add("CHRISTINA");
		saints.add("CHRYSANTHUS");
		saints.add("CHRYSOSTOM");
		saints.add("CLAIRE");
		saints.add("CLARE");
		saints.add("CLARET");
		saints.add("CLAUDE");
		saints.add("CLAVER");
		saints.add("CLEMENT");
		saints.add("CLET");
		saints.add("CLETUS");
		saints.add("CLIMACUS");
		saints.add("CLODOALD");
		saints.add("CLOTILDA");
		saints.add("CLOUD");
		saints.add("CLUNY");
		saints.add("COLETTE");
		saints.add("COLUMBA");
		saints.add("COLUMKILLE");
		saints.add("COMMEMORATION");
		saints.add("COMPANIONS");
		saints.add("CONRAD");
		saints.add("CONVERSION");
		saints.add("CORNILLON");
		saints.add("CORSINI");
		saints.add("COSMAS");
		saints.add("COTTOLENGO");
		saints.add("COUSIN");
		saints.add("CRACOW");
		saints.add("CRESCENTIA");
		saints.add("CRISPIN");
		saints.add("CRISPINIAN");
		saints.add("CROSS");
		saints.add("CUNEGUNDES");
		saints.add("CUPERTINO");
		saints.add("CYPRIAN");
		saints.add("CYRIACUS");
		saints.add("CYRIL");
		saints.add("DAMASCENE");
		saints.add("DAMASUS");
		saints.add("DAMIAN");
		saints.add("DARIA");
		saints.add("DAVID");
		saints.add("DEDICATION");
		saints.add("DELPHINUS");
		saints.add("DENIS");
		saints.add("DIDACE");
		saints.add("DIDACUS");
		saints.add("DIEGO");
		saints.add("DIONYSIA");
		saints.add("DIONYSIUS");
		saints.add("DISCALCED");
		saints.add("DOMINIC");
		saints.add("DONATIAN");
		saints.add("DOROTHY");
		saints.add("EDMUND");
		saints.add("EDWARD");
		saints.add("EGYPT");
		saints.add("EIGHTEEN");
		saints.add("EISLEBEN");
		saints.add("ELEUTHERIUS");
		saints.add("ELIGIUS");
		saints.add("ELIZABETH");
		saints.add("ELOY");
		saints.add("ELPHEGE");
		saints.add("EMILIAN");
		saints.add("EMILIANA");
		saints.add("EMILY");
		saints.add("ENGRATIA");
		saints.add("EPHREM");
		saints.add("EPIPHANIUS");
		saints.add("ETHELDREDA");
		saints.add("EUBULUS");
		saints.add("EUCHERIUS");
		saints.add("EUDES");
		saints.add("EUGENIUS");
		saints.add("EULALIA");
		saints.add("EULOGIUS");
		saints.add("EUPHRASIA");
		saints.add("EUSEBIUS");
		saints.add("EUSTACHIUS");
		saints.add("EVANGELIST");
		saints.add("EVARISTUS");
		saints.add("EYMARD");
		saints.add("FABIEN");
		saints.add("FAGONDEZ");
		saints.add("FALCONIERI");
		saints.add("FATHER");
		saints.add("FAUSTINUS");
		saints.add("FAVRE");
		saints.add("FELICIANUS");
		saints.add("FELICITY");
		saints.add("FELIX");
		saints.add("FERRER");
		saints.add("FIAKER");
		saints.add("FIDELIS");
		saints.add("FINAN");
		saints.add("FINBARR");
		saints.add("FINIAN");
		saints.add("FIRMIN");
		saints.add("FIRST");
		saints.add("FLAVIAN");
		char U = 220;
		saints.add("FL" + U + "E");
		saints.add("FOLIGNO");
		saints.add("FORTY");
		saints.add("FOUNDERS");
		saints.add("FOURIER");
		saints.add("FRANCE");
		saints.add("FRANCES");
		saints.add("FRANCIS");
		saints.add("FREDERICK");
		saints.add("FRUMENTIUS");
		saints.add("FULBERT");
		saints.add("FULGENTIUS");
		saints.add("GABRIEL");
		saints.add("GAL");
		saints.add("GALGANI");
		saints.add("GALL");
		saints.add("GAMBACORTI");
		saints.add("GARICOITS");
		saints.add("GATE");
		saints.add("GATIAN");
		saints.add("GEMMA");
		saints.add("GENEVIEVE");
		saints.add("GENOA");
		saints.add("GEOFFROY");
		saints.add("GEORGE");
		saints.add("GERARD");
		saints.add("GERMAINE");
		saints.add("GERMANUS");
		saints.add("GERTRUDE");
		saints.add("GHYVELDE");
		saints.add("GILES");
		saints.add("GIRAUD");
		saints.add("GOAR");
		saints.add("GOD");
		saints.add("GODFREY");
		saints.add("GONTRAN");
		saints.add("GONZAGA");
		saints.add("GONZALES");
		saints.add("GORETTI");
		saints.add("GREAT");
		saints.add("GREATER");
		saints.add("GREGORY");
		saints.add("GRENOBLE");
		saints.add("GUALBERT");
		saints.add("GUY");
		saints.add("HACKEBORN");
		saints.add("HEDWIG");
		saints.add("HEGESIPPUS");
		saints.add("HELEN");
		saints.add("HELIODORUS");
		saints.add("HENRY");
		saints.add("HERMAN");
		saints.add("HERMENEGILD");
		saints.add("HERMIT");
		saints.add("HILARION");
		saints.add("HILARY");
		saints.add("HOFBAUER");
		saints.add("HOLY");
		saints.add("HONORATUS");
		saints.add("HONORE");
		saints.add("HOSPITALARIAN");
		saints.add("HOSPITIUS");
		saints.add("HUBERT");
		saints.add("HUGH");
		saints.add("HUNGARY");
		saints.add("HYACINTH");
		saints.add("IGNATIUS");
		saints.add("IMELDA");
		saints.add("INCARNATION");
		saints.add("INFANT");
		saints.add("INNOCENTS");
		saints.add("IRENAEUS");
		saints.add("ISCHYRION");
		saints.add("ISIDORE");
		saints.add("JAMES");
		saints.add("JANE");
		saints.add("JANSSOONE");
		saints.add("JANUARIUS");
		saints.add("JAPAN");
		saints.add("JEANNE");
		saints.add("JEROME");
		saints.add("JERUSALEM");
		saints.add("JOACHIM");
		saints.add("JOAN");
		saints.add("JOHN");
		saints.add("JONAS");
		saints.add("JOSAPHAT");
		saints.add("JOSEPH");
		saints.add("JOVITA");
		saints.add("JUDE");
		saints.add("JULIA");
		saints.add("JULIAN");
		saints.add("JULIANA");
		saints.add("JULIUS");
		saints.add("JUSTIN");
		saints.add("JUSTINA");
		saints.add("JUSTINIAN");
		saints.add("KATERI");
		saints.add("KIM");
		saints.add("KING");
		saints.add("KOLBE");
		saints.add("KOSTKA");
		saints.add("LA");
		char E = 201;
		saints.add("LABOUR" + 1);
		saints.add("LABRE");
		saints.add("LADISLAS");
		saints.add("LADY");
		saints.add("LAMBERT");
		saints.add("LAMBERTINI");
		saints.add("LANGRES");
		saints.add("LASALLE");
		saints.add("LATIN");
		saints.add("LAURENCE");
		saints.add("LAVAL");
		saints.add("LAWRENCE");
		saints.add("LEANDER");
		saints.add("LEBER");
		saints.add("LEBLANC");
		saints.add("LEGION");
		saints.add("LELLIS");
		saints.add("LEO");
		saints.add("LEOCADIA");
		saints.add("LEONARD");
		saints.add("LEONIDES");
		saints.add("LESS");
		saints.add("LIBERATUS");
		saints.add("LIGUORI");
		saints.add("LIMA");
		saints.add("LINUS");
		saints.add("LOUIS");
		saints.add("LOUISE");
		saints.add("LOYOLA");
		saints.add("LUCIAN");
		saints.add("LUCY");
		saints.add("LUDGER");
		saints.add("LUKE");
		saints.add("LUPICINUS");
		saints.add("LUXEMBURG");
		saints.add("LYDWINA");
		saints.add("MACARIUS");
		saints.add("MADRID");
		saints.add("MAGDALEN");
		saints.add("MAGDALENE");
		saints.add("MAGLOIRE");
		saints.add("MAILL" + E);
		saints.add("MAJELLA");
		saints.add("MAKHLOUF");
		saints.add("MALACHI");
		saints.add("MAMMERTUS");
		saints.add("MARCELLIANUS");
		saints.add("MARCELLINUS");
		saints.add("MARCELLUS");
		saints.add("MARCHA");
		saints.add("MARCUS");
		saints.add("MARGARET");
		saints.add("MARGUERITE");
		saints.add("MARIA");
		saints.add("MARIE");
		saints.add("MARILLAC");
		saints.add("MARK");
		saints.add("MARSEILLE");
		saints.add("MARTHA");
		saints.add("MARTIN");
		saints.add("MARTINA");
		saints.add("MARTYR");
		saints.add("MARTYRS");
		saints.add("MARY");
		saints.add("MATHA");
		saints.add("MATHILDA");
		saints.add("MATTHEW");
		saints.add("MATTHIAS");
		saints.add("MAURICE");
		saints.add("MAURILIUS");
		saints.add("MAXIMILIAN");
		saints.add("MAXIMIN");
		saints.add("MAXIMUS");
		saints.add("MECHTILDIS");
		saints.add("MEDARD");
		saints.add("MELANIE");
		saints.add("MELLO");
		saints.add("MERICI");
		saints.add("MESMIN");
		saints.add("METHODIUS");
		saints.add("MICAELA");
		saints.add("MICHAEL");
		saints.add("MILAN");
		saints.add("MIRACLE");
		saints.add("MODESTUS");
		saints.add("MONICA");
		saints.add("MONTE");
		saints.add("MONTFORT");
		saints.add("MONTMORENCY");
		saints.add("MORE");
		saints.add("MYRA");
		saints.add("NARCISSUS");
		saints.add("NATIVITY");
		saints.add("NAZARIUS");
		saints.add("NAZIANZEN");
		saints.add("NEMESION");
		saints.add("NEPOMUCENE");
		saints.add("NERI");
		saints.add("NEWMINSTER");
		saints.add("NICASIUS");
		saints.add("NICHOLAS");
		saints.add("NISIBIS");
		saints.add("NOLASCO");
		saints.add("NONNATUS");
		saints.add("NORBERT");
		saints.add("NORTH");
		saints.add("ODON");
		saints.add("OIGNIES");
		saints.add("OLYMPIA");
		saints.add("OMER");
		saints.add("ONESIMUS");
		saints.add("OSWALD");
		saints.add("OUR");
		saints.add("PACHOMIUS");
		saints.add("PADUA");
		saints.add("PALERMO");
		saints.add("PALLADIUS");
		saints.add("PAMPHILUS");
		saints.add("PANTALEON");
		char AE = 198;
		saints.add("PANT" + AE + "NUS");
		saints.add("PAPHNUTIUS");
		saints.add("PARIS");
		saints.add("PASCHAL");
		saints.add("PATERNUS");
		saints.add("PATRICK");
		saints.add("PAUL");
		saints.add("PAULA");
		saints.add("PAULINUS");
		saints.add("PAZZI");
		saints.add("PELLETIER");
		saints.add("PENNAFORT");
		saints.add("PERBOYRE");
		saints.add("PERPETUUS");
		saints.add("PETER");
		saints.add("PETERS");
		saints.add("PETRONILLA");
		saints.add("PHILIP");
		saints.add("PHILOGONIUS");
		saints.add("PHILOMENA");
		saints.add("PIACENZA");
		saints.add("PIUS");
		saints.add("PLACID");
		saints.add("POITIERS");
		saints.add("POLYCARP");
		saints.add("POPE");
		saints.add("PORPHYRY");
		saints.add("PORRES");
		saints.add("PORTUGAL");
		saints.add("POTHINUS");
		saints.add("PRIMUS");
		saints.add("PROSPER");
		saints.add("PULCIANO");
		saints.add("QUEBEC");
		saints.add("QUEEN");
		saints.add("QUENTIN");
		saints.add("QUINZANI");
		saints.add("RADEGUNDES");
		saints.add("RAPHAEL");
		saints.add("RAYMOND");
		saints.add("RAYMUND");
		saints.add("REGIS");
		saints.add("REMI");
		saints.add("REMIGIUS");
		saints.add("RICCI");
		saints.add("RICHARD");
		saints.add("RITA");
		saints.add("ROBERT");
		saints.add("ROCH");
		saints.add("RODRIGUEZ");
		saints.add("ROGATIAN");
		saints.add("ROMANUS");
		saints.add("ROME");
		saints.add("ROMUALD");
		saints.add("ROSALIA");
		saints.add("ROSE");
		saints.add("SABAS");
		saints.add("SABINA");
		saints.add("SABINUS");
		saints.add("SAHAGUN");
		saints.add("SALES");
		saints.add("SANCTUS");
		saints.add("SANTOS");
		saints.add("SARAGOSSA");
		saints.add("SATURNINUS");
		saints.add("SAVIO");
		saints.add("SCHIEDAM");
		saints.add("SCHOLASTICA");
		saints.add("SCOTLAND");
		saints.add("SEBASTE");
		saints.add("SEBASTIAN");
		saints.add("SENNEN");
		saints.add("SERAPHIA");
		saints.add("SERENUS");
		saints.add("SERVITE");
		saints.add("SERVULUS");
		saints.add("SEVEN");
		saints.add("SEVERIANUS");
		saints.add("SEVERINUS");
		saints.add("SEVILLE");
		saints.add("SIENA");
		saints.add("SIGMARINGEN");
		saints.add("SILENT");
		saints.add("SILOS");
		saints.add("SILVERIUS");
		saints.add("SIMEON");
		saints.add("SIMON");
		saints.add("SIMPLICIUS");
		saints.add("SORROWS");
		saints.add("SOTER");
		saints.add("SOUBIROUS");
		saints.add("STANISLAUS");
		saints.add("STEINFELD");
		saints.add("STEPHANIE");
		saints.add("STEPHEN");
		saints.add("STIGMATA");
		saints.add("STOCK");
		saints.add("STRAMBI");
		saints.add("STYLITES");
		saints.add("SUSANNA");
		saints.add("SUZO");
		saints.add("SWEDEN");
		saints.add("SYLVAIN");
		saints.add("SYLVESTER");
		saints.add("SYMPHORIAN");
		saints.add("TAIGI");
		saints.add("TARACHUS");
		saints.add("TARASIUS");
		saints.add("TARSILLA");
		saints.add("TEKAKWITHA");
		saints.add("TERESA");
		saints.add("THEBAN");
		saints.add("THECLA");
		saints.add("THEODORE");
		saints.add("THEODORET");
		saints.add("THEODOSIUS");
		saints.add("THERESE");
		saints.add("THIENA");
		saints.add("THOMAS");
		saints.add("THOURET");
		saints.add("TIBURTIUS");
		saints.add("TIMOTHY");
		saints.add("TITUS");
		saints.add("TOLENTINO");
		saints.add("TOOLE");
		saints.add("TOULOUSE");
		saints.add("TOURS");
		saints.add("TYRO");
		saints.add("UGANDA");
		saints.add("URBAN");
		saints.add("URSULA");
		saints.add("URSULINE");
		saints.add("VALENTINE");
		saints.add("VALERY");
		saints.add("VALOIS");
		saints.add("VENANT");
		saints.add("VERCELLI");
		saints.add("VERONA");
		saints.add("VERONICA");
		saints.add("VIALAR");
		saints.add("VIANNEY");
		saints.add("VICTOR");
		saints.add("VICTORIAN");
		saints.add("VILLANOVA");
		saints.add("VINCENT");
		saints.add("VITALIS");
		saints.add("VITERBE");
		saints.add("VITERBO");
		saints.add("VITUS");
		saints.add("WENCESLAS");
		saints.add("WILFRID");
		saints.add("WILLIAM");
		saints.add("WILLIBRORD");
		saints.add("WORKER");
		saints.add("WULFRAN");
		saints.add("XAVIER");
		saints.add("YOUVILLE");
		saints.add("YVES");
		saints.add("ZACCARIA");
		saints.add("ZACHARY");
		saints.add("ZEPHYRINUS");
		saints.add("ZITA");

		specialSuffixes = new HashMap<String,String>();
		specialSuffixes.put("ST", "ST");
		specialSuffixes.put("STR", "ST");
		specialSuffixes.put("STREET", "ST");
		specialSuffixes.put("ST-R", "STREET");
		specialSuffixes.put("STRT", "ST");
		specialSuffixes.put("AVENUE", "AVE");
		specialSuffixes.put("AVE-R", "AVENUE");
		specialSuffixes.put("AVN", "AVE");
		specialSuffixes.put("WA", "WAY");
		specialSuffixes.put("AVNUE", "AVE");
		
		istateSuffixes = new HashMap<String,String>();
		istateSuffixes.put("INTERSTATE","INTERSTATE");
		istateSuffixes.put("INTERSTATE-R","INTERSTATE");
		istateSuffixes.put("IS","INTERSTATE");
	}
	
	// function to implode the given array into one string given the delimiter
	public static String implode(String delim, String [] in) {
		StringBuilder composite = new StringBuilder(in[0]);
		for (int i = 1; i < in.length; i++){
			composite.append(delim);
			composite.append(in[i]);
		}
		return composite.toString();
	}
	
	public static final boolean isSuffix(String check) {
		if(check == null)
			return false;
		return suffixes.containsKey(check.toUpperCase());
	}
	
	public static final boolean isSpecialSuffix(String check){
		if(check == null)
			return false;		
		return specialSuffixes.containsKey(check);
	}
	
	public static final boolean isIstateSuffix(String check){
		if(check == null)
			return false;				
		return istateSuffixes.containsKey(check);
	}
	
	public static final boolean isIdentifier(String check) {
		if(check == null)
			return false;				
		return identifiers.containsKey(check);
	}
	
	public static final boolean isState(String check) {
		if(check == null)
			return false;				
		return states.containsKey(check);
	}
	
	public static final boolean isStateAbbreviation(String check)
	{
		if(check == null)
			return false;
		return states.containsValue(check);
	}
	
	static final Pattern isNumberPattern = Pattern.compile("(^([0-9]+(ST|ND|RD|TH)?)$)");
	public static final boolean isNumber(String check) {
		if(check == null)
			return false;				
		return numbers.containsKey(check) || isNumberPattern.matcher(check).matches();
	}
	
	public static final String convertWordToNumber(String wordNumber) {
		wordNumber = StringUtils.defaultString(wordNumber);
		String number = StringUtils.defaultString((String) numbers.get(wordNumber));

		return number;
	}
	
	static final Pattern isPlainNumberPattern = Pattern.compile("(^([0-9]+)$)");
	public static final boolean isPlainNumber(String check) {
		if(check == null)
			return false;				
		return isPlainNumberPattern.matcher(check).matches();
	}
	
	public static final boolean isDirectional(String check) {
		if(check == null)
			return false;				
		return directions.containsKey(check);
	}
	
	public static final boolean isSaint(String check){
		if(check == null)
			return false;				
		return saints.contains(check);
	}
	
	public static final int directionType(String key){
		if(key == null)
			return -1;				
		if(directionTypes.containsKey(key))
			return directionTypes.get(key).intValue();
		else
			return -1;
	}
	
	public static String translateSuffix(String key){
		return suffixes.get(key);
	}

	public static String translateState(String key){
		return states.get(key);
	}

	public static String translateIdentifier(String key){
		return identifiers.get(key);
	}

	public static String translateDirection(String key){
		return directions.get(key);
	}

	public static final String extractLastWord(String phrase){
		String[] words = phrase.split(" ");
		for(int i=words.length-1; i>=0; i--)
			if(!words[i].equals(""))
				return words[i];
		return "";
	}
	
	public static final String extractFirstWord(String phrase){
		String[] words = phrase.split(" ");
		for(int i=0; i<words.length; i++)
			if(!words[i].equals(""))
				return words[i];
		return "";
	}
	
	public static final Pattern isCompositeNumberPattern = Pattern.compile("(^" + COMPOSITE_NUMBER + "$)");
	/**
	 * @return true if the given string is a number followed by letters
	 */	 
	public static final boolean isCompositeNumber(String str) {
		if(str == null)
			return false;
		// check for a number
		if (isNumber(str)) return true;
		// we were supposed to have a number, but we don't, try for (number-letter)
		if(isCompositeNumberPattern.matcher(str).matches()) return true;
		// not a number
		return false;
	}
	
	// Implement abbreviations for words at the ends of certain address lines.
	// only transforms suffixes, identifiers and directions based on the mapping tables
	// not changing the overall position of these tokens
	private static final String ALS_EOL_Abbr(String param) {
		int suff = 0;
		
		String [] parts = param.split(" ");
		int count = parts.length - 1;
		String [] out = new String[parts.length];
		
		for (int counter = count; counter > -1; counter--) {
			if ( suffixes.containsKey(parts[counter]) ) {			// if we find a suffix
				if (suff == 0) {						// and we haven't found one yet
					out[counter] = (String)suffixes.get(parts[counter]);	// transform it
					suff++;							// and mark as found
				} else {
					out[counter] = parts[counter];				// if we already processed suffix leave as is
				}
				
			} else if ( identifiers.containsKey(parts[counter]) ) {		// not a suffix, see if an identifier
				out[counter] = (String)identifiers.get(parts[counter]);
				
			} else if ( directions.containsKey(parts[counter]) ) {		// checking for direction
				out[counter] = (String)directions.get(parts[counter]);		// transform
				
			} else {								// none of the above, fill in
				out[counter] = parts[counter];
			}
		}
		return implode(" ", out);						// return the imploded one
	}
	
	
	// trim an address to USPS standard
	// will work if address is in a couple of standard forms
	// 
	// http://pe.usps.gov/cpim/ftp/pubs/Pub28/pub28.pdf
	static public final String trim(String address) {
		
		// remove dots and trim spaces
		address = address.replace(".", " ").replaceAll("\\s{2,}", " ").trim();
		
		if(address.equalsIgnoreCase("N/A")) // treat "N/A" as empty
			return "";		
		
		if (address.equals("")) // empty address
			return address;
		
		// we must leave text in between "" untouched
		Pattern leave = Pattern.compile("([^\"]*)\"([^\"]+)\"([^\"]*)");
		Matcher rest = leave.matcher(address);
		if (rest.matches()) {
			return (trim(rest.group(1)) + " \"" + rest.group(2) + "\" " + trim(rest.group(3))).trim();
		}
		
		address = (address.trim()).toUpperCase();		// change to upper case and take out beginning and trailing

		// if(!disableUpdates){ //debug
		/*
		 * if a suffix or an identifier is followed by "." then replace the "." by a space
		 */        
		if(address.indexOf('.') != -1)
		{
			String []tokens = address.split("[.]");
			String newAddress = "";
			// process all tokens
			for(int i=0; i<tokens.length; i++){					
				String newToken = "";							
				String [] words = tokens[i].split("[ ]");
				boolean addPeriod  = !(isIdentifier(words[words.length-1]) || isSuffix(words[words.length-1]));
				newToken = words[0];
				for(int j=1; j<words.length; j++){
					newToken += (" " + words[j]);
				}
				newAddress += newToken;
				if(addPeriod){					
					// add the period if it is not the last token
					if(i != (tokens.length-1)) 
						newAddress += ".";
					else
						// add the period at last position only if it was also present in the original address
						if(address.charAt(address.length()-1) == '.')
							newAddress += ".";
				}
				else{					
					if(i != (tokens.length-1)){ // check it's not last token												
						if(tokens[i+1].length() != 0) // check if next token not empty
							if(tokens[i+1].charAt(0) != ' ') // check if next token starts with a space
								newAddress += " ";
					}
				}
			}
			//if(!newAddress.equals(address))
			//System.out.println("TRANSF 'ABBREV.' : ["+address+"]" + " -> " + "["+newAddress+"]");
			address = newAddress;
		}
		
		/*
		 * treat the "ST <saint_name>" case
		 */  
		if(address.indexOf("ST") != -1){
			String[] tokens = address.split(" ");
			boolean foundST = false;
			boolean addedQuotes = false;
			int pos = 0;
			// find ST word position
			for(pos=0; pos<tokens.length; pos++)
				if(tokens[pos].equals("ST")){
					foundST = true;
					break;
				}		
			// if we found the ST word - it could be that "ST" was part of another word
			if(foundST){ 
				if(pos != (tokens.length - 1)){ // if it is not the last word
					if(isSaint(tokens[pos+1])){ // if a saint name follows
						tokens[pos] = "\"" + tokens[pos];
						tokens[pos+1] += "\"";
						addedQuotes = true;
					}
				}
				String newAddress = implode(" ", tokens);
				if(!newAddress.equals(address)){
					//System.out.println("TRANSF 'SAINT.' :["+address+"]" + " -> " + "["+newAddress+"]");
				}        
				address = newAddress;
				// if we have added the quotes we must leave text in between "" untouched
				if(addedQuotes){ 					
					leave = Pattern.compile("([^\"]*)\"([^\"]+)\"([^\"]*)");
					rest = leave.matcher(address);
					if (rest.matches())
						return (trim(rest.group(1)) + " \"" + rest.group(2) + "\" " + trim(rest.group(3))).trim();
				}
			}
		}
		
		/*
		 * if a hyphen is followed by an identifier, then we get rid of it
		 */   
		if(address.indexOf('-') != -1){
			String[] tokens = address.split("-");
			StringBuilder newAddress = new StringBuilder(100);
			for(int i=0;i<tokens.length;i++){
				newAddress.append(tokens[i]);
				String w2 = null;
				boolean removeHyphen = false;				
				if(i != (tokens.length-1)){					
					w2 = extractFirstWord(tokens[i+1]);
					removeHyphen = isIdentifier(w2);
				}
				// String w1 = extractLastWord(tokens[i]);
				// removeHyphen = isSuffix(w1) || isSuffix(w2) || isIdentifier(w1) || isIdentifier(w2);
				if(i != (tokens.length-1))
					if(!removeHyphen)
						newAddress.append("-");
					else
						newAddress.append(" ");
			}
			address = newAddress.toString();
		}
		//}  // disableUpdates
		address = address.replaceAll("[#]", " # ");		// add spaces around the #
		address = address.replaceAll("&", "-");			// normalize usual range designators
		address = address.replaceAll("/", "-");
		//address = (address.trim()).toUpperCase();		// change to upper case and take out beginning and trailing
		if (!address.trim().matches("\\d+\\s+ROAD'S END\\s+[A-Z ]+$")){
			address = address.replaceAll("[^A-Z0-9\\s/#.-]", "");	// take out all the non-characters
		}
		
		address = address.replaceAll(COMPOSITE_NUMBER + "\\s*-\\s*" + COMPOSITE_NUMBER, "$1$2-$3$4");
		address = address.replaceAll("([^0-9 ]+)\\s*-\\s*([^0-9 ]+)", "$1-$2");
		
		address = address.replaceAll("([^A-Z]+)([/.-]+)([^0-9]+)", "$1 $3");	// split units such as 109-G in 109 G
		
		// this rule must be modified to take in account composite numbers
		// address = address.replaceAll("([^0-9]+)([/.-]+)([^A-Z]+)", "$1 $3");	// same as above but G-109
		address = address.replaceAll("([\\s]+)", " ");				// replace all multiple spaces with one
		
		// create the patterns for the next operations
		// pattern is "1(anythingbutspace space) 2(letters) 3(space #) 4(space anythingbutspace)"
		Pattern pat = Pattern.compile("(.+[\\s])([A-Z]+)([\\s][#])([\\s].+)");
		Matcher mat = pat.matcher(address);
		
		if ( mat.matches() ) {
			if ( identifiers.containsKey(mat.group(2)) ) {
				address = new String(mat.group(1) + mat.group(2) + mat.group(4));
			}
		}
		// change constructs of the form ... "apt # 300" into "apt 300", so taking out the # in context
		
		address = address.trim();
		
		// process the trivial numbers into numerals
		String [] parts = address.split(" ");
		for (int i = 0; i < parts.length; i++) {
			if (numbers.containsKey(parts[i])) {
				parts[i] = (String) numbers.get(parts[i]);
			}
			else {
				break;
			}
		}

		for (int i = parts.length - 1; i >= 0; i--) {
			if (numbers.containsKey(parts[i])) {
				parts[i] = (String) numbers.get(parts[i]);
			}
			else {
				break;
			}
		}
		address = implode(" ", parts);
		parts = null;
		
		// replace stuff such as "1st Floor" into FL 1
		address = address.replaceAll("([0-9]+)(ST|ND|RD|TH)?([\\s]?)(FL|FLOOR|FLR)$", "FL $1");
		// change NORTH EAST into NORTHEAST
		address = address.replaceAll("(NORTH|SOUTH)([\\s])(EAST|WEST)", "$1$3");
		
		// Take care of rural delivery routes
		// changes "(RD)(maybe spaces)(numbers)(some letters or #s or spaces, at least one)(numbers and letters)(anything)end"
		// into " RR (numbers) BOX (numbers and letters)"
		Pattern rr1Pat = Pattern.compile("^(RR|RFD ROUTE|RURAL ROUTE|RURAL RT|RURAL RTE|RURAL DELIVERY|RD RTE|RD ROUTE)([\\s]?)([0-9]+)([A-Z #]+)([0-9A-Z]+)(.*)$");
		mat = rr1Pat.matcher(address);
		if ( mat.matches() ) {
			return "RR " + mat.group(3) + " BOX " + mat.group(5);
		}
		
		// change "begin(BOX)(spaces or #s)(letters numbers)(space)(route)(maybe spaces)(numbers)(anything)end"
		// into "RR (numbers) BOX (letters numbers)"
		Pattern rr2Pat = Pattern.compile("^(BOX|BX)([ #]*)([0-9A-Z]+)([\\s])(RR|RFD ROUTE|RURAL ROUTE|RURAL RT|RURAL RTE|RURAL DELIVERY|RD RTE|RD ROUTE)([\\s]?)([0-9]+)(.*)$");
		mat = rr2Pat.matcher(address);
		if ( mat.matches() ) {
			return "RR " + mat.group(7) + " BOX " + mat.group(3);
		}
		// end rural delivery routes
		
		
		// po box
		// changes from "beginning(PO BOX)(spaces(#s spaces))(letters numbers)(anything)end"
		// into "PO BOX (letters numbers)"
		Pattern poboxPat = Pattern.compile("^(POST OFFICE BOX|PO BOX|P O|P O BOX|P O B|P O BX|POB|BOX|PO|PO BX|BX|FIRM CALLER|CALLER|BIN|LOCKBOX|DRAWER)([\\s]+([#][\\s])*)([0-9A-Z-]+)(.*)$");
		mat = poboxPat.matcher(address);
		if ( mat.matches() ) {
			return "PO BOX " + mat.group(4);
		}
		
		
		boolean b;String [] atom;
		// county highway
		// does not much more than trim
		Pattern cntyHwyPat = Pattern.compile("^([0-9A-Z.-]+[\\s]?[0-9/]*[\\s]?)(.*)( CNTY| COUNTY)([\\s])(HIGHWAY|HIGHWY|HIWAY|HIWY|HWAY|HWY)( NO | # | )?([0-9A-Z]+)(.*)$");
		mat = cntyHwyPat.matcher(address);
		b = mat.matches();
		if ( b ) {
			atom = new String[9];
			for (int i = 1; i < 9; i++)
				atom[i] = mat.group(i);
			if ( states.containsKey(mat.group(2)) ) {
				atom[2] = (String)states.get(mat.group(2));
			}
			if ( identifiers.containsKey(mat.group(7)) ) {
				atom[7] = (String)identifiers.get(mat.group(7));
				atom[8] = (mat.group(8)).replaceAll(" #", "");
				return atom[1] + atom[2] + " COUNTY HWY " + atom[7] + atom[8];
			}
			return atom[1] + atom[2] + " COUNTY HIGHWAY " + atom[7] + ALS_EOL_Abbr(atom[8]);
		}
		
		// variations on county road
		Pattern varCntyRdPat = Pattern.compile("^([0-9A-Z.-]+[\\s]?[0-9/]*[\\s]?)?(.*)(\\s*\\bCR |(\\s*\\bCNTY|\\s*\\bCOUNTY)([\\s])(RD|ROAD))( NO | # | )?([0-9A-Z]+)(.*)$");
		mat = varCntyRdPat.matcher(address);
		b = mat.matches();
		if ( b ) {
			atom = new String[10];
			for (int i = 1; i < 10; i++)
				atom[i] = mat.group(i);
			if (atom[1] == null)
				atom[1] = "";
			if ( states.containsKey(atom[2]) ) {
				atom[2] = (String)states.get(atom[2]);
			}
			if ( identifiers.containsKey(atom[8]) ) {
				atom[8] = (String)identifiers.get(atom[8]);
				atom[9] = atom[9].replaceAll(" #", "");
				return (atom[1] + atom[2] + " COUNTY RD " + atom[8] + atom[9]).replaceAll("\\s{2,}", " ");
			}
			return (atom[1] + atom[2] + " COUNTY ROAD " + atom[8] + ALS_EOL_Abbr(atom[9])).replaceAll("\\s{2,}", " ").trim();
		}
		
		// state roads
		Pattern stateRdPat = Pattern.compile("^([0-9A-Z.-]+[\\s]?[0-9/]*[\\s]?)(.*)( SR|( ST| STATE)([\\s])(RD|ROAD))( NO | # | )?([0-9A-Z]+)(.*)$");
		mat = stateRdPat.matcher(address);
		b = mat.matches();
		if ( b ) {
			atom = new String[10];
			for (int i = 1; i < 10; i++)
				atom[i] = mat.group(i);
			
			if ( states.containsKey(atom[2]) ) {
				atom[2] = (String)states.get(atom[2]);
			}
			if ( identifiers.containsKey(atom[8]) ) {
				atom[8] = (String)identifiers.get(atom[8]);
				atom[9] = atom[9].replaceAll(" #", "");
				return atom[1] + atom[2] + " STATE RD " + atom[8] + atom[9];
			}
			return atom[1] + atom[2] + " STATE ROAD " + atom[8] + ALS_EOL_Abbr(atom[9]);
		}
		
		// state routes
		Pattern stateRtPat = Pattern.compile("^([0-9A-Z.-]+[\\s]?[0-9/]*[\\s]?)(.*)( ST| STATE)([\\s])(RT|RTE|ROUTE)( NO | # | )?([0-9A-Z]+)(.*)$");
		mat = stateRtPat.matcher(address);
		b = mat.matches();
		if ( b ) {
			atom = new String[9];
			for (int i = 1; i < 9; i++)
				atom[i] = mat.group(i);
			
			if ( states.containsKey(atom[2]) ) {
				atom[2] = (String)states.get(atom[2]);
			}
			if ( identifiers.containsKey(atom[7]) ) {
				atom[7] = (String)identifiers.get(atom[7]);
				atom[8] = atom[8].replaceAll(" #", "");
				return atom[1] + atom[2] + " STATE RTE " + atom[7] + atom[8];
			}
			return atom[1] + atom[2] + " STATE ROUTE " + atom[7] + ALS_EOL_Abbr(atom[8]);
		}
		
		
		// interstates
		// maybe they are selling a gas station on the interstate, or delivering mail to a rest area
		
		/*
		if(disableUpdates){
		pat = Pattern.compile("^([0-9A-Z.-]+[\\s][0-9/]*[\\s]?)(I|INTERSTATE|INTRST|INT)([\\s]?)(HIGHWAY|HIGHWY|HIWAY|HIWY|HWAY|HWY|H)?([\\s]?)([0-9]+)(.*)$");
		mat = pat.matcher(address);
		b = mat.matches();
		if ( b ) {
			atom = new String[8];
			for (int i = 1; i < 8; i++)
				atom[i] = mat.group(i);
			
			atom[7] = atom[7].replaceAll(" BYP ", " BYPASS ");
			return atom[1] + "INTERSTATE " + atom[6] + ALS_EOL_Abbr(atom[7]);
		}
		}else
		*/
		{
			// clean up the interstate pattern a little bit
			Pattern istatePat = Pattern.compile("^((?:[0-9A-Z.-]+[\\s][0-9/]*[\\s]?)?)(I|INTERSTATE|INTRST|INT)([\\s]?)(HIGHWAY|HIGHWY|HIWAY|HIWY|HWAY|HWY|H)?([\\s]?)([0-9]+)(.*)$");
			mat = istatePat.matcher(address);
			b = mat.matches();
			if ( b ) {
				atom = new String[8];
				for (int i = 1; i < 8; i++)
					atom[i] = mat.group(i);
				
				atom[7] = atom[7].replaceAll(" BYP ", " BYPASS ");
				return atom[1] + "INTERSTATE " + atom[6] + ALS_EOL_Abbr(atom[7]);
			}
		}
		// state highways
		Pattern stateHywPat = Pattern.compile("^([0-9A-Z.-]+[\\s]?[0-9/]*[\\s]?)(.*)( ST| STATE)([\\s])(HIGHWAY|HIGHWY|HIWAY|HIWY|HWAY|HWY)( NO | # | )?([0-9A-Z]+)(.*)$");
		mat = stateHywPat.matcher(address);
		b = mat.matches();
		if ( b ) {
			atom = new String[9];
			for (int i = 1; i < 9; i++)
				atom[i] = mat.group(i);
			
			if ( states.containsKey(atom[2]) ) {
				atom[2] = (String)states.get(atom[2]);
			}
			if ( identifiers.containsKey(atom[7]) ) {
				atom[7] = (String)identifiers.get(atom[7]);
				atom[8] = atom[8].replaceAll(" #", "");
				return atom[1] + atom[2] + " STATE HWY " + atom[7] + atom[8];
			}
			return atom[1] + atom[2] + " STATE HIGHWAY " + atom[7] + ALS_EOL_Abbr(atom[8]);
		}
		
		// ranch roads
		Pattern ranchPat = Pattern.compile("^([0-9A-Z.-]+[\\s][0-9/]*[\\s]?)(RANCH )(RD|ROAD)( NO | # | )?([0-9A-Z]+)(.*)$");
		mat = ranchPat.matcher(address);
		b = mat.matches();
		if ( b ) {
			atom = new String[7];
			for (int i = 1; i < 7; i++)
				atom[i] = mat.group(i);
			
			if ( identifiers.containsKey(atom[5]) ) {
				atom[5] = (String)identifiers.get(atom[5]);
				atom[6] = atom[6].replaceAll(" #", "");
				return atom[1] + "RANCH RD " + atom[5] + atom[6];
			}
			return atom[1] + "RANCH ROAD " + atom[5] + ALS_EOL_Abbr(atom[6]);
		}
		
		address = address.replaceAll("^([0-9A-Z.-]+)([\\s])([0-9][/][0-9])", "$1*$3");// ??? may have problems with *
		
		// plain roads
		Pattern plainPat = Pattern.compile("^([0-9A-Z/*.-]+[\\s])(RD|ROAD)([A-Z #]+)([0-9A-Z]+)(.*)$");
		mat = plainPat.matcher(address);
		b = mat.matches();
		if ( b ) {
			if (!(mat.group(3).matches("\\s+[NS]") && mat.group(4).matches("[EW]"))) {	//PICKERINGTON RD NW (OH Fairfield TR, PIN 0080002300)
				atom = new String[6];
				for (int i = 1; i < 6; i++)
					atom[i] = mat.group(i);
				
				atom[1] = atom[1].replaceAll("[*]", " ");
				return atom[1] + "ROAD " + atom[4] + ALS_EOL_Abbr(atom[5]);	
			}
		}
		
		// other strange routes
		Pattern strangePat = Pattern.compile("^([0-9A-Z/*.-]+[\\s])(RT|RTE|ROUTE)([A-Z #]+)([0-9A-Z]+)(.*)$");
		mat = strangePat.matcher(address);
		b = mat.matches();
		if ( b ) {
			atom = new String[6];
			for (int i = 1; i < 6; i++)
				atom[i] = mat.group(i);
			
			atom[1] = atom[1].replaceAll("[*]", " ");
			return atom[1] + "ROUTE " + atom[4] + ALS_EOL_Abbr(atom[5]);
		}
		
		// avenues
		Pattern avenuesPat = Pattern.compile("^([0-9A-Z/*.-]+[\\s])(AV|AVE|AVEN|AVENU|AVENUE|AVN|AVNUE)([\\s])([A-Z]+)(.*)$");
		mat = avenuesPat.matcher(address);
		b = mat.matches();
		if ( b ) {
			atom = new String[6];
			for (int i = 1; i < 6; i++)
				atom[i] = mat.group(i);
			
			atom[1] = atom[1].replaceAll("[*]", " ");
			return atom[1] + "AVENUE " + atom[4] + ALS_EOL_Abbr(atom[5]);
		}
		
		// boulevards
		Pattern boulevardsPat = Pattern.compile("^([0-9A-Z/*.-]+[\\s])(BLVD|BOUL|BOULEVARD|BOULV)([\\s])([A-Z]+)(.*)$");		
		mat = boulevardsPat.matcher(address);
		b = mat.matches();
		if ( b ) {
			atom = new String[6];
			for (int i = 1; i < 6; i++)
				atom[i] = mat.group(i);
			
			atom[1] = atom[1].replaceAll("[*]", " ");
			return atom[1] + "BOULEVARD " + ALS_EOL_Abbr(atom[4] + atom[5]);
		}
		
		/**************************************** END OF TRIMMING STANDARD FORMATS *******************************************/
		
		// System.out.println("After standard forms, got " + address);
		parts = address.split(" ");
		int count = parts.length - 1;
		int suff = 0;
		int id = 0; // identifier count
		String [] out = new String[parts.length];
		for (int i = 0; i < parts.length; i++) out[i] = new String();
		
		for (int counter = count; counter > -1; counter--) {
			if ( suffixes.containsKey(parts[counter]) ) { // found suffix on counter
				if (suff != 0) { // see if we already did suffix, and if so
					if (parts[counter].compareTo("VIA") == 0 || parts[counter].compareTo("LA") == 0) // special cases
						out[counter] = parts[counter];				// copy as it is
					else if (count==2 && counter==1 && isCompositeNumber(parts[0])) {	//1905 GLENN AVE (TN Knox PRI, 094HQ-021)
						out[counter] = parts[counter];
					} else {
						out[counter] = (String)suffixes.get(parts[counter]);
						out[counter] = (String)suffixes.get(out[counter] + "-R"); // get the standard form, expanded
					}
				} else { // we did not do suffix already
					if (parts[counter].compareTo("VIA") == 0 || parts[counter].compareTo("LA") == 0) // special cases
						out[counter] = parts[counter];				// copy as it is
					else {
						out[counter] = (String)suffixes.get(parts[counter]); // replace suffix in the intermediary form
						out[counter] = (String)suffixes.get(out[counter] + "-R"); // get the standard form, expanded
					}
				}
				
				suff++; // we found another suffix
				
			} else if ( identifiers.containsKey(parts[counter]) ) { 		// if it is an identifier
				// replace only the last one
				if (id == 0 
						&& (((counter != 0) && !parts[counter-1].matches("\\d+")) || (counter == 0))) { // fix for bug #1813, addr 5901 LOWER BREMO LN
					out[counter] = (String)identifiers.get(parts[counter]);		// replace it
				} else {
					out[counter] = (String)identifiers.get(parts[counter]);
					out[counter] = (String)identifiers.get(out[counter] + "-R");
				}
				id++;
				
			} else if ( directions.containsKey(parts[counter]) ) {	// now it's a directional
				int Prior = counter - 1;
				int Next = counter + 1;
				// if the direction contains a suffix after it
				if ( Next < parts.length && parts[Next].compareTo("") != 0  &&  suffixes.containsKey(parts[Next]) ) {
					out[counter] = (String)directions.get(parts[counter]); // replace the directional
					if (suff <= 1) {							// if first suffix
						out[counter] = (String)directions.get(out[counter] + "-R");	// get the full form
					}
					// we don't have suffix after us, but we're more than 2 off from the side and have a directional after us
				} else if ( Next < parts.length && counter > 2  &&  parts[Next].compareTo("") != 0  &&  directions.containsKey(parts[Next]) ) {
					out[counter] = parts[counter]; // put in as it is?
					
				} else if ( counter == 2  &&  directions.containsKey(parts[Prior]) ) { // we're exactly two off from the side
					out[counter] = parts[counter];					// the put as it is
					
				} else {
					out[counter] = (String)directions.get(parts[counter]);		// none of the above, transform directional
				} //  End of IF ELSE IF under directions
				
				
				// none of the above, so test if all we have in this atom is numbers, and we're not at the beginning or the end
				// this solves the case where we have the street name be a number, such as "NORTH 11 street"
			} else if( parts[counter].matches("(^([0-9]*)$)") && counter > 0 && counter < count) {
				int Prior = counter - 1;
				int Next = counter + 1;
				// if we have a direction before and after ourselves, put in as it is ???
				// if ( directions.containsKey(parts[Prior]) && directions.containsKey(parts[Next])) {
				if (!isNumber(parts[Prior]) || !suffixes.containsKey(parts[Next])) {
					out[counter] = parts[counter];
					
				} else {
					String comp;
					if (parts[counter].length() >= 2) 
						// check the end to see if we need to add some, transform names of streets that are numbers
						comp = parts[counter].substring(parts[counter].length() -2);
					else comp = parts[counter].substring(parts[counter].length() -1);
					if (comp.compareTo("11") == 0 || comp.compareTo("12") == 0 || comp.compareTo("13") == 0)
						out[counter] = parts[counter] + "TH";
					else {
						if (parts[counter].charAt(parts[counter].length() - 1) == '1')
							out[counter] = parts[counter] + "ST";
						else if (parts[counter].charAt(parts[counter].length() - 1) == '2')
							out[counter] = parts[counter] + "ND";
						else if (parts[counter].charAt(parts[counter].length() - 1) == '3')
							out[counter] = parts[counter] + "RD";
						else
							out[counter] = parts[counter] + "TH";
						// End of SWITCH substr -1 $Temp
					} // End of SWITCH substr -2 $Temp
					
				} // End of IF prior and next are directions under numbers.
				
			} else {
				out[counter] = parts[counter]; // else give up, put as it is - there is nothing we can do
				
			} // End of IF ELSEIF for current part.
			
		} // End of FOR parts counter.
		
		out[0] = out[0].replaceAll("[*]", " ");
		
		return implode(" ", out);
		
	} // End of function transform
	
	public static HashMap<String, String> getAllSuffixes () {
		return suffixes;
	}
	
	public static void main(String[] args) {
		TreeMap<String, TreeSet<String>> allList = new TreeMap<String, TreeSet<String>>();
		
		TreeSet<String> all = new TreeSet<String>();
		for (String string : suffixes.keySet()) {
			String realKey = suffixes.get(string);
			TreeSet<String> currentSet = allList.get(realKey);
			if(currentSet == null) {
				currentSet = new TreeSet<String>();
				allList.put(realKey, currentSet);
			}
			currentSet.add(string);
			all.add(string);
			all.add(suffixes.get(string));
		}
		/*
		for (String string : all) {
			System.out.println(string);
		}
		*/
		for (String realKey : allList.keySet()) {
			String line = StringUtils.rightPad(realKey, 12) + " >>> ";
			
			TreeSet<String> currentSet = allList.get(realKey);
			
			for (String string : currentSet) {
				line += string + ", ";
			}
			System.out.println(line.substring(0, line.length() - 2));
		}
	}

	public static HashMap<String, String> getStates() {
		return states;
	}
}
