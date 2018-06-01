package ro.cst.tsearch.servers.functions;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang.StringUtils;
import org.htmlparser.Node;
import org.htmlparser.filters.TagNameFilter;
import org.htmlparser.tags.TableColumn;
import org.htmlparser.tags.TableRow;
import org.htmlparser.tags.TableTag;
import org.htmlparser.util.NodeList;

import ro.cst.tsearch.extractor.xml.GenericFunctions;
import ro.cst.tsearch.extractor.xml.GenericFunctions1;
import ro.cst.tsearch.extractor.xml.ResultMap;
import ro.cst.tsearch.extractor.xml.StringFormats;
import ro.cst.tsearch.parser.HtmlParser3;
import ro.cst.tsearch.search.name.NameUtils;
import ro.cst.tsearch.servers.response.ServerResponse;
import ro.cst.tsearch.servers.response.OtherInformationSet.OtherInformationSetKey;
import ro.cst.tsearch.servers.response.SaleDataSet.SaleDataSetKey;

public class ILGenericCyberDriveCC {

	public static ResultMap parseIntermediaryRow(TableRow row) {

		ResultMap resultMap = new ResultMap();
		parseIntermediaryRow(row, resultMap);
		return resultMap;
	}

	private static void parseIntermediaryRow(TableRow row, ResultMap resultMap) {
		try {
			TableColumn[] cols = row.getColumns();
			String grantor = "";
			String grantee = "";
			String filedDate = "";
			String instrumentNumber = "";
			String tagPattern = "(?s)\\s*<[^>]*>\\s*";
			String brPattern = "(?is)(.*?)<br[^>]*.*";

			if (cols.length >= 3) {// cols length is 3 for 'Federal Tax Lien' module
				instrumentNumber = cols[0].toHtml().replaceFirst("(?is).*?<a[^>]*>\\s*(.*?)\\s*</a>.*", "$1").replaceAll("(?is)\\s*<[^>]*>\\s*", "");
				filedDate = cols[1].toHtml().replaceFirst(brPattern, "$1").replaceAll(tagPattern, "");
				grantor = cols[2].toHtml().replaceFirst(brPattern, "$1").replaceAll(tagPattern, "");
				if (cols.length > 3) {
					grantee = cols[3].toHtml().replaceFirst(brPattern, "$1").replaceAll(tagPattern, "");
					resultMap.put(SaleDataSetKey.DOCUMENT_TYPE.getKeyName(), "UCC");
				} else if (cols.length == 3) {
					resultMap.put(SaleDataSetKey.DOCUMENT_TYPE.getKeyName(), "FTL");
				}

				if (!instrumentNumber.isEmpty()) {
					resultMap.put(SaleDataSetKey.INSTRUMENT_NUMBER.getKeyName(), instrumentNumber.replaceAll("\\s*\\*\\s*", ""));
				}
				if (!filedDate.isEmpty()) {
					resultMap.put(SaleDataSetKey.RECORDED_DATE.getKeyName(), filedDate);
				}

				String nameTokensToRemovePattern = "\\s*(\\w*\\s*/\\w*)+\\b";// remove tokens like "D/B/A", "C/O", "F/K/A", "T/A" etc.

				if (!grantor.isEmpty()) {
					grantor = grantor.replaceFirst(nameTokensToRemovePattern, "");
					resultMap.put(SaleDataSetKey.GRANTOR.getKeyName(), grantor);
					resultMap.put("tmpGrantorLF", grantor);
				}
				if (!grantee.isEmpty()) {
					grantee = grantee.replaceFirst(nameTokensToRemovePattern, "");
					resultMap.put(SaleDataSetKey.GRANTEE.getKeyName(), StringUtils.defaultString(grantee));
					resultMap.put("tmpGranteeLF", grantee);
				}

				parseNames(resultMap);
			}

			resultMap.removeTempDef();

		} catch (Exception e) {
			e.printStackTrace();
		}
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

	public static Object parseAndFillResultMap(ServerResponse response, String detailsHtml, ResultMap resultMap) {
		try {
			HtmlParser3 htmlParser3 = new HtmlParser3(detailsHtml);
			TableRow intermediariesRow = null;
			Node intermediariesNode = htmlParser3.getNodeById("intermediariesContent");
			Node detailsNode = htmlParser3.getNodeById("details");
			if (intermediariesNode != null) {
				NodeList intermediariesRows = intermediariesNode.getChildren().extractAllNodesThatMatch(new TagNameFilter("tr"), true);
				if (intermediariesRows.size() > 1) {
					intermediariesRow = (TableRow) intermediariesRows.elementAt(1);
					ro.cst.tsearch.servers.functions.ILGenericCyberDriveCC.parseIntermediaryRow(intermediariesRow, resultMap);
				}

				StringBuilder notesSB = new StringBuilder();
				if (detailsNode != null) {
					Node table = HtmlParser3.getFirstTag(detailsNode.getChildren(), TableTag.class, true);
					if (table != null) {
						TableRow[] rows = ((TableTag) table).getRows();
						for (int i = 1; i < rows.length; i++) {
							TableColumn[] columns = rows[i].getColumns();
							if (columns.length > 2) {
								if (!notesSB.toString().isEmpty()) {
									notesSB.append("\n");
								}
								notesSB.append(columns[0].toPlainTextString().trim());
								notesSB.append(", No. " + columns[columns.length - 1].toPlainTextString().trim());
								notesSB.append(", " + columns[1].toPlainTextString().trim());
							}
						}
						if (!notesSB.toString().isEmpty()) {
							resultMap.put(OtherInformationSetKey.REMARKS.getKeyName(), notesSB.toString());
						}
					}
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		return null;
	}
}
