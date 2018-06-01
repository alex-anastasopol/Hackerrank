package ro.cst.tsearch.servers.types;

import java.io.BufferedInputStream;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Serializable;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Random;
import java.util.Set;
import java.util.Vector;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletRequestWrapper;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.io.IOUtils;
import org.apache.log4j.Logger;
import org.htmlparser.util.NodeList;

import ro.cst.tsearch.Logme;
import ro.cst.tsearch.MailConfig;
import ro.cst.tsearch.Search;
import ro.cst.tsearch.ServerConfig;
import ro.cst.tsearch.AutomaticTester.PageAndIndexOfLink;
import ro.cst.tsearch.bean.SearchAttributes;
import ro.cst.tsearch.bean.recoverdocument.ModuleShortDescription;
import ro.cst.tsearch.community.CommunityAttributes;
import ro.cst.tsearch.connection.BridgeConn;
import ro.cst.tsearch.connection.CookieManager;
import ro.cst.tsearch.connection.http.HTTPManager;
import ro.cst.tsearch.connection.http.HTTPManagerException;
import ro.cst.tsearch.connection.http.HTTPRequest;
import ro.cst.tsearch.connection.http.HTTPSiteInterface;
import ro.cst.tsearch.connection.http2.HttpManager;
import ro.cst.tsearch.connection.http2.HttpSite;
import ro.cst.tsearch.connection.http3.HttpManager3;
import ro.cst.tsearch.connection.http3.HttpSite3;
import ro.cst.tsearch.data.CountyConstants;
import ro.cst.tsearch.database.DBManager;
import ro.cst.tsearch.database.transactions.SaveRestoreDocumentTransaction;
import ro.cst.tsearch.emailClient.EmailClient;
import ro.cst.tsearch.exceptions.ServerResponseException;
import ro.cst.tsearch.extractor.xml.Bridge;
import ro.cst.tsearch.extractor.xml.GenericFunctions;
import ro.cst.tsearch.extractor.xml.ResultMap;
import ro.cst.tsearch.generic.Util;
import ro.cst.tsearch.generic.WordsToNumbers;
import ro.cst.tsearch.log.SearchLogFactory;
import ro.cst.tsearch.log.SearchLogPage;
import ro.cst.tsearch.parentsitedescribe.ServerInfoDSMMap;
import ro.cst.tsearch.processExecutor.client.ClientProcessExecutor;
import ro.cst.tsearch.propertyInformation.Instrument;
import ro.cst.tsearch.replication.tsr.FileInfo;
import ro.cst.tsearch.search.FunctionStatesIterator;
import ro.cst.tsearch.search.ModuleStatesIterator;
import ro.cst.tsearch.search.ServerSearchesIterator;
import ro.cst.tsearch.search.filter.BetweenDatesFilterResponse;
import ro.cst.tsearch.search.filter.FilterResponse;
import ro.cst.tsearch.search.filter.RejectAlreadySavedDocumentsForUpdateFilter;
import ro.cst.tsearch.search.filter.newfilters.misc.NoIndexingInfoFilter;
import ro.cst.tsearch.search.filter.newfilters.name.ExactNameFilter;
import ro.cst.tsearch.search.filter.parser.name.NameParser;
import ro.cst.tsearch.search.name.NameCleaner;
import ro.cst.tsearch.search.name.NameUtils;
import ro.cst.tsearch.search.tokenlist.NameTokenList;
import ro.cst.tsearch.search.util.AdditionalInfoKeys;
import ro.cst.tsearch.search.validator.DocsValidator;
import ro.cst.tsearch.searchsites.client.GWTDataSite;
import ro.cst.tsearch.servers.HashCountyToIndex;
import ro.cst.tsearch.servers.SearchDataWrapper;
import ro.cst.tsearch.servers.ServletServerComm;
import ro.cst.tsearch.servers.TSConnectionURL;
import ro.cst.tsearch.servers.bean.DataSite;
import ro.cst.tsearch.servers.functions.ILDuPageRO;
import ro.cst.tsearch.servers.info.TSServerInfo;
import ro.cst.tsearch.servers.info.TSServerInfoConstants;
import ro.cst.tsearch.servers.info.TSServerInfoFunction;
import ro.cst.tsearch.servers.info.TSServerInfoModule;
import ro.cst.tsearch.servers.info.TSServerInfoParam;
import ro.cst.tsearch.servers.parentsite.County;
import ro.cst.tsearch.servers.response.CrossRefCleaner;
import ro.cst.tsearch.servers.response.ImageLinkInPage;
import ro.cst.tsearch.servers.response.ImageTransformation;
import ro.cst.tsearch.servers.response.LinkInPage;
import ro.cst.tsearch.servers.response.OCRParsedDataStruct;
import ro.cst.tsearch.servers.response.OCRParsedDataStruct.StructBookPage;
import ro.cst.tsearch.servers.response.OtherInformationSet.OtherInformationSetKey;
import ro.cst.tsearch.servers.response.ParsedResponse;
import ro.cst.tsearch.servers.response.Parser;
import ro.cst.tsearch.servers.response.RawResponseWrapper;
import ro.cst.tsearch.servers.response.SaleDataSet;
import ro.cst.tsearch.servers.response.ServerResponse;
import ro.cst.tsearch.servlet.BaseServlet;
import ro.cst.tsearch.servlet.URLConnectionReader;
import ro.cst.tsearch.threads.ASMaster;
import ro.cst.tsearch.threads.ASThread;
import ro.cst.tsearch.threads.GPMaster;
import ro.cst.tsearch.threads.OCRDownloader;
import ro.cst.tsearch.titledocument.abstracts.DocumentTypes;
import ro.cst.tsearch.tsr.PrefixFilenameFilter;
import ro.cst.tsearch.user.MyAtsAttributes;
import ro.cst.tsearch.user.UserAttributes;
import ro.cst.tsearch.user.UserUtils;
import ro.cst.tsearch.utils.CrossRefInitSingleton;
import ro.cst.tsearch.utils.CurrentInstance;
import ro.cst.tsearch.utils.FileUtils;
import ro.cst.tsearch.utils.GBManager;
import ro.cst.tsearch.utils.ImageResizer;
import ro.cst.tsearch.utils.ImageResizer.ConversionStatus;
import ro.cst.tsearch.utils.ImageResizer.ResizeResult;
import ro.cst.tsearch.utils.IndividualLogger;
import ro.cst.tsearch.utils.InstanceManager;
import ro.cst.tsearch.utils.Log;
import ro.cst.tsearch.utils.RequestParams;
import ro.cst.tsearch.utils.RequestParamsValues;
import ro.cst.tsearch.utils.SearchLogger;
import ro.cst.tsearch.utils.StringUtils;
import ro.cst.tsearch.utils.Tidy;
import ro.cst.tsearch.utils.URLMaping;

import com.stewart.ats.base.document.AssessorDocument;
import com.stewart.ats.base.document.DocumentI;
import com.stewart.ats.base.document.DocumentI.Fields;
import com.stewart.ats.base.document.DocumentI.SearchType;
import com.stewart.ats.base.document.Image;
import com.stewart.ats.base.document.ImageI;
import com.stewart.ats.base.document.ImageI.IType;
import com.stewart.ats.base.document.InstrumentI;
import com.stewart.ats.base.document.Mortgage;
import com.stewart.ats.base.document.MortgageI;
import com.stewart.ats.base.document.RegisterDocument;
import com.stewart.ats.base.document.RegisterDocumentI;
import com.stewart.ats.base.document.RestoreDocumentData;
import com.stewart.ats.base.document.RestoreDocumentDataI;
import com.stewart.ats.base.document.SSFPriorFileDocument;
import com.stewart.ats.base.document.TaxDocument;
import com.stewart.ats.base.document.Transfer;
import com.stewart.ats.base.document.TransferI;
import com.stewart.ats.base.name.Name;
import com.stewart.ats.base.name.NameFlagsI;
import com.stewart.ats.base.name.NameFormaterI;
import com.stewart.ats.base.name.NameI;
import com.stewart.ats.base.name.NameMortgageGrantee;
import com.stewart.ats.base.name.NameMortgageGranteeI;
import com.stewart.ats.base.name.NameSourceType;
import com.stewart.ats.base.property.PropertyI;
import com.stewart.ats.base.search.DocumentsManager;
import com.stewart.ats.base.search.DocumentsManagerI;
import com.stewart.ats.base.search.SearchAttributesI;
import com.stewart.ats.tsrindex.client.SimpleChapterUtils;
import com.stewart.ats.tsrindex.client.SimpleChapterUtils.DType;
import com.stewart.ats.tsrindex.client.SimpleChapterUtils.PType;
import com.stewart.ats.tsrindex.client.ocrelements.LegalDescription;
import com.stewart.ats.tsrindex.client.ocrelements.LegalDescriptionI;
import com.stewart.ats.tsrindex.client.ocrelements.VestingInfo;
import com.stewart.ats.tsrindex.server.DocumentDataRetreiver;
import com.stewart.ats.tsrindex.server.TsdIndexPageServer;
import com.stewart.ats.webservice.OCRService;
import com.stewart.ats.webservice.ocr.xsd.Output;
import com.stewart.dip.DocumentImageParser;
import com.stewart.dip.bean.CallDipResult;
import com.stewartworkplace.starters.ssf.services.connection.DocAdminConn;
import com.stewartworkplace.starters.ssf.services.docadmin.DocumentInfoResultType;
import com.stewartworkplace.starters.ssf.services.docadmin.DocumentInfoType;
import com.stewartworkplace.starters.ssf.services.docadmin.TxtStarterAndIndexType;

/**
 * @author costin
 */
public  class TSServer implements TSInterface, Serializable, Cloneable {

	public static enum ADD_DOCUMENT_RESULT_TYPES {
		UNDEFINED,
		ERROR,
		ADDED,
		DONE_BUT_WITH_ERRORS,
		ALREADY_EXISTS,
		OVERWRITTEN,
		MERGED
	}
	
	public static final String SELECT_ALL_CHECKBOXES = 
		"<input type=\"checkbox\" title=\"Check\\Uncheck All\" onClick=\"var elems=document.getElementsByName('docLink'); for(var i=0; i<elems.length;i++) {elems[i].checked = this.checked;}\"/>";
	public static final String	DOCLINK_CHECKBOX_START	= "<input type='checkbox' name='docLink' autocomplete='off' value='";
	
	public static String SAVE_DOCUMENT_BUTTON_LABEL = "Save document";
	public static String SAVE_SELECTED_DOCUMENTS_BUTTON_LABEL = "Save selected document(s)";
	public static String SAVE_DOC_WITH_CROSS_REF_BUTTON_LABEL = "Save selected document(s) with cross-references";
	public static String TOO_MANY_PROP = "Too many properties. Check Doc Index.";
	
	public static final String NO_DATA_FOUND = "No Data Found!";
	public static final String INVALIDATED_RESULT = "INVALIDATED_RESULT";
	
	public static final String PARENT_SITE_LOOKUP_MODE = "PARENT_SITE_LOOKUP_MODE";
	
	private boolean goBackOneLevel = false;

	public static final int[] ASSESSOR_LIKE_SITES = GWTDataSite.ASSESSOR_LIKE_SITES;
	public static final int[] TAX_LIKE_TYPE = GWTDataSite.TAX_LIKE_SITES;
	public static final int[] CITYTAX_LIKE_SITES = GWTDataSite.CITYTAX_LIKE_SITES;
	
	public static final int[] RO_LIKE_TYPE = GWTDataSite.RO_LIKE_TYPE;
	public static final int[] GB_LIKE_TYPE = {
		GWTDataSite.RO_TYPE,
		GWTDataSite.RV_TYPE,
		GWTDataSite.DT_TYPE,
		GWTDataSite.LA_TYPE,
		GWTDataSite.CO_TYPE,
		GWTDataSite.PA_TYPE,
		GWTDataSite.PC_TYPE,
		GWTDataSite.PR_TYPE,
		GWTDataSite.AC_TYPE,
		GWTDataSite.DG_TYPE
	};
	//vector used to retain the index and the page on wich the link was cliked 
	public Vector v = new Vector();
	
    static final long serialVersionUID = 10000001;

    protected String msRequestSolverName = ""; //name and path (ifneeded) of
                                               // the class which made the call
                                               // and which will solve any link
                                               // from jsp()

    protected String msServerID = "";

    //the names and values which identify this server for the creator, this val
    // is passed by creator and is used internal only when the server create a
    // html link to itself
    protected String msPrmNameLink = ""; //the parameter name wich identify
                                         // that is a call from a link which
                                         // server itself built and it know how
                                         // to solve it

    protected String msSitePath = ""; //site path

    protected int miGetLinkActionType = TSConnectionURL.idGET;

    protected final String ACTION_TYPE_LINK = "ActionType";

    public static final String SAVE_TO_TSD_PARAM_NAME = "SaveToTSD";

    protected Hashtable attributes = new Hashtable();
    
    /**
     * Used for overridding the result of getServerTypeDirectory()
     * for cases in which a server can return multiple types, like AO and RO
     */
    protected String serverTypeDirectoryOverride = null;

    protected static final int ID_GET_LINK = -1; //used in ParseResponse to
                                                 // identify what parse
                                                 // algorithm to apply

    protected static final int ID_LOGIN = 0; //used in ParseResponse to
                                             // identify what parse algorithm to
                                             // apply

    //protected static final int ID_DETAILS_FROM_AUTOMATIC = 102;
    
    protected static final int ID_SEARCH_BY_NAME = 1; //used in ParseResponse
                                                      // to identify what parse
                                                      // algorithm to apply

    protected static  final int ID_DETAILS =101;
    protected static  final int ID_DETAILS1 = 33101;
    protected static  final int ID_DETAILS2 = 33102;
    
    protected static final int ID_INTERMEDIARY = 33103; 
    
    protected static final int ID_SEARCH_BY_ADDRESS = 2; //used in
                                                         // ParseResponse to
                                                         // identify what parse
                                                         // algorithm to apply

    protected static final int ID_SEARCH_BY_PARCEL = 3; //used in ParseResponse
                                                        // to identify what
                                                        // parse algorithm to
                                                        // apply

    protected static final int ID_SEARCH_BY_TAX_BIL_NO = 4; //used in
                                                            // ParseResponse to
                                                            // identify what
                                                            // parse algorithm
                                                            // to apply

    protected static final int ID_SEARCH_BY_INSTRUMENT_NO = 5; //used in
                                                               // ParseResponse
                                                               // to identify
                                                               // what parse
                                                               // algorithm to
                                                               // apply

    protected static final int ID_SEARCH_BY_SUBDIVISION_NAME = 6; //used in
                                                                  // ParseResponse
                                                                  // to identify
                                                                  // what parse
                                                                  // algorithm
                                                                  // to apply

    protected static final int ID_SAVE_TO_TSD = 7; //used in ParseResponse to
                                                   // identify what parse
                                                   // algorithm to apply

    protected static final int ID_SEARCH_BY_BOOK_AND_PAGE = 8; //used in
                                                               // ParseResponse
                                                               // to identify
                                                               // what parse
                                                               // algorithm to
                                                               // apply

    protected static final int ID_SEARCH_BY_SUBDIVISION_PLAT = 9; //used in
                                                                  // ParseResponse
                                                                  // to identify
                                                                  // what parse
                                                                  // algorithm
                                                                  // to apply
    protected static final int ID_BROWSE_SCANNED_INDEX_PAGES = 10; //used in
																    // ParseResponse
																    // to identify
																    // what parse
																    // algorithm
																    // to apply
    protected static final int ID_BROWSE_BACKSCANNED_PLATS = 11; //used in
																    // ParseResponse
																    // to identify
																    // what parse
																    // algorithm
																    // to apply
    protected static final int ID_SEARCH_BY_PROP_NO = 12; //used in
													    // ParseResponse to
													    // identify what
													    // parse algorithm
													    // to apply
    
    public  static final int ID_GET_IMAGE = 13;	//used in
												    // ParseResponse to
												    // identify what
												    // parse algorithm
												    // to apply
    protected static final int ID_SEARCH_BY_CONDO_NAME = 14;	//used in
															    // ParseResponse to
															    // identify what
															    // parse algorithm
															    // to apply
    protected static final int ID_SEARCH_BY_SECTION_LAND = 15;	//used in
															    // ParseResponse to
															    // identify what
															    // parse algorithm
															    // to apply
    protected static final int ID_SEARCH_BY_SURVEYS = 16;	//used in
														    // ParseResponse to
														    // identify what
														    // parse algorithm
														    // to apply
    
    protected static final int ID_SEARCH_BY_SERIAL_ID = 17; // 
  
    protected static final int ID_SEARCH_BY_SALES = 18; //
    protected static final int ID_SEARCH_BY_MODULE19 = 19; //
    protected static final int ID_SEARCH_BY_MODULE20 = 20; //
    protected static final int ID_SEARCH_BY_MODULE21 = 21;
    protected static final int ID_SEARCH_BY_MODULE22 = 22; //
    protected static final int ID_SEARCH_BY_MODULE23 = 23; //
    protected static final int ID_SEARCH_BY_MODULE24 = 24; //
    protected static final int ID_SEARCH_BY_MODULE25 = 25; //
    protected static final int ID_SEARCH_BY_MODULE26 = 26; //
    protected static final int ID_SEARCH_BY_MODULE27 = 27; //
    protected static final int ID_SEARCH_BY_MODULE28 = 28; //
    protected static final int ID_SEARCH_BY_MODULE29 = 29; //
    protected static final int ID_SEARCH_BY_MODULE30 = 30; //
    protected static final int ID_SEARCH_BY_MODULE31 = 31; //
    protected static final int ID_SEARCH_BY_MODULE32 = 32; //
    protected static final int ID_SEARCH_BY_MODULE33 = 33; //
    protected static final int ID_SEARCH_BY_MODULE34 = 34; //
    protected static final int ID_SEARCH_BY_MODULE35 = 35; //
    protected static final int ID_SEARCH_BY_MODULE36 = 36; //
    protected static final int ID_SEARCH_BY_MODULE37 = 37; //
    protected static final int ID_SEARCH_BY_MODULE38 = 38; //
    protected static final int ID_SEARCH_BY_MODULE39 = 39; //
    protected static final int ID_SEARCH_BY_MODULE40 = 40; //
    protected static final int ID_SEARCH_BY_MODULE41 = 41; //
    protected static final int ID_SEARCH_BY_MODULE42 = 42; //
    protected static final int ID_SEARCH_BY_MODULE43 = 43; //
    protected static final int ID_SEARCH_BY_MODULE44 = 44; //
    protected static final int ID_SEARCH_BY_MODULE45 = 45; //
    protected static final int ID_SEARCH_BY_MODULE46 = 46; //
    protected static final int ID_SEARCH_BY_MODULE47 = 47; //
    protected static final int ID_SEARCH_BY_MODULE48 = 48; //
    protected static final int ID_SEARCH_BY_MODULE49 = 49; //
    protected static final int ID_SEARCH_BY_MODULE50 = 50; //
    protected static final int ID_SEARCH_BY_MODULE51 = 51; //
    protected static final int ID_SEARCH_BY_MODULE52 = 52; //
    protected static final int ID_SEARCH_BY_MODULE53 = 53; //
  
    protected static final int ID_BROWSE_BACKSCANNED_DEEDS = 19;
    
    public static final String NUMBER_OF_UNSAVED_DOCUMENTS = "NUMBER_OF_UNSAVED_DOCUMENTS";
    
    public static final int REQUEST_SAVE_TO_TSD = 0;

    public static final int REQUEST_GO_TO_LINK = 1;

    public static final int REQUEST_SEARCH_BY = 2;

    public static final int REQUEST_GO_TO_LINK_REC = 3;

    public static final int REQUEST_SEARCH_BY_REC = 4;
    
    public static final int REQUEST_CONTINUE_TO_NEXT_SERVER = 5;
    
    public static final int REQUEST_SEARCH_BY_AND_SAVE = 6;

    public static final int UNIQUE_RESULT_TYPE = 1;

    public static final int MULTIPLE_RESULT_TYPE = 2;
    
    public static final String CHECK_DOC_TYPE = "CHECK_DOC_TYPE";  
    
    protected int resultType;

    public final static String DASLFINAL ="DASLFINAL";
    
    public static final String AND_REPLACER = "MyPersonalCSTLinkAnd";

    /**
     * Kept only because I am not sure what will happen if an old search with TSServer serialized is opened
     */
    @Deprecated
    private transient String ERROR_CLASS_NAME = "[TSServer] "; 
 
    protected String msSaveToTSDFileName = "";

    protected String msLastLink = "";

    private TSConnectionURL mTSConnection = new TSConnectionURL();
    
    static Random random = new Random();

    protected transient String msSaveToTSDResponce;

    protected static final Logger logger = Logger.getLogger(TSServer.class);

    protected Search mSearch;

    protected long  searchId = -1;
    
    protected int miServerID;

    protected TSServerInfo msiServerInfo = null;

    protected Parser parser = null;

    protected String msSiteRealPath = ""; //site path

    protected DocsValidator docsValidator = null;
    protected DocsValidator crossRefDocsValidator = null;

    protected int docsValidatorType = DocsValidator.TYPE_NO_VALIDATION;
    protected int crossRefDocsValidatorType = DocsValidator.TYPE_ALWAYS_TRUE;
    
    protected int iteratorType = ModuleStatesIterator.TYPE_DEFAULT;

    protected long startTime = 0;

    protected long stopTime = 0;
    
    protected Vector<DocsValidator> docsValidators = null;
    protected Vector<DocsValidator> crossRefDocsValidators = null;
    
    public static final Pattern docLinkPattern = Pattern.compile( "<input type=\"checkbox\" name=\"docLink\" value=\"(.*?)\">" );
    
    public static Pattern serverIdPattern = Pattern.compile(URLConnectionReader.PRM_NAME_P1 + "=(.*?)&"
                + URLConnectionReader.PRM_NAME_P2 + "=(.*?)");
    
    protected boolean asynchronous = false;
    
    protected boolean parentSite = false;
    
    protected boolean rangeNotExpanded = true;
    
    protected boolean ocrAdded = false;
    
    protected boolean inNextLinkSequence = false;
    
    protected boolean doNotLogSearch = false;
    
    protected boolean repeatDataSource = false;
    
    protected DataSite dataSite = null;
    
    protected int numberOfYearsAllowed = 0;

    public TSServer(long searchId) {
    	this.searchId = searchId;
    }

    public TSServer(String rsRequestSolverName, String rsSitePath, String rsServerID, String rsPrmNameLink,long searchId, int miServerID) {
    	this.miServerID = miServerID;    	
    	this.searchId = searchId;
        msSitePath = rsSitePath;
        msRequestSolverName = rsRequestSolverName;
        msServerID = rsServerID;
        
        msPrmNameLink = rsPrmNameLink;
        
        dataSite = HashCountyToIndex.getDateSiteForMIServerID(getCommunityId(), miServerID);
        
        numberOfYearsAllowed = dataSite.getNumberOfYears();
        resultType = UNIQUE_RESULT_TYPE;
        
        msiServerInfo = getDefaultServerInfoWrapper();
        getTSConnection().setHostIP(msiServerInfo.getServerIP());
        getTSConnection().setHostName(msiServerInfo.getServerAddress());

        docsValidators = new Vector<DocsValidator>();
        crossRefDocsValidators = new Vector<DocsValidator>();

        logger.info("Creating TSServer..." + this.getClass().getName() + " for searchId " + searchId + " serverId " + miServerID);
//        logger.info("\n\n Class instantiated with parameters:" +
//        									  "\n\t - rsRequestSolverName:   - " + 
//        				rsRequestSolverName + "\n\t - rsSitePath:            - " +
//						rsSitePath 			+ "\n\t - rsServerID:            - " + 
//						rsServerID 			+ "\n\t - rsPrmNameLink:         - " + rsPrmNameLink + "\n\n");
    }

    private boolean conditionParam=false;
    protected  void setModulesForGoBackOneLevelSearch(TSServerInfo serverInfo) {
  
    }
    
   public void  setConditonParam(boolean conditionParam){
    	this.conditionParam=conditionParam;
    }
   
    protected transient TSServerInfo defaultServerInfo = null;
  
    public TSServerInfo getDefaultServerInfoWrapper(){
    	if(defaultServerInfo != null){
    		return defaultServerInfo;
    	} else {
	    	defaultServerInfo = getDefaultServerInfo();
	    	return defaultServerInfo;
    	}
    }
    
    public TSServerInfo getDefaultServerInfo() {
    	
    	if(defaultServerInfo != null){
    		return defaultServerInfo;
    	}
    	
    	logger.debug (this.getClass().getName());
    	ServerInfoDSMMap DSM= new ServerInfoDSMMap();
    	TSServerInfo msiServerInfoDefault= null;
    	DSM.setElseParam(this.conditionParam);
    	msiServerInfoDefault=DSM.getServerInfo(this.getClass().getName().substring(this.getClass().getName().lastIndexOf(".")+1)+".xml", searchId);    	
    	msiServerInfoDefault.setupParameterAliases();    	
    		
	/*	if(goBackOneLevel){
			setModulesForGoBackOneLevelSearch(msiServerInfoDefault);
		}else
			*/
		setModulesForAutoSearch(msiServerInfoDefault);
		setModulesForGoBackOneLevelSearch(msiServerInfoDefault);
		defaultServerInfo = msiServerInfoDefault;
		return msiServerInfoDefault;
        
    }

    public void clearDocsValidators(){
    	docsValidators = new Vector<DocsValidator>();
    }
    public void clearCrossRefDocsValidators(){
    	crossRefDocsValidators = new Vector<DocsValidator>();
    }
    
    public TSServerInfo getCurrentClassServerInfo() {
        return msiServerInfo;
    }

    /**
     * Must be implemented for a valid automatic search cycle
     * @param serverInfo
     */
    protected void setModulesForAutoSearch(TSServerInfo serverInfo) {
        List<TSServerInfoModule> l = new ArrayList<TSServerInfoModule>();
        serverInfo.setModulesForAutoSearch(l);
    }

    public ServerResponse performLinkInPage(LinkInPage link)
            throws ServerResponseException {
        if (link == null)
            return new ServerResponse();
        return performLinkInPage(link, link.getActionType());
    }

    public ServerResponse performLinkInPage(LinkInPage link, int newServerAction)
            throws ServerResponseException {
        SearchAttributes sa = mSearch.getSa();
        ServerResponse sr = performAction(newServerAction, link.getLink(), link.getModule(), new SearchDataWrapper(sa));
        return sr;
    }

    public ServerResponse performAction(int viServerAction,
            String requestParams, TSServerInfoModule module,
            Object sd) throws ServerResponseException {
    	return performAction(viServerAction, requestParams, module, sd, null);
    }
    
    
    public ServerResponse performAction(int viServerAction,
            String requestParams, TSServerInfoModule module,
			Object sd, Map<String, Object> extraParams) throws ServerResponseException {

		ServerResponse Result = LogIn(); //

		// ==========================================
		boolean isTSAdmin = false;
		try {
			CurrentInstance curInst = InstanceManager.getManager().getCurrentInstance(mSearch.getID());
			UserAttributes ua = curInst.getCurrentUser();
			isTSAdmin = UserUtils.isTSAdmin(ua);
		} catch (Exception e) {
		}
		boolean alreadyLogedRows = false;
		try {

		if (isTSAdmin == true && mSearch.getSearchType() == Search.PARENT_SITE_SEARCH) {// for presence test

			String className = this.getClass().getName();
			if (className.lastIndexOf(".") >= 0) {
				className = className.substring(className.lastIndexOf(".") + 1);
			}

			String p1 = null, p2 = null;
			p1 = msServerID.substring(msServerID.indexOf("=") + 1, msServerID.indexOf("&"));
			p2 = msServerID.substring(msServerID.lastIndexOf("=") + 1);

			// Saves the data for the instatiation of a TSServerInfo
			mSearch.getSearchRecord().setParameterP1(p1);
			mSearch.getSearchRecord().setParameterP2(p2);
			mSearch.getSearchRecord().setServerName(className);
			mSearch.getSearchRecord().setMsSiteRealPath(msSiteRealPath);
		}

		if (viServerAction == TSServer.REQUEST_SAVE_TO_TSD) {
			Result = SaveToTSD(requestParams, extraParams);
		} else if (viServerAction == TSServer.REQUEST_SEARCH_BY) { //
			
			// let's log last module TASK 7802
			mSearch.getSearchRecord().setModule(module);
						
			// =========================================================
			if (isTSAdmin == true) {// for presence test
				mSearch.getSearchRecord().getPageAndIndex().clear();
			}
			// =========================================================

			Result = SearchBy(module, sd); //

		} else if (viServerAction == TSServer.REQUEST_SEARCH_BY_REC) {
			if (getSearch().getSearchType() != Search.GO_BACK_ONE_LEVEL_SEARCH) {
				Result = SearchBy(module, sd);
				//this is made to logRows when searching with crossreferences before saving the document
				if (Result != null
						&& Result.getParsedResponse() != null
						&& Result.getParsedResponse().getResultRows() != null
						&& !Result.getParsedResponse().getResultRows().isEmpty()
						&& Boolean.TRUE.equals(module.getExtraInformation(TSServerInfoConstants.EXTRA_PARAM_SIMULATE_CROSSREF))){
					
					ArrayList<FilterResponse> filterList = module.getFilterList();
					
					if(filterList != null && !filterList.isEmpty()) {
						for (FilterResponse moduleFilter : module.getFilterList()) {
							moduleFilter.filterResponse(Result);
						}
					} else {
						FilterResponse.logRows(Result.getParsedResponse().getResultRows(), searchId);
					}
					alreadyLogedRows = true;
				}
				recursiveAnalyzeResponse(Result);
			}
		} else if (viServerAction == TSServer.REQUEST_GO_TO_LINK) {

			if (isTSAdmin == true) {// for presence test
				// ============================================================
				// Retain the Link
				int pozitie = requestParams.indexOf("Link="); //
				String LINK = requestParams.substring(pozitie + 5); //

				// String LINK = requestParams;

				// Obtain the HTML page in order to enter the page as a string parameter in the getIndex() method
				// String s = mSearch.getHtmlString();

				// gets the string
				String s = "";
				try {
					s = mSearch.getSearchRecord().getPageAndIndexOfLinkLastElement().getPage();

					if (s == null) {
						s = "";
					}
				} catch (Exception e) {
					logger.error("Page empty");
				}

				// Retain the index of the Link
				int ind = LinkProcessing.getIndex(s, LINK); //

				// sets the index of the page
				mSearch.getSearchRecord().setPageAndIndexIndex(ind); //

				// ==============================================================
			}

			Result = GetLink(requestParams, true); //

		} else if (viServerAction == TSServer.REQUEST_GO_TO_LINK_REC) {
			Result = GetLink(requestParams, true);
			recursiveAnalyzeResponse(Result);
		}

		if (mSearch.getSearchType() == Search.PARENT_SITE_SEARCH
				&& viServerAction != TSServer.REQUEST_SAVE_TO_TSD
				&& Result != null
				&& Result.getParsedResponse() != null
				&& Result.getParsedResponse().getResultRows() != null
				&& !Result.getParsedResponse().getResultRows().isEmpty()
				&& !alreadyLogedRows) {
			try {
				Result.getParsedResponse().setSearchId(searchId);
				logRows(Result.getParsedResponse(), searchId);
			} catch (Exception e) {
				logger.error("Error while logging rows for searchId: " + searchId, e);
			}
		}
		
		} finally {
			// save order count for report Task 7825
			try {
				countOrder();
			} catch (Exception e) {
				logger.error("Could not count order for search id " + searchId, e);
			}
			try {
				mSearch.addDataSource(dataSite);
			} catch (Exception e) {
				logger.error("Exception when adding datasources on search: " + searchId, e);
			}
		}

		return Result;
	}

    public void logRows(ParsedResponse parsedResponse, long searchId){
    	
    	Object isPSLookupMode = parsedResponse.getAttribute(PARENT_SITE_LOOKUP_MODE);
		if (isPSLookupMode != null && isPSLookupMode instanceof Boolean && (Boolean) isPSLookupMode){
			StringBuilder sb = new StringBuilder();
			String header = parsedResponse.getHeader();
			header = header.replaceAll("(?is)<input[^>]*>", "").replaceAll("(?is)</?form[^>]*>", "");
	    	sb.append("<br/><br/><div>").append(header);
	    	
	        for (int i = 0; i < parsedResponse.getResultsCount(); i++){
	        	
	        	String doc = ((ParsedResponse) parsedResponse.getResultRows().get(i)).getResponse();    	
	        	doc = doc.replaceAll("\\s{2,}", " ").replaceAll("(?is)</?tr[^>]*>", "").replaceAll("(?is)<td[^>]*>\\s*<input[^>]*>\\s*</td>", "")
	        			.replaceAll("(?is)<input[^>]*>\\s*", "");
	        	String id = String.valueOf(System.nanoTime());
	        	sb.append("<tr class='row' id='").append(id).append("'>")
	        		.append("<td>").append((i + 1)).append("</td>")
	        		.append(doc)
	        		.append("</tr>");     	
	        	
	        }
	        sb.append("</table></div><br/><br/>");
	        SearchLogger.info(sb.toString(), searchId);
		} else {
			FilterResponse.logRows(parsedResponse.getResultRows(), searchId);
		}
    	
    	
    }
    
    @Override
    public void countOrder() {
		getSearch().countOrder(getSearch().getAbstractorFileNo(), getDataSite().getCityCheckedInt());
	}

	/**
     * @see TSInterface#LogIn()
     */
    public ServerResponse LogIn() throws ServerResponseException {
        return new ServerResponse();
    }

    /**
     * @see TSInterface#SessionExpired()
     */
    public boolean SessionExpired() {
        return false;
    }

    /**
     * @see TSInterface#NewSession()
     */
    public void NewSession() {
    }

    public void SetAttribute(String name, Object value) {
        attributes.put(name, value);
    }

    public Object GetAttribute(String name) {
        return attributes.get(name);
    }

    //protected 
    protected double parseMortgageAmount(String mortgageAmountFreeForm){
    	if (StringUtils.isEmpty(mortgageAmountFreeForm))
    		return SimpleChapterUtils.UNDEFINED_DOUBLE_VALUE;
    	
    	mortgageAmountFreeForm = WordsToNumbers.transformAmountFromWords(mortgageAmountFreeForm);
    		
    	mortgageAmountFreeForm = mortgageAmountFreeForm.replaceAll("(?is)([^\\$]*)(.+)", "$2");
    	mortgageAmountFreeForm = mortgageAmountFreeForm.replaceAll("\\)", "");
    	mortgageAmountFreeForm = mortgageAmountFreeForm.replaceAll("(,\\d{3})(\\d{2})", "$1.$2");//US $227,80000
    	mortgageAmountFreeForm = mortgageAmountFreeForm.replaceAll("(\\$\\d{3})(,\\d{3}),(\\d{2})", "$1$2.$3");//U.S. $562,500,00
    	try{
    		
    		return Double.parseDouble(mortgageAmountFreeForm.replaceAll("[$,]", ""));
    	}
    	catch(Exception e){}
    	return SimpleChapterUtils.UNDEFINED_DOUBLE_VALUE;
    }
    
    public double parseMortgageAmount(Vector<String> mortgageAmountFreeForm){
    	double max = SimpleChapterUtils.UNDEFINED_DOUBLE_VALUE;
		for( String m:mortgageAmountFreeForm ){
			max = Math.max(max, parseMortgageAmount(m));
		}
    	return max;
    }
    
    protected void ParseResponse(String sAction, ServerResponse Response, int viParseID)throws ServerResponseException{
    	throw new RuntimeException(" Please  implement the ParseResponse( ... ) function for " + getDataSite().getName() );
    }

    /*
     * returns an already Followed link (e.g. on RO like sites) 
     * that is pretty formatted and ready to be written in the Log File
     */
    public String getPrettyFollowedLink (String initialFollowedLnk)
    {
    	return "<br/><span class='followed'>Link already followed</span>:" + initialFollowedLnk + "<br/>";
    }
    
    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    /**
     * @see TSInterface#SearchByName(java.lang.String, java.lang.String)
     */
    public ServerResponse SearchBy(TSServerInfoModule module,
            Object sd) throws ServerResponseException {
        return SearchBy(true, module, sd);
    }

    /**
     * log the search parameters used by module 
     * @param module
     */
    protected void logSearchBy(TSServerInfoModule module){
    	logSearchBy(module, null);
    }
    
    /**
     * log the search parameters used by module
     * @param module
     * @param params
     */
    protected void logSearchBy(TSServerInfoModule module, Map<String, String> params){
    	
    	if(module.isVisible() || "GB_MANAGER_OBJECT".equals(module.getSaObjKey())) {//B 4511
        
	    	// get parameters formatted properly
	        Map<String,String> moduleParams = params;
	        if(moduleParams == null){
	        	moduleParams = module.getParamsForLog();
	        }
	        Search search = getSearch();
	        // determine whether it's an automatic search
	        boolean automatic = (search.getSearchType() != Search.PARENT_SITE_SEARCH) 
	        		|| (GPMaster.getThread(searchId) != null);
	        boolean imageSearch = module.getLabel().equalsIgnoreCase("image search") || 
	                              module.getModuleIdx() == TSServerInfo.IMG_MODULE_IDX;
	        
	        // create the message
	        StringBuilder sb = new StringBuilder();
	        SearchLogFactory sharedInstance = SearchLogFactory.getSharedInstance();
	        SearchLogPage searchLogPage = sharedInstance.getSearchLogPage(searchId);
	        sb.append("</div>");
	        
	        Object additional = GetAttribute("additional");
			if(Boolean.TRUE != additional){
	        	searchLogPage.addHR();
	        	sb.append("<hr/>");	
	        }
			int fromRemoveForDB = sb.length();
	        
			//searchLogPage.
	        sb.append("<span class='serverName'>");
	        String serverName = HashCountyToIndex.getDateSiteForMIServerID(getCommunityId(), miServerID).getName();
			sb.append(serverName);
	        sb.append("</span> ");
	
	       	sb.append(automatic? "automatic":"manual");
	       	Object info = module.getExtraInformation(TSServerInfoConstants.EXTRA_PARAM_MODULE_DESCRIPTION);
	       	if(StringUtils.isNotEmpty(module.getLabel())) {
		        
		        if(info!=null){
		        	sb.append(" - " + info + "<br>");
		        }
		        sb.append(" <span class='searchName'>");
		        sb.append(module.getLabel());
	       	} else {
	       		sb.append(" <span class='searchName'>");
		        if(info!=null){
		        	sb.append(" - " + info + "<br>");
		        }
	       	}
	        sb.append("</span> by ");
	        
	        boolean firstTime = true;
	        for(Entry<String,String> entry : moduleParams.entrySet() ){
	        	String value = entry.getValue();
	        	value = value.replaceAll("(, )+$",""); 
	        	if(!firstTime){
	        		sb.append(", ");
	        	} else {
	        		firstTime = false;
	        	}
	        	sb.append(entry.getKey().replaceAll("&lt;br&gt;", "") + " = <b>" + value + "</b>");
	        } 
	        int toRemoveForDB = sb.length();
	        //log time when manual is starting        
	        if (!automatic || imageSearch){
	        	sb.append(" ");
	        	sb.append(SearchLogger.getTimeStamp(searchId));
	        }
	        sb.append(":<br/>");
	        
	        // log the message
	        SearchLogger.info(sb.toString(),searchId);   
	        ModuleShortDescription moduleShortDescription = new ModuleShortDescription();
	        moduleShortDescription.setDescription(sb.substring(fromRemoveForDB, toRemoveForDB));
	        moduleShortDescription.setSearchModuleId(module.getModuleIdx());
	        search.setAdditionalInfo(TSServerInfoConstants.TS_SERVER_INFO_MODULE_DESCRIPTION, moduleShortDescription);
	        String user=InstanceManager.getManager().getCurrentInstance(searchId).getCurrentUser().getAttribute(1).toString();
	        SearchLogger.info(StringUtils.createCollapsibleHeader(),searchId);
	        searchLogPage.addModuleSearchParameters(serverName,additional, info, moduleParams,module.getLabel(), automatic, imageSearch,user);
    	}  
        
    }
    
    @Override
	public void logStartServer(String extraMessage) {
    	logStartOrFinishServer("starting", extraMessage); 
	}

	@Override
	public void logFinishServer(String extraMessage) {
		logStartOrFinishServer("finishing", extraMessage); 
	}
	
	protected void logStartOrFinishServer(String type, String extraMessage) {
		String id = String.valueOf(System.nanoTime()) + "_extraInfo";
		StringBuilder sb = new StringBuilder("</div>");
    	sb.append("<div id='").append(id).append("' style=\"display:none\">")
    		.append("<span class='serverName'>")
    		.append(getDataSite().getName())
    		.append("</span> ").append(type).append(" automatic search ")
    		.append(SearchLogger.getTimeStamp(searchId))
    		.append("<br>");
    	if(extraMessage != null) {
    		sb.append(extraMessage);
    	}
    	sb.append("</div>");
        SearchLogger.info(sb.toString(), searchId);
	}

	/**
     * log the search parameters used by additional modules
     * @param parameters
     */
    public void logAdditionalSearchBy(Map<String,String> parameters, String serverName, String moduleName){
        // create the message
        StringBuilder sb = new StringBuilder();
        boolean automatic = getSearch().getSearchType() != Search.PARENT_SITE_SEARCH;
        sb.append("<hr>\nAdditional \"" + moduleName + "\" search on " + serverName + " " + (automatic? "(automatic)":"(manual)") + " :<br>");
        for(Entry<String,String> entry : parameters.entrySet() ){
        	String key = entry.getKey();
        	String value = entry.getValue();
        	if(!"".equals(key) && value != null && !"".equals(value)){
        		sb.append("\n&nbsp;&nbsp;" + entry.getKey() + " = " + entry.getValue() + "<br>");
        	}
        }
        // log the message
        SearchLogger.info(sb.toString(),searchId);
    }
    
    
    protected ServerResponse SearchBy(boolean bResetQuery,
            TSServerInfoModule module, Object sd)
            throws ServerResponseException {

    	setCertificationDate();
    	
    	// log the search in the SearchLogger
    	logSearchBy(module);
    	
    	
    	prepareModuleForSearch(module);
    	
        getSearch().clearClickedDocuments();
        if(!Boolean.TRUE.equals(module.getExtraInformation(TSServerInfoConstants.EXTRA_PARAM_DO_NOT_REMOVE_IN_MEMORY_DOCS))) {
        	//this is needed because, sometimes, doing a search for image will 
        	//destroy the document viewed but not yet saved in parent site.
        	getSearch().removeAllInMemoryDocs();
        }
        if(Boolean.TRUE.equals(module.getExtraInformation(TSServerInfoConstants.EXTRA_PARAM_CLEAR_VISITED_AND_VALIDATED_LINKS))) {
        	getSearch().clearValidatedLinks();
        	getSearch().clearVisitedLinks();
        }
        
        String imagePath = null;
        if(sd instanceof SearchDataWrapper){
        	SearchDataWrapper searchDataWrapper = (SearchDataWrapper)sd;
        	prepareQuery(bResetQuery, module, searchDataWrapper);
        	if(searchDataWrapper != null && searchDataWrapper.getImage() != null) {
        		imagePath = searchDataWrapper.getImage().getPath();
        	}
        }
        
        String page = (module.getDestinationPage().length() != 0) ? module
                .getDestinationPage() : getTSConnection().getDestinationPage();
        Map<String, Object> extraParams = new HashMap<String, Object>();
        
        try{
        	extraParams.put(TSServerInfoConstants.EXTRA_PARAM_MODULE_OBJECT_SOURCE, module);
	        ServerResponse response = performRequest(page, module.getRequestMethod(), "SearchBy", module.getParserID(), imagePath, null, extraParams);
	        
	        logInitialResponse(response);
	        
        	return response;
        } catch (ServerResponseException sre){
        	SearchLogger.info("Error appeared during search!<br>",searchId);
        	throw sre;
        } catch (RuntimeException re){
        	SearchLogger.info("Error appeared during search!<br>",searchId);
        	throw re;
        }
    }

    /**
     * Method designed to perform last minute updates on the module just before doing the search and after the module is logged in search log<br>
     * Recommended for parameter cleaning so that nothing else needs to be done on connection side
     * @param module the module to be prepared
     */
	protected void prepareModuleForSearch(TSServerInfoModule module) {
	}

	protected void logInitialResponse(ServerResponse response) {
		DownloadImageResult imageResult = response.getImageResult();
		String htmlResponse = response.getParsedResponse().getResponse();
		int noResults = 0;
		if (response.getParsedResponse().getResultRows().size() == 0) {		//details page
			noResults = response.getParsedResponse().getResultsCount();
		} else {
			for (Object object : response.getParsedResponse().getResultRows()) {
				if (object instanceof ParsedResponse) {
					ParsedResponse parsedResponse = (ParsedResponse) object;
					Boolean possibleNavigationRow = (Boolean)parsedResponse.getAttribute(ParsedResponse.SERVER_NAVIGATION_LINK);
					if(!Boolean.TRUE.equals(possibleNavigationRow)) {
						noResults ++;
					} 
				} else {
					noResults ++;
				}
			}
		}

		Set<String> loggedErrors = new HashSet<String>(); 
		
		// check for errors & warnings inside the parsed response first
		if(response.getParsedResponse().isError()){
			
			if(!loggedErrors.contains(response.getParsedResponse().getError())) {
				loggedErrors.add(response.getParsedResponse().getError());
				if (response.getParsedResponse().getError().toLowerCase().contains("no results found")){
					SearchLogger.info("<br/><span class='error'>" + response.getParsedResponse().getError() + "</span>.</br>", searchId);
				} else {
					logInitialResponseSpecific(response);
					SearchLogger.info("<br/><span class='error'>Error appeared: " + response.getParsedResponse().getError() + "</span>.</br>", searchId);
				}
			}
		} else if(response.getParsedResponse().isWarning()){
			SearchLogger.info("<br/><span class='warning'>Warning appeared: " + response.getParsedResponse().getWarning() + ".</span></br>", searchId);
		}
		
		// process the warnings that need to be displayed in TSR index
		if(!StringUtils.isEmpty(response.getWarning())){
			SearchAttributes sa = getSearch().getSa();
			sa.setAtribute(SearchAttributes.SEARCH_WARNING, response.getWarning());
			// do not log the warning again
			if(!response.getParsedResponse().isWarning() && !response.getParsedResponse().isError()){
				SearchLogger.info("<span class='warning'>Warning appeared: " + response.getWarning() + "</span>.<br/>", searchId);
			}
		} 
		
		// log the number of results
		if(response.getError() != null){
			if(!loggedErrors.contains(response.getError())) {
				loggedErrors.add(response.getError());
				SearchLogger.info("<span class='error'>Error appeared:" + response.getError() + "</span>.<br/>",searchId);
			}
		} else if(htmlResponse == null || "".equals(htmlResponse)){
			if(isDoNotLogSearch()) {
				//do not log, it's simple
			} else if(imageResult != null && imageResult.getStatus() == DownloadImageResult.Status.OK) {
				SearchLogger.info("Found and downloaded image.<br/>",searchId);
			} else {
				SearchLogger.info("Found <span class='number'>0</span> <span class='rtype'>intermediate</span> results.<br/>",searchId);
			}
		} else {
			if(!isDoNotLogSearch()) {
				SearchLogger.info("Found <span class='number'>" + noResults + "</span> <span class='rtype'>intermediate</span> results.<br/>",searchId);
			}
		}
	}

	protected void logInitialResponseSpecific(ServerResponse response) {
	}

    protected void prepareQuery(boolean bResetQuery, TSServerInfoModule module,
            SearchDataWrapper sd) {
        if (bResetQuery)
            getTSConnection().BuildQuery(null, null, true);

        getTSConnection().buildQuery(
                this.getCurrentClassServerInfo().getFilterModuleParamsForQuery(
                        sd), false);

        getTSConnection().buildQuery(module.getParamsForQuery(), false);

        getTSConnection().SetReferer(module.getReferer());
        
        //super duper ghertzoiala
        //this is used when we are searching with bookpage or instruments which also represent crossref for RO docs
        //related to B988 and B1014
        //had to trick ATS to believe that this query is crossRef and also send the crossRefDocType
        //the docType is sent so ATS will only validate crossRef that came from certain documents
        //
        //so let's do it.
        
        Boolean simulateCrossRef = (Boolean)module.getExtraInformation(TSServerInfoConstants.EXTRA_PARAM_SIMULATE_CROSSREF);
        if(Boolean.TRUE.equals(simulateCrossRef)){
        	getTSConnection().BuildQuery("isSubResult", "true", false, false);
        	String sourceType = (String)module.getExtraInformation(TSServerInfoConstants.EXTRA_PARAM_CROSSREF_DOC_SOURCE);
        	if(!StringUtils.isEmpty(sourceType))
        		getTSConnection().BuildQuery("crossRefSource", sourceType, false, false);
        }
        
    }

    public boolean continueSeach() {
        
        ASThread thread = ASMaster.getSearch(mSearch);
        long maxTimeOut = 7200000;
        if (CountyConstants.IL_Du_Page_STRING.equals(mSearch.getSa().getAtribute(SearchAttributes.P_COUNTY)) 
        		|| CountyConstants.IL_Kane_STRING.equals(mSearch.getSa().getAtribute(SearchAttributes.P_COUNTY))){
        	maxTimeOut *= 2.5;
        }
        if (thread != null)
            return thread.isStarted() && (System.currentTimeMillis() - thread.getStartTime() < maxTimeOut); 
        else 
            return false;
    }
    
    public boolean skipCurrentSite() {
    
    	ASThread thread = ASMaster.getSearch(mSearch);
        
        if (thread != null)
        {
        	if (thread.getSkipCurrentSite())
        		logger.error("Server: " + this.getClass().toString() + " skip site!");
        	
            return thread.getSkipCurrentSite();
        }
        else 
            return false;
    }
    
    public boolean isStopAutomaticSearch() {
        
    	ASThread thread = ASMaster.getSearch(mSearch);
        
        if (thread != null)
        {
        	if (thread.getStopAutomaticSearch())
        		logger.error("Server: " + this.getClass().toString() + " stoping automatic!");
        	
            return thread.getStopAutomaticSearch();
        }
        else 
            return false;
    }
    
    public boolean continueSeachOnThisServer() 
    {
            
        ASThread thread = ASMaster.getSearch(mSearch);
        
        if (thread != null)
        {   if(serverTimeout==0){
        		serverTimeout = 900000;
        	}
            boolean contOK = (thread.isStarted() && (System.currentTimeMillis() - this.getStartTime() < serverTimeout)) || isParentSite(); // 15 min/server
            if (!contOK)
            {
                logger.error("Server: " + this.getClass().toString() + " search timeout!");
                
            }
            return contOK;
            
        }
        else 
            return false;
    }
    
    public int getNumberOfDocsAllowedForThisModule(ServerResponse result){
    	ASThread thread = ASMaster.getSearch(mSearch);
    	int maxDocAllowed = result.getParsedResponse().getResultRows().size();
    	if (thread != null){
    		if (ModuleStatesIterator.TYPE_OCR_FULL_OR_BOOTSTRAPER == thread.getCrtModule().getIteratorType()){
    			maxDocAllowed = Math.min(maxDocAllowed, 5);
    		}
    	} 
    	
    	return maxDocAllowed;
    }
    
    /**
     * Preprocess a link to be added/checked
     * in/against the list of already visited/analysed links
     * @param link
     * @return
     */
    protected String preProcessLink(String link){
    	String proc = link.toLowerCase();
    	proc = proc.replace("&issubresult=true", "");
    	proc = proc.replace("&isSubResult=true", "");
    	proc = proc.replace("&parentsite=true", "");
    	proc = proc.replace("&parentSite=true", "");
    	proc = proc.replaceFirst("&crossRefSource=[^&]+", "");
    	proc = proc.replaceFirst("&crossrefsource=[^&]+", "");
    	return proc;
    }
    
    /**
     * Check if a certain link is currently being analyzed
     * @param link
     * @return
     */
    protected boolean isRecursiveAnaliseInProgress(String link){
    	return mSearch.isRecursiveAnaliseInProgress(preProcessLink(link));
    }
    
    /**
     * Mark that a certain link is being analyzed
     * @param link
     */
    protected void addRecursiveAnalisedLink(String link){
    	 mSearch.addRecursiveAnalisedLink(preProcessLink(link));
    }
    
    /**
     * Mark that a link is visited
     * @param link
     */
    protected void addLinkVisited(String link){
    	mSearch.addLinkVisited(preProcessLink(link));
    }
    
    /**
     * Verify that a link is visited
     * @param link
     * @return
     */
    protected boolean isLinkVisited(String link){
    	return mSearch.isLinkVisited(preProcessLink(link));
    }
    
    /**
     * Verify that a link is validated
     * @param link
     * @return
     */
    protected boolean isLinkValidated(String link){
    	return mSearch.isLinkValidated(preProcessLink(link));
    }
    
    /**
     * Specify that a link was validated
     * @param link
     */
    protected void addValidatedLink(String link){
    	mSearch.addValidatedLink(preProcessLink(link));
    }
        
    protected boolean getLogAlreadyFollowed(){
    	return true;
    }
    
    public  ServerResponse recursiveAnalyzeResponse(ServerResponse result,
            int overrideActionType) throws ServerResponseException {
        
        Vector v = new Vector();
        
        int size = result.getParsedResponse().getResultRows().size();
        if ("ILKaneRO".equals(mSearch.getCrtServerName(false))){
        	size = getNumberOfDocsAllowedForThisModule(result);//B 4554
        }
                
        for (int i = 0; i < size && ((continueSeachOnThisServer() && continueSeach() && !skipCurrentSite() && !isStopAutomaticSearch()) || result.isParentSiteSearch()); i++) 
        {
            ParsedResponse pr = (ParsedResponse) result.getParsedResponse().getResultRows().get(i);
//            System.out.println(((InfSet)((Vector)pr.infVectorSets.get("SaleDataSet")).get(0)).getAtribute("InstrumentNumber"));
            LinkInPage linkObj = pr.getPageLink();
            String resultQuerry = result.getQuerry(); 
            boolean goBackCrossRef=false;
            
            //Task 9251
            if(linkObj == null && !isDocumentSavedNow(pr)) {
            	manageDocumentSavedBefore(pr);
            	continue;
            }
            
            if (getSearch().getSearchType()==Search.GO_BACK_ONE_LEVEL_SEARCH){//b4669
            	if( linkObj!=null && linkObj.getOriginalLink() != null) {
	            	goBackCrossRef = linkObj.getOriginalLink().contains("isSubResult=true");
	            	if (goBackCrossRef==Boolean.TRUE){
	            		continue;
	            	}
            	}
            }            
            if(linkObj!=null && resultQuerry.contains("isSubResult=true")){
            	String origLink = linkObj.getOriginalLink();
            	String link = linkObj.getLink();
            	if(origLink!=null && !origLink.contains("isSubResult"))
            		linkObj.setOnlyOriginalLink(origLink+"&isSubResult=true");
            	if(link!=null && !link.contains("isSubResult"))
            		linkObj.setOnlyLink(linkObj.getLink()+"&isSubResult=true");
            	
            	String crossRefSource = result.getCrossRefSourceType();
            	if(resultQuerry.contains("crossRefSource") && !StringUtils.isEmpty(crossRefSource)){
            		if(linkObj!=null && !linkObj.getOriginalLink().contains("crossRefSource"))
                		linkObj.setOnlyOriginalLink(linkObj.getOriginalLink()+"&crossRefSource=" + crossRefSource);
                	if(link!=null && !linkObj.getLink().contains("crossRefSource"))
                		linkObj.setOnlyLink(linkObj.getLink()+"&crossRefSource=" + crossRefSource);
            	}
            }
            
            if (linkObj != null && linkObj.getLink()!=null)
            {
            	String fileName = getFileNameFromLink(linkObj.getLink());
            	
                onBeforeAlreadyProcessCheck( fileName );
                
            	boolean isOverwrite = false;
            	if (dataSite.getSiteTypeInt() == GWTDataSite.RO_TYPE || 
            			dataSite.getSiteTypeInt() == GWTDataSite.OR_TYPE)
            		isOverwrite = isOverwriteDocNew(fileName);
                
            	//detect loop
            	//if we have a chain of documents that are gets us to the initial document we must break the 
            	//recursive chain of execution
            	if(isRecursiveAnaliseInProgress(linkObj.getLink())&&isEnabledAlreadyFollowed()){
            		//loop detected --> continue
            		logger.debug("Loop detected!!!! Link: " + linkObj.getLink());
            		
            		//remove from recursive list
            		//mSearch.removeRecursiveAnalisedLink( linkObj.getLink() );
            		//IndividualLogger.info("Link already followed:" + linkObj.getLink() + "\n", searchId);
            		if(getLogAlreadyFollowed()){
            			if (!StringUtils.isEmpty(linkObj.getLink())){
            				SearchLogger.info(getPrettyFollowedLink(linkObj.getLink()), searchId);
            			}
            		}
            		continue;
            	}
            	
	            if ( (isLinkVisited(linkObj.getLink()) || 
	            		isLinkValidated(linkObj.getLink()) || 
	            		isOverwrite )&&isEnabledAlreadyFollowed())
//                daca se face cautare cu lista de instr# sau B-P - crossreferinte care se aduc fara validare
//                ele se salveaza chiar daca au fost procesate intr-o alta cautare, ne-validate si marcate apoi
//                  pentru a nu mai fi procesate de mai multe ori - asta rezolva a doua conditie de mai sus
	            {
                    logger.debug("Already processed: " + linkObj.getLink());
            		//SearchLogger.info("<br/><span class='followed'>Link already followed</span>:" + linkObj.getLink() + "<br/>", searchId);       
                    if(getLogAlreadyFollowed()){
            			if (!StringUtils.isEmpty(linkObj.getLink())){
            				SearchLogger.info(getPrettyFollowedLink(linkObj.getLink()), searchId);
            			}
            		}
	                continue;
	            }
					
		            addLinkVisited(linkObj.getLink());
		            addRecursiveAnalisedLink(linkObj.getLink());
           }

            if (!mSearch.maxDocLimitReached() && linkObj!= null){
            	
        		//SearchLogger.info("<br/><span class='follow'>Following link.</span><br/>", searchId);
            	try {
            		Collection<ParsedResponse> collectionResponses  = getParsedResponsesFromLinkInPage(linkObj, overrideActionType);
            		for (ParsedResponse parsedResponse : collectionResponses) {
            			if(Boolean.TRUE.equals(parsedResponse.getAttribute(ParsedResponse.SERVER_RECURSIVE_ANALYZE))) {
            				ServerResponse intermediaryResponse = new ServerResponse();
            				intermediaryResponse.setParsedResponse(parsedResponse);
            				intermediaryResponse.setQuerry(parsedResponse.getPageLink().getLink());
            		        intermediaryResponse = recursiveAnalyzeResponse(intermediaryResponse);
            		        v.addAll(intermediaryResponse.getParsedResponse().getResultRows());
            			} else {
            				v.add(parsedResponse);
            			}
					}
            		
            	} catch (Exception e) {
					logger.error("Problem in recursive save[searchId:" + searchId + "]",e);
				}
            	
	            if( linkObj != null ){
	            	//mSearch.removeRecursiveAnalisedLink( linkObj.getLink() );
	            }
            } else if(mSearch.maxDocLimitReached()) {
            	break;
            }

        }
        ServerResponse rez = new ServerResponse();
        rez.getParsedResponse().setResultRows(v);
        return rez;

    }

    protected void manageDocumentSavedBefore(ParsedResponse pr) {
    	String instrNo = pr.getInstrumentNumber();
    	String book = pr.getBook();
    	String page = pr.getPage();
    	String instrNoBP = "";
    	if (!"".equals(instrNo)) {
    		instrNoBP = instrNo;
    	}
    	if (!"".equals(book) && !"".equals(book)) {
    		String bp  = book + "_" + page;
    		if (!"".equals(instrNoBP)) {
    			instrNoBP += "(" + bp + ")"; 
    		} else {
    			instrNoBP = bp;
    		}
    	}
    	SearchLogger.info("<br/><span class='followed'>Document already saved</span>: " + instrNoBP + "<br/>", searchId);
	}
    
    private boolean isDocumentSavedNow(ParsedResponse pr) {
    	DocumentI documentI = pr.getDocument();
    	if (documentI!=null) {
    		if (documentI.getIndexId()!=SimpleChapterUtils.UNDEFINED_ID) {
    			return true;
    		}
    	}
    	return false;
    }

	protected boolean isEnabledAlreadyFollowed() {
		return true;
	}

	public Collection<ParsedResponse> getParsedResponsesFromLinkInPage(LinkInPage linkObj,
			int overrideActionType) throws ServerResponseException {
    	ServerResponse sr;
        if (overrideActionType != -1) {
            sr = performLinkInPage(linkObj, overrideActionType);
        } else {
            sr = performLinkInPage(linkObj);
        }
        
        if(!sr.getParsedResponse().isError() && linkObj != null) {
        	mSearch.addValidatedLink(linkObj.getLink().toLowerCase());
        }
        Collection<ParsedResponse> result = new Vector<ParsedResponse>();
        // added to 
        if(sr != null){
        	result.add(sr.getParsedResponse());
        }
        return result;
	}

	/**
     * @see TSInterface#GetLink(javax.servlet.http.HttpServletRequest,
     *      javax.servlet.http.HttpServletResponse)
     */
    //call it with request.getQueryString();
    public ServerResponse GetLink(String vsRequest, boolean vbEncoded)
            throws ServerResponseException {
        msLastLink = vsRequest;
        String sAction = GetRequestSettings(vbEncoded, vsRequest);

        return performRequest(sAction, miGetLinkActionType, "GetLink", ID_GET_LINK, null, vsRequest, null);
    }

    protected ServerResponse FollowLink(String sLink, String imagePath)
            throws ServerResponseException {
        
    	String sAction = null;
    	
    	if (this instanceof TNWilsonRO) {
			sAction = GetRequestSettings(false, sLink);
		} else {
			//this is fucking shit, who the fuck did that and why. next time, test the origin of the server mf
			sAction = (sLink.endsWith("pdf") && !sLink.contains("Link=/specialAlertDocs/")) ? sLink : GetRequestSettings(false, sLink);	
		}
    	
                

        //bug fix in link generation
        if (sAction.startsWith("/http://"))
            sAction = sAction.replaceFirst("/http://", "http://");
        
        ////////////////////////////////
        return performRequest(sAction, miGetLinkActionType, "FollowLink", ID_GET_LINK, imagePath, null, null);
    }

    public ServerResponse recursiveAnalyzeResponse(ServerResponse result)
            throws ServerResponseException {
        return recursiveAnalyzeResponse(result, -1);
    }

    /**
     * @param extraParams 
     * @see TSInterface#SaveToTSD(javax.servlet.http.HttpServletRequest,
     *      javax.servlet.http.HttpServletResponse,String SesionID)
     */
    public ServerResponse SaveToTSD(String vsRequest, Map<String, Object> extraParams)
            throws ServerResponseException {
        return recursiveSaveToTSD(vsRequest, extraParams);
    }

    /**
     * @param vsRequest
     * @param checkForDocType
     * daca e setat pe true, se face verificarea daca mai exista un document care are
     * acelasi instrument No, dar docType diferit, caz in care nu se va suptascie
     * documentul, ci se va salva cu acelasi instrumentNo, si DocType diferit
     * @param extraParams 
     * @return
     * @throws ServerResponseException
     */
    public ServerResponse simpleSaveToTSD(String vsRequest, boolean checkForDocType, Map<String, Object> extraParams)
            throws ServerResponseException {

        msSaveToTSDFileName = "Information.html";
        logger.debug(" saving page " + vsRequest);
        IndividualLogger.info( " saving page " + vsRequest,searchId );
        
        String sAction = GetRequestSettings(false, vsRequest);
        String initialvsRequest = vsRequest;
        vsRequest = vsRequest.replace("&isSubResult=true", "");
        vsRequest = vsRequest.replaceFirst("&crossRefSource=[^&]+", "");
        vsRequest = vsRequest.replaceAll("&saveSomeWithCrossRef=true", "");
        Object item = null;
        if (mSearch.existsInMemoryDoc(vsRequest) || 
            mSearch.existsInMemoryDoc(vsRequest.replaceAll("&parentSite=true", "")) || 
            ((item = getCachedDocument(sAction))!=null)) {
            
            logger.debug(" present in memory ");
            IndividualLogger.info( " present in memory " ,searchId);
            
            if(item==null) {
            	item = (vsRequest.indexOf("&parentSite=true") >= 0 ? 
						mSearch.getInMemoryDoc(vsRequest.replaceAll("&parentSite=true", "")) : 
						mSearch.getInMemoryDoc(vsRequest));

				mSearch.removeInMemoryDoc(vsRequest.indexOf("&parentSite=true") >= 0 ? 
						vsRequest.replaceAll("&parentSite=true", "") : 
						vsRequest);
            }
            
            ServerResponse Response = new ServerResponse();
            
            RawResponseWrapper rrw = null;
            if ( item instanceof String ){
            	rrw = new RawResponseWrapper( (String) item);
            }else if(item instanceof ServerResponse){
            	rrw = new RawResponseWrapper( ((ServerResponse)item).getResult() );
            	Response = (ServerResponse)item;
            }
            else {
            	try{
            		ParsedResponse itemAsParsedResponse = (ParsedResponse)item;
            		itemAsParsedResponse.setAttributes(extraParams);
	            	rrw = new RawResponseWrapper( itemAsParsedResponse.getResponse() );
	            	Response.setParsedResponse( itemAsParsedResponse );
            	}
            	catch( Exception e ){
            		Response.setParsedResponse( new ParsedResponse() );
            	}
            }
            
            if (vsRequest.indexOf("parentSite=true") >= 0)
                Response.setParentSiteSearch(true);
            if (checkForDocType)
            	Response.setCheckForDocType(true);
            
            Response.setQuerry(initialvsRequest);
            Response.getParsedResponse().setParentSite(isParentSite());
            Response.getParsedResponse().setPageLink(new LinkInPage(vsRequest,vsRequest));
            Response.getParsedResponse().setAttributes(extraParams);
            solveResponse(sAction, ID_SAVE_TO_TSD, "SaveToTSD", Response, rrw, null);
            
            return Response;
            
        } else {
            String action = "SaveToTSD";
        	if (checkForDocType)
        		action += "_" + CHECK_DOC_TYPE;
        	if(vsRequest.contains(DASLFINAL)){
        		action  = vsRequest;
        	}
            return performRequest(sAction, miGetLinkActionType, action, ID_SAVE_TO_TSD, null, vsRequest, extraParams);
        }

    }
    
    protected ServerResponse performRequest(String page, int methodType, String action, int parserId, String imagePath, String vbRequest , Map<String, Object> extraParams) throws ServerResponseException {
    	
    	/* Testeaza daca trebuie sa verificam sau nu la suprascrierea documentelor
    	 * care sunt cu acelasi InstNo dar DocType diferit
    	 */
        try
        {
        	boolean checkForDocType = false;
        	
            ServerResponse response = new ServerResponse();
            response.setCheckForDocType(checkForDocType);
            
            if (dataSite.getCountyId() == CountyConstants.TN_Montgomery 
            		&& dataSite.getSiteTypeInt() == GWTDataSite.TR_TYPE) {
                
                String query = getTSConnection().getQuery();
                
                synchronized (BridgeConn.syncaccessMontgomeryTR) {
                
                    response = LogIn();
                
                    logger.info(action + ": " + getDataSite().getName() + query);
                    IndividualLogger.infoDebug( action + ": " + getDataSite().getName() + query,searchId );
                    
                    //if (!BridgeConn.notsupported.contains(ERROR_CLASS_NAME.trim())) {
                    //    String key = ERROR_CLASS_NAME;
    
                        BridgeConn bc = new BridgeConn(getTSConnection(), query, page,
                                methodType, action, searchId, miServerID);
                        
                        bc.process();
                        
                        query = bc.getTsc().getQuery();
    
                    //} else
                    //    getTSConnection().sendRequestToServerPage(page, methodType, "", 3,
                    //            ERROR_CLASS_NAME);
                    if (query.indexOf("parentSite=true") >= 0)
                        response.setParentSiteSearch(true);
                    response.setQuerry(query);
                    response.setCheckForDocType(checkForDocType);
                    solveResponse(page, parserId, action, response, getTSConnection().getResponseWrapper(), imagePath);
                }
            }
            else if (dataSite.getCountyId() == CountyConstants.TN_Sumner 
            		&& dataSite.getSiteTypeInt() == GWTDataSite.TR_TYPE) {
                
                String query = getTSConnection().getQuery();
                
                synchronized (BridgeConn.syncaccessSumnerTR) {
                
                    response = LogIn();
                
                    logger.info(action + ": " + getDataSite().getName() + query);
                    IndividualLogger.infoDebug( action + ": " + getDataSite().getName() + query ,searchId);

                    BridgeConn bc = new BridgeConn(getTSConnection(), query, page,
                            methodType, action, searchId, miServerID);
                    
                    bc.process();
                    
                    query = bc.getTsc().getQuery();
    
                                    
                    if (query.indexOf("parentSite=true") >= 0)
                        response.setParentSiteSearch(true);
                    
                    response.setQuerry(query);
                    response.setCheckForDocType(checkForDocType);
                    solveResponse(page, parserId, action, response, getTSConnection().getResponseWrapper(), imagePath);
                    
                    //CookieManager.addCookie("[TNMontgomeryTR] ", null);
                }
                
            }
            else if (dataSite.getCountyId() == CountyConstants.TN_Davidson 
            		&& dataSite.getSiteTypeInt() == GWTDataSite.TR_TYPE) {
                
                String query = getTSConnection().getQuery();
                
                int count = 1;
                while (count <= 6) {
                    
                    try {
                        
                        response = LogIn();
                
    		            logger.info(action + ": " + getDataSite().getName() + query);
                        IndividualLogger.infoDebug( action + ": " + getDataSite().getName() + query ,searchId);
    		                		           
		                BridgeConn bc = new BridgeConn(getTSConnection(), query, page,
		                        methodType, action, searchId,miServerID);
		                
		                bc.process();
    		            
    		            if (query.indexOf("parentSite=true") >= 0)
    		                response.setParentSiteSearch(true);
    		            
    		            response.setQuerry(query);
    		            response.setCheckForDocType(checkForDocType);
    		            solveResponse(page, parserId, action, response, getTSConnection().getResponseWrapper(), imagePath, extraParams);
    		            
    		            break;
                
    		        } catch (Exception th) {
    		            
    		            logger.error("Unexpected Error...count=" + count + "\n" + th.getMessage());
                        th.printStackTrace( System.err );
                        
    		            CookieManager.addCookie(Integer.toString(miServerID), null);
    		            
    		            if (count == 6) {
                            ServerResponse sr = null;
                            if( th instanceof ServerResponseException )
                            {
                                ServerResponseException sre = (ServerResponseException) th;
                                sr = sre.getServerResponse();
                                sr.setError( ServerResponse.DEFAULT_ERROR );
                            }
                            else
                            {
                                sr = new ServerResponse();
                                sr.setError("Internal Error:" + ServerResponseException.getExceptionStackTrace( th ), ServerResponse.CONNECTION_IO_ERROR);
                            }
                            
                            count = 7;
                            response = sr;
    		            }
    		        }
    		        count++;
    		    }
            
            } 
            else {
                
                String query = getTSConnection().getQuery();
                
                int count = 1;
                while (count <= 6 && !skipCurrentSite() && !isStopAutomaticSearch()) {
                    
                    try {
                        
                        response = LogIn();
                
                        if (query.indexOf("parentSite=true") >= 0 || isParentSite())
    		                response.setParentSiteSearch(true);
                        
    		            logger.info(action + ": " + getDataSite().getName()+ " - " + query);
                        IndividualLogger.infoDebug( action + ": " + getDataSite().getName() + " - " + query ,searchId);
                        
                        String cachedResponse = null;
                        try {
                        	Object cachedDocument = getCachedDocument(page);
                        	if(cachedDocument instanceof String) {
                        		cachedResponse = (String)cachedDocument;
                        	}
                        	
                        } catch (Exception e) {
							logger.error(searchId + ": Error while trying to use cached document", e);
						}
                        
                        if(StringUtils.isNotEmpty(cachedResponse)) {
                        	
                        	response.setQuerry(query);
	    		            response.setCheckForDocType(checkForDocType);
	    		            ParsedResponse parsedResponse = response.getParsedResponse(); 
	    		            if( (parsedResponse.getPageLink() == null || StringUtils.isNotEmpty(parsedResponse.getPageLink().getLink())) && 
	    		            		StringUtils.isNotEmpty(vbRequest)) {
	    		            	parsedResponse.setPageLink(new LinkInPage(vbRequest,vbRequest));
	    		            } 
	    		            parsedResponse.setAttributes(extraParams);
	    		            solveResponse(page, parserId, action, response, new RawResponseWrapper(cachedResponse), imagePath);
	    		            
                        } else {
                        
                        	BridgeConn bc = new BridgeConn(getTSConnection(), query, page,
                        			methodType, action, searchId,miServerID);
	    		                
                        	bc.process();
	    		            
	    		            response.setLastURI(getTSConnection().getLastURI());
	    		            response.setQuerry(query);
	    		            response.setCheckForDocType(checkForDocType);
	    		            ParsedResponse parsedResponse = response.getParsedResponse(); 
	    		            if( (parsedResponse.getPageLink() == null || StringUtils.isNotEmpty(parsedResponse.getPageLink().getLink())) && 
	    		            		StringUtils.isNotEmpty(vbRequest)) {
	    		            	parsedResponse.setPageLink(new LinkInPage(vbRequest,vbRequest));
	    		            } 
	    		            parsedResponse.setAttributes(extraParams);
	    		            solveResponse(page, parserId, action, response, getTSConnection().getResponseWrapper(), imagePath);
    		            
                        }
    		            
    		            break;
                
    		        } catch (Exception th) {
    		            
    		            logger.error("Unexpected Error...count=" + count + "\n" + th.getMessage());
                        IndividualLogger.infoDebug( th.getMessage() + " " + ServerResponseException.getExceptionStackTrace( th ).replaceAll( "<BR>\n", "\n\n" ),searchId );
                        SearchLogger.info("</div>", searchId); //for Bug 2652
                        th.printStackTrace( System.err );
                        
    		            if (dataSite.getCountyId() == CountyConstants.MO_Jackson 
    		            		&& dataSite.getSiteTypeInt() == GWTDataSite.RO_TYPE 
    		            		&& query.indexOf("imgRetrieve") != -1) {
    		                // a crapat cand vroia sa downloadeze imagine de pe JacksonRO (imaginile se genereaza la prima cerere) 
    		                // revenim cu cererea in 20 secunde
    		                try {Thread.sleep(20000);} catch (Exception e) {}
    		            }
    		            
    		            if (count == 6 || !continueSeachOnThisServer() || skipCurrentSite() || isStopAutomaticSearch()) {
                            ServerResponse sr = null;
                            if( th instanceof ServerResponseException )
                            {
                                ServerResponseException sre = (ServerResponseException) th;
                                sr = sre.getServerResponse();
                                String result = sr.getResult();
                                if(result != null && result.startsWith("LoginException:")) {
                                	sr.setError(result.substring("LoginException:".length()));
                                } else {
                                	sr.setError( ServerResponse.DEFAULT_ERROR );
                                }
                            }
                            else
                            {
                                sr = new ServerResponse();
                                sr.setError("Internal Error:" + ServerResponseException.getExceptionStackTrace( th ), ServerResponse.CONNECTION_IO_ERROR);
                            }
                            
                            response = sr;
                            
                            count = 7;
    		            } else {
    		            	ServerResponse sr = null;
    		            	if( th instanceof ServerResponseException ){
                                ServerResponseException sre = (ServerResponseException) th;
                                sr = sre.getServerResponse();
                                if (sr!=null && sr.getResult()!= null && sr.getResult().contains("Connection timed out: connect")){
                                	sr.setError( ServerResponse.CONNECTION_IO_ERROR );                                    
                                    count = 7;
                                    response = sr;
                                }
                            }
    		            }
    		            CookieManager.addCookie(Integer.toString(miServerID), null);
    		        }
    		        count++;
    		    }
            }
           
            
            return response;
        }
        catch( ServerResponseException allSre )
        {
            IndividualLogger.infoDebug( "PerformRequest error:" + allSre.getMessage() + " " + ServerResponseException.getExceptionStackTrace( allSre ).replaceAll( "<BR>\n", "\n\n" ) ,searchId);
            throw allSre;
        } catch (Exception e) {
        	//if other exception was genetared we should handle it
        	e.printStackTrace();
        	
        	System.err.println("Error in performResuest!");
        	
        	ServerResponse sr = new ServerResponse();
        	sr.setError(ServerResponse.DEFAULT_ERROR);
        	return sr;
        }
    }

    public void onBeforeAlreadyProcessCheck( String fileName )
    {
        
    }
    
    public ServerResponse recursiveSaveToTSD(String vsRequest, Map<String, Object> extraParams)
            throws ServerResponseException {

    	boolean saveWithoutCrossRef = vsRequest.contains("&saveWithoutCrossRef=true");
    	vsRequest = vsRequest.replaceAll("&saveWithoutCrossRef=true", "");
    	
    	if(extraParams != null) {
    		
    	}
    	
        String fileName = getFileNameFromLink(vsRequest);
        String fileNameExtended = getServerTypeDirectory() + fileName;
        logger.debug("processing file " + fileNameExtended);
        
        IndividualLogger.info( "" ,searchId);
        IndividualLogger.info( "" ,searchId);
        IndividualLogger.info( "processing file " + fileNameExtended,searchId );
        
        boolean isOverwrite = isOverwriteDocNew(fileName);
        boolean checkForDocType = false;
        
        onBeforeAlreadyProcessCheck( fileName );
        
        if (((mSearch.containsVisitedDoc(fileNameExtended) || (FileAlreadyExist(fileName) )) && isOverwrite)) 
        {            
            if (!fileName.equals("") && 
               // in cazul cand este crossref si trebuie salvat, dar anterior procesat, invalidat si marcat ca deja prelucrat 
                    (vsRequest.indexOf("isSubResult=true") == -1 || FileAlreadyExist(fileName))) 
                 { // not an unknown file 
                     logger.debug("..already processed");
                     IndividualLogger.info( "..already processed",searchId);
                     //checkForDocType = true;
                     return new ServerResponse();
                 }  
        }
        
        ServerResponse result = simpleSaveToTSD(vsRequest, checkForDocType, extraParams);
        vsRequest = result.getQuerry();
        if (vsRequest.contains("&saveWithoutCrossRef=true")) {
        	saveWithoutCrossRef = true;
        	vsRequest = vsRequest.replaceAll("&saveWithoutCrossRef=true", "");
        	result.setQuerry(vsRequest);
        }
        Vector<ServerResponse> extraResponses = new Vector<ServerResponse>();
        //we must clean the request for this flag that saved bootstraps data for update search
        result.getParsedResponse().setAttribute(RequestParams.PARENT_SITE_FOR_UPDATE_SEARCH_PARAMS, null);
        /*if(ServerConfig.isOcrAllDocumentsParentsiteEnable() &&  isParentSite()){
        	DocumentI documentI =  result.getParsedResponse().getDocument();
        	if(documentI!= null &&documentI.isChecked()&& (documentI.getDocType().equals(DocumentTypes.MORTGAGE) ||
					documentI.getDocType().equals(DocumentTypes.TRANSFER))){
        		OCRDownloader ocrDownloader = new OCRDownloader( searchId , documentI);
        		OCRDownloaderPool.getInstance().runTask(ocrDownloader);
        	}
        }*/
        
        if (!fileName.equals("")) { // not an unknown file
            mSearch.addVisitedDoc(fileNameExtended);
        }

        if (result.getErrorCode() == ServerResponse.NOT_VALID_DOC_ERROR) {
            result.clearError();
        } else if(Boolean.TRUE.equals(GetAttribute("DO_NOT_ANALYZE_RESPONSE"))) {
        	//do nothing
        } else if ((continueSeachOnThisServer() && continueSeach() && !skipCurrentSite() && !isStopAutomaticSearch()) || 
        		(result.isParentSiteSearch() && !saveWithoutCrossRef)) {
            recursiveAnalyzeResponse(result);
        }
        
        for (ServerResponse extraResponse : extraResponses) {
        	if (extraResponse.getErrorCode() == ServerResponse.NOT_VALID_DOC_ERROR) {
        		extraResponse.clearError();
            } else if ((continueSeachOnThisServer() && continueSeach() && !skipCurrentSite() && !isStopAutomaticSearch()) || 
            		(extraResponse.isParentSiteSearch() && !saveWithoutCrossRef)) {
                recursiveAnalyzeResponse(extraResponse);
            }
		}
        
        return result;
    }

    protected String getFileNameFromLink(String link) {
        return link;
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    /**
     * GetFile
     */

    private void input2outputStream(BufferedInputStream bf,
            OutputStream otptStream) throws IOException {
        int iBufferSize = 8192;
        int iReadedSize = 0;
        byte[] baBuf = new byte[iBufferSize];
        DataInputStream di = new DataInputStream(bf);
        //set response

        long t0 = System.currentTimeMillis();
        if (bf != null) {
            while (iReadedSize != -1) {

                iReadedSize = di.read(baBuf);
                //logger.debug("iReadedSize = " + iReadedSize);
                if (iReadedSize != -1)
                    otptStream.write(baBuf, 0, iReadedSize);
            }
        }

        otptStream.flush();
        otptStream.close();
        long t1 = System.currentTimeMillis();
        logger.debug("Image download time: " + (t1 - t0));
    }
    
    protected void solveResponse(String sAction, int viParseID, String rsFunctionName, ServerResponse Response, RawResponseWrapper rrw, String imagePath) 
    throws ServerResponseException {
    	solveResponse(sAction, viParseID, rsFunctionName, Response, rrw, imagePath, null);
    }

    protected void solveResponse(String sAction, int viParseID, String rsFunctionName, ServerResponse Response, RawResponseWrapper rrw, String imagePath, Map<String, Object> extraParams) 
    throws ServerResponseException {
        if (rrw.getContentType().indexOf(TSConnectionURL.HTML_CONTENT_TYPE) != -1 
        		|| rrw.getContentType().indexOf(TSConnectionURL.XML_CONTENT_TYPE) != -1)  {
            logger.debug("solveHtmlResponse contentType=" + rrw.getContentType() + " length=" + rrw.getContentLength());
            solveHtmlResponse(sAction, viParseID, rsFunctionName, Response, rrw.getTextResponse(), extraParams);
        } else if(rrw.getContentType().indexOf(TSConnectionURL.JSON_CONTENT_TYPE) != -1
        		|| rrw.getContentType().indexOf(TSConnectionURL.TEXT_PLAIN_CONTENT_TYPE) != -1 ) {
        	logger.debug("solveHtmlResponse contentType=" + rrw.getContentType() + " length=" + rrw.getContentLength());
            try {
				solveHtmlResponse(sAction, viParseID, rsFunctionName, Response, IOUtils.toString(rrw.getBinaryResponse()), extraParams);
			} catch (IOException e) {
				logger.error("Exception while saving JSON_CONTENT_TYPE", e);
			}
        	
        } else {
            logger.debug("solveBinaryResponse contentType=" + rrw.getContentType() + " length=" + rrw.getContentLength());
            solveBinaryResponse(viParseID, rsFunctionName, Response, rrw, imagePath, extraParams);
        }
    }

    void solveBinaryResponse(int viParseID, String rsFunctionName, ServerResponse Response, RawResponseWrapper rw, String imagePath, Map<String, Object> extraParams)throws ServerResponseException {
    	String contentType = rw.getContentType();
        BufferedInputStream bf = rw.getBinaryResponse();
        DownloadImageResult result = new DownloadImageResult();
        result.setStatus( DownloadImageResult.Status.OK ) ;
      
        if(StringUtils.isEmpty(imagePath)){
        	String imagedirs = ServerConfig.getTsrCreationTempFolder() + File.separator;
        	FileUtils.CreateOutputDir(imagedirs);
        	imagePath = imagedirs+"sbrtemp_" + System.nanoTime();
        }
        File saveFile = new File(imagePath);
        OutputStream otptStream = null;
        try {
        	
            otptStream = org.apache.commons.io.FileUtils.openOutputStream( saveFile );
            input2outputStream( bf, otptStream );
    		result.setImageContent(FileUtils.readBinaryFile(imagePath));
    		result.setContentType(contentType);
        } catch (IOException e) {
        	
        	if(saveFile.exists() && saveFile.length() > 0) {
        		result.setImageContent(FileUtils.readBinaryFile(imagePath));
        		result.setContentType(contentType);
        	} else {
	        	logger.error("Error in solveBinaryReponse:", e);
	            Response.setError("Error: " + e.getMessage()  + " received by class: " + getDataSite().getName()+ " in function: " + rsFunctionName + viParseID); //here was SetError
	            result.setStatus( DownloadImageResult.Status.ERROR );
	            Response.setImageResult(result);
	            throw new ServerResponseException(Response);
        	}
        } finally {
            if (rw.getHm() != null){
                rw.getHm().releaseConnection();
            }
            afterDownloadImage(result.getStatus()==DownloadImageResult.Status.OK);
        }
        Response.getParsedResponse().setSolved(true); 
        Response.setImageResult(result);
    }

    protected boolean isDocumentValid( ServerResponse sr ){
    	boolean valid = false;
    	
    	if( docsValidators != null ){
    		Iterator<DocsValidator> docsValidatorIterator = docsValidators.iterator();
    		
    		if( docsValidators.size() > 0 ){
    			valid = true;
    		}
    		
    		while( docsValidatorIterator.hasNext() ){
    			DocsValidator currentValidator = docsValidatorIterator.next();
    			
    			//do not apply the validator if the same filter was applied to the results returned by same module
    			if (currentValidator.isOnlyIfNotFiltered() && currentValidator.getFilter().isInModulesAppliedFor(getSearch().getSearchRecord().getModule().getUniqueKey())) {	
    				continue;
    			}
    			
    			boolean currentValid = currentValidator.isValid( sr );
    			
    			valid = valid && currentValid;
    			
            	String validatorName = currentValidator.getValidatorName() ;
            	if(!"DocsValidator".equals(validatorName)){
            		SearchLogger.info("<span class='filter'>Validate</span> by <span class='criteria'>" + validatorName + "</span>: document is " + (currentValid ? "<font color=\"green\">VALID</font>" : "<font color=\"red\">INVALID</font>") + ".<br>", searchId);
            	} else {
            		//SearchLogger.info("No validation performed : document is considered <font color=\"green\">VALID</font> by default.<br>", searchId);
            	}
            	
            	if(!valid){
            		break;
            	}
    			
    		}
    	}
    	
    	return valid;
    }
    
    /**
     * validates a crossRef document
     * @param sr
     * @return
     */
    protected boolean isCrossRefDocumentValid( ServerResponse sr ){
    	boolean valid = true;
    	
    	if( crossRefDocsValidators != null ){
    		Iterator<DocsValidator> docsValidatorIterator = crossRefDocsValidators.iterator();
    		
    		if( crossRefDocsValidators.size() > 0 ){
    			valid = true;
    		}
    		
    		while( docsValidatorIterator.hasNext() ){
    			DocsValidator currentValidator = docsValidatorIterator.next();
    			
    			boolean currentValid = currentValidator.isValid( sr );
    			
    			valid = valid && currentValid;
    			
            	String validatorName = currentValidator.getValidatorName() ;
            	SearchLogger.info("<span class='filter'>Cross Reference Validator</span> by <span class='criteria'>" + validatorName + "</span> : document is " + 
            			(currentValid ? "<font color=\"green\">VALID</font>" : "<font color=\"red\">INVALID</font>") + ".<br>", searchId);
            	if(!valid){
            		break;
            	}
    		}
    	}
    	return valid;
    }
    
    /**
     * validate the document that refer to a mortgage if that mortgage is valid and saved to TSRI 
     * @param sr
     * @return
     */
    protected boolean passWithoutValidation(ServerResponse sr){
    	boolean valid = false;
    	
    	DocumentI document = sr.getParsedResponse().getDocument();
		DocumentsManagerI manager = getSearch().getDocManager();
		
		if (document != null) {
			if(document.isOneOf(DocumentTypes.ASSIGNMENT, DocumentTypes.APPOINTMENT, DocumentTypes.MODIFICATION, DocumentTypes.RELEASE, DocumentTypes.SUBORDINATION)){
				
				Set<InstrumentI> references = document.getParsedReferences();
				for (InstrumentI reference : references) {
					try{
						manager.getAccess();
						ArrayList<RegisterDocumentI> docList = manager.getRegisterDocuments(reference, true);
						
						for (RegisterDocumentI registerDocumentI : docList) {
						//if(manager.getRegisterDocuments(reference, true).size() > 0){
							if (registerDocumentI.isOneOf(DocumentTypes.MORTGAGE)){
								valid = true;
								SearchLogger.info("Document is a crossreference of " + registerDocumentI.getDocType() +", which is saved in TSRI and does not require validation. <br>", searchId);
								break;
							}
						}
					} finally{
						manager.releaseAccess();
					}
				}
			}
		}	
    	return valid;
    }
    
    protected void solveHtmlResponse(String sAction, int viParseID,
            String rsFunctionName, ServerResponse Response, String htmlString)
            throws ServerResponseException {
    	solveHtmlResponse(sAction, viParseID, rsFunctionName, Response, htmlString, null);
    }
    
    protected void solveHtmlResponse(String sAction, int viParseID,
            String rsFunctionName, ServerResponse Response, String htmlString, Map<String, Object> extraParams)
            throws ServerResponseException {
    	Response.setResult(htmlString);
        Response.setParentSiteSearch(isParentSite());
        Response.getParsedResponse().setParentSite(isParentSite());
    	try {
        
    		if (Response.getResult() != null) { //send it to child to parse the answer
	    			ParseResponse(sAction, Response, viParseID);
	                
	   			 	String x = Response.getParsedResponse().getResponse();
		 		    
	   			 	//build an object of type page and link  
	   			 	PageAndIndexOfLink  palink = new PageAndIndexOfLink();
		 		   
	   			 	//add the search string to the object
	   			 	palink.setPage(x);
	   			 	//add the page and index object to the search record 
		 		    getSearch().getSearchRecord().addPageAndIndex(palink);
	            } else {
	                Response.setError("Error: BLANK RESPONSE "
	                        + " received by class: " + getDataSite().getName()
	                        + " in function: " + rsFunctionName + viParseID);
	                throw new ServerResponseException(Response);
	            }

            //test if we have to save on hdd the response
            if (viParseID == ID_SAVE_TO_TSD) {
            
            	if( mSearch.maxDocLimitReached() ){
                    IndividualLogger.info( "Max document count limit reached, not saving to TSR!" ,searchId);
                    SearchLogger.info("<br>Max document count limit reached, not saving to TSR!<br>",searchId);
                    
                    saveInvalidatedDocumentForRestoration(null, Response, false);
                    
                    return;
            	} 
            	
            	String textRepresentation = Response.getParsedResponse().getTextRepresentation();
            	
            	if(textRepresentation.equals(""))	//B2178
                	return;
            	Response.getParsedResponse().setSearchId(searchId);
            	String tsrIndexRepresentation = Response.getParsedResponse().getTsrIndexRepresentation();
                IndividualLogger.info( "Validating document..." ,searchId);
               
                String type = "details";
                String id = String.valueOf(System.nanoTime()) + "_details";
                
                StringBuilder log = new StringBuilder();
                log.append("<div id='").append(id).append("'>")
                	.append("<br/>Retrieved <span class='rtype'>").append(type).append("</span> document:<br/>")
                	.append(getExtraToShowWhenProcessingDetails(Response))
                	.append("<table border='1' cellspacing='0' width='99%'>")
                	.append("<tr><th width='2%'>DS</th><th width='24%' align='left'>Desc</th><th width='7%'>Date</th><th width='17%'>Grantor</th><th width='17%'>Grantee</th><th width='10%'>Instr Type</th><th width='7%'>Instr</th><th width='16%'>Remarks</th></tr>")
                	.append("<tr>").append(tsrIndexRepresentation).append("</tr></table>");
                
                /*
                SearchLogger.info("<div id='" + id + "'>", searchId);
                SearchLogger.info("<br/>Retrieved <span class='rtype'>" + type + "</span> document:<br/>",searchId);
                SearchLogger.info(
                	"<table border='1' cellspacing='0' width='99%'>" +
                	"<tr><th width='2%'>DS</th><th width='24%' align='left'>Desc</th><th width='7%'>Date</th><th width='17%'>Grantor</th><th width='17%'>Grantee</th><th width='10%'>Instr Type</th><th width='7%'>Instr</th><th width='16%'>Remarks</th></tr>" +
                	"<tr>" + tsrIndexRepresentation + "</tr></table>" , searchId);
                */
                
                
                SearchLogger.info(log.toString(), searchId);
                
                
                /*
                 * document is valid if it is a link cross-reference or if it is validated by validator
                 */
                boolean valid = false;
                if(Response.getParsedResponse().isError()){
                	SearchLogger.info("Document has error code set : document is <font color=\"red\">INVALID</font>. <br>", searchId);                	
                } else { 
                	if(Response.getQuerry().contains("isSubResult=true") && !(this instanceof TSServerDTG) ){
	                	if (Response.getInGoBackOnLevel() || getSearch().getSearchType()==Search.GO_BACK_ONE_LEVEL_SEARCH){
	                		valid = false;
	                		SearchLogger.info("Document is a crossreference and is not valid (is in Go Back On Level): document is <font color=\"red\">NOT VALID</font>. <br>", searchId);
	                	} else {
	                		String crossRefType = Response.getCrossRefSourceType();
	                		if(!StringUtils.isEmpty(crossRefType)){
	                			SearchLogger.info("Document is a crossreference and its source document is " + crossRefType + " <br>", searchId);
	                		}
	                		if(!isCrossRefToBeValidated(crossRefType)){
	                			SearchLogger.info("Document is a crossreference and its source document is " + crossRefType
	                					+ " so we do not require validation : document is <font color=\"green\">VALID</font>. <br>", searchId);
	                			valid = true;
	                		}
	                		else {
		                		SearchLogger.info("Document is a crossreference and has to be validated <br>", searchId);
		                		valid = isCrossRefDocumentValid(Response);
	                		}
	                	}
                	} else {
                		valid = passWithoutValidation(Response);
                		
                		if (!valid){
                				
	                		if( docsValidators != null && docsValidators.size() > 0 ){
	                			valid = isDocumentValid( Response );
	                		}
	                		else{
			                	valid = docsValidator.isValid(Response);
			                	String validatorName = docsValidator.getValidatorName();
			                	if(!"DocsValidator".equals(validatorName)){
			                		SearchLogger.info("<span class='filter'>Valide</span> by <span class='criteria'>" + validatorName + "</span>: document is " + (valid ? "<font color=\"green\">VALID</font>" : "<font color=\"red\">INVALID</font>") + ".<br>", searchId);
			                	} else {
			                		//SearchLogger.info("No validation performed : document is considered <font color=\"green\">VALID</font> by default.<br>", searchId);
			                	}
	                		}
                		}
	                }
                }
            	if (valid)
                    {
                    
                    LinkInPage validatedLinkObj = null;
                    
                    try
                    {
                    	validatedLinkObj = Response.getParsedResponse().getPageLink();
                    	/*
                    	Vector resultRows = Response.getParsedResponse().getResultRows();
                    	if(resultRows.size() > 0){
                    		ParsedResponse pr = (ParsedResponse) resultRows.get(0);
                    		validatedLinkObj = pr.getPageLink();
                    	}*/
                    }catch( Exception e ) {
                    	e.printStackTrace();
                    }
                    
                    if( validatedLinkObj != null )
                    {
                    	addValidatedLink(validatedLinkObj.getLink());
                    	addSpecificValidatedLink(Response);
                    }
                    
                    CrossRefCleaner.removeTooManyCrossRef(Response,mSearch.getID()); // scot crossreferintele daca sunt mai multe
                    
                    IndividualLogger.info( "Document valid\n\n",searchId );
                    
                	//creating the file
                    try {
                    	
                    	boolean overwritten = false;
                        
                    	String sTmpFilePath = mSearch.getSearchDir();
                    	ParsedResponse pr = Response.getParsedResponse();
                    	Object rawServerResponse = pr.getAttribute(ParsedResponse.SERVER_ROW_RESPONSE);
                    	String sTSDHtml = pr.getResponse();
                    	if(rawServerResponse != null && rawServerResponse instanceof String) {
                    		sTSDHtml = (String)rawServerResponse;
                    	}
                        
                        Vector sdSets = pr.getSaleDataSet();
                        String documentType = null;
                        for (Object object : sdSets) {
                        	SaleDataSet sds = null;
							if(object instanceof SaleDataSet)
								sds = (SaleDataSet)object;
							if(sds == null)
								continue;
							String docType = sds.getAtribute("DocumentType");
							if(!StringUtils.isEmpty(docType)) {
								documentType = DocumentTypes.getDocumentCategory(docType, searchId);
								if(!StringUtils.isEmpty(documentType))
									break;
							}
						}
                        
                        if(!StringUtils.isEmpty(documentType)){
                        	
                        	if(StringUtils.isNotEmpty(msSaveToTSDFileName)) {
	                        	String nameWithoutExtension = msSaveToTSDFileName.substring(0,msSaveToTSDFileName.lastIndexOf('.'));
	                        	if( !nameWithoutExtension .endsWith( documentType  ) ){
	                        		msSaveToTSDFileName = msSaveToTSDFileName.replace(".html", documentType + ".html");
	                        		try {
										pr.setFileName(pr.getFileName().replace(
												".html", documentType + ".html"));
									} catch (Exception e) {
										// TODO: handle exception
									}
	                        	}
                        	}
                        	
                        	for (int i = 0; i < pr.getImageLinksCount(); i++) {
                        		String imageName = pr.getImageLink(i).getImageFileName();
                        		String imageNameWithoutExtension = imageName .substring(0,imageName.lastIndexOf('.'));
                        		if(!imageNameWithoutExtension .endsWith(documentType)){
                        			pr.getImageLink(i).setImageFileName(imageName.replace(".", documentType + "."));
                        		}
                        	}
                        }
                        
                        DocumentI document = pr.getDocument();
                        if( document != null ){
                        	document.setOldIndexPath( mSearch.getSearchDir() + pr.getFileName() );
                        }
                        
                        String innerTableStyle = "";
                        if (sTSDHtml.contains("innerTable")){
                        	innerTableStyle = "<LINK REL=StyleSheet HREF='web-resources/css/tsrIndex/parent_site_result.css' TYPE=\"text/css\"/>";
                        }
                        sTSDHtml = "<html><head>" + innerTableStyle + "</head><body>" + sTSDHtml + "</body></html>";
                        
                        sTSDHtml = sTSDHtml.replaceAll("<IMG SRC=\"\\.\\./", "<BASE SRC=\"");
                        sTSDHtml = sTSDHtml.replaceAll("<img src=\"\\.\\./", "<BASE SRC=\"");                        
                        sTSDHtml = sTSDHtml.replaceAll("<TD COLSPAN=\"2\"  width=\"616\"><CENTER>", "<TD COLSPAN=\"2\" ><CENTER>");
                        //se sterg toate comentariile
                        sTSDHtml = sTSDHtml.replaceAll("<!-- NEW PAGE -->", "@@NEWPAGE_NEWPAGE@@");
                        sTSDHtml = sTSDHtml.replaceAll("<!--[a-zA-Z0-9/=,_\\<\\?\\.\\>'\\s\\\"\\r\\n]+-->", "");
                        sTSDHtml = sTSDHtml.replaceAll("@@NEWPAGE_NEWPAGE@@", "<!-- NEW PAGE -->");
                        
                        //logger.error(mSearch.getSearchID() + " SolveHTMLResponse: parsam nume fisier");
                        // daca suntem pe checkForDocType, nu se mai reface structura de directoare, 
                        // deoarece avem certitudinea ca exista un fisier anterior salvat in structura aceasta.
                        overwritten = overwritten || checkOverWritten();
                        
                        if (this instanceof TSServerDASLAdapter || this instanceof TNDavidsonRO) {
                        	//TODO: this if will be removed when the FileAlreadyExist function will be deleted 
							sTmpFilePath = GetFilePath(false);	
						} else {
							sTmpFilePath = GetFilePath(!Response.isCheckForDocType());
						}
                        
                        if( iteratorType == ModuleStatesIterator.TYPE_OCR ||
                        	iteratorType == ModuleStatesIterator.TYPE_OCR_FULL_OR_BOOTSTRAPER){
                        	//ocr document
                        	
                        	//add it to list to identify it when constructing description column
                        	mSearch.addOCRDoc(sTmpFilePath);
                        	updateOcrCrossRefWithSources(Response);
                        	
                        	//add it to the crossrefset of the last transfer
//                        	try{
//                        		
//                        		Vector saleDataSetVector = (Vector)Response.getParsedResponse().infVectorSets.get("SaleDataSet");
//                        		
//                        		if(saleDataSetVector != null && !saleDataSetVector.isEmpty()) {
//                        		
//	                        		String book = ((SaleDataSet)(saleDataSetVector).elementAt(0)).getAtribute( "Book" );
//	                        		String page = ((SaleDataSet)(saleDataSetVector).elementAt(0)).getAtribute( "Page" );
//	                        		String instrument = ((SaleDataSet)(saleDataSetVector).elementAt(0)).getAtribute( "InstrumentNumber" );
//	                        		
//	                        		CrossRefSet crs = new CrossRefSet();
//	                        		crs.setAtribute( "Book" , book);
//	                        		crs.setAtribute( "Page" , page);
//	                        		crs.setAtribute( "InstrumentNumber" , instrument);
//	                        		
//	                        		//get data from last transfer
//	                        		ParsedResponse lastTransferParsedResponse = mSearch.getLastTransferAsParsedResponse(false, false);
//	                        		
//	                        		//add current document to crossrefset of the last transfer
//	                        		Vector vectorCrossref = ((Vector)lastTransferParsedResponse.infVectorSets.get("CrossRefSet"));
//	                        		vectorCrossref.add( crs );
//                        		}
//                        	}
//                        	catch( Exception e ){
//                        		e.printStackTrace();
//                        	}
                        }
                                                
                        mSearch.addMiscDoc(sTmpFilePath, HashCountyToIndex.getDateSiteForMIServerID(getCommunityId(), miServerID).getSiteTypeAbrev());
                        
                         /**
                         * Save the instrument number / book-page in the list of saved instruments,
                         * to be used by the RejectAlreadyPresentFilterResponse TYPE_REJECT_ALREADY_PRESENT
                         */
                        
                        if(!sTmpFilePath.contains("Tax") && !sTmpFilePath.contains("Assessor") && !msServerID.endsWith("9")){                        	
                        	Vector<SaleDataSet> sales = (Vector<SaleDataSet>) Response.getParsedResponse().infVectorSets.get("SaleDataSet");                        	
                        	if(sales != null) for(SaleDataSet sds: sales){                        		
                        		String inst = sds.getAtribute("InstrumentNumber");
                        		String book = sds.getAtribute("Book");
                        		String page = sds.getAtribute("Page"); 
                        		String date = sds.getAtribute("RecordedDate");
                        		date = date != null ? date : "";
                        		String year = StringUtils.extractParameter(date, "(\\d{4})$");
                        		Set<String> derivs = createDerivs(inst, book, page, year);
                        		for(String deriv: derivs){
                        			mSearch.addSavedInst(deriv);
                        		}
                        	}
                        }
                        
                        //logger.error(mSearch.getSearchID() + " SolveHTMLResponse: inainte de boostrap");
                        if(overwritten){
                        	try {
                        		String imageLink = sTmpFilePath.substring(0,sTmpFilePath.lastIndexOf(".")+1) + "tiff";
                        		File imageFile = new File(imageLink);
                        		if(imageFile.exists())
                        			imageFile.delete();
                        	} catch (Exception e) {
								e.printStackTrace();
							}
                        }                        
                        
						Boolean skipBootstrap = false;

						Object skipBootstrapAttr = Response.getParsedResponse().getAttribute(ParsedResponse.SKIP_BOOTSTRAP);
						if (skipBootstrapAttr != null && skipBootstrapAttr instanceof Boolean) {
							skipBootstrap = (Boolean) skipBootstrapAttr;
						}

						if (!skipBootstrap) {
							ServletServerComm.bootstrap(mSearch.getSa(), Response, getServerID());
						}
						   
//				        synchronized( mSearch.getRegisterMap() )
//				        {
//				            mSearch.getRegisterMap().put(sTmpFilePath, (ParsedResponseData) Response.getParsedResponse());
//				        }
				        
				        ADD_DOCUMENT_RESULT_TYPES saveResult = addDocumentInATS( Response, sTSDHtml );
				        if(Boolean.TRUE.equals(GetAttribute("DO_NOT_REALY_SAVE_THE_DOCUMENT"))) {
		    				//do not log
		    			} else if(saveResult.equals(ADD_DOCUMENT_RESULT_TYPES.ADDED)) {
				        	SearchLogger.info("<span class='saved'>Document saved.</span><br/>", searchId);	
				        } else if(saveResult.equals(ADD_DOCUMENT_RESULT_TYPES.OVERWRITTEN)) {
				        	SearchLogger.info("<span class='overwrite'>Document overwritten.</span><br/>", searchId);
				        } else if(saveResult.equals(ADD_DOCUMENT_RESULT_TYPES.ALREADY_EXISTS)) {
				        	SearchLogger.info("<span class='saved'>Document was already saved.</span><br/>", searchId);
				        } else if(saveResult.equals(ADD_DOCUMENT_RESULT_TYPES.MERGED)) {
				        	SearchLogger.info("<span class='saved'>Document was already saved, but new info was found and added to the already saved doc.</span><br/>", searchId);	
				        } else if(saveResult.equals(ADD_DOCUMENT_RESULT_TYPES.DONE_BUT_WITH_ERRORS)) {
				        	SearchLogger.info("<span class='saved'>Document was saved but with possible errors. Please check TSR Index.</span><br/>", searchId);
				        } else if(saveResult.equals(ADD_DOCUMENT_RESULT_TYPES.ERROR)) {
				        	SearchLogger.info("<span class='error'>There was an error saving the document.Please check TSR Index.</span><br/>", searchId);
				        } else if(saveResult.equals(ADD_DOCUMENT_RESULT_TYPES.UNDEFINED)) {
				        	SearchLogger.info("<span class='error'>There was a problem saving the document.Please check TSR Index.</span><br/>", searchId);
				        }
                        
                        Response.getParsedResponse().setOnlyResponse(msSaveToTSDResponce);
                        
                        addAdditionalDocuments(Response.getParsedResponse().getDocument(), Response);
                        
                    } catch (Exception e) {
                        logger.error("ERROR when saving file: ", e);
                        e.printStackTrace();
                        Response.getParsedResponse().setError(
                                "Error: " + e.getMessage()
                                        + " received by class: "
                                        + getDataSite().getName() + " in function: "
                                        + rsFunctionName + viParseID);
                        throw new ServerResponseException(Response);
                    }
                    
					ASThread currentThread = ASMaster.getSearch(mSearch.getSearchID());

					if (currentThread != null) {
						synchronized (currentThread.statusNotifier) {
							currentThread.statusNotifier.setNewChaptersAdded();
							currentThread.statusNotifier.notifyAll();
						}
					}
					
                } else {
                	
                    IndividualLogger.info( "SolveHTMLResponse: NOT VALID DOC",searchId );
                	logger.error(mSearch.getSearchID() + " SolveHTMLResponse: NOT VALID DOC");
                    Response.setError(ServerResponse.NOT_VALID_DOC_ERROR);
                    Response.getParsedResponse().setError(INVALIDATED_RESULT);
                    performAdditionalProcessingWhenInvalidatingDocument(Response);
//                    SearchLogger.info("<span class='notsaved'>Document not saved.</span><br/>", searchId);
                    
                    saveInvalidatedDocumentForRestoration(null, Response, true);
                    
                }
            	
//            	SearchLogger.info("</div>", searchId);
//            	String res = valid ? "_passed" : "_rejected";
//            	String style = valid ? "none" : "";
            	
            	StringBuilder toLogEnd = new StringBuilder();
            	if(!valid) {
            		toLogEnd.append("<span class='notsaved'>Document not saved.</span><br/>");
            		toLogEnd.append("</div><script language=\"JavaScript\">var dv2=document.getElementById(\"").append(id).append("\"); dv2.id += \"_rejected\"; dv2.style.display=\"\";</script>");
            	} else {
            		toLogEnd.append("</div><script language=\"JavaScript\">var dv2=document.getElementById(\"").append(id).append("\"); dv2.id += \"_passed\"; dv2.style.display=\"none\";</script>");
            	}
            	
//           		SearchLogger.info("</div><script language=\"JavaScript\">var dv2=document.getElementById(\"" + id + "\"); dv2.id += \"" + res + "\"; dv2.style.display=\"" + style + "\";</script>", searchId);
            	SearchLogger.info(toLogEnd.toString(), searchId);
            }
                        
        } catch (ServerResponseException sre) {
            if (sre.getServerResponse().getParsedResponse().isWarning()) {
                //should be ignored, and proccessing must continue
            } else {//otherwise is a true errror
                throw sre;
            }
        }

    }

	protected String getExtraToShowWhenProcessingDetails(ServerResponse response) {
		return "";
	}

	protected void addSpecificValidatedLink(ServerResponse response) {
		// TODO Auto-generated method stub
		
	}

	protected String createCustomDescriptionPart(){
    	return "";
    }
    
    
    
    protected void calculateAndSetDescription(DocumentI doc ){
    	String state =InstanceManager.getManager().getCurrentInstance(searchId).getCurrentState().getStateAbv();
    	String county=InstanceManager.getManager().getCurrentInstance(searchId).getCurrentCounty().getName();
    	String siteName = getCrtTSServerName(miServerID);
    	if("ILCook".equalsIgnoreCase(state+county)) {
    		com.stewart.ats.base.document.AddDescrRemarks.calculateAndSetDescriptionILCook(searchId, doc);
    	}
    	else if ("ILKaneRO".equalsIgnoreCase(siteName)){
	    	String description = doc.updateDescription(5);
	    	String aditionalDescription = createCustomDescriptionPart();
	    	if(!StringUtils.isEmpty(aditionalDescription)){
	    		description+="\n"+aditionalDescription;
	        	doc.setDescription(description);
	    	}
    	} else {
    		String description = doc.updateDescription();
	    	String aditionalDescription = createCustomDescriptionPart();
	    	if(!StringUtils.isEmpty(aditionalDescription)){
	    		description+="\n"+aditionalDescription;
	        	doc.setDescription(description);
	    	}
    	}
    }
    
    public static void calculateAndSetFreeForm(DocumentI doc, SimpleChapterUtils.PType ptype, long searchId ){
    	calculateAndSetFreeForm(doc,ptype, searchId, null);
    }
    
    public static void calculateAndSetFreeForm(DocumentI doc, SimpleChapterUtils.PType ptype, long searchId, UserAttributes currentAgent){
    	String stateAbv = InstanceManager.getManager().getCurrentInstance(searchId).getCurrentState().getStateAbv();
    	String c = InstanceManager.getManager().getCurrentInstance(searchId).getCurrentCounty().getName();
    	Search s = InstanceManager.getManager().getCurrentInstance(searchId).getCrtSearchContext();
    	int commId = s.getCommId();
    	 
    	try {
	    	if (doc.getType() == DType.CITYTAX){
	    		String city = "";
	    		for(PropertyI prop:doc.getProperties()){
	    			if(!StringUtils.isEmpty(prop.getAddress().getCity())) {
	    				city = prop.getAddress().getCity();
	    				break;
	    			}
				}
	    		if(StringUtils.isEmpty(city)){
	    			city = HashCountyToIndex.getCityName(commId,  stateAbv, c, doc.getDataSource());
	    		} 
	    		if(StringUtils.isEmpty(city)) {
	    			// the case when city is already set - see Task 7697
	    			String currentGrantee = doc.getGranteeFreeForm();
	    			if(currentGrantee.matches("(?is)City of .*")) {
	    				city = currentGrantee.replaceFirst("(?is)City of ", "").trim();
	    			}
	    		}
	    		
	    		c = StringUtils.isEmpty(city) ? "" : city;
	    	} 
    	}catch(Exception e) {
    		e.printStackTrace();
    	}
    	
    	NameFormaterI nf = MyAtsAttributes.getNameFormatterForSearch(searchId, currentAgent);
    	doc.updatePartyFreeForm(ptype, c, "and", nf);
    	if(ptype ==  PType.GRANTEE) {
    		doc.clearFieldModified(Fields.GRANTEE);
    	} else {
    		doc.clearFieldModified(Fields.GRANTOR);
    	}
    }
    
    protected TSServer.ADD_DOCUMENT_RESULT_TYPES addDocumentInATS(ServerResponse response, String htmlContent) {
    	return addDocumentInATS(response, htmlContent, false);
    }
    
    
	@Override
	@SuppressWarnings("deprecation")
	public TSServer.ADD_DOCUMENT_RESULT_TYPES addDocumentInATS(DocumentI doc,  boolean forceOverritten){
    	ServerResponse response = new ServerResponse();
    	ParsedResponse pr = new ParsedResponse();
    	pr.setDocument(doc);
    	
    	if(doc.getImage()!=null){
	    	ImageLinkInPage ilip = ImageTransformation.imageToImageLinkInPage(doc.getImage());
	    	ilip.setImageFileName( doc.getImage().getFileName() );
	    	pr.addImageLink(ilip);
    	}
    	
    	response.setParsedResponse(pr);
    	
    	return addDocumentInATS(response, doc.asHtml(), forceOverritten);
    }
    
	protected boolean fakeDocumentAlreadyExists(ServerResponse response, String htmlContent, boolean forceOverritten, DocumentsManagerI manager, DocumentI doc){
		if((doc.isFake()||"MISCELLANEOUS".equals(doc.getDocType())) && manager.flexibleContains(doc)){
			return true;
		}
		
		return false;
	}
	
	protected DocumentI getAlreadySavedDocument(DocumentsManagerI manager, DocumentI doc) {
		if(manager.contains(doc)) {
			return doc;
		}
		
		return null;
	}
	
    protected TSServer.ADD_DOCUMENT_RESULT_TYPES addDocumentInATS(ServerResponse response, String htmlContent, boolean forceOverritten){
    	ParsedResponse pr = response.getParsedResponse();
    	Search search=InstanceManager.getManager().getCurrentInstance( searchId ).getCrtSearchContext();
    	GBManager gbm=null;
    	DocumentsManagerI manager = mSearch.getDocManager();
    	boolean resaveDocument = Boolean.TRUE.equals(mSearch.getAdditionalInfo("RESAVE_DOCUMENT"));
    	DocumentI alreadySavedDocument = null;
    	DocumentI doc = pr.getDocument() ;
    	ADD_DOCUMENT_RESULT_TYPES saveStatus = ADD_DOCUMENT_RESULT_TYPES.UNDEFINED;
    	htmlContent = cleanHtmlBeforeSavingDocument(htmlContent);
        try{
        	manager.getAccess();
        	
        	if ( doc!=null ){
        		alreadySavedDocument = getAlreadySavedDocument(manager, doc);
            	
            	if(mSearch.getSa().isDateDown() &&  doc.isOneOf(DType.ASSESOR,DType.CITYTAX, DType.TAX )) {
            		resaveDocument = true;
            	}
            	
        		if( !resaveDocument && !forceOverritten && alreadySavedDocument != null){
        			if (alreadySavedDocument instanceof RegisterDocumentI && doc instanceof RegisterDocumentI){
	        			RegisterDocumentI docFound = (RegisterDocumentI) alreadySavedDocument;
	        			RegisterDocumentI docToCheck = (RegisterDocumentI) doc;
	        			
	        			boolean addedNewInfo = docToCheck.mergeDocumentsInformation(docFound, searchId, true, false);
	        			if (addedNewInfo){
	        				return ADD_DOCUMENT_RESULT_TYPES.MERGED;
	        			}
        			}
        			
        			return ADD_DOCUMENT_RESULT_TYPES.ALREADY_EXISTS;
        		}
        		
        		processInstrumentBeforeAdd(doc);
        		
        		if(fakeDocumentAlreadyExists(response, htmlContent, forceOverritten, manager, doc)){
        			return ADD_DOCUMENT_RESULT_TYPES.ALREADY_EXISTS;
        		}
        		
            	doc.setChecked(true);
            	doc.setIncludeImage(true);
            	if(resaveDocument || forceOverritten) {
            		manager.remove(alreadySavedDocument);
            		if(alreadySavedDocument != null) {
            			saveStatus = ADD_DOCUMENT_RESULT_TYPES.OVERWRITTEN;
            		}
            	}
            	
    			if(mSearch.getSa().isDateDown()  &&  doc.isOneOf(DType.ASSESOR,DType.CITYTAX, DType.TAX )) {
    				for(DocumentI existingDoc : manager.getDocumentsWithType(doc.getType())) {
    					existingDoc.setChecked(false);
    					existingDoc.setIncludeImage(false);
    				}
    			}
    			if(Boolean.TRUE.equals(GetAttribute("DO_NOT_REALY_SAVE_THE_DOCUMENT"))) {
    				//do not add
    			} else {
    				
//    				County county = County.getCounty(new BigDecimal(search.getSa().getAtribute(SearchAttributes.P_COUNTY)));
//    				DocTypeNode subcategoryNode = DocTypeNode.getSubcategory(county, doc.getServerDocType());
//    		    	
//    		    	if(subcategoryNode != null){
//    		    		subcategoryNode.fillDocument(doc);
//    		    	} 
    				
    				manager.add( doc );
    			}
    			
            	//if we overritten the document, keep that info se we know
            	if(saveStatus.equals(ADD_DOCUMENT_RESULT_TYPES.UNDEFINED)) {
            		saveStatus = ADD_DOCUMENT_RESULT_TYPES.DONE_BUT_WITH_ERRORS;
            	}
            	
            	// Task 8149 - for ATS docs we save the template contents as doc index
            	// to allow further modifications when doc index is opened in TSR Index
            	if(!"ATS".equalsIgnoreCase(doc.getDataSource())) {
            		doc.setIndexId( DBManager.addDocumentIndex(specificClean(Tidy.tidyParse(htmlContent, null)), mSearch ) );
            	}
            	
            	if("SF".equalsIgnoreCase(doc.getDataSource())) {
	            	if(doc instanceof SSFPriorFileDocument){
	            		TxtStarterAndIndexType starter = (TxtStarterAndIndexType)response.getParsedResponse().getAttribute(ParsedResponse.SSF_CONTENT);
	            		
	            		if (starter!=null) {
	            			SSFPriorFileDocument ssfDoc = (SSFPriorFileDocument)doc;
	                		ssfDoc.setSsfIdexId(DBManager.addSfDocumentIndex(starter.getTxtStarter(), mSearch));
	                		ssfDoc.setOriginalId(starter.getDocInfo().getId());
	                		ssfDoc.setOriginalRandId(starter.getDocInfo().getRandId());
	            		}
	            	}
            	}
            	
            	//task 8114
            	if("TN".equals(mSearch.getSa().getAtribute(SearchAttributes.P_STATE_ABREV)) && doc.getType() == DType.ROLIKE){
            		doc.setServerDocType(doc.getDocSubType());
            	}
            	
            	addDocumentAdditionalProcessing(doc,response);
            	Set<String> list = new HashSet<String>();
            	boolean hasImage = false;
            	try{
            		int count = pr.getImageLinksCount();
            		for(int j=0;j<count;j++){
            			list.add(pr.getImageLink(j).getLink());
            			hasImage  = true;
            		}
            	}
            	catch(Exception e){}
            	
            	if (doc.getProperties().size() > ServerConfig.getMaxNumberOfPropertiesAllowed()){
            		doc.setProperties(new LinkedHashSet<PropertyI>());
            		doc.setDescription(TOO_MANY_PROP);
            	} else if (!TOO_MANY_PROP.equalsIgnoreCase(doc.getDescription())){
            		calculateAndSetDescription(doc);
            	}
            	if(!doc.isFieldModified(Fields.GRANTOR)) {
            		TSServer.calculateAndSetFreeForm(doc,PType.GRANTOR, searchId);
            	}
            	if(!doc.isFieldModified(Fields.GRANTEE)) {
            		TSServer.calculateAndSetFreeForm(doc,PType.GRANTEE, searchId);
            	}
            	
            	if (search.getSearchType()==Search.GO_BACK_ONE_LEVEL_SEARCH){
           		 	gbm=(GBManager)search.getSa().getObjectAtribute(SearchAttributes.GB_MANAGER_OBJECT);
           		 	gbm.addGbDocsEvidence(doc.getId());
            	}
             	
             	if(!(this instanceof ILCookLA)) {
//	             	try {
//	             		
//	             		if (mSearch.getSearchIterator() != null) { //automatic
//	            			try{
//	            				TSServerInfoModule seedModule = mSearch.getSearchIterator().getInitial();
//	            				
//	            				if(seedModule != null) {
//		            				String searchType = seedModule.getSearchType();
//		            				if(StringUtils.isEmpty(searchType)) {
//		            					doc.setSearchType(TSServerInfo.getDefaultSearchTypeForModule(seedModule.getModuleIdx(), doc));
//		            				} else {
//		            					doc.setSearchType(SearchType.valueOf(searchType));
//		            				}
//	            				}
//	            				
//	            			}catch( Exception e ){
//	            				logger.error("Error getting when extracting SearchType module on searchId: " + searchId, e);
//	            			}
//	            		} else if(doc.getSearchType().equals(DocumentI.SearchType.NA)){	// do not change searchtype if doc already has one ...             		
//							int moduleIdx = mSearch.getSearchRecord().getModuleID();
//							String searchType = getDefaultServerInfo().getModule(moduleIdx).getSearchType();
//							if ("".equals(searchType)) {
//								doc.setSearchType(TSServerInfo.getDefaultSearchTypeForModule(moduleIdx, doc));
//							} else {
//								doc.setSearchType(SearchType.valueOf(searchType));
//							}
//						
//	            		}
//					} catch (Exception e) {
//						logger.error("Error getting when extracting SearchType module on searchId: " + searchId);
//					}
             		getDocumentSearchType(doc, true);
             	}
             	
             	if (isOcrAdded()){
             		if(doc instanceof RegisterDocumentI){
             			((RegisterDocumentI) doc).setSearchType(SearchType.IP);
             		}
             	}
            	

            	if(hasImage){
            		String extension = "";
            		String oldImageFileName = pr.getImageLink(0).getImageFileName();
            		int poz = oldImageFileName .lastIndexOf(".");
            		if(poz>0 && (poz+1 < oldImageFileName.length()) ){
            			extension = oldImageFileName.substring(poz+1);
            		}
            		if(extension.equalsIgnoreCase("tif")){
            			extension = "tiff";
            		}
            		String basePath = getImagePath();
            		File file= new File(basePath);
            		if(!file.exists()){
            			file.mkdirs();
            		}
            		
                	String fileName = doc.getId()+"."+extension;
                	String path 	= basePath+File.separator+fileName;
                	
	            	ImageI image = new Image();
	            	image.setLinks( list );
	            	image.setExtension( extension );
	            	if("tiff".equalsIgnoreCase(extension)){
	            		image.setContentType("image/tiff");
	            	}
	            	else if("pdf".equalsIgnoreCase(extension)){
	            		image.setContentType("application/pdf");
	            	}
	            	image.setFileName(fileName);
	            	image.setPath(path);
	            	image.setSaved(false);
	            	image.setUploaded(false);
	            	doc.setImage(image);
	            	doc.setIncludeImage(true);
            	}
            	else{
            		if(!doc.hasImage()) {
            			doc.setIncludeImage(false);
            		}
            	}
            	if(Boolean.TRUE.equals(GetAttribute("DO_NOT_REALY_SAVE_THE_DOCUMENT"))) {
    				//do not log
    			} else {
	            	if(Boolean.TRUE.equals(GetAttribute("TSRIE_Restore"))) {
	            		TsdIndexPageServer.logAction("TSRIE add ", searchId, doc);
	            	} else if(pr.isParentSite()){
		                TsdIndexPageServer.logAction("Parent site add ", searchId, doc);
	            	}
    			}
            	
        	}
        	
        	addDocumentAdditionalPostProcessing(doc,response);
        	
        	if(isParentSite()){
            	mSearch.applyQARules();
            }
        	
        	//if we added for the first time the document, 
        	//we mark that we've reached the end of the saving part and we are succesfull
        	if(saveStatus.equals(ADD_DOCUMENT_RESULT_TYPES.DONE_BUT_WITH_ERRORS)) {
        		saveStatus = ADD_DOCUMENT_RESULT_TYPES.ADDED;
        	}
        } catch(Exception e){  
        	e.printStackTrace(); 
        } finally{
        	manager.releaseAccess();
        }
        if(saveStatus.equals(ADD_DOCUMENT_RESULT_TYPES.UNDEFINED)) {
        	saveStatus = ADD_DOCUMENT_RESULT_TYPES.ERROR;
        }
        return saveStatus;
        
	}

    public String specificClean(String htmlContent){
    	return htmlContent;
    }

	public final String getImageDirectory() {
		SimpleDateFormat format = new SimpleDateFormat("yyyy_MM_dd");
		Date sdate =mSearch.getStartDate();
		String basePath = ServerConfig.getImageDirectory()+File.separator+format.format(sdate);
		return basePath;
	}
    
    public final String getImagePath() {
		SimpleDateFormat format = new SimpleDateFormat("yyyy_MM_dd");
		Date sdate =mSearch.getStartDate();
		String basePath = ServerConfig.getImageDirectory()+File.separator+format.format(sdate)+File.separator+searchId;
		return basePath;
	}

    
    /**
     * Create instr derrivations
     * @param inst
     * @param book
     * @param page
     * @param year
     * @return
     */
    private static Set<String> createDerivs(String inst, String book, String page, String year){
    	Set<String> derivs = new HashSet<String>();
		if(!StringUtils.isEmpty(year) && !StringUtils.isEmpty(inst)){
			String inst1 = inst.replaceAll("^0+", "");
			derivs.add("year=" + year + ";inst=" + inst);			
			derivs.add("year=" + year + ";inst=" + inst1);
		}
		if(!StringUtils.isEmpty(book) && !StringUtils.isEmpty(page)){			
			String book1 = book.replaceAll("^0+", "");
			String page1 = page.replaceAll("^0+", "");
			derivs.add(book + "_" + page);
			derivs.add(book + "_" + page1);
			derivs.add(book1 + "_" + page);
			derivs.add(book1 + "_" + page1);
	    }                        		
		if(!StringUtils.isEmpty(inst)){
			String inst1 = inst.replaceAll("^0+", "");
			derivs.add(inst);
			derivs.add(inst1);
		}                        		
    	return derivs;
    }

    protected boolean validFakeInstrumentDate(String filledDate) {
        String tsu = mSearch.getSa().getAtribute(SearchAttributes.SEARCHUPDATE);
        if (tsu != null && tsu.trim().toLowerCase().equals("true")
                && !StringUtils.isStringBlank(filledDate)) {
            ServerResponse sr = new ServerResponse();
            SaleDataSet sds = new SaleDataSet();
            sds.setAtribute("RecordedDate", filledDate);
            sr.getParsedResponse().addSaleDataSet(sds);
            if( docsValidators != null && docsValidators.size() > 0 ){
            	return isDocumentValid( sr );
            }
            else{
                return docsValidator.isValid(sr);
            }
        }
        return true;
    }

    protected boolean isFakeDocAlreadyProcessed(String fNameTSD,
            Instrument instr) {
        onBeforeAlreadyProcessCheck( fNameTSD );
        if (instr.isOverwrite()) {
            if (FileAlreadyExist(fNameTSD)) {
                return true;
            } else {
                return false;
            }
        }
        String fName = getServerTypeDirectory() + fNameTSD;
        String prefix = null, suffix = null;
        boolean checkPrefix = false;
        if(fNameTSD.indexOf(".") > 0) {
        	checkPrefix = true;
        	prefix = fNameTSD.substring(0,fNameTSD.indexOf("."));
        	suffix = fNameTSD.substring(fNameTSD.indexOf("."));
        }
        if (mSearch.containsVisitedDoc(fName) || FileAlreadyExist(fNameTSD) || 
        		checkPrefix?samePrefixFileAlreadyExist(mSearch.getSearchDir() + getServerTypeDirectory(), prefix, suffix):false) {
            return true;
        }
        return false;
    }

    protected long serverTimeout = 900000;// default 15 min
    
    protected DownloadImageResult saveImage(ImageI image) throws ServerResponseException {
    	return null;
    }
    
    public DownloadImageResult saveImage(ImageI image, DocumentI doc) throws ServerResponseException {
    	return saveImage(image);
    }
    
    protected DownloadImageResult searchForImage(DocumentI doc) throws ServerResponseException {
    	return null;
    }
    
    protected DownloadImageResult saveImage(ImageLinkInPage image) throws ServerResponseException {
    	getSearch();
        //synchronized (msSaveToTSDFileName) {
            
//	        String msSaveToTSDFileNameTemp = msSaveToTSDFileName;
	        
//	        msSaveToTSDFileName = image.getImageFileName();
	        
	        logger.debug(" SAVE IMAGE: " + image.getImageFileName());
	        ServerResponse res = FollowLink(image.getLink().replaceAll(" ", "%20"), image.getPath());
//	        FollowLink("/title-search/URLConnectionReader?p1=079&p2=5&searchId=4479192&ActionType=2&Link=https://prod-specialalertsupload.stewart.com/Uploads/SA2010027.pdf", image.getPath())
//	        msSaveToTSDFileName = msSaveToTSDFileNameTemp;
	        if (FileUtils.existPath(image.getPath())) {
	        	image.setSolved(true);						
	        	image.setDownloadStatus("DOWNLOAD_OK");
			}
	        
	        return res.getImageResult();
        //}
    }

    protected String GetRequestSettings(boolean vbEncoded, String rsQueryString) {
        String sName = "";
        String sValue = "";
        String sAction;
        String sTmp;
        int iTmp;
        
        boolean disableEncoding = false;
        
        //get ACTION TYPE
        sTmp = rsQueryString.substring(rsQueryString.indexOf(ACTION_TYPE_LINK)
                + ACTION_TYPE_LINK.length() + 1);
        
		// we have added "&disableEncoding=true" in order to instruct the TSServer.GetRequestSettings() and TSConnectionURL.BuildQuery() 
		// not to escape the parameters. this is because the query is custom-made by a javascript and any encoding makes it inutilizable
		// by the document server
        if(sTmp.indexOf("&disableEncoding=true")!=-1){
        	sTmp = sTmp.replace("&disableEncoding=true", "");
        	disableEncoding = true;
        }
        
        iTmp = sTmp.indexOf('&');
        if (iTmp != -1) {
            miGetLinkActionType = Integer.parseInt(sTmp.substring(0, iTmp));
            sTmp = sTmp.substring(iTmp + 1);
        } else {
            miGetLinkActionType = Integer.parseInt(sTmp);
            sTmp = "";
        }
        //get action
        sTmp = sTmp.substring(sTmp.indexOf(msPrmNameLink)
                + msPrmNameLink.length() + 1);
        //logger.debug("GetRequestSettings link= [" + sTmp +"]");
        iTmp = sTmp.indexOf('&');
        if (iTmp != -1) {
            sAction = sTmp.substring(0, iTmp);
            sTmp = sTmp.substring(iTmp + 1);
        } else {
            sAction = sTmp;
            sTmp = "";
        }
        if (sAction.length() != 0)
            if (!sAction.substring(0, 1).equals("/") && !sAction.startsWith("http://"))
                sAction = "/" + sAction;
        //reset request
        getTSConnection().BuildQuery(null, null, true);
        if(sTmp.contains("&useRawLink=true")){ 
        	getTSConnection().setQuery(sTmp.replace("&useRawLink=true", ""));
        } else {
	        while (sTmp.length() != 0) {
	            //get name
	            iTmp = sTmp.indexOf('=');
	            if (iTmp != -1) { //is 'name='
	                sName = sTmp.substring(0, iTmp);
	                //step over =
	                sTmp = sTmp.substring(iTmp + 1);
	                //get value
	                iTmp = sTmp.indexOf('&');
	                if (iTmp != -1) { //is 'value&'
	                    sValue = sTmp.substring(0, iTmp);
	                    sTmp = sTmp.substring(iTmp + 1);
	                } else {
	                    //is 'value'
	                    sValue = sTmp;
	                    sTmp = "";
	                }
	            } else {
	                //get name
	                iTmp = sTmp.indexOf('&');
	                if (iTmp != -1) {
	                    //is 'name&'
	                    sName = sTmp.substring(0, iTmp);
	                    sTmp = sTmp.substring(iTmp + 2);
	                } else {
	                    //is 'name'
	                    sName = sTmp;
	                    sTmp = "";
	                }
	            }
	            /*
	             * if (sName.length() ==0) sName=null; if (sValue.length() ==0)
	             * sValue=null;
	             */
	            //add found parameter
	            if (vbEncoded) {
	                sName = sName.replaceAll(AND_REPLACER, "&");
	                sValue = sValue.replaceAll(AND_REPLACER, "&");
	                try {
	                    sName = URLDecoder.decode(sName, "UTF-8");
	                    if(!disableEncoding){
	                    	sValue = URLDecoder.decode(sValue, "UTF-8");
	                    }else{
	                    	sValue = sValue.replaceAll(" ","%20");
	                    }
	                } catch (UnsupportedEncodingException e) {}
	                catch (IllegalArgumentException e) {}
	            }
	            getTSConnection().BuildQuery(sName, sValue, false, disableEncoding);
	        }
        }
        return sAction;
    }

    protected List GetRequestSettings(String rsForm, boolean bGetAsLink) {
        String sFormUpercase = rsForm.toUpperCase(); //only for searching never
                                                     // get valuse from this
                                                     // string
        List submits = new ArrayList();
        List links = new ArrayList();

        //get action
        String sAction = GetValue(rsForm, "ACTION=");
        if (sAction.length() != 0) {
            while (sAction.substring(0, 3).equals("../"))
                sAction = sAction.substring(3);
            if (!sAction.substring(0, 1).equals("/"))
                sAction = "/" + sAction;
        }

        if (!bGetAsLink) //reset request parameters
            getTSConnection().BuildQuery(null, null, true);

        int iTmp = sFormUpercase.indexOf("<INPUT ");
        while (iTmp > -1) {
            //get Input tag
            String sInput = rsForm.substring(iTmp, sFormUpercase.indexOf(">",
                    iTmp) + 1);
            //get type
            String type = GetValue(sInput, "TYPE=");
            if (type.equalsIgnoreCase("Submit")
                    || type.equalsIgnoreCase("Image")) {
                submits.add(sInput);
            } else {
                sAction = addOneParameter(sAction, sInput, bGetAsLink);
            }

            //go to next Input tag
            iTmp = sFormUpercase.indexOf("<INPUT ", iTmp + 1);
        }
        if ((submits.size() > 1) && bGetAsLink) {
            for (Iterator iter = submits.iterator(); iter.hasNext();) {
                String submit = (String) iter.next();
                links.add(addOneParameter(sAction, submit, bGetAsLink));
            }
        } else {
            if (submits.size() > 0) {
                String submit = (String) submits.get(0);
                links.add(addOneParameter(sAction, submit, bGetAsLink));
            }
        }
        return links;
    }

    private String addOneParameter(String sAction, String sInput,
            boolean bGetAsLink) {
        String onePara = extractOneParameter(sInput, bGetAsLink);
        if (!onePara.equals("")) {
            sAction += "&" + onePara;
        }
        return sAction;
    }

    private String extractOneParameter(String sInput, boolean bGetAsLink) {
        //get Name
        String sName = GetValue(sInput, "NAME=");
        //get value
        String sValue = GetValue(sInput, "VALUE=");
        /*
         * if (sName.length() ==0) sName=null; if (sValue.length() ==0)
         * sValue=null;
         */
        //add found parameter
        if (!bGetAsLink) {
            getTSConnection().BuildQuery(sName, sValue, false);
            return "";
        } else {
            /*
             * if (sName!=null || sValue!=null) { if (sName==null) {
             * sAction+="&=" + sValue; } else if(sValue==null) { sAction+= "&" +
             * sName; } else {
             */
            return sName.replaceAll("&", AND_REPLACER) + "="
                    + sValue.replaceAll("&", AND_REPLACER);
            /*
             * } }
             */
        }
    }

    protected String createLinkFromForm(String form, String linkName) {
        //logger.debug("form = " + form);
        int iActionType = TSConnectionURL.idPOST;
        List links = GetRequestSettings(form, true);
        if (links.size() > 0) {
            return CreateLink(linkName, (String) links.get(0), iActionType);
        } else {
            return "";
        }
    }

    protected String CreateLink(String rsLinkMessage,
            String rsActionAndExtraParameters, int iActionType) {
        return "<A HREF='" + CreatePartialLink(iActionType)
                + rsActionAndExtraParameters.replaceAll("\\?", "&") + "'>"
                + rsLinkMessage + "</a>";
    }

    public String CreatePartialLink(int iActionType) {
        return msRequestSolverName + "?" + msServerID + "&"
                + RequestParams.SEARCH_ID + "=" + mSearch.getSearchID() + "&"
                + getLinkPrefix(iActionType);
    }

    protected String CreateSaveToTSDFormHeader() {
    	return CreateSaveToTSDFormHeader(URLConnectionReader.SAVE_TO_TSD_ACTION, "GET");
    }

    protected String CreateSaveToTSDFormHeader(int action, String method) {
        //need to be as GET because we pass 2 params in one
        //msserverid hidden field will have the value like p1=2&p2=3
    	String s = "<form name=\"SaveToTSD\" id=\"SaveToTSD\" action= \"" + msRequestSolverName + "\"" + " method=\"" + method + "\" > "
                + "<input type=\"hidden\" name=\"dispatcher\" value=\""+ action + "\">"
                + "<input type=\"hidden\" name=\"ServerID\" value=\"" + msServerID + "\">" 
                + "<input type=\"hidden\" name=\"" + RequestParams.SEARCH_ID + "\" value=\"" + mSearch.getSearchID() + "\"> "
                + "<input type=\"hidden\" name=\"" + RequestParams.PARENT_SITE_SAVE_TYPE + "\" id=\"" + RequestParams.PARENT_SITE_SAVE_TYPE + "\" " +
                	"value=\"" + RequestParamsValues.PARENT_SITE_SAVE_TYPE_WITHOUT_CROSSREF + "\">";
    	return s;
    }

    protected String CreateSaveToTSDFormEnd(int viParseID) {
        return CreateSaveToTSDFormEnd(null, viParseID, -1);
    }

    protected String CreateSaveToTSDFormEnd(String name, int parserId, int numberOfUnsavedRows) {
    	
    	if (name == null) {
            name = SAVE_DOCUMENT_BUTTON_LABEL;
        }
    	
    	if(isPropertyOrientedSite()){
    		if(numberOfUnsavedRows < 0 || numberOfUnsavedRows > 0) {
    			return "<input  type=\"button\" class=\"button\" name=\"Button\" value=\"" + name + "\" " 
    				+ "onclick=\"javascript:submitFormByGet();\">\r\n</form>\n" ;
    		} else {
    			return "</form>\n";
    		}
    	}
        
        String s = "";
        
        
        if( !isRoLike(miServerID, true) && (parserId == ID_DETAILS ||parserId == ID_DETAILS1 ||parserId == ID_DETAILS2 )){
        	if(numberOfUnsavedRows < 0 || numberOfUnsavedRows > 0) {
	        	s = "<input  type=\"button\" class=\"button\" name=\"Button\" value=\"" + name + "\" + onclick=\"javascript:submitFormByGet();\">\r\n" +
    	        	" <input type=\"button\" class=\"button\" value=\"" + SAVE_DOC_WITH_CROSS_REF_BUTTON_LABEL + "\" " + 
    	        	" onclick=\"javascript: if(document.getElementById('" + RequestParams.PARENT_SITE_SAVE_TYPE + "'))\r\n " + 
    	        	" document.getElementById('" + RequestParams.PARENT_SITE_SAVE_TYPE + "').value='" + RequestParamsValues.PARENT_SITE_SAVE_TYPE_WITH_CROSSREF + 
    	        	"'; javascript:submitFormByGet();\">\r\n";
        	}
            
        }  else {
        	if (numberOfUnsavedRows < 0 || numberOfUnsavedRows > 0) {
	        	s = "<input  type=\"button\" class=\"button\" name=\"Button\" value=\"" + name + "\" " +"onclick=\"javascript:submitForm();\" >\r\n" +
		        	" <input type=\"button\" class=\"button\" value=\"" + SAVE_DOC_WITH_CROSS_REF_BUTTON_LABEL + "\" " + 
		        	" onclick=\"javascript: if(document.getElementById('" + RequestParams.PARENT_SITE_SAVE_TYPE + "'))\r\n " + 
		        	" document.getElementById('" + RequestParams.PARENT_SITE_SAVE_TYPE + "').value='" + RequestParamsValues.PARENT_SITE_SAVE_TYPE_WITH_CROSSREF + 
		        	"'; " +
		        	"submitForm();" +
		        	"\">\r\n";
        	}
        }
        
        return s+"</form>\n";
    }
    
    //////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    protected static TSServerInfoModule SetModuleSearchByName(
            TSServerInfo siServerInfo, int moduleIndex, String destinationPage,
            int destinationMethod, String lNameParam, String fNameParam) {
        return SetModuleSearchByName(2, siServerInfo, moduleIndex,
                destinationPage, destinationMethod, lNameParam, fNameParam);
    }

    protected static TSServerInfoModule SetModuleSearchByName(
            int FunctionsCount, TSServerInfo siServerInfo, int moduleIndex,
            String destinationPage, int destinationMethod, String lNameParam,
            String fNameParam) {
            
        TSServerInfoModule simTmp;
        //
        simTmp = siServerInfo.ActivateModule(moduleIndex, FunctionsCount);
        simTmp.setName("Name"); //it will be displayed in jsp
        simTmp.setDestinationPage(destinationPage);
        simTmp.setRequestMethod(destinationMethod);
        simTmp.setParserID(ID_SEARCH_BY_NAME);
        { //SET EACH SEARCH PARAMETER
            //LastName
            simTmp.getFunction(0).setName("Last Name:"); //it will be displayed in jsp
            simTmp.getFunction(0).setParamName(lNameParam);
            simTmp.getFunction(0).setIteratorType(FunctionStatesIterator.ITERATOR_TYPE_LAST_NAME_FAKE);
            simTmp.getFunction(0).setSaKey(SearchAttributes.OWNER_LNAME);
            //FirstName
            simTmp.getFunction(1).setName("First Name:"); //it will be displayed in jsp
            simTmp.getFunction(1).setParamName(fNameParam);
            simTmp.getFunction(1).setIteratorType(FunctionStatesIterator.ITERATOR_TYPE_FIRST_NAME_FAKE);
            simTmp.getFunction(1).setSaKey(SearchAttributes.OWNER_FNAME);
        }
        simTmp.setSaObjKey(SearchAttributes.OWNER_OBJECT);
        
        return simTmp;
    }

    protected static TSServerInfoModule SetModuleSearchByAddress(
            TSServerInfo siServerInfo, int moduleIndex, String destinationPage,
            int destinationMethod, String NoParam, String StreeParam) {
        return SetModuleSearchByAddress(2, siServerInfo, moduleIndex,
                destinationPage, destinationMethod, NoParam, StreeParam);
    }

    protected static TSServerInfoModule SetModuleSearchByAddress(
            int FunctionsCount, TSServerInfo siServerInfo, int moduleIndex,
            String destinationPage, int destinationMethod, String NoParam,
            String StreeParam) {
        TSServerInfoModule simTmp;
        //
        simTmp = siServerInfo.ActivateModule(moduleIndex, FunctionsCount);
        simTmp.setName("Address"); //it will be displayed in jsp
        simTmp.setDestinationPage(destinationPage);
        simTmp.setRequestMethod(destinationMethod);
        simTmp.setParserID(ID_SEARCH_BY_ADDRESS);
        { //SET EACH SEARCH PARAMETER
            //Number
            simTmp.getFunction(0).setName("Street No:"); //it will be displayed
                                                         // in jsp
            simTmp.getFunction(0).setParamName(NoParam);
            simTmp.getFunction(0).setSaKey(SearchAttributes.P_STREETNO);
            simTmp.getFunction(0).setIteratorType(
                    FunctionStatesIterator.ITERATOR_TYPE_ST_N0_FAKE);
            //Street
            simTmp.getFunction(1).setName("Street_Name Street_Type:"); //it will be
                                                           // displayed in jsp
            simTmp.getFunction(1).setParamName(StreeParam);
            simTmp.getFunction(1).setSaKey(SearchAttributes.P_STREET_FULL_NAME_NO_SUFFIX);
            simTmp.getFunction(1).setIteratorType(
                    FunctionStatesIterator.ITERATOR_TYPE_ST_NAME_FAKE);
        }
        return simTmp;
    }
    
    protected static TSServerInfoModule SetModuleSearchByAddress2(
            TSServerInfo siServerInfo, int moduleIndex, String destinationPage,
            int destinationMethod, String NoParam, String StreeParam, String DirParam, String SuffixParam) {
        return SetModuleSearchByAddress2(4, siServerInfo, moduleIndex,
                destinationPage, destinationMethod, NoParam, StreeParam, DirParam, SuffixParam);
    }
    protected static TSServerInfoModule SetModuleSearchByAddress2(
            int FunctionsCount, TSServerInfo siServerInfo, int moduleIndex,
            String destinationPage, int destinationMethod, String NoParam,
            String StreeParam, String DirParam, String SuffixParam) {
        TSServerInfoModule simTmp;
        //
        simTmp = siServerInfo.ActivateModule(moduleIndex, FunctionsCount);
        simTmp.setName("Address"); //it will be displayed in jsp
        simTmp.setDestinationPage(destinationPage);
        simTmp.setRequestMethod(destinationMethod);
        simTmp.setParserID(ID_SEARCH_BY_ADDRESS);
        { //SET EACH SEARCH PARAMETER
            simTmp.getFunction(0).setName("Direction:"); //it will be
            // displayed in jsp
			simTmp.getFunction(0).setParamName(DirParam);
			simTmp.getFunction(0).setSaKey(SearchAttributes.P_STREETDIRECTION);
			
            
            //Street
            simTmp.getFunction(1).setName("Street Name:"); //it will be
                                                           // displayed in jsp
            simTmp.getFunction(1).setParamName(StreeParam);
            simTmp.getFunction(1).setSaKey(SearchAttributes.P_STREETNAME);
            simTmp.getFunction(1).setIteratorType(
                    FunctionStatesIterator.ITERATOR_TYPE_ST_NAME_FAKE);
            
            //Number
            simTmp.getFunction(2).setName("Street No:"); //it will be displayed
                                                         // in jsp
            simTmp.getFunction(2).setParamName(NoParam);
            simTmp.getFunction(2).setSaKey(SearchAttributes.P_STREETNO);
            simTmp.getFunction(2).setIteratorType(
                    FunctionStatesIterator.ITERATOR_TYPE_ST_N0_FAKE);
            
            simTmp.getFunction(3).setName("Street Type:"); //it will be
            // displayed in jsp
			simTmp.getFunction(3).setParamName(SuffixParam);
			simTmp.getFunction(3).setSaKey(SearchAttributes.P_STREETSUFIX);
			
           
        }
        return simTmp;
    }
    protected static TSServerInfoModule SetModuleSearchByParcelNo(
            TSServerInfo siServerInfo, int moduleIndex, String destinationPage,
            int destinationMethod, String NoParameter) {
        return SetModuleSearchByParcelNo(1, siServerInfo, moduleIndex,
                destinationPage, destinationMethod, NoParameter);
    }

    protected static TSServerInfoModule SetModuleSearchByParcelNo(
            int FunctionsCount, TSServerInfo siServerInfo, int moduleIndex,
            String destinationPage, int destinationMethod, String NoParameter) {
        TSServerInfoModule simTmp;
        simTmp = siServerInfo.ActivateModule(moduleIndex, FunctionsCount);
        simTmp.setName("Parcel Number"); //it will be displayed in jsp
        simTmp.setDestinationPage(destinationPage);
        simTmp.setRequestMethod(destinationMethod);
        simTmp.setParserID(ID_SEARCH_BY_PARCEL);
        { //SET EACH SEARCH PARAMETER
            //ParcelNo
            simTmp.getFunction(0).setName("Parcel No:"); //it will be displayed
                                                         // in jsp
            simTmp.getFunction(0).setParamName(NoParameter);
            simTmp.getFunction(0).setSaKey(SearchAttributes.LD_PARCELNO);
            //simTmp.getFunction(0).setIteratorType(TSServerInfoFunction.ITERATOR_TYPE_DEFAULT_NOT_EMPTY);
        }
        return simTmp;
    }
    protected static TSServerInfoModule SetModuleSearchByParcelNoExt(
            int FunctionsCount, TSServerInfo siServerInfo, int moduleIndex,
            String destinationPage, int destinationMethod, String NoParameter) {
        TSServerInfoModule simTmp;
        simTmp = siServerInfo.ActivateModule(moduleIndex, FunctionsCount);
        simTmp.setName("Parcel Number"); //it will be displayed in jsp
        simTmp.setDestinationPage(destinationPage);
        simTmp.setRequestMethod(destinationMethod);
        simTmp.setParserID(ID_SEARCH_BY_PARCEL);
        { //SET EACH SEARCH PARAMETER
            //ParcelNo
            simTmp.getFunction(0).setName("Map:"); //it will be displayed
                                                         // in jsp
            simTmp.getFunction(0).setParamName(NoParameter);
            //simTmp.getFunction(0).setSaKey(SearchAttributes.LD_PARCELNO);
            //simTmp.getFunction(0).setIteratorType(TSServerInfoFunction.ITERATOR_TYPE_DEFAULT_NOT_EMPTY);
        }
        return simTmp;
    }
    
    protected static TSServerInfoModule SetModuleSearchByCondoName(
            int FunctionsCount, TSServerInfo siServerInfo, int moduleIndex,
            String destinationPage, int destinationMethod, String NoParameter) {
        TSServerInfoModule simTmp;
        simTmp = siServerInfo.ActivateModule(moduleIndex, FunctionsCount);
        simTmp.setName("Condominium Name"); //it will be displayed in jsp
        simTmp.setDestinationPage(destinationPage);
        simTmp.setRequestMethod(destinationMethod);
        simTmp.setParserID(ID_SEARCH_BY_CONDO_NAME);
        { //SET EACH SEARCH PARAMETER
            //ParcelNo
            simTmp.getFunction(0).setName("Condominium Name:"); //it will be displayed
                                                         // in jsp
            simTmp.getFunction(0).setParamName(NoParameter);
            //simTmp.getFunction(0).setSaKey(SearchAttributes.LD_PARCELNO);
            //simTmp.getFunction(0).setIteratorType(TSServerInfoFunction.ITERATOR_TYPE_DEFAULT_NOT_EMPTY);
        }
        return simTmp;
    }


    protected static TSServerInfoModule SetModuleSearchBySectionLand(
            TSServerInfo siServerInfo, int moduleIndex, String destinationPage,
            int destinationMethod, String NoParameter) {
        return SetModuleSearchBySectionLand(1, siServerInfo, moduleIndex,
                destinationPage, destinationMethod, NoParameter);
    }

    protected static TSServerInfoModule SetModuleSearchBySectionLand(
            int FunctionsCount, TSServerInfo siServerInfo, int moduleIndex,
            String destinationPage, int destinationMethod, String NoParameter) {
        TSServerInfoModule simTmp;
        simTmp = siServerInfo.ActivateModule(moduleIndex, FunctionsCount);
        simTmp.setName("Section Land"); //it will be displayed in jsp
        simTmp.setDestinationPage(destinationPage);
        simTmp.setRequestMethod(destinationMethod);
        simTmp.setParserID(ID_SEARCH_BY_SECTION_LAND);
        { //SET EACH SEARCH PARAMETER
            //ParcelNo
            simTmp.getFunction(0).setName("Section : "); //it will be displayed
                                                         // in jsp
            simTmp.getFunction(0).setParamName(NoParameter);
            //simTmp.getFunction(0).setSaKey(SearchAttributes.LD_PARCELNO);
            //simTmp.getFunction(0).setIteratorType(TSServerInfoFunction.ITERATOR_TYPE_DEFAULT_NOT_EMPTY);
        }
        return simTmp;
    }
	
    protected static TSServerInfoModule SetModuleSearchBySurveys(
            TSServerInfo siServerInfo, int moduleIndex, String destinationPage,
            int destinationMethod, String NoParameter) {
        return SetModuleSearchBySurveys(1, siServerInfo, moduleIndex,
                destinationPage, destinationMethod, NoParameter);
    }

    protected static TSServerInfoModule SetModuleSearchBySurveys(
            int FunctionsCount, TSServerInfo siServerInfo, int moduleIndex,
            String destinationPage, int destinationMethod, String NoParameter) {
        TSServerInfoModule simTmp;
        simTmp = siServerInfo.ActivateModule(moduleIndex, FunctionsCount);
        simTmp.setName("Section Land"); //it will be displayed in jsp
        simTmp.setDestinationPage(destinationPage);
        simTmp.setRequestMethod(destinationMethod);
        simTmp.setParserID(ID_SEARCH_BY_SECTION_LAND);
        { //SET EACH SEARCH PARAMETER
            //ParcelNo
            simTmp.getFunction(0).setName("Section : "); //it will be displayed
                                                         // in jsp
            simTmp.getFunction(0).setParamName(NoParameter);
            //simTmp.getFunction(0).setSaKey(SearchAttributes.LD_PARCELNO);
            //simTmp.getFunction(0).setIteratorType(TSServerInfoFunction.ITERATOR_TYPE_DEFAULT_NOT_EMPTY);
        }
        return simTmp;
    }
	
	
    protected static TSServerInfoModule SetModuleSearchByTaxBilNo(
            TSServerInfo siServerInfo, int moduleIndex, String destinationPage,
            int destinationMethod, String NoParameter) {
        return SetModuleSearchByTaxBilNo(1, siServerInfo, moduleIndex,
                destinationPage, destinationMethod, NoParameter);
    }

    protected static TSServerInfoModule SetModuleSearchByTaxBilNo(
            int FunctionsCount, TSServerInfo siServerInfo, int moduleIndex,
            String destinationPage, int destinationMethod, String NoParameter) {
        TSServerInfoModule simTmp;
        simTmp = siServerInfo.ActivateModule(moduleIndex, FunctionsCount);
        simTmp.setName("Tax Bill Number"); //it will be displayed in jsp
        simTmp.setDestinationPage(destinationPage);
        simTmp.setRequestMethod(destinationMethod);
        simTmp.setParserID(ID_SEARCH_BY_TAX_BIL_NO);
        { //SET EACH SEARCH PARAMETER
            //ParcelNo
            simTmp.getFunction(0).setName("Bill No:"); //it will be displayed
                                                       // in jsp
            simTmp.getFunction(0).setParamName(NoParameter);
        }
        return simTmp;
    }

    protected static TSServerInfoModule SetModuleSearchByPropertyNo(
            TSServerInfo siServerInfo, int moduleIndex, String destinationPage,
            int destinationMethod, String NoParameter) {
        return SetModuleSearchByPropertyNo(1, siServerInfo, moduleIndex,
                destinationPage, destinationMethod, NoParameter);
    }

    protected static TSServerInfoModule SetModuleSearchByPropertyNo(
            int FunctionsCount, TSServerInfo siServerInfo, int moduleIndex,
            String destinationPage, int destinationMethod, String NoParameter) {
        TSServerInfoModule simTmp;
        simTmp = siServerInfo.ActivateModule(moduleIndex, FunctionsCount);
        simTmp.setName("Property number"); //it will be displayed in jsp
        simTmp.setDestinationPage(destinationPage);
        simTmp.setRequestMethod(destinationMethod);
        simTmp.setParserID(ID_SEARCH_BY_PROP_NO);
        { //SET EACH SEARCH PARAMETER
            //Prop
            simTmp.getFunction(0).setName("Property #:"); //it will be displayed
                                                       // in jsp
            simTmp.getFunction(0).setParamName(NoParameter);
        }
        return simTmp;
    }
    protected static TSServerInfoModule SetModuleSearchByInstrumentNo(
            TSServerInfo siServerInfo, int moduleIndex, String destinationPage,
            int destinationMethod, String NoParameter) {
        return SetModuleSearchByInstrumentNo(1, siServerInfo, moduleIndex,
                destinationPage, destinationMethod, NoParameter);
    }

    protected static TSServerInfoModule SetModuleSearchByInstrumentNo(
            int FunctionsCount, TSServerInfo siServerInfo, int moduleIndex,
            String destinationPage, int destinationMethod, String NoParameter) {
        TSServerInfoModule simTmp;
        simTmp = siServerInfo.ActivateModule(moduleIndex, FunctionsCount);
        simTmp.setName("Instrument Number"); //it will be displayed in jsp
        simTmp.setDestinationPage(destinationPage);
        simTmp.setRequestMethod(destinationMethod);
        simTmp.setParserID(ID_SEARCH_BY_INSTRUMENT_NO);
        { //SET EACH SEARCH PARAMETER
            //ParcelNo
            simTmp.getFunction(0).setName("Instrument No:"); //it will be
                                                             // displayed in jsp
            simTmp.getFunction(0).setParamName(NoParameter);
            simTmp.getFunction(0).setSaKey(SearchAttributes.LD_INSTRNO);
        }
        return simTmp;
    }

    protected static TSServerInfoModule SetModuleSearchBySubdivisionName(
            TSServerInfo siServerInfo, int moduleIndex, String destinationPage,
            int destinationMethod, String Name) {
        return SetModuleSearchBySubdivisionName(1, siServerInfo, moduleIndex,
                destinationPage, destinationMethod, Name);
    }

    protected static TSServerInfoModule SetModuleSearchBySubdivisionName(
            int FunctionsCount, TSServerInfo siServerInfo, int moduleIndex,
            String destinationPage, int destinationMethod, String Name) {
        TSServerInfoModule simTmp;
        simTmp = siServerInfo.ActivateModule(moduleIndex, FunctionsCount);
        simTmp.setName("Subdivision Name"); //it will be displayed in jsp
        simTmp.setDestinationPage(destinationPage);
        simTmp.setRequestMethod(destinationMethod);
        simTmp.setParserID(ID_SEARCH_BY_SUBDIVISION_NAME);
        { //SET EACH SEARCH PARAMETER
            //SUBDIVISION NAME
            simTmp.getFunction(0).setName("Subdivision Name:"); //it will be
                                                                // displayed in
                                                                // jsp
            simTmp.getFunction(0).setParamName(Name);
            simTmp.getFunction(0).setSaKey(SearchAttributes.LD_SUBDIV_NAME);
        }
        return simTmp;
    }

    protected static TSServerInfoModule SetModuleSearchBySubdivisionPlat(
            TSServerInfo siServerInfo, int moduleIndex, String destinationPage,
            int destinationMethod, String Name) {
        return SetModuleSearchBySubdivisionPlat(1, siServerInfo, moduleIndex,
                destinationPage, destinationMethod, Name);
    }

    protected static TSServerInfoModule SetModuleSearchBySubdivisionPlat(
            int FunctionsCount, TSServerInfo siServerInfo, int moduleIndex,
            String destinationPage, int destinationMethod, String Name) {
        TSServerInfoModule simTmp;
        simTmp = siServerInfo.ActivateModule(moduleIndex, FunctionsCount);
        simTmp.setName("Subdivision Plat"); //it will be displayed in jsp
        simTmp.setDestinationPage(destinationPage);
        simTmp.setRequestMethod(destinationMethod);
        simTmp.setParserID(ID_SEARCH_BY_SUBDIVISION_PLAT);
        { //SET EACH SEARCH PARAMETER
            //SUBDIVISION PLAT
            simTmp.getFunction(0).setName("Subdivision Plat:"); //it will be
                                                                // displayed in
                                                                // jsp
            simTmp.getFunction(0).setParamName(Name);
            simTmp.getFunction(0).setSaKey(SearchAttributes.LD_SUBDIV_NAME);
        }
        return simTmp;
    }

    protected static TSServerInfoModule SetModuleScannedIndexPages(
            int FunctionsCount, TSServerInfo siServerInfo, int moduleIndex,
            String destinationPage, int destinationMethod) {
        TSServerInfoModule simTmp;
        simTmp = siServerInfo.ActivateModule(moduleIndex, FunctionsCount);
        simTmp.setName("Browse Scanned Index Pages"); //it will be displayed in jsp
        simTmp.setDestinationPage(destinationPage);
        simTmp.setRequestMethod(destinationMethod);
        simTmp.setParserID(ID_BROWSE_SCANNED_INDEX_PAGES);
        // simTmp.setMouleType(TSServerInfoModule.idArchiveLinkModule);
        /*{ //SET EACH SEARCH PARAMETER
            //SUBDIVISION PLAT
            simTmp.getFunction(0).setName("Subdivision Plat:"); //it will be
                                                                // displayed in
                                                                // jsp
            simTmp.getFunction(0).setParamName(Name);
            simTmp.getFunction(0).setSaKey(SearchAttributes.LD_SUBDIV_NAME);
        }*/
        return simTmp;
    }
    
    protected static TSServerInfoModule SetModuleBackScannedPlats(
            int FunctionsCount, TSServerInfo siServerInfo, int moduleIndex,
            String destinationPage, int destinationMethod) {
        TSServerInfoModule simTmp;
        simTmp = siServerInfo.ActivateModule(moduleIndex, FunctionsCount);
        simTmp.setName("Browse Backscanned Plats"); //it will be displayed in jsp
        simTmp.setDestinationPage(destinationPage);
        simTmp.setRequestMethod(destinationMethod);
        simTmp.setParserID(ID_BROWSE_BACKSCANNED_PLATS);
 
        return simTmp;
    }
    
    protected static TSServerInfoModule SetModuleBackScannedDeeds (
            int FunctionsCount, TSServerInfo siServerInfo, int moduleIndex,
            String destinationPage, int destinationMethod) {
    	
        TSServerInfoModule simTmp;
        simTmp = siServerInfo.ActivateModule(moduleIndex, FunctionsCount);
        simTmp.setName("Browse Backscanned Deeds"); //it will be displayed in jsp
        simTmp.setDestinationPage(destinationPage);
        simTmp.setRequestMethod(destinationMethod);
        simTmp.setParserID(ID_BROWSE_BACKSCANNED_DEEDS);
 
        return simTmp;
    }
    
    protected static TSServerInfoModule SetModuleArchiveBrowsing(
            int FunctionsCount, TSServerInfo siServerInfo, int moduleIndex,
            String destinationPage, int destinationMethod) {
        TSServerInfoModule simTmp;
        simTmp = siServerInfo.ActivateModule(moduleIndex, FunctionsCount);
        simTmp.setName("Browse Archive"); //it will be displayed in jsp
        simTmp.setDestinationPage(destinationPage);
        simTmp.setRequestMethod(destinationMethod);
        simTmp.setParserID(ID_BROWSE_SCANNED_INDEX_PAGES);
 
        return simTmp;
    }
    
    protected static TSServerInfoModule SetModuleIndexArchiveBrowsing(
            int FunctionsCount, TSServerInfo siServerInfo, int moduleIndex,
            String destinationPage, int destinationMethod) {
        TSServerInfoModule simTmp;
        simTmp = siServerInfo.ActivateModule(moduleIndex, FunctionsCount);
        simTmp.setName("Browse Archive - Index Book Page Search"); //it will be displayed in jsp
        simTmp.setDestinationPage(destinationPage);
        simTmp.setRequestMethod(destinationMethod);
        simTmp.setParserID(ID_SEARCH_BY_MODULE19);
 
        return simTmp;
    }
    
    protected static TSServerInfoModule SetModuleGeneralFilter(
            TSServerInfo siServerInfo, int moduleIndex, String SortAsc,
            String SortDesc, String SortAscValue, String SortDescValue) {
        return SetModuleGeneralFilter(0, siServerInfo, moduleIndex);
    }

    protected static TSServerInfoModule SetModuleGeneralFilter(int FunctionsCount,
            TSServerInfo siServerInfo, int moduleIndex) {
        TSServerInfoModule simTmp;
        simTmp = siServerInfo.ActivateModule(moduleIndex, FunctionsCount);
        simTmp.setName("Order Results"); //it will be displayed in jsp
        simTmp.setMouleType(TSServerInfoModule.idFilterModule);
        return simTmp;
    }
    
    
    protected static TSServerInfoModule SetModuleDateFilter(
            TSServerInfo siServerInfo, int moduleIndex, String StartDate,
            String EndDate, String StartDateValue, String EndDateValue) {
        return SetModuleDateFilter(2, siServerInfo, moduleIndex, StartDate,
                EndDate, StartDateValue, EndDateValue);
    }

    protected static TSServerInfoModule SetModuleDateFilter(int FunctionsCount,
            TSServerInfo siServerInfo, int moduleIndex, String StartDate,
            String EndDate, String StartDateValue, String EndDateValue) {
        TSServerInfoModule simTmp;
        simTmp = siServerInfo.ActivateModule(moduleIndex, FunctionsCount);
        simTmp.setName("Recorded Between Dates"); //it will be displayed in jsp
        simTmp.setMouleType(TSServerInfoModule.idFilterModule);
        { //SET EACH Filter PARAMETER
            //Start Date
            simTmp.getFunction(0).setName("From:"); //it will be displayed in
                                                    // jsp
            simTmp.getFunction(0).setParamName(StartDate);
            simTmp.getFunction(0).setDefaultValue(StartDateValue);
            simTmp.getFunction(0).setParamType(TSServerInfoFunction.idDate);
            simTmp.getFunction(0).setSaKey(SearchAttributes.FROMDATE);
            //End Date
            simTmp.getFunction(1).setName("To:"); //it will be displayed in jsp
            simTmp.getFunction(1).setParamName(EndDate);
            simTmp.getFunction(1).setDefaultValue(EndDateValue);
            simTmp.getFunction(1).setParamType(TSServerInfoFunction.idDate);
            simTmp.getFunction(1).setSaKey(SearchAttributes.TODATE);
        }
        return simTmp;
    }

    protected static TSServerInfoModule SetModuleSortFilter(
            TSServerInfo siServerInfo, int moduleIndex, String SortAsc,
            String SortDesc, String SortAscValue, String SortDescValue) {
        return SetModuleSortFilter(2, siServerInfo, moduleIndex, SortAsc,
                SortDesc, SortAscValue, SortDescValue);
    }

    protected static TSServerInfoModule SetModuleSortFilter(int FunctionsCount,
            TSServerInfo siServerInfo, int moduleIndex, String SortAsc,
            String SortDesc, String SortAscValue, String SortDescValue) {
        TSServerInfoModule simTmp;
        simTmp = siServerInfo.ActivateModule(moduleIndex, FunctionsCount);
        simTmp.setName("Order Results"); //it will be displayed in jsp
        simTmp.setMouleType(TSServerInfoModule.idFilterModule);
        { //SET EACH Filter PARAMETER
            //Sort ASC
            simTmp.getFunction(0).setName("Ascending:"); //it will be displayed
                                                         // in jsp
            simTmp.getFunction(0).setParamName(SortAsc);
            simTmp.getFunction(0).setValue(SortAscValue);
            simTmp.getFunction(0).setDefaultValue("Checked");
            simTmp.getFunction(0).setParamType(
                    TSServerInfoFunction.idRadioBotton);
            //Sort DESC
            simTmp.getFunction(1).setName("Descending:"); //it will be
                                                          // displayed in jsp
            simTmp.getFunction(1).setParamName(SortDesc);
            simTmp.getFunction(1).setValue(SortDescValue);
            simTmp.getFunction(1).setParamType(
                    TSServerInfoFunction.idRadioBotton);
        }
        return simTmp;
    }

    protected static TSServerInfoModule SetModuleSearchByBookAndPage(
            TSServerInfo siServerInfo, int moduleIndex, String destinationPage,
            int destinationMethod, String BookParam, String PageParam) {
        return SetModuleSearchByBookAndPage(2, siServerInfo, moduleIndex,
                destinationPage, destinationMethod, BookParam, PageParam);
    }

    protected static TSServerInfoModule SetModuleSearchByBookAndPage(
            int FunctionsCount, TSServerInfo siServerInfo, int moduleIndex,
            String destinationPage, int destinationMethod, String BookParam,
            String PageParam) {
        TSServerInfoModule simTmp;
        //
        simTmp = siServerInfo.ActivateModule(moduleIndex, FunctionsCount);
        simTmp.setName("Book And Page"); //it will be displayed in jsp
        simTmp.setDestinationPage(destinationPage);
        simTmp.setRequestMethod(destinationMethod);
        simTmp.setParserID(ID_SEARCH_BY_BOOK_AND_PAGE);
        { //SET EACH SEARCH PARAMETER
            //LastName
            simTmp.getFunction(0).setName("Book No.:"); //it will be displayed
                                                        // in jsp
            simTmp.getFunction(0).setParamName(BookParam);
            simTmp.getFunction(0).setSaKey(SearchAttributes.LD_BOOKNO);
            //FirstName
            simTmp.getFunction(1).setName("Page No.:"); //it will be displayed
                                                        // in jsp
            simTmp.getFunction(1).setParamName(PageParam);
            simTmp.getFunction(1).setSaKey(SearchAttributes.LD_PAGENO);
        }
        return simTmp;
    }

    //////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    private String GetValue(String rsToBeSearched, String rsSearchFor) {
        char cEndChar = ' ';
        String sToBeSearchedUC = rsToBeSearched.toUpperCase(); //only for
                                                               // searching
                                                               // never get
                                                               // valuse from
                                                               // this string
        int iTmp, iTmp1, iTmp2;
        //get where begin the value of rsSearchForUC
        iTmp = sToBeSearchedUC.indexOf(rsSearchFor.toUpperCase());
        if (iTmp == -1)
            return ""; // rsSearchForUC was not found so it's value will be ""
        //pass over rsSearchForUC
        iTmp += rsSearchFor.length();
        //get first quote
        iTmp1 = sToBeSearchedUC.indexOf("'", iTmp);
        //get first dbl quote
        iTmp2 = sToBeSearchedUC.indexOf("\"", iTmp);
        //find sart&end character for the value it can be blank, quodt,
        // dblquote or greater then operator
        if (((iTmp1 < iTmp2) || (iTmp2 == -1)) && (iTmp1 != -1)) {
            //quote is first
            cEndChar = '\'';
        } else if (iTmp2 != -1) {
            //dblquote is first
            cEndChar = '\"';
            iTmp1 = iTmp2;
        }
        if (iTmp1 != -1)
            if (sToBeSearchedUC.substring(iTmp, iTmp1).trim().equals("")) //pass
                                                                          // over
                                                                          // finded
                                                                          // char
                iTmp = iTmp1 + 1;
            else
                //was a char from some were else
                cEndChar = ' ';
        else
            cEndChar = ' ';
        //find end of char
        iTmp1 = sToBeSearchedUC.indexOf(cEndChar, iTmp);
        //get first >
        iTmp2 = sToBeSearchedUC.indexOf(">", iTmp);
        if ((iTmp1 < iTmp2) && (iTmp1 != -1)) //find end char before >
            return rsToBeSearched.substring(iTmp, iTmp1);
        else
            //end char not found get everything until >
            return rsToBeSearched.substring(iTmp, iTmp2);
    }

    public String toString() {
    	if(getDataSite() != null) {
    		return getDataSite().getName();
    	}
    	return "TSServer(" + searchId + ")";
    }

    protected boolean checkOverWritten(){
    	
    	String fileName = mSearch.getSearchDir() + getServerTypeDirectory() + msSaveToTSDFileName;
    	File file = new File(fileName);
    	return file.exists() && file.length() != 0;
    }
    
    protected String GetFilePath(boolean vbCreateFile) throws IOException {
        File tmpFile = null;
        String sFile = null;
        String sDirs = null;
        String sImages = "images";
        FileOutputStream otptStrm;
        FileInputStream inptStrm=null;
        sDirs = mSearch.getSearchDir();
        if (vbCreateFile) {
            tmpFile = new File(sDirs + File.separator + sImages);
            if (!tmpFile.exists()) {
                tmpFile.mkdirs();
                tmpFile = new File(sDirs + File.separator + sImages
                        + File.separator + "pixel.gif");
                tmpFile.createNewFile();
                try {
                    inptStrm = new FileInputStream(BaseServlet.REAL_PATH.substring(0,
                    		BaseServlet.REAL_PATH.lastIndexOf(File.separator))
                            + File.separator
                            + sImages
                            + File.separator
                            + "pixel.gif");
                    byte[] b = new byte[1280];
                    inptStrm.read(b);
                    otptStrm = new FileOutputStream(sDirs + File.separator
                            + sImages + File.separator + "pixel.gif");
                    otptStrm.write(b);
                    otptStrm.flush();
                    otptStrm.close();
                } catch (Exception e) {
                    e.printStackTrace();
                }
                finally{
                	if(inptStrm!=null){
                		inptStrm.close();
                	}
                }
            }
        }
        //add images dir if it does not already exist
        //get directory name based on server type
        sDirs = sDirs + getServerTypeDirectory();
        sFile = sDirs + msSaveToTSDFileName;
        sFile = sFile.replaceAll("[*]", "");
        if (vbCreateFile) {
            tmpFile = new File(sDirs);
            if (!tmpFile.exists())
                tmpFile.mkdirs();
            /*
             else if (sDirs.indexOf("Register" + File.separator) == -1)
             */
            else if (sDirs.indexOf("Assessor" + File.separator) >= 0) {
                // new specs -> don't delete Assessor Info Dir for multiple search cycles				
                //				FileUtils.deleteDir(tmpFile);
                //				tmpFile.mkdirs();
            }
            tmpFile = new File(sFile);
            if (tmpFile.exists())
                tmpFile.delete();
            tmpFile.createNewFile();
        }
        ///logger.debug("path = " + sFile);
        return sFile;
    }

    protected String getServerTypeDirectory() {

    	// check if the directory name was overridden - so far only for ILCookDL
    	if(serverTypeDirectoryOverride != null){
        	return serverTypeDirectoryOverride + File.separator;
        } 

    	// check if the directory name was overridden - so far only for ILCookLA
    	if(serverTypeDirectoryOverride != null){
        	return serverTypeDirectoryOverride + File.separator;
        } 
    	
        String path = "";
        String serverType = msServerID.substring(msServerID.lastIndexOf("=") + 1);
        int serverTypeInt = -1;
        try {
			serverTypeInt = Integer.parseInt(serverType);
		} catch (NumberFormatException e) {
			e.printStackTrace();
		} 
        switch (serverTypeInt) {
        case 0:
        case 15:
            path += "Assessor" + File.separator;
            break;
        case 1:
            path += "Register" + File.separator;
            break;
        case 2:
            if (TSServersFactory.isCityTax(miServerID))
                path += "City Tax" + File.separator;
            else
                path += "County Tax" + File.separator;
            break;
        case 3:
            if (TSServersFactory.isDailyNews(miServerID))
                path += "Register" + File.separator;
            else
                path += "BusinessInformation" + File.separator;
            break;
        case 6:
            if (TSServersFactory.isUniformCommercialCode(miServerID))
                path += "Register" + File.separator;
            else
                path += "Other" + File.separator;
            break;
        case 7:
            /*if (TSServersFactory.isCourts(miServerID))
                path += "Register" + File.separator;
            else*/
                path += "Other" + File.separator;
            break;
        case 8:
            /*if (TSServersFactory.isPacer(miServerID))
                path += "Register" + File.separator;
            else*/
                path += "Other" + File.separator;
            break;
        case 9:
            if (TSServersFactory.isOrbit(miServerID))
                path += "Register" + File.separator;
            else
                path += "Other" + File.separator;
            break;
        case 13:
        case 14:
        case 19:
        	path += "Register" + File.separator;
        	break;
        default:
            path += "Other" + File.separator;
            break;
        }
        return path;
    }

    /**
     * Should test using isInstrumentSaved(...)
     * @param FileName
     * @return
     */
    @Deprecated
    protected boolean FileAlreadyExist(String FileName) {
        
    	/* The AO,TR document must be re-saved in date down searches: Bug 4584 */
    	if(mSearch.getSa().isDateDown() && isAssessorOrTaxServer()) {
    		return false;
    	}
    	
        boolean bRtrn1 = false;
        boolean bRtrn2 = false;
        
        //synchronized (msSaveToTSDFileName) {
	    
            //String sTmp = msSaveToTSDFileName;
	        
	        msSaveToTSDFileName = FileName;
	        try {
	            bRtrn1 = (new File(GetFilePath(false))).exists();
	        } catch (IOException e) {
	        }
	        /*
	        msSaveToTSDFileName = sTmp;
            
            if( !"".equals( msSaveToTSDFileName ) )
            {
                try {
                    bRtrn2 = (new File(GetFilePath(false))).exists();
                } catch (IOException e) {
                }
            }*/
        //}
        
        return bRtrn1 || bRtrn2;
    }
    
    
      protected boolean isAssessorOrTaxServer() {
    	return 
    		TSServersFactory.isAssesor(miServerID) 		|| 
    		TSServersFactory.isCountyTax(miServerID) 	||
    		TSServersFactory.isCityTax(miServerID) 		||
    		TSServersFactory.isCountyTUTax(miServerID) 
    	; 
    }
    
      public boolean isAutomaticSearch(){
    	  return getSearch().getSearchType() == Search.AUTOMATIC_SEARCH;
      }
      
      public boolean isParentSiteSearch(){
    	  return getSearch().getSearchType() == Search.PARENT_SITE_SEARCH;
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
    	if(StringUtils.isEmpty(instrumentNo))
    		return false;
    	
    	/* The AO,TR document must be re-saved in date down searches: Bug 4584 */
    	if(mSearch.getSa().isDateDown() && isAssessorOrTaxServer()) {
    		return false;
    	}
    	
    	DocumentsManagerI documentManager = getSearch().getDocManager();
    	try {
    		documentManager.getAccess();
    		if(documentToCheck != null) {
    			if(documentManager.getDocument(documentToCheck.getInstrument()) != null) {
    				if (GWTDataSite.isRealRoLike(dataSite.getSiteTypeInt())){
	    				RegisterDocumentI docFound = (RegisterDocumentI) documentManager.getDocument(documentToCheck.getInstrument());
	    				RegisterDocumentI docToCheck = (RegisterDocumentI) documentToCheck;
	    				
	    				docToCheck.mergeDocumentsInformation(docFound, searchId, true, false);
    				}
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
    
    protected boolean samePrefixFileAlreadyExist(String directory, String prefixFilter, String suffix){
    	try {
	    	File tsrFolder = new File(directory);
	    	if(tsrFolder == null || !tsrFolder.exists() || !tsrFolder.isDirectory())
	    		return false;
	    	String[] files = tsrFolder.list(new PrefixFilenameFilter(prefixFilter));
	    	for (int i = 0; i < files.length; i++) {
				if(files[i].endsWith(suffix))
					return true;
			}
    	} catch (Exception e) {
    		e.printStackTrace();
		}
    	return false;
    }

    protected String CreateFileAlreadyInTSD() {
        return CreateFileAlreadyInTSD(false);
    }

    protected String CreateFileAlreadyInTSD(boolean hrAfter) {
        String text = " This file is saved in TSR Index!";
        if (hrAfter) {
            return text + "<hr>";
        } else {
            return "<hr>" + text;
        }
    }
    
    protected String CreateFileAlreadyInTSD(boolean hrAfter, boolean hrBefore){
        String text = " This file is saved in TSR Index!";
        if (hrAfter) {
           text += "<hr>";
        }
        if(hrBefore){
        	text = "<hr" + text;
        }
        return text;
    }

    /**
     * Returns the serverID.
     * @return int
     */
    public int getServerID() {
        return miServerID;
    }

    /**
     * Sets the serverID.
     * @param serverID The serverID to set
     */
    public void setServerID(int serverID) {
        miServerID = serverID;
        serverTimeout = HashCountyToIndex.getDateSiteForMIServerID(getCommunityId(), miServerID).getSearchTimeout();
    }

    /**
     * Method getParameter.
     * @param searchParameter the parameter to search
     * @param query the query that should contain the parameter
     * @return String the value of the searched parameter
     */
    public static String getParameter(String searchParameter, String query) {
        String sValue = null;
        String sTmp = query;
        //logger.debug("caut parametrul " + sPrm + " din " + vsRequest);
        try {
        	searchParameter = URLEncoder.encode(searchParameter.replaceAll("&", AND_REPLACER),
                    "UTF-8");
        } catch (UnsupportedEncodingException e) {
        }
        int iTmp = sTmp.indexOf(searchParameter + '=');
        if (iTmp != -1) { //is 'name='
            sTmp = sTmp.substring(iTmp + searchParameter.length() + 1);
            //get value
            iTmp = sTmp.indexOf('&');
            if (iTmp != -1) { //is 'value&'
                sValue = sTmp.substring(0, iTmp);
                sTmp = sTmp.substring(iTmp + 1);
            } else {
                //logger.debug("stmp = " + sTmp);
                //is 'value'
                sValue = sTmp;
            }
            //decode found value
            sValue = sValue.replaceAll(AND_REPLACER, "&");
            try {
                sValue = URLDecoder.decode(sValue, "UTF-8");
            } catch (UnsupportedEncodingException e) {
            }
        }
        //logger.debug(" value= " + sValue);
        return sValue;
    }

    public boolean isTimeToSave(ServerResponse sr) {
        boolean rez;
        if (getResultType() == MULTIPLE_RESULT_TYPE) {
            if (sr.isFakeResponse()) {
                rez = true;
            } else {
            	if (numberOfYearsAllowed > 1){
            		boolean rowsMoreThanExpected = lastAnalysisBeforeSaving(sr);;
            		if (rowsMoreThanExpected){
            			rez = false;
            			SearchLogger.info("<br><font color=\"red\"><b>WARNING: </b>More than one row with the same tax year!</font><br>", searchId);
            		} else{
            			rez = (sr.getParsedResponse().isUnique() || sr
    	                        .getParsedResponse().isMultiple());
            		}
            	} else{
	                rez = (sr.getParsedResponse().isUnique() || sr
	                        .getParsedResponse().isMultiple());
            	}
            }
        } else {
            rez = (sr.getParsedResponse().isUnique());
        }
        //logger.debug( "is time to save the files = " + rez);
        return rez;
    }

    @SuppressWarnings("rawtypes")
	public boolean anotherSearchForThisServer(ServerResponse sr) {
        boolean rez = false;
        if (resultType == MULTIPLE_RESULT_TYPE) {
            rez = true;
        } else {		//UNIQUE_RESULT_TYPE
        	ParsedResponse parsedResponse  = sr.getParsedResponse();
        	if (parsedResponse.getResultsCount()==1) {
        		Vector resultRows = parsedResponse.getResultRows();
        		if (resultRows.size()==0 && INVALIDATED_RESULT.equals(parsedResponse.getError())) {		//one result, found directly, which is invalidated
        			rez = true;
        		} else if (resultRows.size()==1) {
        			Object object = resultRows.get(0);
        			if (object instanceof ParsedResponse) {
        				ParsedResponse newParsedResponse = (ParsedResponse)resultRows.get(0); 
        				if (INVALIDATED_RESULT.equals(newParsedResponse.getError())) {					//one result, remained after filtering, which is invalidated
                			rez = true;
                		}
        			}
        		}
        	} else {
        		rez = false;
        	}
        }
        return rez;
    }

    public boolean mustGoBackToUser(ServerResponse sr) {
        boolean rez;
        if (getResultType() == MULTIPLE_RESULT_TYPE && sr.getErrorCode() != ServerResponse.NOT_PERFECT_MATCH_WARNING) {
            rez = false;
        } else {
            rez = ((sr.getErrorCode() == ServerResponse.NOT_PERFECT_MATCH_WARNING) || sr
                    .getParsedResponse().isMultiple());
        }
        //logger.debug( "mustGoBackToUser= " + rez);
        return rez;
    }
    
    public boolean setNextServer(Search global) {
    	
    	DataSite nextServer = HashCountyToIndex.getNextServer(
    			global.getProductId(),
    			global.getCommId(), 
    			dataSite);
    	
    	int nextServerId = miServerID;
    	
    	if (nextServer != null)
    		nextServerId = (int) TSServersFactory.getSiteId(
    				nextServer.getStateAbrev(), 
    				nextServer.getCountyName(), 
    				nextServer.getSiteTypeAbrev());
    	
        HashCountyToIndex.setSearchServerAfterFactoryId(global, nextServerId);
        
        return (miServerID != nextServerId);
    }

    public boolean goToNextServer(Search global) {
        global.setSearchIterator(null);
        return setNextServer(global);
    }

    public String getSaveToTsdButton(String name, String link,int viParseID) {
        String form = CreateSaveToTSDFormHeader();
        form = form + "<input type=\"hidden\" name=\"" + SAVE_TO_TSD_PARAM_NAME
                + "\" value=\"" + link + "\" >";
        //end form
        form += CreateSaveToTSDFormEnd(name, viParseID, -1);
        return form;
    }

    protected String addSaveToTsdButton(String htmlString, String link, int viParseID) {
        return addSaveToTsdButton(htmlString, link, false, viParseID);
    }

    protected String addSaveToTsdButton(String htmlString, String link,
    		
            boolean hrAtEnd,int viParseID) {
        String form = CreateSaveToTSDFormHeader();
        form = form + "<input type=\"hidden\" name=\"" + SAVE_TO_TSD_PARAM_NAME
                + "\" value=\"" + link + "\" >";
        //end form
        String endForm = hrAtEnd ? CreateSaveToTSDFormEnd(null, viParseID, -1) + "<hr>"
                : "<hr>" + CreateSaveToTSDFormEnd(null, viParseID, -1);
        return form + htmlString + endForm;
    }

    protected String appendOtherLinksText(String rsResponce, String linkEnd)
    throws ServerResponseException {
    	return appendOtherLinksText(rsResponce, linkEnd, false);
    }
    
    protected String appendOtherLinksText(String rsResponce, String linkEnd, boolean ignoreIOException)
            throws ServerResponseException {

        //save other links
        String otherLinksText = "";
        String sTmp1 = msServerID + "&";
        int iTmp = rsResponce.indexOf(sTmp1);
        //while there are links on the page follow links
        while (iTmp != -1) {
            iTmp += sTmp1.length();
            otherLinksText += extraText(iTmp, rsResponce);
            
            try {
	            //make request with the found link
	            ServerResponse stmpResponse = FollowLink(getOtherLinksHref(
	                    rsResponce, iTmp, linkEnd), null);
	            otherLinksText += stmpResponse.getParsedResponse().getResponse();
	            //add an hr
	            otherLinksText += "<hr>";
            } catch (ServerResponseException e) {
            	
            	if (ignoreIOException)
            		e.printStackTrace();
            	else
            		throw e;
            }
            //go to next link
            iTmp = rsResponce.indexOf(sTmp1, iTmp);
        }

        //concat pages
        rsResponce = rsResponce + "<hr>" + otherLinksText;

        return rsResponce;
    }

    protected String extraText(int fromIdx, String rsResponce) {
        return "";
    }

    protected String getOtherLinksHref(String rsResponce, int linkStartIdx,
            String linkEnd) {
        return rsResponce.substring(linkStartIdx, rsResponce.indexOf(linkEnd,
                linkStartIdx)).replace( "&amp;" , "&");
    }

    protected String getLinkPrefix(int type) {
        return ACTION_TYPE_LINK + "=" + type + "&" + msPrmNameLink + "=";
    }

    public void setIteratorType( int iteratorType ){
    	this.iteratorType = iteratorType;
    }
    
    public void setServerForTsd(Search search, String siteRealPath) {
        mSearch = search;
        if(mSearch != null){
        	searchId = mSearch.getID();
        }
        msSiteRealPath = siteRealPath;
        parser = new Parser(BaseServlet.REAL_PATH, miServerID, search,this.searchId);
        setDocsValidator(docsValidatorType, null);
        setCrossRefDocsValidator(crossRefDocsValidatorType, null);
    }

    protected int manageDocsValidatorType(int type) {
        String tsu = mSearch.getSa().getAtribute(SearchAttributes.SEARCHUPDATE);
        if (tsu != null && tsu.trim().toLowerCase().equals("true")
                && type == DocsValidator.TYPE_ALWAYS_TRUE) {
            return DocsValidator.TYPE_DEFAULT;
        }
        return type;
    }

    /**
     * @return
     */
    public DocsValidator getDocsValidator() {
        return docsValidator;
    }
    
    public DocsValidator getCrossRefDocsValidator(){
    	return crossRefDocsValidator;
    }

    public Vector<DocsValidator> getDocsValidators(){
    	return docsValidators;
    }
    public Vector<DocsValidator> getCrossRefDocsValidators(){
    	return crossRefDocsValidators;
    }
    
    /**
     * @param validator
     */
    public void setDocsValidator(DocsValidator validator) {
        docsValidator = validator;
    }
    public void setCrossRefDocsValidator(DocsValidator validator){
    	crossRefDocsValidator = validator;
    }
    
    
    /**
     * @param imgLinkInPage  - the object that define the image link in page
     * @param imageType		 - the type of the image document (see TSServer.ImageTypes class)
     * @param maxErrorCount  - max time to retry if an error ocurs
     * @return				 - OCRParsedDataStruct define the scanded data
     */
    public CallDipResult ocrDownloadAndScanImageIfNeeded( 
    		DocumentI documentToOcr, String imageType, int maxErrorCount, long timeToSleepWhenError, 
    		boolean checkNameOnLastRealTransfer){
    	return ocrDownloadAndScanImageIfNeeded( documentToOcr, imageType,false, maxErrorCount, timeToSleepWhenError, checkNameOnLastRealTransfer);	
    }
    /**
     * @param imgLinkInPage  - the object that define the image link in page
     * @param imageType		 - the type of the image document (see TSServer.ImageTypes class)
     * @param maxErrorCount  - max time to retry if an error ocurs
     * @return				 - OCRParsedDataStruct define the scanded data
     */
    public CallDipResult ocrDownloadAndScanImageIfNeeded( 
    		DocumentI documentToOcr, String imageType, boolean isManualOcr, int maxErrorCount, long timeToSleepWhenError, 
    		boolean checkNameOnLastRealTransfer){
    	
    	ImageI imageToBeOcred 			=	null;
//    	OCRParsedDataStruct ocrData		=	null;
		CallDipResult callDipResult = null;
    	StringBuilder infoToBeLogged	=	new StringBuilder();
    	String legalDescriptionImageFileName = null;
    	String dipInputFile = null;
    	
    	Search global = getSearch();
    	CurrentInstance currentInstance = InstanceManager.getManager().getCurrentInstance(searchId);
    	
    	try{
    		imageToBeOcred 				= documentToOcr.getImage();
    		int count 					=	0;
			String imgSavePath 			= 	null;
    		
			if(imageToBeOcred == null){
				return null;
			}
			imageToBeOcred.setOcrInProgress(true);
    		imageToBeOcred.setOcrDone(false);
			
	    	ImageLinkInPage imgLinkInPage = ImageTransformation.imageToImageLinkInPage(imageToBeOcred);
	    	
	    	long startOperationTime = System.currentTimeMillis();
	    	if( testIfNeedToDownloadAndScanImage(documentToOcr,isManualOcr) ){
				
	    		boolean ifError		=	false;
	    		boolean dipTimeout	=	false;
	    		boolean triedOmnipage = false; // used so that we try omnipage only once
	    		
	    		infoToBeLogged.append("</div><hr/><B>OCR </B> ")
	    			.append("Instrument Number: <B>[").append(documentToOcr.getInstno())
	    			.append("]</B>, Book: <B>[").append(documentToOcr.getBook())
	    			.append("]</B>, Page: <B>[").append(documentToOcr.getPage())
	    			.append("]</B>, DocumentNo: <B>[").append(documentToOcr.getDocno())
	    			.append("]</B>, Category: <B>[").append(documentToOcr.getDocType())
	    			.append("]</B>, Subcategory: <B>[").append(documentToOcr.getDocSubType())
	    			.append("]</B> ")
	    			.append(SearchLogger.getTimeStamp(searchId)).append("<br>")
	    			.append(StringUtils.createCollapsibleHeader());
	        	
				do{
					if(triedOmnipage && ifError)
						ifError = true;
					else
						ifError = false;	//reset value if didn't tried omnipage yet
	    			dipTimeout = false;
	    			imgSavePath = imageToBeOcred.getPath();
	    			boolean imageDownloaded = imageToBeOcred.exists();
	    			
	    			try{
	    				if(imageDownloaded) {
	    					//do nothing we have it and it exists
	    				} else if(imageToBeOcred.isUploaded()) {
	    					//uploaded but not on local cache, let's get it from SSF
	    					if(DocumentDataRetreiver.downloadImageFromSsfIfNeedIt(global, imageToBeOcred)){
	    						imageDownloaded = true;
							}
	    				} else {
	    					DownloadImageResult downloadImageResult = downloadImage(documentToOcr);
	    					/*saveImage(imageToBeOcred);
	    					if(downloadImageResult == null) {
	    						downloadImageResult = saveImage(imgLinkInPage);
	    					}
	    					*/
		    				imageDownloaded = downloadImageResult.getStatus() == DownloadImageResult.Status.OK;
		    				if(imageDownloaded ) {
								/*FileContent.replicateImage(
										ImageTransformation.imageLinkInPageToImage(imgLinkInPage), 
										searchId, 
										false);*/
		    					
		    					File image = new File(imageToBeOcred.getPath());
		    					if(!image.exists()) {
		    						org.apache.commons.io.FileUtils.writeByteArrayToFile(image, downloadImageResult.getImageContent());
		    					}
		    					
		    					TsdIndexPageServer.uploadImageToSSf(documentToOcr.getId(), searchId, false, false);
		    				}
	    				}
	    			}
	    			catch(Exception e){
	    				e.printStackTrace();
	    			}
	    			
		    		if(imageDownloaded){
		    			imageToBeOcred.setSaved(true);
		    			imgLinkInPage.setSolved(true);
		    			if( imgSavePath.endsWith(".pdf") || imgSavePath.endsWith(".PDF") ){
		    				imgSavePath = ro.cst.tsearch.pdftiff.util.Util.resizePdfToA4(imgSavePath, 1.5);
		    				imgSavePath = OCRParsedDataStruct.convertPdfToTiff( imgSavePath );
		    			}
		    			
		    			String dirPath = imgSavePath.substring(0,imgSavePath.lastIndexOf(File.separator)+1);
		    			if(dirPath.length()>0){
		    				dirPath += "OCR_Images";
		    				File dirPathFile = new File(dirPath);
		    				if(!dirPathFile.exists())
		    					dirPathFile.mkdir();
		    			}
						String xmlFileName = dirPath + File.separator + 
							imgSavePath.substring(imgSavePath.lastIndexOf(File.separator)+1,imgSavePath.lastIndexOf(".")) + "_ocr";
						
						// create output folder
						new File(xmlFileName.substring(0, xmlFileName.lastIndexOf(File.separator))).mkdirs();
						
						FileInfo fileInfo = null;
						
						if (documentToOcr instanceof TransferI) {
							fileInfo = new FileInfo(-1, imgSavePath, -1, "");
						} else {					
							if( !(imgSavePath.endsWith(".pdf") || imgSavePath.endsWith(".PDF")) ){
								fileInfo = ro.cst.tsearch.pdftiff.util.Util.getPartOfTheImage(
									imgSavePath, 
									OCRDownloader.DEFAULT_FIRST_PAGES, 
									OCRDownloader.DEFAULT_LAST_PAGES);
							}
						}
						dipInputFile = fileInfo.name;
						
						IndividualLogger.info("Will ocr a " + imageType + " that is in " + dipInputFile, searchId);
						if(fileInfo.size >= 0) {
							infoToBeLogged.append("ATS will only process " + fileInfo.size + " pages from the image");
							infoToBeLogged.append("(first " + 
									((fileInfo.size >= OCRDownloader.DEFAULT_FIRST_PAGES)?
											OCRDownloader.DEFAULT_FIRST_PAGES:
											(OCRDownloader.DEFAULT_FIRST_PAGES - fileInfo.size)) + 
									((OCRDownloader.DEFAULT_LAST_PAGES > 0)?
											((fileInfo.size > OCRDownloader.DEFAULT_FIRST_PAGES)? 
													(fileInfo.size - OCRDownloader.DEFAULT_FIRST_PAGES):""):
											"")+ ")<br>");
							
						} else {
							infoToBeLogged.append("The entire image will be processed ");
							if ( !(documentToOcr instanceof TransferI) ) {
								if(fileInfo.id >= 0) {
									infoToBeLogged.append("(" + fileInfo.id + " pages)<br>");
								} else {
									infoToBeLogged.append("(number of pages could not be determined)<br>");
								}
							} else {
								infoToBeLogged.append("<br>");
							}
						}
						try {						
							if(!triedOmnipage && ServerConfig.isOmnipageEnabled()){
								triedOmnipage = true; 
								int timeout =  ServerConfig.getOmnipageTimeout();
								com.stewart.ats.webservice.ocr.xsd.Image image = com.stewart.ats.webservice.ocr.xsd.Image.Factory.newInstance();
								image.setDescription(dipInputFile);
								
								if( imgSavePath.endsWith(".pdf") || imgSavePath.endsWith(".PDF") ){
									image.setData(org.apache.commons.io.FileUtils.readFileToByteArray((new File(dipInputFile))));
									image.setType("pdf");
								} else {
									ResizeResult result = ImageResizer.getBytesScaledDownInstance(dipInputFile, 8400, 8400);
									if( ConversionStatus.YES.equals(result.status) ){
										infoToBeLogged.append("Success scale down image: "+documentToOcr.prettyPrint() +"<br/>");
										image.setData(result.imageData);
									}else if(ConversionStatus.NO.equals(result.status)){
										image.setData( org.apache.commons.io.FileUtils.readFileToByteArray((new File(dipInputFile))) );
									}else{
										infoToBeLogged.append("Internal error when trying to scale down the image: "+documentToOcr.prettyPrint() +"<br/>");
										return callDipResult;
									}
									
									image.setType("tif");
								}
								 
								boolean interactive = global.getSearchType()==Search.INTERACTIVE_SEARCH;
								long startOCRService = System.currentTimeMillis();
								Output out = OCRService.process(image, timeout, interactive);
								long endOCRService = System.currentTimeMillis();
								System.err.println("SearchID " + searchId + " on " + toString() + " OCRService.process took " + ((endOCRService - startOCRService)) + " millis");
								
								if(out.getSuccess()){		 					
									dipInputFile = xmlFileName + ".omnipage.xml";		
									FileUtils.writeByteArrayToFile(out.getResult(), dipInputFile);
									logger.info("Will OCR the following file " + dipInputFile);
									long startTMS = System.currentTimeMillis();
									callDipResult = callDip(documentToOcr, dipInputFile, xmlFileName, fileInfo.name, infoToBeLogged, null);
									long endTMS = System.currentTimeMillis();
									System.err.println("SearchID " + searchId + " on " + toString() +  "callDip - TSServer" + toString() + ": " + ((endTMS - startTMS)));
									
									ifError = callDipResult.isIfError();
									dipTimeout = callDipResult.isDipTimeout();
									legalDescriptionImageFileName = callDipResult.getLegalDescriptionImageFileName();
									
								} else {
									ifError = true;
									String omnipageMessage = out.getMessage();
									logger.error("Omnipage did not work on image: " + imgSavePath + ": " + omnipageMessage);
									infoToBeLogged.append("FAILED: Omnipage error: " + out.getMessage() + 
											("timeout".equals(omnipageMessage)? "(" + timeout + ")":"") + "<br>");
									
									EmailClient email = new EmailClient();
									email.addTo(MailConfig.getExceptionEmail());
									email.setSubject("Omnipage did not work on image on " + URLMaping.INSTANCE_DIR);
									
									email.addContent(
											SearchLogger.getCurDateTimeCST() +"\n\n"+
											"Abstractor File Id: " + global.getAbstractorFileNo() +"\n\n"+
											"Community: " + currentInstance.getCurrentCommunity().getNAME() +"\n"+
											"State: " + currentInstance.getCurrentState().getName() +"\n"+
											"County: " + currentInstance.getCurrentCounty().getName() +"\n"+
											"Document Server: " + global.getCrtServerType(global.getSearchType()==Search.PARENT_SITE_SEARCH)+"\n"+
											"Document Type: " + documentToOcr.getDocType()+"\n\n"+
											"Hard disk image:" + imgSavePath + "\n\n" + 
											"Omnipage was called on the following attachement.\n"
									);
									email.addAttachment(imgSavePath);
									email.sendAsynchronous();
									
									break;
								}
							} 
						} catch (Exception e){
							logger.error("Error while trying to call Omnipage", e);
							IndividualLogger.info("Error while trying to call Omnipage\n" + ServerResponseException.getExceptionStackTrace( e, "\n" ), searchId);
							infoToBeLogged.append("FAILED: Error while trying to call Omnipage <br>");
							ifError = true;
							EmailClient email = new EmailClient();
							email.addTo(MailConfig.getExceptionEmail());
							email.setSubject("Error while trying to call Omnipage on " + URLMaping.INSTANCE_DIR);
							email.addContent(
									SearchLogger.getCurDateTimeCST() +"\n\n"+
									"Abstractor File Id: " + global.getAbstractorFileNo() +"\n\n"+
									"Community: " + currentInstance.getCurrentCommunity().getNAME() +"\n"+
									"State: " + currentInstance.getCurrentState().getName() +"\n"+
									"County: " + currentInstance.getCurrentCounty().getName() +"\n"+
									"Document Server: " + global.getCrtServerType(global.getSearchType()==Search.PARENT_SITE_SEARCH)+"\n"+
									"Document Type: " + documentToOcr.getDocType()+"\n\n"+
									"Stack Trace: " + e.getMessage() + " \n\n " + ServerResponseException.getExceptionStackTrace( e, "\n" ) + 
									"\nOmnipage was called on the following attachement.\n"
							);
							email.addAttachment(imgSavePath);
							email.sendAsynchronous();
						}
						
					}
		    		else{
		    			ifError=true;
		    		}
		    		
		    		if(ifError){
						try{
							Thread.sleep(timeToSleepWhenError);
						}
						catch(InterruptedException intE){
							intE.printStackTrace(System.err);
						}
					}
		    		
		    		count++;
		    		
		    		if( dipTimeout ){
		    			IndividualLogger.info( "OCR: Dip timeout, aborting OCR processing!" ,searchId);
		    			//SearchLogger.info( "OCR: Dip timeout, aborting OCR processing!<br>" ,searchId);
		    			break;
		    		}
	    		}
				while( ifError && !triedOmnipage && count < maxErrorCount );
				String infoLog = "The ocr process took " + ((System.currentTimeMillis() - startOperationTime)/1000) + " seconds and ";
				if (ifError){
					infoLog += "failed";
				} else{
					if (callDipResult != null && callDipResult.isNothingExtracted()){
						infoLog += "no data was extracted";
					} else{
						infoLog += "completed successfully";
					}
				}
				infoLog += "<br/>";
				infoToBeLogged.append(infoLog);
				if(callDipResult != null && !callDipResult.isNothingExtracted()) {
					Util.copyOCRFiles(imgSavePath, callDipResult.getOcrData(), searchId,"");
				}
			}
	    	long startFinalDataProcess = System.currentTimeMillis();
	    	if(callDipResult != null && callDipResult.getOcrData() != null) {
	    		DocumentsManagerI manager = global.getDocManager();
	    		try{
	    			manager.getAccess();
	    			TransferI lastRealTransfer = manager.getLastRealTransfer();
	    			cleanOCRData(callDipResult.getOcrData());
	    			//update the document Data with what we found from OCR
	    			updateDocumentWithOCRData(manager, documentToOcr, callDipResult.getOcrData(), legalDescriptionImageFileName, global, infoToBeLogged, isManualOcr);
	    			  			
	    			//update Search data using OCR information from Last Real Transfer
	    			if(!isManualOcr && lastRealTransfer!=null && documentToOcr.equals(lastRealTransfer) && !StringUtils.isEmpty(legalDescriptionImageFileName) ){
	    				bootstrapOCRInfoFromDocument(lastRealTransfer, callDipResult.getOcrData(), global);
	    			}
	    		}
	    		finally{
	    			manager.releaseAccess();
	    			imageToBeOcred.setOcrDone(true);
	    		}
	    		callDipResult.getOcrData().addLoggableInfo(infoToBeLogged.toString());
	    	}
	    	System.err.println("SearchID " + searchId + " on " + toString() + " updating documents with OCR data took " + ((System.currentTimeMillis() - startFinalDataProcess)) + " millis");
	    	
	    	infoToBeLogged.append("</div>");
	    	IndividualLogger.info(infoToBeLogged.toString().replace("<b>", "").replace("<br>","\r\n"), searchId);
			SearchLogger.info(infoToBeLogged.toString(), searchId);
    	}
    	finally{
    		if(imageToBeOcred!=null){
    			imageToBeOcred.setOcrInProgress(false);
    			imageToBeOcred.setPlanedForOCR(false);
    		}
    	}
    	if(dipInputFile != null && dipInputFile.contains(".tif") && !dipInputFile.contains("_part")){	//clean generated file
    		try {
    			File fileDipInputFile = new File(dipInputFile);
    			if(fileDipInputFile.exists()){
    				fileDipInputFile.delete();
    			}
    		} catch (Exception e) {
				e.printStackTrace();
			}
    	}
		return callDipResult;
    }
    
    @Override
    public  CallDipResult callDip(DocumentI documentToOcr, String dipInputFilePath, String xmlFileNameRoot, String imageFile,
    		StringBuilder infoToBeLogged, String xmlFileFromSmartEditor){
    	
    	long startCallDip = System.currentTimeMillis();
    	
    	Search search = getSearch();
    	
    	File dipInputFile = new File(dipInputFilePath);
    	if(!dipInputFile.exists()) {
    		OCRDownloader ocrDownloader = new OCRDownloader( searchId , documentToOcr);
    		ocrDownloader.setManualOcr(true);
    		ocrDownloader.run();	//just call the method
    	}
    	
    	if( imageFile.endsWith(".pdf") || imageFile.endsWith(".PDF") ){
    		imageFile = ro.cst.tsearch.pdftiff.util.Util.resizePdfToA4(imageFile, 1.5);
    		imageFile = OCRParsedDataStruct.convertPdfToTiff( imageFile );
		}
    	
    	CallDipResult ret = new CallDipResult(search);
		
		String buyerName = "\"" + search.getSa().getBuyers().getFullNames(NameFormaterI.PosType.OCR, "|") + "\"";
		String sellerName = "\"" + search.getSa().getOwners().getFullNames(NameFormaterI.PosType.OCR, "|") + "\"";
		
		//quick fix for 4078
		buyerName = sellerName;
		sellerName = "\"" + "\"";
		
		DocumentImageParser dip = new DocumentImageParser();
		
		Vector<String> commandVector = new Vector<String>();
		commandVector.add(ServerConfig.getOcrExecutableFullpath());
		if(isRangeNotExpanded()){
			commandVector.add("--no_expand_range");			
			dip.setPageRangeExpandMode(false);
		} else {
			dip.setPageRangeExpandMode(true);
		}
		
		
		if ( documentToOcr instanceof MortgageI) {
			commandVector.add("--mortgage");
			commandVector.add(String.valueOf(OCRDownloader.DEFAULT_FIRST_PAGES));
			commandVector.add(String.valueOf(OCRDownloader.DEFAULT_LAST_PAGES));	
			dip.setDocumentType("mortgage", OCRDownloader.DEFAULT_FIRST_PAGES, OCRDownloader.DEFAULT_LAST_PAGES);
		} else if ( documentToOcr instanceof TransferI) {
			dip.setDocumentType("transfer");
		}
		if(StringUtils.isNotEmpty(xmlFileFromSmartEditor)){
			commandVector.add("--validate_report"); 
			commandVector.add(xmlFileFromSmartEditor);
		}
		
		
		
		dip.setImageFile(imageFile);
		dip.setImageDescriptionFile(dipInputFilePath);
		dip.setNames(search.getSa().getOwners().getFullNames(NameFormaterI.PosType.OCR, "|"));
		
		commandVector.add("--image"); 
		commandVector.add(imageFile);
		commandVector.add(dipInputFilePath);
		commandVector.add(xmlFileNameRoot);
		commandVector.add(buyerName);
		commandVector.add(sellerName);
		
		
//		System.err.println("ocrDownloadAndScanImageIfNeeded");
//		System.err.println("dipInputFile : " + dipInputFilePath);
//		System.err.println("xmlFileName : " + xmlFileNameRoot);
//		System.err.println("buyerName : " + buyerName);
//		System.err.println("sellerName : " + sellerName);
		
		
		try {
			if(StringUtils.isNotEmpty(xmlFileFromSmartEditor)){
				dip.validateReport(xmlFileFromSmartEditor,xmlFileNameRoot);
			} else {
				if (!dip.createReport(xmlFileNameRoot)){
					ret.setNothingExtracted(true);
				}
			}
			
			System.err.println("SearchID " + searchId + " on " + toString() + ". Java DIP took " + (System.currentTimeMillis() - startCallDip) + " millis");
			
		} catch (Exception e) {
			e.printStackTrace();
			
			ret.setIfError(true);
			
            IndividualLogger.info( "OCR: Exception when executing DIP:" + StringUtils.exception2String( e ) ,searchId);
            //SearchLogger.info( "OCR: Exception when executing DIP: " + e.getLocalizedMessage() + "<br>",searchId);
            infoToBeLogged.append(
            		"FAILED: Error when executing Java DIP: " + e.getLocalizedMessage() + 
            		(ret.isDipTimeout()?" (timeout " + ClientProcessExecutor.EXEC_TIMEOUT + ")":"") + "<br>");
            
            //--- backup -- try the old version
            
			ClientProcessExecutor cpe = new ClientProcessExecutor(
					commandVector.toArray(new String[commandVector.size()]),
					false,
					false);
			cpe.setWorkingDirectory(ServerConfig.getOcrDirectory());
			cpe.setSearchId(searchId);

			Log.sendExceptionViaEmail(
					MailConfig.getMailLoggerToEmailAddress(),
					"Problem executing JAVA DIP for " + searchId + " on " + URLMaping.INSTANCE_DIR,
					e,
					documentToOcr.shortPrint() + "\n\n" + dip.toString().replaceAll(",", ",\n"));
			
			try {
				infoToBeLogged.append("Retrying...<br>");
				ret.setIfError(false);		//reset error flag
				cpe.start();
				
				System.err.println("SearchID " + searchId + " on " + toString() + ". Java DIP Error + C DIP took " + (System.currentTimeMillis() - startCallDip) + " millis");
				
			} catch (IOException ioe) {
				ioe.printStackTrace();
				ret.setIfError(true);

				if (cpe.getReturnValue() == ClientProcessExecutor.EXEC_TIMEOUT) {
					// timeout on executing dip
					ret.setDipTimeout(true);
				}

				IndividualLogger.info("OCR: Exception when executing DIP:" + StringUtils.exception2String(e), searchId);
				// SearchLogger.info( "OCR: Exception when executing DIP: " + e.getLocalizedMessage() + "<br>",searchId);
				infoToBeLogged.append(
						"FAILED: Error when executing DIP: " + e.getLocalizedMessage() +
								(ret.isDipTimeout() ? " (timeout " + ClientProcessExecutor.EXEC_TIMEOUT + ")" : "") + "<br>");

			}
			if (cpe.getReturnValue() == ClientProcessExecutor.EXEC_TIMEOUT) {
				// timeout on executing dip
				ret.setDipTimeout(true);
				ret.setIfError(true);
				infoToBeLogged.append(
						"FAILED: Error when executing DIP: " +
								(ret.isDipTimeout() ? " (timeout " + ClientProcessExecutor.EXEC_TIMEOUT + ")" : "") + "<br>");
			} else if (cpe.getReturnValue() != 0) {
				ret.setIfError(true);
				infoToBeLogged.append(
						"FAILED: Error when executing DIP: Return value is " + cpe.getReturnValue() + "<br>");
			}
            
            
		}
		
		long dipEndTime = System.currentTimeMillis();
		System.err.println("SearchID " + searchId + " on " + toString() + ". DIP took " + (System.currentTimeMillis() - startCallDip) + " millis");
		
		String xmlFileName = xmlFileNameRoot +".xml";
		
//		ClientProcessExecutor cpe=new ClientProcessExecutor( 
//				commandVector.toArray(new String[commandVector.size()]), 
//				false, 
//				false);
//		cpe.setWorkingDirectory(WORKING_DIRECTORY);
//		cpe.setSearchId( searchId );
//		
//		try{
//			cpe.start();
//		}
//		catch(IOException e){
//			e.printStackTrace(System.err);
//			ret.ifError =true;
//			
//			if( cpe.getReturnValue() == ClientProcessExecutor.EXEC_TIMEOUT ){
//				//timeout on executing dip
//				ret.dipTimeout = true;
//			}
//			
//            IndividualLogger.info( "OCR: Exception when executing DIP:" + StringUtils.exception2String( e ) ,searchId);
//            //SearchLogger.info( "OCR: Exception when executing DIP: " + e.getLocalizedMessage() + "<br>",searchId);
//            infoToBeLogged.append(
//            		"FAILED: Error when executing DIP: " + e.getLocalizedMessage() + 
//            		(ret.dipTimeout?" (timeout " + ClientProcessExecutor.EXEC_TIMEOUT + ")":"") + "<br>");
//			
//		}
//		if( cpe.getReturnValue() == ClientProcessExecutor.EXEC_TIMEOUT ){
//			//timeout on executing dip
//			ret.dipTimeout = true;
//			ret.ifError = true;
//			infoToBeLogged.append(
//            		"FAILED: Error when executing DIP: " + 
//            		(ret.dipTimeout?" (timeout " + ClientProcessExecutor.EXEC_TIMEOUT + ")":"") + "<br>");
//		} else if(cpe.getReturnValue() != 0) {
//			ret.ifError = true;
//			infoToBeLogged.append(
//            		"FAILED: Error when executing DIP: Return value is " + cpe.getReturnValue() + "<br>");
//		}
		
		try{
			if(!ret.isIfError()){
				
				if (ret.isNothingExtracted()){
					System.err.println("SearchID " + searchId + " on " + toString() + ". DIP nothing extracted.");
				} else{
					if(search.getSa().getAtribute(SearchAttributes.SEARCH_ORIGIN).toUpperCase().startsWith("TITLEDESK")){
						ret.setOcrData(OCRParsedDataStruct.getDataFromXML(xmlFileName, searchId, false));
					}else{
						ret.setOcrData(OCRParsedDataStruct.getDataFromXML(xmlFileName, searchId, true));
					}
					infoToBeLogged.append("Extracted the following information:<br>");
					infoToBeLogged.append(ret.getOcrData().toStringNice("<br>", false));
					ret.setLegalDescriptionImageFileName(xmlFileName + ".tif");
					
					long parseXMLEnd = System.currentTimeMillis();
					System.err.println("SearchID " + searchId + " on " + toString() + ". Parsing XML data took " + (System.currentTimeMillis() - dipEndTime) + " millis");
					 
					
					//we need to store dip results in database used for smart viewer or else ... 
					File jsFile = new File(xmlFileNameRoot + ".js");
	//				OcrFileMapper fileMapper = null;
	//				int fileCounter = 0;
					if(jsFile.exists()) {
						
						ret.addLocalFile(jsFile);
						
	//					fileMapper = new OcrFileMapper();
	//					fileMapper.setFileName(FilenameUtils.getName(jsFile.getName()));
	//					fileMapper.setFileContent(org.apache.commons.io.FileUtils.readFileToByteArray(jsFile));
	//					fileMapper.setDocumentId(documentToOcr.getId());
	//					fileMapper.setImageFileName(documentToOcr.getImage().getFileName());
	//					fileMapper.setType(OcrFileMapper.TYPE_SMART_VIEWER_JS);
	//					fileMapper.setSearchId(searchId);
	//					if (insertInDatabase) {
	//						fileMapper.insertInDatabase();
	//						fileCounter++;
	//					} else {
	//						ocrFileMappers.add(fileMapper);
	//					}
						
						
						for (int i = 1; i < 50; i++) {
							File pngFile = new File(xmlFileNameRoot + "_part_" + i + ".png");
							if(pngFile.exists()) {
								
								ret.addLocalFile(pngFile);
								
	//							fileMapper = new OcrFileMapper();
	//							fileMapper.setFileName(FilenameUtils.getName(pngFile.getName()));
	//							fileMapper.setFileContent(org.apache.commons.io.FileUtils.readFileToByteArray(pngFile));
	//							fileMapper.setDocumentId(documentToOcr.getId());
	//							fileMapper.setImageFileName(documentToOcr.getImage().getFileName());
	//							fileMapper.setType(OcrFileMapper.TYPE_SMART_VIEWER_PNG);
	//							fileMapper.setSearchId(searchId);
	//							if (insertInDatabase) {
	//								fileMapper.insertInDatabase();
	//								fileCounter++;
	//							} else {
	//								ocrFileMappers.add(fileMapper);
	//							}
							} else {
								break;
							}
						}
						
	//					if(insertInDatabase) {
							new Thread(ret.getOcrFileArchiveSaver(), "OcrFileArchiveSaver " + searchId + " - document " + documentToOcr.getNiceFullFileName()).start();
	//					}
					}
				}
//				System.err.println("SearchID " + searchId + " on " + toString() + ". Saving " + fileCounter + 
//						" with insertInDatabase " + insertInDatabase + 
//						" files took " + (System.currentTimeMillis() - parseXMLEnd) + " millis");
			}
		}
		catch(IOException ioe){
			ioe.printStackTrace(System.err);
			ret.setIfError(true);
		}
		catch (RuntimeException e) {
			e.printStackTrace(System.err);
			ret.setIfError(true);
		}
		
		System.err.println("SearchID " + searchId + " on " + toString() + ". Full call dip took " + (System.currentTimeMillis() - startCallDip) + " millis");
		
    	return ret;
    }
    
    public static void bootstrapOCRInfoFromDocument(RegisterDocumentI lastRealTransfer, OCRParsedDataStruct ocrData, Search currentSearch){
    	long searchId 			= currentSearch.getID();
    	SearchAttributesI sa	= currentSearch.getSa();
    	int serverId 			= (int)lastRealTransfer.getSiteId();
    	boolean legalBootstrapEnabled = false;
    	boolean nameBootstrapEnabled = false;
    	if(serverId<0){
    		legalBootstrapEnabled = true;
    		nameBootstrapEnabled = true;
    	}else{
	    	if(!lastRealTransfer.getImage().isUploaded()) {
		    	DataSite dat 			= HashCountyToIndex.getDateSiteForMIServerID( 
		    			currentSearch.getCommId(), 
		    			serverId  );
		    	legalBootstrapEnabled	=	ro.cst.tsearch.searchsites.client.Util.isSiteEnabledLegalBootstrap(dat.getEnabled(currentSearch.getCommId()));
		    	nameBootstrapEnabled	=	ro.cst.tsearch.searchsites.client.Util.isSiteEnabledNameBootstrap(dat.getEnabled(currentSearch.getCommId()));
	    	}
    	}
    	
    	boolean ocrDone = false;
    	
    	if(lastRealTransfer.getImage().isUploaded() || legalBootstrapEnabled){
    		//bootstrap isCondo
    		if(ocrData.isCondo()) {
    			((SearchAttributes)sa).setAtribute(SearchAttributes.IS_CONDO, "true");
    		}
    		
    		//bootstrap units
    		Vector<String> unitVector = ocrData.getUnitVector();
    		if(!unitVector.isEmpty()) {
    			String units = "";
    			for(String unit : unitVector) {
    				units += ", " + unit;
    			}
    			units = units.replaceFirst("^, ", "");
    			((SearchAttributes)sa).setAtribute(SearchAttributes.LD_SUBDIV_UNIT, units);
    		}
    		
    		ocrDone = true;
    	}
    	
    	if(lastRealTransfer.getImage().isUploaded() || nameBootstrapEnabled){
    		//boostrap the names
        	List<NameI> names = null;
        	
        	if(serverId<0){
        		names = defaultParseOCRNames( ocrData );
        	}else{
        		names = TSServersFactory.GetServerInstance(serverId, searchId).parseOCRNames( ocrData );
        	}
        	
        	NameSourceType nameSourceType = new NameSourceType(NameSourceType.OCR, lastRealTransfer.getId());
        	for( NameI name:names ){
        		
        		name.getNameFlags().addSourceType(nameSourceType);
        		name.setMiddleName(NameCleaner.processMiddleName(name.getMiddleName()));
 			   	name.setSufix(NameCleaner.processNameSuffix(name.getSufix()));
        		
            	if ( !ExactNameFilter.isMatchGreaterThenScore(sa.getOwners().getNames(), name,0.99) ){
            		sa.getOwners().add(name);
    	    	}
        	}
        	
        	/**
    		 * A linked set containing names from document already added from the same document by OCR process
    		 */
    		Set<NameI> namesToBeDeleted = new LinkedHashSet<NameI>();
    		
    		for (NameI nameI : sa.getOwners().getNames()) {
    			NameFlagsI nameFlags = nameI.getNameFlags();
    			if(nameFlags.isFrom(nameSourceType)) {
    				if(!names.contains(nameI)) {
    					namesToBeDeleted.add(nameI);
    				}
    			}
    		}
    		
    		if(!namesToBeDeleted.isEmpty()) {
    			for (NameI nameI : namesToBeDeleted) {
					sa.getOwners().remove(nameI);
				}
    		}
    		
    		ocrDone = true;
    	}
    	
    	if (ocrDone) {
    		IndividualLogger.info("OCR: XML FILE START\n"+ ocrData.getXmlContents().replaceAll( "\n" , "<BR>")+"\nOCR: XML FILE END",searchId );
    	}
    }
    
    @Override
    public void updateDocumentWithOCRData(DocumentsManagerI documentsManager, DocumentI documentI, OCRParsedDataStruct ocrData, Search search,  StringBuilder infoToBeLogged){
    	//we detected a good doctype with OCR
		String docCateg = DocumentTypes.getDocumentCategory(ocrData.getDocumentType(), search.getID());
		if (documentI.getDocType().equals(DocumentTypes.MISCELLANEOUS) &&
				!docCateg.equals("MISCELLANEOUS")) {
			
			  try {
				documentI.setDocType(docCateg);

				ArrayList<String> l = new ArrayList<String>();
				RegisterDocumentI regdoc = DocumentsManager.createRegisterDocument( search.getID(), docCateg, (RegisterDocument)documentI, l);
				
				regdoc.setDocSubType(DocumentTypes.getDocumentSubcategory(ocrData.getDocumentType(), search.getID()));
				if(regdoc.getServerDocType() == null || regdoc.getServerDocType().equals(DocumentTypes.MISCELLANEOUS)) {
					regdoc.setServerDocType(ocrData.getDocumentType());
				}
				
				
				//used first for uploads when we change the type from MISCELLANEOUS to MORTGAGE we fill grantee lander
				if(/*doc.isOneOf(DocumentTypes.MISCELLANEOUS)&&*/regdoc.isOneOf(DocumentTypes.MORTGAGE)){
					if(regdoc instanceof MortgageI){
						((MortgageI)regdoc).setGranteeLenderFreeForm(documentI.getGranteeFreeForm());
					}
				}
				
				documentsManager.replace( documentI, regdoc );
				regdoc.setImage(documentI.getImage()); 
				documentI = regdoc;
				TsdIndexPageServer.clearDocmanagerCaches(documentsManager, regdoc);
			  }catch(Exception e) {
				  e.printStackTrace();
			  }
		}

		if(documentI.isUploaded() && !StringUtils.isEmpty(ocrData.getDocumentType())) {
			documentI.setDescription(ocrData.getDocumentType());	
		}
		
		if (((RegisterDocumentI) documentI).getOcrData() == null){
			((RegisterDocumentI) documentI).setOcrData(ocrData);
		}
		
		if(addNamesFromOCR(parseOCRNames(ocrData.getNameRefGrantor()), PType.GRANTOR, documentsManager, (RegisterDocumentI)documentI, infoToBeLogged)){
			TSServer.calculateAndSetFreeForm(documentI, SimpleChapterUtils.PType.GRANTOR, search.getID());
		}
		
		Vector<StructBookPage>	bpVec	= ocrData.getBookPageVector();
		Vector<Instrument>		iVec	= ocrData.getInstrumentVector();
		
		for(StructBookPage bp:bpVec){
			InstrumentI in = bp.toInstrument();
			processOcredInstrument( in, true);
			if( (in.hasBookPage()||in.hasDocNo()||in.hasInstrNo()) && !documentI.flexibleContainsParsedReference(in) ){
				documentI.addParsedReference(in);
			}
		}
		for(Instrument i:iVec){
			InstrumentI in = i.toInstrumet();
			processOcrInstrumentNumber(search.getID(), in);
			processOcredInstrument( in, false);
			if( (in.hasBookPage()||in.hasDocNo()||in.hasInstrNo()) && !documentI.flexibleContainsParsedReference(in) ){
				documentI.addParsedReference(in);
			}
		}
		
		if (documentI instanceof Mortgage) {
			Mortgage mortgage = (Mortgage) documentI;
			double existingOcrAmount = mortgage.getMortgageAmount();
			double ocrAmount = parseMortgageAmount(ocrData.getAmountVector());
			if((existingOcrAmount == 0 || existingOcrAmount == SimpleChapterUtils.UNDEFINED_DOUBLE_VALUE) && ocrAmount != 0 && ocrAmount != SimpleChapterUtils.UNDEFINED_DOUBLE_VALUE){
				mortgage.setMortgageAmount(ocrAmount);	
			}
			Vector<String> vec = ocrData.getAmountVector();
			StringBuilder build = new StringBuilder();
			for(String am:vec){
				build.append(am);
				build.append(" ");
			}
			String existingAmount = mortgage.getMortgageAmountFreeForm().trim();
			String amount = build.toString().trim();
			if(StringUtils.isEmpty(existingAmount) && !StringUtils.isEmpty(amount)){
				mortgage.setMortgageAmountFreeForm(amount);
			}
			
			String existingLoanNo = mortgage.getLoanNo();
			String loanNo = ocrData.getLoanNumber();
			if(StringUtils.isEmpty(existingLoanNo) && !StringUtils.isEmpty(loanNo)){
				mortgage.setLoanNo(loanNo);
			}
			
			List<NameI> grantees = parseOCRNames(ocrData.getNameRefGrantee());
			List<NameI> granteesNameMortgage = new ArrayList<NameI>();
			for (NameI nameI : grantees) {
				NameMortgageGranteeI nameMortgage = new NameMortgageGrantee(nameI);
				nameMortgage.setTrustee(false);
				granteesNameMortgage.add(nameMortgage);
			}
			
			boolean needRecalculateGranteeFreeForm = addNamesFromOCR(granteesNameMortgage, PType.GRANTEE, documentsManager, (RegisterDocumentI)documentI, infoToBeLogged); 
			grantees = parseOCRNames(ocrData.getNameRefGranteeTrustee());
			for (NameI nameI : grantees) {
				NameMortgageGranteeI nameMortgage = new NameMortgageGrantee(nameI);
				nameMortgage.setTrustee(true);
				granteesNameMortgage.add(nameMortgage);
			}
			
			needRecalculateGranteeFreeForm |= addNamesFromOCR(granteesNameMortgage, PType.GRANTEE, documentsManager, (RegisterDocumentI)documentI, infoToBeLogged);
			
			if(needRecalculateGranteeFreeForm){
				TSServer.calculateAndSetFreeForm(documentI, SimpleChapterUtils.PType.GRANTEE, search.getID());
			}
			
		} else if (documentI instanceof Transfer) {
			Transfer transfer = (Transfer) documentI;
			
			String gtee = ocrData.getVestingInfoGrantee();
			String gtor = ocrData.getVestingInfoGrantor();
			
			if(!StringUtils.isEmpty(gtee)){
				VestingInfo v1 = new VestingInfo();
				v1.setVesting(gtee.trim());
				v1.setFilledByOcr(true);
				transfer.setVestingInfoGrantee(v1);
			}
			
	    	if(!StringUtils.isEmpty(gtor)){
				VestingInfo v1 = new VestingInfo();
				v1.setVesting(gtor);
				v1.setFilledByOcr(true);
				transfer.setVestingInfoGrantor(v1);
			}
	    	
		}
		if(documentI instanceof RegisterDocumentI) {
			RegisterDocumentI regDoc = (RegisterDocumentI) documentI;
			
			String instrumentDate = ocrData.getInstrumentDate();
			String recordedDate = ocrData.getRecordedDate();
			String instrumentNumber = ocrData.getInstrumentNumber();	
			String book = ocrData.getBook();
			String page = ocrData.getPage();
			
			// Task 7659
//			if(search.getSearchType() != Search.AUTOMATIC_SEARCH) {
				SimpleDateFormat sdf = new SimpleDateFormat("MM/dd/yy");
				Date startViewDate = documentsManager.getStartViewDate();
				Date endViewDate = documentsManager.getEndViewDate();
				
				if(!StringUtils.isEmpty(instrumentDate)
						&& (regDoc.getInstrumentDate()==null/* || !regDoc.isInstDateEdited()*/) ) {
					try {
						String oldInstrDate = null;
						if(regDoc.getInstrumentDate() != null) {
							oldInstrDate = regDoc.getInstrumentDate().toString();
						}
						regDoc.setInstrumentDate(sdf.parse(instrumentDate));
						if(oldInstrDate != null && !oldInstrDate.equals(regDoc.getInstrumentDate().toString())) {
							TsdIndexPageServer.logAction("Change instrument date" + "-OldInstrumentDate=" + oldInstrDate, search.getID(), regDoc);
						}
					} catch (ParseException e) {
					}
				}
				
				if(!StringUtils.isEmpty(recordedDate)) {
					if(regDoc.getRecordedDate()==null/* || !regDoc.isRecDateEdited()*/) {
						try {
							Date ocredRecDate = sdf.parse(recordedDate);
							if(!(ocredRecDate.before(startViewDate) || ocredRecDate.after(endViewDate))) {
								String oldRecDate = null;
								if(regDoc.getRecordedDate() != null) {
									oldRecDate = regDoc.getRecordedDate().toString();
								}
								regDoc.setRecordedDate(ocredRecDate);
								if(oldRecDate != null && !oldRecDate.equals(regDoc.getRecordedDate().toString())) {
									TsdIndexPageServer.logAction("Change recorded date" + "-OldRecordedDate=" + oldRecDate, search.getID(), regDoc);
								}
							}
						} catch (ParseException e) {
						}
					}
					if(regDoc.getYear() < 0) {
						regDoc.setYear(sdf.getCalendar().get(Calendar.YEAR));
					}
				}
//			}
				
			try {
				boolean canChangeInstrumentNumber = !StringUtils.isEmpty(instrumentNumber)	
														&& (StringUtils.isEmpty(regDoc.getInstrument().getInstno()) || regDoc.getInstrument().isOverwritePermission());
				
				boolean canChangeBookPage =  !StringUtils.isEmpty(book) && !StringUtils.isEmpty(page)
											  && ( (StringUtils.isEmpty(regDoc.getInstrument().getBook()) && StringUtils.isEmpty(regDoc.getInstrument().getPage())) 
													  || regDoc.getInstrument().isOverwritePermission());
						
				if(canChangeInstrumentNumber || canChangeBookPage) {
					
					InstrumentI possibleInstrument = documentI.getInstrument().clone();
					possibleInstrument.setInstno(instrumentNumber);
					possibleInstrument.setPage(page);
					possibleInstrument.setBook(book);
					
					boolean instrumentAlreadyExists = documentsManager.getDocument(possibleInstrument) != null;
						
					possibleInstrument = documentI.getInstrument().clone();
					possibleInstrument.setInstno(instrumentNumber);
					boolean instrumentNoAlreadyExists = documentsManager.getDocument(possibleInstrument) != null;
						
					possibleInstrument = documentI.getInstrument().clone();
					possibleInstrument.setPage(page);
					possibleInstrument.setBook(book);
					boolean bookPageAlreadyExists = documentsManager.getDocument(possibleInstrument) != null;
														
					if(instrumentAlreadyExists) {
						
						canChangeInstrumentNumber = false;
						canChangeBookPage = false;					
						
						if(!instrumentNoAlreadyExists) {
							canChangeInstrumentNumber = true;
						}else {							
							if(!bookPageAlreadyExists) {
								canChangeBookPage = true;
							}
						}
						
						if(!canChangeInstrumentNumber) {
							infoToBeLogged.append("OCR: The extracted instrument for "+ documentI.getInstno()+ ": " + instrumentNumber + " already exists in tsr index.<br>");
						}
						if(!canChangeBookPage) {
							infoToBeLogged.append("OCR: The extracted book "+ book + " and page: " + page + " for "+ documentI.getInstno()+ " already exist in tsr index.<br>");	
						}
					}
				}
				
				if(canChangeInstrumentNumber || canChangeBookPage) {
					
					documentsManager.remove(documentI.getId());
						
					if(canChangeInstrumentNumber) {
						regDoc.getInstrument().setInstno(instrumentNumber);
					}
				
					if(canChangeBookPage) {
						regDoc.getInstrument().setBook(book);
						regDoc.getInstrument().setPage(page);
					}
					
					if(StringUtils.isEmpty(instrumentNumber)&&StringUtils.isNotEmpty(book)&&StringUtils.isNotEmpty(page)){
						if(regDoc.getInstrument().isOverwritePermission()){
							if(StringUtils.isEmpty(instrumentNumber)){
								regDoc.setInstno("");
							}
						}
					}
					
					if(StringUtils.isNotEmpty(instrumentNumber)&&StringUtils.isEmpty(book)&&StringUtils.isEmpty(page)){
						if(regDoc.getInstrument().isOverwritePermission()){
							if(StringUtils.isEmpty(book)){
								regDoc.setBook("");
								regDoc.setPage("");
							}
						}
					}
					
					documentsManager.add(documentI);
				}
				
				
				
			}catch(Exception e) {
				e.printStackTrace();
			}
			
			String legal = ocrData.getLegalDescriptionFormated();
			LegalDescriptionI docLegal = regDoc.getLegalDescription();
	    	if(docLegal == null) {
	    		docLegal = new LegalDescription();
	    		docLegal.setFilledByOcr( true );
	    		regDoc.setLegalDescription(docLegal);
	    	}
			if( !StringUtils.isEmpty(legal) ){
				docLegal.setLegal( legal.trim() );
				docLegal.setFilledByOcr( true );
			}
			docLegal.setCondo(ocrData.isCondo());
			docLegal.setUnitVector(ocrData.getUnitVector());
		}
		
		if(!(documentI instanceof MortgageI)) {
			if(addNamesFromOCR(parseOCRNames(ocrData.getNameRefGrantee()), PType.GRANTEE, documentsManager, (RegisterDocumentI)documentI, infoToBeLogged)){
				TSServer.calculateAndSetFreeForm(documentI, SimpleChapterUtils.PType.GRANTEE, search.getID());
			}
		}
		logger.info("SearchID: " + search.getID() + " OCR completed for document " + documentI.getInstno()); 
		
		if(documentI instanceof TransferI && getDataSite() != null && getDataSite().getStateAbbreviation().equals("TN")) {
			Transfer transfer = (Transfer) documentI;
			
			String gtee = ocrData.getVestingInfoGrantee();
			String gtor = ocrData.getVestingInfoGrantor();
			
			if (org.apache.commons.lang.StringUtils.isNotBlank(gtee) && !gtee.equals(transfer.getGranteeFreeForm()) && !transfer.isFieldModified(DocumentI.Fields.GRANTEE)) {
				transfer.setFieldModified(DocumentI.Fields.GRANTEE);
				transfer.setGranteeWithLimit(gtee);
				infoToBeLogged.append("Saving vesting info grantee <b>" + gtee + "</b> on  transfer.<br>");
			}
			
			if (org.apache.commons.lang.StringUtils.isNotBlank(gtor) && !gtor.equals(transfer.getGrantorFreeForm()) && !transfer.isFieldModified(DocumentI.Fields.GRANTOR)) {
				transfer.setFieldModified(DocumentI.Fields.GRANTOR);
				transfer.setGrantorWithLimit(gtor);
				infoToBeLogged.append("Saving vesting info grantor <b>" + gtor + "</b> on  transfer.<br>");
			}
			
		}
		
    }

	protected boolean addNamesFromOCR(List<NameI> namesFromOCR, PType nameType, DocumentsManagerI documentsManager, RegisterDocumentI documentI, StringBuilder infoToBeLogged) {
		return documentsManager.addNamesFromOCR(namesFromOCR, documentI, nameType, infoToBeLogged);
	}
    
    public void updateDocumentWithOCRData(DocumentsManagerI documentsManager, DocumentI documentI, OCRParsedDataStruct ocrData,
    		String notFuckingUsed, Search search, StringBuilder infoToBeLogged, boolean isManualOcr) {
    	long searchId 			= search.getID();
    	int serverId 			= (int)documentI.getSiteId();
    	TSInterface tsserver	= null;
    	
		if( documentsManager.contains(documentI) ){
			if(!documentI.isUploaded()) {
				tsserver = TSServersFactory.GetServerInstance(serverId, searchId);
				if(tsserver == null) {
					tsserver = this;
				}
			} else {
				tsserver = this;
			}
			tsserver.updateDocumentWithOCRData(documentsManager, documentI, ocrData, search, infoToBeLogged);
		} else {
			logger.info("SearchID: " + searchId + " OCR completed BUT document does NOT exist anymore" + documentI.getInstno());
		}
	}
    
	@Deprecated
	public void processOcrInstrumentNumber(long searchId, InstrumentI instr) {
		
		String state =InstanceManager.getManager().getCurrentInstance(searchId).getCurrentState().getStateAbv();
    	String county=InstanceManager.getManager().getCurrentInstance(searchId).getCurrentCounty().getName();

    	
    	if("IL".equalsIgnoreCase(state) && "Du Page".equalsIgnoreCase(county)) {
    		ILDuPageRO.fixInstrumentForOcr(instr);
    	}
		
	}

	/*
	private static void manageLastTransferOCRData(TransferI lastRealTransfer, OCRParsedDataStruct ocrData, Search currentSearch) {
    	long searchId 		= currentSearch.getID();
    	SearchAttributes sa = currentSearch.getSa();
    	int serverId 		= (int)lastRealTransfer.getSiteId();
    	DataSite dat 		= HashCountyToIndex.getDateSiteForMIServerID( 
    			InstanceManager.getManager().getCommunityId(searchId), 
    			serverId  );
    	TSInterface tsserver= null;
    	boolean boootstrapEnabled	=	ro.cst.tsearch.searchsites.client.Util.isSiteEnabledBootStrap(dat.getEnabled());
    	 
    	
	}*/

    /*
     * se va implementa in asa fel incat sa tina cont de restrictiile pentru un anumit county + site + tip chapter
     */
    private boolean testIfNeedToDownloadAndScanImage(DocumentI document, boolean isManualOcr) {
    	if(	document == null 
    			|| (!document.hasImage()) 
    			|| StringUtils.isEmpty(document.getImage().getPath())) {
			return false;
		}
		if(isManualOcr) {
			return true;
		}else if(document.getImage().isOcrDone()) {
			return false;
		}
		return true;
	}

    public void setDocsValidator(int type, DocsValidator defaultOne) {
        setDocsValidator(DocsValidator.getInstance(
                manageDocsValidatorType(type), mSearch, defaultOne,searchId));
        
        addDocsValidator( type, defaultOne );
    }
    
    public void setCrossRefDocsValidator(int type, DocsValidator defaultOne){
    	setCrossRefDocsValidator(DocsValidator.getInstance(
    			manageDocsValidatorType(type), mSearch, defaultOne, searchId));
    	
    	addCrossRefDocsValidator(type, defaultOne);
    }
    
    public void addDocsValidator(DocsValidator type, DocsValidator defaultOne) {
		if (docsValidators == null) {
			docsValidators = new Vector<DocsValidator>();
		}
		docsValidators.add(type);
	}

	public void addDocsValidator(int type, DocsValidator defaultOne) {
		if (docsValidators == null) {
			docsValidators = new Vector<DocsValidator>();
		}

		DocsValidator validatorInstance = DocsValidator.getInstance(manageDocsValidatorType(type), mSearch, defaultOne, searchId);
		docsValidators.add(validatorInstance);
	}
    
    public void addCrossRefDocsValidator( DocsValidator type, DocsValidator defaultOne ){
    	if( crossRefDocsValidators == null ){
    		crossRefDocsValidators = new Vector<DocsValidator>();
    	}
    	   	
   		crossRefDocsValidators.add( type );

    }
    
    public void addCrossRefDocsValidator( int type, DocsValidator defaultOne ){
    	if( crossRefDocsValidators == null ){
    		crossRefDocsValidators = new Vector<DocsValidator>();
    	}
    	
    	DocsValidator validatorInstance = DocsValidator.getInstance( manageDocsValidatorType(type), mSearch, defaultOne,searchId);
    	
    	/*boolean addValidator = true;
    	
    	for( int i = 0 ; i < crossRefDocsValidators.size() ; i ++ ){
    		String currentValidatorName = ((DocsValidator)crossRefDocsValidators.elementAt( i )).getClass().getSimpleName();
    		
    		if( currentValidatorName.equals( validatorInstance.getClass().getSimpleName() ) ){
    			addValidator = false;
    			break;
    		}
    	}
    	
    	if( addValidator ){*/
    	crossRefDocsValidators.add( validatorInstance );
    	//}
    }

    public static void splitResultRows(Parser p, ParsedResponse pr,
            String htmlString, int pageId, String linkStart, int action, long searchId)
            throws ServerResponseException {
        //dupa ce se impl toate site-urile n-ar strica ca fc sa fie abstracta
        ParsedResponse pr2 = new ParsedResponse();
        pr2.setError("This method should be implemented in children classes");
        throw new ServerResponseException(pr2);
    }

    /**
     * @return
     */
    public long getStartTime() {
        return startTime;
    }

    /**
     * @return
     */
    public long getStopTime() {
        return stopTime;
    }

    /**
     * @param l
     */
    public void setStartTime(long l) {
        startTime = l;
    }

    /**
     * @param l
     */
    public void setStopTime(long l) {
        stopTime = l;
    }

    protected boolean isOverwriteDocNew (String doc) {
        
        if (this instanceof TNShelbyDN)
            return isFromDN(doc);
            
        /*if (this instanceof MOJacksonRO || this instanceof KSJohnsonRO || this instanceof MOClayRO)
            return isFromOR(doc);*/
            
        return isFromRO(doc) || isFromOtherRoLike(doc);
    }
    
    private boolean isFromOtherRoLike(String doc){
    	//mSearch.get
    	Search sx =	mSearch;
        Hashtable hashDT = sx.getMiscDocs("DT");
        Hashtable hashRV = sx.getMiscDocs("RV");
        Hashtable hashTS = sx.getMiscDocs("TS");
        Hashtable hashSF = sx.getMiscDocs("SF");
        
        String tempHashRVString = hashRV.toString();
        String tempHashDTString = hashDT.toString();
        String tempHashTSString = hashTS.toString();
        String tempHashSFString = hashSF.toString();
        
        if (tempHashDTString.indexOf(doc) != -1|| tempHashRVString.indexOf(doc) != -1
        		||tempHashTSString.indexOf(doc) != -1
        		||tempHashSFString.indexOf(doc) != -1) // daca doc este de pe RO...
			return true;
        
        try {
			//the next part must be done because now we also keep, where available,
			//...information about the doctype of the document 
			int index = doc.indexOf(".");
			if(index > 0){
				String id = doc.substring(0,index);
				String extension = "\\" + doc.substring(index);
				
				if( extension.substring(2).matches("\\w+")
						&& tempHashRVString.matches(".*" + id + "\\w+" + extension + ".*") ||
						tempHashDTString.matches(".*" + id + "\\w+" + extension + ".*")
						|| tempHashTSString.matches(".*" + id + "\\w+" + extension + ".*")
						|| tempHashSFString.matches(".*" + id + "\\w+" + extension + ".*"))
					return true;
			}
		} catch (Exception e) {
		}
        
    	return false;
    }
    
    public boolean isFromOR(String doc) {
        
        Search sx =	mSearch;
        Hashtable hashOR = sx.getMiscDocs("OR");
		
        if (hashOR == null) // daca nu am nici un doc de pe OR...
            return false;
            
        
		if (hashOR.toString().indexOf(doc) != -1) // daca doc este de pe OR...
			return true;
	   		
        return false;
    }
    
	protected boolean isFromRO(String doc) {
         				
        Search sx =	mSearch;
        Hashtable hashRO = sx.getRODocs();
		
        if (hashRO == null) // daca nu am nici un doc de pe RO...
            return false;
            
        String tempHashRoString = hashRO.toString(); 
		if (tempHashRoString.indexOf(doc) != -1) // daca doc este de pe RO...
			return true;
		try {
			//the next part must be done because now we also keep, where available,
	        //...information about the doctype of the document 
			int index = doc.indexOf(".");
			if(index > 0){
				String id = doc.substring(0,index);
				String extension = "\\" + doc.substring(index);
				
				if(extension.substring(2).matches("\\w+")
						&& tempHashRoString.matches(".*" + id + "\\w+" + extension + ".*"))
					return true;
			}
		} catch (Exception e) {
		}
	   		
        return false; 
    }

	/*
	public boolean isROServerType(){
	    
		DataSite dat = HashCountyToIndex.getDateSiteForMIServerID(getCommunityId(), miServerID);
		
		int cityChecked =0; 
		try{
			cityChecked = Integer.parseInt(dat.getCityChecked()) ;
		}
		catch(Exception e){
			e.printStackTrace();
			return false;
		}
		
		for(int i=0;i<RO_LIKE_TYPE.length;i++){
			if(	RO_LIKE_TYPE[i]==cityChecked ){
				return true;
			}
		}
		
		return false;
	}
	*/
	/*
	public static boolean isGBServerType(int serverId){
		    
			DataSite dat = HashCountyToIndex.getDateSiteForMIServerID(serverId);
			
			int cityChecked =0; 
			try{
				cityChecked = Integer.parseInt(dat.getCityChecked()) ;
			}
			catch(Exception e){
				e.printStackTrace();
				return false;
			}
			
			for(int i=0;i<GB_LIKE_TYPE.length;i++){
				if(	GB_LIKE_TYPE[i]==cityChecked ){
					return true;
				}
			}
			
			return false;
	}
	*/
	/*
	public boolean isTRServerType(){
	    
	    DataSite dat = HashCountyToIndex.getDateSiteForMIServerID(miServerID);
		
		try{
			if( Integer.parseInt(dat.getCityChecked()) == SiteData.TR_TYPE){
				return true;
			}
		}
		catch(Exception e){
			e.printStackTrace();
		}
		
	    return false;
	}
	*/
	/*
	public boolean isEPServerType(){
	    
	    DataSite dat = HashCountyToIndex.getDateSiteForMIServerID(miServerID);
		
		try{
			if( Integer.parseInt(dat.getCityChecked()) == SiteData.EP_TYPE){
				return true;
			}
		}
		catch(Exception e){
			e.printStackTrace();
		}
		
	    return false;
	}	
	*/
	
	
	
	
	private boolean isFromDN(String doc) {
        
        
        				
        Search sx =	mSearch;
        Hashtable hashDN = sx.getDNDocs();
		        
        // boolean isDN = false;
		
        if (hashDN == null) // daca nu am nici un doc de pe DN...
            return false;
            
        
		if (hashDN.toString().indexOf(doc) != -1) // daca doc este de pe DN...
			return true;
	   		
        return false; 
    }

	
    public boolean isOverwriteDoc(String fName) {
        String instrNo = fName;
        instrNo = (instrNo.lastIndexOf(".") > 0 ? instrNo.substring(0, instrNo
                .lastIndexOf(".")) : instrNo);
        List instrList = (List) getSearch().getSa().getObjectAtribute(
                        SearchAttributes.INSTR_LIST);
        for (Iterator iter = instrList.iterator(); iter.hasNext();) {
            Instrument element = (Instrument) iter.next();
            if (element.getInstrumentNo().equals(instrNo)
                    && element.isOverwrite()) {
                
                element.setOverwrite(false);
                return true;
            }
        }

        ServerSearchesIterator ssi = getSearch().getSearchIterator();

        if (ssi != null) {
            TSServerInfoModule crtMod = (TSServerInfoModule) ssi.current();
            if (crtMod != null)
                if ((crtMod.getIteratorType() == ModuleStatesIterator.TYPE_INSTRUMENT_LIST
                        || crtMod.getIteratorType() == ModuleStatesIterator.TYPE_INSTRUMENT_LIST_SEARCH
                        || crtMod.getIteratorType() == ModuleStatesIterator.TYPE_BOOK_PAGE_LIST || crtMod
                        .getIteratorType() == ModuleStatesIterator.TYPE_BOOK_PAGE_LIST_SEARCH)
                        && !crtMod.hasFakeFunctions()
                        && (crtMod.getModuleIdx() != TSServerInfo.BOOK_AND_PAGE_LOCAL_MODULE_IDX)
                        && (crtMod.getModuleIdx() != TSServerInfo.PARCEL_ID_MODULE_IDX)) {
                    onBeforeAlreadyProcessCheck( fName );
                    if (FileAlreadyExist(fName)) {
                        return false;
                    }
                    return true;
                }
        }

        return false;
    }

    public static String processQry(String in) {
        return in;
    }

    public TSConnectionURL getTSConnection() {
        if (mTSConnection == null)
            mTSConnection = new TSConnectionURL();

        return mTSConnection;
    }
    
    public synchronized Object clone() {
        
        try {
            
            TSServer server = (TSServer) super.clone();
            
            try { server.msRequestSolverName = new String(msRequestSolverName); } catch (Exception ignored) {}
            try { server.msServerID = new String(msServerID); } catch (Exception ignored) {}
            try { server.msPrmNameLink = new String(msPrmNameLink); } catch (Exception ignored) {}
            try { server.msSitePath = new String(msSitePath);  } catch (Exception ignored) {}
            try { server.msSaveToTSDFileName = new String(msSaveToTSDFileName); } catch (Exception ignored) {}
            try { server.msLastLink = new String(msLastLink); } catch (Exception ignored) {}
            try { server.msSaveToTSDResponce = new String(msSaveToTSDResponce); } catch (Exception ignored) {}
            try { server.msSiteRealPath = new String(msSiteRealPath); } catch (Exception ignored) {}

            //
            server.mSearch = null;
            server.parser = null;
            
            server.attributes = attributes;
            try { server.msiServerInfo = (TSServerInfo) msiServerInfo.clone(); } catch (Exception ignored) {}
            server.docsValidator = docsValidator;
            server.crossRefDocsValidator = crossRefDocsValidator;

            // server connection
            try { server.mTSConnection = (TSConnectionURL) mTSConnection.clone(); } catch (Exception ignored) {}
            
            // scalari
            server.miGetLinkActionType = miGetLinkActionType;
            server.resultType = resultType;
            server.miServerID = miServerID;
            server.docsValidatorType = docsValidatorType;
            server.startTime = startTime;
            server.stopTime = stopTime;
            
            return server;
            
        } catch (CloneNotSupportedException cnse) {
            throw new InternalError();
        }
    }
    
    public void resetServerSession()
    {
        
    }
    
    /**
     * not used yet - not finished also
     * @param Response
     * @param msServerID
     */
    public void replaceImageFromDT(ServerResponse Response, String msServerID){
    	try{
    		// identify server county and server type
    		Matcher serverIdS = serverIdPattern.matcher( msServerID );
    		if(!serverIdS.find()){ return; }    		
    		

            // get book & page
            String book = null, page = null, instrument = null;            
            book = ((SaleDataSet)((Vector)Response.getParsedResponse().infVectorSets.get("SaleDataSet")).elementAt(0)).getAtribute( "Book" );
            page = ((SaleDataSet)((Vector)Response.getParsedResponse().infVectorSets.get("SaleDataSet")).elementAt(0)).getAtribute( "Page" );
            instrument = ((SaleDataSet)((Vector)Response.getParsedResponse().infVectorSets.get("SaleDataSet")).elementAt(0)).getAtribute( "InstrumentNumber" );
            
            // check that we have both book & page
            if(book != null && !"".equals(book) && page != null && !"".equals(page)){
            	
            	// get search module
                TSInterface roServerInterface = TSServersFactory.GetServerInstance( serverIdS.group( 1 ), "1" ,searchId);
                roServerInterface.setServerForTsd(getSearch(), msSiteRealPath);
                TSServerInfo roServerInfo = roServerInterface.getDefaultServerInfo();
                TSServerInfoModule searchByBookPage = roServerInterface.getSearchByBookPageModule(roServerInfo, book, page, instrument);
                if( searchByBookPage == null ){
                    return;
                }            	
                
        		// perform the search
        		SearchAttributes sa = getSearch().getSa();    
                roServerInterface.setStartTime( System.currentTimeMillis() );
                ServerResponse sr = roServerInterface.performAction(TSServer.REQUEST_SEARCH_BY,  null, searchByBookPage, new SearchDataWrapper(sa));

                // use the data
                if( sr.getParsedResponse().getResultRows().size() > 0 ){
                	String pageResponse = ((ParsedResponse) sr.getParsedResponse().getResultRows().elementAt( 0 )).getResponse();
                	String initialPage = Response.getParsedResponse().getResponse();
                	Response.getParsedResponse().setResponse( initialPage + pageResponse);
                }
            }
    		
    	}catch(Exception e){
    		e.printStackTrace();
    	}
    }
    
    public TSServerInfoModule getSearchByBookPageModule(TSServerInfo serverInfo, String book, String page, String instrumentNo)
    {
        return null;
    }
    
    /**
     * 
     * @param serverInfo - TSSerferInfo obtained via getDefaultServerInfo
     * @param book - book# we will search with
     * @param page - page# we will search with
     * @return the book - page search module for this TSServer
     * will be overridden in each implementing TSServer
     */
    public TSServerInfoModule getBookPageModule(TSServerInfo serverInfo, String book, String page){
    	return null;
    }

    /**
     * 
     * @param serverInfo - TSSerferInfo obtained via getDefaultServerInfo
     * @param book - book# we will search with
     * @param page - page# we will search with
     * @return the book - page search module for this TSServer
     * will be overridden in each implementing TSServer
     * 
     * for rutherford
     */
    public TSServerInfoModule getBookPageModule2(TSServerInfo serverInfo, String book, String page){
    	return null;
    }
    
    /**
     * 
     * @param serverInfo - TSSerferInfo obtained via getDefaultServerInfo
     * @param instrumentNo - instrumentNo# we will search with
     * @return the book - page search module for this TSServer
     * will be overridden in each implementing TSServer
     */
    public TSServerInfoModule getInstrumentModule(TSServerInfo serverInfo, String instrumentNo){
    	return null;
    }
    
    public TSServerInfoModule getLastTransferSearchModule(TSServerInfo serverInfo, String book, String page, String instrumentNo)
    {
    	//get book - page search module or instrument# search module according to case
    	
    	if( !"".equals( book ) && !"".equals( page ) ){
    		//get book page module
    		return getBookPageModule(serverInfo, book, page);
    	}
    	else if( !"".equals( instrumentNo ) ){
    		//get instrument# search module
    		return getInstrumentModule(serverInfo, instrumentNo);
    	}
    	
    	//no valid book-page or instrument# passed to function
        return null;
    }
    
    /**
     * Adds the moduleToAdd automatic module after each module already in the list
     * @param automaticSearchList - the list of automatic search modules
     * @param serverInfo - TSServerInfo
     * @param moduleToAdd - module to add after each module already in the list
     * @return the new list of automatic search modules
     */
    public static List<TSServerInfoModule> addModuleInterleaved( List automaticSearchList, TSServerInfo serverInfo, TSServerInfoModule moduleToAdd )
    {
        List<TSServerInfoModule> newList = new ArrayList<TSServerInfoModule>();
        
        Iterator it = automaticSearchList.iterator();
        if( it.hasNext() )
        {
            while( it.hasNext() )
            {
                newList.add( (TSServerInfoModule)it.next() );
                newList.add(moduleToAdd);
            }
        }
        else
        {
            newList.add( moduleToAdd );
        }
        
        return newList;
    }
    
    public String addDocLinks( String initialResponse, String beginWith )
    {
        Matcher docLinkMatcher = docLinkPattern.matcher( initialResponse );
        while( docLinkMatcher.find() ){
            String inputStr = docLinkMatcher.group( 0 );
            initialResponse = initialResponse.replace( inputStr, "<input type=\"checkbox\" name=\"docLink\" value=\"" + docLinkMatcher.group( 1 ) + "\"   >" );
        }
        
        return initialResponse;
    }
    
//    public String addRememberDocLinks( String initialResponse, String beginWith )
//    {
//        Matcher docLinkMatcher = docLinkPattern.matcher( initialResponse );
//        String checked = "";
//        while( docLinkMatcher.find() ){
//            checked = "";
//            String inputStr = docLinkMatcher.group( 0 );
//            String ajaxDocumentLink = docLinkMatcher.group( 1 );
//            try
//            {
//                ajaxDocumentLink = URLEncoder.encode( ajaxDocumentLink, "UTF-8" );
//            }catch( Exception e ) {}
//            
//            if( mSearch.isClickedDocumentBeginWith( docLinkMatcher.group( 1 ), beginWith ) )
//            {
//                checked = "checked";
//            }
//            
//            initialResponse = initialResponse.replace( inputStr, "<input type=\"checkbox\" name=\"docLink\" value=\"" + docLinkMatcher.group( 1 ) + "\" onClick=\"javascript: checkDocumentAJAX( '" + ajaxDocumentLink + "', '" + beginWith + "' )\" " + checked + " >" );
//        }
//        
//        return initialResponse;
//    }
    
    
    /**
     * Obtain current server name. If unknown, return "unknown"
     * @return
     */
	public static String getCrtTSServerClassName(long searchId){
		try{
			CurrentInstance currentInstance = InstanceManager.getManager().getCurrentInstance(searchId);
			Search search = currentInstance.getCrtSearchContext();
			return TSServersFactory.getTSServerInstanceClassName(currentInstance.getCommunityId(), search.getP1(),search.getP2());			
		} catch(Exception e) {
			return "unknown";
		}
	}
	
	 /**
     * Obtain current server name. If unknown, return "unknown"
	 * @param isParentSite TODO
     * @return
     */
	public static String getCrtTSServerName(long searchId, boolean isParentSite){
		try{
			CurrentInstance currentInstance = InstanceManager.getManager().getCurrentInstance(searchId);
			Search search = currentInstance.getCrtSearchContext();
			
			String p1 = isParentSite?search.getP1ParentSite():search.getP1();
			String p2 = isParentSite?search.getP2ParentSite():search.getP2();
			
			return TSServersFactory.getTSServerName(currentInstance.getCommunityId(),
					p1,
					p2);			
		} catch(Exception e) {
			return "unknown";
		}
	}
	
	public static String getCrtTSServerName(int miServerId){
		try {
			return TSServersFactory.getTSServerName(HashCountyToIndex.ANY_COMMUNITY, miServerId);
		} catch (Exception e) {
			return "unknown";
		}
	}
	
    /**
     * write image file to client web browser
     * @param fileName
     * @param contentType
     */
    protected boolean writeImageToClient(String fileName, String contentType){   
    	
    	// check if the file exists
    	if(StringUtils.isEmpty(fileName)){
    		logger.error("File Not Specified!");
    		return false; 
    	}
    	File file = new File(fileName);    	
    	if(!file.exists()) {
    		logger.error("File Not Found: " + fileName);
    		return false; 
    	}
    	
    	boolean retVal = true;
    	
    	// write the file to the client
	    HttpServletResponse servletResponse = InstanceManager.getManager().getCurrentInstance(searchId).getHttpResponse();
		servletResponse.setContentType(contentType);
		servletResponse.setContentLength((int)file.length());
		OutputStream outputStream = null;
		InputStream inputStream = null;
		try{
			outputStream = servletResponse.getOutputStream();
			inputStream = new BufferedInputStream(new FileInputStream(file));			
			byte [] buf = new byte[8192];
			int read = 0;
			do{
				read = inputStream.read(buf);
				if(read != -1){
					outputStream.write(buf, 0, read);
				}
			} while (read != -1);
		}catch(IOException e){
			// something went wrong with retrieving the image from hdd
			logger.error("IO Exception", e);
			retVal = false;
		}finally{
			// close input stream - log exception if failed
			if(inputStream != null){
				try{
					inputStream.close();
				} catch(IOException e){
					logger.error("Stream Close Exception",e);
				}
			}
			// close output stream - log exception if failed
			if(outputStream != null){
				try{
					outputStream.flush();
					outputStream.close();
				} catch(IOException e){
					logger.error("Stream Close Exception",e);
				}			
			}
		}
		
		return retVal;
    }

    /**
     * get current search. used to make code smaller/clearer
     */

    public Search getSearch(){
    	if(mSearch == null)
    		return (mSearch = InstanceManager.getManager().getCurrentInstance(searchId).getCrtSearchContext());
    	return mSearch;
    }
    
    protected CurrentInstance getCurrentInstance(){    
    	return InstanceManager.getManager().getCurrentInstance(searchId);
    }
    /**
     * get current search folder. used to make code smaller/clearer
     */
    protected String getCrtSearchDir(){
    	return getSearch().getSearchDir();    	
    }
    
    /**
     * get current search attributes. used to make code smaller/clearer
     */
    protected SearchAttributes getSearchAttributes(){
    	return getSearch().getSa();    	
    }
    
    /**
     * get current search attribute. used to make code smaller/clearer
     */
    protected String getSearchAttribute(String key){
    	return getSearchAttributes().getAtribute(key);    	
    }
    
    protected boolean hasOwner(){
    	return getSearchAttributes().hasOwner();
    }
    
    protected boolean hasBuyer(){
    	return getSearchAttributes().hasBuyer();
    }
 
    public  String getExtraParameterValueAtTSRImageDownloadLink() {
		return null;
	}
    
    public String getExtraParameterNameAtTSRImageDownloadLink() {
		return null;
	}
    
    /**
     * Obtain http site instance associated with the current search 
     * @return
     */
    public HTTPSiteInterface getSearchHttpSite(){ 
    	HTTPSiteInterface httpSite = null;
		try{
			String ssiteID =  getClass().getSimpleName();
			String siteKey = getSearch().getSearchID() + ssiteID;
			httpSite = (HTTPSiteInterface)InstanceManager.getManager().getCurrentInstance(searchId).getSite(siteKey);
			if(httpSite == null){
				httpSite = (HTTPSiteInterface)HTTPManager.getSite(ssiteID, siteKey, searchId,miServerID);
			}			
			return httpSite;
		}catch(HTTPManagerException e){
			logger.error(e);
			return null;
		}
    }
    
    /**
     * Must be overridden to have effect
     */
    public ServerResponse removeUnnecessaryResults( ServerResponse sr ){
    	return sr;
    }

	public boolean isGoBackOneLavel() {
		return goBackOneLevel;
	}

	public void setGoBackOneLevel(boolean goBackOneLavel) {
		this.goBackOneLevel = goBackOneLavel;
	}
    
    /**
     * Extracts parameters from a search module
     * @param module search module
     * @return map with parameters
     */
    protected static Map<String,String> getParams(TSServerInfoModule module){
    	Map<String,String> retVal = new HashMap<String,String>();
        Map map = module.getParamsForQuery();
        for(Object key: map.keySet()){
            Object value = map.get(key);
            if(key instanceof String && value instanceof String){
            	retVal.put((String)key, (String)value);               
            } else if(key instanceof TSServerInfoParam && value instanceof TSServerInfoParam){
                TSServerInfoParam param = (TSServerInfoParam)key;
                retVal.put(param.name, param.value);
            }
        }
        return retVal;
    }
     
    protected static Map<String,String> getNonEmptyParams(TSServerInfoModule module, Map<String, String[]> newParam){
    	Map<String,String> retVal = new HashMap<String,String>();
        Map map = module.getParamsForQuery();
        for(Object key: map.keySet()){
            Object value = map.get(key);
            if(key instanceof String && value instanceof String){
            	if(!"".equals(value)){
            		retVal.put((String)key, ((String)value).trim());
            	}
            } else if(key instanceof TSServerInfoParam && value instanceof TSServerInfoParam){
                TSServerInfoParam param = (TSServerInfoParam)key;
                if(param.value!=null && !"".equals(param.value)){
                	retVal.put(param.name, param.value.trim());
                }
            }
        }
        
        if(newParam!=null){
        	for(TSServerInfoFunction func:module.getFunctionList()){
        		
        		String[] values = func.getParamValues();
        		
        		if(!StringUtils.isEmpty(values)){
        			newParam.put(func.getParamName(), values);
        		}
        		
        	}
        	
        }
        
        return retVal;
    }

	public boolean isParentSite() {
		return parentSite;
	}

	public void setParentSite(boolean parentSite) {
		this.parentSite = parentSite;
	}
	
	public String getCurrentServerName(){
		return getDataSite().getName();
	}
	
	protected boolean isCrossRefToBeValidated(String type) {
		if(type.equals(""))
			return true;
		return !CrossRefInitSingleton.getInstance().isNoValidation(type);
	}
	
	private void updateOcrCrossRefWithSources(ServerResponse resp) {
    	
    	String docType = resp.getCrossRefSourceType();
    	ParsedResponse pr = resp.getParsedResponse();    	
		Vector<ParsedResponse> rows = pr.getResultRows();
		boolean anyChange = false;
		for (ParsedResponse response : rows) {
			if(response.getPageLink() != null) {
				if(response.getPageLink().getLink() != null) {
					if(response.getPageLink().getLink().contains("crossRefSource")){
						
						response.getPageLink().setOnlyLink(
								response.getPageLink().getLink().replaceFirst("&crossRefSource=" + docType,"&crossRefSource=OCR"));
						anyChange = true;
					}
				}
				if(response.getPageLink().getOriginalLink() != null) {
					if(response.getPageLink().getOriginalLink().contains("crossRefSource")){
						response.getPageLink().setOnlyOriginalLink(
								response.getPageLink().getOriginalLink().replaceFirst("&crossRefSource=" + docType,"&crossRefSource=OCR"));
						anyChange = true;
					}
				}
			}
		}
		if(anyChange)
			pr.setOnlyResultRows(rows);
	}
	
	public static boolean isRoLike(int id,boolean isMiServerID){
		try {
			if(isMiServerID){
				DataSite datasite = HashCountyToIndex.getDateSiteForMIServerID(HashCountyToIndex.ANY_COMMUNITY, id);
				return isRoLike(datasite.getCityCheckedInt());
			}
			return isRoLike(id);
		} catch (Exception e) {
			logger.error("Error in isROLike - returning true since there are more RO like sites", e);
			return true;
		}
	}
	
	public static boolean isRoLike(int id,boolean isMiServerID, int commId){
		try {
			if(isMiServerID){
				DataSite datasite = HashCountyToIndex.getDateSiteForMIServerID(commId, id);
				return isRoLike(datasite.getCityCheckedInt());
			}
			return isRoLike(id);
		} catch (Exception e) {
			logger.error("Error in isROLike - returning true since there are more RO like sites", e);
			return true;
		}
	}
	
	 
	public static boolean isRoLike(int type){
		for(int i=0;i<RO_LIKE_TYPE.length;i++){
			if(	RO_LIKE_TYPE[i]==type ){
				return true;
			}
		}
		return false;
	}
	public static boolean isGBLike(int type){
		for(int i=0;i<GB_LIKE_TYPE.length;i++){
			if(	GB_LIKE_TYPE[i]==type ){
				return true;
			}
		}
		return false;
	}
	public static boolean isRoLike(String  type){
		
		String []entry = HashCountyToIndex.getServerTypesAbrev();
		for(int i=0;i<RO_LIKE_TYPE.length;i++){
			if(	 entry[i].equals(type) ){
				return isRoLike(i);
			}
		}
		return false;
	}

	public boolean isPropertyOrientedSite(){
		int typeInt = this.getDataSite().getSiteType();
		
		try {
			for (int i = 0; i < TAX_LIKE_TYPE.length; i++) {
				if (TAX_LIKE_TYPE[i] == typeInt) {
					return true;
				}
			}
			for (int i = 0; i < ASSESSOR_LIKE_SITES.length; i++) {
				if (ASSESSOR_LIKE_SITES[i] == typeInt) {
					return true;
				}
			}
			for (int i = 0; i < CITYTAX_LIKE_SITES.length; i++) {
				if (CITYTAX_LIKE_SITES[i] == typeInt) {
					return true;
				}
			}
		} catch (Exception e) {
			return resultType == UNIQUE_RESULT_TYPE;
		}
		return false;
	}
	
	public boolean isRangeNotExpanded() {
		return rangeNotExpanded;
	}

	public void setRangeNotExpanded(boolean rangeNotExpanded) {
		this.rangeNotExpanded = rangeNotExpanded;
	}
	
	protected int getResultType(){
		return resultType;
	}
	
	public boolean isAoLike(){
		return getResultType() == UNIQUE_RESULT_TYPE;
	}
	
	public static List<NameI> defaultParseOCRNames(OCRParsedDataStruct opds) {
		List<NameI> ret= new ArrayList<NameI>();

		Vector<String>  ocrNames=opds.getNameRefGrantee();
		for (String element : ocrNames) {
			NameTokenList brokenName=NameParser.parseNameFML(element);
			Name name = null;
			if(NameUtils.isCompany(element)) {
				name = new Name("", "", element);
				name.setCompany(true);
			} else {
				String[] names = GenericFunctions.extractSuffix(brokenName.getMiddleNameAsString());
				name = new Name(
						brokenName.getFirstNameAsString(),
						names[0],
						brokenName.getLastNameAsString());
				name.setSufix( names[1] );
				names = GenericFunctions.extractType(brokenName.getMiddleNameAsString());
				name.setNameType(names[1]);
				names = GenericFunctions.extractOtherType(brokenName.getMiddleNameAsString());
				name.setNameOtherType(names[1]);
			}
			ret.add(name);
		}
		return ret;
	}
	
	public List<NameI> parseOCRNames(OCRParsedDataStruct opds) {
		return defaultParseOCRNames(opds);
	}
	
	public List<NameI> parseOCRNames(Vector<String> ocrNames) {
		List<NameI> ret= new ArrayList<NameI>();
		for (String element : ocrNames) {
			NameTokenList brokenName=NameParser.parseNameFML(element);
			String[] names = GenericFunctions.extractSuffix(brokenName.getMiddleNameAsString());
			Name name = new Name(
					brokenName.getFirstNameAsString(),
					names[0],
					brokenName.getLastNameAsString());
			name.setCompany((name.getFirstName().length() == 0) && (name.getMiddleName().length() == 0) && NameUtils.isCompany(name.getLastName()));
			name.setSufix( names[1] );
			names = GenericFunctions.extractType(brokenName.getMiddleNameAsString());
			name.setNameType(names[1]);
			names = GenericFunctions.extractOtherType(brokenName.getMiddleNameAsString());
			name.setNameOtherType(names[1]);
			ret.add(name);
		}
		return ret;
	}	
	 
	 public boolean searchWithSubdivision(){
		 SearchAttributes sa = getSearchAttributes();
		
		 if(StringUtils.isEmpty(sa.getAtribute( SearchAttributes.LD_SUBDIV_NAME )))
			 return false;
		 if(StringUtils.isEmpty(sa.getAtribute( SearchAttributes.LD_LOTNO)) && 
				 StringUtils.isEmpty(sa.getAtribute( SearchAttributes.LD_SUBDIV_BLOCK)) &&
				 StringUtils.isEmpty(sa.getAtribute( SearchAttributes.LD_SUBDIV_PHASE)) &&
				 StringUtils.isEmpty(sa.getAtribute( SearchAttributes.LD_SUBDIV_SEC)) && 
				 StringUtils.isEmpty(sa.getAtribute( SearchAttributes.LD_SUBDIV_UNIT)))
			 return false;
		 return true;
		 
	 }
	 
	 /**
	  * Checks if the SearchAttributes key <b>LD_PARCELNO</b> is not empty. <br>
	  * The value for this key is parsed in <em>PropertyIdentificationSet.ParcelID</em>
	  * @return true if the key is not empty
	  */
	 protected boolean hasPin(){
		 return !StringUtils.isEmpty(getSearchAttribute(SearchAttributes.LD_PARCELNO));
	 }
	 
	 /**
	  * Checks if the SearchAttributes key <b>LD_PARCELNO_PARCEL</b> is not empty. <br>
	  * The value for this key is parsed in <em>PropertyIdentificationSet.ParcelIDParcel</em>
	  * @return true if the key is not empty
	  */
	 protected boolean hasPinParcelNo(){
		 return !StringUtils.isEmpty(getSearchAttribute(SearchAttributes.LD_PARCELNO_PARCEL));
	 }
	 
	 /**
	  * Checks if the SearchAttributes key <b>LD_GEO_NUMBER</b> is not empty. <br>
	  * The value for this key is parsed in <em>PropertyIdentificationSet.GEO_NUMBER</em>
	  * @return true if the key is not empty
	  */
	 protected boolean hasGeoNumber(){
		 return !StringUtils.isEmpty(getSearchAttribute(SearchAttributes.LD_GEO_NUMBER));
	 }
	 
	 protected boolean hasBook(){
		 return !StringUtils.isEmpty(getSearchAttribute(SearchAttributes.LD_BOOKNO));
	 }
	 
	 protected boolean hasPage(){
		 return !StringUtils.isEmpty(getSearchAttribute(SearchAttributes.LD_PAGENO));
	 }
	 
	 protected boolean hasBookAndPage(){
		 return hasBook() && hasPage();
	 }
	 
	 protected boolean hasStreet(){
		 return !StringUtils.isEmpty(getSearchAttribute(SearchAttributes.P_STREETNAME));
	 }
	 
	 protected boolean hasStreetNo(){
		 return !StringUtils.isEmpty(getSearchAttribute(SearchAttributes.P_STREETNO));
	 }
	 
	 protected boolean hasCity(){
		 return !StringUtils.isEmpty(getSearchAttribute(SearchAttributes.P_CITY));
	 }
	 
	 protected boolean hasZip(){
		 return !StringUtils.isEmpty(getSearchAttribute(SearchAttributes.P_ZIP));
	 }
	 
	 public static boolean maiTrebuieAfisatMesajul  = true;   //BUG 3211 -  daca s-a afisat un mesaj in SearchLogger, atunci nu se va mai afisa altul
	 
	 protected void printSubdivisionException (){
		 Search search = getSearch();
		 if(search!=null) {
			 if (!search.getSa().getCountyName().equals("Alachua")) 
				 maiTrebuieAfisatMesajul = true;
		     if (maiTrebuieAfisatMesajul) {	 
				 if( !search.isSubdivisionExceptionPrintedRO() && 
						 search.getSearchType() == Search.AUTOMATIC_SEARCH &&
						 "false".equals(search.getSa().getAtribute(SearchAttributes.SEARCHFINISH))) {
					 search.setSubdivisionExceptionPrintedRO(true);
					 IndividualLogger.info( "Will not search with subdivision name because either subdivision " +
					 		"is missing or we do not have any of the following: lot/unit, block, section, phase for validation.",searchId);
				     SearchLogger.info("<font color=\"red\"><BR>" +
				     		"Will not search with subdivision name because either subdivision" +
				     		" is missing or we do not have any of the following: lot/unit, block, section, phase for validation." +
				     		"</font><BR>", searchId);	 
				 }
		     }
		 }
	 }
	
	 protected void printAddressException()     // BUG 3211 
	 {
		 Search search = getSearch();

		 IndividualLogger.info( "The search by address module is not implemented on county tax for " + search.getSa().getCountyName()+ 
			 		", so it wasn't performed an automatic search on TR by property address.",searchId);
		 SearchLogger.info("<font color=\"red\"><BR>" +
			     		"The search by address module is not implemented on county tax for Alachua, " + 
			 		    " so it wasn't performed an automatic search on TR by property address." +
			     		"</font><BR>", searchId);	
		 maiTrebuieAfisatMesajul = false;
	 }

	 @Override
	 public DownloadImageResult downloadImageAndUpdateSearchDocument(String documentIdStr) throws ServerResponseException{
		DownloadImageResult res = null;
		if( mSearch == null ){
			if( searchId > 0 && mSearch == null){
				mSearch = getSearch();
			}
			else{
				throw new RuntimeException("Incomplete TSServer mSearch == "+ mSearch +" searchId = " + searchId);
			}
		}
 
		DocumentsManagerI doc = mSearch.getDocManager();
		DocumentI docIclone = null;
		DocumentI original = null;
		
		try{
			doc.getAccess();
			original = doc.getDocument(documentIdStr);
			docIclone = doc.getDocument(documentIdStr).clone();
		}finally{
			doc.releaseAccess();
		}
		
		if(docIclone!=null){
			ImageI im = docIclone.getImage();
			res =  downloadImage( docIclone );
			
			if( docIclone.hasImage() ){
				if( res!=null && res.getStatus() == DownloadImageResult.Status.OK ){
					im.setSaved(true);
					
					if (!im.getContentType().equals(original.getImage().getContentType())) {
						if ("application/pdf".equals(original.getImage().getContentType())) {
							original.getImage().setImageAsTiff();
						} else {
							original.getImage().setImageAsPdf();
						}
					}
					
					if(original.hasImage()){
						im.setSsfLink(original.getImage().getSsfLink());
						im.setContainsIndex(original.getImage().isContainsIndex());
						try {
							if(!im.getPath().equals(original.getImage().getPath())) {
								org.apache.commons.io.FileUtils.copyFile(new File(original.getImage().getPath()), new File(im.getPath()));
								res.setImageContent(FileUtils.readBinaryFile(im.getPath()));
							}
						} catch (Exception e) {
							e.printStackTrace();
							logger.error("Error while copying file location after ssf upload", e);
						}
					}
				}
				else{
					return new DownloadImageResult(DownloadImageResult.Status.ERROR, new byte[0],"" );
				}
			}
			
			try{
				doc.getAccess();
				if(original!=null){
					if( docIclone.hasImage() ){
						original.setImage(docIclone.getImage());
					}
				}
			}
			finally{
				doc.releaseAccess();
			}
		}
		
		return res;
	 }
	
	@Override
	public DownloadImageResult downloadImage(DocumentI document) throws ServerResponseException{
		findImageOnSsf(getSearch(), document, false, new ArrayList<DocumentInfoType>());
		return downloadImage(document.getImage(), document.getId());
	}

	 @Override
	 public DownloadImageResult downloadImageAndCreateItOnDocument(String documentIdStr) throws Exception{
		DownloadImageResult res = null;
		if (mSearch == null){
			if (searchId > 0 && mSearch == null){
				mSearch = getSearch();
			} else{
				throw new RuntimeException("Incomplete TSServer mSearch == " + mSearch + " searchId = " + searchId);
			}
		}
 
		DocumentsManagerI doc = mSearch.getDocManager();
		DocumentI docIclone = null;
		DocumentI original = null;
		
		try{
			doc.getAccess();
			original = doc.getDocument(documentIdStr);
			docIclone = doc.getDocument(documentIdStr).clone();
		}finally{
			doc.releaseAccess();
		}
		
		if (docIclone != null){
			
			SearchLogger.info("<br/>Search for Image operation on document " + docIclone.prettyPrint(), searchId);
			
			boolean foundOnSsf = findImageOnSsf(mSearch, docIclone, true, new ArrayList<DocumentInfoType>());
			
			if (!foundOnSsf){
				SearchLogger.info("<br/>Image for document " + docIclone.prettyPrint() + " was not found on SSF", searchId);
				
				Image image = new Image();
				image.setType(IType.TIFF);
				image.setExtension("tiff");
				image.setContentType("image/tiff");
				
				String imageDirectory = mSearch.getImageDirectory();
				FileUtils.CreateOutputDir(imageDirectory);
				String fileName = docIclone.getId() + "." + image.getExtension();
	        	String path = imageDirectory + File.separator + fileName;
	        	
	        	image.setPath(path);
	        	image.setFileName(fileName);
				
	        	docIclone.setImage(image);
	        	
	        	res = searchForImage(docIclone);
			} else{
				if (DocumentDataRetreiver.downloadImageFromSsfIfNeedIt(getSearch(), docIclone.getImage())){
					SearchLogger.info("<br/>Image <a href='" + docIclone.getImage().getSsfLink() + "'>" + docIclone.prettyPrint() + "</a> found on SSF", searchId);
				}
				String imageName = docIclone.getImage().getPath();
				if (FileUtils.existPath(imageName)){
					byte[] imageBytes = FileUtils.readBinaryFile(imageName);
					res = new DownloadImageResult(DownloadImageResult.Status.OK, imageBytes, docIclone.getImage().getContentType());
				}
			}
			
			if (docIclone.hasImage()){
				if (res != null && res.getStatus() == DownloadImageResult.Status.OK ){
					ImageI im = docIclone.getImage();
					im.setSaved(true);
				} else{
					return new DownloadImageResult(DownloadImageResult.Status.ERROR, new byte[0],"" );
				}
			}
			try{
				doc.getAccess();
				if (original != null){
					if (docIclone.hasImage()){
						original.setImage(docIclone.getImage());
					}
				}
			}
			finally{
				doc.releaseAccess();
			}
			if (!foundOnSsf){
				try{
					TsdIndexPageServer.uploadImageToSSf(documentIdStr, searchId, true, true, true);
				} catch(Exception t){
					SearchLogger.info("<br/>Failed to upload image on SSF for document " + docIclone.prettyPrint(), searchId);
					throw t;
				}
			}
		}
		
		return res;
	 }
	 
	/**
	 * If the image associated doesn't have a SSF link we try to find it on SSF and update our image<br>
	 * @param search
	 * @param document the document that has the image to be updated
	 * @param createImageIfMissing if true and document has no image a new image will be created and attached to the document if found on ssf
	 * @return <code>true</code> if a (new) SSF link was set on image
	 */
	public static boolean findImageOnSsf(Search search, DocumentI document, boolean createImageIfMissing, List<DocumentInfoType> documents) {
		if(search == null || document == null) {
			return false;
		}
		ImageI image = document.getImage();
		boolean temporaryImageAddedToDocument = false;
		if(image == null && createImageIfMissing) {
			image = new Image();
			image.setType(IType.TIFF);
			image.setExtension("tiff");
			image.setContentType("image/tiff");
			
			String imageDirectory = search.getImageDirectory();
			FileUtils.CreateOutputDir(imageDirectory);
			String fileName = document.getId() +"."+ image.getExtension();
        	String path = imageDirectory+File.separator+fileName;
        	
        	image.setPath(path);
        	image.setFileName(fileName);
			
			document.setImage(image);
			temporaryImageAddedToDocument = true;
		}
		
		if(image !=null && StringUtils.isEmpty(image.getSsfLink())){
			
			try {
			
				DocAdminConn con  = new DocAdminConn(search.getCommId());
				County county=InstanceManager.getManager().getCurrentInstance(search.getID()).getCurrentCounty();
				
				DocumentInfoResultType results = con.getDocumentInfoWithInstrument(document, 0, 
						county.getState().getStateFips(), 
						county.getCountyFips(), 
						search.getSa().getAtribute(SearchAttributes.LD_PARCELNO),
						!document.isUploaded());
				
				if(results!=null){
					DocumentInfoType docsInfo[] = results.getDocInfo();
					DocumentInfoType docInfo = null;
					if(docsInfo!=null && docsInfo.length>0){
						
						for (int i=0;i<docsInfo.length;i++) {
							org.apache.axis2.databinding.types.URI uri = docsInfo[i].getLink();
							String link = "";
							if (uri!=null) {
								link = uri.toString();
							}
							if (org.apache.commons.lang.StringUtils.isNotBlank(link)) {
								documents.add(docsInfo[i]);
							}
						}
						
						docInfo = docsInfo[0];
						if(org.apache.commons.lang.StringUtils.isNotBlank(docInfo.getLink().toString())){
							
							image.setSsfLink(docInfo.getLink().toString());
							try{
								image.setContentType(docInfo.getDocIndex().getMime().getMimeType().toString());
								image.setExtension(docInfo.getDocIndex().getExt().getExtType().toString());
								
								if("pdf".equals(image.getExtension())) {
									image.setType(IType.PDF);
								} else {
									image.setType(IType.TIFF);
								}
								
								String fileName = image.getFileName();
								int lastIndexOfPoint = fileName.lastIndexOf(".");
								if(lastIndexOfPoint > 0) {
									image.setFileName(fileName.substring(0, lastIndexOfPoint) + "." + image.getExtension());
								}
								String path = image.getPath();
								lastIndexOfPoint = path.lastIndexOf(".");
								if(lastIndexOfPoint > 0) {
									image.setPath(path.substring(0, lastIndexOfPoint) + "." + image.getExtension());
								}
								
								return true;
								
							}catch(Exception e){
								e.printStackTrace();
							}
							//the image will be counted when first downloaded from SSF
							//afterDownloadImage(true, GWTDataSite.SF_TYPE);
							
						}
					}
				}
			} catch (Exception e) {
				logger.error("Failed to get image results from SSF for document "+ document.prettyPrint(), e);
			}
		} 
		/**
		 * If something failed we need to clean the document
		 */
		if(createImageIfMissing && temporaryImageAddedToDocument) {
			document.setImage(null);
		}
		
		if(image !=null && StringUtils.isNotEmpty(image.getSsfLink())){
			return true;
		}
		
		return false;
	}
		
	@Override
	public DownloadImageResult downloadImage(ImageI image, String documentIdStr) throws ServerResponseException{
		return downloadImage(image, documentIdStr, null);
	}
	
	public DownloadImageResult downloadImage(ImageI image, String documentIdStr, DocumentI doc) throws ServerResponseException{
		String test = "Parent Site";
		
		if (doc != null){
			test = doc.prettyPrint();
		} else{
		DocumentsManagerI docma = getSearch().getDocManager();
		try{
			docma.getAccess();
			if(documentIdStr!=null){
				doc = docma.getDocument(documentIdStr);
				if(doc!=null){
					test = doc.prettyPrint();
				}
			}
		}finally{
			docma.releaseAccess();
		}
		}
		boolean imageDownloadedFromSSF = false;
		if(DocumentDataRetreiver.downloadImageFromSsfIfNeedIt(getSearch(), image)){
			imageDownloadedFromSSF = true;
			SearchLogger.info("<br/>Image <a href='"+image.getSsfLink()+"'>"+test+"</a> downloaded from SSF<br/>",searchId);
		}
		
		File f = new File(image.getPath());
		if(f.exists()&&f.isFile()){
			if(!imageDownloadedFromSSF && doc!=null){
				if(org.apache.commons.lang.StringUtils.isBlank(doc.getImage().getSsfLink())){
					try{TsdIndexPageServer.uploadImageToSSf(documentIdStr, searchId, false, false);}catch (Exception e) {
						e.printStackTrace();
					}
				}
			}
			byte b[] = FileUtils.readBinaryFile(image.getPath());
			DownloadImageResult d= new DownloadImageResult();
			d.setContentType( image.getContentType() ) ;
			d.setImageContent( b );
			d.setStatus(DownloadImageResult.Status.OK);
			image.setSaved(true);
			return d;
		} else {
			File imageFolder = f.getParentFile();
			if (imageFolder !=null && !imageFolder.exists()){
				imageFolder.mkdirs();
			}
		}
		
		DownloadImageResult res = saveImage(image);
		if(res == null) {
			ImageLinkInPage ilip = ImageTransformation.imageToImageLinkInPage(image);
			String initialContentType = ilip.getContentType();
			res = saveImage( ilip );
			if(!initialContentType.equals(ilip.getContentType())) {
				if("application/pdf".equals(ilip.getContentType())) {
					image.setImageAsPdf();
					doc.getImage().setImageAsPdf();
				} else {
					image.setImageAsTiff();
					doc.getImage().setImageAsTiff();
				}
			}
			//afterDownloadImage(ilip,res.getStatus()==DownloadImageResult.Status.OK);
		}
		if(res == null) {
			return null;
		}
		if(res.getStatus()==DownloadImageResult.Status.OK){
						
			if(!f.exists() ){
				FileUtils.writeByteArrayToFile(res.getImageContent(), image.getPath());
			}
			
			image.setSaved(true);
			/*if(ServerConfig.isFileReplicationEnabled()){
				FileContent.replicateImage(image, searchId, false);
			}*/
			try{TsdIndexPageServer.uploadImageToSSf(documentIdStr, searchId, false, false);}catch (Exception e) {
				e.printStackTrace();
			}
		}
		return res;
	}
	
	@Override
	public DownloadImageResult downloadImageFromDataSource(ImageI image, String documentIdStr) throws ServerResponseException{
		
		String test = "Parent Site";
		DocumentI doc = null;
		DocumentsManagerI docma = getSearch().getDocManager();
		try{
			docma.getAccess();
			if(documentIdStr != null){
				doc = docma.getDocument(documentIdStr);
				if(doc != null){
					test = doc.prettyPrint();
				}
			}
		}finally{
			docma.releaseAccess();
		}
		
		File f = new File(image.getPath());
		File imageFolder = f.getParentFile();
		if (imageFolder != null && !imageFolder.exists()){
			imageFolder.mkdirs();
		}
		if (f != null && f.exists()){
			org.apache.commons.io.FileUtils.deleteQuietly(f);
		}
		
		if ((this instanceof TSServerDASLAdapter || this instanceof TSServerPI || this instanceof GenericSKLD)
				&& "application/pdf".equals(image.getContentType())){
			image.setImageAsTiff();
		} else {
			String oldImageFileName = image.getLink(0);
			if (oldImageFileName != null) {
				String extension = "";
				int poz = oldImageFileName.lastIndexOf(".");
				if (poz > 0 && (poz + 1 < oldImageFileName.length())) {
					extension = oldImageFileName.substring(poz + 1);
				}
				if (extension.equalsIgnoreCase("tif")) {
					extension = "tiff";
				}
				if("tiff".equalsIgnoreCase(extension)) {
					image.setImageAsTiff();
				} else if ("pdf".equalsIgnoreCase(extension)){
					image.setImageAsPdf();
				}
 			}
		}
		
		DownloadImageResult res = saveImage(image);
		if(res == null){
			ImageLinkInPage ilip = ImageTransformation.imageToImageLinkInPage(image);
			res = saveImage(ilip);
		}
		if(res == null){
			return null;
		}
		if(res.getStatus() == DownloadImageResult.Status.OK){
			
			String path = image.getPath();
			if (path.endsWith(".tiff") && res.getContentType().contains("pdf")){
				image.setImageAsPdf();
				f = new File(image.getPath());
			} else if (path.endsWith(".pdf") && res.getContentType().contains("tiff")){
				image.setImageAsTiff();
				f = new File(image.getPath());
			}
			FileUtils.writeByteArrayToFile(res.getImageContent(), image.getPath());			
			image.setSaved(true);
		} else{
			return null;
		}
		return res;
	}
	
	public void afterDownloadImage() {
		afterDownloadImage(true);
	}
	
	public void afterDownloadImage(boolean downloaded) {
		afterDownloadImage(downloaded,-1);
	}
	
	public void afterDownloadImage(boolean downloaded, int type) {
		try {
			if(!downloaded ) {
				return; 
			}
			if(type==-1) {
				type = getImageDataSource();
			}
			if(isRoLike(type))  {
				mSearch.countNewImage(type);
			}
		}catch(Exception e) {
			e.printStackTrace();
		}
	}
	
	public int getImageDataSource() {
		//DataSite datasite = HashCountyToIndex.getDateSiteForMIServerID(HashCountyToIndex.ANY_COMMUNITY, miServerID);
		return getDataSite().getCityCheckedInt();	
	}
		
	@Override
	public void processLastRealTransfer(TransferI lastRealTransfer, OCRParsedDataStruct ocrRealData) {
		return;
	}

	@Override
	public void processLastTransfer(TransferI lastTransfer,OCRParsedDataStruct ocrData) {
		return;
	}

	@Override
	public void forceSearch(Search search) {
		mSearch = search;
		searchId = search.getID();
		
	}
	
	public static void main(String[] args) {
		/*try{
			while(true){
				String link = "http://atsdip.advantagetitlesearch.com/ocr-server/services/Ocr.OcrHttpSoap12Endpoint";
				
				final int timeout =  ServerConfig.getOmnipageTimeout();
				final OcrPortType ocrServer = ServiceFactory.createOcrPort(link, timeout * 11 * 1000 / 10);
				final String user = ServerConfig.getOmnipageUser();
				final String password = ServerConfig.getOmnipagePassword();
				final com.stewart.ats.webservice.ocr.xsd.Image image = new com.stewart.ats.webservice.ocr.xsd.Image();
				image.setDescription("c:\\1.tiff");
				image.setData(FileUtils.readBinaryFile("c:\\1.tiff"));
				image.setType("tif");
				Thread t = new Thread(){
					@Override
					public void run() {
						/*try{
						Output out = ocrServer.process(user, password, image, timeout);
						}
						catch(Exception e){
							e.printStackTrace();
						}* /
					}
				};
				Thread.sleep(8);
			}
		}
		catch(Exception e){
			e.printStackTrace();
		}*/
	}

	public boolean isOcrAdded() {
		return ocrAdded;
	}

	public void setOcrAdded(boolean ocrAdded) {
		this.ocrAdded = ocrAdded;
	}

	@Override
	public void  addDocumentAdditionalProcessing(DocumentI doc, ServerResponse response) {
		
	}

	@Override
	public void  addDocumentAdditionalPostProcessing(DocumentI doc, ServerResponse response) {
		if(doc!=null && doc.getType()==DType.ROLIKE && doc instanceof RegisterDocumentI){
    		RegisterDocumentI regDoc = (RegisterDocumentI) doc;
        	if("RO\nIP".equalsIgnoreCase(regDoc.getDataSource())){
        		regDoc.setDataSource("RO");
        		regDoc.setSearchType(SearchType.IP);
        	}
    	}
	}
	
	public void  addAdditionalDocuments(DocumentI doc, ServerResponse response) {
		
	}

	public Collection<ParsedResponse> smartParseIntermediary(ServerResponse response, String table,StringBuilder outputTable) {
		// TODO Auto-generated method stub
		return null;
	}
	
	/**
	 * Parses the html and returns a DocumentI. As a side effect, fills the ServerResponse with document information 
	 * @param response the ServerResponse to fill
	 * @param detailsHtml the html to parse 
	 * @return null if parsing was not succesfull
	 */
	public DocumentI smartParseDetails(ServerResponse response, String detailsHtml){
		return smartParseDetails(response, detailsHtml, true);
	}
	
	/**
	 * Parses the html and return a DocumentI. If the <b>fillServerResponse</b> is set to true, 
	 * it also fills the ServerResponse with document information, other wise it just created the document. 
	 * @param response
	 * @param detailsHtml the html to parse 
	 * @param fillServerResponse
	 * @return null if parsing was not succesfull
	 */
	public DocumentI smartParseDetails(ServerResponse response, String detailsHtml, boolean fillServerResponse){
		DocumentI document = null;
		try {
			ResultMap map = new ResultMap();
			parseAndFillResultMap(response,detailsHtml, map);
			map.removeTempDef();//this is for removing tmp items. we remove them here to not remove them in every place when we parse something.
			Bridge bridge = new Bridge(response.getParsedResponse(),map,searchId);
			try{
	    		String prevSrcType = (String)map.get(OtherInformationSetKey.SRC_TYPE.getKeyName());
	    		if(StringUtils.isEmpty(prevSrcType)){	    			
	    			map.getMap().put(OtherInformationSetKey.SRC_TYPE.getKeyName(), getDataSite().getSiteTypeAbrev());
	    		}
			}catch(Exception e){
				e.printStackTrace();
			}  
			document = bridge.importData();
			
		} catch (Exception e) {
			e.printStackTrace();
		}
		if(fillServerResponse) {
			response.getParsedResponse().setResponse(detailsHtml);
			if(document!=null) {
				ParsedResponse newParsedResponse = new ParsedResponse();
    			newParsedResponse.setDocument(document);
    			
    			getDocumentSearchType(document, true);
    			
    			newParsedResponse.setUseDocumentForSearchLogRow(true);
    			response.getParsedResponse().addOneResultRowOnly(newParsedResponse);
				response.getParsedResponse().setDocument(document);
			}
		}
		response.getParsedResponse().setSearchId(this.searchId);
		response.getParsedResponse().setUseDocumentForSearchLogRow(true);
		return document;
	}
	
	/**
	 * Parses the html and fills the resultmap.
	 * @param detailsHtml
	 * @param map
	 * @throws Exception 
	 */
	protected Object parseAndFillResultMap(ServerResponse response,String detailsHtml, ResultMap map) {
		/* Implement me in the derived class */
		return null;
	}
	
	public String getCurrentCommunityId(){
		CommunityAttributes communityAttributes = InstanceManager.getManager().getCurrentInstance(searchId).getCurrentCommunity();
		String string = communityAttributes.getID().toString();
   		return  string;
	}
	
	public int getCurrentCommunityIdAsInt(){ 
		return InstanceManager.getManager().getCurrentInstance(searchId).getCurrentCommunity().getID().intValue();
	}
	
	public static String getCurrentCommunityId(long searchId){
		CommunityAttributes communityAttributes = InstanceManager.getManager().getCurrentInstance(searchId).getCurrentCommunity();
		String string = communityAttributes.getID().toString();
   		return  string;
	}
	@Override
	public boolean isInNextLinkSequence() {
		return inNextLinkSequence;
	}
	@Override
	public void setInNextLinkSequence(boolean inNextLinkSequence) {
		this.inNextLinkSequence = inNextLinkSequence;
	}
	@Override
	public boolean isDoNotLogSearch() {
		return doNotLogSearch;
	}
	@Override
	public void setDoNotLogSearch(boolean doNotLogSearch) {
		this.doNotLogSearch = doNotLogSearch;
	}
	@Override
	public void performAdditionalProcessingWhenInvalidatingDocument(
			ServerResponse response) {
		// TODO Auto-generated method stub
	}
	
	@Override
	public void performAdditionalProcessingBeforeRunningAutomatic() {
		// TODO Auto-generated method stub
	}
	
	@Override
	public void performAdditionalProcessingAfterRunningAutomatic() {
		// TODO Auto-generated method stub
	}
	
	/**
	 * Simple apache htmlParser that return the full nodeList of the document or null if an error occured
	 * @param htmlDocument the document to parser
	 * @return the full nodeList or null if an error has occured
	 */
	public NodeList getAllNodesFromHtml(String htmlDocument) {
		try {
			org.htmlparser.Parser parser = org.htmlparser.Parser.createParser(htmlDocument, null);
			return parser.parse(null);
		} catch (Exception e) {
			logger.error("Error while gettin nodes from html object",e);
		}
		return null;
	}

	@Override
	public int getCommunityId() {
		return InstanceManager.getManager().getCommunityId(searchId);
		
	}
	
	@Override
	public void processInstrumentBeforeAdd(DocumentI doc) { 
		
	}
	
	/**
	 * Get the contents of a link, redirecting to another link
	 * @param link
	 * @return
	 */
	public String getLinkContents(String link){
		
		if(getDataSite().getConnType() == GWTDataSite.HTTP_CONNECTION_2) {
			// acquire a HttpSite
			HttpSite site = HttpManager.getSite(getCurrentServerName(), searchId);
			try {
				for(int i=0; i<3; i++){
					try {
						return site.process(new HTTPRequest(link)).getResponseAsString();
					} catch (RuntimeException e){
						logger.warn("Could not bring link:" + link, e);
					}
				}
			} finally {
				// always release the HttpSite
				HttpManager.releaseSite(site);
			}
		} else if(getDataSite().getConnType() == GWTDataSite.HTTP_CONNECTION_3) {
			HttpSite3 site = HttpManager3.getSite(getCurrentServerName(), searchId);
			try {
				for(int i=0; i<3; i++){
					try {
						return site.process(new HTTPRequest(link)).getResponseAsString();
					} catch (RuntimeException e){
						logger.warn("Could not bring link:" + link, e);
					}
				}
			} finally {
				// always release the HttpSite
				HttpManager3.releaseSite(site);
			}
		}
		return "";
	}
	
	/**
	 * Get link from Search Sites
	 * * @return
	 */
	
	public String getBaseLink() {
		return getDataSite().getLink();
	}
	
	/**
	 * Set the certification date for a ro-like server.
	 */
	protected void setCertificationDate() {
		/* Implement me in the derived class */
	}

	@Override
	public String getContinueForm(String p1, String p2, long searchID) {
		String form = "";
		Search searchContext = getSearch();
		int searchType = searchContext.getSearchType();
		
		if (searchType == Search.AUTOMATIC_SEARCH) {
			//in automatic, clicking on Continue, should continue searching on next available site
			form = "<br>\n<form name=\"formContinue\" "        	
		        	+ "action=\"/title-search/URLConnectionReader\""
		        	+ " method=\"POST\">\n"
		        	+ "<input type=\"hidden\" name=\"dispatcher\" value=\"" + URLConnectionReader.CONTINUE_TO_NEXT_SERVER_ACTION + "\">\n"
		        	+ "<input type=\"hidden\" name=\"ServerID\" value=\"" + URLConnectionReader.PRM_NAME_P1 + "="+ p1 + "&" + URLConnectionReader.PRM_NAME_P2 + "=" +  p2 + "\">\n"
		        	+ "<input type=\"hidden\" name=\"" + RequestParams.SEARCH_ID + "\" value=\"" + searchID + "\">"
		        	+ "<input  type=\"submit\" class=\"button\" name=\"Button\" value=\"Continue\">\n"
		            + "</form>";
		
		} else { 
		    //in PS, clicking on Continue, TSRI should be opened
			long userId = InstanceManager.getManager().getCurrentInstance(searchID).getCurrentUser().getID().longValue();
	    	
	    	form = "<br>\n<form name=\"formContinue\" "        	
	        	+ "action=\"/title-search/jsp/newtsdi/tsdindexpage.jsp?searchId=" + searchID + "&userId=" +  userId + "\""
	        	+ " method=\"POST\">\n"
	        	+ "<input  type=\"submit\" class=\"button\" name=\"Button\" value=\"Continue\">\n"
	            + "</form>";
		}
    	
		return form;
	}
	
	public static Logger getLogger() {
		return logger;
	}
	
	public void smartParseForOldSites(ParsedResponse pr, ResultMap resultMap, String htmlString, int parserId){
		/* Implement me in the derived class */
	}
	
	@Override
	public RestoreDocumentDataI saveInvalidatedDocumentForRestoration(FilterResponse filter, ServerResponse response, boolean invalidated) {
		if(isRoLike(miServerID, true)) {
	    	if(response == null || response.getParsedResponse() == null) {
	    		return null;
	    	}
	    	response.getParsedResponse().setSearchId(getSearch().getID());
	    	
	    	RestoreDocumentDataI rsRestoreDocumentDataI = new RestoreDocumentData(response.getParsedResponse(), getServerID());
	    	
	    	if(invalidated) {
	    		//mark it as invalidated on ATS list
	    		County currentCounty = InstanceManager.getManager().getCurrentInstance(searchId).getCurrentCounty();
	    		((RestoreDocumentData)rsRestoreDocumentDataI).setCounty(currentCounty);
	    		((RestoreDocumentData)rsRestoreDocumentDataI).setSearchId(searchId);
	    		String category = rsRestoreDocumentDataI.getCategory(); 
	    		if("MORTGAGE".equals(category) || "LIEN".equals(category)) {
	    			Object savedObject = getSearch().getAdditionalInfo(AdditionalInfoKeys.REJECTED_MORTGAGES_AND_LIENS);
	    			List<InstrumentI> listWithInvalidatedDocuments = null;
	    			if(savedObject == null) {
	    				listWithInvalidatedDocuments = new ArrayList<InstrumentI>();
	    				getSearch().setAdditionalInfo(AdditionalInfoKeys.REJECTED_MORTGAGES_AND_LIENS, 
	    						listWithInvalidatedDocuments);
	    			} else if(! (savedObject instanceof List<?>) ) {
	    				listWithInvalidatedDocuments = new ArrayList<InstrumentI>();
	    				getSearch().setAdditionalInfo(AdditionalInfoKeys.REJECTED_MORTGAGES_AND_LIENS, 
	    						listWithInvalidatedDocuments);
	    			} else {
	    				listWithInvalidatedDocuments = (List<InstrumentI>) savedObject;
	    			}
	    			listWithInvalidatedDocuments.add(rsRestoreDocumentDataI.toInstrument());
	    		}
	    		if(filter != null) {
	    			if(!(filter instanceof NoIndexingInfoFilter)) {
	    				getSearch().getSa().addInvalidatedInstrument(rsRestoreDocumentDataI.toInstrument());
	    			}
	    		} else {
	    			getSearch().getSa().addInvalidatedInstrument(rsRestoreDocumentDataI.toInstrument());
	    		}
	    	}
	    	
	    	SaveRestoreDocumentTransaction transaction = new SaveRestoreDocumentTransaction(rsRestoreDocumentDataI, getSearch());
	    	boolean transactionResult = (Boolean) DBManager.getTransactionTemplate().execute(transaction);
	    	if(transactionResult) {
	    		return rsRestoreDocumentDataI;
	    	}
		}
		return null;
	}
	
	@Override
	public RestoreDocumentDataI saveInvalidatedDocumentForRestoration(FilterResponse filter, ParsedResponse parsedResponse) {
		try {
			if(isRoLike(miServerID, true, getSearch().getCommId())) {
		    	if(parsedResponse == null ) {
		    		return null;
		    	}
		    	parsedResponse.setSearchId(getSearch().getID());
		    	
		    	RestoreDocumentDataI rsRestoreDocumentDataI = new RestoreDocumentData(parsedResponse, getServerID());
		    	
		    	//mark is as invalidated on ATS list
	    		County currentCounty = InstanceManager.getManager().getCurrentInstance(searchId).getCurrentCounty();
	    		((RestoreDocumentData)rsRestoreDocumentDataI).setCounty(currentCounty);
	    		((RestoreDocumentData)rsRestoreDocumentDataI).setSearchId(searchId);
	    		String category = rsRestoreDocumentDataI.getCategory(); 
	    		if("MORTGAGE".equals(category) || "LIEN".equals(category)) {
	    			Object savedObject = getSearch().getAdditionalInfo(AdditionalInfoKeys.REJECTED_MORTGAGES_AND_LIENS);
	    			List<InstrumentI> listWithInvalidatedDocuments = null;
	    			if(savedObject == null) {
	    				listWithInvalidatedDocuments = new ArrayList<InstrumentI>();
	    				getSearch().setAdditionalInfo(AdditionalInfoKeys.REJECTED_MORTGAGES_AND_LIENS, 
	    						listWithInvalidatedDocuments);
	    			} else if(! (savedObject instanceof List<?>) ) {
	    				listWithInvalidatedDocuments = new ArrayList<InstrumentI>();
	    				getSearch().setAdditionalInfo(AdditionalInfoKeys.REJECTED_MORTGAGES_AND_LIENS, 
	    						listWithInvalidatedDocuments);
	    			} else {
	    				listWithInvalidatedDocuments = (List<InstrumentI>) savedObject;
	    			}
	    			listWithInvalidatedDocuments.add(rsRestoreDocumentDataI.toInstrument());
	    		}
		    	
	    		if(filter != null) {
	    			if(!(filter instanceof NoIndexingInfoFilter)) {
	    				getSearch().getSa().addInvalidatedInstrument(rsRestoreDocumentDataI.toInstrument());
	    			}
	    		} else {
	    			getSearch().getSa().addInvalidatedInstrument(rsRestoreDocumentDataI.toInstrument());
	    		}
	    		
	    		
		    	SaveRestoreDocumentTransaction transaction = new SaveRestoreDocumentTransaction(rsRestoreDocumentDataI, getSearch());
		    	boolean transactionResult = (Boolean) DBManager.getTransactionTemplate().execute(transaction);
		    	if(transactionResult) {
		    		return rsRestoreDocumentDataI;
		    	}
			}
		} catch (Exception e) {
			logger.error("Error saving invalidated document", e);
		}
		return null;
	}

	/**
	 * This must be implemented in order to create a module that will save a document in TSRIExtended.
	 */
	@Override
	public Object getRecoverModuleFrom(RestoreDocumentDataI document) {
		// TODO Auto-generated method stub
		return null;
	}
	
	/**
	 * This must le implemented in order to create a object that will used to download and view a image<br>
	 * It can be the same result like the recover module
	 */
	@Override
	public Object getImageDownloader(RestoreDocumentDataI document) {
		return getRecoverModuleFrom(document);
	}
	
	@Override
	public void setMsServerId(String msServerId) {
		this.msServerID = msServerId;
	}
	
	public boolean isCachedDocument(String key) {
		return getCachedDocument(key) != null;
	}
	public Object getCachedDocument(String key) {
		return null;
	}
	
	@Override
	public String cleanHtmlBeforeSavingDocument(String htmlContent) {
		return htmlContent;
	}
	
	/**
	 * The purpose of this method is to add a date filter between start and end dates available in Search Page<br>
	 * This date filtering is necessary if the search has no interval set or if the interval is not reliable<br>
	 * Also, you cand add as a filter or as a validator
	 * @param module the module to be tested
	 * @param addAlways	if <code>true</code> the test will always be done, otherwise only on Update or DateDown Search
	 * @param addModuleToTester if <code>true</code> the module is added to the tester as parameter
	 * @param addAsFilterNotValidator if <code>true</code> the test is done as a filter, otherwise as a validator
	 * @return <code>true</code> if and only the filter/validator was added to module
	 */
	public boolean addBetweenDateTest(TSServerInfoModule module, boolean addAlways, boolean addModuleToTester, boolean addAsFilterNotValidator ) {
		if(module == null) {
			return false;
		}
		
		Search search = getSearch();
		
		if(addAlways || applyDateFilter() ) {
			BetweenDatesFilterResponse filter = null;
			if(addModuleToTester) {
				filter = BetweenDatesFilterResponse.getDefaultIntervalFilter(search.getID(), module);
			} else {
				filter = BetweenDatesFilterResponse.getDefaultIntervalFilter(search.getID());
			}
			if(addAsFilterNotValidator) {
				module.addFilter(filter);
			} else {
				module.addValidator(filter.getValidator());
			}
			return true;
		}
		return false;
	}
	
	/**
	 * The purpose of this method is to add an instrument filter for docs already saved on the original search
	 * @param module the module to be tested
	 * 
	 * @param addAsFilterNotValidator if <code>true</code> the test is done as a filter, otherwise as a validator
	 * @return <code>true</code> if and only the filter/validator was added to module
	 */
	public boolean addFilterForUpdate(TSServerInfoModule module, boolean addAsFilterNotValidator) {
		if(module == null) {
			return false;
		}
		
		Search search = getSearch();
		
		if(isUpdate()) {
			RejectAlreadySavedDocumentsForUpdateFilter filter = new RejectAlreadySavedDocumentsForUpdateFilter(search.getSearchID());
			if(addAsFilterNotValidator) {
				module.addFilter(filter);
			} else {
				module.addValidator(filter.getValidator());
			}
			return true;
		}
		return false;
	}
	
	/**
	 * Checks if it is necessary to apply date validation
	 * @return <code>true</code> if this is an Update or a DateDown search
	 */
	public boolean applyDateFilter() {
		Search search = getSearch();
		if(isUpdate() || search.getSa().isDateDown() ) {
			return true;
		}
		return false;
	}

	@Override
	public DataSite getDataSite() {
		if(dataSite == null) {
			dataSite = HashCountyToIndex.getDateSiteForMIServerID(getCommunityId(), miServerID);
		}
		return dataSite;
	}

	@Override
	public String getSaveSearchParametersButton(ServerResponse response) {
		if(response == null 
				|| response.getParsedResponse() == null
				|| !getDataSite().isRoLikeSite()) {
			return null;
		}
		
		Object possibleModule = response.getParsedResponse().getAttribute(TSServerInfoConstants.EXTRA_PARAM_MODULE_OBJECT_SOURCE);
		
		if(!(possibleModule instanceof TSServerInfoModule)) {
			return null;
		}
		
		Search search = getSearch();
		
		
		
		String key = "SSP_" + System.currentTimeMillis();
		
		/**
		 * Store this for future use (do not worry, it will not be saved)
		 */
		search.setAdditionalInfo(key, possibleModule);
		/*
		String link = "window.location.href='/title-search" + URLMaping.PARENT_SITE_ACTIONS + "?" + 
					RequestParams.SEARCH_ID + "=" + searchId + 
					"&" + TSOpCode.OPCODE + "=" + ParentSiteActions.SAVE_SEARCH_PARAMS + 
					"&key=" +key +"'";
		*/
		return "<input type=\"button\" name=\"ButtonSSP\" value=\"Save Search Parameters\" onClick=\"saveSearchedParametersAJAX('" + key + "','" + getServerID() + "')\" class=\"button\">";
	}

	@Override
	public void processOcredInstrument(InstrumentI in, boolean ocrReportedBookPage) { }
	
	@Override
	public boolean isRepeatDataSource() {
		return repeatDataSource;
	}
	@Override
	public void setRepeatDataSource(boolean repeatDataSource) {
		this.repeatDataSource = repeatDataSource;
	}
	
	@Override
	public void modifyDefaultServerInfo(){
		
	}
	
	private static int seq = 0;	
	protected synchronized static int getSeq(){
		return seq++;
	}
	
	protected Object getRequestCountType(int moduleIDX) {
		return null;
	}
	
	@Override
	public boolean lastAnalysisBeforeSaving(ServerResponse serverResponse){
		
		return false;
	}
	
	public List<TSServerInfoModule> getMultipleModules(TSServerInfoModule module,Object sd) {
		List<TSServerInfoModule> modules = new ArrayList<TSServerInfoModule>();
		
		
		if (sd instanceof SearchDataWrapper) {
			final HttpServletRequest originalRequest = ((SearchDataWrapper) sd).getRequest();
			int cnt = 0;

			try {
				cnt = Integer.parseInt(originalRequest.getParameter(RequestParams.PARENT_SITE_ADDITIONAL_CNT + module.getMsName()));
			} catch (Exception e) {
			}

			if (cnt == 0) {
				return modules;
			}

			for (int i = 0; i <= cnt; i++) {

				final TSServerInfoModule mod = (TSServerInfoModule) module.clone();
				final int index = i;
				
				HttpServletRequest req = new HttpServletRequestWrapper(originalRequest) {
					@Override
					public String getParameter(String name) {
						if (originalRequest.getParameter(name + "_" + index) == null) {
							if(index!=0){
								return "";
							}
							return originalRequest.getParameter(name);
						} else {
							return originalRequest.getParameter(name + "_" + index);
						}
					}
				};

				mod.setData(new SearchDataWrapper(req));
				modules.add(mod);
			}
		}
		
		//save modules to save search params
		this.getSearch().setAdditionalInfo("modulesSearched", modules);
		
		return modules;
	}
	
	@Override
	public String getIncludedScript() {
		return "";
	}
	
	public void cleanOCRData(OCRParsedDataStruct ocrData){}

	/**
	 * Uses current server and current document to determine the correct search type<br>
	 * If document had already a value set nothing will be changed on document
	 * @param document
	 * @param forceOnDocument if false document will not be changed
	 * @return the search type determined for this document and server
	 */
	public SearchType getDocumentSearchType(DocumentI document, boolean forceOnDocument) {
		SearchType searchType = SearchType.NA;
		
		TSServerInfoModule module = getSearch().getSearchRecord().getModule();

		if (module != null) {
			if(module.getIteratorType() == ModuleStatesIterator.TYPE_OCR_FULL_OR_BOOTSTRAPER 
					|| module.getIteratorType() == ModuleStatesIterator.TYPE_OCR){
				searchType = SearchType.IP;
			} else if (StringUtils.isNotEmpty(module.getSearchType())) {
				searchType = SearchType.valueOf(module.getSearchType());
			}
			if(SearchType.NA.equals(searchType)) {
				searchType = TSServerInfo.getDefaultSearchTypeForModule(module.getModuleIdx(), document);
			}
		} 
		
		if(SearchType.NA.equals(searchType)) {
			if (mSearch.getSearchIterator() != null) { //automatic
				try{
					TSServerInfoModule seedModule = mSearch.getSearchIterator().getInitial();
					if(seedModule != null) {
						if(StringUtils.isEmpty(seedModule.getSearchType())) {
							searchType = TSServerInfo.getDefaultSearchTypeForModule(seedModule.getModuleIdx(), document);
						}
					}
					
				}catch( Exception e ){
					logger.error("Error getting when extracting SearchType module on searchId: " + searchId, e);
				}
			}
		}
		
		if(forceOnDocument && SearchType.NA.equals(document.getSearchType())) {
			document.setSearchType(searchType);
		}
		
		return searchType;
	}
	
	protected boolean isUpdate(){
    	return getSearch().isSearchProductTypeOfUpdate();
    }
	
	@Override
	public DownloadImageResult lookupForImage(DocumentI document) throws ServerResponseException {
		findImageOnSsf(getSearch(), document, true, new ArrayList<DocumentInfoType>());
		return lookupForImage(document, document.getId());
	}
	
	@Override
	public DownloadImageResult lookupForImage(DocumentI doc, String documentIdStr) throws ServerResponseException {
		return null;
	}
		
	public ServerResponse imageSearch(Map<String, String> params) throws ServerResponseException{
		return null;
	}
}