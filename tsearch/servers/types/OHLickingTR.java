package ro.cst.tsearch.servers.types;

import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Vector;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang.StringUtils;
import org.htmlparser.Node;
import org.htmlparser.filters.HasAttributeFilter;
import org.htmlparser.filters.RegexFilter;
import org.htmlparser.filters.TagNameFilter;
import org.htmlparser.tags.TableColumn;
import org.htmlparser.tags.TableRow;
import org.htmlparser.tags.TableTag;
import org.htmlparser.tags.LinkTag;
import org.htmlparser.util.NodeList;

import com.stewart.ats.base.document.DocumentI;
import com.stewart.ats.base.document.TaxDocumentI;

import ro.cst.tsearch.bean.SearchAttributes;
import ro.cst.tsearch.exceptions.ServerResponseException;
import ro.cst.tsearch.extractor.xml.Bridge;
import ro.cst.tsearch.extractor.xml.ResultMap;
import ro.cst.tsearch.extractor.xml.ResultTable;
import ro.cst.tsearch.search.FunctionStatesIterator;
import ro.cst.tsearch.search.filter.FilterResponse;
import ro.cst.tsearch.search.filter.RejectNonRealEstate;
import ro.cst.tsearch.search.filter.newfilters.address.AddressFilterFactory;
import ro.cst.tsearch.search.filter.newfilters.address.CityFilterFactory;
import ro.cst.tsearch.search.filter.newfilters.name.NameFilterFactory;
import ro.cst.tsearch.search.filter.newfilters.pin.PinFilterResponse;
import ro.cst.tsearch.search.iterator.ModuleStatesIteratorFactory;
import ro.cst.tsearch.search.module.ConfigurableNameIterator;
import ro.cst.tsearch.servers.TSConnectionURL;
import ro.cst.tsearch.servers.info.TSServerInfo;
import ro.cst.tsearch.servers.info.TSServerInfoModule;
import ro.cst.tsearch.servers.response.LinkInPage;
import ro.cst.tsearch.servers.response.ParsedResponse;
import ro.cst.tsearch.servers.response.ServerResponse;
import ro.cst.tsearch.servers.response.OtherInformationSet.OtherInformationSetKey;
import ro.cst.tsearch.servers.response.PropertyAppraisalSet.PropertyAppraisalSetKey;
import ro.cst.tsearch.servers.response.PropertyIdentificationSet.PropertyIdentificationSetKey;
import ro.cst.tsearch.servers.response.SaleDataSet.SaleDataSetKey;
import ro.cst.tsearch.servers.response.TaxHistorySet.TaxHistorySetKey;
import ro.cst.tsearch.servers.response.TaxInstallmentSet.TaxInstallmentSetKey;
import ro.cst.tsearch.utils.InstanceManager;
import ro.cst.tsearch.utils.Tidy;
import ro.cst.tsearch.parser.HtmlParser3;

public class OHLickingTR extends TSServer {
	private static final long	serialVersionUID	= 1L;

	public OHLickingTR(long searchId) {
		super(searchId);
	}

	public OHLickingTR(String rsRequestSolverName, String rsSitePath,
			String rsServerID, String rsPrmNameLink, long searchId,
			int miServerID) {
		super(rsRequestSolverName, rsSitePath, rsServerID, rsPrmNameLink,
				searchId, miServerID);
	}

	@Override
	protected void ParseResponse(String sAction, ServerResponse Response, int viParseID) throws ServerResponseException {
		String rsResponse = Response.getResult();
		ParsedResponse parsedResponse = Response.getParsedResponse();

		if (rsResponse.contains("No Records Match Search Criteria")) {
			Response.getParsedResponse().setError("No results found for your query! Please change your search criteria and try again.");
			return;
		}
		if (rsResponse.contains("Parcel Number Not Formatted Correctly.")) {
			Response.getParsedResponse().setError("Parcel Number Not Formatted Correctly. Please Correct It And Try Again.");
			return;
		}
		String activeNextYearMessage = "The Parcel You Have Selected Is Not Active Till The Next Tax Year.";
		if (rsResponse.contains(activeNextYearMessage)) {
			Response.getParsedResponse().setError(activeNextYearMessage);
			return;
		}

		switch (viParseID)
		{
		case ID_SEARCH_BY_NAME:
		case ID_SEARCH_BY_ADDRESS:
			// search by address or owner can bring details page directly if only one result is found
			if (rsResponse.contains("Parcel Summary Information")) {
				ParseResponse(sAction, Response, ID_DETAILS);
			}
			// no result
			StringBuilder outputTable = new StringBuilder();
			Collection<ParsedResponse> smartParsedResponses = smartParseIntermediary(Response, rsResponse, outputTable);
			if (smartParsedResponses.size() == 0) {
				return;
			}

			StringBuilder footer = new StringBuilder();
			parsedResponse.setFooter("<tr><td colspan='6' align='center'>" + footer.toString() + "</td></tr></table>");

			parsedResponse.setResultRows(new Vector<ParsedResponse>(smartParsedResponses));
			parsedResponse.setOnlyResponse(outputTable.toString());
			parsedResponse.setAttribute(ParsedResponse.SERVER_ROW_RESPONSE, outputTable.toString());
			break;

		case ID_GET_LINK:
			ParseResponse(sAction, Response, ID_DETAILS);
			
			break;

		case ID_SEARCH_BY_PARCEL:
		case ID_DETAILS:
		case ID_SAVE_TO_TSD:

			StringBuilder accountId = new StringBuilder();
			String details = getDetails(rsResponse, accountId);

			if (viParseID == ID_DETAILS || viParseID == ID_SEARCH_BY_PARCEL) {
				String originalLink = sAction.replace("?", "&") + "&" + Response.getRawQuerry();
				String sSave2TSDLink = getLinkPrefix(TSConnectionURL.idGET) + originalLink;

				HashMap<String, String> data = new HashMap<String, String>();
				data.put("type", "CNTYTAX");
				if (isInstrumentSaved(accountId.toString().trim(), null, data)) {
					details += CreateFileAlreadyInTSD();
				}
				else {
					mSearch.addInMemoryDoc(sSave2TSDLink, details);
					details = addSaveToTsdButton(details, sSave2TSDLink, ID_DETAILS);
				}

				parsedResponse.setPageLink(new LinkInPage(sSave2TSDLink, originalLink, TSServer.REQUEST_SAVE_TO_TSD));
				parsedResponse.setResponse(details);
			} else {
				String filename = accountId + ".html";
				smartParseDetails(Response, details);

				msSaveToTSDFileName = filename;
				parsedResponse.setFileName(getServerTypeDirectory() + msSaveToTSDFileName);
				parsedResponse.setResponse(details);

				msSaveToTSDResponce = details + CreateFileAlreadyInTSD();
			}
			break;
		}
	}

	private String getDetails(String rsResponse, StringBuilder accountId) {
		HtmlParser3 htmlParser3 = new HtmlParser3(rsResponse);
		NodeList nodes = htmlParser3.getNodeList();

		/* If from memory - use it as is */
		if (!StringUtils.containsIgnoreCase(rsResponse, "<html"))
		{
			if (nodes.size() > 0)
			{
				String id = htmlParser3.getNodeById("ctl00_lblParcelNumberRO").toPlainTextString().trim();
				if (!id.isEmpty()) {
					accountId.append(id);
				}
			}
			else
			{
				return null;
			}
			return rsResponse;
		}

		try {
			if (nodes.size() > 0)
			{
				String id = htmlParser3.getNodeById("ctl00_lblParcelNumberRO").toPlainTextString().trim();
				if (!id.isEmpty()) {
					accountId.append(id);
				}
			}
			StringBuilder details = new StringBuilder();

			Node parcelOwnerLocation = htmlParser3.getNodeById("ctl00_lblParcelNumberRO");
			if (parcelOwnerLocation != null) {
				parcelOwnerLocation = parcelOwnerLocation.getParent().getParent().getParent();
				details.append("<table id=\"finalResults\" width=\"800\" align=\"center\" border=\"2\">");
				details.append("<tr id=\"parcelOwnerInformation\"><td>" + parcelOwnerLocation.toHtml() + "</td></tr>");
			}
			NodeList summaryTable = nodes.extractAllNodesThatMatch(new RegexFilter("Parcel Summary Information For"), true);
			if (summaryTable.size() > 0) {
				TableTag summaryTableTag = (TableTag) summaryTable.elementAt(0).getParent().getParent().getParent();
				details.append("<tr id=\"parcelInfoSummary\"><td>" + summaryTableTag.toHtml() + "</td></tr>");
			}

			String currentYearTaxTables = getTaxTables(htmlParser3, "ctl00_Contentplaceholder1_btnMmTaxes");
			if (!currentYearTaxTables.isEmpty()) {
				details.append("<tr id=\"currentYearTaxTablesRow\"><td>" + currentYearTaxTables + "</td></tr>");
			}

			String previousYearTaxTables = getTaxTables(htmlParser3, "ctl00_Contentplaceholder1_btnMmTaxesPrevious");
			if (!previousYearTaxTables.isEmpty()) {
				details.append("<tr id=\"previousYearTaxTables\"><td>" + previousYearTaxTables + "</td></tr>");
			}

			// Residential
			Node residentialLink = htmlParser3.getNodeById("ctl00_Contentplaceholder1_btnMmResidentialLand");
			if (residentialLink != null)
			{
				LinkTag residentialLinkTag = (LinkTag) residentialLink;

				String residentialTableContent = getLinkContents(dataSite.getLink() + residentialLinkTag.getLink());
				htmlParser3 = new HtmlParser3(residentialTableContent);
				Node residentialTableChild = htmlParser3.getNodeById("ctl00_cntMain_lblYearBuiltRO");
				if (residentialTableChild != null) {
					Node residentialTable = residentialTableChild.getParent().getParent().getParent();
					if (residentialTable != null) {
						TableTag residentialTTag = (TableTag) residentialTable;
						residentialTTag.removeAttribute("rules");
						residentialTTag.removeAttribute("cellspacing");
						residentialTTag.removeAttribute("style");
						residentialTTag.setAttribute("width", "100%");
						residentialTTag.setAttribute("border", "1");
						// header
						String residentialHeader = "Residential";
						NodeList residentialNodeList = htmlParser3.getNodeList();
						Node residentialHeaderNode = residentialNodeList.extractAllNodesThatMatch(new RegexFilter("(?is)Residential\\s+Information"), true)
								.elementAt(0);
						if (residentialHeaderNode != null) {
							residentialHeader = residentialHeaderNode.toPlainTextString();
						}
						details.append("<tr align = \"left\"><td><h3>" + residentialHeader + "</h3></td></tr>");

						details.append("<tr id=\"residential Table\"><td>" + residentialTTag.toHtml() + "</td></tr>");
					}
				}
			}

			// CAUV improvements table 
			Node cauvLink = htmlParser3.getNodeById("ctl00_Contentplaceholder1_btnMmValues");
			if (cauvLink != null)
			{
				LinkTag cauvLinkTag = (LinkTag) cauvLink;

				String cauvTableContent = getLinkContents(dataSite.getLink() + cauvLinkTag.getLink());
				htmlParser3 = new HtmlParser3(cauvTableContent);
				Node ratesNode = htmlParser3.getNodeById("ctl00_cntMain_lblRatesValue");
				details.append("<tr align = \"left\"><td><h3>Assessment Values</h3></td></tr>");
				if (ratesNode != null) {
					Node prevCellRatesNode = ratesNode.getParent().getParent().getFirstChild();
					details.append("<tr><td><table><tr>");
					if (prevCellRatesNode != null) {
						details.append("<td><h4>" + prevCellRatesNode.toPlainTextString() + "</h4></td>");
					}
					details.append("<td><h4>" + ratesNode.toPlainTextString() + "</h4></td></tr></table></td></tr>");
				}
				Node cauvTable = htmlParser3.getNodeById("ctl00_cntMain_gdvCAUV");
				if (cauvTable != null) {
					TableTag cauvTTag = (TableTag) cauvTable;
					cauvTTag.removeAttribute("rules");
					cauvTTag.removeAttribute("cellspacing");
					cauvTTag.removeAttribute("style");
					cauvTTag.setAttribute("width", "100%");

					details.append("<tr><td><table width = \"100%\" align=\"center\"><tr><th width=\"80\"></th><th width = \"240\" align=\"center\">CAUV</th>"
							+ "<th width = \"240\" align=\"center\">IMPROVEMENTS</hd><th width = \"250\">TOTAL</th></tr></table></td></tr>");
					details.append("<tr id=\"cauvTable\"><td>" + cauvTTag.toHtml() + "</td></tr>");
				}
				// MHValues
				Node MHValuesTable = htmlParser3.getNodeById("ctl00_cntMain_pnlMH");

				if (MHValuesTable != null) {

					TableTag MHValuesTTag = (TableTag) MHValuesTable.getFirstChild().getNextSibling();
					MHValuesTTag.removeAttribute("rules");
					MHValuesTTag.removeAttribute("cellspacing");
					MHValuesTTag.removeAttribute("style");
					MHValuesTTag.setAttribute("width", "100%");

					details.append("<tr id=\"MHValuesTTag\"><td>" + MHValuesTTag.toHtml() + "</td></tr>");
				}
			}
			// land data
			Node landLink = htmlParser3.getNodeById("ctl00_Contentplaceholder1_btnMmValues");
			if (landLink != null)
			{
				details.append("<tr><td>&nbsp;</td></tr>");
				if (cauvLink == null) {
					details.append("<tr align = \"left\"><td><h3>Assessment Values</h3></td></tr>");
				}
				LinkTag landLinkTag = (LinkTag) landLink;

				String landTableContent = getLinkContents(dataSite.getLink() + landLinkTag.getLink());
				htmlParser3 = new HtmlParser3(landTableContent);
				Node landTable = htmlParser3.getNodeById("ctl00_cntMain_gdvResults");
				if (landTable != null) {
					TableTag landTTag = (TableTag) landTable;
					landTTag.removeAttribute("rules");
					landTTag.removeAttribute("cellspacing");
					landTTag.removeAttribute("style");
					landTTag.setAttribute("width", "100%");
					landTTag.setAttribute("id", "landTable");
					details.append("<tr><td><table width = \"100%\" align=\"center\"><tr><th width=\"80\"></th><th width = \"240\" align=\"center\">LAND</th>"
							+ "<th width = \"240\" align=\"center\">IMPROVEMENTS</hd><th width = \"250\">TOTAL</th></tr></table></td></tr>");
					details.append("<tr id=\"landTable\"><td>" + landTTag.toHtml() + "</td></tr>");
				}
			}

			// transfers
			Node transfersLink = htmlParser3.getNodeById("ctl00_Contentplaceholder1_btnMmSalesTransfers");
			if (transfersLink != null)
			{
				LinkTag transfersLinkTag = (LinkTag) transfersLink;

				String transfersTableContent = getLinkContents(dataSite.getLink() + transfersLinkTag.getLink());
				htmlParser3 = new HtmlParser3(transfersTableContent);
				Node transfersTable = htmlParser3.getNodeById("ctl00_cntMain_gdvResults");
				if (transfersTable != null) {
					TableTag transfersTTag = (TableTag) transfersTable;
					transfersTTag.removeAttribute("rules");
					transfersTTag.removeAttribute("cellspacing");
					transfersTTag.removeAttribute("style");
					transfersTTag.setAttribute("width", "100%");
					transfersTTag.setAttribute("id", "transfersTable");
					details.append("<tr align = \"left\"><td><h3>Transfers</h3></td></tr>");

					details.append("<tr><td>" + transfersTTag.toHtml() + "</td></tr>");
				}
			}

			details.append("</table>");
			return details
					.toString()
					.replaceAll("(?ism)<a href[^>]*>([^<]*)</a>", "$1")
					.replaceAll("(?ism)<textarea[^>]*>([^<]*)</textarea>", "$1")
					.replaceAll("<img[^>]*>", "")
					.replaceAll("background-color\\s*:[^;]*;", "");

		} catch (Exception e) {
			logger.error("Error while getting details", e);
		}
		return null;
	}

	@Override
	public Collection<ParsedResponse> smartParseIntermediary(ServerResponse response, String page, StringBuilder outputTable) {

		Vector<ParsedResponse> intermediaryResponse = new Vector<ParsedResponse>();
		ParsedResponse parsedResponse = response.getParsedResponse();

		try {
			HtmlParser3 htmlParser = new HtmlParser3(page);

			TableTag interTable = (TableTag) htmlParser.getNodeById("ctl00_cntMain_gdvResults");
			if (interTable == null) {
				return intermediaryResponse;
			}
			TableRow[] rows = interTable.getRows();

			String url = dataSite.getLink();

			for (int i = 0; i < rows.length; i++) {
				TableRow row = rows[i];
				String rowText = row.toPlainTextString();

				if (rowText.contains("Parcel Number")) {
					// table header - perform some cleaning
					parsedResponse.setHeader("<table style='border-collapse: collapse' border='2' width='80%' align='center'>" +
							row.toHtml().replaceAll("(?is)<th[^>]*>", "<th>"));
					continue;
				}
				// process links
				LinkTag linkTag = (LinkTag) row.getColumns()[0].getChild(1);
				String link = linkTag.extractLink();
				String newLink = CreatePartialLink(TSConnectionURL.idGET) + url + link;

				linkTag.setLink(newLink);

				ParsedResponse currentResponse = new ParsedResponse();
				currentResponse.setPageLink(new LinkInPage(newLink, newLink, TSServer.REQUEST_SAVE_TO_TSD));
				currentResponse.setAttribute(ParsedResponse.SERVER_ROW_RESPONSE, row.toHtml());
				currentResponse.setOnlyResponse(row.toHtml());

				ResultMap m = ro.cst.tsearch.servers.functions.OHLickingTR.parseIntermediaryRow(row);
				Bridge bridge = new Bridge(currentResponse, m, searchId);
				DocumentI document = (TaxDocumentI) bridge.importData();
				currentResponse.setDocument(document);
				intermediaryResponse.add(currentResponse);
			}
			parsedResponse.setFooter("</table>");
			outputTable.append(interTable.toHtml());
		} catch (Exception e) {
			logger.error("Error while parsing intermediary results", e);
		}
		return intermediaryResponse;
	}

	@Override
	protected void setModulesForAutoSearch(TSServerInfo serverInfo) {
		List<TSServerInfoModule> modules = new ArrayList<TSServerInfoModule>();
		TSServerInfoModule module;

		FilterResponse addressFilter = AddressFilterFactory.getAddressHighPassFilter(searchId, 0.66d);
		FilterResponse nameFilterHybrid = NameFilterFactory.getHybridNameFilter(SearchAttributes.OWNER_OBJECT, searchId, null);
		
		FilterResponse rejectNonRealEstateFilter = new RejectNonRealEstate(SearchAttributes.OWNER_OBJECT, searchId);
		boolean hasCity = !StringUtils.isEmpty(getSearchAttribute(SearchAttributes.P_CITY));
		FilterResponse cityFilter= CityFilterFactory.getCityFilter(searchId, 0.6d);
		rejectNonRealEstateFilter.setThreshold(new BigDecimal("0.65"));
		
		PinFilterResponse pinFilter = new PinFilterResponse(SearchAttributes.LD_PARCELNO, searchId);
		pinFilter.setStartWith(true);
		pinFilter.setIgNoreZeroes(false);

		// search by parcel ID
		if (hasPin()) {
			module = new TSServerInfoModule(serverInfo.getModule(TSServerInfo.PARCEL_ID_MODULE_IDX));
			module.addFilter(pinFilter);
			module.addFilter(rejectNonRealEstateFilter);
			modules.add(module);
		}

		// search by address
		if (hasStreet()) {
			module = new TSServerInfoModule(serverInfo.getModule(TSServerInfo.ADDRESS_MODULE_IDX));
			module.addFilter(rejectNonRealEstateFilter);
			module.addFilter(addressFilter);
			if (hasCity) {
				module.addFilter(cityFilter);
			}
			modules.add(module);
		}

		// search by owner
		if (hasOwner()) {
			module = new TSServerInfoModule(serverInfo.getModule(TSServerInfo.NAME_MODULE_IDX));
			module.clearSaKeys();
			module.setSaObjKey(SearchAttributes.OWNER_OBJECT);
			module.addFilter(rejectNonRealEstateFilter);
			module.addFilter(nameFilterHybrid);
			if (hasCity) {
				module.addFilter(cityFilter);
			}
			if (hasStreet()) {
				module.addFilter(addressFilter);
			}
			
			module.setIteratorType(0, FunctionStatesIterator.ITERATOR_TYPE_LF_NAME_FAKE);
			ConfigurableNameIterator nameIterator = (ConfigurableNameIterator) ModuleStatesIteratorFactory
					.getConfigurableNameIterator(module, searchId, new String[] { "L;F;" });
			module.addIterator(nameIterator);
			modules.add(module);
		}
		serverInfo.setModulesForAutoSearch(modules);
	}

	private String getSpecialTaxes(Map<String, String> taxesMap, String pattern) {
		StringBuilder specials = new StringBuilder();
		specials
				.append("<tr><th align = \"left\" width = \"40%\">" + taxesMap.get("varSpecialTitle").replaceAll(pattern, "") + "</th>"
						+ "<th align = \"left\" width = \"15%\">Prior Years</th>"
						+ "<th align = \"left\" width = \"15%\">1st Half</th>"
						+ "<th align = \"left\" width = \"15%\">2nd Half</th>"
						+ "<th align = \"left\" width = \"15%\">Total</th></tr>");

		specials.append("<tr><td>Tax</td><td>" + taxesMap.get("varSpecialPrior").replaceAll(pattern, "") + "</td><td>"
				+ taxesMap.get("varSpecial1st").replaceAll(pattern, "") + "</td><td>"
				+ taxesMap.get("varSpecial2nd").replaceAll(pattern, "") + "</td><td>"
				+ taxesMap.get("varSpecialTotal").replaceAll(pattern, "")
				+ "</td></tr>");
		specials.append("<tr><td>Fee</td><td>" + taxesMap.get("varFeeSpecialPrior").replaceAll(pattern, "") + "</td><td>"
				+ taxesMap.get("varFeeSpecial1st").replaceAll(pattern, "") + "</td><td>"
				+ taxesMap.get("varFeeSpecial2nd").replaceAll(pattern, "") + "</td><td>"
				+ taxesMap.get("varFeeSpecialTotal").replaceAll(pattern, "")
				+ "</td></tr>");
		specials.append("<tr><td>Penalty</td><td>" + taxesMap.get("varPenSpecialPrior").replaceAll(pattern, "") + "</td><td>"
				+ taxesMap.get("varPenSpecial1st").replaceAll(pattern, "") + "</td><td>"
				+ taxesMap.get("varPenSpecial2nd").replaceAll(pattern, "") + "</td><td>"
				+ taxesMap.get("varPenSpecialTotal").replaceAll(pattern, "") + "</td></tr>");

		specials.append("<tr><td>Fee Penalty</td><td>" + taxesMap.get("varFeePenSpecialPrior").replaceAll(pattern, "") + "</td><td>"
				+ taxesMap.get("varFeePenSpecial1st").replaceAll(pattern, "") + "</td><td>"
				+ taxesMap.get("varFeePenSpecial2nd").replaceAll(pattern, "") + "</td><td>"
				+ taxesMap.get("varFeePenSpecialTotal").replaceAll(pattern, "") + "</td></tr>");

		specials.append("<tr><td>Aug Interest</td><td>" + taxesMap.get("varAugSpecialPrior").replaceAll(pattern, "") + "</td><td></td><td>"
				+ taxesMap.get("varAugSpecial1st").replaceAll(pattern, "") + "</td><td>"
				+ taxesMap.get("varAugSpecialTotal").replaceAll(pattern, "") + "</td></tr>");

		specials.append("<tr><td>Dec Interest</td><td>" + taxesMap.get("varDecSpecialPrior").replaceAll(pattern, "") + "</td><td></td><td>"
				+ taxesMap.get("varDecSpecial1st").replaceAll(pattern, "") + "</td><td>"
				+ taxesMap.get("varDecSpecialTotal").replaceAll(pattern, "") + "</td></tr>");

		specials.append("<tr><td>Adjustments</td><td>" + taxesMap.get("varSpecialAdjPrior").replaceAll(pattern, "") + "</td><td>"
				+ taxesMap.get("varSpecialAdj1st").replaceAll(pattern, "") + "</td><td>"
				+ taxesMap.get("varSpecialAdj2nd").replaceAll(pattern, "") + "</td><td>"
				+ taxesMap.get("varSpecialAdjTotal").replaceAll(pattern, "") + "</td></tr>");

		specials.append("<tr><td>Total</td><td>" + taxesMap.get("varSpecialTotalPrior").replaceAll(pattern, "") + "</td><td>"
				+ taxesMap.get("varSpecialTotal1st").replaceAll(pattern, "") + "</td><td>"
				+ taxesMap.get("varSpecialTotal2nd").replaceAll(pattern, "") + "</td><td>"
				+ taxesMap.get("varSpecialTotalTotal").replaceAll(pattern, "") + "</td></tr>");

		return specials.toString();
	}

	private String getTaxTables(HtmlParser3 htmlParser3, String LinkID) {
		StringBuilder taxTables = new StringBuilder();
		// get tax tables content
		Node taxTableLinkNode = htmlParser3.getNodeById(LinkID);
		Map<String, String> taxesMap = new HashMap<String, String>();
		if (taxTableLinkNode != null) {
			LinkTag taxTableLinkTag = (LinkTag) taxTableLinkNode;

			String taxTableContent = getLinkContents(dataSite.getLink() + taxTableLinkTag.getLink());
			htmlParser3 = new HtmlParser3(taxTableContent);
			Pattern p = Pattern.compile("(?is)so2\\.addVariable\\(\"([^\"]+)\"\\s*,\\s*\"([^\"]+)\"\\)");
			Matcher m = p.matcher(taxTableContent);

			while (m.find()) {
				taxesMap.put(m.group(1), m.group(2));
			}
			// taxes/reductions table
			StringBuilder taxesReductions = new StringBuilder();
			if (taxesMap.get("varGrossPrior") != null) {
				taxesReductions.append("<table id=\"" + LinkID + "_taxesReductions\" width=\"100%\" align=\"center\" border=\"1\">");
				taxesReductions
						.append("<tr><th align = \"left\" width = \"20%\"></th><th align = \"left\" width = \"20%\">Prior Years</th><th align = \"left\" width = \"20%\">1st Half</th><th align = \"left\" width = \"20%\">2nd Half</th><th align = \"left\" width = \"20%\">Total</th></tr>");
				taxesReductions.append("<tr><td>Gross Taxes</td><td>" + taxesMap.get("varGrossPrior") + "</td><td>"
						+ taxesMap.get("varGross1st") + "</td><td>" + taxesMap.get("varGross2nd") + "</td><td>" + taxesMap.get("varGrossTotal") + "</td></tr>");
				taxesReductions.append("<tr><td width=\"20%\">Tax reduction</td><td width=\"20%\">" + taxesMap.get("varHB920Prior") + "</td><td width=\"20%\">"
						+ taxesMap.get("varHB9201st") + "</td><td>" + taxesMap.get("varHB9202nd") + "</td><td>" + taxesMap.get("varHB920Total") + "</td></tr>");
				taxesReductions.append("<tr><td width=\"20%\">10%</td><td width=\"20%\">" + taxesMap.get("var10Prior") + "</td><td>"
						+ taxesMap.get("var101st") + "</td><td>" + taxesMap.get("var102nd") + "</td><td>" + taxesMap.get("var10Total") + "</td></tr>");
				taxesReductions.append("<tr><td>Owner Occupied</td><td>" + taxesMap.get("varOOPrior") + "</td><td>"
						+ taxesMap.get("varOO1st") + "</td><td>" + taxesMap.get("varOO2nd") + "</td><td>" + taxesMap.get("varOOTotal") + "</td></tr>");
				taxesReductions.append("<tr><td>Homestead</td><td>" + taxesMap.get("varHomesteadPrior") + "</td><td>"
						+ taxesMap.get("varHomestead1st") + "</td><td>" + taxesMap.get("varHomestead2nd") + "</td><td>" + taxesMap.get("varHomesteadTotal")
						+ "</td></tr>");
				taxesReductions.append("<tr><td>Reduction Total</td><td>" + taxesMap.get("varPriorSubTotal") + "</td><td>"
						+ taxesMap.get("var1stSubTotal") + "</td><td>" + taxesMap.get("var2ndSubTotal") + "</td><td>" + taxesMap.get("varTotalSubTotal")
						+ "</td></tr>");
				taxesReductions.append("<tr><td>Total</td><td>" + taxesMap.get("varReductionGrandTotalPrior") + "</td><td>"
						+ taxesMap.get("varReductionGrandTotal1st") + "</td><td>" + taxesMap.get("varReductionGrandTotal2nd") + "</td><td>"
						+ taxesMap.get("varReductionGrandTotalTotal") + "</td></tr>");
				taxesReductions.append("</table>");
			}
			
			// penalties/interest table
			StringBuilder penaltiesInterest = new StringBuilder();
			if (taxesMap.get("varPenPrior") != null) {
				penaltiesInterest.append("<table id=\"" + LinkID + "_penaltiesInterest\" width=\"100%\" align=\"center\" border=\"1\">");
				penaltiesInterest
						.append("<tr><th align = \"left\" width = \"20%\"></th><th align = \"left\" width = \"20%\">Prior Years</th><th align = \"left\" width = \"20%\">1st Half</th><th align = \"left\" width = \"20%\">2nd Half</th><th align = \"left\" width = \"20%\">Total</th></tr>");
				penaltiesInterest.append("<tr><td>Penalties</td><td>" + taxesMap.get("varPenPrior") + "</td><td>"
						+ taxesMap.get("varPen1st") + "</td><td>" + taxesMap.get("varPen2nd") + "</td><td>" + taxesMap.get("varPenTotal") + "</td></tr>");
				penaltiesInterest.append("<tr><td>Adjustments</td><td>" + taxesMap.get("varAdjPrior") + "</td><td>"
						+ taxesMap.get("varAdj1st") + "</td><td>" + taxesMap.get("varAdj2nd") + "</td><td>" + taxesMap.get("varAdjTotal") + "</td></tr>");
				penaltiesInterest.append("<tr><td>Aug Interest</td><td>" + taxesMap.get("varAugPrior") + "</td><td></td><td>"
						+ taxesMap.get("varAug1st") + "</td><td>" + taxesMap.get("varAugTotal") + "</td></tr>");
				penaltiesInterest.append("<tr><td>Dec Interest</td><td>" + taxesMap.get("varDecPrior") + "</td><td></td><td>" + taxesMap.get("varDec1st")
						+ "</td><td>" + taxesMap.get("varDecTotal") + "</td></tr>");
				penaltiesInterest.append("<tr><td>Total</td><td>" + taxesMap.get("varPenGrandTotalPrior") + "</td><td>"
						+ taxesMap.get("varPenGrandTotal1st") + "</td><td>" + taxesMap.get("varPenGrandTotal2nd") + "</td><td>"
						+ taxesMap.get("varPenGrandTotalTotal") + "</td></tr>");

				penaltiesInterest.append("</table>");
			}
			
			// recoupment table
			StringBuilder recoupment = new StringBuilder();
			if (taxesMap.get("varRecoupPrior") != null) {
				recoupment.append("<table id=\"" + LinkID + "_recoupment\" width=\"100%\" align=\"center\" border=\"1\">");
				recoupment
						.append("<tr><th align = \"left\" width = \"20%\"></th><th align = \"left\" width = \"20%\">Prior Years</th><th align = \"left\" width = \"20%\">1st Half</th><th align = \"left\" width = \"20%\">2nd Half</th><th align = \"left\" width = \"20%\">Total</th></tr>");
				recoupment.append("<tr><td>CAUV Recoupment</td><td>" + taxesMap.get("varRecoupPrior") + "</td><td>"
						+ taxesMap.get("varRecoup1st") + "</td><td>" + taxesMap.get("varRecoup2nd") + "</td><td>" + taxesMap.get("varRecoupTotal")
						+ "</td></tr>");
				recoupment.append("<tr><td>Reduction</td><td>" + taxesMap.get("varRecoupRedPrior") + "</td><td>"
						+ taxesMap.get("varRecoupRed1st") + "</td><td>" + taxesMap.get("varRecoupRed2nd") + "</td><td>" + taxesMap.get("varRecoupRedTotal")
						+ "</td></tr>");
				recoupment.append("<tr><td>Ag Recoupment</td><td>" + taxesMap.get("varRecoupPenPrior") + "</td><td>"
						+ taxesMap.get("varRecoupPen1st") + "</td><td>"
						+ taxesMap.get("varRecoupPen2nd") + "</td><td>" + taxesMap.get("varRecoupPenTotal") + "</td></tr>");
				recoupment.append("<tr><td>Penalties</td><td>" + taxesMap.get("varAgPrior") + "</td><td>"
						+ taxesMap.get("varAg1st") + "</td><td>"
						+ taxesMap.get("varAg2nd") + "</td><td>" + taxesMap.get("varAgTotal") + "</td></tr>");
				recoupment.append("<tr><td>Aug Interest</td><td>" + taxesMap.get("varRecoupAugPrior") + "</td><td></td><td>"
						+ taxesMap.get("varRecoupAug1st") + "</td><td>" + taxesMap.get("varRecoupAugTotal") + "</td></tr>");
				recoupment.append("<tr><td>Dec Interest</td><td>" + taxesMap.get("varRecoupDecPrior") + "</td><td></td><td>"
						+ taxesMap.get("varRecoupDec1st") + "</td><td>" + taxesMap.get("varRecoupDecTotal") + "</td></tr>");
				recoupment.append("<tr><td>Adjustments</td><td>" + taxesMap.get("varRecoupAdjPrior") + "</td><td>"
						+ taxesMap.get("varRecoupAdj1st") + "</td><td>"
						+ taxesMap.get("varRecoupAdj2nd") + "</td><td>" + taxesMap.get("varRecoupAdjTotal") + "</td></tr>");
				recoupment.append("<tr><td>Total</td><td>" + taxesMap.get("varRecoupGrandTotalPrior") + "</td><td>"
						+ taxesMap.get("varRecoupGrandTotal1st") + "</td><td>"
						+ taxesMap.get("varRecoupGrandTotal2nd") + "</td><td>" + taxesMap.get("varRecoupGrandTotalTotal") + "</td></tr>");

				recoupment.append("</table>");
			}
			
			// specials table
			StringBuilder specials = new StringBuilder();

			int noOfTables = 1;
			String specialTitle = taxesMap.get("varSpecialTitle");

			if (specialTitle != null) {
				if (specialTitle.contains("ONTRAC")) {
					noOfTables = 2;
				}
				specials.append("<table id=\"" + LinkID + "_specials1\" width=\"100%\" align=\"center\" border=\"1\">");
				specials.append(getSpecialTaxes(taxesMap, "::\\s*ONTRAC\\s*::.*"));
				specials.append("</table>");

				if (noOfTables == 2) {
					specials.append("<table id=\"" + LinkID + "_specials2\" width=\"100%\" align=\"center\" border=\"1\">");
					specials.append(getSpecialTaxes(taxesMap, ".*::\\s*ONTRAC\\s*::"));
					specials.append("</table>");
				}

				specials.append("<table id=\"" + LinkID + "_specialsTotal\" width=\"100%\" align=\"center\" border=\"1\">");
				specials.append("<tr><th align = \"left\" width = \"40%\">ALL Special Assessments Total:</th><th align = \"left\" width = \"15%\">"
						+ taxesMap.get("varSpecialGrandTotalPrior") + "</th><th align = \"left\" width = \"15%\">"
						+ taxesMap.get("varSpecialGrandTotal1st") + "</th><th align = \"left\" width = \"15%\">"
						+ taxesMap.get("varSpecialGrandTotal2nd") + "</th><th align = \"left\" width = \"15%\">"
						+ taxesMap.get("varSpecialGrandTotalTotal")
						+ "</th></tr>");
				specials.append("</table>");
			}
			
			// grossDue line
			StringBuilder grossDue = new StringBuilder();

			if (taxesMap.get("varGrossDuePrior") != null) {
				grossDue.append("<table id=\"" + LinkID + "_grossDue\" width=\"100%\" align=\"center\" border=\"1\">");
				grossDue
						.append("<tr><th align = \"left\" width = \"20%\"></th><th align = \"left\" width = \"20%\">Prior Years</th><th align = \"left\" width = \"20%\">1st Half</th><th align = \"left\" width = \"20%\">2nd Half</th><th align = \"left\" width = \"20%\">Total</th></tr>");
				grossDue.append("<tr><td>Gross Due:</td><td>" + taxesMap.get("varGrossDuePrior") + "</td><td>"
						+ taxesMap.get("varGrossDue1st") + "</td><td>" + taxesMap.get("varGrossDue2nd") + "</td><td>" + taxesMap.get("varGrossDueTotal")
						+ "</td></tr>");
				grossDue.append("</table>");
			}
			
			// netDue line
			StringBuilder netDue = new StringBuilder();

			if (taxesMap.get("varNetGrandTotalPrior") != null) {
				netDue.append("<table id=\"" + LinkID + "_netDue\" width=\"100%\" align=\"center\" border=\"1\">");
				netDue.append("<tr><th align = \"left\" width = \"20%\"></th><th align = \"left\" width = \"20%\">Prior Years</th><th align = \"left\" width = \"20%\">1st Half</th><th align = \"left\" width = \"20%\">2nd Half</th><th align = \"left\" width = \"20%\">Total</th></tr>");
				netDue.append("<tr><td>Net Due:</td><td>" + taxesMap.get("varNetGrandTotalPrior") + "</td><td>"
						+ taxesMap.get("varNetGrandTotal1st") + "</td><td>" + taxesMap.get("varNetGrandTotal2nd") + "</td><td>"
						+ taxesMap.get("varNetGrandTotalTotal")
						+ "</td></tr>");
				netDue.append("</table>");
			}
			
			// payments table
			StringBuilder paymentsTable = new StringBuilder();

			if (taxesMap.get("varNetGrandTotalPrior") != null) {
				paymentsTable.append("<table id=\"" + LinkID + "_paymentsTable\" width=\"100%\" align=\"center\" border=\"1\">");
				paymentsTable.append("<tr>"
						+ "<th align = \"left\" width = \"20%\">Date</th>"
						+ "<th align = \"left\" width = \"20%\">Prior</th><"
						+ "th align = \"left\" width = \"20%\">FirstHalf</th>"
						+ "<th align = \"left\" width = \"20%\">SecondHalf</th>"
						+ "<th align = \"left\" width = \"20%\">Total</th>"
						+ "<th align = \"left\" width = \"20%\">Type</th></tr>");

				if (taxesMap.containsKey("varPaymentsColDate") && taxesMap.containsKey("varPaymentsColAmountPrior")
						&& taxesMap.containsKey("varPaymentsColAmount1st")
						&& taxesMap.containsKey("varPaymentsColAmount2nd") && taxesMap.containsKey("varPaymentsColAmountTotal")
						&& taxesMap.containsKey("varPaymentsColUser")) {
					paymentsTable.append("<tr><td>" + taxesMap.get("varPaymentsColDate").replaceAll("::*ONTRAC::([^:]+)(?:::ONTRAC::([^:]+))?", "$1") + "</td><td>"
							+ taxesMap.get("varPaymentsColAmountPrior").replaceAll("::*ONTRAC::([^:]+)(?:::ONTRAC::([^:]+))?", "$1") + "</td><td>"
							+ taxesMap.get("varPaymentsColAmount1st").replaceAll("::*ONTRAC::([^:]+)(?:::ONTRAC::([^:]+))?", "$1") + "</td><td>"
							+ taxesMap.get("varPaymentsColAmount2nd").replaceAll("::*ONTRAC::([^:]+)(?:::ONTRAC::([^:]+))?", "$1") + "</td><td>"
							+ taxesMap.get("varPaymentsColAmountTotal").replaceAll("::*ONTRAC::([^:]+)(?:::ONTRAC::([^:]+))?", "$1") + "</td><td>"
							+ taxesMap.get("varPaymentsColUser").replaceAll("::*ONTRAC::([^:]+)(?:::ONTRAC::([^:]+))?", "$1") + "</td></tr>");

					paymentsTable.append("<tr><td>" + taxesMap.get("varPaymentsColDate").replaceAll("(?:::*ONTRAC::([^:]+))?::ONTRAC::([^:]+)", "$2") + "</td><td>"
							+ taxesMap.get("varPaymentsColAmountPrior").replaceAll("(?:::*ONTRAC::([^:]+))?::ONTRAC::([^:]+)", "$2") + "</td><td>"
							+ taxesMap.get("varPaymentsColAmount1st").replaceAll("(?:::*ONTRAC::([^:]+))?::ONTRAC::([^:]+)", "$2") + "</td><td>"
							+ taxesMap.get("varPaymentsColAmount2nd").replaceAll("(?:::*ONTRAC::([^:]+))?::ONTRAC::([^:]+)", "$2") + "</td><td>"
							+ taxesMap.get("varPaymentsColAmountTotal").replaceAll("(?:::*ONTRAC::([^:]+))?::ONTRAC::([^:]+)", "$2") + "</td><td>"
							+ taxesMap.get("varPaymentsColUser").replaceAll("(?:::*ONTRAC::([^:]+))?::ONTRAC::([^:]+)", "$2") + "</td></tr>");
				}
				paymentsTable.append("<tr ><td><Strong>Total</strong></td><td><Strong>"
						+ taxesMap.get("varPaymentsGrandTotalPrior").replaceAll("(?:::*ONTRAC::([^:]+))?::ONTRAC::([^:]+)", "$2") + "</strong></td><td><Strong>"
						+ taxesMap.get("varPaymentsGrandTotal1st").replaceAll("(?:::*ONTRAC::([^:]+))?::ONTRAC::([^:]+)", "$2") + "</strong></td><td><Strong>"
						+ taxesMap.get("varPaymentsGrandTotal2nd").replaceAll("(?:::*ONTRAC::([^:]+))?::ONTRAC::([^:]+)", "$2") + "</strong></td><td><Strong>"
						+ taxesMap.get("varPaymentsGrandTotalTotal").replaceAll("(?:::*ONTRAC::([^:]+))?::ONTRAC::([^:]+)", "$2") + "</strong></td><td></td></tr>");
				paymentsTable.append("</table>");
			}
			
			// build tax table
			taxTables.append("<table width=\"100%\" align=\"center\">");
			taxTables.append("<tr><th align=\"left\"><h3>" + taxesMap.get("ty") + "</h3></th></tr>");
			taxTables.append("<tr><td><strong>Taxes/Reductions</strong></td></tr>");
			taxTables.append("<tr><td>" + taxesReductions.toString() + "</td></tr>");

			taxTables.append("<tr><td><strong>Pen/Int/Adj</strong></td></tr>");
			taxTables.append("<tr><td>" + penaltiesInterest.toString() + "</td></tr>");

			taxTables.append("<tr><td><strong>Recoupment</strong></td></tr>");
			taxTables.append("<tr><td>" + recoupment.toString() + "</td></tr>");

			taxTables.append("<tr><td><strong>Specials</strong></td></tr>");
			taxTables.append("<tr><td>" + specials.toString() + "</td></tr>");

			taxTables.append("<tr><td><strong>Gross Due</strong></td></tr>");
			taxTables.append("<tr><td>" + grossDue.toString() + "</td></tr>");

			taxTables.append("<tr><td border = \"1\"><strong>Payments</strong></td></tr>");
			taxTables.append("<tr><td>" + paymentsTable.toString() + "</td></tr>");

			taxTables.append("<tr><td><strong>Net Due</strong></td></tr>");
			taxTables.append("<tr><td>" + netDue.toString() + "</td></tr>");

			taxTables.append("</table>");

		}
		return taxTables.toString();
	}

	@SuppressWarnings({ "rawtypes", "unchecked" })
	public Object parseAndFillResultMap(ServerResponse response, String detailsHtml, ResultMap resultMap) {
		try {
			resultMap.put(OtherInformationSetKey.SRC_TYPE.getKeyName(), "TR");

			HtmlParser3 htmlParser3 = new HtmlParser3(Tidy.tidyParse(detailsHtml, null));
			NodeList nodes = htmlParser3.getNodeList();
			NodeList tables = nodes.extractAllNodesThatMatch(new TagNameFilter("table"), true);
			NodeList finalRes = tables.extractAllNodesThatMatch(new HasAttributeFilter("id", "finalResults"));
			String deedNumber = "";
			String convNumber = "";
			String date = "";
			String currentYearLinkID = "ctl00_Contentplaceholder1_btnMmTaxes";
			if (finalRes.size() > 0) {
				// parcel ID
				Node parcelIDNode = htmlParser3.getNodeById("ctl00_lblParcelNumberRO");
				if (parcelIDNode != null) {
					String parcelID = parcelIDNode.toPlainTextString().trim();
					resultMap.put(PropertyIdentificationSetKey.PARCEL_ID.getKeyName(), parcelID);
					if(parcelID.matches("\\d{3}-\\d+-\\d{2}.\\d{3}")){
						resultMap.put(PropertyIdentificationSetKey.PROPERTY_TYPE.getKeyName(), "Real Estate");
					}
				}
				
				// address
				Node addressNode = htmlParser3.getNodeById("ctl00_lblParcelLocationRO");
				ro.cst.tsearch.servers.functions.OHLickingTR.parseAddressOnServer(resultMap, addressNode);

				// names
				Node ownerNode = htmlParser3.getNodeById("ctl00_lblOwnerRO");
				if (ownerNode != null) {
					ro.cst.tsearch.servers.functions.OHLickingTR.parseNames(resultMap, ownerNode.toPlainTextString().replaceAll("\\sET AL(\\s|$)", " ETAL$1")
							.trim());
				}
				// legal
				ro.cst.tsearch.servers.functions.OHLickingTR.parseLegal(resultMap, htmlParser3);

				// taxHistorySet variables
				int year = 0;
				double baseAmount = 0;
				double amountDue = 0;
				double priorDelinquent = 0;
				double amountPaid = 0;

				// gross due
				double gd1d = 0;
				double gd2d = 0;

				// recoupments
				double r1d = 0;
				double r2d = 0;

				// taxInstallmentSet variables
				double ba1d = 0;
				double ba2d = 0;
				double ad1d = 0;
				double ad2d = 0;
				double pen1d = 0;
				double pen2d = 0;
				double ap1d = 0;
				double ap2d = 0;
				String status1 = "";
				String status2 = "";
				DecimalFormat decimalFormat = new DecimalFormat("0.00");

				// taxHistorySet
				Node taxes = htmlParser3.getNodeById("currentYearTaxTablesRow");
				if (taxes != null) {

					NodeList yearNodeList = taxes.getChildren().extractAllNodesThatMatch(new RegexFilter("(?is)tax\\s+year"), true);
					if (yearNodeList.size() > 0) {
						year = Integer.parseInt(yearNodeList.elementAt(0).toPlainTextString().trim().replaceAll("(?s).*?(\\d+).*", "$1"));
					}
					
					Node taxesReductions = htmlParser3.getNodeById(currentYearLinkID + "_taxesReductions");
					if (taxesReductions != null) {
						TableTag taxesTable = (TableTag) taxesReductions;
						TableRow[] rows = taxesTable.getRows();

						for (int i = 1; i < rows.length; i++)
						{
							TableRow r = rows[i];
							if (r.getColumns()[0].toPlainTextString().trim().matches("\\s*Total\\s*")) {
								String ba1 = r.getColumns()[2].toPlainTextString().replaceAll("[ $,-]", "");
								String ba2 = r.getColumns()[3].toPlainTextString().replaceAll("[ $,-]", "");

								ba1d = StringUtils.isEmpty(ba1) ? 0 : Double.parseDouble(ba1);
								ba2d = StringUtils.isEmpty(ba2) ? 0 : Double.parseDouble(ba2);
								baseAmount = ba1d + ba2d;
							}

						}
					}

					// net due
					Node netDue = htmlParser3.getNodeById(currentYearLinkID + "_netDue");
					if (netDue != null) {
						TableTag netDueTable = (TableTag) netDue;
						TableRow[] rows = netDueTable.getRows();

						for (int i = 1; i < rows.length; i++) {
							TableRow r = rows[i];
							if (r.getColumns()[0].toPlainTextString().contains("Net Due")) {

								String ad1 = r.getColumns()[2].toPlainTextString().replaceAll("[ $,-]", "");
								String ad2 = r.getColumns()[3].toPlainTextString().replaceAll("[ $,-]", "");
								String priorYears = r.getColumns()[1].toPlainTextString().replaceAll("[ $,-]", "");

								ad1d = StringUtils.isEmpty(ad1) ? 0 : Double.parseDouble(ad1);
								ad2d = StringUtils.isEmpty(ad2) ? 0 : Double.parseDouble(ad2);
								amountDue = ad1d + ad2d;
								priorDelinquent = StringUtils.isEmpty(priorYears) ? 0 : Double.parseDouble(priorYears);

							}
						}
					}

					// gross due
					Node grossDue = htmlParser3.getNodeById(currentYearLinkID + "_grossDue");
					if (grossDue != null) {
						TableTag grossDueTable = (TableTag) grossDue;
						TableRow[] rows = grossDueTable.getRows();

						for (int i = 1; i < rows.length; i++) {
							TableRow r = rows[i];
							if (r.getColumns()[0].toPlainTextString().contains("Gross Due")) {
								String gd1 = r.getColumns()[2].toPlainTextString().replaceAll("[ $,-]", "");
								String gd2 = r.getColumns()[3].toPlainTextString().replaceAll("[ $,-]", "");

								gd1d = StringUtils.isEmpty(gd1) ? 0 : Double.parseDouble(gd1);
								gd2d = StringUtils.isEmpty(gd2) ? 0 : Double.parseDouble(gd2);
							}
						}
					}

					// recoupment
					Node recoupment = htmlParser3.getNodeById(currentYearLinkID + "_recoupment");
					if (recoupment != null) {
						TableTag recoupmentTable = (TableTag) recoupment;
						TableRow[] rows = recoupmentTable.getRows();

						for (int i = 1; i < rows.length; i++) {
							TableRow r = rows[i];
							if (r.getColumns()[0].toPlainTextString().trim().matches("Total")) {
								String r1 = r.getColumns()[2].toPlainTextString().replaceAll("[ $,-]", "");
								String r2 = r.getColumns()[3].toPlainTextString().replaceAll("[ $,-]", "");

								r1d = StringUtils.isEmpty(r1) ? 0 : Double.parseDouble(r1);
								r2d = StringUtils.isEmpty(r2) ? 0 : Double.parseDouble(r2);
							}
						}
					}

					// pentalties
					Node penalties = htmlParser3.getNodeById(currentYearLinkID + "_penaltiesInterest");
					if (penalties != null) {
						TableTag penaltiesTable = (TableTag) penalties;
						TableRow[] rows = penaltiesTable.getRows();

						for (int i = 1; i < rows.length; i++)
						{
							TableRow r = rows[i];
							if (r.getColumns()[0].toPlainTextString().trim().matches("Total")) {
								String pen1 = r.getColumns()[2].toPlainTextString().replaceAll("[ $,-]", "");
								String pen2 = r.getColumns()[3].toPlainTextString().replaceAll("[ $,-]", "");

								pen1d = StringUtils.isEmpty(pen1) ? 0 : Double.parseDouble(pen1);
								pen2d = StringUtils.isEmpty(pen2) ? 0 : Double.parseDouble(pen2);
							}
						}
					}
					// paymentsTable
					Node payments = htmlParser3.getNodeById(currentYearLinkID + "_paymentsTable");
					if (payments != null) {
						TableTag paymentsTable = (TableTag) payments;
						TableRow[] rows = paymentsTable.getRows();

						for (int i = 1; i < rows.length; i++)
						{
							TableRow r = rows[i];
							if (r.getColumns()[0].toPlainTextString().trim().matches("Total")) {

								String ap1 = r.getColumns()[2].toPlainTextString().replaceAll("[ $,-]", "");
								String ap2 = r.getColumns()[3].toPlainTextString().replaceAll("[ $,-]", "");

								ap1d = StringUtils.isEmpty(ap1) ? 0 : Double.parseDouble(ap1);
								ap2d = StringUtils.isEmpty(ap2) ? 0 : Double.parseDouble(ap2);
								amountPaid = ap1d + ap2d;
							}
						}

					}

					resultMap.put(TaxHistorySetKey.YEAR.getKeyName(), String.valueOf(year));
					resultMap.put(TaxHistorySetKey.BASE_AMOUNT.getKeyName(), decimalFormat.format(baseAmount));
					resultMap.put(TaxHistorySetKey.AMOUNT_PAID.getKeyName(), decimalFormat.format(amountPaid));
					resultMap.put(TaxHistorySetKey.TOTAL_DUE.getKeyName(), decimalFormat.format(amountDue));
					resultMap.put(TaxHistorySetKey.PRIOR_DELINQUENT.getKeyName(), decimalFormat.format(priorDelinquent));

					// taxInstallmentSet
					Map<String, String[]> installmentsMap = new HashMap<String, String[]>();
					List<List> installmentsBody = new ArrayList<List>();
					ResultTable resultTable = new ResultTable();

					String[] installmentsHeader =
					{
							TaxInstallmentSetKey.BASE_AMOUNT.getShortKeyName(),
							TaxInstallmentSetKey.PENALTY_AMOUNT.getShortKeyName(),
							TaxInstallmentSetKey.TOTAL_DUE.getShortKeyName(),
							TaxInstallmentSetKey.STATUS.getShortKeyName(),
							TaxInstallmentSetKey.AMOUNT_PAID.getShortKeyName()
					};

					for (String s : installmentsHeader) {
						installmentsMap.put(s, new String[] { s, "" });
					}

					if (ad1d == 0) {
						status1 = "PAID";
					}
					else {
						status1 = "UNPAID";
					}

					if (ad2d == 0) {
						status2 = "PAID";
					}
					else {
						status2 = "UNPAID";
					}

					List installmentsRow1 = new ArrayList<String>();
					List installmentsRow2 = new ArrayList<String>();

					installmentsRow1.add(decimalFormat.format(ba1d));
					installmentsRow2.add(decimalFormat.format(ba2d));

					installmentsRow1.add(decimalFormat.format(pen1d));
					installmentsRow2.add(decimalFormat.format(pen2d));

					installmentsRow1.add(decimalFormat.format(ad1d));
					installmentsRow2.add(decimalFormat.format(ad2d));

					installmentsRow1.add(status1);
					installmentsRow2.add(status2);

					installmentsRow1.add(decimalFormat.format(ap1d));
					installmentsRow2.add(decimalFormat.format(ap2d));

					installmentsBody.add(installmentsRow1);
					installmentsBody.add(installmentsRow2);

					if (!installmentsBody.isEmpty()) {
						resultTable.setHead(installmentsHeader);
						resultTable.setMap(installmentsMap);
						resultTable.setBody(installmentsBody);
						resultTable.setReadOnly();
						resultMap.put("TaxInstallmentSet", resultTable);
					}
				}

				// land
				Node land = htmlParser3.getNodeById("ctl00_cntMain_gdvResults_ctl02_lblTaxableLand");
				Node improvements = htmlParser3.getNodeById("ctl00_cntMain_gdvResults_ctl02_lblTaxableImprov");
				Node totalAssessed = htmlParser3.getNodeById("ctl00_cntMain_gdvResults_ctl02_lblTaxableTotal");

				if (land != null) {
					resultMap.put(PropertyAppraisalSetKey.LAND_APPRAISAL.getKeyName(), land.toPlainTextString().replaceAll("[ $,-]", "").trim());
				}

				if (improvements != null) {
					resultMap.put(PropertyAppraisalSetKey.IMPROVEMENT_APPRAISAL.getKeyName(), improvements.toPlainTextString().replaceAll("[ $,-]", "").trim());
				}

				if (totalAssessed != null) {
					resultMap.put(PropertyAppraisalSetKey.TOTAL_APPRAISAL.getKeyName(), totalAssessed.toPlainTextString().replaceAll("[ $,-]", "").trim());
					resultMap.put(PropertyAppraisalSetKey.TOTAL_ASSESSMENT.getKeyName(), totalAssessed.toPlainTextString().replaceAll("[ $,-]", "").trim());
				}

				// transfers
				Node transferList = htmlParser3.getNodeById("transfersTable");
				if (transferList != null) {
					TableTag transfersT = (TableTag) transferList;
					TableRow[] transfersR = transfersT.getRows();

					String[] transferHeader = { SaleDataSetKey.SALES_PRICE.getShortKeyName(),
							SaleDataSetKey.INSTRUMENT_DATE.getShortKeyName(), SaleDataSetKey.GRANTOR.getShortKeyName() };
					List<List> transferBody = new ArrayList<List>();
					List transferRow = new ArrayList();
					ResultTable resT = new ResultTable();
					Map<String, String[]> transferMap = new HashMap<String, String[]>();
					for (String s : transferHeader) {
						transferMap.put(s, new String[] { s, "" });
					}

					for (int i = 1; i < transfersR.length; i++) {
						transferRow = new ArrayList<String>();
						TableRow r = transfersR[i];
						TableColumn[] cols = r.getColumns();
						if (cols.length == 7) {
							
							transferRow.add(cols[4].toPlainTextString().replaceAll("[\\s$,-]", ""));
							transferRow.add(cols[0].toPlainTextString().replaceAll("\\s+", ""));
							transferRow.add(cols[3].toPlainTextString().trim());
							transferBody.add(transferRow);

						}
					}
					if (StringUtils.isNotEmpty(convNumber) && !"0".equals(convNumber)) {
						transferRow = new ArrayList<String>();
						transferRow.add(convNumber);
						transferRow.add("");
						transferRow.add(date);
						transferRow.add("");
						transferBody.add(transferRow);
					}

					if (StringUtils.isNotEmpty(deedNumber) && !"0".equals(deedNumber)) {
						transferRow = new ArrayList<String>();
						transferRow.add(deedNumber);
						transferRow.add("");
						transferRow.add(date);
						transferRow.add("");
						transferBody.add(transferRow);
					}
					if (!transferBody.isEmpty()) {
						resT.setHead(transferHeader);
						resT.setMap(transferMap);
						resT.setBody(transferBody);
						resT.setReadOnly();
						resultMap.put("SaleDataSet", resT);
					}
				}

				Node specials1 = htmlParser3.getNodeById(currentYearLinkID + "_specials1");
				// compute assessment taxes from SpecialAssessmentSet
				if (specials1 != null) {
					// SA installments:
					double sBA1d = 0;
					double sBA2d = 0;
					double sAD1d = 0;
					double sAD2d = 0;
					double sAP1d = 0;
					double sAP2d = 0;
					double sPen1d = 0;
					double sPen2d = 0;
					String sStatus1 = "";
					String sStatus2 = "";

					Map<String, String[]> installmentsMap = new HashMap<String, String[]>();
					TableTag specials1Table = (TableTag) specials1;
					TableRow[] rows1 = specials1Table.getRows();
					List<List> installmentsBody = new ArrayList<List>();
					ResultTable resultTable = new ResultTable();
					String[] installmentsHeader = { "BaseAmount", "TotalDue", "AmountPaid", "PenaltyAmount", "Status" };

					for (String s : installmentsHeader) {
						installmentsMap.put(s, new String[] { s, "" });
					}

					for (int i = 1; i < rows1.length; i++) {
						TableRow r = rows1[i];
						if (r.getColumns()[0].toPlainTextString().trim().matches("\\s*Tax\\s*")) {

							String sBA1 = r.getColumns()[2].toPlainTextString().replaceAll("[ $,-]", "");
							String sBA2 = r.getColumns()[3].toPlainTextString().replaceAll("[ $,-]", "");

							sBA1d = StringUtils.isEmpty(sBA1) ? 0 : Double.parseDouble(sBA1);
							sBA2d = StringUtils.isEmpty(sBA2) ? 0 : Double.parseDouble(sBA2);
						}

						if (r.getColumns()[0].toPlainTextString().trim().matches("\\s*Penalty\\s*")) {

							String sPen1 = r.getColumns()[2].toPlainTextString().replaceAll("[ $,-]", "");
							String sPen2 = r.getColumns()[3].toPlainTextString().replaceAll("[ $,-]", "");

							sPen1d = StringUtils.isEmpty(sPen1) ? 0 : Double.parseDouble(sPen1);
							sPen2d = StringUtils.isEmpty(sPen2) ? 0 : Double.parseDouble(sPen2);
						}
					}
					// SATable2
					Node specials2 = htmlParser3.getNodeById(currentYearLinkID + "_specials2");

					if (specials2 != null) {

						TableTag specials2Table = (TableTag) specials2;
						TableRow[] rows2 = specials2Table.getRows();
						for (int i = 1; i < rows2.length; i++)
						{
							TableRow r2 = rows2[i];
							if (r2.getColumns()[0].toPlainTextString().trim().matches("\\s*Tax\\s*")) {

								String sBA1 = r2.getColumns()[2].toPlainTextString().replaceAll("[ $,-]", "");
								String sBA2 = r2.getColumns()[3].toPlainTextString().replaceAll("[ $,-]", "");

								sBA1d += StringUtils.isEmpty(sBA1) ? 0 : Double.parseDouble(sBA1);
								sBA2d += StringUtils.isEmpty(sBA2) ? 0 : Double.parseDouble(sBA2);
							}

							if (r2.getColumns()[0].toPlainTextString().trim().matches("\\s*Penalty\\s*")) {

								String sPen1 = r2.getColumns()[2].toPlainTextString().replaceAll("[ $,-]", "");
								String sPen2 = r2.getColumns()[3].toPlainTextString().replaceAll("[ $,-]", "");

								sPen1d += StringUtils.isEmpty(sPen1) ? 0 : Double.parseDouble(sPen1);
								sPen2d += StringUtils.isEmpty(sPen2) ? 0 : Double.parseDouble(sPen2);
							}

						}
					}

					// TotalSA
					Node specialsTotal = htmlParser3.getNodeById(currentYearLinkID + "_specialsTotal");

					if (specialsTotal != null) {

						TableTag specialsTotalTable = (TableTag) specialsTotal;
						TableRow[] rowsTotal = specialsTotalTable.getRows();

						TableRow r3 = rowsTotal[0];
						if (r3.getHeaders()[0].toPlainTextString().trim().contains("Special Assessments Total")) {

							String sAD1 = r3.getHeaders()[2].toPlainTextString().replaceAll("[ $,-]", "");
							String sAD2 = r3.getHeaders()[3].toPlainTextString().replaceAll("[ $,-]", "");

							sAD1d = StringUtils.isEmpty(sAD1) ? 0 : Double.parseDouble(sAD1);
							sAD2d = StringUtils.isEmpty(sAD2) ? 0 : Double.parseDouble(sAD2);
						}

						if (ad1d == 0) {
							sStatus1 = "PAID";
							sAD1d = 0.0;
						}
						else {
							sStatus1 = "UNPAID";
						}
						if (ad2d == 0) {
							sStatus2 = "PAID";
							sAD2d = 0.0;
						}
						else {
							sStatus2 = "UNPAID";
						}
					}
					sAP1d = Math.abs(gd1d - ba1d - pen1d - r1d );
					sAP2d = Math.abs(gd2d - ba1d - pen2d - r2d );
					if (amountPaid == 0.0) {
						sAP1d = Math.abs(sAP1d - sAD1d);
						sAP2d = Math.abs(sAP2d - sAD2d);
					}
					List installment1Row = new ArrayList<String>();
					List installment2Row = new ArrayList<String>();

					// baseAmount
					installment1Row.add(decimalFormat.format(sBA1d));
					installment2Row.add(decimalFormat.format(sBA2d));

					// amountDue
					installment1Row.add(decimalFormat.format(sAD1d));
					installment2Row.add(decimalFormat.format(sAD2d));

					// amountPaid
					installment1Row.add(decimalFormat.format(sAP1d));
					installment2Row.add(decimalFormat.format(sAP2d));

					// penalty
					installment1Row.add(decimalFormat.format(sPen1d));
					installment2Row.add(decimalFormat.format(sPen2d));

					// status
					installment1Row.add(sStatus1);
					installment2Row.add(sStatus2);

					installmentsBody.add(installment1Row);
					installmentsBody.add(installment2Row);

					if (!installmentsBody.isEmpty()) {
						try {
							resultTable.setHead(installmentsHeader);
							resultTable.setMap(installmentsMap);
							resultTable.setBody(installmentsBody);
						} catch (Exception e) {
							e.printStackTrace();
						}
						resultTable.setReadOnly();
						resultMap.put("SpecialAssessmentSet", resultTable);
					}
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		return null;
	}
}