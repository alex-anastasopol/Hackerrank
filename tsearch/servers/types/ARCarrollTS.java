package ro.cst.tsearch.servers.types;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Vector;

import com.stewart.ats.base.document.InstrumentI;

import ro.cst.tsearch.connection.dasl.DaslConnectionSiteInterface.DaslResponse;
import ro.cst.tsearch.exceptions.ServerResponseException;
import ro.cst.tsearch.search.filter.BetweenDatesFilterResponse;
import ro.cst.tsearch.search.filter.newfilters.legal.GenericMultipleLegalFilter;
import ro.cst.tsearch.servers.info.TSServerInfo;
import ro.cst.tsearch.servers.info.TSServerInfoConstants;
import ro.cst.tsearch.servers.info.TSServerInfoFunction;
import ro.cst.tsearch.servers.info.TSServerInfoModule;
import ro.cst.tsearch.servers.response.ParsedResponse;
import ro.cst.tsearch.servers.response.ServerResponse;
import ro.cst.tsearch.templates.AddDocsTemplates;
import ro.cst.tsearch.utils.StringUtils;


public class ARCarrollTS extends ARGenericAddictionTS {

	private static final long serialVersionUID = 1L;

	public ARCarrollTS(long searchId) {
		super(searchId);
	}

	public ARCarrollTS(String rsRequestSolverName, String rsSitePath, String rsServerID, String rsPrmNameLink, long searchId, int mid) {
		super(rsRequestSolverName, rsSitePath, rsServerID, rsPrmNameLink, searchId, mid);
	}
	
	@Override
	protected HashMap<String, Object> fillTemplatesParameters(Map<String, String> params) {
		HashMap<String, Object> templateParams = super.fillTemplatesParameters(params);
		
		String countyFips = params.get(AddDocsTemplates.DASLCountyFIPS);
		if(StringUtils.isNotEmpty(countyFips)) {
			templateParams.put(AddDocsTemplates.DASLCountyFIPS, countyFips);
		}
		
		return templateParams;
	}
	
	public ServerResponse SearchBy(TSServerInfoModule module, Object sd) throws ServerResponseException {
		
		int functionId = module.addFunction();
		TSServerInfoFunction function = module.getFunction(functionId);
		function.setName(AddDocsTemplates.DASLCountyFIPS);
		function.forceValue("015");
		function.setParamName(AddDocsTemplates.DASLCountyFIPS);
		ServerResponse firstResponse = super.SearchBy(module, sd);
		
		function.forceValue("999");
		Object oldValue = module.addExtraInformation(TSServerInfoConstants.EXTRA_PARAM_DO_NOT_REMOVE_IN_MEMORY_DOCS, true);
		ServerResponse secondResponse = super.SearchBy(module, sd);
		module.addExtraInformation(TSServerInfoConstants.EXTRA_PARAM_DO_NOT_REMOVE_IN_MEMORY_DOCS, oldValue);
		
		return mergeMultipleResponses(firstResponse, secondResponse);
	}
	
	
	@SuppressWarnings({ "unchecked", "rawtypes" })
	private ServerResponse mergeMultipleResponses(
			ServerResponse ... allValidResponses) {
		
		
		ServerResponse serverResponseFinal = null;
		for (ServerResponse serverResponse : allValidResponses) {
			ParsedResponse parsedResponse = serverResponse.getParsedResponse();
	    	//String rsResponce = serverResponse.getResult();
	    	
	    	//checkErrorMessages(parsedResponse, rsResponce);
	    	
	    	//if(StringUtils.isEmpty(parsedResponse.getError())) {
	    		//Do Something
	    		if(serverResponseFinal == null) {
	    			serverResponseFinal = serverResponse;
	    		} else {
	    			Vector allParsedResponse = serverResponseFinal.getParsedResponse().getResultRows();
	    			allParsedResponse.addAll(parsedResponse.getResultRows());
	    			serverResponseFinal.getParsedResponse().setResultRows(allParsedResponse);
	    		}
	    			
	    	//}
		}
		
		//ServerResponse serverResponse = new ServerResponse();
    	//serverResponse.getParsedResponse().setResultRows(allParsedResponse);
    	
		return serverResponseFinal;
	}
	
	protected DaslResponse  performImageSearch(String book, String page, String docno, String year, String month,
			String day, String type, String isPlat, String DASLImageSearchType, int moduleIDX){
		
		HashMap<String, String> params = new HashMap<String, String>();
		DaslResponse daslResponse = null;

		daslResponse = performImageSearchInternal(book, page, docno, year, month, day, type, isPlat,
				DASLImageSearchType, moduleIDX, params, daslResponse);
		
		if(daslResponse == null) {
			params.clear();
			params.put(AddDocsTemplates.DASLCountyFIPS, "999");
			daslResponse = null;
			daslResponse = performImageSearchInternal(book, page, docno, year, month, day, type, isPlat,
					DASLImageSearchType, moduleIDX, params, daslResponse);
		}
		
		if(daslResponse == null) {
			throw new RuntimeException("Could not find the Image");
		}
		
		
		return daslResponse;
		
	}

	protected DaslResponse performImageSearchInternal(String book, String page, String docno, String year,
			String month, String day, String type, String isPlat, String DASLImageSearchType, int moduleIDX,
			HashMap<String, String> params, DaslResponse daslResponse) {
		String xmlQuery;
		String countyFips = params.get(AddDocsTemplates.DASLCountyFIPS);
		//search with instrument no and type and year
		if(!StringUtils.isEmpty(docno)){
			params.put("docno", docno);
			params.put("type", type);
			params.put("DASLImageSearchType", DASLImageSearchType);
			if( !StringUtils.isEmpty(year) ){
				params.put("year", year);
			}
			if( !StringUtils.isEmpty(month) ){
				params.put("month", month);
			}
			if( !StringUtils.isEmpty(day) ){
				params.put("day", day);
			}
			params.put("isPlat", String.valueOf(isPlat));
			
			// create XML query
			xmlQuery = buildSearchQuery(params, moduleIDX);
			if ( StringUtils.isEmpty( xmlQuery ) ) {
				return null;
			}
			daslResponse = getDaslSite().performSearch(xmlQuery, searchId);
			
			if( isMultipleImagesResultNotNull( daslResponse ) ){
				daslResponse = downloadImagesUsingImageID(params, daslResponse, moduleIDX, true);
			}
		}
		
		if( couldNotDownloadImage(daslResponse) ){
			//search with B P and type and year  
			if(!StringUtils.isEmpty(book)&&!StringUtils.isEmpty(page)){
				params = new HashMap<String, String>();
				params.put(AddDocsTemplates.DASLCountyFIPS, countyFips);
				params.put("book", book);
				params.put("page", page);
				params.put("type", type);
				params.put("DASLImageSearchType", DASLImageSearchType);
				params.put("DASLSearchType", "IMG");
				if(!StringUtils.isEmpty(year)){
					params.put("year", year);
				}
				params.put("isPlat", String.valueOf(isPlat));
				
				// create XML query
				xmlQuery = buildSearchQuery(params, moduleIDX);
				if ( StringUtils.isEmpty(xmlQuery ) ) {
					return null;
				}
				daslResponse = getDaslSite().performSearch(xmlQuery, searchId );
				
				if(couldNotDownloadImage(daslResponse)){//TRY again without year
					params = new HashMap<String, String>();
					params.put(AddDocsTemplates.DASLCountyFIPS, countyFips);
					params.put("book", book);
					params.put("page", page);
					params.put("type", type);
					params.put("DASLImageSearchType", DASLImageSearchType);
					params.put("DASLSearchType", "IMG");
					params.put("year", "");
					params.put("isPlat", String.valueOf(isPlat));
					
					// create XML query
					xmlQuery = buildSearchQuery(params, moduleIDX);
					if ( StringUtils.isEmpty( xmlQuery ) ) {
						return null;
					}
					daslResponse = getDaslSite().performSearch(xmlQuery, searchId);
				}
				
				if( isMultipleImagesResultNotNull( daslResponse ) ){
					daslResponse = downloadImagesUsingImageID(params, daslResponse, moduleIDX, false);
				}
				
				if(daslResponse==null){
					return null;
				}
				boolean useId = useIds(daslResponse.xmlResponse);
				List<String> nameList = getFileNameList( daslResponse.xmlResponse ,useId);
				if( nameList.size()>0 && StringUtils.isEmpty( identifyGoodImageName(book, page, year, type, nameList ) ) ){
					return null;
				}
				
			}
		}
		return daslResponse;
	}
	
	@Override
	protected boolean addBookPageSearch(InstrumentI inst,TSServerInfo serverInfo, List<TSServerInfoModule> modules,long searchId,  boolean isUpdate){
		if(inst.hasBookPage()){
			TSServerInfoModule module = new TSServerInfoModule(serverInfo.getModule(TSServerInfo.BOOK_AND_PAGE_MODULE_IDX));
			module.setData(0, inst.getBook().replaceFirst("^0+", ""));
			module.setData(1, inst.getPage().replaceFirst("^0+", ""));
			
			GenericMultipleLegalFilter legalFilter = getGenericMultipleLegalFilter();
			legalFilter.setUseLegalFromSearchPage(true);
			module.addFilter(legalFilter);
			
			if (isUpdate) {
				module.addFilter(BetweenDatesFilterResponse.getDefaultIntervalFilter(searchId)); 
			}
			modules.add(module);
			return true;
		}
		return false;
	}
	
	@Override
	protected  boolean addInstNoSearch(InstrumentI inst,TSServerInfo serverInfo, List<TSServerInfoModule> modules,long searchId, boolean isUpdate){
		if ( inst.hasInstrNo() ){
			TSServerInfoModule module = new TSServerInfoModule(serverInfo.getModule(TSServerInfo.INSTR_NO_MODULE_IDX));
			String  instNo = prepareInstrumentNoForCounty(inst);
			module.setData(0,  instNo);
			
			GenericMultipleLegalFilter legalFilter = getGenericMultipleLegalFilter();
			legalFilter.setUseLegalFromSearchPage(true);
			module.addFilter(legalFilter);
			
			if (isUpdate) {
				module.addFilter(BetweenDatesFilterResponse.getDefaultIntervalFilter(searchId));
			}
			modules.add(module);
			return true;
		}
		return false;
	}
}
