package ro.cst.tsearch.servers.functions;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;

import org.apache.commons.lang.StringUtils;

import ro.cst.tsearch.extractor.xml.ResultMap;
import ro.cst.tsearch.reports.comparators.IntegerComparator;
import ro.cst.tsearch.utils.RegExUtils;

public class COMesaTR {

	public static void parseLegal(String contents, ResultMap resultMap) {
		parseLot(contents, resultMap);
		parseBlock(contents, resultMap);
		parseSecTwnRng(contents, resultMap);
		parseUnit(contents, resultMap);
		parsePhase(contents, resultMap);
		parseSubdivision(contents, resultMap);
		
	}


	public static void parseSubdivision(String contents, ResultMap resultMap) {
		String subdivision = RegExUtils.getFirstMatch("(?<=LOT|BLK)\\s\\d+(.*)SUBDIVISION", contents, 1);
		resultMap.put("PropertyIdentificationSet.SubdivisionName", subdivision.trim());
	}

	public static void parsePhase(String contents, ResultMap resultMap) {
		String phase = RegExUtils.getFirstMatch("\\bPHASE\\s(\\d+)", contents, 1);
		resultMap.put("PropertyIdentificationSet.SubdivisionPhase", phase.trim());
	}

	public static void parseUnit(String contents, ResultMap resultMap) {
		String unit = RegExUtils.getFirstMatch("\\bUNIT\\s(\\d+\\s?\\d+)", contents, 1);
		String existingUnit = (String) resultMap.get("PropertyIdentificationSet.SubdivisionUnit");
		if (!StringUtils.isEmpty(existingUnit)) {
			unit = ro.cst.tsearch.utils.StringUtils.removeLeadingZeroes(unit);
			unit = existingUnit.contains(unit.trim()) ? existingUnit : unit + " " + existingUnit;
		}
		resultMap.put("PropertyIdentificationSet.SubdivisionUnit", unit.trim());
	}

	public static void parseSecTwnRng(String contents, ResultMap resultMap) {
		String secTwnRange = RegExUtils.getFirstMatch("SEC(TION)?\\s(\\w{1,3})\\s(\\w{1,3})\\s(\\w{1,3})", contents, 0);
		secTwnRange = secTwnRange.replaceAll("SEC(TION)?" , "").trim();
		
		String[] split = secTwnRange.split("\\s");
		int i = 0;
		if (split.length == 3){
			resultMap.put("PropertyIdentificationSet.SubdivisionSection", split[i]);
			resultMap.put("PropertyIdentificationSet.SubdivisionTownship", split[++i]);
			resultMap.put("PropertyIdentificationSet.SubdivisionRange", split[++i]);
		}
	}

	public static void parseBlock(String contents, ResultMap resultMap) {
		String block = RegExUtils.getFirstMatch("BLO?C?K\\s(\\w+)", contents, 1);
		resultMap.put("PropertyIdentificationSet.SubdivisionBlock", block.trim());
	}

	public static void parseLot(String contents, ResultMap resultMap) {
		// parse the intervals
		List<String> intervals = RegExUtils.getMatches("LOTS?\\s*(\\d+\\s*(THRU|TO)\\s*\\d+)", contents, 1);

		List<String> cleanedIntervals = new ArrayList<String>();
		String result = "";

		// clean intervals
		for (String interval : intervals) {
			String replaceAll = interval.replaceAll("(\\d+)\\s*(THRU|TO)\\s*(\\d+)", "$1 THRU $3");
			cleanedIntervals.add(replaceAll);
			result = FLColumbiaTR.enumerationFromInterval(result, replaceAll);
		}

		// find simple case LOT 5 sdafsd LOT 6
		String lotRegEx = "\\b(LOT)(\\s\\w+)";
		String[] findRepeatedOcurrence = ro.cst.tsearch.utils.StringUtils.findRepeatedOcurrence(contents, "LOT ", lotRegEx, 2);
		result = ro.cst.tsearch.utils.StringUtils.addCollectionValuesToString(result, findRepeatedOcurrence);

		// parse enumeration LOTS 4-5-6-7
		List<String> lotEnumeration = RegExUtils.getMatches("LOTS?\\s(\\s?(\\d+)\\s?(-|\\+|&)?)+", contents, 0);

		for (String string : lotEnumeration) {
			result += " " + string.replaceAll("&|\\+|-", " ").replaceAll("LOTS?", "");
		}
		
		String[] strings = result.split("\\s+");
		Arrays.sort(strings, new IntegerComparator());
		String lotValues = ro.cst.tsearch.utils.StringUtils.addCollectionValuesToString("", strings).trim();

		resultMap.put("PropertyIdentificationSet.SubdivisionLotNumber", lotValues);
	}

	public static void parseAddress(ResultMap resultMap, String address) throws Exception {
		COGarfieldTR.parseAddress(resultMap, address);
	}

	public static void parseName(Set<String> hashSet, ResultMap resultMap) throws Exception {
		COWeldTR.parseName(hashSet, resultMap);
	}

}
