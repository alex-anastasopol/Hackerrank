package ro.cst.tsearch.servers.types;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;

import org.apache.commons.io.FileUtils;

import ro.cst.tsearch.Search;
import ro.cst.tsearch.bean.SearchAttributes;
import ro.cst.tsearch.data.CountyConstants;
import ro.cst.tsearch.exceptions.ServerResponseException;
import ro.cst.tsearch.search.FunctionStatesIterator;
import ro.cst.tsearch.search.GenericRuntimeIterator;
import ro.cst.tsearch.search.filter.BetweenDatesFilterResponse;
import ro.cst.tsearch.search.filter.FilterResponse;
import ro.cst.tsearch.search.filter.newfilters.date.LastTransferDateFilter;
import ro.cst.tsearch.search.filter.newfilters.doctype.DocTypeAdvancedFilter;
import ro.cst.tsearch.search.filter.newfilters.name.GenericNameFilter;
import ro.cst.tsearch.search.filter.newfilters.name.NameFilterFactory;
import ro.cst.tsearch.search.filter.newfilters.pin.PinFilterResponse;
import ro.cst.tsearch.search.iterator.instrument.InstrumentGenericIterator;
import ro.cst.tsearch.searchsites.client.GWTDataSite;
import ro.cst.tsearch.servers.SearchDataWrapper;
import ro.cst.tsearch.servers.info.TSServerInfo;
import ro.cst.tsearch.servers.info.TSServerInfoConstants;
import ro.cst.tsearch.servers.info.TSServerInfoModule;
import ro.cst.tsearch.servers.response.ServerResponse;
import ro.cst.tsearch.servers.types.FLSubdividedBasedDASLDT.HasLotFilter;
import ro.cst.tsearch.servers.types.FLSubdividedBasedDASLDT.PropertyTypeFilter;
import ro.cst.tsearch.servers.types.TSInterface.DownloadImageResult.Status;
import ro.cst.tsearch.titledocument.abstracts.DocumentTypes;
import ro.cst.tsearch.utils.FormatDate;
import ro.cst.tsearch.utils.HttpUtils;
import ro.cst.tsearch.utils.InstanceManager;
import ro.cst.tsearch.utils.SearchLogger;
import ro.cst.tsearch.utils.StringUtils;

import com.stewart.ats.base.document.DocumentI;
import com.stewart.ats.base.document.ImageI;
import com.stewart.ats.base.document.Instrument;
import com.stewart.ats.base.document.InstrumentI;
import com.stewart.ats.base.name.NameI;
import com.stewart.ats.base.search.DocumentsManagerI;
import com.stewart.ats.connection.clarkimages.NvClarkAOMConn;
import com.stewart.ats.connection.clarkimages.NvClarkAOMConn.MapType;
import com.stewart.ats.tsrindex.client.SimpleChapterUtils;
import com.stewart.datatree.DataTreeAccount;
import com.stewart.datatree.DataTreeConn;
import com.stewart.datatree.DataTreeImageException;
import com.stewart.datatree.DataTreeManager;
import com.stewart.datatree.SimpleImage;

/**
 * @author MihaiB
 */
public class NVGenericDTG extends TSServerDTG implements DTLikeAutomatic{

	private static final long serialVersionUID = 3024488184071095461L;

	public NVGenericDTG(String rsRequestSolverName, String rsSitePath, String rsServerID, String rsPrmNameLink, long searchId, int mid) throws FileNotFoundException, IOException {
		super(rsRequestSolverName, rsSitePath, rsServerID, rsPrmNameLink, searchId, mid);
	}

	public NVGenericDTG(long searchId) {
		super(searchId);
	}
	
	@Override
	public void setModulesForAutoSearch(TSServerInfo serverInfo) {
		
		Search global = InstanceManager.getManager().getCurrentInstance(searchId).getCrtSearchContext();
		List<TSServerInfoModule> modules = new ArrayList<TSServerInfoModule>();
		DTLikeAutomatic dtAuto = this;
		
		dtAuto.prepareApnPerCounty(searchId);
		
		if(global.getSearchType() == Search.AUTOMATIC_SEARCH) {
		
			boolean isUpdate = (isUpdate()) || global.getSa().isDateDown();
			boolean isTimeShare = !StringUtils.isEmpty(global.getSa().getAtribute(SearchAttributes.WEEK));
			//boolean isRefinance = (Products.REFINANCE_PRODUCT == global.getSearchProduct());
			
			String block = global.getSa().getAtribute(SearchAttributes.LD_SUBDIV_BLOCK);
			String lot = global.getSa().getAtribute(SearchAttributes.LD_LOTNO);
			
			FilterResponse legalOrDocTypeFilter = FLSubdividedBasedDASLDT.getLegalOrDocTypeFilter(searchId, false);
			PinFilterResponse pinFilter = FLSubdividedBasedDASLDT.getPinFilter(searchId, true);
			
			TSServerInfoModule module = null;
			
			dtAuto.addAoLookUpSearches(serverInfo, modules, FLSubdividedBasedDASLDT.getAllAoAndTaxReferences(global), searchId, isUpdate,isTimeShare);
			
			GenericNameFilter nameFilterOwner 	=  (GenericNameFilter)NameFilterFactory.getDefaultNameFilter( SearchAttributes.OWNER_OBJECT, global.getID(), null );
			nameFilterOwner.setInitAgain(true);
			if (dtAuto instanceof TSServer) {
				nameFilterOwner.setUseSynonymsBothWays(((TSServer)dtAuto).getDataSite().getSiteTypeInt() != GWTDataSite.DT_TYPE);
			} else {
				nameFilterOwner.setUseSynonymsBothWays(true);
			}
			nameFilterOwner.setIgnoreMiddleOnEmpty(true);
			FilterResponse[] filtersO 	= { 
					nameFilterOwner, 
					legalOrDocTypeFilter, 
					pinFilter, 
					new LastTransferDateFilter(searchId), 
					new PropertyTypeFilter(searchId)};
			List<FilterResponse> filtersOList = Arrays.asList(filtersO);
			
			FilterResponse[] filtersCrossrefs 	= { 
					legalOrDocTypeFilter, 
					pinFilter,
					new PropertyTypeFilter(searchId)};
			
			if (StringUtils.isNotEmpty(lot) || StringUtils.isNotEmpty(block)){
				List<FilterResponse> grantorGranteeFilterList = new ArrayList<FilterResponse>();
				
				grantorGranteeFilterList.add(nameFilterOwner);
				grantorGranteeFilterList.add(legalOrDocTypeFilter);
				grantorGranteeFilterList.add(pinFilter); 
				grantorGranteeFilterList.add(new LastTransferDateFilter(searchId)); 
				grantorGranteeFilterList.add(new PropertyTypeFilter(searchId));
				grantorGranteeFilterList.add(new HasLotFilter("", searchId));
				
				dtAuto.addGrantorGranteeSearch(modules, serverInfo, SearchAttributes.OWNER_OBJECT, grantorGranteeFilterList);
			}
			
			dtAuto.addIteratorModule(serverInfo, modules, TSServerInfo.SUBDIVISION_MODULE_IDX, searchId, isUpdate, isTimeShare);
			
			if (!isTimeShare){
				dtAuto.addIteratorModule(serverInfo, modules, TSServerInfo.ARB_MODULE_IDX, searchId, isUpdate,isTimeShare);
				
				dtAuto.addIteratorModule(serverInfo, modules, TSServerInfo.SECTION_LAND_MODULE_IDX, searchId, isUpdate,isTimeShare);
				
				module = new TSServerInfoModule(serverInfo.getModule(TSServerInfo.PARCEL_ID_MODULE_IDX));
				module.clearSaKeys();
				module.setData(0, dtAuto.prepareApnPerCounty(searchId));
				if (isUpdate) {
					module.addFilter(new BetweenDatesFilterResponse(searchId));
				}

				if (pinFilter != null){
					module.addFilter(pinFilter);
				}
				modules.add(module);
				
				ArrayList<NameI> searchedNames = dtAuto.addNameSearch(modules, serverInfo, SearchAttributes.OWNER_OBJECT, null, filtersOList);
				
				dtAuto.addOCRSearch(modules, serverInfo, legalOrDocTypeFilter);
				
				dtAuto.addNameSearch(modules, serverInfo,SearchAttributes.OWNER_OBJECT, searchedNames==null?new ArrayList<NameI>():searchedNames, filtersOList);
				
			} else{
				List<FilterResponse> filtersOListTS = new ArrayList<FilterResponse>();
				filtersOListTS.add(new DocTypeAdvancedFilter(searchId));
				filtersOListTS.addAll(filtersOList);
				
				ArrayList<NameI> searchedNames = dtAuto.addNameSearch(modules, serverInfo, SearchAttributes.OWNER_OBJECT, null,  filtersOListTS);
				
				dtAuto.addOCRSearch(modules, serverInfo, legalOrDocTypeFilter);
				
				dtAuto.addNameSearch(modules, serverInfo,SearchAttributes.OWNER_OBJECT, searchedNames == null ? new ArrayList<NameI>() : searchedNames, filtersOListTS);
			}

			GenericNameFilter nameFilterBuyer 	=  (GenericNameFilter) NameFilterFactory.getDefaultNameFilter( SearchAttributes.BUYER_OBJECT, global.getID(), null);
			nameFilterBuyer.setIgnoreMiddleOnEmpty( true );
			nameFilterBuyer.setUseSynonymsBothWays(true);
			nameFilterBuyer.setInitAgain(true);
			
			FilterResponse[] filtersB 	= {nameFilterBuyer, new DocTypeAdvancedFilter(searchId)};
			dtAuto.addNameSearch(modules, serverInfo, SearchAttributes.BUYER_OBJECT, null, Arrays.asList(filtersB));
			  
			GenericRuntimeIterator<InstrumentI> instrumentIterator = dtAuto.getInstrumentIterator(false);
			
			if (instrumentIterator != null){
				final int [] REFERECE_IDXS = {TSServerInfo.INSTR_NO_MODULE_IDX, TSServerInfo.BOOK_AND_PAGE_MODULE_IDX};
				for (int moduleIdx : REFERECE_IDXS){
					module = new TSServerInfoModule(serverInfo.getModule(moduleIdx));
					module.clearSaKeys();
					module.addIterator(dtAuto.getInstrumentIterator(false));
					for (FilterResponse filter : filtersCrossrefs){
						module.addFilter(filter);
					}
					if (isUpdate) {
						module.addFilter(new BetweenDatesFilterResponse(searchId));
					}
					modules.add(module);
				}
			}
			
			dtAuto.addRelatedSearch(serverInfo, modules);
			
			InstrumentGenericIterator instrumentGenericIterator;
			{
				instrumentGenericIterator = getInstrumentIteratorForImageFakeDocs(true);

				module = new TSServerInfoModule(serverInfo.getModule(TSServerInfo.BOOK_AND_PAGE_LOCAL_MODULE_IDX));
				module.addExtraInformation(TSServerInfoConstants.EXTRA_PARAM_MODULE_DESCRIPTION, "Lookup for Image with unsaved references from AO/Tax like documents and save as fake documents");
				module.setIteratorType(2, FunctionStatesIterator.ITERATOR_TYPE_INSTRUMENT_LIST_FAKE);
				module.setIteratorType(3, FunctionStatesIterator.ITERATOR_TYPE_YEAR);
				module.setIteratorType(4, FunctionStatesIterator.ITERATOR_TYPE_SERVER_DOCTYPE_SEARCH);
				
				if (isUpdate) {
					module.addFilter(new BetweenDatesFilterResponse(searchId));
				}
				module.addIterator(instrumentGenericIterator);

				modules.add(module);
			}
			InstrumentGenericIterator bpGenericIterator;
			{
				bpGenericIterator = getInstrumentIteratorForImageFakeDocs(false);
				bpGenericIterator.setRemoveLeadingZerosBP(true);

				module = new TSServerInfoModule(serverInfo.getModule(TSServerInfo.BOOK_AND_PAGE_LOCAL_MODULE_IDX));
				module.addExtraInformation(TSServerInfoConstants.EXTRA_PARAM_MODULE_DESCRIPTION, "Lookup for Image with unsaved references from AO/Tax like documents and save as fake documents");
				module.setIteratorType(0, FunctionStatesIterator.ITERATOR_TYPE_BOOK_FAKE);
				module.setIteratorType(1, FunctionStatesIterator.ITERATOR_TYPE_PAGE_FAKE);
				module.setIteratorType(3, FunctionStatesIterator.ITERATOR_TYPE_YEAR);
				module.setIteratorType(4, FunctionStatesIterator.ITERATOR_TYPE_SERVER_DOCTYPE_SEARCH);

				if (isUpdate) {
					module.addFilter(new BetweenDatesFilterResponse(searchId));
				}
				module.addIterator(bpGenericIterator);
				modules.add(module);
			}
			{
				instrumentGenericIterator = getInstrumentIteratorForImageFakeDocs(true);
				instrumentGenericIterator.setDsToLoad(new String[]{dataSite.getSiteTypeAbrev()});
				instrumentGenericIterator.setLoadFromRoLike(true);
				
				module = new TSServerInfoModule(serverInfo.getModule(TSServerInfo.BOOK_AND_PAGE_LOCAL_MODULE_IDX));
				module.addExtraInformation(TSServerInfoConstants.EXTRA_PARAM_MODULE_DESCRIPTION, "Lookup for Image with unsaved references from RO-like documents and save as fake documents");
				module.setIteratorType(2, FunctionStatesIterator.ITERATOR_TYPE_INSTRUMENT_LIST_FAKE);
				module.setIteratorType(3, FunctionStatesIterator.ITERATOR_TYPE_YEAR);
				module.setIteratorType(4, FunctionStatesIterator.ITERATOR_TYPE_SERVER_DOCTYPE_SEARCH);
				if (isUpdate) {
					module.addFilter(new BetweenDatesFilterResponse(searchId));
				}
				module.addIterator(instrumentGenericIterator);

				modules.add(module);
			}
			{
				bpGenericIterator = getInstrumentIteratorForImageFakeDocs(false);
				bpGenericIterator.setRemoveLeadingZerosBP(true);
				bpGenericIterator.setDsToLoad(new String[]{dataSite.getSiteTypeAbrev()});
				bpGenericIterator.setLoadFromRoLike(true);

				module = new TSServerInfoModule(serverInfo.getModule(TSServerInfo.BOOK_AND_PAGE_LOCAL_MODULE_IDX));
				module.addExtraInformation(TSServerInfoConstants.EXTRA_PARAM_MODULE_DESCRIPTION, "Lookup for Image with unsaved references from RO-like documents and save as fake documents");
				module.setIteratorType(0, FunctionStatesIterator.ITERATOR_TYPE_BOOK_FAKE);
				module.setIteratorType(1, FunctionStatesIterator.ITERATOR_TYPE_PAGE_FAKE);
				module.setIteratorType(3, FunctionStatesIterator.ITERATOR_TYPE_YEAR);
				module.setIteratorType(4, FunctionStatesIterator.ITERATOR_TYPE_SERVER_DOCTYPE_SEARCH);

				if (isUpdate) {
					module.addFilter(new BetweenDatesFilterResponse(searchId));
				}
				module.addIterator(bpGenericIterator);
				modules.add(module);
			}
		}
		serverInfo.setModulesForAutoSearch(modules);
	}
	
	public void addRelatedSearch(TSServerInfo serverInfo, List<TSServerInfoModule> modules) {
		
		TSServerInfoModule module = null;
		String[] relatedSourceDoctype = new String[]{
				DocumentTypes.MORTGAGE, 
				DocumentTypes.LIEN, 
				DocumentTypes.CCER
				};
		
		
		InstrumentGenericIterator instrumentRelatedBPIterator = getInstrumentIterator();
		instrumentRelatedBPIterator.enableBookPage();
		instrumentRelatedBPIterator.setLoadFromRoLike(true);
		instrumentRelatedBPIterator.setRoDoctypesToLoad(relatedSourceDoctype);
		instrumentRelatedBPIterator.setDoNotCheckIfItExists(true);
		
		module = new TSServerInfoModule(serverInfo.getModule(TSServerInfo.MODULE_IDX41));
		module.addExtraInformation(TSServerInfoConstants.EXTRA_PARAM_MODULE_DESCRIPTION, 
				"Searching (related) with book/page list from all " + Arrays.toString(relatedSourceDoctype));
		module.clearSaKeys();
		module.setIteratorType(0, FunctionStatesIterator.ITERATOR_TYPE_BOOK_FAKE);
		module.setIteratorType(1, FunctionStatesIterator.ITERATOR_TYPE_PAGE_FAKE);
		module.addIterator(instrumentRelatedBPIterator);
		if (isUpdate()) {
			module.addFilter(BetweenDatesFilterResponse.getDefaultIntervalFilter(searchId));
		}
		module.addFilter(FLSubdividedBasedDASLDT.getLegalOrDocTypeFilter(searchId , true));
		module.addFilter(new PropertyTypeFilter(searchId));
		modules.add(module);
		
		
		addRelatedInstrumentNoSearch(serverInfo, modules, isUpdate(), relatedSourceDoctype);
		
	}

	public void addRelatedInstrumentNoSearch(TSServerInfo serverInfo, List<TSServerInfoModule> modules,
			boolean isUpdate, String[] relatedSourceDoctype) {
		TSServerInfoModule module;
		InstrumentGenericIterator instrumentRelatedNoIterator = getInstrumentIterator();
		instrumentRelatedNoIterator.enableInstrumentNumber();
		instrumentRelatedNoIterator.setLoadFromRoLike(true);
		instrumentRelatedNoIterator.setRoDoctypesToLoad(relatedSourceDoctype);
		instrumentRelatedNoIterator.setDoNotCheckIfItExists(true);
		instrumentRelatedNoIterator.setInitAgain(true);
		
		module = new TSServerInfoModule(serverInfo.getModule(TSServerInfo.MODULE_IDX41));
		module.addExtraInformation(TSServerInfoConstants.EXTRA_PARAM_MODULE_DESCRIPTION, 
				"Searching (related) with Instrument Number list from all " + Arrays.toString(relatedSourceDoctype));
		module.clearSaKeys();
		module.setIteratorType(3, FunctionStatesIterator.ITERATOR_TYPE_INSTRUMENT_LIST_FAKE);
		module.setIteratorType(4, FunctionStatesIterator.ITERATOR_TYPE_YEAR);
		module.addIterator(instrumentRelatedNoIterator);
		if (isUpdate) {
			module.addFilter(BetweenDatesFilterResponse.getDefaultIntervalFilter(searchId));
		}
		module.addFilter(FLSubdividedBasedDASLDT.getLegalOrDocTypeFilter(searchId , true));
		module.addFilter(new PropertyTypeFilter(searchId));
		modules.add(module);
	}
	
	protected InstrumentGenericIterator getInstrumentIterator() {
		InstrumentGenericIterator instrumentBPIterator = new InstrumentGenericIterator(searchId, dataSite){

			/**
			 * 
			 */
			private static final long serialVersionUID = -5198986646280366512L;
			
			public String getYearFrom(InstrumentI inst, HashMap<String, String> filterCriteria) {
				if (dataSite.getCountyId() == CountyConstants.NV_Clark){
					int year = inst.getYear();
					
					if (year < 0) {
						return "";
					}
					String yearStr =  String.valueOf(year);
					
					SimpleDateFormat format = new SimpleDateFormat("MMdd");
					Date date = inst.getDate();
					if (date != null){
						if (year <= 1999){
							if (yearStr.length() == 4){
								return yearStr.substring(2,4) + format.format(date);
							}
						} else{
							return yearStr + format.format(date);
						}
					}
					return yearStr;
				} else{
					if (inst.getYear() != SimpleChapterUtils.UNDEFINED_YEAR) {
						return Integer.toString(inst.getYear());
					} else {
						return "";
					}
				}
			}
			
		};
		return instrumentBPIterator;
	}
	
	public String cleanInstrNo(String instno, int year){
		return instno;
	}
	
	public InstrumentGenericIterator getInstrumentIterator(boolean instrumentType) {
		InstrumentGenericIterator instrumentGenericIterator = new InstrumentGenericIterator(searchId, getDataSite()) {

			private static final long serialVersionUID = 5399351945130601258L;

			@Override
			protected String cleanInstrumentNo(String instno, int year) {
				return cleanInstrNo(instno, year);

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
	
	public InstrumentGenericIterator getInstrumentIteratorForImageFakeDocs(boolean instrumentType) {
		InstrumentGenericIterator instrumentGenericIterator = new InstrumentGenericIterator(searchId, getDataSite()) {

			private static final long serialVersionUID = 5399351945130601258L;

			@Override
			protected String cleanInstrumentNo(String instno, int year) {
				return cleanInstrNo(instno, year);
			}
			
			@Override
			protected void useInstrumentI(List<InstrumentI> result,	HashSet<String> listsForNow, DocumentsManagerI manager,
												InstrumentI instrumentI) {
				if (!isLoadFromRoLike()){
					instrumentI.setDocType(DocumentTypes.MISCELLANEOUS);
					instrumentI.setDocSubType(DocumentTypes.MISCELLANEOUS);
				}
				if (isEnableInstrumentNumber()) {
					processEnableInstrumentNo(result, listsForNow, manager, instrumentI);
				}
				if (isEnableDocumentNumber()) {
					processEnableDocumentNumber(result, listsForNow, manager, instrumentI);
				}
				if (isEnableBookPage()) {
					processEnableBP(result, listsForNow, manager, instrumentI);
				}
			}

			@Override
			public String getYearFrom(InstrumentI state, HashMap<String, String> filterCriteria) {
				Date recDate = state.getDate();
				if (recDate != null){
					return new SimpleDateFormat(FormatDate.PATTERN_MM_SLASH_DD_SLASH_YYYY).format(recDate);
				} else if (state.getYear() != SimpleChapterUtils.UNDEFINED_YEAR) {
					return Integer.toString(state.getYear());
				}
				
				return "";
			}
			
			@Override
			protected List<DocumentI> getDocumentsWithInstrumentsFlexible(DocumentsManagerI manager, InstrumentI instrumentI) {
				return checkFlexibleInclusion(instrumentI, manager, false);
			}
		};
		if (instrumentType) {
			instrumentGenericIterator.enableInstrumentNumber();
		} else {
			instrumentGenericIterator.enableBookPage();
			instrumentGenericIterator.setRemoveLeadingZerosBP(true);
		}
		instrumentGenericIterator.setCheckIfWasAlreadyInvalidated(true);
		return instrumentGenericIterator;
	}
	
	private List<DocumentI> checkFlexibleInclusion(InstrumentI documentToCheck, DocumentsManagerI documentManager, boolean checkDocType) {
		
		InstrumentI documentToCheckCopy = documentToCheck.clone();
//		String doctype = documentToCheckCopy.getDocType();
//		if (StringUtils.isNotEmpty(doctype)){
//			documentToCheckCopy.setDocType(DocumentTypes.getDocumentCategory(doctype, searchId));
//			documentToCheckCopy.setDocSubType(DocumentTypes.getDocumentSubcategory(doctype, searchId));
//		}
		List<DocumentI> almostLike = documentManager.getDocumentsWithInstrumentsFlexible(false, documentToCheckCopy);
		
		List<DocumentI> alike = new ArrayList<DocumentI>();
		
		if (almostLike != null && !almostLike.isEmpty()) {
			
			return almostLike;
			
		} else {
			if (checkDocType) {
				return almostLike;
			}
			String bookToCheck = org.apache.commons.lang.StringUtils.defaultString(documentToCheck.getBook());
			
			List<DocumentI> allRODocuments = documentManager.getDocumentsWithDataSource(false, getDataSite().getSiteTypeAbrev());
				
			for (DocumentI documentI : allRODocuments) {
				if (org.apache.commons.lang.StringUtils.isNotEmpty(documentI.getBook()) && org.apache.commons.lang.StringUtils.isNotEmpty(bookToCheck) 
							&& documentI.getBook().equals(bookToCheck)
					   && org.apache.commons.lang.StringUtils.isNotEmpty(documentI.getPage()) && org.apache.commons.lang.StringUtils.isNotEmpty(documentToCheck.getPage()) 
					   		&& documentI.getPage().equals(documentToCheck.getPage())) {
					
					alike.add(documentI);
				} else if (documentI.getInstno().equals(documentToCheck.getInstno())){
					alike.add(documentI);
				}
			}
		}
		return alike;
	}
	
	@Override
	protected DownloadImageResult saveImage(ImageI image)throws ServerResponseException {
		HashMap<String, String> map = HttpUtils.getParamsFromLink( image.getLink(0) );
		
		String book 	 = map.get("book") ;
		String page 	 = map.get("page") ;
		String docNumber = map.get("instr");
		String year 	 = map.get("year") ;
		String type 	 = org.apache.commons.lang.StringUtils.defaultIfEmpty(map.get("type"), "Miscellaneous") ;	
    	String month 	 = org.apache.commons.lang.StringUtils.defaultString(map.get("month"));
    	String day 		 = org.apache.commons.lang.StringUtils.defaultString(map.get("day"));
		String dataTreeIndexStr = org.apache.commons.lang.StringUtils.defaultString(map.get("dataTreeIndex"));
    	String dataTreeDesc =  org.apache.commons.lang.StringUtils.defaultString(map.get("dataTreeDesc"));
		
    	int dataTreeIndex = -1;
    	try{dataTreeIndex = Integer.parseInt(dataTreeIndexStr);}catch(Exception e){};
    	int commId = getSearch().getCommId();
    	boolean savedImage = false;
    	
    	int yearInt = -1;
    	try {
			if(getDataSite().getCountyId() == CountyConstants.NV_Clark ) {
				
				String aomBook = map.get("aomBook");
				String aomPage = map.get("aomPage");
				String aomType = map.get("aomType");
				
				if(StringUtils.isNotEmpty(aomBook) && StringUtils.isNotEmpty(aomPage) && StringUtils.isNotEmpty(aomType)) {
					try {
						long siteIdAom = TSServersFactory.getSiteId(
								getSearch().getSa().getAtribute(SearchAttributes.P_STATE_ABREV), 
								getSearch().getSa().getAtribute(SearchAttributes.P_COUNTY_NAME), 
								"AOM");
						NVClarkAOM nvClarkAOM = (NVClarkAOM)TSServersFactory.GetServerInstance((int)siteIdAom, getSearch().getID());
						
						if (nvClarkAOM.getDataSite().isEnableSite(commId)) {
							
							MapType mapType = null;
							
							try {
								mapType = MapType.valueOf(aomType);
							} catch (IllegalArgumentException illegalArgumentException) {
							}
							
							if(mapType != null) {
							
								NvClarkAOMConn conn = new NvClarkAOMConn();
								
								byte[] imageBytes = null;
								
								try{imageBytes = conn.downloadMap(aomBook, aomPage, mapType);}catch(Exception e){}
								boolean imageDownloaded = imageBytes!=null && imageBytes.length>0;
								if (imageDownloaded) {
									return new DownloadImageResult(DownloadImageResult.Status.OK, imageBytes, "application/pdf");
								} else {
									return new DownloadImageResult(Status.ERROR, new byte[0], image.getContentType());
								}
							}
						}
					} catch (Exception e) {
						logger.error("Something happened while tring to get image from NVClarkAOM while on NVClarkDG", e);
					}
				}
				
				yearInt = Integer.parseInt(year);
				
				if (yearInt != SimpleChapterUtils.UNDEFINED_YEAR){
					long siteId = TSServersFactory.getSiteId(
							getSearch().getSa().getAtribute(SearchAttributes.P_STATE_ABREV), 
							getSearch().getSa().getAtribute(SearchAttributes.P_COUNTY_NAME), 
							"IM");
					NVClarkIM nvClarkIM = (NVClarkIM)TSServersFactory.GetServerInstance((int)siteId, getSearch().getID());
					
					if( nvClarkIM.getDataSite().isEnableSite(commId) && nvClarkIM.getDataSite().isEnabledIncludeInTsr(commId)) {
						
						SearchDataWrapper searchDataWrapper = new SearchDataWrapper();
						TSServerInfoModule module = nvClarkIM.getCurrentClassServerInfo().getModuleForSearch(
								TSServerInfo.INSTR_NO_MODULE_IDX, 
								searchDataWrapper);
						module.addExtraInformation(TSServerInfoConstants.EXTRA_PARAM_DO_NOT_REMOVE_IN_MEMORY_DOCS, true);
						module.setData(1, docNumber);
						if(!Character.isLetter(docNumber.charAt(0))) {
							if (!StringUtils.isEmpty(year) && !StringUtils.isEmpty(month) && !StringUtils.isEmpty(day) && 
									year.length()>=month.length()+day.length()+2 && year.endsWith(month+day)) {
								module.setData(0, year);
							} else {
								module.setData(0, year + month + day);
							}
						}
						searchDataWrapper.setImage(image);
						ServerResponse serverResponse = nvClarkIM.SearchBy(module, searchDataWrapper);
						DownloadImageResult imageResult = serverResponse.getImageResult();
						if(imageResult != null) {
							if(imageResult.getStatus() == DownloadImageResult.Status.OK) {
								/*
								 * At this point the image is downloaded, written to disk and counted 
								 * in ro.cst.tsearch.servers.types.TSServer.solveBinaryResponse
								 */
								return imageResult;
							}
						}
					}
				}
			}
		} catch (Exception e) {
			logger.error("Error while saving image from NVClarIM", e);
		}

		if (datTreeList == null){
			datTreeList = initDataTreeStruct();
		}
		
		InstrumentI i = new Instrument();
		i.setBook(book);
		i.setPage(page);
		i.setInstno(docNumber);
		i.setDocType(type);
		
		if (yearInt != SimpleChapterUtils.UNDEFINED_YEAR){
			i.setYear(yearInt);
		}
		
		if (dataTreeIndex>0){
			DataTreeAccount acc = DataTreeManager.getDatatTreeAccount(String.valueOf(getCommunityId()));
			try {
				SimpleImage im = null;
				try {
					im = DataTreeConn.retrieveImage(acc, dataTreeIndex , dataTreeDesc, 999, 0);
				} catch (DataTreeImageException e) {
					e.printStackTrace();
					SearchLogger.info(
							"<br/>FAILED to take Image(searchId=" + searchId + " ) book=" + i.getBook() + " page=" + i.getPage() + 
							" inst=" + i.getInstno() + " from DataTree. " +
							"Official Server Message: [" + e.getLocalizedMessage() + " (" + e.getStatus() + ") ]<br/>", searchId);
				}
				if(im!=null){
					FileUtils.writeByteArrayToFile(new File(image.getPath()),im.getContent());
					logger.info("------------------------ downloadImageFromDataTree return true for instr=" + i + " savePath=" + image.getPath());
					savedImage =  true;
				}
			} catch (IOException e) {
				e.printStackTrace();
			}
		}else{
			try {
				savedImage = FLGenericDASLDT.downloadImageFromDataTree(i, datTreeList, image.getPath(), String.valueOf(getCommunityId()), month, day);
			} catch (DataTreeImageException e) {
				logger.error("Error while getting image ", e);
				SearchLogger.info(
						"<br/>FAILED to take Image(searchId=" + searchId + " ) book=" +
						i.getBook() + " page=" + i.getPage() + " inst=" +
						i.getInstno() + " from DataTree. " +
						"Official Server Message: [" + e.getLocalizedMessage() + " (" + e.getStatus() + ") ]<br/>", searchId);
			}
		}
		
		if (savedImage) {
			SearchLogger.info("<br/>Image(searchId=" + searchId + " )book=" + i.getBook() + "page=" + i.getPage() 
								+ "inst=" + i.getInstno()+ " was taken from DataTree<br/>", searchId);
		}
		afterDownloadImage(savedImage, GWTDataSite.DG_TYPE);
		
		byte[] b = new byte[0];
		if (new File(image.getPath()).exists()){
			try {
				b = FileUtils.readFileToByteArray(new File(image.getPath()));
			} catch (IOException e) {e.printStackTrace();}
		}
		if(b.length>0){
			return new DownloadImageResult(Status.OK, b , image.getContentType());
		}
		
		return new DownloadImageResult(Status.ERROR, b , image.getContentType());
    }
}
