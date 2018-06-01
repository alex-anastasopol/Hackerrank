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

import org.apache.commons.lang.StringEscapeUtils;
import org.htmlparser.Node;
import org.htmlparser.filters.HasAttributeFilter;
import org.htmlparser.filters.RegexFilter;
import org.htmlparser.filters.TagNameFilter;
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
import ro.cst.tsearch.search.filter.RejectAlreadyPresentFilterResponse;
import ro.cst.tsearch.search.filter.newfilters.name.GenericNameFilter;
import ro.cst.tsearch.search.filter.newfilters.name.NameFilterFactory;
import ro.cst.tsearch.search.filter.newfilters.name.NameFilterForNext;
import ro.cst.tsearch.search.module.ConfigurableNameIterator;
import ro.cst.tsearch.servers.TSConnectionURL;
import ro.cst.tsearch.servers.info.TSServerInfo;
import ro.cst.tsearch.servers.info.TSServerInfoModule;
import ro.cst.tsearch.servers.response.CourtDocumentIdentificationSet.CourtDocumentIdentificationSetKey;
import ro.cst.tsearch.servers.response.ImageLinkInPage;
import ro.cst.tsearch.servers.response.LinkInPage;
import ro.cst.tsearch.servers.response.OtherInformationSet.OtherInformationSetKey;
import ro.cst.tsearch.servers.response.ParsedResponse;
import ro.cst.tsearch.servers.response.SaleDataSet.SaleDataSetKey;
import ro.cst.tsearch.servers.response.ServerResponse;
import ro.cst.tsearch.servlet.URLConnectionReader;
import ro.cst.tsearch.utils.InstanceManager;
import ro.cst.tsearch.utils.StringUtils;
import ro.cst.tsearch.utils.Tidy;

import com.stewart.ats.base.document.DocumentI;
import com.stewart.ats.base.document.RegisterDocumentI;

public class OHDelawareCO extends TSServer {

	private static final long	serialVersionUID	= -6550207391242759488L;

	public OHDelawareCO(String rsRequestSolverName, String rsSitePath, String rsServerID, String rsPrmNameLink, long searchId, int mid) {

		super(rsRequestSolverName, rsSitePath, rsServerID, rsPrmNameLink, searchId, mid);
		resultType = MULTIPLE_RESULT_TYPE;
	}

	@Override
	protected void ParseResponse(String sAction, ServerResponse response, int viParseID) throws ServerResponseException {

		String rsResponse = response.getResult();
		ParsedResponse parsedResponse = response.getParsedResponse();

		switch (viParseID) {
		case ID_SEARCH_BY_NAME:

			if (rsResponse.indexOf("No data was found matching search results.") > -1) {
				response.getParsedResponse().setError("No data was found matching search results.");
				response.getParsedResponse().setAttribute(ParsedResponse.SHOW_ONLY_ERROR_MESSAGE, "true");
				return;
			} else if (rsResponse.indexOf("Result set is too large, please further qualify your search.") > -1) {
				response.getParsedResponse().setError("Result set is too large, please further qualify your search.");
				response.getParsedResponse().setAttribute(ParsedResponse.SHOW_ONLY_ERROR_MESSAGE, "true");
				return;
			}
			if (rsResponse.contains("Last name must contain at least two characters.")) {
				response.getParsedResponse().setError("Last name must contain at least two characters.");
				response.getParsedResponse().setAttribute(ParsedResponse.SHOW_ONLY_ERROR_MESSAGE, "true");
				return;
			}
			if (rsResponse.contains("At least one of the following is required to perform a search")) {
				response.getParsedResponse().setError("At least one of the following is required to perform a search: Last Name, Case Number, Company Name");
				response.getParsedResponse().setAttribute(ParsedResponse.SHOW_ONLY_ERROR_MESSAGE, "true");
				return;
			}

			StringBuilder outputTable = new StringBuilder();
			Collection<ParsedResponse> smartParsedResponses = smartParseIntermediary(response, rsResponse, outputTable);

			if (smartParsedResponses.size() == 0) {
				return;
			}

			if (smartParsedResponses.size() > 0) {
				parsedResponse.setResultRows(new Vector<ParsedResponse>(smartParsedResponses));
				parsedResponse.setOnlyResponse(outputTable.toString());
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
		case ID_GET_LINK:
			if (sAction.indexOf("pamw2000.o_case_sum") > -1) {
				ParseResponse(sAction, response, ID_DETAILS);
			} else {
				ParseResponse(sAction, response, ID_SEARCH_BY_NAME);
			}
			break;

		case ID_DETAILS:
		case ID_SAVE_TO_TSD:

			StringBuilder instrumentNumber = new StringBuilder();
			String details = getDetails(rsResponse, instrumentNumber);

			String instNo = instrumentNumber.toString();

			if (viParseID == ID_DETAILS) {
				String originalLink = sAction.replace("?", "&") + "&" + response.getRawQuerry();
				String sSave2TSDLink = getLinkPrefix(TSConnectionURL.idPOST) + originalLink;

				HashMap<String, String> data = new HashMap<String, String>();

				HtmlParser3 htmlParser3 = new HtmlParser3(details);
				Node summaryNode = htmlParser3.getNodeById("summary");
				if (summaryNode != null) {
					String documentType = "";
					NodeList titleNodeList = summaryNode.getChildren().extractAllNodesThatMatch(new RegexFilter("(?is)\\s*-\\s*Summary"), true);
					if (titleNodeList.size() > 0) {
						documentType = titleNodeList.elementAt(0).toPlainTextString().replaceFirst("(?is)^\\s*(.*?)\\s*-\\s*Summary\\s*$", "$1");
						data.put("type", documentType);
					}
					NodeList caseAttributesNodeList = summaryNode.getChildren().extractAllNodesThatMatch(new RegexFilter("Case Attributes"), true);
					if (caseAttributesNodeList.size() > 0) {
						Node caseAttributesNode = HtmlParser3.getFirstParentTag(caseAttributesNodeList.elementAt(0), TableTag.class);

						if (caseAttributesNode != null) {
							caseAttributesNodeList = caseAttributesNode.getChildren();
							instNo = HtmlParser3.getValueFromAbsoluteCell(0, 1, HtmlParser3.findNode(caseAttributesNodeList, "Number"), "", true);
							instNo = instNo.replaceFirst("(?is)<input[^>]*>", "").trim();
							if (!instNo.isEmpty()) {
								data.put("instrno", instNo);
							}
						}
					}
				}

				data.put("dataSource", getDataSite().getSiteTypeAbrev());

				if (isInstrumentSaved(instNo, null, data)) {
					details += CreateFileAlreadyInTSD();
				}
				else {
					mSearch.addInMemoryDoc(sSave2TSDLink, details);
					details = addSaveToTsdButton(details, sSave2TSDLink, viParseID);
				}

				parsedResponse.setPageLink(new LinkInPage(sSave2TSDLink, originalLink, TSServer.REQUEST_SAVE_TO_TSD));
				parsedResponse.setResponse(details);
			} else {
				String filename = instrumentNumber.toString().replaceAll("/", "") + ".html";
				smartParseDetails(response, details);

				msSaveToTSDFileName = filename;
				parsedResponse.setFileName(getServerTypeDirectory() + msSaveToTSDFileName);
				parsedResponse.setResponse(details);

				msSaveToTSDResponce = details + CreateFileAlreadyInTSD();
			}
		}

	}

	public Collection<ParsedResponse> smartParseIntermediary(ServerResponse response, String table, StringBuilder outputTable) {
		Vector<ParsedResponse> intermediaryResponse = new Vector<ParsedResponse>();

		try {
			int numberOfUncheckedElements = 0;

			table = StringEscapeUtils.unescapeHtml(table);
			table = Tidy.tidyParse(table, null);

			HtmlParser3 htmlParser3 = new HtmlParser3(table);
			NodeList nodeList = htmlParser3.getNodeList();
			NodeList mainTableList = nodeList.extractAllNodesThatMatch(new TagNameFilter("table"), true)
					.extractAllNodesThatMatch(new HasAttributeFilter("width", "100%"), true)
					.extractAllNodesThatMatch(new HasAttributeFilter("border", "0"), true);
			if (mainTableList.size() > 0) {

				mainTableList = mainTableList.extractAllNodesThatMatch(new HasAttributeFilter("class", "groupheader"), true)
						.extractAllNodesThatMatch(new HasAttributeFilter("valign", "TOP"), true);
				Node pagingNode = null;
				boolean hasNext = false;
				boolean hasPrev = false;
				if (mainTableList.size() > 0) {
					// paging
					pagingNode = nodeList.extractAllNodesThatMatch(new RegexFilter("\\d+-\\d+\\s+of\\s+\\d+"), true).elementAt(0);
					if (pagingNode != null) {
						TableColumn pagingColumn = HtmlParser3.getFirstParentTag(pagingNode, TableColumn.class);
						if (pagingColumn != null) {
							NodeList previousNodeList = pagingColumn.getChildren().extractAllNodesThatMatch(new HasAttributeFilter("Title", "Previous"), true);
							if (previousNodeList.size() > 0) {
								hasPrev = true;
							}

							NodeList nextNodeList = pagingColumn.getChildren().extractAllNodesThatMatch(new HasAttributeFilter("Name", "NEXT.PROFILE.CRTV"),
									true);
							if (nextNodeList.size() > 0) {
								hasNext = true;
							}

						}
					}

					Node mainTable = mainTableList.elementAt(0).getParent();

					if (mainTable instanceof TableTag) {

						TableRow[] rows = ((TableTag) mainTable).getRows();
						String header = "";
						String footer = "";

						for (int i = 1; i < rows.length; i++) {
							TableRow row = rows[i];

							if (row.getColumnCount() == 6) {
								TableColumn[] cols = row.getColumns();

								// remove duplicate lines
								if (i < rows.length - 1) {
									boolean differentRow = false;
									TableRow nextRow = rows[i + 1];
									if (nextRow.getColumnCount() == 6) {
										TableColumn[] nextRowCols = nextRow.getColumns();
										for (int j = 0; j < cols.length; j++) {
											if (!cols[j].toPlainTextString().trim().equals(nextRowCols[j].toPlainTextString().trim())) {
												differentRow = true;
												break;
											}
										}
									}

									if (!differentRow) {// if next row is identical to current row, skip it
										rows[i + 1] = new TableRow();
										i++;
									}
								}
							}
						}

						// group the rows which have the same Case Number
						LinkedHashMap<String, List<TableRow>> rowsMap = new LinkedHashMap<String, List<TableRow>>();

						int len = rows.length;
						for (int i = 1; i < len; i++) {

							TableRow row = rows[i];
							TableColumn[] cols = row.getColumns();
							if (row.getChildCount() > 0) {// skip empty rows (they are removed duplicates)
								if (row.getColumnCount() >= 6) {
									String key = cols[5].toPlainTextString().trim();
									if (rowsMap.containsKey(key)) { // row already added
										List<TableRow> value = rowsMap.get(key);
										value.add(row);
									} else { // add row
										List<TableRow> value = new ArrayList<TableRow>();
										value.add(row);
										rowsMap.put(key, value);
									}
								}
							}
						}

						Iterator<Entry<String, List<TableRow>>> it = rowsMap.entrySet().iterator();
						while (it.hasNext()) {

							Map.Entry<String, List<TableRow>> pair = (Map.Entry<String, List<TableRow>>) it.next();
							List<TableRow> value = pair.getValue();

							StringBuilder sb = new StringBuilder();

							int idx = 0;

							String detailsLink = CreatePartialLink(TSConnectionURL.idGET) + getBaseLink() + "pa.urd/";

							String caseNumberColumn = value.get(0).getColumns()[5].toHtml();
							Matcher ma = Pattern.compile("(?is)<a[^>]*href=\"?([^\\s>\"]+)\"?").matcher(caseNumberColumn);
							if (ma.find()) {
								detailsLink += ma.group(1);
							}
							caseNumberColumn = caseNumberColumn.replaceFirst("(?is)<a[^>]+href=\"?([^\\s>\"]+)\"?[^>]*>", "<a href=\"" + detailsLink + "\">");

							sb.append(caseNumberColumn);

							// Party/Company
							StringBuilder sbPartyName = new StringBuilder();
							sbPartyName.append("<td><table>");
							for (int j = 0; j < value.size(); j++) {
								sbPartyName.append("<tr>");
								sbPartyName.append(value.get(j).getColumns()[idx].toHtml());
								sbPartyName.append("</tr>");
							}
							sbPartyName.append("</table></td>");
							sb.append(sbPartyName);
							idx++;

							// Affl
							StringBuilder sbAffl = new StringBuilder();
							sbAffl.append("<td><table>");
							for (int j = 0; j < value.size(); j++) {
								sbAffl.append("<tr>");
								sbAffl.append(value.get(j).getColumns()[idx].toHtml());
								sbAffl.append("</tr>");
							}
							sbAffl.append("</table></td>");
							sb.append(sbAffl);
							idx++;

							// Party Type
							StringBuilder sbPartyType = new StringBuilder();
							sbPartyType.append("<td><table>");
							for (int j = 0; j < value.size(); j++) {
								sbPartyType.append("<tr>");
								sbPartyType.append(value.get(j).getColumns()[idx].toHtml());
								sbPartyType.append("</tr>");
							}
							sbPartyType.append("</table></td>");
							sb.append(sbPartyType);
							idx++;

							// Date of Birth
							StringBuilder sbDOB = new StringBuilder();
							sbDOB.append("<td><table>");
							for (int j = 0; j < value.size(); j++) {
								sbDOB.append("<tr>");
								sbDOB.append(value.get(j).getColumns()[idx].toHtml());
								sbDOB.append("</tr>");
							}
							sbDOB.append("</table></td>");
							sb.append(sbDOB);
							idx++;

							// Case Status
							sb.append(value.get(0).getColumns()[idx].toHtml());

							String htmlRow = sb.toString();
							// htmlRow = htmlRow.replaceAll("(?is)<td[^>]+>", "<td>");

							ResultMap resultMap = ro.cst.tsearch.servers.functions.OHDelawareCO.parseIntermediaryRow(htmlRow);
							resultMap.put(OtherInformationSetKey.SRC_TYPE.getKeyName(), getDataSite().getSiteTypeAbrev());
							resultMap.removeTempDef();

							ParsedResponse currentResponse = new ParsedResponse();

							currentResponse.setPageLink(new LinkInPage(detailsLink, detailsLink, TSServer.REQUEST_SAVE_TO_TSD));

							String instrNo = org.apache.commons.lang.StringUtils.defaultString((String) resultMap.get(SaleDataSetKey.INSTRUMENT_NUMBER
									.getKeyName())).replaceAll("\\s+", "");

							String checkBox = "checked";

							HashMap<String, String> data = new HashMap<String, String>();

							data.put("instrno", instrNo);

							String docType = getDocTypeIntermediary(instrNo);

							if (!docType.isEmpty()) {
								data.put("type", docType);
								resultMap.put(SaleDataSetKey.DOCUMENT_TYPE.getKeyName(), docType);
							}

							if (isInstrumentSaved(instrNo, null, data, false)) {
								checkBox = "saved";
							} else {
								numberOfUncheckedElements++;
								LinkInPage linkInPage = new LinkInPage(detailsLink, detailsLink, TSServer.REQUEST_SAVE_TO_TSD);
								checkBox = "<input type='checkbox' name='docLink' value='" + detailsLink + "'>";
								currentResponse.setPageLink(linkInPage);
							}

							String rowHtml = "<tr><td align=\"center\">" + checkBox + "</td>" + htmlRow + "</tr>";

							// remove width
							rowHtml = rowHtml.replaceAll("width=\"[^\"]*\"", "");

							currentResponse.setAttribute(ParsedResponse.SERVER_ROW_RESPONSE, rowHtml);
							currentResponse.setOnlyResponse(rowHtml);

							Bridge bridge = new Bridge(currentResponse, resultMap, searchId);

							DocumentI document = (RegisterDocumentI) bridge.importData();
							currentResponse.setDocument(document);

							intermediaryResponse.add(currentResponse);
						}

						String next = "";
						if (hasNext) {
							next = hasNext ? "<a href=\"" + getNextLink(nodeList) + "\">Next</a>" : "";
						}
						String previous = hasPrev ? "<a href=\"javascript:void(history.go(-1))\">Previous</a>" : "";

						String tableHeader = "<th width=\"12%\"><b>Case Number</b></th><th width=\"38%\"><b>Party</b></th>"
								+ "<th width=\"12%\"><b>Affl</b></th><th width=\"12%\"><b>Party Type</b></th>"
								+ "<th width=\"12%\"><b>D.O.B</b></th><th width=\"12%\"><b>Case Status</b></th>";

						header = "<div align=\"center\">" + pagingNode.toHtml() + "<strong>&nbsp;&nbsp;&nbsp;&nbsp;" + previous + "&nbsp;&nbsp;&nbsp;&nbsp;"
								+ next + "</strong></div><br><table border=\"1px\" style=\"border:1px;border-collapse:collapse\" align=\"center\">" +
								"<tr><th width=\"2%\">" + SELECT_ALL_CHECKBOXES + "</th>" + tableHeader + "</tr>";

						footer += "</table></td></tr></table><br>";

						response.getParsedResponse().setHeader(header);
						response.getParsedResponse().setFooter(footer);

						outputTable.append(table);
						SetAttribute(NUMBER_OF_UNSAVED_DOCUMENTS, numberOfUncheckedElements);
					}
				}
			}
		} catch (Throwable t) {
			logger.error("Error while parsing intermediary data", t);
		}
		return intermediaryResponse;
	}

	private String getDocTypeIntermediary(String instrNo) {
		String docType = "";

		String docTypeAbbrev = instrNo.replaceFirst("^\\d+\\s*([A-Z]{2,3})\\b.*", "$1");

		if (docTypeAbbrev.equals("CAA")) {
			docType = "CRIMINAL APPEAL, COMMON PLEAS";
		} else if (docTypeAbbrev.equals("CAC")) {
			docType = "CRIMINAL APPEAL, MUNICIPAL COURT";
		} else if (docTypeAbbrev.equals("CAD")) {
			docType = "ORIGINAL ACTION";
		} else if (docTypeAbbrev.equals("CAE")) {
			docType = "CIVIL APPEAL, COMMON PLEAS";
		} else if (docTypeAbbrev.equals("CAF")) {
			docType = "DOMESTIC APPEAL, C.P./PRO/JUV";
		} else if (docTypeAbbrev.equals("CAG")) {
			docType = "CIVIL APPEAL, MUNICIPAL COURT";
		} else if (docTypeAbbrev.equals("CAH")) {
			docType = "ADMINISTRATIVE APPEAL";
		} else if (docTypeAbbrev.equals("CJ")) {
			docType = "CERTIFICATE OF JUDGMENT";
		} else if (docTypeAbbrev.equals("CR")) {
			docType = "CRIMINAL";
		} else if (docTypeAbbrev.equals("CV")) {
			docType = "CIVIL";
		} else if (docTypeAbbrev.equals("DO")) {
			docType = "DOMESTIC";
		} else if (docTypeAbbrev.equals("DR")) {
			docType = "DIVORCE";
		} else if (docTypeAbbrev.equals("DS")) {
			docType = "DISSOLUTION";
		} else if (docTypeAbbrev.equals("DU")) {
			docType = "DOMESTIC - SUPPORT";
		} else if (docTypeAbbrev.equals("DV")) {
			docType = "DOMESTIC VIOLENCE";
		} else if (docTypeAbbrev.equals("ST")) {
			docType = "STATE TAX LIEN";
		} else if (docTypeAbbrev.equals("TL")) {
			docType = "ELECTRONIC TAX LIENS";
		} else {
			docType = "MISCELLANEOUS";
		}
		return docType;
	}

	private String getNextLink(NodeList nodeList) {
		StringBuilder nextLink = new StringBuilder();
		try {
			String tagValuePattern = "(?is).*value=\"([^\"]*)\".*";
			String baseLink = CreatePartialLink(TSConnectionURL.idPOST) + dataSite.getLink()
					+ "pa.urd/PAMW6512?NEXT.PROFILE.CRTV.x=&NEXT.PROFILE.CRTV.y=";// these two params can be anything, but are required

			String[] paramNames = { "INIT_SORT.SRCHPR03.PAM", "ORDER_ALL.PROFILE.CRTV", "TIMES_RESORTED.PROFILE.CRTV", "PAGE_NBR.PROFILE.CRTV",
					"LAST_NAME.PAPROFILE.PAM", "LAST_SEQ.PROFILE.CRTV", "LAST_CASE_ID.PROFILE.CRTV", "LAST_AFFL_ID.PROFILE.CRTV", "LAST_IDNT_ID.PROFILE.CRTV",
					"TOTAL_ROWS.PROFILE.CRTV", "FMT_CASE_NBR.PAPROFILE.PAM" };

			nextLink.append(baseLink);

			if (nodeList.size() > 0) {
				for (int i = 0; i < paramNames.length; i++) {
					Node param = nodeList.extractAllNodesThatMatch(new HasAttributeFilter("name", paramNames[i]), true).elementAt(0);

					if (param != null) {
						String paramValue = param.toHtml().replaceFirst(tagValuePattern, "$1");
						if (!paramValue.isEmpty()) {
							nextLink.append("&" + paramNames[i] + "=");
							nextLink.append(paramValue);
						}
					}
				}
			}

		} catch (Exception e) {
			e.printStackTrace();
		}

		return nextLink.toString();
	}

	private String getDetails(String page, StringBuilder instrumentNumber) {

		if (!page.toLowerCase().contains("<html")) {
			return page;
		}

		StringBuilder details = new StringBuilder();

		try {
			String summaryTable = getMainTable(page);
			if (!summaryTable.isEmpty()) {
				details.append("<table id=\"details\" align=\"center\" border=\"1\"><tr id=\"summary\"><td>" + summaryTable + "</td></tr>");
			}
			HtmlParser3 htmlParser3 = new HtmlParser3(page);
			NodeList summaryNodeList = htmlParser3.getNodeList();
			summaryNodeList = summaryNodeList.extractAllNodesThatMatch(new TagNameFilter("form"), true);

			if (summaryNodeList.size() > 0) {

				NodeList areaList = summaryNodeList.extractAllNodesThatMatch(new TagNameFilter("area"), true)
						.extractAllNodesThatMatch(new HasAttributeFilter("class", "hlinktext"), true);
				int areaListSize = areaList.size();

				if (areaListSize > 5) {
					String[] links = new String[10];
					String[] linkContents = new String[10];
					String baseLink = getBaseLink() + "pa.urd/";
					String areaTitlePattern = ".*?title=\\s*\"(.*?)\"\\s*href.*";
					String areaHrefPattern = ".*?href=\\s*(.*?)\\s*>";
					for (int i = 0; i < areaListSize - 1; i++) {// last link is new search, and is not needed
						links[i] = baseLink + areaList.elementAt(i).toHtml().replaceAll(areaHrefPattern, "$1");
						linkContents[i] = getLinkContents(links[i]);
					}

					for (int i = 0; i < areaListSize - 1; i++) {

						String table = getMainTable(linkContents[i]);

						if (!table.isEmpty()) {
							String linkTitle = areaList.elementAt(i).toHtml().replaceAll(areaTitlePattern, "$1");
							details.append("<tr><td id=\"" + linkTitle + "\">" + table + "</td></tr>");

							// sub-links:
							if (linkTitle.contains("Cost Summary")) {
								htmlParser3 = new HtmlParser3(table);
								NodeList costNodeList = htmlParser3.getNodeList();
								if (costNodeList.size() > 0) {
									NodeList costAreaList = costNodeList.extractAllNodesThatMatch(new TagNameFilter("area"), true)
											.extractAllNodesThatMatch(new HasAttributeFilter("class", "hlinktext"), true);
									int costAreaListSize = costAreaList.size();

									if (costAreaListSize > 0) {

										for (int j = 0; j < costAreaListSize; j++) {
											String title = costAreaList.elementAt(j).toHtml().replaceAll(areaTitlePattern, "$1");
											if (title.contains("Checks List")) {
												String checksContents = getAreaLinkContents(areaHrefPattern, costAreaList, j, baseLink);
												if (!checksContents.isEmpty()) {
													details.append("<tr id=\"checksList\"><td>"
															+ checksContents + "</td></tr>");
												}
											}
											else if (title.contains("Receipt List")) {
												String receiptsContents = getAreaLinkContents(areaHrefPattern, costAreaList, j, baseLink);
												if (!receiptsContents.isEmpty()) {
													details.append("<tr id=\"receiptsList\"><td>" + receiptsContents + "</td></tr>");
												}
											}
											else if (title.contains("Financial Dockets Summary")) {
												String financialDockets = getAreaLinkContents(areaHrefPattern, costAreaList, j, baseLink);
												if (!financialDockets.isEmpty()) {
													details.append("<tr id=\"financialDockets\"><td>"
															+ financialDockets.replaceFirst("(?is)<tr[^>]*>.*?Search\\s*Criteria.*?</tr>", "") + "</td></tr>");
												}
											}
										}
									}
								}
							}
							else if (linkTitle.contains("Disposition")) {
								htmlParser3 = new HtmlParser3(table);
								NodeList dispositionNodeList = htmlParser3.getNodeList();
								if (dispositionNodeList.size() > 0) {
									NodeList dispositionLinkNodes = dispositionNodeList.extractAllNodesThatMatch(
											new HasAttributeFilter("class", "groupheader"),
											true);
									int dispositionLinkNodesSize = dispositionLinkNodes.size();

									if (dispositionLinkNodesSize > 0 && dispositionLinkNodes.elementAt(0) instanceof TableRow) {
										TableRow[] dispositionLinkRows = ((TableTag) dispositionLinkNodes.elementAt(0).getParent()).getRows();
										for (int j = 1; j < dispositionLinkNodesSize; j++) {
											TableColumn linkColumn = dispositionLinkRows[j].getColumns()[0];
											String link = baseLink + linkColumn.toHtml().replaceFirst(areaHrefPattern + ".*", "$1");
											link = link.substring(0, link.indexOf("|"));// remove illegal character "|"
											String dispositionLinkContents = getLinkContents(link);
											String tableDisposition = getMainTable(dispositionLinkContents);
											if (!tableDisposition.isEmpty()) {
												details.append("<tr id=\"disposition\"><td>" + tableDisposition + "</td></tr>");
											}
										}
									}
								}
							}
						}
					}
				}
			}

		} catch (Exception e) {
			e.printStackTrace();
		}
		details.append("</table>");

		String detailsString = details.toString().replaceAll("[^\\p{Alnum}\\p{Punct}\\s]", "");

		return detailsString
				.replaceAll("(?is)(<td[^>]*>)(?:\\s*&nbsp;)*\\s*(</td>)", "")
				.replaceAll("(?is)(class=\"?groupheader\"?)", "$1 style=\"font-weight:bold; color:white; background-color:green;\"")// make headers green
				.replaceAll("(?is)<img[^>]*usemap=[^>]*>", "") // remove all images but dockets images
				.replaceAll("(?is)<img\\s*src=.*?efile.jpg\"[^>]*>", "")
				.replaceAll("(?is)<img[^>]*>", "View Image") // replace dockets image with text
				.replaceAll("(?is)(<input)", "$1 style=\"display:none;\"")// hide all inputs
				.replaceAll("(?is)<tr>\\s*</tr>", "")
				.replaceAll("(?is)<select\\s*name=[^>]*>.*?</select>", "")
				.replaceAll("(?i)\\s+width=\"[^\"]*\"", "")
				.replaceAll("(?i)(<table)", "$1 width=\"100%\"")
				.replaceAll("(?is)<tr class=\"groupheader\"[^>]*>(\\s*<td[^>]*>\\s*</td>)*\\s*</tr>", "") // remove empty colored rows
				.replaceAll("(?i)<a\\s*href=[^>]*>", "") // remove all links but images links
				.replaceFirst("(?is)(<table width=)\"100%\"(\\s*id=\"details\")", "$1\"700px\"$2");
	}

	private String getAreaLinkContents(String areaHrefPattern, NodeList areaList, int j, String baseLink) {
		String table = "";

		String link = baseLink + areaList.elementAt(j).toHtml().replaceAll(areaHrefPattern, "$1");
		String linkContents = getLinkContents(link);

		table = getMainTable(linkContents);
		return table;
	}

	private String getMainTable(String linkContents) {
		HtmlParser3 htmlParser3;
		htmlParser3 = new HtmlParser3(linkContents);
		String table = "";
		NodeList linkNodeList = htmlParser3.getNodeList();
		linkNodeList = linkNodeList.extractAllNodesThatMatch(new TagNameFilter("form"), true);

		if (linkNodeList.size() > 0) {
			Node linkNode = HtmlParser3.getFirstTag(linkNodeList, TableTag.class, true);

			if (linkNode != null) {
				TableTag contentTable = (TableTag) linkNode;
				contentTable.setAttribute("align", "center");
				table = contentTable.toHtml();
			}
		}
		return table;
	}

	@Override
	protected Object parseAndFillResultMap(ServerResponse response, String detailsHtml, ResultMap resultMap) {
		try {
			detailsHtml = StringEscapeUtils.unescapeHtml(detailsHtml);
			resultMap.put(OtherInformationSetKey.SRC_TYPE.getKeyName(), getDataSite().getSiteTypeAbrev());

			HtmlParser3 htmlParser3 = new HtmlParser3(detailsHtml);

			Node summaryNode = htmlParser3.getNodeById("summary");
			if (summaryNode != null) {

				NodeList titleNodeList = summaryNode.getChildren().extractAllNodesThatMatch(new RegexFilter("(?is)\\s*-\\s*Summary"), true);

				NodeList caseAttributesNodeList = summaryNode.getChildren().extractAllNodesThatMatch(new RegexFilter("Case Attributes"), true);

				if (caseAttributesNodeList.size() > 0) {
					Node caseAttributesNode = HtmlParser3.getFirstParentTag(caseAttributesNodeList.elementAt(0), TableTag.class);
					if (caseAttributesNode != null) {
						caseAttributesNodeList = caseAttributesNode.getChildren();

						String filed = HtmlParser3.getValueFromAbsoluteCell(0, 1, HtmlParser3.findNode(caseAttributesNodeList, "Filed"), "", true);
						if (!filed.isEmpty()) {
							filed = filed.replaceFirst("(?is)<input[^>]*>", "").trim();
							if (filed.matches("\\d{2}/\\d{2}/\\d+"))
								resultMap.put(CourtDocumentIdentificationSetKey.FILLING_DATE.getKeyName(), filed);
							resultMap.put(SaleDataSetKey.RECORDED_DATE.getKeyName(), filed);
						}

						String caseNumber = HtmlParser3.getValueFromAbsoluteCell(0, 1, HtmlParser3.findNode(caseAttributesNodeList, "Number"), "", true);
						caseNumber = org.apache.commons.lang.StringUtils.defaultString(caseNumber.replaceFirst("(?is)<input[^>]*>", "").trim());

						if (!caseNumber.isEmpty()) {
							resultMap.put(CourtDocumentIdentificationSetKey.CASE_NUMBER.getKeyName(), caseNumber);
							resultMap.put(SaleDataSetKey.INSTRUMENT_NUMBER.getKeyName(), caseNumber);
						}

						else {
							if (titleNodeList.size() > 0) {
								Node caseNumberNode = HtmlParser3.getFirstParentTag(titleNodeList.elementAt(0), TableRow.class);
								if (caseNumberNode != null) {
									caseNumberNode = HtmlParser3.getFirstParentTag(caseNumberNode, TableRow.class);
									if (caseNumberNode != null && caseNumberNode instanceof TableRow) {
										Node caseNumberTable = caseNumberNode.getParent();
										if (caseNumberTable instanceof TableTag) {
											caseNumber = ((TableTag) caseNumberTable).getRow(2).toPlainTextString();
											caseNumber = caseNumber.replaceFirst("(?is)^\\s*(.*\\d(?:\\s*\\w)?).*", "$1");
											if (!caseNumber.isEmpty()) {
												resultMap.put(CourtDocumentIdentificationSetKey.CASE_NUMBER.getKeyName(), caseNumber);
												resultMap.put(SaleDataSetKey.INSTRUMENT_NUMBER.getKeyName(), caseNumber);
											}
										}

									}
								}
							}
						}
					}
				}

				if (titleNodeList.size() > 0) {
					String documentType = titleNodeList.elementAt(0).toPlainTextString().replaceFirst("(?is)^\\s*(.*?)\\s*-\\s*Summary\\s*$", "$1");
					if (!documentType.isEmpty()) {
						resultMap.put(SaleDataSetKey.DOCUMENT_TYPE.getKeyName(), documentType);
						resultMap.put(CourtDocumentIdentificationSetKey.CASE_TYPE.getKeyName(), documentType);
					}
				}
			}

			NodeList judgeNodeList = summaryNode.getChildren().extractAllNodesThatMatch(new RegexFilter("(?is)Case\\s+Information"), true);
			if (judgeNodeList.size() > 0) {
				Node judgeNode = HtmlParser3.getFirstParentTag(judgeNodeList.elementAt(0), TableTag.class);
				String judge = HtmlParser3.getValueFromAbsoluteCell(0, 1,
						HtmlParser3.findNode(judgeNode.getChildren(), "Judge"), "", true).trim();
				if (!judge.isEmpty()) {
					resultMap.put(CourtDocumentIdentificationSetKey.JUDGE_NAME.getKeyName(), judge);
				}
			}

			Node dispositionNode = htmlParser3.getNodeById("disposition");
			if (dispositionNode != null) {
				NodeList dispositionNodeList = dispositionNode.getChildren().extractAllNodesThatMatch(new HasAttributeFilter("name", "DSCR.DSPCD.CRTV"), true);
				if (dispositionNodeList.size() > 0) {
					String disposition = dispositionNodeList.elementAt(0).getParent().toPlainTextString().replaceAll("(?is)<input[^>]*>", "").trim();
					if (!disposition.isEmpty()) {
						resultMap.put(CourtDocumentIdentificationSetKey.DISPOSITION.getKeyName(), disposition);
					}

				}

				dispositionNodeList = dispositionNode.getChildren().extractAllNodesThatMatch(new HasAttributeFilter("name", "DSP_DT.DSP.CRTV"), true);

				if (dispositionNodeList.size() > 0) {
					String dispositionDate = dispositionNodeList.elementAt(0).getParent().toPlainTextString().replaceAll("(?is)<input[^>]*>", "").trim();
					if (!dispositionDate.isEmpty() && dispositionDate.matches("\\d{2}/\\d{2}/\\d+")) {
						resultMap.put(CourtDocumentIdentificationSetKey.DISPOSITION_DATE.getKeyName(), dispositionDate);
					}
				}

				if (resultMap.get(CourtDocumentIdentificationSetKey.JUDGE_NAME.getKeyName()) == null) {
					dispositionNodeList = dispositionNode.getChildren()
							.extractAllNodesThatMatch(new HasAttributeFilter("name", "FULL_NAME.IDNTJDG.CRTV"), true);

					if (dispositionNodeList.size() > 0) {
						String judge = dispositionNodeList.elementAt(0).getParent().toPlainTextString().replaceAll("(?is)<input[^>]*>", "").trim();
						if (!judge.isEmpty()) {
							resultMap.put(CourtDocumentIdentificationSetKey.JUDGE_NAME.getKeyName(), judge);
						}
					}
				}
			}

			// parties
			StringBuilder grantor = new StringBuilder();
			StringBuilder grantee = new StringBuilder();
			Node partiesNode = htmlParser3.getNodeById("View Case Parties.");
			if (partiesNode != null) {
				NodeList casePartiesNodeList = partiesNode.getChildren().extractAllNodesThatMatch(new HasAttributeFilter("name", "%.DUMMY.CRTV.1-1."), true);

				if (casePartiesNodeList.size() > 0) {
					Node tableParties = HtmlParser3.getFirstParentTag(casePartiesNodeList.elementAt(0), TableTag.class);

					if (tableParties != null && tableParties instanceof TableTag) {
						TableRow[] rows = ((TableTag) tableParties).getRows();

						for (int i = 0; i < rows.length; i++) {
							TableColumn[] columns = rows[i].getColumns();

							if (columns.length >= 2) {
								String partyName = columns[0].toPlainTextString().replaceFirst("#\\d+(-\\d+)?", "").trim();
								String partyType = columns[1].toPlainTextString().toUpperCase();

								if (partyType.contains("PLAINTIFF") || partyType.contains("CREDITOR") || partyType.contains("APPELLANT")
										|| partyType.contains("PETITIONER")) {
									grantor.append(partyName).append(" / ");

								} else if (partyType.contains("DEFENDANT") || partyType.contains("DEBTOR") || partyType.contains("APPELLEE")
										|| partyType.contains("RESPONDENT")) {
									grantee.append(partyName).append(" / ");
								}
							}
						}
					}
				}

				String grantorLF = grantor.toString().replaceFirst(" / $", "").trim();
				String granteeLF = grantee.toString().replaceFirst(" / $", "").trim();
				if (!StringUtils.isEmpty(grantorLF)) {
					resultMap.put("tmpGrantorLF", grantorLF);
				}
				if (!StringUtils.isEmpty(granteeLF)) {
					resultMap.put("tmpGranteeLF", granteeLF);
				}

				ro.cst.tsearch.servers.functions.OHDelawareCO.parseNames(resultMap);
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		return null;
	}

	@Override
	protected void setModulesForAutoSearch(TSServerInfo serverInfo) {

		List<TSServerInfoModule> modules = new ArrayList<TSServerInfoModule>();

		// person
		{
			TSServerInfoModule module = new TSServerInfoModule(serverInfo.getModule(TSServerInfo.ADV_SEARCH_MODULE_IDX));
			module.clearSaKeys();
			module.setSaObjKey(SearchAttributes.OWNER_OBJECT);

			module.setIteratorType(0, FunctionStatesIterator.ITERATOR_TYPE_LAST_NAME_FAKE);
			module.setIteratorType(1, FunctionStatesIterator.ITERATOR_TYPE_FIRST_NAME_FAKE);

			ConfigurableNameIterator iterator = new ConfigurableNameIterator(searchId, true, new String[] { "L;F;" });
			iterator.setSearchWithType(ConfigurableNameIterator.SEARCH_WITH_TYPE.PERSON_NAME);
			iterator.setInitAgain(true);
			module.addIterator(iterator);

			module.addFilterForNext(new NameFilterForNext(searchId));
			GenericNameFilter nameFilter = (GenericNameFilter) NameFilterFactory.getDefaultNameFilter(SearchAttributes.OWNER_OBJECT, searchId, module);
			module.addFilter(nameFilter);
			module.addValidator(nameFilter.getValidator());

			modules.add(module);
		}

		// company
		{
			TSServerInfoModule module = new TSServerInfoModule(serverInfo.getModule(TSServerInfo.ADV_SEARCH_MODULE_IDX));
			module.clearSaKeys();
			module.setSaObjKey(SearchAttributes.OWNER_OBJECT);

			module.setIteratorType(7, FunctionStatesIterator.ITERATOR_TYPE_COMPANY_NAME);
			ConfigurableNameIterator iterator = new ConfigurableNameIterator(searchId, true, new String[] { "L;F;" });
			iterator.setSearchWithType(ConfigurableNameIterator.SEARCH_WITH_TYPE.COMPANY_NAME);
			iterator.setInitAgain(true);
			module.addIterator(iterator);

			module.addFilterForNext(new NameFilterForNext(searchId));
			GenericNameFilter nameFilter = (GenericNameFilter) NameFilterFactory.getDefaultNameFilter(SearchAttributes.OWNER_OBJECT, searchId, module);
			module.addFilter(nameFilter);
			module.addValidator(nameFilter.getValidator());

			modules.add(module);
		}

		serverInfo.setModulesForAutoSearch(modules);
	}
}
