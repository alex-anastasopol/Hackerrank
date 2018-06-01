package ro.cst.tsearch.servers.functions;

import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Vector;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang.StringUtils;
import org.htmlparser.filters.HasAttributeFilter;
import org.htmlparser.filters.TagNameFilter;
import org.htmlparser.tags.TableRow;
import org.htmlparser.tags.TableTag;
import org.htmlparser.util.NodeList;

import ro.cst.tsearch.extractor.legal.LegalDescription;
import ro.cst.tsearch.extractor.xml.GenericFunctions;
import ro.cst.tsearch.extractor.xml.ResultMap;
import ro.cst.tsearch.extractor.xml.ResultTable;
import ro.cst.tsearch.extractor.xml.StringFormats;
import ro.cst.tsearch.search.name.NameUtils;
import ro.cst.tsearch.servers.response.OtherInformationSet.OtherInformationSetKey;
import ro.cst.tsearch.servers.response.PropertyIdentificationSet.PropertyIdentificationSetKey;
import ro.cst.tsearch.servers.response.TaxHistorySet.TaxHistorySetKey;
import ro.cst.tsearch.servers.response.TaxInstallmentSet.TaxInstallmentSetKey;
import ro.cst.tsearch.utils.RegExUtils;
import ro.cst.tsearch.utils.Roman;

public class ILDeKalbTR {
	
	//these (plus Maple Park) are the cities and towns from IL DeKalb county
	private static final String[] CITY_NAMES = { "Cortland", "DeKalb", "Genoa", "Hinckley", "Kingston", "Kirkland", "Lee", 
			"Malta", "Sandwich", "Shabbona", "Somonauk", "Sycamore", "Waterman" };
	
	public static boolean stringInArray(String string, String[] array) {
		for (int i=0;i<array.length;i++) 
			if (string.equalsIgnoreCase(array[i])) return true;
		return false;
	}
	
	public static ResultMap parseIntermediaryRow(String row, long searchId) {
		
		ResultMap resultMap = new ResultMap();

		resultMap.put(OtherInformationSetKey.SRC_TYPE.getKeyName(), "TR");
		
		String cols[] = row.split("</?td>");
		resultMap.put(PropertyIdentificationSetKey.PARCEL_ID.getKeyName(),cols[1].replaceFirst("(?is)<a.*?>", ""));
		resultMap.put(PropertyIdentificationSetKey.NAME_ON_SERVER.getKeyName(),cols[3]);
		if (cols[5].length()!=0) resultMap.put(PropertyIdentificationSetKey.ADDRESS_ON_SERVER.getKeyName(),cols[5]);
		if (cols[7].length()!=0) resultMap.put(PropertyIdentificationSetKey.CITY.getKeyName(),cols[7]);

		parseNamesIntermediary(resultMap);
		parseAddress(resultMap);
		
		return resultMap;
	}

	public static void parseNamesIntermediary(ResultMap resultMap) {	//LMF format

		String owner = (String) resultMap.get(PropertyIdentificationSetKey.NAME_ON_SERVER.getKeyName());
		if (StringUtils.isEmpty(owner))
			   return;
		
		owner = owner.replaceAll("\\s*,\\s*&\\s*", " & ");
		owner = owner.replaceAll("\\s*&\\s*,\\s*", " & ");
		owner = owner.replaceFirst(",\\z", "");
		owner = owner.replaceFirst("(?is)TRUST\\s[\\d-]+\\z", "TRUST");
				
		@SuppressWarnings("rawtypes")
		List<List> body = new ArrayList<List>();
		String[] names = {"", "", "", "", "", ""};
		String[] suffixes = {"", ""} , type, otherType;
		names = StringFormats.parseNameNashville(owner, true);
		suffixes = GenericFunctions.extractNameSuffixes(names);
		type = GenericFunctions.extractAllNamesType(names);
		otherType = GenericFunctions.extractAllNamesOtherType(names);
		GenericFunctions.addOwnerNames(names, suffixes[0], suffixes[1], type, otherType,
				NameUtils.isCompany(names[2]), NameUtils.isCompany(names[5]), body);
		try {
			GenericFunctions.storeOwnerInPartyNames(resultMap, body, true);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	public static void parseNamesDetails(ResultMap resultMap) {		//FML format, separated by "<br>"

		String owner = (String) resultMap.get(PropertyIdentificationSetKey.NAME_ON_SERVER.getKeyName());
		if (StringUtils.isEmpty(owner))
			   return;
		
		String ow[] = owner.split("<br>");
		
		@SuppressWarnings("rawtypes")
		List<List> body = new ArrayList<List>();
		String[] names = {"", "", "", "", "", ""};
		String[] suffixes = {"", ""} , type, otherType;
		
		for (int i=0;i<ow.length;i++) 
			if (ow[i].length()!=0) {
				ow[i] = ow[i].replaceFirst("(?is)(TR(?:UST)?)\\s[\\d-]+\\z", "$1");
				names = StringFormats.parseNameFML(ow[i], new Vector<String>(), true, true);
				suffixes = GenericFunctions.extractNameSuffixes(names);
				type = GenericFunctions.extractAllNamesType(names);
				otherType = GenericFunctions.extractAllNamesOtherType(names);
				GenericFunctions.addOwnerNames(names, suffixes[0], suffixes[1], type, otherType,
						NameUtils.isCompany(names[2]), NameUtils.isCompany(names[5]), body);
			}
		try {
			GenericFunctions.storeOwnerInPartyNames(resultMap, body, true);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	public static void parseAddress(ResultMap resultMap) {
		
		boolean fakeNumber = false;
		
		String address = (String) resultMap.get(PropertyIdentificationSetKey.ADDRESS_ON_SERVER.getKeyName());
		if (address==null) return;
		if (StringUtils.isEmpty(address)) return;
		
		address = address.replaceAll("(?is)STATE RTE", "STATE RT");
		if (address.toLowerCase().contains("state rt") && !address.matches("\\d+.*")) {	
			address = "1 " + address;		//STATE RTE 64 -> put a fake number - otherwise 64 will be considered the street number
			fakeNumber = true;				//but 64 is part of the street name
		}
		
		String streetNoAddress = "";
		String city = "";
		
		//separate city from address
		int commaIndex = address.indexOf(",");
		if (commaIndex!=-1) {										//1224 YORKSHIRE DR SO, SYCAMORE
			streetNoAddress = address.substring(0, commaIndex);
			city = address.substring(commaIndex+1);
		}
		else {
			String[] parts = address.split("\\s");
			int length = parts.length;
			int index = length;
			if (length>=2 && "maple".equalsIgnoreCase(parts[length-2]) && "park".equalsIgnoreCase(parts[length-1])) index = length-2;
			else if (length>=1 && stringInArray(parts[length-1], CITY_NAMES)) index = length-1;
						
			for (int i=0;i<index;i++) if (parts[i].length()>0) streetNoAddress += parts[i] + " ";
			for (int i=index;i<length;i++) if (parts[i].length()>0) city += parts[i] + " ";
		}
		
		streetNoAddress = streetNoAddress.trim();
		city = city.trim();
		
		resultMap.put(PropertyIdentificationSetKey.CITY.getKeyName(), city);
		resultMap.put(PropertyIdentificationSetKey.STREET_NAME.getKeyName(), StringFormats.StreetName(streetNoAddress));
		if (!fakeNumber) resultMap.put(PropertyIdentificationSetKey.STREET_NO.getKeyName(), StringFormats.StreetNo(streetNoAddress));
		else resultMap.put(PropertyIdentificationSetKey.STREET_NO.getKeyName(), "");
	}
	
	public static void parseLegalSummary(ResultMap resultMap) {
		
		String legalDescription = (String) resultMap.get(PropertyIdentificationSetKey.LEGAL_DESCRIPTION_ON_SERVER.getKeyName());
		if(StringUtils.isEmpty(legalDescription)) {
			return;
		}
		
		legalDescription = GenericFunctions.replaceNumbers(legalDescription);
		String[] exceptionTokens = { "I", "M", "C", "L", "D" };
		legalDescription = Roman.normalizeRomanNumbersExceptTokens(legalDescription, exceptionTokens); // convert Roman numerals
		
		legalDescription += " - ";	//we need this to be sure all regular expressions match 
		
		String lotexpr1 = "(?is)LOT\\s([\\w-]+)[^/]";
		String lotexpr2 = "(?is)LOTS(((?:\\s*[\\w-]+\\s*[,&]+)+\\s*&?\\s*[\\w-]+)|(\\s\\w+-\\w+))";
		List<String> lot = RegExUtils.getMatches(lotexpr1, legalDescription, 1);
		String subdivisionLot = "";
		for (int i=0; i<lot.size(); i++) subdivisionLot += " " + lot.get(i);
		lot = RegExUtils.getMatches(lotexpr2, legalDescription.replaceAll("(?is)&\\sPT\\sOF\\sLOTS", ""), 1);
		for (int i=0; i<lot.size(); i++) subdivisionLot += " " + lot.get(i).replaceAll("[&,]", " ").trim();
		subdivisionLot = subdivisionLot.trim();
		if (subdivisionLot.length() != 0) {
			subdivisionLot = LegalDescription.cleanValues(subdivisionLot, false, true);
			resultMap.put(PropertyIdentificationSetKey.SUBDIVISION_LOT_NUMBER.getKeyName(), subdivisionLot);
		}
		
		//PT LOT 10 ý WESTFIELD MEADOWS
		legalDescription = legalDescription.replaceAll("ý", "-");			
		String[] tokens1 = {" -", "- "};
		String subdivisionName = LegalDescription.extractSubdivisionNameUntilToken(legalDescription, tokens1);
		String[] tokens2 = {"(?i)P\\.?U\\.?D\\.?.*", "(?i)\\s*-?\\s*PHASE.*", "(?i)\\b[NSEW]{1,2}\\s?-?\\d+.*", "(?i)PT", "(?i)(OF\\s)?LOT.*", 
				"(?i)\\s*-?\\s*UNIT\\s([\\w-]+)", "(?i)RESUB(DIVISION)?.*", "(?i)ASSESSORS?.*", "(?i)PART.*", "(?i)PER.*", "(?i)SEC.*" };
		subdivisionName = LegalDescription.extractSubdivisionNameByCleaning(subdivisionName, tokens2).trim();
		if (subdivisionName.equalsIgnoreCase("sub")) subdivisionName = "";
		//LOT 8 - RICHLAND TRAILS
		if (subdivisionName.length()==0 && (legalDescription.toLowerCase().contains("lot")||legalDescription.toLowerCase().contains("unit"))) {	
			subdivisionName = legalDescription.replaceFirst(".*?-(.*?)-\\s\\z", "$1").trim();
			subdivisionName = LegalDescription.extractSubdivisionNameByCleaning(subdivisionName, tokens2).trim();
		}
		//SUB OF LOTS 1, 2, 7 & 8 OF BLK 27 SYCAMORE (ORIGINAL TOWN)) - LOT 1 OF 7 & SUB LOT 1 OF LOT 8
		if (subdivisionName.length()==0 && (legalDescription.toLowerCase().contains("lot")||legalDescription.toLowerCase().contains("unit"))) {	
			subdivisionName = legalDescription.replaceFirst("(?i).*?BL(?:OC)?K\\s\\d+(.*?)-\\s\\z", "$1").trim();
			subdivisionName = LegalDescription.extractSubdivisionNameByCleaning(subdivisionName, tokens2).trim();
		}
		//PT OF LOT 38 OF PRAIRIE VIEW - UNIT 2
		if (subdivisionName.length()==0 && (legalDescription.toLowerCase().contains("lot")||legalDescription.toLowerCase().contains("unit"))) {	
			subdivisionName = legalDescription.replaceFirst("(?i).*?LOT\\s[\\w-]+[^/]OF(.*?)-\\s\\z", "$1").trim();
			subdivisionName = LegalDescription.extractSubdivisionNameByCleaning(subdivisionName, tokens2).trim();
		}
		subdivisionName = subdivisionName.replaceAll("\\)\\)", ")");		//SYCAMORE (ORIGINAL TOWN))
		subdivisionName = subdivisionName.trim().replaceFirst("\\A-", "").replaceFirst("-\\z", "");
		subdivisionName = subdivisionName.trim().replaceFirst("(?is)(AN )?EXT(ENSION)? OF", "");
		if (subdivisionName.matches("(?i).*?\\d+(\\.\\d+)\\s?FT.*"))		//ASSESSORS LOTS OF SEC 32 - W-LY 82.5FT LOT 17
			subdivisionName = "";
		if (subdivisionName.length()<2)
			subdivisionName = "";
		subdivisionName = subdivisionName.trim();
		if (subdivisionName.length()!=0) resultMap.put(PropertyIdentificationSetKey.SUBDIVISION_NAME.getKeyName(), subdivisionName);
		
		List<String> blk = RegExUtils.getMatches("(?is)BL(?:OC)?K\\s(\\d+)", legalDescription, 1);
		String subdivisionBlock = "";
		for (int i=0; i<blk.size(); i++) subdivisionBlock += " " + blk.get(i);
		subdivisionBlock = subdivisionBlock.trim();
		if (subdivisionBlock.length() != 0) {
			subdivisionBlock = LegalDescription.cleanValues(subdivisionBlock, false, true);
			resultMap.put(PropertyIdentificationSetKey.SUBDIVISION_BLOCK.getKeyName(), subdivisionBlock);
		}
		
		List<String> unit = RegExUtils.getMatches("(?is)UNIT\\s([\\w-]+)", legalDescription, 1);
		String subdivisionUnit = "";
		for (int i=0; i<unit.size(); i++) subdivisionUnit += " " + unit.get(i);
		subdivisionUnit = subdivisionUnit.trim();
		if (subdivisionUnit.length() != 0) {
			subdivisionUnit = LegalDescription.cleanValues(subdivisionUnit, false, true);
			resultMap.put(PropertyIdentificationSetKey.SUBDIVISION_UNIT.getKeyName(), subdivisionUnit);
		}
		
		List<String> phase = RegExUtils.getMatches("(?is)PHASE\\s(\\w+)", legalDescription, 1);
		String subdivisionPhase = "";
		for (int i=0; i<phase.size(); i++) subdivisionPhase += " " + phase.get(i);
		subdivisionPhase = subdivisionPhase.trim();
		if (subdivisionPhase.length() != 0) {
			subdivisionPhase = LegalDescription.cleanValues(subdivisionPhase, false, true);
			resultMap.put(PropertyIdentificationSetKey.SUBDIVISION_PHASE.getKeyName(), subdivisionPhase);
		}
		
		List<String> section = RegExUtils.getMatches("(?is)SEC(?:TION)?\\s(\\d+)", legalDescription, 1);
		String subdivisionSection = "";
		for (int i=0; i<section.size(); i++) subdivisionSection += " " + section.get(i);
		subdivisionSection = subdivisionSection.trim();
		if (subdivisionSection.length() != 0) {
			subdivisionSection = LegalDescription.cleanValues(subdivisionSection, false, true);
			resultMap.put(PropertyIdentificationSetKey.SUBDIVISION_SECTION.getKeyName(), subdivisionSection);
		}
		
		String PATTERN1 = "(?i)SEC(?:TION)?\\s\\d+[\\s-,]*T(?:O)?W(?:NSHI)?P\\s(.+?)R(?:ANGE)?([\\w\\s]+)";
		String PATTERN2 = "(?i)SEC(?:TION)?\\s\\d+[\\s-,]*(?:T|R)\\s?(.+?)R\\s?([\\w\\s]+)";
		
		List<String> township1 = RegExUtils.getMatches(PATTERN1, legalDescription, 1);
		String subdivisionTownship = "";
		for (int i=0; i<township1.size(); i++) subdivisionTownship += "," + township1.get(i).replaceAll("[\\s,-]", "").trim();
		if (subdivisionTownship.length()==0) {
			List<String> township2 = RegExUtils.getMatches(PATTERN2, legalDescription, 1);
			for (int i=0; i<township2.size(); i++) subdivisionTownship += "," + township2.get(i).replaceAll("[\\s,-]", "").trim();
		}
		subdivisionTownship = subdivisionTownship.trim();		
		
		List<String> range1 = RegExUtils.getMatches(PATTERN1, legalDescription, 2);
		String subdivisionRange = "";
		for (int i=0; i<range1.size(); i++) subdivisionRange += "," + range1.get(i).replaceAll("[\\s,-]", "").trim();
		if (subdivisionRange.length()==0) {
			List<String> range2 = RegExUtils.getMatches(PATTERN2, legalDescription, 2);
			for (int i=0; i<range2.size(); i++) subdivisionRange += "," + range2.get(i).replaceAll("[\\s,-]", "").trim();
		}
		subdivisionRange = subdivisionRange.trim();
			
		if (subdivisionTownship.length() != 0) {
			subdivisionTownship = LegalDescription.cleanValues(subdivisionTownship, false, true);
			resultMap.put(PropertyIdentificationSetKey.SUBDIVISION_TOWNSHIP.getKeyName(), subdivisionTownship);
		}
		if (subdivisionRange.length() != 0) {
			subdivisionRange = LegalDescription.cleanValues(subdivisionRange, false, true);
			resultMap.put(PropertyIdentificationSetKey.SUBDIVISION_RANGE.getKeyName(), subdivisionRange);
		}
	}
	
	public static void parseTaxes(ResultMap resultMap, String detailsHtml) {
		
		String details = detailsHtml;
		String year = "";
		String baseAmount = "0.0";
				
		Matcher matcher = Pattern.compile(ro.cst.tsearch.servers.types.ILDeKalbTR.YEAR_PATTERN).matcher(details);
		if (matcher.find()) {
			year = matcher.group(1);
			resultMap.put(TaxHistorySetKey.YEAR.getKeyName(), year);
		}
		
		matcher = Pattern.compile("(?is)Total Taxes Billed.*?</h2>(.*?)\\(").matcher(details);
		if (matcher.find()) {
			baseAmount = matcher.group(1).replaceAll("[\\$,]", "").trim().replaceAll("&nbsp;", "");
			resultMap.put(TaxHistorySetKey.BASE_AMOUNT.getKeyName(), baseAmount);
		}
		
		boolean isInstallment1Paid = false;
		boolean isInstallment2Paid = false;
		double amount = 0.0;
		double installment1AmountPaid = 0.0;
		double installment2AmountPaid = 0.0;
		try {
			TableTag installments;
			TableRow row;
			
			org.htmlparser.Parser htmlParser = org.htmlparser.Parser.createParser(details, null);
			NodeList nodeList = htmlParser.parse(null);
			NodeList taxesPaid = nodeList.extractAllNodesThatMatch(new TagNameFilter("table"), true)	
				.extractAllNodesThatMatch(new HasAttributeFilter("CELLSPACING", "3"));
			if (taxesPaid.size()>0) {
				installments = (TableTag)taxesPaid.elementAt(0);
				if (installments.toPlainTextString().contains("Installment")) {
					for (int i=1;i<installments.getRowCount();i++) {
						row = installments.getRow(i);
						double amt = 0.0;
						try {
							amt = Double.parseDouble(row.getColumns()[2].toPlainTextString().replaceAll("[\\$,]", ""));
						} catch (NumberFormatException nfe) {}
						if ("1".equals(row.getColumns()[0].toPlainTextString().trim())) {
							installment1AmountPaid += amt;
							isInstallment1Paid = true;
						} 
						else if ("2".equals(row.getColumns()[0].toPlainTextString().trim())) {
							installment2AmountPaid += amt;
							isInstallment2Paid = true;
						} 
						amount += amt;
					}
				}
			}
			resultMap.put(TaxHistorySetKey.AMOUNT_PAID.getKeyName(), Double.toString(amount));	
			
		} catch (Exception e) {
			e.printStackTrace();
		}
		
		try {
			
			amount = 0.0;
			SimpleDateFormat sdf = new SimpleDateFormat("MM/dd/yyyy");
	        Date today = new Date();
	        String sum1 = "0.00";
			
	        matcher = Pattern.compile("(?is)Installment 1 Due\\s+(\\d\\d/\\d\\d/\\d\\d\\d\\d):\\s*(.*?)\\*").matcher(details);
			if (matcher.find()) {															
				sum1 = matcher.group(2).replaceAll("[\\$,]", "");
			}
	        String installment1BaseAmount = sum1;						//Installment 1 base amount
	        String installment1AmountDue = "0.00";
	        if (!isInstallment1Paid) {
				Vector<Date> inst1Date  = new Vector<Date>();
				Vector<String> inst1Amount  = new Vector<String>();
				matcher = Pattern.compile("(?is)Total Taxes Billed.*\\*.*?If postmarked after these dates,(.*?)\\*\\*.*?If postmarked after these dates,")
					.matcher(details);
				if (matcher.find()) {															//Installment 1 with penalty
					String anotherDetails = matcher.group(1);
					matcher = Pattern.compile("(?is)br>.*?(\\d\\d/\\d\\d/\\d\\d\\d\\d).*?\\$(.*?)<").matcher(anotherDetails);
					while (matcher.find()) {											
						inst1Date.add(sdf.parse(matcher.group(1)));																//date
						inst1Amount.add(matcher.group(2).replaceAll(",", "").replaceAll("(?is)<.*?>", "").trim());				//amount
					}
				}
				
				int i = 0;
				while (i<inst1Date.size() && today.after(inst1Date.elementAt(i))) i++;
				if (i>0) sum1 = inst1Amount.elementAt(i-1);
				installment1AmountDue = sum1;
				amount += Double.parseDouble(sum1);
			}
			
	        String sum2 = "0.0";
	        matcher = Pattern.compile("(?is)Installment 2 Due\\s+(\\d\\d/\\d\\d/\\d\\d\\d\\d):\\s*(.*?)\\*").matcher(details);
			if (matcher.find()) {															
				sum2 = matcher.group(2).replaceAll("[\\$,]", "");
			}
	        String installment2BaseAmount = matcher.group(2).replaceAll("[\\$,]", "").trim();						//Installment 2 base amount
	        String installment2AmountDue = "0.00";
	        if (!isInstallment2Paid) {
				Vector<Date> inst2Date  = new Vector<Date>();
				Vector<String> inst2Amount  = new Vector<String>();
				matcher = Pattern.compile("(?is)If postmarked after these dates,.*?\\*\\*.*?If postmarked after these dates," +
						"(.*?)IF YOU ARE PAYING WITHOUT AN ORIGINAL TAX BILL").matcher(details);
				if (matcher.find()) {															//Installment 2 with penalty
					String anotherDetails = matcher.group(1);
					matcher = Pattern.compile("(?is)br>.*?(\\d\\d/\\d\\d/\\d\\d\\d\\d).*?\\$(.*?)<").matcher(anotherDetails);
					while (matcher.find()) {											
						inst2Date.add(sdf.parse(matcher.group(1)));																//date
						inst2Amount.add(matcher.group(2).replaceAll(",", "").replaceAll("(?is)<.*?>", "").trim());				//amount
					}
				}
				
				int i = 0;
				while (i<inst2Date.size() && today.after(inst2Date.elementAt(i))) i++;
				if (i>0) sum2 = inst2Amount.elementAt(i-1);
				installment2AmountDue = sum2;
				amount += Double.parseDouble(sum2);
			}
			
			resultMap.put(TaxHistorySetKey.TOTAL_DUE.getKeyName(), Double.toString(amount));
			
			DecimalFormat df = new DecimalFormat("#.##");
			
			List<String> line = new ArrayList<String>();
			@SuppressWarnings("rawtypes")
			List<List> bodyInstallments = new ArrayList<List>();
			line = new ArrayList<String>();
			line.add("Installment1");
			line.add(installment1BaseAmount);
			line.add(df.format(installment1AmountPaid));
			line.add(installment1AmountDue);
			if (isInstallment1Paid) {
				line.add("PAID");
			} else {
				line.add("UNPAID");
			}
			bodyInstallments.add(line);
			line = new ArrayList<String>();
			line.add("Installment2");
			line.add(installment2BaseAmount);
			line.add(df.format(installment2AmountPaid));
			line.add(installment2AmountDue);
			if (isInstallment1Paid) {
				line.add("PAID");
			} else {
				line.add("UNPAID");
			}
			bodyInstallments.add(line);
			
			String [] header = {TaxInstallmentSetKey.INSTALLMENT_NAME.getShortKeyName(), TaxInstallmentSetKey.BASE_AMOUNT.getShortKeyName(),
					TaxInstallmentSetKey.AMOUNT_PAID.getShortKeyName(), TaxInstallmentSetKey.TOTAL_DUE.getShortKeyName(),
					TaxInstallmentSetKey.STATUS.getShortKeyName()};				   
			Map<String,String[]> mapInstallments = new HashMap<String,String[]>();
			mapInstallments.put(TaxInstallmentSetKey.INSTALLMENT_NAME.getShortKeyName(), new String[]{TaxInstallmentSetKey.INSTALLMENT_NAME.getShortKeyName(), ""});
			mapInstallments.put(TaxInstallmentSetKey.BASE_AMOUNT.getShortKeyName(), new String[]{TaxInstallmentSetKey.BASE_AMOUNT.getShortKeyName(), ""});
			mapInstallments.put(TaxInstallmentSetKey.AMOUNT_PAID.getShortKeyName(), new String[]{TaxInstallmentSetKey.AMOUNT_PAID.getShortKeyName(), ""});
			mapInstallments.put(TaxInstallmentSetKey.TOTAL_DUE.getShortKeyName(), new String[]{TaxInstallmentSetKey.TOTAL_DUE.getShortKeyName(), ""});
			mapInstallments.put(TaxInstallmentSetKey.STATUS.getShortKeyName(), new String[]{TaxInstallmentSetKey.STATUS.getShortKeyName(), ""});
			
			ResultTable installmentsRT = new ResultTable();	
			installmentsRT.setHead(header);
			installmentsRT.setBody(bodyInstallments);
			installmentsRT.setMap(mapInstallments);
			resultMap.put("TaxInstallmentSet", installmentsRT);
			
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}
