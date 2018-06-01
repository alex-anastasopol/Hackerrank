package ro.cst.tsearch.servers.functions;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang.StringUtils;

import ro.cst.tsearch.extractor.legal.LegalDescription;
import ro.cst.tsearch.extractor.xml.GenericFunctions;
import ro.cst.tsearch.extractor.xml.GenericFunctions1;
import ro.cst.tsearch.extractor.xml.ResultMap;
import ro.cst.tsearch.extractor.xml.StringFormats;
import ro.cst.tsearch.search.name.NameUtils;
import ro.cst.tsearch.servers.response.PropertyIdentificationSet.PropertyIdentificationSetKey;
import ro.cst.tsearch.utils.Roman;

public class KSJohnsonAO {
	public static void parseNames(ResultMap m, List<String> all_names, String auxString) {
		try {
			if (all_names == null)
				return;
			if (all_names.size() == 0)
				return;

			@SuppressWarnings("rawtypes")
			ArrayList<List> body = new ArrayList<List>();

			StringBuffer nameOnServer = new StringBuffer();

			for (String n : all_names) {
				n = n.toUpperCase().trim().replaceAll("\\s+", " ");

				if (StringUtils.isNotEmpty(n)) {
					nameOnServer.append(n + " & ");
					String[] names = new String[] { "", "", "", "", "", "" };
					names = StringFormats.parseNameNashville(n, true);
					String[] type = GenericFunctions.extractAllNamesType(names);
					String[] otherType = GenericFunctions.extractAllNamesOtherType(names);
					String[] suffixes = GenericFunctions.extractNameSuffixes(names);

					GenericFunctions.addOwnerNames(names, suffixes[0], suffixes[1], type, otherType, NameUtils.isCompany(names[2]),
							NameUtils.isCompany(names[5]), body);
				}
			}
			m.put(PropertyIdentificationSetKey.NAME_ON_SERVER.getKeyName(), nameOnServer.toString().trim().replaceAll("\\&$", "").replace("_", "/"));
			if (body.size() > 0)
				GenericFunctions.storeOwnerInPartyNames(m, body, true);

		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public static void parseLegal(ResultMap m, long searchId) {
		try {
			String legal = (String) m.get("tmpLegal");
			if (StringUtils.isEmpty(legal))
				return;
			legal = legal.replaceAll("\\s*THROUGH\\s*", "-");
			legal = legal.replaceAll("\\s*THRU\\s*", "-");
			legal = legal.replaceAll("\\b[\\d\\.]+\\s+FT\\b", "");
			legal = legal.replaceAll("\\bREPLAT OF PART OF\\b", "");

			legal = legal.replaceAll("[\\(\\)]+", "");
			legal = legal.replaceAll("(\\d+)\\s*TO\\s*(\\d+)", "$1-$2");

			legal = legal.replaceAll("(?ism)\\b[A-Z] \\d+'\\s", " ").replaceAll("\\s+", " ");

			// initial corrections and cleanup of legal description
			legal = GenericFunctions1.replaceNumbers(legal);
			String[] exceptionTokens = { "I", "M", "C", "L", "D" };
			legal = Roman.normalizeRomanNumbersExceptTokens(legal, exceptionTokens);

			String legalTemp = legal;

			// extract lot from legal description
			String lot = ""; // can have multiple occurrences
			Pattern p = Pattern.compile("\\b(LTS?|LOTS?)\\s*([\\d+\\s-&]+|[A-Z]|\\d+[A-Z])\\b");
			Matcher ma = p.matcher(legal);
			while (ma.find()) {
				lot = lot + " " + ma.group(2);
			}
			lot = lot.trim();
			if (lot.length() != 0) {
				lot = LegalDescription.cleanValues(lot, false, true);
				lot = lot.replaceAll("\\A\\s*&\\s+", "");
				m.put(PropertyIdentificationSetKey.SUBDIVISION_LOT_NUMBER.getKeyName(), lot.trim());
			}
			legalTemp = legalTemp.trim().replaceAll("\\s{2,}", " ");
			legal = legalTemp;

			// extract block from legal description
			String block = "";
			p = Pattern.compile("(?is)\\b(BLKS?|BLOCKS?)\\s+([\\d-]+)\\b");
			ma = p.matcher(legal);
			while (ma.find()) {
				block = block + " " + ma.group(2);
			}
			block = block.trim();
			if (block.length() != 0) {
				block = block.replaceAll("\\s*,\\s*", " ");
				block = LegalDescription.cleanValues(block, false, true);
				m.put(PropertyIdentificationSetKey.SUBDIVISION_BLOCK.getKeyName(), block);
			}
			legalTemp = legalTemp.trim().replaceAll("\\s{2,}", " ");
			legal = legalTemp;

			// extract phase from legal description
			String phase = "";
			p = Pattern.compile("(?is)\\b(PH(?:ASE)?)\\s+(\\d+|\\d?[A-Z])\\b");
			ma = p.matcher(legal);
			while (ma.find()) {
				phase = ma.group(2).replaceAll("\\s*,\\s*", " ");
				legalTemp = legalTemp.replace(ma.group(0), "");
				m.put(PropertyIdentificationSetKey.SUBDIVISION_PHASE.getKeyName(), phase.trim());
			}
			legalTemp = legalTemp.trim().replaceAll("\\s{2,}", " ");
			legal = legalTemp;

			// extract unit from legal description
			String unit = "";
			p = Pattern.compile("(?is)\\b(UNIT)\\s+(\\d+)\\b");
			ma = p.matcher(legal);
			while (ma.find()) {
				legalTemp = legalTemp.replace(ma.group(1), "");
				unit = unit + " " + ma.group(2);
				unit = ma.group(2).replaceAll("\\s*,\\s*", " ");
				m.put(PropertyIdentificationSetKey.SUBDIVISION_UNIT.getKeyName(), unit.trim());
			}
			legalTemp = legalTemp.trim().replaceAll("\\s{2,}", " ");
			legal = legalTemp;

			// extract building #
			p = Pattern.compile("(?is)\\b(BLDG|BUILDING)\\s+([\\dA-Z-]+)\\b");
			ma = p.matcher(legal);
			if (ma.find()) {
				legalTemp = legalTemp.replace(ma.group(1), "");
				m.put(PropertyIdentificationSetKey.SUBDIVISION_BLDG.getKeyName(), ma.group(2).trim());
			}
			legalTemp = legalTemp.trim().replaceAll("\\s{2,}", " ");
			legal = legalTemp;

			// extract tract #
			p = Pattern.compile("(?is)\\b(TR|TRACT)\\s+(\\d+|[A-Z]+)\\b");
			ma = p.matcher(legal);
			if (ma.find()) {
				legalTemp = legalTemp.replace(ma.group(), ma.group(1));
				m.put(PropertyIdentificationSetKey.SUBDIVISION_TRACT.getKeyName(), ma.group(2).trim());
			}
			legalTemp = legalTemp.trim().replaceAll("\\s{2,}", " ");
			legal = legalTemp;

			// extract plat b&p
			p = Pattern.compile("(?is)\\b(BK)\\s+(\\d+)\\s+P\\s+(\\d+)\\b");
			ma = p.matcher(legal);
			if (ma.find()) {
				m.put(PropertyIdentificationSetKey.PLAT_BOOK.getKeyName(), ma.group(2).trim());
				m.put(PropertyIdentificationSetKey.PLAT_NO.getKeyName(), ma.group(3).trim());
				legal = legal.replaceFirst("\\b" + ma.group(0) + "\\b", "");
				legal = legal.trim().replaceAll("\\s{2,}", " ");
			}

			legal = legal.replaceAll("(?is)\\b(LO?TS?)(\\w+)", "$1 $2"); // B4157
			if (legal.contains("LOT") && legal.contains("LT")){
				legal = legal.replaceAll("(?is)\\b(LOTS?)\\s+(\\w+)", "");
			}
			if (legal.contains("BLOCK") && legal.contains("BLK")){
				legal = legal.replaceAll("(?is)\\b(BLOCKS?)\\s+(\\w+)", "");
			}
			// String subdiv = "";
			// p = Pattern.compile("(?is)(.+?)\\s+(TR|TRACT|LOTS?|LTS?|BLKS?|BLOCKS?|UNIT)\\b");
			// ma = p.matcher(legal);
			// if (ma.find()) {
			// subdiv = ma.group(1);
			// }
			// subdiv = subdiv.trim();
			// subdiv = subdiv.replaceAll("(?is)(.+) CERT(?:IFICATE)?.*", "$1");
			// subdiv = subdiv.replaceAll("(.+)\\s+ADD(ITION)?.*", "$1");
			// subdiv = subdiv.replaceAll("(?is)(.+)\\s*(\\d+\\s*[ST|ND|RD|TH]).*", "$1");
			// m.put(PropertyIdentificationSetKey.SUBDIVISION_NAME.getKeyName(), subdiv);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	public static void parseAddress(ResultMap m, String addr) {
		addr = StringUtils.strip(StringUtils.defaultString(addr));

		if (StringUtils.isEmpty(addr))
			return;

		m.put(PropertyIdentificationSetKey.ADDRESS_ON_SERVER.getKeyName(), addr.replaceAll("\\s+", " "));

		String newAddr = addr;

		m.put(PropertyIdentificationSetKey.STREET_NO.getKeyName(), StringUtils.defaultString(StringFormats.StreetNo(newAddr)));
		m.put(PropertyIdentificationSetKey.STREET_NAME.getKeyName(), StringUtils.defaultString(StringFormats.StreetName(newAddr)));
	}
}
