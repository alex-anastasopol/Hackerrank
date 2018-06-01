package ro.cst.tsearch.servers.types;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.apache.commons.lang.StringEscapeUtils;
import org.htmlparser.Node;
import org.htmlparser.filters.HasAttributeFilter;
import org.htmlparser.tags.TableRow;
import org.htmlparser.tags.TableTag;
import org.htmlparser.util.NodeList;

import com.stewart.ats.base.document.DocumentI;

import ro.cst.tsearch.Search;
import ro.cst.tsearch.bean.SearchAttributes;
import ro.cst.tsearch.exceptions.ServerResponseException;
import ro.cst.tsearch.extractor.xml.ResultMap;
import ro.cst.tsearch.parser.HtmlParser3;
import ro.cst.tsearch.search.filter.FilterResponse;
import ro.cst.tsearch.search.filter.RejectNonRealEstate;
import ro.cst.tsearch.servers.TSConnectionURL;
import ro.cst.tsearch.servers.info.TSServerInfo;
import ro.cst.tsearch.servers.info.TSServerInfoModule;
import ro.cst.tsearch.servers.response.LinkInPage;
import ro.cst.tsearch.servers.response.ParsedResponse;
import ro.cst.tsearch.servers.response.ServerResponse;
import ro.cst.tsearch.servers.response.OtherInformationSet.OtherInformationSetKey;
import ro.cst.tsearch.utils.StringUtils;

public class CASantaCruzTR extends TSServer {

	private static final long	serialVersionUID	= 7345962955249029771L;

	public CASantaCruzTR(long searchId) {
		super(searchId);
	}

	public CASantaCruzTR(String rsRequestSolverName, String rsSitePath, String rsServerID, String rsPrmNameLink, long searchId, int miServerID) {
		super(rsRequestSolverName, rsSitePath, rsServerID, rsPrmNameLink, searchId, miServerID);
	}

	@Override
	protected void ParseResponse(String sAction, ServerResponse serverResponse,
			int viParseID) throws ServerResponseException {

		String rsResponse = serverResponse.getResult();
		ParsedResponse parsedResponse = serverResponse.getParsedResponse();
		rsResponse = StringEscapeUtils.unescapeHtml(rsResponse).replaceAll("[^\\p{Alnum}\\p{Punct}\\s]", "");

		// treat not found message from site
		String message = ro.cst.tsearch.utils.StringUtils.extractParameter(rsResponse,
				"(?is)<span[^>]*>([^<]*is\\s+not\\s+a\\s+valid\\s+parcel\\s+number\\s+for\\s+Santa\\s+Cruz\\s+County)[^<]*?</span>");
		if (!message.isEmpty()) {
			parsedResponse.setError(message);
			return;
		}

		message = StringUtils.extractParameter(rsResponse,
				">(An\\s+error\\s+occurred\\s+while\\s+processing\\s+your\\s+request\\s*.\\s*)</h2>");
		if (!message.isEmpty()) {
			parsedResponse.setError(message);
			return;
		}

		switch (viParseID) {
		case ID_SEARCH_BY_PARCEL:
		case ID_DETAILS:
		case ID_SAVE_TO_TSD:

			// get details
			String details = getDetails(rsResponse);

			if (StringUtils.isNotEmpty(details)) {

				// get parcel ID
				HtmlParser3 htmlParser3 = new HtmlParser3(details);
				NodeList nodeList = htmlParser3.getNodeList();
				String parcelID = HtmlParser3.findNode(nodeList, "Parcel/Account #").toHtml().trim();
				parcelID = StringUtils.extractParameter(parcelID, "(?is)Parcel/Account\\s*#\\s*([\\d-\\s]+)$");

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
					msSaveToTSDResponce = details + CreateFileAlreadyInTSD();
					htmlParser3 = new HtmlParser3(details);
					Node mainDetailsRow = htmlParser3.getNodeById("mainDetailsRow");
					if (mainDetailsRow != null) {// for the main details doc
						details = "<table align=\"center\" >" + mainDetailsRow.toHtml() + "</table>";
					} else {// for an extra details doc
						details = "<table align=\"center\" >" + details + "</table>";
					}

					String filename = parcelID + ".html";
					smartParseDetails(serverResponse, details);
					parsedResponse.setResponse(details);
					msSaveToTSDFileName = filename;
					parsedResponse.setFileName(getServerTypeDirectory() + msSaveToTSDFileName);
				}
			}
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

	protected String getDetails(String rsResponse) {
		try {

			/* If from memory - use it as is */
			if (!org.apache.commons.lang.StringUtils.containsIgnoreCase(rsResponse, "<html")) {
				return rsResponse;
			}

			StringBuilder mainDetailsDocSB = new StringBuilder();
			String allHistoryDetailsL1 = "";

			String[] extraDetailsL1 = new String[16];// L1 or L2 means Level 1 or Level 2
			String[] extraDetailsL2 = new String[16];
			String[] extraDetailsTableTitle = new String[16];// extra details for current year will be present in history page as well, and shouldn't be added
																// again to details
			String[] detailsL1 = new String[32];
			String[] detailsL2 = new String[32];
			TableRow addrAndParcelRowT = null;
			String serverHomeLink = getDataSite().getServerHomeLink();

			if (StringUtils.isNotEmpty(rsResponse)) {

				String currentDetailsL1Link = StringUtils.extractParameter(rsResponse,
						"(?is)<form[^>]*\\baction=\"([^\"]*)\"[^>]*>\\s*<input[^>]*\\bvalue=\"Show\\s+Current\\s+Property\\s+Tax\\s+Bills\\s+Only\"[^>]*>");
				if (StringUtils.isNotEmpty(currentDetailsL1Link)) {// if on history page somehow, go to current year details page
					currentDetailsL1Link = serverHomeLink + currentDetailsL1Link;
					rsResponse = StringEscapeUtils.unescapeHtml(getLinkContents(currentDetailsL1Link));
				}

				HtmlParser3 htmlParser3 = new HtmlParser3(rsResponse);
				NodeList nodeList = htmlParser3.getNodeList();
				NodeList currentYearDetailsL1Tables = nodeList.extractAllNodesThatMatch(new HasAttributeFilter("class", "TaxBillContainer"), true);

				// get extra details(L1 and L2) - if any
				if (currentYearDetailsL1Tables.size() > 1) {
					for (int i = 1; i < currentYearDetailsL1Tables.size(); i++) {
						Node currentYearDetailsL1Table = currentYearDetailsL1Tables.elementAt(i);
						TableTag currentYearDetailsL1TableT = (TableTag) currentYearDetailsL1Table;
						if (currentYearDetailsL1TableT.getRowCount() > 0) {
							TableRow firstRow = currentYearDetailsL1TableT.getRows()[0];
							if (firstRow.getColumnCount() > 0) {// save table name check it later(to avoid adding it again from history page)
								extraDetailsTableTitle[i - 1] = firstRow.getColumns()[0].toPlainTextString().trim();
							}

							// get extra doc L1 details
							extraDetailsL1[i - 1] = currentYearDetailsL1TableT.toHtml().replaceAll("(?is)(<table\\b)([^>]*>)",
									"$1 border=\"1px\" $2");

							// get extra doc L2 details
							String extraDetailsL2Link = StringUtils.extractParameter(extraDetailsL1[i - 1],
									"(?is)<a\\b[^>]*\\bhref=\"([^\"]*)\"[^>]*>\\s*See\\s+Details\\s*</a>");

							if (StringUtils.isNotEmpty(extraDetailsL2Link)) {
								extraDetailsL2Link = serverHomeLink + extraDetailsL2Link.substring(1);
								extraDetailsL2[i - 1] = getLinkContents(extraDetailsL2Link);
								extraDetailsL2[i - 1] = StringEscapeUtils.unescapeHtml(extraDetailsL2[i - 1]);

								htmlParser3 = new HtmlParser3(extraDetailsL2[i - 1]);
								nodeList = htmlParser3.getNodeList();
								Node extraDetailsL2Table = HtmlParser3.findAncestorWithClass(HtmlParser3.findNode(nodeList, "Tax Rate Area"), TableTag.class);
								if (extraDetailsL2Table != null) {
									extraDetailsL2[i - 1] = extraDetailsL2Table.toHtml()
											.replaceAll("(?is)(<table\\b)([^>]*>)", "$1 border=\"1px\" $2");
								} else {
									extraDetailsL2[i - 1] = "";
								}
							}
						}
					}
				}

				// get main doc: bills' details for current year and also for all previous years; avoid extra docs
				String historyDetailsL1Link = StringUtils.extractParameter(rsResponse,
						"(?is)<form[^>]*\\baction=\"([^\"]*)\"[^>]*>\\s*<input[^>]*\\bvalue=\"Show\\s+Tax\\s+Bill\\s+History\"[^>]*>");

				if (StringUtils.isNotEmpty(historyDetailsL1Link)) {

					historyDetailsL1Link = serverHomeLink + historyDetailsL1Link.substring(1);
					allHistoryDetailsL1 = StringEscapeUtils.unescapeHtml(getLinkContents(historyDetailsL1Link))
							.replaceAll("[^\\p{Alnum}\\p{Punct}\\s]", "");

					htmlParser3 = new HtmlParser3(allHistoryDetailsL1);
					nodeList = htmlParser3.getNodeList();
					NodeList detailsL1Tables = nodeList.extractAllNodesThatMatch(new HasAttributeFilter("class", "TaxBillContainer"), true);

					// get address row
					Node addrAndParcelRow = HtmlParser3.findAncestorWithClass(HtmlParser3.findNode(nodeList, "Situs Address"), TableRow.class);
					if (addrAndParcelRow == null) {
						addrAndParcelRow = HtmlParser3.findAncestorWithClass(HtmlParser3.findNode(nodeList, "Mailing Address"), TableRow.class);
					}
					if (addrAndParcelRow != null) {
						addrAndParcelRowT = (TableRow) addrAndParcelRow;
						addrAndParcelRowT.setAttribute("width", "100%");
						addrAndParcelRowT.setAttribute("class", "situsAddressRow");
						addrAndParcelRowT.setAttribute("style", "font-weight:bold;");
					}

					// get details tables for current year and previous years(will be in main doc)
					if (detailsL1Tables.size() > 0) {
						for (int i = 0; i < detailsL1Tables.size(); i++) {
							TableTag detailsTableTL1 = (TableTag) detailsL1Tables.elementAt(i);
							boolean isNotExtraDoc = true;
							if (detailsTableTL1.getRowCount() > 0) {// check and don't add extra doc details to main details doc content(
								TableRow firstRow = detailsTableTL1.getRows()[0];
								if (firstRow.getColumnCount() > 0) {
									String testTableTitle = firstRow.getColumns()[0].toPlainTextString().trim();
									int j = 0;
									while (StringUtils.isNotEmpty(extraDetailsTableTitle[j]) && j < extraDetailsTableTitle.length) {
										if (extraDetailsTableTitle[j].equals(testTableTitle)) {
											isNotExtraDoc = false;
											break;
										}
										j++;
									}
								}
							}

							if (isNotExtraDoc) {// if it's not an extra doc on current year, get L2 content and add both L1 and L2 content to main details doc
								detailsL1[i] = detailsTableTL1.toHtml();

								String detailsL2Link = StringUtils.extractParameter(detailsL1[i],
										"(?is)<a\\b[^>]*\\bhref=\"([^\"]*)\"[^>]*>\\s*See\\s+Details\\s*</a>");
								if (StringUtils.isNotEmpty(detailsL2Link)) {
									detailsL2Link = serverHomeLink + detailsL2Link.substring(1);

									detailsL2[i] = getLinkContents(detailsL2Link);
									detailsL2[i] = StringEscapeUtils.unescapeHtml(detailsL2[i]);

									htmlParser3 = new HtmlParser3(detailsL2[i]);
									nodeList = htmlParser3.getNodeList();
									Node detailsL2Table = HtmlParser3
											.findAncestorWithClass(HtmlParser3.findNode(nodeList, "Tax Rate Area"), TableTag.class);
									if (detailsL2Table != null) {
										detailsL2[i] = detailsL2Table.toHtml();
									} else {
										detailsL2[i] = "";
									}
								}

								if (StringUtils.isNotEmpty(detailsL1[i])) {
									mainDetailsDocSB.append("<tr id=\"details" + i + "L1\"><td>"
											+ detailsL1[i].replaceAll("(?is)(<table\\b)([^>]*>)", "$1 border=\"1px\" $2") + "</td></tr>");
								}
								if (StringUtils.isNotEmpty(detailsL2[i])) {
									mainDetailsDocSB.append("<tr id=\"details" + i + "L2\"><td>"
											+ detailsL2[i].replaceAll("(?is)(<table\\b)([^>]*>)", "$1 border=\"1px\" $2") + "</td></tr>");
								}
							}
						}
					}
				}
			}

			String allDetails = "";

			if (StringUtils.isNotEmpty(mainDetailsDocSB.toString())) {

				// add address row only if other details are found
				String addressRow = "";
				if (addrAndParcelRowT != null) {
					addressRow = addrAndParcelRowT.toHtml()
							.replaceAll("(?is)<td[^>]*>\\s*</td>", "")
							.replaceFirst("(?is)</td>\\s*<td[^>]*>", "")
							.replaceFirst("(?is)(<td[^>]*>)([^<]*)(</td>)", "$1<h3>$2</h3>$3");
				}

				allDetails = "<tr id=\"mainDetailsRow\"><td><table>" + addressRow + mainDetailsDocSB.toString()
						+ "</table></td></tr>";

				// add extra details content, if any
				int i = 0;
				StringBuilder extraDetailsDocsSB = new StringBuilder();
				while (StringUtils.isNotEmpty(extraDetailsL1[i])) {
					if (i == 0) {
						extraDetailsDocsSB.append("<tr id=\"extraDetailsTitle\"><td><h3>Extra tax bills:</h3></td></tr>");
					}

					extraDetailsDocsSB.append("<tr class=\"extraDetailsRows\"><td><table>" + addressRow);
					extraDetailsDocsSB.append("<tr class=\"extraDetailsL1\"><td>" + extraDetailsL1[i] + "</td></tr>");

					if (StringUtils.isNotEmpty(extraDetailsL2[i])) {
						extraDetailsDocsSB.append("<tr class=\"extraDetailsL2\"><td>" + extraDetailsL2[i] + "</td></tr>");
					}

					extraDetailsDocsSB.append("</table></td></tr>");
					i++;
				}

				if (i > 0) {// if there are any extra details docs
					allDetails += extraDetailsDocsSB.toString();
				}

				allDetails = allDetails.replaceAll("(?is)</?span\\b[^>]*>", "")
						.replaceAll("(?is)<a\\b[^>]*>[^<]*</a>", "")
						.replaceAll("(?is)<(input|link)\\b[^>]*>", "")
						.replaceAll("(?is)<tr[^>]*>(\\s*<td[^>]*>\\s*</td>)+\\s*</tr>", "")
						.replaceFirst("(?is)<a\\b[^>]*>\\s*See\\s+Details\\s*</a>", "")
						.replaceAll("(?is)(<td[^>]*>)([^<]*\\s+Tax\\s+Bill\\s*)(</td>)", "$1<h3>$2<h3>$3")
						.replaceAll("(?is)(<(?:td|th|table)\\b[^>]*)\\bstyle=\"[^\"]*\"([^>]*>)", "$1 $2")
						.replaceAll("(?is)(<(?:table)\\b)([^>]*>)", "$1 width=\"100%\" $2")
						.replaceAll("(?is)<script[^>]*>.*?</script>", "")
						.replaceAll("[^\\p{Alnum}\\p{Punct}\\s]", "");

				allDetails = "<table id=\"allDetails\" align=\"center\" style=\"min-width:700px;\">" + allDetails + "</table>";
			}

			return allDetails;

		} catch (Exception e) {
			logger.error("Error while getting details", e);
		}
		return null;
	}

	@Override
	protected Object parseAndFillResultMap(ServerResponse response, String detailsHtml, ResultMap resultMap) {
		resultMap.put(OtherInformationSetKey.SRC_TYPE.getKeyName(), getDataSite().getSiteTypeAbrev());
		ro.cst.tsearch.servers.functions.CASantaCruzTR.parseAndFillResultMap(detailsHtml, resultMap, getSearch(), getDataSite());
		return null;
	}

	@Override
	protected void setModulesForAutoSearch(TSServerInfo serverInfo) {
		List<TSServerInfoModule> moduleList = new ArrayList<TSServerInfoModule>();

		TSServerInfoModule module = null;
		Search search = getSearch();
		int searchType = search.getSearchType();
		FilterResponse rejectNonRealEstateFilter = new RejectNonRealEstate(SearchAttributes.OWNER_OBJECT, searchId);
		rejectNonRealEstateFilter.setThreshold(new BigDecimal("0.65"));

		if (searchType == Search.AUTOMATIC_SEARCH) {
			if (hasPin()) {
				module = new TSServerInfoModule(serverInfo.getModule(TSServerInfo.PARCEL_ID_MODULE_IDX));
				module.clearSaKeys();
				module.setSaKey(0, SearchAttributes.LD_PARCELNO_GENERIC_TR);
				module.addValidator(rejectNonRealEstateFilter.getValidator());
				moduleList.add(module);
			}
			serverInfo.setModulesForAutoSearch(moduleList);
		}
	}

	@Override
	public void addAdditionalDocuments(DocumentI doc, ServerResponse response) {
		try {
			String details = response.getParsedResponse().getResponse();
			HtmlParser3 htmlParser3 = new HtmlParser3(details);
			Node allDetails = htmlParser3.getNodeById("allDetails");
			if (allDetails != null) {
				NodeList extraDetailsNodeList = allDetails.getChildren().extractAllNodesThatMatch(new HasAttributeFilter("class", "extraDetailsRows"), true);
				int noOfExtraDocs = extraDetailsNodeList.size();

				if (noOfExtraDocs > 0) {
					String[] extraDetailsHtml = new String[noOfExtraDocs];
					for (int i = 0; i < noOfExtraDocs; i++) {

						extraDetailsHtml[i] = extraDetailsNodeList.elementAt(i).toHtml();
						ServerResponse Response = new ServerResponse();
						Response.setResult(extraDetailsHtml[i]);
						String sAction = "/saveExtraDocs";
						Response.getParsedResponse().setAttribute(ParsedResponse.SKIP_BOOTSTRAP, true);
						super.solveHtmlResponse(sAction, ID_SAVE_TO_TSD, "SaveToTSD", Response, extraDetailsHtml[i]);
					}
				}
			}
		} catch (Exception e) {
			logger.error("ERROR while saving additional docs", e);
		}
	}
}
