package ro.cst.tsearch.servers.types;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Vector;
import java.util.regex.Matcher;

import org.apache.commons.lang.StringEscapeUtils;
import org.apache.commons.lang.StringUtils;
import org.htmlparser.Node;
import org.htmlparser.filters.HasAttributeFilter;
import org.htmlparser.filters.TagNameFilter;
import org.htmlparser.tags.LinkTag;
import org.htmlparser.tags.TableRow;
import org.htmlparser.tags.TableTag;
import org.htmlparser.util.NodeList;

import com.stewart.ats.base.document.DocumentI;
import com.stewart.ats.base.document.TaxDocumentI;

import ro.cst.tsearch.bean.SearchAttributes;
import ro.cst.tsearch.exceptions.ServerResponseException;
import ro.cst.tsearch.extractor.xml.Bridge;
import ro.cst.tsearch.extractor.xml.ResultMap;
import ro.cst.tsearch.parser.HtmlParser3;
import ro.cst.tsearch.search.FunctionStatesIterator;
import ro.cst.tsearch.search.filter.FilterResponse;
import ro.cst.tsearch.search.filter.newfilters.name.NameFilterFactory;
import ro.cst.tsearch.search.iterator.ModuleStatesIteratorFactory;
import ro.cst.tsearch.search.module.ConfigurableNameIterator;
import ro.cst.tsearch.servers.TSConnectionURL;
import ro.cst.tsearch.servers.info.TSServerInfo;
import ro.cst.tsearch.servers.info.TSServerInfoModule;
import ro.cst.tsearch.servers.response.LinkInPage;
import ro.cst.tsearch.servers.response.OtherInformationSet.OtherInformationSetKey;
import ro.cst.tsearch.servers.response.ParsedResponse;
import ro.cst.tsearch.servers.response.PropertyIdentificationSet.PropertyIdentificationSetKey;
import ro.cst.tsearch.servers.response.ServerResponse;
import ro.cst.tsearch.utils.RegExUtils;
import ro.cst.tsearch.utils.Tidy;

public class CASanLuisObispoTR extends TSServer {

	private static final long	serialVersionUID	= 3226270263434801479L;

	public CASanLuisObispoTR(long searchId) {
		super(searchId);
	}

	public CASanLuisObispoTR(String rsRequestSolverName, String rsSitePath, String rsServerID, String rsPrmNameLink, long searchId, int miServerID) {
		super(rsRequestSolverName, rsSitePath, rsServerID, rsPrmNameLink, searchId, miServerID);
	}

	@Override
	protected void ParseResponse(String sAction, ServerResponse serverResponse,
			int viParseID) throws ServerResponseException {

		String rsResponse = serverResponse.getResult().replaceAll("&nbsp;", " ");
		ParsedResponse parsedResponse = serverResponse.getParsedResponse();
		rsResponse = StringEscapeUtils.unescapeHtml(rsResponse);
		rsResponse = rsResponse.replaceAll("[^\\p{Alnum}\\p{Punct}\\s]", "");

		// treat not found messages
		String message = ro.cst.tsearch.utils.StringUtils
				.extractParameter(rsResponse, "(?is)<span[^>]*>\\s*(No\\s+matches\\s+found\\s+please\\s+try\\s+again\\s*\\.?)\\s*</span>");
		if (message.isEmpty()) {
			message = ro.cst.tsearch.utils.StringUtils
					.extractParameter(rsResponse,
							"(?is)<span\\b[^>]*>\\s*(Bill\\s+number\\s+not\\s+valid\\s+for\\s+this\\s+system\\s*\\.?)\\s*Please\\s+call\\s+"
									+ "the\\s+Tax\\s+Collector's\\s+Office");
		}

		if (message.isEmpty()) {
			message = ro.cst.tsearch.utils.StringUtils
					.extractParameter(rsResponse, "(?is)<span id=\"Label1\">(Enter the parcel's nine digit Assessment Number, "
							+ "three digits per box. Please exclude any separators such as commas, spaces and hyphens.)</span>");
			if (!message.isEmpty()) {// if the search page is returned, no results found !!
				return;
			}
		}

		if (!message.isEmpty()) {
			parsedResponse.setError(message);
			return;
		}

		switch (viParseID) {
		case ID_SEARCH_BY_MODULE38:// Mailing Address/Name Search

			StringBuilder outputTable = new StringBuilder();
			Collection<ParsedResponse> smartParsedResponses = smartParseIntermediary(serverResponse, rsResponse, outputTable);

			if (smartParsedResponses.size() > 0) {
				parsedResponse.setResultRows(new Vector<ParsedResponse>(smartParsedResponses));
				parsedResponse.setOnlyResponse(outputTable.toString());
				parsedResponse.setAttribute(ParsedResponse.SERVER_ROW_RESPONSE, outputTable.toString());
			}
			break;

		case ID_SEARCH_BY_PARCEL:
		case ID_DETAILS:
		case ID_SAVE_TO_TSD:

			// get bill number
			String parcelId = "";
			String year = "";
			boolean isMainDetailsRow = true;
			String billNumberNodeHtml = "";

			HtmlParser3 htmlParser3 = new HtmlParser3(rsResponse);
			NodeList nodeList = htmlParser3.getNodeList();
			Node billNumberNode = htmlParser3.getNodeById("APN");

			if (billNumberNode != null) {
				parcelId = billNumberNode.toPlainTextString().trim();
				Node yearNode = htmlParser3.getNodeById("Links1_DataYr");
				if (yearNode != null) {
					year = RegExUtils.getFirstMatch("^\\s*(\\d{4})", yearNode.toPlainTextString(), 1);
				}
			}

			billNumberNode = htmlParser3.getNodeById("mainDetailsRow");
			if (billNumberNode == null) {
				isMainDetailsRow = false;
				nodeList = nodeList.extractAllNodesThatMatch(new HasAttributeFilter("class", "extraDetailsRows"), true);
				if (nodeList.size() > 0) {
					billNumberNode = nodeList.elementAt(0);
				}
			}

			if (billNumberNode != null && parcelId.isEmpty()) {
				billNumberNodeHtml = billNumberNode.toHtml();
				parcelId = RegExUtils.getFirstMatch(ro.cst.tsearch.servers.functions.CASanLuisObispoTR.yearAndBillPattern,
						billNumberNodeHtml, 2);
				year = RegExUtils.getFirstMatch(ro.cst.tsearch.servers.functions.CASanLuisObispoTR.yearAndBillPattern,
						billNumberNodeHtml, 1);
			}

			// get details
			String details = getDetails(rsResponse, parcelId);
			if (StringUtils.isEmpty(details)) {
				parsedResponse.setError("Site error.");
				return;
			}

			if (viParseID != ID_SAVE_TO_TSD) {
				String originalLink = sAction.replace("?", "&") + "&" + serverResponse.getRawQuerry();
				String sSave2TSDLink = getLinkPrefix(TSConnectionURL.idPOST) + originalLink;

				HashMap<String, String> data = new HashMap<String, String>();
				loadDataHash(data);
				data.put("year", year);

				if (isInstrumentSaved(parcelId, null, data)) {
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
				if (isMainDetailsRow) {// for the main details doc
					details = billNumberNodeHtml;
				}

				details = "<table align=\"center\" >" + details + "</table>";
				String filename = parcelId + ".html";
				smartParseDetails(serverResponse, details);
				parsedResponse.setResponse(details);
				msSaveToTSDFileName = filename;
				parsedResponse.setFileName(getServerTypeDirectory() + msSaveToTSDFileName);
			}

			break;

		case ID_GET_LINK:
			ParseResponse(sAction, serverResponse, ID_DETAILS);
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

	protected String getDetails(String rsResponse, String parcelID) {
		try {
			/* If from memory - use it as is */
			if (!org.apache.commons.lang.StringUtils.containsIgnoreCase(rsResponse, "<html")) {
				return rsResponse;
			}

			int detailsIndex = 0;
			String[] detailsL1 = new String[16];
			String[] detailsL2 = new String[16];
			StringBuilder tmpSB = new StringBuilder();
			String allDetails = "";
			String mainDetailsRow = "";
			String extraDetailsRows = "";

			rsResponse = Tidy.tidyParse(rsResponse, null);
			HtmlParser3 htmlParser3 = new HtmlParser3(rsResponse);
			NodeList nodeList = htmlParser3.getNodeList();
			NodeList nodeListL1 = new NodeList();
			String dataSiteLink = getDataSite().getLink();

			// get L1 details links
			NodeList detailsL1LinkNodes = nodeList.extractAllNodesThatMatch(new TagNameFilter("input"), true)
					.extractAllNodesThatMatch(new HasAttributeFilter("value", "Bill Detail"));
			Node node = null;
			String billNumber = "";
			if (detailsL1LinkNodes.size() > 0) {
				for (int i = 0; i < detailsL1LinkNodes.size(); i++) {// get all details L1 and L2 - of main and supplemental results(if present)
					node = detailsL1LinkNodes.elementAt(i);
					String nodeHtml = node.toHtml();
					String tempLink = "";
					boolean isMainDetailsLink = false;
					if (!RegExUtils.getFirstMatch("(?is)(SecTaxBills_RadListView1_ctrl0_btnBillDetail)", nodeHtml, 1).isEmpty()) {
						isMainDetailsLink = true;
					}

					// get the assessment no
					TableRow parentRow = HtmlParser3.getFirstParentTag(node, TableRow.class);
					if (parentRow != null) {

						billNumber = RegExUtils.getFirstMatch("(?is)^\\s*[\\d/]+\\s+([\\d,-]+)\\s*$",
								parentRow.toPlainTextString(), 1);
					}

					if (StringUtils.isNotEmpty(billNumber)) {
						if (isMainDetailsLink) {
							tempLink = "Detail.aspx?lblBillnum=" + billNumber + "&csus=0";
						} else {
							tempLink = "detailsupp.aspx?lblBillnum=" + billNumber + "&csus=0";
						}
						String detailsL1Content = getLinkContents(dataSiteLink + tempLink);

						detailsL1Content = Tidy.tidyParse(detailsL1Content, null);
						htmlParser3 = new HtmlParser3(detailsL1Content);
						nodeListL1 = htmlParser3.getNodeList();
						Node form = htmlParser3.getNodeById("Form1");
						if (form != null) {

							// get details L1
							detailsL1[detailsIndex] = "<tr class=\"detailsL1\"><td>" + form.toHtml().replaceFirst("(?is)(<table)[^>]*>", "$1>")
									.replaceAll("(?is)\\s*</?form\\b[^>]*>\\s*", "") + "</td></tr>";
							detailsL2[detailsIndex] = "";

							String jsDoOpenLink = RegExUtils.getFirstMatch("(?is)<script[^>]*>.*?function\\s+doOpen.*?window\\.open"
									+ "\\(\"([^\"]*)\".*?</script>", detailsL1Content, 1);

							// get details L2
							if (nodeListL1.size() > 0) {
								NodeList detailsL2LinkNodes = nodeListL1.extractAllNodesThatMatch(new TagNameFilter("input"), true);
								if (detailsL2LinkNodes.size() > 0) {
									for (int j = 0; j < detailsL2LinkNodes.size(); j++) {
										node = detailsL2LinkNodes.elementAt(j);
										nodeHtml = node.toHtml();
										if (StringUtils.containsIgnoreCase(nodeHtml, "View Bill")) {
											String detailsL2Link = dataSiteLink + jsDoOpenLink // "secbill.aspx?billId=" or supbill.aspx...
													+ RegExUtils.getFirstMatch("(?is)doOpen\\(\"([^\"]*)\"\\)", node.toHtml(), 1);
											String detailsL2Content = getLinkContents(detailsL2Link);
											detailsL2Content = Tidy.tidyParse(detailsL2Content, null);
											htmlParser3 = new HtmlParser3(detailsL2Content);
											form = htmlParser3.getNodeById("Form1");
											if (form != null) {
												NodeList nodeListL2 = form.getChildren().extractAllNodesThatMatch(new TagNameFilter("table"));

												for (int k = 0; k < nodeListL2.size() - 1; k++) {// omit last table, it contains unneeded text
													Node table = nodeListL2.elementAt(k);
													if (table instanceof TableTag) {
														tmpSB.append("<tr><td>" + table.toHtml() + "</td></tr>");
													}
												}

												if (StringUtils.isNotEmpty(tmpSB.toString())) {
													detailsL2[detailsIndex] = "<tr class=\"detailsL2\"><td><table align=\"center\">"
															+ tmpSB.toString()
															+ "</table></td></tr>";
													tmpSB = new StringBuilder();
												}
											}
											break;
										}
									}
								}
							}
							detailsIndex++;
						}
					}
				}
			}

			if (StringUtils.isNotEmpty(detailsL1[0])) {
				// add main details
				mainDetailsRow = "<tr id=\"mainDetailsRow\"><td><table border=\"1\">" + detailsL1[0] + detailsL2[0] + "</table></td></tr>";
				for (int i = 1; i < detailsIndex; i++) {
					if (i > 0 && !RegExUtils.getFirstMatch("(?is)<b[^>]*>\\s*(Prior\\s+Years\\s+Delinquent\\s+Taxes)\\s*</b>", detailsL1[i], 1).isEmpty()) {
						// if prior delinquent doc, add it to main row !!
						String priorDelinquentContent = "<div class=\"priorDelinquent\">" + detailsL1[i]
								.replaceAll("(?is)</?a\\b[^>]*>", "")
								.replaceAll("(?is)(<td\\b[^>]*>)\\s*(?:Details|Payment\\s+Options)\\s*(</td>)", "$1$2")
								.replaceFirst("(?is)<table[^>]*id=\"tblFooter\"[^>]*>.*?</table>\\s*<table[^>]*>.*?</table>", "")
								+ "</div>";
						mainDetailsRow = mainDetailsRow.replaceFirst("(?is)(</table></td></tr>)$",
								Matcher.quoteReplacement(priorDelinquentContent) + "$1");
					} else {
						extraDetailsRows += "<tr class=\"extraDetailsRows\"><td><table border=\"1\">" + detailsL1[i] + detailsL2[i] + "</table></td></tr>";
					}
				}
			}

			if (!mainDetailsRow.isEmpty()) {
				allDetails = "<table id=\"allDetails\" align=\"center\">"
						+ mainDetailsRow + extraDetailsRows + "</table>";

				allDetails = allDetails
						.replaceAll("&nbsp;", " ")
						.replaceAll("(?is)<font[^>]*>(\\s*The\\s+Tax\\s+Collector\\s+is\\s+not\\s+responsible|"
								+ "property\\s+on\\s+which\\s+you\\s+are|We\\s+accept\\s+as\\s+negotiable).*?</font>", "")
						.replaceAll("(?is)<b\\b[^>]*>\\s*SEE\\s+INSTRUCTIONS\\s+FOR\\s+IMPORTANT[^<]*</b>", "")
						.replaceAll("(?is)(<td[^>]*>)\\s*(?:HOME\\s+BANKING\\s+PLEASE\\s+ENTER\\s+BILL|"
								+ "Return\\s+this\\s+stub\\s+with\\s+payment).*?(</td>)", "$1$2")
						.replaceAll("(?is)\\s*<script\\b[^>]*>.*?</script>\\s*", "")
						.replaceAll("(?is)(\\s*\\|)?\\s*<a\\b[^>]*>.*?</a>(\\s*\\|)*\\s*", "")
						.replaceAll("(?is)(<[^>]*)\\bbgcolor=\"[^\"]*\"", "$1")
						.replaceAll("(?is)\\s*</?(head|body|html|meta|link|title|!DOCTYPE|font|input|img|hr|br)\\b[^>]*>\\s*", "")
						.replaceAll("(?is)\\s*(<br\\s*/?>\\s*)*<table\\b[^>]*id=\"Supplemental2_nodata\"[^>]*>.*?</table>(\\s*<br\\b[^>]*>)*\\s*", "")
						.replaceAll("(?is)<td\\b[^>]*>\\s*[^<]*<div[^>]*\\bid=\"Header1_DIV1\"[^>]*>.*?</td>", "");

				return StringEscapeUtils.unescapeHtml(allDetails)
						.replaceAll("[^\\p{Alnum}\\p{Punct}\\s]", "");
			}
		} catch (Exception e) {
			logger.error("Error while getting details", e);
		}
		return null;
	}

	@Override
	protected Object parseAndFillResultMap(ServerResponse response, String detailsHtml, ResultMap resultMap) {
		resultMap.put(OtherInformationSetKey.SRC_TYPE.getKeyName(), getDataSite().getSiteTypeAbrev());
		ro.cst.tsearch.servers.functions.CASanLuisObispoTR.parseAndFillResultMap(detailsHtml, resultMap, searchId);
		return null;
	}

	@Override
	public Collection<ParsedResponse> smartParseIntermediary(
			ServerResponse response, String htmlTable, StringBuilder outputTable) {
		Vector<ParsedResponse> intermediaryResponse = new Vector<ParsedResponse>();

		try {
			String parcel = "";
			String previousParcel = "";

			ParsedResponse parsedResponse = response.getParsedResponse();
			htmlTable = Tidy.tidyParse(htmlTable, null);
			HtmlParser3 htmlParser3 = new HtmlParser3(htmlTable);
			Node form = htmlParser3.getNodeById("Form1");
			if (form != null) {
				NodeList intermediaryResults = form.getChildren().extractAllNodesThatMatch(new TagNameFilter("table"), true);
				if (intermediaryResults.size() > 0) {
					String header = "<table style=\"min-width:600px;\" align=\"center\" border=\"1\" cellspacing=\"0\" "
							+ "cellpadding=\"2\"><tr><td>Assessment #</td><td>Assessee</td></tr>";
					outputTable.append(header);

					for (int i = 2; i < intermediaryResults.size(); i++) {
						TableTag tableResult = (TableTag) intermediaryResults.elementAt(i);
						TableRow tableRow = tableResult.getRows()[0];

						if (tableRow != null && tableRow.getColumnCount() >= 2) {
							Node linkNode = (Node) tableRow.getColumns()[0];
							if (linkNode != null) {
								linkNode = HtmlParser3.getFirstTag(linkNode.getChildren(), LinkTag.class, true);
								LinkTag linkTag = (LinkTag) linkNode;

								String link = CreatePartialLink(TSConnectionURL.idGET)
										+ linkTag.getLink();
								linkTag.setLink(link);
								String fullRow = tableRow.toHtml();
								outputTable.append(fullRow);

								ResultMap resultMap = ro.cst.tsearch.servers.functions.CASanLuisObispoTR.parseIntermediaryRow(tableRow);
								resultMap.put(OtherInformationSetKey.SRC_TYPE.getKeyName(), getDataSite().getSiteTypeAbrev());

								// don't get duplicate intermediary rows !!
								parcel = StringUtils.defaultString((String) resultMap.get(PropertyIdentificationSetKey.PARCEL_ID.getKeyName()));
								if (parcel.equals(previousParcel)) {
									continue;
								}
								previousParcel = parcel;

								ParsedResponse currentResponse = new ParsedResponse();
								currentResponse.setAttribute(ParsedResponse.SERVER_ROW_RESPONSE, fullRow);
								currentResponse.setOnlyResponse(fullRow);
								currentResponse.setPageLink(new LinkInPage(link, link, TSServer.REQUEST_SAVE_TO_TSD));

								Bridge bridge = new Bridge(currentResponse, resultMap, searchId);
								DocumentI document = (TaxDocumentI) bridge.importData();
								currentResponse.setDocument(document);

								intermediaryResponse.add(currentResponse);
							}
						}

					}

					String footer = "</table>";
					parsedResponse.setHeader(header);
					parsedResponse.setFooter(footer);
					outputTable.append(footer);
				}
			}
		} catch (Throwable t) {
			logger.error("Error while parsing intermediary data", t);
		}
		return intermediaryResponse;
	}

	@Override
	protected void setModulesForAutoSearch(TSServerInfo serverInfo) {
		List<TSServerInfoModule> moduleList = new ArrayList<TSServerInfoModule>();
		TSServerInfoModule module = null;

		// pin
		if (hasPin()) {
			module = new TSServerInfoModule(serverInfo.getModule(TSServerInfo.PARCEL_ID_MODULE_IDX));
			module.clearSaKeys();
			module.setSaKey(0, SearchAttributes.LD_PARCELNO);
			moduleList.add(module);
		}

		// owner
		if (hasOwner()) {
			FilterResponse nameFilterHybridDoNotSkipUnique = NameFilterFactory.getHybridNameFilter(SearchAttributes.OWNER_OBJECT, searchId, module);
			nameFilterHybridDoNotSkipUnique.setSkipUnique(false);

			module = new TSServerInfoModule(serverInfo.getModule(TSServerInfo.ADV_SEARCH_MODULE_IDX));
			module.clearSaKeys();

			module.setSaObjKey(SearchAttributes.OWNER_OBJECT);
			module.setIteratorType(6, FunctionStatesIterator.ITERATOR_TYPE_LF_NAME_FAKE);
			module.addFilter(nameFilterHybridDoNotSkipUnique);
			module.addIterator((ConfigurableNameIterator) ModuleStatesIteratorFactory.getConfigurableNameIterator(module, searchId, new String[] { "L;F;" }));
			moduleList.add(module);
		}

		serverInfo.setModulesForAutoSearch(moduleList);
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
