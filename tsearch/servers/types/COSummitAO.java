package ro.cst.tsearch.servers.types;


import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.Vector;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


import org.htmlparser.filters.TagNameFilter;
import org.htmlparser.tags.LinkTag;
import org.htmlparser.tags.TableColumn;
import org.htmlparser.tags.TableRow;
import org.htmlparser.tags.TableTag;
import org.htmlparser.util.NodeList;

import com.stewart.ats.base.document.AssessorDocumentI;
import com.stewart.ats.base.document.DocumentI;

import ro.cst.tsearch.Search;
import ro.cst.tsearch.bean.SearchAttributes;
import ro.cst.tsearch.connection.http.HTTPRequest;
import ro.cst.tsearch.connection.http.HTTPResponse;
import ro.cst.tsearch.connection.http2.HttpManager;
import ro.cst.tsearch.connection.http2.HttpSite;
import ro.cst.tsearch.exceptions.ServerResponseException;
import ro.cst.tsearch.extractor.xml.Bridge;
import ro.cst.tsearch.extractor.xml.ResultMap;
import ro.cst.tsearch.extractor.xml.StringFormats;
import ro.cst.tsearch.search.FunctionStatesIterator;
import ro.cst.tsearch.search.filter.FilterResponse;
import ro.cst.tsearch.search.filter.newfilters.address.AddressFilterFactory;
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
import ro.cst.tsearch.servers.response.ParsedResponse;
import ro.cst.tsearch.servers.response.Parser;
import ro.cst.tsearch.servers.response.ServerResponse;
import ro.cst.tsearch.utils.StringUtils;

/**
 * @author mihaib
  */

public class COSummitAO extends TSServer {

	public static final long serialVersionUID = 10000000L;
	private boolean downloadingForSave; 
	
	private static final Pattern dummyPattern = Pattern.compile("&dummy=([0-9]+)&");
	
	
	public COSummitAO(long searchId) {
		super(searchId);
	}

	public COSummitAO(String rsRequestSolverName, String rsSitePath, String rsServerID, String rsPrmNameLink, long searchId, int mid) {
		super(rsRequestSolverName, rsSitePath, rsServerID, rsPrmNameLink, searchId, mid);
	}
	
	@Override
	public ServerResponse SearchBy(TSServerInfoModule module, Object sd) throws ServerResponseException {
	
		// add dashes to pin if necessary 
       
        return super.SearchBy(module, sd);
	}
	
	@Override
	protected void setModulesForAutoSearch(TSServerInfo serverInfo) {
		List<TSServerInfoModule> modules = new ArrayList<TSServerInfoModule>();
		TSServerInfoModule module = null;

		String streetName = getSearch().getSa().getAtribute(SearchAttributes.P_STREETNAME);
		String pin = getSearch().getSa().getAtribute(SearchAttributes.LD_PARCELNO);
		GenericAddressFilter addressFilter 	= AddressFilterFactory.getGenericAddressHighPassFilter( searchId, 0.8d );
		PinFilterResponse pinFilter = PINFilterFactory.getDefaultPinFilter(searchId);
		//addressFilter.setEnableNumber(false);
		//addressFilter.setEnableDirection(true);
		//addressFilter.setThreshold(new BigDecimal(1));
		FilterResponse defaultNameFilter = NameFilterFactory.getDefaultNameFilter(SearchAttributes.OWNER_OBJECT, searchId, module);
		//((GenericNameFilter) defaultNameFilter).setIgnoreMiddleOnEmpty(true);
		    	
		if (hasPin()){
			//Search by PIN as Schedule Number
			module = new TSServerInfoModule(serverInfo.getModule(TSServerInfo.PARCEL_ID_MODULE_IDX));
			module.clearSaKeys();
			module.getFunction(0).forceValue(pin);
			modules.add(module);
		}
		if (hasPin()){
			//Search by PIN as PPI
			module = new TSServerInfoModule(serverInfo.getModule(TSServerInfo.INSTR_NO_MODULE_IDX));
			module.clearSaKeys();
			module.getFunction(0).forceValue(pin);
			modules.add(module);
		}
		
		if (hasStreet()) {
			//Search by Property Address
			module = new TSServerInfoModule(serverInfo.getModule(TSServerInfo.ADDRESS_MODULE_IDX));
			module.clearSaKeys();
			module.getFunction(0).forceValue(streetName);
			module.addFilter(addressFilter);
			module.addFilter(pinFilter);
			modules.add(module);
		}
		
		if (hasOwner()){
			//Search by Owner
			module = new TSServerInfoModule(serverInfo.getModule(TSServerInfo.NAME_MODULE_IDX));
			module.setSaObjKey(SearchAttributes.OWNER_OBJECT);
			module.clearSaKeys();
			module.addFilter(defaultNameFilter);
			module.addFilter(pinFilter);
			module.setIteratorType(0,FunctionStatesIterator.ITERATOR_TYPE_LAST_NAME_FAKE);
			ConfigurableNameIterator nameIterator = (ConfigurableNameIterator) ModuleStatesIteratorFactory
										.getConfigurableNameIterator(module, searchId, new String[] {"L;;"});
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
		
			case ID_SEARCH_BY_ADDRESS :
				if (rsResponse.indexOf("Search String Not Found") != -1){
					Response.getParsedResponse().setError("<font color=\"red\">No results found</font>");
					return;
				}			
				StringBuilder outputTable = new StringBuilder();
				
				try {
					org.htmlparser.Parser htmlParser = org.htmlparser.Parser.createParser(rsResponse, null);
					NodeList mainList = htmlParser.parse(null);
					NodeList tables = mainList.extractAllNodesThatMatch(new TagNameFilter("table"), true);
					if (tables.size() > 0){
						for (int i = 0; i < tables.size(); i++){
							if (tables.elementAt(i).toHtml().contains("QueryAddress")){
								contents = tables.elementAt(i).toHtml();
								break;
							}
						}
					}
				} catch (Exception e) {
					e.printStackTrace();
				}
				
				if (getSearch().getSearchType() == Search.PARENT_SITE_SEARCH ) {
					try {
						Collection<ParsedResponse> smartParsedResponses = smartParseIntermediaryIntermediary(Response, contents, outputTable);
						
						if(smartParsedResponses.size() > 0) {
							parsedResponse.setResultRows(new Vector<ParsedResponse>(smartParsedResponses));
							parsedResponse.setOnlyResponse(outputTable.toString());
							parsedResponse.setAttribute(ParsedResponse.SERVER_ROW_RESPONSE, outputTable.toString());
			            }
					}catch(Exception e) {
						e.printStackTrace();
					}
				} else {
					try {
						org.htmlparser.Parser htmlParser = org.htmlparser.Parser.createParser(contents, null);
						NodeList mainTableList = htmlParser.parse(null);
						NodeList tableList = mainTableList.extractAllNodesThatMatch(new TagNameFilter("table"), true);
						if (tableList.size() > 0){
							TableTag mainTable = (TableTag)tableList.elementAt(0);
							TableRow[] rows = mainTable.getRows();
							Set<String> links = new HashSet<String>(); 
							for(TableRow row : rows ) {
								if(row.getColumnCount() > 1) {
									TableColumn[] cols = row.getColumns();
									NodeList aList = cols[1].getChildren().extractAllNodesThatMatch(new TagNameFilter("a"), true);
									if (aList.size() > 0){
										String query = ((LinkTag) aList.elementAt(0)).extractLink();
										if (query.indexOf("javascript:QueryAddress") != -1){
											String road = StringUtils.extractParameter(query, "(?is)javascript:QueryAddress\\('([^']*)");
											String pre = StringUtils.extractParameter(query, "(?is)javascript:QueryAddress\\('[^']*'\\s*,\\s*'([^']*)");
											String suff = StringUtils.extractParameter(query, "(?is)javascript:QueryAddress\\('[^']*'\\s*,\\s*'[^']*'\\s*,\\s*'([^']*)");
											String sDir = StringUtils.extractParameter(query, "(?is)javascript:QueryAddress\\('[^']*'\\s*,\\s*'[^']*'\\s*,\\s*'[^']*'\\s*,\\s*'([^']*)");
											String town = StringUtils.extractParameter(query, "(?is)javascript:QueryAddress\\('[^']*'\\s*,\\s*'[^']*'\\s*,\\s*'[^']*'\\s*,\\s*'[^']*'\\s*,\\s*'([^']*)");
											String link = "RoadList.asp?Road="  + road + "&Pre=" + pre + "&Suff=" + suff + "&sDir=" + sDir + "&Town=" + town;
											links.add(link);
										} 
									}
								}
							}
							if (!links.isEmpty()){
								StringBuffer tabel = new StringBuffer("<table BORDER=\"0\" WIDTH=\"400\" CELLSPACING=\"2\" CELLPADDING=\"2\"><tr>"
											 + "<td ALIGN=\"CENTER\"><FONT SIZE=\"2\" FACE=\"VERDANA\"><u>Address</u></td></font><td align=\"right\"></td></tr>");
								String baseLink = getBaseLink();
								HTTPRequest req = null;
								HTTPResponse res = null;
								HttpSite site = HttpManager.getSite(HashCountyToIndex.getDateSiteForMIServerID(getCommunityId(), miServerID).getName(), searchId);
								for (String link : links){
									try {
										req = new HTTPRequest(baseLink + link, HTTPRequest.GET);
										res = site.process(req);
									} finally {
										HttpManager.releaseSite(site);
									}
										String resp = res.getResponseAsString();
										try {
											if (resp != null){
												htmlParser = org.htmlparser.Parser.createParser(resp, null);
												NodeList mainList = htmlParser.parse(null);
												NodeList tables = mainList.extractAllNodesThatMatch(new TagNameFilter("table"), true);
												if (tables.size() > 0){
													TableTag table = (TableTag)tables.elementAt(0);
													tabel.append(table.getChildren().toHtml());
												}
											}
										} catch (Exception e) {
											e.printStackTrace();
											HttpManager.releaseSite(site);
										}
									}
								
								tabel.append("</table>");
								try {
									Collection<ParsedResponse> smartParsedResponses = smartParseIntermediary(Response, tabel.toString(), outputTable);
									
									if(smartParsedResponses.size() > 0) {
										parsedResponse.setResultRows(new Vector<ParsedResponse>(smartParsedResponses));
										parsedResponse.setOnlyResponse(outputTable.toString());
										parsedResponse.setAttribute(ParsedResponse.SERVER_ROW_RESPONSE, outputTable.toString());
						            }
								}catch(Exception e) {
									e.printStackTrace();
								}
							}
						}
					} catch (Exception e) {
						e.printStackTrace();
					}
				}
				
			break;
			
			case ID_SEARCH_BY_SUBDIVISION_NAME :
				if (rsResponse.indexOf("Search String Not Found") != -1){
					Response.getParsedResponse().setError("<font color=\"red\">No results found</font>");
					return;
				}			
				outputTable = new StringBuilder();
				
				try {
					org.htmlparser.Parser htmlParser = org.htmlparser.Parser.createParser(rsResponse, null);
					NodeList mainList = htmlParser.parse(null);
					NodeList tables = mainList.extractAllNodesThatMatch(new TagNameFilter("table"), true);
					if (tables.size() > 0){
						for (int i = 0; i < tables.size(); i++){
							if (tables.elementAt(i).toHtml().contains("QuerySub")){
								contents = tables.elementAt(i).toHtml();
								break;
							}
						}
					}
				} catch (Exception e) {
					e.printStackTrace();
				}
				
				try {
					Collection<ParsedResponse> smartParsedResponses = smartParseIntermediaryIntermediary(Response, contents, outputTable);
					
					if(smartParsedResponses.size() > 0) {
						parsedResponse.setResultRows(new Vector<ParsedResponse>(smartParsedResponses));
						parsedResponse.setOnlyResponse(outputTable.toString());
						parsedResponse.setAttribute(ParsedResponse.SERVER_ROW_RESPONSE, outputTable.toString());
		            }
				}catch(Exception e) {
					e.printStackTrace();
				}
				
			break;
			
			case ID_SEARCH_BY_NAME :
			case ID_SEARCH_BY_PARCEL :
				
				outputTable = new StringBuilder();
				
				if (rsResponse.indexOf("Search String Not Found") != -1){
					Response.getParsedResponse().setError("<font color=\"red\">No results found</font>");
					return;
				}
				if (rsResponse.indexOf("Property Report") != -1){
					ParseResponse(sAction, Response, ID_DETAILS);
					break;
				}
				try {
					org.htmlparser.Parser htmlParser = org.htmlparser.Parser.createParser(rsResponse, null);
					NodeList mainList = htmlParser.parse(null);
					NodeList tables = mainList.extractAllNodesThatMatch(new TagNameFilter("table"), true);
					if (tables.size() > 0){
						for (int i = 0; i < tables.size(); i++){
							if (tables.elementAt(i).toHtml().contains("QuerySummit")){
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
				details = getDetails(rsResponse);
				
				pid = StringUtils.extractParameter(details, "(?i)Schedule\\s*#\\s*:\\s*([^<]*)").trim();
							
				if ((!downloadingForSave))
				{	
					
	                String qry_aux = Response.getRawQuerry();
					qry_aux = "dummy=" + pid + "&" + qry_aux;
					String originalLink = sAction + "&" + qry_aux;
					String sSave2TSDLink = getLinkPrefix(TSConnectionURL.idGET) + originalLink;

					HashMap<String, String> data = new HashMap<String, String>();
					data.put("type","ASSESSOR");
					data.put("dataSource","AO");
					
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
				if (sAction.indexOf("Somedata.asp") != -1){
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
	
	protected String getDetails(String response){
		
		// if from memory - use it as is
		if(!response.toLowerCase().contains("<html")){
			return response;
		}
		
		String contents = "";
		String propDetailLink = "";
		try {
			org.htmlparser.Parser htmlParser = org.htmlparser.Parser.createParser(response, null);
			NodeList mainList = htmlParser.parse(null);
			NodeList tables = mainList.extractAllNodesThatMatch(new TagNameFilter("table"), true);
			if (tables.size() > 0){
				for (int i = 0; i < tables.size(); i++){
					contents += tables.elementAt(i).toHtml();
					if (i == 0){
						NodeList aList = tables.elementAt(i).getChildren().extractAllNodesThatMatch(new TagNameFilter("a"), true);
						if (aList.size() > 0){
							propDetailLink = ((LinkTag) aList.elementAt(0)).extractLink();
						}
					}
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		
		if (StringUtils.isNotEmpty(propDetailLink)){
			String baseLink = getBaseLink();
			HTTPRequest req = new HTTPRequest(baseLink + propDetailLink, HTTPRequest.GET);
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
			try {
				if (resp != null){
					org.htmlparser.Parser htmlParser = org.htmlparser.Parser.createParser(resp, null);
					NodeList mainList = htmlParser.parse(null);
					NodeList tables = mainList.extractAllNodesThatMatch(new TagNameFilter("table"), true);
					if (tables.size() > 0){
						TableTag tablePD = (TableTag)tables.elementAt(0);
						tablePD.setAttribute("id", "propertyDetails");
						contents += "<br><br><br><h2>Property Details</h2><br>" + tablePD.toHtml();
						NodeList aList = tables.elementAt(0).getChildren().extractAllNodesThatMatch(new TagNameFilter("a"), true);
						if (aList.size() > 0){
							String estimatedTaxRateLink = ((LinkTag) aList.elementAt(0)).extractLink();
							if (StringUtils.isNotEmpty(estimatedTaxRateLink)){
								req = new HTTPRequest(baseLink + estimatedTaxRateLink, HTTPRequest.GET);
								res = null;
								try
								{
									res = site.process(req);
								} finally 
								{
									HttpManager.releaseSite(site);
								}
								resp = res.getResponseAsString();
								resp = resp.replaceAll("(?is)</?html>", "").replaceAll("(?is)</?head>", "").replaceAll("(?is)</?center>", "");
								contents += "<br><br><br>" + resp;
							}
						}
					}
				}
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		contents = contents.replaceAll("(?is)<a[^>]*>[^<]*</a>", "");
		contents = contents.replaceAll("(?is)</?a[^>]*>", "");
		contents = contents.replaceAll("(?is)<img[^>]*>", "");
						
		return contents.trim();
	}
	
	public Collection<ParsedResponse> smartParseIntermediary(ServerResponse response, String table, StringBuilder outputTable) {
		
		 Vector<ParsedResponse> intermediaryResponse = new Vector<ParsedResponse>();
		
		try {
				
				if (table.contains("Unit")){
					table = table.replaceAll("(?is)(</table[^>]*>)\\s*<table[^>]*>", "");
					table = table.replaceAll("(?is)(</table[^>]*>)\\s*</table[^>]*>", "$1");
					table = table.replaceAll("(?is)(</tr>)\\s*<table[^>]*>", "$1");
				}
				org.htmlparser.Parser htmlParser = org.htmlparser.Parser.createParser(table, null);
				NodeList mainTableList = htmlParser.parse(null);
				NodeList tableList = mainTableList.extractAllNodesThatMatch(new TagNameFilter("table"), true);
				if (tableList.size() > 0){
					boolean ppiSearch = false;
					TableTag mainTable = (TableTag)tableList.elementAt(0);
					TableRow[] rows = mainTable.getRows();
					String type = rows[0].getColumns()[0].toPlainTextString().trim().toLowerCase();

					for(TableRow row : rows ) {
						if(row.getColumnCount() > 1) {
							
							TableColumn[] cols = row.getColumns();
							if (cols[0].getChildren().toHtml().contains("Address"))
								continue;
							NodeList aList = cols[1].getChildren().extractAllNodesThatMatch(new TagNameFilter("a"), true);
							if (cols.length == 4){
								aList = cols[3].getChildren().extractAllNodesThatMatch(new TagNameFilter("a"), true);
								ppiSearch = true;
							}
							if (aList.size() > 0){
								String querySummit = ((LinkTag) aList.elementAt(0)).extractLink();
								String ppi = StringUtils.extractParameter(querySummit, "(?is)javascript:QuerySummit\\('([^']*)");
								String scheduleNo = StringUtils.extractParameter(querySummit, "(?is)javascript:QuerySummit\\('[^']*'\\s*,\\s*'?([^\\)]*)");
								scheduleNo = scheduleNo.replaceAll("(?is)[']+", "");
								String link = CreatePartialLink(TSConnectionURL.idGET);
								
								if (cols.length == 4){
									link += "Somedata.asp?PPI=PPI = %22"  + ppi + "%22&Schno=" + scheduleNo + "&Tool=1";
								} else {
									link += "Somedata.asp?PPI="  + ppi + "&Schno=" + scheduleNo + "&Tool=1";
								}
								String rowHtml =  row.toHtml();
								rowHtml = rowHtml.replaceAll("<a[^>]*>[^<]*<", "<a href=\"" + link + "\">" + scheduleNo + "<");
								rowHtml = rowHtml.replaceAll("(?is)<!--.*?-->", "").trim();
								rowHtml = rowHtml.replaceAll("(?is)</?font[^>]*>", "");
								rowHtml = rowHtml.replaceAll("(?is)(<td[^>]*>)([^<]*)(</td>)", "$1 <a href=\"" + link + "\">$2</a>$3");
								//rowHtml = rowHtml.replaceAll("(?is)(<\\s*/tr\\s*>)", "<td>" + scheduleNo + "</td></tr>");
								
								ParsedResponse currentResponse = new ParsedResponse();
								currentResponse.setAttribute(ParsedResponse.SERVER_ROW_RESPONSE, rowHtml);
								currentResponse.setOnlyResponse(rowHtml);
								currentResponse.setPageLink(new LinkInPage(link, link, TSServer.REQUEST_SAVE_TO_TSD));
								
								ResultMap resultMap = new ResultMap();
								resultMap.put("OtherInformationSet.SrcType", "AO");
								resultMap.put("PropertyIdentificationSet.ParcelID", scheduleNo.trim());
								if (type.equals("owner name")){
									resultMap.put("tmpOwner",  cols[0].toPlainTextString().trim());
									ro.cst.tsearch.servers.functions.COSummitAO.partyNamesIntermCOSummitAO(resultMap, searchId);
								} else if (type.equals("address")){
									String address = cols[0].toPlainTextString().trim();
									address = address.replaceAll("(?is)\\s+", " ");
									address = org.apache.commons.lang.StringUtils.stripStart(address, "0");
									resultMap.put("PropertyIdentificationSet.StreetNo", StringFormats.StreetNo(address.trim()));
									resultMap.put("PropertyIdentificationSet.StreetName", StringFormats.StreetName(address.trim()));
								} else if (type.equals("property")){
									if (cols.length == 4){
										resultMap.put("tmpOwner",  cols[0].toPlainTextString().trim());
										resultMap.put("PropertyIdentificationSet.SubdivisionBlock", 
																cols[1].toPlainTextString().replaceAll("(?is)\\bBlock\\b", "").replaceAll("(?is)\\A\\s*0+", "").trim());
										resultMap.put("PropertyIdentificationSet.SubdivisionUnit", 
												cols[2].toPlainTextString().replaceAll("(?is)\\bUnit\\b", "").replaceAll("(?is)\\A\\s*0+", "").trim());
										resultMap.put("tmpPPI", "PPISearch");
										ro.cst.tsearch.servers.functions.COSummitAO.partyNamesIntermCOSummitAO(resultMap, searchId);
									} else {
										String property = cols[0].toPlainTextString().trim();
										property = property.replaceAll("(?is)\\s+", " ");
										resultMap.put("PropertyIdentificationSet.PropertyDescription", property.trim());
									}
								}
								resultMap.removeTempDef();
								Bridge bridge = new Bridge(currentResponse, resultMap, searchId);
								DocumentI document = (AssessorDocumentI) bridge.importData();
								currentResponse.setDocument(document);
								
								intermediaryResponse.add(currentResponse);
							}
						}
					}
					String header = "";
					if (ppiSearch){
						header = rows[0].toHtml().replaceAll("(?is)<img[^>]*>", "")
												.replaceAll("(?is)(<\\s*/td\\s*>\\s*<\\s*/tr\\s*>)", 
														"<u>Block</u></td><td><u>Unit</u></td><td><u>Schedule No</u></td></tr>");
					} else {
						header = rows[0].toHtml().replaceAll("(?is)<img[^>]*>", "")
							.replaceAll("(?is)(<\\s*/td\\s*>\\s*<\\s*/tr\\s*>)", "<u>Schedule No</u></td></tr>");
					}
					
					response.getParsedResponse().setHeader("&nbsp;&nbsp;&nbsp;" +  "<br><br>" 
															+ table.substring(table.indexOf("<table"), table.indexOf(">") + 1)
															+ header);
					response.getParsedResponse().setFooter("</table><br><br>");			
				
					outputTable.append(table);
				}
			
		}catch(Exception e) {
			e.printStackTrace();
		}
		
		return intermediaryResponse;
	}
	
	public Collection<ParsedResponse> smartParseIntermediaryIntermediary(ServerResponse response, String table, StringBuilder outputTable) {
		
		 Vector<ParsedResponse> intermediaryResponse = new Vector<ParsedResponse>();
		
		try {
				
				
				org.htmlparser.Parser htmlParser = org.htmlparser.Parser.createParser(table, null);
				NodeList mainTableList = htmlParser.parse(null);
				NodeList tableList = mainTableList.extractAllNodesThatMatch(new TagNameFilter("table"), true);
				if (tableList.size() > 0){
					boolean addressSearch = false;
					TableTag mainTable = (TableTag)tableList.elementAt(0);
					TableRow[] rows = mainTable.getRows();
	
					for(TableRow row : rows ) {
						if(row.getColumnCount() > 1) {
								
							TableColumn[] cols = row.getColumns();
							NodeList aList = cols[1].getChildren().extractAllNodesThatMatch(new TagNameFilter("a"), true);
							if (aList.size() > 0){
								String rowHtml =  row.toHtml();
								String link = CreatePartialLink(TSConnectionURL.idGET);
								String query = ((LinkTag) aList.elementAt(0)).extractLink();
									
								if (query.indexOf("javascript:QueryAddress") != -1){
									String road = StringUtils.extractParameter(query, "(?is)javascript:QueryAddress\\('([^']*)");
									String pre = StringUtils.extractParameter(query, "(?is)javascript:QueryAddress\\('[^']*'\\s*,\\s*'([^']*)");
									String suff = StringUtils.extractParameter(query, "(?is)javascript:QueryAddress\\('[^']*'\\s*,\\s*'[^']*'\\s*,\\s*'([^']*)");
									String sDir = StringUtils.extractParameter(query, "(?is)javascript:QueryAddress\\('[^']*'\\s*,\\s*'[^']*'\\s*,\\s*'[^']*'\\s*,\\s*'([^']*)");
									String town = StringUtils.extractParameter(query, "(?is)javascript:QueryAddress\\('[^']*'\\s*,\\s*'[^']*'\\s*,\\s*'[^']*'\\s*,\\s*'[^']*'\\s*,\\s*'([^']*)");
									link += "RoadList.asp?Road="  + road + "&Pre=" + pre + "&Suff=" + suff
																	+ "&sDir=" + sDir + "&Town=" + town;
										
									rowHtml = rowHtml.replaceAll("<a[^>]*>[^<]*<", "<a href=\"" + link + "\">List Addresses<");
									rowHtml = rowHtml.replaceAll("(?is)</?font[^>]*>", "");
									rowHtml = rowHtml.replaceAll("(?is)(<td[^>]*>)([^<]*)(</td>)", "$1 <a href=\"" + link + "\">$2</a>$3");
									addressSearch = true;
								} else if (query.indexOf("javascript:QuerySub") != -1){
									String subCode = StringUtils.extractParameter(query, "(?is)javascript:QuerySub\\('([^']*)");
									String filing = StringUtils.extractParameter(query, "(?is)javascript:QuerySub\\('[^']*'\\s*,\\s*'([^']*)");
									String phase = StringUtils.extractParameter(query, "(?is)javascript:QuerySub\\('[^']*'\\s*,\\s*'[^']*'\\s*,\\s*'([^']*)");
									link += "LegalList.asp?SubCode="  + subCode + "&Filing=" + filing + "&Phase=" + phase;
										
									rowHtml = rowHtml.replaceAll("<a[^>]*>[^<]*<", "<a href=\"" + link + "\">List Lots/Blocks<");
								}
								ParsedResponse currentResponse = new ParsedResponse();
								currentResponse.setAttribute(ParsedResponse.SERVER_ROW_RESPONSE, rowHtml);
								currentResponse.setOnlyResponse(rowHtml);
								currentResponse.setPageLink(new LinkInPage(link, link, TSServer.REQUEST_GO_TO_LINK));
								
								String addressORSubdivision = cols[0].toPlainTextString().trim();
																	
								ResultMap resultMap = new ResultMap();
								resultMap.put("OtherInformationSet.SrcType", "AO");
								if (addressSearch){
									addressORSubdivision = addressORSubdivision.replaceAll("(?is)([^\\(]+).*", "$1").replaceAll("(?is)\\s+", " ");
									resultMap.put("PropertyIdentificationSet.StreetName", StringFormats.StreetName(addressORSubdivision.trim()));
								} else {
									resultMap.put("PropertyIdentificationSet.PropertyDescription", addressORSubdivision.trim());
								}
							
								resultMap.removeTempDef();
								Bridge bridge = new Bridge(currentResponse, resultMap, searchId);
								DocumentI document = (AssessorDocumentI) bridge.importData();
								currentResponse.setDocument(document);
									
								intermediaryResponse.add(currentResponse);
							}
						}
					}
					String header = "";
					if (addressSearch){
						header = "<tr><td align=\"center\"><u>Address</u></td><td></td></tr>";
					} else {
						header = "<tr><td align=\"center\"><u>Subdivision</u></td><td></td></tr>";
					}
					response.getParsedResponse().setHeader("&nbsp;&nbsp;&nbsp;" +  "<br><br>" 
															+ table.substring(table.indexOf("<table"), table.indexOf(">") + 1)
															+ header);
					response.getParsedResponse().setFooter("</table><br><br>");			
					
					outputTable.append(table);
				}
			
		}catch(Exception e) {
			e.printStackTrace();
		}
		
		return intermediaryResponse;
	}
	
	@Override
	protected Object parseAndFillResultMap(ServerResponse response,String detailsHtml, ResultMap map) {
		ro.cst.tsearch.servers.functions.COSummitAO.parseAndFillResultMap(detailsHtml, map, searchId);
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