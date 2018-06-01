package ro.cst.tsearch;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.Serializable;
import java.io.StringWriter;
import java.io.Writer;
import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;
import java.util.Set;
import java.util.TreeMap;
import java.util.Vector;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.mail.Transport;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;

import mailtemplate.MailTemplateUtils;

import org.apache.commons.lang.math.NumberUtils;
import org.apache.log4j.Category;
import org.apache.log4j.Logger;
import org.springframework.jdbc.core.simple.SimpleJdbcTemplate;
import org.w3c.dom.Document;

import ro.cst.tsearch.SearchFlags.CREATION_SOURCE_TYPES;
import ro.cst.tsearch.AutomaticTester.SearchRecord;
import ro.cst.tsearch.bean.SearchAttributes;
import ro.cst.tsearch.bean.SearchLogRow;
import ro.cst.tsearch.bean.TSDIndexPage;
import ro.cst.tsearch.community.CommunityAttributes;
import ro.cst.tsearch.community.CommunityUtils;
import ro.cst.tsearch.community.Products;
import ro.cst.tsearch.connection.http.HTTPManager;
import ro.cst.tsearch.connection.http.HTTPSiteManagerInterface;
import ro.cst.tsearch.connection.http2.HttpManager;
import ro.cst.tsearch.connection.http2.HttpSiteManager;
import ro.cst.tsearch.connection.http3.HttpManager3;
import ro.cst.tsearch.data.CountyConstants;
import ro.cst.tsearch.data.StateContants;
import ro.cst.tsearch.data.User;
import ro.cst.tsearch.database.DBManager;
import ro.cst.tsearch.database.DBReports;
import ro.cst.tsearch.database.DBSearch;
import ro.cst.tsearch.database.rowmapper.ImageCount;
import ro.cst.tsearch.database.rowmapper.NoteMapper;
import ro.cst.tsearch.database.rowmapper.OrderCount;
import ro.cst.tsearch.database.rowmapper.RequestCount;
import ro.cst.tsearch.emailClient.EmailClient;
import ro.cst.tsearch.emailOrder.MailOrder;
import ro.cst.tsearch.exceptions.BaseException;
import ro.cst.tsearch.exceptions.SaveSearchException;
import ro.cst.tsearch.exceptions.ServerResponseException;
import ro.cst.tsearch.exceptions.UpdateDBException;
import ro.cst.tsearch.extractor.xml.GenericFunctions;
import ro.cst.tsearch.generic.Util;
import ro.cst.tsearch.processExecutor.client.ClientProcessExecutor;
import ro.cst.tsearch.propertyInformation.Instrument;
import ro.cst.tsearch.search.ServerSearchesIterator;
import ro.cst.tsearch.search.address.StandardAddress;
import ro.cst.tsearch.search.filter.newfilters.legal.GenericLegal;
import ro.cst.tsearch.search.filter.newfilters.name.GenericNameFilter;
import ro.cst.tsearch.search.filter.newfilters.name.NameFilterFactory;
import ro.cst.tsearch.search.filter.parser.name.NameParser;
import ro.cst.tsearch.search.name.CompanyNameExceptions;
import ro.cst.tsearch.search.name.NameUtils;
import ro.cst.tsearch.search.tokenlist.NameTokenList;
import ro.cst.tsearch.search.util.AdditionalInfoKeys;
import ro.cst.tsearch.searchsites.client.GWTDataSite;
import ro.cst.tsearch.servers.HashCountyToIndex;
import ro.cst.tsearch.servers.bean.DataSite;
import ro.cst.tsearch.servers.info.TSServerInfo;
import ro.cst.tsearch.servers.info.TSServerInfoModule;
import ro.cst.tsearch.servers.parentsite.County;
import ro.cst.tsearch.servers.parentsite.State;
import ro.cst.tsearch.servers.response.ImageLinkInPage;
import ro.cst.tsearch.servers.response.OCRParsedDataStruct;
import ro.cst.tsearch.servers.response.ParsedResponse;
import ro.cst.tsearch.servers.response.SaleDataSet;
import ro.cst.tsearch.servers.types.TSInterface;
import ro.cst.tsearch.servers.types.TSInterface.DownloadImageResult;
import ro.cst.tsearch.servers.types.TSServer;
import ro.cst.tsearch.servers.types.TSServerDASL;
import ro.cst.tsearch.servers.types.TSServersFactory;
import ro.cst.tsearch.servlet.FileServlet;
import ro.cst.tsearch.servlet.ValidateInputs;
import ro.cst.tsearch.settings.Settings;
import ro.cst.tsearch.templates.MultilineElementsMap;
import ro.cst.tsearch.templates.TemplateBuilder;
import ro.cst.tsearch.threads.ASMaster;
import ro.cst.tsearch.threads.ASThread;
import ro.cst.tsearch.threads.AsynchSearchLogSaverThread;
import ro.cst.tsearch.threads.OCRDownloader;
import ro.cst.tsearch.threads.OCRDownloaderPool;
import ro.cst.tsearch.threads.a2ps.A2PSJobsSolver;
import ro.cst.tsearch.threads.general.CheckTaxDatesExpiredThread;
import ro.cst.tsearch.titledocument.TSDManager;
import ro.cst.tsearch.titledocument.abstracts.Chapter;
import ro.cst.tsearch.titledocument.abstracts.ChapterUtils;
import ro.cst.tsearch.titledocument.abstracts.DocTypeNode;
import ro.cst.tsearch.titledocument.abstracts.DocumentTypes;
import ro.cst.tsearch.user.UserAttributes;
import ro.cst.tsearch.user.UserManager;
import ro.cst.tsearch.user.UserUtils;
import ro.cst.tsearch.utils.CurrentInstance;
import ro.cst.tsearch.utils.DBConstants;
import ro.cst.tsearch.utils.FileCopy;
import ro.cst.tsearch.utils.FileUtils;
import ro.cst.tsearch.utils.FormatDate;
import ro.cst.tsearch.utils.GBManager;
import ro.cst.tsearch.utils.IndividualLogger;
import ro.cst.tsearch.utils.InstanceManager;
import ro.cst.tsearch.utils.Log;
import ro.cst.tsearch.utils.MemoryAllocation;
import ro.cst.tsearch.utils.Roman;
import ro.cst.tsearch.utils.SearchLogger;
import ro.cst.tsearch.utils.SharedDriveUtils;
import ro.cst.tsearch.utils.Sign;
import ro.cst.tsearch.utils.StringUtils;
import ro.cst.tsearch.utils.TSOpCode;
import ro.cst.tsearch.utils.Tidy;
import ro.cst.tsearch.utils.URLMaping;
import ro.cst.tsearch.utils.XStreamManager;
import ro.cst.tsearch.utils.XmlUtils;

import com.gwt.utils.client.UtilsAtsGwt;
import com.stewart.ats.archive.OcrFileArchiveSaver;
import com.stewart.ats.base.address.Address;
import com.stewart.ats.base.address.AddressI;
import com.stewart.ats.base.document.AssessorDocumentI;
import com.stewart.ats.base.document.BoilerPlateObject;
import com.stewart.ats.base.document.DocumentI;
import com.stewart.ats.base.document.DocumentI.SearchType;
import com.stewart.ats.base.document.DocumentUtils;
import com.stewart.ats.base.document.Image;
import com.stewart.ats.base.document.ImageI;
import com.stewart.ats.base.document.InstrumentI;
import com.stewart.ats.base.document.MortgageI;
import com.stewart.ats.base.document.Patriots;
import com.stewart.ats.base.document.PriorFileAtsDocumentI;
import com.stewart.ats.base.document.PriorFileDocumentI;
import com.stewart.ats.base.document.RegisterDocument;
import com.stewart.ats.base.document.RegisterDocumentI;
import com.stewart.ats.base.document.RestoreDocumentDataI;
import com.stewart.ats.base.document.SSFPriorFileDocument;
import com.stewart.ats.base.document.TaxDocumentI;
import com.stewart.ats.base.document.TransferI;
import com.stewart.ats.base.document.sort.EqualsInstrumentAbstractionImpl;
import com.stewart.ats.base.legal.Legal;
import com.stewart.ats.base.legal.LegalI;
import com.stewart.ats.base.legal.Subdivision;
import com.stewart.ats.base.legal.TownShip;
import com.stewart.ats.base.name.Name;
import com.stewart.ats.base.name.NameI;
import com.stewart.ats.base.parties.Party;
import com.stewart.ats.base.parties.PartyI;
import com.stewart.ats.base.property.PropertyI;
import com.stewart.ats.base.search.DocumentsManager;
import com.stewart.ats.base.search.DocumentsManagerI;
import com.stewart.ats.base.search.SsfDocumentMapper;
import com.stewart.ats.base.warning.Warning;
import com.stewart.ats.base.warning.WarningInfo;
import com.stewart.ats.base.warning.WarningInfoI;
import com.stewart.ats.base.warning.WarningManager;
import com.stewart.ats.base.warning.WarningUtils;
import com.stewart.ats.tsrindex.client.InstrumentG;
import com.stewart.ats.tsrindex.client.SKLDInstrumentG;
import com.stewart.ats.tsrindex.client.SearchStatus;
import com.stewart.ats.tsrindex.client.SimpleChapter;
import com.stewart.ats.tsrindex.client.SimpleChapterUtils;
import com.stewart.ats.tsrindex.client.SimpleChapterUtils.DType;
import com.stewart.ats.tsrindex.client.SimpleChapterUtils.PType;
import com.stewart.ats.tsrindex.client.SimpleChapterUtils.SavedFromType;
import com.stewart.ats.tsrindex.client.ocrelements.LegalDescription;
import com.stewart.ats.tsrindex.client.ocrelements.LegalDescriptionI;
import com.stewart.ats.tsrindex.server.DocumentDataRetreiver;
import com.stewart.ats.tsrindex.server.TsdIndexPageServer;
import com.stewartworkplace.starters.ssf.services.connection.DocAdminConn;
import com.stewartworkplace.starters.ssf.services.docadmin.DocumentInfoType;
import com.thoughtworks.xstream.XStream;
import com.thoughtworks.xstream.io.xml.CompactWriter;


@SuppressWarnings("serial")
public class Search implements Serializable, Cloneable {
	
	//-------------------------------------
	//--- start static declaration zone ---
	protected static final Category logger= Logger.getLogger(Search.class);
	public static final long NO_UPDATED_SEARCH = -333;
	    
    public static final int AUTOMATIC_SEARCH					= 0;
    public static final int INTERACTIVE_SEARCH					= 1;
    public static final int PARENT_SITE_SEARCH					= 2;
    public static final int GO_BACK_ONE_LEVEL_SEARCH			= 3;
    public static final int SAVE_SEARCH							= 4;
    public static final int SEARCH_NONE 						= -2;
    public static final long  FROM_MAIL_ORDER_SEARCH_ID 		= -40;
    public static final long  FROM_PRESENCE_TESTER_SEARCH_ID	= -50;
    public static final long  FROM_AUTOMATIC_TESTER_SEARCH_ID	= -60;
    
    
    public static final int SITE_ASSESOR						= 0;
    public static final int SITE_CNTY_TAX						= 1;
    public static final int SITE_CITY_TAX						= 2;
    public static final int SITE_REGISTER						= 3;
    
    public static final long STATE_NONE 						= 0;
	public static final long STATE_FROM_PARENT_SITE 			= 1;
    public static final long STATE_FROM_DATASOURCE_SEARCH		= 2;
	
	public static final int SEARCH_STATUS_N						= 0;
	public static final int SEARCH_STATUS_T						= 1;
	public static final int SEARCH_STATUS_K						= 2;
	public static final int SEARCH_STATUS_ATC					= -2000; // Automatic Test Configuration
	
	public static final int SEARCH_TSR_NOT_CREATED				= 0;
	public static final int SEARCH_TSR_CREATED					= 1;
	
	public static final String DB_SEARCH_STATUS_ID 				= "STATUS_ID";
	public static final String DB_SEARCH_STATUS_NAME 			= "STATUS_SHORT_NAME";
	public static final String DB_SEARCH_STATUS_DESCRIPTION 	= "STATUS_DESCRIPTION";
    
    private final String TEMP_DIR 								= "temp";
    public final static String TEMP_DIR_FOR_TEMPLATES 			= "temp_generated";
    
    public final static String VERSION_FILENAME					= "version.txt";
    
    public static final long MAX_XML_FILE_SIZE					= 40 * 1024 * 1024;
    public static final long MAX_CONTEXT_FILE_SIZE				= 60 * 1024 * 1024;
    public static final int MAX_TSR_INDEX_DOCUMENT_COUNT		= 500;
    public static final int MAX_NO_OF_DOCUMENTS_SAVED_PER_MODULE = 200;
    
    public static final int MAX_NO_OF_DOCUMENTS_FROM_SITE_TO_ANALYZE = 500;
    public static final int MAX_NO_OF_DOCUMENTS_FROM_DASL_TO_ANALYZE = 2000;
    public static final int MAX_NO_OF_DOCUMENTS_FROM_DASL_TP3_TO_ANALYZE = 4000;
    
	/**
	 * Enum that holds the main version of the Search Class<br>
	 * Current Version should be set to the last version created<br>
	 * Adding a new option must increase the version no and update getCurrentVersion method
	 */
	public static enum SEARCH_VERSION {
		PRIORFILE_CHANGE (1),
		PRIOR_BASE_SERCH_CORRECTION (2),
		KS_JOHNSON_AOM_TO_AM (3);
		private int version;
		
		private SEARCH_VERSION(int version) {
			this.version = version;
		}
		public int getVersion() {
			return version;
		}
		/**
		 * @return the max version available
		 */
		public static int getCurrentVersion() {
			return KS_JOHNSON_AOM_TO_AM.version;
		}
	}
    
    /**
     * @see ro.cst.tsearch.Search.addDataSource(DataSite)
     */
    public static enum RUN_TYPE {
		AUTO,
		BOTH,
		PS
	}
    
    //--- end static declaration zone ---
    //-----------------------------------
    
    private Date startDate = null;
	
	private DocumentsManagerI docManager = null;
	
    private boolean backgroundSearch = false;
    private String allImageSsfLink = "";
    private List<SsfDocumentMapper> ssfDocumentsUploaded = new Vector<SsfDocumentMapper>();
	
    transient private HashSet<String> companyNameWarnings = new HashSet<String>();
    ///// USELESS VARIABLES .. BACKWARD COMPATIBILITY
    @SuppressWarnings("unused")
	transient private boolean isFakeSearch					= false;
    @SuppressWarnings("unused")
	transient private String  ocrVestingInfo = "";
    @SuppressWarnings("unused")
	transient private BigDecimal cntyDelinqTax	= null;
    @SuppressWarnings("unused")
	transient private BigDecimal cityDelinqTax	= null;
    @SuppressWarnings({ "unused", "rawtypes" })
	transient private Hashtable dlDocs            = new Hashtable();
	@SuppressWarnings({ "unused", "rawtypes" })
	transient private Hashtable mpts 				= new Hashtable();
    @SuppressWarnings({ "unused", "deprecation" })
	private Chapter lastTransfer		= null;
    @SuppressWarnings("unused")
    private SaleDataSet lastTransferInformation = null;
////////////////////////////////////////////////////////////////////////////////////////////////////////
    
	transient private ServerSearchesIterator ssi 	= null;
	@SuppressWarnings({ "deprecation", "unused" })
	transient private ChapterUtils allChapters 	= null;
	@SuppressWarnings("rawtypes")
	transient private Map additionalInfo          = new HashMap();
	@SuppressWarnings("unused")	//kept for old search serialization compatibility
	transient private A2PSJobsSolver mJobSolver	= null;
	@SuppressWarnings("deprecation")
	transient private Chapter updatedTSDChapter 	= null; // ultimul capitol din index
	
	private SearchAttributes sa			= null;
    private SearchAttributes origSA 	= null;
    transient private SearchAttributes parentSA 	= null;
     
        
    @SuppressWarnings({ "deprecation", "unused" })	//kept for old search serialization compatibility
	private transient TSDIndexPage tsdIndexPage 	= null;
    //private TSDIndexPage tsdIndexPageSortGroupByInstruments 	= null;
    
    transient private Legal legal 		= null;

    
    private UserAttributes agent 		= null;
    private Long agentId                = null;
    
    private Long secondaryAbstractorId 	= null;
    
    private Sign sign					= null;
    private BigDecimal ownerId			= null;
    private BigDecimal buyerId			= null;
    private BigDecimal propertyId		= null;
     
    private boolean maxContextEvent		= false;
    private boolean useOldSSfTransactionIdFifPossible	= false;
    
	private int miSearchType;
    private int tsuNo = 0; // update number pt abtractor file id -ul din searchul curent daca sa bifat update
    private int citychecked;
    private int cityCheckedParentSite;
    
    private String msP1, msP2;
    private String msP1ParentSite, msP2ParentSite;
    private String msUserPath;

    private Map inMemoryDocs 			= new HashMap();   
    private Set removedInstr 			= new HashSet();
    private List visitedDocs 			= new ArrayList();
    
	
	private Hashtable dnDocs 			= null;// daily-news docs
	private Hashtable ocrDocs			= null;
	private Hashtable coDocs 			= new Hashtable();// courts docs
	private Hashtable pcDocs 			= new Hashtable();// pacer docs
	private Hashtable orDocs 			= new Hashtable(); // orbit docs
	private Hashtable uccDocs 			= new Hashtable(); // UCC docs
    private Hashtable prDocs            = new Hashtable(); //prdocs
    private Hashtable dtDocs            = new Hashtable(); //datatrace docs
    private Hashtable tsDocs			= new Hashtable(); //TSP docs
    private Hashtable sfDocs			= new Hashtable(); //SSF docs
    private Hashtable laDocs            = new Hashtable(); //la docs
    private Hashtable rvDocs			= new Hashtable();//rv docs
    private Hashtable piDocs			= new Hashtable();//pi docs
    
    private Vector<String> clickedDocs       = new Vector<String>(); //clicked documents in parent site
	
    @SuppressWarnings({ "unused", "rawtypes" })	//kept for old search serialization compatibility
	private transient Hashtable mapImgs 			= new Hashtable();
	
	private Hashtable plDocs 			= null;
	private Hashtable roDocs 			= null;
    private Hashtable imageProcessed 	= null; // imagini in curs de procesare
    private Hashtable fileProcessed 	= null; // pdf-uri de tip tsr sau tsu in curs de procesare     
    
    /**
     * Although is not used anymore it cannot be set transient because searches prior to documentsManager will not load anymore 
     */
    private Map registerData 			= new TreeMap(); 
    @SuppressWarnings("unused")	//kept for old search serialization compatibility
    private transient Map registerDataComp		= null; 
    private Map chapters 				= new HashMap();
    @SuppressWarnings("unused")	//kept for old search serialization compatibility
    private transient Map timings					= null;
    //kept for old search serialization compatibility
	private Map	imagesMap 				= null;
	private Map	crossRefSetMap 			= new HashMap();
	@SuppressWarnings("unused")	//kept for old search serialization compatibility
	private transient Map crossRefMap 			= new HashMap();
	private Vector instrumentListRef     = new Vector(); //instrument list used by ro.cst.tsearch.search.module.MortgageBookPageIterator
    
	/**
	 * Keeps the status of each server. For example at position <b>i</b> we have
	 * the status of server with site type <b>i</b><br>
	 * The status contains of an array holding the description of the server (name) and 
	 * the description of the status including time.
	 */
	private Vector<String[]> searchStatus  		= new Vector<String[]>();
	            
    private long searchID, ID;
    
    private long parentSearchId = NO_UPDATED_SEARCH;/* the search ID that was updated */
    /**
     * this is the searchId of the search(S) that was flagged for FVS and must be set on the FVS Update of that search(S)
     */
    private transient long FVSParentSearchID;
    
	private boolean searchStarted 		= false;
	private int TS_SEARCH_STATUS		= SEARCH_STATUS_N;
	private long searchState 			= STATE_NONE;
	/**
	 * kept for old search serialization compatibility
	 */
	@SuppressWarnings("unused")
	@Deprecated
	private transient int showAllChapters;
	public long disposeTime 			= 0;
	
	private String patriotSearchName 	= "";
	private String patriotKeyNumber		= "";
	private String TSDFileName			= "";
	public int searchCycle 				= 0; // al catelea ciclu de search s-a terminat
	/**
	 * kept for old search serialization compatibility
	 */
	@SuppressWarnings("unused")
	@Deprecated
	private transient boolean mustSaveSearchHTML;
	
	private boolean update 				= false;
	private String searchDirSuffix 		= "";
	/**
	 * kept for old search serialization compatibility
	 */
	@SuppressWarnings({ "unused", "rawtypes" })
	@Deprecated
	private transient Hashtable htmlIndexes;
	private Date TSROrderDate 			= new Date();
	private Hashtable visitedLinks		= new Hashtable();
    private Vector<String> validatedLinks    = new Vector<String>();
    private transient Vector<String> recursiveAnalisedLinks = new Vector<String>();
    
	@SuppressWarnings("unused") //kept for old search serialization compatibility
	private transient boolean adrNoChanged		= false;
	@SuppressWarnings("unused") //kept for old search serialization compatibility
	private transient boolean adrNameChanged		= false;
	@SuppressWarnings("unused") //kept for old search serialization compatibility
    private transient boolean ownerFNameChg       = false;
	@SuppressWarnings("unused") //kept for old search serialization compatibility
    private transient boolean ownerMNameChg       = false;
	@SuppressWarnings("unused") //kept for old search serialization compatibility
    private transient boolean ownerLNameChg       = false;
	@SuppressWarnings("unused") //kept for old search serialization compatibility
    private transient boolean coOwnerFNameChg       = false;
	@SuppressWarnings("unused") //kept for old search serialization compatibility
    private transient boolean coOwnerMNameChg       = false;
	@SuppressWarnings("unused") //kept for old search serialization compatibility
    private transient boolean coOwnerLNameChg       = false;
	@SuppressWarnings("unused") //kept for old search serialization compatibility
    private transient boolean buyerFNameChg       = false;
	@SuppressWarnings("unused") //kept for old search serialization compatibility
    private transient boolean buyerMNameChg       = false;
	@SuppressWarnings("unused") //kept for old search serialization compatibility
    private transient boolean buyerLNameChg       = false;
	@SuppressWarnings("unused") //kept for old search serialization compatibility
    private transient boolean coBuyerFNameChg       = false;
	@SuppressWarnings("unused") //kept for old search serialization compatibility
    private transient boolean coBuyerMNameChg       = false;
	@SuppressWarnings("unused") //kept for old search serialization compatibility
    private transient boolean coBuyerLNameChg       = false;
	
	private boolean orderedSearch 		= false;
	private boolean allowGetAgentInfoFromDB = true;
	
    private boolean wasOpened			  = false;

    private List<String> disabledChapters =new ArrayList<String>();
    
    private long versionSaveNo = 0;
    
    /**
     * Here should be the version of the current Search Implementation<br>
     * We use this to know which important updates were made in that version<br>
     * <b>0</b> - is the default version just before this field was added<br>
     * <b>1</b> - is the version in which we operated changes to the PRIORFILE category<br>
     * <br>New objects created should already use the upgraded version
     */
	private int									versionSearchNo						= SEARCH_VERSION.getCurrentVersion();
    
    private String jsSortChaptersBy		= "SORTBY_INST";
    private String sortAscendingChapters = "true";
    
    private String goBackType = "one_level";
    
    @SuppressWarnings("unused")
	private transient Date lastTransferDate           = null; 
    
    @SuppressWarnings("unused")
	@Deprecated
    private transient ImageLinkInPage lastTransferImage	= null;
    
    @SuppressWarnings("unused")
	@Deprecated
    private transient String legalDescriptionImageDiskLink		= null;    
    @Deprecated
    private transient OCRParsedDataStruct ocrData	= null;
    private Vector<String> patriotsAlertChapters = new Vector<String>();
    
    
    /**
     * This should be used to keep flags from ts_search_table into this object
     */
    private SearchFlags searchFlags		=	null;
    private String imagePath = null;
  
    
    // the order of the chapter sorted by instrument(group by)
    private Vector<String> vecChaptKeysSorted = new Vector<String>();
    
    //the map with the state of the chapter sorted by instrument(group by)
    private HashMap chapterMap = new HashMap();
            
    // savedInstruments, addSavedInst(), hasSavedInst() are used to create a list of saved docs,
    // to enable filtering/validation after already saved docs
    private Set<String> savedInstruments = new HashSet<String>();        
    
    /**
     * counts how many times a search was opened
     */
    private AtomicInteger openCount = new AtomicInteger();    
    
    /**
     * the last date when the search was saved in the database
     */
    private transient Date lastSaveDate = null;
    
    private transient Date searchDueDate = null;
    
    private volatile int imageTransactionId = -1;
    
    /**
     * Marker for the start interval for when this order is worked upon<br>
     * Should be used to count how many seconds passed with this order opened<br>
     * At a save, if an user is working on this order, this marker will be reinitialized and the previous interval saved to the database 
     */
    private transient Date startIntervalWorkDate = null;
    
    protected Map<Integer, Integer> imagesCount = Collections.synchronizedMap(new HashMap<Integer,Integer>());
    
    public Map<Integer,Integer> getImagesCount() {
    	if(imagesCount==null) {
    		imagesCount = Collections.synchronizedMap(new HashMap<Integer,Integer>());
    	}
    	return imagesCount;
    }
    
    public int getImagesCount(Integer datasource) {
    	Integer count = getImagesCount().get(datasource);
    	if(count == null) {
    		count = 0;
    	}
    	return count;
    }
    
    public boolean isUseOldSSfTransactionIdFifPossible() {
		return useOldSSfTransactionIdFifPossible;
	}

	public void setUseOldSSFTransactionIdIfPossible(boolean useOldSSfTransactionIdFifPossible) {
		this.useOldSSfTransactionIdFifPossible = useOldSSfTransactionIdFifPossible;
	}

	public void setImagesCount(Integer datasource, int count) {
		getImagesCount().put(datasource, count);
		updateImageCount(datasource);
	}
    
    public void countNewImage(Integer datasource) {
    	getImagesCount().put(datasource,  getImagesCount(datasource)+1);
    	updateImageCount(datasource);
    }

    /**
     * Updates the image count for this data source plus the total for all data sources
     * @param datasource current data source to be updated
     */
	private void updateImageCount(Integer datasource) {
		/**
    	 * Save current image
    	 */
    	ImageCount imageCount = new ImageCount();
    	imageCount.setSearchId(getID());
    	imageCount.setDataSource(datasource);
    	imageCount.setCount(getImagesCount(datasource));
    	imageCount.setDescription(HashCountyToIndex.getServerAbbreviationByType(datasource) + "(" + getImagesCount(datasource) + ")");
    	imageCount.saveToDatabase();
    	
    	
    	/**
    	 * Update total images
    	 */
    	int countAllImages = 0;
    	StringBuilder sb = new StringBuilder();
    	for (Integer key : getImagesCount().keySet()) {
    		if(key != ImageCount.DATASOURCE_TOTAL) {
	    		if(sb.length() > 0) {
	    			sb.append(" ");
	    		}
				sb.append(HashCountyToIndex.getServerAbbreviationByType(key)).append("(").append(getImagesCount(key)).append(")");
				countAllImages += getImagesCount(key);
    		}
		}
    	
    	imageCount.setDataSource(ImageCount.DATASOURCE_TOTAL);
    	imageCount.setCount(countAllImages);
    	imageCount.setDescription(sb.toString());
    	imageCount.saveToDatabase();
	}
    
	public int getImageTransactionId() {
		return imageTransactionId;
	}

	public void setImageTransactionId(int imageTransactionId) {
		this.imageTransactionId = imageTransactionId;
	}

	public boolean hasTransactionId(){
		return imageTransactionId>0;
	}
	
	//START order count mechanism task 7825
	/**
	 * Count orders per datasource for report
	 * 
	 * @param datasource
	 */
	public void countOrder(String fileIdToCount, int datasource) {
//		if (isProductType(SearchAttributes.SEARCH_PROD_UPDATE)) {
//			return;
//		}

		if (org.apache.commons.lang.StringUtils.isBlank(fileIdToCount))
			return;

		int commId = getCommId();
		
		HashMap<String, Set<Integer>> orderCountCached = getSa().getOrderCountCached();
		Set<Integer> datasources = orderCountCached.get(fileIdToCount);
		//check if cached on search
		if(datasources != null && datasources.contains(datasource)) {
			return;
		}
		
		
		if (OrderCount.isOrderAlreadyCounted(commId, fileIdToCount, datasource)) {
			//check if count in database and if so cache on search to avoid extra sqls
			if(datasources == null) {
				datasources = new HashSet<Integer>();
				orderCountCached.put(fileIdToCount, datasources);
			}
			datasources.add(datasource);
			return;
		}

		try {
			OrderCount oc = new OrderCount();
			oc.setCommunityId(commId);
			oc.setFileId(fileIdToCount);
			oc.setDataSource(datasource);
			oc.setDateCounted(Calendar.getInstance().getTime());
			oc.setCountyId(Integer.parseInt(getCountyId()));
			oc.setSearchId(getID());
			oc.setProductId(getProductId());

			if(oc.saveToDatabase()) {
				//if save was successful I need to cache on search
				if(datasources == null) {
					datasources = new HashSet<Integer>();
					orderCountCached.put(fileIdToCount, datasources);
				}
				datasources.add(datasource);
				logger.debug("Counted order " + fileIdToCount + " for datasource " + 
						datasource + " on searchid " + getID());
			} else {
				logger.error("ERROR: Could not count order " + fileIdToCount + " for datasource " + 
						datasource + " on searchid " + getID());
			}

			
		} catch (Exception e) {
			e.printStackTrace();
			logger.error("Something went terribly wrong while counting order " + 
					fileIdToCount + " for datasource " + datasource + " on searchid " + getID(), e);
		}

	}
	//END order count
	
	
	//start req count task 7846
	private transient HashMap<Integer, RequestCount>	reqCountMap	= null;

	public HashMap<Integer, RequestCount> getRequestCountMap() {
		if (reqCountMap == null)
			reqCountMap = RequestCount.getRequestCountForSearchID(this.getID());
		reqCountMap.remove(RequestCount.DATASOURCE_TOTAL);
		return reqCountMap;
	}

	// task 7846
	public void countRequest(int datasource, int type) {
		try {
			if(datasource == GWTDataSite.TS_TYPE){
				return;
			}
			
			RequestCount rc = getRequestCountMap().get(datasource);

			if (rc == null) {
				rc = new RequestCount();
			}
			
			rc.setCommunityId(this.getCommId());
			rc.setDataSource(datasource);
			rc.setCountyId(Integer.parseInt(getCountyId()));
			rc.setSearchId(getSearchID());
			rc.incField(type);
			this.reqCountMap.put(datasource, rc);
			rc.saveToDatabase();
			
			updateRequestCount();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	// task 7846
	public void saveRequestCount() {
		RequestCount.saveRequestCountMapToDB(this.reqCountMap);
	}
	
	public void updateRequestCount(){ // used to centralize the request count
		HashMap<Integer, RequestCount> rcMap = getRequestCountMap();
		
		StringBuffer buf = new StringBuffer();
		
		for(Entry<Integer, RequestCount> rc : rcMap.entrySet()){
			buf.append(HashCountyToIndex.getServerAbbreviationByType(rc.getValue().getDataSource()) + 
					"(" + rc.getValue().getInstrumentCount()+"/"+rc.getValue().getNonInstrumentTotal() + ") ");
		}
		
		if(StringUtils.isNotEmpty(buf.toString().trim())){
			RequestCount rc = new RequestCount();
			rc.setSearchId(this.ID);
			rc.setCommunityId(this.getCommId());
			rc.setDataSource(RequestCount.DATASOURCE_TOTAL);
			rc.setCountyId(Integer.parseInt(getCountyId()));
			rc.setDescription(buf.toString().trim());
			rc.saveToDatabase(false);
		}
	}

	//end req count
	
	/**
     * This must be eliminated because the present test will be down using the documentManager only
     * But all in good time
     * @param inst
     */
    @Deprecated
    public void addSavedInst(String inst){ 
    	if(savedInstruments != null){ // de-serialization of old searches
    		savedInstruments.add(inst);
    	}
    }  
    
    public boolean hasSavedInst(String inst){
    	if(savedInstruments==null){  // de-serialization of old searches
    		return false; 
    	}
    	if(StringUtils.isEmpty(inst)){
    		return false;
    	}
    	String inst1 = inst.replaceFirst("^0+", "");
    	return savedInstruments.contains(inst) || 
    	       savedInstruments.contains(inst1); 
    }    
	
    public HashSet<String> getCompanyNameWarning(){
    	if (companyNameWarnings == null) companyNameWarnings = new HashSet<String>();
    	return companyNameWarnings;
    }
    
    public void addCompanyNameWarning(String warning){
    	getCompanyNameWarning().add(warning);
    }
    
    public void clearCompanyNameWarning(){
    	getCompanyNameWarning().clear();
    }
    
    public boolean warningDisplayed(String warning){
    	if (getCompanyNameWarning().contains(warning)){
    		return true;
    	} else {
    		return false;
    	}
    }
    
    public boolean isSkippedCompany(String companyName) {
    	for(Iterator<String> it = getCompanyNameWarning().iterator(); it.hasNext(); ) {
    		String warn = it.next();
    		if(warn.contains(CompanyNameExceptions.CCN_MESSAGE[0] +
    				companyName.toUpperCase() + CompanyNameExceptions.CCN_MESSAGE[1])) {
    			return true;
    		}
    	}
    	return false;
    }
    
    public boolean hasSavedYearInst(String year, String inst){
    	if(savedInstruments==null){  // de-serialization of old searches
    		return false; 
    	}    	
		if(StringUtils.isEmpty(inst) || StringUtils.isEmpty(year)){
			return false;
		}
		String inst1 = "year=" + year + ";inst=" + inst;
		String inst2 = "year=" + year + ";inst=" + (inst.replaceFirst("^0+", ""));
		return savedInstruments.contains(inst1) || savedInstruments.contains(inst2);
	}
	
    /**
     * Checks if a book-page exists in the memory (checked or unchecked)
     * Checks in the documentsManager and as a backward compatibility also in the savedInstruments object
     * @param book
     * @param page
     * @return true if the book-page is found
     */
	public boolean hasSavedBookPage(String book, String page){
		if(StringUtils.isEmpty(book) && StringUtils.isEmpty(page))
			return false;
    	if(docManager!=null){
    		try {
    			docManager.getAccess();
    			Collection<DocumentI> allChapters =  docManager.getDocumentsList( false );
		        for( DocumentI doc:allChapters)
		        {
					if(		book.equalsIgnoreCase(doc.getBook()) && 
							page.equalsIgnoreCase(doc.getPage())){
						return true;
					}
		        }
    		} catch (Exception e) {
				e.printStackTrace();
			} finally {
				docManager.releaseAccess();
			}
    	}
    	return hasSavedBookPageOldCompatibility(book, page);
	}
	
	private boolean hasSavedBookPageOldCompatibility(String book, String page){
		if(savedInstruments==null){  // de-serialization of old searches
    		return false; 
    	}		
		if(StringUtils.isEmpty(book) || StringUtils.isEmpty(page)){
			return false;
		}		
		String book1 = book.replaceFirst("^0+", "");
		String page1 = page.replaceFirst("^0+", "");
		return 
		savedInstruments.contains(book + "_" + page) ||
		savedInstruments.contains(book + "_" + page1) ||
		savedInstruments.contains(book1 + "_" + page) ||
		savedInstruments.contains(book1 + "_" + page1);
	}
    
    // stores which images are existent
    // for now used only on ILCook
    private Set<String> existingImages = new HashSet<String>();
    public boolean imageExists(String insNo){
    	return existingImages != null && existingImages.contains(insNo);
    }    
    public void addImageExists(String insNo){
    	if(existingImages != null) existingImages.add(insNo);
    }
    
    private Hashtable<String , ChapterSavedData>  savedTSDIndexState= new  Hashtable<String , ChapterSavedData>();
    

    public   synchronized void putChapterInChapterMap(String key, String[] value){
        if(chapterMap==null){
          chapterMap = new HashMap();
        }
    	chapterMap.put(key, value);
    }
    
    public   synchronized String[] getFromChapterMap(String key){
        if(chapterMap==null){
          chapterMap = new HashMap();
        }
    	return (String[])chapterMap.get(key);
    }
    
    public   synchronized void clearChapterMap(){
      if(chapterMap==null){
          chapterMap = new HashMap();
        }
    	chapterMap.clear();
    }
    
	////////////////////////////////////////////////////////////////////////////////////////////////////////
	private static volatile String[] serverTypes;
	//cityType check and serverType
	private static  volatile String[] serverTypesAbrev;
	
	
	public  static String [] getReadOnlyServerTypesAbrev(){
		if(serverTypesAbrev ==null){
			serverTypesAbrev  = HashCountyToIndex.getServerTypesAbrev();
		}
		return serverTypesAbrev.clone();
	}
	
	
	public static String SERVER_NA_MSG = "not available";
	public static String TIME_OUT = "time-out";
	public static String SKIPPED = "skipped";
	
	//declares and creates a search record field 
	transient private SearchRecord sr = new SearchRecord();
		 
	//returns the search record of the object 
	public SearchRecord getSearchRecord(){
		
		if( sr == null ){
			sr = new SearchRecord();
		}
		
		return sr;
		
	}
	
	/**
	 * Check if the county is implemented in automatic for current product type<br>
	 * Implemented means at least one server activated in automatic
	 * @return <b>true</b> only if the county is implemented in automatic for current product type
	 */
	public boolean isCountyImplemented() {
	    
	    Vector<DataSite> allDataSites = HashCountyToIndex.getAllDataSites(
	    		getCommId(), 
	    		sa.getAtribute(SearchAttributes.P_STATE_ABREV), 
	    		sa.getCountyName(), 
	    		true, 
	    		getProductId());
	    
	    return allDataSites.size() > 0;
	    
	}

	public void fillSearchStatus() {
		if (searchStatus.size() >= 100) return;
		
		for (int i = 0; i < 100; i++) {
			if (!checkServerTypeStatus(getServerTypes()[i])
			&& !isServerImpl(i)) {
			    try {
			        searchStatus.setElementAt(new String[] { getServerTypes()[i], SERVER_NA_MSG }, i);
			    } catch (Exception e) {}
			}
		}
	}

	public boolean isServerImpl(int srvType) {
		if(srvType >= serverTypesAbrev.length || srvType < 0)
			return false;
		int id= Integer.parseInt(sa.getAtribute(SearchAttributes.P_COUNTY));
		boolean ok = true;
		try {
			TSServersFactory.IdToClass((int)
				TSServersFactory.getSiteIdfromCountyandServerTypeId(id,
					TSServersFactory.getSiteTypeId(serverTypesAbrev[srvType])));
		} catch(Exception e) {
			ok = false;
		}
		return ok;
	}
	
	private boolean checkServerTypeStatus(String srvType) {
		for (int i = 0; i < searchStatus.size(); i++) {
			String[] st = (String[]) searchStatus.elementAt(i);
			if (st[0].equals(srvType)) {
				return true;
			}
		}
		return false;
	}
	
	
	public static String getServerTypeFromCityChecked(int i) {
		if (i >= getServerTypes().length || i < 0)
			return "";
		return getServerTypes()[i];
	}
	
	public static String getServerAbbrevTypeFromCityChecked(int i) {
		if (i >= serverTypesAbrev.length || i < 0)
			return "";
		return serverTypesAbrev[i];
	}

	public String getServerStatusFromCityChecked(int i) {
		if (i >= getServerTypes().length || i < 0)
			return SERVER_NA_MSG;
		String srvType = getServerTypeFromCityChecked(i);
		for (int j = 0; j < searchStatus.size(); j++) {
			String[] st = (String[]) searchStatus.elementAt(j);
			if (st[0].equals(srvType)) {
				return st[1];
			}
		}
		return SERVER_NA_MSG;
	}
				
    private Search() {}

    
    public Search (long iSearchID) {  
        logger.info ( "new Search Initiated with ID:" + iSearchID);    
        if(iSearchID>0){
        	startDate = DBManager.getStartDate(iSearchID);
        }
        if(startDate==null){
        	startDate = new Date();
        }
        setSearchID(iSearchID);
        setID(iSearchID);
        //searchID = iSearchID;
        //ID = iSearchID;
        sa = new SearchAttributes(getID());
        fileProcessed = new Hashtable();
        resetSearchStatus();    	
        legal = new Legal();
        MemoryAllocation.getInstance().addAllocatedSearch();
        docManager = new DocumentsManager(searchID);
    }
    
    public Date getStartDate() {
    	if(startDate == null) {
    		startDate = DBManager.getStartDate(getID());
    	}
	    if(startDate == null){
	    	startDate = new Date();
	    }
		return startDate;
	}
	public void setStartDate(Date startDate) {
		this.startDate = startDate;
	}
	public Search(User user, long iSearchID) {
        logger.info ( "new Search Initiated with ID:" + iSearchID);
        if(iSearchID>0){
        	startDate =DBManager.getStartDate(iSearchID);
        }
        if(startDate==null){
        	startDate = new Date();
        }
        msUserPath = user.getUserPath();
        setSearchID(iSearchID);
        setID(iSearchID);
        //searchID = iSearchID;
        //ID = iSearchID;
        reset(user);
        fileProcessed = new Hashtable();
        resetSearchStatus();
        legal = new Legal();
        MemoryAllocation.getInstance().addAllocatedSearch();
        docManager = new DocumentsManager(searchID);
    }

    public Search(User user, long iSearchID, Search oldSearch) {
        logger.info ( "new Search Initiated with ID:" + iSearchID);
        if(iSearchID>0){
        	startDate =DBManager.getStartDate(iSearchID);
        }
        if(startDate==null){
        	startDate = new Date();
        }
        msUserPath = oldSearch.msUserPath;
        setSearchID(iSearchID);
        setID(iSearchID);
        //searchID = iSearchID;
        //ID = iSearchID;
        reset(user);
        fileProcessed = new Hashtable();
        resetSearchStatus();
        legal = new Legal();
        MemoryAllocation.getInstance().addAllocatedSearch();
        docManager = new DocumentsManager(searchID);
    }
	
    /**
     * ChaptersMap
     */
    public Map getChaptersMap() {
        return chapters;
    }
    /**
     * SearchID
     */
    public long getSearchID() {
    	return getID();
        //return searchID;
    }
    public void setSearchID(long searchID) {
    	setID(searchID);
        //this.searchID = searchID;
    }

    /**
     * ID - DB search ID
     */    
    public long getID() {
        return ID;
    }
    public void setID(long ID) {
    	this.searchID = ID;
        this.ID = ID;
    }
    
    /**
     * InMemoryDoc
     */
    public synchronized  void addInMemoryDoc (String key, String page) {
    	getInMemoryDocs().put(key,page); 
    }
    public synchronized  void addInMemoryDoc (String key, Object page) {
    	
    	getInMemoryDocs().put(key,page); 
    }
    public synchronized  boolean existsInMemoryDoc(String key) {
        return getInMemoryDocs().containsKey(key);
    }
    public synchronized  Object removeInMemoryDoc(String key) {
       return  getInMemoryDocs().remove(key);
    }
    public synchronized  void removeAllInMemoryDocs() {
    	getInMemoryDocs().clear();
    }
    public synchronized  int getInMemoryDocsCount() {
        return getInMemoryDocs().size();
    }
    public synchronized  Object getInMemoryDoc (String key) {
        return getInMemoryDocs().get(key);
    }
    
    public synchronized Map getAllInMemoryDocCopy() {
    	Map map = new HashMap();
    	map.putAll(getInMemoryDocs());
    	return map;
    }
    
    private Map getInMemoryDocs() {
    	if(inMemoryDocs == null) {
    		inMemoryDocs = new HashMap();
    	}
		return inMemoryDocs;
	}
    
    /**
     * additionalInfo
     */
    public synchronized void removeAllAdditionaInfo(){
    	if( additionalInfo == null ){
    		additionalInfo = new HashMap();
    	}
    	
    	additionalInfo.clear();
    }
    public synchronized  Object getAdditionalInfo (String key) {
    	if( additionalInfo == null ){
    		additionalInfo = new HashMap();
    	}
        return additionalInfo.get(key);
    }
    public synchronized void setAdditionalInfo(String key, Object value){
    	if( additionalInfo == null ){
    		additionalInfo = new HashMap();
    	}
    	additionalInfo.put(key, value);
    }
    public synchronized void removeAdditionalInfo(String key){
    	if( additionalInfo == null ){
    		return;
    	}
    	if(additionalInfo.containsKey(key)){    		
    		additionalInfo.remove(key);
    	}
    }
    public synchronized  boolean existsAdditionalInfo(String key) {
    	if( additionalInfo == null ){
    		additionalInfo = new HashMap();
    	}
        return additionalInfo.containsKey(key);
    }    
    /**
     * SearchDir
     */
    // #1132
    public void constructSearchDirs() {
        if (getSearchID() > 0) {
	        File tmpFile= new File(this.getSearchDir()); 
	        FileUtils.deleteDir(tmpFile);
	        tmpFile.mkdirs();
	        tmpFile=new File(this.getSearchTempDir());
	        tmpFile.mkdirs();
	        tmpFile=new File(this.getSearchDir()+Search.TEMP_DIR_FOR_TEMPLATES);
	        tmpFile.mkdirs();
        } else {
            File tmpFile= new File(msUserPath + "fake" + File.separator); 	        
	        tmpFile.mkdirs();
	        tmpFile=new File(msUserPath + "fake" + TEMP_DIR + File.separator);
	        tmpFile.mkdirs();
        }

    }
    
    public void constructSearchDirs( boolean deleteOld ){
        if (getSearchID() > 0) {
	        File tmpFile= new File(this.getSearchDir()); 
	        if( deleteOld ){
	        	FileUtils.deleteDir(tmpFile);
	        }
	        tmpFile.mkdirs();
	        tmpFile=new File(this.getSearchTempDir());
	        tmpFile.mkdirs();
	        tmpFile=new File(this.getSearchDir()+Search.TEMP_DIR_FOR_TEMPLATES);
	        tmpFile.mkdirs();
        } else {
            File tmpFile= new File(msUserPath + "fake" + File.separator); 	        
	        tmpFile.mkdirs();
	        tmpFile=new File(msUserPath + "fake" + TEMP_DIR + File.separator);
	        tmpFile.mkdirs();
        }    	
    }
    
    public void removeSearchDir(){
    	
    	String searchDirPath    = getSearchDir().substring(0,getSearchDir().lastIndexOf(File.separator));
    	File    searchDir				= new File(searchDirPath);
    	   if (searchDir.isDirectory())
    		   FileUtils.deleteDir(searchDir);
    	   else 
    		   logger.info("Search directory " + searchDirPath +" it's not availbale on the filesystem !!");
    	
    }
    
    public void setSearchDirSuffix(String searchDirSuffix) {
        this.searchDirSuffix = searchDirSuffix;
    }
    public String getSearchDirSuffix() {
        return searchDirSuffix;
    }
    
    public String getSearchDir() {
        return msUserPath + getSearchID() + searchDirSuffix + File.separator; 
    }
    
    public String getImagesTempDir() {
    	String imageTempDirectory = ServerConfig.getImageDirectory() + File.separator + 
    			"tempImages"+ File.separator + 
    			getID() + File.separator;
		FileUtils.CreateOutputDir(imageTempDirectory); 
		return imageTempDirectory;
    }
    
    public String getSearchTempDir() {
        return this.getSearchDir() + TEMP_DIR + File.separator;
    }
    
    public String getRelativePath() {
        
        String searchDir = getSearchDir();
        
        String relativePath = searchDir.substring(searchDir.indexOf(TSDManager.TSDDir), searchDir.length());
        
        return relativePath;
    }

    /**
     * Reset Search
     */
    private void reset(User user) {
        
        this.sa = new SearchAttributes(this.getID());
        
        miSearchType = PARENT_SITE_SEARCH;
        
        UserAttributes userAttributes = user.getUserAttributes();
        sa.setAbstractor(userAttributes);

        /*
        //set default tsr index ordering
    	if(!userAttributes.getTSR_SORTBY().equals("")) 
    		this.jsSortChaptersBy = userAttributes.getTSR_SORTBY();
    	
    	if(userAttributes.getTSR_SORT_DIRECTION() == -1) 
    		this.sortAscendingChapters = "true";
        */
        
        //set default agent
        int defaultAgent = userAttributes.getMyAtsAttributes().getSEARCH_PAGE_AGENT().intValue();
        if(defaultAgent>0) 
        	 this.setAgent(UserManager.getUser(new BigDecimal(defaultAgent)));       
        
        // order by date, old version of agents
        UserAttributes agentAttributes = user.getUserAgentClient();
        if (agentAttributes != null) {
        	
        	agentAttributes = UserManager.getUser(agentAttributes.getID());			
        	logger.info("AgentID set for user " + userAttributes.getID() + " (" + userAttributes.getLOGIN() + ") commId= " + 
        			userAttributes.getCOMMID() + " to value " + 
        			agentAttributes.getID() + " (" + agentAttributes.getLOGIN() + ") commId = " + agentAttributes.getCOMMID());
            sa.setOrderedBy(agentAttributes);
            this.setAgent(agentAttributes);
        }

        try {
        	//check to see if user has a default state and county specified in his 'My Profile' page
    		int userState = userAttributes.getMyAtsAttributes().getSEARCH_PAGE_STATE().intValue();
    		int userCounty = userAttributes.getMyAtsAttributes().getSEARCH_PAGE_COUNTY().intValue();
        	String userTsrSortBy = userAttributes.getMyAtsAttributes().getTSR_SORTBY();
        	setJsSortOrder(userTsrSortBy, sortAscendingChapters);
        	
            String defaultStateId = State.getStateFromAbv("TN").getStateId().toString();
            String lastState = new Integer(Settings.selectAttributes(Settings.SEARCH_MODULE,
                                                                     Settings.LAST_STATE,
                                                                     userAttributes.getID())).toString();
            if (!lastState.equals(new Integer(Settings.NO_VALUE).toString()))
                defaultStateId = lastState;
            
            defaultStateId = (userState>0)?(new Integer(userState).toString()):defaultStateId;  
            
            sa.setAtribute (SearchAttributes.P_STATE ,defaultStateId); // last state

            String defaultCountyId = new Integer(Settings.NO_VALUE).toString();
            
            String lastCounty = new Integer(Settings.selectAttributes(Settings.SEARCH_MODULE,
                                                                      Settings.LAST_COUNTY,
                                                                      userAttributes.getID())).toString();

            if (!lastCounty.equals(new Integer(Settings.NO_VALUE).toString()))
            	defaultCountyId = lastCounty;
            
            defaultCountyId = (userCounty>0)?(new Integer(userCounty).toString()):defaultCountyId;

            if (!defaultCountyId.equals(Integer.valueOf(Settings.NO_VALUE).toString()))
            	sa.setAtribute (SearchAttributes.P_COUNTY, defaultCountyId);

            try {
            	
            	County county = County.getCounty(Integer.parseInt(defaultCountyId));
            	if(county.getCountyId().intValue() > 0) {
	            	int index = HashCountyToIndex.getFirstServer(getProductId(), getCommId(), Integer.parseInt(sa.getCountyId()));
	            	
	            	HashCountyToIndex.setSearchServer(this, index);
            	}
            	
            } catch (Exception ignored) {
            	logger.error("Error Setting Server for searchid " + getID(), ignored);
            	
            }

        } catch (Exception e) {//should not happen
            logger.error ("Error when seting the server ", e);
            e.printStackTrace();
        }

        constructSearchDirs();
    }
    
    /** 
     * Used in the past before Document Manager and has to be kept in order to load prior searches
     */
    @Deprecated
    public Map getRegisterMap() {
        return registerData;
    }
	
    
    /**
     * Sign
     */
    public Sign getSign() {
        return sign;
    }
    public void setSign(Sign sign) {
        this.sign= sign;
    }
    
    /**
     * SearchAttributes
     */ 
    public SearchAttributes getSa() {
        return sa;
    }
    public void setSa(SearchAttributes attributes) {
        sa= attributes;
    }
    
    public int getSearchProduct(){
    	String productTypeAsString = sa.getAtribute(SearchAttributes.SEARCH_PRODUCT);
    	if(StringUtils.isEmpty(productTypeAsString)) {
    		return 1;
    	}
    	int searchType = Integer.parseInt(productTypeAsString);
        return searchType;
    }
    /**
     * test if search has an update-like transaction type: UPDATE, FVS
     * @return
     */
    public boolean isSearchProductTypeOfUpdate(){
    	String productTypeAsString = sa.getAtribute(SearchAttributes.SEARCH_PRODUCT);
    	
    	if (StringUtils.isEmpty(productTypeAsString)) {
    		return false;
    	}
    	int productType = Integer.parseInt(productTypeAsString);
    	
    	return Products.isOneOfUpdateProductType(productType);
    	
    }
    
    /**
     * Possible values:<br>
     * AUTOMATIC_SEARCH<br>INTERACTIVE_SEARCH<br>PARENT_SITE_SEARCH<br>GO_BACK_ONE_LEVEL_SEARCH
     */
    public int getSearchType() {
        return miSearchType;
    }
    public void setSearchType(int searchType) {
        miSearchType= searchType;
    }
    
    public boolean isParentSiteSearch(){
    	if (miSearchType == PARENT_SITE_SEARCH){
    		return true;
    	}
    	
    	return false;
    }
    
    public boolean isAutomaticSearch(){
    	if (miSearchType == AUTOMATIC_SEARCH){
    		return true;
    	}
    	
    	return false;
    }
    /**
     * P1.
     */
    public String getP1() {
        return msP1;
    }
    public void setP1(String p1) {
    	if(msP1 == null || !msP1.equals(p1)){
	    	try{
	    		County county = County.getCounty(new BigDecimal(HashCountyToIndex.getCountyId(p1)));
	    		String key  = county.getState().getStateAbv() + county.getName();
		    	if("ILCook".equals(key)){
		    		jsSortChaptersBy = "SORTBY_SRCTYPE";
		    	}
	    	}catch(Exception e){}
    	}
        msP1= p1;
        
    }
    
    /**
     * P2.
     */
    public String getP2() {
        return msP2;
    }
    public void setP2(String p2) {
        msP2= p2;
    }
    
    
    
    public String getP1ParentSite() {
    	if(msP1ParentSite == null) {
    		return msP1;
    	}
		return msP1ParentSite;
	}
	public void setP1ParentSite(String msP1ParentSite) {
		this.msP1ParentSite = msP1ParentSite;
	}
	public String getP2ParentSite() {
		if (msP2ParentSite == null) {
			return msP2;
		}
		return msP2ParentSite;
	}
	public void setP2ParentSite(String msP2ParentSite) {
		this.msP2ParentSite = msP2ParentSite;
	}
	/**
     * Citychecked
     */
    public int getCitychecked() {
        return citychecked;
    }
    public void setCitychecked(int i) {
        citychecked= i;
    }
    
    
    
    public int getCityCheckedParentSite() {
		return cityCheckedParentSite;
	}
	public void setCityCheckedParentSite(int cityCheckedParentSite) {
		this.cityCheckedParentSite = cityCheckedParentSite;
	}
	/**
     * SearchIterator
     */
    public ServerSearchesIterator getSearchIterator() {
        return ssi;
    }
    public void setSearchIterator(ServerSearchesIterator iterator) {
        ssi = iterator;
    }

  
    public synchronized  void removeAllRemovedInstruments() {
        removedInstr.clear();
    }
    public synchronized  void addRemovedInstr(String instrNo) {
        removedInstr.add(instrNo);
    }
    

    /**
     * UpdatedTSDChapter - no longer needed, the field will not be serialized any more
     */
    @Deprecated
    public Chapter getUpdatedTSDChapter() {
        return updatedTSDChapter;
    }
    @Deprecated
    public void setUpdatedTSDChapter(Chapter updatedTSDChapter) {
        this.updatedTSDChapter = updatedTSDChapter;
    }
    
    /**
     * ImageProcessed
     */
    public Hashtable getImageProcessed() {
        return imageProcessed;
    }
    public void setImageProcessed(Hashtable imageProcessed) {
        this.imageProcessed = imageProcessed;
    }
    
    /**
     * VisitedDocs
     */
    public synchronized  void removeAllVisitedDocs() {
        visitedDocs.clear();
    }
    public synchronized  void addVisitedDoc(String fileName) {
        visitedDocs.add(fileName);
    }
    public synchronized  boolean containsVisitedDoc(String fileName) {
        return visitedDocs.contains(fileName);
    }

    /**
     * Convert Process
     */
    public synchronized  void setStartConvert(String fileName) {
        fileProcessed.put(fileName, "");
    }
    public synchronized  ClientProcessExecutor setStartConvertProcess(String fileName, String[] command) throws IOException {
        Object o = fileProcessed.get(fileName);
        if ( o != null )
        {
            ClientProcessExecutor cpe = new ClientProcessExecutor( command, true, true );
            cpe.start();
            
            fileProcessed.put(fileName, cpe);
            
            return cpe;
        }
        else
        {
            return null;
        }
    }
    public boolean isFileProcessed(String fileName) {
        return( fileProcessed.get(fileName) != null );
    }
    public void setStopConvert(String fileName) {
        Object o = fileProcessed.remove(fileName);
        if ( o != null ) {
            if (o instanceof Process) {
                ((Process)o).destroy();
            }
            try {
                File fileNameObj = new File(fileName);
                if (  fileNameObj != null && fileNameObj.exists() &&
                      !fileNameObj.isDirectory() ) {
                    fileNameObj.delete();
                }
            } catch (Exception ioe) {
                ;
            }
        }
    }
    public void setNormalStopConvert(String fileName) {
        fileProcessed.remove(fileName);
    }

    /**
     * TsuNo
     */
    public void setTsuNo(int tsuNo) {
        this.tsuNo = tsuNo;
    }
    public int getTsuNo() {
        return tsuNo;
    }

    /**
     * Agent
     */
    public UserAttributes getAgent() {
    	BigDecimal currentCommunity = null;
    	try {
    		CommunityAttributes communityAttributes = InstanceManager.getManager().getCurrentInstance(getID()).getCurrentCommunity();
    		if(communityAttributes != null) {
    			currentCommunity = communityAttributes.getID();
    		}
    	} catch (Exception e) {
			e.printStackTrace();
		}
    	UserAttributes ret = null;
	        if( agentId != null )
	        {
	            //new search
	            try
	            {
	            	ret = UserUtils.getUserFromId( agentId.longValue() ); 
	            	if(currentCommunity != null && ret != null && !currentCommunity.equals(ret.getCOMMID()))
	            		return null;
	                return ret;
	            }
	            catch( Exception e )
	            { }
	        }
	        else
	        {
	            //old search, get the agent id from the agent object
	            if( agent != null )
	            {
	                Long savedAgentId = agent.getID().longValue();
	                
	                try
	                {
	                	ret = UserUtils.getUserFromId( savedAgentId.longValue() );
	                	if(currentCommunity != null && ret != null && !currentCommunity.equals(ret.getCOMMID()))
		            		return null;
	                    return ret;
	                }
	                catch( Exception e )
	                { }
	            }
	        }
	        
	        if(currentCommunity != null && agent != null && !currentCommunity.equals(agent.getCOMMID()))
        		return null;
	        return agent;
    }
    public void setAgent(UserAttributes attributes) {
//        agent = attributes;
    	try {
    		/*
    		String content = "========== DEBUG INFO ========== \n";
    		StringWriter sw = new StringWriter();
	        PrintWriter pw = new PrintWriter(sw);
	        new Throwable().fillInStackTrace().printStackTrace(pw);
	    	content += "Setting agent "+ attributes.getID() + " for search " + getID() + "\n";
	    	content += "Stack trace: " + sw.toString() + "\n";
    		content += "========== DEBUG INFO ========== \n ";
	    	System.out.println(content);
	    	*/
    	}catch(Exception ignored) {}
    	
        if( attributes != null  && attributes.getID() != null ) {
            agentId = attributes.getID().longValue();
        } else {
            agentId = null;
        }
    }
    
    public void setOwnerId( BigDecimal ownerId ) {
    	this.ownerId = ownerId;
    }
    public BigDecimal getOwnerId() {
    	return ownerId;
    }
    
    public void setBuyerId( BigDecimal buyerId ) {
    	this.buyerId = buyerId;
    }
    public BigDecimal getBuyerId() {
    	return buyerId;
    }
    
    public void setPropertyId( BigDecimal propertyId ) {
    	this.propertyId = propertyId;
    }
    public BigDecimal getPropertyId() {
    	return propertyId;
    }
    
  
	/**
	 * OrigSA
	 */
	public SearchAttributes getOrigSA() {
		return origSA;
	}
	public void setOrigSA(SearchAttributes sa) {
		origSA = sa;
	}

	/**
	 * SearchAttributes of Parent Search
	 * @return
	 */
	public SearchAttributes getParentSA() {
		return parentSA;
	}
	public void setParentSA(SearchAttributes sa) {
		parentSA = sa;
	}
	
	public long getFVSParentSearchID(){
		return FVSParentSearchID;
	}
	public void setFVSParentSearchID(long fVSParentSearchID){
		FVSParentSearchID = fVSParentSearchID;
	}
	/**
	 * SearchStatus
	 */
	public Vector<String[]> getSearchStatus() {
		return searchStatus;
	}
	public void updateSearchStatus(TSServer srv,ServerSearchesIterator ssi) {
		String format = "##.#";
		String stype = getServerType(srv);
		String serverStatus = ssi.getStatus();
		String formattedSearchTime = getFormattedSearchTime(srv, format);
		String status = serverStatus + " (" + formattedSearchTime + " s.)";
		boolean bUpd	= false;
		for(int i=0; i < searchStatus.size(); i++) {
			String[] st = (String[]) searchStatus.elementAt(i);
			if(st[0].equals(stype)) {
				if (srv.isRepeatDataSource()) {		//add current elapsed time to existent elapsed time 
					Matcher matcher = Pattern.compile("\\((\\d+(?:\\.\\d+)?) s\\.\\)$").matcher(st[1]);
					if (matcher.find()) {
						try {
							double oldTime = new Double(matcher.group(1));
							double newTime = new Double(formattedSearchTime);
							newTime += oldTime;
							DecimalFormat df = new DecimalFormat(format);
							formattedSearchTime = df.format(newTime);
							status = serverStatus + " (" + formattedSearchTime + " s.)";
							st[1] = status;
						} catch (NumberFormatException nfe) {}
					} else {
						st[1] = status;
					}
				} else {
				/*if(stype.equals("RO")) {
					st[1] = getROSearchTime(st[1],status);
				} else {*/
					st[1] = status;					
				//}
				}
				bUpd = true;
				break;
			}
		}		 
		if(!bUpd) {
			// pt automatic search pe FL Miami-Dade (B1977)
			/*  cauta si gasea documente pe AO, afisa in TSR si dupa cautarea pe TR,
			     la documentele de inclus in TSR nu se mai afisa ca s-au gasit si pe AO rezultate ;
			     
			     problema era ca suprascria "County Tax: x s." pe acelasi index unde era scris "Assessor Office: y s."
			*/
			//retin index-ul la care tre sa scriu detallile despre site-urile unde s-au gasit doc si timpul de cautare in variabila 
			//poz
			int poz = HashCountyToIndex.getServerIndex(getCommId(), srv.getDataSite());
			  
			String[] searchStatusValue = (String[])searchStatus.get(poz);
			
			while (!searchStatusValue[0].equals("") || !searchStatusValue[1].equals("")){
				poz++;
				searchStatusValue = (String[])searchStatus.get(poz);
			}
			searchStatus.setElementAt(new String[] {stype,status}, poz);
		}
		if (logger.isDebugEnabled())
			logger.debug("Search Status: " + stype + ":" + status);
		
        IndividualLogger.info( "Search Status: " + stype + ":" + status ,this.getID());
        if (ssi.getStatus() != null && ssi.getStatus().equals("not responding")){
            try {
            	SearchLogger.info("</div><br><div><span class=\"serverName\">"
            				+ ((TSServer)srv).getDataSite().getName()
            				+ "</span> <span class=\"error\"> not responding</span></div>", this.getSearchID());
            } catch (Exception e) {
            	e.printStackTrace();
			}
        }
	}
	
    public void updateSearchStatus(TSServer srv,String status) {
    	
    	if(status.contains(TIME_OUT) || status.contains(SKIPPED)){
    		SearchLogger.info("<span class='error'>Server <b>" + srv.getCurrentServerName() + "</b> " + status + "!</span><br/>", ID);
    	}
    	
        String stype    = getServerType(srv); 
        //String status   = isResponseError(res) ? "not responding" : "done";
   //     status          = status + " (" + getFormattedSearchTime(srv,"##.#") + " s.)";
        boolean bUpd    = false;
        for(int i=0; i < searchStatus.size(); i++) {
            String[] st = (String[]) searchStatus.elementAt(i);
            if(st[0].equals(stype)) {
            	if (srv.isRepeatDataSource()) {
            		if (status.contains(TIME_OUT)) {									//TIME_OUT has the highest priority
            			st[1] = status;
            		} else if (!status.contains(SKIPPED) && st[1].contains(SKIPPED)) {	//anything but (TIME_OUT or SKIPPED) 
            			st[1] = status;													//has the second highest priority 		
            		}																	//SKIPPED has the lowest priority
            	} else {
            		/*if(stype.equals("RO")) {
                    	st[1] = getROSearchTime(st[1],status);
                	} else {*/
                    st[1] = status;                 
                    //}
                }
            	bUpd = true;
                break;
            }
        }        
        if(!bUpd) {
            searchStatus.setElementAt(
            		new String[] {stype,status}, 
            		HashCountyToIndex.getServerIndex(getCommId(), srv.getDataSite()));
        }
        if (logger.isDebugEnabled())
            logger.debug("Search Status: " + stype + ":" + status);
    }
    
    
    /**
     * Sets the status of the automatic search on this site (defined with siteType)
     * @param siteType the id of the site
     * @param status the status to be set
     */
	public void updateSearchStatus(int siteType, String status) {
		String stype 	= getServerTypeFromCityChecked(siteType); 
		boolean bUpd	= false;
		for(int i=0; i < searchStatus.size(); i++) {
			String[] st = (String[]) searchStatus.elementAt(i);
			if(st[0].equals(stype)) {
				st[1] = status;					
				bUpd = true;
				break;
			}
		}		 
		if(!bUpd) {
			searchStatus.setElementAt(
					new String[] {stype,status}, 
					HashCountyToIndex.getServerIndex(Integer.parseInt(getSa().getCountyId()), siteType));			
		}
		if (logger.isDebugEnabled())
			logger.debug("Search Status: " + stype + ":" + status);
	}
	
	private String getFormattedSearchTime(TSServer srv,String fmt) {
		DecimalFormat df = new DecimalFormat(fmt);
		return 
			df.format((double)((srv.getStopTime() - srv.getStartTime()) / 1000.0));		
	}
	private String getServerType(TSServer srv) {
		return srv.getDataSite().getSiteDescription();		
	}
	
	public void resetSearchStatus() {
	    searchStatus = new Vector();
	    for (int i = 0; i < 100; i++) {
            searchStatus.add(new String[]{"",""});
        }
	}
	
	public void fillEmptySearchStatus() {
		if (searchStatus.size() == 0){
			resetSearchStatus();
		}
		if (getCommId() > -1 
				&& org.apache.commons.lang.StringUtils.isNotBlank(getStateAbv()) 
				&& org.apache.commons.lang.StringUtils.isNotBlank(getCountyName())){
			
			Vector<DataSite> allSitesForThis = HashCountyToIndex.getAllDataSites(getCommId(), getStateAbv(), getCountyName(), false, getProductId());
			StringBuilder psOnly = new StringBuilder();
			for (DataSite datasite : allSitesForThis){
				
				if (datasite.isEnableSite(getCommId())){
					if (datasite.isEnabledAutomatic(getProductId(), getCommId())){
						int poz = HashCountyToIndex.getServerIndex(getCommId(), datasite);
						String[] searchStatusValue = (String[]) searchStatus.get(poz);
						
						if (org.apache.commons.lang.StringUtils.isBlank(searchStatusValue[0]) 
								&& org.apache.commons.lang.StringUtils.isBlank(searchStatusValue[1])){
							if (searchStatus.size() > poz){
								//avoid duplication
								for (int i = 0; i < 100; i++) {
									String[] entry = searchStatus.get(i);
									if (org.apache.commons.lang.StringUtils.isNotBlank(entry[0])
											&& entry[0].equalsIgnoreCase(datasite.getSiteDescription())){
										searchStatus.setElementAt(new String[] {"", ""}, i);
									}
						        }
								searchStatus.setElementAt(new String[] {datasite.getSiteDescription(), "not searched in Automatic"}, poz);
							}
						}
					} else if (!checkIfPSAndNotDS(datasite)){
						if (psOnly.length() == 0){
							psOnly.append(datasite.getSiteTypeAbrev());
						} else{
							psOnly.append(", ").append(datasite.getSiteTypeAbrev());
						}
					}
				}
			}
			if (psOnly.length() > 0 && searchStatus.size() == 100){
				searchStatus.setElementAt(new String[] {"PS Only", psOnly.toString()}, 99);
			}
		}
	}
	
	public boolean checkIfPSAndNotDS(DataSite datasite){
		String tsServerClassName = datasite.getTsServerClassName();
		boolean hasNoModules = false;
		
		try {
			if (StringUtils.isEmpty(tsServerClassName) && ro.cst.tsearch.searchsites.client.Util.isSiteEnabled(datasite.getEnabled(getCommId()))) {
				hasNoModules = true;
			} else {
				int moduleCount = 0;

				long siteId = TSServersFactory.getSiteId(datasite.getStateAbrev(), datasite.getCountyName(), datasite.getSiteTypeAbrev());
				TSInterface tsServer = TSServersFactory.GetServerInstance(Long.valueOf(siteId).intValue(), getID());
				if (tsServer != null) {
					TSServerInfo defaultServerInfo = tsServer.getDefaultServerInfo();
					moduleCount = defaultServerInfo.getModuleCount();
				} else {
					moduleCount = 0;
				}
				hasNoModules = (moduleCount == 0);
			}
		} catch (Exception e) {}
		
		return hasNoModules;
	}
	
	public void setSearchStatus(Vector searchStatus) {
		this.searchStatus = searchStatus;		
	}
	///////////////////////////////////////////////////////////////////////////
	
	/**
	 * Used only by old order that did not have documentManager in order to create one
	 * @return
	 */
	@Deprecated
	public Map getImagesMap() {
		return imagesMap;
	}
	
	public Object getCrossRefSet(String instrKey) {
	    
	    return (Object) crossRefSetMap.get(instrKey);
	}
	
	public void putCrossRefSet(String instrKey, Object values) {
	    
	    crossRefSetMap.put(instrKey, values);
	}

	public String downloadImages(boolean allImages) throws Exception {
		String warnings = "";
		DocumentsManagerI manager = getDocManager();
		
		Map<Integer, TSInterface> serverInstancesUsed = new HashMap<Integer, TSInterface>();
		
		try {
			manager.getAccess();
			for (DocumentI doc : manager.getDocumentsList()) {

				String documentLog = "";
				if (StringUtils.isNotEmpty(doc.getInstno())) {
					documentLog = doc.getInstno();
				}
				if (doc.hasBookPage()) {
					if (StringUtils.isNotEmpty(documentLog)) {
						documentLog += doc.getBook() + "_" + doc.getPage();
					} else {
						documentLog = doc.getBook() + "_" + doc.getPage();
					}
				}

				try {
					if ((doc.needToSaveImage() 
							|| (allImages && doc.hasImage() && (!doc.getImage().isSaved()
							|| doc.getImage().getPath() == null 
							|| !new File(doc.getImage().getPath()).exists())))
							|| (isStarterStewartOrders() 
									&& ("PF".equalsIgnoreCase(doc.getDataSource()) 
											&& doc.hasImage() 
											&& (!doc.getImage().isSaved() || doc.getImage().getPath() == null || !new File(doc.getImage().getPath()).exists()))
										)
								) 
					{
						if (doc.isUploaded()) {
							if (DocumentDataRetreiver.downloadImageFromSsfIfNeedIt(this, doc.getImage())) {
								SearchLogger.info("<br/>Image <a href='" + doc.getImage().getSsfLink() + "'>" + doc.prettyPrint() + "</a> downloaded from SSF ", ID);
							} else {
								SearchLogger.info("<br/>Could not download the image from SSF server for " + doc.prettyPrint() + " with link ["+doc.getImage().getSsfLink() + "]", ID);
							}
						} else {
							
							if (DocumentDataRetreiver.downloadImageFromSsfIfNeedIt(this, doc.getImage())) {
								SearchLogger.info("<br/>Image <a href='" + doc.getImage().getSsfLink() + "'>" + doc.prettyPrint() + "</a> downloaded from SSF ", ID);
							} else if(TSServer.findImageOnSsf(this, doc, false, new ArrayList<DocumentInfoType>())
									&& DocumentDataRetreiver.downloadImageFromSsfIfNeedIt(this, doc.getImage())) {
								SearchLogger.info("<br/>Image <a href='" + doc.getImage().getSsfLink() + "'>" + doc.prettyPrint() + "</a> downloaded from SSF ", ID);
							} else {
								TSInterface tsi = serverInstancesUsed.get((int) doc.getSiteId());
								if(tsi == null) {
									tsi = TSServersFactory.GetServerInstance((int) doc.getSiteId(), ID);
									serverInstancesUsed.put((int) doc.getSiteId(), tsi);
								}
								DownloadImageResult res = tsi.downloadImage(doc);
								if (res == null || res.getStatus() != DownloadImageResult.Status.OK) {
									doc.getImage().setSaved(false);
									warnings += "Image of document with following instrument number was not successfully retrieved: " + documentLog + "\n";
									SearchLogger.info("<br/>Image of document with following instrument number was not successfully retrieved: " + documentLog + "\n", ID);
								} else {
									doc.getImage().setSaved(true);
								}
							} 
									
						}
					}
				} catch (Exception e) {
					e.printStackTrace();
					doc.getImage().setSaved(false);
					warnings += "Image of document with following instrument number was not successfully retrieved: " + documentLog + "\n";
					SearchLogger.info("<br/>Image of document with following instrument number was not successfully retrieved: " + documentLog + "\n", ID);
				}
			}
		} finally {
			manager.releaseAccess();
		}
		return warnings;
	}

	/**
	 * SearchStarted
	 */
	public void setSearchStarted(boolean b) {
		searchStarted = b;
	}
	public boolean getSearchStarted() {
		return searchStarted;
	}
	
	/**
	 * SearchState
	 */
	public long getSearchState() {
		return searchState;
	}
	public void setSearchState(long l) {
		searchState = l;
	}

	/**
	 * OCR docs
	 */
	public void addOCRDoc( String docLink ){
		if( ocrDocs == null ){
			ocrDocs = new Hashtable<String, String>();
		}
		
		ocrDocs.put( docLink , "e");
	}
	
	public Hashtable<String, String> getOCRDocs(){
		return ocrDocs;
	}
	/**
	 * DNDoc
	 */
	public void addDNDoc(String doc) {
		if (dnDocs == null)
			dnDocs = new Hashtable();
		dnDocs.put(doc, "e");
	}
	public Hashtable getDNDocs() {
		 return dnDocs;
	}
	
	/**
	 * PLDoc
	 */
	public void addPLDoc(String doc) {
		if (plDocs == null)
			plDocs = new Hashtable();
		plDocs.put(doc, "e");
	}
	public Hashtable getPLDocs() {
		 return plDocs;
	}
	
	/**
	 * RODoc
	 */
	public void addRODoc(String doc) {
		if (roDocs == null)
			roDocs = new Hashtable();
		roDocs.put(doc, "e");
	}
	
	public void addMiscDoc(String doc, String categ) 
	{
	    Hashtable docMap = null;
	    
	    docMap = getMiscDocs(categ);
	    
		if (docMap == null)
		    return;
		
		docMap.put(doc, "e");
	}
	
	public Hashtable getRODocs() {
		 return roDocs;
	}
	public Hashtable getMiscDocs(String categ) 
	{
	    if ("UCC".equals(categ))
	        return uccDocs;
	    
	    if ("CO".equals(categ))
			 return coDocs;
	    
	    if ("PC".equals(categ))
			 return pcDocs;
	    
	    if ("OR".equals(categ))
			 return orDocs;
        
        if( "PR".equals( categ ) )
            return prDocs;
	    
        if( "DT".equals( categ ) )
            return dtDocs;
        
        if( "TS".equals( categ ) ) {
        	if(tsDocs==null){
        		tsDocs = new Hashtable();
        	}
            return tsDocs;
        }
        
        if( "SF".equals( categ ) ) {
        	if(sfDocs==null){
        		sfDocs = new Hashtable();
        	}
            return sfDocs;
        }
        
        if( "LA".equals( categ ) )
            return laDocs;
        
        if( "AD".equals( categ ) )
            return laDocs;
        
        if( "RV".equals( categ ) ){
        	if(rvDocs==null){
        		rvDocs = new Hashtable();
        	}
            return rvDocs;
        }
        
        if( "PI".equals( categ ) ){
        	if(piDocs==null){
        		piDocs = new Hashtable();
        	}
            return piDocs;
        }
        
	    return null;
	}

	public void setUpdate(boolean update) {
		this.update = update;
	}
	public boolean isUpdate() {
		 return update;
	}
	
	public boolean equals(Object o) {
        if (o == null || !(o instanceof Search))
            return false;
        Search s=(Search)o;
        
        /*createCompData();
        s.createCompData();
        return registerDataComp.equals(s.registerDataComp);
        */
        
        try {
        	return s.getID() == this.getID();
        }catch(Exception e) {}
        
        return false;
    }
	    
	// STATIC
	public static String escapeHTML(String s) {
	    s=s.replaceAll("\\r?\\n", " ");
        s=s.replaceAll("<", "&lt;");
        s=s.replaceAll(">", "&gt;");
        s=s.replaceAll("&", "&amp;");
        s=s.replaceAll("'", "&apos;");
        s=s.replaceAll("\"", "&quot;");
        return s;
    }
	
	protected static String trimFileName(String s) {
        return s.substring(s.lastIndexOf(File.separatorChar, s.lastIndexOf(File.separatorChar)-1)+1);
    }
	
	/**
	 * @return Returns the tS_SEARCH_STATUS.
	 */
	public int getTS_SEARCH_STATUS() {
		return TS_SEARCH_STATUS;
	}
	/**
	 * @param ts_search_status The tS_SEARCH_STATUS to set.
	 */
	public void setTS_SEARCH_STATUS(int ts_search_status) {
		TS_SEARCH_STATUS = ts_search_status;
	}
	
	
	public synchronized Object clone() {
	    
	    try {
	        
		    Search newSearch = (Search) super.clone();
		    
		    newSearch.generatedTemp			= copyHashMap((HashMap)generatedTemp);
		    newSearch.changesInTSDIndexPageMap = copyHashMap((HashMap)changesInTSDIndexPageMap);
		    try {newSearch.sa				= (SearchAttributes) sa.clone(); } catch (Exception e) {}
		   
		    try {newSearch.msP1				= new String(msP1); } catch (Exception e) {};
		    try {newSearch.msP2				= new String(msP2); } catch (Exception e) {};
		    try {newSearch.msUserPath		= new String(msUserPath); } catch (Exception e) {};

		    try {newSearch.origSA 			= (SearchAttributes) origSA.clone(); } catch (Exception e) {};
		    try {newSearch.agent 			= (UserAttributes) agent.clone(); } catch (Exception e) {};
            try {newSearch.agentId          =  agentId.longValue() ; } catch (Exception e) {};
            try {newSearch.ownerId          =  new BigDecimal( ownerId.longValue() ); } catch (Exception e) {};
            try {newSearch.buyerId          =  new BigDecimal( buyerId.longValue() ); } catch (Exception e) {};
            try {newSearch.propertyId       =  new BigDecimal( propertyId.longValue() ); } catch (Exception e) {};
		    
		    try {newSearch.imagesMap 		= copyImages((Hashtable) imagesMap); } catch (Exception e) {}
		   
		    try {newSearch.dnDocs 			= copyLinks(dnDocs); } catch (Exception e) {}
		    try {newSearch.ocrDocs			= copyLinks(ocrDocs);} catch ( Exception e ) {}
		    try {newSearch.plDocs 			= copyLinks(plDocs); } catch (Exception e) {}
		    try {newSearch.roDocs 		 	= copyLinks(roDocs); } catch (Exception e) {}
		    try {newSearch.fileProcessed 	= copyLinks(fileProcessed); } catch (Exception e) {}
		    try {newSearch.visitedLinks 	= copyLinks(visitedLinks); } catch (Exception e) {}
		   
		    try {newSearch.chapters 		= copyHashMap((HashMap)chapters); } catch (Exception e) {}
		    try {newSearch.inMemoryDocs 	= copyHashMap((HashMap)getInMemoryDocs()); } catch (Exception e) {}
		    try {newSearch.TSROrderDate 	= new Date(TSROrderDate.getTime()); } catch (Exception e) {}
		    
		    try {newSearch.registerData 	= copyRegistredData((TreeMap) registerData); } catch (Exception e) {}
		    
		    try {newSearch.removedInstr 	= copyRmvInst((HashSet)removedInstr); } catch (Exception e) {}
			
			try {newSearch.patriotSearchName= new String(patriotSearchName); } catch (Exception e) {}
			try {newSearch.patriotKeyNumber	= new String(patriotKeyNumber); } catch (Exception e) {}
			try {newSearch.TSDFileName		= new String(TSDFileName); } catch (Exception e) {}
			
			try {newSearch.existingImages   = (Set<String>)((HashSet<String>)existingImages).clone(); } catch (Exception e) {}
			try {newSearch.savedInstruments = (Set<String>)((HashSet<String>)savedInstruments).clone(); } catch (Exception e) {}
			
			newSearch.update 				= update;
			
			newSearch.searchDirSuffix 		= "";
			
			newSearch.ID 	    			= ID;
			newSearch.searchID    			= searchID;
			
			newSearch.searchCycle 			= searchCycle;
			
			newSearch.disposeTime			= 0;
			
		    /*
		     * imageProcessed >>> No longer used
		     * */
		    try {newSearch.imageProcessed 	= (Hashtable) imageProcessed.clone(); } catch (Exception e) {}
		    
		    try {
		        newSearch.visitedDocs 		= new ArrayList();
		        for (int i = 0; i < visitedDocs.size(); i++) {
		            newSearch.visitedDocs.add( (String) visitedDocs.get(i));
		        }
		    } catch (Exception e) {}
		    
		    newSearch.tsriLink = "";
		    
		    return newSearch;
		    
		} catch (CloneNotSupportedException cnse) {
	        throw new InternalError(); 
	    }	   
    }
	
	public Search cloneSearch() {
		//clone current search
    	Search localSearch = (Search) clone();
    	
    	// setam noile id-uri pentru search-ul clonat
    	localSearch.setID(DBManager.getNextId(DBConstants.TABLE_SEARCH));
        localSearch.setSearchID(localSearch.getID());
        
        localSearch.setTSROrderDate(new Date());
    	localSearch.setStartDate(DBManager.getStartDate(localSearch.getID()));

        localSearch.resetImagePath();
        localSearch.getImagesCount().clear();
        
        HashMap<String, String> generatedTemp = getGeneratedTemp();
		for (String generatedTemplate : generatedTemp.values()) {
    		try{	
				TemplateBuilder.cleanJustLink(generatedTemplate, this);
    		} catch (Exception e) {
				logger.error("Something happened while saving saved templates for searchId = " 
						+ getID() + " generatedTemplate = " + generatedTemplate, e);
			}
		}	

        // copiem directorul search-ului curent
        FileCopy.copy(new File(getSearchDir()), new File(localSearch.getSearchDir()));

        DocumentsManagerI localDocManager = localSearch.getDocManager();

        // adaug referinte la documentele din TSRIExtended -> update TABLE_MODULES;
        cloneTSRIExtended(this.searchID, localSearch);
        
        localSearch.copyLogInSambaFrom(getID());
        
    	try {
    		localDocManager.getAccess();
    		for (DocumentI document : localDocManager.getDocumentsList()) {
    			try {
	    			String oldDocumentId = document.getId();
	    			
					document.setId( 
							DocumentsManager.generateDocumentUniqueId( localSearch.getID(), 
							document.getInstrument()));
					
					if (!document.isUploaded() && !document.isFake()){
						String oldSearchID = Long.toString(getID());
						String currentSearchID = Long.toString(localSearch.getID());
						String contentDocIndex = DBManager.getDocumentIndex(document.getIndexId());
						if (contentDocIndex != null) {
							if (contentDocIndex.indexOf("searchId=" + oldSearchID) != -1)
								contentDocIndex = contentDocIndex.replaceAll("(?is)(searchId=)" + oldSearchID, "$1" + currentSearchID);
//							document.setIndexId(DBManager.addDocumentIndex(	DBManager.getDocumentIndex(document.getIndexId()), localSearch.getID()));
							document.setIndexId(DBManager.addDocumentIndex(contentDocIndex, localSearch));
						} else {
							document.setIndexId(DBManager.addDocumentIndex("", localSearch));
						}
						
						if(document instanceof SSFPriorFileDocument) {
							SSFPriorFileDocument sfDocument = (SSFPriorFileDocument)document;
							String contentSfDocIndex = DBManager.getSfDocumentIndex(sfDocument.getSsfIdexId());
							if(contentSfDocIndex != null) {
								if (contentSfDocIndex.indexOf("searchId=" + oldSearchID) != -1) {
									contentSfDocIndex = contentSfDocIndex.replaceAll("(?is)(searchId=)" + oldSearchID, "$1" + currentSearchID);
								}
								sfDocument.setSsfIdexId(DBManager.addSfDocumentIndex(contentSfDocIndex, localSearch));
							} else {
								sfDocument.setSsfIdexId(DBManager.addSfDocumentIndex("", localSearch));
							}
						}
					}
					ImageI image = document.getImage();
					
					if(image != null) {
						image.setViewCount(new AtomicInteger());	//this image was never viewed
						//if(image.isSaved()) {
//							String oldImagePath = image.getPath();
		        			String oldImageFileName = image.getFileName();
		        			image.setFileName( oldImageFileName.replace( oldDocumentId, document.getId()));
		        			image.setPath(localSearch.getImagePath() + image.getFileName());
		        			/*
							org.apache.commons.io.FileUtils.copyFile(new File(oldImagePath), new File(image.getPath()));
							if (document instanceof AssessorDocumentI || document instanceof TaxDocumentI) {
								if (ServerConfig.isFileReplicationEnabled()) {
									FileContent.replicateImage(image, localSearch.getID(), false);
								}
							}
							*/
						//} 
						
		        		OcrFileArchiveSaver ocrFileArchiveSaverSource = new OcrFileArchiveSaver(this);
		        		OcrFileArchiveSaver ocrFileArchiveSaverDestination = new OcrFileArchiveSaver(localSearch);
		        		
		        		List<File> ocrSourceFiles = ocrFileArchiveSaverSource.getLocalFiles(true, oldDocumentId);
		        		String ocrFileArchiveFolderPath = OcrFileArchiveSaver.getOcrFileLocalFolder(localSearch);
		        		for (File file : ocrSourceFiles) {
		        			String fileName = file.getName();
		        			File copiedLocalFile = new File(ocrFileArchiveFolderPath + fileName.replace(oldDocumentId, document.getId()));
		        			if(fileName.endsWith("." + OcrFileArchiveSaver.SMART_VIEWER_JS_EXT) ) {
		        				String fileContent = org.apache.commons.io.FileUtils.readFileToString(file);
								fileContent = fileContent.replaceAll(oldDocumentId, document.getId());
//								fileContent = fileContent.replaceAll("searchId=" + this.searchID, "searchId=" + localSearch.getID());
								org.apache.commons.io.FileUtils.writeStringToFile(copiedLocalFile, fileContent);
		        			} else {
		        				org.apache.commons.io.FileUtils.copyFile(file, copiedLocalFile);
		        			}
		        			ocrFileArchiveSaverDestination.addLocalFile(copiedLocalFile);
		        			
						}
		        		
		        		new Thread(
		        				ocrFileArchiveSaverDestination, 
		        				"OcrFileArchiveSaver on clone " + this.getID() + " to " + localSearch.getID() + " - document " + document.getNiceFullFileName())
		        			.start();
					}
					
    			} catch (Exception e) {
					logger.error("Error cloning one document for search " + searchID, e);
				}
			}
    		
    	} catch (Exception e) {
			e.printStackTrace();
		} finally {
			localDocManager.releaseAccess();
		}
		
		// inlocuim id-ul search-ului initial cu noul id in toate link-urile
        try {
			localSearch = Search.replaceAllSearchId(localSearch, getSearchID());
		} catch (SaveSearchException e) {
			logger.error("Error while replacing allSearchIds from xml....",e );
		}
        
        localSearch.replaceAllSearchFromOrder(localSearch.getID());
		localSearch.getSa().setAtribute(SearchAttributes.ABSTRACTOR_FILENO, "");
		localSearch.getSa().setAtribute(SearchAttributes.ORDERBY_FILENO, "");
		localSearch.setUpdate(false);
		localSearch.setWasOpened(true); 
		localSearch.getOpenCount().incrementAndGet();
		localSearch.getSa().setDefaultFromDate();
		localSearch.getSa().setDefaultToDate();
		localSearch.setPropertyId(null);
		localSearch.setImageTransactionId(-1);
		localSearch.setNotes(getNotes(searchID));
		
		return localSearch;
	}
	
	public void copyLogInSambaFrom(long searchIdSource) {
		AsynchSearchLogSaverThread.copyLogInSamba(searchIdSource, getID());
		
		byte orderFileAsByteArray[] = DBManager.getSearchOrderLogs(searchIdSource, FileServlet.VIEW_ORDER, false);
		if(orderFileAsByteArray != null && orderFileAsByteArray.length != 0 ) {
			String content = new String(orderFileAsByteArray);
			content = content.replaceAll(
					String.valueOf(searchIdSource), 
					String.valueOf(getID()));
			if (ServerConfig.isEnableOrderOldField()) {
				try {
					FileOutputStream orderFile = new FileOutputStream(getSearchDir() + "orderFile.html");
					orderFile.write(content.getBytes());
					orderFile.flush();
					orderFile.close();
				} catch (Exception e) {
					logger.error("Error while moving OrderFile for search " + getID(), e);
					e.printStackTrace();
				}
			}
			String fullPath = SharedDriveUtils.getSharedLogFolderForSearch(getID());
			fullPath += "orderFile.html";
			try {
				org.apache.commons.io.FileUtils.write(new File(fullPath), content, true);
				
			} catch (Exception e) {
				System.err.println("Write to samba failed for path " + fullPath);
				e.printStackTrace();
				Log.sendExceptionViaEmail(
						MailConfig.getMailLoggerToEmailAddress(), 
						"Order File on Samba failed", 
						e, 
						"SearchId used: " + getID() + ", path used: " + fullPath);
			}
		}
		
	}

	private boolean cloneTSRIExtended(long searchID, Search localSearch) {
		// copy modules into table DBConstants.TABLE_MODULES
		// create references to the documents
		// DBConstants.TABLE_MODULE_TO_DOCUMENT
		String GET_MODULES_FOR_SEARCH = "SELECT * FROM "
				+ DBConstants.TABLE_MODULES + " WHERE "
				+ DBConstants.FIELD_MODULE_SEARCH_ID + " = ?";

		String GET_DOCUMENTS_FOR_MODULE = "SELECT * FROM "
				+ DBConstants.TABLE_MODULE_TO_DOCUMENT + " WHERE "
				+ DBConstants.FIELD_MODULE_TO_DOCUMENT_MODULE_ID + " = ?";

		String INSERT_MODULE_TO_DOCUMENTS = "INSERT INTO "
				+ DBConstants.TABLE_MODULE_TO_DOCUMENT + " VALUES (?,?)";

		List<Map<String, Object>> modules = DBManager.getSimpleTemplate()
				.queryForList(GET_MODULES_FOR_SEARCH, searchID);

		for (Map<String, Object> module : modules) {
			ro.cst.tsearch.database.rowmapper.ModuleMapper currentModule = new ro.cst.tsearch.database.rowmapper.ModuleMapper();
			// copy module in DBConstants.TABLE_MODULES
			if (module.get(DBConstants.FIELD_MODULE_ID) != null
					&& module.get(DBConstants.FIELD_MODULE_SERVER_ID) != null
					&& module.get(DBConstants.FIELD_MODULE_SEARCH_MODULE_ID) != null
					&& module.get(DBConstants.FIELD_MODULE_DESCRIPTION) != null
					&& module.get(DBConstants.FIELD_MODULE_SEARCH_ID) != null) {
				currentModule.setServerId(((Long) module
						.get(DBConstants.FIELD_MODULE_SERVER_ID)).intValue());
				currentModule.setSearchModuleId(((Long) module
						.get(DBConstants.FIELD_MODULE_SEARCH_MODULE_ID))
						.intValue());
				currentModule.setDescription((String) module
						.get(DBConstants.FIELD_MODULE_DESCRIPTION));
				currentModule.setSearchId(localSearch.getSearchID());
				if (!currentModule
						.saveInDatabase(DBManager.getSimpleTemplate()))
					return false;

				List<Map<String, Object>> docs = DBManager.getSimpleTemplate()
						.queryForList(GET_DOCUMENTS_FOR_MODULE,
								module.get(DBConstants.FIELD_MODULE_ID));

				// insert reference to the new module documents
				for (Map<String, Object> doc : docs) {
					if (doc.get(DBConstants.FIELD_MODULE_TO_DOCUMENT_MODULE_ID) != null
							&& doc.get(DBConstants.FIELD_MODULE_TO_DOCUMENT_DOCUMENT_ID) != null) {
						DBManager
								.getSimpleTemplate()
								.update(INSERT_MODULE_TO_DOCUMENTS,
										currentModule.getModuleId(),
										doc.get(DBConstants.FIELD_MODULE_TO_DOCUMENT_DOCUMENT_ID));
					}
				}
			}
		}

		return true;
	}
	
	public Search createSearchForFVSUpdate(){
		
		UserAttributes userA = null;
		try {
			userA = UserUtils.getUserFromId(this.agentId);
		} catch (BaseException e) {
			e.printStackTrace();
		}
		User user = MailOrder.getUser(userA.getLOGIN());
		
		Search localSearch = new Search(user, DBManager.getNextId(DBConstants.TABLE_SEARCH));
		localSearch.setAgent(userA);
		localSearch.setSearchID(localSearch.getID());
		long searchId = localSearch.getSearchID();
		
		int comm_id = this.getCommId();
		try {
			CommunityAttributes ca = CommunityUtils.getCommunityFromId(comm_id);
			InstanceManager.getManager().getCurrentInstance(localSearch.getID()).setCurrentCommunity(ca);
		} catch (Exception e) {
			e.printStackTrace();
		}
		
		SearchAttributes sat = localSearch.getSa();
		
		sat.setCommId((int)comm_id);
		sat.setAtribute(SearchAttributes.ORDERBY_FILENO, this.getAgentFileNo());
		sat.setAtribute(SearchAttributes.ABSTRACTOR_FILENO, this.getAbstractorFileNo());
		sat.setAtribute(SearchAttributes.P_STATE, this.getSa().getAtribute(SearchAttributes.P_STATE));
		sat.setAtribute(SearchAttributes.P_COUNTY, this.getCountyId());
		sat.setAtribute(SearchAttributes.SEARCH_PRODUCT, Integer.toString(Products.FVS_PRODUCT));
		sat.setAtribute(SearchAttributes.IS_UPDATED_ONCE, "false");
		sat.setAtribute(SearchAttributes.FVS_UPDATE, "true");
		localSearch.setFVSParentSearchID(this.getSearchID());
		localSearch.constructSearchDirs();
		
		SearchManager.setSearch(localSearch, user);
		
		InstanceManager.getManager().getCurrentInstance(searchId).setCrtSearchContext(localSearch);
		InstanceManager.getManager().getCurrentInstance(searchId).setCurrentUser(userA);
		
		try {
			ValidateInputs.checkIfUpdate(localSearch, TSOpCode.FVS_UPDATE, null);
		} catch (Exception e) {
			e.printStackTrace();
		}
		try {
			MailTemplateUtils.fillAndSaveSearchOrder(localSearch, sat, user);
		} catch (FileNotFoundException e2) {
			e2.printStackTrace();
		} catch (IOException e2) {
			e2.printStackTrace();
		}
		
		localSearch.setStartIntervalWorkDate(new Date());
		
		try {			
			// save empty search
			DBManager.saveCurrentSearchLockedUnlocked(user, localSearch, Search.SEARCH_TSR_NOT_CREATED, null, false); // pass null request
			
		} catch (SaveSearchException e1) {
			logger.error("Error saving the searched received through Search.createSearchForFVSUpdate()!");
			e1.printStackTrace();
		}
       	
		return localSearch;
	}
	
	private static final int MAX_RETRY = 5;
	@SuppressWarnings("unused")
	transient private static final XStream xstream = new XStream();
	
	public static void sendWarningMailMessage( Search search, String warning ){
		//notify technical support
		UserAttributes currentUser = InstanceManager.getManager().getCurrentInstance( search.getSearchID() ).getCurrentUser();
		           
        try {
        	
        	String htmlBody = MailTemplateUtils.fillAndSaveSearchOrder(search, search.getOrigSA(), currentUser);
            
			Properties props = System.getProperties();
			props.put("mail.smtp.host", MailConfig.getMailSmtpHost());
			javax.mail.Session session = javax.mail.Session.getDefaultInstance(props,null);
			
			MimeMessage msg = new MimeMessage(session);
			
			InternetAddress fromAddress = null;
			try {
			    fromAddress = new InternetAddress(currentUser.getEMAIL());
			} catch (Exception ex) {
			    fromAddress = new InternetAddress(MailConfig.getMailFrom());
			}
			msg.setFrom(fromAddress);
			
			msg.setSubject(search.getSa().getAtribute("ORDERBY_FILENO") + warning + currentUser.getUserFullName() + "!");
			msg.setRecipients(javax.mail.Message.RecipientType.TO, 
							InternetAddress.parse(MailConfig.getSupportEmailAddress()));
			msg.setRecipients(javax.mail.Message.RecipientType.CC, 
					InternetAddress.parse(MailConfig.getExceptionEmail()));
			msg.setContent(htmlBody, "text/html");
			Transport.send(msg);
			
		} catch(Exception e){
			e.printStackTrace();
		}
	}

	public static void signalSizeExceeded(Search search) throws SaveSearchException{
		//send email notification
		sendWarningMailMessage( search, " - Search exceeded maximum size ordered by agent " );
		
		//throw a SaveSearchException
		throw new SaveSearchException( "Save search error! Search XML file bigger than 40MB!\\nTechnical support was automatically notified, you will be contacted shortly." );		
	}
		
	public static void saveSearch(Search search) throws SaveSearchException {
		
		if(search.isFakeSearch()) {
			throw new SaveSearchException("Cannot save a fake search: " + search.getID());
		}
				
		String fileName = (search.getSearchDir() + File.separator + "__search.xml").replaceAll("//", "/");
		String fileNameTemp = (search.getSearchDir() + File.separator + "__search__temp.xml").replaceAll("//", "/");
		String backupFileName = (search.getSearchDir() + File.separator + "__search__backup.xml").replaceAll("//", "/");
		
		File oldFile = new File(fileName);
		File backupFile = new File(backupFileName);
		if(backupFile.exists()) {
			backupFile.delete();
		}
		boolean isBackupDone = false;
		if(oldFile.exists()) {
			if( !oldFile.renameTo(backupFile)) {
				try {
					org.apache.commons.io.FileUtils.copyFile(oldFile, backupFile);
					isBackupDone = true;
				} catch (Exception e) {
					e.printStackTrace();
					if(FileUtils.copy(fileName, backupFileName)) {
						if (oldFile.length() != backupFile.length()) {
							try {
								EmailClient email = new EmailClient();
								email.addTo(MailConfig.getExceptionEmail());
								email.setSubject("Problem creating backup for " + search.getID() + " on " + URLMaping.INSTANCE_DIR);
								email.addContent("Problem creating backup for " + search.getID() + " on " + URLMaping.INSTANCE_DIR + "\n\n" + 
										"old __search.xml.length() = " + oldFile.length() + 
										" and backupFile.length() = " + backupFile.length()+ "\n" +
										"All three possibilies failed!!! " + "\n\n" + 
										e.getMessage() + "\n" + 
										ServerResponseException.getExceptionStackTrace( e, "\n" ));
								email.sendAsynchronous();
							} catch (Exception eMail) {
								eMail.printStackTrace();
							}
						} else {
							isBackupDone = true;
						}
	
					} else {
						try {
							EmailClient email = new EmailClient();
							email.addTo(MailConfig.getExceptionEmail());
							email.setSubject("Problem creating backup for " + search.getID() + " on " + URLMaping.INSTANCE_DIR);
							email.addContent("Problem creating backup for " + search.getID() + " on " + URLMaping.INSTANCE_DIR + "\n\n" + 
									"All three possibilies failed!!! " + "\n\n" + 
									e.getMessage() + "\n" + 
									ServerResponseException.getExceptionStackTrace( e, "\n" ));
							//email.sendAsynchronous();
						} catch (Exception eMail) {
							eMail.printStackTrace();
						}
					}
				}
			} else {
				isBackupDone = true;
			}
		
		}
		boolean sizeExceeded = false;
		if(!oldFile.exists() || isBackupDone) {	//do the new strategy
			
			for(int i=0; i<MAX_RETRY && !sizeExceeded; i++){
				if(oldFile.exists())
					oldFile.delete();
				try {	    		
	    			// serialize and write the search to disk
		    		synchronized(search){
		    			search.setSavedVersion();
		    			
		    			CompactWriter cw = new CompactWriter(new BufferedWriter(new FileWriter(oldFile)));
		    			
		    			//52642
		    			XStreamManager.getInstance().marshalSearch(search, cw);
		    			try {
		    				cw.flush();
		    			} catch (Exception e) {}
		    			try {
		    				cw.close();	    
		    			} catch (Exception e) {}
		    			
	
		    		}
	    			//check if xml bigger than 40MB	    			 
	    			if(oldFile.length() > MAX_XML_FILE_SIZE){
	    				sizeExceeded = true;
		    			oldFile.delete();
		    			SearchLogger.info("Automatic search saving failed because size exceeded the maximum limit! The search will be restored to a previous state.", search.getID());
		    			if(backupFile.exists() && !backupFile.renameTo(oldFile)) {
		    				try {
		    					org.apache.commons.io.FileUtils.copyFile(backupFile, oldFile);
		    				} catch (Exception e) {
		    					if(!FileUtils.copy(backupFileName, fileName)) {
		    						if (oldFile.length() != backupFile.length()) {
		    							try {
		    								EmailClient email = new EmailClient();
		    								email.addTo(MailConfig.getExceptionEmail());
		    								email.setSubject("Problem restoring backup for " + search.getID() + " on " + URLMaping.INSTANCE_DIR);
		    								email.addContent("Problem restoring backup for " + search.getID() + " on " + URLMaping.INSTANCE_DIR + "\n\n" + 
		    										"new __search.xml.length() = " + oldFile.length() + 
		    										" and backupFile.length() = " + backupFile.length()+ "\n" +
		    										"All three possibilies failed!!! " + "\n\n" + 
		    										e.getMessage() + "\n" + 
		    										ServerResponseException.getExceptionStackTrace( e, "\n" ));
		    								email.sendAsynchronous();
		    							} catch (Exception eMail) {
		    								eMail.printStackTrace();
		    							}
		    						}
		    					} else {
		    						try {
	    								EmailClient email = new EmailClient();
	    								email.addTo(MailConfig.getExceptionEmail());
	    								email.setSubject("Problem restoring backup for " + search.getID() + " on " + URLMaping.INSTANCE_DIR);
	    								email.addContent("Problem restoring backup for " + search.getID() + " on " + URLMaping.INSTANCE_DIR + "\n\n" + 
	    										"All three possibilies failed!!! " + "\n\n" + 
	    										e.getMessage() + "\n" + 
	    										ServerResponseException.getExceptionStackTrace( e, "\n" ));
	    								email.sendAsynchronous();
	    							} catch (Exception eMail) {
	    								eMail.printStackTrace();
	    							}
		    					}
		    				}
		    			}
		    			signalSizeExceeded(search);	    		
	    			} else {    				
	    				return ;
	    			}
				} catch (Throwable e) {
					e.printStackTrace();
					try {
						EmailClient email = new EmailClient();
						email.addTo(MailConfig.getExceptionEmail());
						email.setSubject("Problem saving __search.xml for " + search.getID() + "(try " + i + ") with backup done on " + URLMaping.INSTANCE_DIR);
						email.addContent("Problem saving __search.xml for " + search.getID() + "(try " + i + ") with backup done on " + URLMaping.INSTANCE_DIR + "\n\n" + 
								e.getMessage() + "\n\n" +
								ServerResponseException.getExceptionStackTrace( e, "\n" ));
						email.sendAsynchronous();
					} catch (Exception eMail) {
						eMail.printStackTrace();
					}
				}
			}
		} 
		
		if(sizeExceeded) {
			try {
				EmailClient email = new EmailClient();
				email.addTo(MailConfig.getExceptionEmail());
				email.setSubject("Problem saving __search.xml for " + search.getID() + " on " + URLMaping.INSTANCE_DIR + (sizeExceeded?" Size Exceeded":""));
				email.addContent("Problem saving __search.xml for " + search.getID() + " on " + URLMaping.INSTANCE_DIR + (sizeExceeded?" Size Exceeded":"") + "\n\n");
				email.sendAsynchronous();
			} catch (Exception eMail) {
				eMail.printStackTrace();
			}
			return;
		}
		
		//run the old code as a last solution
		for(int i=0; i<MAX_RETRY && !sizeExceeded; i++){
	    	try {	    		
    			// serialize and write the search to disk
	    		File file = new File(fileNameTemp); 
	    		if(file.exists())
	    			file.delete();
	    		synchronized(search){
	    			search.setSavedVersion();
	    			CompactWriter cw = new CompactWriter(new BufferedWriter(new FileWriter(fileNameTemp)));
	    			
	    			//52642
	    			XStreamManager.getInstance().marshalSearch(search, cw);
	    			
	    			try {
	    				cw.flush();
	    			} catch (Exception e) {}
	    			try {
	    				cw.close();	    
	    			} catch (Exception e) {}
	    				    		}
    			//check if xml bigger than 40MB
    			
    			if(file.length() > MAX_XML_FILE_SIZE){		    		
	    			sizeExceeded = true;
    				file.delete();
    				SearchLogger.info("<div>Automatic search saving failed because size exceeded the maximum limit! The search will be restored to a previous state.</div>", search.getID());
	    			signalSizeExceeded(search);	    		
    			} else {
    				File oldFileSearch = new File(fileName);
    				if(oldFileSearch.exists())
    					oldFileSearch.delete();
    				if(file.renameTo(oldFileSearch))
    					return ;
    				
    			}
			} catch (Throwable e) {
				e.printStackTrace();
				try {
					EmailClient email = new EmailClient();
					email.addTo(MailConfig.getExceptionEmail());
					email.setSubject("Problem saving __search.xml for " + search.getID() + "(try " + i + ") on " + URLMaping.INSTANCE_DIR);
					email.addContent("Problem saving __search.xml for " + search.getID() + "(try " + i + ") on " + URLMaping.INSTANCE_DIR + "\n\n" + 
							e.getMessage() + "\n\n" +
							ServerResponseException.getExceptionStackTrace( e, "\n" ));
					email.sendAsynchronous();
				} catch (Exception eMail) {
					eMail.printStackTrace();
				}
			}
		}
		try {
			EmailClient email = new EmailClient();
			email.addTo(MailConfig.getExceptionEmail());
			email.setSubject("Problem saving __search.xml for " + search.getID() + " on " + URLMaping.INSTANCE_DIR + (sizeExceeded?" Size Exceeded":""));
			email.addContent("Problem saving __search.xml for " + search.getID() + " on " + URLMaping.INSTANCE_DIR + (sizeExceeded?" Size Exceeded":"") + "\n\n");
			email.sendAsynchronous();
		} catch (Exception eMail) {
			eMail.printStackTrace();
		}
		System.err.println("Problem saving __search.xml for " + search.getID());
		System.err.println("Problem saving __search.xml for " + search.getID());
		System.err.println("Problem saving __search.xml for " + search.getID());
		logger.error("Problem saving __search.xml for " + search.getID());
		logger.error("Problem saving __search.xml for " + search.getID());
		logger.error("Problem saving __search.xml for " + search.getID());
	}
	
	public static void saveSearchToPath(Search search, String pathToSave, String originalSearchFilename) throws SaveSearchException {
	
		try {
			long searchId = search.getID();
	   		synchronized( search )
            {            	
    			SearchAttributes sa = search.sa;
    			
    			String filenameToSave = "";
    			if( originalSearchFilename == null )
    			{
    			    sa.setAbstractorFileName(search);
    			    filenameToSave = pathToSave + File.separator + sa.getAbstractorFileName() + ".xml";
    			}
    			else
    			{
    			    filenameToSave = pathToSave + File.separator + originalSearchFilename;
    			}
    			
    			String shortName = Products.getProductShortNameStringLength3(searchId);
    			filenameToSave = filenameToSave.replaceAll( "TSR", "TC" );
    			filenameToSave = filenameToSave.replaceFirst( shortName, "TC" );
    			
    			logger.info( "Saving " + filenameToSave );
    			
    			// write serialized form
                Writer writer = new BufferedWriter(new FileWriter(filenameToSave));
    			CompactWriter cw = new CompactWriter(writer);
    			XStreamManager.getInstance().marshalSearch(search, cw);
    			cw.close();
    			
    			// check if the saved XML > 40 MB : delete, send email, throw exception
    			File file = new File(filenameToSave);
    			if(file.length() > MAX_XML_FILE_SIZE){
    				file.delete();
    				signalSizeExceeded(search);    			
    			}
			}

		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch(IOException e){
			e.printStackTrace();
		}
	}
	
	public static Search replaceAllSearchId(Search search, long oldSearchId) throws SaveSearchException{

    	try {
			
			synchronized(search) {
			
				// serialize the search 
    			StringWriter writer = new StringWriter();
    			CompactWriter cw = new CompactWriter(writer);
    			XStreamManager.getInstance().marshalSearch(search, cw);
    			cw.close();
				
    			// check if the serialized form 
				if(writer.getBuffer().length() < MAX_XML_FILE_SIZE ){	    			
					String xml = writer.toString();
	    			xml = xml.replaceAll(String.valueOf(oldSearchId), String.valueOf(search.getSearchID()));	    			
	    			search = (Search) XStreamManager.getInstance().fromXML(xml);
				} else {
					signalSizeExceeded(search);
				}
			}
			
		} catch (Exception e) {
			e.printStackTrace();
		} 
        
		return search;
	}
	
	private synchronized HashSet copyRmvInst(HashSet removedInstr) {
		HashSet newRmvInst = new HashSet();
        for (Iterator iter = removedInstr.iterator(); iter.hasNext();) {
            Map.Entry el = (Map.Entry) iter.next();
            newRmvInst.add(el.getValue());
        }
        return newRmvInst;
	}
	
	private TreeMap copyRegistredData (TreeMap registredData) {
		TreeMap newRegData = new TreeMap();
		
		for (Iterator iter = registredData.entrySet().iterator(); iter.hasNext();) {
		    
			Map.Entry el = (Map.Entry) iter.next();

			registredData.put(el.getKey().toString(), 
			        ((TSServer)el.getValue()).clone());
	    }
	    
	    return newRegData;
	}
	
	private Hashtable copyImages(Hashtable imagesMap) {
	    
	    Hashtable newImagesMap = new Hashtable();
	    
        synchronized( imagesMap )
        {
    	    for (Iterator iter = imagesMap.entrySet().iterator(); iter.hasNext();) {
    		    
    			Map.Entry el = (Map.Entry) iter.next();
    			Object imagesValue = el.getValue();
    			
    			if (imagesValue instanceof TSServer) {
    				newImagesMap.put(((ImageLinkInPage) el.getKey()).clone(), 
        			        ((TSServer)imagesValue).getServerID());
				} else if (imagesValue instanceof Integer) {
					newImagesMap.put(((ImageLinkInPage) el.getKey()).clone(), 
	    			        ((Integer)imagesValue));
				} else {
					logger.error("Image lost as being copied!!!");
					System.err.println("Image lost as being copied!!!");
				}
    	    }
        }
	    return newImagesMap;
	}
	
	private Hashtable copyLinks(Hashtable input) {
	    
	    Hashtable newLinks = null;
	    
	    if (input != null) {
	        
	        newLinks = new Hashtable();
	        
		    Enumeration keys = input.keys();
		    while (keys.hasMoreElements()) {
		        
		        try {
		            
			        String key = (String) keys.nextElement();
			        String value = (String) input.get(key);
			        
			        newLinks.put(key, value);
			        
		        } catch (Exception e) {
		            e.printStackTrace();
		        }
		    }
	    }
	    
	    return newLinks;
	}
	
	public static HashMap copyHashMap(HashMap input) {
	    
	    HashMap output = null;
	    
	    if (input != null) {
	    
	        output = new HashMap();
	        
		    for (Iterator iter = input.entrySet().iterator(); iter.hasNext();) {
			    
				Map.Entry el = (Map.Entry) iter.next();
				
				String key = (String) el.getKey();
				Object value=null;
				try{
					value = (String) el.getValue();
					output.put(key, value != null ? new String((String)value) : null);
				}
				catch(Exception e){
					Boolean val =(Boolean)el.getValue();
					output.put(key, value != null ? val : null);
				}
				
				
		    }
	    }
	    
	    return output;
	    
	}
	
	public String getPatriotSearchName() {
		return patriotSearchName;
	}
	public void setPatriotSearchName(String string) {
		patriotSearchName = string;
	}
	public String getPatriotKeyNumber() {
		return patriotKeyNumber;
	}
	public void setPatriotKeyNumber(String string) {
		patriotKeyNumber = string;
	}
	public String getTSDFileName() {
		return TSDFileName;
	}
	public void setTSDFileName(String string) {
	    TSDFileName = string;
	}

	/**
	 * @return Returns the tSROrderDate.
	 */
	public Date getTSROrderDate() {
		return TSROrderDate;
	}
	/**
	 * @param orderDate The tSROrderDate to set.
	 */
	public void setTSROrderDate(Date orderDate) {
		TSROrderDate = orderDate;
	}
	
	
	public void addLinkVisited(String link) {
		visitedLinks.put(link, "e");
	}
	public boolean isLinkVisited(String link) {
		 return visitedLinks.get(link) != null;
	}
	public void removeLinkVisited(String link){
    	if (visitedLinks == null){
    		visitedLinks = new Hashtable();
    	}
    	
    	visitedLinks.remove(link);
    }
   
    /**
     * Scoate suffixul dintr-un intrsumentNo. 
     * @param instNo
     * @return Se returneaza -1 
     * daca nu are sufix.
     */
    public int getSuffixFromInstNo (String instNo) {
    	int suffix = -1;
    	try {
			if (instNo.indexOf(".") != -1) 
			{
			    suffix = Integer.parseInt(
			    		instNo
			    		.substring(
			    				instNo.indexOf(".") + 1, instNo.length()));
			}
		} catch (NumberFormatException e) {
			return -1;
		}
    	return suffix;
    }
    
   
	/**
	 * @return Returns the orderedSearch.
	 */
	public boolean isOrderedSearch() {
		return orderedSearch;
	}
	/**
	 * @param orderedSearch The orderedSearch to set.
	 */
	public void setOrderedSearch(boolean orderedSearch) {
		this.orderedSearch = orderedSearch;
	}
	/**
	 * @return Returns the hasAlreadyAgentInfoFormDB.
	 */
	public boolean isAllowGetAgentInfoFromDB() {
		return allowGetAgentInfoFromDB;
	}
	/**
	 * @param hasAlreadyAgentInfoFormDB The hasAlreadyAgentInfoFormDB to set.
	 */
	public void setAllowGetAgentInfoFromDB(boolean allowGetAgentInfoFromDB) {
		this.allowGetAgentInfoFromDB = allowGetAgentInfoFromDB;
	}
    
    public boolean isWasOpened() {
        return wasOpened;
    }
    public void setWasOpened(boolean wasOpened) {
        this.wasOpened = wasOpened;
        
        if( wasOpened == true )
        {
            DBManager.setDbSearchWasOpened( getSearchID() );
        }
    }
        
	private  HashMap<String,String>  generatedTemp=new HashMap<String,String>();

	public HashMap<String,String> getGeneratedTemp() {
		synchronized (generatedTemp) {
			if( generatedTemp==null ){
				generatedTemp=new HashMap<String,String>();
			}
			return generatedTemp;	
		}
	}
	public void setGeneratedTemp(HashMap<String,String> generatedTemp) {
		synchronized (this.generatedTemp) {
			this.generatedTemp = generatedTemp;	
		}
	}    
	 
	//retains changes that are made in the TSDIndexPage
	private HashMap<String,Object> changesInTSDIndexPageMap= new HashMap<String,Object>();
	
	public synchronized void putTSDIndexChange(String changeKey,Object value){
		if (changesInTSDIndexPageMap==null){
			changesInTSDIndexPageMap = new HashMap();
		}
		changesInTSDIndexPageMap.put(changeKey, value);
	}
	
	public synchronized Object getTSDIndexChange(String changeKey ){
		if (changesInTSDIndexPageMap==null){
			return null;
		}
		return changesInTSDIndexPageMap.get(changeKey);
	}
	
	public synchronized Object removeTSDIndexChange(String changeKey ){
		if (changesInTSDIndexPageMap==null){
			return null;
		}
		return changesInTSDIndexPageMap.remove(changeKey);
	}
	
    
    public synchronized boolean searchedInstrument( String book, String page, String docType )
    {
        boolean searched = false;
        
        for( int i = 0 ; i < instrumentListRef.size() ; i ++ )
        {
            Instrument tmp = (Instrument) instrumentListRef.elementAt( i );
            if( tmp.getBookNo().equals( book ) && tmp.getPageNo().equals( page ) && tmp.getRealdoctype().equals( docType ) )
            {
                searched = true;
                break;
            }
        }
        
        return searched;
    }
    
    /**
     * 
     * @param i
     * @return true if the instrument not already in list
     */
    public synchronized boolean addRefInstrument( Instrument i )
    {
        boolean searched = false;
        
        searched = searchedInstrument( i.getBookNo(), i.getPageNo(), i.getRealdoctype() ); 
        
        if( !searched )
        {
            instrumentListRef.add( i );
        }
        
        return !searched;
    }
    
    public boolean containsClickedDocument( String documentLink, String beginWith )
    {
        boolean contained = false;
        
        if( "".equals( beginWith ) )
        {
            contained = clickedDocs.contains( documentLink );
        }
        else
        {
            int iStart;
            String tmpDocumentLink = documentLink;
            
            iStart = tmpDocumentLink.indexOf( beginWith );
            if( iStart >= 0 )
            {
                tmpDocumentLink = tmpDocumentLink.substring( iStart );
            }
            
            for( int i = 0 ; i < clickedDocs.size() ; i++ )
            {
                String tmpLink = clickedDocs.elementAt( i );
                String tmpSubstring = tmpLink;
                
                iStart = tmpSubstring.indexOf( beginWith );
                if( iStart >= 0 )
                {
                    tmpSubstring = tmpSubstring.substring( iStart );
                }
                
                if( tmpSubstring.equals( tmpDocumentLink ) )
                {
                    contained = true;
                    break;
                }
                
            }
        }
        
        return contained;
    }
    
    public void clickDocument( String documentLink, String beginWith )
    {
        if( !containsClickedDocument( documentLink, beginWith ) )
        {
            clickedDocs.add( documentLink );
        }
        else
        {
            clickedDocs.remove( documentLink );
        }
    }
    
    public boolean isClickedDocument( String documentLink )
    {
        return containsClickedDocument( documentLink, "" );
    }
    
    public boolean isClickedDocumentBeginWith( String documentLink, String beginWith )
    {
        return containsClickedDocument( documentLink, beginWith );
    }
    
    public Vector<String> getClickedDocuments()
    {
        return clickedDocs;
    }
    
    public void clearClickedDocuments()
    {
        if( clickedDocs == null )
        {
            clickedDocs = new Vector<String>();
        }
        clickedDocs.clear();
    }
    
    public boolean isLinkValidated( String link )
    {
        if( validatedLinks != null )
        {
            return validatedLinks.contains( link );
        }
        else
        {
            validatedLinks = new Vector<String>();
            return false;
        }
    }
    
    public void addValidatedLink( String link )
    {
        if( validatedLinks == null )
        {
            validatedLinks = new Vector<String>();
        }
        
        if( !validatedLinks.contains( link ) )
        {
            validatedLinks.add( link );
        }
    }
        
    public void removeRecursiveAnalisedLink( String link ){
    	if( recursiveAnalisedLinks == null ){
    		recursiveAnalisedLinks = new Vector<String>();
    	}
    	
    	recursiveAnalisedLinks.remove( link );
    }
    
    public boolean isRecursiveAnaliseInProgress( String link ){
    	if( recursiveAnalisedLinks == null ){
    		recursiveAnalisedLinks = new Vector<String>();
    	}
    	
    	return recursiveAnalisedLinks.contains( link );	
    }
    
    public void addRecursiveAnalisedLink( String link ){
    	if( recursiveAnalisedLinks == null ){
    		recursiveAnalisedLinks = new Vector<String>();
    	}
    	
    	if( !recursiveAnalisedLinks.contains( link ) ){
    		recursiveAnalisedLinks.add( link );
    	}
    }
    
    public void clearRecursiveAnalisedLinks(  ){
    	if(recursiveAnalisedLinks!=null){
    		recursiveAnalisedLinks.clear();
    	}
    }
    
    public void setJsSortOrder( String jsSortChaptersBy, String sortAscendingChapters ){
    	this.jsSortChaptersBy = jsSortChaptersBy;
    	this.sortAscendingChapters = sortAscendingChapters;
    }
    
    public String getJsSortChaptersBy(){
    	return this.jsSortChaptersBy;
    }
    
    public String getJsSortAscending(){
    	return this.sortAscendingChapters;
    }
    
    /**
     * 15 character unique search id that does not change over time
     */  
    private String uniqueIdentifier = null;
    /**
     * provide an unique identifier, no matter how the search changes over time
     * it is used for creating 15 character search order ids for DataTrace
     * @return
     */
    public synchronized String getUniqueIdentifier(){
    	if(uniqueIdentifier == null){
    		String fileId = getSa().getAbstractorFileName().replace("TSR-","").replaceAll("\\s+", "").replaceAll("[^A-Za-z0-9]", "");
    		fileId = "" + getID() + fileId.replace("UnknownFile","");
    		uniqueIdentifier = (fileId + getID() + System.nanoTime()).substring(0, 15).toUpperCase();    		
    	}
    	return uniqueIdentifier;
    }
    
    public long getCommunity(){
    	return DBManager.getCommunityForSearch(this.getSearchID());
    }   
    
    /**
     * A fake search is used to create a search without actually making one.
     * With a fake search won't be need at all to create files on the hardrive.Anyway, if a search will made we must un-fake the search
     * to actually use it. 
     */
    public void unFakeSearch(User user){    	
    	msUserPath = user.getUserPath();
    	reset(user);    	    	
    }
    
    /**
     * Resets the corect path to the folder of the search. This is usefull when loading a search saved in a folder on a different folder for example
     * @param user
     */
    public void makeServerCompatible(String newContextPath){
    	if(!newContextPath.startsWith(msUserPath)) {
    		msUserPath = newContextPath.replace(File.separator + searchID + File.separator, File.separator);
    		//i modified something it means also templates might have problemes
    		synchronized (getGeneratedTemp()) {
    			for (String key : getGeneratedTemp().keySet()) {
					String oldPath = getGeneratedTemp().get(key);
					if(!oldPath.startsWith(ServerConfig.getFilePath())) {
						String cleanedOldPath = oldPath.replace("\\", "/").replaceAll("/{2,}", "/");
						int indexOf = cleanedOldPath.indexOf("/" + TSDManager.TSDDir + "/");
						if(indexOf >= 0) {
							String newPath = (ServerConfig.getFilePath() + cleanedOldPath.substring(indexOf + 1)).replace("/", File.separator);
							getGeneratedTemp().put(key, newPath);
						}
					}
				}
    		}
    		
			try {
				docManager.getAccess();
				for (DocumentI document : docManager.getDocumentsList()) {
					String oldPath = document.getOldIndexPath();
					if(!oldPath.startsWith(ServerConfig.getFilePath())) {
						String cleanedOldPath = oldPath.replace("\\", "/").replaceAll("/{2,}", "/");
						int indexOf = cleanedOldPath.indexOf("/" + TSDManager.TSDDir + "/");
						if(indexOf >= 0) {
							String newPath = (ServerConfig.getFilePath() + cleanedOldPath.substring(indexOf + 1)).replace("/", File.separator);
							document.setOldIndexPath(newPath);
						}
					}
					if(document.hasImage() && org.apache.commons.lang.StringUtils.isNotBlank(document.getImage().getPath())) {
						oldPath = document.getImage().getPath();
						if(!oldPath.startsWith(ServerConfig.getImageDirectory())) {
							String cleanedOldPath = oldPath.replace("\\", "/").replaceAll("/{2,}", "/");
							if(cleanedOldPath.matches("(.*/)(\\d{4}_\\d{2}_\\d{2}/\\d+/.*)")) {
								String newPath = (ServerConfig.getImageDirectory() + cleanedOldPath.replaceAll("(.*/)(\\d{4}_\\d{2}_\\d{2}/\\d+/.*)", "$2")).replace("/", File.separator);
								document.getImage().setPath(newPath);
							}
						}
					}
					
				}
			} finally {
				docManager.releaseAccess();
			}
    	}
    	
		// if temp folder does not exists, create it
		String tempPath = getSearchTempDir();
		if (!ro.cst.tsearch.utils.FileUtils.existPath(tempPath)) {
			File tmpFile = new File(tempPath);
			tmpFile.mkdirs();
		}
    }
    
    public boolean isFakeSearch(){
    	return StringUtils.isEmpty(msUserPath);
    }

    public long getSavedVersion(){
    	return versionSaveNo;
    }
    
    public void setSavedVersion(){
    	versionSaveNo ++;
    	putSavedVersionToFile( this );
    }
    
    public static long getSavedVersionFromFile( String searchPath ){
    	
    	long readVersion = 0;
    	String versionInfoStr = "";
    	BufferedReader br = null;
    	
    	try{
    		br = new BufferedReader( new FileReader( searchPath + VERSION_FILENAME ) );
    		
    		versionInfoStr = br.readLine();
    	}
    	catch( Exception e ){
    		e.printStackTrace();
    	}
    	
    	if( br != null ){
    		try{
    			br.close();
    		}
    		catch( Exception e ){
    			e.printStackTrace();
    		}
    	}
    	
    	try{
    		readVersion = Long.parseLong( versionInfoStr );
    	}
    	catch( Exception e ){
    		e.printStackTrace();
    	}
    	
    	return readVersion;
    }
    
    public static void putSavedVersionToFile( Search s ){
    	PrintWriter pw = null;
    	
    	try{
    		pw = new PrintWriter( new BufferedOutputStream( new FileOutputStream( s.getSearchDir() + VERSION_FILENAME ) ) );
    		
    		pw.println( s.getSavedVersion() );
    	}
    	catch( Exception e ){
    		e.printStackTrace();
    	}
    	
    	if( pw != null ){
    		try{
    			pw.close();
    		}
    		catch( Exception e ){
    			e.printStackTrace();
    		}
    	}
    }
	
	public synchronized Vector<String> getVecChaptKeysSorted() {
	   if(vecChaptKeysSorted==null){
	       vecChaptKeysSorted = new Vector();
	   }
		return vecChaptKeysSorted;
	}
	public synchronized void setVecChaptKeysSorted(Vector<String> vecChaptKeysSorted) {
		 if(vecChaptKeysSorted==null){
	       vecChaptKeysSorted = new Vector();
	   }
		this.vecChaptKeysSorted = vecChaptKeysSorted;
	
	}
	
	public Hashtable<String, ChapterSavedData> getSavedTSDIndexState() {
	 if(savedTSDIndexState==null){
	       savedTSDIndexState = new Hashtable();
	   }
		return savedTSDIndexState;
	}
	
	public void setSavedTSDIndexState(
			Hashtable<String, ChapterSavedData> saveTSDIndexState) {
			 if(saveTSDIndexState==null){
				 saveTSDIndexState = new Hashtable();
	   }
		this.savedTSDIndexState = saveTSDIndexState;
	}
		
	
	public synchronized void setMaxContextEvent( boolean value ){
		maxContextEvent = value;
	}
	
	public synchronized boolean getMaxContextEvent(){
		return maxContextEvent;
	}
	
	public boolean maxDocLimitReached(){
		
		if(docManager != null) {
			try {
				docManager.getAccess();
				if(docManager.getDocumentsCount()>= MAX_TSR_INDEX_DOCUMENT_COUNT) {
					return true;
				}else {
					return false;
				}
			} catch (Exception e) {
				e.printStackTrace();
			} finally {
				docManager.releaseAccess();
			}
		}
		
		return false;
	}
	
	public int getNumberOfDocs(){
		
		if(docManager != null) {
			try {
				docManager.getAccess();
				return docManager.getDocumentsCount();
			} catch (Exception e) {
				e.printStackTrace();
			} finally {
				docManager.releaseAccess();
			}
		}
		
		return 0;
	}
	
	public int getNumberOfDocsOfSomeDoctype(String doctype){
		
		if(docManager != null) {
			try {
				docManager.getAccess();
				return docManager.getDocumentsWithDocType(doctype).size();
			} catch (Exception e) {
				e.printStackTrace();
			} finally {
				docManager.releaseAccess();
			}
		}
		
		return 0;
	}
		
	public static void initSitesConfiguration(){
		long startTime = System.currentTimeMillis();
		logger.info("Start initSitesConfiguration");
		HashCountyToIndex.fillDataAboutAllImplementedCountyFromDatabase();
		logger.info("initSitesConfiguration: HashCountyToIndex.fillDataAboutAllImplementedCountyFromDatabase() took " + (System.currentTimeMillis() - startTime));
		startTime = System.currentTimeMillis();
		serverTypesAbrev = HashCountyToIndex.getServerTypesAbrev();
		logger.info("initSitesConfiguration: HashCountyToIndex.getServerTypesAbrev() took " + (System.currentTimeMillis() - startTime));
		startTime = System.currentTimeMillis();
		HttpManager.reloadAllSites();
		logger.info("initSitesConfiguration: HttpManager.reloadAllSites() took " + (System.currentTimeMillis() - startTime));
		startTime = System.currentTimeMillis();
		HTTPManager.inst.reloadAllSites();
		logger.info("initSitesConfiguration: HTTPManager.inst.reloadAllSites() took " + (System.currentTimeMillis() - startTime));
		startTime = System.currentTimeMillis();
		HttpManager3.reloadAllSites();
		logger.info("initSitesConfiguration: HttpManager3.reloadAllSites() took " + (System.currentTimeMillis() - startTime));
		startTime = System.currentTimeMillis();
		WarningManager.getInstance().loadWarnings();
		logger.info("initSitesConfiguration: WarningManager.getInstance().loadWarnings() took " + (System.currentTimeMillis() - startTime));
		startTime = System.currentTimeMillis();
		new CheckTaxDatesExpiredThread().start();
		logger.info("initSitesConfiguration: new CheckTaxDatesExpiredThread().start() took " + (System.currentTimeMillis() - startTime));
	}
	
	/**
	 * List of HttpSiteManagers used by this search
	 */
	private transient Collection<HTTPSiteManagerInterface> httpSiteManagers = new HashSet<HTTPSiteManagerInterface>();
	
	/**
	 * Return list of HttpSiteManagers used by this search
	 * @return
	 */
	public Collection<HTTPSiteManagerInterface> getHttpSiteManagers(){
		// deal with transient nature of httpSiteManagers
		if(httpSiteManagers == null){
			httpSiteManagers = new HashSet<HTTPSiteManagerInterface>();
		}
		return Collections.unmodifiableCollection(httpSiteManagers);
	}
	
	/**
	 * Add an entry to the list of HttpSiteManagers used by this search
	 * @param manager
	 */
	public void addHttpSiteManager(HttpSiteManager manager){
		// deal with transient nature of httpSiteManagers
		if(httpSiteManagers == null){
			httpSiteManagers = new HashSet<HTTPSiteManagerInterface>();
		}
		httpSiteManagers.add(manager);
	}
	
	/**
	 * Obtain the XML order from the HTML order
	 * @param htmlOrder
	 * @return
	 */
	public static String getXmlOrder(String htmlOrder){
		
		// check if it is already embedded in the HTML file
		int istart = htmlOrder.indexOf("<?xml version=\"1.0\" encoding=\"UTF-8\"?><ats");
		int iend = htmlOrder.indexOf("</ats>", istart);
		if(istart != -1 && iend != -1 && istart < iend){
			return htmlOrder.substring(istart, iend + "</ats>".length());
		} else {
			return null;
		}
		
	}

	/**
	 * Obtain the search order as XML 
	 * @return
	 */
	public String getXmlOrder(){
		return SearchAttributes.getXmlOrder(this);
	}
	
	/**
	 * Obtain the initial order from the xml inserted in orderFile.html
	 * 
	 */
	public Document getInitialOrderXMLFromOrderFile(){
		
		byte orderFileAsByteArray[] = DBManager.getSearchOrderLogs(getID(), FileServlet.VIEW_ORDER, false);
		
		if(orderFileAsByteArray == null) {
			File file = new File(this.getSearchDir() + "orderFile.html");
			if(file.exists()) {
				try {
					orderFileAsByteArray = org.apache.commons.io.FileUtils.readFileToByteArray(file);
				} catch (IOException e) {
					logger.error("Cannot read file " + file.getPath(), e);
				}
			}
		}
		if(orderFileAsByteArray != null) {
			String tmpXmlOrder = getXmlOrder(new String(orderFileAsByteArray));
			if (tmpXmlOrder != null){
				return XmlUtils.parseXml(tmpXmlOrder);
			}
		}
		
		return null;
	}
	  
    public void addPatriotsAlertChapter(String key){
    	if(patriotsAlertChapters==null){
    		patriotsAlertChapters = new Vector<String>();
    	}
    	patriotsAlertChapters.add(key);
    }
    
    public boolean isAlertForPatriotsChapter(String key){
    	if(patriotsAlertChapters==null){
    		patriotsAlertChapters = new Vector<String>();
    	}
    	return patriotsAlertChapters.contains(key);
    }
    
    /**
     * cached order number
     */
    private String orderNo = "";
    
    public synchronized String getOrderNumber(){

    	// return from cache if already calculated
    	if(!StringUtils.isEmpty(orderNo)){
    		return orderNo;
    	}
    	// try community file no, then abstractor fileno
    	String orderNo = getSa().getAtribute(SearchAttributes.ORDERBY_FILENO);
    	if(StringUtils.isEmpty(orderNo)){
    		orderNo = getSa().getAtribute(SearchAttributes.ABSTRACTOR_FILENO);
    	}
    	
    	orderNo = (orderNo != null) ? orderNo : "";
    	orderNo = orderNo.replaceAll("[^a-zA-Z0-9]", "");
    	
    	if(StringUtils.isEmpty(orderNo)){
    		orderNo = ID+"";
    	}
    	
    	return orderNo;
    }
    
    /**
     * Get crt county, ie TN, MO, TX
     * @param parentSite
     * @return
     */
    public String getCrtState(boolean parentSite){
    	String serverName = getCrtServerName(parentSite);
    	return serverName.substring(0,2);
    }
    
    /**
     * Get crt server type ie AO, RO etc
     * @param parentSite
     * @return
     */
    public String getCrtServerType(boolean parentSite){
    	try {
	    	String p1, p2;
	    	if(parentSite){
	    		p1 = getP1ParentSite();
	    		p2 = getP2ParentSite();
	    	} else {
	    		p1 = getP1();
	    		p2 = getP2();
	    	}
	    	DataSite dataSite = HashCountyToIndex.getDataSite(getCommId(), Integer.parseInt(p1), Integer.parseInt(p2));
	    	return dataSite.getSiteTypeAbrev();
    	}catch(Exception e){
    		throw new RuntimeException(e);
    	}
    }
    
    public DataSite getCrtDataSite(boolean parentSite){
    	try {
	    	String p1, p2;
	    	if(parentSite){
	    		p1 = getP1ParentSite();
	    		p2 = getP2ParentSite();
	    	} else {
	    		p1 = getP1();
	    		p2 = getP2();
	    	}
	    	return HashCountyToIndex.getDataSite(getCommId(), Integer.parseInt(p1), Integer.parseInt(p2));
    	}catch(Exception e){
    		throw new RuntimeException(e);
    	}
    }
    
    /**
     * Get current server name, ie TNShelbyRO
     * @param parentSite
     * @return
     */
    public String getCrtServerName(boolean parentSite){
    	try {
	    	String p1, p2;
	    	if(parentSite){
	    		p1 = getP1ParentSite();
	    		p2 = getP2ParentSite();
	    	} else {
	    		p1 = getP1();
	    		p2 = getP2();
	    	}
	    	return TSServersFactory.getTSServerName(
	    			InstanceManager.getManager().getCommunityId(getID()),
	    			p1, p2);
    	}catch(Exception e){
    		throw new RuntimeException(e);
    	}
    }
    public void clearVisitedLinks()
    {
    	if(visitedLinks!=null){
    		visitedLinks.clear();
    	}
    }
   public void clearValidatedLinks()
   {
	   validatedLinks.clear();   
   }

   public void clearRecursivedAnalisedLink(){
	   recursiveAnalisedLinks.clear();
   }
	public int getGoBackType() {
		if("one_level".equals(goBackType)) {
			return 1;
		} else if ("two_level".equals(goBackType)) {
			return 2;
		} else if( "all_level".equals(goBackType)) {
			return 50; 
		} else if( "reset_level".equals(goBackType)) {
			return 0;
		}
		return 1;
	}
	public void setGoBackType(int level) {
		switch (level) {
		case 1:
			goBackType = "one_level";
			break;
		case 2:
			goBackType = "two_level";
			break;
		case 50:
			goBackType = "all_level";
			break;
		case 0:
			goBackType = "reset_level";
			break;
		default:
			goBackType = "one_level";
			break;
		}
	}



	/**
	 * Check that a string is not empty
	 * @param s
	 * @return
	 */
	private boolean notEmpty(String s){
		if(s == null){ return false; }
		s = s.trim();
		if(s.length() ==0){ return false; }
		if("0".equals(s) || "0_0".equals(s)){ return false; }
		return true;
	}
	
	/**
	 * Cleanup a string by removing non alphanumeric, - and _ characters
	 * @param value
	 * @return
	 */
	private static String cleanup(String value){
		return value.replaceAll("[^a-zA-Z0-9_-]", "");
	}
	
	public  synchronized boolean isDisabledChapter(String path) {
		if(disabledChapters==null){//deserealization for old searches
			disabledChapters = new ArrayList<String>();
		}
		return disabledChapters .contains(path);
	}
	
	public synchronized List<String> getDisabledChapters() {
		if(disabledChapters==null){//deserealization for old searches
			disabledChapters = new ArrayList<String>();
		}
		return disabledChapters;
	}
	
	//we might need to syncronize something in this function
	//I display this string during automatic search
	public String getPrettyModuleInfo(){
		StringBuilder moduleInfo = new StringBuilder();
		if (ssi != null){
			ASThread thread = ASMaster.getSearch(this.ID);
			TSServerInfoModule m  = thread.getCrtModule();
			
			if (m != null){
				moduleInfo.append(" (").append(m.getLabel()).append(": ");
				Map<String,String> moduleParams = m.getParamsForLog();
				boolean firstTime = true;
				for(Entry<String,String> entry : moduleParams.entrySet() ){
					if (!entry.getKey().equals("Start Date") && !entry.getKey().equals("End Date")){
						String value = entry.getValue();
						value = value.replaceAll("(, )+$",""); 
						if(!firstTime){
							moduleInfo.append(", ");
						} else {
							firstTime = false;
						}
						moduleInfo.append(entry.getKey()).append(" = <b>").append(value).append("</b>");
					}
				}
				moduleInfo.append(")");
			}
		}
		return moduleInfo.toString();
	}
	
	/**
	 * get the searchId of the parent search of an update
	 * @return
	 */
	public long getParentSearchId() {
		if( parentSearchId == 0 ){
			return ( parentSearchId  = NO_UPDATED_SEARCH ) ;
		}
		return parentSearchId;
	}
	
	/**
	 * set the searchId of the parent search of an update
	 * @param parentSearchId
	 */
	public void setParentSearchId(long parentSearchId) {
		this.parentSearchId = parentSearchId;
	}
	
	public void replaceAllSearchFromOrder(long oldSearchId) {
		try {
			
			if(ServerConfig.isEnableOrderOldField()) {
				File order = new File(getSearchDir() + "orderFile.html");
				
				if(!order.exists()) {
					if(DBReports.hasOrderInDatabase(getID())) {
		    			byte orderFileAsByteArray[] = DBManager.getSearchOrderLogs(getID(), FileServlet.VIEW_ORDER, false);
		    			if(orderFileAsByteArray != null && orderFileAsByteArray.length != 0) {
		    				try {
								org.apache.commons.io.FileUtils.writeByteArrayToFile(order, orderFileAsByteArray);
							} catch (IOException e) {
								logger.error("Cannot force log from database in replaceAllSearchFromOrder for searchId " + getID(), e);
							}
		    			}
		    		}
			    }
				
				File orderNew = new File(getSearchDir() + "orderFileNew.html");
				if(order.exists()){
					FileOutputStream fout = new FileOutputStream(orderNew);
					fout.write(org.apache.commons.io.FileUtils.readFileToString(order).replaceAll(
							String.valueOf(oldSearchId), 
							String.valueOf(getID())).getBytes());
					fout.flush();
					fout.close();
					if(FileUtils.copy(getSearchDir() + "orderFileNew.html", getSearchDir() + "orderFile.html"))
						orderNew.delete();
	
				}
			}
			
		} catch (Exception e) {
			e.printStackTrace();
		}
		
	}
	
	/**
	 * Determines which files must be included in the search context archive
	 * @return String array containing the included file names
	 */
	/*public String[] getIncludedFiles() {
		Vector<String> includedFiles = getUploadedFiles();
		String legalDescriptionImage = getLegalDescriptionLink();
		if(includedFiles!=null) {
			includedFiles.add(legalDescriptionImage);
			return includedFiles.toArray(new String[includedFiles.size()]);
		}
		return new String[]{legalDescriptionImage};
	}*/
	
    transient private boolean subdivisionExceptionPrintedRO = false;

	public boolean isSubdivisionExceptionPrintedRO() {
		return subdivisionExceptionPrintedRO;
	}
	public void setSubdivisionExceptionPrintedRO(boolean subdivisionExceptionPrintedRO) {
		this.subdivisionExceptionPrintedRO = subdivisionExceptionPrintedRO;
	}
	
	public Legal loadLegalFromSearchAttributes(){
		if (legal == null)
			legal = new Legal();
		if (sa == null)
			return new Legal();		
		Subdivision subdivision = (Subdivision)legal.getSubdivision();
		if( subdivision == null) {
			subdivision = new Subdivision();
			legal.setSubdivision(subdivision);
		}
		subdivision.setName(sa.getAtribute(SearchAttributes.LD_SUBDIV_NAME));
		subdivision.setLot(sa.getAtribute(SearchAttributes.LD_LOTNO));
		subdivision.setSubLot(sa.getAtribute(SearchAttributes.LD_SUBLOT));
		subdivision.setBlock(sa.getAtribute(SearchAttributes.LD_SUBDIV_BLOCK));
		subdivision.setPhase(Roman.normalizeRomanNumbers(sa.getAtribute(SearchAttributes.LD_SUBDIV_PHASE)));
		subdivision.setUnit(sa.getAtribute(SearchAttributes.LD_SUBDIV_UNIT));
		subdivision.setTract(sa.getAtribute(SearchAttributes.LD_SUBDIV_TRACT));
		
		TownShip townShip = (TownShip)legal.getTownShip();
		if(townShip == null){
			townShip = new TownShip();
			legal.setTownShip(townShip);
		}
		townShip.setSection(sa.getAtribute(SearchAttributes.LD_SUBDIV_SEC));
		townShip.setTownship(sa.getAtribute(SearchAttributes.LD_SUBDIV_TWN));
		townShip.setRange(sa.getAtribute(SearchAttributes.LD_SUBDIV_RNG));
		
		return legal;
	}
	
	public Legal getLegal(){
		return legal;
	}
	public SearchFlags getSearchFlags() {
		if(searchFlags == null)
			searchFlags = new SearchFlags();
		return searchFlags;
	}
	public void setSearchFlags(SearchFlags searchFlags) {
		this.searchFlags = searchFlags;
	}
    
	
    
	public DocumentsManagerI getDocManager() {
		return docManager;
	}
	 
	public void setDocManager(DocumentsManagerI docManager) {
		this.docManager = docManager;
	}
	
	public SearchStatus.StatusCode getSearchRuningStatus(){
		ASThread thread = ASMaster.getSearch(this.ID);
		if(thread!=null){
			if(thread.isAlive()){
				if(thread.isGoBack()){
					return SearchStatus.StatusCode.AUTOMATIC_ON_GO_BACK;
				}
				return SearchStatus.StatusCode.AUTOMATIC_ON;
			}
		}
		if(this.isOcrInProgess()){
			return SearchStatus.StatusCode.OCR_IN_PROGRESS;
		}
		return SearchStatus.StatusCode.AUTOMATIC_OFF;
	}
	/**
	 * Returns the Agent File Id as seen in Search Page
	 * @return
	 */
	public String getAgentFileNo(){
		return sa.getAtribute(SearchAttributes.ORDERBY_FILENO);
	}
	
	/**
	 * Returns the Abstractor File Id as seen in Search Page
	 * @return
	 */
	public String getAbstractorFileNo(){
		return sa.getAtribute(SearchAttributes.ABSTRACTOR_FILENO);
	}
	
	public void ocrDocuments() {
		String crtState = getStateAbv();
		ocrDocuments(crtState, DocumentTypes.TRANSFER, true, true, false);
		ocrDocuments(crtState, DocumentTypes.MORTGAGE, false, true, true);
		
		if(!ServerConfig.isOcrAllDocumentsAutomaticEnable()){
			return;
		}
		try {
			docManager.getAccess();
			for (DocumentI documentI : docManager.getDocumentsWithDocType(true,DocumentTypes.MORTGAGE, DocumentTypes.TRANSFER)) {
				if(documentI.hasImage()){
					documentI.getImage().setPlanedForOCR(true);
					OCRDownloader ocrDownloader = new OCRDownloader( this , documentI);
					OCRDownloaderPool.getInstance().runTask(ocrDownloader);
				}
			}
		}catch (Exception e) {
			e.printStackTrace();
		} finally{
			docManager.releaseAccess();
		}
	}
	
	public void ocrDocuments(String state, String documentType, boolean enableTimeLimit, boolean onlyChecked, boolean addReferences) {
		if (ServerConfig.isEnabledAutomaticOcrAllDocuments(state, documentType)) {
			Calendar date = Calendar.getInstance();
			if (enableTimeLimit) {
				date.add(Calendar.MONTH, -ServerConfig.getAutomaticOcrAllDocumentsInterval(state, documentType));
			}
			List<DocumentI> documents = new ArrayList<DocumentI>(); 
			try {
				docManager.getAccess();
				for (DocumentI documentI : docManager.getDocumentsWithDocType(onlyChecked, documentType)) {
					boolean isGood = true;
						if (enableTimeLimit) {
							if (documentI instanceof RegisterDocumentI) {
								RegisterDocumentI registerDocumentI = (RegisterDocumentI)documentI;
								if (registerDocumentI.getRecordedDate().before(date.getTime())) {
									isGood = false;	
								}	
							}	
							
						}
						if (isGood) {
							documents.add(documentI);
							if (addReferences) {
								for (DocumentI reference : documentI.getReferences()) {
									if (!onlyChecked || reference.isChecked()) {
										documents.add(reference);
									}
								}
							}
						}
				}
				for (DocumentI documentI : documents) {
					if(documentI.hasImage()){
						documentI.getImage().setPlanedForOCR(true);
						OCRDownloader ocrDownloader = new OCRDownloader(this, documentI);
						OCRDownloaderPool.getInstance().runTask(ocrDownloader);
					}
				}
			}catch (Exception e) {
				e.printStackTrace();
			} finally{
				docManager.releaseAccess();
			}
		}
	}
	
	public Date setTsrViewFilterForProduct(boolean setDate) {
		String tsrViewFilter = Products.getTsrViwFilter(getCommId(), getProductId());
		
		if(Products.TsrViewFilterOptions.DEF.toString().equalsIgnoreCase(tsrViewFilter)) {
			return null;
		}
		if(docManager == null || (docManager!=null && docManager.isFieldModified(DocumentsManager.Fields.START_VIEW_DATE))) {
			return null;
		}
		
		if(Products.TsrViewFilterOptions.LTD.toString().equalsIgnoreCase(tsrViewFilter)
			|| Products.TsrViewFilterOptions.PTD.toString().equalsIgnoreCase(tsrViewFilter)) {
			try {
				docManager.getAccess();
				
				TransferI transf = Products.TsrViewFilterOptions.LTD.toString().equalsIgnoreCase(tsrViewFilter)
										? docManager.getLastRealTransfer()
										: docManager.getPreviousRealTransfer();	;	
				
				if(transf!=null && transf.getRecordedDate()!=null) {
					if(setDate) {
						docManager.setStartViewDate(transf.getRecordedDate());
					}
					return transf.getRecordedDate();
				}		
				
			} catch (Exception e) {
				e.printStackTrace();
			} finally {
				docManager.releaseAccess();
			}
		}
		
		if(NumberUtils.isDigits(tsrViewFilter)) {
			try {
				docManager.getAccess();
				int years = Integer.parseInt(tsrViewFilter);
				Calendar now = Calendar.getInstance();
				now.add(Calendar.YEAR, -years);
				if(setDate) {
					docManager.setStartViewDate(now.getTime());
				}
				return now.getTime();
			}catch(Exception e) {
				e.printStackTrace();
			}finally {
				docManager.releaseAccess();
			}
		}
		return null;
		
	}
	
	public Date getTsrViewFilterForProduct(int productId) {
		String tsrViewFilter = Products.getTsrViwFilter(getCommId(), productId);
		if(Products.TsrViewFilterOptions.DEF.toString().equalsIgnoreCase(tsrViewFilter)) {
			return getSa().getStartDate();
		}
		if(Products.TsrViewFilterOptions.LTD.toString().equalsIgnoreCase(tsrViewFilter)
			|| Products.TsrViewFilterOptions.PTD.toString().equalsIgnoreCase(tsrViewFilter)) {
			try {
				docManager.getAccess();
				
				TransferI transf = Products.TsrViewFilterOptions.LTD.toString().equalsIgnoreCase(tsrViewFilter)
										? docManager.getLastRealTransfer()
										: docManager.getPreviousRealTransfer();	;	
				
				if(transf!=null && transf.getRecordedDate()!=null) {
					return transf.getRecordedDate();
				}		
				
			} catch (Exception e) {
				e.printStackTrace();
			} finally {
				docManager.releaseAccess();
			}
		}
		if(NumberUtils.isDigits(tsrViewFilter)) {
			try {
				int years = Integer.parseInt(tsrViewFilter);
				Calendar now = Calendar.getInstance();
				now.add(Calendar.YEAR, -years);
				return now.getTime();
			}catch(Exception e) {
				e.printStackTrace();
			}
		}
		return getSa().getStartDate();
	}

	
	private void reloadLegalFromOcr() {

		try{
			docManager.getAccess();
			
			TransferI regDoc = docManager.getLastRealTransfer();
			
			if(regDoc == null || ocrData == null) {
				return;
			}
			if(regDoc.hasImage() && regDoc.getImage().isOcrDone()) {
				ocrData.reloadLegalDescription(searchID);
				
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
			}						
		}catch(Exception e) {
			e.printStackTrace();	
		}finally {
			docManager.releaseAccess();
		}
	}
	
	private void  clearReferences(){
		List<DocumentI> roDocList = docManager.getDocumentsWithType(DType.ROLIKE);
		for( DocumentI doc:roDocList ){
			doc.getReferences().clear();
			if(!doc.isManualChecked()){
				doc.setChecked(true);
			}
			if(!doc.isManualIncludeImage()) {
				doc.setIncludeImage(true);
			}
		}
	}
	
	public void applyQARules(){
		applyQARules(true);
	}
	
	public void applyQARules(boolean addAutomaticallyReferencesToDocs){
		if(docManager == null){
			return;
		}
		try{
			docManager.getAccess();

			reloadLegalFromOcr();
			
			clearReferences();
			
			List allDocs = docManager.getDocumentsList();
			DocumentUtils.sortDocuments( allDocs, MultilineElementsMap.DATE_ORDER_ASC );
			docManager.replaceAllDocumentsWith(allDocs);
			
			applyCountyParticularyQARulesInit();
			
			addReferencesToDocs(addAutomaticallyReferencesToDocs);
			
			if(correctMiscRerecoredDocuments()){
				/*clearReferences();
				
				applyCountyParticularyQARulesInit();
				
				addReferencesToDocs();*/
			}
			
			/*for( DocumentI doc:roDocList ){
				if( doc.getParsedReferences().size() > SimpleChapterUtils.MAX_NO_OF_REFERENCES_ACCEPTED ){
					doc.getParsedReferences().clear();
					for(DocumentI ref:doc.getReferences()){
						doc.addParsedReference(ref.getInstrument());
					}
					doc.setKeptJustGoodReferences(true);
				}
			}*/
			for (DocumentI doc : docManager.getDocuments(false, true, "MORTGAGE")) {
				((MortgageI) doc).setReleased(false);
			}
			
			//uncheck released documents and releases 
			for(DocumentI realRel:docManager.getDocuments(false,/* not docSubtype*/true, "RELEASE","PARTIAL REL","PARTIAL RELEASE")){
				//for CA PR is the subcategory for partial release
				if( "PR".equals(realRel.getDocSubType())||realRel.getDocSubType().toLowerCase().contains("partial") ){ 
					continue;
				}
				Set<RegisterDocumentI> refs = realRel.getReferences();
				boolean atLeastOneGoodTypeReleased = false;
				for(RegisterDocumentI regDoc:refs){
					boolean testatLeastOneGoodTypeReleased = uncheckDocsAndReferences(regDoc,0);
					atLeastOneGoodTypeReleased = atLeastOneGoodTypeReleased || testatLeastOneGoodTypeReleased;
				}
				if(atLeastOneGoodTypeReleased){
					if(!realRel.isManualChecked()){
						realRel.setChecked(false);
					}
					if(!realRel.isManualIncludeImage()) {
						realRel.setIncludeImage(false);
					}
				}
			}
			
			List<DocumentI> allStewartStartersFile = docManager.getDocumentsWithDataSource(false, "SF");
			uncheckDocumentList(allStewartStartersFile, false);
			
			List<DocumentI> allATSStartersFile = docManager.getDocumentsWithDataSource(false, "ATS");
			uncheckDocumentList(allATSStartersFile, false);
			
			List<DocumentI> allPriorFilesList = docManager.getDocumentsWithDataSource(false, "PF");
			uncheckDocumentList(allPriorFilesList, false);
			List<DocumentI> automaticPriorFilesList = new ArrayList<DocumentI>();
			for (DocumentI documentI : allPriorFilesList) {
				if(documentI.isSavedFrom(SavedFromType.AUTOMATIC)){
					automaticPriorFilesList.add(documentI);
				}
			}
			
			if(automaticPriorFilesList.size() > 3) {
				Collections.sort(automaticPriorFilesList, DocumentUtils.getDateComparator(false));
				List<DocumentI> ownerPolicies = new Vector<DocumentI>();
				List<DocumentI> otherPolicies = new Vector<DocumentI>();
				
				HashMap<String, DocumentI> allDocsPF = new HashMap<String, DocumentI>();
				for (DocumentI documentI : automaticPriorFilesList) {
					if(documentI.getInstno().startsWith("O")) {
						ownerPolicies.add(documentI);
					} else {
						otherPolicies.add(documentI);
					}
					allDocsPF.put(documentI.getId(), documentI);
				}
				int limitForPFDocuments = 3;
				List<DocumentI> documentsRetained = new Vector<DocumentI>();
				for (DocumentI documentI : ownerPolicies) {
					if(documentsRetained.size() < limitForPFDocuments) {
						allDocsPF.remove(documentI.getId());
						documentsRetained.add(documentI);
					}
				}
				for (DocumentI documentI : otherPolicies) {
					if(documentsRetained.size() < limitForPFDocuments) {
						allDocsPF.remove(documentI.getId());
						documentsRetained.add(documentI);
					}
				}
				if(allDocsPF.size() > 0) {
					Set<String> toRemoveDocuments = allDocsPF.keySet();
					docManager.remove(toRemoveDocuments);
					
				}
				
				if(documentsRetained.size() > 0) {
					String logMe = "<br>The following SPF policies were retained from Automatic Search: " + documentsRetained.get(0).getInstno();
					for (int i = 1; i < documentsRetained.size(); i++) {
						logMe += ", " + documentsRetained.get(i).getInstno();
					}
					SearchLogger.info(logMe, getID());
				}
				
			}
			
			List<DocumentI> allSKDocuments = docManager.getDocumentsWithDataSource(false, "SK");
			List<DocumentI> allSKDocumentsToUncheck = new Vector<DocumentI>();
			
			for (DocumentI documentI : allSKDocuments) {
				if (documentI instanceof PriorFileDocumentI) {
					allSKDocumentsToUncheck.add(documentI);
				}
			}
			
			uncheckDocumentList(allSKDocumentsToUncheck, false);
			
			List<DocumentI> allHoaFile = docManager.getDocumentsWithDataSource(false, "HO");
			uncheckDocumentList(allHoaFile, false);
			
			uncheckDocumentList(docManager.getDocumentsWithDataSource(false, "GM"), false);
			
			applyParticularyQARules();
			applyCommunityParticularyQARules();
			
			docManager.updateStartViewDate(false);
			
			SearchLogger.info("QA rules applied<br>", getID());
			
		}
		catch (Exception e){
			
			SearchLogger.info("QA rules applied (with problems)<br>", getID());
			
			logger.error("Cannot apply QA rules for searchid " + getID(), e);
		}
		finally{
			docManager.releaseAccess();
		}
		
		
	}

	private boolean  correctMiscRerecoredDocuments() {
		List<DocumentI> rerecDocs = docManager.getDocuments(DocumentTypes.MISCELLANEOUS, "Re-recorded");
		boolean corrected =  false;
		for(DocumentI doc:rerecDocs){
			
			if(!doc.getType().equals(DType.ROLIKE) || !(doc instanceof RegisterDocument)){
				continue;
			}
			
			if(doc.getReferences().size()==1){
				for(RegisterDocumentI regDoc:doc.getReferences()){
					String oldCateg = doc.getInstrument().getDocType();
					String newCateg = regDoc.getDocType();
					
					if(!oldCateg.equalsIgnoreCase(newCateg)){
						TsdIndexPageServer.clearDocmanagerCaches(docManager, doc);
						ArrayList<String> l = new ArrayList<String>();
						RegisterDocumentI regdoc = DocumentsManager.createRegisterDocument( ID, newCateg, (RegisterDocument)doc, l);
						
						ArrayList<String> allsubcategories = DocumentTypes.getSubcategoryForCategory(regDoc.getDocType(), ID);
						if(allsubcategories!=null){
							for(String sub:allsubcategories){
								if(sub!=null && sub.toLowerCase().contains("re-recorded")){
									regdoc.setDocSubType(sub);
								}
							}
						}
						
						if(regdoc.isOneOf(DocumentTypes.MORTGAGE)){
							if(regdoc instanceof MortgageI){
								((MortgageI)regdoc).setGranteeLenderFreeForm(doc.getGranteeFreeForm());
							}
						}
						
						docManager.replace( doc, regdoc );
						TsdIndexPageServer.clearDocmanagerCaches(docManager, regdoc);
						corrected = true;
					}
				}
			}
		}
		return corrected;
	}

	private static RegisterDocumentI findTheRootForDocument(RegisterDocumentI regDoc, String rootCategory, String ... otherCategories){
		if(!regDoc.isOneOf(rootCategory)){
			Set<RegisterDocumentI> refs = regDoc.getReferences();
			ArrayList<RegisterDocumentI> references = new ArrayList<RegisterDocumentI>();
			references.addAll(refs);
			
			DocumentUtils.sortDocuments(references, MultilineElementsMap.DATE_ORDER_DESC);
			for(RegisterDocumentI ref:references){
				Date refRecDate = ref.getRecordedDate();
				Date regDocDate = regDoc.getRecordedDate();
				if(refRecDate!=null){
					if(ref.before(regDoc)||refRecDate.equals(regDocDate)){
						if(ref.isOneOf(otherCategories)){
							return findTheRootForDocument(ref, rootCategory);
						}else if(ref.isOneOf(rootCategory)){
							return ref;
						}
					}
				}
			}
		}
		return regDoc;
	}
	
	private void addDepthReferencesToDocs(String rootCategory, String ... otherCategories){
		List<DocumentI>  docs = docManager.getDocumentsWithDocType(otherCategories);
		DocumentUtils.sortDocuments(docs, MultilineElementsMap.DATE_ORDER_DESC);
		
		for(DocumentI doc:docs){
			if(doc instanceof RegisterDocumentI){
				RegisterDocumentI regDoc = (RegisterDocumentI)doc;
				RegisterDocumentI rootDoc = findTheRootForDocument(regDoc, rootCategory, otherCategories);
				
				if( !rootDoc.equals(regDoc)&&(rootDoc.before(regDoc)||rootDoc.getRecordedDate().equals(regDoc.getRecordedDate())) ){
					if( !regDoc.getReferences().contains(rootDoc) ){
						regDoc.addReference(rootDoc);
						rootDoc.addReference(regDoc);
						
						InstrumentI inst = rootDoc.getInstrument();
						if(!regDoc.flexibleContainsParsedReference(inst,true)){
							InstrumentI inst1 = new com.stewart.ats.base.document.Instrument();
							inst1.setInstno(inst.getInstno());
							inst1.setBook(inst.getBook());
							inst1.setPage(inst.getPage());
							inst1.setDocno(inst.getDocno());
							regDoc.addParsedReference(inst1);
						}
						
					}
				}
			}
		}
	}
	
	/**
	 * 
	 */
	public void addReferencesToDocs(boolean addAutomaticallyReferencesToDocs) {
		County county = InstanceManager.getManager().getCurrentInstance(ID).getCurrentCounty();
		//because the hash for Instrument is changed the HashSet in doc references needs to be updated		
		Map<InstrumentI,InstrumentI> instrModified = new HashMap<InstrumentI,InstrumentI>();
		for( DocumentI doc:docManager.getDocumentsList() ){
		
			//remove short form parsed references if they exists also in long form 
			if (this.getStateId().equals(StateContants.CA_STRING)){
				Set<InstrumentI> docParsedReferences = doc.clone().getParsedReferences();
				for (InstrumentI instrumentI : docParsedReferences) {
					removeShortFormParsedRef(doc, instrumentI);
				}
			}
			for( InstrumentI instr:doc.getParsedReferences() ){
				List<RegisterDocumentI> registerDocuments = getDocumentsByInstrument(instr);
				//||instr.getInstno().equals("20090240029")
//				||doc.getInstrument().getInstno().equals("20070219341")
//				||instr.getInstno().equals("20070223467")
				EqualsInstrumentAbstractionImpl equalsInstrumentAbstractionImpl = null;
				boolean isSpecialCompare = false;
				if (cityCheckedParentSite == GWTDataSite.LA_TYPE){
					if (county.getName().equals("Kendall") || county.getName().equals("Guadalupe")){
						registerDocuments = docManager.getRoLikeDocumentList();
						equalsInstrumentAbstractionImpl = new EqualsInstrumentAbstractionImpl(county);
						isSpecialCompare = true;
					}
				} 
				
				for( RegisterDocumentI regDoc:registerDocuments ){
					boolean goodForAdd = !doc.equals(regDoc);
					if (isSpecialCompare){
						RegisterDocumentI regDocClone = regDoc.clone();
						InstrumentI instrClone = instr.clone();	
						goodForAdd = normaliseInstrumentReferences(instrClone, equalsInstrumentAbstractionImpl, regDoc,
								goodForAdd);
						if (goodForAdd){
							//for now only the intrument in parsed references is modified , when the instrument for doc
							// needs to be updated we should do the same thing
							if (! instrClone.equals(instr )){
								instrModified.put(instr, instrClone);
							}			
						}
					}
					if(goodForAdd){ //make sure we do not have a document that refers himself
						doc.addReference(regDoc);
						if(doc instanceof RegisterDocumentI){
							regDoc.addReference((RegisterDocumentI)doc);
							
						}
					}
				}
			}
			if (instrModified.size()>0){
				for (InstrumentI curr : instrModified.keySet()) {
					doc.getParsedReferences().remove( curr );
					doc.getParsedReferences().add(instrModified.get(curr));
				}
				instrModified.clear();
			}
			
			if (addAutomaticallyReferencesToDocs){
				List<InstrumentI> pair = new ArrayList<InstrumentI>();
				pair.add(doc);
				pair.addAll(doc.getReferences());
				
				// If at least a group of 3 than we need to be sure each refers each.
				if (pair.size() > 1){
					for (InstrumentI instrumentI : pair){
						if (!instrumentI.equals(doc)){	// I am sure I have all references
							if (instrumentI instanceof RegisterDocumentI) {
								RegisterDocumentI regDoc = (RegisterDocumentI)instrumentI;
								
								if (doc instanceof RegisterDocumentI){			
									regDoc.addReference((RegisterDocumentI) doc);
									
									InstrumentI inst = doc.getInstrument();
									if (!regDoc.flexibleContainsParsedReference(inst, true)){
										if (this.getStateId().equals(StateContants.CA_STRING)){
											//remove short form parsed references if they exists also in long form 
											removeShortFormParsedRef(regDoc, inst);
										}
										regDoc.addParsedReference(inst.clone());
									}
								}
							}
						}
					}
				}
			}
		}
		
		ArrayList<String> subcategories = DocumentTypes.getSubcategoryForCategory(DocumentTypes.CCER, ID);
		if(subcategories!=null){
			addDepthReferencesToDocs(DocumentTypes.CCER, DocumentTypes.MODIFICATION);
		}
		addDepthReferencesToDocs(DocumentTypes.MORTGAGE, DocumentTypes.MODIFICATION);
	}

	/**
	 * @param regDoc
	 * @param inst
	 */
	public void removeShortFormParsedRef(DocumentI regDoc, InstrumentI inst) {
		String instrumentNo = inst.getInstno();
		if (instrumentNo.contains("-") && instrumentNo.length() > (instrumentNo.indexOf("-") + 1)){
			instrumentNo = instrumentNo.substring(instrumentNo.indexOf("-") + 1);
			InstrumentI instClone = inst.clone();
			instClone.setInstno(instrumentNo);
			if (regDoc.flexibleContainsParsedReference(instClone, true)){
				Set<InstrumentI> parsedRefs = regDoc.getParsedReferences();
				Set<InstrumentI> parsedReferenceToBeRemoved = new LinkedHashSet<InstrumentI>();
				for(InstrumentI instr : parsedRefs){
					if (instr.flexibleEquals(instClone, true)){
						parsedReferenceToBeRemoved.add(instr);
					}
				}
				if (!parsedReferenceToBeRemoved.isEmpty()){
					for(InstrumentI instr : parsedReferenceToBeRemoved){
						regDoc.removeParsedReference(instr);
					}
				}
			}
		}
	}
	
	public List<RegisterDocumentI> getDocumentsByInstrument(InstrumentI instr) {
		List<RegisterDocumentI> registerDocuments = docManager.getRegisterDocuments(instr);
		String docType = instr.getDocType();
		String docSubType = instr.getDocSubType();
		if (!StringUtils.isEmpty(docType) && StringUtils.isEmpty(docSubType)) {
			InstrumentI clone = instr.clone();
			String serverDocType = docType.replaceAll("\\s+", " ").toUpperCase();
	    	String docCateg = DocumentTypes.getDocumentCategory(serverDocType, searchID); 
        	clone.setDocType(docCateg);
        	String stype = DocumentTypes.getDocumentSubcategory(serverDocType, searchID);
        	if("MISCELLANEOUS".equals(stype)&&!"MISCELLANEOUS".equals(docCateg)){
        		stype = docCateg;
        	}
        	clone.setDocSubType(stype);
        	List<RegisterDocumentI> newRegisterDocuments = docManager.getRegisterDocuments(clone);
        	for (RegisterDocumentI regDoc: newRegisterDocuments) {
        		if (!registerDocuments.contains(regDoc)) {
        			registerDocuments.add(regDoc);
        		}
        	}
        }
		return registerDocuments;
	}

	public RegisterDocumentI remapDoctypeForParsedReferences(RegisterDocumentI regDoc){
		
		RegisterDocumentI regDocClone = regDoc.clone();
		Set<InstrumentI> parsedReferences = regDocClone.getParsedReferences();
		if (parsedReferences != null && parsedReferences.size() > 0){
			for (InstrumentI instrParsedRef : parsedReferences) {
				String doctype = instrParsedRef.getDocType();
				if (org.apache.commons.lang.StringUtils.isNotEmpty(doctype)){
					instrParsedRef.setDocType(DocumentTypes.getDocumentCategory(doctype, searchID));
					instrParsedRef.setDocSubType(DocumentTypes.getDocumentSubcategory(doctype, searchID));
				}
			}
			regDocClone.setParsedReferences(parsedReferences);
		}
		
		return regDocClone;
	}
	/**
	 * @param instr
	 * @param equalsInstrumentAbstractionImpl
	 * @param regDoc
	 * @param goodForAdd
	 * @return
	 */
	private boolean normaliseInstrumentReferences(InstrumentI instr,
			EqualsInstrumentAbstractionImpl equalsInstrumentAbstractionImpl, RegisterDocumentI regDoc,
			boolean goodForAdd) {
		boolean  equalTXKendallInstruments = equalsInstrumentAbstractionImpl.equals(regDoc.getInstrument(), instr);
		
		if (equalTXKendallInstruments){
			equalsInstrumentAbstractionImpl.normalise(regDoc.getInstrument(), instr);
		}
		goodForAdd = goodForAdd && equalTXKendallInstruments;
		return goodForAdd;
	}
	
	private boolean uncheckDocsAndReferences(RegisterDocumentI regDoc, int level) {
		if(level>1){
			return false;
		}
		boolean atLeastOneGoodTypeReleased = false;
		if(regDoc.isOneOf( "MORTGAGE", "LIEN", "MODIFICATION", "SUBORDINATION", "MISCELLANEOUS", "COURT", "CORPORATION", "LEASE", "ASSIGNMENT"
				, DocumentTypes.CCER )){
			atLeastOneGoodTypeReleased = true;
			if(!regDoc.isManualChecked()){
				regDoc.setChecked( false );
				if(!regDoc.isManualIncludeImage()) {
					regDoc.setIncludeImage(false);
				}
				if(regDoc.isOneOf("MORTGAGE") && regDoc instanceof MortgageI){
					((MortgageI)regDoc).setReleased( true );
				}
				for(RegisterDocumentI resfReqDoc:regDoc.getReferences()){
					if(resfReqDoc.isOneOf("MODIFICATION", "SUBORDINATION", "MISCELLANEOUS", "APPOINTMENT", "ASSIGNMENT","SUBSTITUTION","ASSUMPTION","RQNOTICE") || 
							(resfReqDoc.is("RELEASE") && resfReqDoc.getDocSubType().toUpperCase().contains("PARTIAL"))) {
						if(!resfReqDoc.isManualChecked()){
							resfReqDoc.setChecked(false);
							if(!resfReqDoc.isManualIncludeImage()) {
								resfReqDoc.setIncludeImage(false);
							}
						}
					}
					if((resfReqDoc.is("MORTGAGE")&&regDoc.is("MORTGAGE"))){
						if(regDoc.getDocSubType().toLowerCase().contains("re-recorded")) {
							level+=1;
							uncheckDocsAndReferences(resfReqDoc, level);
						}
					} else if(resfReqDoc.is(DocumentTypes.LIEN)&&regDoc.is(DocumentTypes.LIEN)){
						if(resfReqDoc.getDocSubType().toLowerCase().contains("modlien")||regDoc.getDocSubType().toLowerCase().contains("modlien")) {
							level+=1;
							uncheckDocsAndReferences(resfReqDoc, level);
						}
					}
				}
			}
		}
		return atLeastOneGoodTypeReleased;
	}
	
	private void applyCountyParticularyQARulesInit() {
		int stateId = Integer.parseInt(getStateId());
		int countyId = Integer.parseInt(getCountyId());
		
		switch (stateId) {
		case StateContants.FL:
			if(countyId == CountyConstants.FL_Sarasota) {
				applyParticularyQARulesInitForFLSarasota();
			}
			break;
		case StateContants.TX:
			
			switch (countyId) {
			case CountyConstants.TX_Collin:
			case CountyConstants.TX_Dallas:
			case CountyConstants.TX_Denton:
			case CountyConstants.TX_Kaufman:
			case CountyConstants.TX_Tarrant:
				applyParticularyQARulesInitForTXCollin();
				break;

			default:
				break;
			}
			
			break;
		default:
			break;
		}
		
	}
	
	private void applyParticularyQARulesForStewaertOrders() {
		try{
			if(	getSa().getAtribute(SearchAttributes.SEARCH_STEWART_TYPE).equalsIgnoreCase("STARTER") 
					&&
				getSa().getAtribute(SearchAttributes.SEARCH_ORIGIN).toUpperCase().startsWith("STEWARTORDERS")){
				checkDocumentList(docManager.getDocumentsWithDataSource(false, "PF"));
			}
		}catch(Exception e){e.printStackTrace();}
	}
	
	/** Apply QA rules for per community*/
	//task 8097
	private void applyCommunityParticularyQARules() {
		int community = this.getCommId();
		
		int ndexId = ro.cst.tsearch.ServerConfig.getNdexCommunityId();
		
		if(community == ndexId){
			//apply QA for NDEX see 8097
			applyCommunityParticularyQARulesforNDEX();
		}
		
	}
	
	private void applyCommunityParticularyQARulesforNDEX(){
		List<DocumentI> courtDocs = docManager.getDocumentsWithDocType("COURT");
		
		uncheckDocumentList(courtDocs, true);
	}
	
	private void applyParticularyQARules() {
		
		int stateId = Integer.parseInt(getStateId());
		int countyId = Integer.parseInt(getCountyId());
		
		applyParticularyQARulesForStewaertOrders();
		
		applyQARulesForDaslDocuments();
		
		switch (stateId) {
		case StateContants.CA:
			applyParticularyQARulesForCalifornia();
			break;
			
		case StateContants.CO:
			applyParticularyQARulesForCO();
			break;
			
		case StateContants.FL:
			applyParticularyQARulesForFL();
			break;
			
		case StateContants.IL:
			
			switch (countyId) {
			case CountyConstants.IL_Cook:
			case CountyConstants.IL_DeKalb:
			case CountyConstants.IL_Kendall:
			case CountyConstants.IL_Lake:
			case CountyConstants.IL_McHenry:
			case CountyConstants.IL_Will:
				applyParticularyQARulesForCook();
				break;

			default:
				break;
			}
			
			
			break;
			
		case StateContants.IN:
			applyParticularyQARulesForIN();
			break;
			
		case StateContants.KS:
			if(countyId == CountyConstants.KS_Johnson) {
				applyParticularyQARulesForKSJohnson();
			}
			break;
		case StateContants.MO:
			if(countyId == CountyConstants.MO_Clay) {
				applyParticularyQARulesForMOClay();
			}
			break;
			
		case StateContants.OH:
			if (countyId ==  CountyConstants.OH_Franklin){
				applyParticularyQARulesForOH();
			}
			break;
		case StateContants.TN:
			applyParticularyQARulesForTN();
			break;
			
		case StateContants.TX:
//			if(SPECIFIC_TEXAS_COUNTIES.contains(county.toLowerCase())){
				applyParticularyQARulesForTXSpecificConties();
//			}
			
		default:
			break;
		}

		
	}

	/**
	 * Implemented tasks:<br>
	 * <b>10087</b>
	 */
	private void applyParticularyQARulesForKSJohnson() {
		/*
		//stupid task 10087	KS Johnson TR- transpose value of tag TOTAL_ASSESSMENT from AO to TR 
		List<DocumentI> trDocs = docManager.getDocumentsWithSiteType(false, new Integer[] {GWTDataSite.TR_TYPE});
		List<DocumentI> aoDocs = docManager.getDocumentsWithSiteType(false, new Integer[] {GWTDataSite.AO_TYPE});
		
		for (DocumentI trDocumentI : trDocs) {
			if(trDocumentI instanceof TaxDocumentI) {
				TaxDocumentI taxDocumentI = (TaxDocumentI)trDocumentI;
				for (DocumentI aoDocumentI : aoDocs) {
					if(trDocumentI.getYear() == aoDocumentI.getYear() 
							&& trDocumentI.getInstno().equals(aoDocumentI.getInstno())
							&& taxDocumentI.getTotalAssessment() == Double.MIN_VALUE
							&& aoDocumentI instanceof AssessorDocumentI) {
						taxDocumentI.setTotalAssessment(((AssessorDocumentI)aoDocumentI).getTotalAssessement());
					}
				}
			}
		}
		*/
		
	}

	/**
	 * Does specific DASL rules<br>
	 * <b>1.</b><br>
	 * Unchecks all Transfers found on a safe GI search<br>
	 * Safe means we know for sure it is a GI search and not a combined search
	 */
	private void applyQARulesForDaslDocuments() {
		for(RegisterDocumentI doc:docManager.getRoLikeDocumentList()){
			if(!doc.isManualChecked() &&
					doc.is(DocumentTypes.TRANSFER) 
					&& SearchType.GI.equals(doc.getSearchType())
					&& TSServerDASL.isSiteSafeOnGi((int)doc.getSiteId())){
				doc.setChecked(false);
				doc.setIncludeImage(false);
			}
		}
	}

	private void applyParticularyQARulesForTXSpecificConties() {
		List <RegisterDocumentI> documentstoBeUnchecked = new ArrayList<RegisterDocumentI>();
		documentstoBeUnchecked.addAll(docManager.getDocumentsWithSearchType(SearchType.GI));
		
		for(RegisterDocumentI doc:documentstoBeUnchecked){
			if(!doc.isManualChecked() && doc.is(DocumentTypes.TRANSFER)){
				doc.setChecked(false);
				doc.setIncludeImage(false);
			}
		}
		
		uncheckDocumentList(docManager.getDocuments(false, DocumentTypes.OTHERFILES, "GuaranteeFile"), false);
	}
	
	private void applyParticularyQARulesForIN() {
		uncheckDocumentList(docManager.getDocumentsWithDataSource(true, "PA","NB"), false);
	}
	
	private void applyParticularyQARulesForMOClay() {
		/*
		List<DocumentI> all =  docManager.getDocumentsWithDataSource(false, "RO","OR");
		
		uncheckDocumentList(all , true );
		for(DocumentI doc:all){
			if("TRANSFER".equalsIgnoreCase(doc.getDocType())&&doc.hasImage()){
				if(doc.getImage().isSaved()){
					doc.setIncludeImage(true);
				}
			}
		}
		*/
	}
	
	List<RegisterDocumentI> getDocumentsOlderThan(final String CATEGORY, final String SUBCATEGORY, final int YEARS, final int MONTHS, final int DAYS){
		List<RegisterDocumentI> ret = new ArrayList<RegisterDocumentI>();
		Calendar cal = Calendar.getInstance();
		cal.add(Calendar.YEAR, -YEARS);
		cal.add(Calendar.MONTH, -MONTHS);
		cal.add(Calendar.DAY_OF_YEAR, -DAYS);
		
		Date oldDate = cal.getTime();
		
		List<DocumentI> liens = docManager.getDocuments(CATEGORY, SUBCATEGORY);
		for(DocumentI doc:liens){
			if(doc instanceof RegisterDocumentI){
				RegisterDocumentI regI = (RegisterDocumentI) doc;
				Date recDate = regI.getRecordedDate();
				if( recDate.before(oldDate) ){
					ret.add(regI);
				}
			}
		}
		return ret;
	}
	
	private void applyParticularyQARulesForCO() {
		List<DocumentI> tobeUnchecked = new ArrayList<DocumentI>( getDocumentsOlderThan("LIEN", "Mechanic Lien", 0, 13, 0) ); //LIEN/Mechanic Lien - 13 months
		
		tobeUnchecked.addAll( getDocumentsOlderThan("LIEN", "Federal Tax Lien", 10, 0, 30) ); //LIEN/Federal Tax Lien - 10 years and 30 days
		tobeUnchecked.addAll( getDocumentsOlderThan("LIEN", "Lis Pendens", 6, 0, 30) ); //LIEN/Lis Pendens - 6 years and 30 days
		tobeUnchecked.addAll( getDocumentsOlderThan("COURT", "Judgment", 7, 0, 0) ); //BUG 7022 COURT/Judgment  - 7 years
		tobeUnchecked.addAll( getDocumentsOlderThan("COURT", "Bankruptcy", 0, 24, 0) );
		
		/*7230*/
		tobeUnchecked.addAll( docManager.getDocuments(false, DocumentTypes.AFFIDAVIT, "Affidavit"));
//		tobeUnchecked.addAll( docManager.getDocuments(false, DocumentTypes.LIEN, "Notice of Election and Demand")); //7373
		tobeUnchecked.addAll( docManager.getDocuments(false, DocumentTypes.MISCELLANEOUS, "Power of Attorney","Waiver"));
		tobeUnchecked.addAll( docManager.getDocuments(false, DocumentTypes.RELEASE, "Partial Release","Release of ASR/Leases","Release"));
		
		tobeUnchecked.addAll( docManager.getDocumentsWithServerDocType("TUBCARD"));
		tobeUnchecked.addAll(docManager.getDocumentsWithServerDocType("RET"));
		tobeUnchecked.addAll(docManager.getDocumentsWithServerDocType("REALESTATETRANSFERTAX"));
		tobeUnchecked.addAll(docManager.getDocumentsWithServerDocType("RESULTSHEET"));
		
		uncheckDocumentList(tobeUnchecked, false);
	}
	
	private void applyParticularyQARulesForFL() {
		List<TransferI> allTransfers = docManager.getTransferList();
		TransferI lastRealTransfer = docManager.getLastRealTransfer();
		
		for(TransferI t:allTransfers){ //CR 5723
			if( !t.isManualChecked()  && !t.equals(lastRealTransfer) && t.before(lastRealTransfer) ){
				t.setChecked( false );
				if(!t.isManualIncludeImage()) {
					t.setIncludeImage( false );
				}
			}
		}
		
		{ //Notice of Commencement older than 24 months
			Calendar cal = Calendar.getInstance();
			cal.add(Calendar.MONTH, -24);
			Date month24OldDate = cal.getTime();
			
			List<DocumentI> notices = docManager.getDocuments("LIEN","Notice of Commencement");
			for(DocumentI doc:notices){
				if(doc instanceof RegisterDocumentI){
					RegisterDocumentI regI = (RegisterDocumentI) doc;
					Date recDate = regI.getRecordedDate();
					
					if( recDate!=null && regI.isSavedFrom(SavedFromType.AUTOMATIC) && recDate.before(month24OldDate) ){ //CR 5723
						docManager.remove(regI);
						SearchLogger.info("<br/><b>Document "+regI.prettyPrint()+ " was removed from TSRIndex because it's a Notice of Commencement older than 24 months</b>", ID);
					}
					
				}
			}
		}
		
		{ //docs older than 20 years
			Calendar cal = Calendar.getInstance();
			cal.add(Calendar.YEAR, -20);
			Date year20OldDate = cal.getTime();
			
			List<DocumentI> notices = docManager.getDocuments("COURT","Judgment");
			notices.addAll(docManager.getDocuments("COURT","Certified Judgment"));
			//notices.addAll(docManager.getDocuments("COURT","Court"));
			//notices.addAll(docManager.getDocuments("LIEN","Federal Tax Lien"));
			
			for(DocumentI doc:notices){
				if(doc instanceof RegisterDocumentI){
					RegisterDocumentI regI = (RegisterDocumentI) doc;
					Date recDate = regI.getRecordedDate();
					
					if( recDate != null && regI.isSavedFrom(SavedFromType.AUTOMATIC) && recDate.before(year20OldDate) ){
						docManager.remove(regI);
						SearchLogger.info("<br/><b>Document "+regI.prettyPrint()+ " was removed from TSRIndex because it's a "+ regI.getDocType()+"/"+regI.getDocSubType() +" older than 20 years</b>", ID);
					}
					
				}
			}
		}
		
		for(DocumentI rel:docManager.getDocumentsWithDocType("RELEASE","MODIFICATION","SUBORDINATION","APPOINTMENT", "ASSIGNMENT","SUBSTITUTION","ASSUMPTION","RQNOTICE")){
			
			if(! (rel instanceof RegisterDocumentI) ){
				continue;
			}
			
			RegisterDocumentI realRel = (RegisterDocumentI)rel;
			
			Set<RegisterDocumentI> refs = rel.getReferences();
			boolean atLeastOneGoodTypeReleased = false;
			List<RegisterDocumentI> allGood = new ArrayList<RegisterDocumentI>(3);
			for(RegisterDocumentI regDoc:refs){
				if(regDoc.isOneOf("MORTGAGE","LIEN","COURT","CORPORATION","MODIFICATION", "SUBORDINATION", "MISCELLANEOUS", "APPOINTMENT", "ASSIGNMENT","SUBSTITUTION","ASSUMPTION","RQNOTICE","LEASE")){
					allGood.add(regDoc);
				}
			}
			if(allGood.size()==1){
				if("DT".equalsIgnoreCase(rel.getDataSource())){
					if(rel.isOneOf("RELEASE","ASSIGNMENT")){
						if(realRel.getGrantor().size()==0){
							rel.setGrantorWithLimit(allGood.get(0).getGranteeFreeForm());
						}
						if(!rel.isOneOf("ASSIGNMENT")){
							if(realRel.getGrantee().size()==0){
								rel.setGranteeWithLimit(allGood.get(0).getGrantorFreeForm());
							}
						}
					}
					if(rel.isOneOf("MODIFICATION")){
						if(realRel.getGrantor().size()==0){
							rel.setGrantorWithLimit(allGood.get(0).getGrantorFreeForm());
						}
						if(realRel.getGrantee().size()==0){
							rel.setGranteeWithLimit(allGood.get(0).getGranteeFreeForm());
						}
					}
				}
			}	
		}
	}
	
	private void applyParticularyQARulesForCook() {
		uncheckDocumentList( new ArrayList<DocumentI>(docManager.getDocumentsWithSearchType(SearchType.SG)), false); 
		
		List<RegisterDocumentI> allGi = docManager.getDocumentsWithSearchType(SearchType.GI); 
		SearchAttributes sa = getSa();
		String streetNo = sa.getAtribute(SearchAttributes.P_STREETNO);
		
		for(RegisterDocumentI d:allGi){
			if( !d.isChecked() ){
				continue;
			}
			
			Set<PropertyI> props = d.getProperties();
			for(PropertyI p:props ){
				if(p.hasAddress()){
					AddressI a = p.getAddress();
					String no = a.getNumber();
					if(!d.isManualChecked() &&((StringUtils.isEmpty(no) || StringUtils.isEmpty(streetNo))) ){
						d.setChecked(false);
						if(d.hasImage()){
							d.setIncludeImage(false);
						}
					}
					if( !d.isManualChecked() && GenericLegal.computeScoreInternal("", no, streetNo, true, false)<0.75 ){
						d.setChecked(false);
						if(d.hasImage()){
							d.setIncludeImage(false);
						}
					}
				}
				else if(!d.isManualChecked()){
					d.setChecked(false);
					if(d.hasImage()){
						d.setIncludeImage(false);
					}
				}
			}
			if(props.size() == 0) {
				if(!d.isManualChecked()){
					d.setChecked(false);
					if(d.hasImage()){
						d.setIncludeImage(false);
					}
				}
			}
		}
	}
	
	private void applyParticularyQARulesForOH() {
		
		List docsToBeUncheked = new ArrayList<DocumentI>();
		docsToBeUncheked.addAll(docManager.getDocumentsWithDocType("RELEASE"));
		uncheckDocumentList(docsToBeUncheked, false);
	}
	
	private void applyParticularyQARulesForTN() {
		List imageToBeUncheked = new ArrayList<DocumentI>();
		//Task 4133 - Copies to be unchecked so the searcher can check to attach if they choose instead of always having to remember to uncheck
		imageToBeUncheked.addAll(docManager.getDocumentsWithDocType("RESTRICTION","MASTERDEED","BY-LAWS","MASTERDEEDANDBY-LAWS"));
		imageToBeUncheked.addAll(docManager.getDocuments("MISCELLANEOUS", "Charter"));
		uncheckDocumentList( imageToBeUncheked, true);
		
		List docsToBeUncheked = new ArrayList<DocumentI>();
		//Task 4128 - (PICsup4128) CR TN -- Releases on the TSR Index should not be checked
		docsToBeUncheked.addAll(docManager.getDocumentsWithDocType("RELEASE"));
		uncheckDocumentList(docsToBeUncheked, false);
	}
	
	private void applyParticularyQARulesForCalifornia(){
		List <DocumentI> documentstoBeUnchecked = docManager.getDocumentsWithServerDocType("RQ","PR","PA","SR","SX","SY","PY","PC","PS","MH","BASE");
		
		documentstoBeUnchecked.addAll(docManager.getDocumentsWithSearchType(SearchType.GI));
		documentstoBeUnchecked.addAll(docManager.getDocumentsWithDocType("ASSESSOR"));
		if(isRefinance()){
			documentstoBeUnchecked.addAll(docManager.getDocumentsWithDocType("RESTRICTION","EASEMENT"));
			documentstoBeUnchecked.addAll(docManager.getDocumentsWithServerDocType("MP"));
			TransferI lasTransferI = docManager.getLastRealTransfer();
			if( lasTransferI!=null ){
				documentstoBeUnchecked.addAll(docManager.getDocumentsBefore(lasTransferI.getRecordedDate(),true));
			}
		}
		
		uncheckDocumentList(documentstoBeUnchecked, false);
		
		//B 3868 - a forclosure deed deactivate all the previous mortgages
		List<DocumentI> allForClosureDeeds = docManager.getDocumentsWithServerDocType("FD");
		if(allForClosureDeeds.size()>0){
			DocumentUtils.sortDocuments( allForClosureDeeds, MultilineElementsMap.DATE_ORDER_ASC );
			DocumentI docI = allForClosureDeeds.get(allForClosureDeeds.size()-1);
			if(docI instanceof TransferI){
				TransferI tr = (TransferI) docI;
				for(MortgageI m: docManager.getMortgageList(true)){
					if (m.before(tr)){
						uncheckDocsAndReferences(m, 0);
					}
				}
			}
		}
	}
	
	private void applyParticularyQARulesInitForFLSarasota() {
		
		List<DocumentI> allDatatraceDocs = docManager.getDocumentsWithDataSource(false, "DT");
		List<InstrumentI> allReferences = new ArrayList<InstrumentI>();
		
		try {
			for (DocumentI document : allDatatraceDocs) {
				allReferences.addAll(document.getParsedReferences());
			}
			
			for(InstrumentI ref : allReferences) {
				for (DocumentI document : allDatatraceDocs) {
					try {
						if(document.hasYear() && !document.getInstno().isEmpty() && document.getInstno().substring(4).replaceFirst("^0+", "").equals(ref.getInstno().replaceFirst("^0+", ""))) {
							ref.setInstno(document.getInstno());
							break;
						}
					}catch(Exception e) {
						e.printStackTrace();
					}
				}
			}
		}catch(Exception e) {
			e.printStackTrace();
		}
	}
	
  private void applyParticularyQARulesInitForTXCollin() {
		  
	  List<DocumentI> allACDocs = docManager.getDocumentsWithDataSource(false, "AC");
	  List<InstrumentI> allReferences = new ArrayList<InstrumentI>();
		
	  try {
		for (DocumentI document : allACDocs) {
			allReferences.addAll(document.getParsedReferences());
		}
		for(InstrumentI ref : allReferences) {
			for (DocumentI document : allACDocs) {
				try {
					if(document.hasYear() && !document.getInstno().isEmpty() 
							&& ((document.getInstno().equals(ref.getBook()) && ref.getPage().equalsIgnoreCase(String.valueOf(document.getYear())))
									/*||(document.getInstno().equals(ref.getPage()) && ref.getBook().equalsIgnoreCase(String.valueOf(document.getYear()))) */ 
							    )          
						) {
						ref.setInstno(document.getInstno());
						ref.setBook("");
						ref.setPage("");
						break;
					}
				}catch(Exception e) {
					e.printStackTrace();
				}
			}
			}
		}catch(Exception e) {
			e.printStackTrace();
		}
	}
	
	private static void uncheckDocumentList(List<DocumentI> documentstoBeUnchecked, boolean onlyImages){
		if(onlyImages){
			for(DocumentI d:documentstoBeUnchecked){
				if(!d.isManualIncludeImage()){
					d.setIncludeImage(false);
				}
			}
		}
		else{
			for(DocumentI d:documentstoBeUnchecked){
				if(!d.isManualChecked()){
					d.setChecked( false );
				}
				if(!d.isManualIncludeImage()) {
					d.setIncludeImage(false);
				}
			}
		}
	}
	
	private static void checkDocumentList(List<DocumentI> documentstoBechecked){
		for(DocumentI d:documentstoBechecked){
			if(!d.isManualChecked()){
				d.setChecked( true );
			}
			if(!d.isManualIncludeImage()) {
				d.setIncludeImage(true);
			}
		}
	}
	
	public String getImagePath(){
		if(imagePath == null) {
			SimpleDateFormat format = new SimpleDateFormat("yyyy_MM_dd");
			imagePath = 
				ServerConfig.getImageDirectory() + File.separator + 
				format.format(getStartDate()) + File.separator + 
				getID() + File.separator;
		}
		return imagePath;
	}
	
	public void resetImagePath(){
		imagePath = null;
	}
	
	/**
	 * Also creates the folder
	 * @return image_folder + start_date_as_yyyy_MM_dd + search_id
	 */
	public String getImageDirectory(){
		SimpleDateFormat format = new SimpleDateFormat("yyyy_MM_dd");
		Date sdate = getStartDate();
		String basePath = ServerConfig.getImageDirectory()+File.separator+format.format(sdate)+File.separator+ID;
		FileUtils.CreateOutputDir(basePath);
		return basePath;
	}
	
	
	public boolean addDocument(String chapterKey, ParsedResponse pr, String htmlContent) {
		
        try{
        	docManager.getAccess();
        	DocumentI doc = pr.getDocument() ;
        	
        	if ( doc!=null ){
        		if( docManager.contains(doc) ){
        			return false;
        		}
            	doc.setChecked(true);
            	doc.setIncludeImage(true);
            	docManager.add( doc );
            	doc.setIndexId( DBManager.addDocumentIndex(Tidy.tidyParse(htmlContent, null), this ) );
            	
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
            	//calculateAndSetDescription(doc);
            	doc.setDescription( doc.updateDescription() );
            	TSServer.calculateAndSetFreeForm(doc,PType.GRANTOR, ID);
            	TSServer.calculateAndSetFreeForm(doc,PType.GRANTEE, ID);
            	
            	if (doc instanceof RegisterDocumentI) {
					RegisterDocumentI regDoc = (RegisterDocumentI) doc;
					if("*".equals(regDoc.getGranteeFreeForm()) && !StringUtils.isEmpty(regDoc.getGrantee().getFreeParsedForm())){
						regDoc.setGranteeWithLimit(regDoc.getGrantee().getFreeParsedForm());
	            	}
					if("*".equals(regDoc.getGrantorFreeForm()) && !StringUtils.isEmpty(regDoc.getGrantor().getFreeParsedForm())){
						regDoc.setGrantorWithLimit(regDoc.getGrantor().getFreeParsedForm());
	            	}
				}
            	
            	if(hasImage){
            		SimpleDateFormat format = new SimpleDateFormat("yyyy_MM_dd");
            		Date sdate = getStartDate();
            		
            		String extension = "";
            		String oldImageFileName = pr.getImageLink(0).getImageFileName();
            		int poz = oldImageFileName .lastIndexOf(".");
            		if(poz>0 && (poz+1 < oldImageFileName.length()) ){
            			extension = oldImageFileName.substring(poz+1);
            		}
            		if(extension.equalsIgnoreCase("tif")){
            			extension = "tiff";
            		}
            		
            		String basePath = ServerConfig.getImageDirectory() +
            				File.separator + format.format(sdate) + 
            				File.separator + getID();
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
	            	image.setOcrInProgress(false);
	            	image.setPlanedForOCR(false);
	            	image.setSaved(false);
	            	image.setUploaded(false);
	            	doc.setImage(image);
            	}
        	}
        	else{
            	if (chapterKey.contains("Other") && chapterKey.contains("patriots")) { //nu s-a facut parsare pt PA trebuie sa facem
            		String grantor ="";
            		String grantee ="Patriots ACT"; 
            		try {
        				grantor = getPatriotSearchName();
        			}catch( Exception e ){ }
            		

            		InstrumentI instr = new com.stewart.ats.base.document.Instrument();
            		instr.setDocType("PATRIOTS");
            		instr.setDocSubType("PATRIOTS");
            		instr.setInstno(getPatriotKeyNumber());
            		instr.setYear( Calendar.getInstance().get(Calendar.YEAR) );
            		
            		Patriots patDoc = new Patriots(DocumentsManager.generateDocumentUniqueId(getID(), instr)) ;
            		patDoc.setInstrument(instr);
            		
            		PartyI grantee1 = new Party(SimpleChapterUtils.PType.GRANTEE);
            		grantee1.add(new Name("","",grantee));
            		PartyI grantor1 = new Party(SimpleChapterUtils.PType.GRANTOR);
            		grantor1 .add(new Name("","",grantor));
            		patDoc.setGrantee(grantee1);
            		patDoc.setGrantor(grantor1);
            		patDoc.setServerDocType("PATRIOTS");
            		patDoc.setType(DType.ROLIKE);
            		patDoc.setDataSource("PA");
            		Date instrumentDate  = null;
            		try {
            			instrumentDate = Util.dateParser3(pr.getResponse());
            			if(instrumentDate == null ) {
            				instrumentDate = DBManager.getStartDate(getID());
            				Calendar cal = Calendar.getInstance();
            				cal.setTime(instrumentDate);
            				cal.add(Calendar.HOUR, -6);
            				instrumentDate = cal.getTime();
            			}
            		} catch (Exception e) {
						e.printStackTrace();
					}
            		
            		patDoc.setInstrumentDate(instrumentDate);
            		
            		if(getSa().getAtribute(SearchAttributes.P_STATE).equals("5")) {
            			patDoc.setChecked(false);		//b3328
            			patDoc.setIncludeImage(false);
            		}
            		else {
            			patDoc.setChecked(true);
            			patDoc.setIncludeImage(true);
            		}
            		
            		int miServerID = (int)TSServersFactory.getSiteIdfromCountyandServerTypeId(
            				Integer.parseInt(getSa().getAtribute(SearchAttributes.P_COUNTY)), 
            				TSServersFactory.getSiteTypeId("PA"));
            		
            		patDoc.setSiteId(miServerID);
            		
            		patDoc.setIndexId(DBManager.addDocumentIndex(Tidy.tidyParse(htmlContent, null), this));
                	//calculateAndSetDescription(patDoc);
            		patDoc.setDescription(patDoc.updateDescription());
                	TSServer.calculateAndSetFreeForm(patDoc,PType.GRANTOR, getID());
                	TSServer.calculateAndSetFreeForm(patDoc,PType.GRANTEE, getID());
                	
                	if(isAlertForPatriotsChapter(patDoc.getInstno())){
                		patDoc.setHit(true);
                	}
                	
                	Set<String> list = new HashSet<String>();
                	boolean hasImage = false;
                	try{
                		int count = pr.getImageLinksCount();
                		for(int j=0;j<count;j++){
                			list.add(pr.getImageLink(j).getLink());
                			hasImage  = true;
                		}
                	}catch(Exception e){}
                	
                	docManager.add( patDoc );
                	
                	if(hasImage){
                		SimpleDateFormat format = new SimpleDateFormat("yyyy_MM_dd");
                		Date sdate = getStartDate();
                		
                		String extension = "";
                		String oldImageFileName = pr.getImageLink(0).getImageFileName();
                		int poz = oldImageFileName .lastIndexOf(".");
                		if(poz>0 && (poz+1 < oldImageFileName.length()) ){
                			extension = oldImageFileName.substring(poz+1);
                		}
                		if(extension.equalsIgnoreCase("tif")){
                			extension = "tiff";
                		}
                		
                		String basePath = ServerConfig.getImageDirectory() + 
	                			File.separator + 
	                			format.format(sdate) + 
	                			File.separator + 
	                			getID();
                		
                		File file= new File(basePath);
                		if(!file.exists()){
                			file.mkdirs();
                		}
                		
                    	String fileName = patDoc.getId()+"."+extension;
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
    	            	patDoc.setImage(image);
    	            	patDoc.setIncludeImage(true);
                	}
            	}
        	} 
      }
        catch(Exception e){  e.printStackTrace(); }
        finally{
        	docManager.releaseAccess();;
        }
   
		
		return true;
		
	}
	
	public void addImagesToDocument(DocumentI document, String ... imageLinks) {
		
		if(imageLinks == null || imageLinks.length == 0 || document == null) {
			return;
		}
		
		SimpleDateFormat format = new SimpleDateFormat("yyyy_MM_dd");
		Date sdate = getStartDate();
		String oldImageFileName = null;
		Set<String> list = new HashSet<String>();
		for (String imageLink : imageLinks) {
			if(StringUtils.isNotEmpty(imageLink)) {
				list.add(imageLink);
				if(oldImageFileName == null) {
					oldImageFileName = imageLink;
				}
			}
		}
		
		String extension = "";
		
		int poz = oldImageFileName .lastIndexOf(".");
		if(poz>0 && (poz+1 < oldImageFileName.length()) ){
			extension = oldImageFileName.substring(poz+1);
		}
		if(extension.equalsIgnoreCase("tif")){
			extension = "tiff";
		}
		
		String basePath = ServerConfig.getImageDirectory() + 
			File.separator + FormatDate.getDateFormat(FormatDate.PATTERN_yyyy_MM_dd).format(sdate) +
			File.separator + getID();
		File file= new File(basePath);
		if(!file.exists()){
			file.mkdirs();
		}
		
		String fileName = document.getId() + "." + extension;
    	String path 	= basePath+File.separator + fileName;
		
		
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
    	document.setImage(image);
    	document.setIncludeImage(true);
	}
	
	public static String notNull(String s) {
		return (s != null) ? s.trim() : "";
	}
	private static String formatHTML(String s) {
		s = notNull(s);
		if (!"".equals(s)) {
			s = s.replaceAll("<br/?>", "@@BR@@");
			s = s.replaceAll("<span([^>]*)>", "@@SPAN1@@$1@@SPAN2@@");
			s = s.replaceAll("</span>", "@@SPAN3@@");
			s = s.replace("&nbsp;", " ");
			s = s.replace("&", "&amp;");
			s = s.replace("<", "&lt;");
			s = s.replace(">", "&gt;");
			s = s.replaceAll("\\s+", " ");
			s = s.replace("@@BR@@", "<br>");
			s = s.replace("@@SPAN1@@", "<span");
			s = s.replace("@@SPAN2@@", ">");
			s = s.replace("@@SPAN3@@", "</span>");
		} else {
			s = "&nbsp;";
		}
		return s;
	}
	
	public String getAsHTML() {
		
		DocumentsManagerI documentsManager= getDocManager();
		StringBuilder sb = new StringBuilder();
		GBManager gbm=(GBManager)getSa().getObjectAtribute(SearchAttributes.GB_MANAGER_OBJECT);
		SimpleDateFormat sdf = new SimpleDateFormat("MM/dd/yyyy");
		// page header
		sb.append("<table border=\"1\" cellspacing=\"0\">\n");

		// table header row
		sb.append("<tr bgcolor=\"lightblue\">\n");
		sb.append("<td width=\"1%\">&nbsp;</td>\n");
		sb.append("<td width=\"1%\">DS</td>\n");
		sb.append("<td width=\"20%\">DESCRIPTION</td>\n");
		sb.append("<td width=\"7%\">DATE</td>\n");
		sb.append("<td width=\"17%\">GRANTOR</td>\n");
		sb.append("<td width=\"17%\">GRANTEE</td>\n");
		sb.append("<td width=\"14%\">INSTRUMENT TYPE</td>\n");
		sb.append("<td nowrap=\"nowrap\" width=\"7%\">INSTRUMENT</td>\n");
		sb.append("<td width=\"16%\">REMARKS</td>\n");
		sb.append("</tr>\n");
		try{
			documentsManager.getAccess();
			// iterate through all chapters
			List<DocumentI> docs = documentsManager.getDocumentsList();
			
			/* B 4757Sort the documents like on TSRIndex*/
			List<SimpleChapter> sortedChapters = documentsManager.getTsrIndexOrder();
			List<String> idList = new ArrayList<String>();
			for (SimpleChapter simpleChapter : sortedChapters) {
				idList.add(simpleChapter.getDocumentId());
			}
			DocumentUtils.sortDocuments(docs, MultilineElementsMap.TSR_INDEX_ORDER, idList);
			
			Map<String,String> colors = new HashMap<String,String>();
//			List<SimpleChapter> newChapters = new ArrayList<SimpleChapter>();
//			for (DocumentI elem : docs) {
//				SimpleChapter simpleChapter = elem.getSimpleChapterWithoutReferences();
//				newChapters.add(simpleChapter);
//			}
			for (DocumentI elem : docs) {	//add references
				SimpleChapter c = findChapter(elem.getId(), sortedChapters);
				if (c!=null) {
					Set<RegisterDocumentI> references = elem.getReferences();
					for (RegisterDocumentI ref: references) {
						SimpleChapter r = findChapter(ref.getId(), sortedChapters);
						if (r!=null) {
							c.addReference(r);
						}
					}
				}
			}
			
//			List<SimpleChapter> sortedColoredChapters = RecordedDateSort.sortByRecordedDateAndColor(newChapters, true);
//			
//			for (SimpleChapter simpleChapter : newChapters) {
//				applyColorForChapter(simpleChapter, sortedColoredChapters);
//			}
			for (SimpleChapter simpleChapter : sortedChapters) {
				colors.put(simpleChapter.getDocumentId(), simpleChapter.getColorClass());
			}
			
			for (DocumentI elem : docs) {
	
				// skip the TSD
				String description = notNull(elem.getDescription());
				
				// see Document -> updateDescription()
				if((elem.getType() == DType.ROLIKE) && !"PA".equalsIgnoreCase(elem.getDataSource())){
	    			description = elem.getServerDocType() + " " + description;
	    		}
				
				String srcType =elem.getDataSource();
	           
				String color = getTsrIndexColor(colors.get(elem.getId()));
				if (!StringUtils.isEmpty(color)) {
					sb.append("<tr bgcolor=\"" + color + "\">\n");
				} else {
					sb.append("<tr>\n");
				}
				String isOcred = "";
				ImageI image = elem.getImage();
				String link = "";
				if (image!=null) {
					link = image.getSsfLink();
					if (link!=null && link.length()!=0) {
						link = "<a href=\"" + link + "\">I</a>";
					}
					if (image.isOcrDone()){
						isOcred = "<br>Ocred";
					}
				}
				
				// checked
				boolean checked = elem.isChecked();
				
				sb.append("<td align=\"center\"><input type=checkbox ");
				if (checked) {
					sb.append("checked");
				} 
				sb.append(" disabled>").append(link).append("</td>\n");
				
				String searchType = elem.getSearchType().toString().toLowerCase();
				
				if(elem.isUploaded())
					searchType = "";
				if(org.apache.commons.lang.StringUtils.isNotBlank(elem.getExternalSourceType())) {
					searchType += "(" + elem.getExternalSourceType() + ")";
				}
				sb.append("<td>" + formatHTML(srcType) + "<br>" + formatHTML(searchType) + isOcred + "</td>\n");
				sb.append("<td>" + formatHTML(description) + "</td>\n");
	
				// date
				String date = "";
				if(elem instanceof RegisterDocumentI) {
					if(((RegisterDocumentI)elem).getRecordedDate() != null) {
						try {
							String filled = notNull(sdf.format(((RegisterDocumentI)elem).getRecordedDate()));
							if (!"".equals(filled)) 
								date=filled;
						} catch (Exception e) {
							e.printStackTrace();
						}
					}
					
			 	} else { 
			 		date=""+elem.getYear();
			 	}
				sb.append("<td>").append(formatHTML(date)).append("</td>\n");
	
				// grantor
				String grantor = notNull(elem.getGrantorFreeForm());
				sb.append("<td>").append(formatHTML(grantor)).append("</td>\n");
	
				String grantee = DocumentUtils.getGranteeFromDocument(elem);
				sb.append("<td>").append(formatHTML(grantee)).append("</td>\n");
	
				// instrument type
				String instrumentType = notNull(elem.getDocType());
				String serverInstrumentType = notNull(elem.getDocSubType());	//3810
				if (!"".equals(serverInstrumentType)) {
					instrumentType = instrumentType + "<br>("
							+ serverInstrumentType + ")";
				}
				sb.append("<td>").append(formatHTML(instrumentType)).append("</td>\n");
	
				// instrument
				String bookType = notNull(elem.getBookType()); 
				String book = notNull(elem.getBook());
				String page = notNull(elem.getPage());
				book = !"0".equals(book) ? book : "";
				page = !"0".equals(page) ? page : "";
				String bookPage = (!"".equals(book) || !"".equals(page)) ? book
						+ "-" + page : "";
				bookPage = !"".equals(bookPage) ? bookType + "(" + bookPage + ")" : bookPage;
				String instrument = notNull(elem.getInstno());
				if ("".equals(instrument)) {
					instrument = bookPage;
				} else {
					if (!"".equals(book) && !"".equals(page)
							&& !instrument.equals(bookPage)) {
						instrument = instrument + "<br>" + bookPage;
					}
				}
				DType type = elem.getType();
				if (!org.apache.commons.lang.StringUtils.isBlank(elem.getDocno()) && type != DType.ASSESOR && type != DType.TAX && type != DType.CITYTAX) {
					instrument += "<br>" + elem.getDocno();
				}
				sb.append("<td>").append(formatHTML(instrument)).append("</td>\n");
	
				// remarks
				String remarks = null;
				
				//first codes
				String codes = null;
				
				if(checked) {
					for (String code : elem.getCodeBookCodes()) {
						BoilerPlateObject boilerPlateObject = elem.getCodeBookObject(code);
						if(!boilerPlateObject.isManuallyDeleted()) {
							if(codes == null) {
								codes = "Codes:";
							}
							codes += "<br>";
							if(boilerPlateObject.isManuallyAdded()) {
								if(boilerPlateObject.hasModifiedStatement()) {
									codes += "<span style=\"font-style: italic;	font-weight:bold;\">" + code + "</span>";
								} else {
									codes += "<span style=\"font-style: italic;\">" + code + "</span>";
								}
							} else {
								if(boilerPlateObject.hasModifiedStatement()) {
									codes += "<span style=\"font-weight:bold;\">" + code + "</span>";
								} else {
									codes += code;
								}
							}
							
						}
					}
				}
				
				if(codes != null) {
					remarks = codes;
				}
				//second referneces
				String references = getRemarks(elem, sortedChapters);
				if(org.apache.commons.lang.StringUtils.isNotBlank(references)) {
					if(remarks != null) {
						remarks += "<br>References:<br>" + references;
					} else {
						remarks = "References:<br>" + references;
					}
				}
				//third notes
				if(org.apache.commons.lang.StringUtils.isNotBlank(elem.getNote())) {
					String note = elem.getNote().replaceAll("[\\r\\n]", "<br>");
					if(remarks != null) {
						remarks += "<br>Note:<br>" + formatHTML(note);
					} else {
						remarks = "Note:<br>" + formatHTML(note);
					}
				}
				
								
				sb.append("<td>").append(formatHTML(org.apache.commons.lang.StringUtils.defaultString(remarks))).append("</td>\n");
				sb.append("</tr>\n");
			}
		}
		finally{
		  documentsManager.releaseAccess();
	    }
		return sb.toString();
	}
	
	public static SimpleChapter findChapter(String id, List<SimpleChapter> newChapters) {
		for (SimpleChapter c: newChapters) {
			if (id.equals(c.getDocumentId())) {
				return c;
			}
		}
		return null;
	}

	/*
	 * if you modify this function, modify also com.stewart.ats.tsrindex.client.ChaptersTable.applyColorForChapter(SimpleChapter, ChapterAditional, String) 
	 */
	public static void applyColorForChapter(SimpleChapter model, List<SimpleChapter> sortedChapters) {
		String color = "";
		
		if(org.apache.commons.lang.StringUtils.isBlank(model.getColorClass()) || "gwt-default-document".equals(model.getColorClass())) {
		
			if ("TRANSFER".equals(model.getDocType())){
				if ("gwt-goback-transfer".equals(model.getColorClass())) {
					color = "gwt-goback-transfer";
				} else {
					color = "gwt-transfer";
				}
			} else if ("MORTGAGE".equals(model.getDocType())) {
				color = "gwt-mortgage";
			} else if ("OTHER-FILE".equals(model.getDocType())) {
				color = "gwt-other-file";
			}else if ("LIEN".equals(model.getDocType())) {
				if (UtilsAtsGwt.LIS_PENDENS_SUBCATEGORIES.contains(model.getDocSubType())) {
					color = "gwt-lispendens";
				}
				else {
					color = "gwt-lien";
				}
			} else if ("COURT".equals(model.getDocType())) {
				if (UtilsAtsGwt.DIVORCE_SUBCATEGORIES.contains(model.getDocSubType())) {
					color = "gwt-divorce";
				} else {
					color = "gwt-court";
				}
			}  else if ("COUNTYTAX".equals(model.getDocType()) && !"".equals(model.getColorClass())){
						color = model.getColorClass();
			} else {
				color = "gwt-default-document";
				
				int indexOf = sortedChapters.indexOf(model);
				if(indexOf >=0 ) {
					SimpleChapter coloredChapter = sortedChapters.get(indexOf);
					if(!coloredChapter.getColorClass().isEmpty()) {
						color = coloredChapter.getColorClass();	
					}
				}
				
			}
			
			model.setColorClass(color);
		}
	}
	
	public String getTsrIndexColor(String tsrIndexColorClass) {
		
		if (StringUtils.isEmpty(tsrIndexColorClass)) {
			return "";
		}
		
		if (tsrIndexColorClass.equals("gwt-default-document")) {
			return "";
		} else if (tsrIndexColorClass.equals("gwt-divorce")) {
			return "#E6A9EC";
		} else if (tsrIndexColorClass.equals("gwt-court")) {
			return "#FFCCCC";
		} else if (tsrIndexColorClass.equals("gwt-lispendens")) {
			return "#C3FDB8";
		} else if (tsrIndexColorClass.equals("gwt-lien")) {
			return "#FFCCCC";
		} else if (tsrIndexColorClass.equals("gwt-mortgage")) {
			return "#FFFFCC";
		} else if (tsrIndexColorClass.equals("gwt-transfer")) {
			return "#BDEDFF";
		} else if (tsrIndexColorClass.equals("gwt-goback-transfer")) {
			return "#6699EE";
		} else if (tsrIndexColorClass.equals("gwt-patriots-hit")) {
			return "#FF0000";
		} else if (tsrIndexColorClass.equals("gwt-other-file")) {
			return "#FFBF9E";
		} else if (tsrIndexColorClass.equals("gwt-tax-bor-cauv-status")) {
			return "#DFCFCF";
		}
		
		return "";
	}
	
	/*
	 * if you modify this function, modify also com.stewart.ats.tsrindex.client.ChaptersTable.isSavedInTsri(InstrumentG)
	 */
	private boolean isSavedInTsri(InstrumentG i, List<SimpleChapter> chapters){
		
		if(i instanceof SKLDInstrumentG) {
			for(SimpleChapter model:chapters){
				if(i.flexibleEquals(model.getInstrument())) {
					return true;
				}
			}
		} else {
		
			String instNo = i.getInstno();
			if(instNo!=null && instNo.length()>0){
				if(flexibleContainsInstrumentNo(instNo, chapters)){
					return true;
				}
			}
			
			String book = i.getBook();
			String page = i.getPage();
			
			if(book!=null && book.length()>0 && page!=null && page.length()>0 ){
				if(flexibleContainsBookPage(book, page, chapters)){
					return true;
				}
			}
		}
		
		return false;
	}
	
	/*
	 * if you modify this function, modify also com.stewart.ats.tsrindex.client.ChaptersTable.flexibleContainsInstrumentNo(String)
	 */
	public boolean flexibleContainsInstrumentNo(String instrNo, List<SimpleChapter> chapters) {
		if(instrNo==null){
			return false;
		}
		instrNo = instrNo.replaceAll("^0+", "");
		for(SimpleChapter model:chapters){
			if(model.hasInstrumentNo()){
				String tempInst = model.getInstno();
				if(instrNo.equalsIgnoreCase(tempInst)){
					return  true;
				}
				if(tempInst.endsWith(instrNo)){
					String temp = tempInst.replaceAll("^"+model.getYear()+"_?", "").replaceAll("^0+", "");
					if(temp.equals(instrNo)){
						return true;
					} else {
						String yearAsString = model.getYear() + "";
						if(yearAsString.length() == 4) {
							yearAsString = yearAsString.substring(2);
						}
						temp = tempInst.replaceAll("^"+yearAsString, "").replaceAll("^0+", "");
						if(temp.equals(instrNo)){
							return true;
						} else{
							temp = tempInst.replaceAll("^[A-Z]-" + yearAsString + "-", "").replaceAll("^0+", "");
							if(temp.equals(instrNo)){
								return true;
							}
						}
					}
				}
				// Task 8023
				if(tempInst.matches("\\d{4}[A-Z]\\d+") && instrNo.matches("\\d+[A-Z]")) {
					if(tempInst.endsWith(instrNo.substring(0, instrNo.length() - 1))
							&& tempInst.charAt(4) == instrNo.charAt(instrNo.length() - 1)) {
						return true;
					}
				}
				
				String stateAbrev = getSa().getAtribute(SearchAttributes.P_STATE_ABREV);
				String countyName = getSa().getAtribute(SearchAttributes.P_COUNTY_NAME);
				
				//task 8104
				if ("MO".equals(stateAbrev) && "Clay".equals(countyName)) {
					if ( (instrNo.matches("[A-Z]\\d{5}") && tempInst.matches("\\d{6,7}")) ||
						 (instrNo.matches("\\d{6,7}") && tempInst.matches("[A-Z]\\d{5}")) ) {
							String newInstrNo = instrNo.substring(instrNo.length()-5);
							String newTempInst = tempInst.substring(tempInst.length()-5);
							if (newInstrNo.equals(newTempInst)) {
								return true;
							}
					}
				}
				
				//task 9027
				if ("OH".equals(stateAbrev) && "Franklin".equals(countyName)) {
					if ( (instrNo.matches("\\d{3,7}") && tempInst.matches("\\d{15}") && Util.isValidDate(tempInst.substring(0,8))) ||
						 (instrNo.matches("\\d{15}") && tempInst.matches("\\d{3,7}")) && Util.isValidDate(instrNo.substring(0,8))) {
							String newInstrNo = (instrNo.length() == 15) ? instrNo.substring(instrNo.length()-7).replaceAll("^0+", "") : instrNo.replaceAll("^0+", "");
							String newTempInst = (tempInst.length() == 15) ? tempInst.substring(tempInst.length()-7).replaceAll("^0+", "") : tempInst.replaceAll("^0+", "");
							if (newInstrNo.equals(newTempInst)) {
								return true;
							}
					}
				}
				
				if("TX".equals(stateAbrev)) {
					if("Caldwell".equals(countyName)) {
						if(instrNo.length() > 4) {
							String yearAsString = model.getYear() + "";
							if (yearAsString.length() == 4) {
								String firstPart = instrNo.substring(0, instrNo.length() - 4);
								String secondPart = instrNo.substring(instrNo.length() - 4);
								if(yearAsString.endsWith(firstPart) && model.getInstno().endsWith(secondPart)) {
									return true;
								}
								
							}
						}
					}
				}
			}
		}
		return false;
	}
	
	/*
	 * if you modify this function, modify also com.stewart.ats.tsrindex.client.ChaptersTable.flexibleContainsBookPage(String, String)
	 */
	public boolean flexibleContainsBookPage(String book, String page, List<SimpleChapter> chapters) {
		if(book==null||page==null){
			return false;
		}
		book = book.replace("*", "").replaceAll("^0+", "");
		page = page.replace("*", "").replaceAll("^0+", "");
		for(SimpleChapter model:chapters){
			if(model.hasBookPage()){
				String _book = model.getBook().replace("*", "").replaceAll("^0+", "");
				String _page = model.getPage().replace("*", "").replaceAll("^0+", "");
				if(book.equalsIgnoreCase(_book) && page.equalsIgnoreCase(_page)){
					return true;
				}
				// Task 8023
				try {
					if(_book.matches("[A-Z]" + book) && _page.equalsIgnoreCase(page)) {
						return true;
					}
				} catch (Exception e) {
					logger.error("flexibleContainsBookPage1 for searchId " + getID() + " and book-page " + book + "-" + page, e);
				}
				try {
					if(_book.matches("[A-Z]+" + book) && _page.equalsIgnoreCase(page)
							|| book.matches("[A-Z]+" + _book) && _page.equalsIgnoreCase(page)) {
						return true;
					}
				} catch (Exception e) {
					logger.error("flexibleContainsBookPage2 for searchId " + getID() + " and book-page " + book + "-" + page, e);
				}
			}
		}
		return false;
	}
	
	public String getRemarks(DocumentI elem, List<SimpleChapter> chapters) {
		StringBuilder ref = new StringBuilder();
		Set<InstrumentI> refs = elem.getParsedReferences();
		int i=0;
		for(InstrumentI in:refs){
			String str = in.prettyPrint(true);
			if (isSavedInTsri(in.getInstrumentG(), chapters)) {
				ref.append(str);
			} else {
				ref.append("<span style=\"color:red\">" + str + "</span>");
			}
			ref.append(" ");
			if(i==9){
				break;
			}
			i++;
		}
		if(refs.size() > i + 1) {
			ref.append("<br>Plus " + (refs.size() - i - 1) + " more");
		}
		return ref.toString().trim();
	}
	
	
	/**
	 * Increments the counter that keeps record of the number of OCR threads in progress for this search
	 * @return the updated number
	 */
	public int addOcrInProgress(){
		try {
			docManager.getAccess();
			return docManager.addOcrInProgress();
		} catch (Exception e) {
			e.printStackTrace();
			return -1;
		} finally {
			docManager.releaseAccess();
		}
	}
	
	/**
	 * Decrements the counter that keeps record of the number of OCR threads in progress for this search
	 * @return the updated number
	 */
	public int removeOcrInProgress(){
		try {
			docManager.getAccess();
			return docManager.deleteOcrInProgress();
		} catch (Exception e) {
			e.printStackTrace();
			return -1;
		} finally {
			docManager.releaseAccess();
		}
	}
	
	/**
	 * Checks the counter that keeps record of the number of OCR threads in progress for this search and returns true if no thread is alive
	 * @return
	 */
	public boolean isOcrInProgess(){
		try {
			docManager.getAccess();
			return docManager.getOcrInProgress() != 0;
		} catch (Exception e) {
			e.printStackTrace();
			return false;
		} finally {
			docManager.releaseAccess();
		}
	}
	
	/**
	 * This flag is used to know if a search is no logger used and must be removed from the InstanceManager
	 */
	private transient boolean requestClean = false;
	private String tsrLink = "";
	private transient String tsriParentLink = null;
	private String tsriLink = "";
	
	/**
	 * Checks the requestClean flag
	 * This flag is used to know if a search is no logger used and must be removed from the InstanceManager
	 * @return
	 */
	public boolean isRequestClean() {
		return requestClean;
	}
	/**
	 * Sets the requestClean flag
	 * This flag is used to know if a search is no logger used and must be removed from the InstanceManager
	 * @param requestClean
	 */
	public void setRequestClean(boolean requestClean) {
		this.requestClean = requestClean;
	}
	
	
	public boolean isRefinance(){
		
		int searchProduct = -1;
        try {
            searchProduct = Integer.parseInt(sa.getAtribute(SearchAttributes.SEARCH_PRODUCT));
        } catch (NumberFormatException e1) {
            e1.printStackTrace();
        }
        
        if ( searchProduct != SearchAttributes.SEARCH_PROD_REFINANCE){
        	return false;
        }
        
        return true;
        
	}
	@Deprecated
	public OCRParsedDataStruct getOcrData() {
		return ocrData;
	}
	@Deprecated
	public void setOcrData(OCRParsedDataStruct ocrData) {
		this.ocrData = ocrData;
	}
	public boolean isPropertyCondo() {
    	try {
    		docManager.getAccess();
    		boolean isCondo = false;
    		for (DocumentI document : docManager.getDocumentsWithType(DType.ASSESOR, DType.TAX)) {
				for (PropertyI property : document.getProperties()) {
					if(property.getType().equals(PropertyI.PType.CONDO)) {
						return true;
					}
				}
			}
    		
    	} catch (Exception e) {
			e.printStackTrace();
		} finally {
			docManager.releaseAccess();
		}
		return false;
	}
	
	/**
     * Returns true if the search has the specified type
     * For available types see class ro.cst.tsearch.bean.SearchAttributes and use the following constants:
     * SEARCH_PROD_FULL
     * SEARCH_PROD_CURRENT_OWN
     * SEARCH_PROD_CONSTRUCTION
     * SEARCH_PROD_COMMERCIAL
     * SEARCH_PROD_REFINANCE
     * SEARCH_PROD_OE
     * SEARCH_PROD_LIENS
     * SEARCH_PROD_ACREAGE
     * SEARCH_PROD_SUBLOT
     * SEARCH_PROD_UPDATE
     * @param productType one of the above
     * @return
     */
    public boolean isProductType(int productType){
    	return getSa().getAtribute(SearchAttributes.SEARCH_PRODUCT).equals(String.valueOf(productType));
    }
    
    /**
     * Return how many times a search was opened 
     * @return
     */
	public AtomicInteger getOpenCount() {
		if(openCount == null) {
			/* old searches */
			openCount = new AtomicInteger();
			openCount.set(1);
		}
		return openCount;
	}
	
	public void setOpenCount(AtomicInteger openCount) {
		this.openCount = openCount;
	}
	
	public boolean wasNeverOpened() {
		return getOpenCount().get()==0;
	}
	
	public synchronized void setBackgroundSearch(boolean backgroundSearch) {
    	this.backgroundSearch = backgroundSearch;
    }
    public synchronized boolean isBackgroundSearch() {
    	return backgroundSearch;
    }
    
	/**
	 * gets the last date when the search was saved in the database
	 * @return the lastSaveDate
	 */
	public Date getLastSaveDate() {
		return lastSaveDate;
	}

	/**
	 * sets the date when the search is saved in the database
	 * @param lastSaveDate the lastSaveDate to set
	 */
	public void setLastSaveDate(Date lastSaveDate) {
		this.lastSaveDate = lastSaveDate;
	}
	
	public Date getSearchDueDate() {
		return searchDueDate;
	}

	public void setSearchDueDate(Date searchDueDate) {
		this.searchDueDate = searchDueDate;
	}
	
	public HashSet<String> getDistinctCategorySubcategoryPairs(){
		HashSet<String> all = new HashSet<String>();
		DocumentsManagerI docManager = getDocManager();
		try {
			docManager.getAccess();
			for (DocumentI doc : docManager.getDocumentsList()) {
				all.add(doc.getDocType() + "_" + doc.getDocSubType());
			}
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			docManager.releaseAccess();
		}
		return all;
	}
    
    public Calendar getCurrentTaxCalendar() throws BaseException {
    	Calendar cal = Calendar.getInstance();
    	long serverId = TSServersFactory.getSiteId(
    			getSa().getAtribute(SearchAttributes.P_STATE_ABREV),
    			getSa().getCountyName(),
    			HashCountyToIndex.getServerTypesAbrev()[GWTDataSite.TR_TYPE]);
    	CurrentInstance currentInstance = InstanceManager.getManager().getCurrentInstance(getID());
    	
    	boolean isParentSite = false;
    	if (currentInstance.getCrtSearchContext().getSearchRuningStatus().equals(SearchStatus.StatusCode.AUTOMATIC_OFF)){
    		isParentSite = true;
    	}
    	
    	DataSite data = HashCountyToIndex.getCrtServer(getID(), isParentSite);
    	
    	DataSite dat = HashCountyToIndex.getDateSiteForMIServerID(
    			currentInstance.getCurrentCommunity().getID().intValue(),
    			serverId);
    	
    	if (data != null && data.getPayDate() != null){//take pay date of the proper site
    		cal.setTime(data.getPayDate());
    		if ((cal.get(Calendar.YEAR) + "").equals("1900")){// if it doesn't exist, take it from TR, (like it was before)
    			if(dat != null){
    	    		cal.setTime(dat.getPayDate());
    	    	}
    		}
    	} else if(dat != null && dat.getPayDate() != null){
    		cal.setTime(dat.getPayDate());
    	}
    	
    	return cal;
    }
    
    public int getCommId(){
    	return sa.getCommId();
    }
    
    public int getProductId() {
    	return sa.getProductId();
    }
    
    public void setTsrLink(String tsrLink){
    	this.tsrLink = tsrLink;
    }
    
	public String getTsrLink() {
		if(StringUtils.isEmpty(tsrLink)){
			tsrLink = "";
		}
		return tsrLink;
	}
	
	public void setTsriParentLink(String tsriParentLink) {
		this.tsriParentLink = tsriParentLink;
	}

	public String getTsriParentLink() {
		return tsriParentLink;
	}

	 public void setTsriLink(String tsrLink){
	    	this.tsriLink = tsrLink;
	    }
	    
		public String getTsriLink() {
			if(StringUtils.isEmpty(tsriLink)){
				tsriLink = "";
			}
			return tsriLink;
		}
	
	public boolean isStarterStewartOrders() {
		return getSa().isStarterStewartOrders();
	}

	
	public void setAllImagesSsfLink(String imagesLink) {
		allImageSsfLink = imagesLink;
	}
	
	public String getAllImagesSsfLink() {
		if(allImageSsfLink==null){
			allImageSsfLink = "";
		}
		return allImageSsfLink;
	}

	private transient HashMap<Long, RestoreDocumentDataI> restorableDocuments = null;
	public HashMap<Long, RestoreDocumentDataI> getRestorableDocuments() {
		if(restorableDocuments == null) {
			restorableDocuments = new HashMap<Long, RestoreDocumentDataI>();
		}
		return restorableDocuments;
	}
	public void resetRestorableDocuments() {
		getRestorableDocuments().clear();
	}
	public RestoreDocumentDataI addRestorableDocument(RestoreDocumentDataI restoreDocumentDataI) {
		return getRestorableDocuments().put(restoreDocumentDataI.getId(), restoreDocumentDataI);
	}
	
	public String getCountyId(){
		return sa.getCountyId();
	}
	
	public String getCountyName(){
		return sa.getCountyName();
	}
	
	public String getStateId(){
		return sa.getStateId();
	}
	
	public String getStateAbv(){
		return sa.getStateAbv();
	}
	
	public String getStateCounty(){
		return sa.getStateCounty();
	}
	
	public void performAdditionalProcessingBeforeRunningAutomatic(ASThread asThread) {
		docManager.getAccess();
		
		try {
			List<DocumentI> documentsFromATS = docManager.getDocumentsWithDataSource(false, "ATS");
			setAdditionalInfo(AdditionalInfoKeys.PRESENT_ATS_DOCS_AT_AUTOMATIC_START, documentsFromATS);
		} catch (Exception e) {
			logger.error("Error while trying to perform ATS Merge", e);
		} finally {
			docManager.releaseAccess();
		}
		
	}

	public void performAdditionalProcessingAfterRunningAutomatic(ASThread asThread) {
		
		//if automatic touched ATS we need to check and merge
		if(asThread.getTouchedServersInAutomatic().contains("ATS")) {
			docManager.getAccess();
			
			try {
				List<DocumentI> documentsFromATS = docManager.getDocumentsWithDataSource(false, "ATS");
				
				List<DocumentI> presentFromATS = (List<DocumentI>) getAdditionalInfo(AdditionalInfoKeys.PRESENT_ATS_DOCS_AT_AUTOMATIC_START);
				if(presentFromATS == null) {
					presentFromATS = new ArrayList<DocumentI>();
				}
				
				boolean atLeastOneMerge = false;
				
				if(presentFromATS.size() < documentsFromATS.size()) {
					for (DocumentI documentI : documentsFromATS) {
						if(!presentFromATS.contains(documentI)) {
							try {
								mergeSearchWith(Long.parseLong(documentI.getDocno()), "document " + documentI.shortPrint());
								atLeastOneMerge = true;
							} catch (Exception e) {
								logger.error("There was an error merging search " + documentI.getDocno() + " with " + getID(), e);
							}
						}
					}
				}
				
				if(atLeastOneMerge) {
					//update references and checked documents
					applyQARules();
				}
				
			} catch (Exception e) {
				logger.error("Error while trying to perform ATS Merge", e);
			} finally {
				docManager.releaseAccess();
			}
		}
		
		docManager.getAccess();
		try {
			List<DocumentI> allReleases = docManager.getDocumentsWithDocType(true, "RELEASE");	//B4925
			Object savedObject = getAdditionalInfo(AdditionalInfoKeys.REJECTED_MORTGAGES_AND_LIENS);
			removeAdditionalInfo(AdditionalInfoKeys.REJECTED_MORTGAGES_AND_LIENS);
			List<InstrumentI> listWithInvalidatedDocuments = null;
			if(savedObject != null && (savedObject instanceof List<?>)) {
				listWithInvalidatedDocuments = (List<InstrumentI>) savedObject;
				for (DocumentI documentI : allReleases) {
					if (documentI instanceof RegisterDocumentI) {
						RegisterDocumentI registerDocument = (RegisterDocumentI) documentI;
						if(registerDocument.getDataSource().equals("SK") || 
								registerDocument.getDataSource().equals("TS")) {
							if(registerDocument.getReferences().isEmpty()) {	//no references for saved documents
								Set<InstrumentI> parsedReferences = registerDocument.getParsedReferences();
								boolean atLeastOneWasNotFoundAndInvalidated = false;
								for (InstrumentI parsedReference : parsedReferences) {
									boolean foundMatch = false;
									for (InstrumentI invalidatedInstrument : listWithInvalidatedDocuments) {
										if(parsedReference.flexibleEquals(invalidatedInstrument)) {
											foundMatch = true;
											break;
										}
									}
									if(!foundMatch) {
										atLeastOneWasNotFoundAndInvalidated = true;
										break;
									}
									
								}
								if(!atLeastOneWasNotFoundAndInvalidated && !parsedReferences.isEmpty()) {
									if(registerDocument.getProperties().isEmpty()) {
										docManager.remove(documentI);
										SearchLogger.info("<br>Deleted document <b>" + documentI.prettyPrint() + "</b> which is a release and all its references where already invalidated. " +  
												"<br>", getID());
									} else {
										for (PropertyI property : registerDocument.getProperties()) {
											if(!property.hasAddress() && !property.hasLegal()) {
												docManager.remove(documentI);
												SearchLogger.info("<br>Deleted document <b>" + documentI.prettyPrint() + "</b> which is a release and all its references where already invalidated. " +  
														"<br>", getID());
											}
										}
									}
								}
							}
						}
					}
				}
			}
			
			
			String stateIdString = getSa().getStateId();
			int countyId = Integer.parseInt(getCountyId());
			
			/**
			 * Apply rule for all TN sites (TN state id is 43)
			 */
			boolean applyDeleteDocumentWithInvalidatedReferences = false;
			if(StateContants.TN_STRING.equals(stateIdString)) {
				applyDeleteDocumentWithInvalidatedReferences = true;
			} else {
				
				switch (countyId) {
				case CountyConstants.TX_Bastrop:
				case CountyConstants.TX_Burnet:
				case CountyConstants.TX_Caldwell:
				case CountyConstants.TX_Hays:
				case CountyConstants.TX_Llano:
				case CountyConstants.TX_Travis:
				case CountyConstants.TX_Williamson:
					applyDeleteDocumentWithInvalidatedReferences = true;
					break;

				default:
					break;
				}
			}
			if(applyDeleteDocumentWithInvalidatedReferences){
				List<DocumentI> documents = docManager.getDocumentsWithDocType(
						DocumentTypes.APPOINTMENT,
						DocumentTypes.ASSIGNMENT, 
						DocumentTypes.RELEASE,
						DocumentTypes.MODIFICATION,
						DocumentTypes.SUBORDINATION);
				
				StringBuilder toPrint = new StringBuilder();
				
				for (DocumentI documentI : documents) {
					if (documentI instanceof RegisterDocumentI) {
						RegisterDocumentI registerDocument = (RegisterDocumentI) documentI;
						if(GWTDataSite.isRealRoLike(HashCountyToIndex.getServerTypeByAbbreviation(registerDocument.getDataSource())) ) {
							if(registerDocument.getReferences().isEmpty()) { // no references saved 
								Set<InstrumentI> parsedReferences = registerDocument.getParsedReferences();
								boolean toRemove = true;
								for(InstrumentI instr : parsedReferences){ // if all references we're not saved (invalidated)
									if(docManager.getDocument(instr)!=null){
										toRemove = false;
									}
								}
								
								if(toRemove){
									docManager.remove(documentI);
									toPrint.append("<br>Deleted document <b>")
										.append(documentI.prettyPrint())
										.append("</b> which has doctype ")
										.append(documentI.getDocType())
										.append(" and all its references where already invalidated. <br>");
								}
							}
						}
					}
				}
				
				if(toPrint.length() > 0) {
					SearchLogger.info(toPrint.toString(), getID());
				}
				
				
			}
			
			if(isSearchProductTypeOfUpdate()) {
				List<DocumentI> toRemove = new ArrayList<DocumentI>();
				
				Search updateSearch = SearchManager.getSearchFromDisk(getSa().getOriginalSearchId());
				
				
				DocumentsManagerI managerI = updateSearch.getDocManager();
				boolean hasLock = false;
				try {
					
					hasLock = managerI.tryAccess(120, TimeUnit.SECONDS);	//2 minutes
					
					if(hasLock) {
						for (DocumentI documentI : docManager.getRoLikeDocumentList()) {
							if(managerI.getDocumentsWithInstrumentsFlexible(false, documentI.getInstrument()).size() >= 1) {
								toRemove.add(documentI);
							}
						}
					}
				} catch (Throwable t) {
					logger.error("Error computing score for RejectAlreadySavedDocumentsForUpdateFilter", t);
				} finally {
					if(hasLock) {
						managerI.releaseAccess();
					}
				}
				StringBuilder toPrint = new StringBuilder();
				for (DocumentI documentI : toRemove) {
					docManager.remove(documentI);
					toPrint.append("<br>Deleted document <b>")
						.append(documentI.prettyPrint())
						.append("</b> which was already saved in Original Search<br>");
				}
				if(toPrint.length() > 0) {
					SearchLogger.info(toPrint.toString(), getID());
				}
				
				
			}
			
			StringBuilder toLog = new StringBuilder();
			
			
			if(asThread != null && !asThread.interactive && !URLMaping.INSTANCE_DIR.startsWith("local")) {
				List<DocumentI> allDocuments = docManager.getDocumentsWithDataSource(true, "TS"/*, "SK"*/);
				allDocuments.addAll(docManager.getDocumentsWithDataSource(false, "ATI"));
				
				for (DocumentI doc : allDocuments) {
					
					if(doc.hasImage() && doc.isIncludeImage() && (!doc.getImage().isSaved() || doc.getImage().getPath()==null || !new File(doc.getImage().getPath()).exists())) {
						try {
							if(doc.isUploaded()) {
								if(DocumentDataRetreiver.downloadImageFromSsfIfNeedIt(this, doc.getImage())){
									toLog.append("<br/>Image <a href='")
										.append(doc.getImage().getSsfLink())
										.append("'>")
										.append( doc.prettyPrint())
										.append("</a> downloaded from SSF ");
								}else{
									toLog.append("<br/>Could not download the image from SSF server for ")
										.append(doc.prettyPrint())
										.append(" with link [")
										.append(doc.getImage().getSsfLink())
										.append("]");
								}
							} else {
								TSInterface tsi = TSServersFactory.GetServerInstance((int)doc.getSiteId(), ID);
								DownloadImageResult res = tsi.downloadImage( doc );
								if( res.getStatus()!=DownloadImageResult.Status.OK ){
									doc.getImage().setSaved(false);
									toLog.append("<br>Image of document with following instrument number was not successfully retrieved: ")
										.append(doc.prettyPrint());
									doc.setImage(null);
								} else {
									doc.getImage().setSaved(true);
									toLog.append("<br>Image of document with following instrument number was successfully retrieved: <a href='")
										.append(doc.getImage().getSsfLink())
										.append("'>")
										.append(doc.prettyPrint())
										.append("</a>");
									
								}
							}
						} catch (Exception e) {
							doc.getImage().setSaved(false);
							toLog.append("<br>Image of document with following instrument number was not successfully retrieved:")
									.append(doc.prettyPrint());
							doc.setImage(null);
							logger.error("performAdditionalProcessingAfterRunningAutomatic", e);
						}
					}
				}
				
				
			}
			
			if(toLog.length() > 0) {
				toLog.append("<br>");
				SearchLogger.info(toLog.toString(), getID());
			}
			
			if(asThread.isBackgroundSearch()) {
				
				
				TsdIndexPageServer.getAllChaptersForSearch(this);
				
//				//remove cache
//				removeAdditionalInfo(AdditionalInfoKeys.CODE_BOOK_LIST);
//				
//				UserAttributes currentAgent = getAgent();
//				
//				if(currentAgent!=null){
//					try{
//						List<CommunityTemplatesMapper>userTemplates = UserUtils.getUserTemplates(
//								currentAgent.getID().longValue(),
//								-1,
//								UserUtils.FILTER_TSD_AND_BP_ONLY,
//								getProductId(),
//								false);
//						boolean foundTSD = false;
//						CommunityTemplatesMapper codeBookTemplate = null;
//						for (CommunityTemplatesMapper communityTemplatesMapper : userTemplates) {
//							if(!foundTSD && 
//									(communityTemplatesMapper.getName().startsWith(TemplatesInitUtils.TEMPLATE_TSD_START) 
//											|| communityTemplatesMapper.getPath().startsWith(TemplatesInitUtils.TEMPLATE_TSD_START))){
//								foundTSD = true;
//							}
//							if(codeBookTemplate == null && communityTemplatesMapper.isCodeBookLibrary()){
//								codeBookTemplate = UserUtils.getTemplateById(communityTemplatesMapper.getId());
//							}
//						}
//						
//						if (codeBookTemplate != null) {
//
//							String fileContent = codeBookTemplate.getFileContent();
//							boolean hasMoreMatches = fileContent != null;
//							Matcher mat = null;
//							String label = null;
//							int productId = getProductId();
//							Set<String> codeBookFullSet = new TreeSet<String>();
//							Set<String> codeBookStaticSet = new TreeSet<String>();
//							while (hasMoreMatches) {
//								mat = AddDocsTemplates.PATTERN_BPCODE_PRODUCT.matcher(fileContent);
//								if (mat.find()) {
//
//									label = mat.group(1);
//									// take just boilers for product id or default TemplateUtils.BOILER_MAP_DEFAULT_PRODUCT
//									if (label.endsWith("_" + productId)
//											|| (label.endsWith("_" + TemplateUtils.BOILER_MAP_DEFAULT_PRODUCT) && !codeBookFullSet.contains(label
//													.split(TemplatesServlet.LABEL_NAME_DELIM)[0]))) {
//										label = label.split(TemplatesServlet.LABEL_NAME_DELIM)[0];
//										codeBookFullSet.add(label);
//										
//										String fileToStringFragment = fileContent.substring(mat.end());
//										Matcher matcherInner = AddDocsTemplates.PATTERN_BPCODE_PRODUCT.matcher(fileToStringFragment);
//										String content = (matcherInner.find() ? fileToStringFragment.substring(0, matcherInner.start()) : fileToStringFragment).trim();
//										if(!(content.startsWith("<#documents=") && content.endsWith("/#>")) ) {
//											codeBookStaticSet.add(label);
//										}
//									}
//
//									fileContent = fileContent.substring(mat.end());
//									mat = AddDocsTemplates.PATTERN_BPCODE_PRODUCT.matcher(fileContent);
//									
//								} else {
//									hasMoreMatches = false;
//								}
//							}
//							ArrayList<String> codeBookList = new ArrayList<String>(codeBookFullSet);
//							setAdditionalInfo(AdditionalInfoKeys.CODE_BOOK_LIST, codeBookList);
//						}
//						
//					}
//					catch(Exception e){
//					}
//					
//					docManager.reloadCodeBookCodes(getCachedCodeList());
//				}
//				
//				setTsrViewFilterForProduct();
//				docManager.keepOnlyDocumentsInViewRange();
			}
			
		} catch (Exception e) {
			logger.error("Error while trying to delete some releases, B4925", e);
		} finally {
			docManager.releaseAccess();
		}
		
		applyQARules();
	}
	
	public boolean addSsfDocument(SsfDocumentMapper ssfDocument) {
		if(!ssfDocumentsUploaded.contains(ssfDocument)) {
			return ssfDocumentsUploaded.add(ssfDocument);
		}
		return false;
	}
	
	public int deleteAllSsfDocument() {
		int counter = 0;
		for (SsfDocumentMapper ssfDocumentMapper : ssfDocumentsUploaded) {
			//TODO: delete from SSF
			counter ++;
		}
		ssfDocumentsUploaded.clear();	//also delete from search
		return counter;
	}
	
	public List<SsfDocumentMapper> getSffDocumentWithType(int recordedIndex) {
		List<SsfDocumentMapper> result = new Vector<SsfDocumentMapper>();
		for (SsfDocumentMapper ssfDocumentMapper : ssfDocumentsUploaded) {
			if(recordedIndex == ssfDocumentMapper.getSsfRecordedIndex()) {
				result.add(ssfDocumentMapper);
			}
		}
		return result;
	}
	
	/**
	 * This must only be used when loading search object from xml using <b>ro.cst.tsearch.utils.XStreamManager</b><br>
	 * The purpose is to initialize fields that might be null when loading
	 */
	public synchronized void initOnDeserialization() {
		if(ssfDocumentsUploaded == null) {
			ssfDocumentsUploaded = new Vector<SsfDocumentMapper>();
		}
		if( sr == null ){
			sr = new SearchRecord();
		}
		setSearchType(PARENT_SITE_SEARCH);
		
		SearchFlags searchFlags = getSearchFlags();
		if(searchFlags.getCreationSourceType() == null) {
			searchFlags.setCreationSourceType(DBSearch.getCreationSourceType(getID()));
			if(searchFlags.getCreationSourceType() == null) {
				searchFlags.setCreationSourceType(CREATION_SOURCE_TYPES.NORMAL);
			}
		}
		
		List<ImageCount> imageCountForSearch = ImageCount.getImageCountForSearch(getID());
		if(imageCountForSearch != null) {
			for (ImageCount imageCount : imageCountForSearch) {
				Integer onObjectCount = getImagesCount().get(imageCount.getDataSource());
				//if nothing set on object or a lower value than database update object
				if(onObjectCount == null || onObjectCount < imageCount.getCount()) {
					getImagesCount().put(imageCount.getDataSource(), imageCount.getCount());
					logger.info("Updated Image Count on Object for searchid " + 
							getID() + ", datasource " + 
							imageCount.getDataSource() + " with new value " + 
							imageCount.getCount() + " (old value " + onObjectCount + ")");
				}
			}
		}
		DocumentsManagerI manager = this.getDocManager();
		try {
			manager.getAccess();
			for (DocumentI doc : manager.getDocumentsList()) {
				Set<String> codeBookSourceSet = doc.getCodeBookCodes();
				for (String codeBookCode : codeBookSourceSet){
					BoilerPlateObject codeBookCodeSource = doc.getCodeBookObject(codeBookCode);
					String modifiedStatement = codeBookCodeSource.getModifiedStatement();
					if (org.apache.commons.lang.StringUtils.isNotEmpty(modifiedStatement)){
						modifiedStatement = modifiedStatement.replaceAll("https?://ats(?:prdinet|stginet|preinet)?[0-9]*\\.advantagetitlesearch\\.com(?::\\d+)?", ServerConfig.getAppUrl());
						codeBookCodeSource.setModifiedStatement(modifiedStatement);
					}
				}
			}
		}finally {
			manager.releaseAccess();
		}
		
		if (isProductType(SearchAttributes.SEARCH_PROD_UPDATE)) {// 9305
			long parentSearchId = getParentSearchId();
			if (parentSearchId != NO_UPDATED_SEARCH ) {
				
				if(parentSearchId < getID()) {
				
				Search oldSearch = SearchManager.getSearchFromDisk(parentSearchId);
				if(oldSearch != null) {
					List<SsfDocumentMapper> sffDocumentWithType = oldSearch.getSffDocumentWithType(DocAdminConn.TEMPLATE_INDEX_TYPE);
					if(sffDocumentWithType != null) {
						SsfDocumentMapper tsdOldSearch = null;
						for (SsfDocumentMapper ssfDocumentMapper : sffDocumentWithType) {
							if("text/html".equals(ssfDocumentMapper.getMimeType())) {
								tsdOldSearch = ssfDocumentMapper;
								//do not break because on reopen (for old searches) previous files were not cleaned and the correct TSD is the last one
							}
							
						}
						if(tsdOldSearch != null) {
							setTsriParentLink(tsdOldSearch.getSsfLink());
						}
					}
				}
				
//				try {
//					Map<String, Object> tsriParentLinkMap = DBSearch.getTsriLinkInfoFromDB(parentSearchId);
//					String tsriParentLink = org.apache.commons.lang.StringUtils.defaultString((String) tsriParentLinkMap
//							.get(DBConstants.FIELD_SEARCH_TSRI_LINK));
//					setTsriParentLink(tsriParentLink);
//				} catch (Exception e) {
//					e.printStackTrace();
//				}
				} else {
					Log.sendEmail(
							MailConfig.getMailLoggerToEmailAddress(), 
							"Updates has a newer parent", 
							"UpdateID used: " + getID() + ", ParentID : " + parentSearchId);
				}
			}
		}
	}

	public Long getSecondaryAbstractorId() {
		return secondaryAbstractorId;
	}

	public void setSecondaryAbstractorId(Long secondaryAbstractorId) {
		this.secondaryAbstractorId = secondaryAbstractorId;
	}
	
	public void analyzeReportForFVSUpdates(){
		
		if (sa.isFVSUpdate()){
			String fileId = this.getAbstractorFileNo();
			StringBuffer messageReport = new StringBuffer("FVS Update for search with fileID " + fileId + " and searchId " + this.getSearchID() + " ended with the following status:\n\n");
			HashSet<Integer> warnings = searchFlags.getWarnings();
			StringBuffer partialReport = new StringBuffer();
			
			if (warnings.size() > 0){
				long parentSearchId = this.getParentSearchId();
				Search originalSearch = null;
				if (parentSearchId != NO_UPDATED_SEARCH){
					originalSearch = SearchManager.getSearchFromDisk(parentSearchId);
				}
				DocumentsManagerI manager = this.getDocManager();
				MortgageI lastMrtgForCurrentOwner = null;
				try{
					manager.getAccess();
					List<TransferI> allTransfers = manager.getTransferList();
					if (allTransfers.size() > 0 && warnings.contains(Warning.WARNING_NO_TRANSFERS_ID)){
						warnings.remove(Warning.WARNING_NO_TRANSFERS_ID);
					} else if (allTransfers.size() == 0){
						warnings.add(Warning.WARNING_NO_TRANSFERS_ID);
					} else if (allTransfers.size() > 1){
						warnings.add(Warning.WARNING_MORE_THAN_ONE_TRANSFER_ID);
					}
					
					TransferI lastTransfer = manager.getLastTransfer(false);	//do not check names so the warning can be issued
					Vector<String> nameRefGrantees = null;
					
					if (lastTransfer != null){
						PartyI tempOwnersSearchPage = new Party(PType.GRANTOR);
						for (NameI name : sa.getOwners().getNames()) {
							if (name.isValidated()) {
								tempOwnersSearchPage.add(name);
							}
						}
						if (tempOwnersSearchPage.size() == 0) {
							//force all names is none is validated
							tempOwnersSearchPage = sa.getOwners();
						}
						if (GenericNameFilter.isMatchGreaterThenScore(tempOwnersSearchPage, lastTransfer.getGrantee(), NameFilterFactory.NAME_FILTER_THRESHOLD) 
								&&GenericNameFilter.isMatchGreaterThenScore(lastTransfer.getGrantee(), tempOwnersSearchPage , NameFilterFactory.NAME_FILTER_THRESHOLD)){

							StringBuffer message = new StringBuffer();
							message.append("\n\nVesting Deed ").append(lastTransfer.prettyPrint()).append(" for Grantor ")
									.append(lastTransfer.getGrantorFreeForm()).append(" recorded on ").append(lastTransfer.getRecordedDate());
							
							partialReport.append(message);
						}
						
						OCRParsedDataStruct ocrDataFromTransfer = lastTransfer.getOcrData();
						if (ocrDataFromTransfer != null){
							Vector<String> legalDescription = ocrDataFromTransfer.getLegalDescriptionVector();
							Vector<String> addressVector = ocrDataFromTransfer.getAddressVector();
							Vector<String> nameRefGrantors = ocrDataFromTransfer.getNameRefGrantor();
							nameRefGrantees = ocrDataFromTransfer.getNameRefGrantee();
							if (originalSearch != null){
								SearchAttributes originalSA = originalSearch.getSa();
								Set<NameI> originalSearchCurrentOwners = originalSA.getOwners().getNames();

								if (originalSearchCurrentOwners != null){
									Set<NameI> matchedGrantors = new LinkedHashSet<NameI>();
									if (nameRefGrantors != null && nameRefGrantors.size() > 0){
										Set<NameI> refGrantors = parseOCRNames(nameRefGrantors);
										for (NameI nameOriginal : originalSearchCurrentOwners) {
											for (NameI nameI : refGrantors) {
												if (GenericNameFilter.isMatchGreaterThenScore(nameOriginal, nameI, NameFilterFactory.NAME_FILTER_THRESHOLD)
														&& GenericNameFilter.isMatchGreaterThenScore(nameI, nameOriginal, NameFilterFactory.NAME_FILTER_THRESHOLD)){
													matchedGrantors.add(nameI);
												}
											}
										}
										if (matchedGrantors.size() > 0){
											StringBuffer message = new StringBuffer();
											message.append("\nOCRed grantors ").append(matchedGrantors.toString()).append(" found on the new Vesting Deed ")
													.append(lastTransfer.prettyPrint()).append(" must match current owners ")
													.append(originalSearchCurrentOwners.toString()).append("\n");
											partialReport.append(message);
										}
									}
								}
								PropertyI originalSearchValidateProperty = originalSA.getValidatedProperty();
								if (originalSearchValidateProperty != null){
									checkLDForFVS(partialReport, lastTransfer, legalDescription, originalSA, originalSearchValidateProperty);
									
									checkAddressForFVS(partialReport, lastTransfer, addressVector, originalSearchValidateProperty);
								}
							}
						}
					}
					List<MortgageI> allMortgages = manager.getMortgageList(false);
					DocumentUtils.sortDocuments(allMortgages, MultilineElementsMap.DATE_ORDER_DESC);
					
					lastMrtgForCurrentOwner = WarningUtils.getLastMortgageForCurrentOwner(this, allMortgages, lastTransfer, false);
					if (lastMrtgForCurrentOwner != null){
						if (warnings.contains(Warning.WARNING_NO_MORTGAGE_FOR_CURRENT_OWNER_ID)){
							warnings.remove(Warning.WARNING_NO_MORTGAGE_FOR_CURRENT_OWNER_ID);
						}
						StringBuffer message = new StringBuffer();
						message.append("\n\nMortgage/Deed of Trust ").append(lastMrtgForCurrentOwner.prettyPrint()).append(" for Grantor ")
								.append(lastMrtgForCurrentOwner.getGrantorFreeForm()).append(" recorded on ").append(lastMrtgForCurrentOwner.getRecordedDate());
						
						partialReport.append(message);
						
						PartyI mrtgGrantors = lastMrtgForCurrentOwner.getGrantor();
						Set<NameI> deedGrantees = new HashSet<NameI>();
						if (nameRefGrantees != null){
							deedGrantees = parseOCRNames(nameRefGrantees);
						}
						if (mrtgGrantors.size() > 0 && deedGrantees.size() > 0){
							Set<NameI> matchedGtorGtee = new LinkedHashSet<NameI>();
							for (NameI mrtgGrantor : mrtgGrantors.getNames()) {
								for (NameI deedGrantee : deedGrantees) {
									if (GenericNameFilter.isMatchGreaterThenScore(mrtgGrantor, deedGrantee, NameFilterFactory.NAME_FILTER_THRESHOLD)
											&& GenericNameFilter.isMatchGreaterThenScore(deedGrantee, mrtgGrantor, NameFilterFactory.NAME_FILTER_THRESHOLD)){
										matchedGtorGtee.add(deedGrantee);
									}
								}
							}
							if (matchedGtorGtee.size() >0){
								message = new StringBuffer();
								message.append("\nSome OCRed Grantees ").append(matchedGtorGtee.toString()).append(" from Vesting Deed ").append(lastTransfer.prettyPrint())
										.append(" matches with some Grantors ").append(mrtgGrantors.getNames().toString())
										.append(" from Mortgage/Deed of Trust ").append(lastMrtgForCurrentOwner.prettyPrint()).append("\n");
								
								partialReport.append(message);
							}
						}
					} else{
						warnings.add(Warning.WARNING_NO_MORTGAGE_DOCUMENTS_FOR_BUYERS_ID);
					}
				}
				finally{
					manager.releaseAccess();
				}
				boolean noTransfersFound = false, noMortgageFound = false;
				boolean sequenceProblem = false;
					for (Integer warning : warnings) {
						if (warning == Warning.WARNING_NO_TRANSFERS_ID){
							messageReport.append("\n").append(new WarningInfo(Warning.WARNING_NO_TRANSFERS_ID));
							noTransfersFound = true;
						} else if (warning == Warning.WARNING_NO_MORTGAGE_DOCUMENTS_FOR_BUYERS_ID){
							messageReport.append("\n").append(new WarningInfo(Warning.WARNING_NO_MORTGAGE_DOCUMENTS_FOR_BUYERS_ID));
							noMortgageFound = true;
						} else if (warning == Warning.WARNING_MORE_THAN_ONE_TRANSFER_ID){
							messageReport.append("\n").append(new WarningInfo(Warning.WARNING_MORE_THAN_ONE_TRANSFER_ID))
								.append("\n    The transaction is flagged with \"X\", suspicious with the resolution: Possible property flips");
						} else if (warning == Warning.WARNING_RELEASE_FOR_PREVIOUS_MORTGAGE_MISSING_ID){
							messageReport.append("\n").append(new WarningInfo(Warning.WARNING_RELEASE_FOR_PREVIOUS_MORTGAGE_MISSING_ID));
						} else if (warning == Warning.WARNING_MORTGAGE_OUT_OF_SEQUENCE_ID){
							messageReport.append("\n").append(new WarningInfo(Warning.WARNING_MORTGAGE_OUT_OF_SEQUENCE_ID));
							sequenceProblem = true;
						} else if (warning == Warning.WARNING_NO_OPEN_MORTGAGE && lastMrtgForCurrentOwner != null){
							messageReport.append("\n").append((new WarningInfo(Warning.WARNING_NO_OPEN_MORTGAGE)).getText().replaceFirst("@INSTRUMENT@", "(" + lastMrtgForCurrentOwner.prettyPrint() + ")"));
							sequenceProblem = true;
						} else if (warning == Warning.WARNING_OLD_MORTGAGE_ID){
							messageReport.append("\n").append(new WarningInfo(Warning.WARNING_OLD_MORTGAGE_ID));
							sequenceProblem = true;
						}
					}
					
					messageReport.append(partialReport);
					if (!warnings.contains(Warning.WARNING_NO_MORTGAGE_FOR_CURRENT_OWNER_ID) && !warnings.contains(Warning.WARNING_NO_TRANSFERS_ID) && 
							!warnings.contains(Warning.WARNING_MORE_THAN_ONE_TRANSFER_ID)){
						if (warnings.contains(Warning.WARNING_RELEASE_FOR_PREVIOUS_MORTGAGE_MISSING_ID)){
							messageReport.append("\n\nThe transaction is flagged with \"X\", suspicious with the resolution: Previous owner Mortgage/Deed Of Trust may not be released.");
						} else{
							messageReport.append("\n\nRelease of the previous Mortgages exist and there are No other warnings: This transaction is clean and we can delete Y flag");
							
							if (parentSearchId  != NO_UPDATED_SEARCH){
								try {
									DBManager.setFVSFlagSearch(parentSearchId, this.getAgent(), true);;
								} catch (BaseException e){
									e.printStackTrace();
								} catch (UpdateDBException e){
									e.printStackTrace();
								}
							}
						}
					}
					if (sequenceProblem){
						messageReport.append("\n\nThe transaction is flagged with \"X\", suspicious with the resolution: Possible document sequence problems.");
					}
					if (	(noTransfersFound && noMortgageFound) 
							&& 
							(!sa.isFVSAutoLaunched() || (sa.isFVSAutoLaunched() && sa.isLastScheduledFVSUpdate()))
							){
						messageReport.append("\n\nNo new documents found. Transaction may not be closed.");
						
						String msg = messageReport.toString();
						msg = msg.replaceAll("(?is)\n", "<br>");
						SearchLogger.info("\n</div><hr/><div><BR><b>FVS Report</b><BR>" + msg + "<BR></div>\n", searchID);
						
						 //the CurrentCommunity
				        CommunityAttributes ca = InstanceManager.getManager().getCurrentInstance(searchID).getCurrentCommunity();
				        UserAttributes commAdmin = null;
						try {
							commAdmin = UserUtils.getUserFromId(CommunityUtils.getCommunityAdministrator(ca));
						} catch (BaseException e) {
							e.printStackTrace();
						}
				        
				        String commAdmMail = commAdmin.getEMAIL();
						Util.sendMail(null, this.getAgent().getEMAIL(), commAdmMail, null, "FVS Report on " + SearchLogger.getCurDateTimeCST() + " for " + fileId, messageReport.toString());
					}
				}
			}
	}

	/**
	 * @param partialReport
	 * @param lastTransfer
	 * @param addressVector
	 * @param originalSearchValidateProperty
	 */
	public void checkAddressForFVS(StringBuffer partialReport, TransferI lastTransfer, Vector<String> addressVector, PropertyI originalSearchValidateProperty) {
		if (addressVector != null && addressVector.size() > 0){
			AddressI address = originalSearchValidateProperty.getAddress();
			if (address != null){
				for (String adresa : addressVector) {
					adresa = adresa.replaceFirst("(?is)\\b[A-Z]{3,}\\s*,\\s*[A-Z]{2}\\s*[\\d-]+\\s*$", "");
					adresa = adresa.replaceFirst("(?is)\\b[A-Z]{2}\\s*[\\d-]+\\s*$", "");
					adresa = adresa.replaceAll("(?is)[,\\.]+", "");
					
					StandardAddress sda = new StandardAddress(adresa);
					AddressI addressOCRed = new Address();
					
					addressOCRed.setNumber(sda.getAddressElement(StandardAddress.STREET_NUMBER));
					addressOCRed.setStreetName(sda.getAddressElement(StandardAddress.STREET_NAME));
					addressOCRed.setPostDirection(sda.getAddressElement(StandardAddress.STREET_POSTDIRECTIONAL));
					addressOCRed.setPreDiretion(sda.getAddressElement(StandardAddress.STREET_PREDIRECTIONAL));			
					addressOCRed.setIdentifierType(sda.getAddressElement(StandardAddress.STREET_SEC_ADDR_IDENT));
					addressOCRed.setIdentifierNumber(sda.getAddressElement(StandardAddress.STREET_SEC_ADDR_RANGE));	
					addressOCRed.setSuffix(sda.getAddressElement(StandardAddress.STREET_SUFFIX));
					
					if (addressOCRed.equals(address)){
						StringBuffer message = new StringBuffer();
						message.append("\nOCRed address [").append(adresa.toString()).append("] found on Vesting Deed ")
							.append(lastTransfer.prettyPrint()).append(" matches with the address from original search ")
							.append(address.shortFormString());
						partialReport.append(message);
					}
				}
			}
		}
	}

	/**
	 * @param partialReport
	 * @param lastTransfer
	 * @param legalDescription
	 * @param originalSA
	 * @param originalSearchValidateProperty
	 */
	public void checkLDForFVS(StringBuffer partialReport, TransferI lastTransfer, Vector<String> legalDescription, SearchAttributes originalSA,
			PropertyI originalSearchValidateProperty) {
		if (legalDescription != null && legalDescription.size() > 0){
			LegalI legal = originalSearchValidateProperty.getLegal();
			String validatedPB = originalSA.getValidatedPlatBook();
			String validatedPP = originalSA.getValidatedPlatPage();
			String validatedLot = originalSA.getValidatedLot();
			String validatedBlock = originalSA.getValidatedBlock();

			Pattern patLot = Pattern.compile("(?is)\\bLots?\\s+(\\w+)");
			Pattern patBlock = Pattern.compile("(?is)\\bBl(?:oc)?ks?\\s+(\\w+)");
			Pattern patPBPG = Pattern.compile("(?is)(?is)\\bPlat Book\\s+(\\w+),?\\s+Page\\s+(\\w+)");
			for (String ld : legalDescription) {
				boolean foundLot = false, foundBlock = false, foundPB = false, foundPP = false;
				
				Matcher mat = patPBPG.matcher(ld);
				if (mat.find()){
					String ocrPB = mat.group(1);
					String ocrPP = mat.group(2);
					if (validatedPB.equals(ocrPB)){
						foundPB = true;
					}
					if (validatedPP.equals(ocrPP)){
						foundPP = true;
					}
				}
				mat.reset();
				mat = patLot.matcher(ld);
				if (mat.find()){
					String ocrLot = mat.group(1);
					if (validatedLot.equals(ocrLot)){
						foundLot = true;
					}
				}
				mat.reset();
				mat = patBlock.matcher(ld);
				if (mat.find()){
					String ocrBlock = mat.group(1);
					if (validatedBlock.equals(ocrBlock)){
						foundBlock = true;
					}
				}
				if (foundPB && foundPP && (foundLot || foundBlock)){
					StringBuffer message = new StringBuffer();
					message.append("\nOCRed LD [").append(ld.toString().trim()).append("] found on Vesting Deed ")
						.append(lastTransfer.prettyPrint()).append(" matches with validated LD from original search ")
						.append(legal.getSubdivision().shortFormString());
					partialReport.append(message);
				}
			}
		}
	}
	
	/**
	 * fils searchProduct tag from templates
	 */
	public String getSearchProductTemplateTag() {
		try {
			// task 8599
			String starterType = HashCountyToIndex.getStarterTypeString(this.getCommId(), State.getState(new Integer(this.getStateId())).getStateAbv(), County
					.getCounty(new Integer(this.getCountyId())).getName(), this.getProductId());
			String baseType = getBaseSearchProductType();

			String result = org.apache.commons.lang.StringUtils.defaultString(starterType) + ", " +
					org.apache.commons.lang.StringUtils.defaultString(baseType);

			return result.replaceFirst("^,", "").trim();
		} catch (Exception e) {
			e.printStackTrace();
		}

		return "";
	}

	private String getBaseSearchProductType() {
		List<DocumentI> documentsATS = new ArrayList<DocumentI>();
		
		try {
			getDocManager().getAccess();
			documentsATS = getDocManager().getDocumentsWithDataSource(false, HashCountyToIndex.getServerAbbreviationByType(GWTDataSite.ATS_TYPE));
		} catch (Exception e){
			 e.printStackTrace();
		} finally {
			getDocManager().releaseAccess();
		}
		
		boolean isBase = false;
		boolean isPrior = false; 
		
		for(DocumentI d : documentsATS){
			if(d instanceof PriorFileAtsDocumentI){
				if(((PriorFileAtsDocumentI)d).isBase()){
					isBase = true;
				} else {
					isPrior = true;
				}
			}
			
			if(isBase && isPrior){
				break;
			}
		}
		
		String result = "";
		
		if(isBase){
			result+=HashCountyToIndex.ATSB_STRING + ", "; 
		}
		
		if(isPrior){
			result+=HashCountyToIndex.ATSP_STRING; 
		}
		
		return result.trim().replaceAll(",$","");
	}
	
	/**
	 * Easy method of getting all available cached codes for the selected Code Book Library<br>
	 * Please be sure to call it after it is loaded. Currently loading is done when entering TSRIndex Page
	 * @return codes or empty list if none is available
	 */
	public List<String> getCachedCodeList() {
		Object additionalInfo = getAdditionalInfo(AdditionalInfoKeys.CODE_BOOK_LIST);
		if(additionalInfo != null && additionalInfo instanceof List) {
			return (List<String>) additionalInfo;
		}
		return new ArrayList<String>();
	}
	
	public Set<NameI> parseOCRNames(Vector<String> ocrNames) {
		Set<NameI> ret = new LinkedHashSet<NameI>();
		for (String element : ocrNames) {
			NameTokenList brokenName=NameParser.parseNameFML(element);
			String[] names = GenericFunctions.extractSuffix(brokenName.getMiddleNameAsString());
			Name name = new Name(
					brokenName.getFirstNameAsString(),
					names[0],
					brokenName.getLastNameAsString());
			name.setCompany((name.getFirstName().length() == 0) && (name.getMiddleName().length() == 0) && NameUtils.isCompany(name.getLastName()));
			name.setSufix(names[1]);
			names = GenericFunctions.extractType(brokenName.getMiddleNameAsString());
			name.setNameType(names[1]);
			names = GenericFunctions.extractOtherType(brokenName.getMiddleNameAsString());
			name.setNameOtherType(names[1]);
			ret.add(name);
		}
		return ret;
	}	
	
	/**
	 * remove a particular instance of a warning
	 * @param clas
	 */
	public void removeWarning(Class clas){
		List<WarningInfoI> warningList = this.getSearchFlags().getWarningList();
		for (WarningInfoI warningInfoI : warningList) {
			if (clas.isInstance(warningInfoI)){
				this.getSearchFlags().getWarningList().remove(warningInfoI);
				break;
			}
		}
	}
	
	public void addDataSource(DataSite dat){
		
		if (getServerTypes() == null){
			return ;
		}
		if (dat == null){
			return ;
		}
		
		Set<DataSources> dataSourcesList = (Set<DataSources>) this.getSa().getDataSourcesOnSearch();
		
		String serverDescription = getServerTypes()[dat.getSiteTypeInt()];
		String siteType = dat.getSiteTypeAbrev();
		
		if (org.apache.commons.lang.StringUtils.isNotEmpty(dat.getCityName())){
			serverDescription = dat.getCityName() + " " + serverDescription.replace(siteType, "");
		}
		boolean alreadyContained = false;
		for (Iterator<DataSources> iterator = dataSourcesList.iterator(); iterator.hasNext();){
			DataSources dataSources = (DataSources) iterator.next();
			if (dataSources.getSiteTypeInt() == dat.getSiteTypeInt()){
				alreadyContained = true;

				if (isParentSiteSearch() && RUN_TYPE.AUTO.toString().equals(dataSources.getRunType())){
					dataSources.setRunType(RUN_TYPE.BOTH.toString());
				} else if (isAutomaticSearch() && RUN_TYPE.PS.toString().equals(dataSources.getRunType())){
					dataSources.setRunType(RUN_TYPE.BOTH.toString());
				} else if (org.apache.commons.lang.StringUtils.isEmpty(dataSources.getRunType())){
					if (isParentSiteSearch()){
						dataSources.setRunType(RUN_TYPE.PS.toString());
					} else if (isAutomaticSearch()){
						dataSources.setRunType(RUN_TYPE.AUTO.toString());
					}
				}
				break;
			}
		}
		if (!alreadyContained){
			DataSources datasource = new DataSources(dat.getSiteTypeInt(), dat.getSiteTypeAbrev(), serverDescription.trim());
			if (dat.isRoLikeSite()){
				datasource.setEffectiveStartDate(dat.getEffectiveStartDate());
			}
			if (isParentSiteSearch()){
				datasource.setRunType(RUN_TYPE.PS.toString());
			} else if (isAutomaticSearch()){
				datasource.setRunType(RUN_TYPE.AUTO.toString());
			}
			dataSourcesList.add(datasource);
		}
	}
	
	/**
	 * 
     * Here should be the version of the current Search Implementation<br>
     * We use this to know which important updates were made in that version<br>
     * <b>0</b> - is the default version just before this field was added<br>
     * <b>1</b> - is the version in which we operated changes to the 
	 * @return current version saved on search
	 */
	public int getVersionSearchNo() {
		return versionSearchNo;
	}
	/**
	 * Updates the version saved on search with the default one
	 */
	public void updateVersionSearchNo() {
		this.versionSearchNo = SEARCH_VERSION.getCurrentVersion();
	}

	/**
	 * Merges source with current search using rules defined in the doctype file<br>
	 * Only Categories/Subcategories that are allowed to be copied (COPY_ON_MERGE="1") are take into consideration<br>
	 * Documents that exists in the current search are merged and those that don't are copied<br>
	 * Merging includes coping the codes from the source document to the new one updating those that were not saved by the user in current search
	 * @param searchIdSource
	 * @param sourceDescription description of the search to show in log
	 * @return true if the merge was finished succesfully
	 */
	public boolean mergeSearchWith(long searchIdSource, String sourceDescription) {
		Search searchSource = SearchManager.getSearchFromDisk(searchIdSource);
        if(searchSource == null)
        	return false;
        return mergeSearchWith(searchSource, sourceDescription);
	}
	/**
	 * Merges source with current search using rules defined in the doctype file<br>
	 * Only Categories/Subcategories that are allowed to be copied (COPY_ON_MERGE="1") are take into consideration<br>
	 * Documents that exists in the current search are merged and those that don't are copied<br>
	 * Merging includes coping the codes from the source document to the new one updating those that were not saved by the user in current search
	 * @param searchSource
	 * @param sourceDescription description of the search to show in log
	 * @return true if the merge was finished succesfully
	 */
	public boolean mergeSearchWith(Search searchSource, String sourceDescription) {
		
        
        County county = InstanceManager.getManager().getCurrentInstance(getID()).getCurrentCounty();
        
        boolean lockedCurrentSearch = false;
        boolean lockedSourceSearch = false;
        
        List<DocumentI> addedDocuments = new ArrayList<DocumentI>();
        List<DocumentI> updatedDocuments = new ArrayList<DocumentI>();
        List<RegisterDocumentI> alreadyAnalyzedDocuments = new ArrayList<RegisterDocumentI>();
        
        DocumentsManagerI docManagerSource = searchSource.getDocManager();
        searchSource.applyQARules();		//better safe than sorry
        
        try {
        	docManagerSource.getAccess();
        	lockedSourceSearch = true;
        	lockedCurrentSearch = docManager.tryAccess(2, TimeUnit.MINUTES);
        	if(!lockedCurrentSearch) {
        		return false;
        	}
        	
        	List<RegisterDocumentI> roLikeSourceDocumentList = docManagerSource.getRoLikeDocumentList();
        	for (RegisterDocumentI sourceDocumentI : roLikeSourceDocumentList) {
        		internalMergeOneDocument(searchSource, county, addedDocuments, updatedDocuments, alreadyAnalyzedDocuments, docManagerSource, sourceDocumentI, false);
			}
        	
        } finally {
        	if(lockedSourceSearch) {
        		try {
					docManagerSource.releaseAccess();
				} catch (Exception e) {
					e.printStackTrace();
				}
        	}
        	if(lockedCurrentSearch) {
        		try {
					docManager.releaseAccess();
				} catch (Exception e) {
					e.printStackTrace();
				}
        	}
        }
        
        StringBuilder sb = new StringBuilder("<br>");
        if(!addedDocuments.isEmpty()) {
        	sb.append("<div>Following documents were added as a result of the <b>MERGE</b> with ");
        	if(org.apache.commons.lang.StringUtils.isBlank(sourceDescription)) {
        		sb.append("search id ").append(searchSource.getID());
        	} else {
        		sb.append(sourceDescription);
        	}
        	sb.append("</div>");
        	
    		sb.append(ro.cst.tsearch.utils.StringUtils.createCollapsibleHeader())
    			.append("<table border='1' cellspacing='0' width='99%'>")
    			.append("<tr><th>No</th>")
            	.append("<th>DS</th><th align='left'>Desc</th><th>Date</th><th>Grantor</th><th>Grantee</th><th>Instr Type</th><th>Instr</th><th>Remarks</th></tr>");
    		for (int i = 0; i < addedDocuments.size(); i++) {
    			DocumentI documentI = addedDocuments.get(i);
    			SearchLogRow searchLogRow = documentI.getSearchLogRow();
    			sb.append(searchLogRow.getLogIntermediary(i));
			}
    		sb.append("</table></div>");
        } else {
        	sb.append("<div>No documents were added as a result of the <b>MERGE</b> with ");
        	if(org.apache.commons.lang.StringUtils.isBlank(sourceDescription)) {
        		sb.append("search id ").append(searchSource.getID());
        	} else {
        		sb.append(sourceDescription);
        	}
        	sb.append("</div>");
        }
        if(updatedDocuments.isEmpty()) {
        	sb.append("<div>No documents were updated as a result of the <b>MERGE</b> with ");
        	if(org.apache.commons.lang.StringUtils.isBlank(sourceDescription)) {
        		sb.append("search id ").append(searchSource.getID());
        	} else {
        		sb.append(sourceDescription);
        	}
        	sb.append("</div>");
        } else {
        	sb.append("<div>Following documents were updated as a result of the <b>MERGE</b> with ");
        	if(org.apache.commons.lang.StringUtils.isBlank(sourceDescription)) {
        		sb.append("search id ").append(searchSource.getID());
        	} else {
        		sb.append(sourceDescription);
        	}
        	sb.append("</div>");
    		sb.append(ro.cst.tsearch.utils.StringUtils.createCollapsibleHeader())
    			.append("<table border='1' cellspacing='0' width='99%'>")
    			.append("<tr><th>No</th>")
            	.append("<th>DS</th><th align='left'>Desc</th><th>Date</th><th>Grantor</th><th>Grantee</th><th>Instr Type</th><th>Instr</th><th>Remarks</th></tr>");
    		for (int i = 0; i < updatedDocuments.size(); i++) {
    			DocumentI documentI = updatedDocuments.get(i);
    			SearchLogRow searchLogRow = documentI.getSearchLogRow();
    			sb.append(searchLogRow.getLogIntermediary(i));
			}
    		sb.append("</table></div>");
        }
        
        SearchLogger.info(sb.toString(), getID());
        
		return true;
	}

	private void internalMergeOneDocument(Search searchSource, County county, List<DocumentI> addedDocuments, List<DocumentI> updatedDocuments,
			List<RegisterDocumentI> alreadyAnalyzedDocuments, DocumentsManagerI docManagerSource, RegisterDocumentI sourceDocumentI, boolean forceMerge) {
		DocTypeNode subcategoryNode = DocTypeNode.getSubcategoryNode(county, sourceDocumentI.getDocType(), sourceDocumentI.getDocSubType());
		if (forceMerge || (subcategoryNode != null && subcategoryNode.allowCopyOnMerge())) {
			
			if (addedDocuments.contains(sourceDocumentI) || updatedDocuments.contains(sourceDocumentI) || alreadyAnalyzedDocuments.contains(sourceDocumentI)) {
				//already treated this document
				return;
			}
			
			
			DocumentI currentExistingDocument = docManager.getDocument(sourceDocumentI);
			boolean notFound = true;
			if(currentExistingDocument != null) {
				notFound = false;
				if(currentExistingDocument instanceof RegisterDocumentI) {
					mergeExistingDocument(sourceDocumentI, (RegisterDocumentI)currentExistingDocument, updatedDocuments);
					alreadyAnalyzedDocuments.add(sourceDocumentI);
					for (DocumentI documentI : sourceDocumentI.getReferences()) {
						if(documentI instanceof RegisterDocumentI) {
							internalMergeOneDocument(searchSource, county, addedDocuments, updatedDocuments, alreadyAnalyzedDocuments, docManagerSource, (RegisterDocumentI)documentI, true);
						}
					}
				}
			} else {
				List<DocumentI> almostLike = docManager.getDocumentsWithInstrumentsFlexible(false, sourceDocumentI.getInstrument());
				if(almostLike != null && !almostLike.isEmpty()) {
					for (DocumentI currentAlmostLikeDocument : almostLike) {
						if(!notFound) {		//merge first document only
							break;
						}
						notFound = false;
						if(currentAlmostLikeDocument instanceof RegisterDocumentI) {
							mergeExistingDocument(sourceDocumentI, (RegisterDocumentI)currentAlmostLikeDocument, updatedDocuments);
							alreadyAnalyzedDocuments.add(sourceDocumentI);
							for (DocumentI documentI : sourceDocumentI.getReferences()) {
								if(documentI instanceof RegisterDocumentI) {
									internalMergeOneDocument(searchSource, county, addedDocuments, updatedDocuments, alreadyAnalyzedDocuments, docManagerSource, (RegisterDocumentI)documentI, true);
								}
							}
						}
					}
				 	
				}
			}
			if(notFound) {
				//lucky me, let's just copy the document
				DocumentI newDoc = sourceDocumentI.clone();
				
				String contentDocIndex = DBManager.getDocumentIndex(newDoc.getIndexId());
				if (contentDocIndex != null) {
					if (contentDocIndex.indexOf("searchId=" + searchSource.getID()) != -1)
						contentDocIndex = contentDocIndex.replaceAll("(?is)(searchId=)" + searchSource.getID(), "$1" + getID());
					newDoc.setIndexId(DBManager.addDocumentIndex(contentDocIndex, this));
				}
				
				if(newDoc instanceof SSFPriorFileDocument) {
					SSFPriorFileDocument sfDocument = (SSFPriorFileDocument)newDoc;
					String contentSfDocIndex = DBManager.getSfDocumentIndex(sfDocument.getSsfIdexId());
					if(contentSfDocIndex != null) {
						if (contentSfDocIndex.indexOf("searchId=" + searchSource.getID()) != -1) {
							contentSfDocIndex = contentSfDocIndex.replaceAll("(?is)(searchId=)" + searchSource.getID(), "$1" + getID());
						}
						sfDocument.setSsfIdexId(DBManager.addSfDocumentIndex(contentSfDocIndex, this));
					}
				}
				
				boolean addToDocManager = true;
				if(newDoc.is(DocumentTypes.OTHERFILES, DocumentTypes.OTHER_FILE_EXC) 
						|| newDoc.is(DocumentTypes.OTHERFILES, DocumentTypes.OTHER_FILE_REQ)
						|| newDoc.is(DocumentTypes.OTHERFILES, DocumentTypes.OTHER_FILE_ESTATE)
						|| newDoc.is(DocumentTypes.OTHERFILES, DocumentTypes.OTHER_FILE_COMMENT)) {
					ArrayList<RegisterDocumentI> registerDocuments = docManagerSource.getRegisterDocuments(newDoc, true);
					if(!registerDocuments.isEmpty()) {
						addToDocManager = false;
						
						for (RegisterDocumentI currentDocument : registerDocuments) {
							
							updatedDocuments.add(currentDocument);
							
							Set<String> codeBookSourceSet = newDoc.getCodeBookCodes();
		    				for (String codeBookCode : codeBookSourceSet) {
		    					BoilerPlateObject codeBookCodeSource = newDoc.getCodeBookObject(codeBookCode);
		    					//skip deleted codes
		    					if(!codeBookCodeSource.isManuallyDeleted()) {
		    						BoilerPlateObject codeBookCurrentObject = currentDocument.getCodeBookObject(codeBookCode);
		        					
									if(codeBookCurrentObject != null) {
										if(!codeBookCurrentObject.hasModifiedStatement()) {
											if(codeBookCodeSource.hasModifiedStatement()) {
												//copy statement if the source is manually modified
												codeBookCurrentObject.setModifiedStatement(codeBookCodeSource.getModifiedStatement());
											}
										}
									} else {
										//I do not have this code but it might have been manually added so let's try to copy it
										if(codeBookCodeSource.isManuallyAdded()) {
											currentDocument.addCodeBookCode(codeBookCodeSource.clone());
										}
									}
		    					}
							}
						}
					}
				}
				
				if(addToDocManager) {
					if(searchSource.getSearchFlags().isBase()) {
						newDoc.setExternalSourceType("bs");
					} else {
						newDoc.setExternalSourceType("ps");
					}
					newDoc.setExternalSourceId(Long.toString(searchSource.getID()));
					
					List<RegisterDocumentI> savedReferences = new ArrayList<RegisterDocumentI>();
					for (RegisterDocumentI documentI : sourceDocumentI.getReferences()) {
						savedReferences.add(documentI);
					}
					
					newDoc.getReferences().clear();
					addedDocuments.add(newDoc);
					alreadyAnalyzedDocuments.add(sourceDocumentI);
					docManager.add(newDoc);
					
					ImageI image = newDoc.getImage();
					
					if(image != null) {
						image.setViewCount(new AtomicInteger());	//this image was never viewed
					
//						String oldImageFileName = image.getFileName();
//	        			image.setFileName( oldImageFileName.replace( sourceDocumentI.getId(), newDoc.getId()));
//	        			image.setPath(this.getImagePath() + image.getFileName());
						
						OcrFileArchiveSaver ocrFileArchiveSaverSource = new OcrFileArchiveSaver(searchSource);
		        		OcrFileArchiveSaver ocrFileArchiveSaverDestination = new OcrFileArchiveSaver(this);
		        		
		        		List<File> ocrSourceFiles = ocrFileArchiveSaverSource.getLocalFiles(true, newDoc.getId());
		        		String ocrFileArchiveFolderPath = OcrFileArchiveSaver.getOcrFileLocalFolder(this);
		        		for (File file : ocrSourceFiles) {
		        			try {
			        			String fileName = file.getName();
			        			File copiedFile = new File(ocrFileArchiveFolderPath + File.separator + fileName);
//			        			if(fileName.endsWith("." + OcrFileArchiveSaver.SMART_VIEWER_JS_EXT) ) {
//			        				String fileContent = org.apache.commons.io.FileUtils.readFileToString(file);
//									fileContent = fileContent.replaceAll(sourceDocumentI.getId(), newDoc.getId());
////									fileContent = fileContent.replaceAll("searchId=" + searchSource.searchID, "searchId=" + this.getID());
//									org.apache.commons.io.FileUtils.writeStringToFile(copiedFile, fileContent);
//			        			} else {
			        				org.apache.commons.io.FileUtils.copyFile(file, copiedFile);
//			        			}
			        			ocrFileArchiveSaverDestination.addLocalFile(copiedFile);
		        			} catch (Exception e) {
		        				logger.error("Problem treating file " + file.getPath(), e);
		        			}
		        			
						}
		        		
		        		new Thread(
		        				ocrFileArchiveSaverDestination, 
		        				"OcrFileArchiveSaver on merge " + searchSource.getID() + " to " + this.getID() + " - document " + newDoc.getNiceFullFileName())
		        			.start();
	        		
					}
					
					for (RegisterDocumentI documentI : savedReferences) {
						internalMergeOneDocument(searchSource, county, addedDocuments, updatedDocuments, alreadyAnalyzedDocuments, docManagerSource, (RegisterDocumentI)documentI, true);
					}
					
					
				}
				
			}
			
		}
	}

	private void mergeExistingDocument(RegisterDocumentI sourceDocumentI, RegisterDocumentI destinationDocument, List<DocumentI> updatedDocuments) {
		sourceDocumentI.mergeDocumentsInformation(destinationDocument, getID(), true, false);
		updatedDocuments.add(destinationDocument);
  				
		Set<String> codeBookSourceSet = sourceDocumentI.getCodeBookCodes();
		for (String codeBookCode : codeBookSourceSet) {
			BoilerPlateObject codeBookCodeSource = sourceDocumentI.getCodeBookObject(codeBookCode);
			//skip deleted codes
			if(!codeBookCodeSource.isManuallyDeleted()) {
				BoilerPlateObject codeBookCurrentObject = destinationDocument.getCodeBookObject(codeBookCode);
				
				if(codeBookCurrentObject != null) {
					if(!codeBookCurrentObject.hasModifiedStatement()) {
						if(codeBookCodeSource.hasModifiedStatement()) {
							//copy statement if the source is manually modified
							codeBookCurrentObject.setModifiedStatement(codeBookCodeSource.getModifiedStatement());
						}
					}
				} else {
					//I do not have this code but it might have been manually added so let's try to copy it
					if(codeBookCodeSource.isManuallyAdded()) {
						destinationDocument.addCodeBookCode(codeBookCodeSource.clone());
					}
				}
			}
		}
	}

	/**
	 * Deletes all documents that were added from the source source in a previous MERGE operation
	 * @param searchIdSource searchId of the search previously MERGED with current one
	 * @return list of removed documents
	 */
	public DocumentI[] unmergeSearchWith(long searchIdSource) {
		List<DocumentI> deletedDocuments = new ArrayList<DocumentI>();
		try {
			docManager.getAccess();
			List<RegisterDocumentI> allRoDocuments = docManager.getRoLikeDocumentList(false);
			
			String searchIdSourceString = Long.toString(searchIdSource);
			
			for (RegisterDocumentI registerDocumentI : allRoDocuments) {
				if(searchIdSourceString.equals(registerDocumentI.getExternalSourceId())) {
					try {
						if(docManager.remove(registerDocumentI)) {
							TsdIndexPageServer.unMapTransactionDocId(registerDocumentI, getID());
							DBManager.deleteDocumentIndex(this, registerDocumentI.getIndexId());
							if("SF".equalsIgnoreCase(registerDocumentI.getDataSource()) && registerDocumentI instanceof SSFPriorFileDocument) {
								SSFPriorFileDocument ssfDoc = (SSFPriorFileDocument)registerDocumentI;
								DBManager.deleteSfDocumentIndex(this, ssfDoc.getSsfIdexId());
							}
							deletedDocuments.add(registerDocumentI);
						}
					} catch (Exception e) {
						logger.error("Error deleting " + registerDocumentI + " from search " + getID(), e);
					}
				}
			}
			
			for (DocumentI registerDocumentI : docManager.getOutOfRangeDocumentsSet()) {
				if(searchIdSourceString.equals(registerDocumentI.getExternalSourceId())) {
					try {
						if(docManager.removeOutOfRange(registerDocumentI)) {
							TsdIndexPageServer.unMapTransactionDocId(registerDocumentI, getID());
							DBManager.deleteDocumentIndex(this, registerDocumentI.getIndexId());
							deletedDocuments.add(registerDocumentI);
						}
					} catch (Exception e) {
						logger.error("Error deleting " + registerDocumentI + " from search " + getID(), e);
					}
				}
			}
			
		} finally {
			docManager.releaseAccess();
		}
		return deletedDocuments.toArray(new DocumentI[deletedDocuments.size()]);
	}
	
	public List<NoteMapper> getNotes(long searchId){
		
		return NoteMapper.getAllNotes(searchId);
	}
	
	public void setNotes(List<NoteMapper> notes){
		if (notes != null){
			for (NoteMapper note : notes) {
				try {
					note.setNoteSearchId(this.getID());
					NoteMapper.setSearchNote(note);
				} catch (Exception e) {
				}
			}
		}
	}
	
	/**
	 * see {@link #startIntervalWorkDate}
	 * @return can be null if not initialized
	 */
	public Date getStartIntervalWorkDate() {
		return startIntervalWorkDate;
	}
	/**
	 * see {@link #startIntervalWorkDate}
	 * @param startIntervalWorkDate
	 */
	public void setStartIntervalWorkDate(Date startIntervalWorkDate) {
		this.startIntervalWorkDate = startIntervalWorkDate;
	}

	private static String[] getServerTypes() {
		if (serverTypes == null) {
			serverTypes = HashCountyToIndex.getServerTypesDescription();
		}
		return serverTypes;
	}

	public void copyDatabaseFieldsFrom(Search sourceSearch) {
		SimpleJdbcTemplate jdbc = DBManager.getSimpleTemplate();
		Date tsrInitialDate = jdbc.queryForObject(
				"SELECT " + DBConstants.FIELD_SEARCH_TSR_INITIAL_DATE + " FROM " + DBConstants.TABLE_SEARCH + " where " + DBConstants.FIELD_SEARCH_ID + " = ?", 
				Date.class, 
				sourceSearch.getID());
		if(tsrInitialDate != null) {
			jdbc.update("UPDATE " + DBConstants.TABLE_SEARCH + " set " + DBConstants.FIELD_SEARCH_TSR_INITIAL_DATE + " = ? where " + DBConstants.FIELD_SEARCH_ID + " = ?", 
					tsrInitialDate,
					getID());
		}
		
	}

}
