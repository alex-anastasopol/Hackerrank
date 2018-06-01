package ro.cst.tsearch.servers.types;



import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Vector;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.htmlparser.filters.HasAttributeFilter;
import org.htmlparser.filters.TagNameFilter;
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
import ro.cst.tsearch.extractor.xml.ResultMap;
import ro.cst.tsearch.search.FunctionStatesIterator;
import ro.cst.tsearch.search.filter.FilterResponse;
import ro.cst.tsearch.search.filter.newfilters.address.AddressFilterFactory;
import ro.cst.tsearch.search.filter.newfilters.address.GenericAddressFilter;
import ro.cst.tsearch.search.filter.newfilters.name.NameFilterFactory;
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
import ro.cst.tsearch.utils.tags.WhateverTag;

import com.stewart.ats.base.document.DocumentI;
import com.stewart.ats.base.document.TaxDocumentI;


/**
* @author mihaib
**/

public class COPuebloTR extends TSServer {

	public static final long serialVersionUID = 10000000L;
	private boolean downloadingForSave; 
	private static final Pattern dummyPattern = Pattern.compile("&dummy=([0-9]+)&");
	
	private static final Pattern ADD_TREASURE_INFO_PAT = Pattern.compile("(?is)href=\\\"([^\\\"]+)\\\"[^>]*>Additional Treasurer Information");
	public static final Pattern SCHEDULE_PIN_PAT = Pattern.compile("(?is)<dt>Schedule:\\s*</dt>\\s*<dd>([^<]+)");
		
	public COPuebloTR(long searchId) {
		super(searchId);
	}

	public COPuebloTR(String rsRequestSolverName, String rsSitePath, String rsServerID, String rsPrmNameLink, long searchId, int mid) {
		super(rsRequestSolverName, rsSitePath, rsServerID, rsPrmNameLink, searchId, mid);
	}

	@Override
	protected void setModulesForAutoSearch(TSServerInfo serverInfo) {
		List<TSServerInfoModule> modules = new ArrayList<TSServerInfoModule>();
		TSServerInfoModule module = null;
		String pid = getSearch().getSa().getAtribute(SearchAttributes.LD_PARCELNO);
		String streetNO = getSearch().getSa().getAtribute(SearchAttributes.P_STREETNO);
		String streetName = getSearch().getSa().getAtribute(SearchAttributes.P_STREETNAME);
	
		GenericAddressFilter addressFilter 	= AddressFilterFactory.getGenericAddressHighPassFilter( searchId, 0.8d );
		FilterResponse defaultNameFilter = NameFilterFactory.getDefaultNameFilter(SearchAttributes.OWNER_OBJECT , searchId , module );

		if (hasPin()) {
			//Search by Parcel/Schedule Number
			module = new TSServerInfoModule(serverInfo.getModule(TSServerInfo.NAME_MODULE_IDX));
			module.clearSaKeys();
			module.setData(3, pid);
			modules.add(module);
		}
		
		if (hasStreet()) {
			//Search by Property Address
			module = new TSServerInfoModule(serverInfo.getModule(TSServerInfo.NAME_MODULE_IDX));
			module.clearSaKeys();
			module.forceValue(0, streetNO);
			module.forceValue(3, streetName);
			module.addFilter(addressFilter);
			module.addFilter(defaultNameFilter);
			modules.add(module);
		}
		
		if (hasOwner()) {
			//Search by Owner
			module = new TSServerInfoModule(serverInfo.getModule(TSServerInfo.NAME_MODULE_IDX));
			module.clearSaKeys();
			module.setSaObjKey(SearchAttributes.OWNER_OBJECT);
			module.addFilter(addressFilter);
			module.addFilter(defaultNameFilter);
			module.setIteratorType(4, FunctionStatesIterator.ITERATOR_TYPE_LF_NAME_FAKE);
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

		switch (viParseID) {		
		
			case ID_SEARCH_BY_NAME :
				
				if (rsResponse.indexOf("Property not found") != -1){
					Response.getParsedResponse().setError("<font color=\"red\">No results found</font>");
					return;
				}
				
				try {
					 
					StringBuilder outputTable = new StringBuilder();
					ParsedResponse parsedResponse = Response.getParsedResponse();
					 
					String table = "";
					org.htmlparser.Parser htmlParser = org.htmlparser.Parser.createParser(rsResponse, null);
					NodeList mainList = htmlParser.parse(null);
					NodeList tables = mainList.extractAllNodesThatMatch(new TagNameFilter("table"), true);
					for (int k = tables.size() - 1; k < tables.size(); k--){
						if (tables.elementAt(k).toHtml().contains("Owner Name")){
							table = tables.elementAt(k).toHtml();
							break;
						}
					}
							
					Collection<ParsedResponse> smartParsedResponses = smartParseIntermediary(Response, table, outputTable);
											
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
				
				String details = "";
				details = getDetails(rsResponse);				
				String pid = "";
				
				Matcher mat = SCHEDULE_PIN_PAT.matcher(details);
				if (mat.find()){
					pid = mat.group(1).trim();
				}

				if ((!downloadingForSave))
				{	
	                String qry_aux = Response.getRawQuerry();
					qry_aux = "dummy=" + pid + "&" + qry_aux;
					String originalLink = sAction + "&" + qry_aux;
					String sSave2TSDLink = getLinkPrefix(TSConnectionURL.idGET) + originalLink;
					
					HashMap<String, String> data = new HashMap<String, String>();
					data.put("type","CNTYTAX");
					
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
					smartParseDetails(Response, details);
	                msSaveToTSDFileName = pid + ".html";
	                Response.getParsedResponse().setFileName(getServerTypeDirectory() + msSaveToTSDFileName);
	                msSaveToTSDResponce = details + CreateFileAlreadyInTSD();                
				}
				
				break;	
			
			case ID_GET_LINK :
				if (sAction.indexOf("propertyinfo.p") != -1){
						ParseResponse(sAction, Response, ID_DETAILS);
				} 
				
				break;
			
			case ID_SAVE_TO_TSD :

				downloadingForSave = true;
				ParseResponse(sAction, Response, ID_DETAILS);
				downloadingForSave = false;
				break;
		}
	}
	
	protected String getDetails(String rsResponse){
		
		if (rsResponse.toLowerCase().indexOf("<html") == -1)
			return rsResponse;
		
		rsResponse = rsResponse.replaceAll("(?is)type\\s*=\\s*\\\"\\s*hidden\\s*\\\"", "type=\"submit\"");
		
		String contents = "";
		StringBuffer respBuff = new StringBuffer();
		
		try {
			org.htmlparser.Parser htmlParser = org.htmlparser.Parser.createParser(rsResponse, null);
			
			WhateverTag addTags = new WhateverTag();
			htmlParser.setNodeFactory(addTags.addNodeFactory());
			
			NodeList mainList = htmlParser.parse(null);
			
			NodeList dlList = mainList.extractAllNodesThatMatch(new TagNameFilter("dl"), true)
									.extractAllNodesThatMatch(new HasAttributeFilter("id", "PropertySearchDetails"), true);
			if (dlList != null && dlList.size() > 0){
				respBuff.append(dlList.elementAt(0).toHtml());
			}
			
			NodeList divList = mainList.extractAllNodesThatMatch(new TagNameFilter("div"), true)
									.extractAllNodesThatMatch(new HasAttributeFilter("class", "table-box"));
			for (int k = 0; k < divList.size(); k++){
				if (divList.elementAt(k).getChildren().toHtml().contains("propertySearchTable")){
					respBuff.append(divList.elementAt(k).toHtml());
					break;
				}
			}
			
			divList = mainList.extractAllNodesThatMatch(new TagNameFilter("div"), true)
							.extractAllNodesThatMatch(new HasAttributeFilter("id", "invList"));
			respBuff.append(divList.toHtml());
			
			NodeList tables = mainList.extractAllNodesThatMatch(new TagNameFilter("table"), true)
								.extractAllNodesThatMatch(new HasAttributeFilter("id", "dTable"));
			
			if (tables != null && tables.size() > 0){
				respBuff.append(tables.elementAt(0).toHtml());
			}
			
		} catch (Exception e) {
			e.printStackTrace();
		}
		
		contents = respBuff.toString();
		
		String link = "http://www.co.pueblo.co.us/cgi-bin/webatrbroker.wsc/";
		Matcher mat = ADD_TREASURE_INFO_PAT.matcher(rsResponse);
		if (mat.find()){
			link += mat.group(1);
			
			HTTPRequest reqP = new HTTPRequest(link);
		    reqP.setMethod(HTTPRequest.GET);
		    	
		    HTTPResponse resP = null;
	        HttpSite site = HttpManager.getSite(HashCountyToIndex.getDateSiteForMIServerID(getCommunityId(), miServerID).getName(), searchId);
			try
			{
				resP = site.process(reqP);
			} finally 
			{
				HttpManager.releaseSite(site);
			}	
			rsResponse = resP.getResponseAsString();
			
			try {
				org.htmlparser.Parser htmlParser = org.htmlparser.Parser.createParser(rsResponse, null);
				NodeList mainList = htmlParser.parse(null);
				
				NodeList tables = mainList.extractAllNodesThatMatch(new TagNameFilter("table"), true)
									.extractAllNodesThatMatch(new HasAttributeFilter("id", "dTable"));
				
				if (tables != null && tables.size() > 0){
					String taxhistory = tables.elementAt(0).toHtml();
					contents = contents.replaceFirst("(?is)(?:<p[^>]*>\\s*)?<a href=\\\"([^\\\"]+)\\\"[^>]*>Additional T[^<]*</a>\\s*(?:</p>)?", 
														taxhistory);
				}
				
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		
		//contents = contents.replaceAll("(?is)<div.*?</div>(\\s*</div>)?", "");
		contents = contents.replaceAll("(?is)</?a[^>]*>", "");
		contents = contents.replaceAll("(?is)<img[^>]*>", "");
		contents = contents.replaceAll("(?is)<form.*?</form>", "");
		contents = contents.replaceAll("(?is)<font[^>]*>\\s*Click on[^<]+</font>", "");
		
		return contents;
	}
	
	public Collection<ParsedResponse> smartParseIntermediary(ServerResponse response, String table, StringBuilder outputTable) {
		
		 Vector<ParsedResponse> intermediaryResponse = new Vector<ParsedResponse>();
		
		try {
			table = table.replaceAll("\\s*</?font[^>]*>\\s*", "").replaceAll("(?is)<tr[^>]*>\\s*<td[^>]*>\\s*<strong[^>]*>[^>]*></td>\\s*</tr>", "");
			org.htmlparser.Parser htmlParser = org.htmlparser.Parser.createParser(table, null);
			NodeList mainTableList = htmlParser.parse(null);
			TableTag mainTable = (TableTag)mainTableList.elementAt(0);
			
			TableRow[] rows = mainTable.getRows();
						
			for(int i = 1; i < rows.length; i++ ) {
				if(rows[i].getColumnCount() > 1) {
					ResultMap resultMap = new ResultMap();
					
					TableColumn[] cols = rows[i].getColumns();
					String parcelNo = cols[0].toHtml().replaceAll("(?is)</?td[^>]*>", "").replaceAll("(?is)</?a[^>]*>", "").trim();
					String ownerName = cols[1].getChildrenHTML().trim().replaceAll("(?is)&nbsp;", " ");
					String streetNo = cols[2].getChildrenHTML().trim().replaceAll("(?is)&nbsp;", " ");
					resultMap.put(PropertyIdentificationSetKey.STREET_NO.getKeyName(), streetNo);
					String streetName = cols[3].getChildrenHTML().trim().replaceAll("(?is)&nbsp;", " ");
					resultMap.put(PropertyIdentificationSetKey.STREET_NAME.getKeyName(), streetName);
					
					String legal = cols[7].getChildrenHTML().trim().replaceAll("(?is)&nbsp;", " ");
					
					String rowHtml =  rows[i].toHtml().replaceFirst("(?is)href=[\\\"'](propertyinfo[^\\\"']+)[^>]+>",
										" href=\"" + CreatePartialLink(TSConnectionURL.idGET) + "/cgi-bin/webatrbroker.wsc/$1\">" ).replaceAll("&amp;", "&");
					rowHtml = rowHtml.replaceAll("(?is)<a[^>]+>\\s*(View[^<]*)</a>", "$1");
					
					String link = rowHtml.replaceAll("(?is).*?href[^\\\"]+\\\"([^\\\"]+).*", "$1");
					
					ParsedResponse currentResponse = new ParsedResponse();
					currentResponse.setAttribute(ParsedResponse.SERVER_ROW_RESPONSE, rows[i].toHtml());
					currentResponse.setOnlyResponse(rowHtml);
					currentResponse.setPageLink(new LinkInPage(link, link, TSServer.REQUEST_SAVE_TO_TSD));
					
					resultMap.put(OtherInformationSetKey.SRC_TYPE.getKeyName(), "TR");
					resultMap.put(PropertyIdentificationSetKey.PARCEL_ID.getKeyName(), parcelNo);
					resultMap.put("tmpOwner", ownerName);
					resultMap.put(PropertyIdentificationSetKey.PROPERTY_DESCRIPTION.getKeyName(), legal);
										
					ro.cst.tsearch.servers.functions.COPuebloTR.partyNamesCOPuebloTR(resultMap, getSearch().getID());
					ro.cst.tsearch.servers.functions.COPuebloTR.parseLegalCOPuebloTR(resultMap, getSearch().getID());
					
					resultMap.removeTempDef();
					
					Bridge bridge = new Bridge(currentResponse, resultMap, searchId);
					
					DocumentI document = (TaxDocumentI)bridge.importData();				
					currentResponse.setDocument(document);
					
					intermediaryResponse.add(currentResponse);
				}
			}
			
		
		String header1 = "<TABLE border=\"0\" width=\"95%\" align=\"center\" cellpadding=\"4\" cellspacing=\"1\">" + rows[0].toHtml();
	
		header1 = header1.replaceAll("(?is)</?a[^>]*>","");
		response.getParsedResponse().setHeader(header1);	
		response.getParsedResponse().setFooter("</table>");

		
		outputTable.append(table);
			
		}catch(Exception e) {
			e.printStackTrace();
		}
		
		return intermediaryResponse;
	}
		
	@Override
	protected Object parseAndFillResultMap(ServerResponse response, String detailsHtml, ResultMap map) {
			ro.cst.tsearch.servers.functions.COPuebloTR.parseAndFillResultMap(detailsHtml, map, searchId, miServerID);
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