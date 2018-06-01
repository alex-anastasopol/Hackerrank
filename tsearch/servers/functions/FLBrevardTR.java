package ro.cst.tsearch.servers.functions;

import java.util.ArrayList;
import java.util.List;
import java.util.Vector;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import ro.cst.tsearch.extractor.legal.LegalDescription;
import ro.cst.tsearch.extractor.xml.GenericFunctions;
import ro.cst.tsearch.extractor.xml.GenericFunctions2;
import ro.cst.tsearch.extractor.xml.ResultMap;
import ro.cst.tsearch.extractor.xml.ResultTable;
import ro.cst.tsearch.extractor.xml.StringFormats;
import ro.cst.tsearch.search.address2.Normalize;
import ro.cst.tsearch.search.name.NameCleaner;
import ro.cst.tsearch.search.name.NameUtils;
import ro.cst.tsearch.servers.response.CrossRefSet.CrossRefSetKey;
import ro.cst.tsearch.servers.response.PropertyIdentificationSet.PropertyIdentificationSetKey;
import ro.cst.tsearch.utils.RegExUtils;
import ro.cst.tsearch.utils.Roman;
import ro.cst.tsearch.utils.StringUtils;

public class FLBrevardTR {
	
	@SuppressWarnings("rawtypes")
	public static void stdFinalFLBrevardTR(ResultMap m, long searchId) throws Exception {
		List<List> body = new ArrayList<List>();
		String owner = (String) m.get("tmpOwnerAddress");
		int i;
		if (StringUtils.isEmpty(owner))
			return;

		owner = GenericFunctions2.resolveOtherTypes(owner);
		String[] lines = owner.split("\\s{2,}");

		if (lines.length <= 2)
			return;

		// clean address out of names
		String[] lines2 = GenericFunctions.removeAddressFLTR(lines, GenericFunctions.excludeWordsFLBrevardTR, GenericFunctions.wordsFLBrevardTR, 2,
				Integer.MAX_VALUE);
		// m.put("tmpNamexxxxx", lines2);
		for (i = 0; i < lines2.length; i++) {
			String[] names = { "", "", "", "", "", "" };
//			boolean isCompany = false;
//			String suffix = "";
			// true = "L, FM SpouseLastName"
			// false = "F M L"
			boolean nameFormat = true;
			// C/O - I want it before clean because I don't clean company names
			if (lines2[i].matches("(?i).*?c/o.*")) {
				lines2[i] = lines2[i].replaceAll("(?i)c/o\\s*", "");
			}
			/*
			 * if (lines2[i].startsWith("ORRIS")){ i = i; }
			 */
			String[] suffixes = { "", "" }, types = { "", "" }, otherTypes = { "", "" };
			String curLine = NameCleaner.cleanNameAndFix(lines2[i], new Vector<String>(), true);
			if (NameUtils.isCompany(curLine)) {
				// this is not a typo, we don't know what to clean in companies'
				// names
				names[2] = lines2[i];
//				isCompany = true;
			} else {
				// find out the format of c/o name
				if (curLine.indexOf(",") == -1) {
					nameFormat = false;
				}
				curLine = cleanOwnerFLBrevardTR(curLine);
				names = nameFormat ? StringFormats.parseNameNashville(curLine, true) : StringFormats.parseNameDesotoRO(curLine, true);
				names = NameCleaner.removeUnderscore(names);
				types = GenericFunctions.extractAllNamesType(names);
				otherTypes = GenericFunctions.extractAllNamesOtherType(names);
				suffixes = GenericFunctions.extractNameSuffixes(names);
				names = NameCleaner.middleNameFix(names);
			}

			GenericFunctions.addOwnerNames(names, suffixes[0], suffixes[1], types, otherTypes, NameUtils.isCompany(names[2]), NameUtils.isCompany(names[5]),
					body);
		}
		GenericFunctions.storeOwnerInPartyNames(m, body, true);
		/*
		 * Done multinameparsing
		 */

		String[] a = StringFormats.parseNameNashville(cleanOwnerFLBrevardTR(lines[0]), true);
		// if co-owner not already present in first line, extract it from the
		// second line
		if ((a[5].length() == 0) && (lines.length >= 4)) {
			String coowner = cleanOwnerFLBrevardTR(lines[1]);
			coowner = coowner.replaceFirst("^&\\s*", "");
			String[] b = StringFormats.parseNameNashville(coowner, true);
			if (b[0].contains(a[2]) || a[2].contains(b[0])) // co-owner is FML -
															// e.g. 0982-04-3501
															// % DAVID P WILSON
				b = StringFormats.parseNameDesotoRO(coowner, true);
			// remove the 3 owner, if present on 2nd line (parsed in the 2nd
			// owner middle name) - e.g. 0495-03-0023 -DAVIS KELLY L SMITH LORI
			// K
			b[1] = b[1].replaceFirst("^([A-Z]) [A-Z]{2,} [A-Z]{2,}( [A-Z])?.+", "$1");
			a[3] = b[0];
			a[4] = b[1];
			a[5] = b[2];
		}
		
		m.put(PropertyIdentificationSetKey.OWNER_FIRST_NAME.getKeyName(), a[0]);
		m.put(PropertyIdentificationSetKey.OWNER_MIDDLE_NAME.getKeyName(), a[1]);
		m.put(PropertyIdentificationSetKey.OWNER_LAST_NAME.getKeyName(), a[2]);
		m.put(PropertyIdentificationSetKey.SPOUSE_FIRST_NAME.getKeyName(), a[3]);
		m.put(PropertyIdentificationSetKey.SPOUSE_MIDDLE_NAME.getKeyName(), a[4]);
		m.put(PropertyIdentificationSetKey.SPOUSE_LAST_NAME.getKeyName(), a[5]);

		// String suffixes[] = extractNameSuffixes(a);
		// addOwnerNames(a, suffixes[0], suffixes[1], NameUtils.isCompany(a[2]),
		// NameUtils.isCompany(a[5]), body);
	}
	 
	protected static String cleanOwnerFLBrevardTR(String s) {
		s = s.toUpperCase();
		s = s.replaceFirst("\\b\\s*H/W", "");
		s = s.replaceFirst("\\b\\s*\\(F/D\\)", "");
		s = s.replaceFirst("\\bC/O\\b", "");
		s = s.replace("%", "&");
		// s = s.replaceAll("\\([^\\)]+\\)", "");
		s = s.replaceAll("\\s{2,}", " ").trim();
		return s;
	}

		
	@SuppressWarnings("rawtypes")
	public static void stdFLBrevardTR(ResultMap m, long searchId) throws Exception {
		String s = (String) m.get("tmpOwnerName");
		if (s == null || s.length() == 0)
			return;

		s = cleanOwnerFLBrevardTR(s);

		if (!s.matches("[A-Z]{2,}(-[A-Z]{2,})?( (JR|SR|II|III))? [A-Z]+( [A-Z]+)?( [A-Z])?")) {
			if (!s.contains("&")) {
				s = s.replaceFirst("^(([A-Z]{2,})\\b.*)\\b([A-Z]{2,}\\-\\2)\\b", "$1& $3");
			}
			if (!s.contains("&")) {
				s = s.replaceFirst("^(([A-Z]{2,})\\b.*)\\b(\\2)\\b", "$1& $3");
			}
			if (!s.contains("&")) {
				s = s.replaceFirst("([A-Z]+(?: (?:JR|SR|II|III))? [A-Z]+(?: [A-Z])?(?: [A-Z])?) ([A-Z]{2,}(?: (?:JR|SR|II|III))? [A-Z]+.*)", "$1 & $2");
			}
			if (!s.contains("&")) {
				s = s.replaceFirst("([A-Z]{2,}) ([A-Z]{2,}) ", "$2" + ", " + "$1");
			}
		}

		String[] a = StringFormats.parseNameNashville(s, true);
		a[4] = a[4].replaceFirst("^([A-Z]) [A-Z]{2,} [A-Z]{2,}( [A-Z])?.+", "$1");

		m.put(PropertyIdentificationSetKey.OWNER_FIRST_NAME.getKeyName(), a[0]);
		m.put(PropertyIdentificationSetKey.OWNER_MIDDLE_NAME.getKeyName(), a[1]);
		m.put(PropertyIdentificationSetKey.OWNER_LAST_NAME.getKeyName(), a[2]);
		m.put(PropertyIdentificationSetKey.SPOUSE_FIRST_NAME.getKeyName(), a[3]);
		m.put(PropertyIdentificationSetKey.SPOUSE_MIDDLE_NAME.getKeyName(), a[4]);
		m.put(PropertyIdentificationSetKey.SPOUSE_LAST_NAME.getKeyName(), a[5]);

		List<List> body = new ArrayList<List>();
		String[] suffixes = { "", "" }, types = { "", "" }, otherTypes = { "", "" };
		types = GenericFunctions.extractAllNamesType(a);
		otherTypes = GenericFunctions.extractAllNamesOtherType(a);
		suffixes = GenericFunctions.extractAllNamesSufixes(a);

		GenericFunctions.addOwnerNames(a, suffixes[0], suffixes[1], types, otherTypes, NameUtils.isCompany(a[2]), NameUtils.isCompany(a[5]), body);

		GenericFunctions.storeOwnerInPartyNames(m, body, true);
	}
		
	public static void legalFinalFLBrevardTR(ResultMap m, long searchId) throws Exception {
		String legal = (String) m.get(PropertyIdentificationSetKey.PROPERTY_DESCRIPTION.getKeyName());
		if (StringUtils.isEmpty(legal))
			return;

		legalFLBrevardTR(m, legal, true);
	}
			   
	public static void legalFLBrevardTR(ResultMap m, String legal, boolean extractSubd) throws Exception {
		// initial corrections and cleanup of legal description
		legal = GenericFunctions.replaceNumbers(legal);
		String[] exceptionTokens = { "I", "M", "C", "L", "D" };
		legal = Roman.normalizeRomanNumbersExceptTokens(legal, exceptionTokens); // convert roman numbers

		// <3938>
		String streetName = (String) m.get(PropertyIdentificationSetKey.STREET_NAME.getKeyName());
		if (!StringUtils.isEmpty(streetName) && streetName.contains("A1A")) {
			legal = legal.replaceAll("\\bA1A\\b", "");
		}
		legal = legal.replaceAll("\\bOF MELBOURNE (?:INDIALANTIC|MELBOURNE)\\b", "OF MELBOURNE");
		// </3938>

		legal = legal.replaceAll("\\bPORT\\s*ST\\s*JOHN\\b", "PORT SAINT JOHN");
		legal = legal.replaceAll("EXC?\\s*RD\\s*R/W", "");
		legal = legal.replaceAll("\\b(?:RD)?\\s*AS\\s*DES(C)?\\s*IN\\b", " ");
		legal = legal.replaceAll("(.*)[\\s]+(CIR|WY|DR|XXXX|AV|LA|ST|RD|LN|CT|TER|BLVD|ROAD)[\\s]+", "$2" + " ");
		legal = legal.replaceAll("\\b(.*)[\\s]+(HIGHWAY)[\\s]+[A-Z0-9]+[\\s]+[\\d]+\\b", "$2");

		legal = legal.replaceAll("\\s*\\bTHRU\\b\\s*", "-");
		legal = legal.replaceAll("\\bCO-OP\\b", "");
		legal = legal.replaceAll("\\b\\d+(ST|ND|RD|TH) ADD(N)?( TO)?\\b", "");
		legal = legal.replaceAll("\\b(THE )?[NWSE]{1,2}(LY)? [\\d\\./\\s]+(\\s*\\bOF)?\\b", "");
		legal = legal.replaceAll("\\b(FT|PT|PART)( OF)?\\b", "");
		legal = legal.replaceAll("\\bRESUB( OF)?\\b", "");

		legal = legal.replaceAll("\\bALSO\\s*KNOWN\\s*AS\\b", "");
		legal = legal.replaceAll("\\b(\\d+(ST|ND|RD|TH) )?REPLAT( OF)?\\b", "");
		legal = legal.replaceAll("\\b(REVISED|AMENDED) PLAT( OF)?\\b", "");
		// legal = legal.replaceAll(",\\s*SUBJ TO [^,]+,?", "");
		legal = legal.replaceFirst("(?is)(SEC)\\s*([A-Z]+) ([\\d]+)", "$1" + " " + "$2" + "-" + "$3");

		legal = legal.replace(",", "");
		legal = legal.replaceAll("\\s{2,}", " ").trim();

		String legalTemp = legal;

		// extract lot from legal description
		String lot = ""; // can have multiple occurrences
		Pattern p = Pattern.compile("\\b(LOT)S?\\s*([\\d,\\s]+)\\b");
		Matcher ma = p.matcher(legal);
		while (ma.find()) {
			lot = lot + " " + ma.group(2);
			legalTemp = legalTemp.replaceFirst(ma.group(0), ma.group(1) + " ");
		}
		lot = lot.replaceAll("\\s*&\\s*", " ").trim();
		if (lot.length() != 0) {
			lot = LegalDescription.cleanValues(lot, false, true);
			m.put(PropertyIdentificationSetKey.SUBDIVISION_LOT_NUMBER.getKeyName(), lot);
		}
		legalTemp = legalTemp.trim().replaceAll("\\s{2,}", " ");
		legal = legalTemp;

		// extract block from legal description
		String block = "";
		p = Pattern.compile("\\b(BLO?C?K)\\s+([\\dA-Z]+)\\b");
		ma = p.matcher(legal);
		while (ma.find()) {
			block = block + " " + ma.group(2);
			legalTemp = legalTemp.replaceFirst(ma.group(0), ma.group(1) + " ");
		}
		block = block.replaceAll("\\s*&\\s*", " ").trim();
		if (block.length() != 0) {
			block = LegalDescription.cleanValues(block, false, true);
			m.put(PropertyIdentificationSetKey.SUBDIVISION_BLOCK.getKeyName(), block);
		}
		legalTemp = legalTemp.trim().replaceAll("\\s{2,}", " ");
		legal = legalTemp;

		// extract unit from legal description
		String unit = "";
		p = Pattern.compile("\\b(UN)I?T?\\s*([\\d]+)\\b");
		ma = p.matcher(legal);
		if (ma.find()) {
			unit = unit + " " + ma.group(2);
			unit = unit.trim();
			legalTemp = legalTemp.replaceFirst(ma.group(0), ma.group(1) + " ");
			m.put(PropertyIdentificationSetKey.SUBDIVISION_UNIT.getKeyName(), unit);// ma.group(2));
			legalTemp = legalTemp.trim().replaceAll("\\s{2,}", " ");
			legal = legalTemp;
		}

		// extract phase from legal description
		String phase = "";
		p = Pattern.compile("\\b(PH)A?S?E?\\s*([,\\d\\s]+|[A-Z])\\b");
		ma = p.matcher(legal);
		if (ma.find()) {
			phase = phase + " " + ma.group(2);
			phase = phase.trim();
			legalTemp = legalTemp.replaceFirst(ma.group(0), ma.group(1) + " ");
			m.put(PropertyIdentificationSetKey.SUBDIVISION_PHASE.getKeyName(), phase);
			legalTemp = legalTemp.trim().replaceAll("\\s{2,}", " ");
			legal = legalTemp;
		}

		// extract section from legal description
		String sec = "";
		p = Pattern.compile("\\b(SEC)\\s*([-A-Z0-9]+)\\b");
		ma = p.matcher(legal);
		if (ma.find()) {
			sec = sec + " " + ma.group(2);
			sec = sec.trim();
			if (!sec.matches("(?is)TION")) {
				legalTemp = legalTemp.replaceFirst(ma.group(0), ma.group(1));
				m.put(PropertyIdentificationSetKey.SUBDIVISION_SECTION.getKeyName(), sec);
				legalTemp = legalTemp.trim().replaceAll("\\s{2,}", " ");
				legal = legalTemp;
			}
		}

		String tract = "";
		p = Pattern.compile("\\b(TRACT)\\s*([\\dA-Z]+)\\b");
		ma = p.matcher(legal);
		if (ma.find()) {
			tract = tract + " " + ma.group(2);
			tract = tract.trim();
			legalTemp = legalTemp.replaceFirst(ma.group(0), ma.group(1));
			m.put(PropertyIdentificationSetKey.SUBDIVISION_TRACT.getKeyName(), tract);
			legalTemp = legalTemp.trim().replaceAll("\\s{2,}", " ");
			legal = legalTemp;
		}
		
		String bookPagePattern = "\\b(?:ORB|DB|THROUGH)\\s*([\\d]+)\\s+PG\\s*([\\d]+)\\b";
		ResultTable crossRef = new ResultTable();
		@SuppressWarnings("rawtypes")
		List<List> tablebodyRef = new ArrayList<List>();
		List<String> list;
		p = Pattern.compile(bookPagePattern);
		ma = p.matcher(legal);
		while (ma.find()) {
			list = new ArrayList<String>();
			list.add(ma.group(1));
			list.add(ma.group(2));
			tablebodyRef.add(list);
		}
		String[] headerRef = {CrossRefSetKey.BOOK.getShortKeyName(), CrossRefSetKey.PAGE.getShortKeyName()};
		crossRef = GenericFunctions2.createResultTable(tablebodyRef, headerRef);
		if (crossRef != null){
			m.put("CrossRefSet", crossRef);
		}
		
		legalTemp = legalTemp.replaceFirst(bookPagePattern, "ORB");
		legalTemp = legalTemp.trim().replaceAll("\\s{2,}", " ");
		legal = legalTemp;
		
		if (!extractSubd) {
			return;
		}

		String subdiv = "";
		p = Pattern.compile("\\b(?:CIR|WY|DR|HIGHWAY|AVE?|LA|ST|RD|LN|CT|TER|BLVD|ROAD|FLA|PL) (.+?)(?:(\\s+PH\\s+|ORB|\\s+BL\\s+|BLOCK|\\s+UN\\s+|UNIT|LOT|CONDO|PHASE|SEC|TRACT|DB|PG|MIMS))");
		ma = p.matcher(legal);
		if (ma.find()) {
			subdiv = ma.group(1);
			
		} else {
			p = Pattern.compile(".*\\b(?:CIR|WY|DR|HIGHWAY|AVE?|LA|ST|RD|LN|CT|TER|BLVD|ROAD) (.+?) (?:APOLLO ANNEX|BAREFOOT BAY|CAPE CANAVERAL|COCOA|COCOA BEACH|GRANT|INDIALANTIC|INDIAN HARBOUR BEACH|INTERCHANGE SQUARE|MALABAR|MELBOURNE|MELBOURNE BEACH|MELBOURNE VILLAGE|MERRITT ISLAND|MIMS|PALM BAY|PALM BAY WEST|PALM SHORES|PORT CANAVERAL|PORT SAINT JOHN|ROCKLEDGE| SATELLITE BEACH|SCOTTSMOOR|SHARPES|SUNTREE|TITUSVILLE|WEST MELBOURNE)\\b.*");
			ma.usePattern(p);
			ma.reset();
			if (ma.find()) {
				subdiv = ma.group(1);
			}
		}
		
		if (subdiv.length() != 0) {
			subdiv = subdiv.replaceFirst("\\bP.U.D.", "PUD");
			subdiv = subdiv.replaceFirst("\\bNO\\.?\\s+\\d+[A-Z]?\\b", "");
			subdiv = subdiv.replaceFirst("(.+) COND.*", "$1");
			subdiv = subdiv.replaceFirst("(.+) SUB(DIVISION)?.*", "$1");
			subdiv = subdiv.replaceFirst("(.+) SUBD.*", "$1");
			subdiv = subdiv.replaceFirst("(.+) PH(ASE)?.*", "$1");
			subdiv = subdiv.replaceFirst("(.+) LOT(S)?.*", "$1");
			subdiv = subdiv.replaceFirst("(.+) UN(IT).*", "$1");
			subdiv = subdiv.replaceFirst("(.+) (BLK|BLOCK(S)?).*", "$1");
			subdiv = subdiv.replaceFirst("(.+) SUBD.*", "$1");
			subdiv = subdiv.replaceFirst("(.+) RESUBD.*", "$1");
			subdiv = subdiv.replaceFirst("(.+) (ESTATES).*", "$1" + " " + "$2");
			subdiv = subdiv.replaceAll("(.+) UNREC.*", "$1");
			subdiv = subdiv.replaceFirst("\\s*REPLAT\\s*", "");
			subdiv = subdiv.trim().toUpperCase();

			String city = (String) m.get(PropertyIdentificationSetKey.CITY.getKeyName());
			city = org.apache.commons.lang.StringUtils.defaultString(city);
			if (!city.isEmpty()) {
				if (subdiv.equals(city)) {
					subdiv = "";
				} else if (subdiv.endsWith(city)) {
					subdiv = subdiv.replaceFirst(city, "").replaceFirst("\\bTHE\\s*$", "");
				}
			}
			
			m.put(PropertyIdentificationSetKey.SUBDIVISION_NAME.getKeyName(), subdiv);
			if (legal.matches(".*\\b(CONDO(MINIUM)?)\\b.*"))
				m.put(PropertyIdentificationSetKey.SUBDIVISION_COND.getKeyName(), subdiv);
		}
	}
			
	public static void legalIntermFLBrevardTR(ResultMap m, long searchId) throws Exception {
		String descr = (String) m.get(PropertyIdentificationSetKey.PROPERTY_DESCRIPTION.getKeyName());
		if (StringUtils.isEmpty(descr)) {
			return;
		}

		legalFLBrevardTR(m, descr, false);
	}

	public static void parseAddresslFLBrevardTR(ResultMap m, long searchId) {
		String tmpAdr = (String) m.get("tmpAddress");
		if (tmpAdr.contains("BLK") || tmpAdr.contains("BLOCK")) {
			tmpAdr = tmpAdr.replaceFirst("(?is)\\s*\\bBL(?:OC)?K\\b", " ").trim();
		}
		
		if (StringUtils.isNotEmpty(tmpAdr)) {
			Matcher mCity = Pattern.compile("(?is)(.+)\\s+\\b"
					+ "((?:\\w+\\s+)?WEST MELBOURNE(?:\\s+\\w+)?|MELBOURNE(?<!WEST MELBOURNE)(?:\\s+\\w+)?|Titusville|"
					+ "Eau Gallie|Micco|Mims|Sharpes|Rockledge|Valkaria|Barefoot Bay|"
					+ "Cape\\s+\\w+|Cocoa(?:\\s+\\w+)?|Grant(?:\\s*-\\s*\\w+)?|India(?:lantic|n\\s+[\\w\\s]+)|Palm\\s+\\w+|Port\\s+(?:St|Saint)\\s+John|"
					+ "Merritt\\s+\\w+|Satellite\\s+\\w+|South Patrick\\s+\\w+|Whispering\\s+Hills[\\w\\s]+)")
				.matcher(tmpAdr);
			if (mCity.find()) {
				String group1 = mCity.group(1).trim();
				String[] split = group1.split("\\s+");
				int len = split.length;
				if (len>1) {
					String suffix = split[len-1];
					if ((suffix.matches("(?is)[NSEW]+") || suffix.matches("\\d+")) && len > 2) {
						suffix = split[len-2];
					}
					if (Normalize.isSuffix(suffix)) {
						tmpAdr = group1;
						m.put(PropertyIdentificationSetKey.CITY.getKeyName(), mCity.group(2).trim().toUpperCase());
					}
				}
			}
			
			tmpAdr = tmpAdr.trim();
			m.put(PropertyIdentificationSetKey.STREET_NO.getKeyName(), StringFormats.StreetNo(tmpAdr));
			m.put(PropertyIdentificationSetKey.STREET_NAME.getKeyName(), StringFormats.StreetName(tmpAdr));
		}

		return;
	}		

	public static void pisFLBrevardTR(ResultMap m, long searchId) {
		// non real estate properties will have Account No. as parcel ID (instead of GEO No.)
		String propertyType = org.apache.commons.lang.StringUtils.defaultString((String) m.get("tmpTaxType"));
		String accountNo = org.apache.commons.lang.StringUtils.defaultString((String) m.get(PropertyIdentificationSetKey.PARCEL_ID2.getKeyName()));
		if (StringUtils.isNotEmpty(propertyType) && StringUtils.isEmpty(RegExUtils.getFirstMatch("(?i)\\b(Real\\s+Estate)", propertyType, 1))
				&& StringUtils.isNotEmpty(accountNo)) {
			m.put(PropertyIdentificationSetKey.PARCEL_ID.getKeyName(), accountNo);
		}
	}

	public static void pisIntermFLBrevardTR(ResultMap m, long searchId) {
		// non real estate properties will have Account No. as parcel ID (instead of GEO No.)
		String propertyType = org.apache.commons.lang.StringUtils.defaultString((String) m.get(PropertyIdentificationSetKey.PROPERTY_TYPE.getKeyName())).trim();
		String accountNo = org.apache.commons.lang.StringUtils.defaultString((String) m.get(PropertyIdentificationSetKey.PARCEL_ID2.getKeyName()));
		if (propertyType.equalsIgnoreCase("Tangible") && StringUtils.isNotEmpty(accountNo)) {
			m.put(PropertyIdentificationSetKey.PARCEL_ID.getKeyName(), accountNo);
		}
	}
}
