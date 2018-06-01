package ro.cst.tsearch.utils;
/**
 * @author nae
 */
public class TSOpCode {
    
	public static final String OPCODE = "opCode";
	
	public static final int USER_ADD_SUBMIT = 1;
	public static final int USER_ADD_APPLY = 2;
	public static final int USER_VIEW = 3;
	public static final int USER_EDIT = 4;
	public static final int USERS_DEL = 5;
	public static final int USER_DEL = 6;
	public static final int USERS_ADD_APPLY = 7;
	public static final int USER_ADD_SAVE = 8;
	public static final int SAVE_RESOURCE =9;
	public static final int USER_PAGES=10;
	public static final int USER_IMPORT_SAVE=11;
	public static final int USERS_SORTNAME =12;
	public static final int USERS_SORTFULLNAME=13;
	public static final int USERS_SORT_LASTLOGIN=14;
	public static final int USERS_LIKE=15;
	public static final int USERS_USERFIND=16;
	public static final int USER_IMPORT=17;
	public static final int USER_ADD=18;
	public static final int USERS_PAGE=19;
	public static final int USERS_COMMVIEW=20;
	public static final int USERS_SETPAGES=21;
	public static final int USER_EDIT_APPLY = 22;
	public static final int USER_EDIT_SUBMIT = 23;
	
	public static final int COMM_EXP_COLL = 24;
	public static final int SELECT_COMMUNITY = 25;
	public static final int COMM_EDIT =26;
	public static final int COMM_DEL = 27;
	public static final int SAVE_COMMUNITY=28;
	public static final int NEW_CATEGORY=29;
	public static final int NEW_COMMUNITY=30;
	public static final int COMM_VIEW=31;
	public static final int DEL_CATEGORY=32;
	public static final int VIEW_CATEGORY=33;
	public static final int SAVE_CATEGORY=34;
	public static final int ADD_CATEGORY=35;
	public static final int SELECT_COMM=36;
	
	public static final int DISPLAY_SEARCH_MODULES = 37; 
	public static final int CHANGE_STATE_COUNTY = 38; 
		
	// NEW OPCODES
	public static final int ATTACH_IMAGE = 39;
	public static final int AUTOMATIC_SEARCH = 40;
	public static final int CHECK_DOC_TYPE_XML = 41;
	public static final int CONVERT_TO_PDF = 42;
	public static final int DIFF = 43;
	public static final int FILTER = 44;
	public static final int GET_IMAGE_FROM_DISK = 45;
	public static final int LOGOUT = 46;
	public static final int PARENT_SITE = 47;
	public static final int REMOVE_CHAPTERS = 48;
	public static final int SEND_EMAIL = 49;
	public static final int SEND_SCREEN_VIA_EMAIL = 50;
	public static final int SHOW_STATUS = 51;
	public static final int START_TS = 52;
	public static final int STORE_HTTP_SETTINGS = 53;
	public static final int STORE_TA_SETTINGS = 54;
	public static final int TESTER = 55;
	public static final int TSD = 56;
	public static final int URL_CONNECTION_READER = 57;
	public static final int USER_VALIDATION = 58;
	public static final int VALIDATE_INPUTS = 59;
	public static final int XML_VIEWER = 60;
	public static final int LOGIN = 61;
			
	public static final int DOWNLOAD_LOGO = 62;	

	//DownloadFile
	public static final int VIEW_PICTURE = 63;
	public static final int DOWNLOAD_RESUME = 64;

	public static final int UPDATE_TSR_INDEX = 65;
	
	// View JSP Pages	
	public static final int MY_PROFILE_VIEW = 66;
	public static final int MY_PROFILE_EDIT = 67;
	
	// NewTS
	public static final int NEW_TS = 68;
	public static final int CHANGE_CONTEXT = 70;
	
	// TSDIndexPage
	public static final int TSD_INDEX_PAGE_FRAMESET = 71;
	public static final int TSD_INDEX_PAGE = 72;
	public static final int UPDATE_BEAN_INDEX = 73;
	
	public static final int USER_LIST_VIEW = 74;
	public static final int USER_ADD_VIEW = 75;
	public static final int USER_RATES_HISTORY_VIEW = 76;
	public static final int USER_EDIT_VIEW = 77;
	public static final int SETTINGS_VIEW = 78;
	public static final int PAYRATE_HISTORY_VIEW = 79;
	public static final int COMMUNITY_PAGE_ADMIN_VIEW = 80;
	public static final int COMMUNITY_PAGE_ADD_VIEW = 81;
	public static final int COMMUNITY_PAGE_EDIT_VIEW = 82;
	public static final int CATEGORY_PAGE_ADMIN_VIEW = 83;
	public static final int CATEGORY_PAGE_ADD_VIEW = 84;
	
	// Invoice & Report
	public static final int INVOICE_MODULE = 85;
	public static final int INVOICE_EMAIL = 86;
	public static final int REPORT_MODULE = 87;
	public static final int BARCHART_VIEW = 88;
	
	// HttpSettings
	
	public static final int HTTPSETTINGS_APPLY = 89;
	
	public static final int UPLOAD_CHANGE_INSTR_TYPE = 90;
	public static final int UPLOAD_SUBMIT_FILES = 91;
	public static final int UPLOAD_SUBMIT_DATA = 92;
	public static final int UPLOAD_CANCEL = 93;
	public static final int UPLOAD_CLOSE_WINDOW = 94;
	
	// Get Index
	public static final int COMM_UPLOAD_POLICY = 96;
	public static final int COMM_REMOVE_POLICY = 97;
	public static final int COMM_EDIT_POLICY = 98;
	public static final int COMM_VIEW_POLICY_DOC = 99;
	
	public static final int SUBMIT_ORDER = 100;
	public static final int VIEW_ORDER = 101;
	
	public static final int USER_NEW_ACCOUNT = 102;
	
	public static final int MY_PROFILE_EDIT_APPLY = 103;
	public static final int MY_PROFILE_EDIT_SUBMIT = 104;
	
	public static final int OPEN_SEARCH = 105;
	public static final int NEW_SEARCH = 106;
	public static final int VIEW_SEARCH = 107;
	public static final int SAVE_SEARCH = 108;
	public static final int CANCEL_SEARCH = 109;
	
	public static final int CREATE_TSR = 110;
	public static final int CREATE_TSR_START = 111;
	public static final int CREATE_TSR_CANCEL = 112;
	public static final int CREATE_TSR_EDIT = 113;
	public static final int CREATE_TSR_DONE = 114;
	public static final int MANAGE_POLICYS = 115;
	public static final int ERROR_OCCURS = 116;
	public static final int UPLOAD_PREPARE = 117;
	public static final int UPLOAD_PREPARED = 118;
	
	public static final int NO_AGENT_ERROR = 121;
	
	public static final int CHECK_OWNER 		= 122;
	public static final int CHECK_OWNER_OK 		= 123;
	public static final int CHECK_OWNER_FAIL 	= 124;
	
	public static final int VALIDATE_INPUTS_ONLY_TO_SEND_EMAIL = 125;
	
	public static final int NOTE_SAVE			= 126;
	public static final int NOTE_CLOSE			= 127;
	public static final int NOTE_OPEN			= 128;
    
    public static final int TO_ADD           = 129;
    public static final int TO_EDIT           = 130;
    
    public static final int SKIP_SITE         = 131;
    
    public static final int LEGAL_DESCRIPTION_SAVE	 = 132;
	public static final int LEGAL_DESCRIPTION_DELETE = 133;
	public static final int LEGAL_DESCRIPTION_OPEN	 = 134;
	
	public static final int UPDATE_HTML_INDEX = 135;
	public static final int UPDATE_HTML_INDEX_PREPARE = 136;
	public static final int UPDATE_HTML_INDEX_PREPARED = 137;
	public static final int UPDATE_HTML_INDEX_CHANGE_INSTR_TYPE = 138;
	public static final int UPDATE_HTML_INDEX_SUBMIT_FILES = 139;
    
    public static final int AJAX_CHANGE_STATE = 140;
    public static final int AJAX_CHANGE_COUNTY = 141;
    public static final int AJAX_CHANGE_AGENT = 142;
    
    public static final int INVOICE_RESET_LAST_DISCOUNT = 143;
    
    public static final int REMOVE_OLD_ADD_AND_NEW_SEARCH    = 144;
    
    public static final int LBS_ADD_SOURCE			= 145;
    public static final int LBS_UPDATE_SOURCE		= 146;
    public static final int LBS_DELETE_SOURCE		= 147;
    public static final int LBS_SET_DEFAULT_SOURCE	= 148;
    
    public static final int LBS_ADD_SERVER			= 149;
    public static final int LBS_UPDATE_SERVER		= 150;
    public static final int LBS_DELETE_SERVER		= 151;
    public static final int LBS_SET_DEF_SERVER		= 152;
    
    public static final int LBS_ENABLE_LOAD			= 153;
    public static final int LBS_ENABLE_NOTIFICATION	= 154;
    public static final int LBS_ENABLE_SOURCE		= 155;
    public static final int LBS_DISABLE_SOURCE		= 156;
    
    public static final int LBS_SET_DEFAULT_EMAIL	= 157;
    
    //do not change this. if you HAVE to change them change them in lbs project in OpCode class also
    public static final int LBS_MONITORING_LOAD		= 160;
    public static final int LBS_MONITORING_SESSIONS = 161;
    public static final int LBS_MONITORING_DBCONN	= 162;
    public static final int LBS_MONITORING_ADDRESS	= 163;
    public static final int LBS_MONITORING_UPDATE_LOAD	= 164;
    public static final int LBS_MONITORING_SRV_STATUS	= 165;
    
	public static final int SAVE_UNLOCKED_SEARCH = 166;
	
	public static final int UPDATE_HTML_INDEX_ERROR = 167;
    
	public static final int GET_IMAGES = 168;
	public static final int DEL_DOCS = 169;
	
	public static final int TSR_IN_PROGRESS_ERROR	= 170;
	
	public static final int USER_HIDE = 171;
	public static final int USER_UNHIDE = 172;
	
	public static final int AJAX_MY_ATS_CHANGE_DASHBOARD_STATE = 173;
	public static final int AJAX_MY_ATS_CHANGE_SEARCH_PAGE_STATE = 174;
	public static final int AJAX_MY_ATS_CHANGE_DASHBOARD_AGENCY = 175;
	public static final int MY_ATS_VIEW = 176;
	public static final int MY_ATS_EDIT = 177;
	public static final int MY_ATS_EDIT_APPLY = 178;
	public static final int MY_ATS_EDIT_SUBMIT = 179;
	
	
	
	public static final int AJAX_MAINTENANCE_MESSAGE_SET_COLOR = 180;
	public static final int CLONE_SEARCH_CODE		= 181;
	public static final int REUSE_SEARCH_CODE		= 182;
	
	public static final int USER_EDIT_DELPHOTO		= 183;
	public static final int USER_EDIT_DELRESUME		= 184;
	public static final int USER_EDIT_DELPHOTO_MYPROFILE		= 185;
	public static final int USER_EDIT_DELRESUME_MYPROFILE		= 186;
	
	public static final int SAVE_SEARCH_STARTER = 187;
	public static final int LOAD_STARTER	= 188;
	
	public static final int SET_DEFAULT_LEGAL_DESCRIPTION = 189;
	public static final int SET_CERTIFICATION_DATE_OFFSET = 190;
	
	public static final int AJAX_COMPILE_COMMUNITY_TEMPLATES = 191;
	
	public static final int COPY_IMAGES_FOR_OCR		=	192;
	
	public static final int MANAGE_COUNTY_VIEW		= 193;
	public static final int MANAGE_COUNTY_EDIT		= 192;
	
	public static final int LBS_ENABLE_OVERRIDE_DESTINATION		= 194;
	
	public static final int COPY_SEARCH	= 195;
	
	public static final int NO_FILEID_ERROR = 196;
	public static final int USER_RESET_PASSWORD = 197;
	public static final int AJAX_CHANGE_PRODUCT = 198;
	
	public static final int DATEDOWN_SEARCH_CODE= 199;
	
	public static final int SAVE_SEARCH_FOR_REVIEW = 200;
	
	public static final int MOVE_TSR_IMAGES_TO_SSF		=	201;
	
	public static final int CHANGE_PASSWORD_VIEW		=	202;
	public static final int CHANGE_PASSWORD		=	203;
	
	public static final int SET_OFFICIAL_START_DATE_OFFSET = 204;
	
	public static final int SET_DEFAULT_LEGAL_DESCRIPTION_CONDO = 205;		//related to SET_DEFAULT_LEGAL_DESCRIPTION
	
	public static final int STOP_AUTOMATIC_SEARCH = 206;
	
	public static final int COMMUNITY_PAGE_ADMIN_HIDE = 207;
	public static final int HIDE_COMMUNITY = 208;
	public static final int UNHIDE_COMMUNITY = 209;
	
	
	public static final int FVS_UPDATE 					= 210;
	
	public static final int DWNL_ALL_TEMPLATES	= 211;
	
	public static final int USERS_LIST_REPORT = 212;
}