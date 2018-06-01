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
import ro.cst.tsearch.exceptions.ServerResponseException;
import ro.cst.tsearch.extractor.xml.Bridge;
import ro.cst.tsearch.extractor.xml.ResultMap;
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
import ro.cst.tsearch.servers.TSConnectionURL;
import ro.cst.tsearch.servers.info.TSServerInfo;
import ro.cst.tsearch.servers.info.TSServerInfoModule;
import ro.cst.tsearch.servers.response.LinkInPage;
import ro.cst.tsearch.servers.response.ParsedResponse;
import ro.cst.tsearch.servers.response.Parser;
import ro.cst.tsearch.servers.response.ServerResponse;
import ro.cst.tsearch.utils.StringUtils;

import com.stewart.ats.base.document.AssessorDocumentI;
import com.stewart.ats.base.document.DocumentI;
import com.stewart.ats.base.document.TaxDocumentI;

/**
 * @author mihaib
  */

public class AKAnchorageAO extends TSServer {

	public static final long serialVersionUID = 10000000L;
	private boolean downloadingForSave; 
	
	private static final Pattern dummyPattern = Pattern.compile("&dummy=([0-9]+)&");
	
	
	public AKAnchorageAO(long searchId) {
		super(searchId);
	}

	public AKAnchorageAO(String rsRequestSolverName, String rsSitePath, String rsServerID, String rsPrmNameLink, long searchId, int mid) {
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
							if (tables.elementAt(i).toHtml().contains("Parcel ID")){
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
				
				pid = StringUtils.extractParameter(details, "(?i)PARCEL:\\s*(?:\\s*</span>\\s*<span[^>]*>)?([\\d-]+)\\s*");
							
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
				if (sAction.indexOf("Parcel") != -1){
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
		
		try {
			org.htmlparser.Parser htmlParser = org.htmlparser.Parser.createParser(response, null);
			NodeList mainList = htmlParser.parse(null);
			NodeList tables = mainList.extractAllNodesThatMatch(new TagNameFilter("table"), true);
			if (tables.size() > 3){
				for (int i = tables.size() - 1; i > 0 ; i--){
					if (tables.elementAt(i).toHtml().toLowerCase().contains("parcel:")){
						contents = tables.elementAt(i).toHtml();
						break;
					}
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}

		contents = contents.replaceAll("(?is)</?body[^>]*>", "");
		contents = contents.replaceAll("(?is)</?form[^>]*>", "");
		contents = contents.replaceAll("(?is)(?is)<script.*?</script>", "");
		contents = contents.replaceAll("(?is)<input[^>]*>", "");
		contents = contents.replaceAll("(?is)<a\\s+(href|name)=\\\"[^>]*>[^<]*</a>", "");
		contents = contents.replaceAll("(?is)<p\\s+[^>]*>.*?</p>", "");
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
								String link = CreatePartialLink(TSConnectionURL.idPOST) + "?ParcelId="  + parcel.replaceAll("(?is)[^']+'([^']*)'\\)", "$1");
								
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
					mSearch.setAdditionalInfo(getCurrentServerName() + ":paramsDetails:", paramsLink);
					
					String result = response.getResult();
					result = result.replaceAll("(?is)name\\s*=([A-Z]+)\\s+", "name=\"$1\" ");
					
					Map<String, String> paramsForNext = new SimpleHtmlParser(result).getForm("contform").getParams();
					if (paramsForNext != null){
						mSearch.setAdditionalInfo(getCurrentServerName() + ":paramsForNext:", paramsForNext);
					}
					
					String nextLink = "<a href=\"" + CreatePartialLink(TSConnectionURL.idPOST) + form.action + "\">Next</a>";
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
	
	@Override
	protected Object parseAndFillResultMap(ServerResponse response,String detailsHtml, ResultMap map) {
		ro.cst.tsearch.servers.functions.AKAnchorageAO.parseAndFillResultMap(detailsHtml, map, searchId);
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