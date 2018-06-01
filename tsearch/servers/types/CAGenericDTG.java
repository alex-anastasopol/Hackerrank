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
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Vector;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.StringUtils;

import ro.cst.tsearch.Search;
import ro.cst.tsearch.bean.SearchAttributes;
import ro.cst.tsearch.data.CountyConstants;
import ro.cst.tsearch.datatrace.DTRecord;
import ro.cst.tsearch.exceptions.ServerResponseException;
import ro.cst.tsearch.search.FunctionStatesIterator;
import ro.cst.tsearch.search.GenericRuntimeIterator;
import ro.cst.tsearch.search.filter.BetweenDatesFilterResponse;
import ro.cst.tsearch.search.filter.ExactDateFilterResponse;
import ro.cst.tsearch.search.filter.FilterResponse;
import ro.cst.tsearch.search.filter.RejectAlreadySavedDocumentsFilterResponse;
import ro.cst.tsearch.search.filter.RejectAlreadySavedDocumentsForUpdateFilter;
import ro.cst.tsearch.search.filter.matchers.algorithm.LotInterval;
import ro.cst.tsearch.search.filter.matchers.algorithm.LotMatchAlgorithm;
import ro.cst.tsearch.search.filter.newfilters.date.LastTransferDateFilter;
import ro.cst.tsearch.search.filter.newfilters.doctype.DocTypeAdvancedFilter;
import ro.cst.tsearch.search.filter.newfilters.doctype.DoctypeFilterFactory;
import ro.cst.tsearch.search.filter.newfilters.legal.GenericLegal;
import ro.cst.tsearch.search.filter.newfilters.legal.LegalFilterFactory;
import ro.cst.tsearch.search.filter.newfilters.name.GenericNameFilter;
import ro.cst.tsearch.search.filter.newfilters.name.NameFilterFactory;
import ro.cst.tsearch.search.filter.newfilters.pin.PinFilterResponse;
import ro.cst.tsearch.search.iterator.data.LegalStruct;
import ro.cst.tsearch.search.iterator.data.LegalStructDTG;
import ro.cst.tsearch.search.iterator.instrument.InstrumentGenericIterator;
import ro.cst.tsearch.search.iterator.legal.LegalDescriptionIteratorI;
import ro.cst.tsearch.search.util.AdditionalInfoKeys;
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
import ro.cst.tsearch.servers.types.FLSubdividedBasedDASLDT.PropertyTypeFilter;
import ro.cst.tsearch.servers.types.TSInterface.DownloadImageResult.Status;
import ro.cst.tsearch.templates.MultilineElementsMap;
import ro.cst.tsearch.titledocument.abstracts.DocumentTypes;
import ro.cst.tsearch.utils.FormatDate;
import ro.cst.tsearch.utils.HttpUtils;
import ro.cst.tsearch.utils.InstanceManager;
import ro.cst.tsearch.utils.SearchLogger;

import com.stewart.ats.base.document.DocumentI;
import com.stewart.ats.base.document.DocumentUtils;
import com.stewart.ats.base.document.ImageI;
import com.stewart.ats.base.document.Instrument;
import com.stewart.ats.base.document.InstrumentI;
import com.stewart.ats.base.document.RegisterDocument;
import com.stewart.ats.base.document.RegisterDocumentI;
import com.stewart.ats.base.document.RestoreDocumentDataI;
import com.stewart.ats.base.document.TransferI;
import com.stewart.ats.base.legal.Legal;
import com.stewart.ats.base.legal.LegalI;
import com.stewart.ats.base.legal.Subdivision;
import com.stewart.ats.base.legal.SubdivisionI;
import com.stewart.ats.base.legal.TownShip;
import com.stewart.ats.base.legal.TownShipI;
import com.stewart.ats.base.name.NameI;
import com.stewart.ats.base.name.NameSourceType;
import com.stewart.ats.base.parties.PartyI;
import com.stewart.ats.base.property.PropertyI;
import com.stewart.ats.base.search.DocumentsManagerI;
import com.stewart.ats.tsrindex.client.SimpleChapterUtils;
import com.stewart.ats.tsrindex.client.SimpleChapterUtils.PType;
import com.stewart.datatree.DataTreeAccount;
import com.stewart.datatree.DataTreeConn;
import com.stewart.datatree.DataTreeImageException;
import com.stewart.datatree.DataTreeManager;
import com.stewart.datatree.SimpleImage;

/**
 * @author mihaib
 * 
 */

public class CAGenericDTG extends TSServerDTG implements DTLikeAutomatic {

	public static final String CA_MULTIPLE_LEGAL_INSTR = "CA_MULTIPLE_LEGAL_INSTR";
	
	private static final long serialVersionUID = -39475262378410L;

	public CAGenericDTG(String rsRequestSolverName, String rsSitePath, String rsServerID, String rsPrmNameLink, long searchId, int mid)
			throws FileNotFoundException, IOException {
		super(rsRequestSolverName, rsSitePath, rsServerID, rsPrmNameLink, searchId, mid);
		resultType = MULTIPLE_RESULT_TYPE;
		setUsePlatInstrumentInsteadOfPlatForCounty();
	}

	public CAGenericDTG(long searchId) {
		super(searchId);
		setUsePlatInstrumentInsteadOfPlatForCounty();
	}

	public void setUsePlatInstrumentInsteadOfPlatForCounty(){
		if (dataSite.getCountyId() == CountyConstants.CA_Orange){
			setUsePlatInstrumentInsteadOfPlat(true);
		} else{
			setUsePlatInstrumentInsteadOfPlat(false);
		}
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
			
			LastTransferDateFilter ltdf = new LastTransferDateFilter(searchId);
			ltdf.setUseDefaultDocTypeThatPassForGoodName(true);
			
			RejectAlreadySavedDocumentsForUpdateFilter rejectAlreadySavedDocumentsForUpdateFilter = null;
	    	if(isUpdate()) {
	    		rejectAlreadySavedDocumentsForUpdateFilter = new RejectAlreadySavedDocumentsForUpdateFilter(searchId);
	    	}
			
			DocTypeAdvancedFilter docTypeOwnerAdvancedFilter = DoctypeFilterFactory.getDoctypeFilterForGeneralIndexOwnerNameSearch( searchId )
					.setForcePassIfNoReferences(true)
					.setIsUpdate(isUpdate());
			
			FilterResponse[] filtersO = { 
					nameFilterOwner, 
//					legalOrDocTypeFilter, 
//					pinFilter, 
					docTypeOwnerAdvancedFilter,
					ltdf,
					rejectAlreadySavedDocumentsForUpdateFilter
//					new PropertyTypeFilter(searchId) 
					};
			List<FilterResponse> filtersOList = Arrays.asList(filtersO);

			FilterResponse[] filtersCrossrefs = { legalOrDocTypeFilter, pinFilter, new PropertyTypeFilter(searchId) };
			
			InstrumentGenericIterator instrumentGenericIterator = null;
			InstrumentGenericIterator bpGenericIterator = null;
			{
				instrumentGenericIterator = getInstrumentIterator(true);
				instrumentGenericIterator.setDoNotCheckIfItExists(true);

				module = new TSServerInfoModule(serverInfo.getModule(TSServerInfo.INSTR_NO_MODULE_IDX));
				module.addExtraInformation(TSServerInfoConstants.EXTRA_PARAM_MODULE_DESCRIPTION, "Search with references from AO/Tax like documents");
				module.setIteratorType(0, FunctionStatesIterator.ITERATOR_TYPE_INSTRUMENT_LIST_FAKE);
				module.setIteratorType(1, FunctionStatesIterator.ITERATOR_TYPE_YEAR);
				if (isUpdate) {
					module.addFilter(new BetweenDatesFilterResponse(searchId));
				}
				module.addFilter(new ExactDateFilterResponse(searchId));
				module.addIterator(instrumentGenericIterator);
				if (!lookupWithInstrument) {
					lookupWithInstrument = !instrumentGenericIterator.createDerrivations().isEmpty();
				}
				modules.add(module);
			}
			{
				bpGenericIterator = getInstrumentIterator(false);
				bpGenericIterator.setRemoveLeadingZerosBP(true);
				bpGenericIterator.setDoNotCheckIfItExists(true);

				module = new TSServerInfoModule(serverInfo.getModule(TSServerInfo.BOOK_AND_PAGE_MODULE_IDX));
				module.addExtraInformation(TSServerInfoConstants.EXTRA_PARAM_MODULE_DESCRIPTION, "Search with references from AO/Tax like documents");
				module.setIteratorType(0, FunctionStatesIterator.ITERATOR_TYPE_BOOK_FAKE);
				module.setIteratorType(1, FunctionStatesIterator.ITERATOR_TYPE_PAGE_FAKE);

				if (isUpdate) {
					module.addFilter(new BetweenDatesFilterResponse(searchId));
				}
				module.addFilter(new ExactDateFilterResponse(searchId));
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
				grantorGranteeFilterList.add(ltdf);
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
				module.setData(0, prepareApnPerCounty(searchId));
				if (isUpdate) {
					module.addFilter(new BetweenDatesFilterResponse(searchId));
				}
				if (pinFilter != null){
					module.addFilter(pinFilter);
				}
				modules.add(module);
				

//				ArrayList<NameI> searchedNames = super.addNameSearch(modules, serverInfo, SearchAttributes.OWNER_OBJECT, null, filtersOList);

				super.addOCRSearch(modules, serverInfo, legalOrDocTypeFilter);
				super.addNameSearch(modules, serverInfo, SearchAttributes.OWNER_OBJECT, new ArrayList<NameI>(), filtersOList);
//				super.addNameSearch(modules, serverInfo, SearchAttributes.OWNER_OBJECT, searchedNames == null ? new ArrayList<NameI>() : searchedNames,
//						filtersOList);

			} else {
				List<FilterResponse> filtersOListTS = new ArrayList<FilterResponse>();
				filtersOListTS.add(new DocTypeAdvancedFilter(searchId));
				filtersOListTS.addAll(filtersOList);

//				ArrayList<NameI> searchedNames = super.addNameSearch(modules, serverInfo, SearchAttributes.OWNER_OBJECT, null, filtersOListTS);

				super.addOCRSearch(modules, serverInfo, legalOrDocTypeFilter);
				super.addNameSearch(modules, serverInfo, SearchAttributes.OWNER_OBJECT, new ArrayList<NameI>(), filtersOList);
//				super.addNameSearch(modules, serverInfo, SearchAttributes.OWNER_OBJECT, searchedNames == null ? new ArrayList<NameI>() : searchedNames,
//						filtersOListTS);
			}
			GenericRuntimeIterator<InstrumentI> instrumentIterator = super.getInstrumentIterator(false);

			if (instrumentIterator != null) {
				final int[] REFERECE_IDXS = { TSServerInfo.INSTR_NO_MODULE_IDX, TSServerInfo.BOOK_AND_PAGE_MODULE_IDX };
				for (int moduleIdx : REFERECE_IDXS) {
					module = new TSServerInfoModule(serverInfo.getModule(moduleIdx));
					module.addExtraInformation(TSServerInfoConstants.EXTRA_PARAM_MODULE_DESCRIPTION, "Searching with references.");
					module.clearSaKeys();
					module.addIterator(super.getInstrumentIterator(false));
					for (FilterResponse filter : filtersCrossrefs) {
						module.addFilter(filter);
					}
					module.addFilter(new ExactDateFilterResponse(searchId));
					if (isUpdate) {
						module.addFilter(new BetweenDatesFilterResponse(searchId));
					}
					modules.add(module);
				}
			}
			if (!isUpdate){
				addPlatSearch(serverInfo, modules);
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
	public void addIteratorModule(TSServerInfo serverInfo, List<TSServerInfoModule> modules, int code, long searchId, boolean isUpdate, boolean isTimeShare){

		TSServerInfoModule module = new TSServerInfoModule(serverInfo.getModule(code));
		module.clearSaKeys();

		LegalDescriptionIterator it = getLegalDescriptionIterator(true);

		module.addIterator(it);
		if (isUpdate){
			module.addFilter(new BetweenDatesFilterResponse(searchId));
		}
		PinFilterResponse pinFilter = FLSubdividedBasedDASLDT.getPinFilter(searchId, true);
		if (pinFilter != null){
			module.addFilter(pinFilter);
		}

		if (isTimeShare){
			module.addFilter(new FLSubdividedBasedDASLDT.IncompleteLegalFilter("", searchId));
		}
		if (code == TSServerInfo.SECTION_LAND_MODULE_IDX){
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
	
	protected String prepareInstNoForReferenceSearch(InstrumentI inst) {
		String instrumentNumber = inst.getInstno();
		
		if (instrumentNumber.contains("-")){
			instrumentNumber = instrumentNumber.substring(instrumentNumber.indexOf("-") + 1);
			inst.setInstno(instrumentNumber);
		}
		return instrumentNumber;
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
		
		
		addRelatedInstrumentNoSearch(serverInfo, modules, isUpdate(), relatedSourceDoctype);
		
	}

	public void addRelatedInstrumentNoSearch(TSServerInfo serverInfo, List<TSServerInfoModule> modules,
			boolean isUpdate, String[] relatedSourceDoctype) {
		TSServerInfoModule module;
		InstrumentGenericIterator instrumentRelatedNoIterator = new InstrumentGenericIterator(searchId, getDataSite()){

			/**
			 * 
			 */
			private static final long serialVersionUID = -3296158311167332381L;
			
			@Override
			protected String cleanInstrumentNo(String instno, int year) {
				if (StringUtils.isNotEmpty(instno)){
					if (instno.contains("-")){
						instno = instno.substring(instno.indexOf("-") + 1);
					}
					if (instno.length() <= 7) {
						return instno.replaceFirst("^0+", "");
					} else {
						return instno.substring(instno.length() - 7).replaceFirst("^0+", "");
					}
				}
				return "";
			}
			
		};
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

	public void addPlatSearch(TSServerInfo serverInfo, List<TSServerInfoModule> modules) {
		
		String assessorBook  = null;
		String assessorPage = null;
		
		String pin  = getSearchAttribute(SearchAttributes.LD_PARCELNO);
		pin = pin.replaceAll("-", "");
		
		switch (dataSite.getCountyId()) {
			case CountyConstants.CA_Alameda:
				int pinSize = pin.length();
				if (pinSize < 13){
					pin = "0" + pin;
					pinSize += 1;
				}
				if (pinSize >= 8){
					if ("".equals(pin.replaceAll("[^a-zA-Z-]+", ""))){
						assessorBook = pin.substring(1, 4);
						assessorPage = pin.substring(4, 8);
					} else{
						assessorBook = pin.substring(0, 4);
						assessorPage = pin.substring(4, 8);
					}
				}
			break;
			case CountyConstants.CA_Contra_Costa:
				if (pin.length() >= 5){
					assessorBook = pin.substring(0, 3);
					assessorPage = pin.substring(3, 5);
				}
				break;
			case CountyConstants.CA_Fresno:
				if (pin.length() >= 5){
					pin = pin.replaceAll("[^0-9a-zA-Z]+", "");
					assessorBook = pin.substring(0, 3);
					assessorPage = pin.substring(3, 5);
				}
				break;
			case CountyConstants.CA_Imperial:
				if (pin.length() >= 5){
					assessorBook = pin.substring(0, 3);
					assessorPage = pin.substring(3, 5);
				}
				break;
			case CountyConstants.CA_Inyo:
				if (pin.length() >= 5){
					pin = pin.replaceAll("[^0-9a-zA-Z]+", "");
					assessorBook = pin.substring(0, 3);
					assessorPage = pin.substring(3, 5);
				}
				break;
			case CountyConstants.CA_Kern:
			case CountyConstants.CA_Kings:
				if (pin.length() >= 7){
					assessorBook = pin.substring(0, 3);
					assessorPage = pin.substring(3, 5);
				}
				break;
			case CountyConstants.CA_Los_Angeles:
				if (pin.length() >= 7){
					assessorBook = pin.substring(0, 4);
					assessorPage = pin.substring(4, 7);
				}
				break;
			case CountyConstants.CA_Madera:
			case CountyConstants.CA_Marin:
			case CountyConstants.CA_Merced:
			case CountyConstants.CA_Modoc:
			case CountyConstants.CA_Mono:
			case CountyConstants.CA_Monterey:
				if (pin.length() >= 5){
					assessorBook = pin.substring(0, 3);
					assessorPage = pin.substring(3, 5);
				}
				break;
			case CountyConstants.CA_Orange:
				if (pin.length() >= 5){
					pin = pin.replaceAll("[^0-9a-zA-Z]+", "");
					assessorBook = pin.substring(0, 3);
					assessorPage = pin.substring(3, 5);
				}
				break;
			case CountyConstants.CA_Plumas:
				if (pin.length() >= 5){
					assessorBook = pin.substring(0, 3);
					assessorPage = pin.substring(3, 5);
				}
				break;
			case CountyConstants.CA_Riverside:
				if (pin.length() >= 5){
					assessorBook = pin.substring(0, 3);
					assessorPage = pin.substring(3, 5);
				}
				break;
			case CountyConstants.CA_Sacramento:
				if (pin.length() > 6){
					assessorBook = pin.substring(0, 3);
					assessorPage = pin.substring(3, 6);
				}
				break;
			case CountyConstants.CA_San_Benito:
				if (pin.length() >= 7){
					assessorBook = pin.substring(0, 3);
					assessorPage = pin.substring(3, 5);
				}
				break;
			case CountyConstants.CA_San_Bernardino:
				if (pin.length() >= 7){
					assessorBook = pin.substring(0, 4);
					assessorPage = pin.substring(4, 6);
				}
				break;
			case CountyConstants.CA_San_Diego:
				if (pin.length() >= 5){
					assessorBook = pin.substring(0, 3);
					assessorPage = pin.substring(3, 4);
				}
				break;
			case CountyConstants.CA_San_Francisco:
				if (pin.length() >= 7){
					assessorBook = pin.substring(0, 4);
					assessorPage = pin.substring(4, 5);
				}
				break;
			case CountyConstants.CA_San_Luis_Obispo:
				if (pin.length() >= 5){
					assessorBook = pin.substring(0, 3);
					assessorPage = pin.substring(3, 5);
				}
				break;
			case CountyConstants.CA_San_Mateo:
				if (pin.length() >= 5){
					assessorBook = pin.substring(0, 3);
					assessorPage = pin.substring(3, 5);
				}
				break;
			case CountyConstants.CA_Santa_Barbara:
			case CountyConstants.CA_Santa_Clara:
			case CountyConstants.CA_Santa_Cruz:
			case CountyConstants.CA_Sierra:
			case CountyConstants.CA_Siskiyou:
			case CountyConstants.CA_Stanislaus:
				if (pin.length() >= 5){
					assessorBook = pin.substring(0, 3);
					assessorPage = pin.substring(3, 5);
				}
				break;
			case CountyConstants.CA_Tulare:
				if (pin.length() >= 7){
					assessorBook = pin.substring(0, 4);
					assessorPage = pin.substring(4, 7);
				}
				break;
			case CountyConstants.CA_Ventura:
				if (pin.length() >= 7){
					pin = pin.replaceAll("[^0-9]+", "");
					assessorBook = pin.substring(0, 3);
					assessorPage = pin.substring(3, 6);
				}
				break;
			default:
				break;
		}
		
		
		if (StringUtils.isNotEmpty(assessorBook) && StringUtils.isNotEmpty(assessorPage)){
			
			DocumentsManagerI docManager = getSearch().getDocManager();
			List<DocumentI> almostLike = null; 
			if (docManager != null){
				try{
					docManager.getAccess();
					almostLike = docManager.getDocumentsFlexible(StringUtils.stripStart(assessorBook, "0"), StringUtils.stripStart(assessorPage, "0"), "ASSESSOR_MAP");
				}finally{
					docManager.releaseAccess();
				}
			}
			if ((almostLike == null) || (almostLike != null && almostLike.size() == 0)){
				TSServerInfoModule module = new TSServerInfoModule(serverInfo.getModule(TSServerInfo.BOOK_AND_PAGE_LOCAL_MODULE_IDX));
				module.clearSaKeys();
				module.setData(0, assessorBook);
				module.setData(1, assessorPage);
				module.setData(4, "PLAT");
				module.setData(5, "ASSESSOR_MAP");
				modules.add(module);
			}
		}
	}

	protected InstrumentGenericIterator getInstrumentIterator() {
		InstrumentGenericIterator instrumentBPIterator = new InstrumentGenericIterator(searchId, getDataSite());
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
				setDataSite(CAGenericDTG.this.getDataSite());
			}

			private static final long serialVersionUID = -4741635379234782109L;

			private boolean loadFromSearchPage = true;
			private boolean loadFromSearchPageIfNoLookup = false;
			
			@Override
			public boolean isTransferAllowed(RegisterDocumentI doc) {
				return doc != null && doc.isOneOf(DocumentTypes.TRANSFER);
			}
			
			protected List<LegalStructDTG> createDerrivations() {

				Search global = InstanceManager.getManager().getCurrentInstance(searchId).getCrtSearchContext();
				DocumentsManagerI m = global.getDocManager();
				Set<LegalStructDTG> legalStructList = (HashSet<LegalStructDTG>) global.getAdditionalInfo(AdditionalInfoKeys.DG_LOOK_UP_DATA);
				Set<String> multipleLegals = (Set<String>) global.getAdditionalInfo(CA_MULTIPLE_LEGAL_INSTR);
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
						List<RegisterDocumentI> listRodocs = getGoodDocumentsOrForCurrentOwner(m, global, true, 35, "CA");

						if (listRodocs == null || listRodocs.size() == 0) {
							listRodocs = getGoodDocumentsOrForCurrentOwner(m, global, false, 35, "CA");
						}
						if (listRodocs == null || listRodocs.size() == 0) {
							for (DocumentI doc : m.getDocumentsWithDataSource(true, "DT", "DG")) {
								if (doc instanceof RegisterDocumentI) {
									listRodocs.add((RegisterDocumentI) doc);
								}
							}
						}
						DocumentUtils.sortDocuments(listRodocs, MultilineElementsMap.DATE_ORDER_DESC);
						boolean atLeastOneFromDtDg = false;

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
											String platYear = subdiv.getPlatInstrumentYear();
											String unit = subdiv.getUnit();
											legalStructItem.setUnit(StringUtils.isEmpty(unit) ? "" : unit);
											legalStructItem.setSubLot(org.apache.commons.lang.StringUtils.defaultString(subLot));
											legalStructItem.setBlock(StringUtils.isEmpty(block) ? "" : block);
											legalStructItem.setLotThru(StringUtils.isEmpty(lotThru) ? "" : lotThru);
											legalStructItem.setPlatBook(StringUtils.isEmpty(platBook) ? "" : platBook);
											legalStructItem.setPlatPage(StringUtils.isEmpty(platPage) ? "" : platPage);
											legalStructItem.setPlatInst(StringUtils.isEmpty(platInst) ? "" : platInst);
											legalStructItem.setTract(subdiv.getTract());
											legalStructItem.setPlatInstrYear(platYear);

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
								if (!atLeastOneFromDtDg && !tempLegalStructListPerDocument.isEmpty()) {
									atLeastOneFromDtDg = "DT".equals(reg.getDataSource()) || "DG".equals(reg.getDataSource());
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
									if (!testIfExist(tempPersonal, struct, searchId)
											&& !testIfExistsForCounty(tempPersonal, struct, searchId)) {
										tempPersonal.add(struct);
										multipleLegals.add("Search Page");
									}
								}
							}
						}
						
						legalStructList = tempPersonal;
						legalStructList = keepOnlyGoodLegal(legalStructList);

						if (!atLeastOneFromDtDg) {
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
							List<DocumentI> docList = m.getDocumentsWithDataSource(false, "DG");
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
					global.setAdditionalInfo(CA_MULTIPLE_LEGAL_INSTR, multipleLegals);
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
						if (StringUtils.isNotEmpty(str.getPlatInst()) && StringUtils.isNotEmpty(str.getPlatInstrYear())){
							//CAOrange: 930-84-608
							module.forceValue(11, str.getPlatInstrYear());
							module.setData(6, str.getPlatInst());
						} else{
							module.setData(2, str.getLot());
							if (StringUtils.isNotEmpty(str.getLotThru())){
								module.setData(20, str.getLotThru());
							}
							module.setData(3, str.getBlock());
							
							module.forceValue(10, str.getSubLot());
							if (StringUtils.isNotEmpty(str.getTract())){
								module.forceValue(21, str.getTract());
							} else{
								module.setData(4, str.getPlatBook());
								module.setData(5, str.getPlatPage());
							}
						}
					break;
					case TSServerInfo.SECTION_LAND_MODULE_IDX:
						module.setData(0, str.getSection());
						module.setData(1, str.getTownship());
						module.setData(2, str.getRange());
						module.setData(3, str.getQuarterOrder());
						module.setData(4, str.getQuarterValue());
					break;
					case TSServerInfo.ARB_MODULE_IDX:{
						if(org.apache.commons.lang.StringUtils.isNotBlank(str.getArb()) && str.getArb().matches("(?is)\\d+-\\d+-\\d+")){
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

	@Override
	protected boolean incompleteData(LegalStructDTG str) {
		
		if (str == null){
			return true;
		}
		
		boolean emptyLot = StringUtils.isEmpty(str.getLot());
		boolean emptyBlock = StringUtils.isEmpty(str.getBlock());
		boolean emptyPlatInst = StringUtils.isEmpty(str.getPlatInst());
		boolean emptyPlatBook = StringUtils.isEmpty(str.getPlatBook());
		
		boolean emptySection = StringUtils.isEmpty(str.getSection());
		boolean emptyRange = StringUtils.isEmpty(str.getRange());
		boolean emptyTownship = StringUtils.isEmpty(str.getTownship());
		
		boolean emptyArblot = StringUtils.isEmpty(str.getArbLot());
		boolean emptyArbBlock = StringUtils.isEmpty(str.getArbBlock());
		boolean emptyArbBook = StringUtils.isEmpty(str.getArbBook());
		boolean emptyArbPage = StringUtils.isEmpty(str.getArbPage());
		
		boolean emptyTract = StringUtils.isEmpty(str.getTract());
		
		if (SECTIONAL_TYPE.equalsIgnoreCase(str.getType()) || ARB_TYPE.equalsIgnoreCase(str.getType())){
			if (!emptySection){
				return (emptySection || emptyTownship || emptyRange);
			} else{
				return (  (emptyArbBook || emptyArbPage) || (emptyArblot && emptyArbBlock) );
			}
		} else if(SUBDIVIDED_TYPE.equalsIgnoreCase(str.getType())){
			if (!emptyTract){
				return emptyTract;
			} else{
				return (emptyPlatBook && emptyPlatInst) || (emptyBlock && emptyLot);
			}
		}
		
		return   true;
	}
	
	protected List<RegisterDocumentI> getGoodDocumentsOrForCurrentOwner(DocumentsManagerI m, Search search, boolean applyNameMatch, int siteType, String stateAbbrev){
		final List<RegisterDocumentI> ret = new ArrayList<RegisterDocumentI>();
		
		List<RegisterDocumentI> listRodocs = m.getRealRoLikeDocumentList();
		DocumentUtils.sortDocuments(listRodocs, MultilineElementsMap.DATE_ORDER_DESC);
		
		SearchAttributes sa	= search.getSa();
		PartyI owner 		= sa.getOwners();
		
		
		for (RegisterDocumentI doc : listRodocs){
			boolean found = false;
			if ((doc.isOneOf("MORTGAGE", "TRANSFER", "RELEASE")&& applyNameMatch)
					|| (doc.isOneOf("MORTGAGE", "TRANSFER")&& !applyNameMatch)){
				for (PropertyI prop : doc.getProperties()){
					if (prop.hasSubdividedLegal()){
						SubdivisionI sub = prop.getLegal().getSubdivision();
						LegalStructDTG ret1 = new LegalStructDTG(SUBDIVIDED_TYPE);
						ret1.setLot(sub.getLot());
						ret1.setBlock(sub.getBlock());
						ret1.setTract(sub.getTract());
						ret1.setPlatBook(sub.getPlatBook());
						ret1.setPlatPage(sub.getPlatPage());
						
						ret1.setPlatInst(sub.getPlatInstrument());
						ret1.setPlatInstrYear(sub.getPlatInstrumentYear());
						
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
								&& (StringUtils.isNotEmpty(ret1.getLot()) || StringUtils.isNotEmpty(ret1.getPlatInstrYear())) 
								&& (StringUtils.isNotEmpty(ret1.getPlatInst()) || StringUtils.isNotEmpty(ret1.getTract()) || StringUtils.isNotEmpty(ret1.getPlatBook()))){
							found = true;
						}
					} else if (prop.hasTownshipLegal()){
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
						boolean nameMatched = false;
						
						if (applyNameMatch){
							if (GenericNameFilter.isMatchGreaterThenScore(owner, doc.getGrantor(), NameFilterFactory.NAME_FILTER_THRESHOLD)
									|| GenericNameFilter.isMatchGreaterThenScore(doc.getGrantor(), owner, NameFilterFactory.NAME_FILTER_THRESHOLD)
									|| GenericNameFilter.isMatchGreaterThenScore(owner, doc.getGrantee(), NameFilterFactory.NAME_FILTER_THRESHOLD)
									|| GenericNameFilter.isMatchGreaterThenScore(doc.getGrantee(), owner, NameFilterFactory.NAME_FILTER_THRESHOLD)){
									nameMatched = true;
							}
						}
						
						if ((nameMatched || !applyNameMatch) && isCompleteLegal(ret1, qo, qv, siteType, stateAbbrev)){
							found = true;
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
	
	@Override
	 protected LegalI getLegalFromModule(TSServerInfoModule module){
		 LegalI legal = null;
		 SubdivisionI subdivision = null;
		 TownShipI townShip = null;
			
		 if (module.getModuleIdx() == TSServerInfo.SUBDIVISION_MODULE_IDX && module.getFunctionCount() > 21){
			 subdivision = new Subdivision();
				
			 String subdivisionName = module.getFunction(12).getParamValue().trim();
			 subdivision.setName(subdivisionName);
			 subdivision.setLot(module.getFunction(2).getParamValue().trim());
			 subdivision.setBlock(module.getFunction(3).getParamValue().trim());
			 subdivision.setPlatBook(module.getFunction(4).getParamValue().trim());
			 subdivision.setPlatPage(module.getFunction(5).getParamValue().trim());
			 subdivision.setTract(module.getFunction(21).getParamValue().trim());
		 }
		 if (module.getModuleIdx() == TSServerInfo.SECTION_LAND_MODULE_IDX && module.getFunctionCount() > 3){
			 townShip = new TownShip();
				
			 townShip.setSection(module.getFunction(0).getParamValue().trim());
			 townShip.setTownship(module.getFunction(1).getParamValue().trim());
			 townShip.setRange(module.getFunction(2).getParamValue().trim());
			
		 }
		 if (subdivision != null){
			 legal = new Legal();
			 legal.setSubdivision(subdivision);
		 }
		 
		 if (townShip != null){
			 if (legal == null){
				 legal = new Legal();
			 }
			 legal.setTownShip(townShip);
		 }
		 
		 return legal;
	}
	
	@Override
	protected List<LegalI> findLegalsForUpdate(TSServerInfoModule module, Set<LegalI> allLegalsToCheck) {
		LegalI reference = getLegalFromModule(module);
		if(reference == null) {
			return null;
		}
		
		List<LegalI> goodLegals = new ArrayList<LegalI>();
		
		if(reference.hasSubdividedLegal()) {
			SubdivisionI refSubdivision = reference.getSubdivision();
			for (LegalI legalToCheck : allLegalsToCheck) {
				if(legalToCheck.hasSubdividedLegal()) {
					SubdivisionI candSubdivision = legalToCheck.getSubdivision();
					boolean hasRefTract = false;
					if(StringUtils.isNotEmpty(refSubdivision.getTract())) {
						hasRefTract = true;
						if(StringUtils.isNotEmpty(candSubdivision.getTract())
								&& !refSubdivision.getTract().equals(candSubdivision.getTract())) {
							continue;
						}
					}
					
					boolean hasRefSubdivision = false;
					if(StringUtils.isNotEmpty(refSubdivision.getName())) {
						if(StringUtils.isNotEmpty(candSubdivision.getName())
								&& !refSubdivision.getName().equals(candSubdivision.getName())) {
							continue;
						}
						hasRefSubdivision = true;
					}
					
					if(!hasRefTract 
							&& !hasRefSubdivision
							&& StringUtils.isNotEmpty(refSubdivision.getName()) &&
							!candSubdivision.getName().startsWith(refSubdivision.getName())) {
						continue;
					}
					
					if(StringUtils.isNotEmpty(refSubdivision.getLot())) {
						if(StringUtils.isNotEmpty(candSubdivision.getLot())) {
							if(!refSubdivision.getLot().equals(candSubdivision.getLot())) {
								if(StringUtils.isNotEmpty(refSubdivision.getLotThrough())) {
									try {
										int startLot = Integer.parseInt(refSubdivision.getLot().trim());
										int endLot = Integer.parseInt(refSubdivision.getLotThrough().trim());
										int candLot = Integer.parseInt(candSubdivision.getLot().trim());
										if(candLot >= startLot && candLot <= endLot) {
											//same lot, so let the next test
										} else {
											//lot is different
											continue;
										}
									} catch (Exception e) {
										logger.error("Error while trying to get lot interval", e);
										//lot is different
										continue;
									}
								} else {
									//lot is different
									continue;
								}
							} else {
								//same lot, so let the next test
							}
						}
					}
					
					if(StringUtils.isNotEmpty(refSubdivision.getBlock())) {
						if(StringUtils.isNotEmpty(candSubdivision.getBlock())) {
							if(!refSubdivision.getBlock().equals(candSubdivision.getBlock())) {
								if(StringUtils.isNotEmpty(refSubdivision.getBlockThrough())) {
									try {
										int startBlock = Integer.parseInt(refSubdivision.getBlock().trim());
										int endBlock = Integer.parseInt(refSubdivision.getBlockThrough().trim());
										int candBlock = Integer.parseInt(candSubdivision.getBlock().trim());
										if(candBlock >= startBlock && candBlock <= endBlock) {
											//same block, so let the next test
										} else {
											//block is different
											continue;
										}
									} catch (Exception e) {
										logger.error("Error while trying to get block interval", e);
										//block is different
										continue;
									}
								} else {
									//block is different
									continue;
								}
							} else {
								//same block, so let the next test
							}
						}
					}
					LegalI newLegalToAdd = new Legal();
					newLegalToAdd.setSubdivision(candSubdivision);
					//be sure we only add plated legal since the search was plated
					goodLegals.add(newLegalToAdd);
				}
			}
		}
		if(reference.hasTownshipLegal()) {

			TownShipI refTownship = reference.getTownShip();
			for (LegalI legalToCheck : allLegalsToCheck) {
				if(legalToCheck.hasTownshipLegal()) {
					TownShipI candTownship = legalToCheck.getTownShip();
					
					if(StringUtils.isNotEmpty(refTownship.getSection()) 
							&& StringUtils.isNotEmpty(candTownship.getSection())
							&& !refTownship.getSection().equals(candTownship.getSection())) {
						continue;
					}
					
					if(StringUtils.isNotEmpty(refTownship.getTownship()) 
							&& StringUtils.isNotEmpty(candTownship.getTownship())
							&& !refTownship.getTownship().equals(candTownship.getTownship())) {
						continue;
					}
					
					if(StringUtils.isNotEmpty(refTownship.getRange()) 
							&& StringUtils.isNotEmpty(candTownship.getRange())
							&& !refTownship.getRange().equals(candTownship.getRange())) {
						continue;
					}
					
					if(StringUtils.isNotEmpty(refTownship.getArb()) 
							&& StringUtils.isNotEmpty(candTownship.getArb())
							&& !refTownship.getArb().equals(candTownship.getArb())) {
						continue;
					}
					
					if(StringUtils.isNotEmpty(refTownship.getAddition()) &&
							!candTownship.getAddition().startsWith(refTownship.getAddition())) {
						continue;
					}
					
					LegalI newLegalToAdd = new Legal();
					newLegalToAdd.setTownShip(candTownship);
					//be sure we only add plated legal since the search was unplated
					goodLegals.add(newLegalToAdd);
				}
			}
		
		}
		
		return goodLegals;
	}
	
	@Override
	public InstrumentGenericIterator getInstrumentIterator(boolean instrumentType) {
		InstrumentGenericIterator instrumentGenericIterator = new InstrumentGenericIterator(searchId, getDataSite()) {

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
			protected void loadDerrivation(TSServerInfoModule module, InstrumentI inst) {
				super.loadDerrivation(module, inst);
				
				List<FilterResponse> allFilters = module.getFilterList();
				ExactDateFilterResponse dateFilter = null;
				if (allFilters != null) {
					for (FilterResponse filterResponse : allFilters) {
						if (filterResponse instanceof ExactDateFilterResponse) {
							dateFilter = (ExactDateFilterResponse) filterResponse;
							dateFilter.getFilterDates().clear();
							if (inst instanceof RegisterDocument) {
								dateFilter.addFilterDate(((RegisterDocumentI)inst).getRecordedDate());
								dateFilter.addFilterDate(((RegisterDocumentI)inst).getInstrumentDate());
							} else {
								dateFilter.addFilterDate(inst.getDate());
							}
						}
					}
				}
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
				if (StringUtils.isNotEmpty(instno)){
					if (instno.contains("-")){
						instno = instno.substring(instno.indexOf("-") + 1);
					}
					if (instno.length() <= 7) {
						return instno.replaceFirst("^0+", "");
					} else {
						return instno.substring(instno.length() - 7).replaceFirst("^0+", "");
					}
				}

				return "";
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
		String instno = documentToCheckCopy.getInstno();
		if (!instno.contains("-") && documentToCheckCopy.getYear() > SimpleChapterUtils.UNDEFINED_YEAR){
			String year = "";
			try {
				year = Integer.toString(documentToCheckCopy.getYear());
			} catch (Exception e) {
			}
			if (StringUtils.isNotEmpty(year)){
				documentToCheckCopy.setInstno(year + "-" + instno);
			}
		}
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
//    	String month 	 = map.get("month");
		String dataTreeIndexStr = StringUtils.defaultString(map.get("dataTreeIndex"));
    	String dataTreeDesc =  StringUtils.defaultString(map.get("dataTreeDesc"));
    	String isFake =  StringUtils.defaultString(map.get("isFake"));
    	String dataTreeDocType =  StringUtils.defaultString(map.get("dataTreeDocType"));
    	
    	if (StringUtils.isEmpty(isFake)){
    		isFake = "false";
    	}
    	int yearInt = -1;
    	if (StringUtils.isNotEmpty(year)){
	    	try {
				yearInt = Integer.parseInt(year);
			} catch (NumberFormatException e) {
				e.printStackTrace();
			}
    	}
//    	int monthInt = -1;
//    	if (StringUtils.isNotEmpty(month)){
//			try {
//				monthInt = Integer.parseInt(month);
//			} catch (NumberFormatException e) {
//				e.printStackTrace();
//			}
//    	}
		
    	if (CountyConstants.CA_San_Mateo == dataSite.getCountyId()){
			 if (docNumber.length() == 7){
				 if (yearInt == SimpleChapterUtils.UNDEFINED_YEAR){
					 year = docNumber.substring(0, 1);
					 year = "200" + year;
					 try{yearInt = Integer.parseInt(year);}catch(Exception e){}
				 }
				 docNumber = StringUtils.stripStart(docNumber.substring(1), "0");
			 }
			 if (docNumber.length() == 8){
				 if (yearInt == SimpleChapterUtils.UNDEFINED_YEAR){
					 year = docNumber.substring(0, 2);
					 if (year.matches("(?is)\\A0\\d+") || year.matches("(?is)\\A1\\d+") || year.matches("(?is)\\A2\\d+")){
						 year = "20" + year;
					 } else{
						 year = "19" + year;
					 }
					 try{yearInt = Integer.parseInt(year);}catch(Exception e){}
				 }
				 docNumber = StringUtils.stripStart(docNumber.substring(2), "0");
			 }
		 }
    	
    	int dataTreeIndex = -1;
    	try{dataTreeIndex = Integer.parseInt(dataTreeIndexStr);}catch(Exception e){};
    	
    	boolean savedImage = false;

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
					}
				} catch (IOException e) {
					e.printStackTrace();
				}
			} else{
				try {
					savedImage= FLGenericDASLDT.downloadImageFromDataTree(i, datTreeList, image.getPath(),getCommunityId() + "", null, null);
					if (savedImage){
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
			afterDownloadImage(savedImage, GWTDataSite.DG_TYPE);
		}
		if (!savedImage){
			try {
				savedImage = FLGenericDASLDT.downloadImageFromPropertyInsight(image.getPath(), FLGenericDASLDT.getPiQuery(i, searchId), searchId).success;
				if (savedImage){
					SearchLogger.info("<br/>Image(searchId=" + searchId + ") for book=" + i.getBook() + " page=" + i.getPage() + "; inst=" + i.getInstno() 
							+ " year=" + year + " was taken from PI.<br/>", searchId);
				} else{
					SearchLogger.info(
							"<br/>FAILED to take Image(searchId=" + searchId + ") for book=" + i.getBook() + " page=" + i.getPage() + "; inst=" + i.getInstno() 
							+ " year=" + year + " from PI.<br/>", searchId); 
				}
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		
		byte[] b = new byte[0];
		if (new File(image.getPath()).exists()){
			try {
				b = FileUtils.readFileToByteArray(new File(image.getPath()));
			} catch (IOException e) {e.printStackTrace();}
		}
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
			}
			if(instrNo) {
				modInstNo = module.getFunction(0).getParamValue().trim();
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
								String year = module.getFunction(4).getParamValue();
								if (org.apache.commons.lang.StringUtils.isNotBlank(year)) {
									try {
										newReference.setYear(Integer.parseInt(year));
									} catch (Exception e) {
									}
								}
								if (instrNo) {
									if (StringUtils.isEmpty(year)){
										newReference.setInstno(modInstNo);
									} else{
										newReference.setInstno(year + "-" + modInstNo);
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
	
	public static String prepareApnPerCounty(DataSite dataSite, String originalApn, boolean rearrange){
		
		if (dataSite == null){
			return originalApn;
		}
		String apn = originalApn;
		switch (dataSite.getCountyId()) {
			case CountyConstants.CA_Alameda:
				apn = apn.replaceAll("-", "");
				if (apn.length() == 10 || apn.length() == 11){
					apn = apn.replaceAll("(\\d{3}[A-Z]?)([A-Z\\d]{4})([A-Z\\d]{3})", "$1-$2-$3");
					originalApn = apn;
				} else if (apn.length() == 12 || apn.length() == 13){
					apn = apn.replaceAll("(\\d{3}[A-Z]?)([A-Z\\d]{4})([A-Z\\d]{3})([A-Z\\d]{2})", "$1-$2-$3-$4");
					originalApn = apn;
				}
				break;
			case CountyConstants.CA_Contra_Costa:
				apn = apn.replaceAll("-", "");
				if (apn.length() == 10){
					apn = apn.replaceAll("(\\d{3})(\\d{3})(\\d{3})(\\d{1})", "$1-$2-$3");
					originalApn = apn;
				} else if (apn.length() == 9){
					apn = apn.replaceAll("(\\d{3})(\\d{3})(\\d{3})", "$1-$2-$3");
					originalApn = apn;
				}
				break;
			case CountyConstants.CA_Fresno:
				apn = apn.replaceAll("-", "");
				if (apn.length() >= 8){
					apn = apn.replaceAll("(\\d{3})(\\d{3})(\\d{2})", "$1-$2-$3");
					originalApn = apn;
				}
				break;
			case CountyConstants.CA_Imperial:
				apn = apn.replaceAll("-", "");
				if (apn.length() > 11){
					apn = apn.replaceAll("(\\d{3})(\\d{3})(\\d{3})(\\d{3})", "$1-$2-$3-$4");
					originalApn = apn;
				}
				break;
			case CountyConstants.CA_Inyo:
				apn = apn.replaceAll("-", "");
				if (apn.length() > 7){
					apn = apn.replaceAll("(\\d{3})(\\d{3})(\\d+)", "$1-$2-$3");
					originalApn = apn;
				}
				break;
			case CountyConstants.CA_Kern:
				apn = apn.replaceAll("-", "");
				if (dataSite.getSiteTypeInt() == GWTDataSite.DG_TYPE){
					if (apn.length() > 6){
						apn = apn.replaceAll("(\\d{3})(\\d{3})(\\d{2})", "$1-$2-$3");
						originalApn = apn;
					}
				} else{
					if (apn.length() == 8){
						apn = apn.replaceAll("(\\d{3})(\\d{3})(\\d{2})", "$1-$2-$3");
						originalApn = apn;
					} else if (apn.length() == 10){
						apn = apn.replaceAll("(\\d{3})(\\d{3})(\\d{2})(\\d{2})", "$1-$2-$3-$4");
						originalApn = apn;
					}
				}
				break;
			case CountyConstants.CA_Kings:
				apn = apn.replaceAll("-", "");
				if (apn.length() > 11){
					apn = apn.replaceAll("(\\d{3})(\\d{3})(\\d{3})(\\d{3})", "$1-$2-$3");
					originalApn = apn;
				} else if (apn.length() == 9){
					apn = apn.replaceAll("(\\d{3})(\\d{3})(\\d{3})", "$1-$2-$3");
					originalApn = apn;
				}
				break;
			case CountyConstants.CA_Los_Angeles:
				apn = apn.replaceAll("-", "");
				if (apn.length() == 10){
					apn = apn.replaceAll("(\\d{4})(\\d{3})(\\d{3})", "$1-$2-$3");
					originalApn = apn;
				}
				break;
			case CountyConstants.CA_Madera:
				apn = apn.replaceAll("-", "");
				if (apn.length() == 9){
					apn = apn.replaceAll("(\\d{3})(\\d{3})(\\d{3})", "$1-$2-$3");
					originalApn = apn;
				}
				break;
			case CountyConstants.CA_Marin:
				apn = apn.replaceAll("-", "");
				if (apn.length() == 8){
					apn = apn.replaceAll("(\\d{3})(\\d{3})(\\d{2})", "$1-$2-$3");
					originalApn = apn;
				}
				break;
			case CountyConstants.CA_Merced:
				apn = apn.replaceAll("-", "");
				if (apn.length() > 11){
					apn = apn.replaceAll("(\\d{3})(\\d{3})(\\d{3})(\\d{3})", "$1-$2-$3-$4");
					originalApn = apn;
				}
				break;
			case CountyConstants.CA_Modoc:
				apn = apn.replaceAll("-", "");
				if (apn.length() == 10){
					apn = apn.replaceAll("(\\d{3})(\\d{3})(\\d{2})(\\d{2})", "$1-$2-$3-$4");
					originalApn = apn;
				}
				break;
			case CountyConstants.CA_Mono:
			case CountyConstants.CA_Monterey:
				apn = apn.replaceAll("-", "");
				if (apn.length() == 12){
					apn = apn.replaceAll("(\\d{3})(\\d{3})(\\d{3})(\\d{3})", "$1-$2-$3-$4");
					originalApn = apn;
				}
				break;
			case CountyConstants.CA_Orange:
				if (dataSite.getSiteTypeInt() == GWTDataSite.DG_TYPE){
					apn = apn.replaceAll("-", "");
					if (apn.length() == 8){
						apn = apn.replaceAll("(\\d{3})(\\d{3})(\\d{2})", "$1-$2-$3");
						originalApn = apn;
					}
				} else{
					apn = apn.replaceAll("-", "");
					if (rearrange){
						apn = apn.replaceAll("(\\d{3})(\\d{2})(\\d{3})", "$1-$2-$3");
					} else{
						apn = apn.replaceAll("(\\d{3})(\\d{3})(\\d{2})", "$1-$2-$3");
					}
					originalApn = apn;
				}
				break;
			case CountyConstants.CA_Plumas:
				apn = apn.replaceAll("-", "");
				if (apn.length() == 12){
					apn = apn.replaceAll("(\\d{3})(\\d{3})(\\d{3})(\\d{3})", "$1-$2-$3-$4");
					originalApn = apn;
				}
				break;
			case CountyConstants.CA_Riverside:
				apn = apn.replaceAll("-", "");
				if (apn.length() == 9){
					apn = apn.replaceAll("(\\d{3})(\\d{3})(\\d{3})", "$1-$2-$3");
					originalApn = apn;
				}
				break;
			case CountyConstants.CA_Sacramento:
				apn = apn.replaceAll("-", "");
				if (apn.length() == 14){
					apn = apn.replaceAll("(\\d{3})(\\d{4})(\\d{3})(\\d{4})", "$1-$2-$3-$4");
					originalApn = apn;
				}
				break;
			case CountyConstants.CA_San_Bernardino:
				apn = apn.replaceAll("-", "");
				if (apn.length() == 13){
					apn = apn.replaceAll("(\\d{4})(\\d{3})(\\d{2})(\\d{4})", "$1-$2-$3-$4");
					originalApn = apn;
				}
				break;
			case CountyConstants.CA_San_Benito:
				apn = apn.replaceAll("-", "");
				if (apn.length() == 12){
					apn = apn.replaceAll("(\\d{3})(\\d{3})(\\d{3})(\\d{3})", "$1-$2-$3");
					originalApn = apn;
				} else if (apn.length() == 9){
					apn = apn.replaceAll("(\\d{3})(\\d{3})(\\d{3})", "$1-$2-$3");
					originalApn = apn;
				}
				break;
			case CountyConstants.CA_San_Diego:
				apn = apn.replaceAll("-", "");
				if (apn.length() == 10){
					apn = apn.replaceAll("(\\d{3})(\\d{3})(\\d{2})(\\d{2})", "$1-$2-$3-$4");
					originalApn = apn;
				}
				break;
			case CountyConstants.CA_San_Francisco:
				apn = apn.replaceAll("-", "");
				if (apn.length() > 6){
					apn = apn.replaceAll("(\\d{4}[A-Z]?)(\\d{3}[A-Z]?)", "$1-$2");
					originalApn = apn;
				}
				break;
			case CountyConstants.CA_San_Luis_Obispo:
			case CountyConstants.CA_San_Mateo:
			case CountyConstants.CA_Santa_Barbara:
				apn = apn.replaceAll("-", "");
				if (apn.length() > 8){
					apn = apn.replaceAll("(\\d{3})(\\d{3})(\\d{3})", "$1-$2-$3");
					originalApn = apn;
				}
				break;
			case CountyConstants.CA_Santa_Clara:
				apn = apn.replaceAll("-", "");
				if (apn.length() == 8){
					apn = apn.replaceAll("(\\d{3})(\\d{2})(\\d{3})", "$1-$2-$3");
					originalApn = apn;
				}
				break;
			case CountyConstants.CA_Santa_Cruz:
				apn = apn.replaceAll("-", "");
				if (apn.length() > 7){
					apn = apn.replaceAll("(\\d{3})(\\d{3})(\\d{2})(\\d+)?", "$1-$2-$3");
					originalApn = apn;
				}
				break;
			case CountyConstants.CA_Sierra:
				apn = apn.replaceAll("-", "");
				if (apn.length() == 10){
					apn = apn.replaceAll("(\\d{3})(\\d{3})(\\d{3})(\\d{1})", "$1-$2-$3-$4");
					originalApn = apn;
				}
				break;
			case CountyConstants.CA_Siskiyou:
				apn = apn.replaceAll("-", "");
				if (apn.length() == 9){
					apn = apn.replaceAll("(\\d{3})(\\d{3})(\\d{3})", "$1-$2-$3");
					originalApn = apn;
				}
				break;
			case CountyConstants.CA_Stanislaus:
				apn = apn.replaceAll("-", "");
				if (apn.length() > 8){
					apn = apn.replaceAll("(\\d{3})(\\d{3})(\\d{3})(\\d{3})?", "$1-$2-$3");
					originalApn = apn;
				}
				break;
			case CountyConstants.CA_Tulare:
				apn = apn.replaceAll("-", "");
				if (apn.length() > 11){
					apn = apn.replaceAll("(\\d{3})(\\d{3})(\\d{3})(\\d{3})", "$1-$2-$3");
					originalApn = apn;
				} else if (apn.length() == 9){
					apn = apn.replaceAll("(\\d{3})(\\d{3})(\\d{3})", "$1-$2-$3");
					originalApn = apn;
				}
				break;
			case CountyConstants.CA_Ventura:
				apn = apn.replaceAll("-", "");
				if (apn.length() > 9){
					apn = apn.replaceAll("(\\d{3})(\\d{1})(\\d{3})(\\d{3})", "$1-$2-$3-$4");
					originalApn = apn;
				}
				break;
			default:
				break;
			}
			
		return originalApn;
	}
	
	@Override
	protected boolean isTeleTitleCounty(int countyId){
		
		boolean isTeleTitle = false;
		switch (countyId) {
			case CountyConstants.CA_Alameda:
			case CountyConstants.CA_Contra_Costa:
			case CountyConstants.CA_Imperial:
			case CountyConstants.CA_Kern:
			case CountyConstants.CA_Los_Angeles:
			case CountyConstants.CA_Orange:
			case CountyConstants.CA_Riverside:
			case CountyConstants.CA_Sacramento:
			case CountyConstants.CA_San_Bernardino:
			case CountyConstants.CA_San_Diego:
			case CountyConstants.CA_San_Francisco:
			case CountyConstants.CA_San_Mateo:
			case CountyConstants.CA_Santa_Barbara:
			case CountyConstants.CA_Ventura:
				isTeleTitle = true;
				break;
			default:
				break;
		}
		
    	return isTeleTitle;
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
			module.forceValue(2,"INVESTIGATIVE");
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
			module.forceValue(2,"INVESTIGATIVE");
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
		if(PType.GRANTEE.equals(nameType)) {
			if(!namesFromOCR.isEmpty() && !documentI.getGrantee().getNames().isEmpty() && 
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
		} else if(PType.GRANTOR.equals(nameType)) {
			if(!namesFromOCR.isEmpty() && !documentI.getGrantor().getNames().isEmpty() && 
					!documentI.isFieldModified(DocumentI.Fields.GRANTOR)) {
				documentI.getGrantor().clear();
				infoToBeLogged.append("Deleting previos grantor information from document<br>");
			}
		}
		return super.addNamesFromOCR(namesFromOCR, nameType, documentsManager, documentI, infoToBeLogged);
	}
	
	@Override
	public boolean isAlreadySaved(String instrumentNo, DocumentI doc, DTRecord record) {

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
	
	@Override
	protected String formatInstrumentNumber(String instNo){
		if (instNo.contains("-")){
			instNo = instNo.substring(instNo.indexOf("-") + 1);
		}
		
		return instNo;
	}
	
	@Override
	public void processOcredInstrument(InstrumentI in, boolean ocrReportedBookPage) {
		if(!ocrReportedBookPage){
			
			String inst = StringUtils.defaultString(in.getInstno());
			if(!inst.contains("-")){
				if (inst.length() == 7){
					String year = inst.substring(0, 2);
					if (year.startsWith("0") || year.startsWith("1") || year.startsWith("2")){
						year = "20" + year;
					} else{
						year = "19" + year;
					}
					inst = year + "-" + StringUtils.stripStart(inst.substring(2), "0");
				} else if (inst.length() == 11){
					String year = inst.substring(0, 4);
					inst = year + "-" + StringUtils.stripStart(inst.substring(4), "0");
				}
				
				in.setInstno(inst);
			}
		}
	}
}
