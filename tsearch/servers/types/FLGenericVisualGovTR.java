package ro.cst.tsearch.servers.types;

import static org.apache.commons.lang.StringUtils.getNestedString;
import static ro.cst.tsearch.utils.StringUtils.isEmpty;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Vector;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang.StringEscapeUtils;
import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;
import org.htmlparser.Node;
import org.htmlparser.filters.HasAttributeFilter;
import org.htmlparser.filters.TagNameFilter;
import org.htmlparser.tags.LinkTag;
import org.htmlparser.tags.OptionTag;
import org.htmlparser.tags.SelectTag;
import org.htmlparser.tags.Span;
import org.htmlparser.tags.TableColumn;
import org.htmlparser.tags.TableRow;
import org.htmlparser.tags.TableTag;
import org.htmlparser.util.NodeList;

import ro.cst.tsearch.bean.SearchAttributes;
import ro.cst.tsearch.data.CountyConstants;
import ro.cst.tsearch.exceptions.ServerResponseException;
import ro.cst.tsearch.extractor.xml.Bridge;
import ro.cst.tsearch.extractor.xml.GenericFunctions;
import ro.cst.tsearch.extractor.xml.GenericFunctions1;
import ro.cst.tsearch.extractor.xml.ResultMap;
import ro.cst.tsearch.extractor.xml.ResultTable;
import ro.cst.tsearch.extractor.xml.StringFormats;
import ro.cst.tsearch.parser.HtmlParser3;
import ro.cst.tsearch.search.FunctionStatesIterator;
import ro.cst.tsearch.search.filter.FilterResponse;
import ro.cst.tsearch.search.filter.TaxYearFilterResponse;
import ro.cst.tsearch.search.filter.newfilters.address.AddressFilterFactory;
import ro.cst.tsearch.search.filter.newfilters.name.NameFilterFactory;
import ro.cst.tsearch.search.iterator.ModuleStatesIteratorFactory;
import ro.cst.tsearch.search.module.ConfigurableNameIterator;
import ro.cst.tsearch.search.name.NameUtils;
import ro.cst.tsearch.servers.HashCountyToIndex;
import ro.cst.tsearch.servers.TSConnectionURL;
import ro.cst.tsearch.servers.bean.DataSite;
import ro.cst.tsearch.servers.info.TSServerInfo;
import ro.cst.tsearch.servers.info.TSServerInfoModule;
import ro.cst.tsearch.servers.info.spitHTML.HTMLControl;
import ro.cst.tsearch.servers.info.spitHTML.PageZone;
import ro.cst.tsearch.servers.response.LinkInPage;
import ro.cst.tsearch.servers.response.OtherInformationSet.OtherInformationSetKey;
import ro.cst.tsearch.servers.response.ParsedResponse;
import ro.cst.tsearch.servers.response.Parser;
import ro.cst.tsearch.servers.response.PropertyAppraisalSet.PropertyAppraisalSetKey;
import ro.cst.tsearch.servers.response.ServerResponse;
import ro.cst.tsearch.servers.response.PropertyIdentificationSet.PropertyIdentificationSetKey;
import ro.cst.tsearch.servers.response.TaxHistorySet.TaxHistorySetKey;
import ro.cst.tsearch.servers.response.TaxInstallmentSet.TaxInstallmentSetKey;
import ro.cst.tsearch.utils.RegExUtils;
import ro.cst.tsearch.utils.SearchLogger;
import ro.cst.tsearch.utils.StringUtils;

import com.stewart.ats.base.document.DocumentI;
import com.stewart.ats.base.document.TaxDocumentI;

/**
 * for Bay, DeSoto, Franklin, Gulf, Hardee, Jackson, Okeechobee, Wakulla, Calhoun - like sites ADD here the new county implemented with this Generic
 */
public class FLGenericVisualGovTR extends TSServer {

	protected static final long				serialVersionUID	= -8566121136648703791L;
	private boolean							downloadingForSave	= false;
	private static final Pattern			spanYearPattern		= Pattern.compile
																		("(?is)(\\w)\\s+(\\d+)\\s+(\\d+)");
	private static final Pattern			spanKeyPattern		= Pattern.compile
																		("(?is)(\\w)\\s+([-\\w]+)");
	private static HashMap<String, Integer>	taxYears			= new HashMap<String, Integer>();
	private static String					destinationPage		= "/Property/SearchSelect?ClearData=True&Accept=true";
	private static String					intermediaryLink	= "";
	private static String					amountPattern		= "[^\\d.\\(\\)]+";
	protected static String					datePattern			= "\\d+/\\d+/\\d+";

	public FLGenericVisualGovTR(long searchId) {
		super(searchId);
	}

	public FLGenericVisualGovTR(String rsRequestSolverName, String rsSitePath, String rsServerID, String rsPrmNameLink, long searchId, int miServerID) {
		super(rsRequestSolverName, rsSitePath, rsServerID, rsPrmNameLink, searchId, miServerID);
	}

	@Override
	protected void setModulesForAutoSearch(TSServerInfo serverInfo) {
		List<TSServerInfoModule> modules = new ArrayList<TSServerInfoModule>();

		FilterResponse taxYearFilter = new TaxYearFilterResponse(searchId);
		FilterResponse addressFilter = AddressFilterFactory.getAddressHybridFilter(searchId, 0.80d);

		// search by PIN
		String pin = getSearchAttribute(SearchAttributes.LD_PARCELNO_GENERIC_TR);

		if (!isEmpty(pin)) {
			TSServerInfoModule module = new TSServerInfoModule(serverInfo.getModule(TSServerInfo.ADV_SEARCH_MODULE_IDX));
			module.clearSaKeys();
			module.getFunction(3).forceValue(pin);
			module.addFilter(taxYearFilter);
			modules.add(module);
		}

		// search by Address
		String strNo = getSearchAttribute(SearchAttributes.P_STREETNO);
		String strName = getSearchAttribute(SearchAttributes.P_STREETNAME);
		boolean hasAddress = !isEmpty(strNo) && !isEmpty(strName);
		if (hasAddress) {
			TSServerInfoModule module = new TSServerInfoModule(serverInfo.getModule(TSServerInfo.ADV_SEARCH_MODULE_IDX));
			module.clearSaKeys();
			module.getFunction(1).forceValue(strNo);
			module.getFunction(2).forceValue(strName);
			module.addFilter(addressFilter);
			module.addFilter(taxYearFilter);
			modules.add(module);
		}

		// search by name - filter by address
		String name = getSearchAttribute(SearchAttributes.OWNER_LCFM_NAME);
		if (hasOwner() && hasAddress) {
			TSServerInfoModule module = new TSServerInfoModule(serverInfo.getModule(TSServerInfo.ADV_SEARCH_MODULE_IDX));
			module.clearSaKeys();
			module.setSaObjKey(SearchAttributes.OWNER_OBJECT);
			module.addFilter(NameFilterFactory.getDefaultNameFilterNoSinonims(SearchAttributes.OWNER_OBJECT, searchId, module));
			module.addFilter(addressFilter);
			module.addFilter(taxYearFilter);

			module.setIteratorType(0, FunctionStatesIterator.ITERATOR_TYPE_LF_NAME_FAKE);
			
			String derivPattern[] = { "L F;;", "L F M;;" };
			int countyId = dataSite.getCountyId();
			if (countyId == CountyConstants.FL_Bay) {
				derivPattern[0] = "L, F;;";
				derivPattern[1] = "L, F M;;";
			}
			ConfigurableNameIterator nameIterator = (ConfigurableNameIterator) ModuleStatesIteratorFactory
					.getConfigurableNameIterator(module, searchId, new String[] { derivPattern[0], derivPattern[1] });
			module.addIterator(nameIterator);

			modules.add(module);
		} else if (!isEmpty(name) && !hasAddress) {
			String warning = " - Will not search by name because there is no address to filter with.";
			SearchLogger.info(getLoggerErrorMessage(warning), searchId);
		}

		serverInfo.setModulesForAutoSearch(modules);
	}

	protected String getIntermediateResults(String response) {
		JSONObject jsonObject;
		JSONObject jsonResult;
		JSONObject jsonResultList;
		JSONObject jsonResultsInfo;
		String linkStart = CreatePartialLink(TSConnectionURL.idPOST) + dataSite.getLink() + "/Property/TaxBill";

		StringBuilder intermediaries = new StringBuilder();
		try {
			jsonObject = new JSONObject(response);
			jsonResult = new JSONObject(jsonObject.get("result").toString());
			jsonResultList = new JSONObject(jsonResult.get("FLTax").toString());
			jsonResultsInfo = jsonResultList.getJSONObject("ResultsInfo");

			int totalRowCount = Integer.parseInt(jsonResultsInfo.get("TotalRowCount").toString());
			int pageRowCount = Integer.parseInt(jsonResultsInfo.get("Rows").toString());
			int currentPage = (int) Math.ceil(((double) Integer.parseInt(jsonResultsInfo.get("Skip").toString()) + 1) / pageRowCount);
			int noOfPages = (int) Math.ceil((double) totalRowCount / pageRowCount);
			if (totalRowCount == 0) {
				return "";
			}

			Object intermediariesObj = null;
			JSONArray jsonIntermediaries = null;
			boolean multipleRows = false;
			if (jsonResultList.has("ResultsList")) {
				intermediariesObj = jsonResultList.get("ResultsList");

				if (intermediariesObj instanceof JSONArray) {
					multipleRows = true;
					jsonIntermediaries = (JSONArray) intermediariesObj;
				}

				intermediaries.append("Page " + currentPage + " of " + noOfPages + ". Total " + totalRowCount + " records found.");
				intermediaries.append("<table>");

				if (multipleRows == false) {
					jsonRowToHtml(linkStart, intermediaries, (JSONObject) intermediariesObj);
				}
				else {
					for (int i = 0; i < jsonIntermediaries.length(); i++) {
						JSONObject jsonRow = jsonIntermediaries.getJSONObject(i);
						jsonRowToHtml(linkStart, intermediaries, jsonRow);
					}
				}

				intermediaries.append("</table>");
			}
		} catch (JSONException e) {
			e.printStackTrace();
		}
		return intermediaries.toString();
	}

	private void jsonRowToHtml(String linkStart, StringBuilder intermediaries, JSONObject jsonRow) throws JSONException {
		intermediaries.append("<tr>");
		intermediaries.append("<td><a href=\"" + linkStart + "?taxbillno=" + jsonRow.get("TAXBILLNO") + "&rolltype=" + jsonRow.get("ROLLTYPE")
				+ "&taxyear=" + jsonRow.get("TAXYEAR") + "&pq=" + jsonRow.get("PROTESTEDQ") + "&st=" + jsonRow.get("STATUS")
				+ "\">" + jsonRow.get("PROPERTYNO") + " - " + jsonRow.get("TAXYEAR") + "</td>");
		intermediaries.append("<td>" + jsonRow.get("NAME") + "</td>");
		intermediaries.append("<td>" + jsonRow.get("ADDR1") + "</td>");
		intermediaries.append("<td>" + jsonRow.get("TAXBILLNO") + "</td>");
		intermediaries.append("</tr>");
	}

	protected String getDetails(String response) {

		// if from memory - use it as is
		if (!response.contains("<html")) {
			return response;
		}
		StringBuilder details = new StringBuilder();
		try {
			HtmlParser3 htmlParser3 = new HtmlParser3(response);
			NodeList nodeList = htmlParser3.getNodeList();
			if (nodeList.size() > 0) {
				Node mainDivNode = htmlParser3.getNodeById("ctl00_ContentPlaceHolder1_UpdatePanel1");
				if (mainDivNode == null) {
					mainDivNode = htmlParser3.getNodeById("custombody");
				}

				if (mainDivNode != null) {
					String mainDiv = mainDivNode.toHtml().replaceFirst("(?is)^\\s*<td[^>]*>\\s*(.*)\\s*</td>\\s*$", "$1");
					details.append("<table border=\"1\" align=\"center\"><tr id=\"details\"><td>" + mainDiv + "</td></tr></table>");
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		return details.toString().replaceAll("<a[^>]*>(.*?)</a>", "$1")
				.replaceAll("(?is)<input[^>]*>", "")
				.replaceAll("(?is)<script[^>]*>.*?</script>", "")
				.replaceAll("(?is)(<[^>]*\\b)color=\"[^>]*\"([^>]*>)", "$1$2")
				.replaceAll("(?is)(<th\\b[^>]*)color:\\s*white\\s*;([^>]*>)", "$1$2")
				.replaceAll("(?is)<td\\b[^>]*>\\s*</td>\\s*(</tr>)", "$1")
				.replaceFirst("(?is)<th\\b[^>]*>\\s*Add\\s+To\\s+Cart\\s*</th>", "")
				.replaceAll("(?is)</?a[^>]*>", "")
				.replaceFirst("(?is)<tr style\\s*=\\s*\\\"color\\s*:\\s*white\\s*;[^>]+>", "<tr>");
	}

	@Override
	protected void ParseResponse(String action, ServerResponse Response, int viParseID) throws ServerResponseException {

		String response = Response.getResult();
		ParsedResponse parsedResponse = Response.getParsedResponse();
		if (response.contains("The service was not able to retrieve information")) {
			String errorMessage = "The service was not able to retrieve information. Please try again later.";
			Response.getParsedResponse().setError(errorMessage);
			SearchLogger.info(getLoggerErrorMessage(" - " + errorMessage), searchId);
			return;
		}
		if (response.contains("<title>Runtime Error</title>")) {
			String errorMessage = "Server Error.";
			Response.getParsedResponse().setError(errorMessage);
			SearchLogger.info(getLoggerErrorMessage(" - " + errorMessage), searchId);
			return;
		}
		switch (viParseID) {

		case ID_SEARCH_BY_NAME:
		case ID_INTERMEDIARY:
			// save original intermediary post request params for paging
			if (viParseID == ID_SEARCH_BY_NAME) {
				intermediaryLink = Response.getQuerry();
			}
			String interm = getIntermediateResults(response);
			if (isEmpty(interm)) {
				return;
			}
			StringBuilder outputTable = new StringBuilder();
			Collection<ParsedResponse> smartParsedResponses = smartParseIntermediary(Response, interm, outputTable);

			if (smartParsedResponses.size() > 0) {
				parsedResponse.setResultRows(new Vector<ParsedResponse>(smartParsedResponses));
				parsedResponse.setOnlyResponse(outputTable.toString());
				parsedResponse.setAttribute(ParsedResponse.SERVER_ROW_RESPONSE, outputTable.toString());
			}
			break;

		case ID_DETAILS:
			String details = getDetails(response);

			NodeList allNodes = getAllNodesFromHtml(response);
			String year = getYear(allNodes);
			// get key
			String keyNumber = getKeyNumber(allNodes);
			
			if (!downloadingForSave) {
				String originalLink = action + "&" + Response.getRawQuerry()+"&dummy=" + keyNumber;
				String sSave2TSDLink = getLinkPrefix(TSConnectionURL.idPOST) + originalLink;

				HashMap<String, String> data = new HashMap<String, String>();
				loadDataHash(data);
				data.put("year", year);
				if (isInstrumentSaved(keyNumber, null, data)) {
					details += CreateFileAlreadyInTSD();
				}
				else {
					mSearch.addInMemoryDoc(sSave2TSDLink, details);
					details = addSaveToTsdButton(details, sSave2TSDLink, ID_DETAILS);
				}
				Response.getParsedResponse().setPageLink(new LinkInPage(sSave2TSDLink, originalLink, TSServer.REQUEST_SAVE_TO_TSD));
				Response.getParsedResponse().setResponse(details);

				if (isEmpty(keyNumber)) {
					Response.getParsedResponse().setError("Account Number Not Found!");
					logger.error("ParseResponse END: Account Number NOT Found!");
					return;
				}

			} else {
				msSaveToTSDFileName = keyNumber + ".html";
				Response.getParsedResponse().setFileName(getServerTypeDirectory() + msSaveToTSDFileName);
				msSaveToTSDResponce = details + CreateFileAlreadyInTSD();
				smartParseDetails(Response, details);
				Response.getParsedResponse().setResponse(details);
				msSaveToTSDResponce = details + CreateFileAlreadyInTSD();
			}
			break;

		case ID_GET_LINK:
			if (response.matches("(?s)\\s*\\{.*\\}")) {
				ParseResponse(action, Response, ID_INTERMEDIARY);

			} else {
				ParseResponse(action, Response, ID_DETAILS);
			}
			break;

		case ID_SAVE_TO_TSD:
			downloadingForSave = true;
			ParseResponse(action, Response, ID_DETAILS);
			downloadingForSave = false;
			break;
		}

	}

	private String getLoggerErrorMessage(String errorMessage) {
		return "</div><span class=servername>"
				+ this.mSearch.getCrtServerName(false)
				+ "</span><font color=\"red\">"+errorMessage+"</font><BR>";
	}

	private String getYear(NodeList allNodes) {
		if (allNodes == null) {
			return null;
		}
		NodeList yearSpanList = allNodes.extractAllNodesThatMatch(new TagNameFilter("span"), true)
				.extractAllNodesThatMatch(new HasAttributeFilter("id", "ctl00_ContentPlaceHolder1_lblTaxBillNumber"));
		if (yearSpanList.size() > 0) {
			Span span = (Span) yearSpanList.elementAt(0);
			String spanText = span.toPlainTextString();
			Matcher matcher = spanYearPattern.matcher(spanText);
			if (matcher.find()) {
				return matcher.group(3).trim();
			}
		}
		return null;
	}

	private String getKeyNumber(NodeList allNodes) {
		if (allNodes == null) {
			return null;
		}
		NodeList keySpanList = allNodes.extractAllNodesThatMatch(new TagNameFilter("span"), true)
				.extractAllNodesThatMatch(new HasAttributeFilter("id", "ctl00_ContentPlaceHolder1_lblPropertyInfo"));
		if (keySpanList.size() > 0) {
			Span span = (Span) keySpanList.elementAt(0);
			String spanText = span.toPlainTextString();
			Matcher matcher = spanKeyPattern.matcher(spanText);
			if (matcher.find()) {
				return matcher.group(2).trim();
			}
		}
		return null;

	}

	@SuppressWarnings("rawtypes")
	@Override
	protected Object parseAndFillResultMap(ServerResponse response, String detailsHtml, ResultMap resultMap) {
		try {
			resultMap.put(OtherInformationSetKey.SRC_TYPE.getKeyName(), getDataSite().getSiteTypeAbrev());

			detailsHtml = StringEscapeUtils.unescapeHtml(detailsHtml);
			HtmlParser3 htmlParser3 = new HtmlParser3(detailsHtml);

			NodeList nodeList = htmlParser3.getNodeList();
			Node parcelIdNode = htmlParser3.getNodeById("ctl00_ContentPlaceHolder1_lblPropertyInfo");
			if (parcelIdNode != null) {
				String parcelId = parcelIdNode.toPlainTextString().replaceFirst("(?is)^\\s*[A-Z]+\\s*", "")
						.trim();
				if (!parcelId.isEmpty()) {
					resultMap.put(PropertyIdentificationSetKey.PARCEL_ID.getKeyName(), parcelId);
				}
			}

			Node ownerNode = htmlParser3.getNodeById("ctl00_ContentPlaceHolder1_lblName");
			if (ownerNode != null) {
				String owner = ownerNode.toPlainTextString().trim();
				
				Node possibleOwnerName = htmlParser3.getNodeById("ctl00_ContentPlaceHolder1_lblAddressLine");
				if (possibleOwnerName != null) {
					String tmpOwner = possibleOwnerName.toHtml().trim();
					tmpOwner = tmpOwner.replaceFirst("(?is)<span[^>]*>(.*)</span>", "$1");
					if (StringUtils.isNotEmpty(tmpOwner)) {
						String[] lines = tmpOwner.split("<br>");
						if (lines.length > 0) {
							for (int i=0; i<lines.length; i++) {
								if (lines[i].matches("(?is)\\s*(C\\s*/\\s*O\\s*)?\\d+[\\w\\s]+\\b(DRIVE|DR|RDG|RIDGE|AVENUE|AVE|LANE|LN|CT|COURT|CIRCLE|CIR|RD|ROADE|HGWY|HIGHWAY)\\b.*"))
									break;
								else if (lines[i].matches("(?is)\\s*\\d+[\\w\\s]+.*")) 
									break;
								else if (lines[i].matches("(?is)\\s*\\bP\\.?\\s*O\\.?\\s*B(?:\\.|OX)?\\s*\\d+.*"))
									break;
								else if (lines[i].matches("(?is).*\\bFL\\b\\s*[\\d-\\s]+"))
									break;
								else {
									String partOfOwnerName = lines[i].trim();
									if (partOfOwnerName.length() == 1 || owner.matches(".+(&|,)$")) {
										owner += " ";
									} else {
										owner += " & ";
									}

									owner += partOfOwnerName;

									if (partOfOwnerName.matches("(?is)\\w+\\s+\\w+")) {
										owner += " REMOVEME";
									}
								}
							}
						}
					}
				}
				if (!owner.isEmpty()) {
					owner = owner.replaceAll("(?is)\\s+&\\s+(\\bREV(?:OCABLE)?\\b\\s+TR(?:UST)?)\\s+REMOVEME", " $1");
					resultMap.put(PropertyIdentificationSetKey.NAME_ON_SERVER.getKeyName(), owner);
				}
			}
			
			Node addressNode = htmlParser3.getNodeById("ctl00_ContentPlaceHolder1_lblPropertyAddress");
			if (addressNode != null) {
				String address = addressNode.toPlainTextString().trim();
				if (!address.isEmpty()) {
					resultMap.put(PropertyIdentificationSetKey.ADDRESS_ON_SERVER.getKeyName(), address);
					resultMap.put(PropertyIdentificationSetKey.STREET_NO.getKeyName(), StringFormats.parseAddress(address)[0]);
					resultMap.put(PropertyIdentificationSetKey.STREET_NAME.getKeyName(), StringFormats.parseAddress(address)[1]);
				}
			}
			
			Node cityZipNode = htmlParser3.getNodeById("ctl00_ContentPlaceHolder1_lblPropertyCityZip");
			if (cityZipNode != null) {
				String cityZip = cityZipNode.toPlainTextString().trim();
				if (!cityZip.isEmpty()) {
					Matcher ma = Pattern.compile("(?is)(.*?)\\s+(\\d{5}(?:-\\d{4})?)").matcher(cityZip);
					if (ma.find()) {
						resultMap.put(PropertyIdentificationSetKey.CITY.getKeyName(), ma.group(1).trim());
						resultMap.put(PropertyIdentificationSetKey.ZIP.getKeyName(), ma.group(2));
					} else {
						cityZip = cityZip.replaceFirst("(?is),\\s*\\bFL\\b", "");
						resultMap.put(PropertyIdentificationSetKey.CITY.getKeyName(), cityZip.trim());
					}
				}
			}

			// taxHistorySet
			Node taxBillNode = htmlParser3.getNodeById("ctl00_ContentPlaceHolder1_lblTaxBillNumber");

			if (taxBillNode == null) {// for certificates
				taxBillNode = htmlParser3.getNodeById("ctl00_ContentPlaceHolder1_lblTaxBillInfo");
			}
			if (taxBillNode != null) {
				Pattern taxBillPattern = Pattern.compile(".*\\b(\\d+)\\s+(\\d{4})\\s*$");
				Matcher taxBillMatcher = taxBillPattern.matcher(taxBillNode.toPlainTextString());
				if (taxBillMatcher.find()) {
					resultMap.put(TaxHistorySetKey.TAX_BILL_NUMBER.getKeyName(), taxBillMatcher.group(1));
					resultMap.put(TaxHistorySetKey.YEAR.getKeyName(), taxBillMatcher.group(2));
				}
			}

			Node baseAmountNode = htmlParser3.getNodeById("ctl00_ContentPlaceHolder1_lblCombinedTotal");
			if (baseAmountNode == null) {// for certificates
				baseAmountNode = htmlParser3.getNodeById("ctl00_ContentPlaceHolder1_lblCertificateOriginalAmount");
			}
			if (baseAmountNode != null) {
				String baseAmount = baseAmountNode.toPlainTextString().replaceAll("[\\$,\\s]", "");
				if (!baseAmount.isEmpty()) {
					resultMap.put(TaxHistorySetKey.BASE_AMOUNT.getKeyName(), baseAmount);
				}
			}

			Node totalDueNode = htmlParser3.getNodeById("ctl00_ContentPlaceHolder1_lblUnpaidBalance");
			if (totalDueNode == null) {// for certificates
				totalDueNode = htmlParser3.getNodeById("ctl00_ContentPlaceHolder1_lblTotalCertBalance");
			}
			if (totalDueNode != null) {
				String amountDue = totalDueNode.toPlainTextString().replaceAll("[\\$,\\s]", "");
				if (!amountDue.isEmpty()) {
					resultMap.put(TaxHistorySetKey.TOTAL_DUE.getKeyName(), amountDue);
					resultMap.put(TaxHistorySetKey.CURRENT_YEAR_DUE.getKeyName(), amountDue);
				}
			}

			String receiptNumber = "";
			String receiptAmount = "";
			String receiptDate = "";
			Node amountPaidNode = htmlParser3.getNodeById("ctl00_ContentPlaceHolder1_lblAmountCollected");
			if (amountPaidNode == null) {// for certificates
				Node certificateAmountPaid = htmlParser3.getNodeById("lblPA");
				if (certificateAmountPaid != null) {
					amountPaidNode = certificateAmountPaid.getParent();
				}
			}
			if (amountPaidNode != null) {
				receiptAmount = amountPaidNode.toPlainTextString().replaceAll("[\\$,\\s]", "");
				if (!receiptAmount.isEmpty()) {
					resultMap.put(TaxHistorySetKey.AMOUNT_PAID.getKeyName(), receiptAmount);
				}
			}

			// receipt
			Node receiptNumberNode = htmlParser3.getNodeById("ctl00_ContentPlaceHolder1_lblReceiptNumber");
			if (receiptNumberNode == null) {// for certificates
				receiptNumberNode = htmlParser3.getNodeById("lblRN");
			}
			if (receiptNumberNode != null) {
				receiptNumber = receiptNumberNode.toPlainTextString().trim();
			}
			Node receiptDateNode = htmlParser3.getNodeById("ctl00_ContentPlaceHolder1_lblLastPaymentDate");

			if (receiptDateNode == null) {// for certificates
				receiptDateNode = htmlParser3.getNodeById("lblLPD");
			}
			if (receiptDateNode != null) {
				receiptDate = receiptDateNode.toPlainTextString().trim();
			}
			List<List> bodyRT = new ArrayList<List>();
			List<String> line = new ArrayList<String>();
			if (!receiptNumber.isEmpty() || !receiptAmount.isEmpty() || !receiptDate.isEmpty()) {
				line.add(receiptNumber);
				line.add(receiptAmount);
				line.add(receiptDate);
				bodyRT.add(line);
			}
			

			if (!bodyRT.isEmpty()) {
				ResultTable newRT = new ResultTable();
				String[] header = { "ReceiptNumber", "ReceiptAmount", "ReceiptDate" };
				Map<String, String[]> map = new HashMap<String, String[]>();
				map.put("ReceiptNumber", new String[] { "ReceiptNumber", "" });
				map.put("ReceiptAmount", new String[] { "ReceiptAmount", "" });
				map.put("ReceiptDate", new String[] { "ReceiptDate", "" });
				newRT.setHead(header);
				newRT.setMap(map);
				newRT.setBody(bodyRT);
				newRT.setReadOnly();
				resultMap.put("TaxHistorySet", newRT);
			}

			Node installmentsTable = HtmlParser3.getNodeByTypeAttributeDescription(nodeList, "table", "", "",
					new String[] { "Due Date", "Amount", "Receipt#", "Paid", "Amt Due" }, true);

			if (installmentsTable != null && installmentsTable instanceof TableTag) {
				TableRow[] rows = ((TableTag) installmentsTable).getRows();

				// get tax installment set
				Map<String, String[]> installmentsMap = new HashMap<String, String[]>();
				List<List> installmentsBody = new ArrayList<List>();
				ResultTable resultTable = new ResultTable();

				String[] installmentsHeader = {
						TaxInstallmentSetKey.INSTALLMENT_NAME.getShortKeyName(),
						TaxInstallmentSetKey.BASE_AMOUNT.getShortKeyName(),
						TaxInstallmentSetKey.AMOUNT_PAID.getShortKeyName(),
						TaxInstallmentSetKey.STATUS.getShortKeyName()
				};

				for (String s : installmentsHeader) {
					installmentsMap.put(s, new String[] { s, "" });
				}

				String lastPaidDate = "";
				String totalAP = "";

				String[] installmentName = { "First Installment", "Second Installment", "Third Installment", "Fourth Installment" };
				String[] instBA = { "", "", "", "" };
				String[] instAP = { "", "", "", "" };
				String[] instStatus = { "", "", "", "" };
				String[] instDatePaid = { "", "", "", "" };
				String[] instReceiptNo = { "", "", "", "" };

				for (int i = 0; i < 4; i++) {
					TableColumn[] columns = rows[i + 1].getColumns();
					if (columns.length >= 7) {

						List<String> installmentsRow = new ArrayList<String>();

						// get installment name
						installmentsRow.add(installmentName[i]);

						// get base amount
						instBA[i] = columns[2].toPlainTextString().replaceAll(amountPattern, "");
						installmentsRow.add(instBA[i]);

						// get amount paid
						instAP[i] = columns[4].toPlainTextString().replaceAll(amountPattern, "");
						installmentsRow.add(instAP[i]);
						totalAP += instAP[i] + "+";

						// get date paid
						instDatePaid[i] = RegExUtils.getFirstMatch("^\\s*(" + datePattern + ")\\s.*", columns[6].toPlainTextString(), 1);
						if (instDatePaid[i].matches(datePattern)) {
							lastPaidDate = instDatePaid[i];
						}

						// get receipt no
						instReceiptNo[i] = columns[3].toPlainTextString();

						// get status
						if (instDatePaid[i].matches(datePattern)) {
							instStatus[i] = "PAID";
						} else {
							instStatus[i] = "UNPAID";
						}

						installmentsRow.add(instStatus[i]);

						if (installmentsRow.size() == installmentsHeader.length) {
							installmentsBody.add(installmentsRow);
						}
					}
				}

				if (!installmentsBody.isEmpty()) {
					resultTable.setHead(installmentsHeader);
					resultTable.setMap(installmentsMap);
					resultTable.setBody(installmentsBody);
					resultTable.setReadOnly();
					resultMap.put("TaxInstallmentSet", resultTable);
				}

				if (resultMap.get("TaxHistorySet") == null) {
					List<List> bodyHist = new ArrayList<List>();
					resultTable = new ResultTable();
					String[] header = { "ReceiptNumber", "ReceiptAmount", "ReceiptDate" };
					Map<String, String[]> map = new HashMap<String, String[]>();
					map.put("ReceiptNumber", new String[] { "ReceiptNumber", "" });
					map.put("ReceiptAmount", new String[] { "ReceiptAmount", "" });
					map.put("ReceiptDate", new String[] { "ReceiptDate", "" });

					for (int i = 0; i < 4; i++) {
						List<String> receiptRow = new ArrayList<String>();
						if (instStatus[i].equals("PAID")) {

							// get receipt number
							if (StringUtils.isNotEmpty(instAP[i])) {
								receiptRow.add(org.apache.commons.lang.StringUtils.defaultString(instAP[i]));
							}

							// get receipt value
							if (StringUtils.isNotEmpty(instAP[i])) {
								receiptRow.add(org.apache.commons.lang.StringUtils.defaultString(instAP[i]));
							}

							// get receipt date
							if (instDatePaid[i].matches(datePattern)) {
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
				}

				// get last paid date
				if (StringUtils.isNotEmpty(lastPaidDate)) {
					resultMap.put(TaxHistorySetKey.DATE_PAID.getKeyName(), lastPaidDate);
				}

				// total amount paid
				if ((resultMap.get(TaxHistorySetKey.AMOUNT_PAID.getKeyName())) == null && StringUtils.isNotEmpty(totalAP)) {
					totalAP = totalAP.replaceFirst("\\+$", "");
					totalAP = GenericFunctions1.sum(totalAP, getSearch().getID());
					resultMap.put(TaxHistorySetKey.AMOUNT_PAID.getKeyName(), totalAP);
				}
			}

			// get exemption amount
			Node exemptionNode = htmlParser3.getNodeById("ctl00_ContentPlaceHolder1_lblCoExemptValue");
			if (exemptionNode != null) {
				String exemptionAmount = exemptionNode.toPlainTextString().replaceAll(amountPattern, "");
				if (exemptionAmount.contains("(")) {// replace paranthesis with minus
					exemptionAmount = "-" + StringUtils.extractParameter(exemptionAmount, "\\(([\\d.]+)\\)");
				}
				if (StringUtils.isNotEmpty(exemptionAmount)) {
					resultMap.put(TaxHistorySetKey.TAX_EXEMPTION_AMOUNT.getKeyName(), exemptionAmount);
				}

			}

			// get delinquent history table
			Node delinqHistoryNode = htmlParser3.getNodeById("ctl00_ContentPlaceHolder1_dgDelinquentHistory");
			if (delinqHistoryNode != null) {
				String prDlq="";
				double priorDelinq = 0.00d;
				
				TableRow[] rows = ((TableTag) delinqHistoryNode).getRows();
//				if (rows.length > 1 && rows[0].getColumnCount() == 7) {
//					for (int i=1; i < rows.length - 1; i++) {
//						prDlq = rows[i].getColumns()[5].toPlainTextString().trim().replaceAll("[\\$,]", "");
//						if (i==1 && prDlq.equals((String) resultMap.get(TaxHistorySetKey.TOTAL_DUE.getKeyName()))) {} 
//						else {
//							priorDelinq += Double.parseDouble(prDlq);
//						}
//					}
//				}
				if (rows.length > 1 && rows[0].getColumnCount() == 7) {
					String year = rows[1].getColumns()[0].getStringText();
					prDlq = rows[rows.length-1].getColumns()[5].toPlainTextString().trim().replaceAll("[\\$,]", "");
					if (!prDlq.equals(resultMap.get(TaxHistorySetKey.TOTAL_DUE.getKeyName()))) {
						if (Integer.parseInt(year) < Integer.parseInt((String)resultMap.get(TaxHistorySetKey.YEAR.getKeyName())))
							priorDelinq += Double.parseDouble(prDlq);
					}
					if (priorDelinq != 0) {
						if (!"Bay".equals(dataSite.getCountyName())) {
							priorDelinq = priorDelinq - Double.parseDouble((String) resultMap.get(TaxHistorySetKey.TOTAL_DUE.getKeyName()));
						}
						
						resultMap.put(TaxHistorySetKey.PRIOR_DELINQUENT.getKeyName(), Double.toString(priorDelinq));
					}
				}
				
			}
			
			Node legalNode = htmlParser3.getNodeById("ctl00_ContentPlaceHolder1_lblLegal");

			if (legalNode != null) {
				String legalDescription = legalNode.toPlainTextString().replaceAll("\\s+", " ")
						.trim();
				if (!legalDescription.isEmpty()) {
					resultMap.put(PropertyIdentificationSetKey.PROPERTY_DESCRIPTION.getKeyName(), legalDescription);
				}
			}
			else {// for certificates
				NodeList legalNodes = new NodeList();
				for (int i = 1; i <= 4; i++) {
					legalNode = htmlParser3.getNodeById("ctl00_ContentPlaceHolder1_lblLegal" + i);
					if (legalNode != null) {
						legalNodes.add(legalNode);
					}
				}
				if (legalNodes.size() > 0) {
					StringBuilder legalDescriptionSb = new StringBuilder();
					for (int i = 0; i < legalNodes.size(); i++) {
						legalDescriptionSb.append(legalNodes.elementAt(i).toPlainTextString() + " ");
					}
					String legalDescription = legalDescriptionSb.toString();
					if (!legalDescription.isEmpty()) {
						resultMap.put(PropertyIdentificationSetKey.PROPERTY_DESCRIPTION.getKeyName(), legalDescription.replaceAll("\\s+", " ").trim());
					}
				}

			}

			Node assessmentNode = htmlParser3.getNodeById("ctl00_ContentPlaceHolder1_lblGrossTaxValue");
			if (assessmentNode != null) {
				resultMap.put(PropertyAppraisalSetKey.TOTAL_ASSESSMENT.getKeyName(), assessmentNode.toPlainTextString().replaceAll("[\\$,\\s]", ""));
			}

			partyNames(resultMap);
			ro.cst.tsearch.extractor.xml.GenericFunctions2.legalFLVisualGovTR(resultMap, searchId);
		} catch (Exception e) {
			logger.error("Error while parsing details");
		}
		return null;
	}

	@SuppressWarnings("rawtypes")
	public static void splitResultRows(Parser p, ParsedResponse pr, String htmlString, int pageId, String linkStart, int action) throws ServerResponseException {
		p.splitResultRows(pr, htmlString, pageId, "<tr", "</table>", linkStart, action);

		// remove table header
		Vector rows = pr.getResultRows();
		if (rows.size() > 0) {
			ParsedResponse firstRow = (ParsedResponse) rows.remove(0);
			pr.setResultRows(rows);
			pr.setHeader(pr.getHeader() + firstRow.getResponse());
		}
	}

	@SuppressWarnings("deprecation")
	@Override
	protected String getFileNameFromLink(String url) {
		String keyCode = "";
		if (url.contains("dummy=")) {
			keyCode = getNestedString(url, "dummy=", "&");
		} else {

		}
		return keyCode + ".html";
	}

	@Override
	public ServerResponse SearchBy(TSServerInfoModule module, Object sd) throws ServerResponseException {

		// add dashes to pin if necessary
		if (module.getModuleIdx() == TSServerInfo.ADV_SEARCH_MODULE_IDX) {
			// turn 043721000 into 04-3721-000
			String pin = module.getFunction(3).getParamValue();
			if (!pin.isEmpty()) {
				int countyId = dataSite.getCountyId();

				if (countyId == CountyConstants.FL_Okeechobee) {
					// // turn R31537350010014400040 into 3-15-37-35-0010-01440-0040
					// R\d{7}[\dA-Z]{4}\d{5}[\dA-Z]{4}
					Pattern p1 = Pattern.compile("R(\\d)-?(\\d{2})-?(\\d{2})-?(\\d{2})-?([\\dA-Z]{4})-?(\\d{5})-?([\\dA-Z]{4})");
					Matcher ma1 = p1.matcher(pin);
					if (ma1.find()) {
						pin = ma1.group(1) + "-" + ma1.group(2) + "-"
								+ ma1.group(3) + "-" + ma1.group(4) + "-"
								+ ma1.group(5) + "-" + ma1.group(6) + "-"
								+ ma1.group(7);
					}
					/*
					 * else { // turn 3-15-37-35-0010-01440-0040 into R31537350010014400040 // (\\d)-(\\d{2})-(\\d{2})-(\\d{2})-([\\
					 * dA-Z]+)-([\\dA-Z]+)-([\\dA-Z]+) Pattern p2 = Pattern.compile( "(\\d)-(\\d{2})-(\\d{2})-(\\d{2})-([\\dA-Z]+)-([\\dA-Z]+)-([\\dA-Z]+)" );
					 * Matcher ma2 = p2.matcher(pin); if (ma2.find()) { pin = "R" + ma2.group(1) + ma2.group(2) + ma2.group(3) + ma2.group(4) + ma2.group(5) +
					 * ma2.group(6) + ma2.group(7); } }
					 */
				} else if (countyId == CountyConstants.FL_Franklin) {
					// Turn SS-TTT-RRR-xxxx-xxxx-xxxx into SSTTRRxxxxxxxxxxxx
					Pattern pattern = Pattern.compile("([A-Z0-9]{2})-?([A-Z0-9]{3})-?([A-Z0-9]{3})-?([A-Z0-9]{4})-?([A-Z0-9]{4})-?([A-Z0-9]{4})");
					Matcher matcher = pattern.matcher(pin);
					if (matcher.find()) {
						pin = matcher.group(1) + "-" + matcher.group(2) + "-"
								+ matcher.group(3) + "-" + matcher.group(4) + "-"
								+ matcher.group(5) + "-" + matcher.group(6);
					}
				} else if (countyId == CountyConstants.FL_Gulf) {
					// Turn xxxxxxxxR into xxxxx-xxxR
					if (!pin.contains("-")) {
						pin = pin.replaceAll("(?is)(\\d{5})(\\d{3})", "$1-$2");
					}
				} else if (countyId == CountyConstants.FL_DeSoto) {
					if (!pin.contains("-")) {
						pin = pin.replaceAll("(?is)(\\d{2})(\\d{2})(\\d{2})(\\d{4})([A-Z\\d]{4})(\\d{4})", "$1-$2-$3-$4-$5-$6");
					}
				} else {
					pin = pin.replaceFirst("\\s*(\\d{5})-?(\\d{3})-?(\\d{3})\\s*", "$1-$2-$3");
				}
				module.getFunction(3).setParamValue(pin);
			}
		}
		return super.SearchBy(module, sd);
	}

	@Override
	public TSServerInfo getDefaultServerInfo() {

		TSServerInfo msiServerInfoDefault = super.getDefaultServerInfo();
		TSServerInfoModule tsServerInfoModule = msiServerInfoDefault.getModule(TSServerInfo.ADV_SEARCH_MODULE_IDX);

		DataSite dataSite = HashCountyToIndex.getDateSiteForMIServerID(HashCountyToIndex.ANY_COMMUNITY, miServerID);
		String county = dataSite.getCountyName();
		// get tax years select list
		if (tsServerInfoModule != null) {
			tsServerInfoModule.getFunction(6).setHtmlformat(getYearSelect("param_12_6", "param_12_6", dataSite.getLink() + destinationPage));
		}

		tsServerInfoModule = msiServerInfoDefault.getModule(TSServerInfo.ADV_SEARCH_MODULE_IDX);
		if (tsServerInfoModule != null) {

			PageZone pageZone = (PageZone) tsServerInfoModule.getModuleParentSiteLayout();
			for (HTMLControl htmlControl : pageZone.getHtmlControls()) {
				String functionName = htmlControl.getCurrentTSSiFunc().getName();

				if ("Property Owners Name".equals(functionName)) {
					if ("Bay".equals(county)) {
						htmlControl.setFieldNote("Ex. Smith, David");
					} else {
						htmlControl.setFieldNote("Ex. Smith David");
					}
				}

				else if ("Property Number".equals(functionName)) {
					if ("Gulf".equals(county)) {
						htmlControl.setFieldNote("Ex. 12345-678M");
					} else if ("Jackson".equals(county)) {
						htmlControl.setFieldNote("Ex. 12-3Z-45-6789-1234-5678");
					} else if ("Wakulla".equals(county)) {
						htmlControl.setFieldNote("Ex. 12-34-567-891-23456-789");
					} else if ("Bay".equals(county)) {
						htmlControl.setFieldNote("Ex. 31200-002-001");
					} else if ("DeSoto".equals(county)) {
						htmlControl.setFieldNote("Ex. 00-11-22-1234-5678-9111");
					} else if ("Franklin".equals(county)) {
						htmlControl.setFieldNote("Ex. 88-99P-77Z-1234-5678-9010");
					} else if ("Okeechobee".equals(county)) {
						htmlControl.setFieldNote("Ex. 7-34-56-78-9Y12-34567-8912");
					} else if ("Hardee".equals(county)) {
						htmlControl.setFieldNote("Ex. 00-11-22-3456-78912-3456");
					} else if ("Calhoun".equals(county)) {
						htmlControl.setFieldNote("Ex. 00-0Z-55-1234-5678-1213");
					}
				}
			}
		}
		setModulesForAutoSearch(msiServerInfoDefault);
		return msiServerInfoDefault;
	}

	@Override
	public Collection<ParsedResponse> smartParseIntermediary(
			ServerResponse response, String table, StringBuilder outputTable) {
		Vector<ParsedResponse> intermediaryResponse = new Vector<ParsedResponse>();

		try {
			HtmlParser3 htmlParser3 = new HtmlParser3(table);
			NodeList nodeList = htmlParser3.getNodeList();
			NodeList mainTableList = nodeList
					.extractAllNodesThatMatch(new TagNameFilter("table"), true);

			if (mainTableList.size() == 0) {
				return intermediaryResponse;
			}
			TableTag tableTag = (TableTag) mainTableList.elementAt(0);
			TableRow[] rows = tableTag.getRows();
			for (int i = 0; i < rows.length; i++) {
				TableRow row = rows[i];
				if (row.getColumnCount() == 4) {
					LinkTag linkTag = (LinkTag) row.getColumns()[0].getFirstChild();

					String link = linkTag.extractLink().trim().replaceAll("\\s", "%20");
					ParsedResponse currentResponse = new ParsedResponse();
					currentResponse.setAttribute(ParsedResponse.SERVER_ROW_RESPONSE, row.toHtml());
					currentResponse.setOnlyResponse(row.toHtml());
					currentResponse.setPageLink(new LinkInPage(link, link, TSServer.REQUEST_SAVE_TO_TSD));

					ResultMap m = parseIntermediaryRow(row, searchId);
					m.put(OtherInformationSetKey.SRC_TYPE.getKeyName(), getDataSite().getSiteTypeAbrev());

					Bridge bridge = new Bridge(currentResponse, m, searchId);

					DocumentI document = (TaxDocumentI) bridge.importData();
					currentResponse.setDocument(document);

					intermediaryResponse.add(currentResponse);
				}
			}
			String pagingDiv = processLinks(response, table);
			response.getParsedResponse()
					.setHeader(
							pagingDiv
									+ "<table border=\"1\" width=\"50%\"><tr><th>Property Number</th><th>Name</th><th>Address</th><th class=\"taxbill\">Tax Bill #</th></tr>");
			response.getParsedResponse().setFooter("</table>");

			outputTable.append(table);

		} catch (Throwable t) {
			logger.error("Error while parsing intermediary data", t);
		}
		return intermediaryResponse;
	}

	protected String processLinks(ServerResponse response, String table) {

		String link = CreatePartialLink(TSConnectionURL.idPOST) + response.getLastURI() + "?" + intermediaryLink;
		Pattern resultInfoPattern = Pattern.compile("(?is)Page\\s+(\\d+)\\s+of\\s+(\\d+).\\s*Total\\s+(\\d+)\\s+records\\s+found.");
		Matcher resultInfoMatcher = resultInfoPattern.matcher(table);
		resultInfoMatcher.reset();
		int numberOfPages = 0;
		int currentPage = 0;
		// int totalNumberOfResults = 0;

		String resultInfo = "";
		if (resultInfoMatcher.find()) {
			resultInfo = resultInfoMatcher.group(0);
			currentPage = Integer.parseInt(resultInfoMatcher.group(1));
			numberOfPages = Integer.parseInt(resultInfoMatcher.group(2));
			// totalNumberOfResults = Integer.parseInt(resultInfoMatcher.group(3));
		}
		// int resultsPerPage = (int) Math.ceil((double) totalNumberOfResults / numberOfPages);
		StringBuilder pagingDiv = new StringBuilder("<div id=\"resultInfo\" >" + resultInfo + "</div>");
		
		// First,Previous
		pagingDiv.append("<table width=\"50%\" border=\"1\"><tr><td align=\"center\"><div id=\"paging\">");
		String skipPattern = "(?s)(.*?skip=)(\\d*)(.*)";
		if (currentPage == 1) {
			pagingDiv.append("First&nbsp;");
			pagingDiv.append("Previous&nbsp;");
		}
		else {
			link = link.replaceFirst(skipPattern, "$10$3");
			pagingDiv.append("<a href=\"" + link + "\"><u>First</u></a>&nbsp;");

			int previous = (currentPage - 2);
			if (previous <= 1) {
				previous = 1;
			}

			link = link.replaceFirst(skipPattern, "$1" + previous + "$3");
			pagingDiv.append("<a href=\"" + link + "\"><u>Previous</u></a>&nbsp;");
		}
		// pages
		int lowerLimit = currentPage - 5;
		int upperLimit = currentPage + 4;

		if (lowerLimit < 1) {
			lowerLimit = 1;
			upperLimit = lowerLimit + 9;
		}
		if (upperLimit > numberOfPages) {
			upperLimit = numberOfPages;
			lowerLimit = upperLimit - 9;
			if (lowerLimit < 1) {
				lowerLimit = 1;
			}
		}
		for (int i = lowerLimit; i <= upperLimit; i++) {
			if (i == currentPage) {
				pagingDiv.append("<strong>" + currentPage + "</strong>&nbsp;");
				continue;
			}
			link = link.replaceFirst(skipPattern, "$1" + (i - 1) + "$3");
			pagingDiv.append("<a href=\"" + link + "\">" + i + "</a>&nbsp;");
		}

		// Next, Last
		if (currentPage == numberOfPages) {
			pagingDiv.append("Next&nbsp");
			pagingDiv.append("Last&nbsp");
		}
		else {
			link = link.replaceFirst(skipPattern, "$1" + currentPage + "$3");
			String nextLink = "<a href=\"" + link + "\"><u>Next</u></a>&nbsp;";
			pagingDiv.append(nextLink);
			//set next link
			response.getParsedResponse().setNextLink(nextLink);
			
			link = link.replaceFirst(skipPattern, "$1" + (numberOfPages - 1) + "$3");
			pagingDiv.append("<a href=\"" + link + "\"><u>Last</u></a>&nbsp;");
		}
		
			
		
		return pagingDiv + "</div></td></tr></table>";
	}

	private ResultMap parseIntermediaryRow(TableRow row, long searchId) {
		ResultMap resultMap = new ResultMap();
		try {
			TableColumn[] cols = row.getColumns();
			if (cols.length == 4) {
				String parcelID = cols[0].toPlainTextString();// last 7 characters are for the year; ex: " - 2009"
				String nameOnServer = cols[1].toPlainTextString();
				String fullAddress = cols[2].toPlainTextString();
				String[] address = StringFormats.parseAddress(fullAddress.trim());

				resultMap.put(PropertyIdentificationSetKey.PARCEL_ID.getKeyName(), parcelID.substring(0, parcelID.length() - 7).trim());
				resultMap.put(TaxHistorySetKey.YEAR.getKeyName(), parcelID.substring(parcelID.length() - 4, parcelID.length()).trim());
				
				resultMap.put(PropertyIdentificationSetKey.NAME_ON_SERVER.getKeyName(), nameOnServer);
				resultMap.put(PropertyIdentificationSetKey.STREET_NO.getKeyName(), address[0]);
				resultMap.put(PropertyIdentificationSetKey.STREET_NAME.getKeyName(), address[1]);
				partyNames(resultMap);
			}
			resultMap.removeTempDef();
		} catch (Exception e) {
			e.printStackTrace();
		}
		return resultMap;
	}

	//public static void partyNames(ResultMap resultMap) throws Exception {
	@SuppressWarnings("rawtypes")
	private void partyNames(ResultMap resultMap) throws Exception {
		String unparsedName = (String) resultMap.get("PropertyIdentificationSet.NameOnServer");
		List<List> body = new ArrayList<List>();
		if (StringUtils.isNotEmpty(unparsedName)) {
			unparsedName = unparsedName.replaceAll("\\s*-\\s*(ETAL),?\\s*", " $1 - ");

			unparsedName = unparsedName.replaceAll("(?is)\\bJO ANN\\b", "JOFAKEANN");
			unparsedName = unparsedName.replaceAll("(?is)\\.-\\s", ". - ");
			unparsedName = unparsedName.replaceFirst("(?i)\\s+OR\\s+", " & ");

			if (unparsedName.indexOf(" - ") == -1) { // bug 6871
				// WILSON, JAMES FORSYTHE, III , ELISE P. & MICHAEL F.
				unparsedName = unparsedName.replaceAll("(?is),\\s*(JR|SR|II|III|IV)\\b", " $1");
				unparsedName = unparsedName.replaceAll("(?is),\\s*(T(?:(?:RU?)?S?)?(?:TE)?E?S?)\\b", " $1");
				unparsedName = unparsedName.replaceAll("(?is),\\s*(ET[\\s,;]*UX|ET[\\s,;]*AL|ET[\\s,;]*VIR)\\b", " $1");
				// ZWALLY, H. JAY, JO ANN M. & KURT D.
				// RYCHEL B SUSAN W KENT & RYCHEL E LEIGH
				unparsedName = unparsedName.replaceFirst(",", "@@@");
				unparsedName = unparsedName.replaceFirst(",", " &");
				unparsedName = unparsedName.replaceFirst("@@@", ",");
			}

			unparsedName = unparsedName.replaceAll("(?is)\\b(A|F)KA\\b", " - @@@FML@@@");
			unparsedName = unparsedName.replaceAll("(?is)\\bInCareOfName\\b", " - @@@FML@@@");
			unparsedName = unparsedName.replaceAll("(?is)\\s+-\\s+ETAL", " ETAL - ");
			unparsedName = unparsedName.replaceFirst("&\\s*$", "");

			// 9951 issue#4 e.g. 04-4N-10-0000-1560-0080:
			if (unparsedName.split("(,|&)").length >= 4) {
				unparsedName = unparsedName.replaceAll("(&|,)", " - @@@FML@@@");
			} else if (unparsedName.split(" ").length >= 6) {
				unparsedName = unparsedName.replaceAll(",", " - @@@FML@@@");
			}

			String[] mainTokens = unparsedName.split("\\s+-\\s+");

			boolean is_csc = false;// comma separated companies

			// bug 7053
			if (unparsedName.split(",").length > 1) {
				if (unparsedName.split(",")[0].split(" ").length >= 4
						&& unparsedName.split(",")[1].split(" ").length >= 4) {
					mainTokens = unparsedName.split(",");
					is_csc = true;
				}
			}

			boolean onlyCompanies = true;
			for (int i = 0; i < mainTokens.length && onlyCompanies; i++)
				onlyCompanies = onlyCompanies && NameUtils.isCompany(mainTokens[i]);
			if (onlyCompanies && unparsedName.contains("&")) {
				mainTokens = new String[] { unparsedName };
			}

			for (int i = 0; i < mainTokens.length; i++) {
				String currentToken = mainTokens[i];
				if (is_csc) {
					String[] names = StringFormats.parseNameFML(currentToken, new Vector<String>(), false);

					String[] types = GenericFunctions.extractAllNamesType(names);
					String[] otherTypes = GenericFunctions.extractAllNamesOtherType(names);
					String[] suffixes = GenericFunctions.extractAllNamesSufixes(names);

					GenericFunctions.addOwnerNames(names, suffixes[0], suffixes[1], types, otherTypes,
							NameUtils.isCompany(names[2]), NameUtils.isCompany(names[5]), body);

				} else if ((!currentToken.contains("&") && !currentToken.contains(".,") || NameUtils.isCompany(currentToken)) && !currentToken.contains(" REMOVEME")) { // single name
					String[] names;
					if (currentToken.trim().startsWith("@@@FML@@@")) {
						currentToken = currentToken.replaceAll("@@@FML@@@", "").trim();
						names = StringFormats.parseNameDesotoRO(currentToken, true);
					} else
						names = StringFormats.parseNameNashville(currentToken, true);

					String[] types = GenericFunctions.extractAllNamesType(names);
					String[] otherTypes = GenericFunctions.extractAllNamesOtherType(names);
					String[] suffixes = GenericFunctions.extractAllNamesSufixes(names);

					GenericFunctions.addOwnerNames(names, suffixes[0], suffixes[1], types, otherTypes,
							NameUtils.isCompany(names[2]), NameUtils.isCompany(names[5]), body);

				} else {
					String[] possibleNames = currentToken.split("&|\\.,");
					String firstPossibleLastName = null;
					for (int j = 0; j < possibleNames.length; j++) {
						int len = possibleNames.length;
						if (j < len - 1 && possibleNames[len - 1].trim().toLowerCase().endsWith("trustees"))
							currentToken = possibleNames[j].trim() + " TR";
						else
							currentToken = possibleNames[j].trim();
						if (j == 0) {
							if (j + 1 < possibleNames.length) {
								if (GenericFunctions.nameSuffix3.matcher(possibleNames[j + 1].trim()).matches()) {
									currentToken += " " + possibleNames[j + 1].trim();
									j++;
								}
							}
							
							String[] names = StringFormats.parseNameNashville(currentToken, true);

							String[] types = GenericFunctions.extractAllNamesType(names);
							String[] otherTypes = GenericFunctions.extractAllNamesOtherType(names);
							String[] suffixes = GenericFunctions.extractAllNamesSufixes(names);

							GenericFunctions.addOwnerNames(names, suffixes[0], suffixes[1], types, otherTypes,
									NameUtils.isCompany(names[2]), NameUtils.isCompany(names[5]), body);
							firstPossibleLastName = names[2];
						} else {

							if (j + 1 < possibleNames.length) {
								if (GenericFunctions.nameSuffix3.matcher(possibleNames[j].trim()).matches()) {
									currentToken += possibleNames[j].trim();
									j++;
								}
							}

							if (!currentToken.startsWith(firstPossibleLastName)) {
								if (currentToken.matches("\\w\\.\\s+\\w+")
										&& !currentToken.matches("(?is)W\\. KENT") && !currentToken.matches("(?is)E\\. LEIGH")) { // B 6871
									currentToken = currentToken.replaceAll("(\\w\\.)\\s+(\\w+)", "$2 $1");
								}
								if (currentToken.indexOf(",") == -1) {
									Matcher lNameMatcher = Pattern.compile("(?is)(\\w+)\\s+(\\w+)\\s+(\\w+)").matcher(currentToken);
									if ("Franklin".equals(dataSite.getCountyName()) && lNameMatcher.find()) {
										if ("REMOVEME".equals(lNameMatcher.group(3))) {
											currentToken = lNameMatcher.group(2) + ", " + lNameMatcher.group(1); 
										} else {
											currentToken = lNameMatcher.group(3) + ", " + lNameMatcher.group(1) + " " + lNameMatcher.group(2); 
										}
									} else {
										currentToken = firstPossibleLastName + ", " + currentToken;
									}
								}
							}

							String[] names = StringFormats.parseNameNashville(currentToken, true);

							String[] types = GenericFunctions.extractAllNamesType(names);
							String[] otherTypes = GenericFunctions.extractAllNamesOtherType(names);
							String[] suffixes = GenericFunctions.extractAllNamesSufixes(names);

							names[0] = names[0].replaceAll("(?is)\\bJOFAKEANN\\b", "JO ANN");

							GenericFunctions.addOwnerNames(names, suffixes[0], suffixes[1], types, otherTypes,
									NameUtils.isCompany(names[2]), NameUtils.isCompany(names[5]), body);
						}
					}
				}
			}

			try {
				GenericFunctions.storeOwnerInPartyNames(resultMap, body, true);
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}

	public String getYearSelect(String id, String name, String destinationPage) {
		String county = dataSite.getCountyName();
		getTaxYearsRange(county, destinationPage);
		int lastTaxYear = -1;
		try {
			lastTaxYear = taxYears.get("lastTaxYear" + county);
		} catch (Exception e) {
		}
		int firstTaxYear = -1;
		try {
			firstTaxYear = taxYears.get("firstTaxYear" + county);
		} catch (Exception e) {
		}
		if (lastTaxYear <= 0 || firstTaxYear <= 0) {
			// No valid tax years.
			// This is going to happen when official site is down or it's going to change its layout.
			lastTaxYear = 2013;
			firstTaxYear = 1977;
		}

		// Generate input.
		StringBuilder select = new StringBuilder("<select id=\"" + id + "\" name=\"" + name + "\">\n");

		for (int i = lastTaxYear; i >= firstTaxYear; i--) {
			select.append("<option ");
			select.append("value=\"" + i + "\">" + i + "</option>\n");
		}
		select.append("<option selected=\"selected\" value=\"\">All</option>\n");
		select.append("</select>");

		return select.toString();
	}

	/**
	 * Get tax year range from official site
	 */
	private void getTaxYearsRange(String county, String destinationPage) {
		if (taxYears.containsKey("lastTaxYear" + county) && taxYears.containsKey("firstTaxYear" + county))
			return;

		// Get official site html response.
		String response = getLinkContents(destinationPage);

		if (response != null) {
			HtmlParser3 parser = new HtmlParser3(response);
			Node selectList = parser.getNodeById("Years");
			if (selectList == null) {
				// Unable to find the tax year select input.
				logger.error("Unable to parse tax years!");
				return;
			}

			// Get the first and last tax years.
			SelectTag selectTag = (SelectTag) selectList;
			OptionTag[] options = selectTag.getOptionTags();
			try {
				taxYears.put("lastTaxYear" + county, Integer.parseInt(options[0].getValue().trim()));
				taxYears.put("firstTaxYear" + county, Integer.parseInt(options[options.length - 2].getValue().trim()));
			} catch (Exception e) {
				e.printStackTrace();
			}
		} else {
			taxYears.put("lastTaxYear" + county, Calendar.getInstance().get(Calendar.YEAR));
			taxYears.put("firstTaxYear" + county, 2007);
		}
	}

	protected void loadDataHash(HashMap<String, String> data) {
		if (data != null) {
			data.put("type", "CNTYTAX");
		}
	}
}
