package ro.cst.tsearch.servers.types;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Vector;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang.time.DateFormatUtils;
import org.htmlparser.Node;
import org.htmlparser.filters.HasAttributeFilter;
import org.htmlparser.filters.TagNameFilter;
import org.htmlparser.tags.LinkTag;
import org.htmlparser.tags.TableColumn;
import org.htmlparser.tags.TableRow;
import org.htmlparser.tags.TableTag;
import org.htmlparser.util.NodeList;

import ro.cst.tsearch.Search;
import ro.cst.tsearch.bean.SearchAttributes;
import ro.cst.tsearch.exceptions.ServerResponseException;
import ro.cst.tsearch.extractor.xml.Bridge;
import ro.cst.tsearch.extractor.xml.ResultMap;
import ro.cst.tsearch.generic.Util;
import ro.cst.tsearch.parser.HtmlParser3;
import ro.cst.tsearch.search.FunctionStatesIterator;
import ro.cst.tsearch.search.ModuleStatesIterator;
import ro.cst.tsearch.search.filter.BetweenDatesFilterResponse;
import ro.cst.tsearch.search.filter.FilterResponse;
import ro.cst.tsearch.search.filter.newfilters.legal.GenericInstrumentFilter;
import ro.cst.tsearch.search.filter.newfilters.legal.GenericLegal;
import ro.cst.tsearch.search.filter.newfilters.legal.LegalFilterFactory;
import ro.cst.tsearch.search.filter.newfilters.name.NameFilterFactory;
import ro.cst.tsearch.search.filter.newfilters.pin.PINFilterFactory;
import ro.cst.tsearch.search.iterator.ModuleStatesIteratorFactory;
import ro.cst.tsearch.search.iterator.instrument.InstrumentGenericIterator;
import ro.cst.tsearch.search.module.ConfigurableNameIterator;
import ro.cst.tsearch.servers.TSConnectionURL;
import ro.cst.tsearch.servers.info.TSServerInfo;
import ro.cst.tsearch.servers.info.TSServerInfoConstants;
import ro.cst.tsearch.servers.info.TSServerInfoModule;
import ro.cst.tsearch.servers.response.LinkInPage;
import ro.cst.tsearch.servers.response.OtherInformationSet.OtherInformationSetKey;
import ro.cst.tsearch.servers.response.ParsedResponse;
import ro.cst.tsearch.servers.response.SaleDataSet.SaleDataSetKey;
import ro.cst.tsearch.servers.response.ServerResponse;
import ro.cst.tsearch.servlet.URLConnectionReader;
import ro.cst.tsearch.utils.StringUtils;
import ro.cst.tsearch.utils.Tidy;

import com.stewart.ats.base.document.DocumentI;
import com.stewart.ats.base.document.RegisterDocumentI;

/**
 * @author costi
 */
public class CASantaClaraRO extends TSServer implements TSServerROLikeI {
	/**
	 * 
	 */
	private static final long	serialVersionUID	= 1L;
	private static final Pattern certDatePattern = Pattern.compile("(?is)through the most recent web-site update:&nbsp;(\\d{1,2}/\\d{1,2}/\\d{4})");

	public CASantaClaraRO(long searchId) {
		super(searchId);
		resultType = MULTIPLE_RESULT_TYPE;
	}

	public CASantaClaraRO(String rsRequestSolverName, String rsSitePath,
			String rsServerID, String rsPrmNameLink, long searchId,
			int miServerID) {
		super(rsRequestSolverName, rsSitePath, rsServerID, rsPrmNameLink,
				searchId, miServerID);
		resultType = MULTIPLE_RESULT_TYPE;
	}

	@Override
	protected void ParseResponse(String sAction, ServerResponse response, int viParseID) throws ServerResponseException {
		String rsResponse = response.getResult();
		ParsedResponse parsedResponse = response.getParsedResponse();
		// Check if no results were found.
		if (rsResponse.contains("NO MATCHING DOCUMENTS FOUND")) {
			response.getParsedResponse().setError("No results were found for your query! Please change your search criteria and try again.");
			return;
		}
		
		rsResponse = Tidy.tidyParse(rsResponse.replaceAll("(?ims)(<table[^<]*)>?", "$1>"), null);

		switch (viParseID) {
		case ID_SEARCH_BY_MODULE20:
		case ID_SEARCH_BY_MODULE19:
		case ID_SEARCH_BY_PARCEL:
		case ID_SEARCH_BY_NAME:
		case ID_SEARCH_BY_INSTRUMENT_NO:
			// Search by PIN.
			StringBuilder outputTable = new StringBuilder();

			mSearch.setAdditionalInfo("viParseID", viParseID);
			
			Collection<ParsedResponse> smartParsedResponses = null;
			try {
				// Parse intermediary responses.
				smartParsedResponses = smartParseIntermediary(response, rsResponse, outputTable);
			} catch (Exception e) {
				logger.error(e);
			}
			if (smartParsedResponses != null && smartParsedResponses.size() > 0) {
				parsedResponse.setResultRows(new Vector<ParsedResponse>(smartParsedResponses));
				parsedResponse.setOnlyResponse(rsResponse);

				if (getSearch().getSearchType() == Search.PARENT_SITE_SEARCH) {
					if (viParseID != ID_SEARCH_BY_NAME) {
						String header = CreateSaveToTSDFormHeader(URLConnectionReader.MULTIPLE_SAVE_TO_TSD_ACTION, "POST");
						header += parsedResponse.getHeader();

						parsedResponse.setHeader(header);

						Object numberOfUnsavedDocument = GetAttribute(NUMBER_OF_UNSAVED_DOCUMENTS);
						if (numberOfUnsavedDocument != null && numberOfUnsavedDocument instanceof Integer) {
							parsedResponse.setFooter(parsedResponse.getFooter() + getPrevNext(response) +
									CreateSaveToTSDFormEnd(SAVE_SELECTED_DOCUMENTS_BUTTON_LABEL, viParseID, (Integer) numberOfUnsavedDocument));
						} else {
							parsedResponse.setFooter(parsedResponse.getFooter() + getPrevNext(response)
									+ CreateSaveToTSDFormEnd(SAVE_SELECTED_DOCUMENTS_BUTTON_LABEL, viParseID, -1));
						}
					} else {
						parsedResponse.setFooter(parsedResponse.getFooter() + getPrevNext(response));
					}

				} else {
	            	parsedResponse.setAttribute(ParsedResponse.SERVER_RECURSIVE_ANALYZE, true);
				}
				
				if (viParseID == ID_SEARCH_BY_INSTRUMENT_NO) {
					response.getParsedResponse().setNextLink("");
				}

			} else {
				// No intermediary response found.
				parsedResponse.setError("<font color=\"red\">No results found.</font> Please try again.");
				return;
			}
			break;
		case ID_DETAILS:
		case ID_SAVE_TO_TSD:
			// Parse the document response.
			searchId = getSearch().getID();

			StringBuilder accountId = new StringBuilder();
			HashMap<String, String> data = new HashMap<String, String>();
			String details = getDetails(response, rsResponse, accountId, data);
			String accountName = accountId.toString();
			
			if (viParseID != ID_SAVE_TO_TSD) {
				String originalLink = (sAction.replace("?", "&") + "&" + response.getQuerry());
				String sSave2TSDLink = getLinkPrefix(TSConnectionURL.idGET) + originalLink;

				if (isInstrumentSaved(accountName, null, data)) {
					details += CreateFileAlreadyInTSD();
				} else {
					mSearch.addInMemoryDoc(sSave2TSDLink.replaceAll(" ","%20"), details);
					details = addSaveToTsdButton(details, sSave2TSDLink, ID_DETAILS);
				}
				response.getParsedResponse().setPageLink(new LinkInPage(sSave2TSDLink, originalLink, TSServer.REQUEST_SAVE_TO_TSD));
				response.getParsedResponse().setResponse(details);

			} else {
				smartParseDetails(response, details);

				msSaveToTSDFileName = accountName + ".html";
				response.getParsedResponse().setFileName(getServerTypeDirectory() + msSaveToTSDFileName);
				
				String detailsAux = details;
				try{
					detailsAux = details.replaceAll("(?ism)<a href[^>]*>[^/]*/a>", "");
					detailsAux = detailsAux.replace("%26","&").replace("%2F","/").replace("%28", "(").replace("%29", ")").replace("%22", "\"");
				} catch (Exception e) {
					e.printStackTrace();
				}
				response.getParsedResponse().setResponse(detailsAux);

				msSaveToTSDResponce = details + CreateFileAlreadyInTSD();
			}
			
			break;
		case ID_GET_LINK:
			if (sAction.contains("alldetail")){
				ParseResponse(sAction, response, ID_DETAILS);
//			} else if(org.apache.commons.lang.StringUtils.containsIgnoreCase(response.getLastURI().getEscapedURI(), "oresultg01.MBR") ||
//					org.apache.commons.lang.StringUtils.containsIgnoreCase(response.getLastURI().getEscapedURI(), "oresultc01.MBR") ||
//					org.apache.commons.lang.StringUtils.containsIgnoreCase(response.getLastURI().getEscapedURI(), "osearchn01.mbr") || 
//					org.apache.commons.lang.StringUtils.containsIgnoreCase(response.getLastURI().getEscapedURI(), "Osearchad01.html")){
//				ParseResponse(sAction, response, ID_SEARCH_BY_INSTRUMENT_NO);
			} else if(org.apache.commons.lang.StringUtils.containsIgnoreCase(rsResponse, "Search by Grantor or Grantee Name") &&
					!org.apache.commons.lang.StringUtils.containsIgnoreCase(rsResponse, "Narrow Search by Date")){
				ParseResponse(sAction, response, ID_SEARCH_BY_NAME);
			} else {
				ParseResponse(sAction, response, ID_SEARCH_BY_INSTRUMENT_NO);
			}
			break;
		}
	}
	
	protected String getDetails(ServerResponse response, String rsResponse, StringBuilder accountId, HashMap<String, String> data) {
		try {
			StringBuilder details = new StringBuilder();

			NodeList nodes = new HtmlParser3(Tidy.tidyParse(rsResponse, null)).getNodeList();
			NodeList tables = nodes.extractAllNodesThatMatch(new TagNameFilter("table"), true)
					.extractAllNodesThatMatch(new HasAttributeFilter("border", "5"), true);
					

			if (tables.size() < 2)
				return "";

			String instrumentnumber = HtmlParser3.getValueFromAbsoluteCell(0, 1, HtmlParser3.findNode(tables, "Document Number:"), "", false);

			String recDate = HtmlParser3.getValueFromAbsoluteCell(0, 1, HtmlParser3.findNode(tables, "Document Date:"), "", false);
			if (StringUtils.isNotEmpty(recDate)){
				if (recDate.matches("(?is)\\d{2}/\\d{2}/\\d{4}")){
					String year = recDate.substring(recDate.lastIndexOf("/") + 1);
					instrumentnumber = year + "-" + instrumentnumber;
				}
			}
			accountId.append(instrumentnumber);
			data.put("type", dataSite.getSiteTypeAbrev());
			data.put("instrNo", accountId.toString());
			data.put("type", HtmlParser3.getValueFromAbsoluteCell(0, 1, HtmlParser3.findNode(tables, "Document Type:"), "", false));

			/* If from memory - use it as is */
			if (!org.apache.commons.lang.StringUtils.containsIgnoreCase(rsResponse, "<html")) {
				return rsResponse;
			}

			// arange the summary html
			TableTag docTable = (TableTag) tables.elementAt(0);
			TableTag nameTable = (TableTag) tables.elementAt(1);

			Map<String, String> attributesMap = new HashMap<String, String>();
			attributesMap.put("border", "0");
			attributesMap.put("width", "100");

			TableTag linkT = (TableTag) ro.cst.tsearch.servers.functions.CASantaClaraRO
					.getNodeByTypeAttributeDescription(nodes, "table", attributesMap, new String[] { "NEXT 10" }, true);

			String link = "";

			if (linkT != null && linkT.getRowCount() > 0 &&
					linkT.getRow(0).getColumnCount() == 2 &&
					linkT.getRows()[0].getColumns()[1].getChildCount() > 1 &&
					linkT.getRows()[0].getColumns()[1].getChild(1) instanceof LinkTag) {
				LinkTag l = (LinkTag) linkT.getRows()[0].getColumns()[1].getChild(1);
				link = l.getLink();
			}

			if (org.apache.commons.lang.StringUtils.isNotEmpty(link)) {
				List<TableRow> nameRows = getAllOwners(link);

				if (nameRows != null) {
					NodeList children = new NodeList();

					List<TableRow> newNames = new ArrayList<TableRow>();

					Collections.addAll(newNames, nameTable.getRows());
					newNames.addAll(nameRows);

					for (TableRow r : newNames) {
						children.add(r);
					}

					nameTable.setChildren(children);
				}
			}

			docTable.setAttribute("id", "docTable");
			docTable.setAttribute("width", "60%");
			docTable.setAttribute("align", "center");

			nameTable.setAttribute("id", "nameTable");
			nameTable.setAttribute("width", "60%");
			nameTable.setAttribute("align", "center");

			attributesMap.clear();
			attributesMap.put("border", "0");
			attributesMap.put("width", "100%");

			TableTag relatedLinkT = (TableTag) ro.cst.tsearch.servers.functions.CASantaClaraRO
					.getNodeByTypeAttributeDescription(nodes, "table", attributesMap, new String[] { "Document Details", "Next Doc" }, true);

			if (relatedLinkT == null)
				relatedLinkT = (TableTag) ro.cst.tsearch.servers.functions.CASantaClaraRO
						.getNodeByTypeAttributeDescription(nodes, "table", attributesMap, new String[] { "Document Details", "Prev Doc" }, true);

			String relatedLinkTable = "";

			if (relatedLinkT != null) {
				if (relatedLinkT.getRowCount() > 0 && relatedLinkT.getRow(0).getColumnCount() == 3) {
					boolean flag = false;
					relatedLinkTable = "<table id=related border=0 align=center width=95%>" +
							"<tr>";
					if (relatedLinkT.getRow(0).getColumns()[1].getChildCount() > 0 &&
							relatedLinkT.getRow(0).getColumns()[1].getChild(0) instanceof LinkTag) {
						String linkAux = ((LinkTag) relatedLinkT.getRow(0).getColumns()[1].getChild(0)).getLink();
						linkAux = CreatePartialLink(TSConnectionURL.idGET) + linkAux.replaceAll(".*:80/(.*)", "$1").replace("&amp;", "&").replaceAll(" ", "");
						relatedLinkTable += "<td align=left><a href=" + linkAux + ">Prev Document</a></td>";
						flag = true;
					}
					
					if (relatedLinkT.getRow(0).getColumns()[2].getChildCount() > 0 &&
							relatedLinkT.getRow(0).getColumns()[2].getChild(0) instanceof LinkTag) {
						String linkAux = ((LinkTag) relatedLinkT.getRow(0).getColumns()[2].getChild(0)).getLink();
						linkAux = CreatePartialLink(TSConnectionURL.idGET) + linkAux.replaceAll(".*:80/(.*)", "$1").replace("&amp;", "&").replaceAll(" ", "");
						relatedLinkTable += "<td align=right><a href=" + linkAux + ">Next Document</a></td>";
						flag = true;
					}

					relatedLinkTable += "</tr></table>";

					if (!flag)
						relatedLinkTable = "";
				}
			}

			details.append(docTable.toHtml() + "\n<br/>\n" + nameTable.toHtml() + relatedLinkTable + "\n");

			return details.toString();// .replaceAll("(?ism)</?a [^>]*>", "");
		} catch (Exception e) {
			e.printStackTrace();
		}
		return "";
	}
	
	private List<TableRow> getAllOwners(String link) {
		if (StringUtils.isEmpty(link))
			return null;

		List<TableRow> resRows = new ArrayList<TableRow>();
		
		try {
			String linkR = link;
			
			for (int i = 0; i < 10; i++) {
				String ownersPage = getLinkContents(linkR);

				if (StringUtils.isNotEmpty(ownersPage)) {
					NodeList nodes = new HtmlParser3(ownersPage).getNodeList().extractAllNodesThatMatch(new TagNameFilter("table"), true);

					NodeList tables = nodes.extractAllNodesThatMatch(new TagNameFilter("table"), true)
							.extractAllNodesThatMatch(new HasAttributeFilter("border", "5"), true)
							.extractAllNodesThatMatch(new HasAttributeFilter("width", "100%"), true);

					if (tables.size() >= 2) {
						TableTag nameTable = (TableTag) tables.elementAt(1);

						List<TableRow> rows = new ArrayList<TableRow>();
						Collections.addAll(rows, nameTable.getRows());
						
						if(rows.size() > 1 && rows.get(0).toHtml().contains("Grantor"))
							rows.remove(0);
						
						resRows.addAll(rows);

						Map<String, String> attributesMap = new HashMap<String, String>();
						attributesMap.put("border", "0");
						attributesMap.put("width", "100");

						Node n = ro.cst.tsearch.servers.functions.CASantaClaraRO
								.getNodeByTypeAttributeDescription(nodes, "table", attributesMap, new String[] { "NEXT 10" }, true);

						if (n != null && n instanceof TableTag) {
							TableTag linkT = (TableTag) n;

							if (linkT != null && linkT.getRowCount() > 0 &&
									linkT.getRow(0).getColumnCount() == 2 &&
									linkT.getRows()[0].getColumns()[1].getChildCount() > 1 &&
									linkT.getRows()[0].getColumns()[1].getChild(1) instanceof LinkTag) {
								LinkTag l = (LinkTag) linkT.getRows()[0].getColumns()[1].getChild(1);
								linkR = l.getLink();
								continue;
							}
						}
						break;
					}
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		return resRows;
	}

	private String getPrevNext(ServerResponse response) {
		try {
			NodeList nodes = (NodeList) response.getParsedResponse().getAttribute("nodeList");

			if (nodes != null) {
				NodeList tList = nodes.extractAllNodesThatMatch(new TagNameFilter("table"), true)
						.extractAllNodesThatMatch(new HasAttributeFilter("width", "200"), true)
						.extractAllNodesThatMatch(new HasAttributeFilter("border", "0"));

				if (tList.size() > 0) {
					TableTag t = (TableTag) tList.elementAt(0);
					TableRow[] rows = t.getRows();

					for (TableRow r : rows) {
						TableColumn[] cols = r.getColumns();
						for (TableColumn c : cols) {
							if (c.getChildCount() > 1 && c.getChild(1) instanceof LinkTag) {
								LinkTag l = (LinkTag) c.getChild(1);
								if (l != null) {
									String link = l.getLink().replaceAll(" ?& ?", "&").replace("&amp;", "&");
									l.setLink(CreatePartialLink(TSConnectionURL.idGET) + link);
								}
							}
						}
					}
					
					return t.toHtml();
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}

		return "";
	}

	/**
	 * Parse the intermediary response.
	 * 
	 * @param response
	 * @param rsResponse
	 * @param outputTable
	 * @return
	 */
	public Collection<ParsedResponse> smartParseIntermediary(ServerResponse response, String rsResponse, StringBuilder outputTable) {
		Collection<ParsedResponse> responses = new Vector<ParsedResponse>();

		if (rsResponse == null || response == null) {
			return responses;
		}
		searchId = getSearch().getID();
		try {
			int viparseID = (Integer) mSearch.getAdditionalInfo("viParseID");
			
			if(viparseID == ID_SEARCH_BY_NAME){
				return smartParseIntermediaryNameSearch(response, rsResponse, outputTable);
			}
			
			NodeList nodes = new HtmlParser3(rsResponse).getNodeList();
			TableTag table = (TableTag) ro.cst.tsearch.servers.functions.CASantaClaraRO
					.getNodeByTypeAttributeDescription(nodes, "table", null, new String[]{"Pages", "Document", "Date"}, true);

			response.getParsedResponse().setAttribute("nodeList", nodes);
			
			TableRow[] rows = table.getRows();
			// Parse each row.
			for (int i = 1; i < rows.length; i++) {
				ResultMap resultMap = new ResultMap();
				TableRow row = rows[i];

				if(row.toHtml().contains("Document"))
					continue;
				
				// Change external link to internal one.
				NodeList linkList = row.getChildren().extractAllNodesThatMatch(new TagNameFilter("a"), true);
				String link = "";
				if (linkList.size() > 0) {
					LinkTag linkTag = (LinkTag) linkList.elementAt(0);
					link = linkTag.getAttribute("HREF").trim().replaceAll(".*:80/(.*)", "$1");
					link = CreatePartialLink(TSConnectionURL.idGET) + link.replace("&amp;", "&");
					linkTag.setLink(link);
					linkTag.removeAttribute("onclick");
				}

				// Parse columns.
				TableColumn[] columns = row.getColumns();
				if (columns.length < 5)
					continue;
				String instrNr = columns[0].toPlainTextString().trim();
				String serverDocType = columns[3].toPlainTextString().trim();
				resultMap.put(OtherInformationSetKey.SRC_TYPE.getKeyName(), dataSite.getSiteTypeAbrev());
				
				String recDate = columns[1].toPlainTextString().trim();
				if (recDate.matches("(?is)\\d{2}/\\d{2}/\\d{4}")){
					String year = recDate.substring(recDate.lastIndexOf("/") + 1);
					if (org.apache.commons.lang.StringUtils.isNotEmpty(year)){
						instrNr = year + "-" + instrNr;
					}
				}
				
				resultMap.put(SaleDataSetKey.INSTRUMENT_NUMBER.getKeyName(), instrNr);
				resultMap.put(SaleDataSetKey.RECORDED_DATE.getKeyName(), recDate);
				resultMap.put(SaleDataSetKey.DOCUMENT_TYPE.getKeyName(), serverDocType);
				String names = columns[4].toPlainTextString().trim();
				String grantor = names.replace('\n', ' ').replaceAll("(?ism)(.*)\\(R\\)(.*)", "$1");
				grantor = ctrim(grantor);
				String grantee = names.replace('\n', ' ').replaceAll("(?ism).*\\(R\\)(.*)\\(E\\).*", "$1");
				grantee = ctrim(grantee);

				if (StringUtils.isNotEmpty(grantor)) {
					resultMap.put("SaleDataSet.Grantor", grantor);
				}
				if (StringUtils.isNotEmpty(grantee)) {
					resultMap.put("SaleDataSet.Grantee", grantee);
				}

				ro.cst.tsearch.servers.functions.CASantaClaraRO.addNames("GrantorSet", resultMap, grantor);
				ro.cst.tsearch.servers.functions.CASantaClaraRO.addNames("GranteeSet", resultMap, grantee);

				HashMap<String, String> data = new HashMap<String, String>();
				data.put("type", serverDocType);

				// Add current response.
				ParsedResponse currentResponse = new ParsedResponse();
				Bridge bridge = new Bridge(currentResponse, resultMap, searchId);
				RegisterDocumentI document = (RegisterDocumentI) bridge.importData();
				
				String checkbox = isInstrumentSaved(instrNr, document, data, false) ? "saved" : "<input type='checkbox' name='docLink' value='" + link + "'>";
				String rowAsString = row.toHtml().replaceFirst("(<tr.*>)", "$1<td align=\"left\">" + checkbox + "</td>");
				
				currentResponse.setParentSite(response.isParentSiteSearch());
				currentResponse.setOnlyResponse(rowAsString);
				LinkInPage linkInPage = new LinkInPage(link, link, TSServer.REQUEST_SAVE_TO_TSD);
				currentResponse.setPageLink(linkInPage);
				currentResponse.setAttribute(ParsedResponse.SERVER_ROW_RESPONSE, rowAsString);

				currentResponse.setDocument(document);

				responses.add(currentResponse);
			}

			// Create the table header.
			response.getParsedResponse()
					.setHeader(
							"<table border=5 width=\"95%\" cellpadding=0 align=center>"	+
									"<tr valign=\"bottom\">" +
									"<td align=\"left\" valign=\"top\" width=\"5%\" nowrap>" + SELECT_ALL_CHECKBOXES + "</td>" +
									"<td align=\"center\" width=\"5%\" nowrap><font size=\"2\"><b>Document</b><br><b>Number</b></font></td>" +
									"<td align=\"center\" width=\"10%\" nowrap><font size=\"2\"><b>Document</b><br><b>Date</b></font></td>" +
									"<td align=\"center\" nowrap width=\"6%\"><font size=\"2\"><b>Pages</b></font></td>" +
									"<td align=\"center\" width=\"30%\"><font size=\"2\"><b>Document<br>Description</br></b></font></td>" +
									"<td align=\"center\" width=\"53%\" nowrap><font size=\"2\"><b>First Grantor/Grantee</b><br><b>(R=Grantor E=Grantee)</b></br></font></td>" +
									"</tr>");
			response.getParsedResponse().setFooter("</table><br/>");
		} catch (Exception e) {
			e.printStackTrace();
		}

		return responses;
	}
	
	private Collection<ParsedResponse> smartParseIntermediaryNameSearch(ServerResponse response, String rsResponse, StringBuilder outputTable) {
		Collection<ParsedResponse> results = new Vector<ParsedResponse>();

		try {
			NodeList nodes = new HtmlParser3(rsResponse).getNodeList();

			response.getParsedResponse().setAttribute("nodeList", nodes);
			
			NodeList tList = nodes.extractAllNodesThatMatch(new TagNameFilter("table"), true).extractAllNodesThatMatch(new HasAttributeFilter("width", "600"),
					true);

			if (tList.size() > 0) {
				TableTag t = (TableTag) tList.elementAt(0);

				TableRow[] rows = t.getRows();

				for (int i = 1; i < rows.length; i++) {
					TableRow row = rows[i];

					if (row.getColumnCount() == 1) {
						ResultMap resultMap = new ResultMap();
						resultMap.put(OtherInformationSetKey.SRC_TYPE.getKeyName(), dataSite.getSiteTypeAbrev());

						// String name = row.toPlainTextString();

						NodeList linkList = row.getChildren().extractAllNodesThatMatch(new TagNameFilter("a"), true);
						String link = "";
						if (linkList.size() > 0) {
							LinkTag linkTag = (LinkTag) linkList.elementAt(0);
							link = linkTag.getAttribute("HREF").trim().replaceAll(".*:80/(.*)", "$1").replace("&amp;", "&");
							linkTag.setLink(CreatePartialLink(TSConnectionURL.idGET) + link);
						}
						
						if(row.getColumnCount() == 1){
							String grantor = row.getColumns()[0].toPlainTextString().replace('\n', ' ').replaceAll("(.*)\\(R\\)(.*)", "$1");
							grantor = ctrim(grantor);
							
							if(StringUtils.isNotEmpty(grantor)){
								resultMap.put(SaleDataSetKey.GRANTOR.getKeyName(),grantor);
								ro.cst.tsearch.servers.functions.CASantaClaraRO.addNames("GrantorSet", resultMap, grantor);
							}
						}

						String cleanRow = row.toHtml();

						ParsedResponse currentResponse = new ParsedResponse();
						currentResponse.setAttribute(ParsedResponse.SERVER_ROW_RESPONSE, cleanRow);
						currentResponse.setOnlyResponse(cleanRow);
						String linkAux = CreatePartialLink(TSConnectionURL.idGET) + link;
						currentResponse.setPageLink(new LinkInPage(linkAux, linkAux, TSServer.REQUEST_GO_TO_LINK));

						Bridge bridge = new Bridge(currentResponse, resultMap, searchId);

						DocumentI document = (RegisterDocumentI) bridge.importData();
						currentResponse.setDocument(document);

						currentResponse.setAttribute(ParsedResponse.SERVER_ROW_RESPONSE, cleanRow.replaceAll("(?ism)<a [^>]*>([^<]*)<[^>]*>", "$1"));
						currentResponse.setOnlyResponse(cleanRow);

						results.add(currentResponse);

					}
				}
				
				// Create the table header.
				response.getParsedResponse()
						.setHeader(
								"<table border=5 width=\"95%\" cellpadding=0 align=center>" +
								"<tr><td colspan=\"6\" align=\"center\" nowrap><B>Grantor and Grantee Names</B></td></tr>");
				response.getParsedResponse().setFooter("</table>");
			}

		} catch (Exception e) {
			e.printStackTrace();
		}

		return results;
	}

	protected static String ctrim(String input) {
		return input.replace("&nbsp;", " ").replace("&nbsp", " ")
				.replaceAll("\\s+", " ").trim();
	}

	@Override
	protected Object parseAndFillResultMap(ServerResponse response, String detailsHtml, ResultMap resultMap) {
		resultMap.put(OtherInformationSetKey.SRC_TYPE.getKeyName(), dataSite.getSiteTypeAbrev());
		
		return ro.cst.tsearch.servers.functions.CASantaClaraRO.parseAndFillResultsMap(response, detailsHtml, resultMap, getSearch());
	}
		
	@Override
	protected void setModulesForAutoSearch(TSServerInfo serverInfo) {

		List<TSServerInfoModule> modules = new ArrayList<TSServerInfoModule>();

		TSServerInfoModule m = null;

		FilterResponse defaultLegalFilter = LegalFilterFactory.getDefaultLegalFilter(searchId);
		((GenericLegal) defaultLegalFilter).setEnableLot(true);

		// P1 instrument list search from AO and TR for finding Legal
		{
			InstrumentGenericIterator instrumentGenericIterator = new InstrumentGenericIterator(searchId) {
		
				private static final long serialVersionUID = 5399351945130601258L;
		
				@Override
				protected String cleanInstrumentNo(String instno, int year) {
					if (StringUtils.isNotEmpty(instno)){
						if (instno.contains("-")){
							instno = instno.substring(instno.indexOf("-") + 1);
						}
						if (instno.length() <= 7) {
							return instno.replaceFirst("^0+", "");
						} else {
							return instno.substring(instno.length() - 7).replaceFirst("^0+", "");
						}
					}
					return "";
				}
			};
			instrumentGenericIterator.enableInstrumentNumber();

			m = new TSServerInfoModule(serverInfo.getModule(TSServerInfo.INSTR_NO_MODULE_IDX));
			m.addExtraInformation(TSServerInfoConstants.EXTRA_PARAM_MODULE_DESCRIPTION, "Search with references from AO/Tax like documents");
			m.setIteratorType(0, FunctionStatesIterator.ITERATOR_TYPE_INSTRUMENT_LIST_FAKE);
			m.setIteratorType(1, FunctionStatesIterator.ITERATOR_TYPE_INSTRUMENT_LIST_FAKE);
			if (isUpdate()) {
				m.addFilter(new BetweenDatesFilterResponse(searchId));
			}
			m.addIterator(instrumentGenericIterator);
			modules.add(m);
		}
		
		if(hasPin()){
			String pin = getSearchAttribute(SearchAttributes.LD_PARCELNO);
			
			m = new TSServerInfoModule(serverInfo.getModule(TSServerInfo.PARCEL_ID_MODULE_IDX));
			
			m.clearSaKey(0);
			m.setData(0, pin.replaceAll("-", ""));
			modules.add(m);
		}
		
		
		// P2 search with lot/block/subdivision from RO documents

		// P3 search with sec/twp/rng from RO documents

		// P4 search by sub name

		FilterResponse nameFilterDoNotSkipUnique = NameFilterFactory.getDefaultNameFilter(SearchAttributes.OWNER_OBJECT, searchId, m);
		nameFilterDoNotSkipUnique.setSkipUnique(false);
		
		// P5 name modules with names from search page.
		if(hasOwner()){
			m = new TSServerInfoModule(serverInfo.getModule(TSServerInfo.NAME_MODULE_IDX));
			m.clearSaKeys();
			
			m.setSaObjKey(SearchAttributes.OWNER_OBJECT);
			m.setIteratorType(0, FunctionStatesIterator.ITERATOR_TYPE_LF_NAME_FAKE);
			m.setData(1, "R");
			m.addFilter(nameFilterDoNotSkipUnique);
			if(hasPin()){
				FilterResponse pinFilter = PINFilterFactory.getDefaultPinFilter(searchId);
				m.addValidator(pinFilter.getValidator());
			}
			m.addIterator((ConfigurableNameIterator) ModuleStatesIteratorFactory.getConfigurableNameIterator(m, searchId, new String[] { "L;F;" }));
			modules.add(m);
			
		}

		// P5 search by buyers
		if(hasBuyer()){
			m = new TSServerInfoModule(serverInfo.getModule(TSServerInfo.NAME_MODULE_IDX));
			m.clearSaKeys();
			
			m.setSaObjKey(SearchAttributes.OWNER_OBJECT);
			m.setIteratorType(0, FunctionStatesIterator.ITERATOR_TYPE_LF_NAME_FAKE);
			m.setData(1, "E");
			m.addFilter(nameFilterDoNotSkipUnique);
			if(hasPin()){
				FilterResponse pinFilter = PINFilterFactory.getDefaultPinFilter(searchId);
				m.addValidator(pinFilter.getValidator());
			}
			m.addIterator((ConfigurableNameIterator) ModuleStatesIteratorFactory.getConfigurableNameIterator(m, searchId, new String[] { "L;F;" }));
			modules.add(m);
		}

		// P6 OCR last transfer - book page search
		m = new TSServerInfoModule(serverInfo.getModule(TSServerInfo.INSTR_NO_MODULE_IDX));
		m.clearSaKeys();
		m.setIteratorType(ModuleStatesIterator.TYPE_OCR_FULL_OR_BOOTSTRAPER);
		m.getFunction(0).setIteratorType(FunctionStatesIterator.ITERATOR_TYPE_INSTRUMENT_LIST_SEARCH);
		m.addFilter(new GenericInstrumentFilter(searchId));
		m.addFilter(defaultLegalFilter);
		modules.add(m);

		// P7 name module with names added by OCR

		serverInfo.setModulesForAutoSearch(modules);
	}
	
	protected void setModulesForGoBackOneLevelSearch(TSServerInfo serverInfo) {
		
	}
	
	@Override
	protected void setCertificationDate() {
		try {
			if (CertificationDateManager.isCertificationDateInCache(dataSite)){
				String date = CertificationDateManager.getCertificationDateFromCache(dataSite);
				getSearch().getSa().updateCertificationDateObject(dataSite, Util.dateParser3(date));
			} else{
				String page = getLinkContents(dataSite.getLink() + (dataSite.getLink().endsWith("/") ? "" : "/") + "cgi-bin/odsmnu1.html/input");
					
				if (StringUtils.isNotEmpty(page)){
					Matcher certDateMatcher = certDatePattern.matcher(page);
					
					if(certDateMatcher.find()) {
						String date = certDateMatcher.group(1).trim();
						date = DateFormatUtils.format(Util.dateParser3(date), "MM/dd/yyyy");
						
						CertificationDateManager.cacheCertificationDate(dataSite, date);
						getSearch().getSa().updateCertificationDateObject(dataSite, Util.dateParser3(date));
					} else {
						CertificationDateManager.getLogger().error("Cannot parse certification date on " + getDataSite().getName() + " because pattern not found");
					}
				} else {
					CertificationDateManager.getLogger().error("Cannot parse certification date on " + getDataSite().getName() + " because html response is empty");
				}
			}
        } catch (Exception e) {
        	CertificationDateManager.getLogger().error("Error setting certification date on " + getDataSite().getName(), e);
        }
	}
}
