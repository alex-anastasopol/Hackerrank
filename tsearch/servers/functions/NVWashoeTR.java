
package ro.cst.tsearch.servers.functions;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map.Entry;
import java.util.Vector;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.htmlparser.tags.TableRow;

import ro.cst.tsearch.extractor.legal.LegalDescription;
import ro.cst.tsearch.extractor.xml.GenericFunctions;
import ro.cst.tsearch.extractor.xml.ResultMap;
import ro.cst.tsearch.extractor.xml.StringFormats;
import ro.cst.tsearch.search.address2.Normalize;
import ro.cst.tsearch.servers.response.OtherInformationSet.OtherInformationSetKey;
import ro.cst.tsearch.servers.response.PropertyIdentificationSet.PropertyIdentificationSetKey;
import ro.cst.tsearch.utils.StringUtils;

/**
 * @author vladb
 *
 */
public class NVWashoeTR {
	
	public static final String[] CITIES = {"COLD SPRINGS", "CRYSTAL BAY", "EMPIRE", "GERLACH", "GOLDEN VALLEY",		//Cities, Towns and Census Designated Places
		   								   "INCLINE VILLAGE", "LEMMON VALLEY", "MOGUL", "NIXON", "RENO", 
										   "SPANISH SPRINGS", "SPARKS", "SUN VALLEY", "SUTCLIFFE", "VERDI",
										   "WADSWORTH", "WEST RENO",
										   "ANDYS PLACE", "BORDER TOWN", "COPPERFIELD", "DEEP HOLE", "DODGE",		//Other Populated Places (Neighborhoods, Subdivisions and Settlements)
										   "FLANIGAN", "FLEISH", "FRANKTOWN", "GLENDALE", "GRAND VIEW TERRACE",
										   "HAFED", "HIDDEN VALLEY", "LAWTON", "MARTIN", "MAYBERRY-HIGHLAND PARK",
										   "MUSTANG", "NASHVILLE", "NEW WASHOE CITY", "NORTH VALLEY", "OLINGHOUSE",
										   "PANTHER VALLEY", "PATRICK", "PHIL", "PLEASANT VALLEY", "PYRAMID",
										   "RALEIGH HEIGHTS", "REEDERVILLE", "RENO-STEAD", "REYNARD", "SAND PASS",
										   "SANO", "SHEEPSHEAD", "SMOKE CREEK", "STEAMBOAT", "SUNDOWN TOWN",
										   "SWEDES PLACE", "THISBY", "TYROLIAN VILLAGE", "VISTA", "VYA",
										   "WASHOE CITY", "WEDEKIND", "WIMER PLACE", "ZENOBIA"};
	
	public static final HashMap<String,String> CITY_EQUIVALENTS = new HashMap<String,String>();
	static {
		CITY_EQUIVALENTS.put("SPKS", "SPARKS");
		CITY_EQUIVALENTS.put("WCTY", "WASHOE CITY");
		CITY_EQUIVALENTS.put("INCL", "INCLINE VILLAGE");
		CITY_EQUIVALENTS.put("GEIGER GRADE", "");			//it is a state highway
		CITY_EQUIVALENTS.put("FORTY NINER", "");			//it it not a city
		CITY_EQUIVALENTS.put("ARROWHEAD", "");				//it it not a city
	}
	
	public static String[] ALL_CITIES;
	static {
		int size1 = CITIES.length; 
		int size = size1 + CITY_EQUIVALENTS.size();
		ALL_CITIES = new String[size];
		for (int i=0;i<size1;i++) {
			ALL_CITIES[i] = CITIES[i];
		}
		int j = 0;
		Iterator<Entry<String, String>> it = CITY_EQUIVALENTS.entrySet().iterator();
		while (it.hasNext()) {
			Entry<String, String> entry = it.next();
			String key = entry.getKey();
			ALL_CITIES[size1+j] = key;
			j++;
		}
	}
	
	public static String correctCityName(String city) {
		String res = city;
		String eq = CITY_EQUIVALENTS.get(city);
		if (eq!=null) {
			res = eq;
		}
		return res;
	}

	public static ResultMap parseIntermediaryRow(TableRow row) {
		
		ResultMap resultMap = new ResultMap();
		resultMap.put(OtherInformationSetKey.SRC_TYPE.getKeyName(), "TR");
		resultMap.put(PropertyIdentificationSetKey.PARCEL_ID.getKeyName(), row.getColumns()[3].toPlainTextString().trim());
		
		parseNames(resultMap, row.getColumns()[0].toPlainTextString().trim(), false);
		parseAddress(resultMap, row.getColumns()[2].toPlainTextString().trim());
		
		return resultMap;
	}

	public static void parseAddress(ResultMap resultMap, String address) {
		
		address = address.trim();
		if(StringUtils.isEmpty(address)) {
			return;
		}
		
		address = address.replaceFirst("-+$", "");
		address = address.trim();
		
		Matcher ma = Pattern.compile("(?is)(.*?)(?:\\s+[NSEW]{1,2})?\\s+([A-Z]{2})(?:\\s+(\\d{5}(?:-\\d{4})?))?$").matcher(address);
		if (ma.find()) {
			String state = ma.group(2);
			if (Normalize.isStateAbbreviation(state)) {
				address = ma.group(1);
			}
			String zip = ma.group(3);
			if (!StringUtils.isEmpty(zip)) {
				resultMap.put(PropertyIdentificationSetKey.ZIP.getKeyName(), zip);
			}
		}
		
		for (int i=0;i<ALL_CITIES.length;i++) {
			address = address.replaceFirst("(?is)/(" + ALL_CITIES[i] + ")\\b", " $1");
		}
		address = address.replaceFirst("(?is)^\\s*VARIOUS\\b", "");
		
		String addressAndCity[] = StringFormats.parseCityFromAddress2(address, ALL_CITIES);
		String city = addressAndCity[0];
		if (StringUtils.isNotEmpty(city)) {
			city = correctCityName(city);
			if (StringUtils.isNotEmpty(city)) {
				resultMap.put(PropertyIdentificationSetKey.CITY.getKeyName(), city);
			}
			address = addressAndCity[1];
		}
		
		address = address.replaceFirst("(?is)\\b(?:NO|SP|STE)\\s+([\\dA-Z-]+)", "#$1");
		
		String streetNo = StringFormats.StreetNo(address);
		String streetName = StringFormats.StreetName(address);
		
		if(!streetNo.equals("0")) {
			resultMap.put(PropertyIdentificationSetKey.STREET_NO.getKeyName(), streetNo);
		}
		
		if(!streetName.matches("(?is)not supplied|UNSPECIFIED|VARIOUS")) {
			resultMap.put(PropertyIdentificationSetKey.STREET_NAME.getKeyName(), streetName);
		}
	}

	public static void parseNames(ResultMap resultMap, String ownerInfo, boolean hasAddress) {
		
		String owner = ownerInfo;
		if(StringUtils.isEmpty(owner)) {
			return;
		}
		
		Vector<String> companyExpr = new Vector<String>();
		companyExpr.add("MEASUREME");
		companyExpr.add("ADAMS AND SON");
		companyExpr.add("CLEAN");
		companyExpr.add("WINNERS");
		companyExpr.add("MARKETING");
		
		owner = fixLines(owner);
		String[] lines = owner.split("\n");
		if(hasAddress) {
			lines = GenericFunctions.removeAddressFLTR(lines, new Vector<String>(), new Vector<String>(), 2, Integer.MAX_VALUE, true);
		}
		for(int i = 0; i < lines.length; i++) {
			lines[i] = cleanName(lines[i]);
		}
		lines = splitAndFixLines(lines, companyExpr);
		
		COFremontAO.genericParseNames(resultMap, lines, null, companyExpr, false, COFremontAO.ALL_NAMES_LF, -1);
	}

	private static String cleanName(String name) {

		String properName = name;
		properName = properName.replaceAll("\\bMA\\s+MFT(\\b|$)", "");
		properName = COLarimerTR.genericCleanName(properName, new Vector<String>(), "");
		
		return properName;
	}

	private static String[] splitAndFixLines(String[] lines, Vector<String> companyExpr) {
		
		ArrayList<String> newLines = new ArrayList<String>();
		Pattern p = Pattern.compile("(?is)((\\w+).*TRUST\\s*),(.*)");
		for(String line : lines) {
			if(COFremontAO.isCompany(line, companyExpr)) {
				Matcher m = p.matcher(line);
				if(m.find()) {
					newLines.add(m.group(1));
					String extraNames = m.group(2) + "," + m.group(3);
					String[] parts = extraNames.split("(?is)&|/|\\bAND\\b|\\bOR\\b");
					for(String part : parts) {
						newLines.add(part.trim());
					}
				} else {
					newLines.add(line);
				}
			} else {
				String[] parts = line.split("(?is)&|/|\\bAND\\b|\\bOR\\b");
				for(String part : parts) {
					newLines.add(part.trim());
				}
			}
		}
		
		return newLines.toArray(new String[newLines.size()]);
	}

	private static String fixLines(String owner) {
		
		String properOwner = owner;
		properOwner = properOwner.replaceAll("\\b(?:[,(]\\s*)?(?:AS\\s+)?(?:\\bCO-?\\s*)?(?<!BOARD\\sOF\\s)(T(?:(?:RU?)?S)?TEE?S?)(\\s*([)]|OF))?\\b", "\n$1\n");
		properOwner = properOwner.replaceAll("(?is)\\bET\\s*(AL|UX|VIR)\\b", "\nET$1\n");
		properOwner = properOwner.replaceAll("\\s*\n\\s*", "\n").trim();
		
		return properOwner;
	}


	public static void parseLegal(ResultMap resultMap, String legal) {
		if(StringUtils.isEmpty(legal)) {
			return;
		}
		
		Matcher m = null;
		String lot = "";
		String blk = "";
		String unit = "";
		String phase = "";
		String tract = "";
		String sub = "";
		
		// extract lots
		m = Pattern.compile("(?is)(?<=\\bLO?TS?)\\s+(\\d+[A-Z]?|[A-Z])(\\s*(?:-|THRU)\\s*(\\d+[A-Z]?|[A-Z]))?(?:[&,; ]+|-|$)").matcher(legal);
		while(m.find()) {
			lot += m.group(1);
			if(m.group(3) != null) {
				lot += "-" + m.group(3);
			}
			lot += ",";
			legal = m.replaceFirst(" ");
			m.reset(legal);
		}
		if(lot.length() > 0) {
			lot = LegalDescription.cleanValues(lot, false, true);
			resultMap.put(PropertyIdentificationSetKey.SUBDIVISION_LOT_NUMBER.getKeyName(), lot);
		}

		// extract block
		m = Pattern.compile("(?is)(?<=\\bBL(?:OC)?KS?)\\s+(\\d+|[A-Z])(\\s*(?:-|THRU)\\s*(\\d+|[A-Z]))?(?:[&,; ]+|-|$)").matcher(legal);
		while(m.find()) {
			blk += m.group(1);
			if(m.group(3) != null) {
				blk += "-" + m.group(3);
			}
			blk += ",";
			legal = m.replaceFirst(" ");
			m.reset(legal);
		}
		if(blk.length() > 0) {
			blk = LegalDescription.cleanValues(blk, false, true);
			resultMap.put(PropertyIdentificationSetKey.SUBDIVISION_BLOCK.getKeyName(), blk);
		}

		// extract unit
		m = Pattern.compile("(?is)(?<=\\bUNITS?)\\s+(\\d+)(\\s*(?:-|THRU)\\s*(\\d+))?(?:[&,; ]+|-|$)").matcher(legal);
		while(m.find()) {
			unit += m.group(1);
			if(m.group(3) != null) {
				unit += "-" + m.group(3);
			}
			unit += ",";
			legal = m.replaceFirst(" ");
			m.reset(legal);
		}
		if(unit.length() > 0) {
			unit = LegalDescription.cleanValues(unit, false, true);
			resultMap.put(PropertyIdentificationSetKey.SUBDIVISION_UNIT.getKeyName(), unit);
		}

		//extract phase
		m = Pattern.compile("(?is)(?<=\\bPH(?:ASE)?)\\s+(\\d+)(\\s*(?:-|THRU)\\s*(\\d+))?(?:[&,; ]+|-|$)").matcher(legal);
		while(m.find()) {
			phase += m.group(1);
			if(m.group(3) != null) {
				phase += "-" + m.group(3);
			}
			phase += ",";
			legal = m.replaceFirst(" ");
			m.reset(legal);
		}
		if(phase.length() > 0) {
			phase = LegalDescription.cleanValues(phase, false, true);
			resultMap.put(PropertyIdentificationSetKey.SUBDIVISION_PHASE.getKeyName(), phase);
		}

		// extract tract
		m = Pattern.compile("(?is)(?<=\\bTRACTS?)\\s+(\\d+|[A-Z])(\\s*(?:-|THRU|AND)\\s*(\\d+|[A-Z]))?(?:[&,; ]+|-|$)").matcher(legal);
		while(m.find()) {
			tract += m.group(1);
			if(m.group(3) != null) {
				tract += "-" + m.group(3);
			}
			tract += ",";
			legal = m.replaceFirst(" ");
			m.reset(legal);
		}
		if(tract.length() > 0) {
			tract = LegalDescription.cleanValues(tract, false, true);
			resultMap.put(PropertyIdentificationSetKey.SUBDIVISION_TRACT.getKeyName(), tract);
		}
		
		// extract Sec-Twn-Rng
		m = Pattern.compile("(?is)Section\\s+(\\d+)\\b").matcher(legal);
		if(m.find()) {
			resultMap.put(PropertyIdentificationSetKey.SUBDIVISION_SECTION.getKeyName(), m.group(1));
		}
		m = Pattern.compile("(?is)Township\\s+(\\d+(?:[NSEW]{1,2})?)\\b").matcher(legal);
		if(m.find()) {
			resultMap.put(PropertyIdentificationSetKey.SUBDIVISION_TOWNSHIP.getKeyName(), m.group(1));
		}
		m = Pattern.compile("(?is)Range\\s+(\\d+(?:[NSEW]{1,2})?)\\b").matcher(legal);
		if(m.find()) {
			resultMap.put(PropertyIdentificationSetKey.SUBDIVISION_RANGE.getKeyName(), m.group(1));
		}
		
		// extract subdivision
		m = Pattern.compile("(?s)SubdivisionName\\s+([A-Z\\s\\d]*?)\\b(?:(CONDO(?:MINIUM)?S?|EST(ATES)?|SUBD?[.]?(?:IVISION)?|ADD)|([A-Z][a-z]|[A-Z]{1,2}\\d|RANCH|UNIT|$))").matcher(legal);
		if(m.find()) {
			sub = m.group(1);
			if(m.group(2) != null) {
				sub += m.group(2);
			}
			sub = sub.trim();
			if (sub.matches("(?is)UNSPECIFIED")) {
				sub = "";
			}
			if(!sub.matches("[A-Z]?")) {
				resultMap.put(PropertyIdentificationSetKey.SUBDIVISION_NAME.getKeyName(), sub);
			}
		}
	}
}
