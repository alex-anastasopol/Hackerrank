package ro.cst.tsearch.servers.types;



import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Vector;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.htmlparser.filters.TagNameFilter;
import org.htmlparser.tags.TableColumn;
import org.htmlparser.tags.TableRow;
import org.htmlparser.tags.TableTag;
import org.htmlparser.util.NodeList;

import ro.cst.tsearch.bean.SearchAttributes;
import ro.cst.tsearch.exceptions.ServerResponseException;
import ro.cst.tsearch.extractor.xml.Bridge;
import ro.cst.tsearch.extractor.xml.ResultMap;
import ro.cst.tsearch.extractor.xml.StringFormats;
import ro.cst.tsearch.parser.HtmlParser3;
import ro.cst.tsearch.search.FunctionStatesIterator;
import ro.cst.tsearch.search.filter.FilterResponse;
import ro.cst.tsearch.search.filter.newfilters.address.AddressFilterFactory;
import ro.cst.tsearch.search.filter.newfilters.address.GenericAddressFilter;
import ro.cst.tsearch.search.filter.newfilters.name.NameFilterFactory;
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


/**
* @author mihaib
**/

public class TXJohnsonAO extends TSServer {

	public static final long serialVersionUID = 10000000L;
	private boolean downloadingForSave; 
	private static final Pattern dummyPattern = Pattern.compile("&dummy=([0-9]+)&");
	
		
	public TXJohnsonAO(long searchId) {
		super(searchId);
	}

	public TXJohnsonAO(String rsRequestSolverName, String rsSitePath, String rsServerID, String rsPrmNameLink, long searchId, int mid) {
		super(rsRequestSolverName, rsSitePath, rsServerID, rsPrmNameLink, searchId, mid);
	}

	@Override
	public ServerResponse SearchBy(TSServerInfoModule module, Object sd) throws ServerResponseException {
	
		// change account number if necessary
        if(module.getModuleIdx() == TSServerInfo.PARCEL_ID_MODULE_IDX){
        	String pid = module.getFunction(0).getParamValue();
        	pid = pid.replaceAll("(?is)\\p{Punct}", "");
        	pid = pid.replaceAll("(?is)\\s+", "");
        	pid = pid.replaceAll("(?is)(\\d{3})(\\d{4})(\\d{5})", "$1.$2.$3");
			
           	module.getFunction(0).setParamValue(pid);
          
        }
        return super.SearchBy(module, sd);
	}
	
	@Override
	protected void setModulesForAutoSearch(TSServerInfo serverInfo) {
		List<TSServerInfoModule> modules = new ArrayList<TSServerInfoModule>();
		TSServerInfoModule module = null;
		String pid = getSearch().getSa().getAtribute(SearchAttributes.LD_PARCELNO);
		String address = getSearch().getSa().getAtribute(SearchAttributes.P_STREET_FULL_NAME_EX);
	
		GenericAddressFilter addressFilter 	= AddressFilterFactory.getGenericAddressHighPassFilter( searchId, 0.8d );
		FilterResponse defaultNameFilter = NameFilterFactory.getDefaultNameFilter(SearchAttributes.OWNER_OBJECT , searchId , module );

		if (hasPin()) {
			//Search by Account Number
			pid = pid.replaceAll("(?is)\\p{Punct}", "");
			pid = pid.replaceAll("(?is)\\s+", "");
			pid = pid.replaceAll("(?is)(\\d{3})(\\d{4})(\\d{5})", "$1.$2.$3");

			module = new TSServerInfoModule(serverInfo.getModule(TSServerInfo.PARCEL_ID_MODULE_IDX));
			module.clearSaKeys();
			module.setData(0, pid);
			modules.add(module);
		}
		
		if (hasStreet()) {
			//Search by Property Address
			address = address.replaceAll("(?is)\\bcounty\\s+road\\b", "cr");
			module = new TSServerInfoModule(serverInfo.getModule(TSServerInfo.ADDRESS_MODULE_IDX));
			module.clearSaKeys();
			module.forceValue(0, address);
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
			module.setIteratorType(0, FunctionStatesIterator.ITERATOR_TYPE_LF_NAME_FAKE);
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
				
				if (rsResponse.indexOf("No matches found") != -1){
					Response.getParsedResponse().setError("<font color=\"red\">No results found</font>");
					return;
				}
				
				try {
					 
					StringBuilder outputTable = new StringBuilder();
					ParsedResponse parsedResponse = Response.getParsedResponse();
					 
					String table = "";
					HtmlParser3 parser = new HtmlParser3(rsResponse);
					table = parser.getNodeByAttribute("class", "results", true).toHtml();
													
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
				try {
					HtmlParser3 parser = new HtmlParser3(rsResponse);
					pid = parser.getNodeByAttribute("class", "page_title", true).toHtml();
					pid = pid.replaceAll("</?div[^<]*>", "").replaceAll("(?is)\\s*Account\\s+Details\\s+for\\s*", "").trim();
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
					smartParseDetails(Response, details);
	                msSaveToTSDFileName = pid + ".html";
	                Response.getParsedResponse().setFileName(getServerTypeDirectory() + msSaveToTSDFileName);
	                msSaveToTSDResponce = details + CreateFileAlreadyInTSD();                
				}
				
				break;	
			
			case ID_GET_LINK :
				if (sAction.indexOf("details.php") != -1){
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
		
		String contents = "";
		try {
			org.htmlparser.Parser htmlParser = org.htmlparser.Parser.createParser(rsResponse, null);
			NodeList mainList = htmlParser.parse(null);
			HtmlParser3 parser = new HtmlParser3(rsResponse);
			contents += parser.getNodeByAttribute("class", "page_title", true).toHtml();
			NodeList tables = mainList.extractAllNodesThatMatch(new TagNameFilter("table"), true);
			for (int k = 0; k < tables.size(); k++){
				contents += tables.elementAt(k).toHtml() + "<br><br><br>";
			}
			
		} catch (Exception e) {
			e.printStackTrace();
		}
		contents = contents.replaceAll("(?is)(<Table id='history')", "Appraisal History<br><br><br>$1");
		
		return contents;
	}
	
	public Collection<ParsedResponse> smartParseIntermediary(ServerResponse response, String table, StringBuilder outputTable) {
		
		 Vector<ParsedResponse> intermediaryResponse = new Vector<ParsedResponse>();
		
		try {
			
			org.htmlparser.Parser htmlParser = org.htmlparser.Parser.createParser(table, null);
			NodeList mainTableList = htmlParser.parse(null);
			TableTag mainTable = (TableTag)mainTableList.elementAt(0);
			
			TableRow[] rows = mainTable.getRows();
						
			for(int i = 1; i < rows.length; i++ ) {
				if(rows[i].getColumnCount() > 1) {
					ResultMap resultMap = new ResultMap();
					
					TableColumn[] cols = rows[i].getColumns();
					String parcelNo = cols[0].getChild(0).getFirstChild().getText();
					String ownerName = cols[1].getChildrenHTML().trim();
					String address = cols[2].getChildrenHTML().trim();
					
					String rowHtml =  rows[i].toHtml().replaceFirst("(?is)<a\\s+href=[\\\"']([^\\\"']+)[^>]+>",
										"<a href=\"" + CreatePartialLink(TSConnectionURL.idGET) + "$1\">" ).replaceAll("&amp;", "&");
					String link = rowHtml.replaceAll("(?is).*?<a[^\\\"]+\\\"([^\\\"]+).*", "$1");
					
					ParsedResponse currentResponse = new ParsedResponse();
					currentResponse.setAttribute(ParsedResponse.SERVER_ROW_RESPONSE, rows[i].toHtml());
					currentResponse.setOnlyResponse(rowHtml);
					currentResponse.setPageLink(new LinkInPage(link, link, TSServer.REQUEST_SAVE_TO_TSD));
					
					resultMap.put("OtherInformationSet.SrcType", "AO");
					resultMap.put("PropertyIdentificationSet.ParcelID", parcelNo);
					resultMap.put("tmpOwner", ownerName);
					if (StringUtils.isNotEmpty(address)){
						resultMap.put("PropertyIdentificationSet.StreetNo", StringFormats.StreetNo(address));
						resultMap.put("PropertyIdentificationSet.StreetName", StringFormats.StreetName(address));
					}
					
					ro.cst.tsearch.servers.functions.TXJohnsonAO.partyNamesTXJohnsonAO(resultMap, getSearch().getID());
					ro.cst.tsearch.servers.functions.TXJohnsonAO.parseAddressTXJohnsonAO(resultMap, getSearch().getID());
					
					resultMap.removeTempDef();
					
					Bridge bridge = new Bridge(currentResponse, resultMap, searchId);
					
					DocumentI document = (AssessorDocumentI)bridge.importData();				
					currentResponse.setDocument(document);
					
					intermediaryResponse.add(currentResponse);
				}
			}
			
		
		String header1 = rows[0].toHtml();
		String header0 = "<table>";
			
		header1 = header1.replaceAll("(?is)</?a[^>]*>","");
		Pattern tablePat = Pattern.compile("<table[^>]+>");
		Matcher mat = tablePat.matcher(table);
		if (mat.find()){
			header0 = mat.group(0);
		}
		response.getParsedResponse().setHeader(header0 +  header1);
				
		response.getParsedResponse().setFooter("</table>");

		
		outputTable.append(table);
			
		}catch(Exception e) {
			e.printStackTrace();
		}
		
		return intermediaryResponse;
	}
		
	@Override
	protected Object parseAndFillResultMap(ServerResponse response, String detailsHtml, ResultMap map) {
			ro.cst.tsearch.servers.functions.TXJohnsonAO.parseAndFillResultMap(detailsHtml, map, searchId, miServerID);
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