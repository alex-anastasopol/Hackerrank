/**
 * 
 */
package ro.cst.tsearch.servers.types;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.htmlparser.Node;
import org.htmlparser.filters.HasAttributeFilter;
import org.htmlparser.filters.TagNameFilter;
import org.htmlparser.tags.InputTag;
import org.htmlparser.tags.TableColumn;
import org.htmlparser.tags.TableRow;
import org.htmlparser.tags.TableTag;
import org.htmlparser.util.NodeList;

import ro.cst.tsearch.extractor.xml.GenericFunctions2;
import ro.cst.tsearch.extractor.xml.ResultMap;
import ro.cst.tsearch.extractor.xml.ResultTable;
import ro.cst.tsearch.parser.SimpleHtmlParser;
import ro.cst.tsearch.parser.SimpleHtmlParser.Form;
import ro.cst.tsearch.servers.TSConnectionURL;
import ro.cst.tsearch.servers.response.ServerResponse;
import ro.cst.tsearch.servers.response.OtherInformationSet.OtherInformationSetKey;
import ro.cst.tsearch.servers.response.PropertyIdentificationSet.PropertyIdentificationSetKey;
import ro.cst.tsearch.servers.response.TaxHistorySet.TaxHistorySetKey;

/**
 * @author vladb
 *
 */
public class NVElkoTR extends NVGenericCountyTR {

	private static final long serialVersionUID = 1L;

	public NVElkoTR(String rsRequestSolverName, String rsSitePath,
			String rsServerID, String rsPrmNameLink, long searchId,
			int miServerID) {
		super(rsRequestSolverName, rsSitePath, rsServerID, rsPrmNameLink,
				searchId, miServerID);
	}

	@Override
	protected TableTag extractIntermediaryTable(NodeList nodeList) {
		
		TableTag interTable = (TableTag) nodeList
			.extractAllNodesThatMatch(new TagNameFilter("body"), true)
			.elementAt(0).getChildren().extractAllNodesThatMatch(new TagNameFilter("table"), false)
			.extractAllNodesThatMatch(new HasAttributeFilter("width", "100%"), false)
			.elementAt(0);
	
		return interTable;
	}
	
	@Override
	protected String processIntermediaryLink(TableRow row, StringBuilder rowHtml) {
		
		String link = CreatePartialLink(TSConnectionURL.idPOST) + "/cgi-bin/tcw100p?";
		NodeList inputList = row.getChildren().extractAllNodesThatMatch(new TagNameFilter("input"), true);
		for(int j = 0; j < inputList.size(); j++) {
			InputTag input = (InputTag) inputList.elementAt(j);
			String name = input.getAttribute("name");
			String value = input.getAttribute("value");
			link += name + "=" + value + "&";
		}
		rowHtml.append(row.toHtml().replaceFirst("(?is)<input[^>]*>", "<a href=\"" + link + "\">" 
				+ ((InputTag)inputList.elementAt(0)).getAttribute("value") + "</a>"));
		
		return link;
	}
	
	@Override
	protected String processFooter(String page, StringBuilder footer) {
		
		try {
			org.htmlparser.Parser htmlParser = org.htmlparser.Parser.createParser(page, null);
			NodeList nodeList = htmlParser.parse(null);
			Node navigationForm = nodeList.extractAllNodesThatMatch(new HasAttributeFilter("name", "Results"), true).elementAt(0);
			
			Form form = new SimpleHtmlParser(navigationForm.toHtml()).getForm(0);
			Map<String, String> params = form.getParams();
			
			String prevLink = CreatePartialLink(TSConnectionURL.idPOST) + "/cgi-bin/tcw100p?";
			String nextLink = CreatePartialLink(TSConnectionURL.idPOST) + "/cgi-bin/tcw100p?";
			for(Map.Entry<String, String> entry: params.entrySet()){
				String name = entry.getKey();
				String value = entry.getValue();
				if(name.equals("CGIOption")) {
					prevLink += name + "=" + "Page Up" + "&";
					nextLink += name + "=" + "Page Down" + "&";
				} else {
					prevLink += name + "=" + value + "&";
					nextLink += name + "=" + value + "&";
				}
			}
			footer.append("<tr><td colspan=\"5\" align=\"center\"><a href=\"" + prevLink + "\">Prev</a>&nbsp;&nbsp;&nbsp;" +
					"<a href=\"" + nextLink + "\">Next</a></td></tr></table>");
			
			return nextLink;
		} catch (Exception e) {
			e.printStackTrace();
		}
		
		return null;
	}
	
	@Override
	protected ResultMap parseSpecificIntermediaryRow(TableRow row) {
		return ro.cst.tsearch.servers.functions.NVElkoTR.parseIntermediaryRow(row);
	}
	@SuppressWarnings("rawtypes")
	public Object parseAndFillResultMap(ServerResponse response, String detailsHtml, ResultMap resultMap) {

		resultMap.put(OtherInformationSetKey.SRC_TYPE.getKeyName(), "TR");

		Matcher m = Pattern.compile("Parcel\\s*#\\s*(([0-9A-Z]|-)+)").matcher(detailsHtml);
		if (m.find()) {
			resultMap.put(PropertyIdentificationSetKey.PARCEL_ID.getKeyName(), m.group(1));
		}

		try {
			detailsHtml = detailsHtml.replaceAll("(?is)<br>", "\n").replaceAll("&nbsp;", "");
			org.htmlparser.Parser htmlParser = org.htmlparser.Parser.createParser(detailsHtml, null);
			NodeList nodeList = htmlParser.parse(null);

			TableTag mainTable = (TableTag) nodeList.extractAllNodesThatMatch(new HasAttributeFilter("id", "mainTable"), true).elementAt(0);
			TableTag table1 = (TableTag) mainTable.getChildren()
					.extractAllNodesThatMatch(new TagNameFilter("table"), true).elementAt(0);
			TableTag table2 = (TableTag) mainTable.getChildren()
					.extractAllNodesThatMatch(new TagNameFilter("table"), true).elementAt(1);

			String labels1 = table1.getRow(0).getColumns()[0].toPlainTextString().trim();
			String dataCell1 = table1.getRow(0).getColumns()[1].toPlainTextString().trim();
			String ownerInfo = "";
			String address = "";
			if (labels1.indexOf("Current Owner") > -1) {
				ownerInfo = dataCell1.split("\n", 3)[2];
				address = dataCell1.split("\n", 3)[1];
			} else {
				ownerInfo = dataCell1.split("\n", 2)[1];
				address = dataCell1.split("\n", 2)[0];
			}
			ro.cst.tsearch.servers.functions.NVGenericCountyTR.parseAddress(resultMap, address.split(",")[0]);

			String dataCell2 = table1.getRow(0).getColumns()[3].toPlainTextString().trim();
			String year = dataCell2.split("\n")[1];
			resultMap.put(TaxHistorySetKey.YEAR.getKeyName(), year);

			double delinquentAmount = 0d;
			TableRow[] rows = table2.getRows();
			int i = 0;
			int currentYearRowIndex = 0;
			for (TableRow row : rows) {
				if (row.toHtml().matches("(?is).*Current\\s+Year.*") && currentYearRowIndex == 0)
				{
					currentYearRowIndex = i;
					TableRow currentYearRow = rows[currentYearRowIndex - 1];
					TableColumn[] cols = currentYearRow.getColumns();
					delinquentAmount = Double.valueOf(cols[5].toPlainTextString().replaceAll(",", ""));
					resultMap.put(TaxHistorySetKey.PRIOR_DELINQUENT.getKeyName(), String.valueOf(delinquentAmount));
				}
				i++;
				String rowText = row.toPlainTextString().toLowerCase();
				TableColumn[] cols = row.getColumns();

				if (rowText.indexOf("totals") > -1) {
					String tax = cols[1].toPlainTextString().replaceAll(",", "");
					String total = cols[3].toPlainTextString().replaceAll(",", "");
					String paid = cols[4].toPlainTextString().replaceAll(",", "");
					String totalDue = String.valueOf(Double.valueOf(total) - Double.valueOf(paid));

					resultMap.put(TaxHistorySetKey.BASE_AMOUNT.getKeyName(), tax);
					resultMap.put(TaxHistorySetKey.AMOUNT_PAID.getKeyName(), paid);
					resultMap.put(TaxHistorySetKey.TOTAL_DUE.getKeyName(), totalDue);
				}
			}

			// parse payment history table
			TableTag taxHistTable = (TableTag) nodeList.extractAllNodesThatMatch(new HasAttributeFilter("id", "taxHistTable"), true).elementAt(0);
			List<List> body = new ArrayList<List>();
			rows = taxHistTable.getRows();
			for (TableRow row : rows) {
				if (row.getColumnCount() == 5) {
					String col2 = row.getColumns()[2].toPlainTextString().trim();
					String col3 = row.getColumns()[3].toPlainTextString().trim();
					if (col3.indexOf("-") > -1) {
						List<String> line = new ArrayList<String>();
						line.add(col2);
						line.add(col3.replaceAll("-", ""));
						body.add(line);
					}
				}
			}
			String[] header = { "ReceiptDate", "ReceiptAmount" };
			ResultTable rt = GenericFunctions2.createResultTable(body, header);
			if (rt != null) {
				resultMap.put("TaxHistorySet", rt);
			}

			ro.cst.tsearch.servers.functions.NVGenericCountyTR.parseNames(resultMap, ownerInfo, true);

		} catch (Exception e) {
			e.printStackTrace();
		}
		return null;
	}
}
