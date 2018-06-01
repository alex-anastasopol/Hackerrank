package ro.cst.tsearch.servers.functions;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.apache.commons.lang.StringUtils;
import org.htmlparser.filters.HasAttributeFilter;
import org.htmlparser.filters.TagNameFilter;
import org.htmlparser.tags.TableColumn;
import org.htmlparser.tags.TableRow;
import org.htmlparser.tags.TableTag;
import org.htmlparser.util.NodeList;
import org.htmlparser.util.ParserException;

import ro.cst.tsearch.extractor.xml.Bridge;
import ro.cst.tsearch.extractor.xml.GenericFunctions;
import ro.cst.tsearch.extractor.xml.ResultMap;
import ro.cst.tsearch.extractor.xml.StringFormats;
import ro.cst.tsearch.parser.HtmlParser3;
import ro.cst.tsearch.search.name.NameUtils;
import ro.cst.tsearch.servers.response.OtherInformationSet.OtherInformationSetKey;
import ro.cst.tsearch.servers.response.SaleDataSet.SaleDataSetKey;
import ro.cst.tsearch.servers.response.ServerResponse;

import com.stewart.ats.base.document.DocumentI;
import com.stewart.ats.base.document.OffenderInformationDocument;
import com.stewart.ats.base.document.OffenderInformationDocumentI;
import com.stewart.ats.base.document.RegisterDocument;

/**
 * 
 * @author Oprina George
 * 
 *         Apr 28, 2011
 */
public class TXOffenderInformationOI {
	public static HashMap<String, String> SEX = new HashMap<String, String>();

	static {
		SEX.put("M", "MALE");
		SEX.put("F", "FEMALE");
	}

	public static HashMap<String, String> RACE = new HashMap<String, String>();

	static {
		RACE.put("W", "WHITE");
		RACE.put("B", "BLACK");
		RACE.put("H", "HISPANIC");
		RACE.put("I", "AMERICAN INDIAN");
		RACE.put("A", "ASIAN OR PACIFIC ISLANDER");
		RACE.put("U", "UNKNOWN");
	}

	public static void putSearchType(ResultMap resultMap, String type) {
		resultMap.put(OtherInformationSetKey.SRC_TYPE.getKeyName(), type);
	}

	public static ResultMap parseIntermediaryRow(TableRow row, long searchId) {
		ResultMap resultMap = new ResultMap();

		putSearchType(resultMap, "OI");

		TableColumn[] cols = row.getColumns();
		if (cols.length == 7) {
			// col 0 = name
			String name = cols[0].toPlainTextString().trim();
			resultMap.put(SaleDataSetKey.GRANTOR.getKeyName(), name);
			parseNames(resultMap, name, "GrantorSet");
			// col 1 = instr no
			String instrNo = cols[1].toPlainTextString().trim();
			resultMap.put(SaleDataSetKey.INSTRUMENT_NUMBER.getKeyName(),
					instrNo);
			// col 2 = Race
			String r = cols[2].toPlainTextString().trim();
			resultMap.put("tmpRace", RACE.containsKey(r) ? RACE.get(r) : "");
			// col 3 = Sex
			String s = cols[3].toPlainTextString().trim();
			resultMap.put("tmpSex", SEX.containsKey(s) ? SEX.get(s) : "");
			// col 4
			// col 5
			// col 6
			resultMap.put(SaleDataSetKey.GRANTEE.getKeyName(), "TDCJ");
			parseNames(resultMap, "TDCJ", "GranteeSet");
		}
		return resultMap;
	}

	public static OffenderInformationDocumentI makeOffenderDoc(ResultMap m,
			DocumentI document) {
		OffenderInformationDocumentI oiDoc = new OffenderInformationDocument(
				(RegisterDocument) document);
		if (m.get("tmpSID") != null) {
			oiDoc.setSID((String) m.get("tmpSID"));
		}
		if (m.get("tmpRace") != null) {
			oiDoc.setRace((String) m.get("tmpRace"));
		}
		if (m.get("tmpSex") != null) {
			oiDoc.setSex((String) m.get("tmpSex"));
		}
		return oiDoc;
	}

	@SuppressWarnings("rawtypes")
	public static void parseNames(ResultMap resultMap, String name, String set) {
		// all names are LCFM

		name = name.toUpperCase();

		try {
			ArrayList<List> body = new ArrayList<List>();

			String[] names = StringFormats.parseNameNashville(name, true);
			String[] type = GenericFunctions.extractAllNamesType(names);
			String[] otherType = GenericFunctions
					.extractAllNamesOtherType(names);
			String[] suffixes = GenericFunctions.extractNameSuffixes(names);

			GenericFunctions.addOwnerNames(name, names, suffixes[0],
					suffixes[1], type, otherType,
					NameUtils.isCompany(names[2]),
					NameUtils.isCompany(names[5]), body);
			resultMap.put(set, GenericFunctions.storeOwnerInSet(body, true));

		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public static void parseAddress(ResultMap resultMap, String address) {

	}

	public static void parseLegalDescription(ResultMap resultMap,
			String legal_des) {
	}

	public static DocumentI parseAndFillResultMap(ServerResponse response,
			String detailsHtml, long searchId) {
		DocumentI document = null;
		try {
			ResultMap map1 = new ResultMap();

			// add info in map1
			putSearchType(map1, "OI");

			org.htmlparser.Parser htmlParser = org.htmlparser.Parser
					.createParser(detailsHtml, null);
			NodeList divList = htmlParser.parse(null).extractAllNodesThatMatch(
					new TagNameFilter("div"), true);

			// TDCJ number
			String TDCJ_no = getDivValue(divList, "TDCJ Number:");
			map1.put(SaleDataSetKey.INSTRUMENT_NUMBER.getKeyName(), TDCJ_no);

			// recorded date
			String recorded_date = "N/A";
			map1.put(SaleDataSetKey.RECORDED_DATE.getKeyName(), recorded_date);

			// instr date
			htmlParser = org.htmlparser.Parser.createParser(detailsHtml, null);

			NodeList nodeList = htmlParser
					.parse(null)
					.extractAllNodesThatMatch(new TagNameFilter("table"), true)
					.extractAllNodesThatMatch(
							new HasAttributeFilter("border", "1"), true);

			TableTag t = getTableFromList(nodeList, "Offense Date");
			if (t != null) {
				TableRow[] rows = t.getRows();
				for (TableRow row : rows) {
					if (row.getAttribute("valign") != null
							&& row.getAttribute("valign").equals("middle")) {
						if (row.getColumnCount() == 6) {
							String sentence_date = row.getColumns()[0]
									.toPlainTextString()
									.trim()
									.replace(" ", "")
									.replaceAll(
											"(\\d\\d\\d\\d)-(\\d\\d)-(\\d\\d)",
											"$2/$3/$1");
							map1.put(
									SaleDataSetKey.INSTRUMENT_DATE.getKeyName(),
									sentence_date);
							break;
						}
					}
				}
			}

			// doc type / subcateg
			String doc_type = "OFFENDER INFO";
			map1.put(SaleDataSetKey.DOCUMENT_TYPE.getKeyName(), doc_type);

			// grantor
			String name = getDivValue(divList, "Name:");
			map1.put(SaleDataSetKey.GRANTOR.getKeyName(), name);
			parseNames(map1, name, "GrantorSet");

			// grantee = TDCJ
			String grantee = "TDCJ";
			map1.put(SaleDataSetKey.GRANTEE.getKeyName(), grantee);
			parseNames(map1, grantee, "GranteeSet");

			map1.removeTempDef();

			document = (RegisterDocument) new Bridge(
					response.getParsedResponse(), map1, searchId).importData();

			// map with specific information for offenders doc
			ResultMap map2 = new ResultMap();
			// sid
			String sid = getDivValue(divList, "SID Number:");
			map2.put("tmpSID", sid);

			// race
			String race = getDivValue(divList, "Race:");
			map2.put("tmpRace", RACE.containsKey(race) ? RACE.get(race) : "");

			// sex
			String sex = getDivValue(divList, "Gender:");
			map2.put("tmpSex", SEX.containsKey(sex) ? SEX.get(sex) : "");

			document = ro.cst.tsearch.servers.functions.TXOffenderInformationOI
					.makeOffenderDoc(map2, document);

		} catch (Exception e) {
			e.printStackTrace();
		}
		
		return document;
	}

	public static String getNodeByName(String response, String nodeName) {
		String node = "";
		try {
			org.htmlparser.Parser htmlParser = org.htmlparser.Parser
					.createParser(response, null);

			NodeList nodeList = htmlParser.parse(null);
			node = HtmlParser3.getValueFromAbsoluteCell(0, 1,
					HtmlParser3.findNode(nodeList, nodeName), "", true).trim();
		} catch (ParserException e) {
			e.printStackTrace();
		}

		return node;
	}

	public static TableTag getTableFromList(NodeList nodes, String name) {
		TableTag table = null;
		for (int i = 0; i < nodes.size(); i++) {
			if (nodes.elementAt(i).toHtml().contains(name)) {
				table = (TableTag) nodes.elementAt(i);
				break;
			}
		}
		return table;
	}

	public static String getDivValue(NodeList nodes, String name) {
		String res = "";

		for (int i = 0; i < nodes.size(); i++) {
			if (StringUtils.strip(
					nodes.elementAt(i).toPlainTextString()
							.replaceAll("\\s+", " ")).equals(
					StringUtils.strip(name.replaceAll("\\s+", " ")))) {
				if (i + 1 <= nodes.size())
					return StringUtils.strip(nodes.elementAt(i + 1)
							.toPlainTextString().replaceAll("\\s+", " "));
			}
		}

		return res;
	}
}
