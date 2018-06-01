package ro.cst.tsearch.servers.types;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Vector;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.htmlparser.filters.HasAttributeFilter;
import org.htmlparser.filters.TagNameFilter;
import org.htmlparser.tags.Div;
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
import ro.cst.tsearch.parser.HtmlParser3;
import ro.cst.tsearch.search.filter.FilterResponse;
import ro.cst.tsearch.search.filter.newfilters.address.AddressFilterFactory;
import ro.cst.tsearch.search.filter.newfilters.name.NameFilterFactory;
import ro.cst.tsearch.servers.HashCountyToIndex;
import ro.cst.tsearch.servers.TSConnectionURL;
import ro.cst.tsearch.servers.bean.DataSite;
import ro.cst.tsearch.servers.info.TSServerInfo;
import ro.cst.tsearch.servers.info.TSServerInfoModule;
import ro.cst.tsearch.servers.response.LinkInPage;
import ro.cst.tsearch.servers.response.ParsedResponse;
import ro.cst.tsearch.servers.response.PropertyIdentificationSet.PropertyIdentificationSetKey;
import ro.cst.tsearch.servers.response.ServerResponse;
import ro.cst.tsearch.utils.InstanceManager;
import ro.cst.tsearch.utils.StringUtils;

import com.stewart.ats.base.document.DocumentI;
import com.stewart.ats.base.document.TaxDocumentI;

/**
 * 
 * @author Oprina George
 * 
 *         Mar 30, 2011
 */

public class TNRutherfordYC extends TemplatedServer {

	/**
	 * 
	 */
	private static final long serialVersionUID = 4216620752340463418L;

	private int typeOfSearch = -1;

	public TNRutherfordYC(long searchId) {
		super(searchId);
		setSuper();
	}

	public TNRutherfordYC(String rsRequestSolverName, String rsSitePath,
			String rsServerID, String rsPrmNameLink, long searchId,
			int miServerID) {
		super(rsRequestSolverName, rsSitePath, rsServerID, rsPrmNameLink,
				searchId, miServerID);
		setSuper();
	}

	public void setSuper() {
		int[] intermediary_cases = new int[] { ID_SEARCH_BY_NAME,
				ID_SEARCH_BY_ADDRESS };
		super.setIntermediaryCases(intermediary_cases);
		super.setDetailsMessage("Property Detail");
	}

	public String getCityName() {
		DataSite dataSite = HashCountyToIndex.getDateSiteForMIServerID(
				InstanceManager.getManager().getCommunityId(searchId),
				getServerID());
		return dataSite.getCityName().toUpperCase();
	}

	@Override
	protected void setMessages() {
		getErrorMessages().addNoResultsMessages("No matches found");
		// getErrorMessages().addServerErrorMessage("No Records Returned");
	}

	@Override
	protected void ParseResponse(String action, ServerResponse response,
			int viParseID) throws ServerResponseException {

		this.typeOfSearch = viParseID;

		if (isError(response)) {
			response.setError("No data found for this criteria!");
			response.setResult("");
			return;
		}
		if (response.getResult().contains(
				"Please choose the correct name from the following results")
				|| viParseID == ID_SAVE_TO_TSD)
			super.ParseResponse(action, response, viParseID);
		else if (response.getResult().contains("Property Tax Search - Results"))
			super.ParseResponse(action, response, ID_DETAILS);

	}

	@Override
	protected HashMap<String, String> putAdditionalData(String serverResult) {
		HashMap<String, String> data = new HashMap<String, String>();
		data.put("type", "CITYTAX");
		return data;
	}

	@Override
	protected String getAccountNumber(String serverResult) {
		// Parcel #:
		try {
			NodeList nodeList;
			nodeList = org.htmlparser.Parser.createParser(serverResult, null)
					.parse(null);
			String account = HtmlParser3
					.getValueFromAbsoluteCell(0, 0,
							HtmlParser3.findNode(nodeList, "PARCEL NUMBER:"),
							"", true).replaceAll("<[^>]*>", "")
					.replace("PARCEL NUMBER:", "").replace(" ", "").trim();
			return account;
		} catch (ParserException e) {
			e.printStackTrace();
		}
		return "";
	}

	public ArrayList<String> getAllRows(TableRow tableRow, int typeOfSearch) {
		ArrayList<String> allRows = new ArrayList<String>();

		String resultsUrl = "http://www.lavergnetn.gov/records/protax/protaxresults.shtml";

		// get url and params from row
		HashMap<String, String> params = new HashMap<String, String>();

		String name_addr = "";

		TableColumn[] cols = tableRow.getColumns();
		if (cols.length == 1) {
			name_addr = cols[0].toPlainTextString().trim()
					.replaceAll("\\s+", " ");
		}

		params.put("EName", name_addr);
		params.put("SUBMIT", "SUBMIT");

		String url = "";
		if (typeOfSearch == 1)
			url = "http://www.lavergnetn.gov/records/protax/protaxname1.shtml";
		if (typeOfSearch == 2)
			url = "http://www.lavergnetn.gov/records/protax/protaxadd1.shtml";

		try {
			if (!name_addr.equals("")) {
				String intermediaryPage2 = "";
				intermediaryPage2 = getLinkContents(url + "&EName=" + name_addr);
				if (intermediaryPage2 != null && !intermediaryPage2.equals("")) {
					org.htmlparser.Parser htmlParser = org.htmlparser.Parser
							.createParser(intermediaryPage2, null);
					NodeList nodeList = htmlParser.parse(null);
					NodeList mainTableList = nodeList
							.extractAllNodesThatMatch(
									new TagNameFilter("form"), true)
							.extractAllNodesThatMatch(
									new HasAttributeFilter("action",
											"protaxresults.shtml"), true)
							.extractAllNodesThatMatch(
									new TagNameFilter("table"), true)
							.extractAllNodesThatMatch(
									new HasAttributeFilter("cellspacing", "5"),
									true)
							.extractAllNodesThatMatch(
									new HasAttributeFilter("cellpadding", "5"),
									true)
							.extractAllNodesThatMatch(
									new HasAttributeFilter("border", "1"), true);

					if (mainTableList.size() == 1) {
						TableTag tableTag = (TableTag) mainTableList
								.elementAt(0);

						TableRow[] rows = tableTag.getRows();
						for (int i = 1; i < rows.length; i++) {
							allRows.add(ro.cst.tsearch.servers.functions.TNRutherfordYC
									.get_name_addr_Row(name_addr, typeOfSearch,
											rows[i], resultsUrl));
						}

					}

				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		return allRows;
	}

	@Override
	public Collection<ParsedResponse> smartParseIntermediary(
			ServerResponse response, String table, StringBuilder outputTable) {
		Vector<ParsedResponse> intermediaryResponse = new Vector<ParsedResponse>();

		try {
			String link2intremediary = "";

			switch (typeOfSearch) {
			case ID_SEARCH_BY_NAME:
				link2intremediary = "protaxname1.shtml";
				break;
			case ID_SEARCH_BY_ADDRESS:
				link2intremediary = "protaxadd1.shtml";
				break;
			}

			org.htmlparser.Parser htmlParser = org.htmlparser.Parser
					.createParser(table, null);
			NodeList nodeList = htmlParser.parse(null);
			NodeList mainTableList = nodeList
					.extractAllNodesThatMatch(new TagNameFilter("form"), true)
					.extractAllNodesThatMatch(
							new HasAttributeFilter("action", link2intremediary),
							true)
					.extractAllNodesThatMatch(new TagNameFilter("table"), true)
					.extractAllNodesThatMatch(
							new HasAttributeFilter("cellspacing", "5"), true)
					.extractAllNodesThatMatch(
							new HasAttributeFilter("cellpadding", "5"), true)
					.extractAllNodesThatMatch(
							new HasAttributeFilter("border", "1"), true);

			if (mainTableList.size() == 0) {
				return intermediaryResponse;
			}

			// parse rows and concatenate with second intermediary page results
			TableTag tableTag = (TableTag) mainTableList.elementAt(0);

			TableRow[] rows = tableTag.getRows();

			ArrayList<String> allRows = new ArrayList<String>();

			for (int i = 1; i < rows.length; i++) {
				allRows.addAll(getAllRows(rows[i], typeOfSearch));
			}

			String allRows_String = "";
			for (String s : allRows) {
				allRows_String += s;
			}

			NodeList allRowList = new NodeList();

			allRowList = org.htmlparser.Parser
					.createParser(allRows_String, null).parse(null)
					.extractAllNodesThatMatch(new TagNameFilter("tr"));

			tableTag.setChildren(allRowList);

			tableTag = (TableTag) org.htmlparser.Parser
					.createParser(tableTag.toHtml(), null).parse(null)
					.extractAllNodesThatMatch(new TagNameFilter("table"))
					.elementAt(0);

			TableRow[] all_rows = tableTag.getRows();
			for (TableRow row : all_rows) {
				if (row.getChildCount() > 2) {
					TableColumn[] cols = row.getColumns();
					NodeList aList = cols[0].getChildren()
							.extractAllNodesThatMatch(new TagNameFilter("a"),
									true);
					String link = CreatePartialLink(TSConnectionURL.idPOST)
							+ ((LinkTag) aList.elementAt(0)).extractLink();

					LinkTag l = (LinkTag) (aList.elementAt(0));
					l.setLink(link);

					String rowHtml = row.toHtml();
					ParsedResponse currentResponse = new ParsedResponse();
					currentResponse.setAttribute(
							ParsedResponse.SERVER_ROW_RESPONSE, rowHtml);
					currentResponse.setOnlyResponse(rowHtml);
					currentResponse.setPageLink(new LinkInPage(link, link,
							TSServer.REQUEST_SAVE_TO_TSD));

					ResultMap resultMap = ro.cst.tsearch.servers.functions.TNRutherfordYC
							.parseIntermediaryRow(row, searchId);
					Bridge bridge = new Bridge(currentResponse, resultMap,
							searchId);
					resultMap.removeTempDef();

					DocumentI document = (TaxDocumentI) bridge.importData();
					currentResponse.setDocument(document);

					intermediaryResponse.add(currentResponse);
				}
			}
			if (typeOfSearch == ID_SEARCH_BY_NAME)
				response.getParsedResponse()
						.setHeader(
								"<table width=\"85%\" align=\"center\" cellspacing=\"0\" cellpadding=\"3\" border=\"1\" id=\"intermediary_results\" "
										+ "<tr>"
										+ "<th><center>Parcel ID</center></th>"
										+ "<th><center>Lot Number</center></th>"
										+ "<th><center>Property Address</center></th>"
										+ "<th><center>Name</center></th>"
										+ "</tr>");
			if (typeOfSearch == ID_SEARCH_BY_ADDRESS)
				response.getParsedResponse()
						.setHeader(
								"<table width=\"85%\" align=\"center\" cellspacing=\"0\" cellpadding=\"3\" border=\"1\" id=\"intermediary_results\" "
										+ "<tr>"
										+ "<th><center>Parcel ID</center></th>"
										+ "<th><center>Lot Number</center></th>"
										+ "<th><center>Property Address</center></th>"
										+ "</tr>");

			response.getParsedResponse().setFooter("</table>");

			outputTable.append(table);

		} catch (Exception e) {
			e.printStackTrace();
		}

		return intermediaryResponse;
	}

	@Override
	protected String clean(String response) {
		return response;
	}

	@Override
	protected String cleanDetails(String response) {
		String cleanResp = "";
		try {
			org.htmlparser.Parser htmlParser = org.htmlparser.Parser
					.createParser(response, null);
			NodeList nodeList = htmlParser.parse(null);

			// if from memory
			if (!response.contains("<html"))
				return response;

			// extract the result
			NodeList body = nodeList.extractAllNodesThatMatch(
					new TagNameFilter("body"), true);

			body.elementAt(0).getChildren().remove(1);

			String resp = body.toHtml();

			resp = resp.replaceFirst(
					"<table border=\"0\" width=\"100%\">.*</table>", "");

			resp = resp.substring(0, resp.indexOf("Search Complete."));

			// put table id's
			resp = resp
					.replaceAll(
							"(<br>\r\n.*APPRAISED VALUE:.*<br>\r\n)<table border=\"0\">",
							"$1<table id=\"APPRAISED VALUE\" border=\"0\">");

			resp = resp
					.replaceAll(
							"(<br>\r\n.*ASSESSED VALUE:.*<br>\r\n)<table border=\"0\">",
							"$1<table id=\"ASSESSED VALUE\" border=\"0\">");

			resp = resp
					.replaceAll(
							"(<br>\r\n.*PAYMENT HISTORY:.*<br>\r\n)<table border=\"0\">",
							"$1<table id=\"PAYMENT HISTORY\" border=\"0\">");

			resp = resp.replace(
					"<body class=\"BODY\" onContextMenu=\"return false\">",
					"<div id=\"id_results\">");
			resp = resp + "</div>";
			cleanResp = resp;
		} catch (Exception e) {
			e.printStackTrace();
		}

		response = cleanResp;
		return cleanResp;
	}

	@Override
	protected Object parseAndFillResultMap(ServerResponse response,
			String detailsHtml, ResultMap resultMap) {

		try {
			ro.cst.tsearch.servers.functions.TNRutherfordYC.putSearchType(
					resultMap, "YC");

			// detailsHtml = cleanDetails(detailsHtml);

			org.htmlparser.Parser htmlParser = org.htmlparser.Parser
					.createParser(detailsHtml, null);
			NodeList nodeList = htmlParser.parse(null);

			Div results = null;

			if (nodeList
					.extractAllNodesThatMatch(new TagNameFilter("div"), true)
					.extractAllNodesThatMatch(
							new HasAttributeFilter("id", "id_results"), true)
					.size() > 0)
				results = (Div) nodeList
						.extractAllNodesThatMatch(new TagNameFilter("div"),
								true)
						.extractAllNodesThatMatch(
								new HasAttributeFilter("id", "id_results"),
								true).elementAt(0);

			if (results != null) {
				// get owner
				Pattern OWNER_PAT = Pattern
						.compile("PROPERTY OWNER:<br> \r\n<b>(.*)</b><br>");
				Matcher matcher = OWNER_PAT.matcher(results.toHtml());
				if (matcher.find()) {
					String name = matcher.group(1).replaceAll("\\s+", " ")
							.trim();
					ro.cst.tsearch.servers.functions.TNRutherfordYC.parseNames(
							resultMap, name);
				}

				// get addr
				String addr = HtmlParser3
						.getValueFromAbsoluteCell(
								0,
								0,
								HtmlParser3.findNode(nodeList,
										"PROPERTY ADDRESS:"), "", true)
						.replaceAll("<[^>]*>", "")
						.replace("PROPERTY ADDRESS:", "")
						.replaceAll("\\s+", " ").trim();
				ro.cst.tsearch.servers.functions.TNRutherfordYC.parseAddress(
						resultMap, addr);

				// get parcel
				String parcel = HtmlParser3
						.getValueFromAbsoluteCell(
								0,
								0,
								HtmlParser3
										.findNode(nodeList, "PARCEL NUMBER:"),
								"", true).replaceAll("<[^>]*>", "")
						.replace("PARCEL NUMBER:", "").trim();

				resultMap.put(
						PropertyIdentificationSetKey.PARCEL_ID.getKeyName(),
						parcel.replace(" ", ""));

				// get lot
				String lot = HtmlParser3
						.getValueFromAbsoluteCell(0, 0,
								HtmlParser3.findNode(nodeList, "LOT NUMBER:"),
								"", true).replaceAll("<[^>]*>", "")
						.replace("LOT NUMBER:", "").trim();
				resultMap.put(
						PropertyIdentificationSetKey.SUBDIVISION_LOT_NUMBER
								.getKeyName(), lot);

				// get subdiv
				String subdiv = HtmlParser3
						.getValueFromAbsoluteCell(
								0,
								0,
								HtmlParser3.findNode(nodeList,
										"SUBDIVISION NAME:"), "", true)
						.replaceAll("<[^>]*>", "").replaceAll("\\s+", " ")
						.trim();
				// .replace("SUBDIVISION NAME:", "").trim();

				ro.cst.tsearch.servers.functions.TNRutherfordYC
						.parseLegalDescription(resultMap, subdiv);

				NodeList tax = new NodeList();

				tax.add(new NodeList(results).extractAllNodesThatMatch(
						new TagNameFilter("table"), true)
						.extractAllNodesThatMatch(new HasAttributeFilter("id")));

				ro.cst.tsearch.servers.functions.TNRutherfordYC.parseTaxes(
						resultMap, tax);
				
				String city = (String) resultMap.get(PropertyIdentificationSetKey.CITY.getKeyName());
				if(StringUtils.isEmpty(city)){
	    			city = getDataSite().getCityName();
	    		}
				if(StringUtils.isNotEmpty(city)) {
					resultMap.put(PropertyIdentificationSetKey.CITY.getKeyName(), city);
				}
				
			}

		} catch (Exception e) {
			e.printStackTrace();
		}

		return null;
	}

	@Override
	protected void setModulesForAutoSearch(TSServerInfo serverInfo) {
		List<TSServerInfoModule> moduleList = new ArrayList<TSServerInfoModule>();

		TSServerInfoModule module = null;

		String city = getSearchAttribute(SearchAttributes.P_CITY).toUpperCase();
		if (!StringUtils.isEmpty(city)) {
			if (!city.startsWith(getCityName())) {
				return;
			}
		}

		// address
		FilterResponse addressFilter = AddressFilterFactory
				.getAddressHybridFilter(searchId, 0.8d, true);

		if (hasStreet()) {
			FilterResponse nameFilterHybrid = NameFilterFactory
					.getHybridNameFilter(SearchAttributes.OWNER_OBJECT,
							searchId, module);

			module = new TSServerInfoModule(
					serverInfo.getModule(TSServerInfo.ADDRESS_MODULE_IDX));
			module.clearSaKeys();

			module.forceValue(0,
					getSearchAttribute(SearchAttributes.P_STREETNAME));

			if (hasStreetNo()) {
				module.forceValue(1,
						getSearchAttribute(SearchAttributes.P_STREETNO));
			}
			module.addFilter(addressFilter);
			module.addFilter(nameFilterHybrid);
			moduleList.add(module);

		}

		// owner
		if (hasOwner()) {
			FilterResponse nameFilterHybridDoNotSkipUnique = NameFilterFactory
					.getHybridNameFilter(SearchAttributes.OWNER_OBJECT,
							searchId, module);
			nameFilterHybridDoNotSkipUnique.setSkipUnique(false);

			module = new TSServerInfoModule(
					serverInfo.getModule(TSServerInfo.NAME_MODULE_IDX));
			module.clearSaKeys();
			module.forceValue(0,
					getSearchAttribute(SearchAttributes.OWNER_LNAME));
			module.forceValue(1,
					getSearchAttribute(SearchAttributes.OWNER_FNAME));
			module.addFilter(nameFilterHybridDoNotSkipUnique);
			module.addFilter(addressFilter);
			moduleList.add(module);
		}
		serverInfo.setModulesForAutoSearch(moduleList);

	}
}
