package ro.cst.tsearch.servers.types;

import static ro.cst.tsearch.bean.SearchAttributes.LD_BOOKPAGE;
import static ro.cst.tsearch.bean.SearchAttributes.LD_INSTRNO;
import static ro.cst.tsearch.bean.SearchAttributes.LD_LOTNO;
import static ro.cst.tsearch.bean.SearchAttributes.LD_SUBDIV_NAME;
import static ro.cst.tsearch.bean.SearchAttributes.RO_CROSS_REF_INSTR_LIST;
import static ro.cst.tsearch.datatrace.Utils.isEmpty;
import static ro.cst.tsearch.datatrace.Utils.setupSelectBox;
import static ro.cst.tsearch.search.FunctionStatesIterator.ITERATOR_TYPE_BOOK_FAKE;
import static ro.cst.tsearch.search.FunctionStatesIterator.ITERATOR_TYPE_BOOK_SEARCH;
import static ro.cst.tsearch.search.FunctionStatesIterator.ITERATOR_TYPE_INSTRUMENT_LIST_FAKE;
import static ro.cst.tsearch.search.FunctionStatesIterator.ITERATOR_TYPE_INSTRUMENT_LIST_SEARCH;
import static ro.cst.tsearch.search.FunctionStatesIterator.ITERATOR_TYPE_LAST_NAME_FAKE;
import static ro.cst.tsearch.search.FunctionStatesIterator.ITERATOR_TYPE_PAGE_FAKE;
import static ro.cst.tsearch.search.FunctionStatesIterator.ITERATOR_TYPE_PAGE_SEARCH;
import static ro.cst.tsearch.search.ModuleStatesIterator.TYPE_BOOK_PAGE_LIST_NOT_AGAIN;
import static ro.cst.tsearch.search.ModuleStatesIterator.TYPE_BOOK_PAGE_LIST_SEARCH_NOT_AGAIN;
import static ro.cst.tsearch.search.ModuleStatesIterator.TYPE_INSTRUMENT_LIST_NOT_AGAIN;
import static ro.cst.tsearch.search.ModuleStatesIterator.TYPE_INSTRUMENT_LIST_SEARCH_NOT_AGAIN;
import static ro.cst.tsearch.search.ModuleStatesIterator.TYPE_OCR_FULL_OR_BOOTSTRAPER;
import static ro.cst.tsearch.search.filter.FilterResponse.TYPE_BOOK_PAGE_FOR_NEXT_GENERIC;
import static ro.cst.tsearch.search.filter.FilterResponse.TYPE_INSTRUMENT_FOR_NEXT_GENERIC;
import static ro.cst.tsearch.servers.info.TSServerInfoConstants.EXTRA_PARAM_MODULE_DESCRIPTION;
import static ro.cst.tsearch.servers.info.TSServerInfoConstants.EXTRA_PARAM_SIMULATE_CROSSREF;
import static ro.cst.tsearch.servers.info.spitHTML.HTMLControl.HTML_SELECT_BOX;
import static ro.cst.tsearch.servers.info.spitHTML.HTMLControl.HTML_TEXT_FIELD;
import static ro.cst.tsearch.servers.info.spitHTML.HTMLControl.setHiddenParamMulti;
import static ro.cst.tsearch.servers.info.spitHTML.HTMLControl.setRequiredCriticalMulti;
import static ro.cst.tsearch.servers.info.spitHTML.HTMLObject.ORIENTATION_HORIZONTAL;
import static ro.cst.tsearch.servers.info.spitHTML.HTMLObject.PIXELS;

import java.io.File;
import java.math.BigDecimal;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Vector;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.stewart.ats.base.name.NameI;

import ro.cst.tsearch.Search;
import ro.cst.tsearch.bean.SearchAttributes;
import ro.cst.tsearch.connection.http.HTTPRequest;
import ro.cst.tsearch.connection.http2.HttpManager;
import ro.cst.tsearch.connection.http2.HttpSite;
import ro.cst.tsearch.exceptions.ServerResponseException;
import ro.cst.tsearch.parser.HtmlParserTidy;
import ro.cst.tsearch.search.FunctionStatesIterator;
import ro.cst.tsearch.search.filter.BetweenDatesFilterResponse;
import ro.cst.tsearch.search.filter.FilterResponse;
import ro.cst.tsearch.search.filter.RejectAlreadyPresentFilterResponse;
import ro.cst.tsearch.search.filter.SubdivisionNameFilterResponse2;
import ro.cst.tsearch.search.filter.newfilters.address.AddressFilterFactory;
import ro.cst.tsearch.search.filter.newfilters.date.DateFilterFactory;
import ro.cst.tsearch.search.filter.newfilters.legal.LegalFilterFactory;
import ro.cst.tsearch.search.filter.newfilters.name.GenericNameFilter;
import ro.cst.tsearch.search.filter.newfilters.name.NameFilterFactory;
import ro.cst.tsearch.search.filter.newfilters.pin.PINFilterFactory;
import ro.cst.tsearch.search.iterator.ModuleStatesIteratorFactory;
import ro.cst.tsearch.search.module.ConfigurableNameIterator;
import ro.cst.tsearch.search.validator.DocsValidator;
import ro.cst.tsearch.servers.TSConnectionURL;
import ro.cst.tsearch.servers.info.TSServerInfo;
import ro.cst.tsearch.servers.info.TSServerInfoConstants;
import ro.cst.tsearch.servers.info.TSServerInfoModule;
import ro.cst.tsearch.servers.info.spitHTML.FormatException;
import ro.cst.tsearch.servers.info.spitHTML.HTMLControl;
import ro.cst.tsearch.servers.info.spitHTML.PageZone;
import ro.cst.tsearch.servers.response.ImageLinkInPage;
import ro.cst.tsearch.servers.response.LinkInPage;
import ro.cst.tsearch.servers.response.ParsedResponse;
import ro.cst.tsearch.servers.response.Parser;
import ro.cst.tsearch.servers.response.ServerResponse;
import ro.cst.tsearch.servlet.URLConnectionReader;
import ro.cst.tsearch.titledocument.abstracts.DocumentTypes;
import ro.cst.tsearch.utils.FileUtils;
import ro.cst.tsearch.utils.GBManager;
import ro.cst.tsearch.utils.InstanceManager;
import ro.cst.tsearch.utils.SearchLogger;
import ro.cst.tsearch.utils.StringUtils;
import ro.cst.tsearch.utils.Tidy;

public class FLHillsboroughRO extends TSServer implements TSServerROLikeI{

    private static final long serialVersionUID = 1321456581802710L;
    
    private static final int NAME_MODULE_IDX      = 1;
    private static final int LEGAL_MODULE_IDX     = 2;
    private static final int BOOK_PAGE_MODULE_IDX = 3;    
    private static final int INSTR_MODULE_IDX     = 4;
    
    
    private static final int ID_NAME      = 301;
    private static final int ID_BOOK_PAGE = 302;
    private static final int ID_LEGAL     = 303;
    private static final int ID_INSTR     = 304;
    
    Pattern patFirstResult =  Pattern.compile("(?i)<A class='?stdFontResults'? href='showdetails\\.aspx\\?([^']+)'>");
    
    // Patterns
	private static final Pattern [] paramPairPatterns =  new Pattern []{
		Pattern.compile("<input type=\"hidden\" name=\"([^\"]+)\" value=\"([^\"]*)\""),
		Pattern.compile("<input name=\"([^\"]+)\"(?: id=\"[^\"]+\")? type=\"text\" value=\"([^\"]*)\"")
	};	
	private static final Pattern [] paramNamePattern = new Pattern []{
		Pattern.compile("input name=\"([^\"]+)\" type=\"text\" maxlength=\"")
	};
	private static final Pattern detailsPattern = Pattern.compile("id=\\d+&rn=\\d+&pi=\\d+&ref=search");	
	private static final Pattern crtResultsPattern = Pattern.compile("(?i)<span id=\"lblRecordPos\"[^>]*>(\\d+ - \\d+)</span>");
	private static final Pattern totResultsPattern = Pattern.compile("(?i)<span id=\"lblRecordCount\"[^>]*>(\\d+)</span>");
	private static final Pattern imageRequestPattern = Pattern.compile("Link=([^&]+.*)&atsInstr=([A-Z0-9_]*)");
	
    // indexes of parameters
    private static final int IDX_EVENTTARGET = 0;
    private static final int IDX_EVENTARGUMENT = 1;
    private static final int IDX_VIEWSTATE = 2;
    public static final int IDX_SEARCHTYPE = 3;
    private static final int IDX_SEARCHTYPEDESC = 4;
    private static final int IDX_DDPARTYTYPE = 5;
    private static final int IDX_TXTNAME = 6;
    private static final int IDX_DDBOOKTYPE = 7;
    public static final int IDX_TXTBOOK = 8;
    public static final int IDX_TXTPAGE = 9;
    public static final int IDX_TXTINSTRUMENTNUMBER = 10;
    private static final int IDX_TXTLOWERBOUND = 11;
    private static final int IDX_TXTUPPERBOUND = 12;
    private static final int IDX_DDPARCELCHOICE = 13;
    private static final int IDX_TXTPARCELID = 14;
    private static final int IDX_DDCOMMENTSCHOICE = 15;
    private static final int IDX_TXTCOMMENTS = 16;
    private static final int IDX_TXTLEGALFIELDS = 17;
    private static final int IDX_TXTLEGALDESC = 18;
    private static final int IDX_TEXTBOX2 = 19;
    private static final int IDX_DDCASENUMBERCHOICE = 20;
    private static final int IDX_TXTCASENUMBER = 21;
    private static final int IDX_TXTDOCTYPES = 22;
    private static final int IDX_CBOCATEGORIES = 23;
    private static final int IDX_TXTRECORDDATE = 24;
    private static final int IDX_TXTBEGINDATE = 25;
    private static final int IDX_TXTENDDATE = 26;
    private static final int IDX_CMDSUBMIT = 27;

    private boolean downloadingForSave = false;
    
    private static final String CATEGORY_SELECT = 
	    "<select name=\"cboCategories\">" +
		"<option selected=\"selected\" value=\"n/a\">n/a</option>" +
		"<option value=\"AFF,AGD,AGR,ASG,ASINT,BND,CCJ,CND,COHOME,CP,CTF,D,DC,DRCP,DRJUD,EAS,EXP,FIN,GOV,JUD,LN,LP,MAR,MARC,MARD,MARP,MARPC,MARPS,MARW,MIL,MOD,MTG,NCL,NOC,NOT,ORD,POA,PR,PRO,PRREL,REL,RES,SAT,TER,TRA,VOID\">ALL DOC. TYPES</option>" +
		"<option value=\"DC\">DC</option>" +
		"<option value=\"AGD,D\">DEEDS</option>" +
		"<option value=\"MAR,MARA,MARC,MARD,MARP,MARPC,MARPS,MARW\">MARRIAGE LICENSE</option>" +
		"<option value=\"MTG,MOD\">MTG</option>" +
		"</select>";
    
    private static final String PARTY_TYPE_SELECT = 
    	"<select name=\"ddPartyType\">" +
    	"<option selected=\"selected\" value=\"-1\">Both</option>" +
    	"<option value=\"0\">Direct Name</option>" +
    	"<option value=\"1\">Reverse Name</option>" +
    	"</select>";
    
    private static final String BOOK_TYPE_SELECT = 
    	"<select name=\"ddBookType\">" +
    	"<option value=\"C\">Condominium Plan</option>" + 
    	"<option value=\"D\">Deed Plat</option>" + 
    	"<option value=\"L\">Marriage License</option>" + 
    	"<option value=\"M\">Minor Subdivision Survey</option>" + 
    	"<option selected=\"selected\" value=\"O\">Official Records</option>" + 
    	"<option value=\"P\">Subdivision Plat Map</option>" + 
    	"<option value=\"R\">Right of Way Monument</option>" + 
    	"<option value=\"S\">Survey &amp; Location Map</option>" + 
    	"<option value=\"T\">Right of Way Transfer</option>" + 
    	"<option value=\"V\">Right of Way Reservation</option>" + 
    	"<option value=\"W\">Maintained Right of Way</option>" + 
    	"</select>";
    
    private static final String PARCEL_TYPE_SELECT =
		"<select name=\"ddParcelChoice\">" + 
		"<option selected=\"selected\" value=\"0\">Begins with</option>" + 
		"<option value=\"1\">Ends with</option>" + 
		"</select>";
    
    private static final String COMMENTS_TYPE_SELECT = 
    	"<select name=\"ddCommentsChoice\">" +
		"<option selected=\"selected\" value=\"0\">Begins</option>" +
		"<option value=\"1\">Contains</option>" +
		"</select>";
    
    private static final String CASE_TYPE_SELECT = 
    	"<select name=\"ddCaseNumberChoice\">" +
		"<option selected=\"selected\" value=\"0\">Begins</option>" +
		"<option value=\"1\">Contains</option>" +
		"</select>";
    
    public FLHillsboroughRO(String rsRequestSolverName, String rsSitePath, String rsServerID, String rsPrmNameLink,long searchId, int mid) {
        super(rsRequestSolverName, rsSitePath, rsServerID, rsPrmNameLink,searchId,mid);
        resultType = MULTIPLE_RESULT_TYPE;
    }
        
    public ServerResponse SearchBy(TSServerInfoModule module, Object sd) throws ServerResponseException {
    	
    	// implement the txtDocTypes and cboCategories logic
    	String txtDocTypes = module.getFunction(IDX_TXTDOCTYPES).getParamValue();
    	String cboCategories = module.getFunction(IDX_CBOCATEGORIES).getParamValue();
    	if(StringUtils.isEmpty(txtDocTypes)){
    		if("n/a".equals(cboCategories )){
    			txtDocTypes = "All Document Types";
    		} else {
    			txtDocTypes = cboCategories;
    		}
    	} else {
    		cboCategories = "n/a";
    	}
    	module.getFunction(IDX_TXTDOCTYPES).setParamValue(txtDocTypes);
    	module.getFunction(IDX_CBOCATEGORIES).setParamValue(cboCategories);
    	
    	// implement copying 
        return SearchBy(true, module, sd);
    }
	@Override
	public TSServerInfo getDefaultServerInfo() {
		
		TSServerInfo si = new TSServerInfo(4);
		si.setServerAddress("pubrec3.hillsclerk.com");
		si.setServerIP("pubrec3.hillsclerk.com");
		si.setServerLink("http://pubrec3.hillsclerk.com" );
        
		// Name Search
		try {
			
			TSServerInfoModule 		
			sim = si.ActivateModule(NAME_MODULE_IDX, 29);
			sim.setName("Name");
			sim.setDestinationPage("/oncore/search.aspx");
			sim.setRequestMethod(TSConnectionURL.idPOST);
			sim.setParserID(ID_NAME);

			PageZone pz = new PageZone("SearchByName", "Name Search", ORIENTATION_HORIZONTAL, null, 500, 50, PIXELS , true);
            HTMLControl controls [] = createHtmlControls(sim);	

            sim.getFunction(IDX_SEARCHTYPE).setDefaultValue("fullname");
            sim.getFunction(IDX_SEARCHTYPEDESC).setDefaultValue("Search By Name");

            // unhide the desired controls
            setHiddenParamMulti(false, controls[IDX_DDPARTYTYPE],
					controls[IDX_TXTNAME], controls[IDX_TXTDOCTYPES],
					controls[IDX_CBOCATEGORIES], controls[IDX_TXTBEGINDATE],
					controls[IDX_TXTENDDATE]);	
            
            // required
            setRequiredCriticalMulti(true, controls[IDX_TXTNAME]);
            
            // set position of visible controls
            controls[IDX_DDPARTYTYPE].  setColAndRow(1,1,1,1);
            controls[IDX_TXTNAME].      setColAndRow(1,1,2,2);     
            controls[IDX_TXTDOCTYPES].  setColAndRow(1,1,3,3);     
            controls[IDX_CBOCATEGORIES].setColAndRow(1,1,4,4);
            controls[IDX_TXTBEGINDATE]. setColAndRow(1,1,5,5);
            controls[IDX_TXTENDDATE].   setColAndRow(1,1,6,6);
            
            sim.getFunction(IDX_TXTNAME).setSaKey(SearchAttributes.OWNER_LF_NAME);
            
            // add all controls to pagezone
            pz.addHTMLObjectMulti(controls);
            
			sim.setModuleParentSiteLayout(pz);
			
		}catch(Exception e){
			logger.error(e);
		}
		
		// Book Page Search
		try {
			
			TSServerInfoModule 		
			sim = si.ActivateModule(BOOK_PAGE_MODULE_IDX, 29);
			sim.setName("Book Page");
			sim.setDestinationPage("/oncore/search.aspx");
			sim.setRequestMethod(TSConnectionURL.idPOST);
			sim.setParserID(ID_NAME);

			PageZone pz = new PageZone("SearchByBookPage", "Book Page Search", ORIENTATION_HORIZONTAL, null, 500, 50, PIXELS , true);
            HTMLControl controls [] = createHtmlControls(sim);	

            sim.getFunction(IDX_SEARCHTYPE).setDefaultValue("bookpage");
            sim.getFunction(IDX_SEARCHTYPEDESC).setDefaultValue("Search By Book/Page");

            // unhide the desired controls
            setHiddenParamMulti(false, controls[IDX_DDBOOKTYPE],
					controls[IDX_TXTBOOK], controls[IDX_TXTPAGE]);
            
            // required
            setRequiredCriticalMulti(true, controls[IDX_TXTBOOK]);
            setRequiredCriticalMulti(true, controls[IDX_TXTPAGE]);
            
            // set position of visible controls
            controls[IDX_DDBOOKTYPE].setColAndRow(1,2,1,1);
            controls[IDX_TXTBOOK].   setColAndRow(1,1,2,2);     
            controls[IDX_TXTPAGE].   setColAndRow(2,2,2,2);     
            
            controls[IDX_TXTPAGE].setJustifyField(false);
            
            sim.getFunction(IDX_TXTBOOK).setSaKey(SearchAttributes.LD_BOOKNO);
            sim.getFunction(IDX_TXTPAGE).setSaKey(SearchAttributes.LD_PAGENO);
            
            // add all controls to pagezone
            pz.addHTMLObjectMulti(controls);
            
			sim.setModuleParentSiteLayout(pz);
			
		}catch(Exception e){
			logger.error(e);
		}
		
		
		//Image  Search
		if(false) try {
			TSServerInfoModule 		
			sim = si.ActivateModule(TSServerInfo.IMG_MODULE_IDX, 29);
			sim.setName("Image");
			sim.setDestinationPage("/oncore/search.aspx");
			sim.setRequestMethod(TSConnectionURL.idPOST);
			sim.setParserID(ID_GET_IMAGE);

			PageZone pz = new PageZone("SearchByBookPage", "Image Search", ORIENTATION_HORIZONTAL, null, 500, 50, PIXELS , true);
            HTMLControl controls [] = createHtmlControls(sim);	

            sim.getFunction(IDX_SEARCHTYPE).setDefaultValue("img");
            sim.getFunction(IDX_SEARCHTYPEDESC).setDefaultValue("Search By Book/Page");

            // unhide the desired controls
            setHiddenParamMulti(false, controls[IDX_DDBOOKTYPE],
					controls[IDX_TXTBOOK], controls[IDX_TXTPAGE],controls[IDX_TXTINSTRUMENTNUMBER]);
            
            // required
            controls[IDX_TXTINSTRUMENTNUMBER].setRequiredExcl(true);
            controls[IDX_TXTBOOK].setRequiredExcl(true);
            
            // set position of visible controls
            controls[IDX_DDBOOKTYPE].setColAndRow(1,2,1,1);
            controls[IDX_TXTBOOK].   setColAndRow(1,1,2,2);     
            controls[IDX_TXTPAGE].   setColAndRow(2,2,2,2);     
            controls[IDX_TXTINSTRUMENTNUMBER]. setColAndRow(1,2,3,3); 
            controls[IDX_TXTPAGE].setJustifyField(false);
            
            sim.getFunction(IDX_TXTBOOK).setSaKey(SearchAttributes.LD_BOOKNO);
            sim.getFunction(IDX_TXTPAGE).setSaKey(SearchAttributes.LD_PAGENO);
            
            // add all controls to pagezone
            pz.addHTMLObjectMulti(controls);
            
			sim.setModuleParentSiteLayout(pz);
			
		}catch(Exception e){
			logger.error(e);
		}
		
		// Instrument Search
		try {
			
			TSServerInfoModule 		
			sim = si.ActivateModule(INSTR_MODULE_IDX, 29);
			sim.setName("Instrument");
			sim.setDestinationPage("/oncore/search.aspx");
			sim.setRequestMethod(TSConnectionURL.idPOST);
			sim.setParserID(ID_INSTR);

			PageZone pz = new PageZone("SearchByInstrument", "Instrument Search", ORIENTATION_HORIZONTAL, null, 500, 50, PIXELS , true);
            HTMLControl controls [] = createHtmlControls(sim);	

            sim.getFunction(IDX_SEARCHTYPE).setDefaultValue("instrument");
            sim.getFunction(IDX_SEARCHTYPEDESC).setDefaultValue("Search By Instrument #");

            // required
            setRequiredCriticalMulti(true, controls[IDX_TXTINSTRUMENTNUMBER]);
            
            // unhide the desired controls
            setHiddenParamMulti(false, controls[IDX_TXTINSTRUMENTNUMBER]);
            
            // set position of visible controls
            controls[IDX_TXTINSTRUMENTNUMBER].setColAndRow(1,1,1,1);  
            
            // add all controls to pagezone
            pz.addHTMLObjectMulti(controls);
            
			sim.setModuleParentSiteLayout(pz);
			
		}catch(Exception e){
			logger.error(e);
		}
		
		// Legal Search
		try {
			
			TSServerInfoModule 		
			sim = si.ActivateModule(LEGAL_MODULE_IDX, 29);
			sim.setName("Legal");
			sim.setDestinationPage("/oncore/search.aspx");
			sim.setRequestMethod(TSConnectionURL.idPOST);
			sim.setParserID(ID_LEGAL);

			PageZone pz = new PageZone("SearchByLegal", "Legal Search", ORIENTATION_HORIZONTAL, null, 500, 50, PIXELS , true);
            HTMLControl controls [] = createHtmlControls(sim);	

            sim.getFunction(IDX_SEARCHTYPE).setDefaultValue("comments");
            sim.getFunction(IDX_SEARCHTYPEDESC).setDefaultValue("Search By Legal");

            // required
            setRequiredCriticalMulti(true, controls[IDX_TXTCOMMENTS]);
            
            // unhide the desired controls
            setHiddenParamMulti(false, controls[IDX_DDCOMMENTSCHOICE],
					controls[IDX_TXTCOMMENTS], controls[IDX_TXTDOCTYPES],
					controls[IDX_CBOCATEGORIES], controls[IDX_TXTBEGINDATE],
					controls[IDX_TXTENDDATE]);	
            
            // set position of visible controls
            controls[IDX_DDCOMMENTSCHOICE].setColAndRow(1,1,1,1);
            controls[IDX_TXTCOMMENTS].     setColAndRow(1,1,2,2);     
            controls[IDX_TXTDOCTYPES].     setColAndRow(1,1,3,3);     
            controls[IDX_CBOCATEGORIES].   setColAndRow(1,1,4,4);
            controls[IDX_TXTBEGINDATE].    setColAndRow(1,1,5,5);
            controls[IDX_TXTENDDATE].      setColAndRow(1,1,6,6);
            
            sim.getFunction(IDX_TXTCOMMENTS).setSaKey(SearchAttributes.LD_SUBDIV_NAME);
            
            // add all controls to pagezone
            pz.addHTMLObjectMulti(controls);
            
			sim.setModuleParentSiteLayout(pz);
			
		}catch(Exception e){
			logger.error(e);
		}
		
		si.setupParameterAliases();
		
		setModulesForAutoSearch(si);
		setModulesForGoBackOneLevelSearch(si);
		
		return si;
	}
	
    private static FilterResponse getDefaultNameFilter(String saKey,long searchId,TSServerInfoModule module){
    	GenericNameFilter fr = new GenericNameFilter( saKey, searchId, false, module , true);
		fr.setThreshold(new BigDecimal(NameFilterFactory.NAME_FILTER_THRESHOLD));
		return fr;
	}
    
	protected static String BUYER_DOC_TYPES_STR;
	protected static String GO_BACK_DOC_TYPES_STR;
	
	static {
	
		String [] buyerDocTypes = { "CCJ", "COHOME", "CP", "CTF",
				"DC", "DRJUD", "JUD", "MAR", "MARC", "MARD", "MARP", "MARPC",
				"MARPS", "MARW", "ORD", "PRO", "FIN", "LN", "LP", "MEDLN", "AGD",
				"AGR", "ASINT", "BND", "DRCP", "EXP", "GOV", "MARA", "MIL",
				"MTGNIT", "MTGNT", "NCL", "NOC", "NOT", "POA", "ROWM", "ROWR",
				"ROWT", "VOID" };
		BUYER_DOC_TYPES_STR = buyerDocTypes[0];
		for(int i=1; i<buyerDocTypes.length; i++){
			BUYER_DOC_TYPES_STR += "," + buyerDocTypes[i];
		}
		
		String[] goBackDocTypes = { "CCJ", "COHOME", "CP", "CTF", "DC", "DRJUD",
				"JUD", "MAR", "MARC", "MARD", "MARP", "MARPC", "MARPS", "MARW",
				"ORD", "PRO", "FIN", "LN", "LP", "MEDLN", "AGD", "AGR", "ASINT",
				"BND", "DRCP", "EXP", "GOV", "MARA", "MIL", "MTGNIT", "MTGNT",
				"NCL", "NOC", "NOT", "POA", "ROWM", "ROWR", "ROWT", "VOID", "D",
				"TAXDEED", "TRA" };
		GO_BACK_DOC_TYPES_STR = goBackDocTypes[0];
		for(int i=1; i<goBackDocTypes.length; i++){
			GO_BACK_DOC_TYPES_STR += "," + goBackDocTypes[i];
		}
		
	}
	
	@Override
	protected void setModulesForAutoSearch(TSServerInfo serverInfo) {
		List<TSServerInfoModule> modules = new ArrayList<TSServerInfoModule>();
		Search global = getSearch();
		int searchType = global.getSearchType();
		if(searchType == Search.AUTOMATIC_SEARCH) {
		
			TSServerInfoModule module;
					
			FilterResponse alreadyPresentFilter = new RejectAlreadyPresentFilterResponse(searchId);   
			FilterResponse nameFilterOwner   	= getDefaultNameFilter( SearchAttributes.OWNER_OBJECT, searchId, null );
			FilterResponse legalFilter 		    = LegalFilterFactory.getDefaultLegalFilter( searchId );
			FilterResponse subdivFilter         = new SubdivisionNameFilterResponse2(searchId);
			FilterResponse intervalFilter       = BetweenDatesFilterResponse.getDefaultIntervalFilter(searchId);
			
			DocsValidator alreadyPresentValidator = alreadyPresentFilter.getValidator();
			DocsValidator legalValidator          = legalFilter.getValidator();
			DocsValidator subdivValidator         = subdivFilter.getValidator();
			DocsValidator intervalValidator       = intervalFilter.getValidator();
			
			DocsValidator crossRefValidators [] = new DocsValidator[]{ legalValidator, subdivValidator, intervalValidator, alreadyPresentValidator};		
			
			// Owner search
			module = new TSServerInfoModule(serverInfo.getModule(NAME_MODULE_IDX));
			module.clearSaKeys();
			module.setSaObjKey(SearchAttributes.OWNER_OBJECT);
			
			module.setSaKey(IDX_TXTBEGINDATE, SearchAttributes.FROMDATE_MM_SLASH_DD_SLASH_YYYY);
			module.setIteratorType(IDX_TXTBEGINDATE, FunctionStatesIterator.ITERATOR_TYPE_FROM_DATE);
			module.setSaKey(IDX_TXTENDDATE, SearchAttributes.TODATE_MM_SLASH_DD_SLASH_YYYY);
			module.setIteratorType(IDX_TXTNAME, ITERATOR_TYPE_LAST_NAME_FAKE);		
			
			ConfigurableNameIterator nameIterator = (ConfigurableNameIterator) ModuleStatesIteratorFactory
				.getConfigurableNameIterator(module, searchId, true, new String[] { "L F;;", "L M;;" });
			module.addIterator(nameIterator);
			
			module.addFilters(nameFilterOwner, legalFilter, alreadyPresentFilter);		
			addBetweenDateTest(module, true, true, false);
			addFilterForUpdate(module, true);
			
			module.addCrossRefValidators(crossRefValidators);
			modules.add(module);
			
			// Legal search
			String lot = getSearchAttribute(LD_LOTNO);
			String sub = getSearchAttribute(LD_SUBDIV_NAME);		
			if(!isEmpty(lot) && !isEmpty(sub)){
				module = new TSServerInfoModule(serverInfo.getModule(LEGAL_MODULE_IDX));
				module.clearSaKeys();
				module.setData(IDX_TXTCOMMENTS, "L " + lot + " " + sub);
				module.addFilters(legalFilter, subdivFilter, alreadyPresentFilter);
				if(global.getSa().isUpdate() || global.getSa().isDateDown()) {
					module.addValidator(intervalValidator);
				}
				module.addCrossRefValidators(crossRefValidators);
				modules.add(module);
			}
			
			// Search by book page list from AO		
			module = new TSServerInfoModule(serverInfo.getModule(BOOK_PAGE_MODULE_IDX));
			module.clearSaKeys();
			module.setIteratorType(TYPE_BOOK_PAGE_LIST_NOT_AGAIN);
			module.setSaObjKey(SearchAttributes.INSTR_LIST);		
			module.setIteratorType(IDX_TXTBOOK, ITERATOR_TYPE_BOOK_FAKE);
			module.setIteratorType(IDX_TXTPAGE, ITERATOR_TYPE_PAGE_FAKE);		
			module.addFilterForNextType(TYPE_BOOK_PAGE_FOR_NEXT_GENERIC);
			module.addValidators(crossRefValidators);
			module.addCrossRefValidators(crossRefValidators);
			modules.add(module);		
			
			// Search by book and page list from search page
			module = new TSServerInfoModule(serverInfo.getModule(BOOK_PAGE_MODULE_IDX));
			module.clearSaKeys();
			module.setIteratorType(TYPE_BOOK_PAGE_LIST_SEARCH_NOT_AGAIN);
			module.setSaObjKey(LD_BOOKPAGE);		
			module.setIteratorType(IDX_TXTBOOK, ITERATOR_TYPE_BOOK_SEARCH);
			module.setIteratorType(IDX_TXTPAGE, ITERATOR_TYPE_PAGE_SEARCH);		
			module.addFilterForNextType(TYPE_BOOK_PAGE_FOR_NEXT_GENERIC);
			module.addValidators(crossRefValidators);
			module.addCrossRefValidators(crossRefValidators);
			modules.add(module);
			
			// Search by instrument list from search page
			module = new TSServerInfoModule(serverInfo.getModule(INSTR_MODULE_IDX));
			module.clearSaKeys();
			module.setIteratorType(TYPE_INSTRUMENT_LIST_SEARCH_NOT_AGAIN);
			module.setSaObjKey(LD_INSTRNO);
			module.setIteratorType(IDX_TXTINSTRUMENTNUMBER, ITERATOR_TYPE_INSTRUMENT_LIST_SEARCH);
			module.addFilterForNextType(TYPE_INSTRUMENT_FOR_NEXT_GENERIC);
			module.addValidators(crossRefValidators);
			module.addCrossRefValidators(crossRefValidators);
			modules.add(module);
			
			// Search by cross reference book and page from RO
			module = new TSServerInfoModule(serverInfo.getModule(BOOK_PAGE_MODULE_IDX));
			module.addExtraInformation(EXTRA_PARAM_MODULE_DESCRIPTION, TSServerInfoConstants.VALUE_PARAM_LIST_RO_CROSSREF_BP);
			module.addExtraInformation(EXTRA_PARAM_SIMULATE_CROSSREF, Boolean.TRUE);
			module.clearSaKeys();
			module.setSaObjKey(RO_CROSS_REF_INSTR_LIST);
			module.setIteratorType(TYPE_BOOK_PAGE_LIST_NOT_AGAIN);
			module.setIteratorType(IDX_TXTBOOK, ITERATOR_TYPE_BOOK_FAKE);		
			module.setIteratorType(IDX_TXTPAGE, ITERATOR_TYPE_PAGE_FAKE);
			module.addFilterForNextType(TYPE_BOOK_PAGE_FOR_NEXT_GENERIC);
			module.addValidators(crossRefValidators);
			module.addCrossRefValidators(crossRefValidators);
			modules.add(module);	
			
			// Search for cross reference instruments from RO
			module = new TSServerInfoModule(serverInfo.getModule(INSTR_MODULE_IDX));
			module.addExtraInformation(EXTRA_PARAM_MODULE_DESCRIPTION, TSServerInfoConstants.VALUE_PARAM_LIST_RO_CROSSREF_BP);
			module.addExtraInformation(EXTRA_PARAM_SIMULATE_CROSSREF, Boolean.TRUE);
			module.setSaObjKey(RO_CROSS_REF_INSTR_LIST);
			module.setIteratorType(TYPE_INSTRUMENT_LIST_NOT_AGAIN);
			module.setIteratorType(IDX_TXTINSTRUMENTNUMBER, ITERATOR_TYPE_INSTRUMENT_LIST_FAKE);		
			module.addFilterForNextType(TYPE_INSTRUMENT_FOR_NEXT_GENERIC);
			module.addValidators(crossRefValidators);
			module.addCrossRefValidators(crossRefValidators);
			modules.add(module);
	
			// OCR last transfer - book / page search
			module = new TSServerInfoModule(serverInfo.getModule(BOOK_PAGE_MODULE_IDX));
			module.clearSaKeys();
			module.setIteratorType(TYPE_OCR_FULL_OR_BOOTSTRAPER);
			module.setIteratorType(IDX_TXTBOOK, ITERATOR_TYPE_BOOK_SEARCH);
			module.setIteratorType(IDX_TXTPAGE, ITERATOR_TYPE_PAGE_SEARCH);
			module.addFilterForNextType(TYPE_BOOK_PAGE_FOR_NEXT_GENERIC);
			module.addValidators(crossRefValidators);
			module.addCrossRefValidators(crossRefValidators);
			modules.add(module);
			
			// OCR last transfer - instrument search
			module = new TSServerInfoModule(serverInfo.getModule(INSTR_MODULE_IDX));
			module.clearSaKeys();
			module.setIteratorType(TYPE_OCR_FULL_OR_BOOTSTRAPER);
			module.setIteratorType(IDX_TXTINSTRUMENTNUMBER, ITERATOR_TYPE_INSTRUMENT_LIST_SEARCH);
			module.addFilterForNextType(TYPE_INSTRUMENT_FOR_NEXT_GENERIC);
			module.addValidators(crossRefValidators);
			module.addCrossRefValidators(crossRefValidators);
			modules.add(module);
			
			// Owner search
			module = new TSServerInfoModule(serverInfo.getModule(NAME_MODULE_IDX));
			module.clearSaKeys();
			module.setSaObjKey(SearchAttributes.OWNER_OBJECT);
			
			module.setSaKey(IDX_TXTBEGINDATE, SearchAttributes.FROMDATE_MM_SLASH_DD_SLASH_YYYY);
			module.setIteratorType(IDX_TXTBEGINDATE, FunctionStatesIterator.ITERATOR_TYPE_FROM_DATE);
			module.setSaKey(IDX_TXTENDDATE, SearchAttributes.TODATE_MM_SLASH_DD_SLASH_YYYY);
			module.setIteratorType(IDX_TXTNAME, ITERATOR_TYPE_LAST_NAME_FAKE);		
			
			FilterResponse ocrNameFilter = getDefaultNameFilter( SearchAttributes.OWNER_OBJECT, searchId, module );
			ocrNameFilter.setInitAgain(true);
			module.addFilters(ocrNameFilter, legalFilter, alreadyPresentFilter);		
			addBetweenDateTest(module, true, true, false);
			module.addCrossRefValidators(crossRefValidators);
			
			ArrayList<NameI> searchedNames = null;
			if (nameIterator != null) {
				searchedNames = nameIterator.getSearchedNames();
			} else {
				searchedNames = new ArrayList<NameI>();
			}
			nameIterator = (ConfigurableNameIterator) ModuleStatesIteratorFactory
				.getConfigurableNameIterator(module, searchId, true, new String[] { "L F;;", "L M;;" });
			nameIterator.setInitAgain(true);
			nameIterator.setSearchedNames(searchedNames);
			module.addIterator(nameIterator);
			
			modules.add(module);
		
		}
		// set list for automatic search 
		serverInfo.setModulesForAutoSearch(modules);
	}
	
	
	protected void setModulesForGoBackOneLevelSearch(TSServerInfo serverInfo) {
		
	   	ConfigurableNameIterator nameIterator = null;
		DocsValidator defaultLegalValidator = LegalFilterFactory.getDefaultLegalFilter(searchId).getValidator();
		DocsValidator pinValidator = PINFilterFactory.getPinFilter(searchId,true,true).getValidator(); 
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
			 module.setIteratorType(IDX_TXTNAME, ITERATOR_TYPE_LAST_NAME_FAKE);		
		     module.addFilter(NameFilterFactory.getDefaultNameFilter(SearchAttributes.GB_MANAGER_OBJECT, searchId, module));
		 	 module.addFilter( AddressFilterFactory.getAddressHighPassFilter(searchId, 0.8d) );
		 	 module.addFilter(NameFilterFactory.getDefaultTransferNameFilter(searchId, 0.90d, module));
		 	 String date=gbm.getDateForSearch(id,"MM/dd/yyyy", searchId);
		     if (date!=null) 
		    	 module.getFunction(IDX_TXTBEGINDATE).forceValue(date);
		     nameIterator =  (ConfigurableNameIterator)ModuleStatesIteratorFactory.getConfigurableNameIterator(module, searchId, new String[]{"L F;;", "L M;;"} );
		 	 module.addIterator(nameIterator);
		 	 module.addValidator( defaultLegalValidator );
	         module.addValidator( pinValidator );
		  	 module.addValidator(DateFilterFactory.getDateFilterForGoBack(SearchAttributes.GB_MANAGER_OBJECT, searchId, module).getValidator());
		
			 modules.add(module);
		    
		     
		     if(gbm.getNamesForBrokenChain(id, searchId).size()>0){
			     module = new TSServerInfoModule(serverInfo.getModule(TSServerInfo.NAME_MODULE_IDX));
			     module.setIndexInGB(id);
			     module.setTypeSearchGB("grantee");
			     module.clearSaKeys();
				 module.setSaObjKey(SearchAttributes.GB_MANAGER_OBJECT);
				 module.setIteratorType(IDX_TXTNAME, ITERATOR_TYPE_LAST_NAME_FAKE);		
				 module.addFilter(NameFilterFactory.getDefaultNameFilter(SearchAttributes.GB_MANAGER_OBJECT, searchId, module));
				 module.addFilter( AddressFilterFactory.getAddressHighPassFilter(searchId, 0.8d) );
				 module.addFilter(NameFilterFactory.getDefaultTransferNameFilter(searchId, 0.90d, module));
				 date=gbm.getDateForSearchBrokenChain(id,"MM/dd/yyyy", searchId);
				 if (date!=null) 
					 module.getFunction(IDX_TXTBEGINDATE).forceValue(date);
				 nameIterator =  (ConfigurableNameIterator)ModuleStatesIteratorFactory.getConfigurableNameIterator(module, searchId, new String[]{"L F;;", "L M;;"} );
				 module.addIterator(nameIterator);
				 module.addValidator( defaultLegalValidator );
				 module.addValidator( pinValidator );
				 module.addValidator(DateFilterFactory.getDateFilterForGoBack(SearchAttributes.GB_MANAGER_OBJECT, searchId, module).getValidator());
			
	
			 modules.add(module);
			 
		     }

	    }	 
	    serverInfo.setModulesForGoBackOneLevelSearch(modules);	
		
	}
		
	@Override
    public void ParseResponse(String sAction, ServerResponse Response, int viParseID) throws ServerResponseException {
    	
		String response = Response.getResult();
		String linkStart = CreatePartialLink(TSConnectionURL.idGET);
		int istart, iend;
		
    	switch(viParseID){
    	
    	case ID_NAME:
    	case ID_BOOK_PAGE:
    	case ID_LEGAL:
    	case ID_INSTR:
    		// check for no results
    		if(response.contains("Search Returned 0 results")){
    			return;
    		}    		
    		if(response.contains("document.getElementById(\"trExceedMessage\").style.display = \"\"")
    		  && (response.contains("Maximum limit of") || response.contains("records has been exceeded"))){

				String warning = "Maximum limit of records has been exceeded! Please refine the search criteria!"; 
				Response.getParsedResponse().setError(warning);
				Response.setWarning(warning);
				return;					
			}	
    		
    		// isolate the table
    		istart = response.indexOf("id=\"dgResults\""); 
    		if(istart == -1){ return; }
    		istart = response.lastIndexOf("<table", istart);
    		if(istart == -1){ return; }
    		iend = response.indexOf("</table>", istart);
    		if(iend == -1){ return; }
    		iend += "</table>".length();
    		String intermTable = response.substring(istart, iend );
    		
    		// remove the navigation rows
    		String navigationRow = "";
    		for(int i=0; i<2; i++){
	    		istart = intermTable.indexOf("<tr class=\"stdFontPager\"");
	    		if(istart != -1){
	    			iend = intermTable.indexOf("</tr>", istart);
	    			if(iend != -1){
	    				iend += "</tr>".length();
	    				navigationRow = intermTable.substring(istart, iend);
	    				intermTable = intermTable.substring(0,istart) + intermTable.substring(iend);
	    			}
	    		}
    		}
    		
    		// remove column sorting links
    		intermTable = intermTable.replaceAll("(?i)<a href=\"javascript:__doPostBack\\('SortDynamicColumn'[^>]*>([A-Z#0-9 ]+)</a>", "$1");
    		
    		// remove images
    		intermTable = intermTable.replaceAll("(?i)<img [^>]*>", "");
    		
    		// remove additional formatting
    		intermTable = intermTable.replaceAll("(?i)<tr[^>]+>", "<tr>");
    		intermTable = intermTable.replaceAll("(?i)</tr>", "</tr>");

    		// color the table header
    		intermTable = intermTable.replaceFirst("(?i)<tr[^>]*>", "<tr bgcolor=\"#cccccc\">");

    		// remove duplicated links
    		//intermTable = intermTable.replaceAll("(?i)<A class=stdFontResults href='showdetails\\.aspx[^>]*>(\\d{1,3})</A>","$1");
    		    		
    		// rewrite details links    		
    		intermTable = intermTable.replaceAll(
    				"(?i)<A class='?stdFontResults'? href='showdetails\\.aspx\\?([^']+)'>", 
    				"<a href='" + linkStart + "/oncore/details.aspx&" + "$1" + "'>");
    		
    		// navigation links
    		String [] navLinks = extractNavigationLinks(response, navigationRow);
        	
    		// add checkboxes for saving to TSD
    		intermTable = intermTable.replaceAll("(?is)(<TD[^>]+>)\\s*<a href='([^']+)'>(\\d+)</a>", "$1<input type=\"checkbox\" name=\"docLink\"  value=\"$2\"></TD>$1<a href='$2'>$3</a>");
    		intermTable = CreateSaveToTSDFormHeader(URLConnectionReader.MULTIPLE_SAVE_TO_TSD_ACTION, "POST") + intermTable + CreateSaveToTSDFormEnd(SAVE_SELECTED_DOCUMENTS_BUTTON_LABEL, viParseID, -1);
    		intermTable = intermTable.replaceFirst("(?i)<td","<td style=\"padding-left:3px;padding-right:3px;\">" + SELECT_ALL_CHECKBOXES + "</td><td");

    		// set the table header to be used for parsing
    		istart = intermTable.indexOf("<tr");
    		if(istart != -1){
    			iend = intermTable.indexOf("</tr>", istart);
    			if(iend != -1){
    				iend += "</tr>".length();
    				parser.setHeader(intermTable.substring(istart, iend));
    			}
    		}
    		
    		// displayable navinfo incl nav links and number of results
    		String navInfo = getNavInfo(navLinks, response);
    		intermTable += navInfo;
    		
    		// parse the intermediate results
    		parser.Parse(Response.getParsedResponse(), intermTable, Parser.PAGE_ROWS, getLinkPrefix(TSConnectionURL.idGET), TSServer.REQUEST_SAVE_TO_TSD); 
    		
    		if(navLinks != null && navLinks[1] != null){
    			Response.getParsedResponse().setNextLink(navLinks[1]);
    		}
    		
    		break;
    		
    	case ID_DETAILS:
    		
    		// extract details
    		String details = extractDetails(response);    		
    		if(StringUtils.isEmpty(details)){
    			Response.getParsedResponse().setError("Doc information was not retrieved correctly!");
				return;
    		}
    		
    		String docType = StringUtils.extractParameter(response, "<span id=\"lblDocumentType\">([^<]*)</span>");
    		docType = StringUtils.extractParameter(docType, "\\(([^\\)]+)\\)");

    		if(!DocumentTypes.checkDocumentType(docType, DocumentTypes.RELEASE_INT, null, searchId)){
	    		// rewrite reference links
	    		Map<String,String> params = extractParameters(response);
	    		getSearch().setAdditionalInfo("FLHillsboroughRO:cref-viewstate", params.remove("__VIEWSTATE"));
	    		params.remove("__EVENTARGUMENT");
	    		params.put("__EVENTTARGET", "ShowRelatedDoc");
	    		
	    		String action = StringUtils.extractParameter(response, "action=\"details\\.aspx\\?([^\"]+)");   
	    		String link = CreatePartialLink(TSConnectionURL.idGET) + "/oncore/details.aspx&" + action + "&postParams=true";
	    		for(Map.Entry<String, String> entry: params.entrySet()){
	    			link += "&" + StringUtils.urlEncode(entry.getKey()) + "=" + StringUtils.urlEncode(entry.getValue());
	    		}
	    		link += "&pageType=cref";
	    	
	    		// 2007369787 - 18047/1318
	    		details = details.replaceAll(
	    				"(?i)<a class=\"stdFontSmall\" href=\"javascript:__doPostBack\\('ShowRelatedDoc','(\\d+)'[^>]*>([^-]*)-\\s*(\\d+)/(\\d+)", 
	    				"<a HREF='" + link + "&__EVENTARGUMENT=$1&bp=$3_$4'>$2 - $3/$4");
    		} else {
    			
    			details = details.replaceAll(
	    				"(?i)<a class=\"stdFontSmall\" href=\"javascript:__doPostBack\\('ShowRelatedDoc','(\\d+)'[^>]*>([^-]*)-\\s*(\\d+)/(\\d+)[^<]*</a>", 
	    				"$2 - $3/$4");  
    		}
    		
    		// add the instrument number 
    		org.w3c.dom.Document finalDoc = Tidy.tidyParse(response);
			String instrument = HtmlParserTidy.getValueFromTagById(finalDoc, "lblCfn", "span").trim();
			instrument = instrument.replaceAll("(?is)</?span[^>]*>", "");//just in case
			
    		if(!StringUtils.isEmpty(instrument)){
    			details = details.replaceFirst("(?i)<tr", "<tr><td>Instrument #:</td><td>" + instrument + "</td></tr><tr");
    		}
    		
    		String bookPage = StringUtils.extractParameter(response, "<span id=\"lblBookPage\">([^<]*)</span>").replaceAll("\\s", "").replaceAll("/", "_");
    		int poz = bookPage.indexOf("_");
    		String book = bookPage.substring(0,poz);
    		String page = bookPage.substring(poz+1,bookPage.length());
    		String type = DocumentTypes.getDocumentCategory(docType, searchId);
    		String instr = bookPage + type;
    		
    		String imageLink = StringUtils.extractParameter(response, "parent.doc.location.href=\"([^\"]+)");
    		if(!StringUtils.isEmpty(imageLink)){
    			imageLink = linkStart + "/oncore/" + imageLink + "&atsInstr=" + instr + "&book="+book +"&page="+page+"&type="+type;
    			String sFileLink = instr + ".tiff";
    			ImageLinkInPage ilip = new ImageLinkInPage(imageLink, sFileLink);
    			Response.getParsedResponse().addImageLink(ilip);
    			details += "<div align=\"center\"><a href='" + imageLink + "'>View Image</a></div>";
    		}
    		
    		details = "<table align=\"center\" cellspacing=\"0\" border=\"1\"><tr><td>" + details + "</td></tr></table>";
    		
    		if(!downloadingForSave) {
    			String qry = Response.getRawQuerry();
                qry = "dummy=" + instr + "&" + qry;
                String originalLink = sAction + "&" + qry;
                String sSave2TSDLink = getLinkPrefix(TSConnectionURL.idGET) + originalLink;
                if (FileAlreadyExist(instr + ".html") ){
                	details += CreateFileAlreadyInTSD();
                }else {
                	details = addSaveToTsdButton(details, sSave2TSDLink,viParseID);
                    mSearch.addInMemoryDoc(sSave2TSDLink, response);
                }               
                LinkInPage lip = new LinkInPage(sSave2TSDLink, originalLink, TSServer.REQUEST_SAVE_TO_TSD);
                Response.getParsedResponse().setPageLink(lip);
                Response.getParsedResponse().setResponse(details);
    		} else {
    			msSaveToTSDFileName = instr + ".html";
    			//details = details.replaceAll("</?a[^>]*>","");
    			parser.Parse(Response.getParsedResponse(), details, Parser.PAGE_DETAILS,  getLinkPrefix(TSConnectionURL.idGET), TSServer.REQUEST_SAVE_TO_TSD);
    		}    		
    		
    		break;
    		
    	case ID_GET_IMAGE:
    		//    check for no results
    		if(response.contains("Search Returned 0 results")){
    			return;
    		}
    		
    		// isolate the table
    		istart = response.indexOf("id=\"dgResults\""); 
    		if(istart == -1){ return; }
    		istart = response.lastIndexOf("<table", istart);
    		if(istart == -1){ return; }
    		iend = response.indexOf("</table>", istart);
    		if(iend == -1){ return; }
    		iend += "</table>".length();
    		intermTable = response.substring(istart, iend );
    		
    		String id="";
    		Matcher mat = patFirstResult.matcher(intermTable );
    		if(mat.find()){
    			id = mat.group(1).replaceAll("&amp;", "&");
    		}

    		HttpSite site = HttpManager.getSite("FLHillsboroughRO", searchId);
    		String res = "";
    		try {
            	HTTPRequest request = new HTTPRequest("http://pubrec3.hillsclerk.com/oncore/details.aspx?" + id );
            	res = site.process(request).getResponseAsString();     			
    		} finally{
    			HttpManager.releaseSite(site);
    		}
    		
    		instrument = StringUtils.extractParameter(response, "<span id=\"lblCfn\">(\\d+)</span>");
        	docType = StringUtils.extractParameter(res, "<span id=\"lblDocumentType\">([^<]*)</span>");
    		docType = StringUtils.extractParameter(docType, "\\(([^\\)]+)\\)");
    		bookPage = StringUtils.extractParameter(res, "<span id=\"lblBookPage\">([^<]*)</span>").replaceAll("\\s", "").replaceAll("/", "_");
    		instr = bookPage + docType;
        	 
    		int poz1 = id.indexOf("&");
    		id = id.substring(0,poz1);
    		//GET the image
    		GetLink("Link=/oncore/ImageBrowser/default.aspx?"+id+"&dtk="+docType+"&atsInstr="+instr,isParentSite());
    		Response.getParsedResponse().setAttribute("ROIMAGEDOCTYPE", docType);
    	break;
    	
    	case ID_GET_LINK:
    		if(detailsPattern.matcher(Response.getQuerry()).find()){
				ParseResponse(sAction, Response, ID_DETAILS);
    		} else {
    			ParseResponse(sAction, Response, ID_NAME);
    		}
    		break;
        case ID_SAVE_TO_TSD :
            downloadingForSave = true;
            ParseResponse(sAction, Response, ID_DETAILS);
            downloadingForSave = false;
            break;    		
    	}
    	
    }

    @Override
    protected String getFileNameFromLink(String link) {
    	String retVal = StringUtils.extractParameter(link,"&dummy=([^&]+)");
    	if(StringUtils.isEmpty(retVal)){
    		retVal = StringUtils.extractParameter(link,"&bp=([^&]+)");
    	}
    	if(StringUtils.isEmpty(retVal)){
    		retVal = StringUtils.extractParameter(link, "&__EVENTARGUMENT=([^&]+)");
    	}
    	if(StringUtils.isEmpty(retVal)){
    		retVal = link;
    	}
    	return retVal + ".html";    	
    }
    
    
    public ServerResponse GetImageLink(String link,String bpdt,boolean writeImageToClient) throws ServerResponseException{
    	link = link.replace("&atsInstr=" + bpdt, "");
    	
    	
		String folderName = getCrtSearchDir() + "Register" + File.separator;
		new File(folderName).mkdirs();
    	String fileName = folderName + bpdt + ".pdf";
		
    	String fileNameTiff = fileName.replaceAll("[.]pdf", ".tiff");
    	boolean existTiff = FileUtils.existPath(fileNameTiff);
    	boolean existPDF = FileUtils.existPath(fileName);
    	
		if(!existTiff && !existPDF){
			// retrieve the image
			for(int i=0; i<2; i++){
	    		if(retrieveImage(link, fileName)){   		
	    			break;
	    		}
	    	}
		}
		
		existTiff = FileUtils.existPath(fileNameTiff);
		existPDF = FileUtils.existPath(fileName);
			
    	// write the image to the client web-browser
		boolean imageOK = false;
		if(existTiff){
			imageOK = writeImageToClient(fileNameTiff, "image/tiff");
		} else {
			imageOK = writeImageToClient(fileName, "application/pdf");			
		}
		
		// image not retrieved
		if(!imageOK){ 
	        // return error message
			ParsedResponse pr = new ParsedResponse();
			pr.setError("<br><font color=\"red\"><b>Image not found!</b></font> ");
			throw new ServerResponseException(pr);			
		}
		// return solved response
		return ServerResponse.createSolvedResponse();  
    }
    
    /**
     * treat the case in which the user clicked on an image link, and download it only once  
     */
    @Override
    public ServerResponse GetLink(String vsRequest, boolean vbEncodedOrIsParentSite) throws ServerResponseException {
    	    	
    	String link = StringUtils.extractParameter(vsRequest, "Link=(.*)");
    	String bpdt = StringUtils.extractParameter(vsRequest, "atsInstr=([^&?]*)");
    	if(StringUtils.isEmpty(link) || StringUtils.isEmpty(bpdt)){
    		return super.GetLink(vsRequest, vbEncodedOrIsParentSite); 
    	}
    	
    	return GetImageLink 	(link,bpdt,vbEncodedOrIsParentSite);
    	
    }
    
	/**
	 * Called by the parser through reflection
	 */
	public static void splitResultRows(Parser p, ParsedResponse pr, String htmlString, int pageId, String linkStart, int action)
    throws ro.cst.tsearch.exceptions.ServerResponseException {
        p.splitResultRows(pr, htmlString, pageId, "<tr", "</tr>", linkStart, action);
        
        // remove table header
        Vector rows = pr.getResultRows();        
        if (rows.size()>0){ 
            ParsedResponse firstRow = (ParsedResponse)rows.remove(0); 
            pr.setResultRows(rows);
            pr.setHeader(pr.getHeader() + firstRow.getResponse()); 
        }
    }
	
    /**
     * get the image using the add to cart, checkout, etc
     */
    @Override
    protected DownloadImageResult saveImage(ImageLinkInPage image) throws ServerResponseException{
    	
    	Matcher imageRequestMatcher = imageRequestPattern.matcher(image.getLink());
    	if(!imageRequestMatcher.find()){
    		return new DownloadImageResult(DownloadImageResult.Status.ERROR, new byte[0], image.getContentType());
    	}
    	String link = image.getLink().replaceFirst("Link=", "");
    	String fileName =image.getPath();
    	// retrieve the image
    	for(int i=0; i<2; i++){
    		if(retrieveImage(link, fileName)){
    			byte []b = FileUtils.readBinaryFile(fileName);
    			return new DownloadImageResult(DownloadImageResult.Status.OK, b, image.getContentType());
    		}
    	}
    	return new DownloadImageResult(DownloadImageResult.Status.ERROR, new byte[0], image.getContentType());
    }
       
    /**
     * Retrieve an image
     * @param link
     * @param fileName location on disk where file needs to be saved
     * @return true if retrieving succeeded
     */
    private boolean retrieveImage(String link, String fileName){
    	
    	String book = link.replaceAll(".*book=([^&?]*).*", "$1");
    	String page = link.replaceAll(".*page=([^&?]*).*", "$1");
    	String type = link.replaceAll(".*type=([^&?]*).*", "$1");
    	
    	return GenericDASLRV.retrieveImage(book, page, "", type, fileName, mSearch, msSiteRealPath, false);
    }
    
    /**
     * Create HTML controls that will be reused by each search module
     * @param sim
     * @return
     */
    private HTMLControl [] createHtmlControls(TSServerInfoModule sim){
    	
        // build start date and end date formatted strings 
        String startDate="", endDate="";
        try{
        	SimpleDateFormat sdf = new SimpleDateFormat("MMM d, yyyy");
            SearchAttributes sa = InstanceManager.getManager().getCurrentInstance(searchId).getCrtSearchContext().getSa();
            Date start = sdf.parse(sa.getAtribute(SearchAttributes.FROMDATE));	            
            Date end = sdf.parse(sa.getAtribute(SearchAttributes.TODATE));	            
            sdf.applyPattern("MM/dd/yyyy");	            
            startDate = sdf.format(start);
            endDate = sdf.format(end);	            
        }catch (ParseException e){}
        
    	try{
    		HTMLControl
	    	p01 = new HTMLControl(HTML_TEXT_FIELD,  1, 1,  1,  1, 1, sim.getFunction(0),  "__EVENTTARGET","__EVENTTARGET","",searchId),
	    	p02 = new HTMLControl(HTML_TEXT_FIELD,  1, 1,  1,  1, 1, sim.getFunction(1),  "__EVENTARGUMENT","__EVENTARGUMENT","",searchId),
	    	p03 = new HTMLControl(HTML_TEXT_FIELD,  1, 1,  1,  1, 1, sim.getFunction(2),  "__VIEWSTATE","__VIEWSTATE","",searchId),
	    	p04 = new HTMLControl(HTML_TEXT_FIELD,  1, 1,  1,  1, 1, sim.getFunction(3),  "SearchType","SearchType","",searchId),
	    	p05 = new HTMLControl(HTML_TEXT_FIELD,  1, 1,  1,  1, 1, sim.getFunction(4),  "SearchTypeDesc","SearchTypeDesc","",searchId),
	    	p06 = new HTMLControl(HTML_SELECT_BOX,  1, 1,  1,  1, 1, sim.getFunction(5),  "ddPartyType","Party Type","-1",searchId),
	    	p07 = new HTMLControl(HTML_TEXT_FIELD,  1, 1,  1,  1,30, sim.getFunction(6),  "txtName","Name","",searchId),
	    	p08 = new HTMLControl(HTML_SELECT_BOX,  1, 1,  1,  1, 1, sim.getFunction(7),  "ddBookType","Book Type","O",searchId),
	    	p09 = new HTMLControl(HTML_TEXT_FIELD,  1, 1,  1,  1,10, sim.getFunction(8),  "txtBook","Book","",searchId),
	    	p10 = new HTMLControl(HTML_TEXT_FIELD,  1, 1,  1,  1,10, sim.getFunction(9),  "txtPage","Page","",searchId),
	    	p11 = new HTMLControl(HTML_TEXT_FIELD,  1, 1,  1,  1,26, sim.getFunction(10), "txtInstrumentNumber","Instrument","",searchId),
	    	p12 = new HTMLControl(HTML_TEXT_FIELD,  1, 1,  1,  1, 1, sim.getFunction(11), "txtLowerBound","txtLowerBound","",searchId),
	    	p13 = new HTMLControl(HTML_TEXT_FIELD,  1, 1,  1,  1, 1, sim.getFunction(12), "txtUpperBound","txtUpperBound","",searchId),
	    	p14 = new HTMLControl(HTML_SELECT_BOX,  1, 1,  1,  1, 1, sim.getFunction(13), "ddParcelChoice","ddParcelChoice","0",searchId),
	    	p15 = new HTMLControl(HTML_TEXT_FIELD,  1, 1,  1,  1, 1, sim.getFunction(14), "txtParcelId","txtParcelId","",searchId),
	    	p16 = new HTMLControl(HTML_SELECT_BOX,  1, 1,  1,  1, 1, sim.getFunction(15), "ddCommentsChoice","Selection","0",searchId),
	    	p17 = new HTMLControl(HTML_TEXT_FIELD,  1, 1,  1,  1,30, sim.getFunction(16), "txtComments","Comments","",searchId),
	    	p18 = new HTMLControl(HTML_TEXT_FIELD,  1, 1,  1,  1, 1, sim.getFunction(17), "txtLegalFields","txtLegalFields","",searchId),
	    	p19 = new HTMLControl(HTML_TEXT_FIELD,  1, 1,  1,  1, 1, sim.getFunction(18), "txtLegalDesc","txtLegalDesc","",searchId),
	    	p20 = new HTMLControl(HTML_TEXT_FIELD,  1, 1,  1,  1, 1, sim.getFunction(19), "Textbox2","Textbox2","",searchId),
	    	p21 = new HTMLControl(HTML_SELECT_BOX,  1, 1,  1,  1, 1, sim.getFunction(20), "ddCaseNumberChoice","ddCaseNumberChoice","0",searchId),
	    	p22 = new HTMLControl(HTML_TEXT_FIELD,  1, 1,  1,  1, 1, sim.getFunction(21), "txtCaseNumber","txtCaseNumber","",searchId),
	    	p23 = new HTMLControl(HTML_TEXT_FIELD,  1, 1,  1,  1,30, sim.getFunction(22), "txtDocTypes","Document Type","",searchId),
	    	p24 = new HTMLControl(HTML_SELECT_BOX,  1, 1,  1,  1, 1, sim.getFunction(23), "cboCategories","or Category","n/a",searchId),
	    	p25 = new HTMLControl(HTML_TEXT_FIELD,  1, 1,  1,  1, 1, sim.getFunction(24), "txtRecordDate","txtRecordDate",endDate,searchId),
	    	p26 = new HTMLControl(HTML_TEXT_FIELD,  1, 1,  1,  1,10, sim.getFunction(25), "txtBeginDate","Begin Date",startDate,searchId),
	    	p27 = new HTMLControl(HTML_TEXT_FIELD,  1, 1,  1,  1,10, sim.getFunction(26), "txtEndDate","End Date",endDate,searchId),
	    	p28 = new HTMLControl(HTML_TEXT_FIELD,  1, 1,  1,  1, 1, sim.getFunction(27), "cmdSubmit","cmdSubmit","Search Records",searchId);
    		
    		HTMLControl [] controls = new HTMLControl[] { p01, p02, p03, p04, p05, p06, p07, p08,
					p09, p10, p11, p12, p13, p14, p15, p16, p17, p18, p19, p20,
					p21, p22, p23, p24, p25, p26, p27, p28 };
    		
    		setupSelectBox(sim.getFunction(IDX_DDPARTYTYPE), PARTY_TYPE_SELECT);
    		setupSelectBox(sim.getFunction(IDX_CBOCATEGORIES), CATEGORY_SELECT);
    		setupSelectBox(sim.getFunction(IDX_DDBOOKTYPE), BOOK_TYPE_SELECT);
    		setupSelectBox(sim.getFunction(IDX_DDPARCELCHOICE), PARCEL_TYPE_SELECT);
    		setupSelectBox(sim.getFunction(IDX_DDCOMMENTSCHOICE), COMMENTS_TYPE_SELECT);
    		setupSelectBox(sim.getFunction(IDX_DDCASENUMBERCHOICE), CASE_TYPE_SELECT);
    		// set all hidden
    		setHiddenParamMulti(true, controls);
    		
    		return controls;
    		
    	}catch(FormatException e){
    		throw new RuntimeException(e);
    	}
    }

	/**
	 * Extract all parameter names from the search page
	 * @param response
	 * @return
	 */
	private Map<String,String> extractParameters(String response){
		
		Map<String,String> params = new HashMap<String,String>();
		
		for(Pattern pattern: paramNamePattern){
			Matcher matcher = pattern.matcher(response);
			while(matcher.find()){
				params.put(matcher.group(1), "");
			}
		}
		
		for(Pattern pattern: paramPairPatterns){
			Matcher matcher = pattern.matcher(response);
			while(matcher.find()){
				params.put(matcher.group(1), matcher.group(2));
			}
		}
		
		int istart = response.indexOf("<select");
		int iend = response.indexOf("</select>", istart);
		while(istart != -1 && iend != -1 && istart < iend){
			String crtSelect = response.substring(istart, iend);
			String name = StringUtils.extractParameter(crtSelect, "name=\"([^\"]+)\"");
			String value = StringUtils.extractParameter(crtSelect, "<option selected=\"selected\" value=\"([^\"]+)\">");
			if(!StringUtils.isEmpty(name) && !StringUtils.isEmpty(value)){
				params.put(name, value);
			}
			istart = iend;
			istart = response.indexOf("<select", iend);
			iend = response.indexOf("</select>", istart);
		}
		return params;
	}

	/**
	 * Determine next link
	 * @param response
	 * @param navigationRow
	 * @return
	 */
	private String [] extractNavigationLinks(String response, String navigationRow){

		String crtPageStr = StringUtils.extractParameter(navigationRow, "<span>(\\d+)</span>");
		if("".equals(crtPageStr)){ 
			return null; 
		}
		int crtPage = Integer.parseInt(crtPageStr);
		String nextParams = StringUtils.extractParameter(navigationRow, "<a href=\"javascript:__doPostBack\\('dgResults(\\$[_a-z0-9]+\\$[_a-z0-9]+)',''\\)\">" + (crtPage+1) + "</a>");
		String prevParams = "";
		if(crtPage != 1){
			prevParams = StringUtils.extractParameter(navigationRow, "<a href=\"javascript:__doPostBack\\('dgResults(\\$[_a-z0-9]+\\$[_a-z0-9]+)',''\\)\">" + (crtPage-1) + "</a>");
		}
		if("".equals(nextParams) && "".equals(prevParams)){ 
			return null; 
		}
		
		Map<String,String> parameters = extractParameters(response);
		String action = StringUtils.extractParameter(response, "action=\"([^\"]+)\"");		
		if(parameters.size() == 0 || "".equals(action)){
			return null; 
		}
		
		parameters.remove("__EVENTTARGET");
		getSearch().setAdditionalInfo("FLHillsboroughRO:next-viewstate", parameters.remove("__VIEWSTATE"));
		
		String link = CreatePartialLink(TSConnectionURL.idGET) + "/oncore/" + action;
		link += "&postParams=true";
		for(Map.Entry<String, String> entry: parameters.entrySet()){
			link += "&" + StringUtils.urlEncode(entry.getKey()) + "=" + StringUtils.urlEncode(entry.getValue());
		}
		link += "&pageType=next";
		
		String prevLink = null;
		String nextLink = null;
		if(!"".equals(prevParams)){
			prevParams = "&__EVENTTARGET=" + StringUtils.urlEncode("dgResults" + prevParams.replace("$",":"));
			prevLink = "<a href='" + link + prevParams +"'>Previous</a>";
		}
		if(!"".equals(nextParams)){
			nextParams = "&__EVENTTARGET=" + StringUtils.urlEncode("dgResults" + nextParams.replace("$",":"));
			nextLink = "<a href='" + link + nextParams +"'>Next</a>";
		}
		
		return new String[] {prevLink, nextLink};
	}
	
	/**
	 * Get navigation info to be shown below intermediate results table
	 * @param navLinks
	 * @param response
	 * @return
	 */
	private String getNavInfo(String [] navLinks, String response){
		String navInfo = "";
		if (navLinks != null) {
			if (navLinks[0] != null) {
				navInfo += navLinks[0];
			}
			if (navLinks[1] != null) {
				if (navLinks[0] != null) {
					navInfo += "&nbsp;&nbsp;";
				}
				navInfo += navLinks[1];
			}
		}
		if (navInfo.length() != 0) {
			navInfo += "&nbsp;&nbsp;&nbsp;&nbsp;";
		}
		Matcher crtResultsMatcher = crtResultsPattern.matcher(response);
		if (crtResultsMatcher.find()) {
			navInfo += "Showing results " + crtResultsMatcher.group(1);
			Matcher totResultsMatcher = totResultsPattern.matcher(response);
			if (totResultsMatcher.find()) {
				navInfo += " of " + totResultsMatcher.group(1) + "";
			}
		} 
		return navInfo;
	}
	
	/**
	 * Extract details from page
	 * @param response
	 * @return
	 */
	private String extractDetails(String response){
		
		int istart, iend;
		
		istart = response.indexOf("<table class=\"DetailBackground\">");
		if(istart == -1){ return null; }
		istart = response.indexOf("<table", istart + 1);
		if(istart == -1){ return null; }
		iend = response.indexOf("</table>", istart);
		if(iend == -1){ return null; }
		iend += "</table>".length();
		
		return response.substring(istart, iend);
	}

	@Override
    protected boolean isRecursiveAnaliseInProgress(String link){
		if(super.isRecursiveAnaliseInProgress(link)){
			return true;
		}
		String link2 = StringUtils.extractParameter(link, "(&__EVENTARGUMENT=[^&]+&bp=[^&]+)");
		if(!StringUtils.isEmpty(link2)){
			return super.isRecursiveAnaliseInProgress(link2);
		}
		return false;
    }
    
    @Override
    protected void addRecursiveAnalisedLink(String link){
    	 super.addRecursiveAnalisedLink(link);
    	 String link2 = StringUtils.extractParameter(link, "(&__EVENTARGUMENT=[^&]+&bp=[^&]+)");
    	 if(!StringUtils.isEmpty(link2)){
    		 super.addRecursiveAnalisedLink(link2);
    	 }
    }

    @Override
    protected boolean getLogAlreadyFollowed(){
    	return false;
    }
    
	@Override
    public ServerResponse performLinkInPage(LinkInPage link) throws ServerResponseException {
    	
		// do not explore link for doc that was already saved in TSR index
    	if(link != null && link.getLink() != null && link.getLink().contains("isSubResult=true")){    		    		
    		String bp = StringUtils.extractParameter(link.getLink(), "&bp=(\\d+_\\d+)");
    		if(!StringUtils.isEmpty(bp)){
    			if(getSearch().hasSavedInst(bp)){
    				if(getLogAlreadyFollowed()){
    					SearchLogger.info("<div>Document with " + bp + " already saved.</div><br/>", searchId);
    				}
    				return new ServerResponse();
    			}
    		}    		
    	}   
    	
    	return super.performLinkInPage(link);
    }

}