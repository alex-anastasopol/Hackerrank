package ro.cst.tsearch.servers.types;



import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Vector;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang.StringUtils;
import org.htmlparser.filters.HasAttributeFilter;
import org.htmlparser.filters.HasChildFilter;
import org.htmlparser.filters.NotFilter;
import org.htmlparser.filters.StringFilter;
import org.htmlparser.filters.TagNameFilter;
import org.htmlparser.tags.LinkTag;
import org.htmlparser.tags.OptionTag;
import org.htmlparser.tags.SelectTag;
import org.htmlparser.tags.TableColumn;
import org.htmlparser.tags.TableRow;
import org.htmlparser.tags.TableTag;
import org.htmlparser.util.NodeList;

import ro.cst.tsearch.Search;
import ro.cst.tsearch.bean.SearchAttributes;
import ro.cst.tsearch.bean.recoverdocument.ModuleShortDescription;
import ro.cst.tsearch.connection.http2.HttpManager;
import ro.cst.tsearch.connection.http2.HttpSite;
import ro.cst.tsearch.database.DBManager;
import ro.cst.tsearch.exceptions.ServerResponseException;
import ro.cst.tsearch.extractor.legal.LegalDescription;
import ro.cst.tsearch.extractor.xml.Bridge;
import ro.cst.tsearch.extractor.xml.GenericFunctions;
import ro.cst.tsearch.extractor.xml.GenericFunctions2;
import ro.cst.tsearch.extractor.xml.ResultMap;
import ro.cst.tsearch.extractor.xml.ResultTable;
import ro.cst.tsearch.extractor.xml.StringFormats;
import ro.cst.tsearch.log.SearchLogFactory;
import ro.cst.tsearch.log.SearchLogPage;
import ro.cst.tsearch.parser.HtmlParser3;
import ro.cst.tsearch.search.filter.FilterResponse;
import ro.cst.tsearch.search.filter.newfilters.name.NameFilterFactory;
import ro.cst.tsearch.search.filter.newfilters.name.NameFilterForNext;
import ro.cst.tsearch.search.iterator.ModuleStatesIteratorFactory;
import ro.cst.tsearch.search.module.ConfigurableNameIterator;
import ro.cst.tsearch.search.name.NameUtils;
import ro.cst.tsearch.servers.HashCountyToIndex;
import ro.cst.tsearch.servers.TSConnectionURL;
import ro.cst.tsearch.servers.info.TSServerInfo;
import ro.cst.tsearch.servers.info.TSServerInfoConstants;
import ro.cst.tsearch.servers.info.TSServerInfoModule;
import ro.cst.tsearch.servers.response.LinkInPage;
import ro.cst.tsearch.servers.response.OtherInformationSet.OtherInformationSetKey;
import ro.cst.tsearch.servers.response.ParsedResponse;
import ro.cst.tsearch.servers.response.Parser;
import ro.cst.tsearch.servers.response.PropertyAppraisalSet.PropertyAppraisalSetKey;
import ro.cst.tsearch.servers.response.PropertyIdentificationSet.PropertyIdentificationSetKey;
import ro.cst.tsearch.servers.response.ServerResponse;
import ro.cst.tsearch.servers.response.TaxHistorySet.TaxHistorySetKey;
import ro.cst.tsearch.threads.GPMaster;
import ro.cst.tsearch.utils.InstanceManager;
import ro.cst.tsearch.utils.Roman;
import ro.cst.tsearch.utils.SearchLogger;
import ro.cst.tsearch.utils.Tidy;

import com.stewart.ats.base.document.DocumentI;
import com.stewart.ats.base.document.TaxDocumentI;

/**
 * @author mihaib
*/

public class OHCuyahogaServerTR extends TSServer{
	
	private boolean downloadingForSave;
	private static final long serialVersionUID = 1L;

	private static final Pattern VIEW_FULL_SUMMARY_PAT = Pattern.compile("(?is)href\\s*='([^']+)'[^>]*>View full year summary");
	//private static final Pattern PAY_IN_YEAR_PAT = Pattern.compile("(?is)pay\\s+in\\s+(\\d{4})");

	private static LinkedHashMap<String,String> cities = null;
	
	static {
		cities = new LinkedHashMap<String, String>();
		cities.put("ENTIRE COUNTY", "'00101001' AND '99999999z'");
		cities.put("BAY VILLAGE", "'20101001' AND '21099999z'");
		cities.put("BEACHWOOD", "'74101001' AND '74499999z'");
		cities.put("BEDFORD CITY", "'81101001' AND '82099999z'");
		cities.put("BEDFORD HEIGHTS", "'79101001' AND '79299999z'");
		cities.put("BENTLEYVILLE", "'94101001' AND '95099999z'");
		cities.put("BEREA", "'36101001' AND '37099999z'");
		cities.put("BRATENAHL", "'63101001' AND '64099999z'");
		cities.put("BRECKSVILLE", "'60101001' AND '63099999z'");
		cities.put("BROADVIEW HEIGHTS", "'58101001' AND '60099999z'");
		cities.put("BROOK PARK", "'34101001' AND '36099999z'");
		cities.put("BROOKLYN", "'43101001' AND '44099999z'");
		cities.put("BROOKLYN HEIGHTS", "'53101001' AND '54099999z'");
		cities.put("CHAGRIN FALLS TOWNSHIP", "'92101001' AND '93099999z'");
		cities.put("CHAGRIN FALLS VILLAGE", "'93101001' AND '94099999z'");
		cities.put("CLEVELAND EAST/RIVER", "'10101001' AND '14499999z'");
		cities.put("CLEVELAND WEST/RIVER", "'00101001' AND '02999999z'");
		cities.put("CLEVELAND HEIGHTS", "'68101001' AND '70099999z'");
		cities.put("CUYAHOGA HEIGHTS", "'52101001' AND '53099999z'");
		cities.put("EAST CLEVELAND", "'67101001' AND '68099999z'");
		cities.put("EUCLID", "'64101001' AND '66099999z'");
		cities.put("FAIRVIEW PARK", "'32101001' AND '34099999z'");
		cities.put("GARFIELD HEIGHTS", "'54101001' AND '55099999z'");
		cities.put("GATES MILLS", "'84101001' AND '86099999z'");
		cities.put("GLENWILLOW", "'99101001' AND '99999999z'");
		cities.put("HIGHLAND HEIGHTS", "'82101001' AND '83099999z'");
		cities.put("HIGHLAND HILLS", "'75101001' AND '76099999z'");
		cities.put("HUNTING VALLEY", "'88101001' AND '89099999z'");
		cities.put("INDEPENDENCE", "'56101001' AND '57099999z'");
		cities.put("LAKEWOOD", "'31101001' AND '32099999z'");
		cities.put("LINNDALE", "'42101001' AND '43099999z'");
		cities.put("LYNDHURST", "'71101001' AND '72099999z'");
		cities.put("MAPLE HEIGHTS", "'78101001' AND '79099999z'");
		cities.put("MAYFIELD HEIGHTS", "'86101001' AND '87099999z'");
		cities.put("MAYFIELD VILLAGE", "'83101001' AND '84099999z'");
		cities.put("MIDDLEBURG HEIGHTS", "'37101001' AND '39099999z'");
		cities.put("MORELAND HILLS", "'91101001' AND '92099999z'");
		cities.put("NEWBURGH HEIGHTS", "'51101001' AND '52099999z'");
		cities.put("NORTH OLMSTED", "'23101001' AND '25099999z'");
		cities.put("NORTH RANDALL", "'77101001' AND '78099999z'");
		cities.put("NORTH ROYALTON", "'48101001' AND '51099999z'");
		cities.put("OAKWOOD", "'79501001' AND '81099999z'");
		cities.put("OLMSTED FALLS", "'28101001' AND '30099999z'");
		cities.put("OLMSTED TOWNSHIP", "'26101001' AND '28099999z'");
		cities.put("ORANGE", "'90101001' AND '91099999z'");
		cities.put("PARMA", "'44101001' AND '47099999z'");
		cities.put("PARMA HEIGHTS", "'47101001' AND '48099999z'");
		cities.put("PEPPER PIKE", "'87101001' AND '88099999z'");
		cities.put("RICHMOND HEIGHTS", "'66101001' AND '67099999z'");
		cities.put("ROCKY RIVER", "'30101001' AND '31099999z'");
		cities.put("SEVEN HILLS", "'55101001' AND '56099999z'");
		cities.put("SHAKER HEIGHTS", "'73101001' AND '74099999z'");
		cities.put("SOLON", "'95101001' AND '99099999z'");
		cities.put("SOUTH EUCLID", "'70101001' AND '71099999z'");
		cities.put("STRONGSVILLE", "'39101001' AND '42099999z'");
		cities.put("UNIVERSITY HEIGHTS", "'72101001' AND '73099999z'");
		cities.put("VALLEY VIEW", "'57101001' AND '58099999z'");
		cities.put("WALTON HILLS", "'79301001' AND '79499999z'");
		cities.put("WARRENSVILLE HEIGHTS", "'76101001' AND '77099999z'");
		cities.put("WESTLAKE", "'21101001' AND '23099999z'");
		cities.put("WOODMERE", "'89101001' AND '90099999z'");
	}
	
	public OHCuyahogaServerTR(long searchId) {
		super(searchId);
	}

	public OHCuyahogaServerTR(String rsRequestSolverName, String rsSitePath,
			String rsServerID, String rsPrmNameLink, long searchId,	int miServerID) {
		super(rsRequestSolverName, rsSitePath, rsServerID, rsPrmNameLink, searchId,	miServerID);
	}
	
	private String htmlCities;
	
	private void internalLoadCities(String selectedCity) {
		StringBuilder sb = new StringBuilder("<select name=\"City\" size=\"1\" >");
		Iterator<String> it = cities.keySet().iterator();
		while(it.hasNext()) {
			String key = it.next();
			String val = cities.get(key);
			sb.append("<option " + (key.equals(selectedCity) ? "selected" : "") + " value=\"" + val + "\">" + key + "</option>");
		}	
		sb.append("</select>");
		htmlCities = sb.toString();
	}
	
	@Override
	public TSServerInfo getDefaultServerInfo() {
		TSServerInfo defaultServerInfo = super.getDefaultServerInfo();
		
		String city = getSearchAttribute(SearchAttributes.P_CITY).toUpperCase();
		
		TSServerInfoModule tsServerInfoModule = defaultServerInfo.getModule(TSServerInfo.NAME_MODULE_IDX);
		if(tsServerInfoModule != null) {
			internalLoadCities(city);
			tsServerInfoModule.getFunction(1).setHtmlformat(htmlCities);
		}
		
		tsServerInfoModule = defaultServerInfo.getModule(TSServerInfo.TYPE_NAME_MODULE_IDX);
		if(tsServerInfoModule != null) {
			internalLoadCities(city);
			tsServerInfoModule.getFunction(2).setHtmlformat(htmlCities);
		}
		
		return defaultServerInfo;
	}
	
	
	@Override
	protected void setModulesForAutoSearch(TSServerInfo serverInfo) {
		List<TSServerInfoModule> modules = new ArrayList<TSServerInfoModule>();
		TSServerInfoModule module = null;

		FilterResponse defaultNameFilter = NameFilterFactory.getHybridNameFilter(SearchAttributes.OWNER_OBJECT , searchId , module );
		
		modules.add(new TSServerInfoModule(serverInfo.getModule(TSServerInfo.PARCEL_ID_MODULE_IDX)));
		
		String city = getSearchAttribute(SearchAttributes.P_CITY).toUpperCase();
		if (hasOwner()){
			//Search by Owner Last Name and City
			String cityCode = "'00101001' AND '99999999z'";
			if (StringUtils.isNotEmpty(city)){
				cityCode = cities.get(city);
				if ("CLEVELAND".equals(city)){
					cityCode = cities.get("CLEVELAND EAST/RIVER");
				}
			}
			if (StringUtils.isEmpty(cityCode)){
				cityCode = "'00101001' AND '99999999z'";
			}
			module = new TSServerInfoModule(serverInfo.getModule(TSServerInfo.NAME_MODULE_IDX));
			module.forceValue(1, cityCode);
			module.addFilterForNext(new NameFilterForNext(searchId));
			module.addFilter(defaultNameFilter);
			ConfigurableNameIterator nameIterator = (ConfigurableNameIterator) ModuleStatesIteratorFactory
										.getConfigurableNameIterator(module, searchId, new String[] {"L;;"});
			module.addIterator(nameIterator);
			modules.add(module);
			
			if (StringUtils.isNotEmpty(city)){
				cityCode = cities.get(city);
				if ("CLEVELAND".equals(city)){
					cityCode = cities.get("CLEVELAND WEST/RIVER");
					if (StringUtils.isNotEmpty(cityCode)){
						module = new TSServerInfoModule(serverInfo.getModule(TSServerInfo.NAME_MODULE_IDX));
						module.forceValue(1, cityCode);
						module.addFilterForNext(new NameFilterForNext(searchId));
						module.addFilter(defaultNameFilter);
						nameIterator = (ConfigurableNameIterator) ModuleStatesIteratorFactory
													.getConfigurableNameIterator(module, searchId, new String[] {"L;;"});
						module.addIterator(nameIterator);
						modules.add(module);
					}
				}
			}
			
		}
		
		serverInfo.setModulesForAutoSearch(modules);		
	}
	
	
	protected void logSearchBy(TSServerInfoModule module, Map<String, String> params) {

		if (module.isVisible() || "GB_MANAGER_OBJECT".equals(module.getSaObjKey())) {// B 4511

			// get parameters formatted properly
			Map<String, String> moduleParams = params;
			if (moduleParams == null) {
				moduleParams = module.getParamsForLog();
			}
			Search search = getSearch();
			// determine whether it's an automatic search
			boolean automatic = (search.getSearchType() != Search.PARENT_SITE_SEARCH) || (GPMaster.getThread(searchId) != null);
			boolean imageSearch = module.getLabel().equalsIgnoreCase("image search") || module.getModuleIdx() == TSServerInfo.IMG_MODULE_IDX;

			// create the message
			StringBuilder sb = new StringBuilder();
			SearchLogFactory sharedInstance = SearchLogFactory.getSharedInstance();
			SearchLogPage searchLogPage = sharedInstance.getSearchLogPage(searchId);
			sb.append("</div>");

			Object additional = GetAttribute("additional");
			if (Boolean.TRUE != additional) {
				searchLogPage.addHR();
				sb.append("<hr/>");
			}
			int fromRemoveForDB = sb.length();

			// searchLogPage.
			sb.append("<span class='serverName'>");
			String serverName = HashCountyToIndex.getDateSiteForMIServerID(getCommunityId(), miServerID).getName();
			sb.append(serverName);
			sb.append("</span> ");

			sb.append(automatic ? "automatic" : "manual");
			Object info = module.getExtraInformation(TSServerInfoConstants.EXTRA_PARAM_MODULE_DESCRIPTION);
			if (StringUtils.isNotEmpty(module.getLabel())) {

				if (info != null) {
					sb.append(" - " + info + "<br>");
				}
				sb.append(" <span class='searchName'>");
				sb.append(module.getLabel());
			} else {
				sb.append(" <span class='searchName'>");
				if (info != null) {
					sb.append(" - " + info + "<br>");
				}
			}
			sb.append("</span> by ");

			boolean firstTime = true;
			for (Entry<String, String> entry : moduleParams.entrySet()) {
				String value = entry.getValue();
				
				if ("City".equals(entry.getKey())) {
					Iterator<String> it = cities.keySet().iterator();
					while (it.hasNext()) {
						String key = it.next();
						String val = cities.get(key);
						if (value.equals(val)) {
							value = key;
							break;
						}
					}
				}
				
				if (!firstTime) {
					sb.append(", ");
				} else {
					firstTime = false;
				}
				sb.append(entry.getKey().replaceAll("&lt;br&gt;", "") + " = <b>" + value + "</b>");
			}
			//because ALL does not have a value.
			if (!moduleParams.containsKey("File Type")){
				sb.append(", ").append("File Type = <b>ALL</b>");
			}
			
			int toRemoveForDB = sb.length();
			// log time when manual is starting
			if (!automatic || imageSearch) {
				sb.append(" ");
				sb.append(SearchLogger.getTimeStamp(searchId));
			}
			sb.append(":<br/>");

			// log the message
			SearchLogger.info(sb.toString(), searchId);
			ModuleShortDescription moduleShortDescription = new ModuleShortDescription();
			moduleShortDescription.setDescription(sb.substring(fromRemoveForDB, toRemoveForDB));
			moduleShortDescription.setSearchModuleId(module.getModuleIdx());
			search.setAdditionalInfo(TSServerInfoConstants.TS_SERVER_INFO_MODULE_DESCRIPTION, moduleShortDescription);
			String user = InstanceManager.getManager().getCurrentInstance(searchId).getCurrentUser().getAttribute(1).toString();
			SearchLogger.info(ro.cst.tsearch.utils.StringUtils.createCollapsibleHeader(), searchId);
			searchLogPage.addModuleSearchParameters(serverName, additional, info, moduleParams, module.getLabel(), automatic, imageSearch, user);
		}
	}
	
	
	@Override
	protected void ParseResponse(String sAction, ServerResponse Response, int viParseID) throws ServerResponseException {
		
		String initialResponse = Response.getResult();
		
		String rsResponse = initialResponse;
		
		if (rsResponse.indexOf("Data Not Available For Current Search") != -1){
			Response.getParsedResponse().setError("Data Not Available For Current Search.");
			return;
		}
		if (rsResponse.indexOf("routine maintenance") != -1){
			Response.getParsedResponse().setError("Routine maintenance on the Cuyahoga Tax Collector site.");
			return;
		}
	
		switch (viParseID) {
			case ID_SEARCH_BY_NAME :
				
			try {

				StringBuilder outputTable = new StringBuilder();
				ParsedResponse parsedResponse = Response.getParsedResponse();

				Collection<ParsedResponse> smartParsedResponses = smartParseIntermediary(
						Response, rsResponse, outputTable);

				if (smartParsedResponses.size() > 0) {
					parsedResponse.setResultRows(new Vector<ParsedResponse>(smartParsedResponses));
					parsedResponse.setOnlyResponse(outputTable.toString());
				}

			} catch (Exception e) {
				e.printStackTrace();
			}
			break;
			
			case ID_DETAILS :
				
				String details = getDetails(rsResponse, Response);
				
				String docNo = "";
				try {
					details = details.replaceAll("(?is)&nbsp;", " ").replaceAll("(?is)&amp;", " ")
											.replaceAll("(?is)</?p[^>]*>", "").replaceAll("(?is)</?strong[^>]*>", "")
											.replaceAll("(?is)<th ", "<td ").replaceAll("(?is)</th> ", "</td>");
								
					org.htmlparser.Parser htmlParser = org.htmlparser.Parser.createParser(details, null);					
					NodeList mainList = htmlParser.parse(null);
					
					docNo = HtmlParser3.getValueFromAbsoluteCell(1, 0,  HtmlParser3.findNode(mainList, "Parcel Number"), "", true).trim();
					
				} catch (Exception e) {
					e.printStackTrace();
				}

				if ((!downloadingForSave)){	
	                String qry_aux = Response.getRawQuerry();
					qry_aux = "dummy=" + docNo + "&" + qry_aux;
					String originalLink = sAction + "&" + qry_aux;
					originalLink = originalLink.replaceAll("(?is)&$", "");
					try {
						originalLink = URLDecoder.decode(originalLink, "UTF-8");
					} catch (UnsupportedEncodingException e) {
						e.printStackTrace();
					}
					String sSave2TSDLink = getLinkPrefix(TSConnectionURL.idGET) + originalLink;
					
					HashMap<String, String> data = new HashMap<String, String>();
    				data.put("type", "CNTYTAX");
	    				
					if (isInstrumentSaved(docNo, null, data)){
	                	details += CreateFileAlreadyInTSD();
					} else {
						mSearch.addInMemoryDoc(sSave2TSDLink, details);
						details = addSaveToTsdButton(details, sSave2TSDLink, viParseID);
					}
					Response.getParsedResponse().setPageLink(
							new LinkInPage(sSave2TSDLink, originalLink, TSServer.REQUEST_SAVE_TO_TSD));
					parser.Parse(Response.getParsedResponse(), details,	Parser.NO_PARSE); 
	            } 
				else {      
					smartParseDetails(Response, details);
	                msSaveToTSDFileName = docNo + ".html";
	                Response.getParsedResponse().setFileName(getServerTypeDirectory() + msSaveToTSDFileName);
	                msSaveToTSDResponce = details + CreateFileAlreadyInTSD();
	               
				}
				break;
				
			case ID_GET_LINK :
				if (sAction.indexOf("ShowTaxBill") != -1){
						ParseResponse(sAction, Response, ID_DETAILS);
				} else if (Response.getQuerry().toLowerCase().indexOf("page") != -1) {
					ParseResponse(sAction, Response, ID_SEARCH_BY_NAME);
				}
				
				break;
			case ID_SAVE_TO_TSD :
				downloadingForSave = true;
				ParseResponse(sAction, Response, ID_DETAILS);
				downloadingForSave = false;
				break;
		}
	}
	
	public Collection<ParsedResponse> smartParseIntermediary(ServerResponse response, String html, StringBuilder outputTable) {
		
		Vector<ParsedResponse> intermediaryResponse = new Vector<ParsedResponse>();

		try {
			org.htmlparser.Parser htmlParser = org.htmlparser.Parser.createParser(html, null);
			
			NodeList nodeList = htmlParser.parse(null);
			NodeList tableList = nodeList.extractAllNodesThatMatch(new TagNameFilter("form"), true)
									.extractAllNodesThatMatch(new TagNameFilter("table"), true);
			
			StringBuilder newTable = new StringBuilder();
			newTable.append("<table BORDER='1' CELLPADDING='2'>")
					.append("<tr><th>Parcel Number</th><th>Owner</th></tr>");
			
			String pagination = "";

			if (tableList != null && tableList.size() > 0){
				TableTag mainTable = (TableTag)tableList.elementAt(0);
				
				TableRow[] rows = mainTable.getRows();
			
				for(TableRow row : rows ) {
					if(row.getColumnCount() > 0) {
						
						TableColumn[] cols = row.getColumns();
						
						if (cols[0].toHtml().contains("ShowTaxBill")){
							NodeList aList = cols[0].getChildren().extractAllNodesThatMatch(new TagNameFilter("a"), true);
							if (aList.size() > 0){
								String lnk = ((LinkTag) aList.elementAt(0)).extractLink();
								
								String pin = ((LinkTag) aList.elementAt(0)).getStringText();
									
								lnk = lnk.replaceAll("\\.\\./\\.\\.", "");
								String link = CreatePartialLink(TSConnectionURL.idGET) + "/payments/real_prop/" + lnk;
									
								String rowHtml =  row.toHtml();
								rowHtml = rowHtml.replaceAll("<a[^>]*>", "<a href=\"" + link + "\">");
								rowHtml = rowHtml.replaceAll("(?is)</?font[^>]*>", "");
								String[] cells = rowHtml.split("<br>");
								if (cells != null && cells.length > 1){
									rowHtml = cells[0] + "</td><td>" + cells[1];
								}

								ParsedResponse currentResponse = new ParsedResponse();
								currentResponse.setAttribute(ParsedResponse.SERVER_ROW_RESPONSE, rowHtml);
								currentResponse.setOnlyResponse(rowHtml);
								currentResponse.setPageLink(new LinkInPage(link, link, TSServer.REQUEST_SAVE_TO_TSD));
									
								ResultMap resultMap = new ResultMap();
								resultMap.put(OtherInformationSetKey.SRC_TYPE.getKeyName(), dataSite.getSiteTypeAbrev());
								resultMap.put(PropertyIdentificationSetKey.PARCEL_ID.getKeyName(), pin.trim());
								
								String[] infos = cols[0].getChildren().toHtml().split("<br>");
								if (infos != null && infos.length > 1){
									resultMap.put("tmpOwner", infos[1].replaceAll("(?is)</?font[^>]*>", ""));
								}
								partyNamesOHCuyahogaTR(resultMap, searchId);
								
								resultMap.removeTempDef();
								
								Bridge bridge = new Bridge(currentResponse, resultMap, searchId);
								DocumentI document = (TaxDocumentI)bridge.importData();
									
								currentResponse.setDocument(document);	
								intermediaryResponse.add(currentResponse);
							}
						} else if (cols[0].toHtml().contains("PREVIOUS") || cols[0].toHtml().contains("NEXT")){
							NodeList aList = cols[0].getChildren().extractAllNodesThatMatch(new TagNameFilter("a"), true);
							if (aList != null && aList.size() > 0){
								for (int j = 0; j < aList.size(); j++){
									if (aList.elementAt(j).toHtml().contains("PREVIOUS")){
										String prev = aList.elementAt(j).toHtml();
										String lnk = CreatePartialLink(TSConnectionURL.idGET) + ((LinkTag) aList.elementAt(j)).extractLink();
										prev = prev.replaceAll("<a[^>]*>", "<a href=\"" + lnk + "\">");
										pagination += prev + "&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;";
									} else if (aList.elementAt(j).toHtml().contains("NEXT")){
										String next = aList.elementAt(j).toHtml();
										String lnk = CreatePartialLink(TSConnectionURL.idGET) + ((LinkTag) aList.elementAt(j)).extractLink();
										next = next.replaceAll("<a[^>]*>", "<a href=\"" + lnk + "\">");
										pagination += next;
										response.getParsedResponse().setNextLink(next);
									}
								}
							}
						}
					}
				}
			}
			
			String header1 = "<tr><th>Parcel Number</th><th>Owner</th></tr>";

			response.getParsedResponse().setHeader("<br>" + pagination 
					+ "<br><br><table width=\"100%\" border=\"1\" cellspacing=\"0\" cellpadding=\"2\">\n" + header1);
			
			response.getParsedResponse().setFooter("</table><br>" + pagination + "<br>");
			
			newTable.append("</table>");
			outputTable.append(newTable);
			
		} catch (Exception e) {
			e.printStackTrace();
		}
		
		return intermediaryResponse;
	}
	
	protected String getDetails(String response, ServerResponse Response){
		
		// if from memory - use it as is
		if(!response.toLowerCase().contains("<html")){
			return response;
		}
		
		response = Tidy.tidyParse(response, null);
		String details = "";
		try {
			org.htmlparser.Parser htmlParser = org.htmlparser.Parser.createParser(response, null);
			NodeList mainList = htmlParser.parse(null);

			if (mainList != null && mainList.size() > 0){
				NodeList styleList = mainList.extractAllNodesThatMatch(new TagNameFilter("style"), true);

				if (styleList != null && styleList.size() > 0){

					//details += styleList.toHtml();
				}
				NodeList divList = mainList.extractAllNodesThatMatch(new TagNameFilter("body"), true)
								.extractAllNodesThatMatch(new HasAttributeFilter("id", "billbody"));
				if (divList != null && divList.size() > 0){

					divList.keepAllNodesThatMatch(new NotFilter(new HasAttributeFilter("id", "PerfBill")), true);
					divList.keepAllNodesThatMatch(new NotFilter(new HasAttributeFilter("id", "form1")), true);
					
					details += divList.toHtml();
				}

			}
			
			Matcher mat = VIEW_FULL_SUMMARY_PAT.matcher(response);
			if (mat.find()){
				String link = "http://treasurer.cuyahogacounty.us/payments/real_prop/" + mat.group(1);
				HttpSite site = HttpManager.getSite(getCurrentServerName(), searchId);
				if (site != null) {
					try {
						String rsp = ((ro.cst.tsearch.connection.http2.OHCuyahogaConnTR) site).getPage(link, null);
						if (!rsp.contains("Data Not Available For Current Search")){
							rsp = Tidy.tidyParse(rsp, null);
							org.htmlparser.Parser htmlParseoru = org.htmlparser.Parser.createParser(rsp, null);
							NodeList nodeList = htmlParseoru.parse(null);
							
							NodeList bodyList = nodeList.extractAllNodesThatMatch(new TagNameFilter("body"), true);
							if (bodyList != null && bodyList.size() > 0){
								bodyList.keepAllNodesThatMatch(new NotFilter(new TagNameFilter("div")), true);
								//bodyList.keepAllNodesThatMatch(new NotFilter(new HasChildFilter(new StringFilter("DISCLAIMER"), true)));
								String resp = bodyList.toHtml();
								resp = resp.replaceAll("(?is)</?body[^>]*>", "");
								resp = resp.replaceAll("(?is)<form[^>]*>", "");
								resp = resp.replaceAll("(?is)</form[^>]*>.*", "");
								resp = resp.replaceAll("(?is)(<select)([^>]*>)", "$1 disabled=\"disabled\" $2");
								resp = resp.replaceAll("(?is)<a[^>]*>[^<]*PRINT THIS PAGE</a>", "");
								details += "<br><br><br>" + resp;
							}
							SelectTag select = (SelectTag) nodeList.extractAllNodesThatMatch(new TagNameFilter("select"), true)
												.extractAllNodesThatMatch(new HasAttributeFilter("id", "year")).elementAt(0);
							OptionTag[] options = select.getOptionTags();
							if (options != null){
								for (int o = options.length - 2; o >= 0; o--){
									String year = "&year=" + options[o].getValue();
									rsp = ((ro.cst.tsearch.connection.http2.OHCuyahogaConnTR) site).getPage(link + year, null);
									if (!rsp.contains("Data Not Available For Current Search")){
										rsp = Tidy.tidyParse(rsp, null);
										htmlParseoru = org.htmlparser.Parser.createParser(rsp, null);
										nodeList = htmlParseoru.parse(null);
										
										bodyList = nodeList.extractAllNodesThatMatch(new TagNameFilter("body"), true);
										if (bodyList != null && bodyList.size() > 0){
											bodyList.keepAllNodesThatMatch(new NotFilter(new TagNameFilter("div")), true);
											
											String resp = bodyList.toHtml();
											resp = resp.replaceAll("(?is)</?body[^>]*>", "");
											resp = resp.replaceAll("(?is)<form[^>]*>", "");
											resp = resp.replaceAll("(?is)</form[^>]*>.*", "");
											resp = resp.replaceAll("(?is)(<select)([^>]*>)", "$1 disabled=\"disabled\" $2");
											resp = resp.replaceAll("(?is)<a[^>]*>[^<]*PRINT THIS PAGE</a>", "");
											details += "<br><br><br>" + resp;
										}
									}
								}
							}
						}
					} finally {
						// always release the HttpSite
						HttpManager.releaseSite(site);
					}
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		
		
			
		details = details.replaceAll("(?is)<img[^>]*>", "");
		details = details.replaceAll("(?is)</?body[^>]*>", "");
		details = details.replaceAll("(?is)<a[^>]*>[^<]*</a>", "");
		details = details.replaceAll("(?is)\\|", "");
		
		return details;
	}
	
	@Override
	protected Object parseAndFillResultMap(ServerResponse response, String detailsHtml, ResultMap resultMap) {
		try {
			resultMap.put(OtherInformationSetKey.SRC_TYPE.getKeyName(), dataSite.getSiteTypeAbrev());
			detailsHtml = detailsHtml.replaceAll("(?is)&nbsp;", " ").replaceAll("(?is)&amp;", " ")
									.replaceAll("(?is)</?p[^>]*>", "").replaceAll("(?is)</?strong[^>]*>", "");
						
			org.htmlparser.Parser htmlParser = org.htmlparser.Parser.createParser(detailsHtml, null);
			NodeList mainList = htmlParser.parse(null);
			
			String pid = HtmlParser3.getValueFromAbsoluteCell(1, 0,  HtmlParser3.findNode(mainList, "Parcel Number"), "", true).trim();
			if (StringUtils.isNotEmpty(pid)){
				resultMap.put(PropertyIdentificationSetKey.PARCEL_ID.getKeyName(), pid);
			}
			
			String address = HtmlParser3.getValueFromAbsoluteCell(1, 0,  HtmlParser3.findNode(mainList, "Parcel Address"), "", true).trim();
			if (StringUtils.isNotEmpty(address)){
				resultMap.put("tmpAddress", address);
			}
			
			String legalDesc = HtmlParser3.getValueFromAbsoluteCell(1, 0,  HtmlParser3.findNode(mainList, "Property Description"), "", true).trim();
			if (StringUtils.isNotEmpty(legalDesc)){
				resultMap.put("tmpLegal", legalDesc.trim());
			}
			
			String owner = HtmlParser3.getValueFromAbsoluteCell(1, 0,  HtmlParser3.findNode(mainList, "Property Owner"), "", true).trim();
			if (StringUtils.isNotEmpty(owner)){
				resultMap.put("tmpOwner", owner.trim());
			}

			String totalDue = HtmlParser3.getValueFromNextCell(mainList, "TOTAL DUE", "", true);
			if (StringUtils.isNotEmpty(totalDue)){
				totalDue = totalDue.toString().replaceAll("[\\$,]+", "");
				resultMap.put(TaxHistorySetKey.TOTAL_DUE.getKeyName(), totalDue.trim());
			} else {
				resultMap.put(TaxHistorySetKey.TOTAL_DUE.getKeyName(), "0.00");
			}
			/*Calendar cal = Calendar.getInstance();
			int yearInt = cal.get(Calendar.YEAR);
			String currentYear = Integer.toString(yearInt);*/
			
			String payDate = DBManager.getDueOrPayDateBySiteName(dataSite.getSTCounty() + dataSite.getSiteTypeAbrev(), "payDate");
			String currentTaxYear = "";
			if (StringUtils.isNotEmpty(payDate)){
				currentTaxYear = payDate.substring(payDate.lastIndexOf("/") + 1, payDate.length());
				resultMap.put(TaxHistorySetKey.YEAR.getKeyName(), currentTaxYear.trim());
			}
			
			NodeList tblList = mainList.extractAllNodesThatMatch(new TagNameFilter("table"), true).
									extractAllNodesThatMatch(new HasChildFilter(new StringFilter("TOTAL PAYMENTS"), true));
			tblList.keepAllNodesThatMatch(new NotFilter(new HasChildFilter(new HasAttributeFilter("id", "year"), true)));
			
			if (tblList != null && tblList.size() > 1){
				for (int i = 0; i < tblList.size(); i++){
					TableTag table = (TableTag) tblList.elementAt(i);
					if (table != null && StringUtils.isNotEmpty(currentTaxYear) && currentTaxYear.matches("\\d{4}") && table.toHtml().contains("TAX YEAR " + currentTaxYear)){
						TableRow[] rows = table.getRows();
						
							if(rows.length > 0) {
								
								String landValue = HtmlParser3.getValueFromAbsoluteCell(1, 1,  
										HtmlParser3.findNode(tblList.elementAt(i).getChildren(), "ASSESSED VALUES"), "", true);
								if (StringUtils.isNotEmpty(landValue)){
									landValue = landValue.replaceAll("[\\$,]+", "").trim();
									resultMap.put(PropertyAppraisalSetKey.LAND_APPRAISAL.getKeyName(), landValue);
								}
								
								String buildingValue = HtmlParser3.getValueFromAbsoluteCell(2, 1,  
										HtmlParser3.findNode(tblList.elementAt(i).getChildren(), "ASSESSED VALUES"), "", true);
								if (StringUtils.isNotEmpty(buildingValue)){
									buildingValue = buildingValue.replaceAll("[\\$,]+", "").trim();
									resultMap.put(PropertyAppraisalSetKey.IMPROVEMENT_APPRAISAL.getKeyName(), buildingValue);
								}
								
								String totalValue = HtmlParser3.getValueFromAbsoluteCell(2, 1,  
										HtmlParser3.findNode(tblList.elementAt(i).getChildren(), "ASSESSED VALUES"), "", true);
								if (StringUtils.isNotEmpty(totalValue)){
									totalValue = totalValue.replaceAll("[\\$,]+", "").trim();
									resultMap.put(PropertyAppraisalSetKey.TOTAL_APPRAISAL.getKeyName(), totalValue);
									resultMap.put(PropertyAppraisalSetKey.TOTAL_ASSESSMENT.getKeyName(), totalValue);
								}
								
								String baseAmount = HtmlParser3
										.getValueFromNextCell(tblList.elementAt(i).getChildren(), "FULL YEAR CURRENT TAX", "", true).trim();
								if (StringUtils.isNotEmpty(baseAmount)){
									baseAmount = baseAmount.replaceAll("[\\$,]+", "").trim();
									resultMap.put(TaxHistorySetKey.BASE_AMOUNT.getKeyName(), baseAmount);
								}
								
								String amountPaid = HtmlParser3.getValueFromAbsoluteCell(0, 2,  
										HtmlParser3.findNode(tblList.elementAt(i).getChildren(), "TAX YEAR " + currentTaxYear), "", true).trim();
								if (StringUtils.isNotEmpty(amountPaid)){
									amountPaid = amountPaid.replaceAll("[\\$,]+", "").trim();
									resultMap.put(TaxHistorySetKey.AMOUNT_PAID.getKeyName(), amountPaid);
								}
								
								String priorDelinquent = HtmlParser3.getValueFromNextCell(tblList.elementAt(i).getChildren(), "DELINQUENT AMOUNT", "", true);
								if (StringUtils.isNotEmpty(priorDelinquent)){
									priorDelinquent = priorDelinquent.toString().replaceAll("[\\$,]+", "").trim();
									resultMap.put(TaxHistorySetKey.PRIOR_DELINQUENT.getKeyName(), priorDelinquent);
								} else {
									resultMap.put(TaxHistorySetKey.PRIOR_DELINQUENT.getKeyName(), "0.00");
								}
								
								String totalCharges = HtmlParser3.getValueFromNextCell(tblList.elementAt(i).getChildren(), "TAX YEAR " + currentTaxYear, "", true).trim();
								if (StringUtils.isNotEmpty(totalCharges)){
									totalCharges = totalCharges.replaceAll("[\\$,]+", "").trim();
								}
								
								String dueAmount = HtmlParser3.getValueFromAbsoluteCell(0, 3,  
												HtmlParser3.findNode(tblList.elementAt(i).getChildren(), "TAX YEAR " + currentTaxYear), "", true).trim();
								if (StringUtils.isNotEmpty(dueAmount)){
									dueAmount = dueAmount.replaceAll("[\\$,]+", "").trim();
								}
								
								if (StringUtils.isNotEmpty(totalCharges)){
									if (StringUtils.isNotEmpty(priorDelinquent)){
										if (StringUtils.isNotEmpty(amountPaid)){
											String delinqRemain = GenericFunctions.sum(amountPaid + "+-" + priorDelinquent, searchId);
											if ("0.00".equals(delinqRemain)){
												
												resultMap.put(TaxHistorySetKey.PRIOR_DELINQUENT.getKeyName(), "0.00");
												resultMap.put(TaxHistorySetKey.AMOUNT_PAID.getKeyName(), "0.00");
												
											} else if (delinqRemain.startsWith("-")){
												
												dueAmount = GenericFunctions.sum(dueAmount + "+" + delinqRemain, searchId);

												delinqRemain = delinqRemain.replaceAll("[\\-]+", "");
												resultMap.put(TaxHistorySetKey.PRIOR_DELINQUENT.getKeyName(), delinqRemain.trim());
												
												resultMap.put(TaxHistorySetKey.AMOUNT_PAID.getKeyName(), "0.00");
												resultMap.put(TaxHistorySetKey.TOTAL_DUE.getKeyName(), dueAmount.trim());
											} 
//											else if (!delinqRemain.startsWith("-") && !"0.00".equals(delinqRemain)){
//												
//												resultMap.put(TaxHistorySetKey.PRIOR_DELINQUENT.getKeyName(), "0.00");
//												resultMap.put(TaxHistorySetKey.AMOUNT_PAID.getKeyName(), delinqRemain);
//											}
//											resultMap.put(TaxHistorySetKey.TOTAL_DUE.getKeyName(), dueAmount.trim());
										}
									}
								}
								break;
							}
					}
				}
			}
			/*if (tblList != null && tblList.size() > 1){
				for (int i = 0; i < tblList.size(); i++){
					TableTag table = (TableTag) tblList.elementAt(i);
					if (table != null){
						TableRow[] rows = table.getRows();
						
						if(rows.length > 0) {
							String taxPayYear = HtmlParser3.getValueFromAbsoluteCell(1, 0,  
									HtmlParser3.findNode(tblList.elementAt(i).getChildren(), "TOTAL CHARGES"), "", true).trim();
							taxPayYear = taxPayYear.replaceAll("(?is)</?b[^>]*>", "");
							if (taxPayYear.matches("(?is)\\s*TAX\\s+YEAR\\s+\\d{4}\\s* /\\s*PAY\\s*" + currentYear + "\\s*")){
								
								String taxYear = taxPayYear.replaceAll("(?is)\\A\\s*TAX\\s+YEAR\\s+(\\d{4})\\s* /\\s*PAY\\s*" + currentYear + "\\s*$", "$1");
								resultMap.put(TaxHistorySetKey.YEAR.getKeyName(), taxYear.trim());
											
								//String baseAmount = HtmlParser3.getValueFromAbsoluteCell(1, 1,  
											//HtmlParser3.findNode(tblList.elementAt(i).getChildren(), "TOTAL CHARGES"), "", true).trim();
								String baseAmount = HtmlParser3.getValueFromNextCell(tblList.elementAt(i).getChildren(), "FULL YEAR CURRENT TAX", "", true);
								if (StringUtils.isNotEmpty(baseAmount)){
									baseAmount = baseAmount.replaceAll("[\\$,]+", "").trim();
									resultMap.put(TaxHistorySetKey.BASE_AMOUNT.getKeyName(), baseAmount);
									break;
								}
							}
						}
					}
				}
			}*/
			tblList = mainList.extractAllNodesThatMatch(new TagNameFilter("table"), true).
									extractAllNodesThatMatch(new HasChildFilter(new StringFilter("CHARGE AND PAYMENT DETAIL"), true));
			
			if (tblList != null && tblList.size() > 1){
				List<List> bodyInstallments = new ArrayList<List>();
				List<List> bodyInstallmentsSA = new ArrayList<List>();
				
				for (int i = 0; i < tblList.size(); i++){
					TableTag table = (TableTag) tblList.elementAt(i);
					if (table != null){
						TableRow[] rows = table.getRows();
						
						if (rows.length > 0) {
							String firstRow = rows[0].getColumns()[0].getChildrenHTML();

							String nextYear = "";
							try {
								nextYear = Integer.toString(Integer.parseInt(currentTaxYear) + 1);
							} catch (Exception e) {
							}
							if (firstRow.contains(currentTaxYear + " (pay in " + nextYear + ") CHARGE AND PAYMENT DETAIL")){
								
								List<String> firstInstall = new ArrayList<String>();
								List<String> secondInstall = new ArrayList<String>();
								String ba1 = "0.00", ap1  = "0.00", ad1 = "0.00", pa1 = "0.00";
								String ba2 = "0.00", ap2  = "0.00", ad2 = "0.00", pa2 = "0.00";
								
								List<String> firstInstallSA = new ArrayList<String>();
								List<String> secondInstallSA = new ArrayList<String>();
								String baSA1 = "0.00", apSA1  = "0.00", adSA1 = "0.00", paSA1 = "0.00";
								String baSA2 = "0.00", apSA2  = "0.00", adSA2 = "0.00", paSA2 = "0.00";
								
								String priorPaid = "0.00";
								
								boolean nextIsCountyTaxes = false;
								for (int r = 1; r < rows.length; r++){
									TableColumn[] colos = rows[r].getColumns();
									String taxset = colos[0].getStringText().toLowerCase();
									String taxsetFromNextRow = "";
									if (r > 2 && rows.length > (r + 1)){
										taxsetFromNextRow = rows[r + 1].getColumns()[0].getStringText().toLowerCase().trim();
									}
									String chargeType = colos[1].getStringText().toLowerCase();
									
									if (taxset.contains("taxset")){
										nextIsCountyTaxes = true;
										continue;
									}
									
									if (chargeType.equalsIgnoreCase("DELQ BALANCE")){
										if (colos.length > 3){
											String pp = colos[3].getStringText().toLowerCase();
											if (StringUtils.isNotEmpty(pp)){
												pp = pp.replaceAll("[\\$,]+", "").trim();
												priorPaid += "+" + pp;
											}
										}
									}
									//parse general installments
									if (nextIsCountyTaxes){
										if (chargeType.equalsIgnoreCase("1st half tax")){
											if (colos.length > 1){
												ba1 = colos[2].getStringText().toLowerCase();
												if (StringUtils.isNotEmpty(ba1)){
													ba1 = ba1.replaceAll("[\\$,]+", "").trim();
												}
											}
											if (colos.length > 2){
												ap1 = colos[3].getStringText().toLowerCase();
												if (StringUtils.isNotEmpty(ap1)){
													ap1 = ap1.replaceAll("[\\$,]+", "").trim();
												}
											}
											if (colos.length > 3){
												ad1 = colos[4].getStringText().toLowerCase();
												if (StringUtils.isNotEmpty(ad1)){
													ad1 = ad1.replaceAll("[\\$,]+", "").trim();
												}
											}
										}
										if (chargeType.equalsIgnoreCase("1st half penalty")){
											if (colos.length > 3){
												pa1 = colos[4].getStringText().toLowerCase();
												if (StringUtils.isNotEmpty(pa1)){
													pa1 = pa1.replaceAll("[\\$,]+", "").trim();
												}
											}
										}
										if (chargeType.equalsIgnoreCase("2nd half tax")){
											if (colos.length > 1){
												ba2 = colos[2].getStringText().toLowerCase();
												if (StringUtils.isNotEmpty(ba2)){
													ba2 = ba2.replaceAll("[\\$,]+", "").trim();
												}
											}
											if (colos.length > 2){
												ap2 = colos[3].getStringText().toLowerCase();
												if (StringUtils.isNotEmpty(ap2)){
													ap2 = ap2.replaceAll("[\\$,]+", "").trim();
												}
											}
											if (colos.length > 3){
												ad2 = colos[4].getStringText().toLowerCase();
												if (StringUtils.isNotEmpty(ad2)){
													ad2 = ad2.replaceAll("[\\$,]+", "").trim();
												}
											}
										}
										if (chargeType.equalsIgnoreCase("2nd half penalty")){
											if (colos.length > 3){
												pa2 = colos[4].getStringText().toLowerCase();
												if (StringUtils.isNotEmpty(pa2)){
													pa2 = pa2.replaceAll("[\\$,]+", "").trim();
												}
											}
										}
										if (StringUtils.isNotEmpty(taxsetFromNextRow)){
											firstInstall.add(ba1);
											firstInstall.add(ap1);
											firstInstall.add(ad1);
											firstInstall.add(pa1);
											if ("0.00".equals(ad1)){
												firstInstall.add("PAID");
											} else{
												firstInstall.add("UNPAID");
											}
											if (firstInstall.size() == 5 && bodyInstallments.size() == 0){
												bodyInstallments.add(firstInstall);
											}
											
											secondInstall.add(ba2);
											secondInstall.add(ap2);
											secondInstall.add(ad2);
											secondInstall.add(pa2);
											if ("0.00".equals(ad2)){
												secondInstall.add("PAID");
											} else{
												secondInstall.add("UNPAID");
											}
											if (secondInstall.size() == 5 && bodyInstallments.size() == 1){
												bodyInstallments.add(secondInstall);
											}
										}
										//i am off general installments
										if (firstInstall.size() == 5 && secondInstall.size() == 5){
											nextIsCountyTaxes = false;
											continue;
										}
									} else{//parse special assessments installments
										if (chargeType.equalsIgnoreCase("1st half tax")
												|| chargeType.equalsIgnoreCase("1st half SPA fee")){
											if (colos.length > 1){
												String ba = colos[2].getStringText().toLowerCase();
												if (StringUtils.isNotEmpty(ba)){
													ba = ba.replaceAll("[\\$,]+", "").trim();
													baSA1 += "+" + ba;
												}
											}
											if (colos.length > 2){
												String ap = colos[3].getStringText().toLowerCase();
												if (StringUtils.isNotEmpty(ap)){
													ap = ap.replaceAll("[\\$,]+", "").trim();
													apSA1 += "+" + ap;
												}
											}
											if (colos.length > 3){
												String ad = colos[4].getStringText().toLowerCase();
												if (StringUtils.isNotEmpty(ad)){
													ad = ad.replaceAll("[\\$,]+", "").trim();
													adSA1 += "+" + ad;
												}
											}
										} else if (chargeType.equalsIgnoreCase("1st half SPA fee penalty")
														|| chargeType.equalsIgnoreCase("1st half penalty")){
											if (colos.length > 3){
												String penalty = colos[4].getStringText().toLowerCase();
												if (StringUtils.isNotEmpty(penalty)){
													penalty = penalty.replaceAll("[\\$,]+", "").trim();
													paSA1 += "+" + penalty;
												}
											}
										}
										if (chargeType.equalsIgnoreCase("2nd half tax")
												|| chargeType.equalsIgnoreCase("2nd half SPA fee")){
											if (colos.length > 1){
												String ba = colos[2].getStringText().toLowerCase();
												if (StringUtils.isNotEmpty(ba)){
													ba = ba.replaceAll("[\\$,]+", "").trim();
													baSA2 += "+" + ba;
												}
											}
											if (colos.length > 2){
												String ap = colos[3].getStringText().toLowerCase();
												if (StringUtils.isNotEmpty(ap)){
													ap = ap.replaceAll("[\\$,]+", "").trim();
													apSA2 += "+" + ap;
												}
											}
											if (colos.length > 3){
												String ad = colos[4].getStringText().toLowerCase();
												if (StringUtils.isNotEmpty(ad)){
													ad = ad.replaceAll("[\\$,]+", "").trim();
													adSA2 += "+" + ad;
												}
											}
										} else if (chargeType.equalsIgnoreCase("2nd half SPA fee penalty")
														|| chargeType.equalsIgnoreCase("2nd half penalty")){
											if (colos.length > 3){
												String penalty = colos[4].getStringText().toLowerCase();
												if (StringUtils.isNotEmpty(penalty)){
													penalty = penalty.replaceAll("[\\$,]+", "").trim();
													paSA2 += "+" + penalty;
												}
											}
										}
									}
								}
								if (StringUtils.isNotEmpty(baSA1)){
									baSA1 = baSA1.replaceFirst("(?is)\\s*\\+\\s*\\+\\s*", "+");
									baSA1 = GenericFunctions.sum(baSA1, searchId);
									
									apSA1 = apSA1.replaceFirst("(?is)\\s*\\+\\s*\\+\\s*", "+");
									apSA1 = GenericFunctions.sum(apSA1, searchId);
									
									adSA1 = adSA1.replaceFirst("(?is)\\s*\\+\\s*\\+\\s*", "+");
									adSA1 = GenericFunctions.sum(adSA1, searchId);
									
									paSA1 = paSA1.replaceFirst("(?is)\\s*\\+\\s*\\+\\s*", "+");
									paSA1 = GenericFunctions.sum(paSA1, searchId);
									
									firstInstallSA.add(baSA1);
									firstInstallSA.add(apSA1);
									firstInstallSA.add(adSA1);
									firstInstallSA.add(paSA1);
									if ("0.00".equals(adSA1)){
										firstInstallSA.add("PAID");
									} else{
										firstInstallSA.add("UNPAID");
									}
									if (firstInstallSA.size() == 5 && bodyInstallmentsSA.size() == 0){
										bodyInstallmentsSA.add(firstInstallSA);
									}
								}
								if (StringUtils.isNotEmpty(baSA2)){
									baSA2 = baSA2.replaceFirst("(?is)\\s*\\+\\s*\\+\\s*", "+");
									baSA2 = GenericFunctions.sum(baSA2, searchId);
									
									apSA2 = apSA2.replaceFirst("(?is)\\s*\\+\\s*\\+\\s*", "+");
									apSA2 = GenericFunctions.sum(apSA2, searchId);
									
									adSA2 = adSA2.replaceFirst("(?is)\\s*\\+\\s*\\+\\s*", "+");
									adSA2 = GenericFunctions.sum(adSA2, searchId);
									
									paSA2 = paSA2.replaceFirst("(?is)\\s*\\+\\s*\\+\\s*", "+");
									paSA2 = GenericFunctions.sum(paSA2, searchId);
									
									secondInstallSA.add(baSA2);
									secondInstallSA.add(apSA2);
									secondInstallSA.add(adSA2);
									secondInstallSA.add(paSA2);
									if ("0.00".equals(adSA2)){
										secondInstallSA.add("PAID");
									} else{
										secondInstallSA.add("UNPAID");
									}
									if (secondInstallSA.size() == 5 && bodyInstallmentsSA.size() == 1){
										bodyInstallmentsSA.add(secondInstallSA);
									}
								}
								priorPaid = GenericFunctions.sum(priorPaid, searchId);
								if (StringUtils.isNotEmpty(priorPaid) && !"0.00".equals(priorPaid)){
									String priorDelinq = (String) resultMap.get(TaxHistorySetKey.PRIOR_DELINQUENT.getKeyName());
									if (StringUtils.isNotEmpty(priorDelinq)){
										priorDelinq = GenericFunctions.sum(priorDelinq + "+-" + priorPaid, searchId);
										resultMap.put(TaxHistorySetKey.PRIOR_DELINQUENT.getKeyName(), priorDelinq);
									}
									String amountPaid = (String) resultMap.get(TaxHistorySetKey.AMOUNT_PAID.getKeyName());
									if (StringUtils.isNotEmpty(amountPaid)){
										amountPaid = GenericFunctions.sum(amountPaid + "+-" + priorPaid, searchId);
										resultMap.put(TaxHistorySetKey.AMOUNT_PAID.getKeyName(), amountPaid);
									}
								}
							}
						}
					}
				}
				
				if (!bodyInstallments.isEmpty()){
					String[] header = {"BaseAmount", "AmountPaid", "TotalDue", "PenaltyAmount", "Status"};
					Map<String, String[]> map = new HashMap<String, String[]>();
					map.put("BaseAmount", new String[] { "BaseAmount", "" });
					map.put("AmountPaid", new String[] { "AmountPaid", "" });
					map.put("TotalDue", new String[] { "TotalDue", "" });
					map.put("PenaltyAmount", new String[] { "PenaltyAmount", "" });
					map.put("Status", new String[] { "Status", "" });
	
					ResultTable installments = new ResultTable();
					installments.setHead(header);
					installments.setBody(bodyInstallments);
					installments.setMap(map);
					resultMap.put("TaxInstallmentSet", installments);
				}
				if (!bodyInstallmentsSA.isEmpty()){
					String[] header = {"BaseAmount", "AmountPaid", "TotalDue", "PenaltyAmount", "Status"};
					Map<String, String[]> map = new HashMap<String, String[]>();
					map.put("BaseAmount", new String[] { "BaseAmount", "" });
					map.put("AmountPaid", new String[] { "AmountPaid", "" });
					map.put("TotalDue", new String[] { "TotalDue", "" });
					map.put("PenaltyAmount", new String[] { "PenaltyAmount", "" });
					map.put("Status", new String[] { "Status", "" });
	
					ResultTable installments = new ResultTable();
					installments.setHead(header);
					installments.setBody(bodyInstallmentsSA);
					installments.setMap(map);
					resultMap.put("SpecialAssessmentSet", installments);
				}
			}
			
			parseAddressOHCuyahogaTR(resultMap, searchId);
			partyNamesOHCuyahogaTR(resultMap, searchId);
			//parseLegalOHCuyahogaTR(resultMap, searchId);
			
		} catch (Exception e) {
			e.printStackTrace();
		}
		return null;
	}
	
	@SuppressWarnings("rawtypes")
	public static void partyNamesOHCuyahogaTR(ResultMap resultMap, long searchId) throws Exception {
		
		String stringOwner = (String) resultMap.get("tmpOwner");
		
		if (StringUtils.isEmpty(stringOwner))
			return;		
		
		stringOwner = GenericFunctions2.cleanOwnerNameFromPrefix(stringOwner);
		stringOwner = GenericFunctions2.resolveOtherTypes(stringOwner);
		stringOwner = stringOwner.replaceAll("[\\(\\)]+", "");
		stringOwner = stringOwner.replaceAll("(?is)(?is)\\s+([JRS]{2}\\.?),\\s*(\\w+)", " $2 $1 ");
		
		List<List> body = new ArrayList<List>();
		String[] names = {"", "", "", "", "", ""};
		String[] suffixes, types, otherTypes;
		
		String[] ownerName = stringOwner.split("\\s*<br>\\s*");
		//for 95205047: PROBERT, SUSAN J. & DONALD J. & CAROL S.
		if (ownerName.length == 1 
				&& ownerName[0].matches("(?is)\\A([A-Z]{2,}\\s*,?)\\s+(\\w+\\s+\\w+\\.?\\s*)&\\s*(\\w+\\s+\\w+\\.?\\s*&\\s*\\w+\\s+\\w+\\.?)")){
			stringOwner = stringOwner
					.replaceAll("(?is)\\A([A-Z]{2,}\\s*,?)\\s+(\\w+\\s+\\w+\\.?\\s*)&\\s*(\\w+\\s+\\w+\\.?\\s*&\\s*\\w+\\s+\\w+\\.?\\s*<br>)", 
							"$1 $2<br>$1 $3");
			ownerName = stringOwner.split("\\s*<br>\\s*");
		}
		
		for (String name : ownerName) {
			name = name.replaceAll("\\s*,\\s*$", "");
			names = StringFormats.parseNameNashville(name, true);
			
			types = GenericFunctions.extractAllNamesType(names);
			otherTypes = GenericFunctions.extractAllNamesOtherType(names);
			suffixes = GenericFunctions.extractNameSuffixes(names);
			GenericFunctions.addOwnerNames(names, suffixes[0], suffixes[1], types, otherTypes, 
											NameUtils.isCompany(names[2]), NameUtils.isCompany(names[5]), body);
		}

		GenericFunctions.storeOwnerInPartyNames(resultMap, body, true);
		
		resultMap.put(PropertyIdentificationSetKey.NAME_ON_SERVER.getKeyName(), stringOwner.replaceAll("(?is)\\s*<br>\\s*", " and "));	
	}
	
	public static void parseAddressOHCuyahogaTR(ResultMap resultMap, long searchId) throws Exception {
		
		String address = (String) resultMap.get("tmpAddress");
		
		if (StringUtils.isEmpty(address))
			return;
				
		if (address.trim().matches("0+"))
			return;
		
		String streetNuName = address.substring(0, address.indexOf(","));
		if (StringUtils.isNotEmpty(streetNuName)){
			resultMap.put(PropertyIdentificationSetKey.STREET_NO.getKeyName(), StringFormats.StreetNo(streetNuName.trim()));
			resultMap.put(PropertyIdentificationSetKey.STREET_NAME.getKeyName(), StringFormats.StreetName(streetNuName.trim()));
		}
		String city = address.substring(address.indexOf(","), address.lastIndexOf(","));
		if (StringUtils.isNotEmpty(city)){
			city = city.replaceAll(",+", "");
			resultMap.put(PropertyIdentificationSetKey.CITY.getKeyName(), city.trim());
		}
		
		String zip = address.substring(address.indexOf(", OH"), address.length());
		if (StringUtils.isNotEmpty(zip)){
			zip = zip.replaceAll("(?is),\\s*OH", "");
			resultMap.put(PropertyIdentificationSetKey.ZIP.getKeyName(), zip.trim());
		}
		
	}
	
	public static void parseLegalOHCuyahogaTR(ResultMap resultMap, long searchId) throws Exception {
		
		String legal = (String) resultMap.get("tmpLegal");
				
		if (StringUtils.isEmpty(legal))
			return;
		
		resultMap.put(PropertyIdentificationSetKey.PROPERTY_DESCRIPTION.getKeyName(), legal.replaceAll("\\s+", " "));
		
		if (legal.contains("Vehicles") || legal.contains("Business Personal Property"))
			return;
				
		legal = legal.replaceAll("(?is)&quot;", "\"");
		legal = legal.replaceAll("(?is)[\\)\\(]+", "");
		legal = legal.replaceAll("(?is)\\bPER SURVEY\\b", "");
		legal = legal.replaceAll("(?is)\\\"[^\\\"]+\\\"", "");
		legal = legal.replaceAll("(?is)[A-Z]\\s+[/\\d\\sX]+\\s*OF\\s+", "");
		legal = legal.replaceAll("(?is)\\s+(TR(ACT)?)\\b", " , $1");
		legal = legal.replaceAll("(?is)\\s+(PH(ASE)?)\\b", " , $1");
		//W 55 FT OF
		legal = legal.replaceAll("(?is)\\s+[NSEW]{1,2}\\s*\\d+\\s*F\\s*T\\s+OF\\s+", " ");
		//.024598 INT
		legal = legal.replaceAll("(?is)[\\d\\.]+\\s*(INT|AC)\\b", "");
		legal = legal.replaceAll("(?is)[\\(\\)]+", "");
		legal = legal.replaceAll("(?is)(\\d+)\\s*AND\\s*(\\d+)", "$1 & $2");
		legal = legal.replaceAll("(?is)\\bSALES CONTRACT\\b", "");
		
		legal = GenericFunctions.replaceNumbers(legal);
		String[] exceptionTokens = { "I", "M", "C", "L", "D" };
		legal = Roman.normalizeRomanNumbersExceptTokens(legal, exceptionTokens); // convert roman numbers
		
		String legalTemp = legal;
			
		// extract lot from legal description
		String lot = "";
		Pattern p = Pattern.compile("(?is)\\b(LO?TS?\\s*)\\s*([^\\s]+)\\b");
		Matcher ma = p.matcher(legal);
		while (ma.find()) {
			lot = lot + " " + ma.group(2).trim().replaceAll("\\A0+", "");
			legalTemp = legalTemp.replaceFirst(ma.group(0), ma.group(1) + " ");
		}
		lot += LegalDescription.extractLotFromText(legal);
		if (lot.length() != 0) {
			lot = LegalDescription.cleanValues(lot, false, true);
			resultMap.put(PropertyIdentificationSetKey.SUBDIVISION_LOT_NUMBER.getKeyName(), lot);
		}
		legalTemp = legalTemp.trim().replaceAll("\\s{2,}", " ");
		legal = legalTemp;
		
		// extract block from legal description
		String block = "";
		p = Pattern.compile("(?is)\\b(BLKS?\\s*)\\s*([^\\s]+)\\b");
		ma = p.matcher(legal);
		while (ma.find()) {
			block = block + " " + ma.group(2).trim().replaceAll("\\A0+", "");
			legalTemp = legalTemp.replaceFirst(ma.group(0), ma.group(1) + " ");
		}
		block = block.replaceAll("\\s*&\\s*", " ").replaceAll("\\bOF\\s+\\d+", "").trim();
		//19035.038.013.10
		block = block.replaceAll("(\\d/\\d)$", "");
		
		if (block.length() != 0) {
			block = LegalDescription.cleanValues(block, false, true);
			resultMap.put(PropertyIdentificationSetKey.SUBDIVISION_BLOCK.getKeyName(), block);
		}
		legalTemp = legalTemp.trim().replaceAll("\\s{2,}", " ");
		legal = legalTemp;
		
		p = Pattern.compile("(?is)\\b(A\\s*-|Abst|AB\\s+)\\s*(\\d+)\\b");
		ma = p.matcher(legal);
		if (ma.find()) {
			resultMap.put(PropertyIdentificationSetKey.ABS_NO.getKeyName(), ma.group(2).replaceAll("\\s*&\\s*", " ").trim());
			legal = legal.replaceAll(ma.group(2), "");
			legalTemp = legalTemp.replaceFirst(ma.group(0), ma.group(1) + " ");
			legalTemp = legalTemp.trim().replaceAll("\\s{2,}", " ");
			legal = legalTemp;
		}
		
		p = Pattern.compile("(?is)\\b(Acres)\\s*([^,]+)\\b");
		ma = p.matcher(legal);
		if (ma.find()) {
			resultMap.put(PropertyIdentificationSetKey.ACRES.getKeyName(), ma.group(2).replaceAll("\\s*&\\s*", " ").trim());
			legal = legal.replaceAll(ma.group(2), "");
			legalTemp = legalTemp.replaceFirst(ma.group(0), ma.group(1) + " ");
			legalTemp = legalTemp.trim().replaceAll("\\s{2,}", " ");
			legal = legalTemp;
		}
		
		// extract unit from legal description
		String unit = "";
		p = Pattern.compile("(?is)\\b(UNIT)\\s+([\\d]+[A-Z]?|[A-Z])\\b");
		ma = p.matcher(legal);
		while (ma.find()) {
			unit = unit + " " + ma.group(2);
			unit = unit.trim();
			legalTemp = legalTemp.replaceFirst(ma.group(0), ma.group(1) + " ");
			resultMap.put(PropertyIdentificationSetKey.SUBDIVISION_UNIT.getKeyName(), unit);// ma.group(2));
			legalTemp = legalTemp.trim().replaceAll("\\s{2,}", " ");
			legal = legalTemp;
		}
		
		// extract unit from legal description
		String phase = "";
		p = Pattern.compile("(?is)\\b(PH)\\s+([\\d]+[A-Z]?)\\b");
		ma = p.matcher(legal);
		while (ma.find()) {
			phase = phase + " " + ma.group(2);
			phase = phase.trim();
			legalTemp = legalTemp.replaceFirst(ma.group(0), ma.group(1) + " ");
			resultMap.put(PropertyIdentificationSetKey.SUBDIVISION_PHASE.getKeyName(), phase);// ma.group(2));
			legalTemp = legalTemp.trim().replaceAll("\\s{2,}", " ");
			legal = legalTemp;
		}

		// extract section from legal description
		p = Pattern.compile("(?is)\\b(SEC?(?:TION)?)\\s+(\\d+)\\b");
		ma = p.matcher(legal);
		if (ma.find()) {
			resultMap.put(PropertyIdentificationSetKey.SUBDIVISION_SECTION.getKeyName(), ma.group(2));
			legalTemp = legalTemp.replaceFirst(ma.group(0), ma.group(1) + " ");
			legalTemp = legalTemp.trim().replaceAll("\\s{2,}", " ");
			legal = legalTemp;
		}
		
		// extract section from legal description
		p = Pattern.compile("(?is)\\b(TR(?:ACT)?S?)\\s+(\\d+[A-Z]?(?:[\\d-]+)?(?:\\s*&\\s*\\d+[A-Z]?)?)\\b");
		ma = p.matcher(legal);
		if (ma.find()) {
			resultMap.put(PropertyIdentificationSetKey.SUBDIVISION_TRACT.getKeyName(), ma.group(2).replaceAll("\\s*&\\s*", " "));
			legalTemp = legalTemp.replaceFirst(ma.group(0), ma.group(1) + " ");
			legalTemp = legalTemp.trim().replaceAll("\\s{2,}", " ");
			legal = legalTemp;
		}

		// extract building #
		String bldg = "";
		p = Pattern.compile("(?is)\\b(BLDG)\\s+([A-Z]|\\d+)\\b");
		ma = p.matcher(legal);
		if (ma.find()) {
			bldg = bldg + " " + ma.group(2);
			bldg = bldg.trim();
			legalTemp = legalTemp.replaceFirst(ma.group(0), ma.group(1) + " ");
			resultMap.put(PropertyIdentificationSetKey.SUBDIVISION_BLDG.getKeyName(), bldg);
			legalTemp = legalTemp.trim().replaceAll("\\s{2,}", " ");
			legal = legalTemp;
		}
				
		// extract subdivision name from legal description
		String subdiv = "";

		
		p = Pattern.compile("(?is)\\b(BLK)(.*)\\b");
		ma = p.matcher(legal);
		if (ma.find()){
			subdiv = ma.group(2);
		} else {
			p = Pattern.compile("(?is)\\b(LT|ABST|TR)(.*)\\b");
			ma = p.matcher(legal);
			if (ma.find()){
				subdiv = ma.group(2);
			} else {
				p = Pattern.compile("(?is)\\A(.*)(SEC)\\b");
				ma = p.matcher(legal);
				if (ma.find()){
					subdiv = ma.group(1);
				} 
			}
		}
		
		if (subdiv.length() > 0) {
				
			subdiv = subdiv.replaceFirst("(?is)\\bSEC\\b.*", "");
			subdiv = subdiv.replaceFirst("(?is)\\bUNIT.*", "");
			resultMap.put(PropertyIdentificationSetKey.SUBDIVISION_NAME.getKeyName(), subdiv);
				
			if (legal.matches(".*\\bCOND.*"))
				resultMap.put(PropertyIdentificationSetKey.SUBDIVISION_COND.getKeyName(), subdiv);
			}
		}		
}
		
