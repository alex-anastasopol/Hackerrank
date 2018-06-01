/*
 * Created on Nov 18, 2004
 */
package ro.cst.tsearch.servers.types;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Set;
import java.util.Vector;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.log4j.Category;
import org.apache.log4j.Logger;
import org.htmlparser.Tag;
import org.htmlparser.filters.TagNameFilter;
import org.htmlparser.tags.LinkTag;
import org.htmlparser.tags.TableColumn;
import org.htmlparser.tags.TableRow;
import org.htmlparser.tags.TableTag;
import org.htmlparser.util.NodeList;

import ro.cst.tsearch.MailConfig;
import ro.cst.tsearch.Search;
import ro.cst.tsearch.bean.SearchAttributes;
import ro.cst.tsearch.emailClient.EmailClient;
import ro.cst.tsearch.exceptions.ServerResponseException;
import ro.cst.tsearch.extractor.xml.Bridge;
import ro.cst.tsearch.extractor.xml.GenericFunctions1;
import ro.cst.tsearch.extractor.xml.GenericFunctions2;
import ro.cst.tsearch.extractor.xml.ResultMap;
import ro.cst.tsearch.extractor.xml.ResultTable;
import ro.cst.tsearch.extractor.xml.StringFormats;
import ro.cst.tsearch.generic.Util;
import ro.cst.tsearch.parser.HtmlParser3;
import ro.cst.tsearch.search.FunctionStatesIterator;
import ro.cst.tsearch.search.ModuleStatesIterator;
import ro.cst.tsearch.search.filter.BetweenDatesFilterResponse;
import ro.cst.tsearch.search.filter.FilterResponse;
import ro.cst.tsearch.search.filter.newfilters.address.AddressFilterFactory;
import ro.cst.tsearch.search.filter.newfilters.date.DateFilterFactory;
import ro.cst.tsearch.search.filter.newfilters.date.LastTransferDateFilter;
import ro.cst.tsearch.search.filter.newfilters.legal.LegalFilterFactory;
import ro.cst.tsearch.search.filter.newfilters.name.NameFilterFactory;
import ro.cst.tsearch.search.filter.newfilters.pin.PINFilterFactory;
import ro.cst.tsearch.search.filter.newfilters.pin.PinFilterResponse;
import ro.cst.tsearch.search.iterator.ModuleStatesIteratorFactory;
import ro.cst.tsearch.search.module.ConfigurableNameIterator;
import ro.cst.tsearch.search.validator.DocsValidator;
import ro.cst.tsearch.servers.SearchDataWrapper;
import ro.cst.tsearch.servers.TSConnectionURL;
import ro.cst.tsearch.servers.info.TSServerInfo;
import ro.cst.tsearch.servers.info.TSServerInfoConstants;
import ro.cst.tsearch.servers.info.TSServerInfoModule;
import ro.cst.tsearch.servers.response.ImageLinkInPage;
import ro.cst.tsearch.servers.response.LinkInPage;
import ro.cst.tsearch.servers.response.OtherInformationSet.OtherInformationSetKey;
import ro.cst.tsearch.servers.response.ParsedResponse;
import ro.cst.tsearch.servers.response.Parser;
import ro.cst.tsearch.servers.response.PropertyIdentificationSet.PropertyIdentificationSetKey;
import ro.cst.tsearch.servers.response.SaleDataSet.SaleDataSetKey;
import ro.cst.tsearch.servers.response.ServerResponse;
import ro.cst.tsearch.servlet.URLConnectionReader;
import ro.cst.tsearch.titledocument.abstracts.DocumentTypes;
import ro.cst.tsearch.utils.GBManager;
import ro.cst.tsearch.utils.HttpUtils;
import ro.cst.tsearch.utils.InstanceManager;
import ro.cst.tsearch.utils.StringUtils;
import ro.cst.tsearch.utils.Tidy;
import ro.cst.tsearch.utils.URLMaping;

import com.stewart.ats.base.document.DocumentI;
import com.stewart.ats.base.document.InstrumentI;
import com.stewart.ats.base.document.RegisterDocumentI;
import com.stewart.ats.base.document.RestoreDocumentDataI;
import com.stewart.ats.base.name.NameI;
import com.stewart.ats.base.search.DocumentsManagerI;

public class TNWilsonRO extends TSServer {

    protected static final Category logger = Logger.getLogger(TNWilsonRO.class);

    protected final int ID_DETAILS1 = 33101;
    protected final int ID_DETAILS2 = 33102;
    
    static final long serialVersionUID = 10000000;

    private final String IMAGES_SITE = "www.wilsondeeds.com";
    private final String IMAGES_SITE_IP = "www.wilsondeeds.com";

    private boolean downloadingForSave = false;
    
    private static Pattern crossrefDocTypePattern = Pattern.compile("(?is)<font[^>]*>([^<]*)</font>");
    private static Pattern recDatePattern = Pattern.compile("(?is)\\bsubmit'\\s*value='([^']+)");
    
    private static Pattern INSTRUMENT_NUMBER_PATT = Pattern.compile("(?is)\\bInstrument\\s*#\\s*(\\d*)\\s*");

    private String msLastdb = "1";
  
    private static final Pattern certDatePattern = Pattern.compile("(?ism)<b>Date</b>.*?<b>(.*?)</b>");
   
    protected void setModulesForAutoSearch(TSServerInfo serverInfo) {
        List<TSServerInfoModule> l = new ArrayList<TSServerInfoModule>();

        TSServerInfoModule m;

		SearchAttributes sa = InstanceManager.getManager().getCurrentInstance( searchId ).getCrtSearchContext().getSa();
		boolean emptyStreet = "".equals( sa.getAtribute( SearchAttributes.P_STREETNAME ) );
		boolean searchWithSubdivision = searchWithSubdivision();
 
		FilterResponse addressHighPassFilterResponse = AddressFilterFactory.getAddressHighPassFilter(searchId, 0.8d);
		DocsValidator defaultLegalValidator = LegalFilterFactory.getDefaultLegalFilter(searchId).getValidator();
		DocsValidator pinValidator = getPinFilter().getValidator(); 
		DocsValidator addressHighPassValidator = addressHighPassFilterResponse.getValidator();
		DocsValidator lastTransferDateValidator = (new LastTransferDateFilter(searchId)).getValidator();
		DocsValidator recordedDateValidator = BetweenDatesFilterResponse.getDefaultIntervalFilter(searchId).getValidator();
		DocsValidator subdivisionNameValidator = NameFilterFactory.getDefaultNameFilterForSubdivision(
				searchId).getValidator();		

		boolean validateWithDates = applyDateFilter();
		
        if( searchWithSubdivision ){
	        // grantor = subdivision + PLAT
	        m = new TSServerInfoModule(serverInfo.getModule(TSServerInfo.NAME_MODULE_IDX));
	        m.addExtraInformation(TSServerInfoConstants.EXTRA_PARAM_MODULE_DESCRIPTION, 
	        		TSServerInfoConstants.VALUE_PARAM_NAME_SUBDIVISION_PLAT);
	        m.setIteratorType(ModuleStatesIterator.TYPE_DEFAULT);
	        m.getFunction(0).setSaKey(SearchAttributes.LD_SUBDIV_NAME);
	        m.getFunction(0).setIteratorType(FunctionStatesIterator.ITERATOR_TYPE_LAST_WORD_INITIAL);
	        m.getFunction(1).setSaKey("");
	        m.getFunction(4).setData("PLAT");
	        m.clearSaKey(9);
	        m.clearSaKey(10);
	        //m.setData(9, startDate);
	        m.clearFunction(9);
	        m.setSaKey(10, SearchAttributes.TODATE_MM_DD_YYYY);
	        
	        m.addValidator( defaultLegalValidator );
	        m.addValidator( lastTransferDateValidator );
	        m.addValidator( pinValidator );
	        m.addValidator( subdivisionNameValidator );
	        m.addCrossRefValidator( defaultLegalValidator );
	        m.addCrossRefValidator( lastTransferDateValidator );
	        m.addCrossRefValidator( pinValidator );
	        m.addCrossRefValidator( subdivisionNameValidator );
	        if(validateWithDates) {
	        	m.addValidator( recordedDateValidator );
	        	m.addCrossRefValidator( recordedDateValidator );
	        }
	        m.setStopAfterModule(true);
	        l.add(m);
	        
	//      grantor = subdivision + EASEMENT
	        m = new TSServerInfoModule(serverInfo.getModule(TSServerInfo.NAME_MODULE_IDX));
	        m.addExtraInformation(TSServerInfoConstants.EXTRA_PARAM_MODULE_DESCRIPTION, 
	        		TSServerInfoConstants.VALUE_PARAM_NAME_SUBDIVISION_EASEMENT);
	        m.setIteratorType(ModuleStatesIterator.TYPE_DEFAULT);
	        m.getFunction(0).setSaKey(SearchAttributes.LD_SUBDIV_NAME);
	        m.getFunction(0).setIteratorType(FunctionStatesIterator.ITERATOR_TYPE_LAST_WORD_INITIAL);
	        m.getFunction(1).setSaKey("");
	        m.getFunction(4).setData("ESMT");
	        m.setSaKey(9, SearchAttributes.FROMDATE_MM_DD_YYYY);
	        m.setSaKey(10, SearchAttributes.TODATE_MM_DD_YYYY);
	        m.addValidator( defaultLegalValidator );
	        m.addValidator( lastTransferDateValidator );
	        m.addValidator( addressHighPassValidator );
	        m.addValidator( pinValidator );
	        m.addValidator( subdivisionNameValidator );
	        m.addValidator( recordedDateValidator );
	        m.addCrossRefValidator( defaultLegalValidator );
	        m.addCrossRefValidator( lastTransferDateValidator );
	        m.addCrossRefValidator( addressHighPassValidator );
	        m.addCrossRefValidator( pinValidator );
	        m.addCrossRefValidator( subdivisionNameValidator );
	        m.addCrossRefValidator( recordedDateValidator );
	        l.add(m);
	       
	      	//grantor = subdivision + RESTRICTION;
	        m = new TSServerInfoModule(serverInfo.getModule(TSServerInfo.NAME_MODULE_IDX));
	        m.addExtraInformation(TSServerInfoConstants.EXTRA_PARAM_MODULE_DESCRIPTION, 
	        		TSServerInfoConstants.VALUE_PARAM_NAME_SUBDIVISION_RESTRICTION);
	        m.setIteratorType(ModuleStatesIterator.TYPE_DEFAULT);
	        m.getFunction(0).setSaKey(SearchAttributes.LD_SUBDIV_NAME);
	        m.getFunction(0).setIteratorType(FunctionStatesIterator.ITERATOR_TYPE_LAST_WORD_INITIAL);
	        m.getFunction(1).setSaKey("");
	        m.getFunction(4).setData("REST");
	        m.setSaKey(9, SearchAttributes.FROMDATE_MM_DD_YYYY);
	        m.setSaKey(10, SearchAttributes.TODATE_MM_DD_YYYY);
	        m.addValidator( defaultLegalValidator );
	        m.addValidator( lastTransferDateValidator );
	        m.addValidator( addressHighPassValidator );
	        m.addValidator( pinValidator );
	        m.addValidator( subdivisionNameValidator );
	        m.addValidator( recordedDateValidator );
	        m.addCrossRefValidator( defaultLegalValidator );
	        m.addCrossRefValidator( lastTransferDateValidator );
	        m.addCrossRefValidator( addressHighPassValidator );
	        m.addCrossRefValidator( pinValidator );
	        m.addCrossRefValidator( subdivisionNameValidator );
	        m.addCrossRefValidator( recordedDateValidator );
	        l.add(m);
	        
        } else {
        	printSubdivisionException();
        }
        
        //it's included in the search by bookpage from the search list (below)
        //m = new TSServerInfoModule(serverInfo.getModule(TSServerInfo.BOOK_AND_PAGE_MODULE_IDX));
        //m.addCrossRefValidatorType(DocsValidator.TYPE_REGISTER_SUBDIV_LOT);
        //l.add(m);
        
        if( searchWithSubdivision ){
	        m = new TSServerInfoModule(serverInfo.getModule(TSServerInfo.SUBDIVISION_MODULE_IDX));
	        m.addExtraInformation(TSServerInfoConstants.EXTRA_PARAM_MODULE_DESCRIPTION, 
	        		TSServerInfoConstants.VALUE_PARAM_SUBDIVISION_LOT_PHASE);
	        m.addValidator(lastTransferDateValidator);
	        m.addValidator(LegalFilterFactory.getDefaultLotFilter(searchId).getValidator());
	        m.getFunction(0).setSaKey(SearchAttributes.LD_SUBDIV_NAME_AND_PHASE);
	        m.getFunction(2).setIteratorType(FunctionStatesIterator.ITERATOR_TYPE_LOT_INTERVAL);
	        m.addCrossRefValidator( defaultLegalValidator );
	        m.addCrossRefValidator( addressHighPassValidator );
	        m.addCrossRefValidator( pinValidator );
			if (validateWithDates) {
				m.addValidator(recordedDateValidator);
				m.addCrossRefValidator(recordedDateValidator);
			}
	        l.add(m);
	        
	        if(!"".equals(sa.getAtribute(SearchAttributes.LD_SUBDIV_PHASE))){
		        m = new TSServerInfoModule(serverInfo.getModule(TSServerInfo.SUBDIVISION_MODULE_IDX));
		        m.addExtraInformation(TSServerInfoConstants.EXTRA_PARAM_MODULE_DESCRIPTION, 
		        		TSServerInfoConstants.VALUE_PARAM_SUBDIVISION_LOT);
		        m.addValidator(lastTransferDateValidator);
		        m.getFunction(0).setSaKey(SearchAttributes.LD_SUBDIV_NAME_AND_PHASE_ROMAN);
		        m.getFunction(2).setIteratorType(FunctionStatesIterator.ITERATOR_TYPE_LOT);
		        m.addCrossRefValidator( defaultLegalValidator );
		        m.addCrossRefValidator( addressHighPassValidator );
		        m.addCrossRefValidator( pinValidator );
				if (validateWithDates) {
					m.addValidator(recordedDateValidator);
					m.addCrossRefValidator(recordedDateValidator);
				}
		        l.add(m);
	        }
        }

        // search by book and page extracted from AO
        m = new TSServerInfoModule(serverInfo.getModule(TSServerInfo.BOOK_AND_PAGE_MODULE_IDX));
        m.addExtraInformation(TSServerInfoConstants.EXTRA_PARAM_MODULE_DESCRIPTION, 
        		TSServerInfoConstants.VALUE_PARAM_LIST_AO_BP);
        m.setIteratorType(ModuleStatesIterator.TYPE_BOOK_PAGE_LIST);
        m.getFunction(0).setSaKey("");
        m.getFunction(0).setIteratorType(FunctionStatesIterator.ITERATOR_TYPE_BOOK_FAKE);
        m.getFunction(1).setSaKey("");
        m.getFunction(1).setIteratorType(FunctionStatesIterator.ITERATOR_TYPE_PAGE_FAKE);
        m.addCrossRefValidator( defaultLegalValidator );
        m.addCrossRefValidator( addressHighPassValidator );
        m.addCrossRefValidator( pinValidator );
		if (validateWithDates) {
			m.addValidator(recordedDateValidator);
			m.addCrossRefValidator(recordedDateValidator);
		}
        m.setStopAfterModule(true);
        m.setSaObjKey(SearchAttributes.INSTR_LIST);
        l.add(m);
        
        // search by address       
        if( !emptyStreet ){
	        m = new TSServerInfoModule(serverInfo.getModule(TSServerInfo.ADDRESS_MODULE_IDX));
	        m.getFunction(0).setIteratorType(FunctionStatesIterator.ITERATOR_TYPE_DEFAULT_NOT_EMPTY);
	        m.getFunction(1).setIteratorType(FunctionStatesIterator.ITERATOR_TYPE_DEFAULT_NOT_EMPTY);
	        m.getFunction(2).setIteratorType(FunctionStatesIterator.ITERATOR_TYPE_DEFAULT_EMPTY);
	        m.getFunction(3).setIteratorType(FunctionStatesIterator.ITERATOR_TYPE_DEFAULT_EMPTY);
	        m.addCrossRefValidator( defaultLegalValidator );
	        m.addCrossRefValidator( addressHighPassValidator );
	        m.addCrossRefValidator( pinValidator );
			if (validateWithDates) {
				m.addValidator(recordedDateValidator);
				m.addCrossRefValidator(recordedDateValidator);
			}
	        l.add(m);
        }

     	// owner  
        ConfigurableNameIterator nameIterator = null;
        if( hasOwner() ){
        	m = new TSServerInfoModule(serverInfo.getModule(TSServerInfo.NAME_MODULE_IDX));
        	DocsValidator recordedDateNameValidator = BetweenDatesFilterResponse.getDefaultIntervalFilter(searchId, m).getValidator();
 	        m.addExtraInformation(TSServerInfoConstants.EXTRA_PARAM_MODULE_DESCRIPTION, 
 	        		TSServerInfoConstants.VALUE_PARAM_NAME_OWNERS);
 	        m.setSaObjKey(SearchAttributes.OWNER_OBJECT);
 	        m.setSaKey(9, SearchAttributes.FROMDATE_MM_DD_YYYY);
 	        m.setIteratorType(9, FunctionStatesIterator.ITERATOR_TYPE_FROM_DATE);
	        m.setSaKey(10, SearchAttributes.TODATE_MM_DD_YYYY);
 	        
 	        m.addFilter( NameFilterFactory.getDefaultNameFilter(SearchAttributes.OWNER_OBJECT, searchId, m) );
 	        m.addValidator( defaultLegalValidator );
	        m.addValidator( lastTransferDateValidator );
	        m.addValidator( addressHighPassValidator );
	        m.addValidator( pinValidator );
	        m.addValidator( subdivisionNameValidator );
	        m.addValidator( recordedDateNameValidator );
	        addFilterForUpdate(m, false);
	        m.addCrossRefValidator( defaultLegalValidator );
	        m.addCrossRefValidator( lastTransferDateValidator );
	        m.addCrossRefValidator( addressHighPassValidator );
	        m.addCrossRefValidator( pinValidator );
	        m.addCrossRefValidator( subdivisionNameValidator );
	        m.addCrossRefValidator( recordedDateNameValidator );
 	       
	        nameIterator =  (ConfigurableNameIterator)ModuleStatesIteratorFactory.getConfigurableNameIterator(m, searchId, new String[]{"L ; f;"});
			m.addIterator( nameIterator);
			l.add(m);
        	
        	
        }

        // buyer
        if( hasBuyer() ){
        	m = new TSServerInfoModule(serverInfo.getModule(TSServerInfo.NAME_MODULE_IDX));
        	DocsValidator recordedDateNameValidator = BetweenDatesFilterResponse.getDefaultIntervalFilter(searchId, m).getValidator();
 	        m.addExtraInformation(TSServerInfoConstants.EXTRA_PARAM_MODULE_DESCRIPTION, 
 	        		TSServerInfoConstants.VALUE_PARAM_NAME_OWNERS);
 	        m.setSaObjKey(SearchAttributes.BUYER_OBJECT);
 	        m.setSaKey(9, SearchAttributes.FROMDATE_MM_DD_YYYY);
	        m.setSaKey(10, SearchAttributes.TODATE_MM_DD_YYYY);
	        m.setIteratorType(9, FunctionStatesIterator.ITERATOR_TYPE_FROM_DATE);
 	        
		    m.addFilter( NameFilterFactory.getNameFilterIgnoreMiddleOnEmpty(SearchAttributes.BUYER_OBJECT, searchId, m) );
 	        m.addValidator( defaultLegalValidator );
	        m.addValidator( lastTransferDateValidator );
	        m.addValidator( addressHighPassValidator );
	        m.addValidator( pinValidator );
	        m.addValidator( subdivisionNameValidator );
	        m.addValidator( recordedDateNameValidator );
	        addFilterForUpdate(m, false);
	        m.addCrossRefValidator( defaultLegalValidator );
	        m.addCrossRefValidator( lastTransferDateValidator );
	        m.addCrossRefValidator( addressHighPassValidator );
	        m.addCrossRefValidator( pinValidator );
	        m.addCrossRefValidator( subdivisionNameValidator );
	        m.addCrossRefValidator( recordedDateNameValidator );
 	  
	        nameIterator =  (ConfigurableNameIterator)ModuleStatesIteratorFactory.getConfigurableNameIterator(m, searchId, new String[]{"L ; f;"});
			m.addIterator( nameIterator);
			l.add(m);
        }

        // search by book and page list from Search Page
        m = new TSServerInfoModule(serverInfo.getModule(TSServerInfo.BOOK_AND_PAGE_MODULE_IDX));
        m.addExtraInformation(TSServerInfoConstants.EXTRA_PARAM_MODULE_DESCRIPTION, 
        		TSServerInfoConstants.VALUE_PARAM_LIST_SEARCH_PAGE_BP);
        m.setSaObjKey(SearchAttributes.LD_BOOKPAGE);
        m.setIteratorType(ModuleStatesIterator.TYPE_BOOK_PAGE_LIST_SEARCH);
        m.getFunction(0).setSaKey("");
        m.getFunction(0).setIteratorType(FunctionStatesIterator.ITERATOR_TYPE_BOOK_SEARCH);
        m.getFunction(1).setSaKey("");
        m.getFunction(1).setIteratorType(FunctionStatesIterator.ITERATOR_TYPE_PAGE_SEARCH);
        m.addCrossRefValidator( defaultLegalValidator );
        m.addCrossRefValidator( addressHighPassValidator );
        m.addCrossRefValidator( pinValidator );
		if (validateWithDates) {
			m.addValidator(recordedDateValidator);
			m.addCrossRefValidator(recordedDateValidator);
		}
        l.add(m);

        // search by instrument number list from Search Page
        m = new TSServerInfoModule(serverInfo.getModule(TSServerInfo.INSTR_NO_MODULE_IDX));
        m.addExtraInformation(TSServerInfoConstants.EXTRA_PARAM_MODULE_DESCRIPTION, 
        		TSServerInfoConstants.VALUE_PARAM_LIST_SEARCH_PAGE_INSTR);
        m.setSaObjKey(SearchAttributes.LD_INSTRNO);
        m.setIteratorType(ModuleStatesIterator.TYPE_INSTRUMENT_LIST_SEARCH);
        m.getFunction(0).setSaKey("");
        m.getFunction(0).setIteratorType(FunctionStatesIterator.ITERATOR_TYPE_INSTRUMENT_LIST_SEARCH);
        m.addCrossRefValidator( defaultLegalValidator );
        m.addCrossRefValidator( addressHighPassValidator );
        m.addCrossRefValidator( pinValidator );
		if (validateWithDates) {
			m.addValidator(recordedDateValidator);
			m.addCrossRefValidator(recordedDateValidator);
		}
        l.add(m);
        
        //OCR search
        // search by book and page list from OCR
        m = new TSServerInfoModule(serverInfo.getModule(TSServerInfo.BOOK_AND_PAGE_MODULE_IDX));
        m.addExtraInformation(TSServerInfoConstants.EXTRA_PARAM_MODULE_DESCRIPTION, 
        		TSServerInfoConstants.VALUE_PARAM_OCR_SEARCH_BP);
        
        m.setIteratorType(ModuleStatesIterator.TYPE_OCR);
        m.getFunction(0).setSaKey("");
        m.getFunction(0).setIteratorType(FunctionStatesIterator.ITERATOR_TYPE_BOOK_SEARCH);
        m.getFunction(1).setSaKey("");
        m.getFunction(1).setIteratorType(FunctionStatesIterator.ITERATOR_TYPE_PAGE_SEARCH);
        m.addValidator(defaultLegalValidator);
        m.addCrossRefValidator(defaultLegalValidator);
		if (validateWithDates) {
			m.addValidator(recordedDateValidator);
			m.addCrossRefValidator(recordedDateValidator);
		}
        l.add(m);

        // search by instrument number list from OCR
        m = new TSServerInfoModule(serverInfo.getModule(TSServerInfo.INSTR_NO_MODULE_IDX));
        m.addExtraInformation(TSServerInfoConstants.EXTRA_PARAM_MODULE_DESCRIPTION, 
        		TSServerInfoConstants.VALUE_PARAM_OCR_SEARCH_INST);        
        m.getFunction(0).setSaKey("");
        m.getFunction(0).setIteratorType(FunctionStatesIterator.ITERATOR_TYPE_INSTRUMENT_LIST_SEARCH);
        m.addValidator(defaultLegalValidator);
        m.addCrossRefValidator(defaultLegalValidator);
		if (validateWithDates) {
			m.addValidator(recordedDateValidator);
			m.addCrossRefValidator(recordedDateValidator);
		}
        l.add(m);
        
        
        //OCR search end
        {
	    	m = new TSServerInfoModule(serverInfo.getModule(TSServerInfo.NAME_MODULE_IDX));
	    	DocsValidator recordedDateNameValidator = BetweenDatesFilterResponse.getDefaultIntervalFilter(searchId, m).getValidator();
	    	m.addExtraInformation(TSServerInfoConstants.EXTRA_PARAM_MODULE_DESCRIPTION, 
		        		TSServerInfoConstants.VALUE_PARAM_NAME_OWNERS);
		    m.setSaObjKey(SearchAttributes.OWNER_OBJECT);
		    m.setSaKey(9, SearchAttributes.FROMDATE_MM_DD_YYYY);
	        m.setSaKey(10, SearchAttributes.TODATE_MM_DD_YYYY);
	        m.setIteratorType(9, FunctionStatesIterator.ITERATOR_TYPE_FROM_DATE);
		    m.addFilter( NameFilterFactory.getDefaultNameFilter(SearchAttributes.OWNER_OBJECT, searchId, m) );
		    m.addValidator( defaultLegalValidator );
		    m.addValidator( lastTransferDateValidator );
		    m.addValidator( addressHighPassValidator );
		    m.addValidator( pinValidator );
		    m.addValidator( subdivisionNameValidator );
		    m.addValidator( recordedDateNameValidator );
		    
		    m.addCrossRefValidator( defaultLegalValidator );
		    m.addCrossRefValidator( lastTransferDateValidator );
		    m.addCrossRefValidator( addressHighPassValidator );
		    m.addCrossRefValidator( pinValidator );
		    m.addCrossRefValidator( subdivisionNameValidator );
		    m.addCrossRefValidator( recordedDateNameValidator );
		    
	        ArrayList< NameI> searchedNames = null;
			if( nameIterator!=null ){
				searchedNames = nameIterator.getSearchedNames();
			}
			else
			{
				searchedNames = new ArrayList<NameI>();
			}
			nameIterator = (ConfigurableNameIterator)ModuleStatesIteratorFactory.getConfigurableNameIterator( m, searchId, false, new String[]{"L;f;"} );
			//get your values at runtime
			nameIterator.setInitAgain( true );
			nameIterator.setSearchedNames( searchedNames );
			m.addIterator( nameIterator ); 
			l.add(m);
        }

        serverInfo.setModulesForAutoSearch(l);
    }
    
	public PinFilterResponse getPinFilter() {// 9237

		PinFilterResponse filter = new PinFilterResponse(SearchAttributes.LD_PARCELNO, searchId) {
			private static final long	serialVersionUID	= 1L;

			@Override
			protected Set<String> getRefPin() {
				if (parcelNumber != null) {
					return parcelNumber;
				}
				Set<String> ret = new HashSet<String>();
				String[] keys = sa.getAtribute(saKey).trim().split(",");
				ret.addAll(Arrays.asList(keys));

				for (String key : keys) {
					Pattern pattern = Pattern.compile("^(\\w+)[-\\s]\\w{0,2}[-\\s](\\w+).*");
					Matcher matcher = pattern.matcher(key);
					if (matcher.find()) {// ex. 056--056-047.04--000 -> --056-047.04--000
						if (matcher.group(1).equals(matcher.group(2))) {
							ret.add(key.replaceFirst(matcher.group(1), ""));
						}
						else {// ex. 076H A 04700 000 -> A 076H 04700 000
							ret.add(key.replaceFirst("^(\\w*)([-\\s])(\\w{0,2})([-\\s])", "$3$2$1$4")); 
						}
					}
				}
				parcelNumber = ret;
				return ret;
			}
		};
		filter.setStartWith(true);
		filter.setIgNoreZeroes(true);
		return filter;
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
			  		   	    	 
	         module = new TSServerInfoModule(serverInfo.getModule(TSServerInfo.NAME_MODULE_IDX));
	         module.setIndexInGB(id);
	         module.setTypeSearchGB("grantor");
	         module.clearSaKeys();
		     module.setSaObjKey(SearchAttributes.GB_MANAGER_OBJECT);
		     module.addFilter(NameFilterFactory.getDefaultNameFilter(SearchAttributes.GB_MANAGER_OBJECT, searchId, module));
		     String date=gbm.getDateForSearch(id, "MMddyy", searchId);
		     if (date!=null) 
		    	 module.getFunction(9).forceValue(date);
		     nameIterator =  (ConfigurableNameIterator)ModuleStatesIteratorFactory.getConfigurableNameIterator(module, searchId, new String[]{"L;f;"} );
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
				 module.addFilter(NameFilterFactory.getDefaultNameFilter(SearchAttributes.GB_MANAGER_OBJECT, searchId, module));
				 date=gbm.getDateForSearchBrokenChain(id, "MMddyy", searchId);
				 if (date!=null) 
					 module.getFunction(9).forceValue(date);
				 nameIterator =  (ConfigurableNameIterator)ModuleStatesIteratorFactory.getConfigurableNameIterator(module, searchId, new String[]{"L;f;"} );
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

    public TNWilsonRO(String rsRequestSolverName, String rsSitePath,
            String rsServerID, String rsPrmNameLink, long searchId, int mid) {
        super(rsRequestSolverName, rsSitePath, rsServerID, rsPrmNameLink, searchId, mid);
        resultType = MULTIPLE_RESULT_TYPE;
    }

    /**
     * @see TSInterface#GetLink(java.lang.String)
     */
    public ServerResponse GetLink(String vsRequest, boolean vbEncoded)
            throws ServerResponseException {
        ServerResponse rtrnResponse;
        String sTmp = TSServer.getParameter(msPrmNameLink, vsRequest);
        if (sTmp.indexOf(".php") == -1) {
            //try to get the file
            //request file
            getTSConnection().setHostName(IMAGES_SITE);
            getTSConnection().setHostIP(IMAGES_SITE_IP);
            rtrnResponse = super.GetLink(vsRequest, vbEncoded);
            getTSConnection().setHostName(msiServerInfo.getServerAddress());
            getTSConnection().setHostIP(msiServerInfo.getServerIP());
        } else
            rtrnResponse = super.GetLink(vsRequest, vbEncoded);
        return rtrnResponse;
    }

    /**
     * @param rsResponce
     * @param viParseID
     */
    protected void ParseResponse(String sAction, ServerResponse Response, int viParseID) throws ServerResponseException {
        String sTmp1;
        String sTmp;
        String sForm;
        String sFileLink = "";
        int iStart, iEnd;
        StringBuffer sBfr = new StringBuffer();
        String rsResponce = Response.getResult();
        String initialResponse = rsResponce;

        int iTmp, iTmp1;

        msServerID = "p1=068&p2=1";
        
        switch (viParseID) {
        case ID_SEARCH_BY_NAME:
        case ID_SEARCH_BY_ADDRESS:
        case ID_SEARCH_BY_INSTRUMENT_NO:
        case ID_SEARCH_BY_SUBDIVISION_NAME:
        case ID_SEARCH_BY_BOOK_AND_PAGE:
        case ID_DETAILS1: //parse search result
            rsResponce = rsResponce.replaceAll("TRUSTEE'S", "TRUSTEES");
            iTmp = rsResponce.indexOf("align=center border=");
            if (iTmp == -1) {
                return;
            }//no result

            if(rsResponce.matches("(?is).*NO\\s*HITS\\s*FOUND\\s*FOR.*")){		//fix for bug #1579
            	Response.getParsedResponse().setError("NO HITS FOUND.");
            	return;
            }
            if(rsResponce.matches("(?is).*No\\s*Instruments\\s*Were\\s*Found.*")){
                Response.getParsedResponse().setError("No Instruments Were Found.");
                return;
            }
            
            int endOf = rsResponce.lastIndexOf("</table>") + 8;
            if (endOf < iTmp){
            	Response.getParsedResponse().setError("Too much results. The server gave up. Please refine your search criteria.");
            	return;
            }
            sBfr.append("<table width=100"
                    + rsResponce.substring(iTmp, endOf).replaceAll("width=90%", ""));
            sTmp1 = "<FORM ACTION=";
            iTmp = sBfr.indexOf(sTmp1);
            
            while (iTmp > -1) {
                //while we still have a property details form then get it's
                // information
                //get from form until the imput information ends
            	iEnd = sBfr.indexOf("</form>", iTmp);
                sForm = sBfr.substring(iTmp, iEnd + "</form>".length());
                
                Matcher crossrefFindMatcher = crossrefDocTypePattern.matcher(sForm);
                String crossref = null;
                if (crossrefFindMatcher.find()){
                	crossref = crossrefFindMatcher.group(1);
                	sForm = sForm.replace(crossrefFindMatcher.group(0), "");
                } else{
                	Matcher recDateMatcher = recDatePattern.matcher(sForm);
                	if (recDateMatcher.find()){
                		crossref = recDateMatcher.group(1);
                	}
                }
                
                //create link
                sTmp = createLinkFromForm(sForm, crossref);
                sTmp = sTmp.replaceAll("/index.php" , "/newSearch/wilsondeeds_v2/index.php");
                sTmp = sTmp.replace(CreatePartialLink(TSConnectionURL.idPOST) , CreatePartialLink(TSConnectionURL.idGET));
                // sTmp=sTmp.replaceAll("/p4.php", "/ts/p4.php");
                // sTmp=sTmp.replaceAll("/pdetail.php", "/ts/pdetail.php");

                sForm = sForm.replaceAll("(?is)<form[^>]*>" , sTmp + (crossref.contains("/") ? "" : "<br>"));
                sForm = sForm.replaceAll("(?is)<input[^>]*>" , "");
                sForm = sForm.replaceAll("(?is)</form>" , "");
                
                sBfr.replace(iTmp, iEnd + "</form>".length(), sForm);

                //go to next form
                iTmp = sBfr.indexOf(sTmp1, iTmp);
            }
            
            String linkStart = CreatePartialLink(TSConnectionURL.idGET);;
            ////////View Image link fix
            rsResponce = sBfr.toString().replaceAll("</form>", "");
            
            rsResponce = rsResponce.replace( "</td></td>" , "</td>" );
            
            rsResponce = rsResponce.replaceAll("(?is)<A HREF=imgview.php\\?([^>]*)\\s+target\\s*=\\s*'_blank'\\s*>", 
            					"<A HREF='" + linkStart + "/newSearch/wilsondeeds_v2/imgview.php&$1'>");
            rsResponce = rsResponce.replaceAll("(?is)(&type=pdf[\\\"|'])>", "$1 target=\"_blank\">");

            ///////////////////
            linkStart = getLinkPrefix(TSConnectionURL.idGET);
            
            rsResponce = Tidy.tidyParse(rsResponce, null);
            rsResponce = rsResponce.replaceAll("(?is)</?html>", "").replaceAll("(?is)</?body>", "").replaceAll("(?is)<head>.*?</head>", "");
            rsResponce = rsResponce.replaceAll("(?is)</?table[^>]*>", "");
            rsResponce = "<table>" + rsResponce + "</table>";
            rsResponce = rsResponce.replaceAll("(?is)<font[^>]*>\\s*</font>", "");
            try {
				 
				StringBuilder outputTable = new StringBuilder();
				ParsedResponse parsedResponse = Response.getParsedResponse();
																	
				Collection<ParsedResponse> smartParsedResponses = smartParseIntermediary(Response, rsResponce, outputTable);
				
				if(smartParsedResponses.size() > 0) {
					parsedResponse.setResultRows(new Vector<ParsedResponse>(smartParsedResponses));
					parsedResponse.setOnlyResponse(outputTable.toString());
	            }
				
			} catch(Exception e) {
				e.printStackTrace();
			}
            break;
       
        case ID_BROWSE_SCANNED_INDEX_PAGES: 
            iStart = rsResponce.indexOf("SCANNED INDEX PAGE LOOKUP");
            if(iStart < 0)
                return;
            //rsResponce = rsResponce.replaceFirst("(?is)\\b(name=\\\"type\\\")", "$1 disabled");
            rsResponce = "<center>" + rsResponce.substring(iStart);
            rsResponce = rsResponce.replaceAll( "<A href=\"index.php\\?" , "<A href=\"" + CreatePartialLink(TSConnectionURL.idGET) + "/newSearch/wilsondeeds_v2/index.php&" );
            if(rsResponce.indexOf("imgview.php") != -1) {
                rsResponce = rsResponce.replaceAll("(imgview.php)\\?(img=/img/index/[^/]*/[^\\.]+.tif)", CreatePartialLink(TSConnectionURL.idGET) + "/newSearch/wilsondeeds_v2/$1&$2");

                rsResponce = rsResponce.replaceAll("<A target=\"NEW\" href=\"([^\"]*imgview.php&img=/img/index/[^/]*/)([^\\.]+)(.tif&type=)\">([^<]*)</A>", 
                        "<input type=\"checkbox\" name=\"$2\" value=\"$1$2$3\"> <A target=\"NEW\" href=\"$1$2$3\">$4</A>");

            }
            rsResponce = rsResponce.replaceFirst("onChange=\"this\\.form\\.submit\\(\\);\"", "");
            rsResponce = rsResponce.replaceAll("</?form.*?>", "");
            
            rsResponce = "<form name=\"savetiff\" action=\"/title-search/MultiDocSave\" method=\"POST\">" + rsResponce.substring(0, rsResponce.indexOf("</body>"))
				+ "<input name=\"Button\" type=\"button\" class=\"button\" value=\"" + SAVE_SELECTED_DOCUMENTS_BUTTON_LABEL + "\" onClick=\"window.document.savetiff.submit();\">" 
				+ "<input type=\"hidden\" name=\"searchId\" value=\"" + searchId +"\"/>"
				+ "<input type=\"hidden\" name=\"serverId\" value=\"68\"/>"
				+ "<input type=\"hidden\" name=\"searchType\" value=\"" + DocumentTypes.MISCELLANEOUS + "\"/>"
				+ " </form></body></html>";
            
            parser.Parse(Response.getParsedResponse(), rsResponce,
                    Parser.NO_PARSE);
            break;
            
        case ID_BROWSE_BACKSCANNED_DEEDS: 
            
        	iStart = rsResponce.indexOf("Backscanned Deed Viewer");
            if(iStart < 0)
                return;
            
            sTmp = CreatePartialLink(TSConnectionURL.idPOST);
            //rsResponce = rsResponce.replaceFirst("(?is)\\b(name=\\\"type\\\")", "$1 disabled");
            rsResponce = "<center><h2>" + rsResponce.substring(iStart, rsResponce.lastIndexOf("</table>") + 8);
            
            rsResponce = rsResponce.replaceFirst("onChange=\"this\\.form\\.submit\\(\\);\"", "");
            //rsResponce = rsResponce.replaceAll("</?form.*?>", "");
            rsResponce = rsResponce.replaceAll("<form method=\\\"get\\\" action=\\\"#\\\">", "");
            // get PageForm
            iStart = rsResponce.indexOf( "<form action=\"deedviewer.php\" method=\"post\" name=\"deed\">" );
            boolean hasPageSelection = iStart > -1;
            HashMap<String,String> pageParameters = new HashMap<String,String>();
            if ( hasPageSelection ) {
            	String pageForm = rsResponce.substring( iStart, rsResponce.indexOf( "</form>", iStart ) );
            	pageParameters = HttpUtils.getFormParams( pageForm );
            	//directory, book
            }
            
            // get BookForm
            iStart = rsResponce.indexOf( "<form action=\"\" method=\"post\" name=\"book\">" );
            boolean hasBookSelection = iStart > -1;
            HashMap<String,String> bookParameters = new HashMap<String,String>(); 
            if ( hasBookSelection ) {
            	String bookForm = rsResponce.substring( iStart, rsResponce.indexOf( "</form>", iStart ) );
            	bookParameters = HttpUtils.getFormParams( bookForm );
            	//folder, path, bookcode
            }
            
            // clean redundant html code
            rsResponce = rsResponce.replaceAll("(?s)<script>.*?</script>", ""); 
            rsResponce = rsResponce.replaceAll("</?form.*?>", "");
//            rsResponce = rsResponce.replaceAll("</?select.*?>", "");
//            rsResponce = rsResponce.replaceAll("<option>.*?</option>", "");
            rsResponce = rsResponce.replaceAll("<input.*?/>", "");

            // show controls
            if (hasPageSelection){
            	
            	String book = "";
            		
            	// selected controls
            	Pattern bookPattern = Pattern.compile("<option .*?selected ='selected'>([A-Z\\d]+)</option>");
            	Matcher bookMatcher = bookPattern.matcher(rsResponce);
            	if (bookMatcher.find())
            		book = bookMatcher.group(1);
            	
            	rsResponce = rsResponce.replaceAll("<option>-= Pages =-</option>", "<BR><BR>Select Page : <BR><BR>");
            	rsResponce = rsResponce.replaceAll("<option .*?selected ='selected'>([^<]*)</option>", "<b>$1</b><BR>");
            	rsResponce = rsResponce.replaceAll("(?is)<select name=\\\"image\\\"[^>]*>", "");
            	
            	// delete all book options
            	rsResponce = rsResponce.replaceAll("<option value\\s*=(\\d+).*?>(\\d+)</option>", "");
            	// create link for all pages
            	rsResponce = rsResponce.replaceAll("<option value\\s*=(\\d+)[^>]*>(\\d+\\.\\d+)</option>(?:\\s*</select>)?", 
            			"<input type=\"checkbox\" " +
            				"name=\"" + book + "-$1\" " +
            				"value=\"" + sTmp + "/newSearch/wilsondeeds_v2/viewdeed.php&image=$2&directory=" + pageParameters.get("directory") + "&type=\">" +
            			"<A HREF=\"" + sTmp + "/newSearch/wilsondeeds_v2/viewdeed.php&image=$2&" +
                				"directory=" + pageParameters.get("directory") +
                				"\">$2</A><BR/>");
            	
            	rsResponce = rsResponce.replaceAll("(?is)<select name=\\\"book\\\"[^>]*>", "");
            	rsResponce = rsResponce.replaceAll("(?is)<option>-= Books =-</option>", "Book:<br>");
            	
            	rsResponce = rsResponce.replaceAll("(?is)<select name=\\\"bookcode\\\"[^>]*>", "");
            	rsResponce = rsResponce.replaceAll("(?is)<option>(-= Please Select =-)</option>", "");
            	rsResponce = rsResponce.replaceAll("<option value\\s*=(\\d+|trus)[^>]*>(.*?)</option>(?:\\s*</select>)?", "");
            } else if (hasBookSelection){
            	
            	rsResponce = rsResponce.replaceAll("<option .*?selected ='selected'>([^<]*)</option>", "<b>$1</b><BR>");
            	
            	rsResponce = rsResponce.replaceAll("<option>-= Books =-</option>", "<BR><BR>Select Book : <BR><BR>");
            	
            	rsResponce = rsResponce.replaceAll("(?is)<select name=\\\"book\\\"[^>]*>", "");
            	rsResponce = rsResponce.replaceAll("(?is)<option value\\s*=([A-Z\\d]{5,})[^>]*>([A-Z\\d]+)</option>(?:\\s*</select>)?", 
            		"<A HREF=\"" + sTmp + "/newSearch/wilsondeeds_v2/deedlookup.php&op=deedlookup&book=$1&" +
            				"folder=" + bookParameters.get("folder") + "&" +
            				"path=" + bookParameters.get("path") + "&" +
            				"bookcode=" + bookParameters.get("bookcode") +
            				"\">$2</A><BR/>");
            	
            	rsResponce = rsResponce.replaceAll("(?is)<select name=\\\"bookcode\\\"[^>]*>", "");
            	rsResponce = rsResponce.replaceAll("(?is)<option>(-= Please Select =-)</option>", "");
            	rsResponce = rsResponce.replaceAll("<option value\\s*=(\\d+|trus)[^>]*>(.*?)</option>(?:\\s*</select>)?", "");
            } else{
            	rsResponce = rsResponce.replaceAll("(?is)<select name=\\\"bookcode\\\"[^>]*>", "");
            	rsResponce = rsResponce.replaceAll("(?is)<option>(-= Please Select =-)</option>", "$1<br>");
            	rsResponce = rsResponce.replaceAll("(?is)<option value\\s*=([A-Z\\d]+).*?>(.*?)</option>(?:\\s*</select>)?", 
            		"<A HREF=\"" + sTmp + "/newSearch/wilsondeeds_v2/index.php&op=deedlookup&bookcode=$1\">$2</A><br>");
            }
            
            
            if (hasPageSelection){
	            rsResponce = "<form name=\"savetiff\" action=\"/title-search/MultiDocSave\" method=\"POST\">" + rsResponce.substring(0, rsResponce.indexOf("</table>") + 8)
					+ "<input name=\"Button\" type=\"button\" class=\"button\" value=\"" + SAVE_SELECTED_DOCUMENTS_BUTTON_LABEL + "\" onClick=\"getSelectedImageType();window.document.forms[0].submit();\">" 
					+ "<input type=\"hidden\" name=\"searchId\" value=\"" + searchId +"\">"
					+ "<input type=\"hidden\" name=\"serverId\" value=\"68\"/>"
					+ "<input type=\"hidden\" name=\"searchType\" value=\"DEED\"/>"
					+ "<input type=\"hidden\" name=\"image_type\" value=\"\"/>"
					+ " </form>"
					+ "<script>function getSelectedImageType() { \n"
					+ "var imageTypeSelect = document.getElementById(\"type\"); \n"
					+ "if (imageTypeSelect.selectedIndex != -1){\n"
					+ "var selectedImageType = imageTypeSelect.options[imageTypeSelect.selectedIndex].text;\n"
					+ "document.getElementsByName('image_type')[0].value= selectedImageType;"
					+ "}\n}"
					+ "</script>"
					+ "</body></html>";
            }
            
            parser.Parse(Response.getParsedResponse(), rsResponce,
                    Parser.NO_PARSE);
            break;
        
        case ID_BROWSE_BACKSCANNED_PLATS:
            iStart = rsResponce.indexOf("Backscanned Plat Viewer");
            if(iStart < 0)
                return;
            
            //rsResponce = rsResponce.replaceFirst("(?is)\\b(name=\\\"type\\\")", "$1 disabled");
            String bookNo = "";
            rsResponce = "<center><h2>" + rsResponce.substring(iStart, rsResponce.lastIndexOf("</table>") + 8);
            rsResponce = rsResponce.replaceAll("<script language=[^<]+</script>", ""); 
            rsResponce = rsResponce.replaceAll("<form action=\"plathold.php\" method=\"GET\">", "");
            rsResponce = rsResponce.replaceAll("(?is)<\\s*form\\s+action\\s*=\\s*\\\"\\\"\\s+method\\s*=\\s*\\\"GET\\\"\\s*>", "");
            sTmp = CreatePartialLink(TSConnectionURL.idPOST);
            iEnd = rsResponce.indexOf("SELECTED");
            if(iEnd != -1) {
                bookNo = rsResponce.substring(iEnd - 12, iEnd + 1);
                bookNo = StringUtils.getTextBetweenDelimiters("=\"", "\" S", bookNo);
            }

            iStart = rsResponce.indexOf( "<select onChange=\"this.form.submit()\" name=\"book\">" );
            iEnd = rsResponce.indexOf( "</select>" , iStart );
            String bookSelect = rsResponce.substring( iStart , iEnd );
            Matcher optionMatcher = Pattern.compile( "(?is)<option value=\"([^\"]*)\"[^>]*>([^<]*)</option>" ).matcher( bookSelect );
            while(optionMatcher.find()){
            	if("-1".equals(optionMatcher.group(1))){
            		bookSelect = bookSelect.replace(optionMatcher.group(0) , "");            		
            	}
            	else{
            		bookSelect = bookSelect.replace(optionMatcher.group(0), 
            						"<A HREF=\"" + sTmp + "/newSearch/wilsondeeds_v2/index.php&op=plathold&submit=Submit+Query&book=" + optionMatcher.group(1) + "&page=-1\">" + optionMatcher.group(2) + "</A>&nbsp;&nbsp;");
            	}
            }
            
            rsResponce = rsResponce.replaceFirst("(?is)<select onChange=\\\"this\\.form\\.submit\\(\\)\\\" name=\\\"book\\\">.*?</select>", bookSelect);
            
            rsResponce = rsResponce.replaceAll( "<select[^>]*>" , "" );
            rsResponce = rsResponce.replaceAll( "</select>" , "" );            
            
            rsResponce = rsResponce.replaceAll("<select\\s*name=\"page\" onChange=.*>\\s*<option[^<]+</option>", "");
            rsResponce = rsResponce.replaceAll("(?is)<\\s*[/]?form[^>]*>", "");
            // http://search.wilsondeeds.com/imgview.php?img=/img/plathold/23-004.tif
            rsResponce = rsResponce.replaceAll("<option value[^>]*>(\\w+|\\s*)</option>", "&nbsp;<input type=\"checkbox\" name=\"" +bookNo + "-$1" + "\" value=\"" + CreatePartialLink(TSConnectionURL.idGET) + "/newSearch/wilsondeeds_v2/imgview.php?img=/img/plathold/" + bookNo + "-$1.tif\"><A target=\"NEW\" HREF=\"" + CreatePartialLink(TSConnectionURL.idGET) + "/newSearch/wilsondeeds_v2/imgview.php?img=/img/plathold/" + bookNo + "-$1.tif\">" + bookNo + "-$1</A><BR>");
            
            rsResponce = "<form name=\"savetiff\" action=\"/title-search/MultiDocSave\" method=\"POST\">" + rsResponce.substring(0, rsResponce.indexOf("</table>") + 8)
			+ "<input name=\"Button\" type=\"button\" class=\"button\" value=\"" + SAVE_SELECTED_DOCUMENTS_BUTTON_LABEL +"\" onClick=\"window.document.forms[0].submit();\">" 
			+ "<input type=\"hidden\" name=\"searchId\" value=\"" + searchId +"\"/>"
			+ "<input type=\"hidden\" name=\"serverId\" value=\"68\"/>"
			+ "<input type=\"hidden\" name=\"searchType\" value=\"PLAT\"/>"
			+ " </form></body></html>";
			
            parser.Parse(Response.getParsedResponse(), rsResponce,
                    Parser.NO_PARSE);
            break;
        case ID_DETAILS2:
        	//http://www.wilsondeeds.com/newSearch/wilsondeeds_v2/imgview.php?img=/img/2002/1127/02172550.tif
        	
        	String documentTypeFromResponse =  StringUtils.parseByRegEx(rsResponce, "(?<=Doc Type:)(.*?)</FONT>", 1).trim();
        	rsResponce = rsResponce.substring(rsResponce.indexOf("Detail Information For ") + 23);
        	sBfr.append(rsResponce.substring(0, rsResponce.indexOf("</a>")));
        	//test if we have any document image
        	iTmp = rsResponce.indexOf("imgview.php");
        	if (iTmp > -1) {
        		//iTmp += IMAGES_SITE.length();
        		sTmp1 = rsResponce.substring(iTmp, rsResponce.indexOf(" target", iTmp));
	            //sFileLink= CreateLink("View Image", sTmp1,
        		// TSConnectionURL.idGET);
        		sFileLink = "<A target=\"_blank\" HREF='"
        				+ CreatePartialLink(TSConnectionURL.idGET)
        				+ "/newSearch/wilsondeeds_v2/" +sTmp1.replaceAll("\\?", "&") + "'> View Image </a>";
	            sBfr.append("<br>" + sFileLink);
        	}
	            
        	iTmp = rsResponce.indexOf("<table width=95%");
        	if( iTmp >= 0 ){
        		rsResponce = rsResponce.substring( iTmp );
        	}
        	//rsResponce= rsResponce.substring(rsResponce.indexOf("<table
        	// width"));
        	if (rsResponce.indexOf("<!-- START: NOTICES -->") > -1){
        		sBfr.append(rsResponce.substring(0, rsResponce.indexOf("<!-- START: NOTICES -->")));
        	} else{
        		sBfr.append(rsResponce);
        	}
        	//test if we have forms
        	iTmp = sBfr.indexOf("<FORM ");
        	while (iTmp > -1) {
        		//while we still have a property details form then convert it
        		// in link
        		//get from form until the imput information ends
        		iTmp1 = sBfr.indexOf("</form>", iTmp);
        		if (iTmp1 == -1)
        			iTmp1 = sBfr.indexOf("</td>", iTmp);
        		else
        			iTmp1 += 7;
        		sForm = sBfr.substring(iTmp, iTmp1);
        		// sForm=sForm.replaceAll("pdetail.php", "ts/pdetail.php");
        		//replace form with a link
	                
        		String linkRepl = createLinkFromForm(sForm, "View").replace("&=LIST", "&x=4&y=7");
	                
        		linkRepl = linkRepl.replaceAll( "/index.php" , "/newSearch/wilsondeeds_v2/index.php");
	                
        		sBfr.replace(iTmp, iTmp1, "<td>" + linkRepl);
        		iTmp = sBfr.indexOf("<FORM ");
        	}
	
        	rsResponce = "<hr>" + sBfr.toString().replaceAll("<FONT[a-zA-Z0-9/=,_\\<\\?\\.'\\s\\\"\\r\\n]+>", "");
        	rsResponce = rsResponce.replaceAll("<img[a-zA-Z0-9/=,_\\<\\?\\.'\\s\\\"\\r\\n]+>", "");
        	rsResponce = rsResponce.replaceAll("align=center", "");
        	rsResponce = rsResponce.replaceAll("</FONT>", "");
        	rsResponce = rsResponce.replaceAll("bgcolor=BLACK", "");
        	rsResponce = rsResponce.replaceAll("<TEXTAREA[a-zA-Z0-9/=,_\\<\\?\\.'\\s\\\"\\r\\n]+>", "");
        	rsResponce = rsResponce.replaceAll("</textarea>", "");
        	rsResponce = rsResponce.replaceAll( "(?is)<!--.*?-->", "" );
        	rsResponce = rsResponce.replace( "<hr class=\"hide\" />" , "");
        	
        	//put link for pdf image also
        	rsResponce = rsResponce.replaceFirst("(?is)(HREF='[^']+)('>\\s*View Image)", "$1$2 as TIFF</a>&nbsp;|&nbsp;<A target=\"_blank\" $1&type=pdf$2 as PDF</a>");
	            
        	iTmp = rsResponce.indexOf("align=left", rsResponce.lastIndexOf("<table"));
        	if (iTmp > -1){
        		rsResponce = rsResponce.substring(0, iTmp) + rsResponce.substring(iTmp + 11);
        	}
        	
            //find Instrument No. in html
            String instrNo = getInstrNoFromResponse(rsResponce);
            //find Year in html
            String year = getYearFromResponse(rsResponce);

            String docName = instrNo /*+ "_" + year*/;
            String docFullName = docName + ".html";
            
            //download view file if any;
            if (!(sFileLink.equals(""))) {
                //logger.debug(" sFileLink = " + sFileLink );

                String imageName = StringUtils.getTextBetweenDelimiters("&img=", "'>", sFileLink);
                String fileExt = StringUtils.getTextBetweenDelimiters(".", "'>", imageName).toLowerCase();
                String imageFileName = docName + "." + fileExt;

                String link = StringUtils.getTextBetweenDelimiters("HREF='", "'>", sFileLink);

                Response.getParsedResponse().addImageLink(new ImageLinkInPage(link, imageFileName));

                //replace link with local link
                //String localLink = StringUtils.replaceFirstBetweenTags(sFileLink, "='","'>", imageFileName);
                //rsResponce= StringUtils.replaceFirstSubstring(rsResponce, sFileLink, localLink);
            }
            
            if (!downloadingForSave) {

                String originalLink = getOriginalLink(instrNo, year);
                String sSave2TSDLink = getLinkPrefix(TSConnectionURL.idPOST) + originalLink;
                
                HashMap<String, String> data = new HashMap<String, String>();
				data.put("type", documentTypeFromResponse);
               
                if (isInstrumentSaved(instrNo, null, data)) {
                    rsResponce += "<br><br><br><br><br><br><br><br>" + CreateFileAlreadyInTSD();
                } else {
                    //replace any existing form end
                    rsResponce = rsResponce.replaceAll("</FORM>", "");
                    mSearch.addInMemoryDoc(sSave2TSDLink, initialResponse);
                    rsResponce = addSaveToTsdButton(rsResponce, sSave2TSDLink, viParseID);
                    // adjusting TSD button
                    int i = rsResponce.lastIndexOf(SAVE_DOCUMENT_BUTTON_LABEL);
                    i = rsResponce.lastIndexOf("<input", i);
                    rsResponce = rsResponce.substring(0, i)
                            + "<br><br><br><br><br><br><br><br>"
                            + rsResponce.substring(i);
                }
                Response.getParsedResponse().setPageLink(
                        new LinkInPage(sSave2TSDLink, getOriginalLink(instrNo, year), TSServer.REQUEST_SAVE_TO_TSD));
                parser.Parse(Response.getParsedResponse(), rsResponce, Parser.NO_PARSE);
            } else { //saving
                msSaveToTSDResponce = rsResponce + CreateFileAlreadyInTSD();
                msSaveToTSDFileName = docFullName;

                Response.getParsedResponse().setFileName(getServerTypeDirectory() + msSaveToTSDFileName);
                //remove cross ref links
                rsResponce = rsResponce.replaceAll("<td><A HREF=[a-zA-Z0-9/=&,\\-_\\(\\)\\<\\?\\.\\>'\\s\\\"\\r\\n]+</a>", "<td>");
                
                smartParseDetails(Response, rsResponce);
                
                Response.getParsedResponse().setOnlyResponse(rsResponce);
                //logger.debug(" is unique" + Response.getParsedResponse().isUnique());
            }
            break;
        case ID_GET_LINK:
            if (Response.getQuerry().indexOf("op=scannedIndexes") != -1)
                ParseResponse(sAction, Response, ID_BROWSE_SCANNED_INDEX_PAGES);
            if (Response.getQuerry().indexOf("op=plathold") != -1)
                ParseResponse(sAction, Response, ID_BROWSE_BACKSCANNED_PLATS);
            if (Response.getQuerry().indexOf("op=deedlookup") != -1)
                ParseResponse(sAction, Response, ID_BROWSE_BACKSCANNED_DEEDS);
            if (Response.getQuerry().contains( "op=pdetail" )) {
                msLastdb = msLastLink.substring(msLastLink.indexOf("&db=") + 4);
                ParseResponse(sAction, Response, ID_DETAILS2);
            } else
                ParseResponse(sAction, Response, ID_DETAILS1);
            break;
        case ID_SAVE_TO_TSD:
            downloadingForSave = true;
            ParseResponse(sAction, Response, ID_DETAILS2);
            downloadingForSave = false;
            break;
        default:
            break;
        }
    }

    public Collection<ParsedResponse> smartParseIntermediary(ServerResponse response, String table, StringBuilder outputTable) {
		LinkedHashMap<String, ParsedResponse> responses = new LinkedHashMap<String, ParsedResponse>();
		Vector<ParsedResponse> intermediaryResponse = new Vector<ParsedResponse>();
		
		Search search = this.getSearch();
		searchId = search.getID();
		
		/**
		 * We need to find what was the original search module
		 * in case we need some info from it like in the new PS interface
		 */
		TSServerInfoModule moduleSource = null;
		Object objectModuleSource = response.getParsedResponse().getAttribute(
				TSServerInfoConstants.EXTRA_PARAM_MODULE_OBJECT_SOURCE);
		if(objectModuleSource != null) {
			if(objectModuleSource instanceof TSServerInfoModule) {
				moduleSource = (TSServerInfoModule) objectModuleSource;
			} 
		} else {
//			objectModuleSource = search.getAdditionalInfo(
//					this.getKeyForSavingInIntermediaryNextLink(response.getQuerry()));
//			if (objectModuleSource instanceof TSServerInfoModule) {
//				moduleSource = (TSServerInfoModule) objectModuleSource;
//			}
		}
		
		try {
			table = table.replaceAll("(?is)(<td[^>]*>)\\s*(</td>)", "$1&nbsp;$2");
			
			int numberOfUncheckedElements = 0;
			StringBuilder newTable = new StringBuilder();
			newTable.append("<table BORDER='1' CELLPADDING='2'>");
			String tableheader = "";
			boolean isNameSearch = false;
			
			if (StringUtils.isNotEmpty(table)){
				org.htmlparser.Parser tableParser = org.htmlparser.Parser.createParser(table, null);
				NodeList mainTableList = tableParser.parse(new TagNameFilter("table"));
				if (mainTableList != null && mainTableList.size() > 0){
					TableTag mainTable = (TableTag) mainTableList.elementAt(0);		
					if (mainTable != null){
						String oneParty = "";
						
						TableRow[] rows = mainTable.getRows();
						if (rows != null){
							for (int i = 0; i < rows.length; i++){
								TableRow row = rows[i];
								TableColumn[] tc = row.getColumns();
									
								if (tc != null && tc.length == 1){
									String cell = tc[0].toPlainTextString();
									cell = cell.replaceAll("(?is)&nbsp;", " ");
									if (org.apache.commons.lang.StringUtils.isEmpty(cell.trim())){
										continue;
									}
									oneParty = tc[0].toHtml();
									oneParty = oneParty.replaceAll("(?is)\\bcolspan=\\\"\\d+\\\"", "");
									continue;
								} else if (tc != null && tc.length == 8 && tc[1].toPlainTextString().contains("Book")){
									if (org.apache.commons.lang.StringUtils.isEmpty(tableheader)){
										tableheader = row.toHtml();
										tableheader = tableheader.replaceFirst("(?is)(<td[^>]*>\\s*(?:<font[^>]*>)?)\\s*(Reverse\\s+Party)\\s*((?:</font>)?\\s*</td>)",
																			"$1Party$3$1$2$3");
										if (tableheader.toLowerCase().contains("reverse party")){
											isNameSearch = true;
										}
									}
									continue;
								} else if (tc != null && tc.length == 2){
									continue;
								}
									
								if (tc != null && tc.length == 8){
									StringBuffer rowBuff = new StringBuffer();
									
									String recDate = HtmlParser3.getValueFromCell(tc[0], "", false);
									recDate = recDate.replaceAll("(?is)&nbsp;", " ").trim();
									rowBuff.append(tc[0].toHtml());
										
									String book = HtmlParser3.getValueFromCell(tc[1], "", false);
									book = book.replaceAll("(?is)&nbsp;", " ").trim();
									rowBuff.append(tc[1].toHtml());
										
									String page = HtmlParser3.getValueFromCell(tc[2], "", false);
									page = page.replaceAll("(?is)&nbsp;", " ").trim();
									rowBuff.append(tc[2].toHtml());
										
									String docType = HtmlParser3.getValueFromCell(tc[3], "", false);
									docType = docType.replaceAll("(?is)&nbsp;", " ").trim();
									rowBuff.append(tc[3].toHtml());
																				
									String grantors = "";
									String grantees = "";
									
									if (isNameSearch){
										NodeList nl = tc[4].getChildren().extractAllNodesThatMatch(new TagNameFilter("font"), true);
										if (nl != null && nl.size() > 0){
											Tag font = (Tag) nl.elementAt(0);
												
											if ("red".equals(font.getAttribute("color").toLowerCase())){
												grantors = HtmlParser3.getValueFromCell(tc[4], "", true);
												grantees = oneParty;
													
												rowBuff.append(oneParty);
												rowBuff.append(tc[4].toHtml());
											} else{
												grantees = HtmlParser3.getValueFromCell(tc[4], "", true);
												grantors = oneParty;
	
												rowBuff.append(tc[4].toHtml());
												rowBuff.append(oneParty);
											}
										} else{
											grantors = HtmlParser3.getValueFromCell(tc[4], "", true);
											grantees = oneParty;
												
											rowBuff.append(oneParty);
											rowBuff.append(tc[4].toHtml());
										}
									} else{
										grantors = HtmlParser3.getValueFromCell(tc[4], "", true);
										rowBuff.append(tc[4].toHtml());
										
										grantees = HtmlParser3.getValueFromCell(tc[5], "", true);
										rowBuff.append(tc[5].toHtml());
									}
									
									grantors = grantors.replaceAll("(?is)</?td[^>]*>", "").replaceAll("(?is)</?font[^>]*>", "").replaceAll("(?is)</?b[^>]*>", "").replaceAll("(?is)&nbsp;", "");
									grantees = grantees.replaceAll("(?is)</?td[^>]*>", "").replaceAll("(?is)</?font[^>]*>", "").replaceAll("(?is)</?b[^>]*>", "").replaceAll("(?is)&nbsp;", "");
									
									String legalDesc = "";
									
									if (isNameSearch){
										legalDesc = HtmlParser3.getValueFromCell(tc[5], "", true);
										rowBuff.append(tc[5].toHtml());
										legalDesc = legalDesc.replaceAll("(?is)&nbsp;", " ").replaceAll("(?is)<br/?>", " ").replaceAll("(?is)</?font[^>]*>", " ");
										legalDesc = legalDesc.trim();
										
									}
									rowBuff.append(tc[6].toHtml());
									rowBuff.append(tc[7].toHtml());
									
									if (i < rows.length - 1){
										TableRow nextRow = rows[i + 1];
										TableColumn[] tcn = nextRow.getColumns();
										if (tcn.length == 2){
											legalDesc = HtmlParser3.getValueFromCell(tcn[1], "", true);
											legalDesc = legalDesc.replaceAll("(?is)&nbsp;", " ").replaceAll("(?is)<br/?>", " ").replaceAll("(?is)</?font[^>]*>", " ");
											rowBuff.append(nextRow.toHtml());
										}
									}
																				
									String key = book + "_" + page + "_" + docType.replaceAll("\\s+", "_");
									
									ParsedResponse currentResponse = responses.get(key);							 
									if(currentResponse == null) {
										currentResponse = new ParsedResponse();
										responses.put(key, currentResponse);
									}
									
									RegisterDocumentI document = (RegisterDocumentI)currentResponse.getDocument();				
									
									ResultMap resultMap = new ResultMap();
									
									String link = HtmlParser3.getFirstTag(tc[0].getChildren(), LinkTag.class, true).getLink();	
										
									if (document == null){
										String rowHtml =  rowBuff.toString();
										
										resultMap.put(PropertyIdentificationSetKey.PROPERTY_DESCRIPTION.getKeyName(), legalDesc);
										
										List<List> pisBody = new ArrayList<List>();
										String[] header= new String[]{"SubdivisionName", "SubdivisionLotNumber", "SubdivisionSection", "ParcelID", "SubdivisionUnit", "District"};
										ResultTable rt = new ResultTable();
										rt = GenericFunctions2.createResultTable(pisBody, header);
										resultMap.put("PropertyIdentificationSet", rt);
										
										resultMap.put(SaleDataSetKey.GRANTOR.getKeyName(), grantors);
										resultMap.put(SaleDataSetKey.GRANTEE.getKeyName(), grantees);
										resultMap.put(OtherInformationSetKey.SRC_TYPE.getKeyName(), "RO");
										resultMap.put(SaleDataSetKey.BOOK.getKeyName(), org.apache.commons.lang.StringUtils.stripStart(book, "0"));
										resultMap.put(SaleDataSetKey.PAGE.getKeyName(), org.apache.commons.lang.StringUtils.stripStart(page, "0"));
											
										resultMap.put(SaleDataSetKey.DOCUMENT_TYPE.getKeyName(), docType);
										resultMap.put(SaleDataSetKey.RECORDED_DATE.getKeyName(), recDate.trim());
										try {
											ro.cst.tsearch.servers.functions.TNWilsonRO.parseName(resultMap, searchId);
											ro.cst.tsearch.servers.functions.TNWilsonRO.parseLegal(resultMap, searchId);
										} catch (Exception e) {
											e.printStackTrace();
										}
										resultMap.removeTempDef();
								    				
										currentResponse.setAttribute(ParsedResponse.SERVER_ROW_RESPONSE, rowHtml);
																					
										Bridge bridge = new Bridge(currentResponse,resultMap,getSearch().getID());
										document = (RegisterDocumentI) bridge.importData();
													
										currentResponse.setDocument(document);
										String checkBox = "checked";
										HashMap<String, String> data = new HashMap<String, String>();
										data.put("type", docType);
										data.put("book", book);
										data.put("page", page);
													
										if (isInstrumentSaved(" ", null, data) && !Boolean.TRUE.equals(getSearch().getAdditionalInfo("RESAVE_DOCUMENT"))) {
											checkBox = "saved";;
										} else {
											numberOfUncheckedElements++;
											LinkInPage linkInPage = new LinkInPage(link, link, TSServer.REQUEST_SAVE_TO_TSD);
											currentResponse.setPageLink(linkInPage);
											checkBox = "<input type=\"checkbox\" name=\"docLink\" value=\"" + link + "\">";
											/**
											 * Save module in key in additional info. The key is instrument number that should be always available. 
											 */
//											String keyForSavingModules = this.getKeyForSavingInIntermediary(instr);
//											search.setAdditionalInfo(keyForSavingModules, moduleSource);
										}

										//mSearch.addInMemoryDoc(link, rowHtml);
										
										rowHtml = "<td width=\"2%\">" + checkBox + "</td>" + rowHtml;											
										currentResponse.setOnlyResponse("<tr>" + rowHtml + "</tr>");
										newTable.append(currentResponse.getResponse());
										intermediaryResponse.add(currentResponse);
									}
								}								
							}
						}
					}
					newTable.append("</table>");
					outputTable.append(newTable);
					SetAttribute(NUMBER_OF_UNSAVED_DOCUMENTS, numberOfUncheckedElements);
				}
			}
				
			String header1 = tableheader.replaceFirst("(?is)(<tr[^>]*>)\\s*(<td[^>]*>(?:\\s*<font[^>]*>)?)\\s*(Date)\\s*((?:</font>\\s*)?</td>)", "$1$2" + SELECT_ALL_CHECKBOXES + "Check\\Uncheck All$4$2$3$4");
			
			response.getParsedResponse().setHeader(CreateSaveToTSDFormHeader(URLConnectionReader.MULTIPLE_SAVE_TO_TSD_ACTION, "GET") 
							+ "<br><br><table width=\"100%\" border=\"1\" cellspacing=\"0\" cellpadding=\"2\">\n" + header1);
				
			response.getParsedResponse().setFooter("</table>" 
							+ "<br><br>" +  CreateSaveToTSDFormEnd(SAVE_SELECTED_DOCUMENTS_BUTTON_LABEL, 101, -1));
		}catch(Exception e) {
			e.printStackTrace();
		}
		
		return intermediaryResponse;
	}
    
    public boolean isInstrumentSaved(String instrumentNo, DocumentI documentToCheck, HashMap<String, String> data){
  	  return isInstrumentSaved(instrumentNo, documentToCheck, data, true);
    }
    
  /**
   * Looks for the a document having the same instrumentNo
   * @param instrumentNo
   * @param documentToCheck if not null will only compare its instrument with saved documents
   * @param data
   * @return
   */
  public boolean isInstrumentSaved(String instrumentNo, DocumentI documentToCheck, HashMap<String, String> data, boolean checkMiServerId){
  	if (StringUtils.isEmpty(instrumentNo)){
  		if (data != null) {
  			if (org.apache.commons.lang.StringUtils.isEmpty(data.get("book")) && org.apache.commons.lang.StringUtils.isEmpty(data.get("page"))){
  				return false;
  			}
  		} else{
  			return false;
  		}
  	}
  	
  	/* The AO,TR document must be re-saved in date down searches: Bug 4584 */
  	if(mSearch.getSa().isDateDown() && isAssessorOrTaxServer()) {
  		return false;
  	}
  	
  	DocumentsManagerI documentManager = getSearch().getDocManager();
  	try {
  		documentManager.getAccess();
  		if(documentToCheck != null) {
  			if(documentManager.getDocument(documentToCheck.getInstrument()) != null) {
  				return true;
  			} else if(!checkMiServerId) {
  				List<DocumentI> almostLike = documentManager.getDocumentsWithInstrumentsFlexible(false, documentToCheck.getInstrument());
  				if(almostLike != null && !almostLike.isEmpty()) {
  					return true;
  				}
  			}
  		} else {
	    		InstrumentI instr = new com.stewart.ats.base.document.Instrument(instrumentNo);
	    		if(data != null) {
		    		if(!StringUtils.isEmpty(data.get("type"))) {
		        		String serverDocType = data.get("type");
		    	    	String docCateg = DocumentTypes.getDocumentCategory(serverDocType, searchId); 
		            	instr.setDocType(docCateg);
		            	instr.setDocSubType(DocumentTypes.getDocumentSubcategory(serverDocType, searchId));
		    		}
		    		
		    		instr.setBook(data.get("book"));
		    		instr.setPage(data.get("page"));
		    		instr.setDocno(data.get("docno"));
	    		}
	    		
	    		try {
	    			instr.setYear(Integer.parseInt(data.get("year")));
	    		} catch (Exception e) {}
	    		
	    		if(documentManager.getDocument(instr) != null) {
	    			return true;
	    		} else {
	    			List<DocumentI> almostLike = documentManager.getDocumentsWithInstrumentsFlexible(false, instr);
	    			
	    			if(checkMiServerId) {
		    			boolean foundMssServerId = false;
	    				for (DocumentI documentI : almostLike) {
	    					if(miServerID==documentI.getSiteId()){
	    						foundMssServerId  = true;
	    						break;
	    					}
	    				}
		    			
	    				if(!foundMssServerId){
	    					return false;
	    				}
	    			}
	    			
  				if(data!=null) {
  					if(!StringUtils.isEmpty(data.get("type"))){
			        		String serverDocType = data.get("type"); 
			    	    	String docCateg = DocumentTypes.getDocumentCategory(serverDocType, searchId);
			    	    	String dataSource = data.get("dataSource");
			    	    	for (DocumentI documentI : almostLike) {
			    	    		if (serverDocType.equals("ASSESSOR") && dataSource != null) {
									if(documentI.getDocType().equals(docCateg) && documentI.getDataSource().equals(dataSource))//B 4435, must save NDB and ISI doc of the same instrNo
										return true;
			    	    		} else if (serverDocType.equals("CNTYTAX") && dataSource != null) {
			    	    			if(documentI.getDocType().equals(docCateg) && documentI.getDataSource().equals(dataSource))
										return true;
			    	    		} else if (serverDocType.equals("CITYTAX") && dataSource != null) {
			    	    			if(documentI.getDocType().equals(docCateg) && documentI.getDataSource().equals(dataSource))
										return true;
			    	    		}else if( (!checkMiServerId || miServerID==documentI.getSiteId()) && documentI.getDocType().equals(docCateg)){
									return true;
			    	    		}
							}	
  					}
		    		} else {
		    			EmailClient email = new EmailClient();
		    			email.addTo(MailConfig.getExceptionEmail());
		    			email.setSubject("isInstrumentNoSaved problem on " + URLMaping.INSTANCE_DIR + this.getClass().getName());
		    			email.addContent("We should at least have type!!!!\nSearchId=" + searchId);
		    			email.sendAsynchronous();
		    		}
	    		}
  		}
  		
  	} catch (Exception e) {
			e.printStackTrace();
		} finally {
			documentManager.releaseAccess();
		}
  	return false;
  }
  
    private String getYearFromResponse(String rsResponce) {
        int iTmp;
        iTmp = rsResponce.indexOf("In Year ") + 8;
        //get Instrument No.
        String year = rsResponce.substring(iTmp, iTmp + 4);
        return year;
    }

    private String getInstrNoFromResponse(String rsResponce) {
        int iTmp = rsResponce.indexOf("Instrument # ") + 13;
        //get Instrument No.
        String instrNo = rsResponce.substring(iTmp, rsResponce.indexOf(" ",
                iTmp));
        return instrNo;
    }
    
    /**
     * @param sTmp
     * @return
     */
    private String getOriginalLink(String instrNo, String year) {
        return "/newSearch/wilsondeeds_v2/index.php&=LIST&instnum=" + instrNo + "&year=" + year
                + "&db=" + msLastdb;
    }

    protected DownloadImageResult saveImage(ImageLinkInPage image)
            throws ServerResponseException {
        getTSConnection().setHostName(IMAGES_SITE);
        getTSConnection().setHostIP(IMAGES_SITE_IP);
        DownloadImageResult res = super.saveImage(image);
        //undo the modifications
        getTSConnection().setHostName(msiServerInfo.getServerAddress());
        getTSConnection().setHostIP(msiServerInfo.getServerIP());
        return res;
    }

    protected String getFileNameFromLink(String link) {
        String instNo = StringUtils.getTextBetweenDelimiters("instnum=", "&",
                link).trim();
        if (instNo.equals("")){ // links on uniques names pages
    		instNo = String.valueOf(System.currentTimeMillis()); // silimar to Williamson - fix for bug #1123
		}
        return instNo + ".html";
    }

    public static void splitResultRows(Parser p, ParsedResponse pr,
            String htmlString, int pageId, String linkStart, int action)
            throws ro.cst.tsearch.exceptions.ServerResponseException {
        if (pageId == Parser.ONE_ROW){
        	
        	String splitStr = "";
        	if( htmlString.contains( "<tr><td width=2% " ) ){
        		splitStr = "<tr><td width=2% ";
        	}
        	else{
        		splitStr = "<tr><td align=left>";
        	}
        	
            p.splitResultRows(pr, htmlString, pageId,
            		splitStr, "</table", linkStart,
                    action);
        }
        else if (pageId == Parser.ONE_ROW_NAME){
            p.splitResultRows(pr, htmlString, pageId, "<tr>", "</table",
                    linkStart, action);
        }
    }
    
	@Override
	protected void setCertificationDate() {

		try {
			if (CertificationDateManager.isCertificationDateInCache(dataSite)){
				String date = CertificationDateManager.getCertificationDateFromCache(dataSite);
				getSearch().getSa().updateCertificationDateObject(dataSite, Util.dateParser3(date));
			} else{
	        	String html = HttpUtils.downloadPage("http://www.wilsondeeds.com/newSearch/wilsondeeds_v2/index.php?c=ST&nc=1&a=Y&vu=1");
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
			module.forceValue(4, "p2");
		} else if(StringUtils.isNotEmpty(book) && StringUtils.isNotEmpty(page)) {
			module = getDefaultServerInfo().getModule(TSServerInfo.BOOK_AND_PAGE_MODULE_IDX);
			module.forceValue(0, book);
			module.forceValue(1, page);
			module.forceValue(3, "BP");
			module.forceValue(4, "execute search");
			module.forceValue(5, "24");
			module.forceValue(6, "p2");
		} else if(StringUtils.isNotEmpty(restoreDocumentDataI.getDocumentNumber())) {
			module = getDefaultServerInfo().getModule(TSServerInfo.INSTR_NO_MODULE_IDX);
			module.forceValue(0, restoreDocumentDataI.getDocumentNumber());
			module.forceValue(1, "INUM");
			module.forceValue(2, "execute search");
			module.forceValue(3, "24");
			module.forceValue(4, "p2");
		} else {
			module = null;
		}
		return module;
	}
	
	public Object getImageDownloader(RestoreDocumentDataI document) {
		return getRecoverModuleFrom(document);
	}
	
	@Override
	public Object parseAndFillResultMap(ServerResponse serverResponse, String detailsHtml, ResultMap resultMap) {
		
		try {
			detailsHtml = detailsHtml.replaceAll("(?is)&nbsp;", " ");
			detailsHtml = detailsHtml.replaceAll("<th(.*?)>", "<td$1>").replaceAll("</th>", "</td>").replaceAll("</b>", "").replaceAll("(?is)</?font[^>]*>", " ");
			detailsHtml = Tidy.tidyParse(detailsHtml, null);
			
			resultMap.put(OtherInformationSetKey.SRC_TYPE.getKeyName(), "RO");
			
			HtmlParser3 parser= new HtmlParser3(detailsHtml);
			NodeList nodeList = parser.getNodeList();
			
			Matcher mat = INSTRUMENT_NUMBER_PATT.matcher(detailsHtml);
			if (mat.find()){
				String instrumentNumber = mat.group(1);
				if (org.apache.commons.lang.StringUtils.isNotEmpty(instrumentNumber)){
					resultMap.put(SaleDataSetKey.INSTRUMENT_NUMBER.getKeyName(), instrumentNumber.trim());
				}
			}
			String book = HtmlParser3.getValueFromAbsoluteCell(0, 0, HtmlParser3.findNode(nodeList, "Book #"), "", false).trim();
			if (org.apache.commons.lang.StringUtils.isNotEmpty(book)){
				book = book.replaceAll("(?is)\\bBook\\s*#\\s*", "").trim();
				resultMap.put(SaleDataSetKey.BOOK.getKeyName(), book);
			}
			String page = HtmlParser3.getValueFromAbsoluteCell(0, 0, HtmlParser3.findNode(nodeList, "Page #"), "", false).trim();
			if (org.apache.commons.lang.StringUtils.isNotEmpty(page)){
				page = page.replaceAll("(?is)\\bPage\\s*#\\s*", "").trim();
				resultMap.put(SaleDataSetKey.PAGE.getKeyName(), page);
			}
			String recordedDate = HtmlParser3.getValueFromAbsoluteCell(0, 0, HtmlParser3.findNode(nodeList, "File Date"), "", false).trim();
			if (org.apache.commons.lang.StringUtils.isNotEmpty(recordedDate)){
				recordedDate = recordedDate.replaceAll("(?is)\\bFile Date\\s*:\\s*", "").trim();
				resultMap.put(SaleDataSetKey.RECORDED_DATE.getKeyName(), recordedDate);
			}
			String instrumentDate = HtmlParser3.getValueFromAbsoluteCell(0, 0, HtmlParser3.findNode(nodeList, "Instrument Date"), "", false).trim();
			if (org.apache.commons.lang.StringUtils.isNotEmpty(instrumentDate)){
				instrumentDate = instrumentDate.replaceAll("(?is)\\bInstrument Date\\s*:\\s*", "").trim();
				resultMap.put(SaleDataSetKey.INSTRUMENT_DATE.getKeyName(), instrumentDate);
			}
			String documentType = HtmlParser3.getValueFromAbsoluteCell(0, 0, HtmlParser3.findNode(nodeList, "Doc Type"), "", false).trim();
			if (org.apache.commons.lang.StringUtils.isNotEmpty(documentType)){
				documentType = documentType.replaceAll("(?is)\\bDoc Type\\s*:\\s*", "").trim();
				resultMap.put(SaleDataSetKey.DOCUMENT_TYPE.getKeyName(), documentType);
			}
			String considerationAmount = HtmlParser3.getValueFromAbsoluteCell(0, 0, HtmlParser3.findNode(nodeList, "Mort"), "", false).trim();
			if (org.apache.commons.lang.StringUtils.isNotEmpty(considerationAmount)){
				considerationAmount = considerationAmount.replaceAll("(?is)\\bMort\\s*\\$\\s*", "").trim();
				resultMap.put(SaleDataSetKey.MORTGAGE_AMOUNT.getKeyName(), considerationAmount);
			}
			
			String grantor = HtmlParser3.getValueFromAbsoluteCell(1, 0, HtmlParser3.findNode(nodeList, "Grantor"), "", false).trim();
			if (org.apache.commons.lang.StringUtils.isEmpty(grantor)){
				grantor = HtmlParser3.getValueFromAbsoluteCell(1, 0, HtmlParser3.findNode(nodeList, "Debtor"), "", false).trim();
			}
			if (org.apache.commons.lang.StringUtils.isNotEmpty(grantor)){
				resultMap.put(SaleDataSetKey.GRANTOR.getKeyName(), grantor.trim());
			}
			
			String grantee = HtmlParser3.getValueFromAbsoluteCell(1, 0, HtmlParser3.findNode(nodeList, "Grantee"), "", false).trim();
			if (org.apache.commons.lang.StringUtils.isEmpty(grantee)){
				grantee = HtmlParser3.getValueFromAbsoluteCell(1, 0, HtmlParser3.findNode(nodeList, "Secured Party"), "", false).trim();
			}
			if (org.apache.commons.lang.StringUtils.isNotEmpty(grantee)){
				resultMap.put(SaleDataSetKey.GRANTEE.getKeyName(), grantee.trim());
			}
			
			String legalDescription = HtmlParser3.getValueFromAbsoluteCell(0, 0, HtmlParser3.findNode(nodeList, "Legal Desc"), "", false).trim();
			if (org.apache.commons.lang.StringUtils.isNotEmpty(legalDescription)){
				legalDescription = legalDescription.replaceAll("(?is)\\bLegal Desc\\s*:\\s*", "");
			}
			String legalDescription2 = HtmlParser3.getValueFromNextCell(nodeList, "Legal Desc", "", false);
			if (org.apache.commons.lang.StringUtils.isNotEmpty(legalDescription2)){
				legalDescription += " " + legalDescription2;
				resultMap.put(PropertyIdentificationSetKey.PROPERTY_DESCRIPTION.getKeyName(), legalDescription.trim());
			}
			
			NodeList tableList = nodeList.extractAllNodesThatMatch(new TagNameFilter("table"), true);
			if (tableList != null && tableList.size() > 1){
				String crossRefsTable = "";
				String pisTable = "";
				for (int t = tableList.size() - 1; t > 0; t--){
					if (tableList.elementAt(t).toPlainTextString().contains("Inst #")){
						crossRefsTable = tableList.elementAt(t).toHtml();
						break;
					} else if (tableList.elementAt(t).toPlainTextString().contains("Subdivision")){
						pisTable = tableList.elementAt(t).toHtml();
					}
				}
				
				if (org.apache.commons.lang.StringUtils.isNotEmpty(crossRefsTable)){
					List<List<String>> crossRefsList = HtmlParser3.getTableAsList(crossRefsTable, false);
					if (crossRefsList != null && crossRefsList.size() > 0){
						List<String> crossrefLine = null;
						List<List> crossRefBody = new ArrayList<List>();
						for (List<String> list : crossRefsList) {
							if (list.size() == 5){
								crossrefLine = new ArrayList<String>();
								
								crossrefLine.add(list.get(1).replaceAll("-.*", "").trim());
								crossrefLine.add(list.get(3).trim());
								crossrefLine.add(list.get(4).trim());
								crossRefBody.add(crossrefLine);
							}
						}
						if (!crossRefBody.isEmpty()){
							String[] header= new String[]{"InstrumentNumber", "Book", "Page"};
							ResultTable rt = new ResultTable();
							rt = GenericFunctions2.createResultTable(crossRefBody, header);

							resultMap.put("CrossRefSet", rt);
						}
					}
				}
				
				if (org.apache.commons.lang.StringUtils.isNotEmpty(pisTable)){
					List<List<String>> pisList = HtmlParser3.getTableAsList(pisTable, false);
					List<List> pisBody = new ArrayList<List>();
					if (pisList != null && pisList.size() > 0){
						List<String> pisLine = null;
						
						int counter = 0;
						for (List<String> list : pisList){
							if (list.size() == 9){
								pisLine = new ArrayList<String>();
								pisLine.add(list.get(0).trim());
								pisLine.add(list.get(1).trim());
								pisLine.add(list.get(3).trim());
								String parcelId = list.get(5).trim() + "-" + list.get(4).trim() + "-" + list.get(6).trim();
								pisLine.add(parcelId);
								if (org.apache.commons.lang.StringUtils.isNotEmpty(legalDescription) && counter == 0){
									String unit = StringFormats.unitTNWilliamsonRO(legalDescription);
									if (org.apache.commons.lang.StringUtils.isNotEmpty(unit)){
										pisLine.add(unit);
									} else{
										pisLine.add("");
									}
									counter++;
								} else{
									pisLine.add("");
								}
								pisLine.add(list.get(7).trim());
								pisBody.add(pisLine);
							}
						}
					}

					String[] header= new String[]{"SubdivisionName", "SubdivisionLotNumber", "SubdivisionSection", "ParcelID", "SubdivisionUnit", "District"};
					ResultTable rt = new ResultTable();
					rt = GenericFunctions2.createResultTable(pisBody, header);

					resultMap.put("PropertyIdentificationSet", rt);
				}
			}
			
			ro.cst.tsearch.servers.functions.TNWilsonRO.parseName(resultMap, searchId);
			ro.cst.tsearch.servers.functions.TNWilsonRO.parseLegal(resultMap, searchId);
			
			GenericFunctions1.cleanUCCBook(resultMap, searchId);
			GenericFunctions1.checkMortgageAmount(resultMap, searchId);
			
			
		} catch(Exception e){
			e.printStackTrace();
		}
		
		return null;
	}
}