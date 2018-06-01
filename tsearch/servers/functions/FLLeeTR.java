package ro.cst.tsearch.servers.functions;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import ro.cst.tsearch.extractor.legal.LegalDescription;
import ro.cst.tsearch.extractor.xml.GenericFunctions;
import ro.cst.tsearch.extractor.xml.ResultMap;
import ro.cst.tsearch.extractor.xml.ResultTable;
import ro.cst.tsearch.extractor.xml.StringFormats;
import ro.cst.tsearch.search.name.LastNameUtils;
import ro.cst.tsearch.search.name.NameUtils;
import ro.cst.tsearch.servers.response.CrossRefSet.CrossRefSetKey;
import ro.cst.tsearch.servers.response.PropertyIdentificationSet.PropertyIdentificationSetKey;
import ro.cst.tsearch.servers.response.SaleDataSet.SaleDataSetKey;
import ro.cst.tsearch.servers.response.TaxHistorySet.TaxHistorySetKey;
import ro.cst.tsearch.utils.Roman;
import ro.cst.tsearch.utils.StringUtils;

/**
 * parsing functions for FLLeeTR.
 * 
 * @author mihaib
 */

public class FLLeeTR {
	
	protected static String cleanOwnerFLLeeTR(String s) {
		s = s.replaceFirst("\\s*\\+\\s*$", "");
		s = s.replace('+', '&');
		s = s.replaceAll("\\bT/C\\b", "");
		s = s.replaceAll("\\bJ/T\\b", "");
		s = s.replaceAll("\\b\\d+%", "");
		s = s.replaceAll("\\b\\d+/\\d+(\\s*INT)?\\b", "");
		s = s.replaceAll("\\bINT\\b", "");
		s = s.replaceAll("\\bH/W\\b", "");
		s = s.replaceAll("\\bL/E\\b", "");
		s = s.replaceAll("\\bPER REP\\b", "");
		s = s.replaceAll("\\bCO (TR)\\b", "$1");
		s = s.replaceAll("\\s{2,}", " ").trim();
		return s;		
	}
	
	@SuppressWarnings("rawtypes")
	public static void partyNamesInterFLLeeTR(ResultMap resultMap, long searchId) throws Exception {

		String owners = (String) resultMap.get("tmpOwnerName");
		if (StringUtils.isEmpty(owners))
			return;

		String[] lines = owners.trim().split(" {2,}");
		// the owner and co-owner name are stored in the first 1 or 2 lines and
		// the last 2 lines contains the mailing address

		for (int i = 0; i < lines.length; i++) {
			lines[i] = cleanOwnerFLLeeTR(lines[i]);
		}

		// parse line1 as LFM
		String[] a = StringFormats.parseNameNashville(lines[0], true);

		// extract owner name from L2 if the 2nd owner not already extracted
		// from L1
		if (a[5].length() == 0 && lines.length > 1 && lines[1].length() != 0) {
			boolean isFML = lines[1].startsWith("FOR ");
			lines[1] = lines[1].replaceFirst("^FOR\\b\\s*", "");
			String[] b = { "", "", "", "", "", "" };
			int idx = lines[1].indexOf('&');
			if (lines[1].startsWith(a[2]) && idx != -1) { // L2=MILLER ROSEMARY  J TR+ PROFESSIONAL RESEARCH INC FOR ALBERT JAMES
															// MILLER III TRUST in PID=01-44-25-01-00012.0000
				b = StringFormats.parseNameNashville(lines[1].substring(0, idx), true);
			} else if (NameUtils.isCompany(lines[1])) {
				b[2] = lines[1].replaceFirst("(.+? TRUST)\\b.*", "$1");
			} else if (isFML) {
				b = StringFormats.parseNameDesotoRO(lines[1], true); // L2=FOR  MINNIE B COLEMAN EST in PID=18-44-25-P4-01100.0050
			} else {
				if (idx == -1 // L2 contains 1 name
						|| lines[1].matches("(.+?)\\s*&\\s*\\w+(?: \\w)?")) { // L2 is like OL OF OM & SF SM (e.g. BROWN ALLAN & ELEANOR)
					b = StringFormats.parseNameNashville(lines[1], true);
				} else {
					// L2 is like OL OF OM & SL SF SM (e.g. BALL JUNICE J & POTTS LINDA A)
					Pattern p = Pattern.compile("(.+?)\\s*&\\s*.+");
					Matcher ma = p.matcher(lines[1]);
					if (ma.find()) {
						b = StringFormats.parseNameNashville(ma.group(1), true);
					}
				}
			}
			a[3] = b[0];
			a[4] = b[1];
			a[5] = b[2];
		}

		List<List> body = new ArrayList<List>();
		String[] suffixes = { "", "" }, types = { "", "" }, otherTypes = { "", "" };
		types = GenericFunctions.extractAllNamesType(a);
		otherTypes = GenericFunctions.extractAllNamesOtherType(a);
		suffixes = GenericFunctions.extractAllNamesSufixes(a);

		GenericFunctions.addOwnerNames(a, suffixes[0], suffixes[1], types, otherTypes, 
				NameUtils.isCompany(a[2]), NameUtils.isCompany(a[5]), body);

		GenericFunctions.storeOwnerInPartyNames(resultMap, body, true);
	}
	
	@SuppressWarnings("rawtypes")
	public static void partyNamesFLLeeTR(ResultMap resultMap, long searchId) throws Exception {
		   
		String owners = (String) resultMap.get("tmpOwnerName");
		
		if (StringUtils.isEmpty(owners))
			return;
		
		owners = owners.replaceFirst("AMRG/.*", "");
		owners = owners.replaceFirst("-POA", "");
		owners = owners.replaceFirst("K INNIS DANISCO USA", "");
		owners = owners.replaceFirst("(MACHINERY)\\s+\\+\\s+(TRUCK)\\s+(SERVICES)", "$1-$2 $3");
		owners = owners.replaceAll("[A-Z]/[A-Z]\\s", "");
		owners = owners.replaceAll("\\d+\\.\\d+%", "");
		owners = owners.replaceAll("\\b\\d+/\\d+(\\s*INT)?\\b", "");
		owners = owners.replaceAll("\\d+", "");
		// owners = owners.replaceAll("(FOR)(.+)\\s+\\+\\s+(.+)", "$1 $3 + $2");
		owners = owners.replaceAll("\\bFOR\\b", "    ");
		owners = owners.replaceAll("\\+", "&");
		String[] lines = owners.split(" {2,}");
		String[] moreOwners = null;

		for (int i = 0; i < lines.length; i++) {
			lines[i] = cleanOwnerFLLeeTR(lines[i]);
		}

		List<List> body = new ArrayList<List>();
		String[] names = { "", "", "", "", "", "" };
		String[] moreNames = { "", "", "", "", "", "" };
		String[] suffixes, type, otherType;

		String ln = "";

		for (int i = 0; i < lines.length; i++) {
			String ow = lines[i];
			ow = ow.replaceFirst("(MACHINERY)-(TRUCK)", "$1 & $2");
			names = StringFormats.parseNameNashville(ow, true);
			if ("&".equals(ow.trim()))
				continue;

			if (i > 0) {
				moreOwners = ow.split("&");
				names = StringFormats.parseNameNashville("");
			}
			if (moreOwners != null) {
				for (int j = 0; j < moreOwners.length; j++) {
					String own = moreOwners[j];
					if ((own.contains("TRUST")) || (own.contains("EXEC"))) {
						// own = own.replaceAll("\\bTRUST\\b", "");
						own = own.replaceFirst("\\bEXEC\\b", "");
						moreNames = StringFormats.parseNameDesotoRO(own, true);
					} else {
						moreNames = StringFormats.parseNameNashville(own, true);
					}
					if ((moreNames[0].length() == 1) && !LastNameUtils.isLastName(moreNames[2])) {
						moreNames[1] = moreNames[0];
						moreNames[0] = moreNames[2];
						moreNames[2] = ln;
					}
					/*
					 if ((moreNames[0].length() > 1) && !LastNameUtils.isLastName(moreNames[2])) { 
					 	moreNames[1] = moreNames[0]; 
					 	moreNames[0] = moreNames[2]; 
					 	moreNames[2] = ln; 
					 }
					 */
					if ((moreNames[0].length() == 0) && (moreNames[1].length() == 0) && !NameUtils.isCompany(moreNames[2])) {
						moreNames[0] = moreNames[2];
						moreNames[2] = ln;
					}
					ln = moreNames[2];
					suffixes = GenericFunctions.extractNameSuffixes(moreNames);
					type = GenericFunctions.extractAllNamesType(moreNames);
					otherType = GenericFunctions.extractAllNamesOtherType(moreNames);
					GenericFunctions.addOwnerNames(moreNames, suffixes[0], suffixes[1], type, otherType, 
								NameUtils.isCompany(moreNames[2]), NameUtils.isCompany(moreNames[5]), body);
				}
			}

			type = GenericFunctions.extractAllNamesType(names);
			otherType = GenericFunctions.extractAllNamesOtherType(names);
			suffixes = GenericFunctions.extractNameSuffixes(names);
			GenericFunctions.addOwnerNames(names, suffixes[0], suffixes[1], type, otherType, 
						NameUtils.isCompany(names[2]), NameUtils.isCompany(names[5]), body);
		}
		GenericFunctions.storeOwnerInPartyNames(resultMap, body, true);

	}
	
	
	public static void legalTokenizerFLLeeTR(ResultMap resultMap, long searchId) throws Exception {

		
		String instrNo = (String) resultMap.get(SaleDataSetKey.INSTRUMENT_NUMBER.getKeyName());
		if(instrNo != null && instrNo.length() == 13){
			resultMap.put(SaleDataSetKey.RECORDED_DATE.getKeyName(), instrNo.substring(0, 4));
		}
		
		String legal = (String) resultMap.get(PropertyIdentificationSetKey.PROPERTY_DESCRIPTION.getKeyName());
		// initial cleanup of legal description
		legal = legal.replaceAll("\\bPARL( IN)?\\b", "");
		legal = legal.replaceAll("\\+", "&");
		legal = legal.replaceAll("(\\d) AND (\\d)", "$1 & $2");
		legal = legal.replaceAll("(\\d) TO (\\d)", "$1-$2");
		legal = legal.replaceAll("\\sTHRU\\s", "-");
		legal = legal.replaceAll("(\\d)\\s+-", "$1-");
		legal = legal.replaceAll("-\\s+(\\d)", "-$1");
		legal = legal.replaceAll("(?<!UNIT )\\b[SWEN]\\s*[SWEN]?\\s*(\\d+ )?\\d*[\\./]?\\d+(\\s*FT)?(\\s*OF)?\\b", "");
		String unitPatt = "\\d+(?:-?[A-Z])?|[A-Z](?:-?\\d+)?";
		legal = legal.replaceAll("\\b(UNIT (?:" + unitPatt + ")) P(AR)?T[\\.\\s]*\\d*\\b", "$1");
		legal = legal.replaceAll("\\bP(AR)?T\\b\\.?", "");
		legal = legal.replaceAll("\\bUNREC\\b\\.?", "");
		legal = legal.replaceAll("\\bREPL(AT)?\\b\\.?", "");
		legal = legal.replaceAll("\\bAKA\\b", "");
		legal = legal.replaceAll("\\b(A )?SUBD[\\. ]+OF\\b", "");
		legal = legal.replaceAll("\\s{2,}", " ").trim();

		String legalSub = legal;
		legal = GenericFunctions.replaceNumbers(legal);
		String[] exceptionTokens = { "I", "M", "C", "L", "D" };
		legal = Roman.normalizeRomanNumbersExceptTokens(legal, exceptionTokens); // convert roman numbers to arabics
		legal = legal.replaceAll("\\s{2,}", " ").trim();

		// extract and remove unit from legal description
		String unit = "";
		// Pattern p =
		// Pattern.compile("\\b(?:U(?:NI)?T|APT\\b|U-)\\s*("+unitPatt+")\\b");
		Pattern p = Pattern.compile("(?is)\\b(?:UNIT|APT\b|U-|UT|U)\\s*(" + unitPatt + ")\\b");
		Matcher ma = p.matcher(legal);
		while (ma.find()) {
			unit = unit + " " + ma.group(1).replaceFirst("^0+(.+)", "$1");
			legalSub = legalSub.replace(ma.group(0), " UNIT ");
		}
		unit = unit.replaceAll("\\s{2,}", " ").trim();
		if (unit.length() != 0) {
			unit = LegalDescription.cleanValues(unit, false, true);
			resultMap.put(PropertyIdentificationSetKey.SUBDIVISION_UNIT.getKeyName(), unit);
		}

		// extract and remove block from legal description
		p = Pattern.compile("\\bBLK[\\s\\.]+(\\d+|[A-Z]{1,2})\\b");
		ma.usePattern(p);
		ma.reset();
		if (ma.find()) {
			resultMap.put(PropertyIdentificationSetKey.SUBDIVISION_BLOCK.getKeyName(), ma.group(1));
			legalSub = legalSub.replace(ma.group(0), "BLK ");
		}

		// extract and remove plat book & page from legal description
		String platPatt1 = "\\s*(\\d+) P(?:GS?)?\\s*(\\d+(?:\\s*[&-]\\s*\\d+)*)\\b";
		String platPatt2 = "\\s*(\\d+)/(\\d+)\\b";
		p = Pattern.compile("\\bPB" + platPatt1);
		ma.usePattern(p);
		ma.reset();
		boolean foundPlat = true;
		if (!ma.find()) {
			p = Pattern.compile("\\bPB" + platPatt2);
			ma.usePattern(p);
			ma.reset();
			foundPlat = ma.find();
		}
		if (foundPlat) {
			resultMap.put(PropertyIdentificationSetKey.PLAT_BOOK.getKeyName(), ma.group(1).replaceFirst("^0+(.+)", "$1"));
			resultMap.put(PropertyIdentificationSetKey.PLAT_NO.getKeyName(), ma.group(2).replace('&', ' ').replaceAll("\\s{2,}", " ").replaceAll("\\b0+(\\d+)", "$1"));
			legalSub = legalSub.replace(ma.group(0), "PB ");
		}

		// extract and remove condominium plat book & page from legal
		// description
		p = Pattern.compile("\\bCPB" + platPatt1);
		ma.usePattern(p);
		ma.reset();
		boolean foundCondoPlat = true;
		if (!ma.find()) {
			p = Pattern.compile("\\bCPB" + platPatt2);
			ma.usePattern(p);
			ma.reset();
			foundCondoPlat = ma.find();
		}
		if (foundCondoPlat) {
			resultMap.put(PropertyIdentificationSetKey.CONDOMINIUM_PLAT_BOOK.getKeyName(), ma.group(1).replaceFirst("^0+(.+)", "$1"));
			resultMap.put(PropertyIdentificationSetKey.CONDOMINIUM_PLAT_PAGE.getKeyName(),
									ma.group(2).replace('&', ' ').replaceAll("\\s{2,}", " ").replaceAll("\\b0+(\\d+)", "$1"));
			legalSub = legalSub.replace(ma.group(0), "CPB ");
		}

		// extract and remove cross refs b&p from legal description
		List<List> bodyCR = new ArrayList<List>(); // can have multiple
													// occurrences
		p = Pattern.compile("\\b(OR|DB)(?: BK)?\\s*(\\d+) PG?\\s*(\\d+)\\b");
		ma.usePattern(p);
		ma.reset();
		while (ma.find()) {
			List<String> line = new ArrayList<String>();
			line.add(ma.group(2).replaceFirst("^0+(.+)", "$1"));
			line.add(ma.group(3).replaceFirst("^0+(.+)", "$1"));
			line.add(ma.group(1));
			bodyCR.add(line);
			legalSub = legalSub.replace(ma.group(0), "OR ");
		}
		p = Pattern.compile("\\b(OR|DB)\\s*(\\d+)/(\\d+)\\b");
		ma.usePattern(p);
		ma.reset();
		while (ma.find()) {
			List<String> line = new ArrayList<String>();
			line.add(ma.group(2).replaceFirst("^0+(.+)", "$1"));
			line.add(ma.group(3).replaceFirst("^0+(.+)", "$1"));
			line.add(ma.group(1));
			bodyCR.add(line);
			legalSub = legalSub.replace(ma.group(0), "OR ");
		}
		p = Pattern.compile("(?<!\\b(?:OR|DB|CPB|PB)) (\\d+)/(\\d+)\\b");
		ma.usePattern(p);
		ma.reset();
		while (ma.find()) {
			List<String> line = new ArrayList<String>();
			line.add(ma.group(1).replaceFirst("^0+(.+)", "$1"));
			line.add(ma.group(2).replaceFirst("^0+(.+)", "$1"));
			line.add("OR");
			bodyCR.add(line);
			legalSub = legalSub.replace(ma.group(0), "OR ");
		}
		if (!bodyCR.isEmpty()) {
			String[] header = { "Book", "Page", "Book_Page_Type" };
			Map<String, String[]> map = new HashMap<String, String[]>();
			map.put("Book", new String[] { "Book", "" });
			map.put("Page", new String[] { "Page", "" });
			map.put("Book_Page_Type", new String[] { "Book_Page_Type", "" });

			ResultTable cr = new ResultTable();
			cr.setHead(header);
			cr.setBody(bodyCR);
			cr.setMap(map);
			resultMap.put("CrossRefSet", cr);
			legal = legal.replaceAll("\\s{2,}", " ").trim();
		}

		// extract and remove cross refs instrument# from legal description
		p = Pattern.compile("\\bINST#\\s*(\\d{4})-(\\d+)\\b");
		ma.usePattern(p);
		ma.reset();
		if (ma.find()) {
			resultMap.put(CrossRefSetKey.INSTRUMENT_NUMBER.getKeyName(), ma.group(1) + String.format("%1$9s", ma.group(2)).replace(' ', '0'));
			legalSub = legalSub.replace(ma.group(0), "INST ");
		}

		// extract and remove lot from legal description
		String lotPatt = "(?:\\b[A-Z]\\s*)?\\d+(?:\\s*[A-Z])?";
		String lot = ""; // can have multiple occurrences
		p = Pattern.compile("\\bLOTS? (" + lotPatt + "(?:\\s*[&\\-\\s]\\s*" + lotPatt + ")*)\\b");
		ma.usePattern(p);
		ma.reset();
		while (ma.find()) {
			lot = lot + " " + ma.group(1).replace('&', ' ');
			legalSub = legalSub.replace(ma.group(0), "LOT ");
		}
		p = Pattern.compile("\\bLT\\s*(" + lotPatt + ")\\b");
		ma.usePattern(p);
		ma.reset();
		while (ma.find()) {
			lot = lot + " " + ma.group(1);
			legalSub = legalSub.replace(ma.group(0), "LOT ");
		}
		lot = lot.replaceAll("\\s{2,}", " ").trim();
		if (lot.matches("^\\d+.*")) {
			lot = lot.replaceAll("\\b(\\d+) ([A-Z])\\b", "$1$2");
		} else if (lot.matches("^[A-Z]\\b.*")) {
			lot = lot.replaceAll("\\b([A-Z]) (\\d+)\\b", "$1$2");
		}
		if (lot.length() != 0) {
			lot = LegalDescription.cleanValues(lot, false, true);
			resultMap.put(PropertyIdentificationSetKey.SUBDIVISION_LOT_NUMBER.getKeyName(), lot);
		}

		// extract and remove tract from legal description
		p = Pattern.compile("\\bTRACT ([A-Z]|\\d+)\\b");
		ma.usePattern(p);
		ma.reset();
		if (ma.find()) {
			resultMap.put(PropertyIdentificationSetKey.SUBDIVISION_TRACT.getKeyName(), ma.group(1));
			legalSub = legalSub.replace(ma.group(0), "TRACT ");
		}

		// extract and remove building # from legal description
		p = Pattern.compile("\\bBLDG (\\d+|[A-Z]\\d*)\\b");
		ma.usePattern(p);
		ma.reset();
		if (ma.find()) {
			resultMap.put(PropertyIdentificationSetKey.SUBDIVISION_BLDG.getKeyName(), ma.group(1));
			legalSub = legalSub.replace(ma.group(0), "BLDG ");
		}

		String tokens[] = { "X", "L", "C", "D", "M" };
		// extract and remove phase from legal description
		p = Pattern.compile("\\bPH(?:ASE)? (\\d+|[A-Z])\\b");
		ma.usePattern(p);
		ma.reset();
		if (ma.find()) {
			String phase = ma.group(1);
			phase = Roman.normalizeRomanNumbersExceptTokens(phase, tokens);
			resultMap.put(PropertyIdentificationSetKey.SUBDIVISION_PHASE.getKeyName(), phase);
			legalSub = legalSub.replace(ma.group(0), "PHASE ");
		}

		// extract and remove section from legal description
		p = Pattern.compile("\\bSEC(?:TION)? (\\d+|[A-Z])\\b");
		ma.usePattern(p);
		ma.reset();
		if (ma.find()) {
			String sec = ma.group(1).replaceFirst("^0+(.+)", "$1");
			sec = Roman.normalizeRomanNumbersExceptTokens(sec, tokens);
			if (!sec.matches("[A-Z]")) {
				resultMap.put(PropertyIdentificationSetKey.SUBDIVISION_SECTION.getKeyName(), sec);
				legalSub = legalSub.replace(ma.group(0), "SEC ");
			}
		}

		// extract and remove township and range from legal description
		p = Pattern.compile("\\bTWP (\\d+) R (\\d+)\\b");
		ma.usePattern(p);
		ma.reset();
		if (ma.find()) {
			resultMap.put(PropertyIdentificationSetKey.SUBDIVISION_TOWNSHIP.getKeyName(), ma.group(1));
			resultMap.put(PropertyIdentificationSetKey.SUBDIVISION_RANGE.getKeyName(), ma.group(2));
			legalSub = legalSub.replace(ma.group(0), "TWP ");
		}

		// extract subdivision name
		legalSub = legalSub.trim().replaceAll("\\s{2,}", " ");
		String subdiv = "";
		String patt = "(.*?)\\s*\\b(UNIT|OR|CONDO(MINIUM)?|PB|BLK|(TRAIL )?(AS )?DESC|TRACT|PHASE|SEC(TION)?|SUB(D(IVISION)?)?|S/D|PARCELS?|CPB|BLDG|DB|LO?TS?|INST)\\b.*";
		p = Pattern.compile(patt);
		ma = p.matcher(legalSub);
		if (ma.find()) {
			subdiv = ma.group(1);
		}
		if (subdiv.matches(".*\\bCOR OF\\b.*")) { // PID 18-46-23-T4-0060A.0100
			p = Pattern.compile(".*\\b(?:(?:LOT|BLK)\\s*[&\\s]\\s*)+(.*?)\\s*(\\bUNIT\\b.*|$)");
			ma.usePattern(p);
			ma.reset();
			if (ma.find()) {
				subdiv = ma.group(1);
			}
		}
		subdiv = subdiv.replaceFirst("^[SWEN]{1,2} OF\\s*(.*)", "$1");
		if (subdiv.length() != 0) {
			subdiv = subdiv.replaceFirst("\\s*&\\s*$", "");
			// remove last token from subdivision name if it is a number (as
			// roman or arabic)
			p = Pattern.compile("(.*)\\b(\\w+)$");
			ma = p.matcher(subdiv);
			if (ma.find()) {
				String lastToken = ma.group(2);
				lastToken = Roman.normalizeRomanNumbersExceptTokens(lastToken, exceptionTokens);
				if (lastToken.matches("\\d+")) {
					subdiv = ma.group(1);
					subdiv = subdiv.replaceFirst("\\s*#$", "");
				}
			}
			subdiv = subdiv.replaceAll("\\s{2,}", " ").trim();
			if (subdiv.length() != 0) {
				resultMap.put(PropertyIdentificationSetKey.SUBDIVISION_NAME.getKeyName(), subdiv);
				if (foundCondoPlat || legalSub.matches(".*\\bCONDO(MINIUM)?\\b.*"))
					resultMap.put(PropertyIdentificationSetKey.SUBDIVISION_COND.getKeyName(), subdiv);
			}
		}
	}

	@SuppressWarnings("rawtypes")
	public static void taxFLLeeTR(ResultMap resultMap, long searchId) throws Exception {

		String amtPaid = GenericFunctions.sum((String) resultMap.get("tmpAmtPaid"), searchId);
		resultMap.put(TaxHistorySetKey.AMOUNT_PAID.getKeyName(), amtPaid);

		// for installments taxes compute BA as the sum of BA for each installment (Cristi's suggestion) - not sure it's correct, BA on no
		// other site was implemented this way
		// String status = (String) resultMap.get("tmpStatus");
		// if ("INSTALL".equals(status)){ // tax must be paid as installments
		// 		ResultTable tis = (ResultTable) resultMap.get("TaxInstallmentSet");
		// 		if (tis != null){
		// 			String [][] body = tis.body;
		// 			if (body.length > 0 && body[0].length >= 2){
		// 				StringBuilder baInstallments = new StringBuilder("0.00");
		// 				for(int i=0; i<body.length; i++){
		// 					baInstallments.append("+").append(body[i][1]);
		// 				}
		// 				String ba = sum(baInstallments.toString(), searchId);
		// 				if (!"0.00".equals(ba)){
		// 					resultMap.put("TaxHistorySet.BaseAmount", ba);
		// 				}
		// 			}
		// 		}
		// }

		String status = (String) resultMap.get("tmpStatus");

		ResultTable unpaidTbl = (ResultTable) resultMap.get("tmpUnpaidTaxesTbl");
		String crtTaxYear = (String) resultMap.get(TaxHistorySetKey.YEAR.getKeyName());
		StringBuilder priorDelinq = new StringBuilder("0.00");
		StringBuilder amountDue = new StringBuilder("0.00");

		boolean certificateSold = "CERT".equals(status);
		if (unpaidTbl != null && !StringUtils.isEmpty(crtTaxYear)) {
			String[][] body = unpaidTbl.getBodyRef();
			if (body.length > 0 && body[0].length >= 2) {
				for (int i = 0; i < body.length; i++) {
					if (!body[i][0].equals(crtTaxYear)) {
						priorDelinq.append("+").append(body[i][1]);
					}
					if (body[i][0].equals(crtTaxYear) && certificateSold) {
						amountDue.append("+").append(body[i][1]);
					}
				}
			}
		}
		String pd = GenericFunctions.sum(priorDelinq.toString(), searchId);
		String ad = GenericFunctions.sum(amountDue.toString(), searchId);
		if (certificateSold) {
			resultMap.put(TaxHistorySetKey.AMOUNT_PAID.getKeyName(), "");
			resultMap.put(TaxHistorySetKey.TOTAL_DUE.getKeyName(), ad);
		}

		String certNumber = org.apache.commons.lang.StringUtils.defaultIfEmpty((String) resultMap.get("tmpCertificateNumber"), "");
		resultMap.put(TaxHistorySetKey.TAX_BILL_NUMBER.getKeyName(), certNumber);

		resultMap.put(TaxHistorySetKey.PRIOR_DELINQUENT.getKeyName(), pd);

		String receiptDate = (String) resultMap.get(TaxHistorySetKey.RECEIPT_DATE.getKeyName());
		if (!StringUtils.isEmpty(receiptDate) && !certificateSold) {
			ResultTable newRT = new ResultTable();
			List<String> line = new ArrayList<String>();
			line.add(crtTaxYear);
			line.add(amtPaid);
			line.add(receiptDate);

			if (!line.isEmpty()) {
				List<List> bodyRT = new ArrayList<List>();
				bodyRT.add(line);
				String[] header = { "Year", "ReceiptAmount", "ReceiptDate" };
				Map<String, String[]> map = new HashMap<String, String[]>();
				map.put("ReceiptAmount", new String[] { "ReceiptAmount", "" });
				map.put("ReceiptDate", new String[] { "ReceiptDate", "" });
				newRT.setHead(header);
				newRT.setMap(map);
				newRT.setBody(bodyRT);
				newRT.setReadOnly();
				resultMap.put("TaxHistorySet", newRT);
			}
		}
	}
}
