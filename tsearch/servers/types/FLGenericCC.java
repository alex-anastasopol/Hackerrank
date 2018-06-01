package ro.cst.tsearch.servers.types;

import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Vector;

import org.htmlparser.Node;
import org.htmlparser.filters.HasAttributeFilter;
import org.htmlparser.filters.TagNameFilter;
import org.htmlparser.tags.InputTag;
import org.htmlparser.tags.LinkTag;
import org.htmlparser.tags.TableRow;
import org.htmlparser.tags.TableTag;
import org.htmlparser.util.NodeList;

import ro.cst.tsearch.Search;
import ro.cst.tsearch.bean.SearchAttributes;
import ro.cst.tsearch.connection.http2.HttpManager;
import ro.cst.tsearch.connection.http2.HttpSite;
import ro.cst.tsearch.exceptions.ServerResponseException;
import ro.cst.tsearch.extractor.xml.Bridge;
import ro.cst.tsearch.extractor.xml.GenericFunctions2;
import ro.cst.tsearch.extractor.xml.ResultMap;
import ro.cst.tsearch.extractor.xml.ResultTable;
import ro.cst.tsearch.parser.HtmlParser3;
import ro.cst.tsearch.parser.SimpleHtmlParser;
import ro.cst.tsearch.parser.SimpleHtmlParser.Form;
import ro.cst.tsearch.search.FunctionStatesIterator;
import ro.cst.tsearch.search.filter.FilterResponse;
import ro.cst.tsearch.search.filter.newfilters.name.NameFilterFactory;
import ro.cst.tsearch.search.iterator.ModuleStatesIteratorFactory;
import ro.cst.tsearch.search.module.ConfigurableNameIterator;
import ro.cst.tsearch.servers.TSConnectionURL;
import ro.cst.tsearch.servers.info.TSServerInfo;
import ro.cst.tsearch.servers.info.TSServerInfoModule;
import ro.cst.tsearch.servers.response.ImageLinkInPage;
import ro.cst.tsearch.servers.response.LinkInPage;
import ro.cst.tsearch.servers.response.CrossRefSet.CrossRefSetKey;
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

/**
 * 
 * @author Oprina George
 * 
 *         Oct 25, 2011
 */

@SuppressWarnings("deprecation")
public class FLGenericCC extends TSServerROLike {

	/**
	 * 
	 */
	private static final long	serialVersionUID	= 7289351436250918975L;

	private static int			seq					= 0;
	private int					my_seq				= 0;

	public FLGenericCC(long searchId) {
		super(searchId);
	}

	public FLGenericCC(String rsRequestSolverName, String rsSitePath, String rsServerID, String rsPrmNameLink, long searchId,
			int miServerID) {
		super(rsRequestSolverName, rsSitePath, rsServerID, rsPrmNameLink, searchId, miServerID);
	}

	protected synchronized static int getSeq() {
		return seq++;
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

		String typeOfSearch = (String) mSearch.getAdditionalInfo(getDataSite().getName() + ":typeOfSearch");
		if (viParseID != ID_SAVE_TO_TSD && "6".equals(typeOfSearch)) {
			viParseID = ID_DETAILS;
			mSearch.removeAdditionalInfo(getDataSite().getName() + ":typeOfSearch");
		}

		switch (viParseID) {
		case ID_SEARCH_BY_NAME:
			if (rsResponse.indexOf("Error") > -1) {
				Response.getParsedResponse().setError("No Results Found!");
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

			if (viParseID != ID_SAVE_TO_TSD) {
				String originalLink = sAction.replace("?", "&") + "&" + Response.getRawQuerry();
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
			ParseResponse(sAction, Response, rsResponse.contains("Search Results Detail") ? ID_DETAILS : ID_SEARCH_BY_NAME);
			break;
		default:
			break;
		}
	}

	private String makeHeader(int viParseID) {
		String header = "";

		header += "<table align=\"center\" cellspacing=\"0\" cellpadding=\"3\" border=\"1\" width=\"95%\">"
				+ "<TR bgcolor=\"#6699CC\" valign=\"top\">"
				+ "<TH ALIGN=Left>"
				+ SELECT_ALL_CHECKBOXES
				+ "</TH>"
				+ "<th>Name</th>"
				+ "<th>UCC Number</th>"
				+ "<th>Address</th>"
				+ "<th>City</th>"
				+ "<th>State</th>"
				+ "<th>Zip Code</th>"
				+ "<th>Status</th>"
				+ "</tr>";
		return header;
	}

	private String getPrevNext(ServerResponse resp) {
		String response = Tidy.tidyParse(resp.getResult(), null);

		String prev_next = "<table align='center' width = '95%'><tr>";

		try {
			NodeList nodeList = new HtmlParser3(response).getNodeList();

			NodeList nextList = nodeList.extractAllNodesThatMatch(new TagNameFilter("input"), true).extractAllNodesThatMatch(
					new HasAttributeFilter("id", "ButtonNext"), true);
			NodeList prevList = nodeList.extractAllNodesThatMatch(new TagNameFilter("input"), true).extractAllNodesThatMatch(
					new HasAttributeFilter("id", "ButtonPrevious"), true);

			String siteLinkPrefix = ro.cst.tsearch.connection.http2.FLGenericCC.ORIGINAL_LINK;
			String link_prefix = CreatePartialLink(TSConnectionURL.idPOST);

			if (prevList.size() > 0) {
				InputTag prev = (InputTag) prevList.elementAt(0);

				String prev_link = URLDecoder.decode(prev.getAttribute("onclick")).replace("&amp;", "&").replace("&quot;", "'");

				if (StringUtils.isNotEmpty(prev_link) && prev_link.indexOf("SearchResults") != -1) {
					prev_link = prev_link.substring(prev_link.indexOf("SearchResults")).split("'")[0] + "&seq=" + this.my_seq + "&Prev=";

					prev_link = link_prefix + siteLinkPrefix + prev_link;

					prev_next += "<td align='left'>" + "<a href='" + prev_link + "'>" + " Prev " + "</a></td>";
				}
			}

			if (nextList.size() > 0) {
				InputTag next = (InputTag) nextList.elementAt(0);

				String next_link = URLDecoder.decode(next.getAttribute("onclick")).replace("&amp;", "&").replace("&quot;", "'");

				if (StringUtils.isNotEmpty(next_link) && next_link.indexOf("SearchResults") != -1) {
					next_link = next_link.substring(next_link.indexOf("SearchResults")).split("'")[0] + "&seq=" + this.my_seq + "&Next=";

					next_link = link_prefix + siteLinkPrefix + next_link;

					prev_next += "<td align='right'>" + "<a href='" + next_link + "'>" + " Next " + "</a></td>";
				}
			}

		} catch (Exception e) {
			e.printStackTrace();
		}

		prev_next += "</tr></table><br>";

		return prev_next;
	}

	protected String getDetails(String rsResponse, StringBuilder accountId, HashMap<String, String> data, ServerResponse Response) {
		String parseR = rsResponse;

		if (org.apache.commons.lang.StringUtils.containsIgnoreCase(rsResponse, "<html")) {
			parseR = Tidy.tidyParse(rsResponse, null);
		}

		try {
			NodeList nodes = new HtmlParser3(parseR).getNodeList();

			nodes = nodes.extractAllNodesThatMatch(new TagNameFilter("table"), true).extractAllNodesThatMatch(
					new HasAttributeFilter("id", "SearchResultDetailMainTable"));

			String docNo = HtmlParser3.findNode(nodes, "Detail Record For:").toPlainTextString().replace("Detail Record For: ", "");

			NodeList sumTables = nodes.extractAllNodesThatMatch(new HasAttributeFilter("id", "StatusGridView"), true);
			if (sumTables.size() > 0) {
				TableTag docTy = (TableTag) sumTables.elementAt(0);
				if (docTy.getRowCount() == 2 && docTy.getRow(1).getColumnCount() == 5) {
					docNo = docTy.getRow(1).getColumns()[4].toPlainTextString();
				}
			}
			
			accountId.append(docNo);
			
			NodeList docTyTables = nodes.extractAllNodesThatMatch(new HasAttributeFilter("id", "DocumentImagesGridView"), true);
			
			String type = "UCC";
			if (docTyTables.size() > 0) {
				TableTag docTy = (TableTag) docTyTables.elementAt(0);
				if (docTy.getRowCount() == 2 && docTy.getRow(1).getColumnCount() == 4) {
					type = docTy.getRow(1).getColumns()[1].toPlainTextString();
				}
			}
			
			data.put("type", type);

			/* If from memory - use it as is */
			if (!org.apache.commons.lang.StringUtils.containsIgnoreCase(rsResponse, "<html")) {
				return rsResponse;
			}

			if (nodes.size() > 0) {
				TableTag t = (TableTag) nodes.elementAt(0);

				String link = "";

				if (nodes.extractAllNodesThatMatch(new TagNameFilter("a"), true)
						.extractAllNodesThatMatch(new HasAttributeFilter("id", "EventsLink2"), true).size() > 0) {
					LinkTag l = (LinkTag) nodes.extractAllNodesThatMatch(new TagNameFilter("a"), true)
							.extractAllNodesThatMatch(new HasAttributeFilter("id", "EventsLink2"), true).elementAt(0);
					link = URLDecoder.decode(l.getLink()).replace("&amp;", "&");
					l.setAttribute("type", "hidden");
				}

				// modify link for image
				NodeList links = nodes.extractAllNodesThatMatch(new TagNameFilter("a"), true);

				String linkPrefix = CreatePartialLink(TSConnectionURL.idGET);
				String originalLink = ro.cst.tsearch.connection.http2.FLGenericCC.ORIGINAL_LINK;

				for (int i = 0; i < links.size(); i++) {
					Node img_link = links.elementAt(i);
					if (img_link instanceof LinkTag && ((LinkTag) img_link).getLink().contains("RetrieveImage.aspx")) {
						String link1 = ((LinkTag) img_link).getLink();
						Response.getParsedResponse().addImageLink(
								new ImageLinkInPage(originalLink + URLDecoder.decode(link1).replace("&amp;", "&"),
										accountId.toString() + ".tif"));
						((LinkTag) img_link).setLink(linkPrefix + originalLink
								+ URLDecoder.decode(((LinkTag) img_link).getLink()).replace("&amp;", "&"));
						break;
					}
				}

				String historyTable = getHistory(link, Response);

				String details = "<table align='center'><tr><td>" + t.toHtml();

				// add his table
				details += historyTable + "</td></tr></table>";

				// clean details
				details = details.replace("View Filing History", "");
				details = details.replaceAll("(?ism)<input[^>]*>", "");

				return details;
			}
		} catch (Exception e) {
			e.printStackTrace();
		}

		return rsResponse;
	}

	private String getHistory(String link, ServerResponse response) {
		String linkPrefix = CreatePartialLink(TSConnectionURL.idGET);
		String originalLink = ro.cst.tsearch.connection.http2.FLGenericCC.ORIGINAL_LINK;

		try {
			String history = getLinkContents(originalLink + link);
			history = Tidy.tidyParse(history, null);

			String result = "";

			NodeList nodes = new HtmlParser3(history).getNodeList();

			nodes = nodes.extractAllNodesThatMatch(new TagNameFilter("td"), true).extractAllNodesThatMatch(
					new HasAttributeFilter("align", "center"));

			if (nodes.size() == 1) {
				// make links for images
				nodes = nodes.elementAt(0).getChildren().extractAllNodesThatMatch(new TagNameFilter("table"));

				NodeList links = nodes.extractAllNodesThatMatch(new TagNameFilter("a"), true);

				for (int i = 0; i < links.size(); i++) {
					Node img_link = links.elementAt(i);
					if (img_link instanceof LinkTag && ((LinkTag) img_link).getLink().contains("RetrieveImage.aspx")) {
						((LinkTag) img_link).setLink(linkPrefix + originalLink + ((LinkTag) img_link).getLink());
					}
				}

				result = nodes.toHtml();

				result = result.replaceAll("(?ism)<input [^>]*>", "");
				result = result.replaceAll("(?ism)(<table) ([^>]*>)", "$1 align=\"center\" $2");
				result = result.replaceAll("(?ism)(<table[^>]*)(border=[^>]*>)", "$1 id=\"References\" $2");
				result = result.replace("width=\"100%\"", "style=\"width:700px;\"");
				return result;
			}
		} catch (Exception e) {
			e.printStackTrace();
		}

		return "";
	}

	@Override
	public Collection<ParsedResponse> smartParseIntermediary(ServerResponse response, String table, StringBuilder outputTable) {
		int numberOfUncheckedElements = 0;
		Vector<ParsedResponse> intermediaryResponse = new Vector<ParsedResponse>();
		try {
			table = Tidy.tidyParse(table, null);

			// save params for details
			this.my_seq = getSeq();

			Form form = new SimpleHtmlParser(table).getForm("form1");
			if (form != null) {
				Map<String, String> params = form.getParams();

				if (params != null) {
					mSearch.setAdditionalInfo(getCurrentServerName() + ":params:" + this.my_seq, params);
				}
			}

			NodeList nodeList = org.htmlparser.Parser.createParser(table, null).parse(null);
			NodeList mainTableList = nodeList.extractAllNodesThatMatch(new TagNameFilter("table"), true).extractAllNodesThatMatch(
					new HasAttributeFilter("id", "SearchResultsGridView"), true);

			if (!(mainTableList.size() > 0)) {
				return intermediaryResponse;
			}

			TableTag t = (TableTag) mainTableList.elementAt(0);

			TableRow[] rows = t.getRows();

			String siteLinkPrefix = ro.cst.tsearch.connection.http2.FLGenericCC.ORIGINAL_LINK;

			for (int i = 1; i < rows.length; i++) {
				TableRow row = rows[i];
				if (row.getColumnCount() == 8) {

					ResultMap m = ro.cst.tsearch.servers.functions.FLGenericCC.parseIntermediaryRow(row,
							(Integer) mSearch.getAdditionalInfo("viParseID"));

					// index link
					String link_prefix = CreatePartialLink(TSConnectionURL.idGET);
					String doc_link = "";

					if (row.getColumns()[0].getChildren().size() > 0) {
						LinkTag linkTag = (LinkTag) row.getColumns()[0].getChild(0);

						String link = URLDecoder.decode(linkTag.getLink()).replace("&amp;", "&");

						doc_link = siteLinkPrefix + link;

						linkTag.setLink(link_prefix + doc_link);
					}

					String clean_row = row.toHtml();

					clean_row = clean_row.replaceAll("(?ism)<td class=\"hiddencol\">[^<]*</td>", "");

					ParsedResponse currentResponse = new ParsedResponse();
					currentResponse.setAttribute(ParsedResponse.SERVER_ROW_RESPONSE, clean_row);
					currentResponse.setOnlyResponse(clean_row);
					currentResponse.setPageLink(new LinkInPage(doc_link, doc_link, TSServer.REQUEST_SAVE_TO_TSD));

					Bridge bridge = new Bridge(currentResponse, m, searchId);

					DocumentI document = (RegisterDocumentI) bridge.importData();
					currentResponse.setDocument(document);

					String checkBox = "checked";

					String instrNo = org.apache.commons.lang.StringUtils
							.defaultString((String) m.get(SaleDataSetKey.INSTRUMENT_NUMBER.getKeyName()));

					HashMap<String, String> data = new HashMap<String, String>();

					data.put("type",
							org.apache.commons.lang.StringUtils.defaultString((String) m.get(SaleDataSetKey.DOCUMENT_TYPE.getKeyName())));

					if (isInstrumentSaved(instrNo, document, data, true)
							&& !Boolean.TRUE.equals(getSearch().getAdditionalInfo("RESAVE_DOCUMENT"))) {
						checkBox = "saved";
					} else {
						numberOfUncheckedElements++;
						LinkInPage linkInPage = new LinkInPage(link_prefix + doc_link, link_prefix + doc_link, TSServer.REQUEST_SAVE_TO_TSD);
						checkBox = "<input type='checkbox' name='docLink' value='" + link_prefix + doc_link + "'>";
						if (getSearch().getInMemoryDoc(doc_link) == null) {
							getSearch().addInMemoryDoc(doc_link, currentResponse);
						}
						currentResponse.setPageLink(linkInPage);
					}
					String rowHtml = "<tr><td>"
							+ checkBox
							+ "</td>"
							+ clean_row.replaceAll("(?ism)<tr[^>]*>", "").replaceAll("(?ism)</tr>", "") + "</tr>";

					currentResponse.setAttribute(ParsedResponse.SERVER_ROW_RESPONSE,
							rowHtml.replaceAll("(?ism)<a [^>]*>([^<]*)<[^>]*>", "$1"));
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

	@Override
	protected Object parseAndFillResultMap(ServerResponse response, String detailsHtml, ResultMap map) {
		try {
			map.put(OtherInformationSetKey.SRC_TYPE.getKeyName(), "CC");
			// map.put(SaleDataSetKey.DOCUMENT_TYPE.getKeyName(), "UCC");

			NodeList nodes = new HtmlParser3(detailsHtml).getNodeList();

			String instrNo = HtmlParser3.findNode(nodes, "Detail Record For:").toPlainTextString().replace("Detail Record For: ", "");
			map.put(SaleDataSetKey.INSTRUMENT_NUMBER.getKeyName(), org.apache.commons.lang.StringUtils.defaultString(instrNo));

			nodes = nodes.extractAllNodesThatMatch(new TagNameFilter("table"));

			NodeList docTyTables = nodes.extractAllNodesThatMatch(new TagNameFilter("table"), true).extractAllNodesThatMatch(
					new HasAttributeFilter("id", "DocumentImagesGridView"), true);
			if (docTyTables.size() > 0) {
				TableTag docTy = (TableTag) docTyTables.elementAt(0);
				if (docTy.getRowCount() == 2 && docTy.getRow(1).getColumnCount() == 4) {
					String type = docTy.getRow(1).getColumns()[1].toPlainTextString();
					map.put(SaleDataSetKey.DOCUMENT_TYPE.getKeyName(), org.apache.commons.lang.StringUtils.defaultString(type));
				}
			}

			NodeList n = new NodeList();

			if ((n = nodes.extractAllNodesThatMatch(new HasAttributeFilter("id", "StatusGridView"), true)).size() > 0) {
				TableTag dates = (TableTag) n.elementAt(0);

				TableRow[] rows = dates.getRows();

				if (rows.length == 2 && rows[1].getColumnCount() == 5) {
					String dateFilled = rows[1].getColumns()[1].toPlainTextString();
					String recordedDate = rows[1].getColumns()[3].toPlainTextString();

					map.put(SaleDataSetKey.INSTRUMENT_DATE.getKeyName(), org.apache.commons.lang.StringUtils.defaultString(dateFilled));
					map.put(SaleDataSetKey.RECORDED_DATE.getKeyName(), org.apache.commons.lang.StringUtils.defaultString(recordedDate));
				}
			}

			if ((n = nodes.extractAllNodesThatMatch(new TagNameFilter("tr"), true)
					.extractAllNodesThatMatch(new HasAttributeFilter("id", "SecPartyData"), true)).size() > 0) {

				TableRow row = (TableRow) n.elementAt(0);

				if (row.getColumnCount() > 0) {
					String grantees_string = row.getColumns()[0].getChildrenHTML();
					ArrayList<String> granteesList = new ArrayList<String>();

					String grantees[] = grantees_string.split("(?ism)<hr[^>]*>");

					for (String grantee : grantees) {
						granteesList.add(grantee.split("(?ism)<br[^>]*>")[0]);
					}

					ro.cst.tsearch.servers.functions.FLGenericCC.parseNames(map, granteesList, "GranteeSet");
				}

			}

			if ((n = nodes.extractAllNodesThatMatch(new TagNameFilter("tr"), true)
					.extractAllNodesThatMatch(new HasAttributeFilter("id", "DebPartyData"), true)).size() > 0) {

				TableRow row = (TableRow) n.elementAt(0);

				if (row.getColumnCount() > 0) {
					String grantees_string = row.getColumns()[0].getChildrenHTML();
					ArrayList<String> granteesList = new ArrayList<String>();

					String grantees[] = grantees_string.split("(?ism)<hr[^>]*>");

					for (String grantee : grantees) {
						granteesList.add(grantee.split("(?ism)<br[^>]*>")[0]);
					}

					ro.cst.tsearch.servers.functions.FLGenericCC.parseNames(map, granteesList, "GrantorSet");
				}

			}

			if ((n = nodes.extractAllNodesThatMatch(new HasAttributeFilter("id", "References"), true)).size() > 0) {
				TableTag t = (TableTag) n.elementAt(0);
				TableRow[] rows = t.getRows();

				String[] refHeader = { CrossRefSetKey.INSTRUMENT_NUMBER.getShortKeyName() };
				List<List> refBody = new ArrayList<List>();
				List<String> refRow = new ArrayList<String>();
				ResultTable resT = new ResultTable();

				for (int i = 0; i < rows.length; i++) {
					TableRow r = rows[i];
					if (r.getColumnCount() == 5 && !r.getColumns()[0].toPlainTextString().contains("DOCUMENT NUMBER")) {
						refRow = new ArrayList<String>();
						refRow.add(org.apache.commons.lang.StringUtils.defaultString(r.getColumns()[0].toPlainTextString().trim()));
						// refRow.add(org.apache.commons.lang.StringUtils.defaultString(r.getColumns()[1].toPlainTextString().trim()));
						// refRow.add(org.apache.commons.lang.StringUtils.defaultString(r.getColumns()[2].toPlainTextString().trim()));

						refBody.add(refRow);
					}
				}

				if (refBody.size() > 0) {
					resT = GenericFunctions2.createResultTableFromList(refHeader, refBody);
					map.put("CrossRefSet", resT);
				}
			}

		} catch (Exception e) {
			e.printStackTrace();
		}
		return null;
	}

	@Override
	protected void setModulesForAutoSearch(TSServerInfo serverInfo) {
		List<TSServerInfoModule> modules = new ArrayList<TSServerInfoModule>();
		TSServerInfoModule module = null;

		if (hasOwner()) {
			FilterResponse defaultNameFilter = NameFilterFactory.getDefaultNameFilterNoSinonims(SearchAttributes.OWNER_OBJECT, searchId, module);
			defaultNameFilter.setSkipUnique(false);

			module = new TSServerInfoModule(serverInfo.getModule(TSServerInfo.NAME_MODULE_IDX));
			module.clearSaKeys();

			module.setSaObjKey(SearchAttributes.OWNER_OBJECT);
			module.setIteratorType(0, FunctionStatesIterator.ITERATOR_TYPE_LFM_NAME_FAKE);
			module.forceValue(1, "0");
			module.addFilter(defaultNameFilter);

			module.addIterator((ConfigurableNameIterator) ModuleStatesIteratorFactory.getConfigurableNameIterator(module, searchId, new String[] { "L;F;M",
					"L;F;" }));

			modules.add(module);
		}

		serverInfo.setModulesForAutoSearch(modules);
	}

	@Override
	public Object getRecoverModuleFrom(RestoreDocumentDataI document) {
		// TODO Auto-generated method stub
		return null;
	}

}
