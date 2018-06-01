package ro.cst.tsearch.search.iterator.instrument;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Vector;

import ro.cst.tsearch.Search;
import ro.cst.tsearch.search.FunctionStatesIterator;
import ro.cst.tsearch.search.GenericRuntimeIterator;
import ro.cst.tsearch.search.filter.FilterResponse;
import ro.cst.tsearch.search.filter.newfilters.legal.GenericInstrumentFilter;
import ro.cst.tsearch.servers.HashCountyToIndex;
import ro.cst.tsearch.servers.ServletServerComm;
import ro.cst.tsearch.servers.bean.DataSite;
import ro.cst.tsearch.servers.info.TSServerInfoFunction;
import ro.cst.tsearch.servers.info.TSServerInfoModule;
import ro.cst.tsearch.titledocument.abstracts.DocumentTypes;
import ro.cst.tsearch.utils.StringUtils;

import com.stewart.ats.base.document.DocumentI;
import com.stewart.ats.base.document.InstrumentI;
import com.stewart.ats.base.document.RegisterDocumentI;
import com.stewart.ats.base.search.DocumentsManagerI;
import com.stewart.ats.tsrindex.client.SimpleChapterUtils;
import com.stewart.ats.tsrindex.client.SimpleChapterUtils.DType;

public class InstrumentGenericIterator extends
		GenericRuntimeIterator<InstrumentI> {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	private boolean enableBookPage = false;
	private boolean enableInstrumentNumber = true;
	private boolean enableDocumentNumber = false;
	private boolean loadFromRoLike = false;
	private String[] roDoctypesToLoad = null;
	private String[] dsToLoad = null;
	private boolean doNotCheckIfItExists = false;
	private String[] instrumentTypes = null;
	private String[] forceInstrumentTypes = null;
	private boolean useInstrumentType = false;
	private boolean removeLeadingZerosBP = false;
	private boolean checkIfWasAlreadyInvalidated = true;
	private boolean checkOnlyFakeDocs = false;
	private boolean checkRelatedOfFakeDocs = false;
	
	/**
	 * Use ro.cst.tsearch.search.iterator.instrument.InstrumentGenericIterator(long, DataSite)
	 * @param searchId
	 */
	@Deprecated
	public InstrumentGenericIterator(long searchId) {
		super(searchId);
		setInitAgain(true);
	}
	
	/**
	 * Iterator is forced to init again
	 * @param searchId
	 * @param dataSite
	 */
	public InstrumentGenericIterator(long searchId, DataSite dataSite) {
		super(searchId);
		setDataSite(dataSite);
		setInitAgain(true);
	}
	
	public boolean isEnableBookPage() {
		return enableBookPage;
	}

	public final InstrumentGenericIterator enableBookPage() {
		this.enableBookPage = true;
		this.enableInstrumentNumber = false;
		this.enableDocumentNumber = false;
		return this;
	}
	public boolean isEnableInstrumentNumber() {
		return enableInstrumentNumber;
	}
	public final InstrumentGenericIterator enableInstrumentNumber() {
		this.enableInstrumentNumber = true;
		this.enableBookPage = false;
		this.enableDocumentNumber = false;
		return this;
	}
	public final InstrumentGenericIterator enableDocumentNumber() {
		this.enableInstrumentNumber = false;
		this.enableBookPage = false;
		this.enableDocumentNumber = true;
		return this;
	}
	public boolean isEnableDocumentNumber() {
		return enableDocumentNumber;
	}

	public boolean isLoadFromRoLike() {
		return loadFromRoLike;
	}
	public InstrumentGenericIterator setLoadFromRoLike(boolean loadFromRoLike) {
		this.loadFromRoLike = loadFromRoLike;
		return this;
	}

	@Override
	public List<InstrumentI> createDerrivations() {
		
		Search global = getSearch();
		
		List<InstrumentI> result = new Vector<InstrumentI>();
		
		boolean addReferences = true;
		
		//used to keep track of what was already added
		HashSet<String> listsForNow = new HashSet<String>();
		
		DocumentsManagerI manager = global.getDocManager();
		try{
			manager.getAccess();
			List<DocumentI> list = null;
			if(!isLoadFromRoLike()) {
				list = manager.getDocumentsWithType( true, DType.ASSESOR, DType.TAX, DType.CITYTAX );
			} else {
				list = new ArrayList<DocumentI>();
				if(roDoctypesToLoad == null) {
					if(dsToLoad == null) {
						list.addAll(manager.getRoLikeDocumentList());	
					} else {
						if (isCheckOnlyFakeDocs()){
							list.addAll(manager.getFakeDocumentsWithDataSource(false, true, dsToLoad));
						} else{
							list.addAll(manager.getDocumentsWithDataSource(false, dsToLoad));
						}
					}
					
				} else {
					if(dsToLoad == null) {
						list.addAll(manager.getDocumentsWithDocType(false, roDoctypesToLoad));
					} else {
						if (isCheckOnlyFakeDocs()){
							list.addAll(manager.getFakeDocumentsWithDataSource(false, true, dsToLoad));
						} else{
							list.addAll(manager.getDocumentsWithDoctypeAndServerDocType(false, roDoctypesToLoad, dsToLoad));
						}
					}
				}
			}
			if (isCheckOnlyFakeDocs() && isCheckRelatedOfFakeDocs()){
				global.addReferencesToDocs(true);
				for (DocumentI assessor:list){
					try {
						InstrumentI instrumentI = assessor.getInstrument().clone();
			    		useInstrumentI(result, listsForNow, manager, instrumentI);
			    		
			    		for (RegisterDocumentI reg : assessor.getReferences()){
							try {
								InstrumentI instR = reg.getInstrument().clone();
					    		useInstrumentI(result, listsForNow, manager, instR);
							} catch (Exception e) {
								logger.error("Error while processing References", e);
							}
						}
			    		for (InstrumentI instr : assessor.getParsedReferences()) {
							try {
								InstrumentI instrPR = instr.clone();
								useInstrumentI(result, listsForNow, manager, instrPR);
							} catch (Exception e) {
								logger.error("Error while processing Parsed References", e);
							}
						}
					} catch (Exception e) {
						logger.error("Error while processing References", e);
					}
				}
			} else if(roDoctypesToLoad == null) {
				for(DocumentI assessor:list){
					addReferences = true;
					if (assessor.isOneOf(DType.ASSESOR, DType.TAX, DType.CITYTAX) && !HashCountyToIndex.isLegalBootstrapEnabled(global.getCommId(), assessor.getSiteId())) {
						addReferences = false;
					}
					if (addReferences) {
						if(!(assessor.isOneOf(DType.ROLIKE) && assessor.getReferences().size() > ServletServerComm.MAX_CROSS_REFS_SEARCH)) {
							for(RegisterDocumentI reg : assessor.getReferences()){
								try {
									InstrumentI instrumentI = reg.getInstrument().clone();
									
						    		useInstrumentI(result, listsForNow, manager, instrumentI);
								} catch (Exception e) {
									logger.error("Error while processing References", e);
								}
					    		
							}
						}
						if(!(assessor.isOneOf(DType.ROLIKE) && assessor.getParsedReferences().size() > ServletServerComm.MAX_CROSS_REFS_SEARCH)) {
							for (InstrumentI instrumentI : assessor.getParsedReferences()) {
								try {
									instrumentI = instrumentI.clone();
									useInstrumentI(result, listsForNow, manager, instrumentI);
								} catch (Exception e) {
									logger.error("Error while processing Parsed References", e);
								}
							}
						}
					}
				}
			} else {
				for(DocumentI assessor:list){
					try {
						InstrumentI instrumentI = assessor.getInstrument().clone();
						
			    		useInstrumentI(result, listsForNow, manager, instrumentI);
					} catch (Exception e) {
						logger.error("Error while processing References", e);
					}
				}
			}
		}
		finally {
			manager.releaseAccess();
		}
		
		return result;
	}
	
	/**
	 * if the module has year iterator, keep only the derivations which have year 
	 */
	@Override
	protected List<InstrumentI> cleanDerivationsList(List<InstrumentI> list, TSServerInfoModule module) {
		boolean hasYearIterator = false;
		for (TSServerInfoFunction function : module.getFunctionList()) {
			if (function.getIteratorType() == FunctionStatesIterator.ITERATOR_TYPE_YEAR) {
				hasYearIterator = true;
				break;
			}
		}
		if (hasYearIterator) {
			List<InstrumentI> newList = new ArrayList<InstrumentI>();
			for (InstrumentI instrument: list) {
				if (instrument.hasYear()) {
					newList.add(instrument);
				}
			}
			list = newList;
		}
		return list;
	}

	@Override
	protected void loadDerrivation(TSServerInfoModule module, InstrumentI state) {
		
		List<FilterResponse> allFilters = module.getFilterList();
		GenericInstrumentFilter gif = null;
		HashMap<String, String> filterCriteria = null;
		if(allFilters != null) {
			for (FilterResponse filterResponse : allFilters) {
				if (filterResponse instanceof GenericInstrumentFilter) {
					gif = (GenericInstrumentFilter) filterResponse;
					filterCriteria = new HashMap<String, String>();
					gif.clearFilters();
				}
			}
		}
		for (Object functionObject : module.getFunctionList()) {
			if (functionObject instanceof TSServerInfoFunction) {
				TSServerInfoFunction function = (TSServerInfoFunction) functionObject;
				if(function.getIteratorType() == FunctionStatesIterator.ITERATOR_TYPE_INSTRUMENT_LIST_FAKE) {
					function.setParamValue(getInstrumentNoFrom(state, filterCriteria));
				} else if(function.getIteratorType() == FunctionStatesIterator.ITERATOR_TYPE_YEAR) {
					function.setParamValue(getYearFrom(state, filterCriteria));
				} else if(function.getIteratorType() == FunctionStatesIterator.ITERATOR_TYPE_SUFFIX) {
					function.setParamValue(getSuffixFrom(state, filterCriteria));
				} else if(function.getIteratorType() == FunctionStatesIterator.ITERATOR_TYPE_BOOK_FAKE) {
					function.setParamValue(getBookFrom(state, filterCriteria));
				} else if(function.getIteratorType() == FunctionStatesIterator.ITERATOR_TYPE_PAGE_FAKE) {
					function.setParamValue(getPageFrom(state, filterCriteria));
				} else if(function.getIteratorType() == FunctionStatesIterator.ITERATOR_TYPE_DOCNO_LIST_FAKE) {
					function.setParamValue(getDocNoFrom(state, filterCriteria));
				} else if(function.getIteratorType() == FunctionStatesIterator.ITERATOR_TYPE_BP_TYPE) {
					function.setParamValue(getBookTypeFrom(state, filterCriteria));
				} else if(function.getIteratorType() == FunctionStatesIterator.ITERATOR_TYPE_BOOK_PAGE_AS_INSTRUMENT_FAKE) {
					function.setParamValue(getInstrumentNoFromBookAndPage(state, filterCriteria));
				} else if(function.getIteratorType() == FunctionStatesIterator.ITERATOR_TYPE_DOCTYPE_SEARCH) {
					function.setParamValue(getDocTypeFrom(state, filterCriteria));
				} else if(function.getIteratorType() == FunctionStatesIterator.ITERATOR_TYPE_SERVER_DOCTYPE_SEARCH) {
					function.setParamValue(getServerDocTypeFrom(state, filterCriteria));
				}
			}
		}
		if(gif != null) {
			gif.addDocumentCriteria(filterCriteria);
		}
	}
	
	public String getSuffixFrom(InstrumentI state, HashMap<String, String> filterCriteria) {
		return "";
	}
	public String getBookTypeFrom(InstrumentI state, HashMap<String, String> filterCriteria) {
		return state.getBookType();
	}
	public String getDocTypeFrom(InstrumentI state, HashMap<String, String> filterCriteria) {
		return state.getDocType();
	}
	
	public String getServerDocTypeFrom(InstrumentI state, HashMap<String, String> filterCriteria) {
		return state.getServerDocType();
	}
	
	public String getYearFrom(InstrumentI state, HashMap<String, String> filterCriteria) {
		if(state.getYear() != SimpleChapterUtils.UNDEFINED_YEAR) {
			return Integer.toString(state.getYear());
		} else {
			return "";
		}
	}

	public String getInstrumentNoFrom(InstrumentI state, HashMap<String, String> filterCriteria) {
		return state.getInstno().replaceFirst("^0+", "").trim();
	}
	
	public String getInstrumentNoFromBookAndPage(InstrumentI state, HashMap<String, String> filterCriteria){
		return "";
	}
	
	public String getDocNoFrom(InstrumentI state, HashMap<String, String> filterCriteria) {
		return state.getDocno().replaceFirst("^0+", "").trim();
	}
	public String getBookFrom(InstrumentI state, HashMap<String, String> filterCriteria) {
		if(filterCriteria != null) {
			filterCriteria.put("Book", state.getBook());
		}
		return state.getBook().trim();
	}
	public String getPageFrom(InstrumentI state, HashMap<String, String> filterCriteria) {
		if(filterCriteria != null) {
			filterCriteria.put("Page", state.getPage());
		}
		return state.getPage().trim();
	}

	
	protected void useInstrumentI(List<InstrumentI> result,
			HashSet<String> listsForNow, DocumentsManagerI manager,
			InstrumentI instrumentI) {
		instrumentI.setDocType(DocumentTypes.MISCELLANEOUS);
		instrumentI.setDocSubType(DocumentTypes.MISCELLANEOUS);
		if(isEnableInstrumentNumber()) {
			processEnableInstrumentNo(result, listsForNow, manager, instrumentI);
		}
		if(isEnableDocumentNumber()) {
			processEnableDocumentNumber(result, listsForNow, manager,
					instrumentI);
		}
		if(isEnableBookPage()) {
			processEnableBP(result, listsForNow, manager, instrumentI);
		}
	}

	protected void processEnableInstrumentNo(List<InstrumentI> result,
			HashSet<String> listsForNow, DocumentsManagerI manager,
			InstrumentI instrumentI) {
		String instrumentNo = cleanInstrumentNo(instrumentI);
		
		if(org.apache.commons.lang.StringUtils.isBlank(instrumentNo)) {
			return;
		}
		
		if (isCheckIfWasAlreadyInvalidated()){
			if (getSearch().getSa().isInvalidatedInstrument(instrumentNo)){
				return;
			}
		}
		if(instrumentTypes == null || instrumentTypes.length == 0) {
			String instrumentType = null;
			if(useInstrumentType) {
				if(org.apache.commons.lang.StringUtils.isNotBlank(instrumentI.getBookType())) {
					instrumentType = instrumentI.getBookType();
				} else if(forceInstrumentTypes == null || forceInstrumentTypes.length == 0) {
					return;	
				} else {
					for (String forceInstrumentType : forceInstrumentTypes) {
						String key = getKeyInstrumentNo(instrumentI, forceInstrumentType);
						if(!listsForNow.contains(key)) {
							instrumentI.setInstno(instrumentNo);
							
							List<DocumentI> almostLike = getDocumentsWithInstrumentsFlexible(manager, instrumentI);
							if(doNotCheckIfItExists || almostLike.isEmpty()) {
								listsForNow.add(key);
								
								InstrumentI instrumentClone = instrumentI.clone();
								instrumentClone.setBookType(forceInstrumentType);	
								result.add(instrumentClone);
							}
						}
					}
					return;
				}
			}
				
			String key = getKeyInstrumentNo(instrumentI, instrumentType);
			
			if(!listsForNow.contains(key)) {
				instrumentI.setInstno(instrumentNo);
				List<DocumentI> almostLike = getDocumentsWithInstrumentsFlexible(manager, instrumentI);
				if(doNotCheckIfItExists || almostLike.isEmpty()) {
					listsForNow.add(key);
					result.add(instrumentI);
				}
			}
		} else {
			for (String instrumentType : instrumentTypes) {
				String key = getKeyInstrumentNo(instrumentI, instrumentType);
				if(!listsForNow.contains(key)) {
					instrumentI.setInstno(instrumentNo);
					
					List<DocumentI> almostLike = getDocumentsWithInstrumentsFlexible(manager, instrumentI);
					if(doNotCheckIfItExists || almostLike.isEmpty()) {
						listsForNow.add(key);
						
						InstrumentI instrumentClone = instrumentI.clone();
						instrumentClone.setBookType(instrumentType);	
						result.add(instrumentClone);
					}
				}
			}
		}
	}

	protected List<DocumentI> getDocumentsWithInstrumentsFlexible(DocumentsManagerI manager, InstrumentI instrumentI) {
		return manager.getDocumentsWithInstrumentsFlexible(false, instrumentI);
	}

	protected String getKeyInstrumentNo(InstrumentI instrumentI, String instrumentType) {
		String instrumentNo = cleanInstrumentNo(instrumentI.getInstno(), instrumentI.getYear());
		
		if(org.apache.commons.lang.StringUtils.isBlank(instrumentType)) {
			return "Instrument=" + instrumentNo;
		} else {
			return "Instrument=" + instrumentNo + "_ Type=" + instrumentType;
		}
	}

	public void setYear(InstrumentI instrumentI){
		if(instrumentI.hasYear()) {
			instrumentI = instrumentI.clone();
			instrumentI.setYear(-1);
		}
	}
	
	protected void processEnableBP(List<InstrumentI> result,
			HashSet<String> listsForNow, DocumentsManagerI manager,
			InstrumentI instrumentI) {
		
		instrumentI = instrumentI.clone();
		instrumentI.setDisableBookPage(false);
		instrumentI.setDisableInstrNo(true);
		instrumentI.setDisableDocNo(true);
		
		String book = cleanBook(instrumentI.getBook());
		String page = cleanPage(instrumentI.getPage());
		
		setYear(instrumentI);
		
		if(StringUtils.isNotEmpty(book) && StringUtils.isNotEmpty(page)) {
			
			if (isCheckIfWasAlreadyInvalidated()){
				if (getSearch().getSa().isInvalidatedInstrument(instrumentI.getBook(), instrumentI.getPage())){
					return;
				}
			}
			if(getInstrumentTypes() == null || getInstrumentTypes().length == 0) {
				String instrumentType = null;
				if(useInstrumentType) {
					if(org.apache.commons.lang.StringUtils.isNotBlank(instrumentI.getBookType())) {
						instrumentType = instrumentI.getBookType();
					} else if(forceInstrumentTypes == null || forceInstrumentTypes.length == 0) {
						return;	
					} else {
						for (String forceInstrumentType : forceInstrumentTypes) {
							String key = getKeyBookPage(instrumentI, forceInstrumentType);
							
							if(!listsForNow.contains(key)) {
								instrumentI.setBook(book);
								instrumentI.setPage(page);
								//if has the same instrumentNumber we will not save it
								List<DocumentI> almostLike = getDocumentsWithInstrumentsFlexible(manager, instrumentI);
								if(doNotCheckIfItExists || almostLike.isEmpty()) {
									listsForNow.add(key);
									
									instrumentI = instrumentI.clone();
									instrumentI.setBookType(forceInstrumentType);
									result.add(instrumentI);
								}
							}
						}
						return;
					}
				}
				
				String key = getKeyBookPage(instrumentI, instrumentType);
				
				if(!listsForNow.contains(key)) {
					instrumentI.setBook(book);
					instrumentI.setPage(page);
					//if has the same instrumentNumber we will not save it
					List<DocumentI> almostLike = getDocumentsWithInstrumentsFlexible(manager, instrumentI);
					if(doNotCheckIfItExists || almostLike.isEmpty()) {
						listsForNow.add(key);
						result.add(instrumentI);
					}
				}	
			} else {
				for (String instrumentType : getInstrumentTypes()) {
					String key = getKeyBookPage(instrumentI, instrumentType);
					
					if(!listsForNow.contains(key)) {
						instrumentI.setBook(book);
						instrumentI.setPage(page);
						//if has the same instrumentNumber we will not save it
						List<DocumentI> almostLike = getDocumentsWithInstrumentsFlexible(manager, instrumentI);
						if(doNotCheckIfItExists || almostLike.isEmpty()) {
							listsForNow.add(key);
							
							instrumentI = instrumentI.clone();
							instrumentI.setBookType(instrumentType);
							result.add(instrumentI);
						}
					}
				}
			
			}
		}
	}

	protected String getKeyBookPage(InstrumentI instrumentI, String instrumentType) {
		if(org.apache.commons.lang.StringUtils.isBlank(instrumentType)) {
			return "Book=" + cleanBook(instrumentI.getBook()) + "_Page=" + cleanPage(instrumentI.getPage());
		} else {
			return "Book=" + cleanBook(instrumentI.getBook()) + "_Page=" + cleanPage(instrumentI.getPage()) + "_Type=" + instrumentType;
		}
	}

	protected void processEnableDocumentNumber(List<InstrumentI> result,
			HashSet<String> listsForNow, DocumentsManagerI manager,
			InstrumentI instrumentI) {
		
		String docNo = cleanInstrumentNo(instrumentI.getDocno(), instrumentI.getYear());
		
		if (isCheckIfWasAlreadyInvalidated()){
			if (getSearch().getSa().isInvalidatedInstrument(docNo)){
				return;
			}
		}
		
		if(instrumentTypes == null || instrumentTypes.length == 0) {
			String instrumentNo = cleanInstrumentNo(instrumentI.getDocno(), instrumentI.getYear());
			String instrumentType = null;
			if(useInstrumentType) {
				if(org.apache.commons.lang.StringUtils.isNotBlank(instrumentI.getBookType())) {
					instrumentType = instrumentI.getBookType();
				} else if(forceInstrumentTypes == null || forceInstrumentTypes.length == 0) {
					return;	
				} else {
					for (String forceInstrumentType : forceInstrumentTypes) {
						String key = getKeyDocumentNo(instrumentI, forceInstrumentType);
						if(StringUtils.isNotEmpty(instrumentNo) && !listsForNow.contains(key)) {
							instrumentI.setDocno(instrumentNo);
							List<DocumentI> almostLike = getDocumentsWithInstrumentsFlexible(manager, instrumentI);
							if(doNotCheckIfItExists || almostLike.isEmpty()) {
								listsForNow.add(key);
								result.add(instrumentI);
							}
						}
					}
					return;
				}
			}
				
			String key = getKeyDocumentNo(instrumentI, instrumentType);
			if(StringUtils.isNotEmpty(instrumentNo) && !listsForNow.contains(key)) {
				instrumentI.setDocno(instrumentNo);
				List<DocumentI> almostLike = getDocumentsWithInstrumentsFlexible(manager, instrumentI);
				if(doNotCheckIfItExists || almostLike.isEmpty()) {
					listsForNow.add(key);
					result.add(instrumentI);
				}
			}
		} else {
			for (String instrumentType : instrumentTypes) {
				String instrumentNo = cleanInstrumentNo(instrumentI.getDocno(), instrumentI.getYear());
				String key = getKeyDocumentNo(instrumentI, instrumentType);
				if(StringUtils.isNotEmpty(instrumentNo) && !listsForNow.contains(key)) {
					instrumentI.setDocno(instrumentNo);
					List<DocumentI> almostLike = getDocumentsWithInstrumentsFlexible(manager, instrumentI);
					if(doNotCheckIfItExists || almostLike.isEmpty()) {
						listsForNow.add(key);
						result.add(instrumentI);
					}
				}
			}
		}
		
		
	}

	protected String getKeyDocumentNo(InstrumentI instrumentI, String instrumentType) {
		String instrumentNo = cleanInstrumentNo(instrumentI.getDocno(), instrumentI.getYear());
		if(org.apache.commons.lang.StringUtils.isBlank(instrumentType)) {
			return "DocumentNo=" + instrumentNo;
		} else {
			return "DocumentNo=" + instrumentNo + "_Type=" + instrumentType;
		}
	}
	
	

	protected String cleanInstrumentNo(String instno, int year) {
		return instno;
	}
	
	protected String cleanInstrumentNo(InstrumentI instrument){
		return cleanInstrumentNo(instrument.getInstno(), instrument.getYear());
	}
	
	protected String cleanBook(String input) {
		return cleanBookOrPage(input);
	}
	protected String cleanPage(String input) {
		return cleanBookOrPage(input);
	}
	protected String cleanBookOrPage(String input) {
		if(input == null) {
			return null;
		}
		if(!removeLeadingZerosBP) {
			return input;
		}
		return input.replaceFirst("^0+", "");
	}
	

	public String[] getRoDoctypesToLoad() {
		return roDoctypesToLoad;
	}

	public void setRoDoctypesToLoad(String[] roDoctypesToLoad) {
		this.roDoctypesToLoad = roDoctypesToLoad;
	}

	public boolean isDoNotCheckIfItExists() {
		return doNotCheckIfItExists;
	}

	public void setDoNotCheckIfItExists(boolean doNotCheckIfItExists) {
		this.doNotCheckIfItExists = doNotCheckIfItExists;
	}

	public String[] getInstrumentTypes() {
		return instrumentTypes;
	}

	public void setInstrumentTypes(String[] instrumentTypes) {
		this.instrumentTypes = instrumentTypes;
	}

	public String[] getForceInstrumentTypes() {
		return forceInstrumentTypes;
	}

	public void setForceInstrumentTypes(String[] forceInstrumentTypes) {
		this.forceInstrumentTypes = forceInstrumentTypes;
	}

	public String[] getDsToLoad() {
		return dsToLoad;
	}

	public void setDsToLoad(String[] dsToLoad) {
		this.dsToLoad = dsToLoad;
	}

	public boolean isUseInstrumentType() {
		return useInstrumentType;
	}

	public void setUseInstrumentType(boolean useInstrumentType) {
		this.useInstrumentType = useInstrumentType;
	}

	public boolean isRemoveLeadingZerosBP() {
		return removeLeadingZerosBP;
	}

	public void setRemoveLeadingZerosBP(boolean removeLeadingZerosBP) {
		this.removeLeadingZerosBP = removeLeadingZerosBP;
	}

	public boolean isCheckIfWasAlreadyInvalidated() {
		return checkIfWasAlreadyInvalidated;
	}

	public void setCheckIfWasAlreadyInvalidated(boolean checkIfWasAlreadyInvalidated) {
		this.checkIfWasAlreadyInvalidated = checkIfWasAlreadyInvalidated;
	}

	/**
	 * @return the checkOnlyFakeDocs
	 */
	public boolean isCheckOnlyFakeDocs() {
		return checkOnlyFakeDocs;
	}

	/**
	 * @param checkOnlyFakeDocs the checkOnlyFakeDocs to set
	 */
	public void setCheckOnlyFakeDocs(boolean checkOnlyFakeDocs) {
		this.checkOnlyFakeDocs = checkOnlyFakeDocs;
	}

	/**
	 * @return the checkRelatedOfFakeDocs
	 */
	public boolean isCheckRelatedOfFakeDocs() {
		return checkRelatedOfFakeDocs;
	}

	/**
	 * @param checkRelatedOfFakeDocs the checkRelatedOfFakeDocs to set
	 */
	public void setCheckRelatedOfFakeDocs(boolean checkRelatedOfFakeDocs) {
		this.checkRelatedOfFakeDocs = checkRelatedOfFakeDocs;
	}

}
