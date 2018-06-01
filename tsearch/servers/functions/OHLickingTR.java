package ro.cst.tsearch.servers.functions;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang.StringEscapeUtils;
import org.apache.commons.lang.StringUtils;
import org.htmlparser.Node;
import org.htmlparser.tags.TableColumn;
import org.htmlparser.tags.TableRow;

import ro.cst.tsearch.extractor.xml.GenericFunctions;
import ro.cst.tsearch.extractor.xml.ResultMap;
import ro.cst.tsearch.extractor.xml.StringFormats;
import ro.cst.tsearch.parser.HtmlParser3;
import ro.cst.tsearch.search.name.NameUtils;
import ro.cst.tsearch.servers.response.OtherInformationSet.OtherInformationSetKey;
import ro.cst.tsearch.servers.response.PropertyIdentificationSet.PropertyIdentificationSetKey;

public class OHLickingTR {
	private static final Pattern		TOWNSHIP_PATTERN	= Pattern.compile("(\\d+)?\\s*-\\s*([\\dA-Z]+)?\\s*-\\s*(\\d+)?");
	private static final Pattern		LEGAL_UNIT_PATTERN	= Pattern.compile("UNI\\s*T\\s+?(\\d\\s*)?(\\w+)");
	private static final Pattern		LEGAL_TRACT_PATTERN	= Pattern.compile("TR(?:ACT)?\\s*([\\d-]+)");
	private static final Pattern		LEGAL_BLDG_PATTERN	= Pattern.compile("(?is)\\b(?:BLDG|BUILDING)\\s+(\\d+|[A-Z])");
	private static final Pattern		LEGAL_PHASE_PATTERN	= Pattern.compile("(?is)\\bP\\s*H(?:ASE)?\\s*([\\d-]+)\\b");
	private static final Pattern		LEGAL_LOT_PATTERN	= Pattern.compile("LO\\s*TS?\\s*(?:NO)?\\s*?(\\d+\\s*(?:-)*\\s*\\d*)");
	private static final Pattern		ZIP_PATTERN			= Pattern.compile("\\s*(\\d+)\\s*$");
	private static ArrayList<String>	cities				= new ArrayList<String>();
	static {
		cities.add("ADAM MILLS");
		cities.add("ALBANY");
		cities.add("ALEXANDRIA");
		cities.add("ASHLEY");
		cities.add("BALTIMORE");
		cities.add("BATAVIA");
		cities.add("BELLEFONTAINE");
		cities.add("BLADENSBURG");
		cities.add("BROWNSVILLE");
		cities.add("BUCKEYE LAKE");
		cities.add("BUCYRUS");
		cities.add("CANTON");
		cities.add("CENTERBURG");
		cities.add("CHILLICOTHE");
		cities.add("CIRCLEVILLE");
		cities.add("CLARKSBURG");
		cities.add("COLUMBUS");
		cities.add("CORNING");
		cities.add("COSHOCTON");
		cities.add("CROOKSVILLE");
		cities.add("CROTON");
		cities.add("DOVER");
		cities.add("ETNA");
		cities.add("FRAZEYSBURG");
		cities.add("GLENFORD");
		cities.add("GRANVILLE");
		cities.add("GRATIOT");
		cities.add("HEATH");
		cities.add("HEBRON");
		cities.add("HOMER");
		cities.add("HOPEWELL");
		cities.add("JACKSONTOWN");
		cities.add("JOHNSTOWN");
		cities.add("JOHNSTOWN OH");
		cities.add("KIRKERSVILLE");
		cities.add("LANCASTER");
		cities.add("LOGAN");
		cities.add("LONDON");
		cities.add("MALTA");
		cities.add("MANSFIELD");
		cities.add("MARENGO");
		cities.add("MARTINSBURG");
		cities.add("MILLERSPORT");
		cities.add("MOUNT VERNON");
		cities.add("MT PERRY");
		cities.add("MT VERNON");
		cities.add("NASHPORT");
		cities.add("NEW ALBANY");
		cities.add("NEW CONCORD");
		cities.add("NEW LEXINGTON");
		cities.add("NEW PHILADELPHIA");
		cities.add("NEWARK");
		cities.add("NEWAYGO");
		cities.add("OUTVILLE");
		cities.add("PATASKALA");
		cities.add("PHILO");
		cities.add("PICKERINGTON");
		cities.add("PIKETON");
		// cities.add("PU");
		cities.add("REYNOLDSBURG");
		cities.add("ROSEVILLE");
		cities.add("RUSHVILLE");
		cities.add("SAINT LOUISVILLE");
		cities.add("SENECAVILLE");
		cities.add("SIOUX FALLS");
		cities.add("SMITHFIELD");
		cities.add("SOMERSET");
		cities.add("ST LOUISVILLE");
		cities.add("ST. LOUISVILLE");
		cities.add("STOCKPORT");
		cities.add("SUMMIT STATION");
		cities.add("SUNBURY");
		cities.add("THORNVILLE");
		cities.add("UTICA");
		cities.add("VINTON");
		cities.add("ZANESVILLE");
	}

	public static ResultMap parseIntermediaryRow(TableRow row) {
		ResultMap m = new ResultMap();
		m.put(OtherInformationSetKey.SRC_TYPE.getKeyName(), "TR");

		TableColumn[] cols = row.getColumns();

		if (cols.length == 5) {
			String parcel = cols[0].toPlainTextString().trim();
			String owner = cols[1].toPlainTextString().trim();
			String address = cols[2].toPlainTextString().trim();
			String city = cols[3].toPlainTextString().trim();
			String legal = cols[4].toPlainTextString().trim().replaceAll("\\s+", " ");

			if (ro.cst.tsearch.utils.StringUtils.isNotEmpty(parcel)) {
				m.put(PropertyIdentificationSetKey.PARCEL_ID.getKeyName(), parcel);
			
				if(parcel.matches("\\d{3}-\\d+-\\d{2}.\\d{3}")){
					m.put(PropertyIdentificationSetKey.PROPERTY_TYPE.getKeyName(), "Real Estate");
				}
			}

			if (ro.cst.tsearch.utils.StringUtils.isNotEmpty(owner)) {
				parseNames(m, owner.replaceAll("\\sET AL(\\s|$)", " ETAL$1").replace("@2", ""));
			}

			if (ro.cst.tsearch.utils.StringUtils.isNotEmpty(address)) {
				ro.cst.tsearch.servers.functions.OHDelawareTR.parseAddress(m, address);
			}

			if (ro.cst.tsearch.utils.StringUtils.isNotEmpty(city)) {
				m.put(PropertyIdentificationSetKey.CITY.getKeyName(), city);
			}

			if (ro.cst.tsearch.utils.StringUtils.isNotEmpty(legal)) {
				m.put(PropertyIdentificationSetKey.LEGAL_DESCRIPTION_ON_SERVER.getKeyName(), legal);
			}
		}
		return m;
	}

	public static void parseNames(ResultMap m, String owner) {
		try {
			@SuppressWarnings("rawtypes")
			ArrayList<List> body = new ArrayList<List>();

			StringBuffer nameOnServer = new StringBuffer();

			owner = StringUtils.strip(owner);
			owner = owner.replace("CO-TRUSTEES", "TRUSTEES");
			if (ro.cst.tsearch.utils.StringUtils.isNotEmpty(owner)) {
				nameOnServer.append(owner + " & ");
				Pattern pat = Pattern.compile("(?is)\\s*((?:[a-z]+\\s+){3})([a-z]+(?:\\s+[a-z]+)?)\\s*");
				Matcher mat = pat.matcher(owner);
				if (mat.matches()) {
					owner = mat.group(1) + "& " + mat.group(2);
				}
				String[] names = StringFormats.parseNameNashville(owner, true);
				String[] type = GenericFunctions.extractAllNamesType(names);
				String[] otherType = GenericFunctions.extractAllNamesOtherType(names);
				String[] suffixes = GenericFunctions.extractNameSuffixes(names);

				GenericFunctions.addOwnerNames(names, suffixes[0], suffixes[1], type, otherType, NameUtils.isCompany(names[2]),
						NameUtils.isCompany(names[5]), body);
			}
			m.put(PropertyIdentificationSetKey.NAME_ON_SERVER.getKeyName(), StringUtils.strip(nameOnServer.toString()).replaceAll("\\&$", ""));
			if (body.size() > 0)
				GenericFunctions.storeOwnerInPartyNames(m, body, true);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public static void parseLegal(ResultMap resultMap, HtmlParser3 htmlParser3) {
		// legal
		Node legalNode = htmlParser3.getNodeById("ctl00_cntMain_lblLegalDesc");
		if (legalNode != null) {
			String legal = legalNode.getParent().getNextSibling().getNextSibling().toPlainTextString();
			legal = StringEscapeUtils.unescapeHtml(legal);

			Matcher matcher = LEGAL_UNIT_PATTERN.matcher(legal);
			if (matcher.find()) {
				resultMap.put(PropertyIdentificationSetKey.SUBDIVISION_UNIT.getKeyName(), matcher.group(1));
			}

			matcher = LEGAL_TRACT_PATTERN.matcher(legal);
			if (matcher.find()) {
				resultMap.put(PropertyIdentificationSetKey.SUBDIVISION_TRACT.getKeyName(), matcher.group(1));
				legal = legal.replaceFirst(matcher.group(0), "");
			}

			matcher = TOWNSHIP_PATTERN.matcher(legal);
			if (matcher.find()) {
				String section = StringUtils.defaultString(matcher.group(1)).trim();
				String township = StringUtils.defaultString(matcher.group(2)).trim();
				String range = StringUtils.defaultString(matcher.group(3)).trim();
				if (!section.isEmpty()) {
					resultMap.put(PropertyIdentificationSetKey.SUBDIVISION_SECTION.getKeyName(), section);
				}
				if (!township.isEmpty()) {
					resultMap.put(PropertyIdentificationSetKey.SUBDIVISION_TOWNSHIP.getKeyName(), township);
				}
				if (!range.isEmpty()) {
					resultMap.put(PropertyIdentificationSetKey.SUBDIVISION_RANGE.getKeyName(), range);
				}
				legal = legal.replaceFirst(matcher.group(0), "");
			}
			ro.cst.tsearch.servers.functions.OHDelawareTR.parseLegal(resultMap, legal);

			matcher = LEGAL_LOT_PATTERN.matcher(legal);
			if (resultMap.get(PropertyIdentificationSetKey.SUBDIVISION_LOT_NUMBER.getKeyName()) != null && matcher.find()) {
				resultMap.put(PropertyIdentificationSetKey.SUBDIVISION_LOT_NUMBER.getKeyName(), matcher.group(1));
			}
			
			matcher = LEGAL_BLDG_PATTERN.matcher(legal);
			if (matcher.find()) {
				resultMap.put(PropertyIdentificationSetKey.SUBDIVISION_BLDG.getKeyName(), matcher.group(1));
			}
			
			matcher = LEGAL_PHASE_PATTERN.matcher(legal);
			if (matcher.find()) {
				resultMap.put(PropertyIdentificationSetKey.SUBDIVISION_PHASE.getKeyName(), matcher.group(1));
			}
			
			legal = legal.replaceAll("(?is)LO?\\s*TS?\\s+(NO)?\\s*(?:[-\\d\\s&,]+)", "");
			legal = legal.replaceAll("(?is)\\b(?:BLDG|BUILDING)\\s+(?:\\d+|[A-Z])(?:\\s*&\\s*(?:UND)?\\s*.?\\d*)?", "");
			legal = legal.replaceAll("(?is)\\bREPLAT\\b.*", "");
			legal = legal.replaceAll("(?is)\\bSUB\\b.*", "");
			legal = legal.replaceAll("(?is)\\s*SEC(?:TION)?\\s*\\d*", "");
			legal = legal.replaceAll("(?is)\\bP\\s*H(?:ASE)?\\b.*", "");
			legal = legal.replaceFirst("(?is)\\s*TR(?:ACT)?\\s*(?:[-\\d\\s&,]+)", "");
			legal = legal.replaceAll("(?is)\\b*per\\b*.*", "");
			legal = legal.replaceAll("(?:PT)?(\\s*\\d{0,3}\\.(\\d){2,4}+\\s*A\\s*C?)?", "");// remove .00A or PT 22.1020 A C
			legal = legal.replaceAll("(?is)&?\\s*PT\\s+&?\\d*", "");
			legal = legal.trim();
			resultMap.put(PropertyIdentificationSetKey.SUBDIVISION_NAME.getKeyName(), legal);
		}
	}

	public static boolean parseAddressOnServer(ResultMap resultMap, Node addressNode) {
		if (addressNode != null) {
			String address = addressNode.toPlainTextString().trim();
			Matcher matcher = ZIP_PATTERN.matcher(address);
			if (matcher.find()) {
				resultMap.put(PropertyIdentificationSetKey.ZIP.getKeyName(), matcher.group(1));
				address = address.replaceFirst(matcher.group(0), "");
			}
			for (String city : cities)
			{
				if (address.matches(".*" + city + "\\s*$"))
				{
					resultMap.put(PropertyIdentificationSetKey.CITY.getKeyName(), city);
					address = address.replaceAll("(?is)\\s*" + city + "\\s*", "");
					break;
				}
			}
			resultMap.put(PropertyIdentificationSetKey.ADDRESS_ON_SERVER.getKeyName(), address);
		}
		if (resultMap == null) {
			return false;
		}
		String addressOnServer = (String) resultMap.get(PropertyIdentificationSetKey.ADDRESS_ON_SERVER.getKeyName());
		if (StringUtils.isEmpty(addressOnServer)) {
			return false;
		}
		String[] addressParts = StringFormats.parseAddress(addressOnServer);
		if (addressParts == null || addressParts.length != 2) {
			return false;
		}

		resultMap.put(PropertyIdentificationSetKey.STREET_NO.getKeyName(), addressParts[0]);
		resultMap.put(PropertyIdentificationSetKey.STREET_NAME.getKeyName(), addressParts[1]);
		return true;
	}

}
