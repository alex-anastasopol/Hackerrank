package ro.cst.tsearch.servers.types;

import java.net.Authenticator;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Vector;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang.time.DateFormatUtils;
import org.apache.log4j.Category;
import org.apache.log4j.Logger;

import ro.cst.tsearch.MailConfig;
import ro.cst.tsearch.Search;
import ro.cst.tsearch.bean.SearchAttributes;
import ro.cst.tsearch.emailClient.EmailClient;
import ro.cst.tsearch.exceptions.ServerResponseException;
import ro.cst.tsearch.generic.Util;
import ro.cst.tsearch.search.FunctionStatesIterator;
import ro.cst.tsearch.search.ModuleStatesIterator;
import ro.cst.tsearch.search.filter.BetweenDatesFilterResponse;
import ro.cst.tsearch.search.filter.FilterResponse;
import ro.cst.tsearch.search.filter.RejectAlreadySavedDocumentsFilterResponse;
import ro.cst.tsearch.search.filter.newfilters.address.AddressFilterFactory;
import ro.cst.tsearch.search.filter.newfilters.date.DateFilterFactory;
import ro.cst.tsearch.search.filter.newfilters.date.LastTransferDateFilter;
import ro.cst.tsearch.search.filter.newfilters.doctype.DocTypeSimpleFilter;
import ro.cst.tsearch.search.filter.newfilters.doctype.DoctypeFilterFactory;
import ro.cst.tsearch.search.filter.newfilters.legal.LegalFilterFactory;
import ro.cst.tsearch.search.filter.newfilters.name.GenericNameFilter;
import ro.cst.tsearch.search.filter.newfilters.name.NameFilterFactory;
import ro.cst.tsearch.search.filter.newfilters.pin.PINFilterFactory;
import ro.cst.tsearch.search.iterator.ModuleStatesIteratorFactory;
import ro.cst.tsearch.search.iterator.instrument.InstrumentGenericIterator;
import ro.cst.tsearch.search.module.ConfigurableNameIterator;
import ro.cst.tsearch.search.validator.DocsValidator;
import ro.cst.tsearch.servers.SearchDataWrapper;
import ro.cst.tsearch.servers.TSConnectionURL;
import ro.cst.tsearch.servers.info.TSServerInfo;
import ro.cst.tsearch.servers.info.TSServerInfoConstants;
import ro.cst.tsearch.servers.info.TSServerInfoModule;
import ro.cst.tsearch.servers.response.DavidsonSplit;
import ro.cst.tsearch.servers.response.ImageLinkInPage;
import ro.cst.tsearch.servers.response.LinkInPage;
import ro.cst.tsearch.servers.response.ParsedResponse;
import ro.cst.tsearch.servers.response.Parser;
import ro.cst.tsearch.servers.response.ServerResponse;
import ro.cst.tsearch.servlet.URLConnectionReader;
import ro.cst.tsearch.titledocument.abstracts.DocumentTypes;
import ro.cst.tsearch.utils.CSTCalendar;
import ro.cst.tsearch.utils.GBManager;
import ro.cst.tsearch.utils.HttpUtils;
import ro.cst.tsearch.utils.InstanceManager;
import ro.cst.tsearch.utils.StringUtils;
import ro.cst.tsearch.utils.WebAuth;

import com.stewart.ats.base.document.DocumentI;
import com.stewart.ats.base.document.Instrument;
import com.stewart.ats.base.document.InstrumentI;
import com.stewart.ats.base.document.RestoreDocumentDataI;
import com.stewart.ats.base.name.NameI;
import com.stewart.ats.base.search.DocumentsManagerI;


public class TNKnoxRO extends TSServer implements TSServerROLikeI{

    static final long serialVersionUID = 10000000;

    private final static String SEARCH_PATH = "/recordings/SimpleQuery.asp";

    //path to search solver script
    public static final String FILE_REFERER = "http://www.knoxrod.org/recordings/SimpleQuery.asp";

	private static final Pattern bpPattern = Pattern.compile("(?is)\\s*(<i>(?:Bkwd|Fwd)</i>\\s+[0-9A-Z\\s]+\\s+)(([0-9]+[A-Z]*)[^0-9]+([0-9]+[A-Z]*))\\s*");
	private static final Pattern iPattern = Pattern.compile("(?is)\\s*(<i>(?:Bkwd|Fwd)</i>\\s*[A-Z]*\\s+)([0-9A-Z]{6,})\\s*");
	private static final Pattern hrefPattern = Pattern.compile("(?is)\\s*<\\s*a\\s+href\\s*=\\s*");
    
    public static final String DOC_TYPE_PLAT = "MAP";
    
    private static final Category logger = Logger.getLogger(TNKnoxRO.class);


    private static final Pattern certDatePattern = Pattern.compile("(?ism)The Data is Current Thru:</SPAN>(.*?)</font>");
    
    private String getOriginalLink(String InstrumentNo) {
        return "/recordings/SimpleQuery.asp&Instrs=" + InstrumentNo;
    }

    @SuppressWarnings("unchecked")
	protected void setModulesForAutoSearch(TSServerInfo serverInfo) {
		List<TSServerInfoModule> l = new ArrayList<TSServerInfoModule>();
		SearchAttributes sa = InstanceManager.getManager().getCurrentInstance(searchId).getCrtSearchContext().getSa();
		TSServerInfoModule m;
		int i;
		Search global = InstanceManager.getManager().getCurrentInstance(searchId).getCrtSearchContext();

		FilterResponse addressHighPassFilterResponse = AddressFilterFactory.getAddressHighPassFilter(searchId, 0.8d);
		FilterResponse alreadySaved = new RejectAlreadySavedDocumentsFilterResponse(searchId);
		DocsValidator defaultLegalValidator = LegalFilterFactory.getDefaultLegalFilter(searchId).getValidator();
		DocsValidator pinValidator = PINFilterFactory.getPinFilter(searchId, true, true).getValidator();
		DocsValidator addressHighPassValidator = addressHighPassFilterResponse.getValidator();
		DocsValidator lastTransferDateValidator = (new LastTransferDateFilter(searchId)).getValidator();
		DocsValidator recordedDateValidator = BetweenDatesFilterResponse.getDefaultIntervalFilter(searchId).getValidator();
		DocsValidator doctypeValidator = DoctypeFilterFactory.getDoctypeSubdivisionIsGranteeFilter(
				searchId).getValidator();
		DocsValidator subdivisionNameValidator = NameFilterFactory.getDefaultNameFilterForSubdivision(
				searchId).getValidator();
		DocsValidator lotValidator = LegalFilterFactory.getDefaultLotFilter(searchId).getValidator();

		boolean validateWithDates = sa.isUpdate() || sa.isDateDown();
		boolean searchWithSubdivision = searchWithSubdivision();

		// search by instrument number, book and page using iterators
		InstrumentGenericIterator instrumentIterator = getInstrumentIterator(true);
		m = new TSServerInfoModule(serverInfo.getModule(TSServerInfo.INSTR_NO_MODULE_IDX));
		m.addExtraInformation(TSServerInfoConstants.EXTRA_PARAM_MODULE_DESCRIPTION,
				TSServerInfoConstants.VALUE_PARAM_LIST_NDB_INSTR);
		m.clearSaKeys();

		m.setIteratorType(0, FunctionStatesIterator.ITERATOR_TYPE_INSTRUMENT_LIST_FAKE);
		
		m.addValidator( lotValidator );
		m.addValidator(recordedDateValidator);
		m.addCrossRefValidator(defaultLegalValidator);
		m.addCrossRefValidator(PINFilterFactory.getPinFilter(searchId, true, true).getValidator());
		m.addCrossRefValidator(recordedDateValidator);
		m.addCrossRefValidator(subdivisionNameValidator);	
		m.addIterator(instrumentIterator);
		l.add(m);

		instrumentIterator = getInstrumentIterator(false);
		m = new TSServerInfoModule(serverInfo.getModule(TSServerInfo.INSTR_NO_MODULE_IDX));
		m.addExtraInformation(TSServerInfoConstants.EXTRA_PARAM_MODULE_DESCRIPTION,
				TSServerInfoConstants.VALUE_PARAM_LIST_NDB_INSTR);
		m.clearSaKeys();
		m.setIteratorType(1, FunctionStatesIterator.ITERATOR_TYPE_BOOK_FAKE);
		m.setIteratorType(2, FunctionStatesIterator.ITERATOR_TYPE_PAGE_FAKE);
		m.addValidator( lotValidator );
		m.addValidator(recordedDateValidator);
		m.addCrossRefValidator(defaultLegalValidator);
		m.addCrossRefValidator(PINFilterFactory.getPinFilter(searchId, true, true).getValidator());
		m.addCrossRefValidator(recordedDateValidator);
		m.addCrossRefValidator(subdivisionNameValidator);
		m.addIterator(instrumentIterator);
		l.add(m);
		
		if (hasPin()) {
			m = new TSServerInfoModule(serverInfo.getModule(TSServerInfo.PARCEL_ID_MODULE_IDX));
			m.setVisible(true);
			m.addExtraInformation(TSServerInfoConstants.EXTRA_PARAM_MODULE_DESCRIPTION,
					TSServerInfoConstants.VALUE_PARAM_PARCEL_ID);
			l.add(m);

		}

		// subdiv = grantor
		if (searchWithSubdivision) {
			m = new TSServerInfoModule(serverInfo
					.getModule(TSServerInfo.NAME_MODULE_IDX));
			m.setVisible(true);
			m.setIteratorType(ModuleStatesIterator.TYPE_DEFAULT);
			m.setSaObjKey(SearchAttributes.NO_KEY);
			m.getFunction(0).setSaKey(SearchAttributes.LD_SUBDIV_NAME);
			m.getFunction(0).setIteratorType(
					FunctionStatesIterator.ITERATOR_TYPE_LAST_WORD_INITIAL);
			m.getFunction(1).setSaKey("");
			m.getFunction(2).setSaKey("");

			i = m.addFunction();
			m.getFunction(i).setParamName("DocTypes");
			m.getFunction(i).setDefaultValue("MAP"); // Plat
			m.getFunction(i).setHiden(true);

			m.addValidator(doctypeValidator);
			m.addValidator(defaultLegalValidator);
			// m.addValidator( lastTransferDateValidator );
			m.addValidator(pinValidator);
			m.addValidator(subdivisionNameValidator);
			if (validateWithDates) {
				m.addValidator(recordedDateValidator);
			}

			m.addCrossRefValidator(defaultLegalValidator);
			m.addCrossRefValidator(pinValidator);
			m.addCrossRefValidator(recordedDateValidator);
			m.addCrossRefValidator(subdivisionNameValidator);

			l.add(m);

			m = new TSServerInfoModule(serverInfo
					.getModule(TSServerInfo.NAME_MODULE_IDX));
			m.setVisible(true);
			m.setIteratorType(ModuleStatesIterator.TYPE_DEFAULT);
			m.setSaObjKey(SearchAttributes.NO_KEY);
			m.getFunction(0).setSaKey(SearchAttributes.LD_SUBDIV_NAME);
			m.getFunction(0).setIteratorType(
					FunctionStatesIterator.ITERATOR_TYPE_LAST_WORD_INITIAL);
			m.getFunction(1).setSaKey("");
			m.getFunction(2).setSaKey("");

			i = m.addFunction();
			m.getFunction(i).setParamName("DocTypes");
			m.getFunction(i).setDefaultValue("ESMT"); // Easement
			m.getFunction(i).setHiden(true);
			i = m.addFunction();
			m.getFunction(i).setParamName("DocTypes");
			m.getFunction(i).setDefaultValue("REST"); // Restriction
			m.getFunction(i).setHiden(true);

			m.addValidator(doctypeValidator);
			m.addValidator(defaultLegalValidator);
			// m.addValidator( lastTransferDateValidator );
			m.addValidator(addressHighPassValidator);
			m.addValidator(pinValidator);
			m.addValidator(subdivisionNameValidator);
			if (validateWithDates) {
				m.addValidator(recordedDateValidator);
			}

			m.addCrossRefValidator(defaultLegalValidator);
			m.addCrossRefValidator(addressHighPassValidator);
			m.addCrossRefValidator(pinValidator);
			m.addCrossRefValidator(recordedDateValidator);
			m.addCrossRefValidator(subdivisionNameValidator);

			l.add(m);

		}

		// search by subdivision name
		if (searchWithSubdivision) {
			m = new TSServerInfoModule(serverInfo
					.getModule(TSServerInfo.SUBDIVISION_MODULE_IDX));
			m.setVisible(true);
			m.addValidator(defaultLegalValidator);
			m.addValidator(lastTransferDateValidator);
			m.addValidator(addressHighPassValidator);
			m.addValidator(pinValidator);

			m.setIteratorType(ModuleStatesIterator.TYPE_DEFAULT);
			m.setGoOnNextLink(false);
			m.addCrossRefValidator(defaultLegalValidator);
			m.addCrossRefValidator(addressHighPassValidator);
			m.addCrossRefValidator(pinValidator);
			m.addCrossRefValidator(recordedDateValidator);
			if (validateWithDates) {
				m.addValidator(recordedDateValidator);
				// m.addCrossRefValidator(recordedDateValidator);
			}
			l.add(m);
		}

		ConfigurableNameIterator nameIteratorOwner = null;
		// search by owner
		{
			m = new TSServerInfoModule(serverInfo.getModule(TSServerInfo.ADV_SEARCH_MODULE_IDX));
			DocsValidator recordedDateNameValidator = BetweenDatesFilterResponse.getDefaultIntervalFilter(searchId, m).getValidator();

			m.addExtraInformation(TSServerInfoConstants.EXTRA_PARAM_MODULE_DESCRIPTION,
					TSServerInfoConstants.VALUE_PARAM_NAME_OWNERS);
			m.clearSaKeys();
			m.setSaObjKey(SearchAttributes.OWNER_OBJECT);
			m.setSaKey(14, SearchAttributes.FROMDATE);
			m.setSaKey(15, SearchAttributes.TODATE);

			m.setIteratorType(0, FunctionStatesIterator.ITERATOR_TYPE_LAST_NAME_FAKE);
			m.setIteratorType(1, FunctionStatesIterator.ITERATOR_TYPE_FIRST_NAME_FAKE);
			m.setIteratorType(14, FunctionStatesIterator.ITERATOR_TYPE_FROM_DATE);

			m.addFilter(NameFilterFactory.getDefaultNameFilter(SearchAttributes.OWNER_OBJECT, searchId, m));
			m.addValidator(defaultLegalValidator);
			m.addValidator(addressHighPassValidator);
			m.addValidator(pinValidator);
			m.addValidator(recordedDateNameValidator);
			m.addValidator(lastTransferDateValidator);
			addFilterForUpdate(m, false);
			m.addCrossRefValidator(defaultLegalValidator);
			m.addCrossRefValidator(addressHighPassValidator);
			m.addCrossRefValidator(pinValidator);
			m.addCrossRefValidator(recordedDateNameValidator);
			nameIteratorOwner = (ConfigurableNameIterator) ModuleStatesIteratorFactory.getConfigurableNameIterator(m, searchId, new String[] { "L ; f;" });
			m.addIterator(nameIteratorOwner);
			l.add(m);
		}
		// search by buyer
		ConfigurableNameIterator nameIteratorBuyer = null;
		if (hasBuyer()) {

			
			DocTypeSimpleFilter doctypeFilter = (DocTypeSimpleFilter) DoctypeFilterFactory.getDoctypeBuyerFilter(searchId);
			String doctypes[] = { 
					DocumentTypes.MISCELLANEOUS, 
					DocumentTypes.LIEN, 
					DocumentTypes.COURT, 
					DocumentTypes.MORTGAGE, 
					DocumentTypes.AFFIDAVIT,
					DocumentTypes.RELEASE };
			doctypeFilter.setDocTypes(doctypes);
			
			m = new TSServerInfoModule(serverInfo.getModule(TSServerInfo.ADV_SEARCH_MODULE_IDX));
			DocsValidator recordedDateNameValidator = BetweenDatesFilterResponse.getDefaultIntervalFilter(searchId, m).getValidator();
			m.addExtraInformation(TSServerInfoConstants.EXTRA_PARAM_MODULE_DESCRIPTION,
					TSServerInfoConstants.VALUE_PARAM_NAME_BUYERS);

			m.setSaObjKey(SearchAttributes.BUYER_OBJECT);
			m.clearSaKeys();
			m.setSaKey(14, SearchAttributes.FROMDATE);
			m.setSaKey(15, SearchAttributes.TODATE);

			m.setIteratorType(0, FunctionStatesIterator.ITERATOR_TYPE_LAST_NAME_FAKE);
			m.setIteratorType(1, FunctionStatesIterator.ITERATOR_TYPE_FIRST_NAME_FAKE);
			m.setIteratorType(14, FunctionStatesIterator.ITERATOR_TYPE_FROM_DATE);
			m.addFilter(NameFilterFactory.getNameFilterIgnoreMiddleOnEmpty(SearchAttributes.BUYER_OBJECT, searchId, m));
			nameIteratorBuyer = (ConfigurableNameIterator) ModuleStatesIteratorFactory.getConfigurableNameIterator(m, searchId, new String[] { "L ; f;" });
			m.addIterator(nameIteratorBuyer);
			m.addValidator(defaultLegalValidator);
			m.addValidator(addressHighPassValidator);
			m.addValidator(pinValidator);
			m.addValidator(recordedDateNameValidator);
			m.addValidator(lastTransferDateValidator);
			m.addValidator(doctypeFilter.getValidator());
			addFilterForUpdate(m, false);
			m.addCrossRefValidator(defaultLegalValidator);
			m.addCrossRefValidator(addressHighPassValidator);
			m.addCrossRefValidator(pinValidator);
			m.addCrossRefValidator(recordedDateNameValidator);

			l.add(m);
		}

		// search by crossRef book and page list from RO docs

		m = new TSServerInfoModule(serverInfo.getModule(TSServerInfo.INSTR_NO_MODULE_IDX));
		m.addExtraInformation(TSServerInfoConstants.EXTRA_PARAM_MODULE_DESCRIPTION,
				TSServerInfoConstants.VALUE_PARAM_LIST_RO_CROSSREF_BP);
		m.addExtraInformation(TSServerInfoConstants.EXTRA_PARAM_SIMULATE_CROSSREF, Boolean.TRUE);
		m.clearSaKeys();
		m.setSaObjKey(SearchAttributes.RO_CROSS_REF_INSTR_LIST);
		m.setIteratorType(ModuleStatesIterator.TYPE_BOOK_PAGE_LIST_NOT_AGAIN);
		m.getFunction(1).setIteratorType(FunctionStatesIterator.ITERATOR_TYPE_BOOK_FAKE);
		m.getFunction(2).setIteratorType(FunctionStatesIterator.ITERATOR_TYPE_PAGE_FAKE);
		m.addValidator(defaultLegalValidator);
		m.addValidator(addressHighPassValidator);
		m.addValidator(pinValidator);
		m.addValidator(recordedDateValidator);
		m.addCrossRefValidator(defaultLegalValidator);
		m.addCrossRefValidator(addressHighPassValidator);
		m.addCrossRefValidator(pinValidator);
		m.addCrossRefValidator(recordedDateValidator);

		l.add(m);

		// search by crossRef instr# list from RO docs
		m = new TSServerInfoModule(serverInfo.getModule(TSServerInfo.INSTR_NO_MODULE_IDX));
		m.addExtraInformation(TSServerInfoConstants.EXTRA_PARAM_MODULE_DESCRIPTION,
				TSServerInfoConstants.VALUE_PARAM_LIST_RO_CROSSREF_INSTR);
		m.addExtraInformation(TSServerInfoConstants.EXTRA_PARAM_SIMULATE_CROSSREF, Boolean.TRUE);
		m.setSaObjKey(SearchAttributes.RO_CROSS_REF_INSTR_LIST);
		m.setIteratorType(ModuleStatesIterator.TYPE_INSTRUMENT_LIST_NOT_AGAIN);
		m.getFunction(0).setSaKey("");
		m.getFunction(0).setIteratorType(FunctionStatesIterator.ITERATOR_TYPE_INSTRUMENT_LIST_FAKE);
		m.getFunction(1).setSaKey("");
		m.getFunction(2).setSaKey("");
		m.addValidator(defaultLegalValidator);
		m.addValidator(addressHighPassValidator);
		m.addValidator(pinValidator);
		m.addValidator(recordedDateValidator);
		m.addCrossRefValidator(defaultLegalValidator);
		m.addCrossRefValidator(addressHighPassValidator);
		m.addCrossRefValidator(pinValidator);
		m.addCrossRefValidator(recordedDateValidator);
		l.add(m);

		// OCR module
		m = new TSServerInfoModule(serverInfo.getModule(TSServerInfo.INSTR_NO_MODULE_IDX));
		m.addFilter(alreadySaved);
		m.addExtraInformation(TSServerInfoConstants.EXTRA_PARAM_MODULE_DESCRIPTION,
				TSServerInfoConstants.VALUE_PARAM_OCR_SEARCH_BP_INST);

		m.setIteratorType(ModuleStatesIterator.TYPE_OCR);
		m.getFunction(0).setSaKey("");
		m.getFunction(0).setIteratorType(FunctionStatesIterator.ITERATOR_TYPE_INSTRUMENT_LIST_SEARCH);
		m.getFunction(1).setSaKey("");
		m.getFunction(1).setIteratorType(FunctionStatesIterator.ITERATOR_TYPE_BOOK_SEARCH);
		m.getFunction(2).setSaKey("");
		m.getFunction(2).setIteratorType(FunctionStatesIterator.ITERATOR_TYPE_PAGE_SEARCH);
		m.addValidator(recordedDateValidator);
		m.addCrossRefValidator(defaultLegalValidator);
		m.addCrossRefValidator(addressHighPassValidator);
		m.addCrossRefValidator(pinValidator);
		m.addCrossRefValidator(recordedDateValidator);
		l.add(m);

		{
			m = new TSServerInfoModule(serverInfo.getModule(TSServerInfo.ADV_SEARCH_MODULE_IDX));
			DocsValidator recordedDateNameValidator = BetweenDatesFilterResponse.getDefaultIntervalFilter(searchId, m).getValidator();

			m.addExtraInformation(TSServerInfoConstants.EXTRA_PARAM_MODULE_DESCRIPTION,
					TSServerInfoConstants.VALUE_PARAM_NAME_OWNERS);
			m.clearSaKeys();
			m.setSaObjKey(SearchAttributes.OWNER_OBJECT);
			m.setSaKey(14, SearchAttributes.FROMDATE);
			m.setSaKey(15, SearchAttributes.TODATE);

			m.setIteratorType(0, FunctionStatesIterator.ITERATOR_TYPE_LAST_NAME_FAKE);
			m.setIteratorType(1, FunctionStatesIterator.ITERATOR_TYPE_FIRST_NAME_FAKE);
			m.setIteratorType(14, FunctionStatesIterator.ITERATOR_TYPE_FROM_DATE);
			GenericNameFilter nameFilter = (GenericNameFilter) NameFilterFactory.getDefaultNameFilter(SearchAttributes.OWNER_OBJECT, searchId, m);
			nameFilter.setInitAgain(true);
			m.addFilter(nameFilter);
			m.addValidator(defaultLegalValidator);
			m.addValidator(addressHighPassValidator);
			m.addValidator(pinValidator);
			m.addValidator(recordedDateNameValidator);
			m.addValidator(lastTransferDateValidator);
			m.addCrossRefValidator(defaultLegalValidator);
			m.addCrossRefValidator(addressHighPassValidator);
			m.addCrossRefValidator(pinValidator);
			m.addCrossRefValidator(recordedDateNameValidator);
			ArrayList<NameI> searchedNames = null;
			if (nameIteratorOwner != null) {
				searchedNames = nameIteratorOwner.getSearchedNames();
			}
			else
			{
				searchedNames = new ArrayList<NameI>();
			}

			nameIteratorOwner = (ConfigurableNameIterator) ModuleStatesIteratorFactory.getConfigurableNameIterator(m, searchId, false, new String[] { "L;f;" });
			nameIteratorOwner.setInitAgain(true);
			nameIteratorOwner.setSearchedNames(searchedNames);
			m.addIterator(nameIteratorOwner);
			l.add(m);
		}

		serverInfo.setModulesForAutoSearch(l);
	}

	protected void setModulesForGoBackOneLevelSearch(
            TSServerInfo serverInfo) {
 

    	ConfigurableNameIterator nameIterator = null;
		DocsValidator defaultLegalValidator = LegalFilterFactory.getDefaultLegalFilter(searchId).getValidator();
		DocsValidator pinValidator = PINFilterFactory.getPinFilter(searchId,true,true).getValidator(); 
		DocsValidator addressHighPassValidator = AddressFilterFactory.getAddressHighPassFilter(searchId, 0.8d).getValidator();
		
		SearchAttributes sa = InstanceManager.getManager().getCurrentInstance(searchId).getCrtSearchContext().getSa();	
		List<TSServerInfoModule> modules = new ArrayList<TSServerInfoModule>();
	    TSServerInfoModule module;	
	    GBManager gbm=(GBManager)sa.getObjectAtribute(SearchAttributes.GB_MANAGER_OBJECT);

	    for (String id : gbm.getGbTransfers()) {
			  		   	    	 
	         module = new TSServerInfoModule(serverInfo.getModule(TSServerInfo.ADV_SEARCH_MODULE_IDX));
	         module.setIndexInGB(id);
	         module.setTypeSearchGB("grantor");
	         module.clearSaKeys();
		     module.setSaObjKey(SearchAttributes.GB_MANAGER_OBJECT);
		     module.addFilter(NameFilterFactory.getDefaultNameFilter(SearchAttributes.GB_MANAGER_OBJECT, searchId, module));
		     String date=gbm.getDateForSearch(id, "MMM dd, yyyy", searchId);
		     if (date!=null){ 
		    //	 module.getFunction(3).forceValue(date);
		         module.getFunction(14).forceValue(date);
		     }
		     nameIterator =  (ConfigurableNameIterator)ModuleStatesIteratorFactory.getConfigurableNameIterator(module, searchId, new String[]{"L;F;"} );
		 	 module.addIterator(nameIterator);
		 	 module.addValidator( defaultLegalValidator );
			 module.addValidator( addressHighPassValidator );
	         module.addValidator( pinValidator );
             module.addValidator(NameFilterFactory.getDefaultTransferNameFilter(searchId, 0.90d, module).getValidator());
		  	 module.addValidator(DateFilterFactory.getDateFilterForGoBack(SearchAttributes.GB_MANAGER_OBJECT, searchId, module).getValidator());
		
			 modules.add(module);
		    
		     
		     if(gbm.getNamesForBrokenChain(id, searchId).size()>0){
			     module = new TSServerInfoModule(serverInfo.getModule(TSServerInfo.ADV_SEARCH_MODULE_IDX));
			     module.setIndexInGB(id);
			     module.setTypeSearchGB("grantee");
			     module.clearSaKeys();
				 module.setSaObjKey(SearchAttributes.GB_MANAGER_OBJECT);
				 module.addFilter(NameFilterFactory.getDefaultNameFilter(SearchAttributes.GB_MANAGER_OBJECT, searchId, module));
				 date=gbm.getDateForSearchBrokenChain(id, "MMM dd, yyyy", searchId);
				 if (date!=null){ 
			   // 	 module.getFunction(3).forceValue(date);
			         module.getFunction(14).forceValue(date);
			     }
				 nameIterator =  (ConfigurableNameIterator)ModuleStatesIteratorFactory.getConfigurableNameIterator(module, searchId, new String[]{"L;F;"} );
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

    public TNKnoxRO(String rsRequestSolverName, String rsSitePath,
            String rsServerID, String rsPrmNameLink, long searchId, int mid) {
        super(rsRequestSolverName, rsSitePath, rsServerID, rsPrmNameLink, searchId, mid);
        /*
         * loggerCookie.debug("before enter synchronized region " + cookie);
         * synchronized(cookie){ loggerCookie.debug("after enter synchronized
         * region " + cookie); mTSConnection.setCookie(cookie);
         * loggerCookie.debug("before exit synchronized region" + cookie ); }
         * loggerCookie.debug("after exit synchronized region" + cookie );
         */
        resultType = MULTIPLE_RESULT_TYPE;
    }

    public ServerResponse SearchBy(TSServerInfoModule module,
            Object sd) throws ServerResponseException {
        boolean bResetQueryParam = true;

        Authenticator.setDefault(WebAuth.getInstance(searchId));

        if(sd instanceof SearchDataWrapper){
        	msiServerInfo.getModule(TSServerInfo.ADV_SEARCH_MODULE_IDX).setData((SearchDataWrapper)sd);
        }
        String sStartDate = null;
        String sEndDate = null;
        if(module.getModuleIdx() == TSServerInfo.ADV_SEARCH_MODULE_IDX) {
        	sStartDate = module.getFunction(14).getParamValue();
	        sEndDate = module.getFunction(15).getParamValue();
        } else if(module.getModuleIdx() == TSServerInfo.INSTR_NO_MODULE_IDX) {
        	sStartDate = module.getFunction(5).getParamValue();
	        sEndDate = module.getFunction(6).getParamValue();
        }else {
	        sStartDate = msiServerInfo.getModule(TSServerInfo.ADV_SEARCH_MODULE_IDX).getFunction(14).getParamValue();
	        sEndDate = msiServerInfo.getModule(TSServerInfo.ADV_SEARCH_MODULE_IDX).getFunction(15).getParamValue();
        }
        if ((sStartDate != null) && (sStartDate.length() != 0)) {
            getTSConnection().BuildQuery("StartMonth", CSTCalendar.getMonthNameFromInput(sStartDate), bResetQueryParam);
            getTSConnection().BuildQuery("StartDay", CSTCalendar.getDayFromInput(sStartDate), false);
            getTSConnection().BuildQuery("StartYear", CSTCalendar.getYearFromInput(sStartDate), false);
            bResetQueryParam = false;
        }
        if ((sEndDate != null) && (sEndDate.length() != 0)) {
            getTSConnection().BuildQuery("EndMonth", CSTCalendar.getMonthNameFromInput(sEndDate), bResetQueryParam);
            getTSConnection().BuildQuery("EndDay", CSTCalendar.getDayFromInput(sEndDate), false);
            getTSConnection().BuildQuery("EndYear", CSTCalendar.getYearFromInput(sEndDate), false);
            bResetQueryParam = false;
        }
        
        return super.SearchBy(bResetQueryParam, module, sd);
    }

/* Pretty prints a link that was already followed when creating TSRIndex
 * (non-Javadoc)
 * @see ro.cst.tsearch.servers.types.TSServer#getPrettyFollowedLink(java.lang.String)
 */	
    public String getPrettyFollowedLink (String initialFollowedLnk)
    {
    	if (initialFollowedLnk.matches("(?i).*[^a-z]+(instrs|marginals)[=]([0-9]+)[&=a-z]+crossRefSource[=]([a-z]*)[^a-z]*.*"))
    	{
    		String retStr = initialFollowedLnk.replaceFirst(
    				"(?i).*[^a-z]+(instrs|marginals)[=]([0-9]+)[&=a-z]+crossRefSource[=]([a-z]*)[^a-z]*.*", 
    				"Instrument " + "$2" + " " /*+ "$3"*/ + " has already been processed from a previous search in the log file.");
    		retStr =  "<br/><span class='followed'>" + retStr + "</span><br/>";
    		
    		return retStr;
    	}
    	else if (initialFollowedLnk.matches("(?i).*[^a-z]+instrs[=]([0-9]+)[^0-9]*.*"))
    	{
    		String retStr = initialFollowedLnk.replaceFirst(
    				"(?i).*[^a-z]+instrs[=]([0-9]+)[^0-9]*.*", 
    				"Instrument " + "$1" + " has already been processed from a previous search in the log file. ");
    		retStr =  "<br/><span class='followed'>" + retStr + "</span><br/>";
    		
    		return retStr;
    	}
    		
    	
    	return "<br/><span class='followed'>Link already followed</span>:" + initialFollowedLnk + "<br/>";
    }
    
    /**
     * @see TSInterface#GetLink(java.lang.String) the only link for nashvile is
     *      the link to file.
     */
   
    /*
     * Given an instrument number, it forms a link that starts a search 
     * for the respective value.
     */
    private String GetLinkInstrument (String instr)
    {
    	String link_str=new String();
        String default_link=CreatePartialLink(TSConnectionURL.idGET);
        default_link.substring(0,default_link.indexOf("&Link="));
        link_str = " <a HREF='" + default_link + SEARCH_PATH +
        "&Instrs=" + instr + "'>" + instr + "</a> ";
        
        return link_str;
    }
    
    /*
     * Given a book and a page number, it forms a link that starts a search 
     * for the respective values.
     */
    private String GetLinkBookPage (String book, String page, String bk_pg)
    {
    	String link_str=new String();
        String default_link=CreatePartialLink(TSConnectionURL.idGET);
        default_link.substring(0,default_link.indexOf("&Link="));
        link_str = " <a HREF='" + default_link + SEARCH_PATH +
        "&Instrs=&Book=" + book + "&Page=" + page +
        "&SUBMIT=Detail Data&SortDir=ASC&StartDate=&EndDate="+"'>" + bk_pg + "</a> ";
        
        return link_str;
    }
    
    /**
     * Add the document missing links
     * @param record
     * @return
     */
    private String addMissingLinks (String record){
    	
    	// determine start of interesting sequence
    	int istart = record.indexOf("Marginal:");
    	if(istart == -1){return record;}
    	istart = record.indexOf("<TD", istart);
    	if(istart == -1){return record;}
    	istart = record.indexOf(">", istart);
    	if(istart == -1){return record;}
    	istart +=1;
    	
    	// determine end of interesting sequence
    	int iend = record.indexOf("</TD></TR></Table>", istart);
    	if(iend == -1){return record;}
    	
    	// split record into prefix, toProcess, suffix
    	String prefix = record.substring(0, istart);
    	String toProcess = record.substring(istart, iend).trim();
    	String suffix = record.substring(iend);
    	
    	// build the result
    	StringBuilder sb = new StringBuilder();
    	// add prefix
    	sb.append(prefix);
    	
    	// nothing to process
    	if("&nbsp".equals(toProcess) || "&nbsp;".equals(toProcess)){
    		return record;
    	}
    	
//    	<i>Bkwd</i> TB 1777 165 (TD)
//    	<i>Bkwd</i> MapBk 65S MapPg 78
//    	<i>Bkwd</i> PCL 119BK-3
//    	<i>Bkwd</i> REL 309 1138 
//    	<i>Bkwd</i> PCL 081CF-037
//    	<i>Bkwd</i> PCL 6W 119BK 003
    	//$1= REF TYPE DOCTYPE $3=BOOK $4=PAGE 
    	//search for book and page
    	//Search for instrument
    	
    	// process and add each record to output 
    	toProcess = toProcess.replaceAll("</?nobr>","");
    	String [] parts = toProcess.split(",");
    	boolean first = true;
    	for(String part : parts){
    		if(!first){ sb.append(","); } first = false;    
    		
			// re-write part according to rules, using only RE, no indices
			// for 'part' there can be only one type match: (book, page) or instrument
    		Matcher bpMatcher = bpPattern.matcher(part);
    		Matcher hrefMatcher = hrefPattern.matcher(part);
			int ahref = -1;
			if(hrefMatcher.find())
			{
				String ahref_str = hrefMatcher.group(0);
				ahref = part.indexOf(ahref_str);
			}
			
			if(bpMatcher.find())
			{
				String raw_str = bpMatcher.group(0);
				String ltype     = bpMatcher.group(1);
				String bk_pg   = bpMatcher.group(2);
				String book     = bpMatcher.group(3);
				String page     = bpMatcher.group(4);
				
				
				
				if (ahref==-1 || (ahref>part.indexOf(raw_str)) )
				{
					int end_idx =  part.indexOf(raw_str)+raw_str.length();
					String end = part.substring(end_idx);
					if (bk_pg.contains("MapPg"))
					{
						bk_pg = ltype.substring(ltype.indexOf("MapBk")) + bk_pg;
						ltype   = ltype.substring(0, ltype.indexOf("MapBk"));
					}
					
					if (bk_pg.substring(bk_pg.indexOf(book)).contains("BK"))
					{//the pair (book, page) is in this case reversed, (page, book)
						if (book.indexOf("BK")>0)
							book = book.substring(0, book.indexOf("BK"));
						else if (book.indexOf("bk")>0)
							book = book.substring(0, book.indexOf("bk"));
						if (page.indexOf("BK")>0)
							page = page.substring(0, page.indexOf("BK"));	//e.g. instr# 199311020047607, 6W-143BK -> Book=143, Page=6
						else if (page.indexOf("bk")>0)
							page = page.substring(0, page.indexOf("bk"));
						book = book.replaceFirst("[NSEW]+$", "");			//e.g. instr# 199311020047607, 6W-143BK -> Book=143, Page=6
						String aux = page;
						page = book;
						book = aux;
					}
					
					book = book.replaceFirst("[NSEW]+$", "");				//e.g. instr# 199201240029514, MapBk 72S MapPg 26 -> Book=72, Page=26
					book = book.replaceAll( "^0+(.*)" , "$1");
					page = page.replaceAll( "^0+(.*)" , "$1");
					if(ltype.contains("PCL"))
						part = ltype + bk_pg + end;
					else
						part = ltype+GetLinkBookPage(book, page, bk_pg )+end;
				}
			}

			Matcher iMatcher = iPattern.matcher(part);
    		if(iMatcher.find())
    		{
				String raw_str = iMatcher.group(0);
				String ltype = iMatcher.group(1);
				String instrum = iMatcher.group(2);

				if (ahref==-1 || (ahref>part.indexOf(raw_str)) )
				{
					int end_idx = part.indexOf(raw_str)+raw_str.length();
					String end = part.substring(end_idx);
					part = ltype+GetLinkInstrument(instrum)+end;
				}
			}
    		sb.append("<nobr>" + part + "</nobr>");
    	}
//    	sb.append(toProcess);
    	// add suffix
    	sb.append(suffix);    		
    	
    	return sb.toString();
    	
    }
    /**
     * @param rsResponce
     * @param viParseID
     */
    boolean noImage = false;
    @SuppressWarnings("unchecked")
	protected void ParseResponse(String sAction, ServerResponse Response,
            int viParseID) throws ServerResponseException {
        int iTmp;
        String rsResponce = Response.getResult();
        String sFileLink = null;
        switch (viParseID) {
        case ID_SEARCH_BY_NAME:
        case ID_SEARCH_BY_PARCEL:
        case ID_SAVE_TO_TSD:
        case ID_SEARCH_BY_INSTRUMENT_NO:
        case ID_SEARCH_BY_SUBDIVISION_NAME:
        	
        	// Check if the search input was too general.
        	if (rsResponce.indexOf("The selection criteria was too general") != -1){
        		Response.getParsedResponse()
				.setError("The selection criteria was too general.");
        		return;
        	}
        	
        	// Check if no results were found.
    		if (rsResponce.indexOf("NO RECORDS RETRIEVED FOR THE SPECIFIED SEARCH CRITERIA") > -1) {
    			Response.getParsedResponse()
    					.setError("No results were found for your query! Please change your search criteria and try again.");
    			return;
    		}
        	
           
        	if (rsResponce.indexOf("document has never been scanned") != -1) {

    			int i = rsResponce.indexOf("The image for Instrument");
                int j = rsResponce.indexOf("Deeds.", i) + 6;
                Response.getParsedResponse().setError(
                        rsResponce.substring(i, j));
                Response.setDisplayMode(ServerResponse.HIDE_BACK_TO_PARENT_SITE_BUTTON);
                noImage = true;
                return;
            }
        	
            if (rsResponce.indexOf("Security Violation") > -1) {
                Response.getParsedResponse().setError("Security Violation");
                throw new ServerResponseException(Response);
            }
            
            if (rsResponce.indexOf(CreatePartialLink(TSConnectionURL.idGET)) == -1 && rsResponce.indexOf("NO IMAGE") == -1) {
                //response not yet processed by us
                if (rsResponce.indexOf("HREF='LoadImage.asp") == -1 && Response.getRawQuerry().indexOf( "Names+Summary" ) < 0 ) {
                    return;
                }
                // fetch instrument No
                rsResponce = rsResponce.replaceAll("\\r\\n", "");
                //cut all until the first hr
                rsResponce = rsResponce.substring(rsResponce.indexOf("<hr>"));
                
                
                int end = rsResponce.indexOf("<table border=1 width='100%'><tr><td><b>Search Criteria:</b>");
                if(end>0){
                	//skip all the information until the last table and kut all from here
                	rsResponce = rsResponce.substring(0, end);
                }
                
                rsResponce = rsResponce.replaceAll("<A HREF='", "<A target=\"_blank\" HREF='" 
                		+ CreatePartialLink(TSConnectionURL.idGET) + "/recordings/");
                rsResponce = rsResponce.replaceAll("<a href=", "<a href=" 
                		+ CreatePartialLink(TSConnectionURL.idGET) + "/recordings/");
                
                rsResponce = rsResponce.replaceAll("(LoadImage.asp\\?\\d+)", "$1");
                
                rsResponce = rsResponce.replaceAll("'simplequery(.[^'>]*)'>", "simplequery$1>");

                if( Response.getRawQuerry().indexOf( "Names+Summary" ) >= 0 )
                {
                    //automatic name search --> first the name summary list is retrieved
                    String querry = Response.getQuerry();
                    
                    querry = querry.replaceAll( "Names Summary", "Detail Data" );
                    if ((iTmp = rsResponce.indexOf("<form")) != -1)
                    {
                        //logger.debug(" am gasit form!!!");
                        int endIdx = rsResponce.indexOf("/form>", iTmp) + 6;
                        rsResponce = rsResponce.substring(iTmp, endIdx);
                        
                        rsResponce = rsResponce.replaceAll( "<input TYPE=\"checkbox\" NAME=\"Names\" Value=\"(.*?)\">", "<a href='" + CreatePartialLink(TSConnectionURL.idPOST) + sAction + "&" + querry + "&Names=$1&automaticNameSearch=true'>View</a>" );                        
                    }
                }
                else
                {
                    if ((iTmp = rsResponce.indexOf("<form")) != -1) {
                        //logger.debug(" am gasit form!!!");
                        int endIdx = rsResponce.indexOf("/form>", iTmp) + 6;
                        String sForm = rsResponce.substring(iTmp, endIdx);
                        List links = GetRequestSettings(sForm, true);
                                            
                        String replacer = "";
                        if (links.size() == 0) {
                            ;
                        } else if (links.size() == 1) {
                            String name = "";
                            String link = (String) links.get(0);
                            //logger.debug("link =" + link);
                            Matcher m = Pattern.compile("Detail Data \\d*-\\?").matcher(link);
                            if (m.find()) {
                                name = "Next";
                            } else {
                                name = "Previous";
                            }
                            //logger.debug("name =" + name);
                            replacer += CreateLink(name, "/recordings" + link + "&np=1", TSConnectionURL.idPOST);
                            
                        } else {
                            replacer += CreateLink("Previous", "/recordings" + ((String) links.get(0)) + "&np=1", TSConnectionURL.idPOST);
                            replacer += CreateLink("Next", "/recordings" + ((String) links.get(1)) + "&np=1", TSConnectionURL.idPOST);
                        }
                        //logger.debug("replacer =" + replacer);
                        rsResponce = rsResponce.substring(0, iTmp) + replacer
                                + rsResponce.substring(endIdx);
                    }
                }
            }
            
            
            if (viParseID == ID_SAVE_TO_TSD) {
            	
            	/*failsafe for the case where two responses get here*/
                try {
                    DavidsonSplit ds = new DavidsonSplit(); //create new davidson splitter
                    ds.setDoc(rsResponce); //we want to split rsResponce
                    int savedDocumentCount = ds.getSplitNo(); //determine how many docs we have here
                    
                    if( savedDocumentCount > 1 ){ //more than one doc found --> not good --> use only the first one :D
                    	rsResponce = ds.getSplitDoc( 0 );
                    }
                }
                catch( Exception e ){
                	e.printStackTrace();
                }
            	
                String sInstrumentNo = getInstrNoFromResponse(rsResponce);
                logger.info("Instrument NO:" + sInstrumentNo);
                msSaveToTSDFileName = sInstrumentNo + ".html";
                Response.getParsedResponse().setFileName(getServerTypeDirectory() + msSaveToTSDFileName);
                msSaveToTSDResponce = rsResponce + CreateFileAlreadyInTSD(true);
                //download view file if any;
                
                if(rsResponce.indexOf("Display Doc") != -1)
                {
                    //logger.debug(" sFileLink = " + sFileLink );
                    
                    sFileLink = sInstrumentNo + ".tif";
                    int iTmp1 = rsResponce.indexOf(CreatePartialLink(TSConnectionURL.idGET));
                    String imglink = rsResponce.substring(iTmp1, rsResponce.indexOf("'", iTmp1));
                    
                    ImageLinkInPage imageLinkInPage = new ImageLinkInPage(imglink, sFileLink);
                    imageLinkInPage.setFakeLink(true);
                    
                    Response.getParsedResponse().addImageLink(imageLinkInPage);
                }
                //take out 'Show all marginal" link
                rsResponce = rsResponce.replaceFirst(
                        ",\\s*<a href=[^>]+Marginals=(.*?)</a>", "");
                ParsedResponse pr = Response.getParsedResponse();
                //save any coss ref link before removing it
                parser.Parse(pr, rsResponce, Parser.PAGE_DETAILS,
                        getLinkPrefix(TSConnectionURL.idGET),
                        TSServer.REQUEST_SAVE_TO_TSD);
                //addLinksBookAndPage(pr);
                //+
                //	removing "Marginal" link
                rsResponce = rsResponce.replaceAll("<a.*?>(.*?)</a>", "$1");
                pr.setOnlyResponse(rsResponce);
            } else { // not saving to TSD
                if( Response.getRawQuerry().indexOf( "Names+Summary" ) < 0 )
                {
                    List items = new ArrayList();
                    boolean specialCase = false;
                    String prevLink = "";
                    String nextLink = "";
                    try {
                        DavidsonSplit ds = new DavidsonSplit();
                        ds.setDoc(rsResponce);
                        int count = ds.getSplitNo();
                        logger.debug("found count =" + count + " results");
                        prevLink = ds.getPrevLink();
                        nextLink = ds.getNextLink();
                        logger.debug("prev Link =" + prevLink);
                        logger.debug("next Link =" + nextLink);
                        if (count == 0) {
                            // special case: when there is only one document per
                            // page
                            //the splitter returns 0 items, this case should be
                            // treated differently
                            //logger.debug("zero results found, special
                            // treatment");
                            items.add(nextLink);
                            specialCase = true;
                        } else {
                        	String record = "";
                        	String procRecord = "";

                        	for (int i = 0; i < count; i++) {
                            	record = ds.getSplitDoc(i);
                            	try
                            	{
                            		procRecord = addMissingLinks(record);
                            	}
                            	catch (Exception e) {
                                    e.printStackTrace();
                                    logger.error("AddMissingLinksError:", e);
                                }

                                items.add(procRecord);
                            }
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                        logger.error("DavidsonSplitterError:", e);
                    }
                    String result = new String();
                    Vector parsedRows = new Vector();
                    
                    for (Iterator iter = items.iterator(); iter.hasNext();) {
                        
                    	String item = (String) iter.next();
                        String initialResponse = item;
                        //logger.debug("item1="+item);
                        String sInstrumentNo = getInstrNoFromResponse(item);
                        String originalLink = getOriginalLink(sInstrumentNo);
                        String sSave2TSDLink = getLinkPrefix(TSConnectionURL.idGET)
                                + originalLink;
                        
                        HashMap<String, String> data = new HashMap<String,String>();
        				
                        try {
                        	data.put("instno", sInstrumentNo);
                        	Pattern doctypePattern = Pattern.compile("(?ism)Document Type: </B>(.*?)</TD>");
                        	Matcher doctypeMatcher = doctypePattern.matcher(initialResponse);
                        	if(doctypeMatcher.find()) {
                        		data.put("type", doctypeMatcher.group(1));	
                        	}
                        }catch(Exception e) {
                        	e.printStackTrace();
                        }
        				
                        if (isInstrumentSaved(sInstrumentNo, null, data)) {
                            item += CreateFileAlreadyInTSD(true) + "<br>";
                        } else {
                            //special case
                            if (specialCase) {
                                if (item.toLowerCase().indexOf("</table") == -1) {
                                    int i2 = item.indexOf("Display Doc");
                                    int i3 = item.indexOf("</a>", i2);
                                    if (i3 == -1)
                                        i3 = item.indexOf("</A>", i2);
                                    item = new StringBuffer(item).insert(i3 + 4,
                                            "</td></tr></table>").toString();
                                }
                            }
                            
                            //item = addSaveToTsdButton(item, sSave2TSDLink, true);
                            item = item.replaceFirst("</TR></Table>", 
                            		"</TR><TR><TD COLSPAN='100'><input type='checkbox' name='docLink' value='" 
                            		+ sSave2TSDLink + "'>Select for saving to TS Report</TD></TR><TR><TD COLSPAN='100'><hr></TD></TR></Table>");
                            
                            mSearch.addInMemoryDoc(sSave2TSDLink, initialResponse);
                        }
                        ParsedResponse pr = new ParsedResponse();
                        pr.setPageLink(new LinkInPage(sSave2TSDLink, originalLink,
                                TSServer.REQUEST_SAVE_TO_TSD));
                        //logger.debug("item2="+item);
                        //take out 'Show all marginal" link, only for parsing, it will remain for displaying to user
                        String itemWithoutAllMarginalsLink = item.replaceFirst(
                                ",\\s*<a href=[^>]+Marginals=(.*?)</a>", "");
                        parser.Parse(pr, itemWithoutAllMarginalsLink,
                                Parser.PAGE_DETAILS,
                                getLinkPrefix(TSConnectionURL.idGET),
                                TSServer.REQUEST_SAVE_TO_TSD);
                        pr.setOnlyResponse(item);
                        
                        //download view file if any;
                        
                        if(item.indexOf("Display Doc") != -1)
                        {
                            //logger.debug(" sFileLink = " + sFileLink );
                            
                            sFileLink = sInstrumentNo + ".tif";
                            int iTmp1 = item.indexOf(CreatePartialLink(TSConnectionURL.idGET));
                            String imglink = item.substring(iTmp1, item.indexOf("'", iTmp1));
                            
                            ImageLinkInPage imageLinkInPage = new ImageLinkInPage(imglink, sFileLink);
                            imageLinkInPage.setFakeLink(true);
                            
                            pr.addImageLink(imageLinkInPage);
                        }
                        
                        parsedRows.add(pr);
                        result += item;
                    }
                    rsResponce = result;
                    
                    if (mSearch.getSearchType() == Search.PARENT_SITE_SEARCH) {
    	                // add form to result
    	                Response.getParsedResponse().setHeader(CreateSaveToTSDFormHeader(URLConnectionReader.MULTIPLE_SAVE_TO_TSD_ACTION, "POST") +
    	                		SELECT_ALL_CHECKBOXES + "<font color='#08088A'><b>Select all documents</b></font>");
                    
                        if (!prevLink.equals("")) {
                            ParsedResponse pr = new ParsedResponse();
                            parser.Parse(pr, prevLink + "&nbsp;&nbsp;&nbsp;&nbsp;", Parser.NO_PARSE);
                            parsedRows.add(pr);
                        }
                        if (!nextLink.equals("")) {
                            ParsedResponse pr1 = new ParsedResponse();
                            parser.Parse(pr1, nextLink + "&nbsp;&nbsp;&nbsp;", Parser.NO_PARSE);
                            parsedRows.add(pr1);
                        }
                        
                        //add form end to result
                        Response.getParsedResponse().setFooter(CreateSaveToTSDFormEnd(SAVE_SELECTED_DOCUMENTS_BUTTON_LABEL, viParseID, -1));
                    }
                    
                    //parser.Parse(Response.getParsedResponse(), rsResponce, Parser.NO_PARSE);
                    //logger.debug("am gasint rezultate = " + parsedRows.size());
                    
                    Response.getParsedResponse().setResultRows(parsedRows);
                    Response.getParsedResponse().setNextLink(nextLink);
                    Response.getParsedResponse().setOnlyResponse(rsResponce);
                }
                else
                {
                    rsResponce = rsResponce.replaceAll( "(?i)</?form.*?>", "" );
                    rsResponce = rsResponce.replaceAll( "(?i)<input.*?>", "" );
                    rsResponce = rsResponce.replaceAll( "(?i)<br>", "" );
                    iTmp = rsResponce.indexOf( "<p>" );
                    if(iTmp >= 0)
                    {
                        rsResponce = rsResponce.substring( iTmp + 3 );
                    }
                    
                    rsResponce = rsResponce.replaceAll( "(<a href=.*?>.*?</a>)([^<]*)", "<tr><td>$1</td><td>$2</td></tr>" );
                    rsResponce = "<table>" + rsResponce + "</table>";
                    
                    parser.Parse(Response.getParsedResponse(), rsResponce, Parser.PAGE_ROWS_NAME, getLinkPrefix(TSConnectionURL.idPOST), TSServer.REQUEST_GO_TO_LINK_REC);
                    /*
                    for( int i = 0 ; i < Response.getParsedResponse().getResultRows().size() ; i ++ )
                    {
                        ParsedResponse pr = (ParsedResponse) Response.getParsedResponse().getResultRows().get(i);
                        LinkInPage linkObj = pr.getPageLink();
                    }*/
                }
            }
            break;
        case ID_GET_LINK:
            if( Response.getQuerry().indexOf( "automaticNameSearch" ) >= 0 ||
            	Response.getQuerry().indexOf( "np=1" ) >= 0 ) {
            	
                ParseResponse(sAction, Response, ID_SEARCH_BY_NAME);
            }
            else {
                ParseResponse(sAction, Response, ID_SEARCH_BY_INSTRUMENT_NO);
            }
            break;
        default:
            break;
        }
    }

    private String getInstrNoFromResponse(String sTmp) {
        int iTmp = sTmp.indexOf("Instrument");
        iTmp = sTmp.indexOf("/B>", iTmp);
        iTmp = sTmp.indexOf(">", iTmp) + 1;
        String sInstrumentNo = sTmp.substring(iTmp, sTmp.indexOf("<", iTmp))
                .trim();
        return sInstrumentNo;
    }

    protected DownloadImageResult saveImage(ImageLinkInPage image)
            throws ServerResponseException {
        getTSConnection().SetReferer(FILE_REFERER);
        DownloadImageResult res =super.saveImage(image);
        getTSConnection().SetReferer("");
        
        if ("text/html".equals(getTSConnection().getMsContentType())) {
        	setNOImage(image.getInstrumentNo());
        }
        return res ;
    }
    
    private void setNOImage(String instrNo) {
    	
    	if(instrNo == null)
    		return;
    	
    	Search search = InstanceManager.getManager().getCurrentInstance(searchId).getCrtSearchContext();
    	DocumentsManagerI docManager = search.getDocManager();
        
        try {
			docManager.getAccess();
			InstrumentI instr = new Instrument();
			instr.setInstno(instrNo);
			ArrayList<DocumentI> docs = (ArrayList<DocumentI>)docManager.getDocumentsWithInstrumentsFlexible(false, instr);
			if(docs.size() > 0) {
				if (docs.size() == 1) {
					DocumentI doc = docs.get(0);
					doc.setImage(null);
				} else {
					System.err.println("Big problem in TNHamiltonRO method setNoImage");
					EmailClient emailClient = new EmailClient();
					emailClient.setSubject("Big problem in TNHamiltonRO method setNoImage");
					emailClient.addTo(MailConfig.getExceptionEmail());
					emailClient.addContent("Big problem in TNHamiltonRO method setNoImage\n" +
							"instrNo = " + instrNo + "\n" + 
							"SA: " + search.getSa());
					emailClient.sendAsynchronous();
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			docManager.releaseAccess();
		}
    	/*
    	synchronized (search) {
            TSDIndexPage tsd = search.getTSDIndexPage();
            if (tsd != null) {
            	
	            int chapterIndex  = tsd.getChapterIndex(instrNo);
	            if (chapterIndex >= 0) {
	                
	            	Chapter chapter = tsd.getChapter(chapterIndex);
	            	
	            	chapter.setREMARKS("NO IMAGE " + chapter.getREMARKS());
	            	search.removeImage(chapter.getCHECKBOX());
	            	
	            	File file = new File(chapter.getCHECKBOX());
	            	if (file.exists()) {
	            		
	            		try {
	            		
	            			FileInputStream is = new FileInputStream(file);
	            	
			            	ByteArrayOutputStream baos = new ByteArrayOutputStream();
			    			byte[] buff = new byte[1024];
			    			int length;
			    			while ( (length = is.read(buff)) != -1 )
			                {
			    				baos.write(buff, 0, length);
			                }
			    			
			    			String body = new String(baos.toByteArray());
			    			
			    			is.close();
			    			
			    			body = body.replaceFirst("<A target.*?Display Doc</a>", "NO IMAGE");
			    			
			    			FileOutputStream os = new FileOutputStream(file);
			    			os.write(body.getBytes());
			    			os.flush();
			    			os.close();
			    			
	            		} catch (Exception e) {
	            			e.printStackTrace();
	            		}
	            	}
	    			
	            	tsd.setChapter(chapter, chapterIndex);
	            }
            }
    	}*/
    }
    
    protected String getFileNameFromLink(String url) {
    	
    	String rez = url.replaceAll("(?i)(?s).*instrs=(.*?)(?=&|$)", "$1");
    	
    	if (rez.trim().length() > 10) {
    		rez = rez.replaceAll("&parentSite=true", "");
    		rez = rez.replaceAll("&isSubResult=true", "");
    		rez = rez.replaceFirst("&crossRefSource=[^&]+", "");
    	}
        
        return rez.trim() + ".html";
    }

    public static void splitResultRows(Parser p, ParsedResponse pr, String htmlString, int pageId, String linkStart, int action) throws ro.cst.tsearch.exceptions.ServerResponseException {
        p.splitResultRows(pr, htmlString, pageId, "<tr","</table", linkStart,  action);
    }
        
	@Override
	protected void setCertificationDate() {

		try {
			if (CertificationDateManager.isCertificationDateInCache(dataSite)){
				String date = CertificationDateManager.getCertificationDateFromCache(dataSite);
				getSearch().getSa().updateCertificationDateObject(dataSite, Util.dateParser3(date));
			} else{
	        	String html = HttpUtils.downloadPage("http://www.knoxrod.org/recordings/default.asp");
	            Matcher certDateMatcher = certDatePattern.matcher(html);
	            if(certDateMatcher.find()) {
	            	String date = certDateMatcher.group(1).trim();
	            	
	            	date = DateFormatUtils.format(Util.dateParser3(date), "MM/dd/yyyy");
	            	
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
			module.clearSaKeys();
			module.forceValue(0, restoreDocumentDataI.getInstrumentNumber());
			module.forceValue(3, "Detail Data");
			
		} else if(StringUtils.isNotEmpty(book) && StringUtils.isNotEmpty(page)) {
			module = getDefaultServerInfo().getModule(TSServerInfo.BOOK_AND_PAGE_MODULE_IDX);
			module.clearSaKeys();
			module.forceValue(0, book);
			module.forceValue(1, page);
			module.forceValue(3, "Detail Data");
		} else if(StringUtils.isNotEmpty(restoreDocumentDataI.getDocumentNumber())) {
			module = getDefaultServerInfo().getModule(TSServerInfo.INSTR_NO_MODULE_IDX);
			module.clearSaKeys();
			module.forceValue(0, restoreDocumentDataI.getDocumentNumber());
			module.forceValue(3, "Detail Data");
		} else {
			module = null;
		}
		return module;
	}
	
	public Object getImageDownloader(RestoreDocumentDataI document) {
		return getRecoverModuleFrom(document);
	}
	
	 public InstrumentGenericIterator getInstrumentIterator(boolean instrumentType) {
			InstrumentGenericIterator instrumentGenericIterator = new InstrumentGenericIterator(searchId) {

				private static final long serialVersionUID = 5399351945130601258L;

				@Override
				public String getInstrumentNoFrom(InstrumentI state, HashMap<String, String> filterCriteria) {
					if (StringUtils.isNotEmpty(state.getInstno())) {
						if (filterCriteria != null) {
							filterCriteria.put("InstrumentNumber", state.getInstno());
						}
					}
					return state.getInstno();
				}
				
				@Override
				protected String cleanPage(String input) {
					
					if (org.apache.commons.lang.StringUtils.isNotEmpty(input)){
						if (input.matches("(?is)[A-Z]\\d")){
							return (input.substring(0, 1) + "0" + input.substring(1, 2));
						}
					}
					return super.cleanPage(input);
				}

			};

			if (instrumentType) {
				instrumentGenericIterator.enableInstrumentNumber();
			} else {
				instrumentGenericIterator.enableBookPage();
			}
			instrumentGenericIterator.setDoNotCheckIfItExists(true);
			return instrumentGenericIterator;
		}

}