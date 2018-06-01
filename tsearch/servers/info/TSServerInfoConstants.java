package ro.cst.tsearch.servers.info;

public class TSServerInfoConstants {
	public static final String EXTRA_PARAM_MODULE_DESCRIPTION	=	"MODULE_DESCRIPTION";
	public static final String EXTRA_PARAM_MODULE_OBJECT_SOURCE	=	"MODULE_OBJECT";
	public static final String EXTRA_PARAM_MODULE_INDEX_SOURCE	=	"MODULE_INDEX";
	public static final String EXTRA_PARAM_SIMULATE_CROSSREF	=	"SIMULATE_CROSSREF";
	public static final String EXTRA_PARAM_CROSSREF_DOC_SOURCE	=	"CROSSREF_DOC_SOURCE";
	public static final String EXTRA_PARAM_DO_NOT_REMOVE_IN_MEMORY_DOCS	=	"DO_NOT_REMOVE_IN_MEMORY_DOCS";
	public static final String TS_SERVER_INFO_MODULE_DESCRIPTION	= "TS_SERVER_INFO_MODULE_DESCRIPTION";
	public static final String EXTRA_PARAM_DO_NOT_RESAVE_DOCS	=	"EXTRA_PARAM_DO_NOT_RESAVE_DOCS";
	public static final String EXTRA_PARAM_RESTORE_DOCUMENT_SOURCE	=	"RESTORE_DOCUMENT_SOURCE";
	public static final String EXTRA_PARAM_CLEAR_VISITED_AND_VALIDATED_LINKS	=	"CLEAR_VISITED_AND_VALIDATED_LINKS";
	public static final String EXTRA_PARAM_DO_NOT_FAKE_DOCUMENT	=	"EXTRA_PARAM_DO_NOT_FAKE_DOCUMENT";
	public static final String EXTRA_PARAM_CHECK_ALREADY_SAVED	=	"EXTRA_PARAM_CHECK_ALREADY_SAVED";
	
	public static final String VALUE_PARAM_OCR_SEARCH_BP		=	
		"OCR module - searching with book/page extracted from the last transfer image";
	public static final String VALUE_PARAM_OCR_SEARCH_INST		=	
		"OCR module - searching with instruments extracted from the last transfer image";
	public static final String VALUE_PARAM_OCR_SEARCH_BP_INST		=	
		"OCR module - searching with book/page or instruments extracted from the last transfer image";
	public static final String VALUE_PARAM_LIST_SEARCH_PAGE_BP	=
		"Book-page module - searching with book/page list from the Search Page";
	public static final String VALUE_PARAM_LIST_SEARCH_PAGE_INSTR	=
		"Instrument module - searching with instrument list from the Search Page";
	public static final String VALUE_PARAM_LIST_AO_BP	=
		"Book-page module - searching with book/page list extracted from Assessor";
	public static final String VALUE_PARAM_LIST_AO_INSTR	=
		"Instrument module - searching with instrument list extracted from Assessor";
	public static final String VALUE_PARAM_LIST_AO_BP_PLAT	=
		"Book-page module - searching with book/page list extracted from Assessor for PLATs";
	public static final String VALUE_PARAM_LIST_AO_BP_TRANSFERS	=
		"Book-page module - searching with book/page list extracted from Assessor for TRANSFERs";
	public static final String VALUE_PARAM_LIST_RO_CROSSREF_BP	=
		"Book-page module - searching with book/page list extracted as crossref from ROlike docs";
	public static final String VALUE_PARAM_LIST_RO_CROSSREF_INSTR	=
		"Instrument module - searching with instrument list extracted as crossref from ROlike docs";
	public static final String VALUE_PARAM_LIST_NDB_INSTR	=
		"Instrument module - searching with instrument list extracted from NDB";
	public static final String VALUE_PARAM_SUBDIVISION_LOT		= 
		"Subdivision module - searching with subdivision and lot";
	public static final String VALUE_PARAM_CONDOMINIUM_LOT		= 
		"Subdivision module - searching with subdivision as condominium and lot";
	public static final String VALUE_PARAM_SUBDIVISION_AS_CONDO_LOT		= 
		"Subdivision module - searching with subdivision as condominium and lot/unit";
	public static final String VALUE_PARAM_SUBDIVISION_LOT_BLOCK		= 
		"Subdivision module - searching with subdivision and lot and/or block";
	public static final String VALUE_PARAM_CONDOMINIUM_LOT_BLOCK		= 
		"Subdivision module - searching with subdivision as condominium and lot and/or block";
	public static final String VALUE_PARAM_SUBDIVISION_LOT_UNIT		= 
		"Subdivision module - searching with subdivision, lot and/or unit";
	public static final String VALUE_PARAM_SUBDIVISION_LOT_PHASE		= 
		"Subdivision module - searching with subdivision, lot and/or phase";
	public static final String VALUE_PARAM_NAME_SUBDIVISION_PLAT	=
		"Name module - searching with subdivision at last name for PLAT documents";
	public static final String VALUE_PARAM_GRANTEE_NAME_SUBDIVISION_PLAT	=
		"Name module - searching with subdivision at grantee last name for PLAT documents";
	public static final String VALUE_PARAM_GRANTOR_NAME_SUBDIVISION_PLAT	=
		"Name module - searching with subdivision at grantor last name for PLAT documents";
	public static final String VALUE_PARAM_NAME_SUBDIVISION_PL_EAS_RES_MASDEED	=
		"Name module - searching with subdivision at grantor and grantee for PLAT, EASEMENT, RESTRICTION, MASTERDEED and CCER documents";
	public static final String VALUE_PARAM_NAME_SUBDIVISION_SECTION_TWP_RANGE	=
		"Name module - searching with Section, Township and Range";
	public static final String VALUE_PARAM_NAME_SUBDIVISION_EASEMENT	=
		"Name module - searching with subdivision at last name for EASEMENT documents";
	public static final String VALUE_PARAM_NAME_SUBDIVISION_RESTRICTION	=
		"Name module - searching with subdivision at last name for RESTRICTION documents";
	public static final String VALUE_PARAM_NAME_SUBDIVISION_WARRANTY_DEED	=
		"Name module - searching with subdivision at last name for WARRANTY_DEEDs documents";
	public static final String VALUE_PARAM_NAME_OWNERS	=
		"Name module - searching with owner(s) name";
	public static final String VALUE_PARAM_NAME_OWNERS_FOR_LEGAL	=
		"Name module - searching with owner(s) name for Legal";
	public static final String VALUE_PARAM_NAME_BUYERS	=
		"Name module - searching with buyer(s) name";
	public static final String VALUE_PARAM_NAME_BUYERS_LIEN	=
		"Name module - searching with buyer(s) name for LIENs";
	public static final String VALUE_PARAM_PLAT_BOOK_PAGE	=
		"Book-page module - searching with book-page for PLAT documents (book-page considered as PLAT)";
	public static final String VALUE_PARAM_PLAT_DOC	=
		"Instrument module - searching with plat document number for PLAT documents";
	public static final String VALUE_PARAM_LIST_MORTGAGE_INSTR	=
		"Instrument module - searching with saved mortgage instrument list to get extra information";
	public static final String VALUE_PARAM_PLATBOOK_PLATPAGE = 
		"Book-page module - searching with plat book-page in book/page fields";
	public static final String VALUE_PARAM_PLATVOLUME_PLATPAGE = 
		"Book-page module - searching with plat book-page in volume/page fields";
	public static final String VALUE_PARAM_ADDRESS_NO_NAME = 
		"Address module - searching with address (street_no street_name)";
	public static final String VALUE_PARAM_ADDRESS_NO_DIR_NAME = 
		"Address module - searching with address (street_no street_dir street_name)";
	public static final String VALUE_PARAM_PARCEL_ID	=
		"Parcel module - searching with parcel id";
	public static final String VALUE_PARAM_LIST_AO_NDB_TR_INSTR	=
		"Instrument module - searching with instrument list extracted from Assessor/Tax documents";
	public static final String VALUE_PARAM_LIST_AO_NDB_TR_BP	=
		"Instrument module - searching with book-page list extracted from Assessor/Tax documents";
	public static final String VALUE_PARAM_LIST_AO_NDB_TR_DOCNO	=
		"Instrument module - searching with document number list extracted from Assessor/Tax documents";
	public static final String VALUE_PARAM_LEGAL_MAP_ID = 
		"Subdivision module - searching with legal(subdivision - map id) from already saved documents";
	public static final String VALUE_PARAM_SECTION = 
		"Sectional/ARB module - searching with arb,township,range,section from already saved documents";
	public static final String VALUE_PARAM_LIST_SEARCH_PAGE_BP_AND_AO	=
		"Book-page module - searching with book/page list from the Search Page and Asessor Documents";
	//task 8518
	public static final String VALUE_PARAM_ADVANCED_SEARCH_SUBDIVISION_PLAT	=
		"Advanced Search - searching with subdivision at last name for PLAT documents";
	public static final String VALUE_PARAM_ADVANCED_SEARCH_GRANTEE_NAME_SUBDIVISION_PLAT	=
		"Advanced Search - searching with subdivision at grantee last name for PLAT documents";
	public static final String VALUE_PARAM_ADVANCED_SEARCH_GRANTOR_NAME_SUBDIVISION_PLAT	=
		"Advanced Search - searching with subdivision at grantor last name for PLAT documents";
	public static final String VALUE_PARAM_ADVANCED_SEARCH_SUBDIVISION_PL_EAS_RES_MASDEED	=
		"Advanced Search - searching with subdivision at grantor and grantee for PLAT, EASEMENT, RESTRICTION, MASTERDEED and CCER documents";
	public static final String VALUE_PARAM_ADVANCED_SEARCH_SUBDIVISION_SECTION_TWP_RANGE	=
		"Advanced Search - searching with Section, Township and Range";
	public static final String VALUE_PARAM_ADVANCED_SEARCH_SUBDIVISION_EASEMENT	=
		"Advanced Search - searching with subdivision at last name for EASEMENT documents";
	public static final String VALUE_PARAM_ADVANCED_SEARCH_SUBDIVISION_RESTRICTION	=
		"Advanced Search - searching with subdivision at last name for RESTRICTION documents";
	public static final String VALUE_PARAM_ADVANCED_SEARCH_SUBDIVISION_WARRANTY_DEED	=
		"Advanced Search - searching with subdivision at last name for WARRANTY_DEEDs documents";
	public static final String VALUE_PARAM_ADVANCED_SEARCH_OWNERS	=
		"Advanced Search - searching with owner(s) name";
	public static final String VALUE_PARAM_ADVANCED_SEARCH_OWNERS_FOR_LEGAL	=
		"Advanced Search - searching with owner(s) name for Legal";
	public static final String VALUE_PARAM_ADVANCED_SEARCH_BUYERS	=
		"Advanced Search - searching with buyer(s) name";
	public static final String VALUE_PARAM_ADVANCED_SEARCH_BUYERS_LIEN	=
		"Advanced Search - searching with buyer(s) name for LIENs";	
}
