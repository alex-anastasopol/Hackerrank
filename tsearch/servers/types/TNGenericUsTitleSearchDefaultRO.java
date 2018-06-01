/**
 * 
 */
package ro.cst.tsearch.servers.types;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Vector;

import org.apache.commons.lang.StringUtils;

import ro.cst.tsearch.Search;
import ro.cst.tsearch.bean.SearchAttributes;
import ro.cst.tsearch.search.FunctionStatesIterator;
import ro.cst.tsearch.search.GenericRuntimeIterator;
import ro.cst.tsearch.search.ModuleStatesIterator;
import ro.cst.tsearch.search.StatesIterator;
import ro.cst.tsearch.search.filter.BetweenDatesFilterResponse;
import ro.cst.tsearch.search.filter.FilterResponse;
import ro.cst.tsearch.search.filter.RejectAlreadySavedDocumentsFilterResponse;
import ro.cst.tsearch.search.filter.matchers.algorithm.LotInterval;
import ro.cst.tsearch.search.filter.matchers.algorithm.LotMatchAlgorithm;
import ro.cst.tsearch.search.filter.newfilters.address.AddressFilterFactory;
import ro.cst.tsearch.search.filter.newfilters.date.DateFilterFactory;
import ro.cst.tsearch.search.filter.newfilters.date.LastTransferDateFilter;
import ro.cst.tsearch.search.filter.newfilters.doctype.DoctypeFilterFactory;
import ro.cst.tsearch.search.filter.newfilters.legal.GenericLegal;
import ro.cst.tsearch.search.filter.newfilters.legal.LegalFilterFactory;
import ro.cst.tsearch.search.filter.newfilters.misc.CrossReferenceToInvalidatedFilter;
import ro.cst.tsearch.search.filter.newfilters.misc.NoIndexingInfoFilter;
import ro.cst.tsearch.search.filter.newfilters.name.GenericNameFilter;
import ro.cst.tsearch.search.filter.newfilters.name.NameFilterFactory;
import ro.cst.tsearch.search.filter.newfilters.pin.PINFilterFactory;
import ro.cst.tsearch.search.iterator.ConfigurableBookPageIterator;
import ro.cst.tsearch.search.iterator.ModuleStatesIteratorFactory;
import ro.cst.tsearch.search.iterator.instrument.InstrumentGenericIterator;
import ro.cst.tsearch.search.iterator.legal.LegalDescriptionIteratorI;
import ro.cst.tsearch.search.module.ConfigurableNameIterator;
import ro.cst.tsearch.search.strategy.DefaultStatesIterator;
import ro.cst.tsearch.search.validator.DocsValidator;
import ro.cst.tsearch.servers.info.TSServerInfo;
import ro.cst.tsearch.servers.info.TSServerInfoConstants;
import ro.cst.tsearch.servers.info.TSServerInfoModule;
import ro.cst.tsearch.servers.parentsite.County;
import ro.cst.tsearch.templates.MultilineElementsMap;
import ro.cst.tsearch.titledocument.abstracts.DocumentTypes;
import ro.cst.tsearch.utils.GBManager;
import ro.cst.tsearch.utils.InstanceManager;
import ro.cst.tsearch.utils.StringEquivalents;

import com.stewart.ats.base.document.DocumentI;
import com.stewart.ats.base.document.DocumentUtils;
import com.stewart.ats.base.document.InstrumentI;
import com.stewart.ats.base.document.RegisterDocumentI;
import com.stewart.ats.base.document.RestoreDocumentDataI;
import com.stewart.ats.base.legal.LegalI;
import com.stewart.ats.base.legal.SubdivisionI;
import com.stewart.ats.base.name.NameI;
import com.stewart.ats.base.parties.PartyI;
import com.stewart.ats.base.property.PropertyI;
import com.stewart.ats.base.search.DocumentsManagerI;

@SuppressWarnings("serial")
public class TNGenericUsTitleSearchDefaultRO extends TNGenericUsTitleSearchRO {

	public TNGenericUsTitleSearchDefaultRO(long searchId) {
		super(searchId);	
	}

	public TNGenericUsTitleSearchDefaultRO(String rsRequestSolverName, String rsSitePath, String rsServerID, String rsPrmNameLink, long searchId, int miServerID) {		
		super(rsRequestSolverName, rsSitePath, rsServerID, rsPrmNameLink, searchId, miServerID);
	}
	
	public TSServerInfo getDefaultServerInfo() {
		TSServerInfo msiServerInfoDefault = super.getDefaultServerInfo();
		setModulesForGoBackOneLevelSearch(msiServerInfoDefault);
		return msiServerInfoDefault;
	}

	protected String getBookForBPSearch(String book){
		return book;
	}
	
	protected String getPageForBPSearch(String page){
		return page;
	}
	
	protected String specificInstrumentNo(InstrumentI instrument){
		return instrument.getInstno();
	}

	private void addIteratorModule(TSServerInfo serverInfo, List<TSServerInfoModule> modules, int code) {
		GenericLegal defaultLegalFilter = (GenericLegal)LegalFilterFactory.getDefaultLegalFilter(searchId);
		defaultLegalFilter.setEnableLotUnitFullEquivalence(true);
		defaultLegalFilter.setEnableDistrict(true);
		DocsValidator defaultLegalValidator = defaultLegalFilter.getValidator();

		FilterResponse addressFilter = AddressFilterFactory.getAddressHighPassFilter(searchId, 0.8d);
//		((AddressFilterResponse2)addressFilter).setCheckSubdivisionForAddress(true);
		DocsValidator addressHighPassValidator = addressFilter.getValidator();
		
		LastTransferDateFilter ltdf = new LastTransferDateFilter(searchId);
		ltdf.setNameSearch(false);
		DocsValidator lastTransferDateValidator = ltdf.getValidator();
		
		DocsValidator recordedDateValidator = BetweenDatesFilterResponse.getDefaultIntervalFilter(searchId).getValidator();
		DocsValidator crossRefToInvalidatedValidator = new CrossReferenceToInvalidatedFilter(searchId).getValidator();

		TSServerInfoModule module = new TSServerInfoModule(serverInfo.getModule(code));
		module.addExtraInformation(TSServerInfoConstants.EXTRA_PARAM_MODULE_DESCRIPTION, TSServerInfoConstants.VALUE_PARAM_SUBDIVISION_LOT_BLOCK);
		module.clearSaKeys();
		LegalDescriptionIterator it = new LegalDescriptionIterator(searchId);
		it.setEnableSubdividedLegal(true);
		
		module.addValidator( defaultLegalValidator );
		module.addValidator( addressHighPassValidator );
		module.addValidator( lastTransferDateValidator );
		module.addValidator( recordedDateValidator );
		module.addValidator( crossRefToInvalidatedValidator );
		module.addCrossRefValidator( defaultLegalValidator );
		module.addCrossRefValidator( addressHighPassValidator );
        module.addCrossRefValidator( recordedDateValidator );
        module.addCrossRefValidator( crossRefToInvalidatedValidator );
		
		module.addIterator(it);
		modules.add(module);
	}

	static protected class LegalDescriptionIterator extends GenericRuntimeIterator<PersonalDataStruct> implements LegalDescriptionIteratorI {

		private static final long	serialVersionUID		= 9238625486117069L;
		private boolean				enableSubdividedLegal	= false;
		private boolean				enableTownshipLegal		= false;

		LegalDescriptionIterator(long searchId) {
			super(searchId);
		}
		
		@Override
		public boolean isTransferAllowed(RegisterDocumentI doc) {
			return doc != null && doc.isOneOf(DocumentTypes.TRANSFER);
		}
		
		@Override
		public void loadSecondaryPlattedLegal(LegalI legal, ro.cst.tsearch.search.iterator.data.LegalStruct legalStruct) {
			legalStruct.setLot(legal.getSubdivision().getLot());
			legalStruct.setBlock(legal.getSubdivision().getBlock());
		}

		public boolean isEnableSubdividedLegal() {
			return enableSubdividedLegal;
		}

		public void setEnableSubdividedLegal(boolean enableSubdividedLegal) {
			this.enableSubdividedLegal = enableSubdividedLegal;
		}

		public boolean isEnableTownshipLegal() {
			return enableTownshipLegal;
		}

		public void setEnableTownshipLegal(boolean enableTownshipLegal) {
			this.enableTownshipLegal = enableTownshipLegal;
		}

		@SuppressWarnings("unchecked")
		List<PersonalDataStruct> createDerivationInternal(long searchId) {
			Search global = InstanceManager.getManager().getCurrentInstance(searchId).getCrtSearchContext();
			DocumentsManagerI m = global.getDocManager();
			String key = "TN_ROBERTSON_ST_LOOK_UP_DATA";
			if (isEnableSubdividedLegal())
				key += "_SPL";
			else if (isEnableTownshipLegal())
				key += "_STR";
			List<PersonalDataStruct> legalStructList = (List<PersonalDataStruct>) global.getAdditionalInfo(key);

			String aoAndTrLots = global.getSa().getAtribute(SearchAttributes.LD_LOTNO);
			String[] allAoAndTrlots = new String[0];

			if (!StringUtils.isEmpty(aoAndTrLots)) {
				Vector<LotInterval> lots = LotMatchAlgorithm.prepareLotInterval(aoAndTrLots);
				HashSet<String> lotExpanded = new LinkedHashSet<String>();
				for (Iterator<LotInterval> iterator = lots.iterator(); iterator.hasNext();) {
					lotExpanded.addAll(((LotInterval) iterator.next()).getLotList());
				}
				allAoAndTrlots = lotExpanded.toArray(allAoAndTrlots);
			}
			boolean hasLegal = false;
			if (legalStructList == null) {
				legalStructList = new ArrayList<PersonalDataStruct>();

				try {

					m.getAccess();
					List<DocumentI> listRodocs = new ArrayList<DocumentI>();
					listRodocs.addAll(getGoodDocumentsOrForCurrentOwner(m, global, true));
					if (listRodocs.isEmpty()) {
						listRodocs.addAll(getGoodDocumentsOrForCurrentOwner(m, global, false));
					}
					if (listRodocs.isEmpty()) {
						listRodocs.addAll(m.getRoLikeDocumentList(true));
					}

					DocumentUtils.sortDocuments(listRodocs, MultilineElementsMap.DATE_ORDER_ASC);
					for (DocumentI reg : listRodocs) {
						if (reg instanceof RegisterDocumentI) {
							if(!reg.isOneOf(
									DocumentTypes.PLAT,
									DocumentTypes.RESTRICTION,
									DocumentTypes.EASEMENT,
									DocumentTypes.MASTERDEED,
									DocumentTypes.COURT,
									DocumentTypes.LIEN,
									DocumentTypes.CORPORATION,
									DocumentTypes.AFFIDAVIT,
									DocumentTypes.CCER,
									DocumentTypes.TRANSFER)
									
									|| isTransferAllowed((RegisterDocumentI)reg)
									) {
							for (PropertyI prop : reg.getProperties()) {
								if (prop.hasLegal()) {
									LegalI legal = prop.getLegal();

									if (legal.hasSubdividedLegal() && isEnableSubdividedLegal()) {
										hasLegal = true;
										PersonalDataStruct legalStructItem = new PersonalDataStruct();
										SubdivisionI subdiv = legal.getSubdivision();

										String subName = subdiv.getName();
										String section = StringUtils.defaultString(subdiv.getSection());
										String phase = StringUtils.defaultString(subdiv.getPhase());
										String unit = StringUtils.defaultString(subdiv.getUnit());

										String lot = org.apache.commons.lang.StringUtils.defaultString(subdiv.getLot());

										String[] lots = lot.split(" ");
										if (StringUtils.isNotEmpty(subName)) {
											// clean subdiv name

											for (int i = 0; i < lots.length; i++) {
												legalStructItem = new PersonalDataStruct();
												legalStructItem.subName = subName.trim();
												legalStructItem.lot = lots[i].contains("-") ? "" : lots[i];
												legalStructItem.section = section;
												legalStructItem.phase = phase;
												legalStructItem.unit = unit;
												if (!testIfExist(legalStructList, legalStructItem, "subdivision")) {
													legalStructList.add(legalStructItem);
												}
											}
										}
									}
									// if (legal.hasTownshipLegal() && isEnableTownshipLegal()) {
									// PersonalDataStruct legalStructItem = new PersonalDataStruct();
									// TownShipI township = legal.getTownShip();
									//
									// String sec = township.getSection();
									// String tw = township.getTownship();
									// String rg = township.getRange();
									// if (StringUtils.isNotEmpty(sec)) {
									// legalStructItem.section = StringUtils.isEmpty(sec) ? "" : sec;
									// legalStructItem.township = StringUtils.isEmpty(tw) ? "" : tw;
									// legalStructItem.range = StringUtils.isEmpty(rg) ? "" : rg;
									//
									// if (!testIfExist(legalStructList, legalStructItem, "sectional")) {
									// legalStructList.add(legalStructItem);
									// }
									// }
									// }
								}
							}
							}
						}
					}

					global.setAdditionalInfo(key, legalStructList);

				} finally {
					m.releaseAccess();
				}
			} else {
				for (PersonalDataStruct struct : legalStructList) {
					if (StringUtils.isNotEmpty(struct.subName)) {
						hasLegal = true;
					}
				}
				if (hasLegal) {
					legalStructList = new ArrayList<PersonalDataStruct>();
				}
			}
			return legalStructList;
		}

		protected List<PersonalDataStruct> createDerrivations() {
			return createDerivationInternal(searchId);
		}
		
		private List<DocumentI> getGoodDocumentsOrForCurrentOwner(DocumentsManagerI m, Search search, boolean applyNameMatch){
			final List<DocumentI> ret = new ArrayList<DocumentI>();
			
			List<RegisterDocumentI> listRodocs = m.getRealRoLikeDocumentList();
			DocumentUtils.sortDocuments(listRodocs, MultilineElementsMap.DATE_ORDER_DESC);
			
			SearchAttributes sa	= search.getSa();
			PartyI owner 		= sa.getOwners();
			
			
			for(RegisterDocumentI doc:listRodocs){
				boolean found = false;
				for(PropertyI prop:doc.getProperties()){
					if(((doc.isOneOf("MORTGAGE","RELEASE") || isTransferAllowed(doc)) && applyNameMatch)
							|| ((doc.isOneOf("MORTGAGE") || isTransferAllowed(doc)) && !applyNameMatch)){
						if(prop.hasSubdividedLegal()){
							SubdivisionI sub = prop.getLegal().getSubdivision();
							PersonalDataStruct ret1 = new PersonalDataStruct();
							
							ret1.subName = sub.getName();
							ret1.lot = sub.getLot();
							ret1.section = sub.getSection();
							ret1.phase = sub.getPhase();
							
							boolean nameMatched = false;
							
							if(applyNameMatch){
								if(GenericNameFilter.isMatchGreaterThenScore(owner, doc.getGrantor(), NameFilterFactory.NAME_FILTER_THRESHOLD) ||
										GenericNameFilter.isMatchGreaterThenScore(doc.getGrantor(), owner, NameFilterFactory.NAME_FILTER_THRESHOLD)||
										GenericNameFilter.isMatchGreaterThenScore(owner, doc.getGrantee(), NameFilterFactory.NAME_FILTER_THRESHOLD) ||
										GenericNameFilter.isMatchGreaterThenScore(doc.getGrantee(), owner, NameFilterFactory.NAME_FILTER_THRESHOLD)){
										nameMatched = true;
								}
							}
							
							if( (nameMatched||!applyNameMatch) 
									&& (StringUtils.isNotEmpty(ret1.lot) || StringUtils.isNotEmpty(ret1.section) || StringUtils.isNotEmpty(ret1.phase)) 
									&& StringUtils.isNotEmpty(ret1.subName) 
									 ){
								ret.add(doc);
								found = true;
								break;
							}
						}
						
					}
				}
				if(found){
					break;
				}
			}
			
			return ret;
		}

		protected void loadDerrivation(TSServerInfoModule module, PersonalDataStruct str) {

			if (module.getExtraInformation(TSServerInfoConstants.EXTRA_PARAM_MODULE_DESCRIPTION)
					.equals(TSServerInfoConstants.VALUE_PARAM_SUBDIVISION_LOT_BLOCK)) {
				if (!str.isEmpty()) {
					module.setData(0, StringUtils.defaultString(str.subName));
					module.setData(1, StringUtils.defaultString(str.section));
					module.setData(2, StringUtils.defaultString(str.phase));
					module.setData(3, StringUtils.defaultString(str.lot));
					module.setData(4, StringUtils.defaultString(str.unit));
					
					module.setVisible(true);
				} else {
					module.setVisible(false);
				}
			}
		}
	}

	protected static class PersonalDataStruct implements Cloneable {
		String	subName	= "";
		String	lot		= "";
		String	section	= "";
		String	phase	= "";
		String	unit	= "";

		// String section = "";
		// String township = "";
		// String range = "";

		@Override
		protected Object clone() throws CloneNotSupportedException {
			return super.clone();
		}

		public boolean isEmpty() {
			return !(StringUtils.isNotEmpty(this.subName) || StringUtils.isNotEmpty(this.lot) || StringUtils.isNotEmpty(this.phase) || StringUtils
					.isNotEmpty(this.unit) || StringUtils.isNotEmpty(this.section));
		}

		public boolean equalsSubdivision(PersonalDataStruct struct) {
			// boolean retVal = true;
			// if(StringUtils.isNotEmpty(this.block) && StringUtils.isNotEmpty(struct.block)){
			// retVal = this.block.equals(struct.block);
			// }
			//
			// if(StringUtils.isNotEmpty(this.lot) && StringUtils.isNotEmpty(struct.lot)){
			// retVal = retVal && this.lot.equals(struct.lot);
			// }
			//
			// if(StringUtils.isNotEmpty(this.subName) && StringUtils.isNotEmpty(struct.subName)){
			// retVal = retVal && this.subName.equals(struct.subName);
			// }

			return this.lot.equals(struct.lot) && this.subName.equals(struct.subName) &&
					this.section.equals(struct.section) && this.phase.equals(struct.phase) &&
					this.unit.equals(struct.unit);
		}

		// public boolean equalsSectional(PersonalDataStruct struct) {
		// return this.section.equals(struct.section) && this.township.equals(struct.township) && this.range.equals(struct.range);
		// }

	}

	private static boolean testIfExist(List<PersonalDataStruct> legalStruct2, PersonalDataStruct l, String string) {

		if ("subdivision".equalsIgnoreCase(string)) {
			for (PersonalDataStruct p : legalStruct2) {
				if (l.equalsSubdivision(p)) {
					return true;
				}
			}
		}
		// else if ("sectional".equalsIgnoreCase(string)) {
		// for (PersonalDataStruct p : legalStruct2) {
		// if (l.equalsSectional(p)) {
		// return true;
		// }
		// }
		// }
		return false;
	}

	protected void setModulesForAutoSearch(TSServerInfo serverInfo) {
		List<TSServerInfoModule> modules = new ArrayList<TSServerInfoModule>();

		Search search = getSearch();
		TSServerInfoModule m = null;
		SearchAttributes sa = search.getSa();

		FilterResponse rejectSavedDocuments = new RejectAlreadySavedDocumentsFilterResponse(searchId);
		GenericLegal defaultLegalFilter = (GenericLegal)LegalFilterFactory.getDefaultLegalFilter(searchId);
		defaultLegalFilter.setEnableLotUnitFullEquivalence(true);
		defaultLegalFilter.setEnableDistrict(true);
		DocsValidator defaultLegalValidator = defaultLegalFilter.getValidator();

		FilterResponse subdivisionNameFilter = NameFilterFactory.getDefaultNameFilterForSubdivision(searchId);
		subdivisionNameFilter.setThreshold(new BigDecimal(0.90d));
		DocsValidator subdivisionNameValidator = subdivisionNameFilter.getValidator();
		
		FilterResponse addressFilter = AddressFilterFactory.getAddressHighPassFilter(searchId, 0.8d);
//		((AddressFilterResponse2)addressFilter).setCheckSubdivisionForAddress(true);
		DocsValidator addressHighPassValidator = addressFilter.getValidator();
		
		DocsValidator pinValidator = PINFilterFactory.getPinFilter(searchId,true,true).getValidator(); 
		LastTransferDateFilter lastTransferDateFilterForNameSearch = new LastTransferDateFilter(searchId);
		lastTransferDateFilterForNameSearch.setNameSearch(true);
		DocsValidator lastTransferDateValidatorForNameSearch = lastTransferDateFilterForNameSearch.getValidator();
		
		DocsValidator lastTransferDateValidator = new LastTransferDateFilter(searchId).getValidator();
		
		DocsValidator recordedDateValidator = BetweenDatesFilterResponse.getDefaultIntervalFilter(searchId, m).getValidator();
		DocsValidator doctypeValidator = DoctypeFilterFactory.getDoctypeSubdivisionIsGranteeFilter (searchId).getValidator();

		// P1 instrument list search from AO and TR for finding Legal
		
		{
			InstrumentGenericIterator instrumentGenericIterator = getInstrumentIterator(true);
			m = new TSServerInfoModule(serverInfo.getModule(INST_MODULE_IDX));
			m.addExtraInformation(TSServerInfoConstants.EXTRA_PARAM_MODULE_DESCRIPTION, "Search with references from Assessor/Tax like documents");
			m.setIteratorType(0, FunctionStatesIterator.ITERATOR_TYPE_INSTRUMENT_LIST_FAKE);
			
			m.addValidator( defaultLegalValidator );
			m.addValidator( addressHighPassValidator );
			m.addValidator( subdivisionNameValidator );
			m.addValidator( recordedDateValidator );
			
			m.addCrossRefValidator( defaultLegalValidator );
			m.addCrossRefValidator( addressHighPassValidator );
	        m.addCrossRefValidator( pinValidator );
	        m.addCrossRefValidator( recordedDateValidator );
			
			m.addIterator(instrumentGenericIterator);
			modules.add(m);
		}
		{
			InstrumentGenericIterator bpGenericIterator = getInstrumentIterator(false);
			bpGenericIterator.setUseInstrumentType(true);
			bpGenericIterator.setForceInstrumentTypes(bookPageTypeIndexes);

			m = new TSServerInfoModule(serverInfo.getModule(BP_MODULE_IDX));
			m.addExtraInformation(TSServerInfoConstants.EXTRA_PARAM_MODULE_DESCRIPTION, "Search with references from Assessor/Tax like documents");
			m.setIteratorType(0, FunctionStatesIterator.ITERATOR_TYPE_BOOK_FAKE);
			m.setIteratorType(1, FunctionStatesIterator.ITERATOR_TYPE_PAGE_FAKE);
			m.setIteratorType(5, FunctionStatesIterator.ITERATOR_TYPE_BP_TYPE);
			
			m.addValidator( defaultLegalValidator );
			m.addValidator( addressHighPassValidator );
			m.addValidator( subdivisionNameValidator );
			m.addValidator( recordedDateValidator );
			
			m.addCrossRefValidator( defaultLegalValidator );
			m.addCrossRefValidator( addressHighPassValidator );
	        m.addCrossRefValidator( pinValidator );
	        m.addCrossRefValidator( recordedDateValidator );
	        
			m.addIterator(bpGenericIterator);
			modules.add(m);
		}

		addIteratorModule(serverInfo, modules, SUBD_MODULE_IDX);

		String subdivisionName = sa.getAtribute(SearchAttributes.LD_SUBDIV_NAME);
		if (StringUtils.isNotEmpty(subdivisionName)) {
			
			Vector<String> equivalentSubdivisions = StringEquivalents.getInstance().getEquivalents(getSearchAttribute(SearchAttributes.LD_SUBDIV_NAME));
			
			for (String subdivision : equivalentSubdivisions) {
				
				m = new TSServerInfoModule(serverInfo.getModule(SUBD_MODULE_IDX));
				m.addExtraInformation(TSServerInfoConstants.EXTRA_PARAM_MODULE_DESCRIPTION, "Subdivision module - searching with subdivision and lot/section/phase/unit");
				m.clearSaKeys();
	
				m.setData(0, subdivision);
				m.setSaKey(1, SearchAttributes.LD_SUBDIV_SEC);
				m.setSaKey(2, SearchAttributes.LD_SUBDIV_PHASE);
				m.setSaKey(3, SearchAttributes.LD_LOTNO);
				m.setSaKey(4, SearchAttributes.LD_SUBDIV_UNIT);
	
//				m.addFilter(subdivisionNameFilter);
				
				m.addValidator( defaultLegalValidator );
				m.addValidator( pinValidator );
				m.addValidator( addressHighPassValidator );
				m.addValidator( lastTransferDateValidatorForNameSearch );
				m.addValidator( recordedDateValidator );
				m.addCrossRefValidator( defaultLegalValidator );
				m.addCrossRefValidator( addressHighPassValidator );
		        m.addCrossRefValidator( pinValidator );
		        m.addCrossRefValidator( recordedDateValidator );
				
				modules.add(m);
				
				
				
				
				DocsValidator noIndexingValidator = new NoIndexingInfoFilter(searchId).getValidator();
				
				for (String type : platTypeIndexes) {
					// name = subdivision, PLAT
					m = new TSServerInfoModule(serverInfo.getModule(NAME_MODULE_IDX));
					m.addExtraInformation(TSServerInfoConstants.EXTRA_PARAM_MODULE_DESCRIPTION, 
							TSServerInfoConstants.VALUE_PARAM_NAME_SUBDIVISION_PLAT);
					m.setSaObjKey(SearchAttributes.NO_KEY);
					m.clearSaKeys();
					m.setIteratorType(ModuleStatesIterator.TYPE_DEFAULT);
					m.setData(0, subdivision);
					m.setIteratorType(1, FunctionStatesIterator.ITERATOR_TYPE_DEFAULT);
					m.setSaKey(3, sa.isUpdate() ? SearchAttributes.FROMDATE_MM_SLASH_DD_SLASH_YYYY : SearchAttributes.START_HISTORY_DATE_MM_SLASH_DD_SLASH_YYYY );
					m.setParamValue(6, type);
					m.setIteratorType(ModuleStatesIterator.TYPE_DEFAULT);
					m.addFilter(NameFilterFactory.getDefaultNameFilterForGranteeIsSubdivision("", searchId, m));
					m.addValidator( noIndexingValidator );
					m.addValidator( defaultLegalValidator );
					m.addValidator( pinValidator );
					m.addValidator( recordedDateValidator );
					m.addValidator( doctypeValidator );
					m.addValidator( subdivisionNameValidator );
					m.addCrossRefValidator( noIndexingValidator );
					m.addCrossRefValidator( defaultLegalValidator );
			        m.addCrossRefValidator( pinValidator );
			        m.addCrossRefValidator( recordedDateValidator );
			        m.addCrossRefValidator( subdivisionNameValidator );
					modules.add(m);
				}

				for (String type : easementTypeIndexes) {
					// name = subdivision, EASEMENT
					m = new TSServerInfoModule(serverInfo.getModule(NAME_MODULE_IDX));
					m.addExtraInformation(TSServerInfoConstants.EXTRA_PARAM_MODULE_DESCRIPTION, TSServerInfoConstants.VALUE_PARAM_NAME_SUBDIVISION_EASEMENT);
					m.setSaObjKey(SearchAttributes.NO_KEY);
					m.clearSaKeys();
					m.setIteratorType(ModuleStatesIterator.TYPE_DEFAULT);
					m.setData(0, subdivision);
					m.setIteratorType(1, FunctionStatesIterator.ITERATOR_TYPE_DEFAULT);
					m.setParamValue(6, type);
					m.setIteratorType(ModuleStatesIterator.TYPE_DEFAULT);
					m.addFilter(NameFilterFactory.getDefaultNameFilterForGranteeIsSubdivision("", searchId, m));
					m.addValidator( noIndexingValidator );
					m.addValidator( defaultLegalValidator );
					m.addValidator( pinValidator );
					m.addValidator( addressHighPassValidator );
					m.addValidator( recordedDateValidator );
					m.addValidator( doctypeValidator );
					m.addValidator( subdivisionNameValidator );
					m.addCrossRefValidator( noIndexingValidator );
					m.addCrossRefValidator( defaultLegalValidator );
					m.addCrossRefValidator( addressHighPassValidator );
			        m.addCrossRefValidator( pinValidator );
			        m.addCrossRefValidator( recordedDateValidator );
			        m.addCrossRefValidator( subdivisionNameValidator );
					modules.add(m);
				}

				for (String type : restrictionTypeIndexes) {
					// name = subdivision, RESTRICTIONS
					m = new TSServerInfoModule(serverInfo.getModule(NAME_MODULE_IDX));
					m.addExtraInformation(TSServerInfoConstants.EXTRA_PARAM_MODULE_DESCRIPTION, TSServerInfoConstants.VALUE_PARAM_NAME_SUBDIVISION_RESTRICTION);
					m.setSaObjKey(SearchAttributes.NO_KEY);
					m.clearSaKeys();
					m.setIteratorType(ModuleStatesIterator.TYPE_DEFAULT);
					m.setData(0, subdivision);
					m.setIteratorType(1, FunctionStatesIterator.ITERATOR_TYPE_DEFAULT);
					m.setParamValue(6, type);
					m.setIteratorType(ModuleStatesIterator.TYPE_DEFAULT);
					
					m.addFilter(NameFilterFactory.getDefaultNameFilterForGranteeIsSubdivision("", searchId, m));
					m.addValidator( noIndexingValidator );
					m.addValidator( defaultLegalValidator );
					m.addValidator( pinValidator );
					m.addValidator( addressHighPassValidator );
					m.addValidator( recordedDateValidator );
					m.addValidator( doctypeValidator );
					m.addValidator( subdivisionNameValidator );
					m.addCrossRefValidator( noIndexingValidator );
					m.addCrossRefValidator( defaultLegalValidator );
					m.addCrossRefValidator( addressHighPassValidator );
			        m.addCrossRefValidator( pinValidator );
			        m.addCrossRefValidator( recordedDateValidator );
			        m.addCrossRefValidator( subdivisionNameValidator );
					modules.add(m);
				
				}
			
			}
		}
		
		addExtraModuleAfterSubdivision(serverInfo, modules);
		
		if(StringUtils.isNotEmpty(getSearchAttribute(SearchAttributes.LD_PAGENO)) 
				&& StringUtils.isNotEmpty(getSearchAttribute(SearchAttributes.LD_BOOKNO))){
			for (String type : platClassIndexes) {
				if(StringUtils.isNotBlank(type)) {
				    m = new TSServerInfoModule(serverInfo.getModule(BP_MODULE_IDX));
				    m.addExtraInformation(TSServerInfoConstants.EXTRA_PARAM_MODULE_DESCRIPTION, 
			        		TSServerInfoConstants.VALUE_PARAM_PLATBOOK_PLATPAGE);
				    m.clearSaKeys();
					m.forceValue(0, getSearchAttribute(SearchAttributes.LD_BOOKNO));
					m.forceValue(1, getSearchAttribute(SearchAttributes.LD_PAGENO));
					m.forceValue(5, type);
					m.addValidator(defaultLegalValidator);
					m.addValidator(pinValidator);
					m.addValidator(addressHighPassValidator);
					m.addCrossRefValidator( defaultLegalValidator );
			        m.addCrossRefValidator( lastTransferDateValidator );
			        m.addCrossRefValidator( addressHighPassValidator );
			        m.addCrossRefValidator( pinValidator );
					m.addCrossRefValidator( recordedDateValidator );
					modules.add(m);
				}
			}
		}
		

		ConfigurableNameIterator nameIterator = null;
		GenericNameFilter defaultNameFilter = (GenericNameFilter) NameFilterFactory.getDefaultNameFilter(SearchAttributes.OWNER_OBJECT, searchId, m);
		defaultNameFilter.setIgnoreMiddleOnEmpty(true);
		defaultNameFilter.setUseArrangements(false);
		defaultNameFilter.setInitAgain(true);

		
		boolean validateWithDates = applyDateFilter();
		
		FilterResponse crossReferenceToInvalidatedFilter = new CrossReferenceToInvalidatedFilter(searchId);
		DocsValidator crossRefToInvalidatedValidator = crossReferenceToInvalidatedFilter.getValidator();

		if (hasOwner()) {
			m = new TSServerInfoModule(serverInfo.getModule(NAME_MODULE_IDX));
			m.addExtraInformation(TSServerInfoConstants.EXTRA_PARAM_MODULE_DESCRIPTION, TSServerInfoConstants.VALUE_PARAM_NAME_OWNERS);
			m.setSaObjKey(SearchAttributes.OWNER_OBJECT);
			m.clearSaKeys();

			m.addFilter(defaultNameFilter);
			m.addFilter(lastTransferDateFilterForNameSearch);
			addFilterForUpdate(m, true);

			m.setIteratorType(0, FunctionStatesIterator.ITERATOR_TYPE_LAST_NAME_FAKE);
			m.setIteratorType(3, FunctionStatesIterator.ITERATOR_TYPE_FROM_DATE);
			m.setSaKey(3, SearchAttributes.FROMDATE_MM_SLASH_DD_SLASH_YYYY);
			m.setSaKey(4, SearchAttributes.TODATE_MM_SLASH_DD_SLASH_YYYY);

			
			m.addValidator( defaultLegalValidator );
			m.addValidator( pinValidator );
			m.addValidator( addressHighPassValidator );
			m.addValidator( lastTransferDateValidatorForNameSearch );
			m.addValidator( recordedDateValidator );
			m.addValidator( crossRefToInvalidatedValidator );
			addFilterForUpdate(m, false);

			m.addCrossRefValidator( defaultLegalValidator );
			m.addCrossRefValidator( pinValidator );
			m.addCrossRefValidator( addressHighPassValidator );
	        m.addCrossRefValidator( recordedDateValidator );
	        m.addCrossRefValidator( crossRefToInvalidatedValidator );
			
			nameIterator = (ConfigurableNameIterator) ModuleStatesIteratorFactory.getConfigurableNameIterator(m, searchId, new String[] { "L F;;"});
			m.addIterator(nameIterator);
			modules.add(m);
		}

		if (hasBuyer() ) {
			m = new TSServerInfoModule(serverInfo.getModule(NAME_MODULE_IDX));
			m.addExtraInformation(TSServerInfoConstants.EXTRA_PARAM_MODULE_DESCRIPTION, TSServerInfoConstants.VALUE_PARAM_NAME_BUYERS);
			m.setSaObjKey(SearchAttributes.BUYER_OBJECT);
			m.clearSaKeys();

			m.setIteratorType(0, FunctionStatesIterator.ITERATOR_TYPE_LAST_NAME_FAKE);
			m.setIteratorType(3, FunctionStatesIterator.ITERATOR_TYPE_FROM_DATE);
			m.setSaKey(3, SearchAttributes.FROMDATE_MM_SLASH_DD_SLASH_YYYY);
			m.setSaKey(4, SearchAttributes.TODATE_MM_SLASH_DD_SLASH_YYYY);
			
			
			m.addFilter( NameFilterFactory.getNameFilterIgnoreMiddleOnEmpty(SearchAttributes.BUYER_OBJECT, searchId, m) );
			m.addFilter( lastTransferDateFilterForNameSearch);

			m.addValidator( DoctypeFilterFactory.getDoctypeBuyerFilter( searchId ).getValidator() );
		    
			DocsValidator recordedDateNameValidator = BetweenDatesFilterResponse.getDefaultIntervalFilter(searchId, m).getValidator();
			
			if(validateWithDates) {
		    	m.addValidator( recordedDateNameValidator );
		    	m.addCrossRefValidator( recordedDateNameValidator );
		    }
		    addFilterForUpdate(m, false);
	        
	        m.addCrossRefValidator( crossRefToInvalidatedValidator );
			
	        ConfigurableNameIterator buyerNameIterator = (ConfigurableNameIterator) ModuleStatesIteratorFactory.getConfigurableNameIterator(m, searchId, new String[] { "L F;;" });
			m.addIterator(buyerNameIterator);
			modules.add(m);
		}

		// P6 OCR last transfer - book page search
		
		m = new TSServerInfoModule(serverInfo.getModule(INST_MODULE_IDX));
		m.addExtraInformation(TSServerInfoConstants.EXTRA_PARAM_MODULE_DESCRIPTION, TSServerInfoConstants.VALUE_PARAM_OCR_SEARCH_INST);
		m.clearSaKeys();
		m.setIteratorType(ModuleStatesIterator.TYPE_OCR_FULL_OR_BOOTSTRAPER);
		m.getFunction(0).setIteratorType(FunctionStatesIterator.ITERATOR_TYPE_INSTRUMENT_LIST_SEARCH);
		
		m.addValidator( defaultLegalValidator );
		m.addValidator( pinValidator );
		m.addValidator( addressHighPassValidator );
		m.addValidator( recordedDateValidator );
		m.addValidator( lastTransferDateValidator );
		
		m.addCrossRefValidator( defaultLegalValidator );
		m.addCrossRefValidator( addressHighPassValidator );
        m.addCrossRefValidator( pinValidator );
        m.addCrossRefValidator( recordedDateValidator );
				
		modules.add(m);

		for (String clas : bookPageTypeIndexes) {
			m = new TSServerInfoModule(serverInfo.getModule(BP_MODULE_IDX));
			m.addExtraInformation(TSServerInfoConstants.EXTRA_PARAM_MODULE_DESCRIPTION, TSServerInfoConstants.VALUE_PARAM_OCR_SEARCH_INST);
			m.clearSaKeys();
			m.setIteratorType(ModuleStatesIterator.TYPE_OCR_FULL_OR_BOOTSTRAPER);
			m.getFunction(0).setIteratorType(FunctionStatesIterator.ITERATOR_TYPE_BOOK_SEARCH);
			m.getFunction(1).setIteratorType(FunctionStatesIterator.ITERATOR_TYPE_PAGE_SEARCH);
			m.setData(5, clas);
			
			m.addValidator( defaultLegalValidator );
			m.addValidator( pinValidator );
			m.addValidator( addressHighPassValidator );
			m.addValidator( recordedDateValidator );
			m.addValidator( lastTransferDateValidator );
			
			m.addCrossRefValidator( defaultLegalValidator );
			m.addCrossRefValidator( addressHighPassValidator );
	        m.addCrossRefValidator( pinValidator );
	        m.addCrossRefValidator( recordedDateValidator );
	        
			modules.add(m);
		}
		
		// P7 name module with names added by OCR
		m = new TSServerInfoModule(serverInfo.getModule(NAME_MODULE_IDX));
		m.addExtraInformation(TSServerInfoConstants.EXTRA_PARAM_MODULE_DESCRIPTION, TSServerInfoConstants.VALUE_PARAM_NAME_OWNERS);
		m.setSaObjKey(SearchAttributes.OWNER_OBJECT);
		m.clearSaKeys();

		m.addFilter(rejectSavedDocuments);
		m.addFilter(defaultNameFilter);
		m.addFilter(lastTransferDateFilterForNameSearch);
		
		m.addValidator( defaultLegalValidator );
		m.addValidator( pinValidator );
		m.addValidator( addressHighPassValidator );
		m.addValidator( lastTransferDateValidatorForNameSearch );
		m.addValidator( recordedDateValidator );
		m.addValidator( crossRefToInvalidatedValidator );
		addFilterForUpdate(m, false);

		m.addCrossRefValidator( defaultLegalValidator );
		m.addCrossRefValidator( pinValidator );
		m.addCrossRefValidator( addressHighPassValidator );
        m.addCrossRefValidator( recordedDateValidator );
        m.addCrossRefValidator( crossRefToInvalidatedValidator );

		
		
		m.setIteratorType(0, FunctionStatesIterator.ITERATOR_TYPE_LAST_NAME_FAKE);
		m.setIteratorType(3, FunctionStatesIterator.ITERATOR_TYPE_FROM_DATE);
		m.setSaKey(3, SearchAttributes.FROMDATE_MM_SLASH_DD_SLASH_YYYY);
		m.setSaKey(4, SearchAttributes.TODATE_MM_SLASH_DD_SLASH_YYYY);

		ArrayList<NameI> searchedNames = null;
		if (nameIterator != null) {
			searchedNames = nameIterator.getSearchedNames();
		} else {
			searchedNames = new ArrayList<NameI>();
		}

		nameIterator = (ConfigurableNameIterator) ModuleStatesIteratorFactory.getConfigurableNameIterator(m, searchId, new String[] { "L F;;" });
		// get your values at runtime
		nameIterator.setInitAgain(true);

		nameIterator.setSearchedNames(searchedNames);
		m.addIterator(nameIterator);
		modules.add(m);
		
		{
			InstrumentGenericIterator instrumentGenericIterator = getInstrumentIterator(true);
			instrumentGenericIterator.setLoadFromRoLike(true);
			instrumentGenericIterator.setDsToLoad(new String[]{dataSite.getSiteTypeAbrev()});
			
			m = new TSServerInfoModule(serverInfo.getModule(INST_MODULE_IDX));
			m.addExtraInformation(TSServerInfoConstants.EXTRA_PARAM_MODULE_DESCRIPTION, TSServerInfoConstants.VALUE_PARAM_LIST_RO_CROSSREF_INSTR);
			m.setIteratorType(0, FunctionStatesIterator.ITERATOR_TYPE_INSTRUMENT_LIST_FAKE);
			
			m.addValidator( defaultLegalValidator );
			m.addValidator( addressHighPassValidator );
			m.addValidator( subdivisionNameValidator );
			m.addValidator( recordedDateValidator );
			m.addValidator( lastTransferDateValidator );
			
			m.addCrossRefValidator( defaultLegalValidator );
			m.addCrossRefValidator( addressHighPassValidator );
	        m.addCrossRefValidator( pinValidator );
	        m.addCrossRefValidator( recordedDateValidator );
			
			
			m.addIterator(instrumentGenericIterator);
			modules.add(m);
		}
		{
			InstrumentGenericIterator bpGenericIterator = getInstrumentIterator(false);
			bpGenericIterator.setUseInstrumentType(true);
			bpGenericIterator.setForceInstrumentTypes(bookPageTypeIndexes);
			bpGenericIterator.setLoadFromRoLike(true);
			bpGenericIterator.setDsToLoad(new String[]{dataSite.getSiteTypeAbrev()});

			m = new TSServerInfoModule(serverInfo.getModule(BP_MODULE_IDX));
			m.addExtraInformation(TSServerInfoConstants.EXTRA_PARAM_MODULE_DESCRIPTION, TSServerInfoConstants.VALUE_PARAM_LIST_RO_CROSSREF_BP);
			m.setIteratorType(0, FunctionStatesIterator.ITERATOR_TYPE_BOOK_FAKE);
			m.setIteratorType(1, FunctionStatesIterator.ITERATOR_TYPE_PAGE_FAKE);
			m.setIteratorType(5, FunctionStatesIterator.ITERATOR_TYPE_BP_TYPE);
			
			m.addValidator( defaultLegalValidator );
			m.addValidator( addressHighPassValidator );
			m.addValidator( subdivisionNameValidator );
			m.addValidator( recordedDateValidator );
			m.addValidator( lastTransferDateValidator );
			
			m.addCrossRefValidator( defaultLegalValidator );
			m.addCrossRefValidator( addressHighPassValidator );
	        m.addCrossRefValidator( pinValidator );
	        m.addCrossRefValidator( recordedDateValidator );

			m.addIterator(bpGenericIterator);
			modules.add(m);
		}
		

		serverInfo.setModulesForAutoSearch(modules);
	}

	protected void addExtraModuleAfterSubdivision(TSServerInfo serverInfo, List<TSServerInfoModule> modules) {
		
	}

	protected InstrumentGenericIterator getInstrumentIterator(boolean instrumentType) {
		InstrumentGenericIterator instrumentGenericIterator = new InstrumentGenericIterator(searchId, getDataSite()) {
			
			private Map<InstrumentI, Integer> initialDocumentsBeforeRunningStruct = new HashMap<InstrumentI, Integer>();
			private Set<InstrumentI> processedForNextStruct = new HashSet<InstrumentI>();
			
			@Override
			public boolean hasNext(long searchId) {
				boolean hasNext = super.hasNext(searchId);
				if(hasNext && getForceInstrumentTypes() != null && getForceInstrumentTypes().length > 0) {
					StatesIterator myStrategy = super.getStrategy();
					if(myStrategy instanceof DefaultStatesIterator) {
						DefaultStatesIterator iteratorStrategy = (DefaultStatesIterator)myStrategy;
						
						if(iteratorStrategy.getCurrentIndex() >= 0) {
						
							InstrumentI currentStr = (InstrumentI)iteratorStrategy.current();
							
							if(currentStr != null && !processedForNextStruct.contains(currentStr)) {
								Integer initialDocuments = initialDocumentsBeforeRunningStruct.get(currentStr);
								int currentDocuments = getDocumentsManagerDocSize();
								
								if(initialDocuments == null || currentDocuments > initialDocuments) {
									//so only if I have new documents added I need to check and clean the next iterations
									List currentList = iteratorStrategy.getList();
									int currentIndex = iteratorStrategy.getCurrentIndex();
									
									
									//need to copy already processed iterations
									List updatedList = new ArrayList();
									List restOfList = new ArrayList();
									for (int i = 0; i < currentList.size(); i++) {
										if(i > currentIndex) {
											restOfList.add(currentList.get(i));
										} else {
											updatedList.add(currentList.get(i));
										}
									}
									
									if(restOfList.size() > 0) {
									
										//now let's see if the restOfList needs to cleaned for already run iterations
										
										for (Object object : restOfList) {
											InstrumentI toCheckStruct = (InstrumentI)((InstrumentI)object).clone();
											toCheckStruct.setBookType(currentStr.getBookType());
											if(!toCheckStruct.equals(currentStr)) {
												//these structures have only plat book type in common and my type already brought results then I do not need new iterations
												updatedList.add(object);
											}
										}
										
										iteratorStrategy.replaceData(updatedList, currentIndex);
									
										//after altering the list, let's recheck again if we still have next 	
										hasNext = super.hasNext(searchId);
									
									}
									//mark current structure processed
									processedForNextStruct.add(currentStr);
								}
							}
						
						}
					}
				}
				
				return hasNext;
			}
			
			@Override
			protected String cleanInstrumentNo(InstrumentI instrument) {
				return specificInstrumentNo(instrument);
			}
			
			@Override
			protected void loadDerrivation(TSServerInfoModule module, InstrumentI state) {
				if(!initialDocumentsBeforeRunningStruct.containsKey(state)) {
					initialDocumentsBeforeRunningStruct.put(state, getDocumentsManagerDocSize());
				}
				super.loadDerrivation(module, state);
			}
			
		};
		
		if(instrumentType) {
			instrumentGenericIterator.enableInstrumentNumber();
		} else {
			instrumentGenericIterator.enableBookPage();
			instrumentGenericIterator.setRemoveLeadingZerosBP(true);
		}
		return instrumentGenericIterator;
	}
	
	protected ConfigurableBookPageIterator getBookPageIterator() {
		ConfigurableBookPageIterator bookPageIterator = new ConfigurableBookPageIterator(searchId);
		bookPageIterator.setAlsoSearchWithoutLeadingZeroes(false);
		
		return bookPageIterator;
	}
	
	protected ConfigurableBookPageIterator getBookPageIteratorAfterOcr() {
		ConfigurableBookPageIterator bookPageIterator = new ConfigurableBookPageIterator(searchId);
		bookPageIterator.setLoadFromAoDocuments(false)
						.setLoadFromSearchAttributes(false)
						.setLoadFromOcr(true)
						.setAlsoSearchWithoutLeadingZeroes(false)
						.setOnlySearchWithPrefixesAdded(true);
		
		return bookPageIterator;
	}

	@Override
	protected void setModulesForGoBackOneLevelSearch(TSServerInfo serverInfo) {

	  	ConfigurableNameIterator nameIterator = null;
	  	
	  	GenericLegal defaultLegalFilter = (GenericLegal)LegalFilterFactory.getDefaultLegalFilter(searchId);
		defaultLegalFilter.setEnableLotUnitFullEquivalence(true);
		defaultLegalFilter.setEnableDistrict(true);
		DocsValidator defaultLegalValidator = defaultLegalFilter.getValidator();
		
		DocsValidator addressHighPassValidator = AddressFilterFactory.getAddressHighPassFilter(searchId, 0.8d).getValidator();
		
		
		SearchAttributes sa = InstanceManager.getManager().getCurrentInstance(searchId).getCrtSearchContext().getSa();	
		List<TSServerInfoModule> modules = new ArrayList<TSServerInfoModule>();
	    TSServerInfoModule module;	
	    GBManager gbm=(GBManager)sa.getObjectAtribute(SearchAttributes.GB_MANAGER_OBJECT);

	    for (String id : gbm.getGbTransfers()) {
			  		   	    	 
	  		 module =new TSServerInfoModule(serverInfo.getModule(NAME_MODULE_IDX));
	         module.setIndexInGB(id);
	         module.setTypeSearchGB("grantor");
	         module.clearSaKeys();
		     module.setSaObjKey(SearchAttributes.GB_MANAGER_OBJECT);
		     String date=gbm.getDateForSearch(id, "MM/dd/yyyy", searchId);
		     if (date!=null) 
		    	 module.getFunction(3).forceValue(date);
		     module.addFilter(NameFilterFactory.getDefaultNameFilter(SearchAttributes.GB_MANAGER_OBJECT, searchId, module));
		     module.getFunction(0).setIteratorType(FunctionStatesIterator.ITERATOR_TYPE_LAST_NAME_FAKE);
		     nameIterator =  (ConfigurableNameIterator)ModuleStatesIteratorFactory.getConfigurableNameIterator(module, searchId, new String[]{"L F;;"} );
		 	 module.addIterator(nameIterator);
		 	 module.addValidator( defaultLegalValidator );
			 module.addValidator( addressHighPassValidator );
             module.addValidator(NameFilterFactory.getDefaultTransferNameFilter(searchId, 0.90d, module).getValidator());
		  	 module.addValidator(DateFilterFactory.getDateFilterForGoBack(SearchAttributes.GB_MANAGER_OBJECT, searchId, module).getValidator());		
			 modules.add(module);
		    
		     
		     if(gbm.getNamesForBrokenChain(id, searchId).size()>0){
		    	 module =new TSServerInfoModule(serverInfo.getModule(NAME_MODULE_IDX));
			     module.setIndexInGB(id);
			     module.setTypeSearchGB("grantee");
			     module.clearSaKeys();
				 module.setSaObjKey(SearchAttributes.GB_MANAGER_OBJECT);
				 date=gbm.getDateForSearchBrokenChain(id, "MM/dd/yyyy", searchId);
				 if (date!=null) 
					 module.getFunction(3).forceValue(date);
				 module.getFunction(0).setIteratorType(FunctionStatesIterator.ITERATOR_TYPE_LAST_NAME_FAKE);
				 module.addFilter(NameFilterFactory.getDefaultNameFilter(SearchAttributes.GB_MANAGER_OBJECT, searchId, module));
				 nameIterator =  (ConfigurableNameIterator)ModuleStatesIteratorFactory.getConfigurableNameIterator(module, searchId, new String[]{"L F;;"} );
				 module.addIterator(nameIterator);
				 module.addValidator( defaultLegalValidator );
				 module.addValidator( addressHighPassValidator );
			     module.addValidator(NameFilterFactory.getDefaultTransferNameFilter(searchId, 0.90d, module).getValidator());
				 module.addValidator(DateFilterFactory.getDateFilterForGoBack(SearchAttributes.GB_MANAGER_OBJECT, searchId, module).getValidator());				
				 modules.add(module);
			 
		     }

	    }	 
	    serverInfo.setModulesForGoBackOneLevelSearch(modules);	
	}
	
	@Override
	public Object getRecoverModuleFrom(RestoreDocumentDataI restoreDocumentDataI) {
		if(restoreDocumentDataI == null) {
			return null;
		}
		String book = restoreDocumentDataI.getBook();
		String page = restoreDocumentDataI.getPage();
		TSServerInfoModule module = null;
		
		if(StringUtils.isNotEmpty(restoreDocumentDataI.getInstrumentNumber())) {
			module = getDefaultServerInfo().getModule(INST_MODULE_IDX);
			module.forceValue(0, restoreDocumentDataI.getInstrumentNumber());
			module.forceValue(1, "1");
			module.forceValue(2, "1");
		} else if(StringUtils.isNotEmpty(book) && StringUtils.isNotEmpty(page)) {
			
			List<TSServerInfoModule> modules = new ArrayList<>();
			
			if(DocumentTypes.PLAT.equals(restoreDocumentDataI.getCategory())) {
				for (String type : platTypeIndexes) {
					module = getDefaultServerInfo().getModule(BP_MODULE_IDX);
					module.forceValue(0, book);
					module.forceValue(1, page);
					module.forceValue(2, "1");
					module.forceValue(3, "1");
					module.forceValue(5, type);
					modules.add(module);
				}
			} else {
				for (String type : bookPageTypeIndexes) {
					module = getDefaultServerInfo().getModule(BP_MODULE_IDX);
					module.forceValue(0, book);
					module.forceValue(1, page);
					module.forceValue(2, "1");
					module.forceValue(3, "1");
					module.forceValue(5, type);
					modules.add(module);
				}
			}
				
			return modules;
		} else if(StringUtils.isNotEmpty(restoreDocumentDataI.getDocumentNumber())) {
			module = getDefaultServerInfo().getModule(INST_MODULE_IDX);
			module.forceValue(0, restoreDocumentDataI.getDocumentNumber());
			module.forceValue(1, "1");
			module.forceValue(2, "1");
		} else {
			module = null;
		}
		return module;
	}
	
	@Override
	protected void initFields() {
		
		//downloadParentSiteData();
		
		String countyId = getSearch().getSa().getAtribute(SearchAttributes.P_COUNTY);
		County county;
		try {
			county = County.getCounty(Integer.parseInt(countyId));
			String countyName = county.getName();
			
			if(parentSiteInfo.containsKey(countyName)) {
				class_select = parentSiteInfo.get(countyName).get(CountySpecificInfo.CLASS_SELECT);
				instr_type_select = parentSiteInfo.get(countyName).get(CountySpecificInfo.INSTR_TYPE_SELECT);
				platTypeIndexes = parentSiteInfo.get(countyName).get(CountySpecificInfo.PLAT_TYPES).split(";");
				easementTypeIndexes = parentSiteInfo.get(countyName).get(CountySpecificInfo.EASEMENT_TYPES).split(";");
				restrictionTypeIndexes = parentSiteInfo.get(countyName).get(CountySpecificInfo.RESTRICTION_TYPES).split(";");
				bookPageTypeIndexes = parentSiteInfo.get(countyName).get(CountySpecificInfo.BOOK_PAGE_TYPES).split(";");
				platClassIndexes = parentSiteInfo.get(countyName).get(CountySpecificInfo.PLAT_CLASS_TYPE).split(";");
			}
			
			party_type_select = PARTY_TYPE_SELECT;
		}catch(Exception e) {
			e.printStackTrace();
		}
	}
	
	
	
}
