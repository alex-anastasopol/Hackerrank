package ro.cst.tsearch.servers.types;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Set;
import java.util.Vector;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang.StringEscapeUtils;
import org.htmlparser.filters.HasAttributeFilter;
import org.htmlparser.filters.NotFilter;
import org.htmlparser.filters.TagNameFilter;
import org.htmlparser.nodes.TagNode;
import org.htmlparser.tags.ImageTag;
import org.htmlparser.tags.LinkTag;
import org.htmlparser.tags.TableColumn;
import org.htmlparser.tags.TableRow;
import org.htmlparser.tags.TableTag;
import org.htmlparser.util.NodeList;
import org.w3c.dom.Node;

import ro.cst.tsearch.Search;
import ro.cst.tsearch.ServerConfig;
import ro.cst.tsearch.bean.SearchAttributes;
import ro.cst.tsearch.connection.http2.HttpManager;
import ro.cst.tsearch.connection.http2.HttpSite;
import ro.cst.tsearch.database.DBManager;
import ro.cst.tsearch.exceptions.ServerResponseException;
import ro.cst.tsearch.extractor.xml.Bridge;
import ro.cst.tsearch.extractor.xml.ResultMap;
import ro.cst.tsearch.generic.Util;
import ro.cst.tsearch.parser.HtmlParser3;
import ro.cst.tsearch.parser.HtmlParserTidy;
import ro.cst.tsearch.parser.LinkParser;
import ro.cst.tsearch.parser.SimpleHtmlParser;
import ro.cst.tsearch.propertyInformation.Instrument;
import ro.cst.tsearch.search.FunctionStatesIterator;
import ro.cst.tsearch.search.ModuleStatesIterator;
import ro.cst.tsearch.search.filter.FilterResponse;
import ro.cst.tsearch.search.filter.RejectAlreadySavedDocumentsFilterResponse;
import ro.cst.tsearch.search.filter.newfilters.date.DateFilterFactory;
import ro.cst.tsearch.search.filter.newfilters.date.LastTransferDateFilter;
import ro.cst.tsearch.search.filter.newfilters.doctype.DoctypeFilterFactory;
import ro.cst.tsearch.search.filter.newfilters.legal.LegalFilterFactory;
import ro.cst.tsearch.search.filter.newfilters.misc.CrossReferenceToInvalidatedFilter;
import ro.cst.tsearch.search.filter.newfilters.name.NameFilterFactory;
import ro.cst.tsearch.search.filter.newfilters.name.SynonimNameFilter;
import ro.cst.tsearch.search.iterator.ModuleStatesIteratorFactory;
import ro.cst.tsearch.search.iterator.instrument.InstrumentGenericIterator;
import ro.cst.tsearch.search.module.ConfigurableNameIterator;
import ro.cst.tsearch.servers.SearchDataWrapper;
import ro.cst.tsearch.servers.TSConnectionURL;
import ro.cst.tsearch.servers.info.TSServerInfo;
import ro.cst.tsearch.servers.info.TSServerInfoConstants;
import ro.cst.tsearch.servers.info.TSServerInfoModule;
import ro.cst.tsearch.servers.response.ImageLinkInPage;
import ro.cst.tsearch.servers.response.LinkInPage;
import ro.cst.tsearch.servers.response.OCRParsedDataStruct;
import ro.cst.tsearch.servers.response.ParsedResponse;
import ro.cst.tsearch.servers.response.Parser;
import ro.cst.tsearch.servers.response.PropertyIdentificationSet.PropertyIdentificationSetKey;
import ro.cst.tsearch.servers.response.RawResponseWrapper;
import ro.cst.tsearch.servers.response.ServerResponse;
import ro.cst.tsearch.servlet.URLConnectionReader;
import ro.cst.tsearch.titledocument.abstracts.DocumentTypes;
import ro.cst.tsearch.utils.FileUtils;
import ro.cst.tsearch.utils.GBManager;
import ro.cst.tsearch.utils.InstanceManager;
import ro.cst.tsearch.utils.RequestParams;
import ro.cst.tsearch.utils.RequestParamsValues;
import ro.cst.tsearch.utils.SearchLogger;
import ro.cst.tsearch.utils.StringUtils;
import ro.cst.tsearch.utils.Tidy;

import com.stewart.ats.base.document.DocumentI;
import com.stewart.ats.base.document.ImageI;
import com.stewart.ats.base.document.InstrumentI;
import com.stewart.ats.base.document.MortgageI;
import com.stewart.ats.base.document.RegisterDocumentI;
import com.stewart.ats.base.document.RestoreDocumentDataI;
import com.stewart.ats.base.document.TransferI;
import com.stewart.ats.base.name.NameI;
import com.stewart.ats.base.property.PropertyI;
import com.stewart.ats.base.search.DocumentsManagerI;
import com.stewart.ats.tsrindex.client.SimpleChapterUtils;
import com.stewart.ats.tsrindex.server.TsdIndexPageServer;


public class ILDuPageRO extends TSServer {
	public static final Pattern DOCTYPE = Pattern.compile("(?s)<td\\s*>\\s*Doc Desc:</td><td[^>]*>(.*?)</td>");
	public static final Pattern INSTRUMENTNO = Pattern.compile("(?s)<td\\s*>\\s*Doc Number:</td><td[^>]*>(.*?)</td>");
	public static final Pattern INTERLINKS = Pattern.compile("javascript:__doPostBack\\('(_ctl\\d+\\$mainPlaceHolder\\$_ctl\\d+\\$CountySpecificResults\\d+\\$dgResults\\$_ctl\\d+\\$_ctl\\d+)',''\\)");
	public static final Pattern NEXT_LINK = Pattern.compile("<a href=([^>]+)>2</a>");
	public static final Pattern INSTRUMENTNO_ALREADY_FOLLOWED = Pattern.compile("instrNum=([0-9A-Z]+)");
	public static final Pattern INSTRUMENTNO_ALREADY_FOLLOWED2 = Pattern.compile("FK____([0-9A-Z]+)");

	public static final Pattern certDatePattern = Pattern.compile("(?ism)Current\\s+recording\\s+effective\\s+date\\s+is\\s*(?:<strong>)?(.*?)</strong>");
	
	static final long serialVersionUID = 10000000;
	
	public static final String[] ESTM_PLAT_REST = new String[]{"PLAT", "ESMT", "EASEMENT"};
	@Override
	protected void setModulesForGoBackOneLevelSearch(TSServerInfo serverInfo) {
		ConfigurableNameIterator nameIterator = null;
		String endDate = new SimpleDateFormat("MM/dd/yyyy").format(new Date());
		
		SearchAttributes sa = InstanceManager.getManager().getCurrentInstance(searchId).getCrtSearchContext().getSa();	
		List<TSServerInfoModule> modules = new ArrayList<TSServerInfoModule>();
	    TSServerInfoModule module;	
	    GBManager gbm=(GBManager)sa.getObjectAtribute(SearchAttributes.GB_MANAGER_OBJECT);

	    for (String id : gbm.getGbTransfers()) {
			  		   	    	 
	  		 module =new TSServerInfoModule(serverInfo.getModule(TSServerInfo.ADDRESS_MODULE_IDX));
	         module.setIndexInGB(id);
	         module.setTypeSearchGB("grantor");
	         module.clearSaKeys();
		     module.setSaObjKey(SearchAttributes.GB_MANAGER_OBJECT);
		     
		     
		     String date=gbm.getDateForSearch(id, "MM/dd/yyyy", searchId);
		     if (date!=null) {
		    	 module.getFunction(7).forceValue(date);
		     }
		     module.setValue(8, endDate);
		     
		     module.addFilter(NameFilterFactory.getDefaultNameFilter(SearchAttributes.GB_MANAGER_OBJECT, searchId, module));
		     module.setIteratorType(0, FunctionStatesIterator.ITERATOR_TYPE_LCF_NAME_FAKE);
		     module.addFilter(NameFilterFactory.getDefaultTransferNameFilter(searchId, 0.90d, module));
		  	 module.addFilter(DateFilterFactory.getDateFilterForGoBack(SearchAttributes.GB_MANAGER_OBJECT, searchId, module));	
		     nameIterator =  (ConfigurableNameIterator)ModuleStatesIteratorFactory.getConfigurableNameIterator(module, searchId, new String[]{"L;F;", "L;f;", "L;M;", "L;m;"} );
		 	 module.addIterator(nameIterator);
		 	 
	         
            	
			 modules.add(module);
		    
		     
		     if(gbm.getNamesForBrokenChain(id, searchId).size()>0){
		    	 module =new TSServerInfoModule(serverInfo.getModule(TSServerInfo.ADDRESS_MODULE_IDX));
			     module.setIndexInGB(id);
			     module.setTypeSearchGB("grantee");
			     module.clearSaKeys();
				 module.setSaObjKey(SearchAttributes.GB_MANAGER_OBJECT);
				 date=gbm.getDateForSearchBrokenChain(id, "MM/dd/yyyy", searchId);
				 if (date!=null) 
					 module.getFunction(7).forceValue(date);
				 module.setValue(8, endDate);
				 
				 module.setIteratorType(0, FunctionStatesIterator.ITERATOR_TYPE_LCF_NAME_FAKE);
				 module.addFilter(NameFilterFactory.getDefaultNameFilter(SearchAttributes.GB_MANAGER_OBJECT, searchId, module));
				 module.addFilter(NameFilterFactory.getDefaultTransferNameFilter(searchId, 0.90d, module));
			  	 module.addFilter(DateFilterFactory.getDateFilterForGoBack(SearchAttributes.GB_MANAGER_OBJECT, searchId, module));
			  	 
				 nameIterator =  (ConfigurableNameIterator)ModuleStatesIteratorFactory.getConfigurableNameIterator(module, searchId, new String[]{"L;F;", "L;f;", "L;M;", "L;m;"} );
				 module.addIterator(nameIterator);
				 
				 				
				 modules.add(module);
			 
		     }

	    }	 
	    serverInfo.setModulesForGoBackOneLevelSearch(modules);	
	}
	
	@Override
	protected void setModulesForAutoSearch(TSServerInfo serverInfo) {
		List<TSServerInfoModule> l = new ArrayList<TSServerInfoModule>();
		
		Search search = getSearch();
		TSServerInfoModule m = null;
		SearchAttributes sa = search.getSa();
		
        
        FilterResponse  estmPlatRestDoctypeFilter = DoctypeFilterFactory.getDoctypeFilter(searchId, 0.8, ESTM_PLAT_REST, FilterResponse.STRATEGY_TYPE_HIGH_PASS);
		FilterResponse defaultLegalValidator = LegalFilterFactory.getDefaultLegalFilter(searchId);
		defaultLegalValidator.setSaveInvalidatedInstruments(true);
		FilterResponse subdivisionNameFilter = NameFilterFactory.getDefaultNameFilterForSubdivision(searchId);
		FilterResponse crossReferenceToInvalidated = new CrossReferenceToInvalidatedFilter(searchId);
		FilterResponse rejectSavedDocuments = new RejectAlreadySavedDocumentsFilterResponse(searchId);
		
		// instrument list search from AO
		m = new TSServerInfoModule(serverInfo.getModule(TSServerInfo.ADDRESS_MODULE_IDX));
		m.addExtraInformation(TSServerInfoConstants.EXTRA_PARAM_MODULE_DESCRIPTION, 
				TSServerInfoConstants.VALUE_PARAM_LIST_AO_INSTR);
		m.clearSaKeys();
		m.setSaKey(7, SearchAttributes.FROMDATE_MM_SLASH_DD_SLASH_YYYY);
		m.setSaKey(8, SearchAttributes.TODATE_MM_SLASH_DD_SLASH_YYYY);
		m.setIteratorType(4, FunctionStatesIterator.ITERATOR_TYPE_INSTRUMENT_LIST_FAKE);
		m.addIterator(getInstrumentIterator());
		l.add(m);  
		
		if (hasPin()){
			m = new TSServerInfoModule(serverInfo.getModule(TSServerInfo.ADDRESS_MODULE_IDX));
			m.clearSaKeys();
			m.setSaKey(5, SearchAttributes.LD_PARCELNO);
			m.setSaKey(7, SearchAttributes.FROMDATE_MM_SLASH_DD_SLASH_YYYY);
			m.setSaKey(8, SearchAttributes.TODATE_MM_SLASH_DD_SLASH_YYYY);
			m.addFilter(rejectSavedDocuments);
			m.addFilter(crossReferenceToInvalidated);
			l.add(m);
		}
		
		
        
		//search with subdivision
		if(!StringUtils.isEmpty(sa.getAtribute( SearchAttributes.LD_SUBDIV_NAME ))){
			m = new TSServerInfoModule(serverInfo.getModule(TSServerInfo.ADDRESS_MODULE_IDX));
			m.clearSaKeys();
			m.getFunction(0).setSaKey(SearchAttributes.LD_SUBDIV_NAME);
			m.addFilter(rejectSavedDocuments);
			m.addFilter(subdivisionNameFilter);
			m.addFilter(estmPlatRestDoctypeFilter);
			m.addFilter(defaultLegalValidator);
			l.add(m);
		}
		
		//name modules with names from search page.
    	m = new TSServerInfoModule(serverInfo.getModule(TSServerInfo.ADDRESS_MODULE_IDX));
    	m.addExtraInformation(TSServerInfoConstants.EXTRA_PARAM_MODULE_DESCRIPTION, 
    			TSServerInfoConstants.VALUE_PARAM_NAME_OWNERS);
		m.setSaObjKey(SearchAttributes.OWNER_OBJECT);
		m.clearSaKeys();
		m.setSaKey(7, SearchAttributes.FROMDATE_MM_SLASH_DD_SLASH_YYYY);
		m.setIteratorType(7, FunctionStatesIterator.ITERATOR_TYPE_FROM_DATE);
		m.setSaKey(8, SearchAttributes.TODATE_MM_SLASH_DD_SLASH_YYYY);
		
		FilterResponse defaultNameFilter = NameFilterFactory.getDefaultSynonimNameInfoPinCandidateFilter( 
				SearchAttributes.OWNER_OBJECT , searchId , m );
		((SynonimNameFilter)defaultNameFilter).setIgnoreMiddleOnEmpty(true);
		m.addFilter(rejectSavedDocuments);
		m.addFilter(defaultNameFilter);
		m.addFilter(defaultLegalValidator);
		m.addFilter(new LastTransferDateFilter(searchId));
		m.addFilter(crossReferenceToInvalidated);
		addBetweenDateTest(m, false, true, true);
		addFilterForUpdate(m, true);
		m.addValidator(defaultNameFilter.getValidator());
		m.setIteratorType(0, FunctionStatesIterator.ITERATOR_TYPE_LCF_NAME_FAKE);
		ConfigurableNameIterator nameIterator = (ConfigurableNameIterator) ModuleStatesIteratorFactory
			.getConfigurableNameIterator(m, false, searchId, new String[] {"L;F;", "L;f;", "L S;f;", });
		nameIterator.setAllowMcnPersons(false);//B 4361
		nameIterator.setDoScottishNamesDerivations(true);
		nameIterator.setDerivateWithSynonims(true);
		nameIterator.setInitAgain(true);		//initialize again after all parameters are set
		
		m.addIterator(nameIterator);

		l.add(m);
		
		
		//search by crossRef book and page list from RO documents
		m = new TSServerInfoModule(serverInfo.getModule(TSServerInfo.ADDRESS_MODULE_IDX));
		m.addExtraInformation(TSServerInfoConstants.EXTRA_PARAM_MODULE_DESCRIPTION, 
        		TSServerInfoConstants.VALUE_PARAM_LIST_RO_CROSSREF_INSTR);
		m.clearSaKeys();
		m.setSaKey(7, SearchAttributes.FROMDATE_MM_SLASH_DD_SLASH_YYYY);
		m.setSaKey(8, SearchAttributes.TODATE_MM_SLASH_DD_SLASH_YYYY);
		m.addFilter(rejectSavedDocuments);
		m.setSaObjKey(SearchAttributes.RO_CROSS_REF_INSTR_LIST);		    		    
	    m.setIteratorType(ModuleStatesIterator.TYPE_INSTRUMENT_LIST_NOT_AGAIN);	    
		m.getFunction(4).setIteratorType(FunctionStatesIterator.ITERATOR_TYPE_INSTRUMENT_LIST_FAKE);
	    l.add(m);

		// OCR last transfer - instrument search
		m = new TSServerInfoModule(serverInfo.getModule(TSServerInfo.ADDRESS_MODULE_IDX));
		m.clearSaKeys();
		m.setIteratorType(ModuleStatesIterator.TYPE_OCR_FULL_OR_BOOTSTRAPER);
		m.getFunction(4).setIteratorType(FunctionStatesIterator.ITERATOR_TYPE_INSTRUMENT_LIST_SEARCH);
		m.addCrossRefValidator(defaultLegalValidator.getValidator());
		l.add(m);
		
        // name module with names added by OCR
    	m = new TSServerInfoModule(serverInfo.getModule(TSServerInfo.ADDRESS_MODULE_IDX));
    	m.addExtraInformation(TSServerInfoConstants.EXTRA_PARAM_MODULE_DESCRIPTION, 
    			TSServerInfoConstants.VALUE_PARAM_NAME_OWNERS);
		m.setSaObjKey(SearchAttributes.OWNER_OBJECT);
		m.clearSaKeys();
		m.setSaKey(7, SearchAttributes.FROMDATE_MM_SLASH_DD_SLASH_YYYY);
		m.setIteratorType(7, FunctionStatesIterator.ITERATOR_TYPE_FROM_DATE);
		m.setSaKey(8, SearchAttributes.TODATE_MM_SLASH_DD_SLASH_YYYY);
		FilterResponse defaultNameFilterLast = NameFilterFactory.getDefaultSynonimNameInfoPinCandidateFilter( 
				SearchAttributes.OWNER_OBJECT , searchId , m );
		((SynonimNameFilter)defaultNameFilterLast).setIgnoreMiddleOnEmpty(true);
		defaultNameFilterLast.setInitAgain(true);
		m.addFilter(rejectSavedDocuments);
		m.addFilter(defaultNameFilterLast);
		m.addFilter(defaultLegalValidator);
		m.addFilter(new LastTransferDateFilter(searchId));
		m.addFilter(crossReferenceToInvalidated);
		addBetweenDateTest(m, false, true, true);
		m.addValidator(defaultNameFilterLast.getValidator());
		m.setIteratorType(0, FunctionStatesIterator.ITERATOR_TYPE_LCF_NAME_FAKE);
		ArrayList<NameI> searchedNames = null;
		searchedNames = nameIterator.getSearchedNames();
		nameIterator = (ConfigurableNameIterator) ModuleStatesIteratorFactory
			.getConfigurableNameIterator(m, false, searchId, new String[] {"L;F;", "L;f;", "L S;f;"});
		nameIterator.setAllowMcnPersons(false);//B4361
		nameIterator.setDoScottishNamesDerivations(true);
		nameIterator.setDerivateWithSynonims(true);
		nameIterator.setInitAgain(true); //initialize again after all parameters are set
		nameIterator.setSearchedNames(searchedNames);
		m.addIterator(nameIterator);
		l.add(m);
		
		if (hasBuyer()){
        	m = new TSServerInfoModule(serverInfo.getModule(TSServerInfo.ADDRESS_MODULE_IDX));
        	m.addExtraInformation(TSServerInfoConstants.EXTRA_PARAM_MODULE_DESCRIPTION, 
        			TSServerInfoConstants.VALUE_PARAM_NAME_BUYERS);
        	m.setSaObjKey(SearchAttributes.BUYER_OBJECT);
        	m.clearSaKeys();
        	m.setSaKey(7, SearchAttributes.FROMDATE_MM_SLASH_DD_SLASH_YYYY);
        	m.setIteratorType(7, FunctionStatesIterator.ITERATOR_TYPE_FROM_DATE);
    		m.setSaKey(8, SearchAttributes.TODATE_MM_SLASH_DD_SLASH_YYYY);
        	
        	FilterResponse nameFilterHybridDoNotSkipUnique = NameFilterFactory.getDefaultSynonimNameInfoPinCandidateFilter( 
        			SearchAttributes.BUYER_OBJECT , searchId , m );
        	((SynonimNameFilter)nameFilterHybridDoNotSkipUnique).setIgnoreMiddleOnEmpty(true);
			//nameFilterHybridDoNotSkipUnique.setSkipUnique(false);
        	m.addFilter(rejectSavedDocuments);
			m.addFilter(DoctypeFilterFactory.getDoctypeBuyerFilter(searchId));
			m.addFilter(nameFilterHybridDoNotSkipUnique);
			m.addFilter(crossReferenceToInvalidated);
			addBetweenDateTest(m, false, true, true);
			addFilterForUpdate(m, true);
			m.addValidator(nameFilterHybridDoNotSkipUnique.getValidator());
			m.setIteratorType(0, FunctionStatesIterator.ITERATOR_TYPE_LCF_NAME_FAKE);
			ConfigurableNameIterator buyerNameIterator = (ConfigurableNameIterator) ModuleStatesIteratorFactory
				.getConfigurableNameIterator(m, searchId, new String[] {"L;F;", "L;f;", "L S;f;"});
			buyerNameIterator.setAllowMcnPersons(false);//B4361
			buyerNameIterator.setDoScottishNamesDerivations(true);
			buyerNameIterator.setDerivateWithSynonims(true);
			buyerNameIterator.setInitAgain(true); //initialize again after all parameters are set
			m.addIterator(buyerNameIterator);
			l.add(m);
        }
		
		//search by crossRef book and page list from RO documents
		m = new TSServerInfoModule(serverInfo.getModule(TSServerInfo.ADDRESS_MODULE_IDX));
		m.addExtraInformation(TSServerInfoConstants.EXTRA_PARAM_MODULE_DESCRIPTION, 
        		TSServerInfoConstants.VALUE_PARAM_LIST_RO_CROSSREF_INSTR);
		m.clearSaKeys();
		m.setSaKey(7, SearchAttributes.FROMDATE_MM_SLASH_DD_SLASH_YYYY);
		m.setSaKey(8, SearchAttributes.TODATE_MM_SLASH_DD_SLASH_YYYY);
		m.addFilter(rejectSavedDocuments);
		m.setSaObjKey(SearchAttributes.RO_CROSS_REF_INSTR_LIST);		    		    
	    m.setIteratorType(ModuleStatesIterator.TYPE_INSTRUMENT_LIST_NOT_AGAIN);	    
		m.getFunction(4).setIteratorType(FunctionStatesIterator.ITERATOR_TYPE_INSTRUMENT_LIST_FAKE);
	    l.add(m);
	    
	    /*
	    //mortgage resaver iterator B4269 - Display all parties for Mortgages
	    m = new TSServerInfoModule(serverInfo.getModule(TSServerInfo.ADDRESS_MODULE_IDX));
	    m.addExtraInformation(TSServerInfoConstants.EXTRA_PARAM_MODULE_DESCRIPTION, 
        		TSServerInfoConstants.VALUE_PARAM_LIST_MORTGAGE_INSTR);
		m.clearSaKeys();
		m.setIteratorType(4, FunctionStatesIterator.ITERATOR_TYPE_INSTRUMENT_LIST_FAKE);
		m.setValue(7, startDate);
		m.setValue(8, endDate);
		MortgageResaverIterator it = new MortgageResaverIterator(searchId);
		m.addIterator(it);
		//l.add(m);
		*/
	    serverInfo.setModulesForAutoSearch(l);
	}
	
	private InstrumentGenericIterator getInstrumentIterator() {
		InstrumentGenericIterator iterator = new InstrumentGenericIterator(searchId) {
			
			private Set<String> notSearchable = new HashSet<String>();
			private Set<String> searchable = new HashSet<String>();
			
			/**
			 * 
			 */
			private static final long serialVersionUID = 1L;

			@Override
			protected String cleanInstrumentNo(String inst, int year) {
				
				if (inst.startsWith("R")){
					searchable.add(inst);
					return inst;
				}
				
				String instNo = inst.replaceFirst("^0+", "");
				if(instNo.isEmpty()) {
					return instNo;
				}
				if(year == SimpleChapterUtils.UNDEFINED_YEAR){
					notSearchable.add(inst);
					return "";
				} else {
					searchable.add(inst);
				}
				instNo = org.apache.commons.lang.StringUtils.leftPad(instNo, 6, "0");
				
				instNo = "R" + year + instNo;
				
				return instNo;
			}
			
			@Override
			public List<InstrumentI> createDerrivations() {
				List<InstrumentI> derivations = super.createDerrivations();
				
				for (String intrument : notSearchable) {
					if(!searchable.contains(intrument)) {
						SearchLogger.info("Will not search with instrument [" + intrument+ "] because we could not find a valid date for it<br>", searchId);
					}
				}
				
				return derivations;
			}
			
		};
		return iterator;
	}

	
	/**
	 * 
	 * @param rsRequestSolverName
	 * @param rsSitePath
	 * @param rsServerID
	 * @param rsPrmNameLink
	 * @param searchId
	 * @param mid
	 */
	public ILDuPageRO(String rsRequestSolverName, String rsSitePath, String rsServerID, String rsPrmNameLink, long searchId, int mid) {
		super(rsRequestSolverName, rsSitePath, rsServerID, rsPrmNameLink, searchId, mid);
		resultType = MULTIPLE_RESULT_TYPE;
	}

	public ILDuPageRO(long searchId){
		super(searchId);
		resultType = MULTIPLE_RESULT_TYPE;
	}
	
	@Override
	protected void ParseResponse(String sAction, ServerResponse Response, int viParseID) throws ServerResponseException {
		
		String rsResponse = Response.getResult();
		
		//String initialResponse = rsResponse;
		ParsedResponse parsedResponse = Response.getParsedResponse();	
		switch (viParseID) {
		
			case ID_SEARCH_BY_NAME :
			case ID_SEARCH_BY_ADDRESS :
			case ID_SEARCH_BY_PARCEL :
			case ID_SEARCH_BY_SUBDIVISION_NAME :
			case ID_GET_IMAGE:
			    if(rsResponse.contains("image_error")){
			    	Response.getParsedResponse().setError("No Image found for this Document! Please close this tab and continue your work!");
			    	return;
			    }
			    if (rsResponse.indexOf("Create New Account") != -1)
	                return;
	            if (rsResponse.indexOf("An error has occured") != -1)
	                return;
	            if (rsResponse.matches("(?is).*Please\\s*fix\\s*the\\s*indicated\\s*errors\\s*and\\s*try\\s*again.*")){
	            	//extract the error
	            	Node errImg = HtmlParserTidy.getNodeByTagAndAttr(rsResponse, "img", "src", "images/Error.gif");
	            	String error = "";
	            	if (errImg.getParentNode().getNodeName().equalsIgnoreCase("span")
	            			&& errImg.getParentNode().getAttributes().getNamedItem("title") != null){
	            		error = errImg.getParentNode().getAttributes().getNamedItem("title").getNodeValue();
	            	}
	            	Response.getParsedResponse().setError(error);
	            	return;
	            }
	            if (rsResponse.matches("(?is).*Please\\s*enter\\s*your\\s*search\\s*criteria\\s*below\\s*and\\s*click\\s*on\\s*the\\s*search\\s*button.*")){
	            	Response.getParsedResponse().setError("The official site couldn't perform the request. Please try again later.");
	            	return;
	            }
	            
			    String table = "";
	            Node cn = HtmlParserTidy.getNodeById(rsResponse, "_ctl0_mainPlaceHolder__ctl0_CountySpecificResults1_dgResults", "table");
				//clean table
				if (cn != null){
					//remove tags we don't want
					HashSet<String> tags = new HashSet<String>();
					tags.add("script");
					tags.add("input");
					tags.add("img");

					cn = HtmlParserTidy.removeTags(cn, tags);

					//replace links
					String linkStart = CreatePartialLink(TSConnectionURL.idPOST);
					/*String page = "";
					if (rsResponse.matches("(?is).*href=\\\"javascript:__doPostBack\\('_ctl2\\$CountySpecificResults1\\$dgResults\\$_ctl104\\$_ctl0',''\\)\\\">1<.*")){
						page = "&page=2";
					} else {
						page = "&page=1";
					}
					//cn = replaceLinks(cn, page, linkStart, 0);*/

					//remove attribute for non admin listheader
					try {
						table = HtmlParserTidy.getHtmlFromNode(cn);

						//navigation links
						table = table.replaceAll("(?is)<a href=\"javascript:__doPostBack\\('(_ctl\\d+\\$mainPlaceHolder\\$_ctl\\d+\\$CountySpecificResults\\d+\\$dgResults\\$_ctl\\d+\\$_ctl\\d+)',''\\)",
								"<a href=" +linkStart + "/common.aspx?PT=RESULTS&__EVENTTARGET=$1");
						String nextLink = getInformationFromResponse(table, NEXT_LINK);
						if (!StringUtils.isEmpty(nextLink)){
							Response.getParsedResponse().setNextLink("<a href=" + nextLink+ ">2</a>");
						}
						//removeColor
						table = table.replaceFirst("(?s)<tr(.*?AdminListHeader.*?)>", "<teeeeeeeeeee$1>");
						table = table.replaceFirst("(?s)(.*)<tr(.*?AdminListHeader.*?)>", "$1<teeeeeeeeeee$2>");
						table = table.replaceAll("(?s)<tr.*?>", "<tr>");
						table = table.replaceAll("(?s)<teeeeeeeeeee", "<tr");
					} catch (Exception e) {
						e.printStackTrace();
					}
				}
             
				
				StringBuilder outputTable = new StringBuilder();
				
				Collection<ParsedResponse> smartParsedResponses = smartParseIntermediary(Response, 
						table, outputTable);
				
				if(smartParsedResponses.size() > 0) {
					parsedResponse.setResultRows(new Vector<ParsedResponse>(smartParsedResponses));
					parsedResponse.setOnlyResponse(outputTable.toString());
					
		            if (getSearch().getSearchType() == Search.PARENT_SITE_SEARCH ) {
		            	String header = parsedResponse.getHeader();
		               	String footer = parsedResponse.getFooter();                           	
		            	header += CreateSaveToTSDFormHeader(URLConnectionReader.MULTIPLE_SAVE_TO_TSD_ACTION, "POST");
		            	header += "\n<table width=\"100%\" border=\"1\" cellspacing=\"0\" cellpadding=\"2\">\n" +
//		            			"<tr><th rowspan=1>Select</th>" + //Select All<input type='checkbox' name='checkall' onclick='checkedAll(SaveToTSD);'
		            	        "<tr><th rowspan=1>"+ SELECT_ALL_CHECKBOXES + "</th>" +
		            			"<td>Document Number</td>" +
		            			"<td width=\"10%\">Date Recorded</td>" +
		            			"<td width=\"25%\">Party Name</td>" +
		            			"<td width=\"15%\">Party Type</td>" +
		            			"<td width=\"10%\">Document Type</td><" +
		            			"td width=\"35%\">Legal Description</td></tr>";

		            	Object numberOfUnsavedDocument = GetAttribute(NUMBER_OF_UNSAVED_DOCUMENTS);
		            	if(numberOfUnsavedDocument != null && numberOfUnsavedDocument instanceof Integer) {
		            		footer = "\n</table>" + CreateSaveToTSDFormEnd(SAVE_SELECTED_DOCUMENTS_BUTTON_LABEL, viParseID, (Integer)numberOfUnsavedDocument);
		            	} else {
		            		footer = "\n</table>" + CreateSaveToTSDFormEnd(SAVE_SELECTED_DOCUMENTS_BUTTON_LABEL, viParseID, -1);
		            	}

		            	
		            	parsedResponse.setHeader(header);
		            	parsedResponse.setFooter(footer);
		            }
					
					
				} 
				break;
				
			case ID_DETAILS :
				
				DocumentI document = parsedResponse.getDocument();
				
				if(document!= null) {
					msSaveToTSDFileName = document.getInstno() + ".html";
					Response.getParsedResponse().setFileName(getServerTypeDirectory() + msSaveToTSDFileName);
					msSaveToTSDResponce = rsResponse + CreateFileAlreadyInTSD();
				}
				break;
			
			case ID_DETAILS1:
				//captcha form
				String linkStart = CreatePartialLink(TSConnectionURL.idPOST);
				SimpleHtmlParser shp = new SimpleHtmlParser(Response.getResult());
				sAction = linkStart + "/" + shp.getForm("frmCommon").action.replaceFirst("&amp;", "&");
				ServerResponse sr = getCAPCHAForm(sAction);
				Response.setParsedResponse(sr.getParsedResponse());
				Response.setResult(sr.getResult());
				parser.Parse(Response.getParsedResponse(), Response.getResult(), Parser.NO_PARSE, getLinkPrefix(TSConnectionURL.idGET), TSServer.ID_GET_LINK);
				break;
			case ID_GET_LINK :
				if (Response.getResult().matches("(?is).*Verify\\s+and\\s+enter\\s+the" +
					"\\s+access\\s+code\\s+to\\s+obtain\\s+the\\s+detail\\s+information\\s+at\\s+no\\s+charge.*")){
						ParseResponse(sAction, Response, ID_DETAILS1);
				} else if (Response.getResult().matches("(?is).*Full\\s*Document\\s*Detail.*") ){
					ParseResponse(sAction, Response, ID_DETAILS);
				} else if (sAction.equals("/common.aspx?PT=RESULTS")){
					ParseResponse(sAction, Response, ID_SEARCH_BY_NAME);
				}
				break;
			case ID_SAVE_TO_TSD :
				document = parsedResponse.getDocument();
				
				if(document!= null) {
					msSaveToTSDFileName = document.getInstno() + ".html";
					Response.getParsedResponse().setFileName(getServerTypeDirectory() + msSaveToTSDFileName);
					msSaveToTSDResponce = rsResponse + CreateFileAlreadyInTSD();
				} else {
				// on save
				//if (Response.getResult().matches("(?is).*Full\\s*Document\\s*Detail.*") ){
				//	downloadingForSave = true;
					ParseResponse(sAction, Response, ID_DETAILS);
				//	downloadingForSave = false;
				//}
				}
				break;

		}
	}
	
	@Override
	public Collection<ParsedResponse> smartParseIntermediary(ServerResponse response, String table, StringBuilder outputTable) {
		LinkedHashMap<String, ParsedResponse> responses = new LinkedHashMap<String, ParsedResponse>();
		String linkPrefix = getLinkPrefix(TSConnectionURL.idPOST);
		StringBuilder newTable = new StringBuilder();
		newTable.append("<table BORDER='1' CELLPADDING='2'>");
		int numberOfUncheckedElements = 0;
		if(table != null) {
			try {
				org.htmlparser.Parser parser = org.htmlparser.Parser.createParser(table, null);
				NodeList rows = parser.extractAllNodesThatMatch(new TagNameFilter("tr"));
				for (int i = 0; i < rows.size(); i++) {
					NodeList tdList = rows.elementAt(i).getChildren().extractAllNodesThatMatch(new TagNameFilter("td"));
					if(tdList.size() == 6) {	//we must have exactly 6 tds
						NodeList aList = tdList.elementAt(0).getChildren().extractAllNodesThatMatch(new TagNameFilter("a"));
						if(aList.size() == 0) {
							//this is the header
							//newTable.append("<tr><td>&nbsp;</td><td><table><tr>" + rows.elementAt(i).toHtml() + "</tr></table></td></tr>");
							     newTable.append("<tr><th rowspan=1>" + SELECT_ALL_CHECKBOXES + "</th>")
								.append(tdList.elementAt(0).toHtml())
								.append(tdList.elementAt(1).toHtml())
								.append(tdList.elementAt(2).toHtml())
								.append(tdList.elementAt(3).toHtml())
								.append(tdList.elementAt(4).toHtml())
								.append(tdList.elementAt(5).toHtml())
								.append("</tr>");
							continue;
						} else {
							LinkTag link = (LinkTag)aList.elementAt(0);
							String documentNumber = link.getLinkText();
							ParsedResponse currentResponse = responses.get(documentNumber);							 
							if(currentResponse == null) {
								currentResponse = new ParsedResponse();
								responses.put(documentNumber, currentResponse);
							}
							RegisterDocumentI document = (RegisterDocumentI)currentResponse.getDocument();
							String tmpParty = tdList.elementAt(2).toPlainTextString();
							String tmpPartyType = tdList.elementAt(3).toPlainTextString();
							String serverDocType = tdList.elementAt(4).toPlainTextString();
							ResultMap resultMap = new ResultMap();
							String imageLink = createPartialLink(TSConnectionURL.idPOST, TSServerInfo.INSTR_NO_MODULE_IDX) + "&_ctl0:mainPlaceHolder:_ctl0:CountySpecificSearch1:txtDocNumber=" + documentNumber;
							if(document == null) {	//first time we find this document
								String recordedDateString = tdList.elementAt(1).toPlainTextString();
								
								String legalDescription = tdList.elementAt(5).toPlainTextString();
								
								resultMap.put("PropertyIdentificationSet.PropertyDescription", legalDescription);
								
								
			                	
			                	if(currentResponse.getImageLinksCount() == 0){
			                		currentResponse.addImageLink(new ImageLinkInPage (imageLink, documentNumber + ".pdf" ));
			                	}
								
								String responseHtml = "<tr><td>" +
										"<a target=\"_blank\" href=\"" + imageLink + "\">" + documentNumber + "</a>" +
									"</td>" +
									tdList.elementAt(1).toHtml() +
									tdList.elementAt(2).toHtml() +
									tdList.elementAt(3).toHtml() + 
									tdList.elementAt(4).toHtml() + 
									tdList.elementAt(5).toHtml() + "</tr>"; 
								String rawServerResponse = "<tr><td>" +
										documentNumber + 
										"</td>" +
										tdList.elementAt(1).toHtml() +
										tdList.elementAt(2).toHtml() +
										tdList.elementAt(3).toHtml() + 
										tdList.elementAt(4).toHtml() + 
										tdList.elementAt(5).toHtml() + "</tr>";
									
									//rows.elementAt(i).toHtml();
			    				int count = 1;
			    				while((i + 1 )<rows.size()) {
			    					tdList = rows.elementAt(i+1).getChildren().extractAllNodesThatMatch(new TagNameFilter("td"));
			    					if(tdList.size() == 6) {	//we must have exactly 6 tds
			    						aList = tdList.elementAt(0).getChildren().extractAllNodesThatMatch(new TagNameFilter("a"));
			    						if(aList.size() > 0) {
			    							link = (LinkTag)aList.elementAt(0);
			    							String newDocumentNumber = link.getLinkText();
			    							if(newDocumentNumber.equals(documentNumber)) {
			    								i++;
			    								tmpParty += "/" + tdList.elementAt(2).toPlainTextString();
			    								tmpPartyType += "/" + tdList.elementAt(3).toPlainTextString();
			    								responseHtml += "<tr><td>" +  
			    									"<a target=\"_blank\" href=\"" + imageLink + "\">" + documentNumber + "</a>" +
			    													"</td>" +
						    								tdList.elementAt(1).toHtml() +
						    								tdList.elementAt(2).toHtml() +
						    								tdList.elementAt(3).toHtml() + 
						    								tdList.elementAt(4).toHtml() + 
						    								tdList.elementAt(5).toHtml() + "</tr>";
			    								rawServerResponse += "<tr><td>" + documentNumber + "</td>" +
					    								tdList.elementAt(1).toHtml() +
					    								tdList.elementAt(2).toHtml() +
					    								tdList.elementAt(3).toHtml() + 
					    								tdList.elementAt(4).toHtml() + 
					    								tdList.elementAt(5).toHtml() + "</tr>";
			    								count++;
			    							} else {
			    								break;
			    							}
			    						}
			    					} else {
			    						break;
			    					}
			    				}
			    				tmpParty = StringEscapeUtils.unescapeHtml(tmpParty);
			    				resultMap.put("tmpPeople", tmpParty);
			    				resultMap.put("tmpPartyType", tmpPartyType);
							
			    				resultMap.put("SaleDataSet.InstrumentNumber", documentNumber);
			    				resultMap.put("SaleDataSet.RecordedDate", recordedDateString);
			    				resultMap.put("SaleDataSet.DocumentType", serverDocType);
			    				resultMap.put("OtherInformationSet.SrcType", "RO");
			    				ro.cst.tsearch.servers.functions.ILDuPageRO.partyNamesILDuPageRO(resultMap, getSearch().getID());
			    				ro.cst.tsearch.servers.functions.ILDuPageRO.legalILDuPageRO(resultMap, getSearch().getID());
			    				ro.cst.tsearch.servers.functions.ILDuPageRO.reparseDocTypeILDuPageRO(resultMap, getSearch().getID());
								
			    				
			    				resultMap.removeTempDef();
			    				
								Bridge bridge = new Bridge(currentResponse,resultMap,getSearch().getID());
								
								
								currentResponse.setAttribute(ParsedResponse.SERVER_ROW_RESPONSE, "<table BORDER='1' CELLPADDING='2'>" + 
										rows.elementAt(0).toHtml() + rawServerResponse + "</table>");
								document = (RegisterDocumentI)bridge.importData();
								document.setNote((String)resultMap.get("OtherInformationSet.Remarks"));
								
								currentResponse.setDocument(document);
								
								String checkBox = "checked";
								if (isInstrumentSaved(documentNumber, document, null) && !Boolean.TRUE.equals(getSearch().getAdditionalInfo("RESAVE_DOCUMENT"))) {
					    			checkBox = "saved";
					    		} else {
					    			numberOfUncheckedElements++;
					    			LinkInPage linkInPage = new LinkInPage(
					    					linkPrefix + "FK____" + documentNumber, 
					    					linkPrefix + "FK____" + documentNumber, 
					    					TSServer.REQUEST_SAVE_TO_TSD);
					    			checkBox = "<input type='checkbox' name='docLink' value='" + 
					    			linkPrefix + "FK____" + documentNumber + "'>";
					    			
			            			if(getSearch().getInMemoryDoc(linkPrefix + "FK____" + documentNumber)==null){
			            				getSearch().addInMemoryDoc(linkPrefix + "FK____" + documentNumber, currentResponse);
			            			}
			            			currentResponse.setPageLink(linkInPage);
			            			
					    			
					    		}
								currentResponse.setOnlyResponse("<tr><th rowspan=" + count + ">"  +  checkBox + "</th>" + responseHtml.substring(responseHtml.indexOf("<tr>") + 4));
								newTable.append(currentResponse.getResponse());
								
			    				
							} else {
								tmpParty = StringEscapeUtils.unescapeHtml(tmpParty);
			    				resultMap.put("tmpPeople", tmpParty);
			    				resultMap.put("tmpPartyType", tmpPartyType);
			    				resultMap.put("SaleDataSet.DocumentType", serverDocType);
			    				resultMap.put("OtherInformationSet.SrcType", "RO");
			    				resultMap.put("SaleDataSet.DocumentNumber", documentNumber);
			    				ro.cst.tsearch.servers.functions.ILDuPageRO.partyNamesILDuPageRO(resultMap, getSearch().getID());
			    				ro.cst.tsearch.servers.functions.ILDuPageRO.reparseDocTypeILDuPageRO(resultMap, getSearch().getID());
			    				resultMap.removeTempDef();
			    				Bridge bridge = new Bridge(currentResponse,resultMap,getSearch().getID());
			    				RegisterDocumentI documentTemp = (RegisterDocumentI)bridge.importData();
			    				for(NameI nameI : documentTemp.getGrantee().getNames()){
			    					if(!document.getGrantee().contains(nameI)) {
			    						document.getGrantee().add(nameI);
			    					}
			    				}
			    				for(NameI nameI : documentTemp.getGrantor().getNames()) {
			    					if(!document.getGrantor().contains(nameI)) {
			    						document.getGrantor().add(nameI);
			    					}
			    				}
			    				String rawServerResponse = (String)currentResponse.getAttribute(ParsedResponse.SERVER_ROW_RESPONSE);
			    				rawServerResponse = rawServerResponse.substring(0,rawServerResponse.length() - "</table>".length()) + 
					    				"<tr><td>" + documentNumber + "</td>" +
										tdList.elementAt(1).toHtml() +
										tdList.elementAt(2).toHtml() +
										tdList.elementAt(3).toHtml() + 
										tdList.elementAt(4).toHtml() + 
										tdList.elementAt(5).toHtml() + "</tr></table>"; 
			    				currentResponse.setAttribute(ParsedResponse.SERVER_ROW_RESPONSE, rawServerResponse);
			    				
			    				String responseHtml = currentResponse.getResponse();
			    				String countString = StringUtils.extractParameter(responseHtml, "rowspan=(\\d)+");
			    				try {
			    					int count = Integer.parseInt(countString);
			    					responseHtml = responseHtml.replaceAll("rowspan=(\\d)+", "rowspan=" + (count + 1));
			    					responseHtml += "<tr><td>" +  
									"<a target=\"_blank\" href=\"" + imageLink + "\">" + documentNumber + "</a>" +
													"</td>" +
		    								tdList.elementAt(1).toHtml() +
		    								tdList.elementAt(2).toHtml() +
		    								tdList.elementAt(3).toHtml() + 
		    								tdList.elementAt(4).toHtml() + 
		    								tdList.elementAt(5).toHtml() + "</tr>";
			    					currentResponse.setOnlyResponse(responseHtml);
			    				} catch (Exception e) {
									e.printStackTrace();
								}
			    				
			    				
							}
						}
					}
				}
			} catch (Exception e) {
				e.printStackTrace();
				return null;
			}		
		}
		newTable.append("</table>");
		outputTable.append(newTable);
		SetAttribute(NUMBER_OF_UNSAVED_DOCUMENTS, numberOfUncheckedElements);
		return responses.values();
	}
	
	protected String getFileNameFromLink(String link) {
		
		if(link.contains("FK____")){
			link = link.substring(link.indexOf("FK____") + "FK____".length());
			if(link.contains("&")){
				link = link.substring(0,link.indexOf("&"));
			}
		}
			
        return link;
    }
	
	protected String createPartialLink(int iActionType, int dispatcher) {
		StringBuilder build = new StringBuilder(msRequestSolverName);
		build.append("?");
		build.append(msServerID);
		build.append("&");
		build.append(RequestParams.SEARCH_ID);
		build.append("=");
		build.append(mSearch.getSearchID());
		build.append("&");
		build.append("dispatcher=");
		build.append(dispatcher);
		return build.toString();
	}
	
	@Override
	public ServerResponse SearchBy(TSServerInfoModule module, Object sd)
			throws ServerResponseException {
		if(module.getModuleIdx() == TSServerInfo.INSTR_NO_MODULE_IDX) {
			module.addExtraInformation(TSServerInfoConstants.EXTRA_PARAM_DO_NOT_REMOVE_IN_MEMORY_DOCS, true);
		}
		return super.SearchBy(module, sd);
	}
	
	@Override
	protected DownloadImageResult saveImage(ImageI image)
			throws ServerResponseException {
		ServerResponse serverResponse = new ServerResponse();
		try{
			setDoNotLogSearch(true);
			LinkParser linkParser = new LinkParser(image.getLink(0));
			SearchDataWrapper searchDataWrapper = new SearchDataWrapper();
			TSServerInfoModule module = getCurrentClassServerInfo().getModuleForSearch(
					Integer.parseInt(linkParser.getParamValue(URLConnectionReader.PRM_DISPATCHER)), 
					searchDataWrapper);
			
			module.getFunction(4).setData(linkParser.getParamValue("_ctl0:mainPlaceHolder:_ctl0:CountySpecificSearch1:txtDocNumber"));
			if(module.getModuleIdx() == TSServerInfo.INSTR_NO_MODULE_IDX) {
				module.addExtraInformation(TSServerInfoConstants.EXTRA_PARAM_DO_NOT_REMOVE_IN_MEMORY_DOCS, true);
			}
			searchDataWrapper.setImage(image);
			serverResponse = this.SearchBy(module, searchDataWrapper);
			if(serverResponse.getImageResult() == null) {
				serverResponse.setImageResult(new DownloadImageResult( DownloadImageResult.Status.ERROR, new byte[0], image.getContentType() ));
			}
			
		} catch (Exception e) {
			e.printStackTrace();
			serverResponse.setImageResult(new DownloadImageResult( DownloadImageResult.Status.ERROR, new byte[0], image.getContentType() ));
		} finally {
			setDoNotLogSearch(false);
		}
		return serverResponse.getImageResult();
	}

	@Override
	public void addDocumentAdditionalPostProcessing(DocumentI doc,
			ServerResponse response) {
		
		ServerResponse serverResponse = new ServerResponse();
		try{
			setDoNotLogSearch(true);
			SearchDataWrapper searchDataWrapper = new SearchDataWrapper();
			TSServerInfoModule module = new TSServerInfoModule(
					getDefaultServerInfo().getModule(TSServerInfo.INSTR_NO_MODULE_IDX)); 
			module.setDestinationPage(module.getDestinationPage().replace("getFakeImage=true", "getFakeImage=justDetails"));
				
			
			module.getFunction(4).setData(doc.getInstno());
			
			module.addExtraInformation(TSServerInfoConstants.EXTRA_PARAM_DO_NOT_REMOVE_IN_MEMORY_DOCS, true);			
			
			serverResponse = this.SearchBy(module, searchDataWrapper);
			
			String details = serverResponse.getResult();
			
			if(details != null) {
				updateDocument((RegisterDocumentI)doc, details);
				
			}
			
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			setDoNotLogSearch(false);
		}
		
		doc.updateDescription();
		response.getParsedResponse().setUseDocumentForSearchLogRow(true);
		
		super.addDocumentAdditionalPostProcessing(doc, response);
	}
	
	private void updateDocument(RegisterDocumentI document, String page) {
		DocumentsManagerI managerI = getSearch().getDocManager();
		
		if(page.contains("Full Document Detail")) {
			//we have a correct answer
			try {
				org.htmlparser.Parser parser = org.htmlparser.Parser.createParser(page, null);
				NodeList nodeList = parser.parse(null);
				NodeList mainTableList = nodeList.extractAllNodesThatMatch(new TagNameFilter("table"),true)
					.extractAllNodesThatMatch(new HasAttributeFilter("id","tblInsideDiv"));
				if(mainTableList.size() == 0) {
					return;
				}
				mainTableList.keepAllNodesThatMatch(new NotFilter(new TagNameFilter("a")),true);
				NodeList tempList = mainTableList.extractAllNodesThatMatch(new TagNameFilter("img"),true);
				for (int i = 0; i < tempList.size(); i++) {
					ImageTag imageTag = (ImageTag)tempList.elementAt(i);
					imageTag.setAttribute("src","/title-search/web-resources/images/spacer.gif");
				}
				
				tempList = mainTableList.extractAllNodesThatMatch(new HasAttributeFilter("class","headline"),true);
				for (int i = 0; i < tempList.size(); i++) {
					((TagNode)tempList.elementAt(i)).removeAttribute("class");
					((TagNode)tempList.elementAt(i)).setAttribute("style","color:#003333; font-size: 20px; font-weight: bold; line-height: 22px;");
				}
				tempList = mainTableList.extractAllNodesThatMatch(new HasAttributeFilter("class","contentBold"),true);
				for (int i = 0; i < tempList.size(); i++) {
					((TagNode)tempList.elementAt(i)).removeAttribute("class");
					((TagNode)tempList.elementAt(i)).setAttribute("style","color:#000000; font-size: 11px; font-weight: bold; ");
				}
				tempList = mainTableList.extractAllNodesThatMatch(new HasAttributeFilter("class","AdminListHeader"),true);
				for (int i = 0; i < tempList.size(); i++) {
					((TagNode)tempList.elementAt(i)).removeAttribute("class");
					((TagNode)tempList.elementAt(i)).setAttribute("style","color:#000000; font-size: 11px; font-weight: bold; ");
				}
				String serverDocType = "MISC";
				ResultMap resultMap = new ResultMap();
				
				tempList = mainTableList.extractAllNodesThatMatch(new TagNameFilter("table"),true)
					.extractAllNodesThatMatch(new HasAttributeFilter("id","Table3"));
				String description = "";
				if (tempList.size() == 1){
					TableRow[] rows = ((TableTag)tempList.elementAt(0)).getRows();
					if(rows.length > 1) {
						TableColumn[] columns = rows[10].getColumns();
						if (columns.length == 2){
							description = columns[1].toPlainTextString().trim();
							resultMap.put("PropertyIdentificationSet.PropertyDescription", description);
						}
					}
				}
				tempList = mainTableList.extractAllNodesThatMatch(new TagNameFilter("tr"),true);
				for (int i = 0; i < tempList.size(); i++) {
					TableRow tableRow = (TableRow)tempList.elementAt(i);
					TableColumn[] columns = tableRow.getColumns();
					if(columns.length == 4) {
						if (document instanceof TransferI || document instanceof MortgageI) {			
							if(columns[0].toPlainTextString().startsWith("Consideration:") 
									|| columns[2].toPlainTextString().startsWith("Rev Stamp:")){
								try {
									float considerationAmount = Float.parseFloat(columns[1].toPlainTextString().trim());
									if (document instanceof MortgageI) {
										((MortgageI)document).setMortgageAmount(considerationAmount);
									}
									if (document instanceof TransferI) {
										float revAmount = Float.parseFloat(columns[3].toPlainTextString().trim());
										if(considerationAmount == 0 && revAmount == 0) {
											
											try {
												managerI.getAccess();
												if(managerI.contains(document)) {
													document.setDocSubType("QUITCLAIM DEED");
												}
											} catch (Throwable t) {
												logger.error(searchId + ": Unexpected error while updating document doctype", t);
											} finally {
												managerI.releaseAccess();
											}
											
											if (description.matches("(?is)\\ARR\\s+.*")){//B 4445, comment 9-10
												String crossRefNo = description.replaceFirst("(?is).*?(R\\d+-?\\d+).*", "$1");
												boolean crossRefIsRealTransfer = getAmountsOfCrossRef(page, crossRefNo);
												if (crossRefIsRealTransfer){
													document.setDocSubType("RE-RECORDED TRANSFER");
												}
											}
											
										}
									}
								} catch (Exception e) {
									e.printStackTrace();
								}
							}
							if(columns[2].toPlainTextString().startsWith("Document Date:")){
								resultMap.put("SaleDataSet.InstrumentDate", columns[3].toPlainTextString().trim());
							}
						}
					} else if(columns.length == 2) {
						if(columns[0].toPlainTextString().trim().startsWith("Doc Desc:")) {
							serverDocType = columns[1].toPlainTextString().trim();
						}
					}
				}
				
				
				try {
					HtmlParser3 parserNew = new HtmlParser3(page);
					TableTag detailsTable = (TableTag) parserNew.getNodeById("Table3");
					if(detailsTable!=null) {
						String recordedDate = HtmlParser3.getValueFromNextCell(detailsTable.getChildren(),"Recording Date:", "", false);
						String instrumentDate = HtmlParser3.getValueFromNextCell(detailsTable.getChildren(),"Document Date:", "", false);
						String legalDescription = HtmlParser3.getValueFromNextCell(detailsTable.getChildren(),"Description:", "", false);
						resultMap.put("SaleDataSet.RecordedDate", recordedDate);
						resultMap.put("SaleDataSet.InstrumentDate", instrumentDate);
						resultMap.put("PropertyIdentificationSet.PropertyDescription", legalDescription);
						resultMap.put("SaleDataSet.DocumentType", serverDocType);
						resultMap.put(PropertyIdentificationSetKey.PLAT_BOOK.getKeyName(), 
								org.apache.commons.lang.StringUtils.defaultString(HtmlParser3.getValueFromNextCell(detailsTable.getChildren(),"Plat Book:", "", false)).replaceAll("^0+",""));
						resultMap.put(PropertyIdentificationSetKey.PLAT_NO.getKeyName(), 
								org.apache.commons.lang.StringUtils.defaultString(HtmlParser3.getValueFromNextCell(detailsTable.getChildren(),"Plat Page:", "", false)).replaceAll("^0+",""));
	    				
	    				ro.cst.tsearch.servers.functions.ILDuPageRO.legalILDuPageRO(resultMap, getSearch().getID());
	    				ro.cst.tsearch.servers.functions.ILDuPageRO.reparseDocTypeILDuPageRO(resultMap, getSearch().getID());
	    				
					}
				}catch(Exception e) {
					e.printStackTrace();
				}
				
				//ro.cst.tsearch.servers.functions.ILDuPageRO.reparseDocTypeILDuPageRO(resultMap, getSearch().getID());
				
				tempList = mainTableList.extractAllNodesThatMatch(new TagNameFilter("table"),true)
					.extractAllNodesThatMatch(new HasAttributeFilter("id","Table1"));
				if(tempList.size() > 0) {
					TableRow[] rows = ((TableTag)tempList.elementAt(0)).getRows();
					String names = "";
					String types = "";
					for (int i = 1; i < rows.length; i++) {
						TableColumn[] columns = rows[i].getColumns();
						if(columns.length == 6) {
							names += "/" + columns[0].toPlainTextString().trim();
							types += "/" + columns[1].toPlainTextString().trim();
						}
					}
					if(names.length() > 0) {
						names = names.substring(1);
						types = types.substring(1);
					}
					
					
					names = StringEscapeUtils.unescapeHtml(names);
    				resultMap.put("tmpPeople", names);
    				resultMap.put("tmpPartyType", types);
    				resultMap.put("SaleDataSet.DocumentType", serverDocType);
    				resultMap.put("OtherInformationSet.SrcType", "RO");
    				resultMap.put("SaleDataSet.InstrumentNumber", document.getInstno());
    				resultMap.put("SaleDataSet.DocumentNumber", document.getDocno());
    				ro.cst.tsearch.servers.functions.ILDuPageRO.partyNamesILDuPageRO(resultMap, getSearch().getID());
    				
    				tempList = mainTableList.extractAllNodesThatMatch(new TagNameFilter("table"),true)
						.extractAllNodesThatMatch(new HasAttributeFilter("id","_ctl0_mainPlaceHolder__ctl0_CountySpecificDetail1_dgPIN"));
    				if(tempList.size() == 1) {
    					rows = ((TableTag)tempList.elementAt(0)).getRows();
    					if(rows.length > 1) {
    						TableColumn[] columns = rows[1].getColumns();
    						if(columns.length == 6) {
    							resultMap.put("PropertyIdentificationSet.ParcelID", columns[1].toPlainTextString().trim());
    						}
    					}
    				}
    				
    				
    				resultMap.removeTempDef();
    				ParsedResponse currentResponse = new ParsedResponse();
    				Bridge bridge = new Bridge(currentResponse,resultMap,getSearch().getID());
    				RegisterDocumentI documentTemp = (RegisterDocumentI)bridge.importData();
    				    				
    				try {
						managerI.getAccess();
						if(managerI.contains(document)) {
							if(document.getDocType().equalsIgnoreCase(DocumentTypes.MISCELLANEOUS) && !documentTemp.getDocType().equalsIgnoreCase(DocumentTypes.MISCELLANEOUS)) {
								managerI.replace(document, documentTemp);
								documentTemp.setIndexId( DBManager.addDocumentIndex(Tidy.tidyParse(mainTableList.toHtml(), null), getSearch() ) );
								documentTemp.updateDescription();
							}else {
								for(NameI nameI : documentTemp.getGrantee().getNames()){
			    					if(!document.getGrantee().contains(nameI)) {
			    						document.getGrantee().add(nameI);
			    					}
			    				}
			    				for(NameI nameI : documentTemp.getGrantor().getNames()) {
			    					if(!document.getGrantor().contains(nameI)) {
			    						document.getGrantor().add(nameI);
			    					}
			    				}
			    				document.setInstrumentDate(documentTemp.getInstrumentDate());
			    				if(document.getProperties().size() > 0 ) {
			    					if(documentTemp.getProperties().size() > 0) {
			    						PropertyI origProperty = document.getProperties().iterator().next();
			    						PropertyI newProperty = documentTemp.getProperties().iterator().next();
			    						origProperty.setPin(newProperty.getPin());
			    						
			    						if(origProperty.getLegal() != null && newProperty.getLegal() != null
			    								&& newProperty.getLegal().getSubdivision() != null){
			    							origProperty.getLegal().setSubdivision(newProperty.getLegal().getSubdivision());
			    						}
			    					} 
			    				} else {
			    					document.setProperties(documentTemp.getProperties());
			    				}
							}		    				
						}
					} catch (Throwable t) {
						logger.error(searchId + ": Unexpected error while updating document", t);
					} finally {
						managerI.releaseAccess();
					}
    				
					
					
					
				}
				try {
					managerI.getAccess();
					if(managerI.contains(document)) {
						DBManager.updateDocumentIndex(document.getIndexId(),mainTableList.toHtml(), getSearch());
					}
				} catch (Throwable t) {
					logger.error(searchId + ": Unexpected error while updating document index in database", t);
				} finally {
					managerI.releaseAccess();
				}
			} catch (Exception e) {
				logger.error("Error while updating document");
			}
			
			if(!page.contains("Image...") && document.hasImage() && !document.getImage().isUploaded()){
				document.deleteImage(searchId, false);
			}
			
			if(document.hasImage()) {
				ro.cst.tsearch.connection.http2.ILDuPageRO site = 
					(ro.cst.tsearch.connection.http2.ILDuPageRO)HttpManager.getSite(getCurrentServerName(), searchId);
				try {
					RawResponseWrapper rawResponseWrapper = site.getResponseForLink(page, document.getInstno());
					ServerResponse serverResponse = new ServerResponse();
					try {
						if (rawResponseWrapper.getContentType().indexOf(TSConnectionURL.HTML_CONTENT_TYPE) != -1 || 
								rawResponseWrapper.getContentType().indexOf(TSConnectionURL.XML_CONTENT_TYPE) != -1)  {
							//try again, maybe we get lucky
							rawResponseWrapper = site.getResponseForLink(page, document.getInstno());
							if (!(rawResponseWrapper.getContentType().indexOf(TSConnectionURL.HTML_CONTENT_TYPE) != -1 || 
									rawResponseWrapper.getContentType().indexOf(TSConnectionURL.XML_CONTENT_TYPE) != -1)) {
								solveBinaryResponse(1, "", serverResponse, rawResponseWrapper, document.getImage().getPath(), null);
							}
						} else {
							solveBinaryResponse(1, "", serverResponse, rawResponseWrapper, document.getImage().getPath(), null);
						}
					} catch (ServerResponseException e) {
						e.printStackTrace();
					}
				} finally {
					// always release the HttpSite
					HttpManager.releaseSite(site);
				}
				try {
					managerI.getAccess();
					ImageI image = document.getImage();
					if(image.exists()) {
						image.setSaved(true);
						/*if(ServerConfig.isFileReplicationEnabled()){
							FileContent.replicateImage(image, searchId, false);
						}*/
						TsdIndexPageServer.uploadImageToSSf(document.getId(), searchId, false, false);
						logger.info(searchId + ": Saved ILDuPage Image" + image);
					}
				} catch (Throwable t) {
					logger.error("Error while saving image",t);
					
				} finally {
					managerI.releaseAccess();
				}
				
			}
		}
	}

	public static String getInformationFromResponse(String response, Pattern pattern){
		Matcher ma = pattern.matcher(response);
		if (ma.find()){
			return ma.group(1);
		}
		return "";
	}		
	
	public boolean getAmountsOfCrossRef(String rsResponse, String instrNo){
		
		String page = "";
		boolean crossRefIsRealTrans = false;
		
		ro.cst.tsearch.connection.http2.ILDuPageRO site = 
			(ro.cst.tsearch.connection.http2.ILDuPageRO)HttpManager.getSite(getCurrentServerName(), searchId);
		try {
			page = site.getResponse(rsResponse, instrNo);
			
		} finally {
			// always release the HttpSite
			HttpManager.releaseSite(site);
		}
		
		if (StringUtils.isEmpty(page))
			return false;
		
		String docType = "";
		float cons = 0, rev = 0;
		
		try {
			org.htmlparser.Parser parser = org.htmlparser.Parser.createParser(page, null);
			NodeList nodeList = parser.parse(null);
			NodeList mainTableList = nodeList.extractAllNodesThatMatch(new TagNameFilter("table"),true)
				.extractAllNodesThatMatch(new HasAttributeFilter("id","tblInsideDiv"));
			if(mainTableList.size() == 0) {
				return false;
			}
			NodeList tempList = mainTableList.extractAllNodesThatMatch(new TagNameFilter("table"),true)
				.extractAllNodesThatMatch(new HasAttributeFilter("id","Table3"));

			if (tempList.size() == 1){
				TableRow[] rows = ((TableTag)tempList.elementAt(0)).getRows();
				if(rows.length > 1) {
					TableColumn[] columns = rows[3].getColumns();
					if (columns[0].toPlainTextString().trim().startsWith("Doc Desc:")){
						docType = columns[1].toPlainTextString().trim();

					}
					columns = rows[4].getColumns();
					if (columns[0].toPlainTextString().trim().startsWith("Consideration:")){
						cons = Float.parseFloat(columns[1].toPlainTextString().trim());

					}
					if (columns[2].toPlainTextString().trim().startsWith("Rev Stamp:")){
						rev = Float.parseFloat(columns[3].toPlainTextString().trim());

					}
				}
			}
		} catch (Exception e) {
			logger.error("Error while parsing the html details");
		}
		
		if (DocumentTypes.isTransferDocType(docType, searchId)){
			if (cons != 0 || rev != 0){
				crossRefIsRealTrans = true;
			}
		}
		return crossRefIsRealTrans;
	}

	/**
	 * 
	 * @param p
	 * @param pr
	 * @param htmlString
	 * @param pageId
	 * @param linkStart
	 * @param action
	 * @throws ro.cst.tsearch.exceptions.ServerResponseException
	 */
	public static void splitResultRows(Parser p, ParsedResponse pr, String htmlString, int pageId, String linkStart, int action)
	throws ro.cst.tsearch.exceptions.ServerResponseException {
		p.splitResultRows(pr, htmlString, pageId, "<tr>", "</tr>", linkStart, action);
	}
	
	public ServerResponse getCAPCHAForm(String vsRequest) throws ServerResponseException{
		// query for image
		HttpSite site = HttpManager.getSite(getCurrentServerName(), searchId);
		String[] f = ((ro.cst.tsearch.connection.http2.ILDuPageRO)site).getImage(site);
    	String fileId = f[0];
    	String fileName = f[1];
    	
    	if(fileName == null || !FileUtils.existPath(fileName)){
    		logger.error("Image was not downloaded!");
    		return ServerResponse.createErrorResponse("Did not succeed to retrieve security image!");
    	}

    	// create relative fileName
    	if(fileName.startsWith(ServerConfig.getFilePath())){
			fileName = fileName.substring(ServerConfig.getFilePath().length());
		}
		if(fileName.contains("\\")){
			fileName = fileName.replace('\\', '/');
		}
		
		// create server response
		ServerResponse sr = new ServerResponse();
		sr.setResult("");
		ParsedResponse pr = sr.getParsedResponse();    		
		String page = 
		"<form name=\"SaveToTSD\" id=\"SaveToTSD\" action= \"" + vsRequest + "&fileId=" + fileId + "\" method=\"POST\">" +
			"<input type=\"hidden\" name=\"" + RequestParams.PARENT_SITE_SAVE_TYPE + "\" id=\"" + RequestParams.PARENT_SITE_SAVE_TYPE + "\" " +
				"value=\"" + RequestParamsValues.PARENT_SITE_SAVE_TYPE_WITHOUT_CROSSREF + "\">" + 
		    "<table border=\"1\" cellspacing=\"0\">" +
		    "<tr align=\"center\"><th bgcolor='#cccccc' align=\"center\" colspan=\"2\">Write security code from the image and click \"Submit\"</th></tr>" +  
			"<tr align=\"center\"><td align=\"center\"><img border='1' src='/title-search/fs?f=" + fileName + "&searchId=" + searchId + "'/></td>" +
		    "<td align=\"center\"><input type=\"text\" id=\"secEdit\" width=\"10\"/></td></tr>" +
		    "<tr align=\"center\"><td  align=\"center\" colspan=\"2\"> " + 
		    " <input type=\"button\" value=\"Submit\" onClick=\"javascript: var frm = getElementById('SaveToTSD'); frm.action = frm.action + '&strCAPTCHA=' + getElementById('secEdit').value; frm.submit(); \"/> " + 
		    "</td><tr>" +
		    "</table>" +
		"</form>";
		
        pr.setResponse(page);
        pr.setHeader("");
        pr.setFooter("");
        sr.setResult(page);
        sr.setDisplayMode(ServerResponse.HIDE_BACK_TO_PARENT_SITE_BUTTON);        
        solveHtmlResponse("", 1000, "SearchBy", sr, sr.getResult());
            		    		
		return sr;
	}
	
	
	@Override
	public String getPrettyFollowedLink(String initialFollowedLnk) {
		String instrNum = getInformationFromResponse(initialFollowedLnk, INSTRUMENTNO_ALREADY_FOLLOWED);
		if (StringUtils.isEmpty(instrNum)){
			instrNum = getInformationFromResponse(initialFollowedLnk, INSTRUMENTNO_ALREADY_FOLLOWED2);
			if(StringUtils.isEmpty(instrNum)) {
				return super.getPrettyFollowedLink(initialFollowedLnk);
			} else {
				return "<br/><span class='followed'>Instrument " + instrNum +
					" has already been saved.</span><br/>";
			}
		} else {
			return "<br/><span class='followed'>Instrument " + instrNum +
				" has already been saved.</span><br/>";
		}
	}
	
	@Override
	public void processLastRealTransfer(TransferI lastRealTransfer,
			OCRParsedDataStruct ocrRealData) {
		if(ocrRealData != null) {
			Vector<Instrument> instruments = ocrRealData.getInstrumentVector();
			if(instruments != null) {
				for (Instrument instrument : instruments) {
					ro.cst.tsearch.servers.functions.ILDuPageRO.fixInstrumentForOcr(instrument);					
				}
			}
		}
		super.processLastRealTransfer(lastRealTransfer, ocrRealData);
	}
	
	@Override
	public TSServerInfoModule getRecoverModuleFrom(RestoreDocumentDataI restoreDocumentDataI) {
		if(restoreDocumentDataI == null) {
			return null;
		}
		TSServerInfoModule module = null;
		if(StringUtils.isNotEmpty(restoreDocumentDataI.getInstrumentNumber())) {
			module = getDefaultServerInfo().getModule(TSServerInfo.ADDRESS_MODULE_IDX);
			module.clearSaKeys();
			module.forceValue(4, restoreDocumentDataI.getInstrumentNumber());
			module.addExtraInformation(TSServerInfoConstants.EXTRA_PARAM_DO_NOT_REMOVE_IN_MEMORY_DOCS, true);
		}
		return module;
	}
	
	public Object getImageDownloader(RestoreDocumentDataI document) {
		//return createPartialLink(TSConnectionURL.idPOST, TSServerInfo.INSTR_NO_MODULE_IDX) + "&_ctl0:mainPlaceHolder:_ctl0:CountySpecificSearch1:txtDocNumber=" + document.getInstrumentNumber();
		return getRecoverModuleFrom(document);
	}
	
	@Override
	protected void setCertificationDate() {
		try {
			if (CertificationDateManager.isCertificationDateInCache(dataSite)){
				String date = CertificationDateManager.getCertificationDateFromCache(dataSite);
				getSearch().getSa().updateCertificationDateObject(dataSite, Util.dateParser3(date));
			} else{
				ro.cst.tsearch.connection.http2.ILDuPageRO site = 
						(ro.cst.tsearch.connection.http2.ILDuPageRO)HttpManager.getSite(getCurrentServerName(), searchId);
					
				String page = null;
					
				try {
					page = site.getFormPageForCertDate("https://recorder.dupageco.org/common.aspx?PT=SEARCH");
				} finally {
					// always release the HttpSite
					HttpManager.releaseSite(site);
				}
					
				if (StringUtils.isNotEmpty(page)){
					Matcher certDateMatcher = certDatePattern.matcher(page);
					if(certDateMatcher.find()) {
						String date = certDateMatcher.group(1).trim();
						
						CertificationDateManager.cacheCertificationDate(dataSite, date);
						getSearch().getSa().updateCertificationDateObject(dataSite, Util.dateParser3(date));
					} else {
						CertificationDateManager.getLogger().error("Cannot parse certification date on " + getDataSite().getName() + " because pattern not found");
					}
				} else {
					CertificationDateManager.getLogger().error("Cannot parse certification date on " + getDataSite().getName() + " because html response is empty");
				}
			}
        } catch (Exception e) {
        	CertificationDateManager.getLogger().error("Error setting certification date on " + getDataSite().getName(), e);
        }
	}
	
}
