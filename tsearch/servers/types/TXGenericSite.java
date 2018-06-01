package ro.cst.tsearch.servers.types;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStreamWriter;
import java.io.UnsupportedEncodingException;
import java.io.Writer;
import java.net.URLDecoder;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Vector;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang.StringUtils;
import org.htmlparser.Parser;
import org.htmlparser.util.NodeList;
import org.htmlparser.util.ParserException;
import org.pdfbox.pdmodel.PDDocument;
import org.pdfbox.util.PDFTextStripper;

import ro.cst.tsearch.ServerConfig;
import ro.cst.tsearch.bean.SearchAttributes;
import ro.cst.tsearch.exceptions.ServerResponseException;
import ro.cst.tsearch.extractor.xml.Bridge;
import ro.cst.tsearch.extractor.xml.GenericFunctions2;
import ro.cst.tsearch.extractor.xml.ResultMap;
import ro.cst.tsearch.extractor.xml.ResultTable;
import ro.cst.tsearch.parser.HtmlParser3;
import ro.cst.tsearch.parser.SimpleHtmlParser;
import ro.cst.tsearch.parser.SimpleHtmlParser.Form;
import ro.cst.tsearch.parser.SimpleHtmlParser.Input;
import ro.cst.tsearch.search.FunctionStatesIterator;
import ro.cst.tsearch.search.filter.FilterResponse;
import ro.cst.tsearch.search.filter.newfilters.address.AddressFilterFactory;
import ro.cst.tsearch.search.filter.newfilters.address.CityFilterFactory;
import ro.cst.tsearch.search.filter.newfilters.name.NameFilterFactory;
import ro.cst.tsearch.search.iterator.ModuleStatesIteratorFactory;
import ro.cst.tsearch.search.module.ConfigurableNameIterator;
import ro.cst.tsearch.servers.SearchDataWrapper;
import ro.cst.tsearch.servers.TSConnectionURL;
import ro.cst.tsearch.servers.info.TSServerInfo;
import ro.cst.tsearch.servers.info.TSServerInfoModule;
import ro.cst.tsearch.servers.response.ImageLinkInPage;
import ro.cst.tsearch.servers.response.LinkInPage;
import ro.cst.tsearch.servers.response.ParsedResponse;
import ro.cst.tsearch.servers.response.RawResponseWrapper;
import ro.cst.tsearch.servers.response.ServerResponse;
import ro.cst.tsearch.utils.FileUtils;
import ro.cst.tsearch.utils.InstanceManager;
import ro.cst.tsearch.utils.SearchLogger;
import ro.cst.tsearch.utils.gargoylesoftware.HtmlElementHelper;

import com.gargoylesoftware.htmlunit.Page;
import com.gargoylesoftware.htmlunit.ProxyConfig;
import com.gargoylesoftware.htmlunit.WebClient;
import com.gargoylesoftware.htmlunit.html.DomNode;
import com.gargoylesoftware.htmlunit.html.DomNodeList;
import com.gargoylesoftware.htmlunit.html.HtmlElement;
import com.gargoylesoftware.htmlunit.html.HtmlLabel;
import com.gargoylesoftware.htmlunit.html.HtmlPage;
import com.gargoylesoftware.htmlunit.html.HtmlSubmitInput;
import com.gargoylesoftware.htmlunit.html.HtmlTable;
import com.gargoylesoftware.htmlunit.html.HtmlTableRow;
import com.stewart.ats.base.document.DocumentI;
import com.stewart.ats.base.document.Instrument;
import com.stewart.ats.base.document.RegisterDocumentI;

@SuppressWarnings("deprecation")
public class TXGenericSite extends TXGenericAO {

	/**
	 * 
	 */
	private static final long serialVersionUID = 176238956537L;
	private static final SimpleDateFormat sdf = new SimpleDateFormat("MM/dd/yyyy");
	private static final Pattern SELECT_OWNER_PAT = Pattern.compile("(?is)(SelectPropertyOwner\\.aspx[^;]+)");

	private String crtCounty = InstanceManager.getManager().getCurrentInstance(searchId).getCurrentCounty().getName();
	
	public TXGenericSite(long searchId) {
		super(searchId);
	}

	public TXGenericSite(String rsRequestSolverName, String rsSitePath, String rsServerID, String rsPrmNameLink,
			long searchId, int mid) {
		super(rsRequestSolverName, rsSitePath, rsServerID, rsPrmNameLink, searchId, mid);
	}

	@Override
	protected ServerResponse performRequest(String page, int methodType, String action, int parserId, String imagePath,
			String vbRequest, Map<String, Object> extraParams) throws ServerResponseException {
		boolean checkForDocType = false;
		String query = getTSConnection().getQuery();
		ServerResponse response = null;
		try {
			if ("GetLink".equalsIgnoreCase(action) || "SaveToTSD".equalsIgnoreCase(action)) {
				response = getPropertyDetailsUsingLink(vbRequest, getBaseLink());
				//byte[] datasheet = getDatasheet(response);

				ImageLinkInPage imageLink = new ImageLinkInPage(false);
				imageLink.setContentType("application/pdf");
				String pdfUrl = getJavascriptTag(response.getPage());
				imageLink.setLink(pdfUrl);

				// get the link in order to make the fileName
				response.getParsedResponse().addImageLink(imageLink);
			} else {
				Map<String, String> allParams = ro.cst.tsearch.utils.StringUtils.getQueryMap(query);
				response = searchByInternal(allParams, getBaseLink() + page);
			}
			if (query.indexOf("parentSite=true") >= 0 || isParentSite()) {
				response.setParentSiteSearch(true);
			}
			response.setCheckForDocType(checkForDocType);
			response.setQuerry(query);
			response.setCheckForDocType(checkForDocType);

			// response.setImageResult(imageResult);
			ParsedResponse parsedResponse = response.getParsedResponse();
			if ((parsedResponse.getPageLink() == null || StringUtils.isNotBlank(parsedResponse.getPageLink().getLink()))
					&& StringUtils.isNotBlank(vbRequest)) {
				parsedResponse.setPageLink(new LinkInPage(vbRequest, vbRequest));
			}

			RawResponseWrapper rrw = new RawResponseWrapper(response.getPage().asXml());
			solveResponse(page, parserId, action, response, rrw, imagePath);
		} catch (Exception th) {
			th.printStackTrace();
		}
		return response;
	}

	/*private byte[] getDatasheet(ServerResponse response) {
		HtmlPage page = response.getPage();
		// get the js code for building the link in intermediary results
		String url = getJavascriptTag(page);
		Page dataSheet = HtmlElementHelper.getHtmlPageByURL(url);
		byte[] contentAsBytes = dataSheet.getWebResponse().getContentAsBytes();
		return contentAsBytes;
	}*/

	private String getJavascriptTag(HtmlPage page) {
		String javaScriptCode = page.getElementsByTagName("script").get(0).asXml();
		String url = getBaseLink() + getDatasheetLinkFromJavasCript(javaScriptCode).trim();
		return url;
	}

	private static String getDatasheetLinkFromJavasCript(String javascriptCode) {
		String functionName = "NavigateToDatasheet";
		Pattern pattern = Pattern.compile("(?is)(?<=" + functionName + ").*?\\}");
		Matcher matcher = pattern.matcher(javascriptCode);
		String navigateToDataSheetFunction = "";
		if (matcher.find()) {
			navigateToDataSheetFunction = matcher.group();
		}
		Pattern linkPattern = Pattern.compile("(?<=link =).*?;");
		Matcher linkMatcher = linkPattern.matcher(navigateToDataSheetFunction);
		String linkWithQuotation = "";
		if (linkMatcher.find()) {
			linkWithQuotation = linkMatcher.group();
		}
		String url = linkWithQuotation.replace("\"", "").replace(";", "").trim();
		return url;
	}

	protected static ServerResponse getPropertyDetailsUsingLink(String link, String baseLink) {
		int indexOf = baseLink.lastIndexOf("/");
		baseLink = baseLink.substring(0, indexOf);
		link = link.substring(link.lastIndexOf("Link=") + 5);
		String url = baseLink + "/" + link;
		return HtmlElementHelper.getServerResponseByUrl(url);
	}

	private boolean downloadingForSave = false;

	@SuppressWarnings("finally")
	protected void ParseResponse(String action, ServerResponse response, int viParseID) throws ServerResponseException {
		ParsedResponse parsedResponse = response.getParsedResponse();
		String htmlContent = response.getResult();
		switch (viParseID) {

		case ID_DETAILS:
			HtmlPage page = response.getPage();
			Iterable<HtmlElement> children = page.getAllHtmlChildElements();
			int i = 0;
			String details = "";
			DomNode detailsNode = null;
			// de revazut
			for (DomNode domNode : children) {
				i++;
				if (i == 128) {
					detailsNode = domNode;
					// cleanup
					break;
				}
			}

			String pid = getPropertyId(detailsNode.asXml());
			
			HtmlElementHelper.removeElementByXpath(detailsNode, "/html/body/table/tbody/tr[2]/td[2]/table/tbody/tr[34]");
			HtmlElementHelper.removeElementByXpath(detailsNode, "/html/body/table/tbody/tr[2]/td[2]/table/tbody/tr[2]");

			List<HtmlElement> allTables = page.getElementsByTagName("table");
			int d = 0;
			for (HtmlElement table : allTables){
				d++;
				if (table.asXml().contains("Parcel Information") && d > 1){
					details = table.asXml();
					
				}
			}
			
			if ("-1".equals(pid)){
				pid = getPropertyId(details);
			}
			
			//details = detailsNode.asXml();
			
			// used for Values Breakdown table. asXml() method replace &nbsp; with ISO 8859-1 value witch is 160 and looks like naiba in tsrindex
			details = details.replace(((char)160), '^');
			details = details.replaceAll("\\^", "&nbsp;");
			
			//String taxYear = ro.cst.tsearch.utils.StringUtils.extractParameterFromUrl(response.getRawQuerry(), "TaxYear");
			//details = details.replaceFirst("(?is)(<tr[^>]*>)\\s*(<td[^>]*>)\\s*(Owner\\s*ID[^<]*)(</td>\\s*<td[^>]*>)", "$1\r\n$2\r\nTax Year$4\r\n" 
				//					+ taxYear + "</td></tr>$1$2$3$4");
			//just to be sure
			details = details.replaceAll("(?is)<tr>\\s*<td>\\s*<a\\s+href\\s*=\\s*\\\".*?adobe.*?</a>\\s*\\.", "<tr><td>");
			//details = details.replaceAll("\\s", " ");
			details = details.replaceAll("(?is)<img[^>]+>", "");
			
			Pattern billsLink = Pattern.compile("(?is)document\\.location\\.href\\s*=\\s*\\\"(PropertyBills[^\\\"]+)");
			Matcher mat = billsLink.matcher(response.getResult());
			if (mat.find()){
				String taxTable = "";
				if (!crtCounty.contains("Nueces")){
					HtmlPage billPage = (HtmlPage) HtmlElementHelper.getHtmlPageByURL(getBaseLink() + mat.group(1));
					Form form = new SimpleHtmlParser(billPage.asXml()).getForm("PropertyBills.aspx");
					String link = form.action + "?";
					for (Input inp : form.inputs){
						if (!inp.name.contains("SortBy")){
							link += inp.name + "=" + inp.value + "&";
						}
					}
					link += "&SortBy=1";
					if (link.contains("SortBy")){
						billPage = (HtmlPage) HtmlElementHelper.getHtmlPageByURL(getBaseLink() + link);
					} else {
						billPage = null;
					}
					if (billPage != null){
						if ("Guadalupe".equals(crtCounty)){
							try {
								allTables = billPage.getElementsByTagName("table");
								String taxTbl = allTables.get(0).asXml();
								
								taxTbl = taxTbl.replaceAll("(?is)<td class=\\\"ssNavBar\\\".*?</tbody>\\s*</table>\\s*</td>", "");
								taxTbl = taxTbl.replaceAll("(?is)<tr class=\"ssPageHeader\".*?</tr>\\s*(<tr[^>]*>)", "$1");
								taxTbl = taxTbl.replaceAll("(?is)(<tbody>\\s*<tr[^>]*>\\s*<td[^>]*>.*?<tbody>)\\s*<tr>.*?(<tr[^>]*>\\s*<td[^>]*>\\s*<table id=\"tblFees\")", "$1\r\n$2");
								taxTbl = taxTbl.replaceAll("(?is)<col[^>]*>", "");
								Pattern trpat = Pattern.compile("(?is)<tr\\s+[style|id=][^>]*>.*?</tr>");
								Matcher matc = trpat.matcher(taxTbl);
								taxTable = " <table id=\"tblFees\" cellspacing=\"0\" cellpadding=\"0\" style=\"table-layout: fixed; width: 100%\">" 
												+ "<colgroup><col width=\"6%\"/> <col width=\"23%\"/> <col width=\"10%\"/> <col width=\"10%\"/> <col width=\"10%\"/>"
												+ "<col width=\"10%\"/>   <col width=\"10%\"/><col width=\"10%\"/>  <col width=\"12%\"/></colgroup><tbody>";
								while (matc.find()){
									taxTable += "\r\n" + matc.group(0);
								}
								taxTable += "</tbody></table>";
								taxTable = taxTable.replaceAll("(?is)(<td class=\"ssDetailData\" style=\"font-size:12px;\\s*[^;]*;? font-color: black; text-align: right; padding-right: 3px\">[^<]*</td>)\\s*</tr>", "</tr>");
								details += "<br><br><br>" + taxTable + "<br><br>";
							} catch(Exception e) {
								e.printStackTrace();
							}
						} else if ("San Patricio".equals(crtCounty)){
							allTables = billPage.getElementsByTagName("table");
							String taxTbl = allTables.get(0).asXml();
							
							taxTbl = taxTbl.replaceAll("(?is)<td class=\\\"ssNavBar\\\".*?</tbody>\\s*</table>\\s*</td>", "");
							taxTbl = taxTbl.replaceAll("(?is)<tr class=\"ssPageHeader\".*?</tr>\\s*(<tr[^>]*>)", "$1");
							taxTbl = taxTbl.replaceAll("(?is)(<tbody>\\s*<tr[^>]*>\\s*<td[^>]*>.*?<tbody>)\\s*<tr>.*?(<tr[^>]*>\\s*<td[^>]*>\\s*<table id=\"tblFees\")", "$1\r\n$2");
							taxTbl = taxTbl.replaceAll("(?is)<col[^>]*>", "");
							Pattern trpat = Pattern.compile("(?is)<tr\\s+[style|id=][^>]*>.*?</tr>");
							Matcher matc = trpat.matcher(taxTbl);
							taxTable = " <table id=\"tblFees\" cellspacing=\"0\" cellpadding=\"0\" style=\"table-layout: fixed; width: 100%\">" 
											+ "<colgroup><col width=\"6%\"/> <col width=\"23%\"/> <col width=\"10%\"/> <col width=\"10%\"/> <col width=\"10%\"/>"
											+ "<col width=\"10%\"/>   <col width=\"10%\"/><col width=\"10%\"/>  <col width=\"12%\"/></colgroup><tbody>";
							while (matc.find()){
								taxTable += "\r\n" + matc.group(0);
							}
							taxTable += "</tbody></table>";
							taxTable = taxTable.replaceAll("(?is)(<td class=\"ssDetailData\" style=\"font-size:12px;\\s*[^;]*;? font-color: black; text-align: right; padding-right: 3px\">[^<]*</td>)\\s*</tr>", "</tr>");
							details += "<br><br><br>" + taxTable + "<br><br>";
						}
					}
				}
			}
			
			details = details.replaceAll("(?is)<span[^>]*>\\s*Click\\s*here\\s*</span>[^<]*", "");
			
			// test
			// debugFunction(details, pid);
			// end test

			if (!downloadingForSave) {
				String rawQuerry = response.getRawQuerry();
				rawQuerry = "dummy=" + pid + "&" + rawQuerry;
				String originalLink = action + "&" + rawQuerry;
				String save2TSDLink = getLinkPrefix(TSConnectionURL.idGET) + originalLink;

				HashMap<String, String> data = new HashMap<String, String>();
				if (crtCounty.contains("Nueces")){
					data.put("type", "ASSESSOR");
					data.put("dataSource", "AO");
				} else {
					data.put("type", "CNTYTAX");
					data.put("dataSource", "TR");
				}

				if (isInstrumentSaved(pid, null, data)) {
					details += CreateFileAlreadyInTSD();
				} else {
					mSearch.addInMemoryDoc(save2TSDLink, response);
					details = addSaveToTsdButton(details, save2TSDLink, viParseID);
				}
				response.getParsedResponse().setPageLink(
						new LinkInPage(save2TSDLink, originalLink, TSServer.REQUEST_SAVE_TO_TSD));
				parser.Parse(response.getParsedResponse(), details, ro.cst.tsearch.servers.response.Parser.NO_PARSE);

			} else {
				// when the doc is saved then two things need to be done
				// set the pdf("Datasheet") from server page as a image for this
				// doc.

				smartParseDetails(response, details);
				msSaveToTSDFileName = pid + ".html";
				response.getParsedResponse().setFileName(getServerTypeDirectory() + msSaveToTSDFileName);
				msSaveToTSDResponce = details + CreateFileAlreadyInTSD();
				
				if (crtCounty.contains("Nueces")){
					addDocWithPlatFromAO(response, pid);
				}
				
			}

			break;
		case ID_SEARCH_BY_PARCEL:
		case ID_SEARCH_BY_INSTRUMENT_NO:
		case ID_SEARCH_BY_NAME:
		case ID_SEARCH_BY_ADDRESS:
			if (htmlContent.indexOf("No properties matched your search criteria.") > -1) {
				response.getParsedResponse().setError("<font color=\"red\">No results found.</font>");
				return;
			}
			StringBuilder outputTable = new StringBuilder();
			try {
				Collection<ParsedResponse> smartParsedResponses = smartParseIntermediary(response, htmlContent,
						outputTable);
				if (smartParsedResponses.size() > 0) {
					parsedResponse.setResultRows(new Vector<ParsedResponse>(smartParsedResponses));
					parsedResponse.setOnlyResponse(outputTable.toString());
					parsedResponse.setAttribute(ParsedResponse.SERVER_ROW_RESPONSE, outputTable.toString());
				}
			} catch (Exception e) {
				e.printStackTrace();
			} finally {
				break;
			}

		case ID_GET_LINK:
			if (action.indexOf("PropertyID=") != -1) {
				ParseResponse(action, response, ID_DETAILS);
			}
			break;
		case ID_SAVE_TO_TSD:
			downloadingForSave = true;
			ParseResponse(action, response, ID_DETAILS);
			downloadingForSave = false;
			break;
		}

	}

	protected void addDocWithPlatFromAO(ServerResponse response, String pid){

		TSInterface server = TSServersFactory.GetServerInstance((int)TSServersFactory.getSiteId("TX", "Nueces", "TP"), "06976", "24", searchId);
		TSServerInfoModule module = server.getCurrentClassServerInfo().getModuleForSearch(TSServerInfo.BOOK_AND_PAGE_LOCAL_MODULE_IDX, new SearchDataWrapper());
		server.setServerForTsd(mSearch, msSiteRealPath);
		
		Pattern tiffLink = Pattern.compile("(?is)document\\.location\\.href\\s*=\\s*\\\"(/MapImages[^\\\"]+)");;
		Matcher mat = tiffLink.matcher(response.getResult());
		String link = "";
		if (mat.find()){
			link = getBaseLink().replaceAll("/Appraisal/PublicAccess/", "") + mat.group(1);
		}
		String platBook = "", platPage = "";
		Pattern pat = Pattern.compile("(?is)mapimages/([^-]+)-([^\\.]+)");
		mat = pat.matcher(link);
		if (mat.find()){
			platBook = mat.group(1);
			platPage = mat.group(2);
		}
		module.setData(0,platBook);
		module.setData(1,platPage);
		module.addFunction();
		module.forceValue(3, "true");
		
		ServerResponse res = null;
		ImageLinkInPage lip = new ImageLinkInPage(link, pid + ".tif");
		try{
			res = server.SearchBy(module, lip); 
		} catch(Exception e){ 
				e.printStackTrace(); 
		}
		
		if (ro.cst.tsearch.utils.StringUtils.isNotEmpty(link)){
			String date = sdf.format(new Date());
			ParsedResponse pr = new ParsedResponse();
			pr.setResponse(res.getResult());
			res.setParsedResponse(pr);
			res.setResult("<b>Image Plat from TXNuecesAO</b><br>" +
					"<table><tr><td>Grantor</td><td>Grantee</td><td>Book</td><td>Page</td><td>Instrument Date</td><td>Recorded Date</td></tr>" +
					"<tr><td>County of Nueces</td><td></td><td>" + platBook + "</td><td>" + platPage + 
					"</td><td>" + date +"</td><td>" + date + "</td></tr></table>");
			res.getParsedResponse().addImageLink(new ImageLinkInPage(link, pid + ".tif"));

			Instrument instr = new Instrument();
			instr.setBook(platBook);
			instr.setPage(platPage);
			instr.setDocType("PLAT");
			instr.setDocSubType("PLAT");
			instr.setYear((new Date()).getYear()+1900);
			ResultMap m = new ResultMap();
			m.put("OtherInformationSet.SrcType", "TP");
			m.put("SaleDataSet.DocumentType", "PLAT");
			m.put("SaleDataSet.Grantor", "County of Nueces");
			m.put("SaleDataSet.RecordedDate", date);
			m.put("SaleDataSet.InstrumentDate", date);
			Bridge bridge = new Bridge(pr,m,searchId);
			DocumentI docR;
			try {
				docR = (RegisterDocumentI) bridge.importData();
				docR.setDataSource("TP");
				docR.setInstrument(instr);
				docR.setFake(true);
			    res.getParsedResponse().setDocument(docR);
			    pr.setDocument(docR);
	            docR.setChecked(true);
	            docR.setIncludeImage(true);
			} catch (Exception e1) {
				e1.printStackTrace();
			}
			
			addDocumentInATS(res, res.getResult());
			
			SearchLogger.info("<br><div><table border='1' cellspacing='0' width='99%'>" +
            		"<tr><th>No</th>" +
            		"<th>DS</th><th align='left'>Desc</th><th>Date</th><th>Grantor</th><th>Grantee</th><th>Instr Type</th><th>Instr</th><th>Remarks</th>" + 
            		"</tr>", searchId);
            StringBuilder sb = new StringBuilder();
            String doc = ((ParsedResponse) pr).getTsrIndexRepresentation();    	
            String id = String.valueOf(System.nanoTime());
            sb.append("<tr class='row' id='" + id + "'>");
            sb.append("<td>1</td>");
            sb.append(doc);
            sb.append("</tr>");     	
            SearchLogger.info(sb.toString(), searchId);
            SearchLogger.info("</table></div><br/><br/>", searchId);
		}
	}
	
	/*private void debugFunction(String details, String pid) {
		{
			Parser htmlParser = org.htmlparser.Parser.createParser(details, null);
			NodeList nl = null;
			try {
				nl = htmlParser.parse(null);
			} catch (ParserException e) {
				e.printStackTrace();
			}
			if (nl != null) {
				// get legal description and put it in
				String legalDescription = HtmlParser3.getValueFromSecondCell(
						HtmlParser3.findNode(nl, "Legal Description:"), "", false).trim().replaceAll("<br>", "\n");
				try {

					String outputFileName = "C:\\Documents and Settings\\l\\Desktop\\TXNuecesAO\\legal.txt";
					PrintWriter printWriter = new PrintWriter(new FileWriter(outputFileName, true));
					printWriter.println(pid);
					printWriter.println(legalDescription);
					printWriter.println();
					printWriter.close();
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		}
	}*/

	private String getPropertyId(String link) {
		Pattern pattern = Pattern.compile("(?is)(?<=Property Detail Sheet \\()(.*)(?=\\))");
		Matcher matcher = pattern.matcher(link);
		String ret = "-1";
		boolean find = matcher.find();
		if (find) {
			ret = matcher.group(1);
		}
		int idx = ret.indexOf(")");
		if (idx != -1){
			ret = ret.substring(0, ret.indexOf(")"));
		} 
		return ret;
	}

	@Override
	protected ServerResponse searchByInternal(Map<String, String> allParams, String baseLink) {
		Map<String, String> queryMap = ro.cst.tsearch.utils.StringUtils.getQueryMap(baseLink);
		// externale server search type
		String propertySearchType = queryMap.get("PropertySearchType");

		ServerResponse response = null;
		
		if (propertySearchType.equals("0")) {
			response = searchByAccountNumber(allParams, baseLink);
		} else if (propertySearchType.equals("1")) {
			response = searchByPropertyID(allParams, baseLink);
		} else if (propertySearchType.equals("2")) {
			response = searchByOwner(allParams, baseLink);
		} else if (propertySearchType.equals("3")) {
			response = searchByAddress(allParams, baseLink);
		} else if (propertySearchType.equals("4")) {
			response = searchByAdvancedModule(allParams, baseLink);
		} else {
			RuntimeException exception = new RuntimeException("NO MODULE IMPLEMENTED FOR GIVEN LINK!!!!");
			exception.printStackTrace();
		}

		return response;
	}

	private ServerResponse searchByAccountNumber(Map<String, String> allParams, String baseLink) {
		String enc = "UTF-8";
		// legal description specific parameters
		String propertyID = getURLDecodedElement(allParams, "AccountNumber", enc);
		String includeInactive = getURLDecodedElement(allParams, "IncludeInactive", enc);
		
		WebClient webClient = new WebClient();
		webClient.setJavaScriptEnabled(false);
		HtmlPage page = null;
		try {
			page = webClient.getPage(baseLink);
		} catch (Exception e) {
			e.printStackTrace();
		}
		ServerResponse resp = new ServerResponse();

		if (page != null) {
			HtmlElementHelper.setHtmlTextInputValueByName(page, propertyID, "AccountNumber");
			if (includeInactive.equals("on")) HtmlElementHelper.setHtmlCheckBoxInputValueByOptionValue(page, "IncludeInactive", true);
			HtmlSubmitInput searchButton = (HtmlSubmitInput) page.getHtmlElementById("SearchSubmit");

			HtmlPage resultsPage = null;
			
			// please do not delete this
			if (ServerConfig.isBurbProxyEnabled()) {
				ProxyConfig proxyConfig = new ProxyConfig();
				proxyConfig.setProxyHost("localhost");
				proxyConfig.setProxyPort(ServerConfig.getBurbProxyPort(8081));
				webClient.setProxyConfig(proxyConfig);
				// HostConfiguration config = httpClient.getHostConfiguration();
				// config.setProxy("127.0.0.1",
				// ServerConfig.getBurbProxyPort(8081));
				/* Trust unsigned ssl certificates when using proxy */
				// Protocol.registerProtocol("https", new Protocol("https", new
				// EasySSLProtocolSocketFactory(), 443));
			}
			try {
				webClient.setTimeout(0);
				resultsPage = searchButton.click();
			} catch (IOException e) {
				e.printStackTrace();
			}
			resp.setPage(resultsPage);
		}

		return resp;
	}
	
	private ServerResponse searchByPropertyID(Map<String, String> allParams, String baseLink) {
		String enc = "UTF-8";
		// legal description specific parameters
		String propertyID = getURLDecodedElement(allParams, "PropertyID", enc);
		String includeInactive = getURLDecodedElement(allParams, "IncludeInactive", enc);
		
		WebClient webClient = new WebClient();
		webClient.setJavaScriptEnabled(false);
		HtmlPage page = null;
		try {
			page = webClient.getPage(baseLink);
		} catch (Exception e) {
			e.printStackTrace();
		}
		ServerResponse resp = new ServerResponse();

		if (page != null) {
			HtmlElementHelper.setHtmlTextInputValueByName(page, propertyID, "PropertyID");
			if (includeInactive.equals("on")) HtmlElementHelper.setHtmlCheckBoxInputValueByOptionValue(page, "IncludeInactive", true);
			HtmlSubmitInput searchButton = (HtmlSubmitInput) page.getHtmlElementById("SearchSubmit");
						
			HtmlPage resultsPage = null;
			
			// please do not delete this
			if (ServerConfig.isBurbProxyEnabled()) {
				ProxyConfig proxyConfig = new ProxyConfig();
				proxyConfig.setProxyHost("localhost");
				proxyConfig.setProxyPort(ServerConfig.getBurbProxyPort(8081));
				webClient.setProxyConfig(proxyConfig);
				// HostConfiguration config = httpClient.getHostConfiguration();
				// config.setProxy("127.0.0.1",
				// ServerConfig.getBurbProxyPort(8081));
				/* Trust unsigned ssl certificates when using proxy */
				// Protocol.registerProtocol("https", new Protocol("https", new
				// EasySSLProtocolSocketFactory(), 443));
			}
			try {
				webClient.setTimeout(0);
				resultsPage = searchButton.click();
			} catch (IOException e) {
				e.printStackTrace();
			}
			resp.setPage(resultsPage);
		}

		return resp;
	}

	private ServerResponse searchByOwner(Map<String, String> allParams, String baseLink) {
		String enc = "UTF-8";
		// legal description specific parameters
		String nameLast = getURLDecodedElement(allParams, "NameLast", enc);
		String nameFirst = getURLDecodedElement(allParams, "NameFirst", enc);
		String includeInactive = getURLDecodedElement(allParams, "IncludeInactive", enc);
		
		WebClient webClient = new WebClient();
		webClient.setJavaScriptEnabled(false);
		HtmlPage page = null;
		try {
			page = webClient.getPage(baseLink);
		} catch (Exception e) {
			e.printStackTrace();
		}
		ServerResponse resp = new ServerResponse();

		if (page != null) {
			HtmlElementHelper.setHtmlTextInputValueByName(page, nameLast, "NameLast");
			HtmlElementHelper.setHtmlTextInputValueByName(page, nameFirst, "NameFirst");
			if (includeInactive.equals("on")) HtmlElementHelper.setHtmlCheckBoxInputValueByOptionValue(page, "IncludeInactive", true);
			HtmlSubmitInput searchButton = (HtmlSubmitInput) page.getHtmlElementById("SearchSubmit");
						
			HtmlPage resultsPage = null;
			
			// please do not delete this
			if (ServerConfig.isBurbProxyEnabled()) {
				ProxyConfig proxyConfig = new ProxyConfig();
				proxyConfig.setProxyHost("localhost");
				proxyConfig.setProxyPort(ServerConfig.getBurbProxyPort(8081));
				webClient.setProxyConfig(proxyConfig);
				// HostConfiguration config = httpClient.getHostConfiguration();
				// config.setProxy("127.0.0.1",
				// ServerConfig.getBurbProxyPort(8081));
				/* Trust unsigned ssl certificates when using proxy */
				// Protocol.registerProtocol("https", new Protocol("https", new
				// EasySSLProtocolSocketFactory(), 443));
			}
			try {
				webClient.setTimeout(0);
				resultsPage = searchButton.click();
			} catch (IOException e) {
				e.printStackTrace();
			}
			resp.setPage(resultsPage);
		}

		return resp;
	}
	
	private ServerResponse searchByAddress(Map<String, String> allParams, String baseLink) {
		String enc = "UTF-8";
		// legal description specific parameters
		String streetNumber = getURLDecodedElement(allParams, "StreetNumber", enc);
		String streetName = getURLDecodedElement(allParams, "StreetName", enc);
		String city = getURLDecodedElement(allParams, "City", enc);
		String zipCode = getURLDecodedElement(allParams, "ZipCode", enc);
		String includeInactive = getURLDecodedElement(allParams, "IncludeInactive", enc);
		String cbxExact = getURLDecodedElement(allParams, "cbxExact", enc);
		
		WebClient webClient = new WebClient();
		webClient.setJavaScriptEnabled(false);
		HtmlPage page = null;
		try {
			page = webClient.getPage(baseLink);
		} catch (Exception e) {
			e.printStackTrace();
		}
		ServerResponse resp = new ServerResponse();

		if (page != null) {
			HtmlElementHelper.setHtmlTextInputValueByName(page, streetNumber, "StreetNumber");
			HtmlElementHelper.setHtmlTextInputValueByName(page, streetName, "StreetName");
			HtmlElementHelper.setHtmlTextInputValueByName(page, city, "City");
			HtmlElementHelper.setHtmlTextInputValueByName(page, zipCode, "ZipCode");
			if (includeInactive.equals("on")) HtmlElementHelper.setHtmlCheckBoxInputValueByOptionValue(page, "IncludeInactive", true);
			if (cbxExact.equals("1")) HtmlElementHelper.setHtmlCheckBoxInputValueByOptionValue(page, "cbxExact", true);
			HtmlSubmitInput searchButton = (HtmlSubmitInput) page.getHtmlElementById("SearchSubmit");
						
			HtmlPage resultsPage = null;
			
			// please do not delete this
			if (ServerConfig.isBurbProxyEnabled()) {
				ProxyConfig proxyConfig = new ProxyConfig();
				proxyConfig.setProxyHost("localhost");
				proxyConfig.setProxyPort(ServerConfig.getBurbProxyPort(8081));
				webClient.setProxyConfig(proxyConfig);
				// HostConfiguration config = httpClient.getHostConfiguration();
				// config.setProxy("127.0.0.1",
				// ServerConfig.getBurbProxyPort(8081));
				/* Trust unsigned ssl certificates when using proxy */
				// Protocol.registerProtocol("https", new Protocol("https", new
				// EasySSLProtocolSocketFactory(), 443));
			}
			try {
				webClient.setTimeout(0);
				resultsPage = searchButton.click();
			} catch (IOException e) {
				e.printStackTrace();
			}
			resp.setPage(resultsPage);
		}

		return resp;
	}
	
	private ServerResponse searchByAdvancedModule(Map<String, String> allParams, String baseLink) {
		String enc = "UTF-8";
		// legal description specific parameters
		String abstractSubdivisionCode = getURLDecodedElement(allParams, "AbstractSubdivisionCode", enc);
		String neighborhoodCode = getURLDecodedElement(allParams, "NeighborhoodCode", enc);
		String acresLeast = getURLDecodedElement(allParams, "AcresLeast", enc);
		String acresMost = getURLDecodedElement(allParams, "AcresMost", enc);
		String block = getURLDecodedElement(allParams, "Block", enc);
		String tractLot = getURLDecodedElement(allParams, "TractLot", enc);

		// address specific parameters
		String streetName = getURLDecodedElement(allParams, "StreetName", enc);
		String streetNumberLeast = getURLDecodedElement(allParams, "StreetNumberLeast", enc);
		String streetNumberMost = getURLDecodedElement(allParams, "StreetNumberMost", enc);
		String zipCode = getURLDecodedElement(allParams, "ZipCode", enc);
		String city = getURLDecodedElement(allParams, "City", enc);
		String neighborhoodCode2 = getURLDecodedElement(allParams, "NeighborhoodCode2", enc);

		// name specific parameters
		String nameFirst = getURLDecodedElement(allParams, "NameFirst", enc);
		String nameLast = getURLDecodedElement(allParams, "NameLast", enc);
		
		// property type parameter 
		String propertyType = getURLDecodedElement(allParams, "sbxPropertyType", enc);
		
		// sorting specific parameters
		String sortOrder1 = getURLDecodedElement(allParams, "SortOrder1", enc);
		String sortOrder2 = getURLDecodedElement(allParams, "SortOrder2", enc);
		
		// include inactive properties parameter 
		String includeInactive = getURLDecodedElement(allParams, "IncludeInactive", enc);

		WebClient webClient = new WebClient();
		webClient.setJavaScriptEnabled(false);

		HtmlPage page = null;
		try {
			page = webClient.getPage(baseLink);
		} catch (Exception e) {
			e.printStackTrace();
		}
		ServerResponse resp = new ServerResponse();
		if (page != null) {
			HtmlElementHelper.setHtmlTextInputValueByName(page, abstractSubdivisionCode, "AbstractSubdivisionCode");
			HtmlElementHelper.setHtmlTextInputValueByName(page, neighborhoodCode, "NeighborhoodCode");
			HtmlElementHelper.setHtmlTextInputValueByName(page, acresLeast, "AcresLeast");
			HtmlElementHelper.setHtmlTextInputValueByName(page, acresMost, "AcresMost");
			HtmlElementHelper.setHtmlTextInputValueByName(page, block, "Block");
			HtmlElementHelper.setHtmlTextInputValueByName(page, tractLot, "TractLot");
			HtmlElementHelper.setHtmlTextInputValueByName(page, streetName, "StreetName");
			HtmlElementHelper.setHtmlTextInputValueByName(page, streetNumberLeast, "StreetNumberLeast");
			HtmlElementHelper.setHtmlTextInputValueByName(page, streetNumberMost, "StreetNumberMost");
			HtmlElementHelper.setHtmlTextInputValueByName(page, zipCode, "ZipCode");
			HtmlElementHelper.setHtmlTextInputValueByName(page, city, "City");
			HtmlElementHelper.setHtmlTextInputValueByName(page, neighborhoodCode2, "NeighborhoodCode2");

			HtmlElementHelper.setHtmlTextInputValueByName(page, nameFirst, "NameFirst");
			HtmlElementHelper.setHtmlTextInputValueByName(page, nameLast, "NameLast");

			HtmlElementHelper.setHtmlSelectSelectedValueByOptionValue(page, "sbxPropertyType", propertyType);
			
			HtmlElementHelper.setHtmlSelectSelectedValueByOptionValue(page, "SortOrder1", sortOrder1);
			HtmlElementHelper.setHtmlSelectSelectedValueByOptionValue(page, "SortOrder2", sortOrder2);
			
			if (includeInactive.equals("on")) HtmlElementHelper.setHtmlCheckBoxInputValueByOptionValue(page, "IncludeInactive", true);

			// submit the form
			HtmlSubmitInput searchButton = (HtmlSubmitInput) page.getHtmlElementById("SearchSubmit");

			HtmlPage resultsPage = null;

			// please do not delete this
			if (ServerConfig.isBurbProxyEnabled()) {
				ProxyConfig proxyConfig = new ProxyConfig();
				proxyConfig.setProxyHost("localhost");
				proxyConfig.setProxyPort(ServerConfig.getBurbProxyPort(8081));
				webClient.setProxyConfig(proxyConfig);
				// HostConfiguration config = httpClient.getHostConfiguration();
				// config.setProxy("127.0.0.1",
				// ServerConfig.getBurbProxyPort(8081));
				/* Trust unsigned ssl certificates when using proxy */
				// Protocol.registerProtocol("https", new Protocol("https", new
				// EasySSLProtocolSocketFactory(), 443));
			}
			try {
				webClient.setTimeout(0);
				resultsPage = searchButton.click();
			} catch (IOException e) {
				e.printStackTrace();
			}
			//System.out.println(resultsPage.asXml());
			resp.setPage(resultsPage);
		} else {
			return resp;
		}

		System.out.println("  " + webClient);

		return resp;
	}

	private String getURLDecodedElement(Map<String, String> allParams, String parameterName, String enc) {
		String decode = "";
		try {
			decode = URLDecoder.decode(allParams.get(parameterName), enc);
		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
		}
		return decode;
	}

	@Override
	public Collection<ParsedResponse> smartParseIntermediary(ServerResponse response, String table,
			StringBuilder outputTable) {

		LinkedList<ParsedResponse> intermediaryResponse = new LinkedList<ParsedResponse>();

		HtmlPage page = response.getPage();
		
		DomNodeList<HtmlElement> elementsByTagName = page.getElementsByTagName("table");
		// get the table with the results
		HtmlTable tableP = (HtmlTable) elementsByTagName.get(6);

		String linkStart = CreatePartialLink(TSConnectionURL.idGET);
		// get the js code for building the link in intermediary results
		String onClickFunctionSourceCode = page.getElementsByTagName("script").get(0).asXml();

		// tbody index 6 is the header
		StringBuilder tableHeader = new StringBuilder("");
		int i = 0;
		//Map<String, String> names = new HashMap<String, String>();
		//Map<String, String> addresses = new HashMap<String, String>();

		for (HtmlElement htmlElement : tableP.getRows()) {

			// create the html for putput in intermediary results
			if (i == 0) {
				tableHeader.append(htmlElement.asXml());
				i++;
				continue;
			}
			// create the link
			HtmlLabel label = (HtmlLabel) htmlElement.getElementsByTagName("label").get(0);
			String propertyIdString = label.getAttribute("onclick");
			String linkForIntermediaryRow = buildLinkForIntermediaryRow(propertyIdString, onClickFunctionSourceCode);
			String propertyId = label.getTextContent();
			
			String rowHtml = htmlElement.asXml();
			
			if (htmlElement.getElementsByTagName("label").size() > 1){
				HtmlLabel label2 = (HtmlLabel) htmlElement.getElementsByTagName("label").get(1);
				if (label2 != null){
					String labelValue = label2.getTextContent();
					if (labelValue.contains("MULTIPLE OWNERS")){
						Matcher mat = SELECT_OWNER_PAT.matcher(page.asXml());
						if (mat.find()){
							String url = getBaseLink() + 
								mat.group(1).replaceAll("(?is)\\\"[^\\\"]+\\\"", propertyIdString.replaceAll("(?is).*?,[^,]+,([^\\)]+).*", "$1").trim());
							HtmlPage selectOwnerPage = (HtmlPage) HtmlElementHelper.getHtmlPageByURL(url);
							DomNodeList<HtmlElement> elements = selectOwnerPage.getElementsByTagName("table");
							// get the table with the results
							HtmlTable tabelu = (HtmlTable) elements.get(2);
							for (HtmlElement eachElem : tabelu.getRows()) {
								String currentRow = rowHtml;
								HtmlLabel labe = (HtmlLabel) eachElem.getElementsByTagName("label").get(0);
								String onclick = labe.getAttribute("onclick");
								String ownerName = labe.getTextContent();
								Pattern pattern = Pattern.compile("(?is)\\(\\s*([^,]+)\\s*,\\s*([^\\)]+)");
								Matcher matcher = pattern.matcher(onclick);
								if (matcher.find()){
									String interLink = getIntermediaryLinkTemplateForOneOwner(onClickFunctionSourceCode);
									interLink = interLink.replace("\"+PropertyID+\"", matcher.group(1).trim());
									interLink = interLink.replace("\"+PropertyOwnerID", matcher.group(2).trim());
									
									String newLink = "<a href=\"" + linkStart + interLink + "\">" + propertyId + "</a>";
									currentRow = currentRow.
										replaceAll("(?is)<label\\b[^>]*>\\s*?(?:.*?)\\s*?</label>\\s*</td>\\s*<td[^>]*>[^<]+<label\\b[^>]*>\\s*?(?:.*?)\\s*?</label>", 
																	newLink + "</td><td valign=\"top\" class=\"ssDataColumn\">" + ownerName);
									intermediaryResponse.add(addCurrentRowResponse(currentRow, htmlElement, linkStart, interLink));
								}
							}
						}
					}
				}
			} else {
				String newLink = "<a href=\"" + linkStart + linkForIntermediaryRow + "\">" + propertyId + "</a>";
				rowHtml = rowHtml.replaceAll("<label\\b[^>]*>\\s*?(.*?)\\s*?</label>", newLink);
				//label.setNodeValue(newLink);
				intermediaryResponse.add(addCurrentRowResponse(rowHtml, htmlElement, linkStart, linkForIntermediaryRow));
			}
			
			// new HtmlAnchor(propertyId, propertyId, page, attributes);
			
			// create the intermediary parsed row
			// test
			// String name = htmlElement.getChildNodes().get(1).asText();
			// names.put(name, name);
			// // test
			// String address = htmlElement.getChildNodes().get(2).asText();
			// addresses.put(address, address);
			// System.out.println("\"" + name + "\",");

		}
		// test
		// for (String name : names.keySet()) {
		// System.out.println("\"" + name + "\",");
		// }
		// test
		// for (String address : addresses.keySet()) {
		// System.out.println("\"" + address + "\",");
		// }

		response.getParsedResponse().setHeader("<table cellpadding=\"2\" cellspacing=\"0\" width=\"100%\" " +
				"style=\"table-layout: fixed; font-size: 10pt\" border=\"0\"><col width=\"10%\" /><col width=\"30%\" /><col width=\"50%\" />" +
				"<col width=\"10%\" />" + tableHeader);
		response.getParsedResponse().setFooter("</table><br><br>");
		outputTable.append(table);

		return intermediaryResponse;
	}

	private ParsedResponse addCurrentRowResponse(String rowHtml, HtmlElement htmlElement, String linkStart, String linkForIntermediaryRow){
		ParsedResponse currentResponse = new ParsedResponse();

		currentResponse.setAttribute(ParsedResponse.SERVER_ROW_RESPONSE, rowHtml);
		currentResponse.setPageLink(new LinkInPage(linkStart + linkForIntermediaryRow, linkForIntermediaryRow,
				TSServer.REQUEST_SAVE_TO_TSD));
		currentResponse.setOnlyResponse(rowHtml);

		// parse the intermediary row
		ResultMap resultMap = ro.cst.tsearch.servers.functions.TXNuecesAO.parseIntermediaryRow(
				(HtmlTableRow) htmlElement, searchId, miServerID);

		// resultMap.put("PropertyIdentificationSet.ParcelID2",
		// StringUtils.isNotBlank(pid2) ? pid2 : "");
		resultMap.removeTempDef();
		Bridge bridge = new Bridge(currentResponse, resultMap, searchId);

		DocumentI document = null;
		try {
			document = bridge.importData();
		} catch (Exception e) {
			e.printStackTrace();
		}
		currentResponse.setDocument(document);
		
		return currentResponse;
	}
	/**
	 * Format of input is: "ViewPropertyOrOwners( 1 , 179900 , 153957 )"
	 * 
	 * @param propertyIdString
	 * @return
	 */
	private String buildLinkForIntermediaryRow(String propertyIdString, String onClickFunctionSourceCode) {
		String regex = "\\s*(\\d+)\\s*,\\s*(\\d+)\\s*,\\s*(\\d+)\\s*";
		//String[] strings = propertyIdString.split(regex);
		Pattern pattern = Pattern.compile(regex);
		Matcher matcher = pattern.matcher(propertyIdString);

		String link = "";
		int ownerCount = 0;
		String propertyOwnerId = "";
		String propertyId = "";

		if (matcher.find()) {
			ownerCount = Integer.valueOf(matcher.group(1));
			propertyOwnerId = matcher.group(2);
			propertyId = matcher.group(3);
		}

		if (ownerCount <= 1) {
			link = getIntermediaryLinkTemplateForOneOwner(onClickFunctionSourceCode);
			link = link.replace("\"+PropertyID+\"", propertyId);
			link = link.replace("\"+PropertyOwnerID", propertyOwnerId);
		} else {

		}
		return link;
	}

	/**
	 * It should return something like this:
	 * PropertyDetail.aspx?PropertyID="+PropertyID+"
	 * &dbKeyAuth=Appraisal&TaxYear=
	 * 2009&NodeID=11&PropertyOwnerID="+PropertyOwnerID
	 * 
	 * @param onClickFunctionSourceCode
	 * @return
	 */
	private String getIntermediaryLinkTemplateForOneOwner(String onClickFunctionSourceCode) {
		String regEx = "(?<=document.location.href= \").*(?=;)";
		Pattern pattern = Pattern.compile(regEx);
		Matcher matcher = pattern.matcher(onClickFunctionSourceCode);
		String linkTemplate = "";
		if (matcher.find()) {
			linkTemplate = matcher.group();
		}

		return StringUtils.deleteWhitespace(linkTemplate);
	}

	/**
	 * It should return something like this:
	 * 
	 * @param onClickFunctionSourceCode
	 * @return
	 */
	/*private String getIntermediaryLinkTemplateForMultipleOwners(String onClickFunctionSourceCode) {
		return "";
	}*/

	@SuppressWarnings({ "unchecked" })
	@Override
	protected Object parseAndFillResultMap(ServerResponse response, String detailsHtml, ResultMap resultMap) {
		HtmlPage page = response.getPage();
		// "/html/body/table/tbody/tr[2]/td[2]/table/tbody/tr[28]/td/table/tbody/tr/td[1]");

		HtmlElement details = page.getFirstByXPath("/html/body/table/tbody/tr[2]/td[2]/table");
		String pageAsXml = details.asXml().toString();
		// get the property id(parcel ID)
		String propertyId = getPropertyId(pageAsXml);
		resultMap.put("PropertyIdentificationSet.ParcelID", propertyId);

		Parser htmlParser = org.htmlparser.Parser.createParser(pageAsXml, null);
		NodeList nl = null;
		try {
			nl = htmlParser.parse(null);
		} catch (ParserException e) {
			e.printStackTrace();
		}
		if (nl != null) {
			// get legal description and put it in
			String legalDescription = HtmlParser3.getValueFromSecondCell(
					HtmlParser3.findNode(nl, "Legal Description:"), "", false).trim().replaceAll("<br>", "\n");
			resultMap.put("PropertyIdentificationSet.PropertyDescription", StringUtils.defaultString(legalDescription));
			// get address
			String propertyAddress = HtmlParser3.getValueFromSecondCell(HtmlParser3.findNode(nl, "Property Address:"),
					"", false).trim().replaceAll("<br>", "\n");
			resultMap.put("tmpAddress", StringUtils.defaultString(propertyAddress));

			String taxYear = HtmlParser3.getValueFromSecondCell(HtmlParser3.findNode(nl, "Tax Year:"), "", false);
			resultMap.put("TaxHistorySet.Year", StringUtils.defaultString(taxYear));
			
			String owner = HtmlParser3.getValueFromSecondCell(HtmlParser3.findNode(nl, "Owner Name:"), "", false);
			resultMap.put("tmpOwner", StringUtils.defaultString(owner));
			String coOwner = HtmlParser3.getValueFromSecondCell(HtmlParser3.findNode(nl, "Owner Address:"), "", false);
			resultMap.put("tmpcoOwner", StringUtils.defaultString(coOwner));
			
			String landHS = HtmlParser3.getValueFromSecondCell(HtmlParser3.findNode(nl, "Land HS:"), "", false);
			landHS = ro.cst.tsearch.utils.StringUtils.isEmpty(landHS) ? "0" : landHS;
			landHS = landHS.replaceAll("[\\s\\$,\\+]+", "").replaceAll("(?is)([\\d\\.]+).*", "$1");
			String landNHS = HtmlParser3.getValueFromSecondCell(HtmlParser3.findNode(nl, "Land NHS:"), "", false);
			landNHS = ro.cst.tsearch.utils.StringUtils.isEmpty(landNHS) ? "0" : landNHS;
			landNHS = landNHS.replaceAll("[\\s\\$,\\+]+", "").replaceAll("(?is)([\\d\\.]+).*", "$1");
			String landValue = GenericFunctions2.sum(landHS + "+" + landNHS, searchId);
			resultMap.put("PropertyAppraisalSet.LandAppraisal", landValue);
			
			String improvementHS = HtmlParser3.getValueFromSecondCell(HtmlParser3.findNode(nl, "Improvement HS:"), "", false);
			improvementHS = ro.cst.tsearch.utils.StringUtils.isEmpty(improvementHS) ? "0" : improvementHS;
			improvementHS = improvementHS.replaceAll("[\\s\\$,\\+]+", "").replaceAll("(?is)([\\d\\.]+).*", "$1");
			String improvementNHS = HtmlParser3.getValueFromSecondCell(HtmlParser3.findNode(nl, "Improvement NHS:"), "", false);
			improvementNHS = ro.cst.tsearch.utils.StringUtils.isEmpty(improvementNHS) ? "0" : improvementNHS;
			improvementNHS = improvementNHS.replaceAll("[\\s\\$,\\+]+", "").replaceAll("(?is)([\\d\\.]+).*", "$1");
			String improvementValue = GenericFunctions2.sum(improvementHS + "+" + improvementNHS, searchId);
			resultMap.put("PropertyAppraisalSet.ImprovementAppraisal", improvementValue);
			
			String assessed = HtmlParser3.getValueFromSecondCell(HtmlParser3.findNode(nl, "Assessed:"), "", false);
			assessed = ro.cst.tsearch.utils.StringUtils.isEmpty(assessed) ? "0" : assessed;
			assessed = assessed.replaceAll("[\\s\\$,\\+]+", "").replaceAll("(?is)([\\d\\.]+).*", "$1");
			resultMap.put("PropertyAppraisalSet.TotalAssessment", assessed);

			// put saledataset
			//String deedType = HtmlParser3.getValueFromSecondCell(HtmlParser3.findNode(nl, "Deed Type:"), "", false);

			// deedBook+deedPage is in fact instrument number
			ImageLinkInPage imageLink = response.getParsedResponse().getImageLink(0);
			String pdfContent = getPdfContent(imageLink);

			// method to split all results in two Strings
			//String deedBook = HtmlParser3.getValueFromSecondCell(HtmlParser3.findNode(nl, "Deed Book:"), "", false);
			//String deedPage = HtmlParser3.getValueFromSecondCell(HtmlParser3.findNode(nl, "Deed Page:"), "", false);

			//List<List> body = new ArrayList<List>();
			//List<String> line = new ArrayList<String>();
			
			// get the Sales table from pdf
			@SuppressWarnings("rawtypes")
			List<List> buildLineForResultsBody = ro.cst.tsearch.servers.functions.TXNuecesAO
					.buildLineForResultsBody(pdfContent);
			//buildLineForResultsBody.addline);
			
			//deedBook = deedBook.replaceAll("[^\\d+]", "").trim();
			//deedPage = deedPage.replaceAll("[^\\d+]", "").trim();
			//String[] rowsForConcatenation = {deedPage,deedBook};
			//buildLineForResultsBody.addAll(ro.cst.tsearch.servers.functions.TXNuecesAO.constructInstrumentNumber(rowsForConcatenation ));
			
			// is Account Number in TP
			String crossReference = HtmlParser3.getValueFromSecondCell(HtmlParser3.findNode(nl, "Cross Reference:"),
					"", false);
			resultMap.put("PropertyIdentificationSet.ParcelID2", crossReference);
			
			ResultTable rt = new ResultTable();
			String[] header = { "InstrumentNumber", "Book", "Page" };
			rt = GenericFunctions2.createResultTable(buildLineForResultsBody, header);
			resultMap.put("SaleDataSet", rt);
			
			// test
			// ro.cst.tsearch.utils.FileUtils.writeTextFile("C:\\Documents and Settings\\l\\Desktop\\TXNuecesAO\\"+propertyId+".txt",
			// pdfContent);
		}
		
		
		try {
			ro.cst.tsearch.servers.functions.TXNuecesAO.parseLegal(resultMap, searchId, miServerID);
		} catch (Exception e) {
			e.printStackTrace();
		}

		try {
			ro.cst.tsearch.servers.functions.TXNuecesAO.parseNames(resultMap, searchId, miServerID);
		} catch (Exception e) {
			e.printStackTrace();
		}
		
		ro.cst.tsearch.servers.functions.TXNuecesAO.parseAddress(resultMap, searchId, miServerID);
		
		ro.cst.tsearch.servers.functions.TXNuecesAO.parseTax(resultMap, detailsHtml, searchId, miServerID);

		// get map.put("tmpCoOwner", StringUtils.isNotBlank(ownerName) ?
		// ownerName : "");

		return null;
	}

	private String getPdfContent(ImageLinkInPage imageLink) {
		String url = imageLink.getLink();
		Page dataSheet = HtmlElementHelper.getHtmlPageByURL(url);
		if (dataSheet!=null) {
			byte[] contentAsBytes = dataSheet.getWebResponse().getContentAsBytes();
			InputStream stream = new ByteArrayInputStream(contentAsBytes);
			String fileName = url.replaceAll("\\?", "").replaceAll("http://", "").replaceAll("&", "").replaceAll("/", "")
					.replaceAll("=", "").replaceAll("\\.", "");
			String filePath = getSearch().getSearchDir() + File.separator + fileName + ".pdf";
			@SuppressWarnings("unused")
			boolean createNewFile = false;
			try {
				createNewFile = new File(filePath).createNewFile();
			} catch (IOException e1) {
				e1.printStackTrace();
			}
			ro.cst.tsearch.utils.FileUtils.writeStreamToFile(stream, filePath);
			imageLink.setPath(filePath);
			imageLink.setDownloadStatus("DOWNLOAD_OK");
			imageLink.setSolved(true);
			imageLink.setImageFileName(fileName + ".pdf");

			Writer output = null;
			ByteArrayOutputStream bas = new ByteArrayOutputStream();
			PDDocument document = null;
			try {
				document = PDDocument.load(filePath);
				output = new OutputStreamWriter(bas);

				PDFTextStripper stripper = new PDFTextStripper();
				stripper.getTextMatrix();
				//PDFTextStripperByArea textStripperByArea = new PDFTextStripperByArea();

				// textStripperByArea.extractRegions()
				stripper.setWordSeparator("     ");

				//do not change this. will blow up the output of the pdf and the parsing of him will fail
				stripper.setSortByPosition(true);
				// PDFText2HTML stripper = new PDFText2HTML();

				stripper.writeText(document, output);

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

			return bas.toString();
		}
		return "";
	}

	protected void setModulesForAutoSearch(TSServerInfo serverInfo) {
		TSServerInfoModule module = null;
		//FilterResponse adressFilter 	= AddressFilterFactory.getAddressHybridFilter( searchId , 0.8d , true);
		FilterResponse nameFilterHybrid = NameFilterFactory.getHybridNameFilter( SearchAttributes.OWNER_OBJECT , searchId , module );
		FilterResponse cityFilter = CityFilterFactory.getCityFilterDefault(searchId);
		
		
		SearchAttributes sa = getSearch().getSa();
		ArrayList<TSServerInfoModule> modules = new ArrayList<TSServerInfoModule>();
		FilterResponse addressFilter 	= AddressFilterFactory.getAddressHybridFilter( searchId , 0.8d );
		FilterResponse defaultNameFilter = NameFilterFactory.getDefaultNameFilterNoSinonims( 
				SearchAttributes.OWNER_OBJECT , searchId , module );
		String pin = sa.getAtribute(SearchAttributes.LD_PARCELNO).replaceAll("[-]+", "");
		boolean emptyPid = ro.cst.tsearch.utils.StringUtils.isEmpty(pin);
		if (!emptyPid) {
			if (pin.length() > 9){
				module = new TSServerInfoModule(serverInfo.getModule(TSServerInfo.INSTR_NO_MODULE_IDX));
				module.clearSaKeys();
				module.forceValue(0, pin);
				modules.add(module);
				sa.setAtribute(SearchAttributes.LD_PARCELNO2, pin);
			} else {
				module = new TSServerInfoModule(serverInfo.getModule(TSServerInfo.PARCEL_ID_MODULE_IDX));
				module.clearSaKeys();
				module.forceValue(0, pin);
				modules.add(module);
				sa.setAtribute(SearchAttributes.LD_PARCELNO, pin);
			}
		}
		
		if (hasStreet()){
			module = new TSServerInfoModule(serverInfo.getModule(TSServerInfo.ADV_SEARCH_MODULE_IDX));
			module.clearSaKeys();
			module.addFilter(defaultNameFilter);
			module.forceValue(7, sa.getAtribute(SearchAttributes.P_STREETNAME));
			module.forceValue(8, sa.getAtribute(SearchAttributes.P_STREETNO));
			module.forceValue(9, sa.getAtribute(SearchAttributes.P_STREETNO));
			//module.addFilter( adressFilter );
			module.addFilter( cityFilter );
			module.addFilter( nameFilterHybrid );
			modules.add(module);
		}
		
		if (hasOwner()){
			//Search by Owner
			module = new TSServerInfoModule(serverInfo.getModule(TSServerInfo.ADV_SEARCH_MODULE_IDX));
			module.clearSaKeys();
			module.setSaObjKey(SearchAttributes.OWNER_OBJECT);
			module.addFilter(addressFilter);
			module.addFilter(defaultNameFilter);
			module.setIteratorType(13,FunctionStatesIterator.ITERATOR_TYPE_FIRST_NAME_FAKE);
			module.setIteratorType(14,FunctionStatesIterator.ITERATOR_TYPE_LAST_NAME_FAKE);
			ConfigurableNameIterator nameIterator = (ConfigurableNameIterator) ModuleStatesIteratorFactory
				.getConfigurableNameIterator(module, searchId, new String[] {"L;F;", "L;f;"});
			module.addIterator(nameIterator);
			modules.add(module);
		}
		
		NameFilterFactory.getHybridNameFilter( 
				SearchAttributes.OWNER_OBJECT , searchId , module );
		
		serverInfo.setModulesForAutoSearch(modules);
	}

	@Override
	protected DownloadImageResult saveImage(ImageLinkInPage image) throws ServerResponseException {

			String link = image.getLink();
			Page dataSheet = HtmlElementHelper.getHtmlPageByURL(link);
			byte[] contentAsBytes = dataSheet.getWebResponse().getContentAsBytes();
			afterDownloadImage();
			ServerResponse resp = new ServerResponse();

			String imageName = image.getPath();
	    	if(FileUtils.existPath(imageName)){
	    		contentAsBytes = FileUtils.readBinaryFile(imageName);
	    		return new DownloadImageResult( DownloadImageResult.Status.OK, contentAsBytes, image.getContentType() );
	    	}
	    	
			resp.setImageResult(new DownloadImageResult(DownloadImageResult.Status.OK, contentAsBytes,
					((ImageLinkInPage)image).getContentType()));
			
			DownloadImageResult dres = resp.getImageResult();
			//System.out.println("image");

		return dres;
	}
	/*
	@Override
	public TSServerInfo getDefaultServerInfo() {
		
		TSServerInfo msiServerInfoDefault = super.getDefaultServerInfo();
		TSServerInfoModule tsServerInfoModule = msiServerInfoDefault.getModule(TSServerInfo.PARCEL_ID_MODULE_IDX);
		if ("Nueces".equals(crtCounty)){
			msiServerInfoDefault.setServerLink("http://71.40.202.114");
			if (tsServerInfoModule != null){
				//tsServerInfoModule.se
			}
		} else if ("Guadalupe".equals(crtCounty)){
			msiServerInfoDefault.setServerLink("http://www.co.guadalupe.tx.us");
		} else if ("San Patricio".equals(crtCounty)){
			msiServerInfoDefault.setServerLink("http://12.105.136.220");
		}
		
		return msiServerInfoDefault;
	}*/

}
