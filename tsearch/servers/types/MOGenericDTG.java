package ro.cst.tsearch.servers.types;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Vector;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.StringUtils;

import ro.cst.tsearch.Search;
import ro.cst.tsearch.bean.SearchAttributes;
import ro.cst.tsearch.data.CountyConstants;
import ro.cst.tsearch.data.StateContants;
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
import ro.cst.tsearch.search.filter.newfilters.legal.GenericLegal;
import ro.cst.tsearch.search.filter.newfilters.legal.LegalFilterFactory;
import ro.cst.tsearch.search.filter.newfilters.name.GenericNameFilter;
import ro.cst.tsearch.search.filter.newfilters.name.NameFilterFactory;
import ro.cst.tsearch.search.filter.newfilters.pin.PinFilterResponse;
import ro.cst.tsearch.search.iterator.data.LegalStruct;
import ro.cst.tsearch.search.iterator.instrument.InstrumentGenericIterator;
import ro.cst.tsearch.search.iterator.legal.LegalDescriptionIteratorI;
import ro.cst.tsearch.searchsites.client.GWTDataSite;
import ro.cst.tsearch.servers.HashCountyToIndex;
import ro.cst.tsearch.servers.TSConnectionURL;
import ro.cst.tsearch.servers.bean.DataSite;
import ro.cst.tsearch.servers.info.TSServerInfo;
import ro.cst.tsearch.servers.info.TSServerInfoConstants;
import ro.cst.tsearch.servers.info.TSServerInfoModule;
import ro.cst.tsearch.servers.response.ParsedResponse;
import ro.cst.tsearch.servers.response.ServerResponse;
import ro.cst.tsearch.servers.types.FLSubdividedBasedDASLDT.HasLotFilter;
import ro.cst.tsearch.servers.types.FLSubdividedBasedDASLDT.PersonalDataStruct;
import ro.cst.tsearch.servers.types.FLSubdividedBasedDASLDT.PlatIterator;
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
import com.stewart.ats.base.document.DocumentI.SearchType;
import com.stewart.ats.base.document.DocumentUtils;
import com.stewart.ats.base.document.ImageI;
import com.stewart.ats.base.document.Instrument;
import com.stewart.ats.base.document.InstrumentI;
import com.stewart.ats.base.document.RegisterDocumentI;
import com.stewart.ats.base.document.RestoreDocumentDataI;
import com.stewart.ats.base.legal.LegalI;
import com.stewart.ats.base.legal.SubdivisionI;
import com.stewart.ats.base.legal.TownShipI;
import com.stewart.ats.base.name.NameI;
import com.stewart.ats.base.property.PropertyI;
import com.stewart.ats.base.search.DocumentsManagerI;
import com.stewart.ats.tsrindex.client.SimpleChapterUtils;
import com.stewart.datatree.DataTreeAccount;
import com.stewart.datatree.DataTreeConn;
import com.stewart.datatree.DataTreeImageException;
import com.stewart.datatree.DataTreeManager;
import com.stewart.datatree.SimpleImage;

/**
 * @author mihaib
 * 
 */

public class MOGenericDTG extends TSServerDTG implements DTLikeAutomatic {

	private static final long serialVersionUID = -39475262378410L;

	public MOGenericDTG(String rsRequestSolverName, String rsSitePath, String rsServerID, String rsPrmNameLink, long searchId, int mid)
			throws FileNotFoundException, IOException {
		super(rsRequestSolverName, rsSitePath, rsServerID, rsPrmNameLink, searchId, mid);
		resultType = MULTIPLE_RESULT_TYPE;
	}

	public MOGenericDTG(long searchId) {
		super(searchId);
	}

	@Override
	public void setModulesForAutoSearch(TSServerInfo serverInfo) {

		Search global = InstanceManager.getManager().getCurrentInstance(searchId).getCrtSearchContext();
		List<TSServerInfoModule> modules = new ArrayList<TSServerInfoModule>();

		if (global.getSearchType() == Search.AUTOMATIC_SEARCH) {

			boolean isUpdate = (isUpdate()) || global.getSa().isDateDown();
			boolean isTimeShare = !StringUtils.isEmpty(global.getSa().getAtribute(SearchAttributes.WEEK));

			FilterResponse legalOrDocTypeFilter = FLSubdividedBasedDASLDT.getLegalOrDocTypeFilter(searchId, false);
			PinFilterResponse pinFilter = FLSubdividedBasedDASLDT.getPinFilter(searchId, true);

			String block = global.getSa().getAtribute(SearchAttributes.LD_SUBDIV_BLOCK);
			String lot = global.getSa().getAtribute(SearchAttributes.LD_LOTNO);

			TSServerInfoModule module = null;

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
				instrumentGenericIterator = getInstrumentIterator(true);

				module = new TSServerInfoModule(serverInfo.getModule(TSServerInfo.INSTR_NO_MODULE_IDX));
				module.addExtraInformation(TSServerInfoConstants.EXTRA_PARAM_MODULE_DESCRIPTION, TSServerInfoConstants.VALUE_PARAM_LIST_AO_NDB_TR_INSTR);
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
				bpGenericIterator = getInstrumentIterator(false);
				bpGenericIterator.setRemoveLeadingZerosBP(true);

				module = new TSServerInfoModule(serverInfo.getModule(TSServerInfo.BOOK_AND_PAGE_MODULE_IDX));
				module.addExtraInformation(TSServerInfoConstants.EXTRA_PARAM_MODULE_DESCRIPTION, TSServerInfoConstants.VALUE_PARAM_LIST_AO_NDB_TR_INSTR);
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

				ArrayList<NameI> searchedNames = super.addNameSearch(modules, serverInfo, SearchAttributes.OWNER_OBJECT, null, filtersOList);

				super.addOCRSearch(modules, serverInfo, legalOrDocTypeFilter);

				super.addNameSearch(modules, serverInfo, SearchAttributes.OWNER_OBJECT, searchedNames == null ? new ArrayList<NameI>() : searchedNames,
						filtersOList);

			} else {
				List<FilterResponse> filtersOListTS = new ArrayList<FilterResponse>();
				filtersOListTS.add(new DocTypeAdvancedFilter(searchId));
				filtersOListTS.addAll(filtersOList);

				ArrayList<NameI> searchedNames = super.addNameSearch(modules, serverInfo, SearchAttributes.OWNER_OBJECT, null, filtersOListTS);

				super.addOCRSearch(modules, serverInfo, legalOrDocTypeFilter);

				super.addNameSearch(modules, serverInfo, SearchAttributes.OWNER_OBJECT, searchedNames == null ? new ArrayList<NameI>() : searchedNames,
						filtersOListTS);
			}

//			if (isUpdate) {
//
//				GenericNameFilter nameFilterBuyer = (GenericNameFilter) NameFilterFactory.getDefaultNameFilter(SearchAttributes.BUYER_OBJECT, global.getID(),
//						null);
//				nameFilterBuyer.setIgnoreMiddleOnEmpty(true);
//				nameFilterBuyer.setUseSynonymsBothWays(true);
//				nameFilterBuyer.setInitAgain(true);
//
//				FilterResponse[] filtersB = { nameFilterBuyer, new DocTypeAdvancedFilter(searchId) };
//				super.addNameSearch(modules, serverInfo, SearchAttributes.BUYER_OBJECT, null, Arrays.asList(filtersB));
//			}

			GenericRuntimeIterator<InstrumentI> instrumentIterator = super.getInstrumentIterator(false);

			if (instrumentIterator != null) {
				final int[] REFERECE_IDXS = { TSServerInfo.INSTR_NO_MODULE_IDX, TSServerInfo.BOOK_AND_PAGE_MODULE_IDX };
				for (int moduleIdx : REFERECE_IDXS) {
					module = new TSServerInfoModule(serverInfo.getModule(moduleIdx));
					module.clearSaKeys();
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
		module.setIteratorType(2, FunctionStatesIterator.ITERATOR_TYPE_BOOK_FAKE);
		module.setIteratorType(3, FunctionStatesIterator.ITERATOR_TYPE_PAGE_FAKE);
		module.addIterator(instrumentRelatedBPIterator);
		if (isUpdate()) {
			module.addFilter(BetweenDatesFilterResponse.getDefaultIntervalFilter(searchId));
		}
		module.addFilter(FLSubdividedBasedDASLDT.getLegalOrDocTypeFilter(searchId , true));
		module.addFilter(new PropertyTypeFilter(searchId));
		modules.add(module);
		
		
		//addRelatedInstrumentNoSearch(serverInfo, modules, isUpdate, relatedSourceDoctype);
		
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
		module.setIteratorType(0, FunctionStatesIterator.ITERATOR_TYPE_INSTRUMENT_LIST_FAKE);
		module.setIteratorType(4, FunctionStatesIterator.ITERATOR_TYPE_YEAR);
		module.addIterator(instrumentRelatedNoIterator);
		if (isUpdate) {
			module.addFilter(BetweenDatesFilterResponse.getDefaultIntervalFilter(searchId));
		}
		module.addFilter(FLSubdividedBasedDASLDT.getLegalOrDocTypeFilter(searchId , true));
		module.addFilter(new PropertyTypeFilter(searchId));
		modules.add(module);
	}

	protected String prepareInstNoForReferenceSearch(InstrumentI inst){
		
		String instrNo = inst.getInstno().replaceFirst("^0+", "").trim();
		if (inst.getDate() != null){
			String date = FormatDate.getDateFormat(FormatDate.PATTERN_yyyyMMdd).format(inst.getDate());
			
			if (date.length() > 4){
				date = date.substring(4);
			}
		}
		
		return instrNo;
	}

	protected String prepareBookForReferenceSearch(InstrumentI inst){
		String book = inst.getBook();
		
		if (StateContants.MO_STRING_FIPS.equals(getDataSite().getStateFIPS())){
			if (!book.startsWith("D")){
				book = "D" + book.trim();
			}
			if (CountyConstants.MO_St_Louis_City_STRING.equals(getDataSite().getCountyId())){
				if (!book.endsWith("M")){
					book = book.trim() + "M";
				}
			}
		}
		return book;
	}
	
	protected InstrumentGenericIterator getInstrumentIterator() {
		InstrumentGenericIterator instrumentBPIterator = new InstrumentGenericIterator(searchId, getDataSite()){
			
			private static final long serialVersionUID = 5399334530601258L;

			@Override
			public String getInstrumentNoFrom(InstrumentI state, HashMap<String, String> filterCriteria){
				return correctInstrNo(state.getInstno(), state.getDate());
			}
			@Override
			protected String cleanBook(String input){
				return correctBook(input);
			}

		};

		return instrumentBPIterator;
	}

	protected List<RegisterDocumentI> getGoodDocumentsOrForCurrentOwner(DocumentsManagerI m, Search search, boolean applyNameMatch, int siteType, String stateAbbrev){
		return FLSubdividedBasedDASLDT.getGoodDocumentsOrForCurrentOwner(m, search, applyNameMatch, siteType, stateAbbrev);
	}
	
	private LegalDescriptionIterator getLegalDescriptionIterator(boolean lookupWasDoneWithName) {
		LegalDescriptionIterator it = new LegalDescriptionIterator(searchId);
		
		it.setLoadFromSearchPage(false);
		it.setLoadFromSearchPageIfNoLookup(true);
		return it;
	}
		
	class LegalDescriptionIterator extends GenericRuntimeIterator<PersonalDataStruct> implements LegalDescriptionIteratorI{

			public LegalDescriptionIterator(long searchId) {
				super(searchId);
				setDataSite(MOGenericDTG.this.getDataSite());
			}

			private static final long serialVersionUID = -4741635379234782109L;

			private boolean loadFromSearchPage = true;
			private boolean loadFromSearchPageIfNoLookup = false;
			
			@Override
			public boolean isTransferAllowed(RegisterDocumentI doc) {
				return doc != null && doc.isOneOf(DocumentTypes.TRANSFER);
			}
			
			protected List<PersonalDataStruct> createDerrivations() {

				Search global = InstanceManager.getManager().getCurrentInstance(searchId).getCrtSearchContext();
				DocumentsManagerI m = global.getDocManager();
				List<PersonalDataStruct> legalStructList = (List<PersonalDataStruct>) global.getAdditionalInfo("MO_DG_LOOK_UP_DATA");
				Set<String> multipleLegals = (Set<String>) global.getAdditionalInfo("MO_MULTIPLE_LEGAL_INSTR");
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

				CurrentInstance ci = InstanceManager.getManager().getCurrentInstance(searchId);
				String county = ci.getCurrentCounty().getName();

				boolean first = false;
				if (legalStructList == null){
					first = true;
					legalStructList = new ArrayList<PersonalDataStruct>();

					try {
						m.getAccess();
						List<RegisterDocumentI> listRodocs = getGoodDocumentsOrForCurrentOwner(m, global, true, 35, "MO");

						if (listRodocs == null || listRodocs.size() == 0){
							listRodocs = getGoodDocumentsOrForCurrentOwner(m, global, false, 35, "MO");
						}

						if (listRodocs == null || listRodocs.size() == 0){
							for (DocumentI doc : m.getDocumentsWithDataSource(true, "DG")){
								if (doc instanceof RegisterDocumentI){
									listRodocs.add((RegisterDocumentI) doc);
								}
							}
						}

						DocumentUtils.sortDocuments(listRodocs, MultilineElementsMap.DATE_ORDER_DESC);

						boolean atLeastOneFromDtDg = false;

						for (RegisterDocumentI reg : listRodocs){
							if (!reg.isOneOf(DocumentTypes.PLAT, DocumentTypes.RESTRICTION, DocumentTypes.EASEMENT, DocumentTypes.MASTERDEED,
									DocumentTypes.COURT, DocumentTypes.LIEN, DocumentTypes.CORPORATION, DocumentTypes.AFFIDAVIT, DocumentTypes.CCER)){

								List<PersonalDataStruct> tempLegalStructListPerDocument = new ArrayList<PersonalDataStruct>();
								for (PropertyI prop : reg.getProperties()){
									if (prop.hasLegal()){
										LegalI legal = prop.getLegal();

										if (legal.hasSubdividedLegal()){

											PersonalDataStruct legalStructItem = new PersonalDataStruct("subdivided");
											SubdivisionI subdiv = legal.getSubdivision();

											String block = subdiv.getBlock();
											String lot = subdiv.getLot();
											String lotThrough = subdiv.getLotThrough();
											String subLot = subdiv.getSubLot();
											String platBook = subdiv.getPlatBook();
											String platPage = subdiv.getPlatPage();
											String platInst = subdiv.getPlatInstrument();
											String unit = subdiv.getUnit();
											
											legalStructItem.unit = StringUtils.isEmpty(unit) ? "" : unit;
											legalStructItem.subLot = org.apache.commons.lang.StringUtils.defaultString(subLot);
											legalStructItem.block = StringUtils.isEmpty(block) ? "" : block;
											legalStructItem.lotThrough = StringUtils.isEmpty(lotThrough) ? "" : lotThrough;
											legalStructItem.setPlatBook(StringUtils.isEmpty(platBook) ? "" : platBook);
											legalStructItem.setPlatPage(StringUtils.isEmpty(platPage) ? "" : platPage);
											legalStructItem.platInst = StringUtils.isEmpty(platInst) ? "" : platInst;
											legalStructItem.platInstrYear = subdiv.getPlatInstrumentYear();
											
											String ncb = subdiv.getNcbNumber();
											String subdivisionName = subdiv.getName();
											legalStructItem.setNcbNumber(ncb);
											legalStructItem.setSubdivisionName(subdivisionName);

											if (StringUtils.isEmpty(lot)){
												if (!StringUtils.isEmpty(unit)){
													legalStructItem.lot = unit;
													if (!unit.startsWith("U")){
														PersonalDataStruct legalStructItemA = null;
														try {
															legalStructItemA = (PersonalDataStruct) legalStructItem.clone();
														} catch (CloneNotSupportedException e){
															e.printStackTrace();
														}
														if (legalStructItemA != null){
															legalStructItemA.lot = "U" + unit;
															FLSubdividedBasedDASLDT.preparePersonalStructForCounty(legalStructItemA, searchId);
															multipleLegals.add(reg.prettyPrint());
															tempLegalStructListPerDocument.add(legalStructItemA);
														}
													}
												}
											} else{
												lot = StringUtils.stripStart(lot, "0");
												legalStructItem.lot = lot;
											}

											if (StringUtils.isEmpty(legalStructItem.lot)){// try to complete with the unit from search page
												String unitFromSearchPage = global.getSa().getAtribute(SearchAttributes.LD_SUBDIV_UNIT);
												if (!StringUtils.isEmpty(unitFromSearchPage)){
													unitFromSearchPage = unitFromSearchPage.replace("#", "");
												}
												unitFromSearchPage = StringUtils.stripStart(unitFromSearchPage, "0");
												legalStructItem.lot = unitFromSearchPage;
											}

											FLSubdividedBasedDASLDT.preparePersonalStructForCounty(legalStructItem, searchId);
											multipleLegals.add(reg.prettyPrint());
											tempLegalStructListPerDocument.add(legalStructItem);
										}
										if (legal.hasTownshipLegal()){
											PersonalDataStruct legalStructItem = new PersonalDataStruct("sectional");
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

											legalStructItem.setArb(StringUtils.isEmpty(arb) ? "" : arb);
											legalStructItem.setSection(StringUtils.isEmpty(sec) ? "" : sec);
											legalStructItem.setTownship(StringUtils.isEmpty(tw) ? "" : tw);
											legalStructItem.setRange(StringUtils.isEmpty(rg) ? "" : rg);

											legalStructItem.arbLot = StringUtils.isEmpty(arbLot) ? "" : arbLot;
											legalStructItem.arbBlock = StringUtils.isEmpty(arbBlock) ? "" : arbBlock;
											legalStructItem.arbBook = StringUtils.isEmpty(arbBook) ? "" : arbBook;
											legalStructItem.arbPage = StringUtils.isEmpty(arbPage) ? "" : arbPage;

											legalStructItem.quarterValue = StringUtils.isEmpty(qv) ? "" : qv;
											legalStructItem.quarterOrder = String.valueOf(qo <= 0 ? "" : qo);

											multipleLegals.add(reg.prettyPrint());
											tempLegalStructListPerDocument.add(legalStructItem);
										}
									}
								}
								if (allAoAndTrlots.size() > 0){
									List<PersonalDataStruct> aoAndTrMatches = new ArrayList<PersonalDataStruct>();
									for (PersonalDataStruct item : tempLegalStructListPerDocument){
										if (!StringUtils.isEmpty(item.lot)){
											for (String lt : allAoAndTrlots){
												lt = StringUtils.stripStart(lt, "0");
												if (GenericLegal.computeScoreInternal("lot", lt, item.lot, false, false) >= 0.8
														|| GenericLegal.computeScoreInternal("lot", item.lot, lt, false, false) >= 0.8){
													aoAndTrMatches.add(item);
												}
											}
										}
									}
									if (aoAndTrMatches.size() > 0){
										tempLegalStructListPerDocument.clear();
										tempLegalStructListPerDocument.addAll(aoAndTrMatches);
									}
								}

								if (!atLeastOneFromDtDg && !tempLegalStructListPerDocument.isEmpty()){
									atLeastOneFromDtDg = "DG".equals(reg.getDataSource());
								}

								for (PersonalDataStruct item : tempLegalStructListPerDocument){
									if (!FLSubdividedBasedDASLDT.testIfExist(legalStructList, item, searchId)){
										FLSubdividedBasedDASLDT.addLegalStructAtInterval(legalStructList, item);
									}
								}
							}
						}
						if (isTimeShare){
							for (PersonalDataStruct pers : legalStructList){
								String unit = global.getSa().getAtribute(SearchAttributes.P_STREETUNIT);
								String lot = week;
								String block = unit;
								if (!block.toUpperCase().startsWith("U")){
									block = "U" + block;
								}
								lot = StringUtils.stripStart(lot, "0");
								pers.lot = lot;
								pers.block = block;

								String building = global.getSa().getAtribute(SearchAttributes.BUILDING);
								if (!StringUtils.isEmpty(building)){
									pers.lot = block;
									pers.subLot = lot;
									pers.block = building;
								}
							}

							List<PersonalDataStruct> tempList = new ArrayList<PersonalDataStruct>(legalStructList);
							legalStructList.clear();

							for (PersonalDataStruct legalStructItem : tempList){
								if (!FLSubdividedBasedDASLDT.testIfExist(legalStructList, legalStructItem, searchId)) {
									legalStructList.add(legalStructItem);
								}
							}
						}

						legalStructList = FLSubdividedBasedDASLDT.keepOnlyGoodLegals(legalStructList);

						try {
							DataSite dataSite = HashCountyToIndex.getCrtServer(searchId, false);
							long miServerId = dataSite.getServerId();

							List<PersonalDataStruct> tempLegalStructListPerDocument = new ArrayList<PersonalDataStruct>();

							for (LegalI legal : global.getSa().getForUpdateSearchLegalsNotNull(miServerId)){
								if (legal.hasSubdividedLegal()){

									PersonalDataStruct legalStructItem = new PersonalDataStruct("subdivided");
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
									legalStructItem.unit = StringUtils.isEmpty(unit) ? "" : unit;

									legalStructItem.block = StringUtils.isEmpty(block) ? "" : block;
									legalStructItem.subLot = org.apache.commons.lang.StringUtils.defaultString(sublot);
									legalStructItem.lotThrough = StringUtils.isEmpty(lotThru) ? "" : lotThru;
									legalStructItem.setPlatBook(StringUtils.isEmpty(platBook) ? "" : platBook);
									legalStructItem.setPlatPage(StringUtils.isEmpty(platPage) ? "" : platPage);
									legalStructItem.platInst = StringUtils.isEmpty(platInst) ? "" : platInst;
									legalStructItem.platInstrYear = StringUtils.isEmpty(platInstYear) ? "" : platInstYear;

									if (StringUtils.isEmpty(lot)){
										if (!StringUtils.isEmpty(unit)) {
											unit = StringUtils.stripStart(unit, "0");
											legalStructItem.lot = unit;
											if (!unit.startsWith("U")){
												PersonalDataStruct legalStructItemA = null;
												try {
													legalStructItemA = (PersonalDataStruct) legalStructItem.clone();
												} catch (CloneNotSupportedException e){
													e.printStackTrace();
												}
												if (legalStructItemA != null){
													legalStructItemA.lot = "U" + unit;
													FLSubdividedBasedDASLDT.preparePersonalStructForCounty(legalStructItemA, searchId);
													multipleLegals.add("Saved Search Parameters from Parent Site");
													tempLegalStructListPerDocument.add(legalStructItemA);
												}
											}
										}
									} else {
										lot = StringUtils.stripStart(lot, "0");
										legalStructItem.lot = lot;
									}

									FLSubdividedBasedDASLDT.preparePersonalStructForCounty(legalStructItem, searchId);
									multipleLegals.add("Saved Search Parameters from Parent Site");
									tempLegalStructListPerDocument.add(legalStructItem);
								}

								if (legal.hasTownshipLegal()){
									PersonalDataStruct legalStructItem = new PersonalDataStruct("sectional");
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

									legalStructItem.setArb(StringUtils.isEmpty(arb) ? "" : arb);
									legalStructItem.setSection(StringUtils.isEmpty(sec) ? "" : sec);
									legalStructItem.setTownship(StringUtils.isEmpty(tw) ? "" : tw);
									legalStructItem.setRange(StringUtils.isEmpty(rg) ? "" : rg);
									legalStructItem.quarterValue = StringUtils.isEmpty(qv) ? "" : qv;
									legalStructItem.quarterOrder = String.valueOf(qo <= 0 ? "" : qo);

									legalStructItem.arbLot = StringUtils.isEmpty(arbLot) ? "" : arbLot;
									legalStructItem.arbBlock = StringUtils.isEmpty(arbBlock) ? "" : arbBlock;
									legalStructItem.arbBook = StringUtils.isEmpty(arbBook) ? "" : arbBook;
									legalStructItem.arbPage = StringUtils.isEmpty(arbPage) ? "" : arbPage;

									multipleLegals.add("Saved Search Parameters from Parent Site");
									tempLegalStructListPerDocument.add(legalStructItem);
								}
							}
							for (PersonalDataStruct item : tempLegalStructListPerDocument) {
								if (!FLSubdividedBasedDASLDT.testIfExist(legalStructList, item, searchId)){
									legalStructList.add(item);
								}
							}
						} catch (Exception e) {
							logger.error("Error loading names for Update saved from Parent Site", e);
						}
						legalStructList = FLSubdividedBasedDASLDT.keepOnlyGoodLegals(legalStructList);

						ArrayList<PersonalDataStruct> tempPersonal = new ArrayList<PersonalDataStruct>();

						for (PersonalDataStruct pers : legalStructList){
							String lotsAll = pers.lot;
							if (StringUtils.isNotEmpty(lotsAll)){
								String[] lots = lotsAll.split("[ ,\t\n\r]");

								for (String s : lots){
									try {
										PersonalDataStruct legalStructItem = (PersonalDataStruct) pers.clone();
										s = StringUtils.stripStart(s, "0");
										legalStructItem.lot = s;
										if (!FLSubdividedBasedDASLDT.testIfExist(tempPersonal, legalStructItem, searchId)){
											tempPersonal.add(legalStructItem);
										}
									} catch (CloneNotSupportedException e){
										e.printStackTrace();
									}
								}
							} else{
								if (!FLSubdividedBasedDASLDT.testIfExist(tempPersonal, pers, searchId)){
									tempPersonal.add(pers);
								}
							}
						}

						if (isLoadFromSearchPage() || (legalStructList.isEmpty() && isLoadFromSearchPageIfNoLookup())){
							if (StringUtils.isNotEmpty(platBookAO) && StringUtils.isNotEmpty(platPageAO)){
	
								StringBuilder sb = new StringBuilder();
								HashSet<String> newAllAoAndTrlots = new HashSet<String>();
								for (String lot : allAoAndTrlots){
									if (lot.matches("\\d+")){
										lot = StringUtils.stripStart(lot, "0");
										sb.append(lot).append(" ");
									} else {
										newAllAoAndTrlots.add(lot);
									}
								}
								allAoAndTrlots = new HashSet<String>();
								for (LotInterval interval : LotMatchAlgorithm.prepareLotInterval(sb.toString())){
									int lot = interval.getLow();
									int lotThrough = interval.getHigh();
									if (lot == 0){
										continue;
									}
									PersonalDataStruct struct = new PersonalDataStruct("subdivided");
									struct.lot = Integer.toString(lot);
									if (lot != lotThrough){
										struct.lotThrough = Integer.toString(lotThrough);
									}
									struct.subLot = subLotNo;
									struct.block = aoAndTrBloks;
									struct.setPlatBook(platBookAO);
									struct.setPlatPage(platPageAO);
									if (!FLSubdividedBasedDASLDT.testIfExist(tempPersonal, struct, searchId)){
										tempPersonal.add(struct);
										multipleLegals.add("Search Page");
									}
								}
								allAoAndTrlots.clear();
								allAoAndTrlots.addAll(newAllAoAndTrlots);
	
								for (String lot : allAoAndTrlots){
									PersonalDataStruct struct = new PersonalDataStruct("subdivided");
									lot = StringUtils.stripStart(lot, "0");
									struct.lot = lot;
									struct.subLot = subLotNo;
									struct.block = aoAndTrBloks;
									struct.setPlatBook(platBookAO);
									struct.setPlatPage(platPageAO);
									if (!FLSubdividedBasedDASLDT.testIfExist(tempPersonal, struct, searchId)
											&& !FLSubdividedBasedDASLDT.testIfExistsForCounty(tempPersonal, struct, searchId, county)){
										tempPersonal.add(struct);
										multipleLegals.add("Search Page");
									}
								}
							}
						}
						
						legalStructList = tempPersonal;
						legalStructList = FLSubdividedBasedDASLDT.keepOnlyGoodLegals(legalStructList);

						if (!atLeastOneFromDtDg){
							List<PersonalDataStruct> newIterations = new ArrayList<FLSubdividedBasedDASLDT.PersonalDataStruct>();
							for (PersonalDataStruct pers : legalStructList){
								if (pers.isPlated() && !pers.getPlatBook().startsWith("P")){
									try {
										PersonalDataStruct persClone = (PersonalDataStruct) pers.clone();
										persClone.setPlatBook("P" + persClone.getPlatBook());
										newIterations.add(persClone);
									} catch (CloneNotSupportedException e){
										e.printStackTrace();
									}
								}
							}
							if (!newIterations.isEmpty()){
								for (PersonalDataStruct legalStructItem : newIterations){
									boolean atLeastOneEqual = false;

									for (PersonalDataStruct p : legalStructList){
										if (p.isPlated()){
											FLSubdividedBasedDASLDT.preparePersonalStructForCounty(p, searchId);
											if (legalStructItem.equalsStrictSubdivided(p)){
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

						global.setAdditionalInfo("MO_DG_LOOK_UP_DATA", legalStructList);

						if (legalStructList.size() > 0){
							if (FLSubdividedBasedDASLDT.isPlatedMultyLot(legalStructList)){
								FLSubdividedBasedDASLDT.boostrapSubdividedData(legalStructList, global, true);
							} else{
								FLSubdividedBasedDASLDT.boostrapSubdividedData(legalStructList, global, false);
							}
							if (FLSubdividedBasedDASLDT.isSectionalMultyQv(legalStructList) || FLSubdividedBasedDASLDT.isArbExtended(legalStructList) || legalStructList.size() == 1){
								FLSubdividedBasedDASLDT.boostrapSectionalData(legalStructList, global);
							}
						}
						if (isTimeShare && legalStructList.size() > 0){
							List<DocumentI> docList = m.getDocumentsWithDataSource(false, "DT");
							List<String> docIds = new ArrayList<String>();
							for (DocumentI doc : docList){
								docIds.add(doc.getId());
							}
							m.remove(docIds);
						}
					} finally {
						m.releaseAccess();
					}
				}
				if (legalStructList.size() > 1
						&& !(FLSubdividedBasedDASLDT.isPlatedMultyLot(legalStructList) 
								|| FLSubdividedBasedDASLDT.isSectionalMultyQv(legalStructList) 
								|| FLSubdividedBasedDASLDT.isPlatedMultyLotAndIsSectionalMultyQV(legalStructList))){
					
					global.getSa().setAtribute(SearchAttributes.ATS_MULTIPLE_LEGALS_FOUND, "true");
					global.getSa().setAtribute(SearchAttributes.ATS_MULTIPLE_LEGAL_INSTRUMENTS, multipleLegals.toString());
					global.setAdditionalInfo("MO_MULTIPLE_LEGAL_INSTR", multipleLegals);
					if (first){
						SearchLogger.info("<br/><b>Questionable multiple legals found in " + multipleLegals.toString() + "</b>", searchId);
					}
				}

				if (FLSubdividedBasedDASLDT.isPlatedMultyLot(legalStructList) && allAoAndTrlots.size() > 0 && !isTimeShare){
					FLSubdividedBasedDASLDT.addLegalStructItemsUsingAoAndTrLots(legalStructList, allAoAndTrlots.toArray(new String[allAoAndTrlots.size()]), searchId, true);
				}

				// expand legals by Sublot
				legalStructList = expandLegalStructItemsSublot(legalStructList, searchId);

				return legalStructList;
			}
			
			private List<PersonalDataStruct> expandLegalStructItemsSublot(List<PersonalDataStruct> legalStructList, long searchId){
				List<PersonalDataStruct> res = new ArrayList<FLSubdividedBasedDASLDT.PersonalDataStruct>();
				for (PersonalDataStruct pds : legalStructList){
					if (StringUtils.isNotEmpty(pds.subLot)){
						PersonalDataStruct p;
						try {
							p = (PersonalDataStruct) pds.clone();
							p.subLot = "";
							res.add(p);
						} catch (Exception e){
							e.printStackTrace();
						}
					}
					res.add(pds);
				}
				res = FLSubdividedBasedDASLDT.keepOnlyGoodLegals(res);
				
				return res;
			}
			
			protected void loadDerrivation(TSServerInfoModule module, PersonalDataStruct str){
				
				switch(module.getModuleIdx()){
					case TSServerInfo.SUBDIVISION_MODULE_IDX:
						module.setData(2, str.lot);
						module.setData(3, str.block);
						
						if (StringUtils.isNotEmpty(str.lotThrough)){
							module.setData(20, str.lotThrough);
						}
						
						if (dataSite.getCountyId() == CountyConstants.MO_St_Louis_City){
							module.setData(12, str.subdivisionName);
							module.setData(21, str.ncbNumber);
						} else{
							module.setData(4, str.getPlatBook());
							module.setData(5, str.getPlatPage());
							module.setData(6, str.platInst);
							module.forceValue(10, str.subLot);
							if(StringUtils.isNotEmpty(str.platInstrYear)){
								module.forceValue(11, str.platInstrYear);
							}
						}
					break;
					case TSServerInfo.SECTION_LAND_MODULE_IDX:
						module.setData(0, str.getSection());
						module.setData(1, str.getTownship());
						module.setData(2, str.getRange());
						module.setData(3, str.quarterOrder);
						module.setData(4, str.quarterValue);
					break;
					case TSServerInfo.ARB_MODULE_IDX:{
						if(org.apache.commons.lang.StringUtils.isNotBlank(str.arbBook)){
							module.setData(0, str.arbBook);
							module.setData(1, str.arbPage);
							module.setData(2, str.arbBlock);
							module.setData(3, str.arbLot);
						} else{
							module.setData(0, str.getSection());
							module.setData(1, str.getTownship());
							module.setData(2, str.getRange());
							module.setData(3, str.quarterOrder);
							module.setData(4, str.quarterValue);
							module.setData(5, str.getArb());
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
			 * Please check the other flag {@link LegalDescriptionIterator.isLoadFromSearchPage} to turn off default search page
			 * @return
			 */
			public void setLoadFromSearchPageIfNoLookup(boolean loadFromSearchPageIfNoLookup) {
				this.loadFromSearchPageIfNoLookup = loadFromSearchPageIfNoLookup;
			}

			@Override
			public void loadSecondaryPlattedLegal(LegalI legal, LegalStruct legalStruct) {
				// TODO Auto-generated method stub
				
			}
		}
	
	private PlatIterator getPlatBookPageIterator(long searchId, DTLikeAutomatic dtLike) {
		PlatIterator it = new PlatIterator(searchId, dtLike) {
			
			private String extraStringForsection = "";
			
			private static final long serialVersionUID = 823434519L;
			
			protected List<PersonalDataStruct> createDerrivations(){
				LegalDescriptionIterator ldit = new LegalDescriptionIterator(searchId);
				
				List<PersonalDataStruct> list = ldit.createDerrivations();
				if(list.size() == 0){
					list.add(new PersonalDataStruct(""));
				}
				
				List<PersonalDataStruct>  list1 = new ArrayList<PersonalDataStruct>();
				for (PersonalDataStruct p : list){
					boolean alreadyContained = false;
					for (PersonalDataStruct p1 : list1){
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
			
			protected void loadDerrivation(TSServerInfoModule module, PersonalDataStruct str){
				
				switch(module.getModuleIdx()){
					case TSServerInfo.BOOK_AND_PAGE_LOCAL_MODULE_IDX:
						if(module.getSaObjKey().equals(SearchAttributes.LD_BOOKNO)){
							dtLike.addPlatMapSearch(module,str);
						}else if(module.getSaObjKey().equals(SearchAttributes.LD_BOOKNO_1)){
							dtLike.addAssesorMapSearch(module, str, extraStringForsection);
						}else if(module.getSaObjKey().equals(SearchAttributes.LD_SUBDIVISION)){
							dtLike.addSubdivisionMapSearch(module,str);
						}else if(module.getSaObjKey().equals(SearchAttributes.LD_PARCELNO_MAP)){
							dtLike.addParcelMapSearch(module, str);
						}else if(module.getSaObjKey().equals(SearchAttributes.LD_PARCELNO_CONDO)){
							dtLike.addCondoMapSearch(module, str);
						}
					break;
				}
			}

			public void setExtraStringForSection(String str) {
				extraStringForsection = str;
			}
			
		};
		
		return it;
	}

	@Override
	public InstrumentGenericIterator getInstrumentIterator(boolean instrumentType) {
		InstrumentGenericIterator instrumentGenericIterator = new InstrumentGenericIterator(searchId, getDataSite()) {

			private static final long serialVersionUID = 5399351945130601258L;

			@Override
			public String getInstrumentNoFrom(InstrumentI state, HashMap<String, String> filterCriteria){
				String instrNo = state.getInstno().replaceFirst("^0+", "").trim();
				if (StringUtils.isNotEmpty(instrNo)){
					String date = "";
					
					if (state.getDate() != null){
						date = FormatDate.getDateFormat(FormatDate.PATTERN_MM_SLASH_DD_SLASH_YYYY).format(state.getDate());
						if (instrNo.startsWith(date)){
							return StringUtils.stripStart(instrNo.replaceFirst("(?is)\\A" + date, ""), "0");
						}
					}
				}
				
				return "";
			}
			@Override
			protected String cleanBook(String input){
				if (StateContants.MO_STRING_FIPS.equals(getDataSite().getStateFIPS())){
					if (!input.startsWith("D")){
						input = "D" + input.trim();
					}
					if (CountyConstants.MO_St_Louis_City_STRING.equals(getDataSite().getCountyId())){
						if (!input.endsWith("M")){
							input = input.trim() + "M";
						}
					}
				}
				return input;
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
			public String getInstrumentNoFrom(InstrumentI state, HashMap<String, String> filterCriteria){
				String instrNo = state.getInstno().replaceFirst("^0+", "").trim();
				
				if (StringUtils.isNotEmpty(instrNo)){
					String date = "";
					
					if (state.getDate() != null){
						FormatDate.getDateFormat(FormatDate.PATTERN_yyyyMMdd).format(state.getDate());
						if (date.length() > 4){
							date = date.substring(4);
						}
					}
					return date + StringUtils.leftPad(instrNo, 4, "0");
				}
				return "";
			}
			@Override
			protected String cleanBook(String input){
				if (StateContants.MO_STRING_FIPS.equals(getDataSite().getStateFIPS())){
					if (!input.startsWith("D")){
						input = "D" + input.trim();
					}
					if (CountyConstants.MO_St_Louis_City_STRING.equals(getDataSite().getCountyId())){
						if (!input.endsWith("M")){
							input = input.trim() + "M";
						}
					}
				}
				return input;
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
		String doctype = documentToCheckCopy.getDocType();
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
			String bookToCheck = documentToCheck.getBook();
			
			if (StringUtils.isNotEmpty(bookToCheck)){
				if (StateContants.MO_STRING_FIPS.equals(getDataSite().getStateFIPS())){
					if (bookToCheck.startsWith("D")){
						bookToCheck = bookToCheck.replaceFirst("(?is)\\AD", "");
					}
					if (CountyConstants.MO_St_Louis_City_STRING.equals(getDataSite().getCountyId())){
						if (bookToCheck.endsWith("M")){
							bookToCheck = bookToCheck.replaceFirst("(?is)M$", "");
						}
					}
				}
			}
				
			List<DocumentI> allRODocuments = documentManager.getDocumentsWithDataSource(false, getDataSite().getSiteTypeAbrev());
				
			for (DocumentI documentI : allRODocuments) {
				if (org.apache.commons.lang.StringUtils.isNotEmpty(documentI.getBook()) && org.apache.commons.lang.StringUtils.isNotEmpty(bookToCheck) 
							&& documentI.getBook().equals(bookToCheck)
					   && org.apache.commons.lang.StringUtils.isNotEmpty(documentI.getPage()) && org.apache.commons.lang.StringUtils.isNotEmpty(documentToCheck.getPage()) 
					   		&& documentI.getPage().equals(documentToCheck.getPage())) {
						
					alike.add(documentI);
				}  else if (documentI.getInstno().equals(documentToCheck.getInstno())){
					alike.add(documentI);
				}
			}
		}
		return alike;
	}
	
	protected boolean addInstNoSearch(InstrumentI inst, TSServerInfo serverInfo, List<TSServerInfoModule> modules, long searchId, Set<String> searched,
			boolean isUpdate) {
		if (inst.hasInstrNo()) {
			String instr = inst.getInstno().replaceFirst("^0+", "");

			String year1 = String.valueOf(inst.getYear());
			if (!searched.contains(instr + year1)) {
				searched.add(instr + year1);
			} else {
				return false;
			}

			TSServerInfoModule module = new TSServerInfoModule(serverInfo.getModule(TSServerInfo.INSTR_NO_MODULE_IDX));
			module.setData(0, instr);
			if (isUpdate) {
				module.addFilter(new BetweenDatesFilterResponse(searchId));
			}
			module.setData(1, year1);
			modules.add(module);
			return true;
		}
		return false;
	}
	
	@Override
	protected DownloadImageResult saveImage(ImageI image)throws ServerResponseException {
		HashMap<String, String> map = HttpUtils.getParamsFromLink(image.getLink(0));
		
		String book 	 = map.get("book");
		String page 	 = map.get("page");
		String docNumber = map.get("instr");
		String year 	 = map.get("year");
		String type 	 = StringUtils.defaultString(map.get("type"));	
    	String month 	 = map.get("month");
    	String day 		 = map.get("day");
		String dataTreeIndexStr = StringUtils.defaultString(map.get("dataTreeIndex"));
    	String dataTreeDesc =  StringUtils.defaultString(map.get("dataTreeDesc"));
    	String isFake =  StringUtils.defaultString(map.get("isFake"));
    	String dataTreeDocType =  StringUtils.defaultString(map.get("dataTreeDocType"));
    	
    	if (StateContants.MO_STRING_FIPS.equals(getDataSite().getStateFIPS())){
			if (StringUtils.isNotEmpty(book)){ 
				if (book.startsWith("D")){
					book = book.replaceFirst("(?is)\\AD", "");
				}
				if (CountyConstants.MO_St_Louis_City_STRING.equals(getDataSite().getCountyId())){
					if (book.endsWith("M")){
						book = book.replaceFirst("(?is)M$", "");
					}
				}
			}
		}
    	
    	if (StringUtils.isNotEmpty(docNumber)){
    		docNumber = StringUtils.stripStart(docNumber, "0");
    	}
    	if (StringUtils.isEmpty(isFake)){
    		isFake = "false";
    	}
    	int yearInt = -1, monthInt = -1;
    	if (StringUtils.isNotEmpty(year)){
	    	try {
				yearInt = Integer.parseInt(year);
			} catch (NumberFormatException e) {
				e.printStackTrace();
			}
    	}
    	if (StringUtils.isNotEmpty(month)){
			try {
				monthInt = Integer.parseInt(month);
			} catch (NumberFormatException e) {
				e.printStackTrace();
			}
    	}  	
		
    	int dataTreeIndex = -1;
    	try{dataTreeIndex = Integer.parseInt(dataTreeIndexStr);}catch(Exception e){};
    	
    	boolean savedImage = false;
    	
    	docNumber = prepareDocumentNoForImageSearch(docNumber, year, month, day);

		if(datTreeList == null){
			datTreeList = initDataTreeStruct();
		}
				
		InstrumentI i = new Instrument();
		
		if (docNumber.matches("\\d+-\\d+-\\d+")){
			String[] str = docNumber.split("-");
			if (str.length == 3){
				book = str[0];
				page = str[1];
				docNumber = str[2];
			}
		}
		i.setBook(book);
		i.setPage(page);
		i.setInstno(docNumber);
		i.setYear(yearInt);
		i.setDocType(type);
		
		if ("false".equalsIgnoreCase(isFake)){
//			if (!savedImage){
//				try {
//					savedImage = FLGenericDASLDT.downloadImageFromPropertyInsight(image.getPath(), FLGenericDASLDT.getPiQuery(i, searchId), searchId).success;
//					if (savedImage){
//						SearchLogger.info("<br/>Image(searchId=" + searchId + " )book=" + i.getBook() + "page=" + i.getPage() + "inst=" + instrDesc +" was taken from PI.<br/>", searchId);
//					} else{
//						SearchLogger.info(
//								"<br/>FAILED to take Image(searchId=" + searchId + " ) book=" + i.getBook() + " page=" + i.getPage() + " inst=" + instrDesc + " from PI.<br/>", searchId); 
//					}
//				} catch (IOException e) {
//					e.printStackTrace();
//				}
//			}
		}

		if ("true".equalsIgnoreCase(isFake)){
			byte[] b = new byte[0];
			
			InstrumentI instr = i.clone();
			instr.setDocType(dataTreeDocType);
			String fileName = FLSubdividedBasedDASLDT.createTempSaveFileName(instr, getCrtSearchDir());
			try {
				b = FileUtils.readFileToByteArray(new File(fileName));
			} catch (IOException e) {e.printStackTrace();}
			
			if (b.length > 0){
				if (StringUtils.isNotEmpty(image.getPath()) && !image.getPath().contains("temp")){
					try {
						org.apache.commons.io.FileUtils.writeByteArrayToFile(new File(image.getPath()), b);
					} catch (IOException e) {
						e.printStackTrace();
					}
				} else{
					image.setPath(fileName);
				}
				return new DownloadImageResult(Status.OK, b, image.getContentType());
			}
			
		}
		if (!savedImage){
			if (dataTreeIndex > 0){
				DataTreeAccount acc = DataTreeManager.getDatatTreeAccount(String.valueOf(getCommunityId()));
				try {
					SimpleImage im = null;
					try {
						im = DataTreeConn.retrieveImage(acc, dataTreeIndex, dataTreeDesc, 999, 0);
					} catch (DataTreeImageException e) {
						e.printStackTrace();
						SearchLogger.info(
								"<br/>FAILED to take Image(searchId=" + searchId + " ) book=" + i.getBook() + " page=" + i.getPage() + " inst=" + i.getInstno() + " from DataTree. " + 
								"Official Server Message: [" + e.getLocalizedMessage() + " (" + e.getStatus() + ") ]<br/>", searchId);
					}
					if (im != null){
						FileUtils.writeByteArrayToFile(new File(image.getPath()), im.getContent());
						logger.info("------------------------ downloadImageFromDataTree return true for instr=" + i + " savePath=" + image.getPath());
						savedImage = true;
						
						if (savedImage){
							SearchLogger.info("<br/>Image(searchId="+searchId+" )book=" + i.getBook() + "page=" + i.getPage() + "inst=" + i.getInstno()+" was taken from DataTree<br/>", searchId);
						}
						afterDownloadImage(savedImage, GWTDataSite.DG_TYPE);
					}
				} catch (IOException e) {
					e.printStackTrace();
				}
			} else{
				try {
					savedImage= FLGenericDASLDT.downloadImageFromDataTree(i, datTreeList, image.getPath(),getCommunityId() + "", null, null);
					if (savedImage){
						afterDownloadImage(savedImage, GWTDataSite.DG_TYPE);
						SearchLogger.info("<br/>Image(searchId=" + searchId + ") for book=" + i.getBook() + " page=" + 
								i.getPage() + "; inst=" + i.getInstno() + " year=" + year + " was taken from DataTree<br/>", searchId);
					}
				} catch (DataTreeImageException e) {
					logger.error("Error while getting image ", e);
					SearchLogger.info(
							"<br/>FAILED to take Image(searchId=" + searchId + ") for book=" + i.getBook() + " page=" + i.getPage()
							+ "; inst=" + i.getInstno() + " year=" + year + " from DataTree. " +
							"Official Server Message: [" + e.getLocalizedMessage() + " (" + e.getStatus() + ") ]<br/>", searchId);
				}
			}
		}
		
		byte[] b = new byte[0];
		try {
			File file = new File(image.getPath());
			if(file.exists()) {
				b = FileUtils.readFileToByteArray(new File(image.getPath()));
			}
		} catch (IOException e) {e.printStackTrace();}
		
		if (b.length > 0){
			return new DownloadImageResult(Status.OK, b, image.getContentType());
		}
		
		return new DownloadImageResult(Status.ERROR, b, image.getContentType());
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
	
	@Override
	protected TSServer.ADD_DOCUMENT_RESULT_TYPES addDocumentInATS(ServerResponse response, String htmlContent, boolean forceOverritten){
    	ParsedResponse pr = response.getParsedResponse();
    	DocumentsManagerI manager = mSearch.getDocManager();
    	DocumentI doc = pr.getDocument();
    	if(doc instanceof RegisterDocumentI){
	    	RegisterDocumentI regDoc = (RegisterDocumentI)doc;
    		try{
	         	manager.getAccess();
	         	if (regDoc != null){
	         		DocumentI origDoc = manager.getDocument(regDoc);
	         		if(origDoc instanceof RegisterDocumentI){
		         		RegisterDocumentI origRegDoc = (RegisterDocumentI)origDoc;
		         		if (origRegDoc != null){
		         			if(origRegDoc.getParsedReferences().size() < regDoc.getParsedReferences().size()){
		         				forceOverritten = true;
		         			}
		         		}
	         		}
	         	}
	    	 }finally{
	    		 manager.releaseAccess();
	    	 }
    	}
    	return super.addDocumentInATS(response, htmlContent,forceOverritten);
    }
	
	@Override
	protected Map<InstrumentI, DocumentI> processRelated(TSServerInfoModule module, ServerResponse serverResponse) {
		
		Map<InstrumentI, DocumentI> temporaryDocuments = new HashMap<InstrumentI, DocumentI>();
		
		if(isRelatedModule(module)) {
			
			String modBook = null;
			String modPage = null;
			String modInstNo = null;
			
			boolean bookPage = org.apache.commons.lang.StringUtils.isNotBlank(module.getFunction(2).getParamValue())
					&& org.apache.commons.lang.StringUtils.isNotBlank(module.getFunction(3).getParamValue());
			boolean instrNo = org.apache.commons.lang.StringUtils.isNotBlank(module.getFunction(0).getParamValue());
			
			if(bookPage) {
				modBook = module.getFunction(2).getParamValue().trim();
				modPage = module.getFunction(3).getParamValue().trim();
				
				//St. Louis, St. Louis City
				modBook = modBook.replaceAll("(?is)[DM]+", "");
			}
			if(instrNo) {
				modInstNo = module.getFunction(0).getParamValue().trim();
				
				//St. Louis, St. Louis City
				int numberLength = modInstNo.length();
				if (numberLength > 4){
					modInstNo = modInstNo.substring(numberLength - 4);
					modInstNo = StringUtils.stripStart(modInstNo, "0");
				}
			}
			
			if(bookPage || instrNo) {
			
				@SuppressWarnings("rawtypes")
				Vector resultRows = serverResponse.getParsedResponse().getResultRows();
				if(resultRows != null && !resultRows.isEmpty()) {
					for (Object object : resultRows) {
						if (object instanceof ParsedResponse) {
							ParsedResponse parsedResponse = (ParsedResponse) object;
							DocumentI document = parsedResponse.getDocument();
							
							Set<InstrumentI> parsedReferences = document.getParsedReferences();
							boolean foundReference = false;
							if(parsedReferences != null && !parsedReferences.isEmpty()) {
								for (InstrumentI instrumentI : parsedReferences) {
									if(bookPage) {
										if(modBook.equals(instrumentI.getBook()) && modPage.equals(instrumentI.getPage())) {
											foundReference = true;
											break;
										}
									}
									if(instrNo) {
										if(modInstNo.equals(instrumentI.getInstno())) {
											foundReference = true;
											break;
										}
									}
								}
							}
							if(!foundReference) {
								InstrumentI newReference = new Instrument();
								if(bookPage) {
									newReference.setBook(modBook);
									newReference.setPage(modPage);
								}
								if(instrNo) {
									newReference.setInstno(modInstNo);
								}
								if(org.apache.commons.lang.StringUtils.isNotBlank(module.getFunction(4).getParamValue())) {
									try {
										newReference.setYear(Integer.parseInt(module.getFunction(4).getParamValue().trim()));
									} catch (Exception e) {
									}
								}
								document.addParsedReference(newReference);
								temporaryDocuments.put(document.getInstrument(), document);
							}
						}
					}
				}
			}
		}
		
		return temporaryDocuments;
	}
	
	protected boolean isRelatedModule(TSServerInfoModule firstModule) {
		return firstModule.getModuleIdx() == TSServerInfo.MODULE_IDX41 && firstModule.getFunctionCount() == 6;
	}
	
	@Override
	public void addDocumentAdditionalPostProcessing(DocumentI doc, ServerResponse response){
		super.addDocumentAdditionalPostProcessing(doc, response);

		DocumentsManagerI manager = getSearch().getDocManager();
		try {
			manager.getAccess();
			if (manager.contains(doc)) {
				if (doc instanceof RegisterDocumentI){
					RegisterDocumentI regDoc = (RegisterDocumentI)doc;
					if (regDoc.isOneOf("TRANSFER") && SearchType.GI == regDoc.getSearchType()){
						regDoc.setChecked(false);
						regDoc.setIncludeImage(false);
					}
				}
			}
		} catch (Throwable t) {
			logger.error("Error while post processing document", t);
		} finally {
			manager.releaseAccess();
		}
		
	}
	
	public String correctInstrNo(String instrNo, Date recordedDate) {
		instrNo = instrNo.replaceFirst("^0+", "").trim();
		String date = FormatDate.getDateFormat(FormatDate.PATTERN_yyyyMMdd).format(recordedDate);
		if (date.length() > 4){
			date = date.substring(4);
		}
		return date + StringUtils.leftPad(instrNo, 4, "0");
	}
	
	public String correctBook(String input) {
		if (StateContants.MO_STRING_FIPS.equals(getDataSite().getStateFIPS())){
			if (!input.startsWith("D")){
				input = "D" + input.trim();
			}
			if (CountyConstants.MO_St_Louis_City_STRING.equals(getDataSite().getCountyId())){
				if (!input.endsWith("M")){
					input = input.trim() + "M";
				}
			}
		}
		return input;
	}
	
	@Override
	public Object getRecoverModuleFrom(RestoreDocumentDataI restoreDocumentDataI) {
		if(restoreDocumentDataI == null) {
			return null;
		}
		
		List<TSServerInfoModule> list = new ArrayList<TSServerInfoModule>();
		
		String book = restoreDocumentDataI.getBook();
		String page = restoreDocumentDataI.getPage();
		String instrNo = restoreDocumentDataI.getInstrumentNumber();
		Date recordedDate = restoreDocumentDataI.getRecordedDate();
		TSServerInfoModule module = null;
		
		if(ro.cst.tsearch.utils.StringUtils.isNotEmpty(book) && ro.cst.tsearch.utils.StringUtils.isNotEmpty(page)) {
			module = getDefaultServerInfo().getModule(TSServerInfo.BOOK_AND_PAGE_MODULE_IDX);
			module.forceValue(0, correctBook(book));
			module.forceValue(1, page);
			module.forceValue(2, "INVESTIGATIVE");
			list.add(module);
		}
		
		if(ro.cst.tsearch.utils.StringUtils.isNotEmpty(instrNo)	&& recordedDate != null) {
			Calendar cal = Calendar.getInstance();
			cal.setTime(recordedDate);
			
			module = getDefaultServerInfo().getModule(TSServerInfo.INSTR_NO_MODULE_IDX);
			module.forceValue(0, correctInstrNo(instrNo, recordedDate));
			module.forceValue(1, Integer.toString(cal.get(Calendar.YEAR)));
			module.forceValue(2, "INVESTIGATIVE");
			list.add(module);
		}
		
		module = new TSServerInfoModule(0, TSServerInfo.FAKE_MODULE_IDX);
		module.addExtraInformation(TSServerInfoConstants.EXTRA_PARAM_RESTORE_DOCUMENT_SOURCE, restoreDocumentDataI);
		list.add(module);
		
		return list;
	}
	@Override
	protected String formatBook(String book){
		
		if (StateContants.MO_STRING_FIPS.equals(getDataSite().getStateFIPS())){
			if (!book.startsWith("D")){
				book = "D" + book.trim();
			}
			if (CountyConstants.MO_St_Louis_City_STRING.equals(getDataSite().getCountyId())){
				if (!book.endsWith("M")){
					book = book.trim() + "M";
				}
			}
		}
		return book;
	}
}
