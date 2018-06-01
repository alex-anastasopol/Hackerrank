package ro.cst.tsearch.servers.types;

import java.io.UnsupportedEncodingException;
import java.math.BigDecimal;
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
import org.htmlparser.Node;
import org.htmlparser.filters.HasAttributeFilter;
import org.htmlparser.filters.RegexFilter;
import org.htmlparser.filters.TagNameFilter;
import org.htmlparser.tags.LinkTag;
import org.htmlparser.tags.TableColumn;
import org.htmlparser.tags.TableRow;
import org.htmlparser.tags.TableTag;
import org.htmlparser.util.NodeList;

import com.stewart.ats.base.document.DocumentI;
import com.stewart.ats.base.document.TaxDocumentI;

import ro.cst.tsearch.bean.SearchAttributes;
import ro.cst.tsearch.exceptions.ServerResponseException;
import ro.cst.tsearch.extractor.xml.Bridge;
import ro.cst.tsearch.extractor.xml.GenericFunctions;
import ro.cst.tsearch.extractor.xml.GenericFunctions1;
import ro.cst.tsearch.extractor.xml.ResultMap;
import ro.cst.tsearch.extractor.xml.ResultTable;
import ro.cst.tsearch.extractor.xml.StringFormats;
import ro.cst.tsearch.parser.HtmlParser3;
import ro.cst.tsearch.search.filter.FilterResponse;
import ro.cst.tsearch.search.filter.RejectNonRealEstate;
import ro.cst.tsearch.search.name.NameUtils;
import ro.cst.tsearch.search.validator.DocsValidator;
import ro.cst.tsearch.servers.TSConnectionURL;
import ro.cst.tsearch.servers.info.TSServerInfo;
import ro.cst.tsearch.servers.response.LinkInPage;
import ro.cst.tsearch.servers.response.ParsedResponse;
import ro.cst.tsearch.servers.response.ServerResponse;
import ro.cst.tsearch.servers.response.OtherInformationSet.OtherInformationSetKey;
import ro.cst.tsearch.servers.response.PropertyIdentificationSet.PropertyIdentificationSetKey;
import ro.cst.tsearch.servers.response.TaxHistorySet.TaxHistorySetKey;
import ro.cst.tsearch.servers.response.TaxInstallmentSet.TaxInstallmentSetKey;
import ro.cst.tsearch.utils.StringUtils;
import ro.cst.tsearch.utils.Tidy;

public class CAMegabyteCommonSiteTR extends TSServer {
	private static final long	serialVersionUID				= 445321998774294937L;
	protected Pattern			intermediaryRowPattern			= Pattern
																		.compile("(?is)<tr[^>]*>\\s*<td[^>]*>(.+?)</td>\\s*<td[^>]*>(.+?)</td>\\s*<td[^>]*>(.+?)</td>\\s*<td[^>]*>(.+?)</td>\\s*<td[^>]*>(.+?)</td>\\s*");
	protected String			TRAParamName					= "taxRateArea";
	protected String			ownerParamName					= "ownerNameIntermediary";
	protected DocsValidator		rejectNonRealEstateValidator	= null;
	protected static String		amountPattern					= "[^\\d.\\(\\)]+";
	protected static String		amountAbsoluteValuePattern		= "\\(([\\d.]+)\\)";


	public CAMegabyteCommonSiteTR(String rsRequestSolverName, String rsSitePath, String rsServerID, String rsPrmNameLink, long searchId, int miServerID) {
		super(rsRequestSolverName, rsSitePath, rsServerID, rsPrmNameLink, searchId, miServerID);
	}

	public CAMegabyteCommonSiteTR(long searchId) {
		super(searchId);
	}

	@Override
	protected void ParseResponse(String sAction, ServerResponse serverResponse,
			int viParseID) throws ServerResponseException {

		String rsResponse = serverResponse.getResult();
		ParsedResponse parsedResponse = serverResponse.getParsedResponse();
		rsResponse = StringEscapeUtils.unescapeHtml(rsResponse);
		rsResponse = rsResponse.replaceAll("[^\\p{Alnum}\\p{Punct}\\s]", "")
				.replaceAll("(?is)<script[^>]*>.*?</script>", "");
		
		String message = ro.cst.tsearch.utils.StringUtils.extractParameter(rsResponse, "(?is)(No\\s+Records\\s+Found\\s*!)");
		if (!message.isEmpty()) {
			parsedResponse.setError(message);
			return;
		}

		switch (viParseID) {
		case ID_SEARCH_BY_ADDRESS:// it's only on some sites
		case ID_SEARCH_BY_PARCEL:
		case ID_SEARCH_BY_MODULE38:// search by assessment # module - it's only on some sites
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
			HtmlParser3 htmlParser3 = new HtmlParser3(rsResponse);
			NodeList nodeList = htmlParser3.getNodeList();
			String parcelID = HtmlParser3.getValueFromAbsoluteCell(0, 1, HtmlParser3.findNode(nodeList, "Feeparcel"), "", true).trim();
			String year = HtmlParser3.getValueFromAbsoluteCell(0, 1, HtmlParser3.findNode(nodeList, "Taxyear"), "", true).trim();

			// get details
			String details = getDetails(serverResponse, rsResponse, parcelID);

			if (viParseID != ID_SAVE_TO_TSD) {
				String originalLink = sAction.replace("?", "&") + "&" + serverResponse.getRawQuerry();
				String sSave2TSDLink = getLinkPrefix(TSConnectionURL.idPOST) + originalLink;

				HashMap<String, String> data = new HashMap<String, String>();
				loadDataHash(data);
				data.put("year", year);

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

	protected String getDetails(ServerResponse serverResponse, String rsResponse, String parcelID) {
		try {

			/* If from memory - use it as is */
			if (!org.apache.commons.lang.StringUtils.containsIgnoreCase(rsResponse, "<html")) {
				return rsResponse;
			}

			StringBuilder detailsSB = new StringBuilder();
			rsResponse = Tidy.tidyParse(rsResponse, null);
			NodeList nodeList = new HtmlParser3(rsResponse).getNodeList();

			String mainDetails = extractMainDetails(nodeList, serverResponse);
			String extraDetails = extractExtraDetails(parcelID);

			return joinAndCleanExtractedDetails(detailsSB, mainDetails, extraDetails);

		} catch (Exception e) {
			logger.error("Error while getting details", e);
		}
		return null;
	}

	protected String extractMainDetails(NodeList nodeList, ServerResponse serverResponse) throws UnsupportedEncodingException {

		String mainDetails = "";
		NodeList formNodes = nodeList.extractAllNodesThatMatch(new TagNameFilter("form"), true)
				.extractAllNodesThatMatch(new HasAttributeFilter("name", "frmMain"), true);

		if (formNodes.size() > 0) {
			NodeList formChildren = formNodes.elementAt(0).getChildren();
			Node mainTableNode = HtmlParser3.getFirstTag(formChildren, TableTag.class, true);

			if (mainTableNode != null) {
				mainDetails = "<table>" + mainTableNode.toHtml()
						.replaceFirst("(?is)<td[^>]*>\\s*<table[^>]*>", "")
						.replaceFirst("(?is)</table>\\s*</td>", "")
						.replaceAll("(?is)</?table[^>]*>", "")
						+ "</table>";
				String lastLink = serverResponse.getQuerry();
				if (StringUtils.isNotEmpty(lastLink)) {

					// add tax rate area and owner(extracted from link) to details
					String taxRateArea = StringUtils.extractParameterFromUrl(lastLink, TRAParamName);
					if (!taxRateArea.isEmpty()) {
						taxRateArea = URLDecoder.decode(taxRateArea, "UTF-8");
						mainDetails = mainDetails.replaceFirst("(?is)(<tr[^>]*>\\s*<td[^>]*>\\s*Assessment\\s+Info\\s*</td>\\s*</tr>)",
								"$1<tr><td colspan=\"2\">Tax Rate Area</td><td colspan=\"3\" class=\"TRACells\">" + Matcher.quoteReplacement(taxRateArea)
										+ "</td></tr>");
					}

					String owners = StringUtils.extractParameterFromUrl(lastLink, ownerParamName).replaceAll("@@@", "&");
					if (!owners.isEmpty()) {
						owners = URLDecoder.decode(owners, "UTF-8");
						mainDetails = mainDetails.replaceFirst("(?is)(<table[^>]*>)",
								"$1<tr><td colspan=\"2\">Owner Name</td><td colspan=\"3\" class=\"ownersCells\"><b>" + owners + "</b></td></tr>");
					}
				}

				mainDetails = "<tr id=\"mainDetailsRow\" align=\"center\"><td colspan=\"5\">" + mainDetails
						.replaceAll("(?is)(<table)", "$1 border=\"1\" width=\"100%\" style=\"min-width:700px;\"") + "</td></tr>";
			}
		}

		return mainDetails;
	}

	protected String extractExtraDetails(String parcelID) throws UnsupportedEncodingException {

		String extraDetails = "";
		HtmlParser3 htmlParser3;
		NodeList nodeList;
		NodeList assessmentInfoNodes;

		// get the extra details rows, if any
		int extraDetailsIndex = 0;
		StringBuilder extraDetailsSB = new StringBuilder();
		Object extraDetailsLink = null;
		while ((extraDetailsLink = getSearch().getAdditionalInfo(parcelID + "_extraDetails_" + extraDetailsIndex)) != null) {

			// get owner name from intermediaries(it was set on link)
			String extraDetailsLinkStr = extraDetailsLink.toString();
			String owners = StringUtils.extractParameterFromUrl(extraDetailsLinkStr, ownerParamName).replaceAll("@@@", "&");
			owners = URLDecoder.decode(owners, "UTF-8");

			String extraDetail = getLinkContents(extraDetailsLinkStr);
			extraDetail = extraDetail.replaceAll("(?is)<script[^>]*>.*?</script>", "");
			extraDetail = Tidy.tidyParse(extraDetail, null);
			extraDetail = StringEscapeUtils.unescapeHtml(extraDetail);
			extraDetail = extraDetail.replaceAll("[^\\p{Alnum}\\p{Punct}\\s]", "");

			htmlParser3 = new HtmlParser3(extraDetail);
			nodeList = htmlParser3.getNodeList();
			assessmentInfoNodes = nodeList.extractAllNodesThatMatch(new RegexFilter("Assessment\\s+Info"), true);

			if (assessmentInfoNodes.size() > 0) {
				Node extraDetailsTableNode = HtmlParser3.getFirstParentTag(assessmentInfoNodes.elementAt(0), TableTag.class);

				if (extraDetailsTableNode != null) {

					TableTag extraDetailsTableTag = (TableTag) extraDetailsTableNode;
					extraDetailsTableTag.removeAttribute("cellspacing");
					extraDetailsTableTag.setAttribute("width", "100%");
					extraDetailsTableTag.setAttribute("border", "1");
					extraDetailsTableTag.setAttribute("style", "min-width:700px;");
					if (extraDetailsIndex == 0) {
						extraDetailsSB.append("<tr id=\"extraDetailsTitle\"><td><h3>Extra Document(s):</h3></td></tr>");
					}

					String extraDetailsTTStr = extraDetailsTableTag.toHtml();

					// add tax rate area and owner(extracted from link) to details
					String taxRateArea = StringUtils.extractParameterFromUrl(extraDetailsLinkStr, TRAParamName);
					if (!taxRateArea.isEmpty()) {
						taxRateArea = URLDecoder.decode(taxRateArea, "UTF-8");
						extraDetailsTTStr = extraDetailsTTStr.replaceFirst("(?is)(<tr[^>]*>\\s*<td[^>]*>\\s*Assessment\\s+Info\\s*</td>\\s*</tr>)",
								"$1<tr><td colspan=\"2\">Tax Rate Area</td><td colspan=\"3\" class=\"TRACells\">" + Matcher.quoteReplacement(taxRateArea)
										+ "</td></tr>");
					}

					if (!owners.isEmpty()) {
						extraDetailsTTStr = extraDetailsTTStr.replaceFirst("(?is)(<table[^>]*>)",
								"$1<tr><td colspan=\"2\">Owner Name</td><td colspan=\"3\" class=\"ownersCells\"><b>" + owners + "</b></td></tr>");
					}

					extraDetailsSB.append("<tr class=\"extraDetailsRows\" align=\"center\"><td>" +
							extraDetailsTTStr.replaceFirst("(?is)(<td[^>]*>)\\s*<table[^>]*>", "$1<table width=\"100%\" border=\"1\">")
							+ "</td></tr>");
				}
			}
			extraDetailsIndex++;
		}

		if (!extraDetailsSB.toString().isEmpty()) {
			extraDetails = extraDetailsSB.toString().replaceAll("(?is)<script[^>]*>.*?</script>", "");
		}

		return extraDetails;
	}

	protected String joinAndCleanExtractedDetails(StringBuilder detailsSB, String mainDetails, String extraDetails)
			throws UnsupportedEncodingException {

		if (!extraDetails.isEmpty()) {
			detailsSB.append("<tr id=\"mainDetailsTitle\"><td><h3>Main Document:</h3></td></tr>");
		}

		detailsSB.append(mainDetails);

		detailsSB.append(extraDetails);

		String details = "<table id=\"allDetails\" align=\"center\">" + detailsSB.toString() + "</table>";
		return details.replaceAll("(?is)</?(a|input|font)\\b[^>]*>", "")
				.replaceAll("(?s)(<td[^>]*)>((?:Assessment|Taxcode)\\s+Info)", "$1 align=\"center\"><b>$2</b>")
				.replaceAll("(?s)(<tr[^>]*)(>\\s*<td[^>]*>\\s*Assessment\\s*#)", "$1 style=\"font-weight:bold;\" $2")
				.replaceAll("(?is)<tr[^>]*>\\s*<td[^>]*>\\s*(My\\s+Cart|Items\\s+in\\s+cart|Navigation|"
						+ "Last\\s+Search|View(/Print)?\\s+TaxBill|Pay\\s+On-line)[^<]*</td>.*?</tr>", "")
				.replaceAll("(?is)(<td[^>]*>\\s*</td>)+", "")
				.replaceAll("(?is)(<[^>]*)bgcolor=\"[^\"]*\"([^>]*>)", "$1$2");
	}

	@SuppressWarnings("rawtypes")
	@Override
	protected Object parseAndFillResultMap(ServerResponse response, String detailsHtml, ResultMap resultMap) {
		try {
			resultMap.put(OtherInformationSetKey.SRC_TYPE.getKeyName(), getDataSite().getSiteTypeAbrev());

			HtmlParser3 htmlParser3 = new HtmlParser3(detailsHtml);
			Node mainDetailsRow = htmlParser3.getNodeById("mainDetailsRow");
			NodeList nodeList = null;
			if (mainDetailsRow != null) {// when parsing main details doc
				nodeList = mainDetailsRow.getChildren();

				// get parcelID
				String parcelID = HtmlParser3.getValueFromAbsoluteCell(0, 1, HtmlParser3.findNode(nodeList, "Feeparcel"), "", true).trim();
				if (StringUtils.isNotEmpty(parcelID)) {
					resultMap.put(PropertyIdentificationSetKey.PARCEL_ID.getKeyName(), parcelID);
				}
			} else {// when parsing an extra details doc
				nodeList = htmlParser3.getNodeList();

				// for supplemental docs put the assesment no on the parcel ID field
				String assessmentNo = HtmlParser3.getValueFromAbsoluteCell(0, 1, HtmlParser3.findNode(nodeList, "Assessment #"), "", true).trim();
				if (StringUtils.isNotEmpty(assessmentNo)) {
					resultMap.put(PropertyIdentificationSetKey.PARCEL_ID.getKeyName(), assessmentNo);
				}
			}
			if (nodeList.size() > 0) {

				// get address
				String situsAddress = HtmlParser3.getValueFromAbsoluteCell(0, 1, HtmlParser3.findNode(nodeList, "Situs"), "", true).trim();
				if (StringUtils.isNotEmpty(situsAddress)) {

					// remove lot - e.g. some addresses are like 'NORTH/THIRD LOT 4'
					situsAddress = situsAddress.replaceFirst("(?i)\\s+LO?TS?\\s*\\d+[\\d\\s-]*$", "");

					String streetNo = StringUtils.extractParameter(situsAddress, "^\\s*(\\d+)");
					situsAddress = situsAddress.replaceFirst("\\s*" + streetNo + "\\s*", "");
					streetNo = streetNo.replaceFirst("^0+", "");
					if (StringUtils.isNotEmpty(streetNo)) {
						resultMap.put(PropertyIdentificationSetKey.STREET_NO.getKeyName(), streetNo);
					}

					if (StringUtils.isNotEmpty(situsAddress)) {
						resultMap.put(PropertyIdentificationSetKey.STREET_NAME.getKeyName(), situsAddress);
					}
				} else {// some sites have address(e.g. San Benito), the others have names instead(e.g. Stanislaus)

					// parse names
					NodeList ownersList = nodeList.extractAllNodesThatMatch(new HasAttributeFilter("class", "ownersCells"), true);
					if (ownersList.size() > 0) {
						String owners = ownersList.elementAt(0).toPlainTextString().trim();
						resultMap.put(PropertyIdentificationSetKey.NAME_ON_SERVER.getKeyName(), owners);
						parseNames(resultMap);
					}
				}

				NodeList taxRateAreaNodes = nodeList.extractAllNodesThatMatch(new HasAttributeFilter("class", "TRACells"), true);
				if (taxRateAreaNodes.size() > 0) {
					String taxRateArea = taxRateAreaNodes.elementAt(0).toPlainTextString().trim();
					resultMap.put(PropertyIdentificationSetKey.AREA_CODE.getKeyName(), taxRateArea);
				}
				
				// get amount paid
				String amountPaid = HtmlParser3.getValueFromAbsoluteCell(0, 3, HtmlParser3.findNode(nodeList, "Total Paid"), "", true)
						.replaceAll(amountPattern, "");
				if (amountPaid.contains("(")) {
					amountPaid = "-" + StringUtils.extractParameter(amountPaid, amountAbsoluteValuePattern);
				}
				if (StringUtils.isNotEmpty(amountPaid)) {
					resultMap.put(TaxHistorySetKey.AMOUNT_PAID.getKeyName(), amountPaid);
				}

				// get base amount sums - total and per installments
				String[] baseAmountSum = { "", "", "" };
				String[] baseAmountPartial = { "", "", "" };
				Node tableNode = HtmlParser3.findAncestorWithClass(HtmlParser3.findNode(nodeList, "Taxcode Info"), TableTag.class);
				if (tableNode != null) {
					TableTag tableTag = (TableTag) tableNode;
					TableRow[] rows = tableTag.getRows();

					for (int i = 0; i < rows.length; i++) {
						if (rows[i].getColumnCount() == 1 && rows[i].toPlainTextString().matches("(?is)\\s*Taxcode\\s+Info\\s*")) {
							for (int j = i + 2; j < rows.length; j++) {
								TableColumn[] columns = rows[j].getColumns();
								if (columns.length >= 5) {
									baseAmountPartial[0] = columns[2].toPlainTextString().replaceAll(amountPattern, "");
									if (baseAmountPartial[0].contains("(")) {
										baseAmountPartial[0] = "-" + StringUtils.extractParameter(baseAmountPartial[0], amountAbsoluteValuePattern);
									}
									baseAmountSum[0] += baseAmountPartial[0] + "+";

									baseAmountPartial[1] = columns[3].toPlainTextString().replaceAll(amountPattern, "");
									if (baseAmountPartial[1].contains("(")) {
										baseAmountPartial[1] = "-" + StringUtils.extractParameter(baseAmountPartial[1], amountAbsoluteValuePattern);
									}
									baseAmountSum[1] += baseAmountPartial[1] + "+";

									baseAmountPartial[2] = columns[4].toPlainTextString().replaceAll(amountPattern, "") ;
									if (baseAmountPartial[2].contains("(")) {
										baseAmountPartial[2] = "-" + StringUtils.extractParameter(baseAmountPartial[2], amountAbsoluteValuePattern);
									}
									baseAmountSum[2] += baseAmountPartial[2]+"+";
								}
							}
							break;
						}
					}
					baseAmountSum[0] = baseAmountSum[0].replaceFirst("\\+$", "");
					baseAmountSum[1] = baseAmountSum[1].replaceFirst("\\+$", "");
					baseAmountSum[2] = baseAmountSum[2].replaceFirst("\\+$", "");
				}

				// get total base amount
				String totalBaseAmount = "";
				if (StringUtils.isNotEmpty(baseAmountSum[2])) {
					totalBaseAmount = GenericFunctions1.sum(baseAmountSum[2], searchId);
					resultMap.put(TaxHistorySetKey.BASE_AMOUNT.getKeyName(), totalBaseAmount);
				}

				// tax installment set: ba, ad, ap, status
				Map<String, String[]> installmentsMap = new HashMap<String, String[]>();
				List<List> installmentsBody = new ArrayList<List>();
				ResultTable resultTable = new ResultTable();

				String[] installmentsHeader = {
						TaxInstallmentSetKey.BASE_AMOUNT.getShortKeyName(),
						TaxInstallmentSetKey.TOTAL_DUE.getShortKeyName(),
						TaxInstallmentSetKey.AMOUNT_PAID.getShortKeyName(),
						TaxInstallmentSetKey.STATUS.getShortKeyName(),
				};

				for (String s : installmentsHeader) {
					installmentsMap.put(s, new String[] { s, "" });
				}

				String[] instBA = new String[2];
				String[] instAD = new String[2];
				String[] instAP = new String[2];
				String[] instStatus = new String[2];

				String[] instDatePaid = new String[2];

				for (int i = 0; i < 2; i++) {
					List<String> installmentsRow = new ArrayList<String>();

					// get base amount
					if (StringUtils.isNotEmpty(baseAmountSum[i])) {
						instBA[i] = GenericFunctions1.sum(baseAmountSum[i], searchId);
						installmentsRow.add(instBA[i]);
					} else {
						installmentsRow.add("");
					}

					// get amount due
					instAD[i] = HtmlParser3.getValueFromAbsoluteCell(0, 1 + i, HtmlParser3.findNode(nodeList, "Balance"), "", true)
							.replaceAll(amountPattern, "");
					if (instAD[i].contains("(")) {
						instAD[i] = "-" + StringUtils.extractParameter(instAD[i], amountAbsoluteValuePattern);
					}
					if (StringUtils.isNotEmpty(instAD[i])) {
						installmentsRow.add(instAD[i]);
					} else {
						installmentsRow.add("");
					}

					// get amount paid
					instAP[i] = HtmlParser3.getValueFromAbsoluteCell(0, 1 + i, HtmlParser3.findNode(nodeList, "Total Paid"), "", true)
							.replaceAll(amountPattern, "");
					if (instAP[i].contains("(")) {
						instAP[i] = "-" + StringUtils.extractParameter(instAP[i], amountAbsoluteValuePattern);
					}
					if (StringUtils.isNotEmpty(instAP[i])) {
						installmentsRow.add(instAP[i]);
					} else {
						installmentsRow.add("");
					}

					// get payment status
					instStatus[i] = HtmlParser3.getValueFromAbsoluteCell(0, 1 + i, HtmlParser3.findNode(nodeList, "Paid Status"), "", true).trim();
					if (StringUtils.isNotEmpty(instStatus[i])) {
						instStatus[i] = instStatus[i].toUpperCase();
						if (instStatus[i].equals("LATE") || instStatus[i].equals("DUE")) {
							installmentsRow.add("UNPAID");
						} else if (instStatus[i].equals("PAID")) {
							installmentsRow.add(instStatus[i]);
						}
					} else {
						installmentsRow.add("");
					}
					instDatePaid[i] = HtmlParser3.getValueFromAbsoluteCell(0, 1 + i, HtmlParser3.findNode(nodeList, "Due/Paid Date"), "", true).trim();
					if (installmentsRow.size() == installmentsHeader.length) {
						installmentsBody.add(installmentsRow);
					}
				}

				if (!installmentsBody.isEmpty()) {
					resultTable.setHead(installmentsHeader);
					resultTable.setMap(installmentsMap);
					resultTable.setBody(installmentsBody);
					resultTable.setReadOnly();
					resultMap.put("TaxInstallmentSet", resultTable);
				}

				// get receipts values and dates
				List<List> bodyHist = new ArrayList<List>();
				resultTable = new ResultTable();
				String[] header = { "ReceiptAmount", "ReceiptDate" };
				Map<String, String[]> map = new HashMap<String, String[]>();
				map.put("ReceiptAmount", new String[] { "ReceiptAmount", "" });
				map.put("ReceiptDate", new String[] { "ReceiptDate", "" });

				for (int i = 0; i < 2; i++) {
					List<String> receiptRow = new ArrayList<String>();
					if (instStatus[i].equals("PAID")) {

						// get receipt value
						if (StringUtils.isNotEmpty(instAP[i])) {
							receiptRow.add(instAP[i]);
						} else {
							receiptRow.add("");
						}

						// get receipt date
						if (StringUtils.isNotEmpty(instDatePaid[i]) && instDatePaid[i].matches("\\d+/\\d+/\\d+")) {
							receiptRow.add(instDatePaid[i]);
						} else {
							receiptRow.add("");
						}

						if (receiptRow.size() == header.length) {
							bodyHist.add(receiptRow);
						}
					}
				}

				if (!bodyHist.isEmpty()) {
					resultTable.setHead(header);
					resultTable.setBody(bodyHist);
					resultTable.setMap(map);
					resultTable.setReadOnly();
					resultMap.put("TaxHistorySet", resultTable);
				}

				// get tax year
				String taxYear = HtmlParser3.getValueFromAbsoluteCell(0, 1, HtmlParser3.findNode(nodeList, "Taxyear"), "", true).trim();
				if (StringUtils.isNotEmpty(taxYear) && taxYear.matches("\\d+")) {
					resultMap.put(TaxHistorySetKey.YEAR.getKeyName(), taxYear);

					// get amount due/delinquent amount
					String balanceDue = HtmlParser3.getValueFromAbsoluteCell(0, 3, HtmlParser3.findNode(nodeList, "Balance"), "", true)
							.replaceAll(amountPattern, "");
					if (balanceDue.contains("(")) {
						balanceDue = "-" + StringUtils.extractParameter(balanceDue, amountAbsoluteValuePattern);
					}
					if (StringUtils.isNotEmpty(balanceDue)) {
						int currentYearInt = getDataSite().getCurrentTaxYear();
						int taxYearInt = Integer.parseInt(taxYear);
						if (currentYearInt == taxYearInt) {
							resultMap.put(TaxHistorySetKey.TOTAL_DUE.getKeyName(), balanceDue);
						} else if (currentYearInt > taxYearInt) {
							resultMap.put(TaxHistorySetKey.PRIOR_DELINQUENT.getKeyName(), balanceDue);
						}
					}
				}
			}

			resultMap.put(PropertyIdentificationSetKey.PROPERTY_TYPE.getKeyName(), "Real Estate");
			String parcelID = org.apache.commons.lang.StringUtils.defaultString((String) resultMap.get(PropertyIdentificationSetKey.PARCEL_ID.getKeyName()));
			if (parcelID.matches("^(810|820|830|835|860|861|905|906|908|910|920|980|981|990|991).*")) {
				// asmt numbers(parcel ID here) that start with these values are not real estate;
				// got the values from this page: http://000sweb.co.monterey.ca.us/assessor/asmt-query.htm
				resultMap.put(PropertyIdentificationSetKey.PROPERTY_TYPE.getKeyName(), "");
			}
		} catch (Exception e) {
			e.printStackTrace();
		}

		return null;
	}

	@Override
	public Collection<ParsedResponse> smartParseIntermediary(
			ServerResponse response, String htmlTable, StringBuilder outputTable) {
		Vector<ParsedResponse> intermediaryResponse = new Vector<ParsedResponse>();
		try {
			ParsedResponse parsedResponse = response.getParsedResponse();
			HtmlParser3 htmlParser3 = new HtmlParser3(htmlTable);
			NodeList nodes = htmlParser3.getNodeList();

			Node intermediariesTable = HtmlParser3.getNodeByTypeAttributeDescription(nodes, "table", "", "",
					new String[] { "FeeParcel", "Asmt", "Year" }, true);
			if (intermediariesTable != null && intermediariesTable instanceof TableTag) {
				String dataSitePath = "/" + getDataSite().getLink().replaceFirst(getDataSite().getServerHomeLink(), "");
				dataSitePath = dataSitePath.substring(0, dataSitePath.indexOf("Inquiry.aspx"));
				String header = "";
				String previousParcel = "";

				TableTag table = (TableTag) intermediariesTable;
				TableRow[] rows = table.getRows();

				int noOfRows = rows.length;
				int extraDetailsIndex = 0;

				if (noOfRows > 0) {
					TableRow headerRow = rows[0];
					if (headerRow != null) {
						header += getIntermediariesHeader(headerRow);
					}

					for (int i = 1; i < noOfRows; i++) {
						TableRow row = rows[i];
						String detailsLink = "";

						String addrOrOwnerCol = "";
						if (noOfRows > i + 1) {// get owner/addr col to add it to current result
							addrOrOwnerCol = rows[i + 1].toPlainTextString().replaceAll("(?is)</?tr[^>]*>", "").trim();
						}

						if (row.getColumnCount() >= 5 && org.apache.commons.lang.StringUtils.isNotBlank(row.toPlainTextString())) {
							String parcel = row.getColumns()[0].toPlainTextString().trim();

							// add tax rate area to link
							String taxRateArea = row.getColumns()[3].toPlainTextString().trim();
							if (parcel.equals(previousParcel)) {

								// if prev result has same parcel set this row's link on the Search obj for retrieval when getting details for this pid
								String extraDetailsLink = "";
								Node extraDetailsLinkNode = (Node) row.getColumns()[1];
								extraDetailsLinkNode = HtmlParser3.getFirstTag(extraDetailsLinkNode.getChildren(), LinkTag.class, true);

								if (extraDetailsLinkNode != null) {
									LinkTag extraDetailsLinkTag = (LinkTag) extraDetailsLinkNode;
									extraDetailsLink = getDataSite().getServerHomeLink() + dataSitePath.substring(1) + extraDetailsLinkTag.getLink();
									if (siteHasOwner()) {
										extraDetailsLink = extraDetailsLink + "&" + ownerParamName + "=" + addrOrOwnerCol.replaceAll("&", "@@@");
									}
									if (StringUtils.isNotEmpty(taxRateArea)) {
										extraDetailsLink = extraDetailsLink + "&" + TRAParamName + "=" + taxRateArea;
									}
									getSearch().setAdditionalInfo(parcel + "_extraDetails_" + extraDetailsIndex, extraDetailsLink);
									extraDetailsIndex++;
								}

								// don't display lines with same parcel as intermediary results;
								// their details will be displayed/saved when the main doc details will be displayed/saved
								continue;
							} else {
								extraDetailsIndex = 0;
								Node linkNode = (Node) row.getColumns()[1];
								linkNode = HtmlParser3.getFirstTag(linkNode.getChildren(), LinkTag.class, true);

								if (linkNode != null) {
									LinkTag linkTag = (LinkTag) linkNode;

									detailsLink = CreatePartialLink(TSConnectionURL.idGET) + dataSitePath + linkTag.getLink();
									if (siteHasOwner()) {
										detailsLink = detailsLink + "&" + ownerParamName + "=" + addrOrOwnerCol.replaceAll("&", "@@@");
									}
									if (StringUtils.isNotEmpty(taxRateArea)) {
										detailsLink = detailsLink + "&" + TRAParamName + "=" + taxRateArea;
									}

									linkTag.setLink(detailsLink);
								}
							}

							String fullRow = row.toHtml().replaceFirst("(?is)(<tr[^>]*>)", "$1" + "<td>" + addrOrOwnerCol + "</td>");
							if (extraDetailsIndex > 0) {// if it's a row wish same parcel as prev row, remove its link
								// because its link contents will be retrieved in details when clicking on the first(main) row with this pid
								fullRow = fullRow.replaceAll("(?is)</?a\\b[^>]*>", "");
							}
							previousParcel = parcel;

							ParsedResponse currentResponse = new ParsedResponse();
							currentResponse.setAttribute(ParsedResponse.SERVER_ROW_RESPONSE, fullRow);
							currentResponse.setOnlyResponse(fullRow);
							currentResponse.setPageLink(new LinkInPage(detailsLink, detailsLink, TSServer.REQUEST_SAVE_TO_TSD));

							ResultMap resultMap = parseIntermediaryRow(fullRow);
							resultMap.put(OtherInformationSetKey.SRC_TYPE.getKeyName(), getDataSite().getSiteTypeAbrev());
							if (StringUtils.isNotEmpty(taxRateArea)) {
								resultMap.put(PropertyIdentificationSetKey.AREA_CODE.getKeyName(), taxRateArea);
							}

							Bridge bridge = new Bridge(currentResponse, resultMap, searchId);
							DocumentI document = (TaxDocumentI) bridge.importData();
							currentResponse.setDocument(document);

							intermediaryResponse.add(currentResponse);
						}
					}
				}

				parsedResponse.setHeader(header);
				parsedResponse.setFooter("</table>");
				outputTable.append(table);
			}
		} catch (Throwable t) {
			logger.error("Error while parsing intermediary data", t);
		}
		return intermediaryResponse;
	}

	protected String getIntermediariesHeader(TableRow headerRow) {
		String columnName = "";
		if (siteHasOwner()) {
			columnName = "Owner Name";
		} else {
			columnName = "Address";
		}

		return "<table border = \"1\" align=\"center\" style=\"min-width:600px;\">"
				+ headerRow.toHtml().replaceAll("<td\\b", "<th").replaceFirst("(?is)(<tr[^>]*>)", "$1<th>" + columnName + "</th>");
	}

	@Override
	protected void setModulesForAutoSearch(TSServerInfo serverInfo) {
		FilterResponse rejectNonRealEstateFilter = new RejectNonRealEstate(SearchAttributes.OWNER_OBJECT, searchId);
		rejectNonRealEstateFilter.setThreshold(new BigDecimal("0.65"));
		rejectNonRealEstateValidator = rejectNonRealEstateFilter.getValidator();

		/* implement in child class */
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

	protected ResultMap parseIntermediaryRow(String fullRow) {
		ResultMap resultMap = new ResultMap();
		try {
			Matcher rowMatcher = intermediaryRowPattern.matcher(fullRow);
			if (rowMatcher.find()) {
				String parcelID = "";
				if (fullRow.matches("(?is).*?<a[^>]*>.*")) {
					parcelID = rowMatcher.group(2).replaceAll("<[^>]*>", "").trim();
				}

				if (StringUtils.isNotEmpty(parcelID)) {
					resultMap.put(PropertyIdentificationSetKey.PARCEL_ID.getKeyName(), parcelID);
				}

				String year = rowMatcher.group(4).trim();
				if (StringUtils.isNotEmpty(year)) {
					resultMap.put(TaxHistorySetKey.YEAR.getKeyName(), year);
				}
			}
			if (siteHasOwner()) {
				String nameOnServer = rowMatcher.group(1).trim();
				if (StringUtils.isNotEmpty(nameOnServer)) {
					resultMap.put(PropertyIdentificationSetKey.NAME_ON_SERVER.getKeyName(), nameOnServer);
				}
				parseNames(resultMap);
			} else {
				String address = rowMatcher.group(1).trim();
				address = address.replaceFirst("(?i)\\s+LO?TS?\\s*\\d+[\\d\\s-]*$", "");// remove lot - e.g. some addresses are like 'NORTH/THIRD LOT 4'

				if (StringUtils.isNotEmpty(address)) {
					String streetNo = StringUtils.extractParameter(address, "^\\s*(\\d+)");
					address = address.replaceFirst("\\s*" + streetNo + "\\s*", "");
					streetNo = streetNo.replaceFirst("^0+", "");

					if (StringUtils.isNotEmpty(streetNo)) {
						resultMap.put(PropertyIdentificationSetKey.STREET_NO.getKeyName(), streetNo);
					}
					if (StringUtils.isNotEmpty(address)) {
						resultMap.put(PropertyIdentificationSetKey.STREET_NAME.getKeyName(), address);
					}
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		return resultMap;
	}

	@SuppressWarnings("rawtypes")
	protected void parseNames(ResultMap resultMap) {
		String ownersSTR = (String) resultMap.get(PropertyIdentificationSetKey.NAME_ON_SERVER.getKeyName());
		ownersSTR = ownersSTR.replaceFirst("(?is)(\\s*\\bET\\.?\\s*AL\\.?\\b)(\\s*TR(?:USTEE)?S\\s*)$", "$2$1");

		try {
			ArrayList<List> body = new ArrayList<List>();

			List<String> all_names = new ArrayList<String>();
			all_names.add(ownersSTR);
			String[] names = { "", "", "", "", "", "" };
			String[] suffixes = { "", "" };
			String[] type = { "", "" };
			String[] otherType = { "", "" };

			for (String n : all_names) {
				n = n.toUpperCase().trim().replaceAll("\\s+", " ");
				if (NameUtils.isCompany(n)) {// e.g. on CAStanislausTR 'Orvis/Snow LP' is company(pid 001-008-023-000)-> if treated normally it's parsed wrong
					names[2] = n;
				} else if (ro.cst.tsearch.utils.StringUtils.isNotEmpty(n)) {
					names = StringFormats.parseNameNashville(n, true);
					type = GenericFunctions.extractAllNamesType(names);
					otherType = GenericFunctions.extractAllNamesOtherType(names);
					suffixes = GenericFunctions.extractNameSuffixes(names);
				}

				GenericFunctions.addOwnerNames(names, suffixes[0], suffixes[1], type, otherType, NameUtils.isCompany(names[2]),
						NameUtils.isCompany(names[5]), body);
			}

			if (body.size() > 0) {
				GenericFunctions.storeOwnerInPartyNames(resultMap, body, true);
			}

		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	protected boolean siteHasOwner() {
		// some sites(e.g. San Benito, Merced) have the property address in intermediaries, others have the owner name instead(e.g. Stanislaus, Monterey):
		return false;
	}
}