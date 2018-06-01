package ro.cst.tsearch.servers.types;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Vector;
import java.util.Map.Entry;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.htmlparser.Node;
import org.htmlparser.filters.RegexFilter;
import org.htmlparser.filters.TagNameFilter;
import org.htmlparser.tags.TableColumn;
import org.htmlparser.tags.TableRow;
import org.htmlparser.tags.TableTag;
import org.htmlparser.util.NodeList;

import ro.cst.tsearch.Search;
import ro.cst.tsearch.bean.SearchAttributes;
import ro.cst.tsearch.connection.http.HTTPRequest;
import ro.cst.tsearch.connection.http3.HttpManager3;
import ro.cst.tsearch.exceptions.ServerResponseException;
import ro.cst.tsearch.extractor.xml.Bridge;
import ro.cst.tsearch.extractor.xml.ResultMap;
import ro.cst.tsearch.parser.HtmlParser3;
import ro.cst.tsearch.search.FunctionStatesIterator;
import ro.cst.tsearch.search.filter.newfilters.name.GenericNameFilter;
import ro.cst.tsearch.search.filter.newfilters.name.NameFilterFactory;
import ro.cst.tsearch.search.iterator.ModuleStatesIteratorFactory;
import ro.cst.tsearch.search.module.ConfigurableNameIterator;
import ro.cst.tsearch.servers.TSConnectionURL;
import ro.cst.tsearch.servers.info.TSServerInfo;
import ro.cst.tsearch.servers.info.TSServerInfoModule;
import ro.cst.tsearch.servers.response.LinkInPage;
import ro.cst.tsearch.servers.response.OtherInformationSet.OtherInformationSetKey;
import ro.cst.tsearch.servers.response.ParsedResponse;
import ro.cst.tsearch.servers.response.SaleDataSet.SaleDataSetKey;
import ro.cst.tsearch.servers.response.ServerResponse;
import ro.cst.tsearch.servlet.URLConnectionReader;
import ro.cst.tsearch.utils.GBManager;
import ro.cst.tsearch.utils.InstanceManager;
import ro.cst.tsearch.utils.RegExUtils;
import ro.cst.tsearch.utils.StringUtils;
import ro.cst.tsearch.utils.Tidy;

import com.stewart.ats.base.document.DocumentI;
import com.stewart.ats.base.document.RegisterDocumentI;

public class TNShelbyPR extends TSServer {

	private static final long	serialVersionUID	= -6957915556240901493L;

	public TNShelbyPR(long searchId) {
		super(searchId);
		resultType = MULTIPLE_RESULT_TYPE;
	}

	public TNShelbyPR(String rsRequestSolverName, String rsSitePath, String rsServerID, String rsPrmNameLink, long searchId, int miServerID) {
		super(rsRequestSolverName, rsSitePath, rsServerID, rsPrmNameLink, searchId, miServerID);
		resultType = MULTIPLE_RESULT_TYPE;
	}

	@Override
	protected void ParseResponse(String sAction, ServerResponse Response, int viParseID) throws ServerResponseException {

		String rsResponse = Response.getResult();
		ParsedResponse parsedResponse = Response.getParsedResponse();

		switch (viParseID) {
		case ID_SEARCH_BY_NAME: // Case Name Search
			if (StringUtils.isNotEmpty(RegExUtils.getFirstMatch("(?is)(No(?:&nbsp;|\\s*)cases(?:&nbsp;|\\s*)available(?:&nbsp;|\\s*)"
					+ "for(?:&nbsp;|\\s*)name(?:&nbsp;|\\s*)entered)", rsResponse, 1))) {
				parsedResponse.setError(NO_DATA_FOUND);
				parsedResponse.setAttribute(ParsedResponse.SHOW_ONLY_ERROR_MESSAGE, "true");
				return;
			}
			StringBuilder outputTable = new StringBuilder();
			Collection<ParsedResponse> smartParsedResponses = smartParseIntermediary(Response, rsResponse, outputTable);

			if (smartParsedResponses.size() > 0) {
				parsedResponse.setResultRows(new Vector<ParsedResponse>(smartParsedResponses));
				parsedResponse.setOnlyResponse(outputTable.toString());
				parsedResponse.setAttribute(ParsedResponse.SERVER_ROW_RESPONSE, outputTable.toString());
			}

			Object numberOfUnsavedDocument = GetAttribute(NUMBER_OF_UNSAVED_DOCUMENTS);

			String header = parsedResponse.getHeader();
			String footer = parsedResponse.getFooter();

			header = CreateSaveToTSDFormHeader(URLConnectionReader.MULTIPLE_SAVE_TO_TSD_ACTION, "POST") + header;

			if (numberOfUnsavedDocument != null && numberOfUnsavedDocument instanceof Integer) {
				footer += CreateSaveToTSDFormEnd(SAVE_SELECTED_DOCUMENTS_BUTTON_LABEL, viParseID, (Integer) numberOfUnsavedDocument);
			} else {
				footer += CreateSaveToTSDFormEnd(SAVE_SELECTED_DOCUMENTS_BUTTON_LABEL, viParseID, -1);
			}

			parsedResponse.setHeader(header);
			parsedResponse.setFooter(footer);

			break;

		case ID_DETAILS:
		case ID_SAVE_TO_TSD:
		case ID_SEARCH_BY_INSTRUMENT_NO: // Case Number Search

			if ((StringUtils.isNotEmpty(RegExUtils
					.getFirstMatch("(?is)(Select\\s+type\\s+of\\s+information\\s+to\\s+view)", rsResponse, 1))
			&& StringUtils.isNotEmpty(RegExUtils
					.getFirstMatch("(?is)(Searches\\s+for\\s+case\\s+information\\s+can\\s+be\\s+accessed\\s+by\\s+name)", rsResponse, 1)))) {
				parsedResponse.setError(NO_DATA_FOUND);
				parsedResponse.setAttribute(ParsedResponse.SHOW_ONLY_ERROR_MESSAGE, "true");
				return;
			}

			String details = getDetails(Response);
			if (details != null) {
				HtmlParser3 htmlParser3 = new HtmlParser3(details.replaceAll("&nbsp;", " "));
				String caseNumber = getColumnValueByIdOrRegex("CVCAIQH1", "(?i)Case\\s+number", htmlParser3);
				String dateFiled = getColumnValueByIdOrRegex("CVCAIQH8", "(?i)Date\\s+filed", htmlParser3);

				String caseType = getColumnValueByIdOrRegex("", "(?i)Case\\s+type", htmlParser3);
				if (StringUtils.isEmpty(caseType)) {
					Node caseTypePart1Node = htmlParser3.getNodeById("CVCAIQH3");
					Node caseTypePart2Node = htmlParser3.getNodeById("CVCAIQH4");
					if (caseTypePart1Node != null && caseTypePart2Node != null) {
						caseType = caseTypePart1Node.toPlainTextString().trim() + " " + caseTypePart2Node.toPlainTextString().trim();
					}
				}
				String filename = caseNumber + ".html";

				if (viParseID != ID_SAVE_TO_TSD) {
					String originalLink = sAction.replace("?", "&") + "&" + Response.getRawQuerry();
					String sSave2TSDLink = getLinkPrefix(TSConnectionURL.idPOST) + originalLink;

					HashMap<String, String> data = new HashMap<String, String>();
					data.put("dataSource", getDataSite().getSiteTypeAbrev());
					data.put("type", caseType);
					data.put("date", dateFiled);
					if (isInstrumentSaved(caseNumber, null, data, false)) {
						details += CreateFileAlreadyInTSD();
					} else {
						mSearch.addInMemoryDoc(sSave2TSDLink, details);
						details = addSaveToTsdButton(details, sSave2TSDLink, ID_DETAILS);
					}

					parsedResponse.setPageLink(new LinkInPage(sSave2TSDLink, originalLink, TSServer.REQUEST_SAVE_TO_TSD));
					parsedResponse.setResponse(details);

				} else {
					smartParseDetails(Response, details);

					msSaveToTSDFileName = filename;
					parsedResponse.setFileName(getServerTypeDirectory() + msSaveToTSDFileName);
					parsedResponse.setResponse(details);

					msSaveToTSDResponce = details + CreateFileAlreadyInTSD();

				}
			}
			break;

		case ID_GET_LINK:
			String responseTitle = RegExUtils.getFirstMatch("(?i)(<b\\b[^>]*>\\s*Case\\s+Name\\s+Search\\s*</b>)", rsResponse, 1);
			if (StringUtils.isNotEmpty(responseTitle)) {
				ParseResponse(sAction, Response, ID_SEARCH_BY_NAME);
			} else {
				ParseResponse(sAction, Response, ID_DETAILS);
			}
			break;

		default:
			break;
		}
	}

	protected String getColumnValueByIdOrRegex(String nodeID, String regex, HtmlParser3 htmlParser3) {
		String value = "";
		Node node = null;

		if (StringUtils.isNotEmpty(nodeID)) {
			node = htmlParser3.getNodeById(nodeID);
		}
		if (node == null && StringUtils.isNotEmpty(regex)) {
			NodeList allNodes = htmlParser3.getNodeList();
			NodeList nodeList = allNodes.extractAllNodesThatMatch(new RegexFilter(regex), true);
			if (nodeList.size() > 0) {
				node = nodeList.elementAt(0);
			}
		}

		if (node != null) {
			node = HtmlParser3.getFirstParentTag(node, TableRow.class);
			if (node != null) {
				TableColumn[] cols = ((TableRow) node).getColumns();
				if (cols.length > 1) {
					value = cols[1].toPlainTextString().replaceAll("\\s+", " ").trim();
				}
			}
		}
		return value;
	}

	protected String getDetails(ServerResponse Response) {
		try {

			String rsResponse = Response.getResult();
			HtmlParser3 htmlParser3 = new HtmlParser3(rsResponse);
			NodeList nodeList = htmlParser3.getNodeList();

			/* If from memory - use it as is */
			if (!rsResponse.toLowerCase().contains("<html")) {
				return rsResponse;
			}

			nodeList = nodeList.extractAllNodesThatMatch(new TagNameFilter("form"), true);
			if (nodeList.size() > 0) {
				Node mainForm = nodeList.elementAt(0);
				if (mainForm != null) {
					nodeList = mainForm.getChildren();
					String[] linksContents = new String[10];
					int linkIdx = 0;
					String[] events = new String[10];
					int eventsPageIdx = 0;
					String siteLink = getDataSite().getLink();
					String link = siteLink + "?isGetMoreDetailsLink=true";

					// get all links
					NodeList linkNodes = nodeList.extractAllNodesThatMatch(new TagNameFilter("a"), true);
					if (linkNodes.size() > 0) {
						ro.cst.tsearch.connection.http3.TNShelbyPR site = (ro.cst.tsearch.connection.http3.TNShelbyPR) HttpManager3.getSite(
								getCurrentServerName(), searchId);
						try {
							for (int i = 0; i < linkNodes.size(); i++) {
								Node linkNode = linkNodes.elementAt(i);
								String destination = RegExUtils.getFirstMatch("(?is)(Events|Service|Bonds"
										+ "|Claims|Related\\s+Cases|Individuals|Case\\s+Fees)", linkNode.toPlainTextString(), 1)
										.replaceAll("\\s+", "");
								if (linkNode != null && StringUtils.isNotEmpty(destination)) {
									try {
										if (destination.equals("Service")) {
											// get contents of "Next" link(s) in "Events" page
											String nextLinkMatch = RegExUtils.getFirstMatch(
													"(?is)(<script[^>]*>\\s*document\\.write\\(top\\.chgsTonext\\((\"|')F9.*?</script>)",
													linksContents[linkIdx - 1], 1);
											if (StringUtils.isNotEmpty(nextLinkMatch)) {
												boolean pageHasNextLink = true;
												while (pageHasNextLink) {
													HTTPRequest eventsNextPageReq = new HTTPRequest(link, HTTPRequest.POST);
													eventsNextPageReq.setPostParameter("XPBCHO_990101", "C");
													eventsNextPageReq.setPostParameter("MY_PFKEY", "09");
													events[eventsPageIdx] = site.process(eventsNextPageReq).getResponseAsString();

													// test if the new page has another "Next" link in it
													nextLinkMatch = RegExUtils.getFirstMatch(
															"(?is)(<script[^>]*>\\s*document\\.write\\(top\\.chgsTonext\\((\"|')F9.*?</script>)",
															events[eventsPageIdx], 1);
													if (StringUtils.isEmpty(nextLinkMatch)) {
														pageHasNextLink = false;
													}
													eventsPageIdx++;
												}
											}
										}

										// get the link contents
										HTTPRequest request = new HTTPRequest(link, HTTPRequest.POST);
										String pageChoice = RegExUtils.getFirstMatch("(?is)setChoice0?\\('(\\w+)'\\)", linkNode.toHtml(), 1);
										String pageChoiceParamName = destination.equals("Events") ? "XPBCHO_000101" : "XPBCHO_990101";
										request.setPostParameter(pageChoiceParamName, pageChoice);
										if (destination.equals("CaseFees")) {
											request.setPostParameter("MY_PFKEY", "15");
										}
										linksContents[linkIdx] = site.process(request).getResponseAsString();
										linkIdx++;
									} catch (RuntimeException e) {
										logger.warn("Could not bring link:" + link, e);
									}
								}
							}
						} finally {
							HttpManager3.releaseSite(site);
						}
					}

					String details = "";
					StringBuilder detailsSB = new StringBuilder("<table>");
					detailsSB.append("<tr><td id=\"GeneralInfo\">" + mainForm.toHtml() + "</td></tr>");
					for (int i = 0; i < linkIdx; i++) {
						if (StringUtils.isNotEmpty(linksContents[i])) {
							nodeList = new HtmlParser3(linksContents[i]).getNodeList();
							nodeList = nodeList.extractAllNodesThatMatch(new TagNameFilter("form"), true);
							if (nodeList.size() > 0) {
								mainForm = nodeList.elementAt(0);
								if (mainForm != null) {
									detailsSB.append("<tr><td id=\"detailsLinkNo_" + i + "\"" + (i == 0 ? " class=\"events\"" : "") + ">"
											+ mainForm.toHtml() + "</td></tr>");
								}
							}
							if (i == 0) {
								// if adding events page and event page has next link(s) contents, add them also
								for (int j = 0; j < eventsPageIdx; j++) {
									if (StringUtils.isNotEmpty(events[j])) {
										nodeList = new HtmlParser3(events[j]).getNodeList();
										nodeList = nodeList.extractAllNodesThatMatch(new TagNameFilter("form"), true);
										if (nodeList.size() > 0) {
											mainForm = nodeList.elementAt(0);
											if (mainForm != null) {
												detailsSB.append("<tr><td class=\"events\" id=\"eventsNext_" + j + "\">" + mainForm.toHtml() + "</td></tr>");
											}
										}
									}
								}
							}
						}
					}
					detailsSB.append("</table>");
					details = Tidy.tidyParse(detailsSB.toString(), null)
							.replaceAll("(?is)</?(html|head|title|body)\\b[^>]*>", "")
							.replaceAll("&nbsp;", " ")
							.replaceAll("(?is)>\\s*<", "><")
							.replaceAll("(?is)<(table|td|tr)\\b[^>]*>", "<$1>")
							.replaceFirst("<table>", "<table id=\"detailsTable\" align=\"center\" border=\"1\" "
									+ "style=\"min-width:800px; max-width:900px;\">")
							.replaceAll("(?is)<td[^>]*>\\s*(<b\\b[^>]*>\\s*)?PROBATE\\s+CASE\\s+INFORMATION(\\s*</b>)?\\s*</td>", "")
							.replaceAll("(?is)<tr[^>]*>(<td[^>]*></td>)*<td[^>]*><a[^>]*>"// remove links row
									+ "(<font[^>]*>)?New\\s+Search\\s*(</font>)?</a>.*?</tr>", "")
							.replaceAll("(?i)(</?)form\\b[^>]*>", "$1div>")
							.replaceAll("(?is)<a\\b[^>]*>(.*?)</a>", "$1")
							.replaceAll("(?is)<input[^>]*>", "")
							.replaceAll("(?is)<script[^>]*>.*?</script>", "")
							.replaceAll("(?is)<span[^>]*>\\s*</span>", " ")
							.replaceAll("(?is)<font[^>]*>\\s*</font>", "")
							.replaceAll("(?is)<span[^>]*>\\s*</span>", " ")
							.replaceAll("(?is)<td[^>]*>\\s*(</span>)?</td>", "")
							.replaceAll("(?is)<tr[^>]*>\\s*</tr>", "")
							.replaceAll("(?is)<font[^>]*>(\\s*Notice\\s*:\\s*To\\s+view|View\\s+orders\\s+entered\\s+prior).*?</font>", "")
							.replaceAll("(?is)(<span[^>]*>)", "$1&nbsp;");
					// .replaceAll("(?is)(<span\\b[^>]*class\\s*=\\s*\"libNNW\"[^>]*)>", "$1 style=\"font-weight:bold;\">");
					return details;

				}
			}
		} catch (Exception e) {
			logger.error("Error while getting details", e);
		}
		return null;
	}

	@Override
	public Collection<ParsedResponse> smartParseIntermediary(ServerResponse response, String htmlTable, StringBuilder outputTable) {

		Vector<ParsedResponse> intermediaryResponse = new Vector<ParsedResponse>();
		int numberOfUncheckedElements = 0;

		try {
			ParsedResponse parsedResponse = response.getParsedResponse();
			htmlTable = Tidy.tidyParse(htmlTable, null);
			htmlTable = htmlTable.replaceAll("(?is)&nbsp;", " ")
					.replaceAll("(?is)<td[^>]*>\\s*</td>", "")
					.replaceAll("(?is)<tr[^>]*>\\s*</tr>", "");
			HtmlParser3 htmlParser3 = new HtmlParser3(htmlTable);
			NodeList nodeList = htmlParser3.getNodeList();
			Node docketNumberSpan = htmlParser3.getNodeById("CVNAIQL6C2");
			Node resultsNode = null;
			String siteType = getDataSite().getSiteTypeAbrev();
			if (docketNumberSpan != null) {
				resultsNode = HtmlParser3.getFirstParentTag(docketNumberSpan, TableTag.class);
			}
			if (resultsNode == null) {
				resultsNode = HtmlParser3.getNodeByTypeAttributeDescription(nodeList, "table", "", "",
						new String[] { "Docket Number", "Name", "Case Type", "Date Filed" }, true);
			}

			if (resultsNode != null) {
				TableTag resultsTable = (TableTag) resultsNode;
				Map<String, String> rowsMap = new LinkedHashMap<String, String>();

				TableRow[] rows = resultsTable.getRows();
				String tableHeader = null;
				StringBuilder outputTableSB = new StringBuilder();
				int rowsCount = rows.length;
				for (int i = 0; i < rowsCount; i++) {
					TableRow row = rows[i];

					if (row.getColumnCount() >= 4) {
						String rowHtml = row.toHtml();
						TableColumn[] columns = row.getColumns();
						if (columns[0].toPlainTextString().trim().isEmpty() && columns[1].toPlainTextString().trim().isEmpty()) {
							continue;
						}

						// the next row has only one column and it is part of the current result
						if (i + 1 < rowsCount && rows[i + 1].getColumnCount() <= 2) {
							// merge the two rows into one
							String nextRow = rows[++i].toHtml()
									.replaceFirst("(?is)<td[^>]*>\\s*(<span[^>]*>\\s*</span>\\s*)?</td>", "");
							rowHtml = (rowHtml + nextRow)
									.replaceFirst("(?is)</tr>\\s*<tr[^>]*>", "");
						}

						if (StringUtils.isEmpty(tableHeader) && StringUtils.isNotEmpty(RegExUtils
								.getFirstMatch("(?is)(>\\s*(Docket\\s+Number)\\s*<)", columns[0].toHtml(), 1))) {
							tableHeader = rowHtml;
							continue;
						}

						// select only unique results !!
						String caseNo = columns[0].toPlainTextString().replaceAll("\\s+", "");
						String caseType = columns[2].toPlainTextString().trim();
						String dateFiled = columns[3].toPlainTextString().trim();
						String key = caseNo + "_" + caseType + "_" + dateFiled;
						String value = rowsMap.get(key);
						if (value == null) {
							rowsMap.put(key, rowHtml);
						}
					}
				}

				Iterator<Entry<String, String>> it = rowsMap.entrySet().iterator();
				while (it.hasNext()) {// get unique result rows
					Map.Entry<String, String> pair = (Map.Entry<String, String>) it.next();
					String rowFromMap = pair.getValue();
					outputTableSB.append(rowFromMap);

					// parse intermediaries
					ResultMap resultMap = ro.cst.tsearch.servers.functions.TNShelbyPR.parseIntermediaryRow(rowFromMap);
					resultMap.put(OtherInformationSetKey.SRC_TYPE.getKeyName(), siteType);

					// set details links
					String params = "";
					Pattern pat = Pattern.compile("(?is)<input[^>]*\\bname\\s*=\\s*(?:\"|')([^>]+)(?:\"|')[^>]*"
							+ "\\bvalue\\s*=\\s*(?:\"|')([^>]+)(?:\"|')");
					String rowLink = RegExUtils.getFirstMatch("(?is)(<a\\b[^>]*>.*?</a>)", rowFromMap, 1);
					Matcher mat = pat.matcher(rowLink);
					if (mat.find()) {
						params += "&key=" + mat.group(2);
					}
					if (mat.find()) {
						params += "&number=" + mat.group(2);
					}
					String detailsLink = CreatePartialLink(TSConnectionURL.idPOST) + "/intermediaries?isGetDetailsLink=true" + params;
					String htmlRow = rowFromMap;
					htmlRow = htmlRow.replaceFirst("(?is)<a[^>]*>(.*?)</a>", "<a href=\"" + detailsLink + "\">$1</a>");

					ParsedResponse currentResponse = new ParsedResponse();
					currentResponse.setPageLink(new LinkInPage(detailsLink, detailsLink, TSServer.REQUEST_SAVE_TO_TSD));

					// detect and mark already saved docs
					String checkBox = "checked";
					String caseNumber = org.apache.commons.lang.StringUtils
							.defaultString((String) resultMap.get(SaleDataSetKey.INSTRUMENT_NUMBER.getKeyName()));
					String caseType = org.apache.commons.lang.StringUtils
							.defaultString((String) resultMap.get(SaleDataSetKey.DOCUMENT_TYPE.getKeyName()));
					String dateFiled = org.apache.commons.lang.StringUtils
							.defaultString((String) resultMap.get(SaleDataSetKey.RECORDED_DATE.getKeyName()));
					HashMap<String, String> data = new HashMap<String, String>();
					data.put("dataSource", siteType);
					data.put("type", caseType);
					data.put("date", dateFiled);

					if (isInstrumentSaved(caseNumber, null, data, false)) {
						checkBox = "saved";
					} else {
						numberOfUncheckedElements++;
						LinkInPage linkInPage = new LinkInPage(detailsLink, detailsLink, TSServer.REQUEST_SAVE_TO_TSD);
						checkBox = "<input type='checkbox' name='docLink' value='" + detailsLink + "'>";
						currentResponse.setPageLink(linkInPage);
					}
					htmlRow = htmlRow.replaceFirst("(?is)<tr[^>]*>", "<tr><td align=\"center\">" + checkBox + "</td>");

					currentResponse.setAttribute(ParsedResponse.SERVER_ROW_RESPONSE, htmlRow);
					currentResponse.setOnlyResponse(htmlRow);

					Bridge bridge = new Bridge(currentResponse, resultMap, searchId);
					DocumentI document = (RegisterDocumentI) bridge.importData();
					currentResponse.setDocument(document);
					intermediaryResponse.add(currentResponse);
				}

				// set next link, if present
				String nextLink = "";
				String nextLinkMatch = RegExUtils.getFirstMatch("(?is)(<a[^>]*Next\\s+List[^>]*>.*?\\(top\\.chgsTonext\\((?:'|\")F9.*?</a>)", htmlTable, 1);
				if (StringUtils.isNotEmpty(nextLinkMatch)) {
					String myPFKey = RegExUtils.getFirstMatch("(?is)top\\.Go\\((?:'|\")(\\d+)(?:'|\")\\)", nextLinkMatch, 1);
					String pageChoice = RegExUtils.getFirstMatch("(?is)\\.setChoice0?\\('(\\w+)'\\)", nextLinkMatch, 1);
					nextLink = "<a href=\"" + CreatePartialLink(TSConnectionURL.idPOST) + "/intermediaries?isNextLink=true"
							+ "&MY_PFKEY=" + myPFKey + "&XPBCHO_990101=" + pageChoice + "\">Next</a>";
					parsedResponse.setNextLink(nextLink);
				}

				// set table header and footer
				tableHeader = tableHeader.replaceFirst("(?is)^\\s*(<tr[^>]*>)", "$1<td align=\"center\">" + SELECT_ALL_CHECKBOXES + "</td>");
				String header = "<table style=\"min-width:700px; max-width:900px;\" border=\"1\">" + tableHeader;
				String footer = "</table><br><div>&nbsp;&nbsp;&nbsp;<b>" + nextLink + "</b></div><br>";
				parsedResponse.setHeader(header);
				parsedResponse.setFooter(footer);

				outputTable.append(header);
				outputTable.append(outputTableSB.toString());
				outputTable.append(footer);

				SetAttribute(NUMBER_OF_UNSAVED_DOCUMENTS, numberOfUncheckedElements);
			}
		} catch (Throwable t) {
			logger.error("Error while parsing intermediary data", t);
		}
		return intermediaryResponse;
	}

	@Override
	protected Object parseAndFillResultMap(ServerResponse response, String detailsHtml, ResultMap resultMap) {

		detailsHtml = detailsHtml.replaceAll("&nbsp;", " ");
		detailsHtml = detailsHtml.replaceAll("(?is)<!--[^>]*-->", "");
		HtmlParser3 htmlParser3 = new HtmlParser3(detailsHtml);

		resultMap.put(OtherInformationSetKey.SRC_TYPE.getKeyName(), getDataSite().getSiteTypeAbrev());

		String caseNumber = getColumnValueByIdOrRegex("CVCAIQH1", "(?i)Case\\s+number", htmlParser3);
		String dateFiled = getColumnValueByIdOrRegex("CVCAIQH8", "(?i)Date\\s+filed", htmlParser3);
		String caseType = getColumnValueByIdOrRegex("", "(?i)Case\\s+type", htmlParser3);
		if (StringUtils.isEmpty(caseType)) {
			Node caseTypePart1Node = htmlParser3.getNodeById("CVCAIQH3");
			Node caseTypePart2Node = htmlParser3.getNodeById("CVCAIQH4");
			if (caseTypePart1Node != null && caseTypePart2Node != null) {
				caseType = caseTypePart1Node.toPlainTextString().trim() + " " + caseTypePart2Node.toPlainTextString().trim();
			}
		}

		if (StringUtils.isNotEmpty(caseNumber)) {
			resultMap.put(SaleDataSetKey.INSTRUMENT_NUMBER.getKeyName(), caseNumber);
		}
		if (StringUtils.isNotEmpty(caseType)) {
			resultMap.put(SaleDataSetKey.DOCUMENT_TYPE.getKeyName(), caseType);
		}
		if (StringUtils.isNotEmpty(dateFiled)) {
			resultMap.put(SaleDataSetKey.RECORDED_DATE.getKeyName(), dateFiled);
		}

		String grantor = null;
		Node grantorNode = htmlParser3.getNodeById("CVCAIQH7");
		if (grantorNode == null) {
			grantor = getColumnValueByIdOrRegex("", "(?i)Case\\s+name", htmlParser3);
		}
		if (grantorNode != null) {
			grantor = grantorNode.toPlainTextString().trim();
		}
		if (StringUtils.isNotEmpty(grantor)) {
			resultMap.put("tmpGrantor", grantor);
		}
		ro.cst.tsearch.servers.functions.TNShelbyPR.parseNames(resultMap);

		return null;
	}

	@Override
	protected void setModulesForGoBackOneLevelSearch(TSServerInfo serverInfo) {

		ConfigurableNameIterator nameIterator = null;
		SearchAttributes sa = InstanceManager.getManager().getCurrentInstance(searchId).getCrtSearchContext().getSa();
		List<TSServerInfoModule> modules = new ArrayList<TSServerInfoModule>();
		TSServerInfoModule module;
		GBManager gbm = (GBManager) sa.getObjectAtribute(SearchAttributes.GB_MANAGER_OBJECT);
		for (String id : gbm.getGbTransfers()) {
			module = new TSServerInfoModule(serverInfo.getModule(TSServerInfo.NAME_MODULE_IDX));
			module.setIndexInGB(id);
			module.setTypeSearchGB("grantor");
			module.clearSaKeys();
			module.setSaObjKey(SearchAttributes.GB_MANAGER_OBJECT);

			GenericNameFilter defaultNameFilter = (GenericNameFilter) NameFilterFactory.getDefaultNameFilter(SearchAttributes.GB_MANAGER_OBJECT, searchId,
					module);
			module.addFilter(defaultNameFilter);

			module.setIteratorType(0, FunctionStatesIterator.ITERATOR_TYPE_LCF_NAME_FAKE);
			nameIterator = (ConfigurableNameIterator) ModuleStatesIteratorFactory
					.getConfigurableNameIterator(module, searchId, new String[] { "L, F;;" });
			nameIterator.clearSearchedNames();
			nameIterator.setInitAgain(true);
			module.addIterator(nameIterator);

			modules.add(module);

			if (gbm.getNamesForBrokenChain(id, searchId).size() > 0) {

				module = new TSServerInfoModule(serverInfo.getModule(TSServerInfo.NAME_MODULE_IDX));
				module.setIndexInGB(id);
				module.setTypeSearchGB("grantee");
				module.clearSaKeys();
				module.setSaObjKey(SearchAttributes.GB_MANAGER_OBJECT);

				module.setIteratorType(0, FunctionStatesIterator.ITERATOR_TYPE_LCF_NAME_FAKE);
				nameIterator = (ConfigurableNameIterator) ModuleStatesIteratorFactory
						.getConfigurableNameIterator(module, searchId, new String[] { "L, F;;" });
				nameIterator.clearSearchedNames();
				nameIterator.setInitAgain(true);
				module.addIterator(nameIterator);

				modules.add(module);

			}
		}
		serverInfo.setModulesForGoBackOneLevelSearch(modules);
	}

	@Override
	protected void setModulesForAutoSearch(TSServerInfo serverInfo) {

		List<TSServerInfoModule> modules = new ArrayList<TSServerInfoModule>();

		String[] keys = new String[] {
				SearchAttributes.OWNER_OBJECT,
				SearchAttributes.BUYER_OBJECT
		};

		Search search = InstanceManager.getManager().getCurrentInstance(searchId).getCrtSearchContext();

		for (String key : keys) {
			if (!(SearchAttributes.BUYER_OBJECT.equals(key) && search.isProductType(SearchAttributes.SEARCH_PROD_REFINANCE))) {

				TSServerInfoModule module = new TSServerInfoModule(serverInfo.getModule(TSServerInfo.NAME_MODULE_IDX));
				module.clearSaKeys();
				module.setSaObjKey(key);

				module.clearIteratorTypes();
				module.setIteratorType(0, FunctionStatesIterator.ITERATOR_TYPE_LCF_NAME_FAKE);
				ConfigurableNameIterator iterator = new ConfigurableNameIterator(searchId, new String[] { "L, F;;" });
				iterator.clearSearchedNames();
				iterator.setInitAgain(true);
				module.addIterator(iterator);

				GenericNameFilter nameFilter = (GenericNameFilter) NameFilterFactory.getDefaultNameFilter(key, searchId, module);
				module.addFilter(nameFilter);
				addBetweenDateTest(module, true, true, true);

				modules.add(module);
			}
		}

		serverInfo.setModulesForAutoSearch(modules);
	}
}
