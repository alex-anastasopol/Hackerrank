package ro.cst.tsearch.servers.types;


import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Vector;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.htmlparser.filters.TagNameFilter;
import org.htmlparser.tags.LinkTag;
import org.htmlparser.tags.TableColumn;
import org.htmlparser.tags.TableRow;
import org.htmlparser.tags.TableTag;
import org.htmlparser.util.NodeList;

import ro.cst.tsearch.bean.SearchAttributes;
import ro.cst.tsearch.connection.http.HTTPRequest;
import ro.cst.tsearch.connection.http.HTTPResponse;
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
import ro.cst.tsearch.search.filter.FilterResponse;
import ro.cst.tsearch.search.filter.newfilters.address.AddressFilterFactory;
import ro.cst.tsearch.search.filter.newfilters.address.AddressFromDocumentFilterForNext;
import ro.cst.tsearch.search.filter.newfilters.address.GenericAddressFilter;
import ro.cst.tsearch.search.filter.newfilters.name.NameFilterFactory;
import ro.cst.tsearch.search.filter.newfilters.pin.PINFilterFactory;
import ro.cst.tsearch.search.filter.newfilters.pin.PinFilterResponse;
import ro.cst.tsearch.search.iterator.ModuleStatesIteratorFactory;
import ro.cst.tsearch.search.module.ConfigurableNameIterator;
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
import ro.cst.tsearch.utils.StringUtils;

import com.stewart.ats.base.document.AssessorDocumentI;
import com.stewart.ats.base.document.DocumentI;
import com.stewart.ats.base.document.TaxDocumentI;

/**
 * @author mihaib
  */

public class AKAnchorageTR extends TSServer {

	public static final long serialVersionUID = 10000000L;
	private boolean downloadingForSave; 
	
	private static final Pattern PRIOR_YEARS_PAT = Pattern.compile("(?is)<a\\s+href[^,]+,\\s*'([^']*)'\\s*\\)\\s*\\\"\\s*>\\s*Prior\\s*Year");
	private static final Pattern PRIOR_YEARS_LINK_PAT = Pattern.compile("(?is)javascript:doTX09\\(([^,]+),'([^']+)'\\)\\\">Prior");
	
	private static final Pattern PID_FORM_PAGE_PAT = Pattern.compile("(?is)\\bvar\\s+p\\s*=\\s*'\\s*([\\d\\s]+)\\s*'\\s*;");
	
	private static final Pattern OPTION_PAT = Pattern.compile("(?is)<option[^>]+>(\\d+)");
	
	private static final Pattern dummyPattern = Pattern.compile("&dummy=([0-9]+)&");
	
	
	public AKAnchorageTR(long searchId) {
		super(searchId);
	}

	public AKAnchorageTR(String rsRequestSolverName, String rsSitePath, String rsServerID, String rsPrmNameLink, long searchId, int mid) {
		super(rsRequestSolverName, rsSitePath, rsServerID, rsPrmNameLink, searchId, mid);
	}

	@Override
	public ServerResponse SearchBy(TSServerInfoModule module, Object sd) throws ServerResponseException {
	
		// add dashes to pin if necessary 
        if(module.getModuleIdx() == TSServerInfo.PARCEL_ID_MODULE_IDX){
        	String pin = module.getFunction(0).getParamValue();
        	pin = pin.replaceAll("\\p{Punct}", "").replaceAll("\\s+", "");

           	module.getFunction(0).setParamValue(pin);
          
        }
        return super.SearchBy(module, sd);
	}
	
	@Override
	protected void setModulesForAutoSearch(TSServerInfo serverInfo) {
		List<TSServerInfoModule> modules = new ArrayList<TSServerInfoModule>();
		TSServerInfoModule module = null;

		PinFilterResponse pinFilter = PINFilterFactory.getDefaultPinFilter(searchId);
		GenericAddressFilter addressFilter 	= AddressFilterFactory.getGenericAddressHighPassFilter( searchId, 0.8d );
		FilterResponse defaultNameFilter = NameFilterFactory.getDefaultNameFilter(SearchAttributes.OWNER_OBJECT , searchId , module );
		
		AddressFromDocumentFilterForNext addressFilterForNext = new AddressFromDocumentFilterForNext(
    			getSearch().getSa().getAtribute(SearchAttributes.P_STREET_NO_NAME),searchId);
    	addressFilterForNext.setStrategyType(FilterResponse.STRATEGY_TYPE_HIGH_PASS);
    	addressFilterForNext.setThreshold(new BigDecimal(0.7));
		
		if (hasPin()){
			//Search by PIN
			module = new TSServerInfoModule(serverInfo.getModule(TSServerInfo.PARCEL_ID_MODULE_IDX));
			module.addFilterForNextType(FilterResponse.TYPE_CLEANED_PARCEL_FOR_NEXT);
			module.addFilter(pinFilter);
			modules.add(module);

		}
		
		if (hasStreet()) {
			//Search by Property Address
			module = new TSServerInfoModule(serverInfo.getModule(TSServerInfo.ADDRESS_MODULE_IDX));
			module.addFilterForNext(addressFilterForNext);
			module.addFilter(addressFilter);
			module.addFilter(defaultNameFilter);
			modules.add(module);
		}
		
		if (hasOwner()){
			//Search by Owner
			module = new TSServerInfoModule(serverInfo.getModule(TSServerInfo.NAME_MODULE_IDX));
			module.setSaObjKey(SearchAttributes.OWNER_OBJECT);
			module.clearSaKeys();
			module.addFilterForNextType(FilterResponse.TYPE_REGISTER_NAME_FOR_NEXT);
			module.addFilter(addressFilter);
			module.addFilter(defaultNameFilter);
			ConfigurableNameIterator nameIterator = (ConfigurableNameIterator) ModuleStatesIteratorFactory
										.getConfigurableNameIterator(module, searchId, new String[] {"L;F;"});
			module.addIterator(nameIterator);
			modules.add(module);
		}
		
		serverInfo.setModulesForAutoSearch(modules);		
	}
	
	@Override
	protected void ParseResponse(String sAction, ServerResponse Response, int viParseID) throws ServerResponseException {
		
		String initialResponse = Response.getResult();
		String rsResponse = initialResponse;
		String contents = "";
		ParsedResponse parsedResponse = Response.getParsedResponse();	
		
		switch (viParseID) {
		
			case ID_SEARCH_BY_NAME :
			case ID_SEARCH_BY_ADDRESS :
			case ID_SEARCH_BY_PARCEL :
				
				StringBuilder outputTable = new StringBuilder();
				
				if (rsResponse.indexOf("Your search did not match any records") != -1){
					Response.getParsedResponse().setError("<font color=\"red\">No results found</font>");
					return;
				}			
				try {
					org.htmlparser.Parser htmlParser = org.htmlparser.Parser.createParser(rsResponse, null);
					NodeList mainList = htmlParser.parse(null);
					NodeList tables = mainList.extractAllNodesThatMatch(new TagNameFilter("table"), true);
					if (tables.size() > 3){
						for (int i = tables.size() - 1; i > 0 ; i--){
							if (tables.elementAt(i).toHtml().toLowerCase().contains("parcel id")){
								contents = tables.elementAt(i).toHtml();
								break;
							}
						}
					}
				} catch (Exception e) {
					e.printStackTrace();
				}
				
				try {
					Collection<ParsedResponse> smartParsedResponses = smartParseIntermediary(Response, contents, outputTable);
					
					if(smartParsedResponses.size() > 0) {
						parsedResponse.setResultRows(new Vector<ParsedResponse>(smartParsedResponses));
						parsedResponse.setOnlyResponse(outputTable.toString());
						parsedResponse.setAttribute(ParsedResponse.SERVER_ROW_RESPONSE, outputTable.toString());
		            }
				}catch(Exception e) {
					e.printStackTrace();
				}
				break;
					
			case ID_DETAILS :
				
				String details = "", pid = "";
				details = getDetails(rsResponse, sAction);
				
				pid = StringUtils.extractParameter(details, "(?is)(\\d{3}\\s+\\d{3}\\s+\\d{2}\\s+\\d{3})").trim();
							
				if ((!downloadingForSave))
				{	
					
	                String qry_aux = Response.getRawQuerry();
					qry_aux = "dummy=" + pid + "&" + qry_aux;
					String originalLink = sAction + "&" + qry_aux;
					String sSave2TSDLink = getLinkPrefix(TSConnectionURL.idGET) + originalLink;

					HashMap<String, String> data = new HashMap<String, String>();
					if (TSServersFactory.isCountyTax(miServerID)){
						data.put("type","CNTYTAX");
						data.put("dataSource","TR");
					} 
					
					if(isInstrumentSaved(pid, null, data)){
		                details += CreateFileAlreadyInTSD();
					} else {
						mSearch.addInMemoryDoc(sSave2TSDLink, details);
						details = addSaveToTsdButton(details, sSave2TSDLink, viParseID);
					}
					
	                Response.getParsedResponse().setPageLink(
							new LinkInPage(
								sSave2TSDLink,
								originalLink,
								TSServer.REQUEST_SAVE_TO_TSD));
					parser.Parse(Response.getParsedResponse(), details,	Parser.NO_PARSE); 
	            } 
				else 
	            {
					Response.setResult(details);
					details = details.replaceAll("</?a[^>]*>", "");
					smartParseDetails(Response, details);
	                msSaveToTSDFileName = pid + ".html";
	                Response.getParsedResponse().setFileName(getServerTypeDirectory() + msSaveToTSDFileName);
	                msSaveToTSDResponce = details + CreateFileAlreadyInTSD();

				}
				
				break;	
			
			case ID_GET_LINK :
				if (sAction.indexOf("TX1P") != -1){
						ParseResponse(sAction, Response, ID_DETAILS);
				} else {
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
	
	protected String getDetails(String response, String sAction){
		
		// if from memory - use it as is
		if(!response.toLowerCase().contains("<html")){
			return response;
		}
		
		String contents = "";
		
		try {
			org.htmlparser.Parser htmlParser = org.htmlparser.Parser.createParser(response, null);
			NodeList mainList = htmlParser.parse(null);
			NodeList tables = mainList.extractAllNodesThatMatch(new TagNameFilter("table"), true);
			if (tables.size() > 3){
				boolean foundOwnerInfo = false, foundTaxInfo = false, foundOtherInfo = false, foundAccountStatus = false;
				for (int i = 1; i < tables.size() ; i++){
					if (tables.elementAt(i).toHtml().toLowerCase().contains("owner information") && !foundOwnerInfo){
						contents += "<br><br><br>" + tables.elementAt(i).toHtml();
						foundOwnerInfo = true;
					}
					if (tables.elementAt(i).toHtml().toLowerCase().contains("tax information") && !foundTaxInfo){
						contents += "<br><br><br>" + tables.elementAt(i).toHtml();
						foundTaxInfo = true;
					}
					if (tables.elementAt(i).toHtml().toLowerCase().contains("requesting tax information") && !foundOtherInfo){
						contents += "<br><br><br>" + tables.elementAt(i).toHtml();
						foundOtherInfo = true;
					}
					if (tables.elementAt(i).toHtml().toLowerCase().contains("tax account status") && !foundAccountStatus){
						contents += "<br><br><br>" + tables.elementAt(i).toHtml();
						foundAccountStatus = true;
					}
				}
			}
			
			Matcher mat = PRIOR_YEARS_PAT.matcher(response);
			if (mat.find()){
				String link = getBaseLink();
				
				Matcher matParcel = PID_FORM_PAGE_PAT.matcher(response);
				
				if (matParcel.find()){
					String p = matParcel.group(1);
					String p1, p2, p3, p4, parcel8, parcel11;
					
					if (" ".equals(p.substring(3, 4))){
						p1 = p.substring(0, 3);
						p2 = p.substring(4, 7);
						p3 = p.substring(8, 10);
						p4 = p.substring(11, 14);
						parcel8 = p1 + p2 + p3;
						parcel11 = p1 + p2 + p3 + p4;
					} else{
						parcel8 = p.substring(0,8);
						parcel11 = p.substring(0,11);
					}

					Matcher matLink = PRIOR_YEARS_LINK_PAT.matcher(response);
					if (matLink.find()){
				
						String part = "";
						if ("parcel8".equals(matLink.group(1))){
							part = parcel8;
						} else if ("parcel11".equals(matLink.group(1))){
							part = parcel11;
						}
						
						String pth = "/pw/dfhwbtta/TX1P+09" + part + matLink.group(2);
						String pid = sAction.replaceAll("(?is)[^\\+]+\\+\\d{2}([^$]+)", "$1");
						link = link.replaceAll("(?is)(\\.org).*", "$1" + pth);
						HTTPRequest req = new HTTPRequest(link, HTTPRequest.POST);
				    	
				    	HTTPResponse res = null;
			        	HttpSite site = HttpManager.getSite(HashCountyToIndex.getDateSiteForMIServerID(getCommunityId(), miServerID).getName(), searchId);
						try
						{
							res = site.process(req);
						} finally 
						{
							HttpManager.releaseSite(site);
						}
						String resp = res.getResponseAsString();
						HtmlParser3 parser = new HtmlParser3(resp);
						String select = parser.getNodeByAttribute("name", "YEARS", true).toHtml();
						mat = OPTION_PAT.matcher(select);
						int counter = 1;
						while (mat.find()){
							link = link.replaceAll("\\d{4}$", mat.group(1));
							req = new HTTPRequest(link, HTTPRequest.POST);
				        	site = HttpManager.getSite(HashCountyToIndex.getDateSiteForMIServerID(getCommunityId(), miServerID).getName(), searchId);
							try
							{
								res = site.process(req);
							} finally 
							{
								HttpManager.releaseSite(site);
							}
							resp = res.getResponseAsString();
							htmlParser = org.htmlparser.Parser.createParser(resp, null);
							mainList = htmlParser.parse(null);
							tables = mainList.extractAllNodesThatMatch(new TagNameFilter("table"), true);
							if (tables.size() > 3){
								if (counter == 1){
									contents += "<br><br>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;<b>Tax Details</b><br><br>";
								}
								for (int i = tables.size() - 1; i > 0 ; i--){
									if (tables.elementAt(i).toHtml().toLowerCase().contains("transaction")){
//										TableTag tabletag = (TableTag) tables.elementAt(i);
//										int c = 0;
//										for (TableRow row : tabletag.getRows()){
//											if (row.getColumnCount() > 0 && "".equals(row.getColumns()[0].toPlainTextString().trim())){
//												tabletag.removeChild(c);
//												c++;
//											}
//										}
										
										String table = tables.elementAt(i).toHtml();
										table = table.replaceAll("(?is)<tr[^>]*>\\s*<td[^>]*>\\s*</td.*?</tr>", "");
										table = table.replaceAll("(?is)<table\\s+", "<table id=\"detailsTax\" ");
										counter++;
										contents += "<br><br>" + table; 
										break;
									}
								}
							}
						}
					}
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
			
		contents = contents.replaceAll("(?is)</?body[^>]*>", "");
		contents = contents.replaceAll("(?is)See\\s+status\\s+below", "");
		contents = contents.replaceAll("(?is)Click\\s+for\\s+details", "");
		contents = contents.replaceAll("(?is)<input[^>]*>", "");
		contents = contents.replaceAll("(?is)</?a[^>]*>", "");
		contents = contents.replaceAll("(?is)<p[^>]*>.*?</p>", "");
		contents = contents.replaceAll("(?is)<img[^>]*>", "");
		contents = contents.replaceAll("(?is)<div[^>]*>\\s*</div>", "");
				
		return contents.trim();
	}
	
	public Collection<ParsedResponse> smartParseIntermediary(ServerResponse response, String table, StringBuilder outputTable) {
		
		 Vector<ParsedResponse> intermediaryResponse = new Vector<ParsedResponse>();
		
		try {
			
			if (table.contains("Parcel ID")){
				
				Form form = new SimpleHtmlParser(response.getResult()).getForm("GRW");
				Map<String, String> paramsLink = new SimpleHtmlParser(response.getResult()).getForm("GRW").getParams();
				if (paramsLink.containsKey("Parcel")){
					paramsLink.remove("Parcel");
				}
				if (form != null){
					paramsLink.put("action", form.action);
				}
				
				org.htmlparser.Parser htmlParser = org.htmlparser.Parser.createParser(table, null);
				NodeList mainTableList = htmlParser.parse(null);
				NodeList tableList = mainTableList.extractAllNodesThatMatch(new TagNameFilter("table"), true);
				if (tableList.size() > 0){
					TableTag mainTable = (TableTag)tableList.elementAt(0);
					TableRow[] rows = mainTable.getRows();
					
					//String crtCounty = InstanceManager.getManager().getCurrentInstance(searchId).getCurrentCounty().getName().toLowerCase();
					for(TableRow row : rows ) {
						if(row.getColumnCount() > 1) {
							
							TableColumn[] cols = row.getColumns();
							NodeList aList = cols[0].getChildren().extractAllNodesThatMatch(new TagNameFilter("a"), true);
							if (aList.size() > 0){
								String parcel = ((LinkTag) aList.elementAt(0)).extractLink();
								parcel = parcel.replaceAll("(?is)[^']+'([^']*)'\\)\\s*\\)", "$1").replaceAll("\\p{Punct}", "").trim();
								String link = CreatePartialLink(TSConnectionURL.idPOST) + "/pw/dfhwbtta/TX1P+02"  + parcel;
								
								String rowHtml =  row.toHtml();
								rowHtml = rowHtml.replaceAll("<a[^>]*>", "<a href=\"" + link + "\">");
								ParsedResponse currentResponse = new ParsedResponse();
								currentResponse.setAttribute(ParsedResponse.SERVER_ROW_RESPONSE, rowHtml);
								currentResponse.setOnlyResponse(rowHtml);
								currentResponse.setPageLink(new LinkInPage(link, link, TSServer.REQUEST_SAVE_TO_TSD));
								
								ResultMap m = parseIntermediaryRow(row, searchId, miServerID);
								m.removeTempDef();
								Bridge bridge = new Bridge(currentResponse, m, searchId);
								
								DocumentI document = null;
								if (TSServersFactory.isCountyTax(miServerID)){
									document = (TaxDocumentI)bridge.importData();
								} else if (TSServersFactory.isAssesor(miServerID)){
									document = (AssessorDocumentI)bridge.importData();
								}
								currentResponse.setDocument(document);
								
								intermediaryResponse.add(currentResponse);
							}
						}
					}
					//mSearch.setAdditionalInfo(getCurrentServerName() + ":paramsDetails:", paramsLink);
					
					String result = response.getResult();
					result = result.replaceAll("(?is)name\\s*=([A-Z]+)\\s+", "name=\"$1\" ");
					
					Map<String, String> paramsForNext = new SimpleHtmlParser(result).getForm("contform").getParams();
					if (paramsForNext != null){
						mSearch.setAdditionalInfo(getCurrentServerName() + ":paramsForNext:", paramsForNext);
					}
					
					String nextLink = "<a href=\"" + CreatePartialLink(TSConnectionURL.idPOST) + "/pw/gsweb\">Next</a>";
					response.getParsedResponse().setNextLink(nextLink);
					
					response.getParsedResponse().setHeader("&nbsp;&nbsp;&nbsp;" + nextLink + "<br><br>" 
															+ table.substring(table.indexOf("<table"), table.indexOf(">") + 1)
															+ rows[0].toHtml());
					response.getParsedResponse().setFooter("</table><br><br>");			
				
					outputTable.append(table);
				}
			}
			
		}catch(Exception e) {
			e.printStackTrace();
		}
		
		return intermediaryResponse;
	}
	
	protected ResultMap parseIntermediaryRow(TableRow row, long searchId, int miServerId) throws Exception {
		return ro.cst.tsearch.servers.functions.AKAnchorageAO.parseIntermediaryRowAKAnchorage(row, searchId, miServerId);
	}
	
	@SuppressWarnings("unchecked")
	@Override
	protected Object parseAndFillResultMap(ServerResponse response,String detailsHtml, ResultMap map) {
		detailsHtml = detailsHtml.replaceAll("(?is)&nbsp;", "").replaceAll("(?is)&amp;", "&").replaceAll("(?is)<br>", " ");
		detailsHtml = detailsHtml.replaceAll("(?is)<th[^>]*>\\s*<table[^>]*>\\s*<tr[^>]*>\\s*<td[^>]*>\\s*</td\\s*>\\s*<th[^>]*>([^<]*)</th\\s*>\\s*</tr\\s*>\\s*</table\\s*>\\s*(?:<td[^>]*>\\s*</td\\s*>)?\\s*</th\\s*>", 
												"<td>$1</td>");
		detailsHtml = detailsHtml.replaceAll("(?is)<th\\b", "<td ").replaceAll("(?is)</th\\b", "</td");

		map.put(OtherInformationSetKey.SRC_TYPE.getKeyName(), "TR");
			try {
				org.htmlparser.Parser htmlParser = org.htmlparser.Parser.createParser(detailsHtml, null);
				NodeList mainList = htmlParser.parse(null); 
				
				String pid = "";
				pid = StringUtils.extractParameter(detailsHtml, "(?is)(\\d{3}\\s+\\d{3}\\s+\\d{2}\\s+\\d{3})").trim();
				
				if (StringUtils.isNotEmpty(pid)) {
					map.put(PropertyIdentificationSetKey.PARCEL_ID.getKeyName(), pid);
				}
				
				String partialLegal = HtmlParser3.getValueFromAbsoluteCell(0, 2, HtmlParser3.findNode(mainList, "Legal Description"), "", true).trim();
				if (StringUtils.isNotEmpty(partialLegal)){
					if (partialLegal.trim().matches("T\\s*\\d+[A-Z]?\\s+R\\s*\\d+[A-Z]?\\s+S(EC)?\\s*\\d+")){
						//051-081-11-000
						map.put(PropertyIdentificationSetKey.SUBDIVISION_TOWNSHIP.getKeyName(), StringUtils.extractParameter(partialLegal, "T([^\\s]+)"));
						map.put(PropertyIdentificationSetKey.SUBDIVISION_RANGE.getKeyName(), StringUtils.extractParameter(partialLegal, "R([^\\s]+)"));
						map.put(PropertyIdentificationSetKey.SUBDIVISION_SECTION.getKeyName(), StringUtils.extractParameter(partialLegal, "S(?:EC)?\\s*(\\d+)"));
					} else {
						String subName = partialLegal;
						subName = subName.replaceAll("(?is)\\([^\\)]*\\)", "").trim();
						map.put(PropertyIdentificationSetKey.SUBDIVISION_NAME.getKeyName(), subName);
						if (subName.matches(".*\\bCOND.*"))
							map.put(PropertyIdentificationSetKey.SUBDIVISION_COND.getKeyName(), subName);
					}
				}
				int rowWithName = 1;
				String legal = "";
				legal += partialLegal + " ";
				
				while (!HtmlParser3.getValueFromAbsoluteCell(rowWithName, 0, HtmlParser3.findNode(mainList, "Legal Description"), "", true).contains("Site Address")){
					String str = HtmlParser3.getValueFromAbsoluteCell(rowWithName, 2, HtmlParser3.findNode(mainList, "Legal Description"), "", true);
					if (StringUtils.isNotEmpty(str)){
						legal += str + " ";
					}
					rowWithName++;
				}
				
				map.put("tmpLegal", legal);
				
				String siteAddress = HtmlParser3.getValueFromAbsoluteCell(0, 2, HtmlParser3.findNode(mainList, "Site Address"), "", true).trim();
				if (StringUtils.isNotEmpty(siteAddress)){
					map.put("tmpAddress", siteAddress.trim());
				}
				
				String owner = "";
				rowWithName = 1;
				owner = HtmlParser3.getValueFromAbsoluteCell(0, 2, HtmlParser3.findNode(mainList, "Account Name"), "", true).trim();
				owner += "###" + HtmlParser3.getValueFromAbsoluteCell(0, 2, HtmlParser3.findNode(mainList, "Mailing Address"), "", true).trim();
				
				for (; rowWithName < 3; rowWithName++){
					String str = HtmlParser3.getValueFromAbsoluteCell(rowWithName, 2, HtmlParser3.findNode(mainList, "Mailing Address"), "", true).trim();
					if (StringUtils.isNotEmpty(str)){
						owner += "###" + str;
					}
				}
				map.put("tmpOwner", owner);
				
				String taxYear = HtmlParser3.getValueFromAbsoluteCell(0, 1, HtmlParser3.findNode(mainList, "Tax Information"), "", true);
				taxYear = taxYear.replaceAll("(?is)</?font[^>]*>", "").trim().replaceAll("\\A(\\d{4}).*", "$1");
				map.put(TaxHistorySetKey.YEAR.getKeyName(), taxYear);
					
				String amountPaid = "0.00";
				NodeList taxDetails = HtmlParser3.getNodesByID("detailsTax", mainList, true);
				if (taxDetails != null){
					List<List> body = new ArrayList<List>();
					List<String> line = null;
					for (int i = 0; i < taxDetails.size(); i++ ){
						List<List<String>> taxTable = HtmlParser3.getTableAsList(taxDetails.toHtml(), false);
						for (List lst : taxTable){
							if (lst.get(0).toString().toLowerCase().contains("tax payment") 
									|| lst.get(0).toString().toLowerCase().contains("other fees/adj")){
								
								line = new ArrayList<String>();
								if (lst.size() > 0){
								line.add(lst.get(1).toString().trim());
								} else {
									line.add("");
								}
								if (lst.size() == 9){
									line.add(lst.get(8).toString().replaceAll("[-,\\$\\s]+", "").trim());
									if (i == 0){
										amountPaid += "+" + lst.get(3).toString().replaceAll("[,\\$\\s]+", "").trim();
									}
								} else {
									line.add("");
								}
								body.add(line);
							}
						}
						
					}
					if (body != null){
						ResultTable rt = new ResultTable();
						String[] header = {"ReceiptDate", "ReceiptAmount"};
						rt = GenericFunctions2.createResultTable(body, header);
						map.put("TaxHistorySet", rt);
					}
				}
				String baseAmount = HtmlParser3.getValueFromAbsoluteCell(0, 2, HtmlParser3.findNode(mainList, "Tax before Exemptions"), "", true);
				baseAmount = baseAmount.replaceAll("(?is)</?strong[^>]*>", "").replaceAll("(?is)</?font[^>]*>", "").replaceAll("(?is)[\\$,]", "").trim();
				map.put(TaxHistorySetKey.BASE_AMOUNT.getKeyName(), baseAmount);
				
				map.put(TaxHistorySetKey.AMOUNT_PAID.getKeyName(), GenericFunctions2.sum(amountPaid, searchId));
				
				String priorAmount = HtmlParser3.getValueFromAbsoluteCell(0, 6, HtmlParser3.findNode(mainList, "Prior Year"), "", true);
				priorAmount = priorAmount.replaceAll("(?is)</?strong[^>]*>", "").replaceAll("(?is)</?font[^>]*>", "").replaceAll("(?is)[\\$,]", "").trim();
				if (priorAmount.matches("\\.00")){
					priorAmount = "0.00";
				}
				map.put(TaxHistorySetKey.PRIOR_DELINQUENT.getKeyName(), priorAmount);
				
				String totalDue = HtmlParser3.getValueFromAbsoluteCell(0, 6, HtmlParser3.findNode(mainList, "Current Year"), "", true);
				totalDue = totalDue.replaceAll("(?is)</?strong[^>]*>", "").replaceAll("(?is)</?font[^>]*>", "").replaceAll("(?is)[\\$,]", "").trim();
				if (totalDue.matches("\\.00")){
					totalDue = "0.00";
				}
				map.put(TaxHistorySetKey.TOTAL_DUE.getKeyName(), totalDue);
				
			} catch(Exception e) {
				e.printStackTrace();
			}
			try {
				ro.cst.tsearch.servers.functions.AKAnchorageAO.parseAddressAKAnchorage(map, searchId);
				ro.cst.tsearch.servers.functions.AKAnchorageAO.parseLegalAKAnchorage(map, searchId);
				ro.cst.tsearch.servers.functions.AKAnchorageAO.partyNamesAKAnchorage(map, searchId);
					
			}catch(Exception e) {
				e.printStackTrace();
			}
			
			map.removeTempDef();
			
			
		return null;
	}
			
    /**
     * get file name from link
     */
	@Override
	protected String getFileNameFromLink(String link)
	{
		String fileName = "File.html";
		Matcher dummyMatcher = dummyPattern.matcher(link);
		if(dummyMatcher.find())
		{
			fileName = dummyMatcher.group(1);
			fileName = fileName + ".html";
		}
        return fileName;
    }
	
}