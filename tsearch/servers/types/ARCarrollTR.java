package ro.cst.tsearch.servers.types;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Vector;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang.StringUtils;
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
import ro.cst.tsearch.parser.HtmlParser3;
import ro.cst.tsearch.search.FunctionStatesIterator;
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
import ro.cst.tsearch.utils.InstanceManager;

import com.stewart.ats.base.document.DocumentI;
import com.stewart.ats.base.document.TaxDocumentI;

/**
 * 
 * @author Oprina George
 * 
 *         Mar 15, 2011
 */

public class ARCarrollTR extends TemplatedServer {

	private static final long serialVersionUID = 1L;

	private String book = "";

	public ARCarrollTR(long searchId) {
		super(searchId);
		setSuper();
	}

	public ARCarrollTR(String rsRequestSolverName, String rsSitePath,
			String rsServerID, String rsPrmNameLink, long searchId,
			int miServerID) {
		super(rsRequestSolverName, rsSitePath, rsServerID, rsPrmNameLink,
				searchId, miServerID);
		setSuper();
	}

	public void setSuper() {
		int[] intermediary_cases = new int[] { ID_SEARCH_BY_NAME };
		super.setIntermediaryCases(intermediary_cases);
		super.setDetailsMessage("Property Detail");
	}

	@Override
	protected void setMessages() {
		getErrorMessages().addNoResultsMessages("No Records Returned");
		// getErrorMessages().addServerErrorMessage("No Records Returned");
	}

	@Override
	protected void ParseResponse(String action, ServerResponse response,
			int viParseID) throws ServerResponseException {
		if (isError(response)) {
			response.setError("No data found for this criteria!");
			response.setResult("");
			return;
		}
		if (response.getResult().contains("Records Returned")
				|| viParseID == ID_SAVE_TO_TSD)
			super.ParseResponse(action, response, viParseID);
		else if (response.getResult().contains("Property Information"))
			super.ParseResponse(action, response, ID_DETAILS);

	}

	@Override	
	protected void saveCasesParse(ServerResponse response, String serverResult, String filename) {
		DocumentI docI = smartParseDetails(response, serverResult);
		if (docI != null) {
			//for delinquent test cases
//			if (rcpt_t == null && his_rcpt == null && StringUtils.isEmpty(baseAmt) && StringUtils.isEmpty(amtPaid)  && StringUtils.isEmpty(amtDue)) {
				if (serverResult.contains("This property has delinquent taxes") || serverResult.contains("For tax amount due, you must call the Collector's Office")) {
					String textForRemarks = "  This property has delinquent taxes! No Tax information available."; 
					docI.setNote(textForRemarks);
				}
//			}
			
		}
		msSaveToTSDFileName = filename;
		response.getParsedResponse().setFileName(getServerTypeDirectory() + msSaveToTSDFileName);
		response.getParsedResponse().setResponse(serverResult);

		msSaveToTSDResponce = serverResult + CreateFileAlreadyInTSD();
	}
	
	@Override
	protected HashMap<String, String> putAdditionalData(String serverResult) {
		HashMap<String, String> data = new HashMap<String, String>();
		NodeList nodeList;
		String year = "";
		try {
			nodeList = org.htmlparser.Parser.createParser(serverResult, null)
					.parse(null);
			year = HtmlParser3
					.getValueFromAbsoluteCell(0, 1,
							HtmlParser3.findNode(nodeList, "Tax Year/"), "",
							true).replaceAll("(\\d\\d\\d\\d)*.", "$1").trim();
		} catch (ParserException e) {
			e.printStackTrace();
		}

		data.put("type", "CNTYTAX");
		data.put("year", year);
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
					.getValueFromAbsoluteCell(0, 1,
							HtmlParser3.findNode(nodeList, "Parcel #:"), "",
							true).replaceAll("<[^>]*>", "").trim();
			return account;
		} catch (ParserException e) {
			e.printStackTrace();
		}
		return "";
	}

	@Override
	public Collection<ParsedResponse> smartParseIntermediary(
			ServerResponse response, String table, StringBuilder outputTable) {
		Vector<ParsedResponse> intermediaryResponse = new Vector<ParsedResponse>();

		try {
			org.htmlparser.Parser htmlParser = org.htmlparser.Parser
					.createParser(table, null);
			NodeList nodeList = htmlParser.parse(null);
			NodeList mainTableList = nodeList
					.extractAllNodesThatMatch(new TagNameFilter("table"), true)
					.extractAllNodesThatMatch(
							new HasAttributeFilter("width", "100%"), true)
					.extractAllNodesThatMatch(
							new HasAttributeFilter("cellpadding", "3"), true)
					.extractAllNodesThatMatch(
							new HasAttributeFilter("bgcolor", "#aaaaaa"), true);

			if (mainTableList.size() == 0) {
				return intermediaryResponse;
			}

			// parse rows
			TableTag tableTag = (TableTag) mainTableList.elementAt(0);

			TableRow[] rows = tableTag.getRows();

			for (int i = 1; i < rows.length; i++) {
				TableRow row = rows[i];
				if (row.getColumnCount() == 7) {

					LinkTag linkTag = ((LinkTag) row.getColumns()[0]
							.getChild(0));

					String link = CreatePartialLink(TSConnectionURL.idGET)
							+ "http://www.arcountydata.com/"
							+ linkTag.extractLink().trim()
									.replaceAll("\\s", "%20");

					linkTag.setLink(link);

					row.removeAttribute("class");
					row.removeAttribute("onmouseover");
					row.removeAttribute("onMouseOut");
					row.removeAttribute("onClick");
					row.setAttribute("width", "85%");
					row.setAttribute(
							"style",
							"background-color:#EFEFEF;border-color:Black;border-width:1px;border-style:solid;");

					ParsedResponse currentResponse = new ParsedResponse();
					currentResponse.setAttribute(
							ParsedResponse.SERVER_ROW_RESPONSE, row.toHtml());
					currentResponse.setOnlyResponse(row.toHtml());
					currentResponse.setPageLink(new LinkInPage(link, link,
							TSServer.REQUEST_SAVE_TO_TSD));

					ResultMap m = ro.cst.tsearch.servers.functions.ARCarrollTR
							.parseIntermediaryRow(row, searchId);
					Bridge bridge = new Bridge(currentResponse, m, searchId);

					DocumentI document = (TaxDocumentI) bridge.importData();
					currentResponse.setDocument(document);

					intermediaryResponse.add(currentResponse);
				}
			}
			response.getParsedResponse()
					.setHeader(
							"<table width=\"75%\" align=\"center\" cellspacing=\"0\" cellpadding=\"3\" rules=\"cols\" border=\"1\" id=\"intermediary\" style=\"background-color:#EFEFEF;border-color:Black;font-size:X-Small;\">\n"
									+ "<tr style=\"color:#636466;background-color:#DBDBDB;font-weight:bold;\">"
									+ "<th scope=\"col\">Parcel #:</th>"
									+ "<th scope=\"col\">Year</th>"
									+ "<th scope=\"col\">Owner</th>"
									+ "<th scope=\"col\">Book</th>"
									+ "<th scope=\"col\">Site Address</th>"
									+ "<th scope=\"col\">Location</th>"
									+ "<th scope=\"col\">Taxpayer Name</th>"
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
			NodeList tables = nodeList
					.extractAllNodesThatMatch(new TagNameFilter("table"), true)
					.extractAllNodesThatMatch(
							new HasAttributeFilter("cellspacing", "0"), true)
					.extractAllNodesThatMatch(
							new HasAttributeFilter("cellpadding", "3"), true)
					.extractAllNodesThatMatch(
							new HasAttributeFilter("border", "0"), true);

			TableTag table = null;
			for (int i = 0; i < tables.size(); i++) {
				if (tables.elementAt(i).toHtml()
						.contains("Back to Search Results")
						&& tables.elementAt(i).toHtml()
								.contains("Property Information")) {
					table = (TableTag) tables.elementAt(i);
					break;
				}
			}

			if (table != null && table.getRowCount() > 0) {
				TableRow r = table.getRow(0);
				TableColumn[] cols = r.getColumns();

				TableColumn col = null;
				if (cols.length == 2) {
					NodeList children = cols[1].getChildren();
					for (int i = 0; i < children.size(); i++) {
						if (children.elementAt(i) instanceof LinkTag)
							children.remove(children.elementAt(i));
					}
					col = cols[1];
					col.setChildren(children);
				}
				r.setChildren(new NodeList(col));
				table.setChildren(new NodeList(r));
				table.setAttribute("align", "center");
				table.setAttribute("id", "final_results");
				cleanResp = table.toHtml();
			}
			if (cleanResp.equals("")) {
				cleanResp = "No Results Found";
			}
		} catch (Exception e) {
			e.printStackTrace();
		}

		// clean all Links
		cleanResp = cleanResp.replaceAll("<a [^>]*>([\\d]*)</a>", "$1");

		cleanResp = cleanResp.replaceAll(
				"<a [^>]*>(<strong>)*[^<]*(</strong>)*[^<]*</a>", "");

		// clean all .gif
		cleanResp = cleanResp.replaceAll("<img [^>]*>", "");

		// set colors
		cleanResp = cleanResp.replace("bgcolor=\"#313461\"",
				"bgcolor=\"#ffffff\"");

		cleanResp = cleanResp
				.replace("class=\"Row_On\"", "bgcolor=\"#ffffff\"");

		// clean onclick & on mouse over & out
		cleanResp = cleanResp.replaceAll("onclick=\"[^\"]*\"", "");
		cleanResp = cleanResp.replaceAll("onmouseover=\"[^\"]*\"", "");
		cleanResp = cleanResp.replaceAll("onmouseout=\"[^\"]*\"", "");

		return cleanResp;
	}

	@Override
	protected String createCustomDescriptionPart() {
		return book;
	}

	@Override
	protected Object parseAndFillResultMap(ServerResponse response,
			String detailsHtml, ResultMap resultMap) {

		try {
			org.htmlparser.Parser htmlParser = org.htmlparser.Parser
					.createParser(response.getResult(), null);
			NodeList nodeList = htmlParser.parse(null);
			NodeList mainTableList = nodeList.extractAllNodesThatMatch(new TagNameFilter("table"), true)
					.extractAllNodesThatMatch(new HasAttributeFilter("id", "final_results"), true);

			// get parcel
			String parcel = HtmlParser3.getValueFromAbsoluteCell(0, 1, HtmlParser3.findNode(mainTableList, "Parcel #:"), "", true)
					.replaceAll("<[^>]*>([^<]*)<[^>]*>", "$1").trim();

			resultMap.put(PropertyIdentificationSetKey.PARCEL_ID.getKeyName(), 	parcel.trim());
			resultMap.put(PropertyIdentificationSetKey.PARCEL_ID_PARCEL.getKeyName(),  parcel.replace("-", "").trim());

			// get year
			String year = HtmlParser3.getValueFromAbsoluteCell(0, 1, HtmlParser3.findNode(mainTableList, "Tax Year/"), "", true);

			// parse book
			if (year.split("(?ism)<BR>").length > 1) {
				String book = year.split("(?ism)<BR>")[1];
				if (book.contains("Delinquent"))
					book = "Delinquent";
				if (book.equals("Delinquent"))
					this.book = book.toUpperCase();
			}

			year = year.replaceAll("(\\d\\d\\d\\d)*.", "$1").trim();

			resultMap.put(TaxHistorySetKey.YEAR.getKeyName(), year);

			// get prop type
			String prop_type = HtmlParser3.getValueFromAbsoluteCell(0, 1, HtmlParser3.findNode(mainTableList, "Property Type:"), "", true)
					.trim().toUpperCase();
			resultMap.put(PropertyIdentificationSetKey.PROPERTY_TYPE.getKeyName(), prop_type);

			// get owner
			String owner = HtmlParser3.getValueFromAbsoluteCell(0, 1, HtmlParser3.findNode(mainTableList, "Owner:"), "", true)
					.trim().toUpperCase();
			ro.cst.tsearch.servers.functions.ARCarrollTR.parseNames(resultMap, owner);

			// get address
			String address = HtmlParser3.getValueFromAbsoluteCell(0, 1, HtmlParser3.findNode(mainTableList, "Site Address:"),"", true)
					.trim().toUpperCase();
			ro.cst.tsearch.servers.functions.ARCarrollTR.parseAddress(resultMap, address);

			// get subdiv
			String subdiv = HtmlParser3.getValueFromAbsoluteCell(0, 1, HtmlParser3.findNode(mainTableList, "Subdivision:"),	"", true)
					.trim().toUpperCase();
			String unit = "";
			Pattern LEGAL_UNIT = Pattern.compile("UNIT:*\\s+([^\\s]*)");
			Matcher matcher = LEGAL_UNIT.matcher(subdiv + "");
			if (matcher.find()) {
				unit = matcher.group(1);
				subdiv = StringUtils.strip(subdiv.replace(matcher.group(), ""));
				unit = unit.replaceAll("\\s+", " ").trim();
			}

			resultMap.put(PropertyIdentificationSetKey.SUBDIVISION_UNIT.getKeyName(), unit);
			resultMap.put(PropertyIdentificationSetKey.SUBDIVISION_NAME.getKeyName(), subdiv);

			// get lot block
			String lot_block = HtmlParser3.getValueFromAbsoluteCell(0, 1, HtmlParser3.findNode(mainTableList, "Lot Block:"), "", true)
					.trim().toUpperCase();
			String lot = "";
			String block = "";

			if (lot_block.split("&NBSP;").length > 0)
				lot = lot_block.split("&NBSP;")[0];
			if (lot_block.split("&NBSP;").length > 1)
				block = StringUtils.strip(lot_block.split("&NBSP;")[1]);

			resultMap.put(PropertyIdentificationSetKey.SUBDIVISION_LOT_NUMBER.getKeyName(), lot);
			resultMap.put(PropertyIdentificationSetKey.SUBDIVISION_BLOCK.getKeyName(), block);

			// get S-T-R
			String str = HtmlParser3.getValueFromAbsoluteCell(0, 1,	HtmlParser3.findNode(mainTableList, "S-T-R:"), "", true)
					.trim().toUpperCase();
			ro.cst.tsearch.servers.functions.ARCarrollTR.parseSTR(resultMap, str);

			// get legal
			String legal = HtmlParser3.getValueFromAbsoluteCell(0, 1, HtmlParser3.findNode(mainTableList, "Legal:"), "", true)
					.trim().toUpperCase();

			// parse legal
			ro.cst.tsearch.servers.functions.ARCarrollTR.parseLegalDescription(resultMap, legal);

			// parse taxes
			ro.cst.tsearch.servers.functions.ARCarrollTR.parseTaxes(resultMap, mainTableList);

		} catch (Exception e) {
			e.printStackTrace();
		}

		return null;
	}

	@Override
	protected void setModulesForAutoSearch(TSServerInfo serverInfo) {
		List<TSServerInfoModule> moduleList = new ArrayList<TSServerInfoModule>();

		TSServerInfoModule module = null;

		TaxYearFilterResponse frYr = new TaxYearFilterResponse(searchId);

		// pin
		if (hasPin()) {
			module = new TSServerInfoModule(
					serverInfo.getModule(TSServerInfo.GENERIC_MODULE_IDX));
			module.clearSaKeys();

			String parcelno = getSearchAttribute(SearchAttributes.LD_PARCELNO);

			module.forceValue(6, parcelno);
			module.addFilter(frYr);
			moduleList.add(module);
		}

		// address
		FilterResponse addressFilter = AddressFilterFactory
				.getAddressHybridFilter(searchId, 0.8d, true);

		// owner
		if (hasOwner()) {
			FilterResponse nameFilterHybridDoNotSkipUnique = NameFilterFactory
					.getHybridNameFilter(SearchAttributes.OWNER_OBJECT,
							searchId, module);
			nameFilterHybridDoNotSkipUnique.setSkipUnique(false);

			module = new TSServerInfoModule(
					serverInfo.getModule(TSServerInfo.GENERIC_MODULE_IDX));
			module.clearSaKeys();
			module.setSaObjKey(SearchAttributes.OWNER_OBJECT);
			module.setIteratorType(0,
					FunctionStatesIterator.ITERATOR_TYPE_LF_NAME_FAKE);
			module.addFilter(nameFilterHybridDoNotSkipUnique);
			module.addFilter(addressFilter);
			module.addFilter(frYr);
			module.addIterator((ConfigurableNameIterator) ModuleStatesIteratorFactory
					.getConfigurableNameIterator(module, searchId,
							new String[] { "L;F;", "L;F;M" }));
			moduleList.add(module);
		}
		serverInfo.setModulesForAutoSearch(moduleList);
	}

}
