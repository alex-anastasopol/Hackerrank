/**
 * 
 */
package ro.cst.tsearch.servers.types;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Vector;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import ro.cst.tsearch.Search;
import ro.cst.tsearch.bean.SearchAttributes;
import ro.cst.tsearch.exceptions.ServerResponseException;
import ro.cst.tsearch.extractor.xml.Bridge;
import ro.cst.tsearch.extractor.xml.ResultMap;
import ro.cst.tsearch.search.FunctionStatesIterator;
import ro.cst.tsearch.search.filter.FilterResponse;
import ro.cst.tsearch.search.filter.newfilters.address.AddressFilterFactory;
import ro.cst.tsearch.search.filter.newfilters.name.NameFilterFactory;
import ro.cst.tsearch.search.iterator.ModuleStatesIteratorFactory;
import ro.cst.tsearch.search.module.ConfigurableNameIterator;
import ro.cst.tsearch.servers.HashCountyToIndex;
import ro.cst.tsearch.servers.TSConnectionURL;
import ro.cst.tsearch.servers.bean.DataSite;
import ro.cst.tsearch.servers.info.TSServerInfo;
import ro.cst.tsearch.servers.info.TSServerInfoConstants;
import ro.cst.tsearch.servers.info.TSServerInfoModule;
import ro.cst.tsearch.servers.response.LinkInPage;
import ro.cst.tsearch.servers.response.ParsedResponse;
import ro.cst.tsearch.servers.response.SaleDataSet.SaleDataSetKey;
import ro.cst.tsearch.servers.response.ServerResponse;
import ro.cst.tsearch.servlet.URLConnectionReader;
import ro.cst.tsearch.utils.StringUtils;

import com.stewart.ats.base.document.DocumentI;
import com.stewart.ats.base.document.DriverDataDocument;
import com.stewart.ats.base.document.DriverDataDocumentI;
import com.stewart.ats.base.document.RegisterDocument;
import com.stewart.ats.base.document.RegisterDocumentI;

/**
 * @author vladb
 *
 */
public class TXDriverData extends TSServer {

	private static final long serialVersionUID = 1L;

	public TXDriverData(String rsRequestSolverName, String rsSitePath, String rsServerID, String rsPrmNameLink, long searchId, int miServerID) {
		super(rsRequestSolverName, rsSitePath, rsServerID, rsPrmNameLink, searchId, miServerID);		
		resultType = MULTIPLE_RESULT_TYPE;
	}
	
	@Override
	protected void ParseResponse(String sAction, ServerResponse Response, int viParseID) throws ServerResponseException {
		String rsResponse = Response.getResult();
		ParsedResponse parsedResponse = Response.getParsedResponse();
		
		switch (viParseID) {
		case ID_SEARCH_BY_MODULE41: // Driver's License Search
		case ID_SEARCH_BY_MODULE42: // Vehicle Search 
		case ID_SEARCH_BY_MODULE43: // Voter Registration Search 
			if(rsResponse.indexOf("/ErrorMsg/ErrorMsg.asp") > -1) { // too many results, need to press Continue button
				DataSite dataSite = HashCountyToIndex.getDateSiteForMIServerID(HashCountyToIndex.ANY_COMMUNITY, miServerID);
				rsResponse = getLinkContents(dataSite.getLink() + sAction + "?Button1=Continue");
			}
			// no result
			if (rsResponse.indexOf("No information matching your search criteria was discovered") > -1
					|| rsResponse.indexOf("Total Matching Records in Database: 0") > -1
					|| rsResponse.indexOf("Total Matching Records: 0") > -1) {
				Response.getParsedResponse().setError("No results found for your query! Please change your search criteria and try again.");
				return;
			}
			
			StringBuilder outputTable = new StringBuilder();
			Collection<ParsedResponse> smartParsedResponses = smartParseIntermediary(Response, rsResponse, outputTable);
			
			if(smartParsedResponses.size() == 0) {
				return;
			}

			parsedResponse.setHeader("<table border=\"1\">");
			parsedResponse.setFooter("</table>");
			
			parsedResponse.setResultRows(new Vector<ParsedResponse>(smartParsedResponses));
			parsedResponse.setOnlyResponse(outputTable.toString());
			
			if (getSearch().getSearchType() == Search.PARENT_SITE_SEARCH ) {                        	
            	String header = CreateSaveToTSDFormHeader(URLConnectionReader.MULTIPLE_SAVE_TO_TSD_ACTION, "POST");
            	String footer = "";
            	header += "\n<table width=\"100%\" border=\"1\">\n" + "<tr><th>"+ SELECT_ALL_CHECKBOXES + "</th><th></th>";

            	Object numberOfUnsavedDocument = GetAttribute(NUMBER_OF_UNSAVED_DOCUMENTS);
            	if(numberOfUnsavedDocument != null && numberOfUnsavedDocument instanceof Integer) {
            		footer = "\n</table><br>" + CreateSaveToTSDFormEnd(SAVE_SELECTED_DOCUMENTS_BUTTON_LABEL, viParseID, (Integer)numberOfUnsavedDocument);
            	} else {
            		footer = "\n</table><br>" + CreateSaveToTSDFormEnd(SAVE_SELECTED_DOCUMENTS_BUTTON_LABEL, viParseID, -1);
            	}
            	
            	parsedResponse.setHeader(header);
            	parsedResponse.setFooter(footer);
            }
			
			break;
		case ID_SAVE_TO_TSD :
			DocumentI document = parsedResponse.getDocument();
			
			if(document!= null) {
				msSaveToTSDFileName = document.getInstno() + ".html";
				parsedResponse.setFileName(getServerTypeDirectory() + msSaveToTSDFileName);
				msSaveToTSDResponce = rsResponse + CreateFileAlreadyInTSD();
			}
		}
	}

	@Override
	public Collection<ParsedResponse> smartParseIntermediary(ServerResponse response, String page, StringBuilder outputTable) {
		
		LinkedHashMap<String, ParsedResponse> intermediaryResponse = new LinkedHashMap<String, ParsedResponse>();
		int numberOfUncheckedElements = 0;
		String linkPrefix = getLinkPrefix(TSConnectionURL.idPOST);
		
		try {
			String tableHtml = "";
			Matcher m = Pattern.compile("(?is)<div\\s+style='overflow:auto;height:75%;'>(.*?)</div>").matcher(page);
			if(m.find()) {
				tableHtml = m.group(1);
			} else {
				return intermediaryResponse.values();
			}
			
			String[] rows = tableHtml.split("(?is)<hr>");
			
			for(String row : rows) {
				row = row.replaceAll("(?is)<a[^>]*>(.*?)</a>", "$1");
				row = row.replaceAll("\\[MQ Map\\]\\[Yahoo Map\\]", "");
				
				ResultMap resultMap = ro.cst.tsearch.servers.functions.TXDriverData.parseRow(row);
				String pin = (String) resultMap.get(SaleDataSetKey.INSTRUMENT_NUMBER.getKeyName());
				if(StringUtils.isEmpty(pin) || intermediaryResponse.get(pin) != null) {
					continue;
				}
				String dob = (String) resultMap.get("tmpDOB");
				String renewalDate = (String) resultMap.get("tmpRenewalDate");
				String expDate = (String) resultMap.get("tmpExpirationDate");
				String plate = (String) resultMap.get("tmpPlate");
				
				resultMap.removeTempDef();
				ParsedResponse currentResponse = new ParsedResponse();
				Bridge bridge = new Bridge(currentResponse, resultMap, searchId);
				RegisterDocumentI regDoc = (RegisterDocumentI)bridge.importData();
				DriverDataDocumentI driverDataDoc = new DriverDataDocument((RegisterDocument)regDoc);
				
				if(StringUtils.isNotEmpty(dob)) {
					driverDataDoc.setDateOfBirth(dob);
				}
				if(StringUtils.isNotEmpty(renewalDate)) {
					driverDataDoc.setRenewalDate(renewalDate);
				}
				if(StringUtils.isNotEmpty(expDate)) {
					driverDataDoc.setExpirationDate(expDate);
				}
				if(StringUtils.isNotEmpty(plate)) {
					driverDataDoc.setPlate(plate);
				}
				
				currentResponse.setDocument(driverDataDoc);
				
				String checkBox = "checked";
				if (isInstrumentSaved(pin, driverDataDoc, null) && !Boolean.TRUE.equals(getSearch().getAdditionalInfo("RESAVE_DOCUMENT"))) {
	    			checkBox = "saved";
	    		} else {
	    			numberOfUncheckedElements++;
	    			LinkInPage linkInPage = new LinkInPage(
	    					linkPrefix + "DD__" + pin, 
	    					linkPrefix + "DD__" + pin, 
	    					TSServer.REQUEST_SAVE_TO_TSD);
	    			checkBox = "<input type='checkbox' name='docLink' value='" + linkPrefix + "DD__" + pin + "'>";
        			if(getSearch().getInMemoryDoc(linkPrefix + "DD__" + pin)==null){
        				getSearch().addInMemoryDoc(linkPrefix + "DD__" + pin, currentResponse);
        			}
        			currentResponse.setPageLink(linkInPage);
	    		}
				String rowHtml = "<tr><td>" + checkBox + "</td><td>" + row + "</td></tr>";
				currentResponse.setAttribute(ParsedResponse.SERVER_ROW_RESPONSE, row);
				currentResponse.setOnlyResponse(rowHtml);
				
				intermediaryResponse.put(pin, currentResponse);
			}
			
			outputTable.append(page);
		} catch (Exception e) {
			e.printStackTrace();
		}
		SetAttribute(NUMBER_OF_UNSAVED_DOCUMENTS, numberOfUncheckedElements);
		
		return intermediaryResponse.values();
	}
	
	@Override
	protected String getFileNameFromLink(String link) {
		
		if(link.contains("DD__")){
			link = link.substring(link.indexOf("DD__") + "DD__".length());
			if(link.contains("&")){
				link = link.substring(0,link.indexOf("&"));
			}
		}
			
        return link;
    }
	
	@Override
	protected TSServer.ADD_DOCUMENT_RESULT_TYPES addDocumentInATS(ServerResponse response, String htmlContent, boolean forceOverritten) {
		TSServer.ADD_DOCUMENT_RESULT_TYPES status = super.addDocumentInATS(response, htmlContent, forceOverritten);
		if(mSearch.getSearchType() == Search.AUTOMATIC_SEARCH && status == ADD_DOCUMENT_RESULT_TYPES.ADDED) {
			mSearch.setAdditionalInfo(getCurrentServerName() + ":docsSavedInAutomatic:", new Boolean(true));
		}
		
		return status;
	};
	
	/**
	 * if no document was saved while searching by address in automatic, enable searching by name
	 * see Bug 6263 for more details
	 */
	@Override
	protected ServerResponse SearchBy(boolean resetQuery, TSServerInfoModule module, Object sd)throws ServerResponseException {
		
		if (mSearch.getSearchType() == Search.AUTOMATIC_SEARCH){
			Object extraInfo = module.getExtraInformation(TSServerInfoConstants.EXTRA_PARAM_MODULE_DESCRIPTION);
			if (extraInfo != null && extraInfo.equals(TSServerInfoConstants.VALUE_PARAM_NAME_OWNERS)) {
				Boolean docsSavedInAutomatic = (Boolean) mSearch.getAdditionalInfo(getCurrentServerName() + ":docsSavedInAutomatic:");
				Boolean searchByNameEnabled = (Boolean) mSearch.getAdditionalInfo(getCurrentServerName() + ":searchByNameEnabled:");
				if(docsSavedInAutomatic == null || docsSavedInAutomatic.booleanValue() == false) {
					mSearch.setAdditionalInfo(getCurrentServerName() + ":searchByNameEnabled:", new Boolean(true));
					return super.SearchBy(resetQuery, module, sd);
				} else if(searchByNameEnabled != null && searchByNameEnabled.booleanValue() == true) {
					return super.SearchBy(resetQuery, module, sd);
				} else {
					return new ServerResponse();
				}
			}
		}
		
		return super.SearchBy(resetQuery, module, sd);
	}
	
	@Override
	protected void setModulesForAutoSearch(TSServerInfo serverInfo) {
		List<TSServerInfoModule> modules = new ArrayList<TSServerInfoModule>();		
		TSServerInfoModule module;
		FilterResponse addressFilter = AddressFilterFactory.getAddressHybridFilter(searchId, 0.80d);
		
		// search by address, filter by name
		if(hasStreet() && hasStreetNo()) {
			module = new TSServerInfoModule(serverInfo.getModule(TSServerInfo.MODULE_IDX41));
			
			module.clearSaKeys();
			module.setSaKey(3, SearchAttributes.P_STREET_NO_NAME);
			
			module.addFilter(addressFilter); 
			module.addFilter(NameFilterFactory.getDefaultNameFilterNoSinonims(SearchAttributes.OWNER_OBJECT, searchId, module));
			
			modules.add(module);
		}
		
		// search by name and zip
		if(hasOwner() && hasZip()) {
			module = new TSServerInfoModule(serverInfo.getModule(TSServerInfo.MODULE_IDX41));
			/* info used in SearchBy method */
			module.addExtraInformation(TSServerInfoConstants.EXTRA_PARAM_MODULE_DESCRIPTION, 
					TSServerInfoConstants.VALUE_PARAM_NAME_OWNERS);
			
			module.clearSaKeys();
			module.setSaObjKey(SearchAttributes.OWNER_OBJECT);
			module.setSaKey(6, SearchAttributes.P_ZIP);
			
			module.addFilter(NameFilterFactory.getDefaultNameFilterNoSinonims(SearchAttributes.OWNER_OBJECT, searchId, module));
			
			module.setIteratorType(0,FunctionStatesIterator.ITERATOR_TYPE_FIRST_NAME_FAKE);
			module.setIteratorType(1,FunctionStatesIterator.ITERATOR_TYPE_MIDDLE_NAME_FAKE);
			module.setIteratorType(2,FunctionStatesIterator.ITERATOR_TYPE_LAST_NAME_FAKE);
			ConfigurableNameIterator nameIterator = (ConfigurableNameIterator) ModuleStatesIteratorFactory
				.getConfigurableNameIterator(module, searchId, new String[] {"L;F;M", "L;F;"});
			nameIterator.setIgnoreMCLast(true);
			nameIterator.setIgnoreCompanies(true);
			nameIterator.clearSearchedNames();
			nameIterator.setInitAgain(true);
			module.addIterator(nameIterator);
			
			modules.add(module);			
		}

		serverInfo.setModulesForAutoSearch(modules);
	}
}
