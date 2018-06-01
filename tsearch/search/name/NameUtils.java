package ro.cst.tsearch.search.name;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.Vector;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import ro.cst.tsearch.search.tokenlist.NameTokenList;
import org.apache.log4j.Category;

/**
 * Provides name utility methods, like checking if a name token list or a string 
 * are company names
 * @author radu
 *
 */
public class NameUtils {

	protected static final Category logger = Category.getInstance(NameUtils.class.getName());
	
	public final static Pattern firstAndMiddleInitialsWithSeparator = Pattern.compile("[ \t]+[a-zA-Z][ \t]+[a-zA-Z][ \t]*[,][ \t]*");
	public final static Pattern firstAndMiddleInitials = Pattern.compile("[ \t]+[a-zA-Z][ \t]+[a-zA-Z][ \t]+");
		
	public final static Pattern posiblleNameToken = Pattern.compile("[a-zA-Z][a-zA-Z][a-zA-Z][a-zA-Z&]+");
	
	public final static Pattern posiblleNameTokenWithFirstAndMiddleInitials = Pattern.compile("[a-zA-Z][a-zA-Z][a-zA-Z][a-zA-Z]+[ \t]+[a-zA-Z][ \t]+[a-zA-Z][ \t]+");
	
	public final static Pattern patLastInitialLastInitial = Pattern.compile("([a-zA-Z][a-zA-Z][a-zA-Z][a-zA-Z]+)([ \t]+[a-zA-Z][ \t]+)([a-zA-Z][a-zA-Z][a-zA-Z][a-zA-Z]+)(([ \t]+[a-zA-Z])+)[ \t]+");
	
	
	/**
	 * Contains expressions found in company names
	 * TODO: put this into a config file and load it at startup
	 */
	  final static String [] companyExpressions = {
			"ABRACADABRA",
			"ACADEMY",
			"ACCEPTANCE",
			"ACCOUNTANTS",
			"ACQ",
            "ACQUISITION",
            "ACREAS",
            "ACT",
            "ACTION HOBBY",
            "ADMINISTRATION",
			"ADMINISTRATOR",
			"ADV",
			"ADVANCE PLATING",
			"ADVERTISING",
			"ADDITION",
			"AFFAIRS", 		
			"AFFILIATES",
			"AGCY",
			"AGENCY",
			"AGFINANCE",
			"AGREEMT",
			"AGREEMENT",
			"AGRIBIZ",
			"AIR",
			"AIRPORT",
			//"ALLEY", this is also a last name//B5184
			"ALTERATION",
			"ALTERCARE",
			"AMERICAS",
			"ANTIQUES",
			"A P I S D",
			"APARTMENTS",
			"APPLIANCES",
			"APPLICATION",
			"APPRAIS",
			"APPRAISAL",
			"APPRAISALS",
			"APPRAISELS",
			"APPT",
			"APTS",
			"AQUISITION",
			"ACQUISITIONS",
			"ARCHITECT",
			"ARCHITECTS",
			"ARCHITECTURE",
			"ARMY",
			"ART",
			"ARTISTS",
			"ASIAN",
			"ASN",
			"ASPHALT",
			"ASSC",
			"ASSET",
			"ASSN",
			"ASSOC",
			"ASSOC.",
			"ASSOCIAT",
			"ASSOCIATE",
			"ASSOCIATES",
			"ASSOCIATION",
			"AT&T",
			"ATLANTIC",
			"ATM",
			"ATTORNEY",
			"ATTORNEYS",
			"ATTY",
			"AUDIO",
			"AUDIOLOGY",
			"AUTO",
			"AUTOMOTIVE",
			"AUTH",
			"AUTHORITY",
			"AVIATION",
			"BAKERY",
			"BANCO",
			"BAND",
			"BANK",
			"BANCORP",
			"BAPT CH",
			"BAR",
			"BARBER",
			"BARCLAYSAMERICAN",
			"BDHK",
		    "BK",
		    "BLACK BEARDS TOO",
		    "BLACK DOG",
		    "BLDG",
		    "BLDRS",		   
			"BOARDING",
			"BONDING",
			"BOWLING",
			"BRANDS",
			"BREAKFAST",
			"BROKERAGE",
			"BROKERS",
			"BROS",
			"BUREAU",
			"BP",
			"BUILDING",
			"BUILDERS",
			"BURGER",
			"BURGERS",
			"BUSINESS",
			"BUSINE",
			"BY",
			//"C/O", this means  Care Of and IS NOT a company expression
			"C P A",
			"CAB",
			"CADILLAC",
			"CAFE",
			"CAMPGROUND",
			"CANALS",
			"CANTINA",
			"CAPITAL",
			"CARE",
			"CARETAKER",
			"CARMA COLORADO",
			"CARPENTRY",
			"CARS",
			"CATHOLIC",
            "CDC",
            "CED", //Committee for Economic Development
            "CELLULAR",
			"CEMETERY",
			"CEN",
			"CENTER",
			"CENTRAL",
			"CERAMIC",
			"CHANGE",
			"CHAPEL",
			"CHARITABLE",
			"CHECK CASHING",
			"CHEVROLET",
			"CHFC",
			"CHRISP",
			"CHURCH",
			"CHURC",
			"CH & KH",
			"CIGARS",
			"CIRCUIT",
			"CITIGROUP",
			"CITY",
			"CITIES",
			"CITIMORTGAGE",
			"CLEANERS",
			"CLEANING",
			"CLINIC",
			"CLU",
			"CLUB",
			"CMMC",
			"CMNTY",
			"CNTR",
			"CO OFF",
			"CO",
			"CO.",
			"COLLEGE-STATE",
			"COLONIAL",
			"COLOURWORKS",
			"COMMERCIAL",
			"COMMERCIAL OPERATIONS",
			"COMMISSION",
			"COMMITTEE",
//			"COMMONS",			//it is a last name
			"COMMUNICATION", 
			"COMMUNICATIONS",
			"COMMUNITY",
			"COMMUNITIES",
			"COMP",
			"COMM",
			"COMPANIES",
			"COMPANY",
			"COMPANYDOUGLAS",
			"COMPUTER",
			"COMPUTERS",
			"CONCEPTS",
			"CONDITIONING",
			"CONDO",
			"CONDOMINIUM",
			"CONDOMINIUMS",
			"CONGREGATION",
			"CONNECTION",
			"CONSERVANCY", 
			"CONSERVATOR",
			"CONSULT",
			"CONSULTANT",
			"CONSULTANTS",
			"CONSULTING",
			"CONSUMER",
			"CONTR",
			"CONTRACT",
			"CONTRACTING",
			"CONTRACTOR",
			"CONTRACTORS",
			"CONTROL",
			"CONST",
			"CONSTRUC",	
			"CONSTRUCT",
			"CONSTRUCTION",
			"CONSTRUCTOR",
			"CONSTRUCTORS",
            "CONSTR",
            "CONST",
            "CONS",
            "CONVENTION",
            "COOP",
			"CORP",
			"CORPO",
			"CORPORATI",
			"CORPORATION",
			"COTTEN",
			"COUNSELING",
			"COUNTY",
			"CPA",
			"CPAS",
			"CREAMERY",
			"CREATIONS",
			"CRED",
			"CRC",
			"CS",
			"CTR",
			"CTR.",
			"CUSTOM",
			"CWALT",  // Countrywide Alternative Loan Trust
			"D D S",
			"D F",
			"DBA",
			"DDS",
			"DEALER",
			//"DEC",
			"DECL",
			"DECLARATION",
			"DECOR",
			"DECKS",
			"DELIVERY",
			"DEPARTMENT",
			"DEPOT",			
			"DEPT",
			"DESIGN",
			"DESIGNERS",
			"DETAIL",
			"DETAILING",
			"DETECTIVE",
			"DETENTION",
			"DEV",
			"DEVELOPMENT",
			"DEVELOPMNT",
			"DIAMOND MARINE",
			"DIOCESE",
			"DIRECTORS",
			"DISCOUNT",
			"DISPENSARY",
			"DISPLAY",
			"DIST",
			"DISTRIBUTING",
			"DISTRIBUTION",
			"DISTRIBUTORS",
			"DISTRICT",
			"DIVISION",
			"DOKTORS",
			"DOLL HOUSE",
			"DONUTS",
			"DOWNTOWN",
			"DRUG",
			"ELECTRIC",
			"ELECTRICAL",
			"ELECTRONIC",
			"ELECTRONICS",
			"EMPLOYMENT",
			"ENDOSCOPY",
			"ENERGY",
			"ENGINEERED",
			"ENGINEERING",
			"ENTERPRIES",
			"ENTERPRISE",
			"ENTERPRISES",
			"ENTERTAINMENT",
			"EQUIPMENT",
			"EQUITY",
			"ESCROW",
			"ESSENTIALS",
			"EST",
			"ESTATE",
			"ESTATES",
			"ESTHETICS",
			"ETC",
			"EURO",
			"EVANGELICAL",
			"EVERBANK",
			"EXCAVATION",
			"EXCHANGE",
			"EXECUTOR ",
			"EXPERT",
			"EXPLORATION",
			"EXPRESS",
			"EXPRESSO",
			"EXTERIOR",
			"F H A",
			"FABRICATION",
			"FABRICATIONS",
			"FAM TR",
			"FAM.LTD.PTNERSP.",
			"FAMILY",
			"FAMILY LMTD PARTNRSHP",
			"FAMILY TRUST",
			"FAMILYTRUST",
			"FARGO",
			"FARM",
			"FARMS",
			"FASHIONS",
			"FEDERAL",
			"FED",
			"FEED MILL",
			"FELLOWSHIP",
			"FIDELITY",
			"FINANCE",
			"FINANCI",
			"FINANCIAL",
			"FIRE",
			"FIRM",		
			//is a pretty common last name (#1377)
			/*"FISH",*/
			"FITNESS",
			"FLA",
			"FLOOR",
			"FLORIST",
			"FNB",	//FIRST NATIONAL BANK
			"FOOD",
			"FOODS",
			"FORMS",
			"FOUNDATION",
			"FUND",
			"FUNDERS",
			"FUNDS",
			"FUNERAL",
			"FURNITURE",
			"G. P.",
			"G.P.",
			"GALLERIES",
			"GALLERY",
			"GARAGE",
			"GARAGES",
			"GARDENS",
			"GAS",
			"GATHERING",
			"GO",
			"GOLF",
			"GORD",
			"GOVERNMENT",
			"GRAYTON",
			"GRAPHICS",
			"GREATER",
			"GRILL",
			"GROUP",
			"GULF",
			"HABITAT",
			"HARDWARE",
			"HEATING",
			"HLDGS",
			"HNW",
			"HOA",
			"HOLDINGS",
			"HOME",
			"HOMEOWNERS",
			"HOMES",
			"HOMESLOANS",
			"HOMEPLACE",
			"HOLINESS",
			"HOSPITAL",
			"HOT DOG",
			"HOT DOGS",
			"HOUSE",
			"HOUSING",
			"HUD",
			"I R S",
			"IDENTIFICATION",
			"ILLUSTRATION",
			"ILLUSTRATIONS",
			"IMAGE",
			"IMPORTS",
			"IMPROVEMENTS",
			"INC",
			"INC.",
			"INCDENVER",
			"INCORPORATED",
			"IND",
			"INDEPENDENT",
			"INDIANS",
			"INDIANTOWN",
			"INITIALLY",
			"INDUST",
			"INDUSTRIAL",
			"INDUSTRIES",
			"INLAND",
			"INN",
			"INNOVATIONS",
			"INS",
			"INST.",
			"INSTALLATION",
			"INSTALLATIONS",
			"INSTITUTE",
			"INSULATION",
			"INSURANCE",
			"INTERIOR",
			"INTERIORS",	
			"INT'L",
			"INTERNATIONAL",
			"INV",
			"INVST",
			"INVESTMENTS",
			"INVESTIGATIONS",
			"INVESTMENT",
			"INVESTMENT(S)",
			"INVESTORS",
			"IRREVOC",
			"IRREVOCABLE",
			"IRVCBL",
			"ISD",
			"ITF",
			"JOINT VENTURE",
			"KARATE",
			"KNESETH",
			"L P",
			"L.P.",
			"LAB",
			"LA MAISON",
			"JANITORIAL",
			"LABORATORY",
			"LAND",
			"LANDS",
			"LANDSCAPE",
			"LANDSCAPING",
			"LAUNDRY",
			"LAWN CUTTING",
			"LAWN",
			"LAWNS",
			"LC",
			"LE",
			"LEASING",
			"LENDING",
			"L/E",
			"LF/ES",
			"LF EST",
			"(LE)",
			"LIFE",
			"LIFE ESTATE",
			"LIMITED",
			"LIQUOR",
			"LIV TR",
			"LIVING",
			"LIVINGTRUST",
			"LL",
			"L L C",
			"LLC",
			"LLC.",
			"L.L.C",
			"L.L.C.",
			"LLP",
			"LLLP",
			"LLCAND",
			"L/T",
			"LT",
			"L/E",
			"LND",
			"LOAN",
			"LOANS",
			"LODGE",
			"LOT",
			"LOTS",
			"LP",
			"LTD",
			"LTM",
			"LMT",
			"M E R S",
			"M.D",
			"MAINT",
			"MAGAZINE",
			"MAINTENANCE",
			"MALL",
			"MANAGEMENT",
			"MASONRY",
			"MASSAGE",
			"MGMT",
			"MGT",
			"MAKING",
			"MANUFACTURING",
			"MARBLE",
			"MARKET",
			"MART",
			"MD",
			"MEATS",
			"MECHANICAL",
			"MEDICAL",
			"MEDICINALS",
			"MEMORIAL",
			"MERCHANDISE",
			"MERS",
			"METAL",
			"METHODIST",
			"MHP",
			"MILLWORK",
			"MINERAL",
			"MINERALS",
			"MINISTRIES",
			"MIP",
			"MISSOURI",
			"MISSIONARY",
			"MKT",
			"MGMNT",
			"MORTG",
			"MORTGAG",
			"MORTGAGE",
			"MORTUARIES",
			"MORTUARY",
			"MOTEL",
			"MOTORS",
			"MOTORSPORTS",
			"MRKT",
			"MTG",
			"MUSEUM",
			"MUSIC",
			"MUTUAL",
			"N. A.",
			"N.A.",
			"NA",
			"NATIONAL",
			"NATIONSBANK",
			"NATURE",
		    "NATLBK",
		    "NEWS MEDIA",
			"NURSERY",
			"OF",
			"OFFICE",
			"OFFICES",
			"OIL",
			"OLDSMOB",
			"ONE",
			"OPERATIONAL",
			"OPTICIANS",
			"OPTOMERTRIST",
			"ORG",
			"ORGANIZATION",
			"OUTDOORS",
			"OUTFITTERS",
			"OUTLET",
			"OWNERS",
			"P C",
			"P.C",
			"PA",
			//"P A", can't be considered a company expression: SMITH B J & P A
			"P.A",
			"P.A.",
			"PAINT",
			"PAINTI",
			"PAINTING",
			"PALM BEACH",
			"PARK",
			"PARKING",
			"PART",
			"PART.",
			"PARTNER",
			"PARTNERS",
			"PARTNERS I",
			"PARTNERSHIP",
			"PARTNRSHP",
			"PAPERS",
			//"PAT",  this is female first name
			"PC",
			"PEDIATRICS",
			"PERFORMANCE",
			"PENTECOSTAL",
			"PHARMACY",
			"PHOTO",
			"PHOTOGRAPHY",
			"PI",
			"PIZZA",
			"PIT",
			"PL",//PLACE on Collin ACS
			"PLANTATION",
			"PLC",
			"PLLC",
			"PLOMARITY",
			"PLUMBING",
			"PLUMBNG",
			"PLMBNG",
			"POA",
			"POLKA DOTZ",
			"POPULAR",
			"PPT",
			"PRECISION",
			"PRESCHOOL",
			"PRODUCTION",
			"PRODUCTIONS",
			"PRODUCTS",
			"PROFESSIONAL",
			"PROFIT",
			"PROJECT",
			"PROMART",
			"PROP",
			"PROPERTIES",
			"PROPERTIE",
			"PROPERTY",
			"PROTECTION",
			"PTN",
			"PRTNRSHIP",
			"PRTNSHP",
			"PTNRS",
			"PTNRSHIP.",
			"PTNRSHP",
			"PTNSHP",
			"PTNERS",
			"PTS", //Post Trade Services
			"PUBLIC",
			"PURCHASE",
			"QPRT",
			"QTIP",
			"R.C",
			"RADIO",
			"RANCH",
			"RANCHES",
			"RASC",
			"REAL ESTATE",
			"REALTOR",
			"REALTORS",
			"REALITY",
			"REALTY",
			"REC",
			"RECOVERY",
			"REDEVELOPMENT",
			"REGENTS UNIV",
			"REHAB",
			"RENOVATORS",
			"RENT-A-CAR",
			"RENTAL",
			"RENTALS",			
			"REPAIR",
			"REPAIRS",
			"RES",
			"RESERVED",
			"RESIDENCE",
			"RESEARCH",
			"RESIDENTIAL",
			"RESIDUARY",
			"RESORT",
			"RESOURCE",
			"RESOURCES",
			"REGENCY",
			"RESTAURANT",
			"RESTAURANTS",
			"RETAIL",
			"RETREAT",
			"REV",
			"REV LIV TR",
			"RETIREMENT",
			"REVOC",
			"REVOCABLE",
			"RISTORANTE",
			"ROOFING",
			"ROYALTIES",
			"FNMA",
			"FNM",
			"FANNIE MAE",
			"FREDIE MAC",
			"FREDDIE MAC",
			"R/T",
			"S&L",//savings and loan association
			"S & L",//savings and loan association
			"SAFETY",
			"SALAD",
			"SALE",
			"SALES",
			"SALO",
			"SALON",
			"SALOON",
			"SANITARY",
			"SAV",
			"SAVINGS",
			"SAW MILL",
			"SCHOOL",
			"SECTION",
			"SEDONA",
			//"SELF",this is a middle name
			"SELLERS",
			"SERV",
			"SERVICE",
			"SERVICES",
			"SERVICING",
			"SEMINARS",
			"SHARING",
			"SHUTTLE",
			"SHOP",
			"SHOPPE",
			"SHOWCASE",
			"SHOWROOM",
			"SITE",
			"SN",
			"SOC",
			"SOCIETY",
			"SOLUTION",
			"SOLUTIONS",
			"SONS",
			"SOUTH",
			"SOUTHEAST",
			"SOUTHERN",
			"SPA",
			"SPACE",
			"SPEC",
			"SPECIALIST",
			"SPECIALISTS",
			"SPECIALITY",
			"SPECIALTIES",
			"SPRINKLERS",
			"SPTC",
			"SPV",//Special Purpose Vehicle
			"SQUARE",
			"SRVPCORP",
			"ST",
			"ST TROPEZ",
			"ST_LLC",
			"STATE",
			"STATION",
			"STONEWORKS",
			"STORA",
			"STORAGE",	
			"STORE",
			"STREETS",
			"STUDIO",
			"STUDIOS",
			"STYLE",
			"SUBD",
			"SUBS",
			"SUBDIV",
			"SUBDIVISION",
			"SUBMERGED",
			"SUITE",
			"SUITES",
			"SUNBANK",
			"SUPERVISORS",
			"SUPP",
			"SUPPER",
			"SUPPERS",
			"SUPPLIER",
			"SUPPLIERS",
			"SUPPLIES",
			"SUPPLY",
			"SUPPORT",
			"SVC",
			"SVCS",
			"SYS",
			"SYSTEM",
			"SYSTEMS",
			"TAQUERIA",
			"TATTOO",
			"TATTOOS",
			"TAX",
			"TAVERN",
			"TECH",
			"TECHLINE",
			"TECHNOLOGIES",
			"TECHNOLOGY",
			"TELECOMMUNICATIONS",
			"TELEPHONE",
			"TELEVISION",
			"TEMPLE",
			"TESTING",
			"THE",
			"THERAPEUTIC",
			"THERAPY",
			"THROUGH",
			"TIC",
			"TIITF",
			"TITLE",
			"TOWN",
			"TOWNSHIP",
			"TOPSAIL",
			"TRADE",
			"TRADES",
			"TRADING",
			"TRADITIONS",
			"TRAIL",
			"TRANSMISSION",
			"TRANSPORT",
			"TRANSPORTATION",
			"TRAVEL",
			"TREASURE",
			"TREAT",
			"TREATS",
			"TRIM",
			"TROVE",//perhaps just temporary
			"TRUCK",
			"TRUCKING",
			"TRUCKS",
			//"TR",
			//"TRS",
			"TRST",
			//"TRSTE",
			"TRT",
			"TRUST",
			"TRUSTS",
			//"TRUSTEE",
			//"TRUSTEE(S)",
			"TRUT",
			//"TTEES",
			"TRU",
			"TRUS",
			"U S A",
			"UNION",
			"UNIT",
			"UNITED",
			"UNITRUST",
			"UNIV",
			"UNIVERSITY",
			"UNLIMITED",
			"UP",
			"UPHOLSTERY",
			"UT",
			"UTOPIA",
			"VA REO",
			"VACATION",
			"VALLEY",
			"VEHICLE",
			"VENTURE",
			"VETERANS",
			"VIDEO",
			"VILLAGE",
			"VISTA",
			"VLG",
			"VOCABLE",
			"VOLUNTEER",
			"WALLCOVERING",
			"WAREHOUSE",
			"WATER",
			"WAREHOUSES",
			"WHIMSICAL",
			"WHOLESALE",
			"WHOLESALERS",
			"WIGS",
			"WILDLIFE",
			"WIRELESS",
			"WOODWORKING",
			"WOOD WORKS",
			"WORK",
			"WORLD",
			"YELLOW PGS",
			"YOUR",
            /*Sufixe transmise CM*/
            "L L C",
            "CREDIT",
            "LP",
            "RW",
            "Railway",
            "RWY",
            "PLG",
            "LOVING CARE",
            "T V A",
            "MPHS",
            "L P"
	};
	
	/**
	 * Precompiled patterns used for finding company expressions that have multiple words inside
	 */
	 static List<Pattern> companyPatterns = new ArrayList<Pattern>();
	
	/**
	 * Hash set with all company expressions containing a single word
	 */
	 static Set<String> companyWords = new HashSet<String>();
	
	/**
	 * Pattern used for splitting a string into words
	 */
	 static Pattern splitPattern = Pattern.compile("[^0-9a-zA-Z_\\.]+");
	
	/**
	 * Preprocess the list of company expressions. Put the single words into a hash and multiple
	 * words into a list of compiled Patterns. 
	 */
	static{
		//int expr =0;
		for(int i=0; i<companyExpressions.length; i++){
			String expression = companyExpressions[i];
			String [] words = splitPattern.split(expression);
			if(words.length == 1){
				companyWords.add(words[0]);
			}else{
				companyPatterns.add(Pattern.compile("(?i)\\b"+expression+"\\b"));
				//expr++;
			}
		}
		
	}

	/**
	 * Decides if a string is a company name
	 * @param s the input string
	 * @return true if it is a company name
	 */
	 static boolean isCompanyString(String s, Vector<String> exclude) {               
		s = s.toUpperCase();

		if(s.matches("(PARK|SELLERS|CHURCH),? (?!(OF|WALNUT|DEVELOPMENT)\\b)[A-Z]+ [A-Z]+") || s.matches("[A-Z'-]{2,},? PARK [A-Z]( & [A-Z]+.*)?")) {
        	return false;
        }
		
		if (s.matches("\\w+\\s\\w{1} CHURCH.*")){
			return false;
		}
		
		if(s.matches("(PARK)")) { // TNKnoxAO: PID 144FB015; PARK, YONG U & IN S - it's not a company 
        	return false;
        }
		
		String[] s1 = s.split("&"); // FLAlachua NB,PRI,TR: PID 06507013005 --> WILSON & WILSON JR - it's a company
		if (s1.length > 1) {
			boolean company = true;
			for (int index = 0; index < s1.length; index++) {
				if (!NameFactory.getInstance().isLast(s1[index].replaceFirst("(?is)\\s+\\b(SR|JR)\\b", " "))) {
					company = false;
					break;
				}
			}
			if (company) {
				return true;
			}
		}
		
        String [] words = splitPattern.split(s);
        for(int i=0; i<words.length; i++){
        	if(!exclude.contains(words[i]) && companyWords.contains(words[i]))
        		return true;
        }
        for(int i=0; i<companyPatterns.size(); i++){
        	if(!exclude.contains(companyPatterns.get(i).toString()) && companyPatterns.get(i).matcher(s).find())
        		return true;
        }
        return false;
	}
	
	 static boolean isCompanyString(String s){
		return isCompanyString(s, new Vector<String>());
	}

	/**
	 * Decides if a string is a company name
	 * Besides searching for certain words, it also searches for "*" 
	 * character. If found it considers the string as company name
	 * @param s the input string
	 * @return true if it is a company name
	 */
	public static boolean isCompany(String s) {
		Boolean ce = companyException(s);
		if (ce != null){
			return ce;
		} else {
			return isCompanyString(s);
		}
	}
	
	public static boolean isNotCompany(String s){
		return !isCompany(s);
	}
	/**
	 * check if company 
	 * @param s - string to be tested
	 * @param exclude - ignored words from the big list
	 * @param lastLastCompany - false if last & last is not company name (used for alachua)
	 * @return
	 */
	public static boolean isCompany(String s, Vector<String> exclude, boolean lastLastCompany) {
		//lastName1&lastName2 is always a company name
		if (lastLastCompany){
			String[] s1 = s.split("&");
			if (s1.length > 1){
				boolean company = true;
				for (int index = 0; index<s1.length; index++){ 
					if (!NameFactory.getInstance().isLast(s1[index])){
						company = false;
						break;
					}
				}
				if (company) {	
					return true;
				}
			}
		}
		Boolean ce = companyException(s);
		if (ce != null){
			return ce;
		} else {
			return isCompanyString(s, exclude);
		}
	}
		
	
	public static Boolean companyException(String s){
        if (s.indexOf('*')>=0)
            return true;        
        if (s.matches(".+'S(\\s.+|$)"))
        	return true;
        if (s.matches(".+\\.COM\\b.*"))
        	return true;
        if (s.matches("\\b.*LANDSCAPE\\b"))  //GRASS ROOTS LANDSCAPE  -> FL Collier
        	return true;
        if (s.matches("(?i)\\AST\\s+\\w+$"))			// fix for bug #5567, ST TROPEZ 
        	return true;
        
        if (s.matches("(?i)ST\\.? .+") && !s.contains("CHURCH") &&  !s.contains("CHAPEL"))			// fix for bug #2573 
        	return false;
        if (s.matches(".+, .+ MD$"))		// BROWN, THOMAS D MD
        	return false;
        if (s.matches("(?i).+ PAT( [A-Z]+)?"))	// SMITH, PAT
        	return false;
        if (s.matches("(?i)[A-Z-]+ PAT( [A-Z])?"))	// SMITH PAT
        	return false;
        if (s.matches("(?i)PAT( [A-Z])? [A-Z'-]{2,}"))	// PAT WEBER
        	return false;
        if (s.matches("D F(\\s*&| [A-Z]).+"))	//D F & STEPHANIE F SMITH
        	return false;
        if (s.matches("(?i)(BOWLING|SALE|SALO|TEMPLE),? [A-Z]+( [A-Z])?( &.+)?") || s.matches("(?i)[A-Z]+( [A-Z]+)? BOWLING"))
        	return false;
        if (s.matches("(.+),(.+)\\s*@\\s*(.+)"))
        	return true;
        
        if(s.matches("(PARK), [A-Z]+ [A-Z]+ & [A-Z]+ [A-Z]+")) { // TNKnoxAO: PID 144FB015; PARK, YONG U & IN S - it's not a company 
        	return false;
        }
        
        if(s.matches("\\A\\w+\\s+\\w+\\s+&\\s+\\d+.*")) { // COBoulderTR: PID R0020675
        	return false;
        }
        if(s.matches("(?is).*\\s+TRUST\\s*&\\s+\\w+\\s+\\w+")) { // COBoulderTR: PID R0053202
        	return false;
        }
        if(s.matches("(?is)DR\\s+[A-Z]+")) {
        	return true;
        }
        if(s.trim().matches("(?is)[A-Z]+\\s*&\\s*[A-Z]+(\\s*&\\s*[A-Z]+)?\\s+DRS")) {//LUDWIG & FIELDER & BEVANS DRS : PulaskiTR
        	return true;
        }
        if(s.trim().matches("(?is)BARBER\\s+[A-Z]+(\\s+[A-Z])?")) {//BARBER OLIVER W	  
        	return false;
        }
        if(s.trim().matches("(?is)[A-Z]+\\s+([A-Z]+|[A-Z]\\.?)\\s+BARBER(\\s+JR\\.?|SR\\.?|II|III|IV)?")) {//OLIVER WARREN BARBER JR., LEROY G. BARBER	  
        	return false;
        }
        
        Pattern compile = Pattern.compile("(?is)\\bINC\\b");
		Matcher matcher = compile.matcher(s);
		boolean matches = matcher.find();//
        if(matches) {//Il MchenryAO 10-32-476-021;  PRESTON-ROSS, ADAFINCSFG INC
        	return true;
        }
        
        //FLLeonTR BESTBUY STORES #043500  SUPER LUBE #14 b5053
        compile = Pattern.compile("(?is)[A-Z]+\\s+(#|POST\\s*)\\d+\\b");
		matcher = compile.matcher(s);
		matches = matcher.find();//
        if (matches){
        	return true;
        }
        	
        
        //the previous rules didn't match
        return null;
	}
	
	/**
	 * Decides if a NameTokenList is a company name
	 * @param ntl the input NameTokenList
	 * @return true if it is a company name
	 */
	public static boolean isCompany(NameTokenList ntl) {
		String s = ntl.getFirstNameAsString() + " " + ntl.getMiddleNameAsString() + " " + ntl.getLastNameAsString();
		return isCompanyString(s);
	}

	public static String[] getCompanyExpressions() {
		return companyExpressions;
	}

	
	public static boolean isInCompanyList(String temp){
		boolean  testBreak = false;
		for(int i=0;i<companyExpressions.length;i++){
			   if(temp.equalsIgnoreCase(companyExpressions[i])){
				   testBreak = true;
				   break;
			   }
		}
		return testBreak;
	}
	
	/**
	 * Check if a string contains on words from companywords
	 * used, for example, to merge long company names, that are split on 2 rows
	 * @param temp the computed string
	 * @param extra county dependent words
	 * @return
	 */
	public static boolean isCompanyNamesOnly(String temp, Vector<String> extra){
		String[] words = splitPattern.split(temp);
		for (String s: words){
			s = s.toUpperCase();
			if (!companyWords.contains(s) && !extra.contains(s)){
				return false;
			}
		}
		return true;
	}
}
