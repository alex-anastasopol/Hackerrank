package ro.cst.tsearch.servers.types;

import java.util.ArrayList;
import java.util.List;

import org.htmlparser.Node;
import org.htmlparser.filters.HasAttributeFilter;
import org.htmlparser.filters.TagNameFilter;
import org.htmlparser.tags.LinkTag;
import org.htmlparser.tags.TableColumn;
import org.htmlparser.tags.TableRow;
import org.htmlparser.tags.TableTag;
import org.htmlparser.util.NodeList;
import org.htmlparser.util.ParserException;

import ro.cst.tsearch.extractor.xml.GenericFunctions2;
import ro.cst.tsearch.extractor.xml.ResultMap;
import ro.cst.tsearch.extractor.xml.ResultTable;
import ro.cst.tsearch.servers.response.PropertyIdentificationSet.PropertyIdentificationSetKey;
import ro.cst.tsearch.servers.response.ServerResponse;

public class FLMartinAO extends FLGenericGovernmaxAO {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	public FLMartinAO(long searchId) {
		super(searchId);
	}

	public FLMartinAO(String rsRequestSolverName, String rsSitePath,
			String rsServerID, String rsPrmNameLink, long searchId,
			int miServerID) {
		super(rsRequestSolverName, rsSitePath, rsServerID, rsPrmNameLink,
				searchId, miServerID);
	}

	private String getLink(TableColumn column) {
		NodeList links = column.getChildren().extractAllNodesThatMatch(
				new TagNameFilter("a"));
		if (links.size() < 1)
			return null;
		return ((LinkTag) links.elementAt(0)).getLink();
	}

	@Override
	public String getDetails(String rsResponse, StringBuilder accountId)
			throws ParserException {
		org.htmlparser.Parser htmlParser = org.htmlparser.Parser.createParser(
				rsResponse, null);
		NodeList tables = htmlParser.parse(null).extractAllNodesThatMatch(
				new TagNameFilter("table"), true);
		NodeList tableList = tables.extractAllNodesThatMatch(
				new HasAttributeFilter("BORDERCOLOR", "Lime"), true);
		if (tableList.size() < 1) {
			logger.error("Parse details error: Unable to find top table.");
			return null;
		}

		TableTag topTable = (TableTag) tableList.elementAt(0);
		String pin = getParcelId(topTable);
		accountId.append(pin);

		// If from memory - use it as is
		if (rsResponse.contains("<HTML") == false
				&& rsResponse.contains("<html") == false) {
			return rsResponse;
		}

		StringBuilder details = new StringBuilder();
		details.append(topTable.toHtml());

		TableTag detailsTable = null, menuTable = null;
		for (int i = 0; i < tables.size(); i++) {
			Node table = tables.elementAt(i);
			if (table.toHtml().contains("Owner Information&nbsp;"))
				detailsTable = (TableTag) table;
			if (table.toHtml().contains("&nbsp;Tabs"))
				menuTable = (TableTag) table;
		}

		// Append details.
		if (detailsTable == null || menuTable == null) {
			logger.error("Parse details error: Unable to find "
					+ (detailsTable == null ? "details" : "menu") + " table.");
			return null;
		}
		detailsTable.setAttribute("id", "details");
		detailsTable.setAttribute("parcel_id", pin);
		details.append(detailsTable.toHtml());

		TableRow[] rows = menuTable.getRows();
		String assesmentLink = null, salesLink = null;
		for (TableRow row : rows) {
			TableColumn[] columns = row.getColumns();
			if (columns.length < 1)
				continue;
			if (row.toHtml().contains("Assessments")) {
				assesmentLink = getLink(columns[0]);
			} else if (row.toHtml().contains("Sales")) {
				salesLink = getLink(columns[0]);
			}
		}

		// Append assessment history.
		details.append(getLinkContent(assesmentLink, "assessment"));

		// Generate sales link.
		String sessionId = ro.cst.tsearch.connection.http2.FLGenericGovernmaxAO
				.getSessionId(salesLink);
		salesLink = getDataSite().getLink()
				+ "/GRM/tab_sale_v1001.asp?t_nm=sale&l_cr=2&t_wc=|parcelid="
				+ pin + "&sid=" + sessionId;

		// Append sales history.
		String salesDetails = getLinkContent(salesLink, "sales");
		salesDetails = salesDetails
				.replace(
						"<TABLE WIDTH=100% BORDER=0 BORDERCOLOR=Black VALIGN=TOP CELLSPACING=1 CELLPADDING=1>",
						"");
		salesDetails = salesDetails.replace(
				"</TABLE></TABLE></TD>\n</TR>\n</TABLE>", "");
		details.append(salesDetails);
		return "<table width=\"80%\" border=\"0\" align=\"center\" cellpadding=\"0\" cellspacing=\"0\"><tr><td>"
				+ details.toString() + "</td></tr></table>";
	}

	protected String getLinkContent(String link, String id) {
		String html = getLinkContents(link);
		String[] split = html.split("<!--START TAB-->");
		if (split.length < 2)
			return null;
		split = split[1].split("<!--END TAB-->");
		if (split.length < 2)
			return null;
		return split[0].replaceFirst("(?i)table", "table id=\"" + id + "\"");
	}

	private String getParcelId(TableTag table) {
		TableRow[] rows = table.getRows();
		if (rows.length < 2)
			return "";
		TableColumn[] cols = rows[1].getColumns();
		if (cols.length < 1)
			return "";
		return ctrim(cols[0]);
	}

	public static String getPlainPrice(String price) {
		return price.replaceAll("\\$|,", "").replaceAll("(\\d+)\\.(.*)", "$1");
	}

	@Override
	protected Object parseAndFillResultMap(ServerResponse response,
			String detailsHtml, ResultMap map) {
		map.put("OtherInformationSet.SrcType", "AO");
		detailsHtml = detailsHtml.replaceAll("<tr><tr>", "<tr>");
		org.htmlparser.Parser htmlParser = org.htmlparser.Parser.createParser(
				detailsHtml, null);
		try {
			NodeList nodeList = htmlParser.parse(null);

			NodeList nodes = nodeList.extractAllNodesThatMatch(
					new HasAttributeFilter("id", "details"), true);
			TableTag table = (TableTag) nodes.elementAt(0);

			map.put(PropertyIdentificationSetKey.PARCEL_ID.getKeyName(),
					table.getAttribute("parcel_id"));

			TableRow[] rows = table.getRows();
			String owner = null, assessmentValue = null, legal = null, address = "";
			for (TableRow row : rows) {
				TableColumn[] columns = row.getColumns();
				if (row.toHtml().contains("Owner(Current)"))
					// Get owner information.
					owner = ctrim(columns[1]);
				else if (row.toHtml().contains("Market Land Value"))
					assessmentValue = getPlainPrice(ctrim(columns[1]));
				else if (row.toHtml().contains("Legal Description&nbsp;")) {
					// Get address
					nodes = columns[0].getChildren().extractAllNodesThatMatch(
							new TagNameFilter("table"), true);
					if (nodes.size() > 0) {
						try {
							TableRow[] rows2 = ((TableTag) nodes.elementAt(0)).getRows();
							for (TableRow r : rows2) {
								if (r.toHtml().contains("Parcel Address"))
									address = r.getColumns()[1].toPlainTextString();
							}
						} catch (Exception e) {
							e.printStackTrace();
							continue;
						}
					}
					// Get legal description.
					nodes = columns[1].getChildren().extractAllNodesThatMatch(
							new TagNameFilter("table"), true);
					if (nodes.size() > 0) {
						try {
							TableRow[] rows2 = ((TableTag) nodes.elementAt(0)).getRows();
							for (TableRow r : rows2) {
								if (r.toHtml().contains("Legal Description"))
									legal = r.getColumns()[1].toPlainTextString();
							}
						} catch (Exception e) {
							e.printStackTrace();
							continue;
						}
					}
				}

			}
			// Parse owner names.
			ro.cst.tsearch.servers.functions.FLMartinAO.putOwnerNames(map, owner);
			
			// Parse address
			ro.cst.tsearch.servers.functions.FLMartinAO.putAddress(map, ctrim(address));

			// Parse legal info.
			ro.cst.tsearch.servers.functions.FLMartinAO.putLegal(map, ctrim(legal));

			// Put assessment information.
			map.put("PropertyAppraisalSet.TotalAssessment", assessmentValue);

			// Put sales info.
			putSalesInfo(map, nodeList);

		} catch (Exception e) {
			e.printStackTrace();
			logger.error("Could not fill result map. ", e);
		}
		return null;
	}

	private static void putSalesInfo(ResultMap map, NodeList nodeList) {
		NodeList nodes = nodeList.extractAllNodesThatMatch(
				new HasAttributeFilter("id", "sales"), true);
		TableTag table = (TableTag) nodes.elementAt(0);
		nodes = table.getChildren().extractAllNodesThatMatch(
				new TagNameFilter("table"), true);

		@SuppressWarnings("rawtypes")
		List<List> body = new ArrayList<List>();
		List<String> line = null;

		for (int i = 0; i < nodes.size(); i++) {
			String book = "", page = "", date = "", price = "", instrNo = "", docType = "";
			table = (TableTag) nodes.elementAt(i);
			TableRow[] rows = table.getRows();

			TableColumn[] columns = rows[1].getColumns();
			// Parse book and page.
			String[] bookPage = ctrim(columns[5]).split(" ");
			if (bookPage.length == 2) {
				book = bookPage[0];
				page = bookPage[1];
			}
			// Parse instrument number (only if it's a number).
			try {
				instrNo = "" + Integer.parseInt(ctrim(columns[3]));
			} catch (Exception e) {
			}

			// Parse sale date and price.
			for (TableRow row : rows) {
				if (row.toHtml().contains("Sale Date"))
					date = ctrim(row.getColumns()[1]);
				if (row.toHtml().contains("Sale Price"))
					price = getPlainPrice(ctrim(row.getColumns()[1]));
//				if (row.toHtml().contains("Deed Type"))
//					docType = getPlainPrice(ctrim(row.getColumns()[1]));
//				no doctype because it only contains 2 letters and will result in a different category in ATS
			}
			if (instrNo.trim().length() == 0 && book.length() == 0
					&& page.length() == 0)
				continue;

			line = new ArrayList<String>();
			line.add(date);
			line.add(instrNo);
			line.add(book);
			line.add(page);
			line.add(docType);
			line.add(price);
			body.add(line);
		}

		if (!body.isEmpty()) {
			ResultTable rt = new ResultTable();
			String[] header = { "InstrumentDate", "InstrumentNumber", "Book",
					"Page", "DocumentType", "SalesPrice" };
			rt = GenericFunctions2.createResultTable(body, header);
			map.put("SaleDataSet", rt);
		}
		logger.info("");
	}
}
