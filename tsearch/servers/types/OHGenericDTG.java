package ro.cst.tsearch.servers.types;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.math.BigDecimal;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.StringUtils;

import ro.cst.tsearch.Search;
import ro.cst.tsearch.bean.SearchAttributes;
import ro.cst.tsearch.community.Products;
import ro.cst.tsearch.data.CountyConstants;
import ro.cst.tsearch.datatrace.DTRecord;
import ro.cst.tsearch.exceptions.ServerResponseException;
import ro.cst.tsearch.search.FunctionStatesIterator;
import ro.cst.tsearch.search.GenericRuntimeIterator;
import ro.cst.tsearch.search.filter.BetweenDatesFilterResponse;
import ro.cst.tsearch.search.filter.FilterResponse;
import ro.cst.tsearch.search.filter.RejectAlreadySavedDocumentsFilterResponse;
import ro.cst.tsearch.search.filter.matchers.algorithm.LotInterval;
import ro.cst.tsearch.search.filter.matchers.algorithm.LotMatchAlgorithm;
import ro.cst.tsearch.search.filter.newfilters.date.LastTransferDateFilter;
import ro.cst.tsearch.search.filter.newfilters.doctype.DocTypeAdvancedFilter;
import ro.cst.tsearch.search.filter.newfilters.doctype.DocTypeSimpleFilter;
import ro.cst.tsearch.search.filter.newfilters.doctype.DoctypeFilterFactory;
import ro.cst.tsearch.search.filter.newfilters.legal.GenericLegal;
import ro.cst.tsearch.search.filter.newfilters.legal.GenericMultipleLegalFilter;
import ro.cst.tsearch.search.filter.newfilters.legal.LegalFilterFactory;
import ro.cst.tsearch.search.filter.newfilters.name.GenericNameFilter;
import ro.cst.tsearch.search.filter.newfilters.name.NameFilterFactory;
import ro.cst.tsearch.search.filter.newfilters.pin.PinFilterResponse;
import ro.cst.tsearch.search.iterator.ParcelIdIterator;
import ro.cst.tsearch.search.iterator.data.LegalStruct;
import ro.cst.tsearch.search.iterator.data.LegalStructDTG;
import ro.cst.tsearch.search.iterator.instrument.InstrumentGenericIterator;
import ro.cst.tsearch.search.iterator.legal.LegalDescriptionIteratorI;
import ro.cst.tsearch.search.util.AdditionalInfoKeys;
import ro.cst.tsearch.servers.HashCountyToIndex;
import ro.cst.tsearch.servers.TSConnectionURL;
import ro.cst.tsearch.servers.bean.DataSite;
import ro.cst.tsearch.servers.info.TSServerInfo;
import ro.cst.tsearch.servers.info.TSServerInfoConstants;
import ro.cst.tsearch.servers.info.TSServerInfoModule;
import ro.cst.tsearch.servers.types.FLSubdividedBasedDASLDT.HasLotFilter;
import ro.cst.tsearch.servers.types.FLSubdividedBasedDASLDT.PropertyTypeFilter;
import ro.cst.tsearch.servers.types.TSInterface.DownloadImageResult.Status;
import ro.cst.tsearch.templates.MultilineElementsMap;
import ro.cst.tsearch.titledocument.abstracts.DocumentTypes;
import ro.cst.tsearch.utils.CurrentInstance;
import ro.cst.tsearch.utils.FormatDate;
import ro.cst.tsearch.utils.HttpUtils;
import ro.cst.tsearch.utils.InstanceManager;
import ro.cst.tsearch.utils.SearchLogger;

import com.stewart.ats.base.document.DocumentI;
import com.stewart.ats.base.document.DocumentUtils;
import com.stewart.ats.base.document.ImageI;
import com.stewart.ats.base.document.Instrument;
import com.stewart.ats.base.document.InstrumentI;
import com.stewart.ats.base.document.RegisterDocumentI;
import com.stewart.ats.base.legal.LegalI;
import com.stewart.ats.base.legal.SubdivisionI;
import com.stewart.ats.base.legal.TownShipI;
import com.stewart.ats.base.name.NameI;
import com.stewart.ats.base.parties.PartyI;
import com.stewart.ats.base.property.PropertyI;
import com.stewart.ats.base.search.DocumentsManagerI;
import com.stewart.ats.tsrindex.client.SimpleChapterUtils;
import com.stewart.datatree.DataTreeAccount;
import com.stewart.datatree.DataTreeConn;
import com.stewart.datatree.DataTreeImageException;
import com.stewart.datatree.DataTreeManager;
import com.stewart.datatree.SimpleImage;

/**
 * @author cristian stochina
 */
public class OHGenericDTG extends TSServerDTG{

	private static final long serialVersionUID = 3024488184071095461L;
	private static final String OH_MULTIPLE_LEGAL_INSTR = "OH_MULTIPLE_LEGAL_INSTR";

	public OHGenericDTG(String rsRequestSolverName, String rsSitePath,
			String rsServerID, String rsPrmNameLink, long searchId, int mid)
			throws FileNotFoundException, IOException {
		super(rsRequestSolverName, rsSitePath, rsServerID, rsPrmNameLink,
				searchId, mid);
	}

	public OHGenericDTG(long searchId) {
		super(searchId);
	}
	
	@Override
	public void setModulesForAutoSearch(TSServerInfo serverInfo) {

		Search global = InstanceManager.getManager().getCurrentInstance(searchId).getCrtSearchContext();
		List<TSServerInfoModule> modules = new ArrayList<TSServerInfoModule>();

		if (global.getSearchType() == Search.AUTOMATIC_SEARCH) {

			boolean isUpdate = (isUpdate() || global.getSa().isDateDown());
			boolean isTimeShare = !StringUtils.isEmpty(global.getSa().getAtribute(SearchAttributes.WEEK));

			FilterResponse legalOrDocTypeFilter = FLSubdividedBasedDASLDT.getLegalOrDocTypeFilter(searchId, false);
			PinFilterResponse pinFilter = FLSubdividedBasedDASLDT.getPinFilter(searchId, true);

			String block = global.getSa().getAtribute(SearchAttributes.LD_SUBDIV_BLOCK);
			String lot = global.getSa().getAtribute(SearchAttributes.LD_LOTNO);

			TSServerInfoModule module = null;
			
			GenericMultipleLegalFilter multipleLegalFilter = new GenericMultipleLegalFilter(searchId);
			multipleLegalFilter.setUseLegalFromSearchPage(true);
			multipleLegalFilter.setMarkIfCandidatesAreEmpty(true);
			multipleLegalFilter.setThreshold(new BigDecimal(0.7));
			multipleLegalFilter.setAdditionalInfoKey(AdditionalInfoKeys.DG_LOOK_UP_DATA);

			GenericNameFilter nameFilterOwner = (GenericNameFilter) NameFilterFactory.getDefaultNameFilter(SearchAttributes.OWNER_OBJECT, global.getID(), null);
			nameFilterOwner.setUseSynonymsBothWays(true);
			nameFilterOwner.setIgnoreMiddleOnEmpty(true);
			nameFilterOwner.setInitAgain(true);

			FilterResponse[] filtersO = { nameFilterOwner, legalOrDocTypeFilter, pinFilter, new LastTransferDateFilter(searchId),
					new PropertyTypeFilter(searchId) };
			List<FilterResponse> filtersOList = Arrays.asList(filtersO);

			FilterResponse[] filtersCrossrefs = { legalOrDocTypeFilter, pinFilter, new PropertyTypeFilter(searchId) };

			InstrumentGenericIterator instrumentGenericIterator = null;
			InstrumentGenericIterator bpGenericIterator = null;
			{
				instrumentGenericIterator = getInstrumentOrBookTypeIterator(true);

				module = new TSServerInfoModule(serverInfo.getModule(TSServerInfo.INSTR_NO_MODULE_IDX));
				module.addExtraInformation(TSServerInfoConstants.EXTRA_PARAM_MODULE_DESCRIPTION, "Search with references from AO/Tax like documents");
				module.setIteratorType(0, FunctionStatesIterator.ITERATOR_TYPE_INSTRUMENT_LIST_FAKE);
				module.setIteratorType(1, FunctionStatesIterator.ITERATOR_TYPE_YEAR);
				if (isUpdate) {
					module.addFilter(new BetweenDatesFilterResponse(searchId));
				}
				module.addIterator(instrumentGenericIterator);
				if (!lookupWithInstrument) {
					lookupWithInstrument = !instrumentGenericIterator.createDerrivations().isEmpty();
				}
				modules.add(module);
			}
			{
				bpGenericIterator = getInstrumentOrBookTypeIterator(false);
				bpGenericIterator.setRemoveLeadingZerosBP(true);

				module = new TSServerInfoModule(serverInfo.getModule(TSServerInfo.BOOK_AND_PAGE_MODULE_IDX));
				module.addExtraInformation(TSServerInfoConstants.EXTRA_PARAM_MODULE_DESCRIPTION, "Search with references from AO/Tax like documents");
				module.setIteratorType(0, FunctionStatesIterator.ITERATOR_TYPE_BOOK_FAKE);
				module.setIteratorType(1, FunctionStatesIterator.ITERATOR_TYPE_PAGE_FAKE);

				if (isUpdate) {
					module.addFilter(new BetweenDatesFilterResponse(searchId));
				}
				module.addIterator(bpGenericIterator);
				if (!lookupWithInstrument) {
					lookupWithInstrument = !bpGenericIterator.createDerrivations().isEmpty();
				}
				modules.add(module);
			}

			if (StringUtils.isNotEmpty(lot) || StringUtils.isNotEmpty(block)) {
				List<FilterResponse> grantorGranteeFilterList = new ArrayList<FilterResponse>();

				grantorGranteeFilterList.add(nameFilterOwner);
				grantorGranteeFilterList.add(legalOrDocTypeFilter);
				grantorGranteeFilterList.add(pinFilter);
				grantorGranteeFilterList.add(new LastTransferDateFilter(searchId));
				grantorGranteeFilterList.add(new PropertyTypeFilter(searchId));
				grantorGranteeFilterList.add(new HasLotFilter("", searchId));

				super.addGrantorGranteeSearch(modules, serverInfo, SearchAttributes.OWNER_OBJECT, grantorGranteeFilterList);
			}
			
			addIteratorModule(serverInfo, modules, TSServerInfo.SUBDIVISION_MODULE_IDX, searchId, isUpdate, isTimeShare);

			if (!isTimeShare) {
				addIteratorModule(serverInfo, modules, TSServerInfo.ARB_MODULE_IDX, searchId, isUpdate, isTimeShare);

				addIteratorModule(serverInfo, modules, TSServerInfo.SECTION_LAND_MODULE_IDX, searchId, isUpdate, isTimeShare);

				module = new TSServerInfoModule(serverInfo.getModule(TSServerInfo.PARCEL_ID_MODULE_IDX));
				module.clearSaKeys();
				module.setIteratorType(0, FunctionStatesIterator.ITERATOR_TYPE_PARCELID_FAKE);
				ParcelIdIterator it = new ParcelIdIterator(searchId){
					private static final long serialVersionUID = 2750089650252801002L;
	
					@Override
					protected String preparePin(String pin) {
						return preparePid(pin);
					}
				};
				if (isUpdate) {
					module.addFilter(new BetweenDatesFilterResponse(searchId));
				}
				if (pinFilter != null){
					module.addFilter(pinFilter);
				}
				module.addIterator(it);
				modules.add(module);
				
				ArrayList<NameI> searchedNames = super.addNameSearch(modules, serverInfo, SearchAttributes.OWNER_OBJECT, null, filtersOList);

				super.addOCRSearch(modules, serverInfo, legalOrDocTypeFilter);

				super.addNameSearch(modules, serverInfo, SearchAttributes.OWNER_OBJECT, searchedNames == null ? new ArrayList<NameI>() : searchedNames,
						filtersOList);

			} else {
				List<FilterResponse> filtersOListTS = new ArrayList<FilterResponse>();
				filtersOListTS.add(new DocTypeAdvancedFilter(searchId));
				filtersOListTS.addAll(filtersOList);

				ArrayList<NameI> searchedNames = super.addNameSearch(modules, serverInfo, SearchAttributes.OWNER_OBJECT, null, filtersOListTS);

				addOCRSearch(modules, serverInfo, legalOrDocTypeFilter);

				super.addNameSearch(modules, serverInfo, SearchAttributes.OWNER_OBJECT, searchedNames == null ? new ArrayList<NameI>() : searchedNames,
						filtersOListTS);
			}

			{
				GenericNameFilter nameFilterBuyer = (GenericNameFilter) NameFilterFactory
																.getDefaultNameFilter(SearchAttributes.BUYER_OBJECT, global.getID(), null);
				nameFilterBuyer.setIgnoreMiddleOnEmpty(true);
				nameFilterBuyer.setUseSynonymsBothWays(true);
				nameFilterBuyer.setInitAgain(true);
	
				List<FilterResponse> buyerFilterList = new ArrayList<FilterResponse>();
				buyerFilterList.add(nameFilterBuyer);
				if (getSearch().getProductId() == Products.FULL_SEARCH_PRODUCT){
					DocTypeSimpleFilter doctypeBuyerFilter = (DocTypeSimpleFilter) new DoctypeFilterFactory().getDoctypeBuyerFilter(searchId);
					doctypeBuyerFilter.setDocTypes(new String[]{DocumentTypes.LIEN});
					doctypeBuyerFilter.setUseSubCategory(true);
					doctypeBuyerFilter.setSubDocTypes(new String[]{DocumentTypes.SUBCATEGORY_FEDERAL_TAX_LIEN, 
																	DocumentTypes.SUBCATEGORY_CHILD_SUPPORT_LIEN}
													);
					buyerFilterList.add(doctypeBuyerFilter);
				} else{
					DocTypeAdvancedFilter doctypeBuyerFilter = new DoctypeFilterFactory().getDoctypeFilterForGeneralIndexBuyerNameSearch(searchId);
					buyerFilterList.add(doctypeBuyerFilter);
				}
				
				super.addNameSearch(modules, serverInfo, SearchAttributes.BUYER_OBJECT, null, buyerFilterList);
			}
			
			GenericRuntimeIterator<InstrumentI> instrumentIterator = super.getInstrumentIterator(false);

			if (instrumentIterator != null) {
				final int[] REFERECE_IDXS = { TSServerInfo.INSTR_NO_MODULE_IDX, TSServerInfo.BOOK_AND_PAGE_MODULE_IDX };
				for (int moduleIdx : REFERECE_IDXS) {
					module = new TSServerInfoModule(serverInfo.getModule(moduleIdx));
					module.addExtraInformation(TSServerInfoConstants.EXTRA_PARAM_MODULE_DESCRIPTION, "Search with references from RO-like documents");
					module.clearSaKeys();
					module.addFilter(multipleLegalFilter);
					module.addIterator(super.getInstrumentIterator(false));
					for (FilterResponse filter : filtersCrossrefs) {
						module.addFilter(filter);
					}
					if (isUpdate) {
						module.addFilter(new BetweenDatesFilterResponse(searchId));
					}
					modules.add(module);
				}
			}
			
			//plat map fake search
			module = new TSServerInfoModule(serverInfo.getModule(TSServerInfo.BOOK_AND_PAGE_LOCAL_MODULE_IDX));
			module.setSaObjKey(SearchAttributes.LD_BOOKNO);
			module.addExtraInformation(TSServerInfoConstants.EXTRA_PARAM_MODULE_DESCRIPTION, "Search for PLAT document");
			module.clearSaKeys();
			PlatIterator it = getPlatBookPageIterator(searchId);
			module.addIterator(it);
			if (!isUpdate) {
				modules.add(module);
			}
			
			//parcel map fake search
			module = new TSServerInfoModule(serverInfo.getModule(TSServerInfo.BOOK_AND_PAGE_LOCAL_MODULE_IDX));
			module.setSaObjKey(SearchAttributes.LD_PARCELNO_MAP);
			module.addExtraInformation(TSServerInfoConstants.EXTRA_PARAM_MODULE_DESCRIPTION, "Search for PLAT document");
			module.clearSaKeys();
			it = getPlatBookPageIterator(searchId);
			module.addIterator(it);
			if (!isUpdate) {
				modules.add(module);
			}
			
			//parcel map fake search
			module = new TSServerInfoModule(serverInfo.getModule(TSServerInfo.BOOK_AND_PAGE_LOCAL_MODULE_IDX));
			module.setSaObjKey(SearchAttributes.LD_PARCELNO_CONDO);
			module.addExtraInformation(TSServerInfoConstants.EXTRA_PARAM_MODULE_DESCRIPTION, "Search for PLAT document");
			module.clearSaKeys();
			it = getPlatBookPageIterator(searchId);
			module.addIterator(it);
			if (!isUpdate) {
				modules.add(module);
			}
			
			//assessor map fake search
			module = new TSServerInfoModule(serverInfo.getModule(TSServerInfo.BOOK_AND_PAGE_LOCAL_MODULE_IDX));
			module.setSaObjKey(SearchAttributes.LD_BOOKNO_1);
			module.addExtraInformation(TSServerInfoConstants.EXTRA_PARAM_MODULE_DESCRIPTION, "Search for PLAT document");
			module.clearSaKeys();
			it = getPlatBookPageIterator(searchId);
			module.addIterator(it);
			if (!isUpdate) {
				modules.add(module);
			}
			
			//subdivision map fake search
			module = new TSServerInfoModule(serverInfo.getModule(TSServerInfo.BOOK_AND_PAGE_LOCAL_MODULE_IDX));
			module.setSaObjKey(SearchAttributes.LD_SUBDIVISION);
			module.addExtraInformation(TSServerInfoConstants.EXTRA_PARAM_MODULE_DESCRIPTION, "Search for PLAT document");
			module.clearSaKeys();
			it = getPlatBookPageIterator(searchId);
			module.addIterator(it);
			if (!isUpdate) {
				modules.add(module);
			}
			
			addRelatedSearch(serverInfo, modules);
			
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
	
	protected String preparePid(String pin) {
		
		return pin;
	}
	
	@Override
	public void addIteratorModule(TSServerInfo serverInfo, List<TSServerInfoModule> modules, int code, long searchId, boolean isUpdate, boolean isTimeShare) {

		TSServerInfoModule module = new TSServerInfoModule(serverInfo.getModule(code));
		module.clearSaKeys();

		LegalDescriptionIterator it = getLegalDescriptionIterator(true);

		module.addIterator(it);
		if (isUpdate) {
			module.addFilter(new BetweenDatesFilterResponse(searchId));
		}
		PinFilterResponse pinFilter = FLSubdividedBasedDASLDT.getPinFilter(searchId, true);
		if (pinFilter != null) {
			module.addFilter(pinFilter);
		}

		if (isTimeShare) {
			module.addFilter(new FLSubdividedBasedDASLDT.IncompleteLegalFilter("", searchId));
		}
		if (code == TSServerInfo.SECTION_LAND_MODULE_IDX) {
			GenericNameFilter nameFilterOwner = (GenericNameFilter) NameFilterFactory.getDefaultNameFilter(SearchAttributes.OWNER_OBJECT, searchId, null);
			nameFilterOwner.setInitAgain(true);
			nameFilterOwner.setUseSynonymsBothWays(false);
			nameFilterOwner.setIgnoreMiddleOnEmpty(true);
			module.addFilter(nameFilterOwner);
		}

		Search search = InstanceManager.getManager().getCurrentInstance(searchId).getCrtSearchContext();

		if (StringUtils.isNotEmpty(search.getSa().getAtribute(SearchAttributes.LD_SUBLOT))) {
			try {

				RejectAlreadySavedDocumentsFilterResponse rejectAlreadySavedFilter = new RejectAlreadySavedDocumentsFilterResponse(searchId) {

					private static final long serialVersionUID = -7039182937380487641L;

					@Override
					protected InstrumentI formatInstrument(InstrumentI instr) {
						super.formatInstrument(instr);
						return instr;
					}
				};
				module.addFilter(rejectAlreadySavedFilter);
				module.addFilter(LegalFilterFactory.getDefaultSubLotFilter(searchId));
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		modules.add(module);
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
		InstrumentGenericIterator instrumentRelatedNoIterator = getInstrumentOrBookTypeIterator(true);
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
		InstrumentGenericIterator instrumentBPIterator = new InstrumentGenericIterator(searchId);
		return instrumentBPIterator;
	}

	private LegalDescriptionIterator getLegalDescriptionIterator(boolean lookupWasDoneWithName) {
		LegalDescriptionIterator it = new LegalDescriptionIterator(searchId);
		
		it.setLoadFromSearchPage(false);
		it.setLoadFromSearchPageIfNoLookup(true);
		return it;
	}
	
	class LegalDescriptionIterator extends GenericRuntimeIterator<LegalStructDTG> implements LegalDescriptionIteratorI{

		public LegalDescriptionIterator(long searchId) {
			super(searchId);
			setDataSite(OHGenericDTG.this.getDataSite());
		}

		private static final long serialVersionUID = -4741635379234782109L;

		private boolean loadFromSearchPage = true;
		private boolean loadFromSearchPageIfNoLookup = false;
		
		protected List<LegalStructDTG> createDerrivations() {

			Search global = InstanceManager.getManager().getCurrentInstance(searchId).getCrtSearchContext();
			DocumentsManagerI m = global.getDocManager();
			Set<LegalStructDTG> legalStructList = (Set<LegalStructDTG>) global.getAdditionalInfo(AdditionalInfoKeys.DG_LOOK_UP_DATA);
			Set<String> multipleLegals = (Set<String>) global.getAdditionalInfo(OH_MULTIPLE_LEGAL_INSTR);
			if (multipleLegals == null) {
				multipleLegals = new HashSet<String>();
			}

			String platBookAO = global.getSa().getAtribute(SearchAttributes.LD_BOOKNO);
			String platPageAO = global.getSa().getAtribute(SearchAttributes.LD_PAGENO);

			String subLotNo = global.getSa().getAtribute(SearchAttributes.LD_SUBLOT);

			String aoAndTrLots = global.getSa().getAtribute(SearchAttributes.LD_LOTNO);
			HashSet<String> allAoAndTrlots = new HashSet<String>();

			String aoAndTrBloks = global.getSa().getAtribute(SearchAttributes.LD_SUBDIV_BLOCK);
			HashSet<String> allAoAndTrBloks = new HashSet<String>();

			String week = global.getSa().getAtribute(SearchAttributes.WEEK);
			boolean isTimeShare = !StringUtils.isEmpty(week);

			if (aoAndTrLots.contains(",") || aoAndTrLots.contains(" ") || aoAndTrLots.contains("-") || aoAndTrLots.contains(";")) {
				if (!StringUtils.isEmpty(aoAndTrLots)) {
					for (LotInterval interval : LotMatchAlgorithm.prepareLotInterval(aoAndTrLots)) {
						allAoAndTrlots.addAll(interval.getLotList());
					}
				}
			} else {
				if (!StringUtils.isEmpty(aoAndTrLots)) {
					allAoAndTrlots.add(aoAndTrLots);
				}
			}
			if (!StringUtils.isEmpty(aoAndTrBloks)) {
				for (LotInterval interval : LotMatchAlgorithm.prepareLotInterval(aoAndTrBloks)) {
					allAoAndTrBloks.addAll(interval.getLotList());
				}
			}


			boolean first = false;
			if (legalStructList == null) {
				first = true;
				legalStructList = new HashSet<LegalStructDTG>();

				try {
					m.getAccess();
					List<RegisterDocumentI> listRodocs = getGoodDocumentsOrForCurrentOwner(m, global, true, 35, dataSite.getStateAbbreviation());

					if (listRodocs == null || listRodocs.size() == 0) {
						listRodocs = getGoodDocumentsOrForCurrentOwner(m, global, false, 35, dataSite.getStateAbbreviation());
					}
					if (listRodocs == null || listRodocs.size() == 0) {
						for (DocumentI doc : m.getDocumentsWithDataSource(true, dataSite.getSiteTypeAbrev())) {
							if (doc instanceof RegisterDocumentI) {
								listRodocs.add((RegisterDocumentI) doc);
							}
						}
					}

					DocumentUtils.sortDocuments(listRodocs, MultilineElementsMap.DATE_ORDER_DESC);

					boolean atLeastOneFromDg = false;

					for (RegisterDocumentI reg : listRodocs) {
						if (!reg.isOneOf(DocumentTypes.PLAT, DocumentTypes.RESTRICTION, DocumentTypes.EASEMENT, DocumentTypes.MASTERDEED,
								DocumentTypes.COURT, DocumentTypes.LIEN, DocumentTypes.CORPORATION, DocumentTypes.AFFIDAVIT, DocumentTypes.CCER)) {

							List<LegalStructDTG> tempLegalStructListPerDocument = new ArrayList<LegalStructDTG>();
							for (PropertyI prop : reg.getProperties()) {
								if (prop.hasLegal()) {
									LegalI legal = prop.getLegal();

									if (legal.hasSubdividedLegal()) {

										LegalStructDTG legalStructItem = new LegalStructDTG(SUBDIVIDED_TYPE);
										SubdivisionI subdiv = legal.getSubdivision();

										String block = subdiv.getBlock();
										String lot = subdiv.getLot();
										String lotThru = subdiv.getLotThrough();
										String subLot = subdiv.getSubLot();
										String platBook = subdiv.getPlatBook();
										String platPage = subdiv.getPlatPage();
										String platInst = subdiv.getPlatInstrument();
										String unit = subdiv.getUnit();
										legalStructItem.setUnit(StringUtils.isEmpty(unit) ? "" : unit);
										legalStructItem.setSubLot(org.apache.commons.lang.StringUtils.defaultString(subLot));
										legalStructItem.setBlock(StringUtils.isEmpty(block) ? "" : block);
										legalStructItem.setLotThru(StringUtils.isEmpty(lotThru) ? "" : lotThru);
										legalStructItem.setPlatBook(StringUtils.isEmpty(platBook) ? "" : platBook);
										legalStructItem.setPlatPage(StringUtils.isEmpty(platPage) ? "" : platPage);
										legalStructItem.setPlatInst(StringUtils.isEmpty(platInst) ? "" : platInst);
										legalStructItem.setPlatInstrYear(subdiv.getPlatInstrumentYear());

										if (StringUtils.isEmpty(lot)) {
											if (!StringUtils.isEmpty(unit)) {
												legalStructItem.setLot(unit);
												if (!unit.startsWith("U")) {
													LegalStructDTG legalStructItemA = null;
													try {
														legalStructItemA = (LegalStructDTG) legalStructItem.clone();
													} catch (CloneNotSupportedException e) {
														e.printStackTrace();
													}
													if (legalStructItemA != null) {
														legalStructItemA.setLot("U" + unit);
														preparePersonalStructForCounty(legalStructItemA, searchId);
														multipleLegals.add(reg.prettyPrint());
														tempLegalStructListPerDocument.add(legalStructItemA);
													}
												}
											}
										} else {
											lot = StringUtils.stripStart(lot, "0");
											legalStructItem.setLot(lot);
										}

										if (StringUtils.isEmpty(legalStructItem.getLot())) {// try to complete with the unit from search page
											String unitFromSearchPage = global.getSa().getAtribute(SearchAttributes.LD_SUBDIV_UNIT);
											if (!StringUtils.isEmpty(unitFromSearchPage)) {
												unitFromSearchPage = unitFromSearchPage.replace("#", "");
											}
											unitFromSearchPage = StringUtils.stripStart(unitFromSearchPage, "0");
											legalStructItem.setLot(unitFromSearchPage);
										}

										preparePersonalStructForCounty(legalStructItem, searchId);
										multipleLegals.add(reg.prettyPrint());
										tempLegalStructListPerDocument.add(legalStructItem);
									}
									if (legal.hasTownshipLegal()) {
										LegalStructDTG legalStructItem = new LegalStructDTG(SECTIONAL_TYPE);
										TownShipI township = legal.getTownShip();

										String arb = township.getArb();
										String sec = township.getSection();
										String tw = township.getTownship();
										String rg = township.getRange();
										int qo = township.getQuarterOrder();
										String qv = township.getQuarterValue();
										String arbLot = township.getArbLot();
										String arbBlock = township.getArbBlock();
										String arbBook = township.getArbBook();
										String arbPage = township.getArbPage();
										
										if (StringUtils.isNotEmpty(arb) && StringUtils.isEmpty(sec)){
											legalStructItem.setType(ARB_TYPE);
											
											String[] arbs = arb.split("\\s*-\\s*");
											if (arbs.length == 3){
												legalStructItem.setArbDtrct(arbs[0]);
												legalStructItem.setArbParcel(arbs[1]);
												legalStructItem.setArbParcelSplit(arbs[2]);
											}
										}

										legalStructItem.setArb(StringUtils.isEmpty(arb) ? "" : arb);
										legalStructItem.setSection(StringUtils.isEmpty(sec) ? "" : sec);
										legalStructItem.setTownship(StringUtils.isEmpty(tw) ? "" : tw);
										legalStructItem.setRange(StringUtils.isEmpty(rg) ? "" : rg);

										legalStructItem.setArbLot(StringUtils.isEmpty(arbLot) ? "" : arbLot);
										legalStructItem.setArbBlock(StringUtils.isEmpty(arbBlock) ? "" : arbBlock);
										legalStructItem.setArbBook(StringUtils.isEmpty(arbBook) ? "" : arbBook);
										legalStructItem.setArbPage(StringUtils.isEmpty(arbPage) ? "" : arbPage);

										legalStructItem.setQuarterValue(StringUtils.isEmpty(qv) ? "" : qv);
										legalStructItem.setQuarterOrder(String.valueOf(qo <= 0 ? "" : qo));

										multipleLegals.add(reg.prettyPrint());
										tempLegalStructListPerDocument.add(legalStructItem);
									}
								}
							}
							if (allAoAndTrlots.size() > 0) {
								List<LegalStructDTG> aoAndTrMatches = new ArrayList<LegalStructDTG>();
								for (LegalStructDTG item : tempLegalStructListPerDocument) {
									if (!StringUtils.isEmpty(item.getLot())) {
										for (String lt : allAoAndTrlots) {
											lt = StringUtils.stripStart(lt, "0");
											if (GenericLegal.computeScoreInternal("lot", lt, item.getLot(), false, false) >= 0.8
													|| GenericLegal.computeScoreInternal("lot", item.getLot(), lt, false, false) >= 0.8) {
												aoAndTrMatches.add(item);
											}
										}
									}
								}
								if (aoAndTrMatches.size() > 0) {
									tempLegalStructListPerDocument.clear();
									tempLegalStructListPerDocument.addAll(aoAndTrMatches);
								}
							}

							if (!atLeastOneFromDg && !tempLegalStructListPerDocument.isEmpty()) {
								atLeastOneFromDg = dataSite.getSiteTypeAbrev().equals(reg.getDataSource());
							}

							for (LegalStructDTG item : tempLegalStructListPerDocument) {
								if (!testIfExist(legalStructList, item, searchId)) {
									addLegalStructAtInterval(legalStructList, item);
								}
							}
						}
					}
					if (isTimeShare) {
						for (LegalStructDTG pers : legalStructList) {
							String unit = global.getSa().getAtribute(SearchAttributes.P_STREETUNIT);
							String lot = week;
							String block = unit;
							if (!block.toUpperCase().startsWith("U")) {
								block = "U" + block;
							}
							lot = StringUtils.stripStart(lot, "0");
							pers.setLot(lot);
							pers.setBlock(block);

							String building = global.getSa().getAtribute(SearchAttributes.BUILDING);
							if (!StringUtils.isEmpty(building)) {
								pers.setLot(block);
								pers.setSubLot(lot);
								pers.setBlock(building);
							}
						}

						List<LegalStructDTG> tempList = new ArrayList<LegalStructDTG>(legalStructList);
						legalStructList.clear();

						for (LegalStructDTG legalStructItem : tempList) {
							if (!testIfExist(legalStructList, legalStructItem, searchId)) {
								legalStructList.add(legalStructItem);
							}
						}
					}

					legalStructList = keepOnlyGoodLegal(legalStructList);

					try {
						DataSite dataSite = HashCountyToIndex.getCrtServer(searchId, false);
						long miServerId = dataSite.getServerId();

						List<LegalStructDTG> tempLegalStructListPerDocument = new ArrayList<LegalStructDTG>();

						for (LegalI legal : global.getSa().getForUpdateSearchLegalsNotNull(miServerId)) {
							if (legal.hasSubdividedLegal()) {

								LegalStructDTG legalStructItem = new LegalStructDTG(SUBDIVIDED_TYPE);
								SubdivisionI subdiv = legal.getSubdivision();

								String block = subdiv.getBlock();
								String lot = subdiv.getLot();
								String sublot = subdiv.getSubLot();
								String lotThru = subdiv.getLotThrough();
								String platBook = subdiv.getPlatBook();
								String platPage = subdiv.getPlatPage();
								String platInst = subdiv.getPlatInstrument();
								String platInstYear = subdiv.getPlatInstrumentYear();
								String unit = subdiv.getUnit();
								legalStructItem.setUnit(StringUtils.isEmpty(unit) ? "" : unit);

								legalStructItem.setBlock(StringUtils.isEmpty(block) ? "" : block);
								legalStructItem.setSubLot(org.apache.commons.lang.StringUtils.defaultString(sublot));
								legalStructItem.setLotThru(StringUtils.isEmpty(lotThru) ? "" : lotThru);
								legalStructItem.setPlatBook(StringUtils.isEmpty(platBook) ? "" : platBook);
								legalStructItem.setPlatPage(StringUtils.isEmpty(platPage) ? "" : platPage);
								legalStructItem.setPlatInst(StringUtils.isEmpty(platInst) ? "" : platInst);
								legalStructItem.setPlatInstrYear(StringUtils.isEmpty(platInstYear) ? "" : platInstYear);

								if (StringUtils.isEmpty(lot)) {
									if (!StringUtils.isEmpty(unit)) {
										unit = StringUtils.stripStart(unit, "0");
										legalStructItem.setLot(unit);
										if (!unit.startsWith("U")) {
											LegalStructDTG legalStructItemA = null;
											try {
												legalStructItemA = (LegalStructDTG) legalStructItem.clone();
											} catch (CloneNotSupportedException e) {
												e.printStackTrace();
											}
											if (legalStructItemA != null) {
												legalStructItemA.setLot("U" + unit);
												preparePersonalStructForCounty(legalStructItemA, searchId);
												multipleLegals.add("Saved Search Parameters from Parent Site");
												tempLegalStructListPerDocument.add(legalStructItemA);
											}
										}
									}
								} else {
									lot = StringUtils.stripStart(lot, "0");
									legalStructItem.setLot(lot);
								}

								preparePersonalStructForCounty(legalStructItem, searchId);
								multipleLegals.add("Saved Search Parameters from Parent Site");
								tempLegalStructListPerDocument.add(legalStructItem);
							}

							if (legal.hasTownshipLegal()) {
								LegalStructDTG legalStructItem = new LegalStructDTG(SECTIONAL_TYPE);
								TownShipI township = legal.getTownShip();

								String arb = township.getArb();
								String sec = township.getSection();
								String tw = township.getTownship();
								String rg = township.getRange();
								int qo = township.getQuarterOrder();
								String qv = township.getQuarterValue();
								String arbLot = township.getArbLot();
								String arbBlock = township.getArbBlock();
								String arbBook = township.getArbBook();
								String arbPage = township.getArbPage();
								
								if (StringUtils.isNotEmpty(arb) && StringUtils.isEmpty(sec)){
									legalStructItem.setType(ARB_TYPE);
									
									String[] arbs = arb.split("\\s*-\\s*");
									if (arbs.length == 3){
										legalStructItem.setArbDtrct(arbs[0]);
										legalStructItem.setArbParcel(arbs[1]);
										legalStructItem.setArbParcelSplit(arbs[2]);
									}
								}

								legalStructItem.setArb(StringUtils.isEmpty(arb) ? "" : arb);
								legalStructItem.setSection(StringUtils.isEmpty(sec) ? "" : sec);
								legalStructItem.setTownship(StringUtils.isEmpty(tw) ? "" : tw);
								legalStructItem.setRange(StringUtils.isEmpty(rg) ? "" : rg);
								legalStructItem.setQuarterValue(StringUtils.isEmpty(qv) ? "" : qv);
								legalStructItem.setQuarterOrder(String.valueOf(qo <= 0 ? "" : qo));

								legalStructItem.setArbLot(StringUtils.isEmpty(arbLot) ? "" : arbLot);
								legalStructItem.setArbBlock(StringUtils.isEmpty(arbBlock) ? "" : arbBlock);
								legalStructItem.setArbBook(StringUtils.isEmpty(arbBook) ? "" : arbBook);
								legalStructItem.setArbPage(StringUtils.isEmpty(arbPage) ? "" : arbPage);

								multipleLegals.add("Saved Search Parameters from Parent Site");
								tempLegalStructListPerDocument.add(legalStructItem);
							}
						}
						for (LegalStructDTG item : tempLegalStructListPerDocument) {
							if (!testIfExist(legalStructList, item, searchId)) {
								legalStructList.add(item);
							}
						}
					} catch (Exception e) {
						logger.error("Error loading names for Update saved from Parent Site", e);
					}
					legalStructList = keepOnlyGoodLegal(legalStructList);

					Set<LegalStructDTG> tempPersonal = new HashSet<LegalStructDTG>();

					for (LegalStructDTG pers : legalStructList) {
						String lotsAll = pers.getLot();
						if (StringUtils.isNotEmpty(lotsAll)) {
							String[] lots = lotsAll.split("[ ,\t\n\r]");

							for (String s : lots) {
								try {
									LegalStructDTG legalStructItem = (LegalStructDTG) pers.clone();
									s = StringUtils.stripStart(s, "0");
									legalStructItem.setLot(s);
									if (!testIfExist(tempPersonal, legalStructItem, searchId)) {
										tempPersonal.add(legalStructItem);
									}
								} catch (CloneNotSupportedException e) {
									e.printStackTrace();
								}
							}
						} else {
							if (!testIfExist(tempPersonal, pers, searchId)) {
								tempPersonal.add(pers);
							}
						}
					}

					if (isLoadFromSearchPage() || (legalStructList.isEmpty() && isLoadFromSearchPageIfNoLookup())){
						if (StringUtils.isNotEmpty(platBookAO) && StringUtils.isNotEmpty(platPageAO)) {

							StringBuilder sb = new StringBuilder();
							HashSet<String> newAllAoAndTrlots = new HashSet<String>();
							for (String lot : allAoAndTrlots) {
								if (lot.matches("\\d+")) {
									lot = StringUtils.stripStart(lot, "0");
									sb.append(lot).append(" ");
								} else {
									newAllAoAndTrlots.add(lot);
								}
							}
							allAoAndTrlots = new HashSet<String>();
							for (LotInterval interval : LotMatchAlgorithm.prepareLotInterval(sb.toString())) {
								int lot = interval.getLow();
								int lotThru = interval.getHigh();
								if (lot == 0) {
									continue;
								}
								LegalStructDTG struct = new LegalStructDTG(SUBDIVIDED_TYPE);
								struct.setLot(Integer.toString(lot));
								if (lot != lotThru) {
									struct.setLotThru(Integer.toString(lotThru));
								}
								struct.setSubLot(subLotNo);
								struct.setBlock(aoAndTrBloks);
								struct.setPlatBook(platBookAO);
								struct.setPlatPage(platPageAO);
								if (!testIfExist(tempPersonal, struct, searchId)) {
									tempPersonal.add(struct);
									multipleLegals.add("Search Page");
								}
							}
							allAoAndTrlots.clear();
							allAoAndTrlots.addAll(newAllAoAndTrlots);

							for (String lot : allAoAndTrlots) {
								LegalStructDTG struct = new LegalStructDTG(SUBDIVIDED_TYPE);
								lot = StringUtils.stripStart(lot, "0");
								struct.setLot(lot);
								struct.setSubLot(subLotNo);
								struct.setBlock(aoAndTrBloks);
								struct.setPlatBook(platBookAO);
								struct.setPlatPage(platPageAO);
								if (!testIfExist(tempPersonal, struct, searchId)) {
									tempPersonal.add(struct);
									multipleLegals.add("Search Page");
								}
							}
						}
					}
					
					legalStructList = tempPersonal;
					legalStructList = keepOnlyGoodLegal(legalStructList);

					if (!atLeastOneFromDg) {
						List<LegalStructDTG> newIterations = new ArrayList<LegalStructDTG>();
						for (LegalStructDTG pers : legalStructList){
							if (pers.isPlated() && !pers.getPlatBook().startsWith("P")){
								try {
									LegalStructDTG persClone = (LegalStructDTG) pers.clone();
									persClone.setPlatBook("P" + persClone.getPlatBook());
									newIterations.add(persClone);
								} catch (CloneNotSupportedException e) {
									e.printStackTrace();
								}
							}
						}
						if (!newIterations.isEmpty()){
							for (LegalStructDTG legalStructItem : newIterations){
								boolean atLeastOneEqual = false;

								for (LegalStructDTG p : legalStructList) {
									if (p.isPlated()){
										preparePersonalStructForCounty(p, searchId);
										if (legalStructItem.equalsStrictSubdivided(p)) {
											atLeastOneEqual = true;
											break;
										}
									}
								}
								if (!atLeastOneEqual){
									legalStructList.add(legalStructItem);
								}
							}
						}
					}

					global.setAdditionalInfo(AdditionalInfoKeys.DG_LOOK_UP_DATA, legalStructList);

					if (legalStructList.size() > 0) {
						if (isPlatedMultyLot(legalStructList)) {
							boostrapSubdividedData(legalStructList, global, true);
						} else {
							boostrapSubdividedData(legalStructList, global, false);
						}
						if (isSectionalMultyQv(legalStructList) || isArbExtended(legalStructList) || legalStructList.size() == 1) {
							boostrapSectionalData(legalStructList, global);
						}
					}
					if (isTimeShare && legalStructList.size() > 0) {
						List<DocumentI> docList = m.getDocumentsWithDataSource(false, dataSite.getSiteTypeAbrev());
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
					&& !(isPlatedMultyLot(legalStructList) 
							|| isSectionalMultyQv(legalStructList) 
							|| isPlatedMultyLotAndIsSectionalMultyQV(legalStructList))) {
				
				global.getSa().setAtribute(SearchAttributes.ATS_MULTIPLE_LEGALS_FOUND, "true");
				global.getSa().setAtribute(SearchAttributes.ATS_MULTIPLE_LEGAL_INSTRUMENTS, multipleLegals.toString());
				global.setAdditionalInfo(OH_MULTIPLE_LEGAL_INSTR, multipleLegals);
				if (first){
					SearchLogger.info("<br/><b>Questionable multiple legals found in " + multipleLegals.toString() + "</b>", searchId);
				}
			}

			if (isPlatedMultyLot(legalStructList) && allAoAndTrlots.size() > 0 && !isTimeShare) {
				addLegalStructItemsUsingAoAndTrLots(legalStructList, allAoAndTrlots.toArray(new String[allAoAndTrlots.size()]), searchId, true);
			}

			// expand legals by Sublot
			legalStructList = expandLegalStructItemsSublot(legalStructList, searchId);

			return new ArrayList<LegalStructDTG>(legalStructList);
		}
		
		protected void loadDerrivation(TSServerInfoModule module, LegalStructDTG str){
			
			switch(module.getModuleIdx()){
				case TSServerInfo.SUBDIVISION_MODULE_IDX:
					module.setData(2, str.getLot());
					if (StringUtils.isNotEmpty(str.getLotThru())){
						module.setData(20, str.getLotThru());
					}
					module.setData(3, str.getBlock());
					module.setData(4, str.getPlatBook());
					module.setData(5, str.getPlatPage());
					module.setData(6, str.getPlatInst());
					module.forceValue(10, str.getSubLot());
					if(StringUtils.isNotEmpty(str.getPlatInstrYear())){
						module.forceValue(11, str.getPlatInstrYear());
					}
				break;
				case TSServerInfo.ARB_MODULE_IDX:{
					if (StringUtils.isNotEmpty(str.getArbBook())){
						module.setData(0, str.getArbBook());
						module.setData(1, str.getArbPage());
						module.setData(2, str.getArbBlock());
						module.setData(3, str.getArbLot());
					} else if (StringUtils.isNotEmpty(str.getArbDtrct())){
						module.setData(0, str.getArbDtrct());
						module.setData(1, str.getArbParcel());
						module.setData(2, str.getArbParcelSplit());
					}
				}
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

		@Override
		public boolean isTransferAllowed(RegisterDocumentI doc) {
			return doc != null && doc.isOneOf(DocumentTypes.TRANSFER);
		}
	}
	
	@Override
	protected boolean incompleteData(LegalStructDTG str) {
		if (str == null){
			return true;
		}
		
		boolean emptyLot = StringUtils.isEmpty(str.getLot());
		boolean emptyBlock = StringUtils.isEmpty(str.getBlock());
		boolean emptyPlatInst = StringUtils.isEmpty(str.getPlatInst());
		boolean emptyPlatBook = StringUtils.isEmpty(str.getPlatBook());
		boolean emptyPlatPage = StringUtils.isEmpty(str.getPlatPage());
		
		boolean emptyArb = emptyArb(str);
		
		
		if (ARB_TYPE.equalsIgnoreCase(str.getType())){
			if (!emptyArb){
				return emptyArb;
			}
		} else if(SUBDIVIDED_TYPE.equalsIgnoreCase(str.getType())){
			return ((emptyPlatBook || emptyPlatPage) && emptyPlatInst) || (emptyBlock && emptyLot);
		}
		
		return   true;
	}
	
	protected boolean emptyArb(LegalStructDTG str){
		
		return true;
	}
	
	protected List<RegisterDocumentI> getGoodDocumentsOrForCurrentOwner(DocumentsManagerI m, Search search, boolean applyNameMatch, int siteType, String stateAbbrev){
		final List<RegisterDocumentI> ret = new ArrayList<RegisterDocumentI>();
		
		List<RegisterDocumentI> listRodocs = m.getRoLikeDocumentList();
		DocumentUtils.sortDocuments(listRodocs, MultilineElementsMap.DATE_ORDER_DESC);
		
		SearchAttributes sa	= search.getSa();
		PartyI owner 		= sa.getOwners();
		
		
		for (RegisterDocumentI doc : listRodocs){
			boolean found = false;
			for (PropertyI prop : doc.getProperties()){
				if ((doc.isOneOf("MORTGAGE", "TRANSFER", "RELEASE") && applyNameMatch)
						|| (doc.isOneOf("MORTGAGE", "TRANSFER") && !applyNameMatch)){
					if (prop.hasSubdividedLegal()){
						SubdivisionI sub = prop.getLegal().getSubdivision();
						LegalStructDTG ret1 = new LegalStructDTG(SUBDIVIDED_TYPE);
						ret1.setLot(sub.getLot());
						ret1.setBlock(sub.getBlock());
						ret1.setPlatBook(sub.getPlatBook());
						ret1.setPlatPage(sub.getPlatPage());
						ret1.setPlatInst(sub.getPlatInstrument());
						ret1.setPlatInstrYear(sub.getPlatInstrumentYear());
						
						boolean nameMatched = false;
						
						if (applyNameMatch){
							if (GenericNameFilter.isMatchGreaterThenScore(owner, doc.getGrantor(), NameFilterFactory.NAME_FILTER_THRESHOLD) ||
									GenericNameFilter.isMatchGreaterThenScore(doc.getGrantor(), owner, NameFilterFactory.NAME_FILTER_THRESHOLD)||
									GenericNameFilter.isMatchGreaterThenScore(owner, doc.getGrantee(), NameFilterFactory.NAME_FILTER_THRESHOLD) ||
									GenericNameFilter.isMatchGreaterThenScore(doc.getGrantee(), owner, NameFilterFactory.NAME_FILTER_THRESHOLD)){
									nameMatched = true;
							}
						}
						
						if ((nameMatched || !applyNameMatch) 
								&& StringUtils.isNotEmpty(ret1.getLot()) 
								&& StringUtils.isNotEmpty(ret1.getPlatBook()) 
								&& StringUtils.isNotEmpty(ret1.getPlatPage()) ){
							ret.add(doc);
							found = true;
							break;
						}
					}
					else if (prop.hasTownshipLegal()){
						TownShipI sub = prop.getLegal().getTownShip();
						LegalStructDTG ret1 = new LegalStructDTG(SECTIONAL_TYPE);
						ret1.setSection(sub.getSection());
						ret1.setTownship(sub.getTownship());
						ret1.setRange(sub.getRange());
						int qo = sub.getQuarterOrder();
						String qv = sub.getQuarterValue();
						if (qo > 0){
							ret1.setQuarterOrder(qo + "");
						}
						ret1.setQuarterValue(qv);
						ret1.setArb(sub.getArb());
						ret1.setArbLot(sub.getArbLot());
						ret1.setArbBlock(sub.getArbBlock());
						ret1.setArbPage(sub.getArbPage());
						ret1.setArbBook(sub.getArbBook());
						
						if (StringUtils.isNotEmpty(sub.getArb())){
							String[] arbs = sub.getArb().split("\\s*-\\s*");
							if (arbs.length == 3){
								ret1.setArbDtrct(arbs[0]);
								ret1.setArbParcel(arbs[1]);
								ret1.setArbParcelSplit(arbs[2]);
							}
						}
						
						boolean nameMatched = false;
						if (applyNameMatch){
							if (GenericNameFilter.isMatchGreaterThenScore(owner, doc.getGrantor(), NameFilterFactory.NAME_FILTER_THRESHOLD) ||
									GenericNameFilter.isMatchGreaterThenScore(doc.getGrantor(), owner, NameFilterFactory.NAME_FILTER_THRESHOLD)||
									GenericNameFilter.isMatchGreaterThenScore(owner, doc.getGrantee(), NameFilterFactory.NAME_FILTER_THRESHOLD) ||
									GenericNameFilter.isMatchGreaterThenScore(doc.getGrantee(), owner, NameFilterFactory.NAME_FILTER_THRESHOLD)){
									nameMatched = true;
							}
						}
						if ((nameMatched || !applyNameMatch) && isCompleteLegal(ret1, qo, qv, siteType, stateAbbrev)){
							ret.add(doc);
							found = true;
							break;
						}
					}
				}
			}
			if (found){
				break;
			}
		}
		return ret;
	}

	private PlatIterator getPlatBookPageIterator(long searchId) {
		
		PlatIterator platIterator = new PlatIterator(searchId);
		
		return platIterator;
		
	}
	
	protected class PlatIterator extends GenericRuntimeIterator<LegalStructDTG>{
		
		public PlatIterator(long searchId) {
			super(searchId);
		}
			
		private String extraStringForsection = "";
			
		private static final long serialVersionUID = 823434519L;
			
		protected List<LegalStructDTG> createDerrivations(){
			LegalDescriptionIterator ldit = new LegalDescriptionIterator(searchId);
				
			List<LegalStructDTG> list = ldit.createDerrivations();
			if (list.size() == 0){
				list.add(new LegalStructDTG(""));
			}
				
			List<LegalStructDTG>  list1 = new ArrayList<LegalStructDTG>();
			for (LegalStructDTG p : list){
				boolean alreadyContained = false;
				for (LegalStructDTG p1 : list1){
					if (p1.equalsSectionalAndPlat(p)){
						alreadyContained = true;
						break;
					} else{
						alreadyContained = false;
					}
				}
				if (!alreadyContained){
					list1.add(p);
				}
			}
			if (list1.size() > 0){
				return list1;
			}
			return list;
		}
			
		protected void loadDerrivation(TSServerInfoModule module, LegalStructDTG str){
				
			switch(module.getModuleIdx()){
			case TSServerInfo.BOOK_AND_PAGE_LOCAL_MODULE_IDX:
				if(module.getSaObjKey().equals(SearchAttributes.LD_BOOKNO)){
					addPlatMapSearch(module,str);
				}else if(module.getSaObjKey().equals(SearchAttributes.LD_BOOKNO_1)){
					addAssesorMapSearch(module, str, extraStringForsection);
				}else if(module.getSaObjKey().equals(SearchAttributes.LD_SUBDIVISION)){
					addSubdivisionMapSearch(module,str);
				}else if(module.getSaObjKey().equals(SearchAttributes.LD_PARCELNO_MAP)){
					addParcelMapSearch(module, str);
				}else if(module.getSaObjKey().equals(SearchAttributes.LD_PARCELNO_CONDO)){
					addCondoMapSearch(module, str);
				}
				break;
			}
		}

		public void setExtraStringForSection(String str) {
			extraStringForsection = str;
		}
		
	}
	

	public void addPlatMapSearch(TSServerInfoModule module,	LegalStructDTG str) { 
		CurrentInstance ci 	= InstanceManager.getManager().getCurrentInstance(searchId);
		Search search 		= ci.getCrtSearchContext();
	
		String book  = str.getPlatBook();
		String page  = str.getPlatPage();
		
		DocumentsManagerI m = search.getDocManager();
		
		if( StringUtils.isEmpty(book)||StringUtils.isEmpty(page) ){
			try{
				m.getAccess();
				String[] subdivVector = getSubdivisionVector(m);
				if(StringUtils.isEmpty(book)){
					book = subdivVector[0];
				}
				if(StringUtils.isEmpty(page)){
					page = subdivVector[1];
				}
			}finally{
				m.releaseAccess();
			}
		}
		if (!StringUtils.isEmpty(book) && book.length() >= 2){
			book = book.replaceFirst("[a-zA-Z]", "");
		}
		
		if (!StringUtils.isEmpty(page) && page.length() >= 2){
			page = page.replaceAll("[a-zA-Z]", "");
		}
		if (StringUtils.isEmpty(page)){
			page = "";
		}
		module.forceValue(0, book);
		module.forceValue(1, page.split("[ -]")[0]);
		module.forceValue(4, "PLAT");
		module.forceValue(5, "PLAT_MAP");
		return;
	}

	public void addSubdivisionMapSearch(TSServerInfoModule module, LegalStructDTG str) { }

	public void addParcelMapSearch(TSServerInfoModule module, LegalStructDTG str) { }

	public void addCondoMapSearch(TSServerInfoModule module, LegalStructDTG str) {	}

	public void addAssesorMapSearch(TSServerInfoModule module, LegalStructDTG str, String extraStringForsection) {	
		CurrentInstance ci 	= InstanceManager.getManager().getCurrentInstance(searchId);
		Search search 		= ci.getCrtSearchContext();
	
		String book  = str.getPlatBook();
		String page  = str.getPlatPage();
		
		DocumentsManagerI m = search.getDocManager();
		
		if( StringUtils.isEmpty(book)||StringUtils.isEmpty(page) ){
			try{
				m.getAccess();
				String[] subdivVector = getSubdivisionVector(m);
				if(StringUtils.isEmpty(book)){
					book = subdivVector[0];
				}
				if(StringUtils.isEmpty(page)){
					page = subdivVector[1];
				}
			}finally{
				m.releaseAccess();
			}
		}
		if (!StringUtils.isEmpty(book) && book.length() >= 2){
			book = book.replaceFirst("[a-zA-Z]", "");
		}
		
		if (!StringUtils.isEmpty(page) && page.length() >= 2){
			page = page.replaceAll("[a-zA-Z]", "");
		}
		if (StringUtils.isEmpty(page)){
			page = "";
		}
		module.forceValue(0, book);
		module.forceValue(1, page.split("[ -]")[0]);
		module.forceValue(4, "PLAT");
		module.forceValue(5, "ASSESSOR_MAP");
		return;
	}
	
	public InstrumentGenericIterator getInstrumentOrBookTypeIterator(boolean instrumentType) {
		InstrumentGenericIterator instrumentGenericIterator = new InstrumentGenericIterator(searchId) {

			private static final long serialVersionUID = 5399351945130601258L;
			
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
		InstrumentGenericIterator instrumentGenericIterator = new InstrumentGenericIterator(searchId) {

			private static final long serialVersionUID = 5399351945130601258L;

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
			@Override
			public void setYear(InstrumentI instrumentI){}
			
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
		if (almostLike != null && almostLike.isEmpty()){
			boolean tryAgain = false;
			if (dataSite.getCountyId() == CountyConstants.OH_Cuyahoga){
				String instNo = documentToCheck.getInstno();
				if (StringUtils.isNotEmpty(instNo)){
					if (instNo.length() == 12){
						documentToCheckCopy.setInstno(instNo.substring(4));
						tryAgain = true;
					}
				}
			}
			if (tryAgain){
				almostLike = documentManager.getDocumentsWithInstrumentsFlexible(false, documentToCheckCopy);
			}
		}
		if (almostLike != null && !almostLike.isEmpty()) {
			
			return almostLike;
			
		} else {
			if (checkDocType) {
				return almostLike;
			}
			String bookToCheck = documentToCheck.getBook();
				
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
		HashMap<String, String> map = HttpUtils.getParamsFromLink(image.getLink(0));
		
		String book 	 = map.get("book");
		String page 	 = map.get("page");
		String docNumber = map.get("instr");
		String year 	 = map.get("year");
		String type 	 = map.get("type");	
    	String month 	 = map.get("month");
    	String day 		 = map.get("day");
		String dataTreeIndexStr = StringUtils.defaultString(map.get("dataTreeIndex"));
    	String dataTreeDesc =  StringUtils.defaultString(map.get("dataTreeDesc"));
		
    	int dataTreeIndex = -1;
    	try{dataTreeIndex = Integer.parseInt(dataTreeIndexStr);}catch(Exception e){};
    	
    	boolean savedImage = false;
    	
    	docNumber = prepareDocumentNoForImageSearch(docNumber, year, month, day);
    	
    	int yearInt = -1;
    	yearInt = Integer.parseInt(year);
    	
    	if (StringUtils.isNotEmpty(book) && book.matches("(?is)\\A[A-Z]\\d+")){
    		book = book.replaceAll("(?is)\\A[A-Z](\\d+)", "$1");
    	}
    	
    	InstrumentI i = new Instrument();
		i.setBook(book);
		i.setPage(page);
		i.setInstno(docNumber);
		i.setYear(yearInt);
		i.setDocType(type);
    	
		DownloadImageResult savedFromOtherSite = downloadedFromOtherSite(image, i);
    	
		if (TSInterface.DownloadImageResult.Status.OK.equals(savedFromOtherSite.getStatus())){
    		return savedFromOtherSite;
    	}
    	
		if (datTreeList == null){
			datTreeList = initDataTreeStruct();
		}
		
		if (dataTreeIndex > 0){
			DataTreeAccount acc = DataTreeManager.getDatatTreeAccount(String.valueOf(getCommunityId()));
			try {
				SimpleImage im = null;
				try {
					im = DataTreeConn.retrieveImage(acc, dataTreeIndex , dataTreeDesc, 999, 0);
				} catch (DataTreeImageException e) {
					e.printStackTrace();
					SearchLogger.info(
							"<br/>FAILED to take Image(searchId=" + searchId + ") book=" 
									+ i.getBook() + " page=" + i.getPage() + " inst=" + i.getInstno() + " from DataTree. "+
									"Official Server Message: [" + e.getLocalizedMessage() + " (" + e.getStatus() + ") ]<br/>", searchId);
				}
				if (im != null){
					FileUtils.writeByteArrayToFile(new File(image.getPath()),im.getContent());
					logger.info("------------------------ downloadImageFromDataTree return true for instr=" + i + " savePath=" + image.getPath());
					savedImage = true;
				}
			} catch (IOException e) {
				e.printStackTrace();
			}
		} else{
			try {
				savedImage = FLGenericDASLDT.downloadImageFromDataTree(i, datTreeList, image.getPath(), String.valueOf(getCommunityId()), month, day);
			} catch (DataTreeImageException e) {
				logger.error("Error while getting image ", e);
				SearchLogger.info(
						"<br/>FAILED to take Image(searchId=" + searchId + ") book=" +
						i.getBook() + " page=" + i.getPage() + " inst=" + 
						i.getInstno() + " from DataTree. " + 
						"Official Server Message: [" + e.getLocalizedMessage() + " (" + e.getStatus() + ") ]<br/>", searchId);
			}
		}
		
		if (savedImage) {
			afterDownloadImage(savedImage);
			SearchLogger.info("<br/>Image(searchId=" + searchId + ") book=" 
								+ i.getBook() + " page=" + i.getPage() + "inst=" + i.getInstno()
								+ " was taken from DataTree<br/>", searchId);
		}
		
		byte[] b = new byte[0];
		if (new File(image.getPath()).exists()){
			try {
				b = FileUtils.readFileToByteArray(new File(image.getPath()));
			} catch (IOException e) {e.printStackTrace();}
		}
		if (b.length > 0){
			return new DownloadImageResult(Status.OK, b , image.getContentType());
		}
		
		return new DownloadImageResult(Status.ERROR, b , image.getContentType());
    }
	
	public DownloadImageResult downloadedFromOtherSite(ImageI image, InstrumentI instr){
		
		return new DownloadImageResult();
	}
	
	@Override
	protected String getImageLink(DTRecord record){
    	if (record == null){ return null; }
    	
    	String[] dtImgInfo = record.getDTImageInfo();
    	if (dtImgInfo == null) { 
    		dtImgInfo = new String[2]; 
    		dtImgInfo[0] = "";
    		dtImgInfo[1] = "";
    	}
    	
    	String instr = record.getInstrumentNo();
    	if (StringUtils.isBlank(instr) || "unknown".equals(instr)){  
    		instr = "";
    	}
    	
    	String book = record.getBook();
    	String page = record.getPage();
    	String year = record.getInstYear();
    	String day  = record.getInstDay();
		String month = record.getInstMonth();
    	
    	book = StringUtils.isBlank(book) ? "" : book; 
    	page = StringUtils.isBlank(page) ? "" : page;
    	year = StringUtils.isBlank(year) ? "" : year;
    	
    	
    	String dataTreeIndex = "";
    	try{dataTreeIndex = StringUtils.defaultString(record.getInstrumentInfo().get("image.image_params.document_index_id"));}
    	catch(Exception e){};
    	
    	String dataTreeDesc = "";
    	try{dataTreeDesc = StringUtils.defaultString(record.getInstrumentInfo().get("image.image_params.description"));}
    	catch(Exception e){};
    	
    	String dataTreeDocType = "";
    	try{dataTreeDocType = StringUtils.defaultString(record.getInstrumentInfo().get("image.image_params.document_type"));}
    	catch(Exception e){};
    	
    	String instrDesc = "";
    	try{ 
    		instrDesc = StringUtils.defaultString(record.getAliasInfo().get("image.image_params.description"));
	    	String number = StringUtils.defaultString(record.getAliasInfo().get("number"));
	    	String yearInstr = StringUtils.defaultString(record.getAliasInfo().get("year"));
	    	if (StringUtils.isNotEmpty(number) && StringUtils.isNotEmpty(yearInstr)){
	    		instrDesc = yearInstr + "." + number;
	    	}
    	}
    	catch(Exception e){};
    	
    	if (StringUtils.isEmpty(dataTreeIndex) || StringUtils.isEmpty(dataTreeDesc)){
			return null;
    	} else{ 
    		String link = CreatePartialLink(TSConnectionURL.idGET) + "look_for_dt_image&id=" + dtImgInfo[0] + "&description=" + dtImgInfo[1] + "&instr=" + instr 
					+ "&book=" + book + "&page=" + page  + "&year=" + year + "&month=" + month + "&day=" + day
					+ "&dataTreeIndex=" + dataTreeIndex + "&dataTreeDesc=" + dataTreeDesc;
	    	if (StringUtils.isNotEmpty(instrDesc)){
	    			link += "&extraInstrDesc=" + instrDesc;
	    	}
	    	if (StringUtils.isNotEmpty(dataTreeDocType)){
	    		link += "&dataTreeDocType=" + dataTreeDocType;
	    	}
    		return link;	
    	}
	}
	
	public static String prepareApnPerCounty(DataSite dataSite, String originalApn){
		
		if (dataSite == null){
			return originalApn;
		}
		String apn = originalApn;
		switch (dataSite.getCountyId()) {
			case CountyConstants.OH_Fairfield:
				apn = apn.replaceAll("-", "");
				if (apn.length() == 10){
					apn = apn.replaceAll("(\\d{3})(\\d{5})(\\d{2})", "$1-$2-$3");
					originalApn = apn;
				}
				break;
			case CountyConstants.OH_Lorain:
				apn = apn.replaceAll("-", "");
				if (apn.length() == 13){
					apn = apn.replaceAll("(\\d{2})(\\d{2})(\\d{3})(\\d{3})(\\d{3})", "$1-$2-$3-$4-$5");
					originalApn = apn;
				}
				break;
			default:
				break;
			}
			
		return originalApn;
	}
	
}
