package ro.cst.tsearch.utils;

public interface RequestParamsValues {

		public static final String TRUE = "true";
		
	public static final String INVOICE_SET_RECEIVED 		= "setReceived";
	public static final String INVOICE_CLEAR_RECEIVED 		= "clearReceived";
	public static final String INVOICE_TO_INVOICE	 		= "toInvoice";
	public static final String INVOICE_SET_PAID 			= "setPaid";
	public static final String INVOICE_SET_UNPAID 			= "setUnPaid";
	public static final String INVOICE_CLEAR_INVOICED 		= "clearInvoiced";
	public static final String INVOICE_DELETE_SEARCHES 		= "deleteSearches";
	
	public static final String REPORTS_UNLOCK_SEARCHES 		= "unlockSearches";
	public static final String REPORTS_DELETE_SEARCHES 		= "deleteSearches";
	public static final String REPORTS_SET_K_STATUS 		= "setKStatus";
    public static final String REPORTS_SET_K_STATUS_KEEP_ABSTRACTOR         = "setKStatusKeepAbstractor";
    public static final String REPORTS_ASSIGN_ABSTRACTOR	= "assignAbstractor";
    public static final String REPORTS_ASSIGN_AGENT			= "assignAgent";
    public static final String REPORTS_EXPORT_DASHBOARD		= "exportDashboard";

	public static final String SETTINGS_OP_COMMADMIN		= "commAdminSettings";
	public static final String SETTINGS_OP_TSADMIN			= "tsAdminSettings";
	public static final String SETTINGS_OP_DUE_DATE			= "commAdminSetDueDate";
	public static final String SETTINGS_OP_CITY_DUE_DATE	= "commAdminSetCityDueDate";
	public static final String SETTINGS_OP_CNTYIDXX			= "countyIDXX";
	public static final String SETTINGS_OP_UPDATE_DISCOUNT	= "updateDiscount";
	public static final String SETTINGS_OP_EXPORT_TO_COMM	= "exportComm";
	public static final String SETTINGS_OP_EXPORT_TO_FILE	= "exportFile";
	public static final String SETTINGS_OP_IMPORT_FILE		= "importFile";
	public static final String SETTINGS_OP_SET_COUNTY_PAYDATE	= "setCountyPayDate";
	public static final String SETTINGS_OP_SET_CITY_PAYDATE		= "setCityPayDate";
	
	public static final String TSR_SEARCH_MONTH						= "searchMonth";
	public static final String TSR_SEARCH_DAY						= "searchDay";
	public static final String TSR_SEARCH_YEAR						= "searchYear";
	public static final String TSR_SEARCH_INTERVAL					= "searchInterval";
	
	public static final String REPORTS_SET_FVS_SEARCHES					= "setFVSSearches";
	public static final String REPORTS_RESET_FVS_SEARCHES					= "resetFVSSearches";
	
	public static final String PARENT_SITE_SAVE_TYPE_WITH_CROSSREF		= "0";
	public static final String PARENT_SITE_SAVE_TYPE_WITHOUT_CROSSREF	= "1";
	public static final String PARENT_SITE_SAVE_TYPE_COMBINED_CROSSREF	= "2";	//for some types of documents save with cross-references
																				//for the other save without cross-references
	
	public static final String ERR_NO_ERROR_APPEARED					= "0";
	public static final String ERR_NO_INVALID_LOGIN						= "1";
	public static final String ERR_NO_ACCOUNT_BLOCKED					= "3";
	public static final String ERR_NO_INVALID_SESSION					= "2";
	
	public static final int REPORTS_DATE_TYPE_COMBINED				= 1;
	public static final int REPORTS_DATE_TYPE_ORDER					= 2;
	public static final int REPORTS_DATE_TYPE_TSR					= 3;
	
	public static final int ASSIGN_DEFAULT_ROWS_PER_PAGE			= 300;

}
