package ro.cst.tsearch.servers.functions;

import java.util.ArrayList;
import java.util.List;
import java.util.Vector;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import ro.cst.tsearch.extractor.xml.GenericFunctions;
import ro.cst.tsearch.extractor.xml.GenericFunctions2;
import ro.cst.tsearch.extractor.xml.ResultMap;
import ro.cst.tsearch.extractor.xml.ResultTable;
import ro.cst.tsearch.extractor.xml.StringFormats;
import ro.cst.tsearch.search.name.FirstNameUtils;
import ro.cst.tsearch.search.name.LastNameUtils;
import ro.cst.tsearch.search.name.NameCleaner;
import ro.cst.tsearch.search.name.NameFactory;
import ro.cst.tsearch.search.name.NameUtils;
import ro.cst.tsearch.servers.response.PropertyIdentificationSet.PropertyIdentificationSetKey;
import ro.cst.tsearch.utils.StringUtils;

public class FLFranklinTR extends ParseClass {

	public static int counter = 0;

	public static Pattern doubleSuffix = Pattern.compile("\\b(" + GenericFunctions.nameSuffixString + ")\\s*&\\s*("
			+ GenericFunctions.nameSuffixString + ")$");
	public static Pattern endsWithWordInitial = Pattern.compile(".*&\\s*\\w{2,}\\s+\\w");
	public static Pattern twoWords = Pattern.compile("\\w+\\s+\\w+(\\s" + GenericFunctions.nameSuffixString + ")?(\\s*&)?");
	public static Pattern twoWordsNoInitial = Pattern.compile("\\w{2,}\\s+\\w{2,}(\\s" + GenericFunctions.nameSuffixString + ")?(\\s*&)?");
	public static Pattern oneWord = Pattern.compile("(& )?(\\w+)(\\s+\\w{1})?(\\s+(FOR|&|!!!!!))?");
	public static Pattern oneWordSuffix = Pattern.compile("(\\w+)\\s" + GenericFunctions.nameSuffixString);
	public static Pattern initialWord = Pattern.compile("\\w{1}\\s\\w+");
	public static Pattern wordInitial = Pattern.compile("\\w{2,}\\s\\w{1}");
	public static Pattern initialWordSuffix = Pattern.compile("\\w{1}\\s\\w+(\\s" + GenericFunctions.nameSuffixString + ")?");
	public static Pattern initialWordSuffix2 = Pattern.compile("(\\w+(\\s" + GenericFunctions.nameSuffixString + ") )&(.*)");
	public static Pattern fourWords = Pattern.compile("(\\w+)\\s(\\w+)\\s(\\w+)\\s(\\w+)");
	public static Pattern threefourWords = Pattern.compile("(\\w+)(\\s\\w+)?\\s(\\w+)\\s(\\w+)");
	public static Pattern husbandWife = Pattern.compile("(\\w+)\\s\\w+\\s&\\s(\\w+)\\s(\\w+)");
	public static Pattern companySuffixProblem = Pattern.compile("(LLC|INC)\\s*/");

	public static Pattern threeWords = Pattern.compile("(\\w+)\\s(\\w+)\\s(\\w+)");

//	public static void partyNamesFranklinTR(ResultMap m) throws Exception {
//		int i;
//		String owner = (String) m.get("tmpOwnerInfo");
//		if (StringUtils.isEmpty(owner)) {
//			return;
//		}
//		ArrayList<String> lines = new ArrayList<String>();
//		boolean addressCleaned = false;
//
//		//30-07S-04W-0000-0010-0010
//		owner = owner.replaceAll("(?is)\\s+", " ").replaceAll("(?is)&\\s+([A-Z]+)\\s*@@\\s*(LLC\\s*@@)", "@@$1 $2  @@");
//		
//		String oldOwner = owner;
//		owner = cleanFullOwner(owner);
//		addressCleaned = !oldOwner.equals(owner);
////		addressCleaned = true;
//		String[] lTmp = owner.split("@@");
//		for (String s : lTmp) {
//			lines.add(preCleanName(s));
//		}
//
//		String prevLastName = "";
//
//		Vector<String> excludeCompany = new Vector<String>();
//		excludeCompany.add("PAT");
//		excludeCompany.add("ST");
//
//		Vector<String> extraCompany = new Vector<String>();
//		extraCompany.add("GEORGIA");
//		extraCompany.add("GENERAL");
//		extraCompany.add("BUILDINGS");
//		extraCompany.add("A");
//		extraCompany.add("PLAN");
//		extraCompany.add("FLORIDA");
//		extraCompany.add("KORINEK");
//
//		Vector<String> extraInvalid = new Vector<String>();
//		extraInvalid.add("\\d+.*(WAY).*");
//		extraInvalid.add("FOR DISABLED PERSON");
//		extraInvalid.add("^\\d+.*(DRIVE|STREET|COURT|TRAIL)$");
//		extraInvalid.add(".*\\bPO BOX\\b.*");
//		extraInvalid.add("WIFE");
//		extraInvalid.add("AVENUE D");
//
//		List<List> body = new ArrayList<List>();
//		if (!addressCleaned && lines.size() <= 2)
//			return;
//		// clean address out of mailing address
//
//		ArrayList<String> lines2 = new ArrayList<String>();
//		if (addressCleaned) {
//			lines2 = mergeCompanyName(lines, extraInvalid, extraCompany);
//		} else {
//			int minAddressLines = 2;
//			if (lines.size()>=2) {
//				if (lines.get(lines.size()-2).matches("(?i).*?c/(o|0).*")) {
//					minAddressLines = 1;
//				}
//			}
//			lines2 = GenericFunctions.removeAddressFLTR2(lines, excludeCompany, extraCompany, extraInvalid, minAddressLines, 30);
//			for (int j=0;j<lines2.size();j++) {
//				String s = lines2.get(j);
//				if (s.matches("(?i).*?\\s+LIFE\\s+ESTATE\\s*$")) {
//					lines2.add(j+1, s.replaceFirst("\\s+LIFE\\s+ESTATE\\s*$", ""));
//				}
//			}
//		}
//
//		if (lines2.size() == 2) {
//			String line1, line2;
//			line1 = lines2.get(0);
//			line2 = lines2.get(1);
//			String[] splits1 = line1.split(" ");
//			String[] splits2 = line2.split(" ");
//			// JOHNSON DRYWALL & PAINTING\nINC/JOHNSON CARL
//			Matcher ma = companySuffixProblem.matcher(line2);
//			if (ma.find()) {
//				lines2.set(0, line1 + " " + ma.group(1));
//				lines2.set(1, ma.replaceFirst("/")); // this is LFM
//			} else if (initialWordSuffix.matcher(line2).matches()
//			// UNELL BONNIE M & DOUGLAS\nG BABCOCK
//					|| (oneWord.matcher(line2).matches()
//					/* && StringUtils.indexesOf(line1, "&").size() <=1 */)
//					// SMITH DAROLD & GAIL HANCOCK\nSMITH
//					|| oneWordSuffix.matcher(line2).matches()
//					// SMITH PEGGY LOU MCNEILL &\nALTO FRANK SMITH
//					|| (line1.endsWith("&") && splits1[0].equals(splits2[splits2.length - 1]) && StringUtils.indexesOf(line1, "&").size() == 1)) {
//				String join = " & ";
//				if (lines2.get(0).contains("&")) {
//					join = " ";
//				} else if (line1.endsWith("_")) {
//					join = "";
//				}
//				lines2.set(0, lines2.get(0) + join + lines2.get(1));
//				lines2.remove(1);
//			} else if (GenericFunctions.FMiLFind.matcher(line2).find()
//					|| (line1.endsWith("&") && line2.matches("\\w{2,}( \\w{1})? ?& ?\\w{2,}( \\w{1})?"))) {
//				String[] ss = line2.split("\\s*&\\s*");
//				if (ss.length == 2) {
//					lines2.add(ss[1]);
//					lines2.set(1, ss[0]);
//				}
//			}
//		}
//		/*
//		 * if(lines2.size()!=2 ||
//		 * !lines2.get(1).matches("\\w{2,} \\w \\w{2,}\\s*&.*")){ return; }
//		 */
//		String tmpSS = cleanOwnerFLFranklinTR(lines2.get(lines2.size() - 1));
//		if (StringUtils.indexesOf(tmpSS.replaceFirst("^&", "").replaceFirst("&$", "").trim(), "&").size() > 1) {
//			String[] splits = tmpSS.split("\\s*&\\s*");
//			lines2.set(lines2.size() - 1, splits[0]);
//			for (i = 1; i < splits.length; i++) {
//				if (!StringUtils.isEmpty(splits[i]))
//					lines2.add(splits[i]);
//			}
//		}
//		/*
//		 * for(i = 0; i< lines2.size(); i++){ if
//		 * (lines2.get(i).matches("\\w{2,} \\w{2,}\\s*&\\s*\\w{2,} \\w{2,}")){
//		 * break; } } if (i==lines2.size()){ return; }
//		 */
//		// counter++;
//		for (i = 0; i < lines2.size(); i++) {
//			String[] names = { "", "", "", "", "", "" };
//			boolean isCompany = false;
//			// 1 = "L, FM & WFWM"
//			// 2 = "F M L"
//			// 3 = "F & WF L
//			int nameFormat = 1;
//			// C/O - I want it before clean because I don't clean company names
//			String ln = lines2.get(i);
//			if (ln.matches("(?i).*?c/(o|0).*") || ln.matches("^\\s*(%|AKA |C/A).*")) {
//				ln = ln.replaceAll("(?i)c/(o|0)\\s*", "");
//				ln = ln.replaceFirst("^\\s*(%|AKA |C/A )\\s*", "");
//				nameFormat = 2;
//			}
//			
//			if (ln.matches("(?i).*?TUW\\s*$")) {
//				ln = ln.replaceFirst("(?i)TUW\\s*$", "");
//				nameFormat = 2;
//			}
//			
//			ln = ln.replaceAll("(?i)\\b(FORREST)\\s+(BEADNELL)\\b", "$2 $1");
//			
//			String curLine = NameCleaner.cleanNameAndFix(ln, new Vector<String>(), true);
//
//			curLine = cleanOwnerFLFranklinTR(curLine);
//			/*
//			 * Vector<Integer> ampIndexes = StringUtils.indexesOf(curLine, '&');
//			 * Vector<Integer> commasIndexes = StringUtils.indexesOf(curLine,
//			 * ',');
//			 */
//			if (NameUtils.isCompany(curLine, excludeCompany, true)) {
//
//				// this is not a typo, we don't know what to clean in companies'
//				// names
//				names[2] = cleanCompanyName(ln);
//				isCompany = true;
//			} else {
//				if (i > 0) {
//					Vector<Integer> ampIndexes = StringUtils.indexesOf(lines2.get(0), "&");
//					// TURNER REDDICK DALE & ROBERT D TURNER
//					if (GenericFunctions.FMiL.matcher(curLine).matches()) {
//						nameFormat = 2;
//					} else {
//						String[] sss = curLine.split("\\s+");
//						if (ampIndexes.size() > 0 && ampIndexes.get(ampIndexes.size() - 1) + 1 == lines2.get(0).length()) {
//							ampIndexes.remove(ampIndexes.size() - 1);
//						}
//						if (sss.length == 2 && ampIndexes.size() > 0 && NameFactory.getInstance().isLast(sss[1])) {
//							nameFormat = 2;
//						}
//
//					}
//					if (lines2.get(i).startsWith("&")) {
//						ampIndexes = StringUtils.indexesOf(lines2.get(i).replaceAll("\\bAND\\b", "&"), "&");
//						if (ampIndexes.size() > 1) {
//							nameFormat = 3;
//						} else {
//							nameFormat = 2;
//						}
//					}
//				}
//				if (!prevLastName.equals("") && (oneWordSuffix.matcher(curLine).matches() || oneWord.matcher(curLine).matches())) {
//					curLine = prevLastName + " " + curLine;
//				}
//				if (GenericFunctions.FWFL2.matcher(curLine).matches()) {
//					nameFormat = 3;
//				}
//				/*
//				 * if (i == 1 && !curLine.matches("\\w{2,} \\w{2,} \\w{2,}")){
//				 * return;
//				 * 
//				 * }
//				 */
//				switch (nameFormat) {
//				case 1:
//					names = StringFormats.parseNameNashville(curLine, true);
//					break;
//				case 2:
//					names = StringFormats.parseNameDesotoRO(curLine,true);
//					break;
//				case 3:
//					names = StringFormats.parseNameFMWFWML(curLine, excludeCompany, true);
//					break;
//				}
//			}
//			if (StringUtils.isNotEmpty(names[3]) && StringUtils.isNotEmpty(names[5])){
//				if (FirstNameUtils.isFirstName(names[5]) && LastNameUtils.isLastName(names[3])){
//					String xyz = names[3];
//					names[3] = names[5];
//					names[5] = xyz;
//				}
//			}
//
//			String[] suffixes = { "", "" }, type = { "", "" }, otherType = { "", "" };
//			if (!isCompany) {
//				names = NameCleaner.tokenNameAdjustment(names);
//				suffixes = GenericFunctions.extractAllNamesSufixes(names);
//				
//				names = NameCleaner.removeUnderscore(names);
//				
//				if (names[5].length() == 0) {
//					prevLastName = names[2];
//				} else {
//					prevLastName = names[5];
//				}
//			}
//			type = GenericFunctions.extractAllNamesType(names);
//			otherType = GenericFunctions.extractAllNamesOtherType(names);
//			
//			GenericFunctions.addOwnerNames(names, suffixes[0], suffixes[1], type, otherType,
//					NameUtils.isCompany(names[2]), NameUtils.isCompany(names[5]), body);
//		}
//		m.remove(PropertyIdentificationSetKey.OWNER_FIRST_NAME.getKeyName());
//		m.remove(PropertyIdentificationSetKey.OWNER_MIDDLE_NAME.getKeyName());
//		m.remove(PropertyIdentificationSetKey.OWNER_LAST_NAME.getKeyName());
//		m.remove(PropertyIdentificationSetKey.SPOUSE_FIRST_NAME.getKeyName());
//		m.remove(PropertyIdentificationSetKey.SPOUSE_MIDDLE_NAME.getKeyName());
//		m.remove(PropertyIdentificationSetKey.SPOUSE_LAST_NAME.getKeyName());
//		try {
//			GenericFunctions.storeOwnerInPartyNames(m, body, true);
//		} catch (Exception e) {
//			e.printStackTrace();
//		}
//		/*
//		 * done multiowner parsing
//		 */
//
//	}

//	public static String preCleanName(String s) {
//		s = s.replaceFirst(" AS$", "");
//		s = s.replaceAll("\\b(A/F/A|A/K/A)\\b", "&");
//		s = s.replaceAll("\\bOR\\b", "");
//		s = s.replaceAll("\\s*\\*\\s*", "&");
//		s = s.replaceAll("\\bENTP\\b", "");
//		s = s.replaceFirst("\\s*& (W(IFE)?|HUSB)$", "");
//		s = s.replaceFirst(", ?(" + GenericFunctions.nameSuffixString + ")", " $1");
//		s = s.replaceAll("\\bH ?& ?W\\b", "");
//		s = s.replaceAll("\\b(AS )?(CO(-| ))?TRUS?TEES\\b( OF)?", "TRUSTEES ");
//		s = s.replaceAll("\\b(AS )?(CO(-| ))?TRUS?TEE\\b( OF)?", "TRUSTEE ");
//		s = s.replaceAll("\\b(ET)\\s*(AL|UX|VIR)\\b", "$1$2");
//		s = s.replaceAll(" AS CUSTODIAN FOR", "");
//		s = s.replaceAll("\\s*\\.\\s*", " ");
//		s = s.replaceAll("\\b(AND|ADN)\\b", "&");
//		// s = s.replaceAll(",", "&");
////		s = s.replaceFirst("^AS TRUSTEE OF ", "");
//		s = s.replaceFirst("\\s*\\d/\\d( INT(EREST)?)", "");
//		s = s.replaceFirst("\\bHEIR(S)?( OF)?\\b", "");
//		s = s.replaceAll("\\s{2,}", " ");
//		s = s.replaceAll("\\bATTN:?", "");
//		s = s.replaceAll("(?i)\\s*-\\s*NRRE\\b\\s*$", "");
//		return s.trim();
//	}

//	public static String cleanOwnerFLFranklinTR(String s) {
//		s = s.replaceAll("\\s*/\\s*", "");
//		s = s.replaceAll("^&\\s*", "");
//		s = s.replaceAll(",", "&");
//		s = s.replaceAll("\\b111\\b", "");
//		s = s.replaceAll("\\bDR\\b", "");
//		s = s.replaceAll("\\bATTN:?", "");
//
//		return s.trim();
//	}

//	public static String cleanFullOwner(String s) {
//		s = s.replaceFirst("\\bAS( JOINT)? TENANTS.*", "");
//		s = s.replaceAll("\\b(AS TO )?A UNDIVIDED.*", "");
//		s = s.replaceAll("\\b(AS )?JOINT(S)? TEN.*", "");
//		s = s.replaceAll("\\bCONTRACT FOR DEED.*", "");
//		s = s.replaceAll("\\bONE HALF INTEREST.*", "");
//		s = s.replaceAll("\\bAS SUCCESSOR.*", "");
//		s = s.replaceAll("\\bA JOINT WROS.*", "");
//		s = s.replaceAll("\\bTENANTS IN COMMON.*", "");
//		s = s.replaceAll("\\b(AN )?UNDIVIDED.*", "");
//		s = s.replaceAll("\\bAS JOINT WITH.*", "");
//		s = s.replaceAll("\\bWITH RIGHTS OF SURVIVORSHIP.*", "");
//		s = s.replaceAll("\\bWROS TEN.*", "");
//		s = s.replaceAll("\\bAS TENANT.*", "");
//
//		return s;
//	}

//	public static String cleanCompanyName(String s) {
////		s = s.replaceAll("\\b& ETAL\\b", "");
//		s = s.replaceAll("\\s{2,}", " ");
//		return s.trim();
//	}

//	public static ArrayList<String> mergeCompanyName(ArrayList<String> a, Vector<String> extraInvalid, Vector<String> extraWords) {
//		// remove ATTN
//		// merge split names
//		ArrayList<String> tempStr = new ArrayList<String>();
//		int j = 0;
//		for (int i = 0; i < a.size(); i++) {
//			if (!NameCleaner.isValidName(a.get(i), extraInvalid)) {
//				j++;
//			} else {
//				boolean merge = false;
//				// the merge needs improvements
//				if (i >= 1) {
//					merge = NameUtils.isCompanyNamesOnly(a.get(i), extraWords);// company
//																				// names
//																				// split
//																				// on
//																				// multiple
//																				// rows
//				}
//				if (merge) {
//					tempStr.set(i - j - 1, tempStr.get(i - j - 1) + " " + a.get(i));
//					j++;
//				} else {
//					tempStr.add(i - j, a.get(i));
//				}
//			}
//		}
//		return tempStr;
//	}

	public static void taxFLFranklinTR(ResultMap m, long searchId) throws Exception {
		// if there is no amount paid try and compute it from installments
		String amountPaid = (String) m.get("TaxHistorySet.AmountPaid");
		double amtPaid = 0;
		if (amountPaid != null) {
			try {
				amtPaid = Double.parseDouble(amountPaid);
				if (amtPaid != 0) {
					return;
				}
			} catch (NumberFormatException e) {
				e.printStackTrace();
			}
		}
		ResultTable installments = (ResultTable) m.get("TaxHistorySet");
		if (installments == null) {
			return;
		}

		String[] paid = installments.getColumn("Paid", "");
		for (int i = 0; i < paid.length; i++) {
			try {
				double amt = Double.parseDouble(paid[i]);
				amtPaid += amt;
			} catch (NumberFormatException e) {
				e.printStackTrace();
			}
		}

		m.put("TaxHistorySet.AmountPaid", Double.toString(amtPaid));
	}

}
