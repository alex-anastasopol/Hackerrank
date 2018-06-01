package ro.cst.tsearch.servers.functions;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import org.htmlparser.tags.TableTag;
import org.htmlparser.util.NodeList;
import org.htmlparser.util.ParserUtils;

import ro.cst.tsearch.extractor.xml.GenericFunctions;
import ro.cst.tsearch.extractor.xml.GenericFunctions2;
import ro.cst.tsearch.extractor.xml.ResultMap;
import ro.cst.tsearch.extractor.xml.StringFormats;
import ro.cst.tsearch.parser.HtmlParser3;
import ro.cst.tsearch.search.name.NameUtils;
import ro.cst.tsearch.utils.RegExUtils;
import ro.cst.tsearch.utils.ResultBodyUtils;
import ro.cst.tsearch.utils.StringUtils;

public class ILMcHenryAO {
	public static void parseAddress(ResultMap resultMap, String addressOnServer) {
		resultMap.put("PropertyIdentificationSet.AddressOnServer", addressOnServer);
		addressOnServer = addressOnServer.replaceAll(
				"(?is)\\s(TAL|POR|ALE|OAK|CRA|PEM|MIR|HAV|LAK|HOP|DET|HOU|LUT|ROY|MON|CHA|QUI|WIC|JAC)\\s", "");

		addressOnServer = addressOnServer.replace(" & ", "-");
		String streetName = StringFormats.StreetName(addressOnServer);
		String streetNo = StringFormats.StreetNo(addressOnServer);

		resultMap.put("PropertyIdentificationSet.StreetName", streetName);
		resultMap.put("PropertyIdentificationSet.StreetNo", streetNo);
	}

	public static void parseName(ResultMap resultMap, String tmpOwnerName) {
		String[] ownerName = { "", "", "", "", "", "" };
		resultMap.put("PropertyIdentificationSet.NameOnServer", tmpOwnerName);

		tmpOwnerName = GenericFunctions2.cleanOwnerNameFromPrefix(tmpOwnerName);
		tmpOwnerName = tmpOwnerName.replaceAll("\\d{2,2}-\\d{2,2}-\\d{3,3}-\\d{3,3}:", "");
		tmpOwnerName = tmpOwnerName.replaceAll("/", " & ");
		tmpOwnerName = tmpOwnerName.replaceAll(", INC", " INC");

		ParseNameUtil.putNamesInResultMapFromNashvilleParse(resultMap, tmpOwnerName, null);
	}

	public static void parseCityStateZip(ResultMap resultMap, String tmp) {
		String[] split = tmp.split(",");
		if (split.length == 2) {
			resultMap.put("PropertyIdentificationSet.City", split[0]);
			String string = split[1];
			resultMap.put("PropertyIdentificationSet.State", RegExUtils.parseValuesForRegEx(string, "(?is)[A-Z]"));
			resultMap.put("PropertyIdentificationSet.Zip", RegExUtils.parseValuesForRegEx(string, "\\d+"));
		}
	}

	public static void parseLotNumber(ResultMap resultMap, String tmp) {
		tmp = tmp.replaceAll("CCP", " ").replaceAll("PT", " ").replaceAll("LT", " ").replaceAll("N45FT", " ")
				.replaceAll("(?is)\\d+FT", " ").replaceAll("SE\\d+/(\\d+)?", "");
		if (tmp.startsWith("UNIT")) {
			setUnit(resultMap, tmp);
		} else {
			if (!tmp.contains("LOT")) {
				if (tmp.contains("&")) {
					tmp = "LOTS " + tmp;
				} else {
					tmp = "LOT " + tmp;
				}
			}
			setLot(resultMap, tmp);
		}

	}

	public static void parseLegalDescription(ResultMap resultMap, String tmpLegalDescription) {
		// cleaning
		// 09-23-277-079: RUNNING BROOK FARM OF JOHNSBURG PHASE 1; LT 40-9
		tmpLegalDescription = tmpLegalDescription.replaceAll("(?is)(LT)\\s?(\\w+)-(\\d+)", "$1 $2 B$3");

		tmpLegalDescription = setUnit(resultMap, tmpLegalDescription);
		tmpLegalDescription = setPhase(resultMap, tmpLegalDescription);
		tmpLegalDescription = setPlat(resultMap, tmpLegalDescription);
		tmpLegalDescription = setSection(resultMap, tmpLegalDescription);
		String terminations = "OUT|LOT|;|U#|REPLAT|PLAT|PT|/EX|U#|ESTATES|UNIT|LOTS|U\\d+|#\\d+|NEIGHBORHOOD";
		String subdivRegEx = "(?is)[ \\w&]+\\s?(?=" + terminations + ")";
		tmpLegalDescription = tmpLegalDescription.replaceAll("\\r\\n", " ");
		
		String subdivision = StringUtils.parseByRegEx(tmpLegalDescription, subdivRegEx, 0);
		subdivision = subdivision.replaceAll(terminations, "");
		subdivision = subdivision.replaceAll("(?is)\\d+\\sTP\\b", "").trim();
		if (subdivision.length() == 1){
			subdivision = "";
		}

		// clean subdivision LOT 2
		subdivision = subdivision.replaceAll("(?is)LOT \\d+", " ").replaceAll("&", "");
		resultMap.put("PropertyIdentificationSet.SubdivisionName", subdivision);

		tmpLegalDescription = setLot(resultMap, tmpLegalDescription);
		tmpLegalDescription = setBlock(resultMap, tmpLegalDescription);
		setCrossRef(resultMap, tmpLegalDescription);

	}

	private static void setCrossRef(ResultMap map, String legalDescription) {
		String regEx = "(?is)\\d{2,4}R\\d+\\b";
		String crossRef = StringUtils.parseByRegEx(legalDescription, regEx, 0);
		map.put("CrossRefSet.InstrumentNumber", crossRef);
	}

	private static String setPlat(ResultMap resultMap, String tmpLegalDescription) {
		String regex = "(?is)PLAT\\s+#?(\\d+)";
		String parseByRegEx = StringUtils.parseByRegEx(tmpLegalDescription, regex, 1);
		resultMap.put("PropertyIdentificationSet.PlatNo", parseByRegEx);
		tmpLegalDescription = tmpLegalDescription.replaceAll(regex, "");
		return tmpLegalDescription;
	}

	private static String setSection(ResultMap resultMap, String tmpLegalDescription) {
		String regex = "(?is)SECT?S? (\\d+(-|\\s)\\d+(-|\\s)\\d+(-|\\s))";
		String parseByRegEx = StringUtils.parseByRegEx(tmpLegalDescription, regex, 1);
		if (parseByRegEx.contains("-")) {
			String[] split = parseByRegEx.split("-");
			if (split.length == 3) {
				resultMap.put("PropertyIdentificationSet.SubdivisionSection", split[0]);
				resultMap.put("PropertyIdentificationSet.SubdivisionTownship", split[1]);
				resultMap.put("PropertyIdentificationSet.SubdivisionRange", split[2]);
			} else {
				resultMap.put("PropertyIdentificationSet.SubdivisionSection", parseByRegEx);
			}
		} else {
			resultMap.put("PropertyIdentificationSet.SubdivisionSection", parseByRegEx);
		}
		tmpLegalDescription = tmpLegalDescription.replaceAll(regex, "");
		return tmpLegalDescription;
	}

	private static String setUnit(ResultMap resultMap, String legalDescription) {
		// "(?is)UNIT\\s+([\\d+|\\w{1,2}\\-\\d\\s]+)";
		String tmpLegalDescription = legalDescription;
		tmpLegalDescription = tmpLegalDescription.replaceAll("(?is)U(\\d+)", "UNIT $1");
		tmpLegalDescription = tmpLegalDescription.replaceAll("U#", "UNIT ");

		tmpLegalDescription = tmpLegalDescription.replace(" OF ", ": OF ");
		tmpLegalDescription = tmpLegalDescription.replace(" LOT ", ": LOT ");
		tmpLegalDescription = tmpLegalDescription.replace(" LT ", ": LT ");
		tmpLegalDescription = tmpLegalDescription.replace(" ELY ", ": ELY ");

		String regex = "(?is)UNIT\\s+#?([\\d+|\\w{1,2}\\-\\d\\s]+)";
		String units = FLLeonTR.getSuccessionOFValues(tmpLegalDescription, "UNIT ", regex, 1);
		tmpLegalDescription = tmpLegalDescription.replaceAll(regex, "");
		if (StringUtils.isEmpty(units)) {
			regex = "(?is)U#((\\d{1,3}\\w{1,1}?\\s+)+)";
			units = FLLeonTR.getSuccessionOFValues(tmpLegalDescription, "U#", regex, 1);
			tmpLegalDescription = tmpLegalDescription.replaceAll(regex, "");
		}
		String key = "PropertyIdentificationSet.SubdivisionUnit";

		String currentValue = (String) resultMap.get(key);
		if (StringUtils.isEmpty(currentValue)) {
			resultMap.put(key, units);
		} else {
			if (!units.contains(currentValue)) {
				units += " " + currentValue;
				resultMap.put(key, units);
			}
		}

		return tmpLegalDescription;
	}

	private static String setPhase(ResultMap resultMap, String tmpLegalDescription) {
		String regex = "(?is)PHASE (\\d+)";
		String phase = StringUtils.parseByRegEx(tmpLegalDescription, regex, 1);
		tmpLegalDescription.replaceAll("#", " ");
		resultMap.put("PropertyIdentificationSet.SubdivisionPhase", phase);
		return tmpLegalDescription.replaceAll(regex, "");
	}

	private static String setBlock(ResultMap resultMap, String tmpLegalDescription) {
		String regex = "(?is)\\sBL??K?\\s?(\\d+)";
		String block = StringUtils.parseByRegEx(tmpLegalDescription, regex, 1);
		resultMap.put("PropertyIdentificationSet.SubdivisionBlock", block);
		return tmpLegalDescription.replaceAll(regex, "");
	}

	private static String setLot(ResultMap resultMap, String tmpLegalDescription) {
		
		int lotIndex = tmpLegalDescription.indexOf("LOTS");
		lotIndex = (lotIndex == -1) ? tmpLegalDescription.indexOf("LTS") : lotIndex;
		// cleaning
		tmpLegalDescription = tmpLegalDescription.replaceAll(";", " ").replaceAll("-", " ").replaceAll(",", " ");
		// FAIR OAKS 1ST ADDN ; LOTS 1,23 & 24 70.9X249.6X108.6X124.9X50X124.9
		tmpLegalDescription = tmpLegalDescription.replaceAll("(?is)(\\w+\\.\\w+|\\.\\w+)", "");
		String lot = "";
		if (lotIndex > -1) {// LOTS 1,23 & 24
			String regExLots = "(?is)LO?TS ([\\d+\\s]+)";
			String regExMarker = "(?is)LO?TS";
			String lots = StringUtils.getSuccessionOfValuesByRegEX(tmpLegalDescription, regExMarker, regExLots);
			lot = lots.replaceAll("\\s+", " ");
		} else {// LOT 2
			String regex = "(?is)(LT|LOT)\\s*?(\\w+)";// "(?is)(LT|LOT)\\s?(\\d+)";
			// lot = StringUtils.parseByRegEx(tmpLegalDescription, regex, 2);
			String[] lots = StringUtils.findRepeatedOcurrenceByRegExMarker(tmpLegalDescription, "(?is)LT", regex, 2);
			String[] lots1 = StringUtils.findRepeatedOcurrenceByRegExMarker(tmpLegalDescription, "(?is)LOT", regex, 2);

			lot = org.apache.commons.lang.StringUtils.join(lots, " ") + " " + org.apache.commons.lang.StringUtils.join(lots1, " ");
			// if (StringUtils.isEmpty(lot)) {// cases like "11 B2" where 11 is
			// lot
			// regex = "(?is)\\d+\\s?(?=B\\d+)";
			// lot = StringUtils.parseByRegEx(tmpLegalDescription, regex, 0);
			// tmpLegalDescription = tmpLegalDescription.replaceAll(regex, "");
			// }
		}
		String currentValue = (String) resultMap.get("PropertyIdentificationSet.SubdivisionLotNumber");
		// test id the lot is already there
		if (StringUtils.isNotEmpty(currentValue) && !currentValue.contains(lot.trim())) {
			// if (RegExUtils.matches("\\b" + currentValue + "\\b", lot)) {
			lot = lot + " " + currentValue;
			// }
		} else {
			if (StringUtils.isNotEmpty(currentValue)) {
				lot = currentValue;
			}
		}
		resultMap.put("PropertyIdentificationSet.SubdivisionLotNumber", lot);
		return tmpLegalDescription;
	}

	public static void parseSaleDataSet(NodeList nodeList, ResultMap map) {
		nodeList.elementAt(0).getChildren().remove(1);
		List<List<String>> tableAsList = HtmlParser3.getTableAsList((TableTag) nodeList.elementAt(0), false);
		List<HashMap<String, String>> saleDataSet = new ArrayList<HashMap<String, String>>();
		String crossRefInstrNumber = (String) map.get("CrossRefSet.InstrumentNumber");
		if (tableAsList.size() > 0) {
			for (List<String> list : tableAsList) {
				HashMap<String, String> hashMap = new HashMap<String, String>();
				hashMap.put("InstrumentDate", StringUtils.cleanHtml(list.get(1)).replaceAll(",", ""));
				hashMap.put("SalesPrice", StringUtils.cleanHtml(list.get(2).replaceAll(",", "").replaceAll("\\$", "")));
				hashMap.put("DocumentType", StringUtils.cleanHtml(list.get(3).replaceAll(",", "").replaceAll("\\$", "")));
				
				String instrNumber = "";
				if (StringUtils.isNotEmpty(list.get(4))) {
					instrNumber = StringUtils.cleanHtml(list.get(4).replaceAll(",", "").replaceAll("\\$", ""));
					String[] instrNumberParts = instrNumber.split("R");
					if (instrNumberParts.length == 2){
						if (instrNumberParts[0].length() == 2){
							instrNumber = "19" + instrNumberParts[0] + "R" + org.apache.commons.lang.StringUtils.leftPad(instrNumberParts[1], 7, "0");
						} else{
							instrNumber = instrNumberParts[0] + "R" + org.apache.commons.lang.StringUtils.leftPad(instrNumberParts[1], 7, "0");
						}
					}
				}

				hashMap.put("InstrumentNumber", instrNumber);
				if (crossRefInstrNumber.contains(instrNumber)){
					map.put("CrossRefSet.InstrumentNumber", "");
				}
				saleDataSet.add(hashMap);
			}
			ResultBodyUtils.buildSaleDataSet(map, saleDataSet);
		}
	}

}
