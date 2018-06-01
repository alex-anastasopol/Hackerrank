package ro.cst.tsearch.servers.functions;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Comparator;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.htmlparser.Node;
import org.htmlparser.tags.TableColumn;
import org.htmlparser.tags.TableRow;
import org.htmlparser.tags.TableTag;

import ro.cst.tsearch.extractor.legal.LegalDescription;
import ro.cst.tsearch.extractor.xml.GenericFunctions;
import ro.cst.tsearch.extractor.xml.ResultMap;
import ro.cst.tsearch.extractor.xml.StringFormats;
import ro.cst.tsearch.search.name.NameUtils;
import ro.cst.tsearch.servers.response.PropertyIdentificationSet.PropertyIdentificationSetKey;
import ro.cst.tsearch.servers.response.TaxHistorySet.TaxHistorySetKey;
import ro.cst.tsearch.utils.Roman;
import ro.cst.tsearch.utils.StringUtils;

/**
 * 
 * @author Oprina George
 * 
 *         Feb 25, 2011
 */

public class NVClarkTR {

	public static ResultMap parseIntermediaryRow(TableRow row, long searchId) {
		ResultMap resultMap = new ResultMap();
		resultMap.put("OtherInformationSet.SrcType", "TR");

		TableColumn[] cols = row.getColumns();
		if (cols.length == 7) {
			resultMap.put("PropertyIdentificationSet.ParcelID", cols[0]
					.toPlainTextString().trim());
			resultMap.put("PropertyIdentificationSet.ParcelIDParcel", cols[0]
					.toPlainTextString().replaceAll("-", "").trim());

			resultMap.put("PropertyIdentificationSet.NameOnServer",
					cols[1].toPlainTextString()
							.replaceAll("\\s+", " ")
							.trim().replace("REV TR", "REVOCABLE TRUST")
							.replace("REV LIV TR", "REVOCABLE LIVING TRUST")
							.replace("REV LIVING TR", "REVOCABLE LIVING TRUST")
							.replace("REVOCABLE TR", "REVOCABLE TRUST")
							.replace("REVOCABLE LIV TR", "REVOCABLE LIVING TRUST")
							.replace("REVOCABLE LIVING TR", "REVOCABLE LIVING TRUST")
							.replace("FAM TR", "FAMILY TRUST")
							.replace("FAM LIV TR", "FAMILY LIVING TRUST")
							.replace("FAM LIVING TR", "FAMILY LIVING TRUST")
							.replace("FAMILY TR", "FAMILY TRUST")
							.replace("FAMILY LIV TR", "FAMILY LIVING TRUST")
							.replace("FAMILY LIVING TR", "FAMILY LIVING TRUST"));

			String[] addr = cols[6].toPlainTextString().trim().split(",");
			if (addr.length > 1) {
				resultMap.put("PropertyIdentificationSet.City", addr[1].trim());
			}
			String[] address = StringFormats.parseAddressShelbyAO(addr[0]);

			resultMap.put("PropertyIdentificationSet.StreetNo", address[0]);
			resultMap.put("PropertyIdentificationSet.StreetName", address[1]);
			
			String date = cols[5].toPlainTextString();
			
			if (StringUtils.isNotEmpty(date)) {
				if (date.matches("\\d+/\\d+/\\d{4}"))
					resultMap.put(TaxHistorySetKey.YEAR.getKeyName(), date.split("/")[2]);
				if(date.toLowerCase().contains("current"))
					resultMap.put(TaxHistorySetKey.YEAR.getKeyName(), Calendar.getInstance().get(Calendar.YEAR) + "");
			}

			parseNames(resultMap, searchId);

		}
		resultMap.removeTempDef();
		return resultMap;
	}

	public static void parseNames(ResultMap resultMap, long searchId) {
		String unparsedName = (String) resultMap
				.get("PropertyIdentificationSet.NameOnServer");

		resultMap.put(PropertyIdentificationSetKey.NAME_ON_SERVER.getKeyName(),
				unparsedName.replaceAll("&&", "&").replaceAll("\\s+", " ")
						.trim());

		@SuppressWarnings("rawtypes")
		List<List> body = new ArrayList<List>();
		try {
			if (StringUtils.isNotEmpty(unparsedName)) {
				// parseNameNashville
				/*
				 * ","unparsedName = unparsedName .replace("REV LIV TR", ",""REVOCABLE LIVING TRUST") .replace("FAM TR", "FAMILY TRUST")
				 * ",".replace("FAM LIV TR", "FAMILY LIVING TRUST") ",".replace("REVOCABLE LIVING TR", "REVOCABLE LIVING TRUST") ",".replace("REV TR",
				 * "REVOCABLE TRUST") ",".replace("FAMILY LIVING TR", "FAMILY LIVING TRUST") ",".replace("FAMILY TR", "FAMILY TRUST");
				 */
				String[] tokens = unparsedName.split("&&");
				for (int i = 0; i < tokens.length; i++) {

					if (tokens[i].trim().startsWith("%")) {
						String[] nms = tokens[i].split("\\s*%\\s*");
						for (String string : nms) {
							if (StringUtils.isNotEmpty(string)) {
								String[] names = StringFormats.parseNameNashville(string, true);
								if (names[2].length() == 1) {
									names = StringFormats.parseNameDesotoRO(string, true);
								}
								String[] type = GenericFunctions.extractAllNamesType(names);
								String[] otherType = GenericFunctions.extractAllNamesOtherType(names);
								String[] suffixes = GenericFunctions.extractNameSuffixes(names);
								GenericFunctions.addOwnerNames(names, suffixes[0], suffixes[1], type, otherType,
										NameUtils.isCompany(names[2]), NameUtils.isCompany(names[5]), body);
							}
						}
					} else {
						String[] names = StringFormats.parseNameNashville(
								tokens[i].replaceAll("\\s*&\\s*", " & "), true);
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
				}
				GenericFunctions.storeOwnerInPartyNames(resultMap, body, true);
				return;
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public static void parseLegalSummary(ResultMap resultMap, String legal_des) {
		resultMap.put("PropertyIdentificationSet.PropertyDescription",
				legal_des);

		Pattern LEGAL_TRACT_PATTERN = Pattern.compile("\\bTRACT\\s+#?([^\\s]+)");
		//Pattern LEGAL_BOOK_PATTERN = Pattern.compile("\\bBOOK\\s+([^\\s]*)");
		//Pattern LEGAL_PAGE_PATTERN = Pattern.compile("\\bPAGE\\s+([^\\s]*)");
		Pattern LEGAL_PLAT_BOOK_PAGE = Pattern.compile("PLAT\\s+BOOK\\s*([^\\s]+)\\s+PAGE\\s*([^\\s]+)");

		Pattern LEGAL_NAME_PATTERN1 = Pattern.compile(":\\s+(.*)LOT");
		Pattern LEGAL_NAME_PATTERN2 = Pattern.compile(":\\s+(.*)PLAT");
		Pattern LEGAL_NAME_PATTERN3 = Pattern.compile(":\\s+(.*)UNIT");
		Pattern LEGAL_NAME_PATTERN4 = Pattern.compile(":\\s+(.*)TRACT");
		Pattern LEGAL_NAME_PATTERN5 = Pattern.compile(":\\s+(.*)PHASE");
		Pattern LEGAL_NAME_PATTERN6 = Pattern.compile(":\\s+(.*)PARCEL\\s+MAP");
		
		
		
		Pattern LEGAL_PARCEL_MAP = Pattern.compile("\\bPARCEL\\s+MAP\\s*([^-]+)\\s*-\\s*([^\\s]+)");
		Pattern LEGAL_PARCEL_MAP2 = Pattern.compile("\\bPARCEL\\s+MAP\\s+FILE\\s*([^\\s]+)\\s+PAGE\\s+([^\\s]+)");

		Pattern[] name = { LEGAL_NAME_PATTERN1, LEGAL_NAME_PATTERN2,
				LEGAL_NAME_PATTERN3, LEGAL_NAME_PATTERN4, LEGAL_NAME_PATTERN5, LEGAL_NAME_PATTERN6 };

		legal_des = legal_des.replaceAll("[\\)\\(]+", "");

		String legalDescriptionFake = legal_des;
		Matcher matcher = LEGAL_NAME_PATTERN1.matcher(legalDescriptionFake);
		String[] subdiv_name = new String[name.length];
		String div_name = "";

		for (int i = 0; i < name.length; i++) {
			matcher = name[i].matcher(legalDescriptionFake);
			if (matcher.find()) {
				subdiv_name[i] = matcher.group(1);
			}
			if (!StringUtils.isEmpty(subdiv_name[i]))
				div_name = subdiv_name[i];
		}

		// get shortest div name
		for (int i = 0; i < name.length; i++) {
			if (subdiv_name[i] != null && !div_name.equals("")
					&& !subdiv_name[i].equals("")
					&& subdiv_name[i].length() < div_name.length())
				div_name = subdiv_name[i];
		}
		div_name = div_name.replaceAll("-$", " ").replaceAll("\\s{2,}", " ").trim();
		if(!div_name.startsWith("PARCEL MAP")) {
			resultMap.put("PropertyIdentificationSet.SubdivisionName", div_name);
		}

		matcher = LEGAL_TRACT_PATTERN.matcher(legalDescriptionFake);
		if (matcher.find()) {
			legalDescriptionFake = legalDescriptionFake.replace(
					matcher.group(), " ");
			resultMap.put("PropertyIdentificationSet.SubdivisionTract", matcher.group(1).replace("#", ""));
		}

		String block = LegalDescription.extractBlockFromText(
				legalDescriptionFake).replace("GEOID:", "");
		if (block.length() > 0)
			resultMap.put("PropertyIdentificationSet.SubdivisionBlock", block);

		matcher = LEGAL_PLAT_BOOK_PAGE.matcher(legalDescriptionFake);
		if (matcher.find()) {
			legalDescriptionFake = legalDescriptionFake.replace(matcher.group(), " ");
			resultMap.put(PropertyIdentificationSetKey.PLAT_BOOK.getKeyName(), matcher.group(1));
			resultMap.put(PropertyIdentificationSetKey.PLAT_NO.getKeyName(), matcher.group(2));
		}
		
		matcher = LEGAL_PARCEL_MAP.matcher(legalDescriptionFake);
		if (matcher.find()) {
			legalDescriptionFake = legalDescriptionFake.replace(matcher.group(), " ");
			resultMap.put(PropertyIdentificationSetKey.PLAT_BOOK.getKeyName(),
					"PM" + matcher.group(1));
			resultMap.put(PropertyIdentificationSetKey.PLAT_NO.getKeyName(),
					matcher.group(2));
		}
		matcher = LEGAL_PARCEL_MAP2.matcher(legalDescriptionFake);
		if (matcher.find()) {
			legalDescriptionFake = legalDescriptionFake.replace(matcher.group(), " ");
			resultMap.put(PropertyIdentificationSetKey.PLAT_BOOK.getKeyName(), "PM" + matcher.group(1));
			resultMap.put(PropertyIdentificationSetKey.PLAT_NO.getKeyName(), matcher.group(2));
		}
		
		String lot = LegalDescription.extractLotFromText(legalDescriptionFake)
				.replace("GEOID:", "");
		if (lot.length() > 0)
			resultMap.put("PropertyIdentificationSet.SubdivisionLotNumber",
					Roman.normalizeRomanNumbers(lot));

		String unit = LegalDescription
				.extractUnitFromText(legalDescriptionFake);
		if (unit.length() > 0)
			resultMap.put("PropertyIdentificationSet.SubdivisionUnit",
					Roman.normalizeRomanNumbers(unit));

		String section = LegalDescription
				.extractSectionFromText(legalDescriptionFake);
		if (section.length() > 0)
			resultMap
					.put("PropertyIdentificationSet.SubdivisionSection",
							Roman.normalizeRomanNumbers(section).replaceAll(
									"0+[^\\d]*", ""));

		String[] township_range = legal_des.substring(legal_des.indexOf("SEC"))
				.split(" ");
		if (township_range.length == 4) {
			resultMap.put("PropertyIdentificationSet.SubdivisionRange",
					township_range[township_range.length - 1]);
			resultMap.put("PropertyIdentificationSet.SubdivisionTownship",
					township_range[township_range.length - 2]);

		}

		String phase = LegalDescription
				.extractPhaseFromText(legalDescriptionFake);
		if (phase.length() > 0)
			resultMap.put("PropertyIdentificationSet.SubdivisionPhase", phase);

		String bldg = LegalDescription
				.extractBuildingFromText(legalDescriptionFake);
		if (bldg.length() > 0)
			resultMap.put("PropertyIdentificationSet.SubdivisionBldg", bldg);
	}

	// city vector sorted by length
	public final static String[]	cities	= { "Kyle Canyon Summer Home Area",
											"Lee Canyon Summer Home Area", "Desert View Point",
											"Eastland Heights", "Mountain Springs", "Mount Charleston",
											"Victory Village", "Mountain's Edge", "Bonanza Village",
											"North Las Vegas", "Lucky Jim Camp", "Stewarts Point",
											"Indian Springs", "East Las Vegas", "Cactus Springs",
											"Bonnie Springs", "Vegas Heights", "Spring Valley", "Lincoln City",
											"Sandy Valley", "Moapa Valley", "Boulder City", "Texas Acres",
											"Searchlight", "Goodsprings", "Carver Park", "Cal-Nev-Ari",
											"Bunkerville", "Sandy Mill", "Marie Camp", "Winchester",
											"Moapa Town", "Enterprise", "Riverside", "Logandale", "Las Vegas",
											"Henderson", "Arrowhead", "Paradise", "Mesquite", "Laughlin",
											"Glendale", "Glassand", "Echo Bay", "Dry Lake", "Junction",
											"Arrolime", "Whitney", "Sunrise", "Overton", "Jackman", "Farrier",
											"Bracken", "Boulder", "Diamond", "Valley", "Ripley", "Nelson",
											"Lovell", "Garnet", "Manor", "Sloan", "Roach", "Primm", "Moapa",
											"Borax", "Arden", "Amber", "Wann", "Jean", "Erie", "Dike", "Blue",
											"Bard", "Apex", "Ute" };

	public static void parseAddress(ResultMap resultMap, String address) {

		String city = "";
		boolean foundValidCity = false;
		for (int i = 0; i < cities.length; i++) {
			city = cities[i];
			if (address.contains(cities[i].toUpperCase())) {
				// 5047 WILLOWLYN CRT WHITNEY RANCH -> 5047 WILLOWLYN CRT    
				address = address.replaceFirst(Pattern.quote(city.toUpperCase())+".*", "");
				foundValidCity = true;
				break;
			}
		}

		if(foundValidCity) {
			resultMap.put("PropertyIdentificationSet.City", city.toUpperCase());
		}

		resultMap.put("PropertyIdentificationSet.StreetNo",
				StringFormats.StreetNo(address));
		resultMap.put("PropertyIdentificationSet.StreetName",
				StringFormats.StreetName(address));

	}

	public static class NodeComp implements Comparator<Node> {

		@Override
		public int compare(Node o1, Node o2) {
			if (o1.toHtml().length() > o2.toHtml().length())
				return 1;
			else if (o1.toHtml().length() < o2.toHtml().length())
				return -1;
			else
				return 0;
		}
	}

	public static TableTag getTableByContent(Node[] nodes, String[] content) {

		for (int i = 0; i < nodes.length; i++) {
			boolean flag = true;
			for (String s : content) {
				if (!nodes[i].toHtml().contains(s))
					flag = false;
			}
			if (flag)
				return (TableTag) nodes[i];
		}

		return null;
	}
}
