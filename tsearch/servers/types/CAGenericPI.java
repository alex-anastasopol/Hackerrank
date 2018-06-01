package ro.cst.tsearch.servers.types;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang.StringEscapeUtils;
import org.apache.commons.lang.StringUtils;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import ro.cst.tsearch.Search;
import ro.cst.tsearch.bean.SearchAttributes;
import ro.cst.tsearch.data.CountyConstants;
import ro.cst.tsearch.data.StateCountyManager;
import ro.cst.tsearch.exceptions.ServerResponseException;
import ro.cst.tsearch.extractor.legal.LegalDescription;
import ro.cst.tsearch.extractor.xml.GenericFunctions2;
import ro.cst.tsearch.extractor.xml.ResultMap;
import ro.cst.tsearch.extractor.xml.ResultTable;
import ro.cst.tsearch.extractor.xml.StringFormats;
import ro.cst.tsearch.parentsitedescribe.ComboValue;
import ro.cst.tsearch.parentsitedescribe.HtmlControlMap;
import ro.cst.tsearch.search.FunctionStatesIterator;
import ro.cst.tsearch.search.GenericRuntimeIterator;
import ro.cst.tsearch.search.ModuleStatesIterator;
import ro.cst.tsearch.search.filter.BetweenDatesFilterResponse;
import ro.cst.tsearch.search.filter.FilterResponse;
import ro.cst.tsearch.search.filter.RejectAlreadySavedDocumentsFilterResponse;
import ro.cst.tsearch.search.filter.newfilters.address.AddressFilterFactory;
import ro.cst.tsearch.search.filter.newfilters.date.LastTransferDateFilter;
import ro.cst.tsearch.search.filter.newfilters.doctype.DocTypeAdvancedFilter;
import ro.cst.tsearch.search.filter.newfilters.legal.GenericInstrumentFilter;
import ro.cst.tsearch.search.filter.newfilters.legal.GenericMultipleLegalFilter;
import ro.cst.tsearch.search.filter.newfilters.legal.LegalFilterFactory;
import ro.cst.tsearch.search.filter.newfilters.name.GenericNameFilter;
import ro.cst.tsearch.search.filter.newfilters.name.NameFilterFactory;
import ro.cst.tsearch.search.iterator.ModuleStatesIteratorFactory;
import ro.cst.tsearch.search.iterator.data.LegalStruct;
import ro.cst.tsearch.search.iterator.data.LegalStructPI;
import ro.cst.tsearch.search.iterator.instrument.InstrumentGenericIterator;
import ro.cst.tsearch.search.iterator.legal.LegalDescriptionIteratorI;
import ro.cst.tsearch.search.module.ConfigurableNameIterator;
import ro.cst.tsearch.search.util.AdditionalInfoKeys;
import ro.cst.tsearch.searchsites.client.GWTDataSite;
import ro.cst.tsearch.servers.HashCountyToIndex;
import ro.cst.tsearch.servers.SearchDataWrapper;
import ro.cst.tsearch.servers.TSConnectionURL;
import ro.cst.tsearch.servers.bean.DataSite;
import ro.cst.tsearch.servers.functions.CAParsingPI;
import ro.cst.tsearch.servers.info.TSServerInfo;
import ro.cst.tsearch.servers.info.TSServerInfoConstants;
import ro.cst.tsearch.servers.info.TSServerInfoModule;
import ro.cst.tsearch.servers.info.spitHTML.HTMLControl;
import ro.cst.tsearch.servers.info.spitHTML.PageZone;
import ro.cst.tsearch.servers.parentsite.ModuleWrapperManager;
import ro.cst.tsearch.servers.response.LinkInPage;
import ro.cst.tsearch.servers.response.OtherInformationSet.OtherInformationSetKey;
import ro.cst.tsearch.servers.response.ParsedResponse;
import ro.cst.tsearch.servers.response.PropertyIdentificationSet.PropertyIdentificationSetKey;
import ro.cst.tsearch.servers.response.SaleDataSet.SaleDataSetKey;
import ro.cst.tsearch.servers.response.ServerResponse;
import ro.cst.tsearch.templates.MultilineElementsMap;
import ro.cst.tsearch.titledocument.abstracts.DocumentTypes;
import ro.cst.tsearch.utils.FileUtils;
import ro.cst.tsearch.utils.InstanceManager;
import ro.cst.tsearch.utils.SearchLogger;
import ro.cst.tsearch.utils.XmlUtils;

import com.stewart.ats.base.document.DocumentI;
import com.stewart.ats.base.document.DocumentUtils;
import com.stewart.ats.base.document.ImageI;
import com.stewart.ats.base.document.InstrumentI;
import com.stewart.ats.base.document.RegisterDocumentI;
import com.stewart.ats.base.document.RestoreDocumentDataI;
import com.stewart.ats.base.document.TransferI;
import com.stewart.ats.base.legal.LegalI;
import com.stewart.ats.base.legal.SubdivisionDetailed;
import com.stewart.ats.base.legal.SubdivisionDetailedI;
import com.stewart.ats.base.legal.SubdivisionI;
import com.stewart.ats.base.legal.TownShipI;
import com.stewart.ats.base.name.NameI;
import com.stewart.ats.base.name.NameSourceType;
import com.stewart.ats.base.parties.PartyI;
import com.stewart.ats.base.property.Property;
import com.stewart.ats.base.property.PropertyI;
import com.stewart.ats.base.search.DocumentsManagerI;
import com.stewart.ats.connection.titlepoint.PropertyInsightImageConn;
import com.stewart.ats.connection.titlepoint.PropertyInsightImageConn.ImageDownloadResponse;
import com.stewart.ats.tsrindex.client.SimpleChapterUtils;
import com.stewart.ats.tsrindex.client.SimpleChapterUtils.PType;

public class CAGenericPI extends GenericPI{

	private static final long serialVersionUID = 8863687363582028102L;
	
	public static final String ARB_TYPE = "arb";
	public static final String SECTIONAL_TYPE = "sectional";
	public static final String SUBDIVIDED_TYPE = "subdivided";

	public CAGenericPI(String rsRequestSolverName, String rsSitePath, String rsServerID, String rsPrmNameLink, long searchId, int mid) {
		super(rsRequestSolverName, rsSitePath, rsServerID, rsPrmNameLink, searchId, mid);
	}

	public CAGenericPI(long searchId) {
		super(searchId);
	}
	
	@Override
	public TSServerInfo getDefaultServerInfo() {
		TSServerInfo msiServerInfoDefault = super.getDefaultServerInfo();
		
		ModuleWrapperManager moduleWrapperManager = ModuleWrapperManager.getInstance();
		DataSite dataSite = HashCountyToIndex.getDateSiteForMIServerID(getCommunityId(), miServerID);
		String siteName = StateCountyManager.getInstance().getSTCounty(dataSite.getCountyId()) + dataSite.getSiteType();
		
		TSServerInfoModule tsServerInfoModule = msiServerInfoDefault.getModule(TSServerInfo.PARCEL_ID_MODULE_IDX);
		if (tsServerInfoModule != null){
			HashMap<String, Integer> nameToIndex = new HashMap<String, Integer>();
			for (int i = 0; i < tsServerInfoModule.getFunctionCount(); i++) {
				nameToIndex.put(tsServerInfoModule.getFunction(i).getName(), i);					
			}
			PageZone pageZone = (PageZone) tsServerInfoModule.getModuleParentSiteLayout();
			for (HTMLControl htmlControl : pageZone.getHtmlControls()) {
				String functionName = htmlControl.getCurrentTSSiFunc().getName();
				if (StringUtils.isNotEmpty(functionName)){
					String comment = moduleWrapperManager.getCommentForSiteAndFunction(siteName, TSServerInfo.PARCEL_ID_MODULE_IDX, nameToIndex.get(functionName));
					if (comment != null) {
						htmlControl.setFieldNote(comment);
					}
				}
			}
		}
		
		tsServerInfoModule = msiServerInfoDefault.getModule(TSServerInfo.ADDRESS_MODULE_IDX);
		if (tsServerInfoModule != null){
				
			HashMap<String, Integer> nameToIndex = new HashMap<String, Integer>();
			for (int i = 0; i < tsServerInfoModule.getFunctionCount(); i++) {
				nameToIndex.put(tsServerInfoModule.getFunction(i).getName(), i);					
			}
			PageZone pageZone = (PageZone) tsServerInfoModule.getModuleParentSiteLayout();
			for (HTMLControl htmlControl : pageZone.getHtmlControls()) {
				String functionName = htmlControl.getCurrentTSSiFunc().getName();
				if (StringUtils.isNotEmpty(functionName)) {
					String comment = moduleWrapperManager.getCommentForSiteAndFunction(siteName, TSServerInfo.ADDRESS_MODULE_IDX, nameToIndex.get(functionName));
					if (comment != null) {
						htmlControl.setFieldNote(comment);
					}
				}
			}
		}
		
		tsServerInfoModule = msiServerInfoDefault.getModule(TSServerInfo.BOOK_AND_PAGE_MODULE_IDX);
		if (tsServerInfoModule != null) {

			HashMap<String, Integer> nameToIndex = new HashMap<String, Integer>();
			for (int i = 0; i < tsServerInfoModule.getFunctionCount(); i++) {
				nameToIndex.put(tsServerInfoModule.getFunction(i).getName(), i);
			}
			PageZone pageZone = (PageZone) tsServerInfoModule.getModuleParentSiteLayout();
			for (HTMLControl htmlControl : pageZone.getHtmlControls()) {
				String functionName = htmlControl.getCurrentTSSiFunc().getName();
				if (StringUtils.isNotEmpty(functionName)) {
					String comment = moduleWrapperManager.getCommentForSiteAndFunction(siteName, TSServerInfo.BOOK_AND_PAGE_MODULE_IDX, nameToIndex.get(functionName));
					if (comment != null) {
						htmlControl.setFieldNote(comment);
					}
				}
			}
		}
		
		tsServerInfoModule = msiServerInfoDefault.getModule(TSServerInfo.INSTR_NO_MODULE_IDX);
		if (tsServerInfoModule != null){
			HashMap<String, Integer> nameToIndex = new HashMap<String, Integer>();
			for (int i = 0; i < tsServerInfoModule.getFunctionCount(); i++) {
				nameToIndex.put(tsServerInfoModule.getFunction(i).getName(), i);
			}
			PageZone pageZone = (PageZone) tsServerInfoModule.getModuleParentSiteLayout();
			for (HTMLControl htmlControl : pageZone.getHtmlControls()) {
				String functionName = htmlControl.getCurrentTSSiFunc().getName();
				if (StringUtils.isNotEmpty(functionName)) {
					String comment = moduleWrapperManager.getCommentForSiteAndFunction(siteName, TSServerInfo.INSTR_NO_MODULE_IDX, nameToIndex.get(functionName));
					if (comment != null) {
						htmlControl.setFieldNote(comment);
					}
				}
			}
		}
		
		tsServerInfoModule = msiServerInfoDefault.getModule(TSServerInfo.SUBDIVISION_MODULE_IDX);
		if (tsServerInfoModule != null){
				
			HashMap<String, Integer> nameToIndex = new HashMap<String, Integer>();
			for (int i = 0; i < tsServerInfoModule.getFunctionCount(); i++) {
				nameToIndex.put(tsServerInfoModule.getFunction(i).getName(), i);					
			}
			PageZone pageZone = (PageZone) tsServerInfoModule.getModuleParentSiteLayout();
			for (HTMLControl htmlControl : pageZone.getHtmlControls()) {
				String functionName = htmlControl.getCurrentTSSiFunc().getName();
				if (StringUtils.isNotEmpty(functionName)) {
					String comment = moduleWrapperManager.getCommentForSiteAndFunction(siteName, TSServerInfo.SUBDIVISION_MODULE_IDX, nameToIndex.get(functionName));
					if (comment != null) {
						htmlControl.setFieldNote(comment);
					}
					if (functionName.contains("Map Code")){
						LinkedList<ComboValue> comboList = htmlControl.getComboList();
						if (comboList.isEmpty()){
							String comboString = moduleWrapperManager.getComboValuesForSiteAndFunction(siteName, TSServerInfo.SUBDIVISION_MODULE_IDX, nameToIndex.get(functionName));
							comboList = HtmlControlMap.createComboValueSet(comboString);
							if (!comboList.isEmpty()){
								htmlControl.setComboList(comboList);
								String defaultVal = (String) getSearch().getAdditionalInfo(SearchAttributes.LD_PI_MAP_CODE);
								if (StringUtils.isNotEmpty(defaultVal)){
									htmlControl.setDefaultValue(defaultVal);
								}
							}
						}
					}
					if (functionName.contains("Major Legal Name")){
						LinkedList<ComboValue> comboList = htmlControl.getComboList();
						if (comboList.isEmpty()){
							String comboString = moduleWrapperManager.getComboValuesForSiteAndFunction(siteName, TSServerInfo.SUBDIVISION_MODULE_IDX, nameToIndex.get(functionName));
							comboList = HtmlControlMap.createComboValueSet(comboString);
							if (!comboList.isEmpty()){
								htmlControl.setComboList(comboList);
								String defaultVal = (String) getSearch().getAdditionalInfo(SearchAttributes.LD_PI_MAJ_LEGAL_NAME);
								if (StringUtils.isNotEmpty(defaultVal)){
									htmlControl.setDefaultValue(defaultVal);
								}
							}
						}
					}
					if (functionName.contains("Help")){
						String htmlString = moduleWrapperManager.getHtmlStringForSiteAndFunction(siteName, TSServerInfo.SUBDIVISION_MODULE_IDX, nameToIndex.get(functionName));
						if (htmlString != null) {
							htmlControl.getCurrentTSSiFunc().setHtmlformat(StringEscapeUtils.unescapeHtml(htmlString));
						}
					}
				}
			}
		}
		
		tsServerInfoModule = msiServerInfoDefault.getModule(TSServerInfo.SUBDIVISION_PLAT_MODULE_IDX);
		if (tsServerInfoModule != null){
				
			HashMap<String, Integer> nameToIndex = new HashMap<String, Integer>();
			for (int i = 0; i < tsServerInfoModule.getFunctionCount(); i++) {
				nameToIndex.put(tsServerInfoModule.getFunction(i).getName(), i);					
			}
			PageZone pageZone = (PageZone) tsServerInfoModule.getModuleParentSiteLayout();
			for (HTMLControl htmlControl : pageZone.getHtmlControls()) {
				String functionName = htmlControl.getCurrentTSSiFunc().getName();
				if (StringUtils.isNotEmpty(functionName)) {
					String comment = moduleWrapperManager.getCommentForSiteAndFunction(siteName, TSServerInfo.SUBDIVISION_PLAT_MODULE_IDX, nameToIndex.get(functionName));
					if (comment != null) {
						htmlControl.setFieldNote(comment);
					}
				}
			}
		}
		
		tsServerInfoModule = msiServerInfoDefault.getModule(TSServerInfo.ARCHIVE_DOCS_MODULE_IDX);
		if (tsServerInfoModule != null){
				
			HashMap<String, Integer> nameToIndex = new HashMap<String, Integer>();
			for (int i = 0; i < tsServerInfoModule.getFunctionCount(); i++) {
				nameToIndex.put(tsServerInfoModule.getFunction(i).getName(), i);					
			}
			PageZone pageZone = (PageZone) tsServerInfoModule.getModuleParentSiteLayout();
			for (HTMLControl htmlControl : pageZone.getHtmlControls()) {
				String functionName = htmlControl.getCurrentTSSiFunc().getName();
				if (StringUtils.isNotEmpty(functionName)) {
					String comment = moduleWrapperManager.getCommentForSiteAndFunction(siteName, TSServerInfo.ARCHIVE_DOCS_MODULE_IDX, nameToIndex.get(functionName));
					if (comment != null) {
						htmlControl.setFieldNote(comment);
					}
				}
			}
		}
		
		return msiServerInfoDefault;
	}
	
	protected void setModulesForAutoSearch(TSServerInfo serverInfo) {
		List<TSServerInfoModule> modules = new ArrayList<TSServerInfoModule>();
		Search global = getSearch();
		int searchType = global.getSearchType();
		
		if (searchType == Search.AUTOMATIC_SEARCH) {
			 
			FilterResponse nameFilterOwner 	= NameFilterFactory.getDefaultNameFilter(SearchAttributes.OWNER_OBJECT, global.getID(), null);
			nameFilterOwner.setInitAgain(true);
			FilterResponse legalFilter 		= LegalFilterFactory.getDefaultLegalFilter(searchId);
			FilterResponse addressFilter 	= AddressFilterFactory.getAddressHighPassFilter(searchId, 0.8d);
			
			GenericMultipleLegalFilter multipleLegalFilter = new GenericMultipleLegalFilter(searchId);
			multipleLegalFilter.setUseLegalFromSearchPage(true);
			multipleLegalFilter.setMarkIfCandidatesAreEmpty(true);
			multipleLegalFilter.setThreshold(new BigDecimal(0.7));
			multipleLegalFilter.setAdditionalInfoKey(AdditionalInfoKeys.PI_LOOK_UP_DATA);
			
			LastTransferDateFilter ltdf = new LastTransferDateFilter(searchId);
			ltdf.setUseDefaultDocTypeThatPassForGoodName(true);
			
			FilterResponse rejectSavedDocuments = new RejectAlreadySavedDocumentsFilterResponse(searchId);
			
			FilterResponse[] filtersO 	= { nameFilterOwner, ltdf};
			FilterResponse[] filters 	= { multipleLegalFilter };
			FilterResponse[] filtersRef = { multipleLegalFilter };
			
			//Ao/Tax references search
			{
				InstrumentGenericIterator instrumentGenericIterator = getInstrumentIterator(true);
				instrumentGenericIterator.setDoNotCheckIfItExists(true);

				TSServerInfoModule module = new TSServerInfoModule(serverInfo.getModule(TSServerInfo.INSTR_NO_MODULE_IDX));
				module.addExtraInformation(TSServerInfoConstants.EXTRA_PARAM_MODULE_DESCRIPTION, TSServerInfoConstants.VALUE_PARAM_LIST_AO_NDB_TR_INSTR);
				module.setIteratorType(0, FunctionStatesIterator.ITERATOR_TYPE_INSTRUMENT_LIST_FAKE);
				module.setIteratorType(1, FunctionStatesIterator.ITERATOR_TYPE_YEAR);
				if (isUpdate()) {
					module.addFilter(new BetweenDatesFilterResponse(searchId));
				}
				module.addIterator(instrumentGenericIterator);
				module.addCrossRefValidator(addressFilter.getValidator());
				module.addCrossRefValidator(legalFilter.getValidator());
				modules.add(module);
			}
			{
				InstrumentGenericIterator bpGenericIterator = getInstrumentIterator(false);
				bpGenericIterator.setRemoveLeadingZerosBP(true);
				bpGenericIterator.setDoNotCheckIfItExists(true);

				TSServerInfoModule module = new TSServerInfoModule(serverInfo.getModule(TSServerInfo.BOOK_AND_PAGE_MODULE_IDX));
				module.addExtraInformation(TSServerInfoConstants.EXTRA_PARAM_MODULE_DESCRIPTION, TSServerInfoConstants.VALUE_PARAM_LIST_AO_NDB_TR_BP);
				module.setIteratorType(0, FunctionStatesIterator.ITERATOR_TYPE_BOOK_FAKE);
				module.setIteratorType(1, FunctionStatesIterator.ITERATOR_TYPE_PAGE_FAKE);

				if (isUpdate()) {
					module.addFilter(new BetweenDatesFilterResponse(searchId));
				}
				module.addIterator(bpGenericIterator);
				module.addCrossRefValidator(addressFilter.getValidator());
				module.addCrossRefValidator(legalFilter.getValidator());
				
				modules.add(module);
			}
			//Address Search
			{
				TSServerInfoModule module = new TSServerInfoModule(serverInfo.getModule(TSServerInfo.ADDRESS_MODULE_IDX));
				module.addExtraInformation(TSServerInfoConstants.EXTRA_PARAM_MODULE_DESCRIPTION, "Search with address");
	
				if (isUpdate()) {
					module.addFilter(new BetweenDatesFilterResponse(searchId));
				}
				module.addValidator(addressFilter.getValidator());
				module.addCrossRefValidator(addressFilter.getValidator());
				module.addCrossRefValidator(legalFilter.getValidator());
				
				modules.add(module);
			}
			//Legal Search
			addIteratorModule(serverInfo, modules, TSServerInfo.SUBDIVISION_MODULE_IDX, searchId, isUpdate());
			
			// OCR last transfer - Instrument search
			addOCRSearch(modules, serverInfo, filters);

			//Name Search + OCR last transfer - Name search
			addNameSearch(modules, serverInfo, SearchAttributes.OWNER_OBJECT, null, filtersO);
			
			//Buyer Search
			{
				GenericNameFilter nameFilterBuyer = (GenericNameFilter) NameFilterFactory.getDefaultNameFilter(SearchAttributes.BUYER_OBJECT, global.getID(), null);
				nameFilterBuyer.setIgnoreMiddleOnEmpty(true);
				nameFilterBuyer.setUseSynonymsBothWays(true);
				nameFilterBuyer.setInitAgain(true);
	
				FilterResponse[] filtersB = { nameFilterBuyer, new DocTypeAdvancedFilter(searchId) };
				addNameSearch(modules, serverInfo, SearchAttributes.BUYER_OBJECT, null, filtersB);
			}
			
			// search by crossRef list from RO documents
			{
				TSServerInfoModule module = new TSServerInfoModule(serverInfo.getModule(TSServerInfo.INSTR_NO_MODULE_IDX));
				module.addExtraInformation(TSServerInfoConstants.EXTRA_PARAM_MODULE_DESCRIPTION, TSServerInfoConstants.VALUE_PARAM_LIST_RO_CROSSREF_INSTR);
				module.addFilter(rejectSavedDocuments);
	
				InstrumentGenericIterator instrumentNoInterator = getInstrumentIterator(true);
				instrumentNoInterator.setLoadFromRoLike(true);
				module.addIterator(instrumentNoInterator);
				
				module.setIteratorType(0, FunctionStatesIterator.ITERATOR_TYPE_INSTRUMENT_LIST_FAKE);
				module.setIteratorType(1, FunctionStatesIterator.ITERATOR_TYPE_YEAR);
				module.addFilter(new GenericInstrumentFilter(searchId));
				for (int i = 0; i < filtersRef.length; i++){
			    	module.addFilter(filtersRef[i]);
			    }
				module.addCrossRefValidator(addressFilter.getValidator());
				module.addCrossRefValidator(multipleLegalFilter.getValidator());
				modules.add(module);	
			}
			{
				TSServerInfoModule module = new TSServerInfoModule(serverInfo.getModule(TSServerInfo.BOOK_AND_PAGE_MODULE_IDX));
				module.addExtraInformation(TSServerInfoConstants.EXTRA_PARAM_MODULE_DESCRIPTION, TSServerInfoConstants.VALUE_PARAM_LIST_RO_CROSSREF_BP);
				module.addFilter(rejectSavedDocuments);
	
				InstrumentGenericIterator bpGenericIterator = getInstrumentIterator(false);
				bpGenericIterator.setLoadFromRoLike(true);
				module.addIterator(bpGenericIterator);
				
				module.setIteratorType(0, FunctionStatesIterator.ITERATOR_TYPE_BOOK_FAKE);
				module.setIteratorType(1, FunctionStatesIterator.ITERATOR_TYPE_PAGE_FAKE);
				for (int i = 0; i < filtersRef.length; i++){
			    	module.addFilter(filtersRef[i]);
			    }
				module.addCrossRefValidator(addressFilter.getValidator());
				module.addCrossRefValidator(multipleLegalFilter.getValidator());
				modules.add(module);	
			}
		}
		serverInfo.setModulesForAutoSearch(modules);	
   }

	protected void addOCRSearch(List<TSServerInfoModule> modules,TSServerInfo serverInfo, FilterResponse ...filters){
		// OCR last transfer - book / page search
		TSServerInfoModule module = new TSServerInfoModule(serverInfo.getModule(TSServerInfo.BOOK_AND_PAGE_MODULE_IDX));
		module.addExtraInformation(TSServerInfoConstants.EXTRA_PARAM_MODULE_DESCRIPTION, TSServerInfoConstants.VALUE_PARAM_OCR_SEARCH_BP);
	    module.setIteratorType(ModuleStatesIterator.TYPE_OCR_FULL_OR_BOOTSTRAPER);
	    module.clearSaKeys();
	    module.getFunction(0).setIteratorType(FunctionStatesIterator.ITERATOR_TYPE_BOOK_SEARCH);
	    module.getFunction(1).setIteratorType(FunctionStatesIterator.ITERATOR_TYPE_PAGE_SEARCH);
	    for (int i = 0; i < filters.length; i++){
	    	module.addFilter(filters[i]);
	    }
	    addBetweenDateTest(module, false, false, false);
		modules.add(module);
		
	    // OCR last transfer - instrument search
	    module = new TSServerInfoModule(serverInfo.getModule(TSServerInfo.INSTR_NO_MODULE_IDX));
	    module.addExtraInformation(TSServerInfoConstants.EXTRA_PARAM_MODULE_DESCRIPTION, TSServerInfoConstants.VALUE_PARAM_OCR_SEARCH_INST);
	    module.setIteratorType(ModuleStatesIterator.TYPE_OCR_FULL_OR_BOOTSTRAPER);
	    module.clearSaKeys();
	    module.getFunction(0).setIteratorType(FunctionStatesIterator.ITERATOR_TYPE_INSTRUMENT_LIST_SEARCH);
	    module.getFunction(1).setIteratorType(FunctionStatesIterator.ITERATOR_TYPE_YEAR);
	    for (int i = 0; i < filters.length; i++){
	    	module.addFilter(filters[i]);
	    }
	    addBetweenDateTest(module, false, false, false);
		modules.add(module);
	}
	
	public InstrumentGenericIterator getInstrumentIterator(boolean instrumentType) {
		InstrumentGenericIterator instrumentGenericIterator = new InstrumentGenericIterator(searchId, dataSite) {

			private static final long serialVersionUID = 5399351945130601258L;

			@Override
			public String getYearFrom(InstrumentI state, HashMap<String, String> filterCriteria) {
				if(state.getYear() != SimpleChapterUtils.UNDEFINED_YEAR) {
					return Integer.toString(state.getYear());
				} else {
					String instNo = state.getInstno();
					if (StringUtils.isNotEmpty(instNo)){
						if (instNo.contains("-")){
							return instNo.substring(0, 4);
						}
					}
					return "";
				}
			}

			@Override
			public String getInstrumentNoFrom(InstrumentI state, HashMap<String, String> filterCriteria) {
				String instNo = state.getInstno();
				if (StringUtils.isNotEmpty(instNo)){
					if (instNo.contains("-")){
						return instNo.substring(5).replaceFirst("^0+", "").trim();
					}
				}
				return state.getInstno().replaceFirst("^0+", "").trim();
			}
			
			@Override
			public String getBookFrom(InstrumentI state, HashMap<String, String> filterCriteria) {
				if (getDataSite().getCountyId() == CountyConstants.CA_Sacramento){
					int year = state.getYear();
					String book = state.getBook().trim();
					if (year != SimpleChapterUtils.UNDEFINED_YEAR){
						String yr = "";
						try {
							yr = Integer.toString(year);
						} catch (Exception e) {
						}
						if (StringUtils.isNotBlank(yr) && yr.length() == 4){
							String shortYear = StringUtils.stripStart(yr.substring(2), "0");
							if (book.startsWith(shortYear)){
								if (shortYear.length() == 1){
									book = yr + book.substring(1);
								} else if (shortYear.length() == 2){
									book = yr + book.substring(2);
								}
							}
						}
					}
					if (filterCriteria != null) {
						filterCriteria.put("Book", book);
					}
					return book;
				} else{
					return state.getBook().trim();
				}
			}
			
			@Override
			protected void processEnableInstrumentNo(List<InstrumentI> result,
					HashSet<String> listsForNow, DocumentsManagerI manager, InstrumentI instrumentI) {
				String instrumentNo = cleanInstrumentNo(instrumentI);
				
				if (org.apache.commons.lang.StringUtils.isBlank(instrumentNo)) {
					return;
				}
				
				if (instrumentI.getYear() == SimpleChapterUtils.UNDEFINED_YEAR && instrumentI.getDate() == null) {
					return;
				}
				
				super.processEnableInstrumentNo(result, listsForNow, manager, instrumentI);
			}
		};
		if (instrumentType) {
			instrumentGenericIterator.enableInstrumentNumber();
		} else {
			instrumentGenericIterator.enableBookPage();
			instrumentGenericIterator.setRemoveLeadingZerosBP(true);
		}
		return instrumentGenericIterator;
	}
	
	private LegalDescriptionIterator getLegalDescriptionIterator(boolean lookupWasDoneWithName) {
		LegalDescriptionIterator it = new LegalDescriptionIterator(searchId);
		
		it.setLoadFromSearchPage(false);
		it.setLoadFromSearchPageIfNoLookup(true);
		return it;
	}
		
	class LegalDescriptionIterator extends GenericRuntimeIterator<LegalStructPI> implements LegalDescriptionIteratorI{

			public LegalDescriptionIterator(long searchId) {
				super(searchId);
				setDataSite(CAGenericPI.this.getDataSite());
			}

			private static final long serialVersionUID = -4741635379234782109L;

			private boolean loadFromSearchPage = true;
			private boolean loadFromSearchPageIfNoLookup = false;
			
			@Override
			public boolean isTransferAllowed(RegisterDocumentI doc) {
				return doc != null && doc.isOneOf(DocumentTypes.TRANSFER);
			}
			
			protected List<LegalStructPI> createDerrivations() {

				Search global = InstanceManager.getManager().getCurrentInstance(searchId).getCrtSearchContext();
				DocumentsManagerI m = global.getDocManager();
				@SuppressWarnings("unchecked")
				Set<LegalStructPI> legalStructList = (HashSet<LegalStructPI>) global.getAdditionalInfo(AdditionalInfoKeys.PI_LOOK_UP_DATA);
				@SuppressWarnings("unchecked")
				Set<String> multipleLegals = (Set<String>) global.getAdditionalInfo("CA_PI_MULTIPLE_LEGAL_INSTR");
				if (multipleLegals == null) {
					multipleLegals = new HashSet<String>();
				}

//				String spLot = global.getSa().getAtribute(SearchAttributes.LD_LOTNO);
//				HashSet<String> allSPLots = new HashSet<String>();
//				String spBlock = global.getSa().getAtribute(SearchAttributes.LD_SUBDIV_BLOCK);
//				HashSet<String> allSPBlocks = new HashSet<String>();
//				
//				if (spLot.contains(",") || spLot.contains(" ") || spLot.contains("-") || spLot.contains(";")) {
//					if (!StringUtils.isEmpty(spLot)) {
//						for (LotInterval interval : LotMatchAlgorithm.prepareLotInterval(spLot)) {
//							allSPLots.addAll(interval.getLotList());
//						}
//					}
//				} else {
//					if (!StringUtils.isEmpty(spLot)) {
//						allSPLots.add(spLot);
//					}
//				}
//				if (!StringUtils.isEmpty(spBlock)) {
//					for (LotInterval interval : LotMatchAlgorithm.prepareLotInterval(spBlock)) {
//						allSPBlocks.addAll(interval.getLotList());
//					}
//				}
				
				String week = global.getSa().getAtribute(SearchAttributes.WEEK);
				boolean isTimeShare = !StringUtils.isEmpty(week);

				boolean first = false;
				if (legalStructList == null) {
					first = true;
					legalStructList = new HashSet<LegalStructPI>();
					try {
						m.getAccess();
						List<RegisterDocumentI> listRodocs = getGoodDocumentsOrForCurrentOwner(m, global, true, GWTDataSite.PI_TYPE, "CA");

						if (listRodocs == null || listRodocs.size() == 0) {
							listRodocs = getGoodDocumentsOrForCurrentOwner(m, global, false, GWTDataSite.PI_TYPE, "CA");
						}
						if (listRodocs == null || listRodocs.size() == 0) {
							for (DocumentI doc : m.getDocumentsWithDataSource(true, "PI")) {
								if (doc instanceof RegisterDocumentI) {
									listRodocs.add((RegisterDocumentI) doc);
								}
							}
						}
						DocumentUtils.sortDocuments(listRodocs, MultilineElementsMap.DATE_ORDER_DESC);

						for (RegisterDocumentI reg : listRodocs) {
							if (!reg.isOneOf(DocumentTypes.PLAT, DocumentTypes.RESTRICTION, DocumentTypes.EASEMENT, DocumentTypes.MASTERDEED,
									DocumentTypes.COURT, DocumentTypes.LIEN, DocumentTypes.CORPORATION, DocumentTypes.AFFIDAVIT, DocumentTypes.CCER)) {

								List<LegalStructPI> tempLegalStructListPerDocument = new ArrayList<LegalStructPI>();
								for (PropertyI prop : reg.getProperties()) {
									if (prop.hasLegal()) {
										LegalI legal = prop.getLegal();
										String mapCode = "", mapLegalName = "";
										
										SubdivisionI subdiv = legal.getSubdivision();
										if (subdiv != null && subdiv instanceof SubdivisionDetailedI) {
											SubdivisionDetailedI subdivisionDetailedI = (SubdivisionDetailedI)subdiv;
											mapCode = org.apache.commons.lang.StringUtils.defaultString(subdivisionDetailedI.getPlatBookType());
											mapLegalName = org.apache.commons.lang.StringUtils.defaultString(subdivisionDetailedI.getPlatInstrumentType());
										} else{
											continue;
										}
										
										if (legal.hasSubdividedLegal()) {

											LegalStructPI legalStructItem = new LegalStructPI(SUBDIVIDED_TYPE);
											
											String block = org.apache.commons.lang.StringUtils.defaultString(subdiv.getBlock());
											String lot = org.apache.commons.lang.StringUtils.defaultString(subdiv.getLot());
											String subLot = org.apache.commons.lang.StringUtils.defaultString(subdiv.getSubLot());
											
											String platBook = org.apache.commons.lang.StringUtils.defaultString(subdiv.getPlatBook());
											platBook = platBook.replaceFirst("(?is)\\A[A-Z](\\d+)", "$1");
											
											String platPage = org.apache.commons.lang.StringUtils.defaultString(subdiv.getPlatPage());
											
											String platInstr = org.apache.commons.lang.StringUtils.defaultString(subdiv.getPlatInstrument());
											platInstr = platInstr.replaceFirst("(?is)\\A[A-Z](\\d+)", "$1");
											
											String unit = org.apache.commons.lang.StringUtils.defaultString(subdiv.getUnit());
											
											legalStructItem.setLot(lot);
											legalStructItem.setUnit(unit);
											legalStructItem.setCommonLot(subLot);
											legalStructItem.setBlock(block);
											legalStructItem.setMapBook(platBook);
											legalStructItem.setMapPage(platPage);
											legalStructItem.setPlatInst(platInstr);
											legalStructItem.setTract(org.apache.commons.lang.StringUtils.defaultString(subdiv.getTract()));
											
											legalStructItem.setMapCode(mapCode);
											legalStructItem.setMapLegalName(mapLegalName);
											
											if (legal.hasTownshipLegal()) {											
												TownShipI township = legal.getTownShip();
												
												if (StringUtils.isNotEmpty(township.getArb())){
													String arb = org.apache.commons.lang.StringUtils.defaultString(township.getArb());
													legalStructItem.setType(ARB_TYPE);
													legalStructItem.setArbTract(arb);
													legalStructItem.setMapBook("");
													legalStructItem.setMapPage("");
													legalStructItem.setParcel(org.apache.commons.lang.StringUtils.defaultString(township.getParcel()));
												}
											}
											
											multipleLegals.add(reg.prettyPrint());
											tempLegalStructListPerDocument.add(legalStructItem);
										}
										if (tempLegalStructListPerDocument.size() == 0){
											if (legal.hasTownshipLegal()) {											
												TownShipI township = legal.getTownShip();
												
												if (StringUtils.isNotEmpty(township.getSection())){
													LegalStructPI legalStructItem = new LegalStructPI(SECTIONAL_TYPE);
													String sec = org.apache.commons.lang.StringUtils.defaultString(township.getSection());
													String tw = org.apache.commons.lang.StringUtils.defaultString(township.getTownship());
													String rg = org.apache.commons.lang.StringUtils.defaultString(township.getRange());
													
													legalStructItem.setSection(sec);
													legalStructItem.setTownship(tw);
													legalStructItem.setRange(rg);
													
													legalStructItem.setMapCode(mapCode);
													legalStructItem.setMapLegalName(mapLegalName);
													
	//												int qo = township.getQuarterOrder();
	//												String qv = township.getQuarterValue();
	//												legalStructItem.setQuarterValue(org.apache.commons.lang.StringUtils.defaultString(qv));
	//												legalStructItem.setQuarterOrder(String.valueOf(qo <= 0 ? "" : qo));
	
													multipleLegals.add(reg.prettyPrint());
													tempLegalStructListPerDocument.add(legalStructItem);
												} else if (StringUtils.isNotEmpty(township.getArb())){
													LegalStructPI legalStructItem = new LegalStructPI(ARB_TYPE);
													String arb = org.apache.commons.lang.StringUtils.defaultString(township.getArb());
													legalStructItem.setArbTract(arb);
													legalStructItem.setParcel(org.apache.commons.lang.StringUtils.defaultString(township.getParcel()));
													legalStructItem.setMapCode(mapCode);
													legalStructItem.setMapLegalName(mapLegalName);
													
													multipleLegals.add(reg.prettyPrint());
													tempLegalStructListPerDocument.add(legalStructItem);
												}
											}
										}
									}
								}
								for (LegalStructPI item : tempLegalStructListPerDocument) {
									if (!testIfExist(legalStructList, item, searchId)) {
										legalStructList.add(item);
									}
								}
							}
						}

						legalStructList = keepOnlyGoodLegal(legalStructList);

						global.setAdditionalInfo(AdditionalInfoKeys.PI_LOOK_UP_DATA, legalStructList);

						if (isTimeShare && legalStructList.size() > 0) {
							List<DocumentI> docList = m.getDocumentsWithDataSource(false, "PI");
							List<String> docIds = new ArrayList<String>();
							for (DocumentI doc : docList) {
								docIds.add(doc.getId());
							}
							m.remove(docIds);
						}
					} finally {
						m.releaseAccess();
					}
				}
				if (legalStructList.size() > 1
								) {
					
					global.getSa().setAtribute(SearchAttributes.ATS_MULTIPLE_LEGALS_FOUND, "true");
					global.getSa().setAtribute(SearchAttributes.ATS_MULTIPLE_LEGAL_INSTRUMENTS, multipleLegals.toString());
					global.setAdditionalInfo("CA_PI_MULTIPLE_LEGAL_INSTR", multipleLegals);
					if (first){
						SearchLogger.info("<br/><b>Questionable multiple legals found in " + multipleLegals.toString() + "</b>", searchId);
					}
				}

				return new ArrayList<LegalStructPI>(legalStructList);
			}
			
			protected void loadDerrivation(TSServerInfoModule module, LegalStructPI str){
				
				switch(module.getModuleIdx()){
					case TSServerInfo.SUBDIVISION_MODULE_IDX:
						if(org.apache.commons.lang.StringUtils.isNotBlank(str.getArbTract())){
							module.setData(18, str.getArbTract());
						} else if (StringUtils.isNotEmpty(str.getMapBook()) && StringUtils.isNotEmpty(str.getMapPage())){
							module.setData(1, str.getMapBook());
							module.setData(2, str.getMapPage());
						} else if (StringUtils.isNotEmpty(str.getPlatInst())){
							module.setData(25, str.getPlatInst());
						}
						if (StringUtils.isNotEmpty(str.getLot())){
							module.setData(0, str.getLot());
						}
						if (StringUtils.isNotEmpty(str.getCommonLot())){
							module.setData(5, str.getCommonLot());
						}
						if (StringUtils.isNotEmpty(str.getUnit())){
							module.setData(6, str.getUnit());
						}
						if (StringUtils.isNotEmpty(str.getBlock())){
							module.setData(7, str.getBlock());
						}
						if (StringUtils.isNotEmpty(str.getParcel())){
							module.setData(11, str.getParcel());
						}
							
						if (StringUtils.isNotEmpty(str.getTract())){
							module.forceValue(3, str.getTract());
						}
						module.setData(13, str.getMapCode());
						module.setData(14, str.getMapLegalName());
					break;
				}
			}
			
			public boolean isLoadFromSearchPage() {
				return loadFromSearchPage;
			}
			
			/**
			 * Load information from search page not just from lookup<br>
			 * Default is to load
			 * @param loadFromSearchPage
			 */
			public void setLoadFromSearchPage(boolean loadFromSearchPage) {
				this.loadFromSearchPage = loadFromSearchPage;
			}
			
			public boolean isLoadFromSearchPageIfNoLookup() {
				return loadFromSearchPageIfNoLookup;
			}
			
			/**
			 * Load information from search page only if nothing found after lookup<br>
			 * Search page data will be considered only as backup<br><br>
			 * Default is set to <code>false</code>.
			 * Please check the other flag {LegalDescriptionIterator.isLoadFromSearchPage} to turn off default search page
			 * @return
			 */
			public void setLoadFromSearchPageIfNoLookup(boolean loadFromSearchPageIfNoLookup) {
				this.loadFromSearchPageIfNoLookup = loadFromSearchPageIfNoLookup;
			}

			@Override
			public void loadSecondaryPlattedLegal(LegalI legal, LegalStruct legalStruct) {				
			}
		}
	
	protected boolean testIfExist(Set<LegalStructPI> legalStruct2, LegalStructPI l,long searchId) {
		
		if (ARB_TYPE.equalsIgnoreCase(l.getType())){
			for (LegalStructPI p : legalStruct2){
				if (p.isArb()){
					if (l.equalsArb(p)){
						return true;
					}
				}
			}
		}else if (SECTIONAL_TYPE.equalsIgnoreCase(l.getType())){
			for (LegalStructPI p : legalStruct2){
				if (p.isSectional() || p.isArb()){
					if (l.equalsSectional(p)){
						return true;
					}
				}
			}
		}else if(SUBDIVIDED_TYPE.equalsIgnoreCase(l.getType())){
			for (LegalStructPI p : legalStruct2){
				if (p.isPlated()){
					if (l.equalsSubdivided(p)){
						return true;
					}
				}
			}
		}
		return false;
	}
	
	protected Set<LegalStructPI> keepOnlyGoodLegal(Set<LegalStructPI> legals){
		 Set<LegalStructPI> good = new HashSet<LegalStructPI>();
		 for (LegalStructPI str : legals){
			 if (!incompleteData(str)){
				 good.add(str);
			 }
		 }
		 return good;
	 }
		
	 protected boolean incompleteData(LegalStructPI str) {
		 if (str == null){
			 return true;
		 }
			
		 boolean emptyLot = StringUtils.isEmpty(str.getLot());
		 boolean emptyBlock = StringUtils.isEmpty(str.getBlock());
		 boolean emptyPlatBook = StringUtils.isEmpty(str.getMapBook());
		 boolean emptyPlatPage = StringUtils.isEmpty(str.getMapPage());
		 
		 boolean emptyPlatInstr = StringUtils.isEmpty(str.getPlatInst());
			
		 boolean emptySection = StringUtils.isEmpty(str.getSection());
		 boolean emptyRange = StringUtils.isEmpty(str.getRange());
		 boolean emptyTownship = StringUtils.isEmpty(str.getTownship());
		
		 boolean emptyArbTract = StringUtils.isEmpty(str.getArbTract());
		 
//		 boolean emptyArblot = StringUtils.isEmpty(str.getArbLot());
//		 boolean emptyArbBlock = StringUtils.isEmpty(str.getArbBlock());
//		 boolean emptyArbBook = StringUtils.isEmpty(str.getArbBook());
//		 boolean emptyArbPage = StringUtils.isEmpty(str.getArbPage());
			
		 boolean emptyNcbNumber = org.apache.commons.lang.StringUtils.isEmpty(str.getNcbNumber());
		 boolean emptySubdivisionName = org.apache.commons.lang.StringUtils.isEmpty(str.getSubdivisionName());
		 boolean emptyTract = StringUtils.isEmpty(str.getTract());
			
		 if (SECTIONAL_TYPE.equalsIgnoreCase(str.getType()) || ARB_TYPE.equalsIgnoreCase(str.getType())){
			 if (!emptySection){
				 return (emptySection || emptyTownship || emptyRange);
			}else{
				return (emptyArbTract);
			}
		} else if(SUBDIVIDED_TYPE.equalsIgnoreCase(str.getType())){
			if (!emptyNcbNumber){
				return (emptyNcbNumber && emptySubdivisionName);
			} else if (!emptyTract){
				return emptyTract;
			} else{
				return ((emptyPlatBook || emptyPlatPage) && emptyPlatInstr) || (emptyBlock && emptyLot);
			}
		}
			
		 return true;
	 }
	
	protected List<RegisterDocumentI> getGoodDocumentsOrForCurrentOwner(DocumentsManagerI m, Search search, boolean applyNameMatch, int siteType, String stateAbbrev){
		final List<RegisterDocumentI> ret = new ArrayList<RegisterDocumentI>();
		
		List<RegisterDocumentI> listRodocs = m.getRealRoLikeDocumentList();
		DocumentUtils.sortDocuments(listRodocs, MultilineElementsMap.DATE_ORDER_DESC);
		
		SearchAttributes sa	= search.getSa();
		PartyI owner 		= sa.getOwners();
		
		for (RegisterDocumentI doc : listRodocs){
			boolean found = false;
			if ((doc.isOneOf("MORTGAGE", "TRANSFER", "RELEASE") && applyNameMatch)
					|| (doc.isOneOf("MORTGAGE", "TRANSFER") && !applyNameMatch)){
				for (PropertyI prop : doc.getProperties()){
					String mapCode = "", mapLegalName = "";
					SubdivisionI sub = prop.getLegal().getSubdivision();
					if (sub != null && sub instanceof SubdivisionDetailedI) {
						SubdivisionDetailedI subdivisionDetailedI = (SubdivisionDetailedI)sub;
						mapCode = subdivisionDetailedI.getPlatBookType();
						mapLegalName = subdivisionDetailedI.getPlatInstrumentType();
					}
					
					if (prop.hasSubdividedLegal()){
						LegalStructPI legalStruct = new LegalStructPI(SUBDIVIDED_TYPE);
						legalStruct.setLot(sub.getLot());
						legalStruct.setCommonLot(sub.getSubLot());
						legalStruct.setBlock(sub.getBlock());
						legalStruct.setTract(sub.getTract());
						
						String platBook = sub.getPlatBook();
						platBook = platBook.replaceFirst("(?is)\\A[A-Z](\\d+)", "$1");
						legalStruct.setMapBook(platBook);
						legalStruct.setMapPage(sub.getPlatPage());
						
						String platInstr = sub.getPlatInstrument();
						platInstr = platInstr.replaceFirst("(?is)\\A[A-Z](\\d+)", "$1");
						legalStruct.setPlatInst(platInstr);
						
						legalStruct.setMapCode(mapCode);
						legalStruct.setMapLegalName(mapLegalName);
						if (prop.hasTownshipLegal()){
							TownShipI township = prop.getLegal().getTownShip();
							if (StringUtils.isNotEmpty(township.getArb())){
								legalStruct.setType(ARB_TYPE);
								legalStruct.setArbTract(township.getArb());
								legalStruct.setMapBook("");
								legalStruct.setMapPage("");
							}
							legalStruct.setParcel(township.getParcel());
						}
						
						boolean nameMatched = false;
						
						if (applyNameMatch){
							if (GenericNameFilter.isMatchGreaterThenScore(owner, doc.getGrantor(), NameFilterFactory.NAME_FILTER_THRESHOLD)
									|| GenericNameFilter.isMatchGreaterThenScore(doc.getGrantor(), owner, NameFilterFactory.NAME_FILTER_THRESHOLD)
									|| GenericNameFilter.isMatchGreaterThenScore(owner, doc.getGrantee(), NameFilterFactory.NAME_FILTER_THRESHOLD)
									|| GenericNameFilter.isMatchGreaterThenScore(doc.getGrantee(), owner, NameFilterFactory.NAME_FILTER_THRESHOLD)){
									nameMatched = true;
							}
						}
						
						if((nameMatched || !applyNameMatch) 
								&& (StringUtils.isNotEmpty(legalStruct.getLot()) || StringUtils.isNotEmpty(legalStruct.getCommonLot()) || StringUtils.isNotEmpty(legalStruct.getBlock())) 
								&& (StringUtils.isNotEmpty(legalStruct.getTract()) || StringUtils.isNotEmpty(legalStruct.getPlatInst()) || (StringUtils.isNotEmpty(legalStruct.getMapBook()) || StringUtils.isNotEmpty(legalStruct.getArbTract())))
								&& (StringUtils.isNotEmpty(legalStruct.getMapCode()))
								){
							found = true;
						}
					} else if (prop.hasTownshipLegal()){
							
							boolean nameMatched = false;
							
							if (applyNameMatch){
								if (GenericNameFilter.isMatchGreaterThenScore(owner, doc.getGrantor(), NameFilterFactory.NAME_FILTER_THRESHOLD)
										|| GenericNameFilter.isMatchGreaterThenScore(doc.getGrantor(), owner, NameFilterFactory.NAME_FILTER_THRESHOLD)
										|| GenericNameFilter.isMatchGreaterThenScore(owner, doc.getGrantee(), NameFilterFactory.NAME_FILTER_THRESHOLD)
										|| GenericNameFilter.isMatchGreaterThenScore(doc.getGrantee(), owner, NameFilterFactory.NAME_FILTER_THRESHOLD)){
										nameMatched = true;
								}
							}
							
							TownShipI twp = prop.getLegal().getTownShip();
							if (StringUtils.isNotEmpty(twp.getSection())){
								LegalStructPI legalStruct = new LegalStructPI(SECTIONAL_TYPE);
								legalStruct.setSection(twp.getSection());
								legalStruct.setTownship(twp.getTownship());
								legalStruct.setRange(twp.getRange());
								legalStruct.setMapCode(mapCode);
								legalStruct.setMapLegalName(mapLegalName);
								if ((nameMatched || !applyNameMatch)
										 && (StringUtils.isNotEmpty(legalStruct.getSection()) 
												 && StringUtils.isNotEmpty(legalStruct.getTownship()) 
												 && StringUtils.isNotEmpty(legalStruct.getRange()))){
									found = true;
								}
							} else if (StringUtils.isNotEmpty(twp.getArb())){
								LegalStructPI legalStruct = new LegalStructPI(ARB_TYPE);
								legalStruct.setArbTract(twp.getArb());
								legalStruct.setMapCode(mapCode);
								legalStruct.setMapLegalName(mapLegalName);
								legalStruct.setParcel(twp.getParcel());
								
								if ((nameMatched || !applyNameMatch)
										 && (StringUtils.isNotEmpty(legalStruct.getArbTract()))) {
									found = true;
								}
							}
						}
				}
			}
			if (found){
				ret.add(doc);
				break;
			}
		}
		
		return ret;
	}
	
	public void addIteratorModule(TSServerInfo serverInfo, List<TSServerInfoModule> modules, int code, long searchId, boolean isUpdate){

		TSServerInfoModule module = new TSServerInfoModule(serverInfo.getModule(code));
		module.clearSaKeys();
		
		FilterResponse legalFilter 		= LegalFilterFactory.getDefaultLegalFilter(searchId);
		FilterResponse addressFilter 	= AddressFilterFactory.getAddressHighPassFilter(searchId, 0.8d);

		LegalDescriptionIterator it = getLegalDescriptionIterator(true);

		module.addIterator(it);
		if (isUpdate){
			module.addFilter(new BetweenDatesFilterResponse(searchId));
		}

		module.addCrossRefValidator(addressFilter.getValidator());
		module.addCrossRefValidator(legalFilter.getValidator());
		
		if (code == TSServerInfo.SECTION_LAND_MODULE_IDX){
			GenericNameFilter nameFilterOwner = (GenericNameFilter) NameFilterFactory.getDefaultNameFilter(SearchAttributes.OWNER_OBJECT, searchId, null);
			nameFilterOwner.setInitAgain(true);
			nameFilterOwner.setUseSynonymsBothWays(false);
			nameFilterOwner.setIgnoreMiddleOnEmpty(true);
			module.addFilter(nameFilterOwner);
		}


		modules.add(module);
	}
	
	protected ArrayList<NameI>  addNameSearch(List<TSServerInfoModule> modules, TSServerInfo serverInfo, String key, ArrayList<NameI> searchedNames, FilterResponse ...filters) {
		ConfigurableNameIterator nameIterator = null;
		
		TSServerInfoModule module = new TSServerInfoModule(serverInfo.getModule(TSServerInfo.NAME_MODULE_IDX));
		if (StringUtils.isNotBlank(key)){
			if (SearchAttributes.OWNER_OBJECT.equals(key)){
				module.addExtraInformation(TSServerInfoConstants.EXTRA_PARAM_MODULE_DESCRIPTION, TSServerInfoConstants.VALUE_PARAM_NAME_OWNERS);
			} else if (SearchAttributes.BUYER_OBJECT.equals(key)){
				module.addExtraInformation(TSServerInfoConstants.EXTRA_PARAM_MODULE_DESCRIPTION, TSServerInfoConstants.VALUE_PARAM_NAME_BUYERS);
			}
		}
		module.clearSaKey(1);
		module.setSaObjKey(key);
		
		for (int i = 0; i < filters.length; i++){
			module.addFilter(filters[i]);
		}
		FilterResponse addressFilter 	= AddressFilterFactory.getAddressHighPassFilter(searchId, 0.8d);
		
		GenericMultipleLegalFilter multipleLegalFilter = new GenericMultipleLegalFilter(searchId);
		multipleLegalFilter.setUseLegalFromSearchPage(true);
		multipleLegalFilter.setMarkIfCandidatesAreEmpty(true);
		multipleLegalFilter.setThreshold(new BigDecimal(0.7));
		multipleLegalFilter.setAdditionalInfoKey(AdditionalInfoKeys.PI_LOOK_UP_DATA);
		
		module.addCrossRefValidator(addressFilter.getValidator());
		module.addCrossRefValidator(multipleLegalFilter.getValidator());
		
		addBetweenDateTest(module, false, true, true);
		addFilterForUpdate(module, true);

		module.setIteratorType(0,  FunctionStatesIterator.ITERATOR_TYPE_FIRST_NAME_FAKE);
		module.setIteratorType(2,  FunctionStatesIterator.ITERATOR_TYPE_LAST_NAME_FAKE);
		
		nameIterator = (ConfigurableNameIterator) ModuleStatesIteratorFactory.getConfigurableNameIterator(module, searchId, new String[] { "L;F;" });
		nameIterator.setAllowMcnPersons( true );
		
		if (searchedNames != null) {
			nameIterator.setInitAgain(true);
			nameIterator.setSearchedNames(searchedNames);
		}
		
		searchedNames = nameIterator.getSearchedNames();
		module.addIterator(nameIterator);
		
	
		modules.add(module);
		return searchedNames;
	}
	
	@Override
	public ImageDownloadResponse downloadImageResponse(Map<String, String> params, PropertyInsightImageConn imagesConn){
	 	
 		ImageDownloadResponse imageresponse = null;
		try {
			String imageId = params.get("imageId");
			if (StringUtils.isNotEmpty(imageId)){
				imageId = prepareImageId(imageId);
				if (!imageId.contains("Type=")){
					imageId += ",Type=Rec,SubType=All";
				}
				String query = getBasePiQuery(dataSite.getStateFIPS(), dataSite.getCountyFIPS(), searchId) + "," + imageId;
				imageresponse = imagesConn.getDocumentsByParameters2(query.replaceAll(";", ","));
				//imageresponse = imagesConn.getDocumentsByParameters2("FIPS=17097,Type=STR,SubType=PLANT,Year=2003,Order=ST5050464,Flag=ST");
				//imageresponse = imagesConn.getDocumentsByParameters2("FIPS=17097,Type=REC,SubType=ALL,Year=1989");
				//imageresponse = imagesConn.getDocumentsByParameters2("FIPS=17097,County=ILLK,Type=STR,SubType=PLANT,DATE=19991011,ORDER=632042,CMP=CTI,CMP3=CTI,DOC=STR,FLAG=ST");

			}
			
		} catch (IOException e) {
			e.printStackTrace();
		}
		return imageresponse;
	}
	
	@Override
	public void saveWithCrossReferences(InstrumentI instrument, ParsedResponse currentResponse){

		String book 	= instrument.getBook();
		String page 	= instrument.getPage();   
		String instNo 	= instrument.getInstno(); 
		
		if (StringUtils.isNotEmpty(book) && StringUtils.isNotEmpty(page)) {

			TSServerInfoModule module = this.getDefaultServerInfo().getModuleForSearch(TSServerInfo.BOOK_AND_PAGE_MODULE_IDX, new SearchDataWrapper());
			module.addExtraInformation(TSServerInfoConstants.EXTRA_PARAM_DO_NOT_REMOVE_IN_MEMORY_DOCS, true);
			module.addExtraInformation(TSServerInfoConstants.EXTRA_PARAM_SIMULATE_CROSSREF, Boolean.TRUE);
			module.addExtraInformation(TSServerInfoConstants.EXTRA_PARAM_CHECK_ALREADY_SAVED, Boolean.TRUE);
			module.addExtraInformation(TSServerInfoConstants.EXTRA_PARAM_MODULE_DESCRIPTION, "Save with crossreferences");

			module.forceValue(0, book);
			module.forceValue(1, page);

			String tempBookPageLink = createPartialLink(TSConnectionURL.idPOST, TSServerInfo.BOOK_AND_PAGE_MODULE_IDX) + "&book=" + book
																	+ "&page=" + page + "&dummy=" + PREFIX_FINAL_LINK + book + "_" + page + "&isSubResult=true";

			LinkInPage linkInPage = new LinkInPage(module, TSServer.REQUEST_SEARCH_BY_REC);
			if (tempBookPageLink != null) {
				linkInPage.setOnlyLink(tempBookPageLink);
			}
			ParsedResponse prChild = new ParsedResponse();
			prChild.setPageLink(linkInPage);
			currentResponse.addOneResultRowOnly(prChild);
		}
		if (StringUtils.isNotEmpty(instNo)){
			
			String year	= "";
			try {
				year = Integer.toString(instrument.getYear());
			} catch (Exception e) {
			}
			if (instNo.contains("-")){
				String[] ss = instNo.split("\\s*-\\s*");
				if (StringUtils.isEmpty(year)){
					year = ss[0];
				}
				instNo = ss[1];
			}
			if (StringUtils.isNotEmpty(year)){
				TSServerInfoModule module = this.getDefaultServerInfo().getModuleForSearch(TSServerInfo.INSTR_NO_MODULE_IDX, new SearchDataWrapper());
				module.addExtraInformation(TSServerInfoConstants.EXTRA_PARAM_DO_NOT_REMOVE_IN_MEMORY_DOCS, true);
				module.addExtraInformation(TSServerInfoConstants.EXTRA_PARAM_SIMULATE_CROSSREF, Boolean.TRUE);
				module.addExtraInformation(TSServerInfoConstants.EXTRA_PARAM_CHECK_ALREADY_SAVED, Boolean.TRUE);
				module.addExtraInformation(TSServerInfoConstants.EXTRA_PARAM_MODULE_DESCRIPTION, "Save with crossreferences");
				
				module.forceValue(0, instNo);
				module.forceValue(1, year);

				String tempInstrumentLink = createPartialLink(TSConnectionURL.idPOST, TSServerInfo.INSTR_NO_MODULE_IDX) + "&instrument=" + instNo
																+ "&year=" + year + "&dummy=" + PREFIX_FINAL_LINK + year + "_" + instNo + "&isSubResult=true";
				
				LinkInPage linkInPage = new LinkInPage(module, TSServer.REQUEST_SEARCH_BY_REC);
				if (tempInstrumentLink != null) {
					linkInPage.setOnlyLink(tempInstrumentLink);
				}
				ParsedResponse prChild = new ParsedResponse();
				prChild.setPageLink(linkInPage);
		
				currentResponse.addOneResultRowOnly(prChild);
			}
		}
	}
	
	@Override
	public String getPrettyFollowedLink (String initialFollowedLnk){
		Matcher ssfLinkMatcher = finalDocLinkPattern.matcher(initialFollowedLnk);
		
		if (ssfLinkMatcher.find()) {
			return "<br/><span class='followed'>Document: " + ssfLinkMatcher.group(1) + " has already been processed from a previous search in the log file.</span><br/>";
		}
    	
		return super.getPrettyFollowedLink(initialFollowedLnk);
    }
	@Override
	protected DownloadImageResult saveImage(ImageI image)  throws ServerResponseException{
	   
		TSServerInfo info = getDefaultServerInfo();
	   	TSServerInfoModule module = info.getModule(TSServerInfo.IMG_MODULE_IDX);
	    String imageId = "";
	    if (image.getLinks().size() > 0){
			String link = image.getLink(0);
			int poz = link.indexOf("imageId=");
			
			if (poz > 0){
				link = link.substring(poz + 8);
			}
		   	imageId = prepareImageId(link);
		   	module.setParamValue(0, imageId);
		  
		   
		   	String imageName = image.getPath();
		   	if (FileUtils.existPath(imageName)){
		   		byte b[] = FileUtils.readBinaryFile(imageName);
		   		return new DownloadImageResult(DownloadImageResult.Status.OK, b, image.getContentType());
		   	}
		   	
		   	ServerResponse response = SearchBy(module, null);
		   	DownloadImageResult res = response.getImageResult();
		   	return res;
	    }
	    return new DownloadImageResult(DownloadImageResult.Status.ERROR, new byte[0], image.getContentType());
	}
	
	public String prepareImageId(String imageId){
		
		imageId = imageId.replaceAll("@", "=");
		if (imageId.contains(":")){
			imageId = imageId.replaceFirst("[^:]+:(.*\\s+)", "Year=$1").replaceFirst("\\s+", ",Inst=");
		}
		
		return imageId;
	}
	
	@Override
	public String cleanHtmlBeforeSavingDocument(String htmlContent){
		
		htmlContent = htmlContent.replaceAll("(?is)<tr[^>]*>\\s*<td[^>]*>\\s*<input[^>]*>\\s*</td>\\s*<td[^>]*>\\s*(<b>\\s*)?PI\\s*(</b>\\s*)?</td>", "");
		return htmlContent;
	}
	
	@Override
	public void processOcredInstrument(InstrumentI in, boolean ocrReportedBookPage) {
		if(!ocrReportedBookPage){
			
			String inst = StringUtils.defaultString(in.getInstno());
			if(inst.contains("-")){				
				inst = inst.replaceAll("[-]+", "");
				
				in.setInstno(inst);
			}
		}
	}
	
	@Override
	protected boolean isTPPCounty(DataSite dataSite){
 		if (CountyConstants.CA_Alameda == dataSite.getCountyId()
 			|| CountyConstants.CA_Contra_Costa == dataSite.getCountyId()
 			|| CountyConstants.CA_Merced == dataSite.getCountyId()
 			|| CountyConstants.CA_Sacramento == dataSite.getCountyId()
 			|| CountyConstants.CA_San_Francisco == dataSite.getCountyId()
 			|| CountyConstants.CA_San_Joaquin == dataSite.getCountyId()
 			|| CountyConstants.CA_Solano == dataSite.getCountyId()
 			|| CountyConstants.CA_Stanislaus == dataSite.getCountyId()
 				
 				){
 			return true;
 		}
		
 		return false;
 	}
	
	@Override
	protected void addPropertiesToRegisterDoc(DocumentI doc, Node itemAsNode, ParsedResponse item){
		
		NodeList allLegals = getTransactions(itemAsNode, "Parcels");
		if (allLegals != null && allLegals.getLength() > 0){
			PropertyI propertyNew = null;
			Set<PropertyI> properties = doc.getProperties();
			if (properties != null && properties.size() == 1){
				for (PropertyI propertyI : properties) {
					propertyNew = propertyI;
					doc.setProperties(new  LinkedHashSet<PropertyI>());
					break;
				}
			}
			if (propertyNew == null){
				propertyNew = Property.createEmptyProperty();
			}
			for (int i = 0; i < allLegals.getLength(); i++){
				String content = XmlUtils.createXMLString(allLegals.item(i), true);
				if (ro.cst.tsearch.utils.StringUtils.isNotEmpty(content)){
					
					PropertyI property = propertyNew.clone();
					SubdivisionDetailedI subdivisionDetailedI = new SubdivisionDetailed();
					property.getLegal().setSubdivision(subdivisionDetailedI);
					
					Pattern p = Pattern.compile("(?is)<Unit>([^<]*)</Unit>");
					Matcher mat = p.matcher(content);
					if (mat.find()){
						String unit = mat.group(1).trim();
						subdivisionDetailedI.setUnit(unit);
					}
						
					p = Pattern.compile("(?is)<Block>([^<]*)</Block>");
					mat = p.matcher(content);
					if (mat.find()){
						String block = mat.group(1).trim();
						subdivisionDetailedI.setBlock(block);
					}
					p = Pattern.compile("(?is)<Lot>([^<]*)</Lot>");
					mat = p.matcher(content);
					String lot = "";
					if (mat.find()){
						lot = mat.group(1).trim();
					}
					if (StringUtils.isNotEmpty(lot)){
						lot = LegalDescription.cleanValues(lot, false, true);
						subdivisionDetailedI.setLot(lot);
					}
					p = Pattern.compile("(?is)<CommonLot>([^<]*)</CommonLot>");
					mat = p.matcher(content);
					if (mat.find()){
						String commonLot = mat.group(1).trim();
						subdivisionDetailedI.setSubLot(commonLot);
					}
					p = Pattern.compile("(?is)<ArbTract>([^<]*)</ArbTract>");
					mat = p.matcher(content);
					if (mat.find()){
						String arb = mat.group(1).trim();
						property.getLegal().getTownShip().setArb(arb);
					} else{
						p = Pattern.compile("(?is)</MajorLegalName>\\s*<Book>([^<]*)</Book>");
						mat = p.matcher(content);
						if (mat.find()){
							String pb = mat.group(1).trim();
							subdivisionDetailedI.setPlatBook(pb);
						}
						p = Pattern.compile("(?is)</Book>\\s*<Page>([^<]*)</Page>");
						mat = p.matcher(content);
						if (mat.find()){
							String pp = mat.group(1).trim();
							subdivisionDetailedI.setPlatPage(pp);
						}
					}
					//CASan Diego: MB-MP: - 	Lot: 5	Map Name: PARCEL MAP NO 14961	Map Date: 9/4/1987	MCode: MAP	MLName: PARCEL MAPS
					p = Pattern.compile("(?is)<Map>([^<]*)</Map>");
					mat = p.matcher(content);
					if (mat.find()){
						String pInstr = mat.group(1).trim();
						if (StringUtils.isNumeric(pInstr)){
							subdivisionDetailedI.setPlatInstrument(pInstr);
						}
					}
					
					p = Pattern.compile("(?is)<Tract>([^<]*)</Tract>");
					mat = p.matcher(content);
					if (mat.find()){
						String tract = mat.group(1).trim();
						subdivisionDetailedI.setTract(tract);
					}
					p = Pattern.compile("(?is)<Phase>([^<]*)</Phase>");
					mat = p.matcher(content);
					if (mat.find()){
						String phase = mat.group(1).trim();
						subdivisionDetailedI.setPhase(phase);
					}
					p = Pattern.compile("(?is)<MapCode>([^<]*)</MapCode>");
					mat = p.matcher(content);
					if (mat.find()){
						String mapCode = mat.group(1).trim();
						subdivisionDetailedI.setPlatBookType(mapCode);
					}
					p = Pattern.compile("(?is)<MajorLegalName>([^<]*)</MajorLegalName>");
					mat = p.matcher(content);
					if (mat.find()){
						String mjrLglName = mat.group(1).trim();
						subdivisionDetailedI.setPlatInstrumentType(mjrLglName);
					}
					
					p = Pattern.compile("(?is)<Section>([^<]*)</Section>");
					mat = p.matcher(content);
					if (mat.find()){
						String sec = mat.group(1).trim();
						property.getLegal().getTownShip().setSection(sec);
					}
					p = Pattern.compile("(?is)<Township>([^<]*)</Township>");
					mat = p.matcher(content);
					if (mat.find()){
						String twp = mat.group(1).trim();
						property.getLegal().getTownShip().setTownship(twp);
					}
					p = Pattern.compile("(?is)<Range>([^<]*)</Range>");
					mat = p.matcher(content);
					if (mat.find()){
						String rng = mat.group(1).trim();
						property.getLegal().getTownShip().setRange(rng);
					}
					p = Pattern.compile("(?is)<Parcel>([^<]*)</Parcel>");
					mat = p.matcher(content);
					if (mat.find()){
						String parcel = mat.group(1).trim();
						property.getLegal().getTownShip().setParcel(parcel);
					}
					doc.addProperty(property);
				}
			}
			item.setUseDocumentForSearchLogRow(true);
		}
	}
	
	@SuppressWarnings({ "unchecked", "rawtypes" })
	@Override
	public void parseAndFillResultMap(Node node, ResultMap m, long searchId, int moduleIDX, String resultType) {
		
		m.put(OtherInformationSetKey.SRC_TYPE.getKeyName(), dataSite.getSiteTypeAbrev());
		
		
		if (O_E_PI_TYPE.equalsIgnoreCase(resultType)){
			//for fake image module
			
			String book = StringUtils.defaultString(findFastNodeValue(node, "Book"));
			String page = StringUtils.defaultString(findFastNodeValue(node, "Page"));
			String instrNo = StringUtils.defaultString(findFastNodeValue(node, "DocumentNumber"));
			String serverDocType = StringUtils.defaultString(findFastNodeValue(node, "DocumentType"));
			String recordingDate = StringUtils.defaultString(findFastNodeValue(node,"RecordingDate"));
			String instrumentDate = StringUtils.defaultString(findFastNodeValue(node,"ContractDate"));
			String amount = StringUtils.defaultString(findFastNodeValue(node, "Loan1Amount"));
			if (StringUtils.isBlank(amount)){
				amount = StringUtils.defaultString(findFastNodeValue(node, "LoanAmount"));
			}
			String imageId = StringUtils.defaultString(findFastNodeValue(node,"Image"));
			
			try {
				String year = recordingDate.substring(0, 4);
				if (StringUtils.isNotEmpty(year) && year.matches("(?is)\\d{4}")){
					instrNo = year + "-" + instrNo;
				}
				m.put(SaleDataSetKey.INSTRUMENT_NUMBER.getKeyName(), instrNo);
				m.put(SaleDataSetKey.BOOK.getKeyName(), book);
				m.put(SaleDataSetKey.PAGE.getKeyName(), page);
				m.put(SaleDataSetKey.DOCUMENT_TYPE.getKeyName(), serverDocType);
				m.put(SaleDataSetKey.RECORDED_DATE.getKeyName(), recordingDate);
				m.put(SaleDataSetKey.INSTRUMENT_DATE.getKeyName(), instrumentDate);
				m.put(SaleDataSetKey.INSTR_CODE.getKeyName(), imageId);
				if (ro.cst.tsearch.utils.StringUtils.isNotEmpty(amount)){
					amount = amount.replaceAll("(?is)[\\$,]+", "");
					m.put(SaleDataSetKey.MORTGAGE_AMOUNT.getKeyName(), amount);
					m.put(SaleDataSetKey.CONSIDERATION_AMOUNT.getKeyName(), amount);
				}
			}catch(Exception e) {
				e.printStackTrace();
			}
		
		} else{
		
			String instrNo = StringUtils.defaultString(findFastNodeValue(node, "DocumentIdentification/InstrumentNumber"));
			if (StringUtils.isEmpty(instrNo)){
				instrNo = StringUtils.defaultString(findFastNodeValue(node, "DocumentIdentification/CaseNumber"));
			}
			String bookPage = StringUtils.defaultString(findFastNodeValue(node, "DocumentIdentification/BookPage"));
			if (StringUtils.isNotEmpty(bookPage)){
				String[] bp = bookPage.split("\\s*/\\s*");
				if (bp.length == 2){
					m.put(SaleDataSetKey.BOOK.getKeyName(), bp[0]);
					m.put(SaleDataSetKey.PAGE.getKeyName(), bp[1]);
				}
			}
			if (StringUtils.isEmpty(instrNo)){
				instrNo = StringUtils.defaultString(findFastNodeValue(node, "DocumentIdentification/Note"));
			}
			String serverDocType = StringUtils.defaultString(findFastNodeValue(node, "DocumentFullName"));
			
			String recordingDate = StringUtils.defaultString(findFastNodeValue(node, "DocumentIdentification/RecordingDate"));
			String instrumentDate = StringUtils.defaultString(findFastNodeValue(node, "DocumentIdentification/DatedDate"));
//			if (Utils.isJvmArgumentTrue("debugForATSProgrammer")){
//				try {
//					String serverDocTypeCode = StringUtils.defaultString(findFastNodeValue(node, "DocumentType"));
//					FileWriter fwi = new FileWriter(new File("D:\\PI\\" + dataSite.getStateAbbreviation() + "\\" + dataSite.getCountyName() + "_doctypes.txt"),true);
//					fwi.write(instrNo + "#####" + dataSite.getCountyName() + "#####" + serverDocType + "#####" + serverDocTypeCode + "#####" + recordingDate);
//					fwi.write("\n");
//					fwi.close();
//				} catch (IOException e2) {
//					e2.printStackTrace();
//				}
//			}
			if (StringUtils.isEmpty(instrumentDate)){
				instrumentDate = StringUtils.defaultString(findFastNodeValue(node, "DocumentIdentification/JudgementDate"));
			}
			
			if (StringUtils.isNotEmpty(instrNo)){
				if (StringUtils.isNotEmpty(recordingDate) && recordingDate.length() > 4){
					instrNo = recordingDate.substring(0, 4) + "-" + instrNo;
				} else if (StringUtils.isNotEmpty(instrumentDate) && instrumentDate.length() > 4){
					instrNo = instrumentDate.substring(0, 4) + "-" + instrNo;
				}  
			}
				
			m.put(SaleDataSetKey.INSTRUMENT_NUMBER.getKeyName(), instrNo);
			m.put(SaleDataSetKey.DOCUMENT_TYPE.getKeyName(), serverDocType);
			m.put(SaleDataSetKey.RECORDED_DATE.getKeyName(), recordingDate);
			m.put(SaleDataSetKey.INSTRUMENT_DATE.getKeyName(), instrumentDate);

			String amount = StringUtils.defaultString(findFastNodeValue(node, "LoanAmount"));
			m.put(SaleDataSetKey.MORTGAGE_AMOUNT.getKeyName(), amount);
			if (StringUtils.isEmpty(amount)){
				amount = StringUtils.defaultString(findFastNodeValue(node, "CourtAmount"));
			}
			if (StringUtils.isEmpty(amount)){
				amount = StringUtils.defaultString(findFastNodeValue(node, "EstimatedPurchaseAmount"));
			}
				
			amount = amount.replaceAll("[\\$,]+", "");
			m.put(SaleDataSetKey.CONSIDERATION_AMOUNT.getKeyName(), amount);
			
			StringBuilder imageId = new StringBuilder();
			String imageInstrNumber = StringUtils.defaultString(findFastNodeValue(node, "Image/Item/InstrumentNumber"));
			if (StringUtils.isNotBlank(imageInstrNumber)){
				String imageDocInfo = StringUtils.defaultString(findFastNodeValue(node, "Image/Item/DocInfo"));
				String imageType = StringUtils.defaultString(findFastNodeValue(node, "Image/Item/Type"));
				String imageSubType = StringUtils.defaultString(findFastNodeValue(node, "Image/Item/SubType"));
				
				if (StringUtils.isNotBlank(imageDocInfo)){
					imageId.append(imageDocInfo);
				}
				if (StringUtils.isNotBlank(imageType)){
					imageId.append(";Type=").append(imageType);
				}
				if (StringUtils.isNotBlank(imageSubType)){
					imageId.append(";SubType=").append(imageSubType);
				}
					
//			imageId.append(",");imageId.append("Type=").append("REC");//ro.cst.tsearch.utils.StringUtils.extractParameterFromUrl(docInfo, "Doc"));
//			imageId.append(",");
//			imageId.append("SubType=").append("ALL");//StringUtils.defaultString(findFastNodeValue(node, "Image/Item/SubType")));
//			imageId.append(",");
				
//			imageId.append(",");imageId.append("Type=").append("REC");//ro.cst.tsearch.utils.StringUtils.extractParameterFromUrl(docInfo, "Doc"));
//			imageId.append(",");
//			imageId.append("SubType=").append("ALL");//StringUtils.defaultString(findFastNodeValue(node, "Image/Item/SubType")));
//			imageId.append(",");
				
				m.put(SaleDataSetKey.INSTR_CODE.getKeyName(), imageId.toString());
			}
				
			String remarks = StringUtils.defaultString(findFastNodeValue(node, "PropertyRemark"));
				
			remarks = remarks.replace(((char) 254), ' ');
			remarks = remarks.replaceAll("&#254;", " ");
			remarks = remarks.replaceAll("&thorn;", " ");
			
			if (StringUtils.isNotEmpty(remarks)){
				m.put("tmpRemarks", remarks);
			}
				
			String situsAddress = StringUtils.defaultString(findFastNodeValue(node, "DocumentAddresses/Address/SitusAddress"));
			if (StringUtils.isNotEmpty(situsAddress)){
				situsAddress = situsAddress.replaceAll("(?is)\\bCREEK CR\\b", "CREEK CIR");//2012R0013218 McHenry
				m.put(PropertyIdentificationSetKey.STREET_NO.getKeyName(), StringFormats.StreetNo(situsAddress));
				m.put(PropertyIdentificationSetKey.STREET_NAME.getKeyName(), StringFormats.StreetName(situsAddress));
			}
				
			try {
				CAParsingPI.parsingRemarks(m, searchId);
			} catch (Exception e1) {
				e1.printStackTrace();
			}
			try {
				parseNames(m, node, resultType, searchId);
			} catch (Exception e1) {
				e1.printStackTrace();
			}
				
			String refInstrNo = StringUtils.defaultString(findFastNodeValue(node, "ReferencedDocuments/DocumentIdentification/InstrumentNumber"));
			String refBookPage = StringUtils.defaultString(findFastNodeValue(node, "ReferencedDocuments/DocumentIdentification/BookPage"));
			if (StringUtils.isNotEmpty(refInstrNo) || StringUtils.isNotEmpty(refBookPage)){
				String refRecordingDate = StringUtils.defaultString(findFastNodeValue(node, "ReferencedDocuments/DocumentIdentification/RecordingDate"));
					
				String year = "";
				if (StringUtils.isNotEmpty(refRecordingDate)){
					year = refRecordingDate.substring(0, 4);
				}
					
				if (StringUtils.isNotEmpty(refInstrNo) && StringUtils.isNotEmpty(year) && year.length() == 4){
					refInstrNo = year + "-" + refInstrNo;
				}
						
				String day = "", month = "";
				if (StringUtils.isNotEmpty(refRecordingDate) && refRecordingDate.matches("(?is)\\d{4}-\\d{2}-\\d{2}")){
					month = refRecordingDate.substring(5, 7);
					day = refRecordingDate.substring(8, 10);
				}
				String book = "", page = "";
				if (StringUtils.isNotEmpty(refBookPage)){
					String[] bp = refBookPage.split("\\s*/\\s*");
					if (bp.length == 2){
						book = bp[0];
						page = bp[1];
					}
				}
						
				List<List> body = new ArrayList<List>();
				List line = new ArrayList<String>();
				line.add(refInstrNo);
				line.add(book);
				line.add(page);
				line.add(year);
				line.add(month);
				line.add(day);
				body.add(line);
				
				if (!body.isEmpty()) {
					ResultTable crs = (ResultTable) m.get("CrossRefSet");
					if (crs != null){
						String[][] bodyCRS = crs.getBodyRef();
						for (String[] bodyLine : bodyCRS) {
							List newLine = new ArrayList<String>();
							for (String string : bodyLine) {
								
								newLine.add(string);
							}
							//Ref Instr from document contains Date, so, is more complete 
							if (!newLine.isEmpty() && !body.contains(newLine)){
								if ((line.get(0).equals(newLine.get(0)))
										|| (line.get(1).equals(newLine.get(1)) && line.get(2).equals(newLine.get(2)))){
									continue;
								}
								body.add(newLine);
							}
						}
					}
					String[] header = { "InstrumentNumber", "Book", "Page", "Year", "Month", "Day" };
					m.put("CrossRefSet", GenericFunctions2.createResultTable(body, header));
				}
			}
		}
	}
	
	@Override
	public Object getRecoverModuleFrom(RestoreDocumentDataI restoreDocumentDataI) {
		if (restoreDocumentDataI == null) {
			return null;
		}
		
		List<TSServerInfoModule> list = new ArrayList<TSServerInfoModule>();
		
		String book = restoreDocumentDataI.getBook();
		String page = restoreDocumentDataI.getPage();
		TSServerInfoModule module = null;
		
		if (ro.cst.tsearch.utils.StringUtils.isNotEmpty(book) && ro.cst.tsearch.utils.StringUtils.isNotEmpty(page)) {
			module = getDefaultServerInfo().getModule(TSServerInfo.BOOK_AND_PAGE_MODULE_IDX);
			module.forceValue(0, book);
			module.forceValue(1, page);
			list.add(module);
		}
		
		if (ro.cst.tsearch.utils.StringUtils.isNotEmpty(restoreDocumentDataI.getInstrumentNumber()) 
				&& restoreDocumentDataI.getRecordedDate() != null) {
			Calendar cal = Calendar.getInstance();
			cal.setTime(restoreDocumentDataI.getRecordedDate());
			
			String instrumentNo = restoreDocumentDataI.getInstrumentNumber();
			if (instrumentNo.contains("-")){
				instrumentNo = instrumentNo.substring(instrumentNo.indexOf("-") + 1);
			}
			module = getDefaultServerInfo().getModule(TSServerInfo.INSTR_NO_MODULE_IDX);
			module.forceValue(0, instrumentNo);
			module.forceValue(1, Integer.toString(cal.get(Calendar.YEAR)));
			module.forceValue(2, "TitlePoint.Geo.Document");
			list.add(module);
		}
		
		module = getDefaultServerInfo().getModule(TSServerInfo.FAKE_MODULE_IDX);
		module.addExtraInformation(TSServerInfoConstants.EXTRA_PARAM_RESTORE_DOCUMENT_SOURCE, 
				restoreDocumentDataI);
		list.add(module);
		
		return list;
	}
	
	@Override
	protected boolean addNamesFromOCR(List<NameI> namesFromOCR, PType nameType, 
			DocumentsManagerI documentsManager, 
			RegisterDocumentI documentI,
			StringBuilder infoToBeLogged) {
		if (PType.GRANTEE.equals(nameType)) {
			if (!namesFromOCR.isEmpty() && !documentI.getGrantee().getNames().isEmpty() && 
					!documentI.isFieldModified(DocumentI.Fields.GRANTEE)) {
				documentI.getGrantee().clear();
				infoToBeLogged.append("Deleting previous grantee information from document<br>");
			}
			if(documentI instanceof TransferI) {
				PartyI owners = getSearch().getSa().getOwners();
				Set<NameI> toRemoveNames = new LinkedHashSet<>();
				for (NameI nameI : owners.getNames()) {
					if(nameI.getNameFlags().isFrom(new NameSourceType(NameSourceType.DOCUMENT, documentI.getId()))) {
						toRemoveNames.add(nameI);
					}
				}
				if(!toRemoveNames.isEmpty()) {
					infoToBeLogged.append("Deleting names from Search Page previously added by this document:<br>");
					for (NameI nameI : toRemoveNames) {
						infoToBeLogged.append("&nbsp;&nbsp;").append(nameI.toString()).append("<br>");
					}
					owners.getNames().removeAll(toRemoveNames);
				}
			}
		} else if (PType.GRANTOR.equals(nameType)) {
			if (!namesFromOCR.isEmpty() && !documentI.getGrantor().getNames().isEmpty() && 
					!documentI.isFieldModified(DocumentI.Fields.GRANTOR)) {
				documentI.getGrantor().clear();
				infoToBeLogged.append("Deleting previous grantor information from document<br>");
			}
		}
		return super.addNamesFromOCR(namesFromOCR, nameType, documentsManager, documentI, infoToBeLogged);
	}
	
	@Override
	public boolean isAlreadySaved(String instrumentNo, DocumentI doc) {

		boolean isAlreadySaved = isInstrumentSaved(instrumentNo, doc, null);

		if (!isAlreadySaved) {
			DocumentsManagerI documentManager = getSearch().getDocManager();
			try {
				documentManager.getAccess();
				DocumentI docClone = null;
				if (doc != null) {
					docClone = doc.clone();
					docClone.setDocSubType(null);

					List<DocumentI> almostLike = documentManager.getDocumentsWithInstrumentsFlexible(false, docClone.getInstrument());
					if (almostLike != null && !almostLike.isEmpty()) {
						return true;
					}
				}
			} catch (Exception e) {
				e.printStackTrace();
			} finally {
				documentManager.releaseAccess();
			}
		}
		return isAlreadySaved;
	}
}
