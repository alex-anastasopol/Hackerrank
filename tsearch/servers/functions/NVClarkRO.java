package ro.cst.tsearch.servers.functions;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Vector;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.htmlparser.filters.TagNameFilter;
import org.htmlparser.tags.Span;
import org.htmlparser.tags.TableColumn;
import org.htmlparser.tags.TableRow;
import org.htmlparser.tags.TableTag;
import org.htmlparser.util.NodeList;

import ro.cst.tsearch.Search;
import ro.cst.tsearch.extractor.xml.GenericFunctions;
import ro.cst.tsearch.extractor.xml.GenericFunctions1;
import ro.cst.tsearch.extractor.xml.ResultMap;
import ro.cst.tsearch.extractor.xml.StringFormats;
import ro.cst.tsearch.parser.HtmlParser3;
import ro.cst.tsearch.search.name.NameUtils;
import ro.cst.tsearch.servers.response.OtherInformationSet.OtherInformationSetKey;
import ro.cst.tsearch.servers.response.PropertyIdentificationSet.PropertyIdentificationSetKey;
import ro.cst.tsearch.servers.response.SaleDataSet.SaleDataSetKey;
import ro.cst.tsearch.servers.response.ServerResponse;

/**
 * @author costi
 * 
 */
public class NVClarkRO {

	public static Object parseAndFillResultsMap(ServerResponse response,
			String detailsHtml, ResultMap resultMap, Search search) {
		if (detailsHtml == null)
			return null;

		resultMap.put(OtherInformationSetKey.SRC_TYPE.getKeyName(), "RO");

		HashMap<String, String> details = getDetailedContent(detailsHtml);
		resultMap.put(SaleDataSetKey.INSTRUMENT_NUMBER.getKeyName(),
				getShortInstrumentNumber(details.get("instrNr")));
		
		// Add recorded date.
		if (details.containsKey("record_date"))
			resultMap.put(SaleDataSetKey.RECORDED_DATE.getKeyName(),
					details.get("record_date"));
		
		String docType = details.get("type");
		// Add document type.
		if (details.containsKey("type"))
			resultMap.put(SaleDataSetKey.DOCUMENT_TYPE.getKeyName(),
					docType);
		
		// Add book type.
		if (details.containsKey("book_type"))
			resultMap.put(SaleDataSetKey.BOOK_TYPE.getKeyName(),
					details.get("book_type"));
		
		// Add book/page.
		if (details.containsKey("book"))
			resultMap
					.put(SaleDataSetKey.BOOK.getKeyName(), details.get("book"));
		if (details.containsKey("page"))
			resultMap
					.put(SaleDataSetKey.PAGE.getKeyName(), details.get("page"));
		
		// Add instrument date.
		if (details.containsKey("instr_date"))
			resultMap.put(SaleDataSetKey.INSTRUMENT_DATE.getKeyName(),
					details.get("instr_date"));
		
		// Add instrument consideration amoun.
		if (details.containsKey("value"))
			resultMap.put(SaleDataSetKey.CONSIDERATION_AMOUNT.getKeyName(),
					details.get("value"));
		
		// Add parcel id.
		if (details.containsKey("pin"))
			resultMap.put(PropertyIdentificationSetKey.PARCEL_ID.getKeyName(),
					details.get("pin"));

		// Add grantor/grantee.
		if (details.containsKey("grantor")) {
			resultMap.put("SaleDataSet.Grantor", details.get("grantor")
					.replaceAll("<br />", " / "));
			addNames("GrantorSet", resultMap, details.get("grantor"), search.getSearchID());
		}

		if (details.containsKey("grantee")) {
			resultMap.put("SaleDataSet.Grantee", details.get("grantee")
					.replaceAll("<br />", " / "));
			addNames("GranteeSet", resultMap, details.get("grantee"), search.getSearchID());
		}

		return null; // No image link yet
	}

	/**
	 * Gets the detailed document contents
	 * 
	 * @param rsResponse
	 * @param link
	 * @param nvClarkRO
	 * @return An array containing the html on the first index and the
	 *         instrument number on the second one.
	 */
	public static HashMap<String, String> getDetailedContent(String rsResponse) {
		if (rsResponse == null)
			return null;
		HashMap<String, String> result = new HashMap<String, String>();

		// Parse the html.
		HtmlParser3 parser = new HtmlParser3(rsResponse);
		NodeList nodeList = parser.getNodeList();
		NodeList mainTableList = nodeList.extractAllNodesThatMatch(
				new TagNameFilter("table"), true);
		if (mainTableList.size() == 0)
			return null;
		TableTag table = (TableTag) mainTableList.elementAt(0);

		if (rsResponse.startsWith("<div")) {
			// Already parsed.
			result.put("html", rsResponse);
			result.put("instrNr", table.getAttribute("instrNr"));
		} else {
			// Get the instrument number.
			String instrNr = getInstrumentNumber(nodeList);
			table.setAttribute("instrNr", instrNr);
			table.setAttribute("align", "center");
			table.setAttribute("width", "700");
			table.removeAttribute("style");
			table.setAttribute("cellspacing", "2");

			String html = "<div></div><style type=\"text/css\">body {background-color:#FFFFFF;margin-top:20px;margin-left:20px;margin-right:0px;font-family:Verdana;font-size:12pt;}</style>"
					+ table.toHtml();
			result.put("html", html);
			result.put("instrNr", instrNr);
		}

		// Parse each document row.
		TableRow[] rows = table.getRows();
		for (TableRow row : rows) {
			TableColumn[] columns = row.getColumns();
			if (columns.length < 2)
				continue;
			String c1 = columns[0].toPlainTextString().trim();
			String c2 = columns[1].toPlainTextString().trim();
			if (c1.contains("Record Date")) {
				// Parse the recorded date.
				result.put("record_date", c2);
			} else if (c1.contains("Parcel #:")) {
				// Parse the Parcel ID.
				result.put("pin", c2);
			} else if (c1.contains("Book Type:")) {
				// Parse the book type.
				result.put("book_type", c2);
			} else if (c1.contains("Marriage Date")) {
				// Parse the insrument dat.
				result.put("instr_date", c2);
			} else if (c1.contains("Total Value")) {
				// Parse the consideration value.
				result.put("value", c2.replaceAll("\\$|,", "").replaceAll("(\\d+)\\.(.*)","$1"));
			} else if (c1.contains("Book Page:")) {
				// Parse the book and page
				String[] split = c2.split("/");
				if (split.length < 2)
					continue;
				result.put("book", split[0].trim());
				result.put("page", split[1].trim());
			} else if (c1.contains("1st Party") || c1.contains("Grantor")
					|| c1.contains("Groom")) {
				// Parse the grantor(s) names.
				result.put("grantor", getNamesFromColumn(columns[1]));
			} else if (c1.contains("2nd Party") || c1.contains("Grantee")
					|| c1.contains("Bride")) {
				// Parse the grantee(s) names.
				result.put("grantee", getNamesFromColumn(columns[1]));
			} else if (c1.contains("Number of Pages")) {
				result.put("nr_of_pages", c2);
			} else if (c1.contains("Document Type:")) {
				// Parse the document type.
				result.put("type", c2);
				// // Parse the document type. Matcher m =
				// Pattern.compile("\\((.*)\\)").matcher(
				// columns[1].toPlainTextString());
				// if (m.find())
				// result.put("type", m.group(1));
			}

		}

		return result;
	}

	/**
	 * Parse the names inside the column. eg. <span>BAUMAN, JOSEPH<br />
	 * </span><span>BAUMAN, KRISTINE<br />
	 * </span> returns BAUMAN, JOSEPH<br />
	 * BAUMAN, KRISTINE
	 * 
	 * @param column
	 * @return
	 */
	private static String getNamesFromColumn(TableColumn column) {
		NodeList nodes = column.getChildren().extractAllNodesThatMatch(
				new TagNameFilter("span"), true);
		if (nodes.size() == 0)
			return column.toPlainTextString().trim();
		StringBuilder names = new StringBuilder();
		for (int i = 0; i < nodes.size(); i++) {
			Span node = (Span) nodes.elementAt(i);
			if (i > 0)
				names.append("<br />");
			names.append(node.toPlainTextString());
		}
		return names.toString();
	}
	
	public static String getShortInstrumentNumber(String instrNr) {
		Matcher m = Pattern.compile("(?is)(\\d{4})(\\d{2})(\\d{2})(\\d+)").matcher(instrNr);
		if (m.find()) {
			//Remove leading zeroes.
			return m.group(4).replaceFirst("^0+(?!$)", "");
		}
		return instrNr;
	}
	
	/**
	 * Gets the instrument number from the title tag.
	 * 
	 * @param nodeList
	 * @return
	 */
	private static String getInstrumentNumber(NodeList nodeList) {
		// Get the <title> tag.
		NodeList titleTag = nodeList.extractAllNodesThatMatch(
				new TagNameFilter("title"), true);
		if (titleTag.size() != 1)
			return null;
		
		// Parse the instrument number.
		String[] title = titleTag.elementAt(0).toPlainTextString().trim()
				.split("-");
		if (title.length < 2)
			return null;
		return title[1].trim();
	}

	/**
	 * Parse and add the names in the result map.
	 * 
	 * @param key
	 * @param resultMap
	 * @param nString
	 *            The names - delimited by <br />
	 * 
	 */
	public static void addNames(String key, ResultMap resultMap, String nString, long searchId) {
		String[] namesArray = nString.split("<br />");

		@SuppressWarnings("rawtypes")
		ArrayList<List> body = new ArrayList<List>();
		for (String name : namesArray) {
			String[] names = StringFormats.parseNameFML(name,
					new Vector<String>(), false);
			String[] types = GenericFunctions.extractAllNamesType(names);
			String[] otherTypes = GenericFunctions
					.extractAllNamesOtherType(names);
			String[] suffixes = GenericFunctions.extractAllNamesSufixes(names);

			GenericFunctions.addOwnerNames(name, names, suffixes[0],
					suffixes[1], types, otherTypes,
					NameUtils.isCompany(names[2]),
					NameUtils.isCompany(names[5]), body);
		}
		try {
			resultMap.put(key, GenericFunctions.storeOwnerInSet(body, true));
			GenericFunctions1.setGranteeLanderTrustee2(resultMap, searchId, true);
		} catch (Exception e) {
			System.err.println("Error while storing owner in party names");
			e.printStackTrace();
		}
	}

}
