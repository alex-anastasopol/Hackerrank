package ro.cst.tsearch.servers.types;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Vector;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.htmlparser.Node;
import org.htmlparser.filters.HasAttributeFilter;
import org.htmlparser.filters.TagNameFilter;
import org.htmlparser.tags.FormTag;
import org.htmlparser.tags.InputTag;
import org.htmlparser.tags.LinkTag;
import org.htmlparser.tags.OptionTag;
import org.htmlparser.tags.SelectTag;
import org.htmlparser.tags.TableColumn;
import org.htmlparser.tags.TableRow;
import org.htmlparser.tags.TableTag;
import org.htmlparser.util.NodeList;

import ro.cst.tsearch.Search;
import ro.cst.tsearch.bean.SearchAttributes;
import ro.cst.tsearch.connection.http.HTTPRequest;
import ro.cst.tsearch.connection.http.HTTPResponse;
import ro.cst.tsearch.connection.http2.HttpManager;
import ro.cst.tsearch.connection.http2.HttpSite;
import ro.cst.tsearch.exceptions.ServerResponseException;
import ro.cst.tsearch.extractor.xml.Bridge;
import ro.cst.tsearch.extractor.xml.GenericFunctions;
import ro.cst.tsearch.extractor.xml.GenericFunctions1;
import ro.cst.tsearch.extractor.xml.ResultMap;
import ro.cst.tsearch.extractor.xml.ResultTable;
import ro.cst.tsearch.extractor.xml.StringFormats;
import ro.cst.tsearch.parser.HtmlParser3;
import ro.cst.tsearch.parser.SimpleHtmlParser;
import ro.cst.tsearch.parser.SimpleHtmlParser.Form;
import ro.cst.tsearch.search.FunctionStatesIterator;
import ro.cst.tsearch.search.filter.FilterResponse;
import ro.cst.tsearch.search.filter.TaxYearFilterResponse;
import ro.cst.tsearch.search.filter.newfilters.address.AddressFilterFactory;
import ro.cst.tsearch.search.filter.newfilters.name.GenericNameFilter;
import ro.cst.tsearch.search.filter.newfilters.name.NameFilterFactory;
import ro.cst.tsearch.search.iterator.ModuleStatesIteratorFactory;
import ro.cst.tsearch.search.module.ConfigurableNameIterator;
import ro.cst.tsearch.search.name.NameUtils;
import ro.cst.tsearch.search.validator.DocsValidator;
import ro.cst.tsearch.servers.HashCountyToIndex;
import ro.cst.tsearch.servers.SearchDataWrapper;
import ro.cst.tsearch.servers.TSConnectionURL;
import ro.cst.tsearch.servers.info.TSServerInfo;
import ro.cst.tsearch.servers.info.TSServerInfoModule;
import ro.cst.tsearch.servers.response.LinkInPage;
import ro.cst.tsearch.servers.response.OtherInformationSet.OtherInformationSetKey;
import ro.cst.tsearch.servers.response.ParsedResponse;
import ro.cst.tsearch.servers.response.Parser;
import ro.cst.tsearch.servers.response.PropertyIdentificationSet.PropertyIdentificationSetKey;
import ro.cst.tsearch.servers.response.ServerResponse;
import ro.cst.tsearch.servers.response.TaxHistorySet.TaxHistorySetKey;
import ro.cst.tsearch.utils.InstanceManager;
import ro.cst.tsearch.utils.PDFUtils;
import ro.cst.tsearch.utils.StringUtils;
import ro.cst.tsearch.utils.Tidy;

import com.stewart.ats.base.document.DocumentI;
import com.stewart.ats.base.document.TaxDocumentI;

/**
 * @author oliviav
 */

public class TNWilliamsonTR extends TSServerAssessorandTaxLike {

	public static final long serialVersionUID = 10000000L;
	private boolean downloadingForSave;
	protected static HashMap<String, Integer> taxYears = new HashMap<String, Integer>();
	private static final String FORM_NAME = "aspnetForm";

	private static final Pattern dummyPattern = Pattern.compile("&dummy=([0-9]+)&");

	public TNWilliamsonTR(long searchId) {
		super(searchId);
	}

	public TNWilliamsonTR(String rsRequestSolverName, String rsSitePath,
			String rsServerID, String rsPrmNameLink, long searchId, int mid) {
		super(rsRequestSolverName, rsSitePath, rsServerID, rsPrmNameLink,
				searchId, mid);
	}

	protected void getTaxYears(String county) {
		if (taxYears.containsKey("lastTaxYear" + county)  && taxYears.containsKey("firstTaxYear" + county))
			return;
		
		String response = "";
		
		HttpSite site = HttpManager.getSite(getCurrentServerName(), searchId);
		try {
			response = ((ro.cst.tsearch.connection.http2.TNWilliamsonTR)site).getMainPageForTaxYear();
		} finally {
			HttpManager.releaseSite(site);
		}

		if(response != null) {
			HtmlParser3 parser = new HtmlParser3(response);
			NodeList selectList = parser.getNodeList()
					.extractAllNodesThatMatch(new TagNameFilter("select"), true)
					.extractAllNodesThatMatch(new HasAttributeFilter("name", "ctl00$MainContent$SkeletonCtrl_4$drpTaxYear"));
			if(selectList == null || selectList.size() == 0) {
				// Unable to find the tax year select input.
				logger.error("Unable to parse tax years!");
				return;
			}
			
			// Get the first and last tax years.
			SelectTag selectTag = (SelectTag) selectList.elementAt(0);
			OptionTag[] options = selectTag.getOptionTags();
			try {
				taxYears.put("lastTaxYear"+county, Integer.parseInt(options[0].getValue().trim()));
				taxYears.put("firstTaxYear"+county, Integer.parseInt(options[options.length - 2].getValue().trim()));
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}
	
	public String getYearSelect(String id, String name){
		// getTaxYears();
		String county = dataSite.getCountyName();
		getTaxYears(county);
		int lastTaxYear = taxYears.get("lastTaxYear" + county);
		int firstTaxYear = taxYears.get("firstTaxYear" + county);
		if (lastTaxYear <= 0 || firstTaxYear <= 0) {
			// No valid tax years.
			// This is going to happen when official site is down or it's going to change its layout.
			lastTaxYear = 2013;
			firstTaxYear = 2009;
		}
		
		// Generate input.
		StringBuilder select  = new StringBuilder("<select id=\"" + id + "\" name=\"" + name + "\" size=\"1\">\n");
		for (int i = lastTaxYear; i >= firstTaxYear; i--){
			select.append("<option ");
			select.append(i == lastTaxYear ? " selected " : "");
			select.append("value=\"" + i + "\">" + i + "</option>\n");
		}
		select.append("<option value=\"All\">All</option>");
		select.append("</select>");
		
		return select.toString();
	}
	
	
	@Override
	public TSServerInfo getDefaultServerInfo() {
		TSServerInfo msiServerInfoDefault = super.getDefaultServerInfo();

		String selectYearListAsHtml = getYearSelect("param_0_4", "param_0_4");  //for TaxYear select list
		TSServerInfoModule tsServerInfoModule = msiServerInfoDefault.getModule(TSServerInfo.NAME_MODULE_IDX);
		String currentTaxYear = Integer.toString(super.getCurrentTaxYear());
		String regExp = "(?is)(.*\\\"" + currentTaxYear + "\\\")";
		selectYearListAsHtml = selectYearListAsHtml.replaceFirst("selected ", "");
		selectYearListAsHtml = selectYearListAsHtml.replaceFirst(regExp, "$1 selected");
		
		if (tsServerInfoModule != null) {
			tsServerInfoModule.getFunction(4).setHtmlformat(selectYearListAsHtml);
		}
		
		tsServerInfoModule = msiServerInfoDefault.getModule(TSServerInfo.ADDRESS_MODULE_IDX);
		if (tsServerInfoModule != null) {
			selectYearListAsHtml = selectYearListAsHtml.replaceAll("(?is)param_\\d+_\\d+", "param_1_4");
			tsServerInfoModule.getFunction(4).setHtmlformat(selectYearListAsHtml);
		}
		
		tsServerInfoModule = msiServerInfoDefault.getModule(TSServerInfo.PARCEL_ID_MODULE_IDX);
		if (tsServerInfoModule != null) {
			selectYearListAsHtml = selectYearListAsHtml.replaceAll("(?is)param_\\d+_\\d+", "param_2_9");
			tsServerInfoModule.getFunction(9).setHtmlformat(selectYearListAsHtml);
		}

		tsServerInfoModule = msiServerInfoDefault.getModule(TSServerInfo.BOOK_AND_PAGE_MODULE_IDX);
		if (tsServerInfoModule != null) {
			selectYearListAsHtml = selectYearListAsHtml.replaceAll("(?is)param_\\d+_\\d+", "param_3_4");
			tsServerInfoModule.getFunction(4).setHtmlformat(selectYearListAsHtml);
		}

		setModulesForAutoSearch(msiServerInfoDefault);
		return msiServerInfoDefault;
	}
	
	
	@Override
	protected void setModulesForAutoSearch(TSServerInfo serverInfo) {
		List<TSServerInfoModule> modules = new ArrayList<TSServerInfoModule>();
		TSServerInfoModule module = null;

		String selectYearListAsHtml = getYearSelect("param_0_4", "param_0_4");  //for TaxYear select list
		
		String address = getSearch().getSa().getAtribute(SearchAttributes.P_STREET_NA_SU_NO);
		DocsValidator addressValidator = AddressFilterFactory.getGenericAddressHighPassFilter(searchId, 0.8d).getValidator();
		FilterResponse defaultNameFilter = NameFilterFactory.getDefaultNameFilter(SearchAttributes.OWNER_OBJECT, searchId, module);
		((GenericNameFilter) defaultNameFilter).setIgnoreMiddleOnEmpty(true);
		
		FilterResponse taxYearFilter = new TaxYearFilterResponse(searchId, true, numberOfYearsAllowed, true);

		Search global = InstanceManager.getManager().getCurrentInstance(searchId).getCrtSearchContext();
		int searchType = global.getSearchType();
		
		if (Search.AUTOMATIC_SEARCH == searchType) {
			//in automatic, default value = All; 
			//in PS default selection is 2013, as it is on official site
			String regExp = "(?is)(.*\\\"All\\\")";
			selectYearListAsHtml = selectYearListAsHtml.replaceFirst("selected ", "");
			selectYearListAsHtml = selectYearListAsHtml.replaceFirst(regExp, "$1 selected");			
		} 
		
		if (hasPin()) {
			// Search by PIN
				module = new TSServerInfoModule(serverInfo.getModule(TSServerInfo.PARCEL_ID_MODULE_IDX));
				module.clearSaKey(9);
				module.getFunction(9).forceValue("All");
				module.addFilter(taxYearFilter);
				modules.add(module);
		}
			
		if (hasStreet()) {
			// Search by Property Address
				module = new TSServerInfoModule(serverInfo.getModule(TSServerInfo.ADDRESS_MODULE_IDX));
				module.clearSaKeys();
				module.getFunction(4).forceValue("All");
				module.getFunction(0).forceValue(address);
				module.addFilter(defaultNameFilter);
				module.addFilter(taxYearFilter);
				module.addValidator(addressValidator);
				modules.add(module);
		}
			
		if (hasOwner()) {
			// Search by Owner
				module = new TSServerInfoModule(serverInfo.getModule(TSServerInfo.NAME_MODULE_IDX));
				module.setSaObjKey(SearchAttributes.OWNER_OBJECT);
				module.clearSaKeys();
				module.getFunction(4).forceValue("All");
				module.addFilter(defaultNameFilter);
				module.addFilter(taxYearFilter);
				module.addValidator(addressValidator);
				module.setIteratorType(0, FunctionStatesIterator.ITERATOR_TYPE_LF_NAME_FAKE);
				ConfigurableNameIterator nameIterator = (ConfigurableNameIterator) ModuleStatesIteratorFactory
						.getConfigurableNameIterator(module, searchId, new String[] { "L;F;" });
				module.addIterator(nameIterator);
				modules.add(module);
		}
			
		serverInfo.setModulesForAutoSearch(modules);
	}

	protected void loadDataHash(HashMap<String, String> data, String taxYear) {
		if (data != null) {
			data.put("type", "CNTYTAX");
			data.put("year", taxYear);
		}
	}
	
	@Override
	protected void ParseResponse(String sAction, ServerResponse Response, int viParseID) throws ServerResponseException {
		String initialResponse = Response.getResult();
		String rsResponse = initialResponse;
		
		ParsedResponse parsedResponse = Response.getParsedResponse();

		switch (viParseID) {
		case ID_SEARCH_BY_NAME:
		case ID_SEARCH_BY_ADDRESS:
		case ID_SEARCH_BY_PARCEL:
			StringBuilder outputTable = new StringBuilder();

			if (rsResponse.indexOf("No Records Found") != -1) {
				Response.getParsedResponse().setError("<font color=\"red\">No results found!</font>");
				return;
			} else if (rsResponse.indexOf("Too Many Records Returned.") != -1) {
				Response.getParsedResponse().setError("<font color=\"red\">Too many records returned.  Please narrow your search criteria! </font>");
				return;
			} else if (rsResponse.indexOf("An error has occured and has been logged") != -1) {
				Response.getParsedResponse().setError("<font color=\"red\">An error has occured and has been logged. If this continues, Please contact the Revenue Division.</font>");
				return;
			}

			try {
				Collection<ParsedResponse> smartParsedResponses = smartParseIntermediary(Response, rsResponse, outputTable);

				if (smartParsedResponses.size() > 0) {
					parsedResponse.setResultRows(new Vector<ParsedResponse>(smartParsedResponses));
					parsedResponse.setOnlyResponse(outputTable.toString());
					parsedResponse.setAttribute(ParsedResponse.SERVER_ROW_RESPONSE, outputTable.toString());
					
					// parse and store parameters on search
					Form form = new SimpleHtmlParser(rsResponse).getForm(FORM_NAME);
					Map<String, String> params = form.getParams();
					int seq = getSeq();
					mSearch.setAdditionalInfo(getCurrentServerName() + ":params:" + seq, params);
				}
				
			} catch (Exception e) {
				e.printStackTrace();
			}
			
			break;

		case ID_DETAILS:
			String details = "",
			pid = "",
			taxYear = "";
			
			details = getDetails(rsResponse);
			
			try {
				HtmlParser3 parser = new HtmlParser3(details);
				NodeList mainList = parser.getNodeList();
				pid = mainList.extractAllNodesThatMatch(new TagNameFilter("span"), true)
						.extractAllNodesThatMatch(new HasAttributeFilter("id", "ctl00_MainContent_SkeletonCtrl_4_lblParcel"))
						.elementAt(0).toPlainTextString().trim();
				
				taxYear = mainList.extractAllNodesThatMatch(new TagNameFilter("span"), true)
						.extractAllNodesThatMatch(new HasAttributeFilter("id", "ctl00_MainContent_SkeletonCtrl_4_lblTaxYear"))
						.elementAt(0).toPlainTextString().trim();
				
			} catch (Exception e) {
				e.printStackTrace();
			}

			if (!details.contains("TAX HISTORY:")) {
				details += addTaxHistoryInfoToDetailsPage(details);
			}
			
			if ((!downloadingForSave)) {

				String qry_aux = Response.getRawQuerry();
				qry_aux = "dummy=" + pid + "&" + qry_aux;
				String originalLink = sAction + "&" + qry_aux;
				String sSave2TSDLink = getLinkPrefix(TSConnectionURL.idGET)	+ originalLink;

				HashMap<String, String> data = new HashMap<String, String>();
				loadDataHash(data, taxYear);
				
				if (isInstrumentSaved(pid, null, data)) {
					details += CreateFileAlreadyInTSD();
				} else {
					mSearch.addInMemoryDoc(sSave2TSDLink, details);
					details = addSaveToTsdButton(details, sSave2TSDLink, viParseID);
				}

				Response.getParsedResponse().setPageLink( new LinkInPage(sSave2TSDLink, originalLink, TSServer.REQUEST_SAVE_TO_TSD) );
				parser.Parse(Response.getParsedResponse(), details, Parser.NO_PARSE);
				
			} else {
				Response.setResult(details);
				details = details.replaceAll("</?a[^>]*>", "");
				smartParseDetails(Response, details);
				msSaveToTSDFileName = pid + ".html";
				Response.getParsedResponse().setFileName(getServerTypeDirectory() + msSaveToTSDFileName);
				msSaveToTSDResponce = details + CreateFileAlreadyInTSD();
			}

			break;

		case ID_GET_LINK:
			if (sAction.indexOf("default") != -1) {
				ParseResponse(sAction, Response, ID_DETAILS);
			} else {
				ParseResponse(sAction, Response, ID_SEARCH_BY_NAME);
			}

			break;

		case ID_DETAILS1:
			setDoNotLogSearch(true);
			break;
			
		case ID_SAVE_TO_TSD:
			downloadingForSave = true;
			ParseResponse(sAction, Response, ID_DETAILS);
			downloadingForSave = false;
			break;
		}
	}

	private static HashMap<String, String> parseLegal(String input) {
		HashMap<String, String> map = new HashMap<String, String>();
		Matcher matcher;
//		// Parse deed book/page and date
//		matcher = Pattern.compile("Deed Bk:\\s*(\\d*)\\s*Pg:\\s*(\\d*)\\s*Date: (\\d{1,2}/\\d{1,2}/\\d{4})").matcher(input);
//		if (matcher.find()) {
//			map.put("deed_book", matcher.group(1));
//			map.put("deed_page", matcher.group(2));
//			map.put("deed_date", matcher.group(3));
//		}

		// Parse legal.
		String pdfAsString = input;
		pdfAsString = pdfAsString.replaceFirst("(?is).*(Property Address)", "$1");
		pdfAsString = pdfAsString.replaceFirst("(?is)Your portion of.*(Subdivision)", "$1");
		pdfAsString = pdfAsString.replaceAll("\\s*@\\s*", " at ");
		pdfAsString = pdfAsString.replaceFirst("Your payment options are:\\s*", "");
		pdfAsString = pdfAsString.replaceFirst("(?is)(?:-|\\u2022) By mail:[^\\r]+(Lot\\s*Acres\\s*EQ Factor)", "$1");
		pdfAsString = pdfAsString.replaceFirst("(?is)(?:-|\\u2022) At our office:[\\d\\sA-Z\\.;,]+TN", "");
		pdfAsString = pdfAsString.replaceFirst("(?is)(?:-|\\u2022) At participating local banks \\(see back for list of banks\\)", "");
		pdfAsString = pdfAsString.replaceFirst("(?is)(?:-|\\u2022) On-line with credit[^\\r]+", " Additional Description");
		pdfAsString = pdfAsString.replaceFirst("(?is)To avoid penalty and interest, taxes must be paid by[^\\.]+\\.", "");
		pdfAsString = pdfAsString.replaceFirst("(?is)\\s*(Penalty/Interest\\s*\\d\\.\\d{2}).*(Current[^\\$]+\\$[^\\.]+\\.\\d{2}).*(Total due[^\\$]+\\$[^\\.]+\\.\\d{2}).*","    $1  $2  $3");
		
		matcher = Pattern.compile("(?is).*Subdivision\\s*\\r(.*)Lot\\s*Acres\\s*EQ Factor\\s*\\r(.*?)Additional Description.*").matcher(pdfAsString);
		if (matcher.find()) {
			String fullSubdiv = matcher.group(1).trim();
			String blk = fullSubdiv;
			blk = blk.replaceFirst("(?is).*Bl(?:oc)?k\\s*([^\\s]+).*", "$1");
			if (blk.equals(fullSubdiv)) {
				blk = "";
			} else {
				fullSubdiv = fullSubdiv.replaceFirst("Bl(?:oc)?k\\s*([^\\s]+)", " ");
			}
			
			String sec = fullSubdiv;
			sec = sec.replaceFirst("(?is).*Sec(?:tion)?\\s*([A-Z\\d-]+).*", "$1");
			if (sec.equals(fullSubdiv)) {
				sec = "";
			} else {
				fullSubdiv = fullSubdiv.replaceFirst("Sec(?:tion)?\\s*([A-Z\\d-]+)", " ");
			}
			
			String subdiv = fullSubdiv;
			
			if (StringUtils.isNotEmpty(blk)) {
				map.put("blk", blk);
			}
			
			if (StringUtils.isNotEmpty(sec)) {
				map.put("subdivision_section", sec);
			}
			
			if (StringUtils.isNotEmpty(subdiv)) {
				map.put("subdivision", subdiv);
			}
			
			String lot = matcher.group(2).trim();
			if (lot.matches("(?is)(\\d+)\\s*([\\d\\s\\.]+)?")) {
				lot = lot.replaceFirst("(?is)(\\d+)\\s*([\\d\\s\\.]+)?", "$1");
				lot = lot.replaceFirst("^0+", "");
			}
			
			if (StringUtils.isNotEmpty(lot)) {
				map.put("lot", lot);
			}
			
		}

		return map;
	}

	private HashMap<String, String> getAndParsePdf(ResultMap map, String pdfUrl) throws Exception {
		HttpSite site = HttpManager.getSite(HashCountyToIndex.getDateSiteForMIServerID(getCommunityId(), miServerID).getName(), searchId);

		try {
			HTTPRequest reqP = new HTTPRequest(getDataSite().getServerHomeLink() + pdfUrl);
			HTTPResponse resP = null;

			resP = site.process(reqP);
			String rsp = PDFUtils.extractTextFromPDF(resP.getResponseAsStream(), true);

			if (StringUtils.isNotEmpty(rsp)) {
				return parseLegal(rsp);
			}
				
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			HttpManager.releaseSite(site);
		}
		return null;
	}

	public String extractLinkForPDFDownload(NodeList list, String idTypeOfDoc, String idLink) {
		String link = "";
		
		Node tagFotViewPDFDoc = list.elementAt(0).getChildren()
				.extractAllNodesThatMatch(new HasAttributeFilter("id", idTypeOfDoc), true).elementAt(0);
		
		if (tagFotViewPDFDoc != null) {
			// get PDF url
			LinkTag linkTagPDF = (LinkTag) tagFotViewPDFDoc.getChildren()
					.extractAllNodesThatMatch(new HasAttributeFilter("id", idLink), true).elementAt(0);
			
			if (linkTagPDF != null) {
				link = linkTagPDF.extractLink().trim();
			}
		}
		
		return link;
	}
	
	public String addPDFViewBillContent(String link) {
		String url = link;
		String content = "";
		
		if (StringUtils.isNotEmpty(url)) {
			// Get last tax receipt pdf and parse it.
			url = url.replaceAll("&amp;", "&");
			url = url.replaceFirst("/", "");
			
			try {
				HashMap<String, String> data = getAndParsePdf(null, url.replaceAll(" ", "%20"));

				if (data != null) {
					// Add this information to the html.
					String subdiv = org.apache.commons.lang.StringUtils.defaultString(data.get("subdivision"));
					String subdivSection = org.apache.commons.lang.StringUtils.defaultString(data.get("subdivision_section"));
					String lot = org.apache.commons.lang.StringUtils.defaultString(data.get("lot"));
					
					if (StringUtils.isNotEmpty(subdiv) || StringUtils.isNotEmpty(subdivSection) || StringUtils.isNotEmpty(lot)) {
					
						StringBuilder fromPdf = new StringBuilder(
								"<br/><br/><table id=\"legal\" cellspacing=\"0\" cellpadding=\"3\" rules=\"all\" border=\"1\"  style=\"border-collapse:collapse;width: 500px;\">\n");
						if (StringUtils.isNotEmpty(subdiv)) {
							fromPdf.append("<tr>\n<td><b>Subdivision: </b>" + subdiv);
							fromPdf.append("</td></tr>");
						}
						if (StringUtils.isNotEmpty(subdivSection)) {
							fromPdf.append("<tr><td><b>Section: </b>" + subdivSection);
							fromPdf.append("</td></tr>");
						}
						if (StringUtils.isNotEmpty(lot)) {
							fromPdf.append("<tr><td><b>Lot: </b>" + lot + "</td></tr>");
						}
						
						content = fromPdf.toString();
					}
					
				}
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		
		return content;
	}
	
	protected String addTaxHistoryInfoToDetailsPage (String contentOfDetailsPage) {
		String taxResponse = "";
		//Add Tax History information
		taxResponse += "</table> <table> <tr align=\"center\"><td colspan=\"3\"></br><b> TAX HISTORY: </b></td></tr>";
		
		HtmlParser3 htmlParser = new HtmlParser3(Tidy.tidyParse(contentOfDetailsPage, null));
		NodeList mainList = htmlParser.getNodeList();
		
		String taxYear = mainList.extractAllNodesThatMatch(new TagNameFilter("span"), true)
						.extractAllNodesThatMatch(new HasAttributeFilter("id", "ctl00_MainContent_SkeletonCtrl_4_lblTaxYear"))
						.elementAt(0).getFirstChild().toPlainTextString().trim();
		String pid = mainList.extractAllNodesThatMatch(new TagNameFilter("span"), true)
				.extractAllNodesThatMatch(new HasAttributeFilter("id", "ctl00_MainContent_SkeletonCtrl_4_lblParcel"))
				.elementAt(0).toPlainTextString().trim();
		
		int currentTaxYear = Integer.parseInt(taxYear); //current Tax Year
		String[] splitedPin;
		if ("Brentwood".equals(dataSite.getCityName())) {
			splitedPin = TNWilliamsonYB.splitAPN(pid);
		} else {
			splitedPin = splitPin(pid);
		}
				
		ServerResponse serverResponse = new ServerResponse();
		try {
			SearchDataWrapper searchDataWrapper = new SearchDataWrapper();
			TSServerInfoModule module = getCurrentClassServerInfo().getModuleForSearch(TSServerInfo.PARCEL_ID_MODULE_IDX, searchDataWrapper);
			module.setVisible(false);
				
			if (splitedPin != null) {
				for (int idx=0; idx < 6; idx++) {
					if (StringUtils.isNotEmpty(splitedPin[idx])) {
						module.setData(idx, splitedPin[idx]);
					}
				}
			}
				
			module.setData(9, "All");
				
			TSInterface tnWilliamsonTRServer = TSServersFactory.GetServerInstance( miServerID, searchId);
			tnWilliamsonTRServer.setDoNotLogSearch(true);
			serverResponse = tnWilliamsonTRServer.SearchBy(module, searchDataWrapper);

			if (serverResponse != null) {
				StringBuilder outputTable = new StringBuilder();
				String linkForDetails = "";
				Collection<ParsedResponse> intermResults = smartParseIntermediary(serverResponse, serverResponse.getResult(), outputTable);
				if (intermResults.size() > 0) {
					String year = "";
					for (ParsedResponse rez : intermResults) {
						LinkInPage linkObj = rez.getPageLink();
						if (linkObj != null) {
							linkForDetails = linkObj.getLink();
							int idx = linkForDetails.indexOf("TaxYear");
							year = linkForDetails.substring(idx + 8, idx + 12);
							int yearInterm = Integer.parseInt(year); //tax year from interm results
							
							if (yearInterm < currentTaxYear) {
								String sAction = super.GetRequestSettings(false, linkForDetails);
								serverResponse = performRequest(sAction, miGetLinkActionType, "GetLink", ID_DETAILS1, null, linkForDetails, null);
								 
								 if (StringUtils.isNotEmpty(serverResponse.getResult()) && !serverResponse.getResult().contains("No Records Found")) {
										taxResponse += "<tr> <td colspan=\"2\" align=\"center\"> Tax information for Year: " + year + "</td> </tr>";
										taxResponse += "<tr> <td colspan=\"2\" align=\"center\">";
										taxResponse += getDetailsForTaxHistory(serverResponse.getResult());
										taxResponse += "</td> </tr>";
								 }
							}
						}
					}
				}
			}
				
		} catch (Exception e) {
			e.printStackTrace();
		}
		
		taxResponse += "</table>";
		
		return taxResponse;
	}
	
	
	private String getDetailsForTaxHistory(String response) {
		NodeList mainList = new NodeList();
		String contents = "";
				
		try {
			HtmlParser3 htmlParser = new HtmlParser3(Tidy.tidyParse(response, null));
			mainList = htmlParser.getNodeList();
					
			mainList = mainList.extractAllNodesThatMatch(new HasAttributeFilter("class", "tab-content"), true);
					
			if (mainList.elementAt(0).getChildren().size() >= 9) {
				mainList.elementAt(0).getChildren().remove(9);
				mainList.elementAt(0).getChildren().remove(7);
			}
			
			Node viewPrintBill = mainList.elementAt(0).getChildren().extractAllNodesThatMatch(new HasAttributeFilter("id", "vViewBill"), true).elementAt(0);
			Node viewPrintReceipt = mainList.elementAt(0).getChildren().extractAllNodesThatMatch(new HasAttributeFilter("id", "vViewReceipt"), true).elementAt(0);
			
			mainList.elementAt(0).getChildren().remove(viewPrintBill);
			mainList.elementAt(0).getChildren().remove(viewPrintReceipt);
			
			mainList = mainList.extractAllNodesThatMatch(new TagNameFilter("table"), true)
					.extractAllNodesThatMatch(new HasAttributeFilter("class", "infoTable"), true);
			contents = mainList.elementAt(1).toHtml();

		} catch (Exception e) {
			e.printStackTrace();
		}

				
		contents = contents.replaceAll("(?is)</?body[^>]*>", "");
		contents = contents.replaceAll("(?is)</?form[^>]*>", "");
		contents = contents.replaceAll("(?is)(?is)<script.*?</script>", "");
		contents = contents.replaceAll("(?is)<input[^>]*>", "");
		contents = contents.replaceAll("(?is)<img[^>]*>", "");
		contents = contents.replaceAll("(?is)<div[^>]*>\\s*</div>", "");
		contents = contents.replaceAll("(?is)(<select)\\s+", "$1 disabled=\"true\"");
		contents = contents.replaceAll("(?is)</?a[^>]*>", "");
		contents += "<br> <br>";
				
		return contents.trim();
	}

	protected String getDetails(String response) {
		// if from memory - use it as is
		if (!response.toLowerCase().contains("<html")) {
			return response;
		}
		
		NodeList mainList = new NodeList();
		String contents = "";
		String pin = "";
		String taxEntity = "";
		
		try {
			HtmlParser3 htmlParser = new HtmlParser3(Tidy.tidyParse(response, null));
			mainList = htmlParser.getNodeList();
			
			mainList = mainList.extractAllNodesThatMatch(new HasAttributeFilter("class", "tab-content"), true);
			
			if (mainList.elementAt(0).getChildren().size() >= 9) {
				mainList.elementAt(0).getChildren().remove(9);
				mainList.elementAt(0).getChildren().remove(7);
			}
			
			
			pin = mainList.extractAllNodesThatMatch(new TagNameFilter("span"), true)
					.extractAllNodesThatMatch(new HasAttributeFilter("id", "ctl00_MainContent_SkeletonCtrl_4_lblParcel"))
					.elementAt(0).getFirstChild().toPlainTextString().trim();
			
			taxEntity = mainList.extractAllNodesThatMatch(new TagNameFilter("span"), true)
					.extractAllNodesThatMatch(new HasAttributeFilter("id", "ctl00_MainContent_SkeletonCtrl_4_lblTaxSource"))
					.elementAt(0).getFirstChild().toPlainTextString().trim();
			
		} catch (Exception e) {
			e.printStackTrace();
		}

		String pdfUrl1 = "";
		String pdfUrl2 = "";
		
		pdfUrl1 = extractLinkForPDFDownload(mainList, "vViewBill", "ctl00_MainContent_SkeletonCtrl_4_linkSaveBill");
		pdfUrl2 = extractLinkForPDFDownload(mainList, "vViewReceipt", "ctl00_MainContent_SkeletonCtrl_4_linkSaveReceipt");
		
		Node viewPrintBill = mainList.elementAt(0).getChildren().extractAllNodesThatMatch(new HasAttributeFilter("id", "vViewBill"), true).elementAt(0);
		Node viewPrintReceipt = mainList.elementAt(0).getChildren().extractAllNodesThatMatch(new HasAttributeFilter("id", "vViewReceipt"), true).elementAt(0);
		
		mainList.elementAt(0).getChildren().remove(viewPrintBill);
		mainList.elementAt(0).getChildren().remove(viewPrintReceipt);
		contents = mainList.elementAt(0).toHtml();

		contents += addPDFViewBillContent(pdfUrl1); // add content from View Bill pdf
		
		contents = contents.replaceAll("(?is)</?body[^>]*>", "");
		contents = contents.replaceAll("(?is)</?form[^>]*>", "");
		contents = contents.replaceAll("(?is)(?is)<script.*?</script>", "");
		contents = contents.replaceAll("(?is)<input[^>]*>", "");
		contents = contents.replaceAll("(?is)<img[^>]*>", "");
		contents = contents.replaceAll("(?is)<div[^>]*>\\s*</div>", "");
		contents = contents.replaceAll("(?is)(<select)\\s+", "$1 disabled=\"true\"");
		contents = contents.replaceAll("(?is)</?a[^>]*>", "");
		contents += "<br> <br>";
		
		return contents.trim();
	}

	
	public static String[] splitPin(String pin) {
		String parts[] = new String[6];
		for (int i = 0; i < parts.length; i++)
			parts[i] = "";

		if (StringUtils.isEmpty(pin))
			return parts;
		
		if (StringUtils.isNotEmpty(pin)) {
			pin = pin.replaceAll("-", " ");
			String ctrl1 = "", ctrl2 = "", group = "", parcel = "", id = "", si = "";
			
			String regExp = "(?is)(.*\\d+\\.\\d+)([A-Z])(\\d+)";
			Matcher m = Pattern.compile(regExp).matcher(pin);
			if (m.find()) {
				// 108 17.00P1 or 62 P C 30.00P1
				pin = pin.replaceFirst("(?is)(\\d+)([A-Z])\\s([A-Z])", "$1 $2 $3");
			}
			
			regExp = "(?is)(\\d+)\\s*([A-Z])\\s*([A-Z])\\s*(\\d{1,3}\\.\\d{2})\\s*([A-Z])\\s*(\\d{1,3})";
			m = Pattern.compile(regExp).matcher(pin);
			if (m.matches()) {
				//cases like: 62 P C 30.00P1
				ctrl1 = m.group(1);
				ctrl2 = m.group(2);
				group = m.group(3);
				parcel = m.group(4);
				id = m.group(5);
				si = m.group(6).trim();

			} else {
				regExp = "(?is)(\\d{1,3})\\s*(\\d{1,3}\\.\\d{2})\\s*([A-Z])\\s*(\\d{1,3})";
				m = Pattern.compile(regExp).matcher(pin);
				if (m.matches()) {
					ctrl1 = m.group(1);
					parcel = m.group(2);
					id = m.group(3);
					si =  m.group(4).trim();
				
				} else {
					regExp = "(?is)(\\d{1,3})\\s*([A-Z])\\s*([A-Z])\\s*(\\d{1,3}\\.\\d{2})\\s*(\\d{1,3})";
					m = Pattern.compile(regExp).matcher(pin);
					if (m.matches()) {
						ctrl1 = m.group(1);
						ctrl2 = m.group(2);
						group = m.group(3);
						parcel = m.group(4);
						si =  m.group(5).trim();
					} else {
						regExp = "(?is)(\\d{1,3})\\s*([A-Z])\\s*([A-Z])\\s*(\\d{1,3}\\.\\d{2})";
						m = Pattern.compile(regExp).matcher(pin);
						if (m.matches()) {
							// cases like 012 E C 008.00
							ctrl1 = m.group(1);
							ctrl2 = m.group(2);
							group = m.group(3);
							parcel = m.group(4);
						} else {
							regExp = "(?is)(\\d{1,3})\\s*(\\d{1,3}\\.\\d{2})\\s*(\\d{1,3})";
							m = Pattern.compile(regExp).matcher(pin);
							if (m.matches()) {
								ctrl1 = m.group(1);
								parcel = m.group(2);
								si =  m.group(3).trim();
							} else {
								regExp = "(?is)(\\d{3})\\s*(\\d{1,3}\\.\\d{2})";
								m = Pattern.compile(regExp).matcher(pin);
								if (m.matches()) {
									ctrl1 = m.group(1);
									parcel = m.group(2);
								}
							}
						}
					}
				}
			}
			
			if (StringUtils.isNotEmpty(ctrl1)) {
				parts[0] = ctrl1;				
			}
			if (StringUtils.isNotEmpty(ctrl2)) {
				parts[1] = ctrl2;			
			}
			if (StringUtils.isNotEmpty(group)) {
				parts[2] = group;				
			}
			if (StringUtils.isNotEmpty(parcel)) {
				parts[3] = parcel;				
			}
			if (StringUtils.isNotEmpty(id) && !"0".equals(id)) {
				parts[4] = id;				
			}
			if (StringUtils.isNotEmpty(si) && !("0".equals(si) || "000".equals(si))) {
				parts[5] = si;				
			}
		} 
		
		return parts;
	}
	

	protected void putSrcType(ResultMap map) {
		map.put(OtherInformationSetKey.SRC_TYPE.getKeyName(), getDataSite().getSiteTypeAbrev());
	}
	
	public Collection<ParsedResponse> smartParseIntermediary(ServerResponse response, String table, StringBuilder outputTable) {
		Vector<ParsedResponse> intermediaryResponse = new Vector<ParsedResponse>();
		
		try {						
			Map<String, String> params = null;			 			
			HttpSite site = HttpManager.getSite(getCurrentServerName(), searchId);
			if (site != null) {
				try {
					params = HttpSite.fillConnectionParams(table,((ro.cst.tsearch.connection.http2.TNWilliamsonTR) site)
							.getTargetArgumentParameters(), FORM_NAME);
					
				} finally {
					// always release the HttpSite
					HttpManager.releaseSite(site);
				}
			}

			table = table.replaceAll("(?is)(\\s*<td[^>]+>\\s*<input[^>]+>\\s*)(</td>)", "$1" + "<p> </p>" + "$2");
			
			HtmlParser3 htmlParser = new HtmlParser3(table);
			NodeList nodeList = htmlParser.getNodeList();
			
			FormTag form = (FormTag) nodeList.extractAllNodesThatMatch(new TagNameFilter("form"), true)
					.extractAllNodesThatMatch(new HasAttributeFilter("id", FORM_NAME)).elementAt(0);
			form.setAttribute("name", FORM_NAME);
			String action = form.getFormLocation();	
			
			nodeList = htmlParser.getNodeList().extractAllNodesThatMatch(new TagNameFilter("table"), true)
					.extractAllNodesThatMatch(new HasAttributeFilter("id", "ctl00_MainContent_SkeletonCtrl_4_gvRecords"), true);
						
			if(nodeList.size() == 0) {
				return intermediaryResponse;
			}
			
			TableTag tableTag = (TableTag) nodeList.elementAt(0);
			tableTag.setAttribute("border", "1");
			
			TableRow[] rows  = tableTag.getRows();
			int seq = getSeq();
			
			for (int i = 1; i < rows.length; i++) {
				TableRow row = rows[i];
				if(row.getColumnCount() == 8) {
					ParsedResponse currentResponse = new ParsedResponse();					
					TableColumn col = row.getColumns()[2];
					String taxYear = col.getChildrenHTML().trim();
					
					col = row.getColumns()[7];
					if (col != null) {
						InputTag linkTag = ((InputTag)col.getChildren().extractAllNodesThatMatch(new TagNameFilter("input")).elementAt(0));
						String newParam = linkTag.getAttribute("name").trim();
						String newParamValue = linkTag.getAttribute("value").trim();
						
						mSearch.setAdditionalInfo(getCurrentServerName() + ":params:" + seq, params);
						
						String link = (CreatePartialLink(TSConnectionURL.idPOST) + action
								+ "?" + newParam + "=" + newParamValue + "&TaxYear=" + taxYear + "&seq=" + seq)
								.replaceAll("\\s", "%20");
						
						LinkTag linkRow = new LinkTag();
						linkRow.setLink(link);
					
						String tmp = linkRow.toHtml() + "View" + "</A>";
					
						col.getChild(0).setText(tmp);
						col.removeChild(1);
					
						col = row.getColumns()[6];
						col.removeChild(1);
						
						currentResponse.setAttribute(ParsedResponse.SERVER_ROW_RESPONSE, row.toHtml());
						currentResponse.setOnlyResponse(row.toHtml());
						currentResponse.setPageLink(new LinkInPage(link,link,TSServer.REQUEST_SAVE_TO_TSD));
					}
					
					ResultMap map = parseIntermediaryRow(row, searchId);
					putSrcType(map);
					
					Bridge bridge = new Bridge(currentResponse, map, searchId);
					
					DocumentI document = (TaxDocumentI)bridge.importData();				
					currentResponse.setDocument(document);
					
					intermediaryResponse.add(currentResponse);					
				}
			}
			
			response.getParsedResponse().setHeader("<table border=\"1\" cellspacing=\"0\" cellpadding=\"8\">\n" + 
					"<tr> <th>Name</th>		<th>Entity</th> 	<th>Tax Year</th> 	<th>Receipt</th> " +
					"	  <th>Last Pmt</th> <th>Tax Amount</th> <th>Status</th> 	<th>Select</th> </tr>");			
			response.getParsedResponse().setFooter("</table>");
			
			outputTable.append(table);
						
		} catch (Throwable t){
			logger.error("Error while parsing intermediary data", t);
		}
		
		return intermediaryResponse;
	
	}
	
	public static ResultMap parseIntermediaryRow(TableRow row, long searchId) {
		ResultMap resultMap = new ResultMap();
		
		TableColumn[] cols = row.getColumns();
		if(cols.length == 8) {
			String owner	 	= cols[0].toPlainTextString().trim();
			String taxYear		= cols[2].toPlainTextString().trim();
			String baseAmount 	= cols[5].toPlainTextString().trim();
			
			if (StringUtils.isNotEmpty(owner)) {
				resultMap.put(PropertyIdentificationSetKey.NAME_ON_SERVER.getKeyName(), owner);
				try {
					parseNamesIntermediary (resultMap, searchId);
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
			
			if (StringUtils.isNotEmpty(taxYear)) {
				resultMap.put(TaxHistorySetKey.YEAR.getKeyName(), taxYear);
			}
			
			if (StringUtils.isNotEmpty(baseAmount)) {
				resultMap.put(TaxHistorySetKey.BASE_AMOUNT.getKeyName(), baseAmount);
			}
		}
		
		return resultMap;
	}
	
	private static void parseNamesIntermediary(ResultMap resultMap,	long searchId) {
		   List<List> body = new ArrayList<List>();
		   String owner = (String) resultMap.get(PropertyIdentificationSetKey.NAME_ON_SERVER.getKeyName());
		   owner = owner.replaceFirst("(?is)\\s*Trustee-Trustor\\s*", " Trustee ");
		   
		   if (StringUtils.isEmpty(owner))
			   return;
		   
		   else {
				String[] names = null;
				owner = owner.replaceAll("\\s*/\\s*", " & ");
				names = StringFormats.parseNameWilliamson(owner);
						
				String[] types = GenericFunctions.extractAllNamesType(names);
				String[] otherTypes = GenericFunctions.extractAllNamesOtherType(names);
				String[] suffixes = GenericFunctions.extractAllNamesSufixes(names);
							
				GenericFunctions.addOwnerNames(names, suffixes[0], suffixes[1], types, otherTypes, 
						NameUtils.isCompany(names[2]), NameUtils.isCompany(names[5]), body);
			}
		   
		   try {
				GenericFunctions.storeOwnerInPartyNames(resultMap, body, true);
				
			} catch (Exception e) {
				e.printStackTrace();
			}
	}

	public static void parseNamesTNWilliamsonTR(ResultMap m, long searchId) throws Exception {
		String s = "";
		s = (String) m.get(PropertyIdentificationSetKey.NAME_ON_SERVER.getKeyName());

		if (StringUtils.isEmpty(s))
			return;

		s = s.replaceAll("&amp;", "&");
		s = s.replaceAll("\\bETA\\b", "ETAL");
		s = s.replaceAll("\\bMRS\\b", "");
		s = s.replaceAll("[\\(\\)]+", "");
		s = s.replaceAll("\\bATT(ENTIO)?N:?\\s*:?\\b", "");
		s = s.replaceAll("\\s+\\(LE\\)\\s+", " ");
		s = s.replaceAll("(?is)\\A\\s*<\\s*br\\s*/?\\s*>", "");
		s = s.replaceAll("(?is)\\s*<\\s*br\\s*/?\\s*>\\s*(ETVIR|ETAL|ETUX)\\b", " $1");
		s = s.replaceAll("(?is)\\b(ETVIR|ETUX)\\s*<\\s*br\\s*/?\\s*>\\s*", "$1 ");
		s = s.replaceAll("(?is)E\\s*<\\s*br\\s*/?\\s*>\\s*", "ETUX ");
		s = s.replaceAll("(?is)\\b\\s*LE\\s*\\b", " LIFE ESTATE ");

		String[] owners;
		owners = s.split(",");

		@SuppressWarnings("rawtypes")
		List<List> body = new ArrayList<List>();
		String[] names = { "", "", "", "", "", "" };
		String[] suffixes, type, otherType;

		for (int i = 0; i < owners.length; i++) {
			String ow = owners[i];
			ow = ow.replaceAll("U/D/T", "");
			if (NameUtils.isCompany(ow)) {
				names[2] = ow;
			} else {
				names = StringFormats.parseNameWilliamson(ow, true);
			}

			type = GenericFunctions.extractAllNamesType(names);
			otherType = GenericFunctions.extractAllNamesOtherType(names);
			suffixes = GenericFunctions1.extractNameSuffixes(names);

			if (ow.matches(".*" + names[2] + " I")) { // WILLIAM L PATTERSON I
				names[1] = names[1].replaceAll("(.*)I\\z", "$1").trim();
				suffixes[0] = "I";
			}

			GenericFunctions1.addOwnerNames(names, suffixes[0], suffixes[1],
					type, otherType, NameUtils.isCompany(names[2]),
					NameUtils.isCompany(names[5]), body);
		}
		GenericFunctions1.storeOwnerInPartyNames(m, body, true);
	}
	
	
	@Override
	protected Object parseAndFillResultMap(ServerResponse response, String detailsHtml, ResultMap map) {
		detailsHtml = detailsHtml.replaceAll("(?is)&nbsp;", "");
		putSrcType(map);
		
		try {
			org.htmlparser.Parser htmlParser = org.htmlparser.Parser.createParser(detailsHtml, null);
			NodeList mainList = htmlParser.parse(null);
			String pin = mainList.extractAllNodesThatMatch(new TagNameFilter("span"), true)
					.extractAllNodesThatMatch(new HasAttributeFilter("id","ctl00_MainContent_SkeletonCtrl_4_lblParcel"))
					.elementAt(0).toPlainTextString().trim();
			
			if (StringUtils.isNotEmpty(pin)) {
				pin = pin.replaceAll("(?is)(\\d+\\.\\d+)([A-Z])(\\d+)", "$1 $2 $3");// 52 H E 18.00P1
				map.put(PropertyIdentificationSetKey.PARCEL_ID.getKeyName(), pin);
			}
			
			String address = mainList.extractAllNodesThatMatch(new TagNameFilter("span"), true)
					.extractAllNodesThatMatch(new HasAttributeFilter("id", "ctl00_MainContent_SkeletonCtrl_4_lblPropertyAddress"))
					.elementAt(0).toPlainTextString().trim();
			
			if (StringUtils.isNotEmpty(address)) {
				String regExp = "(?is)([A-Z\\s'-\\.]+)(\\d+)";
				if (address.matches(regExp)) {
					address = address.replaceFirst(regExp, "$2 " + "$1");
				}
				map.put(PropertyIdentificationSetKey.STREET_NO.getKeyName(), StringFormats.StreetNo(address));
				map.put(PropertyIdentificationSetKey.STREET_NAME.getKeyName(), StringFormats.StreetName(address));
			}
			
			String ownerName = mainList.extractAllNodesThatMatch(new TagNameFilter("span"), true)
					.extractAllNodesThatMatch(new HasAttributeFilter("id", "ctl00_MainContent_SkeletonCtrl_4_lblOwnerName"))
					.elementAt(0).getChildren().toHtml().replaceAll("<br\\s*/>", " & ").trim();
			
			if (StringUtils.isNotEmpty(ownerName)) {
				map.put(PropertyIdentificationSetKey.NAME_ON_SERVER.getKeyName(), ownerName);
			}
			
			String taxYear = mainList.extractAllNodesThatMatch(new TagNameFilter("span"), true)
					.extractAllNodesThatMatch(new HasAttributeFilter("id", "ctl00_MainContent_SkeletonCtrl_4_lblTaxYear"))
					.elementAt(0).toPlainTextString().trim();
			
			if (StringUtils.isNotEmpty(taxYear)) {
				map.put(TaxHistorySetKey.YEAR.getKeyName(), taxYear);
			}
			
			try {
				String city = mainList.extractAllNodesThatMatch(new TagNameFilter("span"), true)
						.extractAllNodesThatMatch(new HasAttributeFilter("id", "ctl00_MainContent_SkeletonCtrl_4_lblTaxSource"))
						.elementAt(0).toPlainTextString().trim();
				city = city.replaceFirst("(?is)\\s+History\\s*$", "");
				
				if (StringUtils.isNotEmpty(city) && !"williamson".equals(city.toLowerCase())) {
					map.put(PropertyIdentificationSetKey.CITY.getKeyName(), city);
				}
				
			} catch (Exception e) {
				e.printStackTrace();
			}
			
			String baseAmount = mainList.extractAllNodesThatMatch(new TagNameFilter("span"), true)
					.extractAllNodesThatMatch(new HasAttributeFilter("id", "ctl00_MainContent_SkeletonCtrl_4_lblBaseAmtDue"))
					.elementAt(0).toPlainTextString().trim();
			
			if (StringUtils.isNotEmpty(baseAmount)) {
				baseAmount = baseAmount.replaceAll("(?is)[\\$,]+", "");
				map.put(TaxHistorySetKey.BASE_AMOUNT.getKeyName(), baseAmount);
			}

			String amountPaid = mainList.extractAllNodesThatMatch(new TagNameFilter("span"), true)
					.extractAllNodesThatMatch(new HasAttributeFilter("id", "ctl00_MainContent_SkeletonCtrl_4_lblPaidAmount"))
					.elementAt(0).toPlainTextString().trim();
			
			if (StringUtils.isNotEmpty(amountPaid)) {
				amountPaid = amountPaid.replaceAll("(?is)[\\$,]+", "");
				map.put(TaxHistorySetKey.AMOUNT_PAID.getKeyName(), amountPaid);
			}
			
			String receiptDate = mainList.extractAllNodesThatMatch(new TagNameFilter("span"), true)
					.extractAllNodesThatMatch(new HasAttributeFilter("id", "ctl00_MainContent_SkeletonCtrl_4_lblPaidDate"))
					.elementAt(0).toPlainTextString().trim();
			
			if (StringUtils.isNotEmpty(receiptDate)) {
				//map.put(TaxHistorySetKey.DATE_PAID.getKeyName(), paidDate);
				
				if (StringUtils.isNotEmpty(amountPaid)) {
					try {
						ResultTable receipts = new ResultTable();
						Map<String, String[]> tmpMap = new HashMap<String, String[]>();
						String[] header = { "ReceiptAmount", "ReceiptDate" };
						List<List<String>> bodyRT = new ArrayList<List<String>>();
						List<String> paymentRow = new ArrayList<String>();
						
						paymentRow.add(amountPaid);
						paymentRow.add(receiptDate);
						bodyRT.add(paymentRow);
					
						tmpMap.put("ReceiptAmount", new String[] { "ReceiptAmount", "" });
						tmpMap.put("ReceiptDate", new String[] { "ReceiptDate", "" });
						receipts.setHead(header);
						receipts.setMap(tmpMap);
						receipts.setBody(bodyRT);
						receipts.setReadOnly();
						map.put("TaxHistorySet", receipts);
					
					} catch (Exception e) {
						e.printStackTrace();
					}
				}
			}
			
			String totalDue = mainList.extractAllNodesThatMatch(new TagNameFilter("span"), true)
					.extractAllNodesThatMatch(new HasAttributeFilter("id", "ctl00_MainContent_SkeletonCtrl_4_lblBalanceDue"))
					.elementAt(0).toPlainTextString().trim();
			
			if (StringUtils.isNotEmpty(totalDue)) {
				totalDue = totalDue.replaceAll("(?is)[\\$,]+", "");
				if (!totalDue.contains("No longer collected by the Trustee")) {
					map.put(TaxHistorySetKey.TOTAL_DUE.getKeyName(), totalDue);
				} else {
					if (StringUtils.isNotEmpty(baseAmount)) {
						map.put(TaxHistorySetKey.TOTAL_DUE.getKeyName(), "0.00");
					}
				}
			}
			
			String baseAmountCity = mainList.extractAllNodesThatMatch(new TagNameFilter("span"), true)
					.extractAllNodesThatMatch(new HasAttributeFilter("id", "ctl00_MainContent_SkeletonCtrl_4_lblOtherTaxes"))
					.elementAt(0).toPlainTextString().trim();
			
			if (StringUtils.isNotEmpty(baseAmountCity)) {
				baseAmountCity = baseAmountCity.replaceAll("(?is)[\\$,]+", "");
				map.put(TaxHistorySetKey.BASE_AMOUNT_EP.getKeyName(), baseAmountCity);
			}

			// Parse legal
				TableTag legalTable = (TableTag) mainList.extractAllNodesThatMatch(new HasAttributeFilter("id", "legal")).elementAt(0);
				
				if (legalTable != null) {
					TableRow[] rows = legalTable.getRows();
					for (TableRow row : rows) {
						String text = row.getColumns()[0].getChildrenHTML().trim();
						if (text.contains("Subdivision")) {
							text = text.replaceFirst("(?is)[^/]+/b>\\s*(.*)", "$1");
							if (text.contains("Ph") || text.contains("Phase")) {
								String phase = text;
								phase = phase.replaceFirst("(?is).*\\bPh(?:ase)?\\b\\s*(\\d+).*", "$1");
								text = text.replaceFirst("(?is)\\s*\\bPh(?:ase)?\\b\\s*\\d+", "");
								if (StringUtils.isNotEmpty(phase)) {
									map.put(PropertyIdentificationSetKey.SUBDIVISION_PHASE.getKeyName(), phase);
								}
							}
							if (StringUtils.isNotEmpty(text)) {
								map.put(PropertyIdentificationSetKey.SUBDIVISION_NAME.getKeyName(), text);
							}
							
						} else if (text.contains("Section")) {
							text = text.replaceFirst("(?is)[^/]+/b>\\s*(.*)", "$1");
							if (StringUtils.isNotEmpty(text)) {
								map.put(PropertyIdentificationSetKey.SUBDIVISION_SECTION.getKeyName(), text);
							}
							
						} else if (text.contains("Lot")) {
							text = text.replaceFirst("(?is)[^/]+/b>\\s*(.*)", "$1");
							if (StringUtils.isNotEmpty(text)) {
								map.put(PropertyIdentificationSetKey.SUBDIVISION_LOT_NUMBER.getKeyName(), text);
							}

						} else if (text.contains("Block")) {
							text = text.replaceFirst("(?is)[^/]+/b>\\s*(.*)", "$1");
							if (StringUtils.isNotEmpty(text)) {
								map.put(PropertyIdentificationSetKey.SUBDIVISION_BLOCK.getKeyName(), text);
							}
						}
					}
				}
		} catch (Exception e) {
			e.printStackTrace();
		}
		try {
			parseNamesTNWilliamsonTR(map, searchId);
		} catch (Exception e) {
			e.printStackTrace();
		}
		return null;
	}

	/**
	 * get file name from link
	 */
	@Override
	protected String getFileNameFromLink(String link) {
		String fileName = "File.html";
		Matcher dummyMatcher = dummyPattern.matcher(link);
		if (dummyMatcher.find()) {
			fileName = dummyMatcher.group(1);
			fileName = fileName + ".html";
		}
		return fileName;
	}

}