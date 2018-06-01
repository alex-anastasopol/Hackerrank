package ro.cst.tsearch.servers.types;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Vector;

import com.stewart.ats.base.document.DocumentI;
import com.stewart.ats.base.document.Instrument;
import com.stewart.ats.base.document.InstrumentI;
import com.stewart.ats.base.search.DocumentsManagerI;


import ro.cst.tsearch.Search;
import ro.cst.tsearch.bean.SearchAttributes;
import ro.cst.tsearch.exceptions.ServerResponseException;
import ro.cst.tsearch.search.FunctionStatesIterator;
import ro.cst.tsearch.search.filter.FilterResponse;
import ro.cst.tsearch.search.filter.RejectAlreadySavedDocumentsFilterResponse;
import ro.cst.tsearch.search.filter.newfilters.address.AddressFilterFactory;
import ro.cst.tsearch.search.filter.newfilters.address.CityFilterFactory;
import ro.cst.tsearch.search.filter.newfilters.address.GenericAddressFilter;
import ro.cst.tsearch.search.filter.newfilters.legal.GenericLegal;
import ro.cst.tsearch.search.filter.newfilters.legal.LegalFilterFactory;
import ro.cst.tsearch.search.filter.newfilters.misc.NoImageNoExceptionFilter;
import ro.cst.tsearch.search.filter.newfilters.pin.PINFilterFactory;
import ro.cst.tsearch.search.iterator.PlatBookPageIterator;
import ro.cst.tsearch.search.validator.DocsValidator;
import ro.cst.tsearch.servers.HashCountyToIndex;
import ro.cst.tsearch.servers.info.TSServerInfo;
import ro.cst.tsearch.servers.info.TSServerInfoConstants;
import ro.cst.tsearch.servers.info.TSServerInfoFunction;
import ro.cst.tsearch.servers.info.TSServerInfoModule;
import ro.cst.tsearch.servers.response.ServerResponse;
import ro.cst.tsearch.utils.InstanceManager;
import ro.cst.tsearch.utils.StringUtils;


public class AKStewartPriorPF extends XXStewartPriorPF {

    static final long serialVersionUID = 10000000;

    
    public AKStewartPriorPF(long searchId) {
    	super(searchId);
        resultType = MULTIPLE_RESULT_TYPE;
    }
    
    public AKStewartPriorPF(String rsRequestSolverName, String rsSitePath,
            String rsServerID, String rsPrmNameLink, long searchId, int mid) {
        super(rsRequestSolverName, rsSitePath, rsServerID, rsPrmNameLink, searchId, mid);
        resultType = MULTIPLE_RESULT_TYPE;
    }
    
    @Override
    public void setServerID(int serverID) {
        super.setServerID(serverID);
        treatAllDocumentsTheSame = true;
    }

    public class AKPlatBookPageIterator extends PlatBookPageIterator {

    	/**
    	 * 
    	 */
    	private static final long serialVersionUID = 1L;

    	private boolean loadFromSearchAttributes = true;
    	private boolean loadFromPlatDocuments = true;
    	private boolean alsoSearchWithoutLeadingZeroes = true;
    	
    	public AKPlatBookPageIterator(long searchId) {
    		super(searchId);
    	}
    	
    	

    	@Override
    	protected List<InstrumentI> createDerrivations() {
    		Search global = getSearch();
    		List<InstrumentI> derivations = new Vector<InstrumentI>();
    		HashSet<String> listsForNow = new HashSet<String>();
    		for (InstrumentI instrumentI : derivations) {
    			String key = "Book_"  + instrumentI.getBook() + "**Page_" + instrumentI.getPage();
    			listsForNow.add(key);
    		}
    		
    		if(loadFromSearchAttributes) {
    			String platBook = global.getSa().getAtribute(SearchAttributes.LD_BOOKNO_1);
    			String platPage = global.getSa().getAtribute(SearchAttributes.LD_PAGENO_1);
    			if(StringUtils.isNotEmpty(platBook) && StringUtils.isNotEmpty(platPage)) {
    				InstrumentI instrument = new Instrument();
    				instrument.setBook(platBook);
    				instrument.setPage(platPage);
    				String key = "Book_"  + instrument.getBook() + "**Page_" + instrument.getPage();
    				if(!listsForNow.contains(key)) {
    					listsForNow.add(key);
    					derivations.add(instrument);
    				}
    				
    				if(alsoSearchWithoutLeadingZeroes) {
    					platBook = platBook.replaceAll("^0+", "");
    					platPage = platPage.replaceAll("^0+", "");
    					instrument = new Instrument();
    					instrument.setBook(platBook);
    					instrument.setPage(platPage);
    					key = "Book_"  + instrument.getBook() + "**Page_" + instrument.getPage();
    					if(!listsForNow.contains(key)) {
    						listsForNow.add(key);
    						derivations.add(instrument);
    					}
    				}
    				
    				
    			}
    		}
    		if(loadFromPlatDocuments) {
    			DocumentsManagerI documentsManagerI = global.getDocManager();
    			try {
    				documentsManagerI.getAccess();
    				List<DocumentI> availablePlats = documentsManagerI.getDocumentsWithDocType("PLAT");
    				for (DocumentI documentI : availablePlats) {
    					if(documentI.hasBookPage()) {
    						String key = "Book_"  + documentI.getBook() + "**Page_" + documentI.getPage();
    						if(!listsForNow.contains(key)) {
    							listsForNow.add(key);
    							derivations.add(documentI);
    						}
    						if(alsoSearchWithoutLeadingZeroes) {
    							String platBook = documentI.getBook().replaceAll("^0+", "");
    							String platPage = documentI.getPage().replaceAll("^0+", "");
    							InstrumentI instrument = new Instrument();
    							instrument.setBook(platBook);
    							instrument.setPage(platPage);
    							key = "Book_"  + instrument.getBook() + "**Page_" + instrument.getPage();
    							if(!listsForNow.contains(key)) {
    								listsForNow.add(key);
    								derivations.add(instrument);
    							}
    						}
    					} else if(documentI.hasInstrNo() && !documentI.hasBookPage()) {
    						String instNo = documentI.getInstno();
    						if (instNo.matches("\\d+-\\d+")){
    							String book = instNo.substring(0, instNo.indexOf("-"));
    							String page = instNo.substring(instNo.indexOf("-") + 1, instNo.length());
    							
	    						String key = "Book_"  + book + "**Page_" + page;
	    						if(!listsForNow.contains(key)) {
	    							listsForNow.add(key);
	    							derivations.add(documentI);
	    						}
	    						if(alsoSearchWithoutLeadingZeroes) {
	    							String platBook = book.replaceAll("^0+", "");
	    							String platPage = page.replaceAll("^0+", "");
	    							InstrumentI instrument = new Instrument();
	    							instrument.setBook(platBook);
	    							instrument.setPage(platPage);
	    							key = "Book_"  + instrument.getBook() + "**Page_" + instrument.getPage();
	    							if(!listsForNow.contains(key)) {
	    								listsForNow.add(key);
	    								derivations.add(instrument);
	    							}
	    						}
    						}
    					}
    				}
    				
    			} catch (Exception e) {
    				e.printStackTrace();
    			} finally {
    				documentsManagerI.releaseAccess();
    			}
    		}
    		return derivations;
    	}

    	@Override
    	protected void loadDerrivation(TSServerInfoModule module, InstrumentI state) {
    		for (Object functionObject : module.getFunctionList()) {
    			if (functionObject instanceof TSServerInfoFunction) {
    				TSServerInfoFunction function = (TSServerInfoFunction) functionObject;
    				if(function.getIteratorType() == FunctionStatesIterator.ITERATOR_TYPE_BOOK_FAKE) {
    					function.setParamValue(state.getBook());
    				} else if(function.getIteratorType() == FunctionStatesIterator.ITERATOR_TYPE_PAGE_FAKE) {
    					function.setParamValue(state.getPage());
    				}
    			}
    		}
    	}

    }
    
    @Override
    protected void setModulesForAutoSearch(TSServerInfo serverInfo) {

        List<TSServerInfoModule> l = new ArrayList<TSServerInfoModule>();

        Search global = InstanceManager.getManager().getCurrentInstance(searchId).getCrtSearchContext();
		int searchType = global.getSearchType();
		
		if(searchType == Search.AUTOMATIC_SEARCH) {
        
	        TSServerInfoModule m = null;
	        GenericLegal defaultLegalFilter = (GenericLegal) LegalFilterFactory.getDefaultLegalFilter(searchId);
	        defaultLegalFilter.setEnableLotUnitFullEquivalence(true);
	        GenericAddressFilter addressFilter = AddressFilterFactory.getGenericAddressHighPassFilter( searchId, 0.77d );
	        FilterResponse cityFilter 		= CityFilterFactory.getCityFilter(searchId, 0.6d);
	        addressFilter.setEnableUnit(false);
	        addressFilter.setTryAddressFromDocument(true);
	        
	        DocsValidator rejectSavedDocuments = new RejectAlreadySavedDocumentsFilterResponse(searchId).getValidator();
	        DocsValidator rejectNoImageNoExceptionDocuments = new NoImageNoExceptionFilter(searchId).getValidator();
	        DocsValidator pinValidator = PINFilterFactory.getDefaultPinFilter(searchId).getValidator();
	                
	     // lot, block , plat volume book - page search
	        {
				m = new TSServerInfoModule(serverInfo.getModule(TSServerInfo.GENERIC_MODULE_IDX));
				m.clearSaKeys();
				m.addExtraInformation(TSServerInfoConstants.EXTRA_PARAM_MODULE_DESCRIPTION,
						TSServerInfoConstants.VALUE_PARAM_PLATVOLUME_PLATPAGE);
				m.addExtraInformation("SPECIAL_REQUEST", "MUST_STOP");
				m.addExtraInformation("DATASOURCE", "PF");
		    	m.forceValue(10, global.getSa().getAtribute(SearchAttributes.P_STATE_ABREV));
				m.forceValue(11, global.getSa().getAtribute(SearchAttributes.P_COUNTY_FIPS));
				m.setSaKey(16, SearchAttributes.LD_LOTNO);
	        	m.setSaKey(17, SearchAttributes.LD_SUBDIV_BLOCK);
				m.setIteratorType(22, FunctionStatesIterator.ITERATOR_TYPE_BOOK_FAKE);
				m.setIteratorType(24, FunctionStatesIterator.ITERATOR_TYPE_PAGE_FAKE);
				m.addFilter(cityFilter);
		    	m.addValidator(rejectSavedDocuments);
		    	m.addValidator(rejectNoImageNoExceptionDocuments);
		    	m.addValidator(pinValidator);
		    	m.addValidator(addressFilter.getValidator());
		    	m.addValidator(defaultLegalFilter.getValidator());
		    	PlatBookPageIterator iterator = new AKPlatBookPageIterator(searchId);
		    	m.addIterator(iterator);
		    	l.add(m);
	        }
	      
	        // block, plat volume book - page search
	        {
				m = new TSServerInfoModule(serverInfo.getModule(TSServerInfo.GENERIC_MODULE_IDX));
				m.clearSaKeys();
				m.addExtraInformation(TSServerInfoConstants.EXTRA_PARAM_MODULE_DESCRIPTION,
						TSServerInfoConstants.VALUE_PARAM_PLATVOLUME_PLATPAGE);
				m.addExtraInformation("SPECIAL_REQUEST", "MUST_STOP");
				m.addExtraInformation("DATASOURCE", "PF");
		    	m.forceValue(10, global.getSa().getAtribute(SearchAttributes.P_STATE_ABREV));
				m.forceValue(11, global.getSa().getAtribute(SearchAttributes.P_COUNTY_FIPS));
				m.setSaKey(17, SearchAttributes.LD_SUBDIV_BLOCK);
				m.setIteratorType(22, FunctionStatesIterator.ITERATOR_TYPE_BOOK_FAKE);
				m.setIteratorType(24, FunctionStatesIterator.ITERATOR_TYPE_PAGE_FAKE);
				m.addFilter(cityFilter);
		    	m.addValidator(rejectSavedDocuments);
		    	m.addValidator(rejectNoImageNoExceptionDocuments);
		    	m.addValidator(pinValidator);
		    	m.addValidator(addressFilter.getValidator());
		    	m.addValidator(defaultLegalFilter.getValidator());
		    	PlatBookPageIterator iterator = new AKPlatBookPageIterator(searchId);
		    	m.addIterator(iterator);
		    	l.add(m);
	        }
			
	     // plat volume book - page search
	        {
				m = new TSServerInfoModule(serverInfo.getModule(TSServerInfo.GENERIC_MODULE_IDX));
				m.clearSaKeys();
				m.addExtraInformation(TSServerInfoConstants.EXTRA_PARAM_MODULE_DESCRIPTION,
						TSServerInfoConstants.VALUE_PARAM_PLATVOLUME_PLATPAGE);
				m.addExtraInformation("SPECIAL_REQUEST", "MUST_STOP");
				m.addExtraInformation("DATASOURCE", "PF");
		    	m.forceValue(10, global.getSa().getAtribute(SearchAttributes.P_STATE_ABREV));
				m.forceValue(11, global.getSa().getAtribute(SearchAttributes.P_COUNTY_FIPS));
				m.setIteratorType(22, FunctionStatesIterator.ITERATOR_TYPE_BOOK_FAKE);
				m.setIteratorType(24, FunctionStatesIterator.ITERATOR_TYPE_PAGE_FAKE);
				m.addFilter(cityFilter);
		    	m.addValidator(rejectSavedDocuments);
		    	m.addValidator(rejectNoImageNoExceptionDocuments);
		    	m.addValidator(pinValidator);
		    	m.addValidator(addressFilter.getValidator());
		    	m.addValidator(defaultLegalFilter.getValidator());
		    	PlatBookPageIterator iterator = new AKPlatBookPageIterator(searchId);
		    	m.addIterator(iterator);
		    	l.add(m);
	        }
			
		}
        serverInfo.setModulesForAutoSearch(l);
    }
    
    @Override
	protected ServerResponse SearchBy(boolean resetQuery, TSServerInfoModule module, Object sd)throws ServerResponseException {
		
		if (mSearch.getSearchType() == Search.AUTOMATIC_SEARCH){
				Search global = InstanceManager.getManager().getCurrentInstance(searchId).getCrtSearchContext();
				DocumentsManagerI m = global.getDocManager();
				try{
					m.getAccess();
					if (m.getDocumentsWithDataSource(false, "PF").size() == 0){
						return super.SearchBy(resetQuery, module, sd);
					} else {
						return new ServerResponse();
					}
				}
				finally{
					m.releaseAccess();
				}
		}
		
		return super.SearchBy(resetQuery, module, sd);
	}
	
}