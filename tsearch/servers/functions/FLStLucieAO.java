package ro.cst.tsearch.servers.functions;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;

import org.apache.commons.lang.StringUtils;

import ro.cst.tsearch.extractor.xml.GenericFunctions;
import ro.cst.tsearch.extractor.xml.GenericFunctions2;
import ro.cst.tsearch.extractor.xml.ResultMap;
import ro.cst.tsearch.extractor.xml.ResultTable;
import ro.cst.tsearch.extractor.xml.StringFormats;
import ro.cst.tsearch.search.name.NameUtils;
import ro.cst.tsearch.servers.response.CrossRefSet;
import ro.cst.tsearch.servers.response.PropertyIdentificationSet.PropertyIdentificationSetKey;
import ro.cst.tsearch.utils.RegExUtils;
import ro.cst.tsearch.utils.ResultBodyUtils;

public class FLStLucieAO {
	/**
	 * resultMap should be filled with what needs to be parsed with the keys
	 * that keep the original server data : e.g.
	 * PropertyIdentificationSet.NameOnServer for Owner name,
	 * PropertyIdentificationSet.AddressOnServer for address
	 * 
	 * @param resultMap
	 */
	public static void parseAndFillResultMap(ResultMap resultMap) {
		String nameOnServer = (String) resultMap.get("PropertyIdentificationSet.NameOnServer");
		if (StringUtils.isNotEmpty(nameOnServer)) {
			setName(resultMap, nameOnServer, null);
		}

		String addressOnServer = (String) resultMap.get("PropertyIdentificationSet.AddressOnServer");
		if (StringUtils.isNotEmpty(addressOnServer)) {
			setAddress(resultMap, addressOnServer);
		}
		setSectionTownshipRange(resultMap, "");
		
		String legalDescriptionOnServer = (String) resultMap.get("PropertyIdentificationSet.LegalDescriptionOnServer");
		if (StringUtils.isNotEmpty(legalDescriptionOnServer)){
			parseLegal(resultMap, legalDescriptionOnServer);
		}
	}

	public static void setSectionTownshipRange(ResultMap resultMap, String name) {
		name = StringUtils.defaultIfEmpty((String) resultMap.get("PropertyIdentificationSet.SubdivisionSection"), "");
		String[] split = name.split(":");
		if (split != null && split.length == 3) {
			resultMap.put("PropertyIdentificationSet.SubdivisionSection", split[0].trim().replaceFirst("^0+", ""));
			resultMap.put("PropertyIdentificationSet.SubdivisionTownship", split[1].trim().replaceFirst("^0+", ""));
			resultMap.put("PropertyIdentificationSet.SubdivisionRange", split[2].trim().replaceFirst("^0+", ""));
		}

	}

	@SuppressWarnings("rawtypes")
	public static void setName(ResultMap resultMap, String name, List body) {
		String tmpOwnerName = name;
		resultMap.put("PropertyIdentificationSet.NameOnServer", tmpOwnerName);
		tmpOwnerName = GenericFunctions2.cleanOwnerNameFromPrefix(tmpOwnerName);
		String intermediaryNameKey = "tmpIntermediaryName";
		String intermediaryName =(String) resultMap.get(intermediaryNameKey);
		if (intermediaryName==null) {
			intermediaryName = "";
		}
		intermediaryName =  StringUtils.strip(intermediaryName);
		String intermediaryName2 = intermediaryName.replaceAll("(?is),?\\s*ET(AL|UX|VIR)$", "");
		if (body == null) {
			body = new ArrayList();
		}

		// if (StringUtils.contains(tmpOwnerName, intermediaryName)) {
		/*
		 * // 1st owner Name String coOwner =
		 * tmpOwnerName.replace(intermediaryName, ""); if
		 * (StringUtils.isNotEmpty(coOwner)) { // setName(resultMap, coOwner);
		 * coOwner = cleanName(coOwner);
		 * ParseNameUtil.putNamesInResultMapFromDeSotoParse(resultMap, coOwner,
		 * body); }
		 * 
		 * // 2nd owner Name if (StringUtils.isNotEmpty(intermediaryName)) { //
		 * setName(resultMap, intermediaryName); intermediaryName =
		 * cleanName(intermediaryName);
		 * ParseNameUtil.putNamesInResultMapFromDeSotoParse(resultMap,
		 * intermediaryName, body); }b
		 * 
		 * 
		 * if (NameUtils.isCompany(tmpOwnerName)) { parseCompanyName(resultMap,
		 * tmpOwnerName, body); }
		 * 
		 * else { if (tmpOwnerName.equals(intermediaryName) ||
		 * StringUtils.isEmpty(intermediaryName)) {
		 * ParseNameUtil.putNamesInResultMapFromDeSotoParse(resultMap,
		 * tmpOwnerName, body); } else { if (StringUtils.contains(tmpOwnerName,
		 * intermediaryName)) { } } }
		 */
		if (tmpOwnerName.equals(intermediaryName) || StringUtils.isEmpty(intermediaryName)) {
			tmpOwnerName = cleanName(tmpOwnerName);
			if (NameUtils.isCompany(tmpOwnerName)) {
				parseCompanyName(resultMap, tmpOwnerName, body);
			} else {
				ParseNameUtil.putNamesInResultMapFromDeSotoParse(resultMap, tmpOwnerName, body);
			}

			// setName(resultMap, intermediaryName);
		} else {
			if (StringUtils.contains(tmpOwnerName, intermediaryName) || StringUtils.contains(tmpOwnerName, intermediaryName2)) {
				try {
					resultMap.remove(intermediaryNameKey);
				} catch (Exception e1) {
					e1.printStackTrace();
				}
				if (!StringUtils.contains(tmpOwnerName, intermediaryName) && StringUtils.contains(tmpOwnerName, intermediaryName2)) {
					intermediaryName = intermediaryName2;
				}
				String coOwner = tmpOwnerName.replace(intermediaryName, "");
				
				if (StringUtils.isNotEmpty(intermediaryName)) {
					// setName(resultMap, intermediaryName);
					intermediaryName = cleanName(intermediaryName).trim();
					setName(resultMap, intermediaryName, body);
					// ParseNameUtil.putNamesInResultMapFromDeSotoParse(resultMap,
					// intermediaryName, body);
				}
				
				if (StringUtils.isNotEmpty(coOwner)) {
					// setName(resultMap, coOwner);
					coOwner = cleanName(coOwner).trim();
					setName(resultMap, coOwner, body);
					// ParseNameUtil.putNamesInResultMapFromDeSotoParse(resultMap,
					// coOwner, body);
				}

			} else {
				tmpOwnerName = cleanName(tmpOwnerName);
				ParseNameUtil.putNamesInResultMapFromDeSotoParse(resultMap, tmpOwnerName, body);
			}
		}

		/*
		 * Pattern p =Pattern.compile(
		 * "([a-zA-Z]+'?[a-zA-Z]+)(\\s+\\w{1,1})?\\s+([a-zA-Z]+'?[a-zA-Z]+)\\s+([a-zA-Z]+'?[a-zA-Z]+)(\\s+\\w{1,1})?\\s+([a-zA-Z]+'?[a-zA-Z]+)\\s+"
		 * ); Matcher matcher = p.matcher(tmpOwnerName); int groupCount =
		 * matcher.groupCount(); if (groupCount == 4){//cazul FN LN WFN WLN
		 * tmpOwnerName = tmpOwnerName.replaceAll(tmpOwnerName,
		 * "$1 $2 AND $3 $4"); } if (groupCount ==5 ){ boolean
		 * firstNameHasMiddleInitial = matcher.group(1).length()==1; if
		 * (firstNameHasMiddleInitial){//cazul FN MI LN WFN WLN tmpOwnerName =
		 * tmpOwnerName.replaceAll(tmpOwnerName, "$1 $2 $3 AND $4 $5");
		 * }else{//cazul FN LN WFN WMI WLN tmpOwnerName =
		 * tmpOwnerName.replaceAll(tmpOwnerName, "$1 $2 AND $3 $4 $5"); } }
		 * 
		 * if (groupCount == 6 ){//cazul FN MI LN WFN WMI WLN tmpOwnerName =
		 * tmpOwnerName.replaceAll(tmpOwnerName, "$1 $2 AND $3 $4 $5"); }
		 */
		// }
	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	private static void parseCompanyName(ResultMap resultMap, String tmpOwnerName, List body) {
		String[] parsedName = new String[] { "", "", "", "", "", "" };
		parsedName[2] = tmpOwnerName;
		String[] suffixes = GenericFunctions.extractNameSuffixes(parsedName);
		GenericFunctions.addOwnerNames(parsedName, suffixes[0], suffixes[1], NameUtils.isCompany(parsedName[2]), NameUtils
				.isCompany(parsedName[5]), body);

		try {
			GenericFunctions.storeOwnerInPartyNames(resultMap, body);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	private static String cleanName(String tmpOwnerName) {
		tmpOwnerName = tmpOwnerName.replaceAll("(?is)\\(TR\\)", "TR");
		tmpOwnerName = tmpOwnerName.replaceAll("(?is)\\(((?:LI?FE?\\s+)?EST)\\)", "$1");
		tmpOwnerName = tmpOwnerName.replaceAll("(?is)\\bIRA\\b", "");
		tmpOwnerName = tmpOwnerName.replaceAll("(?is),\\s*(ETAL)\\b", " $1");
		return tmpOwnerName;
	}

	public static void setAddress(ResultMap resultMap, String addressOnServer) {
		//4416-501-0001-000-4 "TBD"
		resultMap.put("PropertyIdentificationSet.AddressOnServer", addressOnServer);
		// addressOnServer = addressOnServer.replaceAll(
		// "(?is)\\s(TAL|POR|ALE|OAK|CRA|PEM|MIR|HAV|LAK|HOP|DET|HOU|LUT|ROY|MON|CHA|QUI|WIC|JAC)\\s",
		// "");

		// addressOnServer = addressOnServer.replace(" & ", "-");
		addressOnServer = addressOnServer.replaceAll("-\\s\\d+", "");
		String streetName = StringFormats.StreetName(addressOnServer).trim();
		String streetNo = StringFormats.StreetNo(addressOnServer);

		resultMap.put("PropertyIdentificationSet.StreetName", streetName);
		resultMap.put("PropertyIdentificationSet.StreetNo", streetNo);

	}

	public static void parseLegal(ResultMap map, String legal) {
		setSection(map, legal);
		setUnit(map, legal);
		setBlock(map, legal);
		setPhase(map, legal);
		setLot(map, legal);
		setSubdivision(map, legal);
		setCrossRefSet(map, legal);
	}

	private static void setSubdivision(ResultMap map, String legal) {
		String subdivisionRegEx = "(.*?(?=(-|REPLAT|S/D|UNIT|BLK|\\(OR|PHASE)))";
		List<String> matches = RegExUtils.getMatches(subdivisionRegEx, legal, 1);
		
		if (matches.size()>=1){
			String subdivisionKey="PropertyIdentificationSet.SubdivisionName";
			String subdivisionName = matches.get(0).replaceAll("(?is)\\bLOT\\b.*", "").trim();
			String parseByRegEx = ro.cst.tsearch.utils.StringUtils.parseByRegEx(subdivisionName, "\\d+\\s\\d+", 0);
			if (StringUtils.isEmpty(parseByRegEx)){
				setKyInResultMap(map, subdivisionName, subdivisionKey, "");
			}
		}
		
	}

	private static void setPhase(ResultMap map, String legal) {
		String unit = ro.cst.tsearch.utils.StringUtils.parseByRegEx(legal, "PHASE (\\d+|[MCVI]+)", 1);
		String subdivisionKey = "PropertyIdentificationSet.SubdivisionPhase";

		String existingSection = (String) map.get(subdivisionKey);
		setKyInResultMap(map, unit, subdivisionKey, existingSection);
	}

	public static void setCrossRefSet(ResultMap map, String legal) {
		//int lotIndex = legal.indexOf("OR(");
		String regExRefs = "\\((PB|OR)(.*?)\\)";
		List<String> matches = RegExUtils.getMatches(regExRefs, legal,0);
		
		List<HashMap<String, String>> sourceSet = new LinkedList<HashMap<String,String>>();
		ResultTable saleDataSet = (ResultTable) map.get("SaleDataSet");
		for (String match : matches) {
			List<String> matches2 = RegExUtils.getMatches("\\d+-\\d+", match,0);
			for (String bookPage : matches2) {
				HashMap<String, String> bp = new HashMap<String, String>();
				String[] split = bookPage.split("-");
				if (split!=null && split.length == 2){
					String book = ro.cst.tsearch.utils.StringUtils.removeLeadingZeroes(split[0].trim());
					bp.put("Book" , book);
					String page = ro.cst.tsearch.utils.StringUtils.removeLeadingZeroes(split[1].trim());
					bp.put("Page" , page);
				}
				boolean isAlreadyIn = false;
				if (saleDataSet!=null) {
					for (String[] string : saleDataSet.getBody()) {
						if ((string[0].equals(bp.get("Book")) && string[1].equals(bp.get("Page")))){
							isAlreadyIn = true;
						}
					}
				}
				if (!isAlreadyIn){
					sourceSet.add(bp);
				}
				
			}
		}
		String[] header = new String[] {"Book", "Page"};
        
		ResultBodyUtils.buildInfSet(map, sourceSet, header , CrossRefSet.class);
		
//		map.put("PropertyIdentificationSet.SubdivisionLotNumber", lots.replaceAll("\\s+", " "));
		
	}

	public static void setLot(ResultMap map, String legal) {
		if (legal.contains("LOT")) {
			int lotIndex = legal.indexOf("LOT");
			String regExLots = "(?is)LOTS? ([\\d+\\s]+)";
			legal = legal.replaceAll("(\\d)(,)", "$1 ");
			legal = legal.replaceAll("\\b(AND)\\b", "&");
			if (lotIndex >= 0) {
				String lots = ro.cst.tsearch.utils.StringUtils.getSuccessionOFValues(legal, lotIndex, regExLots);				
				map.put("PropertyIdentificationSet.SubdivisionLotNumber", lots.replaceAll("\\s+", " "));
			}
		}
	}

	public static void setBlock(ResultMap map, String legal) {
		String unit = ro.cst.tsearch.utils.StringUtils.parseByRegEx(legal, "BLK\\s?((?:\\d+)|(?:[A-Z]))", 1);
		String subdivisionKey = "PropertyIdentificationSet.SubdivisionBlock";
		unit = StringUtils.removeEnd(unit, "-");

		String existingSection = (String) map.get(subdivisionKey);
		setKyInResultMap(map, unit, subdivisionKey, existingSection);
	}

	public static void setUnit(ResultMap map, String legal) {
		String unit = ro.cst.tsearch.utils.StringUtils.parseByRegEx(legal, "(UNIT|APT)\\s([A-Z\\-0-9]+)", 2);
		String subdivisionKey = "PropertyIdentificationSet.SubdivisionUnit";
		unit = StringUtils.removeEnd(unit, "-");

		String existingSection = (String) map.get(subdivisionKey);
		setKyInResultMap(map, unit, subdivisionKey, existingSection);
	}

	public static void setSection(ResultMap resultMap, String legal) {
		String legalSection = ro.cst.tsearch.utils.StringUtils.parseByRegEx(legal, "SECTION\\s?(\\d+)", 1);
		legalSection = ro.cst.tsearch.utils.StringUtils.removeLeadingZeroes(legalSection);
		String subdivisionKey = PropertyIdentificationSetKey.SECTION.getKeyName();
		String existingSection = (String) resultMap.get(subdivisionKey);

		setKyInResultMap(resultMap, legalSection, subdivisionKey, existingSection);

	}

	private static void setKyInResultMap(ResultMap resultMap, String legalSection, String subdivisionKey, String existingSection) {
		if (StringUtils.isNotEmpty(legalSection)) {
			if (StringUtils.isEmpty(existingSection)) {
				resultMap.put(subdivisionKey, legalSection);
			} else {
				resultMap.put(subdivisionKey, existingSection + " " + legalSection);
			}
		}
	}

	public static void parseAddress(ResultMap resultMap, String addressOnServer) {

	}

}
