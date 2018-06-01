package ro.cst.tsearch.servers.functions;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.htmlparser.tags.TableRow;

import ro.cst.tsearch.extractor.legal.LegalDescription;
import ro.cst.tsearch.extractor.xml.GenericFunctions;
import ro.cst.tsearch.extractor.xml.GenericFunctions1;
import ro.cst.tsearch.extractor.xml.GenericFunctions2;
import ro.cst.tsearch.extractor.xml.ResultMap;
import ro.cst.tsearch.extractor.xml.ResultTable;
import ro.cst.tsearch.extractor.xml.StringFormats;
import ro.cst.tsearch.search.address2.Normalize;
import ro.cst.tsearch.search.name.FirstNameUtils;
import ro.cst.tsearch.search.name.LastNameUtils;
import ro.cst.tsearch.search.name.NameUtils;
import ro.cst.tsearch.servers.response.OtherInformationSet.OtherInformationSetKey;
import ro.cst.tsearch.servers.response.PropertyIdentificationSet.PropertyIdentificationSetKey;
import ro.cst.tsearch.servers.response.SaleDataSet.SaleDataSetKey;
import ro.cst.tsearch.utils.RegExUtils;
import ro.cst.tsearch.utils.StringUtils;

public class MOCassAO {

	public final static String[] CITIES = {"ARCHIE", "BALDWIN PARK", "BELTON", "CLEVELAND", "CREIGHTON",
		                                   "DREXEL" ,"EAST LYNNE", "FREEMAN", "GARDEN CITY", "GUNN CITY", 
		                                   "HARRISONVILLE", "KANSAS CITY", "LAKE ANNETTE", "LAKE WINNEBAGO", "LEE'S SUMMIT", 
		                                   "LOCH LLOYD", "PECULIAR", "PLEASANT HILL", "RAYMORE", "STRASBURG", 
		                                   "WEST LINE",
		                                   "UNKNOWN"};	//not a real city, but can appear in the address 
	
	public static ResultMap parseIntermediaryRow(TableRow row, long searchId) {
		
		ResultMap resultMap = new ResultMap();
		
		resultMap.put(OtherInformationSetKey.SRC_TYPE.getKeyName(), "AO");
		
		String pin = row.getColumns()[1].toPlainTextString().trim();
		resultMap.put(PropertyIdentificationSetKey.PARCEL_ID2.getKeyName(), pin);
		
		String owner = row.getColumns()[2].toPlainTextString().trim();
		resultMap.put(PropertyIdentificationSetKey.NAME_ON_SERVER.getKeyName(), owner);
		
		String address = row.getColumns()[3].toPlainTextString().trim();
		resultMap.put(PropertyIdentificationSetKey.ADDRESS_ON_SERVER.getKeyName(), address);
		
		String city = row.getColumns()[4].toPlainTextString().trim();
		city = city.replaceAll("(?i)UNKNOWN", "");
		if (!StringUtils.isEmpty(city)) {
			resultMap.put(PropertyIdentificationSetKey.CITY.getKeyName(), city);
		}
		
		parseNames(resultMap, searchId);
		parseAddress(resultMap);
		
		return resultMap;
	}
	
	@SuppressWarnings("rawtypes")
	public static void parseNames(ResultMap resultMap, long searchId) {
		
		String owner = (String) resultMap.get(PropertyIdentificationSetKey.NAME_ON_SERVER.getKeyName());
		if (StringUtils.isEmpty(owner))
			   return;
		
		owner = owner.toUpperCase();
		owner = owner.replaceAll("(?is)&\\s*HUSB", "");
		owner = owner.replaceAll("(?is)&\\s*WF", "");
		owner = owner.replaceAll("(?is)&\\s*ETAL", "ETAL");
		owner = owner.replaceAll("(?is)\\bMRS$", "");
		owner = owner.replaceAll("(?is)\\bCPA$", "");	//Certified Public Accountant
		owner = owner.replaceAll("%", "<br>###FML###");	//M & C PROPERTIES % MICHAEL J CURLEY (070210300000106000)
		owner = owner.replaceAll("\\bA/K/A/", "<br>###FML###");	//MUELLER, ANNA MARIE A/K/A/ ANNA M MUELLER (040930100004003001)
		owner = owner.replaceAll("\\bVACANT\\b", "");	//120736200000307000, 120736200000308002
		owner = owner.replaceAll("-$", "");
		if (!NameUtils.isCompany(owner)) {	//COX, ANNITA G & HAROLD & PEGGY HOWARD ->
											//COX, ANNITA G & COX, HAROLD & HOWARD, PEGGY (030614000000005000)
			owner = owner.replaceAll("(.+?)&(.+?)&(.+)", "$1&$2<br>###FML###$3");
			owner = owner.replaceAll("\\s+OR\\s+", "&");	//MEINS, RONNIE L OR JUDY K TR (120736200000013000)
		}
		
		String[] names = {"", "", "", "", "", ""};
		String[] suffixes = {"", ""} , type, otherType;
		List<List> body = new ArrayList<List>();
		String[] split1 = owner.split("<br>");
		for (String ow1: split1) {
			String[] split2 = ow1.split("(?is)\\bDBA\\s*/?\\b");
			for (String ow2: split2) {
				boolean isCompany = false;
				boolean twoNames = false;
				String secondName = "";
				ow2 = ow2.replaceAll("\\bFAMILY\\s+TR\\b", "FAMILY TRUST");
				if (NameUtils.isCompany(ow2)) {
					ow2 = ow2.replaceAll("/", "@@@");
					isCompany = true;
				} else {	//COLLICHIO, NOAH BISHOP & AMANDA JOY YORK -> COLLICHIO, NOAH BISHOP and YORK, AMANDA JOY (060932104000027000)
					String[] result = extractAllFromName(ow2);
					Matcher matcher1 = Pattern.compile("^(.+?)&\\s*(\\w+)\\s+(\\w+)\\s+(\\w+)$").matcher(result[0]);
					if (matcher1.find()) {
						String p1 = matcher1.group(1);
						String p2 = matcher1.group(2);
						String p3 = matcher1.group(3);
						String p4 = matcher1.group(4);
						if (isFirstMiddleLastName(p2, p3, p4)) {
								ow2 = p1;
								twoNames = true;
								secondName = p4 + ", " + p2 + " " + p3 + " " + result[1];
							}
					} else {	//SANDIFER, DONALD E & MARGARET MCCARTY -> SANDIFER, DONALD E & MCCARTY, MARGARET (030614000000004000)
								//SEIBOLT, WM MITCHELL & LEZLIE ALLEN -> SEIBOLT, WM MITCHELL & ALLEN, LEZLIE (030614000000004001)
								//FRANKE-BROWN, ANDREA C & JW BROWN II -> FRANKE-BROWN, ANDREA C & BROWN, JW II (040306000000087000)
						Matcher matcher2 = Pattern.compile("^(.+?)&\\s*(\\w+)\\s+(\\w+)$").matcher(result[0]);
						if (matcher2.find()) {
							String p1 = matcher2.group(1);
							String p2 = matcher2.group(2);
							String p3 = matcher2.group(3);
							if (isFirstLastName(p2, p3)) {
									ow2 = p1;
									twoNames = true;
									secondName = p3 + ", " + p2 + " " + result[1];
								}
							}
					}
				}
				if (ow2.startsWith("###FML###") && !isCompany && !ow2.contains(",")) {
					names = StringFormats.parseNameDesotoRO(ow2.replaceAll("###FML###", ""), true);
				} else {
					names = StringFormats.parseNameNashville(ow2.replaceAll("###FML###", ""), true);
				}
				if (isCompany) {
					names[2] = names[2].replaceAll("@@@", "/");
				}
				suffixes = GenericFunctions.extractNameSuffixes(names);
				type = GenericFunctions.extractAllNamesType(names);
				otherType = GenericFunctions.extractAllNamesOtherType(names);
				GenericFunctions.addOwnerNames(names, suffixes[0], suffixes[1], type, otherType,
						NameUtils.isCompany(names[2]), NameUtils.isCompany(names[5]), body);
				if (twoNames) {
					names = StringFormats.parseNameNashville(secondName, true);
					suffixes = GenericFunctions.extractNameSuffixes(names);
					type = GenericFunctions.extractAllNamesType(names);
					otherType = GenericFunctions.extractAllNamesOtherType(names);
					GenericFunctions.addOwnerNames(names, suffixes[0], suffixes[1], type, otherType,
							NameUtils.isCompany(names[2]), NameUtils.isCompany(names[5]), body);
				}
			}
		}
		
		try {
			GenericFunctions.storeOwnerInPartyNames(resultMap, body, true);
		} catch (Exception e) {
			e.printStackTrace();
		}
		
	}
	
	//extract suffix, type, other type from name
	public static String[] extractAllFromName(String name) {
		String[] result = {"", ""};
		Matcher matcher = Pattern.compile("(.+?)&(.+)").matcher(name);
		if (matcher.find()) {
			String suffixTypeOtherype = "";
			String[] v1 = GenericFunctions1.extractSuffix(matcher.group(2));
			suffixTypeOtherype += v1[1];
			String[] v2 = GenericFunctions1.extractType(v1[0]);
			suffixTypeOtherype += " " + v2[1];
			String[] v3 = GenericFunctions1.extractOtherType(v2[0]);
			suffixTypeOtherype += " " + v3[1];
			result[0] = matcher.group(1)+ "& " + v3[0];
			result[1] = suffixTypeOtherype.trim();
			return result;
		} else {
			result[0] = name;
			return result;
		}
	}
	
	public static boolean isFirstMiddleLastName(String n1, String n2, String n3) {
		if (n3.length()<2) {
			return false;
		}
		if (LastNameUtils.isLastName(n3) && !FirstNameUtils.isFirstName(n3) &&
				(FirstNameUtils.isFirstName(n1)||n1.length()<=2) && 
				(FirstNameUtils.isFirstName(n2))||n2.length()<=2) {
			return true;
		}
		if (LastNameUtils.isLastName(n3) && !FirstNameUtils.isFemaleName(n3) &&
				FirstNameUtils.isFemaleName(n1) && 
				(FirstNameUtils.isFemaleName(n2))||n2.length()<=2) {
			return true;
		}
		if (LastNameUtils.isLastName(n3) && !FirstNameUtils.isMaleName(n3) &&
				FirstNameUtils.isMaleName(n1) && 
				(FirstNameUtils.isMaleName(n2))||n2.length()==1) {
			return true;
		}
		return false;
	}
	
	public static boolean isFirstLastName(String n1, String n2) {
		if (n2.length()<2) {
			return false;
		}
		if ("Lezlie".equalsIgnoreCase(n1) && "Allen".equalsIgnoreCase(n2)) {	//first last according to NB 0133801
			return true;														//doesn't match any other case below
		}
		if (LastNameUtils.isLastName(n2) && !FirstNameUtils.isFirstName(n2) &&
				(FirstNameUtils.isFirstName(n1)||n1.length()<=2)) {				//Jw Brown (040306000000087000)
			return true;
		}
		if (LastNameUtils.isLastName(n2) && !FirstNameUtils.isFemaleName(n2) &&
				FirstNameUtils.isFemaleName(n1)) {
			return true;
		}
		if (LastNameUtils.isLastName(n2) && !FirstNameUtils.isMaleName(n2) &&
				FirstNameUtils.isMaleName(n1)) {
			return true;
		}
		return false;
	} 
	
	public static void parseAddress(ResultMap resultMap) {
		
		String address = (String) resultMap.get(PropertyIdentificationSetKey.ADDRESS_ON_SERVER.getKeyName());
		if (StringUtils.isEmpty(address))
			return;
		
		String[] split = StringFormats.parseCityFromAddress(address, CITIES);
		split[0] = split[0].replaceAll("(?i)\\bUNKNOWN\\b", "");
		if (!StringUtils.isEmpty(split[0])) {
			resultMap.put(PropertyIdentificationSetKey.CITY.getKeyName(), split[0]);
		}
		address = split[1];
		
		address = address.replaceFirst("(?i)STE\\s*", "");
		address = address.replaceFirst("(?i)APTS?\\s*", "#");
		address = address.replaceFirst("(&\\s*)#", "$1");
		address = address.replaceFirst("^(\\d+)(?:\\s*[,&]\\s*\\d+)*\\s+", "$1 ");
		Matcher matcher = Pattern.compile("(?i)(.+?)(?:(?:\\s+&\\s+)|(?:\\s+THRU\\s*))(\\d+.+)").matcher(address);
		if (matcher.find()) {
			String part1 = matcher.group(1).trim();
			part1 = part1.replaceFirst("(\\d+\\s+)*\\d+$", "").trim();
			String part2 = matcher.group(2).trim();
			String lastWord1 = "";
			String lastWord2 = "";
			int spaceIndex1 = part1.lastIndexOf(" ");
			int spaceIndex2 = part2.lastIndexOf(" ");
			if (spaceIndex1>-1) {
				lastWord1 = part1.substring(spaceIndex1+1);
			}
			if (spaceIndex2>-1) {
				lastWord2 = part2.substring(spaceIndex2+1);
			}
			if (!"".equals(lastWord2)) {
				if (Normalize.isSuffix(lastWord2)) {
					if ("".equals(lastWord1) || !Normalize.isSuffix(lastWord1)) {
						part1 += " " + lastWord2;
					}
				}
			}
			address = part1;
		}
		
		String streetName = StringFormats.StreetName(address).trim();
		String streetNo = StringFormats.StreetNo(address);
		resultMap.put(PropertyIdentificationSetKey.STREET_NAME.getKeyName(), streetName);
		resultMap.put(PropertyIdentificationSetKey.STREET_NO.getKeyName(), streetNo);
		
	}
	
	public static boolean alreadyInBody(String[][] body, String book, String page) {
		if (body==null) {
			return false;
		}
		for (int i=0;i<body.length;i++) {
			if (body[i].length>4) {
				if (book.equals(body[i][3]) && page.equals(body[i][4])) {
					return true;
				}
			}
		}
		return false;
	}
	
	@SuppressWarnings("rawtypes")
	public static void parseLegalSummary(ResultMap resultMap)
	{
		
		String legalDescription = (String) resultMap.get(PropertyIdentificationSetKey.LEGAL_DESCRIPTION_ON_SERVER.getKeyName());
		if(StringUtils.isEmpty(legalDescription)) {
			return;
		}
		
		String[][] tablebody = null;
		ResultTable salesHistory = (ResultTable)resultMap.get("SaleDataSet");
		if (salesHistory!=null) {
			tablebody = salesHistory.getBody();
		}
		List<List> newtablebody = new ArrayList<List>();
		String refExpr = "(?is)\\b(\\d+)/(\\d+)\\s*,";
		legalDescription += ","; 
		Matcher matcher1 = Pattern.compile(refExpr).matcher(legalDescription); 
		while (matcher1.find()) {
			if (!alreadyInBody(tablebody, matcher1.group(1), matcher1.group(2))) {
				List<String> list = new ArrayList<String>();
				list.add("");	//date
				list.add("");	//grantor
				list.add("");	//grantee
				list.add(matcher1.group(1));	//book
				list.add(matcher1.group(2));	//page
				list.add("");	//type
				list.add("");	//amount
				newtablebody.add(list);
			}
		}
		String[] header = {SaleDataSetKey.RECORDED_DATE.getShortKeyName(), SaleDataSetKey.GRANTOR.getShortKeyName(), 
				SaleDataSetKey.GRANTEE.getShortKeyName(), SaleDataSetKey.BOOK.getShortKeyName(), 
				SaleDataSetKey.PAGE.getShortKeyName(), SaleDataSetKey.DOCUMENT_TYPE.getShortKeyName(),
				SaleDataSetKey.SALES_PRICE.getShortKeyName()};
		List<List> completetablebody = new ArrayList<List>();
		if (tablebody==null) {
			completetablebody = newtablebody;
		} else {
			for (int i=0;i<tablebody.length;i++) {
				completetablebody.add(Arrays.asList(tablebody[i]));
			}
			for (int i=0;i<newtablebody.size();i++) {
				completetablebody.add(newtablebody.get(i));
			}
		}
		ResultTable completeSalesHistory = GenericFunctions2.createResultTable(completetablebody, header);
		if (completeSalesHistory != null && completetablebody.size()>0){
			resultMap.put("SaleDataSet", completeSalesHistory);
		}
		legalDescription = legalDescription.replaceAll(refExpr, "");
		legalDescription = legalDescription.replaceFirst("-$", "");
		
		String lotExpr1 = "(?i)LO?T\\s+([A-Z]+)";
		List<String> lot = RegExUtils.getMatches(lotExpr1, legalDescription, 1);
		StringBuilder sb = new StringBuilder();
		for (int i=0; i<lot.size(); i++)
			sb.append(lot.get(i)).append(" ");
		String lotSubExpr = "\\d+(?:(?:-\\d+)|(?:\\s*TO\\s*\\d+))?";
		String lotExpr2 = "(?i)LO?TS?\\s*(" + lotSubExpr + "(?:\\s*,\\s*" + lotSubExpr + ")*(?:\\s*&\\s*" + lotSubExpr +")?)";
		lot = RegExUtils.getMatches(lotExpr2, legalDescription, 1);
		for (int i=0; i<lot.size(); i++) {
			String everyLot = lot.get(i);
			everyLot = everyLot.replaceAll("(?i)(\\d+)\\s*TO\\s*(\\d+)", "$1-$2");
			everyLot = everyLot.replaceAll("[,&]", " ");
			sb.append(everyLot).append(" ");
		}
		String subdivisionLot = sb.toString().trim();
		if (subdivisionLot.length() != 0) {
			subdivisionLot = LegalDescription.cleanValues(subdivisionLot, false, true);
			resultMap.put(PropertyIdentificationSetKey.SUBDIVISION_LOT_NUMBER.getKeyName(), subdivisionLot);
		}
		
		List<String> block = RegExUtils.getMatches("(?i)BLK\\s*([0-9A-Z]+)", legalDescription, 1);
		sb = new StringBuilder();
		for (int i=0; i<block.size(); i++) 
			sb.append(block.get(i)).append(" ");
		String subdivisionBlock = sb.toString().trim();
		if (subdivisionBlock.length() != 0) {
			subdivisionBlock = LegalDescription.cleanValues(subdivisionBlock, false, true);
			resultMap.put(PropertyIdentificationSetKey.SUBDIVISION_BLOCK.getKeyName(), subdivisionBlock);
		}
		
		String subdivisionExpr1 = "(?i)(.+?)\\s+(LO?TS?|BLK|BG)";
		Matcher matcher2 = Pattern.compile(subdivisionExpr1).matcher(legalDescription);
		if (matcher2.find()) {
			String subd = matcher2.group(1);
			subd = subd.replaceFirst("(?is)BLK.*", "");
			subd = subd.replaceFirst("(?is)\\bBG.*", "");
			subd = subd.replaceFirst("(?is)\\b(NE|NW|SE|SW|N|S|E|W)(\\d+|').*", "");
			subd = subd.replaceFirst("(?is)\\bTOWN\\s+OF\\b", "");
			subd = subd.replaceFirst("(?is)\\bTRACT.*", "");
			subd = subd.replaceFirst("(?is)\\bPLEASANT VIEW AC\\b", "PLEASANT VIEW ACRES");
			subd = subd.replaceFirst("(?is)\\bMHLL\\b", "");
			subd = subd.replaceFirst("(?is)\\bMHLL\\b", "");
			subd = subd.replaceFirst("(?is)^[A-Z]\\s+[A-Z]$", "");
			subd = subd.trim();
			if (subd.length()>0) {
				if (subd.matches(".*?\\bCONDO")) {
					resultMap.put(PropertyIdentificationSetKey.SUBDIVISION_COND.getKeyName(), subd);
				}
				resultMap.put(PropertyIdentificationSetKey.SUBDIVISION_NAME.getKeyName(), subd);
			}
		} else {
			String subdivisionExpr2 = "(?i),(.+?)\\bAS\\s+REC\\b";
			Matcher matcher3 = Pattern.compile(subdivisionExpr2).matcher(legalDescription);
			if (matcher3.find()) {
				String subd = matcher3.group(1).trim();
				if (subd.length()>0) {
					if (subd.matches(".*?\\bCONDO")) {
						resultMap.put(PropertyIdentificationSetKey.SUBDIVISION_COND.getKeyName(), subd);
					}
					resultMap.put(PropertyIdentificationSetKey.SUBDIVISION_NAME.getKeyName(), subd);
				}
			}
		}	
		
		List<String> unit = RegExUtils.getMatches("(?is)\\bUNIT\\s([^\\s]+)\\s", legalDescription, 1);
		sb = new StringBuilder();
		for (int i=0; i<unit.size(); i++) 
			sb.append(unit.get(i)).append(" ");
		String subdivisionUnit = sb.toString().trim();
		if (subdivisionUnit.length() != 0) {
			subdivisionUnit = LegalDescription.cleanValues(subdivisionUnit, false, true);
			resultMap.put(PropertyIdentificationSetKey.SUBDIVISION_UNIT.getKeyName(), subdivisionUnit);
		}
		
		List<String> phase = RegExUtils.getMatches("(?is)\\bPH\\s(\\d+)", legalDescription, 1);
		sb = new StringBuilder();
		for (int i=0; i<phase.size(); i++) 
			sb.append(phase.get(i)).append(" ");
		String subdivisionPhase = sb.toString().trim();
		if (subdivisionPhase.length() != 0) {
			subdivisionPhase = LegalDescription.cleanValues(subdivisionPhase, false, true);
			resultMap.put(PropertyIdentificationSetKey.SUBDIVISION_PHASE.getKeyName(), subdivisionPhase);
		}
		
		List<String> tract = RegExUtils.getMatches("(?i)TR\\s+(\\d+)", legalDescription, 1);
		sb = new StringBuilder();
		for (int i=0; i<tract.size(); i++) 
			sb.append(tract.get(i)).append(" ");
		String subdivisionTract = sb.toString().trim();
		if (subdivisionTract.length() != 0) {
			subdivisionTract = LegalDescription.cleanValues(subdivisionTract, true, true);
			resultMap.put(PropertyIdentificationSetKey.SUBDIVISION_TRACT.getKeyName(), subdivisionTract);
		}
	}
}
