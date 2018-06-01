package ro.cst.tsearch.servers.functions;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import ro.cst.tsearch.extractor.legal.LegalDescription;
import ro.cst.tsearch.extractor.xml.GenericFunctions1;
import ro.cst.tsearch.extractor.xml.ResultMap;
import ro.cst.tsearch.extractor.xml.StringFormats;
import ro.cst.tsearch.search.address2.Normalize;
import ro.cst.tsearch.search.address2.StandardAddress;
import ro.cst.tsearch.servers.response.PropertyIdentificationSet.PropertyIdentificationSetKey;
import ro.cst.tsearch.utils.RegExUtils;
import ro.cst.tsearch.utils.StringUtils;

public class ILWillAO implements ParserClass {

	private static Map<String, String> siteKeyValuesToAtsKeyValues = new HashMap<String, String>();
	static {
		siteKeyValuesToAtsKeyValues.put("Pin", "PropertyIdentificationSet.ParcelID");
		siteKeyValuesToAtsKeyValues.put("Address", "PropertyIdentificationSet.AddressOnServer");
		siteKeyValuesToAtsKeyValues.put("City", "PropertyIdentificationSet.City");
		siteKeyValuesToAtsKeyValues.put("Zip", "PropertyIdentificationSet.Zip");
		siteKeyValuesToAtsKeyValues.put("Class", "SaleDataSet.PropertyClass");
		siteKeyValuesToAtsKeyValues.put("Sale Date", "SaleDataSet.RecordedDate");
		siteKeyValuesToAtsKeyValues.put("Sale Price", "SaleDataSet.SalesPrice");
	}

	public static Map<String, String> parseLegalFromPIN(String pin) {
		String[] split = pin.split("-");
		Map<String, String> map = new HashMap<String, String>();
		if (split.length == 6) {
			map.put("SubdivisionLotNumber", StringUtils.removeLeadingZeroes(split[4]));
			map.put("SubdivisionBlock", StringUtils.removeLeadingZeroes(split[3]));
		}
		return map;
	}

	public static void parseAndFillResultMap(ResultMap resultMap, Map<String, String> tableRowData) {
		Set<String> keySet = tableRowData.keySet();
		for (String key : keySet) {
			resultMap.put(siteKeyValuesToAtsKeyValues.get(key), tableRowData.get(key));
		}
	}

	public static Map<String, String> parseAndFillResultMap(ResultMap rMap, String legalDescription, Map<String, String> pisMap2) {
		Map<String, String> map = new HashMap<String, String>();
		ResultMap newMap = new ResultMap();
		ILWillAO.setLot(newMap, legalDescription, map, pisMap2);
		setBlock(newMap, legalDescription, map, pisMap2);
		setUnit(rMap, newMap, legalDescription, map, pisMap2);
		setPhase(newMap, legalDescription, map);
		setSecTwnRng(newMap, legalDescription, map);
		setSubdivision(newMap, legalDescription, map);
		return map;
		// concatenate results
	}

	public static void setSubdivision(ResultMap resultMap, String legalDescription, Map<String, String> map) {
		String regex = "(?is)(\\b(IN|OF)\\b)(.*?\\bSUB\\b)";
		String subdivisionName = StringUtils.parseByRegEx(legalDescription, regex, 3);
		while (StringUtils.isNotEmpty(subdivisionName)) {
			subdivisionName = StringUtils.parseByRegEx(subdivisionName, regex, 3);
			String defaultStr = "";
			if (StringUtils.isNotEmpty(subdivisionName)) {
				String value = org.apache.commons.lang.StringUtils.defaultIfEmpty(subdivisionName.replaceAll("\\bSUB", "").trim(),
						defaultStr);
				String string = "PropertyIdentificationSet.";
				resultMap.put(string + "SubdivisionName", value);
			}
		}
	}

	public static void addValueToMaps(String keyPrefix, String key, ResultMap map1, Map map2, String value) {
		map1.put(keyPrefix + key, value.trim());
		map2.put(key, value.trim());
	}

	private static void setSecTwnRng(ResultMap resultMap, String legalDescription, Map<String, String> map) {
		// (?is)(SEC\\.?\\s+(\\d+),?\\s+)(T(\\d+\\w+)\\-R(\\d+\\w+))
		String section = StringUtils.parseByRegEx(legalDescription, "(?is)(SEC\\.?\\s+(\\d+),?\\s+)", 2);
		String[] twnRng = RegExUtils.parseByRegEx(legalDescription, "(?is)(SEC\\.?\\s+(\\d+),?\\s+)?(T(\\d+\\w+)(\\-|\\s)R(\\d+\\w+))",
				new int[] { 4, 6 });
		String defaultStr = "";
		String keyPrefix = "PropertyIdentificationSet.";

		String value = org.apache.commons.lang.StringUtils.defaultIfEmpty(section, defaultStr);
		addValueToMaps(keyPrefix, "SubdivisionSection", resultMap, map, value);

		// resultMap.put(keyPrefix + "SubdivisionSection", value);

		value = org.apache.commons.lang.StringUtils.defaultIfEmpty(twnRng[0], defaultStr);
		// resultMap.put(keyPrefix + "SubdivisionTownship", value);
		addValueToMaps(keyPrefix, "SubdivisionTownship", resultMap, map, value);

		value = org.apache.commons.lang.StringUtils.defaultIfEmpty(twnRng[1], defaultStr);
		// resultMap.put(keyPrefix + "SubdivisionRange", value);
		addValueToMaps(keyPrefix, "SubdivisionRange", resultMap, map, value);

	}

	private static void setPhase(ResultMap resultMap, String legalDescription, Map<String, String> map) {
		String unit = StringUtils.parseByRegEx(legalDescription, "(?is)PHASE\\s+(\\w+)", 1);
		String keyPrefix = "PropertyIdentificationSet.";
		addValueToMaps(keyPrefix, "SubdivisionPhase", resultMap, map, unit);
		// resultMap.put(keyPrefix + "SubdivisionPhase", unit);

	}

	public static void setUnit(ResultMap existingResultMap, ResultMap resultMap, String legalDescription, Map<String, String> map, Map<String, String> pisMap2) {
		String unit = StringUtils.parseByRegEx(legalDescription, "(?is)UNIT\\s+#?(\\w+)", 1);
		boolean alreadyAddressUnit = false;	//if the legal unit is the same with the address unit, do not add it
		String address = (String)existingResultMap.get(PropertyIdentificationSetKey.ADDRESS_ON_SERVER.getKeyName());
		if (!StringUtils.isEmpty(address)) {
			StandardAddress standardAddress = new StandardAddress(address);
			String addressUnit = standardAddress.getAddressElement(ro.cst.tsearch.search.address.StandardAddress.STREET_SEC_ADDR_RANGE);
			if (!StringUtils.isEmpty(addressUnit)) {
				if (addressUnit.equalsIgnoreCase(unit)) {
					alreadyAddressUnit = true;
				}
			}
		}
		if (alreadyAddressUnit) {
			unit = "";
		}
		unit += " " + pisMap2.get("SubdivisionUnit");
		unit = GenericFunctions1.replaceOnlyNumbers(unit);
		String keyPrefix = "PropertyIdentificationSet.";
		addValueToMaps(keyPrefix, "SubdivisionUnit", resultMap, map, unit);
		// resultMap.put("PropertyIdentificationSet.SubdivisionUnit", unit);
	}

	public static void setBlock(ResultMap resultMap, String legalDescription, Map<String, String> map, Map<String, String> pisMap2) {
		String[] blockIndividual = StringUtils.findRepeatedOcurrenceByRegExMarker(legalDescription, "BLK,?", "(?is)\\bBLK (\\d*)", 1);
		String result = StringUtils.addCollectionValuesToString("", blockIndividual);
		if (legalDescription.contains("BLKS")) {
			String regExBlocks = "(?is)BLKS\\s+([\\d+\\s]+)";
			String values = StringUtils.getSuccessionOFValues(legalDescription, "BLKS", regExBlocks);
			String[] blkSuccession = values.split(" ");
			result = StringUtils.addCollectionValuesToString(result, blkSuccession);
		}
		result += " " + pisMap2.get("SubdivisionBlock");
		result = LegalDescription.cleanValues(result, false, false);
		String keyPrefix = "PropertyIdentificationSet.";
		addValueToMaps(keyPrefix, "SubdivisionBlock", resultMap, map, result);
		// resultMap.put("PropertyIdentificationSet.SubdivisionBlock", result);
	}

	public static void parseAddress(ResultMap resultMap) {
		String addressOnServer = (String) resultMap.get("PropertyIdentificationSet.AddressOnServer");
		
		String split[] = addressOnServer.split("\\s+");
		int len = split.length;
		if (len>1) {
			if (Normalize.isSuffix(split[len-2]) && !Normalize.isDirectional(split[len-1]) 
					&& split[len-1].matches("[A-Z0-9]+(-[A-Z0-9]+)?")) {
				split[len-1] = "#" + split[len-1];	//unit number
			}
		}
		StringBuilder sb = new StringBuilder();
		for (int i=0;i<len;i++) {
			sb.append(split[i]).append(" ");
		}
		addressOnServer = sb.toString().trim();
		
		String streetName = StringFormats.StreetName(addressOnServer);
		String streetNo = StringFormats.StreetNo(addressOnServer);
		
		streetName = streetName.replaceAll("(?is)-([A-Z]|\\d+)\\s*$", " $1");
		streetName = streetName.replaceAll("(?is)\\bFIRST\\b", "1ST");//401 FIRST ST; ILWill
		streetName = streetName.replaceAll("(?is)\\bSECOND\\b", "2ND");
		streetName = streetName.replaceAll("(?is)\\bTHIRD\\b", "3RD");
		
		resultMap.put("PropertyIdentificationSet.StreetName", streetName);
		resultMap.put("PropertyIdentificationSet.StreetNo", streetNo);
	}

	public static void setLot(ResultMap resultMap, String legalDescription, Map<String, String> map, Map<String, String> pisMap2) {
		String[] lotIndividual = StringUtils.findRepeatedOcurrenceByRegExMarker(legalDescription, "LOT,?", "(?is)\\bLOT (\\d*)", 1);
		String regExLots = "(?is)LOTS ([\\d+\\s]+)";
		String result = StringUtils.addCollectionValuesToString("", lotIndividual);
		if (legalDescription.contains("LOTS")) {
			// clean
			// replace 3,4,5, with 3 4 5
			legalDescription = legalDescription.replaceAll("(\\d)(,)", "$1 ");
			String values = StringUtils.getSuccessionOFValues(legalDescription, "LOTS", regExLots);
			String[] lotSuccession = values.split(" ");
			result = StringUtils.addCollectionValuesToString(result, lotSuccession);
			String lotIntervalDelimiters = "(THRU|TO)";
			String lotInterval = StringUtils.parseByRegEx(legalDescription, "(?is)LOTS\\s*(\\d+\\s" + lotIntervalDelimiters + "\\s*?\\d+)",
					1);
			lotInterval = lotInterval.replaceAll(lotIntervalDelimiters, "-");
			result = StringUtils.addCollectionValuesToString(result, new String[] { lotInterval });
		}
		result += " " + pisMap2.get("SubdivisionLotNumber");
		result = LegalDescription.cleanValues(result, true, true);
		
		result = result.replaceAll("(?is)\\A\\s*\\p{Punct}", "");
		
		String keyPrefix = "PropertyIdentificationSet.";
		addValueToMaps(keyPrefix, "SubdivisionLotNumber", resultMap, map, result);
		// resultMap.put("PropertyIdentificationSet.SubdivisionLotNumber",
		// result);
	}

}
