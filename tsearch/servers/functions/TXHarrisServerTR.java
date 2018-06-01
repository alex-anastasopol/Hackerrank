package ro.cst.tsearch.servers.functions;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang.StringEscapeUtils;
import org.htmlparser.tags.TableColumn;
import org.htmlparser.tags.TableRow;

import ro.cst.tsearch.extractor.legal.LegalDescription;
import ro.cst.tsearch.extractor.xml.GenericFunctions;
import ro.cst.tsearch.extractor.xml.GenericFunctions2;
import ro.cst.tsearch.extractor.xml.ResultMap;
import ro.cst.tsearch.extractor.xml.StringFormats;
import ro.cst.tsearch.search.name.NameUtils;
import ro.cst.tsearch.servers.response.OtherInformationSet.OtherInformationSetKey;
import ro.cst.tsearch.servers.response.PropertyIdentificationSet.PropertyIdentificationSetKey;
import ro.cst.tsearch.utils.Roman;
import ro.cst.tsearch.utils.StringUtils;

public class TXHarrisServerTR {

	public static ResultMap parseIntermediaryRow(TableRow row, long searchId, int miServerId) throws Exception {

		ResultMap resultMap = new ResultMap();
		resultMap.put(OtherInformationSetKey.SRC_TYPE.getKeyName(), "TR");
		TableColumn[] cols = row.getColumns();

		if (cols.length == 3) {
			String accountNo = StringEscapeUtils.unescapeHtml(cols[0].toPlainTextString().trim());
			String owner = StringEscapeUtils.unescapeHtml(cols[1].toPlainTextString().trim());
			String address = StringEscapeUtils.unescapeHtml(cols[2].toPlainTextString().trim());

			resultMap.put("tmpOwner", owner);
			resultMap.put("tmpAddress", address);
			resultMap.put(PropertyIdentificationSetKey.PARCEL_ID.getKeyName(), accountNo);

			partyNamesTXHarrisTR(resultMap, searchId);
			parseAddressTXHarrisTR(resultMap, searchId);
		}
		return resultMap;
	}

	@SuppressWarnings("rawtypes")
	public static void partyNamesTXHarrisTR(ResultMap resultMap, long searchId) throws Exception {

		String stringOwner = (String) resultMap.get("tmpOwner");

		if (StringUtils.isEmpty(stringOwner))
			return;

		stringOwner = GenericFunctions2.cleanOwnerNameFromPrefix(stringOwner);
		stringOwner = GenericFunctions2.resolveOtherTypes(stringOwner);

		List<List> body = new ArrayList<List>();
		String[] names = { "", "", "", "", "", "" };
		String[] suffixes = { "", "" }, types = { "", "" }, otherTypes = { "", "" };
		if (NameUtils.isCompany(stringOwner)) {
			names[2] = stringOwner;
		}
		else {
			names = StringFormats.parseNameNashville(stringOwner, true);
			suffixes = GenericFunctions.extractNameSuffixes(names);
			types = GenericFunctions.extractAllNamesType(names);
			otherTypes = GenericFunctions.extractAllNamesOtherType(names);
		}
		GenericFunctions.addOwnerNames(names, suffixes[0], suffixes[1], types, otherTypes,
				NameUtils.isCompany(names[2]), NameUtils.isCompany(names[5]), body);

		GenericFunctions.storeOwnerInPartyNames(resultMap, body, true);

		resultMap.put(PropertyIdentificationSetKey.NAME_ON_SERVER.getKeyName(), stringOwner);
	}

	public static void parseAddressTXHarrisTR(ResultMap resultMap, long searchId) throws Exception {

		String address = (String) resultMap.get("tmpAddress");

		if (StringUtils.isEmpty(address))
			return;

		if (address.trim().matches("0+"))
			return;
		//add zip
		Pattern p = Pattern.compile("(?s).*\\s+(\\d{5})+\\s*$");
		Matcher m = p.matcher(address);
		if(m.find()){
			resultMap.put(PropertyIdentificationSetKey.ZIP.getKeyName(), m.group(1).trim());
		}
		address = address.replaceAll("\\s+\\d{5}+\\s*$", "");// remove zipcode

		address = address.replaceAll("\\b\\d+\\s+(#)\\d+/(\\d+)", "$1$2"); // e.g. Account Number 1153560030002

		resultMap.put(PropertyIdentificationSetKey.STREET_NAME.getKeyName(), StringFormats.StreetName(address.trim()));
		resultMap.put(PropertyIdentificationSetKey.STREET_NO.getKeyName(), StringFormats.StreetNo(address.trim()));

	}

	public static void parseLegalTXHarrisTR(ResultMap resultMap, long searchId) throws Exception {

		String legal = (String) resultMap.get("tmpLegal");

		if (StringUtils.isEmpty(legal))
			return;

		resultMap.put(PropertyIdentificationSetKey.PROPERTY_DESCRIPTION.getKeyName(), legal.replaceAll("\\s+", " "));

		if (legal.contains("Vehicles") || legal.contains("Business Personal Property"))
			return;

		legal = legal.replaceAll("(?is)&quot;", "\"");
		legal = legal.replaceAll("(?is)[\\)\\(]+", "");
		legal = legal.replaceAll("(?is)\\bPER SURVEY\\b", "");
		legal = legal.replaceAll("(?is)\\\"[^\\\"]+\\\"", "");
		legal = legal.replaceAll("(?is)[A-Z]\\s+[/\\d\\sX]+\\s*OF\\s+", "");
		legal = legal.replaceAll("(?is)\\s+(TR(ACT)?)\\b", " , $1");
		legal = legal.replaceAll("(?is)\\s+(PH(ASE)?)\\b", " , $1");
		// W 55 FT OF
		legal = legal.replaceAll("(?is)\\s+[NSEW]{1,2}\\s*\\d+\\s*F\\s*T\\s+OF\\s+", " ");
		// .024598 INT
		legal = legal.replaceAll("(?is)[\\d\\.]+\\s*(INT|AC)\\b", "");
		legal = legal.replaceAll("(?is)[\\(\\)]+", "");
		legal = legal.replaceAll("(?is)(\\d+)\\s*AND\\s*(\\d+)", "$1 & $2");
		legal = legal.replaceAll("(?is)\\bSALES CONTRACT\\b", "");

		legal = GenericFunctions.replaceNumbers(legal);
		String[] exceptionTokens = { "I", "M", "C", "L", "D" };
		legal = Roman.normalizeRomanNumbersExceptTokens(legal, exceptionTokens); // convert roman numbers

		String legalTemp = legal;

		// extract lot from legal description
		String lot = "";
		Pattern p = Pattern.compile("(?is)\\b(LO?TS?\\s*)\\s*([^\\s]+)\\b");
		Matcher ma = p.matcher(legal);
		while (ma.find()) {
			lot = lot + " " + ma.group(2).trim().replaceAll("\\A0+", "");
			legalTemp = legalTemp.replaceFirst(ma.group(0), ma.group(1) + " ");
		}
		lot += " " + LegalDescription.extractLotFromText(legal);
		if (lot.length() != 0) {
			lot = LegalDescription.cleanValues(lot, false, true);
			resultMap.put(PropertyIdentificationSetKey.SUBDIVISION_LOT_NUMBER.getKeyName(), lot);
		}
		legalTemp = legalTemp.trim().replaceAll("\\s{2,}", " ");
		legal = legalTemp;

		// extract block from legal description
		String block = "";
		p = Pattern.compile("(?is)\\b(BLKS?\\s*)\\s*([^\\s]+)\\b");
		ma = p.matcher(legal);
		while (ma.find()) {
			block = block + " " + ma.group(2).trim().replaceAll("\\A0+", "");
			legalTemp = legalTemp.replaceFirst(ma.group(0), ma.group(1) + " ");
		}
		block = block.replaceAll("\\s*&\\s*", " ").replaceAll("\\bOF\\s+\\d+", "").trim();
		// 19035.038.013.10
		block = block.replaceAll("(\\d/\\d)$", "");

		if (block.length() != 0) {
			block = LegalDescription.cleanValues(block, false, true);
			resultMap.put(PropertyIdentificationSetKey.SUBDIVISION_BLOCK.getKeyName(), block);
		}
		legalTemp = legalTemp.trim().replaceAll("\\s{2,}", " ");
		legal = legalTemp;

		p = Pattern.compile("(?is)\\b(A\\s*-|Abst|AB\\s+)\\s*(\\d+)\\b");
		ma = p.matcher(legal);
		if (ma.find()) {
			resultMap.put(PropertyIdentificationSetKey.ABS_NO.getKeyName(), ma.group(2).replaceAll("\\s*&\\s*", " ").trim());
			legal = legal.replaceAll(ma.group(2), "");
			legalTemp = legalTemp.replaceFirst(ma.group(0), ma.group(1) + " ");
			legalTemp = legalTemp.trim().replaceAll("\\s{2,}", " ");
			legal = legalTemp;
		}

		p = Pattern.compile("(?is)\\b(Acres)\\s*([^,]+)\\b");
		ma = p.matcher(legal);
		if (ma.find()) {
			resultMap.put(PropertyIdentificationSetKey.ACRES.getKeyName(), ma.group(2).replaceAll("\\s*&\\s*", " ").trim());
			legal = legal.replaceAll(ma.group(2), "");
			legalTemp = legalTemp.replaceFirst(ma.group(0), ma.group(1) + " ");
			legalTemp = legalTemp.trim().replaceAll("\\s{2,}", " ");
			legal = legalTemp;
		}

		// extract unit from legal description
		String unit = "";
		p = Pattern.compile("(?is)\\b(UNIT)\\s+([\\d]+[A-Z]?|[A-Z])\\b");
		ma = p.matcher(legal);
		while (ma.find()) {
			unit = unit + " " + ma.group(2);
			unit = unit.trim();
			legalTemp = legalTemp.replaceFirst(ma.group(0), ma.group(1) + " ");
			resultMap.put(PropertyIdentificationSetKey.SUBDIVISION_UNIT.getKeyName(), unit);// ma.group(2));
			legalTemp = legalTemp.trim().replaceAll("\\s{2,}", " ");
			legal = legalTemp;
		}

		// extract unit from legal description
		String phase = "";
		p = Pattern.compile("(?is)\\b(PH)\\s+([\\d]+[A-Z]?)\\b");
		ma = p.matcher(legal);
		while (ma.find()) {
			phase = phase + " " + ma.group(2);
			phase = phase.trim();
			legalTemp = legalTemp.replaceFirst(ma.group(0), ma.group(1) + " ");
			resultMap.put(PropertyIdentificationSetKey.SUBDIVISION_PHASE.getKeyName(), phase);// ma.group(2));
			legalTemp = legalTemp.trim().replaceAll("\\s{2,}", " ");
			legal = legalTemp;
		}

		// extract section from legal description
		p = Pattern.compile("(?is)\\b(SEC?(?:TION)?)\\s+(\\d+)\\b");
		ma = p.matcher(legal);
		if (ma.find()) {
			resultMap.put(PropertyIdentificationSetKey.SUBDIVISION_SECTION.getKeyName(), ma.group(2));
			legalTemp = legalTemp.replaceFirst(ma.group(0), ma.group(1) + " ");
			legalTemp = legalTemp.trim().replaceAll("\\s{2,}", " ");
			legal = legalTemp;
		}

		// extract tract from legal description
		p = Pattern.compile("(?is)\\b(TR(?:ACT)?S?)\\s+(\\d+[A-Z]?(?:[\\d-]+)?(?:\\s*&\\s*\\d+[A-Z]?)?)\\b");
		ma = p.matcher(legal);
		if (ma.find()) {
			resultMap.put(PropertyIdentificationSetKey.SUBDIVISION_TRACT.getKeyName(), ma.group(2).replaceAll("\\s*&\\s*", " "));
			legalTemp = legalTemp.replaceFirst(ma.group(0), ma.group(1) + " ");
			legalTemp = legalTemp.trim().replaceAll("\\s{2,}", " ");
			legal = legalTemp;
		}

		// extract building #
		String bldg = "";
		p = Pattern.compile("(?is)\\b(BLDG)\\s+([A-Z]|\\d+)\\b");
		ma = p.matcher(legal);
		if (ma.find()) {
			bldg = bldg + " " + ma.group(2);
			bldg = bldg.trim();
			legalTemp = legalTemp.replaceFirst(ma.group(0), ma.group(1) + " ");
			resultMap.put(PropertyIdentificationSetKey.SUBDIVISION_BLDG.getKeyName(), bldg);
			legalTemp = legalTemp.trim().replaceAll("\\s{2,}", " ");
			legal = legalTemp;
		}

		// extract subdivision name from legal description
		String subdiv = "";

		p = Pattern.compile("(?is)\\b(BLK)\\b(.*)\\b");
		ma = p.matcher(legal);
		if (ma.find()) {
			subdiv = ma.group(2);
		} else {
			p = Pattern.compile("(?is)\\b(LT|ABST|TR|BLDG)\\b(.*)\\b");
			ma = p.matcher(legal);
			if (ma.find()) {
				subdiv = ma.group(2);
			} else {
				p = Pattern.compile("(?is)\\A(.*)\\b(SEC)\\b");
				ma = p.matcher(legal);
				if (ma.find()) {
					subdiv = ma.group(1);
				}
			}
		}

		if (subdiv.length() == 0) {
			Matcher matcher = Pattern.compile("(?is)\\A\\d{4}(?:EST)?(.*?)\\d+X\\d+.*").matcher(legal); // e.g. Account Number 6000000885814 or 6000000926194
			if (matcher.find()) {
				subdiv = matcher.group(1).trim();
			}
		}

		subdiv = subdiv.replaceAll("(\\A|\\s).*?\\d+-\\d+.*?\\s", ""); // e.g. Account Number 0820350000235 or 1105240000013
		subdiv = subdiv.trim();
		if (subdiv.length() > 0) {

			subdiv = subdiv.replaceFirst("(?is)\\bSEC\\b.*", "");
			subdiv = subdiv.replaceFirst("(?is)\\bUNIT.*", "");
			resultMap.put(PropertyIdentificationSetKey.SUBDIVISION_NAME.getKeyName(), subdiv);

			if (legal.matches(".*\\bCOND.*"))
				resultMap.put(PropertyIdentificationSetKey.SUBDIVISION_COND.getKeyName(), subdiv);
		}
	}
}
