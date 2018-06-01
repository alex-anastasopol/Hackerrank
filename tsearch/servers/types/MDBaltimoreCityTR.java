package ro.cst.tsearch.servers.types;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Vector;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang.StringUtils;
import org.htmlparser.filters.HasAttributeFilter;
import org.htmlparser.filters.TagNameFilter;
import org.htmlparser.tags.FormTag;
import org.htmlparser.tags.LinkTag;
import org.htmlparser.tags.ScriptTag;
import org.htmlparser.tags.Span;
import org.htmlparser.tags.TableColumn;
import org.htmlparser.tags.TableRow;
import org.htmlparser.tags.TableTag;
import org.htmlparser.util.NodeList;

import ro.cst.tsearch.bean.SearchAttributes;
import ro.cst.tsearch.connection.http2.HttpManager;
import ro.cst.tsearch.connection.http2.HttpSite;
import ro.cst.tsearch.exceptions.ServerResponseException;
import ro.cst.tsearch.extractor.xml.Bridge;
import ro.cst.tsearch.extractor.xml.ResultMap;
import ro.cst.tsearch.parser.HtmlParser3;
import ro.cst.tsearch.search.FunctionStatesIterator;
import ro.cst.tsearch.search.filter.FilterResponse;
import ro.cst.tsearch.search.filter.newfilters.address.AddressFilterFactory;
import ro.cst.tsearch.search.filter.newfilters.name.NameFilterFactory;
import ro.cst.tsearch.search.iterator.ModuleStatesIteratorFactory;
import ro.cst.tsearch.search.module.ConfigurableNameIterator;
import ro.cst.tsearch.servers.TSConnectionURL;
import ro.cst.tsearch.servers.info.TSServerInfo;
import ro.cst.tsearch.servers.info.TSServerInfoModule;
import ro.cst.tsearch.servers.response.LinkInPage;
import ro.cst.tsearch.servers.response.ParsedResponse;
import ro.cst.tsearch.servers.response.ServerResponse;
import ro.cst.tsearch.servers.response.OtherInformationSet.OtherInformationSetKey;
import ro.cst.tsearch.servers.response.PropertyIdentificationSet.PropertyIdentificationSetKey;

import com.stewart.ats.base.document.DocumentI;
import com.stewart.ats.base.document.TaxDocumentI;

public class MDBaltimoreCityTR extends TSServer {


	private static final long serialVersionUID = 4940198725194257741L;

	public MDBaltimoreCityTR(long searchId) {
		super(searchId);
	}
	
	public MDBaltimoreCityTR(String rsRequestSolverName, String rsSitePath, String rsServerID, String rsPrmNameLink, long searchId, int miServerID) {
		super(rsRequestSolverName, rsSitePath, rsServerID, rsPrmNameLink, searchId, miServerID);
	}

	
	private static int seq = 0;

	protected synchronized static int getSeq() {
		return seq++;
	}
	
	
	public String getHiddenParam(NodeList nodeList)  {
		String hiddenVal = "";
		String hiddenKey = "";
		
		if (nodeList!= null && nodeList.size() > 2) {
			ScriptTag s = (ScriptTag) nodeList.elementAt(2);			
			
			if (s != null) {
				Pattern p = Pattern.compile("(?is)[^?]+\\?_TSM_HiddenField_=([^&]+)[^_]+_TSM_CombinedScripts_=([^\"]+)");
				Matcher m = p.matcher(s.getAttribute("src"));
				
				if (m.find()) {
					hiddenKey = m.group(1);
					if ("ctl00_ctl00_ScriptManager1_HiddenField".equals(hiddenKey)) {
						hiddenVal = m.group(2);
						try {
							hiddenVal = URLDecoder.decode(hiddenVal, "UTF-8");
						} catch (UnsupportedEncodingException e) {							
							e.printStackTrace();
							logger.trace(e);
						}
					}
				}
			}
		}
		
		return hiddenVal;
	}

	@Override
	public Collection<ParsedResponse> smartParseIntermediary(ServerResponse response, String table, StringBuilder outputTable) {
		Vector<ParsedResponse> intermediaryResponse = new Vector<ParsedResponse>();
		
		try {						
			
			Map<String, String> params = null;			 			
			HttpSite site = HttpManager.getSite(getCurrentServerName(), searchId);
			if (site != null) {
				try {
					params = HttpSite.fillConnectionParams(table,
							((ro.cst.tsearch.connection.http2.MDBaltimoreCityTR) site)
									.getTargetArgumentParameters(), "aspnetForm");
				} finally {
					// always release the HttpSite
					HttpManager.releaseSite(site);
				}
			}

			HtmlParser3 htmlParser = new HtmlParser3(table);
			NodeList nodeList = htmlParser.getNodeList();
			
			FormTag form = (FormTag) nodeList.extractAllNodesThatMatch(new TagNameFilter("form"), true)
					.extractAllNodesThatMatch(new HasAttributeFilter("name", "aspnetForm")).elementAt(0);
			String action = form.getFormLocation();	
			
			NodeList scripts = nodeList.extractAllNodesThatMatch(new TagNameFilter("form"), true)
					.extractAllNodesThatMatch(new HasAttributeFilter("name", "aspnetForm"))
					.extractAllNodesThatMatch(new TagNameFilter("script"), true);
			String hiddenVal = getHiddenParam(scripts);
			
			nodeList = htmlParser.getNodeList()
					.extractAllNodesThatMatch(new TagNameFilter("table"), true)
					.extractAllNodesThatMatch(new HasAttributeFilter("id", "ctl00_ctl00_rootMasterContent_LocalContentPlaceHolder_DataGrid1"), true);
						
			if(nodeList.size() == 0) {
				return intermediaryResponse;
			}
			
			TableTag tableTag = (TableTag) nodeList.elementAt(0);
			tableTag.removeAttribute("style");
			tableTag.setAttribute("border", "1");
			
			TableRow[] rows  = tableTag.getRows();
			
			for (int i = 1; i < rows.length; i++) {
				TableRow row = rows[i];
				if(row.getColumnCount() == 6) {
					ParsedResponse currentResponse = new ParsedResponse();					
					int seq = getSeq();
					HashMap<String,String> paramsOfReq = htmlParser.getListOfPostParams("aspnetForm");
					
					row.removeChild(6);	 // remove last column with not useful links						
					
					LinkTag linkTag = ((LinkTag)row.getColumns()[4].getChildren().extractAllNodesThatMatch(new TagNameFilter("a")).elementAt(0));
					String tmpLnk = linkTag.extractLink().trim().replaceAll("(?is)[^']+'([^']+)[^)]+\\)", "$1");					
					
					if (paramsOfReq != null) {
						paramsOfReq.remove("ctl00$ctl00$imgBtnGoogleSearch");
						paramsOfReq.remove("ctl00$ctl00$rootMasterContent$LocalContentPlaceHolder$lbQuery");
						if (StringUtils.isEmpty(paramsOfReq.get("ctl00_ctl00_ScriptManager1_HiddenField"))) {
							paramsOfReq.put("ctl00_ctl00_ScriptManager1_HiddenField", hiddenVal);
						}
						params.putAll(paramsOfReq);
						params.remove("__EVENTTARGET");						
					}
					
					mSearch.setAdditionalInfo(getCurrentServerName() + ":params:" + seq, params);
					
					String link = CreatePartialLink(TSConnectionURL.idPOST) + "/realproperty/" + action   
							+ (action.contains("?") ? "&" : "?") + "__EVENTTARGET=" + tmpLnk + "&seq=" + seq;
					
					linkTag.setLink(link);
					row.getColumns()[4].getChild(1).getChildren().remove(1);
					
					currentResponse.setAttribute(ParsedResponse.SERVER_ROW_RESPONSE, row.toHtml());
					currentResponse.setOnlyResponse(row.toHtml());
					currentResponse.setPageLink(new LinkInPage(link,link,TSServer.REQUEST_SAVE_TO_TSD));
					
					ResultMap map = ro.cst.tsearch.servers.functions.MDBaltimoreCityTR.parseIntermediaryRow(row, searchId); 
					Bridge bridge = new Bridge(currentResponse, map, searchId);
					
					DocumentI document = (TaxDocumentI)bridge.importData();				
					currentResponse.setDocument(document);
					
					intermediaryResponse.add(currentResponse);					
				}
			}
		
			response.getParsedResponse().setHeader("<table border=\"1\" cellspacing=\"0\" cellpadding=\"5\">\n" + 
					"<tr> <th>Block</th> <th>Lot</th> <th>Property Address</th> <th>Owner</th> <th>Select</th> </tr>");			
			response.getParsedResponse().setFooter("</table>");
			
			outputTable.append(table);
						
		} catch (Throwable t){
			logger.error("Error while parsing intermediary data", t);
		}
		
		return intermediaryResponse;
	}
	
	
	
	@Override
	protected void ParseResponse(String sAction, ServerResponse Response, int viParseID) throws ServerResponseException {
		String rsResponse = Response.getResult();
		ParsedResponse parsedResponse = Response.getParsedResponse();
		
		// 	no result returned - error message on official site
		if (rsResponse.contains("An unexpected error has occurred:")) {
			Response.getParsedResponse().setError("Official site not functional");
    		return;
    	} else if (rsResponse.indexOf("No records found matching your criteria") > -1) {
			Response.getParsedResponse().setError("No results found for your query! Please change your search criteria and try again.");
			return;
		} else if (rsResponse.indexOf("If Block & lot used, no other fields are allowed") > -1) {
			Response.getParsedResponse().setError("No other search criteria should be added when searching by Lot and Block! Please change your search criteria and try again.");
			return;
		} else if (rsResponse.indexOf("The Parcel ID you entered is not valid") > -1) {
			Response.getParsedResponse().setError("The Parcel ID you entered is not valid!");
			return;
		} else if (rsResponse.indexOf("You must enter some criteria") > -1) {
			Response.getParsedResponse().setError("You should set at least one search criteria when searching!");
			return;
		}
		
		switch (viParseID) {			
			case ID_SEARCH_BY_SUBDIVISION_PLAT:
			case ID_SEARCH_BY_PROP_NO:
				StringBuilder outputTable = new StringBuilder();
				Collection<ParsedResponse> smartParsedResponses = smartParseIntermediary(Response, rsResponse, outputTable);
				
				if(smartParsedResponses.size() > 0) {
					parsedResponse.setResultRows(new Vector<ParsedResponse>(smartParsedResponses));
					parsedResponse.setOnlyResponse(outputTable.toString());
					parsedResponse.setAttribute(ParsedResponse.SERVER_ROW_RESPONSE, outputTable.toString());
	            }			
				break;
				
			case ID_DETAILS:
			case ID_SAVE_TO_TSD:						
				StringBuilder accountId = new StringBuilder();
				String details = getDetails(rsResponse, accountId);
				String filename = accountId + ".html";
				
				if (viParseID != ID_SAVE_TO_TSD) {
					String originalLink = sAction.replace("?", "&") + "&" + Response.getRawQuerry();
					String sSave2TSDLink = getLinkPrefix(TSConnectionURL.idGET) + originalLink;
	
					HashMap<String, String> data = new HashMap<String, String>();
					data.put("type","CNTYTAX");
					String year = ro.cst.tsearch.servers.functions.MDBaltimoreCityTR.getTaxYear(rsResponse); 
					if (StringUtils.isNotEmpty(year)) {
						data.put("year", year);
					}
					if (isInstrumentSaved(accountId.toString(),null,data)){
						details += CreateFileAlreadyInTSD();
					
					} else {
						mSearch.addInMemoryDoc(sSave2TSDLink, details);					
						details = addSaveToTsdButton(details, sSave2TSDLink, ID_DETAILS);
					}
	
					Response.getParsedResponse().setPageLink(new LinkInPage(sSave2TSDLink,originalLink,TSServer.REQUEST_SAVE_TO_TSD));
					Response.getParsedResponse().setResponse(details);
					
				} else {
					smartParseDetails(Response,details);
					
					msSaveToTSDFileName = filename;
					Response.getParsedResponse().setFileName(getServerTypeDirectory() + msSaveToTSDFileName);	
					Response.getParsedResponse().setResponse(details);
					
					msSaveToTSDResponce = details + CreateFileAlreadyInTSD();					
				}
				break;
				
			case ID_GET_LINK :
				ParseResponse(sAction, Response, ID_DETAILS);
				break;
				
			default:
				break;
		}
	}	
	
	
	protected String createAPNvalue (TableTag table) {
		String apn = "";
		
		if (table.getRowCount() > 3) {
			int dim = table.getRows()[2].getColumns().length;
			
			if (dim ==2) {							
				TableColumn col = table.getRows()[2].getColumns()[1];
				NodeList detailsList = col.getChildren();
				
				Span span = (Span) detailsList.extractAllNodesThatMatch
						(new HasAttributeFilter("id","ctl00_ctl00_rootMasterContent_LocalContentPlaceHolder_ward")).elementAt(0);	
				if (span != null) {
					String ward = span.getChildrenHTML().trim();
					apn += ward + "-";
				}
				
				span  = (Span) detailsList.extractAllNodesThatMatch
						(new HasAttributeFilter("id","ctl00_ctl00_rootMasterContent_LocalContentPlaceHolder_sect")).elementAt(0);
				if (span != null) {
					String section = span.getChildrenHTML().trim();
					apn += section + "-";
				}
				
				span = (Span) detailsList.extractAllNodesThatMatch
						(new HasAttributeFilter("id","ctl00_ctl00_rootMasterContent_LocalContentPlaceHolder_block")).elementAt(0);
				if (span != null) {
					String block = span.getChildrenHTML().trim();
					apn += block + "-";
				}
				
				span = (Span) detailsList.extractAllNodesThatMatch
						(new HasAttributeFilter("id","ctl00_ctl00_rootMasterContent_LocalContentPlaceHolder_lot")).elementAt(0);
				if (span != null) {
					String lot = span.getChildrenHTML().trim();
					apn += lot;
				}
			}
		}
		
		return apn;
	}
	
	
	protected String[] getPageDetailsPerYear(String rsResponse, int year) {
		String[] parseRespAndNextResp = {"",""};
		StringBuilder parsedResponse = new StringBuilder();
		String rspOfNextReq = "";
		String hiddenVal = "";
		String currentTaxYear = "";
		
		HtmlParser3 htmlParser = new HtmlParser3(rsResponse); // we know for sure that htmlParser contains "<html"
				
		
		NodeList tmpList = htmlParser.getNodeList().extractAllNodesThatMatch(new TagNameFilter("form"), true)
				.extractAllNodesThatMatch(new HasAttributeFilter("name", "aspnetForm"))
				.extractAllNodesThatMatch(new TagNameFilter("script"), true);
		
		if (tmpList != null) {
			hiddenVal = getHiddenParam(tmpList);
		}
		
		NodeList nodeList = htmlParser.getNodeList()
				.extractAllNodesThatMatch(new TagNameFilter("div"), true)
				.extractAllNodesThatMatch(new HasAttributeFilter("id", "ctl00_ctl00_rootMasterContent_LocalContentPlaceHolder_pnlDetail"), true);	
		
		if (nodeList.size() == 0) {
			return null;
		}
		
		tmpList = nodeList.extractAllNodesThatMatch(new TagNameFilter("table"), true)
				.extractAllNodesThatMatch(new HasAttributeFilter("id", "ctl00_ctl00_rootMasterContent_LocalContentPlaceHolder_tblDetail"), true);
		
		if (tmpList!= null) {
			TableTag tb = (TableTag) tmpList.elementAt(0);
			if (tb.getRowCount() > 1 && tb.getRows()[1].getColumnCount() > 0) {
				TableColumn col = tb.getRows()[1].getColumns()[0];
				Span span = (Span) col.getChildren().extractAllNodesThatMatch(new HasAttributeFilter
						("id", "ctl00_ctl00_rootMasterContent_LocalContentPlaceHolder_LabelStartFY"), true)
						.elementAt(0);   
				if (span != null) {
					currentTaxYear = span.getChildrenHTML().trim();
				}
			}	
						
			// adding heading row
			if (year == 3) { //details page of taxes from current year									
				parsedResponse.append("<tr align=\"center\"><td colspan=\"5\" align=\"center\">" +
						"</br><b> Real Property Tax Information </b></br> ~ Tax Year: " + currentTaxYear + " ~ </td></tr>");	
			
			} else if (year == 2) {
					parsedResponse.append("<tr align=\"center\"><td colspan=\"5\"></br></br> <b> TAX HISTORY: </b></td></tr>");					
			}
			if (year != 3) {
				parsedResponse.append("<tr border = \"0\"><td align=\"center\"></br> ~ Tax Year: " + currentTaxYear + " ~ </td></tr>");
			}

				parsedResponse.append("<tr border = \"0\"><td>");
				TableTag personalInfo = (TableTag) tmpList.elementAt(0);
				if (personalInfo != null) {
					personalInfo.removeAttribute("id");
					personalInfo.setAttribute("id","\"tblDetail" + year + "\"");					
					parsedResponse.append(personalInfo.toHtml());
					
					parsedResponse.append("<tr border = \"0\" id=\"amountDue" + year + "\"><td border = \"0\"> Amount Due: &nbsp; &nbsp; ");
					String amountDue = nodeList.extractAllNodesThatMatch(new HasAttributeFilter
							("id","ctl00_ctl00_rootMasterContent_LocalContentPlaceHolder_AmountDue"), true)
							.elementAt(0).getFirstChild().toHtml().trim().replaceAll("(?is)[\\$,]+", "");
					parsedResponse.append(amountDue + "</td></tr>");
					
					// requests to grab Tax History
					HttpSite site = HttpManager.getSite(getCurrentServerName(), searchId);
					String serverHomeLink = site.getSiteLink();
					HashMap<String,String> paramsOfReq = htmlParser.getListOfPostParams("aspnetForm"); 					
					try {
						if (StringUtils.isNotEmpty(hiddenVal)) {								
							paramsOfReq.put("ctl00_ctl00_ScriptManager1_HiddenField", hiddenVal);
						}
						paramsOfReq.remove("ctl00$ctl00$rootMasterContent$LocalContentPlaceHolder$lbData");
						paramsOfReq.remove("ctl00$ctl00$rootMasterContent$LocalContentPlaceHolder$btnPayWithAccount");
						paramsOfReq.remove("ctl00$ctl00$rootMasterContent$LocalContentPlaceHolder$lbQuery2");
						paramsOfReq.remove("ctl00$ctl00$imgBtnGoogleSearch");								
						paramsOfReq.remove("ctl00$ctl00$rootMasterContent$LocalContentPlaceHolder$btnPrintBill2");
						paramsOfReq.remove("ctl00$ctl00$rootMasterContent$LocalContentPlaceHolder$btnNextYear");
						
						String lnk = serverHomeLink + "?";
						rspOfNextReq = ((ro.cst.tsearch.connection.http2.MDBaltimoreCityTR)site).getPage(lnk, paramsOfReq);
						
					} finally {
						// always release the HttpSite
						HttpManager.releaseSite(site);
					}					
				}
		}
		parseRespAndNextResp[0] = parsedResponse.toString();
		parseRespAndNextResp[1] = rspOfNextReq;
		return parseRespAndNextResp;
	}
	
	protected String getDetails(String rsResponse, StringBuilder accountId) {
		try {
			/* If from memory - use it as is */
			if(!rsResponse.contains("<html")){
				return rsResponse;
			
			} else {
				try {
					StringBuilder details = new StringBuilder();
					int year = 3;
					HtmlParser3 htmlParser = new HtmlParser3(rsResponse);
					details.append("<html>");
					
					NodeList nodeList = htmlParser.getNodeList()
							.extractAllNodesThatMatch(new TagNameFilter("div"), true)
							.extractAllNodesThatMatch(new HasAttributeFilter("id", "ctl00_ctl00_rootMasterContent_LocalContentPlaceHolder_pnlDetail"), true);				
					
					if(nodeList.size() == 0) {
						return rsResponse;
					
					} else {
						NodeList detailsList = nodeList.elementAt(0).getChildren()
								.extractAllNodesThatMatch(new TagNameFilter("table"), true)
								.extractAllNodesThatMatch(new HasAttributeFilter("id", "ctl00_ctl00_rootMasterContent_LocalContentPlaceHolder_tblDetail"), true);  // get table with details info info

						TableTag personalInfo = (TableTag) detailsList.elementAt(0);
						
						String apn = createAPNvalue (personalInfo);							
						if (StringUtils.isNotEmpty(apn)) {
							accountId.append(apn);		
						}
						String[] tmpRsp = {"", ""};
						while (year > 0) { 							
							if (year == 3) {
								tmpRsp[1] = rsResponse;
								details.append("<table border = \"1\" id=\"mainTable\">");
							}
							tmpRsp = getPageDetailsPerYear(tmpRsp[1], year);
							if (tmpRsp != null) {
								details.append(tmpRsp[0]);
								year --;
							
							} else {
								break;
							}
						}							
						
						details.append("</table> </br> </html>");
					
						return details.toString();
					}
					
				} catch (Throwable t){
					logger.error("Error while parsing details page data", t);
				}
			}
			
		} catch (Exception e) {
			logger.error("Error while getting details", e);
		}
		return rsResponse;
	}

	
	@Override
	protected Object parseAndFillResultMap(ServerResponse rsResponse, String detailsHtml, ResultMap map) {
		try {	
			String accountID = "";			
			map.put(OtherInformationSetKey.SRC_TYPE.getKeyName(),"TR");
			
			detailsHtml = detailsHtml.replaceAll("(?is)<th([^>]*>)", "<td$1").replaceAll("(?is)</th>", "</td>");
			
			HtmlParser3 htmlParser = new HtmlParser3(detailsHtml);
			NodeList list = htmlParser.getNodeList();		
			TableTag table = (TableTag) list.extractAllNodesThatMatch
					(new HasAttributeFilter("id","tblDetail3"), true).elementAt(0);
			
			accountID = createAPNvalue(table) + "@";
			
			if (StringUtils.isNotEmpty(accountID)) {
				Pattern p = Pattern.compile("(?is)(\\d{2}-(\\d{3})-([^-]+)-([^@]+))@");
				Matcher m = p.matcher(accountID);
				if (m.matches()) {
					map.put(PropertyIdentificationSetKey.PARCEL_ID.getKeyName(), m.group(1));
					map.put(PropertyIdentificationSetKey.SUBDIVISION_SECTION.getKeyName(),
							m.group(2).trim().replaceAll("(?is)\\b0*(\\w+)\\b", "$1"));
					map.put(PropertyIdentificationSetKey.SUBDIVISION_BLOCK.getKeyName(),
							m.group(3).trim().replaceAll("(?is)\\b0*(\\w+)\\b", "$1"));
					map.put(PropertyIdentificationSetKey.SUBDIVISION_LOT_NUMBER.getKeyName(),
							m.group(4).trim().replaceAll("(?is)\\b0*(\\w+)\\b", "$1"));
				}				
			}
				
			ro.cst.tsearch.servers.functions.MDBaltimoreCityTR.parseDetails(list, map, searchId);
			
		} catch (Exception e) {
			logger.error("Error while getting details", e);
		}
		
		return null;	
	}
	
	
	@Override
	protected void setModulesForAutoSearch(TSServerInfo serverInfo) {
		List<TSServerInfoModule> moduleList = new ArrayList<TSServerInfoModule>();
		TSServerInfoModule module = null;
			
		FilterResponse addressFilter = AddressFilterFactory.getAddressHybridFilter (searchId , 0.8d , true);		
		FilterResponse nameFilterHybrid = NameFilterFactory.getHybridNameFilter (SearchAttributes.OWNER_OBJECT , searchId , module );
		
		if(hasPin()) {
			module = new TSServerInfoModule(serverInfo.getModule(TSServerInfo.SUBDIVISION_MODULE_IDX)); 
			module.clearSaKeys();
			Pattern p = Pattern.compile("(?is)\\b(\\d{4}[A-Z]*)-*(\\d{3}[A-Z]*)\\b");
			Matcher m = p.matcher(getSearchAttribute(SearchAttributes.LD_PARCELNONDB));
			if (m.find()) {
				String lot = m.group(2); 
				String block = m.group(1);
				module.forceValue(1,block);
				module.forceValue(2,lot);
				moduleList.add(module);
			}			
		}
		
		if(hasPin()) {
			module = new TSServerInfoModule(serverInfo.getModule(TSServerInfo.SUBDIVISION_MODULE_IDX)); 
			module.clearSaKeys();
			Pattern p = Pattern.compile("(?is)\\b(\\d{4}[A-Z]*)-*(\\d{3}[A-Z]*)\\b");
			Matcher m = p.matcher(getSearchAttribute(SearchAttributes.LD_PARCELNO));
			if (m.find()) {
				String lot = m.group(2); 
				String block = m.group(1);
				module.forceValue(1,block);
				module.forceValue(2,lot);
			}			
			moduleList.add(module);
		}
		
		if (!StringUtils.isEmpty(getSearchAttribute(SearchAttributes.LD_LOTNO)) &&
				!StringUtils.isEmpty(getSearchAttribute(SearchAttributes.LD_SUBDIV_BLOCK))) {
			module = new TSServerInfoModule(serverInfo.getModule(TSServerInfo.SUBDIVISION_MODULE_IDX)); 		
			module.clearSaKeys();
			module.setSaKey(1,SearchAttributes.LD_SUBDIV_BLOCK);
			module.setSaKey(2,SearchAttributes.LD_LOTNO);
			module.addFilter(addressFilter);
			module.addFilter(nameFilterHybrid);
			moduleList.add(module);	
		}
		
		if(hasStreet() && hasStreetNo()) {							
			module = new TSServerInfoModule(serverInfo.getModule(TSServerInfo.GENERIC_MODULE_IDX)); 
			module.clearSaKeys();
			module.setSaKey(1, SearchAttributes.P_STREET_NO_DIR_NAME_POSTDIR);
//			module.setSaKey(1, SearchAttributes.P_STREET_NO_NAME);			
			module.addFilter(addressFilter);
			module.addFilter(nameFilterHybrid);
			moduleList.add(module);		
		}
		
		if (hasStreet()) {
			module = new TSServerInfoModule(serverInfo.getModule(TSServerInfo.GENERIC_MODULE_IDX)); 
			module.clearSaKeys();
			module.setSaKey(1, SearchAttributes.P_STREET_FULL_NAME);
			module.addFilter(addressFilter);
			module.addFilter(nameFilterHybrid);
			moduleList.add(module);		
		}
		
		if (hasOwner()) {
			module = new TSServerInfoModule(serverInfo.getModule(TSServerInfo.GENERIC_MODULE_IDX)); 
			module.clearSaKeys();
			module.setSaObjKey(SearchAttributes.OWNER_OBJECT);
			module.setIteratorType(2, FunctionStatesIterator.ITERATOR_TYPE_LCF_NAME_FAKE);
			module.addFilter(nameFilterHybrid);		
			module.addFilter(addressFilter);
			module.addIterator((ConfigurableNameIterator) ModuleStatesIteratorFactory
					.getConfigurableNameIterator(module, searchId, new String[] {"L;F;"}));
			moduleList.add(module);		
		}
		
		serverInfo.setModulesForAutoSearch(moduleList);
	}
	
}
