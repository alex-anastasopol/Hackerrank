package ro.cst.tsearch.servers.functions;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.htmlparser.Node;
import org.htmlparser.tags.TableRow;
import org.htmlparser.tags.TableTag;
import org.htmlparser.util.NodeList;

import ro.cst.tsearch.extractor.xml.GenericFunctions;
import ro.cst.tsearch.extractor.xml.ResultMap;
import ro.cst.tsearch.extractor.xml.StringFormats;
import ro.cst.tsearch.parser.HtmlParser3;
import ro.cst.tsearch.search.name.NameUtils;
import ro.cst.tsearch.servers.response.CourtDocumentIdentificationSet.CourtDocumentIdentificationSetKey;
import ro.cst.tsearch.servers.response.SaleDataSet.SaleDataSetKey;
import ro.cst.tsearch.utils.StringUtils;

public class OHDelawareCO {

	public static ResultMap parseIntermediaryRow(String htmlRow) {

		ResultMap resultMap = new ResultMap();

		htmlRow = "<tr>" + htmlRow + "</tr>";
		HtmlParser3 htmlParser3 = new HtmlParser3(htmlRow);
		Node node = htmlParser3.getNodeList().elementAt(0);
		NodeList nodeList = node.getChildren();

		if (node != null && node instanceof TableRow) {
			TableRow row = (TableRow) node;

			if (row.getColumnCount() == 6) {

				String instrNo = row.getColumns()[0].toPlainTextString().trim();
				resultMap.put(SaleDataSetKey.INSTRUMENT_NUMBER.getKeyName(), instrNo);
				resultMap.put(CourtDocumentIdentificationSetKey.CASE_NUMBER.getKeyName(), instrNo);

				List<String> partyCompanyList = new ArrayList<String>();
				List<String> partyTypeList = new ArrayList<String>();

				TableTag partyCompanyTable = (TableTag) nodeList.elementAt(1).getFirstChild();
				if (partyCompanyTable != null) {
					for (int i = 0; i < partyCompanyTable.getRowCount(); i++) {
						partyCompanyList.add(partyCompanyTable.getRow(i).toPlainTextString().trim());
					}
				}

				TableTag partyTypeTable = (TableTag) nodeList.elementAt(3).getFirstChild();
				if (partyTypeTable != null) {
					for (int i = 0; i < partyTypeTable.getRowCount(); i++) {
						partyTypeList.add(partyTypeTable.getRow(i).toPlainTextString().trim());
					}
				}

				StringBuilder grantor = new StringBuilder();
				StringBuilder grantee = new StringBuilder();

				for (int i = 0; i < partyCompanyList.size(); i++) {
					String partyCompany = partyCompanyList.get(i);
					if (i < partyTypeList.size()) {
						String partyType = partyTypeList.get(i);
						if (partyType.matches("(?is).*\\b(PLNTF|PET|APPT|CRDT)\\b.*")) {
							grantor.append(partyCompany).append(" / ");
						} else if (partyType.matches("(?is).*\\b(DFNDT|R(?:E)?SP[A-Z]+|APPE|DBTR)\\b.*")) {
							grantee.append(partyCompany).append(" / ");
						}
					}
				}

				String grantorString = grantor.toString().replaceFirst(" / $", "");
				String granteeString = grantee.toString().replaceFirst(" / $", "");

				if (!StringUtils.isEmpty(grantorString)) {
					resultMap.put("tmpGrantorLF", grantorString);
				}
				if (!StringUtils.isEmpty(granteeString)) {
					resultMap.put("tmpGranteeLF", granteeString);
				}

				parseNames(resultMap);
			}
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

		for (int i = 0; i < split.length; i++) {
			String name = split[i];

			String[] spl = name.split("(?is)(\\b[ANF]KA|FNA|DBA:?\\b)|(%)");
			if (spl.length == 1) {
				Matcher ma = Pattern.compile("(?is)(.+)\\bMINOR\\s+(.*)\\bBY\\s+(.+)\\b(?:FATHER|MOTHER)(.*)").matcher(name);
				if (ma.find()) {
					spl = new String[2];
					spl[0] = ma.group(1) + " " + ma.group(2);
					spl[1] = ma.group(3) + " " + ma.group(4);
				}
			}
			boolean onlyPersons = true;
			for (int j = 0; j < spl.length; j++) {
				if (NameUtils.isCompany(spl[j])) {
					onlyPersons = false;
					break;
				}
			}
			String lastName = "";
			for (int j = 0; j < spl.length; j++) {
				String nm = spl[j];
				nm = cleanName(nm);
				if (isCompany(nm)) {
					names[2] = nm;
					names[0] = names[1] = names[3] = names[4] = names[5];
				} else {
					if ((!isCompany(nm) && nm.indexOf(",") != -1)) {
						names = StringFormats.parseNameNashville(nm, true);
					}
				}
				type = GenericFunctions.extractAllNamesType(names);
				otherType = GenericFunctions.extractAllNamesOtherType(names);
				suffixes = GenericFunctions.extractNameSuffixes(names);

				if (onlyPersons) {
					if (j == 0) {
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

}
