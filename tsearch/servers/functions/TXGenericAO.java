package ro.cst.tsearch.servers.functions;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.gargoylesoftware.htmlunit.html.HtmlTableCell;
import com.gargoylesoftware.htmlunit.html.HtmlTableRow;

import ro.cst.tsearch.extractor.legal.LegalDescription;
import ro.cst.tsearch.extractor.xml.GenericFunctions;
import ro.cst.tsearch.extractor.xml.GenericFunctions2;
import ro.cst.tsearch.extractor.xml.ResultMap;
import ro.cst.tsearch.extractor.xml.StringFormats;
import ro.cst.tsearch.search.name.FirstNameUtils;
import ro.cst.tsearch.search.name.LastNameUtils;
import ro.cst.tsearch.search.name.NameUtils;
import ro.cst.tsearch.servers.response.PropertyIdentificationSet.PropertyIdentificationSetKey;
import ro.cst.tsearch.utils.CountyCities;
import ro.cst.tsearch.utils.InstanceManager;
import ro.cst.tsearch.utils.Roman;
import ro.cst.tsearch.utils.StringUtils;


/**
 * @author mihaib
*/

public class TXGenericAO {
			
	public static void legalTokenizer(ResultMap m, long searchId, int serverId) throws Exception {
		
		String crtCounty = InstanceManager.getManager().getCurrentInstance(searchId).getCurrentCounty().getName();
		
		if ("Bexar".equals(crtCounty)){
			parseLegalTXBexarIS(m, searchId, serverId);
		} else if ("Comal".equals(crtCounty)){
			parseLegalTXComalTR(m, searchId, serverId);
		} else if ("Kendall".equals(crtCounty)){
			parseLegalTXKendallTR(m, searchId, serverId);
		} else if ("Kaufman".equals(crtCounty)){
			parseLegalTXKaufmanTR(m, searchId, serverId);
		}else{
			parseLegalTXKendallTR(m, searchId, serverId);
		}
	}
	
	@SuppressWarnings("rawtypes")
	public static void partyNamesTXBexarIS(ResultMap m, long searchId, int serverId) throws Exception {
		
		String defaultStr ="";
		String stringOwner = org.apache.commons.lang.StringUtils.defaultIfEmpty((String) m.get("tmpOwner"), defaultStr );
		stringOwner = GenericFunctions2.cleanOwnerNameFromPrefix(stringOwner);;
		if (StringUtils.isEmpty(stringOwner))
			return;		
		
		stringOwner = stringOwner.replaceAll("(?is)[^L]/[A-Z]", "");
		
		List<List> body = new ArrayList<List>();
		String[] names = {"", "", "", "", "", ""}, namesCo = {"", "", "", "", "", ""};
		String[] suffixes, types, otherTypes;
		
		String stringCoOwner = org.apache.commons.lang.StringUtils.defaultIfEmpty((String) m.get("tmpCoOwner"), defaultStr);
		
		if (StringUtils.isNotEmpty(stringCoOwner)){
			String[] lines = stringCoOwner.split("\\s{2}");
			
			if (!lines[0].trim().matches("\\d+.*")){
				if (!lines[0].contains("BOX")) {
					if (lines[0].trim().matches("\\w+\\s*\\w?")){
						stringOwner += " " + lines[0];
						stringCoOwner = "";
					} else {
						stringCoOwner = lines[0];
					}
				} else {
					stringCoOwner = "";
				}
			} else {
				stringCoOwner = "";
			}
		}
		
			if (stringOwner.trim().endsWith("&") || stringOwner.contains("&")){
				String[] ownerParts = stringOwner.replaceFirst("&\\s*$", "").split("&");
				if (ownerParts.length == 1 && StringUtils.isNotEmpty(stringCoOwner)){
					stringOwner = stringOwner + stringCoOwner;
					stringCoOwner = "";
				}else{
					String[] split = stringOwner.split("&");
					if (split.length ==2 ){
						String[] split2 = split[1].trim().split("\\s");
						if (split2.length ==2){
							stringOwner = stringOwner.replace("&", "AND");
						}
					}
				}
			}
			
			names = StringFormats.parseNameNashville(stringOwner, true);
			
			boolean secondOwnerOnlyOneFirstName = false;
			if (!NameUtils.isCompany(stringOwner)) {
				int index = stringOwner.indexOf("&");
				if (index>-1) {
					String secondOwner = stringOwner.substring(index+1).trim();
					if (secondOwner.indexOf(" ")==-1)
						secondOwnerOnlyOneFirstName = true;
				}
			}
			
			if (StringUtils.isNotEmpty(names[5])) {
				if (LastNameUtils.isNotLastName(names[5]) && NameUtils.isNotCompany(names[5]) && StringUtils.isNotEmpty(names[4])
						&& LastNameUtils.isLastName(names[4])){
					String aux = names[3];
					names[3] = names[5];
					names[5] = names[4];
					names[4] = aux;
				} else if (!secondOwnerOnlyOneFirstName && LastNameUtils.isNotLastName(names[5]) 
						&& NameUtils.isNotCompany(names[5]) && StringUtils.isEmpty(names[4])){
					names[4] = names[3];
					names[3] = names[5];
					names[5] = names[2];
				} else if (StringUtils.isNotEmpty(names[4]) && FirstNameUtils.isFemaleName(names[3]) //BARNETT HARVEY JR & VIRGINIA DALE:Kaufman
						&& ((FirstNameUtils.isMaleName(names[3]) &&  FirstNameUtils.isMaleName(names[4])) || LastNameUtils.isLastName(names[4]))){
					names[5] = names[4];
					names[4] = "";
				}
			}
			types = GenericFunctions.extractAllNamesType(names);
			otherTypes = GenericFunctions.extractAllNamesOtherType(names);
			suffixes = GenericFunctions.extractNameSuffixes(names);
			
			GenericFunctions.addOwnerNames(names, suffixes[0], suffixes[1], types, otherTypes,
												NameUtils.isCompany(names[2]), NameUtils.isCompany(names[5]), body);
			
			if (StringUtils.isNotEmpty(stringCoOwner)){
				//stringCoOwner = stringCoOwner.replaceAll("(?is)\\bTTEES?\\b", "");
				stringCoOwner = stringCoOwner.replaceAll("(?is)\\s*,\\s*", "");
				stringCoOwner = stringCoOwner.replaceAll("(?is)ATTN\\s*:?\\s*", "");
				stringCoOwner = stringCoOwner.replaceAll("(?is)C/O\\s*:?\\s*", "");
				namesCo = StringFormats.parseNameDesotoRO(stringCoOwner, true);
				if (LastNameUtils.isLastName(namesCo[0]) || LastNameUtils.isLastName(namesCo[0].replaceFirst("\\w+\\s*-\\s*(\\w+)", "$1"))){
					namesCo = StringFormats.parseNameNashville(stringCoOwner, true);
				}
				types = GenericFunctions.extractAllNamesType(namesCo);
				otherTypes = GenericFunctions.extractAllNamesOtherType(namesCo);
				suffixes = GenericFunctions.extractNameSuffixes(namesCo);
				GenericFunctions.addOwnerNames(namesCo, suffixes[0], suffixes[1], types, otherTypes,
												NameUtils.isCompany(namesCo[2]), NameUtils.isCompany(namesCo[5]), body);
			}
			
			GenericFunctions.storeOwnerInPartyNames(m, body, true);
			String nameOnServer = stringOwner;
			if (StringUtils.isNotEmpty(stringCoOwner)){
				nameOnServer +=  " & " + stringCoOwner;
			}
			
			m.put("PropertyIdentificationSet.NameOnServer", nameOnServer);
		
		
	}
	
	public static ResultMap parseIntermediaryRowTXBexarIS(HtmlTableRow tableRow, long searchId,int serverId) throws Exception {
		
		ResultMap resultMap = new ResultMap();

		List<HtmlTableCell> cells = tableRow.getCells();
		if (cells.size() > 6){
			String pid = org.apache.commons.lang.StringUtils.defaultString(cells.get(2).getTextContent()).trim();
			resultMap.put("PropertyIdentificationSet.ParcelID", org.apache.commons.lang.StringUtils.isNotBlank(pid) ? pid.replaceAll("-", "") : "");
			
			String address = org.apache.commons.lang.StringUtils.defaultString(cells.get(4).getTextContent()).trim();
			resultMap.put("tmpAddress", org.apache.commons.lang.StringUtils.isNotBlank(address) ? address : "");
			
			String legal = org.apache.commons.lang.StringUtils.defaultString(cells.get(5).getTextContent()).trim();
			resultMap.put("PropertyIdentificationSet.PropertyDescription", org.apache.commons.lang.StringUtils.isNotBlank(legal) ? legal : "");
			
			String owner = org.apache.commons.lang.StringUtils.defaultString(cells.get(6).getTextContent()).trim();
			resultMap.put("tmpOwner", org.apache.commons.lang.StringUtils.isNotBlank(owner) ? owner : "");
			
			partyNamesTXBexarIS(resultMap, searchId, serverId);
			parseAddressTXBexarIS(resultMap, searchId, serverId);
			legalTokenizer(resultMap, searchId, serverId);
			
		}
		
		return resultMap;
	}

	public static void parseLegalTXComalTR(ResultMap m, long searchId, int serverId) throws Exception {
		
		String legal = (String) m.get("PropertyIdentificationSet.PropertyDescription");
		
		if (StringUtils.isEmpty(legal))
			return;
		
		legal = legal.replaceAll("(?is)\\bLO T\\b", "LOT");
		legal = legal.replaceAll("(?is)\\bBL OCK\\b", "BLOCK");
		legal = legal.replaceAll("(?is)[^-]\\b[SWNE]+\\s*[\\d\\s,\\.']+(?:\\s*F\\s*T)?\\s*O\\s*F\\b", "");
		legal = legal.replaceAll("(?is)\\b(?:EX[A-Z]\\s*)?[SENW]+\\s*(?:IRR|TRI)\\s*[\\d\\s,\\.']+(?:\\s*F\\s*T)?\\s*(?:O\\s*F)?\\b", "");                                                                                                                                   
		legal = legal.replaceAll("(?is)\\b(LOTS?\\s*:?)\\s*(\\d+[A-Z])\\s*&\\s*(\\d+[A-Z])\\b", "$1 $2 $1 $3");
		legal = legal.replaceAll("(?is)(\\d+)\\s*,?\\s*AND\\s*(\\d+)", "$1 & $2");
		legal = legal.replaceAll("(?is)GR PT", " ");
		legal = legal.replaceAll("(?is)\\(\\s*[\\d\\.,]+\\s*\\)", " ");
		
		legal = GenericFunctions.replaceNumbers(legal);
		String[] exceptionTokens = { "I", "M", "C", "L", "D" };
		legal = Roman.normalizeRomanNumbersExceptTokens(legal, exceptionTokens); // convert roman numbers
		
		String legalTemp = legal;
		
		// extract lot from legal description (?:[A-Z])?
		String lot = "";
		Pattern p = Pattern.compile("(?is)\\b(LOTS?\\s*:?)\\s*([\\d&,\\s-]+|[\\d\\s-[A-Z]]+)\\b");
		Matcher ma = p.matcher(legal);
		while (ma.find()) {
			lot = lot + " " + ma.group(2);
			legalTemp = legalTemp.replaceFirst(ma.group(0), ma.group(1) + " ");
		}
		lot = lot.replaceAll("\\s*&\\s*", " ").trim();
		lot = lot.replaceAll("-(\\d)", " $1").trim();
		lot = lot.replaceAll("-\\s", " ").trim();
		lot = lot.replaceAll("\\s*LOT\\s*", " ").trim();
		if (lot.length() != 0) {
			lot = LegalDescription.cleanValues(lot, false, true);
			m.put("PropertyIdentificationSet.SubdivisionLotNumber", lot);
		}
		legalTemp = legalTemp.trim().replaceAll("\\s{2,}", " ");
		legal = legalTemp;
		
		
		// extract ncb from legal description
		String ncb = "";
		p = Pattern.compile("(?is)\\b(CB:?|CITY\\s*BLOCK)\\s*([\\d(?:[A-Z])?]+)\\b");
		ma = p.matcher(legal);
		while (ma.find()) {
			ncb = ncb + " " + ma.group(2);
			legalTemp = legalTemp.replaceFirst(ma.group(0), ma.group(1) + " ");
		}
		ncb = ncb.replaceAll("\\s*&\\s*", " ").trim();
		if (ncb.length() != 0) {
			ncb = LegalDescription.cleanValues(ncb, false, true);
			m.put("PropertyIdentificationSet.NcbNo", ncb);
		}
		legalTemp = legalTemp.trim().replaceAll("\\s{2,}", " ");
		legal = legalTemp;
		
		// extract abstract from legal description
		String absNo = "";
		p = Pattern.compile("(?is)\\b(ABS(?:TRACT)?|A)\\s*[:-]?\\s*(\\d+)\\b");
		ma = p.matcher(legal);
		while (ma.find()) {
			absNo = absNo + " " + ma.group(2).replaceAll("(?is)\\A0+(\\d+)", "$1");
			legalTemp = legalTemp.replaceFirst(ma.group(0), ma.group(1) + " ");
		}
		absNo = absNo.replaceAll("\\s*&\\s*", " ").trim();
		if (absNo.length() != 0) {
			absNo = LegalDescription.cleanValues(absNo, false, true);
			m.put("PropertyIdentificationSet.AbsNo", absNo);
		}
		legalTemp = legalTemp.trim().replaceAll("\\s{2,}", " ");
		legal = legalTemp;
		
		// extract block from legal description
		String block = "";
		p = Pattern.compile("(?is)\\b(BL\\s*(?:OC)?KS?\\s*:?)\\s*(\\d+(?:[A-Z])?|[A-Z])\\b");
		ma = p.matcher(legal);
		while (ma.find()) {
			block = block + " " + ma.group(2);
			legalTemp = legalTemp.replaceFirst(ma.group(0), ma.group(1) + " ");
		}
		block = block.replaceAll("\\s*&\\s*", " ").trim();
		if (block.length() != 0) {
			block = LegalDescription.cleanValues(block, false, true);
			m.put("PropertyIdentificationSet.SubdivisionBlock", block);
		}
		legalTemp = legalTemp.trim().replaceAll("\\s{2,}", " ");
		legal = legalTemp;

		// extract phase from legal description
		String phase = "";
		p = Pattern.compile("(?is)\\b(PH\\s*(?:ASE)?-?)\\s*(\\d{1,3}[A-Z]?)\\b");
		ma = p.matcher(legal);
		if (ma.find()) {
			phase = phase + " " + ma.group(2);
			phase = phase.trim();
			legalTemp = legalTemp.replaceFirst(ma.group(0), ma.group(1) + " ");
			m.put("PropertyIdentificationSet.SubdivisionPhase", phase);
			legalTemp = legalTemp.trim().replaceAll("\\s{2,}", " ");
			legal = legalTemp;
		}
		
		// extract tract from legal description
		String tract = "";
		p = Pattern.compile("(?is)\\b(TRACT)\\s*(\\d+)\\b");
		ma = p.matcher(legal);
		if (ma.find()) {
			tract = tract + " " + ma.group(2);
			tract = tract.trim();
			legalTemp = legalTemp.replaceFirst(ma.group(0), ma.group(1) + " ");
			m.put("PropertyIdentificationSet.SubdivisionTract", tract);
			legalTemp = legalTemp.trim().replaceAll("\\s{2,}", " ");
			legal = legalTemp;
		}
		
		// extract unit from legal description
		String unit = "";
		p = Pattern.compile("(?is)\\b(U\\s*N?\\s*I?T\\s*-?)\\s*([A-Z]?[\\d-#]+[A-Z]?)\\b");
		ma = p.matcher(legal);
		while (ma.find()) {
			unit = unit + " " + ma.group(2);
			unit = unit.trim();
			legalTemp = legalTemp.replaceFirst(ma.group(0), ma.group(1) + " ");
			m.put("PropertyIdentificationSet.SubdivisionUnit", unit);// ma.group(2));
			legalTemp = legalTemp.trim().replaceAll("\\s{2,}", " ");
			legal = legalTemp;
		}

		// extract section from legal description
		p = Pattern.compile("(?is)\\b(SEC?(?:TION)?)\\s+(\\d+)\\b");
		ma = p.matcher(legal);
		if (ma.find()) {
			m.put("PropertyIdentificationSet.SubdivisionSection", ma.group(1));

		}

		legal = extractBuilding(m, legal, legalTemp);
		
		m.put("PropertyIdentificationSet.SubdivisionName", "");
			
				
	}
	
	public static void parseLegalTXKendallTR(ResultMap m, long searchId, int serverId) throws Exception {
		
		String legal = (String) m.get(PropertyIdentificationSetKey.PROPERTY_DESCRIPTION.getKeyName());
		
		if (StringUtils.isEmpty(legal))
			return;
		
		legal = legal.replaceAll("(?is)(\\d+)\\s+THRU\\s+(\\d+)", "$1-$2");
		legal = legal.replaceAll("(?is)\\bBL OCK\\b", "BLOCK");
		legal = legal.replaceAll("(?is)[^-]\\b[SWNE]+\\s*[\\d\\s,\\.']+(?:\\s*F\\s*T)?\\s*O\\s*F\\b", "");
		legal = legal.replaceAll("(?is)(\\s+[SWNE]{1,2})?\\s+PT\\b", "");
		legal = legal.replaceAll("(?is)\\b[\\d\\.]+\\s+ACRES\\b", "");
		legal = legal.replaceAll("(?is)\\b(?:EX[A-Z]\\s*)?[SENW]+\\s*(?:IRR|TRI)\\s*[\\d\\s,\\.']+(?:\\s*F\\s*T)?\\s*(?:O\\s*F)?\\b", "");                                                                                                                                   
		legal = legal.replaceAll("(?is)\\b(LOTS?\\s*:?)\\s*(\\d+[A-Z])\\s*&\\s*(\\d+[A-Z])\\b", "$1 $2 $1 $3");
		legal = legal.replaceAll("(?is)(\\d+)\\s*,?\\s*AND\\s*(\\d+)", "$1 & $2");
		legal = legal.replaceAll("(?is)GR PT", " ");
		legal = legal.replaceAll("(?is)\\(\\s*[\\d\\.,]+\\s*\\)", " ");
		
		//remove N 1/2 OF from  HART, BLOCK AL, LOT N 1/2 OF 4
		legal = legal.replaceAll("\\b(N|S|W|E)\\b\\s\\d+/\\d+ OF", "");
		// remove  LESS 10 FT from  HIGHWOOD, BLOCK 11, LOT 8 LESS 10 FT & 9
		legal = legal.replaceAll("LESS \\d+ FT", "");
		
		legal = GenericFunctions.replaceNumbers(legal);
		String[] exceptionTokens = { "I", "M", "C", "L", "D" };
		legal = Roman.normalizeRomanNumbersExceptTokens(legal, exceptionTokens); // convert roman numbers
		
		String legalTemp = legal;
		
		// extract lot from legal description 
		legalTemp = extractLot(m, legal, legalTemp);
		legal = legalTemp;
		
		// extract ncb from legal description
		legalTemp = extractNCB(m, legal, legalTemp);
		legal = legalTemp;
		
		// extract abstract from legal description
		legalTemp = exractAbsNo(m, legal, legalTemp);
		legal = legalTemp;
		
		// extract block from legal description
		legalTemp = extractBlock(m, legal, legalTemp);
		legal = legalTemp;

		// extract phase from legal description
		legalTemp = extractPhase(m, legal, legalTemp);
		legal = legalTemp;
		
		// extract tract from legal description
		legalTemp = extractTract(m, legal, legalTemp);
		legal = legalTemp;
		
		// extract unit from legal description
		legalTemp = extractUnit(m, legal, legalTemp);
		legal = legalTemp;

		// extract section from legal description
		extractSection(m, legal);

		// extract building #
		legal = extractBuilding(m, legal, legalTemp);
		
		// extract subdivision name from legal description
		extractSubdivisionName(m, legal);
			
				
	}

	/**
	 * @param m
	 * @param legal
	 */
	public static void extractSection(ResultMap m, String legal) {
		Pattern p;
		Matcher ma;
		p = Pattern.compile("(?is)\\b(SEC(?:TION)?)\\s+(\\d+)\\b");
		ma = p.matcher(legal);
		if (ma.find()) {
			m.put("PropertyIdentificationSet.SubdivisionSection", ma.group(1));

		}
	}

	/**
	 * @param m
	 * @param legal
	 * @param legalTemp
	 * @return
	 */
	public static String extractUnit(ResultMap m, String legal, String legalTemp) {
		Pattern p;
		Matcher ma;
		String unit = "";
		p = Pattern.compile("(?is)\\b(UN?I?T?)\\s+(\\d+[A-Z]?)\\b");
		ma = p.matcher(legal);
		while (ma.find()) {
			unit = unit + " " + ma.group(2);
			unit = unit.trim();
			legalTemp = legalTemp.replaceFirst(ma.group(0), ma.group(1) + " ");
			m.put("PropertyIdentificationSet.SubdivisionUnit", unit);// ma.group(2));
			legalTemp = legalTemp.trim().replaceAll("\\s{2,}", " ");
		}
		
		return legalTemp;
	}

	/**
	 * @param m
	 * @param legal
	 * @param legalTemp
	 * @return
	 */
	public static String extractTract(ResultMap m, String legal, String legalTemp) {
		Pattern p;
		Matcher ma;
		String tract = "";
		p = Pattern.compile("(?is)\\b(TRACT)\\s*(\\d+)\\b");
		ma = p.matcher(legal);
		if (ma.find()) {
			tract = tract + " " + ma.group(2);
			tract = tract.trim();
			legalTemp = legalTemp.replaceFirst(ma.group(0), ma.group(1) + " ");
			m.put("PropertyIdentificationSet.SubdivisionTract", tract);
			legalTemp = legalTemp.trim().replaceAll("\\s{2,}", " ");
		}
		return legalTemp;
	}

	/**
	 * @param m
	 * @param legal
	 * @param legalTemp
	 * @return
	 */
	public static String extractPhase(ResultMap m, String legal, String legalTemp) {
		Pattern p;
		Matcher ma;
		String phase = "";
		p = Pattern.compile("(?is)\\b(PH\\s*(?:ASE)?-?)\\s*(\\d{1,3}[A-Z]?)\\b");
		ma = p.matcher(legal);
		if (ma.find()) {
			phase = phase + " " + ma.group(2);
			phase = phase.trim();
			legalTemp = legalTemp.replaceFirst(ma.group(0), ma.group(1) + " ");
			m.put("PropertyIdentificationSet.SubdivisionPhase", phase);
			legalTemp = legalTemp.trim().replaceAll("\\s{2,}", " ");
		}
		return legalTemp;
	}

	/**
	 * @param m
	 * @param legal
	 * @param legalTemp
	 * @return
	 */
	public static String extractBlock(ResultMap m, String legal, String legalTemp) {
		Pattern p;
		Matcher ma;
		String block = "";
		p = Pattern.compile("(?is)\\b(BLO?C?KS?)\\s*(\\d+|[A-Z])\\b");
		ma = p.matcher(legal);
		while (ma.find()) {
			block = block + " " + ma.group(2);
			legalTemp = legalTemp.replaceFirst(ma.group(0), ma.group(1) + " ");
		}
		block = block.replaceAll("\\s*&\\s*", " ").trim();
		if (block.length() != 0) {
			block = LegalDescription.cleanValues(block, false, true);
			m.put("PropertyIdentificationSet.SubdivisionBlock", block);
		}
		legalTemp = legalTemp.trim().replaceAll("\\s{2,}", " ");
		return legalTemp;
	}

	/**
	 * @param m
	 * @param legal
	 * @param legalTemp
	 * @return
	 */
	public static String exractAbsNo(ResultMap m, String legal, String legalTemp) {
		Pattern p;
		Matcher ma;
		String absNo = "";
		p = Pattern.compile("(?is)\\b(ABS(?:TRACT)?\\s?)?[A-Z]?(\\d+)[-|\\s]+SURVEY\\s+\\d+\\b");
		ma = p.matcher(legal);
		while (ma.find()) {
			absNo = absNo + " " + ma.group(2).replaceAll("(?is)\\A0+(\\d+)", "$1");
			legalTemp = legalTemp.replaceFirst(ma.group(0), " ");
		}
		absNo = absNo.replaceAll("\\s*&\\s*", " ").trim();
		if (absNo.length() != 0) {
			absNo = LegalDescription.cleanValues(absNo, false, true);
			m.put("PropertyIdentificationSet.AbsNo", absNo);
		}
		legalTemp = legalTemp.trim().replaceAll("\\s{2,}", " ");
		return legalTemp;
	}

	/**
	 * @param m
	 * @param legal
	 * @param legalTemp
	 * @return
	 */
	public static String extractNCB(ResultMap m, String legal, String legalTemp) {
		Pattern p;
		Matcher ma;
		String ncb = "";
		p = Pattern.compile("(?is)\\b(CB\\s*:?)\\s+(\\d+[A-Z]?)\\b");
		ma = p.matcher(legal);
		while (ma.find()) {
			ncb = ncb + " " + ma.group(2);
			legalTemp = legalTemp.replaceFirst(ma.group(0), ma.group(1) + " ");
		}
		ncb = ncb.replaceAll("\\s*&\\s*", " ").trim();
		if (ncb.length() != 0) {
			ncb = LegalDescription.cleanValues(ncb, false, true);
			m.put("PropertyIdentificationSet.NcbNo", ncb);
		}
		legalTemp = legalTemp.trim().replaceAll("\\s{2,}", " ");
		return legalTemp;
	}

	/**
	 * @param m
	 * @param legal
	 * @param legalTemp
	 * @return
	 */
	public static String extractLot(ResultMap m, String legal, String legalTemp) {
		String lot = "";
		Pattern p = Pattern.compile("(?is)\\b(LO?TS?)\\s+([A-Z]?[\\d\\s&-]+[A-Z]?)\\b");
		Matcher ma = p.matcher(legal);
		while (ma.find()) {
			lot = lot + " " + ma.group(2);
			legalTemp = legalTemp.replaceFirst(ma.group(0), ma.group(1) + " ");
		}
		lot = lot.replaceAll("\\s*&\\s*", " ").trim();
		lot = lot.replaceAll("-(\\d)", " $1").trim();
		lot = lot.replaceAll("-\\s", " ").trim();
		lot = lot.replaceAll("\\s*LOT\\s*", " ").trim();
		if (lot.length() != 0) {
			lot = LegalDescription.cleanValues(lot, false, true);
			m.put("PropertyIdentificationSet.SubdivisionLotNumber", lot);
		}
		legalTemp = legalTemp.trim().replaceAll("\\s{2,}", " ");
		return legalTemp;
	}

	/**
	 * @param m
	 * @param legal
	 * @param legalTemp
	 * @return
	 */
	public static String extractBuilding(ResultMap m, String legal, String legalTemp) {
		Pattern p;
		Matcher ma;
		String bldg = "";
		p = Pattern.compile("(?is)\\b(BLDG)\\s+([A-Z]?-?\\d*?)\\b");
		ma = p.matcher(legal);
		if (ma.find()) {
			bldg = bldg + " " + ma.group(2);
			bldg = bldg.trim();
			legalTemp = legalTemp.replaceFirst(ma.group(0), ma.group(1) + " ");
			m.put("PropertyIdentificationSet.SubdivisionBldg", bldg);
			legalTemp = legalTemp.trim().replaceAll("\\s{2,}", " ");
			legal = legalTemp;
		}
		return legal;
	}

	/**
	 * @param m
	 * @param legal
	 */
	public static void extractSubdivisionName(ResultMap m, String legal) {
		Pattern p;
		Matcher ma;
		String subdiv = "";
		boolean hasSub = false;
		p = Pattern.compile("(?is)([^,]+)");
		ma = p.matcher(legal);
		if (ma.find()) {
			subdiv = ma.group(1);
			hasSub = true;
		}
		if (!hasSub | subdiv.matches("\\w\\s+\\w\\s+\\w+.*")) {
			subdiv = "";
		}

		subdiv = subdiv.replaceAll("&", " & ");
		
		if (subdiv.length() != 0) {
		
			subdiv = subdiv.replaceFirst("(.*)\\s+SU\\s*(?:BD?)?.*", "$1");
			subdiv = subdiv.replaceFirst("(.*)\\s+UNIT\\b.*", "$1");
			subdiv = subdiv.replaceFirst("(.*)\\s+BLOCK\\b.*", "$1");
			subdiv = subdiv.replaceFirst("(.*)\\s+LOT\\b.*", "$1");
			subdiv = subdiv.replaceFirst("(.*)\\s+PH(ASE)?\\b.*", "$1");
			subdiv = subdiv.replaceFirst("(.+)\\s+[A-Z]/.*", "$1");
			subdiv = subdiv.replaceFirst("(.+)\\sADD?.*", "$1");
			subdiv = subdiv.replaceFirst("(.+)\\sUNDIV.*", "$1");
			subdiv = subdiv.replaceFirst("(.+)\\sREFER.*", "$1");
			subdiv = subdiv.replaceFirst("(.*)\\s+U\\b.*", "$1");
			subdiv = subdiv.replaceFirst(",", "");
			m.put("PropertyIdentificationSet.SubdivisionName", subdiv);
			
			if (legal.matches(".*\\bCOND.*"))
				m.put("PropertyIdentificationSet.SubdivisionCond", subdiv);
		}
	}
	
	public static void parseLegalTXKaufmanTR(ResultMap m, long searchId, int serverId) throws Exception {
		
		String legal = (String) m.get("PropertyIdentificationSet.PropertyDescription");
		
		if (StringUtils.isEmpty(legal))
			return;
		
		legal = legal.replaceAll("(?is)(\\d+)\\s+THRU\\s+(\\d+)", "$1-$2");
		legal = legal.replaceAll("(?is)\\bBL OCK\\b", "BLOCK");
		legal = legal.replaceAll("(?is)[^-]\\b[SWNE]+\\s*[\\d\\s,\\.']+(?:\\s*F\\s*T)?\\s*O\\s*F\\b", "");
		legal = legal.replaceAll("(?is)(\\s+[SWNE]{1,2})?\\s+PT\\b", "");
		legal = legal.replaceAll("(?is)\\b[\\d\\.]+\\s+ACRES\\b", "");
		legal = legal.replaceAll("(?is)\\b(?:EX[A-Z]\\s*)?[SENW]+\\s*(?:IRR|TRI)\\s*[\\d\\s,\\.']+(?:\\s*F\\s*T)?\\s*(?:O\\s*F)?\\b", "");                                                                                                                                   
		legal = legal.replaceAll("(?is)\\b(LOTS?\\s*:?)\\s*(\\d+[A-Z])\\s*&\\s*(\\d+[A-Z])\\b", "$1 $2 $1 $3");
		legal = legal.replaceAll("(?is)(\\d+)\\s*,?\\s*AND\\s*(\\d+)", "$1 & $2");
		legal = legal.replaceAll("(?is)\\b(GR )?PT", "");
		legal = legal.replaceAll("(?is)\\(\\s*[\\d\\.,]+\\s*\\)", " ");
		legal = legal.replaceAll("(?is)(\\w+\\s*/\\s*\\w+)", "; $1");
		
		legal = GenericFunctions.replaceNumbers(legal);
		String[] exceptionTokens = { "I", "M", "C", "L", "D" };
		legal = Roman.normalizeRomanNumbersExceptTokens(legal, exceptionTokens); // convert roman numbers
		
		String legalTemp = legal;
		
		// extract lot from legal description 
		String lot = "";
		Pattern p = Pattern.compile("(?is)\\b(LO?TS?)\\s*(?:,|:)?\\s+(\\d?[A-Z]?[\\d\\s&,-]+[A-Z]?)\\b");
		Matcher ma = p.matcher(legal);
		while (ma.find()) {
			lot = lot + " " + ma.group(2);
			legalTemp = legalTemp.replaceFirst(ma.group(0), ma.group(1) + " ");
		}
		lot = lot.replaceAll("\\s*&\\s*", " ").trim();
		lot = lot.replaceAll("-(\\d)", " $1").trim();
		lot = lot.replaceAll("-\\s", " ").trim();
		lot = lot.replaceAll("\\s*LOT\\s*", " ").trim();
		if (lot.length() != 0) {
			lot = LegalDescription.cleanValues(lot, false, true);
			m.put("PropertyIdentificationSet.SubdivisionLotNumber", lot);
		}
		legalTemp = legalTemp.trim().replaceAll("\\s{2,}", " ");
		legal = legalTemp;
		
		
		legalTemp = extractNCB(m, legal, legalTemp);
		legal = legalTemp;
		
		// extract abstract from legal description
		String absNo = "";
		p = Pattern.compile("(?i)\\b(AB?S?T?(?:RACT)?)\\s*(\\d+)\\b");
		ma = p.matcher(legal);
		while (ma.find()) {
			absNo = absNo + " " + ma.group(2).replaceAll("(?is)\\A0+(\\d+)", "$1");
			legalTemp = legalTemp.replaceFirst(ma.group(0), " ");
		}
		absNo = absNo.replaceAll("\\s*&\\s*", " ").trim();
		if (absNo.length() != 0) {
			absNo = LegalDescription.cleanValues(absNo, false, true);
			m.put("PropertyIdentificationSet.AbsNo", absNo);
		}
		legalTemp = legalTemp.trim().replaceAll("\\s{2,}", " ");
		legal = legalTemp;
		
		legalTemp = extractBlock(m, legal, legalTemp);
		legal = legalTemp;

		// extract phase from legal description
		String phase = "";
		p = Pattern.compile("(?is)\\b(PH\\s*(?:ASE)?)\\s+\\&?\\s?(\\d+[A-Z]?)\\b");
		ma = p.matcher(legal);
		while (ma.find()) {
			phase = phase + " " + ma.group(2);
			phase = phase.trim();
			legalTemp = legalTemp.replaceFirst(ma.group(0), ma.group(1) + " ");
			
			legalTemp = legalTemp.trim().replaceAll("\\s{2,}", " ");
			legal = legalTemp;
			ma = p.matcher(legal);
		}
		
		if(StringUtils.isNotEmpty(phase)){
			m.put("PropertyIdentificationSet.SubdivisionPhase", phase.trim().replaceAll("\\s+", " "));
		}
		
		// extract tract from legal description
		String tract = "";
		p = Pattern.compile("(?is)\\b(TR\\s*(?:ACT)?)\\s+([\\d\\.]+-?[A-Z]?-?\\d?|[A-Z])\\b");
		ma = p.matcher(legal);
		if (ma.find()) {
			tract = tract + " " + ma.group(2);
			tract = tract.trim();
			legalTemp = legalTemp.replaceFirst(ma.group(0), ma.group(1) + " ");
			m.put("PropertyIdentificationSet.SubdivisionTract", tract);
			legalTemp = legalTemp.trim().replaceAll("\\s{2,}", " ");
			legal = legalTemp;
		}
		
		// extract unit from legal description
		String unit = "";
		p = Pattern.compile("(?is)\\b(UNIT)\\s+(\\d+[A-Z]?)\\b");
		ma = p.matcher(legal);
		while (ma.find()) {
			unit = unit + " " + ma.group(2);
			unit = unit.trim();
			legalTemp = legalTemp.replaceFirst(ma.group(0), ma.group(1) + " ");
			m.put("PropertyIdentificationSet.SubdivisionUnit", unit);// ma.group(2));
			legalTemp = legalTemp.trim().replaceAll("\\s{2,}", " ");
			legal = legalTemp;
		}

		extractSection(m, legal);

		legal = extractBuilding(m, legal, legalTemp);
		
		// extract subdivision name from legal description
		String subdiv = "";
		p = Pattern.compile("(?is)(.*?)\\s+(PH.*)");
		ma = p.matcher(legal.trim());
		if (ma.find()) {
			subdiv = ma.group(1);
		} else {
			p = Pattern.compile("(?is)(.*?)\\s+(BLO?C?K?.*)");
			ma = p.matcher(legal.trim());
			if (ma.find()) {
				subdiv = ma.group(1);
			} else {
				p = Pattern.compile("(?is)(.*?)\\s+(LO?TS?.*)");
				ma = p.matcher(legal.trim());
				if (ma.find()) {
					subdiv = ma.group(1);
				} else {
					p = Pattern.compile("(?is)(.*?)\\s+(TRACT.*)");
					ma = p.matcher(legal.trim());
					if (ma.find()) {
						subdiv = ma.group(1);
					} else {
						p = Pattern.compile("(?is)([^,]+),\\s+.*");
						ma = p.matcher(legal.trim());
						if (ma.find()) {
							subdiv = ma.group(1);
						} else {
							if (!legal.contains("UNIT") &&
									!legal.contains(",") &&
									!legal.matches("(?ism).*(AB?S?T?(?:RACT)?).*") &&
									!legal.matches("(?ism).*(LO?TS?).*") &&
									!legal.matches("(PH\\s*(?:ASE)?)") &&
									!legal.matches("(TR\\s*(?:ACT)?)")									
									) {
									//task 7834
									subdiv = legal.trim();
							}
						}
					}
				}
			}
		}
		
		if (subdiv.length() != 0) {

			subdiv = subdiv.replaceAll("(?is)\\A\\sAB?S?T?\\s+", "");
			subdiv = subdiv.replaceAll("(?is)\\s+DESC\\b.*", "");

			m.put("PropertyIdentificationSet.SubdivisionName", subdiv.trim());
			if (legal.matches(".*\\b(CONDO(MINIUM)?)\\b.*"))
				m.put("PropertyIdentificationSet.SubdivisionCond", subdiv.trim());
		}
			
				
	}

	public static void parseLegalTXBexarIS(ResultMap m, long searchId, int serverId) throws Exception {
		
		String legal = (String) m.get("PropertyIdentificationSet.PropertyDescription");
		
		if (StringUtils.isEmpty(legal))
			return;
		
		legal = legal.replaceAll("(?is)\\bLO T\\b", "LOT");
		legal = legal.replaceAll("(?is)\\bBL OCK\\b", "BLOCK");
		legal = legal.replaceAll("(?is)\\b[SWNE]+\\s*[\\d\\s,\\.']+(?:\\s*F\\s*T)?\\s*O\\s*F\\b", "");
		legal = legal.replaceAll("(?is)\\b(?:EX[A-Z]\\s*)?[SENW]+\\s*(?:IRR|TRI)\\s*[\\d\\s,\\.']+(?:\\s*F\\s*T)?\\s*(?:O\\s*F)?\\b", "");                                                                                                                                   
		legal = legal.replaceAll("(?is)\\b(LOTS?\\s*:?)\\s*(\\d+[A-Z])\\s*&\\s*(\\d+[A-Z])\\b", "$1 $2 $1 $3");
		legal = legal.replaceAll("(?is)(\\d+)\\s*,?\\s*AND\\s*(\\d+)", "$1 & $2");
		
		legal = GenericFunctions.replaceNumbers(legal);
		String[] exceptionTokens = { "I", "M", "C", "L", "D" };
		legal = Roman.normalizeRomanNumbersExceptTokens(legal, exceptionTokens); // convert roman numbers
		
		String legalTemp = legal;
		
		// extract lot from legal description
		String lot = "";
		Pattern p = Pattern.compile("(?is)\\b(LOTS?\\s*:?)\\s*([\\d&,\\s-]+|[A-Z]?\\s*-?\\s*\\d+[A-Z]?|[A-Z])\\b");
		Matcher ma = p.matcher(legal);
		while (ma.find()) {
			lot = lot + " " + ma.group(2);
			legalTemp = legalTemp.replaceFirst(ma.group(0), ma.group(1) + " ");
		}
		lot = lot.replaceAll("\\s*&\\s*", " ").trim();
		if (lot.length() != 0) {
			lot = LegalDescription.cleanValues(lot, false, true);
			m.put("PropertyIdentificationSet.SubdivisionLotNumber", lot);
		}
		legalTemp = legalTemp.trim().replaceAll("\\s{2,}", " ");
		legal = legalTemp;
		
		
		// extract ncb from legal description
		String ncb = "";
		p = Pattern.compile("(?is)\\b(NCB)\\s*([\\d]+[A-Z]?)\\b");
		ma = p.matcher(legal);
		while (ma.find()) {
			ncb = ncb + " " + ma.group(2);
			legalTemp = legalTemp.replaceFirst(ma.group(0), ma.group(1) + " ");
		}
		ncb = ncb.replaceAll("\\s*&\\s*", " ").trim();
		if (ncb.length() != 0) {
			ncb = LegalDescription.cleanValues(ncb, false, true);
			m.put("PropertyIdentificationSet.NcbNo", ncb);
		}
		legalTemp = legalTemp.trim().replaceAll("\\s{2,}", " ");
		legal = legalTemp;
		
		// extract block from legal description
		String block = "";
		p = Pattern.compile("(?is)\\b(BL\\s*(?:OC)?KS?\\s*:?)\\s*(\\d+|[A-Z])\\b");
		ma = p.matcher(legal);
		while (ma.find()) {
			block = block + " " + ma.group(2);
			legalTemp = legalTemp.replaceFirst(ma.group(0), ma.group(1) + " ");
		}
		block = block.replaceAll("\\s*&\\s*", " ").trim();
		if (block.length() != 0) {
			block = LegalDescription.cleanValues(block, false, true);
			m.put("PropertyIdentificationSet.SubdivisionBlock", block);
		}
		legalTemp = legalTemp.trim().replaceAll("\\s{2,}", " ");
		legal = legalTemp;

		// extract phase from legal description
		String phase = "";
		p = Pattern.compile("(?is)\\b(PH\\s*(?:ASE)?)\\s*(\\d{1,3}[A-Z]?)\\b");
		ma = p.matcher(legal);
		if (ma.find()) {
			phase = phase + " " + ma.group(2);
			phase = phase.trim();
			legalTemp = legalTemp.replaceFirst(ma.group(0), ma.group(1) + " ");
			m.put("PropertyIdentificationSet.SubdivisionPhase", phase);
			legalTemp = legalTemp.trim().replaceAll("\\s{2,}", " ");
			legal = legalTemp;
		}
		
		// extract unit from legal description
		String unit = "";
		p = Pattern.compile("(?is)\\b(U\\s*N?\\s*I?T\\s*-?)\\s*([A-Z]?[\\d-]+[A-Z]?)\\b");
		ma = p.matcher(legal);
		while (ma.find()) {
			unit = unit + " " + ma.group(2);
			unit = unit.trim();
			legalTemp = legalTemp.replaceFirst(ma.group(0), ma.group(1) + " ");
			m.put("PropertyIdentificationSet.SubdivisionUnit", unit);// ma.group(2));
			legalTemp = legalTemp.trim().replaceAll("\\s{2,}", " ");
			legal = legalTemp;
		}

		// extract section from legal description
		p = Pattern.compile("(?is)\\b(SEC?(?:TION)?)\\s+(\\d+)\\b");
		ma = p.matcher(legal);
		if (ma.find()) {
			m.put("PropertyIdentificationSet.SubdivisionSection", ma.group(1));

		}

		// extract building #
		String bldg = "";
		p = Pattern.compile("(?is)\\b(BLDG)\\s+([A-Z]|\\d+)\\b");
		ma = p.matcher(legal);
		if (ma.find()) {
			bldg = bldg + " " + ma.group(2);
			bldg = bldg.trim();
			legalTemp = legalTemp.replaceFirst(ma.group(0), ma.group(1) + " ");
			m.put("PropertyIdentificationSet.SubdivisionBldg", bldg);
			legalTemp = legalTemp.trim().replaceAll("\\s{2,}", " ");
			legal = legalTemp;
		}
		
		// extract subdivision name from legal description
		String subdiv = "";
		boolean hasSub = false;
		p = Pattern.compile("(?is)\\(([A-Z]+[^\\)]+)\\)");
		ma = p.matcher(legal);
		if (ma.find()) {
			subdiv = ma.group(1);
			hasSub = true;
		} else {
			ma.reset();
			p = Pattern.compile("(?is)\\\"([A-Z]+[^\\\"]+)\\\"");
			ma.usePattern(p);
			if (ma.find()) {
				subdiv = ma.group(1);
				hasSub = true;
			} else {
				ma.reset();
				p = Pattern.compile("(?is)\\b(LOTS?\\s*:?)\\s+(.*)");
				ma.usePattern(p);
				if (ma.find()) {
					subdiv = ma.group(2);
					hasSub = true;
				}
			}
		} 
		if (!hasSub | subdiv.matches("\\w\\s+\\w\\s+\\w+.*")) {
			subdiv = "";
		}
	
		
		subdiv = subdiv.replaceAll("&", " & ");
		
		if (subdiv.length() != 0) {
		
			subdiv = subdiv.replaceFirst("(.*)\\s+SU\\s*(?:BD?)?.*", "$1")
					.replaceFirst("(.*)\\s+(U\\s*N?\\s*I?T\\s*-?)", "$1")
					.replaceFirst("(.+)\\s+[A-Z]/.*", "$1")
					.replaceFirst("(.+)\\sADD?.*", "$1")
					.replaceFirst("(.+)\\sFLG.*", "$1")
					.replaceFirst("(.+)\\sREFER.*", "$1")
					.replaceFirst(",", "")
					.replaceAll("(?is).*?\\sFT\\sOF\\s(.*)", "$1")
					.replaceAll("(?is)(^|\\s)LOT(\\s|$)", " ")
					.replaceAll("\\s+", " ").trim();
			m.put("PropertyIdentificationSet.SubdivisionName", subdiv);
			
			if (legal.matches(".*\\bCOND.*"))
				m.put("PropertyIdentificationSet.SubdivisionCond", subdiv);
		}
		
		// extract plat book&page from legal description
		//047110000111   CB 4711 P-11*(7.77), P-100 (.253) A-528 ; CB 4733 P-2D (1.437),P-100A (1.194) A- 153 

		String[] plats = legal.split("\\bCB\\b");
		String pb = "";
		String pg = "";
		for (String eachBook : plats){
			if (StringUtils.isNotEmpty(eachBook)){
				eachBook = "CB " + eachBook;
				p = Pattern.compile("(?is)\\b(CB)\\s*(\\d+[A-Z]?)\\s*(P\\s*-?)\\s*(\\d+[A-Z]?)");
				ma = p.matcher(eachBook);
				if (StringUtils.isNotEmpty(pb.trim())){
					pb += ";";
				}
				if (ma.find()){
					pb = pb + " " + ma.group(2).trim();
				}
				if (StringUtils.isNotEmpty(pg.trim())){
					pg += ";";
				}
				p = Pattern.compile("(?is)\\b([P|A]\\s*-?)\\s*(\\d+[A-Z]?)");
				ma = p.matcher(eachBook);
				while (ma.find()){
					pg = pg + " " + ma.group(2).trim();
				}
			}
		}
		m.put("PropertyIdentificationSet.PlatBook", pb.trim());
		m.put("PropertyIdentificationSet.PlatNo", pg.trim());
		
	}
	
	public static void parseAddressTXBexarIS(ResultMap m, long searchId, int serverId) throws Exception {
		
		String address = (String) m.get("tmpAddress");
		
		if (StringUtils.isEmpty(address))
			return;

		String crtCounty = InstanceManager.getManager().getCurrentInstance(searchId).getCurrentCounty().getName();
		String[] cities = null;
		if ("Bexar".equals(crtCounty)){
			cities = CountyCities.TX_BEXAR;
		} else if ("Comal".equals(crtCounty)){
			cities = CountyCities.TX_COMAL;
		} else if ("Kaufman".equals(crtCounty)){
			cities = CountyCities.TX_KAUFMAN;
		}
		address = address.replaceAll(",", "");
		address = address.replaceAll("\\s+", " ");
		//05124-000-0182
		String[] addr = address.split("\\s+TX");
		
		parseCityFromAddress(m, cities, addr);
		
		if (addr.length > 1){
			if (addr[1].trim().length() > 1){
				m.put("PropertyIdentificationSet.Zip", addr[1].trim());
			}
		} else {
			if (address.matches("(?is)(.+)\\s+(\\d{5,})\\s*$")){
				String zip = address.replaceAll("(?is)(.*?)\\s+(\\d{5,})\\s*$", "$2").trim();
				
				m.put("PropertyIdentificationSet.Zip", zip.replaceAll("\\A\\*0+", ""));
				addr[0] = addr[0].replaceAll("(?is)" + zip, "");
			}
		}
		m.put("PropertyIdentificationSet.StreetName", StringFormats.StreetName(addr[0].trim()));
		m.put("PropertyIdentificationSet.StreetNo", StringFormats.StreetNo(addr[0].trim()));
		
	}

	public static void parseCityFromAddress(ResultMap m, String[] cities, String[] addr) {
		if (cities != null) {
			for (int i = 0; i < cities.length; i++){
				if (addr[0].toUpperCase().contains(cities[i].toUpperCase())){
					m.put("PropertyIdentificationSet.City", cities[i]);
					addr[0] = addr[0].toUpperCase().replaceFirst(cities[i].toUpperCase() + "((?:\\s+\\d+)?\\s*)$", "$1");
					break;
				}
			}
		}
	}
	
}
