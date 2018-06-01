package ro.cst.tsearch.servers.types;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Vector;

import org.apache.commons.lang.StringEscapeUtils;
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
import ro.cst.tsearch.extractor.xml.ResultMap;
import ro.cst.tsearch.parser.HtmlParser3;
import ro.cst.tsearch.search.FunctionStatesIterator;
import ro.cst.tsearch.search.filter.FilterResponse;
import ro.cst.tsearch.search.filter.RejectNonRealEstate;
import ro.cst.tsearch.search.filter.newfilters.legal.GenericLegal;
import ro.cst.tsearch.search.filter.newfilters.legal.LegalFilterFactory;
import ro.cst.tsearch.search.filter.newfilters.name.NameFilterFactory;
import ro.cst.tsearch.search.filter.newfilters.pin.PINFilterFactory;
import ro.cst.tsearch.search.filter.newfilters.pin.PinFilterResponse;
import ro.cst.tsearch.search.iterator.ModuleStatesIteratorFactory;
import ro.cst.tsearch.search.module.ConfigurableNameIterator;
import ro.cst.tsearch.servers.TSConnectionURL;
import ro.cst.tsearch.servers.info.TSServerInfo;
import ro.cst.tsearch.servers.info.TSServerInfoConstants;
import ro.cst.tsearch.servers.info.TSServerInfoModule;
import ro.cst.tsearch.servers.response.LinkInPage;
import ro.cst.tsearch.servers.response.ParsedResponse;
import ro.cst.tsearch.servers.response.ServerResponse;
import ro.cst.tsearch.servers.response.OtherInformationSet.OtherInformationSetKey;
import ro.cst.tsearch.utils.RegExUtils;
import ro.cst.tsearch.utils.StringUtils;
import ro.cst.tsearch.utils.Tidy;

import com.stewart.ats.base.document.DocumentI;
import com.stewart.ats.base.document.TaxDocumentI;

public class FLGilchristTR extends FLGenericGovernmaxTR {

	private static final long	serialVersionUID	= 6525099447955957961L;
	public static final String LARGE_PIN_FORMAT = "(?is)([\\dA-Z]{6})([\\dA-Z]{8})([\\dA-Z]{4})";
	public static final String SMALL_PIN_FORMAT = "(?is)([\\dA-Z]{6})([\\dA-Z]{2})";
	
	private static final CheckTangible CHECK_TANGIBLE = new CheckTangible() {
		public boolean isTangible(String row){
			String linkText = getLinkText(row);
			return linkText.matches("(?is)([\\dA-Z]{6})\\s*-\\s*([\\dA-Z]{8})\\s*-\\s*([\\dA-Z]{4})");
		}
	};
	
	public FLGilchristTR(String rsRequestSolverName, String rsSitePath, String rsServerID,
			String rsPrmNameLink, long searchId, int miServerID) {
		super(rsRequestSolverName, rsSitePath, rsServerID, rsPrmNameLink, searchId, miServerID);
		checkTangible = CHECK_TANGIBLE;	
	}
	
	@Override
	protected void ParseResponse(String sAction, ServerResponse serverResponse,
			int viParseID) throws ServerResponseException {

		String rsResponse = serverResponse.getResult();
		ParsedResponse parsedResponse = serverResponse.getParsedResponse();
		parsedResponse.setAttribute("checkTangible", checkTangible);

		rsResponse = StringEscapeUtils.unescapeHtml(rsResponse);
		rsResponse = rsResponse.replaceAll("[^\\p{Alnum}\\p{Punct}\\s]", "");

		String message = ro.cst.tsearch.utils.StringUtils.extractParameter(rsResponse,
				"(?is)<p\\b[^>]*>\\s*(Sorry,\\s*no\\s+records\\s+were\\s+found)(?:\\.\\.\\.)?\\s*</p>");
		if (!message.isEmpty()) {
			parsedResponse.setError(message);
			return;
		}

		switch (viParseID) {
		case ID_SEARCH_BY_MODULE38:
		case ID_INTERMEDIARY:
			StringBuilder outputTable = new StringBuilder();
			Collection<ParsedResponse> smartParsedResponses = smartParseIntermediary(serverResponse, rsResponse, outputTable);

			if (smartParsedResponses.size() > 0) {
				parsedResponse.setResultRows(new Vector<ParsedResponse>(smartParsedResponses));
				parsedResponse.setOnlyResponse(outputTable.toString());
				parsedResponse.setAttribute(ParsedResponse.SERVER_ROW_RESPONSE, outputTable.toString());
			}

			break;

		case ID_DETAILS:
		case ID_SAVE_TO_TSD:

			// get parcel ID
			rsResponse = rsResponse.replaceAll("(?is)(</?t)h\\b", "$1d");
			HtmlParser3 htmlParser3 = new HtmlParser3(rsResponse);
			NodeList nodeList = htmlParser3.getNodeList();
			Node pidNode = htmlParser3.getNodeById("dnn_ctr389_ModuleContent");
			String parcelID = "";
			if (pidNode != null) {
				nodeList = pidNode.getChildren();
				parcelID = HtmlParser3.getValueFromAbsoluteCell(1, 0, HtmlParser3.findNode(nodeList, "Account"), "", true).trim();
			}

			// get details
			String details = getDetails(serverResponse, rsResponse);

			if (viParseID != ID_SAVE_TO_TSD) {
				String originalLink = sAction.replace("?", "&") + "&" + serverResponse.getRawQuerry();
				String sSave2TSDLink = getLinkPrefix(TSConnectionURL.idPOST) + originalLink;

				HashMap<String, String> data = new HashMap<String, String>();
				loadDataHash(data);

				if (isInstrumentSaved(parcelID, null, data)) {
					details += CreateFileAlreadyInTSD();
				} else {
					mSearch.addInMemoryDoc(sSave2TSDLink, details);
					details = addSaveToTsdButton(details, sSave2TSDLink, ID_DETAILS);
				}

				parsedResponse.setPageLink(new LinkInPage(sSave2TSDLink, originalLink, TSServer.REQUEST_SAVE_TO_TSD));
				parsedResponse.setResponse(details);

			} else {

				String filename = parcelID + ".html";
				smartParseDetails(serverResponse, details);
				parsedResponse.setResponse(details);
				msSaveToTSDFileName = filename;
				parsedResponse.setFileName(getServerTypeDirectory() + msSaveToTSDFileName);
				msSaveToTSDResponce = details + CreateFileAlreadyInTSD();
			}

			break;

		case ID_GET_LINK:
			if (org.apache.commons.lang.StringUtils.containsIgnoreCase(sAction, "/SearchTaxRoll/RealEstateTaxes.aspx?page385=")) {
				ParseResponse(sAction, serverResponse, ID_INTERMEDIARY);
			} else {
				ParseResponse(sAction, serverResponse, ID_DETAILS);
			}

			break;

		default:
			break;
		}

	}

	protected String getDetails(ServerResponse serverResponse, String rsResponse) {
		try {
			/* If from memory - use it as is */
			if (!org.apache.commons.lang.StringUtils.containsIgnoreCase(rsResponse, "<html")) {
				return rsResponse;
			}

			rsResponse = Tidy.tidyParse(rsResponse, null);

			StringBuilder detailsSB = new StringBuilder();
			String details = "";
			HtmlParser3 htmlParser3 = new HtmlParser3(rsResponse);
			Node mainNode = htmlParser3.getNodeById("ContentContainerCell");

			if (mainNode != null && mainNode instanceof TableColumn) {// get main contents
				TableColumn mainNodeTC = (TableColumn) mainNode;
				mainNodeTC.setAttribute("id", "mainDetails");
				detailsSB.append("<tr>"
						+ mainNodeTC.toHtml().replaceAll("(?is)(Tax\\s+Account|Property\\s+Type|Last\\s+Update"
						 + "|Legal\\s+Description|Tax\\s+Bills)", "<b>$1</b>")
						+ "</tr>");
			}

			// get tax bills links contents
			String linkStart = getDataSite().getServerHomeLink();
			linkStart = linkStart.substring(0, linkStart.lastIndexOf("/"));
			String taxBillsLink = "";

			Node taxHistoryDiv = htmlParser3.getNodeById("390");
			if (taxHistoryDiv != null) {
				NodeList taxBillLinkNodes = taxHistoryDiv.getChildren().extractAllNodesThatMatch(new TagNameFilter("a"), true);
				if (taxBillLinkNodes.size() > 0) {
					for (int i = 0; i < taxBillLinkNodes.size(); i++) {
						Node taxBillLinkNode = taxBillLinkNodes.elementAt(i);
						if (taxBillLinkNode != null && taxBillLinkNode instanceof LinkTag) {
							taxBillsLink = linkStart + ((LinkTag) taxBillLinkNode).getLink();
							// on shorter account numbers replace account no with account no+6 spaces+"-" .. otherwise it doesn't work
							// http://fl-gilchrist-taxcollector.publicaccessnow.com/SearchTaxRoll/RealEstateTaxes/AccountDetail/BillDetail.aspx?p=020250-00
							// -&a=6665.0000&b=107553.0000&y=1995
							String pidParam = StringUtils.extractParameterFromUrl(taxBillsLink, "p");
							if (!pidParam.matches(ro.cst.tsearch.servers.functions.FLGilchristTR.REAL_ESTATE_ACCOUNT_TYPE_PATTERN)) {
								String newPID = pidParam.replaceFirst("\\s-", "      -");
								taxBillsLink = taxBillsLink.replaceFirst(pidParam, newPID);
							}

							String tmpBillDetails = getLinkContents(taxBillsLink);
							htmlParser3 = new HtmlParser3(tmpBillDetails);
							Node contentNode = htmlParser3.getNodeById("dnn_ContentPane");

							if (contentNode != null) {
								String contentHtml = StringEscapeUtils.unescapeHtml(contentNode.toHtml()).replaceAll("[^\\p{Alnum}\\p{Punct}\\s]", "")
										.replaceFirst("(?is)<div[^>]*\\s+id=\"lxT389\"[^>]*>.*?</table>\\s*</div>", "")
										.replaceFirst("(?is)<span[^>]*>\\s*Tax\\s+Account\\s*</span>", "");
								detailsSB.append("<tr class=\"taxBillsDetails\""
										+ (i == 0 ? "id=\"currentBillDetails\"" : "") + ">" + contentHtml + "</tr>");
							}
						}
					}
				}
			}

			if (!detailsSB.toString().isEmpty()) {
				details = "<table id=\"allDetails\" align=\"center\" border=\"1\" width=\"800px\">"
						+ detailsSB.toString().replaceAll("(?is)</?(a|input|font)\\b[^>]*>", "")
								.replaceAll("(?is)(</?h)(?:1|2)\\b", "$13")// replace all h1 or h2 with h3	
						+ "</table>";

				return details;
			}
		} catch (Exception e) {
			logger.error("Error while getting details", e);
		}
		return null;
	}

	@Override
	public Collection<ParsedResponse> smartParseIntermediary(ServerResponse response, String intermediariesHtml, StringBuilder outputTable) {
		Vector<ParsedResponse> intermediaryResponse = new Vector<ParsedResponse>();
		try {
			ParsedResponse parsedResponse = response.getParsedResponse();
			HtmlParser3 htmlParser3 = new HtmlParser3(intermediariesHtml);
			NodeList nodeList = htmlParser3.getNodeList();
			Node intermediariesNode = htmlParser3.getNodeById("MVPQuickSearch");

			String header = "<table id=\"intermediaries\" style=\"min-width:600px;\" align=\"center\" border=\"1\">";
			String footer = "</table>";
			StringBuilder intermediariesTableSB = new StringBuilder();

			String linkStart = getDataSite().getLink();
			linkStart = linkStart.substring(linkStart.indexOf("http://") + 7);
			linkStart = linkStart.substring(linkStart.indexOf("/"));
			linkStart = CreatePartialLink(TSConnectionURL.idGET) + linkStart;

			if (intermediariesNode != null) {// turn divs into rows
				NodeList rowDivs = nodeList.extractAllNodesThatMatch(new HasAttributeFilter("class", "results-row ui-widget ui-widget-content clearFix"), true);

				intermediariesTableSB.append(header);
				for (int i = 0; i < rowDivs.size(); i++) {
					intermediariesTableSB.append("<tr><td>" + rowDivs.elementAt(i).toHtml() + "</td></tr>");
				}
				intermediariesTableSB.append(footer);
			}

			intermediariesHtml = intermediariesTableSB.toString()
					.replaceAll("</li>", "<br>")
					.replaceAll("(?is)\\s*</?(div|ul|li|font)[^>]*>\\s*", "")
					.replaceAll("<!--.*?-->", "");

			htmlParser3 = new HtmlParser3(intermediariesHtml);
			Node intermediariesTable = htmlParser3.getNodeById("intermediaries");
			if (intermediariesTable != null) {
				TableTag intermediariesTableT = (TableTag) intermediariesTable;
				TableRow[] rows = intermediariesTableT.getRows();

				for (int i = 0; i < rows.length; i++) {

					TableRow row = rows[i];

					String detailsLink = "";

					// get details link
					LinkTag detailsLinkTag = (LinkTag) HtmlParser3.getFirstTag(row.getChildren(), LinkTag.class, true);
					if (detailsLinkTag != null) {
						detailsLink = linkStart + detailsLinkTag.getLink();
						detailsLinkTag.setLink(detailsLink);
					}

					String fullRow = row.toHtml();
					ResultMap resultMap = new ResultMap();
					ro.cst.tsearch.servers.functions.FLGilchristTR.parseIntermediaryRow(resultMap, fullRow, getSearch().getID());
					resultMap.put(OtherInformationSetKey.SRC_TYPE.getKeyName(), getDataSite().getSiteTypeAbrev());

					ParsedResponse currentResponse = new ParsedResponse();
					currentResponse.setAttribute(ParsedResponse.SERVER_ROW_RESPONSE, fullRow);
					currentResponse.setOnlyResponse(fullRow);
					currentResponse.setPageLink(new LinkInPage(detailsLink, detailsLink, TSServer.REQUEST_SAVE_TO_TSD));

					Bridge bridge = new Bridge(currentResponse, resultMap, searchId);
					DocumentI document = (TaxDocumentI) bridge.importData();
					currentResponse.setDocument(document);

					intermediaryResponse.add(currentResponse);
				}
			}

			// paging links
			NodeList linksNodes = nodeList.extractAllNodesThatMatch(new HasAttributeFilter("class", "pagination"), true);
			if (linksNodes.size() > 0) {
				String nextLink = "";
				StringBuilder footerSB = new StringBuilder(footer);
				footerSB.append("<br><div id=\"pagingDiv\" align=\"center\">");

				// links look like '?page385=2'
				Node linksNode = linksNodes.elementAt(0);
				if (linksNode != null) {
					linksNodes = linksNode.getChildren();
					for (int i = 0; i < linksNodes.size(); i++) {
						if (linksNodes.elementAt(i) instanceof LinkTag) {

							LinkTag tmpLinkT = (LinkTag) linksNodes.elementAt(i);
							String tmpLink = linkStart + tmpLinkT.getLink();
							tmpLinkT.setLink(tmpLink);
							String tmpLinkTHtml = tmpLinkT.toHtml();

							if (RegExUtils.getFirstMatch("(?i)(page385)", tmpLink, 1).isEmpty()) {
								tmpLinkTHtml = tmpLinkTHtml.replaceAll("(?i)</?a\\b[^>]*>", "");
							}
							if (nextLink.isEmpty()) {
								if (!RegExUtils.getFirstMatch("(?is)(Next)", tmpLinkTHtml, 1).isEmpty()) {
									nextLink = "<a href=\"" + tmpLink + "\">Next</a>";
									parsedResponse.setNextLink(nextLink);
								}
							}

							footerSB.append(tmpLinkTHtml + "&nbsp;");
						}
					}
					footerSB.append("</div>");
					footer = footerSB.toString();
				}
			}

			parsedResponse.setHeader(header);
			parsedResponse.setFooter(footer);
			parsedResponse.setResultRows(intermediaryResponse);
			parsedResponse.setOnlyResultRows(intermediaryResponse);
			outputTable.append(intermediariesHtml);

		} catch (Exception e) {
			e.printStackTrace();
		}
		return intermediaryResponse;
	}

	@Override
	protected Object parseAndFillResultMap(ServerResponse response, String detailsHtml, ResultMap resultMap) {
		resultMap.put(OtherInformationSetKey.SRC_TYPE.getKeyName(), getDataSite().getSiteTypeAbrev());
		ro.cst.tsearch.servers.functions.FLGilchristTR.parseAndFillResultMap(response, detailsHtml, resultMap, searchId, getDataSite());
		return null;
	}

	@Override
	protected void setModulesForAutoSearch(TSServerInfo serverInfo) {
		List<TSServerInfoModule> modules = new ArrayList<TSServerInfoModule>();

		TSServerInfoModule module;
		FilterResponse fr = new RejectNonRealEstate(SearchAttributes.OWNER_OBJECT, searchId);
		fr.setThreshold(new BigDecimal("0.65"));
		PinFilterResponse pf = PINFilterFactory.getDefaultPinFilter(searchId);
		FilterResponse legalFilter = LegalFilterFactory.getDefaultLegalFilter(searchId);

		// search by PIN
		if (hasPin()) {
			module = new TSServerInfoModule(serverInfo.getModule(TSServerInfo.ADV_SEARCH_MODULE_IDX));
			module.addExtraInformation(TSServerInfoConstants.EXTRA_PARAM_MODULE_DESCRIPTION,
					"Search by Account #");
			module.clearSaKeys();
			module.setSaKey(0, SearchAttributes.LD_PARCELNO_GENERIC_TR);
			module.addFilter(fr);
			modules.add(module);
		}

		// search by Address
		if (hasStreet()) {
			module = new TSServerInfoModule(serverInfo.getModule(TSServerInfo.ADV_SEARCH_MODULE_IDX));
			module.addExtraInformation(TSServerInfoConstants.EXTRA_PARAM_MODULE_DESCRIPTION, 
	    			"Search by Address");
			module.clearSaKeys();
			module.setSaKey(0, SearchAttributes.P_STREET_NO_NAME);
			
			FilterResponse nameFilterHybrid = NameFilterFactory.getHybridNameFilter(SearchAttributes.OWNER_OBJECT, searchId, module);
			module.addFilter(nameFilterHybrid);
			module.addFilter(fr);
			if (hasLegal()) {
				module.addFilter(legalFilter);
			}
			modules.add(module);
		}

		// search by Owner Name
		if (hasName()) {
			module = new TSServerInfoModule(serverInfo.getModule(TSServerInfo.ADV_SEARCH_MODULE_IDX));
			module.addExtraInformation(TSServerInfoConstants.EXTRA_PARAM_MODULE_DESCRIPTION, 
	    			"Search by Name");
			module.clearSaKeys();
			module.setSaObjKey(SearchAttributes.OWNER_OBJECT);
			module.addFilter(fr);

			FilterResponse nameFilterHybrid = NameFilterFactory.getHybridNameFilter(SearchAttributes.OWNER_OBJECT, searchId, module);
			module.addFilter(nameFilterHybrid);
			module.addFilter(pf);
			module.addFilter(legalFilter);
			module.setIteratorType(0, FunctionStatesIterator.ITERATOR_TYPE_LF_NAME_FAKE);
			ConfigurableNameIterator nameIterator = (ConfigurableNameIterator) ModuleStatesIteratorFactory
					.getConfigurableNameIterator(module, searchId, new String[] { "L F;;" });
			module.addIterator(nameIterator);
			modules.add(module);
		}

		serverInfo.setModulesForAutoSearch(modules);
	}

	protected void loadDataHash(HashMap<String, String> data) {
		if (data != null) {
			data.put("type", "CNTYTAX");
		}
	}

	protected boolean hasName() {
		return !StringUtils.isEmpty(getSearchAttribute(SearchAttributes.OWNER_LFM_NAME));
	}

	protected boolean hasLegal() {
		return GenericLegal.hasLegal(getSearchAttributes());
	}
}
