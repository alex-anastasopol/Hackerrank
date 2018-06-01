package ro.cst.tsearch.utils;

public interface RequestParams {

	
	public static final String ACTION = "ActionName";
	public static final String CURRENTPAGE = "CurrentPage";
	public static final String SEARCH_STARTED = "searchstarted";
	public static final String UPLOAD_STARTED = "uploadstarted";


	/** UserInformation fields **/
	public static final String USER_USERID 		= "UserID";
	public static final String USER_FIRSTNAME = "FirstName";
	public static final String USER_LASTNAME = "LastName";
	public static final String USER_SYSTEMUSERID = "SystemUserID";
	public static final String USER_PASSWORD = "Password";
	public static final String USER_COMMUNITYID = "CommunityID";	
	
	public static final String USER_ADDRESS = "Address";
	public static final String USER_CITY = "City";
	public static final String USER_ZIP = "Zip";
	public static final String USER_PHONE = "Phone";
	public static final String USER_PHONEHOME = "PhoneHome";
	public static final String USER_FAX = "Fax";
	public static final String USER_PAGER = "Pager";
	public static final String USER_MOBILE = "Mobile";
	public static final String USER_EMAIL = "Email";
	
	/** User additional fields **/
	public static final String SEARCH_ID = "searchId";
	public static final String SEARCH_DB_ID = "searchDBId";
	public static final String FORWARD_LINK = "forwardLink";
	public static final String CONTINUE_SEARCH = "resetSearchIterator";
	public static final String GO_BACK_ONE_LEVEL = "goBackOneLevel";
	public static final String EMAIL_REQUEST = "emailRequest";
	public static final String SESSION_ID = "sessionId";
	public static final String SHOW_HIDDEN = "showHidden";
	
	//Invoice template fields
	public static final String LOGO_PATH 		= "logoFile";
	public static final String COMM_NAME 		= "commName";
	public static final String COMM_ADDRESS 	= "commAddress";
	public static final String COMM_PHONE 		= "commPhone";
	public static final String COMM_EMAIL 		= "commEmail";
	public static final String COMM_ID	 		= "commId";
	
	public static final String AGENT_NAME 		= "agentInvName";
	public static final String AGENT_ADDRESS 	= "agentInvAddress";
	public static final String AGENT_PHONE 		= "agentInvPhone";
	public static final String AGENT_EMAIL 		= "agentInvEmail";

	public static final String START_INTERVAL 				= "startInterval";
	public static final String END_INTERVAL 					= "endInterval";
	public static final String INV_TIMESTAMP 				= "invTimestamp";
	
	public static final String NR_SEARCHES	 				= "nrSearches";
	public static final String NR_CURRENTOWNER		= "nrCurrentOwner";
	public static final String NR_CONSTRUCTION			= "nrConstruction";
	public static final String NR_COMMERCIAL				= "nrCommercial";
	public static final String NR_REFINANCE					= "nrRefinance";
	public static final String NR_OE								= "nrOe";
	public static final String NR_LIENS							= "nrLiens";
	public static final String NR_ACREAGE					= "nrAcreage";
	public static final String NR_SUBLOT						= "nrSublot";
	public static final String NR_UPDATES	 					= "nrUpdates";
	
	
	public static final String COST_SEARCHES					= "costSearches";
	public static final String COST_CURRENTOWNER		= "costCurrentOwner";
	public static final String COST_CONSTRUCTION			= "costConstruction";
	public static final String COST_COMMERCIAL				= "costCommercial";
	public static final String COST_REFINANCE					= "costRefinance";
	public static final String COST_OE								= "costOe";
	public static final String COST_LIENS							= "costLiens";
	public static final String COST_ACREAGE					= "costAcreage";
	public static final String COST_SUBLOT						= "costSublot";
	public static final String COST_UPDATES	 					= "costUpdates";
	
	public static final String SUBTOTAL_SEARCHES				= "subtotalSearches";
	public static final String SUBTOTAL_CURRENTOWNER	= "subtotalCurrentOwner";
	public static final String SUBTOTAL_CONSTRUCTION		= "subtotalConstruction";
	public static final String SUBTOTAL_COMMERCIAL          = "subtotalCommercial";
	public static final String SUBTOTAL_REFINANCE				= "subtotalRefinance";
	public static final String SUBTOTAL_OE							= "subtotalOe";
	public static final String SUBTOTAL_LIENS						= "subtotalLiens";
	public static final String SUBTOTAL_ACREAGE				= "subtotalAcreage";
	public static final String SUBTOTAL_SUBLOT					= "subtotalSublot";	
	public static final String SUBTOTAL_UPDATES					= "subtotalUpdates";
	
	public static final String COST_TOTAL = "costTotal";
	public static final String INV_DETAILS = "invDetails";
	public static final String TABLE_SUBTOTALS = "tableSubTotal";
	
	public static final String COST_TOTAL_SEARCHES					= "totalSearchesCost";
	public static final String COST_TOTAL_CURRENTOWNER		= "totalCurrentOwnerCost";
	public static final String COST_TOTAL_CONSTRUCTION			= "totalConstructionCost";
	public static final String COST_TOTAL_COMMERCIAL				= "totalCommercialCost";
	public static final String COST_TOTAL_REFINANCE					= "totalRefinanceCost";
	public static final String COST_TOTAL_OE								= "totalOeCost";
	public static final String COST_TOTAL_LIENS							= "totalLiensCost";
	public static final String COST_TOTAL_ACREAGE					= "totalAcreageCost";
	public static final String COST_TOTAL_SUBLOT						= "totalSublotCost";
	public static final String COST_TOTAL_UPDATES						= "totalUpdatesCost";
	
	
	public static final String FULL_SEARCHES_PRODUCT						= "FullSearches";
	public static final String CURRENTOWNER_SEARCH_PRODUCT		= "CurrentOwnerSearches";
	public static final String CONSTRUCTION_SEARCH_PRODUCT		= "ConstructionSearches";
	public static final String COMMERCIAL_SEARCH_PRODUCT			= "CommercialSearches";
	public static final String REFINANCE_SEARCH_PRODUCT				= "RefinanceSearches";
	public static final String OE_SEARCH_PRODUCT								= "OeSearches";
	public static final String LIENS_SEARCH_PRODUCT							= "LiensSearches";
	public static final String ACREAGE_SEARCH_PRODUCT					=  "AcreageSearches";
	public static final String SUBLOT_SEARCH_PRODUCT						= "SublotSearches";
	public static final String SEARCH_UPDATES_PRODUCT					= "SearchUpdates";
	public static final String PRODUCT_INDEX					= "SearchIndex";
	
	public static final String ARCHIVE_DOWNLOADS_PRODUCT			= "ArchiveDownloads";
	
	public static final String NR_SEARCHES_DISCOUNT	 				= "nrSearchesDiscount";
	public static final String NR_CURRENTOWNER_DISCOUNT			= "nrCurrentOwnerDiscount";
	public static final String NR_CONSTRUCTION_DISCOUNT			= "nrConstructionDiscount";
	public static final String NR_COMMERCIAL_DISCOUNT				= "nrCommercialDiscount";
	public static final String NR_REFINANCE_DISCOUNT					= "nrRefinanceDiscount";
	public static final String NR_OE_DISCOUNT									= "nrOeDiscount";
	public static final String NR_LIENS_DISCOUNT							= "nrLiensDiscount";
	public static final String NR_ACREAGE_DISCOUNT						= "nrAcreageDiscount";
	public static final String NR_SUBLOT_DISCOUNT						= "nrSublotDiscount";
	public static final String NR_UPDATES_DISCOUNT	 					= "nrUpdatesDiscount";
	

	public static final String COST_SEARCHES_DISCOUNT					= "costSearchesDiscount";
	public static final String COST_CURRENTOWNER_DISCOUNT			= "costCurrentOwnerDiscount";
	public static final String COST_CONSTRUCTION_DISCOUNT			= "costConstructionDiscount";
	public static final String COST_COMMERCIAL_DISCOUNT				= "costCommercialDiscount";
	public static final String COST_REFINANCE_DISCOUNT					= "costRefinanceDiscount";
	public static final String COST_OE_DISCOUNT								= "costOeDiscount";
	public static final String COST_LIENS_DISCOUNT							= "costLiensDiscount";
	public static final String COST_ACREAGE_DISCOUNT						= "costAcreageDiscount";
	public static final String COST_SUBLOT_DISCOUNT						= "costSublotDiscount";
	public static final String COST_UPDATES_DISCOUNT				 		= "costUpdatesDiscount";
	
	public static final String SUBTOTAL_SEARCHES_DISCOUNT				= "subtotalSearchesDiscount";
	public static final String SUBTOTAL_CURRENTOWNER_DISCOUNT		= "subtotalCurrentOwnerDiscount";
	public static final String SUBTOTAL_CONSTRUCTION_DISCOUNT		= "subtotalConstructionDiscount";
	public static final String SUBTOTAL_COMMERCIAL_DISCOUNT           = "subtotalCommercialDiscount";
	public static final String SUBTOTAL_REFINANCE_DISCOUNT				= "subtotalRefinanceDiscount";
	public static final String SUBTOTAL_OE_DISCOUNT							= "subtotalOeDiscount";
	public static final String SUBTOTAL_LIENS_DISCOUNT						= "subtotalLiensDiscount";
	public static final String SUBTOTAL_ACREAGE_DISCOUNT					= "subtotalAcreageDiscount";
	public static final String SUBTOTAL_SUBLOT_DISCOUNT					= "subtotalSublotDiscount";	
	public static final String SUBTOTAL_UPDATES_DISCOUNT					= "subtotalUpdatesDiscount";
	
	
	public static final String COST_TOTAL_SEARCHES_DISCOUNT					= "totalSearchesCostDiscount";
	public static final String COST_TOTAL_CURRENTOWNER_DISCOUNT			= "totalCurrentOwnerCostDiscount";
	public static final String COST_TOTAL_CONSTRUCTION_DISCOUNT			= "totalConstructionCostDiscount";
	public static final String COST_TOTAL_COMMERCIAL_DISCOUNT				= "totalCommercialCostDiscount";
	public static final String COST_TOTAL_REFINANCE_DISCOUNT					= "totalRefinanceCostDiscount";
	public static final String COST_TOTAL_OE_DISCOUNT								= "totalOeCostDiscount";
	public static final String COST_TOTAL_LIENS_DISCOUNT							= "totalLiensCostDiscount";
	public static final String COST_TOTAL_ACREAGE_DISCOUNT						= "totalAcreageCostDiscount";
	public static final String COST_TOTAL_SUBLOT_DISCOUNT						= "totalSublotCostDiscount";
	public static final String COST_TOTAL_UPDATES_DISCOUNT						= "totalUpdatesCostDiscount";
	
	
	public static final String FULL_SEARCHES_PRODUCT_DISCOUNT						= "FullSearchesDiscount";
	public static final String CURRENTOWNER_SEARCH_PRODUCT_DISCOUNT		= "CurrentOwnerSearchesDiscount";
	public static final String CONSTRUCTION_SEARCH_PRODUCT_DISCOUNT			= "ConstructionSearchesDiscount";
	public static final String COMMERCIAL_SEARCH_PRODUCT_DISCOUNT				= "CommercialSearchesDiscount";
	public static final String REFINANCE_SEARCH_PRODUCT_DISCOUNT				    = "RefinanceSearchesDiscount";
	public static final String OE_SEARCH_PRODUCT_DISCOUNT								= "OeSearchesDiscount";
	public static final String LIENS_SEARCH_PRODUCT_DISCOUNT							= "LiensSearchesDiscount";
	public static final String ACREAGE_SEARCH_PRODUCT_DISCOUNT						=  "AcreageSearchesDiscount";
	public static final String SUBLOT_SEARCH_PRODUCT_DISCOUNT						= "SublotSearchesDiscount";	
	public static final String SEARCH_UPDATES_PRODUCT_DISCOUNT						= "SearchUpdatesDiscount";
	public static final String PRODUCT_INDEX_DISCOUNT = "SearchIndexDiscount";
		
	

	/** Reports fields **/
	public static final String REPORTS_DATE_TYPE		= "reportsDateType";
	public static final String REPORTS_SEARCH_ALL       = "reportsSearchAll";
	public static final String REPORTS_SEARCH_FIELD     = "reportsSearchField";
	public static final String REPORTS_SEARCH_FIELD_FROM     = "reportsSearchFieldFrom";
	public static final String REPORTS_STATE	 		= "reportState";
	public static final String REPORTS_COUNTY	 		= "reportCounty";
	public static final String REPORTS_CITY	 		    = "reportCity";
	public static final String REPORTS_ORDER_BY	 		= "orderBy";
	public static final String REPORTS_ORDER_TYPE 		= "orderType";
	public static final String REPORTS_STATUS	 		= "reportStatus";
	public static final String REPORTS_DAY		 		= "dayReport";
	public static final String REPORTS_MONTH	 		= "monthReport";
	public static final String REPORTS_YEAR		 		= "yearReport";
	public static final String REPORTS_AGENT	 		= "reportAgent";
	public static final String REPORTS_COMPANY_AGENT	= "reportCompanyAgent";
	public static final String REPORTS_ABSTRACTOR 		= "reportAbstractor";
	public static final String REPORTS_USER 			= "reportUser";
	public static final String REPORTS_LIST_OPERATION 	= "reportsOperation";
	public static final String REPORTS_LIST_CHK			= "reportsListChk";
	public static final String REPORTS_LIST_UNCHK		= "reportsListUnchk";
	public static final String REPORTS_PAGE_NAME		= "reportsPageName";
	public static final String REPORTS_FROM_DAY			= "fromDay";
	public static final String REPORTS_FROM_MONTH		= "fromMonth";
	public static final String REPORTS_FROM_YEAR		= "fromYear";
	public static final String REPORTS_TO_DAY			= "toDay";
	public static final String REPORTS_TO_MONTH			= "toMonth";
	public static final String REPORTS_TO_YEAR			= "toYear";
	public static final String REPORTS_OPEN_SEARCH_ID	= "reportsOpenSearchId";
	public static final String REPORTS_OPEN_SEARCH_DB_ID= "reportsOpenSearchDBId";
	public static final String REPORTS_OPEN_SEARCH_TIME = "reportsOpenSearchTime";
	public static final String REPORTS_COMMUNITY		= "reportCommunity";
	public static final String REPORTS_DASHBOARD		= "dash";
	public static final String REPORTS_ORDER_EMAIL		= "orderEmail";
	public static final String REPORTS_ORDER_XML		= "orderXml";
	public static final String REPORTS_EDIT_EMAIL		= "editEmail";
	public static final String REPORTS_PAGE	 		= "reportPage";
	public static final String REPORTS_ROWS_PER_PAGE	 		= "rowsPerPage";	
	public static final String REPORTS_FILTER_STATUS_CHANGED = "filterStatusChanged";
	public static final String REPORTS_EXPORT				= "reportsExport";
	public static final String DATE_TO_NEW 				= "date_to_new";
	public static final String DATE_FROM_NEW 				= "date_from_new";
	public static final String DATE_MONTH_NEW 				= "date_month_new";
	public static final String DATE_DUE_NEW				= "dueDate";
	public static final String INSTUMENT_DATE			= "instrumentDate";
	public static final String RECORDED_DATE			= "recordedDate";
	public static final String DEFAULT_DATE1			= "defaultDate1";
	public static final String DEFAULT_DATE2			=	"defaultDate2";
	//day report line data fields
	public static final String SEARCH_ABS_NAME	 		= "abstractorName";
	public static final String SEARCH_ABS_COLUMN	 		= "abstractorColumn";
	public static final String SEARCH_OWN_NAME	 		= "ownerName";
	public static final String SEARCH_AGN_NAME		 	= "agentName";
	public static final String SEARCH_COUNTY_NAME	 	= "propertyFullCounty";
	public static final String SEARCH_PROP_ADDRESS	 	= "propertyAddress";
	public static final String SEARCH_HOUR		 		= "hour";
	public static final String SEARCH_TIMEZONE	 		= "timeZone";
	public static final String SEARCH_FILE_ID	 		= "fileId";
	public static final String SEARCH_SENT_TO	 		= "sendTo";
    public static final String SEARCH_PRODUCT_TYPE      = "productType";
    public static final String SEARCH_PRODUCT_NAME     = "productName";
    public static final String SEARCH_FEE               = "searchFee";
    public static final String A2CRATES					= "a2crates";
    public static final String C2ARATES					= "c2arates";
    
    public static final String CATEGORY_SELECT			=	"categorySelect";
    public static final String SUBCATEGORY_SELECT		=	"subcategorySelect";
    public static final String WARNING_SELECT			=	"warningSelect";

	/** Invoice update fields **/
	public static final String INVOICE_LIST_CHK	 		= "listChecked";
	public static final String INVOICE_LIST_UNCHK	 	= "listUnchecked";
	public static final String INVOICE_LIST_OPERATION 	= "invoiceOperation";
	public static final String INVOICE_PAGE_NAME	 	= "invoicePage";
	public static final String INVOICE_FROM_DAY		 	= "fromDay";
	public static final String INVOICE_FROM_MONTH	 	= "fromMonth";
	public static final String INVOICE_FROM_YEAR	 	= "fromYear";
	public static final String INVOICE_TO_DAY		 	= "toDay";
	public static final String INVOICE_TO_MONTH		 	= "toMonth";
	public static final String INVOICE_TO_YEAR	 		= "toYear";
	public static final String INVOICE_EMAIL_FAI		= "fai";
	public static final String INVOICE_EMAIL_XLS		= "xls";
	public static final String INVOICE_EMAIL_PDF		= "pdf";
	
	public static final String INVOICE_DISCOUNT_VALUE		= "discountValue";
	public static final String INVOICE_DISCOUNT_NO_SEARCHES	= "discountNoSearches";
	public static final String INVOICE_DISCOUNT_LAST		= "discountLast";
	
	public static final String INVOICE_SEND_FORM_PDF	= "sendFormPdf";
	public static final String INVOICE_SEND_FORM_XML	= "sendFormXml";
	public static final String INVOICE_SEND_FORM_XLS	= "sendFormXls";
	public static final String INVOICE_SEND_FORM_CSV	= "sendFormCsv";
	
	public static final String INVOICE_SEND_PDF_CHECKBOX	= "sendPDFChk";
	public static final String INVOICE_SEND_XLS_CHECKBOX	= "sendXLSChk";
	public static final String INVOICE_SEND_XML_CHECKBOX	= "sendXMLChk";
	public static final String INVOICE_SEND_CSV_CHECKBOX	= "sendCSVChk";

	/** Settings update fields **/
	public static final String SETTINGS_OPERATION	 				= "settingsOp";
	
	public static final String SETTINGS_TSC_FULLSEARCH		 		= "settingsTSCfs";
	public static final String SETTINGS_TSC_UPDATESEARCH	 		= "settingsTSCus";
	public static final String SETTINGS_TSC_FVS	 					= "settingsTSCfvs";
	//public static final String SETTINGS_TSC_FROMARCHIVE		 		= "settingsTSCfa";
	public static final String SETTINGS_TSC_CURRENTOWNER			= "settingsTSCcu";
	public static final String SETTINGS_TSC_REFINANCE					= "settingsTSCre";
//	public static final String SETTINGS_TSC_DOCRETRIEVAL			= "settingsTSCdr";
	public static final String SETTINGS_TSC_CONSTRUCTION			= "settingsTSCcs";
	public static final String SETTINGS_TSC_COMMERCIAL				= "settingsTSCcm";
	public static final String SETTINGS_TSC_OE								= "settingsTSCoe";
	public static final String SETTINGS_TSC_LIENS							= "settingsTSCli";
	public static final String SETTINGS_TSC_ACREAGE						= "settingsTSCac";
	public static final String SETTINGS_TSC_SUBLOT						= "settingsTSCsb";
	public static final String SETTINGS_A2C_INDEX				= "settingsA2CIndex";
	public static final String SETTINGS_C2A_INDEX				= "settingsC2AIndex";
	
	
	public static final String SETTINGS_CST_FULLSEARCH		 		= "settingsCSTfs";
	public static final String SETTINGS_CST_UPDATESEARCH	 		= "settingsCSTus";
	public static final String SETTINGS_CST_FVS	 					= "settingsCSTfvs";
	//public static final String SETTINGS_CST_FROMARCHIVE		 		= "settingsCSTfa";
	public static final String SETTINGS_CST_CURRENTOWNER			= "settingsCSTcu";
	public static final String SETTINGS_CST_REFINANCE					= "settingsCSTre";
	//public static final String SETTINGS_CST_DOCRETRIEVAL			= "settingsCSTdr";
	public static final String SETTINGS_CST_CONSTRUCTION			= "settingsCSTcs";
	public static final String SETTINGS_CST_COMMERCIAL				= "settingsCSTcm";
	public static final String SETTINGS_CST_OE								= "settingsCSToe";
	public static final String SETTINGS_CST_LIENS							= "settingsCSTli";
	public static final String SETTINGS_CST_ACREAGE						= "settingsCSTac";
	public static final String SETTINGS_CST_SUBLOT						= "settingsCSTsb";	
	
	public static final String SETTINGS_IMPORT_FILE_NAME			= "importFileName";
	
	public static final String SETTINGS_INV_DETAILS	 				= "settingsInvDet";
	public static final String SETTINGS_COMMITMENT_DOC 				= "settingsComDoc";
	public static final String SETTINGS_DUE_DATE			 		= "formattedDueDate";

	public static final String PAYRATE_TSC_FULLSEARCH		 				= "formattedSearchValue";
	public static final String PAYRATE_TSC_UPDATESEARCH	 			= "formattedUpdateValue";
	public static final String PAYRATE_TSC_FVS	 					= "formattedFVSValue";
	public static final String PAYRATE_TSC_CURRENTOWNER				= "formattedCownerValue";
	public static final String PAYRATE_TSC_REFINANCE						= "formattedRefinanceValue";
	public static final String PAYRATE_TSC_CONSTRUCTION				= "formattedConstructionValue";
	public static final String PAYRATE_TSC_COMMERCIAL					= "formattedCommercialValue";
	public static final String PAYRATE_TSC_OE										= "formattedOEValue";
	public static final String PAYRATE_TSC_LIENS									= "formattedLiensValue";
	public static final String PAYRATE_TSC_ACREAGE							= "formattedAcreageValue";
	public static final String PAYRATE_TSC_SUBLOT								= "formattedSublotValue";
	public static final String PAYRATE_C2A_INDEX								= "formattedIndexC2A";
	
	public static final String PAYRATE_CST_FULLSEARCH		 				= "formattedSearchCost";
	public static final String PAYRATE_CST_UPDATESEARCH	 			= "formattedUpdateCost";
	public static final String PAYRATE_CST_FVS	 					= "formattedFVSCost";
	public static final String PAYRATE_CST_CURRENTOWNER				= "formattedCownerCost";
	public static final String PAYRATE_CST_REFINANCE						= "formattedRefinanceCost";
	public static final String PAYRATE_CST_CONSTRUCTION				= "formattedConstructionCost";
	public static final String PAYRATE_CST_COMMERCIAL					= "formattedCommercialCost";
	public static final String PAYRATE_CST_OE										= "formattedOECost";
	public static final String PAYRATE_CST_LIENS									= "formattedLiensCost";
	public static final String PAYRATE_CST_ACREAGE							= "formattedAcreageCost";
	public static final String PAYRATE_CST_SUBLOT								= "formattedSublotCost";	
	public static final String PAYRATE_A2C_INDEX								= "formattedIndexA2C";
	
	
	
	public static final String PAYRATE_CST_STARTDATE		 		= "formattedStartDate";
	public static final String PAYRATE_COUNTY				 		= "countyFullName";
	public static final String PAYRATE_COUNTY_ID			 		= "countyId";
	public static final String PAYRATE_COUNTY_IDX			 		= "countyIDXX";
	public static final String PAYRATE_CITY_ID			 			= "cityID";
	public static final String PAYRATE_CITY_NAME			 		= "cityName";
	public static final String ARCHIVE_FILE_NAME			 		= "archiveFileName";
	public static final String REPORTS_SEARCH_TSR 					= "TSRsearchString";
	public static final String REPORTS_FIND_EXTERNAL				= "findExternal";
	public static final String PAYRATE_CITY_DUE 					= "cityDue";
	
	public static final String ORDER_TS								= "orderTS";
	public static final String SEARCH_TYPE							= "searchtype";
	public static final String ERROR_BODY							= "errorBody";
	public static final String ERROR_TYPE							= "errorType";
	
	public static final String TSR_SEARCH_TYPE						= "searchType";
	
	public static final String GET_INDEX_OPENED_TIME				= "GIOpenTime";
	
	public static final String LOGIN_MUST_REDIRECT					= "redir";
	
	public static final String LBS_SOURCE_CLIENT					= "lbs_src_clnt";
	public static final String LBS_SOURCE_IP_SRC					= "lbs_src_ips";
	public static final String LBS_SOURCE_IP_MASK					= "lbs_src_mask";
	public static final String LBS_SOURCE_IP_DEST					= "lbs_src_dest";
	public static final String LBS_SOURCE_ENABLE					= "lbs_src_enbl";
	public static final String LBS_SOURCE_COMMNAME					= "lbs_src_commname";
	public static final String LBS_SOURCE_DEFAULT_DEST				= "lbs_src_def_dest";
	public static final String LBS_SOURCE_NOTIF_EMAILS				= "lbs_scr_notif_email";
	
	public static final String LBS_SOURCE_ID						= "id";
	public static final String LBS_SOURCE_ADDRESS					= "address";
	public static final String LBS_SOURCE_NETMASK					= "netmask";
	public static final String LBS_SOURCE_REDIRADDRESS				= "redirectAddress";
	public static final String LBS_SOURCE_CLNT_USERNAME				= "clientUsername";
	public static final String LBS_SOURCE_CLNT_COMMNAME				= "clientCommname";
	public static final String LBS_SOURCE_ENBL						= "enable";
	public static final String LBS_SOURCE_CHECKED					= "checked";
	
	public static final String LBS_SERVER_ID						= "lbs_srv_id";
	public static final String LBS_SERVER_IP_ADDR					= "lbs_srv_ipaddr";
	public static final String LBS_SERVER_IP_MASK					= "lbs_srv_ipmask";
	public static final String LBS_SERVER_HOST_NAME					= "lbs_srv_hostname";
	public static final String LBS_SERVER_ALIAS						= "lbs_srv_alias";
	public static final String LBS_SERVER_ENABLE					= "lbs_srv_enable";
	public static final String LBS_SERVER_PATH						= "lbs_srv_path";
	public static final String LBS_SERVER_CHECK_SEARCH_ACCESS		= "lbs_srv_check_search_access";
	
	public static final String LBS_SERVER_FIELD_ID					= "id";
	public static final String LBS_SERVER_FIELD_IP_ADDR				= "ip";
	public static final String LBS_SERVER_FIELD_IP_MASK				= "ipMask";
	public static final String LBS_SERVER_FIELD_HOST_NAME			= "hostName";
	public static final String LBS_SERVER_FIELD_ALIAS				= "alias";
	public static final String LBS_SERVER_FIELD_ENABLE				= "enabled";
	public static final String LBS_SERVER_FIELD_CHECK_SEARCH_ACCESS	= "checkSearchAccess";
	public static final String LBS_SERVER_FIELD_PATH				= "path";
	public static final String LBS_SERVER_FIELD_STATUS				= "status";
	public static final String LBS_SERVER_FIELD_TIME				= "time";
	
	public static final String LBS_ENABLE_SOURCE					= "lbs_enbl_source";
	public static final String LBS_ENABLE_LOAD						= "lbs_enbl_load";
	public static final String LBS_ENABLE_NOTIF_EMAIL				= "lbs_enbl_notif";
	public static final String LBS_ENABLE_OVERRIDE					= "lbs_enbl_override";
	
	public static final String PDF_FILE											= "pdfFile";
	public static final String Display_PDF_FILE								= "displayPdfFile";
	public static final String AGENT_ID											= "agentId";
	public static final String PARENT_SITE_SAVE_TYPE				= "parentSiteSaveType";
	public static final String PARENT_SITE_SAVE_TYPE_COMBINED		= "parentSiteSaveTypeCombined";
	public static final String PARENT_SITE_FOR_UPDATE_SEARCH_PARAMS	= "forUpdateSearchParams";
	public static final String PARENT_SITE_REQUEST_NUMBER_OF_SAVED_DOCUMENTS	= "requestNumberOfSavedDocuments";
	
	//manage county list
	public static final String USER_COUNTY_SORT_BY					= "sortBy";
	public static final String USER_COUNTY_SORT_ORDER				= "sortOrder";
	public static final String USER_COUNTY_EXPORT_USER_FROM			= "exportUserFrom";
	public static final String USER_COUNTY_EXPORT_USER_TO			= "exportUserTo";
	
	//status messages
	public static final String PARAM_ERROR_MESSAGE					= "errNo";
	
	//file servlet
	/**
	 * Keep the same value as com.stewart.ats.tsrindex.client.shared.RequestParams.FORCE_DOWNLOAD
	 */
	public static final String FORCE_DOWNLOAD 						= "forcedownload";
	public static final String STARTER_SEARCH_ID					= "starterSearchId";
	public static final String SHOW_FILE_ID							= "showFileId";

	public static final String SETTINGS_COUNTY_IDS 					= "countyIds";
	public static final String SETTINGS_DEFAULT_LD 					= "defaultLD";
	public static final String SETTINGS_DEFAULT_LD_CONDO			= "defaultLDCondo";
	public static final String SETTINGS_CERTIFICATION_DATE_OFFSET	= "certificationDateOffset";
	public static final String SETTINGS_OFFICIAL_START_DATE_OFFSET	= "officialStartDateOffset";
	
	public static final String SEARCH_WAS_UNLOCKED 					= "searchWasUnlocked";
	
	public static final String PARENT_SITE_ADDITIONAL_CNT			= "cnt_";
	public static final String PARENT_SITE_ADDITIONAL_CNT_ROW		= "cnt_row_";
	public static final String USER_TEMPLATES_PRODUCT				= "user_templates_product";
	public static final String USER_TEMPLATES_EXPORT_FORMAT			= "user_templates_export_format";
	
	public static final String SEARCH_PAGE_OWNER_MANUAL_VALIDATION_PREFIX = "sp_o_man_val_pref";
	public static final String SEARCH_PAGE_BUYER_MANUAL_VALIDATION_PREFIX = "sp_b_man_val_pref";
	
	public final static String INSTRUMENT_NUMBER = "instrumentNumber";
	
	/** Google Maps fields **/
	public final static String GOOGLE_MAPS_CHANGE_MAP = "changeMap";
	public final static String GOOGLE_MAPS_LATITUDE = "latitude";
	public final static String GOOGLE_MAPS_LONGITUDE = "longitude";
	public final static String GOOGLE_MAPS_ZOOM = "zoom";
	public final static String GOOGLE_MAPS_TYPE_ID = "mapTypeId";
	public final static String GOOGLE_MAPS_IS_ALREADY_SAVED = "isAlreadySaved";
	
}
