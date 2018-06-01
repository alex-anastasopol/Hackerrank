package ro.cst.tsearch.servers.functions;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Vector;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang.ArrayUtils;
import org.htmlparser.Text;
import org.htmlparser.filters.HasAttributeFilter;
import org.htmlparser.filters.TagNameFilter;
import org.htmlparser.tags.CompositeTag;
import org.htmlparser.tags.TableColumn;
import org.htmlparser.tags.TableRow;
import org.htmlparser.tags.TableTag;
import org.htmlparser.util.NodeList;

import ro.cst.tsearch.extractor.legal.LegalDescription;
import ro.cst.tsearch.extractor.xml.GenericFunctions;
import ro.cst.tsearch.extractor.xml.GenericFunctions2;
import ro.cst.tsearch.extractor.xml.ResultMap;
import ro.cst.tsearch.extractor.xml.ResultTable;
import ro.cst.tsearch.extractor.xml.StringFormats;
import ro.cst.tsearch.parser.HtmlParser3;
import ro.cst.tsearch.search.name.LastNameUtils;
import ro.cst.tsearch.search.name.NameUtils;
import ro.cst.tsearch.servers.response.OtherInformationSet.OtherInformationSetKey;
import ro.cst.tsearch.servers.response.PropertyAppraisalSet.PropertyAppraisalSetKey;
import ro.cst.tsearch.servers.response.PropertyIdentificationSet.PropertyIdentificationSetKey;
import ro.cst.tsearch.servers.response.SaleDataSet.SaleDataSetKey;
import ro.cst.tsearch.servers.response.TaxHistorySet.TaxHistorySetKey;
import ro.cst.tsearch.utils.CountyCities;
import ro.cst.tsearch.utils.Roman;
import ro.cst.tsearch.utils.StringUtils;

/**
 * @author mihaib
 */

public class TXGenericTaxNetAO {

	
	public static final String GEO_KEY = "GEO";
	public static final String GEO_PATTERN = "<b>\\s*GEO\\s*:?\\s*</b>([^<]*)";
	
	public static final String GEO_NUMBER_KEY = "GEO Number";
	public static final String GEO_NUMBER_PATTERN = "<b>\\s*GEO Number\\s*:?\\s*</b>([^<]*)";
	
	public static final String LONG_ACCOUNT_NUMBER_KEY = "Long Account Number";
	public static final String LONG_ACCOUNT_NUMBER_PATTERN = "<b>\\s*Long Account Number\\s*:?\\s*</b>([^<]*)";
	
	public static final String ACCOUNT_NUMBER_KEY = "Account Number";
	public static final String ACCOUNT_NUMBER_PATTERN = "<b>\\s*Account Number\\s*:?\\s*</b>([^<]*)";
	
	public static final String ACCOUNT_NO_KEY = "Account No.";
	public static final String ACCOUNT_NO_PATTERN = "<b>\\s*Account No.\\s*:\\s*</b>([^<]*)";
	
	public static final String PIDN_KEY = "PIDN";
	public static final String PIDN_PATTERN = "<b>\\s*PIDN\\s*:?\\s*</b>([^<]*)";
	
	
	// list of 120 counties atascosa like form and results
	public static String[]		ATASCOSA_121		= { "andrews", "angelina", "atascosa",
													"bandera", "bell", "bexar", "blanco", "brazoria", "brazos",
													"brewster", "brooks", "brown", "burnet", "caldwell", "calhoun",
													"callahan", "cameron", "camp", "cass", "cherokee", "collin",
													"colorado", "comal", "comanche", "cooke", "coryell", "dallam",
													"deafsmith", "delta", "denton", "dimmit", "duval", "edwards",
													"elpaso", "ellis", "erath", "fannin", "fayette", "floyd",
													"gillespie", "grayson", "gregg", "guadalupe", "hale", "hamilton",
													"harrison", "hartley", "haskell", "henderson", "hidalgo", "hill",
													"hockley", "hood", "hopkins", "hudspeth", "hunt", "kaufman",
													"kendall", "kenedy", "kerr", "kimble", "kinney", "kleberg", "knox",
													"lasalle", "lamar", "lamb", "lavaca", "lee", "liberty", "lipscomb",
													"llano", "madison", "matagorda", "maverick", "mclennan",
													"mcmullen", "midland", "milam", "mills", "montague", "moore",
													"morris", "navarro", "parker", "parmer", "polk", "rains", "real",
													"rockwall", "runnels", "sanjacinto", "schleicher", "scurry",
													"shackelford", "shelby", "somervell", "stephens", "sterling",
													"sutton", "swisher", "taylor", "terrell", "titus", "travis",
													"upshur", "upton", "uvalde", "valverde", "vanzandt", "victoria",
													"walker", "webb", "wichita", "willacy", "wilson", "winkler",
													"wise", "wood", "yoakum", "zapata" };

	public static List<String>	ATASCOSA_121_LIST	= Arrays.asList(ro.cst.tsearch.servers.functions.TXGenericTaxNetAO.ATASCOSA_121);

	// list of 55 sites armstrong like
	public static String[]		ARMSTRONG_58		= { "armstrong", "bee", "borden",
													"burleson", "carson", "chambers", "childress", "concho", "cottle",
													"culberson", "dawson", "dewitt", "dickens", "donley", "falls",
													"foard", "franklin", "freestone", "frio", "goliad", "hall",
													"hansford", "hemphill", "houston", "hutchinson", "irion",
													"jeffdavis", "jimhogg", "jones", "karnes", "kent", "king",
													"lampasas", "leon", "loving", "lynn", "marion", "mason",
													"mcculloch", "menard", "nacogdoches", "nolan", "palopinto",
													"panola", "pecos", "reagan", "reeves", "refugio", "rusk",
													"sansaba", "sherman", "starr", "wheeler", "wilbarger", "young",
													"gonzales", "sanpatricio", "coke"
													};

	public static List<String>	ARMSTRONG_58_LIST	= Arrays.asList(ro.cst.tsearch.servers.functions.TXGenericTaxNetAO.ARMSTRONG_58);

	// list of 16 sites aransas like
	public static String[]		ARANSAS_15			= { "aransas", "bastrop", "fortbend",
													"galveston", "grimes", "hays", "jackson", "limestone", "lubbock",
													"medina", "montgomery", "newton", "orange", 
													/*"sanpatricio",*/
													"washington", "williamson" };

	public static List<String>	ARANSAS_15_LIST		= Arrays.asList(ro.cst.tsearch.servers.functions.TXGenericTaxNetAO.ARANSAS_15);

	// archer like 20 sites
	public static String[]		ARCHER_19			= { "archer", "baylor", "castro", "clay",
													"collingsworth", "crane", "crockett", "eastland", "fisher",
													"hardeman", "jack", "martin", "mitchell", "presidio",
													"sabine", "sanaugustine", "stonewall", "throckmorton", "ward" };

	public static List<String>	ARCHER_19_LIST		= Arrays.asList(ro.cst.tsearch.servers.functions.TXGenericTaxNetAO.ARCHER_19);

	// bowie like 12 sites
	public static String[]		BOWIE_12			= { "bowie", "cochran", "crosby", "garza",
													"glasscock", "hardin", "howard", "jasper", "liveoak", "redriver",
													"robertson", "terry" };

	public static List<String>	BOWIE_12_LIST		= Arrays.asList(ro.cst.tsearch.servers.functions.TXGenericTaxNetAO.BOWIE_12);

	// anderson like 11 sites
	public static String[]		ANDERSON_11			= { "anderson", "austin", "bosque",
													"coleman", "ector", "johnson", "smith", "trinity", "tyler",
													"waller", "wharton" };

	public static List<String>	ANDERSON_11_LIST	= Arrays.asList(ro.cst.tsearch.servers.functions.TXGenericTaxNetAO.ANDERSON_11);

	// briscoe like 6 sites
	public static String[]		BRISCOE_5			= { "briscoe", "gray", "motley", "oldham", "roberts" };

	public static List<String>	BRISCOE_5_LIST		= Arrays.asList(ro.cst.tsearch.servers.functions.TXGenericTaxNetAO.BRISCOE_5);

	// dallas like 3 sites
	public static String[]		DALLAS_3			= { "dallas", "jimwells", "tomgreen" };

	public static List<String>	DALLAS_3_LIST		= Arrays.asList(ro.cst.tsearch.servers.functions.TXGenericTaxNetAO.DALLAS_3);

	// potter like 5 sites
	public static String[]		POTTER_5			= { "potter", "randall", "zavala", "jefferson", "harris" };

	public static List<String>	POTTER_5_LIST		= Arrays.asList(ro.cst.tsearch.servers.functions.TXGenericTaxNetAO.POTTER_5);

	// bailey like 2 sites
	public static String[]		BAILEY_2			= { "bailey", "gaines" };

	public static List<String>	BAILEY_2_LIST		= Arrays.asList(ro.cst.tsearch.servers.functions.TXGenericTaxNetAO.BAILEY_2);

	public static TableTag getTableFromNodeList(NodeList nodes, String[] header) {
		TableTag t = null;
		nodes = nodes
				.extractAllNodesThatMatch(new TagNameFilter("table"), true);
		for (int i = 0; i < nodes.size(); i++) {
			boolean is = true;
			for (String s : header) {
				if (!nodes.elementAt(i).toHtml().contains(s))
					is = false;
			}
			if (is)
				t = (TableTag) nodes.elementAt(i);
		}
		return t;
	}

	private static void parseAddressTXAndersonAO(ResultMap m, String county,
			long searchId) throws Exception {
		String[] anderson_towns = { "Palestine", "Elkhart", "Frankston" };
		String[] austin_towns = { "Bellville", "Brazos Country", "Industry",
				"Sealy", "Wallis", "San Felipe", "Bleiblerville", "Cat Spring",
				"Kenney", "New Ulm", "Shelby" };

		String[] wharton_towns = { "East Bernard", "El Campo", "Wharton",
				"Boling-Iago", "Hungerford", "Louise", "Bonus", "Newgulf",
				"Glen Flora", "Egypt", "Hillje", "Lane City", "Lissie",
				"Pierce", "Spanish Camp" };

		String[] towns = (String[]) ArrayUtils.addAll(anderson_towns,
				austin_towns);
		towns = (String[]) ArrayUtils.addAll(towns, wharton_towns);

		String address = (String) m.get("tmpAddress");
		if (StringUtils.isEmpty(address))
			return;

		for (String s : towns)
			if (address.toUpperCase().contains(s.toUpperCase())) {
				m.put(PropertyIdentificationSetKey.CITY.getKeyName(),
						s.toUpperCase());
				address = address.toUpperCase().replace(s.toUpperCase(), "");
				break;
			}

		address = address.replaceAll("(?ism)\\([^\\(]*\\)", "");
		address = address.replaceAll("(?ism)N/A", "");
		address = org.apache.commons.lang.StringUtils.strip(address.replaceAll(
				"\\s+", " "));

		m.put(PropertyIdentificationSetKey.ADDRESS_ON_SERVER.getKeyName(),
				address);

		if (address.contains("ACR")
				&& address.matches("(?ism)\\d*\\s*\\w+\\s+\\d+")) {
			String[] parts = address.split(" ");
			if (parts.length == 2) {
				m.put(PropertyIdentificationSetKey.STREET_NAME.getKeyName(),
						parts[0] + " " + parts[1]);
			} else if (parts.length == 3) {
				m.put(PropertyIdentificationSetKey.STREET_NAME.getKeyName(),
						parts[1] + " " + parts[2]);
				m.put(PropertyIdentificationSetKey.STREET_NO.getKeyName(),
						parts[0]);
			}
			return;
		}

		m.put(PropertyIdentificationSetKey.STREET_NAME.getKeyName(),
				StringFormats.StreetName(address));
		m.put(PropertyIdentificationSetKey.STREET_NO.getKeyName(),
				StringFormats.StreetNo(address));
	}

	private static void parseAddressTXAransasAO(ResultMap m, String county,
			long searchId) {
		String address = (String) m.get("tmpAddress");

		if (address == null)
			return;

		String[] parts = address.split(",");

		m.put(PropertyIdentificationSetKey.ADDRESS_ON_SERVER.getKeyName(),
				parts[0]);

		m.put(PropertyIdentificationSetKey.STREET_NAME.getKeyName(),
				StringFormats.StreetName(parts[0]));
		m.put(PropertyIdentificationSetKey.STREET_NO.getKeyName(),
				StringFormats.StreetNo(parts[0]));

	}

	private static void parseAddressTXArcherAO(ResultMap m, String county,
			long searchId) throws Exception {
		parseAddressTXGenericTaxNetAO(m, county, searchId);
	}

	private static void parseAddressTXArmstrongAO(ResultMap m, String county,
			long searchId) {
		String address = (String) m.get("tmpAddress");

		if (address == null || StringUtils.isEmpty(address))
			return;

		if (address.contains("@"))
			return;

		String[] parts = address.split("(?ism)<br>");

		if (parts.length == 0)
			return;

		parts[0] = parts[0].replaceAll("[.,]", "");

		m.put(PropertyIdentificationSetKey.ADDRESS_ON_SERVER.getKeyName(),
				parts[0]);

		if (org.apache.commons.lang.StringUtils.strip(parts[0]).matches("\\d+")) {
			m.put(PropertyIdentificationSetKey.SUBDIVISION_UNIT.getKeyName(),
					parts[0]);
		} else {
			m.put(PropertyIdentificationSetKey.STREET_NAME.getKeyName(),
					StringFormats.StreetName(parts[0]));
			m.put(PropertyIdentificationSetKey.STREET_NO.getKeyName(),
					StringFormats.StreetNo(parts[0]));
		}

	}

	public static void parseAddressTXAtascosaAO(ResultMap m, String county,
			long searchId) throws Exception {

		String address = (String) m.get("tmpAddress");

		if (StringUtils.isEmpty(address))
			return;

		address = address.replaceAll("N/A", "");
		address = address.replaceAll("(?ism)<br>", " ").replaceAll("\\s+", " ");
		address = org.apache.commons.lang.StringUtils.strip(address);

		if (address.equals("0"))
			return;

		if (StringUtils.isEmpty(address))
			return;

		if (address.matches("\\d+"))
			return;

		address = address.replaceAll("\\b(OFF )?TANK HOLLOW?\\b", "");
		address = address.replace("\\sS/S\\sGA", "");
		address = address.replace(",", "");
		String[] cities = CountyCities.TX_ATASCOSA;

		String city = "";
		for (int i = 0; i < cities.length; i++) {
			if (address.toLowerCase().contains(cities[i].toLowerCase())) {
				city = cities[i];
				m.put("PropertyIdentificationSet.City", city);
				address = address.replaceAll("(?is)" + city + ".*", "").trim();
				address = address.replaceAll("(\\d)-([A-Z])", "$1$2");
				break;
			}
		}

		m.put(PropertyIdentificationSetKey.STREET_NAME.getKeyName(),
				StringFormats.StreetName(address));
		m.put(PropertyIdentificationSetKey.STREET_NO.getKeyName(),
				StringFormats.StreetNo(address));

	}

	private static void parseAddressTXBaileyAO(ResultMap m, String county,
			long searchId) throws Exception {
		if (county.equals("gaines")) {
			return;
		}
		parseAddressTXGenericTaxNetAO(m, county, searchId);
	}

	private static void parseAddressTXBowieAO(ResultMap m, String county,
			long searchId) throws Exception {
		parseAddressTXGenericTaxNetAO(m, county, searchId);
	}

	private static void parseAddressTXBriscoeAO(ResultMap m, String county,
			long searchId) throws Exception {

		String address = (String) m.get("tmpAddress");
		if (StringUtils.isEmpty(address)
				|| address.replaceAll("[^\\w]", "").matches("\\d+"))
			return;

		parseAddressTXGenericTaxNetAO(m, county, searchId);
	}

	private static void parseAddressTXDallasAO(ResultMap m, String county,
			long searchId) throws Exception {
		parseAddressTXGenericTaxNetAO(m, county, searchId);
	}

	public static void parseAddressTXGenericTaxNetAO(ResultMap m,
			String county, long searchId) throws Exception {
		if (county.equalsIgnoreCase("ATASCOSA")) {
			parseAddressTXAtascosaAO(m, county, searchId);
			return;
		}

		String address = (String) m.get("tmpAddress");

		if (StringUtils.isEmpty(address) || address.matches("\\d+"))
			return;

		address = address.split("(?ism)<br>")[0];
		address = address.split(",")[0];

		if (county.equals("jimwells")) {
			address = address.replaceAll("\\([^)]*\\)", "");

			if (address.contains("(") && !address.contains(")"))
				address = address.split("\\(")[0];

			address = address.replaceAll("\\s+", " ");
		}

		// remove ZIP code
		address = address.replaceFirst("\\s*\\d{5}(-\\d{4})?\\s*$", "");

		address = address.replaceAll("\\s{2,}", " ");

		if ("nueces".equalsIgnoreCase(county)) {
			String[] splitted = StringFormats.parseCityFromAddress(address, ro.cst.tsearch.utils.CountyCities.TX_NUECES);
			m.put(PropertyIdentificationSetKey.CITY.getKeyName(), splitted[0]);
			address = splitted[1];
		}

		if ("brazoria".equalsIgnoreCase(county)) {
			String[] splitted = StringFormats.parseCityFromAddress(address, ro.cst.tsearch.utils.CountyCities.TX_BRAZORIA);
			m.put(PropertyIdentificationSetKey.CITY.getKeyName(), splitted[0]);
			address = splitted[1];
			if(address.split("\\s+").length>2 && (address.contains("HWY") || address.contains("HIGHWAY"))){
				address= address.replaceAll("(?ism)HI?G?H?WA?Y \\d+","").trim();
			}
		}
		if("kaufman".equalsIgnoreCase(county)) {
			String[] addrArray = new String[]{address};
			TXGenericAO.parseCityFromAddress(m, CountyCities.TX_KAUFMAN, addrArray);
			address = addrArray[0].trim();
		}
		if ("comal".equalsIgnoreCase(county)) {
			String[] addrArray = new String[] { address };
			TXGenericAO.parseCityFromAddress(m, CountyCities.TX_COMAL, addrArray);
			address = addrArray[0].trim();
		}

		m.put(PropertyIdentificationSetKey.ADDRESS_ON_SERVER.getKeyName(), address);

		String addr = address.replaceAll("(?ism)([\\d]* [\\w]* [\\w]+ *[\\w]* [\\w]*  )([[\\w]+ *]* [\\d+]*)?", "$1");
		String city_zip = address.replaceAll("(?ism)([\\d]* [\\w]* [\\w]+ *[\\w]* [\\w]*  )([[\\w]+ *]* [\\d+]*)?", "$2");

		if (StringUtils.isNotEmpty(addr)) {
			String streetName = StringFormats.StreetName(addr).replaceAll("\\s+", " ");
			String streetNo = StringFormats.StreetNo(addr);
			
			if(addr.matches("(?ism)HI?G?H?WA?Y \\d+")){
				streetName = addr;
				streetNo = "";
			}
			
			m.put(PropertyIdentificationSetKey.STREET_NAME.getKeyName(), streetName);
			m.put(PropertyIdentificationSetKey.STREET_NO.getKeyName(), streetNo);
		}

		if (StringUtils.isNotEmpty(city_zip) && !city_zip.equals(addr)
				&& !county.equals("jimwells")) {
			String city = city_zip.replaceAll("([[\\w]+ *]*) ([\\d+]*)", "$1");
			String zip = city_zip.replaceAll("([[\\w]+ *]*) ([\\d+]*)", "$2");
			m.put(PropertyIdentificationSetKey.CITY.getKeyName(), city);
			m.put(PropertyIdentificationSetKey.ZIP.getKeyName(), zip);
		}

	}

	private static void parseAddressTXPotterAO(ResultMap m, String county,
			long searchId) throws Exception {
		parseAddressTXGenericTaxNetAO(m, county, searchId);
	}

	public static void parseAndFillResultMap(String detailsHtml, ResultMap m,
			String county, long searchId) {

		detailsHtml = detailsHtml.replaceAll("(?is)&nbsp;", " ");

		if (county.equals("bosque") || county.equals("coleman")
				|| county.equals("waller")) {
			parseAndFillResultMapTXBosqueAO(detailsHtml, m, county, searchId);
			return;
		}

		boolean atascosa = ATASCOSA_121_LIST.contains(county);

		boolean armstrong = ARMSTRONG_58_LIST.contains(county);

		boolean aransas = ARANSAS_15_LIST.contains(county);

		boolean archer = ARCHER_19_LIST.contains(county);

		boolean bowie = BOWIE_12_LIST.contains(county);

		boolean anderson = ANDERSON_11_LIST.contains(county);

		boolean briscoe = BRISCOE_5_LIST.contains(county);

		boolean dallas = DALLAS_3_LIST.contains(county);

		boolean potter = POTTER_5_LIST.contains(county);

		boolean bailey = BAILEY_2_LIST.contains(county);
		
		if (atascosa || bailey || "nueces".equalsIgnoreCase(county))
			parseAndFillResultMapTXAtascosaAO(detailsHtml, m, county, searchId);
		else if (armstrong || "sanpatricio".equalsIgnoreCase(county))
			parseAndFillResultMapTXArmstrongAO(detailsHtml, m, county, searchId);
		else if (aransas)
			parseAndFillResultMapTXAransasAO(detailsHtml, m, county, searchId);
		else if (archer)
			parseAndFillResultMapTXArcherAO(detailsHtml, m, county, searchId);
		else if (bowie)
			parseAndFillResultMapTXBowieAO(detailsHtml, m, county, searchId);
		else if (anderson)
			parseAndFillResultMapTXAndersonAO(detailsHtml, m, county, searchId);
		else if (briscoe)
			parseAndFillResultMapTXBriscoeAO(detailsHtml, m, county, searchId);
		else if (dallas)
			parseAndFillResultMapTXDallasAO(detailsHtml, m, county, searchId);
		else if (potter)
			parseAndFillResultMapTXPotterAO(detailsHtml, m, county, searchId);
		else if (county.equals("ochiltree") || county.equals("tarrant"))
			parseAndFillResultMapTXOchiltreeAO(detailsHtml, m, county, searchId);
	}

	public static void parseAndFillResultMapTXBosqueAO(String detailsHtml,
			ResultMap m, String county, long searchId) {
		try {
			org.htmlparser.Parser htmlParser = org.htmlparser.Parser
					.createParser(detailsHtml, null);
			NodeList mainList = htmlParser.parse(null);

			if (county.equals("bosque")) {
				String pidn = getPidLike(mainList, PIDN_KEY, PIDN_PATTERN);
				if (StringUtils.isNotEmpty(pidn)) {
					m.put("PropertyIdentificationSet.ParcelID", pidn.trim());
				}

				String account_no = getPidLike(mainList, LONG_ACCOUNT_NUMBER_KEY, LONG_ACCOUNT_NUMBER_PATTERN);
				if (StringUtils.isNotEmpty(account_no)) {
					m.put("PropertyIdentificationSet.ParcelID2",
							account_no.trim());
					m.put(PropertyIdentificationSetKey.GEO_NUMBER.getKeyName(),
							account_no.trim());
				}
			}

			String total_estimated_taxes = HtmlParser3
					.getValueFromAbsoluteCell(
							0,
							1,
							HtmlParser3.findNode(mainList,
									"Total Estimated Taxes"), "", false)
					.replaceAll("[-\\s,$]", "");

			m.put(TaxHistorySetKey.BASE_AMOUNT.getKeyName(),
					total_estimated_taxes);

			String ownerName = HtmlParser3.getValueFromNextCell(
					HtmlParser3.findNode(mainList, "Owner Name"), "", true)
					.trim();
			String coOwnerName = HtmlParser3.getValueFromNextCell(
					HtmlParser3.findNode(mainList, "Mailing"), "", true).trim();
			coOwnerName = coOwnerName.replaceAll(
					"(?is)\\bUNABLE TO DELIVER\\b", "UNKNOWN");
			String[] lines = coOwnerName.split("<br>");
			boolean nameInAddressField = false;
			if (!lines[0].trim().matches("(?is).*\\d+.*")) {
				nameInAddressField = true;
				if (lines[0].trim().indexOf("BOX") == -1) {
					nameInAddressField = true;
					if (lines[0].trim().indexOf("UNKNOWN") == -1) {
						nameInAddressField = true;
					} else {
						nameInAddressField = false;
					}
				} else {
					nameInAddressField = false;
				}
			}
			if (nameInAddressField) {
				lines[0] = lines[0].trim().replaceAll("(?is)\\bOF THE\\b", "");
				if (ownerName.endsWith("&") || lines[0].trim().startsWith("&")
						|| lines[0].trim().startsWith("AND ")) {
					ownerName += " " + lines[0].trim();
				} else {
					ownerName += " & " + lines[0].trim();
				}
				if (lines[1].trim().startsWith("%")) {
					ownerName += " & " + lines[1].trim();
				}
			}
			ownerName = ownerName.replaceAll("\\s*&\\s*&\\s*", " & ");

			m.put("tmpOwner", StringUtils.isNotEmpty(ownerName) ? ownerName
					: "");

			String lotOrTract = "";

			lotOrTract = HtmlParser3.getValueFromNextCell(
					HtmlParser3.findNode(mainList, "Tract or Lot"), "", true)
					.trim();

			if (lotOrTract.toLowerCase().contains("tr")) {
				lotOrTract = lotOrTract.replaceAll("\\bTR\\s*", "");
				m.put("PropertyIdentificationSet.SubdivisionTract", lotOrTract);
			} else {
				lotOrTract = lotOrTract
						.replaceAll("\\b[NSWE]?/?PT\\s+OF\\s*", "")
						.replaceAll("\\(ALL OF\\)", "")
						.replaceAll("\\s*,\\s*", " ");
				lotOrTract = lotOrTract.replaceAll(
						"\\b[NSWE]{1,2}\\s*[/\\d\\.]+(\\s*OF\\s*)?", "");
				lotOrTract = lotOrTract.replaceAll("\\bOF\\s+BK\\s*\\d+,?,?",
						"");
				lotOrTract = lotOrTract.replaceAll("\\bLT\\b", "")
						.replaceAll("[\\)\\(]+", "")
						.replaceAll("\\s*&\\s*", " ")
						.replaceAll("\\bOF\\b", "");
				lotOrTract = lotOrTract.replaceAll("\\bLOT\\s*", "");
				lotOrTract = lotOrTract.replaceAll("\\s{2,}", " ");
				lotOrTract = LegalDescription.cleanValues(lotOrTract, false,
						true);
				m.put("PropertyIdentificationSet.SubdivisionLotNumber",
						StringUtils.removeLeadingZeroes(lotOrTract));
			}
			String block = HtmlParser3.getValueFromNextCell(
					HtmlParser3.findNode(mainList, "Block"), "", true).trim();
			block = block.replaceAll("\\b[NSWE]/\\d+", "").replaceAll(
					"[\\)\\(]+", "");
			m.put("PropertyIdentificationSet.SubdivisionBlock", block
					.replaceAll("\\A0+", "").replace("BLK", ""));

			String legal = HtmlParser3.getValueFromNextCell(
					HtmlParser3.findNode(mainList, "Legal"), "", true).trim();
			m.put("tmpLegal", legal);

			putAbsNo(m, mainList, "Abstract Code");

			String siteAddress = HtmlParser3.getValueFromNextCell(
					HtmlParser3.findNode(mainList, "Location"), "", true)
					.trim();
			if (StringUtils.isNotEmpty(siteAddress)) {
				m.put("tmpAddress", siteAddress);
			}

			String landAppraisal = HtmlParser3.getValueFromNextCell(
					HtmlParser3.findNode(mainList, "Land Value"), "", true)
					.trim();
			landAppraisal = landAppraisal.replaceAll("[\\$,]", "");
			m.put("PropertyAppraisalSet.LandAppraisal", landAppraisal);

			String improvementAppraisal = HtmlParser3
					.getValueFromNextCell(
							HtmlParser3.findNode(mainList, "Improvement Value"),
							"", true).replaceAll("[\\$,]", "").trim();

			m.put(PropertyAppraisalSetKey.IMPROVEMENT_APPRAISAL.getKeyName(),
					improvementAppraisal);

			String assessed = HtmlParser3.getValueFromNextCell(
					HtmlParser3.findNode(mainList, "Assessed Value"), "", true)
					.trim();
			m.put("PropertyAppraisalSet.TotalAssessment",
					assessed.replaceAll("[-$,]", ""));

			String apprisal = HtmlParser3.getValueFromNextCell(
					HtmlParser3.findNode(mainList, "Total Value"), "", true)
					.trim();

			m.put(PropertyAppraisalSetKey.TOTAL_APPRAISAL.getKeyName(),
					apprisal);

			String deedVol = HtmlParser3.getValueFromNextCell(
					HtmlParser3.findNode(mainList, "Deed Volume"), "", true)
					.trim();
			String deedPage = HtmlParser3.getValueFromNextCell(
					HtmlParser3.findNode(mainList, "Deed Page"), "", true)
					.trim();

			@SuppressWarnings("rawtypes")
			List<List> body = new ArrayList<List>();
			List<String> line = new ArrayList<String>();
			if (!StringUtils.isEmpty(deedPage) || !StringUtils.isEmpty(deedVol)) {
				line.add(deedVol);
				line.add(deedPage);
				body.add(line);
			}

			if (body != null && body.size() > 0) {
				ResultTable rt = new ResultTable();
				String[] header = { "Book", "Page" };
				rt = GenericFunctions2.createResultTable(body, header);
				m.put("SaleDataSet", rt);
			}

			try {
				parseAddressTXGenericTaxNetAO(m, county, searchId);
				parseLegalTXGenericTaxNetAO(m, county, searchId);
				partyNamesTXGenericTaxNetAO(m, county, searchId);

			} catch (Exception e) {
				e.printStackTrace();
			}

			m.removeTempDef();

		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public static void parseAndFillResultMapTXAndersonAO(String detailsHtml,
			ResultMap m, String county, long searchId) {
		try {
			org.htmlparser.Parser htmlParser = org.htmlparser.Parser.createParser(detailsHtml, null);
			NodeList mainList = htmlParser.parse(null);

			String geoNo = getPidLike(mainList, GEO_KEY, GEO_PATTERN);
			
			if (StringUtils.isNotEmpty(geoNo)) {
				m.put("PropertyIdentificationSet.ParcelID2", geoNo.trim());
				m.put(PropertyIdentificationSetKey.GEO_NUMBER.getKeyName(), geoNo.trim());
			}

			String pidn = getPidLike(mainList, ACCOUNT_NUMBER_KEY, ACCOUNT_NUMBER_PATTERN);
			if (StringUtils.isNotEmpty(pidn)) {
				m.put("PropertyIdentificationSet.ParcelID", pidn.trim());
			}

			String total_estimated_taxes = HtmlParser3
					.getValueFromAbsoluteCell(
							0,
							1,
							HtmlParser3.findNode(mainList,
									"Total Estimated Taxes"), "", false)
					.replaceAll("[-\\s,$]", "");

			m.put(TaxHistorySetKey.BASE_AMOUNT.getKeyName(),
					total_estimated_taxes);

			String ownerName = HtmlParser3.getValueFromNextCell(
					HtmlParser3.findNode(mainList, "Owner Name"), "", true)
					.trim();
			String coOwnerName = HtmlParser3.getValueFromNextCell(
					HtmlParser3.findNode(mainList, "Mailing"), "", true).trim();
			coOwnerName = coOwnerName.replaceAll(
					"(?is)\\bUNABLE TO DELIVER\\b", "UNKNOWN");
			String[] lines = coOwnerName.split("<br>");
			boolean nameInAddressField = false;
			if (!lines[0].trim().matches("(?is).*\\d+.*")) {
				nameInAddressField = true;
				if (lines[0].trim().indexOf("BOX") == -1) {
					nameInAddressField = true;
					if (lines[0].trim().indexOf("UNKNOWN") == -1) {
						nameInAddressField = true;
					} else {
						nameInAddressField = false;
					}
				} else {
					nameInAddressField = false;
				}
			}
			if (nameInAddressField) {
				lines[0] = lines[0].trim().replaceAll("(?is)\\bOF THE\\b", "");
				if (ownerName.endsWith("&") || lines[0].trim().startsWith("&")
						|| lines[0].trim().startsWith("AND ")) {
					ownerName += " " + lines[0].trim();
				} else {
					ownerName += " & " + lines[0].trim();
				}
				if (lines[1].trim().startsWith("%")) {
					ownerName += " & " + lines[1].trim();
				}
			}
			ownerName = ownerName.replaceAll("\\s*&\\s*&\\s*", " & ");

			m.put("tmpOwner", StringUtils.isNotEmpty(ownerName) ? ownerName
					: "");

			String legal = HtmlParser3.getValueFromNextCell(
					HtmlParser3.findNode(mainList, "Legal"), "", true).trim();
			m.put("tmpLegal", legal);

			String siteAddress = HtmlParser3.getValueFromNextCell(
					HtmlParser3.findNode(mainList, "Location"), "", true)
					.trim();
			if (StringUtils.isNotEmpty(siteAddress)) {
				m.put("tmpAddress", siteAddress);
			}

			NodeList tables = mainList.extractAllNodesThatMatch(
					new TagNameFilter("table"), true).extractAllNodesThatMatch(
					new HasAttributeFilter("bgcolor", "#D0D0D0"), true);

			TableTag value_info = getTableFromNodeList(tables, new String[] {
					"Land Value", "Total Value" });

			String landAppraisal = value_info
					.toHtml()
					.replaceAll(
							"(?ism).*<td class=\"reports_defred\">Land Value</td>[^<]*<td align=\"right\" width=\"1%\">([^<]*)</td>.*",
							"$1").replaceAll("[\\$,]", "");

			if (landAppraisal.length() < 20)
				m.put(PropertyAppraisalSetKey.LAND_APPRAISAL.getKeyName(),
						landAppraisal);

			String improvementAppraisal = value_info
					.toHtml()
					.replaceAll(
							"(?ism).*<td class=\"reports_defred\">Improvement Value</td>[^<]*<td align=\"right\" width=\"1%\">([^<]*)</td>.*",
							"$1").replaceAll("[\\$,]", "").trim();

			if (improvementAppraisal.length() < 20)
				m.put(PropertyAppraisalSetKey.IMPROVEMENT_APPRAISAL
						.getKeyName(), improvementAppraisal);

			String apprisal = value_info
					.toHtml()
					.replaceAll(
							"(?ism).*<td class=\"reports_defred\">Total Value</td>[^<]*<td align=\"right\" width=\"1%\" class=\"red_b\">([^<]*)</td>.*",
							"$1").replaceAll("[\\$,]", "").trim();

			if (apprisal.length() < 20)
				m.put(PropertyAppraisalSetKey.TOTAL_APPRAISAL.getKeyName(),
						apprisal);

			String deedPage = HtmlParser3.getValueFromNextCell(
					HtmlParser3.findNode(mainList, "Deed Page"), "", true)
					.trim();
			String deedDate = HtmlParser3.getValueFromNextCell(
					HtmlParser3.findNode(mainList, "Deed Date"), "", true)
					.trim();

			deedDate = org.apache.commons.lang.StringUtils.strip(deedDate
					.split(" ")[0].replaceAll("[-\\s]", ""));
			if (deedDate.length() == 8 && deedDate.matches("\\d+"))
				deedDate = deedDate.replaceAll("(\\d{4})(\\d{2})(\\d{2})",
						"$2/$3/$1");

			@SuppressWarnings("rawtypes")
			List<List> body = new ArrayList<List>();
			List<String> line = new ArrayList<String>();
			if (!StringUtils.isEmpty(deedPage)) {
				// line.add(deedVol);
				line.add(deedPage);
				line.add(deedDate);
				body.add(line);
			}

			if (body != null && body.size() > 0) {
				ResultTable rt = new ResultTable();
				String[] header = { "Page", "InstrumentDate" };
				rt = GenericFunctions2.createResultTable(body, header);
				m.put("SaleDataSet", rt);
			}

			try {
				parseAddressTXAndersonAO(m, county, searchId);
				parseLegalTXGenericTaxNetAO(m, county, searchId);
				partyNamesTXAndersonAO(m, county, searchId);

			} catch (Exception e) {
				e.printStackTrace();
			}

			m.removeTempDef();

		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	/**
	 * Clean multiple whitespaces + trim
	 * @param input
	 * @return
	 */
	protected static String ctrim(String input) {
		return input.replace("&nbsp;", " ").replaceAll("\\s+", " ").trim();
	}

	/**
	 * Parse lot numbers from the legal description 
	 * @param legal
	 * @return
	 */
	public static String parseLot(String legal) {
		String lot = "";

		// L 1 THRU 5
		Pattern p = Pattern.compile("L\\s*([\\d]+)\\s*THRU\\s*(\\d+)");
		Matcher ma = p.matcher(legal);
		if (ma.find()) {
			try {
				int from = Integer.parseInt(ma.group(1));
				int to = Integer.parseInt(ma.group(2));
				for (int i = from; i <= to; i++)
					lot += i + " ";
			} catch (Exception e) {
			}
		} else {
			// L 5
			// or
			// L 5 & 6
			p = Pattern.compile("L\\s*([\\d\\s\\&]+)");
			ma = p.matcher(legal);
			if (ma.find()) {
				lot = ctrim(ma.group(1).replace("&", " "));
			}
		}
		
		
		return LegalDescription.cleanValues(lot, false, true);
	}

	public static void parseAndFillResultMapTXAransasAO(String detailsHtml,
			ResultMap m, String county, long searchId) {
		try {
			org.htmlparser.Parser htmlParser = org.htmlparser.Parser
					.createParser(detailsHtml, null);
			NodeList mainList = htmlParser.parse(null);

			String pidn = getPidLike(mainList, PIDN_KEY, PIDN_PATTERN);
			if (StringUtils.isNotEmpty(pidn)) {
				m.put(PropertyIdentificationSetKey.PARCEL_ID.getKeyName(),
						pidn.trim());
			}

			String total_estimated_taxes = HtmlParser3
					.getValueFromAbsoluteCell(
							0,
							1,
							HtmlParser3.findNode(mainList,
									"Total Estimated Taxes"), "", false)
					.replaceAll("[-\\s,$]", "");

			m.put(TaxHistorySetKey.BASE_AMOUNT.getKeyName(),
					total_estimated_taxes);

			String lotOrTract = HtmlParser3.getValueFromNextCell(HtmlParser3.findNode(mainList, "Tract or Lot"), "", true).trim();
			
			if(lotOrTract.contains("(")) {
				lotOrTract = lotOrTract.substring(0, lotOrTract.indexOf("("));
			}
			lotOrTract = lotOrTract.replaceAll("AND", " ").replaceAll("\\s+", " ");
			lotOrTract = lotOrTract.replaceAll("\\s*&\\s*", " ").trim();
			m.put(PropertyIdentificationSetKey.SUBDIVISION_LOT_NUMBER.getKeyName(), 
					StringUtils.removeLeadingZeroes(lotOrTract));

			String block = HtmlParser3.getValueFromNextCell(
					HtmlParser3.findNode(mainList, "Block"), "", true).trim();
			m.put(PropertyIdentificationSetKey.SUBDIVISION_BLOCK.getKeyName(),
					block.trim());

			// get owner table
			NodeList owner_t_list = mainList
					.extractAllNodesThatMatch(new TagNameFilter("table"), true)
					.extractAllNodesThatMatch(
							new HasAttributeFilter("width", "100%"), true)
					.extractAllNodesThatMatch(
							new HasAttributeFilter("cellspacing", "1"), true)
					.extractAllNodesThatMatch(
							new HasAttributeFilter("cellpadding", "8"), true)
					.extractAllNodesThatMatch(
							new HasAttributeFilter("bgcolor", "#D0D0D0"), true);

			if (owner_t_list.size() > 0) {

				TableTag owners = (TableTag) owner_t_list.elementAt(0);

				if (owners != null && owners.getRows().length <= 3) {
					String ownerName = HtmlParser3.getValueFromNextCell(
							HtmlParser3.findNode(mainList, "Owner"), "", true)
							.trim();

					// String coOwner = ownerName
					// .replaceAll(
					// "(?ism).*<span class=\"reports_blacktxt\">([^<]*)<.*",
					// "$1");
					//

					//
					// if (coOwner.contains("&") || coOwner.contains("%"))
					// ownerName += coOwner.replaceAll("\\s+", " ").replace(
					// "%", "&");
					//

					ownerName = ownerName
							.replaceAll(
									"(?ism)<span class=\"bold_blue_gray\">([^<]*)</span>.*",
									"$1");
					ownerName = ownerName.replaceAll("\\s*&\\s*&*\\s*", " & ")
							.toUpperCase();

					m.put("tmpOwner",
							StringUtils.isNotEmpty(ownerName) ? ownerName : "");
				} else {
					TableRow[] rows = owners.getRows();

					String owner = "";

					for (int i = 0; i < rows.length; i++) {
						if (rows[i].getColumnCount() == 2) {
							if (rows[i].getColumns()[0].toPlainTextString()
									.contains("Owner")) {
								String aux_owner = rows[i].getColumns()[1]
										.toHtml()
										.replaceAll(
												"(?ism)<span class=\"bold_blue_gray\">([^<]*)</span>.*",
												"$1")
										.replaceAll("(?ism)<[^>]*>", "");
								owner += aux_owner + " && ";
							}
						}
					}

					m.put("tmpOwner", StringUtils.isNotEmpty(owner) ? owner
							: "");
				}
			}

			String legal = HtmlParser3.getValueFromNextCell(
					HtmlParser3.findNode(mainList, "Legal"), "", true).trim();
			m.put("tmpLegal", legal);

			putAbsNo(m, mainList, "Abstract Code");
			putAcres(m, mainList);

			String siteAddress = HtmlParser3.getValueFromNextCell(
					HtmlParser3.findNode(mainList, "Location"), "", true)
					.trim();
			if (StringUtils.isNotEmpty(siteAddress)) {
				m.put("tmpAddress", siteAddress);
			}

			String improvementAppraisal = HtmlParser3.getValueFromNextCell(
					HtmlParser3.findNode(mainList, "Improvement Value"), "",
					true).trim();
			improvementAppraisal = improvementAppraisal
					.replaceAll("[\\$,]", "");
			m.put("PropertyAppraisalSet.ImprovementAppraisal",
					improvementAppraisal);

			String assessed = HtmlParser3.getValueFromNextCell(
					HtmlParser3.findNode(mainList, "Assessed Value"), "", true)
					.trim();
			// assessed = assessed.replaceAll("(?is)([\\d,]+).*", "$1");
			m.put("PropertyAppraisalSet.TotalAssessment",
					assessed.replaceAll("[-$,]", ""));

			@SuppressWarnings("rawtypes")
			List<List> body = new ArrayList<List>();
			List<String> line = new ArrayList<String>();

			TableTag deed_info = getTableFromNodeList(mainList, new String[] {
					"Deed Volume", "Deed Page", "Instrument Number" });

			if (deed_info != null) {
				TableRow[] rows = deed_info.getRows();
				for (int i = 1; i < rows.length; i++) {
					if (rows[i].getColumnCount() == 6) {
						if (!"0".equals(rows[i].getColumns()[1]
								.toPlainTextString())
								|| !"0".equals(rows[i].getColumns()[2]
										.toPlainTextString())) {
							line = new ArrayList<String>();
							line.add(rows[i].getColumns()[3]
									.toPlainTextString());
							line.add(rows[i].getColumns()[1]
									.toPlainTextString());
							line.add(rows[i].getColumns()[2]
									.toPlainTextString());
							line.add(rows[i].getColumns()[0]
									.toPlainTextString());
							body.add(line);
						}
					}
				}
			}

			if (body != null && body.size() > 0) {
				ResultTable rt = new ResultTable();
				String[] header = { "InstrumentNumber", "Book", "Page",
						"InstrumentDate" };
				rt = GenericFunctions2.createResultTable(body, header);
				m.put("SaleDataSet", rt);
			}

			try {
				parseAddressTXAransasAO(m, county, searchId);
				parseLegalTXGenericTaxNetAO(m, county, searchId);
				partyNamesTXAransasAO(m, county, searchId);

			} catch (Exception e) {
				e.printStackTrace();
			}

			m.removeTempDef();

		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	

	private static void parseAndFillResultMapTXArcherAO(String detailsHtml,
			ResultMap m, String county, long searchId) {
		try {
			org.htmlparser.Parser htmlParser = org.htmlparser.Parser
					.createParser(detailsHtml, null);
			NodeList mainList = htmlParser.parse(null);

			String pidn = getPidLike(mainList, ACCOUNT_NUMBER_KEY, ACCOUNT_NUMBER_PATTERN);
			
			if (StringUtils.isNotEmpty(pidn)) {
				m.put(PropertyIdentificationSetKey.PARCEL_ID.getKeyName(),
						pidn.trim());
			}

			String total_estimated_taxes = HtmlParser3
					.getValueFromAbsoluteCell(
							0,
							1,
							HtmlParser3.findNode(mainList,
									"Total Estimated Taxes"), "", false)
					.replaceAll("[-\\s,$]", "");

			m.put(TaxHistorySetKey.BASE_AMOUNT.getKeyName(),
					total_estimated_taxes);

			// get owner table
			NodeList owner_t_list = mainList
					.extractAllNodesThatMatch(new TagNameFilter("table"), true)
					.extractAllNodesThatMatch(
							new HasAttributeFilter("width", "100%"), true)
					.extractAllNodesThatMatch(
							new HasAttributeFilter("cellspacing", "1"), true)
					.extractAllNodesThatMatch(
							new HasAttributeFilter("cellpadding", "8"), true)
					.extractAllNodesThatMatch(
							new HasAttributeFilter("bgcolor", "#D0D0D0"), true);

			if (owner_t_list.size() > 0) {

				TableTag owners = (TableTag) owner_t_list.elementAt(0);

				if (owners != null && owners.getRows().length <= 4) {
					String ownerName = HtmlParser3.getValueFromNextCell(
							HtmlParser3.findNode(mainList, "Owner"), "", true)
							.trim();

					ownerName = ownerName
							.replaceAll(
									"(?ism)<span class=\"bold_blue_gray\">([^<]*)</span>.*",
									"$1");
					ownerName = ownerName.replaceAll("\\s*&\\s*&*\\s*", " & ")
							.toUpperCase();

					m.put("tmpOwner",
							StringUtils.isNotEmpty(ownerName) ? ownerName : "");
				}
			}

			String legal = HtmlParser3.getValueFromNextCell(
					HtmlParser3.findNode(mainList, "Legal"), "", true).trim();
			m.put("tmpLegal", legal);

			String siteAddress = HtmlParser3.getValueFromNextCell(
					HtmlParser3.findNode(mainList, "Location"), "", true)
					.trim();
			if (StringUtils.isNotEmpty(siteAddress)) {
				m.put("tmpAddress", siteAddress);
			}

			String improvementAppraisal = HtmlParser3.getValueFromNextCell(
					HtmlParser3.findNode(mainList, "Improvement Value"), "",
					true).trim();
			improvementAppraisal = improvementAppraisal
					.replaceAll("[\\$,]", "");
			m.put("PropertyAppraisalSet.ImprovementAppraisal",
					improvementAppraisal);

			String apprisal = HtmlParser3.getValueFromNextCell(
					HtmlParser3.findNode(mainList, "Total Value"), "", true)
					.trim();
			// assessed = assessed.replaceAll("(?is)([\\d,]+).*", "$1");
			m.put(PropertyAppraisalSetKey.TOTAL_APPRAISAL.getKeyName(),
					apprisal.replaceAll("[-$,]", ""));

			@SuppressWarnings("rawtypes")
			List<List> body = new ArrayList<List>();
			List<String> line = new ArrayList<String>();

			TableTag deed_info = getTableFromNodeList(mainList, new String[] {
					"Sale Volume", "Sale Page", "Sale Deed Date" });

			if (deed_info != null) {
				TableRow[] rows = deed_info.getRows();
				for (int i = 1; i < rows.length; i++) {
					if (rows[i].getColumnCount() == 7) {
						if (!"0".equals(rows[i].getColumns()[4]
								.toPlainTextString())
								|| !"0".equals(rows[i].getColumns()[5]
										.toPlainTextString())) {
							line = new ArrayList<String>();
							line.add(rows[i].getColumns()[4]
									.toPlainTextString());
							line.add(rows[i].getColumns()[5]
									.toPlainTextString());
							String date = org.apache.commons.lang.StringUtils
									.strip(rows[i].getColumns()[6]
											.toPlainTextString().replaceAll(
													"\\s", ""));
							if (date.length() == 8 && date.matches("\\d+")) {
								line.add(date.replaceAll(
										"(\\d{4})(\\d{2})(\\d{2})", "$2/$3/$1"));
							} else
								line.add("");
							body.add(line);
						}
					}
				}
			}

			if (body != null && body.size() > 0) {
				ResultTable rt = new ResultTable();
				String[] header = { "Book", "Page", "InstrumentDate" };
				rt = GenericFunctions2.createResultTable(body, header);
				m.put("SaleDataSet", rt);
			}

			try {
				parseAddressTXArcherAO(m, county, searchId);
				parseLegalTXGenericTaxNetAO(m, county, searchId);
				partyNamesTXArcherAO(m, county, searchId);

			} catch (Exception e) {
				e.printStackTrace();
			}

			m.removeTempDef();

		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public static String getPidLike(NodeList nodeList, String textToFind, String regexToExtract) {
		String result = HtmlParser3.getValueFromCell(
				(CompositeTag) HtmlParser3.findNode(nodeList,textToFind).getParent(),
				regexToExtract, 
				true);
		if(result != null) {
			result = result.replace("&nbsp;", "");
		}
		return result;
	}

	public static void parseAndFillResultMapTXArmstrongAO(String detailsHtml,
			ResultMap m, String county, long searchId) {
		try {
			org.htmlparser.Parser htmlParser = org.htmlparser.Parser
					.createParser(detailsHtml, null);
			NodeList mainList = htmlParser.parse(null);

			String lotOrTract = "";

			lotOrTract = HtmlParser3.getValueFromNextCell(
					HtmlParser3.findNode(mainList, "Tract/Lot"), "", true)
					.trim();

			if (lotOrTract.toLowerCase().contains("tr")) {
				lotOrTract = lotOrTract.replaceAll("\\bTR\\s*", "");
				m.put("PropertyIdentificationSet.SubdivisionTract", lotOrTract);
			} else {
				lotOrTract = lotOrTract
						.replaceAll("\\b[NSWE]?/?PT\\s+OF\\s*", "")
						.replaceAll("\\(ALL OF\\)", "")
						.replaceAll("\\s*,\\s*", " ");
				lotOrTract = lotOrTract.replaceAll(
						"\\b[NSWE]{1,2}\\s*[/\\d\\.]+(\\s*OF\\s*)?", "");
				lotOrTract = lotOrTract.replaceAll("\\bOF\\s+BK\\s*\\d+,?,?",
						"");
				lotOrTract = lotOrTract.replaceAll("\\bLT\\b", "")
						.replaceAll("[\\)\\(]+", "")
						.replaceAll("\\s*&\\s*", " ")
						.replaceAll("\\bOF\\b", "");
				lotOrTract = lotOrTract.replaceAll("\\bLOT\\s*", "");
				lotOrTract = lotOrTract.replaceAll("\\s{2,}", " ");
				lotOrTract = LegalDescription.cleanValues(lotOrTract, false,
						true);
				m.put("PropertyIdentificationSet.SubdivisionLotNumber",
						StringUtils.removeLeadingZeroes(lotOrTract));
			}
			String block = HtmlParser3.getValueFromNextCell(
					HtmlParser3.findNode(mainList, "Block"), "", true).trim();
			block = block.replaceAll("\\b[NSWE]/\\d+", "").replaceAll(
					"[\\)\\(]+", "");
			m.put("PropertyIdentificationSet.SubdivisionBlock", block
					.replaceAll("\\A0+", "").replace("BLK", ""));

			String geoNo = getPidLike(mainList, GEO_NUMBER_KEY, GEO_NUMBER_PATTERN);

			geoNo = org.apache.commons.lang.StringUtils.strip(geoNo);

			if (StringUtils.isNotEmpty(geoNo)) {
				m.put("PropertyIdentificationSet.ParcelID2", geoNo.trim());
				m.put(PropertyIdentificationSetKey.GEO_NUMBER.getKeyName(), geoNo.trim());
			}

			String account_number = getPidLike(mainList, ACCOUNT_NUMBER_KEY, ACCOUNT_NUMBER_PATTERN);
			if (StringUtils.isNotEmpty(account_number)) {
				m.put("PropertyIdentificationSet.ParcelID",
						account_number.trim());
			}

			String total_estimated_taxes = HtmlParser3
					.getValueFromAbsoluteCell(
							0,
							1,
							HtmlParser3.findNode(mainList,
									"Total Estimated Taxes"), "", false)
					.replaceAll("[-\\s,$]", "");

			m.put(TaxHistorySetKey.BASE_AMOUNT.getKeyName(),
					total_estimated_taxes);

			String ownerName = HtmlParser3.getValueFromNextCell(
					HtmlParser3.findNode(mainList, "Owner Name"), "", true)
					.trim();
			String coOwnerName = HtmlParser3.getValueFromNextCell(
					HtmlParser3.findNode(mainList, "Mailing"), "", true).trim();
			coOwnerName = coOwnerName.replaceAll(
					"(?is)\\bUNABLE TO DELIVER\\b", "UNKNOWN");
			String[] lines = coOwnerName.split("<br>");
			boolean nameInAddressField = false;
			if (!lines[0].trim().matches("(?is).*\\d+.*")) {
				nameInAddressField = true;
				if (lines[0].trim().indexOf("BOX") == -1) {
					nameInAddressField = true;
					if (lines[0].trim().indexOf("UNKNOWN") == -1) {
						nameInAddressField = true;
					} else {
						nameInAddressField = false;
					}
				} else {
					nameInAddressField = false;
				}
			}
			if (nameInAddressField) {
				lines[0] = lines[0].trim().replaceAll("(?is)\\bOF THE\\b", "");
				if (ownerName.endsWith("&") || lines[0].trim().startsWith("&")
						|| lines[0].trim().startsWith("AND ")) {
					ownerName += " " + lines[0].trim();
				} else {
					ownerName += " && " + lines[0].trim();
				}
				if (lines[1].trim().startsWith("%")) {
					ownerName += " && " + lines[1].trim();
				}
			}

			ownerName = ownerName.replaceAll("C/O", " ");
			ownerName = ownerName.replaceAll("/", " & ");

			m.put("tmpOwner", StringUtils.isNotEmpty(ownerName) ? ownerName
					: "");

			String legal = HtmlParser3.getValueFromNextCell(
					HtmlParser3.findNode(mainList, "Legal"), "", true).trim();
			m.put("tmpLegal", legal);

			putAbsNo(m, mainList, "Abstract Number");

			String siteAddress = HtmlParser3.getValueFromNextCell(
					HtmlParser3.findNode(mainList, "Location"), "", true)
					.trim();
			if (StringUtils.isNotEmpty(siteAddress)) {
				m.put("tmpAddress", siteAddress);
			}

			String landAppraisal = HtmlParser3.getValueFromNextCell(
					HtmlParser3.findNode(mainList, "Land Value"), "", true)
					.trim();
			landAppraisal = landAppraisal.replaceAll("[\\$,]", "");
			m.put("PropertyAppraisalSet.LandAppraisal", landAppraisal);

			String improvementAppraisal = HtmlParser3.getValueFromNextCell(
					HtmlParser3.findNode(mainList, "Improvement Value"), "",
					true).trim();
			improvementAppraisal = improvementAppraisal
					.replaceAll("[\\$,]", "");
			m.put("PropertyAppraisalSet.ImprovementAppraisal",
					improvementAppraisal);

			String assessed = HtmlParser3.getValueFromNextCell(
					HtmlParser3.findNode(mainList, "Assessed Value"), "", true)
					.trim();
			// assessed = assessed.replaceAll("(?is)([\\d,]+).*", "$1");
			m.put("PropertyAppraisalSet.TotalAssessment",
					assessed.replaceAll("[-$,]", ""));

			@SuppressWarnings("rawtypes")
			List<List> body = new ArrayList<List>();
			List<String> line = new ArrayList<String>();

			TableTag deed_info = getTableFromNodeList(mainList, new String[] {
					"Volume", "Page", "Seller" });

			if (deed_info != null) {
				TableRow[] rows = deed_info.getRows();
				for (int i = 1; i < rows.length; i++) {
					if (rows[i].getColumnCount() == 6) {
						if (!"0".equals(rows[i].getColumns()[3]
								.toPlainTextString())
								|| !"0".equals(rows[i].getColumns()[4]
										.toPlainTextString())) {
							line = new ArrayList<String>();
							line.add(rows[i].getColumns()[3]
									.toPlainTextString());
							line.add(rows[i].getColumns()[4]
									.toPlainTextString());
							line.add(rows[i].getColumns()[5]
									.toPlainTextString().split(" ")[0]);

							body.add(line);
						}
					}
				}
			}

			if (body != null && body.size() > 0) {
				ResultTable rt = new ResultTable();
				String[] header = { "Book", "Page", "InstrumentDate" };
				rt = GenericFunctions2.createResultTable(body, header);
				m.put("SaleDataSet", rt);
			}

			try {
				parseAddressTXArmstrongAO(m, county, searchId);
				parseLegalTXGenericTaxNetAO(m, county, searchId);
				partyNamesTXArmstrongAO(m, county, searchId);

			} catch (Exception e) {
				e.printStackTrace();
			}

			m.removeTempDef();

		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public static void parseAndFillResultMapTXAtascosaAO(String detailsHtml, ResultMap m, String county, long searchId) {
		try {
			org.htmlparser.Parser htmlParser = org.htmlparser.Parser.createParser(detailsHtml, null);
			NodeList mainList = htmlParser.parse(null);

			String lotOrTract = "";

			lotOrTract = HtmlParser3.getValueFromNextCell(
					HtmlParser3.findNode(mainList, "Tract or Lot"), "", true)
					.trim();

			if (lotOrTract.toLowerCase().contains("tr")) {
				lotOrTract = lotOrTract.replaceAll("\\bTR\\s*", "");
				m.put("PropertyIdentificationSet.SubdivisionTract", lotOrTract);
			} else {
				lotOrTract = lotOrTract
						.replaceAll("\\b[NSWE]?/?PT\\s+OF\\s*", "")
						.replaceAll("\\(ALL OF\\)", "")
						.replaceAll("\\s*,\\s*", " ")
						.replaceAll("\\b[NSWE]{1,2}\\s*[/\\d\\.]+(\\s*OF\\s*)?", "")
						.replaceAll("\\bOF\\s+BK\\s*\\d+,?,?","")
						.replaceAll("\\bLT\\b", "")
						.replaceAll("[\\)\\(]+", "")
						.replaceAll("\\s*&\\s*", " ")
						.replaceAll("\\bOF\\b", "")
						.replaceAll("\\bPT\\b", "")
						.replaceAll("\\bLOT\\s*", "")
						.replaceAll("\\bSEC?(TION)?\\s+([[A-Z-]|\\d]+)\\b", "")
						.replaceAll("\\b(PHA?S?E?)\\s+([A-Z]|\\d+)\\b", "")
						.replaceAll("\\s{2,}", " ");
				
				lotOrTract = LegalDescription.cleanValues(lotOrTract, false, true);
				m.put("PropertyIdentificationSet.SubdivisionLotNumber", StringUtils.removeLeadingZeroes(lotOrTract));
			}
			String block = HtmlParser3.getValueFromNextCell(
					HtmlParser3.findNode(mainList, "Block"), "", true).trim();
			block = block.replaceAll("\\b[NSWE]/\\d+", "")
					.replaceAll("[\\)\\(]+", "").replaceAll("\\A0+", "")
					.replaceAll("(?ism)SEC [^(BLK)]*", "").replace("BLK", "")
					.replaceAll(" ", "");

			m.put("PropertyIdentificationSet.SubdivisionBlock", block);

			String geoNo = getPidLike(mainList, GEO_KEY, GEO_PATTERN);
			if (StringUtils.isNotEmpty(geoNo)) {
				if ("brazoria".equalsIgnoreCase(county)||"guadalupe".equalsIgnoreCase(county)) {
					m.put(PropertyIdentificationSetKey.PARCEL_ID.getKeyName(), geoNo.trim());
				} else {
					m.put(PropertyIdentificationSetKey.PARCEL_ID2.getKeyName(), geoNo.trim());
					m.put(PropertyIdentificationSetKey.GEO_NUMBER.getKeyName(), geoNo.trim());
				}
			}

//			tc = (TableColumn) HtmlParser3.findNode(mainList, "PIDN").getParent();
			String pidn = getPidLike(mainList, PIDN_KEY, PIDN_PATTERN);
			if (StringUtils.isNotEmpty(pidn)) {
				if ("brazoria".equalsIgnoreCase(county)||"guadalupe".equalsIgnoreCase(county)) {
					m.put(PropertyIdentificationSetKey.PARCEL_ID2.getKeyName(), pidn.trim());
					m.put(PropertyIdentificationSetKey.GEO_NUMBER.getKeyName(), pidn.trim());
				} else {
					m.put(PropertyIdentificationSetKey.PARCEL_ID.getKeyName(), pidn.trim());
				}
			}

			String total_estimated_taxes = HtmlParser3
					.getValueFromAbsoluteCell(
							0,
							1,
							HtmlParser3.findNode(mainList,
									"Total Estimated Taxes"), "", false)
					.replaceAll("[-\\s,$]", "");

			m.put(TaxHistorySetKey.BASE_AMOUNT.getKeyName(),
					total_estimated_taxes);

			String ownerName = HtmlParser3.getValueFromNextCell(
					HtmlParser3.findNode(mainList, "Owner Name"), "", true)
					.trim();
			String coOwnerName = HtmlParser3.getValueFromNextCell(
					HtmlParser3.findNode(mainList, "Mailing"), "", true).trim();
			coOwnerName = coOwnerName.replaceAll(
					"(?is)\\bUNABLE TO DELIVER\\b", "UNKNOWN");
			String[] lines = coOwnerName.split("<br>");
			boolean nameInAddressField = false;
			if (!lines[0].trim().matches("(?is).*\\d+.*")) {
				nameInAddressField = true;
				if (lines[0].trim().indexOf("BOX") == -1) {
					nameInAddressField = true;
					if (lines[0].trim().indexOf("UNKNOWN") == -1) {
						nameInAddressField = true;
					} else {
						nameInAddressField = false;
					}
				} else {
					nameInAddressField = false;
				}
			}
			if (nameInAddressField) {
				lines[0] = lines[0].trim().replaceAll("(?is)\\bOF THE\\b", "");
				if (ownerName.endsWith("&") || lines[0].trim().startsWith("&")
						|| lines[0].trim().startsWith("AND ")) {
					ownerName += " " + lines[0].trim();
				} else {
					ownerName += " & " + lines[0].trim();
				}
				if (lines[1].trim().startsWith("%")) {
					ownerName += " & " + lines[1].trim();
				}
			}
			ownerName = ownerName.replaceAll("\\s*&\\s*&\\s*", " & ");

			m.put("tmpOwner", StringUtils.isNotEmpty(ownerName) ? ownerName
					: "");

			String legal = HtmlParser3.getValueFromNextCell(
					HtmlParser3.findNode(mainList, "Legal"), "", true).trim();
			m.put("tmpLegal", legal);

			putAbsNo(m, mainList, "Abstract Code");

			String siteAddress = HtmlParser3.getValueFromNextCell(HtmlParser3.findNode(mainList, "Location"), "", true).trim();
			if (StringUtils.isNotEmpty(siteAddress)) {
				siteAddress = siteAddress.replaceAll("(?is)\\s*@.*", "");
				m.put("tmpAddress", siteAddress);
			}

			String landAppraisal = HtmlParser3.getValueFromNextCell(
					HtmlParser3.findNode(mainList, "Land Value"), "", true)
					.trim();
			landAppraisal = landAppraisal.replaceAll("[\\$,]", "");
			m.put("PropertyAppraisalSet.LandAppraisal", landAppraisal);

			String improvementAppraisal = HtmlParser3
					.getValueFromNextCell(
							HtmlParser3.findNode(mainList, "Improvement Value"),
							"", true).replaceAll("[\\$,]", "").trim();

			m.put(PropertyAppraisalSetKey.IMPROVEMENT_APPRAISAL.getKeyName(), improvementAppraisal);

			String assessed = HtmlParser3.getValueFromNextCell(HtmlParser3.findNode(mainList, "Assessed Value"), "", true).trim();
			// assessed = assessed.replaceAll("(?is)([\\d,]+).*", "$1");
			m.put("PropertyAppraisalSet.TotalAssessment", assessed.replaceAll("[-$,]", ""));

			putAcres(m, mainList);
			
			if ("brazoria".equalsIgnoreCase(county)) {
				parseSaleDataSetBrazoria(mainList, m, county);
			} else {
				parseSaleDataSetAtascosa(mainList, m, county);
			}

			try {
				if (BAILEY_2_LIST.contains(county))
					parseAddressTXBaileyAO(m, county, searchId);
				else
					parseAddressTXGenericTaxNetAO(m, county, searchId);
				parseLegalTXGenericTaxNetAO(m, county, searchId);
				partyNamesTXGenericTaxNetAO(m, county, searchId);

			} catch (Exception e) {
				e.printStackTrace();
			}

			m.removeTempDef();

		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	protected static void putAbsNo(ResultMap m, NodeList mainList, String headerText) {
		String absNo = HtmlParser3.getValueFromNextCell(mainList, headerText, "", false);
		m.put(PropertyIdentificationSetKey.ABS_NO.getKeyName(), absNo.trim().replaceAll("^[A-Z]*0*", ""));
	}

	protected static void putAcres(ResultMap m, NodeList mainList) {
		String acres = HtmlParser3.getValueFromNextCell(mainList, "Land Acres", "", false);
		if(org.apache.commons.lang.StringUtils.isNotBlank(acres)) {
			try {
				Double.valueOf(acres);
				m.put(PropertyIdentificationSetKey.ACRES.getKeyName(), acres);
			} catch (NumberFormatException nfe) {}
		}
	}

	private static void parseSaleDataSetBrazoria(NodeList mainList, ResultMap m, String county) {
		String deedDate = null;

		try {
			Text findNode = HtmlParser3.findNode(mainList, "Deed Date");
			if (findNode != null) {
				deedDate = HtmlParser3.getValueFromNextCell(findNode, "", true);
				if (deedDate != null) {
					deedDate = deedDate.trim();
					if (deedDate.matches("\\s*/\\s*/\\s*")) {
						deedDate = "";
					}
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		
		deedDate = org.apache.commons.lang.StringUtils.defaultString(deedDate);
		String deedVol = HtmlParser3.getValueFromNextCell(HtmlParser3.findNode(mainList, "Deed Volume"), "", true).trim();
		deedVol = deedVol.replaceFirst("-$", "");
		String deedPage = HtmlParser3.getValueFromNextCell(HtmlParser3.findNode(mainList, "Deed Page"), "", true).trim();
		String docketNo = HtmlParser3.getValueFromNextCell(HtmlParser3.findNode(mainList, "Docket No."), "", true).trim();
		docketNo = StringUtils.removeLeadingZeroes(docketNo);
		
		if ("brazoria".equalsIgnoreCase(county) && deedVol.length() == 2 && deedPage.length() == 6) {
			deedVol = deedPage; // parse deedPage as instrument number
			deedPage = "";
		}
		
		@SuppressWarnings("rawtypes")
		List<List> body = new ArrayList<List>();
		List<String> line = null;
		if (StringUtils.isEmpty(deedPage)) {
			line = new ArrayList<String>();
			line.add(StringUtils.removeLeadingZeroes(deedVol));
			line.add("");
			line.add("");
			line.add(deedDate);
			body.add(line);
		} else {
			if (deedPage.contains("/")) { // pidn 496441 Brazoria, bug 7552
				// add to lines
				if (deedVol.length() == 2 && deedDate.length() > 2 && deedDate.substring(deedDate.length() - 2).equals(deedVol)) {
					// deed pages are instruments
					line = new ArrayList<String>();
					line.add(StringUtils.removeLeadingZeroes(deedPage.split("/")[0]));
					line.add("");
					line.add("");
					line.add(deedDate);
					body.add(line);

					line = new ArrayList<String>();
					line.add(StringUtils.removeLeadingZeroes(deedPage.split("/")[1]));
					line.add("");
					line.add("");
					line.add(deedDate);
					body.add(line);
				} else {
					line = new ArrayList<String>();
					line.add("");
					line.add(StringUtils.removeLeadingZeroes(deedVol));
					line.add(StringUtils.removeLeadingZeroes(deedPage.split("/")[0]));
					line.add(deedDate);
					body.add(line);

					line = new ArrayList<String>();
					line.add("");
					line.add(StringUtils.removeLeadingZeroes(deedVol));
					line.add(StringUtils.removeLeadingZeroes(deedPage.split("/")[1]));
					line.add(deedDate);
					body.add(line);
				}
			} else {
				line = new ArrayList<String>();
				line.add("");
				line.add(StringUtils.removeLeadingZeroes(deedVol));

				if (deedPage.contains("-")) {
					deedPage = deedPage.substring(0, deedPage.indexOf("-"));
				}

				line.add(StringUtils.removeLeadingZeroes(deedPage));
				line.add(deedDate);
				body.add(line);
			}
		}

		if (StringUtils.isNotEmpty(docketNo)) {
			line = new ArrayList<String>();
			line.add(docketNo);
			line.add("");
			line.add("");
			line.add(deedDate);
			body.add(line);
		}

		if (body != null) {
			ResultTable rt = new ResultTable();
			String[] header = { "InstrumentNumber", "Book", "Page", "InstrumentDate" };
			rt = GenericFunctions2.createResultTable(body, header);
			m.put("SaleDataSet", rt);
		}
	}

	private static void parseSaleDataSetAtascosa(NodeList mainList, ResultMap m, String county) {

		String deedDate = null;

		try {
			Text findNode = HtmlParser3.findNode(mainList, "Deed Date");
			if (findNode != null) {
				deedDate = HtmlParser3.getValueFromNextCell(findNode, "", true);
				if (deedDate != null) {
					deedDate = deedDate.trim();
					if (deedDate.matches("\\s*/\\s*/\\s*")) {
						deedDate = "";
					}
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}

		String deedVol = HtmlParser3.getValueFromNextCell(HtmlParser3.findNode(mainList, "Deed Volume"), "", true).trim();
		deedVol = deedVol.replaceFirst("-$", "");
		String deedPage = HtmlParser3.getValueFromNextCell(HtmlParser3.findNode(mainList, "Deed Page"), "", true).trim();
		String docketNo = HtmlParser3.getValueFromNextCell(HtmlParser3.findNode(mainList, "Docket No."), "", true).trim();
		
		if ("nueces".equalsIgnoreCase(county)) {
			deedVol = deedVol.replaceAll("[^0-9]+", "");
			deedPage = deedPage.replaceAll("[^0-9]+", "");
			docketNo = docketNo.replaceAll("[^0-9]+", "");
		} else if("kendall".equals(county)){
			//task 8333
			if(docketNo.matches("\\d{2}-\\d{3}-\\w{2}")){
				docketNo = docketNo.replaceAll("(\\d{2})-(\\d{3})-\\w{2}", 20+"$1$2");
			}
		} else if ("guadalupe".equalsIgnoreCase(county)) {
			if (deedDate != null) {
				if (docketNo.matches("\\d{2}-\\d{4,6}")) {
					docketNo = docketNo.replaceFirst("\\d{2}-", "");
					docketNo = org.apache.commons.lang.StringUtils.leftPad(docketNo, 6, "0");
					docketNo = deedDate.substring(deedDate.lastIndexOf("/")+1) + docketNo;
				} 
			}
			
		} else {
			docketNo = docketNo
					.replaceFirst("[A-Z]*(?:$|&)", "");		//Travis, PIN: 736766 has Reference 2013064600TR
		}

		@SuppressWarnings("rawtypes")
		List<List> body = new ArrayList<List>();
		List<String> line = null;
		if (StringUtils.isEmpty(deedPage) && StringUtils.isNotEmpty(deedVol)) {
			line = new ArrayList<String>();
			line.add(StringUtils.removeLeadingZeroes(deedVol));
			line.add("");
			line.add("");
			line.add(org.apache.commons.lang.StringUtils.defaultString(deedDate));
			body.add(line);
		} else {
			if (StringUtils.isNotEmpty(deedVol) || StringUtils.isNotEmpty(deedPage)) {
				line = new ArrayList<String>();
				line.add("");
				line.add(StringUtils.removeLeadingZeroes(deedVol));

				if (deedPage.contains("-")) {
					deedPage = deedPage.substring(0, deedPage.indexOf("-"));
				}

				line.add(StringUtils.removeLeadingZeroes(deedPage));
				line.add(org.apache.commons.lang.StringUtils.defaultString(deedDate));
				body.add(line);
			}
		}

		if (StringUtils.isNotEmpty(docketNo)) {
			line = new ArrayList<String>();
			line.add(StringUtils.removeLeadingZeroes(docketNo));
			line.add("");
			line.add("");
			line.add(org.apache.commons.lang.StringUtils.defaultString(deedDate));
			body.add(line);
		}

		if (body != null) {
			ResultTable rt = new ResultTable();
			String[] header = { "InstrumentNumber", "Book", "Page", "InstrumentDate" };
			rt = GenericFunctions2.createResultTable(body, header);
			m.put("SaleDataSet", rt);
		}
	}

	public static void parseAndFillResultMapTXOchiltreeAO(String detailsHtml,
			ResultMap m, String county, long searchId) {
		try {
			org.htmlparser.Parser htmlParser = org.htmlparser.Parser
					.createParser(detailsHtml.replaceAll("><", ">\n<"), null);
			NodeList mainList = htmlParser.parse(null);

			String lotOrTract = "";

			lotOrTract = HtmlParser3.getValueFromNextCell(
					HtmlParser3.findNode(mainList, "Lot"), "", true).trim();

			if (!StringUtils.isEmpty(lotOrTract))
				m.put("PropertyIdentificationSet.SubdivisionLotNumber",
						StringUtils.removeLeadingZeroes(lotOrTract));

			String block = HtmlParser3.getValueFromNextCell(
					HtmlParser3.findNode(mainList, "Block"), "", true).trim();
			block = block.replaceAll("\\b[NSWE]/\\d+", "")
					.replaceAll("[\\)\\(]+", "").replaceAll("\\A0+", "")
					.replaceAll("(?ism)SEC [^(BLK)]*", "").replace("BLK", "")
					.replaceAll(" ", "");

			if (!StringUtils.isEmpty(block))
				m.put("PropertyIdentificationSet.SubdivisionBlock", block);

			if (county.equals("tarrant")) {
				String geoNo = getPidLike(mainList, ACCOUNT_NUMBER_KEY, ACCOUNT_NO_PATTERN);
				if (StringUtils.isNotEmpty(geoNo)) {
					m.put("PropertyIdentificationSet.ParcelID2", geoNo.trim());
					m.put(PropertyIdentificationSetKey.GEO_NUMBER.getKeyName(), geoNo.trim());
				}

				String pidn = getPidLike(mainList, PIDN_KEY, PIDN_PATTERN);
				pidn = org.apache.commons.lang.StringUtils.strip(pidn);
				if (StringUtils.isNotEmpty(pidn)) {
					m.put("PropertyIdentificationSet.ParcelID",
							pidn.split(" ")[0]);
				}
			} else {
				String geoNo = getPidLike(mainList, ACCOUNT_NO_KEY, ACCOUNT_NO_PATTERN);
				if (StringUtils.isNotEmpty(geoNo)) {
					m.put("PropertyIdentificationSet.ParcelID2", geoNo.trim());
					m.put(PropertyIdentificationSetKey.GEO_NUMBER.getKeyName(), geoNo.trim());
				}

				String pidn = getPidLike(mainList, PIDN_KEY, PIDN_PATTERN);
				if (StringUtils.isNotEmpty(pidn)) {
					m.put("PropertyIdentificationSet.ParcelID", pidn.trim());
				}
			}

			String total_estimated_taxes = HtmlParser3
					.getValueFromAbsoluteCell(
							0,
							1,
							HtmlParser3.findNode(mainList,
									"Total Estimated Taxes"), "", false)
					.replaceAll("[-\\s,$]", "");

			m.put(TaxHistorySetKey.BASE_AMOUNT.getKeyName(),
					total_estimated_taxes);

			String ownerName = HtmlParser3.getValueFromNextCell(
					HtmlParser3.findNode(mainList, "Owner Name"), "", true)
					.trim();
			String coOwnerName = HtmlParser3.getValueFromNextCell(
					HtmlParser3.findNode(mainList, "Mailing"), "", true).trim();
			coOwnerName = coOwnerName.replaceAll(
					"(?is)\\bUNABLE TO DELIVER\\b", "UNKNOWN");
			String[] lines = coOwnerName.split("<br>");
			boolean nameInAddressField = false;
			if (!lines[0].trim().matches("(?is).*\\d+.*")) {
				nameInAddressField = true;
				if (lines[0].trim().indexOf("BOX") == -1) {
					nameInAddressField = true;
					if (lines[0].trim().indexOf("UNKNOWN") == -1) {
						nameInAddressField = true;
					} else {
						nameInAddressField = false;
					}
				} else {
					nameInAddressField = false;
				}
			}
			if (nameInAddressField) {
				lines[0] = lines[0].trim().replaceAll("(?is)\\bOF THE\\b", "");
				if (ownerName.endsWith("&") || lines[0].trim().startsWith("&")
						|| lines[0].trim().startsWith("AND ")) {
					ownerName += " " + lines[0].trim();
				} else {
					ownerName += " & " + lines[0].trim();
				}
				if (lines[1].trim().startsWith("%")) {
					ownerName += " & " + lines[1].trim();
				}
			}
			ownerName = ownerName.replaceAll("\\s*&\\s*&\\s*", " & ");

			m.put("tmpOwner", StringUtils.isNotEmpty(ownerName) ? ownerName
					: "");

			String legal = HtmlParser3.getValueFromNextCell(
					HtmlParser3.findNode(mainList, "Legal"), "", true).trim();
			m.put("tmpLegal", legal);

			String siteAddress = HtmlParser3.getValueFromNextCell(
					HtmlParser3.findNode(mainList, "Location"), "", true)
					.trim();
			if (StringUtils.isNotEmpty(siteAddress)) {
				m.put("tmpAddress", siteAddress);
			}

			if (county.equals("tarrant")) {
				String landAppraisal = HtmlParser3.getValueFromNextCell(
						HtmlParser3.findNode(mainList, "Land Value"), "", true)
						.trim();
				landAppraisal = landAppraisal.replaceAll("[\\$,]", "");
				m.put("PropertyAppraisalSet.LandAppraisal", landAppraisal);

				String improvementAppraisal = HtmlParser3
						.getValueFromNextCell(
								HtmlParser3.findNode(mainList,
										"Improvement Value"), "", true)
						.replaceAll("[\\$,]", "").trim();

				m.put(PropertyAppraisalSetKey.IMPROVEMENT_APPRAISAL
						.getKeyName(), improvementAppraisal);
			} else {
				NodeList tables = mainList.extractAllNodesThatMatch(
						new TagNameFilter("table"), true)
						.extractAllNodesThatMatch(
								new HasAttributeFilter("bgcolor", "#D0D0D0"),
								true);

				TableTag value_info = getTableFromNodeList(tables,
						new String[] { "Land Value", "Total Value" });

				String landAppraisal = value_info
						.toHtml()
						.replaceAll(
								"(?ism).*<td class=\"reports_defred\">Land Value</td>[^<]*<td align=\"right\" width=\"1%\">([^<]*)</td>.*",
								"$1").replaceAll("[\\$,]", "");

				if (landAppraisal.length() < 20)
					m.put(PropertyAppraisalSetKey.LAND_APPRAISAL.getKeyName(),
							landAppraisal);

				String improvementAppraisal = value_info
						.toHtml()
						.replaceAll(
								"(?ism).*<td class=\"reports_defred\">Improvement Value</td>[^<]*<td align=\"right\" width=\"1%\">([^<]*)</td>.*",
								"$1").replaceAll("[\\$,]", "").trim();

				if (improvementAppraisal.length() < 20)
					m.put(PropertyAppraisalSetKey.IMPROVEMENT_APPRAISAL
							.getKeyName(), improvementAppraisal);

				String assessed = value_info
						.toHtml()
						.replaceAll(
								"(?ism).*<td class=\"reports_defred\">Assessed Value</td>[^<]*<td align=\"right\" width=\"1%\">([^<]*)</td>.*",
								"$1").replaceAll("[\\$,]", "").trim();

				if (assessed.length() < 20)
					m.put(PropertyAppraisalSetKey.TOTAL_ASSESSMENT.getKeyName(),
							assessed);

				String apprisal = value_info
						.toHtml()
						.replaceAll(
								"(?ism).*<td class=\"reports_defred\">Total Value</td>[^<]*<td align=\"right\" width=\"1%\" class=\"red_b\">([^<]*)</td>.*",
								"$1").replaceAll("[\\$,]", "").trim();

				if (apprisal.length() < 20)
					m.put(PropertyAppraisalSetKey.TOTAL_APPRAISAL.getKeyName(),
							apprisal);
			}

			String docketNo = HtmlParser3.getValueFromNextCell(
					HtmlParser3.findNode(mainList, "Docket No."), "", true)
					.trim();

			if (!StringUtils.isEmpty(docketNo))
				m.put(SaleDataSetKey.INSTRUMENT_NUMBER.getKeyName(), docketNo);

			if (county.equals("tarrant")) {
				String deedVol = HtmlParser3
						.getValueFromNextCell(
								HtmlParser3.findNode(mainList, "Deed Volume"),
								"", true).trim();
				String deedPage = HtmlParser3.getValueFromNextCell(
						HtmlParser3.findNode(mainList, "Deed Page"), "", true)
						.trim();

				@SuppressWarnings("rawtypes")
				List<List> body = new ArrayList<List>();
				List<String> line = new ArrayList<String>();
				if (!StringUtils.isEmpty(deedPage)
						|| !StringUtils.isEmpty(deedVol)) {
					line.add(StringUtils.removeLeadingZeroes(deedVol));
					line.add(StringUtils.removeLeadingZeroes(deedPage));
					body.add(line);
				}

				if (body != null && body.size() > 0) {
					ResultTable rt = new ResultTable();
					String[] header = { "Book", "Page" };
					rt = GenericFunctions2.createResultTable(body, header);
					m.put("SaleDataSet", rt);
				}
			}

			try {

				parseAddressTXGenericTaxNetAO(m, county, searchId);
				parseLegalTXGenericTaxNetAO(m, county, searchId);
				if (county.equals("tarrant"))
					partyNamesTXAransasAO(m, county, searchId);
				else
					partyNamesTXGenericTaxNetAO(m, county, searchId);

			} catch (Exception e) {
				e.printStackTrace();
			}

			m.removeTempDef();

		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	private static void parseAndFillResultMapTXBowieAO(String detailsHtml,
			ResultMap m, String county, long searchId) {
		try {
			org.htmlparser.Parser htmlParser = org.htmlparser.Parser
					.createParser(detailsHtml, null);
			NodeList mainList = htmlParser.parse(null);

			String account_nr = getPidLike(mainList, LONG_ACCOUNT_NUMBER_KEY, LONG_ACCOUNT_NUMBER_PATTERN);
			if (StringUtils.isNotEmpty(account_nr)) {
				m.put(PropertyIdentificationSetKey.PARCEL_ID2.getKeyName(), account_nr.trim());
			}

			String pidn = getPidLike(mainList, PIDN_KEY, PIDN_PATTERN); 
			if (StringUtils.isNotEmpty(pidn)) {
				m.put(PropertyIdentificationSetKey.PARCEL_ID.getKeyName(), pidn.trim());
			}

			String total_estimated_taxes = HtmlParser3
					.getValueFromAbsoluteCell(
							0,
							1,
							HtmlParser3.findNode(mainList,
									"Total Estimated Taxes"), "", false)
					.replaceAll("[-\\s,$]", "");

			m.put(TaxHistorySetKey.BASE_AMOUNT.getKeyName(),
					total_estimated_taxes);

			// get owner table
			NodeList owner_t_list = mainList
					.extractAllNodesThatMatch(new TagNameFilter("table"), true)
					.extractAllNodesThatMatch(
							new HasAttributeFilter("width", "100%"), true)
					.extractAllNodesThatMatch(
							new HasAttributeFilter("cellspacing", "1"), true)
					.extractAllNodesThatMatch(
							new HasAttributeFilter("cellpadding", "8"), true)
					.extractAllNodesThatMatch(
							new HasAttributeFilter("bgcolor", "#D0D0D0"), true);

			if (owner_t_list.size() > 0) {

				TableTag owners = (TableTag) owner_t_list.elementAt(0);

				if (owners != null && owners.getRows().length <= 4) {
					String ownerName = HtmlParser3.getValueFromNextCell(
							HtmlParser3.findNode(mainList, "Owner"), "", true)
							.trim();

					ownerName = ownerName
							.replaceAll(
									"(?ism)<span class=\"bold_blue_gray\">([^<]*)</span>.*",
									"$1");
					ownerName = ownerName.replaceAll("\\s*&\\s*&*\\s*", " & ")
							.toUpperCase();

					m.put("tmpOwner",
							StringUtils.isNotEmpty(ownerName) ? ownerName : "");
				}
			}

			String legal = HtmlParser3.getValueFromNextCell(
					HtmlParser3.findNode(mainList, "Legal"), "", true).trim();
			m.put("tmpLegal", legal);

			String siteAddress = HtmlParser3
					.getValueFromNextCell(
							HtmlParser3.findNode(mainList, "Location"), "",
							true).replaceAll("\\s+", " ").trim();
			if (StringUtils.isNotEmpty(siteAddress)) {
				m.put("tmpAddress", siteAddress);
			}

			String improvementAppraisal = HtmlParser3.getValueFromNextCell(
					HtmlParser3.findNode(mainList, "Improvement Value"), "",
					true).trim();
			improvementAppraisal = improvementAppraisal
					.replaceAll("[\\$,]", "");
			m.put(PropertyAppraisalSetKey.IMPROVEMENT_APPRAISAL.getKeyName(),
					improvementAppraisal);

			String land_value = HtmlParser3.getValueFromNextCell(
					HtmlParser3.findNode(mainList, "Land Value"), "", true)
					.trim();
			land_value = land_value.replaceAll("[\\$,]", "");
			m.put(PropertyAppraisalSetKey.LAND_APPRAISAL.getKeyName(),
					land_value);

			String apprisal = HtmlParser3.getValueFromNextCell(
					HtmlParser3.findNode(mainList, "Total Assessed Value"), "",
					true).trim();
			// assessed = assessed.replaceAll("(?is)([\\d,]+).*", "$1");
			m.put(PropertyAppraisalSetKey.TOTAL_ASSESSMENT.getKeyName(),
					apprisal.replaceAll("[-$,]", ""));

			try {
				parseAddressTXBowieAO(m, county, searchId);
				parseLegalTXGenericTaxNetAO(m, county, searchId);
				partyNamesTXBowieAO(m, county, searchId);

			} catch (Exception e) {
				e.printStackTrace();
			}

			m.removeTempDef();

		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public static void parseAndFillResultMapTXBriscoeAO(String detailsHtml,
			ResultMap m, String county, long searchId) {
		try {
			org.htmlparser.Parser htmlParser = org.htmlparser.Parser.createParser(detailsHtml, null);
			NodeList mainList = htmlParser.parse(null);

			String pidn = getPidLike(mainList, ACCOUNT_NO_KEY, ACCOUNT_NO_PATTERN);

			if (StringUtils.isNotEmpty(pidn)) {
				m.put(PropertyIdentificationSetKey.PARCEL_ID.getKeyName(), pidn.trim());
			}

			String total_estimated_taxes = HtmlParser3
					.getValueFromAbsoluteCell(
							0,
							1,
							HtmlParser3.findNode(mainList,
									"Total Estimated Taxes"), "", false)
					.replaceAll("[-\\s,$]", "");

			m.put(TaxHistorySetKey.BASE_AMOUNT.getKeyName(), total_estimated_taxes);

			String ownerName = HtmlParser3.getValueFromNextCell(HtmlParser3.findNode(mainList, "Owner Name"), "", true).trim();
			String coOwnerName = HtmlParser3.getValueFromNextCell(HtmlParser3.findNode(mainList, "Mailing"), "", true).trim();
			
			coOwnerName = coOwnerName.replaceAll("(?is)\\bUNABLE TO DELIVER\\b", "UNKNOWN");
			String[] lines = coOwnerName.split("<br>");
			boolean nameInAddressField = false;
			if (!lines[0].trim().matches("(?is).*\\d+.*")) {
				nameInAddressField = true;
				if (lines[0].trim().indexOf("BOX") == -1) {
					nameInAddressField = true;
					if (lines[0].trim().indexOf("UNKNOWN") == -1) {
						nameInAddressField = true;
					} else {
						nameInAddressField = false;
					}
				} else {
					nameInAddressField = false;
				}
			}
			if (nameInAddressField) {
				lines[0] = lines[0].trim().replaceAll("(?is)\\bOF THE\\b", "");
				if (ownerName.endsWith("&") || lines[0].trim().startsWith("&")
						|| lines[0].trim().startsWith("AND ")) {
					ownerName += " " + lines[0].trim();
				} else {
					ownerName += " & " + lines[0].trim();
				}
				if (lines[1].trim().startsWith("%")) {
					ownerName += " & " + lines[1].trim();
				}
			}
			ownerName = ownerName.replaceAll("\\s*&\\s*&\\s*", " & ");

			m.put("tmpOwner", StringUtils.isNotEmpty(ownerName) ? ownerName : "");

			String legal = HtmlParser3.getValueFromNextCell(
					HtmlParser3.findNode(mainList, "Legal"), "", true).trim();
			m.put("tmpLegal", legal);

			String siteAddress = HtmlParser3.getValueFromNextCell(
					HtmlParser3.findNode(mainList, "Location"), "", true)
					.trim();
			if (StringUtils.isNotEmpty(siteAddress)) {
				m.put("tmpAddress", siteAddress);
			}

			
			
			NodeList reportsDefredTdList = mainList.extractAllNodesThatMatch(new TagNameFilter("td"), true)
					.extractAllNodesThatMatch(new HasAttributeFilter("class", "reports_defred"));
			
			for (int i = 0; i < reportsDefredTdList.size(); i++) {
				TableColumn column = (TableColumn)reportsDefredTdList.elementAt(i);
				String text = column.toPlainTextString().trim();
				if("Lot".equals(text)) {
					String plainValue = HtmlParser3.getValueFromNextCell(column, null, false);
					m.put(PropertyIdentificationSetKey.SUBDIVISION_LOT_NUMBER.getKeyName(), 
							org.apache.commons.lang.StringUtils.defaultString(plainValue));
				} else if ("Block".equals(text)) {
					String plainValue = HtmlParser3.getValueFromNextCell(column, null, false);
					m.put(PropertyIdentificationSetKey.SUBDIVISION_BLOCK.getKeyName(), 
							org.apache.commons.lang.StringUtils.defaultString(plainValue));
				} else if ("Land Value".equals(text)) {
					String plainValue = HtmlParser3.getValueFromNextCell(column, null, false);
					if(plainValue != null) {
						plainValue = plainValue.replaceAll("[\\$,]", "").trim();
					}
					m.put(PropertyAppraisalSetKey.LAND_APPRAISAL.getKeyName(), 
							org.apache.commons.lang.StringUtils.defaultString(plainValue));
				} else if ("Improvement Value".equals(text)) {
					String plainValue = HtmlParser3.getValueFromNextCell(column, null, false);
					if(plainValue != null) {
						plainValue = plainValue.replaceAll("[\\$,]", "").trim();
					}
					m.put(PropertyAppraisalSetKey.IMPROVEMENT_APPRAISAL.getKeyName(), 
							org.apache.commons.lang.StringUtils.defaultString(plainValue));
				} else if ("Assessed Value".equals(text)) {
					String plainValue = HtmlParser3.getValueFromNextCell(column, null, false);
					if(plainValue != null) {
						plainValue = plainValue.replaceAll("[\\$,]", "").trim();
					}
					m.put(PropertyAppraisalSetKey.TOTAL_ASSESSMENT.getKeyName(), 
							org.apache.commons.lang.StringUtils.defaultString(plainValue));
				}
			}
			

			try {
				parseAddressTXBriscoeAO(m, county, searchId);
				parseLegalTXGenericTaxNetAO(m, county, searchId);
				partyNamesTXBriscoeAO(m, county, searchId);

			} catch (Exception e) {
				e.printStackTrace();
			}

			m.removeTempDef();

		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public static void parseAndFillResultMapTXCallahanAO(String detailsHtml,
			ResultMap m, String county, long searchId) {

	}

	public static void parseAndFillResultMapTXDallasAO(String detailsHtml,
			ResultMap m, String county, long searchId) {
		try {
			org.htmlparser.Parser htmlParser = org.htmlparser.Parser
					.createParser(detailsHtml, null);
			NodeList mainList = htmlParser.parse(null);

			String pidn = "";

			if (county.equals("jimwells")) {
				pidn = getPidLike(mainList, LONG_ACCOUNT_NUMBER_KEY, LONG_ACCOUNT_NUMBER_PATTERN);
			} else {
				pidn = getPidLike(mainList, ACCOUNT_NUMBER_KEY, ACCOUNT_NUMBER_PATTERN);
			}

			if (StringUtils.isNotEmpty(pidn)) {
				m.put("PropertyIdentificationSet.ParcelID", pidn.trim());
			}

			String total_estimated_taxes = HtmlParser3
					.getValueFromAbsoluteCell(
							0,
							1,
							HtmlParser3.findNode(mainList,
									"Total Estimated Taxes"), "", false)
					.replaceAll("[-\\s,$]", "");

			m.put(TaxHistorySetKey.BASE_AMOUNT.getKeyName(),
					total_estimated_taxes);

			String ownerName = HtmlParser3.getValueFromNextCell(
					HtmlParser3.findNode(mainList, "Owner Name"), "", true)
					.trim();
			String coOwnerName = HtmlParser3.getValueFromNextCell(
					HtmlParser3.findNode(mainList, "Mailing"), "", true).trim();
			coOwnerName = coOwnerName.replaceAll(
					"(?is)\\bUNABLE TO DELIVER\\b", "UNKNOWN");
			String[] lines = coOwnerName.split("<br>");
			boolean nameInAddressField = false;
			if (!lines[0].trim().matches("(?is).*\\d+.*")) {
				nameInAddressField = true;
				if (lines[0].trim().indexOf("BOX") == -1) {
					nameInAddressField = true;
					if (lines[0].trim().indexOf("UNKNOWN") == -1) {
						nameInAddressField = true;
					} else {
						nameInAddressField = false;
					}
				} else {
					nameInAddressField = false;
				}
			}
			if (nameInAddressField) {
				lines[0] = lines[0].trim().replaceAll("(?is)\\bOF THE\\b", "");
				if (ownerName.endsWith("&") || lines[0].trim().startsWith("&")
						|| lines[0].trim().startsWith("AND ")) {
					ownerName += " " + lines[0].trim();
				} else {
					ownerName += " & " + lines[0].trim();
				}
				if (lines[1].trim().startsWith("%")) {
					ownerName += " & " + lines[1].trim();
				}
			}

			ownerName = ownerName.replaceAll("\\s*&\\s*&\\s*", " & ");

			m.put("tmpOwner", StringUtils.isNotEmpty(ownerName) ? ownerName
					: "");

			String legal = HtmlParser3.getValueFromNextCell(
					HtmlParser3.findNode(mainList, "Legal"), "", true).trim();
			m.put("tmpLegal", legal);

			String siteAddress = HtmlParser3.getValueFromNextCell(
					HtmlParser3.findNode(mainList, "Location"), "", true)
					.trim();
			if (StringUtils.isNotEmpty(siteAddress)) {
				m.put("tmpAddress", siteAddress);
			}

			String landAppraisal = HtmlParser3.getValueFromNextCell(
					HtmlParser3.findNode(mainList, "Land Value"), "", true)
					.trim();
			landAppraisal = landAppraisal.replaceAll("[\\$,]", "");
			m.put("PropertyAppraisalSet.LandAppraisal", landAppraisal);

			String improvementAppraisal = HtmlParser3
					.getValueFromNextCell(
							HtmlParser3.findNode(mainList, "Improvement Value"),
							"", true).replaceAll("[\\$,]", "").trim();

			m.put(PropertyAppraisalSetKey.IMPROVEMENT_APPRAISAL.getKeyName(),
					improvementAppraisal);

			if (!county.equals("jimwells")) {
				String appraisal = HtmlParser3
						.getValueFromNextCell(
								HtmlParser3.findNode(mainList, "Total Value"),
								"", true).trim();
				m.put(PropertyAppraisalSetKey.TOTAL_APPRAISAL.getKeyName(),
						appraisal.replaceAll("[-$,]", ""));
			}

			if (county.equals("jimwells")) {
				String assessed = HtmlParser3.getValueFromNextCell(
						HtmlParser3.findNode(mainList, "Total Assessed Value"),
						"", true).trim();
				m.put(PropertyAppraisalSetKey.TOTAL_APPRAISAL.getKeyName(),
						assessed.replaceAll("[-$,]", ""));

				String deedVol = HtmlParser3
						.getValueFromNextCell(
								HtmlParser3.findNode(mainList, "Deed Volume"),
								"", true).trim();
				String deedPage = HtmlParser3.getValueFromNextCell(
						HtmlParser3.findNode(mainList, "Deed Page"), "", true)
						.trim();

				@SuppressWarnings("rawtypes")
				List<List> body = new ArrayList<List>();
				List<String> line = new ArrayList<String>();
				if (!StringUtils.isEmpty(deedPage)
						|| !StringUtils.isEmpty(deedVol)) {
					line.add(deedVol);
					line.add(deedVol);
					body.add(line);
				}
				if (body != null && body.size() > 0) {
					ResultTable rt = new ResultTable();
					String[] header = { "Book", "Page" };
					rt = GenericFunctions2.createResultTable(body, header);
					m.put("SaleDataSet", rt);
				}
			}

			if (county.equals("tomgreen")) {
				String deedPage = HtmlParser3.getValueFromNextCell(
						HtmlParser3.findNode(mainList, "Deed Page"), "", true)
						.trim();

				if (!StringUtils.isEmpty(deedPage))
					m.put(SaleDataSetKey.PAGE.getKeyName(), deedPage);
			}

			try {
				parseAddressTXDallasAO(m, county, searchId);
				parseLegalTXGenericTaxNetAO(m, county, searchId);
				partyNamesTXDallasAO(m, county, searchId);

			} catch (Exception e) {
				e.printStackTrace();
			}

			m.removeTempDef();

		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public static void parseAndFillResultMapTXPotterAO(String detailsHtml,
			ResultMap m, String county, long searchId) {
		try {
			org.htmlparser.Parser htmlParser = org.htmlparser.Parser
					.createParser(detailsHtml, null);
			NodeList mainList = htmlParser.parse(null);

			if (county.equals("jefferson")) {
				String pidn = getPidLike(mainList, LONG_ACCOUNT_NUMBER_KEY, LONG_ACCOUNT_NUMBER_PATTERN);
				if (StringUtils.isNotEmpty(pidn)) {
					m.put("PropertyIdentificationSet.ParcelID", pidn.trim());
				}
			} else if (county.equals("harris")) {
				String pidn = getPidLike(mainList, ACCOUNT_NUMBER_KEY, ACCOUNT_NUMBER_PATTERN);
				if (StringUtils.isNotEmpty(pidn)) {
					m.put("PropertyIdentificationSet.ParcelID", pidn.trim());
				}
			} else {
				String pidn = getPidLike(mainList, PIDN_KEY, PIDN_PATTERN);
				if (StringUtils.isNotEmpty(pidn)) {
					m.put("PropertyIdentificationSet.ParcelID", pidn.trim());
				}
			}

			String total_estimated_taxes = HtmlParser3
					.getValueFromAbsoluteCell(
							0,
							1,
							HtmlParser3.findNode(mainList,
									"Total Estimated Taxes"), "", false)
					.replaceAll("[-\\s,$]", "");

			m.put(TaxHistorySetKey.BASE_AMOUNT.getKeyName(),
					total_estimated_taxes);

			String ownerName = HtmlParser3.getValueFromNextCell(
					HtmlParser3.findNode(mainList, "Owner Name"), "", true)
					.trim();

			ownerName = ownerName.replaceAll("<br[^>]*>", " & ").replaceAll(
					"\\s*&\\s*&\\s*", " & ");

			m.put("tmpOwner", StringUtils.isNotEmpty(ownerName) ? ownerName
					: "");

			String legal = HtmlParser3.getValueFromNextCell(
					HtmlParser3.findNode(mainList, "Legal"), "", true).trim();
			m.put("tmpLegal", legal);

			String siteAddress = HtmlParser3
					.getValueFromNextCell(
							HtmlParser3.findNode(mainList, "Location"), "",
							true).replaceAll("\\s+", " ").trim();
			if (StringUtils.isNotEmpty(siteAddress)) {
				m.put("tmpAddress", siteAddress);
			}

			String landAppraisal = HtmlParser3.getValueFromNextCell(
					HtmlParser3.findNode(mainList, "Land Value"), "", true)
					.trim();
			landAppraisal = landAppraisal.replaceAll("[\\$,]", "");
			m.put("PropertyAppraisalSet.LandAppraisal", landAppraisal);

			String improvementAppraisal = HtmlParser3
					.getValueFromNextCell(
							HtmlParser3.findNode(mainList, "Improvement Value"),
							"", true).replaceAll("[\\$,]", "").trim();

			m.put(PropertyAppraisalSetKey.IMPROVEMENT_APPRAISAL.getKeyName(),
					improvementAppraisal);

			String assessed = HtmlParser3.getValueFromNextCell(
					HtmlParser3.findNode(mainList, "Total Value"), "", true)
					.trim();
			m.put(PropertyAppraisalSetKey.TOTAL_APPRAISAL.getKeyName(),
					assessed.replaceAll("[-$,]", ""));

			if (!county.equals("jefferson") && !county.equals("harris")) {
				String deedVol = HtmlParser3.getValueFromNextCell(
						HtmlParser3.findNode(mainList, "Volumne"), "", true)
						.trim();

				String deedPage = HtmlParser3.getValueFromNextCell(
						HtmlParser3.findNode(mainList, "Page"), "", true)
						.trim();

				@SuppressWarnings("rawtypes")
				List<List> body = new ArrayList<List>();
				List<String> line = new ArrayList<String>();
				if (!StringUtils.isEmpty(deedPage)
						|| !StringUtils.isEmpty(deedVol)) {
					line.add(StringUtils.removeLeadingZeroes(deedVol));
					line.add(StringUtils.removeLeadingZeroes(deedPage));
					body.add(line);
				}

				if (body != null && body.size() > 0) {
					ResultTable rt = new ResultTable();
					String[] header = { "Book", "Page" };
					rt = GenericFunctions2.createResultTable(body, header);
					m.put("SaleDataSet", rt);
				}
			}
			try {
				if (county.equals("harris"))
					parseAddressTXPotterAO(m, county, searchId);
				else
					parseAddressTXPotterAO(m, county, searchId);
				parseLegalTXGenericTaxNetAO(m, county, searchId);
				partyNamesTXPotterAO(m, county, searchId);
			} catch (Exception e) {
				e.printStackTrace();
			}

			m.removeTempDef();

		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public static ResultMap parseIntermediaryRowTXAndersonAO(TableRow row,
			String county, long searchId) throws Exception {
		ResultMap resultMap = new ResultMap();
		resultMap.put("OtherInformationSet.SrcType", "AO");

		TableColumn[] cols = row.getColumns();
		int count = 0;
		String contents = "";
		if (cols.length == 4)
			for (TableColumn col : cols) {

				if (count < 3) {
					switch (count) {
					case 0:
						if (col.getChildren() != null)
							contents = col.getChildren().elementAt(0)
									.getChildren().toHtml();
						contents = contents.replaceAll(
								"(?is)\\s*<a[^>]+>([^<]*)</a>.*", "$1");
						resultMap.put("PropertyIdentificationSet.ParcelID",
								contents.trim());

						break;
					case 1:
						if (col.getChildren() != null)
							contents = col.getChildren().elementAt(0).toHtml();
						if (StringUtils.isNotEmpty(contents)) {
							resultMap.put("tmpOwner", contents);
						}
						break;
					case 2:
						if (col.getChildren() != null)
							contents = col.getChildren().elementAt(0).toHtml();
						if (StringUtils.isNotEmpty(contents)) {
							resultMap.put("tmpAddress", contents.trim());
						}
						break;
					default:
						break;
					}
					count++;
				} else
					break;
			}
		else
			for (TableColumn col : cols) {
				if (count < 4) {
					switch (count) {
					case 0:
						if (col.getChildren() != null)
							contents = col.getChildren().elementAt(0)
									.getChildren().toHtml();
						contents = contents.replaceAll(
								"(?is)\\s*<a[^>]+>([^<]*)</a>.*", "$1");
						resultMap.put("PropertyIdentificationSet.ParcelID",
								contents.trim());

						break;
					case 1:
						if (col.getChildren() != null)
							contents = col.getChildren().elementAt(0).toHtml();
						resultMap.put("PropertyIdentificationSet.ParcelID2",
								contents.trim());
						resultMap.put(PropertyIdentificationSetKey.GEO_NUMBER.getKeyName(),
								contents.trim());
						break;
					case 2:
						if (col.getChildren() != null)
							contents = col.getChildren().elementAt(0).toHtml();
						if (StringUtils.isNotEmpty(contents)) {
							resultMap.put("tmpOwner", contents);
						}
						break;
					case 3:
						if (col.getChildren() != null)
							contents = col.getChildren().elementAt(0).toHtml();
						if (StringUtils.isNotEmpty(contents)) {
							resultMap.put("tmpAddress", contents.trim());
						}
						break;
					default:
						break;
					}
					count++;
				} else
					break;
			}
		partyNamesTXGenericTaxNetAO(resultMap, county, searchId);
		parseAddressTXAndersonAO(resultMap, county, searchId);

		resultMap.removeTempDef();

		return resultMap;
	}

	public static ResultMap parseIntermediaryRowTXAransasAO(TableRow row,
			String county, long searchId) throws Exception {
		ResultMap resultMap = new ResultMap();
		resultMap.put("OtherInformationSet.SrcType", "AO");

		TableColumn[] cols = row.getColumns();
		int count = 0;
		String contents = "";
		for (TableColumn col : cols) {

			if (count < 3) {
				switch (count) {
				case 0:
					if (col.getChildren() != null)
						contents = col.getChildren().elementAt(0).getChildren()
								.toHtml();
					contents = contents.replaceAll(
							"(?is)\\s*<a[^>]+>([^<]*)</a>.*", "$1");
					resultMap.put("PropertyIdentificationSet.ParcelID",
							contents.trim());

					break;
				case 1:
					if (col.getChildren() != null)
						contents = col.getChildren().elementAt(0).toHtml();
					if (StringUtils.isNotEmpty(contents)) {
						resultMap.put("tmpOwner", contents);
					}
					break;
				case 2:
					if (col.getChildren() != null)
						contents = col.getChildren().elementAt(0).toHtml();
					if (StringUtils.isNotEmpty(contents)) {
						resultMap.put("tmpAddress", contents.trim());
					}
					break;
				default:
					break;
				}
				count++;
			} else
				break;
		}

		partyNamesTXGenericTaxNetAO(resultMap, county, searchId);
		parseAddressTXGenericTaxNetAO(resultMap, county, searchId);

		resultMap.removeTempDef();

		return resultMap;
	}

	public static ResultMap parseIntermediaryRowTXArcherAO(TableRow row,
			String county, long searchId) throws Exception {
		return parseIntermediaryRowTXAransasAO(row, county, searchId);
	}

	public static ResultMap parseIntermediaryRowTXArmstrongAO(TableRow row,
			String county, long searchId) throws Exception {
		ResultMap resultMap = new ResultMap();
		resultMap.put("OtherInformationSet.SrcType", "AO");

		TableColumn[] cols = row.getColumns();
		int count = 0;
		String contents = "";
		for (TableColumn col : cols) {

			if (count < 3) {
				switch (count) {
				case 0:
					if (col.getChildren() != null)
						contents = col.getChildren().elementAt(0).getChildren()
								.toHtml();
					contents = contents.replaceAll(
							"(?is)\\s*<a[^>]+>([^<]*)</a>.*", "$1");
					resultMap.put("PropertyIdentificationSet.ParcelID",
							contents.trim());

					break;
				case 1:
					if (col.getChildren() != null)
						contents = col.getChildren().elementAt(0).toHtml();
					if (StringUtils.isNotEmpty(contents)) {
						resultMap.put("tmpOwner", contents);
					}
					break;
				case 2:
					if (col.getChildren() != null)
						contents = col.getChildren().elementAt(0).toHtml();
					if (StringUtils.isNotEmpty(contents)) {
						resultMap.put("tmpAddress", contents.trim());
					}
					break;
				default:
					break;
				}
				count++;
			} else
				break;
		}

		partyNamesTXGenericTaxNetAO(resultMap, county, searchId);
		parseAddressTXGenericTaxNetAO(resultMap, county, searchId);

		resultMap.removeTempDef();

		return resultMap;
	}

	public static ResultMap parseIntermediaryRowTXAtascosaAO(TableRow row,
			String[] headers, String county, long searchId) throws Exception {

		ResultMap resultMap = new ResultMap();
		resultMap.put(OtherInformationSetKey.SRC_TYPE.getKeyName(), "AO");

		TableColumn[] cols = row.getColumns();

		for (int i = 0; i < headers.length; i++) {
			
			switch (headers[i]) {
			case "Account Number":
				if ("brazoria".equalsIgnoreCase(county) || "guadalupe".equalsIgnoreCase(county)) {
					loadInterPlainText(county, resultMap, cols[i], 
							PropertyIdentificationSetKey.PARCEL_ID2.getKeyName(), 
							PropertyIdentificationSetKey.GEO_NUMBER.getKeyName());
				} else {
					loadInterPlainText(county, resultMap, cols[i], 
							PropertyIdentificationSetKey.PARCEL_ID.getKeyName());						
				}
				break;
			case "Geo No.":
				if ("brazoria".equalsIgnoreCase(county) || "guadalupe".equalsIgnoreCase(county)) {
					loadInterPlainText(county, resultMap, cols[i], 
							PropertyIdentificationSetKey.PARCEL_ID.getKeyName());
				} else {
					loadInterPlainText(county, resultMap, cols[i], 
							PropertyIdentificationSetKey.PARCEL_ID2.getKeyName(), 
							PropertyIdentificationSetKey.GEO_NUMBER.getKeyName());
				}
				break;
			case "Owner Name":
				loadInterHtml(county, resultMap, cols[i], "tmpOwner");
				break;
			case "Address":
				loadInterHtml(county, resultMap, cols[i], "tmpAddress");
				break;
			default:
				break;
			}
		}
		
		
		
//		//do it old style
//		for (int i = 0; i < cols.length; i++) {
//			switch (i) {
//			case 0:
//				if ("brazoria".equalsIgnoreCase(county) || "guadalupe".equalsIgnoreCase(county)) {
//					loadInterPlainText(county, resultMap, cols[i], 
//							PropertyIdentificationSetKey.PARCEL_ID2.getKeyName(), 
//							PropertyIdentificationSetKey.GEO_NUMBER.getKeyName());
//				} else {
//					loadInterPlainText(county, resultMap, cols[i], 
//							PropertyIdentificationSetKey.PARCEL_ID.getKeyName());						
//				}
//				break;
//			case 1:
//				if ("brazoria".equalsIgnoreCase(county) || "guadalupe".equalsIgnoreCase(county)) {
//					loadInterPlainText(county, resultMap, cols[i], 
//							PropertyIdentificationSetKey.PARCEL_ID.getKeyName());
//				} else {
//					loadInterPlainText(county, resultMap, cols[i], 
//							PropertyIdentificationSetKey.PARCEL_ID2.getKeyName(), 
//							PropertyIdentificationSetKey.GEO_NUMBER.getKeyName());
//				}
//				
//				break;
//			case 2:
//				loadInterHtml(county, resultMap, cols[i], "tmpOwner");
//				break;
//			case 3:
//				loadInterHtml(county, resultMap, cols[i], "tmpAddress");
//				break;
//			default:
//				break;
//			}
//		}

		if (BAILEY_2_LIST.contains(county))
			parseAddressTXBaileyAO(resultMap, county, searchId);
		else
			parseAddressTXAtascosaAO(resultMap, county, searchId);
		partyNamesTXGenericTaxNetAO(resultMap, county, searchId);

		resultMap.removeTempDef();

		return resultMap;
	}

	protected static String loadInterPlainText(String county, ResultMap resultMap, TableColumn col, String ... keys) {
		String contents = col.toPlainTextString().trim();
		if (StringUtils.isNotEmpty(contents)) {
			for (String key : keys) {
				resultMap.put(key, contents);
			}
		}
		return contents;
	}
	
	protected static String loadInterHtml(String county, ResultMap resultMap, TableColumn col, String ... keys) {
		String contents = col.toPlainTextString().trim();
		for (String key : keys) {
			resultMap.put(key, contents);
		}
		return contents;
	}

	protected static String loadInterPIN(String county, ResultMap resultMap, TableColumn col) {
		String contents = col.toPlainTextString().trim();
		if ("brazoria".equalsIgnoreCase(county)||"guadalupe".equalsIgnoreCase(county)) {
			resultMap.put(PropertyIdentificationSetKey.PARCEL_ID2.getKeyName(), contents);
			resultMap.put(PropertyIdentificationSetKey.GEO_NUMBER.getKeyName(), contents);
		} else {
			resultMap.put(PropertyIdentificationSetKey.PARCEL_ID.getKeyName(), contents);
		}
		return contents;
	}

	public static ResultMap parseIntermediaryRowTXBaileyAO(TableRow row,
			String county, long searchId) throws Exception {
		return parseIntermediaryRowTXAransasAO(row, county, searchId);
	}

	public static ResultMap parseIntermediaryRowTXBowieAO(TableRow row,
			String county, long searchId) throws Exception {
		ResultMap resultMap = new ResultMap();
		resultMap.put("OtherInformationSet.SrcType", "AO");

		TableColumn[] cols = row.getColumns();
		int count = 0;
		String contents = "";
		for (TableColumn col : cols) {

			if (count < 4) {
				switch (count) {
				case 0:
					if (col.getChildren() != null)
						contents = col.getChildren().elementAt(0).getChildren()
								.toHtml();
					contents = contents.replaceAll(
							"(?is)\\s*<a[^>]+>([^<]*)</a>.*", "$1");
					resultMap.put("PropertyIdentificationSet.ParcelID2",
							contents.trim());
					resultMap.put(PropertyIdentificationSetKey.GEO_NUMBER.getKeyName(),
							contents.trim());

					break;
				case 1:
					if (col.getChildren() != null)
						contents = col.getChildren().elementAt(0).toHtml();
					if (StringUtils.isNotEmpty(contents)) {
						resultMap.put("tmpOwner", contents);
					}
					break;
				case 2:
					if (col.getChildren() != null)
						contents = col.getChildren().elementAt(0).toHtml();
					if (StringUtils.isNotEmpty(contents)) {
						resultMap.put("tmpAddress", contents.trim());
					}
					break;
				default:
					break;
				}
				count++;
			} else
				break;
		}

		partyNamesTXGenericTaxNetAO(resultMap, county, searchId);
		parseAddressTXBowieAO(resultMap, county, searchId);

		resultMap.removeTempDef();

		return resultMap;
	}

	public static ResultMap parseIntermediaryRowTXBriscoeAO(TableRow row,
			String county, long searchId) throws Exception {
		return parseIntermediaryRowTXAransasAO(row, county, searchId);
	}

	public static ResultMap parseIntermediaryRowTXDallasAO(TableRow row,
			String county, long searchId) throws Exception {
		return parseIntermediaryRowTXAransasAO(row, county, searchId);
	}

	public static ResultMap parseIntermediaryRowTXPotterAO(TableRow row,
			String county, long searchId) throws Exception {
		return parseIntermediaryRowTXAransasAO(row, county, searchId);
	}

	public static void parseLegalTXGenericTaxNetAO(ResultMap m, String county,
			long searchId) throws Exception {

		String legal = (String) m.get("tmpLegal");

		if (StringUtils.isEmpty(legal))
			return;

		legal = legal.replaceAll("(?ism)<br */*>", " ");
		
		
		
		m.put("PropertyIdentificationSet.PropertyDescription", legal);

		legal = legal.replaceAll("_", "&")
				.replaceAll("(?is)\\bLO T\\b", "LOT")
				.replaceAll("(?is)\\bBL OCK\\b", "BLOCK")
				.replaceAll("(?is)\\b[SWNE]+\\s*[\\d\\s,\\.']+(?:\\s*F\\s*T)?\\s*O\\s*F\\b", "")
				.replaceAll("(?is)\\b(?:EX[A-Z]\\s*)?[SENW]+\\s*(?:IRR|TRI)\\s*[\\d\\s,\\.']+(?:\\s*F\\s*T)?\\s*(?:O\\s*F)?\\b", "")
				.replaceAll("(?is)\\b[SWNE]+\\s*[/\\d]+\\b(?:\\s+OF)?\\b", "")
				.replaceAll("(?is)(\\d+)\\s*AND\\s*(\\d+)", "$1 & $2");
		
		if("kendall".equals(county)){
			legal = legal.replaceAll("(?ism)(UNIT\\s*\\d+)\\s+([A-Z])\\b", "$1$2")
					.replaceAll("(?ism)\\bLOT\\s*PT\\b", " LOT ");
		}
		
		legal = legal.replaceAll("\\s+", " ");

		legal = GenericFunctions.replaceNumbers(legal);
		String[] exceptionTokens = { "I", "M", "C", "L", "D" };
		legal = Roman.normalizeRomanNumbersExceptTokens(legal, exceptionTokens); // convert
																					// roman
																					// numbers

		//String legalTemp = legal;

		// extract unit from legal description
		String unit = "";
		Pattern p = Pattern.compile("(?is)\\b(UNI?T)\\s*([\\d]+[A-Z]?)\\b");
		Matcher ma = p.matcher(legal);
		if (ma.find()) {
			unit = unit + " " + ma.group(2);
			unit = unit.trim();
			m.put("PropertyIdentificationSet.SubdivisionUnit", unit);// ma.group(2));
			legal = legal.replaceFirst(ma.group(0), ma.group(1) + " ").trim().replaceAll("\\s{2,}", " ");
		}

		// extract section from legal description
		p = Pattern.compile("(?is)\\b(SEC?(?:TION:?)?)\\s+([[A-Z-]|\\d]+)\\b");
		ma = p.matcher(legal);
		if (ma.find()) {
			m.put("PropertyIdentificationSet.SubdivisionSection", ma.group(2));
			legal = legal.replace(ma.group(), " ").trim().replaceAll("\\s+"," ");
		}

		// extract tract from legal description
		p = Pattern.compile("(?is)\\bTRA?C?T?:?\\s+([[A-Z-]|\\d]+)\\b");
		ma = p.matcher(legal);
		if (ma.find()) {
			// in Property Details table we have Tract or Lot
			m.put("PropertyIdentificationSet.SubdivisionTract", ma.group(1));
			m.put("PropertyIdentificationSet.SubdivisionLotNumber", "");
		}

		// extract building #
		String bldg = "";
		p = Pattern.compile("(?is)\\b(BLDG)\\s+([A-Z]|\\d+)\\b");
		ma = p.matcher(legal);
		if (ma.find()) {
			bldg = bldg + " " + ma.group(2);
			bldg = bldg.trim();
			m.put("PropertyIdentificationSet.SubdivisionBldg", bldg);
			legal = legal.replaceFirst(ma.group(0), ma.group(1) + " ").trim().replaceAll("\\s{2,}", " ");
		}

		// get block
		String block = "";
		p = Pattern.compile("(?is)\\b((BLO?C?K?:?)|(BK:?))\\s+([[A-Z]|\\d]+)\\b");
		ma = p.matcher(legal);
		if (ma.find()) {
			block = ma.group(4);
			block = block.replaceAll(" ", "");
			block = block.replaceAll("\\(.*\\)", "");
			block = block.replaceAll("\\bADDN\\b", "").trim();
			if (StringUtils.isEmpty((String) m.get("PropertyIdentificationSet.SubdivisionBlock")))
				m.put("PropertyIdentificationSet.SubdivisionBlock", block);
			legal = legal.replaceFirst(ma.group(0), ma.group(1) + " ").trim().replaceAll("\\s{2,}", " ");
		}

		String page = "";
		p = Pattern.compile("(?is)\\b(PAGE)\\s+(\\d+)\\b");
		ma = p.matcher(legal);
		if (ma.find()) {
			page = ma.group(2);
			page = page.trim();
			if (StringUtils.isEmpty((String) m.get(SaleDataSetKey.PAGE.getKeyName())))
				m.put(SaleDataSetKey.PAGE.getKeyName(), page);
			legal = legal.replaceFirst(ma.group(0), ma.group(1) + " ").trim().replaceAll("\\s{2,}", " ");
		}

		
		
		p = Pattern.compile("(?is)\\b(LO?TS?)\\s+([\\d-,\\s&]+[A-Z]?|[\\d&A-Z]+|\\d+[A-Z]{1,2})\\b");
		legal = TXGenericFunctionsNTN.extractLot(m, legal.replaceAll("\\d+\\.\\d+", ""), p);
		
		if ("lubbock".equalsIgnoreCase(county)) {
			// Bug 8133
			m.put("PropertyIdentificationSet.SubdivisionLotNumber", parseLot(legal));
		}

		String phase = "";
		p = Pattern.compile("(?is)\\b(PHA?S?E?)\\s+([A-Z]|\\d+[A-Z])\\b");
		ma = p.matcher(legal);
		if (ma.find()) {
			phase = ma.group(2);
			phase = phase.trim();
			if (StringUtils.isEmpty((String) m.get(PropertyIdentificationSetKey.SUBDIVISION_PHASE.getKeyName())))
				m.put(PropertyIdentificationSetKey.SUBDIVISION_PHASE.getKeyName(), phase);
			legal = legal.replaceFirst(ma.group(0), ma.group(1) + " ").trim().replaceAll("\\s{2,}", " ");
		}

		// extract subdivision name from legal description
		String subdiv = "";

		p = Pattern.compile("(?is)(.*?)\\s+(PH|UNIT|BLO?C?K|LO?T|TRA?C?T?)");
		ma = p.matcher(legal);
		if (ma.find()) {
			subdiv = ma.group(1);
		} else {
			ma.reset();
			p = Pattern.compile("(?is)([^,]+)");
			ma.usePattern(p);
			if (ma.find()) {
				subdiv = ma.group(1);
			}
		}
		if (StringUtils.isNotEmpty(subdiv)) {
			if (county.equals("atascosa")) {

				subdiv = subdiv.replaceFirst("(.*)\\s+SU\\s*(?:BD?)?.*", "$1");
				subdiv = subdiv.replaceFirst("(.*)\\s+(U\\s*N?\\s*I?T\\s*-?)", "$1");
				subdiv = subdiv.replaceFirst("(.+)\\s+[A-Z]/.*", "$1");
				subdiv = subdiv.replaceFirst("(.+)\\sADD?.*", "$1");
				subdiv = subdiv.replaceFirst("(.+)\\sFLG.*", "$1");
				subdiv = subdiv.trim().replaceFirst("\\AABS\\s+[A-Z][\\d-]+\\s+(.+)", "$1");
				subdiv = subdiv.trim().replaceFirst("ABS\\s+[A-Z][\\d-]+\\s*(.*)", "$1");
				subdiv = subdiv.replaceAll("(?is)\\b(SEC?(?:TION)?)\\s+(\\d+)\\b", "");
				subdiv = subdiv.replaceFirst(",", "");
				subdiv = subdiv.replace(
						m.get(PropertyIdentificationSetKey.ABS_NO.getKeyName()) == null ? "" : (String) m.get(PropertyIdentificationSetKey.ABS_NO
								.getKeyName()), "");
				subdiv = subdiv.replaceAll("\\s+", " ").trim();
				// if (subdiv.matches("[^\\d]"))
				m.put("PropertyIdentificationSet.SubdivisionName", subdiv);

				if (legal.matches(".*\\bCOND.*"))
					m.put("PropertyIdentificationSet.SubdivisionCond", subdiv);
			}

			if ("brazoria".equalsIgnoreCase(county)) {
				// bug 7552 comment 4
				// remove A0134
				subdiv = subdiv.replaceFirst(",", "");
				subdiv = subdiv.replaceAll("(?ism)\\([^\\)]*\\)", " ");
				subdiv = subdiv.replaceAll("(?ism)[A-Z]\\d+\\s", " ").trim().replaceAll("\\s+"," ");

				m.put("PropertyIdentificationSet.SubdivisionName", subdiv);
			}
			
			if ("kendall".equals(county)){
				subdiv = subdiv.replaceAll(",$", "")
						.split(",")[0]
						.replaceAll("(?ism)ABSTRACT \\d+", "").trim();
				
				m.put("PropertyIdentificationSet.SubdivisionName", subdiv);
			}
		}

	}

	private static void partyNamesTXAndersonAO(ResultMap m, String county,
			long searchId) throws Exception {
		partyNamesTXGenericTaxNetAO(m, county, searchId);
	}

	private static void partyNamesTXAransasAO(ResultMap m, String county,
			long searchId) {
		try {
			String unparsedName = org.apache.commons.lang.StringUtils
					.strip((String) m.get("tmpOwner"));

			m.put(PropertyIdentificationSetKey.NAME_ON_SERVER.getKeyName(),
					unparsedName.replaceAll("&&", " & ")
							.replaceAll("\\s+", " "));

			@SuppressWarnings("rawtypes")
			List<List> body = new ArrayList<List>();
			if (StringUtils.isNotEmpty(unparsedName)) {
				String[] tokens = unparsedName.split("&&");
				for (int i = 0; i < tokens.length; i++) {

					String[] names = StringFormats.parseNameNashville(
							tokens[i].replaceAll("\\s*&\\s*", "&"), true);
					String[] type = GenericFunctions.extractAllNamesType(names);
					String[] otherType = GenericFunctions
							.extractAllNamesOtherType(names);
					String[] suffixes = GenericFunctions
							.extractNameSuffixes(names);
					GenericFunctions.addOwnerNames(names, suffixes[0],
							suffixes[1], type, otherType,
							NameUtils.isCompany(names[2]),
							NameUtils.isCompany(names[5]), body);
				}
				GenericFunctions.storeOwnerInPartyNames(m, body, true);
				return;
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	private static void partyNamesTXArcherAO(ResultMap m, String county,
			long searchId) {
		partyNamesTXAransasAO(m, county, searchId);
	}

	private static void partyNamesTXArmstrongAO(ResultMap m, String county,
			long searchId) {
		try {
			String unparsedName = org.apache.commons.lang.StringUtils
					.strip((String) m.get("tmpOwner"));
			
			unparsedName = unparsedName.replaceAll("\\s+_\\s+", " & ");

			m.put(PropertyIdentificationSetKey.NAME_ON_SERVER.getKeyName(),
					unparsedName.replaceAll("&&", " & ")
							.replaceAll("\\s+", " "));

			unparsedName = unparsedName.replace(" MRS ", " & ");
			unparsedName = unparsedName.replace(" DEC", "");
			unparsedName = unparsedName.replace(" ET UX", " ETUX");
			unparsedName = unparsedName.replaceAll("[^\\w^&^ ]", "");

			@SuppressWarnings("rawtypes")
			List<List> body = new ArrayList<List>();
			if (StringUtils.isNotEmpty(unparsedName)) {
				String[] tokens = unparsedName.split("&&");
				for (int i = 0; i < tokens.length; i++) {

					String[] names = StringFormats.parseNameNashville(
							tokens[i].replaceAll("\\s*&\\s*", "&"), true);
					String[] type = GenericFunctions.extractAllNamesType(names);
					String[] otherType = GenericFunctions
							.extractAllNamesOtherType(names);
					String[] suffixes = GenericFunctions
							.extractNameSuffixes(names);
					GenericFunctions.addOwnerNames(names, suffixes[0],
							suffixes[1], type, otherType,
							NameUtils.isCompany(names[2]),
							NameUtils.isCompany(names[5]), body);
				}
				GenericFunctions.storeOwnerInPartyNames(m, body, true);
				return;
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	private static void partyNamesTXBowieAO(ResultMap m, String county,
			long searchId) {
		partyNamesTXAransasAO(m, county, searchId);
	}

	private static void partyNamesTXBriscoeAO(ResultMap m, String county,
			long searchId) throws Exception {
		partyNamesTXGenericTaxNetAO(m, county, searchId);
	}

	private static void partyNamesTXDallasAO(ResultMap m, String county,
			long searchId) throws Exception {
		partyNamesTXGenericTaxNetAO(m, county, searchId);
	}

	public static void partyNamesTXGenericTaxNetAO(ResultMap m, String county,
			long searchId) throws Exception {

		String stringOwner = (String) m.get("tmpOwner");

		if (StringUtils.isEmpty(stringOwner))
			return;

		stringOwner = GenericFunctions2.cleanOwnerNameFromPrefix(stringOwner);
		stringOwner = GenericFunctions2.resolveOtherTypes(stringOwner);
		
		stringOwner = stringOwner.replaceAll("\\s+_\\s+", " & ");

		// TO DO: ATTN case
		stringOwner = stringOwner.replaceAll("\\bATTN: C/O\\b", "");

		stringOwner = stringOwner.replaceAll("(?is)[^L]/[A-Z]\\b", "");
		stringOwner = stringOwner.replaceAll("\\s*&\\s*(ETAL)\\s*$", " $1");
		stringOwner = stringOwner.replaceAll("[\\(\\)]+", "");
		stringOwner = stringOwner.replaceAll("(?is)\\bAND\\b(.*?\\bWI?FE?)\\b", "&$1");
		stringOwner = stringOwner.replaceAll("(?is)\\bWI?FE?\\b", " ");
		// TO DO: ATTN case
		stringOwner = stringOwner.replaceAll("\\bATTN", "");
		stringOwner = stringOwner.replaceAll("(?is)\\bDECD", "");
		stringOwner = stringOwner.replaceAll("(?is)\\bMRS", "");
		stringOwner = stringOwner.replaceAll(
				"(?i)(\\w+\\s+\\w+(?:\\s+\\w+)?)\\s*,\\s*(\\w+\\s+\\w+)",
				"$1 AND $2");
		stringOwner = stringOwner.replaceAll("(?is)\\s*&\\s*$", "");

		stringOwner = stringOwner.trim();
		// TX Denton AO, PIDN 246235: ZINNA, MICHAEL S & TIFFANY L
		stringOwner = stringOwner.replaceFirst("^(\\w+),\\s+\\w+\\s+\\w\\s+&\\s+\\w+\\s+\\w$", "$0 $1");

		// TX Nueces AO, PIDN 183527, GILBERT LYNN & JACK G
		stringOwner = stringOwner.replaceFirst("^(\\w+)\\s+\\w+(\\s+\\w)?\\s*&\\s*\\w+\\s+\\w$", "$0 $1");

		@SuppressWarnings("rawtypes")
		List<List> body = new ArrayList<List>();
		String[] names = { "", "", "", "", "", "" };
		String[] suffixes, types, otherTypes;
		String ln = "";
		boolean coOwner = false;
		if (stringOwner
				.trim()
				.matches(
						"(?is)(.*LIVING TRUST)\\s+(\\w+\\s+\\w+)\\s*&\\s*(\\w+\\s+\\w+\\s+\\w+)")) {// 02330-00-184-000201
																									// AtascosaAO
			stringOwner = stringOwner
					.replaceAll(
							"(?is)(.*LIVING TRUST)\\s+(\\w+\\s+\\w+)\\s*(&\\s*\\w+\\s+\\w+\\s+\\w+)",
							"$1 $3 AND $2");
		}
		String[] owners = stringOwner.split(" & ");
		if ((stringOwner.matches(".*%?\\s*\\w+\\s*&\\s*\\w+") || stringOwner
				.matches("\\w+\\s*&\\s*\\w+\\s+\\w+"))
				&& NameUtils.isCompany(stringOwner)) {
			stringOwner = stringOwner.replaceAll("\\s*&\\s*%\\s*", " aaaaaaa ");
			owners = stringOwner.split("aaaaaaa");
		}
		if (stringOwner.matches("\\w+(\\s+\\w+)(\\s+\\w+)?\\s+&\\s*\\w+")
				&& NameUtils.isNotCompany(stringOwner)) {
			owners = stringOwner.split("aaaaaaa");
		}

		for (int i = 0; i < owners.length; i++) {
			if (StringUtils.isNotEmpty(owners[i])) {
				if (i == 0) {
					names = StringFormats.parseNameNashville(owners[i], true);
					ln = names[2];
					if (NameUtils.isCompany(names[2])) {
						ln = names[2].replaceAll("\\A(\\w+)\\s+.*", "$1");
					}
				} else {
					if (NameUtils.isCompany(owners[i], new Vector<String>(),
							true)) {
						names[2] = owners[i];
					} else {
						names = StringFormats
								.parseNameDesotoRO(owners[i], true);
					}
					coOwner = true;
				}
			}

			if (coOwner) {
				if (NameUtils.isCompany(names[2])
						&& names[2].matches("(?is).*?\\s+TR\\b")) {
					names[2] = names[2].replaceAll("(?is)(.*?)\\s+TR\\b", "$1 "
							+ ln + " TR");
				}

				if (NameUtils.isCompany(names[5]) && names[2].length() > 0) {
					names[2] = names[2] + "&" + names[5];
					names[5] = "";
				}

				if (LastNameUtils.isNotLastName(names[2])
						&& NameUtils.isNotCompany(names[2])
						&& names[0].length() == 0 && names[1].length() == 0) {
					names[0] = names[2];
					names[2] = ln;
				} else if (LastNameUtils.isNotLastName(names[2])
						&& LastNameUtils.isLastName(names[0])
						&& names[1].length() != 0) {
					String aux = names[2];
					names[2] = names[0];
					names[0] = names[1];
					names[1] = aux;
				} else if (LastNameUtils.isNotLastName(names[2])
						&& NameUtils.isNotCompany(names[2])
						&& LastNameUtils.isLastName(names[0])
						&& names[1].length() == 0) {
					String aux = names[2];
					names[2] = names[0];
					names[0] = aux;

				} else if (names[2].length() == 1
						|| (LastNameUtils.isNotLastName(names[2]) && NameUtils
								.isNotCompany(names[2]))) {
					names[1] = names[2];
					names[2] = ln;
				} else if (names[0].length() == 0 && names[1].length() == 0
						&& NameUtils.isNotCompany(names[2])
						&& LastNameUtils.isNotLastName(names[2])) {
					names[0] = names[2];
					names[2] = ln;
				} else if (names[0].length() > 1 && names[1].length() == 0
						&& NameUtils.isNotCompany(names[2])
						&& LastNameUtils.isNotLastName(names[2])) {
					if (!names[2].equals(ln)) {
						names[1] = names[2];
						names[2] = ln;
					}
				}

				if (LastNameUtils.isNotLastName(names[2])
						&& LastNameUtils.isLastName(names[0])) {
					String aux = names[2];
					names[2] = names[0];
					names[0] = aux;
				}
				ln = names[2];
			}

			names[2] = names[2].replaceAll("(?is),", "");
			if (StringUtils.isNotEmpty(names[5])) {// 044200010010 SMITH GEORGE
													// T AND FAYE KEITH
				if (NameUtils.isNotCompany(names[5])) {
					if (LastNameUtils.isNotLastName(names[5])
							&& StringUtils.isNotEmpty(names[5])) {
						names[4] = names[3];
						names[3] = names[5];
						names[5] = ln;
					}
				}
			}
			types = GenericFunctions.extractAllNamesType(names);
			otherTypes = GenericFunctions.extractAllNamesOtherType(names);
			suffixes = GenericFunctions.extractNameSuffixes(names);
			GenericFunctions.addOwnerNames(names, suffixes[0], suffixes[1],
					types, otherTypes, NameUtils.isCompany(names[2]),
					NameUtils.isCompany(names[5]), body);
		}
		GenericFunctions.storeOwnerInPartyNames(m, body, true);

		m.put("PropertyIdentificationSet.NameOnServer", stringOwner);

		// String[] a = StringFormats.parseNameNashville(stringOwner, true);
		// m.put("PropertyIdentificationSet.OwnerFirstName", a[0]);
		// m.put("PropertyIdentificationSet.OwnerMiddleName", a[1]);
		// m.put("PropertyIdentificationSet.OwnerLastName", a[2]);
		// m.put("PropertyIdentificationSet.SpouseFirstName", a[3]);
		// m.put("PropertyIdentificationSet.SpouseMiddleName", a[4]);
		// m.put("PropertyIdentificationSet.SpouseLastName", a[5]);

	}

	private static void partyNamesTXPotterAO(ResultMap m, String county,
			long searchId) throws Exception {
		partyNamesTXAransasAO(m, county, searchId);
	}

	// public static void main(String[] args) throws Exception {
	//
	// ResultMap m = new ResultMap();
	//
	// m.put("tmpAddress", "123 ACR 404 (OFF)");
	//
	// parseAddressTXAndersonAO(m, "", 123L);
	// }

}
