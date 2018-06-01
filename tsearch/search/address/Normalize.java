package ro.cst.tsearch.search.address;

import java.io.Serializable;
import java.util.*;
import java.util.regex.*;
import java.util.ResourceBundle;
import ro.cst.tsearch.utils.URLMaping;


/**
 * Address preprocessing.
 */
public class Normalize implements Serializable {
	private static ResourceBundle rbc = ResourceBundle.getBundle(URLMaping.SERVER_CONFIG);
	private static boolean useNewVersionFlag = Boolean.parseBoolean(rbc.getString("use.new.address.matcher").trim());	
	private static boolean useNewVersion(){ return useNewVersionFlag; }
    
	static final long serialVersionUID = 10000000;
    
	public static final int ABREV_NONE = 0;
	public static final int ABREV_STATE = 1;
	public static final int ABREV_SUFFIX = 2;
	public static final int ABREV_IDENT = 3;
	public static final int ABREV_DIR = 4;

	/**
	 * Address states map.
	 */
	private static HashMap states;
	/**
	 * Address suffixes map. 
	 */
	private static HashMap suffixes;
	/**
	 * Address identifiers map.
	 */
	private static HashMap identifiers;
	/**
	 * Address directions map.
	 */
	private static HashMap directions;
	/**
	 * Address numbers map.
	 */
	private static HashMap numbers;

	/**
	 * Static block for the variables.
	 */
	static {

		// fill in directions map.		
		directions = new HashMap();
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

		// fill in numbers map
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

		// fill in states and their abrevs
		states = new HashMap();
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

		// fill in identifiers
		identifiers = new HashMap();
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
		identifiers.put("UNITS", "UNIT");             
		identifiers.put("UNIT-R", "UNIT");
		identifiers.put("UPPER", "UPPR");
		identifiers.put("UPPR-R", "UPPER");
		identifiers.put("UPPR", "UPPR");
		identifiers.put("UPR", "UPPR");

		// fill in suffixes
		suffixes = new HashMap();
		suffixes.put("ALLEE", "ALY");
		suffixes.put("ALLEY", "ALY");
		suffixes.put("ALY-R", "ALLEY");
		suffixes.put("ALLY", "ALY");
		suffixes.put("ALY", "ALY");
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
		suffixes.put("BVLD", "BLVD"); 
		suffixes.put("BV", "BLVD"); 
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
		suffixes.put("BY PASS", "BYP");
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
		suffixes.put("CTS-R", "COURT");
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
		suffixes.put("PK", "PARK");
		suffixes.put("PARK-R", "PARK");
		suffixes.put("PARKS", "PARK");
		suffixes.put("PRK", "PARK");
		suffixes.put("PAS", "PASS");
		suffixes.put("PASS", "PASS");
		suffixes.put("PASS-R", "PASS");
		suffixes.put("PATH", "PATH");
		suffixes.put("PATH-R", "PATH");
		suffixes.put("PATHS", "PATH");
		suffixes.put("PKE", "PIKE");
		suffixes.put("PIKE", "PIKE");
		suffixes.put("PIKE-R", "PIKE");
		suffixes.put("PIKES", "PIKE");
		suffixes.put("PARKWAY", "PKWY");
		suffixes.put("PKWY-R", "PARKWAY");
		suffixes.put("PW", "PKWY");
		suffixes.put("PARKWAYS", "PKWY");
		suffixes.put("PARKWY", "PKWY");
		suffixes.put("PKWAY", "PKWY");
		suffixes.put("PKWY", "PKWY");
		suffixes.put("PKWYS", "PKWY");
		suffixes.put("PKY", "PKWY");
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
		suffixes.put("TRCE-R", "TRACE");
		suffixes.put("TRACES", "TRCE");
		suffixes.put("TRCE", "TRCE");
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

	}
	public static String translateSuffix(String key){
		if(useNewVersion()) 
			return ro.cst.tsearch.search.address2.Normalize.translateSuffix(key); 
		return (String)suffixes.get(key);
	}

	public static String translateState(String key){
		if(useNewVersion()) 
			return ro.cst.tsearch.search.address2.Normalize.translateState(key); 		
		return (String)states.get(key);
	}

	public static String translateIdentifier(String key){
		if(useNewVersion()) 
			return ro.cst.tsearch.search.address2.Normalize.translateIdentifier(key); 				
		return (String)identifiers.get(key);
	}

	public static String translateDirection(String key){
		if(useNewVersion()) 
			return ro.cst.tsearch.search.address2.Normalize.translateDirection(key); 						
		return (String)directions.get(key);
	}

	/**
	 * Implode the given array into one string given the delimiter.
	 */
	private static String implode(String delim, String[] in) {
		String composite = new String(in[0]);
		for (int i = 1; i < in.length; i++)
			composite = composite.concat(delim + in[i]);
		return composite;
	}

	public static boolean isSuffix(String check) {
		if(useNewVersion()) 
			return ro.cst.tsearch.search.address2.Normalize.isSuffix(check);		
		return suffixes.containsKey(check);
	}

	public static boolean isIdentifier(String check) {
		if(useNewVersion()) 
			return ro.cst.tsearch.search.address2.Normalize.isIdentifier(check);				
		return identifiers.containsKey(check);
	}

	public static boolean isState(String check) {
		if(useNewVersion()) 
			return ro.cst.tsearch.search.address2.Normalize.isState(check);						
		return states.containsKey(check);
	}

	public static boolean isNumber(String check) {
		if(useNewVersion()) 
			return ro.cst.tsearch.search.address2.Normalize.isNumber(check);								
		return numbers.containsKey(check) || check.matches("(^([0-9]*)$)");
	}

	public static boolean isDirectional(String check) {
		if(useNewVersion()) 
			return ro.cst.tsearch.search.address2.Normalize.isDirectional(check);										
		return directions.containsKey(check);
	}

	/**
	 * Implement abbreviations for words at the ends of certain address lines.
	 * Oonly transforms suffixes, identifiers and directions based on the mapping tables.
	 * Not changing the overall position of these tokens.
	 */ 
	private static String ALS_EOL_Abbr(String param) {
		int suff = 0;
		int id = 0;

		String[] Parts = param.split(" ");
		int Count = Parts.length - 1;
		String[] Out = new String[Parts.length];

		for (int Counter = Count; Counter > -1; Counter--) {
			if (suffixes.containsKey(Parts[Counter])) { // if we find a suffix
				if (suff == 0) { // and we haven't found one yet
					Out[Counter] = (String) suffixes.get(Parts[Counter]);
					// transform it
					suff++; // and mark as found
					if (Counter == Count) {
						// if we found it at the end set some
						id = 1; // id we never use
					}
				} else {
					Out[Counter] = Parts[Counter];
					// if we already processed suffix leave as is
				}

			} else if (
				identifiers.containsKey(
					Parts[Counter])) { // not a suffix, see if an identifier
				Out[Counter] = (String) identifiers.get(Parts[Counter]);
				// if so replace, and set the unnecessary ID
				id = 1;

			} else if (
				directions.containsKey(
					Parts[Counter])) { // checking for direction
				Out[Counter] = (String) directions.get(Parts[Counter]);
				// transform
				if (Counter == Count) {
					id = 1;
				}

			} else { // none of the above, fill in
				Out[Counter] = Parts[Counter];
			}
		}

		return implode(" ", Out); // return the imploded one

	}

	/**
	 * Trim an address to USPS standard.
	 * Will work if address is in a couple of standard forms.
	 * 
	 *@see http://pe.usps.gov/cpim/ftp/pubs/Pub28/pub28.pdf
	 */
	public static String trim(String Address) {
		if(useNewVersion()) 
			return ro.cst.tsearch.search.address2.Normalize.trim(Address);
		
		// trivial case
		if (Address.compareTo("") == 0) {
			return "";
		}

		Address = Address.replaceAll("[#]", " # "); // add spaces around the #
		Address = (Address.trim()).toUpperCase();
		// change to upper case and take out beginning and trailing
		Address = Address.replaceAll("[^A-Z0-9\\s\"/#.-]", "");
		// take out all the non-characters
		Address = Address.replaceAll("([A-Z])([.])", "$1 ");
		// normalize blocks of chars +other to just blocks
		Address = Address.replaceAll("([A-Z]+)([-])([A-Z]+)", "$1 $3");
		// split *-* into * *
		Address = Address.replaceAll("([^A-Z]+)([/.-]+)([^0-9]+)", "$1$3");
		// split units such as 109-G in 109G
		Address =
			Address.replaceAll("([^0-9]+[A-Z]+)([/.-]+)([^A-Z]+)", "$1$3");
		// same as above but G-109
		Address = Address.replaceAll("([\\s]+)", " ");
		// replace all multiple spaces with one

		// create the patterns for the next operations
		// pattern is "1(anythingbutspace space) 2(letters) 3(space #) 4(space anythingbutspace)"
		Pattern pat = Pattern.compile("(.+[\\s])([A-Z]+)([\\s][#])([\\s].+)");
		Matcher mat = pat.matcher(Address);

		if (mat.matches()) {
			if (identifiers.containsKey(mat.group(2))) {
				Address =
					new String(mat.group(1) + mat.group(2) + mat.group(4));
			}
		}
		// change constructs of the form ... "apt # 300" into "apt 300", so taking out the # in context

		Address = Address.trim();

		// process the trivial numbers into numerals
		String[] Parts = Address.split(" ");
		for (int i = 0; i < Parts.length; i++) {
			if (numbers.containsKey(Parts[i])) {
				Parts[i] = (String) numbers.get(Parts[i]);
			}
		}
		Address = implode(" ", Parts);
		Parts = null;

		// replace stuff such as "1st Floor" into FL 1
		Address =
			Address.replaceAll(
				"([0-9]+)(ST|ND|RD|TH)?([\\s]?)(FL|FLOOR|FLR)$",
				"FL $1");
		// change NORTH EAST into NORTHEAST
		Address = Address.replaceAll("(NORTH|SOUTH)([\\s])(EAST|WEST)", "$1$3");

		// Take care of rural delivery routes
		// changes "(RD)(maybe spaces)(numbers)(some letters or #s or spaces, at least one)(numbers and letters)(anything)end"
		// into " RR (numbers) BOX (numbers and letters)"
		pat =
			Pattern.compile(
				"^(RR|RFD ROUTE|RURAL ROUTE|RURAL RT|RURAL RTE|RURAL DELIVERY|RD RTE|RD ROUTE)([\\s]?)([0-9]+)([A-Z #]+)([0-9A-Z]+)(.*)$");
		mat = pat.matcher(Address);
		if (mat.matches()) {
			return "RR " + mat.group(3) + " BOX " + mat.group(5);
		}

		// change "begin(BOX)(spaces or #s)(letters numbers)(space)(route)(maybe spaces)(numbers)(anything)end"
		// into "RR (numbers) BOX (letters numbers)"
		pat =
			Pattern.compile(
				"^(BOX|BX)([ #]*)([0-9A-Z]+)([\\s])(RR|RFD ROUTE|RURAL ROUTE|RURAL RT|RURAL RTE|RURAL DELIVERY|RD RTE|RD ROUTE)([\\s]?)([0-9]+)(.*)$");
		mat = pat.matcher(Address);
		if (mat.matches()) {
			return "RR " + mat.group(7) + " BOX " + mat.group(3);
		}
		// end rural delivery routes

		// po box
		// changes from "beginning(PO BOX)(spaces(#s spaces))(letters numbers)(anything)end"
		// into "PO BOX (letters numbers)"
		pat =
			Pattern.compile(
				"^(POST OFFICE BOX|PO BOX|P O|P O BOX|P O B|P O BX|POB|BOX|PO|PO BX|BX|FIRM CALLER|CALLER|BIN|LOCKBOX|DRAWER)([\\s]+([#][\\s])*)([0-9A-Z-]+)(.*)$");
		mat = pat.matcher(Address);
		if (mat.matches()) {
			return "PO BOX " + mat.group(4);
		}

		boolean b;
		String[] Atom;
		// county highway
		// does not much more than trim
		pat =
			Pattern.compile(
				"^([0-9A-Z.-]+[\\s]?[0-9/]*[\\s]?)(.*)( CNTY| COUNTY)([\\s])(HIGHWAY|HIGHWY|HIWAY|HIWY|HWAY|HWY)( NO | # | )?([0-9A-Z]+)(.*)$");
		mat = pat.matcher(Address);
		b = mat.matches();
		if (b) {
			Atom = new String[9];
			for (int i = 1; i < 9; i++)
				Atom[i] = mat.group(i);
			if (states.containsKey(mat.group(2))) {
				Atom[2] = (String) states.get(mat.group(2));
			}
			if (identifiers.containsKey(mat.group(7))) {
				Atom[7] = (String) identifiers.get(mat.group(7));
				Atom[8] = (mat.group(8)).replaceAll(" #", "");
				return Atom[1] + Atom[2] + " COUNTY HWY " + Atom[7] + Atom[8];
			}
			return Atom[1]
				+ Atom[2]
				+ " COUNTY HIGHWAY "
				+ Atom[7]
				+ ALS_EOL_Abbr(Atom[8]);
		}

		// variations on county road
		pat =
			Pattern.compile(
				"^([0-9A-Z.-]+[\\s]?[0-9/]*[\\s]?)(.*)( CR |( CNTY| COUNTY)([\\s])(RD|ROAD))( NO | # | )?([0-9A-Z]+)(.*)$");
		mat = pat.matcher(Address);
		b = mat.matches();
		if (b) {
			Atom = new String[10];
			for (int i = 1; i < 10; i++)
				Atom[i] = mat.group(i);
			if (states.containsKey(Atom[2])) {
				Atom[2] = (String) states.get(Atom[2]);
			}
			if (identifiers.containsKey(Atom[8])) {
				Atom[8] = (String) identifiers.get(Atom[8]);
				Atom[9] = Atom[9].replaceAll(" #", "");
				return Atom[1] + Atom[2] + " COUNTY RD " + Atom[8] + Atom[9];
			}
			return Atom[1]
				+ Atom[2]
				+ " COUNTY ROAD "
				+ Atom[8]
				+ ALS_EOL_Abbr(Atom[9]);
		}

		// state roads
		pat =
			Pattern.compile(
				"^([0-9A-Z.-]+[\\s]?[0-9/]*[\\s]?)(.*)( SR|( ST| STATE)([\\s])(RD|ROAD))( NO | # | )?([0-9A-Z]+)(.*)$");
		mat = pat.matcher(Address);
		b = mat.matches();
		if (b) {
			Atom = new String[10];
			for (int i = 1; i < 10; i++)
				Atom[i] = mat.group(i);

			if (states.containsKey(Atom[2])) {
				Atom[2] = (String) states.get(Atom[2]);
			}
			if (identifiers.containsKey(Atom[8])) {
				Atom[8] = (String) identifiers.get(Atom[8]);
				Atom[9] = Atom[9].replaceAll(" #", "");
				return Atom[1] + Atom[2] + " STATE RD " + Atom[8] + Atom[9];
			}
			return Atom[1]
				+ Atom[2]
				+ " STATE ROAD "
				+ Atom[8]
				+ ALS_EOL_Abbr(Atom[9]);
		}

		// state routes
		pat =
			Pattern.compile(
				"^([0-9A-Z.-]+[\\s]?[0-9/]*[\\s]?)(.*)( ST| STATE)([\\s])(RT|RTE|ROUTE)( NO | # | )?([0-9A-Z]+)(.*)$");
		mat = pat.matcher(Address);
		b = mat.matches();
		if (b) {
			Atom = new String[9];
			for (int i = 1; i < 9; i++)
				Atom[i] = mat.group(i);

			if (states.containsKey(Atom[2])) {
				Atom[2] = (String) states.get(Atom[2]);
			}
			if (identifiers.containsKey(Atom[7])) {
				Atom[7] = (String) identifiers.get(Atom[7]);
				Atom[8] = Atom[8].replaceAll(" #", "");
				return Atom[1] + Atom[2] + " STATE RTE " + Atom[7] + Atom[8];
			}
			return Atom[1]
				+ Atom[2]
				+ " STATE ROUTE "
				+ Atom[7]
				+ ALS_EOL_Abbr(Atom[8]);
		}

		// interstates
		// maybe they are selling a gas station on the interstate, or delivering mail to a rest area
		pat =
			Pattern.compile(
				"^([0-9A-Z.-]+[\\s][0-9/]*[\\s]?)(I|INTERSTATE|INTRST|INT)([\\s]?)(HIGHWAY|HIGHWY|HIWAY|HIWY|HWAY|HWY|H)?([\\s]?)([0-9]+)(.*)$");
		mat = pat.matcher(Address);
		b = mat.matches();
		if (b) {
			Atom = new String[8];
			for (int i = 1; i < 8; i++)
				Atom[i] = mat.group(i);

			Atom[7] = Atom[7].replaceAll(" BYP ", " BYPASS ");
			return Atom[1] + "INTERSTATE " + Atom[6] + ALS_EOL_Abbr(Atom[7]);
		}

		// state highways
		pat =
			Pattern.compile(
				"^([0-9A-Z.-]+[\\s]?[0-9/]*[\\s]?)(.*)( ST| STATE)([\\s])(HIGHWAY|HIGHWY|HIWAY|HIWY|HWAY|HWY)( NO | # | )?([0-9A-Z]+)(.*)$");
		mat = pat.matcher(Address);
		b = mat.matches();
		if (b) {
			Atom = new String[9];
			for (int i = 1; i < 9; i++)
				Atom[i] = mat.group(i);

			if (states.containsKey(Atom[2])) {
				Atom[2] = (String) states.get(Atom[2]);
			}
			if (identifiers.containsKey(Atom[7])) {
				Atom[7] = (String) identifiers.get(Atom[7]);
				Atom[8] = Atom[8].replaceAll(" #", "");
				return Atom[1] + Atom[2] + " STATE HWY " + Atom[7] + Atom[8];
			}
			return Atom[1]
				+ Atom[2]
				+ " STATE HIGHWAY "
				+ Atom[7]
				+ ALS_EOL_Abbr(Atom[8]);
		}

		// ranch roads
		pat =
			Pattern.compile(
				"^([0-9A-Z.-]+[\\s][0-9/]*[\\s]?)(RANCH )(RD|ROAD)( NO | # | )?([0-9A-Z]+)(.*)$");
		mat = pat.matcher(Address);
		b = mat.matches();
		if (b) {
			Atom = new String[7];
			for (int i = 1; i < 7; i++)
				Atom[i] = mat.group(i);

			if (identifiers.containsKey(Atom[5])) {
				Atom[5] = (String) identifiers.get(Atom[5]);
				Atom[6] = Atom[6].replaceAll(" #", "");
				return Atom[1] + "RANCH RD " + Atom[5] + Atom[6];
			}
			return Atom[1] + "RANCH ROAD " + Atom[5] + ALS_EOL_Abbr(Atom[6]);
		}

		Address =
			Address.replaceAll("^([0-9A-Z.-]+)([\\s])([0-9][/][0-9])", "$1*$3");
		// ??? may have problems with *

		// plain roads
		pat =
			Pattern.compile(
				"^([0-9A-Z/*.-]+[\\s])(RD|ROAD)([A-Z #]+)([0-9A-Z]+)(.*)$");
		mat = pat.matcher(Address);
		b = mat.matches();
		if (b) {
			Atom = new String[6];
			for (int i = 1; i < 6; i++)
				Atom[i] = mat.group(i);

			Atom[1] = Atom[1].replaceAll("[*]", " ");
			return Atom[1] + "ROAD " + Atom[4] + ALS_EOL_Abbr(Atom[5]);
		}

		// other strange routes
		pat =
			Pattern.compile(
				"^([0-9A-Z/*.-]+[\\s])(RT|RTE|ROUTE)([A-Z #]+)([0-9A-Z]+)(.*)$");
		mat = pat.matcher(Address);
		b = mat.matches();
		if (b) {
			Atom = new String[6];
			for (int i = 1; i < 6; i++)
				Atom[i] = mat.group(i);

			Atom[1] = Atom[1].replaceAll("[*]", " ");
			return Atom[1] + "ROUTE " + Atom[4] + ALS_EOL_Abbr(Atom[5]);
		}

		// avenues
		pat =
			Pattern.compile(
				"^([0-9A-Z/*.-]+[\\s])(AV|AVE|AVEN|AVENU|AVENUE|AVN|AVNUE)([\\s])([A-Z]+)(.*)$");
		mat = pat.matcher(Address);
		b = mat.matches();
		if (b) {
			Atom = new String[6];
			for (int i = 1; i < 6; i++)
				Atom[i] = mat.group(i);

			Atom[1] = Atom[1].replaceAll("[*]", " ");
			return Atom[1] + "AVENUE " + Atom[4] + ALS_EOL_Abbr(Atom[5]);
		}

		// boulevards
		pat =
			Pattern.compile(
				"^([0-9A-Z/*.-]+[\\s])(BLVD|BOUL|BOULEVARD|BOULV)([\\s])([A-Z]+)(.*)$");
		mat = pat.matcher(Address);
		b = mat.matches();
		if (b) {
			Atom = new String[6];
			for (int i = 1; i < 6; i++)
				Atom[i] = mat.group(i);

			Atom[1] = Atom[1].replaceAll("[*]", " ");
			return Atom[1] + "BOULEVARD " + ALS_EOL_Abbr(Atom[4] + Atom[5]);
		}

		// for saint at the end, this one does it
		Address = Address.replaceAll("^([0-9A-Z/*.-]+[\\s])(ST )", "$1SAINT ");

		/**************************************** END OF TRIMMING STANDARD FORMATS *******************************************/

		Parts = Address.split(" ");
		int Count = Parts.length - 1;
		int Suff = 0;
		int ID = 0;
		String[] Out = new String[Parts.length];
		for (int i = 0; i < Parts.length; i++)
			Out[i] = new String();

		for (int Counter = Count; Counter > -1; Counter--) {
			if (suffixes.containsKey(Parts[Counter])) {
				// found suffix on Counter
				if (Suff != 0) { // see if we already did suffix, and if so
					if (Counter + 2 < Parts.length
						&& Out[Counter + 1].compareTo("") != 0
						&& Out[Counter + 2].compareTo("") != 0) {
						// we are 2 positions off
						// if we have two directionals of the same type one of them is part of the name of the street
						// such as E West Main // put in as it is?
						String comp = Out[Counter + 1] + " " + Out[Counter + 2];
						if (comp.compareTo("EAST W") == 0
							|| comp.compareTo("WEST E") == 0
							|| comp.compareTo("NORTH S") == 0
							|| comp.compareTo("SOUTH N") == 0)
							Out[Counter] = Parts[Counter];
						else
							Out[Counter] =
								(String) suffixes.get(Parts[Counter]);
						// none of that strange stuff, just replace it
					} else {
						Out[Counter] = (String) suffixes.get(Parts[Counter]);
						// same thing as above, just replace
						Out[Counter] =
							(String) suffixes.get(Out[Counter] + "-R");
						// get the standard form, expanded
					}
					if (Counter == Count) {
						ID++;
						// set the ID that we never use, if we replaced at the end
					}

				} else { // we did not do suffix already
					if (Parts[Counter].compareTo("VIA") == 0
						|| Parts[Counter].compareTo("LA") == 0) // special cases
						Out[Counter] = Parts[Counter]; // copy as it is
					else {
						Out[Counter] = (String) suffixes.get(Parts[Counter]);
						// replace suffix in the intermediary form
						Out[Counter] =
							(String) suffixes.get(Out[Counter] + "-R");
						// get the standard form, expanded
					}
				} // End of IF stop under suffixes.

				Suff++; // we found another suffix

			} else if (
				identifiers.containsKey(
					Parts[Counter])) { // if it is an identifier
				Out[Counter] = (String) identifiers.get(Parts[Counter]);
				// replace it
				if (Suff > 0) { // if we already got a suffix
					Out[Counter] =
						(String) identifiers.get(Out[Counter] + "-R");
					// make sure it is in the final form (unabrev)
				}
				ID++;
				// found another one - i think this is the number of instances replaced or something

			} else if (
				directions.containsKey(
					Parts[Counter])) { // now it's a directional
				int Prior = Counter - 1;
				int Next = Counter + 1;
				// if the direction contains a suffix after it
				if (Next < Parts.length
					&& Parts[Next].compareTo("") != 0
					&& suffixes.containsKey(Parts[Next])) {
					Out[Counter] = (String) directions.get(Parts[Counter]);
					// replace the directional
					if (Suff <= 1) { // if first suffix
						Out[Counter] =
							(String) directions.get(Out[Counter] + "-R");
						// get the full form
					}
					// we don't have suffix after us, but we're more than 2 off from the side and have a directional after us
				} else if (
					Next < Parts.length
						&& Counter > 2
						&& Parts[Next].compareTo("") != 0
						&& directions.containsKey(Parts[Next])) {
					Out[Counter] = Parts[Counter]; // put in as it is?

				} else if (
					Counter == 2
						&& directions.containsKey(
							Parts[Prior])) { // we're exactly two off from the side
					Out[Counter] = Parts[Counter]; // the put as it is

				} else {
					Out[Counter] = (String) directions.get(Parts[Counter]);
					// none of the above, transform directional
				} //  End of IF ELSE IF under directions

				if (Counter == Count) { // if we replaced at the end, add
					ID = 1;
				}

				// none of the above, so test if all we have in this atom is numbers, and we're not at the beginning or the end
				// this solves the case where we have the street name be a number, such as "NORTH 11 street"
			} else if (
				Parts[Counter].matches("(^([0-9]*)$)")
					&& Counter > 0
					&& Counter < Count) {
				int Prior = Counter - 1;
				int Next = Counter + 1;
				// if we have a direction before and after ourselves, put in as it is ???
				if ((directions.containsKey(Parts[Prior])
					&& directions.containsKey(Parts[Next]))
					|| (suffixes.containsKey(Parts[Prior])
						&& !Parts[Prior].equals("RTE")
						&& !Parts[Prior].equals("ROUTE"))
					|| isNumber(Parts[Prior])
					|| Parts[Prior].equals("#") 
					|| Parts[Next].equals("#") 
					|| identifiers.containsKey(Parts[Next])) { 
					Out[Counter] = Parts[Counter];

				} else {
					String comp;
					if (Parts[Counter].length() >= 2)
						// check the end to see if we need to add some, transform names of streets that are numbers
						comp =
							Parts[Counter].substring(
								Parts[Counter].length() - 2);
					else
						comp =
							Parts[Counter].substring(
								Parts[Counter].length() - 1);
					if (comp.compareTo("11") == 0
						|| comp.compareTo("12") == 0
						|| comp.compareTo("13") == 0)
						Out[Counter] = Parts[Counter] + "TH";
					else {
						if (Parts[Counter].charAt(Parts[Counter].length() - 1)
							== '1')
							Out[Counter] = Parts[Counter] + "ST";
						else if (
							Parts[Counter].charAt(Parts[Counter].length() - 1)
								== '2')
							Out[Counter] = Parts[Counter] + "ND";
						else if (
							Parts[Counter].charAt(Parts[Counter].length() - 1)
								== '3')
							Out[Counter] = Parts[Counter] + "RD";
						else
							Out[Counter] = Parts[Counter] + "TH";
					}

				}

			} else {
				// else give up, put as it is - there is nothing we can do
				Out[Counter] = Parts[Counter];
			}
		}

		Out[0] = Out[0].replaceAll("[*]", " ");

		return implode(" ", Out);
	}
	
    /**
     * This function searches for all occurences of suffixes, directionals and identifiers 
     * in a string and puts them into the standard form so that for example two equivalent 
     * address strings can be then compared
     * @param inputString the input string
     * @return output string, with suffixes, directionals and identifiers normalized
     */
	public static String normalizeString(String inputString){
		StringBuilder sb = new StringBuilder(2 * inputString.length());
		String [] inputWords = inputString.split(" ");
		for(int i=0; i<inputWords.length; i++){
			String crtWord = inputWords[i];
			if(isSuffix(crtWord)){
				crtWord = translateSuffix(crtWord);
			}else if(isDirectional(crtWord)){
				crtWord = translateDirection(crtWord);
			}else if(isIdentifier(crtWord)){
				crtWord = translateIdentifier(crtWord);
			}
		    sb.append(crtWord);
			sb.append(" ");
		}
		return sb.toString().trim(); 
	}
}
