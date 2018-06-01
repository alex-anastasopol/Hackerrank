package ro.cst.tsearch.servers.types;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Vector;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.htmlparser.Node;
import org.htmlparser.filters.HasAttributeFilter;
import org.htmlparser.filters.TagNameFilter;
import org.htmlparser.tags.LinkTag;
import org.htmlparser.tags.TableColumn;
import org.htmlparser.tags.TableRow;
import org.htmlparser.tags.TableTag;
import org.htmlparser.util.NodeList;

import ro.cst.tsearch.bean.SearchAttributes;
import ro.cst.tsearch.exceptions.ServerResponseException;
import ro.cst.tsearch.extractor.xml.Bridge;
import ro.cst.tsearch.extractor.xml.GenericFunctions2;
import ro.cst.tsearch.extractor.xml.ResultMap;
import ro.cst.tsearch.extractor.xml.ResultTable;
import ro.cst.tsearch.extractor.xml.StringFormats;
import ro.cst.tsearch.parser.HtmlParser3;
import ro.cst.tsearch.search.FunctionStatesIterator;
import ro.cst.tsearch.search.filter.FilterResponse;
import ro.cst.tsearch.search.filter.newfilters.address.AddressFilterFactory;
import ro.cst.tsearch.search.filter.newfilters.name.NameFilterFactory;
import ro.cst.tsearch.search.iterator.ModuleStatesIteratorFactory;
import ro.cst.tsearch.search.module.ConfigurableNameIterator;
import ro.cst.tsearch.servers.HashCountyToIndex;
import ro.cst.tsearch.servers.TSConnectionURL;
import ro.cst.tsearch.servers.bean.DataSite;
import ro.cst.tsearch.servers.info.TSServerInfo;
import ro.cst.tsearch.servers.info.TSServerInfoModule;
import ro.cst.tsearch.servers.response.LinkInPage;
import ro.cst.tsearch.servers.response.ParsedResponse;
import ro.cst.tsearch.servers.response.ServerResponse;

import com.stewart.ats.base.document.DocumentI;
import com.stewart.ats.base.document.TaxDocumentI;

/**
 * 
 * @author george oprina
 * 
 */

public class UTWeberTR extends TSServer {

	/**
	 * @author george oprina
	 */
	private static final long serialVersionUID = 1L;

	public UTWeberTR(long searchId) {
		super(searchId);
	}

	public UTWeberTR(String rsRequestSolverName, String rsSitePath,
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
		case ID_SEARCH_BY_ADDRESS:
		case ID_SEARCH_BY_PARCEL:
		case ID_SEARCH_BY_NAME:
			if (rsResponse.indexOf("No results found for query") > -1) {
				Response.getParsedResponse()
						.setError(
								"No results found for your query! Please change your search criteria and try again.");
				return;
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
		case ID_SEARCH_BY_BOOK_AND_PAGE:
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
					mSearch.addInMemoryDoc(sSave2TSDLink, details);
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

			org.htmlparser.Parser htmlParser = org.htmlparser.Parser
					.createParser(rsResponse, null);

			NodeList nodeList = htmlParser.parse(null);

			if (nodeList.toHtml().contains("No Parcel Data Found")) {
				return "Nothing found for this criteria";
			}

			/* If from memory - use it as is */
			if (!rsResponse.contains("<html")) {
				NodeList headerList = nodeList.extractAllNodesThatMatch(
						new TagNameFilter("table"), true);

				if (headerList.size() == 0) {
					return null;
				}
				return rsResponse;
			}

			NodeList tableAccountSummaryList = nodeList
					.extractAllNodesThatMatch(new TagNameFilter("table"), true)
					.extractAllNodesThatMatch(
							new HasAttributeFilter("width", "100%"), true)
					.extractAllNodesThatMatch(
							new HasAttributeFilter("cellpadding", "0"), true);

			NodeList headerList = tableAccountSummaryList;
			if (headerList.size() == 0) {
				return null;
			}
			String regex="php.id=(\\d+)\">";
			Pattern p = Pattern.compile(regex);
			Matcher m = p.matcher(headerList.toHtml());
			if(m.find()){
				accountId.append(m.group(1));
			}

			TableTag mainTable = (TableTag) tableAccountSummaryList
					.elementAt(1);

			int posLastHeader = -1;
			int posLastRow = -1;
			TableRow[] rows = mainTable.getRows();

			for (int i = 1; i < rows.length && i != 2 && i != 3; i++) {
				if (posLastHeader < 0) {
					if (rows[i].toPlainTextString().trim().equals("Images")) {
						posLastHeader = mainTable.findPositionOf(rows[i]);
					}
				} else {
					posLastRow = mainTable.findPositionOf(rows[i]);
					break;
				}
			}

			if (posLastRow > 0) {
				mainTable.removeChild(posLastRow);
			}
			if (posLastHeader > 0) {
				mainTable.removeChild(posLastHeader);
			}

			mainTable.removeChild(mainTable.findPositionOf(rows[0]));

			NodeList secdivList = new NodeList();
			mainTable.childAt(mainTable.findPositionOf(rows[2])).collectInto(
					secdivList, new HasAttributeFilter("width", "100%"));

			TableTag secTable = (TableTag) secdivList.elementAt(0);
			TableRow[] secrows = secTable.getRows();
			secTable.removeChild(secTable.findPositionOf(secrows[0]));
			secTable.removeChild(secTable.findPositionOf(secrows[1]));

			mainTable.removeChild(mainTable
					.findPositionOf(rows[rows.length - 1]));
			mainTable.removeChild(mainTable
					.findPositionOf(rows[rows.length - 2]));

			details.append(
					"<table align=\"center\" border=\"1\"><tr><td align=\"center\">")
					.append(tableAccountSummaryList.elementAt(1).toHtml())
					.append("</td></tr><tr><td align=\"center\">")
					.append("</td></tr>");

			NodeList linkList = nodeList
					.extractAllNodesThatMatch(new TagNameFilter("table"), true)
					.extractAllNodesThatMatch(
							new HasAttributeFilter("width", "100%"), true)
					.extractAllNodesThatMatch(
							new HasAttributeFilter("cellpadding", "2"), true)
					.extractAllNodesThatMatch(new TagNameFilter("a"), true);

			DataSite site = HashCountyToIndex.getDateSiteForMIServerID(
					getCommunityId(), getServerID());
			String serverHomeLink = site.getServerHomeLink();

			boolean dt = false, oi = false, th = false;

			for (int i = 0; i < linkList.size(); i++) {
				LinkTag link = (LinkTag) linkList.elementAt(i);
				if (link.toHtml().contains("delinquent.php") && !dt) {
					dt = true;
					details.append("<tr><td colspan=\"2\" class=\"department-header\" align=\"center\">Tax Status</td></tr>");
					details.append("<tr><td align=\"center\">")
							.append(getDelinquentTaxes(serverHomeLink
									+ "psearch/" + link.extractLink()))
							.append("</td></tr>");
				}
			}

			for (int i = 0; i < linkList.size(); i++) {
				LinkTag link = (LinkTag) linkList.elementAt(i);
				if (link.toHtml().contains("=\"summary.php") && !oi) {
					oi = true;
					details.append("<tr><td align=\"center\">")
							.append(getOwnershipInfo(serverHomeLink
									+ "psearch/" + link.extractLink()))
							.append("</td></tr>");
				}

				if (link.toHtml().trim().contains("tax.php") && !th) {
					th = true;
					for (int j = 0; j < 4; j++) {
						switch (j) {
						case 0:
							details.append("<tr><td colspan=\"2\" class=\"department-header\" align=\"center\">Property Charges</td></tr>");
							break;
						case 1:
							details.append("<tr><td colspan=\"2\" class=\"department-header\" align=\"center\">Payments</td></tr>");
							break;
						case 2:
							details.append("<tr><td colspan=\"2\" class=\"department-header\" align=\"center\">Property Values</td></tr>");
							break;
						case 3:
							details.append("<tr><td colspan=\"2\" class=\"department-header\" align=\"center\">Taxing Unit Areas</td></tr>");
							break;
						}

						details.append("<tr><td align=\"center\">")
								.append(getTaxHistory(serverHomeLink
										+ "psearch/" + link.extractLink(), j))
								.append("</td></tr>");
					}
				}
			}

			details.append("</table>");
			return details.toString();

		} catch (Exception e) {
			logger.error("Error while getting details", e);
		}
		return null;
	}

	private String getOwnershipInfo(String extractLink) {
		String ownerHtml = getLinkContents(extractLink);

		if (ownerHtml == null)
			return "Error getting ownership info";

		try {

			org.htmlparser.Parser htmlParser = org.htmlparser.Parser
					.createParser(ownerHtml, null);
			NodeList divList = htmlParser.parse(null).extractAllNodesThatMatch(
					new HasAttributeFilter("align", "center"), true);
			NodeList secdivList = divList.extractAllNodesThatMatch(
					new TagNameFilter("div"), true).extractAllNodesThatMatch(
					new HasAttributeFilter("align", "center"), true);
			for (int i = 0; i < secdivList.size(); i++) {
				if (secdivList.elementAt(i).toHtml().contains("Click")) {
					secdivList.elementAt(i).setChildren(null);
				}
			}

			TableTag mainTable = (TableTag) divList.elementAt(6);
			TableRow[] rows = mainTable.getRows();

			TableColumn[] col = rows[rows.length - 5].getColumns();
			Node[] n = new Node[5];

			if (col.length == 2) {
				n[0] = col[0].childAt(0);
				col[0].setAttribute("width", "100%");
				col[0].setAttribute("align", "left");
				col[1].setAttribute("width", "0%");
				TableTag t1 = (TableTag) n[0];
				t1.setAttribute("align", "left");
				TableRow[] r1 = t1.getRows();

				for (int i = 6; i < r1.length; i++) {
					t1.removeChild(t1.findPositionOf(r1[i]));
				}

				for (int j = 0; j < col[1].getChildCount(); j++) {
					col[1].removeChild(j);
				}
				col[1].setChildren(new NodeList());
			}

			for (int i = 0; i < rows.length - 5; i++) {
				mainTable.removeChild(mainTable.findPositionOf(rows[i]));
			}

			ownerHtml = divList.elementAt(6).toHtml()
					.replaceAll("(?ism)<a [^>]*>([^<]*)<[^>]*>", "$1");

		} catch (Exception e) {
			logger.error("Error while getting getOwner " + extractLink, e);
		}

		return ownerHtml.replaceAll("<[^>]*>Click([^<]*)<[^>]*>", "")
				.replaceAll("#0509ab", "#000000");
	}

	private String getDelinquentTaxes(String extractLink) {

		String deliquentHtml = getLinkContents(extractLink);
		try {

			org.htmlparser.Parser htmlParser = org.htmlparser.Parser
					.createParser(deliquentHtml, null);
			NodeList divList = htmlParser.parse(null).extractAllNodesThatMatch(
					new HasAttributeFilter("class", "full-border"), true);

			TableTag mainTable = (TableTag) divList.elementAt(0);
			TableRow[] rows = mainTable.getRows();
			mainTable.removeChild(mainTable.findPositionOf(rows[0]));
			mainTable.removeChild(mainTable.findPositionOf(rows[1]));
			mainTable.removeChild(mainTable.findPositionOf(rows[2]));
			mainTable.removeChild(mainTable.findPositionOf(rows[3]));

			NodeList secdivList = new NodeList();
			mainTable.childAt(mainTable.findPositionOf(rows[4])).collectInto(
					secdivList, new HasAttributeFilter("width", "100%"));

			TableTag secTable = (TableTag) secdivList.elementAt(0);
			TableRow[] secrows = secTable.getRows();
			secTable.removeChild(secTable.findPositionOf(secrows[0]));

			secdivList = new NodeList();
			mainTable.childAt(mainTable.findPositionOf(rows[5])).collectInto(
					secdivList, new TagNameFilter("table"));

			if (secdivList != null) {
				secTable = (TableTag) secdivList.elementAt(2);
				if (secTable != null) {
					secrows = secTable.getRows();
					for (int i = 0; i < secrows.length; i++) {
						secTable.removeChild(secTable
								.findPositionOf(secrows[i]));
					}
				}
			}

			mainTable.removeChild(mainTable
					.findPositionOf(rows[rows.length - 1]));
			mainTable.removeChild(mainTable
					.findPositionOf(rows[rows.length - 2]));
			mainTable.removeChild(mainTable
					.findPositionOf(rows[rows.length - 3]));

			deliquentHtml = divList.elementAt(0).toHtml()
					.replaceAll("(?ism)<a [^>]*>([^<]*)<[^>]*>", "$1")
					.replaceAll("<[^>]*>Click([^<]*)<[^>]*>", "");

		} catch (Exception e) {
			logger.error("Error while getting getDeliquent " + extractLink, e);
		}
		return deliquentHtml;
	}

	protected String getTaxHistory(String extractLink, int i) {

		int rand = (int) (Math.random() * 99999999);

		String taxHistoryHtml = getLinkContents(extractLink);
		try {
			org.htmlparser.Parser htmlParser;
			NodeList divList = new NodeList();
			htmlParser = org.htmlparser.Parser.createParser(taxHistoryHtml,
					null);
			divList = htmlParser.parse(null).extractAllNodesThatMatch(
					new HasAttributeFilter("class", "full-borderlightgray"),
					true);
			TableTag mainTable = (TableTag) divList.elementAt(i);

			if (i == 2) {
				mainTable.setAttribute("width", "100%", '"');
			}

			taxHistoryHtml = divList.elementAt(i).toHtml()
					.replaceAll("(?ism)<a [^>]*>([^<]*)<[^>]*>", "$1");

			String aux = extractLink.replaceAll("tax.php", "");
			String sectaxHistoryHtml = "";
			org.htmlparser.Parser sechtmlParser;
			NodeList secdivList = new NodeList();

			switch (i) {
			case 0:
				extractLink = aux + "charges_history.php?show=2&rand=" + rand;
				sectaxHistoryHtml = getLinkContents(extractLink);
				sechtmlParser = org.htmlparser.Parser.createParser(
						sectaxHistoryHtml, null);
				secdivList = sechtmlParser.parse(null)
						.extractAllNodesThatMatch(
								new HasAttributeFilter("table"), true);

				taxHistoryHtml += (secdivList.elementAt(0).toHtml().replaceAll(
						"(?ism)<a [^>]*>([^<]*)<[^>]*>", "$1"));
				break;
			case 1:
				extractLink = aux + "payments.php?show=3&rand=" + rand;
				sectaxHistoryHtml = getLinkContents(extractLink);
				sechtmlParser = org.htmlparser.Parser.createParser(
						sectaxHistoryHtml, null);
				secdivList = sechtmlParser.parse(null)
						.extractAllNodesThatMatch(
								new HasAttributeFilter("table"), true);

				taxHistoryHtml += (secdivList.elementAt(0).toHtml().replaceAll(
						"(?ism)<a [^>]*>([^<]*)<[^>]*>", "$1"));
				taxHistoryHtml = taxHistoryHtml.replaceAll("\\s+", " ");
				taxHistoryHtml = taxHistoryHtml.replace("$-", "$").replace(
						"$ -", "$");
				taxHistoryHtml = ro.cst.tsearch.servers.functions.UTWeberTR
						.parseDate(taxHistoryHtml);
				break;
			case 2:
				extractLink = aux + "history.php?show=1&rand=" + rand;
				sectaxHistoryHtml = getLinkContents(extractLink);
				sechtmlParser = org.htmlparser.Parser.createParser(
						sectaxHistoryHtml, null);
				secdivList = sechtmlParser.parse(null)
						.extractAllNodesThatMatch(
								new HasAttributeFilter("table"), true);

				taxHistoryHtml += (secdivList.elementAt(0).toHtml().replaceAll(
						"(?ism)<a [^>]*>([^<]*)<[^>]*>", "$1"));
				break;
			case 3:
				extractLink = aux + "tax_unit.php?show=4&rand=" + rand;
				sectaxHistoryHtml = getLinkContents(extractLink);
				sechtmlParser = org.htmlparser.Parser.createParser(
						sectaxHistoryHtml, null);
				secdivList = sechtmlParser.parse(null)
						.extractAllNodesThatMatch(
								new HasAttributeFilter("table"), true);

				taxHistoryHtml += (secdivList.elementAt(0).toHtml().replaceAll(
						"(?ism)<a [^>]*>([^<]*)<[^>]*>", "$1"));
				break;

			}
		} catch (Exception e) {
			logger.error("Error while getting getTaxHistory " + extractLink, e);
		}
		return taxHistoryHtml.replaceAll("<[^>]*>Click([^<]*)<[^>]*>", "");
	}

	@Override
	public Collection<ParsedResponse> smartParseIntermediary(
			ServerResponse response, String table, StringBuilder outputTable) {
		Vector<ParsedResponse> intermediaryResponse = new Vector<ParsedResponse>();

		try {
			org.htmlparser.Parser htmlParser = org.htmlparser.Parser
					.createParser(table, null);
			NodeList nodeList = htmlParser.parse(null);
			NodeList mainTableList = nodeList.extractAllNodesThatMatch(
					new HasAttributeFilter("class", "full-borderlightgray"),
					true);

			if (mainTableList.size() != 1) {
				return intermediaryResponse;
			}
			TableTag tableTag = (TableTag) mainTableList.elementAt(0);
			TableRow[] rows = tableTag.getRows();
			for (int i = 2; i < rows.length; i++) {
				TableRow row = rows[i];
				if (row.getColumnCount() == 3) {

					LinkTag linkTag = ((LinkTag) row.getColumns()[0]
							.getChildren()
							.extractAllNodesThatMatch(new TagNameFilter("a"))
							.elementAt(0));

					String link = CreatePartialLink(TSConnectionURL.idGET)
							+ "/psearch/"
							+ linkTag.extractLink().trim()
									.replaceAll("\\s", "%20");

					linkTag.setLink(link);

					ParsedResponse currentResponse = new ParsedResponse();
					currentResponse.setAttribute(
							ParsedResponse.SERVER_ROW_RESPONSE, row.toHtml());
					currentResponse.setOnlyResponse(row.toHtml());
					currentResponse.setPageLink(new LinkInPage(link, link,
							TSServer.REQUEST_SAVE_TO_TSD));

					ResultMap m = ro.cst.tsearch.servers.functions.UTWeberTR
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
									+ "<tr><th>Parcel#</th><th>Owner</th><th>Address</th></tr>");
			response.getParsedResponse().setFooter("</table>");

			outputTable.append(table);

		} catch (Throwable t) {
			logger.error("Error while parsing intermediary data", t);
		}

		return intermediaryResponse;
	}

	protected static final Pattern TRANFERS_BOOK_PAGE_PATTERN = Pattern
			.compile("\\s*B:\\s*(\\d+)\\s*P:\\s*(\\d+)\\s*");

	@Override
	protected Object parseAndFillResultMap(ServerResponse response,
			String detailsHtml, ResultMap resultMap) {
		try {
			org.htmlparser.Parser htmlParser = org.htmlparser.Parser
					.createParser(detailsHtml, null);

			NodeList nodeList = htmlParser.parse(null);

			NodeList headerList = nodeList
					.extractAllNodesThatMatch(new TagNameFilter("table"), true)
					.extractAllNodesThatMatch(
							new HasAttributeFilter("border", "1"), true)
					.extractAllNodesThatMatch(
							new HasAttributeFilter("align", "center"), true);
			if (headerList.size() == 0) {
				return null;
			}

			// get parcel ID
			String parcelID = HtmlParser3.getValueFromAbsoluteCell(0, 0,
					HtmlParser3.findNode(headerList, "Parcel Nbr:"), "", false);
			resultMap.put("PropertyIdentificationSet.ParcelID", parcelID
					.replaceAll("Parcel Nbr: ", "").replaceAll("-", ""));
			resultMap.put("OtherInformationSet.SrcType", "TR");
			resultMap.put("PropertyIdentificationSet.ParcelIDParcel", parcelID
					.replaceAll("Parcel Nbr: ", "").replaceAll("-", ""));

			// get owner
			TableTag mainTable = (TableTag) headerList
					.extractAllNodesThatMatch(new TagNameFilter("table"), true)
					.elementAt(4);
			TableRow[] rows = mainTable.getRows();

			String owner = rows[1].toHtml().replaceAll("<br>", "")
					.replaceAll("(?ism)<[^>]*>([^<]*)<[^>]*>", "$1").trim();
			resultMap.put("PropertyIdentificationSet.NameOnServer", owner);

			// get address

			NodeList nl = headerList
					.extractAllNodesThatMatch(new TagNameFilter("table"), true)
					.extractAllNodesThatMatch(
							new HasAttributeFilter("align", "left"), true)
					.extractAllNodesThatMatch(
							new HasAttributeFilter("width", "62%"), true);

			TableTag col = (TableTag) nl.elementAt(0);
			if (col != null) {
				String add = "";
				rows = col.getRows();
				if (rows[2].getChildCount() > 2) {
					String address = rows[2].childAt(3).toHtml();
					if (address.contains("<br>")) {
						add = address.substring(0, address.indexOf("<br>"))
								.replace("<td>", "").trim();
					}
				}
				resultMap.put("PropertyIdentificationSet.StreetNo",
						StringFormats.StreetNo(add));
				resultMap.put("PropertyIdentificationSet.StreetName",
						StringFormats.StreetName(add));
			}
			// get description
			mainTable = (TableTag) headerList
					.extractAllNodesThatMatch(new TagNameFilter("table"), true)
					.extractAllNodesThatMatch(
							new HasAttributeFilter("class", "small_print"),
							true).elementAt(0);
			rows = mainTable.getRows();

			String description = rows[0].toHtml().trim().replaceAll("<br>", "")
					.replaceAll("(?ism)<[^>]*>([^<]*)<[^>]*>", "$1").trim();
			resultMap.put("PropertyIdentificationSet.PropertyDescription",
					description);

			// get totalassesment
			mainTable = (TableTag) headerList.extractAllNodesThatMatch(
					new TagNameFilter("table"), true).elementAt(6);
			rows = mainTable.getRows();
			NodeList total_a = HtmlParser3.getNodeListByType(
					rows[1].getChildren(), "td", true);
			rows[1].toHtml();
			String totalAssessment = total_a.elementAt(0).toHtml()
					.replaceAll("(?ism)<[^>]*>([^<]*)<[^>]*>", "$1").trim();
			resultMap.put("PropertyAppraisalSet.TotalAssessment",
					totalAssessment);

			// get table
			@SuppressWarnings("rawtypes")
			List<List> body = new ArrayList<List>();
			List<String> line = null;

			// for debug
			NodeList debugT = headerList
					.extractAllNodesThatMatch(new TagNameFilter("table"), true)
					.extractAllNodesThatMatch(
							new HasAttributeFilter("width", "100%"), true)
					.extractAllNodesThatMatch(
							new HasAttributeFilter("border", "0"), true)
					.extractAllNodesThatMatch(
							new HasAttributeFilter("cellpadding", "0"), true)
					.extractAllNodesThatMatch(
							new HasAttributeFilter("cellspacing", "0"), true);

			for (int i = 1; i < debugT.size(); i++) {
				if (debugT.elementAt(i).toHtml().contains("Entry # ")) {
					mainTable = (TableTag) debugT.elementAt(i);
				}
			}
			rows = mainTable.getRows();

			TableRow[] rows_aux = new TableRow[rows.length];
			TableRow[] rows_aux1 = new TableRow[rows.length];

			int k = 0;

			for (int i = 1; i < rows.length; i++) {
				if (rows[i].toHtml().contains("tr align=\"center\"")) {
					rows_aux[k] = rows[i];
					k++;
				}
				if (rows[i].toHtml().contains("tr class=\"menu-sub\"")) {
					break;
				}
			}
			k = 0;

			for (int i = 1; i < rows.length - 1; i++) {
				if (rows[i].toHtml().contains("tr class=\"menu-sub\"")) {
					rows_aux1[k] = rows[i + 1];
					k++;
				}
				if (rows[i].toHtml().contains("&nbsp;")) {
					break;
				}
			}

			NodeList td = null, td1 = null;

			k = 0;
			for (int i = 0; i < rows_aux.length; i++) {
				if (rows_aux[i] != null)
					k++;
				else
					break;
			}

			for (int i = 0; i < k; i++) {
				if (rows_aux[i] != null) {
					td = HtmlParser3.getNodeListByType(
							rows_aux[i].getChildren(), "td", true);
					td1 = HtmlParser3.getNodeListByType(
							rows_aux1[i].getChildren(), "td", true);
				}
				line = new ArrayList<String>();
				line.add("");
				for (int j = 0; j < td.size(); j++) {
					String l = td.elementAt(j).toHtml()
							.replaceAll("(?ism)<[^>]*>([^<]*)<[^>]*>", "$1")
							.trim();
					if (j == 2 && !l.equals("")) {
						int x = Integer.parseInt(l);
						line.add(x + "");
					} else
						line.add(l);
				}
				String elem = line.get(4);
				line.remove(4);
				line.add(1, elem);
				for (int j = 0; j < td1.size(); j++) {
					line.add(td1.elementAt(j).toHtml()
							.replaceAll("(?ism)<[^>]*>([^<]*)<[^>]*>", "$1")
							.trim());
				}
				body.add(line);
			}

			ro.cst.tsearch.servers.functions.UTWeberTR.parseLegalSummary(
					resultMap, body, searchId);
			ro.cst.tsearch.servers.functions.UTWeberTR.parseNames(resultMap,
					searchId);
			ro.cst.tsearch.servers.functions.UTWeberTR.parseTaxes(nodeList,
					resultMap, searchId);

			// adding all cross references - should contain transfer table and
			// info parsed from legal description
			if (body != null && body.size() > 0) {
				ResultTable rtt = new ResultTable();
				String[] header = { "SalesPrice", "InstrumentDate",
						"InstrumentNumber", "Book", "Page", "DocumentType" };
				rtt = GenericFunctions2.createResultTable(body, header);
				resultMap.put("SaleDataSet", rtt);
			}

		} catch (Exception e) {
			logger.error("Error while getting details", e);
		}
		return null;
	}

	@Override
	protected void setModulesForAutoSearch(TSServerInfo serverInfo) {
		List<TSServerInfoModule> moduleList = new ArrayList<TSServerInfoModule>();

		TSServerInfoModule module = null;

		if (hasPin()) {
			String parcelno = getSearchAttribute(SearchAttributes.LD_PARCELNO)
					.replaceAll("-", "");

			if (parcelno.length() == 9) {
				module = new TSServerInfoModule(
						serverInfo
								.getModule(TSServerInfo.BOOK_AND_PAGE_MODULE_IDX));
				module.clearSaKeys();

				String book = parcelno.substring(0, 2);
				String page = parcelno.substring(2, 5);
				String parcel = parcelno.substring(5);

				module.forceValue(0, book);
				module.forceValue(1, page);
				module.forceValue(2, parcel);

				moduleList.add(module);
			}
		}

		if (hasPinParcelNo()) {
			module = new TSServerInfoModule(
					serverInfo.getModule(TSServerInfo.PARCEL_ID_MODULE_IDX));
			module.clearSaKeys();
			module.forceValue(0,
					getSearchAttribute(SearchAttributes.LD_PARCELNO_PARCEL)
							.replaceAll("-", ""));
			moduleList.add(module);
		}

		boolean hasOwner = hasOwner();
		FilterResponse addressFilter = AddressFilterFactory
				.getAddressHybridFilter(searchId, 0.8d, true);
		if (hasStreet()) {
			FilterResponse nameFilterHybrid = NameFilterFactory
					.getHybridNameFilter(SearchAttributes.OWNER_OBJECT,
							searchId, module);

			if (hasStreetNo()) {
				module = new TSServerInfoModule(
						serverInfo.getModule(TSServerInfo.ADDRESS_MODULE_IDX));
				module.clearSaKeys();
				if (!getSearchAttribute(SearchAttributes.P_STREETDIRECTION)
						.equals(""))
					module.forceValue(
							0,
							getSearchAttribute(SearchAttributes.P_STREETNO)
									+ " "
									+ getSearchAttribute(SearchAttributes.P_STREETDIRECTION)
									+ " "
									+ getSearchAttribute(SearchAttributes.P_STREETNAME));
				else
					module.forceValue(
							0,
							getSearchAttribute(SearchAttributes.P_STREETNO)
									+ " "
									+ getSearchAttribute(SearchAttributes.P_STREETNAME));
				module.addFilter(addressFilter);
				module.addFilter(nameFilterHybrid);
				moduleList.add(module);
			}
		}

		if (hasOwner) {
			FilterResponse nameFilterHybridDoNotSkipUnique = NameFilterFactory
					.getHybridNameFilter(SearchAttributes.OWNER_OBJECT,
							searchId, module);
			nameFilterHybridDoNotSkipUnique.setSkipUnique(false);

			module = new TSServerInfoModule(
					serverInfo.getModule(TSServerInfo.NAME_MODULE_IDX));
			module.clearSaKeys();
			module.setSaObjKey(SearchAttributes.OWNER_OBJECT);
			module.setIteratorType(0,
					FunctionStatesIterator.ITERATOR_TYPE_LCF_NAME_FAKE);
			module.addFilter(nameFilterHybridDoNotSkipUnique);
			module.addFilter(addressFilter);
			module.addIterator((ConfigurableNameIterator) ModuleStatesIteratorFactory
					.getConfigurableNameIterator(module, searchId,
							new String[] { "L;F;", "L;F;M" }));
			moduleList.add(module);
		}

		serverInfo.setModulesForAutoSearch(moduleList);
	}
}
