package ro.cst.tsearch.utils;

import ro.cst.tsearch.bean.ErrorRequestBean;
import ro.cst.tsearch.database.rowmapper.ImageCount;
import ro.cst.tsearch.database.rowmapper.OrderCount;
import ro.cst.tsearch.database.rowmapper.SearchExternalFlag;
import ro.cst.tsearch.database.rowmapper.SearchUserTimeMapper;


public interface DBConstants {
	
	//generic
	public static final int DB_OPERATION_ADD					=	1;
	public static final int DB_OPERATION_UPDATE					=	2;
	public static final int DB_OPERATION_DELETE					=	3;
	
	//name formats
	public static final int NAMES_NO_CHANGE = -1;
	public static final int NAMES_UPPERCASE = 1;
	public static final int NAMES_LOWERCASE = 2;
	public static final int NAMES_TITLECASE = 3;
	public static final int NAMES_FORMAT_LFM = 1;
	public static final int NAMES_FORMAT_FML = 2;
	
	// sequence
	public static final String SEQ_USER							=	"SEQ_USER";
	public static final String SEQ_AGENT						= 	"SEQ_AGENT";
	public static final String SEQ_COMPANY						=	"SEQ_COMPANY";
	public static final String SEQ_SEARCH						=	"SEQ_SEARCH";
	public static final String SEQ_PROPERTY						=	"SEQ_PROPERTY";
	public static final String SEQ_PAYRATE						= 	"SEQ_PAYRATE";
	public static final String SEQ_DUE_DATE						= 	"SEQ_DUE_DATE";
	public static final String SEQ_USER_RATE					=	"SEQ_USER_RATE"; 		
	public static final String SEQ_TABLE_SEARCH_ID				= 	"SEQ_TABLE_SEARCH_ID";
	
	//retCodes for user
	public static final int RETCODE_USER_ALREADY_EXIST			=	-1;
	
	//code that signifies that an entry in the TS_SEARCH is not valid yet
	public static final long SEARCH_NOT_SAVED					=	-1000;
	
	// tables
	public static final String TABLE_USER						=	"ts_user";
	public static final String TABLE_AGENT						=	"ts_agent";
	public static final String TABLE_USER_COMMUNITY				=	"ts_user_community";
	public static final String TABLE_COMPANY					=	"ts_company";
	public static final String TABLE_USER_SETTINGS  			=	"ts_user_settings";
	public static final String TABLE_USER_GROUP					=	"ts_user_group";
	public static final String TABLE_USER_PHOTO					=	"ts_user_photo";
	public static final String TABLE_USER_RESUME				=	"ts_user_resume";
	public static final String TABLE_USER_RATING				=	"ts_user_rating";
	public static final String TABLE_USER_TEMPLATES				= 	"ts_user_templates";
	public static final String TABLE_GROUP						=	"ts_group";
	public static final String TABLE_CATEGORY					=	"ts_category";
	public static final String TABLE_COMMUNITY					=	"ts_community";
	public static final String TABLE_COMMUNITY_LOGO				=	"ts_community_logo";
	public static final String TABLE_COMMUNITY_TEMPLATES		=	"ts_community_templates";
	public static final String TABLE_COMMUNITY_TERMS_OF_USE		=	"ts_community_terms_of_use";
	public static final String TABLE_COMMUNITY_SEQ				=	"ts_community_seq";
	public static final String TABLE_COMMUNITY_DISCOUNT			=	"ts_community_discount";
	public static final String TABLE_COMMUNITY_SITES			=	"ts_community_sites";
	public static final String TABLE_SETTINGS					=	"ts_settings";
	public static final String TABLE_SEARCH						=	"ts_search";
	public static final String TABLE_SUBDIVISIONS_HAMILTON		=	"ts_subdivisions_hamilton";
	public static final String TABLE_SUBDIVISIONS_OAKLAND		=   "ts_subdivisions_oakland";
	public static final String TABLE_SUBDIVISIONS_MACOMB		=   "ts_subdivisions_macomb";
	public static final String TABLE_SUBDIVISIONS_IL_KANE		=   "ts_subdivisions_il_kane";
	public static final String TABLE_SUBDIVISIONS_OAKLAND_TYPE	=   "ts_subdivisions_oakland_type";	
	public static final String TABLE_SUBDIVISIONS				=	"ts_subdivisions";
	public static final String TABLE_LOCKED_SEARCHES			=	"locked_searches";
	public static final String TABLE_COUNTY						=	"ts_county";
	public static final String TABLE_STATE						=	"ts_state";
	public static final String TABLE_FILTER_TEST_FILES			=	"ts_filter_test_files";
	public static final String TABLE_MIMACOMBCO_PARTIES			=	"ts_MIMacombCOParties";
	public static final String TABLE_MIWAYNEPR_PARTIES			=	"ts_MIWaynePR_Parties";
	public static final String TABLE_PAYRATE					=	"ts_payrate";
	public static final String TABLE_SEARCH_STATUS				=	"ts_search_status";
	public static final String TABLE_DUE_DATE					=	"ts_due_date";
	public static final String TABLE_CITY						=	"ts_city";
	public static final String TABLE_TESTSUITE					=	"ts_testsuite";
	public static final String TABLE_TESTCASE					=	"ts_testcase";
	public static final String TABLE_USER_COUNTY				=	"ts_user_county";
	public static final String TABLE_PRESENCE_TEST				=	"ts_presence_test";
	public static final String TABLE_PRESENCE_TEST_SERVERS		=	"ts_presence_test_servers";
	public static final String TABLE_EXTRA_TABLE				=	"ts_extra_table";
	public static final String TABLE_SEARCH_DATA				= 	"ts_search_data1";
	public static final String TABLE_ATS_FOLDER_FILES			=	"ts_atsfolder";
	public static final String TABLE_DESCRIPTIBLE				=	"ts_descriptible_parent";
	public static final String TABLE_PASSWORDS					=	"ts_passwords";
	public static final String TABLE_USAGE_INFO					=	"ts_usage_info";
	public static final String TABLE_LOAD_INFO					=	"ts_load_info";
	public static final String TABLE_SEARCH_STATISTICS			=	"ts_search_statistics";
	public static final String TABLE_USAGE_DISK					=	"ts_usage_disk";
	public static final String TABLE_SERVER						=	"ts_server";
	public static final String TABLE_LBS_SOURCES				=	"ts_lbs_sources";
	public static final String TABLE_CONFIGS					=	"ts_configs";
	public static final String TABLE_TAX_DATES					=	"ts_tax_dates";
	public static final String TABLE_SEARCH_PRODUCTS			=	"ts_search_products";
	public static final String TABLE_COMMUNITY_PRODUCTS			=	"ts_community_products";
	
	public static final String TABLE_SEARCH_DATA_BLOB			=	"ts_search_data_blob";
	public static final String TABLE_SEARCH_FLAGS				= 	"ts_search_flags";
	public static final String TABLE_SEARCH_FILTERS				=	"search_filters";
	public static final String TABLE_IMAGE_COUNT				=	ImageCount.TABLE_IMAGE_COUNT;
	public static final String TABLE_ORDER_COUNT				=	OrderCount.TABLE_ORDER_COUNT;
	
	public static final String TABLE_INTERFACE_SETTINGS			=	"ts_interface_settings";
	public static final String TABLE_COUNTY_COMMUNITY			=	"county_community";
	
	public static final String TABLE_ORDERS_GRAPH				= 	"ts_orders_graph";
	public static final String TABLE_DOCUMENTS_INDEX			=	"ts_documents_index";
	public static final String TABLE_SF_DOCUMENTS_INDEX			=	"ts_sf_documents_index";
	public static final String TABLE_THREAD_LOGS				=	"thread_logs";
	
	public static final String TABLE_ERROR_REQUEST				=	ErrorRequestBean.TABLE_ERROR_REQUEST;
	
	public static final String TABLE_MAP_FAKE_COUNTY_INDEX_FOR_EP = "ts_map_fake_county_index_for_ep";
	public static final String TABLE_MAP_SITE_TYPE_TO_P2		=	"ts_map_site_type_to_p2";
	
	//implemented sites
	public static final String TS_SITES = "ts_sites";
	
	public static final String TABLE_TSR_INSTANCES				=	"tsr_instances";
	public static final String TABLE_EXPENSES                   =   "ts_expenses";
	
	//presence test table
	public static final String TABLE_PARENTSITE_TESTS =  "ts_parentsite_tests";
	
	//presence test result
	public static final String TABLE_PARENTSITE_TESTS_RESULT =  "ts_parentsite_test_result";
	
	public static final String TABLE_NAME_SYNONYMS 				= 	"name_synonyms";
	public static final String TABLE_RECOVER_DOCUMENTS			=	"documents";
	public static final String TABLE_MODULES					=	"modules";
	public static final String TABLE_MODULE_TO_DOCUMENT			=	"module_to_document";
	
	public static final String TABLE_FVS_DATA					=	"fvs_data";
	
	public static final String TABLE_SEARCH_EXTERNAL_FLAGS		=	SearchExternalFlag.TABLE_SEARCH_EXTERNAL_FLAGS;
	public static final String TABLE_FIELD_SEARCH_ID		    =	SearchExternalFlag.FIELD_SEARCH_ID;
	public static final String TABLE_FIELD_SO_ORDER_ID          =   SearchExternalFlag.FIELD_SO_ORDER_ID;
	
	public static final String TABLE_SEARCH_USER_TIME			=	SearchUserTimeMapper.TABLE_SEARCH_USER_TIME;
	
	public static final String GROUP_ID				= 		"GROUP_ID";
	public static final String ABSTRACTOR_ID		=		"ABSTRACT_ID";
	public static final String AGENT_ID				=		"AGENT_ID";
	// user settings
	public static final String USER_PAGES			=		"PAGES";
	public static final int USERS_ROWNUM			=		10;
	public static final String USER_RESUME_TITLE	= 		"RESUME_NAME";
	public static final String USER_RESUME_SIZE	=		"RESUME_SIZE"; 
	
	// This should be the smallest value of their data type. Not -1.  !!!!!! FIX !!!!!
	/** default value for int type */
	public static final int BLANK_VALUE_INT 						= Integer.MIN_VALUE; 
	/** default value for long type */
	public static final long BLANK_VALUE_LONG 					= Long.MIN_VALUE;
	/** default value for double type */
	public static final double BLANK_VALUE_DOUBLE 				= -922337203685477D; // this is the smallest money value for MSSQL

	
	
	//ts_sites info
	public static final String FIELD_SITES_ID_COUNTY							= "ID_COUNTY";
    public static final String FIELD_SITES_SITE_TYPE							= "SITE_TYPE";
    public static final String FIELD_SITES_P2									= "P2";
    public static final String FIELD_SITES_IS_ENDBLED							= "IS_ENABLED";
    public static final String FIELD_SITES_ADDR_TOKEN_MISS						= "ADRESS_TOKEN_MISS";
    public static final String FIELD_SITES_EFFECTIVE_START_DATE					= "effective_start_date";
    public static final String FIELD_SITES_NUMBER_OF_YEARS						= "number_of_years";
    
    
    //ts_MAP_FAKE_COUNTY_INDEX_FOR_EP info
    public static final String FIELD_MAP_FAKE_COUNTY_INDEX_FOR_EP_REAL_COUNTY_INDEX ="REAL_COUNTY_INDEX";
    public static final String FIELD_MAP_FAKE_COUNTY_INDEX_FOR_EP_FAKE_COUNTY_INDEX ="FAKE_COUNTY_INDEX";
    
    //ts_map_site_type_to_p2
    public static final String FIELD_MAP_SITE_TO_P2_SITE_TYPE	=	"site_type";
    public static final String FIELD_MAP_SITE_TO_P2_SITE_ABREV	=	"site_abrev";
    public static final String FIELD_MAP_SITE_TO_P2_P2			=	"p2";
    public static final String FIELD_MAP_SITE_TO_P2_DESCRIPTION	=	"description";
    
    
    //ts_COUNTY  info
    public static final String FIELD_COUNTY_NAME  = "NAME";
    public static final String FIELD_COUNTY_ID  = "ID";
    public static final String FIELD_COUNTY_STATE_ID  = "STATE_ID";
    public static final String FIELD_COUNTY_REAL_INDEX_COUNTY = "REAL_INDEX_COUNTY";
	public static final String FIELD_COUNTY_FIPS_ID  = "countyFIPS";
	public static final String FIELD_COUNTY_DOCTYPE  = "doctype";
	
	//county_community  info
	public static final String FIELD_COUNTY_COMMUNITY_COUNTY_ID								=  "county_id";
	public static final String FIELD_COUNTY_COMMUNITY_COMMUNITY_ID							=  "community_id";
	public static final String FIELD_COUNTY_COMMUNITY_DEFAULT_CERTIFICATION_DATE_OFFSET		=  "default_certification_date_offset";
	public static final String FIELD_COUNTY_COMMUNITY_TEMPLATE_ID							=  "templateId";
	public static final String FIELD_COUNTY_COMMUNITY_DEFAULT_OFFICIAL_START_DATE_OFFSET	=  "default_start_date_offset";
	
	//ts_STATE info
	public static final String FIELD_STATE_ID = "id";
	public static final String FIELD_STATE_NAME = "name";
	public static final String FIELD_STATE_ABV = "stateabv";
	public static final String FIELD_STATE_FIPS = "stateFIPS";
	
    //ts_community
    public static final String FIELD_COMMUNITY_CATEG_ID = "categ_id";
    public static final String FIELD_COMMUNITY_CATEG_ID_BACKUP = "categ_id_backup";
    public static final String FIELD_COMMUNITY_COMM_ID = "comm_id";
    public static final String FIELD_COMMUNITY_COMM_NAME = "comm_name";
    public static final String FIELD_COMMUNITY_DESCRIPTION = "description";
    public static final String FIELD_COMMUNITY_COMM_ADMIN = "comm_admin";
    public static final String FIELD_COMMUNITY_CTIME = "ctime";
    public static final String FIELD_COMMUNITY_LAST_ACCESS = "last_access";
    public static final String FIELD_COMMUNITY_ADDRESS = "address";
    public static final String FIELD_COMMUNITY_PHONE = "phone";
    public static final String FIELD_COMMUNITY_EMAIL = "email";
    public static final String FIELD_COMMUNITY_CONTACT_PERSON = "contact_person";
    public static final String FIELD_COMMUNITY_COMM_CODE = "comm_code";
    public static final String FIELD_COMMUNITY_SEE_ALSO = "see_also";
    public static final String FIELD_COMMUNITY_INV_DUE_OFFSET = "inv_due_offset";
    public static final String FIELD_COMMUNITY_CRNCY_ID = "crncy_id";
    public static final String FIELD_COMMUNITY_TEMPLATE = "template";
    public static final String FIELD_COMMUNITY_TIMESTAMP = "timestamp";
    public static final String FIELD_COMMUNITY_TSR_INDEX = "tsd_index";
    public static final String FIELD_COMMUNITY_COMMITMENT = "commitment";
    /**
	 * Maps the field <code>templates_path</code> from table <b>ts_community</b>
	 */
    public static final String FIELD_COMMUNITY_TEMPLATES_PATH = "templates_path";
    public static final String FIELD_COMMUNITY_OFFSET = "offset";
    public static final String FIELD_COMMUNITY_AUTOFILEID = "autofileid";
    public static final String FIELD_COMMUNITY_DEFAULT_SLA = "default_sla";
    
    //ts_community_sites table info
    public static final String FIELD_COMMUNITY_SITES_COMMUNITY_ID = "community_id";
    public static final String FIELD_COMMUNITY_SITES_COUNTY_ID = "county_id";
    public static final String FIELD_COMMUNITY_SITES_SITE_TYPE = "site_type";
    public static final String FIELD_COMMUNITY_SITES_CITY_TYPE_P2 = "city_type_p2";
    public static final String FIELD_COMMUNITY_SITES_ENABLE_STATUS = "enableStatus";
    
	//OAKLAND SUBDIVISIONS TABLE INFO
	public static final String FIELD_SUBDIVISIONS_OAKLAND_ID							= "ID";
    public static final String FIELD_SUBDIVISIONS_OAKLAND_NAME							= "NAME";
    public static final String FIELD_SUBDIVISIONS_OAKLAND_CODE							= "CODE";
    public static final String FIELD_SUBDIVISIONS_OAKLAND_AREA							= "AREA";
    public static final String FIELD_SUBDIVISIONS_OAKLAND_TYPEID						= "TYPE_ID";
    
    //IL KANE SUBDIVISIONS TABLE INFO
    public static final String FIELD_SUBDIVISIONS_IL_KANE_CODE							= "code";
    public static final String FIELD_SUBDIVISIONS_IL_KANE_NAME							= "name";
    public static final String FIELD_SUBDIVISIONS_IL_KANE_PLAT_DOC						= "plat_doc";
    
	//Macomb SUBDIVISIONS TABLE INFO
	public static final String FIELD_SUBDIVISIONS_MACOMB_ID							= "ID";
    public static final String FIELD_SUBDIVISIONS_MACOMB_NAME							= "NAME";
    public static final String FIELD_SUBDIVISIONS_MACOMB_CODE							= "CODE";
    public static final String FIELD_SUBDIVISIONS_MACOMB_AREA							= "AREA";
    public static final String FIELD_SUBDIVISIONS_MACOMB_PHASE							= "PHASE";    
    public static final String FIELD_SUBDIVISIONS_MACOMB_TYPEID						= "TYPE_ID";
    
    //OAKLAND SUBDIVISIONS TYPE TABLE INFO
	public static final String FIELD_SUBDIVISIONS_OAKLAND_TYPE_ID						= "ID";
	public static final String FIELD_SUBDIVISIONS_OAKLAND_TYPE_NAME						= "NAME";
	
	
	//COMMUNITY DISCOUNT TABLE INFO
	public static final String FIELD_COMMUNITY_DISCOUNT_COMM_ID							= "COMM_ID";
	public static final String FIELD_COMMUNITY_DISCOUNT_VALUE							= "DISC_VALUE";
	public static final String FIELD_COMMUNITY_DISCOUNT_NO_SEARCHES						= "NO_SEARCHES";
	public static final String FIELD_COMMUNITY_DISCOUNT_MONTH							= "MONTH";
	public static final String FIELD_COMMUNITY_DISCOUNT_YEAR							= "YEAR";
	public static final String FIELD_COMMUNITY_DISCOUNT_NO_SEARCHES_LEFT				= "NOS_LEFT";
	
	//MACOMB SUBDIVISIONS AND CONDOMINIUMS INFO
    public static final String FIELD_SUBDIVISIONS_MACOMB_SUBDIV						= "MACOMB_SUBDIV";
    public static final String FIELD_SUBDIVISIONS_MACOMB_CONDO						= "MACOMB_CONDO";

    //ts_search table fields
    public static final String FIELD_SEARCH_ID											=   "id";
    public static final String FIELD_SEARCH_OWNER_ID									=	"owner_id";
    public static final String FIELD_SEARCH_BUYER_ID									=	"buyer_id";
    public static final String FIELD_SEARCH_AGENT_ID									=	"agent_id";
    public static final String FIELD_SEARCH_ABSTRACT_ID									=	"abstract_id";
    public static final String FIELD_SEARCH_PROPERTY_ID									=	"property_id";
    public static final String FIELD_SEARCH_TYPE										=	"search_type";
    
    //fvs_data table fields
    public static final String FIELD_FVS_SEARCH_ID 						= "search_id";
	public static final String FIELD_FVS_FLAG 							= "flag";
	public static final String FIELD_FVS_ABSTR_FILE_NO 					= "abstr_fileno";
	public static final String FIELD_FVS_RUN_TIME 						= "run_time";
	public static final String FIELD_FVS_FLAG_DATE 						= "flag_date";
	public static final String FIELD_FVS_COMM_ID 						= "comm_id";
	public static final String FIELD_FVS_AGENT_ID 						= "agent_id";
	public static final String FIELD_FVS_UPDATES_RUNNED 				= "updates_runned";
	public static final String FIELD_FVS_COUNTY_ID 						= "county_id";
    
    /**
     * This field can be found in Search Page and is the File ID of the Agent<br> 
     * Should be mapped with ro.cst.tsearch.bean.SearchAttributes.ORDERBY_FILENO
     */
    public static final String FIELD_SEARCH_AGENT_FILENO								=	"agent_fileno";
    /**
     * This field is shown in dashboard in TSR File ID column
     */
    public static final String FIELD_SEARCH_ABSTRACT_FILENO								=	"abstr_fileno";
    
    /**
     * This field can be found in Search Page and is the File ID of the Abstractor<br> 
     * Should be mapped with ro.cst.tsearch.bean.SearchAttributes.ABSTRACTOR_FILENO
     * @since 11 November 2010
     */
    public static final String FIELD_SEARCH_FILE_ID										=	"file_id";
    
    public static final String FIELD_SEARCH_SDATE										=	"sdate";
    public static final String FIELD_SEARCH_PAYRATE_ID									=	"payrate_id";
    public static final String FIELD_SEARCH_TSR_FILE_LINK								=	"tsr_file_link";
    public static final String FIELD_SEARCH_TSR_SENT_TO									=	"tsr_sent_to";
    public static final String FIELD_SEARCH_USER_RATING_ID								=	"user_rating_id";
    public static final String FIELD_SEARCH_TSR_FOLDER									=	"tsr_folder";
    public static final String FIELD_SEARCH_STATUS										=	"status";
    public static final String FIELD_SEARCH_COMM_ID										=	"comm_id";
    public static final String FIELD_SEARCH_TSR_DATE									=	"tsr_date";
    public static final String FIELD_SEARCH_TSR_INITIAL_DATE							=	"tsr_initial_date";
    public static final String FIELD_SEARCH_OPEN_DATE									=	"open_date";
    public static final String FIELD_SEARCH_NOTE_CLOB									=	"note_clob";
    public static final String FIELD_SEARCH_NOTE_STATUS									=	"note_status";
    public static final String FIELD_SEARCH_AGENT_RATING_ID								=	"agent_rating_id";
    public static final String FIELD_SEARCH_DISCOUNT_RATIO								=	"discount_ratio";
    public static final String FIELD_SEARCH_TIME_ELAPSED								= 	"time_elapsed";
    public static final String FIELD_SEARCH_LEGAL_ID									=	"legal_id";
    //public static final String FIELD_SEARCH_STATUSADMIN								=	"statusadmin";
    //public static final String FIELD_SEARCH_STATUSCOMADMIN							=	"statuscomadmin";
    public static final String FIELD_SEARCH_SERVER_NAME									= 	"aux_server_name";
    public static final String FIELD_SEARCH_LAST_SAVE_DATE								= 	"lastSaveDate";
    public static final String FIELD_SEARCH_TSRI_LINK									= 	"tsri_link";
    /**
     * This field should be used to filter searches by date in Reports Page<br>
     * It should contain TSR_DATE if available otherwise SDATE
     */
    public static final String FIELD_SEARCH_REPORTS_DATE								=	"reports_date";
    
    /**
     * 	This field is used after the TSR is first created to keep the abstractor that reopened it 
     * (in case it's different than the abstractor that finished it)<br>
     * 	If the search is a clone, it will hold the original abstractor 
     * (from the original search that was cloned) 
     * @since 18 August 2011
     */
    public static final String FIELD_SEARCH_SEC_ABSTRACT_ID								=	"sec_abstract_id";
    
    /*
    // ts_orders_graph
    public static final String FIELD_ORDERS_GRAPH_HOUR 									=  "beginHour";
    public static final String FIELD_ORDERS_GRAPH_NUMBER_PERHOUR 						=  "numberPerHour";
    public static final String FIELD_ORDERS_GRAPH_SERVER_NAME							=  "serverName";
    */
    
    //search data table info
	public static final String FIELD_SEARCH_DATA_SEARCHID								= "searchId";
	public static final String FIELD_SEARCH_DATA_DATESTRING								= "dateString";
	public static final String FIELD_SEARCH_DATA_CONTEXT								= "context";
	public static final String FIELD_SEARCH_DATA_VERSION								= "version";
	
	//atsfolder table fields
	public static final String FIELD_ATSFOLDER_FILENAME								= "fileName";
	public static final String FIELD_ATSFOLDER_CONTENTS								= "contents";
	
	//ts_passwords fields
	public static final String FIELD_PASSWORD_MACHINE_NAME									= "machineName";
	public static final String FIELD_PASSWORD_NAME									= "passwordName";
	public static final String FIELD_PASSWORD_VALUE									= "passwordValue";
	public static final String FIELD_PASSWORD_SITE									= "siteName";
	public static final String FIELD_PASSWORD_COMMUNITY_ID									= "comm_id";
	
	//ts_community_templates_fields
	public static final String FIELD_COMMUNITY_TEMPLATES_ID							= "template_id";
	public static final String FIELD_COMMUNITY_TEMPLATES_COMM_ID				= "comm_id";
	public static final String FIELD_COMMUNITY_TEMPLATES_NAME					= "name";
	/**
	 * Maps the field <code>path</code> from table <b>ts_community_templates</b>
	 */
	public static final String FIELD_COMMUNITY_TEMPLATES_FILENAME				= "path";
	public static final String FIELD_COMMUNITY_TEMPLATES_LAST_UPDATE		= "last_update";
	public static final String FIELD_COMMUNITY_TEMPLATES_SHORT_NAME		= "short_name";
	public static final String FIELD_COMMUNITY_TEMPLATES_CONTENT				= "content";
	
	//ts_filter_test_files
	public static final String FIELD_FILTER_TESTFILES_ID = "id";
	public static final String FIELD_FILTER_TESTFILES_NAME = "name";
	public static final String FIELD_FILTER_TESTFILES_CONTENT = "content";
	
	//ts_user
	public static final String FIELD_USER_ID															= "user_id";
	public static final String FIELD_USER_LOGIN													= "login";
	public static final String FIELD_USER_PASSWORD_PLAIN											= "passwd";
	public static final String FIELD_USER_LAST_NAME											= "last_name";
	public static final String FIELD_USER_FIRST_NAME											= "first_name";
	public static final String FIELD_USER_MIDDLE_NAME										= "middle_name";
	public static final String FIELD_USER_COMPANY												= "company";
	public static final String FIELD_USER_EMAIL													= "email";
	public static final String FIELD_USER_A_EMAIL												= "a_email";
	public static final String FIELD_USER_PHONE													= "phone";
	public static final String FIELD_USER_A_PHONE												= "a_phone";
	public static final String FIELD_USER_ICQ_NUMBER										= "icq_number";
	public static final String FIELD_USER_AOL_NAME											= "aol_screen_name";
	public static final String FIELD_USER_YAHOO_NAME										= "yahoo_messager";
	public static final String FIELD_USER_WORK_ADDRESS									= "waddress";
	public static final String FIELD_USER_WORK_CITY											= "wcity";
	public static final String FIELD_USER_WORK_ZIP											= "wzcode";
	public static final String FIELD_USER_WORK_COUNTRY									= "wzcountry";
	public static final String FIELD_USER_WORK_COMPANY									= "wcompany";
	public static final String FIELD_USER_EDIT_HIMSELF										= "edithimself";
	public static final String FIELD_USER_GID														= "gid";
	public static final String FIELD_USER_LAST_LOGIN											= "last_login";
	public static final String FIELD_USER_DELETED_FLAG										= "deleted_flag";
	public static final String FIELD_USER_MESSAGE												= "umessage";
	public static final String FIELD_USER_LAST_COMMUNITY								= "last_community";
	public static final String FIELD_USER_TIME_STAMP											= "time_stamp";
	public static final String FIELD_USER_PCARD_ID												= "pcard_id";
	public static final String FIELD_USER_WCARD_ID											= "wcard_id";
	public static final String FIELD_USER_DATE_OF_BIRTH									= "dateofbirth";
	public static final String FIELD_USER_PLACE													= "place";
	public static final String FIELD_USER_PADDRESS											= "paddress";
	public static final String FIELD_USER_PLOCATION											= "plocation";
	public static final String FIELD_USER_HPHONE												= "hphone";
	public static final String FIELD_USER_MPHONE												= "mphone";
	public static final String FIELD_USER_PAGER													= "pager";
	public static final String FIELD_USER_INSTANT_MESSENGER						    = "instant_messenger";
	public static final String FIELD_USER_MESSENGER_NUMBER							= "messenger_number";
	public static final String FIELD_USER_HCITY													= "hcity";
	public static final String FIELD_USER_HSTATE													= "hstate";
	public static final String FIELD_USER_HZIPCODE												= "hzipcode";
	public static final String FIELD_USER_HCOUNTRY											= "hcountry";
	public static final String FIELD_USER_COMM_ID												= "comm_id";
	public static final String FIELD_USER_AGENT_ID												= "agent_id";
	public static final String FIELD_USER_STREET_NO											= "streetno";
	public static final String FIELD_USER_STREET_DIRECTION								= "streetdirection";
	public static final String FIELD_USER_STREET_NAME										= "streetname";
	public static final String FIELD_USER_STREET_SUFFIX									= "streetsuffix";
	public static final String FIELD_USER_STREET_UNIT										= "streetunit";
	public static final String FIELD_USER_STATE_ID												= "state_id";
	public static final String FIELD_USER_DITRIBUTION_MODE								= "distribution_mode";
	public static final String FIELD_USER_ADDRESS												= "address";
	public static final String FIELD_USER_DELIV_TEMPLATES								= "deliv_templates";
	public static final String FIELD_USER_HIDDEN									= "hidden_flag";
	public static final String FIELD_USER_HIDDEN_BACKUP									= "hidden_flag_backup";
	public static final String FIELD_USER_INTERACTIVE  								= "interactive";
	public static final String FIELD_USER_OUTSOURCE  								= "outsource";
	public static final String FIELD_USER_RANDOM_TOKEN							=	"randomToken";
	public static final String FIELD_USER_LAST_PASSWORD_CHANGE_DATE				=	"lastPassChangeDate";
	public static final String FIELD_USER_PASSWORD_ENCRYPTED					= 	"password";
	public static final String FIELD_USER_NOTIFICATION_EXPIRE_PASS_SENT			=	"notificationExpirePassSent";
	
		
	//ts_usage_info
	public static final String FIELD_USAGE_INFO_ID								=	"id";
	public static final String FIELD_USAGE_INFO_CPU								=	"cpu";
	public static final String FIELD_USAGE_INFO_MEMORY							=	"memory";
	public static final String FIELD_USAGE_INFO_NETWORK							=	"network";
	public static final String FIELD_USAGE_INFO_TIMESTAMP						=	"timestamp";
	public static final String FIELD_USAGE_INFO_SERVERCONN						=	"server_conn";
	public static final String FIELD_USAGE_INFO_DBCONN							=	"db_conn";
	public static final String FIELD_USAGE_INFO_SERVER_NAME						=	"server_name";
	public static final String FIELD_USAGE_INFO_MEMORY_FREE						=	"MEMORY_FREE";
	public static final String FIELD_USAGE_INFO_LOAD_FACTOR						=	"load_factor";
	
	
	//ts_load_info
	public static final String FIELD_LOAD_INFO_ID								=	"id";
	public static final String FIELD_LOAD_INFO_CPU								=	"cpu";
	public static final String FIELD_LOAD_INFO_MEMORY							=	"memory";
	public static final String FIELD_LOAD_INFO_NETWORK							=	"network";
	public static final String FIELD_LOAD_INFO_TIMESTAMP						=	"timestamp";
	public static final String FIELD_LOAD_INFO_SERVERCONN						=	"server_conn";
	public static final String FIELD_LOAD_INFO_DBCONN							=	"db_conn";
	public static final String FIELD_LOAD_INFO_SERVER_NAME						=	"server_name";
	public static final String FIELD_LOAD_INFO_MEMORY_FREE						=	"MEMORY_FREE";
	public static final String FIELD_LOAD_INFO_LOAD_FACTOR						=	"load_factor";
	public static final String FIELD_LOAD_INFO_TYPE								=	"type";
	
	//ts_search_statistics
	public static final String FIELD_SEARCH_STATISTICS_ORDER_COUNT				=	"search_order_count";
	public static final String FIELD_SEARCH_STATISTICS_TYPE						=	"type";
	public static final String FIELD_SEARCH_STATISTICS_TIMESTAMP				=	"timestamp";
	public static final String FIELD_SEARCH_STATISTICS_SERVER_NAME				=	"server_name";
	
	//ts_usage_disk
	public static final String FIELD_USAGE_DISK_ID								=	"id";
	public static final String FIELD_USAGE_DISK_DISK1							=	"disk1";	//used for /data folder
	public static final String FIELD_USAGE_DISK_TIMESTAMP						=	"timestamp";
	public static final String FIELD_USAGE_DISK_SERVER_NAME						=	"server_name";
	public static final String FIELD_USAGE_DISK_MAX_VALUE						=	"disk1_max_value";
	public static final String FIELD_USAGE_DISK_NAME							=	"disk1_name";
	public static final String FIELD_USAGE_DISK_TYPE							=	"type";
	
	//ts_user_templates_fields
	public static final String FIELD_USER_TEMPLATES_ID							= 	"id";
	public static final String FIELD_USER_TEMPLATES_TEMPLATE_ID	    			= 	"template_id";
	public static final String FIELD_USER_TEMPLATES_USER_ID						= 	"user_id";
	public static final String FIELD_USER_TEMPLATES_ENABLE_PRODUCT				= 	"enableProduct";
	public static final String FIELD_USER_TEMPLATES_EXPORT_FORMAT					= 	"exportFormat";
	
	//ts_search_products
	public static final String FIELD_SEARCH_PRODUCTS_ID							=	"product_id";
	public static final String FIELD_SEARCH_PRODUCTS_NAME						=	"name";
	public static final String FIELD_SEARCH_PRODUCTS_ALIAS						=	"alias";
	public static final String FIELD_SEARCH_PRODUCTS_ORDER						=	"order";
	public static final String FIELD_SEARCH_PRODUCTS_SHORT_NAME					=	"shortName";
	
	//ts_server
	public static final String FIELD_SERVER_ID									=	"id";
	public static final String FIELD_SERVER_NAME								=	"server_name";
	public static final String FIELD_SERVER_FRIEND								=	"server_friend";
	public static final String FIELD_SERVER_LOAD								=	"load_factor";
	public static final String FIELD_SERVER_TIMESTAMP							=	"timestamp";
	public static final String FIELD_SERVER_ENABLED								=	"enabled";
	public static final String FIELD_SERVER_DEFAULT								=	"defaultDest";
	public static final String FIELD_SERVER_IP_ADDRESS							=	"ip_address";
	public static final String FIELD_SERVER_IP_MASK								=	"ip_mask";
	public static final String FIELD_SERVER_ALIAS								=	"alias";
	public static final String FIELD_SERVER_PATH								=	"path";
	public static final String FIELD_SERVER_IP_BACKUP							=	"ip_backup";
	public static final String FIELD_SERVER_LOGIN_TIME							=	"login_time";
	public static final String FIELD_SERVER_OVERRIDE_DESTINATION				=	"overrideDestination";
	public static final String FIELD_SERVER_CHECK_SEARCH_ACCESS					=	"check_search_access";
	public static final String FIELD_SERVER_CPU									=	"cpu";
	public static final String FIELD_SERVER_MEM									=	"memory_free";
	public static final String FIELD_SERVER_NET									=	"network";
	//ts_descriptible_parent
	public static final String FIELD_DP_FILE_NAME								= "filename";
	public static final String FIELD_DP_DATA_FILE								= "datafile";
	public static final String FIELD_DP_STATE 									       = "state";
	public static final String FIELD__DP_COUNTY 							       = "county";
	//ts_lbs_sources
	public static final String FIELD_LBS_SOURCES_ID								=	"id";
	public static final String FIELD_LBS_SOURCES_ADDRESS						=	"address";
	public static final String FIELD_LBS_SOURCES_NETMASK						=	"netmask";
	public static final String FIELD_LBS_SOURCES_ENABLE							= 	"enable";
	public static final String FIELD_LBS_SOURCES_SERVER_NAME					=	"server_name";
	public static final String FIELD_LBS_SOURCES_REDIRECT_ADDRESS				=	"redirect_address";
	public static final String FIELD_LBS_SOURCES_CLNT_USERNAME					=	"clnt_username";
	public static final String FIELD_LBS_SOURCES_CLNT_COMMNAME					=	"clnt_commname";
	
	//all parties tables (ts_MIMacombCOParties, ts_MIWaynePR_Parties)
	public static final String FIELD_PARTIES_TYPE								=	"party_type"; 
	public static final String FIELD_PARTIES_CATEGORY							=	"party_category";
	
	//ts_configs
	public static final String FIELD_CONFIGS_ID									=	"id";
	public static final String FIELD_CONFIGS_NAME								=	"name";
	public static final String FIELD_CONFIGS_VALUE								=	"value";
	
	//ts_parentsite_tests
	public static final String FIELD_PARENTSITE_TESTS_TEST_ID = "test_id";
	public static final String FIELD_PARENTSITE_TESTS_TEST_DATA =  "test_data";
	public static final String FIELD_SERVERNAME = "ServerName";
	public static final String FIELD_TEST_CASE_CREATION_DATE = "TestCaseCreationDate";
	public static final String FIELD_ENABLED_OR_DISABLED = "EnabledOrDisabled";
	
	//ts_parentsite_tests_result
	public static final String FIELD_PARENTSITE_TESTS_IDRESULT_ID = "test_id";
	public static final String FIELD_PARENTSITE_TESTS_DATE = "date";
	public static final String FIELD_PARENTSITE_TESTS_RESULT = "testResult";
	
	//ts_search_data_blob
	public static final String FIELD_SEARCH_DATA_BLOB_ID						=	"search_id";
	public static final String FIELD_SEARCH_DATA_BLOB_LEGAL_DESCR				=	"legal_description";
	public static final String FIELD_SEARCH_DATA_BLOB_NOTE						=	"note";
	public static final String FIELD_SEARCH_DATA_BLOB_ORDER						=	"searchOrder";
	public static final String FIELD_SEARCH_DATA_BLOB_LOG						=	"searchLog";
	public static final String FIELD_SEARCH_DATA_BLOB_INDEX						=	"searchIndex";
	
	
	//ts_search_flags
	public static final String FIELD_SEARCH_FLAGS_INVOICE						=	"invoice";
	public static final String FIELD_SEARCH_FLAGS_INVOICED						=	"invoiced";
	public static final String FIELD_SEARCH_FLAGS_PAID							=	"paid";
	public static final String FIELD_SEARCH_FLAGS_CONFIRMED						=	"confirmed";
	public static final String FIELD_SEARCH_FLAGS_ARCHIVED						=	"archived";
	public static final String FIELD_SEARCH_FLAGS_TSR_CREATED					=	"tsr_created";
	public static final String FIELD_SEARCH_FLAGS_CHECKED_BY					=	"checked_by";
	public static final String FIELD_SEARCH_FLAGS_ID							=	"search_id";
	public static final String FIELD_SEARCH_FLAGS_LEGAL_DESCR_STATUS			=	"legal_description_status";
	public static final String FIELD_SEARCH_FLAGS_WAS_OPENED					=	"was_opened";
	public static final String FIELD_SEARCH_FLAGS_PAID_CADM						=	"paid_cadm";
	public static final String FIELD_SEARCH_FLAGS_INVOICED_CADM					=	"invoiced_cadm";
	public static final String FIELD_SEARCH_FLAGS_INVOICE_CADM					=	"invoice";
	/**
	 * It is not really used so all references should be deleted 
	 */
	@Deprecated
	public static final String FIELD_SEARCH_FLAGS_ARCHIVED_CADM					=	"archived_cadm";
	public static final String FIELD_SEARCH_FLAGS_ORDER_STATUS					=	"searchOrderStatus";
	public static final String FIELD_SEARCH_FLAGS_LOG_STATUS					=	"searchLogStatus";
	public static final String FIELD_SEARCH_FLAGS_INDEX_STATUS					=	"searchIndexStatus";	
	public static final String FIELD_SEARCH_FLAGS_STARTER						=	"starter";
	public static final String FIELD_SEARCH_FLAGS_TO_DISK						=	"toDisk";
	public static final String FIELD_SEARCH_FLAGS_IS_CLOSED						=	"isClosed";
	public static final String FIELD_SEARCH_FLAGS_SOURCE_CREATION_TYPE			= 	"sourceCreationType";
	public static final String FIELD_SEARCH_FLAGS_FOR_REVIEW					= 	"forReview";
	public static final String FIELD_SEARCH_FLAGS_COLOR_FLAG					= 	"color_flag";
	public static final String FIELD_SEARCH_FLAGS_FOR_FVS						= 	"forFVS";
	public static final String FIELD_SEARCH_FLAGS_LOG_ORIGINAL_LOCATION			= 	"log_original_location";

	
	//ts_user_county
	public static final String FIELD_USER_COUNTY_USER_ID						=	"user_id";
	public static final String FIELD_USER_COUNTY_COUNTY_ID						=	"county_id";
	
	
	//ts_legal
	public static final String TABLE_LEGAL									= 	"ts_legal";
	public static final String FIELD_LEGAL_ID								=	"legalId";
	public static final String FIELD_LEGAL_SEARCH_ID						=	"searchId";
	public static final String FIELD_LEGAL_SUBDIVISION_ID					=	"subdivisionId";
	public static final String FIELD_LEGAL_TOWNSHIP_ID						=	"townshipId";
	public static final String FIELD_LEGAL_FREEFORM							=	"freeform";
	public static final String FIELD_LEGAL_APN								=	"apn";
	
	//ts_interface_settings
	public static final String SEARCH_PAGE_HEIGHT							= 	"search_page_height";
	public static final String SEARCH_PAGE_WIDTH							=	"search_page_width";
	public static final String REPORTS_HEIGHT								=	"reports_height";
	public static final String REPORTS_WIDTH								=	"reports_width";
	
	//ts_subdivision
	public static final String TABLE_SUBDIVISION							=	"ts_subdivision";
	public static final String FIELD_SUBDIVISION_ID							=	"subdivisionId";
	public static final String FIELD_SUBDIVISION_NAME						=	"name";
	public static final String FIELD_SUBDIVISION_LOT						=	"lot";
	public static final String FIELD_SUBDIVISION_BLOCK						=	"block";
	public static final String FIELD_SUBDIVISION_PHASE						=	"phase";
	public static final String FIELD_SUBDIVISION_UNIT						=	"unit";
	public static final String FIELD_SUBDIVISION_TRACT						=	"tract";
	
	//ts_township
	public static final String TABLE_TOWNSHIP								=	"ts_township";
	public static final String FIELD_TOWNSHIP_ID							=	"townshipId";
	public static final String FIELD_TOWNSHIP_RANGE							=	"`range`";
	public static final String FIELD_TOWNSHIP_TOWNSHIP						=	"township";
	public static final String FIELD_TOWNSHIP_SECTION						=	"section";
	
	
	//ts_legal_lot
	public static final String TABLE_LEGAL_LOT								=	"ts_legal_lot";
	public static final String FIELD_LEGAL_LOT_ID							=	"legalLotId";
	public static final String FIELD_LEGAL_LOT_SEARCH_ID					=	"searchId";
	public static final String FIELD_LEGAL_LOT_VALUE						=	"lotValue";
	
	public static final String TIME_GRAPHIC_VALUE							=	"value";
	public static final String TIME_GRAPHIC_TIME							=	"time";
	public static final String TIME_GRAPHIC_SERVER							= 	"server_name";
	public static final String TIME_GRAPHIC_EXTRA_FIELD						=	"extraField";
	
	public static final String LOAD_USAGE_HOUR								=	"1";
	public static final String LOAD_USAGE_DAY								=	"2";
	public static final String LOAD_USAGE_WEEK								=	"3";
	public static final String LOAD_USAGE_MONTH								=	"4";
	public static final String LOAD_USAGE_YEAR								=	"5";
	
	//ts_county_data
	public static final String TABLE_COUNTY_LEGAL_TEMPALTES					= 	"ts_county_legal_templates";
	public static final String FIELD_COUNTY_LEGAL_TEMPLATES_ID				=	"templateId";
	public static final String FIELD_COUNTY_LEGAL_TEMPLATES_DEFAULT_LD 		= 	"detaultLd";
	public static final String FIELD_COUNTY_LEGAL_TEMPLATES_OVERWRITE_OCR_LEGAL 	= 	"overwriteOCRlegal";
	public static final String FIELD_COUNTY_LEGAL_TEMPLATES_DEFAULT_LD_CONDO 		= 	"defaultLdCondo";
	public static final String FIELD_COUNTY_LEGAL_TEMPLATES_OVERWRITE_OCR_LEGAL_CONDO 	= 	"overwriteOCRlegalCondo";
	
	//property_owner
	public static final String TABLE_PROPERTY_OWNER							=	"property_owner";
	public static final String FIELD_PROPERTY_OWNER_ID						=	"id";
	public static final String FIELD_PROPERTY_OWNER_LAST_NAME				=	"lastName";
	public static final String FIELD_PROPERTY_OWNER_FIRST_NAME				=	"firstName";
	public static final String FIELD_PROPERTY_OWNER_MIDDLE_NAME				=	"middleName";
	public static final String FIELD_PROPERTY_OWNER_SUFFIX					=	"suffix";
	public static final String FIELD_PROPERTY_OWNER_PREFIX					=	"prefix";
	public static final String FIELD_PROPERTY_OWNER_IS_COMPANY				=	"isCompany";
	public static final String FIELD_PROPERTY_OWNER_SSN4					=	"ssn4";
	public static final String FIELD_PROPERTY_OWNER_COLOR					=	"color";
	public static final String FIELD_PROPERTY_OWNER_SEARCH_ID				=	"searchId";
	
	//property_buyer
	public static final String TABLE_PROPERTY_BUYER							=	"property_buyer";
	public static final String FIELD_PROPERTY_BUYER_ID						=	"id";
	public static final String FIELD_PROPERTY_BUYER_LAST_NAME				=	"lastName";
	public static final String FIELD_PROPERTY_BUYER_FIRST_NAME				=	"firstName";
	public static final String FIELD_PROPERTY_BUYER_MIDDLE_NAME				=	"middleName";
	public static final String FIELD_PROPERTY_BUYER_SUFFIX					=	"suffix";
	public static final String FIELD_PROPERTY_BUYER_PREFIX					=	"prefix";
	public static final String FIELD_PROPERTY_BUYER_IS_COMPANY				=	"isCompany";
	public static final String FIELD_PROPERTY_BUYER_SSN4					=	"ssn4";
	public static final String FIELD_PROPERTY_BUYER_COLOR					=	"color";
	public static final String FIELD_PROPERTY_BUYER_SEARCH_ID				=	"searchId";
	
	
	//ts_property
	public static final String TABLE_PROPERTY					=	"ts_property";
	public static final String FIELD_PROPERTY_ID				=	"id";
	public static final String FIELD_PROPERTY_ADDR_NO			=	"address_no";
	public static final String FIELD_PROPERTY_ADDR_DIR			=	"address_direction";
	public static final String FIELD_PROPERTY_ADDR_NAME			=	"address_name";
	public static final String FIELD_PROPERTY_ADDR_SUFF			=	"address_suffix";
	public static final String FIELD_PROPERTY_ADDR_UNIT			=	"address_unit";
	public static final String FIELD_PROPERTY_CITY				=	"city";
	public static final String FIELD_PROPERTY_COUNTY_ID			=	"county_id";
	public static final String FIELD_PROPERTY_STATE_ID			=	"state_id";
	public static final String FIELD_PROPERTY_ZIP				=	"zip";
	public static final String FIELD_PROPERTY_INSTRUMENT		=	"instrument";
	public static final String FIELD_PROPERTY_PARCEL_ID			=	"parcel_id";
	public static final String FIELD_PROPERTY_PLAT_BOOK			=	"platbook";
	public static final String FIELD_PROPERTY_PLAT_PAGE			=	"page";
	public static final String FIELD_PROPERTY_SUBDIVISION		=	"subdivision";
	public static final String FIELD_PROPERTY_LOTNO				=	"lotno";
	public static final String FIELD_PROPERTY_IS_BOOTSTRAPPED	=	"isBootstrapped";
	
	//ts_user_rating
	public static final String FIELD_USER_RATING_USER_ID		=	"user_id";
	public static final String FIELD_USER_RATING_START_DATE		=	"start_date";
	public static final String FIELD_USER_RATING_C2ARATEINDEX	= 	"c2arateindex";
	public static final String FIELD_USER_RATING_ATS2CRATEINDEX	=	"ats2crateindex";
	public static final String FIELD_USER_RATING_ID				=	"id";
	public static final String FIELD_USER_RATING_COUNTY_ID		=	"county_id";
	
	//ts_tax_dates
	public static final String FIELD_TAX_DATES_SITE_NAME		=	"name";
	public static final String FIELD_TAX_DATES_DUE_DATE			=	"dueDate";
	public static final String FIELD_TAX_DATES_PAY_DATE			=	"payDate";
	public static final String FIELD_TAX_TAX_YEAR_MODE			= 	"tax_year_mode";
	
	//name_synonims
	public static final String FIELD_NAME_SYNONYMS_SYNONYM_KEY		=	"synonym_key";
	public static final String FIELD_NAME_SYNONYMS_SYNONYM_VALUE	=	"synonym_value";
	
	public static final String FIELD_THREAD_LOGS_ID				= "id";
	public static final String FIELD_THREAD_LOGS_DATE			= "save_time";
	public static final String FIELD_THREAD_LOGS_CONTENT		= "thread_stack";
	
	//public static final String TABLE_RECOVER_DOCUMENTS			=	"documents";
	public static final String FIELD_RECOVER_DOCUMENTS_ID		=	"id";
	public static final String FIELD_RECOVER_DOCUMENTS_SERVER_ID=	"server_id";
	public static final String FIELD_RECOVER_DOCUMENTS_BOOK		=	"book";
	public static final String FIELD_RECOVER_DOCUMENTS_PAGE		=	"page";
	public static final String FIELD_RECOVER_DOCUMENTS_INSTRUMENT_NUMBER = "instrumentNumber";
	public static final String FIELD_RECOVER_DOCUMENTS_DOCUMENT_NUMBER = "documentNumber";
	public static final String FIELD_RECOVER_DOCUMENTS_YEAR		=	"year";
	public static final String FIELD_RECOVER_DOCUMENTS_DOCTYPE_FOR_SEARCH = "doctypeForSearch";
	public static final String FIELD_RECOVER_DOCUMENTS_DESCRIPTION = "description";
	public static final String FIELD_RECOVER_DOCUMENTS_GRANTOR	= 	"grantor";
	public static final String FIELD_RECOVER_DOCUMENTS_GRANTEE	=	"grantee";
	public static final String FIELD_RECOVER_DOCUMENTS_RECORDED_DATE = "recordedDate";
	public static final String FIELD_RECOVER_DOCUMENTS_REMARKS	=	"remarks";
	public static final String FIELD_RECOVER_DOCUMENTS_SEARCH_TYPE	=	"searchType";
	
	//public static final String TABLE_MODULES					=	"modules";
	public static final String FIELD_MODULE_ID					=	"module_id";
	public static final String FIELD_MODULE_SERVER_ID			=	"server_id";
	public static final String FIELD_MODULE_SEARCH_ID			=	"search_id";
	public static final String FIELD_MODULE_SEARCH_MODULE_ID	=	"search_module_id";
	public static final String FIELD_MODULE_DESCRIPTION			=	"description";
	
	//public static final String TABLE_MODULE_TO_DOCUMENT			=	"module_to_document";
	public static final String FIELD_MODULE_TO_DOCUMENT_MODULE_ID	=	"module_id";
	public static final String FIELD_MODULE_TO_DOCUMENT_DOCUMENT_ID	=	"document_id";
	
	public static final String FIELD_ERROR_REQUEST_ID				=	ErrorRequestBean.FIELD_ID;
	public static final String FIELD_ERROR_REQUEST_SEARCH_ID		=	ErrorRequestBean.FIELD_SEARCH_ID;
	public static final String FIELD_ERROR_REQUEST_REQUEST_DATE		=	ErrorRequestBean.FIELD_REQUEST_DATE;
	public static final String FIELD_ERROR_REQUEST_REQUEST			=	ErrorRequestBean.FIELD_REQUEST;
	public static final String FIELD_ERROR_REQUEST_RESPONSE			=	ErrorRequestBean.FIELD_RESPONSE;
	public static final String FIELD_ERROR_REQUEST_ERROR			=	ErrorRequestBean.FIELD_ERROR;
	
	public static final String TABLE_DISTRICTS_NV_CLARK = "ts_districts_nv_clark";
	public static final String FIELD_DISTRICT = "district";
	public static final String FIELD_NAME = "name";
	public static final String FIELD_CODE = "code";
	
	//dt_error_code
	public static final String TABLE_DT_ERROR_CODE 						= "dt_error_code";
	public static final String FIELD_DT_ERROR_CODE_ERROR_CODE 			= "error_code";
	public static final String FIELD_DT_ERROR_CODE_LEVEL 				= "level";
	public static final String FIELD_DT_ERROR_CODE_TYPE 				= "type";
	public static final String FIELD_DT_ERROR_CODE_SUMMARY 				= "summary";
	public static final String FIELD_DT_ERROR_CODE_EXPLANATION 			= "explanation";
	public static final String FIELD_DT_ERROR_CODE_ALTERNATE_MESSAGE 	= "alternate_message";
	
	//TABLE_DOCUMENTS_INDEX
	public static final String TABLE_DOCUMENT_INDEX	 					= "ts_documents_index";
	public static final String FIELD_DOCUMENT_INDEX_ID	 				= "id";
	public static final String FIELD_DOCUMENT_INDEX_CONTENT 			= "content";
	public static final String FIELD_DOCUMENT_INDEX_SEARCHID 			= "searchid";
	public static final String FIELD_DOCUMENT_INDEX_BLOB 				= "document";
	
}
