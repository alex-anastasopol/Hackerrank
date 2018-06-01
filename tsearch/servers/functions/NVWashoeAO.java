package ro.cst.tsearch.servers.functions;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Vector;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.htmlparser.filters.HasAttributeFilter;
import org.htmlparser.tags.TableColumn;
import org.htmlparser.tags.TableRow;
import org.htmlparser.tags.TableTag;
import org.htmlparser.util.NodeList;
import org.htmlparser.util.ParserException;

import ro.cst.tsearch.extractor.xml.GenericFunctions;
import ro.cst.tsearch.extractor.xml.GenericFunctions2;
import ro.cst.tsearch.extractor.xml.ResultMap;
import ro.cst.tsearch.extractor.xml.ResultTable;
import ro.cst.tsearch.extractor.xml.StringFormats;
import ro.cst.tsearch.search.address.StandardAddress;
import ro.cst.tsearch.search.name.NameUtils;
import ro.cst.tsearch.servers.response.PropertyIdentificationSet.PropertyIdentificationSetKey;
import ro.cst.tsearch.utils.StringUtils;

/**
 * @author costi
 */

public class NVWashoeAO {
	private static int ownerCounter;

	public static String getParcelId(NodeList nodeList) {
		NodeList nodes = nodeList.extractAllNodesThatMatch(
				new HasAttributeFilter("class", "data"), true);
		if (nodes.size() < 1)
			return "";
		return nodes.elementAt(0).toPlainTextString().trim();
	}

	private static String ctrim(String value) {
		return value.replace("&nbsp;", " ").trim();
	}

	private static String getTotalAssessment(NodeList nodeList) {

		NodeList table = nodeList.extractAllNodesThatMatch(
				new HasAttributeFilter("id", "valuation-table"), true);
		if (table.size() != 1)
			return "";

		TableRow[] rows = ((TableTag) table.elementAt(0)).getRows();
		for (TableRow row : rows) {
			TableColumn[] columns = row.getColumns();
			if (columns.length < 3)
				continue;

			String s = columns[0].toPlainTextString();
			String s2 = ctrim(columns[2].toPlainTextString());
			if (s.contains("Total Assessed"))
				return ctrim(s2).replace(",", "");
		}

		return "";
	}

	private static HashMap<String, String> getInfo(NodeList nodeList) {
		HashMap<String, String> m = new HashMap<String, String>();
		ownerCounter = 0;
		NodeList infoTable = nodeList.extractAllNodesThatMatch(
				new HasAttributeFilter("id", "info-table"), true);
		if (infoTable.size() != 1)
			return m;
		TableRow[] rows = ((TableTag) infoTable.elementAt(0)).getRows();
		for (TableRow row : rows) {
			TableColumn[] columns = row.getColumns();
			if (columns.length < 2)
				continue;
			String s = ctrim(columns[0].toPlainTextString());
			String s2 = ctrim(columns[1].toPlainTextString());

			// Parcel Id
			if (s.contains("APN"))
				m.put("pin", s2);

			//
			if (s.contains("Situs"))
				m.put("address", s2);
			if (s.contains("Owner") && !s.contains("Prior Owner"))
				m.put("owner" + ++ownerCounter, s2);
			if (s.contains("Owner 2"))
				m.put("owner2", s2);
			if (s.contains("Owner 3"))
				m.put("owner3", s2);
			if (s.contains("Subdivision"))
				m.put("subdivision", s2);
			if (s.contains("Keyline Desc"))
				m.put("propertyDesc", s2);
			if (s.contains("Block")) {
				String[] split = columns[0].toPlainTextString().split(
						"&nbsp;&nbsp;");
				if (split.length < 2)
					continue;

				// Get lot number.
				String[] split2 = ctrim(split[0]).split(" ");
				if (split2.length >= 2)
					m.put("subdivisionLot", ctrim(split2[1]));

				// Get block number.
				split2 = ctrim(split[1]).split(" ");
				if (split2.length >= 2)
					m.put("subdivisionBlock", ctrim(split2[1]));
			}
			if (s.contains("Township")) {
				String[] split = columns[0].toPlainTextString().split(
						"&nbsp; &nbsp;");
				if (split.length < 3)
					continue;

				// Get section.
				String[] split2 = ctrim(split[0]).split(" ");
				if (split2.length >= 2)
					m.put("subdivisionSection", ctrim(split2[1]));

				// Get township.
				split2 = ctrim(split[1]).split(" ");
				if (split2.length >= 2)
					m.put("subdivisionTownship", ctrim(split2[1]));

				// Get range.
				split2 = ctrim(split[2]).split(" ");
				if (split2.length >= 2)
					m.put("subdivisionRange", ctrim(split2[1]));
			}
		}

		return m;
	}

	private static String composedName(String name) {
		if (name == null)
			return name;
		if (NameUtils.isCompany(name))
			return name.replace("&", "|").replace(",", "~");
		return name.replace("TRUSTEE", "").replace("TTEE", "");
	}

	private static String[] cleanOwnerName(String[] names) {
		if (NameUtils.isCompany(names[2]))
			names[2] = names[2].replace("|", "&").replace("~", ",");
		return names;
	}

	public static void parseAndFillResultMap(String detailsHtml, ResultMap m,
			long searchId, int miServerID) throws ParserException {
		m.put("OtherInformationSet.SrcType", "AO");

		org.htmlparser.Parser htmlParser = org.htmlparser.Parser.createParser(
				detailsHtml, null);
		NodeList nodeList = htmlParser.parse(null);

		HashMap<String, String> map = getInfo(nodeList);

		if (map.containsKey("pin"))
			m.put(PropertyIdentificationSetKey.PARCEL_ID.getKeyName(),
					map.get("pin"));
		if (map.containsKey("address"))
			m.put(PropertyIdentificationSetKey.ADDRESS_ON_SERVER.getKeyName(),
					map.get("address"));
		if (map.containsKey("subdivision"))
			m.put(PropertyIdentificationSetKey.SUBDIVISION_NAME.getKeyName(),
					map.get("subdivision"));
		if (map.containsKey("subdivisionBlock"))
			m.put(PropertyIdentificationSetKey.SUBDIVISION_BLOCK.getKeyName(),
					map.get("subdivisionBlock"));
		if (map.containsKey("subdivisionLot"))
			m.put(PropertyIdentificationSetKey.SUBDIVISION_LOT_NUMBER
					.getKeyName(), map.get("subdivisionLot"));
		if (map.containsKey("subdivisionSection"))
			m.put(PropertyIdentificationSetKey.SUBDIVISION_SECTION.getKeyName(),
					map.get("subdivisionSection"));
		if (map.containsKey("subdivisionTownship"))
			m.put(PropertyIdentificationSetKey.SUBDIVISION_TOWNSHIP
					.getKeyName(), map.get("subdivisionTownship"));
		if (map.containsKey("subdivisionRange"))
			m.put(PropertyIdentificationSetKey.SUBDIVISION_RANGE.getKeyName(),
					map.get("subdivisionRange"));
		if (map.containsKey("propertyDesc"))
			m.put(PropertyIdentificationSetKey.PROPERTY_DESCRIPTION
					.getKeyName(), map.get("propertyDesc"));
		m.put("PropertyAppraisalSet.TotalAssessment",
				getTotalAssessment(nodeList));

		// Parse owner names.
		@SuppressWarnings("rawtypes")
		List<List> body = new ArrayList<List>();
		for (int i = 1; i <= ownerCounter; i++) {
			String owner = composedName(map.get("owner" + i));
			if (owner == null)
				continue;
			String[] names = StringFormats.parseNameFMWFWML(owner,
					new Vector<String>());
			String[] types = GenericFunctions.extractAllNamesType(names);
			String[] otherTypes = GenericFunctions
					.extractAllNamesOtherType(names);
			String[] suffixes = GenericFunctions.extractAllNamesSufixes(names);

			names = cleanOwnerName(names);
			GenericFunctions.addOwnerNames(names, suffixes[0], suffixes[1],
					types, otherTypes, NameUtils.isCompany(names[2]),
					NameUtils.isCompany(names[5]), body);
		}

		// Store owners.
		try {
			GenericFunctions.storeOwnerInPartyNames(m, body, true);
		} catch (Exception e) {
			e.printStackTrace();
		}

		// Store address.
		String address = map.get("address");
		if (address != null) {
			String streetNo = StringFormats.StreetNo(address.trim());
			String streetName = StringFormats.StreetName(address.trim());

			if (StringUtils.isEmpty(streetNo)) {
				StandardAddress tokAddr = new StandardAddress(address);
				streetNo = tokAddr
						.getAddressElement(StandardAddress.STREET_NUMBER);
				if (StringUtils.isNotEmpty(streetNo)) {
					streetName = address.replaceFirst(streetNo, "").trim();
				}
			}
			if (streetNo.startsWith("0")) {
				streetNo = streetNo.replaceFirst("^0+", "");
			}

			m.put(PropertyIdentificationSetKey.STREET_NO.getKeyName(), streetNo);
			m.put(PropertyIdentificationSetKey.STREET_NAME.getKeyName(),
					streetName);
		}

		putSales(m, nodeList);
	}

	private static final boolean isNumber(final String s) {
		try {
			Long.parseLong(s);
		} catch (Exception e) {
			return false;
		}
		return true;
	}

	private static void putSales(ResultMap m, NodeList nodeList) {
		NodeList salesTable = nodeList.extractAllNodesThatMatch(
				new HasAttributeFilter("id", "sales-transfers"), true);
		if (salesTable.size() != 1)
			return;
		TableRow[] rows = ((TableTag) salesTable.elementAt(0)).getRows();
		if (rows.length < 4)
			return;
		@SuppressWarnings("rawtypes")
		List<List> body = new ArrayList<List>();
		List<String> line = null;
		for (int i = 3; i < rows.length; i++) {
			line = new ArrayList<String>();
			TableColumn[] columns = rows[i].getColumns();
			if (columns.length < 4)
				continue;
			if (columns[1].toPlainTextString().contains("N/A"))
				continue;
			// Grantor
			line.add(columns[0].toPlainTextString().replace("&nbsp;", "")
					.trim());
			// Instrument Number
			line.add(columns[1].toPlainTextString().replace("&nbsp;", "")
					.trim());
			// Instrument date
			line.add(columns[2].toPlainTextString().replace("&nbsp;", "")
					.trim());
			// Price
			line.add(columns[4].toPlainTextString().replace("&nbsp;", "")
					.replace(",", "").trim());

			if (!isNumber(line.get(1)))
				// Invalid instrument number
				continue;
			
			body.add(line);
		}
		if (!body.isEmpty()) {
			ResultTable rt = new ResultTable();
			String[] header = { "Grantor", "InstrumentNumber",
					"InstrumentDate", "SalesPrice" };
			rt = GenericFunctions2.createResultTable(body, header);
			m.put("SaleDataSet", rt);
		}
	}

	/**
	 * Removes unnecessary information.
	 * 
	 * @param table
	 * @return
	 */
	public static String sanitizeTable(TableTag table) {
		table.setAttribute("width", "100%");

		TableRow[] rows = table.getRows();

		int pos = -1, pos2 = -1;
		for (int i = 0; i < rows.length; i++) {
			TableRow row = rows[i];
			if (row.toPlainTextString().contains("Map Warehouse")) {
				pos = table.findPositionOf(row);
			}
			if (row.toPlainTextString().contains("Bldg Type")) {
				pos2 = table.findPositionOf(row);
			}
		}
		if (pos >= 0) {
			table.removeChild(pos);
		}
		if (pos2 >= 0) {
			table.removeChild(pos2);
		}

		return table.toHtml();
	}

	/**
	 * Removes link tags from the input.
	 * 
	 * @param input
	 * @return
	 */
	public static String stripLinks(String input) {
		String regex = "(<a.*>)(.*)(</a>)";
		Pattern p = Pattern.compile(regex);
		Matcher m = p.matcher(input);
		String output = m.replaceAll("$2");
		return output;
	}
}
