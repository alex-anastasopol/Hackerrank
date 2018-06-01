package ro.cst.tsearch.servers.functions;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collections;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang.StringUtils;

import ro.cst.tsearch.extractor.legal.LegalDescription;
import ro.cst.tsearch.extractor.xml.GenericFunctions;
import ro.cst.tsearch.extractor.xml.GenericFunctions2;
import ro.cst.tsearch.extractor.xml.ResultMap;
import ro.cst.tsearch.extractor.xml.ResultTable;
import ro.cst.tsearch.extractor.xml.StringFormats;
import ro.cst.tsearch.search.name.FirstNameUtils;
import ro.cst.tsearch.search.name.NameUtils;
import ro.cst.tsearch.servers.response.CrossRefSet.CrossRefSetKey;
import ro.cst.tsearch.servers.response.PropertyIdentificationSet.PropertyIdentificationSetKey;
import ro.cst.tsearch.servers.response.SaleDataSet.SaleDataSetKey;
import ro.cst.tsearch.utils.RegExUtils;

public class ARWashingtonRO {
	
	public static final String[] LEGAL_DESCRIPTION_MARKERS = {"ADDN", "ADDITION", "REPLAT", "SUBD", "SUBDIVISION"};
	
	public static final String[] WORDS_TO_IGNORE_IN_SUBDIVISION_NAME = {"FAYETTEVILLE", "FINAL"};
	
	public static final String[] CITIES = {"ELKINS", "ELM SPRINGS", "FARMINGTON", "FAYETTEVILLE",
										   "GOSHEN", "GREENLAND", "JOHNSON", "LINCOLN",
										   "PRAIRIE GROVE", "SPRINGDALE", "TONTITOWN", "WEST FORK",
										   "WINSLOW",
										   "BENTONVILLE"};
	
	public static final String[] CITIES_ABBREVIATIONS = {"FAY", "SPG"};
	
	public static final String[] LEGAL_DESCRIPTION_EXCEPTIONS = {"FIRST NATIONAL BANK", "PUBLIC"};
	
	public static final String[] COMPANY_NAMES = {"SIMMONS FIRST", "FAST TRAX"};

	public static String cleanName(String name) {
		
		name = name.replaceAll("(-|\\b)DEF\\b", "");
		name = name.replaceAll("(-|\\b)PLF\\b", "");
		name = name.replaceAll("(-|\\b)INTERVENOR\\b", "");
		name = name.replaceAll("\\bMINOR\\s*$", "");
		name = name.replaceAll("\\bDR\\s*$", "");
		name = name.replaceAll("\\bDBA\\s*$", "");
		name = name.replaceAll("\\(\\s*CO[\\s-]?TRUSTEE\\s*\\)\\s*$", " TRUSTEE");
		name = name.replaceAll("^\\s*-", "");
		name = name.replaceAll("-\\s*$", "");
		name = name.replaceAll("\\s*-\\s*,", ",");
		name = name.replaceAll("(?is)\\bEX\\s+PARTE\\b", "");
		name = name.replaceAll("(?is)^-+$", "");
		
		//SMITH, ALLISON D (FORMERLY GLASSCOCK) -> ALLISON D SMITH and ALLISON D GLASSCOCK 
		Matcher matcher1 = Pattern.compile("(?is)(.+?,)(.+?)\\(\\s*FORMERLY\\s+(.+?)\\)\\s*$").matcher(name);
		if (matcher1.matches()) {
			name = matcher1.group(1) + matcher1.group(2) + " & " + matcher1.group(3) + ", " + matcher1.group(2); 
		}
		
		//FROST, H G (JACK) JR -> H G FROST and JACK FROST
		Matcher matcher2 = Pattern.compile("(?is)(.+?,)(.+?)\\((.+?)\\)(.*?)$").matcher(name);
		if (matcher2.matches()) {
			String group4 = "";
			if (matcher2.groupCount()==4) {
				group4 = matcher2.group(4);
			}
			boolean alternateFirst = true;
			String[] split = matcher2.group(3).split("\\s+");
			for (int i=0;i<split.length;i++) {
				//if not first name and not initial -> it isn't an alternate first name
				if (!FirstNameUtils.isFirstName(split[i]) && !split[i].matches("[A-Z]")) {
					alternateFirst = false;
					break;
				}
			}
			if (alternateFirst) {
				name = matcher2.group(1) + matcher2.group(2) + group4 + 
					" & " + matcher2.group(1) + " " + matcher2.group(3);
			}
		}
		
		return name.trim();
	}
	
	@SuppressWarnings("rawtypes")
	public static void parseName(String rawNames, ArrayList<List> list, ResultMap resultMap) {
		
		String names[] = {"", "", "", "", "", ""};
		String[] suffixes, type, otherType;
		
		String split[] = rawNames.split("(?is)<br>");
		boolean isLegalDescription = true;
		for (int i=0;i<split.length;i++) {
			if (split[i].trim().length()!=0 && !isLegalDescription(split[i])) {
				isLegalDescription = false;
			}
		}
		for (int i=0;i<split.length;i++) {
			
			String name = split[i];
			if (isLegalDescription) {
				parseLegal(name, resultMap);
			} else {
				name = cleanName(name);
				if (!StringUtils.isEmpty(name)) {
					boolean threeLastNames = false;
					String thirdLastName = "";
					//HENSON CHATFIELD LOVEJOY, STACIE S
					//but not STATE OF ARKANSAS, CSEU
					Matcher matcher = Pattern.compile("^([\\w'-]+ \\w+)( \\w+),(.*)").matcher(name);
					if (matcher.find() && !NameUtils.isCompany(matcher.group(1)+matcher.group(2))) {
						thirdLastName = matcher.group(2);
						name = matcher.group(1) + "," + matcher.group(3);
						threeLastNames = true;
					}
					//CIT GROUP/SALES FINANCING - a single company
					if (NameUtils.isCompany(name)) {
						name = name.replaceAll("/", "@@@");
					}
					boolean firstNameisCompany = false;
					for (int j=0;j<COMPANY_NAMES.length;j++) {
						if (name.toUpperCase().trim().equals(COMPANY_NAMES[j])) {
							names[2] = COMPANY_NAMES[j];
							names[0] = names[1] = names[3] = names[4] = names[5];
							firstNameisCompany = true;
							break;
						}
					}
					if (!firstNameisCompany) {
						names = StringFormats.parseNameNashville(name, true);
					}
					if (NameUtils.isCompany(names[2])) {
						names[2] = names[2].replaceAll("@@@", "/");
					}
					if (threeLastNames) {
						names[2] += thirdLastName;
					}
					type = GenericFunctions.extractAllNamesType(names);
					otherType = GenericFunctions.extractAllNamesOtherType(names);
					suffixes = GenericFunctions.extractNameSuffixes(names);
					if (!firstNameisCompany) {
						firstNameisCompany = NameUtils.isCompany(names[2]);
					}
					GenericFunctions.addOwnerNames(split[i], names, suffixes[0],
							suffixes[1], type, otherType,
							firstNameisCompany,
							NameUtils.isCompany(names[5]), list);
				}
			}
		}
	}
	
	@SuppressWarnings("rawtypes")
	public static void parseNames(ResultMap resultMap) {

		ArrayList<List> grantor = new ArrayList<List>();
		String tmpPartyGtor = (String)resultMap.get("tmpGrantor");
		
		if (StringUtils.isNotEmpty(tmpPartyGtor)){
			
			parseName(tmpPartyGtor, grantor, resultMap);
			try {
				resultMap.put("GrantorSet", GenericFunctions.storeOwnerInSet(grantor, true));
			} catch (Exception e) {
				e.printStackTrace();
			}
			
		}
		resultMap.remove("tmpGrantor");
		resultMap.put(SaleDataSetKey.GRANTOR.getKeyName(), concatenatedNames(grantor));
		
		ArrayList<List> grantee = new ArrayList<List>();
		String tmpPartyGtee = (String)resultMap.get("tmpGrantee");
		
		if (StringUtils.isNotEmpty(tmpPartyGtee)){
			
			parseName(tmpPartyGtee, grantee, resultMap);
			try {
				resultMap.put("GranteeSet", GenericFunctions.storeOwnerInSet(grantee, true));
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		resultMap.remove("tmpGrantee");
		resultMap.put(SaleDataSetKey.GRANTEE.getKeyName(), concatenatedNames(grantee));
	}

	@SuppressWarnings("rawtypes")
	public static String concatenatedNames(ArrayList<List> nameList) {
		String result = "";
		
		StringBuilder result_sb = new StringBuilder();
		for (List list: nameList) {
			if (list.size()>3) {
				result_sb.append(list.get(3)).append(", ").append(list.get(1)).append(" ").append(list.get(2)).append(" / ");
			}
			if (list.size()>6) {
				result_sb.append(list.get(6)).append(", ").append(list.get(4)).append(" ").append(list.get(5)).append(" / ");
			}
		}
		result = result_sb.toString().replaceAll("/\\s*,\\s*/", " / ").replaceAll(",\\s*/", " /").
			replaceAll("\\s{2,}", " ").replaceAll("/\\s*$", "").trim();
		return result;
	}
	
	/** 
	 * returns true if a value that appears at Grantor/Grantee is in fact a legal description
	 * */
	public static boolean isLegalDescription(String name) {
		
		name = name.trim();
		
		if (name.length()==0) {
			return false;
		}
		
		for (int i=0;i<LEGAL_DESCRIPTION_EXCEPTIONS.length;i++) {
			if (name.matches("(?is)" + LEGAL_DESCRIPTION_EXCEPTIONS[i])) {
				return false;
			}
		}
		
		//WILSONS 1ST ADDN FAY
		//WILSONS SECOND ADDITION
		//TOMLYN VALLEY VIEW ORIGINAL (REPLAT)
		//BROADVIEW SUBD FAYETTEVILLE
		//FINCHERS SUBDIVISION
		String[] split = name.split("\\s+");
		for (int i=0;i<split.length;i++) {
			String word = split[i];
			word = word.replaceFirst("^\\(", "");
			word = word.replaceFirst("\\)$", "");
			word = word.trim();
			for (int j=0;j<LEGAL_DESCRIPTION_MARKERS.length;j++) {
				if (word.equalsIgnoreCase(LEGAL_DESCRIPTION_MARKERS[j])) {
					return true;
				}
			}
		}
		
		//MILDRED LEE ESTATES PH II
		//HYLAND PARK PH 5 FAYETTEVILLE
		Matcher matcher = Pattern.compile("(?is)\\bPH\\s*[\\dMDCLXVI]+\\b").matcher(name);
		if (matcher.find()) {
			return true;
		}
		
		//WILSON-WILSON SD FAY
		if (name.matches("(?is).*?\\bSD(\\s+FAY(ETTEVILE)?)?$")) {
			return true;
		}
		
		//CARLON CENTER FAYETTEVILLE
		//CLAY YOE
		//HAMESTRING SOUTH
		//TIMBER CREST FINAL
		for (int i=0;i<WORDS_TO_IGNORE_IN_SUBDIVISION_NAME.length;i++) {
			name = name.replaceAll("(?is)\\b" + WORDS_TO_IGNORE_IN_SUBDIVISION_NAME[i] + "\\b" , "");
		}
		name = name.replaceAll("\\s{2,}", " ");
		name = name.trim();
		name = name.toUpperCase();
		List<String> subdivisionNameList = ro.cst.tsearch.servers.types.ARWashingtonRO.getSubdivisionNameList();
		for (int i=0;i<subdivisionNameList.size();i++) {
			if (subdivisionNameList.get(i).contains(name)) {
				return true;
			}
		}
		
		return false;
	}
	
	public static String sortValues(String s) {
		StringBuilder res = new StringBuilder();
		String[] split = s.split("\\s+");
		List<String> digits = new ArrayList<String>();
		List<String> nondigits = new ArrayList<String>();
		for (int i=0;i<split.length;i++) {
			if (split[i].matches("\\d+(-\\d+)?")) {
				digits.add(split[i]);
			} else {
				nondigits.add(split[i]);
			}
		}
		Collections.sort(nondigits);
		for (String el: digits) {
			res.append(el).append(" ");
		}
		for (String el: nondigits) {
			res.append(el).append(" ");
		}
		
		return res.toString().trim();
	}
	
	public static void parseLegal(String forcedLegalDescription, ResultMap resultMap) {
		
		String legalDescription = forcedLegalDescription;	//legal description from Grantor/Grantee column
		if (StringUtils.isEmpty(legalDescription)) {
			legalDescription = (String) resultMap.get(PropertyIdentificationSetKey.LEGAL_DESCRIPTION_ON_SERVER.getKeyName());
		}
		if(StringUtils.isEmpty(legalDescription)) {
			return;
		}
		
		legalDescription = legalDescription.replaceAll("(?is)<br>", "   ");
		
		Matcher matcher1 = Pattern.compile("\\$([\\d,\\.]+)").matcher(legalDescription);
		if (matcher1.find()) {
			resultMap.put(SaleDataSetKey.CONSIDERATION_AMOUNT.getKeyName(), matcher1.group(1).replaceAll(",", ""));  
		}
		
		legalDescription = legalDescription.replaceAll("(?is)\\b(LO?TS?(?:\\s+|:)(?:[\\w,]+))\\s*-\\s*([\\w,]+)\\b", "$1-$2");
		legalDescription = legalDescription.replaceAll("(?is)\\b(LO?TS?(?:\\s+|:)(?:[\\w,]+))\\s*(?:AND|&)\\s*([\\w,]+)\\b", "$1&$2");
		String lotExpr = "(?is)\\bLO?TS?(?:\\s+|:)([\\w-,&]+)\\b";
		List<String> lot = RegExUtils.getMatches(lotExpr, legalDescription, 1);
		legalDescription = legalDescription.replaceAll(lotExpr, " LOT ");
		StringBuilder subdivisionLot = new StringBuilder();
		String alreadyLot = (String)resultMap.get(PropertyIdentificationSetKey.SUBDIVISION_LOT_NUMBER.getKeyName());
		if (!StringUtils.isEmpty(alreadyLot)) {
			subdivisionLot.append(alreadyLot).append(" ");
		}
		for (int i=0; i<lot.size(); i++) {
			subdivisionLot.append(lot.get(i).replaceAll("[,&]", " ")).append(" ");
		} 
		String subdivisionLotString = subdivisionLot.toString().trim();
		if (subdivisionLotString.length() != 0) {
			subdivisionLotString = LegalDescription.cleanValues(subdivisionLotString, false, true);
			subdivisionLotString = sortValues(subdivisionLotString);
			resultMap.put(PropertyIdentificationSetKey.SUBDIVISION_LOT_NUMBER.getKeyName(), subdivisionLotString);
		}
		
		legalDescription = legalDescription.replaceAll("(?is)\\b(BL(?:OC)?KS?(?:\\s+|:)(?:[\\w,]+))\\s*-\\s*([\\w,]+)\\b", "$1-$2");
		legalDescription = legalDescription.replaceAll("(?is)\\b(BL(?:OC)?KS?(?:\\s+|:)(?:[\\w,]+))\\s*(AND|&)\\s*BL(?:OC)?KS?\\b", "$1$2");
		legalDescription = legalDescription.replaceAll("(?is)\\b(BL(?:OC)?KS?(?:\\s+|:)(?:[\\w,]+))\\s*(?:AND|&)\\s*([\\w,]+)\\b", "$1&$2");
		String blockExpr = "(?is)\\bBL(?:OC)?KS?(?:\\s+|:)([\\w-,&]+)\\b";
		List<String> block = RegExUtils.getMatches(blockExpr, legalDescription, 1);
		legalDescription = legalDescription.replaceAll(blockExpr, " BLOCK ");
		StringBuilder subdivisionBlock = new StringBuilder();
		String alreadyBlock = (String)resultMap.get(PropertyIdentificationSetKey.SUBDIVISION_BLOCK.getKeyName());
		if (!StringUtils.isEmpty(alreadyBlock)) {
			subdivisionBlock.append(alreadyBlock).append(" ");
		}
		for (int i=0; i<block.size(); i++) {
			subdivisionBlock.append(block.get(i).replaceAll("[,&]", " ")).append(" ");
		} 
		String subdivisionBlockString = subdivisionBlock.toString().trim();
		if (subdivisionBlockString.length() != 0) {
			subdivisionBlockString = LegalDescription.cleanValues(subdivisionBlockString, false, true);
			subdivisionBlockString = sortValues(subdivisionBlockString);
			resultMap.put(PropertyIdentificationSetKey.SUBDIVISION_BLOCK.getKeyName(), subdivisionBlockString);
		}
		
		legalDescription = legalDescription.replaceAll("(?is)\\bAPT\\s+(UNITS?)", "$1");
		legalDescription = legalDescription.replaceAll("(?is)\\b((?:APT|UNITS?)\\s+[\\w-]+)\\s+(#[\\w-]+)\\b", "$1$2");
		String unitExpr = "(?is)\\b(?:APT|UNITS?)(?:\\s+|:)([\\w-,&@#]+)\\b";
		List<String> unit = RegExUtils.getMatches(unitExpr, legalDescription, 1);
		legalDescription = legalDescription.replaceAll(unitExpr, " UNIT ");
		StringBuilder subdivisionUnit = new StringBuilder();
		String alreadyUnit = (String)resultMap.get(PropertyIdentificationSetKey.SUBDIVISION_UNIT.getKeyName());
		if (!StringUtils.isEmpty(alreadyUnit)) {
			subdivisionUnit.append(alreadyUnit).append(" ");
		}
		for (int i=0; i<unit.size(); i++) {
			subdivisionUnit.append(unit.get(i).replaceAll("[,&]", " ")).append(" ");
		} 
		String subdivisionUnitString = subdivisionUnit.toString().trim();
		if (subdivisionUnitString.length() != 0) {
			subdivisionUnitString = LegalDescription.cleanValues(subdivisionUnitString, false, true);
			subdivisionUnitString = sortValues(subdivisionUnitString);
			resultMap.put(PropertyIdentificationSetKey.SUBDIVISION_UNIT.getKeyName(), subdivisionUnitString);
		}
		
		String bldgExpr = "(?is)\\bBLDG\\s+([\\w]+)\\b";
		List<String> bldg = RegExUtils.getMatches(bldgExpr, legalDescription, 1);
		legalDescription = legalDescription.replaceAll(bldgExpr, " BLGD ");
		StringBuilder subdivisionBldg = new StringBuilder();
		String alreadyBldg = (String)resultMap.get(PropertyIdentificationSetKey.SUBDIVISION_BLDG.getKeyName());
		if (!StringUtils.isEmpty(alreadyBldg)) {
			subdivisionBldg.append(alreadyBldg).append(" ");
		}
		for (int i=0; i<bldg.size(); i++) {
			subdivisionBldg.append(bldg.get(i)).append(" ");
		} 
		String subdivisionBldgString = subdivisionBldg.toString().trim();
		if (subdivisionBldgString.length() != 0) {
			subdivisionBldgString = LegalDescription.cleanValues(subdivisionBldgString, false, true);
			subdivisionBldgString = sortValues(subdivisionBldgString);
			resultMap.put(PropertyIdentificationSetKey.SUBDIVISION_BLDG.getKeyName(), subdivisionBldgString);
		}
		
		String phaseExpr = "(?is)\\bPH(?:ASE)?\\s+([\\w-]+)\\b";
		List<String> phase = RegExUtils.getMatches(phaseExpr, legalDescription, 1);
		StringBuilder subdivisionPhase = new StringBuilder();
		String alreadyPhase = (String)resultMap.get(PropertyIdentificationSetKey.SUBDIVISION_PHASE.getKeyName());
		if (!StringUtils.isEmpty(alreadyPhase)) {
			subdivisionPhase.append(alreadyPhase).append(" ");
		}
		for (int i=0; i<phase.size(); i++) {
			subdivisionPhase.append(phase.get(i)).append(" ");
		} 
		String subdivisionPhaseString = subdivisionPhase.toString().trim();
		if (subdivisionPhaseString.length() != 0) {
			subdivisionPhaseString = LegalDescription.cleanValues(subdivisionPhaseString, false, true);
			subdivisionPhaseString = sortValues(subdivisionPhaseString);
			resultMap.put(PropertyIdentificationSetKey.SUBDIVISION_PHASE.getKeyName(), subdivisionPhaseString);
		}
		
		String tractExpr = "(?is)\\bTRACT\\s+([\\w-]+)\\b";
		List<String> tract = RegExUtils.getMatches(tractExpr, legalDescription, 1);
		legalDescription = legalDescription.replaceAll(tractExpr, " TRACT ");
		StringBuilder subdivisionTract = new StringBuilder();
		String alreadyTract = (String)resultMap.get(PropertyIdentificationSetKey.SUBDIVISION_TRACT.getKeyName());
		if (!StringUtils.isEmpty(alreadyTract)) {
			subdivisionTract.append(alreadyTract).append(" ");
		}
		for (int i=0; i<tract.size(); i++) {
			subdivisionTract.append(tract.get(i)).append(" ");
		} 
		String subdivisionTractString = subdivisionTract.toString().trim();
		if (subdivisionTractString.length() != 0) {
			subdivisionTractString = LegalDescription.cleanValues(subdivisionTractString, false, true);
			subdivisionTractString = sortValues(subdivisionTractString);
			resultMap.put(PropertyIdentificationSetKey.SUBDIVISION_TRACT.getKeyName(), subdivisionTractString);
		}
		
		String sectionExpr = "(?is)\\bSECTION\\s+([\\d]+)\\b";
		String section = RegExUtils.getFirstMatch(sectionExpr, legalDescription, 1);
		if (!StringUtils.isEmpty(section)) {
			resultMap.put(PropertyIdentificationSetKey.SECTION.getKeyName(), section);
		}
		
		String strExpr = "(?is)\\bSection:(\\d+)\\s+Township:(\\d+)\\s+Range:(\\d+)\\b";
		Matcher matcher2 = Pattern.compile(strExpr).matcher(legalDescription);
		if (matcher2.find()) {
			legalDescription = legalDescription.replaceFirst(strExpr, " STR ");
			resultMap.put(PropertyIdentificationSetKey.SUBDIVISION_SECTION.getKeyName(), matcher2.group(1));
			resultMap.put(PropertyIdentificationSetKey.SUBDIVISION_TOWNSHIP.getKeyName(), matcher2.group(2));
			resultMap.put(PropertyIdentificationSetKey.SUBDIVISION_RANGE.getKeyName(), matcher2.group(3));
		} else {
			strExpr = "(?is)(.)(\\d+)-(\\d+)-(\\d+)(.)";
			boolean found = false;
			Matcher matcher3 = Pattern.compile(strExpr).matcher(" " + legalDescription + " ");
			while (!found && matcher3.find() && !"(".equals(matcher3.group(1)) && !")".equals(matcher3.group(5))) {
				found = true;
				resultMap.put(PropertyIdentificationSetKey.SUBDIVISION_SECTION.getKeyName(), matcher3.group(2));
				resultMap.put(PropertyIdentificationSetKey.SUBDIVISION_TOWNSHIP.getKeyName(), matcher3.group(3));
				resultMap.put(PropertyIdentificationSetKey.SUBDIVISION_RANGE.getKeyName(), matcher3.group(4));
			}
		}
		
		ResultTable crossRefTable = new ResultTable();
		@SuppressWarnings("rawtypes")
		List<List> crossRefBody = new ArrayList<List>();
		List<String> list;
		String refExpr = "(?is)\\((L?\\d+)-([\\d-,]+(?:-[\\d-,]+)?)\\)";
		Matcher matcher3 = Pattern.compile(refExpr).matcher(legalDescription);
		while (matcher3.find()) {
			String part1 = matcher3.group(1);
			String part2 = matcher3.group(2);
			String[] part2Parts = part2.split(",");
			for (int i=0;i<part2Parts.length;i++) {
				part2Parts[i] = part2Parts[i].replaceFirst("^0+", "");
				part2Parts[i] = part2Parts[i].replaceFirst("-.*", "");	//(92-316-317) -> (92-316)
				list = new ArrayList<String>();
				if (part1.toUpperCase().startsWith("L") || isYear(part1)) {	//instrument number
					list.add("");
					list.add("");
					list.add(part1 + "-" + part2Parts[i]);
				} else {				//book-page
					list.add(part1);
					list.add(part2Parts[i]);
					list.add("");
				}
				crossRefBody.add(list);
			}
		}
		legalDescription = legalDescription.replaceAll(refExpr, " REFERENCE ");
		String[] crossRefHeader = {CrossRefSetKey.BOOK.getShortKeyName(), CrossRefSetKey.PAGE.getShortKeyName(), 
				CrossRefSetKey.INSTRUMENT_NUMBER.getShortKeyName()};
		crossRefTable = GenericFunctions2.createResultTable(crossRefBody, crossRefHeader);
		if (crossRefTable != null && crossRefTable.getBody().length!=0){
			resultMap.put("CrossRefSet", crossRefTable);
		}
		
		//subdivision name at the beginning of the legal description
		String subdivisionExpr1 = "(?i)(.+?)\\b(?:BL(?:OC)?KS?\\b|LO?TS?\\b|UNIT\\b|TRACT\\b|\\s{3})";
		String subdivisionName = "";
		if (StringUtils.isEmpty(forcedLegalDescription)) {
			legalDescription = legalDescription.trim();
		} else {
			legalDescription += " BLOCK";
		}
		Matcher matcher4 = Pattern.compile(subdivisionExpr1).matcher(legalDescription);
		boolean isCondo = false;
		if (matcher4.find()) {
			subdivisionName = matcher4.group(1);
			subdivisionName = cleanSubdivisionName(subdivisionName);
			subdivisionName = correctSubdivisionName(subdivisionName);
			if (subdivisionName.contains("@@@ISCONDO@@@")) {
				isCondo = true;
				subdivisionName = subdivisionName.replaceAll("@@@ISCONDO@@@", "");
			}
		}
		String alreadySubdivisionName = (String)resultMap.get(PropertyIdentificationSetKey.SUBDIVISION_NAME.getKeyName());
		if (StringUtils.isEmpty(alreadySubdivisionName) && !StringUtils.isEmpty(subdivisionName)) {
			resultMap.put(PropertyIdentificationSetKey.SUBDIVISION_NAME.getKeyName(), subdivisionName);
			if (isCondo) {
				resultMap.put(PropertyIdentificationSetKey.SUBDIVISION_COND.getKeyName(), subdivisionName);
			}
		} else {
			//subdivision name at the end of the legal description
			String subdivisionExpr2 = "(?i).*(?:LOT|BLOCK|TRACT)(.+)";
			Matcher matcher5 = Pattern.compile(subdivisionExpr2).matcher(legalDescription);
			if (matcher5.find()) {
				subdivisionName = matcher5.group(1);
				subdivisionName = cleanSubdivisionName(subdivisionName);
				subdivisionName = correctSubdivisionName(subdivisionName);
				if (subdivisionName.contains("@@@ISCONDO@@@")) {
					isCondo = true;
					subdivisionName = subdivisionName.replaceAll("@@@ISCONDO@@@", "");
				}
				if (StringUtils.isEmpty(alreadySubdivisionName) && !StringUtils.isEmpty(subdivisionName)) {
					resultMap.put(PropertyIdentificationSetKey.SUBDIVISION_NAME.getKeyName(), subdivisionName);
					if (isCondo) {
						resultMap.put(PropertyIdentificationSetKey.SUBDIVISION_COND.getKeyName(), subdivisionName);
					}
				}
			}
		}
		
	}
	
	public static String cleanSubdivisionName(String subdivisionName) {
		
		if (StringUtils.isEmpty(subdivisionName.trim())) {
			return "";
		}
		
		subdivisionName = subdivisionName.toUpperCase();
		
		subdivisionName = subdivisionName.replaceFirst("\\bPT\\b.*", "");
		
		subdivisionName = subdivisionName.replaceAll("\\bLOT\\b", "");
		subdivisionName = subdivisionName.replaceAll("\\bBLOCK\\b", "");
		subdivisionName = subdivisionName.replaceAll("\\bUNIT\\b", "");
		subdivisionName = subdivisionName.replaceAll("\\bBLDG\\b", "");
		subdivisionName = subdivisionName.replaceAll("\\bTRACT\\b", "");
		subdivisionName = subdivisionName.replaceAll("\\bSTR\\b", "");
		subdivisionName = subdivisionName.replaceAll("\\bREFERENCE\\b", "");
		subdivisionName = subdivisionName.replaceAll("\\$[\\d,\\.]+", "");
		subdivisionName = subdivisionName.trim();
		
		for (int i=0;i<CITIES.length;i++) {
			if (!subdivisionName.matches("ORIG(INAL)?\\b.*") && !subdivisionName.matches("TOWN\\b.*")) {
				subdivisionName = subdivisionName.replaceAll("\\b" + CITIES[i] + "\\b\\s*$", "");
			}
		}
		for (int i=0;i<CITIES_ABBREVIATIONS.length;i++) {
			if (!subdivisionName.matches("ORIG(INAL)?\\b.*") && !subdivisionName.matches("TOWN\\b.*")) {
				subdivisionName = subdivisionName.replaceAll("\\b" + CITIES_ABBREVIATIONS[i] + "(\\s+REPLAT)?" + "\\b\\s*$", "");
			}
		}
		
		subdivisionName = subdivisionName.replaceAll("\\bNO\\s+LEGAL\\b", "");
		subdivisionName = subdivisionName.replaceAll("^\\s*ORG\\s*$", "");
		subdivisionName = subdivisionName.replaceAll("\\(NO BOOK & PG\\)", "");
		subdivisionName = subdivisionName.replaceAll("\\b\\d{2,}[A-Z]{2,}\\d+\\b", "");
		
		subdivisionName = subdivisionName.replaceAll("\\s{2,}", " ");
		subdivisionName = subdivisionName.trim();
		
		if (subdivisionName.length()==0) {
			return "";
		}
		
		return subdivisionName;
	}
	
	//returns true if is condominium
	public static String correctSubdivisionName(String subdivisionName) {
		
		String backupSubdivisionName = subdivisionName;
		
		boolean containsCondo = subdivisionName.matches("(?is).*\\bCONDO.*");
		List<String> possibleCorrection = new ArrayList<String>();
		
		possibleCorrection.add(subdivisionName);
		subdivisionName = backupSubdivisionName.replaceAll("\\bREGIME\\b\\s*$", "");
		if (!possibleCorrection.contains(subdivisionName)) {
			possibleCorrection.add(subdivisionName);
		}
		subdivisionName = backupSubdivisionName.replaceAll("^THE\\s+", "");
		if (!possibleCorrection.contains(subdivisionName)) {
			possibleCorrection.add(subdivisionName);
		}
		subdivisionName = backupSubdivisionName.replaceFirst("\\bSU?B?D\\b", "SUBDIVISION");
		if (!possibleCorrection.contains(subdivisionName)) {
			possibleCorrection.add(subdivisionName);
		}
		subdivisionName = backupSubdivisionName.replaceFirst("\\bADDN?\\b", "ADDITION");
		if (!possibleCorrection.contains(subdivisionName)) {
			possibleCorrection.add(subdivisionName);
		}
		subdivisionName = backupSubdivisionName.replaceFirst("\\(FINAL\\)", "").replaceAll("\\s{2,}", " ") .trim();
		if (!possibleCorrection.contains(subdivisionName)) {
			possibleCorrection.add(subdivisionName);
		}
		subdivisionName = backupSubdivisionName + " ADDITION";
		if (!possibleCorrection.contains(subdivisionName)) {
			possibleCorrection.add(subdivisionName);
		}
		subdivisionName = backupSubdivisionName + " SUBDIVISION";
		if (!possibleCorrection.contains(subdivisionName)) {
			possibleCorrection.add(subdivisionName);
		}
		//WILSON PLACE REPLAT -> WILSON PLACE ADDN REPLAT
		subdivisionName = backupSubdivisionName.replaceFirst("(?is)(.+)\\s+(REPLAT)\\s*$", "$1 ADDN $2");
		if (!possibleCorrection.contains(subdivisionName)) {
			possibleCorrection.add(subdivisionName);
		}
		subdivisionName = backupSubdivisionName.replaceFirst("(?is)\\bREPLAT.*", "");
		if (!possibleCorrection.contains(subdivisionName)) {
			possibleCorrection.add(subdivisionName);
		}
		subdivisionName = backupSubdivisionName.replaceFirst("(?is)\\b,?\\s*\\bETC.*", "");
		if (!possibleCorrection.contains(subdivisionName)) {
			possibleCorrection.add(subdivisionName);
		}
		//WILSONS 1ST ADDN -> WILSONS FIRST ADDITION
		subdivisionName = backupSubdivisionName.replaceFirst("(?is)\\b1ST\\b", "FIRST");
		if (!possibleCorrection.contains(subdivisionName)) {
			possibleCorrection.add(subdivisionName);
		}
		subdivisionName = subdivisionName.replaceFirst("\\bADDN\\b", "ADDITION");
		if (!possibleCorrection.contains(subdivisionName)) {
			possibleCorrection.add(subdivisionName);
		}
		//TOMLYN VALLEY VIEW ADDITION (SECTION 2) (FINAL) -> TOMLYN VALLEY VIEW ADDITION SECTION 2 FINAL
		subdivisionName = backupSubdivisionName.replaceAll("[\\(\\)]", "");
		if (!possibleCorrection.contains(subdivisionName)) {
			possibleCorrection.add(subdivisionName);
		}
		//TOMLYN VALLEY VIEW ADDITION (SECTION 2) (FINAL) -> TOMLYN VALLEY VIEW ADDITION SECTION 2 FINAL
		subdivisionName = backupSubdivisionName.replaceAll("[\\(\\)]", "");
		if (!possibleCorrection.contains(subdivisionName)) {
			possibleCorrection.add(subdivisionName);
		}
		subdivisionName = backupSubdivisionName.replaceAll("(?is)FINAL", "ADDITION");
		if (!possibleCorrection.contains(subdivisionName)) {
			possibleCorrection.add(subdivisionName);
		}
		subdivisionName = backupSubdivisionName.replaceAll("\\(.*$", "");
		if (!possibleCorrection.contains(subdivisionName)) {
			possibleCorrection.add(subdivisionName);
		}
		
		int index = backupSubdivisionName.lastIndexOf(" ");
		if (index>-1) {
			String p1 = backupSubdivisionName.substring(0, index).trim();
			String p2 = backupSubdivisionName.substring(index+1).trim();
			List<String> tokens = Arrays.asList(LEGAL_DESCRIPTION_MARKERS);
			List<String> otherTokens = new ArrayList<String>();
			otherTokens.add("ADD");
			otherTokens.add("SD");
			if (otherTokens.contains(p2) || tokens.contains(p2)) { 
				//INDUSTRIAL PARK WEST ADD -> INDUSTRIAL PARK WEST
				if (!possibleCorrection.contains(p1)) {
					possibleCorrection.add(p1);
				}
				//W.C. BRALY ADD -> W.C. BRALY's ADDITION
				p1 = p1 + "'S" + " " + p2; 
				if (!possibleCorrection.contains(p1)) {
					possibleCorrection.add(p1);
				}
				subdivisionName = p1.replaceFirst("\\bSD\\b", "SUBDIVISION");
				if (!possibleCorrection.contains(subdivisionName)) {
					possibleCorrection.add(subdivisionName);
				}
				subdivisionName = p1.replaceFirst("\\bADD\\b", "ADDITION");
				if (!possibleCorrection.contains(subdivisionName)) {
					possibleCorrection.add(subdivisionName);
				}
			}
			p1 = backupSubdivisionName.substring(0, index).trim();
			p2 = backupSubdivisionName.substring(index+1).trim();
			Matcher matcher1 = Pattern.compile("(?is)^(PH(?:ASE)?\\s+[\\w-]+)\\s+(.*)$").matcher(p1);
			if (matcher1.find()) {
				//PH I-A ARKANSHIRE SD -> ARKANSHIRE SUBDIVISION PH I-A
				p1 = matcher1.group(2) + " " + matcher1.group(1) + " " + p2;
				if (!possibleCorrection.contains(p1)) {
					possibleCorrection.add(p1);
				}
				subdivisionName = p1.replaceFirst("\\bSD\\b", "SUBDIVISION");
				if (!possibleCorrection.contains(subdivisionName)) {
					possibleCorrection.add(subdivisionName);
				}
				subdivisionName = p1.replaceFirst("\\bADD\\b", "ADDITION");
				if (!possibleCorrection.contains(subdivisionName)) {
					possibleCorrection.add(subdivisionName);
				}
			}
			Matcher matcher2 = Pattern.compile("(?is)^(FINAL)\\s+(.*)$").matcher(backupSubdivisionName);
			if (matcher2.find()) {
				//FINAL FAYETTEVILLE INDUSTRIAL PARK-WEST -> FAYETTEVILLE INDUSTRIAL PARK-WEST FINAL 
				p1 = matcher2.group(2) + " " + matcher2.group(1);
				if (!possibleCorrection.contains(p1)) {
					possibleCorrection.add(p1);
				}
				subdivisionName = p1.replaceFirst("\\bSD\\b", "SUBDIVISION");
				if (!possibleCorrection.contains(subdivisionName)) {
					possibleCorrection.add(subdivisionName);
				}
				subdivisionName = p1.replaceFirst("\\bADD\\b", "ADDITION");
				if (!possibleCorrection.contains(subdivisionName)) {
					possibleCorrection.add(subdivisionName);
				}
			}
		}
		
		for (String s: possibleCorrection) {
			subdivisionName = s.trim();
			if (containsCondo) {
				String result = ro.cst.tsearch.servers.types.ARWashingtonRO.getCondominiumCodeFromName(subdivisionName);
				if (!StringUtils.isEmpty(result)) {
					return subdivisionName + "@@@ISCONDO@@@";
				}
				result = ro.cst.tsearch.servers.types.ARWashingtonRO.getSubdivisionCodeFromName(subdivisionName);
				if (!StringUtils.isEmpty(result)) {
					return subdivisionName;
				}
			} else {
				String result = ro.cst.tsearch.servers.types.ARWashingtonRO.getSubdivisionCodeFromName(subdivisionName);
				if (!StringUtils.isEmpty(result)) {
					return subdivisionName;
				}
				result = ro.cst.tsearch.servers.types.ARWashingtonRO.getCondominiumCodeFromName(subdivisionName);
				if (!StringUtils.isEmpty(result)) {
					return subdivisionName + "@@@ISCONDO@@@";
				}
			}
		}
		
		subdivisionName = backupSubdivisionName;
		return subdivisionName;
	}
	
	/**
	 * a string is an year on RO if it appears in instrument number before the first dash and
	 * has 2 digits and it is >= last 2 digits of first year taken from 'Verified Dates'
	 * or
	 * has four digits and it is >= first year taken from 'Verified Dates' and <= current year 
	 * */
	public static boolean isYear(String s) {
		int year4Digits = ro.cst.tsearch.servers.types.ARWashingtonRO.getFromYear();
		if (s.matches("\\d{2}")) {
			int year = Integer.parseInt(s);
			int year2Digits = 0;
			if (year4Digits>=1900 && year4Digits<=1999) {
				year2Digits = year4Digits-1900;
			}
			if (year>=year2Digits) {
				return true;
			}
		} else if (s.matches("\\d{4}")){
			int year = Integer.parseInt(s);
			int currentYear = Calendar.getInstance().get(Calendar.YEAR);
			if (year>=year4Digits && year<=currentYear) {
				return true;
			}
		}
		return false;
	}
}
