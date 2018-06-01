package ro.cst.tsearch.servers.types;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Vector;

import org.apache.commons.lang.StringEscapeUtils;
import org.apache.commons.lang.StringUtils;
import org.htmlparser.Node;
import org.htmlparser.filters.HasAttributeFilter;
import org.htmlparser.filters.TagNameFilter;
import org.htmlparser.tags.LinkTag;
import org.htmlparser.tags.Span;
import org.htmlparser.tags.TableColumn;
import org.htmlparser.tags.TableRow;
import org.htmlparser.tags.TableTag;
import org.htmlparser.util.NodeList;

import ro.cst.tsearch.bean.SearchAttributes;
import ro.cst.tsearch.connection.http2.HttpManager;
import ro.cst.tsearch.connection.http2.HttpSite;
import ro.cst.tsearch.exceptions.ServerResponseException;
import ro.cst.tsearch.extractor.xml.Bridge;
import ro.cst.tsearch.extractor.xml.GenericFunctions2;
import ro.cst.tsearch.extractor.xml.ResultMap;
import ro.cst.tsearch.extractor.xml.ResultTable;
import ro.cst.tsearch.extractor.xml.StringFormats;
import ro.cst.tsearch.parser.HtmlParser3;
import ro.cst.tsearch.search.filter.FilterResponse;
import ro.cst.tsearch.search.filter.newfilters.address.AddressFilterFactory;
import ro.cst.tsearch.search.filter.newfilters.name.NameFilterFactory;
import ro.cst.tsearch.servers.TSConnectionURL;
import ro.cst.tsearch.servers.info.TSServerInfo;
import ro.cst.tsearch.servers.info.TSServerInfoModule;
import ro.cst.tsearch.servers.response.LinkInPage;
import ro.cst.tsearch.servers.response.ParsedResponse;
import ro.cst.tsearch.servers.response.PropertyAppraisalSet.PropertyAppraisalSetKey;
import ro.cst.tsearch.servers.response.PropertyIdentificationSet.PropertyIdentificationSetKey;
import ro.cst.tsearch.servers.response.TaxHistorySet.TaxHistorySetKey;
import ro.cst.tsearch.servers.response.ServerResponse;
import ro.cst.tsearch.utils.RegExUtils;

import com.stewart.ats.base.document.DocumentI;
import com.stewart.ats.base.document.TaxDocumentI;

public class COArapahoeTR extends TSServer {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	public COArapahoeTR(String rsRequestSolverName, String rsSitePath,
			String rsServerID, String rsPrmNameLink, long searchId,
			int miServerID) {
		super(rsRequestSolverName, rsSitePath, rsServerID, rsPrmNameLink,
				searchId, miServerID);
	}

	@Override
	protected void ParseResponse(String sAction, ServerResponse Response, int viParseID) throws ServerResponseException {
		String rsResponse = Response.getResult();
		ParsedResponse parsedResponse = Response.getParsedResponse();

		switch (viParseID) {
		case ID_SEARCH_BY_ADDRESS:
			// no result
			if (rsResponse.indexOf("No Records were returned") > -1) {
				Response.getParsedResponse().setError("No results found! Please change your search criteria and try again.");
				return;
			}

			// sometimes search by address fetches details directly
			HtmlParser3 parser = new HtmlParser3(rsResponse);
			TableTag tableTag = (TableTag) parser.getNodeById("ContentPlaceHolder1_DocumentTable");
			if (tableTag == null && !RegExUtils.getFirstMatch("(?s)(\\bProperty\\s+Tax\\s+Detail)", rsResponse, 1).isEmpty()) {
				ParseResponse(sAction, Response, ID_DETAILS);
				break;
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
			if (rsResponse.contains("No matching record was found")) {
//				Response.getParsedResponse().setError("<font color=\"red\">No results found. Check the parcel number you entered. You must "
//										+ "include any leading zeroes, all of the hyphens and no spaces.<br><br>"
//										+ "Real Estate schedule numbers are formatted like this: <b>0000-00-0-00-000</b><br>"
//										+ "Centrally Assessed schedule numbers are formatted like this: <b>00000-00000-0000</b><br>"
//										+ "Personal Property schedule numbers are formatted like this: <b>00000-00000-000</b><br></font>");
				Response.getParsedResponse().setError("<font color=\"red\">No results found.</font>");
				return;
			}
		case ID_DETAILS:
		case ID_SAVE_TO_TSD:

			StringBuilder accountId = new StringBuilder();
			String details = getDetails(rsResponse, accountId);
			String filename = accountId + ".html";

			if (viParseID != ID_SAVE_TO_TSD) {
				String originalLink = sAction.replace("?", "&") + "&"
						+ Response.getRawQuerry();
				String sSave2TSDLink = getLinkPrefix(TSConnectionURL.idPOST)
						+ originalLink;

				HashMap<String, String> data = new HashMap<String, String>();
				data.put("type", "CNTYTAX");
				if (isInstrumentSaved(accountId.toString(), null, data)) {
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
			String query = Response.getQuerry();
			if (query.contains("seq=")) {
				ParseResponse(sAction, Response, ID_SEARCH_BY_ADDRESS);
			} else {
				ParseResponse(sAction, Response, ID_DETAILS);
			}
			break;
		default:
			break;
		}
	}

	@Override
	public Collection<ParsedResponse> smartParseIntermediary(ServerResponse response, String table, StringBuilder outputTable) {
		Vector<ParsedResponse> intermediaryResponse = new Vector<ParsedResponse>();
		try {
			HtmlParser3 parser = new HtmlParser3(table);
			TableTag tableTag = (TableTag) parser.getNodeById("ContentPlaceHolder1_DocumentTable");
			if (tableTag == null) {
				return intermediaryResponse;
			}

			TableRow[] rows = tableTag.getRows();
			String footer = "";
			for (int i = 1; i < rows.length; i++) {
				TableRow row = rows[i];
				if (row.getColumnCount() == 5) {

					LinkTag linkTag = ((LinkTag) row.getColumns()[0].getChildren()
							.extractAllNodesThatMatch(new TagNameFilter("a"),true).elementAt(0));

					String link = CreatePartialLink(TSConnectionURL.idGET)
							+ linkTag.extractLink().trim().replaceAll("\\s", "%20");

					linkTag.setLink(link);

					ResultMap m = ro.cst.tsearch.servers.functions.COArapahoeTR.parseIntermediaryRow(row, searchId);

					ParsedResponse currentResponse = new ParsedResponse();
					currentResponse.setAttribute(ParsedResponse.SERVER_ROW_RESPONSE, row.toHtml());
					currentResponse.setOnlyResponse(row.toHtml());
					currentResponse.setPageLink(new LinkInPage(link, link, TSServer.REQUEST_SAVE_TO_TSD));

					Bridge bridge = new Bridge(currentResponse, m, searchId);

					DocumentI document = (TaxDocumentI) bridge.importData();
					currentResponse.setDocument(document);

					intermediaryResponse.add(currentResponse);
				}
			}
			footer = processLinks(response, parser);
			response.getParsedResponse()
					.setHeader("<table width=\"100%\" border=\"1\" cellspacing=\"0\" cellpadding=\"2\">\n"
								+ "<tr><th>PIN</th><th>Owner</th><th>Street Address</th><th>City</th><th>Type</th></tr>");
			response.getParsedResponse().setFooter(footer + "</table>");

			outputTable.append(table);

		} catch (Throwable t) {
			logger.error("Error while parsing intermediary data", t);
		}

		return intermediaryResponse;
	}

	private String processLinks(ServerResponse response, HtmlParser3 parser) {
		StringBuilder footer = new StringBuilder();
			TableTag noteTable = (TableTag) parser
				.getNodeById("ContentPlaceHolder1_SearchInfoTable");
		if (noteTable != null) {
			noteTable.removeAttribute("width");
			footer.append("<tr><td colspan=\"5\" align=\"center\">")
					.append(noteTable.toHtml()).append("</td></tr>");
		}
		TableTag pagingTable = (TableTag) parser
				.getNodeById("ContentPlaceHolder1_PagingTable");
		HttpSite site = HttpManager.getSite(getCurrentServerName(), searchId);
		if (site != null && pagingTable != null) {
			Map<String, String> params;
			try {
				params = HttpSite.fillConnectionParams(parser.getHtml(),
						((ro.cst.tsearch.connection.http2.COArapahoeTR) site)
								.getTargetArgumentParameters(), "form1");
				params.remove("__EVENTTARGET");
				
				
			} finally {
				// always release the HttpSite
				HttpManager.releaseSite(site);
			}
			int seq = getSeq();
			mSearch.setAdditionalInfo(
					getCurrentServerName() + ":params:" + seq, params);
			pagingTable.removeAttribute("width");

			NodeList links = pagingTable.getChildren()
					.extractAllNodesThatMatch(new TagNameFilter("a"), true);
			for (int i = 0; i < links.size(); i++) {
				LinkTag linkTag = (LinkTag) links.elementAt(i);
				linkTag.removeAttribute("class");
				String eventTarget=StringEscapeUtils.unescapeHtml(linkTag.getLink());
				linkTag.setLink(eventTarget
						.replaceAll(
								"(?i)__doPostBack\\('([^']+)',''\\)",
								CreatePartialLink(TSConnectionURL.idPOST)
										+ "/searchResults.aspx?__EVENTTARGET=$1&seq="
										+ seq));
				if ("ContentPlaceHolder1_NextPage".equals(linkTag.getAttribute("id"))) {
					response.getParsedResponse().setNextLink(
							"<a href=" + linkTag.extractLink() + ">Next</a>");
				}
			}
			footer.append("<tr><td colspan=\"5\" align=\"center\">")
					.append(pagingTable.toHtml()).append("</td></tr>");

		}

		return footer.toString();
	}

	@Override
	protected Object parseAndFillResultMap(ServerResponse response, String detailsHtml, ResultMap map) {
		try {
			detailsHtml = detailsHtml.replaceAll("&nbsp;", " ");
			HtmlParser3 parser = new HtmlParser3(detailsHtml);
			map.put(PropertyIdentificationSetKey.PARCEL_ID.getKeyName(), parser.getNodePlainTextById("ContentPlaceHolder1_lblPIN"));
			map.put(PropertyIdentificationSetKey.PARCEL_ID_PARCEL.getKeyName(), parser.getNodePlainTextById("ContentPlaceHolder1_lblAIN"));
			String[] address = StringFormats.parseAddressShelbyAO(parser.getNodePlainTextById("ContentPlaceHolder1_lblAddress"));
			map.put(PropertyIdentificationSetKey.STREET_NO.getKeyName(), address[0]);
			map.put(PropertyIdentificationSetKey.STREET_NAME.getKeyName(), address[1]);
			map.put(PropertyIdentificationSetKey.CITY.getKeyName(), parser.getNodePlainTextById("ContentPlaceHolder1_lblCity"));

			// get name
			String owner_name = "";
			if ("".equals(owner_name = parser.getNodePlainTextById("ContentPlaceHolder1_lblOwner"))) {
				if (StringUtils.isNotEmpty(parser.getNodePlainTextById("RecordDetail1_lblOwner"))) {
					owner_name = parser.getNodePlainTextById("RecordDetail1_lblOwner");
				}
			}
				
			
			ro.cst.tsearch.servers.functions.COArapahoeTR.parseNames(map, owner_name, parser.getNodePlainTextById("ucParcelHeader_lblCoOwnerNameTxt"));

//			map.put(PropertyAppraisalSetKey.TOTAL_ASSESSMENT.getKeyName(), parser.getNodePlainTextById("ucParcelValue_lblAssdTotal").replaceAll("[$,]", ""));
//			map.put(PropertyAppraisalSetKey.TOTAL_APPRAISAL.getKeyName(), parser.getNodePlainTextById("ucParcelValue_lblApprTotal").replaceAll("[$,]", ""));
			map.put(PropertyAppraisalSetKey.LAND_APPRAISAL.getKeyName(), parser.getNodePlainTextById("ContentPlaceHolder1_lblAssessedLnd").replaceAll("[$,]", ""));
			map.put(PropertyAppraisalSetKey.IMPROVEMENT_APPRAISAL.getKeyName(), parser.getNodePlainTextById("ContentPlaceHolder1_lblAssessImp").replaceAll("[$,]", ""));

			map.put(TaxHistorySetKey.TOTAL_DUE.getKeyName(), parser.getNodePlainTextById("ContentPlaceHolder1_lblOwedTotal").replaceAll("[$,]", ""));
			map.put(TaxHistorySetKey.AMOUNT_PAID.getKeyName(), parser.getNodePlainTextById("ContentPlaceHolder1_lblPaidTotal").replaceAll("[$,]", ""));
			map.put(TaxHistorySetKey.BASE_AMOUNT.getKeyName(), parser.getNodePlainTextById("ContentPlaceHolder1_lblOrigTotalAmt").replaceAll("[$,]", ""));
			map.put(TaxHistorySetKey.YEAR.getKeyName(), parser.getNodePlainTextById("ContentPlaceHolder1_lblPayable").replaceAll("Property Taxes for (\\d+) Payable \\d+","$1"));

			Node saleNode = parser.getNodeById("ucParcelSale_rptrSale_lblSaleTitle");
			if (saleNode != null && saleNode instanceof Span) {
				Node saleTableNode = saleNode.getParent();
				while (!(saleTableNode instanceof TableTag)) {
					saleTableNode = saleNode.getParent();
					saleNode = saleTableNode;
				}

				@SuppressWarnings("rawtypes")
				List<List> body = new ArrayList<List>();
				List<String> line = null;

				TableTag mainTable = (TableTag) saleTableNode;
				TableRow[] rows = mainTable.getRows();

				for (int i = 1; i < rows.length; i++) {
					TableColumn[] columns = rows[i].getColumns();
					if (columns.length >= 5) {
						line = new ArrayList<String>();
						String[] bookPage = columns[1].toPlainTextString().trim().split("\\s+");
						String instrumentNumber = "";
						if (bookPage.length == 2) {
							if (bookPage[0].matches("(?i)[a-z]\\w*")) {
								// if book starts with letter don't add book page and add instNo as book+page instead
								line.add("");
								line.add("");
								instrumentNumber = bookPage[0] + bookPage[1];
							} else {
								line.add(StringUtils.stripStart(bookPage[0], "0"));
								line.add(StringUtils.stripStart(bookPage[1], "0"));
							}
						} else {
							line.add("");
							line.add("");
						}
						line.add(columns[2].toPlainTextString().trim());
						line.add(columns[4].toPlainTextString().trim());
						line.add(columns[3].toPlainTextString().replaceAll("[^\\d.]", ""));
						line.add(instrumentNumber);

						body.add(line);
					}

				}

				// adding all cross references - should contain transfer table
				// and info parsed from legal description
				if (body != null && body.size() > 0) {
					ResultTable rt = new ResultTable();
					String[] header = { "Book", "Page", "InstrumentDate", "DocumentType", "SalesPrice", "InstrumentNumber" };
					rt = GenericFunctions2.createResultTable(body, header);
					map.put("SaleDataSet", rt);
				}

			}

		} catch (Exception e) {
			logger.error("Error while getting details", e);
		}
		return null;
	}

	private String getDetails(String rsResponse, StringBuilder accountId) {
		try {
			StringBuilder details = new StringBuilder();
			HtmlParser3 parser = new HtmlParser3(rsResponse);

			/* If from memory - use it as is */
			if (!rsResponse.contains("<HTML") && !rsResponse.contains("<html")) {
				//Node node = parser.getNodeById("ContentPlaceHolder1_lblAccount");
				Node node = parser.getNodeById("ContentPlaceHolder1_lblPIN");
				if (node == null) {
					return null;
				} else {
					accountId.append(node.toPlainTextString().trim());
				}
				return rsResponse;
			}

			//Node node = parser.getNodeById("ContentPlaceHolder1_lblAccount");
			String accountNo = "";
			Node node = parser.getNodeById("ContentPlaceHolder1_lblPIN");
			if (node == null) {
				return null;
			} else {
				accountNo = node.toPlainTextString().trim();
				accountId.append(accountNo);
			}
			TableTag mainTable = (TableTag) parser.getNodeById("ContentPlaceHolder1_Table1");

			TableRow[] rows = mainTable.getRows();
			mainTable.removeChild(mainTable.findPositionOf(rows[rows.length-1]));
			mainTable.removeChild(mainTable.findPositionOf(rows[0]));

			details.append("<table align=\"center\" border=\"1\" style=\"max-width:900px;\"><tr><td align=\"center\">")
				   .append(mainTable.toHtml()).append("</td></tr>");
			
			mainTable = (TableTag) parser.getNodeById("ContentPlaceHolder1_Table5"); //summary tax info
			if (mainTable != null) {
				details.append("<tr><td align=\"center\">");
				details.append(mainTable.toHtml()).append("</td></tr>");
			}
			
			mainTable = (TableTag) parser.getNodeById("ContentPlaceHolder1_Table4"); //tax details
			if (mainTable != null) {
				details.append("<tr><td align=\"center\">");
				String taxDetails = mainTable.toHtml();
				taxDetails = taxDetails.replaceAll("(?is)<(?:img|input)[^>]+>\\s*(?:</(?:img|input)>)?\\s*(<br\\s*/?>\\s*)*", "");
				details.append(taxDetails).append("</td></tr>");
			}

			Node scheduleNo = parser.getNodeById("ContentPlaceHolder1_lblAIN");
			
			if (scheduleNo != null) {
				String link = "http://ParcelSearch.ArapahoeGov.com/PPINum.aspx?PPINum=" + scheduleNo.toPlainTextString().trim();
				String assessorInformation = getAssessorInformation(link);
				
				if (!assessorInformation.isEmpty()) {
					parser = new HtmlParser3(assessorInformation);
					TableTag aoInfoTable = (TableTag) parser.getNodeById("shadedTable");
					
					if (aoInfoTable != null) {
						assessorInformation = aoInfoTable.toHtml();
						assessorInformation = assessorInformation.replaceFirst("(?is)<meta[^>]*>\\s*<script.*</script>", "");
						assessorInformation = assessorInformation.replaceAll("(?is)(?:<div[^>]*>\\s*)?<a [^>]+>([^<]+)</a>(?:\\s*</div>)?", "$1");
						assessorInformation = assessorInformation.replaceFirst("(?is)<td[A-Z\\s\\\"-:;\\d=]+rowspan=\\\"12\\\"[^>]*>[<\\sA-Z\\d=\\\"_'>/]+</td>","");
						assessorInformation = assessorInformation.replaceAll("(?is)(?:<tr[^>]*>\\s*)?<td[^>]*>\\s*(?:New Search|Printer Friendly)\\s*</td>(?:\\s*</tr>)?", "");
						assessorInformation = assessorInformation.replaceAll("(?is)(?:<div[^>]*>\\s*)?<span[^>]*>\\s*</span>(?:\\s*</div>)?","");
						assessorInformation = assessorInformation.replaceAll("(?is)<div[^>]*>\\s*</div>","");
						details.append("<tr><td align=\"center\">Assessor's Parcel Information</td></tr>");
						details.append("<tr><td align=\"center\">");
						details.append(assessorInformation).append("</td></tr>");
					}
				}
			}
			details.append("</table>");
			

			return details.toString();
		} catch (Exception e) {
			logger.error("Error while getting details", e);
		}
		return null;
	}

	private String getAssessorInformation(String extractLink) {
		String resultHtml = getLinkContents(extractLink);
		if (resultHtml.contains("No matching records were found.")) {
			return "";
		}
		try {
			resultHtml = resultHtml.replace("<span id=\"ucParcelSection_rptrSection__ctl0_lblSectionTitle\"","<div><span id=\"ucParcelSection_rptrSection__ctl0_lblSectionTitle\"")
					.replaceAll("(?i)<input [^>]*type=\"button\"[^>]*>","");
			HtmlParser3 parser = new HtmlParser3(resultHtml);
			NodeList tableList = parser.getNodeList().extractAllNodesThatMatch(new TagNameFilter("table"), true)
					.extractAllNodesThatMatch(new HasAttributeFilter("width", "82%"));
			if (tableList.size() > 0) {
				TableTag mainTable = (TableTag) tableList.elementAt(0);
				mainTable.removeAttribute("bgcolor");
				if (mainTable.getRowCount() > 1) {
					mainTable.removeChild(mainTable.findPositionOf(mainTable.getRow(1)));
				}
				NodeList nodeList = tableList.extractAllNodesThatMatch(new TagNameFilter("script"), true);
				for (int i = nodeList.size() - 1; i >= 0; i--) {
					mainTable.getRow(0).getColumns()[0].removeChild(mainTable
							.getRow(0).getColumns()[0].findPositionOf(nodeList
							.elementAt(i)));
				}
				NodeList tdList = tableList.extractAllNodesThatMatch(new TagNameFilter("td"), true)
						.extractAllNodesThatMatch(new HasAttributeFilter("rowspan", "12"));
				if (tdList.size() > 0) {
					((TableColumn) tdList.elementAt(0)).setChildren(new NodeList());
				}
				resultHtml = mainTable.toHtml()
						.replaceAll("(?ism)<a [^>]*>([^<]*)<[^>]*>", "$1")
						.replaceAll("(?ism)\\*?\\s*Photo[&nbsp;\\s]*Sketch", "");
			}

		} catch (Exception e) {
			logger.error("Error while getting getTaxHistory " + extractLink, e);
		}
		return resultHtml;
	}

	@Override
	protected void setModulesForAutoSearch(TSServerInfo serverInfo) {
		List<TSServerInfoModule> moduleList = new ArrayList<TSServerInfoModule>();

		TSServerInfoModule module = null;
//		if (hasPin()) {
//			String pin = getSearchAttribute(SearchAttributes.LD_PARCELNO);
//			if (!pin.matches("\\d{4}-\\d{2}-\\d-\\d{2}-\\d{3}")) {
//				pin = pin.replaceAll("-", "");
//				if (pin.matches("\\d{12}")) {
//					pin = pin.substring(0, 4) + "-" + pin.substring(4, 6) + "-"
//							+ pin.substring(6, 7) + "-" + pin.substring(7, 9)
//							+ "-" + pin.substring(9);
//				} else {
//					pin = null;
//				}
//
//			}
//			if (pin != null) {
//				module = new TSServerInfoModule(serverInfo.getModule(TSServerInfo.PARCEL_ID_MODULE_IDX));
//				module.clearSaKeys();
//				module.forceValue(0, pin);
//				moduleList.add(module);
//			}
//		}

//		if (hasPinParcelNo()) {
//			String pin = getSearchAttribute(SearchAttributes.LD_PARCELNO_PARCEL);
//			if (!pin.matches("\\d{4}-\\d{2}-\\d-\\d{2}-\\d{3}")) {
//				pin = pin.replaceAll("-", "");
//				if (pin.matches("\\d{12}")) {
//					pin = pin.substring(0, 4) + "-" + pin.substring(4, 6) + "-"
//							+ pin.substring(6, 7) + "-" + pin.substring(7, 9)
//							+ "-" + pin.substring(9);
//				} else {
//					pin = null;
//				}
//
//			}
//			if (pin != null) {
//				module = new TSServerInfoModule(serverInfo.getModule(TSServerInfo.PARCEL_ID_MODULE_IDX));
//				module.clearSaKeys();
//				module.forceValue(0, pin);
//				moduleList.add(module);
//			}
//
//		}

		if (hasStreet()) {
			FilterResponse nameFilterHybrid = NameFilterFactory.getHybridNameFilter(SearchAttributes.OWNER_OBJECT,	searchId, module);
			FilterResponse addressFilter = AddressFilterFactory.getAddressHybridFilter(searchId, 0.8d, true);
			module = new TSServerInfoModule(serverInfo.getModule(TSServerInfo.ADDRESS_MODULE_IDX));
			module.addFilter(addressFilter);
			module.addFilter(nameFilterHybrid);
			moduleList.add(module);

		}

		serverInfo.setModulesForAutoSearch(moduleList);
	}

	private static int seq = 0;

	protected synchronized static int getSeq() {
		return seq++;
	}
}
