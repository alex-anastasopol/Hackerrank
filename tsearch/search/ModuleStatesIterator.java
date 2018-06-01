package ro.cst.tsearch.search;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.Vector;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.log4j.Category;
import org.apache.log4j.Logger;

import ro.cst.tsearch.Search;
import ro.cst.tsearch.bean.SearchAttributes;
import ro.cst.tsearch.exceptions.ServerResponseException;
import ro.cst.tsearch.propertyInformation.Instrument;
import ro.cst.tsearch.search.filter.FilterResponse;
import ro.cst.tsearch.search.filter.newfilters.name.GenericNameFilter;
import ro.cst.tsearch.search.module.AddressModuleStatesIterator;
import ro.cst.tsearch.search.module.BookAndPageModuleStatesIteratorSearch;
import ro.cst.tsearch.search.module.BookPageListModuleStatesIterator;
import ro.cst.tsearch.search.module.CompanyNameDerrivator;
import ro.cst.tsearch.search.module.ConfigurableNameIterator;
import ro.cst.tsearch.search.module.DeedBookPageIterator;
import ro.cst.tsearch.search.module.EmptyModuleStatesIterator;
import ro.cst.tsearch.search.module.ImageLookupModuleStatesIteratorSearch;
import ro.cst.tsearch.search.module.InstrumentModuleStatesIteratorSearch;
import ro.cst.tsearch.search.module.InstrumentNoModuleStatesIterator;
import ro.cst.tsearch.search.module.LotModuleStatesIterator;
import ro.cst.tsearch.search.module.OcrOrBootStraperIterator;
import ro.cst.tsearch.search.module.RutherfordAddressIterator;
import ro.cst.tsearch.search.module.ShelbyEPParcelIDModuleStatesIterator;
import ro.cst.tsearch.search.module.SubdivNameModuleStatesIterator;
import ro.cst.tsearch.search.module.SubdivisionModuleStatesIterator;
import ro.cst.tsearch.search.strategy.CounterLikeIterator;
import ro.cst.tsearch.search.strategy.DefaultStatesIterator;
import ro.cst.tsearch.search.strategy.StrategyBasedIterator;
import ro.cst.tsearch.servers.HashCountyToIndex;
import ro.cst.tsearch.servers.SearchDataWrapper;
import ro.cst.tsearch.servers.bean.DataSite;
import ro.cst.tsearch.servers.info.TSServerInfo;
import ro.cst.tsearch.servers.info.TSServerInfoConstants;
import ro.cst.tsearch.servers.info.TSServerInfoFunction;
import ro.cst.tsearch.servers.info.TSServerInfoModule;
import ro.cst.tsearch.servers.response.ParsedResponse;
import ro.cst.tsearch.servers.response.ServerResponse;
import ro.cst.tsearch.servers.types.TSInterface;
import ro.cst.tsearch.servers.types.TSServer;
import ro.cst.tsearch.utils.InstanceManager;
import ro.cst.tsearch.utils.Log;
import ro.cst.tsearch.utils.SearchLogger;
import ro.cst.tsearch.utils.StringUtils;

import com.stewart.ats.base.document.DocumentI;
import com.stewart.ats.base.search.DocumentsManagerI;
import com.stewart.ats.base.warning.ResultsLimitPerModuleWarning;
import com.stewart.ats.base.warning.Warning;

/** 
 * @author elmarie
 */
public class ModuleStatesIterator extends StrategyBasedIterator {

	static final long serialVersionUID = 10000000;

	protected TSServerInfoModule initialState = null;

	protected static final Category logger = Logger.getLogger(ModuleStatesIterator.class);
	protected static final Category loggerDetails =
		Logger.getLogger(Log.DETAILS_PREFIX + ModuleStatesIterator.class.getName());

	public static final int TYPE_DEFAULT = -1;
	public static final int TYPE_REVERSE = 6;
	public static final int TYPE_BOOK_PAGE_LIST = 8;
	public static final int TYPE_ADDRESS__NUMBER_NOT_EMPTY = 9;
	public static final int TYPE_ADDRESS__NUMBER_EMPTY = 10;
	public static final int TYPE_INSTRUMENT_LIST = 11;
	public static final int TYPE_PARCEL_ID_FAKE = 12;
	public static final int TYPE_ASSESSOR_SUBDIV_DESOTO = 14;
	public static final int TYPE_INSTRUMENT_LIST_SEARCH = 15;
	public static final int TYPE_BOOK_PAGE_LIST_SEARCH = 16;
	public static final int TYPE_LOT_SEARCH = 27;
    public static final int TYPE_MISSOURI_SUBDIVISION = 31;
    public static final int TYPE_COMPANY_NAME_DERRIVATOR = 32;
    public static final int HAMILTON_IMAGE_LOOKUP_BOOK_PAGE = 33;
    public static final int TYPE_DEED_BOOK_PAGE_ITERATOR = 39;
    public static final int TYPE_OCR = 46;
    public static final int TYPE_OCR_FULL_OR_BOOTSTRAPER = 47;
    
    public static final int TYPE_BOOK_PAGE_LIST_NOT_AGAIN = 49;
    public static final int TYPE_BOOK_PAGE_LIST_SEARCH_NOT_AGAIN = 50;
    public static final int TYPE_INSTRUMENT_LIST_NOT_AGAIN = 51;
    public static final int TYPE_INSTRUMENT_LIST_SEARCH_NOT_AGAIN = 52;
    
    public static final int TYPE_ADDRESS_RUTH = 58;
    
    public static final int TYPE_REGISTER_NAME_DEFAULT = 60;
    public static final int TYPE_REGISTER_NAME_SINGLEWORD = 61;
    public static final int TYPE_ASSEOR_SHELBY = 62;
    public static final int TYPE_COMPANY_NAME_DERRIVATOR_WITH_COMMA=63;
    public static final int TYPE_REGISTER_NAME_LAST_FIRST=65;
    
    public static final int TYPE_ADDRESS = 66;
    
	protected List<FilterResponse> filters = new ArrayList<FilterResponse>();
	protected List<FilterResponse> filtersForNext = new ArrayList<FilterResponse>();
	protected boolean reverse = false;
	protected boolean preserveName = false;

	private boolean resetOnce = false;
	
	protected long  searchId=-1;
	
	transient protected Search search = null;
	transient protected DataSite dataSite = null;
	
	/**
	 * Use ro.cst.tsearch.search.ModuleStatesIterator(long, DataSite) instead
	 * @param searchId
	 */
	@Deprecated
	public ModuleStatesIterator(long searchId) {
		this.searchId = searchId;
		//logger.debug("new " + this.getClass().getName());
	}
	
	public ModuleStatesIterator(long searchId, DataSite dataSite) {
		this.searchId = searchId;
		this.dataSite = dataSite;
	}

	public ModuleStatesIterator(TSServerInfoModule initial, boolean reverse,long searchId) {
		this.searchId = searchId;
		//logger.debug("new " + this.getClass().getName());
		this.reverse = reverse;
	}

	protected void init(TSServerInfoModule initial, String[] registerNameTokens,long searchId) {
		initFilter(initial);
		initInitialState(initial);
		
		// bug #808 (CR): enable/disable derivations
		boolean  nameDerrivation = true;
		try{
			nameDerrivation = HashCountyToIndex.getCrtServer(searchId, false).isEnabledNameDerivation(InstanceManager.getManager().getCommunityId(searchId));
		}catch(Exception e){
			e.printStackTrace();
		}
		
		if(!nameDerrivation){
			setupStrategy(new String[]{"lf", "slf"});
		} else {
			setupStrategy(registerNameTokens);
		}
	}
	
	public void init(TSServerInfoModule initial) {
		initFilter(initial);
		initInitialState(initial);
		
	////	if (derivType == DERIV_AO_TNREALESTATE)
	//	    setupStrategy1();
	//	else
		
		setupStrategy();
	}
	
	public boolean timeToStop(Decision d) {
		//logger.debug("modulestatesiterator: timeToStop");

		if ((d == null) || (d.getServerResponse() == null)) {
			return false;
		}

		ServerResponse sr = d.getServerResponse();
		ParsedResponse pr = sr.getParsedResponse();

		if (pr.isUnique()
			|| (pr.getResultsCount() == ParsedResponse.UNKNOW_RESULTS_COUNT)) {
			//logger.debug("obtained unique or unknown => stop");
			return true;
		} else if (pr.isMultiple()) {
			//logger.debug("obtained multiple => stop");
			return true;
		} else if (pr.isNone() && isFakeDocumentModule()) {
			//logger.debug("obtained none for instrument list sau book and page list search => create fake file");
			sr.setFakeResponse(true);
			return true;
		} else {
			//logger.debug("obtained none");
			return false;
		}
	}

	protected void initInitialState(TSServerInfoModule initial) {
		if (initial != null) {
			this.initialState = new TSServerInfoModule(initial);
			SearchAttributes sa =
				InstanceManager.getManager()
					.getCurrentInstance(searchId)
					.getCrtSearchContext()
					.getSa();
			this.initialState.setData(new SearchDataWrapper(sa));
		}
	}

	protected void initFilter(TSServerInfoModule initial) {
		if (initial != null) {
			filters.clear();	//clear filters first
				
//				int countNumber = initial.getFilterList().size();
//				if(countNumber == 0) {
//					logger.warn("No filter to add to iterator!!!!!!!!! ARE YOU SURE ????" );
//					logger.warn("No filter to add to iterator!!!!!!!!! ARE YOU SURE ????" );
//					logger.warn("No filter to add to iterator!!!!!!!!! ARE YOU SURE ????" );
//				}
				
			for(FilterResponse fr: initial.getFilterList()){
				filters.add(fr);
			}
		}
	}

	protected void initFilterForNext() {
		filtersForNext.clear();
		TSServerInfoModule crtModule = (TSServerInfoModule) current();
		if (crtModule != null) {
			List<Integer> filterForNextIds = crtModule.getFilterForNextTypes();
			List<FilterResponse> filterForNextObjects = crtModule.getFilterForNextList();
			if(filterForNextObjects.size() > 0) {
				filtersForNext.addAll(filterForNextObjects);
			} else {
				//backup system so we will not need to changed old filters
				for (Iterator<Integer> iter = filterForNextIds.iterator(); iter.hasNext();) {
					int filter_type = iter.next();
					filtersForNext.add(
							FilterResponse.getInstanceForNext(filter_type, crtModule,searchId));
				}
			}
		}
	}
	
	protected void setupStrategy(String[] args) {
		setupStrategy();
	}

	protected void setupStrategy() {
		if ((initialState == null) || (initialState.isEmpty())) {
			setStrategy(new DefaultStatesIterator());
		} else {
			List l = new ArrayList();
			for (int i = 0; i < initialState.getFunctionCount(); i++) {
				TSServerInfoFunction fct = initialState.getFunction(i);
				l.add(new FunctionStatesIterator(fct));
				//logger.debug(" fct " + i +" = " + fct);
			}

			//logger.debug("list = " + l);
			//coleectia trebuie in general reversed...ca sa fct ca:
			//00, 01, 10, 11 si nu ca 00, 10, 01, 11 cum e default
			Collections.reverse(l);
			// in anumite cazuri totusi trebuie schimbata ordinea si ramasa cu cea default
			if (reverse) {
				Collections.reverse(l);
			}
			//logger.debug("reverse list = " + l);
			setStrategy(new CounterLikeIterator(l,searchId));
		}
	}
	


	protected List getCurrentFromStrategy() {
		List l = new ArrayList();
		l.addAll((List) getStrategy().current());
		//logger.debug("list = " + l);

		//coleectia trebuie in general reversed...ca sa fct ca:
		//00, 01, 10, 11 si nu ca 00, 10, 01, 11 cum e default
		Collections.reverse(l);
		// in anumite cazuri totusi trebuie schimbata ordinea
		if (reverse) {
			Collections.reverse(l);
		}
		return l;
	}

	public Object current() {
		List l = getCurrentFromStrategy();
		TSServerInfoModule crtState = new TSServerInfoModule(initialState);

		for (int i = 0; i < l.size(); i++) {
			FunctionStatesIterator fctIter = (FunctionStatesIterator) l.get(i);
			crtState.setFunction(i, (TSServerInfoFunction) fctIter.current());
			//logger.debug(" fct " + i +" = " + fctIter.current());
		}

		//logger.debug(" crtState for " + initialState + " =  " + crtState);
		return crtState;
	}

	public TSServerInfoModule getInitial() {
		return new TSServerInfoModule(initialState);
	}

	public static ModuleStatesIterator getInstance(TSServerInfoModule module,long searchId) {

		/* 
		   each time a new iterator is created, the bookPageType is cleared
		   the bookPageType is currently set only by KYJeffersonRO referenceTO 
		   search module in automatic
		 */ 
		InstanceManager.getManager().getCurrentInstance(searchId).clearBookPageType();
		
		int type = module.getIteratorType();

		switch (type) {
			case TYPE_PARCEL_ID_FAKE :
				{
					ModuleStatesIterator msi =
						new ShelbyEPParcelIDModuleStatesIterator(searchId);
					msi.init(module);
					return msi;
				}

			case TYPE_ADDRESS:
			case TYPE_ADDRESS__NUMBER_NOT_EMPTY :
				{
					AddressModuleStatesIterator msi =
						new AddressModuleStatesIterator(searchId);
					msi.init(module);
					if (type == TYPE_ADDRESS__NUMBER_NOT_EMPTY && StringUtils
						.isStringBlank(
							msi.getRefAddress().getStreetNoAsString())) {
						return new EmptyModuleStatesIterator(searchId);
					} else {
						return msi;
					}
				}
			case TYPE_ADDRESS__NUMBER_EMPTY :
				{
					AddressModuleStatesIterator msi =
						new AddressModuleStatesIterator(searchId);
					msi.init(module);
					msi.getRefAddress().getStreetNo().clear();
					return msi;
				}
			case TYPE_BOOK_PAGE_LIST :
			case TYPE_BOOK_PAGE_LIST_NOT_AGAIN:
				{
					ModuleStatesIterator msi = new BookPageListModuleStatesIterator(searchId);					
					if(type == TYPE_BOOK_PAGE_LIST_NOT_AGAIN){
						msi.setSearchIfPresent(false);
					}
					msi.init(module);
					return msi;
				}
			case TYPE_BOOK_PAGE_LIST_SEARCH :
			case TYPE_BOOK_PAGE_LIST_SEARCH_NOT_AGAIN :
				{
					ModuleStatesIterator msi = new BookAndPageModuleStatesIteratorSearch(searchId);					
					if(type == TYPE_BOOK_PAGE_LIST_SEARCH_NOT_AGAIN){
						msi.setSearchIfPresent(false);
					}
					msi.init(module);
					return msi;
				}
            case TYPE_DEED_BOOK_PAGE_ITERATOR:
            {
                ModuleStatesIterator msi =
                    new DeedBookPageIterator(searchId);
                msi.init(module);
                return msi;                
            }
            case HAMILTON_IMAGE_LOOKUP_BOOK_PAGE:
            {
                ModuleStatesIterator msi = new ImageLookupModuleStatesIteratorSearch(searchId);
                msi.init(module);
                return msi;                
            }
			case TYPE_INSTRUMENT_LIST :
			case TYPE_INSTRUMENT_LIST_NOT_AGAIN:
				{
					ModuleStatesIterator msi = new InstrumentNoModuleStatesIterator(searchId);					
					if(type == TYPE_INSTRUMENT_LIST_NOT_AGAIN){
						msi.setSearchIfPresent(false);
					}
					msi.init(module);
					return msi;
				}
			case TYPE_INSTRUMENT_LIST_SEARCH :
			case TYPE_INSTRUMENT_LIST_SEARCH_NOT_AGAIN :
				{
					ModuleStatesIterator msi = new InstrumentModuleStatesIteratorSearch(searchId);
					if(type == TYPE_INSTRUMENT_LIST_SEARCH_NOT_AGAIN){
						msi.setSearchIfPresent(false);
					}
					msi.init(module);
					return msi;
				}
            case TYPE_OCR:
            {
                ModuleStatesIterator msi = new OcrOrBootStraperIterator(searchId);
                msi.init(module);
                return msi;                
            }
            case TYPE_OCR_FULL_OR_BOOTSTRAPER:
            {
            	//does not remove leading zeroes from book / pages / instruments
                ModuleStatesIterator msi = new OcrOrBootStraperIterator( false ,searchId);
                msi.setSearchIfPresent(false); // default not search again for a doc                
                msi.init(module);
                return msi;            	
            }
            case TYPE_MISSOURI_SUBDIVISION:
            {
                ModuleStatesIterator msi =
                    new SubdivisionModuleStatesIterator(searchId);
                msi.init(module);
                return msi;                
            }
            case TYPE_COMPANY_NAME_DERRIVATOR:
            case TYPE_COMPANY_NAME_DERRIVATOR_WITH_COMMA:
            {
                ModuleStatesIterator msi = new CompanyNameDerrivator(searchId);
                if (type == TYPE_COMPANY_NAME_DERRIVATOR_WITH_COMMA){
                	msi.setSearchIfPresent(false);
                }
                msi.init( module );
                return msi;
            }
			case TYPE_REVERSE :
				{
					ModuleStatesIterator msi = new ModuleStatesIterator(searchId);
					msi.reverse = true;
					msi.init(module);
					return msi;
				}
			case TYPE_ASSESSOR_SUBDIV_DESOTO :
				{
					ModuleStatesIterator msi =
						new SubdivNameModuleStatesIterator(searchId);
					msi.init(module);
					return msi;
				}
			
			case TYPE_LOT_SEARCH :
			{
				ModuleStatesIterator msi = new LotModuleStatesIterator(searchId);
				msi.init(module);
				return msi;
			}
			case TYPE_ADDRESS_RUTH :
			{
				ModuleStatesIterator msi = new RutherfordAddressIterator(searchId);
				msi.init(module);
				return msi;				
			}
			case TYPE_REGISTER_NAME_DEFAULT:
			{
				ConfigurableNameIterator cmsi = new ConfigurableNameIterator(searchId);
				cmsi.setDerivPatterns(new String[]{"L;f;", "L;m;"});
				cmsi.init(module);
				return cmsi;
			}
			case TYPE_REGISTER_NAME_LAST_FIRST:
			{
				ConfigurableNameIterator cmsi = new ConfigurableNameIterator(searchId);
				cmsi.setDerivPatterns(new String[]{"L;F;"});
				cmsi.init(module);
				return cmsi;
			}
			case TYPE_REGISTER_NAME_SINGLEWORD:
			{
				ConfigurableNameIterator cmsi = new ConfigurableNameIterator(searchId);
				cmsi.setDerivPatterns(new String[]{"L f;;", "L m;;"});
				cmsi.init(module);
				return cmsi;				
			}
			case TYPE_ASSEOR_SHELBY:
			{
				ConfigurableNameIterator cmsi = new ConfigurableNameIterator(searchId);
				cmsi.setDerivPatterns(new String[]{"L;F;", "L;f;", "L;M;", "L;m;"});
				cmsi.init(module);
				return cmsi;				
			}
			default :
				{
					ModuleStatesIterator msi = new ModuleStatesIterator(searchId);
					msi.init(module);
					return msi;
				}
		}

	}

	public void filterResponse(ServerResponse sr, TSInterface intrfServer)
		throws ServerResponseException {

		initFilterForNext();

		int initialSize = sr.getParsedResponse().getResultsCount();
		appendNextResults(sr, intrfServer, initialSize);
		int finalSize = sr.getParsedResponse().getResultsCount();
		if(initialSize != finalSize){
			SearchLogger.info("Total number of <span class='rtype'>intermediate</span> results: <span class='number'>" + finalSize + "</span>.<br/>", searchId);
		}
		
		if(finalSize >= Search.MAX_NO_OF_DOCUMENTS_FROM_SITE_TO_ANALYZE) {
			SearchLogger.info("Found at least <span class='number'>" +
					finalSize + "</span> <span class='rtype'>intermediate</span> results, more that the ATS limit of " + 
					Search.MAX_NO_OF_DOCUMENTS_FROM_SITE_TO_ANALYZE + ". <br/>", searchId);
			
			String moduleName = ((TSServerInfoModule) current()).getLabel();
			if(StringUtils.isEmpty(moduleName)) {
				moduleName = "Unknown search module";
			}
			
			ResultsLimitPerModuleWarning warning = new ResultsLimitPerModuleWarning(
					Warning.WARNING_RESULTS_FOUND_LIMIT_PER_MODULE_ID,
					intrfServer.getDataSite().getSiteTypeAbrev(),
					finalSize,
					Search.MAX_NO_OF_DOCUMENTS_FROM_SITE_TO_ANALYZE, 
					moduleName); 
			SearchLogger.info("<br><font color=\"red\"><b>WARNING: </b>" + warning.toString() + "</font><br>", searchId);
			getSearch().getSearchFlags().addWarning(warning);
			
			sr.getParsedResponse().setResultRows( new Vector() );
		
		}
		
		//remove unnecessary rows before filtering
		sr = intrfServer.removeUnnecessaryResults( sr );
		sr.setTsInterface(intrfServer);
		ParsedResponse parsedResponse = sr.getParsedResponse();
		Vector resultsToFilter = parsedResponse.getResultRows();
		for (Object object : resultsToFilter) {
			if(object instanceof ParsedResponse) {
				((ParsedResponse)object).setTsInterface(intrfServer);
			}
		}
		
        if ( !((TSServer)intrfServer).continueSeachOnThisServer() || ((TSServer)intrfServer).skipCurrentSite() || ((TSServer)intrfServer).isStopAutomaticSearch() )
            return;
         
		if (sr.getParsedResponse().getResultsCount() >= 1)
		{
			
			//we need to make this copy to avoid unnecessary java.util.ConcurrentModificationException
			FilterResponse[] currentFilters = filters.toArray(new FilterResponse[filters.size()]);
			
			for (int i = 0; i < currentFilters.length; i++) {
				
				FilterResponse filter = currentFilters[i];
				
				if ( !((TSServer)intrfServer).continueSeachOnThisServer() || ((TSServer)intrfServer).skipCurrentSite() || ((TSServer)intrfServer).isStopAutomaticSearch() )
					return;
				boolean applyFilter = true;
				
				if ( sr.getParsedResponse().getResultsCount()<= 1 && filter instanceof GenericNameFilter) {
					GenericNameFilter nameFilter = (GenericNameFilter) filter;
					if(nameFilter.isSkipUnique())
						applyFilter = false;
				}
				if(applyFilter)
					filter.filterResponse(sr);
			}
			
			Vector resultRows = sr.getParsedResponse().getResultRows();
			if(resultRows.size() > Search.MAX_NO_OF_DOCUMENTS_SAVED_PER_MODULE) {
				if(initialState != null && initialState.getValidatorList().isEmpty()) {
					
					String moduleName = initialState.getLabel();
					if(StringUtils.isEmpty(moduleName)) {
						moduleName = "Unknown search module";
					}
					
					ResultsLimitPerModuleWarning warning = new ResultsLimitPerModuleWarning(
							Warning.WARNING_RESULTS_LIMIT_PER_MODULE_ID,
							intrfServer.getDataSite().getSiteTypeAbrev(),
							resultRows.size(),
							Search.MAX_NO_OF_DOCUMENTS_SAVED_PER_MODULE, 
							moduleName); 
					SearchLogger.info("<br><font color=\"red\"><b>WARNING: </b>" + warning.toString() + "</font><br>", searchId);
					getSearch().getSearchFlags().addWarning(warning);
					
					sr.getParsedResponse().setResultRows(new Vector<>());
				}
			}
			
		} else {
			
		}
		
	}

	public boolean appendNextResults(ServerResponse Response, TSInterface intrfServer, int alreadyFoundResults)
		throws ServerResponseException {
	    
	    if ( !(((TSServer)intrfServer).continueSeach() || 
               Response.isParentSiteSearch()) ||
             !((TSServer)intrfServer).continueSeachOnThisServer() ||
             ((TSServer)intrfServer).skipCurrentSite() || 
             ((TSServer)intrfServer).isStopAutomaticSearch())
	        return false;

		long originalRowsNo = Response.getParsedResponse().getResultsCount();

		for (Iterator iter = filtersForNext.iterator(); iter.hasNext();) 
        {
			FilterResponse filter = (FilterResponse) iter.next();
			filter.filterResponse(Response);
			if (Response.getErrorCode() == ServerResponse.NOT_PERFECT_MATCH_WARNING_FIRST) {
				
				Response.getParsedResponse().setNextLink("");
				Response.getParsedResponse().setFooter("</table><br>");
				
			    return true;
			}
		}

		String alllinkNext = Response.getParsedResponse().getNextLink();

		if (StringUtils.isStringBlank(alllinkNext)) {
			return false;
		}

		Response.getParsedResponse().setNextLink("");
		//String footer = Response.getParsedResponse().getFooter();
		Response.getParsedResponse().setFooter("</table><br>");//footer);

		TSServerInfoModule crtModule = (TSServerInfoModule) current();
		if (!crtModule.isGoOnNextLink()) {
			return false;
		}

		long afterFilteringRowsNo = Response.getParsedResponse().getResultsCount();
		if (originalRowsNo - afterFilteringRowsNo > 5) {
			return false;
		}
		
		// only on KYJeffersonRO. put a check here please
        /*
		String p2 = InstanceManager.getCurrentInstance().getCrtSearchContext().getP2();
		String p1 = InstanceManager.getCurrentInstance().getCrtSearchContext().getP1();
		if("073".equals(p1) && "1".equals(p2)){
			if(originalRowsNo != afterFilteringRowsNo){
				return false;
			}
		}*/

		Matcher m =
			Pattern
				.compile(
					"<a\\s+href=['\\\"]?([^>'\\\"]*)['\\\">]",
					Pattern.CASE_INSENSITIVE)
				.matcher(alllinkNext);
		if (m.find()) {
			String linkNext = m.group(1);
			//logger.debug(" actualLink= " + linkNext);

			if (linkNext.endsWith(Response.getQuerry())) {
				//logger.debug("  next-link points to the same page!!  don't go there");
				if(!StringUtils.isEmpty(Response.getQuerry())){
					return false;
				}
			}
			
			if(alreadyFoundResults > Search.MAX_NO_OF_DOCUMENTS_FROM_SITE_TO_ANALYZE) {
				return false;
			} 
			

			// these have been added on KYJeffersonRO, to bypass the above test
			// since all next pages have the same address on this site
			linkNext = linkNext.replaceFirst("&unqDmyNmbr=\\d+","");
			
			ServerResponse stmpResponse = intrfServer.GetLink(linkNext, true);
			int nextCount = stmpResponse.getParsedResponse().getResultsCount();
			SearchLogger.info("Found <span class='number'>" +
					nextCount + "</span> more <span class='rtype'>intermediate</span> results. <br/>", searchId);

			appendNextResults(stmpResponse, intrfServer, alreadyFoundResults + nextCount);

			String nextPageText = stmpResponse.getParsedResponse().getResponse();
			Vector nextPageRows = stmpResponse.getParsedResponse().getResultRows();

			String crtPageText = Response.getParsedResponse().getResponse();
			crtPageText += "<br> " + nextPageText;
			Response.getParsedResponse().setOnlyResponse(crtPageText);

			Vector parsedRows = Response.getParsedResponse().getResultRows();
			parsedRows.addAll(nextPageRows);
			Response.getParsedResponse().setResultRows(parsedRows);
			//logger.debug(" found rows = " + parsedRows.size());

			return true;
		} else {
			return false;
		}
	}

	protected boolean isFakeDocumentModule() {
		if (!isOnRegisterServer()) {
			return false;
		} else
			return (
				this.initialState.getIteratorType()
					== ModuleStatesIterator.TYPE_INSTRUMENT_LIST
					|| this.initialState.getIteratorType()
						== ModuleStatesIterator.TYPE_INSTRUMENT_LIST_SEARCH
					|| this.initialState.getIteratorType()
						== ModuleStatesIterator.TYPE_BOOK_PAGE_LIST
					|| this.initialState.getIteratorType()
						== ModuleStatesIterator.TYPE_BOOK_PAGE_LIST_SEARCH
					|| this.initialState.getIteratorType()
						== ModuleStatesIterator.TYPE_OCR
					|| this.initialState.getIteratorType()
						== ModuleStatesIterator.TYPE_OCR_FULL_OR_BOOTSTRAPER
                    )
				&& !this.initialState.hasFakeFunctions()
				&& (this.initialState.getModuleIdx()
					!= TSServerInfo.BOOK_AND_PAGE_LOCAL_MODULE_IDX)
				&& (this.initialState.getModuleIdx()
					!= TSServerInfo.PARCEL_ID_MODULE_IDX)
				&& !Boolean.TRUE.equals(this.initialState.getExtraInformation(TSServerInfoConstants.EXTRA_PARAM_DO_NOT_FAKE_DOCUMENT));
	}

	protected boolean isOnRegisterServer() {
		String p2 =
			InstanceManager.getManager().getCurrentInstance(searchId).getCrtSearchContext().getP2();
		return p2.trim().equals("1"); // 1 => Register
	}
	public void setPreserveNameIfBlank(boolean pres)
	{
	    preserveName = pres;
	}
	public boolean getPreserveNameIfBlank()
	{
	    return preserveName;
	}
	
	// used by book-page list and instrument list iterators
	protected boolean searchIfPresent = true;
	public void setSearchIfPresent(boolean value){
		searchIfPresent = value;
	}
   
	/**
     * Receives a list of instruments and creates another list containing only instruments not already retrieved
     * @param list input insrument list
     * @param searchId id of current search
     * @return filtered instrument list
     */
     public static List removeAlreadyRetrieved(List list, long searchId){
 	   
     	// create set of all instrument numbers and book-pages
     	Set<String> savedInstruments = new HashSet<String>();
		Search search = InstanceManager.getManager().getCurrentInstance(searchId).getCrtSearchContext();
		 Search currentSearch = InstanceManager.getManager().getCurrentInstance(searchId).getCrtSearchContext();
	        
	        List instrList = new ArrayList();
	        DocumentsManagerI manager = currentSearch.getDocManager();
	        
	        try{
	        	manager.getAccess();
	        	Collection<DocumentI> allChapters =  manager.getDocumentsList( true );
		        for( DocumentI doc:allChapters)
		        {
				    // add book_page
				    String book = doc.getBook();
				    String page = doc.getPage();
				    if(book != null && page != null && !"".equals(book) &&!"".equals(page)){
				    	savedInstruments.add(book + "_" + page);
				    }
				    // add instrument
				    String instr =doc.getInstno();
				    if(instr != null && !"".equals(instr)){
				    	savedInstruments.add(instr);
				    }
		        }
	        }
	        finally{
	        	manager.releaseAccess();
	        }
			
		// list of instruments that remain after removing the already brought
		List<Instrument> retVal = new ArrayList<Instrument>();			
		for(Iterator it=list.iterator(); it.hasNext(); ){
			
			// leave it alone if it's not an instrument
			Object obj = it.next();
			if(!(obj instanceof Instrument)){ 
				continue; 
			}
			Instrument inst = (Instrument)obj;

			// check if it's already retrieved
			String book = inst.getBookNo();
			String page = inst.getPageNo();
			String instNo = inst.getInstrumentNo();
			
			if(search.hasSavedBookPage(book, page) || search.hasSavedInst(instNo)){
				//System.err.println("Removed instrument: " + instNo);
				continue;
			}
				
			if(savedInstruments.contains("" + book + "_" + page) || savedInstruments.contains("" + instNo)){
				//System.err.println("Removed instrument: " + instNo);
				continue;
			}
			//System.out.println("Added instrument: " + instNo);
			retVal.add(inst);
		}
		return retVal;
     }

	public boolean needInitAgain() {
		return resetOnce;
	}

	public void setInitAgain(boolean resetOnce) {
		this.resetOnce = resetOnce;
	}
	
	public Search getSearch(){
		if(search == null) {
			return (search = InstanceManager.getManager().getCurrentInstance(searchId).getCrtSearchContext());
		}
		else if(searchId != search.getID()) {
			return (search = InstanceManager.getManager().getCurrentInstance(searchId).getCrtSearchContext());
		} else {
			return search;
		}
	}
	
	/**
     * get current search attribute. used to make code smaller/clearer
     */
    protected String getSearchAttribute(String key){
    	return getSearch().getSa().getAtribute(key);    	
    }

	/**
	 * Method used to get the number of documents for this data source where this iterator is used<br>
	 * If dataSite is not set the result is 0
	 * @return
	 */
	protected int getDocumentsManagerDocSize() {
		
		if(dataSite == null) {
			logger.warn("DataSite not set in " + this.getClass() + " for search id " + searchId);
			return 0;
		}
		
		DocumentsManagerI docManager = getSearch().getDocManager();
		int newSiteDocuments = 0;
		try {
			docManager.getAccess();
			newSiteDocuments = docManager.getDocumentsWithDataSource(false, dataSite.getSiteTypeAbrev()).size();
		} finally {
			docManager.releaseAccess();
		}
		return newSiteDocuments;
	}
	
	public void setDataSite(DataSite dataSite) {
		this.dataSite = dataSite;
	}
	
	public DataSite getDataSite() {
		return dataSite;
	}
    
}
