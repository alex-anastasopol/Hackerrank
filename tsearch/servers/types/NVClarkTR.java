package ro.cst.tsearch.servers.types;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Vector;

import org.htmlparser.Node;
import org.htmlparser.filters.HasAttributeFilter;
import org.htmlparser.filters.TagNameFilter;
import org.htmlparser.nodes.TagNode;
import org.htmlparser.tags.LinkTag;
import org.htmlparser.tags.TableColumn;
import org.htmlparser.tags.TableRow;
import org.htmlparser.tags.TableTag;
import org.htmlparser.util.NodeList;

import ro.cst.tsearch.bean.SearchAttributes;
import ro.cst.tsearch.exceptions.ServerResponseException;
import ro.cst.tsearch.extractor.xml.Bridge;
import ro.cst.tsearch.extractor.xml.ResultMap;
import ro.cst.tsearch.extractor.xml.ResultTable;
import ro.cst.tsearch.generic.Util;
import ro.cst.tsearch.parser.HtmlParser3;
import ro.cst.tsearch.search.FunctionStatesIterator;
import ro.cst.tsearch.search.filter.DuplicateInstrumentFilterResponse;
import ro.cst.tsearch.search.filter.FilterResponse;
import ro.cst.tsearch.search.filter.TaxYearFilterResponse;
import ro.cst.tsearch.search.filter.newfilters.address.AddressFilterFactory;
import ro.cst.tsearch.search.filter.newfilters.name.NameFilterFactory;
import ro.cst.tsearch.search.iterator.ModuleStatesIteratorFactory;
import ro.cst.tsearch.search.module.ConfigurableNameIterator;
import ro.cst.tsearch.servers.TSConnectionURL;
import ro.cst.tsearch.servers.info.TSServerInfo;
import ro.cst.tsearch.servers.info.TSServerInfoModule;
import ro.cst.tsearch.servers.response.LinkInPage;
import ro.cst.tsearch.servers.response.ParsedResponse;
import ro.cst.tsearch.servers.response.PropertyIdentificationSet.PropertyIdentificationSetKey;
import ro.cst.tsearch.servers.response.ServerResponse;
import ro.cst.tsearch.servers.response.TaxHistorySet.TaxHistorySetKey;
import ro.cst.tsearch.utils.StringUtils;
import ro.cst.tsearch.utils.Tidy;

import com.stewart.ats.base.document.DocumentI;
import com.stewart.ats.base.document.TaxDocumentI;

/**
 * 
 * @author Oprina George
 * 
 *         Feb 25, 2011
 */

public class NVClarkTR extends TSServer {

	private static final long serialVersionUID = 1L;

	public NVClarkTR(long searchId) {
		super(searchId);
	}

	public NVClarkTR(String rsRequestSolverName, String rsSitePath,
			String rsServerID, String rsPrmNameLink, long searchId,
			int miServerID) {
		super(rsRequestSolverName, rsSitePath, rsServerID, rsPrmNameLink,
				searchId, miServerID);
	}

	@Override
	protected void ParseResponse(String sAction, ServerResponse Response,
			int viParseID) throws ServerResponseException {
		String rsResponse = Response.getResult();
		ParsedResponse parsedResponse = Response.getParsedResponse();

		switch (viParseID) {
		case ID_SEARCH_BY_NAME:
			if (rsResponse
					.indexOf("No parcel met the criteria! No Matches Found!") > -1
					|| (rsResponse.contains("runtime") && rsResponse
							.contains("error"))) {
				Response.getParsedResponse()
						.setError(
								"No results found for your query! Please change your search criteria and try again.");
				return;
			}

			// directly final results for search by parcel ID
			if (rsResponse.contains("Printable Page")) {
				ParseResponse(sAction, Response, ID_DETAILS);
			}

			StringBuilder outputTable = new StringBuilder();
			Collection<ParsedResponse> smartParsedResponses = smartParseIntermediary(
					Response, rsResponse, outputTable);

			if (smartParsedResponses.size() > 0) {
				parsedResponse.setResultRows(new Vector<ParsedResponse>(
						smartParsedResponses));
				parsedResponse.setOnlyResponse(outputTable.toString());
				parsedResponse.setAttribute(ParsedResponse.SERVER_ROW_RESPONSE,
						outputTable.toString());
			}
			break;
		case ID_DETAILS:
		case ID_SAVE_TO_TSD:

			StringBuilder accountId = new StringBuilder();
			String details = getDetails(rsResponse, accountId);

			String filename = accountId + ".html";

			if (viParseID != ID_SAVE_TO_TSD) {
				String originalLink = sAction.replace("?", "&") + "&"
						+ Response.getRawQuerry();
				String sSave2TSDLink = getLinkPrefix(TSConnectionURL.idPOST)
						+ originalLink;

				HashMap<String, String> data = new HashMap<String, String>();
				loadDataHash(data);
				if (isInstrumentSaved(accountId.toString(), null, data)) {
					details += CreateFileAlreadyInTSD();
				} else {
					mSearch.addInMemoryDoc(sSave2TSDLink, rsResponse);
					details = addSaveToTsdButton(details, sSave2TSDLink,
							ID_DETAILS);
				}
				Response.getParsedResponse().setPageLink(
						new LinkInPage(sSave2TSDLink, originalLink,
								TSServer.REQUEST_SAVE_TO_TSD));
				Response.getParsedResponse().setResponse(details);

			} else {
				smartParseDetails(Response, details);

				msSaveToTSDFileName = filename;
				Response.getParsedResponse().setFileName(
						getServerTypeDirectory() + msSaveToTSDFileName);
				Response.getParsedResponse().setResponse(details);

				msSaveToTSDResponce = details + CreateFileAlreadyInTSD();

			}

			break;
		case ID_GET_LINK:
			ParseResponse(
					sAction,
					Response,
					rsResponse.contains("Sort Search Results") ? ID_SEARCH_BY_NAME
							: ID_DETAILS);
			break;
		default:
			break;
		}
	}

	protected void loadDataHash(HashMap<String, String> data) {
		if (data != null) {
			data.put("type", "CNTYTAX");
		}
	}

	protected String getDetails(String rsResponse, StringBuilder accountId) {
		try {
			StringBuilder details = new StringBuilder();
			if (rsResponse
					.contains("No Parcel Met the Search Criteria! No Matches Found!"))
				return "Nothing found for this criteria";

			org.htmlparser.Parser htmlParser = org.htmlparser.Parser
					.createParser(Tidy.tidyParse(rsResponse
							.replaceAll("</TR>[^<]*<TD", "</TR><TR><TD")
							.replaceAll("</TR>[^<]*</TR>", "</TR>"), null), null);
			NodeList nodeList = htmlParser.parse(null);

			/* If from memory - use it as is */
			if (!rsResponse.contains("<html")) {
				NodeList headerList = nodeList.extractAllNodesThatMatch(
						new TagNameFilter("table"), true);

				if (headerList.size() == 0) {
					return null;
				}
				return rsResponse;
			}

			NodeList tableList = nodeList
					.extractAllNodesThatMatch(new TagNameFilter("table"), true)
					.extractAllNodesThatMatch(
							new HasAttributeFilter("width", "650"), true)
					.extractAllNodesThatMatch(
							new HasAttributeFilter("cellspacing", "0"), true)
					.extractAllNodesThatMatch(
							new HasAttributeFilter("cellpadding", "0"), true)
					.extractAllNodesThatMatch(
							new HasAttributeFilter("border", "1"), true)
					.extractAllNodesThatMatch(
							new HasAttributeFilter("bgcolor"), true);
			if (tableList.size() == 0) {
				return null;
			}

			String account = HtmlParser3
					.getValueFromAbsoluteCell(0, 1,
							HtmlParser3.findNode(tableList, "Parcel ID"), "",
							false).replace("&nbsp;", "").trim();
			accountId.append(account + "");

			NodeList tables = new NodeList();

			NodeList nodes = tableList.elementAt(0).getChildren()
					.extractAllNodesThatMatch(new TagNameFilter("table"), true);

			Node[] nodes_array = nodes.toNodeArray();

			Arrays.sort(nodes_array,
					new ro.cst.tsearch.servers.functions.NVClarkTR.NodeComp());

			// header
			TableTag n = ro.cst.tsearch.servers.functions.NVClarkTR
					.getTableByContent(
							nodes_array,
							new String[] { "Property Account Inquiry - Summary Screen" });

			if (n != null) {
				n.setAttribute("id", "header");
				tables.add(n);
			}

			// table one (pin ....)
			n = ro.cst.tsearch.servers.functions.NVClarkTR.getTableByContent(
					nodes_array, new String[] { "Parcel ID", "Tax Year",
							"District" });

			if (n != null) {
				n.setAttribute("id", "pin");
				tables.add(n);
			}

			// table with address
			n = ro.cst.tsearch.servers.functions.NVClarkTR.getTableByContent(
					nodes_array, new String[] { "Situs Address:",
							"Legal Description:" });

			if (n != null) {
				n.setAttribute("id", "address");
				tables.add(n);
			}

			// table under address
			NodeList children = new NodeList();

			n = ro.cst.tsearch.servers.functions.NVClarkTR.getTableByContent(
					nodes_array, new String[] { "Status:" });

			if (n != null) {
				n.setAttribute("id", "property_stat");
				children.add(n);
			}

			n = ro.cst.tsearch.servers.functions.NVClarkTR.getTableByContent(
					nodes_array, new String[] { "Property Characteristics" });

			if (n != null) {
				n.setAttribute("id", "property_carac");
				children.add(n);
			}

			n = ro.cst.tsearch.servers.functions.NVClarkTR.getTableByContent(
					nodes_array, new String[] { "Property Values" });

			if (n != null) {
				n.setAttribute("id", "property_val");
				children.add(n);
			}

			n = ro.cst.tsearch.servers.functions.NVClarkTR.getTableByContent(
					nodes_array, new String[] { "Property Documents" });

			if (n != null) {
				n.setAttribute("id", "property_doc");
				children.add(n);
			}
			StringBuilder property = new StringBuilder();

			property.append("<table><tr>");
			for (int i = 0; i < children.size(); i++) {
				property.append("<td valign=\"top\" align=\"left\">"
						+ children.elementAt(i).toHtml() + "</td>");
			}
			property.append("</tr></table>");

			// table with owners
			n = ro.cst.tsearch.servers.functions.NVClarkTR.getTableByContent(
					nodes_array, new String[] { "Role", "Name", "Address",
							"Since" });

			if (n != null) {
				n.setAttribute("id", "owners");
				n.setAttribute("border", "1");
				tables.add(n);
			}

			// table with summary
			n = ro.cst.tsearch.servers.functions.NVClarkTR.getTableByContent(
					nodes_array, new String[] { "Summary", "Item", "Amount" });

			if (n != null) {
				n.setAttribute("id", "summary");
				n.setAttribute("border", "1");
				tables.add(n);
			}

			// table with amounts
			n = ro.cst.tsearch.servers.functions.NVClarkTR.getTableByContent(
					nodes_array, new String[] { "Detail of Amount Due",
							"Charge Category", "Charge", "Balance Due" });

			if (n != null) {
				n.setAttribute("id", "amounts");
				tables.add(n);
			}

			// table with rcpt
			n = ro.cst.tsearch.servers.functions.NVClarkTR.getTableByContent(
					nodes_array, new String[] { "Payment Posted",
							"Receipt No.", "Due Charges", "Amount Paid" });

			if (n != null) {
				n.setAttribute("id", "rcpt");
				tables.add(n);
			}

			details.append("<table align=\"center\" border=\"1\"><tr><td align=\"center\">");

			boolean flag = false;
			for (int i = 0; i < tables.size(); i++) {
				if (i == 3) {
					flag = true;
					details.append(property);
				}
				details.append(tables.elementAt(i).toHtml());
			}
			if (!flag)
				details.append(property);

			details.append("</td></tr>");
			details.append("</table>");
			return details.toString();
		} catch (Exception e) {
			logger.error("Error while getting details", e);
		}
		return null;
	}

	public Collection<ParsedResponse> smartParseIntermediary(
			ServerResponse response, String table, StringBuilder outputTable) {
		Vector<ParsedResponse> intermediaryResponse = new Vector<ParsedResponse>();

		try {
			if (table
					.contains("There are: 0 records returned from your search input.")) {
				return intermediaryResponse;
			}

			org.htmlparser.Parser htmlParser = org.htmlparser.Parser
					.createParser(table, null);
			NodeList nodeList = htmlParser.parse(null);
			NodeList mainTableList = nodeList
					.extractAllNodesThatMatch(new TagNameFilter("div"), true)
					.extractAllNodesThatMatch(new TagNameFilter("table"), true)
					.extractAllNodesThatMatch(
							new HasAttributeFilter("id", "Table7"), true);

			if (mainTableList.size() != 1) {
				return intermediaryResponse;
			}
			NodeList tdList = nodeList
					.extractAllNodesThatMatch(new TagNameFilter("td"), true)
					.extractAllNodesThatMatch(
							new HasAttributeFilter("BGColor", "FFFFFF"), true)
					.extractAllNodesThatMatch(
							new HasAttributeFilter("CLASS", "CellData"), true)
					.extractAllNodesThatMatch(
							new HasAttributeFilter("ALIGN", "LEFT"), true);

			TableTag tableTag = makeTable(tdList);

			org.htmlparser.Parser htmlParser_aux = org.htmlparser.Parser
					.createParser(tableTag.toHtml(), null);
			NodeList nodeList_aux = htmlParser_aux.parse(null);

			TableTag tableTag_aux = (TableTag) nodeList_aux.elementAt(0);
			TableRow[] rows = tableTag_aux.getRows();
			for (int i = 0; i < rows.length; i++) {
				TableRow row = rows[i];
				if (row.getColumnCount() == 7) {

					LinkTag linkTag = ((LinkTag) row.getColumns()[0]
							.getFirstChild().getFirstChild());

					String link = CreatePartialLink(TSConnectionURL.idGET)
							+ linkTag.extractLink().trim()
									.replaceAll("\\s", "%20");

					linkTag.setLink(link);

					ParsedResponse currentResponse = new ParsedResponse();
					currentResponse.setAttribute(
							ParsedResponse.SERVER_ROW_RESPONSE, row.toHtml());
					currentResponse.setOnlyResponse(row.toHtml());
					currentResponse.setPageLink(new LinkInPage(link, link,
							TSServer.REQUEST_SAVE_TO_TSD));

					ResultMap m = ro.cst.tsearch.servers.functions.NVClarkTR
							.parseIntermediaryRow(row, searchId);
					Bridge bridge = new Bridge(currentResponse, m, searchId);

					DocumentI document = (TaxDocumentI) bridge.importData();
					currentResponse.setDocument(document);

					intermediaryResponse.add(currentResponse);
				}
			}
			response.getParsedResponse()
					.setHeader(
							"<table width=\"100%\" border=\"1\" cellspacing=\"0\" cellpadding=\"3\">\n"
									+ "<tr>"
									+ "<TH bgcolor=\"#99CCFF\" ALIGN = \"CENTER\" WIDTH=\"84\" ><P><bold><FONT FACE=\"Arial\" COLOR=\"#000000\">Parcel ID</FONT></bold></TH> "
									+ "<TH bgcolor=\"#99CCFF\" ALIGN = \"CENTER\" WIDTH=\"180\"><P><FONT FACE=\"Arial\" COLOR=\"#000000\">NAME</FONT></TH>"
									+ "<TH bgcolor=\"#99CCFF\" ALIGN = \"CENTER\" WIDTH=\"180\"><P><FONT FACE=\"Arial\" COLOR=\"#000000\">MAILING ADDRESS</FONT></TH>"
									+ "<TH bgcolor=\"#99CCFF\" ALIGN = \"CENTER\" WIDTH=\"40\"><P><FONT FACE=\"Arial\" COLOR=\"#000000\">ROLE</FONT></TH>"
									+ "<TH bgcolor=\"#99CCFF\" ALIGN = \"CENTER\" WIDTH=\"40\"><P><FONT FACE=\"Arial\" COLOR=\"#000000\">FROM</FONT></TH>"
									+ "<TH bgcolor=\"#99CCFF\" ALIGN = \"CENTER\" WIDTH=\"40\"><P><FONT FACE=\"Arial\" COLOR=\"#000000\">TO</FONT></TH>"
									+ "<TH bgcolor=\"#99CCFF\" ALIGN = \"CENTER\" WIDTH=\"180\"><P><FONT FACE=\"Arial\" COLOR=\"#000000\">LOCATION ADDRESS</FONT></TH>"
									+ "</tr>");
			response.getParsedResponse().setFooter("</table>");
			outputTable.append(table);
		} catch (Exception e) {
			logger.error("Error while parsing intermediary data", e);
		}

		return intermediaryResponse;
	}

	private TableTag makeTable(NodeList tdList) {
		TableTag t = new TableTag();
		NodeList n = new NodeList();
		for (int i = 2; i < tdList.size(); i += 7) {
			TableRow r = new TableRow();
			NodeList td = new NodeList();
			for (int j = 0; j < 7; j++) {
				td.add(tdList.elementAt(i + j));
			}
			r.setChildren(td);
			TagNode tag = new TagNode();
			tag.setAttribute("", "/TR");
			r.setEndTag(tag);

			n.add(r);
		}
		t.setChildren(n);
		TagNode tag = new TagNode();
		tag.setAttribute("", "/TABLE");
		t.setEndTag(tag);
		return t;
	}

	protected Object parseAndFillResultMap(ServerResponse response,
			String detailsHtml, ResultMap resultMap) {
		try {
			String rsResponse = response.getResult();
			if (!StringUtils.isNotEmpty(rsResponse)) {
				return null;
			}
			org.htmlparser.Parser htmlParser = org.htmlparser.Parser
					.createParser(detailsHtml, null);

			NodeList nodeList = htmlParser.parse(null);

			NodeList tableList = nodeList.extractAllNodesThatMatch(
					new TagNameFilter("table"), true);

			// type of search
			resultMap.put("OtherInformationSet.SrcType", "TR");

			// parcel id
			String parcel = HtmlParser3.getValueFromAbsoluteCell(0, 1,
					HtmlParser3.findNode(tableList, "Parcel ID"), "", false)
					.replace("&nbsp;", "");
			resultMap.put("PropertyIdentificationSet.ParcelID", parcel.trim());
			resultMap.put("PropertyIdentificationSet.ParcelIDParcel", parcel
					.replaceAll("-", "").trim());

			// get owner
			NodeList ownerTable = tableList
					.extractAllNodesThatMatch(new TagNameFilter("table"), true)
					.extractAllNodesThatMatch(
							new HasAttributeFilter("id", "owners"), true)
					.extractAllNodesThatMatch(
							new HasAttributeFilter("border", "1"), true)
					.extractAllNodesThatMatch(
							new HasAttributeFilter("width", "650"), true);

			if (ownerTable.size() > 0) {
				TableTag owners = (TableTag) ownerTable.elementAt(0);
				TableRow[] owner_row = owners.getRows();
				String owner = "";
				StringBuffer buf = new StringBuffer();
				String possibleCoOwner = "";
				for (int i = 1; i < owner_row.length; i++) {
					if (owner_row[i].getColumnCount() == 5
							&& owner_row[i].getColumns()[4].toPlainTextString()
									.replaceAll("(?i)&nbsp;", "").equalsIgnoreCase("current")){
						buf.append(owner_row[i].getColumns()[1]
								.toPlainTextString().replace("&nbsp;", " ") + " && ");
						if (owner_row[i].getColumns()[2].toPlainTextString().replaceAll("(?i)&nbsp;", " ").trim().startsWith("%")){
							possibleCoOwner = owner_row[i].getColumns()[2].toPlainTextString().replaceAll("(?i)&nbsp;", " ").trim();
						}
					}
				}
				if (possibleCoOwner.length() > 0){
					possibleCoOwner = possibleCoOwner.replaceAll("(?is)(%.*?)\\d+.*", "$1");
					buf.append(" ").append(possibleCoOwner);
				}

				owner = buf.toString();
				
				owner = owner.replace("& &", "&")
						.replace(" REV TR ", " REVOCABLE TRUST ")
						.replace(" REV LIV TR ", " REVOCABLE LIVING TRUST ")
						.replace(" REV LIVING TR ", " REVOCABLE LIVING TRUST ")
						.replace(" LIVING TR ", " LIVING TRUST ")
						.replace(" REVOCABLE TR ", " REVOCABLE TRUST ")
						.replace(" REVOCABLE LIV TR ", " REVOCABLE LIVING TRUST ")
						.replace(" REVOCABLE LIVING TR ", " REVOCABLE LIVING TRUST ")
						.replace(" FAM TR ", " FAMILY TRUST ")
						.replace(" FAM LIV TR ", " FAMILY LIVING TRUST ")
						.replace(" FAM LIVING TR ", " FAMILY LIVING TRUST ")
						.replace(" FAMILY TR ", " FAMILY TRUST ")
						.replace(" FAMILY LIV TR ", " FAMILY LIVING TRUST ")
						.replace(" FAMILY LIVING TR ", " FAMILY LIVING TRUST ")
						.replace(" CO-TRS ", " TRS ").replaceAll("\\s+", " ")
						.trim();
				if (owner.endsWith("&&"))
					owner = owner.substring(0, owner.length() - 2);

				resultMap.put("PropertyIdentificationSet.NameOnServer", owner);

				ro.cst.tsearch.servers.functions.NVClarkTR.parseNames(
						resultMap, 0);
			}

			// get address
			String address = HtmlParser3
					.getValueFromAbsoluteCell(0, 1,
							HtmlParser3.findNode(tableList, "Situs Address:"),
							"", false).replace("&nbsp;", "")
					.replaceAll("\\s+", " ");
			ro.cst.tsearch.servers.functions.NVClarkTR.parseAddress(resultMap,
					address);

			// legal des
			String legal_des = HtmlParser3
					.getValueFromAbsoluteCell(
							0,
							1,
							HtmlParser3.findNode(tableList,
									"Legal Description:"), "", false)
					.replace("&nbsp;", "").replaceAll("\\s+", " ");

			// parse description
			ro.cst.tsearch.servers.functions.NVClarkTR.parseLegalSummary(
					resultMap, legal_des);

			// get totalassesment
			String totalAssessment = HtmlParser3
					.getValueFromAbsoluteCell(
							0,
							1,
							HtmlParser3.findNode(tableList,
									"Total Assessed Value"), "", false)
					.replace("&nbsp;", "").replaceAll("\\s+", " ");
			resultMap.put("PropertyAppraisalSet.TotalAssessment",
					totalAssessment);

			// get total appraisal
			String totalAppraisal = totalAssessment;
			resultMap
					.put("PropertyAppraisalSet.TotalAppraisal", totalAppraisal);

			// get year
			String year = HtmlParser3
					.getValueFromAbsoluteCell(0, 1,
							HtmlParser3.findNode(tableList, "Tax Year"), "",
							false).replace("&nbsp;", "")
					.replaceAll("\\s+", " ");
			resultMap.put("TaxHistorySet.Year", year);
			
			// get district
			String district = HtmlParser3
					.getValueFromAbsoluteCell(0, 1,
							HtmlParser3.findNode(tableList, "District"), "",
							false).replace("&nbsp;", "")
					.replaceAll("\\s*", "")
			        .replaceAll("\\A0+", "");		//050 ->50
			resultMap.put(PropertyIdentificationSetKey.DISTRICT.getKeyName(), district);

			// get land
			NodeList land_list = tableList.extractAllNodesThatMatch(
					new TagNameFilter("table"), true).extractAllNodesThatMatch(
					new HasAttributeFilter("id", "property_val"), true);

			if (land_list.size() > 0) {

				String land = HtmlParser3
						.getValueFromAbsoluteCell(0, 1,
								HtmlParser3.findNode(land_list, "Land"), "",
								false).replace("&nbsp;", "")
						.replaceAll("\\s+", " ");

				resultMap.put("PropertyAppraisalSet.LandAppraisal", land);

				// get improvements
				String improvements = HtmlParser3
						.getValueFromAbsoluteCell(0, 1, HtmlParser3.findNode(land_list, "Improvements"), "", false)
						.replace("&nbsp;", "")
						.replaceAll("\\s+", " ");

				resultMap.put("PropertyAppraisalSet.ImprovementAppraisal", improvements);
			}

			NodeList summary = tableList.extractAllNodesThatMatch(new HasAttributeFilter("id", "summary"));

			String base_amount = null;
			if (summary.size() > 0) {

				// get baseamount
				base_amount = HtmlParser3
						.getValueFromAbsoluteCell(0, 1, HtmlParser3.findNode(summary, "Net Taxes"), "", false)
						.replace("&nbsp;", "")
						.replaceAll("\\s+", " ")
						.replaceAll("[$,-]", "");
				if (StringUtils.isNotEmpty(base_amount))
					resultMap.put(TaxHistorySetKey.BASE_AMOUNT.getKeyName(), base_amount);
			}

			NodeList total_due_list = tableList.extractAllNodesThatMatch(new HasAttributeFilter("id", "amounts"));

			boolean weHaveCharge = false;
			boolean weHavePaid = false;
			
			DecimalFormat df = new DecimalFormat("#.##");
			 
			double charge = 0.0;
			double due = 0.0;
			double baseAmountInstallments[] = {0.0, 0.0, 0.0, 0.0};
			double amountPaidInstallments[] = {0.0, 0.0, 0.0, 0.0};
			double totalDueInstallments[] = {0.0, 0.0, 0.0, 0.0};
			String statustInstallments[] = {"PAID", "PAID", "PAID", "PAID"};
			List<Double> paymentsDueCharges = new ArrayList<Double>();
			
			if (total_due_list.size() > 0) {
				// get total due & ap

				String adString = HtmlParser3
						.getValueFromAbsoluteCell(0, 3, HtmlParser3.findNode(total_due_list, "TOTAL Due"), "", false)
						.replaceAll("[^\\d.]", "");

				double ad = 0;

				if (org.apache.commons.lang.StringUtils.isNotEmpty(adString) && adString.matches("\\d+\\.\\d+")) {
					ad = Double.parseDouble(adString);
				}

				String chargeString = HtmlParser3
						.getValueFromAbsoluteCell(0, 1, HtmlParser3.findNode(total_due_list, "TOTAL Due"), "", false)
						.replaceAll("[^\\d.]", "");

				charge = Double.parseDouble(chargeString);
				if (org.apache.commons.lang.StringUtils.isNotEmpty(chargeString) && chargeString.matches("\\d+\\.\\d+")) {
					
					if(charge > 0) {
						base_amount = chargeString;
						double penalty = 0.0;
						if (total_due_list.size()>0) {
							Node node = total_due_list.elementAt(0);
							if (node instanceof TableTag) {
								TableTag table = (TableTag)node;
								for (int i=0;i<table.getRowCount();i++) {
									TableRow row = table.getRow(i);
									if (row.getColumnCount()>4) {
										if (row.getColumns()[1].toPlainTextString().contains("Property Tax Penalty")) {
											String penaltyString = row.getColumns()[3].toPlainTextString()
												.replaceFirst("(?is)&nbsp;", "").replaceFirst("[$,]", "").trim();
											penalty += Double.parseDouble(penaltyString);
										}
									}
								}
							}
						}
						double ba = charge - penalty;
						resultMap.put(TaxHistorySetKey.BASE_AMOUNT.getKeyName(), Double.toString(ba));
					}
					
					double ap = charge;
					
					if (ap > 0) {
						weHaveCharge = true;
						due = ad;
						resultMap.put(TaxHistorySetKey.TOTAL_DUE.getKeyName(), Double.toString(ad));
						
						ap = ap - ad;
						if (ap > 0) {
							weHavePaid = true;
							resultMap.put(TaxHistorySetKey.AMOUNT_PAID.getKeyName(), df.format(ap));
							baseAmountInstallments[0] = ap;
						}
					}
				}
				
				if (!weHavePaid) {
					String minimumDue = HtmlParser3
							.getValueFromAbsoluteCell(0, 2, HtmlParser3.findNode(total_due_list, "TOTAL Due"), "", false)
							.replaceAll("[^\\d.]", "");
					if (org.apache.commons.lang.StringUtils.isNotEmpty(minimumDue) && minimumDue.matches("\\d+\\.\\d+")) {
						baseAmountInstallments[0] = Double.parseDouble(minimumDue);
					}
				}
				
				if (charge-baseAmountInstallments[0]>0) {
					baseAmountInstallments[1] = baseAmountInstallments[2] = baseAmountInstallments[3] = (charge-baseAmountInstallments[0])/3;
				}
				
			}

			// get amountpaid
			NodeList rcpt = tableList.extractAllNodesThatMatch(new HasAttributeFilter("id",	"rcpt"));
			double ap = 0;
			if (rcpt.size() > 0) {

				

				TableTag rcpts = (TableTag) rcpt.elementAt(0);

				TableRow[] rows = rcpts.getRows();

				// create table receipt
				String[] rcpt_header = { "ReceiptDate", "ReceiptNumber", "ReceiptAmount" };
				List<List<String>> rcpt_body = new ArrayList<List<String>>();
				List<String> rcpt_row = new ArrayList<String>();

				Date payDate = getDataSite().getPayDate();
				
				boolean firstRow = true;
				for (TableRow r : rows) {
					TableColumn[] columns = r.getColumns();
					if (r.getColumnCount() == 4 && columns[0].toPlainTextString().replaceAll("&nbsp;", "").matches("\\d+/\\d+/\\d+")) {
						
						String date = columns[0].toPlainTextString().replaceAll("&nbsp;", "").trim();
						
						try {
							if (!weHaveCharge) {
								Date rcptDate = Util.dateParser3(date);

								if (payDate != null && rcptDate != null && rcptDate.after(payDate)) {
									ap += Double.valueOf(columns[3].toPlainTextString().replaceAll("[^\\d.]", ""));
								}

								if (firstRow) {
									// compute ad
									firstRow = false;

									due = Double.valueOf(columns[2].toPlainTextString().replaceAll("[^\\d.]", ""));
									due = due - ap;

									if(due>0){
										resultMap.put(TaxHistorySetKey.TOTAL_DUE.getKeyName(), df.format(due));
									}
								}
							}
						} catch (Exception e) {
							//parse double problems 
							e.printStackTrace();
						}
						
						rcpt_row = new ArrayList<String>();
						// date
						rcpt_row.add(date);
						// no
						rcpt_row.add(columns[1].toPlainTextString()
								.replaceAll("&nbsp;", "")
								.replaceAll("[ -]", "").trim());
						// amount
						String amountPaidString = columns[3].toPlainTextString()
								.replaceAll("&nbsp;", "")
								.replaceAll("[ -,$]", "").trim();
						rcpt_row.add(amountPaidString);
//						String dueChargesString = columns[2].toPlainTextString()
//								.replaceAll("&nbsp;", "")
//								.replaceAll("[ -,$]", "").trim();
//						double dueCharges = 0.0;
//						if (org.apache.commons.lang.StringUtils.isNotEmpty(dueChargesString) && dueChargesString.matches("\\d+\\.\\d+")) {
//							dueCharges = Double.parseDouble(dueChargesString);
//							if (dueCharges!=0 && charge==dueCharges) {
//								double amountPaid = 0.0;
//								if (org.apache.commons.lang.StringUtils.isNotEmpty(amountPaidString) && amountPaidString.matches("\\d+\\.\\d+")) {
//									amountPaid = Double.parseDouble(amountPaidString);
//									paymentsDueCharges.add(0, amountPaid);
//								}
//							}
//						}
						
						try {
							Date rcptDate = Util.dateParser3(date); 
							if(rcptDate != null && rcptDate.after(getDataSite().getPayDate())) {
								double amountPaid = 0.0;
								if (org.apache.commons.lang.StringUtils.isNotEmpty(amountPaidString) && amountPaidString.matches("\\d+\\.\\d+")) {
									amountPaid = Double.parseDouble(amountPaidString);
									paymentsDueCharges.add(0, amountPaid);
								}
							}
							
						} catch (Exception e) {
							logger.error("Error while parsing paid amounts", e);
						}
						
						rcpt_body.add(rcpt_row);
					}
				}

				if (!weHaveCharge && ap>0) {
					resultMap.put(TaxHistorySetKey.AMOUNT_PAID.getKeyName(), Double.toString(ap));
				}

				ResultTable receipts = new ResultTable();
				Map<String, String[]> rcpt_map = new HashMap<String, String[]>();
				rcpt_map.put("ReceiptDate", new String[] { "ReceiptDate", "" });
				rcpt_map.put("ReceiptNumber", new String[] { "ReceiptNumber", "" });
				rcpt_map.put("ReceiptAmount", new String[] { "ReceiptAmount", "" });
				receipts.setHead(rcpt_header);
				receipts.setMap(rcpt_map);
				receipts.setBody(rcpt_body);
				receipts.setReadOnly();
				resultMap.put("TaxHistorySet", receipts);
			}
			
			//calculate installments
			if (total_due_list.size() > 0) {
				
				int len = paymentsDueCharges.size();
				
				double amountToPay = 0;
				for (double installment : baseAmountInstallments) {
					amountToPay +=  installment;
				}
				
				if(amountToPay == 0) {
					
					if (len>1) {
						for (int i=0;i<len;i++) {
							baseAmountInstallments[i] = Double.valueOf(paymentsDueCharges.get(i));
							amountPaidInstallments[i] = baseAmountInstallments[i];
						}
					} else {
						baseAmountInstallments[0] = ap;
						amountPaidInstallments[0] = ap;
						totalDueInstallments[0] = 0.0;
						statustInstallments[0] = "PAID";
					}
					
					resultMap.put(TaxHistorySetKey.AMOUNT_PAID.getKeyName(), Double.toString(ap));
					resultMap.put(TaxHistorySetKey.TOTAL_DUE.getKeyName(), "0.0");
					
				} else {
					
					if (len==3) {
						for (int i=0;i<len;i++) {
							baseAmountInstallments[i] = Double.valueOf(paymentsDueCharges.get(i));
						}
						baseAmountInstallments[3] = due;
					} else if (len==2) {
						for (int i=0;i<len;i++) {
							baseAmountInstallments[i] = Double.valueOf(paymentsDueCharges.get(i));
						}
						baseAmountInstallments[2] = due/2;
						baseAmountInstallments[3] = due/2;
					}
					
					for (int i=0;i<4&&i<len;i++) {
						amountPaidInstallments[i] = Double.valueOf(paymentsDueCharges.get(i));
						totalDueInstallments[i] = 0.0;
						statustInstallments[i] = "PAID";
					}
				
					for (int i=len;i<4;i++) {
						if (baseAmountInstallments[i]!=0) {
							amountPaidInstallments[i] = 0.0;
							totalDueInstallments[i] = baseAmountInstallments[i];
							statustInstallments[i] = "UNPAID";
						}
					}
				}
				
				List<String> line = new ArrayList<String>();
				@SuppressWarnings("rawtypes")
				List<List> bodyInstallments = new ArrayList<List>();
				for (int i=0;i<4;i++) {
					line = new ArrayList<String>();
					line.add("Installment" + (i+1));
					line.add(df.format(baseAmountInstallments[i]));
					line.add(df.format(amountPaidInstallments[i]));
					line.add(df.format(totalDueInstallments[i]));
					line.add(statustInstallments[i]);
					bodyInstallments.add(line);
				}
				String [] header = {"InstallmentName", "BaseAmount", "AmountPaid", "TotalDue", "Status"};				   
				Map<String,String[]> map = new HashMap<String,String[]>();
				map.put("InstallmentName", new String[]{"InstallmentName", ""});
				map.put("BaseAmount", new String[]{"BaseAmount", ""});
				map.put("AmountPaid", new String[]{"AmountPaid", ""});
				map.put("TotalDue", new String[]{"TotalDue", ""});
				map.put("Status", new String[]{"Status", ""});
				
				ResultTable installmentsRT = new ResultTable();	
				installmentsRT.setHead(header);
				installmentsRT.setBody(bodyInstallments);
				installmentsRT.setMap(map);
				resultMap.put("TaxInstallmentSet", installmentsRT);
			}

			// deed table
			ResultTable prop_doc = new ResultTable();
			Map<String, String[]> doc_map = new HashMap<String, String[]>();
			doc_map.put("InstrumentNumber", new String[] { "InstrumentNumber", "" });
			doc_map.put("InstrumentDate", new String[] { "InstrumentDate", "" });
			String[] doc_header = { "InstrumentNumber", "InstrumentDate" };
			List<List<String>> doc_body = new ArrayList<List<String>>();

			NodeList doc_list = tableList.extractAllNodesThatMatch(
					new TagNameFilter("table"), true).extractAllNodesThatMatch(
					new HasAttributeFilter("id", "property_doc"), true);

			if (doc_list.size() > 0) {
				TableTag doc_t = (TableTag) doc_list.elementAt(0);
				TableRow[] doc_r = doc_t.getRows();
				for (TableRow r : doc_r) {
					if (r.getColumnCount() == 2) {
						List<String> doc_row = new ArrayList<String>();
						String docNo = r.getColumns()[0].toPlainTextString().replace("&nbsp;", "").replaceAll("\\s+", " ").trim();
						String date = r.getColumns()[1].toPlainTextString().replace("&nbsp;", "").replaceAll("\\s+", " ").trim();
						if (StringUtils.isEmpty(date)){
							date = docNo.substring(0, docNo.length() - 5);
							if (date.length() == 6){
								String yearFromDate = date.substring(0, 2);
								try {
									int intYear = Integer.parseInt(yearFromDate);
									
									if (intYear <= 20) {
										date = "20" + date;
									} else if (intYear > 20) {
										date = "19" + date;
									}
									date = date.replaceAll("(\\d{4})(\\d{2})(\\d{2})", "$1-$2-$3");
								} catch (Exception e) {
								}
							}
						}
						
						docNo = docNo.substring(docNo.length() - 5, docNo.length());
						docNo = org.apache.commons.lang.StringUtils.stripStart(docNo, "0");
						doc_row.add(docNo);
						doc_row.add(date);
						doc_body.add(doc_row);
					}
				}
			}
			prop_doc.setHead(doc_header);
			prop_doc.setMap(doc_map);
			prop_doc.setBody(doc_body);
			prop_doc.setReadOnly();
			resultMap.put("SaleDataSet", prop_doc);

		} catch (Exception e) {
			logger.error("Error while filling result map", e);
		}
		return null;
	}

	protected void setModulesForAutoSearch(TSServerInfo serverInfo) {
		List<TSServerInfoModule> moduleList = new ArrayList<TSServerInfoModule>();

		TSServerInfoModule module = null;

		module = new TSServerInfoModule(
				serverInfo.getModule(TSServerInfo.GENERIC_MODULE_IDX));
		module.clearSaKeys();

		FilterResponse taxYearFilter = new TaxYearFilterResponse(searchId);
		DuplicateInstrumentFilterResponse duplicateInstrFilter = new DuplicateInstrumentFilterResponse(searchId);
		
		if (hasPin()) {
			String parcelno = getSearchAttribute(SearchAttributes.LD_PARCELNO_GENERIC_TR);

			if (parcelno.length() == 14) {
				module.forceValue(2, parcelno);
				module.addFilter(taxYearFilter);
				module.addFilter(duplicateInstrFilter);
				moduleList.add(module);
			}
		}

		FilterResponse addressFilter = AddressFilterFactory
				.getAddressHybridFilter(searchId, 0.8d, true);

		if (hasStreet()) {
			module = new TSServerInfoModule(
					serverInfo.getModule(TSServerInfo.GENERIC_MODULE_IDX));
			// remove city if necesary
			String address = getSearchAttribute(
					SearchAttributes.P_STREET_NO_NAME).toUpperCase().trim();

			for (String s : ro.cst.tsearch.servers.functions.NVClarkTR.cities) {
				if (address.contains(s.toUpperCase())) {
					address = address.replace(s.toUpperCase(), "");
					break;
				}
			}

			module.forceValue(8, address);
			module.addFilter(taxYearFilter);
			module.addFilter(duplicateInstrFilter);
			moduleList.add(module);
		}

		if (hasOwner()) {
			module = new TSServerInfoModule(
					serverInfo.getModule(TSServerInfo.GENERIC_MODULE_IDX));
			FilterResponse nameFilterHybridDoNotSkipUnique = NameFilterFactory
					.getHybridNameFilter(SearchAttributes.OWNER_OBJECT,
							searchId, module);
			nameFilterHybridDoNotSkipUnique.setSkipUnique(false);

			module.setSaObjKey(SearchAttributes.OWNER_OBJECT);
			module.setIteratorType(0, FunctionStatesIterator.ITERATOR_TYPE_LFM_NAME_FAKE);
			module.addFilter(nameFilterHybridDoNotSkipUnique);
			module.addFilter(addressFilter);
			module.addFilter(taxYearFilter);
			module.addFilter(duplicateInstrFilter);
			module.addIterator((ConfigurableNameIterator) ModuleStatesIteratorFactory
					.getConfigurableNameIterator(module, searchId,
							new String[] { "L;F;", "L;F;M" }));
			moduleList.add(module);
		}
		serverInfo.setModulesForAutoSearch(moduleList);
	}
}
