package ro.cst.tsearch.servers.types;

import static ro.cst.tsearch.servers.info.spitHTML.HTMLControl.HTML_TEXT_FIELD;

import java.io.File;
import java.io.InputStream;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.Vector;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.htmlparser.filters.HasAttributeFilter;
import org.htmlparser.filters.TagNameFilter;
import org.htmlparser.tags.InputTag;
import org.htmlparser.tags.LinkTag;
import org.htmlparser.tags.TableColumn;
import org.htmlparser.tags.TableRow;
import org.htmlparser.tags.TableTag;
import org.htmlparser.util.NodeList;

import ro.cst.tsearch.Search;
import ro.cst.tsearch.ServerConfig;
import ro.cst.tsearch.connection.http.HTTPRequest;
import ro.cst.tsearch.connection.http.HTTPResponse;
import ro.cst.tsearch.connection.http2.HttpManager;
import ro.cst.tsearch.connection.http2.HttpSite;
import ro.cst.tsearch.exceptions.ServerResponseException;
import ro.cst.tsearch.extractor.xml.Bridge;
import ro.cst.tsearch.extractor.xml.ResultMap;
import ro.cst.tsearch.extractor.xml.StringFormats;
import ro.cst.tsearch.parser.HtmlParser3;
import ro.cst.tsearch.parser.SimpleHtmlParser;
import ro.cst.tsearch.parser.SimpleHtmlParser.Form;
import ro.cst.tsearch.search.filter.newfilters.legal.GenericInstrumentFilter;
import ro.cst.tsearch.search.name.NameUtils;
import ro.cst.tsearch.servers.TSConnectionURL;
import ro.cst.tsearch.servers.info.TSServerInfo;
import ro.cst.tsearch.servers.info.TSServerInfoConstants;
import ro.cst.tsearch.servers.info.TSServerInfoFunction;
import ro.cst.tsearch.servers.info.TSServerInfoModule;
import ro.cst.tsearch.servers.info.spitHTML.FormatException;
import ro.cst.tsearch.servers.info.spitHTML.HTMLControl;
import ro.cst.tsearch.servers.response.ImageLinkInPage;
import ro.cst.tsearch.servers.response.LinkInPage;
import ro.cst.tsearch.servers.response.ParsedResponse;
import ro.cst.tsearch.servers.response.SaleDataSet.SaleDataSetKey;
import ro.cst.tsearch.servers.response.ServerResponse;
import ro.cst.tsearch.servlet.URLConnectionReader;
import ro.cst.tsearch.utils.FileUtils;
import ro.cst.tsearch.utils.InstanceManager;
import ro.cst.tsearch.utils.StringUtils;

import com.stewart.ats.base.document.DocumentI;
import com.stewart.ats.base.document.ImageI;
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

/**
 * 
 * @author Oprina George
 * 
 *         Jan 13, 2012
 */

@SuppressWarnings("deprecation")
public class KSSedgwickRO extends TSServerROLike {

	/**
	 * 
	 */
	private static final long	serialVersionUID	= 1256324517106517391L;

	public KSSedgwickRO(long searchId) {
		super(searchId);
	}

	public KSSedgwickRO(String rsRequestSolverName, String rsSitePath, String rsServerID, String rsPrmNameLink, long searchId, int miServerID) {
		super(rsRequestSolverName, rsSitePath, rsServerID, rsPrmNameLink, searchId, miServerID);
	}

	private static int		seq			= 0;

	protected synchronized static int getSeq() {
		return seq++;
	}

	public static class Credentials {

		public String	imageId		= "";
		public String	fileName	= "";
		public String	viewState	= "";

		public Credentials(String imageId, String fileName, String viewState) {
			this.imageId = imageId;
			this.fileName = fileName;
			this.viewState = viewState;
		}

		public String toString() {
			return "Credentials(imageId=" + imageId + ",fileName=" + fileName + ",imageCode=" + viewState + ")";
		}

	}

	private Credentials getImageCredentials() {
		Credentials cr = (Credentials) getSearch().getAdditionalInfo(getCurrentServerName() + ":credentials");
		for (int i = 0; i < 5 && cr == null; i++) {
			cr = createImageCredentials();
		}
		if (cr != null) {
			getSearch().setAdditionalInfo(getCurrentServerName() + ":credentials", cr);
		}
		return cr;
	}

	private Credentials createImageCredentials() {

		HttpSite httpSite = HttpManager.getSite(getCurrentServerName(), searchId);
		try {
			String viewState = ((ro.cst.tsearch.connection.http2.KSSedgwickRO) httpSite).getViewState();

			if (StringUtils.isEmpty(viewState)) {
				return null;
			}

			// get captcha
			HTTPResponse httpResponse = httpSite.process(new HTTPRequest("https://rod.sedgwickcounty.org/capimage.aspx"));
			if (!httpResponse.getContentType().contains("image/")) {
				logger.error("Did not obtain \"image/\");");
				return null;
			}

			String fn = Long.toString(new Random().nextLong()).replace("-", "");

			String folderName = getCrtSearchDir() + "temp";
			new File(folderName).mkdirs();
			String fileName = folderName + File.separator + fn + ".jpg";

			InputStream inputStream = httpResponse.getResponseAsStream();
			FileUtils.writeStreamToFile(inputStream, fileName);

			if (!FileUtils.existPath(fileName)) {
				logger.error("Image was not downloaded!");
				return null;
			}

			getSearch().setAdditionalInfo(getCurrentServerName() + ":viewState", viewState);

			return new Credentials(fn, fileName, viewState);

		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			HttpManager.releaseSite(httpSite);
		}
		return null;
	}

	@Override
	public TSServerInfo getDefaultServerInfo() {

		TSServerInfo msiServerInfoDefault = super.getDefaultServerInfo();
		TSServerInfoModule tsServerInfoModule = msiServerInfoDefault.getModule(TSServerInfo.GENERIC_MODULE_IDX);

		if (tsServerInfoModule != null && tsServerInfoModule.getFunctionList() != null && tsServerInfoModule.getFunctionList().size() > 26) {
			// get credentials
			Credentials credentials = getImageCredentials();

			if (credentials == null) {
				return msiServerInfoDefault;
			}

			// set captcha
			TSServerInfoFunction func = tsServerInfoModule.getFunction(26);
			try {
				new HTMLControl(HTML_TEXT_FIELD, 1, 1, 33, 33, 26, func, "image", "Image", null, searchId);
			} catch (FormatException e) {
				e.printStackTrace();
			}
			func.setHtmlformat("<img border='1' src='/title-search/fs?f=" + credentials.fileName + "&searchId=" + searchId + "'/>");
		}

		setModulesForAutoSearch(msiServerInfoDefault);
		return msiServerInfoDefault;

	}

	@Override
	public ServerResponse SearchBy(TSServerInfoModule module, Object sd) throws ServerResponseException {
		Credentials cr = (Credentials) getSearch().getAdditionalInfo(getCurrentServerName() + ":credentials");
		if (cr == null && getSearch().getSearchType() == Search.PARENT_SITE_SEARCH) {
			return ServerResponse.createErrorResponse("Authentication Image Missing");
		}

		try {
			// remove credentials
			if (cr != null) {
				getSearch().removeAdditionalInfo(getCurrentServerName() + ":credentials");
			}
			// perform search
			return super.SearchBy(module, sd);
		} finally {
			if (cr != null)
			{// delete credentials file
				new File(ServerConfig.getFilePath() + cr.fileName).delete();
			}
		}
	}

	@Override
	protected void ParseResponse(String sAction, ServerResponse Response, int viParseID) throws ServerResponseException {
		String rsResponse = Response.getResult();
		ParsedResponse parsedResponse = Response.getParsedResponse();

		if (rsResponse.contains("Your search criteria has located over 1,500 records. Please refine your search area.")) {
			Response.getParsedResponse().setError("Your search criteria has located over 1,500 records. Please refine your search area!");
			return;
		}

		if (rsResponse.contains("Login Error!")) {
			Response.getParsedResponse().setError("Login Error! Go back to parent site and reinsert the code from the image.");
			return;
		}

		if (rsResponse.contains("Error geting image!")) {
			Response.getParsedResponse().setError("Error geting image!");
			return;
		}

		if (rsResponse.contains("No results matched your criteria, please try again.") ||
				rsResponse.contains("error") || rsResponse.contains("Error")) {
			Response.getParsedResponse().setError("No results found for your query! Please change your search criteria and try again.");
			return;
		}

		switch (viParseID) {
		case ID_SEARCH_BY_NAME:

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
					String originalLink = sAction.replace("?", "&");
					String sSave2TSDLink = getLinkPrefix(TSConnectionURL.idGET) + originalLink;
					if (isInstrumentSaved(accountName, null, data)) {
						details += CreateFileAlreadyInTSD();
					} else {
						mSearch.addInMemoryDoc(sSave2TSDLink, details);
						details = addSaveToTsdButton(details, sSave2TSDLink, ID_DETAILS);
					}
					Response.getParsedResponse().setPageLink(new LinkInPage(sSave2TSDLink, originalLink, TSServer.REQUEST_SAVE_TO_TSD));
					Response.getParsedResponse().setResponse(details);
				} catch (Exception e) {
					e.printStackTrace();
				}
			else {
				if (!isInstrumentSaved(accountName, null, data)) {
					smartParseDetails(Response, details);

					msSaveToTSDFileName = filename;
					Response.getParsedResponse().setFileName(getServerTypeDirectory() + msSaveToTSDFileName);
					Response.getParsedResponse().setResponse(details.replaceAll("(?ism)<a href[^>]*>([^<]*)</a>", "$1"));

					msSaveToTSDResponce = details.replaceAll("(?ism)<a href[^>]*>([^<]*)</a>", "$1") + CreateFileAlreadyInTSD();

					String resultForCross = details;

					Pattern crossRefLinkPattern = Pattern.compile("(?ism)<a href=[\\\"|'](.*?)[\\\"|']>");
					Matcher crossRefLinkMatcher = crossRefLinkPattern.matcher(resultForCross);
					while (crossRefLinkMatcher.find()) {
						ParsedResponse prChild = new ParsedResponse();
						String link = crossRefLinkMatcher.group(1);
						if (link.contains("details.aspx")) {
							LinkInPage pl = new LinkInPage(link, link, TSServer.REQUEST_SAVE_TO_TSD);
							prChild.setPageLink(pl);
							Response.getParsedResponse().addOneResultRowOnly(prChild);
						}
					}
				}
			}
			break;
		case ID_GET_LINK:
			ParseResponse(sAction, Response, rsResponse.contains("RMIS Detail Results") ? ID_DETAILS : ID_SEARCH_BY_NAME);
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

		header += "<TH ALIGN=Left>DOC.#/FLM-PG:</TH>"
				+ "<TH ALIGN=Left>Recorded Date</TH>"
				+ "<TH ALIGN=Left>Instrument Type</TH>"
				+ "<TH ALIGN=Left>Party</TH>"
				+ "<TH ALIGN=Left>FILM/PAGE:</TH>"
				+ "<TH ALIGN=Left>Details</TH>"
				+ "<TH ALIGN=Left>RMS Image</TH>";

		header += "</TR>";

		return header;
	}

	private String getPrevNext(ServerResponse resp) {

		String prev_next = "\n<table id=\"prev_next_links\">\n<tr>";
		try {
			NodeList nodeList = (NodeList) resp.getParsedResponse().getAttribute("nodeList");

			if (nodeList == null) {
				return "";
			}

			// get viewState
			String viewState = "";

			NodeList nodes = nodeList
					.extractAllNodesThatMatch(new TagNameFilter("input"), true)
					.extractAllNodesThatMatch(new HasAttributeFilter("name", "__VIEWSTATE"), true);

			if (nodes.size() > 0) {
				InputTag in = (InputTag) nodes.elementAt(0);
				viewState = in.getAttribute("value");

				NodeList mainTableList = nodeList
						.extractAllNodesThatMatch(new TagNameFilter("table"), true)
						.extractAllNodesThatMatch(new HasAttributeFilter("id", "dgrdResults"), true);

				if (mainTableList.size() > 0) {
					TableRow[] rows = ((TableTag) mainTableList.elementAt(0)).getRows();

					TableRow row = rows[rows.length - 1];

					if (row.getColumnCount() == 1) {
						TableColumn col = row.getColumns()[0];
						for (int i = 0; i < col.getChildCount(); i++) {
							if (col.getChild(i) instanceof LinkTag) {
								LinkTag l = (LinkTag) col.getChild(i);
								String href = l.getLink();

								String newHref = CreatePartialLink(TSConnectionURL.idPOST) + "results.aspx&__EVENTTARGET="
										+ href.split(",")[0].replaceAll("[^']*'([^']*)'", "$1").replace("$", ":") + "&__VIEWSTATE="
										+ viewState + "&__EVENTARGUMENT=";

								l.setLink(newHref);
							}
						}
						prev_next += col.toHtml();
					}
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		prev_next += "\n</tr></table>\n<hr>";
		return prev_next;
	}

	protected String getDetails(String rsResponse, StringBuilder accountId, HashMap<String, String> data, ServerResponse Response) {

		try {
			StringBuffer details = new StringBuffer();

			NodeList nodeList = new HtmlParser3(rsResponse).getNodeList();

			// tables
			NodeList tableList = nodeList
					.extractAllNodesThatMatch(new TagNameFilter("table"), true)
					.extractAllNodesThatMatch(new HasAttributeFilter("id", "tblDetails"));

			if (tableList.size() != 1) {
				return null;
			}

			String type = HtmlParser3.getValueFromAbsoluteCell(1, 0, HtmlParser3.findNode(tableList, "Instrument Type"), "", true);
			String docno = HtmlParser3.getValueFromAbsoluteCell(1, 0, HtmlParser3.findNode(tableList, "DOC.#/FLM-PG:"), "", true);

			data.put("type", type.trim());
			data.put("docno", docno);

			accountId.append(docno);

			String siteLinkPrefix = "";

			String viewImage = siteLinkPrefix + "viewimage.aspx&id=" + org.apache.commons.lang.StringUtils.leftPad(docno, docno.length() >= 12 ? 0 : 12, "0");

			if (StringUtils.isNotEmpty(viewImage)) {
				Response.getParsedResponse().addImageLink(new ImageLinkInPage(viewImage, accountId.toString() + ".tif"));
			}

			/* If from memory - use it as is */
			if (!rsResponse.contains("<HTML")) {
				return rsResponse;
			}

			TableTag table = (TableTag) tableList.elementAt(0);

			details.append("<table id=resultsDetail width=95% align=center>" +
					"<tr><td>" +
					"<p align=\"center\"><font style=\"font-size:xx-large;\"><b>RMIS Detail Results for Instrument " + docno + "</b></font></p><br>");

			// extract just the necesary rows

			NodeList tableRows = table.getChildren().extractAllNodesThatMatch(new TagNameFilter("tr")).extractAllNodesThatMatch(new HasAttributeFilter("id"));

			// make links for crossref
			NodeList crossrefs = tableRows.extractAllNodesThatMatch(new TagNameFilter("table"), true)
					.extractAllNodesThatMatch(new HasAttributeFilter("id", "dgrdRefer"));

			if (crossrefs.size() > 0) {
				TableTag t = (TableTag) crossrefs.elementAt(0);
				TableRow[] rws = t.getRows();
				for (int i = 1; i < rws.length; i++) {
					if (rws[i].getColumnCount() == 5 && rws[i].getColumns()[0].getChild(0) instanceof LinkTag) {
						LinkTag l = (LinkTag) rws[i].getColumns()[0].getChild(0);
						l.setLink(CreatePartialLink(TSConnectionURL.idGET) + l.getLink());
					}
				}
			}

			if (tableRows.size() > 0) {
				table.setChildren(tableRows);
			}

			String tableString = table.toHtml();

			// replace images
			tableString = tableString.replaceAll("(?ism)<img[^>]*>", "");

			details.append(tableString);

			// put image link
			if (StringUtils.isNotEmpty(viewImage)) {
				details.append("<table><tr><td><a href=\"" + CreatePartialLink(TSConnectionURL.idGET) + viewImage + "\">View Image</a></td></tr></table>");
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
			NodeList nodeList = new HtmlParser3(table).getNodeList();

			response.getParsedResponse().setAttribute("nodeList", nodeList);

			NodeList mainTableList = nodeList.extractAllNodesThatMatch(new TagNameFilter("table"), true)
					.extractAllNodesThatMatch(new HasAttributeFilter("id", "dgrdResults"), true);

			if (mainTableList.size() == 0) {
				return intermediaryResponse;
			}

			// save aspx parameters
			int seq = getSeq();

			Form form = new SimpleHtmlParser(table).getForm("frmResults");
			Map<String, String> params = form.getParams();
			mSearch.setAdditionalInfo(getCurrentServerName() + ":params:" + seq, params);

			TableTag t = (TableTag) mainTableList.elementAt(0);

			TableRow[] rows = t.getRows();

			String linkPrefix = CreatePartialLink(TSConnectionURL.idGET);

			for (int i = 1; i < rows.length - 1; i++) {
				TableRow row = rows[i];
				if (row.getColumnCount() == 7) {

					ResultMap m = ro.cst.tsearch.servers.functions.KSSedgwickRO.parseIntermediaryRow(row, (Integer) mSearch.getAdditionalInfo("viParseID"));

					String docLink = "";
					String imageLink = "";

					TableColumn col = row.getColumns()[5];

					if (col.getChildren().size() > 0 && col.getChild(0) instanceof LinkTag) {

						LinkTag linkTag = (LinkTag) (col.getChild(0));

						docLink = linkTag.getLink();

						if (StringUtils.isNotEmpty(docLink)) {
							linkTag.setLink(linkPrefix + docLink);
						}
					}

					// image link
					col = row.getColumns()[6];

					if (col.getChildren().size() > 0 && col.getChild(0) instanceof LinkTag) {

						LinkTag linkTag = (LinkTag) (col.getChild(0));

						imageLink = linkTag.getLink();

						if (StringUtils.isNotEmpty(imageLink)) {
							linkTag.setLink(linkPrefix + imageLink);
						}
					}

					String clean_row = row.toHtml();

					ParsedResponse currentResponse = new ParsedResponse();
					currentResponse.setAttribute(ParsedResponse.SERVER_ROW_RESPONSE, clean_row);
					currentResponse.setOnlyResponse(clean_row);
					currentResponse.setPageLink(new LinkInPage(docLink, docLink, TSServer.REQUEST_SAVE_TO_TSD));

					Bridge bridge = new Bridge(currentResponse, m, searchId);

					DocumentI document = (RegisterDocumentI) bridge.importData();
					currentResponse.setDocument(document);

					String checkBox = "checked";

					HashMap<String, String> data = new HashMap<String, String>();

					String docno = org.apache.commons.lang.StringUtils.defaultString((String) m.get(SaleDataSetKey.DOCUMENT_NUMBER.getKeyName()));

					data.put("docno", docno);
					data.put("type", org.apache.commons.lang.StringUtils.defaultString((String) m.get(SaleDataSetKey.DOCUMENT_TYPE.getKeyName())));

					if (isInstrumentSaved(docno, null, data, true) && !Boolean.TRUE.equals(getSearch().getAdditionalInfo("RESAVE_DOCUMENT"))) {
						checkBox = "saved";
					} else {
						numberOfUncheckedElements++;
						LinkInPage linkInPage = new LinkInPage(linkPrefix + docLink, linkPrefix + docLink, TSServer.REQUEST_SAVE_TO_TSD);
						checkBox = "<input type='checkbox' name='docLink' value='" + linkPrefix + docLink + "'>";
						if (getSearch().getInMemoryDoc(docLink) == null) {
							getSearch().addInMemoryDoc(docLink, currentResponse);
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
			module = getDefaultServerInfo().getModule(TSServerInfo.BOOK_AND_PAGE_MODULE_IDX);
			filterCriteria.put("Book", book);
			filterCriteria.put("Page", page);
			module.forceValue(0, book);
			module.forceValue(1, page);
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
			HTTPResponse resp = ((ro.cst.tsearch.connection.http2.KSSedgwickRO) site).getImage(link);

			if (resp != null) {
				imageBytes = resp.getResponseAsByte();
			}
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

	protected void setCertificationDate() {
	}

	protected String getInstrumentNumberForSavingInFinalResults(DocumentI doc) {
		return super.getInstrumentNumberForSavingInFinalResults(doc);
	}

	protected Object parseAndFillResultMap(ServerResponse response, String detailsHtml, ResultMap map) {
		ro.cst.tsearch.servers.functions.KSSedgwickRO.parseAndFillResultMap(response, detailsHtml, map, searchId);
		return null;
	}

}
