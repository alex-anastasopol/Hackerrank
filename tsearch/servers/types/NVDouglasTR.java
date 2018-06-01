package ro.cst.tsearch.servers.types;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Vector;

import org.htmlparser.filters.HasAttributeFilter;
import org.htmlparser.filters.TagNameFilter;
import org.htmlparser.tags.TableRow;
import org.htmlparser.tags.TableTag;
import org.htmlparser.util.NodeList;

import ro.cst.tsearch.bean.SearchAttributes;
import ro.cst.tsearch.exceptions.ServerResponseException;
import ro.cst.tsearch.extractor.xml.Bridge;
import ro.cst.tsearch.extractor.xml.GenericFunctions2;
import ro.cst.tsearch.extractor.xml.ResultMap;
import ro.cst.tsearch.extractor.xml.ResultTable;
import ro.cst.tsearch.search.FunctionStatesIterator;
import ro.cst.tsearch.search.filter.FilterResponse;
import ro.cst.tsearch.search.filter.newfilters.address.AddressFilterFactory;
import ro.cst.tsearch.search.filter.newfilters.name.NameFilterFactory;
import ro.cst.tsearch.search.filter.newfilters.pin.PINFilterFactory;
import ro.cst.tsearch.search.iterator.ModuleStatesIteratorFactory;
import ro.cst.tsearch.search.module.ConfigurableNameIterator;
import ro.cst.tsearch.servers.TSConnectionURL;
import ro.cst.tsearch.servers.info.TSServerInfo;
import ro.cst.tsearch.servers.info.TSServerInfoModule;
import ro.cst.tsearch.servers.response.LinkInPage;
import ro.cst.tsearch.servers.response.ParsedResponse;
import ro.cst.tsearch.servers.response.PropertyIdentificationSet.PropertyIdentificationSetKey;
import ro.cst.tsearch.servers.response.ServerResponse;
import ro.cst.tsearch.utils.RequestParams;

import com.stewart.ats.base.document.DocumentI;
import com.stewart.ats.base.document.TaxDocumentI;

public class NVDouglasTR extends TSServer {
		
	private static final long serialVersionUID = 1L;
	
	public NVDouglasTR(long searchId) {
		super(searchId);
	}
	
	public NVDouglasTR(String rsRequestSolverName, String rsSitePath,
			String rsServerID, String rsPrmNameLink, long searchId,
			int miServerID) {
		super(rsRequestSolverName, rsSitePath, rsServerID, rsPrmNameLink, searchId,
				miServerID);
	}
	
	@Override
	protected void ParseResponse(String sAction, ServerResponse Response,
			int viParseID) throws ServerResponseException {
		
		String rsResponse = Response.getResult();
		ParsedResponse parsedResponse = Response.getParsedResponse();
		
		switch (viParseID) {
		case ID_SEARCH_BY_NAME:
		case ID_SEARCH_BY_ADDRESS:
		case ID_SEARCH_BY_PARCEL:
			
			if (rsResponse.indexOf("Your search string of #Len(SearchText)# characters would return too many records.") > -1) {
				Response.getParsedResponse().setError("Your search string of #Len(SearchText)# characters would return too many records. "
						+ "Please enter a search string of 3 or more charaters. Thank You.");
				return;
			}
			
			if (rsResponse.indexOf("How To Search:") > -1) {
				Response.getParsedResponse().setError("How To Search: "
						+ "Select what kind of information you want to search for on the Left "
						+ "Side (ie. Assessed Owner Name). "
						+ "Next type the what you want to search for (ie. SMITH, JOHN). "
						+ "Then click the search button or hit ENTER on your keyboard. "
						+ "The server will then search the database for matching records. Please "
						+ "be patient. It will show you all of the records it has found. Click on one "
						+ "of these records to view more information.");
				return;
			}
			
			if (rsResponse.indexOf("No records match your search") > -1) {
				Response.getParsedResponse().setError("No results found");
				return;
			}
			
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
			
			StringBuilder serialNumber = new StringBuilder();
			String details = getDetails(rsResponse, serialNumber);
			String filename = serialNumber + ".html";
			
			if (viParseID != ID_SAVE_TO_TSD) {
				String originalLink = sAction.replace("?", "&") + "&" + Response.getRawQuerry();
				String sSave2TSDLink = getLinkPrefix(TSConnectionURL.idPOST) + originalLink;

				HashMap<String, String> data = new HashMap<String, String>();
				loadDataHash(data);
				if (isInstrumentSaved(serialNumber.toString(),null,data)){
					details += CreateFileAlreadyInTSD();
				}
				else {
					mSearch.addInMemoryDoc(sSave2TSDLink, details);
					details = addSaveToTsdButton(details, sSave2TSDLink, ID_DETAILS);
				}

				Response.getParsedResponse().setPageLink(new LinkInPage(sSave2TSDLink,originalLink,TSServer.REQUEST_SAVE_TO_TSD));
				Response.getParsedResponse().setResponse( details );
				
			} else {
				smartParseDetails(Response,details);
				
				msSaveToTSDFileName = filename;
				Response.getParsedResponse().setFileName( getServerTypeDirectory() + msSaveToTSDFileName);	
				Response.getParsedResponse().setResponse( details );
				
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
	
	protected void loadDataHash(HashMap<String, String> data) {
		if(data != null) {
			data.put("type","CNTYTAX");
		}
	}

	protected String getDetails(String rsResponse, StringBuilder parcelNumber) {
		try {
			
			StringBuilder details = new StringBuilder();
			
			org.htmlparser.Parser htmlParser = org.htmlparser.Parser.createParser(rsResponse, null);
			NodeList nodeList = htmlParser.parse(null);
									
			/* If from memory - use it as is */
			if(!rsResponse.contains("<html")){
				NodeList parcelList = nodeList.extractAllNodesThatMatch(new TagNameFilter("table"), true)
					.extractAllNodesThatMatch(new HasAttributeFilter("id", "table1"))
					.extractAllNodesThatMatch(new TagNameFilter("td"), true);
				String parcelID = parcelList.elementAt(0).toPlainTextString().trim();
				parcelNumber.append(parcelID);
								
				return rsResponse;
			}
			
			NodeList parcelList = nodeList.extractAllNodesThatMatch(new TagNameFilter("table"), true)
				.extractAllNodesThatMatch(new HasAttributeFilter("id", "table1"))
				.extractAllNodesThatMatch(new TagNameFilter("td"), true);
			String parcelID = parcelList.elementAt(0).toPlainTextString().trim();
			parcelNumber.append(parcelID);
			
			NodeList tables = nodeList.extractAllNodesThatMatch(new TagNameFilter("table"), true)	
				.extractAllNodesThatMatch(new HasAttributeFilter("id", "table1"));
			
			if(tables.size() != 2) { 
				return null;
			}

			NodeList yearList = nodeList.extractAllNodesThatMatch(new TagNameFilter("h2"), true)
				.extractAllNodesThatMatch(new HasAttributeFilter("align", "center"));
			if (yearList.size() != 0)							
				details.append(yearList.elementAt(0).toHtml());	//title (which contains tax year)
			
			details.append(tables.elementAt(0).toHtml()			//details table
					.replaceAll("<a(.*?)</a>", ""));			//remove "Click here to pay taxes"
					
			details.append("<table width=\"100%\"><tr><td align=\"center\" valign=\"top\">"
					+"<h3>\"All Prior Years\" - Scroll down to see more</h3></td></tr></table>");
			
			details.append(tables.elementAt(1).toHtml());		//prior years taxes table
						
			return details.toString();
			
		} catch (Exception e) {
			logger.error("Error while getting details", e);
		}
		return null;
	}
		
	protected String createPartialLink(int iActionType, int dispatcher) {
		StringBuilder build = new StringBuilder(msRequestSolverName);
		build.append("?");
		build.append(msServerID);
		build.append("&");
		build.append(RequestParams.SEARCH_ID);
		build.append("=");
		build.append(mSearch.getSearchID());
		build.append("&");
		build.append("dispatcher=");
		build.append(dispatcher);
		return build.toString();
	}
		
	@Override
	public Collection<ParsedResponse> smartParseIntermediary(
			ServerResponse response, String table, StringBuilder outputTable) {
		Vector<ParsedResponse> intermediaryResponse = new Vector<ParsedResponse>();
		
		TableTag resultsTable = null;
		
		try {
			org.htmlparser.Parser htmlParser = org.htmlparser.Parser.createParser(table, null);
			NodeList nodeList = htmlParser.parse(null);
			NodeList mainTable = nodeList.extractAllNodesThatMatch(new TagNameFilter("table"), true)
				.extractAllNodesThatMatch(new HasAttributeFilter("width","600"), true)
				.extractAllNodesThatMatch(new TagNameFilter("table"), true);
			
			if (mainTable.size() == 4)
				resultsTable = (TableTag)mainTable.elementAt(3); 
			
			//if there are results
			if (resultsTable != null && resultsTable.getRowCount() != 0)		
			{
				TableRow[] rows  = resultsTable.getRows();
				String serialNumber = "";
				
				for (int i = 1; i < rows.length; i++) {
					TableRow row = rows[i];
										
					String htmlRow = row.toHtml();
					ParsedResponse currentResponse = new ParsedResponse();
					
					serialNumber = row.getColumns()[3].toPlainTextString();
					
					String link = CreatePartialLink(TSConnectionURL.idGET) 
						+ "/view1.cfm?parcel=" + serialNumber;
					
					//replace the links
					htmlRow = htmlRow.replaceAll("href=\".*\"", "href=" + link);
										
					currentResponse.setPageLink(new LinkInPage(link,link,TSServer.REQUEST_SAVE_TO_TSD));
									
					currentResponse.setAttribute(ParsedResponse.SERVER_ROW_RESPONSE,htmlRow);
					currentResponse.setOnlyResponse(htmlRow);
					
					ResultMap m = ro.cst.tsearch.servers.functions.NVDouglasTR.parseIntermediaryRow( row, searchId ); 
					Bridge bridge = new Bridge(currentResponse, m, searchId);
					
					DocumentI document = (TaxDocumentI)bridge.importData();				
					currentResponse.setDocument(document);
					
					intermediaryResponse.add(currentResponse);
					 
				}
				
				String header = "<table><tr>" +
					"<td><strong><font face=\"arial, helvecta\" size=2>Assessed Name</font></strong></td>" +
					"<td align=\"center\"><strong><font face=\"arial, helvecta\" size=2>#</font></strong></td>" +
					"<td><strong><font face=\"arial, helvecta\" size=2>Street</font></strong></td>" +
					"<td align=\"right\"><strong><font face=\"arial, helvecta\" size=2>Parcel Number</font></strong></td>" +
					"</tr>";
				response.getParsedResponse().setHeader(header);
				response.getParsedResponse().setFooter("</table>");
												
				outputTable.append(table);
			}
			
		} catch (Throwable t){
			logger.error("Error while parsing intermediary data", t);
		}
		
		return intermediaryResponse;
	}
	
	@Override
	protected Object parseAndFillResultMap(ServerResponse response,
			String detailsHtml, ResultMap resultMap) {
				
		try {
					
			resultMap.put("OtherInformationSet.SrcType","TR");
			
			org.htmlparser.Parser htmlParser = org.htmlparser.Parser.createParser(detailsHtml, null);
			NodeList nodeList = htmlParser.parse(null);
						
			NodeList parcelList = nodeList.extractAllNodesThatMatch(new TagNameFilter("table"), true)
				.extractAllNodesThatMatch(new HasAttributeFilter("id", "table1"))
				.extractAllNodesThatMatch(new TagNameFilter("td"), true);
			String parcelID = parcelList.elementAt(0).toPlainTextString().trim();
			resultMap.put(PropertyIdentificationSetKey.PARCEL_ID.getKeyName(), parcelID);
			
			String owner = parcelList.elementAt(1).toPlainTextString().trim();
			resultMap.put(PropertyIdentificationSetKey.NAME_ON_SERVER.getKeyName(), owner);
									
			ResultTable rt = new ResultTable();			//tax history table
			@SuppressWarnings("rawtypes")
			List<List> tablebody = new ArrayList<List>();
			List<String> list;
			
			TableTag taxesTable = (TableTag) nodeList.extractAllNodesThatMatch(new TagNameFilter("table"), true)	
				.extractAllNodesThatMatch(new HasAttributeFilter("id", "table1")).elementAt(1);
			int rowsNumber = taxesTable.getRowCount();
			String receiptAmount = "";
			String receiptDate = "";
			String operationType = "";
			
			for (int i=rowsNumber-1; i>=3; i--)			//first rows are headers
			{
				TableRow row = taxesTable.getRow(i);
				operationType = row.getColumns()[0].toPlainTextString().trim();
				if (operationType.equals("-Payment-") || operationType.equals("Penalty"))
				{
					receiptDate = row.getColumns()[1].toPlainTextString();
					if (operationType.equals("-Payment-")) 
						receiptAmount = row.getColumns()[2].toPlainTextString()
						.replaceAll("[\\(\\),\\$]", ""); 
					else if (operationType.equals("Penalty")) 
						receiptAmount = row.getColumns()[3].toPlainTextString()
						.replaceAll("[\\(\\),\\$]", "");;
					
					list = new ArrayList<String>();
					list.add(receiptAmount);
					list.add(receiptDate);
					tablebody.add(list);
				}
			}
			
			//payments for current year
			boolean isCurrentYearPaymentDates = false;
			taxesTable = (TableTag) nodeList.extractAllNodesThatMatch(new TagNameFilter("table"), true)	
				.extractAllNodesThatMatch(new HasAttributeFilter("id", "table1")).elementAt(0);
			rowsNumber = taxesTable.getRowCount();
			for (int i = 2; i < rowsNumber; i++)
			{
				TableRow row = taxesTable.getRow(i);
				String rowValue = row.toPlainTextString().replaceAll("&nbsp;", "");
				if (rowValue.equals("")) continue;
				if (rowValue.contains("Current Year Payment Dates"))
					isCurrentYearPaymentDates = true;
				else if (rowValue.contains("Prior Installments and Other Amounts (if any)")) 
				{
					isCurrentYearPaymentDates = false;
					break;
				}
				else if (isCurrentYearPaymentDates)
				{
					list = new ArrayList<String>();
					list.add(row.getColumns()[1].toPlainTextString());
					list.add(row.getColumns()[0].toPlainTextString());
					tablebody.add(list);
				} 
			}
			
			String[] header = {"ReceiptAmount", "ReceiptDate"};
			rt = GenericFunctions2.createResultTable(tablebody, header);
			if (rt != null){
				resultMap.put("TaxHistorySet", rt);
			}
						
			ro.cst.tsearch.servers.functions.NVDouglasTR.parseNames(resultMap, searchId);
			ro.cst.tsearch.servers.functions.NVDouglasTR.parseTaxes(nodeList, resultMap, searchId);
									
		
		} catch (Exception e) {
			logger.error("Error while getting details", e);
		}
		return null;
	}
		
	@Override
	protected void setModulesForAutoSearch(TSServerInfo serverInfo) {
		
		List<TSServerInfoModule> moduleList = new ArrayList<TSServerInfoModule>();
		
		TSServerInfoModule module = null;
		
		if(hasPin()) {
			FilterResponse pinFilter = PINFilterFactory.getDefaultPinFilter(searchId);
			module = new TSServerInfoModule(serverInfo.getModule(TSServerInfo.PARCEL_ID_MODULE_IDX));
			module.clearSaKeys();
			module.setSaKey(0, SearchAttributes.LD_PARCELNO);
			module.addFilter(pinFilter);
			moduleList.add(module);
		}
						
		boolean hasOwner = hasOwner();
		FilterResponse addressFilter = AddressFilterFactory.getAddressHybridFilter(searchId , 0.8d , true);
				
		if(hasStreet()) {
			FilterResponse nameFilter = NameFilterFactory.getHybridNameFilter( 
					SearchAttributes.OWNER_OBJECT , searchId , module);
			module = new TSServerInfoModule(serverInfo.getModule(TSServerInfo.ADDRESS_MODULE_IDX));
			module.clearSaKeys();
			module.setSaKey(0, SearchAttributes.P_STREETNAME);
			module.addFilter(addressFilter);
			module.addFilter(nameFilter);
			moduleList.add(module);
		}
								
		if(hasOwner) {
			FilterResponse nameFilterHybridDoNotSkipUnique = NameFilterFactory.getHybridNameFilter( 
					SearchAttributes.OWNER_OBJECT , searchId , module);
			nameFilterHybridDoNotSkipUnique.setSkipUnique(false);
			module = new TSServerInfoModule(serverInfo.getModule(TSServerInfo.NAME_MODULE_IDX));
			module.clearSaKeys();
			module.setSaObjKey(SearchAttributes.OWNER_OBJECT);
			module.setIteratorType(0, FunctionStatesIterator.ITERATOR_TYPE_LCF_NAME_FAKE);
			module.addFilter(addressFilter);
			module.addFilter(nameFilterHybridDoNotSkipUnique);
			module.addIterator((ConfigurableNameIterator) ModuleStatesIteratorFactory
					.getConfigurableNameIterator(module, searchId, new String[] {"L;F;","L;M;"})
					);
			moduleList.add(module);
		}
		
		serverInfo.setModulesForAutoSearch(moduleList);
	}
}
