package ro.cst.tsearch.servers.types;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Vector;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.htmlparser.Node;
import org.htmlparser.filters.HasAttributeFilter;
import org.htmlparser.filters.TagNameFilter;
import org.htmlparser.tags.LinkTag;
import org.htmlparser.tags.TableRow;
import org.htmlparser.tags.TableTag;
import org.htmlparser.util.NodeList;

import ro.cst.tsearch.Search;
import ro.cst.tsearch.connection.http2.HttpManager;
import ro.cst.tsearch.connection.http2.HttpSite;
import ro.cst.tsearch.exceptions.ServerResponseException;
import ro.cst.tsearch.extractor.xml.Bridge;
import ro.cst.tsearch.extractor.xml.GenericFunctions2;
import ro.cst.tsearch.extractor.xml.ResultMap;
import ro.cst.tsearch.parser.HtmlParser3;
import ro.cst.tsearch.servers.TSConnectionURL;
import ro.cst.tsearch.servers.response.LinkInPage;
import ro.cst.tsearch.servers.response.OtherInformationSet.OtherInformationSetKey;
import ro.cst.tsearch.servers.response.ParsedResponse;
import ro.cst.tsearch.servers.response.SaleDataSet.SaleDataSetKey;
import ro.cst.tsearch.servers.response.ServerResponse;
import ro.cst.tsearch.servlet.URLConnectionReader;
import ro.cst.tsearch.utils.FileUtils;
import ro.cst.tsearch.utils.StringUtils;
import ro.cst.tsearch.utils.Tidy;

import com.stewart.ats.base.document.DocumentI;
import com.stewart.ats.base.document.ImageI;
import com.stewart.ats.base.document.RegisterDocumentI;
import com.stewart.ats.base.document.RestoreDocumentDataI;

public class COGenericSOS extends TSServerROLike {

	/**
	 * 
	 */
	private static final long	serialVersionUID	= -7776707046913554180L;

	public COGenericSOS(long searchId) {
		super(searchId);
	}

	public COGenericSOS(String rsRequestSolverName, String rsSitePath, String rsServerID, String rsPrmNameLink, long searchId,
			int miServerID) {
		super(rsRequestSolverName, rsSitePath, rsServerID, rsPrmNameLink, searchId, miServerID);
	}

	@Override
	protected DownloadImageResult saveImage(ImageI image) throws ServerResponseException {
		String link = "";
		if (image.getLink(0).split("&Link=").length == 2) {
			link = image.getLink(0).split("&Link=")[1];
		} else
			link = image.getLink(0).split("&Link=")[0];

		byte[] imageBytes = null;

		HttpSite site = HttpManager.getSite(getCurrentServerName(), searchId);
		try {
			imageBytes = ((ro.cst.tsearch.connection.http2.FLGenericCC) site).getImage(link);
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			HttpManager.releaseSite(site);
		}

		ServerResponse resp = new ServerResponse();

		if (imageBytes != null) {
			afterDownloadImage(true);
		} else {
			return new DownloadImageResult(DownloadImageResult.Status.ERROR, new byte[0], image.getContentType());
		}

		String imageName = image.getPath();
		if (ro.cst.tsearch.utils.FileUtils.existPath(imageName)) {
			imageBytes = ro.cst.tsearch.utils.FileUtils.readBinaryFile(imageName);
			return new DownloadImageResult(DownloadImageResult.Status.OK, imageBytes, image.getContentType());
		}

		resp.setImageResult(new DownloadImageResult(DownloadImageResult.Status.OK, imageBytes, image.getContentType()));

		if (!ro.cst.tsearch.utils.FileUtils.existPath(imageName)) {
			FileUtils.writeByteArrayToFile(resp.getImageResult().getImageContent(), image.getPath());
		}

		DownloadImageResult dres = resp.getImageResult();

		return dres;
	}

	protected void ParseResponse(String sAction, ServerResponse Response, int viParseID) throws ServerResponseException {
		String rsResponse = Response.getResult();
		ParsedResponse parsedResponse = Response.getParsedResponse();

		mSearch.setAdditionalInfo("viParseID", viParseID);

		switch (viParseID) {
		case ID_INTERMEDIARY:
		case ID_SEARCH_BY_NAME:
		case ID_SEARCH_BY_SALES:
		case ID_SEARCH_BY_SUBDIVISION_NAME:
		case ID_SEARCH_BY_CONDO_NAME:
			if (rsResponse.contains("Entered ID number not found in database, please check ID and try again")
					|| rsResponse.contains("No results found for the specified")) {
				Response.getParsedResponse().setError("No Results Found!");
				return;
			}

			if (rsResponse.contains("ID or document number must be 11 digits")
					|| rsResponse.contains("404 Not Found")) {
				Response.getParsedResponse().setError("Incorect search criteria!");
				return;
			}
			
			if (rsResponse.contains("You must check at least one box to specify the subject of your search criteria")) {
				Response.getParsedResponse().setError("You must check at least one box to specify the subject of your search criteria.");
				Response.getParsedResponse().setAttribute(ParsedResponse.SHOW_ONLY_ERROR_MESSAGE, "true");
				return;
			}

			if (rsResponse.contains("Exceeded Record Count, please refine search")) {
				Response.getParsedResponse().setError("Exceeded Record Count, please refine search!");
				return;
			}

			StringBuilder outputTable = new StringBuilder();
			Collection<ParsedResponse> smartParsedResponses = smartParseIntermediary(Response, rsResponse, outputTable);

			if (smartParsedResponses.size() == 0) {
				return;
			}

			parsedResponse.setHeader("<table border=\"1\">");
			parsedResponse.setFooter("</table>");

			parsedResponse.setResultRows(new Vector<ParsedResponse>(smartParsedResponses));
			parsedResponse.setOnlyResponse(outputTable.toString());

			if (getSearch().getSearchType() == Search.PARENT_SITE_SEARCH) {
				String header = "<table>";
				String footer = "";

				// set prev next buttons
				String prevNext = getPrevNext(Response);

				footer = "\n</table><br>" + prevNext;
				if (viParseID == ID_SEARCH_BY_NAME || viParseID == ID_SEARCH_BY_CONDO_NAME) {
					header = CreateSaveToTSDFormHeader(URLConnectionReader.MULTIPLE_SAVE_TO_TSD_ACTION, "POST");
					Object numberOfUnsavedDocument = GetAttribute(NUMBER_OF_UNSAVED_DOCUMENTS);
					if (numberOfUnsavedDocument != null && numberOfUnsavedDocument instanceof Integer) {
						footer += CreateSaveToTSDFormEnd(SAVE_SELECTED_DOCUMENTS_BUTTON_LABEL, viParseID, (Integer) numberOfUnsavedDocument);
					} else {
						footer += CreateSaveToTSDFormEnd(SAVE_SELECTED_DOCUMENTS_BUTTON_LABEL, viParseID, -1);
					}
				}

				header += makeHeader(viParseID, Response);

				parsedResponse.setHeader(header);
				parsedResponse.setFooter(footer);
			}

			break;

		case ID_DETAILS:
		case ID_SEARCH_BY_PARCEL:
		case ID_SAVE_TO_TSD:

			StringBuilder accountId = new StringBuilder();

			HashMap<String, String> data = new HashMap<String, String>();

			String details = getDetails(rsResponse, accountId, data, Response);

			String accountName = accountId.toString();

			String filename = accountName + ".html";

			if (viParseID != ID_SAVE_TO_TSD) {
				String originalLink = sAction + "&" + Response.getRawQuerry();
				String sSave2TSDLink = getLinkPrefix(TSConnectionURL.idGET) + originalLink;

				if (isInstrumentSaved(accountName, null, data)) {
					details += CreateFileAlreadyInTSD();
				} else {
					mSearch.addInMemoryDoc(sSave2TSDLink, details);
					details = addSaveToTsdButton(details, sSave2TSDLink, ID_DETAILS);
				}
				Response.getParsedResponse().setPageLink(new LinkInPage(sSave2TSDLink, originalLink, TSServer.REQUEST_SAVE_TO_TSD));
				Response.getParsedResponse().setResponse(details);

			} else {
				smartParseDetails(Response, details);

				msSaveToTSDFileName = filename;
				Response.getParsedResponse().setFileName(getServerTypeDirectory() + msSaveToTSDFileName);
				Response.getParsedResponse().setResponse(details.replaceAll("(?ism)<a href[^>]*>([^<]*)</a>", "$1"));

				msSaveToTSDResponce = details.replaceAll("(?ism)<a href[^>]*>([^<]*)</a>", "$1") + CreateFileAlreadyInTSD();
				// InstanceManager.getManager().getCurrentInstance(searchId).getCrtSearchContext()
				// .setLastTransferData(Response.getParsedResponse());

			}

			break;
		case ID_GET_LINK:
			String url = Response.getLastURI().getEscapedURI();

			if (url.contains("AdvancedSearchResults.do")) {
				ParseResponse(sAction, Response, ID_SEARCH_BY_SALES);
			} else if (url.contains("AdvancedSearchCriteria.do")
					|| url.contains("TradenameOwnerResults.do")
					|| url.contains("NameReservationOwnerResults.do")
					|| url.contains("RegAgentOwnerResults.do")
					|| url.contains("TrademarkOwnerResults.do")
					|| url.contains("BusinessEntityResultsAdv.do")) {
				ParseResponse(sAction, Response, ID_SEARCH_BY_NAME);
			} else if (url.contains("AdvancedTrademarkSearchResults.do")) {
				ParseResponse(sAction, Response, ID_SEARCH_BY_CONDO_NAME);
			} else {
				ParseResponse(sAction, Response, rsResponse.contains("Business Search Results") ? ID_INTERMEDIARY : ID_DETAILS);
			}

			break;
		default:
			break;
		}
	}

	private String makeHeader(int viParseID, ServerResponse resp) {
		String header = "";

		String th = "";

		if (resp.getParsedResponse().getAttribute("mainTableList") != null) {
			NodeList mainTableList = (NodeList) resp.getParsedResponse().getAttribute("mainTableList");
			NodeList matching = mainTableList.elementAt(2).getChildren()
					.extractAllNodesThatMatch(new TagNameFilter("div"), true)
					.extractAllNodesThatMatch(new HasAttributeFilter("id", "box"), true)
					.extractAllNodesThatMatch(new TagNameFilter("table"), true);
					//.extractAllNodesThatMatch(new HasAttributeFilter("border", "0"), true);

			// get header
			if (matching.size() > 0) {
				TableTag t = (TableTag) matching.elementAt(0);
				TableRow[] rows = t.getRows();

				if (rows.length > 0) {
					th = rows[0].toHtml();
					th = th.replaceAll("(?ism)</?tr>", "")
							.replaceAll("(?ism)<(/?)th[^>]*>", "<$1th>")
							.replaceAll("(?ism)<!--.*?-->", "")
							.replaceAll("(?ism)</?a[^>]*>", "")
							.replaceAll("(?ism)<img[^>]*>", "");
				}
			}
		}

		switch (viParseID) {
		case ID_SEARCH_BY_NAME:
		case ID_SEARCH_BY_CONDO_NAME:
			header += "<table align=\"center\" cellspacing=\"0\" cellpadding=\"3\" border=\"1\" width=\"95%\">"
					+ "<TR bgcolor=\"#6699CC\" valign=\"top\">"
					+ "<TH ALIGN=\"center\">"
					+ SELECT_ALL_CHECKBOXES
					+ "</TH>"
					+ th
					+ "</tr>";
			break;
		case ID_SEARCH_BY_SALES:
		case ID_SEARCH_BY_SUBDIVISION_NAME:
			header += "<table align=\"center\" cellspacing=\"0\" cellpadding=\"3\" border=\"1\" width=\"95%\">"
					+ "<TR bgcolor=\"#6699CC\" valign=\"top\">"
					+ th
					+ "</tr>";
			break;

		default:
			break;
		}

		return header;
	}

	private String getPrevNext(ServerResponse resp) {
		if (resp.getParsedResponse().getAttribute("mainTableList") != null) {
			NodeList mainTableList = (NodeList) resp.getParsedResponse().getAttribute("mainTableList");

			NodeList secTableList = mainTableList.elementAt(2).getChildren()
					.extractAllNodesThatMatch(new TagNameFilter("div"), true)
					.extractAllNodesThatMatch(new HasAttributeFilter("class", "box"), true);

			NodeList matching = mainTableList.elementAt(1).getChildren()
					.extractAllNodesThatMatch(new TagNameFilter("div"), true)
					.extractAllNodesThatMatch(new HasAttributeFilter("id", "box"), true)
					.extractAllNodesThatMatch(new TagNameFilter("table"), true)
					.extractAllNodesThatMatch(new HasAttributeFilter("border", "0"), true);

			String found = "";
			String prevLink = "";
			String nextLink = "";

			String linkPrefix = CreatePartialLink(TSConnectionURL.idGET);

			if (matching.size() > 0 && matching.elementAt(0).getChildren().size() > 2) {
				Node n = matching.elementAt(0).getChildren().elementAt(2);
				found = n.toPlainTextString().replace("&nbsp;", " ").replaceAll("\\s+", " ").trim();
			}

			if (secTableList.size() > 0) {
				// we have multiple pages
				NodeList prev = secTableList.extractAllNodesThatMatch(new HasAttributeFilter("class", "linkPrev"), true);
				NodeList next = secTableList.extractAllNodesThatMatch(new HasAttributeFilter("class", "linkNext"), true);

				if (prev.size() > 0 && prev.elementAt(0).getChildren().size() > 0) {
					prevLink = "<a href=" + linkPrefix + ((LinkTag) prev.elementAt(0).getChildren().elementAt(0)).getLink() + ">Prev";
				}

				if (next.size() > 0 && next.elementAt(0).getChildren().size() > 0) {
					nextLink = "<a href=" + linkPrefix + ((LinkTag) next.elementAt(0).getChildren().elementAt(0)).getLink() + ">Next";
				}
			}
			// make result
			String res = "<table align=center width=95% >\n<tr><td>"
					+ found + "\n</td></tr>"
					+ "\n<tr><td>" + prevLink + "</td>\n"
					+ "<td>" + nextLink + "</td>\n</tr>\n</table><br/>";

			return res;
		}
		return "";
	}

	protected String getDetails(String rsResponse, StringBuilder accountId, HashMap<String, String> data, ServerResponse Response) {
		try {

			NodeList nodeList = org.htmlparser.Parser.createParser(Tidy.tidyParse(rsResponse.replaceAll("(?ism)</?font[^>]*>", ""), null), null).parse(null);

			NodeList mainTableList = nodeList.extractAllNodesThatMatch(new TagNameFilter("form"), true);

			if (!(mainTableList.size() == 3)) {
				return rsResponse;
			}
			
			String form = mainTableList.toHtml();

			String id = HtmlParser3.getValueFromAbsoluteCell(0, 1, HtmlParser3.findNode(nodeList, "ID number"), "", false);

			if (StringUtils.isNotEmpty(id)) {
				accountId.append(id);
			}

			data.put("type", "UCC");

			/* If from memory - use it as is */
			if (!org.apache.commons.lang.StringUtils.containsIgnoreCase(rsResponse, "<html")) {
				return rsResponse;
			}

			mainTableList = mainTableList.elementAt(2).getChildren().extractAllNodesThatMatch(new TagNameFilter("table"), true)
				.extractAllNodesThatMatch(new HasAttributeFilter("border", "0"))
				.extractAllNodesThatMatch(new HasAttributeFilter("align", "center"));
				
			StringBuffer details = new StringBuffer();

			details.append("<tr><td><p id=\"fakeHeader\" align=\"center\" width=100%><font style=\"font-size:xx-large;\"><b>Summary</b></font></p><br></td></tr>");

			if (mainTableList.size() > 0) {
				// needed Table
				TableTag t = (TableTag) mainTableList.elementAt(0);

				t.setAttribute("id", "summary");
				t.setAttribute("align", "center");

				// remove scrap
				details.append(t.toHtml().replaceAll("(?ism)<div class=\"box\">.*</div>", ""));
				
			}
			
			// get history
			Matcher ma = Pattern.compile("(?is)<a[^>]+href=\"([^\"]+)\"[^>]*>\\s*Filing\\s+history\\s+and\\s+documents\\s*</a>").matcher(form);
			if (ma.find()) {
				details.append(getHistory(ma.group(1).replace("&amp;", "&"), Response));
			}
						
			return details.toString();

		} catch (Exception e) {
			e.printStackTrace();
		}
		return null;
	}

	private String getHistory(String link, ServerResponse response) {
		StringBuffer sb = new StringBuffer();

		try {
			ArrayList<TableTag> histList = new ArrayList<TableTag>();

			String history = getLinkContents(getDataSite().getServerHomeLink() + "biz/" + link);

			NodeList nodeList = org.htmlparser.Parser.createParser(Tidy.tidyParse(history, null), null).parse(null);
			NodeList mainTableList = nodeList.extractAllNodesThatMatch(new TagNameFilter("form"), true);

			if (mainTableList.size() != 3) {
				return "";
			}

			// put this history table in
			NodeList secTableList = mainTableList.elementAt(2).getChildren()
					.extractAllNodesThatMatch(new TagNameFilter("div"), true)
					.extractAllNodesThatMatch(new HasAttributeFilter("id", "box"), true)
					.extractAllNodesThatMatch(new TagNameFilter("table"), true)
					.extractAllNodesThatMatch(new HasAttributeFilter("border", "0"), true);

			if (secTableList.size() > 0) {
				histList.add((TableTag) secTableList.elementAt(0));
			}

			// has multiple pages
			NodeList auxPages = mainTableList.elementAt(2).getChildren()
					.extractAllNodesThatMatch(new TagNameFilter("dd"), true)
					.extractAllNodesThatMatch(new HasAttributeFilter("class", "links"), true);

			if (auxPages.size() > 0 && auxPages.elementAt(0).getChildren().size() > 0) {
				NodeList children = auxPages.elementAt(0).getChildren();
				for (int i = 0; i < children.size(); i++) {
					if (children.elementAt(i) instanceof LinkTag) {
						String auxHist = getLinkContents(((LinkTag) children.elementAt(i)).getLink());
						NodeList aux = org.htmlparser.Parser.createParser(Tidy.tidyParse(auxHist, null), null).parse(null);
						aux = aux.extractAllNodesThatMatch(new TagNameFilter("form"), true);
						if (aux.size() == 3) {
							aux = aux.elementAt(2).getChildren()
									.extractAllNodesThatMatch(new TagNameFilter("div"), true)
									.extractAllNodesThatMatch(new HasAttributeFilter("id", "box"), true)
									.extractAllNodesThatMatch(new TagNameFilter("table"), true)
									.extractAllNodesThatMatch(new HasAttributeFilter("border", "0"), true);
							if (aux.size() > 0) {
								histList.add((TableTag) aux.elementAt(0));
							}
						}
					}
				}
			}

			String linkPrefix = CreatePartialLink(TSConnectionURL.idGET) + getDataSite().getServerHomeLink() + "biz/";

			sb.append("<p id=\"fakeHeader\" align=\"center\" width=100%><font style=\"font-size:xx-large;\"><b>History and Documents</b></font></p><br>");

			// parse tables
			int i = 0;

			for (TableTag t : histList) {
				t.setAttribute("id", "history" + i);
				t.setAttribute("width", "95%");
				t.setAttribute("align", "center");

				// rows = t.getRows();
				//
				// for (TableRow r : rows) {
				// if (r.getColumnCount() == 7 && !r.toHtml().contains("<th>")) {
				// LinkTag l = ((LinkTag) r.getColumns()[5].getChild(0));
				// }
				// }

				sb.append(t.toHtml()
						.replaceAll("(?ism)<img[^>]*>", "")
						.replaceAll("(?ism)<br>[^<]*<font[^>]*>[^<]*</font>", "")
						.replaceAll("(?ism)<a href[^>]*>[^<]*(<b>[^<]*</b>)[^<]*</a>", "$1")
						.replaceAll("(?ism)(<a href=\")([^\"]*)(\"[^>]*>[^<]*</a>)", "$1" + linkPrefix + "$2$3")
						+ "<br><br>\n");
				i++;
			}

		} catch (Exception e) {
			e.printStackTrace();
		}

		return sb.toString();
	}

	private Collection<ParsedResponse> smartParseIntermediaryName(ServerResponse response, TableTag t, int number) {
		int numberOfUncheckedElements = 0;
		Vector<ParsedResponse> intermediaryResponse = new Vector<ParsedResponse>();

		try {
			TableRow[] rows = t.getRows();

			for (int i = 1; i < rows.length; i++) {
				TableRow row = rows[i];
				if (row.getColumnCount() > 3) {
					ResultMap m = ro.cst.tsearch.servers.functions.COGenericSOS.parseIntermediaryRow(row, number);

					// index link
					String linkPrefix = CreatePartialLink(TSConnectionURL.idGET);
					String docLink = "";

					if (row.getColumns()[1].getChildren().size() > 0) {
						LinkTag linkTag = (LinkTag) row.getColumns()[1].getChild(0);

						docLink = linkTag.getLink().replace("&amp;", "&").replaceFirst("\\?", "&");

						linkTag.setLink(linkPrefix + docLink);
					}

					String cleanRow = row.toHtml();

					ParsedResponse currentResponse = new ParsedResponse();
					currentResponse.setAttribute(ParsedResponse.SERVER_ROW_RESPONSE, cleanRow);
					currentResponse.setOnlyResponse(cleanRow);
					currentResponse.setPageLink(new LinkInPage(docLink, docLink, TSServer.REQUEST_SAVE_TO_TSD));

					Bridge bridge = new Bridge(currentResponse, m, searchId);

					DocumentI document = (RegisterDocumentI) bridge.importData();
					currentResponse.setDocument(document);

					String checkBox = "checked";

					String instrNo = org.apache.commons.lang.StringUtils.defaultString((String) m.get(SaleDataSetKey.INSTRUMENT_NUMBER.getKeyName()));

					HashMap<String, String> data = new HashMap<String, String>();

					data.put("type", "UCC");

					if (isInstrumentSaved(instrNo, null, data) && !Boolean.TRUE.equals(getSearch().getAdditionalInfo("RESAVE_DOCUMENT"))) {
						checkBox = "saved";
					} else {
						numberOfUncheckedElements++;
						LinkInPage linkInPage = new LinkInPage(linkPrefix + docLink, linkPrefix + docLink, TSServer.REQUEST_SAVE_TO_TSD);
						checkBox = "<input type='checkbox' name='docLink' value='" + linkPrefix + docLink + "'>";
						if (getSearch().getInMemoryDoc(docLink) == null) {
							getSearch().addInMemoryDoc(docLink, currentResponse);
						}
						currentResponse.setPageLink(linkInPage);
					}
					String rowHtml = "<tr align=\"center\"><td>"
							+ checkBox
							+ "</td>"
							+ cleanRow.replaceAll("(?ism)<tr[^>]*>", "").replaceAll("(?ism)</tr>", "") + "</tr>";

					currentResponse.setAttribute(ParsedResponse.SERVER_ROW_RESPONSE, rowHtml.replaceAll("(?ism)<a [^>]*>([^<]*)<[^>]*>", "$1"));
					currentResponse.setOnlyResponse(rowHtml);

					intermediaryResponse.add(currentResponse);
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		SetAttribute(NUMBER_OF_UNSAVED_DOCUMENTS, numberOfUncheckedElements);
		return intermediaryResponse;
	}

	private Collection<ParsedResponse> smartParseIntermediaryAdvancedName(ServerResponse response, TableTag t, int number) {
		Vector<ParsedResponse> intermediaryResponse = new Vector<ParsedResponse>();

		try {
			TableRow[] rows = t.getRows();

			for (int i = 1; i < rows.length; i++) {
				TableRow row = rows[i];
				if (row.getColumnCount() == 5) {
					ResultMap m = ro.cst.tsearch.servers.functions.COGenericSOS.parseIntermediaryRow(row, number);

					// index link
					String linkPrefix = CreatePartialLink(TSConnectionURL.idGET);
					String docLink = "";

					if (row.getColumns()[1].getChildren().size() > 0) {
						LinkTag linkTag = (LinkTag) row.getColumns()[1].getChild(0);

						docLink = linkTag.getLink().replace("&amp;", "&").replaceFirst("\\?", "&");

						linkTag.setLink(linkPrefix + docLink);
					}

					String cleanRow = row.toHtml();

					ParsedResponse currentResponse = new ParsedResponse();
					currentResponse.setAttribute(ParsedResponse.SERVER_ROW_RESPONSE, cleanRow);
					currentResponse.setOnlyResponse(cleanRow);
					currentResponse.setPageLink(new LinkInPage(docLink, docLink, TSServer.REQUEST_GO_TO_LINK));

					Bridge bridge = new Bridge(currentResponse, m, searchId);

					DocumentI document = (RegisterDocumentI) bridge.importData();
					currentResponse.setDocument(document);

					currentResponse.setAttribute(ParsedResponse.SERVER_ROW_RESPONSE, cleanRow.replaceAll("(?ism)<a [^>]*>([^<]*)<[^>]*>", "$1"));
					currentResponse.setOnlyResponse(cleanRow);

					intermediaryResponse.add(currentResponse);
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}

		return intermediaryResponse;
	}

	@Override
	public Collection<ParsedResponse> smartParseIntermediary(ServerResponse response, String table, StringBuilder outputTable) {
		Vector<ParsedResponse> intermediaryResponse = new Vector<ParsedResponse>();
		try {
			NodeList nodeList = org.htmlparser.Parser.createParser(Tidy.tidyParse(table, null), null).parse(null);

			NodeList mainTableList = nodeList.extractAllNodesThatMatch(new TagNameFilter("form"), true);

			if (!(mainTableList.size() == 3)) {
				return intermediaryResponse;
			}

			response.getParsedResponse().setAttribute("mainTableList", mainTableList);

			int viparseID = (Integer) mSearch.getAdditionalInfo("viParseID");

			mainTableList = mainTableList.elementAt(2).getChildren().extractAllNodesThatMatch(new TagNameFilter("table"), true);

			if (mainTableList.size() == 3) {
				TableTag t = (TableTag) mainTableList.elementAt(1);

				int nameColumn = 3; // AdvancedSearchCriteria.do -> name search

				// String url = response.getLastURI().getEscapedURI();

				if (t.getRows().length > 0 && t.getRows()[0].getHeaderCount() > 3) {
					if (t.getRows()[0].getHeaders()[1].toPlainTextString().contains("Name")) {
						nameColumn = 1;
					} else if (t.getRows()[0].getHeaders()[2].toPlainTextString().contains("Name")
							|| t.getRows()[0].getHeaders()[2].toPlainTextString().contains("Description")) {
						nameColumn = 2;
					} else if (t.getRows()[0].getHeaders()[3].toPlainTextString().contains("Name")) {
						nameColumn = 3;
					}
				}

				switch (viparseID) {
				case ID_SEARCH_BY_NAME:
				case ID_SEARCH_BY_CONDO_NAME:
					intermediaryResponse = (Vector<ParsedResponse>) smartParseIntermediaryName(response, t, nameColumn);
					break;
				case ID_SEARCH_BY_SALES:
				case ID_SEARCH_BY_SUBDIVISION_NAME:
					intermediaryResponse = (Vector<ParsedResponse>) smartParseIntermediaryAdvancedName(response, t, nameColumn);
					break;
				default:
					break;
				}
				outputTable.append(table);
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		return intermediaryResponse;
	}

	@SuppressWarnings("rawtypes")
	@Override
	protected Object parseAndFillResultMap(ServerResponse response, String detailsHtml, ResultMap map) {
		try {
			map.put(OtherInformationSetKey.SRC_TYPE.getKeyName(), "CC");
			map.put(SaleDataSetKey.DOCUMENT_TYPE.getKeyName(), "UCC");

			NodeList nodes = new HtmlParser3(detailsHtml).getNodeList();

			nodes = nodes.extractAllNodesThatMatch(new TagNameFilter("table"), true);

			String id = "";

			if (nodes.size() > 0) {
				// extract summary
				NodeList summaryList = nodes.extractAllNodesThatMatch(new HasAttributeFilter("id", "summary"), true);
				if (summaryList.size() > 0) {
					// get name
					// Trade Name: , Name: , Trademark:
					// Name:, Name Reservation Name:,

					String name = HtmlParser3.getValueFromAbsoluteCell(0, 1, HtmlParser3.findNode(summaryList, "Trademark"), "", false);

					if (StringUtils.isEmpty(name)) {
						name = HtmlParser3.getValueFromAbsoluteCell(0, 1, HtmlParser3.findNode(summaryList, "Trade name"), "", false);
					}
					
					if (StringUtils.isEmpty(name)) {
						name = HtmlParser3.getValueFromAbsoluteCell(0, 1, HtmlParser3.findNode(summaryList, "Name"), "", false);
					}

					if (StringUtils.isNotEmpty(name)) {
						name = name.replace("&nbsp;", " ").replace("&amp;", "&").trim();
						name = name.replaceAll("(?ism), Delinquent \\w+ \\d+, \\d+", "");
						name = name.replaceAll("(?ism), Dissolved.*$", "");
						ro.cst.tsearch.servers.functions.COGenericSOS.parseNames(map, Arrays.asList(new String[] { name }), "GrantorSet");
					}

					// get docNo
					// ID Number:
					id = HtmlParser3.getValueFromAbsoluteCell(0, 1, HtmlParser3.findNode(summaryList, "ID number"), "", false);

					if (StringUtils.isNotEmpty(id)) {
						id = id.trim();
						map.put(SaleDataSetKey.INSTRUMENT_NUMBER.getKeyName(), id);
					}

					// get date
					// Formation Date:
					String date = HtmlParser3.getValueFromAbsoluteCell(0, 1, HtmlParser3.findNode(summaryList, "Formation date"), "", false);
					
					if (StringUtils.isEmpty(date)) {
						date = HtmlParser3.getValueFromAbsoluteCell(0, 1, HtmlParser3.findNode(summaryList, "Formation Date"), "", false);
					}

					if (StringUtils.isEmpty(date)) {
						date = HtmlParser3.getValueFromAbsoluteCell(0, 1, HtmlParser3.findNode(summaryList, "Registration date"), "", false);
					}
					
					if (StringUtils.isEmpty(date)) {
						date = HtmlParser3.getValueFromAbsoluteCell(0, 1, HtmlParser3.findNode(summaryList, "Effective date"), "", false);
					}

					if (StringUtils.isNotEmpty(date) && date.matches("(?ism)\\d+/\\d+/\\d+")) {
						date = date.trim();
						map.put(SaleDataSetKey.INSTRUMENT_DATE.getKeyName(), date);
						map.put(SaleDataSetKey.RECORDED_DATE.getKeyName(), date);
					}
				}

				// get related docs
				List<TableTag> related = new ArrayList<TableTag>();

				int i = 0;
				while (true) {
					NodeList histT = nodes.extractAllNodesThatMatch(new HasAttributeFilter("id", "history" + i), true);

					if (histT.size() > 0) {
						related.add((TableTag) histT.elementAt(0));
					} else {
						break;
					}
					i++;
				}

				String[] refHeader = { SaleDataSetKey.INSTRUMENT_NUMBER.getShortKeyName(), SaleDataSetKey.RECORDED_DATE.getShortKeyName() };
				List<List> refBody = new ArrayList<List>();
				List<String> refRow = new ArrayList<String>();

				for (TableTag t : related) {
					TableRow[] rows = t.getRows();
					for (TableRow r : rows) {
						if (r.getColumnCount() == 7 && !r.toHtml().contains("Document #")) {
							refRow = new ArrayList<String>();

							String idr = r.getColumns()[5].toPlainTextString().replace("&nbsp;", "").trim();
							idr = org.apache.commons.lang.StringUtils.defaultString(idr);

							if (!id.equals(idr)) {
								refRow.add(idr);
								refRow.add(r.getColumns()[2].toPlainTextString().replace("&nbsp;", "").trim());
								refBody.add(refRow);
							}
						}
					}
				}

				if (refBody.size() > 0) {
					map.put("CrossRefSet", GenericFunctions2.createResultTableFromList(refHeader, refBody));
				}
			}

		} catch (Exception e) {
			e.printStackTrace();
		}

		return null;
	}

	@Override
	public Object getRecoverModuleFrom(RestoreDocumentDataI document) {
		// TODO Auto-generated method stub
		return null;
	}
}
