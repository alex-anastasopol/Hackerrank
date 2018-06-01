package ro.cst.tsearch.servers.functions;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang.StringUtils;
import org.htmlparser.Node;
import org.htmlparser.tags.TableColumn;
import org.htmlparser.tags.TableRow;

import ro.cst.tsearch.extractor.xml.GenericFunctions;
import ro.cst.tsearch.extractor.xml.GenericFunctions1;
import ro.cst.tsearch.extractor.xml.ResultMap;
import ro.cst.tsearch.extractor.xml.ResultTable;
import ro.cst.tsearch.extractor.xml.StringFormats;
import ro.cst.tsearch.parser.HtmlParser3;
import ro.cst.tsearch.search.name.NameUtils;
import ro.cst.tsearch.servers.response.PropertyIdentificationSet.PropertyIdentificationSetKey;
import ro.cst.tsearch.servers.response.SaleDataSet.SaleDataSetKey;

public class OHFairfieldRO {

	public static ResultMap parseIntermediaryRow(String htmlRow) {
		ResultMap resultMap = new ResultMap();
		try {
			HtmlParser3 htmlParser3 = new HtmlParser3(htmlRow);
			Node row = htmlParser3.getNodeList().elementAt(0);

			if (row.toHtml() != null && row instanceof TableRow) {
				TableColumn[] cols = ((TableRow) row).getColumns();

				if (cols.length >= 9) {// name(both Recorded Land and UCC office type) and date search results->second level of intermediaries

					String name = StringUtils.strip(cols[0].toPlainTextString());
					String partyType = StringUtils.strip(cols[1].toPlainTextString());
					String documentType = StringUtils.strip(cols[2].toPlainTextString());
					String recordedDate = StringUtils.strip(cols[3].toPlainTextString());
					String instrNo = StringUtils.strip(cols[4].toPlainTextString()).replaceAll("^0+", "");
					String book = StringUtils.strip(cols[5].toPlainTextString()).replaceAll("^0+", "");
					String page = StringUtils.strip(cols[6].toPlainTextString()).replaceAll("^0+", "");

					if (partyType.equals("Grantor")) {
						resultMap.put(SaleDataSetKey.GRANTOR.getKeyName(), StringUtils.defaultString(name));
						resultMap.put("tmpGrantorLF", name);
					}
					else if (partyType.equals("Grantee")) {
						resultMap.put(SaleDataSetKey.GRANTEE.getKeyName(), StringUtils.defaultString(name));
						resultMap.put("tmpGranteeLF", name);
					}

					parseNames(resultMap);

					resultMap.put(SaleDataSetKey.DOCUMENT_TYPE.getKeyName(), StringUtils.defaultString(documentType));
					resultMap.put(SaleDataSetKey.RECORDED_DATE.getKeyName(), StringUtils.defaultString(recordedDate));
					resultMap.put(SaleDataSetKey.INSTRUMENT_NUMBER.getKeyName(), StringUtils.defaultString(instrNo));
					resultMap.put(SaleDataSetKey.BOOK.getKeyName(), StringUtils.defaultString(book));
					resultMap.put(SaleDataSetKey.PAGE.getKeyName(), StringUtils.defaultString(page));

				}
				else if (cols.length >= 7) {// document(both Recorded Land and UCC office type) and book/page search results->only one level of intermediaries

					String documentType = StringUtils.strip(cols[1].toPlainTextString());
					String recordedDate = StringUtils.strip(cols[2].toPlainTextString());
					String instrNo = StringUtils.strip(cols[0].toPlainTextString()).replaceAll("^0+", "");
					String book = StringUtils.strip(cols[3].toPlainTextString()).replaceAll("^0+", "");
					String page = StringUtils.strip(cols[4].toPlainTextString()).replaceAll("^0+", "");

					resultMap.put(SaleDataSetKey.INSTRUMENT_NUMBER.getKeyName(), StringUtils.defaultString(instrNo));
					resultMap.put(SaleDataSetKey.DOCUMENT_TYPE.getKeyName(), StringUtils.defaultString(documentType));
					resultMap.put(SaleDataSetKey.RECORDED_DATE.getKeyName(), StringUtils.defaultString(recordedDate));
					resultMap.put(SaleDataSetKey.BOOK.getKeyName(), StringUtils.defaultString(book));
					resultMap.put(SaleDataSetKey.PAGE.getKeyName(), StringUtils.defaultString(page));
				}
				else {// 1st level of intermediaries - search by name
					String name = StringUtils.strip(cols[0].toPlainTextString());
					resultMap.put(SaleDataSetKey.GRANTOR.getKeyName(), StringUtils.defaultString(name));
					resultMap.put("tmpGrantorLF", name);

					parseNames(resultMap);
				}
			}
			resultMap.removeTempDef();
		} catch (Exception e) {
			e.printStackTrace();
		}

		return resultMap;
	}

	@SuppressWarnings("rawtypes")
	public static void parseNames(ResultMap resultMap) {

		ArrayList<List> grantor = new ArrayList<List>();
		String tmpGrantorLF = (String) resultMap.get("tmpGrantorLF");
		if (StringUtils.isNotEmpty(tmpGrantorLF)) {
			parseName(tmpGrantorLF, grantor);
		}

		grantor = removeDuplicates(grantor);
		if (grantor.size() > 0) {
			try {
				resultMap.put("GrantorSet", GenericFunctions.storeOwnerInSet(grantor, true));
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		resultMap.put(SaleDataSetKey.GRANTOR.getKeyName(), concatenateNames(grantor));

		ArrayList<List> grantee = new ArrayList<List>();
		String tmpGranteeLF = (String) resultMap.get("tmpGranteeLF");
		if (StringUtils.isNotEmpty(tmpGranteeLF)) {
			parseName(tmpGranteeLF, grantee);
		}

		grantee = removeDuplicates(grantee);
		if (grantee.size() > 0) {
			try {
				resultMap.put("GranteeSet", GenericFunctions.storeOwnerInSet(grantee, true));
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		resultMap.put(SaleDataSetKey.GRANTEE.getKeyName(), concatenateNames(grantee));
	}

	@SuppressWarnings("rawtypes")
	public static void parseName(String rawNames, ArrayList<List> list) {

		String names[] = { "", "", "", "", "", "" };
		String[] suffixes, type, otherType;

		String split[] = rawNames.split(" / ");
		boolean isPerson = true;
		String lastName = "";
		for (int i = 0; i < split.length; i++) {
			String name = split[i];
			name = cleanName(name);
			if (isCompany(name)) {
				names[2] = name;
				names[0] = names[1] = names[3] = names[4] = names[5];
				isPerson = false;
			} else {
				if ((!isCompany(name))) {
					names = StringFormats.parseNameNashville(name, true);
				}
			}
			type = GenericFunctions.extractAllNamesType(names);
			otherType = GenericFunctions.extractAllNamesOtherType(names);
			suffixes = GenericFunctions.extractNameSuffixes(names);

			if (isPerson) {
				if (i == 0) {
					lastName = names[2];
				} else {
					if (names[2].length() < 2 || names[2].matches("[A-Z]\\.")) {
						names[1] = names[2];
						names[2] = lastName;
					}
				}
			}

			GenericFunctions.addOwnerNames(split[i], names, suffixes[0],
					suffixes[1], type, otherType, isCompany(names[2]), isCompany(names[5]), list);
		}

	}

	public static String cleanName(String name) {

		name = name.replaceFirst("(?is),?\\s*AS\\s+TREASURER\\b.*", "");
		name = name.replaceFirst("(?is)-AS\\s+TREASURER\\b.*", "");
		name = name.replaceFirst("(?is)-AS\\s+STATUTORY\\s+AGENT\\b.*", "");
		name = name.replaceFirst("(?is)-\\s*ATTORNEY\\s+GENERAL\\b.*", "");
		name = name.replaceFirst("(?is)\\b+COUNTY\\s+PROSECUTOR\\b.*", "");

		name = name.replaceFirst("(?is)(UNKNOWN\\s+)?(HEIRS|SPOUSE)\\b.*?\\bOF", "");

		name = name.replaceFirst("(?is)\\bDEC'D\\b", "");
		name = name.replaceFirst("(?is)\\bDECEASED\\b", "");

		name = name.replaceFirst("(?is)\\bSHAREHOLDER(\\s+ETC)?\\b", "");
		name = name.replaceFirst("(?is)\\bACTING(\\s+ADMINISTRATOR)?\\b", "");

		name = name.replaceAll("(?is)\\bMR\\b", "");

		name = name.replaceAll("(?is)\\bA\\s*CORPORATION\\s*$", "");

		name = name.replaceAll("(?is)" + ro.cst.tsearch.extractor.xml.GenericFunctions1.nameSuffixString + "\\.", "$1");
		name = name.replaceAll("(?is)[,-]\\s*" + ro.cst.tsearch.extractor.xml.GenericFunctions1.nameOtherTypeString, " $1");
		name = name.replaceFirst("-\\s*" + GenericFunctions1.nameSuffixString, "$1");

		if (name.indexOf(",") == -1) {
			name = name.replaceAll("(?is)" + ro.cst.tsearch.extractor.xml.GenericFunctions1.nameSuffixString + "(\\s+.+)", "$1,$2");
		}

		name = name.replaceFirst("^\\s*:\\s*", "");
		name = name.replaceFirst("\\s*-\\s*$", "");
		name = name.replaceFirst("\\s*,\\s*$", "");

		return name;
	}

	public static boolean isCompany(String name) {
		if (name.matches("(?is).*\\bCORP\\..*")) {
			return true;
		}
		return NameUtils.isCompany(name);
	}

	@SuppressWarnings("rawtypes")
	public static String concatenateNames(ArrayList<List> nameList) {
		String result = "";

		StringBuilder resultSb = new StringBuilder();
		for (List list : nameList) {
			if (list.size() > 3) {
				resultSb.append(list.get(3)).append(", ").append(list.get(1)).append(" ").append(list.get(2)).append(" / ");
			}
		}
		result = resultSb.toString().replaceAll("/\\s*,\\s*/", " / ").replaceAll(",\\s*/", " /").
				replaceAll("\\s{2,}", " ").replaceAll("/\\s*$", "").trim();
		return result;
	}

	@SuppressWarnings({ "rawtypes", "unchecked" })
	public static ArrayList<List> removeDuplicates(ArrayList<List> list) {
		ArrayList<List> newList = new ArrayList<List>();
		for (List l : list) {
			l.set(0, "");
			if (!newList.contains(l)) {
				newList.add(l);
			}
		}
		return newList;
	}

	@SuppressWarnings("rawtypes")
	public static void parseLegal(ResultMap resultMap, String legal) throws Exception {

		if (StringUtils.isEmpty(legal)) {
			return;
		}
		legal = legal.toUpperCase();

		// after @@@@ is the free form content-which contains the subdivision name
		resultMap.put(PropertyIdentificationSetKey.LEGAL_DESCRIPTION_ON_SERVER.getKeyName(), legal.replaceAll(" @@@@", ""));

		String legalDes = legal;

		legalDes = parseCrossRefs(resultMap, legalDes);

		Pattern LEGAL_LOT_INTERVAL = Pattern.compile("LOTS?\\s+(\\d+)\\s*-\\s*(\\d+)");
		Pattern LEGAL_LOT = Pattern.compile("LOTS?\\s+(\\d+)");
		Pattern LEGAL_LOT_ENUMERATION = Pattern.compile("(?:LOTS?\\s*)?((\\d+,\\s*)+\\d+(\\s*&\\s*\\d+)?)");
		Pattern LEGAL_TRACT = Pattern.compile("TRACT ?([\\d-]+)");
		Pattern LEGAL_BLDG = Pattern.compile("BLDG ?(\\w+)");
		Pattern LEGAL_PHASE = Pattern.compile("PHA?S?E? ?(\\w+)");
		Pattern LEGAL_SECTION = Pattern.compile("SEC(TION)? ?(\\w+)");
		Pattern LEGAL_UNIT = Pattern.compile("UNIT ?(\\w+)");

		Matcher matcher = LEGAL_LOT_ENUMERATION.matcher(legalDes);
		// get lot enumeration
		String lots = "";
		while (matcher.find()) {
			legalDes = legalDes.replaceFirst(matcher.group(), "");
			lots += matcher.group(1).replaceAll("(\\s*,\\s*)|(\\s*&\\s*)", " ")+" ";

		}
		
		if (!lots.isEmpty()) {
			resultMap.put(PropertyIdentificationSetKey.SUBDIVISION_LOT_NUMBER.getKeyName(), lots);
		}

		matcher = LEGAL_LOT_INTERVAL.matcher(legalDes);
		
		// get lot interval
		if (matcher.find()) {
			legalDes = legalDes.replaceFirst(matcher.group(), "");

			resultMap.put(PropertyIdentificationSetKey.SUBDIVISION_LOT_NUMBER.getKeyName(), matcher.group(1));
			resultMap.put(PropertyIdentificationSetKey.SUBDIVISION_LOT_THROUGH.getKeyName(), matcher.group(2));
		}

		matcher = LEGAL_LOT.matcher(legalDes);

		// get lot
		if (matcher.find()) {
			legalDes = legalDes.replaceFirst(matcher.group(), "");

			resultMap.put(PropertyIdentificationSetKey.SUBDIVISION_LOT_NUMBER.getKeyName(), matcher.group(1));
		}

		// get tract
		matcher = LEGAL_TRACT.matcher(legalDes);
		if (matcher.find()) {
			String tract = matcher.group(1);
			legalDes = legalDes.replace(matcher.group(), "");
			resultMap.put(PropertyIdentificationSetKey.SUBDIVISION_TRACT.getKeyName(), StringUtils.strip(tract));
		}

		// get bldg
		matcher = LEGAL_BLDG.matcher(legalDes);
		if (matcher.find()) {
			String bldg = matcher.group(1);
			legalDes = legalDes.replace(matcher.group(), "");
			resultMap.put(PropertyIdentificationSetKey.SUBDIVISION_BLDG.getKeyName(), StringUtils.strip(bldg));
		}

		// get phase
		matcher = LEGAL_PHASE.matcher(legalDes);
		if (matcher.find()) {
			String phase = matcher.group(1);
			legalDes = legalDes.replace(matcher.group(), "");
			resultMap.put(PropertyIdentificationSetKey.SUBDIVISION_PHASE.getKeyName(), StringUtils.strip(phase));
		}

		// get section-township-range
		// extract and remove sec-twn-rng from legal description
		List<List> body = new ArrayList<List>();
		Pattern p = Pattern
				.compile("(?s)R\\s*-\\s*(\\d+)\\s+T\\s*-\\s*(\\d+)\\s+S\\s*-\\s*(\\d+)\\b");
		Matcher ma = p.matcher(legalDes);
		if (ma.find()) {
			List<String> line = new ArrayList<String>();
			line.add(ma.group(3));
			line.add(ma.group(2));
			line.add(ma.group(1));
			body.add(line);
			legalDes = legalDes.replace(ma.group(0), "");
		}

		// extract and replace section from legal description
		p = Pattern.compile("\\bSEC(?:TION)? (\\d+)\\b(?![\\.'/])");
		ma = p.matcher(legalDes);
		if (ma.find()) {
			List<String> line = new ArrayList<String>();
			line.add(ma.group(1));
			line.add("");
			line.add("");
			body.add(line);
			legalDes = legalDes.replace(ma.group(0), "SEC ");
		}
		if (!body.isEmpty()) {
			String[] header = { "SubdivisionSection", "SubdivisionTownship", "SubdivisionRange" };

			Map<String, String[]> map = new HashMap<String, String[]>();
			map.put("SubdivisionSection", new String[] { "SubdivisionSection", "" });
			map.put("SubdivisionTownship", new String[] { "SubdivisionTownship", "" });
			map.put("SubdivisionRange", new String[] { "SubdivisionRange", "" });

			ResultTable pis = new ResultTable();
			pis.setHead(header);
			pis.setBody(body);
			pis.setMap(map);
			resultMap.put("PropertyIdentificationSet", pis);
		}

		// get section
		matcher = LEGAL_SECTION.matcher(legalDes);
		if (matcher.find()) {
			String sec = matcher.group(2);
			legalDes = legalDes.replace(matcher.group(), "");
			resultMap.put(PropertyIdentificationSetKey.SUBDIVISION_SECTION.getKeyName(), StringUtils.strip(sec));
		}

		// get unit
		matcher = LEGAL_UNIT.matcher(legalDes);
		if (matcher.find()) {
			String unit = matcher.group(1);
			legalDes = legalDes.replace(matcher.group(), "");
			resultMap.put(PropertyIdentificationSetKey.SUBDIVISION_UNIT.getKeyName(), StringUtils.strip(unit));
		}

		// get subdivision name
		Pattern pat = Pattern.compile("(.*?)\\s*\\b([A-Z]/[A-Z]|LO?TS?|UN(?:IT)?|APT|PH|BL?K|SEC|CONDO(MINIUM)?|\\d+-\\d+ " +
				"[A-Z]|\\d+(ST|ND|RD|TH) (REV(ISION)?|ADDN?|SEC(TION)?)|REV PLAT|SECTOR|SUB|LAND SEC(TION)?|SEC(TION)? \\d+|\\d+-\\d+-\\d+)?\\b.*");
		Matcher mat = pat.matcher(legalDes.replaceFirst("(?s)(.*?)@@@@.*", "$1"));
		if (mat.find()) {
			try {
				String subdName = mat.group(0);
				subdName = subdName.replaceAll("(?s)\\bMECH.*?\\s*\\bL(IE)?N\\b", "");
				subdName = subdName
						.replaceAll("(?s)\\b(REESES|RENEWAL|IN|DIV|PART(\\s+IN)?|DEED|BL?K|SUB|PT|SEC|TRACT|REPLAT|OR|PR|V?OL|[NEWS]{1,2}|WB|"
								+ "PAGE|PGS?|ASSIGN(MENT)?|PERS\\s+TA?X\\s+L(IE)?N|(EX\\s+)?(C\\s+)?AFFIDAVIT|"
								+ "(TERMINATE\\s+)?UCC\\s+[\\d-#]+|RELEASE\\s*[\\d-#]+|(REF\\s+)?DEED)\\b\\s*(\\d*|[A-Z])", "");
				subdName = subdName.replaceAll("(?s)(\\d(ST|ND|RD|TH))?\\s*\\bADD\\b\\s*\\d*", "");
				subdName = subdName.replaceAll("(?s)\\bLO?TS?\\s+([-\\d\\s&,]+)", "");
				subdName = subdName.replaceAll("(?s)\\s*(?:NORTH|SOUTH|EAST|WEST).*?\\s*\\b(ADD|SEC)\\s*\\d*", "");
				subdName = subdName.replaceAll("(?s)\\s*\\bPH(?:ASE)?\\s*(?:[\\dA-Z]+)", "");
				subdName = subdName.replaceAll("(?s)\\s*&\\s*\\d+", "");
				subdName = subdName.replaceAll("(?s)\\d*\\s*\\bREF(ERENCES)?\\b(\\s+SEE)?(\\s+RECORD)?", "");
				subdName = subdName.replaceAll("(?is)&?\\s*\\d*\\s*$", "");
				subdName = subdName.replaceAll("(?is)(\\d*\\s*(&|,)\\s*\\d*\\s*)+$", "");
				if (!subdName.trim().isEmpty()) {

					subdName = subdName.trim();
					resultMap.put(PropertyIdentificationSetKey.SUBDIVISION_NAME.getKeyName(), subdName);
				}

			} catch (Exception e) {
				e.printStackTrace();
			}
		}

	}

	@SuppressWarnings("rawtypes")
	private static String parseCrossRefs(ResultMap resultMap, String legal) throws Exception {
		if (!legal.isEmpty()) {
			Pattern pat = Pattern.compile("(?i)(?:MTG)?\\s*(?:OR|V?OL|MV|PR)\\s+(\\d+)\\s+(?:PGS?|PAGE)\\s+(\\d+)");
			Matcher mat = pat.matcher(legal);
			List<List> bodyCR = new ArrayList<List>();

			while (mat.find()) {
				// add to crossrefset
				List<String> line = new ArrayList<String>();

				line.add(ro.cst.tsearch.utils.StringUtils.removeLeadingZeroes(mat.group(1)));
				line.add(ro.cst.tsearch.utils.StringUtils.removeLeadingZeroes(mat.group(2)));
				bodyCR.add(line);
				legal = legal.replaceFirst(Pattern.quote(mat.group(0)), "");
			}
			if (!bodyCR.isEmpty()) {
				String[] header = { "Book", "Page" };
				Map<String, String[]> map = new HashMap<String, String[]>();
				map.put("Book", new String[] { "Book", "" });
				map.put("Page", new String[] { "Page", "" });
				ResultTable crossRefsTable = new ResultTable();
				crossRefsTable.setHead(header);
				crossRefsTable.setBody(bodyCR);
				crossRefsTable.setMap(map);

				resultMap.put("CrossRefSet", crossRefsTable);
			}
		}
		return legal;
	}

}