package ro.cst.tsearch.servers.types;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Vector;

import org.apache.commons.lang.StringEscapeUtils;
import org.htmlparser.Node;
import org.htmlparser.filters.HasAttributeFilter;
import org.htmlparser.tags.FormTag;
import org.htmlparser.tags.InputTag;
import org.htmlparser.tags.LinkTag;
import org.htmlparser.tags.TableColumn;
import org.htmlparser.tags.TableRow;
import org.htmlparser.tags.TableTag;
import org.htmlparser.util.NodeList;

import com.stewart.ats.base.document.DocumentI;
import com.stewart.ats.base.document.TaxDocumentI;

import ro.cst.tsearch.Search;
import ro.cst.tsearch.bean.SearchAttributes;
import ro.cst.tsearch.connection.http3.HttpSite3;
import ro.cst.tsearch.exceptions.ServerResponseException;
import ro.cst.tsearch.extractor.xml.Bridge;
import ro.cst.tsearch.extractor.xml.ResultMap;
import ro.cst.tsearch.parser.HtmlParser3;
import ro.cst.tsearch.search.filter.FilterResponse;
import ro.cst.tsearch.search.filter.newfilters.address.AddressFilterFactory;
import ro.cst.tsearch.search.filter.newfilters.address.CityFilterFactory;
import ro.cst.tsearch.servers.TSConnectionURL;
import ro.cst.tsearch.servers.info.TSServerInfo;
import ro.cst.tsearch.servers.info.TSServerInfoModule;
import ro.cst.tsearch.servers.response.LinkInPage;
import ro.cst.tsearch.servers.response.ParsedResponse;
import ro.cst.tsearch.servers.response.ServerResponse;
import ro.cst.tsearch.servers.response.OtherInformationSet.OtherInformationSetKey;
import ro.cst.tsearch.utils.StringUtils;
import ro.cst.tsearch.utils.Tidy;

public class CAKernTR extends TSServer {
	private static final long		serialVersionUID	= 445321998774294937L;
	private static int				seq					= 0;
	private static final String		VIEWSTATE			= "__VIEWSTATE";
	private static final String[]	ADD_PARAM_NAMES		= { VIEWSTATE };
	public static final String		EVENTTARGET			= "__EVENTTARGET";
	public static final String		FORM_NAME			= "aspnetForm";

	public CAKernTR(String rsRequestSolverName, String rsSitePath, String rsServerID, String rsPrmNameLink, long searchId, int miServerID) {
		super(rsRequestSolverName, rsSitePath, rsServerID, rsPrmNameLink, searchId, miServerID);
	}

	public CAKernTR(long searchId) {
		super(searchId);
	}

	@Override
	protected void ParseResponse(String sAction, ServerResponse serverResponse,
			int viParseID) throws ServerResponseException {
		String rsResponse = serverResponse.getResult().replaceAll("[^\\p{Alnum}\\p{Punct}\\s]", "");
		ParsedResponse parsedResponse = serverResponse.getParsedResponse();

		String message = StringUtils.extractParameter(rsResponse, "(?is)(Welcome\\s+to\\s+the\\s+Kern\\s+County\\s+Treasurer\\s+and\\s+Tax\\s*.)");
		if (!message.isEmpty()) {
			parsedResponse.setError("No results found !");
			return;
		}

		message = StringUtils.extractParameter(rsResponse, "(?is)(An\\s+error\\s+occurred\\s+during\\s+processing.)");
		if (!message.isEmpty()) {// should never happen, occurs if requests are not done right
			parsedResponse.setError(message);
			return;
		}

		switch (viParseID) {

		case ID_SEARCH_BY_ADDRESS:

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

			message = StringUtils.extractParameter(rsResponse,
					"(?is)(No\\s+matches\\s+found\\s*.)");
			if (!message.isEmpty()) {
				parsedResponse.setError(message);
				return;
			}

			HtmlParser3 htmlParser3 = new HtmlParser3(rsResponse);
			Node detailsL1Node = htmlParser3.getNodeById("Table7");
			Node intermediariesL2 = htmlParser3.getNodeById("ctl00_ContentPlaceHolder1_tblBills");
			if (detailsL1Node == null && intermediariesL2 == null) {
				// interm L2 node is null and details L1 node is null, we're on interm L1(some searches by tax/bill/file no bring L1 interm)
				ParseResponse(sAction, serverResponse, ID_SEARCH_BY_ADDRESS);
				break;
			}

			String details = getDetails(rsResponse);
			if (StringUtils.isNotEmpty(details)) {

				// get parcelId
				String parcelId = "";
				htmlParser3 = new HtmlParser3(details);
				boolean isMainDoc = false;
				Node parcelIdNode = htmlParser3.getNodeById("mainDetailsRow");

				if (parcelIdNode != null) {// if main doc - pid is ATN
					isMainDoc = true;

					parcelIdNode = htmlParser3.getNodeById("ctl00_ContentPlaceHolder1_lblAtnOrFileType");
					if (parcelIdNode != null) {
						parcelIdNode = HtmlParser3.getFirstParentTag(parcelIdNode, TableRow.class);
						if (parcelIdNode != null) {
							NodeList parcelIdNodes = HtmlParser3.getTag(parcelIdNode.getChildren(), TableColumn.class, true);

							if (parcelIdNodes.size() > 1) {
								parcelIdNode = parcelIdNodes.elementAt(1);
								if (parcelIdNode != null) {
									parcelId = parcelIdNode.toPlainTextString().replaceAll("[^\\d-]", "");
								}
							}
						}
					}
				} else {// if extra doc - pid is bill no
					parcelIdNode = htmlParser3.getNodeById("ctl00_ContentPlaceHolder1_lblBillNumber");
					if (parcelIdNode != null) {
						parcelId = parcelIdNode.toPlainTextString().replaceAll("[^\\d-]", "");
					}
				}
				if (viParseID != ID_SAVE_TO_TSD) {

					String query = serverResponse.getRawQuerry();
					String originalLink = (sAction.replace("?", "&") + "&" + query);

					String sSave2TSDLink = getLinkPrefix(TSConnectionURL.idPOST) + originalLink;

					HashMap<String, String> data = new HashMap<String, String>();
					loadDataHash(data);

					if (isInstrumentSaved(parcelId, null, data)) {
						details += CreateFileAlreadyInTSD();
						if (mSearch.getSearchType() == Search.AUTOMATIC_SEARCH) {// if doc already saved and auto search add it in memory
							// so that ATS won't redo all the HTTP requests to get the doc again
							mSearch.addInMemoryDoc(sSave2TSDLink, details);
						}
					} else {
						mSearch.addInMemoryDoc(sSave2TSDLink, details);
						details = addSaveToTsdButton(details, sSave2TSDLink, ID_DETAILS);
					}

					parsedResponse.setPageLink(new LinkInPage(sSave2TSDLink, originalLink, TSServer.REQUEST_SAVE_TO_TSD));
					parsedResponse.setResponse(details);

				} else {
					if (isMainDoc) {// hide extra docs
						details = details.replaceAll("(<tr class=\"extraDetailsRows\")", "$1 hidden=\"true\"");
					} else {// unhide extra docs
						details = details.replaceAll("(<tr class=\"extraDetailsRows\") hidden=\"true\"", "$1");
					}

					String filename = parcelId + ".html";
					smartParseDetails(serverResponse, details);

					msSaveToTSDFileName = filename;
					parsedResponse.setFileName(getServerTypeDirectory() + msSaveToTSDFileName);
					parsedResponse.setResponse(details);

					msSaveToTSDResponce = details + CreateFileAlreadyInTSD();
				}
			}

			break;

		case ID_GET_LINK:

			if (sAction.contains("/AddressSummary.aspx")) {
				ParseResponse(sAction, serverResponse, ID_SEARCH_BY_ADDRESS);
			} else {
				ParseResponse(sAction, serverResponse, ID_DETAILS);
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

			int numberOfDocs = 0;
			StringBuilder detailsSB = new StringBuilder();
			String detailsL1Content[] = new String[20];
			NodeList detailsL1LinkNodes = new NodeList();

			rsResponse = Tidy.tidyParse(rsResponse, null);

			HtmlParser3 htmlParser3 = new HtmlParser3(rsResponse);
			String linkStart = dataSite.getLink().substring(0, dataSite.getLink().lastIndexOf("/") + 1);

			Node intermediariesL2 = htmlParser3.getNodeById("ctl00_ContentPlaceHolder1_tblBills");

			if (intermediariesL2 != null) {// if on level 2 intermediaries, get level 1 details for all docs
				// see what happens to unsecured..now we're getting all unsecured
				detailsL1LinkNodes = HtmlParser3.getTag(intermediariesL2.getChildren(), LinkTag.class, true);

				if (detailsL1LinkNodes.size() > 0) {
					for (int i = 0; i < detailsL1LinkNodes.size(); i++) {
						String detailsL1Link = ((LinkTag) detailsL1LinkNodes.elementAt(i)).getLink();

						// follow only bill links - there could be others (e.g. help links)
						if (!StringUtils.extractParameter(detailsL1Link, "(?i)(BillSummary\\.aspx)").isEmpty()) {

							// if bill was cancelled, we don't need it
							Node detailsL1LinkRow = (Node) HtmlParser3.getFirstParentTag(detailsL1LinkNodes.elementAt(i), TableRow.class);
							if (detailsL1LinkRow != null) {
								Node nextRow = detailsL1LinkRow.getNextSibling().getNextSibling();
								if (nextRow != null) {
									if (!StringUtils.extractParameter(nextRow.toHtml(), "(?is)(Bill\\s+was\\s+cancelled\\s+on)").isEmpty()) {
										continue;
									}
								}
							}

							detailsL1Link = linkStart + detailsL1Link;
							detailsL1Content[numberOfDocs++] = getLinkContents(detailsL1Link);
						}
					}
				}
			} else {// if we are already on L1 details
				numberOfDocs++;
				detailsL1Content[0] = rsResponse;
			}

			// get L2 details
			htmlParser3 = new HtmlParser3(detailsL1Content[0]);
			Node detailsL1Node = htmlParser3.getNodeById("Table7");
			if (detailsL1Node != null) {
				for (int i = 0; i < numberOfDocs; i++) {
					htmlParser3 = new HtmlParser3(detailsL1Content[i]);
					String rowIdentifier = "class=\"extraDetailsRows\"";
					if (i == 0) {
						rowIdentifier = "id=\"mainDetailsRow\"";
					}
					detailsSB.append("<tr " + rowIdentifier + "\"><td>");
					
					if (detailsL1Node != null && detailsL1Node instanceof TableTag) {
						TableTag detailsL1Table = (TableTag) detailsL1Node;
						detailsL1Table.setAttribute("style", "min-width:600px;");
						detailsSB.append("<div align=\"center\">" + detailsL1Table.toHtml() + "</div>");
					}

					String detailsL2Link = linkStart;
					Node form = htmlParser3.getNodeById(FORM_NAME);
					if (form != null && form instanceof FormTag) {
						String action = StringEscapeUtils.unescapeHtml(((FormTag) form).getAttribute("action"));
						detailsL2Link += action;
					}

					Node detailsL2Node = htmlParser3.getNodeById("ctl00_ContentPlaceHolder1_btnDetails");
					if (detailsL2Node != null) {// this request needs to be turned into a POST in conn class
						String detailsL2Str = detailsL2Node.toHtml();
						String paramName = StringUtils.extractParameter(detailsL2Str, "(?is)<input[^>]*name=\"([^\"]*)\"");
						String paramValue = StringUtils.extractParameter(detailsL2Str, "(?is)<input[^>]*value=\"([^\"]*)\"");
						detailsL2Link += "&" + paramName + "=" + paramValue;

						Map<String, String> params;
						try {
							params = HttpSite3.fillConnectionParams(detailsL1Content[i], ADD_PARAM_NAMES, FORM_NAME);
							int seq = getSeq();
							detailsL2Link += "&seq=" + seq;
							mSearch.setAdditionalInfo(getCurrentServerName() + ":params:" + seq, params);
						} catch (Exception e) {
							e.printStackTrace();
						}
						String detailsL2Content = getLinkContents(detailsL2Link);

						detailsL2Node = new HtmlParser3(detailsL2Content).getNodeById("Table5");
						if (detailsL2Node != null && detailsL2Node instanceof TableTag) {
							TableTag detailsL2Table = (TableTag) detailsL2Node;
							detailsL2Table.setAttribute("style", "min-width:600px;");
							detailsSB.append("<div align=\"center\">" + detailsL2Table.toHtml() + "</div>");
						}
					}

					if (i == 0) {// get and put previous years data(on main document row)
						Node previousYearsLinkNode = null;
						htmlParser3 = new HtmlParser3(detailsL1Content[0]);
						Node findOtherBillsNode = htmlParser3.getNodeById("ctl00_ContentPlaceHolder1_lnkParent");
						String prevGenericLink = linkStart;

						form = htmlParser3.getNodeById(FORM_NAME);
						if (form != null && form instanceof FormTag) {
							String action = ((FormTag) form).getAttribute("action");
							prevGenericLink += action;
						}

						if (findOtherBillsNode != null && findOtherBillsNode instanceof LinkTag) {
							LinkTag taxNoLinkTag = (LinkTag) findOtherBillsNode;
							String prevL1Link = linkStart + StringEscapeUtils.unescapeHtml(taxNoLinkTag.getLink());
							String pageWithLinkToPrevYears = getLinkContents(prevL1Link);
							prevGenericLink = prevL1Link;

							Map<String, String> params;
							try {
								params = HttpSite3.fillConnectionParams(pageWithLinkToPrevYears, ADD_PARAM_NAMES, FORM_NAME);
								int seq = getSeq();
								prevGenericLink += "&seq=" + seq;
								mSearch.setAdditionalInfo(getCurrentServerName() + ":params:" + seq, params);
							} catch (Exception e) {
								e.printStackTrace();
							}

							htmlParser3 = new HtmlParser3(pageWithLinkToPrevYears);
							previousYearsLinkNode = htmlParser3.getNodeById("ctl00_ContentPlaceHolder1_btnPreviousYear");

							if (previousYearsLinkNode != null) {
								String previousYearsL1Str = previousYearsLinkNode.toHtml();
								String paramName = StringUtils.extractParameter(previousYearsL1Str, "(?is)<input[^>]*name=\"([^\"]*)\"");
								String paramValue = StringUtils.extractParameter(previousYearsL1Str, "(?is)<input[^>]*value=\"([^\"]*)\"");

								String prevL2Link = prevGenericLink + "&" + paramName + "=" + paramValue + "&seq=" + seq;
								String prevYearsContent = getLinkContents(prevL2Link);
								htmlParser3 = new HtmlParser3(prevYearsContent);

								Node previousYearsNode = htmlParser3.getNodeById("ctl00_ContentPlaceHolder1_tblBills");
								if (previousYearsNode != null && previousYearsNode instanceof TableTag) {
									TableTag previousYearsTable = (TableTag) previousYearsNode;
									previousYearsTable.setAttribute("style", "min-width:600px;");
									detailsSB.append("<div id=\"previousYears\" align=\"center\">" + previousYearsTable.toHtml() + "</div>");
								}
							}
						}
					}
					detailsSB.append("</td></tr>");
				}
			}

			if (!detailsSB.toString().isEmpty()) {
				String details = "<table id=\"allDetails\" border=\"1\" align=\"center\" style=\"min-width:600px;\">"
						+ detailsSB.toString()
								.replaceAll("(?is)<(?:img|input)[^>]*>", "")
								.replaceAll("(?is)(<[^>]*class\\s*=\\s*\"txt12ptAHSBlackBold\")", "$1 style=\"font-weight:bold;\"")
								.replaceAll("(?is)<a[^>]*>(.*?)</a>", "$1")
								.replaceAll("(?is)(<[^>]*)\\bstyle=\"[^\"]*(?:background-color)\\s*:[^\"]*\"([^>]*>)", "$1$2")
								.replaceAll("(?is)(<[^>]*)\\s+(?:width)\\s*=\\s*\"[^\"]*\"", "$1")
								.replaceAll("(?is)(<table\\b)", "$1 align=\"center\"")
								.replaceAll("[^\\p{Alnum}\\p{Punct}\\s]", "")
						+ "</table>";

				return details;
			}

		} catch (Exception e) {
			logger.error("Error while getting details", e);
		}
		return null;
	}

	@Override
	protected Object parseAndFillResultMap(ServerResponse response, String detailsHtml, ResultMap resultMap) {
		resultMap.put(OtherInformationSetKey.SRC_TYPE.getKeyName(), getDataSite().getSiteTypeAbrev());
		ro.cst.tsearch.servers.functions.CAKernTR.parseAndFillResultMap(detailsHtml, resultMap, searchId);
		return null;
	}

	@Override
	public Collection<ParsedResponse> smartParseIntermediary(
			ServerResponse response, String htmlTable, StringBuilder outputTable) {
		Vector<ParsedResponse> intermediaryResponse = new Vector<ParsedResponse>();

		try {

			boolean isATNSearchIntermediaries = false;
			if (!StringUtils.extractParameter(htmlTable, "(?is)(Results\\s+of\\s+Assessor\\s+Tax\\s+Number\\s+Search)").isEmpty()) {
				isATNSearchIntermediaries = true;
			}
			ParsedResponse parsedResponse = response.getParsedResponse();
			htmlTable = htmlTable.replaceAll("(?is)(<[^>]*)bgcolor=\"[^\"]*\"([^>]*>)", "$1$2");
			htmlTable = Tidy.tidyParse(htmlTable, null);
			HtmlParser3 htmlParser3 = new HtmlParser3(htmlTable);
			Node node = htmlParser3.getNodeById("ctl00_ContentPlaceHolder1_dlSearchResults");

			if (node != null && node instanceof TableTag) {
				node = HtmlParser3.getFirstTag(node.getChildren(), TableTag.class, true);
				String dataSitePath = dataSite.getLink().substring(0, dataSite.getLink().lastIndexOf("/") + 1);
				dataSitePath = StringUtils.extractParameter(dataSitePath, "(/[^/]*/)$");

				TableTag table = (TableTag) node;
				TableRow[] rows = table.getRows();
				String header = "<table style=\"min-width:600px;\" align=\"center\" border=\"1\" cellspacing=\"0\" cellpadding=\"2\">";

				if (rows.length > 0) {
					TableRow headerRow = rows[0];

					if (headerRow != null) {
						header += headerRow.toHtml();
					}
					for (int i = 1; i < rows.length; i++) {
						TableRow row = rows[i];

						if (row.getColumnCount() == 1) {
							row = HtmlParser3.getFirstTag(((Node) row).getChildren(), TableRow.class, true);
						}

						if (row != null && row.getColumnCount() >= 2) {
							Node linkNode = (Node) row.getColumns()[0];
							linkNode = HtmlParser3.getFirstTag(linkNode.getChildren(), LinkTag.class, true);
							if (linkNode != null) {
								LinkTag linkTag = (LinkTag) linkNode;

								String link = CreatePartialLink(TSConnectionURL.idGET) + dataSitePath + linkTag.getLink();
								linkTag.setLink(link);
								String fullRow = row.toHtml();

								ParsedResponse currentResponse = new ParsedResponse();
								currentResponse.setAttribute(ParsedResponse.SERVER_ROW_RESPONSE, fullRow);
								currentResponse.setOnlyResponse(fullRow);
								currentResponse.setPageLink(new LinkInPage(link, link, TSServer.REQUEST_SAVE_TO_TSD));

								ResultMap resultMap = ro.cst.tsearch.servers.functions.CAKernTR.parseIntermediaryRow(row);
								resultMap.put(OtherInformationSetKey.SRC_TYPE.getKeyName(), getDataSite().getSiteTypeAbrev());

								Bridge bridge = new Bridge(currentResponse, resultMap, searchId);
								DocumentI document = (TaxDocumentI) bridge.importData();
								currentResponse.setDocument(document);

								intermediaryResponse.add(currentResponse);
							}
						}
					}

					// set paging links; e.g.GET http://www.kcttc.co.kern.ca.us/payment/AddressSummary.aspx?NUMBER=10&STREET=main&TYPE=&DIR=&CITY=
					// or POST http://www.kcttc.co.kern.ca.us/payment/ATNSummary.aspx?NUMBER=3022204900&NUM_TYPE=AT ; - for ATN search intermediaries L1
					StringBuilder footerSB = new StringBuilder("</table>");
					String idNextTag = "ctl00_ContentPlaceHolder1_btnNext";
					String idPrevTag = "ctl00_ContentPlaceHolder1_btnPrev";
					String nameNextTag = "ctl00$ContentPlaceHolder1$btnNext";
					String namePrevTag = "ctl00$ContentPlaceHolder1$btnPrev";
					String nextLink = "";
					String prevLink = "";
					String basePath = "";
					node = null;

					Node nextNode = htmlParser3.getNodeById(idNextTag);
					Node prevNode = htmlParser3.getNodeById(idPrevTag);
					if (prevNode == null) {
						idPrevTag = "ctl00_ContentPlaceHolder1_btnPrevious";
						namePrevTag = "ctl00$ContentPlaceHolder1$btnPrevious";
						prevNode = htmlParser3.getNodeById(idPrevTag);
					}
					if (nextNode != null || prevNode != null) {
						footerSB.append("<div id=\"pagingDiv\" align=\"center\">");

						node = htmlParser3.getNodeById(FORM_NAME);
						if (node != null && node instanceof FormTag) {
							String action = ((FormTag) node).getAttribute("action");
							basePath = CreatePartialLink(TSConnectionURL.idPOST) + dataSitePath + action;
						}
						Map<String, String> params;
						try {
							params = HttpSite3.fillConnectionParams(htmlTable, ADD_PARAM_NAMES, FORM_NAME);
							int seq = getSeq();
							basePath += "&seq=" + seq;
							mSearch.setAdditionalInfo(getCurrentServerName() + ":params:" + seq, params);
						} catch (Exception e) {
							e.printStackTrace();
						}
						if (isATNSearchIntermediaries) {// for ATN intermediaries e.g. 302-220-49-00
							String numberParamId = "ctl00_ContentPlaceHolder1_txtSearchByNumber";
							String numberParamName = "ctl00$ContentPlaceHolder1$txtSearchByNumber";

							node = htmlParser3.getNodeById(numberParamId);
							if (node != null && node instanceof InputTag) {
								String numberParamValue = ((InputTag) node).getAttribute("value");
								basePath += "&" + numberParamName + "=" + numberParamValue;
							}

						}
						if (prevNode != null) {
							prevLink = "<a href=\"" + basePath + "&" + EVENTTARGET + "=" + namePrevTag + "\">Previous</a>";
							footerSB.append("<b>" + prevLink + "&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;</b>");
						}

						if (nextNode != null) {
							nextLink = "<a href=\"" + basePath + "&" + EVENTTARGET + "=" + nameNextTag + "\">Next</a>";
							parsedResponse.setNextLink(nextLink);
							footerSB.append("<b>" + nextLink + "</b>");
						}

						footerSB.append("</div>");
					}

					String footer = footerSB.toString();

					parsedResponse.setHeader(header);
					parsedResponse.setFooter(footer);
					outputTable.append(table);
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
		Search search = getSearch();
		int searchType = search.getSearchType();

		if (searchType == Search.AUTOMATIC_SEARCH) {
			if (hasPin()) {
				module = new TSServerInfoModule(serverInfo.getModule(TSServerInfo.PARCEL_ID_MODULE_IDX));
				module.clearSaKeys();
				module.setSaKey(0, SearchAttributes.LD_PARCELNO);
				moduleList.add(module);
			}

			FilterResponse addressFilter = AddressFilterFactory.getAddressHybridFilter(searchId, 0.8d, true);
			FilterResponse cityFilter = CityFilterFactory.getCityFilterDefault(searchId);
			if (hasStreet()) {
				if (hasStreetNo()) {
					module = new TSServerInfoModule(serverInfo.getModule(TSServerInfo.ADDRESS_MODULE_IDX));
					module.clearSaKeys();
					module.setSaKey(0, SearchAttributes.P_STREETNO);
					module.setSaKey(2, SearchAttributes.P_STREETNAME);
					module.addFilterForNextType(FilterResponse.TYPE_ADDRESS_FOR_NEXT);
					module.addFilter(cityFilter);
					module.addFilter(addressFilter);
					moduleList.add(module);
				}
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

	protected synchronized static int getSeq() {
		return seq++;
	}
}