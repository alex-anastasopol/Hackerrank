package ro.cst.tsearch.servers.types;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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

import ro.cst.tsearch.Search;
import ro.cst.tsearch.bean.SearchAttributes;
import ro.cst.tsearch.exceptions.ServerResponseException;
import ro.cst.tsearch.extractor.xml.GenericFunctions1;
import ro.cst.tsearch.extractor.xml.ResultMap;
import ro.cst.tsearch.extractor.xml.ResultTable;
import ro.cst.tsearch.parser.HtmlParser3;
import ro.cst.tsearch.servers.TSConnectionURL;
import ro.cst.tsearch.servers.info.TSServerInfo;
import ro.cst.tsearch.servers.info.TSServerInfoModule;
import ro.cst.tsearch.servers.response.LinkInPage;
import ro.cst.tsearch.servers.response.ParsedResponse;
import ro.cst.tsearch.servers.response.ServerResponse;
import ro.cst.tsearch.servers.response.OtherInformationSet.OtherInformationSetKey;
import ro.cst.tsearch.servers.response.PropertyIdentificationSet.PropertyIdentificationSetKey;
import ro.cst.tsearch.servers.response.TaxHistorySet.TaxHistorySetKey;
import ro.cst.tsearch.servers.response.TaxInstallmentSet.TaxInstallmentSetKey;
import ro.cst.tsearch.utils.StringUtils;
import ro.cst.tsearch.utils.Tidy;

public class CASacramentoTR extends TSServer {

	private static final long	serialVersionUID	= 7345962955249029771L;

	public CASacramentoTR(long searchId) {
		super(searchId);
	}

	public CASacramentoTR(String rsRequestSolverName, String rsSitePath, String rsServerID, String rsPrmNameLink, long searchId, int miServerID) {
		super(rsRequestSolverName, rsSitePath, rsServerID, rsPrmNameLink, searchId, miServerID);
	}

	@Override
	protected void ParseResponse(String sAction, ServerResponse serverResponse,
			int viParseID) throws ServerResponseException {

		String rsResponse = serverResponse.getResult();
		ParsedResponse parsedResponse = serverResponse.getParsedResponse();
		rsResponse = StringEscapeUtils.unescapeHtml(rsResponse).replaceAll("[^\\p{Alnum}\\p{Punct}\\s]", "");

		// treat no results found message
		String message = StringUtils.extractParameter(rsResponse,
				"(?is)<div[^>]*>\\s*(Parcel\\s+number\\s+not\\s+found\\s*--?\\s*please\\s+check\\s+and\\s+re-?enter.)\\s*</div");
		if (!message.isEmpty()) {
			parsedResponse.setError(message + ".");
			return;
		}

		// treat wrong format message
		message = StringUtils.extractParameter(rsResponse,
				"(?is)<div[^>]*>\\s*(You\\s+must\\s+enter\\s+a\\s+parcel\\s+number.)\\s*</div>");
		if (!message.isEmpty()) {
			parsedResponse.setError(message + ".");
			return;
		}

		switch (viParseID) {
		case ID_SEARCH_BY_PARCEL:
		case ID_DETAILS:
		case ID_SAVE_TO_TSD:

			// get details
			String details = getDetails(rsResponse);

			if (StringUtils.isNotEmpty(details)) {
				String accountNo = "";
				HtmlParser3 htmlParser3 = new HtmlParser3(details);
				Node mainDetailsRow = htmlParser3.getNodeById("mainDetailsRow");
				if (mainDetailsRow != null) {
					// get accountId from parcel ID for main doc
					accountNo = StringUtils
							.extractParameter(details, "(?is)(?:<strong>)?Parcel\\s+Number\\s*:\\s*(?:</strong>)?\\s*([\\s\\d-]+)")
							.trim();

				} else {
					NodeList extraDetailsRow = htmlParser3.getNodeList().extractAllNodesThatMatch(new HasAttributeFilter("class", "extraDetailsRows"));
					if (extraDetailsRow.size() > 0) {
						// get accountId from bill no for extra docs
						accountNo = StringUtils
								.extractParameter(extraDetailsRow.elementAt(0).toHtml(),
										"(?is)(?:<strong>)?Bill\\s+Number\\s*:\\s*(?:</strong>)?\\s*([\\s\\d-]+)")
								.trim();
					}
				}

				if (viParseID != ID_SAVE_TO_TSD) {
					String originalLink = sAction.replace("?", "&") + "&" + serverResponse.getRawQuerry();
					String sSave2TSDLink = getLinkPrefix(TSConnectionURL.idPOST) + originalLink;

					HashMap<String, String> data = new HashMap<String, String>();
					loadDataHash(data);

					if (isInstrumentSaved(accountNo, null, data)) {
						details += CreateFileAlreadyInTSD();
					} else {
						mSearch.addInMemoryDoc(sSave2TSDLink, details);
						details = addSaveToTsdButton(details, sSave2TSDLink, ID_DETAILS);
					}

					parsedResponse.setPageLink(new LinkInPage(sSave2TSDLink, originalLink, TSServer.REQUEST_SAVE_TO_TSD));
					parsedResponse.setResponse(details);

				} else {
					msSaveToTSDResponce = details + CreateFileAlreadyInTSD();
					if (mainDetailsRow != null) {// for the main details doc
						details = "<table align=\"center\" >" + mainDetailsRow.toHtml() + "</table>";
					} else {// for an extra details doc
						details = "<table align=\"center\" >" + details + "</table>";
					}

					String filename = accountNo + ".html";
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
			if (!org.apache.commons.lang.StringUtils.containsIgnoreCase(rsResponse, "<body")) {
				// used '<body' instead of '<html' because it contains <!DOCTYPE html instead of <html !!
				return rsResponse;
			}

			rsResponse = Tidy.tidyParse(rsResponse, null);
			String allDetails = "";
			StringBuilder mainDetailsSB = new StringBuilder();
			StringBuilder extraDetailsSB = new StringBuilder();
			StringBuilder tempSB = new StringBuilder();
			String propertyInfo = "";

			String serverHomeLink = getDataSite().getServerHomeLink();
			HtmlParser3 htmlParser3 = new HtmlParser3(rsResponse);
			NodeList nodeList = htmlParser3.getNodeList();
			Node mainContentDiv = htmlParser3.getNodeById("maincontent");

			if (mainContentDiv != null) {

				nodeList = mainContentDiv.getChildren();

				// get tax rate area p
				propertyInfo += getParentNodeContent(nodeList, "(?is)Tax Rate Area").replaceFirst("(?is)<p[^>]*>", "<p>");

				// get parcel div
				propertyInfo += getParentNodeContent(nodeList, "(?is)Parcel Number");
				if (!propertyInfo.isEmpty()) {
					mainDetailsSB.append("<tr><td align=\"center\"><strong>Tax Bill Summary</strong></tr>");
					mainDetailsSB.append("<tr><td id=\"parcelInfo\">" + propertyInfo + "</td></tr>");
				}
			}

			// get bills table and bills details for each table row
			NodeList billsTables = nodeList.extractAllNodesThatMatch(new TagNameFilter("table"), true);
			if (billsTables.size() > 0) {
				TableTag billsTableT = (TableTag) billsTables.elementAt(0);
				billsTableT.setAttribute("id", "billsTable");
				String billsTableS = billsTableT.toHtml();
				if (!StringUtils.extractParameter(billsTableS, "(?is)<th[^>]*>\\s*(Bill\\s+Type)\\s*</th>").isEmpty()) {
					// mainDetailsSB.append("<tr><td>" + billsTableS + "</td></tr>"); - this is the intermediaries results table
					// shouldn't be added to main details doc

					TableRow[] billsTableRow = billsTableT.getRows();

					boolean isFirstBill = true;
					for (int i = 0; i < billsTableRow.length; i++) {

						tempSB = new StringBuilder();
						StringBuilder billDetails = new StringBuilder();
						String levies = "";

						// get bill number details
						if (billsTableRow[i].getColumnCount() > 0) {
							TableColumn[] col = billsTableRow[i].getColumns();
							Node billDetailsLinkTag = col[0].getFirstChild();
							if (billDetailsLinkTag instanceof LinkTag) {
								String billDetailsLink = serverHomeLink + ((LinkTag) billDetailsLinkTag).getLink();
								String billDetailsContent = getLinkContents(billDetailsLink);

								htmlParser3 = new HtmlParser3(billDetailsContent);
								nodeList = htmlParser3.getNodeList();

								// get bill info div
								String propertyMoreInfo = getParentNodeContent(nodeList, "(?is)Assessment\\s+Year");
								if (!propertyMoreInfo.isEmpty()) {
									billDetails.append(propertyMoreInfo);
								}

								NodeList billDetailsTables = nodeList.extractAllNodesThatMatch(new TagNameFilter("table"), true);
								if (billDetailsTables.size() > 0) {
									TableTag billDetailsTableT = (TableTag) billDetailsTables.elementAt(0);
									billDetailsTableT.setAttribute("class", "billDetailsTable");
									billsTableS = billDetailsTableT.toHtml();
									if (!StringUtils.extractParameter(billsTableS, "(?is)<th[^>]*>\\s*(First\\s+Installment)\\s*</th>").isEmpty()) {
										billDetails.append(billsTableS);
									}
								}

								// get levy details - for each row..
								String leviesLink = StringUtils.extractParameter(billDetailsContent,
										"(?is)<a[^>]*href=\"([^\"]*)\"[^>]*>\\s*View\\s+Direct\\s+Levies\\s*</a>");
								if (!leviesLink.isEmpty()) {
									String leviesContent = getLinkContents(serverHomeLink + leviesLink);
									htmlParser3 = new HtmlParser3(leviesContent);
									nodeList = htmlParser3.getNodeList();

									// get levyInfo
									String levyInfo = getParentNodeContent(nodeList, "(?is)Direct\\s+Levy\\s+Amount");
									if (!levyInfo.isEmpty()) {
										levies = levyInfo;
									}

									NodeList leviesTable = nodeList.extractAllNodesThatMatch(new TagNameFilter("table"), true);
									if (leviesTable.size() > 0) {
										String leviesTableS = leviesTable.toHtml().replaceAll("(?is)</?(span|br)\\b[^>]*>", "");
										if (!StringUtils.extractParameter(leviesTableS, "(?is)<th[^>]*>\\s*(Levy\\s+Name)\\s*</th>").isEmpty()) {
											levies += leviesTableS;
										}
									}
								}
							}
						}

						if (!billDetails.toString().isEmpty()) {
							tempSB.append("<tr><td align=\"center\"><strong>Tax Bill Details</strong></tr>");
							tempSB.append("<tr class=\"billDetailsRows\" id=\"billDetails" + i + "\"><td>" + billDetails.toString() + "</td></tr>");
						}
						if (!levies.isEmpty()) {
							tempSB.append("<tr><td align=\"center\"><strong>Levy Details</strong></td></tr>");
							tempSB.append("<tr class=\"leviesRows\"><td>" + levies + "</td></tr>");
						}

						if (!tempSB.toString().isEmpty()) {
							if (isFirstBill) {// add to main bill details
								mainDetailsSB.append(tempSB.toString());
							} else {// add to extra bills details
								if (!propertyInfo.isEmpty()) {
									extraDetailsSB.append("<tr><td align=\"center\"><strong>Tax Bill Summary</strong></tr>");
									extraDetailsSB.append("<tr><td>" + propertyInfo + "</td></tr>");
								}
								extraDetailsSB.append(tempSB.toString());
							}

							isFirstBill = false;
						}
					}
				}
			}

			// get Parcel Payment History link contents
			String parcelBillHistoryLink = StringUtils.extractParameter(rsResponse,
					"(?is)<a[^>]*href=\"([^\"]*)\"[^>]*>\\s*Parcel\\s+Payment\\s+History\\s*</a>");

			if (!parcelBillHistoryLink.isEmpty()) {
				String parcelBillHistory = getLinkContents(serverHomeLink + parcelBillHistoryLink);
				htmlParser3 = new HtmlParser3(parcelBillHistory);
				nodeList = htmlParser3.getNodeList();
				NodeList historyBillsTables = nodeList.extractAllNodesThatMatch(new TagNameFilter("table"), true);
				if (historyBillsTables.size() > 0) {
					TableTag historyBillsTableT = (TableTag) historyBillsTables.elementAt(0);
					historyBillsTableT.setAttribute("id", "historyBillsTable");

					String historyBillsTable = historyBillsTableT.toHtml();
					mainDetailsSB.append("<tr><td align=\"center\"><strong>Parcel Payment History</strong></td></tr>");
					mainDetailsSB.append("<tr><td>" + historyBillsTable + "</td></tr>");
				}
			}

			if (!mainDetailsSB.toString().isEmpty()) {
				if (!extraDetailsSB.toString().isEmpty()) {
					allDetails += "<tr><td><h3>Main Document:</h3></td></tr>";
				}
				allDetails += "<tr id=\"mainDetailsRow\"><td><table align=\"center\" style=\"min-width:700px;\" border=\"1\">"
						+ mainDetailsSB.toString()
						+ "</table></td></tr>";
			}

			if (!extraDetailsSB.toString().isEmpty()) {
				allDetails += "<tr><td><h3>Extra Document(s):</h3></td></tr>";
				allDetails += "<tr class=\"extraDetailsRows\"><td><table align=\"center\" style=\"min-width:700px;\" border=\"1\">"
						+ extraDetailsSB.toString()
						+ "</table></td></tr>";
			}

			if (!allDetails.isEmpty()) {
				allDetails = "<table id=\"allDetails\" align=\"center\">" +
						allDetails.replaceFirst("(?is)<a[^>]*>\\s*View\\s+Direct\\s+Levies\\s*</a>", "")
								.replaceAll("(?is)</?(a|input)\\b[^>]*>", "")
						+ "</table>";

				return allDetails;
			}

		} catch (Exception e) {
			logger.error("Error while getting details", e);
		}
		return null;
	}

	protected String getParentNodeContent(NodeList nodeList, String regex) {
		String parentNodeContent = "";
		NodeList taxRateAreaNodes = nodeList.extractAllNodesThatMatch(new RegexFilter(regex), true);
		if (taxRateAreaNodes.size() > 0) {
			Node taxRateAreaNode = taxRateAreaNodes.elementAt(0).getParent();
			if (taxRateAreaNode != null) {
				parentNodeContent = taxRateAreaNode.toHtml();
			}
		}
		return parentNodeContent;
	}

	@SuppressWarnings("rawtypes")
	@Override
	protected Object parseAndFillResultMap(ServerResponse response, String detailsHtml, ResultMap resultMap) {
		resultMap.put(OtherInformationSetKey.SRC_TYPE.getKeyName(), getDataSite().getSiteTypeAbrev());

		try {
			detailsHtml = detailsHtml.replaceAll("<th\\b", "<td").replaceAll("(?is)</?strong[^>]*>", "");
			HtmlParser3 htmlParser3 = new HtmlParser3(detailsHtml);
			NodeList nodeList = htmlParser3.getNodeList();

			boolean isMainDoc = true;
			Node mainDetailsRow = htmlParser3.getNodeById("mainDetailsRow");
			if (mainDetailsRow == null) {
				isMainDoc = false;
			}

			Node parcelInfoNode = htmlParser3.getNodeById("parcelInfo");
			NodeList billDetailsNodes = nodeList.extractAllNodesThatMatch(new HasAttributeFilter("class", "billDetailsRows"), true);

			Node billDetailsNode = null;
			if (billDetailsNodes.size() > 0) {
				billDetailsNode = billDetailsNodes.elementAt(0);
			}

			String parcelID = "";
			String billNumber = "";
			String parcelInfo = "";
			String billDetails = "";
			if (parcelInfoNode != null) {
				parcelInfo = parcelInfoNode.toHtml();
			}

			if (billDetailsNode != null) {
				billDetails = billDetailsNode.toHtml();
			}

			// get bill number
			billNumber = StringUtils
					.extractParameter(billDetails, "(?is)Bill\\s+Number\\s*:\\s*([\\s\\d-]+)")
					.trim();

			if (!billNumber.isEmpty()) {
				resultMap.put(TaxHistorySetKey.TAX_BILL_NUMBER.getKeyName(), billNumber);
				if (!isMainDoc) {
					resultMap.put(PropertyIdentificationSetKey.PARCEL_ID.getKeyName(), billNumber);
				}
			}

			if (isMainDoc) {
				// get parcel ID
				parcelID = StringUtils
						.extractParameter(parcelInfo, "(?is)Parcel\\s+Number\\s*:\\s*([\\s\\d-]+)")
						.trim();
				if (!parcelID.isEmpty()) {
					resultMap.put(PropertyIdentificationSetKey.PARCEL_ID.getKeyName(), parcelID);
				}

			} else {// pid for extra docs is bill no

			}

			// get tax rate area
			String taxRateArea = StringUtils
					.extractParameter(parcelInfo, "(?is)Tax\\s+Rate\\s+Area\\s*:\\s*([\\s\\d-]+)")
					.trim();
			if (!taxRateArea.isEmpty()) {
				resultMap.put(PropertyIdentificationSetKey.AREA_CODE.getKeyName(), taxRateArea);
			}

			// from billDetailsRow:
			// get tax year
			String taxYear = StringUtils
					.extractParameter(billDetails, "(?is)Assessment\\s+Year\\s*:\\s*([\\s\\d-]+)")
					.trim();
			taxYear = StringUtils.extractParameter(taxYear, "^(\\d{4})");
			if (!taxYear.isEmpty()) {
				resultMap.put(TaxHistorySetKey.YEAR.getKeyName(), taxYear);
			}

			// get base amount
			String baseAmount = StringUtils
					.extractParameter(billDetails, "(?is)Original\\s+Bill\\s+Amount\\s*:\\s*([\\s\\d-.,\\$]+)")
					.trim().replaceAll("[^\\d.]", "");
			if (!baseAmount.isEmpty()) {
				resultMap.put(TaxHistorySetKey.BASE_AMOUNT.getKeyName(), baseAmount);
			}

			String amountDue = StringUtils
					.extractParameter(billDetails, "(?is)Total\\s+Bill\\s+Amount\\s+Due\\s*:\\s*([\\s\\d-.,\\$]+)")
					.trim().replaceAll("[^\\d.]", "");
			if (!amountDue.isEmpty()) {
				resultMap.put(TaxHistorySetKey.TOTAL_DUE.getKeyName(), amountDue);
			}

			// if current tax year > tax year, amount due is prior delinquent
			if (StringUtils.isNotEmpty(amountDue)) {
				int currentYearInt = getDataSite().getCurrentTaxYear();
				int taxYearInt = Integer.parseInt(taxYear);
				if (currentYearInt == taxYearInt) {
					resultMap.put(TaxHistorySetKey.TOTAL_DUE.getKeyName(), amountDue);
				} else if (currentYearInt > taxYearInt) {
					resultMap.put(TaxHistorySetKey.PRIOR_DELINQUENT.getKeyName(), amountDue);
				}
			}

			// get tax installment set: ba, ad, ap, date paid, penalty amount
			NodeList installmentTables = nodeList.extractAllNodesThatMatch(new HasAttributeFilter("class", "billDetailsTable"), true);
			if (installmentTables.size() > 0) {
				Node installmentTable = installmentTables.elementAt(0);
				if (installmentTable != null) {
					String[] installmentName = { "First Installment", "Second Installment" };
					String[] installmentStatus = { "", "" };
					String[] installmentBaseAmount = { "", "" };
					String[] installmentPenalty = { "", "" };
					String[] installmentBalanceDue = { "", "" };
					String[] installmentAmountPaid = { "", "" };

					String[] installmentDatePaid = { "", "" };

					TableRow[] installmentTableRow = ((TableTag) installmentTable).getRows();
					for (int i = 0; i < installmentTableRow.length; i++) {
						TableColumn[] installmentColumn = installmentTableRow[i].getColumns();

						if (installmentColumn.length > 2) {

							if (!StringUtils.extractParameter(installmentColumn[0].toPlainTextString(), "(?is)(Amount\\s*:)").isEmpty()) {
								installmentBaseAmount[0] = installmentColumn[1].toPlainTextString().replaceAll("[^\\d.]", "");
								installmentBaseAmount[1] = installmentColumn[2].toPlainTextString().replaceAll("[^\\d.]", "");
							} else if (!StringUtils.extractParameter(installmentColumn[0].toPlainTextString(), "(?is)(Penalty)").isEmpty()) {
								installmentPenalty[0] = installmentColumn[1].toPlainTextString().replaceAll("[^\\d.]", "");
								installmentPenalty[1] = installmentColumn[2].toPlainTextString().replaceAll("[^\\d.]", "");
							} else if (!StringUtils.extractParameter(installmentColumn[0].toPlainTextString(), "(?is)(Installment\\s+Amount\\s+Due)").isEmpty()) {
								installmentBalanceDue[0] = installmentColumn[1].toPlainTextString().replaceAll("[^\\d.]", "");
								installmentBalanceDue[1] = installmentColumn[2].toPlainTextString().replaceAll("[^\\d.]", "");
							} else if (!StringUtils.extractParameter(installmentColumn[0].toPlainTextString(), "(?is)(Status)").isEmpty()) {
								installmentStatus[0] = installmentColumn[1].toPlainTextString().trim();
								installmentStatus[1] = installmentColumn[2].toPlainTextString().trim();
								installmentDatePaid[0] = StringUtils.extractParameter(installmentStatus[0], "(?is)Paid\\s*(\\d+/\\d+/\\d+)");
								installmentDatePaid[1] = StringUtils.extractParameter(installmentStatus[1], "(?is)Paid\\s*(\\d+/\\d+/\\d+)");

								installmentStatus[0] = installmentStatus[0].replaceFirst("(?is)\\s*\\d+/\\d+/\\d+$", "").toUpperCase();
								installmentStatus[1] = installmentStatus[1].replaceFirst("(?is)\\s*\\d+/\\d+/\\d+$", "").toUpperCase();

							}
						}
					}

					// get tax installments
					Map<String, String[]> installmentsMap = new HashMap<String, String[]>();
					List<List> installmentsBody = new ArrayList<List>();
					ResultTable resultTable = new ResultTable();

					String[] installmentsHeader = {
							TaxInstallmentSetKey.INSTALLMENT_NAME.getShortKeyName(),
							TaxInstallmentSetKey.STATUS.getShortKeyName(),
							TaxInstallmentSetKey.BASE_AMOUNT.getShortKeyName(),
							TaxInstallmentSetKey.PENALTY_AMOUNT.getShortKeyName(),
							TaxInstallmentSetKey.TOTAL_DUE.getShortKeyName(),
							TaxInstallmentSetKey.AMOUNT_PAID.getShortKeyName()
					};

					for (String s : installmentsHeader) {
						installmentsMap.put(s, new String[] { s, "" });
					}

					for (int i = 0; i < 2; i++) {
						List<String> installmentsRow = new ArrayList<String>();

						// get installment name
						installmentsRow.add(installmentName[i]);

						// get installment status
						if (installmentStatus[i].equals("PAID") || installmentStatus[i].equals("UNPAID")) {
							installmentsRow.add(installmentStatus[i]);
						} else {
							installmentsRow.add("");
						}
						// get base amount per installment
						installmentsRow.add(installmentBaseAmount[i]);

						// get penalty per installment
						installmentsRow.add(installmentPenalty[i]);

						// get total due per installment

						installmentsRow.add(installmentBalanceDue[i]);

						// get amount paid per installment
						if (installmentStatus[i].equals("PAID")) {
							installmentAmountPaid[i] = GenericFunctions1.sum(installmentBaseAmount[i] + "+" + installmentPenalty[i] + "+-"
									+ installmentBalanceDue[i], searchId);
							installmentsRow.add(installmentAmountPaid[i]);
						} else {
							installmentsRow.add("");
						}

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

						if (installmentStatus[i].equals("PAID")) {
							receiptRow.add(installmentBalanceDue[i]);
							receiptRow.add(installmentDatePaid[i]);
						}

						if (receiptRow.size() == header.length) {
							bodyHist.add(receiptRow);
						}
					}

					if (!bodyHist.isEmpty()) {
						resultTable.setHead(header);
						resultTable.setBody(bodyHist);
						resultTable.setMap(map);
						resultTable.setReadOnly();
						resultMap.put("TaxHistorySet", resultTable);
					}

					String totalPaid = GenericFunctions1.sum(installmentAmountPaid[0] + "+" + installmentAmountPaid[1], searchId);
					if (!totalPaid.isEmpty()) {
						resultMap.put(TaxHistorySetKey.AMOUNT_PAID.getKeyName(), totalPaid);
					}
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}

		return null;
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
