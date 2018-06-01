package ro.cst.tsearch.servers.types;


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
import ro.cst.tsearch.extractor.xml.ResultMap;
import ro.cst.tsearch.parser.HtmlParser3;
import ro.cst.tsearch.search.FunctionStatesIterator;
import ro.cst.tsearch.search.filter.FilterResponse;
import ro.cst.tsearch.search.filter.newfilters.address.AddressFilterFactory;
import ro.cst.tsearch.search.filter.newfilters.name.GenericNameFilter;
import ro.cst.tsearch.search.filter.newfilters.name.NameFilterFactory;
import ro.cst.tsearch.search.iterator.ModuleStatesIteratorFactory;
import ro.cst.tsearch.search.module.ConfigurableNameIterator;
import ro.cst.tsearch.servers.TSConnectionURL;
import ro.cst.tsearch.servers.info.TSServerInfo;
import ro.cst.tsearch.servers.info.TSServerInfoModule;
import ro.cst.tsearch.servers.response.LinkInPage;
import ro.cst.tsearch.servers.response.OtherInformationSet.OtherInformationSetKey;
import ro.cst.tsearch.servers.response.ParsedResponse;
import ro.cst.tsearch.servers.response.Parser;
import ro.cst.tsearch.servers.response.ServerResponse;
import ro.cst.tsearch.utils.HttpUtils;

import com.stewart.ats.base.document.AssessorDocumentI;
import com.stewart.ats.base.document.DocumentI;

/**
 * @author mihaib
*/

public class CODenverAO extends TSServer{

	static final long serialVersionUID = 10000000;
	private static final Pattern CHAIN_OF_TITLE_LINK = Pattern.compile("(?is)<a\\s+href\\s*=\\s*\\\"([^\\\"]+)\\\">\\s*Link\\s*to\\s*chain\\s*of\\s*title");

	private boolean downloadingForSave; 

	/**
	 * 
	 * @param rsRequestSolverName
	 * @param rsSitePath
	 * @param rsServerID
	 * @param rsPrmNameLink
	 * @param searchId
	 * @param mid
	 */
	public CODenverAO(String rsRequestSolverName, String rsSitePath, String rsServerID, String rsPrmNameLink, long searchId, int mid) {
		super(rsRequestSolverName, rsSitePath, rsServerID, rsPrmNameLink, searchId, mid);
	}

	public CODenverAO(long searchId){
		super(searchId);
	}
	
	
	public ServerResponse SearchBy(TSServerInfoModule module, Object sd) throws ServerResponseException
    {
		String pin;
		int ServerInfoModuleID = module.getModuleIdx();

		if (ServerInfoModuleID == 2) //search by pin 
		{
			pin = module.getFunction(0).getParamValue();
			if (pin != null) {
				pin = pin.replaceAll("\\p{Punct}", "");
				module.getFunction(0).setParamValue(pin);
			}
		}
		return super.SearchBy(module, sd);
    }
	
	@Override
	protected void setModulesForAutoSearch(TSServerInfo serverInfo) {
		List<TSServerInfoModule> modules = new ArrayList<TSServerInfoModule>();
		TSServerInfoModule module = null;
		modules.add(new TSServerInfoModule(serverInfo.getModule(TSServerInfo.PARCEL_ID_MODULE_IDX)));
		
		String pin = getSearchAttribute(SearchAttributes.LD_PARCELNO_GENERIC_AO);
		if(pin.length() == 13 && pin.endsWith("000")) {
			pin = pin.replaceAll("(\\d{7})(\\d{3})0{3}", "$1$2$2");
			module = new TSServerInfoModule(serverInfo.getModule(TSServerInfo.PARCEL_ID_MODULE_IDX));
			module.clearSaKeys();
			module.forceValue(0, pin);
			modules.add(module);
		}
		
		FilterResponse addressFilter = AddressFilterFactory.getAddressHybridFilter( searchId , 0.8d );
		GenericNameFilter defaultNameFilter = (GenericNameFilter)NameFilterFactory.getDefaultNameFilter( 
					SearchAttributes.OWNER_OBJECT , searchId , module );
		defaultNameFilter.setUseSynonymsForCandidates(false);
		defaultNameFilter.setSkipUnique(false);
		if (hasStreet()) {
			//Search by Property Address
			module = new TSServerInfoModule(serverInfo.getModule(TSServerInfo.ADDRESS_MODULE_IDX));
			module.clearSaKey(2);
			module.clearSaKey(5);
			module.addFilter(addressFilter);
			module.addFilter(defaultNameFilter);
			modules.add(module);
		}
		
		if(hasOwner()) {
			//Search by Name
			module = new TSServerInfoModule(serverInfo.getModule(TSServerInfo.NAME_MODULE_IDX));
			module.clearSaKeys();
			module.setSaObjKey(SearchAttributes.OWNER_OBJECT);
			module.setIteratorType(0, FunctionStatesIterator.ITERATOR_TYPE_LAST_NAME_FAKE);
			module.addFilter(addressFilter);
			module.addFilter(defaultNameFilter);
			module.addIterator((ConfigurableNameIterator) ModuleStatesIteratorFactory
					.getConfigurableNameIterator(module, searchId, new String[] {"L,F;;","L,M;;"})
					);
			modules.add(module);
		}
		
	    serverInfo.setModulesForAutoSearch(modules);
	}
	
	private static int seq = 0;	
	protected synchronized static int getSeq(){
		return seq++;
	}
	
	@Override
	protected void ParseResponse(String sAction, ServerResponse Response, int viParseID) throws ServerResponseException {
		
		String initialResponse = Response.getResult();
		String rsResponse = initialResponse;
		String contents = "";
		ParsedResponse parsedResponse = Response.getParsedResponse();	
		String linkStart = CreatePartialLink(TSConnectionURL.idGET);
		switch (viParseID) {
		
			case ID_SEARCH_BY_ADDRESS :
			case ID_SEARCH_BY_NAME :
				
				int tmp = getSeq();
				StringBuilder outputTable = new StringBuilder();
				
				if (rsResponse.indexOf("We're sorry, but the system is unable to locate this property information") != -1){
					Response.getParsedResponse().setError("<font color=\"red\">No results found</font>");
					return;
				}
				
				if (rsResponse.indexOf("Select a name from the list") == -1){
					ParseResponse(sAction, Response, ID_DETAILS);
					return;
				}
				
				contents = rsResponse.replaceAll("(?is).*?(Select a name from the list below.*)", "$1");
				contents = contents.replaceAll("(?is)</table[^>]*>\\s*</td\\s*>\\s*</tr\\s*>.*?</table\\s*>.*", "</table>");
				contents = contents.replaceAll("(?is)(href\\s*=\\s*\\\")javascript[^']+'([^']+)'\\s*,\\s*'\\s*([^']+)\\s*'\\s*\\)\\s*;", 
						"$1" + linkStart + "/apps/realpropertyapplication/realproperty.asp?tmp=" + tmp + "&parcelid=$3&searchname=$2");
				
				Map<String,String> params = HttpUtils.getFormParams( rsResponse , true);
				params.remove("searchname");
				params.remove("parcelid");
				
				mSearch.setAdditionalInfo(getCurrentServerName() + ":params:" + tmp, params);
			   
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
			   //parser.Parse(parsedResponse, contents, Parser.PAGE_ROWS, getLinkPrefix(TSConnectionURL.idGET), TSServer.REQUEST_SAVE_TO_TSD);

				break;
				
			case ID_SEARCH_BY_PARCEL:	
			case ID_DETAILS :
				
				if (rsResponse.indexOf("We're sorry, but the system is unable to locate this property information") != -1){
					Response.getParsedResponse().setError("<font color=\"red\">No results found</font>");
					return;
				}
				
				String details = "";
				details = getDetails(rsResponse);
				String pid = "";
				try {
					org.htmlparser.Parser htmlParser = org.htmlparser.Parser.createParser(details, null);
					NodeList mainList = htmlParser.parse(null);
					pid = HtmlParser3.getValueFromNextCell(HtmlParser3.findNode(mainList, "Parcel"),"", true).trim();
				} catch(Exception e) {
					e.printStackTrace();
				}
				 
				if ((!downloadingForSave))
				{	
	                String qry_aux = Response.getRawQuerry();
					qry_aux = "dummy=" + pid + "&" + qry_aux;
					String originalLink = sAction + "&" + qry_aux;
					String sSave2TSDLink = getLinkPrefix(TSConnectionURL.idGET) + originalLink;
					
					HashMap<String, String> data = new HashMap<String, String>();
					data.put("type","ASSESSOR");
					data.put("dataSource", "AO");
					
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
	                //parser.Parse(Response.getParsedResponse(), details, Parser.PAGE_DETAILS);	 

				}
				
				break;	
			
			case ID_GET_LINK :
				if (rsResponse.indexOf("PROPERTY INFORMATION") != -1){
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
	
	protected String getDetails(String response){
		
		// if from memory - use it as is
		if(!(response.contains("<html") || response.contains("<HTML"))){
			return response;
		}
	
		String details = "";
		try {
			
			org.htmlparser.Parser htmlParser = org.htmlparser.Parser.createParser(response, null);
			NodeList mainTableList = htmlParser.parse(null);
			NodeList tableList = mainTableList.extractAllNodesThatMatch(new TagNameFilter("table"), true);
			if (tableList.size() == 8) {
				details = tableList.elementAt(2).toHtml() + tableList.elementAt(6).toHtml() + tableList.elementAt(7).toHtml();
			} else {
				details = response.replaceAll("(?is).*?</form>(.*)", "$1");
				details = details.replaceAll("(?is)</table[^>]*>\\s*</td\\s*>\\s*</tr\\s*>.*?</table\\s*>.*", "</table>");
				details = details.replaceAll("(?is)</a>", "");
			}
			details = details.replaceAll("(?is)(?is)<a.*?</a>", "");
			details = details.replaceAll("(?is)&amp;", "&");
			
			Matcher mat = CHAIN_OF_TITLE_LINK.matcher(response);
			if (mat.find()){
				HTTPRequest reqP = new HTTPRequest("http://www.denvergov.org/apps/realpropertyapplication/" + mat.group(1));
		    	reqP.setMethod(HTTPRequest.GET);
		    	
		    	HTTPResponse resP = null;
	        	HttpSite site = HttpManager.getSite("CODenverTR", searchId);
				try
				{
					resP = site.process(reqP);
				} finally 
				{
					HttpManager.releaseSite(site);
				}	
				String rsp = resP.getResponseAsString();
				rsp = rsp.replaceAll("(?is).*?(<table[^>]+>\\s*<tr[^>]*>\\s*<td[^>]*>\\s*\\*\\s*\\*\\s*CHAIN\\s*OF\\s*TITLE.*)", "$1");
				rsp = rsp.replaceAll("(?is)</table[^>]*>\\s*</td\\s*>\\s*</tr\\s*>.*?</table\\s*>.*", "</table>");
				details += "<br><br><br>" + rsp + "<br><br><br>";// if you modified the <br> tags, please check the parsing in server/functions/CODenverTR.java/parseAndFillResultMap
			}

		} catch (Exception e) {
			e.printStackTrace();
		}	
		return details;
	}
	
	public Collection<ParsedResponse> smartParseIntermediary(ServerResponse response, String table, StringBuilder outputTable) {
		
		 Vector<ParsedResponse> intermediaryResponse = new Vector<ParsedResponse>();;
		
		try {
			
			org.htmlparser.Parser htmlParser = org.htmlparser.Parser.createParser(table, null);
			NodeList mainTableList = htmlParser.parse(null);
			TableTag mainTable = (TableTag)mainTableList.elementAt(3);
			
			TableRow[] rows = mainTable.getRows();
			for(TableRow row : rows ) {
				if(row.getColumnCount() > 0) {
					
					TableColumn[] cols = row.getColumns();
					NodeList aList = cols[0].getChildren().extractAllNodesThatMatch(new TagNameFilter("a"), true);
					if (aList.size() > 0){
						String link = ((LinkTag) aList.elementAt(0)).extractLink();
						
						String rowHtml =  row.toHtml();
						
						ParsedResponse currentResponse = new ParsedResponse();
						currentResponse.setAttribute(ParsedResponse.SERVER_ROW_RESPONSE, row.toHtml());
						currentResponse.setOnlyResponse(rowHtml);
						currentResponse.setPageLink(new LinkInPage(link, link, TSServer.REQUEST_SAVE_TO_TSD));
						
						ResultMap m = ro.cst.tsearch.servers.functions.CODenverAO.parseIntermediaryRowCODenverAO(row, searchId);
						m.removeTempDef();
						Bridge bridge = new Bridge(currentResponse, m, searchId);
						
						DocumentI document = (AssessorDocumentI)bridge.importData();				
						currentResponse.setDocument(document);
						
						intermediaryResponse.add(currentResponse);
					}
				}
			}
				
			response.getParsedResponse().setHeader("Select a name from the list below to see the properties associated with that name.  The number in "
													+ "parenthesis represents the number of properties owned by that name.<br><br><table width=\"100%\" border=\"1\">");
			response.getParsedResponse().setFooter("</table>");			
		
		outputTable.append(table);
			
		}catch(Exception e) {
			e.printStackTrace();
		}
		
		return intermediaryResponse;
	}
	
	@Override
	protected Object parseAndFillResultMap(ServerResponse response,String detailsHtml, ResultMap map) {
		map.put(OtherInformationSetKey.SRC_TYPE.getKeyName(), getDataSite().getSiteTypeAbrev());
		ro.cst.tsearch.servers.functions.CODenverAO.parseAndFillResultMap(detailsHtml, map, searchId);
		return null;
	}

}
