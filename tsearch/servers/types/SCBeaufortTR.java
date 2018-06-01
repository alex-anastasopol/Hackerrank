package ro.cst.tsearch.servers.types;

import static ro.cst.tsearch.datatrace.Utils.setupSelectBox;

import java.io.File;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Vector;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.io.FileUtils;
import org.htmlparser.filters.HasAttributeFilter;
import org.htmlparser.filters.TagNameFilter;
import org.htmlparser.tags.TableRow;
import org.htmlparser.tags.TableTag;
import org.htmlparser.util.NodeList;

import ro.cst.tsearch.ServerConfig;
import ro.cst.tsearch.bean.SearchAttributes;
import ro.cst.tsearch.exceptions.ServerResponseException;
import ro.cst.tsearch.extractor.xml.Bridge;
import ro.cst.tsearch.extractor.xml.GenericFunctions2;
import ro.cst.tsearch.extractor.xml.ResultMap;
import ro.cst.tsearch.extractor.xml.ResultTable;
import ro.cst.tsearch.search.FunctionStatesIterator;
import ro.cst.tsearch.search.filter.FilterResponse;
import ro.cst.tsearch.search.filter.newfilters.address.AddressFilterFactory;
import ro.cst.tsearch.search.filter.newfilters.name.NameFilterFactory;
import ro.cst.tsearch.search.filter.newfilters.pin.PINFilterFactory;
import ro.cst.tsearch.search.iterator.ModuleStatesIteratorFactory;
import ro.cst.tsearch.search.module.ConfigurableNameIterator;
import ro.cst.tsearch.servers.TSConnectionURL;
import ro.cst.tsearch.servers.info.TSServerInfo;
import ro.cst.tsearch.servers.info.TSServerInfoModule;
import ro.cst.tsearch.servers.response.LinkInPage;
import ro.cst.tsearch.servers.response.OtherInformationSet.OtherInformationSetKey;
import ro.cst.tsearch.servers.response.ParsedResponse;
import ro.cst.tsearch.servers.response.PropertyIdentificationSet.PropertyIdentificationSetKey;
import ro.cst.tsearch.servers.response.SaleDataSet.SaleDataSetKey;
import ro.cst.tsearch.servers.response.ServerResponse;
import ro.cst.tsearch.servers.response.TaxHistorySet.TaxHistorySetKey;
import ro.cst.tsearch.servlet.BaseServlet;
import ro.cst.tsearch.utils.RequestParams;
import ro.cst.tsearch.utils.StringUtils;

import com.stewart.ats.base.document.DocumentI;
import com.stewart.ats.base.document.TaxDocumentI;

@SuppressWarnings("deprecation")
public class SCBeaufortTR extends TSServer {

	private static final long serialVersionUID = 1L;

	// for Neighborhood Code
	private static String NEIGHBORHOOD_CODE_SELECT = "";
	private static String errorMessage = "Could not bring information";
	private static String detailsProblem = "There was a problem retriving the information. If you need this information please try again.<br>";

	static {
		String folderPath = ServerConfig
				.getModuleDescriptionFolder(BaseServlet.REAL_PATH
						+ "WEB-INF/classes/resource/module/comments/");
		File folder = new File(folderPath);
		if (!folder.exists() || !folder.isDirectory()) {
			throw new RuntimeException("The folder [" + folderPath
					+ "] does not exist. Module Information not loaded!");
		}
		try {
			String selects = FileUtils.readFileToString(new File(folderPath
					+ File.separator + "SCBeaufortTRNeighborhoodCode.xml"));
			NEIGHBORHOOD_CODE_SELECT = selects;
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public static String getNEIGHBORHOOD_CODE_SELECT() {
		return NEIGHBORHOOD_CODE_SELECT;
	}

	public SCBeaufortTR(long searchId) {
		super(searchId);
	}

	public SCBeaufortTR(String rsRequestSolverName, String rsSitePath,
			String rsServerID, String rsPrmNameLink, long searchId,
			int miServerID) {
		super(rsRequestSolverName, rsSitePath, rsServerID, rsPrmNameLink,
				searchId, miServerID);
	}

	@Override
	public TSServerInfo getDefaultServerInfo() {

		TSServerInfo msiServerInfoDefault = super.getDefaultServerInfo();
		TSServerInfoModule tsServerInfoModule = msiServerInfoDefault
				.getModule(TSServerInfo.GENERIC_MODULE_IDX);

		if (tsServerInfoModule != null) {
			setupSelectBox(tsServerInfoModule.getFunction(10),
					NEIGHBORHOOD_CODE_SELECT);
		}

		setModulesForGoBackOneLevelSearch(msiServerInfoDefault);
		return msiServerInfoDefault;
	}

	@Override
	protected void ParseResponse(String sAction, ServerResponse Response,
			int viParseID) throws ServerResponseException {

		String rsResponse = Response.getResult();
		ParsedResponse parsedResponse = Response.getParsedResponse();

		// if the search by name or the search by address return a single result
		// the details page is displayed (without intermediary page)
		if ((viParseID == ID_SEARCH_BY_NAME || viParseID == ID_SEARCH_BY_ADDRESS)
				&& rsResponse
						.contains("Beaufort County makes every effort to produce the most accurate information possible"))
			viParseID = ID_DETAILS;

		switch (viParseID) {
		case ID_SEARCH_BY_NAME:
		case ID_SEARCH_BY_ADDRESS:
		case ID_SEARCH_BY_INSTRUMENT_NO: // Search by Alternate ID (AIN)
		case ID_SEARCH_BY_MODULE30: // Search by Legal Description
		case ID_SEARCH_BY_SALES:

			if (rsResponse.indexOf("Overview") > -1) {
				Response.getParsedResponse().setError("No results found");
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

		case ID_SEARCH_BY_PARCEL:
		case ID_DETAILS:
		case ID_SAVE_TO_TSD:

			if (rsResponse.indexOf("Overview") == -1) {
				Response.getParsedResponse().setError("No results found");
				return;
			}

			if (rsResponse.indexOf("Taxes are missing") > -1) {
				Response.getParsedResponse().setError(
						"Error! Taxes are missing! Please try again!");
				return;
			}

			StringBuilder serialNumber = new StringBuilder();
			String details = getDetails(rsResponse, serialNumber);
			String filename = serialNumber + ".html";

			if (viParseID != ID_SAVE_TO_TSD) {
				String originalLink = sAction.replace("?", "&") + "&"
						+ Response.getRawQuerry();
				String sSave2TSDLink = getLinkPrefix(TSConnectionURL.idPOST)
						+ originalLink;

				HashMap<String, String> data = new HashMap<String, String>();
				loadDataHash(data);
				if (isInstrumentSaved(serialNumber.toString(), null, data)) {
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
			if (rsResponse.contains("Search by Owner Name"))
				ParseResponse(sAction, Response, ID_SEARCH_BY_NAME);
			else if (rsResponse.contains("Search by Street Address"))
				ParseResponse(sAction, Response, ID_SEARCH_BY_ADDRESS);
			else if (rsResponse.contains("Search by Alternate ID (AIN)"))
				ParseResponse(sAction, Response, ID_SEARCH_BY_INSTRUMENT_NO);
			else if (rsResponse.contains("Search by Legal&nbsp;Description"))
				ParseResponse(sAction, Response, ID_SEARCH_BY_MODULE30);
			else if (rsResponse.contains("Search by Sales"))
				ParseResponse(sAction, Response, ID_SEARCH_BY_SALES);
			else
				ParseResponse(sAction, Response, ID_DETAILS);
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

	protected String getDetails(String rsResponse, StringBuilder parcelNumber) {
		try {

			StringBuilder details = new StringBuilder();

			org.htmlparser.Parser htmlParser = org.htmlparser.Parser
					.createParser(rsResponse, null);
			NodeList nodeList = htmlParser.parse(null);

			/* If from memory - use it as is */
			if (!rsResponse.toLowerCase().contains("<html")) {
				NodeList parcelList = nodeList.extractAllNodesThatMatch(
						new TagNameFilter("span"), true)
						.extractAllNodesThatMatch(
								new HasAttributeFilter("class", "listdata"));
				String parcelID = parcelList.elementAt(0).toPlainTextString()
						.replaceAll("\\s", "");
				parcelNumber.append(parcelID);

				return rsResponse;
			}

			NodeList parcelList = nodeList.extractAllNodesThatMatch(
					new TagNameFilter("span"), true).extractAllNodesThatMatch(
					new HasAttributeFilter("class", "listdata"));
			String parcelID = parcelList.elementAt(0).toPlainTextString()
					.replaceAll("\\s", "");
			parcelNumber.append(parcelID);

			NodeList tables = nodeList
					.extractAllNodesThatMatch(new TagNameFilter("table"), true)
					.extractAllNodesThatMatch(
							new HasAttributeFilter("bordercolor", "Silver"))
					.extractAllNodesThatMatch(new TagNameFilter("table"), true)
					.extractAllNodesThatMatch(
							new HasAttributeFilter("width", "100%"));

			details.append("<font face=\"Verdana\"><b>Overview</b></font><br><br>");
			if (tables.size() >= 2)
				details.append(tables.elementAt(1).toHtml()
						.replaceAll("(?i)\\bcolor=\"(.*?)\"", "") // first table
						.replaceFirst("(?i)<table", "<table id=\"Overview1\""));
			if (tables.size() >= 3)
				details.append(tables.elementAt(2).toHtml()
						.replaceAll("(?i)\\bcolor=\"(.*?)\"", "") // second
																	// table
						.replaceAll("(?i)href=\"(.*?)\"", "") // remove links
						.replaceFirst("(?i)<table", "<table id=\"Overview2\""));

			NodeList linkList = nodeList.extractAllNodesThatMatch(
					new TagNameFilter("a"), true).extractAllNodesThatMatch(
					new HasAttributeFilter("class", "menulevel2"));

			String parcelDetails = "";
			Matcher matcher = Pattern.compile("(?i)href=\\\"(.*?)\\\"")
					.matcher(linkList.elementAt(0).toHtml());
			if (matcher.find())
				parcelDetails = getAnotherDetails(matcher.group(1), 0);
			if (StringUtils.isNotEmpty(parcelDetails))
				details.append(parcelDetails);

			String landDetails = "";
			matcher = Pattern.compile("(?i)href=\\\"(.*?)\\\"").matcher(
					linkList.elementAt(1).toHtml());
			if (matcher.find())
				landDetails = getAnotherDetails(matcher.group(1), 1);
			if (StringUtils.isNotEmpty(landDetails))
				details.append(landDetails);

			String improvementsDetails = "";
			matcher = Pattern.compile("(?i)href=\\\"(.*?)\\\"").matcher(
					linkList.elementAt(2).toHtml());
			if (matcher.find())
				improvementsDetails = getAnotherDetails(matcher.group(1), 2);
			if (StringUtils.isNotEmpty(improvementsDetails))
				details.append(improvementsDetails);

			String salesDetails = "";
			matcher = Pattern.compile("(?i)href=\\\"(.*?)\\\"").matcher(
					linkList.elementAt(3).toHtml());
			if (matcher.find())
				salesDetails = getAnotherDetails(matcher.group(1), 3);
			if (StringUtils.isNotEmpty(salesDetails))
				details.append(salesDetails);

			String taxesDetails = "";
			matcher = Pattern.compile("(?i)href=\\\"(.*?)\\\"").matcher(
					linkList.elementAt(4).toHtml());
			if (matcher.find())
				taxesDetails = getAnotherDetails(matcher.group(1), 4);
			if (StringUtils.isNotEmpty(taxesDetails))
				details.append(taxesDetails);
			if (details.indexOf(errorMessage) != -1) {
				details = new StringBuilder(
						"Error! Taxes are missing! Please try again!");
			} else {
				String valueHistoryDetails = "";
				matcher = Pattern.compile("(?i)href=\\\"(.*?)\\\"").matcher(
						linkList.elementAt(5).toHtml());
				if (matcher.find()) {
					if (matcher.group(1).contains("tab_assessments")) { // there
																		// is no
																		// Vehicle
																		// Tax
																		// Page
						valueHistoryDetails = getAnotherDetails(
								matcher.group(1), 6);
						if (StringUtils.isNotEmpty(valueHistoryDetails))
							details.append(valueHistoryDetails);
					}
				} else {
					String vehicleTaxDetails = "";
					vehicleTaxDetails = getAnotherDetails(matcher.group(1), 5);
					if (StringUtils.isNotEmpty(vehicleTaxDetails))
						details.append(vehicleTaxDetails);

					matcher = Pattern.compile("(?i)href=\\\"(.*?)\\\"")
							.matcher(linkList.elementAt(6).toHtml());
					if (matcher.find())
						valueHistoryDetails = getAnotherDetails(
								matcher.group(1), 6);
					if (StringUtils.isNotEmpty(valueHistoryDetails))
						details.append(valueHistoryDetails);
				}

			}

			return details.toString();

		} catch (Exception e) {
			logger.error("Error while getting details", e);
		}
		return null;
	}

	private String getAnotherDetails(String link, int type) {

		StringBuilder details = new StringBuilder();

		try {

			String detailHtml = getLinkContents(link.replaceAll("\\|", "%7C"));

			org.htmlparser.Parser htmlParser = org.htmlparser.Parser
					.createParser(detailHtml, null);
			NodeList nodeList = htmlParser.parse(null);

			NodeList tables = null;

			if (type == 4) // taxes page
				tables = nodeList.extractAllNodesThatMatch(
						new TagNameFilter("table"), true)
						.extractAllNodesThatMatch(
								new HasAttributeFilter("width", "625"));
			else if (type == 5) // vehicle tax page
				tables = nodeList.extractAllNodesThatMatch(
						new TagNameFilter("table"), true)
						.extractAllNodesThatMatch(
								new HasAttributeFilter("width", "625"));
			else if (type == 6) // value history
				tables = nodeList
						.extractAllNodesThatMatch(new TagNameFilter("table"),
								true)
						.extractAllNodesThatMatch(
								new HasAttributeFilter("height", "600"))
						.extractAllNodesThatMatch(new TagNameFilter("table"),
								true)
						.extractAllNodesThatMatch(
								new HasAttributeFilter("cellspacing", "0"));
			else
				tables = nodeList
						.extractAllNodesThatMatch(new TagNameFilter("table"),
								true)
						.extractAllNodesThatMatch(
								new HasAttributeFilter("bordercolor", "Silver"))
						.extractAllNodesThatMatch(new TagNameFilter("table"),
								true)
						.extractAllNodesThatMatch(
								new HasAttributeFilter("width", "100%"));

			String text = "";
			switch (type) {
			case 0:
				text = "Parcel";
				break;
			case 1:
				text = "Land";
				break;
			case 2:
				text = "Improvements";
				break;
			case 3:
				text = "Sales Disclosure";
				break;
			case 4:
				text = "Taxes";
				break;
			case 5:
				text = "Vehicle Tax Page";
				break;
			case 6:
				text = "Value History";
				break;
			}

			details.append("<br><font face=\"Verdana\"><b>" + text
					+ "</b></font><br><br>");

			String pageDetails = "";
			if (type == 4) { // taxes page
				if (tables.size() == 1)
					pageDetails = tables
							.elementAt(0)
							.toHtml()
							.replaceAll("(?i)\\bcolor=\"(.*?)\"", "")
							.replaceAll("(?is)<div id=\"payBtn\"(.*?)</div>",
									"")
							// remove pay form (if any)
							.replaceAll(
									"(?is)<div id=\"payOption\"(.*?)</div>", "")
							.replaceAll(
									"(?is)<P STYLE=\"padding:10px 0px 30px 0px;text-align:center\">(.*?)</P>",
									"")
							.replaceFirst("(?is)<table", "<table id=\"Taxes\"")
							.replaceAll("(?is)href=\"(.*?)\"", ""); // remove
																	// links
				if (pageDetails.length() == 0)
					pageDetails = errorMessage;
				details.append(pageDetails);
			} else if (type == 5) { // vehicle tax page
				if (tables.size() == 1)
					pageDetails = tables
							.elementAt(0)
							.toHtml()
							.replaceAll("(?i)\\bcolor=\"(.*?)\"", "")
							.replaceFirst("(?is)<table",
									"<table id=\"VehicleTax\"")
							.replaceAll("(?is)href=\"(.*?)\"", "") // remove
																	// links
							.replaceAll("(?is)<div id=\"payBtn\"(.*?)</div>",
									"") // remove pay form (if any)
							.replaceAll(
									"(?is)<div id=\"payOption\"(.*?)</div>", "");
				if (pageDetails.length() == 0)
					pageDetails = detailsProblem;
				details.append(pageDetails);
			} else if (type == 6) { // value history page
				if (tables.size() >= 2)
					pageDetails = tables
							.elementAt(1)
							.toHtml()
							.replaceAll("(?i)\\bcolor=\"(.*?)\"", "")
							.replaceFirst("(?is)<table",
									"<table id=\"ValueHistory\"");
				if (pageDetails.length() == 0)
					pageDetails = detailsProblem;
				details.append(pageDetails);
			} else {
				if (tables.size() >= 3)
					pageDetails = tables
							.elementAt(2)
							.toHtml()
							.replaceAll("(?i)\\bcolor=\"(.*?)\"", "")
							.replaceFirst(
									"(?is)<table",
									"<table id=\"" + text.replaceAll("\\s", "")
											+ "\"")
							.replaceAll("(?is)href=\"(.*?)\"", "") // remove
																	// links
							.replaceAll(
									"(?is)<a(.*?)(View Details|Print View)(.*?)</a\\s?>",
									" &nbsp; "); // remove View Details / Print
													// View
				if (pageDetails.length() == 0)
					pageDetails = detailsProblem;
				details.append(pageDetails);
			}
		} catch (Exception e) {
			logger.error("Error while getting details " + link, e);
		}

		return details.toString();
	}

	protected String createPartialLink(int iActionType, int dispatcher) {
		StringBuilder build = new StringBuilder(msRequestSolverName);
		build.append("?");
		build.append(msServerID);
		build.append("&");
		build.append(RequestParams.SEARCH_ID);
		build.append("=");
		build.append(mSearch.getSearchID());
		build.append("&");
		build.append("dispatcher=");
		build.append(dispatcher);
		return build.toString();
	}

	@Override
	public Collection<ParsedResponse> smartParseIntermediary(
			ServerResponse response, String table, StringBuilder outputTable) {
		Vector<ParsedResponse> intermediaryResponse = new Vector<ParsedResponse>();

		TableTag resultsTable = null;
		String header = "";
		String footer = "";

		try {
			org.htmlparser.Parser htmlParser = org.htmlparser.Parser
					.createParser(table, null);
			NodeList nodeList = htmlParser.parse(null);
			NodeList mainTable = nodeList.extractAllNodesThatMatch(
					new TagNameFilter("table"), true).extractAllNodesThatMatch(
					new HasAttributeFilter("BORDERCOLOR", "Black"), true);

			if (mainTable.size() != 0)
				resultsTable = (TableTag) mainTable.elementAt(0);

			// if there are results
			if (resultsTable != null && resultsTable.getRowCount() != 0) {
				TableRow[] rows = resultsTable.getRows();

				// row 0 is the header
				for (int i = 1; i < rows.length; i++) {
					TableRow row = rows[i];

					String htmlRow = row.toHtml();
					ParsedResponse currentResponse = new ParsedResponse();

					String link = row.getColumns()[0].toHtml();
					Matcher matcher = Pattern.compile("(?i)href=\"(.*?)\"")
							.matcher(link);
					if (matcher.find())
						link = matcher.group(1).replaceAll("(\\.\\./)+", "");

					link = CreatePartialLink(TSConnectionURL.idGET) + link;

					// replace the links
					htmlRow = htmlRow.replaceAll("(?i)href=\".*\"", "href="
							+ link);

					currentResponse.setPageLink(new LinkInPage(link, link,
							TSServer.REQUEST_SAVE_TO_TSD));

					currentResponse.setAttribute(
							ParsedResponse.SERVER_ROW_RESPONSE, htmlRow);
					currentResponse.setOnlyResponse(htmlRow);

					ResultMap m = ro.cst.tsearch.servers.functions.SCBeaufortTR
							.parseIntermediaryRow(row, searchId);
					Bridge bridge = new Bridge(currentResponse, m, searchId);

					DocumentI document = (TaxDocumentI) bridge.importData();
					currentResponse.setDocument(document);

					intermediaryResponse.add(currentResponse);

				}

				int columnCount = rows[0].getColumnCount();
				// Search by Owner Name, Search by Street Address
				if (columnCount == 3)
					header = "<table><tr>"
							+ "<TD WIDTH=\"15%\" ALIGN=\"Left\" NOWRAP><FONT FACE=\"Verdana\" SIZE=\"2pt\">Parcel ID</A></FONT></TD>"
							+ "<TD WIDTH=\"30%\" ALIGN=\"Left\" NOWRAP><FONT FACE=\"Verdana\" SIZE=\"2pt\">Address</A></FONT></TD>"
							+ "<TD WIDTH=\"20%\" ALIGN=\"Left\" NOWRAP><FONT FACE=\"Verdana\" SIZE=\"2pt\">Owner Name</A></FONT></TD>"
							+ "</tr>";
				// Search by Alternate ID (AIN)
				else if (columnCount == 4
						&& rows[0].getColumns()[1].toPlainTextString().trim()
								.equalsIgnoreCase("Alternate ID"))
					header = "<table><tr>"
							+ "<TD WIDTH=\"15%\" ALIGN=\"Left\" NOWRAP><FONT FACE=\"Verdana\" SIZE=\"2pt\">Parcel ID</A></FONT></TD>"
							+ "<TD WIDTH=\"30%\" ALIGN=\"Left\" NOWRAP><FONT FACE=\"Verdana\" SIZE=\"2pt\">Alternate ID</A></FONT></TD>"
							+ "<TD WIDTH=\"30%\" ALIGN=\"Left\" NOWRAP><FONT FACE=\"Verdana\" SIZE=\"2pt\">Address</A></FONT></TD>"
							+ "<TD WIDTH=\"20%\" ALIGN=\"Left\" NOWRAP><FONT FACE=\"Verdana\" SIZE=\"2pt\">Owner Name</A></FONT></TD>"
							+ "</tr>";
				// Search by Legal Description
				else if (columnCount == 4
						&& rows[0].getColumns()[1].toPlainTextString().trim()
								.equalsIgnoreCase("SL.LEGAL"))
					header = "<table><tr>"
							+ "<TD WIDTH=\"15%\" ALIGN=\"Left\" NOWRAP><FONT FACE=\"Verdana\" SIZE=\"2pt\">Parcel ID</A></FONT></TD>"
							+ "<TD WIDTH=\"10%\" ALIGN=\"Left\" NOWRAP><FONT FACE=\"Verdana\" SIZE=\"2pt\">SL.LEGAL</A></FONT></TD>"
							+ "<TD WIDTH=\"30%\" ALIGN=\"Left\" NOWRAP><FONT FACE=\"Verdana\" SIZE=\"2pt\">Address</A></FONT></TD>"
							+ "<TD WIDTH=\"20%\" ALIGN=\"Left\" NOWRAP><FONT FACE=\"Verdana\" SIZE=\"2pt\">Owner Name</A></FONT></TD>"
							+ "</tr>";
				// Search by Sales
				else if (columnCount == 13)
					header = "<table><tr>"
							+ "<TD WIDTH=\"15%\" ALIGN=\"Left\" NOWRAP><FONT FACE=\"Verdana\" SIZE=\"2pt\">Parcel ID</A></FONT></TD>"
							+ "<TD WIDTH=\"15%\" ALIGN=\"Left\" NOWRAP><FONT FACE=\"Verdana\" SIZE=\"2pt\">Owner Name</A></FONT></TD>"
							+ "<TD WIDTH=\"6%\" ALIGN=\"Center\" NOWRAP><FONT FACE=\"Verdana\" SIZE=\"2pt\">Sale Date</A></FONT></TD>"
							+ "<TD WIDTH=\"6%\" ALIGN=\"Center\" NOWRAP><FONT FACE=\"Verdana\" SIZE=\"2pt\">Total Sale</A></FONT></TD>"
							+ "<TD WIDTH=\"6%\" ALIGN=\"Center\" NOWRAP><FONT FACE=\"Verdana\" SIZE=\"2pt\">Dist. No.</A></FONT>"
							+ "<TD WIDTH=\"6%\" ALIGN=\"Center\" NOWRAP><FONT FACE=\"Verdana\" SIZE=\"2pt\">Township No.</A></FONT></TD>"
							+ "<TD WIDTH=\"6%\" ALIGN=\"Center\" NOWRAP><FONT FACE=\"Verdana\" SIZE=\"2pt\">Neigh. Code</A></FONT></TD>"
							+ "<TD WIDTH=\"6%\" ALIGN=\"Center\" NOWRAP><FONT FACE=\"Verdana\" SIZE=\"2pt\">Year Const.</A></FONT></TD>"
							+ "<TD WIDTH=\"6%\" ALIGN=\"Center\" NOWRAP><FONT FACE=\"Verdana\" SIZE=\"2pt\">Full Bath</A></FONT></TD>"
							+ "<TD WIDTH=\"6%\" ALIGN=\"Center\" NOWRAP><FONT FACE=\"Verdana\" SIZE=\"2pt\">Half Bath</A></FONT></TD>"
							+ "<TD WIDTH=\"6%\" ALIGN=\"Center\" NOWRAP><FONT FACE=\"Verdana\" SIZE=\"2pt\">Floor Area</A></FONT></TD>"
							+ "<TD WIDTH=\"6%\" ALIGN=\"Center\" NOWRAP><FONT FACE=\"Verdana\" SIZE=\"2pt\">Bedroom</A></FONT></TD>"
							+ "<TD WIDTH=\"6%\" ALIGN=\"Center\" NOWRAP><FONT FACE=\"Verdana\" SIZE=\"2pt\">Story Ht.</A></FONT></TD>"
							+ "</tr>";

				footer = processLinks(response, nodeList);
				response.getParsedResponse().setHeader(header);
				response.getParsedResponse().setFooter(footer + "</table>");

				outputTable.append(table);
			}

		} catch (Throwable t) {
			logger.error("Error while parsing intermediary data", t);
		}

		return intermediaryResponse;
	}

	protected String processLinks(ServerResponse response, NodeList nodeList) {

		String originalLink = "";
		String newLink = "";

		StringBuilder footer = new StringBuilder(
				"<TABLE ALIGN=\"CENTER\" VALIGN=\"MIDDLE\" WIDTH=\"130\""
						+ " BORDER=\"0\" BORDERCOLOR=\"GREEN\" CELLSPACING=\"0\" CELLPADDING=\"3\"><TR>");
		NodeList tableList = nodeList.extractAllNodesThatMatch(
				new TagNameFilter("TABLE"), true).extractAllNodesThatMatch(
				new HasAttributeFilter("WIDTH", "130"));
		if (tableList.size() > 0) {
			TableTag linksTable = (TableTag) tableList.elementAt(0);
			TableRow row = linksTable.getRow(0);
			for (int i = 0; i < row.getColumnCount(); i++) {
				originalLink = "";
				String column = row.getColumns()[i].toHtml().replaceAll(
						"(?i)</a><p>", "<p>");
				Matcher matcher = Pattern.compile("(?i)href=\"(.*?)\"")
						.matcher(column);
				if (matcher.find())
					originalLink = matcher.group(1);
				if (originalLink.length() != 0) {
					newLink = originalLink.replaceAll("(\\.\\./)+", "");
					newLink = CreatePartialLink(TSConnectionURL.idGET)
							+ newLink;
					// replace the links
					column = column.replaceAll(
							originalLink.replaceAll("\\?", "\\\\?"), newLink);
					column = column.replaceAll("<a href=\"(.*?)\"",
							"<a href=\"" + newLink + "\"");
				}

				footer.append(column);
			}
		}
		return footer + "</TR></TABLE>";
	}

	@SuppressWarnings({ "rawtypes" })
	@Override
	protected Object parseAndFillResultMap(ServerResponse response,
			String detailsHtml, ResultMap resultMap) {
	
		try {
						
			resultMap.put(OtherInformationSetKey.SRC_TYPE.getKeyName(),"TR");
				
			org.htmlparser.Parser htmlParser = org.htmlparser.Parser.createParser(detailsHtml, null);
			NodeList nodeList = htmlParser.parse(null);
							
			NodeList parcelList = nodeList.extractAllNodesThatMatch(new TagNameFilter("span"), true)
				.extractAllNodesThatMatch(new HasAttributeFilter("class", "listdata"));
			String parcelID = parcelList.elementAt(0).toPlainTextString().replaceAll("\\s", "");
			resultMap.put(PropertyIdentificationSetKey.PARCEL_ID.getKeyName(), parcelID);
			resultMap.put(PropertyIdentificationSetKey.GEO_NUMBER.getKeyName(), parcelList.elementAt(1).toPlainTextString().trim());	//Alternate ID (AIN)
				
			TableTag table1 = (TableTag)nodeList.extractAllNodesThatMatch(new TagNameFilter("table"), true)
				.extractAllNodesThatMatch(new HasAttributeFilter("id", "Overview1")).elementAt(0);
			String address = table1.getRow(1).getColumns()[2].toPlainTextString().trim();
			if (address.endsWith(",")) address = address.substring(0, address.length()-1);
			resultMap.put(PropertyIdentificationSetKey.ADDRESS_ON_SERVER.getKeyName(), address);
				
			TableTag table2 = (TableTag)nodeList.extractAllNodesThatMatch(new TagNameFilter("table"), true)
				.extractAllNodesThatMatch(new HasAttributeFilter("id", "Overview2")).elementAt(0);
			TableTag innerTable1 = (TableTag)table2.getRow(0).getColumns()[0].getChild(1);
			TableTag innerTable2 = (TableTag)innerTable1.getRow(0).getColumns()[0].getChild(0);
			String owner = innerTable2.getRow(0).getColumns()[1].toPlainTextString().trim();
			resultMap.put(PropertyIdentificationSetKey.NAME_ON_SERVER.getKeyName(), owner);
				
			TableTag innerTable3 = (TableTag)innerTable1.getRow(1).getColumns()[0].getChild(0);
			String legal = innerTable3.getRow(0).getColumns()[1].toPlainTextString().trim();
			resultMap.put(PropertyIdentificationSetKey.LEGAL_DESCRIPTION_ON_SERVER.getKeyName(), legal);
									
			ResultTable rt = new ResultTable();			//tax history table
			List<List> tablebody = new ArrayList<List>();
			List<String> list;
			
			//tax history from Overview
			NodeList nodeTaxes = nodeList.extractAllNodesThatMatch(new TagNameFilter("table"), true)	
				.extractAllNodesThatMatch(new HasAttributeFilter("id", "VehicleTax"))
				.extractAllNodesThatMatch(new TagNameFilter("div"), true)
				.extractAllNodesThatMatch(new HasAttributeFilter("id", "tab_pmt_data"))
				.extractAllNodesThatMatch(new TagNameFilter("table"), true)
				.extractAllNodesThatMatch(new HasAttributeFilter("bordercolor", "#C0C0C0"))
				.extractAllNodesThatMatch(new HasAttributeFilter("border", "0"));
			if (nodeTaxes != null)
			{
				String receiptAmount = "";
				String receiptDate = "";
				String receiptNumber = "";
				for (int i=0;i<nodeTaxes.size();i++)
				{
					TableTag table = (TableTag)nodeTaxes.elementAt(i);
					TableRow row = table.getRow(1);
					receiptDate = row.getColumns()[0].toPlainTextString().trim();
					receiptAmount = row.getColumns()[1].toPlainTextString().replaceAll("[\\$,(&nbsp;)\\s]", "");
					receiptNumber = row.getColumns()[2].toPlainTextString().trim();
					list = new ArrayList<String>();
					list.add(receiptDate);
					list.add(receiptAmount);
					list.add(receiptNumber);
					tablebody.add(list);
				}	
			}
											
			String[] header = {TaxHistorySetKey.RECEIPT_DATE.getShortKeyName(), TaxHistorySetKey.RECEIPT_AMOUNT.getShortKeyName(), 
					TaxHistorySetKey.RECEIPT_NUMBER.getShortKeyName()};
			rt = GenericFunctions2.createResultTable(tablebody, header);
			if (rt != null){
				resultMap.put("TaxHistorySet", rt);
			}
			
			ResultTable transactionHistory = new ResultTable();			//transaction history table
			List<List> tablebodytrans = new ArrayList<List>();
			List<String> listtrans;
			
			NodeList nodeTransactionTable = nodeList.extractAllNodesThatMatch(new TagNameFilter("table"), true)	
				.extractAllNodesThatMatch(new HasAttributeFilter("id", "Overview2"))
				.extractAllNodesThatMatch(new TagNameFilter("table"), true)
				.extractAllNodesThatMatch(new HasAttributeFilter("cellspacing", "1"));
			if (nodeTransactionTable.size() >=4 )
			{
				TableTag transactionHistoryTable = (TableTag) nodeTransactionTable.elementAt(3);
				int rowsNumber = transactionHistoryTable.getRowCount();
				String grantor = "";
				String bookPage = "";
				String date = "";
				String documentType = "";
				String price = "";
				String[] bookAndPage = {"", ""};
				
				for (int i=1; i<rowsNumber; i++)			//row 0 is the header
				{
					TableRow row = transactionHistoryTable.getRow(i);
					bookPage = row.getColumns()[1].toPlainTextString().replaceAll("&nbsp;", "").trim();
					if (bookPage.length()!=0) { 
						bookAndPage = bookPage.split(" ");
						grantor = row.getColumns()[0].toPlainTextString().replaceAll("&nbsp;", "").trim();
						date = row.getColumns()[2].toPlainTextString().replaceAll("&nbsp;", "").trim();
						documentType = row.getColumns()[3].toPlainTextString().trim();
						price = row.getColumns()[5].toPlainTextString().replaceAll("[,\\$(&nbsp;)]", "").trim();
						
						listtrans = new ArrayList<String>();
						listtrans.add(grantor);
						listtrans.add(bookAndPage[0]);
						listtrans.add(bookAndPage[1]);
						listtrans.add(date);
						listtrans.add(documentType);
						listtrans.add(price);
						tablebodytrans.add(listtrans);
					}
				}
				
				String[] headertrans = {SaleDataSetKey.GRANTOR.getShortKeyName(), SaleDataSetKey.BOOK.getShortKeyName(), 
						SaleDataSetKey.PAGE.getShortKeyName(), SaleDataSetKey.RECORDED_DATE.getShortKeyName(), 
						SaleDataSetKey.DOCUMENT_TYPE.getShortKeyName(),	SaleDataSetKey.SALES_PRICE.getShortKeyName()};
				transactionHistory = GenericFunctions2.createResultTable(tablebodytrans, headertrans);
				if (transactionHistory != null){
					resultMap.put("SaleDataSet", transactionHistory);
				}
			}
						
			ro.cst.tsearch.servers.functions.SCBeaufortTR.parseNames(resultMap, searchId);
			ro.cst.tsearch.servers.functions.SCBeaufortTR.parseAddress(resultMap);
			ro.cst.tsearch.servers.functions.SCBeaufortTR.parseLegalSummary(resultMap);
			ro.cst.tsearch.servers.functions.SCBeaufortTR.parseTaxes(nodeList, resultMap, searchId);
			
			try {
				Calendar c = Calendar.getInstance();
				c.setTime(getDataSite().getPayDate());
				if (Integer.parseInt((String) resultMap
						.get(TaxHistorySetKey.YEAR.getKeyName())) != c
						.get(Calendar.YEAR)) {
					resultMap.put(TaxHistorySetKey.YEAR.getKeyName(),
							c.get(Calendar.YEAR) + "");
					resultMap.put(TaxHistorySetKey.BASE_AMOUNT.getKeyName(),
							"0.0");
					resultMap.put(TaxHistorySetKey.TOTAL_DUE.getKeyName(),
							"0.0");
					resultMap.put(TaxHistorySetKey.AMOUNT_PAID.getKeyName(),
							"0.0");
				}
			} catch (Exception e) {
				e.printStackTrace();
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
			String parcelID = getSearchAttribute(SearchAttributes.LD_PARCELNO);
			if (parcelID.length() == 18) // insert blanks in parcel ID
			{
				parcelID = parcelID.substring(0, 4) + " "
						+ parcelID.substring(4, 7) + " "
						+ parcelID.substring(7, 10) + " "
						+ parcelID.substring(10, 14) + " "
						+ parcelID.substring(14, 18);
			}
			FilterResponse pinFilter = PINFilterFactory
					.getDefaultPinFilter(searchId);
			module = new TSServerInfoModule(
					serverInfo.getModule(TSServerInfo.PARCEL_ID_MODULE_IDX));
			module.clearSaKeys();
			module.forceValue(0, parcelID);
			module.addFilter(pinFilter);
			moduleList.add(module);
		}

		boolean hasOwner = hasOwner();
		FilterResponse addressFilter = AddressFilterFactory
				.getAddressHybridFilter(searchId, 0.8d, true);

		if (hasStreet()) {
			FilterResponse nameFilter = NameFilterFactory.getHybridNameFilter(
					SearchAttributes.OWNER_OBJECT, searchId, module);
			module = new TSServerInfoModule(
					serverInfo.getModule(TSServerInfo.ADDRESS_MODULE_IDX));
			module.clearSaKeys();
			module.setSaKey(1, SearchAttributes.P_STREET_NO_NAME);
			module.addFilter(addressFilter);
			module.addFilter(nameFilter);
			moduleList.add(module);
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
			module.setIteratorType(1,
					FunctionStatesIterator.ITERATOR_TYPE_LFM_NAME_FAKE);
			module.addFilter(addressFilter);
			module.addFilter(nameFilterHybridDoNotSkipUnique);
			module.addIterator((ConfigurableNameIterator) ModuleStatesIteratorFactory
					.getConfigurableNameIterator(module, searchId,
							new String[] { "L;F;", "L;M;" }));
			moduleList.add(module);
		}

		serverInfo.setModulesForAutoSearch(moduleList);
	}
}
