package ro.cst.tsearch.servers.types;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Vector;

import org.htmlparser.Node;
import org.htmlparser.filters.HasAttributeFilter;
import org.htmlparser.filters.TagNameFilter;
import org.htmlparser.tags.LinkTag;
import org.htmlparser.tags.TableColumn;
import org.htmlparser.tags.TableRow;
import org.htmlparser.tags.TableTag;
import org.htmlparser.util.NodeList;
import org.htmlparser.util.ParserException;

import ro.cst.tsearch.bean.SearchAttributes;
import ro.cst.tsearch.exceptions.ServerResponseException;
import ro.cst.tsearch.extractor.xml.Bridge;
import ro.cst.tsearch.extractor.xml.GenericFunctions;
import ro.cst.tsearch.extractor.xml.ResultMap;
import ro.cst.tsearch.extractor.xml.StringFormats;
import ro.cst.tsearch.search.FunctionStatesIterator;
import ro.cst.tsearch.search.filter.FilterResponse;
import ro.cst.tsearch.search.filter.newfilters.address.AddressFilterFactory;
import ro.cst.tsearch.search.filter.newfilters.name.NameFilterFactory;
import ro.cst.tsearch.search.iterator.ModuleStatesIteratorFactory;
import ro.cst.tsearch.search.module.ConfigurableNameIterator;
import ro.cst.tsearch.search.name.NameUtils;
import ro.cst.tsearch.servers.TSConnectionURL;
import ro.cst.tsearch.servers.info.TSServerInfo;
import ro.cst.tsearch.servers.info.TSServerInfoModule;
import ro.cst.tsearch.servers.response.LinkInPage;
import ro.cst.tsearch.servers.response.ParsedResponse;
import ro.cst.tsearch.servers.response.ServerResponse;

import com.stewart.ats.base.document.AssessorDocumentI;
import com.stewart.ats.base.document.DocumentI;

/**
 * @author costi
 */
public class NVWashoeAO extends TSServer {
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	private String baseLink = "http://www.washoecounty.us",
			transferHistoryLink, additionalOwnersLink;

	public NVWashoeAO(long searchId) {
		super(searchId);
	}

	public NVWashoeAO(String rsRequestSolverName, String rsSitePath,
			String rsServerID, String rsPrmNameLink, long searchId,
			int miServerID) {
		super(rsRequestSolverName, rsSitePath, rsServerID, rsPrmNameLink,
				searchId, miServerID);
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
					new HasAttributeFilter("class", "quickinfo_subgrp"), true);
			if (mainTableList.size() != 1) {
				return intermediaryResponse;
			}

			TableRow[] rows = ((TableTag) mainTableList.elementAt(0)).getRows();
			// Parse every search result row.
			for (TableRow row : rows) {
				if (row.getColumnCount() != 6)
					// Not a usable row.
					continue;
				TableColumn[] columns = row.getColumns();
				if (columns[0].getAttribute("class").equals("header"))
					// The header row.
					continue;

				// Change the external links to internal ones.
				NodeList links = columns[0].getChildren()
						.extractAllNodesThatMatch(new TagNameFilter("a"));
				LinkTag linkTag = (LinkTag) links.elementAt(0);
				String link = CreatePartialLink(TSConnectionURL.idGET)
						+ "assessor/cama/" + linkTag.extractLink().trim().replaceAll("\\s", "%20")
								.replace("search.php~", "mysearch.php?");
				for (TableColumn column : columns) {
					linkTag = (LinkTag) column.getChildren()
							.extractAllNodesThatMatch(new TagNameFilter("a"))
							.elementAt(0);
					linkTag.setLink(link);
					linkTag.removeAttribute("target");
				}

				ParsedResponse currentResponse = new ParsedResponse();
				currentResponse.setAttribute(
						ParsedResponse.SERVER_ROW_RESPONSE, row.toHtml());
				currentResponse.setOnlyResponse(row.toHtml());
				currentResponse.setPageLink(new LinkInPage(link, link,
						TSServer.REQUEST_SAVE_TO_TSD));

				ResultMap m = parseIntermediaryRow(row, searchId);
				Bridge bridge = new Bridge(currentResponse, m, searchId);

				DocumentI document = (AssessorDocumentI) bridge.importData();
				currentResponse.setDocument(document);

				intermediaryResponse.add(currentResponse);
			}
			response.getParsedResponse()
					.setHeader(
							"<table width=\"98%\" align=\"center\" border=\"1\" cellspacing=\"0\" cellpadding=\"2\">\n"
									+ "<tr>\n<td class=\"header\" style=\"width:100px\">APN</td>\n<td class=\"header\" style=\"width:22px\">Card</td>\n<td class=\"header\">Situs</td>\n<td class=\"header\" style=\"width:111px\">Owner Name</td>\n<td class=\"header\">Mailing Address</td>\n<td class=\"header\">Last<br>Transaction Date</td>\n</tr>");
			response.getParsedResponse().setFooter("</table>");
			outputTable.append(table);

		} catch (Throwable t) {
			logger.error("Error while parsing intermediary data", t);
		}

		return intermediaryResponse;
	}

	private ResultMap parseIntermediaryRow(TableRow row, long searchId) {
		ResultMap resultMap = new ResultMap();
		resultMap.put("OtherInformationSet.SrcType", "AO");

		TableColumn[] cols = row.getColumns();
		if (cols.length != 6)
			return resultMap;

		// Parcel ID
		resultMap.put("PropertyIdentificationSet.ParcelID", cols[0]
				.toPlainTextString().trim().replaceAll("-", ""));

		// Address
		String fullAddress = cols[2].toPlainTextString().trim();
		String[] address = StringFormats.parseAddress(fullAddress);
		if (!address[0].equals("0"))
			resultMap.put("PropertyIdentificationSet.StreetNo", address[0]);
		resultMap.put("PropertyIdentificationSet.StreetName", address[1]);

		// Owner name
		String nameOnServer = cols[3].toPlainTextString()
				.replaceAll("\\s+", " ").trim();
		nameOnServer = nameOnServer.replace("&amp;", "&");
		resultMap.put("PropertyIdentificationSet.NameOnServer", nameOnServer);

		@SuppressWarnings("rawtypes")
		List<List> body = new ArrayList<List>();
		String[] names = StringFormats.parseNameFMWFWML(nameOnServer,
				new Vector<String>());
		String[] types = GenericFunctions.extractAllNamesType(names);
		String[] otherTypes = GenericFunctions.extractAllNamesOtherType(names);
		String[] suffixes = GenericFunctions.extractAllNamesSufixes(names);

		GenericFunctions.addOwnerNames(names, suffixes[0], suffixes[1], types,
				otherTypes, NameUtils.isCompany(names[2]),
				NameUtils.isCompany(names[5]), body);

		try {
			GenericFunctions.storeOwnerInPartyNames(resultMap, body, true);
		} catch (Exception e) {
			logger.error("Error while storing owner in party names", e);
		}

		return resultMap;
	}

	private void parseSearch(ServerResponse response) {
		String rsResponse = response.getResult();
		ParsedResponse parsedResponse = response.getParsedResponse();

		// Check if no results were found.
		if (rsResponse.indexOf("No results were found") > -1) {
			response.getParsedResponse()
					.setError(
							"No results were found for your query! Please change your search criteria and try again.");
			return;
		}

		// Create the response.
		StringBuilder outputTable = new StringBuilder();
		Collection<ParsedResponse> smartParsedResponses = smartParseIntermediary(
				response, rsResponse, outputTable);

		if (smartParsedResponses.size() > 0) {
			parsedResponse.setResultRows(new Vector<ParsedResponse>(
					smartParsedResponses));
			parsedResponse.setOnlyResponse(outputTable.toString());
			parsedResponse.setAttribute(ParsedResponse.SERVER_ROW_RESPONSE,
					outputTable.toString());
		}
	}

	@Override
	protected void ParseResponse(String sAction, ServerResponse response,
			int viParseID) throws ServerResponseException {
		if (viParseID != ID_GET_LINK && viParseID != ID_SAVE_TO_TSD
				&& response.getResult().contains("WASHOE COUNTY QUICK INFO"))
			viParseID = ID_GET_LINK;
		switch (viParseID) {
		case ID_SEARCH_BY_NAME:
		case ID_SEARCH_BY_ADDRESS:
		case ID_SEARCH_BY_BOOK_AND_PAGE:
		case ID_SEARCH_BY_PARCEL:
			// Search by owner name.
			parseSearch(response);
			break;
		case ID_GET_LINK:

		case ID_DETAILS:
		case ID_SAVE_TO_TSD:

			StringBuilder accountId = new StringBuilder();
			String details = null;
			try {
				details = getDetails(response.getResult(), accountId);
			} catch (ParserException e) {
				logger.error("Error while getting details", e);
				return;
			}
			if (viParseID != ID_SAVE_TO_TSD) {
				String originalLink = sAction.replace("?", "&") + "&"
						+ response.getRawQuerry();
				String sSave2TSDLink = getLinkPrefix(TSConnectionURL.idPOST)
						+ originalLink;

				HashMap<String, String> data = new HashMap<String, String>();
				data.put("type", "ASSESSOR");
				data.put("dataSource", "AO");
				if (isInstrumentSaved(accountId.toString(), null, data)) {
					details += CreateFileAlreadyInTSD();
				} else {
					mSearch.addInMemoryDoc(sSave2TSDLink, details);
					details = addSaveToTsdButton(details, sSave2TSDLink,
							ID_DETAILS);
				}

				response.getParsedResponse().setPageLink(
						new LinkInPage(sSave2TSDLink, originalLink,
								TSServer.REQUEST_SAVE_TO_TSD));
				response.getParsedResponse().setResponse(details);

			} else {
				String filename = accountId + ".html";
				smartParseDetails(response, details);

				msSaveToTSDFileName = filename;
				response.getParsedResponse().setFileName(
						getServerTypeDirectory() + msSaveToTSDFileName);
				response.getParsedResponse().setResponse(details);

				msSaveToTSDResponce = details + CreateFileAlreadyInTSD();

			}

			break;
		}
	}

	/**
	 * @param extractLink
	 *            The link to be extracted.
	 * @param withParagraph
	 *            The required information is inside a paragraph
	 * @return A html table with the information
	 */
	private String getLinkContent(String extractLink, String id,
			boolean withParagraph) {
		String transferHistoryHtml = getLinkContents(extractLink);
		try {
			org.htmlparser.Parser htmlParser = org.htmlparser.Parser
					.createParser(transferHistoryHtml, null);
			NodeList nodeList = htmlParser.parse(null);
			NodeList nodes = nodeList.extractAllNodesThatMatch(
					new HasAttributeFilter("id", "content_interior_hidden"),
					true);
			if (nodes.size() != 1)
				throw new Exception("Bad response.");
			Node node = null;
			if (withParagraph) {
				// Our table is inside a paragraph.
				node = nodes.elementAt(0).getChildren()
						.extractAllNodesThatMatch(new TagNameFilter("p"))
						.elementAt(0).getChildren()
						.extractAllNodesThatMatch(new TagNameFilter("table"))
						.elementAt(0);
			} else {
				node = nodes.elementAt(0).getChildren()
						.extractAllNodesThatMatch(new TagNameFilter("table"))
						.elementAt(0);
			}
			((TableTag) node).setAttribute("width", "100%");
			((TableTag) node).setAttribute("id", '"' + id + '"');
			return node.toHtml();
		} catch (Exception e) {
			logger.error("Error while parsing link content" + extractLink, e);
		}
		return "";
	}

	private String getDetails(String rsResponse, StringBuilder accountId)
			throws ParserException {
		StringBuilder details = new StringBuilder();
		org.htmlparser.Parser htmlParser = org.htmlparser.Parser.createParser(
				rsResponse, null);
		NodeList nodeList = htmlParser.parse(null);

		// Generate account id
		accountId.append(ro.cst.tsearch.servers.functions.NVWashoeAO
				.getParcelId(nodeList));

		/* If from memory - use it as is */
		if (rsResponse.contains("<html") == false) {
			return rsResponse;
		}

		NodeList nodes = nodeList.extractAllNodesThatMatch(
				new HasAttributeFilter("class", "quickinfo_subgrp"), true);

		generateLinks(nodes.elementAt(0));
		// Append CSS.
		details.append("<style type=\"text/css\">\n<!--\n.quickinfo_shell {\n       margin-left: auto;\n      margin-right: auto;\n           padding: 0px 0px 0px 0px;\n             ;\n      border-color: #fff;\n      border-width: 0px;\n      border-style: solid;\n  background-color: #fff;\n   border-collapse: collapse;\n}\n.quickinfo_shell td.tdbutton {\n  background-color: #aad;\n         font-size: 11px;\n        text-align: center;\n  vertical-align: middle;\n}\n.quickinfo_subgrp {\n       margin-left: auto;\n      margin-right: auto;\n           padding: 0px 0px 0px 0px;\n             \n      border-color: #333;\n      border-width: 0px;\n      border-style: solid;\n  background-color: #fff;\n   border-collapse: collapse;\n}\n.quickinfo_subgrp td {\n           padding: 2px;\n      border-width: 1px;\n      border-style: solid;\n      border-color: #999;\n  background-color: #fff;\n         font-size: 11px;\n             color: #000;\n    vertical-align: top;\n            height: 14;\n}\n.quickinfo_subgrp td.closed {\n      border-color: #c00;\n  background-color: #fff;\n}\n.quickinfo_subgrp td.closed a {\n       font-weight: bold;\n         font-size: 14px;\n             color: #c00;\n}\n.quickinfo_subgrp td.topheader {\n  background-color: #000;\n       font-weight: normal;\n             color: #fff;\n        text-align: center;\n}\n.quickinfo_subgrp td.header {\n  background-color: #ccc;\n         font-size: 11px;\n       font-weight: bold;\n        text-align: center;\n}\n.quickinfo_subgrp td.data {\n  background-color: #fff;\n         font-size: 11px;\n       font-weight: normal;\n        text-align: left;\n}\n.quickinfo_subgrp td.data_rt {\n  background-color: #fff;\n         font-size: 11px;\n       font-weight: normal;\n        text-align: right;\n}\n.quickinfo_subgrp td.data_ct {\n  background-color: #fff;\n         font-size: 11px;\n       font-weight: normal;\n        text-align: center;\n}\n.quickinfo_subgrp td.data a {\n       font-weight: bold;\n   text-decoration: none;\n}\n.quickinfo_subgrp td.data_ct a {\n       font-weight: bold;\n   text-decoration: none;\n}\n.quickinfo_subgrp td.data_rt a {\n       font-weight: bold;\n   text-decoration: none;\n}\n.quickinfo_subgrp td.label {\nfont-family: Verdana, Arial, Helvetica, sans-serif; font-size: 12px;\n  background-color: #fff;\n         font-size: 9px;\n       font-weight: bold;\n             color: #306;\n        text-align: right;\n}\n.quickinfo_subgrp td.label a {\nfont-family: Verdana, Arial, Helvetica, sans-serif; font-size: 12px;\n       font-weight: bold;\n   text-decoration: none;\n}\n\n.quickinfo_shell td.pageheader {\n          padding: 4px;\n      font-weight: bold;\n        font-size: 14px;\n            color: #fff;\n background-color: #88a;\n       text-align: left\n}\n.quickinfo_shell td.pageheader2 {\n          padding: 12px;\n      font-weight: bold;\n        font-size: 14px;\n            color: #000;\n background-color: #fff;\n       text-align: center;\n}\n.quickinfo_shell td.shelldata {\n            width: 50%;\n          padding: 5px;\n background-color: #fff;\n}\n\n.quickinfo_form {\n       margin-left: auto;\n      margin-right: auto;\n           padding: 0px 0px 0px 0px;\n             ;\n      border-color: #333;\n      border-width: 0px;\n      border-style: solid;\n  background-color: #fff;\n   border-collapse: collapse;\n}\n.quickinfo_form td {\n           padding: 5px 2px 5px 5px;\n      border-width: 1px;\n      border-style: solid;\n      border-color: #666;\n  background-color: #fff;\n         font-size: 11px;\n             color: #000;\n    vertical-align: top;\n            height: 30px;\n}\n.quickinfo_form td.header_form {\n  background-color: #ccc;\n         font-size: 12px;\n       font-weight: bold;\n        text-align: center;\n  border-top-width: 3px;\nborder-bottom-width: 0px;\n}\n.quickinfo_form td.label {\n  background-color: #fff;\n         font-size: 11px;\n       font-weight: bold;\n             color: #306;\n        text-align: right;\n}\n.quickinfo_form td.header_form_lt {\n  background-color: #ccc;\n         font-size: 12px;\n       font-weight: bold;\n        text-align: left;\n  border-top-width: 3px;\nborder-bottom-width: 0px;\n}\n.quickinfo_form td.tdbutton {\n  background-color: #aad;\n         font-size: 11px;\n        text-align: center;\n  border-top-width: 0px;\n}\n.quickinfo_form td.data {\n   background-color: #fff;\n          font-size: 11px;\n        font-weight: normal;\n         text-align: left;\n   border-top-width: 0px;\nborder-bottom-width: 0px;\n}\n.quickinfo_form td.data_ct {\n   background-color: #fff;\n          font-size: 11px;\n        font-weight: normal;\n         text-align: center;\n}\n.quickinfo_form td.help_data {\n   background-color: #fff;\n          font-size: 11px;\n        font-weight: normal;\n         text-align: left;\n   border-top-width: 0px;\nborder-bottom-width: 1px;\n}\n.quickinfo_shell td.error_search {\n              padding:4px;\n   background-color: #ffc;\n          font-size: 12px;\n        font-weight: bold;\n         text-align: center;\n      color: #c00;\n      border-color: #c00;\n      border-width: 2px;\n      border-style: solid;\n}\n.closed {\n  border-width:0px;\n}\n.data, .header, .data_ct, .topheader { font-family: Verdana, Arial, Helvetica, sans-serif; font-size: 12px; } \n\n-->\n</style>\n");
		// Append header.
		details.append("<table width=\"50%\" border=\"0\" align=\"center\" cellpadding=\"0\" cellspacing=\"0\">\n<tr>\n<td colspan=\"2\">\n");
		TableTag headerTable = (TableTag) nodes.elementAt(1);
		headerTable.setAttribute("align", "center");
		details.append(headerTable.toHtml());

		for (int i = 2; i <= 5; i++) {
			// Add owner/building/land details.
			if (i == 2)
				((TableTag) nodes.elementAt(i)).setAttribute("id",
						"\"info-table\"");
			else if (i == 5)
				((TableTag) nodes.elementAt(i)).setAttribute("id",
						"\"valuation-table\"");

			details.append("</td></tr><tr valign=\"top\"><td><br/><br/>");
			details.append("\n"
					+ ro.cst.tsearch.servers.functions.NVWashoeAO
							.sanitizeTable((TableTag) nodes.elementAt(i)));
		}

		// Append transfer history.
		details.append("</td></tr><tr valign=\"top\"><td><br/><br/>");
		details.append("\n"
				+ getLinkContent(transferHistoryLink, "sales-transfers", false));

		// Append additional owners.
		details.append("</td></tr><tr valign=\"top\"><td><br/><br/>");
		details.append("\n"
				+ getLinkContent(additionalOwnersLink, "owners", true));

		details.append("</td></tr></table>");

		return ro.cst.tsearch.servers.functions.NVWashoeAO.stripLinks(details
				.toString());
	}

	/**
	 * Generates links for transfer history and additional owners.
	 * 
	 * @param node
	 */
	private void generateLinks(Node node) {
		TableTag table = (TableTag) node;
		TableColumn column = table.getRows()[1].getColumns()[1];
		NodeList links = column.getChildren().extractAllNodesThatMatch(
				new TagNameFilter("a"));
		transferHistoryLink = baseLink
				+ ((LinkTag) links.elementAt(0)).getLink();
		additionalOwnersLink = baseLink
				+ ((LinkTag) links.elementAt(1)).getLink();
	}

	@Override
	protected Object parseAndFillResultMap(ServerResponse response,
			String detailsHtml, ResultMap map) {
		try {
			ro.cst.tsearch.servers.functions.NVWashoeAO.parseAndFillResultMap(
					detailsHtml, map, searchId, miServerID);
		} catch (ParserException e) {
			logger.error("Error while parsing and filling result map", e);
		}
		return null;
	}

	@Override
	protected void setModulesForAutoSearch(TSServerInfo serverInfo) {
		List<TSServerInfoModule> modules = new ArrayList<TSServerInfoModule>();
		TSServerInfoModule module = null;

		FilterResponse addressFilter = AddressFilterFactory
				.getAddressHybridFilter(searchId, 0.8d, true);
		FilterResponse nameFilterHybrid = NameFilterFactory
				.getHybridNameFilter(SearchAttributes.OWNER_OBJECT, searchId,
						module);

		if (hasPin()) {
			// Search by PIN.
			module = new TSServerInfoModule(
					serverInfo.getModule(TSServerInfo.PARCEL_ID_MODULE_IDX));
			modules.add(module);
		}

		if (hasStreet()) {
			// Search by Property Address.
			module = new TSServerInfoModule(
					serverInfo.getModule(TSServerInfo.ADDRESS_MODULE_IDX));
			
			module.addFilter(addressFilter);
			module.addFilter(nameFilterHybrid);
			modules.add(module);
		}

		if (hasOwner()) {
			// Search by Property Owner.
			module = new TSServerInfoModule(
					serverInfo.getModule(TSServerInfo.NAME_MODULE_IDX));
			module.clearSaKeys();
			module.setSaObjKey(SearchAttributes.OWNER_OBJECT);
			module.setIteratorType(0,
					FunctionStatesIterator.ITERATOR_TYPE_FIRST_NAME_FAKE);
			module.setIteratorType(1,
					FunctionStatesIterator.ITERATOR_TYPE_LAST_NAME_FAKE);
			module.addFilter(nameFilterHybrid);
			module.addFilter(addressFilter);
			module.addIterator((ConfigurableNameIterator) ModuleStatesIteratorFactory
					.getConfigurableNameIterator(module, searchId,
							new String[] { "L;F;", "L;M;" }));
			modules.add(module);
		}

		serverInfo.setModulesForAutoSearch(modules);
	}
}
