package ro.cst.tsearch.servers.types;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Vector;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.htmlparser.filters.HasAttributeFilter;
import org.htmlparser.filters.TagNameFilter;
import org.htmlparser.nodes.TagNode;
import org.htmlparser.tags.InputTag;
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
import ro.cst.tsearch.search.FunctionStatesIterator;
import ro.cst.tsearch.search.filter.FilterResponse;
import ro.cst.tsearch.search.filter.TaxYearFilterResponse;
import ro.cst.tsearch.search.filter.newfilters.address.AddressFilterFactory;
import ro.cst.tsearch.search.filter.newfilters.address.GenericAddressFilter;
import ro.cst.tsearch.search.filter.newfilters.name.GenericNameFilter;
import ro.cst.tsearch.search.filter.newfilters.name.NameFilterFactory;
import ro.cst.tsearch.search.iterator.ModuleStatesIteratorFactory;
import ro.cst.tsearch.search.module.ConfigurableNameIterator;
import ro.cst.tsearch.search.name.NameUtils;
import ro.cst.tsearch.servers.HashCountyToIndex;
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

public class TNWilliamsonYC extends TSServerAssessorandTaxLike {

	public static final long serialVersionUID = 10000000L;
	private boolean downloadingForSave;

	private static final Pattern dummyPattern = Pattern
			.compile("&dummy=([0-9]+)&");

	public TNWilliamsonYC(long searchId) {
		super(searchId);
	}

	public TNWilliamsonYC(String rsRequestSolverName, String rsSitePath,
			String rsServerID, String rsPrmNameLink, long searchId, int mid) {
		super(rsRequestSolverName, rsSitePath, rsServerID, rsPrmNameLink,
				searchId, mid);
	}

	@Override
	public ServerResponse SearchBy(TSServerInfoModule module, Object sd)
			throws ServerResponseException {

		Search global = InstanceManager.getManager().getCurrentInstance(searchId).getCrtSearchContext();
		int searchType = global.getSearchType();
		
		if ((module.getModuleIdx() == TSServerInfo.PARCEL_ID_MODULE_IDX) && (searchType == Search.AUTOMATIC_SEARCH)) {
			String pin = getSearch().getSa().getAtribute(SearchAttributes.LD_PARCELNO);
//			pin = pin.replaceAll("\\p{Punct}", "").replaceAll("\\s+", "");
			String[] pidParts = splitPin(pin);
			module.getFunction(0).setParamValue(pidParts[0]);  //ctrl1
			module.getFunction(1).setParamValue(pidParts[1]);  //ctrl2
			module.getFunction(2).setParamValue(pidParts[2]);  //group
			module.getFunction(3).setParamValue(pidParts[3]);  //parcel
			module.getFunction(4).setParamValue(pidParts[4]);  //id
			module.getFunction(5).setParamValue(pidParts[5]);  //si
		}
		return super.SearchBy(module, sd);
	}

	@Override
	protected void setModulesForAutoSearch(TSServerInfo serverInfo) {
		List<TSServerInfoModule> modules = new ArrayList<TSServerInfoModule>();
		TSServerInfoModule module = null;

		String address = getSearch().getSa().getAtribute(SearchAttributes.P_STREET_NA_SU_NO);
		GenericAddressFilter addressFilter = AddressFilterFactory.getGenericAddressHighPassFilter(searchId, 0.8d);
		FilterResponse defaultNameFilter = NameFilterFactory.getDefaultNameFilter(SearchAttributes.OWNER_OBJECT, searchId, module);
		((GenericNameFilter) defaultNameFilter).setIgnoreMiddleOnEmpty(true);
		
		FilterResponse taxYearFilter = new TaxYearFilterResponse(searchId, true, numberOfYearsAllowed, true);

		String city = getSearchAttribute(SearchAttributes.P_CITY).toUpperCase();
		
		if(StringUtils.isEmpty(city)) {
			return;
		} else {
			if (!city.startsWith(getDataSite().getCityName().toUpperCase())) {
				return;
			}
		}
		
		
		if (hasPin()) {
			// Search by PIN
			module = new TSServerInfoModule(serverInfo.getModule(TSServerInfo.PARCEL_ID_MODULE_IDX));
			module.clearSaKeys();
			module.getFunction(0).forceValue(getSearch().getSa().getAtribute(SearchAttributes.LD_TN_WILLIAMSON_YC_Ctl1));
			module.getFunction(1).forceValue(getSearch().getSa().getAtribute(SearchAttributes.LD_TN_WILLIAMSON_YC_Ctl2));
			module.getFunction(2).forceValue(getSearch().getSa().getAtribute(SearchAttributes.LD_TN_WILLIAMSON_YC_Group));
			module.getFunction(3).forceValue(getSearch().getSa().getAtribute(SearchAttributes.LD_TN_WILLIAMSON_YC_Parcel));
			module.getFunction(4).forceValue(getSearch().getSa().getAtribute(SearchAttributes.LD_TN_WILLIAMSON_YC_Id));
			module.getFunction(5).forceValue(getSearch().getSa().getAtribute(SearchAttributes.LD_TN_WILLIAMSON_YC_Si));

			module.addFilter(taxYearFilter);
			modules.add(module);

		}

		if (hasStreet()) {
			// Search by Property Address
			module = new TSServerInfoModule(serverInfo.getModule(TSServerInfo.ADDRESS_MODULE_IDX));
			module.clearSaKeys();
			module.getFunction(0).forceValue(address);
			module.addFilter(addressFilter);
			module.addFilter(defaultNameFilter);
			module.addFilter(taxYearFilter);
			modules.add(module);
		}

		if (hasOwner()) {
			// Search by Owner
			module = new TSServerInfoModule(serverInfo.getModule(TSServerInfo.NAME_MODULE_IDX));
			module.setSaObjKey(SearchAttributes.OWNER_OBJECT);
			module.clearSaKeys();
//			module.getFunction(4).forceValue("All");
			module.addFilter(addressFilter);
			module.addFilter(defaultNameFilter);
			module.addFilter(taxYearFilter);
			module.setIteratorType(0, FunctionStatesIterator.ITERATOR_TYPE_LF_NAME_FAKE);
			ConfigurableNameIterator nameIterator = (ConfigurableNameIterator) ModuleStatesIteratorFactory
					.getConfigurableNameIterator(module, searchId, new String[] { "LFM;;", "L;F;" });
			module.addIterator(nameIterator);
			modules.add(module);
		}

		serverInfo.setModulesForAutoSearch(modules);
	}

	protected void loadDataHash(HashMap<String, String> data, String taxYear) {
		if (data != null) {
			data.put("type", "CITYTAX");
			data.put("year", taxYear);
		}
	}
	
	@Override
	protected void ParseResponse(String sAction, ServerResponse Response,
			int viParseID) throws ServerResponseException {

		String initialResponse = Response.getResult();
		String rsResponse = initialResponse;
		String contents = "";
		ParsedResponse parsedResponse = Response.getParsedResponse();

		switch (viParseID) {

		case ID_SEARCH_BY_NAME:
		case ID_SEARCH_BY_ADDRESS:
		case ID_SEARCH_BY_PARCEL:

			StringBuilder outputTable = new StringBuilder();

			if (rsResponse.indexOf("No Records Found") != -1) {
				Response.getParsedResponse().setError("<font color=\"red\">No results found!</font>");
				return;
			}
			try {
				org.htmlparser.Parser htmlParser = org.htmlparser.Parser.createParser(rsResponse, null);
				NodeList mainList = htmlParser.parse(null);
				contents = HtmlParser3.getNodeByID("ctl00_cphMainContent_SkeletonCtrl_10_gvRecords", mainList, true)
						.toHtml();
			} catch (Exception e) {
				e.printStackTrace();
				Response.getParsedResponse().setError("<font color=\"red\">Server error! No results found!</font>");
				return;
			}

			try {
				Collection<ParsedResponse> smartParsedResponses = smartParseIntermediary(Response, contents, outputTable);

				if (smartParsedResponses.size() > 0) {
					parsedResponse.setResultRows(new Vector<ParsedResponse>(
							smartParsedResponses));
					parsedResponse.setOnlyResponse(outputTable.toString());
					parsedResponse.setAttribute(
							ParsedResponse.SERVER_ROW_RESPONSE,
							outputTable.toString());
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
				org.htmlparser.Parser htmlParser = org.htmlparser.Parser.createParser(details, null);
				NodeList mainList = htmlParser.parse(null);
				
				pid = mainList.extractAllNodesThatMatch(new TagNameFilter("span"),true)
						.extractAllNodesThatMatch(new HasAttributeFilter("id", "ctl00_cphMainContent_SkeletonCtrl_10_tbcTaxes_tbOverview_lblParcel"))
						.elementAt(0).toPlainTextString().trim();
				
				taxYear = mainList
						.extractAllNodesThatMatch(new TagNameFilter("span"), true)
						.extractAllNodesThatMatch(new HasAttributeFilter("id", "ctl00_cphMainContent_SkeletonCtrl_10_tbcTaxes_tbOverview_lblTaxYear"))
						.elementAt(0).toPlainTextString().trim();
				
			} catch (Exception e) {
				e.printStackTrace();
			}

			if ((!downloadingForSave)) {

				String qry_aux = Response.getRawQuerry();
				qry_aux = "dummy=" + pid + "&" + qry_aux;
				String originalLink = sAction + "&" + qry_aux;
				String sSave2TSDLink = getLinkPrefix(TSConnectionURL.idGET)  +  originalLink;

				HashMap<String, String> data = new HashMap<String, String>();
				loadDataHash(data,taxYear);
				
				if (isInstrumentSaved(pid, null, data)) {
					details += CreateFileAlreadyInTSD();
					
				} else {
					mSearch.addInMemoryDoc(sSave2TSDLink, details);
					details = addSaveToTsdButton(details, sSave2TSDLink,
							viParseID);
				}

				Response.getParsedResponse().setPageLink(new LinkInPage(sSave2TSDLink, originalLink, TSServer.REQUEST_SAVE_TO_TSD));
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
			if (sAction.indexOf("details") != -1) {
				ParseResponse(sAction, Response, ID_DETAILS);
			} else {
				ParseResponse(sAction, Response, ID_SEARCH_BY_NAME);
			}

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

//		// Parse deed book/page and date
//		Matcher matcher = Pattern.compile("Deed Bk:\\s*(\\d*)\\s*Pg:\\s*(\\d*)\\s*Date: (\\d{1,2}/\\d{1,2}/\\d{4})")
//				.matcher(input);
//		
//		if (matcher.find()) {
//			map.put("deed_book", matcher.group(1));
//			map.put("deed_page", matcher.group(2));
//			map.put("deed_date", matcher.group(3));
//		}

		// Parse legal.
//		Matcher matcher = Pattern.compile("Plat Bk:\\s*(\\d*)\\s*Pg:\\s*(\\d*)\\s*Blk:\\s*(\\d*)\\s*")
		Matcher matcher = Pattern.compile(".*Bl(?:oc)?k\\s*(\\d*).*")
				.matcher(input);
		if (matcher.find()) {
//			map.put("plat_bk", matcher.group(1));
//			map.put("plat_pg", matcher.group(2));
			if (StringUtils.isNotEmpty(matcher.group(1))) {
				map.put(PropertyIdentificationSetKey.SUBDIVISION_BLOCK.getKeyName(), matcher.group(1));
				input = input.replaceFirst("\\s*\\bBl(?:oc)?k\\b\\s*(\\d*)?"," ");
			}
		}

		matcher = Pattern.compile(".*Subdivision\\s*(.*)")
				.matcher(input);
		
		if (matcher.find()) {
			String subdivision = matcher.group(1).trim();
			if (subdivision != null) {
				matcher = Pattern.compile(".*SEC\\s*(\\d+).*").matcher(subdivision);
				if (matcher.find()) {
					map.put(PropertyIdentificationSetKey.SUBDIVISION_SECTION.getKeyName(), matcher.group(1).trim());
					subdivision = subdivision.replaceAll("\\s*SEC\\s*(\\d+)\\s*", " ");
				}
				matcher = Pattern.compile(".*PH\\s*(\\d+).*").matcher(subdivision);
				if (matcher.find()) {
					map.put(PropertyIdentificationSetKey.SUBDIVISION_PHASE.getKeyName(), matcher.group(1).trim());
					subdivision = subdivision.replaceAll("\\s*PH\\s*(\\d+)\\s*", " ");
				}
				matcher = Pattern.compile(".*Lot\\s*Acres\\s*EQ Factor.*").matcher(subdivision);
				if (matcher.find()) { 
					//there is no subdiv name
					subdivision = "";
				}
				if (StringUtils.isNotEmpty(subdivision)) {
					map.put(PropertyIdentificationSetKey.SUBDIVISION.getKeyName(), subdivision);
				}
			}
			
			matcher = Pattern.compile(".*Lot\\s*(\\d*)")
					.matcher(input);
			if (matcher.find()) {
				String lot = matcher.group(1).trim();
				lot = lot.replaceFirst("\\s*\\b0+", "").trim();
				if (StringUtils.isNotEmpty(lot)) {
					map.put(PropertyIdentificationSetKey.SUBDIVISION_LOT_NUMBER.getKeyName(), lot);
				} 
				input = input.replaceFirst("\\s*\\bLot\\b\\s*(?:\\d+)?", " ");
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
			String rsp = PDFUtils.extractTextFromPDF(resP.getResponseAsStream(),
					true);

			if (rsp == null || rsp.trim().length() == 0)
				// No valid response.
				return new HashMap<String, String>();
			return parseLegal(rsp);
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			HttpManager.releaseSite(site);
		}
		return null;
	}

	protected String getDetails(String response) {

		// if from memory - use it as is
		if (!response.toLowerCase().contains("<html")) {
			return response;
		}
		
		String contents = "";
		String pin = "", taxYear = "";

		NodeList mainList = new NodeList();
		
		try {
			org.htmlparser.Parser htmlParser = org.htmlparser.Parser.createParser(Tidy.tidyParse(response, null), null);
			mainList = htmlParser.parse(null);
			contents = HtmlParser3.getNodeByID("tblRecordView", mainList, true).toHtml();
			
			pin = mainList.extractAllNodesThatMatch(new TagNameFilter("span"), true)
					.extractAllNodesThatMatch(new HasAttributeFilter("id", "ctl00_cphMainContent_SkeletonCtrl_10_tbcTaxes_tbOverview_lblParcel"))
					.elementAt(0).toPlainTextString().trim();
			
			taxYear = mainList.extractAllNodesThatMatch(new TagNameFilter("span"), true)
					.extractAllNodesThatMatch(new HasAttributeFilter("id", "ctl00_cphMainContent_SkeletonCtrl_10_tbcTaxes_tbOverview_lblTaxYear"))
					.elementAt(0).toPlainTextString().trim();
			
			//save parameters
			Map<String, String> paramsLink = ro.cst.tsearch.connection.http2.TNWilliamsonYC.isolateParams(
					response, "aspnetForm", getDataSite().getCountyName());
			
			mSearch.setAdditionalInfo(getCurrentServerName() + ":paramsReceiptDetails:", paramsLink);
			
		} catch (Exception e) {
			e.printStackTrace();
		}

		String pdfUrl = "";
		
		String[] htmlParts = response.split("Sys.Application.add_init");
		htmlParts[0] = "";
		
		
		boolean hasReceiptTab = true;
		
		for(String s : htmlParts){
			if(s.contains("__tab_ctl00_cphMainContent_SkeletonCtrl_10_tbcTaxes_tbMailer")){
				if(s.contains("\"enabled\":false")){
					hasReceiptTab = false;
				}
				break;
			}
		}
		
		NodeList auxNodes = new NodeList();
		
		if (hasReceiptTab) {
			// get PDF url
			HttpSite site = HttpManager.getSite(HashCountyToIndex.getDateSiteForMIServerID(getCommunityId(), miServerID).getName(), searchId);

			try {
				HTTPRequest reqP = new HTTPRequest(getDataSite().getServerHomeLink() + "taxSearch/default.aspx", HTTPRequest.POST);
				HTTPResponse resP = null;

				reqP.setPostParameter("ctl00$ScriptManager1", "ctl00$cphMainContent$SkeletonCtrl_10$UpdatePanel2|ctl00$cphMainContent$SkeletonCtrl_10$tbcTaxes");
				reqP.setPostParameter("ctl00_cphMainContent_SkeletonCtrl_10_tbcTaxes_ClientState", "{\"ActiveTabIndex\":1,\"TabState\":[true,true,true,true,true]}");
				reqP.setPostParameter("__EVENTTARGET","ctl00$cphMainContent$SkeletonCtrl_10$tbcTaxes");
				reqP.setPostParameter("__EVENTARGUMENT","activeTabChanged:1");
				
				resP = site.process(reqP);
				
				if(resP!=null){
					auxNodes = new HtmlParser3(resP.getResponseAsString()).getNodeList();
					//get link 
					auxNodes = auxNodes.extractAllNodesThatMatch(new TagNameFilter("div"),true)
//							.extractAllNodesThatMatch(new HasAttributeFilter("id","ctl00_cphMainContent_SkeletonCtrl_10_tbcTaxes_tbMailer_tab"));
							.extractAllNodesThatMatch(new HasAttributeFilter("id", "ctl00_cphMainContent_SkeletonCtrl_10_tbcTaxes_tbBill"));
					
					if(auxNodes.size() > 0){
						auxNodes = auxNodes.elementAt(0).getChildren().extractAllNodesThatMatch(new TagNameFilter("iframe"));
						
						if(auxNodes.size() > 0){
							TagNode f = (TagNode) auxNodes.elementAt(0);
							
							pdfUrl = f.getAttribute("src");
						}
					}
				}

			} catch (Exception e) {
				e.printStackTrace();
			} finally {
				HttpManager.releaseSite(site);
			}
		}
		
		if (StringUtils.isNotEmpty(pdfUrl)) {
			// Get last tax receipt pdf and parse it.
			try {
				HashMap<String, String> data = getAndParsePdf(null, pdfUrl.replaceAll(" ", "%20"));

				if (data != null) {
					// Add this information to the html.
					StringBuilder fromPdf = new StringBuilder(
							"<br/><br/><table id=\"legal\" cellspacing=\"0\" cellpadding=\"3\" rules=\"all\" border=\"1\"  style=\"border-collapse:collapse;width: 40%;\">\n<tr>\n<td>");
					fromPdf.append("Subdivision: " + org.apache.commons.lang.StringUtils.defaultString(data.get(PropertyIdentificationSetKey.SUBDIVISION.getKeyName())) + ";  ");
					if (StringUtils.isNotEmpty(data.get(PropertyIdentificationSetKey.SUBDIVISION_PHASE.getKeyName()))) {
						fromPdf.append("Phase: " + org.apache.commons.lang.StringUtils.defaultString(data.get(PropertyIdentificationSetKey.SUBDIVISION_PHASE.getKeyName())) + ";  ");
					}
					fromPdf.append("</td></tr><tr><td>");
//					fromPdf.append("Plat Book: " + org.apache.commons.lang.StringUtils.defaultString(data.get("plat_bk")) +
//							"   -  Pg: " + org.apache.commons.lang.StringUtils.defaultString(data.get("plat_pg")) +
					if (StringUtils.isNotEmpty(data.get(PropertyIdentificationSetKey.SUBDIVISION_BLOCK.getKeyName()))) { 
						fromPdf.append("Block: " + org.apache.commons.lang.StringUtils.defaultString(data.get(PropertyIdentificationSetKey.SUBDIVISION_BLOCK.getKeyName())) + ";  ");
					}
					
					fromPdf.append("Lot: " + org.apache.commons.lang.StringUtils.defaultString(data.get(PropertyIdentificationSetKey.SUBDIVISION_LOT_NUMBER.getKeyName())) + ";  ");
//					fromPdf.append("</td></tr><tr><td>");
//					fromPdf.append("Deed Bk: " + org.apache.commons.lang.StringUtils.defaultString(data.get("deed_book")) +
//							" Pg: " + org.apache.commons.lang.StringUtils.defaultString(data.get("deed_page")) +
//							" Date: " + org.apache.commons.lang.StringUtils.defaultString(data.get("deed_date")));
//					fromPdf.append("</td></tr><tr><td>");
					fromPdf.append("</td></tr></table><br/></br>");
					contents += fromPdf.toString();
				}
			} catch (Exception e) {
				e.printStackTrace();
			}
		}

		contents = contents.replaceAll("(?is)</?body[^>]*>", "");
		contents = contents.replaceAll("(?is)</?form[^>]*>", "");
		contents = contents.replaceAll("(?is)(?is)<script.*?</script>", "");
		contents = contents.replaceAll("(?is)<input[^>]*>", "");
		contents = contents.replaceAll("(?is)<img[^>]*>", "");
		contents = contents.replaceAll("(?is)<div[^>]*>\\s*</div>", "");
		contents = contents.replaceAll("(?is)(<select)\\s+",
				"$1 disabled=\"true\"");

		contents = contents.replaceAll("(?is)</?a[^>]*>", "");

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
			
			String regExp = "(?is)(\\d+)([A-Z])\\s([A-Z]).*";
			Matcher m = Pattern.compile(regExp).matcher(pin);
			if (m.find()) {
				// 021E A 003.00
				pin = pin.replaceFirst("(?is)(\\d+)([A-Z])\\s([A-Z])", "$1 $2 $3");
			}
			
			regExp = "(?is)(\\d+)\\s([A-Z]+)\\s([A-Z])\\s*(\\d{1,3}\\.\\d{2})([A-Z])(\\s*\\d+)";
			m = Pattern.compile(regExp).matcher(pin);
			if (m.find()) {
				//cases like: 78 J A 5.00P24
				ctrl1 = m.group(1);
				ctrl2 = m.group(2);
				group = m.group(3);
				parcel = m.group(4);
				id = m.group(5);
				si = m.group(6).trim();

			} else {
				regExp = "(?is)(\\d+)\\s([A-Z]+)\\s([A-Z])\\s(\\d{1,3}\\.\\d{2})(\\s*\\d+)";
				m = Pattern.compile(regExp).matcher(pin);
				if (m.find()) {
					ctrl1 = m.group(1);
					ctrl2 = m.group(2);
					group = m.group(3);
					parcel = m.group(4);
					si =  m.group(5).trim();
				
				} else {
					regExp = "(?is)(\\d+)\\s([A-Z]+)\\s([A-Z])\\s(\\d{1,3}\\.\\d{2})";
					m = Pattern.compile(regExp).matcher(pin);
					if (m.find()) {
						ctrl1 = m.group(1);
						ctrl2 = m.group(2);
						group = m.group(3);
						parcel = m.group(4);
						
					} else {
						regExp = "(?is)(\\d+)\\s([A-Z])\\s(\\d{1,3}\\.\\d{2})(\\s*\\d+)";
						m = Pattern.compile(regExp).matcher(pin);
						if (m.find()) {
							ctrl1 = m.group(1);
							group = m.group(2);
							parcel = m.group(3);
							si =  m.group(4).trim();
						
						} else {
							regExp = "(?is)(\\d+)\\s(\\d{1,3}\\.\\d{2})(\\s*[A-Z])(\\s*\\d+)";
							m = Pattern.compile(regExp).matcher(pin);
							if (m.find()) {
								ctrl1 = m.group(1);
								parcel = m.group(2);
								id = m.group(3);
								si =  m.group(4).trim();
							
							} else {
								regExp = "(?is)(\\d+)\\s(\\d{1,3}\\.\\d{2})(\\s*\\d+)";
								m = Pattern.compile(regExp).matcher(pin);
								if (m.find()) {
									ctrl1 = m.group(1);
									parcel = m.group(2);
									si =  m.group(3).trim();
								
								} else {
									regExp = "(?is)(\\d+)\\s(\\d{1,3}\\.\\d{2})";
									m = Pattern.compile(regExp).matcher(pin);
									if (m.find()) {
										ctrl1 = m.group(1);
										parcel = m.group(2);
									}
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
			if (StringUtils.isNotEmpty(id)) {
				parts[4] = id;				
			}
			if (StringUtils.isNotEmpty(si)) {
				parts[5] = si;				
			}
		} 
		
		return parts;
	}
	

	protected void putSrcType(ResultMap m){
		m.put(OtherInformationSetKey.SRC_TYPE.getKeyName(), "YC");
	}
	
	
	public static void parseNamesTNWilliamsonYC(ResultMap m, long searchId) throws Exception {
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
		// String ln="";

		for (int i = 0; i < owners.length; i++) {
			String ow = owners[i];
			ow = ow.replaceAll("U/D/T", "");
			if (NameUtils.isCompany(ow)) {
				names[2] = ow;
			} else {
//				if (i == 1)
//					names = StringFormats.parseNameDesotoRO(ow, true);
//				else
//					names = StringFormats.parseNameNashville(ow, true);
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
	
	
	public Collection<ParsedResponse> smartParseIntermediary(ServerResponse response, String table, StringBuilder outputTable) {

		Vector<ParsedResponse> intermediaryResponse = new Vector<ParsedResponse>();

		try {

			table = table.replaceAll("\n", "").replaceAll("\r", "");

			Map<String, String> paramsLink = ro.cst.tsearch.connection.http2.TNWilliamsonYC
					.isolateParams(response.getResult(), "aspnetForm", getDataSite().getCountyName());
			
			String query = response.getQuerry();
			if (StringUtils.isNotEmpty(query)) {
				String[] params = query.split("&");
				for (String param : params) {
					String name = param.replaceAll("(?is)([^=]+)=.*", "$1");
					String value = param.replaceAll("(?is)([^=]+)=(.*)", "$2");
					paramsLink.put(name, value);
				}
			}

			org.htmlparser.Parser htmlParser = org.htmlparser.Parser.createParser(table, null);
			NodeList mainTableList = htmlParser.parse(null);
			NodeList tableList = mainTableList.extractAllNodesThatMatch(new TagNameFilter("table"), true);
			if (tableList.size() > 0) {
				TableTag mainTable = (TableTag) tableList.elementAt(0);
				TableRow[] rows = mainTable.getRows();
				TableRow headerRow = rows[0];
				String tableHeader = headerRow.toHtml();

				for (TableRow row : rows) {
					if (row.getColumnCount() > 6) {

						TableColumn[] cols = row.getColumns();
						NodeList inputList = cols[6].getChildren()
								.extractAllNodesThatMatch(new TagNameFilter("input"), true);

						if (inputList.size() > 0) {
							InputTag input = (InputTag) inputList.elementAt(0);
							String name = input.getAttribute("name");
							String value = input.getAttribute("value");
							String link = CreatePartialLink(TSConnectionURL.idPOST)
									+ "/taxSearch/default.aspx?details=&" + name + "=" + value;

							String rowHtml = row.toHtml();
							rowHtml = rowHtml.replaceAll("(?is)<img[^>]+>", "");
							rowHtml = rowHtml.replaceAll("(?is)\\$", "@@@");
							link = link.replaceAll("(?is)\\$", "@@@");
							rowHtml = rowHtml.replaceAll("(?is)<input[^>]+>", "<a href=\"" + link + "\"> VIEW </a>");
							rowHtml = rowHtml.replaceAll("(?is)@@@", "\\$");
							link = link.replaceAll("(?is)@@@", "\\$");
							ParsedResponse currentResponse = new ParsedResponse();
							currentResponse.setAttribute(ParsedResponse.SERVER_ROW_RESPONSE, rowHtml);
							currentResponse.setOnlyResponse(rowHtml);
							currentResponse.setPageLink(new LinkInPage(link, link, TSServer.REQUEST_SAVE_TO_TSD));

							ResultMap m = new ResultMap();
							String ownerName = cols[0].getChildren().toHtml().trim();
							if (StringUtils.isNotEmpty(ownerName)) {
								m.put(PropertyIdentificationSetKey.NAME_ON_SERVER.getKeyName(), ownerName);
							}
							String taxYear = cols[1].getChildren().toHtml().trim();
							if (StringUtils.isNotEmpty(taxYear)) {
								m.put(TaxHistorySetKey.YEAR.getKeyName(), taxYear);
							}
							
							String baseAmount = cols[4].getChildren().toHtml().trim();
							if (StringUtils.isNotEmpty(baseAmount)) {
								m.put(TaxHistorySetKey.BASE_AMOUNT.getKeyName(), baseAmount);
							}
							
							
							parseNamesTNWilliamsonYC(m, searchId);
							
							m.removeTempDef();
							putSrcType(m);
							Bridge bridge = new Bridge(currentResponse, m, searchId);

							DocumentI document = (TaxDocumentI) bridge.importData();
							currentResponse.setDocument(document);

							intermediaryResponse.add(currentResponse);
						}
					}
				}
				
				mSearch.setAdditionalInfo(getCurrentServerName() + ":paramsDetails:", paramsLink);

				String result = response.getResult();
				result = result.replaceAll("(?is)name\\s*=([A-Z]+)\\s+", "name=\"$1\" ");

				response.getParsedResponse().setHeader("&nbsp;&nbsp;&nbsp;<br><br>"
										+ table.substring(table.indexOf("<table"), table.indexOf(">") + 1)
										+ tableHeader.replaceAll("(?is)</?a[^>]*>", ""));
				
				response.getParsedResponse().setFooter("</table><br><br>");

				outputTable.append(table);
			}

		} catch (Exception e) {
			e.printStackTrace();
		}

		return intermediaryResponse;
	}


	@Override
	protected Object parseAndFillResultMap(ServerResponse response, String detailsHtml, ResultMap map) {
		
		detailsHtml = detailsHtml.replaceAll("(?is)&nbsp;", "")
				.replaceAll("(?is)&amp;", "&").replaceAll("(?is)<th\\b", "<td")
				.replaceAll("(?is)</th\\b", "</td")
				.replaceAll("(?is)</?b>", "")
				.replaceAll("(?is)</?font[^>]*>", "");
		
		putSrcType(map);
		
		try {
			org.htmlparser.Parser htmlParser = org.htmlparser.Parser
					.createParser(detailsHtml, null);
			NodeList mainList = htmlParser.parse(null);
			
			String pin = mainList.extractAllNodesThatMatch(new TagNameFilter("span"), true)
					.extractAllNodesThatMatch(new HasAttributeFilter("id", "ctl00_cphMainContent_SkeletonCtrl_10_tbcTaxes_tbOverview_lblParcel"))
					.elementAt(0).toPlainTextString().trim();
			
			if (StringUtils.isNotEmpty(pin)) {
				map.put(PropertyIdentificationSetKey.PARCEL_ID.getKeyName(), pin);
//				splitPin(pin, map);
			}
			
			String address = mainList.extractAllNodesThatMatch(new TagNameFilter("span"), true)
					.extractAllNodesThatMatch(new HasAttributeFilter("id", "ctl00_cphMainContent_SkeletonCtrl_10_tbcTaxes_tbOverview_lblPropertyAddress"))
					.elementAt(0).toPlainTextString().trim();
			
			if (StringUtils.isNotEmpty(address)) {
				map.put(PropertyIdentificationSetKey.STREET_NO.getKeyName(),   StringFormats.StreetNo(address));
				map.put(PropertyIdentificationSetKey.STREET_NAME.getKeyName(), StringFormats.StreetName(address));
			}
			
			String ownerName = mainList.extractAllNodesThatMatch(new TagNameFilter("span"), true)
					.extractAllNodesThatMatch(new HasAttributeFilter("id", "ctl00_cphMainContent_SkeletonCtrl_10_tbcTaxes_tbOverview_lblOwnerName"))
					.elementAt(0).toHtml().replaceAll("(?is)</?span[^>]*>", "").trim();
			
			if (StringUtils.isNotEmpty(ownerName)) {
				map.put(PropertyIdentificationSetKey.NAME_ON_SERVER.getKeyName(), ownerName);
			}
			
			String taxYear = mainList.extractAllNodesThatMatch(new TagNameFilter("span"), true)
					.extractAllNodesThatMatch(new HasAttributeFilter("id", "ctl00_cphMainContent_SkeletonCtrl_10_tbcTaxes_tbOverview_lblTaxYear"))
					.elementAt(0).toPlainTextString().trim();
			
			if (StringUtils.isNotEmpty(taxYear)) {
				map.put(TaxHistorySetKey.YEAR.getKeyName(), taxYear);
			}
			
			String baseAmount = mainList.extractAllNodesThatMatch(new TagNameFilter("span"), true)
					.extractAllNodesThatMatch(new HasAttributeFilter("id", "ctl00_cphMainContent_SkeletonCtrl_10_tbcTaxes_tbOverview_lblBaseAmtDue"))
					.elementAt(0).toPlainTextString().trim();
			if (StringUtils.isNotEmpty(baseAmount)) {
				baseAmount = baseAmount.replaceAll("(?is)[\\$,]+", "");
				map.put(TaxHistorySetKey.BASE_AMOUNT.getKeyName(), baseAmount);
			}

			String amountPaid = mainList
					.extractAllNodesThatMatch(new TagNameFilter("span"), true)
					.extractAllNodesThatMatch(new HasAttributeFilter("id", "ctl00_cphMainContent_SkeletonCtrl_10_tbcTaxes_tbOverview_lblPaidAmount"))
					.elementAt(0).toPlainTextString().trim();
			
			if (StringUtils.isNotEmpty(amountPaid)) {
				amountPaid = amountPaid.replaceAll("(?is)[\\$,]+", "");
				map.put(TaxHistorySetKey.AMOUNT_PAID.getKeyName(), amountPaid);
			}
			
			String totalDue = mainList.extractAllNodesThatMatch(new TagNameFilter("span"), true)
					.extractAllNodesThatMatch(new HasAttributeFilter("id", "ctl00_cphMainContent_SkeletonCtrl_10_tbcTaxes_tbOverview_lblBalanceDue"))
					.elementAt(0).toPlainTextString().trim();
			
			if (StringUtils.isNotEmpty(totalDue)) {
				totalDue = totalDue.replaceAll("(?is)[\\$,]+", "");
				if (!totalDue.contains("No longer collected by the Trustee")) {
					map.put(TaxHistorySetKey.TOTAL_DUE.getKeyName(), totalDue.replaceAll("(?is)[\\$,]+", "").trim());
				} else {
					if (StringUtils.isNotEmpty(baseAmount)) {
						map.put(TaxHistorySetKey.TOTAL_DUE.getKeyName(), baseAmount);
					}
				}
			}

			String taxStatus = mainList.extractAllNodesThatMatch(new TagNameFilter("span"), true)
					.extractAllNodesThatMatch(new HasAttributeFilter("id", "ctl00_cphMainContent_SkeletonCtrl_10_tbcTaxes_tbOverview_lblPaymentStatus"))
					.elementAt(0).toPlainTextString().trim();
			
			if (StringUtils.isNotEmpty(taxStatus) && !"unpaid".equals(taxStatus.toLowerCase())) {
				String receiptNo = mainList.extractAllNodesThatMatch(new TagNameFilter("span"), true)
						.extractAllNodesThatMatch(new HasAttributeFilter("id", "ctl00_cphMainContent_SkeletonCtrl_10_tbcTaxes_tbOverview_lblReceipt")) 
						.elementAt(0).toPlainTextString().trim();
				
				String receiptDate =  mainList.extractAllNodesThatMatch(new TagNameFilter("span"), true)
						.extractAllNodesThatMatch(new HasAttributeFilter("id", "ctl00_cphMainContent_SkeletonCtrl_10_tbcTaxes_tbOverview_lblPaidDate"))
						.elementAt(0).toPlainTextString().trim();
				
				Map<String, String[]> tmpMap = new HashMap<String, String[]>();
				ResultTable receipts = new ResultTable();
				String[] header = { TaxHistorySetKey.RECEIPT_AMOUNT.getShortKeyName(), TaxHistorySetKey.RECEIPT_NUMBER.getShortKeyName(), TaxHistorySetKey.RECEIPT_DATE.getShortKeyName() };
				List<List<String>> bodyRT = new ArrayList<List<String>>();
				List<String> paymentRow = new ArrayList<String>();
				
				if (!"0.00".equals(amountPaid) && StringUtils.isNotEmpty(receiptNo) && StringUtils.isNotEmpty(receiptDate)) {
					paymentRow.add(amountPaid);
					paymentRow.add(receiptNo);
					paymentRow.add(receiptDate);
					bodyRT.add(paymentRow);
				}
				
				tmpMap.put("ReceiptAmount", new String[] { "ReceiptAmount", "" });
				tmpMap.put("ReceiptNumber", new String[] { "ReceiptNumber", "" });
				tmpMap.put("ReceiptNumber", new String[] { "ReceiptDate", "" });
				try {
					receipts.setHead(header);
					receipts.setMap(tmpMap);
					receipts.setBody(bodyRT);
					receipts.setReadOnly();
					map.put("TaxHistorySet", receipts);	
				} catch (Exception e) {
					e.printStackTrace();
				}
				
			}

		} catch (Exception e) {
			e.printStackTrace();
		}
		try {
			parseNamesTNWilliamsonYC(map, searchId);
			
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