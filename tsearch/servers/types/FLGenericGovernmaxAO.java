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
import ro.cst.tsearch.extractor.xml.ResultMap;
import ro.cst.tsearch.search.FunctionStatesIterator;
import ro.cst.tsearch.search.filter.FilterResponse;
import ro.cst.tsearch.search.filter.newfilters.address.AddressFilterFactory;
import ro.cst.tsearch.search.filter.newfilters.name.NameFilterFactory;
import ro.cst.tsearch.search.iterator.ModuleStatesIteratorFactory;
import ro.cst.tsearch.search.module.ConfigurableNameIterator;
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
public abstract class FLGenericGovernmaxAO extends TSServer {
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	public FLGenericGovernmaxAO(long searchId) {
		super(searchId);
	}

	public FLGenericGovernmaxAO(String rsRequestSolverName, String rsSitePath,
			String rsServerID, String rsPrmNameLink, long searchId,
			int miServerID) {
		super(rsRequestSolverName, rsSitePath, rsServerID, rsPrmNameLink,
				searchId, miServerID);
	}

	/**
	 * Replace the external link(s) from a table row. Returns the first internal
	 * link.
	 * 
	 * @param row
	 * @return
	 */
	private String replaceExternalLinks(TableRow row) {
		if (row == null || row.getColumnCount() == 0)
			return null;
		TableColumn[] columns = row.getColumns();
		String firstLink = null;
		for (TableColumn column : columns) {
			NodeList links = column.getChildren().extractAllNodesThatMatch(
					new TagNameFilter("a"));
			if (links.size() < 1)
				// Not expected.
				continue;
			LinkTag linkTag = (LinkTag) links.elementAt(0);
			String link = CreatePartialLink(TSConnectionURL.idGET)
					+ linkTag
							.extractLink()
							.trim()
							.replaceAll("\\.\\./", "")
							.replace("tab_sale_v1001.asp?t_nm=sale",
									"tab_parcel_v1002.asp?t_nm=base");
			NodeList nodes = column.getChildren().extractAllNodesThatMatch(
					new TagNameFilter("a"));
			if (nodes.size() > 0) {
				linkTag = (LinkTag) nodes.elementAt(0);
				linkTag.setLink(link);
			}

			if (firstLink == null)
				firstLink = link;
		}
		return firstLink;
	}

	@Override
	public Collection<ParsedResponse> smartParseIntermediary(
			ServerResponse response, String table, StringBuilder outputTable) {
		Vector<ParsedResponse> intermediaryResponse = new Vector<ParsedResponse>();
		table = table.replaceAll("(?is)<p>\\s*<br>\\s*<b>\\s*(First|Previous|Next|Last)\\s*</b>\\s*</br>", "<br><b>$1</b></br>");
		org.htmlparser.Parser htmlParser = org.htmlparser.Parser.createParser(
				table, null);
		NodeList nodes = null, mainTableNodes = null;
		try {
			nodes = htmlParser.parse(null).extractAllNodesThatMatch(
					new TagNameFilter("table"), true);

			mainTableNodes = nodes.extractAllNodesThatMatch(
					new HasAttributeFilter("BORDERCOLOR", "Black"), true);
			if (mainTableNodes.size() < 1) {
				return intermediaryResponse;
			}
		} catch (ParserException e) {
			logger.error("Error while parsing intermediary data", e);
			return intermediaryResponse;
		}
		TableTag mainTable = (TableTag) mainTableNodes.elementAt(0);
		TableRow[] rows = mainTable.getRows();
		int nrColumns = 0;
		String extraColumnName = "";
		// Parse every search result row.
		for (TableRow row : rows) {
			if (row.getColumnCount() == 3)
				nrColumns = 3;
			else if (row.getColumnCount() == 4)
				nrColumns = 4;
			else
				// Not a usable row.
				continue;
			TableColumn[] columns = row.getColumns();
			if (columns[0].getAttribute("BGCOLOR") != null) {
				if (nrColumns == 4)
					extraColumnName = columns[1].toPlainTextString();
				// Header row.
				continue;
			}

			// Change the external links to internal ones.
			String link = replaceExternalLinks(row);
			if (link == null)
				continue;

			ParsedResponse currentResponse = new ParsedResponse();
			currentResponse.setAttribute(ParsedResponse.SERVER_ROW_RESPONSE,
					row.toHtml());
			currentResponse.setOnlyResponse(row.toHtml());
			currentResponse.setPageLink(new LinkInPage(link, link,
					TSServer.REQUEST_SAVE_TO_TSD));

			ResultMap m = parseIntermediaryRow(row, searchId);
			Bridge bridge = new Bridge(currentResponse, m, searchId);

			DocumentI document = null;
			try {
				document = (AssessorDocumentI) bridge.importData();
			} catch (Exception e) {
				logger.error("Error while parsing intermediary data", e);
				return intermediaryResponse;
			}
			currentResponse.setDocument(document);

			intermediaryResponse.add(currentResponse);
		}

		// Add extra column.
		String additional = extraColumnName.length() == 0 ? ""
				: "<TD WIDTH=\"30%\"  ALIGN=\"Left\"  BGCOLOR=\"346633\" NOWRAP >	<FONT  FACE=\"Arial\" SIZE=\"2pt\" COLOR=\"white\"><B>"
						+ extraColumnName + "</B></FONT>	</TD>";
		response.getParsedResponse()
				.setHeader(
						"<TABLE WIDTH=80% BORDER=0 BORDERCOLOR=Black align=\"center\" VALIGN=TOP CELLSPACING=0 CELLPADDING=2><TR>	<TD WIDTH=\"15%\"  ALIGN=\"Left\"  BGCOLOR=\"346633\" NOWRAP >	<FONT  FACE=\"Arial\" SIZE=\"2pt\" COLOR=\"white\"><B>Parcel ID</B></FONT>	</TD> "
								+ additional
								+ "<TD WIDTH=\"30%\"  ALIGN=\"Left\"  BGCOLOR=\"346633\" NOWRAP >	<FONT  FACE=\"Arial\" SIZE=\"2pt\" COLOR=\"white\"><B>Address</B></FONT>	</TD>	<TD WIDTH=\"20%\"  ALIGN=\"Left\"  BGCOLOR=\"346633\" NOWRAP >	<FONT  FACE=\"Arial\" SIZE=\"2pt\" COLOR=\"white\"><B>Owner(Current)</B></FONT>	</TD></TR>");
		response.getParsedResponse().setFooter(
				"</table>" + getIntermediaryPagingTable(nodes));
		outputTable.append(table);

		return intermediaryResponse;
	}

	/**
	 * Returns the paging table html.
	 * 
	 * @param nodes
	 * @return
	 */
	private String getIntermediaryPagingTable(NodeList nodes) {
		nodes = nodes.extractAllNodesThatMatch(new HasAttributeFilter(
				"BORDERCOLOR", "GREEN"), true);
		if (nodes.size() < 1)
			return "";

		// Replace external links to internal ones.
		TableTag table = (TableTag) nodes.elementAt(0);
		for (TableRow row : table.getRows()) {
			replaceExternalLinks(row);
		}

		// Return the html
		return table.toHtml();
	}

	protected static String ctrim(String input) {
		return input.replace("&nbsp;", " ").replaceAll("\\s+", " ").trim();
	}

	protected static String ctrim(Node node) {
		return node.toPlainTextString().replace("&nbsp;", " ")
				.replaceAll("\\s+", " ").trim();
	}

	protected ResultMap parseIntermediaryRow(TableRow row, long searchId) {
		ResultMap resultMap = new ResultMap();
		resultMap.put("OtherInformationSet.SrcType", "AO");

		TableColumn[] cols = row.getColumns();
		String parcelId = "", fullAddress = "", owner = "";
		if (cols.length == 3) {
			parcelId = ctrim(cols[0].toPlainTextString());
			fullAddress = ctrim(cols[1].toPlainTextString());
			owner = ctrim(cols[2].toPlainTextString());
		} else if (cols.length == 4) {
			parcelId = ctrim(cols[0].toPlainTextString());
			fullAddress = ctrim(cols[2].toPlainTextString());
			owner = ctrim(cols[3].toPlainTextString());
		} else {
			// Not usable.
			logger.error("Unable to parse intermediary row");
			return resultMap;
		}

		// Parcel ID
		resultMap.put("PropertyIdentificationSet.ParcelID", parcelId);

		// Address
		ro.cst.tsearch.servers.functions.FLMartinAO.putAddress(resultMap, fullAddress);

		// Owner name
		ro.cst.tsearch.servers.functions.FLMartinAO.putOwnerNames(resultMap, owner);

		return resultMap;
	}

	private void parseSearch(ServerResponse response) {
		String rsResponse = response.getResult();
		ParsedResponse parsedResponse = response.getParsedResponse();

		// Check if no results were found.
		if (rsResponse.indexOf("No Records Found") > -1) {
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
		if (viParseID == ID_GET_LINK
				&& response.getResult().contains("Search By"))
			viParseID = ID_SEARCH_BY_PARCEL;
		if (viParseID != ID_GET_LINK && viParseID != ID_SAVE_TO_TSD
				&& response.getResult().contains("Owner Information&nbsp;"))
			viParseID = ID_GET_LINK;

		if (viParseID != ID_GET_LINK && viParseID != ID_SAVE_TO_TSD
				&& response.getResult().contains("Sale Information for")
				&& response.getLastURI().toString().contains("tab_sale")) {
			// Website redirected to sales page instead of Summary page.
			// Retrieve the correct page.
			response.setResult(getLinkContents(response
					.getLastURI()
					.toString()
					.replace("tab_sale_v1001.asp?t_nm=sale",
							"tab_parcel_v1002.asp?t_nm=base")));
			viParseID = ID_GET_LINK;
		}

		switch (viParseID) {
		case ID_SEARCH_BY_NAME:
		case ID_SEARCH_BY_ADDRESS:
		case ID_SEARCH_BY_PARCEL:
		case ID_SEARCH_BY_INSTRUMENT_NO:
		case ID_SEARCH_BY_MODULE19: // Legal description
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
				data.put("type","ASSESSOR");
				data.put("dataSource","AO");
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
	 * Retrieves document details
	 * 
	 * @param rsResponse
	 * @param accountId
	 * @return
	 * @throws ParserException
	 */
	public abstract String getDetails(String rsResponse, StringBuilder accountId)
			throws ParserException;

	@Override
	protected abstract Object parseAndFillResultMap(ServerResponse response,
			String detailsHtml, ResultMap map);

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
			module.setIteratorType(1,
					FunctionStatesIterator.ITERATOR_TYPE_LF_NAME_FAKE);
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
