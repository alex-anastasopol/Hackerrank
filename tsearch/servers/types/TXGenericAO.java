package ro.cst.tsearch.servers.types;

import java.io.IOException;
import java.math.BigDecimal;
import java.net.MalformedURLException;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Vector;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.httpclient.Cookie;
import org.apache.commons.httpclient.protocol.Protocol;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.math.NumberUtils;

import ro.cst.tsearch.ServerConfig;
import ro.cst.tsearch.bean.SearchAttributes;
import ro.cst.tsearch.connection.http.auth.ssl.EasySSLProtocolSocketFactory;
import ro.cst.tsearch.datatrace.Utils;
import ro.cst.tsearch.exceptions.ServerResponseException;
import ro.cst.tsearch.extractor.xml.Bridge;
import ro.cst.tsearch.extractor.xml.GenericFunctions;
import ro.cst.tsearch.extractor.xml.GenericFunctions1;
import ro.cst.tsearch.extractor.xml.GenericFunctions2;
import ro.cst.tsearch.extractor.xml.ResultMap;
import ro.cst.tsearch.extractor.xml.ResultTable;
import ro.cst.tsearch.extractor.xml.StringFormats;
import ro.cst.tsearch.parentsitedescribe.ParentSiteEditorUtils;
import ro.cst.tsearch.search.FunctionStatesIterator;
import ro.cst.tsearch.search.filter.FilterResponse;
import ro.cst.tsearch.search.filter.newfilters.address.AddressFilterFactory;
import ro.cst.tsearch.search.filter.newfilters.address.CityFilterFactory;
import ro.cst.tsearch.search.filter.newfilters.name.NameFilterFactory;
import ro.cst.tsearch.search.iterator.ModuleStatesIteratorFactory;
import ro.cst.tsearch.search.module.ConfigurableNameIterator;
import ro.cst.tsearch.servers.HashCountyToIndex;
import ro.cst.tsearch.servers.TSConnectionURL;
import ro.cst.tsearch.servers.bean.DataSite;
import ro.cst.tsearch.servers.info.TSServerInfo;
import ro.cst.tsearch.servers.info.TSServerInfoModule;
import ro.cst.tsearch.servers.response.LinkInPage;
import ro.cst.tsearch.servers.response.ParsedResponse;
import ro.cst.tsearch.servers.response.Parser;
import ro.cst.tsearch.servers.response.RawResponseWrapper;
import ro.cst.tsearch.servers.response.ServerResponse;
import ro.cst.tsearch.servers.response.PropertyAppraisalSet.PropertyAppraisalSetKey;
import ro.cst.tsearch.servers.response.TaxHistorySet.TaxHistorySetKey;
import ro.cst.tsearch.utils.IndividualLogger;
import ro.cst.tsearch.utils.InstanceManager;
import ro.cst.tsearch.utils.RegExUtils;
import ro.cst.tsearch.utils.SearchLogger;

import com.gargoylesoftware.htmlunit.CookieManager;
import com.gargoylesoftware.htmlunit.FailingHttpStatusCodeException;
import com.gargoylesoftware.htmlunit.ProxyConfig;
import com.gargoylesoftware.htmlunit.WebClient;
import com.gargoylesoftware.htmlunit.html.DomNodeList;
import com.gargoylesoftware.htmlunit.html.HtmlAnchor;
import com.gargoylesoftware.htmlunit.html.HtmlDivision;
import com.gargoylesoftware.htmlunit.html.HtmlElement;
import com.gargoylesoftware.htmlunit.html.HtmlPage;
import com.gargoylesoftware.htmlunit.html.HtmlSelect;
import com.gargoylesoftware.htmlunit.html.HtmlSpan;
import com.gargoylesoftware.htmlunit.html.HtmlSubmitInput;
import com.gargoylesoftware.htmlunit.html.HtmlTable;
import com.gargoylesoftware.htmlunit.html.HtmlTableCell;
import com.gargoylesoftware.htmlunit.html.HtmlTableRow;
import com.gargoylesoftware.htmlunit.html.HtmlTextInput;
import com.stewart.ats.base.document.DocumentI;

/**
 * 
 * for TX BexarIS(AO), ComalTR, KaufmanTR, KendallTR -like sites ADD here the
 * new county implemented with this Generic
 * 
 */
public class TXGenericAO extends TSServer {

	private boolean downloadingForSave;
	private static final long serialVersionUID = -211586944970644086L;
	String crtCounty = InstanceManager.getManager().getCurrentInstance(searchId).getCurrentCounty().getName();

	public TXGenericAO(long searchId) {
		super(searchId);
	}

	public TXGenericAO(String rsRequestSolverName, String rsSitePath, String rsServerID, String rsPrmNameLink, long searchId, int mid) {
		super(rsRequestSolverName, rsSitePath, rsServerID, rsPrmNameLink, searchId, mid);
	}

	@Override
	public ServerResponse SearchBy(TSServerInfoModule module, Object sd) throws ServerResponseException {
		return super.SearchBy(module, sd);
	}

	
	@Override
	public TSServerInfo getDefaultServerInfo() {
		
		TSServerInfo msiServerInfoDefault = super.getDefaultServerInfo();
		TSServerInfoModule tsServerInfoModule = msiServerInfoDefault.getModule(TSServerInfo.GENERIC_MODULE_IDX);
		if (tsServerInfoModule!= null && tsServerInfoModule.getFunctionList()!=null && tsServerInfoModule.getFunctionList().size() >7){
			//TSServerInfoFunction yearsFunction = tsServerInfoModule.getFunction(7);
			int currentYear = Calendar.getInstance().get(Calendar.YEAR);
			int defaultYear = currentYear;
			String generateYearSelectOptions = "";
			if (miServerID == 562801){//TXBexarIS(AO)
				defaultYear = currentYear;
			} else if (miServerID == 565902){//TXComalTR
				defaultYear = currentYear;
			} else if (miServerID == 574202){//TXKAufmanTR
				defaultYear = currentYear - 1;
			} else if (miServerID == 574302){//TXKendallTR
				defaultYear = currentYear;
			} else if (miServerID == 581202){//TXRockwallTR
				defaultYear = currentYear - 1;
				
			}
			generateYearSelectOptions = ParentSiteEditorUtils.generateYearSelectOptions("year", getStartYear(), currentYear, defaultYear);
			Utils.setupSelectBox(tsServerInfoModule.getFunction(7), generateYearSelectOptions);
			tsServerInfoModule.getFunction(7).setDefaultValue(Integer.toString(defaultYear));
		}
		setModulesForAutoSearch(msiServerInfoDefault);
		return msiServerInfoDefault;
	}
	
	protected int getStartYear(){
		return 1997;
	}
	
	@Override
	public String getBaseLink() {
		int commId = InstanceManager.getManager().getCommunityId(searchId);
		String baseLink = HashCountyToIndex.getDateSiteForMIServerID(commId, miServerID).getLink();
		int pos = baseLink.lastIndexOf('/');
		if (pos > 0) {
			baseLink = baseLink.substring(0, pos + 1);
		}
		return baseLink;
	}

	protected String getCIDFromLink() {
		int commId = InstanceManager.getManager().getCommunityId(searchId);
		String baseLink = HashCountyToIndex.getDateSiteForMIServerID(commId, miServerID).getLink();
		int pos = baseLink.lastIndexOf('?');
		if (pos > 0) {
			baseLink = baseLink.substring(pos + 1, baseLink.length());
		}
		return baseLink;
	}

	private static int seq = 0;

	protected synchronized static int getSeq() {
		return seq++;
	}

	@SuppressWarnings("deprecation")
	protected ServerResponse searchByInternal(Map<String, String> allParams, String baseLink) {

		String ownerName = URLDecoder.decode(StringUtils.defaultString(allParams.get("ownerName")));
		String streetNo = URLDecoder.decode(StringUtils.defaultString(allParams.get("streetNo")));
		String streetName = URLDecoder.decode(StringUtils.defaultString(allParams.get("streetName")));
		String pid = URLDecoder.decode(StringUtils.defaultString(allParams.get("pid")));
		String apn = URLDecoder.decode(StringUtils.defaultString(allParams.get("apn")));
		String business = URLDecoder.decode(StringUtils.defaultString(allParams.get("business")));
		String year = URLDecoder.decode(StringUtils.defaultString(allParams.get("year")));
		String propType = URLDecoder.decode(StringUtils.defaultString(allParams.get("propType")));
		String orderBy = URLDecoder.decode(StringUtils.defaultString(allParams.get("orderBy")));
		String resultsPerPage = URLDecoder.decode(StringUtils.defaultString(allParams.get("resultsPerPage")));

		final WebClient webClient = new WebClient();
		webClient.setJavaScriptEnabled(false);
		if (ServerConfig.isBurbProxyEnabled()) {
			ProxyConfig proxyConfig = new ProxyConfig();
			proxyConfig.setProxyPort(ServerConfig.getBurbProxyPort(8090));
			proxyConfig.setProxyHost("127.0.0.1");
			webClient.setProxyConfig(proxyConfig);
			/* Trust unsigned ssl certificates when using proxy */
			Protocol.registerProtocol("https", new Protocol("https", new EasySSLProtocolSocketFactory(), 443));
		}

		try {
			final HtmlPage searchPage = webClient.getPage(baseLink + "PropertySearch.aspx?" + getCIDFromLink());

			HtmlSubmitInput advancedBut = (HtmlSubmitInput) searchPage.getHtmlElementById("propertySearchOptions_advanced");
			HtmlPage advancedSearchPage = advancedBut.click();

			HtmlTextInput textInputName = (HtmlTextInput) advancedSearchPage.getHtmlElementById("propertySearchOptions_ownerName");
			textInputName.setValueAttribute(ownerName);

			HtmlTextInput textInputStreetNo = (HtmlTextInput) advancedSearchPage.getHtmlElementById("propertySearchOptions_streetNumber");
			textInputStreetNo.setValueAttribute(streetNo);

			HtmlTextInput textInputStreetName = (HtmlTextInput) advancedSearchPage.getHtmlElementById("propertySearchOptions_streetName");
			textInputStreetName.setValueAttribute(streetName);

			HtmlTextInput textInputPid = (HtmlTextInput) advancedSearchPage.getHtmlElementById("propertySearchOptions_propertyid");
			textInputPid.setValueAttribute(pid);

			HtmlTextInput textInputGeoNo = (HtmlTextInput) advancedSearchPage.getHtmlElementById("propertySearchOptions_geoid");
			textInputGeoNo.setValueAttribute(apn);

			HtmlTextInput textInputBussiness = (HtmlTextInput) advancedSearchPage.getHtmlElementById("propertySearchOptions_dba");
			textInputBussiness.setValueAttribute(business);

			HtmlSelect taxYearSelect = (HtmlSelect) advancedSearchPage.getHtmlElementById("propertySearchOptions_taxyear");
			taxYearSelect.getOptionByValue(year).setSelected(true);

			HtmlSelect propertyTypeSelect = (HtmlSelect) advancedSearchPage.getHtmlElementById("propertySearchOptions_propertyType");
			propertyTypeSelect.getOptionByValue(propType).setSelected(true);

			HtmlSelect propertyTypeOrderBySelect = (HtmlSelect) advancedSearchPage
					.getHtmlElementById("propertySearchOptions_orderResultsBy");
			propertyTypeOrderBySelect.getOptionByValue(orderBy).setSelected(true);

			HtmlSelect resultsperPageSelect = (HtmlSelect) advancedSearchPage.getHtmlElementById("propertySearchOptions_recordsPerPage");
			resultsperPageSelect.getOptionByValue(resultsPerPage).setSelected(true);

			HtmlSubmitInput searchButton = (HtmlSubmitInput) advancedSearchPage.getHtmlElementById("propertySearchOptions_search");
			HtmlPage resultsPage = searchButton.click();
			ServerResponse resp = new ServerResponse();
			resp.setPage(resultsPage);
			return resp;

		} catch (FailingHttpStatusCodeException e) {
			e.printStackTrace();
		} catch (MalformedURLException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		} catch (Exception e) { 
			e.printStackTrace();
		} finally {
			webClient.closeAllWindows();
		}
		return new ServerResponse();
	}

	protected  ServerResponse getPropertyDetailsUsingLink(String link, String baseLink, String cid, String querry) {
		final WebClient webClient = new WebClient();
		if (ServerConfig.isBurbProxyEnabled()) {
			ProxyConfig proxyConfig = new ProxyConfig();
			proxyConfig.setProxyPort(ServerConfig.getBurbProxyPort(8090));
			proxyConfig.setProxyHost("127.0.0.1");
			webClient.setProxyConfig(proxyConfig);
			/* Trust unsigned ssl certificates when using proxy */
			Protocol.registerProtocol("https", new Protocol("https", new EasySSLProtocolSocketFactory(), 443));
		}

		webClient.setJavaScriptEnabled(false);
		try {
			String sessionId = (String) mSearch.getAdditionalInfo(getCurrentServerName() + ":params:" + seq);
			if (StringUtils.isEmpty(sessionId)){
				webClient.getPage(baseLink + "PropertySearch.aspx?" + cid);
			}
			baseLink = prepareDetailsRequest(baseLink, webClient, sessionId);
			final HtmlPage detailPage = webClient.getPage(baseLink + link);
			ServerResponse resp = new ServerResponse();
			resp.setPage(detailPage);
			return resp;
		} catch (FailingHttpStatusCodeException e) {
			e.printStackTrace();
		} catch (MalformedURLException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			webClient.closeAllWindows();
		}
		return null;
	}

	/**
	 * @param baseLink
	 * @param webClient
	 * @param sessionId
	 * @return
	 */
	protected String prepareDetailsRequest(String baseLink, final WebClient webClient, String sessionId) {
		if (StringUtils.isNotEmpty(sessionId)){
			String domain = RegExUtils.getFirstMatch("http://(.*?)/", baseLink, 1);
			Cookie cookie = new Cookie(domain, getSessionKey(), sessionId);
			cookie.setPath("/");
			cookie.setPathAttributeSpecified(true);
			CookieManager cookieManager = webClient.getCookieManager();
			cookieManager.clearCookies();
			cookieManager.addCookie(cookie);
		}
		if (baseLink.endsWith("/")){
			baseLink = baseLink.substring(0, baseLink.length()-1);
		}
		return baseLink;
	}

	/**
	 * @return
	 */
	public String getSessionKey() {
		return "ASP.NET_SessionId";
	}

	@Override
	protected ServerResponse performRequest(String page, int methodType, String action, int parserId, String imagePath, String vbRequest,
			Map<String, Object> extraParams) throws ServerResponseException {

		boolean checkForDocType = false;
		String query = getTSConnection().getQuery();

		int count = 1;
		ServerResponse response = null;
		while (count <= 3) {
			try {
				if ("GetLink".equalsIgnoreCase(action) || "SaveToTSD".equalsIgnoreCase(action)) {
					response = getPropertyDetailsUsingLink(page, getBaseLink(), getCIDFromLink(), query);
				} else {
					Map<String, String> allParams = new HashMap<String, String>();
					String[] allParameters = query.split("[&=]");
					for (int i = 0; i < allParameters.length - 1; i += 2) {
						allParams.put(allParameters[i], allParameters[i + 1]);
					}
					response = searchByInternal(allParams, getBaseLink());
				}
				if (query.indexOf("parentSite=true") >= 0 || isParentSite()) {
					response.setParentSiteSearch(true);
				}
				response.setCheckForDocType(checkForDocType);
				response.setQuerry(query);
				response.setCheckForDocType(checkForDocType);
				ParsedResponse parsedResponse = response.getParsedResponse();
				if ((parsedResponse.getPageLink() == null || StringUtils.isNotBlank(parsedResponse.getPageLink().getLink()))
						&& StringUtils.isNotBlank(vbRequest)) {
					parsedResponse.setPageLink(new LinkInPage(vbRequest, vbRequest));
				}

				if(response.getPage() == null)
					response.setError(" Data Source Error!");
				
				RawResponseWrapper rrw = new RawResponseWrapper(response.getPage() != null ? response.getPage().asXml(): "");
				
				solveResponse(page, parserId, action, response, rrw, imagePath);
				break;
			} catch (Exception th) {
				logger.error("Unexpected Error...count=" + count + "\n" + th.getMessage());
				IndividualLogger.infoDebug(
						th.getMessage() + " " + ServerResponseException.getExceptionStackTrace(th).replaceAll("<BR>\n", "\n\n"), searchId);
				SearchLogger.info("</div>", searchId); // for Bug 2652
				th.printStackTrace(System.err);

				if (count == 6 || !continueSeachOnThisServer()) {
					ServerResponse sr = null;
					if (th instanceof ServerResponseException) {
						ServerResponseException sre = (ServerResponseException) th;
						sr = sre.getServerResponse();
						sr.setError(ServerResponse.DEFAULT_ERROR);
					} else {
						sr = new ServerResponse();
						sr.setError("Internal Error:" + ServerResponseException.getExceptionStackTrace(th),
								ServerResponse.CONNECTION_IO_ERROR);
					}

					response = sr;
					count = 7;
				}
			}
			count++;
		}

		return response;
	}

	private static final Pattern PAT_PID = Pattern.compile("(?i)<td[^>]*>\\s*Property\\s+ID\\s*:\\s*</td>\\s*<td>\\s*([0-9]+)\\s*</td>");
	private static final Pattern PAT_GEO_ID = Pattern
			.compile("(?i)<td[^>]*>\\s*Geographic\\s+ID\\s*:\\s*</td>\\s*<td>\\s*([0-9-]+)\\s*</td>");
	private static final Pattern PAT_GEO_ID_KAUF = Pattern
			.compile("(?i)<td[^>]*>\\s*Geographic\\s+ID\\s*:\\s*</td>\\s*<td>\\s*([0-9-A-Z\\.]+)\\s*</td>");

	@Override
	protected void ParseResponse(String sAction, ServerResponse Response, int viParseID) throws ServerResponseException {

		ParsedResponse parsedResponse = Response.getParsedResponse();
		String content = Response.getResult();
		switch (viParseID) {
		case ID_SEARCH_BY_PARCEL:
			StringBuilder outputTable = new StringBuilder();
			try {
				Collection<ParsedResponse> smartParsedResponses = smartParseIntermediary(Response, content, outputTable);
				if (smartParsedResponses.size() > 0) {
					parsedResponse.setResultRows(new Vector<ParsedResponse>(smartParsedResponses));
					parsedResponse.setOnlyResponse(outputTable.toString());
					parsedResponse.setAttribute(ParsedResponse.SERVER_ROW_RESPONSE, outputTable.toString());
				}

			} catch (Exception e) {
				e.printStackTrace();
			}
			break;

		case ID_DETAILS:
			if (Response.getPage().asXml().indexOf("Property not found") != -1) {
				Response.getParsedResponse().setError("<font color=\"red\">No results found</font>");
				return;
			}
			HtmlDivision main = (HtmlDivision) Response.getPage().getElementById("tabContent");

			DomNodeList<HtmlElement> allDivs = main.getElementsByTagName("div");
			for (HtmlElement div : allDivs) {
				div.removeAttribute("style");
			}
			removeElementWithTagName("input", main);
			removeElementWithTagName("img", main);
			removeElementWithTagName("a", main);

			modifyAttributeValueWithTagName("table", "cellpadding", main, "5");
			String details = main.asXml();
			String pid = "";
			if (this instanceof TXKaufmanTR || this instanceof TXComalTR) {
				pid = getPid(details, PAT_GEO_ID_KAUF);
			} else {
				pid = getPid(details, PAT_GEO_ID);
			}
			pid = pid.replaceAll("[\\.-]+", "");

			HtmlSpan spanWithTaxYear = (HtmlSpan) Response.getPage().getElementById("propertyHeading_propertyInfo");
			if (spanWithTaxYear != null) {
				details = spanWithTaxYear.asXml() + details;
			}

			// method asXml() replace &nbsp;, &ndash; with ISO 8859-1 value
			// witch is 160 and looks like naiba in tsrindex
			details = details.replace(((char) 160), '^');
			details = details.replaceAll("\\^", "&nbsp;");

			// &ndash; ---||----
			details = details.replace(((char) 8211), '-');

			if ((!downloadingForSave)) {
				String qry_aux = Response.getRawQuerry();
				qry_aux = "dummy=" + pid + "&" + qry_aux;
				String originalLink = sAction + "&" + qry_aux;
				String sSave2TSDLink = getLinkPrefix(TSConnectionURL.idGET) + originalLink;

				HashMap<String, String> data = new HashMap<String, String>();
				if (crtCounty.toLowerCase().contains("bexar")) {
					data.put("type", "ASSESSOR");
				} else {
					data.put("type", "CNTYTAX");
				}

				if (isInstrumentSaved(pid, null, data)) {
					details += CreateFileAlreadyInTSD();
				} else {
					mSearch.addInMemoryDoc(sSave2TSDLink, Response);
					details = addSaveToTsdButton(details, sSave2TSDLink, viParseID);
				}

				Response.getParsedResponse().setPageLink(new LinkInPage(sSave2TSDLink, originalLink, TSServer.REQUEST_SAVE_TO_TSD));
				parser.Parse(Response.getParsedResponse(), details, Parser.NO_PARSE);
			} else {
				smartParseDetails(Response, details);
				msSaveToTSDFileName = pid + ".html";
				Response.getParsedResponse().setFileName(getServerTypeDirectory() + msSaveToTSDFileName);
				msSaveToTSDResponce = details + CreateFileAlreadyInTSD();
			}

			break;

		case ID_GET_LINK:
			if (sAction.indexOf("prop_id=") != -1) {
				ParseResponse(sAction, Response, ID_DETAILS);
			}

			break;

		case ID_SAVE_TO_TSD:
			downloadingForSave = true;
			ParseResponse(sAction, Response, ID_DETAILS);
			downloadingForSave = false;
			break;
		}
	}

	public Collection<ParsedResponse> smartParseIntermediary(ServerResponse response, String table, StringBuilder outputTable) {

		Vector<ParsedResponse> intermediaryResponse = new Vector<ParsedResponse>();
		String tableHeader = "";

		HtmlPage page = response.getPage();

		if(page == null)
			return intermediaryResponse;
		
		HtmlTable tableRows = page.getHtmlElementById("propertySearchResults_resultsTable");
		String linkStart = CreatePartialLink(TSConnectionURL.idGET);
		int sequenceID = getSeq();
		Set<Cookie>cookies = response.getPage().getWebClient().getCookieManager().getCookies();
		String sessionIdKey = getSessionKey();
		for (Cookie cookie : cookies) {
			if (sessionIdKey.equals(cookie.getName())) {
				String sessionID = cookie.getValue();
				this.mSearch.setAdditionalInfo(getCurrentServerName() + ":params:" + seq, sessionID);
			}
		}
		
		int i = 0;
		for (HtmlTableRow tableRow : tableRows.getRows()) {
			try {
				HtmlAnchor anchor = null;
				String link = "";

				if (i == 0) {
					tableHeader = tableRow.asXml();
					continue;
				} else if (tableRow.getCells().size() <= 1) {
					continue;
				}

				List<HtmlElement> list = tableRow.getElementsByTagName("a");
				if (list != null && list.size() > 0) {
					anchor = (HtmlAnchor) list.get(0);
					link = anchor.getAttribute("href") + "&seq="+seq;
					anchor.setAttribute("href", linkStart + link);
				}

				removeElementWithTagName("img", tableRow);
				removeElementWithTagName("input", tableRow);
				tableRow.removeChild("a", 1); // delete view map link

				if (tableRow.getAttribute("class").equals("oddRow")) {
					tableRow.setAttribute("style", "background-color:#F0F5FB");
				}
				String rowHtml = tableRow.asXml();
				String pid2 = "";
				list = tableRow.getHtmlElementsByTagName("span");
				if (list != null && list.size() > 0) {
					HtmlSpan span = (HtmlSpan) list.get(0);
					pid2 = span.getAttribute("prop_id");
				}

				ParsedResponse currentResponse = new ParsedResponse();
				currentResponse.setAttribute(ParsedResponse.SERVER_ROW_RESPONSE, rowHtml);
				currentResponse.setOnlyResponse(rowHtml);
				currentResponse.setPageLink(new LinkInPage(linkStart + link, link, TSServer.REQUEST_SAVE_TO_TSD));

				ResultMap m = ro.cst.tsearch.servers.functions.TXGenericAO.parseIntermediaryRowTXBexarIS(tableRow, searchId, miServerID);
				m.put("PropertyIdentificationSet.ParcelID2", StringUtils.isNotBlank(pid2) ? pid2 : "");
				m.removeTempDef();
				Bridge bridge = new Bridge(currentResponse, m, searchId);

				DocumentI document = bridge.importData();
				currentResponse.setDocument(document);

				intermediaryResponse.add(currentResponse);

			} catch (Exception e) {
				e.printStackTrace();
			} finally {
				i++;
			}
		}

		response.getParsedResponse().setHeader("<table width=\"90%\" >" + tableHeader);
		response.getParsedResponse().setFooter("</table><br><br>");
		outputTable.append(table);

		return intermediaryResponse;
	}

	private static void removeElementWithTagNameAndString(String tagName, HtmlElement main, String text) {
		List<HtmlElement> allImages = main.getHtmlElementsByTagName(tagName);
		for (HtmlElement el : allImages) {
			if (!el.getFirstChild().getNextSibling().asXml().toLowerCase().contains(text.toLowerCase())) {
				el.remove();
			}
		}
	}

	private static void removeElementWithTagName(String tagName, HtmlElement main) {
		List<HtmlElement> allImages = main.getHtmlElementsByTagName(tagName);
		for (HtmlElement el : allImages) {
			el.remove();
		}
	}

	private static void modifyAttributeValueWithTagName(String tagName, String attribName, HtmlElement main, String newValue) {
		List<HtmlElement> allImages = main.getHtmlElementsByTagName(tagName);
		for (HtmlElement el : allImages) {
			if (el.hasAttribute(attribName)) {
				el.removeAttribute(attribName);
				el.setAttribute(attribName, newValue);
			}
		}
	}

	@Override
	protected Object parseAndFillResultMap(ServerResponse response, String detailsHtml, ResultMap map) {

		HtmlDivision main = (HtmlDivision) response.getPage().getElementById("tabContent");
		HtmlDivision taxDiv = (HtmlDivision) response.getPage().getElementById("taxDueDetails_dataSection");

		DomNodeList<HtmlElement> allDivs = main.getElementsByTagName("div");
		for (HtmlElement div : allDivs) {
			div.removeAttribute("style");
		}

		String details = main.asXml();
		String pid2 = getPid(details, PAT_PID);

		try {
			map.put("PropertyIdentificationSet.ParcelID2", StringUtils.isNotBlank(pid2) ? pid2 : "");
			HtmlTable deedHistory = (HtmlTable) main.getElementById("deedHistoryDetails_deedHistoryTable");

			int i = 0;
			List<List> body = new ArrayList<List>();
			List<String> line = null;
			for (HtmlTableRow row : deedHistory.getRows()) {
				if (i > 0) {
					line = new ArrayList<String>();
					String instr = "";
					String date = "";
					String type = "";
					String book = "";
					String page = "";
					String grantor = "";
					String grantee = "";
					List<HtmlTableCell> cells = row.getCells();
					if (cells.size() >= 8) {
						date = StringUtils.defaultString(cells.get(1).getTextContent());
						type = StringUtils.defaultString(cells.get(2).getTextContent());
						grantor = StringUtils.defaultString(cells.get(4).getTextContent());
						grantee = StringUtils.defaultString(cells.get(5).getTextContent());
						book = StringUtils.defaultString(cells.get(6).getTextContent());
						page = StringUtils.defaultString(cells.get(7).getTextContent());
						
						if(cells.size() == 9 ){
							instr = StringUtils.defaultString(cells.get(8).getTextContent());
							if(instr.matches("0+"))
								instr = "";
						}
							
						
						date = date.replaceAll("(?is)([\\d/]+)\\s+.*", "$1");
						
						book = book.replaceAll("(?is)([A-Z\\d]+)\\s+[A-Z\\s]+", "$1");// 130126000100
																						// ComalTR
						book = book.replaceAll("(?i)[^A-Z\\d]", "");
						book = ro.cst.tsearch.utils.StringUtils.removeLeadingZeroes(book);
						page = ro.cst.tsearch.utils.StringUtils.removeLeadingZeroes(page);
						
						if (book.trim().matches("0+") || page.trim().matches("0+")) {// B
																						// 4846
							continue;
						}
						if ((StringUtils.isBlank(page) || page.matches("(?is)[A-Z\\s]+")) && !StringUtils.isBlank(book)
								&& book.length() > 5) {
							
							if(StringUtils.isEmpty(instr))
								instr = book;
							
							book = "";
							if (page.matches("(?is)[A-Z\\s]+")) {
								page = page.replaceAll("(?is)[A-Z\\s]+", "");
							}
						}
						String[] instruments = null;
						if (instr.contains("-")) {
							instruments = StringFormats.ReplaceIntervalWithEnumeration(instr).split("\\s+");
							instr = instr.substring(0, instr.indexOf("-"));
						}
						if (page.contains("-")) {
							page = page.substring(0, page.indexOf("-"));
						}
						line.add(instr);
						line.add(date);
						line.add(type);
						line.add(grantor);
						line.add(grantee);
						line.add(book);
						line.add(page);
						body.add(line);

						// if there are more instruments then add them to the set on separate lines, without Date and Page
						if (instruments != null && instruments.length < 20) {
							for (int j = 1; j < instruments.length; j++) {
								line = new ArrayList<String>();
								line.add(instruments[j]);
								line.add("");
								line.add(type);
								line.add(grantor);
								line.add(grantee);
								line.add("");
								line.add("");
								body.add(line);
							}
						}
					}
				}
				i++;
			}

			ResultTable rt = new ResultTable();
			String[] header = { "InstrumentNumber", "InstrumentDate", "DocumentType", "Grantor", "Grantee", "Book", "Page" };
			rt = GenericFunctions2.createResultTable(body, header);
			map.put("SaleDataSet", rt);

			List<HtmlElement> tdLegal = main.getElementsByAttribute("td", "class", "propertyDetailsLegalDescription");
			if (tdLegal.size() == 1) {
				String legal = tdLegal.get(0).getTextContent();
				map.put("PropertyIdentificationSet.PropertyDescription", StringUtils.isNotBlank(legal) ? legal : "");
			}

			HtmlElement propDiv = (HtmlElement) main.getElementById("propertyDetails");
			Iterable<HtmlElement> propTable = propDiv.getChildElements();
			for (HtmlElement elem : propTable) {
				if (elem.asXml().contains("<table")) {
					HtmlTable table = (HtmlTable) elem;
					int rowcounter = 0;
					for (HtmlTableRow row : table.getRows()) {
						List<HtmlTableCell> cells = row.getCells();
						if (cells.size() > 1) {
							if (StringUtils.defaultString(cells.get(0).getTextContent()).contains("Geographic ID:")) {
								String pid = StringUtils.defaultString(cells.get(1).getTextContent()).trim();
								map.put("PropertyIdentificationSet.ParcelID", StringUtils.isNotBlank(pid) ? pid.replaceAll("[-\\.]+", "")
										: "");
							}

							if (StringUtils.defaultString(cells.get(0).getTextContent()).contains("Address:") && rowcounter < 9) {
								String address = StringUtils.defaultString(cells.get(1).getTextContent()).trim();
								map.put("tmpAddress", StringUtils.isNotBlank(address) ? address : "");
							}
							if (StringUtils.defaultString(cells.get(0).getTextContent()).contains("Name:")) {
								String ownerName = StringUtils.defaultString(cells.get(1).getTextContent()).trim();
								map.put("tmpOwner", StringUtils.isNotBlank(ownerName) ? ownerName : "");
							}
							if (StringUtils.defaultString(cells.get(0).getTextContent()).contains("Mailing Address:")) {
								String coOwnerName = StringUtils.defaultString(cells.get(1).getTextContent()).trim();
								map.put("tmpCoOwner", StringUtils.isNotBlank(coOwnerName) ? coOwnerName : "");
							}
						}
						rowcounter++;
					}
				}
			}
			
			if (TSServersFactory.isCountyTax(miServerID)) {
				HtmlElement valuesDiv = (HtmlElement) main.getElementById("valuesDetails");
				Iterable<HtmlElement> valueTable = valuesDiv.getChildElements();
				for (HtmlElement elem : valueTable) {
					if (elem.asXml().contains("<table")) {
						HtmlTable table = (HtmlTable) elem;
						StringBuilder landValue = new StringBuilder();
						StringBuilder improvementValue = new StringBuilder();
						landValue.append("0");
						improvementValue.append("0");
						for (HtmlTableRow row : table.getRows()) {
							List<HtmlTableCell> cells = row.getCells();
							if (cells.size() > 1) {
								if (StringUtils.defaultString(cells.get(0).getTextContent()).contains("Land Homesite Value") || 
										StringUtils.defaultString(cells.get(0).getTextContent()).contains("Land Non-Homesite Value")) {
									landValue.append("+").append(StringUtils
										.defaultString(cells.get(2).getTextContent().replaceAll("[\\$,]", "")).trim());
								}
								if (StringUtils.defaultString(cells.get(0).getTextContent()).contains("Improvement Homesite Value") || 
										StringUtils.defaultString(cells.get(0).getTextContent()).contains("Improvement Non-Homesite Value")) {
									improvementValue.append("+").append(StringUtils
										.defaultString(cells.get(2).getTextContent().replaceAll("[\\$,]", "")).trim());
								}
								if (StringUtils.defaultString(cells.get(0).getTextContent()).contains("Assessed Value")) {
									String assessedValue = StringUtils
										.defaultString(cells.get(2).getTextContent().replaceAll("[\\$,]", "")).trim();
									map.put(PropertyAppraisalSetKey.TOTAL_ASSESSMENT.getKeyName(), StringUtils.isNotBlank(assessedValue) ? assessedValue : "");
								}
							}
						}
						String landValueString = GenericFunctions1.sum(landValue.toString(), searchId);
						String improvementValueString = GenericFunctions1.sum(improvementValue.toString(), searchId);
						map.put(PropertyAppraisalSetKey.LAND_APPRAISAL.getKeyName(), StringUtils.isNotBlank(landValueString) ? landValueString : "");
						map.put(PropertyAppraisalSetKey.IMPROVEMENT_APPRAISAL.getKeyName(),	StringUtils.isNotBlank(improvementValueString) ? improvementValueString : "");
					}
				}
			}
			
			String taxYear = "";
			if (taxDiv != null) {
				if (taxDiv.hasChildNodes()) {
					HtmlTable taxTable = (HtmlTable) taxDiv.getFirstChild();
					if (taxTable != null) {

						for (HtmlTableRow row : taxTable.getRows()) {
							List<HtmlTableCell> cells = row.getCells();
							if (cells.size() == 9) {
								taxYear = cells.get(0).getTextContent().trim();
								if (taxYear.matches("\\d+")) {
									map.put("TaxHistorySet.Year", taxYear);
									break;
								}
							}
						}

						removeElementWithTagNameAndString("tr", taxTable, "County");

						int rowCounter = 0;
						String priorDelinq = "";
						for (HtmlTableRow row : taxTable.getRows()) {
							List<HtmlTableCell> cells = row.getCells();
							if (cells.size() == 9) {
								String year = cells.get(0).getTextContent().trim();
								if (rowCounter == 0  || year.equals(taxYear)) {
									map.put(TaxHistorySetKey.YEAR.getKeyName(), StringUtils.isNotBlank(year) ? year : "");

									String baseAmount = cells.get(3).getTextContent().trim();
									baseAmount = baseAmount.replaceAll("(?is)[\\$,]+", "");
									String previousSavedBaseAmount = (String) map.get(TaxHistorySetKey.BASE_AMOUNT.getKeyName());
									BigDecimal newAmount = new BigDecimal("0");
									newAmount = addAmounts(baseAmount, previousSavedBaseAmount);
									map.put(TaxHistorySetKey.BASE_AMOUNT.getKeyName(),"" + newAmount.doubleValue());

									String oldAmountPaid = (String) map.get(TaxHistorySetKey.AMOUNT_PAID.getKeyName());
									String amountPaid = cells.get(4).getTextContent().trim();
									amountPaid = amountPaid.replaceAll("(?is)[\\$,]+", "");
									BigDecimal newAmountPaid = addAmounts(amountPaid, oldAmountPaid);
									map.put(TaxHistorySetKey.AMOUNT_PAID.getKeyName(), "" + newAmountPaid.doubleValue());

									String totalDue = cells.get(8).getTextContent().trim();
									totalDue = totalDue.replaceAll("(?is)[\\$,]+", "");
									String oldTotalDue = (String) map.get(TaxHistorySetKey.TOTAL_DUE.getKeyName());
									BigDecimal newTotalDue = addAmounts(totalDue, oldTotalDue);
									map.put(TaxHistorySetKey.TOTAL_DUE.getKeyName(), "" + newTotalDue.doubleValue());
								} else {
									String priorDue = cells.get(8).getTextContent().trim();
									priorDue = priorDue.replaceAll("(?is)[\\$,]+", "");
									priorDelinq += (StringUtils.isNotBlank(priorDue) ? priorDue : "") + "+";
								}
								rowCounter++;

							}
						}
						map.put("TaxHistorySet.PriorDelinquent", GenericFunctions.sum(priorDelinq, searchId));

					}
				}
			}
			ro.cst.tsearch.servers.functions.TXGenericAO.parseAddressTXBexarIS(map, searchId, miServerID);
			ro.cst.tsearch.servers.functions.TXGenericAO.partyNamesTXBexarIS(map, searchId, miServerID);
			ro.cst.tsearch.servers.functions.TXGenericAO.legalTokenizer(map, searchId, miServerID);

		} catch (Exception e) {
			e.printStackTrace();
		}

		return null;
	}

	/**
	 * @param baseAmount
	 * @param previousSavedBaseAmount
	 * @param newAmount
	 * @return
	 */
	public BigDecimal addAmounts(String baseAmount, String previousSavedBaseAmount) {
		BigDecimal newAmount = new BigDecimal(0.0);
		if (NumberUtils.isNumber(previousSavedBaseAmount) && NumberUtils.isNumber(baseAmount)){
			BigDecimal oldAmount = new BigDecimal(previousSavedBaseAmount);
			newAmount = oldAmount.add(new BigDecimal(baseAmount));
		}else {
			if (NumberUtils.isNumber(baseAmount)){
				newAmount = new BigDecimal(baseAmount);	
			}
		}
		return newAmount;
	}

	protected String getPid(String details, Pattern pat) {
		Matcher matPid = pat.matcher(details);
		if (matPid.find()) {
			return matPid.group(1);
		}
		throw new RuntimeException("Could not extract the PID");
	}

	protected void setModulesForAutoSearch(TSServerInfo serverInfo) {
		List<TSServerInfoModule> modules = new ArrayList<TSServerInfoModule>();
		TSServerInfoModule module = null;
		SearchAttributes sa = getSearch().getSa();
		FilterResponse adressFilter = AddressFilterFactory.getAddressHybridFilter(searchId, 0.8d, true);
		FilterResponse nameFilterHybrid = NameFilterFactory.getHybridNameFilter(SearchAttributes.OWNER_OBJECT, searchId, module);
		FilterResponse cityFilter = CityFilterFactory.getCityFilterDefault(searchId);
		String strNo = getSearchAttribute(SearchAttributes.P_STREETNO);
		FilterResponse nameFilterHybridDoNotSkipUnique = null;

		int commId = InstanceManager.getManager().getCommunityId(searchId);
		DataSite site = HashCountyToIndex.getDateSiteForMIServerID(commId, miServerID);

		String pin = sa.getAtribute(SearchAttributes.LD_PARCELNO).replaceAll("[-]+", "");
		boolean emptyPid = "".equals(pin);

		if (site.getName().toLowerCase().contains("bexar") && pin.length() == 12) {
			pin = pin.replaceAll("([0-9]{5})([0-9]{3})([0-9]{4})", "$1-$2-$3");
		} else if (site.getName().toLowerCase().contains("kendall") && pin.length() == 13) {
			pin = pin.replaceAll("([0-9]{1})([0-9]{4})([0-9]{4})([0-9]{4})", "$1-$2-$3-$4");
		}

		if (!emptyPid) {
			module = new TSServerInfoModule(serverInfo.getModule(TSServerInfo.GENERIC_MODULE_IDX));
			module.clearSaKeys();
			if(pin.length()<=6){
				module.forceValue(4, pin);
			}else{
				module.forceValue(0, pin);
				sa.setAtribute(SearchAttributes.LD_GEO_NUMBER, pin);
			}
			modules.add(module);
		}

		// construct the list of street names
		String tmpName = getSearchAttribute(SearchAttributes.P_STREETNAME).trim();
		Set<String> strNames = new LinkedHashSet<String>();
		if (!StringUtils.isEmpty(tmpName)) {
			strNames.add(tmpName);
		}

		// we have cases when they put "." in the name of the street St.Jhons
		tmpName = tmpName.replace(".", " ").replaceAll("\\s{2,}", " ").trim();
		if (!StringUtils.isEmpty(tmpName)) {
			strNames.add(tmpName);
		}

		for (String strName : strNames) {

			// search without suffix
			if (!StringUtils.isBlank(strName)) {
				module = new TSServerInfoModule(serverInfo.getModule(TSServerInfo.GENERIC_MODULE_IDX));
				module.clearSaKeys();
				module.setData(2, strNo);
				module.setData(3, strName);
				module.addFilter(adressFilter);
				module.addFilter(cityFilter);
				module.addFilter(nameFilterHybrid);
				modules.add(module);
			}

			// eliminate direction from street name
			String DIR = "NORTH|SOUTH|EAST|WEST|N|S|E|W|NORTHEAST|NORTHWEST|SOUTHEAST|SOUTHWEST|NE|NW|SE|SW";
			String strName1 = strName.toUpperCase().replaceFirst("^(" + DIR + ")\\s(.+)", "$2");
			if (!strName.equalsIgnoreCase(strName1)) {
				module = new TSServerInfoModule(serverInfo.getModule(TSServerInfo.GENERIC_MODULE_IDX));
				module.clearSaKeys();
				module.setData(2, strNo);
				module.setData(3, strName1);
				module.addFilter(adressFilter);
				module.addFilter(cityFilter);
				module.addFilter(nameFilterHybrid);
				modules.add(module);
			}

			// eliminate second word from street name
			int idx = strName.indexOf(" ");
			String strName2 = strName;
			if (idx > 5) {
				strName2 = strName.substring(0, idx);
			}

			if (!strName.equalsIgnoreCase(strName2)) {
				module = new TSServerInfoModule(serverInfo.getModule(TSServerInfo.GENERIC_MODULE_IDX));
				module.clearSaKeys();
				module.setData(2, strNo);
				module.setData(3, strName2);
				module.addFilter(adressFilter);
				module.addFilter(cityFilter);
				module.addFilter(nameFilterHybrid);
				modules.add(module);
			}
		}

		// P3: Search by owners
		if (hasOwner()) {
			module = new TSServerInfoModule(serverInfo.getModule(TSServerInfo.GENERIC_MODULE_IDX));
			module.clearSaKeys();
			module.setSaObjKey(SearchAttributes.OWNER_OBJECT);
			nameFilterHybridDoNotSkipUnique = NameFilterFactory.getHybridNameFilter(SearchAttributes.OWNER_OBJECT, searchId, module);
			nameFilterHybridDoNotSkipUnique.setSkipUnique(false);
			if (strNames.size() > 0) {
				module.addFilter(adressFilter);
			}
			module.addFilter(nameFilterHybridDoNotSkipUnique);
			module.setIteratorType(1, FunctionStatesIterator.ITERATOR_TYPE_LAST_NAME_FAKE);

			module.addIterator((ConfigurableNameIterator) ModuleStatesIteratorFactory.getConfigurableNameIterator(module, searchId,
					new String[] { "L F;;" }));

			modules.add(module);
		}

		serverInfo.setModulesForAutoSearch(modules);
	}

	@Override
	protected ServerResponse FollowLink(String link, String imagePath) throws ServerResponseException {
		return super.FollowLink(link, imagePath);
	}

}
