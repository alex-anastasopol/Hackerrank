package ro.cst.tsearch.servers.functions;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import ro.cst.tsearch.extractor.legal.LegalDescription;
import ro.cst.tsearch.extractor.xml.GenericFunctions;
import ro.cst.tsearch.extractor.xml.GenericFunctions2;
import ro.cst.tsearch.extractor.xml.ResultMap;
import ro.cst.tsearch.extractor.xml.ResultTable;
import ro.cst.tsearch.extractor.xml.StringFormats;
import ro.cst.tsearch.search.name.NameUtils;
import ro.cst.tsearch.servers.response.CrossRefSet.CrossRefSetKey;
import ro.cst.tsearch.servers.response.PropertyIdentificationSet.PropertyIdentificationSetKey;
import ro.cst.tsearch.utils.RegExUtils;
import ro.cst.tsearch.utils.Roman;
import ro.cst.tsearch.utils.StringUtils;

public class FLMartinAO {
	
	public static String getSessionId(String response) {
		Matcher m = Pattern.compile("sid=([a-zA-Z0-9]+)").matcher(response);
		if (m.find()) {
			return m.group(1);
		}
		return "";
	}

	protected static String ctrim(String input) {
		return input.replace("&nbsp;", " ").replaceAll("\\s+", " ").trim();
	}
	
	public static void putLegal(ResultMap map, String legalDescription) {
		
		if(StringUtils.isEmpty(legalDescription)) {
			return;
		}
		
		legalDescription = ctrim(legalDescription);
		
		map.put(PropertyIdentificationSetKey.LEGAL_DESCRIPTION_ON_SERVER.getKeyName(), legalDescription);
		
		legalDescription = GenericFunctions.replaceNumbers(legalDescription);
		String[] exceptionTokens = { "I", "M", "C", "L", "D" };
		legalDescription = Roman.normalizeRomanNumbersExceptTokens(legalDescription, exceptionTokens); // convert roman numbers
		
		legalDescription = legalDescription.replaceAll("(?is)\\bLOT/UNIT\\b", "LOT");
		legalDescription = legalDescription.replaceAll("(?is)\\bBLK/BLDG\\b", "BLDG");
				
		String strexpr = "\\A(\\d{2})[- ](\\d{2})[- ](\\d{2})";
		Matcher strMatcher = Pattern.compile(strexpr).matcher(legalDescription);
		if (strMatcher.find()) {
			String section = strMatcher.group(1).replaceAll("\\A0+", "");
			String township = strMatcher.group(2).replaceAll("\\A0+", "");
			String range = strMatcher.group(3).replaceAll("\\A0+", "");
			map.put(PropertyIdentificationSetKey.SUBDIVISION_SECTION.getKeyName(), section);
			map.put(PropertyIdentificationSetKey.SUBDIVISION_TOWNSHIP.getKeyName(), township);
			map.put(PropertyIdentificationSetKey.SUBDIVISION_RANGE.getKeyName(), range);
			legalDescription = legalDescription.replaceAll(strexpr, "");
		} else {
			strMatcher = Pattern.compile("(?is)\\bSEC\\s+(\\d+)-(\\d+)-(\\d+)\\b").matcher(legalDescription);
			if (strMatcher.find()) {
				String section = strMatcher.group(1).replaceAll("\\A0+", "");
				String township = strMatcher.group(2).replaceAll("\\A0+", "");
				String range = strMatcher.group(3).replaceAll("\\A0+", "");
				map.put(PropertyIdentificationSetKey.SUBDIVISION_SECTION.getKeyName(), section);
				map.put(PropertyIdentificationSetKey.SUBDIVISION_TOWNSHIP.getKeyName(), township);
				map.put(PropertyIdentificationSetKey.SUBDIVISION_RANGE.getKeyName(), range);
				legalDescription = legalDescription.replaceFirst(strMatcher.group(0), " STR ");
			}	
		}
		
		List<String> sec = RegExUtils.getMatches("(?is)\\bSEC\\s+(\\d+)\\b", legalDescription, 1);
		StringBuilder sb = new StringBuilder();
		for (int i=0; i<sec.size(); i++) {
			sb.append(sec.get(i)).append(" ");
		}
		String section = sb.toString().trim();
		if (section.length() != 0) {
			section = LegalDescription.cleanValues(section, false, true);
			map.put(PropertyIdentificationSetKey.SECTION.getKeyName(), section);
		}
		
		String lotexpr1 = "(?i)\\bLOT\\s+([A-Z](?:\\s+\\d+)?)";
		List<String> lot = RegExUtils.getMatches(lotexpr1, legalDescription, 1);
		sb = new StringBuilder();
		for (int i=0;i<lot.size();i++) {
			sb.append(lot.get(i)).append(" ");
		}
		String lotexpr2 = "(?is)\\bLOTS?\\s((?:(?:\\d+(?:-?[A-Z])?(\\s*THRU\\s*\\d+)?)[\\s,&]*)+)";
		lot = RegExUtils.getMatches(lotexpr2, legalDescription.replaceAll("\\d+/\\d+TH", ""), 1);
		for (int i=0;i<lot.size();i++) {
			String currentLot = lot.get(i);
			currentLot = currentLot.replaceAll("[&,]", "");
			currentLot = currentLot.replaceAll("(?is)(\\d+)\\s*THRU\\s*(\\d+)", "$1-$2");
			sb.append(currentLot.trim()).append(" ");
		}
		String subdivisionLot = sb.toString().trim();
		if (subdivisionLot.length() != 0) {
			subdivisionLot = LegalDescription.cleanValues(subdivisionLot, false, true);
			map.put(PropertyIdentificationSetKey.SUBDIVISION_LOT_NUMBER.getKeyName(), subdivisionLot);
		}
		
		String phaseexpr = "(?is)\\bPH(?:ASE|S)?\\s+([\\w-]+(\\s*&\\s*[\\w-]+)?)\\b";
		List<String> phase = RegExUtils.getMatches(phaseexpr, legalDescription, 1);
		sb = new StringBuilder();
		for (int i=0;i<phase.size();i++) {
			sb.append(phase.get(i).replaceAll("&", " ")).append(" ");
		}
		String subdivisionPhase = sb.toString().trim();
		if (subdivisionPhase.length() != 0) {
			subdivisionPhase = LegalDescription.cleanValues(subdivisionPhase, false, true);
			map.put(PropertyIdentificationSetKey.SUBDIVISION_PHASE.getKeyName(), subdivisionPhase);
		}
		
		List<String> blk = RegExUtils.getMatches("(?is)\\bBLK\\s+(\\d+)\\b", legalDescription, 1);
		sb = new StringBuilder();
		for (int i=0; i<blk.size(); i++) {
			sb.append(blk.get(i)).append(" ");
		}
		String subdivisionBlock = sb.toString().trim();
		if (subdivisionBlock.length() != 0) {
			subdivisionBlock = LegalDescription.cleanValues(subdivisionBlock, false, true);
			map.put(PropertyIdentificationSetKey.SUBDIVISION_BLOCK.getKeyName(), subdivisionBlock);
		}
		
		String bldgexpr = "(?is)\\bB(?:UI)?LD(?:IN)?G\\s+\\\"?(\\w+)\\\"?\\b";
		List<String> bldg = RegExUtils.getMatches(bldgexpr, legalDescription, 1);
		sb = new StringBuilder();
		for (int i=0; i<bldg.size(); i++) {
			sb.append(bldg.get(i)).append(" ");
		}
		String subdivisionBldg = sb.toString().trim();
		if (subdivisionBldg.length() != 0) {
			subdivisionBldg = LegalDescription.cleanValues(subdivisionBldg, false, true);
			map.put(PropertyIdentificationSetKey.SUBDIVISION_BLDG.getKeyName(), subdivisionBldg);
		}
		
		String unitexpr = "(?is)\\b(?:APTS?|UNITS?)\\s+((?:(?:\\d+(?:-[A-Z])?)|[A-Z])(?:\\s*[-&]\\s*\\d+)?)\\b";
		List<String> unit = RegExUtils.getMatches(unitexpr, legalDescription, 1);
		sb = new StringBuilder();
		for (int i=0; i<unit.size(); i++) {
			sb.append(unit.get(i).replaceAll(" ", "").replaceAll("&", " ")).append(" ");
		} 
		String subdivisionUnit = sb.toString().trim();
		if (subdivisionUnit.length() != 0) {
			subdivisionUnit = LegalDescription.cleanValues(subdivisionUnit, false, true);
			map.put(PropertyIdentificationSetKey.SUBDIVISION_UNIT.getKeyName(), subdivisionUnit);
		}
		
		List<String> tract1 = RegExUtils.getMatches("(?is)\\bTR(?:ACTS?;?)? \\(?((?:(?:(?:[\\d-]+|[A-Z]+-\\d+)([ ]*THRU[ ]*(?:[\\d-]+|[A-Z]+-\\d+))?)[ ,&]*)+)\\b", legalDescription, 1);
		sb = new StringBuilder();
		for (int i=0; i<tract1.size(); i++) {
			String currentTract = tract1.get(i);
			currentTract = currentTract.replaceAll("\\d{4}\\s*$", "");
			currentTract = currentTract.replaceAll("[&,]", " ");
			currentTract = currentTract.replaceAll("- ", "");
			currentTract = currentTract.replaceAll(" -", "");
			Matcher ma = Pattern.compile("(?is)\\b([A-Z]+)-(\\d+)[ ]*THRU[ ]*\\1-(\\d+)\\b").matcher(currentTract);
			if (ma.find()) {
				String letters = ma.group(1);
				String digits1 = ma.group(2);
				String digits2 = ma.group(3);
				String digits = StringFormats.ReplaceIntervalWithEnumeration(digits1 + "-" + digits2);
				StringBuilder newsb = new StringBuilder();
				String split[] = digits.split(" ");
				for (int j=0;j<split.length;j++) {
					newsb.append(letters + "-" + split[j]).append(" ");
				}
				currentTract = newsb.toString();
			}
			currentTract = currentTract.replaceAll("(\\d+)[ ]*THRU[ ]*(\\d+)", "$1-$2");
			sb.append(currentTract).append(" ");
		}
		List<String> tract2 = RegExUtils.getMatches("(?is)\\bTR(?:ACTS?)? (?:-[ ]*)?\"?([A-Z]([ ]*,?[ ]*[A-Z])*[ ]*&[ ]*[A-Z])\"?\\b", legalDescription, 1);
		for (int i=0; i<tract2.size(); i++) {
			sb.append(tract2.get(i).replaceAll("[&,]", " ")).append(" ");
		}
		String subdivisionTract = sb.toString().trim();
		if (subdivisionTract.length() != 0) {
			subdivisionTract = LegalDescription.cleanValues(subdivisionTract, false, true);
			map.put(PropertyIdentificationSetKey.SUBDIVISION_TRACT.getKeyName(), subdivisionTract);
		}
		
		List<String> plat_book = RegExUtils.getMatches("(?is)\\bPB\\s+(\\d+)\\s+PG\\s+(\\d+)\\b", legalDescription, 1);
		List<String> plat_no = RegExUtils.getMatches("(?is)\\bPB\\s+(\\d+)\\s+PG\\s+(\\d+)\\b", legalDescription, 2);
		ResultTable platTable = new ResultTable();
		@SuppressWarnings("rawtypes")
		List<List> tablebodyPlat = new ArrayList<List>();
		for (int i=0; i<plat_book.size(); i++) {
			List<String> list;
			list = new ArrayList<String>();
			list.add(plat_book.get(i).replaceAll("\\A0+", ""));
			list.add(plat_no.get(i).replaceAll("\\A0+", ""));
			tablebodyPlat.add(list);
		}
		String[] headerPlat = {PropertyIdentificationSetKey.PLAT_BOOK.getShortKeyName(), PropertyIdentificationSetKey.PLAT_NO.getShortKeyName()};
		platTable = GenericFunctions2.createResultTable(tablebodyPlat, headerPlat);
		if (platTable != null && tablebodyPlat.size()!=0){
			map.put("PropertyIdentificationSet", platTable);
		}
		
		List<String> book = RegExUtils.getMatches("(?is)\\bOR\\s+(\\d+)/(\\d+)\\b", legalDescription, 1);
		List<String> page = RegExUtils.getMatches("(?is)\\bOR\\s+(\\d+)/(\\d+)\\b", legalDescription, 2);
		ResultTable crossRef = new ResultTable();
		@SuppressWarnings("rawtypes")
		List<List> tablebodyRef = new ArrayList<List>();
		for (int i=0; i<book.size(); i++) {
			List<String> list;
			list = new ArrayList<String>();
			list.add(book.get(i).replaceAll("\\A0+", ""));
			list.add(page.get(i).replaceAll("\\A0+", ""));
			tablebodyRef.add(list);
		}
		String[] headerRef = {CrossRefSetKey.BOOK.getShortKeyName(), CrossRefSetKey.PAGE.getShortKeyName()};
		crossRef = GenericFunctions2.createResultTable(tablebodyRef, headerRef);
		if (crossRef != null && tablebodyRef.size()!=0){
			map.put("CrossRefSet", crossRef);
		}
		
		Matcher subdivisionMatcher = Pattern.compile("(?i)(.+?)(?:,|R?P\\.?U\\.?D\\.?|LOT|BLK|PHASE|B(?:UI)?LD(?:IN)?G|PLAT|[A-Z0-9]{2}-\\d{2}|TRS|UNREC|TRACT|APT|PER|\\(|UNIT|SEC)")
				.matcher(legalDescription.replaceAll("^\\s*"+unitexpr, "").replaceAll("^\\s*"+bldgexpr, ""));
		if (subdivisionMatcher.find()) {
			String subdivision = subdivisionMatcher.group(1);
			if (subdivision.matches(lotexpr1 + "\\s+") || subdivision.matches(lotexpr2 + "\\s+")) {
				if (subdivisionMatcher.find()) {
					subdivision = subdivisionMatcher.group(1);
					subdivision = subdivision.replaceAll("^\\s*\\d+\\s*", "");
				}
			}
			subdivision = subdivision.replaceAll(lotexpr1, "");
			subdivision = subdivision.replaceAll(lotexpr2, "");
			subdivision = subdivision.replaceAll("(?is)\\b[EWNS]{1,2}\\s*\\d+/\\d+.*", "");
			subdivision = subdivision.replaceAll(unitexpr, "");
			subdivision = subdivision.replaceAll(phaseexpr, "");
			if (subdivision.matches("(?is).*\\bRETREAT.*") || subdivision.matches("(?is).*\\bREVISED.*")) {
				subdivision = "";
			}
			subdivision = subdivision.trim();
			if (subdivision.length()>0)
				map.put(PropertyIdentificationSetKey.SUBDIVISION_NAME.getKeyName(), subdivision);
				if (subdivision.matches("(?is).*\\bCOND.*")) {
					map.put(PropertyIdentificationSetKey.SUBDIVISION_COND.getKeyName(), subdivision);
				}
		}
		
	}

	public static void putOwnerNames(ResultMap map, String owner) {
		
		if (StringUtils.isEmpty(owner)) {
			return;
		}
		
		owner = owner.replace("&amp;", "&");
		map.put(PropertyIdentificationSetKey.NAME_ON_SERVER.getKeyName(), owner);
				
		owner = owner.replaceAll("\\(\\s*$", "");
		owner = owner.replaceAll("(?is)\\b(?:CO-|J)?(TRS?)\\b", "$1 ### ");
		owner = owner.replaceAll("(?is)\\((TRS? ### )\\)?", "$1");
		owner = owner.replaceAll("(?is)\\bHEIR-L/E\\b", "");
		owner = owner.replaceAll(" ### \\s*$", "");
		
		if (!NameUtils.isCompany(owner)) {
			
			owner = owner.replaceAll("\\(.+?\\)", "");
			owner = owner.replaceAll("(?is)\\bAND\\b", "&").trim();
			if (owner.indexOf(" ### ")==-1) {
				//SMITH CHARLES CAREY JR TRANOS MARY LOUISE
				owner = owner.replaceAll("(?is)^([\\w-]+\\s+[\\w-]+\\s+[\\w-]+\\s+(?:JR|SR|II|III|IV))\\s+([\\w-]+\\s+[\\w-]+(?:\\s+[\\w-]+)?)$", "$1 ### $2");
				//SMITH ERIC L JONES MARJORIE E
				owner = owner.replaceAll("(?is)^([\\w-]+\\s+[\\w-]+\\s+[A-Z])\\s+([\\w-]+\\s+[\\w-]+(?:\\s+[A-Z])?)$", "$1 ### $2");
				//SMITH DAVID JESSE SMITH VALERIE LYNN
				owner = owner.replaceAll("(?is)^([\\w-]+\\s+[\\w-]+\\s+[\\w-]+)\\s+([\\w-]+\\s+[\\w-]+\\s+[\\w-]+)$", "$1 ### $2");
				//SMITH PINKIE SMITH HILTON L
				owner = owner.replaceAll("(?is)^([\\w-]+\\s+[\\w-]+)\\s+([\\w-]+\\s+[\\w-]+\\s+[A-Z])$", "$1 ### $2");
				//SMITH MERLE RAY SMITH NANCY
				owner = owner.replaceAll("(?is)^([\\w-]+)\\s+([\\w-]+)\\s+([\\w-]+)\\s+(\\1\\s+[\\w-]+)$", "$1 $2 $3 ### $4");
			}
		}
		
		String owners[] = owner.split(" ### ");
		
		@SuppressWarnings("rawtypes")
		List<List> body = new ArrayList<List>();
		for (int i=0;i<owners.length;i++) {
			String ow = owners[i];
			if (!StringUtils.isEmpty(ow)) {
				String[] names = StringFormats.parseNameNashville(ow, true);
				String[] types = GenericFunctions.extractAllNamesType(names);
				String[] otherTypes = GenericFunctions.extractAllNamesOtherType(names);
				String[] suffixes = GenericFunctions.extractAllNamesSufixes(names);

				GenericFunctions.addOwnerNames(names, suffixes[0], suffixes[1],
						types, otherTypes, NameUtils.isCompany(names[2]),
						NameUtils.isCompany(names[5]), body);
			}
		}
		
		// Store owners.
		try {
			GenericFunctions.storeOwnerInPartyNames(map, body, true);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	public static void putAddress(ResultMap map, String fullAddress) {
		if (StringUtils.isEmpty(fullAddress)) {
			return;
		}
		
		fullAddress = fullAddress.replace("&amp;", "&");
		
		String[] addressAndCity = fullAddress.split(",");
		
		addressAndCity[0] = addressAndCity[0].replaceAll("(?is)\\bUNASSIGNED\\b", "").trim();
		map.put(PropertyIdentificationSetKey.ADDRESS_ON_SERVER.getKeyName(), addressAndCity[0]);
		addressAndCity[0] = addressAndCity[0].replaceAll("(?is)\\bUNIT\\s+", "#").trim();
		String streetName = StringFormats.StreetName(addressAndCity[0]);
		String streetNo = StringFormats.StreetNo(addressAndCity[0]);
		map.put(PropertyIdentificationSetKey.STREET_NO.getKeyName(), streetNo);
		map.put(PropertyIdentificationSetKey.STREET_NAME.getKeyName(), streetName);
		if (addressAndCity.length==2) {
			map.put(PropertyIdentificationSetKey.CITY.getKeyName(), addressAndCity[1].trim());
		}
	}
	
	
}
