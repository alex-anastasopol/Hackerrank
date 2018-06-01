package ro.cst.tsearch.servers.types;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Set;
import java.util.Vector;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang.StringEscapeUtils;
import org.htmlparser.Node;
import org.htmlparser.filters.RegexFilter;
import org.htmlparser.filters.TagNameFilter;
import org.htmlparser.tags.LinkTag;
import org.htmlparser.tags.TableColumn;
import org.htmlparser.tags.TableRow;
import org.htmlparser.tags.TableTag;
import org.htmlparser.util.NodeList;

import ro.cst.tsearch.Search;
import ro.cst.tsearch.bean.SearchAttributes;
import ro.cst.tsearch.connection.http3.HttpManager3;
import ro.cst.tsearch.connection.http3.HttpSite3;
import ro.cst.tsearch.exceptions.ServerResponseException;
import ro.cst.tsearch.extractor.xml.Bridge;
import ro.cst.tsearch.extractor.xml.ResultMap;
import ro.cst.tsearch.generic.Util;
import ro.cst.tsearch.parser.HtmlParser3;
import ro.cst.tsearch.search.FunctionStatesIterator;
import ro.cst.tsearch.search.ModuleStatesIterator;
import ro.cst.tsearch.search.filter.BetweenDatesFilterResponse;
import ro.cst.tsearch.search.filter.ExactDateFilterResponse;
import ro.cst.tsearch.search.filter.FilterResponse;
import ro.cst.tsearch.search.filter.RejectAlreadySavedDocumentsForUpdateFilter;
import ro.cst.tsearch.search.filter.newfilters.date.LastTransferDateFilter;
import ro.cst.tsearch.search.filter.newfilters.doctype.DoctypeFilterFactory;
import ro.cst.tsearch.search.filter.newfilters.legal.LegalFilterFactory;
import ro.cst.tsearch.search.filter.newfilters.misc.CrossReferenceToInvalidatedFilter;
import ro.cst.tsearch.search.filter.newfilters.name.GenericNameFilter;
import ro.cst.tsearch.search.filter.newfilters.name.NameFilterFactory;
import ro.cst.tsearch.search.filter.newfilters.name.SubdivisionFilter;
import ro.cst.tsearch.search.iterator.ModuleStatesIteratorFactory;
import ro.cst.tsearch.search.iterator.instrument.InstrumentGenericIterator;
import ro.cst.tsearch.search.module.ConfigurableNameIterator;
import ro.cst.tsearch.search.module.OcrOrBootStraperIterator;
import ro.cst.tsearch.search.validator.DocsValidator;
import ro.cst.tsearch.servers.TSConnectionURL;
import ro.cst.tsearch.servers.info.TSServerInfo;
import ro.cst.tsearch.servers.info.TSServerInfoConstants;
import ro.cst.tsearch.servers.info.TSServerInfoModule;
import ro.cst.tsearch.servers.response.ImageLinkInPage;
import ro.cst.tsearch.servers.response.ImageTransformation;
import ro.cst.tsearch.servers.response.LinkInPage;
import ro.cst.tsearch.servers.response.OtherInformationSet.OtherInformationSetKey;
import ro.cst.tsearch.servers.response.ParsedResponse;
import ro.cst.tsearch.servers.response.SaleDataSet.SaleDataSetKey;
import ro.cst.tsearch.servers.response.ServerResponse;
import ro.cst.tsearch.servlet.URLConnectionReader;
import ro.cst.tsearch.titledocument.abstracts.DocumentTypes;
import ro.cst.tsearch.utils.FileUtils;
import ro.cst.tsearch.utils.SearchLogger;
import ro.cst.tsearch.utils.StringUtils;
import ro.cst.tsearch.utils.Tidy;

import com.stewart.ats.base.document.DocumentI;
import com.stewart.ats.base.document.ImageI;
import com.stewart.ats.base.document.InstrumentI;
import com.stewart.ats.base.document.RegisterDocument;
import com.stewart.ats.base.document.RegisterDocumentI;
import com.stewart.ats.base.document.RestoreDocumentDataI;
import com.stewart.ats.base.name.NameI;
import com.stewart.ats.base.search.DocumentsManagerI;

public class OHFairfieldRO extends TSServerROLike {
	private static final long	serialVersionUID		= 4736334948695655463L;
	String						pdfContentType			= "application/pdf";

	// pattern contains O because for ex. instr 200000011618 has mortgage value $150,0O0,00
	String						amountPattern			= "(?i).*?(\\$\\s*[\\d+\\.,O]+)";

	public OHFairfieldRO(long searchId) {
		super(searchId);
		resultType = MULTIPLE_RESULT_TYPE;
	}

	public OHFairfieldRO(String rsRequestSolverName, String rsSitePath, String rsServerID, String rsPrmNameLink, long searchId, int miServerID) {
		super(rsRequestSolverName, rsSitePath, rsServerID, rsPrmNameLink, searchId, miServerID);
		resultType = MULTIPLE_RESULT_TYPE;
	}

	@Override
	public Object getRecoverModuleFrom(RestoreDocumentDataI restoreDocumentDataI) {
		if (restoreDocumentDataI == null) {
			return null;
		}
		List<TSServerInfoModule> list = new ArrayList<TSServerInfoModule>();
		return list;
	}

	protected void ParseResponse(String sAction, ServerResponse Response, int viParseID) throws ServerResponseException {

		String rsResponse = Response.getResult();
		ParsedResponse parsedResponse = Response.getParsedResponse();

		if (rsResponse.contains("No record found for this criteria or your search has timed out")) {
			Response.getParsedResponse().setError(
					"No record found for this criteria or your search has timed out, please narrow down your search criteria and try again.");
			return;
		}

		switch (viParseID) {
		case ID_SEARCH_BY_NAME:
		case ID_SEARCH_BY_INSTRUMENT_NO:
		case ID_SEARCH_BY_BOOK_AND_PAGE:
		case ID_SEARCH_BY_MODULE38:
		case ID_SEARCH_BY_MODULE39:
		case ID_SEARCH_BY_MODULE40:
		case ID_INTERMEDIARY:
			StringBuilder outputTable = new StringBuilder();
			Collection<ParsedResponse> smartParsedResponses = smartParseIntermediary(Response, rsResponse, outputTable);

			if (smartParsedResponses.size() > 0) {
				parsedResponse.setResultRows(new Vector<ParsedResponse>(smartParsedResponses));
				parsedResponse.setOnlyResponse(outputTable.toString());
				parsedResponse.setAttribute(ParsedResponse.SERVER_ROW_RESPONSE, outputTable.toString());
			}
			if (smartParsedResponses.size() == 0) {
				return;
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
			HtmlParser3 htmlParser3 = new HtmlParser3(rsResponse);
			Node detailsNode = htmlParser3.getNodeById("accordion_detail");

			String details = getDetails(Response, rsResponse);

			htmlParser3 = new HtmlParser3(details);
			String instrumentNumber = getValueDetailsTable1(details, "Number");

			String documentType = getValueDetailsTable2(details, "Type");
			String book = getValueDetailsTable2(details, "Volume").replaceAll("^0+", "");
			String page = getValueDetailsTable2(details, "Page").replaceAll("^0+", "");

			String imageFileName = instrumentNumber + ".pdf";
			addImageLink(parsedResponse, details, imageFileName);

			if (viParseID != ID_SAVE_TO_TSD) {

				String originalLink = sAction.replace("?", "&") + "&" + Response.getRawQuerry();
				String sSave2TSDLink = getLinkPrefix(TSConnectionURL.idPOST) + originalLink;

				HashMap<String, String> data = new HashMap<String, String>();
				data.put("dataSource", getDataSite().getSiteTypeAbrev());

				if (!documentType.isEmpty()) {
					data.put("type", documentType);
				}

				if (!instrumentNumber.isEmpty()) {
					data.put("year", instrumentNumber.substring(0, 4));

					instrumentNumber = instrumentNumber.substring(4).replaceAll("^0+", "");
					data.put("instrno", instrumentNumber);
				}
				if (!book.isEmpty()) {
					data.put("book", book);
				}
				if (!page.isEmpty()) {
					data.put("page", page);
				}

				if (isInstrumentSaved(instrumentNumber, null, data, false)) {
					details += CreateFileAlreadyInTSD();
				} else {
					mSearch.addInMemoryDoc(sSave2TSDLink, details);
					details = addSaveToTsdButton(details, sSave2TSDLink, ID_DETAILS);
				}

				parsedResponse.setPageLink(new LinkInPage(sSave2TSDLink, originalLink, TSServer.REQUEST_SAVE_TO_TSD));
				parsedResponse.setResponse(details);

			} else {
				smartParseDetails(Response, details);

				// remove image link
				details = details.replaceFirst("(?is)<tr\\s+id=\\\"pdfImageLinkRow\\\"[^>]*>.*?</tr>", "");

				msSaveToTSDFileName = instrumentNumber + ".html";
				parsedResponse.setFileName(getServerTypeDirectory() + msSaveToTSDFileName);
				parsedResponse.setResponse(details);

				msSaveToTSDResponce = details + CreateFileAlreadyInTSD();
			}

			break;
		case ID_GET_LINK:
			htmlParser3 = new HtmlParser3(rsResponse);
			detailsNode = htmlParser3.getNodeById("accordion_detail");
			if (detailsNode != null) {
				ParseResponse(sAction, Response, ID_DETAILS);
			}
			else {
				ParseResponse(sAction, Response, ID_INTERMEDIARY);
			}
			break;
		default:
			break;
		}
	}

	@SuppressWarnings("deprecation")
	private void addImageLink(ParsedResponse parsedResponse, String details, String imageFileName) {

		if (details.indexOf("pdfImageLinkRow") != -1) {// the id of the image link row

			int imageLinkIndex = details.indexOf(CreatePartialLink(TSConnectionURL.idGET));

			if (imageLinkIndex >= 0) {
				String imgLink = details.substring(imageLinkIndex, details.indexOf("\"", imageLinkIndex));

				ImageLinkInPage imageLinkInPage = new ImageLinkInPage(imgLink, imageFileName);
				parsedResponse.addImageLink(imageLinkInPage);
			}
		}
	}

	private String getValueDetailsTable1(String details, String columnName) {

		String value = "";
		details = details.replaceAll("(?is)</?(?:font|b)[^>]*>", "").replaceAll("(?is)(</?t)h", "$1d");

		HtmlParser3 htmlParser3 = new HtmlParser3(details);
		NodeList nodeList = htmlParser3.getNodeList();
		Node firstTable = HtmlParser3.getNodeByTypeAttributeDescription(nodeList, "table", "", "",
				new String[] { "Number", "File Date", "Inst. Date" }, true);

		if (firstTable != null) {
			value = HtmlParser3.getValueFromAbsoluteCell(1, 0, HtmlParser3.findNode(firstTable.getChildren(), columnName), "", true)
					.trim();
		}

		return value;
	}

	private String getValueDetailsTable2(String details, String columnName) {

		String docType = "";
		details = details.replaceAll("(?is)</?(?:font|b)[^>]*>", "").replaceAll("(?is)(</?t)h", "$1d");

		HtmlParser3 htmlParser3 = new HtmlParser3(details);
		NodeList nodeList = htmlParser3.getNodeList();
		Node docTypeTable = HtmlParser3.getNodeByTypeAttributeDescription(nodeList, "table", "", "",
				new String[] { "Type", "Volume", "Page" }, true);

		if (docTypeTable != null) {
			docType = HtmlParser3.getValueFromAbsoluteCell(1, 0, HtmlParser3.findNode(docTypeTable.getChildren(), columnName), "", true)
					.replaceAll("[\\)\\(]", "")
					.trim();
		}

		return docType;
	}

	@Override
	public Collection<ParsedResponse> smartParseIntermediary(
			ServerResponse response, String table, StringBuilder outputTable) {
		Vector<ParsedResponse> intermediaryResponse = new Vector<ParsedResponse>();

		try {
			int numberOfUncheckedElements = 0;

			table = Tidy.tidyParse(table, null);
			table = StringEscapeUtils.unescapeHtml(table).replaceAll("[^\\p{Alnum}\\p{Punct}\\s]", "");

			HtmlParser3 htmlParser3 = new HtmlParser3(table);
			NodeList nodeList = htmlParser3.getNodeList();

			Node row1 = htmlParser3.getNodeById("myRow1");
			String header = "";
			Node pagingTopDiv = nodeList.extractAllNodesThatMatch(new RegexFilter("Result\\s+Matches"), true).elementAt(0);

			if (pagingTopDiv != null) {
				pagingTopDiv = HtmlParser3.getFirstParentTag(pagingTopDiv, TableRow.class);
			}

			int columnCount = 0;
			if (row1 != null) {
				Node mainTable = HtmlParser3.getFirstParentTag(row1, TableTag.class);

				TableTag tableTag = (TableTag) mainTable;
				TableRow[] rows = tableTag.getRows();

				if (rows[0] != null) {
					header = rows[0].toHtml().replaceAll("</?tr>", "").replaceFirst("(?is)<th[^>]*>\\s*</th>", "").trim();
				}

				for (int i = 1; i < rows.length; i++) {
					
					boolean isAlreadySaved = false;
					
					TableRow row = rows[i];
					columnCount = row.getColumnCount();
					if (columnCount >= 3) {// there are two levels of intermediaries
						LinkTag linkTag = (LinkTag) row.getColumns()[1].getFirstChild();
						String link = linkTag.extractLink().trim().replaceAll("\\s", "%20");
						link = CreatePartialLink(TSConnectionURL.idGET) + link;
						linkTag.setLink(link);
						String htmlRow = row.toHtml();

						// remove first column(it's empty) for both levels of interm.
						htmlRow = htmlRow.replaceFirst("(?is)(<tr[^>]*>.*?)<td[^>]*>.*?</td>(.*)", "$1$2");

						ParsedResponse currentResponse = new ParsedResponse();

						int action = TSServer.REQUEST_SAVE_TO_TSD;
						if (columnCount <= 3) {
							action = TSServer.REQUEST_GO_TO_LINK_REC;
						}

						ResultMap resultMap = ro.cst.tsearch.servers.functions.OHFairfieldRO.parseIntermediaryRow(htmlRow);
						resultMap.put(OtherInformationSetKey.SRC_TYPE.getKeyName(), getDataSite().getSiteTypeAbrev());

						Bridge bridge = new Bridge(currentResponse, resultMap, searchId);

						RegisterDocumentI document = (RegisterDocumentI) bridge.importData();
						currentResponse.setDocument(document);
						
						// checkboxes
						if (columnCount >= 8 || columnCount == 5) {// for second level intermediaries only
							String checkBox = "checked";

							String instrNo = org.apache.commons.lang.StringUtils.defaultString((String) resultMap.get(SaleDataSetKey.INSTRUMENT_NUMBER
									.getKeyName())).replaceAll("\\s+", "").replaceAll("^\\d{2}0+", "");
							String recDate = org.apache.commons.lang.StringUtils.defaultString((String) resultMap.get(SaleDataSetKey.RECORDED_DATE
									.getKeyName())).replaceAll("\\s+", "");
							String book = org.apache.commons.lang.StringUtils.defaultString((String) resultMap.get(SaleDataSetKey.BOOK
									.getKeyName())).replaceAll("^0+", "");
							String page = org.apache.commons.lang.StringUtils.defaultString((String) resultMap.get(SaleDataSetKey.PAGE
									.getKeyName())).replaceAll("^0+", "");
							HashMap<String, String> data = new HashMap<String, String>();

							if (recDate.length() >= 4) {
								String year = recDate.substring(recDate.length() - 4, recDate.length());
								data.put("year", year);
							}

							data.put("instrno", instrNo);
							String docType = org.apache.commons.lang.StringUtils.defaultString((String) resultMap.get(SaleDataSetKey.DOCUMENT_TYPE
									.getKeyName()));

							if (!docType.isEmpty()) {
								data.put("type", docType);
							}
							if (!book.isEmpty()) {
								data.put("book", book);
							}
							if (!page.isEmpty()) {
								data.put("page", page);
							}

							if (isInstrumentSaved(instrNo, document, data, false)) {
								isAlreadySaved=true;
								checkBox = "saved";
							} else {
								numberOfUncheckedElements++;
								checkBox = "<input type='checkbox' name='docLink' value='" + link + "'>";
							}

							htmlRow = "<tr><td align=\"center\">" + checkBox + "</td>" + htmlRow.replaceAll("(?is)</?tr[^>]*>", "") + "</tr>";
						}

						currentResponse.setAttribute(ParsedResponse.SERVER_ROW_RESPONSE, htmlRow);
						currentResponse.setOnlyResponse(htmlRow);
						if (!isAlreadySaved) {
							currentResponse.setPageLink(new LinkInPage(link, link, action));
						}
						
						intermediaryResponse.add(currentResponse);
					}
				}
			}

			String pagingTop = "";
			String pagingBottom = "";
			pagingTopDiv = nodeList.extractAllNodesThatMatch(new RegexFilter("Result\\s+Matches"), true).elementAt(0);

			if (pagingTopDiv != null) {
				pagingTopDiv = HtmlParser3.getFirstParentTag(pagingTopDiv, TableRow.class);

				pagingTop = processLinks(pagingTopDiv, response.getParsedResponse());

				Node pagingBottomDiv = HtmlParser3.getFirstParentTag(pagingTopDiv, TableTag.class).getParent();
				pagingBottomDiv = HtmlParser3.getFirstParentTag(pagingBottomDiv, TableTag.class).getParent().getLastChild().getPreviousSibling()
						.getPreviousSibling().getPreviousSibling();

				if (pagingBottomDiv != null) {
					pagingBottom = processLinks(pagingBottomDiv, response.getParsedResponse());
				}
			}

			response.getParsedResponse().setHeader(pagingTop + "<table border=\"1\" style=\"min-width:500px;\"><tr >"
					+ (columnCount == 3 ? "" : "<th>" + SELECT_ALL_CHECKBOXES + "</th>")
					+ header + "</tr>");
			response.getParsedResponse().setFooter(pagingBottom + "</table>");

			outputTable.append(table);
			SetAttribute(NUMBER_OF_UNSAVED_DOCUMENTS, numberOfUncheckedElements);

		} catch (Throwable t) {
			logger.error("Error while parsing intermediary data", t);
		}
		return intermediaryResponse;
	}

	private String processLinks(Node pagingDiv, ParsedResponse parsedResponse) {
		String paging;
		NodeList links = pagingDiv.getChildren().extractAllNodesThatMatch(new TagNameFilter("a"), true);
		int numberOfLinks = links.size();
		if (numberOfLinks > 0) {
			for (int i = 0; i < numberOfLinks; i++) {
				String linkTo = links.elementAt(i).toPlainTextString().replaceAll("[\\]\\[]", "").trim().toLowerCase();
				LinkTag linkTag = (LinkTag) links.elementAt(i);
				String link = CreatePartialLink(TSConnectionURL.idGET) + linkTag.getLink();
				linkTag.setLink(link);

				// set next link
				if (linkTo.equals("next")) {
					parsedResponse.setNextLink("<a href=\"" + link + "\">Next</a>");
				}
			}
		}

		paging = "<div>" + pagingDiv.toHtml().replaceAll("<table[^>]*>", "<table>") + "</div>";
		return paging;
	}

	protected String getDetails(ServerResponse serverResponse, String rsResponse) {

		// if from memory - use it as is
		if (!rsResponse.contains("<html")) {
			return rsResponse;
		}
		StringBuilder detailsSB = new StringBuilder();
		String pdfImageLink = "";
		try {
			rsResponse = StringEscapeUtils.unescapeHtml(rsResponse);
			rsResponse = Tidy.tidyParse(rsResponse, null);

			HtmlParser3 htmlParser3 = new HtmlParser3(rsResponse);

			Node detailsDiv = htmlParser3.getNodeById("detailsPanel");
			if (detailsDiv != null) {
				detailsSB.append("<table border=\"1\" align=\"center\" style=\"min-width:800px;\"><tr><td>" + detailsDiv.toHtml() + "</td></tr>");

				String documentType = getValueDetailsTable2(rsResponse, "Type");

				// if searching in UCC office type, there is no pdf link in details
				if (!documentType.equals("UCC") && !documentType.equals("TERM") && !documentType.equals("CPD") && !documentType.equals("PREL")
						&& !documentType.equals("AMEN")) {
					pdfImageLink = getImageLink(rsResponse);
				}

			}
		} catch (Exception e) {
			e.printStackTrace();
		}

		return detailsSB.toString().replaceAll("(?is)<a[^>]*>(.*?)</a>", "$1")
				.replaceAll("(?is)<input[^>]*>", "")
				.replaceAll("(?is)<img[^>]*>", "")
				.replaceAll("(?is)<h2>\\s*Loading\\s*</h2>", "")
				.replaceAll("(?is)<script[^>]*>.*?</script>", "")
				.replaceAll("(?is)<td[^>]*>\\s*</td>", "")
				.replaceAll("(?is)<tr[^>]*>\\s*</tr>", "")
				.replaceAll("(?is)(?is)(<font)[^>]*>", "$1>")
				+ pdfImageLink + "</table>";
	}

	private String getImageLink(String rsResponse) {
		String pdfImageLink = "";
		Pattern pdfViewPattern = Pattern.compile("(?is)<a[^>]*>\\s*PDF Viewer\\s*</a>");
		Matcher pdfViweMatcher = pdfViewPattern.matcher(rsResponse);
		if (pdfViweMatcher.find()) {
			// get PDF image link
			HtmlParser3 htmlParser3 = new HtmlParser3(rsResponse);
			Node formDetails = htmlParser3.getNodeById("frmdetail");

			if (formDetails.toHtml() != null) {
				// test to see it it's not an ucc details result
				NodeList propertyAssignedList = formDetails.getChildren().extractAllNodesThatMatch(new RegexFilter("Property\\s+Assigned"), true);
				if (propertyAssignedList.size() <= 0) {

					NodeList inputList = formDetails.getChildren().extractAllNodesThatMatch(new TagNameFilter("input"));
					int inputListSize = inputList.size();
					if (inputListSize > 0) {
						String formParams = "";

						for (int i = 0; i < inputListSize; i++) {
							Node input = inputList.elementAt(i);
							if (input != null) {
								formParams += input.toHtml().replaceFirst("(?is)<input[^>]*name=\"([^\"]*)\"[^>]*value=\"([^\"]*)\".*", "&$1=$2");
							}
						}

						Pattern pat = Pattern.compile("(?is)var\\s+url\\s*=\\s*'([^']*)'[^']*'([^']*?)&?'");
						Matcher mat = pat.matcher(rsResponse);

						String pdfLink = "";

						if (mat.find()) {
							pdfLink += CreatePartialLink(TSConnectionURL.idGET) + "ohlrf3/" + mat.group(1) + mat.group(2) + formParams;
							pdfImageLink = pdfLink;
						}

						pdfImageLink = "<tr id=\"pdfImageLinkRow\" align=\"center\"><td><a target=\"_blank\" href=\""
								+ pdfLink + "\" ><strong>View Image</strong></a></td></tr>";
					}
				}
			}
		}
		return pdfImageLink;
	}

	@Override
	protected Object parseAndFillResultMap(ServerResponse response, String detailsHtml, ResultMap resultMap) {
		try {
			detailsHtml = StringEscapeUtils.unescapeHtml(detailsHtml);
			detailsHtml = detailsHtml.replaceAll("(?is)</?(?:font|b)[^>]*>", "").replaceAll("(?is)(</?t)h", "$1d");

			HtmlParser3 htmlParser3 = new HtmlParser3(detailsHtml);
			NodeList nodeList = htmlParser3.getNodeList();

			String instrumentNumber = getValueDetailsTable1(detailsHtml, "Number");
			String recordedDate = getValueDetailsTable1(detailsHtml, "File Date").replaceFirst("(\\d+/\\d+/\\d+).*", "$1");

			if (instrumentNumber.length() >= 4) {
				resultMap.put(SaleDataSetKey.INSTRUMENT_NUMBER.getKeyName(), instrumentNumber.substring(4).replaceAll("^0+", ""));
			}
			if (!recordedDate.isEmpty()) {
				resultMap.put(SaleDataSetKey.RECORDED_DATE.getKeyName(), recordedDate);
			}

			Node docTypeTable = HtmlParser3.getNodeByTypeAttributeDescription(nodeList, "table", "", "",
					new String[] { "Type", "Volume", "Page" }, true);

			if (docTypeTable != null) {

				String documentType = "";
				String book = "";
				String page = "";

				documentType = getValueDetailsTable2(detailsHtml, "Type");
				book = getValueDetailsTable2(detailsHtml, "Volume");
				page = getValueDetailsTable2(detailsHtml, "Page");

				if (!documentType.isEmpty()) {
					resultMap.put(SaleDataSetKey.DOCUMENT_TYPE.getKeyName(), documentType);

				}
				if (!book.isEmpty()) {
					resultMap.put(SaleDataSetKey.BOOK.getKeyName(), StringUtils.removeLeadingZeroes(book));
				}
				if (!page.isEmpty()) {
					resultMap.put(SaleDataSetKey.PAGE.getKeyName(), StringUtils.removeLeadingZeroes(page));
				}
			}

			// remarks, free form: legal, cross-refs,mortgage/consideration amount
			NodeList remarksList = nodeList.extractAllNodesThatMatch(new RegexFilter("Remarks"), true);
			NodeList freeFormList = nodeList.extractAllNodesThatMatch(new RegexFilter("Free Form"), true);

			int freeFormListSize = freeFormList.size();

			int remarksListSize = remarksList.size();
			StringBuilder legalSB = new StringBuilder();

			if (freeFormListSize > 0) {

				for (int i = 0; i < freeFormListSize; i++) {

					Node freeFormNode = HtmlParser3.getFirstParentTag(freeFormList.elementAt(i), TableTag.class);
					TableRow[] freeFormRows = ((TableTag) freeFormNode).getRows();
					freeFormNode = freeFormRows[freeFormRows.length - 1];

					TableColumn freeFormColumn = ((TableRow) freeFormNode).getColumns()[0];
					String freeformContent = "";

					if (freeFormColumn != null) {
						freeformContent = freeFormColumn.toPlainTextString().trim();
						if (freeformContent.matches(amountPattern)) {
							freeformContent = parseAndRemoveAmount(resultMap, freeformContent);
						}
						if (!freeformContent.matches("(?is)NO\\s+PROPERTY\\s+SHOWN") && !freeformContent.matches("\\*"))
							legalSB.append(freeformContent + " ");
					}
				}
			}

			if (remarksListSize > 0) {
				legalSB.append(" @@@@ ");
				for (int i = 0; i < remarksListSize; i++) {
					Node remarksNode = HtmlParser3.getFirstParentTag(remarksList.elementAt(i), TableRow.class);

					if (remarksNode != null) {

						TableColumn[] remarkColumns = ((TableRow) remarksNode).getColumns();
						String remarkColContent = "";

						if (remarkColumns.length >= 2) {
							for (int j = 1; j < remarkColumns.length; j++) {
								remarkColContent = remarkColumns[j].toPlainTextString().trim();
								if (remarkColContent.matches(amountPattern)) {
									remarkColContent = parseAndRemoveAmount(resultMap, remarkColContent);
								}
								legalSB.append(remarkColContent + " ");
							}
						}
					}
				}
			}

			// legal
			ro.cst.tsearch.servers.functions.OHFairfieldRO.parseLegal(resultMap, legalSB.toString().trim());

			// parties
			Node partiesTable = HtmlParser3.getNodeByTypeAttributeDescription(nodeList, "table", "", "", new String[] { "Series", "Name" }, true);
			if (partiesTable != null && partiesTable instanceof TableTag) {

				int noOfParties = ((TableTag) partiesTable).getRowCount();

				StringBuilder grantors = new StringBuilder();
				StringBuilder grantees = new StringBuilder();

				if (noOfParties > 0) {
					for (int i = 0; i < noOfParties - 1; i++) {
						String party = HtmlParser3
								.getValueFromAbsoluteCell(i + 1, 0, HtmlParser3.findNode(partiesTable.getChildren(), "Name"), "", true)
								.replaceAll("[\\)\\(]", "").replaceAll("(?s)<.*?>", "").trim();
						String partyType = HtmlParser3
								.getValueFromAbsoluteCell(i + 1, 0, HtmlParser3.findNode(partiesTable.getChildren(), "Series"), "", true)
								.trim();

						if (partyType.contains("Grantor")) {
							grantors.append(party).append(" / ");
						}

						else if (partyType.contains("Grantee")) {
							grantees.append(party).append(" / ");
						}
					}
				}

				String grantorLF = grantors.toString().replaceFirst(" / $", "").trim();
				String granteeLF = grantees.toString().replaceFirst(" / $", "").trim();

				if (!StringUtils.isEmpty(grantorLF)) {
					resultMap.put("tmpGrantorLF", grantorLF);
				}

				if (!StringUtils.isEmpty(granteeLF)) {
					resultMap.put("tmpGranteeLF", granteeLF);
				}

				ro.cst.tsearch.servers.functions.OHFairfieldRO.parseNames(resultMap);
			}

		} catch (Exception e) {
			logger.error("Error while parsing details");
		}
		return null;
	}

	private String parseAndRemoveAmount(ResultMap resultMap, String remarkColContent) {

		try {
			Object docTypeObject = null;

			String docType = "";
			if ((docTypeObject = resultMap.get(SaleDataSetKey.DOCUMENT_TYPE.getKeyName())) != null) {
				docType = docTypeObject.toString();
			}

			String amount = remarkColContent.replaceFirst(amountPattern, "$1");
			remarkColContent = remarkColContent.replaceFirst("(CF)?\\s*" + Pattern.quote(amount), "").trim();

			amount = StringUtils.cleanAmount(amount);
			amount = amount.replaceAll("(?i)O", "0");

			if (docType.equalsIgnoreCase("Mortgage")) {
				resultMap.put(SaleDataSetKey.MORTGAGE_AMOUNT.getKeyName(), amount);
			}
			else {
				resultMap.put(SaleDataSetKey.CONSIDERATION_AMOUNT.getKeyName(), amount);
			}

		} catch (Exception e) {
			e.printStackTrace();
		}
		return remarkColContent.trim();
	}

	public static void processAllReferencedInstruments(DocumentI doc) {
		try {
			Set<InstrumentI> parsedRefs = doc.getParsedReferences();
			for (InstrumentI instr : parsedRefs) {
				processInstrumentNo(instr);
			}

		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public static void processInstrumentNo(InstrumentI instr) {
		try {
			String instrNo = instr.getInstno();
			Matcher m = Pattern.compile("(\\d{4})(\\d{4})(\\d+)").matcher(instrNo);
			if (m.find()) {
				int instYear = Integer.parseInt(m.group(1));
				if (instYear <= Calendar.getInstance().get(Calendar.YEAR)) {
					if (Util.isValidDate(m.group(1))) {
						if (m.group(3).trim().length() == 7)
							instr.setEnableInstrNoTailMatch(true);
					}
				}
			}

		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	protected void setModulesForAutoSearch(TSServerInfo serverInfo) {
		// 1.AO-like cross-referencess(book-page, instrument) - 2 modules
		// 2.Name-Recorded Land(owner, buyer), Name-ucc(owner, buyer)-> - 4 modules
		// 3.OCR: book-page, instrument, Recorded Land(owner) and UCC(owner) - 4 modules
		// 4.Book-Page from RO docs - 1 module

		List<TSServerInfoModule> moduleList = new ArrayList<TSServerInfoModule>();
		Search search = getSearch();
		int searchType = search.getSearchType();

		if (searchType == Search.AUTOMATIC_SEARCH) {

			TSServerInfoModule module = null;

			FilterResponse crossReferenceToInvalidated = new CrossReferenceToInvalidatedFilter(searchId);
			FilterResponse legalFilter = LegalFilterFactory.getDefaultLegalFilter(searchId);
			ExactDateFilterResponse exactDateFilterResponse = new ExactDateFilterResponse(searchId);
			RejectAlreadySavedDocumentsForUpdateFilter rejectAlreadySavedDocumentsForUpdateFilter = null;
			if (isUpdate()) {
				rejectAlreadySavedDocumentsForUpdateFilter = new RejectAlreadySavedDocumentsForUpdateFilter(searchId);
			}
			SubdivisionFilter subdivisionNameFilter = new SubdivisionFilter(searchId);
			subdivisionNameFilter.setThreshold(new BigDecimal(0.90d));
			subdivisionNameFilter.setLoadFromAdditionalInfo(true);
			DocsValidator subdivisionNameValidator = subdivisionNameFilter.getValidator();

			{
				 // search by instrument number list from Fake Documents saved from DTG
				InstrumentGenericIterator instrumentIteratorFk = getInstrumentIteratorForFakeDocsFromDTG(true);
				module = new TSServerInfoModule(serverInfo.getModule(TSServerInfo.INSTR_NO_MODULE_IDX));
				module.addExtraInformation(TSServerInfoConstants.EXTRA_PARAM_MODULE_DESCRIPTION, 
		    			"Search again for Fake docs from DTG");
	
				if (rejectAlreadySavedDocumentsForUpdateFilter != null) {
					module.addFilter(rejectAlreadySavedDocumentsForUpdateFilter);
				}
				module.addFilter(exactDateFilterResponse);
				module.addValidator(legalFilter.getValidator());
				module.addValidator(subdivisionNameValidator);
				module.addCrossRefValidator(subdivisionNameValidator);
	
				module.setIteratorType(0, FunctionStatesIterator.ITERATOR_TYPE_INSTRUMENT_LIST_FAKE);
				module.addIterator(instrumentIteratorFk);
	
				moduleList.add(module);
			}
	        {
		        // search by book and page list from Fake Documents saved from DTG
	        	InstrumentGenericIterator instrumentIteratorFk = getInstrumentIteratorForFakeDocsFromDTG(false);        	        
		        module = new TSServerInfoModule(serverInfo.getModule(TSServerInfo.BOOK_AND_PAGE_MODULE_IDX));
				module.addExtraInformation(TSServerInfoConstants.EXTRA_PARAM_MODULE_DESCRIPTION, 
		    			"Search again for Fake docs from DTG");
	
				if (rejectAlreadySavedDocumentsForUpdateFilter != null) {
					module.addFilter(rejectAlreadySavedDocumentsForUpdateFilter);
				}
				module.addFilter(exactDateFilterResponse);
				module.addValidator(legalFilter.getValidator());
				module.addValidator(subdivisionNameValidator);
				module.addCrossRefValidator(subdivisionNameValidator);
	
				module.setIteratorType(0, FunctionStatesIterator.ITERATOR_TYPE_BOOK_FAKE);
				module.setIteratorType(1, FunctionStatesIterator.ITERATOR_TYPE_PAGE_FAKE);
				module.setIteratorType(2, FunctionStatesIterator.ITERATOR_TYPE_PAGE_FAKE);
				module.addIterator(instrumentIteratorFk);
	
				moduleList.add(module);
			}
	        
			{// search with book-page from AO-like
				module = new TSServerInfoModule(serverInfo.getModule(TSServerInfo.BOOK_AND_PAGE_MODULE_IDX));
				module.addExtraInformation(TSServerInfoConstants.EXTRA_PARAM_MODULE_DESCRIPTION,
						TSServerInfoConstants.VALUE_PARAM_LIST_AO_NDB_TR_BP);

				if (rejectAlreadySavedDocumentsForUpdateFilter != null) {
					module.addFilter(rejectAlreadySavedDocumentsForUpdateFilter);
				}
				module.addFilter(exactDateFilterResponse);
				module.addValidator(legalFilter.getValidator());
				module.addValidator(subdivisionNameValidator);
				module.addCrossRefValidator(subdivisionNameValidator);

				module.setIteratorType(0, FunctionStatesIterator.ITERATOR_TYPE_BOOK_FAKE);
				module.setIteratorType(1, FunctionStatesIterator.ITERATOR_TYPE_PAGE_FAKE);
				module.setIteratorType(2, FunctionStatesIterator.ITERATOR_TYPE_PAGE_FAKE);
				InstrumentIterator bpIterator = new InstrumentIterator(searchId);
				bpIterator.enableBookPage();
				module.addIterator(bpIterator);

				moduleList.add(module);
			}

			{// search with instrument from AO-like
				InstrumentIterator instrumentIterator = new InstrumentIterator(searchId);
				instrumentIterator.enableInstrumentNumber();

				module = new TSServerInfoModule(serverInfo.getModule(TSServerInfo.INSTR_NO_MODULE_IDX));
				module.addExtraInformation(TSServerInfoConstants.EXTRA_PARAM_MODULE_DESCRIPTION,
						TSServerInfoConstants.VALUE_PARAM_LIST_AO_NDB_TR_INSTR);

				if (rejectAlreadySavedDocumentsForUpdateFilter != null) {
					module.addFilter(rejectAlreadySavedDocumentsForUpdateFilter);
				}
				module.addFilter(exactDateFilterResponse);
				module.addValidator(legalFilter.getValidator());
				module.addValidator(subdivisionNameValidator);
				module.addCrossRefValidator(subdivisionNameValidator);

				module.setIteratorType(0, FunctionStatesIterator.ITERATOR_TYPE_INSTRUMENT_LIST_FAKE);
				module.addIterator(instrumentIterator);

				moduleList.add(module);
			}

			ConfigurableNameIterator recLandNameIterator = null;
			ConfigurableNameIterator uccNameIterator = null;
			ConfigurableNameIterator buyerNameIterator = null;

			{// Recorded Land - search by owner in search page
				module = new TSServerInfoModule(serverInfo.getModule(TSServerInfo.NAME_MODULE_IDX));
				module.addExtraInformation(TSServerInfoConstants.EXTRA_PARAM_MODULE_DESCRIPTION, TSServerInfoConstants.VALUE_PARAM_NAME_OWNERS);
				module.setSaObjKey(SearchAttributes.OWNER_OBJECT);
				module.clearSaKeys();
				module.setSaKey(5, SearchAttributes.FROMDATE_MM_SLASH_DD_SLASH_YYYY);
				module.setSaKey(6, SearchAttributes.TODATE_MM_SLASH_DD_SLASH_YYYY);
				module.forceValue(3, "1");// party type - grantor

				FilterResponse nameFilterOwner = NameFilterFactory.getDefaultNameFilter(
						SearchAttributes.OWNER_OBJECT, searchId, module);
				((GenericNameFilter) nameFilterOwner).setIgnoreMiddleOnEmpty(true);
				((GenericNameFilter) nameFilterOwner).setUseArrangements(false);
				((GenericNameFilter) nameFilterOwner).setInitAgain(true);

				if (rejectAlreadySavedDocumentsForUpdateFilter != null) {
					module.addFilter(rejectAlreadySavedDocumentsForUpdateFilter);
				}
				module.addFilter(nameFilterOwner);
				module.addValidator(crossReferenceToInvalidated.getValidator());
				module.addValidator(legalFilter.getValidator());
				module.addValidator(nameFilterOwner.getValidator());
				module.addValidator(new LastTransferDateFilter(searchId).getValidator());
				module.addValidator(subdivisionNameValidator);
				module.addCrossRefValidator(subdivisionNameValidator);
				if (rejectAlreadySavedDocumentsForUpdateFilter != null) {
					module.addCrossRefValidator(rejectAlreadySavedDocumentsForUpdateFilter.getValidator());
				}

				module.setIteratorType(0, FunctionStatesIterator.ITERATOR_TYPE_LAST_NAME_FAKE);
				module.setIteratorType(1, FunctionStatesIterator.ITERATOR_TYPE_FIRST_NAME_FAKE);
				recLandNameIterator = (ConfigurableNameIterator) ModuleStatesIteratorFactory
						.getConfigurableNameIterator(module, searchId, new String[] { "L;F;" });
				module.addIterator(recLandNameIterator);

				moduleList.add(module);

			}

			{// Recorded Land - search by buyer in search page
				module = new TSServerInfoModule(serverInfo.getModule(TSServerInfo.NAME_MODULE_IDX));
				module.addExtraInformation(TSServerInfoConstants.EXTRA_PARAM_MODULE_DESCRIPTION, TSServerInfoConstants.VALUE_PARAM_NAME_BUYERS);
				module.setSaObjKey(SearchAttributes.BUYER_OBJECT);
				module.clearSaKeys();
				module.setSaKey(5, SearchAttributes.FROMDATE_MM_SLASH_DD_SLASH_YYYY);
				module.setSaKey(6, SearchAttributes.TODATE_MM_SLASH_DD_SLASH_YYYY);
				module.forceValue(3, "2");// party type-grantee

				FilterResponse nameFilterBuyer = NameFilterFactory.getDefaultNameFilter(SearchAttributes.BUYER_OBJECT, search.getID(), module);
				((GenericNameFilter) nameFilterBuyer).setIgnoreMiddleOnEmpty(true);
				((GenericNameFilter) nameFilterBuyer).setUseArrangements(false);
				((GenericNameFilter) nameFilterBuyer).setInitAgain(true);

				FilterResponse doctTypeFilter = DoctypeFilterFactory.getDoctypeFilterForGeneralIndexBuyerNameSearch(searchId)
						.setForcePassIfNoReferences(true)
						.setDocTypesForGoodDocuments(new String[] { DocumentTypes.RELEASE, DocumentTypes.LIEN, DocumentTypes.COURT })
						.setIsUpdate(isUpdate());

				if (rejectAlreadySavedDocumentsForUpdateFilter != null) {
					module.addFilter(rejectAlreadySavedDocumentsForUpdateFilter);
				}
				module.addFilter(nameFilterBuyer);
				module.addValidator(doctTypeFilter.getValidator());
				module.addValidator(crossReferenceToInvalidated.getValidator());
				module.addValidator(legalFilter.getValidator());
				module.addValidator(nameFilterBuyer.getValidator());
				module.addValidator(subdivisionNameValidator);
				module.addCrossRefValidator(subdivisionNameValidator);
				if (rejectAlreadySavedDocumentsForUpdateFilter != null) {
					module.addCrossRefValidator(rejectAlreadySavedDocumentsForUpdateFilter.getValidator());
				}

				module.setIteratorType(0, FunctionStatesIterator.ITERATOR_TYPE_LAST_NAME_FAKE);
				module.setIteratorType(1, FunctionStatesIterator.ITERATOR_TYPE_FIRST_NAME_FAKE);
				buyerNameIterator = (ConfigurableNameIterator) ModuleStatesIteratorFactory
						.getConfigurableNameIterator(module, searchId, new String[] { "L;F;" });
				module.addIterator(buyerNameIterator);

				moduleList.add(module);

			}

			{// UCC - search by owner in search page
				module = new TSServerInfoModule(serverInfo.getModule(TSServerInfo.MODULE_IDX39));
				module.addExtraInformation(TSServerInfoConstants.EXTRA_PARAM_MODULE_DESCRIPTION, TSServerInfoConstants.VALUE_PARAM_NAME_OWNERS);

				module.setSaObjKey(SearchAttributes.OWNER_OBJECT);
				module.clearSaKeys();
				module.setSaKey(5, SearchAttributes.FROMDATE_MM_SLASH_DD_SLASH_YYYY);
				module.setSaKey(6, SearchAttributes.TODATE_MM_SLASH_DD_SLASH_YYYY);
				module.forceValue(3, "1");// party type-grantor

				FilterResponse nameFilterOwner = NameFilterFactory.getDefaultNameFilter(
						SearchAttributes.OWNER_OBJECT, searchId, module);
				((GenericNameFilter) nameFilterOwner).setIgnoreMiddleOnEmpty(true);
				((GenericNameFilter) nameFilterOwner).setUseArrangements(false);
				((GenericNameFilter) nameFilterOwner).setInitAgain(true);

				if (rejectAlreadySavedDocumentsForUpdateFilter != null) {
					module.addFilter(rejectAlreadySavedDocumentsForUpdateFilter);
				}
				module.addFilter(nameFilterOwner);
				module.addValidator(crossReferenceToInvalidated.getValidator());
				module.addValidator(legalFilter.getValidator());
				module.addValidator(nameFilterOwner.getValidator());
				module.addValidator(new LastTransferDateFilter(searchId).getValidator());
				module.addValidator(subdivisionNameValidator);
				module.addCrossRefValidator(subdivisionNameValidator);
				if (rejectAlreadySavedDocumentsForUpdateFilter != null) {
					module.addCrossRefValidator(rejectAlreadySavedDocumentsForUpdateFilter.getValidator());
				}

				module.setIteratorType(0, FunctionStatesIterator.ITERATOR_TYPE_LAST_NAME_FAKE);
				module.setIteratorType(1, FunctionStatesIterator.ITERATOR_TYPE_FIRST_NAME_FAKE);
				uccNameIterator = (ConfigurableNameIterator) ModuleStatesIteratorFactory
						.getConfigurableNameIterator(module, searchId, new String[] { "L;F;" });
				module.addIterator(uccNameIterator);

				moduleList.add(module);

			}

			{// UCC - search by buyer in search page
				module = new TSServerInfoModule(serverInfo.getModule(TSServerInfo.MODULE_IDX39));
				module.addExtraInformation(TSServerInfoConstants.EXTRA_PARAM_MODULE_DESCRIPTION, TSServerInfoConstants.VALUE_PARAM_NAME_BUYERS);
				module.setSaObjKey(SearchAttributes.BUYER_OBJECT);
				module.clearSaKeys();
				module.setSaKey(5, SearchAttributes.FROMDATE_MM_SLASH_DD_SLASH_YYYY);
				module.setSaKey(6, SearchAttributes.TODATE_MM_SLASH_DD_SLASH_YYYY);
				module.forceValue(3, "2");// party type-grantee

				FilterResponse nameFilterBuyer = NameFilterFactory.getDefaultNameFilter(SearchAttributes.BUYER_OBJECT, search.getID(), module);
				((GenericNameFilter) nameFilterBuyer).setIgnoreMiddleOnEmpty(true);
				((GenericNameFilter) nameFilterBuyer).setUseArrangements(false);
				((GenericNameFilter) nameFilterBuyer).setInitAgain(true);

				FilterResponse doctTypeFilter = DoctypeFilterFactory.getDoctypeFilterForGeneralIndexBuyerNameSearch(searchId)
						.setForcePassIfNoReferences(true)
						.setDocTypesForGoodDocuments(new String[] { DocumentTypes.RELEASE, DocumentTypes.LIEN, DocumentTypes.COURT })
						.setIsUpdate(isUpdate());

				if (rejectAlreadySavedDocumentsForUpdateFilter != null) {
					module.addFilter(rejectAlreadySavedDocumentsForUpdateFilter);
				}
				module.addFilter(nameFilterBuyer);
				module.addValidator(doctTypeFilter.getValidator());
				module.addValidator(crossReferenceToInvalidated.getValidator());
				module.addValidator(legalFilter.getValidator());
				module.addValidator(nameFilterBuyer.getValidator());
				module.addValidator(subdivisionNameValidator);
				module.addCrossRefValidator(subdivisionNameValidator);
				if (rejectAlreadySavedDocumentsForUpdateFilter != null) {
					module.addCrossRefValidator(rejectAlreadySavedDocumentsForUpdateFilter.getValidator());
				}

				module.setIteratorType(0, FunctionStatesIterator.ITERATOR_TYPE_LAST_NAME_FAKE);
				module.setIteratorType(1, FunctionStatesIterator.ITERATOR_TYPE_FIRST_NAME_FAKE);
				buyerNameIterator = (ConfigurableNameIterator) ModuleStatesIteratorFactory
						.getConfigurableNameIterator(module, searchId, new String[] { "L;F;" });
				module.addIterator(buyerNameIterator);

				moduleList.add(module);

			}

			{// OCR last transfer - book and page search
				module = new TSServerInfoModule(serverInfo.getModule(TSServerInfo.BOOK_AND_PAGE_MODULE_IDX));
				module.clearSaKeys();
				module.setSaKey(4, SearchAttributes.FROMDATE_MM_SLASH_DD_SLASH_YYYY);
				module.setSaKey(5, SearchAttributes.TODATE_MM_SLASH_DD_SLASH_YYYY);
				module.addExtraInformation(TSServerInfoConstants.EXTRA_PARAM_MODULE_DESCRIPTION,
						TSServerInfoConstants.VALUE_PARAM_OCR_SEARCH_BP);
				module.setIteratorType(ModuleStatesIterator.TYPE_OCR_FULL_OR_BOOTSTRAPER);
				module.setIteratorType(0, FunctionStatesIterator.ITERATOR_TYPE_BOOK_SEARCH);
				module.setIteratorType(1, FunctionStatesIterator.ITERATOR_TYPE_PAGE_SEARCH);
				module.setIteratorType(2, FunctionStatesIterator.ITERATOR_TYPE_PAGE_SEARCH);

				if (isUpdate()) {
					module.addValidator(BetweenDatesFilterResponse.getDefaultIntervalFilter(searchId, module).getValidator());
				}
				module.addValidator(new LastTransferDateFilter(searchId).getValidator());
				module.addValidator(legalFilter.getValidator());
				module.addValidator(subdivisionNameValidator);
				module.addCrossRefValidator(subdivisionNameValidator);

				OcrOrBootStraperIterator ocrBPIterator = new OcrOrBootStraperIterator(searchId);
				ocrBPIterator.setInitAgain(true);
				module.addIterator(ocrBPIterator);

				moduleList.add(module);
			}

			{ // OCR last transfer - instrument number search
				module = new TSServerInfoModule(serverInfo.getModule(TSServerInfo.INSTR_NO_MODULE_IDX));
				module.clearSaKeys();
				module.setSaKey(3, SearchAttributes.FROMDATE_MM_SLASH_DD_SLASH_YYYY);
				module.setSaKey(4, SearchAttributes.TODATE_MM_SLASH_DD_SLASH_YYYY);
				module.addExtraInformation(TSServerInfoConstants.EXTRA_PARAM_MODULE_DESCRIPTION,
						TSServerInfoConstants.VALUE_PARAM_OCR_SEARCH_INST);
				module.setIteratorType(ModuleStatesIterator.TYPE_OCR_FULL_OR_BOOTSTRAPER);
				module.setIteratorType(0, FunctionStatesIterator.ITERATOR_TYPE_INSTRUMENT_LIST_SEARCH);

				if (isUpdate()) {
					module.addValidator(BetweenDatesFilterResponse.getDefaultIntervalFilter(searchId, module).getValidator());
				}
				module.addValidator(new LastTransferDateFilter(searchId).getValidator());
				module.addValidator(legalFilter.getValidator());
				module.addValidator(subdivisionNameValidator);
				module.addCrossRefValidator(subdivisionNameValidator);

				OcrOrBootStraperIterator ocrInstrNoIterator = new OcrOrBootStraperIterator(searchId);
				ocrInstrNoIterator.setInitAgain(true);
				module.addIterator(ocrInstrNoIterator);

				moduleList.add(module);
			}

			ArrayList<NameI> recLandSearchedNames = null;
			ArrayList<NameI> uccSearchedNames = null;
			recLandSearchedNames = recLandNameIterator.getSearchedNames();
			uccSearchedNames = uccNameIterator.getSearchedNames();

			{// Recorded Land - names added by OCR - last transfer
				module = new TSServerInfoModule(serverInfo.getModule(TSServerInfo.NAME_MODULE_IDX));
				module.addExtraInformation(TSServerInfoConstants.EXTRA_PARAM_MODULE_DESCRIPTION, TSServerInfoConstants.VALUE_PARAM_NAME_OWNERS);

				module.setSaObjKey(SearchAttributes.OWNER_OBJECT);
				module.clearSaKeys();
				module.setSaKey(5, SearchAttributes.FROMDATE_MM_SLASH_DD_SLASH_YYYY);
				module.setSaKey(6, SearchAttributes.TODATE_MM_SLASH_DD_SLASH_YYYY);
				module.forceValue(3, "1");// party type - grantor

				FilterResponse nameFilterOwner = NameFilterFactory.getDefaultNameFilter(
						SearchAttributes.OWNER_OBJECT, searchId, module);
				((GenericNameFilter) nameFilterOwner).setIgnoreMiddleOnEmpty(true);
				((GenericNameFilter) nameFilterOwner).setUseArrangements(false);
				((GenericNameFilter) nameFilterOwner).setInitAgain(true);

				if (rejectAlreadySavedDocumentsForUpdateFilter != null) {
					module.addFilter(rejectAlreadySavedDocumentsForUpdateFilter);
				}
				module.addFilter(nameFilterOwner);
				module.addValidator(crossReferenceToInvalidated.getValidator());
				module.addValidator(legalFilter.getValidator());
				module.addValidator(nameFilterOwner.getValidator());
				module.addValidator(new LastTransferDateFilter(searchId).getValidator());
				module.addValidator(subdivisionNameValidator);
				module.addCrossRefValidator(subdivisionNameValidator);
				if (rejectAlreadySavedDocumentsForUpdateFilter != null) {
					module.addCrossRefValidator(rejectAlreadySavedDocumentsForUpdateFilter.getValidator());
				}

				module.setIteratorType(0, FunctionStatesIterator.ITERATOR_TYPE_LAST_NAME_FAKE);
				module.setIteratorType(1, FunctionStatesIterator.ITERATOR_TYPE_FIRST_NAME_FAKE);
				recLandNameIterator = (ConfigurableNameIterator) ModuleStatesIteratorFactory
						.getConfigurableNameIterator(module, searchId, new String[] { "L;F;" });
				recLandNameIterator.setInitAgain(true);
				recLandNameIterator.setSearchedNames(recLandSearchedNames);
				module.addIterator(recLandNameIterator);

				moduleList.add(module);
			}

			{// UCC - names added by OCR - last transfer
				module = new TSServerInfoModule(serverInfo.getModule(TSServerInfo.MODULE_IDX39));
				module.addExtraInformation(TSServerInfoConstants.EXTRA_PARAM_MODULE_DESCRIPTION, TSServerInfoConstants.VALUE_PARAM_NAME_OWNERS);

				module.setSaObjKey(SearchAttributes.OWNER_OBJECT);
				module.clearSaKeys();
				module.setSaKey(5, SearchAttributes.FROMDATE_MM_SLASH_DD_SLASH_YYYY);
				module.setSaKey(6, SearchAttributes.TODATE_MM_SLASH_DD_SLASH_YYYY);
				module.forceValue(3, "1");// party type-grantor

				FilterResponse nameFilterOwner = NameFilterFactory.getDefaultNameFilter(
						SearchAttributes.OWNER_OBJECT, searchId, module);
				((GenericNameFilter) nameFilterOwner).setIgnoreMiddleOnEmpty(true);
				((GenericNameFilter) nameFilterOwner).setUseArrangements(false);
				((GenericNameFilter) nameFilterOwner).setInitAgain(true);

				if (rejectAlreadySavedDocumentsForUpdateFilter != null) {
					module.addFilter(rejectAlreadySavedDocumentsForUpdateFilter);
				}
				module.addFilter(nameFilterOwner);
				module.addValidator(crossReferenceToInvalidated.getValidator());
				module.addValidator(legalFilter.getValidator());
				module.addValidator(nameFilterOwner.getValidator());
				module.addValidator(new LastTransferDateFilter(searchId).getValidator());
				module.addValidator(subdivisionNameValidator);
				module.addCrossRefValidator(subdivisionNameValidator);
				if (rejectAlreadySavedDocumentsForUpdateFilter != null) {
					module.addCrossRefValidator(rejectAlreadySavedDocumentsForUpdateFilter.getValidator());
				}

				module.setIteratorType(0, FunctionStatesIterator.ITERATOR_TYPE_LAST_NAME_FAKE);
				module.setIteratorType(1, FunctionStatesIterator.ITERATOR_TYPE_FIRST_NAME_FAKE);
				recLandNameIterator = (ConfigurableNameIterator) ModuleStatesIteratorFactory
						.getConfigurableNameIterator(module, searchId, new String[] { "L;F;" });
				uccNameIterator.setInitAgain(true);
				uccNameIterator.setSearchedNames(uccSearchedNames);
				module.addIterator(uccNameIterator);

				moduleList.add(module);
			}

			{// search by crossRef book and page list from RO documents
				module = new TSServerInfoModule(serverInfo.getModule(TSServerInfo.BOOK_AND_PAGE_MODULE_IDX));
				module.addExtraInformation(TSServerInfoConstants.EXTRA_PARAM_MODULE_DESCRIPTION,
						TSServerInfoConstants.VALUE_PARAM_LIST_RO_CROSSREF_BP);
				module.clearSaKeys();
				module.setSaKey(4, SearchAttributes.FROMDATE_MM_SLASH_DD_SLASH_YYYY);
				module.setSaKey(5, SearchAttributes.TODATE_MM_SLASH_DD_SLASH_YYYY);
				module.setSaObjKey(SearchAttributes.RO_CROSS_REF_INSTR_LIST);

				if (rejectAlreadySavedDocumentsForUpdateFilter != null) {
					module.addFilter(rejectAlreadySavedDocumentsForUpdateFilter);
				}
				module.addFilter(exactDateFilterResponse);
				module.addValidator(legalFilter.getValidator());
				module.addValidator(subdivisionNameValidator);
				module.addCrossRefValidator(subdivisionNameValidator);
				if (rejectAlreadySavedDocumentsForUpdateFilter != null) {
					module.addCrossRefValidator(rejectAlreadySavedDocumentsForUpdateFilter.getValidator());
				}

				module.setIteratorType(0, FunctionStatesIterator.ITERATOR_TYPE_BOOK_FAKE);
				module.setIteratorType(1, FunctionStatesIterator.ITERATOR_TYPE_PAGE_FAKE);
				module.setIteratorType(2, FunctionStatesIterator.ITERATOR_TYPE_PAGE_FAKE);

				InstrumentIterator bpIterator = new InstrumentIterator(searchId);
				bpIterator.enableBookPage();
				bpIterator.setLoadFromRoLike(true);
				module.addIterator(bpIterator);

				moduleList.add(module);
			}
		}
		serverInfo.setModulesForAutoSearch(moduleList);
	}

	public InstrumentGenericIterator getInstrumentIteratorForFakeDocsFromDTG(boolean instrumentType) {
		InstrumentGenericIterator instrumentGenericIterator = new InstrumentGenericIterator(searchId);

		if (instrumentType) {
			instrumentGenericIterator.enableInstrumentNumber();
		} else {
			instrumentGenericIterator.enableBookPage();
		}
		instrumentGenericIterator.setDoNotCheckIfItExists(true);
		instrumentGenericIterator.setCheckOnlyFakeDocs(true);
		instrumentGenericIterator.setLoadFromRoLike(true);
		instrumentGenericIterator.setDsToLoad(new String[]{"DG"});
		return instrumentGenericIterator;
	}
	
	@Override
	public ServerResponse GetLink(String vsRequest, boolean vbEncodedOrIsParentSite) throws ServerResponseException {

		String link = StringUtils.extractParameter(vsRequest, "Link=(.*)");
		String instrumentNumber = StringUtils.extractParameter(vsRequest, "instrumentnumber=([^&?]*)");
		String commandFlag = StringUtils.extractParameter(vsRequest, "commandflag=([^&?]*)").toLowerCase();

		String fileName = instrumentNumber + ".pdf";
		String imagePath = getSearch().getImagesTempDir() + fileName;

		if (StringUtils.isEmpty(instrumentNumber) || !commandFlag.equals("getdocument")) {
			return super.GetLink(vsRequest, vbEncodedOrIsParentSite);
		}

		return GetImageLink(link, imagePath, vbEncodedOrIsParentSite);

	}

	public ServerResponse GetImageLink(String link, String imagePath, boolean writeImageToClient) throws ServerResponseException {

		boolean imageExists = FileUtils.existPath(imagePath);

		if (!imageExists) {
			retrieveImage(link, imagePath, false);
		}

		imageExists = FileUtils.existPath(imagePath);

		// write the image to the client web-browser
		boolean imageOK = false;
		if (imageExists) {
			imageOK = writeImageToClient(imagePath, pdfContentType);
		}

		// image not retrieved
		if (!imageOK) {
			// return error message
			ParsedResponse pr = new ParsedResponse();
			pr.setError("<br><font color=\"red\"><b>Image not found!</b></font> ");
			throw new ServerResponseException(pr);
		}
		// return solved response
		return ServerResponse.createSolvedResponse();
	}

	protected boolean retrieveImage(String link, String imagePath, boolean saveForDTG) {

		byte[] imageBytes = null;

		HttpSite3 site3 = HttpManager3.getSite(getCurrentServerName(), searchId);
		synchronized (OHFairfieldRO.class) {
			try {
				imageBytes = ((ro.cst.tsearch.connection.http3.OHFairfieldRO) site3).getImage(link, saveForDTG);
			} catch (Exception e) {
				e.printStackTrace();
			} finally {
				HttpManager3.releaseSite(site3);
			}
		}

		ServerResponse resp = new ServerResponse();

		if (imageBytes != null) {
			afterDownloadImage(true);
		} else {
			return false;
		}

		resp.setImageResult(new DownloadImageResult(DownloadImageResult.Status.OK, imageBytes, pdfContentType));

		if (!ro.cst.tsearch.utils.FileUtils.existPath(imagePath)) {
			FileUtils.writeByteArrayToFile(resp.getImageResult().getImageContent(), imagePath);
		}

		return true;
	}

	@Override
	@SuppressWarnings("deprecation")
	protected DownloadImageResult saveImage(ImageLinkInPage image) throws ServerResponseException {
		String imageContentType = image.getContentType();

		try {
			byte[] imageBytes = null;
			String link = image.getLink();
			boolean saveForDTG = false;
			String fileName = "";
			String imagePath = image.getPath();

			if (link.contains("look_for_dt_image")) {// save image for DTG
				saveForDTG = true;
				fileName = imagePath.substring(imagePath.lastIndexOf("\\") + 1, imagePath.length() - 5) + ".pdf";
				imagePath = imagePath.substring(0, imagePath.lastIndexOf("\\") + 1) + fileName;
			}

			if (ro.cst.tsearch.utils.FileUtils.existPath(imagePath)) {
				imageBytes = ro.cst.tsearch.utils.FileUtils.readBinaryFile(imagePath);
				return new DownloadImageResult(DownloadImageResult.Status.OK, imageBytes, imageContentType);
			}

			if (retrieveImage(link, imagePath, saveForDTG)) {
				imageBytes = ro.cst.tsearch.utils.FileUtils.readBinaryFile(imagePath);
				return new DownloadImageResult(DownloadImageResult.Status.OK, imageBytes, imageContentType);
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		return new DownloadImageResult(DownloadImageResult.Status.ERROR, new byte[0], imageContentType);
	}

	@SuppressWarnings("deprecation")
	public DownloadImageResult saveImageFromRO(ImageI image) {

		ImageLinkInPage imgLinkInPg = ImageTransformation.imageToImageLinkInPage(image);
		DownloadImageResult res = null;
		synchronized (OHFairfieldRO.class) {
			try {
				res = saveImage(imgLinkInPg);
			} catch (ServerResponseException e) {
				e.printStackTrace();
			}
		}
		if (res == null) {
			return null;
		}
		if (res.getStatus() == DownloadImageResult.Status.OK) {
			image.setSaved(true);
		}

		return res;
	}

	@Override
	public boolean isInstrumentSaved(String instrumentNo, DocumentI documentToCheck, HashMap<String, String> data, boolean checkMiServerId) {
		if (StringUtils.isEmpty(instrumentNo))
			return false;

		DocumentsManagerI documentManager = getSearch().getDocManager();
		try {
			documentManager.getAccess();
			if (documentToCheck != null) {
				if (documentManager.getDocument(documentToCheck.getInstrument()) != null) {
					RegisterDocumentI docFound = (RegisterDocumentI) documentManager.getDocument(documentToCheck.getInstrument());
					RegisterDocumentI docToCheck = (RegisterDocumentI) documentToCheck;

					if (docFound.isFake() && isAutomaticSearch()){
						if (docToCheck.isOneOf(DocumentTypes.PLAT)){
		    				return false;
		    			}
	    				documentManager.remove(docFound);
	    				SearchLogger.info("<span class='error'>Document was a fake one " + docFound.getDataSource() 
	    										+ " and was removed to be saved from RO.</span><br/>", searchId);
	    				return false;
	    			}
	    			
					processAllReferencedInstruments(docToCheck);
					docToCheck.mergeDocumentsInformation(docFound, searchId, true, false);

					return true;
				} else if (!checkMiServerId) {
					List<DocumentI> almostLike = documentManager.getDocumentsWithInstrumentsFlexible(false, documentToCheck.getInstrument());
					if (almostLike != null && !almostLike.isEmpty()) {
						if (isAutomaticSearch()){
    						if (documentToCheck.isOneOf(DocumentTypes.PLAT)){
        	    				return false;
        	    			}
	    					for (DocumentI documentI : almostLike) {
	    						if (documentI.isFake()){
	    							documentManager.remove(documentI);
	    							SearchLogger.info("<span class='error'>Document was a fake one from " + documentI.getDataSource() 
	    									+ " and was removed to be saved from RO.</span><br/>", searchId);
	        	    				return false;
	        	    			}
							}
    					}
						return true;
					}
				}
			} else {
				InstrumentI instr = new com.stewart.ats.base.document.Instrument(instrumentNo);
				if (data != null) {
					if (!StringUtils.isEmpty(data.get("type"))) {
						String serverDocType = data.get("type");
						String docCateg = DocumentTypes.getDocumentCategory(serverDocType, searchId);
						instr.setDocType(docCateg);
					}

					instr.setDocno(data.get("docno"));
				}

				try {
					instr.setYear(Integer.parseInt(data.get("year")));
				} catch (Exception e) {
				}

				if (documentManager.getDocument(instr) != null) {
					return true;
				} else {
					List<DocumentI> almostLike = documentManager.getDocumentsWithInstrumentsFlexible(false, instr);

					if (checkMiServerId) {
						boolean foundMssServerId = false;
						for (DocumentI documentI : almostLike) {
							if (miServerID == documentI.getSiteId()) {
								foundMssServerId = true;
								break;
							}
						}

						if (!foundMssServerId) {
							return false;
						}
					}

					if (data != null) {
						if (!StringUtils.isEmpty(data.get("type"))) {
							String serverDocType = data.get("type");
							String docCateg = DocumentTypes.getDocumentCategory(serverDocType, searchId);
							for (DocumentI documentI : almostLike) {
								if ((!checkMiServerId || miServerID == documentI.getSiteId())
										&& (documentI.getDocType().equals(docCateg))) {
									// check book-page without the first letter in reference book
									String referenceBook = org.apache.commons.lang.StringUtils.defaultString(documentI.getBook()).replaceFirst("^[A-Z]+", "");
									String referencePage = org.apache.commons.lang.StringUtils.defaultString(documentI.getPage());
									String candidateBook = org.apache.commons.lang.StringUtils.defaultString(data.get("book"));
									String candidatePage = org.apache.commons.lang.StringUtils.defaultString(data.get("page"));
									boolean ignoreBook = false;
									boolean ignorePage = false;
									if (referenceBook.isEmpty() || candidateBook.isEmpty()) {
										ignoreBook = true;
									}
									if (referencePage.isEmpty() || candidatePage.isEmpty()) {
										ignorePage = true;
									}
									if ((ignoreBook || candidateBook.equals(referenceBook)) && (ignorePage || candidatePage.equals(referencePage))) {
										if (isAutomaticSearch()){ 
											if (documentI.isFake()){
							    				documentManager.remove(documentI);
							    				SearchLogger.info("<span class='error'>Document was a fake one " + documentI.getDataSource() 
							    										+ " and was removed to be saved from RO.</span><br/>", searchId);
							    				return false;
							    			}
										}
										return true;
									}
								}
							}
						}
					}
				}
			}

		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			documentManager.releaseAccess();
		}
		return false;
	}

	public class InstrumentIterator extends InstrumentGenericIterator {

		private static final long	serialVersionUID	= 1L;

		public InstrumentIterator(long searchId) {
			super(searchId);
		}

		protected String cleanInstrumentNo(String instno, int year) {
			return instno.replaceFirst("^0+", "");
		}

		@Override
		protected void loadDerrivation(TSServerInfoModule module, InstrumentI inst) {
			super.loadDerrivation(module, inst);

			List<FilterResponse> allFilters = module.getFilterList();
			ExactDateFilterResponse dateFilter = null;
			if (allFilters != null) {
				for (FilterResponse filterResponse : allFilters) {
					if (filterResponse instanceof ExactDateFilterResponse) {
						dateFilter = (ExactDateFilterResponse) filterResponse;
						dateFilter.getFilterDates().clear();
						if (inst instanceof RegisterDocument) {
							Date recordedDate = ((RegisterDocumentI) inst).getRecordedDate();
							Date instrumentDate = ((RegisterDocumentI) inst).getInstrumentDate();

							if (recordedDate == null && instrumentDate == null) {
								module.removeFilter(dateFilter);
								allFilters.remove(dateFilter);
								break;
							}
							else {
								if (recordedDate != null) {
									dateFilter.addFilterDate(recordedDate);
								}
								if (instrumentDate != null) {
									dateFilter.addFilterDate(instrumentDate);
								}
							}
						} else {
							Date instrumentDate = inst.getDate();
							if (instrumentDate == null) {
								module.removeFilter(dateFilter);
								allFilters.remove(dateFilter);
								break;
							}
							else {
								dateFilter.addFilterDate(instrumentDate);
							}
						}
					}
				}
			}
		}
	}
}
