package ro.cst.tsearch.servers.types;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.Vector;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.log4j.Category;
import org.apache.log4j.Logger;
import org.htmlparser.filters.TagNameFilter;
import org.htmlparser.util.NodeList;

import ro.cst.tsearch.Search;
import ro.cst.tsearch.bean.SearchAttributes;
import ro.cst.tsearch.connection.http.HTTPRequest;
import ro.cst.tsearch.connection.http.HTTPResponse;
import ro.cst.tsearch.connection.http2.HttpManager;
import ro.cst.tsearch.connection.http2.HttpSite;
import ro.cst.tsearch.exceptions.ServerResponseException;
import ro.cst.tsearch.generic.Util;
import ro.cst.tsearch.parser.HtmlParser3;
import ro.cst.tsearch.search.FunctionStatesIterator;
import ro.cst.tsearch.search.ModuleStatesIterator;
import ro.cst.tsearch.search.filter.BetweenDatesFilterResponse;
import ro.cst.tsearch.search.filter.FilterResponse;
import ro.cst.tsearch.search.filter.newfilters.address.AddressFilterFactory;
import ro.cst.tsearch.search.filter.newfilters.date.DateFilterFactory;
import ro.cst.tsearch.search.filter.newfilters.date.LastTransferDateFilter;
import ro.cst.tsearch.search.filter.newfilters.legal.GenericInstrumentFilter;
import ro.cst.tsearch.search.filter.newfilters.legal.LegalFilterFactory;
import ro.cst.tsearch.search.filter.newfilters.name.GenericNameFilter;
import ro.cst.tsearch.search.filter.newfilters.name.NameFilterFactory;
import ro.cst.tsearch.search.filter.newfilters.pin.PINFilterFactory;
import ro.cst.tsearch.search.iterator.ModuleStatesIteratorFactory;
import ro.cst.tsearch.search.module.ConfigurableNameIterator;
import ro.cst.tsearch.search.validator.DocsValidator;
import ro.cst.tsearch.servers.HashCountyToIndex;
import ro.cst.tsearch.servers.SearchDataWrapper;
import ro.cst.tsearch.servers.TSConnectionURL;
import ro.cst.tsearch.servers.info.TSServerInfo;
import ro.cst.tsearch.servers.info.TSServerInfoConstants;
import ro.cst.tsearch.servers.info.TSServerInfoModule;
import ro.cst.tsearch.servers.response.ImageLinkInPage;
import ro.cst.tsearch.servers.response.LinkInPage;
import ro.cst.tsearch.servers.response.ParsedResponse;
import ro.cst.tsearch.servers.response.Parser;
import ro.cst.tsearch.servers.response.ServerResponse;
import ro.cst.tsearch.servlet.URLConnectionReader;
import ro.cst.tsearch.utils.GBManager;
import ro.cst.tsearch.utils.HttpUtils;
import ro.cst.tsearch.utils.IndividualLogger;
import ro.cst.tsearch.utils.InstanceManager;
import ro.cst.tsearch.utils.SearchLogger;
import ro.cst.tsearch.utils.StringEquivalents;
import ro.cst.tsearch.utils.StringUtils;

import com.stewart.ats.base.document.DocumentI;
import com.stewart.ats.base.document.InstrumentI;
import com.stewart.ats.base.document.RegisterDocumentI;
import com.stewart.ats.base.document.RestoreDocumentDataI;
import com.stewart.ats.base.name.NameI;
import com.stewart.ats.base.search.DocumentsManagerI;
import com.stewart.ats.tsrindex.client.SimpleChapterUtils.DType;

@SuppressWarnings("deprecation")
public class TNWilliamsonRO extends TSServer implements TSServerROLikeI {
	
	protected static final Category logger= Logger.getLogger(TNWilliamsonRO.class);
	
	protected final int ID_DETAILS1 = 33101;
	protected final int ID_DETAILS2 = 33102;
	
	static final long serialVersionUID = 10000000;
	private final String IMAGES_SITE = "www.rod.williamson-tn.org";
	private final String IMAGES_SITE_IP = "www.rod.williamson-tn.org";

	//used in ParseResponse to identify what parse algorithm to apply
	private boolean downloadingForSave = false;
	private String msLastdb = "1";

	private static final Pattern certDatePattern = Pattern.compile("(?ism)<b>Date</b>.*?<b>(.*?)</b>");
	
	private static final Pattern CROSS_REFS_LINK = Pattern.compile("(?is)link\\s*=\\s*(/ts[^\\\"]+)");
	
	
	public TSServerInfo getDefaultServerInfo()
	{
		TSServerInfo msiServerInfoDefault=super.getDefaultServerInfo();
		 setModulesForGoBackOneLevelSearch( msiServerInfoDefault);
		 return msiServerInfoDefault;
	}
	
	protected void printSubdivisionException (){
		 Search search = getSearch();
		 if( search!=null && !search.isSubdivisionExceptionPrintedRO() && search.getSearchType() == Search.AUTOMATIC_SEARCH &&
				 "false".equals(search.getSa().getAtribute(SearchAttributes.SEARCHFINISH))) {
				 search.setSubdivisionExceptionPrintedRO(true);
				 IndividualLogger.info( "Will not search with subdivision name because either subdivision " +
				 		"is missing or lot/unit is mising  for validation.",searchId);
			     SearchLogger.info("Will not search with subdivision name because either subdivision " +
					 		"is missing or lot/unit is mising  for validation.",searchId);;	 
		 }
	 }
	
	 public boolean searchWithSubdivision(){
		 SearchAttributes sa = getSearchAttributes();
		
		 //we must have subdivision name
		 if(StringUtils.isEmpty(sa.getAtribute( SearchAttributes.LD_SUBDIV_NAME ))){
			 return false;
		 }
		 
		//we must have lot or unit
		 if(StringUtils.isEmpty(sa.getAtribute( SearchAttributes.LD_LOTNO)) && 
				 StringUtils.isEmpty(sa.getAtribute( SearchAttributes.LD_SUBDIV_UNIT))){
			 return false;
		 }
		 
		 return true;
	 }
	
	private static Set<InstrumentI> getAllAoAndTaxReferences(Search search) {
		DocumentsManagerI manager = search.getDocManager();
		Set<InstrumentI> allAoRef = new HashSet<InstrumentI>();
		try {
			manager.getAccess();
			List<DocumentI> list = manager.getDocumentsWithType(true,
					DType.ASSESOR, DType.TAX, DType.CITYTAX);
			for (DocumentI assessor : list) {
				if (HashCountyToIndex.isLegalBootstrapEnabled(search.getCommId(), assessor.getSiteId())) {
					for (RegisterDocumentI reg : assessor.getReferences()) {
						allAoRef.add(reg.getInstrument());
					}
					allAoRef.addAll(assessor.getParsedReferences());
				}
			}
		} finally {
			manager.releaseAccess();
		}
		return removeEmptyReferences(allAoRef);
	}

	private static Set<InstrumentI> removeEmptyReferences(Set<InstrumentI> allAo) {
		Set<InstrumentI> ret = new HashSet<InstrumentI>();
		for (InstrumentI i : allAo) {
			if (i.hasBookPage() || i.hasInstrNo()) {
				ret.add(i);
			}
		}
		return ret;
	}

	private boolean addAoAndTaxReferenceSearches(TSServerInfo serverInfo,
			List<TSServerInfoModule> modules, Set<InstrumentI> allAoRef,
			long searchId, boolean isUpdate, DocsValidator[] validator_array) {
		boolean atLeastOne = false;
		final Set<String> searched = new HashSet<String>();

		for (InstrumentI inst : allAoRef) {
			boolean temp = addBookPageSearch(inst, serverInfo, modules,
					searchId, searched, isUpdate, validator_array);
			atLeastOne = atLeastOne || temp;
			temp = addInstNoSearch(inst, serverInfo, modules, searchId,
					searched, isUpdate, validator_array);
			atLeastOne = atLeastOne || temp;
		}
		return atLeastOne;
	}

	private static boolean addBookPageSearch(InstrumentI inst,
			TSServerInfo serverInfo, List<TSServerInfoModule> modules,
			long searchId, Set<String> searched, boolean isUpdate, DocsValidator[] validator_array) {

		if (inst.hasBookPage()) {
			String originalB = inst.getBook();
			String originalP = inst.getPage();

			String book = originalB.replaceFirst("^0+", "");
			String page = originalP.replaceFirst("^0+", "");
			if (!searched.contains(book + "_" + page)) {
				searched.add(book + "_" + page);
			} else {
				return false;
			}

			TSServerInfoModule module = new TSServerInfoModule(
					serverInfo.getModule(TSServerInfo.BOOK_AND_PAGE_MODULE_IDX));
			module.clearSaKeys();
			module.setData(0, book);
			module.setData(1, page);
			HashMap<String, String> filterCriteria = new HashMap<String, String>();
			filterCriteria.put("Book", book);
			filterCriteria.put("Page", page);
			GenericInstrumentFilter filter = new GenericInstrumentFilter(
					searchId, filterCriteria);
			module.getFilterList().clear();
			module.addFilter(filter);
			
			module.addValidator(BetweenDatesFilterResponse.getDefaultIntervalFilter(searchId).getValidator());
			
			for(DocsValidator v : validator_array)
				module.addCrossRefValidator(v);
					
			modules.add(module);
			return true;
		}
		return false;
	}

	private boolean addInstNoSearch(InstrumentI inst, TSServerInfo serverInfo,
			List<TSServerInfoModule> modules, long searchId,
			Set<String> searched, boolean isUpdate, DocsValidator[] validator_array) {
		if (inst.hasInstrNo()) {

			String instr = inst.getInstno().replaceFirst("^0+", "");
			if (!searched.contains(instr)) {
				searched.add(instr);
			} else {
				return false;
			}
			TSServerInfoModule module = new TSServerInfoModule(
					serverInfo.getModule(TSServerInfo.INSTR_NO_MODULE_IDX));
			module.clearSaKeys();
			module.setData(0, instr);
			if (isUpdate) {
				module.addFilter(new BetweenDatesFilterResponse(searchId));
			}
			
			module.addValidator(BetweenDatesFilterResponse.getDefaultIntervalFilter(searchId).getValidator());
			
			for(DocsValidator v : validator_array)
				module.addCrossRefValidator(v);
			
			modules.add(module);
			return true;
		}
		return false;
	}
	 
	protected void setModulesForAutoSearch(TSServerInfo serverInfo)
	{
		List<TSServerInfoModule> l = new ArrayList<TSServerInfoModule>();
		TSServerInfoModule m;

		SearchAttributes sa = getSearchAttributes();
		
		FilterResponse addressHighPassFilterResponse = AddressFilterFactory.getAddressHighPassFilter(searchId, 0.8d);
		DocsValidator defaultLegalValidator = LegalFilterFactory.getDefaultLegalFilter(searchId).getValidator();
		DocsValidator pinValidator = PINFilterFactory.getPinFilter(searchId,true,true).getValidator(); 
		DocsValidator addressHighPassValidator = addressHighPassFilterResponse.getValidator();
	    DocsValidator lastTransferDateValidator = (new LastTransferDateFilter(searchId)).getValidator();
		DocsValidator recordedDateValidator = BetweenDatesFilterResponse.getDefaultIntervalFilter(searchId).getValidator();
		DocsValidator subdivisionNameValidator = NameFilterFactory.getDefaultNameFilterForSubdivision(
				searchId).getValidator();
		boolean validateWithDates = applyDateFilter();
		
		Search global = InstanceManager.getManager().getCurrentInstance(searchId).getCrtSearchContext();

		Set<InstrumentI> allAoRef = getAllAoAndTaxReferences(global);

		DocsValidator validator_array[] = {recordedDateValidator,defaultLegalValidator,addressHighPassValidator,pinValidator,subdivisionNameValidator };

		
		// get all AO TR cross ref
		addAoAndTaxReferenceSearches(serverInfo, l, allAoRef, searchId,
				isUpdate(), validator_array);
		
		// search by book and page extracted from AO 
//		m = new TSServerInfoModule(serverInfo.getModule(TSServerInfo.BOOK_AND_PAGE_MODULE_IDX));
//		m.addExtraInformation(TSServerInfoConstants.EXTRA_PARAM_MODULE_DESCRIPTION, 
//				TSServerInfoConstants.VALUE_PARAM_LIST_AO_BP);
//		m.setSaObjKey(SearchAttributes.INSTR_LIST);
//		m.setIteratorType(ModuleStatesIterator.TYPE_BOOK_PAGE_LIST);
//		m.getFunction(0).setSaKey("");
//		m.getFunction(0).setIteratorType(FunctionStatesIterator.ITERATOR_TYPE_BOOK_FAKE);
//		m.getFunction(1).setSaKey("");
//		m.getFunction(1).setIteratorType(FunctionStatesIterator.ITERATOR_TYPE_PAGE_FAKE);
//		m.addValidator(recordedDateValidator);
//		m.addCrossRefValidator(recordedDateValidator);
//		m.addCrossRefValidator( defaultLegalValidator );
//        m.addCrossRefValidator( addressHighPassValidator );
//        m.addCrossRefValidator( pinValidator );
//        m.addCrossRefValidator( subdivisionNameValidator );
//		l.add(m);
		
		//search by subdivision and lot
		//search by last name = subdivision
		if( searchWithSubdivision() ){
			
			Vector<String> equivalentSubdivisions = StringEquivalents.getInstance().getEquivalents(sa.getAtribute(SearchAttributes.LD_SUBDIV_NAME));
			
			for (String currentSubdivision : equivalentSubdivisions) {
				//subdiv and lot
				m = new TSServerInfoModule(serverInfo.getModule(TSServerInfo.SUBDIVISION_MODULE_IDX));
				m.addExtraInformation(TSServerInfoConstants.EXTRA_PARAM_MODULE_DESCRIPTION, 
						TSServerInfoConstants.VALUE_PARAM_SUBDIVISION_LOT);
				m.getFunction(2).setIteratorType(FunctionStatesIterator.ITERATOR_TYPE_LOT_INTERVAL);
				m.getFunction(0).setSaKey("");
				m.getFunction(0).setData(currentSubdivision);
				m.addValidator( LegalFilterFactory.getDefaultLotFilter(searchId).getValidator() );
				m.addValidator( recordedDateValidator );	
				m.addValidator( lastTransferDateValidator );
				m.addCrossRefValidator( defaultLegalValidator );
		        m.addCrossRefValidator( addressHighPassValidator );
		        m.addCrossRefValidator( pinValidator );
		        m.addCrossRefValidator( subdivisionNameValidator );
				m.addCrossRefValidator( recordedDateValidator );

				l.add(m);
				
				//last name and subdivision plat
				m = new TSServerInfoModule( serverInfo.getModule( TSServerInfo.NAME_MODULE_IDX ) );
				m.addExtraInformation(TSServerInfoConstants.EXTRA_PARAM_MODULE_DESCRIPTION, 
						TSServerInfoConstants.VALUE_PARAM_NAME_SUBDIVISION_PLAT);
				m.clearSaKey(0);
				m.setData(0, currentSubdivision);
				m.addValidator( defaultLegalValidator );
		        m.addValidator( pinValidator );
		        m.addValidator( subdivisionNameValidator );
		        m.addCrossRefValidator( defaultLegalValidator );
		        m.addCrossRefValidator( pinValidator );
		        m.addCrossRefValidator( subdivisionNameValidator );
		        m.clearFunction(9);
		        m.setSaKey(10, SearchAttributes.TODATE_MM_DD_YYYY);
		        
				m.getFunction( 1 ).setSaKey( "" );
				m.getFunction( 4 ).setParamValue( "PLAT" );
				if(validateWithDates) {
					m.addValidator( recordedDateValidator );
					m.addCrossRefValidator( recordedDateValidator );
				}
				l.add(m);
				
				//last name and subdivision easement
				m = new TSServerInfoModule( serverInfo.getModule( TSServerInfo.NAME_MODULE_IDX ) );
				m.addExtraInformation(TSServerInfoConstants.EXTRA_PARAM_MODULE_DESCRIPTION, 
						TSServerInfoConstants.VALUE_PARAM_NAME_SUBDIVISION_EASEMENT);
				m.getFunction( 0 ).setSaKey( "" );
				m.getFunction( 0 ).setData(currentSubdivision);
				m.setSaKey(9, SearchAttributes.FROMDATE_MM_DD_YYYY);
				m.setSaKey(10, SearchAttributes.TODATE_MM_DD_YYYY);		        
		        m.addValidator( defaultLegalValidator );
		        m.addValidator( addressHighPassValidator );
		        m.addValidator( pinValidator );
		        m.addValidator( subdivisionNameValidator );
		        m.addValidator( recordedDateValidator );
		        m.addCrossRefValidator( defaultLegalValidator );
		        m.addCrossRefValidator( addressHighPassValidator );
		        m.addCrossRefValidator( pinValidator );
		        m.addCrossRefValidator( subdivisionNameValidator );
		        m.addCrossRefValidator( recordedDateValidator );
		        
				m.getFunction( 1 ).setSaKey( "" );
				m.getFunction( 4 ).setParamValue( "EASE" );
				l.add(m);
				
				//last name and subdivision restriction
				
				m = new TSServerInfoModule( serverInfo.getModule( TSServerInfo.NAME_MODULE_IDX ) );
				m.addExtraInformation(TSServerInfoConstants.EXTRA_PARAM_MODULE_DESCRIPTION, 
						TSServerInfoConstants.VALUE_PARAM_NAME_SUBDIVISION_RESTRICTION);
				m.getFunction( 0 ).setSaKey( "" );
				m.getFunction( 0 ).setData(currentSubdivision);
				m.setSaKey(9, SearchAttributes.FROMDATE_MM_DD_YYYY);
				m.setSaKey(10, SearchAttributes.TODATE_MM_DD_YYYY);			        
		        m.addValidator( defaultLegalValidator );
		        //m.addValidator( lastTransferDateValidator );
		        m.addValidator( addressHighPassValidator );
		        m.addValidator( pinValidator );
		        m.addValidator( subdivisionNameValidator );
		        m.addValidator( recordedDateValidator );
		        //m.addCrossRefValidatorType(DocsValidator.TYPE_REGISTER_SUBDIV_LOT);
		        m.addCrossRefValidator( defaultLegalValidator );
		        //m.addCrossRefValidator( lastTransferDateValidator );
		        m.addCrossRefValidator( addressHighPassValidator );
		        m.addCrossRefValidator( pinValidator );
		        m.addCrossRefValidator( subdivisionNameValidator );
		        m.addCrossRefValidator( recordedDateValidator );
				
				m.getFunction( 1 ).setSaKey( "" );
				m.getFunction( 4 ).setParamValue( "RC" );
				l.add(m);
				
			}

		} else {
			printSubdivisionException();
		}
		
		//search by owner name
		ConfigurableNameIterator nameIterator = null;
		if( hasOwner() ){
			m = new TSServerInfoModule(serverInfo.getModule(TSServerInfo.NAME_MODULE_IDX));
			DocsValidator recordedDateNameValidator = BetweenDatesFilterResponse.getDefaultIntervalFilter(searchId, m).getValidator();
			m.setSaObjKey(SearchAttributes.OWNER_OBJECT);
			m.addExtraInformation(TSServerInfoConstants.EXTRA_PARAM_MODULE_DESCRIPTION,TSServerInfoConstants.VALUE_PARAM_NAME_OWNERS );
			m.setIteratorType(0, FunctionStatesIterator.ITERATOR_TYPE_LAST_NAME_FAKE);
			m.setIteratorType(1, FunctionStatesIterator.ITERATOR_TYPE_FIRST_NAME_FAKE);
			m.addFilter( NameFilterFactory.getDefaultNameFilter(SearchAttributes.OWNER_OBJECT, searchId, m) );
			m.setSaKey(9, SearchAttributes.FROMDATE_MM_DD_YYYY);
			m.setIteratorType(9, FunctionStatesIterator.ITERATOR_TYPE_FROM_DATE);
			m.setSaKey(10, SearchAttributes.TODATE_MM_DD_YYYY);			
			nameIterator =  (ConfigurableNameIterator)ModuleStatesIteratorFactory.getConfigurableNameIterator(m, searchId, new String[]{"L;f;", "L;m;"});
			m.addIterator( nameIterator);
		
	        m.addValidator( defaultLegalValidator );
			m.addValidator( addressHighPassValidator );
	        m.addValidator( pinValidator );
	        m.addValidator( recordedDateNameValidator );
	        m.addValidator( lastTransferDateValidator );
	        addFilterForUpdate(m, false);
	        m.addCrossRefValidator( defaultLegalValidator );
			m.addCrossRefValidator( addressHighPassValidator );
	        m.addCrossRefValidator( pinValidator );
	        m.addCrossRefValidator( recordedDateNameValidator );
	        l.add(m);
		}
		
		//BUYER SEARCH
		if( hasBuyer() ){
			
			m = new TSServerInfoModule(serverInfo.getModule(TSServerInfo.NAME_MODULE_IDX));
			DocsValidator recordedDateNameValidator = BetweenDatesFilterResponse.getDefaultIntervalFilter(searchId, m).getValidator();
			m.setSaObjKey(SearchAttributes.BUYER_OBJECT);
			m.addExtraInformation(TSServerInfoConstants.EXTRA_PARAM_MODULE_DESCRIPTION,TSServerInfoConstants.VALUE_PARAM_NAME_BUYERS );
		    m.addFilter( NameFilterFactory.getNameFilterIgnoreMiddleOnEmpty(SearchAttributes.BUYER_OBJECT, searchId, m) );
			m.setSaKey(9, SearchAttributes.FROMDATE_MM_DD_YYYY);
			m.setIteratorType(9, FunctionStatesIterator.ITERATOR_TYPE_FROM_DATE);
			m.setSaKey(10, SearchAttributes.TODATE_MM_DD_YYYY);		
			nameIterator =  (ConfigurableNameIterator)ModuleStatesIteratorFactory.getConfigurableNameIterator(m, searchId, new String[]{"L;f;", "L;m;"});
			m.addIterator( nameIterator);
			
	        m.addValidator( defaultLegalValidator );
			m.addValidator( addressHighPassValidator );
	        m.addValidator( pinValidator );
	        m.addValidator( recordedDateNameValidator );
	        m.addValidator( lastTransferDateValidator );
	        addFilterForUpdate(m, false);
	        m.addCrossRefValidator( defaultLegalValidator );
			m.addCrossRefValidator( addressHighPassValidator );
	        m.addCrossRefValidator( pinValidator );
	        m.addCrossRefValidator( recordedDateNameValidator );
	        l.add(m);
		}
		//OCR search
		// search by book and page list from OCR
		m = new TSServerInfoModule(serverInfo.getModule(TSServerInfo.BOOK_AND_PAGE_MODULE_IDX));
		m.addExtraInformation(TSServerInfoConstants.EXTRA_PARAM_MODULE_DESCRIPTION, 
				TSServerInfoConstants.VALUE_PARAM_OCR_SEARCH_BP);
		m.addValidator(defaultLegalValidator);
		m.addValidator(recordedDateValidator);	
		m.setIteratorType(ModuleStatesIterator.TYPE_OCR);
		m.getFunction(0).setSaKey("");
		m.getFunction(0).setIteratorType(FunctionStatesIterator.ITERATOR_TYPE_BOOK_SEARCH);
		m.getFunction(1).setSaKey("");
		m.getFunction(1).setIteratorType(FunctionStatesIterator.ITERATOR_TYPE_PAGE_SEARCH);
		m.addCrossRefValidator(defaultLegalValidator);
		m.addCrossRefValidator(recordedDateValidator);
		m.addCrossRefValidator( pinValidator );

		l.add(m);
		
		// search by instrument number list from OCR
		m = new TSServerInfoModule(serverInfo.getModule(TSServerInfo.INSTR_NO_MODULE_IDX));
		m.addExtraInformation(TSServerInfoConstants.EXTRA_PARAM_MODULE_DESCRIPTION, 
				TSServerInfoConstants.VALUE_PARAM_OCR_SEARCH_INST);
		m.addValidator(defaultLegalValidator);
		m.addValidator(recordedDateValidator);
		m.setIteratorType(ModuleStatesIterator.TYPE_OCR);
		m.getFunction(0).setSaKey("");
		m.getFunction(0).setIteratorType(FunctionStatesIterator.ITERATOR_TYPE_INSTRUMENT_LIST_SEARCH);
		m.addCrossRefValidator(defaultLegalValidator);
		m.addCrossRefValidator(recordedDateValidator);
		m.addCrossRefValidator( pinValidator );
		l.add(m);
		//OCR search end
	
		
		// search by book and page list from Search Page
		m = new TSServerInfoModule(serverInfo.getModule(TSServerInfo.BOOK_AND_PAGE_MODULE_IDX));
		m.addExtraInformation(TSServerInfoConstants.EXTRA_PARAM_MODULE_DESCRIPTION, 
				TSServerInfoConstants.VALUE_PARAM_LIST_SEARCH_PAGE_BP);
		m.setSaObjKey(SearchAttributes.LD_BOOKPAGE);
		m.setIteratorType(ModuleStatesIterator.TYPE_BOOK_PAGE_LIST_SEARCH_NOT_AGAIN);
		m.getFunction(0).setSaKey("");
		m.getFunction(0).setIteratorType(FunctionStatesIterator.ITERATOR_TYPE_BOOK_SEARCH);
		m.getFunction(1).setSaKey("");
		m.getFunction(1).setIteratorType(FunctionStatesIterator.ITERATOR_TYPE_PAGE_SEARCH);
		m.addValidator(recordedDateValidator);
		m.addCrossRefValidator(defaultLegalValidator);
		m.addCrossRefValidator(addressHighPassValidator);
		m.addCrossRefValidator(pinValidator);
		m.addCrossRefValidator(recordedDateValidator);
		l.add(m);
		
		// search by instrument number list from Search Page
		m = new TSServerInfoModule(serverInfo.getModule(TSServerInfo.INSTR_NO_MODULE_IDX));
		m.addExtraInformation(TSServerInfoConstants.EXTRA_PARAM_MODULE_DESCRIPTION, 
				TSServerInfoConstants.VALUE_PARAM_LIST_SEARCH_PAGE_INSTR);

		m.setSaObjKey(SearchAttributes.LD_INSTRNO);
		m.setIteratorType(ModuleStatesIterator.TYPE_INSTRUMENT_LIST_SEARCH_NOT_AGAIN);
		m.getFunction(0).setSaKey("");
		m.getFunction(0).setIteratorType(FunctionStatesIterator.ITERATOR_TYPE_INSTRUMENT_LIST_SEARCH);
		m.addValidator(recordedDateValidator);
		m.addCrossRefValidator(defaultLegalValidator);
		m.addCrossRefValidator(addressHighPassValidator);
		m.addCrossRefValidator(pinValidator);
		m.addCrossRefValidator(recordedDateValidator);
		l.add(m);
		
		// search by book and page
		m = new TSServerInfoModule( serverInfo.getModule( TSServerInfo.BOOK_AND_PAGE_MODULE_IDX ) );
		m.addExtraInformation(TSServerInfoConstants.EXTRA_PARAM_MODULE_DESCRIPTION, 
				TSServerInfoConstants.VALUE_PARAM_PLAT_BOOK_PAGE);
		m.addValidator(recordedDateValidator);
		m.addCrossRefValidator(defaultLegalValidator);
		m.addCrossRefValidator(addressHighPassValidator);
		m.addCrossRefValidator(pinValidator);
		m.addCrossRefValidator(recordedDateValidator);
		l.add( m );
		
		{
			m = new TSServerInfoModule(serverInfo.getModule(TSServerInfo.NAME_MODULE_IDX));
			DocsValidator recordedDateNameValidator = BetweenDatesFilterResponse.getDefaultIntervalFilter(searchId, m).getValidator();
			m.setSaObjKey(SearchAttributes.OWNER_OBJECT);
			m.addExtraInformation(
					TSServerInfoConstants.EXTRA_PARAM_MODULE_DESCRIPTION,
					TSServerInfoConstants.VALUE_PARAM_NAME_OWNERS);
			GenericNameFilter nameFilter = (GenericNameFilter) NameFilterFactory
					.getDefaultNameFilter(SearchAttributes.OWNER_OBJECT,searchId, m);
			nameFilter.setInitAgain(true);
			m.addFilter(nameFilter);

			m.setSaKey(9, SearchAttributes.FROMDATE_MM_DD_YYYY);
			m.setIteratorType(9, FunctionStatesIterator.ITERATOR_TYPE_FROM_DATE);
			m.setSaKey(10, SearchAttributes.TODATE_MM_DD_YYYY);
			
			m.addValidator(defaultLegalValidator);
			m.addValidator(addressHighPassValidator);
			m.addValidator(pinValidator);
			m.addValidator(lastTransferDateValidator);
			m.addValidator(recordedDateNameValidator);
			m.addCrossRefValidator(defaultLegalValidator);
			m.addCrossRefValidator(addressHighPassValidator);
			m.addCrossRefValidator(pinValidator);
			m.addCrossRefValidator(recordedDateNameValidator);
			ArrayList<NameI> searchedNames = null;
			if (nameIterator != null) {
				searchedNames = nameIterator.getSearchedNames();
			} else {
				searchedNames = new ArrayList<NameI>();
			}
			nameIterator = (ConfigurableNameIterator) ModuleStatesIteratorFactory
					.getConfigurableNameIterator(m, searchId, false,
							new String[] { "L;f;", "L;m;" });
			nameIterator.setInitAgain(true);
			nameIterator.setSearchedNames(searchedNames);
			m.addIterator(nameIterator);

			l.add(m);
		}
		
		
		
		serverInfo.setModulesForAutoSearch(l);
	}

	protected void setModulesForGoBackOneLevelSearch(TSServerInfo serverInfo)
	{
	
	  	ConfigurableNameIterator nameIterator = null;
		DocsValidator defaultLegalValidator = LegalFilterFactory.getDefaultLegalFilter(searchId).getValidator();
		DocsValidator pinValidator = PINFilterFactory.getPinFilter(searchId,true,true).getValidator(); 
		DocsValidator addressHighPassValidator = AddressFilterFactory.getAddressHighPassFilter(searchId, 0.8d).getValidator();
		
		SearchAttributes sa = InstanceManager.getManager().getCurrentInstance(searchId).getCrtSearchContext().getSa();	
		List<TSServerInfoModule> modules = new ArrayList<TSServerInfoModule>();
	    TSServerInfoModule module;	
	    GBManager gbm=(GBManager)sa.getObjectAtribute(SearchAttributes.GB_MANAGER_OBJECT);

	    for (String id : gbm.getGbTransfers()) {
			  		   	    	 
	         module = new TSServerInfoModule(serverInfo.getModule(TSServerInfo.NAME_MODULE_IDX));
	         module.setIndexInGB(id);
	         module.setTypeSearchGB("grantor");
	         module.clearSaKeys();
		     module.setSaObjKey(SearchAttributes.GB_MANAGER_OBJECT);
		     String date=gbm.getDateForSearch(id, "MMddyy", searchId);
		     if (date!=null) 
		    	 module.getFunction(9).forceValue(date);
		     module.addFilter(NameFilterFactory.getDefaultNameFilter(SearchAttributes.GB_MANAGER_OBJECT, searchId, module));
		     nameIterator =  (ConfigurableNameIterator)ModuleStatesIteratorFactory.getConfigurableNameIterator(module, searchId, new String[]{"L;f;", "L;m;"} );
		 	 module.addIterator(nameIterator);
		 	 module.addValidator( defaultLegalValidator );
			 module.addValidator( addressHighPassValidator );
	         module.addValidator( pinValidator );
             module.addValidator(NameFilterFactory.getDefaultTransferNameFilter(searchId, 0.90d, module).getValidator());
		  	 module.addValidator(DateFilterFactory.getDateFilterForGoBack(SearchAttributes.GB_MANAGER_OBJECT, searchId, module).getValidator());		
			 modules.add(module);
		    
		     
		     if(gbm.getNamesForBrokenChain(id, searchId).size()>0){
			     module = new TSServerInfoModule(serverInfo.getModule(TSServerInfo.NAME_MODULE_IDX));
			     module.setIndexInGB(id);
			     module.setTypeSearchGB("grantee");
			     module.clearSaKeys();
				 module.setSaObjKey(SearchAttributes.GB_MANAGER_OBJECT);
				 date=gbm.getDateForSearchBrokenChain(id, "MMddyy", searchId);
				 if (date!=null) 
					 module.getFunction(9).forceValue(date);
				 module.addFilter(NameFilterFactory.getDefaultNameFilter(SearchAttributes.GB_MANAGER_OBJECT, searchId, module));
				 nameIterator =  (ConfigurableNameIterator)ModuleStatesIteratorFactory.getConfigurableNameIterator(module, searchId, new String[]{"L;f;", "L;m;"} );
				 module.addIterator(nameIterator);
				 module.addValidator( defaultLegalValidator );
				 module.addValidator( addressHighPassValidator );
				 module.addValidator( pinValidator );
			     module.addValidator(NameFilterFactory.getDefaultTransferNameFilter(searchId, 0.90d, module).getValidator());
				 module.addValidator(DateFilterFactory.getDateFilterForGoBack(SearchAttributes.GB_MANAGER_OBJECT, searchId, module).getValidator());				
				 modules.add(module);
			 
		     }

	    }	 
	    serverInfo.setModulesForGoBackOneLevelSearch(modules);			
	}
	public TNWilliamsonRO(
		String rsRequestSolverName,
		String rsSitePath,
		String rsServerID,
		String rsPrmNameLink, long searchId, int mid)
	{
		super(rsRequestSolverName, rsSitePath, rsServerID, rsPrmNameLink, searchId, mid);
		resultType = MULTIPLE_RESULT_TYPE;
	}
	/**
	 * @see TSInterface#GetLink(java.lang.String)
	 */
	public ServerResponse GetLink(String vsRequest, boolean vbEncoded)
		throws ServerResponseException
	{
		ServerResponse rtrnResponse;
		String sTmp = TSServer.getParameter(msPrmNameLink, vsRequest);
		if (sTmp.indexOf(".php") == -1)
		{
			//try to get the file
			//request file
			getTSConnection().setHostName(IMAGES_SITE);
			getTSConnection().setHostIP(IMAGES_SITE_IP);
			rtrnResponse = super.GetLink(vsRequest, vbEncoded);
			getTSConnection().setHostName(msiServerInfo.getServerAddress());
			getTSConnection().setHostIP(msiServerInfo.getServerIP());
		}
		else
			rtrnResponse = super.GetLink(vsRequest, vbEncoded);
		return rtrnResponse;
	}
	
/* Pretty prints a link that was already followed when creating TSRIndex
 * (non-Javadoc)
 * @see ro.cst.tsearch.servers.types.TSServer#getPrettyFollowedLink(java.lang.String)
 */	
    public String getPrettyFollowedLink (String initialFollowedLnk)
    {
    	if (initialFollowedLnk.matches("(?i).*[^a-z]+instnum[=]([0-9]+)[&=a-z]+.*"))
    	{
/*"Book 13676 Page 1504 which is a Court doc type has already been saved from a
previous search in the log file."*/
    		String retStr = initialFollowedLnk.replaceFirst(
    				"(?i).*[^a-z]+instnum[=]([0-9]+)[&=a-z]+.*", 
    				"Instrument " + "$1" + 
    				" has already been processed from a previous search in the log file.");
    		retStr =  "<br/><span class='followed'>"+retStr+"</span><br/>";
    		
    		return retStr;
    	}
    	
    	return "<br/><span class='followed'>Link already followed</span>:" + initialFollowedLnk + "<br/>";
    }
	
	
	/**
	 * @param rsResponce
	 * @param viParseID
	 */
	protected void ParseResponse(
		String sAction,
		ServerResponse Response,
		int viParseID)
		throws ServerResponseException
	{
		String sTmp1;
		String sTmp;
		String sForm;
		String sFileLink = "";
		//String sTmp2, sTmp3;
		StringBuffer sBfr = new StringBuffer();
		String rsResponce = Response.getResult();
		String initialResponse = rsResponce;
		/*		logger.info("vvvvvvvvvvvvvvvv");
				logger.info(rsResponce);
				logger.info("^^^^^^^^^^^^^^^^");*/
		int iTmp, iTmp1;
		
		msServerID = "p1=094&p2=1";
		
		switch (viParseID)
		{
			case ID_SEARCH_BY_NAME :
			case ID_SEARCH_BY_ADDRESS :
			case ID_SEARCH_BY_PARCEL :
			case ID_SEARCH_BY_INSTRUMENT_NO :
			case ID_SEARCH_BY_SUBDIVISION_NAME :
			case ID_SEARCH_BY_BOOK_AND_PAGE :
			case ID_DETAILS1  : //parse search result
				iTmp = rsResponce.indexOf("% align=center border=");
				if (iTmp == -1)
				{
					return;
				} //no result
				sBfr.append(
					"<table width=100"
						+ rsResponce.substring(
							iTmp,
							rsResponce.lastIndexOf("</table>") + 8).replaceAll(
							"width=90%",
							""));
				sTmp1 = "<FORM ACTION=";
				iTmp = sBfr.indexOf(sTmp1);
				while (iTmp > -1)
				{
					//while we still have a property details form then get it's information
					//get from form until the imput information ends
					sForm = sBfr.substring(iTmp, sBfr.indexOf("</form>", iTmp));
					//create link
					sTmp = createLinkFromForm(sForm, "View");
					sTmp = sTmp.replaceAll("/p4.php", "/ts/p4.php");
					sTmp = sTmp.replaceAll("/pdetail.php", "/ts/pdetail.php");
					sBfr.replace(iTmp, sBfr.indexOf("</td>", iTmp), sTmp);
					//go to next form
					iTmp = sBfr.indexOf(sTmp1, iTmp);
				}
				String linkStart = CreatePartialLink(TSConnectionURL.idGET);
				////////View Image link fix
				rsResponce = sBfr.toString().replaceAll( "</form>", "" );
                
				if (viParseID == ID_DETAILS1 || 
                		viParseID == ID_SEARCH_BY_BOOK_AND_PAGE ||
                		viParseID == ID_SEARCH_BY_ADDRESS ||
                		viParseID == ID_SEARCH_BY_PARCEL ||
                		viParseID == ID_SEARCH_BY_SUBDIVISION_NAME ||
                		viParseID == ID_SEARCH_BY_INSTRUMENT_NO)
                {
                    
                    rsResponce = rsResponce.replaceAll("<td width=2% bgcolor=silver>", 
                            "<td width=2% bgcolor=silver>" + SELECT_ALL_CHECKBOXES + "</td><td width=2% bgcolor=silver>&nbsp;");
                    rsResponce = rsResponce.replaceAll("(<td.*?>)(<A HREF=['\"](.*?)['\"]>View</a>)(</td>)",  
                        "$1<input type=\"checkbox\" name=\"docLink\" value=\"$3\"></td><td width=2%>$2$4");
                    
                    rsResponce = CreateSaveToTSDFormHeader(URLConnectionReader.MULTIPLE_SAVE_TO_TSD_ACTION, "POST") 
                                    + rsResponce 
                                    + CreateSaveToTSDFormEnd(SAVE_SELECTED_DOCUMENTS_BUTTON_LABEL, viParseID, -1);
                }
                
				rsResponce =
					rsResponce.replaceAll(
						"<A HREF=/cgi-bin/imageview\\?" + "([^>]*)>",
						"<A HREF='" + linkStart + "/cgi-bin/imageview&$1'>");
				///////////////////
				linkStart = getLinkPrefix(TSConnectionURL.idPOST);
				int pageId =
					(viParseID == ID_SEARCH_BY_NAME)
						? Parser.PAGE_ROWS_NAME
						: Parser.PAGE_ROWS;
				int action =
					(viParseID == ID_SEARCH_BY_NAME)
						? TSServer.REQUEST_GO_TO_LINK_REC
						: TSServer.REQUEST_SAVE_TO_TSD;
				parser.Parse(
					Response.getParsedResponse(),
					rsResponce,
					pageId,
					linkStart,
					action);
				break;
			case ID_DETAILS2 :
	        	String documentTypeFromResponse =  StringUtils.parseByRegEx(rsResponce, "(?<=Doc Type:)(.*?)</FONT>", 1).trim();
				rsResponce =
					rsResponce.substring(
						rsResponce.indexOf("Detail Information For ") + 23);
				sBfr.append(
					rsResponce.substring(0, rsResponce.indexOf("</a>")));
				//test if we have any document image
				iTmp = rsResponce.indexOf("/cgi-bin");
				if (iTmp > -1)
				{
					//iTmp += IMAGES_SITE.length();
					sTmp1 =
						rsResponce.substring(
							iTmp,
							rsResponce.indexOf(">", iTmp));
					//sFileLink= CreateLink("View Image", sTmp1, TSConnectionURL.idGET);
					sFileLink =
						"<A target=\"_blank\" HREF='"
							+ CreatePartialLink(TSConnectionURL.idGET)
							+ sTmp1.replaceAll("\\?", "&")
							+ "'> View Image </a>";
					sBfr.append("<br>" + sFileLink);
				}
				
				
				rsResponce = 
					rsResponce.substring(
						rsResponce.indexOf("<table width=95%"));
				//rsResponce= rsResponce.substring(rsResponce.indexOf("<table width"));
				if (rsResponce.indexOf("</body>") > -1)
					sBfr.append(
						rsResponce.substring(rsResponce.indexOf("</body>")));
				else
					sBfr.append(rsResponce);
				//test if we have forms
				iTmp = sBfr.indexOf("<FORM ");
				while (iTmp > -1)
				{
					//while we still have a property details form then convert it in link
					//get from form until the imput information ends
					iTmp1 = sBfr.indexOf("</form>", iTmp);
					if (iTmp1 == -1)
						iTmp1 = sBfr.indexOf("</td>", iTmp);
					else
						iTmp1 += 7;
					sForm = sBfr.substring(iTmp, iTmp1);
					sForm = sForm.replaceAll("pdetail.php", "ts/pdetail.php");
					//replace form with a link
					sBfr.replace(
						iTmp,
						iTmp1,
						"<td>" + createLinkFromForm(sForm, "View"));
					iTmp = sBfr.indexOf("<FORM ");
				}
				rsResponce =
					"<hr>"
						+ sBfr.toString().replaceAll(
							"<FONT[a-zA-Z0-9/=,_\\<\\?\\.'\\s\\\"\\r\\n]+>",
							"");
				rsResponce =
					rsResponce.replaceAll(
						"<img[a-zA-Z0-9/=,_\\<\\?\\.'\\s\\\"\\r\\n]+>",
						"");
				rsResponce = rsResponce.replaceAll("align=center", "");
				rsResponce = rsResponce.replaceAll("</FONT>", "");
				rsResponce = rsResponce.replaceAll("bgcolor=BLACK", "");
				rsResponce =
					rsResponce.replaceAll(
						"<TEXTAREA[a-zA-Z0-9/=,_\\<\\?\\.'\\s\\\"\\r\\n]+>",
						"");
				rsResponce = rsResponce.replaceAll("</textarea>", "");
				iTmp =
					rsResponce.indexOf(
						"align=left",
						rsResponce.lastIndexOf("<table"));
				rsResponce =
					rsResponce.substring(0, iTmp)
						+ rsResponce.substring(iTmp + 11);
				
				
				rsResponce = getDataFromCrossRefsDocs(rsResponce);//B 4423
				

				
				//find Instrument No. in html
				String instrNo = getInstrNoFromResponse(rsResponce);
				//find Year in html
				String year = getYearFromResponse(rsResponce);
				String docName = instrNo /*+ "_" + year*/;
				String docFullName = docName + ".html";
				
				// download view file if any;
				if (!(sFileLink.equals("")))
				{
					//logger.debug(" sFileLink = " + sFileLink );
					String imageName =
						StringUtils.getTextBetweenDelimiters(
							"&img=",
							"'>",
							sFileLink);
					String fileExt =
						StringUtils
							.getTextBetweenDelimiters(".", "'>", imageName)
							.toLowerCase();
					String imageFileName = docName + "." + fileExt;
					String link =
						StringUtils.getTextBetweenDelimiters(
							msServerID + "&",
							"'>",
							sFileLink);
					
					// fix for B2261
					String newLink = StringUtils.extractParameter(sFileLink, "HREF='([^']+)'");
					if(!StringUtils.isEmpty(newLink)){
						link = newLink;
					}
					
					Response.getParsedResponse().addImageLink(
						new ImageLinkInPage(link, imageFileName));
				}
				
				if (!downloadingForSave)
				{
					String originalLink = getOriginalLink(instrNo, year);
					String sSave2TSDLink =
						getLinkPrefix(TSConnectionURL.idPOST) + originalLink;
	                HashMap<String, String> data = new HashMap<String, String>();
					data.put("type", documentTypeFromResponse);
	               
	                if (isInstrumentSaved(instrNo, null, data)) {
						rsResponce += "<br><br><br><br><br><br><br><br>"
							+ CreateFileAlreadyInTSD();
					}
					else
					{
						//replace any existing form end
						rsResponce = rsResponce.replaceAll("</FORM>", "");
						rsResponce =
							addSaveToTsdButton(rsResponce, sSave2TSDLink, viParseID);
						mSearch.addInMemoryDoc(sSave2TSDLink, initialResponse);
						// adjusting TSD button
						int i = rsResponce.lastIndexOf(SAVE_DOCUMENT_BUTTON_LABEL);
						i = rsResponce.lastIndexOf("<input", i);
						rsResponce =
							rsResponce.substring(0, i)
								+ "<br><br><br><br><br><br><br><br>"
								+ rsResponce.substring(i);
					}
					Response.getParsedResponse().setPageLink(
						new LinkInPage(
							sSave2TSDLink,
							getOriginalLink(instrNo, year),
							TSServer.REQUEST_SAVE_TO_TSD));
					parser.Parse(
						Response.getParsedResponse(),
						rsResponce,
						Parser.NO_PARSE);
				}
				else
				{ //saving
					msSaveToTSDResponce = rsResponce + CreateFileAlreadyInTSD();
					msSaveToTSDFileName = docFullName;
					Response.getParsedResponse().setFileName(
						getServerTypeDirectory() + msSaveToTSDFileName);
					
						
						//replace link with local link
						//String localLink = StringUtils.replaceFirstBetweenTags(sFileLink, "='","'>", imageFileName);
						//rsResponce= StringUtils.replaceFirstSubstring(rsResponce, sFileLink, localLink);
					
					//save any coss ref link before removing it
					parser.Parse(
						Response.getParsedResponse(),
						rsResponce,
						Parser.PAGE_DETAILS,
						getLinkPrefix(TSConnectionURL.idPOST),
						TSServer.REQUEST_SAVE_TO_TSD);
					
//					remove cross ref links
					rsResponce =
						rsResponce.replaceAll(
							"<td><A HREF=[a-zA-Z0-9/=&,\\-_\\(\\)\\<\\?\\.\\>'\\s\\\"\\r\\n]+</a>",
							"<td>");
					
					Response.getParsedResponse().setOnlyResponse(rsResponce);
					//logger.debug(" is unique" + Response.getParsedResponse().isUnique());
				}
				break;
			case ID_GET_LINK :
				if (sAction.equals("/ts/pdetail.php"))
				{
					msLastdb =
						msLastLink.substring(msLastLink.indexOf("&db=") + 4);
					ParseResponse(sAction, Response, ID_DETAILS2);
				}
				else
					ParseResponse(sAction, Response, ID_DETAILS1);
				break;
			case ID_SAVE_TO_TSD :
				downloadingForSave = true;
				ParseResponse(sAction, Response, ID_DETAILS2);
				downloadingForSave = false;
				break;
			default :
				break;
		}
	}
	
	private String getDataFromCrossRefsDocs(String rsResponce){
		List<String> line = new ArrayList<String>();
		List<List> body = new ArrayList<List>();
		HTTPRequest req = null;
		HTTPResponse res = null;
		String tabel = "<table name=\"crossRefsInfo\" id=\"crossRefsInfo\" style=\"display: none\"><tr><td>Instrument No</td><td>Recorded Date</td><td>Doc Type</td></tr>";
		List<String> crossRefLink = new ArrayList<String>();
		Matcher mat = CROSS_REFS_LINK.matcher(rsResponce);
		while (mat.find()){
			crossRefLink.add("http://www.rod.williamson-tn.org" + mat.group(1).replaceAll("(?is)php&", "php?"));
		}
		if (crossRefLink.size() > 0){
			for (int i = 0; i < crossRefLink.size(); i++){
				line = new ArrayList<String>();
				req = new HTTPRequest(crossRefLink.get(i));
				tabel += "<tr><td>" + StringUtils.extractParameterFromUrl(crossRefLink.get(i), "instnum") + "</td>";
				req.setMethod( HTTPRequest.GET );
				
				HttpSite site = HttpManager.getSite("TNWilliamsonRO", searchId);
				try
				{
					res = site.process(req);
				} finally 
				{
					HttpManager.releaseSite(site);
				}	
				String rsp = res.getResponseAsString();
				if (rsp != null){
					try {
						NodeList rows = new HtmlParser3(rsp).getNodeList().extractAllNodesThatMatch(new TagNameFilter("tr"));
						for (int j = 0; j < rows.size(); j++) {
							NodeList tdList = rows.elementAt(j).getChildren().extractAllNodesThatMatch(new TagNameFilter("td"));
							if (tdList.elementAt(0).toHtml().contains("File Date")){
								String recDate = tdList.elementAt(0).toHtml();
								recDate = recDate.replaceAll("(?is).*?File\\s+Date\\s*:\\s*([^<]*).*", "$1");
								line.add(recDate.trim());
								tabel += "<td>" + recDate.trim() + "</td>";
							} else if (tdList.elementAt(0).toHtml().contains("Doc Type")){
								String docType = tdList.elementAt(0).toHtml();
								docType = docType.replaceAll("(?is).*?Doc\\s+Type\\s*:\\s*([^<]*).*", "$1");
								line.add(docType.trim());
								tabel += "<td>" + docType.trim() + "</td></tr>";
								body.add(line);
								break;
							}
						}
					} catch (Exception e) {
						e.printStackTrace();
					}	
				}
			}
		}
		
		tabel += "</table>";
		rsResponce += tabel;
		return rsResponce;
	}
	
	private String getYearFromResponse(String rsResponce)
	{
		int iTmp;
		iTmp = rsResponce.indexOf("In Year ") + 8;
		//get Instrument No.
		String year = rsResponce.substring(iTmp, iTmp + 4);
		return year;
	}
	private String getInstrNoFromResponse(String rsResponce)
	{
		int iTmp = rsResponce.indexOf("Instrument # ") + 13;
		//get Instrument No.
		String instrNo =
			rsResponce.substring(iTmp, rsResponce.indexOf(" ", iTmp));
		return instrNo;
	}
	/**
	 * @param sTmp
	 * @return
	 */
	private String getOriginalLink(String instrNo, String year)
	{
		return "/ts/pdetail.php&=LIST&instnum="
			+ instrNo
			+ "&year="
			+ year
			+ "&db="
			+ msLastdb;
	}
	protected DownloadImageResult saveImage(ImageLinkInPage image)
		throws ServerResponseException
	{
		getTSConnection().setHostName(IMAGES_SITE);
		getTSConnection().setHostIP(IMAGES_SITE_IP);
		
		// fix for B2261
		String link = image.getLink();
		if(!link.startsWith("searchId=") && link.contains("searchId=")){			
			// work on a clone of the image - fix for B2907
			String imagePath = image.getPath();
			image = new ImageLinkInPage(image);
			link = link.substring(link.indexOf("searchId="));
			image.setLink(link);
			image.setPath(imagePath);
		}
		
		DownloadImageResult res  = super.saveImage(image);
		//undo the modifications
		getTSConnection().setHostName(msiServerInfo.getServerAddress());
		getTSConnection().setHostIP(msiServerInfo.getServerIP());
		return res;
	}
	protected String getFileNameFromLink(String link)
	{
		if ( link.indexOf("instnum") == -1 )
			return System.currentTimeMillis() + ".html";
		else
			return StringUtils.getTextBetweenDelimiters("instnum=", "&", link).trim() + ".html";
	}
	
	 protected String CreateLink(String rsLinkMessage,
	            String rsActionAndExtraParameters, int iActionType) {
	        return "<A HREF=\"" + CreatePartialLink(iActionType)
	                + rsActionAndExtraParameters.replaceAll("\\?", "&") + "\">"
	                + rsLinkMessage + "</a>";
	    }
	 
	public static void splitResultRows(
		Parser p,
		ParsedResponse pr,
		String htmlString,
		int pageId,
		String linkStart,
		int action)
		throws ro.cst.tsearch.exceptions.ServerResponseException
	{
		if (pageId == Parser.ONE_ROW)
			p.splitResultRows(
				pr,
				htmlString,
				pageId,
				"<tr><td width=2% align=left>",
				"</table",
				linkStart,
				action);
		else if (pageId == Parser.ONE_ROW_NAME)
			p.splitResultRows(
				pr,
				htmlString,
				pageId,
				"><A HREF=\"",
				"</table",
				linkStart,
				action);
	}
    
	@Override
	protected void setCertificationDate() {

		try {
        
			if (CertificationDateManager.isCertificationDateInCache(dataSite)){
				String date = CertificationDateManager.getCertificationDateFromCache(dataSite);
				getSearch().getSa().updateCertificationDateObject(dataSite, Util.dateParser3(date));
			} else{
		        //TSConnectionURL t = new TSConnectionURL();
		        //TSServerInfo info = TSServersFactory.GetServerInfo((int) TSServersFactory.getSiteId("TNWilsonRO"),searchId);
		        //t.setHostName(info.getServerAddress());
		        //t.setHostIP(info.getServerIP());
	        	String html = HttpUtils.downloadPage("http://www.rod.williamson-tn.org/ts/menu.php?cnum=24");
	            Matcher certDateMatcher = certDatePattern.matcher(html);
	            if(certDateMatcher.find()) {
	            	String date = certDateMatcher.group(1).trim();
	            	
	            	CertificationDateManager.cacheCertificationDate(dataSite, date);
	            	getSearch().getSa().updateCertificationDateObject(dataSite, Util.dateParser3(date));
	            }
			}
        } catch (Exception e) {
            logger.error(e.getMessage());
        }
	}
	
	@Override
	public TSServerInfoModule getRecoverModuleFrom(RestoreDocumentDataI restoreDocumentDataI) {
		if(restoreDocumentDataI == null) {
			return null;
		}
		String book = restoreDocumentDataI.getBook();
		String page = restoreDocumentDataI.getPage();
		TSServerInfoModule module = null;
		
		if(StringUtils.isNotEmpty(restoreDocumentDataI.getInstrumentNumber())) {
			module = getDefaultServerInfo().getModule(TSServerInfo.INSTR_NO_MODULE_IDX);
			module.forceValue(0, restoreDocumentDataI.getInstrumentNumber());
			module.forceValue(1, "INUM");
			module.forceValue(2, "execute search");
			module.forceValue(3, "24");
		} else if(StringUtils.isNotEmpty(book) && StringUtils.isNotEmpty(page)) {
			module = getDefaultServerInfo().getModule(TSServerInfo.BOOK_AND_PAGE_MODULE_IDX);
			module.forceValue(0, book);
			module.forceValue(1, page);
			module.forceValue(3, "BP");
			module.forceValue(4, "execute search");
			module.forceValue(5, "24");
		} else if(StringUtils.isNotEmpty(restoreDocumentDataI.getDocumentNumber())) {
			module = getDefaultServerInfo().getModule(TSServerInfo.INSTR_NO_MODULE_IDX);
			module.forceValue(0, restoreDocumentDataI.getDocumentNumber());
			module.forceValue(1, "INUM");
			module.forceValue(2, "execute search");
			module.forceValue(3, "24");
		} else {
			module = null;
		}
		return module;
	}
	
	public Object getImageDownloader(RestoreDocumentDataI document) {
		return getRecoverModuleFrom(document);
	}
}
