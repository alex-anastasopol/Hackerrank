package ro.cst.tsearch.servers.types;

import java.math.BigDecimal;
import java.net.URLDecoder;
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
import java.util.Map.Entry;
import java.util.Set;
import java.util.Vector;

import org.html.parser.HtmlHelper;
import org.htmlparser.Node;
import org.htmlparser.filters.HasAttributeFilter;
import org.htmlparser.filters.TagNameFilter;
import org.htmlparser.tags.LinkTag;
import org.htmlparser.tags.TableRow;
import org.htmlparser.tags.TableTag;
import org.htmlparser.util.NodeList;

import ro.cst.tsearch.MailConfig;
import ro.cst.tsearch.Search;
import ro.cst.tsearch.bean.SearchAttributes;
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
import ro.cst.tsearch.search.filter.BetweenDatesFilterResponse;
import ro.cst.tsearch.search.filter.FilterResponse;
import ro.cst.tsearch.search.filter.RejectAlreadySavedDocumentsFilterResponse;
import ro.cst.tsearch.search.filter.matchers.algorithm.LotInterval;
import ro.cst.tsearch.search.filter.matchers.algorithm.LotMatchAlgorithm;
import ro.cst.tsearch.search.filter.newfilters.address.AddressFilterFactory;
import ro.cst.tsearch.search.filter.newfilters.date.DateFilterFactory;
import ro.cst.tsearch.search.filter.newfilters.date.LastTransferDateFilter;
import ro.cst.tsearch.search.filter.newfilters.legal.GenericInstrumentFilter;
import ro.cst.tsearch.search.filter.newfilters.legal.GenericLegal;
import ro.cst.tsearch.search.filter.newfilters.legal.LegalFilterFactory;
import ro.cst.tsearch.search.filter.newfilters.name.GenericNameFilter;
import ro.cst.tsearch.search.filter.newfilters.name.NameFilterFactory;
import ro.cst.tsearch.search.iterator.ModuleStatesIteratorFactory;
import ro.cst.tsearch.search.iterator.legal.LegalDescriptionIteratorI;
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
import ro.cst.tsearch.utils.StringUtils;
import ro.cst.tsearch.utils.URLMaping;

import com.stewart.ats.base.document.DocumentI;
import com.stewart.ats.base.document.DocumentUtils;
import com.stewart.ats.base.document.ImageI;
import com.stewart.ats.base.document.InstrumentI;
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
import com.stewart.ats.tsrindex.client.SimpleChapterUtils;
import com.stewart.ats.tsrindex.client.SimpleChapterUtils.DType;

/**
 * 
 * @author Oprina George
 * 
 *         Oct 14, 2011
 */

@SuppressWarnings("deprecation")
public class TXBexarRO extends TSServerROLike {

	/**
	 * 
	 */
	private static final long	serialVersionUID	= -3971061795885691551L;

	public TXBexarRO(long searchId) {
		super(searchId);
	}

	public TXBexarRO(String rsRequestSolverName, String rsSitePath, String rsServerID, String rsPrmNameLink, long searchId, int miServerID) {
		super(rsRequestSolverName, rsSitePath, rsServerID, rsPrmNameLink, searchId, miServerID);
	}

	@Override
	protected void ParseResponse(String sAction, ServerResponse Response, int viParseID) throws ServerResponseException {
		String rsResponse = Response.getResult();
		ParsedResponse parsedResponse = Response.getParsedResponse();

		if (rsResponse.indexOf("Your search returned no records.\nPlease enter a different set of criteria\nand try your search again.") > -1
				|| rsResponse.contains("error") || rsResponse.contains("Error")) {
			Response.getParsedResponse().setError("No results found for your query! Please change your search criteria and try again.");
			return;
		}

		switch (viParseID) {
		case ID_SEARCH_BY_NAME:
		case ID_SEARCH_BY_INSTRUMENT_NO:
		case ID_SEARCH_BY_BOOK_AND_PAGE:
		case ID_SEARCH_BY_SUBDIVISION_PLAT:
		case ID_SEARCH_BY_SALES:

			mSearch.setAdditionalInfo("viParseID", viParseID);

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
				String header = CreateSaveToTSDFormHeader(URLConnectionReader.MULTIPLE_SAVE_TO_TSD_ACTION, "POST");
				String footer = "";

				header += makeHeader(viParseID);

				Object numberOfUnsavedDocument = GetAttribute(NUMBER_OF_UNSAVED_DOCUMENTS);

				// set prev next buttons
				String prev_next = getPrevNext(Response);

				if (numberOfUnsavedDocument != null && numberOfUnsavedDocument instanceof Integer) {
					footer = "\n</table><br>" + prev_next
							+ CreateSaveToTSDFormEnd(SAVE_SELECTED_DOCUMENTS_BUTTON_LABEL, viParseID, (Integer) numberOfUnsavedDocument);
				} else {
					footer = "\n</table><br>" + prev_next + CreateSaveToTSDFormEnd(SAVE_SELECTED_DOCUMENTS_BUTTON_LABEL, viParseID, -1);
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

			if (viParseID != ID_SAVE_TO_TSD)
				try {
					String originalLink = sAction.replace("?", "&") + "&" + Response.getRawQuerry();
					String sSave2TSDLink = getLinkPrefix(TSConnectionURL.idPOST) + originalLink;
					String sSave2TSDLink1 = getLinkPrefix(TSConnectionURL.idPOST) + URLDecoder.decode(originalLink, "UTF-8");
					if (isInstrumentSaved("", null, data)) {
						details += CreateFileAlreadyInTSD();
					} else {
						mSearch.addInMemoryDoc(sSave2TSDLink1, details);
						details = addSaveToTsdButton(details, sSave2TSDLink, ID_DETAILS);
					}
					Response.getParsedResponse().setPageLink(new LinkInPage(sSave2TSDLink, originalLink, TSServer.REQUEST_SAVE_TO_TSD));
					Response.getParsedResponse().setResponse(details);
				} catch (Exception e) {
					e.printStackTrace();
				}
			else {
				smartParseDetails(Response, details);

				msSaveToTSDFileName = filename;
				Response.getParsedResponse().setFileName(getServerTypeDirectory() + msSaveToTSDFileName);
				Response.getParsedResponse().setResponse(details.replaceAll("(?ism)<a href[^>]*>([^<]*)</a>", "$1"));

				msSaveToTSDResponce = details.replaceAll("(?ism)<a href[^>]*>([^<]*)</a>", "$1") + CreateFileAlreadyInTSD();

//				String resultForCross = details;
//
//				Pattern crossRefLinkPattern = Pattern.compile("(?ism)<a href=[\\\"|'](.*?)[\\\"|']>");
//				Matcher crossRefLinkMatcher = crossRefLinkPattern.matcher(resultForCross);
//				while (crossRefLinkMatcher.find()) {
//					ParsedResponse prChild = new ParsedResponse();
//					String link = crossRefLinkMatcher.group(1);
//					if (link.contains("viewIndex.asp")) {
//						LinkInPage pl = new LinkInPage(link, link, TSServer.REQUEST_SAVE_TO_TSD);
//						prChild.setPageLink(pl);
//						Response.getParsedResponse().addOneResultRowOnly(prChild);
//					}
//				}
			}
			break;
		case ID_GET_LINK:
			ParseResponse(sAction, Response, rsResponse.contains("Document Index Detail") ? ID_DETAILS : ID_SEARCH_BY_NAME);
			break;
		}
	}

	private String makeHeader(int viParseID) {
		String header = "";

		header += "<table cellspacing=\"0\" cellpadding=\"3\" border=\"1\" width=\"95%\" align=\"center\">"
				+ "<TR bgcolor=\"#6699CC\" valign=\"top\">"
				+ "<TH ALIGN=Left>"
				+ SELECT_ALL_CHECKBOXES
				+ "</TH>";

		switch (viParseID) {
		case ID_SEARCH_BY_NAME:
			header += "<TH ALIGN=Left>Index<br>Detail</TH>"
					+ "<TH ALIGN=Left>View<br>Image</TH>"
					+ "<TH ALIGN=Left>Searched Name</TH>"
					+ "<TH ALIGN=Left>GTR<br>GTE</TH>"
					+ "<TH ALIGN=Left>Doc. #</TH>"
					+ "<TH ALIGN=Left>Filed Date</TH>"
					+ "<TH ALIGN=Left>Book Type</TH>"
					+ "<TH ALIGN=Left>Book<br>Page</TH>"
					+ "<TH ALIGN=Left>Instrument Type</TH>"
					+ "<TH ALIGN=Left>Opposite Name</TH>"
					+ "<TH ALIGN=Left>Lot</TH>"
					+ "<TH ALIGN=Left>Block</TH>"
					+ "<TH ALIGN=Left>NCB</TH>"
					+ "<TH ALIGN=Left>County Block</TH>"
					+ "<TH ALIGN=Left>Subdivision</TH>";
			break;
		case ID_SEARCH_BY_INSTRUMENT_NO:
			header += "<TH ALIGN=Left>Index<br>Detail</TH>"
					+ "<TH ALIGN=Left>View<br>Image</TH>"
					+ "<TH ALIGN=Left>Doc. #</TH>"
					+ "<TH ALIGN=Left>Filed Date</TH>"
					+ "<TH ALIGN=Left>Instrument Type</TH>"
					+ "<TH ALIGN=Left>Book Type</TH>"
					+ "<TH ALIGN=Left>Book<br>Page</TH>"
					+ "<TH ALIGN=Left>Grantor</TH>"
					+ "<TH ALIGN=Left>Grantee</TH>"
					+ "<TH ALIGN=Left>Lot</TH>"
					+ "<TH ALIGN=Left>Block</TH>"
					+ "<TH ALIGN=Left>NCB</TH>"
					+ "<TH ALIGN=Left>County Block</TH>"
					+ "<TH ALIGN=Left>Subdivision</TH>";
			break;
		case ID_SEARCH_BY_BOOK_AND_PAGE:
			header += "<TH ALIGN=Left>Index<br>Detail</TH>"
					+ "<TH ALIGN=Left>View<br>Image</TH>"
					+ "<TH ALIGN=Left>Book<br>Page</TH>"
					+ "<TH ALIGN=Left>Book Type</TH>"
					+ "<TH ALIGN=Left>Filed Date</TH>"
					+ "<TH ALIGN=Left>Instrument Type</TH>"
					+ "<TH ALIGN=Left>Doc. #</TH>"
					+ "<TH ALIGN=Left>Grantor</TH>"
					+ "<TH ALIGN=Left>Grantee</TH>"
					+ "<TH ALIGN=Left>Lot</TH>"
					+ "<TH ALIGN=Left>Block</TH>"
					+ "<TH ALIGN=Left>NCB</TH>"
					+ "<TH ALIGN=Left>County Block</TH>"
					+ "<TH ALIGN=Left>Subdivision</TH>";
			break;
		case ID_SEARCH_BY_SUBDIVISION_PLAT:
			header += "<TH ALIGN=Left>Index<br>Detail</TH>"
					+ "<TH ALIGN=Left>View<br>Image</TH>"
					+ "<TH ALIGN=Left>Lot</TH>"
					+ "<TH ALIGN=Left>Block</TH>"
					+ "<TH ALIGN=Left>NCB</TH>"
					+ "<TH ALIGN=Left>County Block</TH>"
					+ "<TH ALIGN=Left>Subdivision</TH>"
					+ "<TH ALIGN=Left>Water Permit</TH>"
					+ "<TH ALIGN=Left>Instrument Type</TH>"
					+ "<TH ALIGN=Left>Doc. #</TH>"
					+ "<TH ALIGN=Left>Book Type</TH>"
					+ "<TH ALIGN=Left>Book<br>Page</TH>"
					+ "<TH ALIGN=Left>Grantor</TH>"
					+ "<TH ALIGN=Left>Grantee</TH>";
			break;
		case ID_SEARCH_BY_SALES:
			header += "<TH ALIGN=Left>Index<br>Detail</TH>"
					+ "<TH ALIGN=Left>View<br>Image</TH>"
					+ "<TH ALIGN=Left>Instrument Type</TH>"
					+ "<TH ALIGN=Left>Filed Date</TH>"
					+ "<TH ALIGN=Left>Doc. #</TH>"
					+ "<TH ALIGN=Left>Book Type</TH>"
					+ "<TH ALIGN=Left>Book<br>Page</TH>"
					+ "<TH ALIGN=Left>Grantor</TH>"
					+ "<TH ALIGN=Left>Grantee</TH>"
					+ "<TH ALIGN=Left>Lot</TH>"
					+ "<TH ALIGN=Left>Block</TH>"
					+ "<TH ALIGN=Left>NCB</TH>"
					+ "<TH ALIGN=Left>County Block</TH>"
					+ "<TH ALIGN=Left>Subdivision</TH>";
			break;
		}

		header += "</TR>";

		return header;
	}

	private String getPrevNext(ServerResponse resp) {

		String response = resp.getResult();

		String prev_next = "\n<table id=\"prev_next_links\">\n<tr>";
		try {
			NodeList nodeList = (NodeList) resp.getParsedResponse().getAttribute("nodeList");

			nodeList = nodeList.extractAllNodesThatMatch(new TagNameFilter("div"), true)
					.extractAllNodesThatMatch(new HasAttributeFilter("id", "paging_grouping_top"), true);

			if (nodeList.size() > 0) {

				String prevLink = "";
				String pageDiv = "";
				String nextLink = "";

				NodeList nodes = null;

				if ((nodes = nodeList.extractAllNodesThatMatch(new TagNameFilter("a"), true).extractAllNodesThatMatch(
						new HasAttributeFilter("title", "Previous Page"), true))
						.size() > 0) {
					prevLink = ((LinkTag) nodes.elementAt(0)).getLink();
				}

				if ((nodes = nodeList.extractAllNodesThatMatch(new TagNameFilter("div"), true).extractAllNodesThatMatch(
						new HasAttributeFilter("class", "page_label"), true))
						.size() > 0) {
					pageDiv = nodes.elementAt(0).toHtml();
				}

				if ((nodes = nodeList.extractAllNodesThatMatch(new TagNameFilter("a"), true).extractAllNodesThatMatch(
						new HasAttributeFilter("title", "Next Page"), true))
						.size() > 0) {
					nextLink = ((LinkTag) nodes.elementAt(0)).getLink();
				}

				// get params from frmSort form
				HashMap<String, String> mapSort = ro.cst.tsearch.servers.functions.TXBexarRO.getFormParams(response, "frmSort");
				// get params from frmResult form
				HashMap<String, String> mapRes = ro.cst.tsearch.servers.functions.TXBexarRO.getFormParams(response, "frmResult");

				if (StringUtils.isNotEmpty(prevLink)) {
					String[] l_parts = prevLink.replaceAll(".*submitForm\\(([^)]*)\\);.*", "$1").replace("'", "").split(",");
					if (l_parts.length == 5) {
						String link = CreatePartialLink(TSConnectionURL.idPOST);
						mapSort.put("currentPage", l_parts[1]);
						// merge params
						String params = mergeParams(mergeParams("", mapSort), mapRes);
						// make link
						prev_next += "<td>" + "<a href=\"" + link + "SearchResults.asp&" + params + "\">Prev</a>" + "</td>";
					}
				}

				if (StringUtils.isNotEmpty(pageDiv)) {
					prev_next += "<td>" + pageDiv + "</td>";
				}

				if (StringUtils.isNotEmpty(nextLink)) {
					String[] l_parts = nextLink.replaceAll(".*submitForm\\(([^)]*)\\);.*", "$1").replace("'", "").split(",");
					if (l_parts.length == 5) {
						String link = CreatePartialLink(TSConnectionURL.idPOST);
						mapSort.put("currentPage", l_parts[1]);
						// merge params
						String params = mergeParams(mergeParams("", mapSort), mapRes);
						// make link
						prev_next += "<td>" + "<a href=\"" + link + "SearchResults.asp&" + params + "\">Next</a>" + "</td>";
					}
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		prev_next += "\n</tr></table>\n<hr>";
		return prev_next;
	}

	private String mergeParams(String initial, HashMap<String, String> map) {
		StringBuffer buf = new StringBuffer(org.apache.commons.lang.StringUtils.defaultString(initial));

		for (String key : map.keySet())
			buf.append("&" + key + "=" + org.apache.commons.lang.StringUtils.defaultString(map.get(key)));

		return buf.toString();
	}

	protected String getDetails(String rsResponse, StringBuilder accountId, HashMap<String, String> data, ServerResponse Response) {

		try {
			StringBuilder details = new StringBuilder();
			org.htmlparser.Parser htmlParser = org.htmlparser.Parser
					.createParser(rsResponse, null);

			NodeList nodeList = htmlParser.parse(null);

			// tables
			NodeList tableList = nodeList
					.extractAllNodesThatMatch(new TagNameFilter("table"), true)
					.extractAllNodesThatMatch(new HasAttributeFilter("border", "0"))
					.extractAllNodesThatMatch(new HasAttributeFilter("style"))
					.extractAllNodesThatMatch(new HasAttributeFilter("width", "100%"));

			if (tableList.size() != 1) {
				return null;
			}

			// String book_type = HtmlParser3.getValueFromNextCell(HtmlParser3.findNode(tableList, "Book Type"), "", true);
			String book = HtmlParser3.getValueFromNextCell(HtmlParser3.findNode(tableList, "Book #"), "", true);
			String page = HtmlParser3.getValueFromNextCell(HtmlParser3.findNode(tableList, "Page #"), "", true);
			String instrument_date = HtmlParser3.getValueFromNextCell(HtmlParser3.findNode(tableList, "Instrument Date:"), "", true);
			String type = HtmlParser3.getValueFromNextCell(HtmlParser3.findNode(tableList, "Instrument Type:"), "", true);
			String year = instrument_date.replaceAll("\\d+/\\d+/(\\d+)", "$1");

			data.put("type", type.replaceAll(" ", ""));
			data.put("book", book);
			data.put("page", page);
			data.put("year", year);

			accountId.append(type.replaceAll(" ", "") + book + page);

			String siteLinkPrefix = "";

			String view_image = siteLinkPrefix + "viewImageFake.asp" + "?" + Response.getQuerry();
			view_image = view_image.replaceAll("(?ism)&parentSite=[^&]*", "");

			if (view_image.contains("&bPrev=")) {
				view_image = view_image.replace("?", "&");
				view_image = view_image.replaceAll("&iApplNum=[^&]*", "");
				view_image = view_image.replaceAll("&bPrev=[^&]*", "");
				view_image = view_image.replaceFirst("&", "?") + "&iPamendID=&iImageID=&related=true";
			}

			if (HtmlParser3.getNodesByID("view_image_button_bottom", nodeList,
					true).size() > 0) {
				String fakeLink = siteLinkPrefix + "viewImage.asp" + "?type=" + type + "&book=" + book + "&page=" + page;

				Response.getParsedResponse().addImageLink(
						new ImageLinkInPage(fakeLink, accountId.toString()
								+ ".tif"));
			}

			/* If from memory - use it as is */
			if (!rsResponse.contains("<html")) {
				return rsResponse;
			}

			TableTag table = (TableTag) tableList.elementAt(0);

			details.append("<table id=resultsDetail width=95% align=center><tr><td><p align=\"center\"><font style=\"font-size:xx-large;\"><b>Document Index Detail <br> Bexar County</b></font></p>"
					+ "<br>");

			// fake table here -> fake image and details links for related
			// documents
			NodeList tables = nodeList.extractAllNodesThatMatch(new TagNameFilter("table"), true);

			Node[] tables_array = tables.toNodeArray();

			Arrays.sort(tables_array, new ro.cst.tsearch.servers.functions.NVClarkTR.NodeComp());

			TableTag crossref_table = ro.cst.tsearch.servers.functions.NVClarkTR.getTableByContent(tables_array, new String[] { "Related Instr Type",
					"Related Book No", "Related Page No" });

			String related_doc_link = CreatePartialLink(TSConnectionURL.idPOST) + siteLinkPrefix + "viewIndex.asp";
			String related_img_link = CreatePartialLink(TSConnectionURL.idPOST) + siteLinkPrefix + "viewImage.asp";

			if (crossref_table != null) {
				TableRow[] rows = crossref_table.getRows();
				for (int j = 1; j < rows.length; j++) {
					if (rows[j].getColumnCount() == 6) {
						if (rows[j].getColumns()[4].getChildCount() > 0 && rows[j].getColumns()[4].getChild(0) instanceof LinkTag) {
							String link = ((LinkTag) rows[j].getColumns()[4].getChild(0)).getLink();

							String new_link = link.replaceAll("viewRelatedIndex\\('(\\d+)','(\\d+)'\\);",
									"&reldetail=true&iApplNum=$2&bPrev=false&iRecordID=$1&iDataSetNum=$2");
							((LinkTag) rows[j].getColumns()[4].getChild(0)).setLink(related_doc_link + new_link);
						}

						if (rows[j].getColumns()[5].getChildCount() > 0 && rows[j].getColumns()[5].getChild(0) instanceof LinkTag) {
							String link = ((LinkTag) rows[j].getColumns()[5].getChild(0)).getLink();

							String new_link = link.replaceAll("viewRelatedImage\\('(\\d+)','(\\d+)'\\);",
									"&related=true&iRecordID=$1&iPamendID=&iImageID=&iDataSetNum=$2");
							((LinkTag) rows[j].getColumns()[5].getChild(0)).setLink(related_img_link + new_link);
						}
					}
				}
			}

			String table_s = table.toHtml();

			// replace images
			table_s = table_s.replaceAll("(?ism)<img src=\"images/view_detail_small.gif\" border=\"0\" title=\"View Related Document Index\">",
					"View Related Document Index");

			table_s = table_s.replaceAll("(?ism)<img src=\"images/view_image_small.gif\" border=\"0\" title=\"View Related Document Image\">",
					"View Related Document Image");

			table_s = table_s.replaceAll("(?ism)<img[^>]*>", "");

			// remove other links
			// table_s = table_s.replaceAll("<a href=\"/title-search/URLConnectionReader", "<aFAKE href=\"/title-search/URLConnectionReader");
			//
			// table_s = table_s.replaceAll("(?ism)<a href=[^>]*>([^<]*)<[^>]*>", "$1");
			//
			// table_s = table_s.replaceAll("Previously Viewed Related Indexes:", "");
			//
			// table_s = table_s.replaceAll("<aFAKE", "<a");

			details.append(table_s);

			// put image link
			if (HtmlParser3.getNodesByID("view_image_button_bottom", nodeList, true).size() > 0) {
				details.append("<table><tr><td><a href=\"" + CreatePartialLink(TSConnectionURL.idPOST) + view_image + "\">View Image</a></td></tr></table>");
			}

			details.append("</td></tr></table>");

			return details.toString();
		} catch (Exception e) {
			e.printStackTrace();
		}
		return "";
	}

	@Override
	public Collection<ParsedResponse> smartParseIntermediary(ServerResponse response, String table, StringBuilder outputTable) {
		Vector<ParsedResponse> intermediaryResponse = new Vector<ParsedResponse>();
		int numberOfUncheckedElements = 0;

		/**
		 * We need to find what was the original search module in case we need some info from it like in the new PS interface
		 */
		TSServerInfoModule moduleSource = null;
		Object objectModuleSource = response.getParsedResponse().getAttribute(TSServerInfoConstants.EXTRA_PARAM_MODULE_OBJECT_SOURCE);
		if (objectModuleSource != null) {
			if (objectModuleSource instanceof TSServerInfoModule) {
				moduleSource = (TSServerInfoModule) objectModuleSource;
			}
		} else {
			objectModuleSource = getSearch().getAdditionalInfo(this.getKeyForSavingInIntermediaryNextLink(response.getQuerry()));
			if (objectModuleSource instanceof TSServerInfoModule) {
				moduleSource = (TSServerInfoModule) objectModuleSource;
			}
		}

		try {
			org.htmlparser.Parser htmlParser = org.htmlparser.Parser.createParser(table, null);
			NodeList nodeList = htmlParser.parse(null);

			response.getParsedResponse().setAttribute("nodeList", nodeList);

			NodeList mainTableList = nodeList.extractAllNodesThatMatch(new TagNameFilter("div"), true)
					.extractAllNodesThatMatch(new HasAttributeFilter("id", "results"), true)
					.extractAllNodesThatMatch(new TagNameFilter("table"), true);

			if (!(mainTableList.size() > 0)) {
				return intermediaryResponse;
			}

			// save params for details
			HashMap<String, String> params = ro.cst.tsearch.servers.functions.TXBexarRO.getFormParams(table, "frmResult");

			StringBuffer paramsSb = new StringBuffer();
			if (params != null) {
				for (Entry<String, String> e : params.entrySet()) {
					if (!e.getKey().equals("iSequence") && !e.getKey().equals("iRowNumber") && !e.getKey().equals("sTableid")
							&& !e.getKey().equals("passedTableID")) {
						paramsSb.append(e.getKey() + "=" + org.apache.commons.lang.StringUtils.defaultString(e.getValue()) + "&");
					}
				}
			}

			TableTag t = (TableTag) mainTableList.elementAt(0);

			TableRow[] rows = t.getRows();

			String link_prefix = CreatePartialLink(TSConnectionURL.idPOST);
			String originalLink = "";

			for (int i = 1; i < rows.length; i++) {
				TableRow row = rows[i];
				if (row.getColumnCount() == 15 || row.getColumnCount() == 14) {

					ResultMap m = ro.cst.tsearch.servers.functions.TXBexarRO.parseIntermediaryRow(row, (Integer) mSearch.getAdditionalInfo("viParseID"));

					// index link

					String doc_link = "";
					String image_link = "";

					if (row.getColumns()[0].getChildren().size() > 1 && row.getColumns()[0].getChild(1).getChildren().size() > 1) {

						row.getColumns()[0].removeAttribute("width");

						LinkTag linkTag = ((LinkTag) (row.getColumns()[0].getChild(1).getChildren().elementAt(1)));

						linkTag.setChildren(null);

						HtmlHelper.appendPlainText(linkTag, "View Details");

						String lnk = linkTag.getLink();

						String[] link_parts = lnk.replaceAll("viewIndex\\(([^)]*)\\);", "$1").replace("'", "").split(",");

						if (link_parts.length == 3) {
							doc_link = originalLink + "viewIndex.asp&" + paramsSb + "iSequence=" + link_parts[0] + "&iRowNumber=" + link_parts[1]
									+ "&sTableid=" + link_parts[2] + "&passedTableID=" + link_parts[2];
						}

						if (StringUtils.isNotEmpty(doc_link)) {
							linkTag.setLink(link_prefix + doc_link);
						}
					}

					// image link
					if (row.getColumns()[1].getChildren().size() > 1 && row.getColumns()[0].getChild(1).getChildren().size() > 1) {

						row.getColumns()[1].removeAttribute("width");

						LinkTag linkTag1 = ((LinkTag) (row.getColumns()[1].getChild(1).getChildren().elementAt(1)));

						linkTag1.setChildren(null);

						HtmlHelper.appendPlainText(linkTag1, "View Image");

						String lnk1 = linkTag1.getLink();

						String[] link_parts = lnk1.replaceAll("viewImage\\(([^)]*)\\);", "$1").replace("'", "").split(",");

						if (link_parts.length == 3) {
							image_link = originalLink + "viewImageFake.asp&" + paramsSb + "iSequence=" + link_parts[0] + "&iRowNumber=" + link_parts[1]
									+ "&sTableid=" + link_parts[2] + "&passedTableID=" + link_parts[2];
						}

						if (StringUtils.isNotEmpty(image_link)) {
							linkTag1.setLink(link_prefix + image_link);
						}
					}

					String clean_row = row.toHtml();

					clean_row = clean_row.replaceAll("(?ism)(</div>[^<]*)\\([^)]*\\)([^<]*</td>)", "$1$2");
					clean_row = clean_row.replaceAll("(?ism)<a href[^>]*javascript:loadBook[^>]*>([^<]*)</a>", "$1");

					if (clean_row.contains("This image has been secured at the County's request.")) {
						clean_row = clean_row.replaceAll("<img [^>]*src=\"images/lock.gif\"[^>]*>", "");
					}

					ParsedResponse currentResponse = new ParsedResponse();
					currentResponse.setAttribute(ParsedResponse.SERVER_ROW_RESPONSE, clean_row);
					currentResponse.setOnlyResponse(clean_row);
					currentResponse.setPageLink(new LinkInPage(doc_link, doc_link, TSServer.REQUEST_SAVE_TO_TSD));

					Bridge bridge = new Bridge(currentResponse, m, searchId);

					DocumentI document = (RegisterDocumentI) bridge.importData();
					currentResponse.setDocument(document);

					String checkBox = "checked";

					String instrNo = org.apache.commons.lang.StringUtils.defaultString((String) m.get(SaleDataSetKey.INSTRUMENT_NUMBER.getKeyName()));

					HashMap<String, String> data = new HashMap<String, String>();

					data.put("instrno", instrNo);
					data.put("type", org.apache.commons.lang.StringUtils.defaultString((String) m.get(SaleDataSetKey.DOCUMENT_TYPE.getKeyName())));
					data.put("book", org.apache.commons.lang.StringUtils.defaultString((String) m.get(SaleDataSetKey.BOOK.getKeyName())));
					data.put("page", org.apache.commons.lang.StringUtils.defaultString((String) m.get(SaleDataSetKey.PAGE.getKeyName())));

					if (isInstrumentSaved(instrNo, document, data, true)) {
						checkBox = "saved";
					} else {
						numberOfUncheckedElements++;
						LinkInPage linkInPage = new LinkInPage(link_prefix + doc_link, link_prefix + doc_link, TSServer.REQUEST_SAVE_TO_TSD);
						checkBox = "<input type='checkbox' name='docLink' value='" + link_prefix + doc_link + "'>";
						if (getSearch().getInMemoryDoc(doc_link) == null) {
							// getSearch().addInMemoryDoc(doc_link, currentResponse);
						}
						currentResponse.setPageLink(linkInPage);

						/**
						 * Save module in key in additional info. The key is instrument number that should be always available.
						 */
						String keyForSavingModules = this.getKeyForSavingInIntermediary(getInstrumentNumberForSavingInFinalResults(document));
						getSearch().setAdditionalInfo(keyForSavingModules, moduleSource);
					}
					String rowHtml = "<tr><td>" + checkBox + "</td>" + clean_row.replaceAll("(?ism)<tr[^>]*>", "").replaceAll("(?ism)</tr>", "") + "</tr>";

					currentResponse.setAttribute(ParsedResponse.SERVER_ROW_RESPONSE, rowHtml.replaceAll("(?ism)<a [^>]*>([^<]*)<[^>]*>", "$1"));
					currentResponse.setOnlyResponse(rowHtml);

					intermediaryResponse.add(currentResponse);
				}
			}
			outputTable.append(table);
		} catch (Exception e) {
			e.printStackTrace();
		}
		SetAttribute(NUMBER_OF_UNSAVED_DOCUMENTS, numberOfUncheckedElements);
		return intermediaryResponse;
	}

	private static Set<InstrumentI> getAllAoAndTaxReferences(Search search) {
		DocumentsManagerI manager = search.getDocManager();
		Set<InstrumentI> allAoRef = new HashSet<InstrumentI>();
		try {
			manager.getAccess();
			List<DocumentI> list = manager.getDocumentsWithType(true, DType.ASSESOR, DType.TAX);
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

	private boolean addAoAndTaxReferenceSearches(TSServerInfo serverInfo, List<TSServerInfoModule> modules, Set<InstrumentI> allAoRef, long searchId,
			boolean isUpdate) {
		boolean atLeastOne = false;
		final Set<String> searched = new HashSet<String>();

		for (InstrumentI inst : allAoRef) {
			boolean temp = addBookPageSearch(inst, serverInfo, modules, searchId, searched, isUpdate);
			atLeastOne = atLeastOne || temp;
			temp = addInstNoSearch(inst, serverInfo, modules, searchId, searched, isUpdate);
			atLeastOne = atLeastOne || temp;
		}
		return atLeastOne;
	}

	private static boolean addBookPageSearch(InstrumentI inst, TSServerInfo serverInfo, List<TSServerInfoModule> modules, long searchId, Set<String> searched,
			boolean isUpdate) {

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

			TSServerInfoModule module = new TSServerInfoModule(serverInfo.getModule(TSServerInfo.BOOK_AND_PAGE_MODULE_IDX));
			module.clearSaKeys();
			module.setData(0, book);
			module.setData(1, page);
			HashMap<String, String> filterCriteria = new HashMap<String, String>();
			filterCriteria.put("Book", book);
			filterCriteria.put("Page", page);
			GenericInstrumentFilter filter = new GenericInstrumentFilter(searchId, filterCriteria);
			module.addFilter(filter);
			module.addFilter(new RejectAlreadySavedDocumentsFilterResponse(searchId));
			modules.add(module);
			return true;
		}
		return false;
	}

	private boolean addInstNoSearch(InstrumentI inst, TSServerInfo serverInfo, List<TSServerInfoModule> modules, long searchId, Set<String> searched,
			boolean isUpdate) {
		if (inst.hasInstrNo()) {

			String instr = inst.getInstno().replaceFirst("^0+", "");
			if (!searched.contains(instr)) {
				searched.add(instr);
			} else {
				return false;
			}
			TSServerInfoModule module = new TSServerInfoModule(serverInfo.getModule(TSServerInfo.INSTR_NO_MODULE_IDX));
			module.setData(0, instr);
			if (isUpdate) {
				module.addFilter(new BetweenDatesFilterResponse(searchId));
			}
			module.addFilter(new RejectAlreadySavedDocumentsFilterResponse(searchId));
			modules.add(module);
			return true;
		}
		return false;
	}

	static protected class LegalDescriptionIterator extends GenericRuntimeIterator<PersonalDataStruct> implements LegalDescriptionIteratorI {

		private static final long	serialVersionUID		= 9238625486117069L;
		private boolean				enableSubdividedLegal	= false;
		private boolean				enableTownshipLegal		= false;

		LegalDescriptionIterator(long searchId) {
			super(searchId);
		}
		
		@Override
		public boolean isTransferAllowed(RegisterDocumentI doc) {
			return doc != null && doc.isOneOf(DocumentTypes.TRANSFER);
		}
		
		@Override
		public void loadSecondaryPlattedLegal(LegalI legal, ro.cst.tsearch.search.iterator.data.LegalStruct legalStruct) {
			legalStruct.setLot(legal.getSubdivision().getLot());
			legalStruct.setBlock(legal.getSubdivision().getBlock());
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
			Search global = InstanceManager.getManager().getCurrentInstance(searchId).getCrtSearchContext();
			DocumentsManagerI m = global.getDocManager();
			String key = "TX_BEXAR_RO_LOOK_UP_DATA";
			if (isEnableSubdividedLegal())
				key += "_SPL";
			else if (isEnableTownshipLegal())
				key += "_STR";
			List<PersonalDataStruct> legalStructList = (List<PersonalDataStruct>) global.getAdditionalInfo(key);

			String aoAndTrLots = global.getSa().getAtribute(SearchAttributes.LD_LOTNO);
			String[] allAoAndTrlots = new String[0];

			if (!StringUtils.isEmpty(aoAndTrLots)) {
				Vector<LotInterval> lots = LotMatchAlgorithm.prepareLotInterval(aoAndTrLots);
				HashSet<String> lotExpanded = new LinkedHashSet<String>();
				for (Iterator<LotInterval> iterator = lots.iterator(); iterator.hasNext();) {
					lotExpanded.addAll(((LotInterval) iterator.next()).getLotList());
				}
				allAoAndTrlots = lotExpanded.toArray(allAoAndTrlots);
			}
			boolean hasLegal = false;
			if (legalStructList == null) {
				legalStructList = new ArrayList<PersonalDataStruct>();

				try {

					m.getAccess();
					List<DocumentI> listRodocs = new ArrayList<DocumentI>();
					listRodocs.addAll(ro.cst.tsearch.search.iterator.legal.LegalDescriptionIterator.getGoodDocumentsOrForCurrentOwner(this, m,global,true));
					if(listRodocs.isEmpty()){
						listRodocs.addAll(ro.cst.tsearch.search.iterator.legal.LegalDescriptionIterator.getGoodDocumentsOrForCurrentOwner(this, m, global, false));
					}
					if(listRodocs.isEmpty()){
						listRodocs.addAll(m.getRoLikeDocumentList(true));
					}
							
					DocumentUtils.sortDocuments(listRodocs, MultilineElementsMap.DATE_ORDER_ASC);
					for (DocumentI reg : listRodocs) {
						if (reg instanceof RegisterDocumentI) {
							for (PropertyI prop : reg.getProperties()) {
								if (prop.hasLegal()) {
									LegalI legal = prop.getLegal();

									if (legal.hasSubdividedLegal() && isEnableSubdividedLegal()) {
										hasLegal = true;
										PersonalDataStruct legalStructItem = new PersonalDataStruct();
										SubdivisionI subdiv = legal.getSubdivision();

										String subName = subdiv.getName();
										String block = subdiv.getBlock();
										String lot = org.apache.commons.lang.StringUtils.defaultString(subdiv.getLot());
										String[] lots = lot.split(" ");
										if (StringUtils.isNotEmpty(subName)) {
											// clean subdiv name
											subName = subName.replaceAll("(?is)\\(([^\\)]*)\\)", "#$1").trim();
											subName = subName.replaceAll(" PUD\\s|$", " ").replaceAll("\\s+"," ").trim();
											
											for (int i = 0; i < lots.length; i++) {
												legalStructItem = new PersonalDataStruct();
												legalStructItem.subName = subName;
												legalStructItem.block = StringUtils.isEmpty(block) ? "" : block;
												legalStructItem.lot = lots[i].contains("-") ? "" : lots[i];
												if (!testIfExist(legalStructList, legalStructItem, "subdivision")) {
													legalStructList.add(legalStructItem);
												}
											}
										}
									}
									// if (legal.hasTownshipLegal() && isEnableTownshipLegal()) {
									// PersonalDataStruct legalStructItem = new PersonalDataStruct();
									// TownShipI township = legal.getTownShip();
									//
									// String sec = township.getSection();
									// String tw = township.getTownship();
									// String rg = township.getRange();
									// if (StringUtils.isNotEmpty(sec)) {
									// legalStructItem.section = StringUtils.isEmpty(sec) ? "" : sec;
									// legalStructItem.township = StringUtils.isEmpty(tw) ? "" : tw;
									// legalStructItem.range = StringUtils.isEmpty(rg) ? "" : rg;
									//
									// if (!testIfExist(legalStructList, legalStructItem, "sectional")) {
									// legalStructList.add(legalStructItem);
									// }
									// }
									// }
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

		protected void loadDerrivation(TSServerInfoModule module, PersonalDataStruct str) {
			FilterResponse lotFilter = LegalFilterFactory.getDefaultLotFilter(searchId);
			FilterResponse blockFilter = LegalFilterFactory.getDefaultBlockFilter(searchId);

			if (module.getExtraInformation(TSServerInfoConstants.EXTRA_PARAM_MODULE_DESCRIPTION)
					.equals(TSServerInfoConstants.VALUE_PARAM_SUBDIVISION_LOT_BLOCK)) {
				if (StringUtils.isNotEmpty(str.subName) || StringUtils.isNotEmpty(str.lot) || StringUtils.isNotEmpty(str.block)) {
					module.setData(0, StringUtils.isNotEmpty(str.lot) ? str.lot : "");
					module.setData(1, StringUtils.isNotEmpty(str.block) ? str.block : "");
					module.setData(4, StringUtils.isNotEmpty(str.subName) ? str.subName : "");
					module.addValidator(lotFilter.getValidator());
					module.addValidator(blockFilter.getValidator());
					module.setVisible(true);
				} else {
					module.setVisible(false);
				}
			}
		}
	}

	protected static class PersonalDataStruct implements Cloneable {
		String	subName	= "";
		String	lot		= "";
		String	block	= "";

		// String section = "";
		// String township = "";
		// String range = "";

		@Override
		protected Object clone() throws CloneNotSupportedException {
			return super.clone();
		}

		public boolean equalsSubdivision(PersonalDataStruct struct) {
//			boolean retVal = true;
//			if(StringUtils.isNotEmpty(this.block) && StringUtils.isNotEmpty(struct.block)){
//				retVal = this.block.equals(struct.block);
//			} 
//			
//			if(StringUtils.isNotEmpty(this.lot) && StringUtils.isNotEmpty(struct.lot)){
//				retVal = retVal && this.lot.equals(struct.lot);
//			} 
//			
//			if(StringUtils.isNotEmpty(this.subName) && StringUtils.isNotEmpty(struct.subName)){
//				retVal = retVal && this.subName.equals(struct.subName);
//			}
			
			return this.block.equals(struct.block) && this.lot.equals(struct.lot) && this.subName.equals(struct.subName);
		}

		// public boolean equalsSectional(PersonalDataStruct struct) {
		// return this.section.equals(struct.section) && this.township.equals(struct.township) && this.range.equals(struct.range);
		// }

	}

	private static boolean testIfExist(List<PersonalDataStruct> legalStruct2, PersonalDataStruct l, String string) {

		if ("subdivision".equalsIgnoreCase(string)) {
			for (PersonalDataStruct p : legalStruct2) {
				if (l.equalsSubdivision(p)) {
					return true;
				}
			}
		}
		// else if ("sectional".equalsIgnoreCase(string)) {
		// for (PersonalDataStruct p : legalStruct2) {
		// if (l.equalsSectional(p)) {
		// return true;
		// }
		// }
		// }
		return false;
	}

	private void addIteratorModule(TSServerInfo serverInfo, List<TSServerInfoModule> modules, int code, long searchId) {
		FilterResponse lotFilter = LegalFilterFactory.getDefaultLotFilter(searchId);
		FilterResponse blockFilter = LegalFilterFactory.getDefaultBlockFilter(searchId);
		FilterResponse addressFilter = AddressFilterFactory.getAddressHybridFilter(searchId, 0.8d, true);
		FilterResponse unitFilter = LegalFilterFactory.getDefaultUnitFilter(searchId);
		
		TSServerInfoModule module = new TSServerInfoModule(serverInfo.getModule(code));
		module.addExtraInformation(TSServerInfoConstants.EXTRA_PARAM_MODULE_DESCRIPTION, TSServerInfoConstants.VALUE_PARAM_SUBDIVISION_LOT_BLOCK);
		module.clearSaKeys();
		LegalDescriptionIterator it = new LegalDescriptionIterator(searchId);
		it.setEnableSubdividedLegal(true);
		module.addValidator(blockFilter.getValidator());
		module.addValidator(lotFilter.getValidator());
		module.addValidator(addressFilter.getValidator());
		module.addValidator(unitFilter.getValidator());
		module.addIterator(it);
		modules.add(module);
	}

	protected void setModulesForAutoSearch(TSServerInfo serverInfo) {
		List<TSServerInfoModule> modules = new ArrayList<TSServerInfoModule>();
		Search global = InstanceManager.getManager().getCurrentInstance(searchId).getCrtSearchContext();
		Set<InstrumentI> allAoRef = getAllAoAndTaxReferences(global);

		Search search = getSearch();
		TSServerInfoModule m = null;
		SearchAttributes sa = search.getSa();

		FilterResponse rejectSavedDocuments = new RejectAlreadySavedDocumentsFilterResponse(searchId);
		GenericLegal defaultLegalFilter = (GenericLegal)LegalFilterFactory.getDefaultLegalFilter(searchId);
		defaultLegalFilter.setEnableLot(true);

		FilterResponse subdivisionNameFilter = NameFilterFactory.getDefaultNameFilterForSubdivision(searchId);
		subdivisionNameFilter.setThreshold(new BigDecimal(0.90d));
		FilterResponse unitNameFilter = LegalFilterFactory.getDefaultUnitFilter(searchId);

		// P1 instrument list search from AO and TR for finding Legal
		addAoAndTaxReferenceSearches(serverInfo, modules, allAoRef, searchId, isUpdate());

		// P2 search with lot/block/subdivision from RO documents
		addIteratorModule(serverInfo, modules, TSServerInfo.SUBDIVISION_MODULE_IDX, searchId);

		// P3 search with sec/twp/rng from RO documents

		// P4 search by sub name
		String subdivisionName = sa.getAtribute(SearchAttributes.LD_SUBDIV_NAME);
		if (StringUtils.isNotEmpty(subdivisionName)) {
			m = new TSServerInfoModule(serverInfo.getModule(TSServerInfo.SUBDIVISION_MODULE_IDX));
			m.addExtraInformation(TSServerInfoConstants.EXTRA_PARAM_MODULE_DESCRIPTION, TSServerInfoConstants.VALUE_PARAM_SUBDIVISION_LOT);
			m.clearSaKeys();
			m.forceValue(0, org.apache.commons.lang.StringUtils.defaultString(sa.getAtribute(SearchAttributes.LD_LOTNO)));
			m.forceValue(1, org.apache.commons.lang.StringUtils.defaultString(sa.getAtribute(SearchAttributes.LD_SUBDIV_BLOCK)));
			if(StringUtils.isNotEmpty(sa.getAtribute(SearchAttributes.LD_SUBDIV_UNIT))){
				subdivisionName += " #"+sa.getAtribute(SearchAttributes.LD_SUBDIV_UNIT);
			}
			m.forceValue(4, subdivisionName);
			m.setSaKey(6, SearchAttributes.FROMDATE_MM_SLASH_DD_SLASH_YYYY);
			m.setSaKey(7, SearchAttributes.TODATE_MM_SLASH_DD_SLASH_YYYY);
			m.addFilter(subdivisionNameFilter);
			m.addValidator(defaultLegalFilter.getValidator());
			m.addValidator(unitNameFilter.getValidator());
			modules.add(m);
		}

		ConfigurableNameIterator nameIterator = null;
		GenericNameFilter defaultNameFilter = (GenericNameFilter) NameFilterFactory.getDefaultNameFilter(SearchAttributes.OWNER_OBJECT, searchId, m);
		defaultNameFilter.setIgnoreMiddleOnEmpty(true);
		defaultNameFilter.setUseArrangements(false);
		defaultNameFilter.setInitAgain(true);

		FilterResponse addressFilter = AddressFilterFactory.getAddressHybridFilter(searchId, 0.8d, true);
		
		// P5 name modules with names from search page.
		if (hasOwner()) {
			m = new TSServerInfoModule(serverInfo.getModule(TSServerInfo.NAME_MODULE_IDX));
			m.addExtraInformation(TSServerInfoConstants.EXTRA_PARAM_MODULE_DESCRIPTION, TSServerInfoConstants.VALUE_PARAM_NAME_OWNERS);
			m.setSaObjKey(SearchAttributes.OWNER_OBJECT);
			m.clearSaKeys();
			m.setSaKey(3, SearchAttributes.FROMDATE_MM_SLASH_DD_SLASH_YYYY);
			m.setSaKey(4, SearchAttributes.TODATE_MM_SLASH_DD_SLASH_YYYY);

			m.addFilter(rejectSavedDocuments);
			m.addFilter(defaultNameFilter);
			m.addFilter(new LastTransferDateFilter(searchId));
			addFilterForUpdate(m, true);

			m.addValidator(defaultLegalFilter.getValidator());
			m.addValidator(addressFilter.getValidator());
			m.setIteratorType(0, FunctionStatesIterator.ITERATOR_TYPE_LFM_NAME_FAKE);
			nameIterator = (ConfigurableNameIterator) ModuleStatesIteratorFactory.getConfigurableNameIterator(m, searchId, new String[] { "L;F;M", "L;F;" });
			m.addIterator(nameIterator);
			modules.add(m);
		}

		// P5 search by buyers
		if (hasBuyer()
				&& !InstanceManager.getManager().getCurrentInstance(searchId).getCrtSearchContext().isProductType(SearchAttributes.SEARCH_PROD_REFINANCE)) {
			m = new TSServerInfoModule(serverInfo.getModule(TSServerInfo.NAME_MODULE_IDX));
			m.addExtraInformation(TSServerInfoConstants.EXTRA_PARAM_MODULE_DESCRIPTION, TSServerInfoConstants.VALUE_PARAM_NAME_BUYERS);
			m.setSaObjKey(SearchAttributes.BUYER_OBJECT);
			m.clearSaKeys();
			m.setSaKey(3, SearchAttributes.FROMDATE_MM_SLASH_DD_SLASH_YYYY);
			m.setSaKey(4, SearchAttributes.TODATE_MM_SLASH_DD_SLASH_YYYY);

			FilterResponse nameFilter = NameFilterFactory.getDefaultNameFilter(SearchAttributes.BUYER_OBJECT, searchId, m);
			((GenericNameFilter) nameFilter).setIgnoreMiddleOnEmpty(true);
			((GenericNameFilter) nameFilter).setUseArrangements(false);
			((GenericNameFilter) nameFilter).setInitAgain(true);
			m.addFilter(rejectSavedDocuments);
			m.addFilter(nameFilter);
			addFilterForUpdate(m, true);

			m.addValidator(defaultLegalFilter.getValidator());
			m.setIteratorType(0, FunctionStatesIterator.ITERATOR_TYPE_LFM_NAME_FAKE);
			m.setIteratorType(3, FunctionStatesIterator.ITERATOR_TYPE_FROM_DATE);
			ConfigurableNameIterator buyerNameIterator = (ConfigurableNameIterator) ModuleStatesIteratorFactory.getConfigurableNameIterator(m, searchId,
					new String[] { "L;F;M", "L;F;" });
			buyerNameIterator.setAllowMcnPersons(false);
			m.addIterator(buyerNameIterator);
			modules.add(m);
		}

		// P6 OCR last transfer - book page search
		m = new TSServerInfoModule(serverInfo.getModule(TSServerInfo.BOOK_AND_PAGE_MODULE_IDX));
		m.clearSaKeys();
		m.setIteratorType(ModuleStatesIterator.TYPE_OCR_FULL_OR_BOOTSTRAPER);
		m.getFunction(0).setIteratorType(FunctionStatesIterator.ITERATOR_TYPE_BOOK_SEARCH);
		m.getFunction(1).setIteratorType(FunctionStatesIterator.ITERATOR_TYPE_PAGE_SEARCH);
		m.addFilter(new GenericInstrumentFilter(searchId));
		m.addFilter(defaultLegalFilter);
		modules.add(m);

		// P7 name module with names added by OCR
		m = new TSServerInfoModule(serverInfo.getModule(TSServerInfo.NAME_MODULE_IDX));
		m.addExtraInformation(TSServerInfoConstants.EXTRA_PARAM_MODULE_DESCRIPTION, TSServerInfoConstants.VALUE_PARAM_NAME_OWNERS);
		m.setSaObjKey(SearchAttributes.OWNER_OBJECT);
		m.clearSaKeys();

		m.addFilter(rejectSavedDocuments);
		m.addFilter(defaultNameFilter);
		m.addFilter(new LastTransferDateFilter(searchId));
		m.addValidator(defaultLegalFilter.getValidator());
		m.setSaKey(3, SearchAttributes.FROMDATE_MM_SLASH_DD_SLASH_YYYY);
		m.setSaKey(4, SearchAttributes.TODATE_MM_SLASH_DD_SLASH_YYYY);
		m.setIteratorType(0, FunctionStatesIterator.ITERATOR_TYPE_LFM_NAME_FAKE);
		m.setIteratorType(3, FunctionStatesIterator.ITERATOR_TYPE_FROM_DATE);
		ArrayList<NameI> searchedNames = null;
		if (nameIterator != null) {
			searchedNames = nameIterator.getSearchedNames();
		} else {
			searchedNames = new ArrayList<NameI>();
		}
		nameIterator = (ConfigurableNameIterator) ModuleStatesIteratorFactory.getConfigurableNameIterator(m, searchId, new String[] { "L;F;M", "L;F;" });
		// get your values at runtime
		nameIterator.setInitAgain(true);
		nameIterator.setSearchedNames(searchedNames);
		m.addIterator(nameIterator);
		modules.add(m);

		serverInfo.setModulesForAutoSearch(modules);
	}

	protected void setModulesForGoBackOneLevelSearch(TSServerInfo serverInfo) {

		ConfigurableNameIterator nameIterator = null;
		String endDate = new SimpleDateFormat("MM/dd/yyyy").format(new Date());

		SearchAttributes sa = InstanceManager.getManager().getCurrentInstance(searchId).getCrtSearchContext().getSa();
		List<TSServerInfoModule> modules = new ArrayList<TSServerInfoModule>();
		TSServerInfoModule module;
		GBManager gbm = (GBManager) sa.getObjectAtribute(SearchAttributes.GB_MANAGER_OBJECT);

		for (String id : gbm.getGbTransfers()) {

			module = new TSServerInfoModule(serverInfo.getModule(TSServerInfo.NAME_MODULE_IDX));
			module.setIndexInGB(id);
			module.setTypeSearchGB("grantor");
			module.clearSaKeys();
			module.setSaObjKey(SearchAttributes.GB_MANAGER_OBJECT);

			String date = gbm.getDateForSearch(id, "MM/dd/yyyy", searchId);
			if (date != null) {
				module.getFunction(3).forceValue(date);
			}
			module.setValue(4, endDate);

			module.addFilter(NameFilterFactory.getDefaultNameFilter(SearchAttributes.GB_MANAGER_OBJECT, searchId, module));
			module.setIteratorType(0, FunctionStatesIterator.ITERATOR_TYPE_LFM_NAME_FAKE);
			nameIterator = (ConfigurableNameIterator) ModuleStatesIteratorFactory.getConfigurableNameIterator(module, searchId,
					new String[] { "L;F;M", "L;F;" });
			module.addIterator(nameIterator);
			module.addFilter(NameFilterFactory.getDefaultTransferNameFilter(searchId, 0.90d, module));
			module.addFilter(DateFilterFactory.getDateFilterForGoBack(SearchAttributes.GB_MANAGER_OBJECT, searchId, module));
			modules.add(module);

			if (gbm.getNamesForBrokenChain(id, searchId).size() > 0) {
				module = new TSServerInfoModule(serverInfo.getModule(TSServerInfo.NAME_MODULE_IDX));
				module.setIndexInGB(id);
				module.setTypeSearchGB("grantee");
				module.clearSaKeys();
				module.setSaObjKey(SearchAttributes.GB_MANAGER_OBJECT);
				date = gbm.getDateForSearchBrokenChain(id, "MM/dd/yyyy", searchId);
				if (date != null)
					module.getFunction(3).forceValue(date);
				module.setValue(4, endDate);
				module.setIteratorType(0, FunctionStatesIterator.ITERATOR_TYPE_LFM_NAME_FAKE);
				module.addFilter(NameFilterFactory.getDefaultNameFilter(SearchAttributes.GB_MANAGER_OBJECT, searchId, module));
				nameIterator = (ConfigurableNameIterator) ModuleStatesIteratorFactory.getConfigurableNameIterator(module, searchId,
						new String[] { "L;F;M", "L;F;" });
				module.addIterator(nameIterator);
				module.addFilter(NameFilterFactory.getDefaultTransferNameFilter(searchId, 0.90d, module));
				module.addFilter(DateFilterFactory.getDateFilterForGoBack(SearchAttributes.GB_MANAGER_OBJECT, searchId, module));
				modules.add(module);
			}
		}
		serverInfo.setModulesForGoBackOneLevelSearch(modules);
	}

	@Override
	protected NameI getNameFromModule(TSServerInfoModule module) {
		NameI name = new Name();
		if (module.getModuleIdx() == TSServerInfo.NAME_MODULE_IDX && module.getFunctionCount() > 1) {
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
	protected LegalI getLegalFromModule(TSServerInfoModule module) {
		LegalI legal = new Legal();

		if (module.getModuleIdx() == TSServerInfo.SUBDIVISION_MODULE_IDX && module.getFunctionCount() > 11) {
			SubdivisionI subdivision = new Subdivision();

			subdivision.setName(module.getFunction(4).getParamValue().trim());
			subdivision.setLot(module.getFunction(0).getParamValue().trim());
			subdivision.setBlock(module.getFunction(1).getParamValue().trim());

			TownShipI townShip = new TownShip();

			legal = new Legal();
			legal.setSubdivision(subdivision);
			legal.setTownShip(townShip);
		}

		return legal;
	}

	@Override
	public TSServerInfoModule getRecoverModuleFrom(RestoreDocumentDataI restoreDocumentDataI) {
		if (restoreDocumentDataI == null) {
			return null;
		}

		TSServerInfoModule module = null;

		String book = restoreDocumentDataI.getBook();
		String page = restoreDocumentDataI.getPage();

		if (StringUtils.isNotEmpty(book) && StringUtils.isNotEmpty(page)) {
			HashMap<String, String> filterCriteria = new HashMap<String, String>();
			module = new TSServerInfoModule(getDefaultServerInfo().getModule(TSServerInfo.BOOK_AND_PAGE_MODULE_IDX));
			filterCriteria.put("Book", book);
			filterCriteria.put("Page", page);
			module.forceValue(0, book);
			module.forceValue(1, page);
			module.forceValue(7, "17003");
			GenericInstrumentFilter filter = new GenericInstrumentFilter(searchId, filterCriteria);
			module.getFilterList().clear();
			module.addFilter(filter);
		}
		return module;
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
			imageBytes = ((ro.cst.tsearch.connection.http2.TXBexarRO) site).getImage(link);
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

	
	@Override
	public boolean isInstrumentSaved(String instrumentNo, DocumentI documentToCheck, HashMap<String, String> data){
  	  return isInstrumentSaved(instrumentNo, documentToCheck, data, false);
    }
	
	@Override
	public boolean isInstrumentSaved(String instrumentNo, DocumentI documentToCheck, HashMap<String, String> data, boolean checkMiServerId) {
    	
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
    		for(DocumentI e: documentManager.getDocumentsWithDataSource(false, "TP")){
    			InstrumentI savedInst = e.getInstrument();
    			if( savedInst.getInstno().equals(instToCheck.getInstno())  
    					&& (savedInst.getBook().equals(instToCheck.getBook()) && savedInst.getPage().equals(instToCheck.getPage()))  
    					&& savedInst.getDocno().equals(instToCheck.getDocno())
    					&& e.getDocType().equals(documentToCheck.getDocType())
    					&& (savedInst.getYear() == instToCheck.getYear() || instToCheck.getYear() == SimpleChapterUtils.UNDEFINED_YEAR)
    			){
    				return true;
    			}
    		}
    	} finally {
    		documentManager.releaseAccess();
    	}
		
    	return false;
    }

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
					html = ((ro.cst.tsearch.connection.http2.TXBexarRO) site).getCertDate();
				} catch (RuntimeException e) {
					e.printStackTrace();
				} finally {
					HttpManager.releaseSite(site);
				}
	
				if (StringUtils.isNotEmpty(html)) {
					try {
						NodeList mainList = new HtmlParser3(html).getNodeList();
	
						mainList = mainList
								.extractAllNodesThatMatch(new TagNameFilter("div"), true)
								.extractAllNodesThatMatch(new HasAttributeFilter("id", "searches_screen_help"), true)
								.extractAllNodesThatMatch(new TagNameFilter("table"), true)
								.extractAllNodesThatMatch(new HasAttributeFilter("cellpadding", "2"));
						if (mainList.size() > 0) {
							TableTag t = (TableTag) mainList.elementAt(0);
							String date = t.getRow(0).getColumns()[3].toPlainTextString();
							
							CertificationDateManager.cacheCertificationDate(dataSite, date);
							getSearch().getSa().updateCertificationDateObject(dataSite, Util.dateParser3(date));
						}
					} catch (Exception e) {
						e.printStackTrace();
					}
				}
			}
		} catch (Exception e) {
			logger.error(e.getMessage());
		}
	}

	protected String getInstrumentNumberForSavingInFinalResults(DocumentI doc) {
		return (doc.getDocno() + doc.getDocType() + doc.getBook() + doc.getPage()).toUpperCase();
	}

	protected Object parseAndFillResultMap(ServerResponse response, String detailsHtml, ResultMap map) {
		ro.cst.tsearch.servers.functions.TXBexarRO.parseAndFillResultMap(response, detailsHtml, map, searchId);
		return null;
	}
}
