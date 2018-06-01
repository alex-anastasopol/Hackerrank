package ro.cst.tsearch.servers.functions;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang.StringUtils;

import ro.cst.tsearch.extractor.xml.GenericFunctions;
import ro.cst.tsearch.extractor.xml.GenericFunctions1;
import ro.cst.tsearch.extractor.xml.GenericFunctions2;
import ro.cst.tsearch.extractor.xml.ResultMap;
import ro.cst.tsearch.extractor.xml.ResultTable;
import ro.cst.tsearch.extractor.xml.StringFormats;
import ro.cst.tsearch.search.name.FirstNameUtils;
import ro.cst.tsearch.search.name.NameUtils;
import ro.cst.tsearch.servers.response.PropertyIdentificationSet.PropertyIdentificationSetKey;
import ro.cst.tsearch.servers.response.SaleDataSet.SaleDataSetKey;
import ro.cst.tsearch.utils.Roman;

public class OHDelawareRO {

	public static void parseNames(ResultMap resultMap, long searchId) {
		ArrayList<List> grantor = new ArrayList<List>();
		String tmpPartyGtor = (String)resultMap.get("tmpGrantor");
		
		if (StringUtils.isNotEmpty(tmpPartyGtor)) {
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
		
		try {
			GenericFunctions1.setGranteeLanderTrustee2(resultMap, searchId,true);
			GenericFunctions.checkTNCountyROForMERSForMortgage(resultMap, searchId);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	
	public static final String[] COMPANY_NAMES = {"SIMMONS FIRST", "FAST TRAX", "FIRST FEDERAL S & L"};
	
	public static String cleanName(String name) {
		name = name.replaceAll("(-|\\b)DEF\\b", "");
		name = name.replaceAll("(-|\\b)PLF\\b", "");
		name = name.replaceAll("(-|\\b)INTERVENOR\\b", "");
		name = name.replaceAll("\\bMINOR\\s*$", "");
		name = name.replaceAll("\\bDR\\s*$", "");
		name = name.replaceAll("\\bDBA\\s*$", "");
		name = name.replaceAll("\\(\\s*CO[\\s-]?TRUSTEE\\s*\\)\\s*$", " TRUSTEE");
		name = name.replaceAll("(?is)-{2,}", "");
		name = name.replaceAll("^\\s*-", "");
		name = name.replaceAll("-\\s*$", "");
		name = name.replaceAll("\\s*-\\s*,", ",");
		name = name.replaceAll("(?is)\\bEX\\s+PARTE\\b", "");
		name = name.replaceAll("(?is)^-+$", "");
		
		//SMITH, ALLISON D (FORMERLY GLASSCOCK) -> ALLISON D SMITH and ALLISON D GLASSCOCK 
		Matcher m = Pattern.compile("(?is)(.+?,)(.+?)\\(\\s*FORMERLY\\s+(.+?)\\)\\s*$").matcher(name);
		if (m.matches()) {
			name = m.group(1) + m.group(2) + " & " + m.group(3) + ", " + m.group(2); 
		}
		
		//FROST, H G (JACK) JR -> H G FROST and JACK FROST
		m = Pattern.compile("(?is)(.+?,)(.+?)\\((.+?)\\)(.*?)$").matcher(name);
		if (m.matches()) {
			String group4 = "";
			if (m.groupCount()==4) {
				group4 = m.group(4);
			}
			boolean alternateFirst = true;
			String[] split = m.group(3).split("\\s+");
			for (int i=0;i<split.length;i++) {
				//if not first name and not initial -> it isn't an alternate first name
				if (!FirstNameUtils.isFirstName(split[i]) && !split[i].matches("[A-Z]")) {
					alternateFirst = false;
					break;
				}
			}
			if (alternateFirst) {
				name = m.group(1) + m.group(2) + group4 + 
					" & " + m.group(1) + " " + m.group(3);
			}
		}
		
		return name.trim();
	}
	
	@SuppressWarnings("rawtypes")
	public static void parseName(String rawNames, ArrayList<List> list, ResultMap resultMap) {
		String names[] = {"", "", "", "", "", ""};
		String[] suffixes, type, otherType;
		
		String split[] = rawNames.split("(?is)<br>");
		for (int i=0;i<split.length;i++) {
			String name = split[i];
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
				GenericFunctions.addOwnerNames(split[i], names, suffixes[0], suffixes[1], type, otherType, firstNameisCompany, NameUtils.isCompany(names[5]), list);
			}
		}
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
	
	public static String getListOfNames(String setOfNames) {
		String names = setOfNames;
		if (setOfNames.contains("<table")) {
			names = names.replaceAll("(?is)(?:\\s*</tr>\\s*)?\\s*</?table[^>]*>\\s*(?:\\s*<tr>\\s*)?", "");
			names = names.replaceAll("(?is)\\s*</tr>\\s*<tr>\\s*", "");
			names = names.replaceAll("(?is)\\s*<td[^>]*>\\s*(?:<b>)?([^<]+)(?:</b>)?</td>\\s*", "$1" + "<br>");
		}
		
		return names;
	}
	
	public static String checkIfDuplicatesLD(String legalInfo) {
		String tmp = legalInfo;
		boolean keepJustOneLegalDesc = false;
		
		//Matcher m = Pattern.compile("(?is)([\\w\\d\\s-]+)((?:Lot|Bl(?:oc)?k\\|Unit|Subdiv|Condo(?:minium)?)(?:[\\s:=])?\\s*(?:[A-Z]?[\\d-\\s,]+[A-Z]?))(?:[A-Z\\s\\d:,-]+)?(?:<br>)?[^$]*").matcher(tmp);
		Matcher m = Pattern.compile("(?is)([\\w\\d\\s-]+)[@\\s]*([^$]*)").matcher(tmp);
		if (m.find()) {
			int count = 0;
			String refText = m.group(1).trim();
			String candText = m.group(2).trim();
			m = Pattern.compile(refText).matcher(tmp);
			while (m.find()) {
				count++;
				if (count > 1) {
					keepJustOneLegalDesc = true;
					tmp = refText;
					break;
				} else {
					//to be improved
					int idx = refText.lastIndexOf(" ");
					if (idx > -1) {
						if (candText.contains(refText.subSequence(0, idx))) {
							count ++;
							if (count > 1) {
								keepJustOneLegalDesc = true;
								tmp = refText;
								break;
							}
						}
					}
					
				}
			}
			
		}
		
		if (keepJustOneLegalDesc) {
			legalInfo = tmp;
		}
		
		return legalInfo;
	}
	
	public static String cleanLegalDesc(String legalInfo) {
		String cleanLD = legalInfo;
		
		cleanLD = cleanLD.replaceAll("(?is)<br/?>", " ");
		cleanLD = cleanLD.replaceAll("(?is)</a>", "");
		cleanLD = cleanLD.replaceFirst("(?is)\\[?\\s*(?:<b>\\s*)?Parcel(?: Number)?\\s*:\\s*(?:</b>\\s*)?SEE (DO?CO?UMENT|NUMBER)\\s*\\]?", "");
		cleanLD = cleanLD.replaceFirst("(?is)NO LEGAL DESCRIPTION FILED CERTIF", "");
		cleanLD = cleanLD.replaceFirst("(?is)CLAIREDAN DRIVE EXTENSTION DEDICATION", "");
		cleanLD = cleanLD.replaceFirst("(?is)PERSONAL PROPERTY(?: TAX LIEN)?( SEE DOCUMENT(?: FOR AMOUNT)?)?", "");
		cleanLD = cleanLD.replaceFirst("(?is) SEE DOCUMENT FOR LEGAL DESCRIPTION", "");
		cleanLD = cleanLD.replaceFirst("(?is)AND VOL(?:UMES?)? & PAGES FOR (EASEMENTS?|DEEDS?)", "");
		cleanLD = cleanLD.replaceFirst("(?is)\\s*(?:\\bAFFIDAVIT )?CORRECTING DIMENSIONS?\\b(?: \\bBETWEEN| \\bBTW\\b)?", "");
		cleanLD = cleanLD.replaceAll("(?is)(?:3rd|THIRD|2nd|SECOND|1st|FIRST)\\s+AMEND(?:MENT)?", "");
		cleanLD = cleanLD.replaceFirst("(?ism)^(?:MEMORANDUM OF LEASE|MEM OF LEA?SE?|POWER OF ATTORNEY|POA|RESTRICTIONS?|RESTR?|NOTICE OF COMMENCEMENT|NOC|ANNEXATION(?:(?: OF )?\\s*[\\d\\.,]+\\s*\\bACR(?:ES)?\\b(?: FROM)?)?)", "");
		cleanLD = cleanLD.replaceAll("(?is)REFILE OF\\s*([\\d\\s-&]+)","");
		cleanLD = cleanLD.replaceAll("(?is)CERT OF PREMIUM DUE", "");
		
		cleanLD = cleanLD.replaceAll("(?is)(?:(?:AMENDED\\b|\\bRE\\b)?\\s*(?:PLAT\\b)?\\s+)?(?:ANNEXATION\\b (?:\\bOF\\b)?)?" +
				"(?:\\bCAB(?:INET)?\\b\\s+\\d+)?\\s*\\bSL(?:IDES?)?\\b\\s*" +
				"(\\d+[A-Z]?\\s*\\bTHRU\\s*\\d+[A-Z]?(\\s*\\bINCL\\b)?|\\d+\\s*&?,?\\s*\\d+[A-Z]?(?:\\s*&\\s*[A-Z]\\b)?|\\d+[A-Z]?)(\\s*&?,?\\s*\\d+[A-Z]?\\s*)?", "");
		cleanLD = cleanLD.replaceAll("(?is)\\bAFF(?:IDAVIT)? OF COMPLETION\\b\\s+", "");
//		cleanLD = cleanLD.replaceAll("(?is)(?:\\s*&\\s*)?(P(?:AR)?T(?:\\bOF\\b )?\\s*\\bA\\b\\s+)?\\b(?:RE)?(?:SUB(?:DI?V(?:ISION)?)?)?" +
//				"\\b(?: \\bOF\\b )?\\s*(?:\\b[A-Z]\\b(\\s*,?&?\\s*\\b[A-Z]\\b)?)?" +
//				"(?:\\s*\\b(COLUMBUS\\b|LIBERTY\\b|DEL(?:AWARE)?\\b))?", "");
		cleanLD = cleanLD.replaceFirst("(?is)(\\bLOTS?\\s*[\\d\\s&-,]+(?:\\bTHRU\\b\\s*[\\d,&\\s]+)?)\\s+" +
				"(?:(?:ORANGE|COLUMBUS|DEL(?:AWARE)?|DUBLIN|SUNBURY|LIBERTY|GENOA|SUNBURY|POWELL|CONCORD|RADNOR)\\b" +
				"(?:\\s*&\\s*)?(?:\\s*EASEMENTS?|\\s*ESMT\\s*|\\s*SURVIVORSHIP|\\s*ASSIGN OF RENTS|\\s*NOTICE OF COMMENCEMENT)?)" +
				"((?:\\s*\\$[\\d\\.,]+)?(?:\\s*\\[?Parcel\\s*:\\s*[\\d-\\.]+\\s*\\]?)?\\s*)","$1 $2");
		cleanLD = cleanLD.replaceFirst("(?is)(\\bUNITS?\\b\\s*(?:[\\d-&,\\s]+|[A-Z]\\b)(?:\\s*\\bBLDG\\b\\s*(?:\\d+|\\d+-?[A-Z]\\b|[A-Z]\\b))?)\\s*" +
				"(?:(?:ORANGE|COLUMBUS|DEL(?:AWARE)?|DUBLIN|SUNBURY|LIBERTY|GENOA|SUNBURY|POWELL|CONCORD|RADNOR)\\b" +
				"(?:\\s*&\\s*)?(?:\\s*EASEMENTS?|\\s*ESMT\\s*|\\s*SURVIVORSHIP|\\s*ASSIGN OF RENTS|\\s*NOTICE OF COMMENCEMENT)?)" +
				"((?:\\s*\\$[\\d\\.,]+)?(?:\\s*\\[?Parcel\\s*:\\s*[\\d-]+\\s*\\]?)?\\s*)", "$1");
		cleanLD = cleanLD.replaceAll("(?is)(?:(?:(?:PARTIAL|PRT|PAR|MASTER)\\s+)?(?:MTG|MORTGAGE|SHERIFF'?S|TRANSFER ON DEATH)?\\s*)?\\bREL?(?:EASE)?\\b\\s*(?:\\s*OFF? |:\\s*)?" +
				"(?:MTG|MORTGAGE|DEED|LIEN|LEASE|AGMTS?(?:\\s*&\\s*PA)?)?\\s*" +
				"((?:VOL|OR|REC)?(?: VOL)?\\s*(?:[A-Z]-{1,})?\\d+\\s*(?:PAGE|PG)\\s*\\d+(?:\\s*&\\s*)?\\s*(?:[\\d-\\s]+)?)" +
				"(?:CERTIFICATE|CERT|MECHANICS LIEN RELEASE|MECH LN|NOT COMM|AFFIDAVIT|AFF|ASSIGN(?:MENT)?|MTG (?:ASSIGN(?:MENT)?|SUB(?:ORD)?|REL(?:EASE)?|TERM(?:\\s*INATION)? OF LIFE EST(?:ATE?)?))?","$1");
		//cleanLD = cleanLD.replaceAll("(?is)(?:\\bOR\\b|OFF REC )?((?:\\s*VOL\\s*)?\\d+\\s*(?:PG|PAGE?)\\s*[\\d-,&\\s]+)", "$1");
		cleanLD = cleanLD.replaceAll("(?is)(?:\\bRE\\b )?(?:\\bOR\\b |(?:OFF )?REC )?((?:\\s*VOL\\s*)?\\d+\\s*(?:PG|PAGE?)\\s*[\\d-,&\\s]+)", "");
		cleanLD = cleanLD.replaceAll("(?is)(?:ANNEXATION\\s*(?: OF)?)?\\s*[\\d\\.]+\\s*ACR(?:ES)?(?: FROM)?\\s*(?:\\s*ANNEXATION)?", "p");
		cleanLD = cleanLD.replaceAll("(?is)(TERM(?:\\s*INATION)?|(?:PTL|PRT|PART(?:IAL)?)?\\s*REL(?:EASE)?|CANCELLATION)\\s+OF\\s+([A-Z\\s]+|\\d+(?:[-\\s,&\\d]+)(?:FROM\\s*\\d{1,2}\\s*[/-]\\s*\\d{1,2}\\s*[/-]\\s*\\d{2,4})?)", "");
		cleanLD = cleanLD.replaceAll("(?is)(?:(?:PTL |PARTIAL )?REL(?:EASE)?(?: OF )?)?(?:(?:MECHANICS|UNEMPLOYMENT)? LIEN|RIGHT OF FIRST REFUSAL|MORTGAGE|EASEMENT)\\s*(?:REL(?:EL?ASE)?)?", "");
		cleanLD = cleanLD.replaceAll("(?is)\\s*(?:\\bORD(?:INANCE)?\\b|\\bNOT(?:ICE)?\\s+OF\\s+COMM(?:ENCEMENT)?|(?:MEM(?:ORANDUM)?\\s+OF\\s+)?\\bLEASE\\b|" +
				"CORP(?:ORAT(?:ION|E))?|\\bPARTNERSHIPS?|\\bCANCELLATIONS?|SURVIVORSHIP|" +
				"(?:(?:FED(?:ERAL)?(?: TAX)?|UNEMPLOYMENT|MECHANIC'?S)\\s+LIEN(?: REL(?:EASE)?)?|" +
				"FIN(?:ANCING)?\\s+ST(?:ATEMENT)?)\\s*(?:\\s*NO|#)?\\s*[\\d-\\s]+|" +
				"AFF(?:IDAVIT)?(?: OF [A-Z\\s]+)?|POA|(?:DURABLE )?POWER OF ATTORNEY|" +
				"(?:PTL\\b\\s+|PRT\\b\\s+|PAR(?:TIAL)?\\s+)?MTG\\s*(?:REL(?:EASE)?|SUBORD(?:INATION)?|ASSIGN(?:MENT)?)?)", "");
		cleanLD = cleanLD.replaceAll("(?is)ASSIGN(?:MENT)? (?:MTG|OF (?:RENTS|LEASES?|[\\d\\s-]+))", "");
		cleanLD = cleanLD.replaceAll("(?is)FIRST SUPPLEMENTAL OF(?: DECLARATION)?", "");
		cleanLD = cleanLD.replaceAll("(?is)TRANSFERS? ON DEATH", "");
		cleanLD = cleanLD.replaceAll("(?is)(\\bSEC\\b\\s*(?:\\d+|[IVX]+)(?:PH(?:ASE)?\\s*(?:\\d+|[IVX]+))?)\\s+(?:PTS?|PART)\\s*(?:\\d+|[-&]?[A-Z](?:\\s*[&-]\\s*[A-Z])?)", "$1");
		cleanLD = cleanLD.replaceAll("(?is)\\bCERT(?:IF(?:ICATE)?)?\\s+OF\\b\\b\\s+[A-Z\\s]+", "");
		cleanLD = cleanLD.replaceAll("(?is)\\bCERT(?:IF(?:ICATE)?)?\\b", "");
		cleanLD = cleanLD.replaceFirst("(?is)\\bUS ROUTE\\s*\\d+/?", "");
		cleanLD = cleanLD.replaceAll("(?is)Survey\\s*(?:No|#)?:?\\d+\\s*", " ");
		cleanLD = cleanLD.replaceFirst("-\\s*-\\s*-\\s*-", "");
		cleanLD = cleanLD.replaceFirst("\\s{2,}", " ").trim();
		
		return cleanLD;
	}
	
	
	public static void parseLegalDesc(ResultMap resultMap) {
		String legal = (String)resultMap.get("tmpDescription");
		
		if (legal != null) {
			resultMap.remove("tmpDescription");
			resultMap.put(PropertyIdentificationSetKey.PROPERTY_DESCRIPTION.getKeyName(), legal);
			legal = cleanLegalDesc(legal);
			if (StringUtils.isNotEmpty(legal)) {
				if (legal.contains("$")) {
					String considerationAmount = legal;
					considerationAmount = considerationAmount.replaceFirst("(?is).*(\\$[\\d,\\.]+).*", "$1");
					if (StringUtils.isNotEmpty(considerationAmount)) {
						legal = legal.replaceFirst("(?is)\\$[\\d,\\.]+", "");
						considerationAmount = considerationAmount.replaceAll("[,\\$]", "");
						resultMap.put(SaleDataSetKey.CONSIDERATION_AMOUNT.getKeyName(), considerationAmount);
					}
				}
				
				if (legal.toUpperCase().contains("PARCEL")) {
					String parcelNo = legal;
					parcelNo = parcelNo.replaceAll("(?is).*\\[?\\s*(?:<b>\\s*)?Parcel\\s*(?:Number)?\\s*:(?:</b>)?\\s*" +
							"(\\d{14}|\\d{3}\\s*[-\\.]\\s*\\d{3}\\s*[-\\.]\\s*\\d{2}\\s*[-\\.]\\s*\\d{3}\\s*[-\\.]\\s*\\d{3}|\\d{2}\\s*[-\\.]\\s*\\d{6})\\s*\\]?.*", "$1").trim();
					if (StringUtils.isNotEmpty(parcelNo)) {
						resultMap.put(PropertyIdentificationSetKey.PARCEL_ID.getKeyName(), parcelNo);
						legal = legal.replaceFirst("(?is)\\[?\\s*(?:<b>\\s*)?Parcel\\s*(?:Number)?\\s*:(?:</b>)?\\s*" +
							"(\\d{14}|\\d{3}\\s*[-\\.]\\s*\\d{3}\\s*[-\\.]\\s*\\d{2}\\s*[-\\.]\\s*\\d{3}\\s*[-\\.]\\s*\\d{3}|\\d{2}\\s*[-\\.]\\s*\\d{6})\\s*\\]?", "");
					}
				}
				
				if (legal.toUpperCase().contains("BLDG") || legal.toUpperCase().contains("BUILDING")) {
					String bldg = legal;
					bldg = bldg.replaceFirst("(?is).*(?:BLDG|BUILDING)\\s*((?:[A-Z]-?)?[\\d-\\s]+(?:-?[A-Z]\\b)?).*", "$1").trim();
					if (StringUtils.isNotEmpty(bldg)) {
						resultMap.put(PropertyIdentificationSetKey.SUBDIVISION_BLDG.getKeyName(), bldg);
						legal = legal.replaceFirst("(?is)(?:BLDG|BUILDING)\\s*((?:[A-Z]-?)?[\\d-\\s]+(?:-?[A-Z]\\b)?)", "@ ");
					}
				}
				
				if (legal.toUpperCase().contains("UN ") || legal.toUpperCase().contains("UNIT")) {
					String unit = legal;
					//unit = unit.replaceFirst("(?is).*\\bUN(?:ITS?)?\\s*((?:[A-Z]-?)?[\\d-\\s]+(?:-?[A-Z]\\b)?).*", "$1").trim();
					unit = unit.replaceFirst("(?is).*UNITS?\\s*((?:\\d+(?:[\\s-]?[A-Z]\\b)?\\s*(?:\\bTHRU\\b|\\bINCL\\b|-|&)?\\s*)*).*", "$1").trim();
					if (StringUtils.isNotEmpty(unit)) {
						unit = unit.replaceAll("(?is)\\bTHRU\\b", "-");
						unit = unit.replaceAll("(?is)\\bINCL(?:UDING)?\\b", "");
						unit = unit.replaceAll("(?is)\\s{2,}", " ");
						resultMap.put(PropertyIdentificationSetKey.SUBDIVISION_UNIT.getKeyName(), unit);
						legal = legal.replaceFirst("(?is)UNITS?\\s*((?:\\d+(?:[\\s-]?[A-Z]\\b)?\\s*(?:\\bTHRU\\b|\\bINCL\\b|-|&)?\\s*)*)", "@ ");
					}
				}
				
				if (legal.toUpperCase().contains("PH ") || legal.toUpperCase().contains("PHASE")) {
					String ph = legal;
					ph = ph.replaceFirst("(?is).*\\bPH(?:ASE)?\\s*(\\d+|[IVX]+).*", "$1").trim();
					if (StringUtils.isNotEmpty(ph)) {
						ph = Roman.normalizeRomanNumbers(ph);
						resultMap.put(PropertyIdentificationSetKey.SUBDIVISION_PHASE.getKeyName(), ph);
						legal = legal.replaceFirst("(?is)\\bUN(?:IT)?\\s*((?:[A-Z]-?)?[\\d-\\s]+(?:-?[A-Z]\\b)?)", "@ ");
					}
				}
				
				if (legal.toUpperCase().contains("PG") || legal.toUpperCase().contains("PAGE")) {
//					String crossRefs = legal;
//					crossRefs = crossRefs.replaceAll("(?is).*((?:VOL|OR|(?:OFF )?REC)?(?: VOL)?\\s*(?:[A-Z]-{1,})?\\d+\\s*(?:PAGE|PG)\\s*\\d+(\\s*&\\s*[\\d-\\s]+)?).*", "$1");
					legal = legal.replaceAll("(?is)(?:VOL|OR|(?:OFF )?REC)?(?: VOL)?\\s*(?:[A-Z]-{1,})?\\d+\\s*(?:PAGE|PG)\\s*\\d+(\\s*&\\s*[\\d-\\s]+)?", "");
				}
				
				StringBuilder lots = new StringBuilder();
				if (legal.toUpperCase().contains(" LOT") || legal.toUpperCase().contains(" INLOT") || legal.toUpperCase().contains(" OUTLOT") || legal.toUpperCase().contains("LOTS")) {
					String tmp = legal;
					Matcher lotMatcher = Pattern.compile("(?is)(?:IN|OUT)?\\(?\\s*LOTS?\\s*:?\\s*(?:#|NO\\.?)?\\s*([\\d&-,\\s]+)\\s*\\)?").matcher(tmp);
					while (lotMatcher.find()) {
						String val = lotMatcher.group(1).trim();
						val = val.replaceAll("[&,]", " ");
						lots.append(val + " ");
						legal = legal.replaceFirst("(?is)(?:IN|OUT)?\\(?\\s*LOTS?\\s*:?\\s*(?:#|NO\\.?)?\\s*([\\d&-,\\s]+)\\s*\\)?", "@ ");
					}
				}
				
				String subdivName = legal.trim();
				subdivName = checkIfDuplicatesLD(subdivName);
				subdivName = subdivName.replaceAll("@\\s*", " & ");
				subdivName = subdivName.replaceFirst("(?is)& \\bDEL\\b\\s*$", "");
				Matcher m = Pattern.compile("(?is)(?:\\d+(?:\\s*-\\s*\\d+)?)?([A-Z\\s]+)\\bSEC(?:TION)?\\b\\s+(?:(?:NO|#)\\s*)?([\\d-\\s]+|[A-Z]+)\\s+" +
						"(ASHLEY|BERLIN|BELLE?POINT|ORANGE|COLUMBUS|DEL(?:AWARE)?(?:\\s+ CITY)?|DUBLIN|SUNBURY|LIBERTY|GENOA|GALENA|KINGSTONE|OXFORD|POWELL|CONCORD\\b|RADNOR)(?:\\s+\\d+)?")
						.matcher(subdivName);
				if (m.find()) {
					subdivName = m.group(1).trim();
					String section = m.group(2);
					section = section.replaceFirst("(?is)\\s+\\b(?:RE)SUB(?:DI?V(?:ISION)?)?\\b", "").trim();
					if (StringUtils.isNotEmpty(section)) {
						section = Roman.normalizeRomanNumbers(section);
						resultMap.put(PropertyIdentificationSetKey.SECTION.getKeyName(), section);
					}
				} else {
					m = Pattern.compile("(?is)((?:\\d+(?:\\s*-\\s*\\d+)?)?([A-Z\\s]+)\\b(?:RE)?SUB(?:DI?V(?:ISION)?)?(?: (?:\\bNO\\b|#)?\\s*([\\s\\d+-]))?).*").matcher(subdivName);
					if (m.find()) {
						subdivName = m.group(1).trim();
						if ((subdivName.contains(" NO") && subdivName.matches("(?is).*\\bNO\\b.*"))|| subdivName.contains("#")) {
							String section = m.group(3).trim();
							if (StringUtils.isNotEmpty(section)) {
								section = Roman.normalizeRomanNumbers(section);
								resultMap.put(PropertyIdentificationSetKey.SECTION.getKeyName(), section);
							}
						}
					
					} else {
						m = Pattern.compile("(?is)((?:\\d+(?:\\s*-\\s*\\d+)?)?[A-Z\\s]+)\\bCOND(?:O?(?:MINIUM)?)?\\b.*").matcher(subdivName);
						if (m.find()) {
							subdivName = m.group(1).trim();
							if (StringUtils.isNotEmpty(subdivName)) {
								resultMap.put(PropertyIdentificationSetKey.SUBDIVISION_COND.getKeyName(), subdivName);
							}
						
						} else {
							subdivName = subdivName.replaceFirst("(?is)(\\d+)\\s*-\\s*(\\d+)\\s*-\\s*(\\d+)\\s*-\\s*\\s*-\\s*", "$1-$2-$3-@");
							m = Pattern.compile("(?ism)^([[A-Z]\\s&']+)(\\d+)\\s*-\\s*(\\d+)\\s*-\\s*(\\d+)\\s*-\\s*(@|\\d+(?:[,&\\s\\d]+)?).*").matcher(subdivName);
							if (m.find()) {
								subdivName = m.group(1).trim();
								String sec = m.group(2).trim();
								String twn = m.group(3).trim();
								String rng = m.group(4).trim();
								String lot = m.group(5).replaceAll("[,&@]", " ").trim();
								if (StringUtils.isNotEmpty(sec)) {
									resultMap.put(PropertyIdentificationSetKey.SUBDIVISION_SECTION.getKeyName(), sec);
								}
								if (StringUtils.isNotEmpty(twn)) {
									resultMap.put(PropertyIdentificationSetKey.SUBDIVISION_TOWNSHIP.getKeyName(), twn);
								}
								if (StringUtils.isNotEmpty(rng)) {
									resultMap.put(PropertyIdentificationSetKey.SUBDIVISION_RANGE.getKeyName(), rng);
								}
								if (StringUtils.isNotEmpty(lot)) {
									lots.append(lot);
								}
							}
						}
					}
				}
				if (StringUtils.isNotEmpty(lots.toString())) {
					resultMap.put(PropertyIdentificationSetKey.SUBDIVISION_LOT_NUMBER.getKeyName(), lots.toString().replaceAll("\\s+", " ").trim());
				}
				if (StringUtils.isNotEmpty(subdivName)) {
					subdivName = subdivName.replaceFirst("(?is)(.*)&\\s*$", "$1");
					resultMap.put(PropertyIdentificationSetKey.SUBDIVISION_NAME.getKeyName(), subdivName);
				}
			}
		}
		
		return;
	}
	
	public static void parseCrossReferences(ResultMap resultMap) {
		String references = (String)resultMap.get("tmpCrossRef");
		
		if (references != null) {
			references = references.replaceFirst("(?is)&nbsp;\\s*<br>", "");
			if (StringUtils.isNotEmpty(references)) {
				String regExpBkPgWithInstr = "(?is)\\s*(\\d+)\\s*/\\s*(\\d+)\\s*@@@\\s*(\\d{4}\\s*-\\s*\\d{8})\\s*";
				String regExpBkPgOnly = "(?is)\\s*(\\d+)\\s*/\\s*(\\d+)\\s*";
				String regExpInstrOnly = "(?is)\\s*(\\d{4}\\s*-\\s*\\d{8})\\s*";
				List<List> bodyCR = new ArrayList<List>();
				List<String> line = new ArrayList<String>();
				
				String[] refs = references.split("<br>");
				for (int i=0; i<refs.length; i++) {
					String info = refs[i];
					
					Matcher m = Pattern.compile(regExpBkPgWithInstr).matcher(info);
					if (m.find()) {
						line = new ArrayList<String>();
						line.add(m.group(1).trim());
						line.add(m.group(2).trim());
						String instrNo = m.group(3).trim();
						if (instrNo.length() == 13) { // YYYY-NNNNNNNN
							instrNo = instrNo.substring(0,4) + instrNo.substring(5);
						}
						line.add(instrNo);
						bodyCR.add(line);
					} else {
						m = Pattern.compile(regExpBkPgOnly).matcher(info);
						if (m.find()) {
							line = new ArrayList<String>();
							line.add(m.group(1).trim());
							line.add(m.group(2).trim());
							line.add("");
							bodyCR.add(line);
						} else {
							m = Pattern.compile(regExpInstrOnly).matcher(info);
							if (m.find()) {
								line = new ArrayList<String>();
								line.add("");
								line.add("");
								String instrNo = m.group(1).trim();
								if (instrNo.length() == 13) { // YYYY-NNNNNNNN
									instrNo = instrNo.substring(0,4) + instrNo.substring(5);
								}
								line.add(instrNo);
								bodyCR.add(line);
							}
						}
					}
				}
				
				if (!bodyCR.isEmpty()){
					String[] header = { "Book", "Page", "InstrumentNumber" };
					ResultTable rt = GenericFunctions2.createResultTable(bodyCR, header);
					resultMap.put("CrossRefSet", rt);
				}
			}
			
			resultMap.remove("tmpCrossRef");
		}
		
		return;
	}
	
}
