package ro.cst.tsearch.servers.types;

import java.math.BigDecimal;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.Vector;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.htmlparser.Node;
import org.htmlparser.filters.HasAttributeFilter;
import org.htmlparser.filters.TagNameFilter;
import org.htmlparser.tags.Div;
import org.htmlparser.tags.LinkTag;
import org.htmlparser.tags.TableRow;
import org.htmlparser.tags.TableTag;
import org.htmlparser.util.NodeList;

import ro.cst.tsearch.MailConfig;
import ro.cst.tsearch.Search;
import ro.cst.tsearch.bean.SearchAttributes;
import ro.cst.tsearch.connection.http.HTTPRequest;
import ro.cst.tsearch.connection.http.HTTPResponse;
import ro.cst.tsearch.connection.http2.HttpManager;
import ro.cst.tsearch.connection.http2.HttpSite;
import ro.cst.tsearch.emailClient.EmailClient;
import ro.cst.tsearch.exceptions.ServerResponseException;
import ro.cst.tsearch.extractor.xml.Bridge;
import ro.cst.tsearch.extractor.xml.ResultMap;
import ro.cst.tsearch.extractor.xml.StringFormats;
import ro.cst.tsearch.generic.Util;
import ro.cst.tsearch.parser.HtmlParser3;
import ro.cst.tsearch.search.FunctionStatesIterator;
import ro.cst.tsearch.search.GenericRuntimeIterator;
import ro.cst.tsearch.search.ModuleStatesIterator;
import ro.cst.tsearch.search.filter.ExactDateFilterResponse;
import ro.cst.tsearch.search.filter.FilterResponse;
import ro.cst.tsearch.search.filter.RejectAlreadySavedDocumentsFilterResponse;
import ro.cst.tsearch.search.filter.matchers.algorithm.LotInterval;
import ro.cst.tsearch.search.filter.matchers.algorithm.LotMatchAlgorithm;
import ro.cst.tsearch.search.filter.newfilters.date.DateFilterFactory;
import ro.cst.tsearch.search.filter.newfilters.date.LastTransferDateFilter;
import ro.cst.tsearch.search.filter.newfilters.doctype.DocTypeSimpleFilter;
import ro.cst.tsearch.search.filter.newfilters.legal.GenericInstrumentFilter;
import ro.cst.tsearch.search.filter.newfilters.legal.GenericLegal;
import ro.cst.tsearch.search.filter.newfilters.legal.LegalFilterFactory;
import ro.cst.tsearch.search.filter.newfilters.name.GenericNameFilter;
import ro.cst.tsearch.search.filter.newfilters.name.NameFilterFactory;
import ro.cst.tsearch.search.iterator.ModuleStatesIteratorFactory;
import ro.cst.tsearch.search.module.ConfigurableNameIterator;
import ro.cst.tsearch.search.name.NameUtils;
import ro.cst.tsearch.servers.HashCountyToIndex;
import ro.cst.tsearch.servers.TSConnectionURL;
import ro.cst.tsearch.servers.info.TSServerInfo;
import ro.cst.tsearch.servers.info.TSServerInfoConstants;
import ro.cst.tsearch.servers.info.TSServerInfoModule;
import ro.cst.tsearch.servers.response.ImageLinkInPage;
import ro.cst.tsearch.servers.response.LinkInPage;
import ro.cst.tsearch.servers.response.ParsedResponse;
import ro.cst.tsearch.servers.response.SaleDataSet.SaleDataSetKey;
import ro.cst.tsearch.servers.response.ServerResponse;
import ro.cst.tsearch.servlet.URLConnectionReader;
import ro.cst.tsearch.templates.MultilineElementsMap;
import ro.cst.tsearch.titledocument.abstracts.DocumentTypes;
import ro.cst.tsearch.utils.FileUtils;
import ro.cst.tsearch.utils.GBManager;
import ro.cst.tsearch.utils.InstanceManager;
import ro.cst.tsearch.utils.RequestParams;
import ro.cst.tsearch.utils.StringUtils;
import ro.cst.tsearch.utils.URLMaping;

import com.stewart.ats.base.document.DocumentI;
import com.stewart.ats.base.document.DocumentUtils;
import com.stewart.ats.base.document.InstrumentI;
import com.stewart.ats.base.document.RegisterDocument;
import com.stewart.ats.base.document.RegisterDocumentI;
import com.stewart.ats.base.document.RestoreDocumentDataI;
import com.stewart.ats.base.legal.Legal;
import com.stewart.ats.base.legal.LegalI;
import com.stewart.ats.base.legal.Subdivision;
import com.stewart.ats.base.legal.SubdivisionI;
import com.stewart.ats.base.legal.TownShip;
import com.stewart.ats.base.legal.TownShipI;
import com.stewart.ats.base.name.Name;
import com.stewart.ats.base.name.NameI;
import com.stewart.ats.base.property.PropertyI;
import com.stewart.ats.base.search.DocumentsManagerI;
import com.stewart.ats.tsrindex.client.SimpleChapterUtils.DType;

/**
 * 
 * @author Oprina George
 * 
 *         Jul 25, 2011
 */

@SuppressWarnings("deprecation")
public class ARBentonRO extends TSServerROLike {

	/**
	 * 
	 */
	private static final long	serialVersionUID	= 1542730890062527574L;

	public ARBentonRO(long searchId) {
		super(searchId);
	}

	public ARBentonRO(String rsRequestSolverName, String rsSitePath,
			String rsServerID, String rsPrmNameLink, long searchId,
			int miServerID) {
		super(rsRequestSolverName, rsSitePath, rsServerID, rsPrmNameLink,
				searchId, miServerID);
	}

	@Override
	protected void ParseResponse(String sAction, ServerResponse Response,
			int viParseID) throws ServerResponseException {
		String rsResponse = Response.getResult();
		ParsedResponse parsedResponse = Response.getParsedResponse();

		if (rsResponse.indexOf("Page Access Error") > 0) {
			Response.getParsedResponse().setError(
					"Your session has expired! Please start a new search.");
			return;
		}

		if (rsResponse
				.indexOf("Your search returned no records.\nPlease enter a different set of criteria\nand try your search again.") > -1
				|| rsResponse.contains("error") || rsResponse.contains("Error")) {
			Response.getParsedResponse()
					.setError(
							"No results found for your query! Please change your search criteria and try again.");
			return;
		}

		switch (viParseID) {
		case ID_SEARCH_BY_NAME:
		case ID_SEARCH_BY_BOOK_AND_PAGE:
		case ID_SEARCH_BY_SALES:
		case ID_SEARCH_BY_SUBDIVISION_PLAT:

			mSearch.setAdditionalInfo("viParseID", viParseID);

			StringBuilder outputTable = new StringBuilder();
			Collection<ParsedResponse> smartParsedResponses = smartParseIntermediary(
					Response, rsResponse, outputTable);

			if (smartParsedResponses.size() == 0) {
				return;
			}

			parsedResponse.setHeader("<table border=\"1\">");
			parsedResponse.setFooter("</table>");

			parsedResponse.setResultRows(new Vector<ParsedResponse>(
					smartParsedResponses));
			parsedResponse.setOnlyResponse(outputTable.toString());

			if (getSearch().getSearchType() == Search.PARENT_SITE_SEARCH) {
				String header = CreateSaveToTSDFormHeader(
						URLConnectionReader.MULTIPLE_SAVE_TO_TSD_ACTION, "POST");
				String footer = "";

				header += makeHeader(viParseID);

				Object numberOfUnsavedDocument = GetAttribute(NUMBER_OF_UNSAVED_DOCUMENTS);

				// set prev next buttons
				String prev_next = getPrevNext(Response);

				if (numberOfUnsavedDocument != null
						&& numberOfUnsavedDocument instanceof Integer) {
					footer = "\n</table><br>"
							+ prev_next
							+ CreateSaveToTSDFormEnd(SAVE_SELECTED_DOCUMENTS_BUTTON_LABEL,
									viParseID,
									(Integer) numberOfUnsavedDocument);
				} else {
					footer = "\n</table><br>"
							+ prev_next
							+ CreateSaveToTSDFormEnd(SAVE_SELECTED_DOCUMENTS_BUTTON_LABEL,
									viParseID, -1);
				}

				parsedResponse.setHeader(header);
				parsedResponse.setFooter(footer);
			}

			break;

		case ID_DETAILS:
		case ID_SAVE_TO_TSD:

			StringBuilder accountId = new StringBuilder();

			HashMap<String, String> data = new HashMap<String, String>();

			String details = getDetails(rsResponse, accountId, data, Response);

			String accountName = accountId.toString();

			String filename = accountName + ".html";

			if (viParseID != ID_SAVE_TO_TSD) {
				String originalLink = sAction.replace("?", "&") + "&" + Response.getRawQuerry();
				String sSave2TSDLink = getLinkPrefix(TSConnectionURL.idPOST) + originalLink.replaceFirst("/http", "http");

				if (isInstrumentSaved("", null, data)) {
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
				Response.getParsedResponse().setResponse(
						details.replaceAll("(?ism)<a href[^>]*>[^/]*/a>", ""));

				msSaveToTSDResponce = details.replaceAll(
						"(?ism)<a href[^>]*>[^/]*/a>", "")
						+ CreateFileAlreadyInTSD();

				String resultForCross = details;

				Pattern crossRefLinkPattern = Pattern
						.compile("(?ism)<a href=[\\\"|'](.*?)[\\\"|']>");
				Matcher crossRefLinkMatcher = crossRefLinkPattern
						.matcher(resultForCross);
				while (crossRefLinkMatcher.find()) {
					ParsedResponse prChild = new ParsedResponse();
					String link = crossRefLinkMatcher.group(1)
							+ "&isSubResult=true";
					if (link.contains("viewIndex.asp")) {
						LinkInPage pl = new LinkInPage(link, link,
								TSServer.REQUEST_SAVE_TO_TSD);
						prChild.setPageLink(pl);
						Response.getParsedResponse().addOneResultRowOnly(
								prChild);
					}
				}
			}

			break;
		case ID_GET_LINK:
			ParseResponse(
					sAction,
					Response,
					(Integer) (rsResponse.contains("Document Index Detail") ? ID_DETAILS
							: (mSearch.getAdditionalInfo("viParseID") == null ? 1
									: mSearch.getAdditionalInfo("viParseID"))));
			break;
		default:
			break;
		}

	}

	private String getPrevNext(ServerResponse Response) {
		String rsResponse = Response.getResult();
		String prev_next = "";

		try {
			org.htmlparser.Parser htmlParser = org.htmlparser.Parser
					.createParser(rsResponse, null);

			NodeList nodeList = htmlParser.parse(null);

			nodeList = nodeList.extractAllNodesThatMatch(
					new TagNameFilter("div"), true).extractAllNodesThatMatch(
					new HasAttributeFilter("id", "paging_grouping_top"), true);

			nodeList = nodeList.extractAllNodesThatMatch(
					new HasAttributeFilter("class", "paging_controls"), true);

			if (nodeList.size() > 0) {

				Div prev_div = null;
				Div page_label = null;
				Div next_div = null;

				prev_next = "\n<table id=\"prev_next_links\">\n<tr>";

				String siteLinkPrefix = (String) mSearch.getAdditionalInfo(getDataSite().getName() + ":" + "siteLinkPrefix");

				if (StringUtils.isEmpty(siteLinkPrefix))
					throw new Exception("Problem with site link! verify connection class!");

				StringBuilder querry = new StringBuilder("");

				HashMap<String, String> map = ro.cst.tsearch.servers.functions.ARBentonRO.getParams(rsResponse, "frmResult");

				if (map != null) {
					for (String key : map.keySet()) {
						querry.append("&" + key + "=" + org.apache.commons.lang.StringUtils.defaultString(map.get(key)));
					}

				}

				String link = CreatePartialLink(TSConnectionURL.idPOST) + "PREV_NEXT"
						+ "SearchResults.asp" + querry.toString() + "&currentPage=";

				NodeList aux = nodeList
						.elementAt(0)
						.getChildren()
						.extractAllNodesThatMatch(new TagNameFilter("div"),
								true)
						.extractAllNodesThatMatch(
								new HasAttributeFilter("id", "icon_left_top"),
								true);

				if (aux.size() > 0) {
					prev_div = (Div) aux.elementAt(0);

					if (prev_div.getChildCount() > 1
							&& prev_div.getChild(1) instanceof LinkTag) {
						LinkTag l = (LinkTag) prev_div.getChild(1);

						if (l.getLink().split(",").length > 1) {
							String prev_link = l.getLink().split(",")[1];

							prev_next += "\n<td><a href=\"" + link + prev_link
									+ "\">Previous</a></td>";
						}
					}
				}

				aux = nodeList
						.elementAt(0)
						.getChildren()
						.extractAllNodesThatMatch(new TagNameFilter("div"),
								true)
						.extractAllNodesThatMatch(
								new HasAttributeFilter("class", "page_label"),
								true);

				if (aux.size() > 0) {
					page_label = (Div) aux.elementAt(0);

					String page_x_of_y = page_label.toPlainTextString();

					prev_next += "\n<td>" + page_x_of_y + "</td>";
				}

				aux = nodeList
						.elementAt(0)
						.getChildren()
						.extractAllNodesThatMatch(new TagNameFilter("div"),
								true)
						.extractAllNodesThatMatch(
								new HasAttributeFilter("id", "icon_right_top"),
								true);

				if (aux.size() > 0) {
					next_div = (Div) aux.elementAt(0);

					if (next_div.getChildCount() > 1
							&& next_div.getChild(1) instanceof LinkTag) {
						LinkTag l = (LinkTag) next_div.getChild(1);

						if (l.getLink().split(",").length > 1) {
							String next_link = l.getLink().split(",")[1];

							prev_next += "\n<td><a href=\"" + link + next_link
									+ "\">Next</a></td>";
						}
					}
				}

				prev_next += "\n</tr></table>\n";

			}

		} catch (Exception e) {
			e.printStackTrace();
		}

		return prev_next;
	}

	private String makeHeader(int viParseID) {
		String header = "";

		header += "<table cellspacing=\"0\" cellpadding=\"3\" border=\"1\" width=\"100%\">"
				+ "<TR bgcolor=\"#6699CC\" valign=\"top\">"
				+ "<TH ALIGN=Left>"
				+ SELECT_ALL_CHECKBOXES + "</TH>";

		switch (viParseID) {
		case ID_SEARCH_BY_NAME:
			header += "<TH ALIGN=Left>Index<br>Detail</TH>"
					+ "<TH ALIGN=Left>View<br>Image</TH>"
					+ "<TH ALIGN=Left>Searched Name</TH>"
					+ "<TH ALIGN=Left>Party Type</TH>"
					+ "<TH ALIGN=Left>Book Type</TH>"
					+ "<TH ALIGN=Left>Book<br>Page</TH>"
					+ "<TH ALIGN=Left>Filed Date</TH>"
					+ "<TH ALIGN=Left>Instrument Type</TH>"
					+ "<TH ALIGN=Left>Instrument<br>Date</TH>"
					+ "<TH ALIGN=Left>Opposite Name</TH>"
					+ "<TH ALIGN=Left>Amount</TH>"
					+ "<TH ALIGN=Left>Description</TH>"
					+ "<TH ALIGN=Left>Case #</TH>";
			break;
		case ID_SEARCH_BY_BOOK_AND_PAGE:
			header += "<TH ALIGN=Left>Index<br>Detail</TH>"
					+ "<TH ALIGN=Left>View<br>Image</TH>"
					+ "<TH ALIGN=Left>Book Type</TH>"
					+ "<TH ALIGN=Left>Book<br>Page</TH>"
					+ "<TH ALIGN=Left>Filed Date</TH>"
					+ "<TH ALIGN=Left>Instrument Date</TH>"
					+ "<TH ALIGN=Left>Instrument Type</TH>"
					+ "<TH ALIGN=Left>Grantor</TH>"
					+ "<TH ALIGN=Left>Grantee</TH>"
					+ "<TH ALIGN=Left>Amount</TH>"
					+ "<TH ALIGN=Left>Description</TH>"
					+ "<TH ALIGN=Left>Case #</TH>";
			break;
		case ID_SEARCH_BY_SALES:
			header += "<TH ALIGN=Left>Index<br>Detail</TH>"
					+ "<TH ALIGN=Left>View<br>Image</TH>"
					+ "<TH ALIGN=Left>Filed Date</TH>"
					+ "<TH ALIGN=Left>Instrument Type</TH>"
					+ "<TH ALIGN=Left>Book Type</TH>"
					+ "<TH ALIGN=Left>Book<br>Page</TH>"
					+ "<TH ALIGN=Left>Instrument Date</TH>"
					+ "<TH ALIGN=Left>Grantor</TH>"
					+ "<TH ALIGN=Left>Grantee</TH>"
					+ "<TH ALIGN=Left>Amount</TH>"
					+ "<TH ALIGN=Left>Description</TH>"
					+ "<TH ALIGN=Left>Case #</TH>";
			break;
		case ID_SEARCH_BY_SUBDIVISION_PLAT:
			header += "<TH ALIGN=Left>Index<br>Detail</TH>"
					+ "<TH ALIGN=Left>View<br>Image</TH>"
					+ "<TH ALIGN=Left>Lot List</TH>"
					+ "<TH ALIGN=Left>Block</TH>"
					+ "<TH ALIGN=Left>Subdivision</TH>"
					+ "<TH ALIGN=Left>City</TH>"
					+ "<TH ALIGN=Left>Section</TH>"
					+ "<TH ALIGN=Left>Township</TH>"
					+ "<TH ALIGN=Left>Range</TH>"
					+ "<TH ALIGN=Left>Book Type</TH>"
					+ "<TH ALIGN=Left>Book<br>Page</TH>"
					+ "<TH ALIGN=Left>Filed Date</TH>"
					+ "<TH ALIGN=Left>Grantor</TH>"
					+ "<TH ALIGN=Left>Grantee</TH>"
					+ "<TH ALIGN=Left>Instrument Type</TH>";
			break;
		}

		header += "</TR>";

		return header;
	}

	protected String createPartialLink(int iActionType, int dispatcher) {
		StringBuilder build = new StringBuilder(msRequestSolverName);
		build.append("?");
		build.append(msServerID);
		build.append("&");
		build.append(RequestParams.SEARCH_ID);
		build.append("=");
		build.append(mSearch.getSearchID());
		build.append("&");
		build.append("dispatcher=");
		build.append(dispatcher);
		return build.toString();
	}

	protected String getDetails(String rsResponse, StringBuilder accountId,
			HashMap<String, String> data, ServerResponse Response) {
		try {
			StringBuilder details = new StringBuilder();
			org.htmlparser.Parser htmlParser = org.htmlparser.Parser
					.createParser(rsResponse, null);

			NodeList nodeList = htmlParser.parse(null);

			// tables
			NodeList tableList = nodeList
					.extractAllNodesThatMatch(new TagNameFilter("table"), true)
					.extractAllNodesThatMatch(
							new HasAttributeFilter("border", "0"))
					.extractAllNodesThatMatch(new HasAttributeFilter("style"))
					.extractAllNodesThatMatch(
							new HasAttributeFilter("width", "100%"));

			if (tableList.size() != 1) {
				return null;
			}

			String book_type = HtmlParser3.getValueFromNextCell(
					HtmlParser3.findNode(tableList, "Book Type"), "", true);
			String book = HtmlParser3.getValueFromNextCell(
					HtmlParser3.findNode(tableList, "Book #"), "", true);
			String page = HtmlParser3.getValueFromNextCell(
					HtmlParser3.findNode(tableList, "Page #"), "", true);
			String instrument_date = HtmlParser3.getValueFromNextCell(
					HtmlParser3.findNode(tableList, "Instrument Date:"), "",
					true);

			String year = instrument_date.replaceAll("\\d+/\\d+/(\\d+)", "$1");

			String type = getTypeFromIntermediarySearch(book_type, book, page);

			if (StringUtils.isEmpty(type))
				type = "";

			data.put("type", type.replaceAll(" ",""));
			data.put("book", book);
			data.put("page", page);
			data.put("year", year);

			accountId.append(type.replaceAll(" ","") + book + page);

			String siteLinkPrefix = (String) mSearch.getAdditionalInfo(getDataSite().getName() + ":" + "siteLinkPrefix");

			if (StringUtils.isEmpty(siteLinkPrefix))
				throw new Exception("Problem with site link! verify connection class!");

			String view_image = siteLinkPrefix + "viewImage.asp"
					+ "?" + Response.getQuerry();
			view_image = view_image.replaceAll("(?ism)&parentSite=[^&]*", "");

			if (view_image.contains("&bPrev=")) {
				view_image = view_image.replace("?", "&");
				view_image = view_image.replaceAll("&iApplNum=[^&]*", "");
				view_image = view_image.replaceAll("&bPrev=[^&]*", "");
				view_image = view_image.replaceFirst("&", "?")
						+ "&iPamendID=&iImageID=&related=true";
			}

			if (HtmlParser3.getNodesByID("view_image_button_bottom", nodeList,
					true).size() > 0) {
				String fakeLink = siteLinkPrefix + "viewImage.asp" + "?type="+type+"&book="+book+"&page="+page;
							
				Response.getParsedResponse().addImageLink(
						new ImageLinkInPage(fakeLink, accountId.toString()
								+ ".tif"));
			}

			/* If from memory - use it as is */
			if (!rsResponse.contains("<html")) {
				NodeList headerList = nodeList.extractAllNodesThatMatch(
						new TagNameFilter("table"), true);

				if (headerList.size() == 0) {
					return null;
				}
				return rsResponse;
			}

			TableTag table = (TableTag) tableList.elementAt(0);

			details.append("<p align=\"center\"><font style=\"font-size:xx-large;\"><b>Document Index Detail <br> Benton County</b></font></p>"
					+ "<br>");

			details.append("\n<br>\n<table align='left'><tr><td>"
					+ "Instrument Type:</td>\n<td>" + type
					+ "</td>\n</tr>\n</table>");

			// fake table here -> fake image and details links for related
			// documents
			NodeList tables = nodeList.extractAllNodesThatMatch(
					new TagNameFilter("table"), true);

			Node[] tables_array = tables.toNodeArray();

			Arrays.sort(tables_array,
					new ro.cst.tsearch.servers.functions.NVClarkTR.NodeComp());

			TableTag crossref_table = ro.cst.tsearch.servers.functions.NVClarkTR
					.getTableByContent(tables_array,
							new String[] { "Book #", "Page #", "Book Type",
									"Index Detail", "View Image" });

			String related_doc_link = CreatePartialLink(TSConnectionURL.idPOST)
					+ siteLinkPrefix + "viewIndex.asp";
			String related_img_link = CreatePartialLink(TSConnectionURL.idPOST)
					+ siteLinkPrefix + "viewImage.asp";

			if (crossref_table != null) {
				TableRow[] rows = crossref_table.getRows();
				for (int j = 1; j < rows.length; j++) {
					if (rows[j].getColumnCount() == 5) {
						// String rel_book_type = rows[j].getColumns()[2]
						// .toPlainTextString();
						// String rel_book = rows[j].getColumns()[0]
						// .toPlainTextString();
						// String rel_page = rows[j].getColumns()[1]
						// .toPlainTextString();

						if (rows[j].getColumns()[3].getChildCount() > 0
								&& rows[j].getColumns()[3].getChild(0) instanceof LinkTag) {
							String link = ((LinkTag) rows[j].getColumns()[3]
									.getChild(0)).getLink();

							String new_link = link
									.replaceAll("viewRelatedIndex\\('(\\d+)','(\\d+)'\\);","&reldetail=true&iApplNum=$2&bPrev=false&iRecordID=$1&iDataSetNum=$2");
							((LinkTag) rows[j].getColumns()[3].getChild(0))
									.setLink(related_doc_link + new_link);
						}

						if (rows[j].getColumns()[4].getChildCount() > 0
								&& rows[j].getColumns()[4].getChild(0) instanceof LinkTag) {
							String link = ((LinkTag) rows[j].getColumns()[4]
									.getChild(0)).getLink();

							String new_link = link
									.replaceAll("viewRelatedImage\\('(\\d+)','(\\d+)'\\);","&related=true&iRecordID=$1&iPamendID=&iImageID=&iDataSetNum=$2");
							((LinkTag) rows[j].getColumns()[4].getChild(0))
									.setLink(related_img_link + new_link);
						}
					}
				}
			}

			String table_s = table.toHtml();

			// replace images
			table_s = table_s
					.replaceAll(
							"(?ism)<img src=\"images/view_detail_small.gif\" border=\"0\" title=\"View Related Document Index\">",
							"View Related Document Index");

			table_s = table_s
					.replaceAll(
							"(?ism)<img src=\"images/view_image_small.gif\" border=\"0\" title=\"View Related Document Image\">",
							"View Related Document Image");

			// remove other links
			table_s = table_s.replaceAll(
					"<a href=\"/title-search/URLConnectionReader",
					"<aFAKE href=\"/title-search/URLConnectionReader");

			table_s = table_s.replaceAll("(?ism)<a href=[^>]*>([^<]*)<[^>]*>",
					"$1");

			table_s = table_s.replaceAll("Previously Viewed Related Indexes:",
					"");

			table_s = table_s.replaceAll("<aFAKE", "<a");

			details.append(table_s);

			// put image link
			if (HtmlParser3.getNodesByID("view_image_button_bottom", nodeList,
					true).size() > 0) {
				details.append("<table><tr><td><a href=\""
						+ CreatePartialLink(TSConnectionURL.idPOST)
						+ view_image + "\">View Image</a></td></tr></table>");
			}

			return details.toString();
		} catch (Exception e) {
			e.printStackTrace();
		}
		return "";
	}

	private String getTypeFromIntermediarySearch(String book_type, String book,
			String page) {
		if (StringUtils.isEmpty(book_type) || StringUtils.isEmpty(book)
				|| StringUtils.isEmpty(page))
			return null;

		String siteLinkPrefix = (String) mSearch.getAdditionalInfo(getDataSite().getName() + ":" + "siteLinkPrefix");

		if (StringUtils.isEmpty(siteLinkPrefix)) {
			try {
				throw new Exception("Problem with site link! verify connection class!");
			} catch (Exception e1) {
				e1.printStackTrace();
			}
			return null;
		}

		HTTPRequest req = new HTTPRequest(siteLinkPrefix + "SearchResults.asp", HTTPRequest.POST);

		req.setPostParameter("SearchDocType", "ALL");
		req.setPostParameter("SearchbyDateFrom", "");
		req.setPostParameter("SearchbyDateTo", "");
		req.setPostParameter("SearchbyPageTo", "");
		req.setPostParameter("sSearchType", "BP");
		req.setPostParameter("SearchBookType", book_type);
		req.setPostParameter("SearchbyBook", book);
		req.setPostParameter("SearchbyPageFrom", page);

		HttpSite site = HttpManager.getSite(getCurrentServerName(), searchId);

		String result = "";

		try {
			result = site.process(req).getResponseAsString();
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			// always release the HttpSite
			HttpManager.releaseSite(site);
		}

		if (StringUtils.isEmpty(result))
			return null;

		try {

			org.htmlparser.Parser htmlParser = org.htmlparser.Parser
					.createParser(result, null);
			NodeList nodeList = htmlParser.parse(null);
			NodeList mainTableList = nodeList
					.extractAllNodesThatMatch(new TagNameFilter("div"), true)
					.extractAllNodesThatMatch(
							new HasAttributeFilter("id", "results"), true)
					.extractAllNodesThatMatch(new TagNameFilter("table"), true);

			if (!(mainTableList.size() > 0)) {
				return null;
			}

			TableTag t = (TableTag) mainTableList.elementAt(0);

			TableRow[] rows = t.getRows();

			if (rows.length != 2)
				return null;

			TableRow row = rows[1];
			if (row.getColumnCount() == 12) {
				return org.apache.commons.lang.StringUtils.strip(row
						.getColumns()[6].toPlainTextString());
			}

		} catch (Exception e) {
			e.printStackTrace();
		}

		return null;
	}

	private static Set<InstrumentI> getAllAoAndTaxReferences(
			Search search) {
		DocumentsManagerI manager = search.getDocManager();
		Set<InstrumentI> allAoRef = new HashSet<InstrumentI>();
		try {
			manager.getAccess();
			List<DocumentI> list = manager.getDocumentsWithType(true,
					DType.ASSESOR, DType.TAX);
			for (DocumentI assessor : list) {
				if (HashCountyToIndex.isLegalBootstrapEnabled(search.getCommId(), assessor.getSiteId())) {
					for (RegisterDocumentI reg : assessor.getReferences()) {
						allAoRef.add(reg.getInstrument());
					}
					allAoRef.addAll(assessor.getParsedReferences());
				}
			}
		} finally {
			manager.releaseAccess();
		}
		return removeEmptyReferences(allAoRef);
	}

	private static Set<InstrumentI> removeEmptyReferences(Set<InstrumentI> allAo) {
		Set<InstrumentI> ret = new HashSet<InstrumentI>();
		for (InstrumentI i : allAo) {
			if (i.hasBookPage() || i.hasInstrNo()) {
				ret.add(i);
			}
		}
		return ret;
	}

	private boolean addAoAndTaxReferenceSearches(TSServerInfo serverInfo, List<TSServerInfoModule> modules,
			Set<InstrumentI> allAoRef, long searchId, boolean isUpdate) {
		boolean atLeastOne = false;
		final Set<String> searched = new HashSet<String>();

		for (InstrumentI inst : allAoRef) {
			boolean temp = addBookPageSearch(inst, serverInfo, modules, searchId, searched, isUpdate);
			atLeastOne = atLeastOne || temp;
		}
		return atLeastOne;
	}

	private static boolean addBookPageSearch(InstrumentI inst,
			TSServerInfo serverInfo, List<TSServerInfoModule> modules,
			long searchId, Set<String> searched, boolean isUpdate) {

		if (inst.hasBookPage()) {
			String originalB = inst.getBook();
			String originalP = inst.getPage();

			String book = originalB.replaceFirst("^0+", "");
			String page = originalP.replaceFirst("^0+", "");
			if (!searched.contains(book + "_" + page)) {
				searched.add(book + "_" + page);
			} else {
				return false;
			}

			TSServerInfoModule module = new TSServerInfoModule(
					serverInfo.getModule(TSServerInfo.BOOK_AND_PAGE_MODULE_IDX));
			module.setData(0, book);
			module.setData(1, page);
			HashMap<String, String> filterCriteria = new HashMap<String, String>();
			filterCriteria.put("Book", book);
			filterCriteria.put("Page", page);
			GenericInstrumentFilter filter = new GenericInstrumentFilter(
					searchId, filterCriteria);
			module.addFilter(filter);
			
			if(!inst.getDocType().equalsIgnoreCase("MISCELLANEOUS") && !inst.getDocSubType().equalsIgnoreCase("MISCELLANEOUS")){
				DocTypeSimpleFilter docTypefilter = new DocTypeSimpleFilter(searchId);
				docTypefilter.setDocTypes(new String[]{inst.getDocType(),inst.getDocSubType()});
				module.addFilter(docTypefilter);
			}
			
			ExactDateFilterResponse dateFilter = new ExactDateFilterResponse(searchId);
			if(inst instanceof RegisterDocument) {
				dateFilter.addFilterDate(((RegisterDocumentI)inst).getRecordedDate());
				dateFilter.addFilterDate(((RegisterDocumentI)inst).getInstrumentDate());
			} else {
				dateFilter.addFilterDate(inst.getDate());
			}
			if(!dateFilter.getFilterDates().isEmpty())
				module.addFilter(dateFilter);
			modules.add(module);
			return true;
		}
		return false;
	}

	static protected class LegalDescriptionIterator extends
			GenericRuntimeIterator<PersonalDataStruct> {

		private static final long	serialVersionUID		= 9238625486117069L;
		private boolean				enableSubdividedLegal	= false;
		private boolean				enableTownshipLegal		= false;

		LegalDescriptionIterator(long searchId) {
			super(searchId);
		}

		public boolean isEnableSubdividedLegal() {
			return enableSubdividedLegal;
		}

		public void setEnableSubdividedLegal(boolean enableSubdividedLegal) {
			this.enableSubdividedLegal = enableSubdividedLegal;
		}

		public boolean isEnableTownshipLegal() {
			return enableTownshipLegal;
		}

		public void setEnableTownshipLegal(boolean enableTownshipLegal) {
			this.enableTownshipLegal = enableTownshipLegal;
		}

		@SuppressWarnings("unchecked")
		List<PersonalDataStruct> createDerivationInternal(long searchId) {
			Search global = InstanceManager.getManager()
					.getCurrentInstance(searchId).getCrtSearchContext();
			DocumentsManagerI m = global.getDocManager();
			String key = "AR_BENTON_RO_LOOK_UP_DATA";
			if (isEnableSubdividedLegal())
				key += "_SPL";
			else if (isEnableTownshipLegal())
				key += "_STR";
			List<PersonalDataStruct> legalStructList = (List<PersonalDataStruct>) global
					.getAdditionalInfo(key);

			String aoAndTrLots = global.getSa().getAtribute(
					SearchAttributes.LD_LOTNO);
			String[] allAoAndTrlots = new String[0];

			if (!StringUtils.isEmpty(aoAndTrLots)) {
				Vector<LotInterval> lots = LotMatchAlgorithm
						.prepareLotInterval(aoAndTrLots);
				HashSet<String> lotExpanded = new LinkedHashSet<String>();
				for (Iterator<LotInterval> iterator = lots.iterator(); iterator
						.hasNext();) {
					lotExpanded.addAll(((LotInterval) iterator.next())
							.getLotList());
				}
				allAoAndTrlots = lotExpanded.toArray(allAoAndTrlots);
			}
			boolean hasLegal = false;
			if (legalStructList == null) {
				legalStructList = new ArrayList<PersonalDataStruct>();

				try {

					m.getAccess();
					List<RegisterDocumentI> listRodocs = m
							.getRoLikeDocumentList(true);
					DocumentUtils.sortDocuments(listRodocs,
							MultilineElementsMap.DATE_ORDER_ASC);
					for (RegisterDocumentI reg : listRodocs) {
						for (PropertyI prop : reg.getProperties()) {
							if (prop.hasLegal()) {
								LegalI legal = prop.getLegal();

								if (legal.hasSubdividedLegal()
										&& isEnableSubdividedLegal()) {
									hasLegal = true;
									PersonalDataStruct legalStructItem = new PersonalDataStruct();
									SubdivisionI subdiv = legal
											.getSubdivision();

									String subName = subdiv.getName();
									String block = subdiv.getBlock();
									String lot = subdiv.getLot();
									String[] lots = lot.split("  ");
									if (StringUtils.isNotEmpty(subName)) {
										for (int i = 0; i < lots.length; i++) {
											legalStructItem = new PersonalDataStruct();
											legalStructItem.subName = subName;
											legalStructItem.block = StringUtils
													.isEmpty(block) ? ""
													: block;
											legalStructItem.lot = lots[i];
											if (!testIfExist(legalStructList,
													legalStructItem,
													"subdivision")) {
												legalStructList
														.add(legalStructItem);
											}
										}
									}
								}
								if (legal.hasTownshipLegal()
										&& isEnableTownshipLegal()) {
									PersonalDataStruct legalStructItem = new PersonalDataStruct();
									TownShipI township = legal.getTownShip();

									String sec = township.getSection();
									String tw = township.getTownship();
									String rg = township.getRange();
									if (StringUtils.isNotEmpty(sec)) {
										legalStructItem.section = StringUtils
												.isEmpty(sec) ? "" : sec;
										legalStructItem.township = StringUtils
												.isEmpty(tw) ? "" : tw;
										legalStructItem.range = StringUtils
												.isEmpty(rg) ? "" : rg;

										if (!testIfExist(legalStructList,
												legalStructItem, "sectional")) {
											legalStructList
													.add(legalStructItem);
										}
									}
								}
							}
						}
					}

					global.setAdditionalInfo(key, legalStructList);

				} finally {
					m.releaseAccess();
				}
			} else {
				for (PersonalDataStruct struct : legalStructList) {
					if (StringUtils.isNotEmpty(struct.subName)) {
						hasLegal = true;
					}
				}
				if (hasLegal) {
					legalStructList = new ArrayList<PersonalDataStruct>();
				}
			}
			return legalStructList;
		}

		protected List<PersonalDataStruct> createDerrivations() {
			return createDerivationInternal(searchId);
		}

		protected void loadDerrivation(TSServerInfoModule module,
				PersonalDataStruct str) {
			FilterResponse lotFilter = LegalFilterFactory
					.getDefaultLotFilter(searchId);
			FilterResponse blockFilter = LegalFilterFactory
					.getDefaultBlockFilter(searchId);

			if (module
					.getExtraInformation(
							TSServerInfoConstants.EXTRA_PARAM_MODULE_DESCRIPTION)
					.equals(TSServerInfoConstants.VALUE_PARAM_SUBDIVISION_LOT_BLOCK)) {
				if (StringUtils.isNotEmpty(str.subName)
						|| StringUtils.isNotEmpty(str.lot)
						|| StringUtils.isNotEmpty(str.block)) {
					module.setData(0, StringUtils.isNotEmpty(str.lot) ? str.lot
							: "");
					module.setData(1,
							StringUtils.isNotEmpty(str.block) ? str.block : "");
					module.setData(2,
							StringUtils.isNotEmpty(str.subName) ? str.subName
									: "");
					module.addValidator(lotFilter.getValidator());
					module.addValidator(blockFilter.getValidator());
					module.setVisible(true);
				} else {
					module.setVisible(false);
				}
			} else if (module
					.getExtraInformation(
							TSServerInfoConstants.EXTRA_PARAM_MODULE_DESCRIPTION)
					.equals(TSServerInfoConstants.VALUE_PARAM_NAME_SUBDIVISION_SECTION_TWP_RANGE)) {
				if (StringUtils.isNotEmpty(str.section)
						|| StringUtils.isNotEmpty(str.township)
						|| StringUtils.isNotEmpty(str.range)) {
					module.setData(4,
							StringUtils.isNotEmpty(str.section) ? str.section
									: "");
					module.setData(5,
							StringUtils.isNotEmpty(str.township) ? str.township
									: "");
					module.setData(6,
							StringUtils.isNotEmpty(str.range) ? str.range : "");
					module.setVisible(true);
				} else {
					module.setVisible(false);
				}
			}
		}
	}

	protected static class PersonalDataStruct implements Cloneable {
		String	subName		= "";
		String	lot			= "";
		String	block		= "";
		String	section		= "";
		String	township	= "";
		String	range		= "";

		@Override
		protected Object clone() throws CloneNotSupportedException {
			return super.clone();
		}

		public boolean equalsSubdivision(PersonalDataStruct struct) {
			return this.block.equals(struct.block)
					&& this.lot.equals(struct.lot)
					&& this.subName.equals(struct.subName);
		}

		public boolean equalsSectional(PersonalDataStruct struct) {
			return this.section.equals(struct.section)
					&& this.township.equals(struct.township)
					&& this.range.equals(struct.range);
		}

	}

	private static boolean testIfExist(List<PersonalDataStruct> legalStruct2,
			PersonalDataStruct l, String string) {

		if ("subdivision".equalsIgnoreCase(string)) {
			for (PersonalDataStruct p : legalStruct2) {
				if (l.equalsSubdivision(p)) {
					return true;
				}
			}
		} else if ("sectional".equalsIgnoreCase(string)) {
			for (PersonalDataStruct p : legalStruct2) {
				if (l.equalsSectional(p)) {
					return true;
				}
			}
		}
		return false;
	}

	private void addIteratorModule(TSServerInfo serverInfo,
			List<TSServerInfoModule> modules, int code, long searchId) {
		FilterResponse lotFilter = LegalFilterFactory
				.getDefaultLotFilter(searchId);
		FilterResponse blockFilter = LegalFilterFactory
				.getDefaultBlockFilter(searchId);
		TSServerInfoModule module = new TSServerInfoModule(
				serverInfo.getModule(code));
		module.addExtraInformation(
				TSServerInfoConstants.EXTRA_PARAM_MODULE_DESCRIPTION,
				TSServerInfoConstants.VALUE_PARAM_SUBDIVISION_LOT_BLOCK);
		module.clearSaKeys();
		LegalDescriptionIterator it = new LegalDescriptionIterator(searchId);
		it.setEnableSubdividedLegal(true);
		module.addValidator(blockFilter.getValidator());
		module.addValidator(lotFilter.getValidator());
		module.addIterator(it);
		modules.add(module);

	}

	@Override
	protected void setModulesForAutoSearch(TSServerInfo serverInfo) {
		List<TSServerInfoModule> modules = new ArrayList<TSServerInfoModule>();
		Search search = getSearch();
		
		Set<InstrumentI> allAoRef = getAllAoAndTaxReferences(search);

		TSServerInfoModule m = null;
		SearchAttributes sa = search.getSa();

		FilterResponse rejectSavedDocuments = new RejectAlreadySavedDocumentsFilterResponse(searchId);
		FilterResponse defaultLegalFilter = LegalFilterFactory.getDefaultLegalFilter(searchId);
		((GenericLegal) defaultLegalFilter).setEnableLot(true);

		FilterResponse subdivisionNameFilter = NameFilterFactory.getDefaultNameFilterForSubdivision(searchId);
		subdivisionNameFilter.setThreshold(new BigDecimal(0.90d));

		// P1 instrument list search from AO and TR for finding Legal
		addAoAndTaxReferenceSearches(serverInfo, modules, allAoRef, searchId, isUpdate());

		// P2 search with lot/block/subdivision from RO documents
		addIteratorModule(serverInfo, modules, TSServerInfo.SUBDIVISION_MODULE_IDX, searchId);

		// P3 search with sec/twp/rng from RO documents
		addIteratorModuleSTR(serverInfo, modules, TSServerInfo.SUBDIVISION_MODULE_IDX, searchId);

		// P4 search by sub name
		String subdivisionName = sa
				.getAtribute(SearchAttributes.LD_SUBDIV_NAME);
		if (StringUtils.isNotEmpty(subdivisionName)) {
			m = new TSServerInfoModule(serverInfo.getModule(TSServerInfo.SUBDIVISION_MODULE_IDX));
			m.addExtraInformation(
					TSServerInfoConstants.EXTRA_PARAM_MODULE_DESCRIPTION,
					TSServerInfoConstants.VALUE_PARAM_SUBDIVISION_LOT);
			m.clearSaKeys();
			m.forceValue(2, subdivisionName);
			m.setSaKey(8, SearchAttributes.FROMDATE_MM_SLASH_DD_SLASH_YYYY);
			m.setSaKey(9, SearchAttributes.TODATE_MM_SLASH_DD_SLASH_YYYY);
			m.addFilter(subdivisionNameFilter);
			m.addValidator(defaultLegalFilter.getValidator());

			modules.add(m);
		} 

		ConfigurableNameIterator nameIterator = null;
		GenericNameFilter defaultNameFilter = (GenericNameFilter) NameFilterFactory.getDefaultNameFilter(
				SearchAttributes.OWNER_OBJECT, searchId, m);
		defaultNameFilter.setIgnoreMiddleOnEmpty(true);
		defaultNameFilter.setUseArrangements(false);
		defaultNameFilter.setInitAgain(true);

		// P5 name modules with names from search page.
		if (hasOwner()) {
			m = new TSServerInfoModule(serverInfo.getModule(TSServerInfo.NAME_MODULE_IDX));
			m.addExtraInformation(
					TSServerInfoConstants.EXTRA_PARAM_MODULE_DESCRIPTION,
					TSServerInfoConstants.VALUE_PARAM_NAME_OWNERS);
			m.setSaObjKey(SearchAttributes.OWNER_OBJECT);
			m.clearSaKeys();
			m.setSaKey(4, SearchAttributes.FROMDATE_MM_SLASH_DD_SLASH_YYYY);
			m.setSaKey(5, SearchAttributes.TODATE_MM_SLASH_DD_SLASH_YYYY);

			
			m.addFilter(rejectSavedDocuments);
			m.addFilter(defaultNameFilter);
			m.addFilter(new LastTransferDateFilter(searchId));
			addFilterForUpdate(m, true);

			m.addValidator(defaultLegalFilter.getValidator());
			m.setIteratorType(0,
					FunctionStatesIterator.ITERATOR_TYPE_LFM_NAME_FAKE);
			nameIterator = (ConfigurableNameIterator) ModuleStatesIteratorFactory
					.getConfigurableNameIterator(m, searchId, new String[] {
							"L;F;M", "L;F;" });
			m.addIterator(nameIterator);
			modules.add(m);
		}

		// P5 search by buyers
		if (hasBuyer() && !search.isProductType(SearchAttributes.SEARCH_PROD_REFINANCE)) {
			m = new TSServerInfoModule(serverInfo.getModule(TSServerInfo.NAME_MODULE_IDX));
			m.addExtraInformation(
					TSServerInfoConstants.EXTRA_PARAM_MODULE_DESCRIPTION,
					TSServerInfoConstants.VALUE_PARAM_NAME_BUYERS);
			m.setSaObjKey(SearchAttributes.BUYER_OBJECT);
			m.clearSaKeys();
			m.setSaKey(4, SearchAttributes.FROMDATE_MM_SLASH_DD_SLASH_YYYY);
			m.setSaKey(5, SearchAttributes.TODATE_MM_SLASH_DD_SLASH_YYYY);

			FilterResponse nameFilter = NameFilterFactory.getDefaultNameFilter(
					SearchAttributes.BUYER_OBJECT, searchId, m);
			((GenericNameFilter) nameFilter).setIgnoreMiddleOnEmpty(true);
			((GenericNameFilter) nameFilter).setUseArrangements(false);
			((GenericNameFilter) nameFilter).setInitAgain(true);
			m.addFilter(rejectSavedDocuments);
			m.addFilter(nameFilter);
			addFilterForUpdate(m, true);

			m.addValidator(defaultLegalFilter.getValidator());
			m.setIteratorType(0,
					FunctionStatesIterator.ITERATOR_TYPE_LFM_NAME_FAKE);
			m.setIteratorType(4, FunctionStatesIterator.ITERATOR_TYPE_FROM_DATE);
			ConfigurableNameIterator buyerNameIterator = (ConfigurableNameIterator) ModuleStatesIteratorFactory
					.getConfigurableNameIterator(m, searchId, new String[] {
							"L;F;M", "L;F;" });
			buyerNameIterator.setAllowMcnPersons(false);
			m.addIterator(buyerNameIterator);
			modules.add(m);
		}

		// P6 OCR last transfer - book page search
		m = new TSServerInfoModule(
				serverInfo.getModule(TSServerInfo.BOOK_AND_PAGE_MODULE_IDX));
		m.clearSaKeys();
		m.setIteratorType(ModuleStatesIterator.TYPE_OCR_FULL_OR_BOOTSTRAPER);
		m.getFunction(0).setIteratorType(
				FunctionStatesIterator.ITERATOR_TYPE_BOOK_SEARCH);
		m.getFunction(1).setIteratorType(
				FunctionStatesIterator.ITERATOR_TYPE_PAGE_SEARCH);
		m.addFilter(new GenericInstrumentFilter(searchId));
		m.addFilter(defaultLegalFilter);
		modules.add(m);

		// P7 name module with names added by OCR
		m = new TSServerInfoModule(serverInfo.getModule(TSServerInfo.NAME_MODULE_IDX));
		m.addExtraInformation(
				TSServerInfoConstants.EXTRA_PARAM_MODULE_DESCRIPTION,
				TSServerInfoConstants.VALUE_PARAM_NAME_OWNERS);
		m.setSaObjKey(SearchAttributes.OWNER_OBJECT);
		m.clearSaKeys();

		m.addFilter(rejectSavedDocuments);
		m.addFilter(defaultNameFilter);
		m.addFilter(new LastTransferDateFilter(searchId));
		m.addValidator(defaultLegalFilter.getValidator());
		m.setSaKey(4, SearchAttributes.FROMDATE_MM_SLASH_DD_SLASH_YYYY);
		m.setSaKey(5, SearchAttributes.TODATE_MM_SLASH_DD_SLASH_YYYY);
		m.setIteratorType(0, FunctionStatesIterator.ITERATOR_TYPE_LFM_NAME_FAKE);
		m.setIteratorType(4, FunctionStatesIterator.ITERATOR_TYPE_FROM_DATE);
		ArrayList<NameI> searchedNames = null;
		if (nameIterator != null) {
			searchedNames = nameIterator.getSearchedNames();
		} else {
			searchedNames = new ArrayList<NameI>();
		}
		nameIterator = (ConfigurableNameIterator) ModuleStatesIteratorFactory
				.getConfigurableNameIterator(m, searchId, new String[] {
						"L;F;M", "L;F;" });
		// get your values at runtime
		nameIterator.setInitAgain(true);
		nameIterator.setSearchedNames(searchedNames);
		m.addIterator(nameIterator);
		modules.add(m);

		serverInfo.setModulesForAutoSearch(modules);
	}

	private void addIteratorModuleSTR(TSServerInfo serverInfo,
			List<TSServerInfoModule> modules, int code, long searchId) {

		TSServerInfoModule module = new TSServerInfoModule(
				serverInfo.getModule(code));
		module.addExtraInformation(
				TSServerInfoConstants.EXTRA_PARAM_MODULE_DESCRIPTION,
				TSServerInfoConstants.VALUE_PARAM_NAME_SUBDIVISION_SECTION_TWP_RANGE);

		FilterResponse nameFilter = NameFilterFactory.getDefaultNameFilter(
				SearchAttributes.OWNER_OBJECT, searchId, module);
		((GenericNameFilter) nameFilter).setIgnoreMiddleOnEmpty(true);
		((GenericNameFilter) nameFilter).setUseArrangements(false);
		((GenericNameFilter) nameFilter).setInitAgain(true);
		FilterResponse defaultLegalFilter = LegalFilterFactory
				.getDefaultLegalFilter(searchId);

		module.clearSaKeys();
		LegalDescriptionIterator it = new LegalDescriptionIterator(searchId);
		it.setEnableTownshipLegal(true);
		module.addFilter(nameFilter);
		module.addFilter(defaultLegalFilter);
		module.addIterator(it);
		modules.add(module);
	}

	@Override
	protected void setModulesForGoBackOneLevelSearch(TSServerInfo serverInfo) {
		ConfigurableNameIterator nameIterator = null;
		String endDate = new SimpleDateFormat("MM/dd/yyyy").format(new Date());

		SearchAttributes sa = InstanceManager.getManager()
				.getCurrentInstance(searchId).getCrtSearchContext().getSa();
		List<TSServerInfoModule> modules = new ArrayList<TSServerInfoModule>();
		TSServerInfoModule module;
		GBManager gbm = (GBManager) sa
				.getObjectAtribute(SearchAttributes.GB_MANAGER_OBJECT);

		for (String id : gbm.getGbTransfers()) {

			module = new TSServerInfoModule(
					serverInfo.getModule(TSServerInfo.NAME_MODULE_IDX));
			module.setIndexInGB(id);
			module.setTypeSearchGB("grantor");
			module.clearSaKeys();
			module.setSaObjKey(SearchAttributes.GB_MANAGER_OBJECT);

			String date = gbm.getDateForSearch(id, "MM/dd/yyyy", searchId);
			if (date != null) {
				module.getFunction(4).forceValue(date);
			}
			module.setValue(5, endDate);

			module.addFilter(NameFilterFactory.getDefaultNameFilter(
					SearchAttributes.GB_MANAGER_OBJECT, searchId, module));
			module.setIteratorType(0,
					FunctionStatesIterator.ITERATOR_TYPE_LFM_NAME_FAKE);
			nameIterator = (ConfigurableNameIterator) ModuleStatesIteratorFactory
					.getConfigurableNameIterator(module, searchId,
							new String[] { "L;F;M", "L;F;" });
			module.addIterator(nameIterator);

			module.addFilter(NameFilterFactory.getDefaultTransferNameFilter(
					searchId, 0.90d, module));
			module.addFilter(DateFilterFactory.getDateFilterForGoBack(
					SearchAttributes.GB_MANAGER_OBJECT, searchId, module));
			modules.add(module);

			if (gbm.getNamesForBrokenChain(id, searchId).size() > 0) {
				module = new TSServerInfoModule(
						serverInfo.getModule(TSServerInfo.NAME_MODULE_IDX));
				module.setIndexInGB(id);
				module.setTypeSearchGB("grantee");
				module.clearSaKeys();
				module.setSaObjKey(SearchAttributes.GB_MANAGER_OBJECT);
				date = gbm.getDateForSearchBrokenChain(id, "MM/dd/yyyy",
						searchId);
				if (date != null)
					module.getFunction(4).forceValue(date);
				module.setValue(5, endDate);

				module.setIteratorType(0,
						FunctionStatesIterator.ITERATOR_TYPE_LFM_NAME_FAKE);
				module.addFilter(NameFilterFactory.getDefaultNameFilter(
						SearchAttributes.GB_MANAGER_OBJECT, searchId, module));
				nameIterator = (ConfigurableNameIterator) ModuleStatesIteratorFactory
						.getConfigurableNameIterator(module, searchId,
								new String[] { "L;F;M", "L;F;" });
				module.addIterator(nameIterator);

				module.addFilter(NameFilterFactory
						.getDefaultTransferNameFilter(searchId, 0.90d, module));
				module.addFilter(DateFilterFactory.getDateFilterForGoBack(
						SearchAttributes.GB_MANAGER_OBJECT, searchId, module));
				modules.add(module);

			}
		}

		serverInfo.setModulesForGoBackOneLevelSearch(modules);
	}

	@Override
	protected LegalI getLegalFromModule(TSServerInfoModule module) {
		LegalI legal = new Legal();

		if (module.getModuleIdx() == TSServerInfo.SUBDIVISION_MODULE_IDX
				&& module.getFunctionCount() > 11) {
			SubdivisionI subdivision = new Subdivision();

			subdivision.setName(module.getFunction(2).getParamValue().trim());
			subdivision.setLot(module.getFunction(0).getParamValue().trim());
			subdivision.setBlock(module.getFunction(1).getParamValue().trim());

			TownShipI townShip = new TownShip();

			townShip.setSection(module.getFunction(4).getParamValue().trim());
			townShip.setTownship(module.getFunction(5).getParamValue().trim());
			townShip.setRange(module.getFunction(6).getParamValue().trim());

			legal = new Legal();
			legal.setSubdivision(subdivision);
			legal.setTownShip(townShip);
		}

		return legal;
	}

	@Override
	protected NameI getNameFromModule(TSServerInfoModule module) {
		NameI name = new Name();
		if (module.getModuleIdx() == TSServerInfo.NAME_MODULE_IDX
				&& module.getFunctionCount() > 1) {
			String usedName = module.getFunction(0).getParamValue();
			if (StringUtils.isEmpty(usedName)) {
				return null;
			}
			String[] names = null;
			if (NameUtils.isCompany(usedName)) {
				names = new String[] { "", "", usedName, "", "", "" };
			} else {
				names = StringFormats.parseNameNashville(usedName, true);
			}
			name.setLastName(names[2]);
			name.setFirstName(names[0]);
			name.setMiddleName(names[1]);
			return name;
		}
		return null;
	}

	@Override
	public TSServerInfoModule getRecoverModuleFrom(
			RestoreDocumentDataI restoreDocumentDataI) {
		if (restoreDocumentDataI == null) {
			return null;
		}

		TSServerInfoModule module = null;

		String book = restoreDocumentDataI.getBook();
		String page = restoreDocumentDataI.getPage();

		if (StringUtils.isNotEmpty(book) && StringUtils.isNotEmpty(page)) {
			HashMap<String, String> filterCriteria = new HashMap<String, String>();
			module = getDefaultServerInfo().getModule(
					TSServerInfo.BOOK_AND_PAGE_MODULE_IDX);
			filterCriteria.put("Book", book);
			filterCriteria.put("Page", page);
			module.forceValue(0, book);
			module.forceValue(1, page);
			module.forceValue(3, "ALL");
			module.forceValue(6, "ALL");
			module.forceValue(7, "BP");
			
			ExactDateFilterResponse dateFilter = new ExactDateFilterResponse(searchId);
			
			dateFilter.addFilterDate(restoreDocumentDataI.getRecordedDate());
			module.getFilterList().clear();
			
			if(!dateFilter.getFilterDates().isEmpty()) {
				module.addFilter(dateFilter);
			}

		}
		return module;
	}

	@Override
	protected String getInstrumentNumberForSavingInFinalResults(DocumentI doc) {
		return (doc.getDocType() + doc.getBook() + doc.getPage()).toUpperCase();
	}

	@Override
	public Collection<ParsedResponse> smartParseIntermediary(
			ServerResponse response, String table, StringBuilder outputTable) {
		Vector<ParsedResponse> intermediaryResponse = new Vector<ParsedResponse>();
		int numberOfUncheckedElements = 0;

		/**
		 * We need to find what was the original search module in case we need some info from it like in the new PS interface
		 */
		TSServerInfoModule moduleSource = null;
		Object objectModuleSource = response.getParsedResponse().getAttribute(
				TSServerInfoConstants.EXTRA_PARAM_MODULE_OBJECT_SOURCE);
		if (objectModuleSource != null) {
			if (objectModuleSource instanceof TSServerInfoModule) {
				moduleSource = (TSServerInfoModule) objectModuleSource;
			}
		} else {
			objectModuleSource = getSearch().getAdditionalInfo(
					this.getKeyForSavingInIntermediaryNextLink(response
							.getQuerry()));
			if (objectModuleSource instanceof TSServerInfoModule) {
				moduleSource = (TSServerInfoModule) objectModuleSource;
			}
		}

		try {
			org.htmlparser.Parser htmlParser = org.htmlparser.Parser
					.createParser(table, null);
			NodeList nodeList = htmlParser.parse(null);
			NodeList mainTableList = nodeList
					.extractAllNodesThatMatch(new TagNameFilter("div"), true)
					.extractAllNodesThatMatch(
							new HasAttributeFilter("id", "results"), true)
					.extractAllNodesThatMatch(new TagNameFilter("table"), true);

			if (!(mainTableList.size() > 0)) {
				return intermediaryResponse;
			}

			// save params for details
			HashMap<String, String> params = ro.cst.tsearch.servers.functions.ARBentonRO
					.getParams(table, "frmResult");

			StringBuffer params_buffer = new StringBuffer("");

			if (params != null) {
				mSearch.setAdditionalInfo(getCurrentServerName() + ":params",
						params);

				for (String s : params.keySet())
					if (!s.equals("iSequence") && !s.equals("iRowNumber"))
						params_buffer.append("&"
								+ s
								+ "="
								+ (StringUtils.isEmpty(params.get(s)) ? ""
										: params.get(s)));
			}

			TableTag t = (TableTag) mainTableList.elementAt(0);

			TableRow[] rows = t.getRows();

			String siteLinkPrefix = (String) mSearch.getAdditionalInfo(getDataSite().getName() + ":" + "siteLinkPrefix");

			if (StringUtils.isEmpty(siteLinkPrefix))
				throw new Exception("Problem with site link! verify connection class!");

			for (int i = 1; i < rows.length; i++) {
				TableRow row = rows[i];
				if (row.getColumnCount() == 13 || row.getColumnCount() == 12
						|| row.getColumnCount() == 15) {

					ResultMap m = ro.cst.tsearch.servers.functions.ARBentonRO
							.parseIntermediaryRow(row, (Integer) mSearch
									.getAdditionalInfo("viParseID"));

					// index link
					String link_prefix = CreatePartialLink(TSConnectionURL.idPOST);
					String doc_link = "";
					String image_link = "";

					if (row.getColumns()[0].getChildren().size() > 1
							&& row.getColumns()[0].getChild(1).getChildren()
									.size() > 1) {
						LinkTag linkTag = ((LinkTag) row.getColumns()[0]
								.getChild(1).getChildren().elementAt(1));

						String lnk = linkTag.getLink();

						String[] seq_num = lnk
								.replaceAll("viewIndex\\(([^)]*)\\);", "$1")
								.replace("'", "").split(",");

						String seq = "";
						String rowNum = "";

						if (seq_num.length == 3) {
							seq = seq_num[0];
							rowNum = seq_num[1];
						}

						doc_link = siteLinkPrefix + "viewIndex.asp"
								+ params_buffer.toString().replaceFirst("&",
										"?")
								+ "&iSequence="
								+ seq
								+ "&iRowNumber=" + rowNum;

						linkTag.setLink(link_prefix + doc_link);
						// + ro.cst.tsearch.servers.functions.ARBentonRO
						// .makePartOfFakeLink(m));
					}

					// image link
					if (row.getColumns()[1].getChildren().size() > 1
							&& row.getColumns()[0].getChild(1).getChildren()
									.size() > 1) {
						LinkTag linkTag1 = ((LinkTag) row.getColumns()[1]
								.getChild(1).getChildren().elementAt(1));

						String lnk1 = linkTag1.getLink();

						String seq1 = "";
						String rowNum1 = "";

						String[] seq_num1 = lnk1
								.replaceAll("viewImage\\(([^)]*)\\);", "$1")
								.replace("'", "").split(",");

						if (seq_num1.length == 3) {
							seq1 = seq_num1[0];
							rowNum1 = seq_num1[1];
						}

						image_link = siteLinkPrefix + "viewImage.asp"
								+ params_buffer.toString().replaceFirst("&",
										"?")
								+ "&iSequence="
								+ seq1
								+ "&iRowNumber=" + rowNum1;

						linkTag1.setLink(link_prefix + image_link);
						// + ro.cst.tsearch.servers.functions.ARBentonRO
						// .makePartOfFakeLink(m));
					}

					String clean_row = row.toHtml();

					clean_row = clean_row.replaceAll("<img [^>]*>",
							"FAKE_IMAGE");

					clean_row = clean_row.replaceFirst("FAKE_IMAGE",
							"View Details");
					clean_row = clean_row.replaceFirst("FAKE_IMAGE",
							"View Image");

					clean_row = clean_row.replaceAll(
							"(?ism)</div>[^(]*\\([^)]*\\)[^<]*</td>",
							"</div>\n</td>");
					clean_row = clean_row
							.replaceAll(
									"(?ism)<td valign=\"top\" align=\"left\">[\r\n\t]+</td>",
									"<td valign=\"top\" align=\"left\">&nbsp;</td>");

					clean_row = clean_row
							.replaceAll(
									"(?ism)<a[^>]*class=\"book_button\"[^>]*>([^<]*)<[^>]*>",
									"$1");

					ParsedResponse currentResponse = new ParsedResponse();
					currentResponse.setAttribute(
							ParsedResponse.SERVER_ROW_RESPONSE, clean_row);
					currentResponse.setOnlyResponse(clean_row);
					currentResponse.setPageLink(new LinkInPage(doc_link,
							doc_link, TSServer.REQUEST_SAVE_TO_TSD));

					Bridge bridge = new Bridge(currentResponse, m, searchId);

					DocumentI document = (RegisterDocumentI) bridge
							.importData();
					currentResponse.setDocument(document);

					String checkBox = "checked";

					String instrNo = "";

					HashMap<String, String> data = new HashMap<String, String>();

					data.put("type", (String) m
							.get(SaleDataSetKey.DOCUMENT_TYPE.getKeyName()));
					data.put("book",
							(String) m.get(SaleDataSetKey.BOOK.getKeyName()));
					data.put("page",
							(String) m.get(SaleDataSetKey.PAGE.getKeyName()));

					if (isInstrumentSaved(instrNo, document, data, true)
							&& !Boolean.TRUE.equals(getSearch()
									.getAdditionalInfo("RESAVE_DOCUMENT"))) {
						checkBox = "saved";
					} else {
						numberOfUncheckedElements++;
						LinkInPage linkInPage = new LinkInPage(link_prefix
								+ doc_link, link_prefix + doc_link,
								TSServer.REQUEST_SAVE_TO_TSD);
						checkBox = "<input type='checkbox' name='docLink' value='"
								+ link_prefix + doc_link + "'>";
						if (getSearch().getInMemoryDoc(doc_link) == null) {
							getSearch().addInMemoryDoc(doc_link,
									currentResponse);
						}
						currentResponse.setPageLink(linkInPage);

						/**
						 * Save module in key in additional info. The key is instrument number that should be always available.
						 */
						String keyForSavingModules = this
								.getKeyForSavingInIntermediary(getInstrumentNumberForSavingInFinalResults(document));
						getSearch().setAdditionalInfo(keyForSavingModules,
								moduleSource);
					}
					String rowHtml = "<tr><td>"
							+ checkBox
							+ "</td>"
							+ clean_row.replaceAll("(?ism)<tr[^>]*>", "")
									.replaceAll("(?ism)</tr>", "") + "</tr>";

					currentResponse.setAttribute(
							ParsedResponse.SERVER_ROW_RESPONSE, rowHtml
									.replaceAll(
											"(?ism)<a [^>]*>([^<]*)<[^>]*>",
											"$1"));
					currentResponse.setOnlyResponse(rowHtml);

					intermediaryResponse.add(currentResponse);
				}
			}

			table = table.replaceAll(
					"(?ism)<img [^>]*alt=\"Index Icon\"[^>]*>", "View Details");
			table = table.replaceAll(
					"(?ism)<img [^>]*alt=\"Image Icon\"[^>]*>", "View Image");

			table = table.replaceAll("(?ism)</div>[^(]*\\([^)]*\\)[^<]*</td>",
					"</div>\n</td>");

			outputTable.append(table);
		} catch (Exception e) {
			e.printStackTrace();
		}
		SetAttribute(NUMBER_OF_UNSAVED_DOCUMENTS, numberOfUncheckedElements);
		return intermediaryResponse;
	}

	@Override
	protected DownloadImageResult saveImage(ImageLinkInPage image) throws ServerResponseException {

		String link = image.getLink();
		byte[] imageBytes = null;

		if (link.contains("book") && link.contains("page") && link.contains("type") && link.contains("?")) {
			HTTPRequest req = new HTTPRequest(ro.cst.tsearch.connection.http2.ARBentonRO.SITE_LINK_PREFIX + "SearchResults.asp", HTTPRequest.POST);

			req.setPostParameter("SearchbyDateFrom", "");
			req.setPostParameter("SearchbyDateTo", "");
			req.setPostParameter("SearchbyPageTo", "");
			req.setPostParameter("sSearchType", "BP");

			String params[] = link.split("\\?")[1].split("&");
			HashMap<String, String> map = new HashMap<String, String>();
			for (String s : params) {
				map.put(s.split("=")[0], s.split("=").length == 2 ? s.split("=")[1] : "");
			}

			req.setPostParameter("SearchDocType", org.apache.commons.lang.StringUtils.defaultString(map.get("type")));
			req.setPostParameter("SearchBookType", "ALL");
			req.setPostParameter("SearchbyBook", org.apache.commons.lang.StringUtils.defaultString(map.get("book")));
			req.setPostParameter("SearchbyPageFrom", org.apache.commons.lang.StringUtils.defaultString(map.get("page")));

			HTTPResponse result = null;

			HttpSite site = HttpManager.getSite(getCurrentServerName(), searchId);
			try {
				result = site.process(req);
			} catch (Exception e) {
				e.printStackTrace();
			} finally {
				HttpManager.releaseSite(site);
			}

			if (result != null) {
				NodeList nodes = new HtmlParser3(result.getResponseAsString()).getNodeList();

				if (nodes.size() > 0) {
					NodeList mainTableList = nodes
							.extractAllNodesThatMatch(new TagNameFilter("div"), true)
							.extractAllNodesThatMatch(new HasAttributeFilter("id", "results"), true)
							.extractAllNodesThatMatch(new TagNameFilter("table"), true);

					if (mainTableList.size() > 0) {
						TableTag t = (TableTag) mainTableList.elementAt(0);
						TableRow[] rows = t.getRows();

						HashMap<String, String> paramsReq = ro.cst.tsearch.servers.functions.ARBentonRO
								.getParams(result.getResponseAsString(), "frmResult");

						String seq = "";
						String rowNum = "";

						if (rows.length > 1 && rows[1].getColumnCount() == 12) {
							link = rows[1].getColumns()[1].toHtml();

							String linkAux = link.replaceAll("(?ism).*<a href=\"([^\"]*)\"[^>]*>.*", "$1");

							String[] seq_num = linkAux.replaceAll("javascript:viewImage\\(([^)]*)\\);", "$1").replace("'", "").split(",");

							if (seq_num.length == 3) {
								seq = seq_num[0];
								rowNum = seq_num[1];
							}
						}

						StringBuffer buf = new StringBuffer();
						if (paramsReq != null) {
							for (String s : paramsReq.keySet()) {
								if (!s.equals("iSequence") && !s.equals("iRowNumber")) {
									buf.append("&" + s + "=" + org.apache.commons.lang.StringUtils.defaultString(paramsReq.get(s)));
								}
							}
						}
						buf.append("&iSequence=" + org.apache.commons.lang.StringUtils.defaultString(seq));
						buf.append("&iRowNumber=" + org.apache.commons.lang.StringUtils.defaultString(rowNum));

						link = ro.cst.tsearch.connection.http2.ARBentonRO.SITE_LINK_PREFIX + ro.cst.tsearch.connection.http2.ARBentonRO.SITE_LINK_PREFIX
								+ "viewImage.asp" + buf.toString().replaceFirst("&", "?");

					}
				}
			}
		}

		if (link.contains(ro.cst.tsearch.connection.http2.ARBentonRO.SITE_LINK_PREFIX)) {
			HttpSite site = HttpManager.getSite(getCurrentServerName(), searchId);
			try {
				imageBytes = ((ro.cst.tsearch.connection.http2.ARBentonRO) site).getImage(link);
			} catch (Exception e) {
				e.printStackTrace();
			} finally {
				HttpManager.releaseSite(site);
			}
		}

		ServerResponse resp = new ServerResponse();

		if (imageBytes != null) {
			afterDownloadImage(true);
		} else {
			return new DownloadImageResult(DownloadImageResult.Status.ERROR,
					new byte[0], image.getContentType());
		}

		String imageName = image.getPath();
		if (ro.cst.tsearch.utils.FileUtils.existPath(imageName)) {
			imageBytes = ro.cst.tsearch.utils.FileUtils
					.readBinaryFile(imageName);
			return new DownloadImageResult(DownloadImageResult.Status.OK,
					imageBytes, image.getContentType());
		}

		resp.setImageResult(new DownloadImageResult(
				DownloadImageResult.Status.OK, imageBytes,
				((ImageLinkInPage) image).getContentType()));

		if (!ro.cst.tsearch.utils.FileUtils.existPath(imageName)) {
			FileUtils.writeByteArrayToFile(resp.getImageResult()
					.getImageContent(), image.getPath());
		}

		DownloadImageResult dres = resp.getImageResult();

		return dres;
	}

	@Override
	protected void setCertificationDate() {

		try {
			String countyName = dataSite.getCountyName();

			logger.debug("Intru pe get Certification Date - " + countyName);

			if (CertificationDateManager.isCertificationDateInCache(dataSite)){
				String date = CertificationDateManager.getCertificationDateFromCache(dataSite);
				getSearch().getSa().updateCertificationDateObject(dataSite, Util.dateParser3(date));
			} else{

				String html = "";
				HttpSite site = HttpManager.getSite(getCurrentServerName(), searchId);
				try {
					html = ((ro.cst.tsearch.connection.http2.ARBentonRO) site).getCertDate();
				} catch (RuntimeException e) {
					e.printStackTrace();
				} finally {
					HttpManager.releaseSite(site);
				}
	
				if (StringUtils.isNotEmpty(html)) {
					org.htmlparser.Parser htmlParser = org.htmlparser.Parser.createParser(html, null);
					NodeList mainList = htmlParser.parse(null);
	
					String date = HtmlParser3
							.getValueFromAbsoluteCell(0, 3, HtmlParser3.findNode(mainList, "Index Data: from"), "",
									true).replaceAll("<[^>]*>", "").trim();
	
					CertificationDateManager.cacheCertificationDate(dataSite, date);
					
					getSearch().getSa().updateCertificationDateObject(dataSite, Util.dateParser3(date));
				} else {
					CertificationDateManager.getLogger().error("Cannot parse certification date on " + getDataSite().getName() + " because html response is empty");
				}
			}
		} catch (Exception e) {
			CertificationDateManager.getLogger().error("Error setting certification date on " + getDataSite().getName(), e);
		}
	}

	@Override
	public boolean isInstrumentSaved(String instrumentNo, DocumentI documentToCheck, HashMap<String, String> data){
  	  return isInstrumentSaved(instrumentNo, documentToCheck, data, false);
    }
	
	/**
     * Looks for the a document having the same instrumentNo
     * @param instrumentNo
     * @param documentToCheck if not null will only compare its instrument with saved documents
     * @param data
     * @return
     */
	@Override
    public boolean isInstrumentSaved(String instrumentNo, DocumentI documentToCheck, HashMap<String, String> data, boolean checkMiServerId){
    	
    	DocumentsManagerI documentManager = getSearch().getDocManager();
    	try {
    		documentManager.getAccess();
    		if(documentToCheck != null) {
    			if(documentManager.getDocument(documentToCheck.getInstrument()) != null)
    				return true;
    		} else {
	    		InstrumentI instr = new com.stewart.ats.base.document.Instrument(instrumentNo);
	    		if(data != null) {
		    		if(!StringUtils.isEmpty(data.get("type"))) {
		        		String serverDocType = data.get("type");
		    	    	String docCateg = DocumentTypes.getDocumentCategory(serverDocType, searchId); 
		            	instr.setDocType(docCateg);
		            	instr.setDocSubType("MISCELLANEOUS");
		    		}
		    		
		    		instr.setBook(data.get("book"));
		    		instr.setPage(data.get("page"));
		    		instr.setDocno(data.get("docno"));
	    		}
	    		
	    		try {
	    			instr.setYear(Integer.parseInt(data.get("year")));
	    		} catch (Exception e) {}
	    		
	    		if(documentManager.getDocument(instr) != null) {
	    			return true;
	    		} else {
	    			List<DocumentI> almostLike = documentManager.getDocumentsWithInstrumentsFlexible(false, instr);
	    			
    				if(data!=null) {
    					if(!StringUtils.isEmpty(data.get("type"))){
			        		String serverDocType = data.get("type"); 
			    	    	String docCateg = DocumentTypes.getDocumentCategory(serverDocType, searchId);
			    	    	for (DocumentI documentI : almostLike) {
			    	    		if(documentI.getDocType().equals(docCateg)){
									return true;
			    	    		}
							}	
    					}
		    		} else {
		    			EmailClient email = new EmailClient();
		    			email.addTo(MailConfig.getExceptionEmail());
		    			email.setSubject("isInstrumentNoSaved problem on " + URLMaping.INSTANCE_DIR + this.getClass().getName());
		    			email.addContent("We should at least have type!!!!\nSearchId=" + searchId);
		    			email.sendAsynchronous();
		    		}
	    		}
    		}
    		
    	} catch (Exception e) {
			e.printStackTrace();
		} finally {
			documentManager.releaseAccess();
		}
    	
    	if(documentToCheck == null) {
			return false;
		}
		try {
    		documentManager.getAccess();
    		InstrumentI instToCheck = documentToCheck.getInstrument();
    		for(DocumentI e: documentManager.getDocumentsWithDataSource(false, "TS")){
    			InstrumentI savedInst = e.getInstrument();
    			if( savedInst.getInstno().equals(instToCheck.getInstno())  
    					&& (savedInst.getBook().equals(instToCheck.getBook()) && savedInst.getPage().equals(instToCheck.getPage()))  
    					&& savedInst.getDocno().equals(instToCheck.getDocno())
    					&& e.getDocType().equals(documentToCheck.getDocType())
    					&& savedInst.getYear() == instToCheck.getYear()
    			){
    				return true;
    			}
    		}
    	} finally {
    		documentManager.releaseAccess();
    	}
		
    	return false;
    }
	
	

	@Override
	protected Object parseAndFillResultMap(ServerResponse response,
			String detailsHtml, ResultMap map) {
		ro.cst.tsearch.servers.functions.ARBentonRO.parseAndFillResultMap(
				response, detailsHtml, map, searchId);
		return null;
	}
}
