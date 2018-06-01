package ro.cst.tsearch.servers.types;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.math.BigDecimal;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Vector;

import org.apache.commons.lang.StringUtils;
import org.htmlparser.Node;
import org.htmlparser.tags.TableColumn;
import org.htmlparser.tags.TableRow;
import org.htmlparser.util.NodeList;
import org.htmlparser.util.SimpleNodeIterator;
import org.pdfbox.pdmodel.PDDocument;
import org.pdfbox.util.PDFTextStripper;

import ro.cst.tsearch.Search;
import ro.cst.tsearch.bean.SearchAttributes;
import ro.cst.tsearch.connection.http2.HttpManager;
import ro.cst.tsearch.connection.http2.HttpSite;
import ro.cst.tsearch.datatrace.Utils;
import ro.cst.tsearch.exceptions.ServerResponseException;
import ro.cst.tsearch.extractor.xml.Bridge;
import ro.cst.tsearch.extractor.xml.GenericFunctions;
import ro.cst.tsearch.extractor.xml.ResultMap;
import ro.cst.tsearch.parser.HtmlParser3;
import ro.cst.tsearch.search.FunctionStatesIterator;
import ro.cst.tsearch.search.filter.FilterResponse;
import ro.cst.tsearch.search.filter.TaxYearFilterResponse;
import ro.cst.tsearch.search.filter.newfilters.address.AddressFilterFactory;
import ro.cst.tsearch.search.filter.newfilters.name.NameFilterFactory;
import ro.cst.tsearch.search.filter.newfilters.pin.PINFilterFactory;
import ro.cst.tsearch.search.filter.newfilters.pin.PinFilterResponse;
import ro.cst.tsearch.search.iterator.ModuleStatesIteratorFactory;
import ro.cst.tsearch.search.module.ConfigurableNameIterator;
import ro.cst.tsearch.servers.TSConnectionURL;
import ro.cst.tsearch.servers.info.TSServerInfo;
import ro.cst.tsearch.servers.info.TSServerInfoModule;
import ro.cst.tsearch.servers.response.ImageLinkInPage;
import ro.cst.tsearch.servers.response.LinkInPage;
import ro.cst.tsearch.servers.response.ParsedResponse;
import ro.cst.tsearch.servers.response.PropertyIdentificationSet.PropertyIdentificationSetKey;
import ro.cst.tsearch.servers.response.ServerResponse;
import ro.cst.tsearch.servers.response.TaxHistorySet.TaxHistorySetKey;
import ro.cst.tsearch.servlet.URLConnectionReader;
import ro.cst.tsearch.utils.FileUtils;
import ro.cst.tsearch.utils.RegExUtils;
import ro.cst.tsearch.utils.gargoylesoftware.HtmlElementHelper;

import com.gargoylesoftware.htmlunit.Page;
import com.stewart.ats.base.document.DocumentI;

public class MOPlatteTROld extends TemplatedServer {

	/**
	 * 
	 */
	private static final long serialVersionUID = -5510063110625065679L;

	public MOPlatteTROld(String rsRequestSolverName, String rsSitePath, String rsServerID, String rsPrmNameLink, long searchId, int miServerID) {
		super(rsRequestSolverName, rsSitePath, rsServerID, rsPrmNameLink, searchId, miServerID);

		// int[] intermediary_cases = {TSServer.ID_SEARCH_BY_NAME};
		// setINTERMEDIARY_CASES(intermediary_cases);

		int[] link_cases = new int[] { ID_GET_LINK };
		setLINK_CASES(link_cases);

		int[] save_cases = new int[] { ID_SAVE_TO_TSD };
		setSAVE_CASES(save_cases);

		int[] details_cases = new int[] { ID_DETAILS };
		setDetailsCases(details_cases);

		// setDETAILS_MESSAGE("PIN #:");
		setIntermediaryMessage("Click on 'Print' for the row you wish to get a receipt/statement.");

	}

	@Override
	protected void setMessages() {
		getErrorMessages().addServerErrorMessage(ro.cst.tsearch.connection.http2.MOPlatteTROld.THE_SITE_DOESN_T_CONTAIN_THE_REQUIRED_PDF_FILE);
	}

	@Override
	protected String getAccountNumber(String serverResult) {
		String parcelID = RegExUtils.getFirstMatch("([0-9\\.\\-]+)\"", serverResult, 1);
		return parcelID;
	}

	protected String prepareAccountNumber(String serverResult) {
		// String parcelID = RegExUtils.getFirstMatch("([0-9\\.\\-]+)\"",
		// serverResult, 1);
		String accountNumber = getAccountNumber(serverResult);
		// 20-2.0-10-100-8-069-000
		// 19-3.0-08-100-003-001-000
		// 19-3.0-8-100-3-1.000-2010
		// 7-8.0-28- 0-0-5.000
		// 7-8.0-28-0-0-5.000-2007
		// from NB is brought doc with Pin: 078028000000005000
		// from AO is brought doc with Pin: 07-8.0-28-000-000-005-000
		// 7-8.0-28-0-0-5.000-2007
		String[] split = accountNumber.split("-");
		StringBuilder newAccount = new StringBuilder();
		if (split.length == 7) {
			int i = 0;
			String firstGroup = split[i];
			if (firstGroup.length() == 1) {
				firstGroup = "0" + firstGroup;
			}
			newAccount.append(firstGroup + "-").append(split[++i] + "-");
			if (split[2].length() == 1) {
				// 2
				newAccount.append("0" + split[++i] + "-");
			} else {
				// 2
				newAccount.append(split[++i] + "-");
			}
			// 3
			String thirdGroup = split[++i];
			if (thirdGroup.length() < 3) {
				thirdGroup += "00";
				thirdGroup = thirdGroup.substring(thirdGroup.length() - 3, thirdGroup.length());
			}
			newAccount.append(thirdGroup + "-");
			// 4
			if (split[++i].length() == 2 || split[i].length() == 1) {
				if (split[i].length() == 2)
					newAccount.append("0" + split[i] + "-");
				else
					newAccount.append("00" + split[i] + "-");
			} else {
				newAccount.append(split[i] + "-");
			}
			// 5
			String[] fifthGroup = split[++i].split("\\.");
			if (fifthGroup.length == 2) {
				switch (fifthGroup[0].length()) {
				case 2:
					newAccount.append("0" + fifthGroup[0] + "-");
					break;
				case 1:
					newAccount.append("00" + fifthGroup[0] + "-");
					break;
				default:
					newAccount.append(fifthGroup[0] + "-");
				}
				newAccount.append(fifthGroup[1]);
			}
		}
		return newAccount.toString();
	}

	@Override
	protected void ParseResponse(String action, ServerResponse response, int viParseID) throws ServerResponseException {
		if (viParseID == TSServer.ID_SEARCH_BY_NAME) {
			super.ParseResponse(action, response, ID_DETAILS);
		} else {
			super.ParseResponse(action, response, viParseID);
			if (isError(response)) {
				response.setError(ro.cst.tsearch.connection.http2.MOPlatteTROld.THE_SITE_DOESN_T_CONTAIN_THE_REQUIRED_PDF_FILE);
				response.setError(ServerResponse.NOT_VALID_DOC_ERROR);
				response.setResult("");
				response.getParsedResponse().setError(
						ro.cst.tsearch.connection.http2.MOPlatteTROld.THE_SITE_DOESN_T_CONTAIN_THE_REQUIRED_PDF_FILE);
			}
		}
	}

	@Override
	protected String detailsCasesParse(String action, ServerResponse response, int viParseID, String serverResult, String accountNumber) {
		// get the result body
		String resultBody = RegExUtils.getFirstMatch("(?is)<div id=\"main\".*?</div>", serverResult, 0);
		// remove the unnecessary descriptor table
		String matchString = "RECEIPTS/STATEMENTS USE ADOBE ACROBAT FOR A VIEWER, PLEASE DOWNLOAD THE FREE VERSION IF YOU NEED IT.";
		resultBody = resultBody.replaceAll("(?is)<table cellspacing=\"3\" cellpadding=\"2\" width = \"100%\">.*?" + matchString
				+ "*?</table>", "");
		resultBody = resultBody.replaceAll("</?form>", "");

		// create links from Print button
		String docNumberPath = "onlinerec/platter_rec_state.php?fullparcel=";
		String link = CreatePartialLink(TSConnectionURL.idGET) + docNumberPath;
		resultBody = resultBody.replaceAll("(?is)<input type=button.*?([0-9\\-\\.]+)'\\)\">", "<a href=\"" + link
				+ "$1\" target=\"_blank\">	Show document</a>");

		// change the succession of tables to a succession of table rows
		List<String> list = RegExUtils.getMatches("(?is)<table cellspacing=\"3\" cellpadding=\"2\" width = \"100%\">(.*?)</table>",
				resultBody, 1);

		String newTable = "<table>" + StringUtils.join(list, "") + "</table>";
		List<HashMap<String, String>> tableAsListMap = HtmlParser3.getTableAsListMap(newTable);

		Vector<ParsedResponse> intermediaryResponse = new Vector<ParsedResponse>();
		String tableHeader = list.get(0);
		String aoMess = "<font color='red' > <b> &nbsp;&nbsp;Select just one document !</b> </font>";
		if (getSearch().getSearchType() == Search.PARENT_SITE_SEARCH) {
			aoMess = "";
		}
		StringBuilder finalHtml = new StringBuilder();
		for (int i = 0; i < tableAsListMap.size(); i++) {
			ParsedResponse currentResponse = new ParsedResponse();
			String currentHtmlRow = list.get(i + 1);

			HashMap<String, String> data = new HashMap<String, String>();
			data.put("type", "CNTYTAX");
			HashMap<String, String> currentRecordValues = tableAsListMap.get(i);
			String currentYear = currentRecordValues.get("Year");
			data.put("year", "" + currentYear);
			String parcelNo = currentRecordValues.get("Parcel");
			
			accountNumber = getAccountNumber(currentHtmlRow);
			String checkbox = "";
			String originalLink = action + "&" + docNumberPath;
			
			//19 - 3.0 - 5 - 300 - 7 - 6.000 must become 19-3.0-05-300-007-006.000 
			String[] parcelNoParts = parcelNo.split("\\s*-\\s*");
			if (parcelNoParts.length == 6){
				StringBuffer parcelNoBuff = new StringBuffer(parcelNoParts[0]);
				parcelNoBuff.append("-").append(parcelNoParts[1])
						 .append("-").append(StringUtils.leftPad(parcelNoParts[2], 2, '0'))
						 .append("-").append(parcelNoParts[3])
						 .append("-").append(StringUtils.leftPad(parcelNoParts[4], 3, '0'))
						 .append("-").append(StringUtils.leftPad(parcelNoParts[5], 7, '0'));
				parcelNo = parcelNoBuff.toString();
			}
			
			if (isInstrumentSaved(parcelNo, null, data)) {
				checkbox = "saved";
			} else {
				String linkToPDF = link + accountNumber;
				checkbox = "<input type='checkbox' name='docLink' value='" + linkToPDF + "'>";
				mSearch.addInMemoryDoc(linkToPDF, currentResponse);
				currentResponse.setPageLink(new LinkInPage(linkToPDF, originalLink, TSServer.REQUEST_SAVE_TO_TSD));
			}
				
			currentHtmlRow = currentHtmlRow.replaceAll("<td width = \"2%\"></td>", "<td width = \"2%\">" + checkbox + "</td>");
			currentResponse.setAttribute(ParsedResponse.SERVER_ROW_RESPONSE, currentHtmlRow);
			currentResponse.setOnlyResponse(currentHtmlRow);


			ResultMap resultMap = new ResultMap();

			finalHtml.append(currentHtmlRow);
			String name = currentRecordValues.get("Name");
			String address = ro.cst.tsearch.utils.StringUtils.cleanHtml(currentRecordValues.get("Address"));
			resultMap.put("PropertyIdentificationSet.ParcelID", prepareAccountNumber(currentHtmlRow));
			resultMap.put("PropertyIdentificationSet.NameOnServer", name);
			resultMap.put("PropertyIdentificationSet.AddressOnServer", address);
			resultMap.put("TaxHistorySet.Year", currentYear);
			resultMap.put("OtherInformationSet.SrcType", "TR");

			// if (getSearch().getSearchType() == Search.AUTOMATIC_SEARCH ) {
			// ro.cst.tsearch.servers.functions.MOPlatteTR.getInstance().parseName(name,
			// resultMap);
			// }
			ro.cst.tsearch.servers.functions.MOPlatteTR.getInstance().parseAddress(address, resultMap);
			ro.cst.tsearch.servers.functions.MOPlatteTR.getInstance().parseName(name, resultMap);
			Bridge bridge = new Bridge(currentResponse, resultMap, searchId);
			DocumentI document = null;
			try {
				document = bridge.importData();
			} catch (Exception e) {
				e.printStackTrace();
			}
			currentResponse.setDocument(document);
			intermediaryResponse.add(currentResponse);
		}

		ParsedResponse parsedResponse = response.getParsedResponse();
		parsedResponse.setResponse(finalHtml.toString());
		parsedResponse.setResultRows(intermediaryResponse);
		parsedResponse.setHeader("<table>" + tableHeader);

		String header = parsedResponse.getHeader();
		String footer = parsedResponse.getFooter();

		header += CreateSaveToTSDFormHeader(URLConnectionReader.MULTIPLE_SAVE_TO_TSD_ACTION, "POST");
		header = header.replaceFirst("<td width = \"2%\"></td>", "<td width = \"2%\"><div >" + SELECT_ALL_CHECKBOXES + "</div></td>");

		footer = "\n</table>\n" + aoMess + "<br/>" + CreateSaveToTSDFormEnd(SAVE_DOCUMENT_BUTTON_LABEL, viParseID, -1);

		
		parsedResponse.setHeader(header);
		parsedResponse.setFooter(footer);
		finalHtml.insert(0, header);
		finalHtml.append(footer);
//		super.detailsCasesParse(action, response, viParseID, finalHtml.toString(), accountNumber);
		return newTable;
	}

	protected void saveTestDataToFiles(ResultMap map) {
		if (Utils.isJvmArgumentTrue("debugForATSProgrammer")) {
			// test
			String name = "" + map.get("PropertyIdentificationSet.NameOnServer");
			String text = name + "\r\n" + map.get("PropertyIdentificationSet.AddressOnServer") + "\r\n\r\n\r\n";

			String path = "D:\\work\\" + this.getClass().getSimpleName() + "\\";
			ro.cst.tsearch.utils.FileUtils.appendToTextFile(path + "name_address.txt", text);

			String uniqueIdentifier = map.get("PropertyIdentificationSet.ParcelID").toString().replaceAll("-|\\.", "");

			String legal = StringUtils.defaultIfEmpty((String) map.get("PropertyIdentificationSet.LegalDescriptionOnServer"), "");

			text = uniqueIdentifier + "\r\n" + legal + "\r\n\r\n\r\n";

			ro.cst.tsearch.utils.FileUtils.appendToTextFile(path + "legal_description.txt", text);

			text = (String) map.get("tmpPdf");
			if (StringUtils.isNotEmpty(text)) {
				path = "D:\\work\\" + this.getClass().getSimpleName() + "\\";

				ro.cst.tsearch.utils.FileUtils.appendToTextFile(path + uniqueIdentifier + ".txt", text);
			}

			// end test
		}
	}

	@Override
	public Collection<ParsedResponse> smartParseIntermediary(ServerResponse response, String table, StringBuilder outputTable) {
		MessageFormat link = null;
		return ro.cst.tsearch.servers.functions.MOPlatteTR.getInstance().parseIntermediary(response, table, searchId, link);
	}

	@Override
	protected Object parseAndFillResultMap(ServerResponse response, String detailsHtml, ResultMap map) {
		HtmlParser3 parser = new HtmlParser3(detailsHtml.trim());

		NodeList list = parser.getNodeList();
		if (list.size() >= 1) {
			Node row = list.elementAt(0);
			if (row instanceof TableRow) {
				SimpleNodeIterator elements = row.getChildren().elements();
				int i = 0;
				String year = "";
				String name = "";
				String address = "";
				String parcelID = "";
				String accountNumber = getAccountNumber(detailsHtml);
				while (elements.hasMoreNodes() && (i <= 4)) {
					Node nextNode = elements.nextNode();
					if (nextNode instanceof TableColumn) {
						if (i == 1) {
							year = nextNode.getChildren().toHtml().trim();
						}
						if (i == 2) {
							parcelID = nextNode.getChildren().toHtml().trim();
						}
						if (i == 3) {
							name = nextNode.getChildren().toHtml().trim();
						}
						if (i == 4) {
							address = nextNode.getChildren().toHtml().trim();
						}
						i++;
					}
				}

				map.put(PropertyIdentificationSetKey.PARCEL_ID.getKeyName(), parcelID);
				map.put(PropertyIdentificationSetKey.NAME_ON_SERVER.getKeyName(), name.replaceAll("&amp;", "&"));
				map.put(PropertyIdentificationSetKey.ADDRESS_ON_SERVER.getKeyName(), address);
				map.put(TaxHistorySetKey.YEAR.getKeyName(), year);

				String pdfText = getPDF(response, map, accountNumber, false);

				ro.cst.tsearch.servers.functions.MOPlatteTR atsParser = ro.cst.tsearch.servers.functions.MOPlatteTR.getInstance();
				
				atsParser.parseName(name, map);
				atsParser.parseAddress(address, map);

				atsParser.setSecTwnRng(pdfText, map);
				atsParser.setTaxData(pdfText, map);
				parseTaxes(map, accountNumber, pdfText);
				atsParser.setAppraisalData(pdfText, map);
				atsParser.parseLegalDescription(pdfText, map);
				
				String result = "";
				String tableTaxes = (String) map.get("tmpTableTaxes");
				if(tableTaxes != null) {
					result += tableTaxes + "<br>";
				}
				String tablePreviousDelinquent = (String) map.get("tmpTablePreviousDelinquent");
				if (tablePreviousDelinquent!=null) {
					result += tablePreviousDelinquent;
				}
				response.setResult(result);
				
				parcelID = RegExUtils.getFirstMatch("(?is)PARCEL ID#:\\s*([0-9\\.\\s\\-]+)\n", pdfText, 1);
				if (ro.cst.tsearch.utils.StringUtils.isNotEmpty(parcelID)){
					map.put(PropertyIdentificationSetKey.PARCEL_ID.getKeyName(), parcelID);
				}
				// atsParser.s
			}
		}

		return null;
	}
	
	/** 
	 * this method creates the tables requested in Task 7737 and Task 7678
	 */
	public void parseTaxes(ResultMap resultMap, String accountNumber, String pdfText) {
		String delinquentYears = (String) resultMap.get("tmpDelinquentYears");
		boolean delinquentYearsPresent = false;
		
		StringBuilder tablePreviousDelinquent = new StringBuilder();
		tablePreviousDelinquent.append("Delinquent Years:</br>")
			.append("<table border=\"1\"><tr><th>Tax Year</th><th>Base Amount</th><th>Amount Due</th><th>Notes</th></tr>");
		
		StringBuilder tableTaxes = new StringBuilder();
		tableTaxes.append("Tax Information:</br>")
			.append("<table border=\"1\"><tr><th>Tax Year</th><th>Base Amount</th><th>Amount Paid</th>" +
					"<th>Paid On</th><th>Receipt#</th><th>Amount Due</th><th>Prior Delinquent</th></tr>");
		
		int currentYear = 0;
		try {
			currentYear = Integer.parseInt((String) resultMap.get(TaxHistorySetKey.YEAR.getKeyName()));
		} catch (NumberFormatException e1) {
			return;
		}
		
		String delinquentAmount = "0.0";
		LinkedList<String[]> history = new LinkedList<String[]>();
		
		for(int year = currentYear;; year--) {
			String correctAccountNumber = accountNumber.replaceFirst("\\d{4}$", String.valueOf(year));
			String yearText = "";
			
			if(year == currentYear) {
				yearText = pdfText;
			} else {
				try {
					yearText = getPDF(null, null, correctAccountNumber, true);
				} catch (Exception e) {
					//the pdf could not be obtained
					break;
				}
			}
			
			if (!StringUtils.isEmpty(yearText)) {
				ResultMap tmpMap = new ResultMap();
				ro.cst.tsearch.servers.functions.MOPlatteTR atsParser = ro.cst.tsearch.servers.functions.MOPlatteTR.getInstance();
				atsParser.setTaxData(yearText, tmpMap);
				
				String[] record = new String[6];
				record[0] = String.valueOf(year);
				record[1] = (String) tmpMap.get("TaxHistorySet.BaseAmount");
				record[2] = (String) tmpMap.get("TaxHistorySet.AmountPaid");
				record[3] = (String) tmpMap.get("TaxHistorySet.DatePaid");
				record[4] = (String) tmpMap.get("TaxHistorySet.ReceiptNumber");
				record[5] = (String) tmpMap.get("TaxHistorySet.TotalDue");
				history.addFirst(record);
			} else {
				break;
			}
		}
		
		for(String[] record : history) {
			String year = record[0] != null ? record[0] : "-";
			String baseAmount = record[1] != null ? record[1] : "-";
			String amountPaid = record[2] != null ? record[2] : "-";
			String datePaid = record[3] != null ? record[3] : "-";
			String receiptNo = record[4] != null ? record[4] : "-";
			String amountDue = record[5] != null ? record[5] : "-";
			
			tableTaxes.append("<tr><td>" + year + "</td>");
			tableTaxes.append("<td>" + baseAmount + "</td>");
			tableTaxes.append("<td>" + amountPaid + "</td>");
			tableTaxes.append("<td>" + datePaid + "</td>");
			tableTaxes.append("<td>" + receiptNo + "</td>");
			tableTaxes.append("<td>" + amountDue + "</td>");
			tableTaxes.append("<td>" + GenericFunctions.sum(delinquentAmount, searchId) + "</td></tr>");
			
			if (delinquentYears.contains(year)) {
				delinquentAmount += "+" + amountDue;
				delinquentYearsPresent = true;
				tablePreviousDelinquent.append("<tr><td>"+year+"</td>");
				tablePreviousDelinquent.append("<td>"+baseAmount.replaceAll(",", "")+"</td>");
				tablePreviousDelinquent.append("<td>"+amountDue.replaceAll(",", "")+"</td><td>DLQ</td></tr>");
			}
		}
		
		delinquentAmount = GenericFunctions.sum(delinquentAmount, searchId);
		resultMap.put(TaxHistorySetKey.PRIOR_DELINQUENT.getKeyName(), delinquentAmount);
		
		tableTaxes.append("</table>");
		resultMap.put("tmpTableTaxes", tableTaxes.toString());
		
		if (delinquentYearsPresent) {
			tablePreviousDelinquent.append("</table>");
			resultMap.put("tmpTablePreviousDelinquent", tablePreviousDelinquent.toString());
		}
	}

	@Override
	protected String cleanDetails(String response) {
		return response;
	}

	protected String addInfoToDetailsPage(ServerResponse response, String serverResult, int viParseID) {
		return serverResult;
	}

	@Override
	protected void saveCasesParse(ServerResponse response, String serverResult, String filename) {
		super.saveCasesParse(response, serverResult, filename);
		String cleanedResponse = serverResult.replaceAll("<td widt.*checkbox.*?</td>", "");
		cleanedResponse = cleanedResponse.replaceAll("(?is)<a.*?document.*?</a>", "");
		cleanedResponse = "<table>" + cleanedResponse + "</table>";
		
		String tablePreviousDelinquent = response.getResult();
		if (!StringUtils.isEmpty(tablePreviousDelinquent)) {
			cleanedResponse += "</br>" + tablePreviousDelinquent;
		}

		response.getParsedResponse().getAttribute(ParsedResponse.SERVER_ROW_RESPONSE);
		response.getParsedResponse().setAttribute(ParsedResponse.SERVER_ROW_RESPONSE, cleanedResponse);
		response.getParsedResponse().setResponse(cleanedResponse);
		response.setResult(cleanedResponse);

	}

	@SuppressWarnings("deprecation")
	@Override
	protected DownloadImageResult saveImage(ImageLinkInPage image) throws ServerResponseException {

		String link = image.getLink();
		Page dataSheet = HtmlElementHelper.getHtmlPageByURL(link);
		byte[] contentAsBytes = dataSheet.getWebResponse().getContentAsBytes();
		afterDownloadImage();
		ServerResponse resp = new ServerResponse();

		String imageName = image.getPath();
		if (FileUtils.existPath(imageName)) {
			contentAsBytes = FileUtils.readBinaryFile(imageName);
			return new DownloadImageResult(DownloadImageResult.Status.OK, contentAsBytes, image.getContentType());
		}

		resp.setImageResult(new DownloadImageResult(DownloadImageResult.Status.OK, contentAsBytes, ((ImageLinkInPage) image)
				.getContentType()));

		DownloadImageResult dres = resp.getImageResult();
		// System.out.println("image");

		return dres;
	}

	protected String getPDF(ServerResponse response, ResultMap map, String accountNumber, boolean onlyPDF) {
		// get the PDF
		ImageLinkInPage imageLink = new ImageLinkInPage(false);
		imageLink.setContentType("application/pdf");
		String pdfUrl = ro.cst.tsearch.connection.http2.MOPlatteTR.PDF_URL + accountNumber;
		imageLink.setLink(pdfUrl);
		imageLink.setPath(accountNumber + ".pdf");

		// response.getParsedResponse().addImageLink(new ImageLinkInPage(pdfUrl,
		// accountNumber + ".pdf"));

		ByteArrayOutputStream bas = new ByteArrayOutputStream();
		HttpSite site = HttpManager.getSite(getCurrentServerName(), searchId);
		byte[] pdfPage = null;
		try {
			if (StringUtils.isNotEmpty(accountNumber)) {
				pdfPage = ((ro.cst.tsearch.connection.http2.MOPlatteTROld) site).getPDFDocument(accountNumber);
			}
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			HttpManager.releaseSite(site);
		}
		if (pdfPage.length > 0 && pdfPage.length != 93) { //93 is the length of the response when there is no PDF
			
			if (!onlyPDF) {
				// get the link in order to make the fileName
				response.getParsedResponse().addImageLink(imageLink);
			}
			
			String fileName = accountNumber.replaceAll("-|\\.", "");
			String filePath = getImagePath() + ".pdf"; // getSearch().getSearchDir()
														// + File.separator +
														// fileName + ".pdf";

			boolean createNewFile = false;

			try {
				
				if(ro.cst.tsearch.utils.FileUtils.createDirectory(getImageDirectory())) {
					createNewFile = new File(filePath).createNewFile();
				}
			} catch (IOException e1) {
				e1.printStackTrace();
			}

			byte[] contentAsBytes = pdfPage;

			InputStream stream = new ByteArrayInputStream(contentAsBytes);
			ro.cst.tsearch.utils.FileUtils.writeStreamToFile(stream, filePath);
			imageLink.setPath(filePath);
			imageLink.setDownloadStatus("DOWNLOAD_OK");
			imageLink.setSolved(true);
			imageLink.setImageFileName(fileName + ".pdf");

			Writer output = null;
			PDDocument document = null;
			try {
				document = PDDocument.load(filePath);
				output = new OutputStreamWriter(bas);

				PDFTextStripper stripper = new PDFTextStripper();
				stripper.getTextMatrix();
				// PDFTextStripperByArea textStripperByArea = new
				// PDFTextStripperByArea();
				org.pdfbox.util.PDFText2HTML textStripperByArea = new org.pdfbox.util.PDFText2HTML();
				// textStripperByArea.extractRegions()
				stripper.setWordSeparator("     ");

				stripper.setLineSeparator("\n");
				stripper.setPageSeparator("\n");

				// do not change this. will blow up the output of the pdf and
				// the
				// parsing of him will fail
				stripper.setSortByPosition(true);
				// PDFText2HTML stripper = new PDFText2HTML();

				stripper.writeText(document, output);
				
				if (!onlyPDF) {
					map.put("tmpPdf", bas.toString());
				}
				

			} catch (IOException e) {
				e.printStackTrace();
			} finally {
				if (output != null) {
					try {
						output.close();
					} catch (IOException e) {
						e.printStackTrace();
					}
				}
				if (document != null) {
					try {
						document.close();
					} catch (IOException e) {
						e.printStackTrace();
					}
				}
			}
		}
		return bas.toString();
	}

	protected void setModulesForAutoSearch(TSServerInfo serverInfo) {
		ArrayList<TSServerInfoModule> modules = new ArrayList<TSServerInfoModule>();

		TSServerInfoModule module = null;
		PinFilterResponse pinFilter = PINFilterFactory.getDefaultPinFilter(searchId);
		FilterResponse addressFilter = AddressFilterFactory.getAddressHybridFilter(searchId, 0.7d);
		FilterResponse nameFilterHybrid = NameFilterFactory.getHybridNameFilter(SearchAttributes.OWNER_OBJECT, searchId, null);
		TaxYearFilterResponse yearFilter = new TaxYearFilterResponse(searchId);
		yearFilter.setThreshold(new BigDecimal("0.95"));

		if (hasOwner()) {
			module = new TSServerInfoModule(serverInfo.getModule(TSServerInfo.NAME_MODULE_IDX));
			module.clearSaKeys();
			module.setSaObjKey(SearchAttributes.OWNER_OBJECT);

			if (hasPin()) {
				module.addFilter(pinFilter);
				module.addFilter(yearFilter);
			} else {
				module.addFilter(addressFilter);
				module.addFilter(nameFilterHybrid);
				module.addFilter(yearFilter);
			}

			module.setIteratorType(1, FunctionStatesIterator.ITERATOR_TYPE_LCF_NAME_FAKE);

			ConfigurableNameIterator nameIterator = (ConfigurableNameIterator) ModuleStatesIteratorFactory.getConfigurableNameIterator(
					module, searchId, new String[] { "L; F M;", "L; f m;", "L; f;", "L; m;" });
			module.addIterator(nameIterator);
			modules.add(module);
		}
		serverInfo.setModulesForAutoSearch(modules);
	}

}
