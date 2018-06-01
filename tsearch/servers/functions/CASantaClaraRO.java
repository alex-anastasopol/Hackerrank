package ro.cst.tsearch.servers.functions;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.commons.lang.StringUtils;
import org.htmlparser.Node;
import org.htmlparser.filters.HasAttributeFilter;
import org.htmlparser.filters.TagNameFilter;
import org.htmlparser.tags.TableColumn;
import org.htmlparser.tags.TableRow;
import org.htmlparser.tags.TableTag;
import org.htmlparser.util.NodeList;

import ro.cst.tsearch.Search;
import ro.cst.tsearch.extractor.xml.GenericFunctions;
import ro.cst.tsearch.extractor.xml.ResultMap;
import ro.cst.tsearch.extractor.xml.StringFormats;
import ro.cst.tsearch.parser.HtmlParser3;
import ro.cst.tsearch.search.name.NameUtils;
import ro.cst.tsearch.servers.response.PropertyIdentificationSet.PropertyIdentificationSetKey;
import ro.cst.tsearch.servers.response.SaleDataSet.SaleDataSetKey;
import ro.cst.tsearch.servers.response.ServerResponse;

public class CASantaClaraRO {

	/**
	 * Parse and add the names in the result map.
	 * 
	 * @param key
	 * @param resultMap
	 * @param nString
	 *            The names - delimited by <br />
	 * 
	 */
	public static void addNames(String key, ResultMap resultMap, String nString) {
		if(StringUtils.isEmpty(nString))
			return;
		
		nString = nString.replaceAll("(?ism)EDU TRT BY TR","EDU TRUST")
				.replaceAll("(?ism)EDU TRT","EDU TRUST")
				.replaceAll("(?ism)TRT BY TR","TRUST")
				.replaceAll("(?ism)TR/TRT","TR/TRUST")
				.replaceAll("(?ism)BY TR","TR")
				.replaceAll("(?ism)FAMILY TR","FAMILY TRUST");
		
		String[] namesArray = nString.split("(?ism)(?<=\n|TRUST| AKA |/)");

		@SuppressWarnings("rawtypes")
		ArrayList<List> body = new ArrayList<List>();
		for (int i = 0; i < namesArray.length; i++) {
			String name = namesArray[i].replace("\n", "").replaceAll("(?ism)( AKA | AKA$|/)", "").trim();

			if (StringUtils.isNotEmpty(name)) {

				if (name.equals("TRUST") && i > 0) {
					name = namesArray[i - 1].replace("TR/","") + name;
				}
				
				if(name.equals("TRUST"))
					continue;

				String[] names = StringFormats.parseNameNashville(name, true);
				String[] types = GenericFunctions.extractAllNamesType(names);
				String[] otherTypes = GenericFunctions
						.extractAllNamesOtherType(names);
				String[] suffixes = GenericFunctions.extractAllNamesSufixes(names);

				GenericFunctions.addOwnerNames(name, names, suffixes[0],
						suffixes[1], types, otherTypes,
						NameUtils.isCompany(names[2]),
						NameUtils.isCompany(names[5]), body);
			}
		}
		try {
			if(body.size() > 0)
				resultMap.put(key, GenericFunctions.storeOwnerInSet(body, true));
		} catch (Exception e) {
			System.err.println("Error while storing owner in party names");
			e.printStackTrace();
		}
	}

	/**
	 * Gets the detailed document contents
	 * 
	 * @param rsResponse
	 * @return An array containing the html on the first index and the
	 *         instrument number on the second one.
	 */
	public static HashMap<String, String> getDetailedContent(String rsResponse) {
		if (rsResponse == null)
			return null;
		HashMap<String, String> result = new HashMap<String, String>();
		TableTag table = null, table2 = null;
		// Parse the html.
		if (rsResponse.startsWith("<div")) {
			// Already parsed.
			result.put("html", rsResponse);

			// Get instrument number.
			NodeList mainTableList = new HtmlParser3(rsResponse).getNodeList()
					.extractAllNodesThatMatch(new TagNameFilter("table"), true);
			if (mainTableList.size() == 0)
				return null;
			table = (TableTag) mainTableList.elementAt(0);
			table2 = (TableTag) mainTableList.elementAt(1);
			result.put("instrNr", table.getAttribute("instrNr"));
		} else {
			HtmlParser3 parser = new HtmlParser3(rsResponse);
			NodeList nodeList = parser.getNodeList();
			NodeList mainTableList = nodeList
					.extractAllNodesThatMatch(new TagNameFilter("table"), true)
					.extractAllNodesThatMatch(new HasAttributeFilter("id", "docTable"));
			if (mainTableList.size() > 0)
				table = (TableTag) mainTableList.elementAt(0);
			NodeList nameTableList = nodeList
					.extractAllNodesThatMatch(new TagNameFilter("table"), true)
					.extractAllNodesThatMatch(new HasAttributeFilter("id", "nameTable"));
			if (nameTableList.size() > 0)
				table2 = (TableTag) nameTableList.elementAt(0);
		}
		
		for (TableRow row : table.getRows()) {
			TableColumn[] columns = row.getColumns();
			if (columns.length < 2)
				continue;
			String c1 = ctrim(columns[0].toPlainTextString());
			String c2 = ctrim(columns[1].toPlainTextString());
			if (c1.contains("Document Number")) {
				// Parse the instrument number.
				result.put("instrNr", c2);
			} else if (c1.contains("Document Date")) {
				// Parse the instrument date.
				result.put("record_date", c2);
			} else if (c1.contains("Document Date")) {
				// Parse the instrument date.
				result.put("record_date", c2);
			} else if (c1.contains("Document Type")) {
				// Parse the instrument type.
				try {
					result.put("type", c2);
					result.put("type", URLDecoder.decode(c2, "UTF-8").replace("\"", ""));
				} catch (UnsupportedEncodingException e) {
					e.printStackTrace();
				}
			} else if (c1.contains("Book/Page")) {
				// Parse the book and page.
				String[] split = c2.split("/");
				if (split.length < 2)
					continue;
				String book = ctrim(split[0]);
				String page = ctrim(split[1]);
//				if (!isNumber(book) || !isNumber(page))
//					continue;
				result.put("book", book.equalsIgnoreCase("NA") ? "" : book);
				result.put("page", page.equalsIgnoreCase("NA") ? "" : page);
			} else if (c1.contains("Parcel Number")) {
				// Parse the parcel id.
				if (isNumber(c2))
					result.put("pin", c2);
			}
		}

		// Parse grantors / grantees.
		StringBuilder grantors = new StringBuilder();
		StringBuilder grantees = new StringBuilder();
		for (TableRow row : table2.getRows()) {
			TableColumn[] columns = row.getColumns();
			if (columns.length < 2)
				continue;
			String c1 = ctrim(columns[0].toPlainTextString());
			String c2 = ctrim(columns[1].toPlainTextString());
			if (c1.contains("Grantor Names") || c2.contains("Grantee Names"))
				continue;
			if (c1.length() > 0)
				grantors.append(c1 + "\n");
			if (c2.length() > 0)
				grantees.append(c2 + "\n");
		}

		result.put("grantors", grantors.toString().trim());
		result.put("grantees", grantees.toString().trim());

		if (!rsResponse.startsWith("<div")) {
			table.setAttribute("instrNr", result.get("instrNr"));
			table.setAttribute("align", "center");
			table.setAttribute("width", "700");
			table.setAttribute("cellspacing", "2");
			
			table2.setAttribute("align", "center");
			table2.setAttribute("width", "700");
			table2.setAttribute("cellspacing", "2");

			String html = "<div></div>" + table.toHtml() + "\n"
					+ table2.toHtml();
			result.put("html", html);
			result.put("instrNr", result.get("instrNr"));
		}

		return result;
	}

	protected static String ctrim(String input) {
		return input.replace("&nbsp;", " ").replace("&nbsp", " ")
				.replaceAll("\\s+", " ").trim();
	}

	private static final boolean isNumber(final String s) {
		try {
			Long.parseLong(s);
		} catch (Exception e) {
			return false;
		}
		return true;
	}
	
	public static Object parseAndFillResultsMap(ServerResponse response,
			String detailsHtml, ResultMap resultMap, Search search) {
		if (detailsHtml == null)
			return null;

		HashMap<String, String> details = getDetailedContent(detailsHtml);
		String instrumentNumber = details.get("instrNr");
		String year = "";
		resultMap.put(SaleDataSetKey.INSTRUMENT_NUMBER.getKeyName(), instrumentNumber);
		
		// Add recorded date.
		if (details.containsKey("record_date") && !"NA".equals(details.get("record_date"))){
			String recDate = details.get("record_date");
			if (StringUtils.isNotEmpty(recDate)){
				resultMap.put(SaleDataSetKey.RECORDED_DATE.getKeyName(), recDate);
				if (recDate.matches("(?is)\\d{2}/\\d{2}/\\d{4}")){
					year = recDate.substring(recDate.lastIndexOf("/") + 1);
				}
			}
		}
		
		// Add instrument date.
		if (details.containsKey("instr_date")){
			String instrDate = details.get("instr_date");
			if (StringUtils.isNotEmpty(instrDate)){
				resultMap.put(SaleDataSetKey.INSTRUMENT_DATE.getKeyName(), instrDate);
				if (StringUtils.isEmpty(year) && instrDate.matches("(?is)\\d{2}/\\d{2}/\\d{4}")){
					year = instrDate.substring(instrDate.lastIndexOf("/") + 1);
				}
			}
		}
		if (StringUtils.isNotEmpty(year) && year.matches("(?is)\\d{4}")){
			instrumentNumber = year + "-" + instrumentNumber;
		}
		resultMap.put(SaleDataSetKey.INSTRUMENT_NUMBER.getKeyName(), instrumentNumber);
		
		String docType = details.get("type");
		// Add document type.
		if (details.containsKey("type"))
			resultMap.put(SaleDataSetKey.DOCUMENT_TYPE.getKeyName(),
					docType);
		
		// Add book/page.
		if (details.containsKey("book"))
			resultMap
					.put(SaleDataSetKey.BOOK.getKeyName(), details.get("book"));
		if (details.containsKey("page"))
			resultMap
					.put(SaleDataSetKey.PAGE.getKeyName(), details.get("page"));
		
		// Add parcel id.
		if (details.containsKey("pin")){
			String pin = details.get("pin").replaceAll("NA", "");
			if(StringUtils.isNotEmpty(pin))
				resultMap.put(PropertyIdentificationSetKey.PARCEL_ID.getKeyName(), pin);
		}

		// Add grantor/grantee.
		if (details.containsKey("grantors")) {
			resultMap.put("SaleDataSet.Grantor", details.get("grantors")
					.replaceAll("<br />", " / "));
			addNames("GrantorSet", resultMap, details.get("grantors"));
		}

		if (details.containsKey("grantees")) {
			resultMap.put("SaleDataSet.Grantee", details.get("grantees")
					.replaceAll("<br />", " / "));
			addNames("GranteeSet", resultMap, details.get("grantees"));
		}

		return null; // No image link yet
	}
	
	public static Node getNodeByTypeAttributeDescription(NodeList nl, String type, Map<String, String> attributesMap, String[] description,
			boolean recursive) {
		NodeList returnList = null;

		try {
			if (attributesMap != null && !attributesMap.isEmpty()) {
				returnList = nl.extractAllNodesThatMatch(new TagNameFilter(type), recursive);

				for (Entry<String, String> e : attributesMap.entrySet()) {
					returnList = nl.extractAllNodesThatMatch(new HasAttributeFilter(e.getKey(), e.getValue()), recursive);
				}
			} else
				returnList = nl.extractAllNodesThatMatch(new TagNameFilter(type), recursive);

			for (int i = returnList.size() - 1; i >= 0; i--) {
				boolean flag = true;
				for (String s : description) {
					if (!StringUtils.containsIgnoreCase(returnList.elementAt(i).toHtml(), s)) {
						flag = false;
						break;
					}
				}
				if (flag)
					return returnList.elementAt(i);
			}

		} catch (Exception e) {
			e.printStackTrace();
		}
		return null;
	}

}
