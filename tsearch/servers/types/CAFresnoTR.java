package ro.cst.tsearch.servers.types;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang.StringEscapeUtils;

import ro.cst.tsearch.utils.StringUtils;

import org.htmlparser.Node;
import org.htmlparser.filters.HasAttributeFilter;
import org.htmlparser.tags.InputTag;
import org.htmlparser.tags.TableColumn;
import org.htmlparser.tags.TableRow;
import org.htmlparser.tags.TableTag;
import org.htmlparser.util.NodeList;

import com.stewart.ats.base.document.DocumentI;

import ro.cst.tsearch.bean.SearchAttributes;
import ro.cst.tsearch.exceptions.ServerResponseException;
import ro.cst.tsearch.extractor.xml.GenericFunctions;
import ro.cst.tsearch.extractor.xml.ResultMap;
import ro.cst.tsearch.extractor.xml.ResultTable;
import ro.cst.tsearch.parser.HtmlParser3;
import ro.cst.tsearch.search.filter.TaxYearFilterResponse;
import ro.cst.tsearch.servers.TSConnectionURL;
import ro.cst.tsearch.servers.info.TSServerInfo;
import ro.cst.tsearch.servers.info.TSServerInfoModule;
import ro.cst.tsearch.servers.response.LinkInPage;
import ro.cst.tsearch.servers.response.ParsedResponse;
import ro.cst.tsearch.servers.response.ServerResponse;
import ro.cst.tsearch.servers.response.OtherInformationSet.OtherInformationSetKey;
import ro.cst.tsearch.servers.response.PropertyAppraisalSet.PropertyAppraisalSetKey;
import ro.cst.tsearch.servers.response.PropertyIdentificationSet.PropertyIdentificationSetKey;
import ro.cst.tsearch.servers.response.TaxHistorySet.TaxHistorySetKey;
import ro.cst.tsearch.servers.response.TaxInstallmentSet.TaxInstallmentSetKey;

public class CAFresnoTR extends TSServer {

	private static final long	serialVersionUID	= 8927511955979069141L;

	public CAFresnoTR(String rsRequestSolverName, String rsSitePath, String rsServerID, String rsPrmNameLink, long searchId, int miServerID) {
		super(rsRequestSolverName, rsSitePath, rsServerID, rsPrmNameLink, searchId, miServerID);
	}

	public CAFresnoTR(long searchId) {
		super(searchId);
	}

	@Override
	public ServerResponse SearchBy(TSServerInfoModule module, Object sd) throws ServerResponseException {
		if (module.getModuleIdx() == TSServerInfo.PARCEL_ID_MODULE_IDX) {
			// expand pin
			String parcelNumber = module.getFunction(0).getParamValue().trim();
			String[] pins = ro.cst.tsearch.servers.functions.CAFresnoTR.extractPins(parcelNumber);
			if (pins == null) {
				return ServerResponse.createErrorResponse("Invalid PIN!");
			}
			for (int i = 1; i < pins.length; i++) {
				module.getFunction(i).setParamValue(pins[i]);
			}
		}

		try {
			return super.SearchBy(module, sd);
		} finally {
		}
	}

	@Override
	protected void ParseResponse(String sAction, ServerResponse Response,
			int viParseID) throws ServerResponseException {

		String rsResponse = StringEscapeUtils.unescapeHtml(Response.getResult());
		rsResponse = rsResponse.replaceAll("(?is)<script[^>]*>.*?</script>", "");
		ParsedResponse parsedResponse = Response.getParsedResponse();

		String message = StringUtils.extractParameter(rsResponse, "(?is)(The\\s+Property\\s+you\\s+have\\s+"
				+ "accessed\\s+can\\s+not\\s+be\\s+added\\s+to\\s+your\\s+Search\\s+Results\\s+at\\s+this"
				+ "\\s+time.\\s*Please\\s+wait\\s+30\\s+minutes\\s+then\\s+try\\s+again.)");
		if (!message.isEmpty()) {
			parsedResponse.setError(message);
			return;
		}

		message = StringUtils.extractParameter(rsResponse,
				"(?is)(There\\s+are\\s+no\\s+property\\s+taxes\\s+that\\s+match\\s+the\\s+Parcel\\s+Number\\s+you\\s+searched.)");
		if (!message.isEmpty()) {
			parsedResponse.setError(message);
			return;
		}

		switch (viParseID) {

		case ID_SEARCH_BY_PARCEL:
		case ID_DETAILS:
		case ID_SAVE_TO_TSD:

			String filename = "";
			String parcelId = "";

			String details = getDetails(Response, rsResponse, parsedResponse);

			Node parcelNoNode = null;

			if (details != null) {

				HtmlParser3 htmlParser3 = new HtmlParser3(details);
				NodeList nodeList = htmlParser3.getNodeList();
				parcelNoNode = htmlParser3.getNodeById("mainDetailsRow");
				boolean isMainDoc = true;

				if (parcelNoNode == null) {// for extra docs
					isMainDoc = false;
					NodeList parcelNodeList = nodeList.extractAllNodesThatMatch(new HasAttributeFilter("class", "extraDetailsRows"));
					if (parcelNodeList.size() > 0) {
						parcelNoNode = parcelNodeList.elementAt(0);
					}
				}
				if (parcelNoNode != null) {
					parcelId = HtmlParser3.getValueFromAbsoluteCell(1, 0, HtmlParser3.findNode(parcelNoNode.getChildren(), "PARCEL NUMBER"), "", true)
							.replaceAll("[^\\w-]", "");
					filename = parcelId + ".html";
				}

				if (viParseID != ID_SAVE_TO_TSD) {
					String originalLink = sAction.replace("?", "&") + "&" + Response.getRawQuerry();
					String sSave2TSDLink = getLinkPrefix(TSConnectionURL.idPOST) + originalLink;

					HashMap<String, String> data = new HashMap<String, String>();
					loadDataHash(data);

					if (isInstrumentSaved(parcelId, null, data)) {
						details += CreateFileAlreadyInTSD();
					}
					else {
						mSearch.addInMemoryDoc(sSave2TSDLink, details);
						details = addSaveToTsdButton(details, sSave2TSDLink, ID_DETAILS);
					}

					parsedResponse.setPageLink(new LinkInPage(sSave2TSDLink, originalLink, TSServer.REQUEST_SAVE_TO_TSD));
					parsedResponse.setResponse(details);
				} else {

					if (isMainDoc) {
						details = details.replaceAll("(<tr class=\"extraDetailsRows\")", "$1 hidden=\"true\"");
					} else {
						details = details.replaceAll("(<tr class=\"extraDetailsRows\") hidden=\"true\"", "$1");
					}

					filename = parcelId + ".html";
					smartParseDetails(Response, details);

					msSaveToTSDFileName = filename;
					parsedResponse.setFileName(getServerTypeDirectory() + msSaveToTSDFileName);
					parsedResponse.setResponse(details);

					msSaveToTSDResponce = details + CreateFileAlreadyInTSD();
				}
			}

			break;

		case ID_GET_LINK:
			ParseResponse(sAction, Response, ID_DETAILS);
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

	protected String getDetails(ServerResponse Response, String rsResponse, ParsedResponse parsedResponse) {
		try {

			/* If from memory - use it as is */
			if (!org.apache.commons.lang.StringUtils.containsIgnoreCase(rsResponse, "<html")) {
				return rsResponse;
			}

			StringBuilder detailsSB = new StringBuilder();
			HtmlParser3 htmlParser3 = new HtmlParser3(rsResponse);

			int noOfExtraDetailsLinks = 0;
			String[] extraDetailsLinks = new String[20];
			String detailsMainContent = "";
			String siteLink = getDataSite().getLink();
			siteLink = siteLink.substring(0, siteLink.lastIndexOf("/") + 1);
			Node intermediariesTable = htmlParser3.getNodeById("Table2");

			if (intermediariesTable != null) {
				TableTag tableTag = (TableTag) intermediariesTable;
				TableRow[] rows = tableTag.getRows();
				for (int i = 2; i < rows.length; i++) {
					TableRow rowPart1 = rows[i];
					if (rowPart1.getColumnCount() >= 8) {
						TableColumn detailsLinkColumn = rowPart1.getColumns()[0];

						if (detailsLinkColumn != null) {
							Node linkNode = HtmlParser3.getFirstTag(detailsLinkColumn.getChildren(), InputTag.class, true);

							if (linkNode != null) {
								InputTag inputTag = (InputTag) linkNode;
								String link = StringUtils.extractParameter(inputTag.getAttribute("onClick"),
										"(?is)window.location\\s*=\\s*'([^']*)'");

								if (!link.isEmpty() && detailsMainContent.isEmpty()) {
									detailsMainContent = getLinkContents(siteLink + link);

									String message = StringUtils.extractParameter(detailsMainContent,
											"(?is)(There\\s+is\\s+no\\s+detailed\\s+bill\\s+information\\s+available\\s+at\\s+this\\s+time\\s*.)");
									if (!message.isEmpty()) {
										parsedResponse.setError(message);
										return null;
									}

									htmlParser3 = new HtmlParser3(detailsMainContent);
									Node mainNode = null;
									mainNode = htmlParser3.getNodeById("Form1");

									if (mainNode != null) {
										detailsMainContent = mainNode.toHtml();
									}
								} else if (!link.isEmpty()) {
									extraDetailsLinks[noOfExtraDetailsLinks++] = siteLink + link;
								}
							}
						}
					}
				}
			}

			// add main doc table
			detailsSB.append("<tr id=\"mainDetailsRow\"><td>" + detailsMainContent + "</td></tr>");

			// add extra docs
			if (noOfExtraDetailsLinks > 0) {
				String[] extraDetailsContents = new String[noOfExtraDetailsLinks];

				for (int i = 0; i < noOfExtraDetailsLinks; i++) {
					extraDetailsContents[i] = getLinkContents(extraDetailsLinks[i]);
					// getSearch().setAdditionalInfo("extraDetailsContents" + i, extraDetailsContents[i]);
					htmlParser3 = new HtmlParser3(extraDetailsContents[i]);
					Node extraDetailsNode = htmlParser3.getNodeById("Form1");
					if (extraDetailsNode != null) {
						detailsSB.append("<tr class=\"extraDetailsRows\"><td>" + extraDetailsNode.toHtml() + "</td></tr>");
					}
				}

			}
			if (!detailsSB.toString().isEmpty()) {
				String allDetails = "<table id=\"allDetails\" border=\"1\" align=\"center\" style=\"min-width:300px;\">"
						+ detailsSB.toString()
								.replaceAll("(?is)<a[^>]*>.*?</a>", "")
								.replaceAll("(?is)<input[^>]*>", "")
								.replaceAll("(?is)<table[^>]*\\bid=\"Table1\"[^>]*>.*?</table>", "")
								.replaceAll("(?is)(<td[^>]*taxbilltitle[^>]*>)(\\s*Fresno\\s+County\\s+.*?)(</td>)", "$1<h3>$2</h3>$3")
								.replaceAll("<font[^>]*class=\"whitetext\"[^>]*>\\s*\\.\\s*</font>", "")
						+ "</table>";
				return allDetails;
			}
		} catch (Exception e) {
			logger.error("Error while getting details", e);
		}
		return null;
	}

	@Override
	protected Object parseAndFillResultMap(ServerResponse response, String detailsHtml, ResultMap resultMap) {
		try {
			resultMap.put(OtherInformationSetKey.SRC_TYPE.getKeyName(), getDataSite().getSiteTypeAbrev());
			HtmlParser3 htmlParser3 = new HtmlParser3(detailsHtml);
			NodeList nodeList = htmlParser3.getNodeList();
			Node detailsNode = htmlParser3.getNodeById("mainDetailsRow");
			NodeList detailsNodeChildren = null;
			boolean isMainDoc = true;

			if (detailsNode == null) {
				isMainDoc = false;
				NodeList detailsNodeList = nodeList.extractAllNodesThatMatch(new HasAttributeFilter("class", "extraDetailsRows"));
				if (detailsNodeList.size() > 0) {
					detailsNode = detailsNodeList.elementAt(0);
				}
				// detailsNode = htmlParser3.getNodeById("extraDetailsRows");
			}
			if (detailsNode != null) {
				detailsNodeChildren = detailsNode.getChildren();
			}

			// get parcel number
			String parcelNumber = HtmlParser3.getValueFromAbsoluteCell(1, 0,
					HtmlParser3.findNode(detailsNodeChildren, "PARCEL NUMBER"), "", true);
			if (parcelNumber.contains("<")) {
				parcelNumber = parcelNumber.substring(0, parcelNumber.indexOf("<"));
			}
			parcelNumber = parcelNumber.replaceAll("[^\\w-]", "");
			parcelNumber = parcelNumber.replaceFirst("(?i)(\\d)S$", "$1");// remove 'S'(if present) at the end of secured(main) documents(e.g. '568-262-17S')
			if (!parcelNumber.isEmpty()) {
				resultMap.put(PropertyIdentificationSetKey.PARCEL_ID.getKeyName(), parcelNumber);
			}

			if (isMainDoc) {// get year from main doc and put it on search for the other docs
				String year = StringUtils.extractParameter(detailsNode.toHtml(), "(?s)<br[^>]*>\\s*FISCAL\\s+YEAR\\s*\\b(\\d+)\\b");
				if (!year.isEmpty()) {
					resultMap.put(TaxHistorySetKey.YEAR.getKeyName(), year);
					mSearch.setAdditionalInfo("fiscalYear_" + parcelNumber.replaceAll("[^\\d]+", ""), year);
				}
			} else {// parse year on supplemental/delq documents
				String year = (String) mSearch.getAdditionalInfo("fiscalYear_" + parcelNumber.replaceAll("[^\\d]+", ""));
				if (year != null) {
					resultMap.put(TaxHistorySetKey.YEAR.getKeyName(), year);
				}
			}

			// get land
			String land = HtmlParser3.getValueFromAbsoluteCell(1, 0,
					HtmlParser3.findNode(detailsNodeChildren, "LAND"), "", true);
			if (land.contains("<")) {
				land = land.substring(0, land.indexOf("<"));
			}
			land = land.replaceAll("[^\\d\\.]", "");
			if (!land.isEmpty()) {
				resultMap.put(PropertyAppraisalSetKey.LAND_APPRAISAL.getKeyName(), land);
			}

			// get improvements
			String improvements = HtmlParser3.getValueFromAbsoluteCell(1, 0,
					HtmlParser3.findNode(detailsNodeChildren, "IMPROVEMENT"), "", true);
			if (improvements.contains("<")) {
				improvements = improvements.substring(0, improvements.indexOf("<"));
			}
			improvements = improvements.replaceAll("[^\\d.]", "");
			if (!improvements.isEmpty()) {
				resultMap.put(PropertyAppraisalSetKey.IMPROVEMENT_APPRAISAL.getKeyName(), improvements);
			}

			// get full address, street no, street name, city
			String location = HtmlParser3.getValueFromAbsoluteCell(1, 0,
					HtmlParser3.findNode(detailsNodeChildren, "LOCATION"), "", true);
			if (location.contains("<")) {
				location = location.substring(0, location.indexOf("<"));
			}

			location = location.trim();

			if (!location.isEmpty()) {

				resultMap.put(PropertyIdentificationSetKey.ADDRESS_ON_SERVER.getKeyName(), location);

				String streetNumber = StringUtils.extractParameter(location, "(?s)\\s*(\\d+)\\b\\s*.*");
				String streetName = StringUtils.extractParameter(location, "(?s)\\s*\\d+\\b\\s+(.+?)(?:,|$)").trim();
				String city = StringUtils.extractParameter(location, "(?s).*?,(.+)").trim();

				if (!streetNumber.isEmpty()) {
					resultMap.put(PropertyIdentificationSetKey.STREET_NO.getKeyName(), streetNumber);
				}
				if (!streetName.isEmpty()) {
					resultMap.put(PropertyIdentificationSetKey.STREET_NAME.getKeyName(), streetName);
				}
				if (!city.isEmpty()) {
					resultMap.put(PropertyIdentificationSetKey.CITY.getKeyName(), city);
				}
			}

			// get base amount
			String baseAmount = StringUtils.extractParameter(detailsNode.toHtml(),
					"(?is)<tr>\\s*<td[^>]*>\\s*TOTAL\\s+TAX.*?</td>\\s*<td[^>]*>(.*?)</td>\\s*</tr>");
			if (!baseAmount.isEmpty()) {
				baseAmount = baseAmount.replaceAll("[^\\d.]", "");
				if (!baseAmount.isEmpty()) {
					resultMap.put(TaxHistorySetKey.BASE_AMOUNT.getKeyName(), baseAmount);
				}
			}

			// get taxInstallmentSet
			Node installmentsTable = HtmlParser3.getNodeByTypeAttributeDescription(detailsNodeChildren, "table", "border", "1", new String[] {
					"1st Installment", "2nd Installment" }, true);

			String[] installmentStatus = new String[2];
			String[] installmentBaseAmount = new String[2];
			String[] installmentPenaltyAmount = new String[2];
			String[] installmentAmountDue = new String[2];
			String[] installmentDatePaid = new String[2];
			String totalPaid = "";
			String totalDue = "";

			if (installmentsTable instanceof TableTag) {
				TableRow[] rows = ((TableTag) installmentsTable).getRows();
				if (rows.length > 0) {

					Map<String, String[]> installmentsMap = new HashMap<String, String[]>();
					List<List<String>> installmentsBody = new ArrayList<List<String>>();
					ResultTable resultTable = new ResultTable();
					String[] installmentsHeader = {
							TaxInstallmentSetKey.STATUS.getShortKeyName(),
							TaxInstallmentSetKey.BASE_AMOUNT.getShortKeyName(),
							TaxInstallmentSetKey.PENALTY_AMOUNT.getShortKeyName(),
							TaxInstallmentSetKey.TOTAL_DUE.getShortKeyName(),
							TaxInstallmentSetKey.AMOUNT_PAID.getShortKeyName(),
					};

					for (String s : installmentsHeader) {
						installmentsMap.put(s, new String[] { s, "" });
					}

					for (int i = 0; i < rows.length; i++) {
						if (rows[i].getColumnCount() >= 4) {
							if (!StringUtils.extractParameter(rows[i].getColumns()[0].toPlainTextString(), "(Status)").isEmpty()) {
								installmentStatus[0] = rows[i].getColumns()[1].toHtml();
								installmentStatus[1] = rows[i].getColumns()[3].toHtml();

								installmentDatePaid[0] = StringUtils.extractParameter(installmentStatus[0], "(?is).*?<br[^>]*>(.*?)<").trim();
								installmentDatePaid[1] = StringUtils.extractParameter(installmentStatus[1], "(?is).*?<br[^>]*>(.*?)<").trim();

								installmentStatus[0] = StringUtils.extractParameter(installmentStatus[0], "(?is)<td[^>]*>(.*?)<").trim().toUpperCase();
								installmentStatus[1] = StringUtils.extractParameter(installmentStatus[1], "(?is)<td[^>]*>(.*?)<").trim().toUpperCase();
							}
							if (!StringUtils.extractParameter(rows[i].getColumns()[0].toPlainTextString(), "(Taxes\\s+Due)").isEmpty()) {
								installmentBaseAmount[0] = rows[i].getColumns()[1].toPlainTextString().replaceAll("[^\\d.]", "");
								installmentBaseAmount[1] = rows[i].getColumns()[3].toPlainTextString().replaceAll("[^\\d.]", "");
							}
							if (!StringUtils.extractParameter(rows[i].getColumns()[0].toPlainTextString(), "(Penalties\\s+Due)").isEmpty()) {
								installmentPenaltyAmount[0] = rows[i].getColumns()[1].toPlainTextString().replaceAll("[^\\d.]", "");
								installmentPenaltyAmount[1] = rows[i].getColumns()[3].toPlainTextString().replaceAll("[^\\d.]", "");
							}
							if (!StringUtils.extractParameter(rows[i].getColumns()[0].toPlainTextString(), "(Total\\s+Amount\\s+Due)").isEmpty()) {
								installmentAmountDue[0] = rows[i].getColumns()[1].toPlainTextString().replaceAll("[^\\d.]", "");
								installmentAmountDue[1] = rows[i].getColumns()[3].toPlainTextString().replaceAll("[^\\d.]", "");
							}
						}
					}

					for (int j = 0; j < 2; j++) {
						List<String> installmentsRow = new ArrayList<String>();
						if (!installmentStatus[j].isEmpty()) {
							installmentsRow.add(installmentStatus[j]);
						} else {
							installmentsRow.add("");
						}

						if (!installmentBaseAmount[j].isEmpty()) {
							installmentsRow.add(installmentBaseAmount[j]);
						} else {
							installmentsRow.add("");
						}

						if (!installmentPenaltyAmount[j].isEmpty()) {
							installmentsRow.add(installmentPenaltyAmount[j]);
						} else {
							installmentsRow.add("");
						}

						if (!installmentAmountDue[j].isEmpty()) {
							installmentsRow.add(installmentAmountDue[j]);
						} else {
							installmentsRow.add("");
						}

						if (installmentStatus[j].equals("PAID") && !installmentAmountDue[j].isEmpty()) {// amount paid is amount due - if status is 'PAID'
							installmentsRow.add(installmentAmountDue[j]);
							totalPaid += installmentAmountDue[j] + "+";
						} else {
							if (!installmentStatus[j].equals("PAID") && !installmentAmountDue[j].isEmpty()) {
								totalDue = installmentAmountDue[j] + "+";
							}
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
				}
			}

			// get total amount paid
			if (!totalPaid.isEmpty()) {
				totalPaid = totalPaid.substring(0, totalPaid.length() - 1);
				totalPaid = GenericFunctions.sum(totalPaid, searchId);
				resultMap.put(TaxHistorySetKey.AMOUNT_PAID.getKeyName(), totalPaid);
			}

			// get total amount due
			if (!totalDue.isEmpty()) {
				totalDue = totalDue.substring(0, totalDue.length() - 1);
				totalDue = GenericFunctions.sum(totalDue, searchId);
				resultMap.put(TaxHistorySetKey.TOTAL_DUE.getKeyName(), totalDue);
			}

			// get receipts values and dates
			List<List<String>> bodyHist = new ArrayList<List<String>>();
			ResultTable resultTable = new ResultTable();
			String[] header = { "ReceiptDate", "ReceiptAmount" };
			Map<String, String[]> map = new HashMap<String, String[]>();
			map.put("ReceiptAmount", new String[] { "ReceiptAmount", "" });
			map.put("ReceiptDate", new String[] { "ReceiptDate", "" });

			for (int i = 0; i < 2; i++) {
				List<String> receiptRow = new ArrayList<String>();
				if (!installmentDatePaid[i].isEmpty()) {

					receiptRow.add(installmentDatePaid[i]);

					if (!installmentAmountDue[i].isEmpty()) {
						receiptRow.add(installmentAmountDue[i]);
					} else {
						receiptRow.add("");
					}
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

			// get delinquent amount - no examples found yet

		} catch (Exception e) {
			logger.error("Error while getting details", e);
		}
		return null;
	}

	@Override
	protected void setModulesForAutoSearch(TSServerInfo serverInfo) {
		List<TSServerInfoModule> moduleList = new ArrayList<TSServerInfoModule>();

		TSServerInfoModule module = null;

		TaxYearFilterResponse taxYearFilter = new TaxYearFilterResponse(searchId);
		if (hasPin()) {
			module = new TSServerInfoModule(serverInfo.getModule(TSServerInfo.PARCEL_ID_MODULE_IDX));
			module.clearSaKeys();
			module.setSaKey(0, SearchAttributes.LD_PARCELNO);
			module.addFilter(taxYearFilter);
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
