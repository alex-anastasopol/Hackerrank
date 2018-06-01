package ro.cst.tsearch.servers.types;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Vector;

import org.apache.commons.lang.StringUtils;
import org.htmlparser.Text;
import org.htmlparser.filters.HasAttributeFilter;
import org.htmlparser.filters.TagNameFilter;
import org.htmlparser.tags.LinkTag;
import org.htmlparser.tags.TableColumn;
import org.htmlparser.tags.TableRow;
import org.htmlparser.tags.TableTag;
import org.htmlparser.util.NodeList;

import ro.cst.tsearch.Search;
import ro.cst.tsearch.bean.SearchAttributes;
import ro.cst.tsearch.exceptions.ServerResponseException;
import ro.cst.tsearch.extractor.xml.Bridge;
import ro.cst.tsearch.extractor.xml.ResultMap;
import ro.cst.tsearch.parser.HtmlParser3;
import ro.cst.tsearch.search.filter.FilterResponse;
import ro.cst.tsearch.search.filter.newfilters.address.AddressFilterFactory;
import ro.cst.tsearch.servers.TSConnectionURL;
import ro.cst.tsearch.servers.info.TSServerInfo;
import ro.cst.tsearch.servers.info.TSServerInfoModule;
import ro.cst.tsearch.servers.response.LinkInPage;
import ro.cst.tsearch.servers.response.ParsedResponse;
import ro.cst.tsearch.servers.response.ServerResponse;
import ro.cst.tsearch.utils.Tidy;

import com.stewart.ats.base.document.DocumentI;
import com.stewart.ats.base.document.TaxDocumentI;

/**
 * 
 * @author Oprina George
 * 
 *         Mar 26, 2012
 */

public class CASantaClaraTR extends TSServer {

	/**
	 * 
	 */
	private static final long	serialVersionUID	= 1L;

	public CASantaClaraTR(String rsRequestSolverName, String rsSitePath, String rsServerID, String rsPrmNameLink, long searchId, int miServerID) {
		super(rsRequestSolverName, rsSitePath, rsServerID, rsPrmNameLink, searchId, miServerID);
	}

	@Override
	protected void ParseResponse(String sAction, ServerResponse Response, int viParseID) throws ServerResponseException {
		String rsResponse = Response.getResult();
		ParsedResponse parsedResponse = Response.getParsedResponse();

		mSearch.setAdditionalInfo("viParseID", viParseID);

		if (rsResponse.contains("Session has timed out")) {
			Response.getParsedResponse().setError("Session has timed out!");
			Response.getParsedResponse().setResponse("");
			return;
		}

		if (rsResponse.contains("No data found for") || rsResponse.contains("No current tax bill data found")) {
			Response.getParsedResponse().setError("No data found!");
			Response.getParsedResponse().setResponse("");
			return;
		}

		if (rsResponse.contains("View/Pay Secured Taxes")) {
			Response.getParsedResponse().setError("Error! Modify the search criteria and try again!");
			Response.getParsedResponse().setResponse("");
			return;
		}

		if (rsResponse.contains("error")) {
			Response.getParsedResponse().setError("Error! Try again!");
			Response.getParsedResponse().setResponse("");
			return;
		}

		if (viParseID != ID_SAVE_TO_TSD) {
			if (rsResponse.contains("Results of Address Information Search")) {
				viParseID = ID_INTERMEDIARY;
			} else {
				viParseID = ID_DETAILS;
			}
		}

		switch (viParseID) {
		case ID_SEARCH_BY_ADDRESS:
		case ID_INTERMEDIARY:

			StringBuilder outputTable = new StringBuilder();
			Collection<ParsedResponse> smartParsedResponses = smartParseIntermediary(Response, rsResponse, outputTable);

			if (smartParsedResponses.size() == 0) {
				return;
			}

			parsedResponse.setResultRows(new Vector<ParsedResponse>(smartParsedResponses));
			parsedResponse.setOnlyResponse(outputTable.toString());

			break;

		case ID_DETAILS:
		case ID_SEARCH_BY_PARCEL:
		case ID_SAVE_TO_TSD:

			StringBuilder accountId = new StringBuilder();
			HashMap<String, String> data = new HashMap<String, String>();
			String details = getDetails(Response, rsResponse, accountId, data);
			String accountName = accountId.toString();

			if (viParseID != ID_SAVE_TO_TSD) {
				String originalLink = sAction.replace("?", "&") + "&" + Response.getRawQuerry();
				String sSave2TSDLink = getLinkPrefix(TSConnectionURL.idPOST) + originalLink;

				if (isInstrumentSaved(accountName, null, data)) {
					details += CreateFileAlreadyInTSD();
				} else {
					mSearch.addInMemoryDoc(sSave2TSDLink, details);
					details = addSaveToTsdButton(details, sSave2TSDLink, ID_DETAILS);
				}
				Response.getParsedResponse().setPageLink(new LinkInPage(sSave2TSDLink, originalLink, TSServer.REQUEST_SAVE_TO_TSD));
				Response.getParsedResponse().setResponse(details);

			} else {
				smartParseDetails(Response, details);

				msSaveToTSDFileName = accountName + ".html";
				Response.getParsedResponse().setFileName(getServerTypeDirectory() + msSaveToTSDFileName);
				Response.getParsedResponse().setResponse(details);

				msSaveToTSDResponce = details + CreateFileAlreadyInTSD();
			}

			break;
		case ID_GET_LINK:
			ParseResponse(sAction, Response, rsResponse.contains("Assessor's Parcel Number (APN)") ? ID_DETAILS : ID_INTERMEDIARY);
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

	protected String getDetails(ServerResponse response, String rsResponse, StringBuilder accountId, HashMap<String, String> data) {
		try {
			loadDataHash(data);
			StringBuilder details = new StringBuilder();

			NodeList nodes = new HtmlParser3(Tidy.tidyParse(rsResponse, null)).getNodeList();
			NodeList tables = nodes.extractAllNodesThatMatch(new TagNameFilter("table"), true)
					.extractAllNodesThatMatch(new HasAttributeFilter("cellspacing", "0"), true)
					.extractAllNodesThatMatch(new HasAttributeFilter("cellpadding", "7"), true)
					.extractAllNodesThatMatch(new HasAttributeFilter("border", "0"), true);

			if (tables.size() > 0) {
				String id = HtmlParser3.getValueFromAbsoluteCell(0, 1, HtmlParser3.findNode(tables, "Assessor's Parcel Number (APN)"), "", true).trim();
				accountId.append(id.replaceAll("[^\\d]", ""));
			} else
				return "";

			/* If from memory - use it as is */
			if (!org.apache.commons.lang.StringUtils.containsIgnoreCase(rsResponse, "<html")
					&& !org.apache.commons.lang.StringUtils.containsIgnoreCase(rsResponse, "</html")) {
				return rsResponse;
			}

			// arange the summary html
			TableTag summaryTable = (TableTag) tables.elementAt(0);
			TableRow[] summaryRows = summaryTable.getRows();
			NodeList newSummmaryRows = new NodeList();

			for (TableRow r : summaryRows) {
				newSummmaryRows.add(r);
			}

			TableRow linksRow = null;

			if (newSummmaryRows.size() > 0) {
				linksRow = (TableRow) newSummmaryRows.remove(newSummmaryRows.size() - 1);
				if (newSummmaryRows.elementAt(newSummmaryRows.size() - 1).toPlainTextString().contains("NOTICE: If you are checking to see")) {
					newSummmaryRows.remove(newSummmaryRows.size() - 1);
				}
				summaryTable.setChildren(newSummmaryRows);
			}
			summaryTable.setAttribute("id", "SummaryTable");
			summaryTable.setAttribute("width", "95%");
			summaryTable.setAttribute("align", "center");
			summaryTable.removeAttribute("nowrap");

			if (linksRow != null) {

			}

			details.append(summaryTable.toHtml() + "\n");

			Text txt = HtmlParser3.findNode(tables, "00", true);
			if (txt != null && txt.getParent() instanceof LinkTag) {
				details.append(getHistory(ro.cst.tsearch.connection.http2.CASantaClaraTR.SERVER_LINK + "/payment/jsp/"
						+ ((LinkTag) txt.getParent()).getLink(), "details"));
			}
			
			if(summaryTable.toHtml().contains("Delinquent")){
				txt = HtmlParser3.findNode(tables, "View Detail", true);
				if (txt != null && txt.getParent() instanceof LinkTag) {
					details.append(getHistory(ro.cst.tsearch.connection.http2.CASantaClaraTR.SERVER_LINK + "/payment/jsp/"
							+ ((LinkTag) txt.getParent()).getLink(), "delinquent"));
				}
			}
			
			String historyLink = "http://payments.scctax.org/payment/jsp/securedHistory.jsp?apn=" + accountId
					+ "&list=false&sort=false&number=&street=&city=-NO+SELECTION-";
			details.append(getHistory(historyLink, "history"));

			String yearlyTotals = "http://payments.scctax.org/payment/jsp/securedHistory.jsp?apn=" + accountId
					+ "&list=false&sort=true&number=&street=&city=-NO+SELECTION-";
			details.append(getHistory(yearlyTotals, "yearlyTotals"));

			return details.toString().replaceAll("(?ism)</?a [^>]*>", "");
		} catch (Exception e) {
			e.printStackTrace();
		}
		return "";
	}

	private Object getHistory(String link, String name) {
		try {
			String result = getLinkContents(link);

			if (result.contains("On-line Bill Presentment and Payment Site"))
				return "";

			NodeList nodes = new HtmlParser3(Tidy.tidyParse(result, null)).getNodeList();

			nodes = nodes.extractAllNodesThatMatch(new TagNameFilter("table"), true)
					.extractAllNodesThatMatch(new HasAttributeFilter("cellspacing", "0"), true)
					.extractAllNodesThatMatch(new HasAttributeFilter("cellpadding", "7"), true);

			if (nodes.size() > 0) {
				TableTag detailedTable = (TableTag) nodes.elementAt(0);
				TableRow[] summaryRows = detailedTable.getRows();
				NodeList newSummmaryRows = new NodeList();

				for (TableRow r : summaryRows) {
					newSummmaryRows.add(r);
				}

				if (newSummmaryRows.size() > 2) {
					newSummmaryRows.remove(0);
					newSummmaryRows.remove(newSummmaryRows.size() - 1);

					detailedTable.setChildren(newSummmaryRows);

					detailedTable.setAttribute("id", name);
					detailedTable.setAttribute("width", "95%");
					detailedTable.setAttribute("align", "center");
					detailedTable.removeAttribute("nowrap");

					return detailedTable.toHtml();
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		return "";
	}

	private String makeHeader(int viParseID) {
		String header = "<table id=\"intermediaryResults\" align=\"center\" cellspacing=\"0\" cellpadding=\"3\" border=\"1\" width=\"95%\">";

		header += "<tr>" +
				"<th bgcolor=\"#FFFFFF\">Number</th>" +
				"<th bgcolor=\"#FFFFFF\">APN</th>" +
				"<th bgcolor=\"#FFFFFF\">Property Address</th>" +
				"</tr>";

		return header;
	}

	private String getPrevNext(ServerResponse resp, NodeList nodes) {
		return "";
	}

	public Collection<ParsedResponse> smartParseIntermediary(ServerResponse response, String table, StringBuilder outputTable) {
		Vector<ParsedResponse> intermediaryResponse = new Vector<ParsedResponse>();
		try {
			ParsedResponse parsedResponse = response.getParsedResponse();
			Integer viParseId = (Integer) mSearch.getAdditionalInfo("viParseID");
			String rsResponse = response.getResult();

			NodeList nodes = new HtmlParser3(rsResponse.replaceAll("(?ism)</?b>", "")).getNodeList();

			NodeList tableList = nodes.extractAllNodesThatMatch(new TagNameFilter("table"), true)
					.extractAllNodesThatMatch(new HasAttributeFilter("width", "560"), true)
					.extractAllNodesThatMatch(new HasAttributeFilter("bgcolor", "#0000ff"), true)
					.extractAllNodesThatMatch(new HasAttributeFilter("cellpadding", "5"), true);

			if (tableList.size() > 0) {

				String serverLink = ro.cst.tsearch.connection.http2.CASantaClaraTR.SERVER_LINK;

				TableTag intermediary = (TableTag) tableList.elementAt(0);
				TableRow[] rows = intermediary.getRows();
				for (int i = 1; i < rows.length; i++) {
					TableRow row = rows[i];
					if (row.getColumnCount() == 3) {
						String link = "";
						TableColumn c = row.getColumns()[1];
						if (c.getChildCount() > 0 && c.childAt(0) instanceof LinkTag) {
							LinkTag linkTag = (LinkTag) c.childAt(0);
							link = CreatePartialLink(TSConnectionURL.idGET) + serverLink + "/payment/jsp/" + linkTag.getLink();
							linkTag.setLink(link);
						}

						ParsedResponse currentResponse = new ParsedResponse();
						currentResponse.setAttribute(ParsedResponse.SERVER_ROW_RESPONSE, row.toHtml().replaceAll("(?ism)</?font[^>]*>", ""));
						currentResponse.setOnlyResponse(row.toHtml());
						currentResponse.setPageLink(new LinkInPage(link, link, TSServer.REQUEST_SAVE_TO_TSD));

						ResultMap m = ro.cst.tsearch.servers.functions.CASantaClaraTR.parseIntermediaryRow(row, viParseId);
						Bridge bridge = new Bridge(currentResponse, m, searchId);

						DocumentI document = (TaxDocumentI) bridge.importData();
						currentResponse.setDocument(document);

						intermediaryResponse.add(currentResponse);

					}
				}
			}

			if (getSearch().getSearchType() == Search.PARENT_SITE_SEARCH) {
				parsedResponse.setHeader(makeHeader(viParseId));
				parsedResponse.setFooter("\n</table><br>" + getPrevNext(response, nodes));
			} else {
				parsedResponse.setHeader("<table border=\"1\">");
				parsedResponse.setFooter("</table>");
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		return intermediaryResponse;
	}

	protected Object parseAndFillResultMap(ServerResponse response, String detailsHtml, ResultMap resultMap) {
		ro.cst.tsearch.servers.functions.CASantaClaraTR.parseAndFillResultMap(response, detailsHtml, resultMap);
		return null;
	}

	protected void setModulesForAutoSearch(TSServerInfo serverInfo) {

		List<TSServerInfoModule> moduleList = new ArrayList<TSServerInfoModule>();

		TSServerInfoModule module = null;

		// pin
		if (hasPin()) {
			module = new TSServerInfoModule(serverInfo.getModule(TSServerInfo.PARCEL_ID_MODULE_IDX));
			module.clearSaKeys();

			module.forceValue(1, getSearchAttribute(SearchAttributes.LD_PARCELNO_GENERIC_TR));

			moduleList.add(module);
		}

		// address
		String city = getSearchAttribute(SearchAttributes.P_CITY);

		if (hasStreet() && hasStreetNo() && StringUtils.isNotEmpty(city)) {
			FilterResponse addressFilter = AddressFilterFactory.getAddressHybridFilter(searchId, 0.8d, true);

			module = new TSServerInfoModule(serverInfo.getModule(TSServerInfo.ADDRESS_MODULE_IDX));
			module.clearSaKeys();

			module.forceValue(3, getSearchAttribute(SearchAttributes.P_STREETNO));
			module.forceValue(4, getSearchAttribute(SearchAttributes.P_STREETNAME));
			module.forceValue(6, city);
			module.addFilter(addressFilter);

			moduleList.add(module);
		}

		serverInfo.setModulesForAutoSearch(moduleList);

	}
}
