package ro.cst.tsearch.servers.types;


import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Vector;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang.StringEscapeUtils;
import org.htmlparser.filters.HasAttributeFilter;
import org.htmlparser.filters.RegexFilter;
import org.htmlparser.filters.TagNameFilter;
import org.htmlparser.tags.LinkTag;
import org.htmlparser.tags.TableColumn;
import org.htmlparser.tags.TableHeader;
import org.htmlparser.tags.TableRow;
import org.htmlparser.tags.TableTag;
import org.htmlparser.util.NodeList;
import org.htmlparser.Node;

import ro.cst.tsearch.bean.SearchAttributes;
import ro.cst.tsearch.exceptions.ServerResponseException;
import ro.cst.tsearch.extractor.xml.Bridge;
import ro.cst.tsearch.extractor.xml.ResultMap;
import ro.cst.tsearch.extractor.xml.ResultTable;
import ro.cst.tsearch.parser.HtmlParser3;
import ro.cst.tsearch.search.filter.FilterResponse;
import ro.cst.tsearch.search.filter.newfilters.address.AddressFilterFactory;
import ro.cst.tsearch.search.filter.newfilters.address.GenericAddressFilter;
import ro.cst.tsearch.search.filter.newfilters.name.NameFilterFactory;
import ro.cst.tsearch.search.iterator.ModuleStatesIteratorFactory;
import ro.cst.tsearch.search.module.ConfigurableNameIterator;
import ro.cst.tsearch.servers.TSConnectionURL;
import ro.cst.tsearch.servers.info.TSServerInfo;
import ro.cst.tsearch.servers.info.TSServerInfoModule;
import ro.cst.tsearch.servers.response.LinkInPage;
import ro.cst.tsearch.servers.response.OtherInformationSet.OtherInformationSetKey;
import ro.cst.tsearch.servers.response.ParsedResponse;
import ro.cst.tsearch.servers.response.Parser;
import ro.cst.tsearch.servers.response.PropertyAppraisalSet.PropertyAppraisalSetKey;
import ro.cst.tsearch.servers.response.PropertyIdentificationSet.PropertyIdentificationSetKey;
import ro.cst.tsearch.servers.response.ServerResponse;
import ro.cst.tsearch.servers.response.TaxHistorySet.TaxHistorySetKey;
import ro.cst.tsearch.utils.StringUtils;

import com.stewart.ats.base.document.DocumentI;
import com.stewart.ats.base.document.TaxDocumentI;

/**
 * @author mihaib
*/

public class TXHarrisServerTR extends TSServer{
	
	private boolean downloadingForSave;
	private static final long serialVersionUID = 1L;
	
	public TXHarrisServerTR(long searchId) {
		super(searchId);
	}

	public TXHarrisServerTR(String rsRequestSolverName, String rsSitePath,
			String rsServerID, String rsPrmNameLink, long searchId,	int miServerID) {
		super(rsRequestSolverName, rsSitePath, rsServerID, rsPrmNameLink, searchId,	miServerID);
	}
	
	@Override
	protected void ParseResponse(String sAction, ServerResponse Response, int viParseID) throws ServerResponseException {
		
		String initialResponse = Response.getResult();
		
		String rsResponse = initialResponse;
		
		if (rsResponse.indexOf("<p>No results could be found, please check your input and try again.</p>") != -1){
			Response.getParsedResponse().setError("No Record Found.");
			return;
		}
		if (rsResponse.indexOf("Tax Deferral for Account") != -1){//1064760000313
			Response.getParsedResponse().setError("This account has been granted a Tax Deferrral by the Harris County Appraisal District.");
		return;
			
		}
	
		switch (viParseID) {
		case ID_SEARCH_BY_NAME:
		case ID_SEARCH_BY_PARCEL:
		case ID_SEARCH_BY_ADDRESS:
			try {
				StringBuilder outputTable = new StringBuilder();
				ParsedResponse parsedResponse = Response.getParsedResponse();

				Collection<ParsedResponse> smartParsedResponses = smartParseIntermediary(
						Response, rsResponse, outputTable);

				if (smartParsedResponses.size() > 0) {
					parsedResponse.setResultRows(new Vector<ParsedResponse>(smartParsedResponses));
					parsedResponse.setOnlyResponse(outputTable.toString());
				}

			} catch (Exception e) {
				e.printStackTrace();
			}
			break;

		case ID_DETAILS:

			String details = getDetails(rsResponse, Response);
			String docNo = StringUtils.extractParameterFromUrl(sAction, "Account");
			if (docNo.isEmpty()) {
				try {
					HtmlParser3 htmlParser3 = new HtmlParser3(details);
					NodeList mainList = htmlParser3.getNodeList();

					docNo = HtmlParser3.getValueFromAbsoluteCell(1, 0, HtmlParser3.findNode(mainList, "Account Number"), "", true).trim();
					if (StringUtils.isNotEmpty(docNo)) {
						docNo = docNo.replaceAll("-", "").replaceAll("(?is)</?font[^>]*>", "").replaceAll("(?is)</?b[^>]*>", "").trim();
					}
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
			if ((!downloadingForSave)) {	
                String qry_aux = Response.getRawQuerry();
				qry_aux = "dummy=" + docNo + "&" + qry_aux;
				String originalLink = sAction + "&" + qry_aux;
				originalLink = originalLink.replaceAll("(?is)&$", "");
				try {
					originalLink = URLDecoder.decode(originalLink, "UTF-8");
				} catch (UnsupportedEncodingException e) {
					e.printStackTrace();
				}
				String sSave2TSDLink = getLinkPrefix(TSConnectionURL.idGET) + originalLink;
				
				HashMap<String, String> data = new HashMap<String, String>();
				data.put("type", "CNTYTAX");
    				
				if (isInstrumentSaved(docNo, null, data)){
                	details += CreateFileAlreadyInTSD();
				} else {
					mSearch.addInMemoryDoc(sSave2TSDLink, details);
					details = addSaveToTsdButton(details, sSave2TSDLink, viParseID);
				}
				Response.getParsedResponse().setPageLink(
						new LinkInPage(sSave2TSDLink, originalLink, TSServer.REQUEST_SAVE_TO_TSD));
				parser.Parse(Response.getParsedResponse(), details,	Parser.NO_PARSE); 
            } else {
				smartParseDetails(Response, details);
				msSaveToTSDFileName = docNo + ".html";
				Response.getParsedResponse().setFileName(getServerTypeDirectory() + msSaveToTSDFileName);
				msSaveToTSDResponce = details + CreateFileAlreadyInTSD();
			}
			break;

		case ID_GET_LINK:
			ParseResponse(sAction, Response, ID_DETAILS);
			break;
		case ID_SAVE_TO_TSD:
			downloadingForSave = true;
			ParseResponse(sAction, Response, ID_DETAILS);
			downloadingForSave = false;
			break;
		}
	}
	
	public Collection<ParsedResponse> smartParseIntermediary(ServerResponse response, String html, StringBuilder outputTable) {
		
		Vector<ParsedResponse> intermediaryResponse = new Vector<ParsedResponse>();

		try {
			HtmlParser3 htmlParser3 = new HtmlParser3(html);
			NodeList nodeList = htmlParser3.getNodeList();
			Node tableNode = htmlParser3.getNodeById("table");

			StringBuilder newTable = new StringBuilder();
			newTable.append("<table BORDER='1' CELLPADDING='2'>")
					.append("<tr><th>Account Number</th><th>Assessed Owner</th><th>Property Address</th></tr>");

			if (tableNode == null) {
				tableNode = nodeList.extractAllNodesThatMatch(new TagNameFilter("table"), true).elementAt(0);
			}
			if (tableNode != null) {
				TableTag mainTable = (TableTag) tableNode;

				TableRow[] rows = mainTable.getRows();

				for (TableRow row : rows) {
					//row.setAttribute("align","center");
					if (row.getHeaderCount() > 0) {
						continue;
					}
					
					if (row.getColumnCount() == 3) {
						
						TableColumn[] cols = row.getColumns();
						NodeList aList = cols[0].getChildren().extractAllNodesThatMatch(new TagNameFilter("a"), true);
						if (aList.size() > 0) {
							LinkTag  linkTag =  (LinkTag)aList.elementAt(0);
							String pageAddr = linkTag.extractLink();
							String link = CreatePartialLink(TSConnectionURL.idGET)+pageAddr;
							linkTag.setLink(link);
							String rowHtml = row.toHtml();

							ParsedResponse currentResponse = new ParsedResponse();
							currentResponse.setPageLink(new LinkInPage(link, link, TSServer.REQUEST_SAVE_TO_TSD));
							
							currentResponse.setAttribute(ParsedResponse.SERVER_ROW_RESPONSE, rowHtml);
							currentResponse.setOnlyResponse(rowHtml);

							ResultMap m =  ro.cst.tsearch.servers.functions.TXHarrisServerTR.parseIntermediaryRow(row, searchId, miServerID);
							m.removeTempDef();
							Bridge bridge = new Bridge(currentResponse, m, searchId);

							DocumentI document = (TaxDocumentI) bridge.importData();

							currentResponse.setDocument(document);
							intermediaryResponse.add(currentResponse);
						}
					}
				}
			}
			String header = "<tr><th>Account Number</th><th>Assessed Owner</th><th>Property Address</th></tr>";

			response.getParsedResponse().setHeader("<table width=\"100%\" border=\"1\" cellspacing=\"0\" cellpadding=\"2\">\n" + header);
			response.getParsedResponse().setFooter("</table>");
			newTable.append("</table>");
			outputTable.append(newTable);
		} catch (Exception e) {
			e.printStackTrace();
		}
		return intermediaryResponse;
	}
	
	protected String getDetails(String response, ServerResponse Response) {

		HtmlParser3 htmlParser3 = new HtmlParser3(response);
		// if from memory - use it as is
		if (!response.toLowerCase().contains("<html")) {
			return response;
		}
		String parcelID=""; 
		StringBuilder details = new StringBuilder();
		details.append("<table id=\"details\" width=\"800\" align=\"center\" border=\"2\">");

		Node detailsNode = htmlParser3.getNodeById("CurrentStatement");
		if (detailsNode != null) {
			String detailsCurrentYear = detailsNode.toHtml().replaceAll("(?is)<a[^>]*>.*?</a>", "")
					.replaceAll("(?is)<div[^>]*class\\s*=\\s*\"ribbon\"[^>]*>.*?</div>", "")
					.replaceAll("(?is)(<td\\s+class=\\s*\"Subtitle\")", "$1 style=\"font-weight: bold\"");
			if (!detailsCurrentYear.contains("Server Error in")) {
				details.append("<tr id=\"currentYearTax\"><td>" + detailsCurrentYear

						+ "</td></tr>");
			}

			// previous years tax receipts
			Pattern pattern = Pattern.compile("(?is)(load\\('FullReceipt.*?\\))");
			Matcher matcher = pattern.matcher(response);
			String siteLink = dataSite.getLink();
			siteLink = dataSite.getServerRelativeLink();
			int counter = 0;
			while (matcher.find()) {
				String receiptLink = siteLink;

				Pattern receiptLinkPattern = Pattern.compile("'([^']*)'");
				Matcher receiptLinkMatcher = receiptLinkPattern.matcher(matcher.group(1));
				
				while (receiptLinkMatcher.find()) {
					if (counter == 1) {
						parcelID = receiptLinkMatcher.group(1);
					}
					receiptLink += receiptLinkMatcher.group(1);
					counter++;
				}
				if (!receiptLink.isEmpty()) {
					String receiptContents = getLinkContents(receiptLink);
					if (!receiptContents.contains("Server Error in")) {
						details.append("<tr><td class=\"receipts\">" + receiptContents.replaceAll("(?is)<a[^>]*>.*?</a>", "") + "</td></tr>");
					}
				}
			}
			pattern = Pattern.compile("(?is)load\\('(History[^']*)'");
			matcher = pattern.matcher(response);

			// get 5 year account history
			if (matcher.find()) {
				String historyLink = siteLink + "HistoryTab?Account=" + parcelID;
				if (!historyLink.isEmpty()) {
					String historyContents = getLinkContents(historyLink).replaceAll("(?is)(<style>.*?)table td\\s*\\{(.*?</style>)", "$1table td.num$2");
					if (!historyContents.contains("Server Error in")) {
						details.append("<tr><td id = \"accountHistory\">" + historyContents + "</td></tr>");
					}
				}
			}
		}
		else {
			// delinquent details
			Node delinquentContentNode = htmlParser3.getNodeById("DelStatement");

			if (delinquentContentNode == null) {
				String delinquentLink = this.msLastLink.replaceFirst("(?i)TaxStatement", "DelStatement");
				if (!delinquentLink.isEmpty()) {
					String delinquentContent = getLinkContents(delinquentLink);
					if (!delinquentContent.contains("There was a problem retrieving the the statement for this account")) {
						htmlParser3 = new HtmlParser3(delinquentContent);
						delinquentContentNode = htmlParser3.getNodeById("DelStatement");
					}
				}
			}

			if (delinquentContentNode != null) {
				Node titleDiv = htmlParser3.getNodeList().extractAllNodesThatMatch(new RegexFilter("Property\\s+Tax\\s+Statement"), true).elementAt(0);
				String delinquent = delinquentContentNode.toHtml().replaceAll("(?is)<div class=\"ribbon\"[^>]*>.*?</div>", "");

				if (!delinquent.contains("Server Error in")) {
					if (titleDiv != null) {
						details.append("<tr><th><h3>" + titleDiv.toHtml() + "</h3></th></tr>");
					}

					details.append("<tr id=\"delqTax\"><td>" + delinquent
							+ "</td></tr>");
				}

				// jurisdiction details
				Node jurisdictionDetailsNode = htmlParser3.getNodeById("DelJur");
				if (jurisdictionDetailsNode != null) {
					details.append("<tr><th><h3>Jurisdiction Details</h3></th></tr>");
					details.append("<tr id=\"delqTaxDetails\"><td>" + jurisdictionDetailsNode.toHtml()+ "</td></tr>");
				}
			}
		}
		details.append("</table>");
		return details.toString()
				.replaceAll("(?is)<button[^>]*>.*?</button>", "")
				.replaceAll("(?is)<a[^>]*>(.*?)</a>", "$1")
				.replaceAll("(?is)(<table\\s+class=\"tot\")", "$1 style=\"font-weight: bold\"")
				.replaceAll("(?is)(<div class=\"ContentTitle\">)(.*?)(</div>)", "$1<h3>$2</h3>$3");
	}
	
	@SuppressWarnings("rawtypes")
	@Override
	protected Object parseAndFillResultMap(ServerResponse response, String detailsHtml, ResultMap resultMap) {
		try {
			resultMap.put(OtherInformationSetKey.SRC_TYPE.getKeyName(), "TR");
			detailsHtml = StringEscapeUtils.unescapeHtml(detailsHtml);

			HtmlParser3 htmlParser3 = new HtmlParser3(detailsHtml);

			NodeList mainList = htmlParser3.getNodeList();

			String pid = HtmlParser3.getValueFromAbsoluteCell(1, 0, HtmlParser3.findNode(mainList, "Account Number"), "", true);
			
			if(pid.trim().isEmpty()){
				Node accountNode = htmlParser3.getNodeById("DelStatement");
				if(accountNode!=null){
					pid = accountNode.toHtml().replaceAll("(?is).*Account\\s*#\\s*:?\\s*(\\d+).*", "$1");
				}
			}
			
			if (StringUtils.isNotEmpty(pid)) {
				resultMap.put(PropertyIdentificationSetKey.PARCEL_ID.getKeyName(), pid.replaceAll("-", "")
						.replaceAll("(?is)</?font[^>]*>", "").replaceAll("(?is)</?b[^>]*>", "").trim());
			}
			String propertyDescription = HtmlParser3.getValueFromAbsoluteCell(1, 0, HtmlParser3.findNode(mainList, "Property Description"), "", true);
			String address = propertyDescription.replaceAll("(?s)(.*?)<br[^>]*>.*", "$1");

			if (StringUtils.isNotEmpty(address)) {
				resultMap.put("tmpAddress", address.trim());
			}
			String legalDescription = propertyDescription.replaceAll("(?s).*<\\s*br[^>]*>(.*)", "$1").replaceAll("\\s+", " ");

			if (StringUtils.isNotEmpty(legalDescription)) {
				resultMap.put("tmpLegal", legalDescription.trim());
			}

			String owner = HtmlParser3.getValueFromAbsoluteCell(1, 0, HtmlParser3.findNode(mainList, "Assessed Owner"), "", true);

			if (StringUtils.isNotEmpty(owner)) {
				resultMap.put("tmpOwner", owner.replaceAll("(?s)(.*?)<br[^>]*>.*", "$1").trim());
			}

			if (detailsHtml.toLowerCase().contains("delinquent property tax statement")) {
				String priorDelinquent = "";
				Node priorDelinquentOffsetNode = HtmlParser3.findNode(mainList, "Total Due >");

				if (priorDelinquentOffsetNode != null) {
					Node priorDelinquentNode = priorDelinquentOffsetNode.getParent().getParent().getLastChild().getPreviousSibling();
					
					if (priorDelinquentNode != null) {
						priorDelinquent = priorDelinquentNode.toPlainTextString();
					}
				}
				
				if (StringUtils.isNotEmpty(priorDelinquent)) {
					priorDelinquent = priorDelinquent.replaceAll("[ $,-]", "");
					resultMap.put(TaxHistorySetKey.PRIOR_DELINQUENT.getKeyName(), priorDelinquent);
				}
				String year = "";
				Node yearNode = HtmlParser3.getNodeByTypeAttributeDescription(mainList, "table", "class", "dels",
						new String[] { "Year", "Tax", "Penalties & Interest", "Collection Penalty", "Total Due" },
						true);
				if (yearNode != null) {
					TableTag yearTable = (TableTag) yearNode;
					TableRow[] rows = yearTable.getRows();

					if (rows.length >= 2)
					{
						TableRow currentYearRow = rows[rows.length - 2];
						TableColumn[] currentYearRowCols = currentYearRow.getColumns();
						if (currentYearRowCols.length >= 5) {
							year = currentYearRowCols[0].toPlainTextString();
							if (!year.isEmpty()) {
								resultMap.put(TaxHistorySetKey.YEAR.getKeyName(), year.replaceAll("[ $,-]", ""));
							}
							String baseAmount = currentYearRowCols[1].toPlainTextString();
							if (!baseAmount.isEmpty()) {
								resultMap.put(TaxHistorySetKey.BASE_AMOUNT.getKeyName(), baseAmount.replaceAll("[ $,-]", ""));
							}
							String amountDue = currentYearRowCols[4].toPlainTextString();

							double delinquentAmountd = 0.0;
							double amountDued = 0.0;
							if (!amountDue.isEmpty()) {
								amountDue = amountDue.replaceAll("[ $,-]", "");
								resultMap.put(TaxHistorySetKey.TOTAL_DUE.getKeyName(), amountDue);
								amountDued = Double.parseDouble(amountDue);
							}

							TableRow totalRow = rows[rows.length - 1];
							TableHeader[] totalHeaders = totalRow.getHeaders();
							if (totalHeaders.length >= 5) {
								String totalDue = totalHeaders[4].toPlainTextString().replaceAll("[ $,-]", "");
								delinquentAmountd = Double.parseDouble(totalDue) - amountDued;
								resultMap.put(TaxHistorySetKey.PRIOR_DELINQUENT.getKeyName(), String.valueOf(delinquentAmountd));
							}
						}
					}
				}
			} else {
				// parser propertyAppraisalSet
				String land = HtmlParser3.getValueFromAbsoluteCell(0, 1, HtmlParser3.findNode(mainList, "Land - Market Value:"), "", true).replaceAll(
						"[ $,-]", "");
				if (StringUtils.isNotEmpty(land)) {
					land = land.replaceAll("[ $,-]", "");
					resultMap.put(PropertyAppraisalSetKey.LAND_APPRAISAL.getKeyName(), land);
				}

				String improvements = HtmlParser3.getValueFromAbsoluteCell(0, 1, HtmlParser3.findNode(mainList, "Impr - Market Value:"), "", true).replaceAll(
						"[ $,-]", "");
				if (StringUtils.isNotEmpty(improvements)) {
					improvements = improvements.replaceAll("[ $,-]", "");
					resultMap.put(PropertyAppraisalSetKey.IMPROVEMENT_APPRAISAL.getKeyName(), improvements);
				}

				String appraised = HtmlParser3.getValueFromAbsoluteCell(0, 1, HtmlParser3.findNode(mainList, "Appraised Value:"), "", true);
				if (StringUtils.isNotEmpty(appraised)) {
					appraised = appraised.replaceAll("[ $,-]", "");
					resultMap.put(PropertyAppraisalSetKey.TOTAL_APPRAISAL.getKeyName(), appraised);
					resultMap.put(PropertyAppraisalSetKey.TOTAL_ASSESSMENT.getKeyName(), appraised);
				}

				// TaxHistorySet
				String yearRegex = "Property\\s+Tax\\s+Statement";
				Node yearNode = mainList.extractAllNodesThatMatch(new RegexFilter(yearRegex), true).elementAt(0);
				if (yearNode != null) {
					String year = yearNode.toPlainTextString().replaceAll(yearRegex, "");
					resultMap.put(TaxHistorySetKey.YEAR.getKeyName(), year.trim());
				}

				// ba
				String baseAmount = HtmlParser3.getValueFromAbsoluteCell(0, 1, HtmlParser3.findNode(mainList, "Taxes Due by January 31"), "", true);
				if (StringUtils.isNotEmpty(baseAmount)) {
					baseAmount = baseAmount.replaceAll("[ $,-]", "");
					resultMap.put(TaxHistorySetKey.BASE_AMOUNT.getKeyName(), baseAmount);
				}

				// ap
				String amountPaid = HtmlParser3.getValueFromAbsoluteCell(0, 1, HtmlParser3.findNode(mainList, "Payments applied to"), "", true);
				if (StringUtils.isNotEmpty(amountPaid)) {
					amountPaid = amountPaid.replaceAll("[ $,-]", "");
					resultMap.put(TaxHistorySetKey.AMOUNT_PAID.getKeyName(), amountPaid);
				}

				// ad
				String amountDue = HtmlParser3.getValueFromAbsoluteCell(0, 1, HtmlParser3.findNode(mainList, "Total Current Taxes Due"), "", true);
				if (StringUtils.isNotEmpty(amountDue)) {
					amountDue = amountDue.replaceAll("[ $,-]", "");
					resultMap.put(TaxHistorySetKey.TOTAL_DUE.getKeyName(), amountDue);
				}

				// pd
				String priorDelinquent = HtmlParser3.getValueFromAbsoluteCell(0, 1, HtmlParser3.findNode(mainList, "Prior year(s) taxes due"), "", true);
				if (StringUtils.isNotEmpty(priorDelinquent)) {
					priorDelinquent = priorDelinquent.replaceAll("[ $,-]", "");
					resultMap.put(TaxHistorySetKey.PRIOR_DELINQUENT.getKeyName(), priorDelinquent);
				}
				
				//receipts
				NodeList receiptsList = mainList.extractAllNodesThatMatch(new HasAttributeFilter("class","receipts"), true);
				if (receiptsList.size() > 0) {
					List<String> line = null;
					List<List> bodyRT = new ArrayList<List>();

					for (int i = 1; i < receiptsList.size(); i++) {
						line = new ArrayList<String>();
						NodeList receipt = receiptsList.elementAt(i).getChildren();
						String receiptNo = HtmlParser3.findNode(receipt, "Remit Seq No").toPlainTextString();
						if (StringUtils.isNotEmpty(receiptNo)) {
							receiptNo = receiptNo.replaceAll(".*?(\\w+)$", "$1").trim();
							line.add(receiptNo);
						}
						else {
							line.add("");
						}

						String receiptDate =  HtmlParser3.findNode(receipt, "Receipt Date").toPlainTextString();
						if (StringUtils.isNotEmpty(receiptDate)) {
							receiptDate = receiptDate.replaceAll(".*?([\\d+/]+)$", "$1").trim();
							line.add(receiptDate);
						}
						else {
							line.add("");
						}
						
						Node receiptTotalPaidOffsetNode = HtmlParser3.findNode(receipt, "Total Paid");
						String receiptAmountPaid="";
						if (receiptTotalPaidOffsetNode != null) {
							Node receiptTotalPaidNode = receiptTotalPaidOffsetNode.getParent().getParent().getLastChild();
							if (receiptTotalPaidNode != null) {
								receiptAmountPaid = receiptTotalPaidNode.toPlainTextString();
							}	
						}
						if (StringUtils.isNotEmpty(receiptAmountPaid)) {
							receiptAmountPaid = receiptAmountPaid.replaceAll("[ $,-]", "");
							line.add(receiptAmountPaid);
						}
						else {
							line.add("");
						}
						bodyRT.add(line);
					}
					
					if (!bodyRT.isEmpty()) {
						ResultTable newRT = new ResultTable();
						String[] header = { "ReceiptNumber", "ReceiptDate", "ReceiptAmount" };
						Map<String, String[]> map = new HashMap<String, String[]>();
						map.put("ReceiptNumber", new String[] { "ReceiptNumber", "" });
						map.put("ReceiptDate", new String[] { "ReceiptDate", "" });
						map.put("ReceiptAmount", new String[] { "ReceiptAmount", "" });
						newRT.setHead(header);
						newRT.setMap(map);
						newRT.setBody(bodyRT);
						newRT.setReadOnly();
						resultMap.put("TaxHistorySet", newRT);
					}
				}
			}
			ro.cst.tsearch.servers.functions.TXHarrisServerTR.parseAddressTXHarrisTR(resultMap, searchId);
			ro.cst.tsearch.servers.functions.TXHarrisServerTR.partyNamesTXHarrisTR(resultMap, searchId);
			ro.cst.tsearch.servers.functions.TXHarrisServerTR.parseLegalTXHarrisTR(resultMap, searchId);

		} catch (Exception e) {
			e.printStackTrace();
		}
		return null;
	}

	@Override
	protected void setModulesForAutoSearch(TSServerInfo serverInfo) {
		List<TSServerInfoModule> modules = new ArrayList<TSServerInfoModule>();
		TSServerInfoModule module = null;

		GenericAddressFilter addressFilter 	= AddressFilterFactory.getGenericAddressHighPassFilter( searchId, 0.8d );
		FilterResponse defaultNameFilter = NameFilterFactory.getHybridNameFilter(SearchAttributes.OWNER_OBJECT , searchId , module );
		
		if (hasPin()) {
		modules.add(new TSServerInfoModule(serverInfo.getModule(TSServerInfo.PARCEL_ID_MODULE_IDX)));
		}
		
		if (hasStreet()) {
			//Search by Property Address
			module = new TSServerInfoModule(serverInfo.getModule(TSServerInfo.ADDRESS_MODULE_IDX));
			module.addFilter(addressFilter);
			module.addFilter(defaultNameFilter);
			modules.add(module);
		}
		
		if (hasOwner()){
			//Search by Owner
			module = new TSServerInfoModule(serverInfo.getModule(TSServerInfo.NAME_MODULE_IDX));
			module.addFilter(addressFilter);
			module.addFilter(defaultNameFilter);
			ConfigurableNameIterator nameIterator = (ConfigurableNameIterator) ModuleStatesIteratorFactory
										.getConfigurableNameIterator(module, searchId, new String[] {"L;F;"});
			module.addIterator(nameIterator);
			modules.add(module);
		}
		
		serverInfo.setModulesForAutoSearch(modules);		
	}
}
		
