package ro.cst.tsearch.servers.types;

import java.io.File;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.math.BigDecimal;
import java.net.URLDecoder;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Vector;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Category;
import org.apache.log4j.Logger;
import org.htmlparser.Node;
import org.htmlparser.Text;
import org.htmlparser.filters.HasAttributeFilter;
import org.htmlparser.filters.TagNameFilter;
import org.htmlparser.tags.FormTag;
import org.htmlparser.tags.InputTag;
import org.htmlparser.tags.LinkTag;
import org.htmlparser.tags.TableColumn;
import org.htmlparser.tags.TableTag;
import org.htmlparser.util.NodeList;

import ro.cst.tsearch.Search;
import ro.cst.tsearch.bean.SearchAttributes;
import ro.cst.tsearch.connection.http.HTTPRequest;
import ro.cst.tsearch.connection.http.HTTPResponse;
import ro.cst.tsearch.connection.http3.HttpManager3;
import ro.cst.tsearch.connection.http3.HttpSite3;
import ro.cst.tsearch.exceptions.ServerResponseException;
import ro.cst.tsearch.extractor.legal.LegalDescription;
import ro.cst.tsearch.extractor.xml.Bridge;
import ro.cst.tsearch.extractor.xml.GenericFunctions;
import ro.cst.tsearch.extractor.xml.GenericFunctions1;
import ro.cst.tsearch.extractor.xml.GenericFunctions2;
import ro.cst.tsearch.extractor.xml.ResultMap;
import ro.cst.tsearch.extractor.xml.StringFormats;
import ro.cst.tsearch.generic.Util;
import ro.cst.tsearch.parser.HtmlParser3;
import ro.cst.tsearch.parser.LinkParser;
import ro.cst.tsearch.propertyInformation.Instrument;
import ro.cst.tsearch.search.FunctionStatesIterator;
import ro.cst.tsearch.search.ModuleStatesIterator;
import ro.cst.tsearch.search.filter.BetweenDatesFilterResponse;
import ro.cst.tsearch.search.filter.FilterResponse;
import ro.cst.tsearch.search.filter.newfilters.address.AddressFilterFactory;
import ro.cst.tsearch.search.filter.newfilters.date.DateFilterFactory;
import ro.cst.tsearch.search.filter.newfilters.date.LastTransferDateFilter;
import ro.cst.tsearch.search.filter.newfilters.doctype.DoctypeFilterFactory;
import ro.cst.tsearch.search.filter.newfilters.legal.GenericLegal;
import ro.cst.tsearch.search.filter.newfilters.legal.GenericMultipleLegalFilter;
import ro.cst.tsearch.search.filter.newfilters.legal.LegalFilterFactory;
import ro.cst.tsearch.search.filter.newfilters.name.GenericNameFilter;
import ro.cst.tsearch.search.filter.newfilters.name.NameFilterFactory;
import ro.cst.tsearch.search.filter.newfilters.name.SubdivisionFilter;
import ro.cst.tsearch.search.filter.newfilters.pin.PINFilterFactory;
import ro.cst.tsearch.search.filter.newfilters.pin.PinFilterResponse;
import ro.cst.tsearch.search.iterator.ModuleStatesIteratorFactory;
import ro.cst.tsearch.search.iterator.data.LegalStruct;
import ro.cst.tsearch.search.iterator.data.PlatBookPage;
import ro.cst.tsearch.search.iterator.instrument.InstrumentGenericIterator;
import ro.cst.tsearch.search.iterator.legal.LegalDescriptionIterator;
import ro.cst.tsearch.search.module.ConfigurableNameIterator;
import ro.cst.tsearch.search.module.OcrOrBootStraperIterator;
import ro.cst.tsearch.search.name.NameUtils;
import ro.cst.tsearch.search.util.AdditionalInfoKeys;
import ro.cst.tsearch.search.validator.DocsValidator;
import ro.cst.tsearch.searchsites.client.GWTDataSite;
import ro.cst.tsearch.servers.TSConnectionURL;
import ro.cst.tsearch.servers.info.TSServerInfo;
import ro.cst.tsearch.servers.info.TSServerInfoConstants;
import ro.cst.tsearch.servers.info.TSServerInfoFunction;
import ro.cst.tsearch.servers.info.TSServerInfoModule;
import ro.cst.tsearch.servers.response.ImageLinkInPage;
import ro.cst.tsearch.servers.response.LinkInPage;
import ro.cst.tsearch.servers.response.OtherInformationSet.OtherInformationSetKey;
import ro.cst.tsearch.servers.response.ParsedResponse;
import ro.cst.tsearch.servers.response.SaleDataSet.SaleDataSetKey;
import ro.cst.tsearch.servers.response.ServerResponse;
import ro.cst.tsearch.servers.types.TSInterface.DownloadImageResult.Status;
import ro.cst.tsearch.servlet.URLConnectionReader;
import ro.cst.tsearch.templates.MultilineElementsMap;
import ro.cst.tsearch.titledocument.abstracts.DocumentTypes;
import ro.cst.tsearch.utils.GBManager;
import ro.cst.tsearch.utils.HttpUtils;
import ro.cst.tsearch.utils.InstanceManager;
import ro.cst.tsearch.utils.Roman;
import ro.cst.tsearch.utils.SearchLogger;

import com.stewart.ats.base.document.DocumentI;
import com.stewart.ats.base.document.DocumentI.SearchType;
import com.stewart.ats.base.document.DocumentUtils;
import com.stewart.ats.base.document.ImageI;
import com.stewart.ats.base.document.InstrumentI;
import com.stewart.ats.base.document.RegisterDocumentI;
import com.stewart.ats.base.document.RestoreDocumentDataI;
import com.stewart.ats.base.legal.LegalI;
import com.stewart.ats.base.legal.SubdivisionI;
import com.stewart.ats.base.legal.TownShipI;
import com.stewart.ats.base.name.NameI;
import com.stewart.ats.base.parties.PartyI;
import com.stewart.ats.base.property.PinI;
import com.stewart.ats.base.property.PinI.PinType;
import com.stewart.ats.base.property.Property;
import com.stewart.ats.base.property.PropertyI;
import com.stewart.ats.base.search.DocumentsManagerI;

public class OHFranklinRO extends TSServerROLike implements TSServerROLikeI{
	
	private static final Category logger = Logger.getLogger(OHFranklinRO.class);
	
	private final static String platTypes[] = {"CONDOPLAT", "RRCONDOPLT", "RRPLAT", "CB", "CF", "CR", "DB", "IB", "LB", "MB", "OR", "PB", "PLAT", "ZB"};
	private final static String easementTypes[] = {"AMEA", "EA", "RREA", "RRSUEA", "RRVAEA", "SUEA", "VAEA"};
    
	
    private static final long serialVersionUID = -8652726141269675991L;
    public static SimpleDateFormat sdf = new SimpleDateFormat("MMM d, yyyy");
    private static final Calendar startCalendarForPlat;
    static {
    	startCalendarForPlat = (GregorianCalendar) GregorianCalendar.getInstance();
    	try{
            Date initial = sdf.parse(SearchAttributes.INITDATE);
            startCalendarForPlat.setTime( initial );
        }catch (ParseException e)
        {
        }
    }
    
    public static final Pattern bookPagePattern = Pattern.compile("\\d+[\\sA-Z]+\\d+");
    public static final Pattern aoPidPattern = Pattern.compile( "(\\d{3})-?(\\d{6})(?:-(\\d{3}))?" );
 
//  public final Pattern instrNoCrossrefPattern = Pattern.compile( a"I\\s+(\\d+)" );
    public final Pattern instrNoCrossrefPattern = Pattern.compile( "(?is)((<\\s*a[^>]*href\\s*=([^\\\"\\\'>]*)[^>]*>)|(<\\s*a[^>]*href\\s*=[\\\"]([^\\\">]*)[\\\"][^>]*>)|(<\\s*a[^>]*href\\s*=[\\\']([^\\\">]*)[\\\'][^>]*>))\\s*([0-9A-Z]{6,})<\\s*/\\s*a\\s*>" );
    
//	private static final Pattern bpPattern = Pattern.compile("(?is)\\s*(<i>(?:Bkwd|Fwd)</i>\\s+[A-HJ-Z]\\s*)(([0-9]+)\\s*([A-Za-z][0-9]+)\\s*|([0-9]{4})\\s*([0-9]+)\\s*)");
//	private static final Pattern iPattern = Pattern.compile("(?is)\\s*(<i>(?:Bkwd|Fwd)</i>\\s+I\\s+)([0-9A-Z]{6,})\\s*");
//	private static final Pattern hrefPattern = Pattern.compile("(?is)\\s*<\\s*a\\s+href\\s*=\\s*");
	
	private static final Pattern CROSSREF_PAT = Pattern.compile("(?is)(\\bhref=[\\\"|'])(simplequery\\.asp\\?instrs=[^'|^\\\"]+)");
//	private static final Pattern MARGINAL_PAT = Pattern.compile("(?is)(\\bhref=[\\\"|'])(simplequery\\.asp\\?Marginals=[^'|^\\\"]+)");
    
    public static final String TABLE_TEXT = "<table border=1 width='100%'><tr><td><b>Search Criteria:</b>";
    
    public OHFranklinRO( String rsRequestSolverName, String rsSitePath, String rsServerID, String rsPrmNameLink, long searchId, int mid){
        super(rsRequestSolverName, rsSitePath, rsServerID, rsPrmNameLink, searchId, mid);
        resultType = MULTIPLE_RESULT_TYPE;
        setRangeNotExpanded(true); //B2578
    }
    

    public static void processAllReferencedInstruments(DocumentI doc) {
    	try {
    		Set<InstrumentI> parsedRefs = doc.getParsedReferences();
    		for (InstrumentI instr : parsedRefs) {
    			processInstrumentNo(instr);
			}
		
    	} catch(Exception e) {
			e.printStackTrace();
		}
    }
    
    public static void processInstrumentNo(InstrumentI instr) {
		try {
			String instrNo = instr.getInstno();
			Matcher m = Pattern.compile("(\\d{4})(\\d{4})(\\d+)").matcher(instrNo);
			if (m.find()) {
				int instYear = Integer.parseInt(m.group(1));
				if (instYear <=  Calendar.getInstance().get(Calendar.YEAR)) {
					if ( Util.isValidDate(m.group(1))) {
						if (m.group(3).trim().length() == 7)
							instr.setEnableInstrNoTailMatch(true);
					}
				}
			}
			
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
    
	protected  void setModulesForGoBackOneLevelSearch( TSServerInfo serverInfo ) {
  	 
    	ConfigurableNameIterator nameIterator = null;
		DocsValidator defaultLegalValidator = LegalFilterFactory.getDefaultLegalFilter(searchId).getValidator();
		DocsValidator pinValidator = PINFilterFactory.getPinFilter(searchId,true,true).getValidator(); 
		DocsValidator addressHighPassValidator = AddressFilterFactory.getAddressHighPassFilter(searchId, 0.8d).getValidator();
		
		SearchAttributes sa = InstanceManager.getManager().getCurrentInstance(searchId).getCrtSearchContext().getSa();	
		List<TSServerInfoModule> modules = new ArrayList<TSServerInfoModule>();
	    TSServerInfoModule module;	
	    GBManager gbm=(GBManager)sa.getObjectAtribute(SearchAttributes.GB_MANAGER_OBJECT);
	    Calendar cal = Calendar.getInstance();
	    SimpleDateFormat sdf = new SimpleDateFormat("MM/dd/yyyy");
	    for (String id : gbm.getGbTransfers()) {
			  		   	    	 
	    	module = new TSServerInfoModule(serverInfo.getModule(TSServerInfo.ADV_SEARCH_MODULE_IDX));
	    	module.setIndexInGB(id);
	    	module.setTypeSearchGB("grantor");
	    	module.clearSaKeys();
		    module.setSaObjKey(SearchAttributes.GB_MANAGER_OBJECT);
		    module.addFilter(NameFilterFactory.getDefaultNameFilter(SearchAttributes.GB_MANAGER_OBJECT, searchId, module));
		    module.getFunction(2).setParamValue("Summary Data");
		    module.getFunction(2).setDefaultValue("Summary Data");      
		    module.getFunction(14).setParamValue("DESC");
		    String date = gbm.getDateForSearch(id, "MM/dd/yyyy", searchId);
		    try {
		    	if (date != null) 
		    		cal.setTime(sdf.parse(date));
			} catch (ParseException e) {
				e.printStackTrace();
			}
		    if (date != null) {
		    	module.getFunction(15).setData("" + cal.get(GregorianCalendar.DAY_OF_MONTH));
		        module.getFunction(16).setData("" + (cal.get(GregorianCalendar.MONTH) + 1));
		        module.getFunction(17).setData("" + cal.get(GregorianCalendar.YEAR));
		    }
		    nameIterator = (ConfigurableNameIterator)ModuleStatesIteratorFactory.getConfigurableNameIterator(module, searchId, new String[]{"L;F;"});
		 	module.addIterator(nameIterator);
		 	module.addValidator(defaultLegalValidator);
			module.addValidator(addressHighPassValidator);
	        module.addValidator(pinValidator);
            module.addValidator(NameFilterFactory.getDefaultTransferNameFilter(searchId, 0.90d, module).getValidator());
		  	module.addValidator(DateFilterFactory.getDateFilterForGoBack(SearchAttributes.GB_MANAGER_OBJECT, searchId, module).getValidator());
			modules.add(module);
		    
		    if (gbm.getNamesForBrokenChain(id, searchId).size() > 0){
		    	module = new TSServerInfoModule(serverInfo.getModule(TSServerInfo.ADV_SEARCH_MODULE_IDX));
			    module.setIndexInGB(id);
			    module.setTypeSearchGB("grantee");
			    module.clearSaKeys();
				module.setSaObjKey(SearchAttributes.GB_MANAGER_OBJECT);
				module.addFilter(NameFilterFactory.getDefaultNameFilter(SearchAttributes.GB_MANAGER_OBJECT, searchId, module));
				module.getFunction(2).setParamValue("Summary Data");
			    module.getFunction(2).setDefaultValue("Summary Data");      
			    module.getFunction(14).setParamValue("DESC");
				date = gbm.getDateForSearchBrokenChain(id, "MM/dd/yyyy", searchId);
				try {
					 if (date != null)	
					 cal.setTime(sdf.parse(date));
					} catch (ParseException e) {
						e.printStackTrace();
					}
				if (date != null) {
			        module.getFunction(15).setData("" + cal.get(GregorianCalendar.DAY_OF_MONTH));
			        module.getFunction(16).setData("" + (cal.get(GregorianCalendar.MONTH) + 1));
			        module.getFunction(17).setData("" + cal.get(GregorianCalendar.YEAR)); 
				}
				nameIterator = (ConfigurableNameIterator)ModuleStatesIteratorFactory.getConfigurableNameIterator(module, searchId, new String[]{"L;F;"});
				module.addIterator(nameIterator);
				module.addValidator(defaultLegalValidator);
				module.addValidator(addressHighPassValidator);
				module.addValidator(pinValidator);
			    module.addValidator(NameFilterFactory.getDefaultTransferNameFilter(searchId, 0.90d, module).getValidator());
				module.addValidator(DateFilterFactory.getDateFilterForGoBack(SearchAttributes.GB_MANAGER_OBJECT, searchId, module).getValidator());
				
				modules.add(module);
		    }
	    }	 
	    serverInfo.setModulesForGoBackOneLevelSearch(modules);	     
    }
    
    protected void setModulesForAutoSearch(TSServerInfo serverInfo) {
        
        List<TSServerInfoModule> l = new ArrayList<TSServerInfoModule>();
        
        if (getSearch().getSearchType() == Search.AUTOMATIC_SEARCH) {
        TSServerInfoModule m = null;
        int newFunctionNum = 0;

        SearchAttributes sa = getSearch().getSa();
        
        String parcelNo = sa.getAtribute(SearchAttributes.LD_PARCELNO);
        
        DocsValidator legalValidator = getLegalValidator();
        DocsValidator recordedDateValidator = BetweenDatesFilterResponse.getDefaultIntervalFilter(searchId).getValidator();
        boolean validateWithDates = applyDateFilter();
        
        GenericMultipleLegalFilter defaultLegalFilter = new GenericMultipleLegalFilter(searchId);
		defaultLegalFilter.setUseLegalFromSearchPage(true);
		defaultLegalFilter.setEnableSection(true);
		defaultLegalFilter.setEnableLotUnitFullEquivalence(true);
		defaultLegalFilter.setMarkIfCandidatesAreEmpty(true);
		defaultLegalFilter.setThreshold(new BigDecimal(0.7));
		defaultLegalFilter.setAdditionalInfoKey(AdditionalInfoKeys.RO_LOOK_UP_DATA);
		
		FilterResponse subdivisionNameFilter = NameFilterFactory.getDefaultNameFilterForSubdivision(searchId);
		DocsValidator subdivisionNameValidator = subdivisionNameFilter.getValidator();
		
		PinFilterResponse pinFilter = PINFilterFactory.getPinFilter(searchId, true, false);
		
		DocsValidator multipleLegalValidator = defaultLegalFilter.getValidator();
		DocsValidator pinValidator = PINFilterFactory.getPinFilter(searchId, true, true).getValidator();
        
		 // search by instrument number list from Fake Documents saved from DTG
		InstrumentGenericIterator instrumentIterator = getInstrumentIteratorForFakeDocsFromDTG(true);
        m = new TSServerInfoModule(serverInfo.getModule(TSServerInfo.INSTR_NO_MODULE_IDX));
        m.addExtraInformation(TSServerInfoConstants.EXTRA_PARAM_MODULE_DESCRIPTION, 
    			"Search again for Fake docs and their related docs from DTG");
        m.clearSaKeys();
        m.getFunction(3).forceValue("Summary Data");
        m.setIteratorType(0, FunctionStatesIterator.ITERATOR_TYPE_INSTRUMENT_LIST_FAKE);
        m.addCrossRefValidator(legalValidator);
        m.addCrossRefValidator(pinValidator);
        if (validateWithDates) {
        	m.addValidator(recordedDateValidator);
        	m.addCrossRefValidator(recordedDateValidator);
        }
        m.addIterator(instrumentIterator);
        l.add(m);
        
        // search by book and page list from Fake Documents saved from DTG
        instrumentIterator = getInstrumentIteratorForFakeDocsFromDTG(false);
        m = new TSServerInfoModule(serverInfo.getModule(TSServerInfo.INSTR_NO_MODULE_IDX));
        m.addExtraInformation(TSServerInfoConstants.EXTRA_PARAM_MODULE_DESCRIPTION, 
    			"Search again for Fake docs and their related docs from DTG");
        m.clearSaKeys();
        m.getFunction(3).forceValue("Summary Data");
        m.getFunction(1).setIteratorType(FunctionStatesIterator.ITERATOR_TYPE_BOOK_FAKE);
        m.getFunction(2).setIteratorType(FunctionStatesIterator.ITERATOR_TYPE_PAGE_FAKE);
        m.addCrossRefValidator(legalValidator);
        m.addCrossRefValidator(pinValidator);
        if (validateWithDates) {
        	m.addValidator(recordedDateValidator);
        	m.addCrossRefValidator(recordedDateValidator);
        }
        m.addIterator(instrumentIterator);
        l.add(m);

        // search by instrument number list from Ao/Tax
		instrumentIterator = getInstrumentIterator(true);
		instrumentIterator.setLoadFromRoLike(false);
        m = new TSServerInfoModule(serverInfo.getModule(TSServerInfo.INSTR_NO_MODULE_IDX));
        m.addExtraInformation(TSServerInfoConstants.EXTRA_PARAM_MODULE_DESCRIPTION, TSServerInfoConstants.VALUE_PARAM_LIST_AO_NDB_TR_INSTR);
        m.clearSaKeys();
        m.getFunction(3).forceValue("Summary Data");
        m.setIteratorType(0, FunctionStatesIterator.ITERATOR_TYPE_INSTRUMENT_LIST_FAKE);
        m.addCrossRefValidator(legalValidator);
        m.addCrossRefValidator(pinValidator);
        if (validateWithDates) {
        	m.addValidator(recordedDateValidator);
        	m.addCrossRefValidator( recordedDateValidator);
        }
        m.addIterator(instrumentIterator);
        l.add(m);
        
        // search by book and page list from Ao/Tax
        instrumentIterator = getInstrumentIterator(false);
        instrumentIterator.setLoadFromRoLike(false);
        m = new TSServerInfoModule(serverInfo.getModule(TSServerInfo.INSTR_NO_MODULE_IDX));
        m.addExtraInformation(TSServerInfoConstants.EXTRA_PARAM_MODULE_DESCRIPTION, TSServerInfoConstants.VALUE_PARAM_LIST_AO_NDB_TR_BP);
        m.clearSaKeys();
        m.getFunction(3).forceValue("Summary Data");
        m.getFunction(1).setIteratorType(FunctionStatesIterator.ITERATOR_TYPE_BOOK_FAKE);
        m.getFunction(2).setIteratorType(FunctionStatesIterator.ITERATOR_TYPE_PAGE_FAKE);
        m.addCrossRefValidator(legalValidator);
        m.addCrossRefValidator(pinValidator);
        if (validateWithDates) {
        	m.addValidator(recordedDateValidator);
        	m.addCrossRefValidator(recordedDateValidator);
        }
        m.addIterator(instrumentIterator);
        l.add(m);
        
		//search by PID
        if (!StringUtils.isEmpty(parcelNo)){
            m = new TSServerInfoModule(serverInfo.getModule(TSServerInfo.ADV_SEARCH_MODULE_IDX));
            m.addExtraInformation(TSServerInfoConstants.EXTRA_PARAM_MODULE_DESCRIPTION, "Search by Parcel ID");
            m.clearSaKeys();
            m.addFilter(pinFilter);
            m.getFunction(2).forceValue("Detail Data");
            m.getFunction(5).setParamValue("ParcelNum");
            Matcher aoPidMatch = aoPidPattern.matcher( parcelNo);
            if (aoPidMatch.find()){
                m.setData(6, aoPidMatch.group(1) + "-" + aoPidMatch.group(2));
            }
            
            m.getFunction(14).setParamValue("DESC");
	        m.setSaKey(15, SearchAttributes.FROMDATE_DD);
	        m.setSaKey(16, SearchAttributes.FROMDATE_MM);
	        m.setSaKey(17, SearchAttributes.FROMDATE_YEAR);
	        m.setSaKey(18, SearchAttributes.TODATE_DD);
	        m.setSaKey(19, SearchAttributes.TODATE_MM);
	        m.setSaKey(20, SearchAttributes.TODATE_YEAR);
            m.addCrossRefValidator(legalValidator);
            if (validateWithDates){
            	m.addValidator(recordedDateValidator);
            	m.addCrossRefValidator(recordedDateValidator);
            }
            l.add(m);
        }
        
        LegalDescriptionIterator it = getLegalDescriptionIterator(false);
        LastTransferDateFilter ltdfs = (new LastTransferDateFilter(searchId));
        ltdfs.setNameSearch(false);
        
	    //search by subdivision + lot
        //do not expand the lot
        m = new TSServerInfoModule(serverInfo.getModule(TSServerInfo.ADV_SEARCH_MODULE_IDX));
        m.clearSaKeys();           
		            
		m.addValidator(pinValidator);
		m.addValidator(multipleLegalValidator);
		m.addValidator(subdivisionNameValidator);
		m.addValidator(ltdfs.getValidator());
		
		m.getFunction(2).forceValue("Detail Data");//needs Detail Data if search is getting too much results
		//m.setData(8, subdivsDerivation.get(i));
		m.setIteratorType(8, FunctionStatesIterator.ITERATOR_TYPE_GENERIC_67);
		//m.setData(12, lotNo);
		m.setIteratorType(12, FunctionStatesIterator.ITERATOR_TYPE_LOT);
		            
		if (!getSearch().isPropertyCondo()) {
			m.setIteratorType(FunctionStatesIterator.ITERATOR_TYPE_LOT_INTERVAL);
			m.addExtraInformation(TSServerInfoConstants.EXTRA_PARAM_MODULE_DESCRIPTION, 
					TSServerInfoConstants.VALUE_PARAM_SUBDIVISION_LOT);
		} else {
			m.addExtraInformation(TSServerInfoConstants.EXTRA_PARAM_MODULE_DESCRIPTION, 
					TSServerInfoConstants.VALUE_PARAM_SUBDIVISION_AS_CONDO_LOT);
		}
		m.setData(14, "DESC");
		m.addCrossRefValidator(multipleLegalValidator);
		m.addCrossRefValidator(ltdfs.getValidator());
		m.addCrossRefValidator(pinValidator);
		m.addCrossRefValidator(subdivisionNameValidator);
		if (validateWithDates) {
			m.addValidator(recordedDateValidator);
			m.addCrossRefValidator(recordedDateValidator);
		}
		m.addIterator(it);
		l.add(m);
	        
		//search by subdivision + unit
		it = getLegalDescriptionIteratorForSubdAndUnit(false);
		m = new TSServerInfoModule(serverInfo.getModule(TSServerInfo.ADV_SEARCH_MODULE_IDX));
		m.addExtraInformation(TSServerInfoConstants.EXTRA_PARAM_MODULE_DESCRIPTION, 
	    			"Subdivision module - searching with subdivision and unit");
		m.clearSaKeys();
		          
		m.addValidator(pinValidator);
		m.addValidator(multipleLegalValidator);
		m.addValidator(ltdfs.getValidator());
		m.addValidator(subdivisionNameValidator);
		
		m.getFunction(2).forceValue("Summary Data");
		m.setData(5, "UnitNum");
		//m.setData(6, unitNo);
		m.setIteratorType(6, FunctionStatesIterator.ITERATOR_TYPE_GENERIC_68);
		//m.setData(8, subdivsDerivation.get(i));
		m.setIteratorType(8, FunctionStatesIterator.ITERATOR_TYPE_GENERIC_67);
		m.setData(14, "DESC");
		m.addCrossRefValidator(multipleLegalValidator);
		m.addCrossRefValidator(ltdfs.getValidator());
		m.addCrossRefValidator(pinValidator);
		m.addCrossRefValidator(subdivisionNameValidator);
		if (validateWithDates) {
			m.addValidator(recordedDateValidator);
			m.addCrossRefValidator(recordedDateValidator);
		}
		m.addIterator(it);
		l.add(m);        
        
        //owner search
    	ConfigurableNameIterator nameOwnerIterator = null;
        if (hasOwner()){
        	m = new TSServerInfoModule(serverInfo.getModule(TSServerInfo.ADV_SEARCH_MODULE_IDX));
        	m.addExtraInformation(TSServerInfoConstants.EXTRA_PARAM_MODULE_DESCRIPTION, TSServerInfoConstants.VALUE_PARAM_NAME_OWNERS);
        	LastTransferDateFilter ltdf = new LastTransferDateFilter(searchId);
        	ltdf.setUseDefaultDocTypeThatPassForGoodName(false);
        	
        	m.clearSaKeys();
	        m.addFilter(NameFilterFactory.getDefaultNameFilter(SearchAttributes.OWNER_OBJECT, searchId, m));
	        m.addValidator(multipleLegalValidator);
	        m.addValidator(ltdf.getValidator());
	        m.addValidator(pinValidator);
	        m.setSaObjKey(SearchAttributes.OWNER_OBJECT);
	        
	        m.getFunction(2).forceValue("Names Summary");
	        
//	        m.getFunction(2).forceValue("Summary Data");
	        m.getFunction(14).setParamValue("DESC");
	        m.setSaKey(15, SearchAttributes.FROMDATE_DD);
	        m.setSaKey(16, SearchAttributes.FROMDATE_MM);
	        m.setSaKey(17, SearchAttributes.FROMDATE_YEAR);
	        
	        m.setIteratorType(0, FunctionStatesIterator.ITERATOR_TYPE_LAST_NAME_FAKE);
			m.setIteratorType(1, FunctionStatesIterator.ITERATOR_TYPE_FIRST_NAME_FAKE);
			m.setIteratorType(15, FunctionStatesIterator.ITERATOR_TYPE_FROM_DATE);
			m.setIteratorType(16, FunctionStatesIterator.ITERATOR_TYPE_FROM_DATE);
			m.setIteratorType(17, FunctionStatesIterator.ITERATOR_TYPE_FROM_DATE);
			
			addFilterForUpdate(m, false);
	        m.addCrossRefValidator(multipleLegalValidator);
	        m.addCrossRefValidator(ltdf.getValidator());
	        m.addCrossRefValidator(pinValidator);
			if (validateWithDates) {
				DocsValidator recordedDateNameValidator = BetweenDatesFilterResponse.getDefaultIntervalFilter(searchId, m).getValidator();
				m.addValidator(recordedDateNameValidator);
				m.addCrossRefValidator(recordedDateNameValidator);
			}
	        nameOwnerIterator =  (ConfigurableNameIterator)ModuleStatesIteratorFactory.getConfigurableNameIterator(m, searchId, new String[]{"L;F;"});
			m.addIterator(nameOwnerIterator);
			l.add(m);

        }
       
        //buyer search
        if (hasBuyer() && !InstanceManager.getManager().getCurrentInstance(searchId).getCrtSearchContext().isProductType(SearchAttributes.SEARCH_PROD_REFINANCE) ){
            m = new TSServerInfoModule(serverInfo.getModule(TSServerInfo.ADV_SEARCH_MODULE_IDX));
            m.addExtraInformation(TSServerInfoConstants.EXTRA_PARAM_MODULE_DESCRIPTION, TSServerInfoConstants.VALUE_PARAM_NAME_BUYERS);
            m.clearSaKeys();			
		    m.addFilter(NameFilterFactory.getNameFilterIgnoreMiddleOnEmpty(SearchAttributes.BUYER_OBJECT, searchId, m));
            m.addValidator(DoctypeFilterFactory.getDoctypeBuyerFilter(searchId).getValidator());
            
            m.setSaObjKey(SearchAttributes.BUYER_OBJECT);
            m.getFunction(2).forceValue("Names Summary");
//            m.getFunction(2).forceValue("Summary Data");
            m.getFunction(14).setParamValue("DESC");
            
            m.setSaKey(15, SearchAttributes.FROMDATE_DD);
	        m.setSaKey(16, SearchAttributes.FROMDATE_MM);
	        m.setSaKey(17, SearchAttributes.FROMDATE_YEAR);
            
            m.setIteratorType(0, FunctionStatesIterator.ITERATOR_TYPE_LAST_NAME_FAKE);
			m.setIteratorType(1, FunctionStatesIterator.ITERATOR_TYPE_FIRST_NAME_FAKE);
			m.setIteratorType(15, FunctionStatesIterator.ITERATOR_TYPE_FROM_DATE);
			m.setIteratorType(16, FunctionStatesIterator.ITERATOR_TYPE_FROM_DATE);
			m.setIteratorType(17, FunctionStatesIterator.ITERATOR_TYPE_FROM_DATE);
            
            m.addCrossRefValidator(DoctypeFilterFactory.getDoctypeBuyerFilter(searchId).getValidator());
            m.addIterator((ConfigurableNameIterator)ModuleStatesIteratorFactory.getConfigurableNameIterator(m, searchId, new String[]{"L;F;"}));
            if (validateWithDates) {
            	DocsValidator recordedDateNameValidator = BetweenDatesFilterResponse.getDefaultIntervalFilter(searchId, m).getValidator();
				m.addValidator(recordedDateNameValidator);
				m.addCrossRefValidator(recordedDateNameValidator);
            }
            addFilterForUpdate(m, false);
            l.add(m);
        }
        
        SubdivisionFilter subdivisionFilter = new SubdivisionFilter(searchId);
        //search for plats and easements
        it = getLegalDescriptionIterator(false);
        m = new TSServerInfoModule(serverInfo.getModule(TSServerInfo.ADV_SEARCH_MODULE_IDX));
        m.addExtraInformation(TSServerInfoConstants.EXTRA_PARAM_MODULE_DESCRIPTION, TSServerInfoConstants.VALUE_PARAM_NAME_SUBDIVISION_PLAT);
        m.clearSaKeys();
        m.addFilter(subdivisionFilter);
        m.addValidator(multipleLegalValidator );
        m.addValidator(DoctypeFilterFactory.getDoctypeSubdivisionIsGranteeFilter(searchId).getValidator());
        m.addValidator(pinValidator);
        //m.setData(0, subdivsDerivation.get(j));
        m.setIteratorType(0, FunctionStatesIterator.ITERATOR_TYPE_GENERIC_67);
        m.getFunction(2).forceValue("Detail Data");
        m.setParamValue(14, "DESC" );
        m.setParamValue(15, String.valueOf(startCalendarForPlat.get(Calendar.DATE)));
        m.setParamValue(16, String.valueOf(startCalendarForPlat.get(Calendar.MONTH) + 1));
        m.setParamValue(17, String.valueOf(startCalendarForPlat.get(Calendar.YEAR)));
        for (int i = 0; i < platTypes.length; i++){
        	newFunctionNum = m.addFunction();
        	m.getFunction(newFunctionNum).setParamName("DocTypes");
        	m.getFunction(newFunctionNum).setDefaultValue(platTypes[i]);
        	m.getFunction(newFunctionNum).setParamValue(platTypes[i]);
        	m.getFunction(newFunctionNum).setHiden(true);
        }
        m.addCrossRefValidator(multipleLegalValidator);
        m.addCrossRefValidator(DoctypeFilterFactory.getDoctypeSubdivisionIsGranteeFilter(searchId).getValidator());
        m.addCrossRefValidator(pinValidator);
        if (validateWithDates) {
        	m.addValidator(recordedDateValidator);
        	m.addCrossRefValidator(recordedDateValidator);
        }
        m.addIterator(it);
        l.add(m);

        //easements
        it = getLegalDescriptionIterator(false);
        m = new TSServerInfoModule(serverInfo.getModule(TSServerInfo.ADV_SEARCH_MODULE_IDX));
        m.addExtraInformation(TSServerInfoConstants.EXTRA_PARAM_MODULE_DESCRIPTION, TSServerInfoConstants.VALUE_PARAM_NAME_SUBDIVISION_EASEMENT);
        m.clearSaKeys();
        m.addFilter(subdivisionFilter);
        m.addValidator(multipleLegalValidator);
        m.addValidator(DoctypeFilterFactory.getDoctypeSubdivisionIsGranteeFilter(searchId).getValidator());
        m.addValidator(pinValidator);
        //m.getFunction(0).setData(subdivsDerivation.get(j));
        m.setIteratorType(0, FunctionStatesIterator.ITERATOR_TYPE_GENERIC_67);
        m.getFunction(2).forceValue("Detail Data");
        m.getFunction(14).setParamValue("DESC");
        m.setParamValue(15, String.valueOf(startCalendarForPlat.get(Calendar.DATE)));
        m.setParamValue(16, String.valueOf(startCalendarForPlat.get(Calendar.MONTH) + 1));
        m.setParamValue(17, String.valueOf(startCalendarForPlat.get(Calendar.YEAR)));
        
        for (int i = 0; i < easementTypes.length; i++){
        	newFunctionNum = m.addFunction();
        	m.getFunction(newFunctionNum).setParamName("DocTypes");
        	m.getFunction(newFunctionNum).setDefaultValue(easementTypes[i]);
        	m.getFunction(newFunctionNum).setParamValue(easementTypes[i]);
        	m.getFunction(newFunctionNum).setHiden(true);
        }
        m.addCrossRefValidator(multipleLegalValidator);
        m.addCrossRefValidator(DoctypeFilterFactory.getDoctypeSubdivisionIsGranteeFilter(searchId).getValidator());
        m.addCrossRefValidator(pinValidator);
        if (validateWithDates) {
        	m.addValidator(recordedDateValidator);
        	m.addCrossRefValidator(recordedDateValidator);
        }
        m.addIterator(it);
        l.add(m);
	    
        //search by crossRef book and page list from RO docs
        instrumentIterator = getInstrumentIterator(false);
        instrumentIterator.setLoadFromRoLike(true);
		m = new TSServerInfoModule(serverInfo.getModule(TSServerInfo.INSTR_NO_MODULE_IDX));
		m.clearSaKeys();
		m.addExtraInformation(TSServerInfoConstants.EXTRA_PARAM_MODULE_DESCRIPTION,  TSServerInfoConstants.VALUE_PARAM_LIST_RO_CROSSREF_BP);
		m.addExtraInformation(TSServerInfoConstants.EXTRA_PARAM_SIMULATE_CROSSREF, true);
		m.getFunction(3).forceValue("Summary Data");
	    m.getFunction(1).setIteratorType(FunctionStatesIterator.ITERATOR_TYPE_BOOK_FAKE);		    
	    m.getFunction(2).setIteratorType(FunctionStatesIterator.ITERATOR_TYPE_PAGE_FAKE);
	    m.addValidator(multipleLegalValidator);
	    m.addValidator(pinValidator);
	    m.addCrossRefValidator(multipleLegalValidator);
	    m.addCrossRefValidator(pinValidator);
        if (validateWithDates) {
        	m.addValidator(recordedDateValidator);
        	m.addCrossRefValidator(recordedDateValidator);
        }
        m.addIterator(instrumentIterator);
	    l.add(m);

		//search by crossRef instr# list from RO docs
	    instrumentIterator = getInstrumentIterator(true);
	    instrumentIterator.setLoadFromRoLike(true);
		m = new TSServerInfoModule(serverInfo.getModule(TSServerInfo.INSTR_NO_MODULE_IDX));
		m.clearSaKeys();
		m.addExtraInformation(TSServerInfoConstants.EXTRA_PARAM_MODULE_DESCRIPTION,
				TSServerInfoConstants.VALUE_PARAM_LIST_RO_CROSSREF_INSTR);
		m.addExtraInformation(TSServerInfoConstants.EXTRA_PARAM_SIMULATE_CROSSREF, true);		
        m.getFunction(0).setIteratorType(FunctionStatesIterator.ITERATOR_TYPE_INSTRUMENT_LIST_FAKE); 
        m.getFunction(3).forceValue("Summary Data");
        m.addValidator(multipleLegalValidator);
        m.addValidator(pinValidator);
		m.addCrossRefValidator(multipleLegalValidator);
		m.addCrossRefValidator(pinValidator);
        if (validateWithDates) {
        	m.addValidator(recordedDateValidator);
        	m.addCrossRefValidator(recordedDateValidator);
        }
        m.addIterator(instrumentIterator);
		l.add(m);
		
        //OCR search - instrument number
		OcrOrBootStraperIterator ocrBPIteratoriterator = new OcrOrBootStraperIterator(searchId) {
	    	private static final long serialVersionUID = 1L;
	    	
	    	@Override
	    	public List<Instrument> extractInstrumentNoList(TSServerInfoModule initial) {
	    		List<Instrument> extractInstrumentNoList = super.extractInstrumentNoList(initial);
	    		
	    		for (Instrument instrument : extractInstrumentNoList) {
	    			String page = org.apache.commons.lang.StringUtils.defaultString(instrument.getPageNo());
	    			if (page.contains("-")){				
	    				page = page.replaceAll("[-]+", "");
	    				
	    				instrument.setPageNo(page);
	    			}
				}
	    		setSearchIfPresent(false);
	    		return extractInstrumentNoList;
	    	}
	    };
	    ocrBPIteratoriterator.setInitAgain(true);
	    
        m = new TSServerInfoModule(serverInfo.getModule(TSServerInfo.INSTR_NO_MODULE_IDX));
        m.addExtraInformation(TSServerInfoConstants.EXTRA_PARAM_MODULE_DESCRIPTION, 
        		TSServerInfoConstants.VALUE_PARAM_OCR_SEARCH_BP_INST);
        m.clearSaKeys();
        m.setIteratorType(ModuleStatesIterator.TYPE_OCR_FULL_OR_BOOTSTRAPER);
        m.addValidator(multipleLegalValidator);
        m.addValidator(pinValidator);
        m.getFunction(3).forceValue("Summary Data");
        m.getFunction(1).setIteratorType(FunctionStatesIterator.ITERATOR_TYPE_BOOK_SEARCH);
        m.getFunction(2).setIteratorType(FunctionStatesIterator.ITERATOR_TYPE_PAGE_SEARCH);
        m.addIterator(ocrBPIteratoriterator);
        m.addCrossRefValidator(multipleLegalValidator);
        m.addCrossRefValidator(pinValidator);
        l.add(m);
        
        m = new TSServerInfoModule(serverInfo.getModule(TSServerInfo.INSTR_NO_MODULE_IDX));
        m.addExtraInformation(TSServerInfoConstants.EXTRA_PARAM_MODULE_DESCRIPTION, 
        		TSServerInfoConstants.VALUE_PARAM_OCR_SEARCH_BP_INST);
        m.clearSaKeys();
        m.setIteratorType(ModuleStatesIterator.TYPE_OCR_FULL_OR_BOOTSTRAPER);
        m.addValidator(multipleLegalValidator);
        m.addValidator(pinValidator);
        m.getFunction(3).forceValue("Summary Data");
        m.getFunction(0).setIteratorType(FunctionStatesIterator.ITERATOR_TYPE_INSTRUMENT_LIST_SEARCH);
        m.addCrossRefValidator(multipleLegalValidator);
        m.addCrossRefValidator(pinValidator);
        l.add(m);
        
        m = new TSServerInfoModule(serverInfo.getModule(TSServerInfo.ADV_SEARCH_MODULE_IDX));
   	    LastTransferDateFilter ltdf = new LastTransferDateFilter(searchId);
   	    ltdf.setUseDefaultDocTypeThatPassForGoodName(false);
   	    m.clearSaKeys();
        m.addFilter(NameFilterFactory.getDefaultNameFilter(SearchAttributes.OWNER_OBJECT, searchId, m));
        m.addValidator(multipleLegalValidator);
        m.addValidator(ltdf.getValidator());
        m.addValidator(pinValidator);
        m.setSaObjKey(SearchAttributes.OWNER_OBJECT);
        m.getFunction(0).setIteratorType(FunctionStatesIterator.ITERATOR_TYPE_LAST_NAME_FAKE);
		m.getFunction(1).setIteratorType(FunctionStatesIterator.ITERATOR_TYPE_FIRST_NAME_FAKE);
		m.getFunction(2).forceValue("Names Summary");
//      m.getFunction(2).forceValue("Summary Data");
        m.getFunction(14).setParamValue("DESC");
        m.addCrossRefValidator(multipleLegalValidator);
        m.addCrossRefValidator(ltdf.getValidator());
        m.addCrossRefValidator(pinValidator);
        if (validateWithDates) {
        	DocsValidator recordedDateNameValidator = BetweenDatesFilterResponse.getDefaultIntervalFilter(searchId, m).getValidator();
			m.addValidator(recordedDateNameValidator);
			m.addCrossRefValidator(recordedDateNameValidator);
        }
        
        ArrayList< NameI > searchedNames = null;
		if (nameOwnerIterator!=null){
			searchedNames = nameOwnerIterator.getSearchedNames();
		} else {
			searchedNames = new ArrayList<NameI>();
		}
		
		nameOwnerIterator = (ConfigurableNameIterator)ModuleStatesIteratorFactory.getConfigurableNameIterator(m, searchId, false, new String[]{"L;F;"});
		nameOwnerIterator.setInitAgain(true);
		nameOwnerIterator.setSearchedNames(searchedNames);
		m.addIterator(nameOwnerIterator); 
		l.add(m);
        }

        serverInfo.setModulesForAutoSearch(l);
    }
    
    public InstrumentGenericIterator getInstrumentIteratorForFakeDocsFromDTG(boolean instrumentType) {
		InstrumentGenericIterator instrumentGenericIterator = new InstrumentGenericIterator(searchId, getDataSite()) {

			private static final long serialVersionUID = 5399351945130601258L;

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
		instrumentGenericIterator.setCheckOnlyFakeDocs(true);
		instrumentGenericIterator.setCheckRelatedOfFakeDocs(true);
		instrumentGenericIterator.setLoadFromRoLike(true);
		instrumentGenericIterator.setDsToLoad(new String[]{"DG"});
//		instrumentGenericIterator.setRoDoctypesToLoad(new String[]{""});
		return instrumentGenericIterator;
	}
    
    public InstrumentGenericIterator getInstrumentIterator(boolean instrumentType) {
		InstrumentGenericIterator instrumentGenericIterator = new InstrumentGenericIterator(searchId, getDataSite()) {

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
			protected String cleanInstrumentNo(InstrumentI instrument){
				return cleanInstrumentNo(instrument.getInstno(), instrument.getYear());
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
		instrumentGenericIterator.setDoNotCheckIfItExists(false);
		return instrumentGenericIterator;
	}
    
    private LegalDescriptionIterator getLegalDescriptionIterator(boolean lookupWasDoneWithName) {
		LegalDescriptionIterator it = new LegalDescriptionIterator(searchId, lookupWasDoneWithName, false, getDataSite()) {

			private static final long serialVersionUID = -4741635379234782109L;
			
			private Map<LegalStruct, Integer> initialDocumentsBeforeRunningStruct = new HashMap<LegalStruct, Integer>();
			private Set<String> allSubdivisionNames = new HashSet<String>();
			
			public List<DocumentI> getGoodDocumentsOrForCurrentOwner(DocumentsManagerI m, Search search, boolean applyNameMatch, int siteType){
				final List<DocumentI> ret = new ArrayList<DocumentI>();
				
				List<RegisterDocumentI> listRodocs = m.getRealRoLikeDocumentList();
				DocumentUtils.sortDocuments(listRodocs, MultilineElementsMap.DATE_ORDER_DESC);
				
				SearchAttributes sa	= search.getSa();
				PartyI owner 		= sa.getOwners();
				
				for (RegisterDocumentI doc : listRodocs){
					boolean found = false;
					for (PropertyI prop : doc.getProperties()){
						if (((doc.isOneOf("MORTGAGE", "TRANSFER", "RELEASE") || isTransferAllowed(doc)) && applyNameMatch)
								|| ((doc.isOneOf("MORTGAGE") || isTransferAllowed(doc)) && !applyNameMatch)){
							if (prop.hasSubdividedLegal()){
								SubdivisionI sub = prop.getLegal().getSubdivision();
								TownShipI twp = prop.getLegal().getTownShip();
								LegalStruct lglStruct = new LegalStruct(false);
				
								lglStruct.setAddition(sub.getName());
								lglStruct.setLot(sub.getLot());
								lglStruct.setUnit(sub.getUnit());
								lglStruct.setPlatBook(sub.getPlatBook());
								lglStruct.setPlatPage(sub.getPlatPage());
								if (twp != null){
									lglStruct.setSection(org.apache.commons.lang.StringUtils.defaultIfEmpty(twp.getSection(), ""));
								}
				
								boolean nameMatched = false;
				
								if (applyNameMatch){
									if (GenericNameFilter.isMatchGreaterThenScore(owner, doc.getGrantor(), NameFilterFactory.NAME_FILTER_THRESHOLD) 
											|| GenericNameFilter.isMatchGreaterThenScore(doc.getGrantor(), owner, NameFilterFactory.NAME_FILTER_THRESHOLD)
											|| GenericNameFilter.isMatchGreaterThenScore(owner, doc.getGrantee(), NameFilterFactory.NAME_FILTER_THRESHOLD) 
											|| GenericNameFilter.isMatchGreaterThenScore(doc.getGrantee(), owner, NameFilterFactory.NAME_FILTER_THRESHOLD)){
										nameMatched = true;
									}
								}
								if ((nameMatched || !applyNameMatch)
										&& StringUtils.isNotEmpty(lglStruct.getAddition())
//										&& StringUtils.isNotEmpty(lglStruct.getLot())
										){
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
			
			@SuppressWarnings("unchecked")
			protected List<DocumentI> loadLegalFromRoDocs(Search global, DocumentsManagerI m) {
				List<DocumentI> listRodocs = new ArrayList<DocumentI>();
				
				if (AdditionalInfoKeys.RO_SAVED_DOCUMENTS_FOR_LEGAL_ITERATOR.equals(getCheckAlreadyFilledKeyWithDocuments())) {
					List<DocumentI> listRodocsSaved = (List<DocumentI>) global.getAdditionalInfo(getCheckAlreadyFilledKeyWithDocuments());
					if (listRodocsSaved != null && !listRodocsSaved.isEmpty()) {
						listRodocs.addAll(listRodocsSaved);
					}
				}
				if (listRodocs.isEmpty()) {
					if (getRoDoctypesToLoad() == null) {
						listRodocs.addAll(getGoodDocumentsOrForCurrentOwner(m, global, true, GWTDataSite.RO_TYPE));
						if (listRodocs.isEmpty()){
							listRodocs.addAll(getGoodDocumentsOrForCurrentOwner(m, global, false, GWTDataSite.RO_TYPE));
						}
					} else {
						listRodocs.addAll(m.getDocumentsWithDocType(true, getRoDoctypesToLoad()));
					}
					if (AdditionalInfoKeys.RO_SAVED_DOCUMENTS_FOR_LEGAL_ITERATOR.equals(getCheckAlreadyFilledKeyWithDocuments())) {
						global.setAdditionalInfo(getCheckAlreadyFilledKeyWithDocuments(), listRodocs);	
					}
				}
				if (listRodocs.isEmpty()){
					try {
						LegalStruct lglStruct = new LegalStruct(false);
						SearchAttributes sa = getSearchAttributes();
						lglStruct.setAddition(sa.getAtribute(SearchAttributes.LD_SUBDIV_NAME));
						lglStruct.setLot(sa.getAtribute(SearchAttributes.LD_LOTNO));
						lglStruct.setUnit(sa.getAtribute(SearchAttributes.LD_SUBDIV_UNIT));
						lglStruct.setPlatBook(sa.getAtribute(SearchAttributes.LD_BOOKNO));
						lglStruct.setPlatPage(sa.getAtribute(SearchAttributes.LD_PAGENO));
						lglStruct.setSection(sa.getAtribute(SearchAttributes.LD_SUBDIV_SEC));
						legalStruct.add(lglStruct);
					} catch (Exception e) {
					}
				} else{
					DocumentUtils.sortDocuments(listRodocs, MultilineElementsMap.DATE_ORDER_DESC);
					
					for (DocumentI reg : listRodocs){
						if (!reg.isOneOf(
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
									
								|| isTransferAllowed(reg)){
							for (PropertyI prop : reg.getProperties()){
								if (prop.hasLegal()){
									LegalI legal = prop.getLegal();
									treatLegalFromSavedDocument(reg.prettyPrint(), legal, true, null);
								}
							}
						}
					}
				}
				legalStruct = keepOnlyGoodLegals(legalStruct);
				if (legalStruct.size() == 1){
					for (LegalStruct str : legalStruct){
						if (org.apache.commons.lang.StringUtils.isNotEmpty(str.getAddition())){
							allSubdivisionNames.add(str.getAddition());
						}
					}
				}
				if (legalStruct.isEmpty()){
					setLoadFromSearchPage(false);
					setLoadFromSearchPageIfNoLookup(false);
				}
				return listRodocs;
			}
			
			protected int getDocumentsManagerDocSize() {
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
						
			@Override
			protected void loadDerrivation(TSServerInfoModule module, LegalStruct str) {
				
				if (!initialDocumentsBeforeRunningStruct.containsKey(str)) {
					initialDocumentsBeforeRunningStruct.put(str, getDocumentsManagerDocSize());
				}
				
				for (Object functionObject : module.getFunctionList()) {
					if (functionObject instanceof TSServerInfoFunction) {
						TSServerInfoFunction function = (TSServerInfoFunction) functionObject;
						switch (function.getIteratorType()) {
							case FunctionStatesIterator.ITERATOR_TYPE_LOT:
								function.setParamValue(str.getLot());
								break;
							case FunctionStatesIterator.ITERATOR_TYPE_GENERIC_67:
								function.setParamValue(org.apache.commons.lang.StringUtils.defaultString(str.getAddition()));
								break;
						}
					}
				}
			}
			
			@Override
			public void processSubdivisionName(Search global, String originalLot, String originalBlock,
					String originalUnit, Set<String> temporarySubdivisionsForCondoSearch) {
			}
			@Override
			protected void processSubdivisionLotBlock(String subdivName, String lot, String block) {
			}
			@Override
			protected void processSubdivisionTractPlatBookPage(String subdivName, String platBook, String platPage,
					String tract) {
			}
			
			@Override
			protected String treatOnlySubdividedLegal(String sourceKey, LegalI legal,
					boolean useAlsoSubdivisionName, String subdivisionName, Set<PlatBookPage> platBookPageFromUser) {
				if(isEnableSubdividedLegal() && legal.hasSubdividedLegal()){
					SubdivisionI subdiv = legal.getSubdivision();

					String lot = subdiv.getLot();
					String unit = subdiv.getUnit();
					String platBook = cleanPlatBook(subdiv.getPlatBook());
					String platPage = cleanPlatPage(subdiv.getPlatPage());
					String platInst = subdiv.getPlatInstrument();
										
					LegalStruct legalStruct1 = new LegalStruct(false);
					
					legalStruct1.setPlatBook(StringUtils.isEmpty(platBook) ? "" : platBook);
					legalStruct1.setPlatPage(StringUtils.isEmpty(platPage) ? "" : platPage);
					legalStruct1.setPlatInst(StringUtils.isEmpty(platInst) ? "" : platInst);
					
					TownShipI twp = legal.getTownShip();
					String section = "";
					if (twp != null){
						section = twp.getSection();
					}
					legalStruct1.setSection(StringUtils.isEmpty(section) ? "" : section);
					
					if (useAlsoSubdivisionName) {
						subdivisionName = subdiv.getName();
						if (StringUtils.isNotEmpty(subdivisionName)){
							
							legalStruct1.setAddition(subdivisionName);
							legalStruct1.setLot(lot);
							legalStruct1.setUnit(unit);
								
							saveSubdivisionName(subdivisionName);

							legalStruct.add(legalStruct1);
						}
					}
				}
				return subdivisionName;
			}
			
			@Override
			protected Set<LegalStruct> keepOnlyGoodLegals(Set<LegalStruct> legals){
				Set<LegalStruct> good = new HashSet<LegalStruct>();
				for (LegalStruct str : legals){
					if (!incompleteData(str)){
						good.add(str);
					}
				}
				return good;
			}
			
			private boolean incompleteData(LegalStruct str){
				
				if (str == null){
					return true;
				}
//				boolean emptyLot = StringUtils.isEmpty(str.getLot());
				boolean emptySubdivisionName = StringUtils.isEmpty(str.getAddition());		
								
				return (emptySubdivisionName);
			}
			
			@Override
			public boolean isTransferAllowed(RegisterDocumentI doc) {
				
				if (doc != null && doc.isOneOf(DocumentTypes.TRANSFER)) {
					String[] realTransferSubcategories = DocumentTypes.getRealTransferSubcategories(
							Integer.parseInt(getSearch().getStateId()), 
							Integer.parseInt(getSearch().getCountyId()));
					if (doc.isOneOfSubcategory(realTransferSubcategories)) {
						return true;
					}
				}
				return false;
			}
			
			@Override
			protected String cleanPlatBook(String platbook){
				if (org.apache.commons.lang.StringUtils.isNotEmpty(platbook)){
					platbook = platbook.replaceFirst("(?is)\\A[A-Z](\\d+)", "$1");
					
					return platbook;
				}
				
				return "";
			}
		};
		
		it.setAdditionalInfoKey(AdditionalInfoKeys.RO_LOOK_UP_DATA);
		it.setEnableTownshipLegal(false);
		it.setEnableSubdividedLegal(true);
		it.setEnableSubdivision(true);
		it.setLoadFromSearchPage(false);
		it.setLoadFromSearchPageIfNoLookup(false);
		
		return it;
	}
    
    private String cleanPlatPage(String platPage){
		if (org.apache.commons.lang.StringUtils.isNotEmpty(platPage)){
			platPage = platPage.replaceFirst("(?is)(\\d+)[A-Z]", "$1");
			
			return platPage;
		}
		
		return "";
	}
    
    private LegalDescriptionIterator getLegalDescriptionIteratorForSubdAndUnit(boolean lookupWasDoneWithName) {
		LegalDescriptionIterator it = new LegalDescriptionIterator(searchId, lookupWasDoneWithName, false, getDataSite()) {

			private static final long serialVersionUID = -4741635379234782109L;
			
			private Map<LegalStruct, Integer> initialDocumentsBeforeRunningStruct = new HashMap<LegalStruct, Integer>();
			private Set<String> allSubdivisionNames = new HashSet<String>();
			
			public List<DocumentI> getGoodDocumentsOrForCurrentOwner(DocumentsManagerI m, Search search, boolean applyNameMatch, int siteType){
				final List<DocumentI> ret = new ArrayList<DocumentI>();
				
				List<RegisterDocumentI> listRodocs = m.getRealRoLikeDocumentList();
				DocumentUtils.sortDocuments(listRodocs, MultilineElementsMap.DATE_ORDER_DESC);
				
				SearchAttributes sa	= search.getSa();
				PartyI owner 		= sa.getOwners();
				
				for (RegisterDocumentI doc : listRodocs){
					boolean found = false;
					for (PropertyI prop : doc.getProperties()){
						if (((doc.isOneOf("MORTGAGE", "TRANSFER", "RELEASE") || isTransferAllowed(doc)) && applyNameMatch)
								|| ((doc.isOneOf("MORTGAGE") || isTransferAllowed(doc)) && !applyNameMatch)){
							if (prop.hasSubdividedLegal()){
								SubdivisionI sub = prop.getLegal().getSubdivision();
								TownShipI twp = prop.getLegal().getTownShip();
								LegalStruct lglStruct = new LegalStruct(false);
				
								lglStruct.setAddition(sub.getName());
								lglStruct.setLot(sub.getLot());
								lglStruct.setUnit(sub.getUnit());
								lglStruct.setPlatBook(sub.getPlatBook());
								lglStruct.setPlatPage(sub.getPlatPage());
								if (twp != null){
									lglStruct.setSection(org.apache.commons.lang.StringUtils.defaultIfEmpty(twp.getSection(), ""));
								}
				
								boolean nameMatched = false;
				
								if (applyNameMatch){
									if (GenericNameFilter.isMatchGreaterThenScore(owner, doc.getGrantor(), NameFilterFactory.NAME_FILTER_THRESHOLD) 
											|| GenericNameFilter.isMatchGreaterThenScore(doc.getGrantor(), owner, NameFilterFactory.NAME_FILTER_THRESHOLD)
											|| GenericNameFilter.isMatchGreaterThenScore(owner, doc.getGrantee(), NameFilterFactory.NAME_FILTER_THRESHOLD) 
											|| GenericNameFilter.isMatchGreaterThenScore(doc.getGrantee(), owner, NameFilterFactory.NAME_FILTER_THRESHOLD)){
										nameMatched = true;
									}
								}
				
								if ((nameMatched || !applyNameMatch)
										&& StringUtils.isNotEmpty(lglStruct.getAddition())
										&& StringUtils.isNotEmpty(lglStruct.getUnit())){
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
			
			@SuppressWarnings("unchecked")
			protected List<DocumentI> loadLegalFromRoDocs(Search global, DocumentsManagerI m) {
				List<DocumentI> listRodocs = new ArrayList<DocumentI>();
				
				if (AdditionalInfoKeys.RO_SAVED_DOCUMENTS_FOR_LEGAL_ITERATOR.equals(getCheckAlreadyFilledKeyWithDocuments())) {
					List<DocumentI> listRodocsSaved = (List<DocumentI>) global.getAdditionalInfo(getCheckAlreadyFilledKeyWithDocuments());
					if (listRodocsSaved != null && !listRodocsSaved.isEmpty()) {
						listRodocs.addAll(listRodocsSaved);
					}
				}
				legalStruct = new HashSet<LegalStruct>();
				
				if (listRodocs.isEmpty()) {
					if (getRoDoctypesToLoad() == null) {
						listRodocs.addAll(getGoodDocumentsOrForCurrentOwner(m, global, true, GWTDataSite.RO_TYPE));
						if (listRodocs.isEmpty()){
							listRodocs.addAll(getGoodDocumentsOrForCurrentOwner(m, global, false, GWTDataSite.RO_TYPE));
						}
					} else {
						listRodocs.addAll(m.getDocumentsWithDocType(true, getRoDoctypesToLoad()));
					}
					if (AdditionalInfoKeys.RO_SAVED_DOCUMENTS_FOR_LEGAL_ITERATOR.equals(getCheckAlreadyFilledKeyWithDocuments())) {
						global.setAdditionalInfo(getCheckAlreadyFilledKeyWithDocuments(), listRodocs);	
					}
				}
				if (listRodocs.isEmpty()){
					try {
						SearchAttributes sa = getSearchAttributes();
						String subdivision = sa.getAtribute(SearchAttributes.LD_SUBDIV_NAME);
						String unit = sa.getAtribute(SearchAttributes.LD_SUBDIV_UNIT);
						if (org.apache.commons.lang.StringUtils.isNotEmpty(subdivision) && org.apache.commons.lang.StringUtils.isNotEmpty(unit)) {
							LegalStruct lglStruct = new LegalStruct(false);
							lglStruct.setAddition(subdivision);
							lglStruct.setUnit(unit);
							lglStruct.setLot(sa.getAtribute(SearchAttributes.LD_LOTNO));
							lglStruct.setPlatBook(sa.getAtribute(SearchAttributes.LD_BOOKNO));
							lglStruct.setPlatPage(sa.getAtribute(SearchAttributes.LD_PAGENO));
							lglStruct.setSection(sa.getAtribute(SearchAttributes.LD_SUBDIV_SEC));

							legalStruct.add(lglStruct);
						}
					} catch (Exception e) {
					}
				} else{
					DocumentUtils.sortDocuments(listRodocs, MultilineElementsMap.DATE_ORDER_DESC);
					
					for (DocumentI reg : listRodocs){
						if (!reg.isOneOf(
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
									
								|| isTransferAllowed(reg)){
							for (PropertyI prop : reg.getProperties()){
								if (prop.hasLegal()){
									LegalI legal = prop.getLegal();
									treatLegalFromSavedDocument(reg.prettyPrint(), legal, true, null);
								}
							}
						}
					}
				}
				legalStruct = keepOnlyGoodLegals(legalStruct);
				
				if (legalStruct.size() == 1){
					for (LegalStruct str : legalStruct){
						if (org.apache.commons.lang.StringUtils.isNotEmpty(str.getAddition())){
							allSubdivisionNames.add(str.getAddition());
						}
					}
				}
				if (legalStruct.isEmpty()){
					setLoadFromSearchPage(false);
					setLoadFromSearchPageIfNoLookup(false);
				}
				return listRodocs;
			}
			
			protected int getDocumentsManagerDocSize() {
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
						
			@Override
			protected void loadDerrivation(TSServerInfoModule module, LegalStruct str) {
				
				if (!initialDocumentsBeforeRunningStruct.containsKey(str)) {
					initialDocumentsBeforeRunningStruct.put(str, getDocumentsManagerDocSize());
				}
				
				for (Object functionObject : module.getFunctionList()) {
					if (functionObject instanceof TSServerInfoFunction) {
						TSServerInfoFunction function = (TSServerInfoFunction) functionObject;
						switch (function.getIteratorType()) {
							case FunctionStatesIterator.ITERATOR_TYPE_GENERIC_67:
								function.setParamValue(org.apache.commons.lang.StringUtils.defaultString(str.getAddition()));
								break;
							case FunctionStatesIterator.ITERATOR_TYPE_GENERIC_68:
								function.setParamValue(org.apache.commons.lang.StringUtils.defaultString(str.getUnit()));
								break;
						}
					}
				}
			}
			
			@Override
			public void processSubdivisionName(Search global, String originalLot, String originalBlock,
					String originalUnit, Set<String> temporarySubdivisionsForCondoSearch) {
			}
			@Override
			protected void processSubdivisionLotBlock(String subdivName, String lot, String block) {
			}
			@Override
			protected void processSubdivisionTractPlatBookPage(String subdivName, String platBook, String platPage,
					String tract) {
			}
			
			@Override
			protected String treatOnlySubdividedLegal(String sourceKey, LegalI legal,
					boolean useAlsoSubdivisionName, String subdivisionName, Set<PlatBookPage> platBookPageFromUser) {
				if(isEnableSubdividedLegal() && legal.hasSubdividedLegal()){
					SubdivisionI subdiv = legal.getSubdivision();

					String unit = subdiv.getUnit();
					String lot = subdiv.getLot();
					String platBook = cleanPlatBook(subdiv.getPlatBook());
					String platPage = cleanPlatPage(subdiv.getPlatPage());
					String platInst = subdiv.getPlatInstrument();
										
					LegalStruct legalStruct1 = new LegalStruct(false);
					
					legalStruct1.setPlatBook(StringUtils.isEmpty(platBook) ? "" : platBook);
					legalStruct1.setPlatPage(StringUtils.isEmpty(platPage) ? "" : platPage);
					legalStruct1.setPlatInst(StringUtils.isEmpty(platInst) ? "" : platInst);
					
					TownShipI twp = legal.getTownShip();
					String section = "";
					if (twp != null){
						section = twp.getSection();
					}
					legalStruct1.setSection(StringUtils.isEmpty(section) ? "" : section);
					
					if (useAlsoSubdivisionName) {
						subdivisionName = subdiv.getName();
						if (StringUtils.isNotEmpty(subdivisionName) && StringUtils.isNotEmpty(unit)){
							
							legalStruct1.setAddition(subdivisionName);
							legalStruct1.setUnit(unit);
							legalStruct1.setLot(lot);
								
							saveSubdivisionName(subdivisionName);
							legalStruct.add(legalStruct1);
						}
					}
				}
				return subdivisionName;
			}
			
			@Override
			public Set<LegalStruct> keepOnlyGoodLegals(Set<LegalStruct> legals){
				Set<LegalStruct> good = new HashSet<LegalStruct>();
				for (LegalStruct str : legals){
					if (!incompleteData(str)){
						good.add(str);
					}
				}
				return good;
			}
			
			private boolean incompleteData(LegalStruct str){
				
				if (str == null){
					return true;
				}
				boolean emptySubdivisionName = StringUtils.isEmpty(str.getAddition());
				boolean emptyUnit = StringUtils.isEmpty(str.getUnit());
			
				return (emptySubdivisionName && emptyUnit);
			}
			
			@Override
			public boolean isTransferAllowed(RegisterDocumentI doc) {
				
				if (doc != null && doc.isOneOf(DocumentTypes.TRANSFER)) {
					String[] realTransferSubcategories = DocumentTypes.getRealTransferSubcategories(
							Integer.parseInt(getSearch().getStateId()), 
							Integer.parseInt(getSearch().getCountyId()));
					if (doc.isOneOfSubcategory(realTransferSubcategories)) {
						return true;
					}
				}
				return false;
			}
			
			@Override
			protected String cleanPlatBook(String platbook){
				if (org.apache.commons.lang.StringUtils.isNotEmpty(platbook)){
					platbook = platbook.replaceFirst("(?is)\\A[A-Z](\\d+)", "$1");
					
					return platbook;
				}
				
				return "";
			}
		};
		
		it.setEnableTownshipLegal(false);
		it.setEnableSubdividedLegal(true);
		it.setEnableSubdivision(true);
		it.setLoadFromSearchPage(false);
		it.setLoadFromSearchPageIfNoLookup(false);
		
		return it;
	}
    
//    public static List<DocumentI> getGoodDocumentsOrForCurrentOwner(LegalDescriptionIteratorI legalDescriptionIteratorI, DocumentsManagerI m, 
//    																Search search, boolean applyNameMatch){
//    	final List<DocumentI> ret = new ArrayList<DocumentI>();
//
//    	List<RegisterDocumentI> listRodocs = m.getRealRoLikeDocumentList();
//    	DocumentUtils.sortDocuments(listRodocs, MultilineElementsMap.DATE_ORDER_DESC);
//			
//    	SearchAttributes sa	= search.getSa();
//    	PartyI owner 		= sa.getOwners();
//			
//    	for (RegisterDocumentI doc : listRodocs){
//    		boolean found = false;
//			for (PropertyI prop : doc.getProperties()){
//				if (((doc.isOneOf("MORTGAGE", "RELEASE") || legalDescriptionIteratorI.isTransferAllowed(doc)) && applyNameMatch)
//							|| ((doc.isOneOf("MORTGAGE") || legalDescriptionIteratorI.isTransferAllowed(doc)) && !applyNameMatch)){
//					if (prop.hasSubdividedLegal()){
//						SubdivisionI sub = prop.getLegal().getSubdivision();
//						LegalStruct ret1 = new LegalStruct(false);
//			
//						ret1.setPlatBook(sub.getPlatBook());
//						ret1.setPlatPage(sub.getPlatPage());
//						ret1.setAddition(sub.getName());
//						ret1.setUnit(sub.getUnit());
//			
//						boolean nameMatched = false;
//			
//						if (applyNameMatch){
//							if (GenericNameFilter.isMatchGreaterThenScore(owner, doc.getGrantor(), NameFilterFactory.NAME_FILTER_THRESHOLD) 
//									|| GenericNameFilter.isMatchGreaterThenScore(doc.getGrantor(), owner, NameFilterFactory.NAME_FILTER_THRESHOLD)
//									|| GenericNameFilter.isMatchGreaterThenScore(owner, doc.getGrantee(), NameFilterFactory.NAME_FILTER_THRESHOLD) 
//									|| GenericNameFilter.isMatchGreaterThenScore(doc.getGrantee(), owner, NameFilterFactory.NAME_FILTER_THRESHOLD)){
//								nameMatched = true;
//							}
//						}
//						if ((nameMatched || !applyNameMatch) 
//								&& (StringUtils.isNotEmpty(ret1.getLot()) || StringUtils.isNotEmpty(ret1.getBlock())) 
//								&& StringUtils.isNotEmpty(ret1.getPlatBook()) 
//								&& StringUtils.isNotEmpty(ret1.getPlatPage()) ){
//							ret.add(doc);
//							found = true;
//							break;
//						}
//					}
//				}
//				}
//				if (found){
//					break;
//				}	
//			}
//			
//		return ret;
//	}
    
    private DocsValidator getLegalValidator() {
    	GenericLegal genericLegal = (GenericLegal)LegalFilterFactory.getDefaultLegalFilter(searchId);
		genericLegal.setDoNotIntervalExpand(getSearch().isPropertyCondo());
		genericLegal.setEnablePlatBook(true);
		genericLegal.setEnablePlatPage(true);
		return genericLegal.getValidator();
	}

    public static String cleanResp(String resp) {
    	if (!StringUtils.isEmpty(resp)) {
    		 resp = resp.replaceAll("\\r\\n", "");
             //Apartments Condominiums
    		 resp = resp.replaceAll("(?i)Apartments|Condominiums", "");
             //cut all until the first hr
             int i = resp.indexOf("<hr>");
             if (i > -1) {
            	 resp = resp.substring(i);
             }
            	
             int j = resp.indexOf(TABLE_TEXT);
             //keep all the information until the last table and cut all from here
             if (j>-1) {
            	 resp = resp.substring( 0, j); 
             }
		}
    	return resp;
    }
    
   	@SuppressWarnings("rawtypes")
	protected void ParseResponse(String sAction, ServerResponse Response, int viParseID)  throws ServerResponseException{
        String rsResponse = Response.getResult();
        ParsedResponse parsedResponse = Response.getParsedResponse();

        if (rsResponse.indexOf("THE MINIMUM SEARCH CRITERIA WAS NOT PROVIDED") >= 0){
            return;
        }
        if (rsResponse.indexOf("NO RECORDS RETRIEVED FOR THE SPECIFIED SEARCH CRITERIA") >= 0){
        	String message = "<br>NO RECORDS RETRIEVED FOR THE SPECIFIED SEARCH CRITERIA!";
        	if (rsResponse.indexOf("Try making the search criteria more general") >= 0){
        		message += "<br>Try making the search criteria more general.";
        	}

        	if (org.apache.commons.lang.StringUtils.isNotEmpty(message)){
        		Response.getParsedResponse().setWarning(NO_DATA_FOUND + message);
        		Response.getParsedResponse().setOnlyResultRows(new Vector());
        		return;
        	}
        }
        if (rsResponse.indexOf("YOU CAN NOW VIEW PAGES FROM BOOK") >= 0){
            return;
        }
        if (rsResponse.indexOf("The image for this instrument is not available at this time") >= 0){
            return;
        }
        
        switch (viParseID) {
        	
        case ID_SEARCH_BY_NAME:
        case ID_SEARCH_BY_PARCEL:
        case ID_SEARCH_BY_SUBDIVISION_NAME:
        case ID_SEARCH_BY_INSTRUMENT_NO:
        	
        	if (rsResponse.indexOf("Search Criteria") > -1) {
        		
        		if (Response.getRawQuerry().indexOf("SUBMIT=Detail+Data") > -1){
        			ParseResponse(sAction, Response, ID_DETAILS);
        			return;
        		}
        		
        		rsResponse = cleanResp(rsResponse);
				
        		if (Response.getRawQuerry().indexOf("Names+Summary") > -1){
        			try {
						 
						StringBuilder outputTable = new StringBuilder();
						Collection<ParsedResponse> smartParsedResponses = smartParseNamesSummary(Response, rsResponse, outputTable);
												
						if (smartParsedResponses.size() > 0) {
							parsedResponse.setResultRows(new Vector<ParsedResponse>(smartParsedResponses));
							parsedResponse.setOnlyResponse(outputTable.toString());
//							parsedResponse.setAttribute(PARENT_SITE_LOOKUP_MODE, true);
			            }
						
					} catch(Exception e) {
						e.printStackTrace();
					}
        		} else{
        			try {
						 
						StringBuilder outputTable = new StringBuilder();
						Collection<ParsedResponse> smartParsedResponses = smartParseSummaryData(Response, rsResponse, outputTable, false);
												
						if (smartParsedResponses.size() > 0) {
							parsedResponse.setResultRows(new Vector<ParsedResponse>(smartParsedResponses));
							parsedResponse.setOnlyResponse(outputTable.toString());
			            }
						
					} catch(Exception e) {
						e.printStackTrace();
					}
        		}
        	}
        	
        	break;
        case ID_SEARCH_BY_ADDRESS:
        	
        	try {
        		StringBuilder outputTable = new StringBuilder();
        		Collection<ParsedResponse> smartParsedResponses = smartParseSummaryData(Response, rsResponse, outputTable, true);
												
        		if (smartParsedResponses.size() > 0) {
        			parsedResponse.setResultRows(new Vector<ParsedResponse>(smartParsedResponses));
        			parsedResponse.setOnlyResponse(outputTable.toString());
        		}
						
        	} catch(Exception e) {
        		e.printStackTrace();
        	}
        	
        	break;
        case ID_DETAILS:
        	if (rsResponse.indexOf("Search Criteria") > -1) {
        		
        		rsResponse = cleanResp(rsResponse);
        		//remove links to AO 
        		rsResponse = rsResponse.replaceAll("(?is)\\b(PrpId:\\s*)<a[^>]*>(.*?)</a>", "$1$2");
        		try {
					 
					StringBuilder outputTable = new StringBuilder();
					Collection<ParsedResponse> smartParsedResponses = smartParseDetailData(Response, rsResponse, outputTable);
					
					if (smartParsedResponses.size() > 0) {
						parsedResponse.setResultRows(new Vector<ParsedResponse>(smartParsedResponses));
						parsedResponse.setOnlyResponse(outputTable.toString());
		            }
					
				} catch(Exception e) {
					e.printStackTrace();
				}
        	}
        	
        	break;
        case ID_SAVE_TO_TSD:
        	
        	if (rsResponse.toLowerCase().contains("<html")){
        		ParseResponse(sAction, Response, ID_DETAILS);
        	}
        	DocumentI document = parsedResponse.getDocument();
        	
        	if (document == null){
	        	if (Response.getParsedResponse().getResultRows().size() == 1){
	        		parsedResponse = (ParsedResponse) Response.getParsedResponse().getResultRows().get(0);
	        		Response.setParsedResponse(parsedResponse);
	        		document = parsedResponse.getDocument();
	        	} else{
	        		return;
	        	}
        	}

			if (document != null) {
				msSaveToTSDFileName = document.getId() + ".html";
				Response.getParsedResponse().setFileName(getServerTypeDirectory() + msSaveToTSDFileName);
			}
        	break;
        	
        case ID_GET_LINK:
            if (Response.getQuerry().indexOf("Summary Data") > -1){
                ParseResponse(sAction, Response, ID_SEARCH_BY_NAME);
            } else if (Response.getQuerry().indexOf("Detail Data") > -1 && rsResponse.contains("Instr:")){
            	ParseResponse(sAction, Response, ID_SEARCH_BY_ADDRESS);
            }else if (Response.getQuerry().indexOf("Detail Data") > -1
            		|| Response.getLastURI().toString().toLowerCase().indexOf("instrs=") > -1
            		|| Response.getLastURI().toString().toLowerCase().indexOf("marginals=") > -1){
            	ParseResponse(sAction, Response, ID_DETAILS);
            }
            else{
                ParseResponse(sAction, Response, ID_SEARCH_BY_PARCEL);
            }
        break;
        default:
            break;
        }

    }
    
   	public Collection<ParsedResponse> smartParseNamesSummary(ServerResponse response, String table, StringBuilder outputTable) {
		Vector<ParsedResponse> intermediaryResponse = new Vector<ParsedResponse>();
		
		/**
		 * We need to find what was the original search module
		 * in case we need some info from it like in the new PS interface
		 */
		TSServerInfoModule moduleSource = null;
		Object objectModuleSource = response.getParsedResponse().getAttribute(TSServerInfoConstants.EXTRA_PARAM_MODULE_OBJECT_SOURCE);
		if (objectModuleSource != null) {
			if(objectModuleSource instanceof TSServerInfoModule) {
				moduleSource = (TSServerInfoModule) objectModuleSource;
			} 
		} else {
			objectModuleSource = getSearch().getAdditionalInfo(this.getKeyForSavingInIntermediaryNextLink(response.getQuerry()));
			if (objectModuleSource instanceof TSServerInfoModule) {
				moduleSource = (TSServerInfoModule) objectModuleSource;
			}
		}
		
		try {
			table = table.replaceAll("(?is)(<td[^>]*>)\\s*(</td>)", "$1&nbsp;$2");
			org.htmlparser.Parser htmlParser = org.htmlparser.Parser.createParser(table, null);
			
			StringBuilder newTable = new StringBuilder();
			newTable.append("<table BORDER='1' CELLPADDING='2'>");
			String action = "";
			StringBuilder hiddenInputs = new StringBuilder();
			NodeList mainFormList = htmlParser.parse(new TagNameFilter("form"));
			if (mainFormList != null && mainFormList.size() > 0){
				FormTag mainForm = (FormTag) mainFormList.elementAt(0);
				if (mainForm != null){
					action += mainForm.getAttribute("action");
					NodeList inputsList = mainForm.getFormInputs();
					if (inputsList != null){
						StringBuilder linkDetail = new StringBuilder();
						linkDetail.append(CreatePartialLink(TSConnectionURL.idPOST)).append(action).append("?");
						for (int i = 0; i < inputsList.size(); i++) {
							InputTag input = (InputTag) inputsList.elementAt(i);
							String type = input.getAttribute("type");
							if (StringUtils.isNotBlank(type)){
								if ("hidden".equalsIgnoreCase(type)){
									hiddenInputs.append(input.toHtml()).append("\r\n");
									linkDetail.append(input.getAttribute("name")).append("=").append(input.getAttribute("value")).append("&");
								} 
							}
						}
						for (int i = 0; i < inputsList.size(); i++) {
							InputTag input = (InputTag) inputsList.elementAt(i);
							String type = input.getAttribute("type");
							if (StringUtils.isNotBlank(type)){		
								if ("checkbox".equalsIgnoreCase(type)){
									StringBuilder linkToDetail = new StringBuilder(linkDetail);
									linkToDetail.append("SUBMIT=Detail Data").append("&Names=").append(input.getAttribute("Value"));
									String link = "<a href=\"" + linkToDetail.toString() + "\" title=\"Detail Data\">";
									ParsedResponse currentResponse = new ParsedResponse();
									StringBuilder row = new StringBuilder();
									String grantor = input.getNextSibling().getText();
									row.append("<tr><td>").append(input.toHtml()).append(link).append(grantor).append("</a></td></tr>");
									
									ResultMap resultMap = new ResultMap();
									resultMap.put("tmpPartyGtor", grantor.replaceAll("(?is)\\(\\d+\\)", ""));
									resultMap.put(OtherInformationSetKey.SRC_TYPE.getKeyName(), getDataSite().getSiteTypeAbrev());
									try {
										parseNamesRO(resultMap, searchId);
									} catch (Exception e) {
										e.printStackTrace();
									}
									resultMap.removeTempDef();
						    				
									currentResponse.setUseDocumentForSearchLogRow(true);
									
									Bridge bridge = new Bridge(currentResponse,resultMap,getSearch().getID());
									RegisterDocumentI document = (RegisterDocumentI) bridge.importData();
									
									if (moduleSource != null){
										document.setSearchType(SearchType.valueOf(moduleSource.getSearchType()));
									}
									LinkInPage linkInPage = new LinkInPage(linkToDetail.toString(), linkToDetail.toString(), TSServer.REQUEST_GO_TO_LINK_REC);
									currentResponse.setPageLink(linkInPage);
									
									currentResponse.setDocument(document);
									currentResponse.setOnlyResponse(row.toString());
									newTable.append(currentResponse.getResponse());
									intermediaryResponse.add(currentResponse);
								}
							}
						}
					}
				}
			}
			newTable.append("</table>");
			outputTable.append(newTable);
			
			String header1 = "<TH colspan=\"2\" width=\"5%\" align=\"justify\">Place a Check Mark by the names you would like more detailed information on.</TH>";
			
			response.getParsedResponse().setHeader(CreateSummaryOrDetailFormHeader("GET", action, hiddenInputs)
								+ "<table width=\"100%\" border=\"1\" cellspacing=\"0\" cellpadding=\"2\">\n" + header1);
				
			response.getParsedResponse().setFooter("</table>" +  CreateSummaryOrDetailFormEnd(SAVE_SELECTED_DOCUMENTS_BUTTON_LABEL, 101));
		}catch(Exception e) {
			e.printStackTrace();
		}
		
		return intermediaryResponse;
	}
   	
   	protected String CreateSummaryOrDetailFormHeader(String method, String action, StringBuilder hiddenInputs){
    	String s = "<form name=\"SummaryOrDetail\" id=\"SummaryOrDetail\" action= \"" + CreatePartialLink(TSConnectionURL.idGET) + "\"" + " method=\"" + method + "\" > "
                + new LinkParser(CreatePartialLink(TSConnectionURL.idGET) + action).toStringParam("<input TYPE='hidden' NAME='", "' VALUE='", "'>\n")
                + hiddenInputs;
    	return s;
    }
   	
   	protected String CreateSummaryOrDetailFormEnd(String name, int parserId) {
    	        
        String s = 
        		"<input type=\"submit\" class=\"button\" name=\"SUBMIT\" value=\"Summary Data\" >" +
        		"<input type=\"submit\" class=\"button\" name=\"SUBMIT\" value=\"Detail Data\" >\r\n";
        
        
        return s + "</form>\n";
	}
   	
   	public Collection<ParsedResponse> smartParseSummaryData(ServerResponse response, String table, StringBuilder outputTable, boolean useSummaryDataAsDetail) {
		LinkedHashMap<String, ParsedResponse> responses = new LinkedHashMap<String, ParsedResponse>();
		Vector<ParsedResponse> intermediaryResponse = new Vector<ParsedResponse>();
		
		Search search = this.getSearch();
		searchId = search.getID();
		
		/**
		 * We need to find what was the original search module
		 * in case we need some info from it like in the new PS interface
		 */
		TSServerInfoModule moduleSource = null;
		Object objectModuleSource = response.getParsedResponse().getAttribute(TSServerInfoConstants.EXTRA_PARAM_MODULE_OBJECT_SOURCE);
		if (objectModuleSource != null) {
			if(objectModuleSource instanceof TSServerInfoModule) {
				moduleSource = (TSServerInfoModule) objectModuleSource;
			} 
		} else {
			objectModuleSource = search.getAdditionalInfo(this.getKeyForSavingInIntermediaryNextLink(response.getQuerry()));
			if (objectModuleSource instanceof TSServerInfoModule) {
				moduleSource = (TSServerInfoModule) objectModuleSource;
			}
		}
		
		try {
			table = table.replaceAll("(?is)(<td[^>]*>)\\s*(</td>)", "$1&nbsp;$2");
			org.htmlparser.Parser htmlParser = org.htmlparser.Parser.createParser(table, null);
			
			int numberOfUncheckedElements = 0;
			StringBuilder newTable = new StringBuilder();
			newTable.append("<table BORDER='1' CELLPADDING='2'>");
			NodeList formList = htmlParser.parse(new TagNameFilter("form"));
			
			String[] tables = table.split("<hr>");
			for (String tabel : tables) {
				if (StringUtils.isNotBlank(tabel)){
					org.htmlparser.Parser tableParser = org.htmlparser.Parser.createParser(tabel, null);
					NodeList mainTableList = tableParser.parse(new TagNameFilter("table"));
					if (mainTableList != null && mainTableList.size() > 0){
						TableTag mainTable = (TableTag) mainTableList.elementAt(0);
				
						String instr = "";
						Text instrumentNode = HtmlParser3.findNode(mainTable.getChildren(), "Instr:");
						if (instrumentNode != null && instrumentNode.getParent() != null){
							TableColumn tc = (TableColumn) instrumentNode.getParent();
							instr = HtmlParser3.getValueFromCell(tc, "", false);
							instr = instr.replaceAll("(?is)Instr:", "").replaceAll("(?is)&nbsp;", " ").trim();
						}
						
						String bookPage = "";
						Text bookPageNode = HtmlParser3.findNode(mainTable.getChildren(), "Vol/Page:");
						if (bookPageNode != null && bookPageNode.getParent() != null){
							TableColumn tc = (TableColumn) bookPageNode.getParent();
							bookPage = HtmlParser3.getValueFromCell(tc, "", false);
							bookPage = bookPage.replaceAll("(?is)Vol/Page:", "").replaceAll("(?is)&nbsp;", " ").trim();
						}
						
						String recDate = "";
						Text recDateNode = HtmlParser3.findNode(mainTable.getChildren(), "Rec:");
						if (recDateNode != null && recDateNode.getParent() != null){
							TableColumn tc = (TableColumn) recDateNode.getParent();
							recDate = HtmlParser3.getValueFromCell(tc, "", false);
							recDate = recDate.replaceAll("(?is)Rec:", "").replaceAll("(?is)&nbsp;", " ").trim();
						}
						
						String serverDocType = "";
						Text serverDocTypeNode = HtmlParser3.findNode(mainTable.getChildren(), "Type:");
						if (serverDocTypeNode != null && serverDocTypeNode.getParent() != null){
							TableColumn tc = (TableColumn) serverDocTypeNode.getParent();
							serverDocType = HtmlParser3.getValueFromCell(tc, "", false);
							serverDocType = serverDocType.replaceAll("(?is)Type:", "").replaceAll("(?is)&nbsp;", " ").trim();
						}
						
						String grantors = HtmlParser3.getValueFromNearbyCell(2, HtmlParser3.findNode(mainTable.getChildren(), "Grantor:"), "", true);
						if (StringUtils.isEmpty(grantors)){
							grantors = HtmlParser3.getValueFromNearbyCell(2, HtmlParser3.findNode(mainTable.getChildren(), "Debtor:"), "", true);
						}
						String grantees = HtmlParser3.getValueFromNearbyCell(2, HtmlParser3.findNode(mainTable.getChildren(), "Grantee:"), "", true);
						if (StringUtils.isEmpty(grantees)){
							grantees = HtmlParser3.getValueFromNearbyCell(2, HtmlParser3.findNode(mainTable.getChildren(), "Secured Party:"), "", true);
						}
						
						String legalDesc = HtmlParser3.getValueFromNearbyCell(0, HtmlParser3.findNode(mainTable.getChildren(), "Legal:"), "", true);
						legalDesc = legalDesc.replaceAll("(?is)Legal:", "").replaceAll("(?is)</?i>", "").replaceAll("(?is)<br>", ", ")
												.replaceAll("(?is)</?b>", "");
							
						String key = instr + "_" + serverDocType.replaceAll("\\s+", "_");
		
						ParsedResponse currentResponse = responses.get(key);
						if (currentResponse == null) {
							currentResponse = new ParsedResponse();
							responses.put(key, currentResponse);
						}
						StringBuilder imageLink = new StringBuilder();
						Text nodeDisplayDoc = HtmlParser3.findNode(mainTable.getChildren(), "Display Doc");
						if (nodeDisplayDoc != null){
							if (nodeDisplayDoc.getParent() != null){
								Node parentNode = nodeDisplayDoc.getParent();
								if (parentNode instanceof LinkTag){
									String link = ((LinkTag) parentNode).getLink();
									
									imageLink.append(CreatePartialLink(TSConnectionURL.idGET)).append(link);
									
									currentResponse.addImageLink(new ImageLinkInPage(imageLink.toString(), instr + ".tif"));
								}
							}
						}
						RegisterDocumentI document = (RegisterDocumentI)currentResponse.getDocument();				
						
						ResultMap resultMap = new ResultMap();
								
						StringBuilder linkDetailData = new StringBuilder();
						if (formList != null && formList.size() > 0){
							FormTag form = (FormTag) formList.elementAt(0);
							if (form != null){
//								linkDetailData.append(CreatePartialLink(TSConnectionURL.idPOST));
								String action = form.getAttribute("action");
								if (StringUtils.isNotBlank(action)){
									linkDetailData.append(action).append("?");
								}
								NodeList inputs = form.getFormInputs();
								if (inputs != null){
									for (int i = 0; i < inputs.size(); i++) {
										InputTag input = (InputTag) inputs.elementAt(i);
										String type = input.getAttribute("type");
										if (StringUtils.isNotBlank(type) && "hidden".equalsIgnoreCase(type)){
											String name = input.getAttribute("name");
											String value = input.getAttribute("value");
											if (name.equalsIgnoreCase("UserQuery")){
												Map<String,String> params = new HashMap<String, String>();
												params.put(name, value);
												int seq = getSeq();
												
												mSearch.setAdditionalInfo(getCurrentServerName() + ":UserQuery:" + seq, params);
												linkDetailData.append(name).append("=").append(seq).append("&");
												continue;
											}
											linkDetailData.append(name).append("=").append(value).append("&");
										}
									}
								}
							}
						}
						linkDetailData.append("SUBMIT=Detail Data");
						if (StringUtils.isNotBlank(instr)){
							linkDetailData.append("&Instrs=").append(instr);
						}
						String link = CreatePartialLink(TSConnectionURL.idPOST) + linkDetailData.toString();
						if (document == null) {	//first time we find this document
									
							String rowHtml =  mainTable.toHtml();
							rowHtml = rowHtml.replaceAll("(?is)<input[^>]*>", "");
							//"<a href=\"" + link + "\">Detail Data</a>"
							if (useSummaryDataAsDetail){
								rowHtml = rowHtml.replaceAll("(?is)(<b>\\s*Instr\\s*:\\s*</b>)\\s*<a href=[^>]+>([^<]+)</a>\\s*(</td>)", "$1$2$3");
								rowHtml = rowHtml.replaceAll("(?is)(<b>\\s*Vol/Page\\s*:\\s*</b>)\\s*<a href=[^>]+>([^<]+)</a>\\s*(</td>)", "$1$2$3");
							} else{
								rowHtml = rowHtml.replaceAll("(?is)(<b>\\s*Instr\\s*:\\s*</b>)([^<]+)(</td>)", "$1<a href=\"" + link + "\">$2</a>$3");
								rowHtml = rowHtml.replaceAll("(?is)(<b>\\s*Vol/Page\\s*:\\s*</b>)([^<]+)(</td>)", "$1<a href=\"" + link + "\">$2</a>$3");
							}
									
							resultMap.put("tmpPartyGtor", grantors);
							resultMap.put("tmpPartyGtee", grantees);
							resultMap.put(OtherInformationSetKey.SRC_TYPE.getKeyName(), getDataSite().getSiteTypeAbrev());
							resultMap.put(SaleDataSetKey.INSTRUMENT_NUMBER.getKeyName(), instr);
							String[] bp = bookPage.split("\\s+");
							if (bp.length == 2){
								resultMap.put(SaleDataSetKey.BOOK.getKeyName(), bp[0]);
								resultMap.put(SaleDataSetKey.PAGE.getKeyName(), bp[1]);
							} else if (bp.length == 3){
								resultMap.put(SaleDataSetKey.BOOK.getKeyName(), bp[0] + bp[1]);
								resultMap.put(SaleDataSetKey.PAGE.getKeyName(), bp[2]);
							}
							resultMap.put(SaleDataSetKey.DOCUMENT_TYPE.getKeyName(), serverDocType);
							resultMap.put(SaleDataSetKey.RECORDED_DATE.getKeyName(), recDate.replaceAll("(?is)\\A\\s*([\\d/]+)\\s+.*", "$1").trim());
							try {
								parseNamesRO(resultMap, searchId);
							} catch (Exception e) {
								e.printStackTrace();
							}
							resultMap.removeTempDef();
				    				
							currentResponse.setUseDocumentForSearchLogRow(true);
							
							Bridge bridge = new Bridge(currentResponse,resultMap,getSearch().getID());
							document = (RegisterDocumentI) bridge.importData();
							
							if (moduleSource != null){
								document.setSearchType(SearchType.valueOf(moduleSource.getSearchType()));
							}
							
							try {
								parseLegalSummaryData(legalDesc, document, searchId);
							} catch (Exception e) {
								e.printStackTrace();
							}
							
							currentResponse.setDocument(document);
							String checkBox = "checked";
							if (isAlreadySaved(instr, document) && !Boolean.TRUE.equals(getSearch().getAdditionalInfo("RESAVE_DOCUMENT"))) {
								checkBox = "saved";
							} else {
								numberOfUncheckedElements++;
								
								if (useSummaryDataAsDetail){
									LinkInPage linkInPage = response.getParsedResponse().getPageLink();
									if (linkInPage != null){
										linkInPage.setActionType(TSServer.REQUEST_SAVE_TO_TSD);
										link = linkInPage.getLink();
										currentResponse.setPageLink(linkInPage);
									}
								} else{
									LinkInPage linkInPage = new LinkInPage(link, link, TSServer.REQUEST_GO_TO_LINK_REC);
									currentResponse.setPageLink(linkInPage);
								}
								checkBox = "<input type='checkbox' name='docLink' value='" + link + "'>Select for saving to TS Report";
								/**
								 * Save module in key in additional info. The key is instrument number that should be always available. 
								 */
								String keyForSavingModules = this.getKeyForSavingInIntermediary(instr);
								search.setAdditionalInfo(keyForSavingModules, moduleSource);
							}
							
							if (getSearch().getSearchType() == Search.PARENT_SITE_SEARCH ){
								rowHtml = rowHtml.replaceFirst( "(?is)<a href([^>]*)>Display Doc</a>" , "<A id=\"imageLink\" target=\"_blank\" HREF=\"" + imageLink.toString() + "\">Display Doc</A>");
							
							}
//							if (useSummaryDataAsDetail){
//								currentResponse.setOnlyResponse(rowHtml);
//								mSearch.addInMemoryDoc(link, currentResponse);
//							} 
//							else{
								mSearch.addInMemoryDoc(linkDetailData + "&UseSummaryData=true", currentResponse);
								mSearch.addInMemoryDoc(linkDetailData + "&UseSummaryDataResult=true", rowHtml);
//							}
							
							rowHtml = rowHtml.replaceFirst("(?is)</TR></Table>", 
									"</TR><TR><TD COLSPAN='100'>" + checkBox + "</TD></TR><TR><TD COLSPAN='100'><hr></TD></TR></table>");
							currentResponse.setOnlyResponse(rowHtml);
							newTable.append(currentResponse.getResponse());
							intermediaryResponse.add(currentResponse);
						}
			
						newTable.append("</table>");
						outputTable.append(newTable);
						SetAttribute(NUMBER_OF_UNSAVED_DOCUMENTS, numberOfUncheckedElements);
					}
				}
			}
				
			String header1 = "<TH width=\"5%\" align=\"justify\">" + SELECT_ALL_CHECKBOXES + "Check\\Uncheck All</TH>";
			
			response.getParsedResponse().setHeader(CreateSaveToTSDFormHeader(URLConnectionReader.MULTIPLE_SAVE_TO_TSD_ACTION, "GET") 
								+ "<table width=\"100%\" border=\"1\" cellspacing=\"0\" cellpadding=\"2\">\n" + header1);
				
			response.getParsedResponse().setFooter("</table>" +  CreateSaveToTSDFormEnd(SAVE_SELECTED_DOCUMENTS_BUTTON_LABEL, 101, -1));
		}catch(Exception e) {
			e.printStackTrace();
		}
		
		return intermediaryResponse;
	}
   	
   	public Collection<ParsedResponse> smartParseDetailData(ServerResponse response, String table, StringBuilder outputTable) {
		LinkedHashMap<String, ParsedResponse> responses = new LinkedHashMap<String, ParsedResponse>();
		Vector<ParsedResponse> intermediaryResponse = new Vector<ParsedResponse>();
		
		Search search = this.getSearch();
		searchId = search.getID();
		
		/**
		 * We need to find what was the original search module
		 * in case we need some info from it like in the new PS interface
		 */
		TSServerInfoModule moduleSource = null;
		Object objectModuleSource = response.getParsedResponse().getAttribute(TSServerInfoConstants.EXTRA_PARAM_MODULE_OBJECT_SOURCE);
		if (objectModuleSource != null) {
			if (objectModuleSource instanceof TSServerInfoModule) {
				moduleSource = (TSServerInfoModule) objectModuleSource;
			} 
		} else {
			objectModuleSource = search.getAdditionalInfo(this.getKeyForSavingInIntermediaryNextLink(response.getQuerry()));
			if (objectModuleSource instanceof TSServerInfoModule) {
				moduleSource = (TSServerInfoModule) objectModuleSource;
			}
		}
		
		try {
			table = table.replaceAll("(?is)(<td[^>]*>)\\s*(</td>)", "$1&nbsp;$2");
			org.htmlparser.Parser htmlParser = org.htmlparser.Parser.createParser(table, null);
			NodeList formList = htmlParser.parse(new TagNameFilter("form"));
			
			int numberOfUncheckedElements = 0;
			StringBuilder newTable = new StringBuilder();
			newTable.append("<table BORDER='1' CELLPADDING='2'>");
			String prevLink = "", nextLink = "";
			
			String[] tables = table.split("<hr>");
			for (String tabel : tables) {
				if (StringUtils.isNotBlank(tabel)){
					org.htmlparser.Parser tableParser = org.htmlparser.Parser.createParser(tabel, null);
					NodeList mainTableList = tableParser.parse(new TagNameFilter("table"));
					if (mainTableList != null && mainTableList.size() > 0){
						TableTag mainTable = (TableTag) mainTableList.elementAt(0);
				
						String instr = "";
						Text instrumentNode = HtmlParser3.findNode(mainTable.getChildren(), "Instrument:");
						if (instrumentNode != null && instrumentNode.getParent() != null){
							TableColumn tc = (TableColumn) instrumentNode.getParent();
							instr = HtmlParser3.getValueFromCell(tc, "", false);
							instr = instr.replaceAll("(?is)Instrument:", "").replaceAll("(?is)&nbsp;", " ").trim();
						}
						
						String bookPage = "";
						Text bookPageNode = HtmlParser3.findNode(mainTable.getChildren(), "Volume Page:");
						if (bookPageNode != null && bookPageNode.getParent() != null){
							TableColumn tc = (TableColumn) bookPageNode.getParent();
							bookPage = HtmlParser3.getValueFromCell(tc, "", false);
							bookPage = bookPage.replaceAll("(?is)Volume Page:", "").replaceAll("(?is)&nbsp;", " ").trim();
						}
						
						String recDate =  "";
						Text recordedNode = HtmlParser3.findNode(mainTable.getChildren(), "Recorded:");
						if (recordedNode != null && recordedNode.getParent() != null){
							TableColumn tc = (TableColumn) recordedNode.getParent();
							recDate = HtmlParser3.getValueFromCell(tc, "", false);
							recDate = recDate.replaceAll("(?is)Recorded:", "").replaceAll("(?is)&nbsp;", " ").trim();
						}
						
						String consideration = "";
						Text considerationNode = HtmlParser3.findNode(mainTable.getChildren(), "Consideration:");
						if (considerationNode != null && considerationNode.getParent() != null){
							TableColumn tc = (TableColumn) considerationNode.getParent();
							consideration = HtmlParser3.getValueFromCell(tc, "", false);
							consideration = consideration.replaceAll("(?is)Consideration:", "").replaceAll("(?is)&nbsp;", " ").replaceAll("[$,]+", "").trim();
						}

						String serverDocType = "";
						Text serverDoctypeNode = HtmlParser3.findNode(mainTable.getChildren(), "Document Type:");
						if (serverDoctypeNode != null && serverDoctypeNode.getParent() != null){
							TableColumn tc = (TableColumn) serverDoctypeNode.getParent();
							serverDocType = HtmlParser3.getValueFromCell(tc, "", false);
							serverDocType = serverDocType.replaceAll("(?is)Document Type:", "").replaceAll("(?is)&nbsp;", " ").trim();
						}
						
						Text documentDateNode = HtmlParser3.findNode(mainTable.getChildren(), "Document Date:");
						String instrDate = "";
						if (documentDateNode != null && documentDateNode.getParent() != null){
							TableColumn tc = (TableColumn) documentDateNode.getParent();
							instrDate = HtmlParser3.getValueFromCell(tc, "", false);
							instrDate = instrDate.replaceAll("(?is)Document Date:", "").replaceAll("(?is)&nbsp;", " ").trim();
						}
						
						String grantors = HtmlParser3.getValueFromNearbyCell(2, HtmlParser3.findNode(mainTable.getChildren(), "Grantor:"), "", true);
						if (StringUtils.isEmpty(grantors)){
							grantors = HtmlParser3.getValueFromNearbyCell(2, HtmlParser3.findNode(mainTable.getChildren(), "Debtor:"), "", true);
							if (StringUtils.isEmpty(grantors)){
								grantors = HtmlParser3.getValueFromNearbyCell(2, HtmlParser3.findNode(mainTable.getChildren(), "Plat:"), "", true);
								grantors = grantors.replaceAll("(?is)</?nobr>", "").replaceAll("(?is)<br>", " / ");
								grantors = grantors.replaceAll("(?is)\\s*/\\s*", " / ");
							}
						}
						grantors = grantors.replaceAll("(?is)</?nobr>", "").replaceAll("(?is)<br>", " / ");
						
						String grantees = HtmlParser3.getValueFromNearbyCell(2, HtmlParser3.findNode(mainTable.getChildren(), "Grantee:"), "", true);
						if (StringUtils.isEmpty(grantees)){
							grantees = HtmlParser3.getValueFromNearbyCell(2, HtmlParser3.findNode(mainTable.getChildren(), "Secured Party:"), "", true);
							if (StringUtils.isEmpty(grantees)){
								grantees = HtmlParser3.getValueFromNearbyCell(2, HtmlParser3.findNode(mainTable.getChildren(), "Developer Name:"), "", true);
							}
						}
						String marginal = HtmlParser3.getValueFromNextCell(mainTableList, "Marginal:", "", true);
						
						String legalDesc = HtmlParser3.getValueFromNextCell(mainTableList, "Legal Description:", "", true);
						legalDesc = legalDesc.replaceAll("(?is)</?i>", "").replaceAll("(?is)<br>", ", ");
							
						String key = instr + "_" + serverDocType.replaceAll("\\s+", "_");
						if (StringUtils.isBlank(instr)){
							key = bookPage + "_" + serverDocType.replaceAll("\\s+", "_");
						}
		
						ParsedResponse currentResponse = responses.get(key);							 
						if (currentResponse == null) {
							currentResponse = new ParsedResponse();
							responses.put(key, currentResponse);
						}
						StringBuilder imageLink = new StringBuilder();
						Text nodeDisplayDoc = HtmlParser3.findNode(mainTable.getChildren(), "Display Doc");
						if (nodeDisplayDoc != null){
							if (nodeDisplayDoc.getParent() != null){
								Node parentNode = nodeDisplayDoc.getParent();
								if (parentNode instanceof LinkTag){
									String link = ((LinkTag) parentNode).getLink();
									
									imageLink.append(CreatePartialLink(TSConnectionURL.idGET)).append(link);
									
									currentResponse.addImageLink(new ImageLinkInPage(imageLink.toString(), instr + ".tif"));
								}
							}
						}
								
						StringBuilder linkDetailData = new StringBuilder();
						if (formList != null && formList.size() > 0){
							Node pageMenuNode = formList.extractAllNodesThatMatch(new HasAttributeFilter("id", "PageMenu"), true).elementAt(0);
							FormTag pageMenuForm = (FormTag) pageMenuNode;
							if (pageMenuForm != null){
								String action = pageMenuForm.getAttribute("action");
								String link = CreatePartialLink(TSConnectionURL.idPOST) + action + "?";
								linkDetailData.append(link);
								
								NodeList inputs = pageMenuForm.getFormInputs();
								if (inputs != null){
									Map<String,String> paramsForNav = new HashMap<String, String>();
									for (int j = 0; j < inputs.size(); j++){
										InputTag input = (InputTag) inputs.elementAt(j);
										if ("hidden".equals(input.getAttribute("type"))){
											if (input.getAttribute("name") != null){
												if (input.getAttribute("value") != null){
													paramsForNav.put(input.getAttribute("name"), input.getAttribute("value"));
												} else {
													paramsForNav.put(input.getAttribute("name"), "");
												}
											}
										} else if ("submit".equals(input.getAttribute("type"))){
											if (input.getAttribute("name") != null){
												String submit = input.getAttribute("value");
												Matcher m = Pattern.compile("Detail Data \\d*-\\?").matcher(submit);
												if (m.find()) {
													nextLink = "<a href=\"" + link + "navig=Next&submit=" + submit + "\">Next</a>";
													response.getParsedResponse().setNextLink(nextLink);
												} else {
													prevLink = "<a href=\"" + link + "navig=Prev&submit=" + submit + "\">Previous</a>";
												}
											} 
											
										}
									}
									if (!paramsForNav.isEmpty()){
										mSearch.setAdditionalInfo(getCurrentServerName() + ":paramsNav:", paramsForNav);
									}
								}
							}
						}
						linkDetailData.append("SUBMIT=Detail Data");
						if (StringUtils.isNotBlank(key)){
							linkDetailData.append("&Instrs=").append(key);
						}
						String link = linkDetailData.toString();
						
						RegisterDocumentI document = (RegisterDocumentI)currentResponse.getDocument();				
						ResultMap resultMap = new ResultMap();
						
						if (StringUtils.isNotBlank(marginal)){
							String[] marginals = marginal.split("\\s*,\\s*");
							@SuppressWarnings("rawtypes")
							List<List> body = new ArrayList<List>();
							List<String> line = null;
							for (String crossref : marginals) {
								line = new ArrayList<String>();
								crossref = crossref.replaceAll("(?is)</?a[^>]*>", "").replaceAll("(?is)</?i[^>]*>", "")
													.replaceAll("(?is)</?nobr[^>]*>", "");
//								crossref = crossref.replaceAll("(?is)\\(\\w+\\)$", "");
								
								Matcher mat = Pattern.compile("(?is)(?:Bkwd|Fwd)\\s+(?:[A-Z]\\s+)?(\\d{2,5})\\w{3}(?!\\s*\\d)").matcher(crossref);
								if (mat.find()){
									line.add("");
									String book = crossref.replaceAll("(?is)(?:Bkwd|Fwd)\\s+(?:[A-Z]\\s+)?(\\d{2,5})\\w{3}(?!\\s*\\d)", "$1");
									book = book.replaceAll("(?is)\\(\\w+\\)$", "");
									line.add(book.trim());
									String page = crossref.replaceAll("(?is)(?:Bkwd|Fwd)\\s+(?:[A-Z]\\s+)?(\\d{2,5})(\\w{3}(?!\\s*\\d))", "$2");
									page = page.replaceAll("(?is)\\(\\w+\\)$", "");
									line.add(page.trim());
									line.add("");
									line.add("");
									line.add("");
								} else{
									mat = Pattern.compile("(?is)(?:Bkwd|Fwd)\\s+(?:(?:I|M)\\s+)?(\\w{7,}).*").matcher(crossref);
									if (mat.find()){
										crossref = crossref.replaceAll("(?is)(?:Bkwd|Fwd)\\s+(?:(?:I|M)\\s+)?(\\w{7,}).*", "$1");
										crossref = crossref.replaceAll("(?is)\\(\\w+\\)$", "");
										line.add(crossref.trim());
										line.add("");
										line.add("");
										line.add("");
										line.add("");
										line.add("");
									}
								}
								if (!line.isEmpty()){
									body.add(line);
								}
							}
							if (!body.isEmpty()){
								String[] header = { "InstrumentNumber", "Book", "Page", "Month", "Day", "Year"};
								resultMap.put("CrossRefSet", GenericFunctions2.createResultTable(body, header));
								GenericFunctions1.crossRefTokenizeOHFranklinRO(resultMap, searchId);
							}
						}
						if (document == null) {	//first time we find this document
							String rowHtml =  mainTable.toHtml();
									
							resultMap.put("tmpPartyGtor", grantors);
							resultMap.put("tmpPartyGtee", grantees);
							resultMap.put(OtherInformationSetKey.SRC_TYPE.getKeyName(), getDataSite().getSiteTypeAbrev());
							resultMap.put(SaleDataSetKey.INSTRUMENT_NUMBER.getKeyName(), instr);
							bookPage = bookPage.replaceFirst("(?is)\\A\\s*(\\d+)([A-Z]\\d+)\\s*$", "$1 $2");
							String[] bp = bookPage.split("\\s+");
							if (bp.length == 2){
								resultMap.put(SaleDataSetKey.BOOK.getKeyName(), bp[0]);
								resultMap.put(SaleDataSetKey.PAGE.getKeyName(), bp[1]);
							}
							resultMap.put(SaleDataSetKey.DOCUMENT_TYPE.getKeyName(), serverDocType);
							resultMap.put(SaleDataSetKey.RECORDED_DATE.getKeyName(), recDate.replaceAll("(?is)\\A\\s*([\\d/]+)\\s+.*", "$1").trim());
							resultMap.put(SaleDataSetKey.CONSIDERATION_AMOUNT.getKeyName(), consideration);
							resultMap.put(SaleDataSetKey.MORTGAGE_AMOUNT.getKeyName(), consideration);
							
							try {
								parseNamesRO(resultMap, searchId);
							} catch (Exception e) {
								e.printStackTrace();
							}
							resultMap.removeTempDef();
				    				
							currentResponse.setAttribute(ParsedResponse.SERVER_ROW_RESPONSE, rowHtml);
							currentResponse.setUseDocumentForSearchLogRow(true);
																	
							Bridge bridge = new Bridge(currentResponse,resultMap,getSearch().getID());
							document = (RegisterDocumentI) bridge.importData();
							
							if (moduleSource != null){
								document.setSearchType(SearchType.valueOf(moduleSource.getSearchType()));
							} else if (response.getQuerry().contains("submit=Detail Data ")){
								document.setSearchType(SearchType.CS);
							}
							
							try {
								parseLegalDetailData(legalDesc, document, searchId);
							} catch (Exception e) {
								e.printStackTrace();
							}
									
							currentResponse.setDocument(document);
							String checkBox = "checked";
									
							if (isAlreadySaved(instr, document) && !Boolean.TRUE.equals(getSearch().getAdditionalInfo("RESAVE_DOCUMENT"))) {
								checkBox = "saved";
							} else {
								numberOfUncheckedElements++;
								LinkInPage linkInPage = new LinkInPage(link, link,TSServer.REQUEST_SAVE_TO_TSD);
								currentResponse.setPageLink(linkInPage);
								checkBox = "<input type='checkbox' name='docLink' value='" + link + "'>Select for saving to TS Report";
								/**
								 * Save module in key in additional info. The key is instrument number that should be always available. 
								 */
								String keyForSavingModules = this.getKeyForSavingInIntermediary(instr);
								search.setAdditionalInfo(keyForSavingModules, moduleSource);
							}
							
							Matcher crossRefLinkMatcher = CROSSREF_PAT.matcher(rowHtml);
							while (crossRefLinkMatcher.find()) {
								ParsedResponse prChild = new ParsedResponse();
								String crossLink = CreatePartialLink(TSConnectionURL.idGET) + crossRefLinkMatcher.group(2) + "&isSubResult=true";
								LinkInPage pl = new LinkInPage(crossLink, crossLink, TSServer.REQUEST_SAVE_TO_TSD);
								prChild.setPageLink(pl);
								currentResponse.addOneResultRowOnly(prChild);
							}
//							crossRefLinkMatcher = MARGINAL_PAT.matcher(rowHtml);
//							while (crossRefLinkMatcher.find()) {
//								ParsedResponse prChild = new ParsedResponse();
//								String crossLink = CreatePartialLink(TSConnectionURL.idGET) + crossRefLinkMatcher.group(2) + "&isSubResult=true";
//								LinkInPage pl = new LinkInPage(crossLink, crossLink, TSServer.REQUEST_SAVE_TO_TSD);
//								prChild.setPageLink(pl);
//								currentResponse.addOneResultRowOnly(prChild);
//							}
							rowHtml = rowHtml.replaceAll("(?is)(\\bhref=[\\\"|'])(simplequery\\.asp\\?instrs=[^>]+[\\\"|']>)", "$1" + CreatePartialLink(TSConnectionURL.idGET) + "$2");
							rowHtml = rowHtml.replaceAll("(?is)(\\bhref=[\\\"|'])(simplequery\\.asp\\?Marginals=[^>]+[\\\"|']>)", "$1" + CreatePartialLink(TSConnectionURL.idGET) + "$2");
							
							if (getSearch().getSearchType() == Search.PARENT_SITE_SEARCH ){
								rowHtml = rowHtml.replaceFirst("(?is)<a href([^>]*)>Display Doc</a>", "<A id=\"imageLink\" target=\"_blank\" HREF=\"" + imageLink.toString() + "\">Display Doc</A>");
							
							}
							mSearch.addInMemoryDoc(link, currentResponse);
							
							rowHtml = rowHtml.replaceFirst("(?is)</TR></Table>",
											"</TR><TR><TD COLSPAN='100'>" + checkBox + "</TD></TR><TR><TD COLSPAN='100'><hr></TD></TR></table>");
							currentResponse.setOnlyResponse(rowHtml);
//							currentResponse.setResponse(rowHtml.replaceAll("(?is)</?a[^>]*>", ""));
							newTable.append(currentResponse.getResponse());
							intermediaryResponse.add(currentResponse);
						}
			
						newTable.append("</table>");
						outputTable.append(newTable);
						SetAttribute(NUMBER_OF_UNSAVED_DOCUMENTS, numberOfUncheckedElements);
					}
				}
			}
			
			String header1 = "<TH width=\"5%\" align=\"justify\">" + SELECT_ALL_CHECKBOXES + "Check\\Uncheck All</TH>";
			
			response.getParsedResponse().setHeader(CreateSaveToTSDFormHeader(URLConnectionReader.MULTIPLE_SAVE_TO_TSD_ACTION, "GET") 
								+ "<br>" + prevLink + "&nbsp;&nbsp;&nbsp;" + nextLink + "<br><br>" 
					+ "<table width=\"100%\" border=\"1\" cellspacing=\"0\" cellpadding=\"2\">\n" + header1);
				
			response.getParsedResponse().setFooter("</table>" +  
							"<br>" + prevLink + "&nbsp;&nbsp;&nbsp;" + nextLink + "<br><br>" +  CreateSaveToTSDFormEnd(SAVE_SELECTED_DOCUMENTS_BUTTON_LABEL, 101, -1));
		}catch(Exception e) {
			e.printStackTrace();
		}
		
		return intermediaryResponse;
	}

   	@Override
   	public String specificClean(String htmlContent){
   		htmlContent = htmlContent.replaceAll("(?is)</?a[^>]*>", "");
   		htmlContent = htmlContent.replaceAll("(?is)<input[^>]*>\\s*Select for saving to TS Report", "");
   		htmlContent = htmlContent.replaceAll("(?is)<input[^>]*>", "");
    	return htmlContent;
    }
   	
    public static int seq = 0;
    synchronized public static int getSeq(){
    	return ++seq;
    }
    
    public String getFileNameFromLink(String link)
    {
    	link = link.toLowerCase();
        String fileName = "";
        int iStart = -1;
        int iEnd = -1;
        String[] possibleKeys = new String[]{"dummy=", "instrs=", "marginals="};
        
        for (int i = 0; i < possibleKeys.length; i++) {
        	iStart = link.indexOf( possibleKeys[i] );
            if( iStart >= 0 )
            {
                iStart = link.indexOf( "=", iStart ) + 1;
                
                iEnd = link.indexOf( "&", iStart );
                
                if( iEnd >= 0 )
                {
                    fileName = link.substring( iStart, iEnd );
                }
                else
                {
                    fileName = link.substring( iStart );
                }
                break;
            }
            
		}
        if ("".equals(fileName)){
        	fileName = "unique_" + getSeq();
        }
        
        return fileName + ".html";
    }
    
    /* Redefined SearchBy method only for Seach By Name module: if first name ends with "Jr", append this suffix to first name using "/" separator- bug #902 
     * E.g.: Claude A JR -> Claude A/JR; John JR -> John/JR
     */
    private static Pattern fnPat = Pattern.compile("(?i)(.+) JR"); 
	public ServerResponse SearchBy( TSServerInfoModule module, Object sd) throws ServerResponseException{
		//SearchAttributes sa = InstanceManager.getManager().getCurrentInstance(searchId).getCrtSearchContext().getSa();
		//Search search = InstanceManager.getManager().getCurrentInstance(searchId).getCrtSearchContext();
		
		List<TSServerInfoModule> modules = getMultipleModules(module, sd);
		if (modules.size() > 1){
			List<ServerResponse> serverResponses = new ArrayList<ServerResponse>();
			Vector<ParsedResponse> prs = new Vector<ParsedResponse>();
			boolean firstSearchBy = true;
			String header = "", footer = "";
			for (TSServerInfoModule mod : modules){
				if (verifyModule(mod)){

					if (mod.getModuleIdx() == TSServerInfo.ADV_SEARCH_MODULE_IDX){ 
						//B 902
			        	String value = mod.getFunction(1).getParamValue();
			        	Matcher m = fnPat.matcher(value);
			        	if(m.matches()){
			        		value = m.group(1) + "/JR";
			        	}        	
			        	mod.getFunction(1).setParamValue(value);
					}
					
					if (firstSearchBy){
						firstSearchBy = false;
					} else{
						mod.addExtraInformation(TSServerInfoConstants.EXTRA_PARAM_DO_NOT_REMOVE_IN_MEMORY_DOCS, true);
					}
					ServerResponse res = super.SearchBy(mod, sd);
					if (res != null){
						res.getParsedResponse().setAttribute(TSServerInfoConstants.EXTRA_PARAM_MODULE_OBJECT_SOURCE, mod);
						serverResponses.add(res);
						if (res.getParsedResponse().getResultRows().size() > 0){
							prs.addAll(res.getParsedResponse().getResultRows());
							header = res.getParsedResponse().getHeader();
							footer = res.getParsedResponse().getFooter();
						}
					}
				}
			}
			if (prs.size() > 0){
				ServerResponse serverResponse = new ServerResponse();
				serverResponse.getParsedResponse().setAttribute(TSServerInfoConstants.EXTRA_PARAM_MODULE_OBJECT_SOURCE, module);
				serverResponse.getParsedResponse().setResultRows(prs);
				serverResponse.setResult("");
				serverResponse.getParsedResponse().setHeader(header);
				serverResponse.getParsedResponse().setFooter(footer);
				solveHtmlResponse("", module.getParserID(), "SearchBy", serverResponse, serverResponse.getResult());
				
				return serverResponse;
			} else{
				return ServerResponse.createEmptyResponse();
			}
		} else{
			if (module.getModuleIdx() == TSServerInfo.ADV_SEARCH_MODULE_IDX){ 
				//B 902
	        	String value = module.getFunction(1).getParamValue();
	        	Matcher m = fnPat.matcher(value);
	        	if(m.matches()){
	        		value = m.group(1) + "/JR";
	        	}        	
	        	module.getFunction(1).setParamValue(value);
			}
        }        
       
        return super.SearchBy(module, sd);
    }
	
	private boolean verifyModule(TSServerInfoModule mod) {
    	
    	if (mod == null)
    		return false;
    	
    	if (mod.getModuleIdx() == TSServerInfo.INSTR_NO_MODULE_IDX) {
    		if (mod.getFunctionCount() > 2 
    				&& (StringUtils.isNotEmpty(mod.getFunction(0).getParamValue())
    						|| (StringUtils.isNotEmpty(mod.getFunction(1).getParamValue()) && StringUtils.isNotEmpty(mod.getFunction(2).getParamValue())))) {
				return true;
			} 
			return false;
		}
    	if (mod.getModuleIdx() == TSServerInfo.ADV_SEARCH_MODULE_IDX) {
    		if (mod.getFunctionCount() > 0 && StringUtils.isNotEmpty(mod.getFunction(0).getParamValue())) {
				return true;
			} 
			return false;
		}
    	System.err.println(this.getClass() + "I shouldn't be here!!!");
		return false;
	}

    public String getPrettyFollowedLink (String initialFollowedLnk){
    	if (initialFollowedLnk.matches("(?i).*[^a-z]+(instrs|marginals)[=]([0-9]+)[&=a-z]+crossrefSource[=]([a-z]*)[^a-z]*.*")){
					/*"Book 13676 Page 1504 which is a Court doc type has already been saved from a
					previous search in the log file."*/
    		String retStr = initialFollowedLnk.replaceFirst(
    				"(?i).*[^a-z]+(instrs|marginals)[=]([A-Z0-9-]+)[&=a-z]+crossrefSource[=]([a-z]*)[^a-z]*.*", 
    				"Instrument " + "$2" + " " + "$3" + 
    				" has already been processed from a previous search in the log file. ");
    		retStr =  "<br/><span class='followed'>"+retStr+"</span><br/>";
    		
    		return retStr;
    	} else if (initialFollowedLnk.matches("(?i).*[^a-z]+(instrs|marginals)[=]([0-9]+)[&=a-z]+isSubResult*.*")){
			String retStr = initialFollowedLnk.replaceFirst(
					"(?i).*[^a-z]+(instrs|marginals)[=]([0-9]+)[&=a-z]+isSubResult.*", 
					"Instrument $2 has already been processed from a previous search in the log file. ");
			retStr =  "<br/><span class='followed'>" + retStr + "</span><br/>";
	
			return retStr;
    	} else if (initialFollowedLnk.matches("(?i).*[^a-z]+(instrs|marginals)[=]([0-9]+)")){
			String retStr = initialFollowedLnk.replaceFirst(
					"(?i).*[^a-z]+(instrs|marginals)[=]([0-9]+).*", 
					"Instrument $2 has already been processed from a previous search in the log file. ");
			retStr =  "<br/><span class='followed'>" + retStr + "</span><br/>";
	
			return retStr;
    	} else if (initialFollowedLnk.matches("(?i).*[^a-z]+(instrs|marginals)[=]([^_]+).*")){
			String retStr = initialFollowedLnk.replaceFirst(
					"(?i).*[^a-z]+(instrs|marginals)[=]([^_]+).*", 
					"Instrument $2 has already been processed from a previous search in the log file. ");
			retStr =  "<br/><span class='followed'>" + retStr + "</span><br/>";
	
			return retStr;
    	}
    	return "<br/><span class='followed'>Link already followed</span>:" + initialFollowedLnk + "<br/>";
    }
	
	protected static HashMap<String, String> subdivException = new HashMap<String, String>();
	static{
		subdivException.put("MCGOWN", "MCGOWANS");
		subdivException.put("MCFADYENS ACRES", "MCFAYDENS ACRES");
		subdivException.put("BELLEVIEW AVE", "MCDOWELLS BELLEVIEW AVENUE");
		subdivException.put("ROSE HILL", "GUSTAVES OCHS ROSE HILL");
		subdivException.put("AUBURNDALE", "MCAULEY AUBURNDALE");
		subdivException.put("ELMWOOD", "MCCLELLANDS SUBD");		
		subdivException.put("DOTTERS PARCELS", "AUGUSTA AND JULIUS STUARTS PARSON AVENUE SUBD"); //B3623
	}
	
	protected static HashMap<Pattern, String> replaceException = new HashMap<Pattern, String>();
	static {
		replaceException.put(Pattern.compile("(\\d+)\\s*\\-\\s*(\\d+)"), "$1 & $2");
		replaceException.put(Pattern.compile("^MAC"), "MC");
		replaceException.put(Pattern.compile("^(LA|LE|DE|MAC|O|VAN|SAN|VON|ST|MC|DE(?: LA)?|DI|EL|DEL)\\s+(.*)"), "$1$2");
	}
	
	protected static Vector<String> subdivDeriv(String s){
		
		Vector<String> tmp = new Vector<String>();
		//original name
		tmp.add(s);
		//exceptions
		if (subdivException.get(s) != null){
			tmp.add(subdivException.get(s));
		}
		//replaceExceptions
		String s2;
		Iterator<Pattern> i = replaceException.keySet().iterator();
		while (i.hasNext()){
			Pattern p = i.next();
			Matcher ma = p.matcher(s);
			s2 = ma.replaceAll(replaceException.get(p));
			if (tmp.indexOf(s2) == -1){
				tmp.add(s2);
			}
		}
		return tmp;
	}
	
	@Override
	protected boolean isFromRO(String doc) {
		DocumentsManagerI docManager = mSearch.getDocManager();
		
		try {
			docManager.getAccess();
			
			if(docManager.containsInstrumentNo(doc.replace(".html", ""))) {
				return true;
			} 
			
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			try {
				docManager.releaseAccess();
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		
		return false;
	}
	
	@Override
	public TSServerInfoModule getRecoverModuleFrom(RestoreDocumentDataI restoreDocumentDataI) {
		if (restoreDocumentDataI == null) {
			return null;
		}
		
		TSServerInfoModule module = null;
		
		if (ro.cst.tsearch.utils.StringUtils.isNotEmpty(restoreDocumentDataI.getInstrumentNumber()) && restoreDocumentDataI.getRecordedDate() != null) {
			Calendar cal = Calendar.getInstance();
			cal.setTime(restoreDocumentDataI.getRecordedDate());
			
			module = getDefaultServerInfo().getModule(TSServerInfo.INSTR_NO_MODULE_IDX);
			module.forceValue(0, restoreDocumentDataI.getInstrumentNumber());
			module.forceValue(3,"Summary Data");
			return module;
		}
		
		String book = restoreDocumentDataI.getBook();
		String page = restoreDocumentDataI.getPage();
		if (ro.cst.tsearch.utils.StringUtils.isNotEmpty(book) && ro.cst.tsearch.utils.StringUtils.isNotEmpty(page)) {
			module = getDefaultServerInfo().getModule(TSServerInfo.INSTR_NO_MODULE_IDX);
			module.forceValue(1, book);
			module.forceValue(2, page);
			module.forceValue(3,"Summary Data");
			return module;
		}
		
		return module;
	}
	
	public boolean isAlreadySaved(String instrumentNo, DocumentI doc){
		
		DocumentI docToCheck = null;
		if (doc != null){
			docToCheck = doc.clone();
			docToCheck.setDocSubType(null);
		}

		return isInstrumentSaved(instrumentNo, docToCheck, null, false);
	}
	
	@Override
	public boolean isInstrumentSaved(String instrumentNo, DocumentI documentToCheck, HashMap<String, String> data, boolean checkMiServerId){
    	
    	DocumentsManagerI documentManager = getSearch().getDocManager();
    	try {
    		documentManager.getAccess();
    		if(documentToCheck != null) {
    			if(documentManager.getDocument(documentToCheck.getInstrument()) != null) {
	    			RegisterDocumentI docFound = (RegisterDocumentI) documentManager.getDocument(documentToCheck.getInstrument());
	    			RegisterDocumentI docToCheck = (RegisterDocumentI) documentToCheck;
	    			
	    			if ((docFound.isFake() ||"MISCELLANEOUS".equals(docFound.getDocType())) /*&& isAutomaticSearch()*/){
	    				if (docToCheck.isOneOf(DocumentTypes.PLAT)){
		    				return false;
		    			}
	    				documentManager.remove(docFound);
	    				SearchLogger.info("<span class='error'>Document " + docFound.prettyPrint() + " was a fake one " + docFound.getDataSource() 
	    										+ " and was removed to be saved from RO.</span><br/>", searchId);
	    				return false;
	    			}
	    			
	    			processAllReferencedInstruments(docToCheck);
	    			docToCheck.mergeDocumentsInformation(docFound, searchId, true, false);
    				
	    			return true;
    			} else if(!checkMiServerId) {
    				List<DocumentI> almostLike = documentManager.getDocumentsWithInstrumentsFlexible(false, documentToCheck.getInstrument());
    				if(almostLike != null && !almostLike.isEmpty()) {
//    					if (isAutomaticSearch()){
    						if (documentToCheck.isOneOf(DocumentTypes.PLAT)){
        	    				return false;
        	    			}
	    					for (DocumentI documentI : almostLike) {
	    						if (documentI.isFake() ||"MISCELLANEOUS".equals(documentI.getDocType())){
	    							documentManager.remove(documentI);
	    							SearchLogger.info("<span class='error'>Document " + documentI.prettyPrint() + " was a fake one from " + documentI.getDataSource() 
	    									+ " and was removed to be saved from RO.</span><br/>", searchId);
	        	    				return false;
	        	    			}
							}
//    					}
	    				for (DocumentI documentI : almostLike) {
	    					RegisterDocumentI docFound = (RegisterDocumentI) documentI;
	    	    			RegisterDocumentI docToCheck = (RegisterDocumentI) documentToCheck;
	    	    			docToCheck.mergeDocumentsInformation(docFound, searchId, true, false);
						}
    					return true;
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
	
	@Override
	public DownloadImageResult saveImage(ImageI image) throws ServerResponseException {

		DownloadImageResult res = null;
		logger.error("a intrat pe saveImage: " + searchId);
		if (image != null) {
    		String imageLink = image.getLink(0);
    		
    		if (StringUtils.isNotEmpty(imageLink)) {
    			logger.error("linkul: " + imageLink + " searchId: " + searchId);
    			byte[] imageBytes = null;
	    			
    			imageLink = getBaseLink() + imageLink.substring(imageLink.indexOf("LoadImage"));
    			imageLink = imageLink.replaceFirst("(?is)LoadImage\\.asp&", "LoadImage.asp?");
    			logger.error("linkul pt request: " + imageLink + " searchId: " + searchId);
	    			
    			HttpSite3 site = HttpManager3.getSite(getCurrentServerName(), searchId);
    			HTTPResponse resp = null;
    			try {
    				HTTPRequest req = new HTTPRequest(imageLink, HTTPRequest.GET);
    				resp = ((ro.cst.tsearch.connection.http3.OHFranklinRO) site).process(req);
				} catch (Exception e) {
					e.printStackTrace();
				} finally {
					HttpManager3.releaseSite(site);
				}
				
    			if (resp != null){
    				if ("image/tiff".equalsIgnoreCase(resp.getContentType())){
    					imageBytes = resp.getResponseAsByte();
    				}
    			}
    			if (imageBytes != null){ 
    				res = new DownloadImageResult(DownloadImageResult.Status.OK, imageBytes, image.getContentType());
    				afterDownloadImage(true);
	    					
    				if (res.getStatus() == DownloadImageResult.Status.OK){
    					File f = new File(image.getPath());
    					if (!f.exists()){
    						try {
								FileUtils.writeByteArrayToFile(f, res.getImageContent());
							} catch (IOException e) { }
    					}
    					logger.error("imaginea este : " + res.getStatus() + " searchId: " + searchId);
    					return res;
	        		}
	    		}
    		}
		}
		return new DownloadImageResult(DownloadImageResult.Status.ERROR, new byte[0], image.getContentType());
	}
	
	@Override
	public void processOcredInstrument(InstrumentI in, boolean ocrReportedBookPage) {
		if (ocrReportedBookPage){
			String page = org.apache.commons.lang.StringUtils.defaultString(in.getPage());
			if(page.contains("-")){				
				page = page.replaceAll("[-]+", "");
				
				in.setPage(page);
			}
		}
	}
	
	public DownloadImageResult downloadImageFromRO(ImageI image, InstrumentI instr){

		HashMap<String, String> params = HttpUtils.getParamsFromLink(image.getLink(0));
		if (params != null){

			String docNumber = params.get("instr");
			String year 	 = params.get("year");	
	    	String month 	 = params.get("month");
	    	String day 		 = params.get("day");
	    	String book 	 = params.get("book");
			String page 	 = params.get("page");
	    	
	    	if (StringUtils.isNotEmpty(year) && year.length() > 4){
	    		if (year.length() == 7){
	    			if (StringUtils.isEmpty(month)){
	    				month = year.substring(0, 1);
	    			}
	    			if (StringUtils.isEmpty(day)){
	    				day = year.substring(1, 3);
	    			}
	    		} else if (year.length() == 8){
	    			if (StringUtils.isEmpty(month)){
	    				month = year.substring(0, 2);
	    			}
	    			if (StringUtils.isEmpty(day)){
	    				day = year.substring(2, 4);
	    			}
	    		}
	    		year = year.substring(year.length() - 4);
	    	}
	    	boolean missingValidDate = (StringUtils.isEmpty(year) || StringUtils.isEmpty(month) || StringUtils.isEmpty(day));
	    	boolean missingBookOrPage = (org.apache.commons.lang.StringUtils.isEmpty(book) || org.apache.commons.lang.StringUtils.isEmpty(book));
	    	
	    	if (missingValidDate && missingBookOrPage){
	    		String message = "<br/>Image(searchId=" + searchId + ") book=" 
						+ instr.getBook() + " page=" + instr.getPage() + "inst=" + instr.getInstno()
						+ " can't be taken from RO because";
	    		if (missingValidDate){
	    			message += " requires full date in format MMddyyyy";
	    			if (missingBookOrPage){
	    				message += " and book and page are empty.";
	    			}
	    		}
	    		SearchLogger.info(message + "<br/>", searchId);
	    		return new DownloadImageResult(Status.ERROR, new byte[0], image.getContentType());
	    	}
	    	
	    	HashSet<String> linkRO = new HashSet<String>();
	    	
	    	if (org.apache.commons.lang.StringUtils.isNotEmpty(docNumber) && !missingValidDate){
		    	String date = year + org.apache.commons.lang.StringUtils.leftPad(month, 2, "0") + org.apache.commons.lang.StringUtils.leftPad(day, 2, "0");
		    	String instrument = date + org.apache.commons.lang.StringUtils.leftPad(docNumber, 7, "0");
		    	String imageLink = CreatePartialLink(TSConnectionURL.idGET) + "LoadImage.asp?" + instrument;
		    	linkRO.add(imageLink);
	    	}
	    	
	    	ImageI imageForRo = image.clone();
	    	if (imageForRo.getLinks().size() > 0){
	    		imageForRo.setLinks(linkRO);
	    		
	    		try {
	    			DownloadImageResult dir = null;
	    			if (linkRO.size() > 0){
	    				dir = saveImage(imageForRo);
	    			}
	    			if (dir == null){
	    				
	    				if (org.apache.commons.lang.StringUtils.isNotEmpty(book) && org.apache.commons.lang.StringUtils.isNotEmpty(book)){
	    					String html = "";
	    					book = book.replaceFirst("(?is)\\A[A-Z]", "");
	    	    			HttpSite3 site = null;
	    					try {
	    						site = HttpManager3.getSite(getCurrentServerName(), searchId);
	    					
		    					if (site != null) {
		    						HTTPRequest req = new HTTPRequest(getBaseLink() + "SimpleQuery.asp", HTTPRequest.POST);
		    						req.setPostParameter("Instrs", "");
		    						req.setPostParameter("Book", book);
		    						req.setPostParameter("Page", page);
		    						req.setPostParameter("SUBMIT", "Detail Data");
		    						
		    						HTTPResponse res = site.process(req);
		    						
		    						html = res.getResponseAsString();
		    					}
	    					} finally {
	    						HttpManager3.releaseSite(site);
	    					}
	    						
	    					if (StringUtils.isNotBlank(html)){
	    						Pattern IMAGE_LINK_PAT = Pattern.compile("(?is)<a href='([^']*)'>Display Doc</a>");
	    						Matcher mat = IMAGE_LINK_PAT.matcher(html);
	    						if (mat.find()){
	    							linkRO.clear();
	    							linkRO.add(CreatePartialLink(TSConnectionURL.idGET) + mat.group(1));
		    					    	
	    							if (imageForRo.getLinks().size() > 0){
	    								imageForRo.setLinks(linkRO);
	    								dir = saveImage(imageForRo);	
		    						}
	    						}
	    					}
	    	    		}
	    				if (dir == null){
	    					SearchLogger.info("<br/>Image(searchId=" + searchId + ") book=" 
	    							+ instr.getBook() + " page=" + instr.getPage() + "inst=" + instr.getInstno()
	    							+ " failed to be taken from RO<br/>", searchId);
	    				}
	    			}
	    			if (dir != null && TSInterface.DownloadImageResult.Status.OK.equals(dir.getStatus())){
	    				byte[] b = new byte[0];
	    	    		try {
	    	    			b = org.apache.commons.io.FileUtils.readFileToByteArray(new File(image.getPath()));
	    	    		} catch (IOException e) {e.printStackTrace();}
	    	    		
	    	    		if (b.length > 0){
	    	    			afterDownloadImage(true, GWTDataSite.RO_TYPE);
	    	    			SearchLogger.info("<br/>Image(searchId=" + searchId + ") book=" 
									+ instr.getBook() + " page=" + instr.getPage() + "inst=" + instr.getInstno()
									+ " was taken from RO<br/>", searchId);
	    	    			return dir;
	    	    		}
	    			}
				} catch (ServerResponseException e) {
					e.printStackTrace();
				}
	    	}
		}
	return new DownloadImageResult();
	}
	
	@SuppressWarnings("rawtypes")
	public static void parseNamesRO(ResultMap m, long searchId) throws Exception{
		
		String names[] = {"", "", "", "", "", ""};
		String[] suffixes = {"", ""}, type = {"", ""}, otherType = {"", ""};
		ArrayList<List> grantor = new ArrayList<List>();
		ArrayList<List> grantee = new ArrayList<List>();
		
		String tmpPartyGtor = (String)m.get("tmpPartyGtor");
		if (StringUtils.isNotEmpty(tmpPartyGtor)){
			tmpPartyGtor = prepareName(tmpPartyGtor);
			
			String[] gtors = tmpPartyGtor.split("\\s+/\\s+");
			for (String grantorName : gtors){
				grantorName = grantorName.replaceAll("/", " ");
				names = StringFormats.parseNameNashville(grantorName, true);
				
				type = GenericFunctions.extractAllNamesType(names);
				otherType = GenericFunctions.extractAllNamesOtherType(names);
				if (NameUtils.isNotCompany(names[2])){
					suffixes = GenericFunctions.extractNameSuffixes(names);
				}
				
				GenericFunctions.addOwnerNames(grantorName, names, suffixes[0],
												suffixes[1], type, otherType,
												NameUtils.isCompany(names[2]),
												NameUtils.isCompany(names[5]), grantor);
			}
			
			m.put(SaleDataSetKey.GRANTOR.getKeyName(), tmpPartyGtor);
			m.put("GrantorSet", GenericFunctions.storeOwnerInSet(grantor, true));
		}
		
		String tmpPartyGtee = (String)m.get("tmpPartyGtee");
		if (StringUtils.isNotEmpty(tmpPartyGtee)){
			tmpPartyGtee = prepareName(tmpPartyGtee);
			
			String[] gtee = tmpPartyGtee.split("\\s+/\\s+");
			for (String granteeName : gtee){
	
				granteeName = granteeName.replaceAll("/", " ");
				names = StringFormats.parseNameNashville(granteeName, true);
				
				type = GenericFunctions.extractAllNamesType(names);
				otherType = GenericFunctions.extractAllNamesOtherType(names);
				if (NameUtils.isNotCompany(names[2])){
					suffixes = GenericFunctions.extractNameSuffixes(names);
				}
				
				GenericFunctions.addOwnerNames(granteeName, names, suffixes[0],
						suffixes[1], type, otherType,
						NameUtils.isCompany(names[2]),
						NameUtils.isCompany(names[5]), grantee);
			}
			
			m.put(SaleDataSetKey.GRANTEE.getKeyName(), tmpPartyGtee);
			m.put("GranteeSet", GenericFunctions.storeOwnerInSet(grantee, true));
			
		}
		
		GenericFunctions1.setGranteeLanderTrustee2(m, searchId,true);
	
	}

	private static String prepareName(String name){
	
		name = name.replaceAll("(?is)</?nobr>", "").replaceAll("(?is)<br>", " / ");
		name = name.replaceAll("\\s*-\\s*(\\(?DECEASED\\)?|EXECUTOR|HEIR|NOMINEE|SUCCESSOR BY MERGER|SUCCESSOR IN INTEREST|THIRD PARTY)\\b", "");
		name = name.replaceFirst("(?is)\\bDECD\\b", "");
		name = name.replaceAll("\\s+&\\s+[HUSB|HUSBAND|WIFE]\\b", "");
		name = name.replaceAll("\\s+&\\s+[A-Z]F?\\b", "");
		name = name.replaceAll("\\s+&\\s+(ETAL|ETUX|ETVIR)\\b", " $1");
		name = name.replaceAll("\\s+-\\s*(?:CO-\\s*)?(TRUSTEES?)", " $1");
		name = name.replaceAll("(?is)\\s+\\bONG\\b\\s*", " ");
		name = name.replaceAll("(\\w+)-\\s*\\d+.*", "$1");
		name = name.replaceAll("[\\)\\(]+", "");
		name = name.replaceFirst("^\\s*/\\s*", "");
		name = name.replaceFirst("\\s*/\\s*$", "");
		name = name.replaceAll("(?is)\\s*-\\s*\\b(JR|SR|III|II|I)\\b", " $1");
		name = name.replaceAll("(?is)&nbsp;", " ");
		
		return name;
	}
	
	public void parseLegalDetailData(String legal, DocumentI document, long searchId) throws Exception{

		if (StringUtils.isNotEmpty(legal)){
			
			Set<PropertyI> properties = document.getProperties();
			
			legal = legal.replaceAll("(?is)&nbsp;", " ").replaceAll("(?is)\\bSEE RECORD\\b", " ");
			legal = GenericFunctions.replaceNumbers(legal);
			String[] exceptionTokens = { "I", "M", "C", "L", "D" };
			
			String[] legals = legal.split("\\s+/\\s+");
			
			String subdivisionName = "", platBook = "", platPage = "";
			for (String eachLegal : legals) {
				PropertyI property = Property.createEmptyProperty();
				SubdivisionI subdivision = property.getLegal().getSubdivision();
//	        	TownShipI township = property.getLegal().getTownShip();
	        	PinI pin = property.getPin();
				
	        	if (eachLegal.contains("Sub:")){
					Pattern SUBD_PAT = Pattern.compile("(?is)Sub: ([^,]+),");
					Matcher mat = SUBD_PAT.matcher(eachLegal);
					if (mat.find()){
						subdivisionName = mat.group(1);
						subdivisionName = subdivisionName.replaceAll("(?is)(.*?)\\s(CONDOMINIUM|CONDOS?|SEC\\sNO\\s\\d+(\\sPART\\s\\d+)?|SECTION\\s(NO\\s)?\\d+(\\sPART\\s\\d+)?|SEC\\s\\d+(\\s(PT|PHASE|PH)\\s\\d+)?|PHASE\\s\\d+|AMENDED|RESUBD|PLAT\\s(NO\\s)?\\d+)(.*?)$", "$1");
					} else{
						subdivisionName = eachLegal.replaceAll("(?is)Sub:", "").trim();
					}
					subdivision.setName(subdivisionName);
	        	}
				
				Pattern PBG_PAT = Pattern.compile("(?is)\\bCabSld: (?:P|C)B (\\d+) (?:P(?:G|B) )?(\\d+(-?\\w+)?).*?");
				Matcher mat = PBG_PAT.matcher(eachLegal);
				if (mat.find()){
					platBook = mat.group(1);
					platPage = mat.group(2);
					platPage = platPage.replaceAll("(?is)(\\w+)\\s*-\\s*(\\w+)", "$1");
				}
				
				if (StringUtils.isNotBlank(platBook) && StringUtils.isNotBlank(platPage)){
					subdivision.setPlatBook(platBook);
					subdivision.setPlatPage(platPage);
				}
				
				Pattern LOT_PAT = Pattern.compile("(?i)\\b(Lt|Lts|Lot): ([^,]*).*?");
				mat = LOT_PAT.matcher(eachLegal);
				if (mat.find()){
					String lot = mat.group(2);
					lot = LegalDescription.cleanValues(lot, false, true);
					subdivision.setLot(lot);
				}
				
				if (eachLegal.contains("PrpId:")){
					Pattern PARCELID_PAT = Pattern.compile("(?is)\\bPrpId: ([^,]*).*?");
					mat = PARCELID_PAT.matcher(eachLegal);
					if (mat.find()){
						String parcelId = mat.group(1);
						pin.addPin(PinType.PID, parcelId);
					} else{
						String parcelId = eachLegal.replaceAll("(?is)PrpId:", "").trim();;
						pin.addPin(PinType.PID, parcelId);
					}
				}
				
				Pattern UNIT_PAT = Pattern.compile("(?is)\\bUn: ([^,]*).*?");
				mat = UNIT_PAT.matcher(eachLegal);
				if (mat.find()){
					String unit = mat.group(1);
					subdivision.setUnit(unit);
				}
				Pattern SEC_PAT = Pattern.compile("(?is)\\bbSub: .*?\\b(?:SECTION NO?|SECTION|SEC NO?|SEC) (\\d+).*?");
				mat = SEC_PAT.matcher(eachLegal);
				if (mat.find()){
					String section = mat.group(1);
					section = Roman.normalizeRomanNumbersExceptTokens(section, exceptionTokens); // convert roman numbers to arabics	
					subdivision.setSection(section);
				}
				
				properties.add(property);
			}
		}
	}
	
	public void parseLegalSummaryData(String legal, DocumentI document, long searchId) throws Exception{
		
		if (StringUtils.isNotEmpty(legal)){
			
			Set<PropertyI> properties = document.getProperties();
			
			legal = legal.replaceAll("(?is)&nbsp;", " ");
			legal = GenericFunctions.replaceNumbers(legal);
			String[] exceptionTokens = { "I", "M", "C", "L", "D" };
			
			String platBook = "", platPage = ""; //subdivisionName = "";
			PropertyI property = Property.createEmptyProperty();
			SubdivisionI subdivision = property.getLegal().getSubdivision();
//	        TownShipI township = property.getLegal().getTownShip();
	        PinI pin = property.getPin();
	        legal = legal.replaceAll("(?is)\\b(\\d+)([A-Z]+)\\b", "$1 $2");
				
	        Pattern LOT_PAT = Pattern.compile("(?i)\\b(Lt/Un) ([\\d-]+[A-Z]?).*?");
	        Matcher mat = LOT_PAT.matcher(legal);
	        if (mat.find()){
	        	String lot = mat.group(2);
	        	lot = LegalDescription.cleanValues(lot, false, true);
	        	subdivision.setLot(lot);
	        	legal = legal.replaceAll(mat.group(), "");
			}
	        
	        Pattern LOT_PAT1 = Pattern.compile("(?i)\\b(desc:) ([\\d\\s]+).*?");
	        mat = LOT_PAT1.matcher(legal);
	        if (mat.find()){
	        	String lot = mat.group(2);
	        	lot = LegalDescription.cleanValues(lot, false, true);
	        	subdivision.setLot(lot);
	        	legal = legal.replaceAll(mat.group(), "");
			}
				
	        Pattern PB_PAT = Pattern.compile("(?is)\\bPlt\\s+(?:P|C)B (\\d+).*?");
	        mat = PB_PAT.matcher(legal);
	        if (mat.find()){
	        	platBook = mat.group(1);
			}
	        Pattern PG_PAT = Pattern.compile("(?is)\\bPlt\\s+(?:P|C)B \\d+ (?:P(?:G|B) )?(\\d+(-?\\w+)?).*?");
	        mat = PG_PAT.matcher(legal);
	        if (mat.find()){
	        	platPage = mat.group(1);
	        	platPage = platPage.replaceAll("(?is)(\\w+)\\s*-\\s*(\\w+)", "$1");
	        	legal = legal.replaceAll(mat.group(), "");
			}
	        if (StringUtils.isNotBlank(platBook) && StringUtils.isNotBlank(platPage)){
	        	subdivision.setPlatBook(platBook);
	        	subdivision.setPlatPage(platPage);
			}
			
	        Pattern PARCELID_PAT = Pattern.compile("(?is)\\bPcl\\s*# ([\\d-]+).*?");
	        mat = PARCELID_PAT.matcher(legal);
	        if (mat.find()){
	        	String parcelId = mat.group(1);
	        	pin.addPin(PinType.PID, parcelId);
	        	legal = legal.replaceAll(mat.group(), "");
			}
	        Pattern UNIT_PAT = Pattern.compile("(?is)\\bNO\\s+(\\d+)\\b");
	        mat = UNIT_PAT.matcher(legal);
	        if (mat.find()){
	        	String unit = mat.group(1);
	        	subdivision.setUnit(unit);
			}
	        
	        Pattern ACREAGE_PAT = Pattern.compile("(?is)\\bAc\\s+([\\d\\.]+)\\b");
	        mat = ACREAGE_PAT.matcher(legal);
	        if (mat.find()){
	        	String acreage = mat.group(1);
	        	subdivision.setAcreage(acreage);
			}
	        
	        Pattern SEC_PAT = Pattern.compile("(?is)\\b(?:SECTION NO?|SECTION|SEC NO?|SEC) (\\d+).*?");
	        mat = SEC_PAT.matcher(legal);
	        if (mat.find()){
	        	String section = mat.group(1);
	        	section = Roman.normalizeRomanNumbersExceptTokens(section, exceptionTokens); // convert roman numbers to arabics	
	        	subdivision.setSection(section);
	        	legal = legal.replaceAll(mat.group(), "");
			}
	        
//	        Pattern SUBD_PAT = Pattern.compile("(?is)Sub: ([^,]+),");
//	        mat = SUBD_PAT.matcher(legal);
//	        if (mat.find()){
//	        	subdivisionName = mat.group(1);
//	        	subdivisionName = subdivisionName.replaceAll("(?is)(.*?)\\s(CONDOMINIUM|CONDOS?|SEC\\sNO\\s\\d+(\\sPART\\s\\d+)?|SECTION\\s(NO\\s)?\\d+(\\sPART\\s\\d+)?|SEC\\s\\d+(\\s(PT|PHASE|PH)\\s\\d+)?|PHASE\\s\\d+|AMENDED|RESUBD|PLAT\\s(NO\\s)?\\d+|NO\\b)(.*?)$", "$1");
//			}
	        legal = legal.replaceAll("(?is)\\*", "");
	        legal = legal.replaceAll("(?is)\\bPcl\\s*#\\s*", "");
	        legal = legal.replaceAll("(?s)\\bUn\\b", "");
	        
	        legal = legal.replaceAll("(?is)(.*?)\\s(CONDOMINIUM|CONDOS?|SEC\\sNO\\s\\d+(\\sPART\\s\\d+)?|SECTION\\s(NO\\s)?\\d+(\\sPART\\s\\d+)?|SEC\\s\\d+(\\s(PT|PHASE|PH)\\s\\d+)?|PHASE\\s\\d+|AMENDED|RESUBD|PLAT\\s(NO\\s)?\\d+)(.*?)$", "$1");
	        subdivision.setName(legal.trim());
				
	        properties.add(property);
		}
	}
	
	protected void solveHtmlResponse(String sAction, int viParseID,
            String rsFunctionName, ServerResponse Response, String htmlString, Map<String, Object> extraParams) throws ServerResponseException {
    	
		Response.setResult(htmlString);
        Response.setParentSiteSearch(isParentSite());
        Response.getParsedResponse().setParentSite(isParentSite());
        
        if (Response.getQuerry().contains("SUBMIT=Detail Data")){
	        if (Response.getResult() == null) {//if official site doesn't send Detail Data then use Summary Data as document index
	        	if (Response.getParsedResponse() != null && Response.getParsedResponse().getPageLink() != null){
	        		String link = Response.getParsedResponse().getPageLink().getLink();
	        		if (StringUtils.isNotBlank(link) && link.contains("&Link=")){
	        			try {
							link = URLDecoder.decode(link, "UTF-8");
						} catch (UnsupportedEncodingException e) {
						}
	        			if (viParseID == TSServer.ID_SAVE_TO_TSD){
	        				link = link.substring(link.indexOf("&Link=") + 6) + "&UseSummaryData=true";
	        				if (getSearch().existsInMemoryDoc(link)
		        					|| getSearch().existsInMemoryDoc(link.replaceAll("&parentSite=true", ""))
		        					|| getSearch().existsInMemoryDoc(link.replaceAll("(?is).*?(SUBMIT.*)", "$1"))){
	        					
	        					Object inMemoryDoc = (link.indexOf("&parentSite=true") >= 0 ? 
	        							getSearch().getInMemoryDoc(link.replaceAll("&parentSite=true", "")) : 
	        							getSearch().getInMemoryDoc(link));
	        					if (inMemoryDoc == null){
	        						inMemoryDoc = getSearch().getInMemoryDoc(link.replaceAll("(?is).*?(SUBMIT.*)", "$1"));
	        					}
	        					if (inMemoryDoc instanceof ParsedResponse){
	        						ParsedResponse pr = (ParsedResponse) inMemoryDoc;
	        						Response.setParsedResponse(pr);
	        						htmlString = (pr.getResponse());
	        						
	        						getSearch().removeRecursiveAnalisedLink(Response.getParsedResponse().getPageLink().getLink().toLowerCase());
	        						getSearch().removeLinkVisited(Response.getParsedResponse().getPageLink().getLink().toLowerCase());
	        						getSearch().removeRecursiveAnalisedLink(Response.getParsedResponse().getPageLink().getLink().toLowerCase());
		        					getSearch().removeRecursiveAnalisedLink(Response.getParsedResponse().getPageLink().getLink().toLowerCase().replaceFirst("&issubresult=true", ""));
		        					getSearch().removeLinkVisited(Response.getParsedResponse().getPageLink().getLink().toLowerCase());
		        					getSearch().removeLinkVisited(Response.getParsedResponse().getPageLink().getLink().toLowerCase().replaceFirst("&issubresult=true", ""));
	        					}
		    	        	}
	        			} else{
	        				link = link.substring(link.indexOf("&Link=") + 6) + "&UseSummaryDataResult=true";
		        			if (getSearch().existsInMemoryDoc(link)
		        					|| getSearch().existsInMemoryDoc(link.replaceAll("&parentSite=true", ""))){
		        				Object inMemoryDoc = (link.indexOf("&parentSite=true") >= 0 ? 
	        							getSearch().getInMemoryDoc(link.replaceAll("&parentSite=true", "")) : 
	        							getSearch().getInMemoryDoc(link));
	        					if (inMemoryDoc instanceof String){
	        						htmlString = (String) inMemoryDoc;
	        					}
	        					getSearch().removeRecursiveAnalisedLink(Response.getParsedResponse().getPageLink().getLink().toLowerCase());
	        					getSearch().removeRecursiveAnalisedLink(Response.getParsedResponse().getPageLink().getLink().toLowerCase().replaceFirst("&issubresult=true", ""));
	        					getSearch().removeLinkVisited(Response.getParsedResponse().getPageLink().getLink().toLowerCase());
	        					getSearch().removeLinkVisited(Response.getParsedResponse().getPageLink().getLink().toLowerCase().replaceFirst("&issubresult=true", ""));
		    	        	}
	        			}
	        		}
	        	}        	
	        }
        }
        super.solveHtmlResponse(sAction, viParseID, rsFunctionName, Response, htmlString, extraParams);
    }
}